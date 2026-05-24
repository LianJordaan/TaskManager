package com.lianjordaan.taskmanager.fabric;

import com.lianjordaan.taskmanager.TaskManager;
import net.fabricmc.api.ModInitializer;

public final class TaskManagerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TaskManager.init();
    }
}
