package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.renderer.AtomicPostFx;
import com.minhphuc.weapons.client.renderer.AtomicRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void weapons$onRenderLevel(PoseStack poseStack, float partialTick, long finishTimeNano,
                                       boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                       LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AtomicRenderer.render(poseStack, camera, partialTick);
        AtomicPostFx.capture(poseStack.last().pose(), projectionMatrix, partialTick);
        AtomicPostFx.renderAfterWorld();
    }
}
