package com.lianjordaan.taskmanager.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownRenderer {
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern LIST_PATTERN = Pattern.compile("^(\\s*)([-*+]\\s+|\\d+\\.\\s+)(.*)$");
    private static final Pattern QUOTE_PATTERN = Pattern.compile("^>\\s?(.*)$");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)]\\((.+?)\\)");

    private MarkdownRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, Font font, String markdown, int x, int y, int width, int maxHeight, int textAlpha) {
        int drawY = y;
        for (RenderedLine line : layout(font, markdown, width)) {
            if (drawY + font.lineHeight > y + maxHeight) {
                return;
            }

            if (line.code) {
                guiGraphics.fill(x + line.indent - 2, drawY - 1, x + width - 2, drawY + font.lineHeight + 1, withAlpha(0x243245, textAlpha));
            }
            if (line.quote) {
                guiGraphics.fill(x + line.indent - 5, drawY - 1, x + line.indent - 3, drawY + font.lineHeight + 1, withAlpha(0xE2B366, textAlpha));
            }

            guiGraphics.drawString(font, line.text, x + line.indent, drawY, withAlpha(line.color, textAlpha), false);
            drawY += font.lineHeight + (line.heading ? 2 : 0);
        }
    }

    public static int measureHeight(Font font, String markdown, int width) {
        int height = 0;
        for (RenderedLine line : layout(font, markdown, width)) {
            height += font.lineHeight + (line.heading ? 2 : 0);
        }
        return height;
    }

    private static List<RenderedLine> layout(Font font, String markdown, int width) {
        List<RenderedLine> renderedLines = new ArrayList<>();
        String[] sourceLines = markdown == null ? new String[]{""} : markdown.split("\\R", -1);
        boolean inCodeBlock = false;

        for (String rawLine : sourceLines) {
            String trimmed = rawLine.trim();
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            if (inCodeBlock) {
                appendWrapped(font, renderedLines, sanitizeInline(rawLine), width, 0, 0xFFD0D6E0, false, true, false);
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(rawLine);
            if (headingMatcher.matches()) {
                int level = headingMatcher.group(1).length();
                int color = switch (level) {
                    case 1 -> 0xFFF6F4D9;
                    case 2 -> 0xFFE7F1FF;
                    case 3 -> 0xFFD6F6EA;
                    default -> 0xFFE6E8F0;
                };
                appendWrapped(font, renderedLines, sanitizeInline(headingMatcher.group(2)), width, 0, color, true, false, false);
                continue;
            }

            Matcher listMatcher = LIST_PATTERN.matcher(rawLine);
            if (listMatcher.matches()) {
                appendWrapped(font, renderedLines, "• " + sanitizeInline(listMatcher.group(3)), width, 10, 0xFFD8E2EE, false, false, false);
                continue;
            }

            Matcher quoteMatcher = QUOTE_PATTERN.matcher(rawLine);
            if (quoteMatcher.matches()) {
                appendWrapped(font, renderedLines, sanitizeInline(quoteMatcher.group(1)), width, 10, 0xFFF1D6A4, false, false, true);
                continue;
            }

            if (trimmed.matches("^-{3,}$")) {
                appendWrapped(font, renderedLines, "────────", width, 0, 0xFF8FA3B9, false, false, false);
                continue;
            }

            appendWrapped(font, renderedLines, sanitizeInline(rawLine), width, 0, 0xFFE7ECF5, false, false, false);
        }

        if (renderedLines.isEmpty()) {
            renderedLines.add(new RenderedLine("", 0xFFE7ECF5, 0, false, false, false));
        }
        return renderedLines;
    }

    private static void appendWrapped(
        Font font,
        List<RenderedLine> target,
        String text,
        int width,
        int indent,
        int color,
        boolean heading,
        boolean code,
        boolean quote
    ) {
        int usableWidth = Math.max(24, width - indent);
        List<String> wrappedLines = wrapText(font, text.isBlank() ? " " : text, usableWidth);
        for (String wrappedLine : wrappedLines) {
            target.add(new RenderedLine(wrappedLine, color, indent, heading, code, quote));
        }
    }

    private static List<String> wrapText(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text.isBlank()) {
            lines.add("");
            return lines;
        }

        String remaining = text;
        while (!remaining.isEmpty()) {
            if (font.width(remaining) <= width) {
                lines.add(remaining);
                break;
            }

            int splitIndex = findWrapIndex(font, remaining, width);
            lines.add(remaining.substring(0, splitIndex).trim());
            remaining = remaining.substring(splitIndex).trim();
        }

        return lines;
    }

    private static int findWrapIndex(Font font, String text, int width) {
        int candidate = text.length();
        while (candidate > 1 && font.width(text.substring(0, candidate)) > width) {
            candidate--;
        }

        int lastSpace = text.substring(0, candidate).lastIndexOf(' ');
        if (lastSpace > 6) {
            return lastSpace;
        }
        return Math.max(1, candidate);
    }

    private static String sanitizeInline(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text == null ? "" : text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1)));
        }
        matcher.appendTail(buffer);
        return buffer.toString()
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace("*", "")
            .replace("_", "");
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private record RenderedLine(String text, int color, int indent, boolean heading, boolean code, boolean quote) {
    }
}