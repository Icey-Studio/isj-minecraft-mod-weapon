package com.minhphuc.weapons.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("setText")
    void weapons$setText(Component text);

    @Invoker("setBackgroundColor")
    void weapons$setBackgroundColor(int color);
}
