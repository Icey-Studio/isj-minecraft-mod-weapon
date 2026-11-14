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
        if (ClientIaiHandler.isAstralPlayer(livingEntity)
                || com.minhphuc.weapons.client.ClientCinematicSkillHandler.isEntityHiddenForTeleportArrival(livingEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void weapons$hideLivingEntityDuringTeleport(T livingEntity, float entityYaw, float partialTicks,
                                                        com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                        net.minecraft.client.renderer.MultiBufferSource buffer,
                                                        int packedLight, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.minhphuc.weapons.client.ClientCinematicSkillHandler.isEntityHiddenForTeleportArrival(livingEntity)) {
            ci.cancel();
        }
    }
}

