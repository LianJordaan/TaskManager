package com.lianjordaan.taskmanager.client.hud;

import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.screen.WorkspaceOverlayScreen;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class WorkspaceHudRenderer {
    private final WorkspaceManager workspaceManager = WorkspaceManager.getInstance();

    public void render(Minecraft client, GuiGraphics guiGraphics, float partialTick) {
        if (client == null || client.player == null || client.screen != null) {
            return;
        }

        if (client.screen instanceof WorkspaceOverlayScreen) {
            return;
        }

        workspaceManager.refreshContext(client);
        for (WorkspaceNote note : workspaceManager.getCombinedNotes(false)) {
            WorkspaceNoteRenderer.render(guiGraphics, client.font, note, false, false);
        }
    }
}