package com.minhphuc.weapons.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * CLIENT OPENGL ADDITIVE BLENDING RENDERER: HẮC LONG PLASMA HƯ KHÔNG (AZATHOTH DRAGON)
 * Render trực tiếp Cự Long Plasma bằng hình học GPU & Additive Blending chuẩn Lời Chúc Tử Vong / God Wand.
 * 100% Thuần Plasma phát quang, không sử dụng bất kỳ khối hay item Minecraft nào.
 */
public final class ClientAzathothDragonRenderer {

    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.fromNamespaceAndPath("weapons", "atomic_energy");

    private static ClientLevel world = null;
    private static UUID casterUuid = null;
    private static Vec3 startPos = Vec3.ZERO;
    private static Vec3 dir = Vec3.ZERO;
    private static Vec3 right = Vec3.ZERO;
    private static Vec3 up = Vec3.ZERO;
    private static int totalTicks = 35;
    private static long startWorldTick = 0;
    private static boolean active = false;

    public static void receive(UUID caster, Vec3 start, Vec3 lookDir, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        world = mc.level;
        casterUuid = caster;
        startPos = start;
        dir = lookDir.normalize();

        Vec3 r = dir.cross(new Vec3(0, 1, 0)).normalize();
        if (r.lengthSqr() < 0.01) {
            r = new Vec3(1, 0, 0);
        }
        right = r;
        up = right.cross(dir).normalize();

        totalTicks = duration;
        startWorldTick = world.getGameTime();
        active = true;
    }

    public static void reset() {
        active = false;
        world = null;
        casterUuid = null;
        startPos = Vec3.ZERO;
        dir = Vec3.ZERO;
    }

    public static boolean isActive() {
        return active;
    }

    public static double getAge(float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.level != world) return -1;
        long elapsed = mc.level.getGameTime() - startWorldTick;
        if (elapsed > totalTicks) return -1;
        return elapsed + delta;
    }

    public static void render(PoseStack matrices, Camera camera, float delta) {
        double age = getAge(delta);
        if (age < 0 || age >= totalTicks) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Cập nhật vị trí bắt đầu bám theo mắt người chơi nếu còn sống
        if (casterUuid != null) {
            var entity = mc.level.getPlayerByUUID(casterUuid);
            if (entity != null) {
                startPos = entity.getEyePosition(delta).add(entity.getLookAngle().scale(0.3));
                dir = entity.getLookAngle().normalize();
                Vec3 r = dir.cross(new Vec3(0, 1, 0)).normalize();
                if (r.lengthSqr() >= 0.01) {
                    right = r;
                    up = right.cross(dir).normalize();
                }
            }
        }

        ensureEnergyTexture();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        Vec3 camPos = camera.getPosition();
        matrices.pushPose();
        matrices.translate(startPos.x - camPos.x, startPos.y - camPos.y, startPos.z - camPos.z);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            emitDragon(mesh, mat, age);

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            matrices.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void emitDragon(VertexConsumer v, Matrix4f m, double age) {
        float maxRange = 50.0F;
        float currentLength;
        float scaleFactor;
        float alpha;
        float jawAngle;

        // Giai đoạn 1: Phóng rồng (0 -> 16 ticks)
        if (age < 16.0) {
            float progress = (float) (age / 16.0);
            currentLength = maxRange * Math.min(1.0F, progress * 1.5F);
            scaleFactor = 1.0F;
            alpha = (float) Math.min(1.0, age / 3.0);
            jawAngle = 20.0F;
        }
        // Giai đoạn 2: Thu hồi dạ dày hư không (16 -> 35 ticks)
        else {
            float retract = (float) ((age - 16.0) / 19.0);
            currentLength = (float) (maxRange * (1.0 - retract));
            scaleFactor = Math.max(0.05F, 1.0F - retract * 0.85F);
            alpha = Math.max(0.0F, 1.0F - retract * 0.9F);
            jawAngle = Math.max(0.0F, 20.0F * (1.0F - retract * 2.5F));
        }

        if (currentLength < 0.5F || alpha <= 0.001F) return;

        // ==============================================================
        // 1. LONG THÂN UỐN LƯỢN (Sinusoidal Plasma Dragon Spine)
        // ==============================================================
        int segments = 32;
        Vec3 prevPos = Vec3.ZERO;
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / (float) segments;
            double dist = t * currentLength;

            double wavePhase = (t * 7.5) - (age * 0.55);
            double waveR = Math.sin(wavePhase) * 1.8 * scaleFactor;
            double waveU = Math.cos(wavePhase) * 1.0 * scaleFactor;

            Vec3 currPos = dir.scale(dist).add(right.scale(waveR)).add(up.scale(waveU));

            if (i > 0) {
                // Thân rồng phình to ở giữa và thon dần về đầu/đuôi
                float taper = (float) Math.sin(t * Math.PI);
                float width = (0.8F + taper * 2.2F) * scaleFactor;

                // Cột Plasma 3 lớp: Lõi trắng + Vỏ tím cực hạn
                stroke(v, m, prevPos, currPos, width, alpha);

                // Các nhánh lôi điện Plasma xoáy quanh thân rồng
                if (i % 2 == 0) {
                    double arcSeed = (i * 0.618) + (age * 0.08);
                    double arcAngle = arcSeed * Math.PI * 2.0;
                    double arcR = (width * 1.4) + Math.sin(arcSeed * 12.0) * 0.4;
                    Vec3 arcTip = currPos.add(right.scale(Math.cos(arcAngle) * arcR)).add(up.scale(Math.sin(arcAngle) * arcR));
                    stroke(v, m, currPos, arcTip, width * 0.25F, alpha * 0.65F);
                }
            }
            prevPos = currPos;
        }

        // ==============================================================
        // 2. LONG THỦ (Dragon Head & Plasma Jaws)
        // ==============================================================
        Vec3 headBase = prevPos;
        double headWavePhase = 7.5 - (age * 0.55);
        Vec3 headDir = dir.scale(Math.cos(Math.sin(headWavePhase) * 0.15))
                .add(right.scale(Math.cos(headWavePhase) * 0.25))
                .normalize();

        // Hàm Trên (Upper Jaw Plasma)
        Vec3 upperJawDir = headDir.scale(Math.cos(Math.toRadians(jawAngle)))
                .add(up.scale(Math.sin(Math.toRadians(jawAngle))))
                .normalize();
        Vec3 upperJawEnd = headBase.add(upperJawDir.scale(12.0 * scaleFactor));
        stroke(v, m, headBase.add(up.scale(0.8 * scaleFactor)), upperJawEnd, 1.8F * scaleFactor, alpha * 1.2F);

        // Hàm Dưới (Lower Jaw Plasma)
        Vec3 lowerJawDir = headDir.scale(Math.cos(Math.toRadians(-jawAngle * 1.2)))
                .add(up.scale(Math.sin(Math.toRadians(-jawAngle * 1.2))))
                .normalize();
        Vec3 lowerJawEnd = headBase.add(lowerJawDir.scale(10.0 * scaleFactor));
        stroke(v, m, headBase.add(up.scale(-0.8 * scaleFactor)), lowerJawEnd, 1.6F * scaleFactor, alpha * 1.1F);

        // 2 Sừng Rồng Plasma vuốt ngược ra sau (Dragon Horns)
        Vec3 hornLDir = headDir.scale(-0.75).add(up.scale(0.65)).add(right.scale(-0.4)).normalize();
        Vec3 hornRDir = headDir.scale(-0.75).add(up.scale(0.65)).add(right.scale(0.4)).normalize();
        stroke(v, m, headBase.add(up.scale(1.2 * scaleFactor)).add(right.scale(-0.8 * scaleFactor)),
                headBase.add(hornLDir.scale(7.5 * scaleFactor)), 0.9F * scaleFactor, alpha * 0.9F);
        stroke(v, m, headBase.add(up.scale(1.2 * scaleFactor)).add(right.scale(0.8 * scaleFactor)),
                headBase.add(hornRDir.scale(7.5 * scaleFactor)), 0.9F * scaleFactor, alpha * 0.9F);

        // Mắt Rồng Cực Quang (Dragon Eyes)
        disc(v, m, headBase.add(up.scale(0.9 * scaleFactor)).add(right.scale(-1.1 * scaleFactor)), 0.9F * scaleFactor, 0xFFFFFF, alpha * 1.5F);
        disc(v, m, headBase.add(up.scale(0.9 * scaleFactor)).add(right.scale(1.1 * scaleFactor)), 0.9F * scaleFactor, 0xFFFFFF, alpha * 1.5F);

        // ==============================================================
        // 3. VÂY LƯNG & CÁNH NĂNG LƯỢNG (Dorsal Energy Wings & Fins)
        // ==============================================================
        double[] finDistances = {0.35, 0.65};
        for (double fd : finDistances) {
            double dist = fd * currentLength;
            double wavePhase = (fd * 7.5) - (age * 0.55);
            Vec3 finBase = dir.scale(dist).add(right.scale(Math.sin(wavePhase) * 1.8 * scaleFactor)).add(up.scale(Math.cos(wavePhase) * 1.0 * scaleFactor));

            // Vây trái và vây phải
            Vec3 wingL = right.scale(-3.5 * scaleFactor).add(up.scale(1.8 * scaleFactor));
            Vec3 wingR = right.scale(3.5 * scaleFactor).add(up.scale(1.8 * scaleFactor));
            stroke(v, m, finBase, finBase.add(wingL), 0.8F * scaleFactor, alpha * 0.7F);
            stroke(v, m, finBase, finBase.add(wingR), 0.8F * scaleFactor, alpha * 0.7F);
        }
    }

    private static void stroke(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, float alpha) {
        if (alpha <= 0.001F) return;
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1e-6) return;
        direction = direction.normalize();

        Vec3 side = direction.cross(Math.abs(direction.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 other = direction.cross(side).normalize();

        // 3 Tầng Plasma lồng nhau: Lõi trắng tuyền + Thân tím + Vành tím hồng hào quang
        for (int layer = 2; layer >= 0; layer--) {
            double w = width * (layer == 2 ? 3.8 : layer == 1 ? 1.8 : 0.8);
            float opacity = alpha * (layer == 2 ? 0.08F : layer == 1 ? 0.35F : 0.95F);
            int color = layer == 0 ? 0xF5DFFF : (layer == 1 ? 0xA550FF : 0x7C1AFF);

            for (int axis = 0; axis < 2; axis++) {
                Vec3 n = (axis == 0 ? side : other).scale(w);
                quad(v, m, a.subtract(n), a.add(n), b.add(n), b.subtract(n), color, opacity);
            }
        }
    }

    private static void disc(VertexConsumer v, Matrix4f m, Vec3 center, float radius, int color, float alpha) {
        if (alpha <= 0.001F) return;
        int opacity = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        float r = radius;

        Vec3 a = center.add(-r, -r, 0);
        Vec3 b = center.add(r, -r, 0);
        Vec3 c = center.add(r, r, 0);
        Vec3 d = center.add(-r, r, 0);

        energyVertex(v, m, a, color, opacity, 0.0078125F, 0.984375F);
        energyVertex(v, m, b, color, opacity, 0.4921875F, 0.984375F);
        energyVertex(v, m, c, color, opacity, 0.4921875F, 0.015625F);
        energyVertex(v, m, d, color, opacity, 0.0078125F, 0.015625F);
    }

    private static void quad(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        energyVertex(v, m, a, color, opacity, 0.875F, 0.5F);
        energyVertex(v, m, b, color, opacity, 0.875F, 0.5F);
        energyVertex(v, m, c, color, opacity, 0.875F, 0.5F);
        energyVertex(v, m, d, color, opacity, 0.875F, 0.5F);
    }

    private static void energyVertex(VertexConsumer v, Matrix4f m, Vec3 p, int color, int opacity, float u, float texV) {
        v.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, opacity)
                .setUv(u, texV);
    }

    private static void ensureEnergyTexture() {
        AtomicRenderer.ensureEnergyTexture();
    }
}
