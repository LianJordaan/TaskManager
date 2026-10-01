package com.lianjordaan.taskmanager.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class WorkspaceCanvasCompat {
    private WorkspaceCanvasCompat() {
    }

    public static WorkspaceCanvas wrap(Object graphics) {
        GuiGraphicsExtractor delegate = (GuiGraphicsExtractor) graphics;
        return new WorkspaceCanvas() {
            @Override
            public void push(float x, float y, float scale) {
                delegate.pose().pushMatrix();
                delegate.pose().translate(x, y);
                delegate.pose().scale(scale, scale);
            }

            @Override
            public void pop() {
                delegate.pose().popMatrix();
            }

            @Override
            public void fill(int left, int top, int right, int bottom, int color) {
                delegate.fill(left, top, right, bottom, color);
            }

            @Override
            public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
                delegate.text(font, text, x, y, color, shadow);
            }
        };
    }
}
