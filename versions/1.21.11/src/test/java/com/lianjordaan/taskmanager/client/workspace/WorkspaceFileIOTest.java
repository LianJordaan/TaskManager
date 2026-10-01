package com.lianjordaan.taskmanager.client.workspace;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorkspaceFileIOTest {
    private final Gson gson = new Gson();

    @TempDir
    Path directory;

    @Test
    void fallsBackToLastCompleteFileAfterAnInterruptedWrite() throws Exception {
        Path path = directory.resolve("notes.json");
        WorkspaceFileIO.write(path, gson, new State("first note"), State.class,
            state -> state != null && state.value != null);
        WorkspaceFileIO.write(path, gson, new State("edited note"), State.class,
            state -> state != null && state.value != null);

        Files.writeString(path, "{\"value\":", StandardCharsets.UTF_8);
        State recovered = WorkspaceFileIO.read(path, gson, State.class, state -> state != null);
        assertEquals("first note", recovered.value);

        WorkspaceFileIO.write(path, gson, new State("restored note"), State.class,
            state -> state != null && state.value != null);
        assertEquals("restored note", WorkspaceFileIO.read(path, gson, State.class,
            state -> state != null).value);
        assertEquals("first note", WorkspaceFileIO.read(path.resolveSibling("notes.json.bak"),
            gson, State.class, state -> state != null).value);

        Files.writeString(path, "{}", StandardCharsets.UTF_8);
        WorkspaceFileIO.write(path, gson, new State("after incomplete JSON"), State.class,
            state -> state != null && state.value != null);
        assertEquals("first note", WorkspaceFileIO.read(path.resolveSibling("notes.json.bak"),
            gson, State.class, state -> state != null && state.value != null).value);
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(file -> file.getFileName().toString().endsWith(".tmp")));
        }
        try (var files = Files.list(directory)) {
            assertEquals(2, files.filter(file -> file.getFileName().toString().endsWith(".corrupt")).count());
        }
    }

    private static final class State {
        private String value;

        private State(String value) {
            this.value = value;
        }
    }
}
