package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.OrbitalKineticProjectileModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * CLIENT OPENGL ADDITIVE BLENDING RENDERER: VŨ KHÍ TẤN CÔNG QUỸ ĐẠO (STELLAR REMOTE)
 * Đạt chuẩn đồ họa Lời Chúc Tử Vong / God Wand:
 * 1. SS-01 · Railgun: Cột Plasma Siêu Trọng Trường 3 tầng (White Core + Cyan Plasma + Sapphire Corona) + 24 nhánh Lôi điện
 * 2. SS-03 · Gungnir: Thần thương Thần đạo rực lửa phóng từ quỹ đạo Sao Mộc xuyên thủng khí quyển
 * 3. SS-04 · The Seven Stars: Tinh đồ Bắc Đẩu Thất Tinh liên hoàn oanh tạc
 * ĐỒNG BỘ 100% VỚI CAMERA VŨ TRỤ & LAO DỐC SIÊU THANH (Va chạm mặt đất tại Tick 45)
 */
public final class ClientOrbitalStrikeRenderer {

    public static final int MODE_RAILGUN = 0;
    public static final int MODE_GUNGNIR = 1;
    public static final int MODE_SEVEN_STARS = 2;

    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.fromNamespaceAndPath("weapons", "atomic_energy");

    private static ClientLevel world;
    private static Vec3 targetPos = Vec3.ZERO;
    private static int strikeMode = 0;
    private static long startTick = 0;
    private static boolean active = false;

    // Tọa độ 7 sao Bắc Đẩu tương đối
    public static final double[][] BIG_DIPPER_OFFSETS = {
            {-10.0, -12.0}, // Dubhe (Thiên Xu)
            {-14.0, -6.0},  // Merak (Thiên Toàn)
            {-8.0, 4.0},    // Phecda (Thiên Cơ)
            {-1.0, 2.0},    // Megrez (Thiên Quyền)
            {6.0, 7.0},     // Alioth (Ngọc Hành)
            {12.0, 11.0},   // Mizar (Khai Dương)
            {0.0, 0.0}      // Alkaid (Dao Quang - Tâm điểm)
    };

    public static void reset() {
        active = false;
        world = null;
        startTick = 0;
        targetPos = Vec3.ZERO;
    }

    public static void receive(Vec3 target, int mode) {
        reset();
        Minecraft mc = Minecraft.getInstance();
        world = mc.level;
        if (world == null) return;
        targetPos = target;
        strikeMode = mode;
        startTick = world.getGameTime();
        active = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static double getAge(float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.level != world) return -1;
        long elapsed = mc.level.getGameTime() - startTick;
        int maxAge = switch (strikeMode) {
            case MODE_RAILGUN -> 75;
            case MODE_GUNGNIR -> 75;
            case MODE_SEVEN_STARS -> 100;
            default -> 75;
        };
        if (elapsed > maxAge) return -1;
        return elapsed + delta;
    }

    private static OrbitalKineticProjectileModel projectileModel;

    private static void ensureProjectileModel() {
        if (projectileModel == null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getEntityModels() != null) {
                    projectileModel = new OrbitalKineticProjectileModel(mc.getEntityModels().bakeLayer(OrbitalKineticProjectileModel.LAYER_LOCATION));
                }
            } catch (Exception ignored) {
            }
            if (projectileModel == null) {
                projectileModel = new OrbitalKineticProjectileModel(OrbitalKineticProjectileModel.createBodyLayer().bakeRoot());
            }
        }
    }

    public static void render(PoseStack matrices, Camera cameraObj, float tickDelta) {
        double age = getAge(tickDelta);
        if (age < 0) return;

        AtomicRenderer.ensureEnergyTexture();

        Vec3 cam = cameraObj.getPosition();
        matrices.pushPose();
        matrices.translate(targetPos.x - cam.x, targetPos.y - cam.y, targetPos.z - cam.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            if (strikeMode == MODE_RAILGUN) {
                renderRailgun(mesh, mat, age);
            } else if (strikeMode == MODE_GUNGNIR) {
                renderGungnir(mesh, mat, age);
            } else if (strikeMode == MODE_SEVEN_STARS) {
                renderSevenStars(mesh, mat, age);
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            // Render 3D Model đầu đạn siêu thanh (Tungsten Rod / Gungnir Lance / Seven Stars)
            render3DProjectile(matrices, age);

            matrices.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * DỰNG 3D CUSTOM MODEL ĐẦU ĐẠN QUỸ ĐẠO SIÊU THANH (Blockbench Custom 3D Model)
     */
    private static void render3DProjectile(PoseStack matrices, double age) {
        if (age < 24.0 || age >= 45.0) return;
        ensureProjectileModel();
        if (projectileModel == null) return;

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        RenderType projType = RenderType.entityCutoutNoCull(OrbitalKineticProjectileModel.TEXTURE);

        double dropProgress = (age - 24.0) / 21.0;
        double ease = dropProgress * dropProgress * dropProgress; // Gia tốc trọng trường cực lớn

        if (strikeMode == MODE_RAILGUN) {
            double curY = 450.0 * (1.0 - ease);
            matrices.pushPose();
            matrices.translate(0, curY, 0);
            matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0f));
            matrices.scale(3.2f, 3.2f, 3.2f);
            projectileModel.setupMode(MODE_RAILGUN, (float) age, 3.8f);

            VertexConsumer buf = bufferSource.getBuffer(projType);
            projectileModel.renderToBuffer(matrices, buf, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            bufferSource.endBatch(projType);
            matrices.popPose();
        } else if (strikeMode == MODE_GUNGNIR) {
            double launchH = 450.0;
            double curY = launchH * (1.0 - ease);
            double curX = -45.0 * (1.0 - ease);
            double curZ = -45.0 * (1.0 - ease);

            matrices.pushPose();
            matrices.translate(curX, curY, curZ);
            // Hướng mũi thương theo vector lao xuống (45, -launchH, 45)
            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45.0f));
            matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(172.0f));
            matrices.scale(3.5f, 3.5f, 3.5f);
            projectileModel.setupMode(MODE_GUNGNIR, (float) age, 2.5f);

            VertexConsumer buf = bufferSource.getBuffer(projType);
            projectileModel.renderToBuffer(matrices, buf, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            bufferSource.endBatch(projType);
            matrices.popPose();
        } else if (strikeMode == MODE_SEVEN_STARS) {
            for (int i = 0; i < 7; i++) {
                double starHitTick = 38.0 + i * 6.0;
                double starStartTick = starHitTick - 14.0;
                if (age >= starStartTick && age < starHitTick) {
                    double starProgress = (age - starStartTick) / 14.0;
                    double starEase = starProgress * starProgress * starProgress;
                    double curY = 420.0 * (1.0 - starEase);
                    double sx = BIG_DIPPER_OFFSETS[i][0];
                    double sz = BIG_DIPPER_OFFSETS[i][1];

                    matrices.pushPose();
                    matrices.translate(sx, curY, sz);
                    matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0f));
                    matrices.scale(2.5f, 2.5f, 2.5f);
                    projectileModel.setupMode(MODE_SEVEN_STARS, (float) age + i * 10.0f, 4.0f);

                    VertexConsumer buf = bufferSource.getBuffer(projType);
                    projectileModel.renderToBuffer(matrices, buf, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    bufferSource.endBatch(projType);
                    matrices.popPose();
                }
            }
        }
    }

    /**
     * SS-01: RAILGUN CỘT PLASMA SIÊU TRỌNG TRƯỜNG CHUẨN LỜI CHÚC TỬ VONG
     */
    private static void renderRailgun(VertexConsumer v, Matrix4f m, double age) {
        float beamHeight = 450.0f;

        // Giai đoạn 1 (0 -> 24 ticks): Laser ngắm dẫn đường mỏng và các đĩa khóa mục tiêu
        if (age < 24.0) {
            float laserAlpha = (float) (0.35 + 0.65 * Math.sin(age * 0.9));
            cylinder(v, m, 0.15f, beamHeight, laserAlpha * 0.9f, 0x00E5FF, age);
            cylinder(v, m, 0.45f, beamHeight, laserAlpha * 0.35f, 0x0091EA, age);

            // Vòng radar nhấp nháy trên mặt đất
            float ringPulse = (float) ((age * 2.0) % 18.0);
            ring(v, m, 0.1f, ringPulse, 0.2f, 0x00E5FF, (1.0f - ringPulse / 18.0f) * 0.8f);
        }
        // Giai đoạn 2 (24 -> 65 ticks): Khai hỏa từ vệ tinh quỹ đạo và cắm xuống mặt đất tại tick 45
        else if (age < 65.0) {
            double blastAge = age - 24.0;
            float fade = (age < 55.0) ? 1.0f : (float) Math.max(0.0, 1.0 - (age - 55.0) / 10.0);

            // Bán kính nở rộng cực đại khi va chạm mặt đất (tick 45)
            float expand = (age < 45.0) ? (float) (0.5 + 0.5 * (blastAge / 21.0)) : 1.0f;
            float coreR = 3.8f * expand;
            float midR = 8.5f * expand;
            float coronaR = 14.0f * expand;

            // 1. Cột Plasma 3 tầng phát quang rực rỡ
            cylinder(v, m, coreR, beamHeight, 0.95f * fade, 0xFFFFFF, age);    // Lõi trắng chói
            cylinder(v, m, midR, beamHeight, 0.65f * fade, 0x00E5FF, age + 5);  // Vỏ Cyan điện từ
            cylinder(v, m, coronaR, beamHeight, 0.25f * fade, 0x0055FF, age + 10); // Hào quang Sapphire

            // 2. 24 Nhánh Lôi điện xoắn quanh thân cột
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12.0 + age * 0.01;
                Vec3 last = polar(midR * (0.9 + 0.1 * Math.sin(i * 2.0)), 0, a);
                for (int j = 1; j <= 5; j++) {
                    double y = j * (beamHeight / 5.0);
                    double jitter = Math.sin(i * 12.3 + j * 5.7 + age * 0.2) * 0.08;
                    Vec3 next = polar(midR * (0.95 + jitter), y, a + jitter);
                    stroke(v, m, last, next, 0.08f, fade * 0.7f, 0x00E5FF);
                    last = next;
                }
            }

            // 3. SÓNG XUNG KÍCH MẶT ĐẤT KHI CHÙM TIA VA CHẠM (Tick 45 -> 65)
            if (age >= 45.0) {
                double groundImpactAge = age - 45.0;
                for (int i = 0; i < 3; i++) {
                    float r = (float) (groundImpactAge * 2.2 + i * 4.0);
                    if (r < 48.0f) {
                        float ringAlpha = (float) (1.0 - r / 48.0f) * fade;
                        ring(v, m, 0.15f + i * 0.1f, r, 0.35f, 0x00E5FF, ringAlpha * 0.9f);
                        ring(v, m, 0.25f + i * 0.2f, r * 0.92f, 0.20f, 0xFFFFFF, ringAlpha * 0.7f);
                    }
                }
            }
        }
    }

    /**
     * SS-03: GUNGNIR THẦN THƯƠNG RỰC LỬA PHÓNG TỪ QUỸ ĐẠO
     */
    private static void renderGungnir(VertexConsumer v, Matrix4f m, double age) {
        float launchH = 450.0f;

        // Giai đoạn 1 (0 -> 24 ticks): Laser ngắm hồng ngoại màu đỏ
        if (age < 24.0) {
            float laserAlpha = (float) (0.4 + 0.6 * Math.sin(age * 1.2));
            cylinder(v, m, 0.18f, launchH, laserAlpha * 0.85f, 0xFF1744, age);
        }
        // Giai đoạn 2 (24 -> 45 ticks): Thần Thương lao dốc siêu thanh từ không gian
        else if (age < 45.0) {
            double dropProgress = (age - 24.0) / 21.0;
            double ease = dropProgress * dropProgress * dropProgress; // Gia tốc trọng lực

            double curY = launchH * (1.0 - ease);
            double curX = -45.0 * (1.0 - ease);
            double curZ = -45.0 * (1.0 - ease);

            Vec3 tip = new Vec3(curX, curY, curZ);
            Vec3 dir = new Vec3(45.0, -launchH, 45.0).normalize();
            Vec3 tail = tip.subtract(dir.scale(32.0));

            // Thân thương Gungnir bằng Plasma Vàng Kim - Đỏ
            stroke(v, m, tail, tip, 0.85f, 0.95f, 0xFFD54F);
            stroke(v, m, tail.subtract(dir.scale(12.0)), tip, 1.8f, 0.65f, 0xFF3D00);

            // Nón sóng xung kích âm thanh (Sonic Shock Cone)
            for (int k = 0; k < 3; k++) {
                Vec3 coneCenter = tip.subtract(dir.scale(k * 7.0 + 3.0));
                ring(v, m, (float) coneCenter.y, (float) (2.5 + k * 2.8), 0.35f, 0xFF9100, 0.75f);
            }
        }
        // Giai đoạn 3 (45 -> 70 ticks): Vụ nổ Thần Thương cắm đất hủy diệt
        else if (age < 70.0) {
            double impactAge = age - 45.0;
            float fade = (float) Math.max(0.0, 1.0 - impactAge / 25.0);

            // Cột lửa thần giáng thế
            float colRadius = (float) Math.max(0.2, 5.0 * fade);
            cylinder(v, m, colRadius * 0.6f, 180.0f, 0.9f * fade, 0xFFEA00, age);
            cylinder(v, m, colRadius, 180.0f, 0.5f * fade, 0xFF3D00, age);

            // Các đĩa sóng nổ nhiệt hạch trên mặt đất
            for (int i = 0; i < 3; i++) {
                float r = (float) (impactAge * 2.5 + i * 5.0);
                if (r < 52.0f) {
                    float ringAlpha = (float) (1.0 - r / 52.0f) * fade;
                    ring(v, m, 0.2f + i * 0.15f, r, 0.4f, 0xFF6D00, ringAlpha * 0.85f);
                    ring(v, m, 0.3f + i * 0.2f, r * 0.85f, 0.25f, 0xFFFF00, ringAlpha * 0.7f);
                }
            }
        }
    }

    /**
     * SS-04: THE SEVEN STARS (BẮC ĐẨU THẤT TINH) 7 MŨI OANH TẠC LIÊN HOÀN
     */
    private static void renderSevenStars(VertexConsumer v, Matrix4f m, double age) {
        float beamHeight = 420.0f;

        // Vẽ 7 vị trí sao Bắc Đẩu
        for (int i = 0; i < 7; i++) {
            double starHitTick = 38.0 + i * 6.0; // Bắt đầu cắm đất từ tick 38 đến 74
            double starAge = age - starHitTick;

            double sx = BIG_DIPPER_OFFSETS[i][0];
            double sz = BIG_DIPPER_OFFSETS[i][1];

            // Trước khi bắn: Tia ngắm tím thiên hà
            if (age < starHitTick) {
                float alpha = (float) (0.2 + 0.3 * Math.sin(age * 0.8 + i));
                cylinder(v, m, 0.12f, beamHeight, alpha, 0xD500F9, age);
            }
            // Khi oanh tạc: Cột Plasma tím cực hạn cắm thẳng xuống
            else if (starAge >= 0 && starAge < 20.0) {
                float fade = (float) (1.0 - starAge / 20.0);
                float radius = (i == 6) ? 4.5f * fade : 2.5f * fade; // Ngôi sao thứ 7 (Alkaid) to gấp đôi
                int coreColor = (i == 6) ? 0xFFFFFF : 0xF5DFFF;
                int outerColor = (i == 6) ? 0xAA00FF : 0xD500F9;

                cylinderAt(v, m, (float) sx, (float) sz, radius * 0.6f, beamHeight, fade * 0.95f, coreColor, age);
                cylinderAt(v, m, (float) sx, (float) sz, radius, beamHeight, fade * 0.55f, outerColor, age);

                // Sóng xung kích riêng từng ngôi sao
                float rRing = (float) (starAge * 1.5);
                if (rRing < 22.0f) {
                    ringAt(v, m, (float) sx, (float) sz, 0.15f, rRing, 0.25f, outerColor, (float) (1.0 - rRing / 22.0f));
                }
            }
        }

        // Vẽ lưới liên kết chòm sao Bắc Đẩu trên bầu trời (ở độ cao y = 280m)
        if (age >= 10.0 && age <= 78.0) {
            float lineAlpha = (float) (0.6 * Math.min(1.0, (age - 10.0) / 12.0) * Math.max(0.0, 1.0 - (age - 68.0) / 10.0));
            for (int i = 0; i < 6; i++) {
                double x1 = BIG_DIPPER_OFFSETS[i][0];
                double z1 = BIG_DIPPER_OFFSETS[i][1];
                double x2 = BIG_DIPPER_OFFSETS[i + 1][0];
                double z2 = BIG_DIPPER_OFFSETS[i + 1][1];
                stroke(v, m, new Vec3(x1, 280.0, z1), new Vec3(x2, 280.0, z2), 0.25f, lineAlpha, 0xE040FB);
            }
        }
    }

    // ==============================================================
    // CÁC HÀM TIỆN ÍCH RENDER PLASMA OPENGL
    // ==============================================================

    private static void cylinder(VertexConsumer v, Matrix4f m, float r, float height, float alpha, int color, double age) {
        cylinderAt(v, m, 0, 0, r, height, alpha, color, age);
    }

    private static void cylinderAt(VertexConsumer v, Matrix4f m, float cx, float cz, float r, float height, float alpha, int color, double age) {
        if (alpha <= 0.001f || r <= 0.001f) return;
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;

            Vec3 p1 = new Vec3(cx + Math.cos(a) * r, 0, cz + Math.sin(a) * r);
            Vec3 p2 = new Vec3(cx + Math.cos(b) * r, 0, cz + Math.sin(b) * r);
            Vec3 p3 = new Vec3(cx + Math.cos(b) * r, height, cz + Math.sin(b) * r);
            Vec3 p4 = new Vec3(cx + Math.cos(a) * r, height, cz + Math.sin(a) * r);

            quad(v, m, p1, p2, p3, p4, color, alpha);
        }
    }

    private static void ring(VertexConsumer v, Matrix4f m, float y, float radius, float width, int color, float alpha) {
        ringAt(v, m, 0, 0, y, radius, width, color, alpha);
    }

    private static void ringAt(VertexConsumer v, Matrix4f m, float cx, float cz, float y, float radius, float width, int color, float alpha) {
        if (alpha <= 0.001f || radius <= 0.001f) return;
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;

            Vec3 p1 = new Vec3(cx + Math.cos(a) * (radius - width), y, cz + Math.sin(a) * (radius - width));
            Vec3 p2 = new Vec3(cx + Math.cos(a) * (radius + width), y, cz + Math.sin(a) * (radius + width));
            Vec3 p3 = new Vec3(cx + Math.cos(b) * (radius + width), y, cz + Math.sin(b) * (radius + width));
            Vec3 p4 = new Vec3(cx + Math.cos(b) * (radius - width), y, cz + Math.sin(b) * (radius - width));

            quad(v, m, p1, p2, p3, p4, color, alpha);
        }
    }

    private static void stroke(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, float alpha, int color) {
        if (alpha <= 0.001f) return;
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1e-6) return;
        direction = direction.normalize();

        Vec3 side = direction.cross(Math.abs(direction.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 other = direction.cross(side).normalize();

        for (int axis = 0; axis < 2; axis++) {
            Vec3 n = (axis == 0 ? side : other).scale(width);
            quad(v, m, a.subtract(n), a.add(n), b.add(n), b.subtract(n), color, alpha);
        }
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

    /**
     * HUD Overlay: Hiệu ứng Radar Vệ Tinh Quỹ Đạo khi đang khóa mục tiêu
     */
    public static void renderHudOverlay(GuiGraphics graphics, float partialTick) {
        if (!active) return;
        double age = getAge(partialTick);
        if (age < 0) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Chớp sáng màn hình tại thời điểm va chạm mặt đất (tick 45)
        boolean impact = (age >= 45.0 && age <= 48.0);
        if (impact) {
            int flashColor = ((int) (0xCC * (1.0 - (age - 45.0) / 3.0)) << 24) | 0xFFFFFF;
            graphics.fill(0, 0, screenWidth, screenHeight, flashColor);
        }
    }
}
