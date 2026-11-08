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

/**
 * CLIENT OPENGL ADDITIVE BLENDING RENDERER: CỘT PLASMA SIÊU TÂN TINH (SUPERNOVA PLASMA PILLAR)
 * Render trực tiếp Cột Plasma Đa Tầng bằng hình học GPU & Additive Blending chuẩn Lời Chúc Tử Vong / God Wand.
 * 100% Thuần Plasma phát quang vô hạn xuyên mây trời (384m), 0 Entity Spam, 0 Lag.
 * Duy trì vĩnh viễn với các đĩa sóng xung kích và lôi điện cuộn trào cho đến khi giải trừ.
 */
public final class ClientSupernovaPlasmaRenderer {

    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.fromNamespaceAndPath("weapons", "atomic_energy");

    private static ClientLevel world = null;
    private static Vec3 groundCenter = Vec3.ZERO;
    private static long startWorldTick = 0;
    private static boolean active = false;

    public static void start(Vec3 groundPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        world = mc.level;
        groundCenter = groundPos;
        startWorldTick = world.getGameTime();
        active = true;
    }

    public static void reset() {
        active = false;
        world = null;
        groundCenter = Vec3.ZERO;
        startWorldTick = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static double getAge(float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.level != world) return -1;
        long elapsed = mc.level.getGameTime() - startWorldTick;
        return elapsed + delta;
    }

    public static void render(PoseStack matrices, Camera camera, float delta) {
        double age = getAge(delta);
        if (age < 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = camera.getPosition();
        if (camPos.distanceToSqr(groundCenter) > 380 * 380) return;

        AtomicRenderer.ensureEnergyTexture();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        matrices.pushPose();
        matrices.translate(groundCenter.x - camPos.x, groundCenter.y - camPos.y, groundCenter.z - camPos.z);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();
            Vec3 eyeRel = camPos.subtract(groundCenter);

            emitPlasmaColumn(mesh, mat, age, eyeRel);

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

    private static void emitPlasmaColumn(VertexConsumer v, Matrix4f m, double age, Vec3 eye) {
        // Animation Bung Nở Thiên Giáng (0 -> 15 ticks): Smoothstep expansion
        float height = 384.0F;
        float baseRadius = 14.0F;
        float expandProgress = (float) Math.min(1.0, age / 15.0);
        float ease = expandProgress * expandProgress * (3.0F - 2.0F * expandProgress);

        float currentRadius = Math.max(0.2F, baseRadius * ease);
        float currentHeight = Math.max(10.0F, height * ease);
        float fade = Math.min(1.0F, (float) (age / 3.0));

        // 1. LÕI TRẮNG CHÓI LÒA & VỎ TÍM HỒNG ĐA TẦNG (Dual Plasma Cylinders)
        cylinder(v, m, currentRadius * 0.72F, currentHeight, 0.85F * fade, 0xF5DFFF, age);
        cylinder(v, m, currentRadius, currentHeight, 0.35F * fade, 0x932BFF, age + 10);

        // 2. 40 NHÁNH LÔI ĐIỆN XOẮN DỌC THÂN CỘT PLASMA (Spiraling Lightning Tendrils)
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI / 20.0 + 0.006 * age;
            Vec3 last = polar(currentRadius * (0.88 + 0.08 * Math.sin(i * 1.5)), 0, a);
            for (int j = 1; j <= 6; j++) {
                double y = j * currentHeight / 6.0;
                double jitter = Math.sin(i * 18.23 + j * 8.3 + age * 0.15) * 0.05;
                Vec3 next = polar(currentRadius * (0.95 + jitter), y, a + jitter + (j * 0.08));
                stroke(v, m, last, next, 0.06F + (i % 4) * 0.02F, fade * (i % 3 == 0 ? 0.9F : 0.45F));
                last = next;
            }
        }

        // 3. ĐĨA SÓNG XUNG KÍCH MẶT ĐẤT & THIÊN GIÁNG (Ground & Cascading Shockwaves)
        if (age < 20.0) {
            // Sóng bộc phát ban đầu dội từ trên trời xuống
            double dropH1 = Math.max(0.5, 60.0 * (1.0 - ease));
            double dropH2 = Math.max(0.5, 90.0 * (1.0 - ease));
            ring(v, m, (float) dropH1, 28.0F * ease, 0.28F, fade * 0.85F);
            ring(v, m, (float) dropH2, 22.0F * ease, 0.22F, fade * 0.7F);
        } else {
            // Sóng xung kích cuộn trào tuần hoàn vô hạn (Cascading Shockwave Halos)
            int cycleTick1 = (int) (age - 20) % 60;
            double h1 = Math.max(0.5, 55.0 * (1.0 - (cycleTick1 / 60.0)));
            int cycleTick2 = ((int) (age - 20) + 30) % 60;
            double h2 = Math.max(0.5, 55.0 * (1.0 - (cycleTick2 / 60.0)));

            ring(v, m, (float) h1, 26.0F, 0.25F, 0.75F);
            ring(v, m, (float) h2, 20.0F, 0.20F, 0.65F);
        }

        // 4. CÁC VÒNG ÁP SUẤT TẠI MẶT ĐẤT (Ground Shock Rings)
        for (int i = 0; i < 3; i++) {
            double ringAge = (age + i * 20.0) % 60.0;
            float r = (float) (currentRadius + (ringAge / 60.0) * 48.0);
            float opacity = (float) (1.0 - (ringAge / 60.0)) * fade * 0.75F;
            ring(v, m, 0.2F + i * 0.15F, r, 0.22F, opacity);
            ring(v, m, 1.0F + i * 0.5F, r * 0.92F, 0.14F, opacity * 0.5F);
        }

        // 5. CÁC TIA SẤM SÉT BÒ LAN TRÊN MẶT ĐẤT (Ground Fractal Lightning)
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16.0;
            double r1 = currentRadius * 0.9;
            double r2 = currentRadius + 18.0 + Math.sin(i * 12.3 + age * 0.2) * 6.0;
            stroke(v, m, polar(r1, 0.15, a), polar(r2, 0.2, a + 0.05), 0.045F, fade * 0.6F);
        }

        // 6. HÀO QUANG PLASMA CUỘN TRÀO KHÔNG GIAN (Swirling Aura Discs)
        pressureAura(v, m, age, fade, eye, currentRadius);
    }

    private static void cylinder(VertexConsumer v, Matrix4f m, float r, float height, float alpha, int color, double age) {
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI / 24.0, b = (i + 1) * Math.PI / 24.0;
            for (int j = 0; j < 6; j++) {
                double low = (height) * j / 6.0;
                double high = (height) * (j + 1) / 6.0;
                double ra = r * (0.95 + 0.05 * Math.sin(i * 2.7 + j * 1.8 + age * 0.13));
                quad(v, m, polar(ra, low, a), polar(ra, low, b), polar(ra, high, b), polar(ra, high, a), color, alpha);
            }
        }
    }

    private static void ring(VertexConsumer v, Matrix4f m, float y, float radius, float width, float alpha) {
        if (alpha <= 0.001F) return;
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            Vec3 p1 = polar(radius - width, y, a);
            Vec3 p2 = polar(radius + width, y, a);
            Vec3 p3 = polar(radius + width, y, b);
            Vec3 p4 = polar(radius - width, y, b);
            quad(v, m, p1, p2, p3, p4, 0xD946EF, alpha);
            quad(v, m, polar(radius - width * 0.5, y + 0.02, a), polar(radius + width * 0.5, y + 0.02, a),
                    polar(radius + width * 0.5, y + 0.02, b), polar(radius - width * 0.5, y + 0.02, b), 0xFFFFFF, alpha * 0.8F);
        }
    }

    private static void pressureAura(VertexConsumer v, Matrix4f m, double age, float fade, Vec3 eye, float colRadius) {
        for (int plume = 0; plume < 48; plume++) {
            double seed = fract(plume * 0.61803398875);
            double life = (age * 0.6 + plume * 2.0) % 40.0;
            double lifeNorm = life / 40.0;
            double angle = plume * 2.39996 + (age * 0.02);
            double size = 4.0 + seed * 5.0;
            double r = colRadius + 2.0 + lifeNorm * 14.0;
            double y = 4.0 + lifeNorm * 65.0 + seed * 12.0;

            Vec3 center = polar(r, y, angle);
            float alpha = fade * (float) (Math.sin(lifeNorm * Math.PI)) * 0.25F;
            if (alpha <= 0.005F) continue;

            Vec3 normal = eye.subtract(center);
            if (normal.lengthSqr() < 1e-4) continue;
            normal = normal.normalize();

            Vec3 right = new Vec3(normal.z, 0, -normal.x);
            if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
            right = right.normalize();
            Vec3 up = normal.cross(right).normalize();

            Vec3 x = right.scale(size);
            Vec3 yVec = up.scale(size * 0.85);

            disc(v, m, center.subtract(x).subtract(yVec), center.add(x).subtract(yVec),
                    center.add(x).add(yVec), center.subtract(x).add(yVec), 0x7222D8, alpha * 0.5F);
            disc(v, m, center.subtract(x.scale(0.6)).subtract(yVec.scale(0.6)), center.add(x.scale(0.6)).subtract(yVec.scale(0.6)),
                    center.add(x.scale(0.6)).add(yVec.scale(0.6)), center.subtract(x.scale(0.6)).add(yVec.scale(0.6)), 0xAD52F2, alpha * 0.7F);
        }
    }

    private static void stroke(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, float alpha) {
        if (alpha <= 0.001F) return;
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1e-6) return;
        direction = direction.normalize();

        Vec3 side = direction.cross(Math.abs(direction.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 other = direction.cross(side).normalize();

        for (int layer = 2; layer >= 0; layer--) {
            double w = width * (layer == 2 ? 4.0 : layer == 1 ? 2.0 : 0.9);
            float opacity = alpha * (layer == 2 ? 0.07F : layer == 1 ? 0.28F : 0.9F);
            int color = layer == 0 ? 0xF5DFFF : (layer == 1 ? 0xA550FF : 0x7C1AFF);

            for (int axis = 0; axis < 2; axis++) {
                Vec3 n = (axis == 0 ? side : other).scale(w);
                quad(v, m, a.subtract(n), a.add(n), b.add(n), b.subtract(n), color, opacity);
            }
        }
    }

    private static void disc(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
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

    private static Vec3 polar(double radius, double y, double angle) {
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static double fract(double x) {
        return x - Math.floor(x);
    }
}
