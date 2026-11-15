package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.minhphuc.weapons.client.renderer.UnderworldSkyRenderer;
import com.minhphuc.weapons.content.tensura.WeatherBarrierTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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
                return;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (UnderworldSkyRenderer.isUnderworld(mc.level)) {
            UnderworldSkyRenderer.renderUnderworldSky(frustumMatrix, partialTick, camera);
            ci.cancel();
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

    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void weapons$onTickRain(Camera camera, CallbackInfo ci) {
        if (WeatherBarrierTracker.isClientCameraProtected()) {
            ci.cancel();
        }
    }
}
