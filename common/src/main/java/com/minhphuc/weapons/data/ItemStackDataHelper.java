package com.minhphuc.weapons.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ItemStackDataHelper {

    public static int getInt(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getInt(key);
        }
        return 0;
    }

    public static void putInt(ItemStack stack, String key, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, value));
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getBoolean(key);
        }
        return false;
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(key, value));
    }

    public static String getString(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getString(key);
        }
        return "";
    }

    public static void putString(ItemStack stack, String key, String value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(key, value));
    }

    public static float getFloat(ItemStack stack, String key, float defaultValue) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains(key)) {
            return customData.copyTag().getFloat(key);
        }
        return defaultValue;
    }

    public static void putFloat(ItemStack stack, String key, float value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(key, value));
    }

    public static long getLong(ItemStack stack, String key, long defaultValue) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains(key)) {
            return customData.copyTag().getLong(key);
        }
        return defaultValue;
    }

    public static void putLong(ItemStack stack, String key, long value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(key, value));
    }

    public static boolean hasKey(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().contains(key);
    }

    public static void remove(ItemStack stack, String key) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(key));
    }
}
