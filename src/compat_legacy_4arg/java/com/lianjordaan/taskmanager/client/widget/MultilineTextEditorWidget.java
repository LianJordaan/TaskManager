package com.lianjordaan.taskmanager.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MultilineTextEditorWidget extends AbstractWidget {
    private final Font font;

    private Consumer<String> responder = value -> {
    };
    private String value = "";
    private int cursorIndex;
    private int selectionAnchor;
    private int preferredColumn = -1;
    private int scrollOffset;
    private boolean editable = true;
    private boolean singleLine;
    private boolean dragSelecting;

    public MultilineTextEditorWidget(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Workspace note editor"));
        this.font = font;
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder == null ? value -> {
        } : responder;
    }

    public void setValue(String value) {
        this.value = normalizeIncomingValue(value);
        cursorIndex = Math.min(cursorIndex, this.value.length());
        selectionAnchor = Math.min(selectionAnchor, this.value.length());
        ensureCursorVisible();
    }

    public String getValue() {
        return value;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setBounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        this.width = width;
        this.height = height;
    }

    public void setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;
        this.value = normalizeIncomingValue(value);
        cursorIndex = Math.min(cursorIndex, this.value.length());
        selectionAnchor = Math.min(selectionAnchor, this.value.length());
        scrollOffset = 0;
        ensureCursorVisible();
    }

    public boolean isSingleLine() {
        return singleLine;
    }

    public void insertSnippet(String snippet, int cursorBacktrack) {
        if (!editable) {
            return;
        }

        replaceSelection(normalizeInsertedText(snippet == null ? "" : snippet), true);
        cursorIndex = Math.max(0, cursorIndex - Math.max(0, cursorBacktrack));
        selectionAnchor = cursorIndex;
        preferredColumn = -1;
        ensureCursorVisible();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            preferredColumn = -1;
            dragSelecting = false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            setFocused(false);
            dragSelecting = false;
            return false;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        setFocused(true);
        int targetIndex = cursorIndexForPosition(mouseX, mouseY);
        if (Screen.hasShiftDown()) {
            if (!hasSelection()) {
                selectionAnchor = cursorIndex;
            }
            cursorIndex = targetIndex;
        } else {
            cursorIndex = targetIndex;
            selectionAnchor = cursorIndex;
        }
        dragSelecting = true;
        preferredColumn = -1;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragSelecting || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isFocused()) {
            return false;
        }

        cursorIndex = cursorIndexForPosition(mouseX, mouseY);
        preferredColumn = -1;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragSelecting) {
            dragSelecting = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (singleLine || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int maxScroll = Math.max(0, getLines().size() - visibleLineCount());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || !editable) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean controlDown = Screen.hasControlDown();
        boolean shiftDown = Screen.hasShiftDown();

        if (Screen.isPaste(keyCode)) {
            insertText(minecraft.keyboardHandler.getClipboard());
            return true;
        }

        if (controlDown && keyCode == GLFW.GLFW_KEY_A) {
            selectAll();
            return true;
        }

        if (controlDown && keyCode == GLFW.GLFW_KEY_C) {
            if (hasSelection()) {
                minecraft.keyboardHandler.setClipboard(getSelectedText());
            }
            return true;
        }

        if (controlDown && keyCode == GLFW.GLFW_KEY_X) {
            if (hasSelection()) {
                minecraft.keyboardHandler.setClipboard(getSelectedText());
                deleteSelection(true);
            }
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                deleteBackward();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                deleteForward();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!singleLine) {
                    insertText("\n");
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_TAB -> {
                if (!singleLine) {
                    insertText("    ");
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveHorizontal(-1, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveHorizontal(1, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveVertical(-1, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveVertical(1, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveToLineEdge(true, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveToLineEdge(false, shiftDown);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused() || !editable) {
            return false;
        }

        if (codePoint < 32 || codePoint == 127) {
            return false;
        }

        insertText(Character.toString(codePoint));
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;

        guiGraphics.fill(left, top, right, bottom, 0xE918202C);
        guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, isFocused() ? 0xFF7CE2C2 : 0x6039495B);

        List<String> lines = getLines();
        int visibleLines = visibleLineCount();
        renderSelection(guiGraphics, lines, left, top, visibleLines);

        int drawY = top + 6;
        for (int lineIndex = scrollOffset; lineIndex < lines.size() && lineIndex < scrollOffset + visibleLines; lineIndex++) {
            String line = lines.get(lineIndex);
            guiGraphics.drawString(font, trimToWidth(line, width - 12), left + 6, drawY, editable ? 0xFFE7ECF5 : 0xFF98A7B8, false);
            drawY += font.lineHeight;
        }

        if (isFocused() && editable && (System.currentTimeMillis() / 530L) % 2L == 0L) {
            CursorPosition cursorPosition = getCursorPosition();
            if (cursorPosition.line >= scrollOffset && cursorPosition.line < scrollOffset + visibleLines) {
                String lineText = lines.get(cursorPosition.line);
                int safeColumn = Math.min(cursorPosition.column, lineText.length());
                int cursorX = left + 6 + font.width(lineText.substring(0, safeColumn));
                int cursorY = top + 6 + (cursorPosition.line - scrollOffset) * font.lineHeight;
                guiGraphics.fill(cursorX, cursorY - 1, cursorX + 1, cursorY + font.lineHeight - 1, 0xFFF4F7FA);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private void insertText(String text) {
        replaceSelection(normalizeInsertedText(text), true);
    }

    private void deleteBackward() {
        if (deleteSelection(true)) {
            return;
        }
        if (cursorIndex <= 0) {
            return;
        }
        value = value.substring(0, cursorIndex - 1) + value.substring(cursorIndex);
        cursorIndex--;
        selectionAnchor = cursorIndex;
        preferredColumn = -1;
        ensureCursorVisible();
        responder.accept(value);
    }

    private void deleteForward() {
        if (deleteSelection(true)) {
            return;
        }
        if (cursorIndex >= value.length()) {
            return;
        }
        value = value.substring(0, cursorIndex) + value.substring(cursorIndex + 1);
        selectionAnchor = cursorIndex;
        preferredColumn = -1;
        ensureCursorVisible();
        responder.accept(value);
    }

    private boolean deleteSelection(boolean notify) {
        if (!hasSelection()) {
            return false;
        }

        int start = getSelectionStart();
        int end = getSelectionEnd();
        value = value.substring(0, start) + value.substring(end);
        cursorIndex = start;
        selectionAnchor = cursorIndex;
        preferredColumn = -1;
        ensureCursorVisible();
        if (notify) {
            responder.accept(value);
        }
        return true;
    }

    private void replaceSelection(String text, boolean notify) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        value = value.substring(0, start) + text + value.substring(end);
        cursorIndex = start + text.length();
        selectionAnchor = cursorIndex;
        preferredColumn = -1;
        ensureCursorVisible();
        if (notify) {
            responder.accept(value);
        }
    }

    private void moveHorizontal(int delta, boolean extendSelection) {
        if (!extendSelection && hasSelection()) {
            cursorIndex = delta < 0 ? getSelectionStart() : getSelectionEnd();
            selectionAnchor = cursorIndex;
            preferredColumn = -1;
            ensureCursorVisible();
            return;
        }

        if (extendSelection && !hasSelection()) {
            selectionAnchor = cursorIndex;
        }

        cursorIndex = Math.max(0, Math.min(value.length(), cursorIndex + delta));
        if (!extendSelection) {
            selectionAnchor = cursorIndex;
        }
        preferredColumn = -1;
        ensureCursorVisible();
    }

    private void moveVertical(int delta, boolean extendSelection) {
        if (singleLine) {
            return;
        }

        CursorPosition cursorPosition = getCursorPosition();
        List<String> lines = getLines();
        int targetLine = Math.max(0, Math.min(lines.size() - 1, cursorPosition.line + delta));
        int targetColumn = preferredColumn >= 0 ? preferredColumn : cursorPosition.column;
        if (extendSelection && !hasSelection()) {
            selectionAnchor = cursorIndex;
        }
        preferredColumn = targetColumn;
        cursorIndex = absoluteIndex(lines, targetLine, Math.min(lines.get(targetLine).length(), targetColumn));
        if (!extendSelection) {
            selectionAnchor = cursorIndex;
        }
        ensureCursorVisible();
    }

    private void moveToLineEdge(boolean start, boolean extendSelection) {
        CursorPosition cursorPosition = getCursorPosition();
        List<String> lines = getLines();
        if (extendSelection && !hasSelection()) {
            selectionAnchor = cursorIndex;
        }
        cursorIndex = absoluteIndex(lines, cursorPosition.line, start ? 0 : lines.get(cursorPosition.line).length());
        if (!extendSelection) {
            selectionAnchor = cursorIndex;
        }
        preferredColumn = -1;
        ensureCursorVisible();
    }

    private void selectAll() {
        selectionAnchor = 0;
        cursorIndex = value.length();
        preferredColumn = -1;
        ensureCursorVisible();
    }

    private boolean hasSelection() {
        return cursorIndex != selectionAnchor;
    }

    private int getSelectionStart() {
        return Math.min(cursorIndex, selectionAnchor);
    }

    private int getSelectionEnd() {
        return Math.max(cursorIndex, selectionAnchor);
    }

    private String getSelectedText() {
        if (!hasSelection()) {
            return "";
        }
        return value.substring(getSelectionStart(), getSelectionEnd());
    }

    private void ensureCursorVisible() {
        CursorPosition cursorPosition = getCursorPosition();
        int visibleLines = visibleLineCount();
        if (cursorPosition.line < scrollOffset) {
            scrollOffset = cursorPosition.line;
        } else if (cursorPosition.line >= scrollOffset + visibleLines) {
            scrollOffset = cursorPosition.line - visibleLines + 1;
        }
        if (singleLine) {
            scrollOffset = 0;
        }
    }

    private int cursorIndexForPosition(double mouseX, double mouseY) {
        List<String> lines = getLines();
        int line = singleLine
            ? 0
            : Math.max(0, Math.min(lines.size() - 1, scrollOffset + (int) ((mouseY - getY() - 6) / font.lineHeight)));
        String lineText = lines.get(line);
        int relativeX = Math.max(0, (int) mouseX - getX() - 6);
        int column = 0;
        while (column < lineText.length() && font.width(lineText.substring(0, column + 1)) <= relativeX) {
            column++;
        }
        return absoluteIndex(lines, line, column);
    }

    private CursorPosition getCursorPosition() {
        List<String> lines = getLines();
        int remaining = cursorIndex;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            int lineLength = lines.get(lineIndex).length();
            if (remaining <= lineLength) {
                return new CursorPosition(lineIndex, remaining);
            }
            remaining -= lineLength + 1;
        }
        int lastLine = lines.size() - 1;
        return new CursorPosition(lastLine, lines.get(lastLine).length());
    }

    private int absoluteIndex(List<String> lines, int line, int column) {
        int index = 0;
        for (int lineIndex = 0; lineIndex < line; lineIndex++) {
            index += lines.get(lineIndex).length() + 1;
        }
        return index + column;
    }

    private List<String> getLines() {
        if (singleLine) {
            List<String> lines = new ArrayList<>(1);
            lines.add(value);
            return lines;
        }

        String[] split = value.split("\\n", -1);
        List<String> lines = new ArrayList<>(split.length);
        for (String line : split) {
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private int visibleLineCount() {
        if (singleLine) {
            return 1;
        }
        return Math.max(1, (height - 12) / font.lineHeight);
    }

    private void renderSelection(GuiGraphics guiGraphics, List<String> lines, int left, int top, int visibleLines) {
        if (!hasSelection() || !isFocused()) {
            return;
        }

        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        int absoluteIndex = 0;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String lineText = lines.get(lineIndex);
            int lineStart = absoluteIndex;
            int lineEnd = lineStart + lineText.length();
            int nextIndex = lineEnd + 1;

            if (lineIndex >= scrollOffset && lineIndex < scrollOffset + visibleLines) {
                int overlapStart = Math.max(selectionStart, lineStart);
                int overlapEnd = Math.min(selectionEnd, lineEnd);
                if (selectionEnd > lineEnd && selectionStart <= lineEnd) {
                    overlapEnd = lineEnd;
                }
                if (overlapStart < overlapEnd || (selectionEnd > lineEnd && selectionStart <= lineEnd)) {
                    int startColumn = Math.max(0, overlapStart - lineStart);
                    int endColumn = Math.max(startColumn, Math.min(lineText.length(), overlapEnd - lineStart));
                    int selectionLeft = left + 6 + font.width(lineText.substring(0, startColumn));
                    int selectionRight = left + 6 + font.width(lineText.substring(0, endColumn));
                    if (selectionStart <= lineEnd && selectionEnd > lineEnd) {
                        selectionRight = left + 6 + font.width(lineText);
                    }
                    int selectionTop = top + 6 + (lineIndex - scrollOffset) * font.lineHeight;
                    guiGraphics.fill(selectionLeft, selectionTop - 1, Math.max(selectionLeft + 1, selectionRight), selectionTop + font.lineHeight - 1, 0x803A89C9);
                }
            }

            absoluteIndex = nextIndex;
        }
    }

    private String trimToWidth(String text, int maxWidth) {
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

    private String normalizeIncomingValue(String value) {
        String safeValue = value == null ? "" : value.replace("\r", "");
        return singleLine ? normalizeSingleLineText(safeValue) : safeValue;
    }

    private String normalizeInsertedText(String text) {
        String safeText = text == null ? "" : text.replace("\r", "");
        return singleLine ? normalizeSingleLineText(safeText) : safeText;
    }

    private String normalizeSingleLineText(String text) {
        return text.replace('\n', ' ').trim();
    }

    private record CursorPosition(int line, int column) {
    }
}