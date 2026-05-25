package com.lianjordaan.taskmanager.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public final class HudRenderCompat {
    private HudRenderCompat() {
    }

    public static void register(WorkspaceHudRenderer renderer) {
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> renderer.render(Minecraft.getInstance(), guiGraphics, 0.0F));
    }
}