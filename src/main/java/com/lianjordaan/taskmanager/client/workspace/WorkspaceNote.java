package com.lianjordaan.taskmanager.client.workspace;

import java.util.UUID;

public final class WorkspaceNote {
    private static final String EMPTY_SUMMARY = "Empty note";

    private final UUID id;
    private final WorkspaceScope scope;
    private String content;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scale;
    private boolean hidden;
    private boolean locked;
    private long zIndex;

    public WorkspaceNote(
        UUID id,
        WorkspaceScope scope,
        String content,
        float x,
        float y,
        float width,
        float height,
        float scale,
        boolean hidden,
        boolean locked,
        long zIndex
    ) {
        this.id = id;
        this.scope = scope;
        this.content = content == null ? "" : content;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.hidden = hidden;
        this.locked = locked;
        this.zIndex = zIndex;
    }

    public UUID getId() {
        return id;
    }

    public WorkspaceScope getScope() {
        return scope;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getZIndex() {
        return zIndex;
    }

    public void setZIndex(long zIndex) {
        this.zIndex = zIndex;
    }

    public int getRenderedWidth() {
        return Math.max(160, Math.round(width * scale));
    }

    public int getRenderedHeight() {
        return Math.max(96, Math.round(height * scale));
    }

    public String getSummary() {
        String[] lines = content.split("\\R");
        for (String line : lines) {
            String trimmed = line.replace("#", "").replace("*", "").replace("`", "").trim();
            if (!trimmed.isEmpty()) {
                return trimmed.length() > 34 ? trimmed.substring(0, 31) + "..." : trimmed;
            }
        }
        return EMPTY_SUMMARY;
    }
}