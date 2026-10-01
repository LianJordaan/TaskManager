package com.lianjordaan.taskmanager.test;

import com.lianjordaan.taskmanager.client.screen.WorkspaceOverlayScreen;
import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** A private production-JAR client check and source of real in-game screenshots. */
@SuppressWarnings("UnstableApiUsage")
public final class TaskManagerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        verifyProductionJar();
        context.getInput().resizeWindow(1600, 900);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(60);
            context.runOnClient(client -> {
                client.options.guiScale().set(2);
            });
            context.runOnClient(client -> ClientScreenControl.setScreen(client, new WorkspaceOverlayScreen(true)));
            context.waitForScreen(WorkspaceOverlayScreen.class);
            context.waitTicks(8);

            context.runOnClient(client -> {
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
            });
            context.runOnClient(client -> ClientScreenControl.setScreen(client, new WorkspaceOverlayScreen(false)));
            context.waitForScreen(WorkspaceOverlayScreen.class);
            context.waitTicks(5);
            Path overlay = context.takeScreenshot(screenshotName("overlay"));
            if (!Files.isRegularFile(overlay)) {
                throw new AssertionError("Overlay screenshot was not written");
            }
            System.out.println("TaskManager overlay screenshot: " + overlay.toAbsolutePath());

            double[] click = context.computeOnClient(client -> {
                WorkspaceNote note = latestContextNote();
                double guiX = note.getX() + note.getScale() * (20 + client.font.width("[ ]") / 2.0);
                double guiY = note.getY() + note.getScale()
                    * (WorkspaceNoteRenderer.HEADER_HEIGHT + 8 + client.font.lineHeight / 2.0);
                return new double[]{
                    guiX * client.getWindow().getWidth() / client.getWindow().getGuiScaledWidth(),
                    guiY * client.getWindow().getHeight() / client.getWindow().getGuiScaledHeight()
                };
            });
            context.getInput().setCursorPos(click[0], click[1]);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(3);
            String checked = context.computeOnClient(client -> latestContextNote().getContent());
            if (!checked.startsWith("- [x] Gather supplies")) {
                throw new AssertionError("Clicking the task checkbox did not save the checked state: " + checked);
            }
            Path checkedOverlay = context.takeScreenshot(screenshotName("task-checked"));
            if (!Files.isRegularFile(checkedOverlay)) {
                throw new AssertionError("Checked-task screenshot was not written");
            }
            System.out.println("TaskManager checked-task screenshot: " + checkedOverlay.toAbsolutePath());

            context.runOnClient(client -> ClientScreenControl.setScreen(client, null));
            context.waitTicks(5);
            Path hud = context.takeScreenshot(screenshotName("hud"));
            if (!Files.isRegularFile(hud)) {
                throw new AssertionError("HUD screenshot was not written");
            }
            System.out.println("TaskManager HUD screenshot: " + hud.toAbsolutePath());
        }
    }

    private static WorkspaceNote latestContextNote() {
        List<WorkspaceNote> notes = WorkspaceManager.getInstance().getNotes(WorkspaceScope.CONTEXT);
        if (notes.isEmpty()) {
            throw new AssertionError("The test task card was not found");
        }
        return notes.get(notes.size() - 1);
    }

    private static String screenshotName(String scene) {
        return "taskmanager-" + System.getProperty("taskmanager.test.minecraft", "unknown") + "-" + scene;
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
                throw new AssertionError("TaskManager production JAR differs from the pinned SHA-512");
            }
            System.out.println("TaskManager tested JAR: " + jar.toAbsolutePath() + " SHA-512 " + actual);
        } catch (Exception error) {
            throw new AssertionError("Could not verify TaskManager production JAR", error);
        }
    }
}
