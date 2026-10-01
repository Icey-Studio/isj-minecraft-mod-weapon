package com.minhphuc.weapons.client.gui;

import net.minecraft.client.Minecraft;

/**
 * Helper cô lập lệnh mở màn hình Client để tránh crash NoClassDefFoundError trên Dedicated Server.
 */
public class ClientPrimordialGuiOpener {
    public static void openSummonScreen() {
        Minecraft.getInstance().setScreen(new PrimordialSummonScreen());
    }

    public static void openRebirthScreen() {
        Minecraft.getInstance().setScreen(new PrimordialRebirthSelectScreen());
    }
}
