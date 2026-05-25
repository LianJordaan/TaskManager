package com.lianjordaan.taskmanager.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public final class HudRenderCompat {
    private HudRenderCompat() {
    }

    public static void register(WorkspaceHudRenderer renderer) {
        HudRenderCallback.EVENT.register((guiGraphics, partialTick) -> renderer.render(Minecraft.getInstance(), guiGraphics, partialTick));
    }
}