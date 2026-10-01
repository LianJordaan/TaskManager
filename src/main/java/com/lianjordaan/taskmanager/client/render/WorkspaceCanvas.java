package com.lianjordaan.taskmanager.client.render;

import net.minecraft.client.gui.Font;

/** The small drawing surface used by note cards across Minecraft GUI revisions. */
public interface WorkspaceCanvas {
    void push(float x, float y, float scale);

    void pop();

    void fill(int left, int top, int right, int bottom, int color);

    void drawString(Font font, String text, int x, int y, int color, boolean shadow);
}
