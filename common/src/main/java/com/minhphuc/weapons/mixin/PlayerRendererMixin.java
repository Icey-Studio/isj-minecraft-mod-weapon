package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientPossessionHandler;
import com.minhphuc.weapons.client.renderer.ClientPossessionRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void weapons$getPossessedDemonTexture(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        if (ClientPossessionHandler.isPossessing(player)) {
            ResourceLocation loc = ClientPossessionRenderer.getPossessedTexture(player);
            if (loc != null) {
                cir.setReturnValue(loc);
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void weapons$renderPossessedDemonF5(AbstractClientPlayer player, float entityYaw, float partialTicks,
                                               PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (ClientPossessionHandler.isPossessing(player)) {
            ClientPossessionRenderer.renderPossessedDemon((PlayerRenderer) (Object) this, player, entityYaw, partialTicks, poseStack, buffer, packedLight);
            ci.cancel();
        }
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void weapons$renderPossessedRightHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (ClientPossessionHandler.isPossessing(player)) {
            ClientPossessionRenderer.renderFirstPersonDemonHand(poseStack, buffer, packedLight, player, true);
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void weapons$renderPossessedLeftHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (ClientPossessionHandler.isPossessing(player)) {
            ClientPossessionRenderer.renderFirstPersonDemonHand(poseStack, buffer, packedLight, player, false);
            ci.cancel();
        }
    }
}
