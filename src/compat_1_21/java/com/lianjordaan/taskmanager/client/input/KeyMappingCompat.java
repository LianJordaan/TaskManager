package com.lianjordaan.taskmanager.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

final class KeyMappingCompat {
    private KeyMappingCompat() {
    }

    static KeyMapping create(String translationKey, int keyCode, String category) {
        return new KeyMapping(translationKey, InputConstants.Type.KEYSYM, keyCode, KeyMapping.Category.MISC);
    }
}