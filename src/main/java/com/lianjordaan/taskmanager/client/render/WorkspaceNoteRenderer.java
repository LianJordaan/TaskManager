package com.lianjordaan.taskmanager.client.render;

import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class WorkspaceNoteRenderer {
    public static final int HEADER_HEIGHT = 22;
    public static final int RESIZE_HANDLE_SIZE = 10;

    private WorkspaceNoteRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, Font font, WorkspaceNote note, boolean overlayMode, boolean selected) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        int width = note.getRenderedWidth();
        int height = note.getRenderedHeight();
        int tint = note.getTintColor();
        int surfaceRgb = blend(0x101722, tint, 0.22F);
        int accent = withAlpha(blend(tint, 0xFFFFFF, 0.08F), alphaFromOpacity(note.getCardOpacity() + 0.08F));
        int surface = withAlpha(surfaceRgb, alphaFromOpacity(note.getCardOpacity()));
        int outline = selected ? 0xFFF8E38A : withAlpha(blend(surfaceRgb, 0xD7E0EA, 0.28F), 96);
        int divider = withAlpha(blend(surfaceRgb, tint, 0.42F), 72);
        int textAlpha = overlayMode && note.isHidden() ? 144 : 255;

        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, outline);
        guiGraphics.fill(x, y, x + width, y + height, surface);
        guiGraphics.fill(x, y, x + width, y + HEADER_HEIGHT, accent);
        guiGraphics.fill(x, y + HEADER_HEIGHT, x + width, y + HEADER_HEIGHT + 1, divider);

        guiGraphics.drawString(font, note.getSummary(), x + 8, y + 7, withAlpha(0xFFF9FCFF, textAlpha), false);
        guiGraphics.drawString(font, note.getScope() == WorkspaceScope.GLOBAL ? "GLOBAL" : "CONTEXT", x + width - 48, y + 7, withAlpha(0xFFF9FCFF, textAlpha), false);

        int badgeX = x + width - 68;
        if (note.isHidden()) {
            guiGraphics.drawString(font, "H", badgeX, y + 7, withAlpha(0xFFD5E7FF, textAlpha), false);
            badgeX -= 12;
        }
        if (note.isLocked()) {
            guiGraphics.drawString(font, "L", badgeX, y + 7, withAlpha(0xFFD5E7FF, textAlpha), false);
        }

        MarkdownRenderer.render(guiGraphics, font, note.getContent(), x + 10, y + HEADER_HEIGHT + 8, width - 18, height - HEADER_HEIGHT - 16, textAlpha);

        if (overlayMode) {
            if (note.isHidden()) {
                guiGraphics.fill(x, y, x + width, y + height, withAlpha(blend(0x303B4C, tint, 0.12F), 102));
            }
            if (selected && !note.isLocked()) {
                guiGraphics.fill(x + width - RESIZE_HANDLE_SIZE, y + height - RESIZE_HANDLE_SIZE, x + width, y + height, 0xFF7CE2C2);
            }
        }
    }

    public static boolean contains(WorkspaceNote note, double mouseX, double mouseY) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        return mouseX >= x && mouseX <= x + note.getRenderedWidth() && mouseY >= y && mouseY <= y + note.getRenderedHeight();
    }

    public static boolean isHeaderHit(WorkspaceNote note, double mouseX, double mouseY) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        return mouseX >= x && mouseX <= x + note.getRenderedWidth() && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public static boolean isResizeHandleHit(WorkspaceNote note, double mouseX, double mouseY) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        int width = note.getRenderedWidth();
        int height = note.getRenderedHeight();
        return mouseX >= x + width - RESIZE_HANDLE_SIZE && mouseX <= x + width
            && mouseY >= y + height - RESIZE_HANDLE_SIZE && mouseY <= y + height;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int alphaFromOpacity(float opacity) {
        float clamped = Math.max(0.20F, Math.min(1.0F, opacity));
        return Math.max(51, Math.min(255, Math.round(255.0F * clamped)));
    }

    private static int blend(int from, int to, float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        int fromRed = (from >> 16) & 0xFF;
        int fromGreen = (from >> 8) & 0xFF;
        int fromBlue = from & 0xFF;
        int toRed = (to >> 16) & 0xFF;
        int toGreen = (to >> 8) & 0xFF;
        int toBlue = to & 0xFF;

        int red = Math.round(fromRed + (toRed - fromRed) * clamped);
        int green = Math.round(fromGreen + (toGreen - fromGreen) * clamped);
        int blue = Math.round(fromBlue + (toBlue - fromBlue) * clamped);
        return (red << 16) | (green << 8) | blue;
    }
}