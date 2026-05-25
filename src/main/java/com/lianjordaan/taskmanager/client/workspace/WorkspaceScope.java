package com.lianjordaan.taskmanager.client.workspace;

public enum WorkspaceScope {
    GLOBAL,
    CONTEXT;

    public String getDisplayName() {
        return this == GLOBAL ? "Global" : "Context";
    }
}