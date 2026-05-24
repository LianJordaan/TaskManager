package com.lianjordaan.taskmanager.forge;

import com.lianjordaan.taskmanager.TaskManager;
import net.minecraftforge.fml.common.Mod;

@Mod(TaskManager.MOD_ID)
public final class TaskManagerForge {
    public TaskManagerForge() {
        TaskManager.init();
    }
}
