package com.minhphuc.weapons.client.gui;

import net.minecraft.client.Minecraft;

public class ClientSkillEvolutionOpener {
    public static void openScreen() {
        Minecraft.getInstance().setScreen(new SkillEvolutionScreen());
    }
}
