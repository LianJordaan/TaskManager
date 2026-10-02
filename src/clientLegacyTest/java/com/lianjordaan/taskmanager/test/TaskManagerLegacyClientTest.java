package com.lianjordaan.taskmanager.test;

import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.screen.WorkspaceOverlayScreen;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayLayoutState;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayPanel;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** Test-only real-client UI check for releases before Fabric's client GameTest API. */
public final class TaskManagerLegacyClientTest implements ClientModInitializer {
    private int ticks;
    private int step;
    private int stepAt;
    private boolean connecting;
    private boolean failed;
    private Path image;

    @Override
    public void onInitializeClient() {
        verifyProductionJar();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (failed || step == 9) {
            return;
        }
        try {
            ticks++;
            if (ticks > 2400) {
                throw new AssertionError("TaskManager client test timed out at step " + step);
            }
            if (step == 0) {
                if (client.player != null && client.level != null) {
                    System.out.println("TaskManager joined server: " + client.player.getName().getString());
                    advance(1);
                } else if (!connecting && ticks >= 180) {
                    connecting = true;
                    LegacyConnect.connect(client);
                }
            } else if (step == 1 && waited(450)) {
                client.setScreen(new WorkspaceOverlayScreen(true));
                advance(2);
            } else if (step == 2 && waited(12)) {
                requireOverlay(client);
                WorkspaceManager manager = WorkspaceManager.getInstance();
                manager.refreshContext(client);
                List<WorkspaceNote> notes = manager.getNotes(WorkspaceScope.CONTEXT);
                if (notes.isEmpty()) {
                    throw new AssertionError("Quick-create did not make a context task card");
                }
                WorkspaceNote note = notes.get(notes.size() - 1);
                note.setTitle("Expedition checklist");
                note.setContent("- [ ] Gather supplies\n- [x] Find a village\n- [ ] Set up camp");
                manager.saveNote(note);
                client.setScreen(new WorkspaceOverlayScreen(false));
                advance(3);
            } else if (step == 3 && waited(12)) {
                requireOverlay(client);
                requireReadableLayout(client);
                capture(client, "overlay");
                advance(4);
            } else if (step == 4 && imageReady()) {
                System.out.println("TaskManager overlay screenshot: " + image.toAbsolutePath());
                WorkspaceNote note = latestContextNote();
                double x = note.getX() + note.getScale() * (20 + client.font.width("[ ]") / 2.0);
                double y = note.getY() + note.getScale()
                    * (WorkspaceNoteRenderer.HEADER_HEIGHT + 8 + client.font.lineHeight / 2.0);
                requireOverlay(client);
                client.screen.mouseClicked(x, y, 0);
                advance(5);
            } else if (step == 5 && waited(5)) {
                String checked = latestContextNote().getContent();
                if (!checked.startsWith("- [x] Gather supplies")) {
                    throw new AssertionError("Rendered checkbox click did not change the task: " + checked);
                }
                if (!savedTaskExists(client)) {
                    throw new AssertionError("Checked task was not written to the context JSON");
                }
                capture(client, "task-checked");
                advance(6);
            } else if (step == 6 && imageReady()) {
                System.out.println("TaskManager checked-task screenshot: " + image.toAbsolutePath());
                client.setScreen(null);
                advance(7);
            } else if (step == 7 && waited(12)) {
                capture(client, "hud");
                advance(8);
            } else if (step == 8 && imageReady()) {
                System.out.println("TaskManager HUD screenshot: " + image.toAbsolutePath());
                System.out.println("TASKMANAGER_LEGACY_PASS rendered checkbox, saved context, and HUD");
                advance(9);
                client.stop();
            }
        } catch (Throwable error) {
            failed = true;
            System.err.println("TASKMANAGER_LEGACY_FAIL step=" + step + " " + error);
            error.printStackTrace();
            client.stop();
        }
    }

    private void advance(int next) {
        step = next;
        stepAt = ticks;
    }

    private boolean waited(int count) {
        return ticks - stepAt >= count;
    }

    private boolean imageReady() {
        if (ticks - stepAt > 120) {
            throw new AssertionError("Screenshot did not finish: " + image);
        }
        try {
            return Files.isRegularFile(image) && Files.size(image) > 8;
        } catch (Exception error) {
            throw new AssertionError("Could not inspect screenshot " + image, error);
        }
    }

    private void capture(Minecraft client, String scene) {
        String name = "taskmanager-" + System.getProperty("taskmanager.test.minecraft")
            + "-" + scene + ".png";
        image = client.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
            message -> System.out.println("TaskManager screenshot result: " + message.getString()));
    }

    private static void requireOverlay(Minecraft client) {
        if (!(client.screen instanceof WorkspaceOverlayScreen)) {
            throw new AssertionError("TaskManager overlay did not open: " + client.screen);
        }
    }

    private static WorkspaceNote latestContextNote() {
        List<WorkspaceNote> notes = WorkspaceManager.getInstance().getNotes(WorkspaceScope.CONTEXT);
        if (notes.isEmpty()) {
            throw new AssertionError("The context task card disappeared");
        }
        return notes.get(notes.size() - 1);
    }

    private static void requireReadableLayout(Minecraft client) {
        WorkspaceNote note = latestContextNote();
        try {
            Field field = WorkspaceOverlayScreen.class.getDeclaredField("overlayLayout");
            field.setAccessible(true);
            WorkspaceOverlayLayoutState layout = (WorkspaceOverlayLayoutState) field.get(client.screen);
            WorkspaceOverlayLayoutState.PanelLayout editor = layout.getPanel(WorkspaceOverlayPanel.EDITOR);
            if (editor == null) {
                throw new AssertionError("The task editor is missing in the UI test");
            }
            if (editor.isMinimized()) {
                WorkspaceOverlayLayoutState.PanelLayout settings = layout.getPanel(WorkspaceOverlayPanel.SETTINGS);
                if (settings != null && !settings.isMinimized()) {
                    throw new AssertionError("Compact settings panel did not minimize");
                }
                if (note.getX() < 86 && note.getX() + note.getRenderedWidth() > 8
                    && note.getY() < 60 && note.getY() + note.getRenderedHeight() > 40) {
                    throw new AssertionError("Compact task card obscures the Notes drawer toggle");
                }
                boolean headerOverlapsCard = note.getX() < editor.getX() + editor.getWidth()
                    && note.getX() + note.getRenderedWidth() > editor.getX()
                    && note.getY() < editor.getY() + 22
                    && note.getY() + note.getRenderedHeight() > editor.getY();
                if (headerOverlapsCard) {
                    throw new AssertionError("Compact task card obscures the minimized editor control");
                }
                if (settings != null) {
                    boolean settingsOverlapsCard = note.getX() < settings.getX() + settings.getWidth()
                        && note.getX() + note.getRenderedWidth() > settings.getX()
                        && note.getY() < settings.getY() + 22
                        && note.getY() + note.getRenderedHeight() > settings.getY();
                    if (settingsOverlapsCard || settings.getY() + 22 > editor.getY()) {
                        throw new AssertionError("Compact panel headers collide with the task card or each other");
                    }
                }
                double controlX = editor.getX() + editor.getWidth() - 15;
                double controlY = editor.getY() + 10;
                client.screen.mouseClicked(controlX, controlY, 0);
                if (editor.isMinimized()) {
                    throw new AssertionError("The editor could not be reopened on the narrow screen");
                }
                client.screen.mouseClicked(editor.getX() + editor.getWidth() - 15,
                    editor.getY() + 10, 0);
                if (!editor.isMinimized()) {
                    throw new AssertionError("The editor could not be minimized again");
                }
                if (settings != null && (note.getY() + note.getRenderedHeight() > settings.getY()
                    || settings.getY() + 22 > editor.getY())) {
                    throw new AssertionError("Compact panel headers moved over the task after closing the editor");
                }
                System.out.println("TaskManager compact editor reopen PASS: GUI "
                    + client.screen.width + "x" + client.screen.height);
                return;
            }
            boolean overlap = note.getX() < editor.getX() + editor.getWidth()
                && note.getX() + note.getRenderedWidth() > editor.getX()
                && note.getY() < editor.getY() + editor.getHeight()
                && note.getY() + note.getRenderedHeight() > editor.getY();
            if (overlap) {
                throw new AssertionError("Task card overlaps the editor at this GUI scale: card="
                    + note.getX() + "," + note.getY() + " width=" + note.getRenderedWidth()
                    + "; editor=" + editor.getX() + "," + editor.getY());
            }
            System.out.println("TaskManager layout clear: GUI " + client.screen.width + "x"
                + client.screen.height + ", card x=" + note.getX() + " width="
                + note.getRenderedWidth() + ", editor x=" + editor.getX());
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not inspect the real TaskManager panel layout", error);
        }
    }

    private static boolean savedTaskExists(Minecraft client) {
        Path contexts = client.gameDirectory.toPath().resolve("config/taskmanager/contexts");
        if (!Files.isDirectory(contexts)) {
            return false;
        }
        try (var files = Files.list(contexts)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".json"))
                .anyMatch(path -> {
                    try {
                        String json = Files.readString(path);
                        return json.contains("Expedition checklist")
                            && json.contains("- [x] Gather supplies");
                    } catch (Exception error) {
                        return false;
                    }
                });
        } catch (Exception error) {
            throw new AssertionError("Could not inspect saved TaskManager context", error);
        }
    }

    private static void verifyProductionJar() {
        String expected = System.getProperty("taskmanager.test.sha512", "");
        if (!expected.matches("[a-f0-9]{128}")) {
            throw new AssertionError("A frozen production JAR SHA-512 is required");
        }
        Path jar = FabricLoader.getInstance().getModContainer("taskmanager")
            .orElseThrow(() -> new AssertionError("TaskManager is not loaded"))
            .getOrigin().getPaths().stream().findFirst()
            .orElseThrow(() -> new AssertionError("TaskManager origin is missing"));
        if (!Files.isRegularFile(jar)) {
            throw new AssertionError("TaskManager did not load from a production JAR: " + jar);
        }
        try (InputStream input = Files.newInputStream(jar)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] block = new byte[1024 * 1024];
            int count;
            while ((count = input.read(block)) >= 0) {
                digest.update(block, 0, count);
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!expected.equals(actual)) {
                throw new AssertionError("TaskManager production JAR differs from the frozen SHA-512");
            }
            System.out.println("TaskManager tested JAR: " + jar.toAbsolutePath() + " SHA-512 " + actual);
        } catch (Exception error) {
            throw new AssertionError("Could not verify TaskManager production JAR", error);
        }
    }
}
