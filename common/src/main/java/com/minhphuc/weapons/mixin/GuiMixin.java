package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.client.CustomImpactFrameOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void weapons$onRenderGui(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        CustomImpactFrameOverlay.render(guiGraphics, partialTick);
        ClientNihilityCinematicHandler.renderCinematicOverlay(guiGraphics, partialTick);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void weapons$hideCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ClientNihilityCinematicHandler.isActive()) {
            ci.cancel();
        }
    }
}
