package com.lianjordaan.taskmanager.test;

import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.screen.WorkspaceOverlayScreen;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.InputStream;
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
            } else if (step == 1 && waited(150)) {
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
