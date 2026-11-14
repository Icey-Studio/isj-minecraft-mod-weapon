package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.content.tensura.WeatherBarrierTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow public abstract boolean isClientSide();

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
    private void weapons$onIsRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (WeatherBarrierTracker.isWeatherProtected(self, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("RETURN"), cancellable = true)
    private void weapons$onGetHeight(net.minecraft.world.level.levelgen.Heightmap.Types types, int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (types == net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING
                || types == net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES) {
            Level self = (Level) (Object) this;
            int roofY = WeatherBarrierTracker.getProtectedRoofY(self, x, z);
            if (roofY > cir.getReturnValue()) {
                cir.setReturnValue(roofY);
            }
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void weapons$onGetThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (this.isClientSide() && WeatherBarrierTracker.isClientCameraProtected()) {
            cir.setReturnValue(0.0F);
        }
    }
}
