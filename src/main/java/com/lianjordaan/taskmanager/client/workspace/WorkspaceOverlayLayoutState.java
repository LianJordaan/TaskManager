package com.lianjordaan.taskmanager.client.workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WorkspaceOverlayLayoutState {
    private final EnumMap<WorkspaceOverlayPanel, PanelLayout> panels = new EnumMap<>(WorkspaceOverlayPanel.class);
    private boolean snapToGrid = true;
    private boolean noteDrawerOpen;
    private boolean topBarCollapsed;
    private long nextPanelZIndex = 1L;

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    public boolean isNoteDrawerOpen() {
        return noteDrawerOpen;
    }

    public void setNoteDrawerOpen(boolean noteDrawerOpen) {
        this.noteDrawerOpen = noteDrawerOpen;
    }

    public boolean isTopBarCollapsed() {
        return topBarCollapsed;
    }

    public void setTopBarCollapsed(boolean topBarCollapsed) {
        this.topBarCollapsed = topBarCollapsed;
    }

    public PanelLayout getPanel(WorkspaceOverlayPanel panel) {
        return panels.get(panel);
    }

    public PanelLayout ensurePanel(WorkspaceOverlayPanel panel, int x, int y, int width, int height) {
        PanelLayout layout = panels.get(panel);
        if (layout == null) {
            layout = new PanelLayout(x, y, width, height, nextPanelZIndex++);
            panels.put(panel, layout);
            return layout;
        }

        if (layout.getWidth() <= 0) {
            layout.setWidth(width);
        }
        if (layout.getHeight() <= 0) {
            layout.setHeight(height);
        }
        if (layout.getZIndex() <= 0L) {
            layout.setZIndex(nextPanelZIndex++);
        }
        return layout;
    }

    public void setPanel(WorkspaceOverlayPanel panel, PanelLayout layout) {
        if (layout != null) {
            panels.put(panel, layout);
            nextPanelZIndex = Math.max(nextPanelZIndex, layout.getZIndex() + 1L);
        }
    }

    public void bringToFront(WorkspaceOverlayPanel panel) {
        PanelLayout layout = panels.get(panel);
        if (layout == null) {
            return;
        }
        layout.setZIndex(nextPanelZIndex++);
    }

    public List<WorkspaceOverlayPanel> orderedPanels() {
        List<Map.Entry<WorkspaceOverlayPanel, PanelLayout>> ordered = new ArrayList<>(panels.entrySet());
        ordered.sort(Comparator.comparingLong(entry -> entry.getValue().getZIndex()));

        List<WorkspaceOverlayPanel> result = new ArrayList<>(ordered.size());
        for (Map.Entry<WorkspaceOverlayPanel, PanelLayout> entry : ordered) {
            result.add(entry.getKey());
        }
        return result;
    }

    public long getNextPanelZIndex() {
        return nextPanelZIndex;
    }

    public void setNextPanelZIndex(long nextPanelZIndex) {
        this.nextPanelZIndex = Math.max(1L, nextPanelZIndex);
    }

    public static final class PanelLayout {
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean minimized;
        private long zIndex;

        public PanelLayout() {
        }

        public PanelLayout(int x, int y, int width, int height, long zIndex) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.zIndex = zIndex;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public long getZIndex() {
            return zIndex;
        }

        public void setZIndex(long zIndex) {
            this.zIndex = zIndex;
        }

        public boolean isMinimized() {
            return minimized;
        }

        public void setMinimized(boolean minimized) {
            this.minimized = minimized;
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
        }

        public boolean headerContains(double mouseX, double mouseY, int headerHeight) {
            return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= y + headerHeight;
        }
    }
}