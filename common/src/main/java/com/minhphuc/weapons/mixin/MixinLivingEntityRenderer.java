package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientIaiHandler;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity> {

    @Inject(method = "isBodyVisible", at = @At("HEAD"), cancellable = true)
    private void weapons$renderAstralSoulTranslucent(T livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientIaiHandler.isAstralPlayer(livingEntity)) {
            cir.setReturnValue(false);
        }
    }
}

