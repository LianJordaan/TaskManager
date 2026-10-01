package com.lianjordaan.taskmanager.client.input;

import com.lianjordaan.taskmanager.TaskManager;
import com.lianjordaan.taskmanager.client.screen.WorkspaceOverlayScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class WorkspaceKeybindings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(TaskManager.MOD_ID, "controls"));

    private static KeyMapping toggleOverlayKey;
    private static KeyMapping quickCreateKey;
    private static boolean initialized;

    private WorkspaceKeybindings() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        toggleOverlayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + TaskManager.MOD_ID + ".toggle_overlay",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_O, CATEGORY));
        quickCreateKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + TaskManager.MOD_ID + ".quick_create_note",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_N, CATEGORY));

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
        if (client.gui.screen() instanceof WorkspaceOverlayScreen workspaceOverlayScreen) {
            if (quickCreateRequested) {
                workspaceOverlayScreen.requestQuickCreate();
            } else {
                client.gui.setScreen(null);
            }
            return;
        }
        client.gui.setScreen(new WorkspaceOverlayScreen(quickCreateRequested));
    }
}
