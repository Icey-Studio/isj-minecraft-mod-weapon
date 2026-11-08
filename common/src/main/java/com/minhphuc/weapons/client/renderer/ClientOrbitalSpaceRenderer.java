package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.OrbitalDefenseSatelliteModel;
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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * CLIENT OPENGL RENDERER: KHÔNG GIAN QUỸ ĐẠO & ĐỊA CẦU SIÊU THỰC (HYPER-REALISTIC ORBITAL SPACE & EARTH)
 * Nâng cấp toàn diện chất lượng đồ họa chuẩn điện ảnh:
 * 1. Màn che vũ trụ đen tuyệt đối (Cosmic Blackout Sky Sphere) che phủ hoàn toàn bầu trời ban ngày của Minecraft.
 * 2. Dải Ngân Hà rực rỡ (The Milky Way Galaxy Dust Lane) uốn lượn qua vòm trời đêm.
 * 3. Mặt Trời rực rỡ từ xa (Blazing Distant Sun) với quầng hào quang và hiệu ứng lóa sáng (Lens Flare).
 * 4. 650+ vì sao sắc nét đa màu sắc (O/B xanh kim, G vàng kim, M đỏ thẫm).
 * 5. Bán Cầu Trái Đất Siêu Thực (Hyper-Realistic Curved Earth):
 *    - Độ cong chân trời chuẩn trạm vũ trụ ISS.
 *    - Phân chia ngày & đêm chân thực (Day-Night Terminator).
 *    - Đại dương sâu thẳm phản chiếu ánh Mặt Trời (Ocean Specular Glint).
 *    - Ánh đèn các siêu đô thị ban đêm (Glowing Golden City Lights on Night Hemisphere).
 *    - Các xoáy bão mây trắng bồng bềnh đổ bóng xuống biển.
 *    - Vành đai tán xạ khí quyển Rayleigh đa tầng (Cyan-Blue Atmospheric Rim Scattering).
 * 6. Trạm Vũ Khí Vệ Tinh Quỹ Đạo Chi Tiết (High-Tech Military Orbital Satellite):
 *    - Khung giáp Titan, lớp cách nhiệt mạ vàng kim (Gold Foil).
 *    - Hai cánh pin mặt trời quang điện rộng lớn.
 *    - Đèn tín hiệu nhấp nháy (Red/Green/White Nav Strobe).
 *    - Động cơ đẩy phản lực khí nén RCS (Puffing Attitude Jets).
 *    - 6 cuộn cảm từ trường Plasma nạp năng lượng cực hạn.
 */
public final class ClientOrbitalSpaceRenderer {

    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.fromNamespaceAndPath("weapons", "atomic_energy");

    public static void render(PoseStack matrices, Camera camera, float partialTick, Vec3 targetPos, int mode, float age) {
        if (age < 0 || age >= 46.0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = camera.getPosition();
        float spaceFade = (age < 28.0f) ? 1.0f : Math.max(0.0f, 1.0f - (age - 28.0f) / 16.0f);
        if (spaceFade <= 0.001f) return;

        AtomicRenderer.ensureEnergyTexture();

        // 1. MÀN ĐÊM VŨ TRỤ TUYỆT ĐỐI (Che phủ bầu trời xanh Minecraft) & DẢI NGÂN HÀ
        renderCosmicVoidAndMilkyWay(matrices, camPos, spaceFade, age);

        // 2. BIỂN SAO SẮC NÉT & MẶT TRỜI XA XÔI
        renderStarsAndSun(matrices, camPos, spaceFade, age);

        // 3. ĐỊA CẦU TRÁI ĐẤT SIÊU THỰC (Đại dương, Đèn thành phố ban đêm, Khí quyển Rayleigh)
        renderHyperRealisticEarth(matrices, camPos, targetPos, spaceFade, age);

        // 4. TRẠM VŨ KHÍ VỆ TINH QUỸ ĐẠO QUÂN SỰ
        renderMilitarySatellite(matrices, camPos, targetPos, mode, spaceFade, age);

        // 5. VỆT KHÍ ĐỘNG HỌC SIÊU THANH KHI LAO DỐC
        if (age >= 28.0f && age < 45.0f) {
            renderReentryStreaks(matrices, camPos, targetPos, age);
        }
    }

    /**
     * DỰNG MÀN ĐÊM VŨ TRỤ ĐEN TUYỆT ĐỐI & DẢI NGÂN HÀ (Cosmic Void & Milky Way Galaxy)
     */
    private static void renderCosmicVoidAndMilkyWay(PoseStack matrices, Vec3 camPos, float fade, float age) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        matrices.pushPose();
        matrices.translate(0, 0, 0);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f mat = matrices.last().pose();

            // Vòm cầu hắc ám bán kính 380m bao bọc toàn bộ tầm nhìn
            int domeSegs = 36;
            int domeRings = 18;
            double radius = 380.0;
            int blackAlpha = Math.clamp(Math.round(fade * 255.0f), 0, 255);

            for (int r = 0; r < domeRings; r++) {
                double t1 = (double) r / domeRings * Math.PI * 0.55;
                double t2 = (double) (r + 1) / domeRings * Math.PI * 0.55;

                double sinT1 = Math.sin(t1), cosT1 = Math.cos(t1);
                double sinT2 = Math.sin(t2), cosT2 = Math.cos(t2);

                for (int s = 0; s < domeSegs; s++) {
                    double p1 = (double) s / domeSegs * Math.PI * 2.0;
                    double p2 = (double) (s + 1) / domeSegs * Math.PI * 2.0;

                    Vec3 pA = new Vec3(radius * sinT1 * Math.cos(p1), radius * cosT1, radius * sinT1 * Math.sin(p1));
                    Vec3 pB = new Vec3(radius * sinT1 * Math.cos(p2), radius * cosT1, radius * sinT1 * Math.sin(p2));
                    Vec3 pC = new Vec3(radius * sinT2 * Math.cos(p2), radius * cosT2, radius * sinT2 * Math.sin(p2));
                    Vec3 pD = new Vec3(radius * sinT2 * Math.cos(p1), radius * cosT2, radius * sinT2 * Math.sin(p1));

                    // Màu đen tuyền vũ trụ 0x010003
                    addColoredQuad(mesh, mat, pA, pB, pC, pD, 1, 0, 3, blackAlpha);
                }
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }

            // Vẽ Dải Ngân Hà (Milky Way Dust Band) uốn lượn rực rỡ
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

            var mwMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int mwPoints = 32;
            double mwRadius = 370.0;

            for (int i = 0; i < mwPoints; i++) {
                double frac1 = (double) i / mwPoints;
                double frac2 = (double) (i + 1) / mwPoints;

                double angle1 = frac1 * Math.PI * 1.6 - 0.4;
                double angle2 = frac2 * Math.PI * 1.6 - 0.4;

                double y1 = Math.sin(frac1 * Math.PI) * 180.0 + 40.0;
                double y2 = Math.sin(frac2 * Math.PI) * 180.0 + 40.0;

                double r1 = Math.cos(frac1 * Math.PI * 0.5) * 310.0;
                double r2 = Math.cos(frac2 * Math.PI * 0.5) * 310.0;

                Vec3 pt1 = new Vec3(Math.cos(angle1) * r1, y1, Math.sin(angle1) * r1);
                Vec3 pt2 = new Vec3(Math.cos(angle2) * r2, y2, Math.sin(angle2) * r2);

                Vec3 perp1 = new Vec3(-Math.sin(angle1) * 35.0, 15.0, Math.cos(angle1) * 35.0);
                Vec3 perp2 = new Vec3(-Math.sin(angle2) * 35.0, 15.0, Math.cos(angle2) * 35.0);

                float mwAlpha = fade * 0.38f * (float) Math.sin(frac1 * Math.PI);
                int opacity = Math.clamp(Math.round(mwAlpha * 255.0f), 0, 255);

                energyVertex(mwMesh, mat, pt1.subtract(perp1), 0x3D2B56, opacity, 0.05f, 0.5f);
                energyVertex(mwMesh, mat, pt1.add(perp1), 0x1E3A8A, opacity, 0.95f, 0.5f);
                energyVertex(mwMesh, mat, pt2.add(perp2), 0x1E3A8A, opacity, 0.95f, 0.5f);
                energyVertex(mwMesh, mat, pt2.subtract(perp2), 0x3D2B56, opacity, 0.05f, 0.5f);
            }

            var mwBuilt = mwMesh.build();
            if (mwBuilt != null) {
                BufferUploader.drawWithShader(mwBuilt);
            }

        } finally {
            matrices.popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * DỰNG 650+ VÌ SAO SẮC NÉT & MẶT TRỜI CHÓI CHÂN THỰC
     */
    private static void renderStarsAndSun(PoseStack matrices, Vec3 camPos, float fade, float age) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        matrices.pushPose();
        matrices.translate(0, 0, 0);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f mat = matrices.last().pose();

            // 1. 650 ngôi sao sắc nét không nhấp nháy thô thiển
            int starCount = 650;
            double domeRadius = 360.0;

            for (int i = 0; i < starCount; i++) {
                double seed = fract(i * 0.61803398875);
                double seed2 = fract(i * 0.38196601125);
                double seed3 = fract(i * 0.17320508075);

                double phi = seed * Math.PI * 2.0;
                double theta = Math.acos(0.04 + seed2 * 0.96);

                double sx = Math.sin(theta) * Math.cos(phi) * domeRadius;
                double sy = Math.cos(theta) * domeRadius;
                double sz = Math.sin(theta) * Math.sin(phi) * domeRadius;

                // Các vì sao trong vũ trụ có độ sáng tĩnh ổn định, độ chớp tinh tế
                float starAlpha = fade * (0.65f + (float) seed * 0.35f);
                float starSize = 0.8f + (float) seed3 * 2.2f;

                // Phân loại quang phổ sao: 65% Trắng xanh, 20% Vàng kim, 15% Đỏ thẫm
                int r, g, b;
                if (seed3 < 0.65) {
                    r = 210; g = 235; b = 255; // O/B White-Blue
                } else if (seed3 < 0.85) {
                    r = 255; g = 240; b = 180; // G Yellow
                } else {
                    r = 255; g = 160; b = 140; // M Orange-Red
                }

                int a = Math.clamp(Math.round(starAlpha * 255.0f), 0, 255);
                billboardStar(mesh, mat, new Vec3(sx, sy, sz), starSize, r, g, b, a);
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }

            // 2. Mặt Trời Chói Lóa (Blazing Sun) ở góc chếch trên cao
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

            var sunMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Vec3 sunDir = new Vec3(-180.0, 280.0, -120.0).normalize();
            Vec3 sunPos = sunDir.scale(350.0);

            // Đĩa mặt trời chói trắng 0xFFFFFF
            renderSunCorona(sunMesh, mat, sunPos, 36.0f, 0xFFFFFF, fade * 0.95f);
            // Quầng hào quang vàng kim 0xFFE082
            renderSunCorona(sunMesh, mat, sunPos, 70.0f, 0xFFD54F, fade * 0.65f);
            // Vệt lóa sáng ngang anamorphic lens flare
            renderAnamorphicFlare(sunMesh, mat, sunPos, 220.0f, 6.0f, 0x80D8FF, fade * 0.75f);

            var sunBuilt = sunMesh.build();
            if (sunBuilt != null) {
                BufferUploader.drawWithShader(sunBuilt);
            }

        } finally {
            matrices.popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * DỰNG ĐỊA CẦU TRÁI ĐẤT SIÊU THỰC (Đại dương phản quang, Đèn thành phố ban đêm, Khí quyển Rayleigh)
     */
    private static void renderHyperRealisticEarth(PoseStack matrices, Vec3 camPos, Vec3 targetPos, float fade, float age) {
        matrices.pushPose();

        // Đặt tâm cầu Trái Đất bên dưới và chếch nhẹ để tạo đường cong chân trời ISS chuẩn xác
        double earthRadius = 900.0;
        double earthCenterY = targetPos.y - earthRadius + 110.0;
        matrices.translate(targetPos.x - camPos.x, earthCenterY - camPos.y, targetPos.z - camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f mat = matrices.last().pose();

            int rings = 36;
            int segments = 64;
            double maxTheta = Math.toRadians(70.0); // Góc nhìn đường chân trời ôm trọn màn hình

            // Hướng nguồn sáng Mặt Trời chiếu xuống Trái Đất (tạo đường phân chia ngày/đêm)
            Vec3 sunDir = new Vec3(-0.6, 0.7, -0.4).normalize();

            for (int r = 0; r < rings; r++) {
                double t1 = (double) r / rings * maxTheta;
                double t2 = (double) (r + 1) / rings * maxTheta;

                double sinT1 = Math.sin(t1), cosT1 = Math.cos(t1);
                double sinT2 = Math.sin(t2), cosT2 = Math.cos(t2);

                for (int s = 0; s < segments; s++) {
                    double p1 = (double) s / segments * Math.PI * 2.0;
                    double p2 = (double) (s + 1) / segments * Math.PI * 2.0;

                    double sinP1 = Math.sin(p1), cosP1 = Math.cos(p1);
                    double sinP2 = Math.sin(p2), cosP2 = Math.cos(p2);

                    Vec3 v1 = new Vec3(earthRadius * sinT1 * cosP1, earthRadius * cosT1, earthRadius * sinT1 * sinP1);
                    Vec3 v2 = new Vec3(earthRadius * sinT1 * cosP2, earthRadius * cosT1, earthRadius * sinT1 * sinP2);
                    Vec3 v3 = new Vec3(earthRadius * sinT2 * cosP2, earthRadius * cosT2, earthRadius * sinT2 * sinP2);
                    Vec3 v4 = new Vec3(earthRadius * sinT2 * cosP1, earthRadius * cosT2, earthRadius * sinT2 * sinP1);

                    // Tính góc chiếu sáng mặt trời (Dot Product)
                    Vec3 normal = v1.normalize();
                    double sunDot = normal.dot(sunDir); // > 0 là ban ngày, < 0 là ban đêm

                    // Lục địa & Đại dương đa tần số
                    double landNoise = Math.sin(p1 * 5.0 + 1.4) * Math.cos(t1 * 7.0)
                            + Math.sin(p1 * 11.0) * 0.35 + Math.cos(t1 * 14.0) * 0.25;
                    boolean isLand = landNoise > 0.12;

                    // Dải mây trắng bồng bềnh cuộn xoáy
                    double cloudNoise = Math.sin(p1 * 8.0 + age * 0.008) * Math.cos(t1 * 12.0)
                            + Math.cos(p1 * 14.0 + 0.8) * 0.4;
                    boolean isCloud = cloudNoise > 0.42;

                    int cr, cg, cb;
                    float baseAlpha = fade * 0.98f;

                    if (sunDot > 0.05) {
                        // === PHẦN BAN NGÀY (DAY SIDE) ===
                        float sunFactor = (float) Math.clamp(sunDot * 1.5, 0.2, 1.0);

                        if (isCloud) {
                            // Mây trắng bồng bềnh
                            cr = Math.round(245 * sunFactor);
                            cg = Math.round(250 * sunFactor);
                            cb = Math.round(255 * sunFactor);
                        } else if (isLand) {
                            // Lục địa xanh ngọc & vàng cát
                            cr = Math.round(45 * sunFactor);
                            cg = Math.round(135 * sunFactor);
                            cb = Math.round(65 * sunFactor);
                        } else {
                            // Đại dương sâu thẳm phản quang xanh sapphire
                            cr = Math.round(12 * sunFactor);
                            cg = Math.round(55 * sunFactor);
                            cb = Math.round(135 * sunFactor);
                        }
                    } else if (sunDot < -0.08) {
                        // === PHẦN BAN ĐÊM (NIGHT SIDE) ===
                        // Hiệu ứng ĐÈN THÀNH PHỐ BAN ĐÊM (City Night Lights) rực rỡ như ảnh chụp ISS
                        if (isLand && !isCloud && fract(landNoise * 18.0) > 0.45) {
                            // Cụm đèn thành phố phát quang vàng cam rực sáng
                            cr = 255; cg = 185; cb = 65;
                            baseAlpha = fade * 0.92f;
                        } else {
                            // Đại dương & lục địa đêm tối tuyền
                            cr = 3; cg = 8; cb = 22;
                        }
                    } else {
                        // === ĐƯỜNG PHÂN CHIA HOÀNG HÔN / BÌNH MINH (TERMINATOR) ===
                        // Ánh hoàng hôn vàng cam lộng lẫy
                        cr = 180; cg = 75; cb = 35;
                    }

                    int alphaByte = Math.clamp(Math.round(baseAlpha * 255.0f), 0, 255);
                    addColoredQuad(mesh, mat, v1, v2, v3, v4, cr, cg, cb, alphaByte);
                }
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            // cleanup
        }

        // =========================================================================
        // VÀNH ĐAI TÁN XẠ KHÍ QUYỂN RAYLEIGH ĐA TẦNG (Cyan-Blue Atmospheric Rim)
        // Vòng phát quang màu xanh ngọc lam ôm trọn đường cong Trái Đất
        // =========================================================================
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        try {
            var atmoMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            double atmoRadius = earthRadius + 4.0;
            int atmoSegments = 64;
            double rimTheta = Math.toRadians(66.5);

            for (int layer = 0; layer < 5; layer++) {
                double layerRadius = atmoRadius + layer * 9.0;
                float layerAlpha = fade * (layer == 0 ? 0.92f : layer == 1 ? 0.65f : layer == 2 ? 0.40f : layer == 3 ? 0.22f : 0.08f);
                int color = (layer == 0) ? 0x80D8FF : (layer == 1 ? 0x00E5FF : (layer == 2 ? 0x0091EA : 0x01579B));

                for (int s = 0; s < atmoSegments; s++) {
                    double p1 = (double) s / atmoSegments * Math.PI * 2.0;
                    double p2 = (double) (s + 1) / atmoSegments * Math.PI * 2.0;

                    Vec3 in1 = new Vec3(earthRadius * Math.sin(rimTheta) * Math.cos(p1), earthRadius * Math.cos(rimTheta), earthRadius * Math.sin(rimTheta) * Math.sin(p1));
                    Vec3 in2 = new Vec3(earthRadius * Math.sin(rimTheta) * Math.cos(p2), earthRadius * Math.cos(rimTheta), earthRadius * Math.sin(rimTheta) * Math.sin(p2));
                    Vec3 out1 = new Vec3(layerRadius * Math.sin(rimTheta) * Math.cos(p1), layerRadius * Math.cos(rimTheta), layerRadius * Math.sin(rimTheta) * Math.sin(p1));
                    Vec3 out2 = new Vec3(layerRadius * Math.sin(rimTheta) * Math.cos(p2), layerRadius * Math.cos(rimTheta), layerRadius * Math.sin(rimTheta) * Math.sin(p2));

                    int opacity = Math.clamp(Math.round(layerAlpha * 255.0f), 0, 255);
                    energyVertex(atmoMesh, mat, in1, color, opacity, 0.875f, 0.5f);
                    energyVertex(atmoMesh, mat, in2, color, opacity, 0.875f, 0.5f);
                    energyVertex(atmoMesh, mat, out2, color, 0, 0.875f, 0.5f);
                    energyVertex(atmoMesh, mat, out1, color, 0, 0.875f, 0.5f);
                }
            }

            var atmoBuilt = atmoMesh.build();
            if (atmoBuilt != null) {
                BufferUploader.drawWithShader(atmoBuilt);
            }
        } finally {
            matrices.popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static OrbitalDefenseSatelliteModel satelliteModel;
    private static OrbitalKineticProjectileModel projectileModel;

    private static void ensureModels() {
        if (satelliteModel == null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getEntityModels() != null) {
                    satelliteModel = new OrbitalDefenseSatelliteModel(mc.getEntityModels().bakeLayer(OrbitalDefenseSatelliteModel.LAYER_LOCATION));
                }
            } catch (Exception ignored) {
            }
            if (satelliteModel == null) {
                satelliteModel = new OrbitalDefenseSatelliteModel(OrbitalDefenseSatelliteModel.createBodyLayer().bakeRoot());
            }
        }
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

    /**
     * DỰNG TRẠM VŨ KHÍ VỆ TINH QUÂN SỰ SIÊU CHI TIẾT (High-Tech Military Orbital Satellite)
     */
    private static void renderMilitarySatellite(PoseStack matrices, Vec3 camPos, Vec3 targetPos, int mode, float fade, float age) {
        if (age >= 28.0f) return;

        ensureModels();

        // Vệ tinh đặt ở vị trí phía trước chếch tầm nhìn của camera
        Vec3 satCenter = targetPos.add(18.0, 440.0, 26.0);

        matrices.pushPose();
        matrices.translate(satCenter.x - camPos.x, satCenter.y - camPos.y, satCenter.z - camPos.z);

        // Định hướng 3D trong không gian:
        // Xoay 180 độ trục Z để nòng pháo hướng thẳng xuống mặt đất
        matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0f));
        // Xoay trục Y 35 độ tạo góc phối cảnh điện ảnh 3D đón ánh sáng Mặt Trời
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(35.0f));

        float satScale = 4.2f;
        matrices.scale(satScale, satScale, satScale);

        float chargeProgress = Math.clamp((age - 14.0f) / 10.0f, 0.0f, 1.0f);
        if (satelliteModel != null) {
            satelliteModel.setupAnim(age, chargeProgress, mode);
        }

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        int alpha = Math.clamp(Math.round(fade * 255.0f), 0, 255);
        int tint = (alpha << 24) | 0x00FFFFFF;

        // 1. DỰNG MÔ HÌNH 3D TRẠM VỆ TINH PHÒNG THỦ QUÂN SỰ (Blockbench Custom Model)
        if (satelliteModel != null) {
            RenderType satRenderType = RenderType.entityCutoutNoCull(OrbitalDefenseSatelliteModel.TEXTURE);
            VertexConsumer satBuffer = bufferSource.getBuffer(satRenderType);
            satelliteModel.renderToBuffer(matrices, satBuffer, 0xF000F0, OverlayTexture.NO_OVERLAY, tint);
            bufferSource.endBatch(satRenderType);
        }

        // 2. DỰNG MÔ HÌNH 3D ĐẦU ĐẠN TẤN CÔNG QUỸ ĐẠO TRONG NÒNG PHÁO
        if (projectileModel != null) {
            matrices.pushPose();
            // Đầu đạn nằm trong ray nòng pháo
            if (age < 24.0f) {
                matrices.translate(0, 1.5, 0); // Vị trí trong ray gia tốc
                projectileModel.setupMode(mode, age, 0.0f);
            } else {
                // Khai hỏa: đạn phóng vụt xuống theo gia tốc siêu thanh
                float fireProgress = (age - 24.0f) / 4.0f;
                float exitY = 1.5f + fireProgress * fireProgress * 15.0f;
                matrices.translate(0, exitY, 0);
                projectileModel.setupMode(mode, age, 3.0f);
            }
            matrices.scale(1.1f, 1.1f, 1.1f);
            RenderType projRenderType = RenderType.entityCutoutNoCull(OrbitalKineticProjectileModel.TEXTURE);
            VertexConsumer projBuffer = bufferSource.getBuffer(projRenderType);
            projectileModel.renderToBuffer(matrices, projBuffer, 0xF000F0, OverlayTexture.NO_OVERLAY, tint);
            bufferSource.endBatch(projRenderType);
            matrices.popPose();
        }

        // 3. ĐÈN TÍN HIỆU HÀNG HẢI QUỸ ĐẠO (Navigational Strobes)
        renderSatelliteBeacons(matrices, age);

        // 4. NẠP NĂNG LƯỢNG 6 CUỘN CẢM PLASMA & ĐỘNG CƠ PHẢN LỰC RCS
        renderSatelliteChargingCoils(matrices, age, mode);

        matrices.popPose();
    }

    /**
     * ĐÈN TÍN HIỆU HÀNG HẢI QUỸ ĐẠO (Navigational Strobes)
     */
    private static void renderSatelliteBeacons(PoseStack matrices, float age) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f mat = matrices.last().pose();

            boolean strobe = (Math.sin(age * 0.4) > 0.0);
            // Đèn đỏ mạn trái (Port Red)
            billboardStar(mesh, mat, new Vec3(-51.0 / 16.0, 0, 0), 0.35f, 255, 30, 30, strobe ? 255 : 40);
            // Đèn xanh mạn phải (Starboard Green)
            billboardStar(mesh, mat, new Vec3(51.0 / 16.0, 0, 0), 0.35f, 30, 255, 30, strobe ? 255 : 40);
            // Đèn chớp trắng đỉnh tháp cảm biến
            billboardStar(mesh, mat, new Vec3(0, -19.0 / 16.0, 0), 0.45f, 255, 255, 255, (Math.sin(age * 0.8) > 0.4) ? 255 : 30);

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * NẠP NĂNG LƯỢNG 6 VÒNG CUỘN CẢM PLASMA & KHÍ NÉN RCS
     */
    private static void renderSatelliteChargingCoils(PoseStack matrices, float age, int mode) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            // Sạc năng lượng từ tick 14 -> 24
            float chargeProgress = Math.clamp((age - 14.0f) / 10.0f, 0.0f, 1.0f);
            float pulse = (float) (0.55 + 0.45 * Math.sin(age * 1.8));
            float coilAlpha = Math.max(0.25f, chargeProgress * pulse);

            int coilColor = (mode == 0) ? 0x00E5FF : (mode == 1 ? 0xFF9100 : 0xD500F9);

            // 6 Vòng từ trường cuộn cảm dọc nòng pháo (Model space: Y từ 20/16 đến 52.5/16)
            for (int c = 0; c < 6; c++) {
                double coilY = (20.0 + c * 6.5) / 16.0;
                double coilR = 0.32 + (1.0 - chargeProgress) * 0.08;

                renderTorusRing(mesh, mat, coilY, (float) coilR, 0.045f, coilColor, coilAlpha);
            }

            // Động cơ phản lực khí nén RCS (Puffing White Gas Jets)
            if (chargeProgress > 0.25f) {
                float rcsAlpha = (float) (0.4 + 0.6 * Math.sin(age * 2.2));
                int rcsOpacity = Math.clamp(Math.round(rcsAlpha * 0.7f * 255.0f), 0, 255);
                double rcsY = -6.0 / 16.0;
                disc(mesh, mat, new Vec3(8.5 / 16.0, rcsY, 0), new Vec3(14.0 / 16.0, rcsY, 0), new Vec3(14.0 / 16.0, rcsY + 0.2, 0), new Vec3(8.5 / 16.0, rcsY + 0.2, 0), 0xFFFFFF, rcsOpacity);
                disc(mesh, mat, new Vec3(-8.5 / 16.0, rcsY, 0), new Vec3(-14.0 / 16.0, rcsY, 0), new Vec3(-14.0 / 16.0, rcsY + 0.2, 0), new Vec3(-8.5 / 16.0, rcsY + 0.2, 0), 0xFFFFFF, rcsOpacity);
            }

            // Muzzle Flash bộc phát cực đại khi khai hỏa (ticks 23 -> 27.5)
            if (age >= 23.0f && age <= 27.5f) {
                float fireProgress = (age - 23.0f) / 4.5f;
                float flashSize = (1.0f - fireProgress) * 4.5f;
                int flashColor = (mode == 0) ? 0xCCFFFF : 0xFFE082;
                double muzzleY = (12.0 + 52.0) / 16.0;
                renderMuzzleFlash(mesh, mat, muzzleY, flashSize, flashColor);
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            // cleanup
        }
    }

    /**
     * VỆT KHÍ ĐỘNG HỌC SIÊU THANH KHI LAO DỐC (Hypersonic Re-entry Streaks)
     */
    private static void renderReentryStreaks(PoseStack matrices, Vec3 camPos, Vec3 targetPos, float age) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        matrices.pushPose();
        matrices.translate(0, 0, 0);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            int streakCount = 80;
            float streakAlpha = Math.min(1.0f, (age - 28.0f) / 4.0f) * Math.max(0.0f, 1.0f - (age - 39.0f) / 6.0f);

            for (int i = 0; i < streakCount; i++) {
                double seed = fract(i * 0.61803398875);
                double seed2 = fract(i * 0.38196601125);

                double angle = seed * Math.PI * 2.0;
                double dist = 6.0 + seed2 * 26.0;
                double streakY = camPos.y + ((age * 45.0 + i * 18.0) % 90.0) - 45.0;

                Vec3 p1 = new Vec3(camPos.x + Math.cos(angle) * dist, streakY + 16.0, camPos.z + Math.sin(angle) * dist);
                Vec3 p2 = new Vec3(camPos.x + Math.cos(angle) * dist, streakY - 16.0, camPos.z + Math.sin(angle) * dist);

                renderStreak(mesh, mat, p1, p2, 0.18f, 0x00E5FF, streakAlpha * 0.75f);
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            matrices.popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    // ==============================================================
    // CÁC HÀM TIỆN ÍCH HÌNH HỌC GPU
    // ==============================================================

    private static void billboardStar(VertexConsumer v, Matrix4f m, Vec3 p, float size, int r, int g, int b, int a) {
        v.addVertex(m, (float) (p.x - size), (float) (p.y - size), (float) p.z).setColor(r, g, b, a);
        v.addVertex(m, (float) (p.x + size), (float) (p.y - size), (float) p.z).setColor(r, g, b, a);
        v.addVertex(m, (float) (p.x + size), (float) (p.y + size), (float) p.z).setColor(r, g, b, a);
        v.addVertex(m, (float) (p.x - size), (float) (p.y + size), (float) p.z).setColor(r, g, b, a);
    }

    private static void renderSunCorona(VertexConsumer v, Matrix4f m, Vec3 p, float size, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        Vec3 a = p.add(-size, -size, 0);
        Vec3 b = p.add(size, -size, 0);
        Vec3 c = p.add(size, size, 0);
        Vec3 d = p.add(-size, size, 0);
        disc(v, m, a, b, c, d, color, opacity);
    }

    private static void renderAnamorphicFlare(VertexConsumer v, Matrix4f m, Vec3 p, float width, float height, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        Vec3 a = p.add(-width, -height, 0);
        Vec3 b = p.add(width, -height, 0);
        Vec3 c = p.add(width, height, 0);
        Vec3 d = p.add(-width, height, 0);
        disc(v, m, a, b, c, d, color, opacity);
    }

    private static void addColoredQuad(VertexConsumer v, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int b, int a) {
        v.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
        v.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
        v.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
        v.addVertex(m, (float) p4.x, (float) p4.y, (float) p4.z).setColor(r, g, b, a);
    }

    private static void renderTorusRing(VertexConsumer v, Matrix4f m, double y, float radius, float width, int color, float alpha) {
        int segs = 32;
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        for (int i = 0; i < segs; i++) {
            double a1 = i * Math.PI * 2.0 / segs;
            double a2 = (i + 1) * Math.PI * 2.0 / segs;

            Vec3 p1 = new Vec3(Math.cos(a1) * (radius - width), y, Math.sin(a1) * (radius - width));
            Vec3 p2 = new Vec3(Math.cos(a1) * (radius + width), y, Math.sin(a1) * (radius + width));
            Vec3 p3 = new Vec3(Math.cos(a2) * (radius + width), y, Math.sin(a2) * (radius + width));
            Vec3 p4 = new Vec3(Math.cos(a2) * (radius - width), y, Math.sin(a2) * (radius - width));

            energyVertex(v, m, p1, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p2, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p3, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p4, color, opacity, 0.875f, 0.5f);
        }
    }

    private static void renderMuzzleFlash(VertexConsumer v, Matrix4f m, double y, float size, int color) {
        int opacity = 255;
        Vec3 a = new Vec3(-size, y, -size);
        Vec3 b = new Vec3(size, y, -size);
        Vec3 c = new Vec3(size, y, size);
        Vec3 d = new Vec3(-size, y, size);

        energyVertex(v, m, a, color, opacity, 0.0078125f, 0.984375f);
        energyVertex(v, m, b, color, opacity, 0.4921875f, 0.984375f);
        energyVertex(v, m, c, color, opacity, 0.4921875f, 0.015625f);
        energyVertex(v, m, d, color, opacity, 0.0078125f, 0.015625f);
    }

    private static void renderStreak(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        Vec3 dir = b.subtract(a).normalize();
        Vec3 right = dir.cross(new Vec3(0, 0, 1)).normalize().scale(width);

        energyVertex(v, m, a.subtract(right), color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, a.add(right), color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, b.add(right), color, 0, 0.875f, 0.5f);
        energyVertex(v, m, b.subtract(right), color, 0, 0.875f, 0.5f);
    }

    private static void disc(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int opacity) {
        energyVertex(v, m, a, color, opacity, 0.0078125f, 0.984375f);
        energyVertex(v, m, b, color, opacity, 0.4921875f, 0.984375f);
        energyVertex(v, m, c, color, opacity, 0.4921875f, 0.015625f);
        energyVertex(v, m, d, color, opacity, 0.0078125f, 0.015625f);
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
