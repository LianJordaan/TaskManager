package com.lianjordaan.taskmanager.client.render;

import net.minecraft.client.gui.GuiGraphics;

public final class WorkspaceRenderCompat {
    private WorkspaceRenderCompat() {
    }

    public static void pushTranslateScale(GuiGraphics guiGraphics, float x, float y, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
    }

    public static void pop(GuiGraphics guiGraphics) {
        guiGraphics.pose().popPose();
    }
}