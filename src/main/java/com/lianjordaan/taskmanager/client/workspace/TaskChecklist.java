package com.lianjordaan.taskmanager.client.workspace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Edits Markdown checkboxes without changing the rest of a note. */
public final class TaskChecklist {
    private static final Pattern CHECKBOX = Pattern.compile("^(\\s*[-*+]\\s+\\[)([ xX])(\\].*)$", Pattern.DOTALL);

    private TaskChecklist() {
    }

    public static String toggleLine(String content, int lineIndex) {
        if (content == null || lineIndex < 0) {
            return content;
        }
        String[] lines = content.split("\\n", -1);
        if (lineIndex >= lines.length) {
            return content;
        }
        Matcher match = CHECKBOX.matcher(lines[lineIndex]);
        if (!match.matches()) {
            return content;
        }
        String next = match.group(2).equals(" ") ? "x" : " ";
        lines[lineIndex] = match.group(1) + next + match.group(3);
        return String.join("\n", lines);
    }
}
