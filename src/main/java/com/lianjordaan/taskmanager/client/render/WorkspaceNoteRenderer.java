package com.lianjordaan.taskmanager.client.render;

import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import com.lianjordaan.taskmanager.client.workspace.TaskChecklist;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;

public final class WorkspaceNoteRenderer {
    public static final int HEADER_HEIGHT = 22;
    public static final int RESIZE_HANDLE_SIZE = 10;
    private static final int ACTION_BUTTON_SIZE = 12;
    private static final int ACTION_BUTTON_GAP = 3;
    private static final int ACTION_BUTTON_COUNT = 3;
    private static final int BASE_TEXT_COLOR = 0xFFF9FCFF;
    private static final int SELECTED_OUTLINE = 0xFF71C8FF;

    private WorkspaceNoteRenderer() {
    }

    public static void render(Object graphics, Font font, WorkspaceNote note, boolean overlayMode, boolean selected) {
        render(graphics, font, note, overlayMode, selected, false);
    }

    public static void render(Object graphics, Font font, WorkspaceNote note, boolean overlayMode, boolean selected, boolean showActions) {
        WorkspaceCanvas guiGraphics = WorkspaceCanvasCompat.wrap(graphics);
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
        int actionWidth = showActions ? actionStripWidth() + 8 : 0;
        int titleWidth = Math.max(40, width - scopeLabelWidth - actionWidth - 22);

        guiGraphics.push(x, y, note.getScale());

        guiGraphics.fill(-1, -1, width + 1, height + 1, outline);
        guiGraphics.fill(0, 0, width, height, surface);
        guiGraphics.fill(0, 0, width, HEADER_HEIGHT, accent);
        guiGraphics.fill(0, HEADER_HEIGHT, width, HEADER_HEIGHT + 1, divider);

        guiGraphics.drawString(font, trimToWidth(font, note.getDisplayTitle(), titleWidth), 8, 7, withAlpha(BASE_TEXT_COLOR, textAlpha), false);
        guiGraphics.drawString(font, note.getScope() == WorkspaceScope.GLOBAL ? "GLOBAL" : "CONTEXT", width - scopeLabelWidth - 8, 7, withAlpha(BASE_TEXT_COLOR, textAlpha), false);

        if (showActions) {
            renderActionButtons(guiGraphics, width, scopeLabelWidth, note, textAlpha);
        } else {
            int badgeX = width - scopeLabelWidth - 28;
            if (note.isHidden()) {
                guiGraphics.drawString(font, "H", badgeX, 7, withAlpha(0xFFD5E7FF, textAlpha), false);
                badgeX -= 12;
            }
            if (note.isLocked()) {
                guiGraphics.drawString(font, "L", badgeX, 7, withAlpha(0xFFD5E7FF, textAlpha), false);
            }
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

        guiGraphics.pop();
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

    /** Toggle a visible Markdown task marker in the overlay, if one was clicked. */
    public static boolean toggleCheckboxAt(WorkspaceNote note, Font font, double mouseX, double mouseY) {
        float scale = Math.max(0.01F, note.getScale());
        double localX = (mouseX - note.getX()) / scale - 10;
        double localY = (mouseY - note.getY()) / scale - HEADER_HEIGHT - 8;
        int width = Math.max(160, Math.round(note.getWidth()));
        int height = Math.max(96, Math.round(note.getHeight()));
        if (localX < 0 || localX >= width - 18 || localY < 0 || localY >= height - HEADER_HEIGHT - 16) {
            return false;
        }
        int line = MarkdownRenderer.checkboxAt(font, note.getContent(), width - 18, localX, localY);
        if (line < 0) {
            return false;
        }
        String updated = TaskChecklist.toggleLine(note.getContent(), line);
        if (updated.equals(note.getContent())) {
            return false;
        }
        note.setContent(updated);
        return true;
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

    public static boolean isHideButtonHit(WorkspaceNote note, Font font, double mouseX, double mouseY) {
        return isActionButtonHit(note, font, mouseX, mouseY, 0);
    }

    public static boolean isLockButtonHit(WorkspaceNote note, Font font, double mouseX, double mouseY) {
        return isActionButtonHit(note, font, mouseX, mouseY, 1);
    }

    public static boolean isDeleteButtonHit(WorkspaceNote note, Font font, double mouseX, double mouseY) {
        return isActionButtonHit(note, font, mouseX, mouseY, 2);
    }

    public static int getRenderedActionStripWidth(WorkspaceNote note) {
        return Math.max(1, Math.round(actionStripWidth() * note.getScale()));
    }

    public static int getRenderedHeaderHeight(WorkspaceNote note) {
        return Math.max(14, Math.round(HEADER_HEIGHT * note.getScale()));
    }

    private static void renderActionButtons(WorkspaceCanvas guiGraphics, int width, int scopeLabelWidth, WorkspaceNote note, int textAlpha) {
        int left = actionStripLeft(width, scopeLabelWidth);
        int top = 5;
        renderHideIcon(guiGraphics, left, top, note.isHidden(), textAlpha);
        renderLockIcon(guiGraphics, left + ACTION_BUTTON_SIZE + ACTION_BUTTON_GAP, top, note.isLocked(), textAlpha);
        renderDeleteIcon(guiGraphics, left + (ACTION_BUTTON_SIZE + ACTION_BUTTON_GAP) * 2, top, textAlpha);
    }

    private static void renderHideIcon(WorkspaceCanvas guiGraphics, int left, int top, boolean hidden, int textAlpha) {
        renderActionBackground(guiGraphics, left, top, hidden ? 0x74546A7F : 0x54384A5F);
        int color = withAlpha(0xFFE8F4FF, textAlpha);
        guiGraphics.fill(left + 2, top + 5, left + 10, top + 7, color);
        guiGraphics.fill(left + 5, top + 3, left + 7, top + 9, color);
        if (hidden) {
            for (int step = 0; step < 8; step++) {
                guiGraphics.fill(left + 2 + step, top + 9 - step, left + 3 + step, top + 10 - step, color);
            }
        }
    }

    private static void renderLockIcon(WorkspaceCanvas guiGraphics, int left, int top, boolean locked, int textAlpha) {
        renderActionBackground(guiGraphics, left, top, locked ? 0x746B5A34 : 0x54384A5F);
        int color = withAlpha(0xFFFFF1C6, textAlpha);
        guiGraphics.fill(left + 3, top + 6, left + 9, top + 10, color);
        guiGraphics.fill(left + 4, top + 3, left + 8, top + 5, color);
        guiGraphics.fill(left + 3, top + 4, left + 4, top + 7, color);
        guiGraphics.fill(left + 8, top + 4, left + 9, top + 7, color);
    }

    private static void renderDeleteIcon(WorkspaceCanvas guiGraphics, int left, int top, int textAlpha) {
        renderActionBackground(guiGraphics, left, top, 0x62563A43);
        int color = withAlpha(0xFFFFD8DD, textAlpha);
        for (int step = 0; step < 7; step++) {
            guiGraphics.fill(left + 3 + step, top + 3 + step, left + 4 + step, top + 4 + step, color);
            guiGraphics.fill(left + 9 - step, top + 3 + step, left + 10 - step, top + 4 + step, color);
        }
    }

    private static void renderActionBackground(WorkspaceCanvas guiGraphics, int left, int top, int fill) {
        guiGraphics.fill(left, top, left + ACTION_BUTTON_SIZE, top + ACTION_BUTTON_SIZE, fill);
        guiGraphics.fill(left, top, left + ACTION_BUTTON_SIZE, top + 1, 0x48FFFFFF);
    }

    private static boolean isActionButtonHit(WorkspaceNote note, Font font, double mouseX, double mouseY, int index) {
        int x = Math.round(note.getX());
        int y = Math.round(note.getY());
        float scale = Math.max(0.01F, note.getScale());
        double localX = (mouseX - x) / scale;
        double localY = (mouseY - y) / scale;
        int width = Math.max(160, Math.round(note.getWidth()));
        int scopeLabelWidth = font.width(note.getScope() == WorkspaceScope.GLOBAL ? "GLOBAL" : "CONTEXT");
        int left = actionStripLeft(width, scopeLabelWidth) + index * (ACTION_BUTTON_SIZE + ACTION_BUTTON_GAP);
        int top = 5;
        return localX >= left && localX <= left + ACTION_BUTTON_SIZE && localY >= top && localY <= top + ACTION_BUTTON_SIZE;
    }

    private static int actionStripLeft(int width, int scopeLabelWidth) {
        return width - scopeLabelWidth - 14 - actionStripWidth();
    }

    private static int actionStripWidth() {
        return ACTION_BUTTON_COUNT * ACTION_BUTTON_SIZE + (ACTION_BUTTON_COUNT - 1) * ACTION_BUTTON_GAP;
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
