package com.lianjordaan.taskmanager.client.hud;

import com.lianjordaan.taskmanager.TaskManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class HudRenderCompat {
    private HudRenderCompat() {
    }

    public static void register(WorkspaceHudRenderer renderer) {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(TaskManager.MOD_ID, "notes"),
            (graphics, deltaTracker) -> renderer.render(Minecraft.getInstance(), graphics, 0.0F));
    }
}
