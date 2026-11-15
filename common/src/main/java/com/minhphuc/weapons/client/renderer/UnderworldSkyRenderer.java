package com.minhphuc.weapons.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Bầu trời Ma Giới (Underworld - Tensura):
 * Thay thế bầu trời/sương mù ngột ngạt của Nether bằng vòm trời đỏ thẫm - tím huyết mở rộng,
 * có Mặt Trăng Ma Giới khổng lồ, dải tinh vân tím-đỏ và các hòn đảo / mảnh vỡ cổ đại trôi nổi ở chân trời xa.
 */
public final class UnderworldSkyRenderer {

    private UnderworldSkyRenderer() {}

    public static boolean isUnderworld(Level level) {
        return level != null && level.dimension() == Level.NETHER;
    }

    public static void renderUnderworldSky(Matrix4f frustumMatrix, float partialTick, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !isUnderworld(mc.level)) return;

        float gameTime = (mc.level.getGameTime() + partialTick) * 0.008F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        Matrix4f mat = poseStack.last().pose();

        // 1. Vòm trời đa tầng Ma Giới (đỉnh đen tím huyền bí -> giữa tím magenta -> chân trời đỏ máu)
        var skyMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float rDome = 140.0F;
        int stacks = 12;
        int slices = 24;
        for (int i = 0; i < stacks; i++) {
            float phi1 = (float) (-Math.PI / 2.0 + Math.PI * i / stacks);
            float phi2 = (float) (-Math.PI / 2.0 + Math.PI * (i + 1) / stacks);

            float y1 = Mth.sin(phi1) * rDome;
            float ring1 = Mth.cos(phi1) * rDome;
            float y2 = Mth.sin(phi2) * rDome;
            float ring2 = Mth.cos(phi2) * rDome;

            float[] c1 = sampleSkyColor(phi1);
            float[] c2 = sampleSkyColor(phi2);

            for (int j = 0; j < slices; j++) {
                float theta1 = (float) (2.0 * Math.PI * j / slices);
                float theta2 = (float) (2.0 * Math.PI * (j + 1) / slices);

                float x11 = Mth.cos(theta1) * ring1;
                float z11 = Mth.sin(theta1) * ring1;
                float x12 = Mth.cos(theta2) * ring1;
                float z12 = Mth.sin(theta2) * ring1;

                float x21 = Mth.cos(theta1) * ring2;
                float z21 = Mth.sin(theta1) * ring2;
                float x22 = Mth.cos(theta2) * ring2;
                float z22 = Mth.sin(theta2) * ring2;

                skyMesh.addVertex(mat, x11, y1, z11).setColor(c1[0], c1[1], c1[2], 1.0F);
                skyMesh.addVertex(mat, x12, y1, z12).setColor(c1[0], c1[1], c1[2], 1.0F);
                skyMesh.addVertex(mat, x22, y2, z22).setColor(c2[0], c2[1], c2[2], 1.0F);
                skyMesh.addVertex(mat, x21, y2, z21).setColor(c2[0], c2[1], c2[2], 1.0F);
            }
        }
        BufferUploader.drawWithShader(skyMesh.buildOrThrow());

        // 2. Nhật Thực Huyết Nguyệt khổng lồ trên nền trời Ma Giới + Đảo Lơ Lửng xa
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        var celestialMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Vòng sáng ma tố tím đỏ quanh Nhật Thực
        float moonX = 48.0F;
        float moonY = 72.0F;
        float moonZ = -95.0F;
        float halo = 26.0F;
        celestialMesh.addVertex(mat, moonX - halo, moonY - halo, moonZ + 2.0F).setColor(0.55F, 0.06F, 0.22F, 0.0F);
        celestialMesh.addVertex(mat, moonX + halo, moonY - halo, moonZ - 2.0F).setColor(0.55F, 0.06F, 0.22F, 0.0F);
        celestialMesh.addVertex(mat, moonX + halo, moonY + halo, moonZ - 2.0F).setColor(0.85F, 0.14F, 0.35F, 0.52F);
        celestialMesh.addVertex(mat, moonX - halo, moonY + halo, moonZ + 2.0F).setColor(0.85F, 0.14F, 0.35F, 0.52F);

        // Tinh vân ma tố lấp lánh trên bầu trời
        for (int s = 0; s < 36; s++) {
            float angle = s * 0.42F + gameTime * 0.05F;
            float dist = 92.0F;
            float sx = Mth.cos(angle) * dist;
            float sz = Mth.sin(angle) * dist;
            float sy = 22.0F + Mth.sin(s * 1.7F) * 42.0F;
            float szize = 0.55F + (s % 3) * 0.35F;
            float pulse = 0.55F + 0.45F * Mth.sin(gameTime * 3.0F + s);
            celestialMesh.addVertex(mat, sx - szize, sy - szize, sz).setColor(0.95F, 0.55F, 0.85F, pulse);
            celestialMesh.addVertex(mat, sx + szize, sy - szize, sz).setColor(0.95F, 0.55F, 0.85F, pulse);
            celestialMesh.addVertex(mat, sx + szize, sy + szize, sz).setColor(1.0F, 0.82F, 0.96F, pulse);
            celestialMesh.addVertex(mat, sx - szize, sy + szize, sz).setColor(1.0F, 0.82F, 0.96F, pulse);
        }

        BufferUploader.drawWithShader(celestialMesh.buildOrThrow());

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static float[] sampleSkyColor(float phi) {
        // phi từ -pi/2 (đáy) -> 0 (chân trời) -> +pi/2 (đỉnh)
        if (phi < 0.0F) {
            return new float[]{0.32F, 0.03F, 0.10F};
        }
        float t = Mth.clamp(phi / (float) (Math.PI / 2.0), 0.0F, 1.0F);
        if (t < 0.35F) {
            float k = t / 0.35F;
            // Chân trời đỏ huyết -> dải tím hồng magenta (giống ảnh anime Ma Giới)
            return new float[]{
                    Mth.lerp(k, 0.58F, 0.42F),
                    Mth.lerp(k, 0.05F, 0.04F),
                    Mth.lerp(k, 0.17F, 0.30F)
            };
        } else {
            float k = (t - 0.35F) / 0.65F;
            // Dải tím hồng -> đỉnh vòm trời đen huyền bí
            return new float[]{
                    Mth.lerp(k, 0.42F, 0.04F),
                    Mth.lerp(k, 0.04F, 0.01F),
                    Mth.lerp(k, 0.30F, 0.07F)
            };
        }
    }
}
