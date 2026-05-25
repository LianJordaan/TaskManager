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
    private int preferredColumn = -1;
    private int scrollOffset;
    private boolean editable = true;

    public MultilineTextEditorWidget(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Workspace note editor"));
        this.font = font;
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder == null ? value -> {
        } : responder;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        cursorIndex = Math.min(cursorIndex, this.value.length());
        ensureCursorVisible();
    }

    public String getValue() {
        return value;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void insertSnippet(String snippet, int cursorBacktrack) {
        if (!editable) {
            return;
        }

        String safeSnippet = snippet == null ? "" : snippet;
        value = value.substring(0, cursorIndex) + safeSnippet + value.substring(cursorIndex);
        cursorIndex += safeSnippet.length();
        cursorIndex = Math.max(0, cursorIndex - Math.max(0, cursorBacktrack));
        preferredColumn = -1;
        ensureCursorVisible();
        responder.accept(value);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            preferredColumn = -1;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            setFocused(false);
            return false;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        setFocused(true);
        cursorIndex = cursorIndexForPosition(mouseX, mouseY);
        preferredColumn = -1;
        ensureCursorVisible();
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int maxScroll = Math.max(0, getLines().size() - visibleLineCount());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(amount)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || !editable) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (Screen.isPaste(keyCode)) {
            insertText(minecraft.keyboardHandler.getClipboard());
            return true;
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_C) {
            minecraft.keyboardHandler.setClipboard(value);
            return true;
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_X) {
            minecraft.keyboardHandler.setClipboard(value);
            value = "";
            cursorIndex = 0;
            responder.accept(value);
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
                insertText("\n");
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                insertText("    ");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveHorizontal(-1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveHorizontal(1);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveVertical(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveVertical(1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveToLineEdge(true);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveToLineEdge(false);
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
                int cursorX = left + 6 + font.width(lineText.substring(0, Math.min(cursorPosition.column, lineText.length())));
                int cursorY = top + 6 + (cursorPosition.line - scrollOffset) * font.lineHeight;
                guiGraphics.fill(cursorX, cursorY - 1, cursorX + 1, cursorY + font.lineHeight - 1, 0xFFF4F7FA);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private void insertText(String text) {
        value = value.substring(0, cursorIndex) + text + value.substring(cursorIndex);
        cursorIndex += text.length();
        preferredColumn = -1;
        ensureCursorVisible();
        responder.accept(value);
    }

    private void deleteBackward() {
        if (cursorIndex <= 0) {
            return;
        }
        value = value.substring(0, cursorIndex - 1) + value.substring(cursorIndex);
        cursorIndex--;
        preferredColumn = -1;
        ensureCursorVisible();
        responder.accept(value);
    }

    private void deleteForward() {
        if (cursorIndex >= value.length()) {
            return;
        }
        value = value.substring(0, cursorIndex) + value.substring(cursorIndex + 1);
        ensureCursorVisible();
        responder.accept(value);
    }

    private void moveHorizontal(int delta) {
        cursorIndex = Math.max(0, Math.min(value.length(), cursorIndex + delta));
        preferredColumn = -1;
        ensureCursorVisible();
    }

    private void moveVertical(int delta) {
        CursorPosition cursorPosition = getCursorPosition();
        List<String> lines = getLines();
        int targetLine = Math.max(0, Math.min(lines.size() - 1, cursorPosition.line + delta));
        int targetColumn = preferredColumn >= 0 ? preferredColumn : cursorPosition.column;
        preferredColumn = targetColumn;
        cursorIndex = absoluteIndex(targetLine, Math.min(lines.get(targetLine).length(), targetColumn));
        ensureCursorVisible();
    }

    private void moveToLineEdge(boolean start) {
        CursorPosition cursorPosition = getCursorPosition();
        cursorIndex = absoluteIndex(cursorPosition.line, start ? 0 : getLines().get(cursorPosition.line).length());
        preferredColumn = -1;
        ensureCursorVisible();
    }

    private void ensureCursorVisible() {
        CursorPosition cursorPosition = getCursorPosition();
        int visibleLines = visibleLineCount();
        if (cursorPosition.line < scrollOffset) {
            scrollOffset = cursorPosition.line;
        } else if (cursorPosition.line >= scrollOffset + visibleLines) {
            scrollOffset = cursorPosition.line - visibleLines + 1;
        }
    }

    private int cursorIndexForPosition(double mouseX, double mouseY) {
        List<String> lines = getLines();
        int line = Math.max(0, Math.min(lines.size() - 1, scrollOffset + (int) ((mouseY - getY() - 6) / font.lineHeight)));
        String lineText = lines.get(line);
        int relativeX = Math.max(0, (int) mouseX - getX() - 6);
        int column = 0;
        while (column < lineText.length() && font.width(lineText.substring(0, column + 1)) <= relativeX) {
            column++;
        }
        return absoluteIndex(line, column);
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

    private int absoluteIndex(int line, int column) {
        List<String> lines = getLines();
        int index = 0;
        for (int lineIndex = 0; lineIndex < line; lineIndex++) {
            index += lines.get(lineIndex).length() + 1;
        }
        return index + column;
    }

    private List<String> getLines() {
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
        return Math.max(1, (height - 12) / font.lineHeight);
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

    private record CursorPosition(int line, int column) {
    }
}