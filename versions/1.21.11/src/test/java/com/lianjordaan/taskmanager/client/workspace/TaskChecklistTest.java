package com.lianjordaan.taskmanager.client.workspace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskChecklistTest {
    @Test
    void togglesOnlyTheSelectedTaskWithoutChangingOtherText() {
        String note = "# Plans\n- [ ] Build base\n- [x] Check roof\nOther note\n";
        assertEquals("# Plans\n- [x] Build base\n- [x] Check roof\nOther note\n",
            TaskChecklist.toggleLine(note, 1));
        assertEquals("# Plans\n- [ ] Build base\n- [ ] Check roof\nOther note\n",
            TaskChecklist.toggleLine(note, 2));
    }

    @Test
    void preservesWindowsLineEndingsAndIgnoresNonTasks() {
        String note = "Title\r\n* [ ] Task\r\n- Plain\r\n";
        assertEquals("Title\r\n* [x] Task\r\n- Plain\r\n", TaskChecklist.toggleLine(note, 1));
        assertEquals(note, TaskChecklist.toggleLine(note, 2));
        assertEquals(note, TaskChecklist.toggleLine(note, 99));
    }
}
