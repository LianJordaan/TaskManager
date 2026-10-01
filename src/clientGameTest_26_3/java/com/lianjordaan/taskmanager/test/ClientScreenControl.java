package com.lianjordaan.taskmanager.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ClientScreenControl {
    private ClientScreenControl() {
    }

    static void setScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }
}
