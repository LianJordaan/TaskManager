package com.lianjordaan.taskmanager;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskManager implements ModInitializer {
    public static final String MOD_ID = "taskmanager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("TaskManager initialized.");
    }
}