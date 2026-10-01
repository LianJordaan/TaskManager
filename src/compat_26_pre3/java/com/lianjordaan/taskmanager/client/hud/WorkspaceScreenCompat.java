package com.lianjordaan.taskmanager.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class WorkspaceScreenCompat {
    private WorkspaceScreenCompat() {
    }

    public static Screen currentScreen(Minecraft client) {
        return client.screen;
    }
}
