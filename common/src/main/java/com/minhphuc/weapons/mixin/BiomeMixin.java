package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.content.tensura.WeatherBarrierTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void weapons$onGetPrecipitationAt(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (WeatherBarrierTracker.isPosWeatherProtected(pos)) {
            cir.setReturnValue(Biome.Precipitation.NONE);
        }
    }

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
    private void weapons$onShouldFreeze(LevelReader level, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level lvl && WeatherBarrierTracker.isWeatherProtected(lvl, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void weapons$onShouldSnow(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level lvl && WeatherBarrierTracker.isWeatherProtected(lvl, pos)) {
            cir.setReturnValue(false);
        }
    }
}
