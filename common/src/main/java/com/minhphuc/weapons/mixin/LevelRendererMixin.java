package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void weapons$onRenderSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        if (ClientOrbitalCinematicHandler.isActive()) {
            float age = ClientOrbitalCinematicHandler.getAge(partialTick);
            if (age >= 0 && age < 28.0f) {
                // Hủy bầu trời ban ngày của Minecraft khi đang ngoài quỹ đạo vũ trụ
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void weapons$onRenderClouds(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        if (ClientOrbitalCinematicHandler.isActive()) {
            float age = ClientOrbitalCinematicHandler.getAge(partialTick);
            if (age >= 0 && age < 28.0f) {
                // Hủy các đám mây phẳng vanilla Minecraft khi nhìn từ vũ trụ xuống
                ci.cancel();
            }
        }
    }
}
