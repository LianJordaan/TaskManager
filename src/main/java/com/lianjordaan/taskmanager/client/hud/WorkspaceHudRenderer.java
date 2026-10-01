package com.lianjordaan.taskmanager.client.hud;

import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import net.minecraft.client.Minecraft;

public final class WorkspaceHudRenderer {
    private final WorkspaceManager workspaceManager = WorkspaceManager.getInstance();

    public void render(Minecraft client, Object guiGraphics, float partialTick) {
        if (client == null || client.player == null || WorkspaceScreenCompat.currentScreen(client) != null) {
            return;
        }

        workspaceManager.refreshContext(client);
        for (WorkspaceNote note : workspaceManager.getCombinedNotes(false)) {
            WorkspaceNoteRenderer.render(guiGraphics, client.font, note, false, false);
        }
    }
}
