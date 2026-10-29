package com.minhphuc.weapons.init;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY_WEAPONS = "key.category.weapons";
    public static final String KEY_SELECT_BULLET = "key.weapons.select_bullet";
    public static final String KEY_BEELZEBUTH = "key.weapons.cast_beelzebuth";
    public static final String KEY_TENGAI_SLASH = "key.weapons.tengai_slash";

    public static final KeyMapping SELECT_BULLET_KEY = new KeyMapping(
            KEY_SELECT_BULLET,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PAGE_UP,
            KEY_CATEGORY_WEAPONS
    );

    public static final KeyMapping BEELZEBUTH_KEY = new KeyMapping(
            KEY_BEELZEBUTH,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            KEY_CATEGORY_WEAPONS
    );

    public static final KeyMapping TENGAI_SLASH_KEY = new KeyMapping(
            KEY_TENGAI_SLASH,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KEY_CATEGORY_WEAPONS
    );

    public static void register() {
        KeyMappingRegistry.register(SELECT_BULLET_KEY);
        KeyMappingRegistry.register(BEELZEBUTH_KEY);
        KeyMappingRegistry.register(TENGAI_SLASH_KEY);
    }
}
