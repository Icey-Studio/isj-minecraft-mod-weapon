package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ẩn hoàn toàn cánh tay và vật phẩm cầm tay ở góc nhìn thứ nhất (First-Person Hand & Held Item)
 * khi camera đang ở chế độ điện ảnh (Cinematic Camera) hoặc khi Camera bị tách rời (Detached).
 * Giúp khung hình phim tuyệt kỹ điện ảnh luôn trong trẻo, không bị bàn tay lơ lửng che tầm nhìn.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void weapons$hideHandsDuringCinematic(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer player, int light, CallbackInfo ci) {
        if (ClientCinematicSkillHandler.isActive()
                || ClientHellflareCinematicHandler.isActive()
                || ClientAzathothCinematicHandler.isActive()
                || ClientNihilityCinematicHandler.isActive()
                || ClientOrbitalCinematicHandler.isActive()
                || (Minecraft.getInstance().gameRenderer != null
                && Minecraft.getInstance().gameRenderer.getMainCamera() != null
                && Minecraft.getInstance().gameRenderer.getMainCamera().isDetached())) {
            ci.cancel();
        }
    }
}
