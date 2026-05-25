package com.lianjordaan.taskmanager.client.workspace;

import java.util.UUID;

public final class WorkspaceNote {
    private static final String EMPTY_SUMMARY = "Empty note";
    private static final int DEFAULT_GLOBAL_TINT = 0x58BFD7;
    private static final int DEFAULT_CONTEXT_TINT = 0xF0B96B;
    private static final float DEFAULT_CARD_OPACITY = 0.90F;
    private static final float MIN_CARD_OPACITY = 0.20F;

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
    private int tintColor;
    private float cardOpacity;

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
        this(id, scope, content, x, y, width, height, scale, hidden, locked, zIndex, defaultTintColor(scope), DEFAULT_CARD_OPACITY);
    }

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
        long zIndex,
        int tintColor,
        float cardOpacity
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
        this.tintColor = normalizeTintColor(tintColor, scope);
        this.cardOpacity = normalizeCardOpacity(cardOpacity);
    }

    public static int defaultTintColor(WorkspaceScope scope) {
        return scope == WorkspaceScope.GLOBAL ? DEFAULT_GLOBAL_TINT : DEFAULT_CONTEXT_TINT;
    }

    public static float defaultCardOpacity() {
        return DEFAULT_CARD_OPACITY;
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

    public int getTintColor() {
        return tintColor;
    }

    public void setTintColor(int tintColor) {
        this.tintColor = normalizeTintColor(tintColor, scope);
    }

    public float getCardOpacity() {
        return cardOpacity;
    }

    public void setCardOpacity(float cardOpacity) {
        this.cardOpacity = normalizeCardOpacity(cardOpacity);
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

    private static int normalizeTintColor(int tintColor, WorkspaceScope scope) {
        int normalized = tintColor & 0x00FFFFFF;
        return normalized == 0 ? defaultTintColor(scope) : normalized;
    }

    private static float normalizeCardOpacity(float cardOpacity) {
        if (cardOpacity <= 0.0F) {
            return DEFAULT_CARD_OPACITY;
        }
        return Math.max(MIN_CARD_OPACITY, Math.min(1.0F, cardOpacity));
    }
}