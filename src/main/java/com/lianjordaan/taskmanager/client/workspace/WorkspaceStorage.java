package com.lianjordaan.taskmanager.client.workspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lianjordaan.taskmanager.TaskManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class WorkspaceStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path baseDirectory = FabricLoader.getInstance().getGameDir().resolve("config").resolve(TaskManager.MOD_ID);
    private final Path contextDirectory = baseDirectory.resolve("contexts");

    public List<WorkspaceNote> loadGlobalNotes() {
        return loadNotes(baseDirectory.resolve("global-workspace.json"), WorkspaceScope.GLOBAL);
    }

    public List<WorkspaceNote> loadContextNotes(WorkspaceContext context) {
        return loadNotes(contextDirectory.resolve(sanitizeFragment(context.getKey()) + ".json"), WorkspaceScope.CONTEXT);
    }

    public void saveGlobalNotes(List<WorkspaceNote> notes) {
        saveNotes(baseDirectory.resolve("global-workspace.json"), "global", "Global workspace", notes);
    }

    public void saveContextNotes(WorkspaceContext context, List<WorkspaceNote> notes) {
        saveNotes(contextDirectory.resolve(sanitizeFragment(context.getKey()) + ".json"), context.getKey(), context.getLabel(), notes);
    }

    public static String sanitizeFragment(String value) {
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() > 96) {
            sanitized = sanitized.substring(0, 96);
        }
        if (sanitized.isEmpty()) {
            return "workspace";
        }
        return sanitized;
    }

    private List<WorkspaceNote> loadNotes(Path path, WorkspaceScope fallbackScope) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StoredWorkspace workspace = GSON.fromJson(reader, StoredWorkspace.class);
            if (workspace == null || workspace.notes == null) {
                return new ArrayList<>();
            }

            List<WorkspaceNote> notes = new ArrayList<>();
            for (StoredNote storedNote : workspace.notes) {
                WorkspaceScope scope = fallbackScope;
                if (storedNote.scope != null) {
                    try {
                        scope = WorkspaceScope.valueOf(storedNote.scope);
                    } catch (IllegalArgumentException ignored) {
                        scope = fallbackScope;
                    }
                }

                notes.add(new WorkspaceNote(
                    parseUuid(storedNote.id),
                    scope,
                    storedNote.title,
                    storedNote.content,
                    storedNote.x,
                    storedNote.y,
                    storedNote.width,
                    storedNote.height,
                    storedNote.scale <= 0.0F ? 1.0F : storedNote.scale,
                    storedNote.hidden,
                    storedNote.locked,
                    storedNote.zIndex,
                    storedNote.tintColor == null ? WorkspaceNote.defaultTintColor(scope) : storedNote.tintColor,
                    storedNote.cardOpacity == null ? WorkspaceNote.defaultCardOpacity() : storedNote.cardOpacity
                ));
            }
            return notes;
        } catch (IOException exception) {
            TaskManager.LOGGER.warn("Failed to load workspace notes from {}", path, exception);
            return new ArrayList<>();
        }
    }

    private void saveNotes(Path path, String workspaceKey, String workspaceLabel, List<WorkspaceNote> notes) {
        try {
            Files.createDirectories(path.getParent());

            StoredWorkspace workspace = new StoredWorkspace();
            workspace.version = 3;
            workspace.workspaceKey = workspaceKey;
            workspace.workspaceLabel = workspaceLabel;
            workspace.notes = new ArrayList<>();

            for (WorkspaceNote note : notes) {
                StoredNote storedNote = new StoredNote();
                storedNote.id = note.getId().toString();
                storedNote.scope = note.getScope().name();
                storedNote.title = note.getTitle();
                storedNote.content = note.getContent();
                storedNote.x = note.getX();
                storedNote.y = note.getY();
                storedNote.width = note.getWidth();
                storedNote.height = note.getHeight();
                storedNote.scale = note.getScale();
                storedNote.hidden = note.isHidden();
                storedNote.locked = note.isLocked();
                storedNote.zIndex = note.getZIndex();
                storedNote.tintColor = note.getTintColor();
                storedNote.cardOpacity = note.getCardOpacity();
                workspace.notes.add(storedNote);
            }

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(workspace, writer);
            }
        } catch (IOException exception) {
            TaskManager.LOGGER.warn("Failed to save workspace notes to {}", path, exception);
        }
    }

    private static UUID parseUuid(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }

    private static final class StoredWorkspace {
        private int version;
        private String workspaceKey;
        private String workspaceLabel;
        private List<StoredNote> notes;
    }

    private static final class StoredNote {
        private String id;
        private String scope;
        private String title;
        private String content;
        private float x;
        private float y;
        private float width;
        private float height;
        private float scale;
        private boolean hidden;
        private boolean locked;
        private long zIndex;
        private Integer tintColor;
        private Float cardOpacity;
    }
}