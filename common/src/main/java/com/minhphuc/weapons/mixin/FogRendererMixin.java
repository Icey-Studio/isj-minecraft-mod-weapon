package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupColor", at = @At("HEAD"), cancellable = true)
    private static void weapons$onSetupFogColor(Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
        if (ClientOrbitalCinematicHandler.isActive()) {
            float age = ClientOrbitalCinematicHandler.getAge(partialTicks);
            if (age >= 0 && age < 28.0f) {
                // Đen tuyệt đối trong vũ trụ (Pure Space Black)
                RenderSystem.clearColor(0.003F, 0.002F, 0.008F, 0.0F);
                ci.cancel();
            }
        }
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void weapons$onSetupFogDistance(Camera camera, FogRenderer.FogMode fogMode, float viewDistance, boolean thickFog, float partialTicks, CallbackInfo ci) {
        if (ClientOrbitalCinematicHandler.isActive()) {
            float age = ClientOrbitalCinematicHandler.getAge(partialTicks);
            if (age >= 0 && age < 28.0f) {
                // Triệt tiêu hoàn toàn sương mù trong chân không vũ trụ
                RenderSystem.setShaderFogStart(999999.0F);
                RenderSystem.setShaderFogEnd(1000000.0F);
                ci.cancel();
            }
        }
    }
}
