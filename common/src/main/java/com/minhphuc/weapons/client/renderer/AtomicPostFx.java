package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.mixin.PostChainAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Quản lý chuỗi Post-Processing Shaders (Bloom, Aberration, Volumetric Beam, Darkening)
 * mượn từ God Wand để mang lại chất lượng đồ họa đỉnh cao chuẩn anime.
 */
public final class AtomicPostFx {
    private record Frame(Matrix4f view, Matrix4f projection, float delta) {}
    private static Frame pending;
    private static PostChain chain;
    private static int width, height;
    private static boolean failed;

    public static void capture(Matrix4f view, Matrix4f projection, float delta) {
        pending = AtomicRenderer.nearestFrame(delta) != null
                ? new Frame(new Matrix4f(view), new Matrix4f(projection), delta) : null;
    }

    public static boolean available() { return chain != null && !failed; }

    public static void reset() { reload(); pending = null; }

    public static void reload() {
        if (chain != null) chain.close();
        chain = null; failed = false; width = height = 0;
    }

    public static void renderAfterWorld() {
        Frame context = pending; pending = null;
        if (context == null || failed) return;
        var fx = AtomicRenderer.nearestFrame(context.delta);
        var client = Minecraft.getInstance();
        if (fx == null || client.level == null) return;
        int w = client.getWindow().getWidth(), h = client.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;
        try {
            if (chain == null) chain = new PostChain(client.getTextureManager(), client.getResourceManager(), client.getMainRenderTarget(),
                    ResourceLocation.withDefaultNamespace("shaders/post/godwand_atomic.json"));
            if (w != width || h != height) {
                chain.resize(w, h);
                int bloomW = Math.max(1, w / 2);
                int bloomH = Math.max(1, h / 2);
                for (String name : new String[]{"atomic_emit", "atomic_blur_x", "atomic_blur_y"})
                    chain.getTempTarget(name).resize(bloomW, bloomH, false);
                for (var pass : ((PostChainAccessor) chain).weapons$getPasses())
                    pass.setOrthoMatrix(new Matrix4f().setOrtho(0, pass.outTarget.width, 0, pass.outTarget.height, .1f, 1000));
                width = w; height = h;
            }
            var camera = client.gameRenderer.getMainCamera().getPosition();
            var clip = new Vector4f((float)(fx.center().x - camera.x), (float)(fx.center().y + 2 - camera.y), (float)(fx.center().z - camera.z), 1)
                    .mul(context.view).mul(context.projection);
            float centerX = clip.w > .001 ? clip.x / clip.w * .5f + .5f : .5f, centerY = clip.w > .001 ? clip.y / clip.w * .5f + .5f : .5f;
            var inverseProjection = new Matrix4f(context.projection).invert();
            var inverseView = new Matrix4f(context.view).invert();
            int index = 0;
            for (var pass : ((PostChainAccessor) chain).weapons$getPasses()) {
                var effect = pass.getEffect();
                effect.safeGetUniform("EmissionOnly").set(index++ == 1 ? 1f : 0f);
                effect.safeGetUniform("InvProjMat").set(inverseProjection);
                effect.safeGetUniform("InvViewMat").set(inverseView);
                effect.safeGetUniform("CameraPos").set((float) camera.x, (float) camera.y, (float) camera.z);
                effect.safeGetUniform("BlastCenter").set((float) fx.center().x, (float) fx.center().y, (float) fx.center().z);
            }
            chain.setUniform("Age", fx.age());
            chain.setUniform("SequenceEnd", AtomicSequence.END);
            chain.setUniform("DetonationTick", AtomicSequence.DETONATION);
            chain.setUniform("ChargeProgress", AtomicSequence.charge(fx.age()));
            chain.setUniform("ReleaseFlash", AtomicSequence.releaseFlash(fx.age()));
            chain.setUniform("Compression", AtomicSequence.compression(fx.age()));
            chain.setUniform("FxFade", AtomicSequence.visibility(fx.age()));
            chain.setUniform("Cut", AtomicSequence.blackout(fx.age()));
            chain.setUniform("HoloRadius", AtomicSequence.holoRadius(fx.age()));
            chain.setUniform("BeamRadius", AtomicSequence.beamRadius(fx.age()));
            chain.setUniform("BeamHeight", AtomicSequence.beamHeight(fx.age()));
            chain.setUniform("BeamDepth", (float) AtomicSequence.BEAM_DEPTH);
            chain.setUniform("BlastPulse", AtomicSequence.blastPulse(fx.age()));
            chain.setUniform("ShockRadius", AtomicSequence.shockRadius(fx.age()));
            chain.setUniform("ObserverGain", AtomicRenderer.distanceGain(client.player != null ? client.player.position().distanceTo(fx.center()) : 0));
            chain.setUniform("ImpactPulse", AtomicSequence.impact(fx.age()) ? 1 : 0);
            chain.setUniform("ImpactFrames", 1);
            chain.setUniform("Darken", 1);
            chain.setUniform("CenterX", centerX); chain.setUniform("CenterY", centerY);
            chain.setUniform("BloomStrength", 1.2f);
            chain.setUniform("BloomBurst", fx.age() >= AtomicSequence.DETONATION && clip.w > 0
                    ? Math.max(0, 1 - (fx.age() - AtomicSequence.DETONATION) / 24) : 0);
            RenderSystem.disableBlend(); RenderSystem.disableDepthTest(); RenderSystem.resetTextureMatrix();
            chain.process(context.delta);
        } catch (Exception error) {
            failed = true;
            if (chain != null) { chain.close(); chain = null; }
        } finally {
            client.getMainRenderTarget().bindWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        }
    }
}
