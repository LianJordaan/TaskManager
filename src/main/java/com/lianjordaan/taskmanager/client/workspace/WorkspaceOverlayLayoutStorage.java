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

public final class WorkspaceOverlayLayoutStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path = FabricLoader.getInstance().getGameDir().resolve("config").resolve(TaskManager.MOD_ID).resolve("overlay-layout.json");

    public WorkspaceOverlayLayoutState load() {
        if (!Files.exists(path)) {
            return new WorkspaceOverlayLayoutState();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StoredOverlayLayout storedLayout = GSON.fromJson(reader, StoredOverlayLayout.class);
            WorkspaceOverlayLayoutState state = new WorkspaceOverlayLayoutState();
            if (storedLayout == null) {
                return state;
            }

            state.setSnapToGrid(storedLayout.snapToGrid == null || storedLayout.snapToGrid);
            state.setNoteDrawerOpen(storedLayout.noteDrawerOpen != null && storedLayout.noteDrawerOpen);
            state.setNextPanelZIndex(storedLayout.nextPanelZIndex == null ? 1L : storedLayout.nextPanelZIndex);
            if (storedLayout.panels != null) {
                for (StoredPanel panel : storedLayout.panels) {
                    if (panel == null || panel.id == null) {
                        continue;
                    }
                    try {
                        WorkspaceOverlayPanel panelId = WorkspaceOverlayPanel.valueOf(panel.id);
                        state.setPanel(panelId, new WorkspaceOverlayLayoutState.PanelLayout(
                            panel.x == null ? 0 : panel.x,
                            panel.y == null ? 0 : panel.y,
                            panel.width == null ? 0 : panel.width,
                            panel.height == null ? 0 : panel.height,
                            panel.zIndex == null ? 0L : panel.zIndex
                        ));
                        WorkspaceOverlayLayoutState.PanelLayout layout = state.getPanel(panelId);
                        if (layout != null) {
                            layout.setMinimized(panel.minimized != null && panel.minimized);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return state;
        } catch (IOException exception) {
            TaskManager.LOGGER.warn("Failed to load overlay layout from {}", path, exception);
            return new WorkspaceOverlayLayoutState();
        }
    }

    public void save(WorkspaceOverlayLayoutState state) {
        try {
            Files.createDirectories(path.getParent());

            StoredOverlayLayout storedLayout = new StoredOverlayLayout();
            storedLayout.version = 2;
            storedLayout.snapToGrid = state.isSnapToGrid();
            storedLayout.noteDrawerOpen = state.isNoteDrawerOpen();
            storedLayout.nextPanelZIndex = state.getNextPanelZIndex();
            storedLayout.panels = new StoredPanel[WorkspaceOverlayPanel.values().length];

            int index = 0;
            for (WorkspaceOverlayPanel panel : WorkspaceOverlayPanel.values()) {
                WorkspaceOverlayLayoutState.PanelLayout layout = state.getPanel(panel);
                StoredPanel storedPanel = new StoredPanel();
                storedPanel.id = panel.name();
                if (layout != null) {
                    storedPanel.x = layout.getX();
                    storedPanel.y = layout.getY();
                    storedPanel.width = layout.getWidth();
                    storedPanel.height = layout.getHeight();
                    storedPanel.minimized = layout.isMinimized();
                    storedPanel.zIndex = layout.getZIndex();
                }
                storedLayout.panels[index++] = storedPanel;
            }

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(storedLayout, writer);
            }
        } catch (IOException exception) {
            TaskManager.LOGGER.warn("Failed to save overlay layout to {}", path, exception);
        }
    }

    private static final class StoredOverlayLayout {
        private int version;
        private Boolean snapToGrid;
        private Boolean noteDrawerOpen;
        private Long nextPanelZIndex;
        private StoredPanel[] panels;
    }

    private static final class StoredPanel {
        private String id;
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
        private Boolean minimized;
        private Long zIndex;
    }
}