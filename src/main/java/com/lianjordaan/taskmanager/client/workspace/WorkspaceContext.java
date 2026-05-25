package com.lianjordaan.taskmanager.client.workspace;

import java.util.Objects;

public final class WorkspaceContext {
    private static final WorkspaceContext NONE = new WorkspaceContext("none", "", "No active world or server", false);

    private final String type;
    private final String key;
    private final String label;
    private final boolean available;

    private WorkspaceContext(String type, String key, String label, boolean available) {
        this.type = type;
        this.key = key;
        this.label = label;
        this.available = available;
    }

    public static WorkspaceContext none() {
        return NONE;
    }

    public static WorkspaceContext singleplayer(String key, String label) {
        return new WorkspaceContext("singleplayer", key, label, true);
    }

    public static WorkspaceContext multiplayer(String key, String label) {
        return new WorkspaceContext("multiplayer", key, label, true);
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspaceContext workspaceContext)) {
            return false;
        }
        return available == workspaceContext.available
            && Objects.equals(type, workspaceContext.type)
            && Objects.equals(key, workspaceContext.key)
            && Objects.equals(label, workspaceContext.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, label, available);
    }
}