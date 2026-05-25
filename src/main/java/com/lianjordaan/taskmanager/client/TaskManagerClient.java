package com.lianjordaan.taskmanager.client;

import com.lianjordaan.taskmanager.TaskManager;
import com.lianjordaan.taskmanager.client.hud.HudRenderCompat;
import com.lianjordaan.taskmanager.client.hud.WorkspaceHudRenderer;
import com.lianjordaan.taskmanager.client.input.WorkspaceKeybindings;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import net.fabricmc.api.ClientModInitializer;

public final class TaskManagerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorkspaceManager.getInstance().initialize();
        WorkspaceKeybindings.initialize();
        HudRenderCompat.register(new WorkspaceHudRenderer());
        TaskManager.LOGGER.info("TaskManager client initialized.");
    }
}