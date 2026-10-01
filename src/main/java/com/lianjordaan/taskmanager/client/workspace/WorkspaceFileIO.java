package com.lianjordaan.taskmanager.client.workspace;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.lianjordaan.taskmanager.TaskManager;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

/** Keeps the last complete JSON file available if a save or a later load fails. */
final class WorkspaceFileIO {
    private WorkspaceFileIO() {
    }

    static <T> T read(Path path, Gson gson, Class<T> type, Predicate<T> valid) {
        for (Path candidate : new Path[] {path, backupPath(path)}) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try (Reader reader = Files.newBufferedReader(candidate, StandardCharsets.UTF_8)) {
                T value = gson.fromJson(reader, type);
                if (valid.test(value)) {
                    if (!candidate.equals(path)) {
                        TaskManager.LOGGER.warn("Recovered workspace data from {}", candidate);
                    }
                    return value;
                }
                TaskManager.LOGGER.warn("Workspace data in {} is incomplete; trying the backup", candidate);
            } catch (IOException | JsonParseException exception) {
                TaskManager.LOGGER.warn("Failed to load workspace data from {}", candidate, exception);
            }
        }
        return null;
    }

    static <T> void write(Path path, Gson gson, Object value, Class<T> type, Predicate<T> valid) throws IOException {
        Path parent = path.getParent();
        Files.createDirectories(parent);
        String json = gson.toJson(value);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString() + ".", ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            if (Files.isRegularFile(path)) {
                if (containsValidData(path, gson, type, valid)) {
                    Path backupTemporary = Files.createTempFile(parent,
                        path.getFileName().toString() + ".backup.", ".tmp");
                    try {
                        Files.copy(path, backupTemporary, StandardCopyOption.REPLACE_EXISTING);
                        replace(backupTemporary, backupPath(path));
                    } finally {
                        Files.deleteIfExists(backupTemporary);
                    }
                } else {
                    Path damagedCopy = Files.createTempFile(parent,
                        path.getFileName().toString() + ".", ".corrupt");
                    Files.copy(path, damagedCopy, StandardCopyOption.REPLACE_EXISTING);
                    TaskManager.LOGGER.warn("Preserved incomplete workspace data as {}", damagedCopy);
                }
            }
            replace(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static <T> boolean containsValidData(Path path, Gson gson, Class<T> type,
                                                 Predicate<T> valid) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return valid.test(gson.fromJson(reader, type));
        } catch (JsonParseException exception) {
            TaskManager.LOGGER.warn("Preserving the last workspace backup because {} is malformed", path, exception);
            return false;
        }
    }

    private static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName().toString() + ".bak");
    }

    private static void replace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
