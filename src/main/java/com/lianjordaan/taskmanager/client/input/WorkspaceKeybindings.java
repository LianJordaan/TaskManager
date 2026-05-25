package com.lianjordaan.taskmanager.client.input;

import com.lianjordaan.taskmanager.TaskManager;
import com.lianjordaan.taskmanager.client.screen.WorkspaceOverlayScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class WorkspaceKeybindings {
    private static final String CATEGORY = "key.categories." + TaskManager.MOD_ID;

    private static KeyMapping toggleOverlayKey;
    private static KeyMapping quickCreateKey;
    private static boolean initialized;

    private WorkspaceKeybindings() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        toggleOverlayKey = KeyBindingHelper.registerKeyBinding(KeyMappingCompat.create(
            "key." + TaskManager.MOD_ID + ".toggle_overlay",
            GLFW.GLFW_KEY_O,
            CATEGORY
        ));

        quickCreateKey = KeyBindingHelper.registerKeyBinding(KeyMappingCompat.create(
            "key." + TaskManager.MOD_ID + ".quick_create_note",
            GLFW.GLFW_KEY_N,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOverlayKey.consumeClick()) {
                toggleOverlay(client, false);
            }

            while (quickCreateKey.consumeClick()) {
                toggleOverlay(client, true);
            }
        });

        initialized = true;
    }

    private static void toggleOverlay(Minecraft client, boolean quickCreateRequested) {
        if (client.player == null) {
            return;
        }

        if (client.screen instanceof WorkspaceOverlayScreen workspaceOverlayScreen) {
            if (quickCreateRequested) {
                workspaceOverlayScreen.requestQuickCreate();
                return;
            }

            client.setScreen(null);
            return;
        }

        client.setScreen(new WorkspaceOverlayScreen(quickCreateRequested));
    }
}