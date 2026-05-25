package com.lianjordaan.taskmanager.client.render;

import net.minecraft.client.gui.GuiGraphics;

public final class WorkspaceRenderCompat {
    private WorkspaceRenderCompat() {
    }

    public static void pushTranslateScale(GuiGraphics guiGraphics, float x, float y, float scale) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
    }

    public static void pop(GuiGraphics guiGraphics) {
        guiGraphics.pose().popMatrix();
    }
}