package com.lianjordaan.taskmanager.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class WorkspaceCanvasCompat {
    private WorkspaceCanvasCompat() {
    }

    public static WorkspaceCanvas wrap(Object graphics) {
        GuiGraphics delegate = (GuiGraphics) graphics;
        return new WorkspaceCanvas() {
            @Override
            public void push(float x, float y, float scale) {
                WorkspaceRenderCompat.pushTranslateScale(delegate, x, y, scale);
            }

            @Override
            public void pop() {
                WorkspaceRenderCompat.pop(delegate);
            }

            @Override
            public void fill(int left, int top, int right, int bottom, int color) {
                delegate.fill(left, top, right, bottom, color);
            }

            @Override
            public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
                delegate.drawString(font, text, x, y, color, shadow);
            }
        };
    }
}
