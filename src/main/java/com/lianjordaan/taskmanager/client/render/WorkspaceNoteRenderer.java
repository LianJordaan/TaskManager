package com.lianjordaan.taskmanager.client.render;

import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class WorkspaceNoteRenderer {
    public static final int HEADER_HEIGHT = 22;
    public static final int RESIZE_HANDLE_SIZE = 10;
    private static final int BASE_TEXT_COLOR = 0xFFF9FCFF;
    private static final int SELECTED_OUTLINE = 0xFF71C8FF;

    private WorkspaceNoteRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, Font font, WorkspaceNote note, boolean overlayMode, boolean selected) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        int width = Math.max(160, Math.round(note.getWidth()));
        int height = Math.max(96, Math.round(note.getHeight()));
        int tint = note.getTintColor();
        int surfaceRgb = blend(0x101722, tint, 0.22F);
        int accent = withAlpha(blend(tint, 0xFFFFFF, 0.08F), alphaFromOpacity(note.getCardOpacity() + 0.08F));
        int surface = withAlpha(surfaceRgb, alphaFromOpacity(note.getCardOpacity()));
        int outline = selected ? SELECTED_OUTLINE : withAlpha(blend(surfaceRgb, 0xD7E0EA, 0.28F), 96);
        int divider = withAlpha(blend(surfaceRgb, tint, 0.42F), 72);
        int textAlpha = overlayMode && note.isHidden() ? 144 : 255;
        int scopeLabelWidth = font.width(note.getScope() == WorkspaceScope.GLOBAL ? "GLOBAL" : "CONTEXT");
        int titleWidth = Math.max(40, width - scopeLabelWidth - 22);

        WorkspaceRenderCompat.pushTranslateScale(guiGraphics, x, y, note.getScale());

        guiGraphics.fill(-1, -1, width + 1, height + 1, outline);
        guiGraphics.fill(0, 0, width, height, surface);
        guiGraphics.fill(0, 0, width, HEADER_HEIGHT, accent);
        guiGraphics.fill(0, HEADER_HEIGHT, width, HEADER_HEIGHT + 1, divider);

        guiGraphics.drawString(font, trimToWidth(font, note.getDisplayTitle(), titleWidth), 8, 7, withAlpha(BASE_TEXT_COLOR, textAlpha), false);
        guiGraphics.drawString(font, note.getScope() == WorkspaceScope.GLOBAL ? "GLOBAL" : "CONTEXT", width - scopeLabelWidth - 8, 7, withAlpha(BASE_TEXT_COLOR, textAlpha), false);

        int badgeX = width - scopeLabelWidth - 28;
        if (note.isHidden()) {
            guiGraphics.drawString(font, "H", badgeX, 7, withAlpha(0xFFD5E7FF, textAlpha), false);
            badgeX -= 12;
        }
        if (note.isLocked()) {
            guiGraphics.drawString(font, "L", badgeX, 7, withAlpha(0xFFD5E7FF, textAlpha), false);
        }

        MarkdownRenderer.render(guiGraphics, font, note.getContent(), 10, HEADER_HEIGHT + 8, width - 18, height - HEADER_HEIGHT - 16, textAlpha);

        if (overlayMode) {
            if (note.isHidden()) {
                guiGraphics.fill(0, 0, width, height, withAlpha(blend(0x303B4C, tint, 0.12F), 102));
            }
            if (selected && !note.isLocked()) {
                int handleSize = getRenderedResizeHandleSize(note, true);
                guiGraphics.fill(width - handleSize, height - handleSize, width, height, 0xFF7CE2C2);
            }
        }

        WorkspaceRenderCompat.pop(guiGraphics);
    }

    public static boolean contains(WorkspaceNote note, double mouseX, double mouseY) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        return mouseX >= x && mouseX <= x + note.getRenderedWidth() && mouseY >= y && mouseY <= y + note.getRenderedHeight();
    }

    public static boolean isHeaderHit(WorkspaceNote note, double mouseX, double mouseY) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        return mouseX >= x && mouseX <= x + note.getRenderedWidth() && mouseY >= y && mouseY <= y + getRenderedHeaderHeight(note);
    }

    public static boolean isResizeHandleHit(WorkspaceNote note, double mouseX, double mouseY) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        int width = note.getRenderedWidth();
        int height = note.getRenderedHeight();
        int handleSize = getRenderedResizeHandleSize(note, false);
        return mouseX >= x + width - handleSize && mouseX <= x + width
            && mouseY >= y + height - handleSize && mouseY <= y + height;
    }

    public static int getRenderedHeaderHeight(WorkspaceNote note) {
        return Math.max(14, Math.round(HEADER_HEIGHT * note.getScale()));
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int alphaFromOpacity(float opacity) {
        float clamped = Math.max(0.10F, Math.min(1.0F, opacity));
        return Math.max(26, Math.min(255, Math.round(255.0F * clamped)));
    }

    private static int getRenderedResizeHandleSize(WorkspaceNote note, boolean localSpace) {
        int scaledSize = Math.max(8, Math.round(RESIZE_HANDLE_SIZE * note.getScale()));
        if (localSpace) {
            return Mth.ceil(scaledSize / Math.max(0.01F, note.getScale()));
        }
        return scaledSize;
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();
        while (end > 1 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
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