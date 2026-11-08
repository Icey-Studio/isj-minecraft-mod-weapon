package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.CustomImpactFrameOverlay;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * CLIENT OPENGL RENDERER: HẮC VIÊM HỎA CẦU (DARK FLAME SHOT & PRISON)
 * 1. Khối Cầu Lửa Đen (Black Flame Orb):
 *    - Lõi đen tuyền nuốt chửng ánh sáng (Pitch-Black Abyss Core 0x000000)
 *    - Quầng sáng tím hắc ám (Obsidian Rim 0x300040) cùng vệt khói đen sao chổi
 * 2. Khối Lập Phương Không Gian Giam Hãm 24m (Bán kính 12m):
 *    - Bắt đầu nhỏ 1m rồi bành trướng dữ dội lên 24m
 *    - BÊN NGOÀI HOÀN TOÀN KHÔNG NHÌN VÀO ĐƯỢC: 6 mặt lập phương trở thành MÀN CHẮN ĐEN TUYỀN 100% OPAQUE
 *    - Viền khung 12 cạnh phát quang tím thẫm ma trận Neon Obsidian/Violet
 *    - 8 nút tinh thể ma trận tại 8 đỉnh
 * 3. Hỏa Ngục Lửa Đen Nội Cảnh (True Black Fire Inferno):
 *    - Render chuẩn LỬA ĐEN THẬT SỰ (Chất lửa đen tuyền cuộn xoáy nuốt chửng ánh sáng)
 *    - Lốc xoáy Hắc Viêm 3 tầng lượn sóng cuộn trào từ đáy lên đỉnh
 *    - 12 dải lửa đen bốc cuộn quanh vách ngục
 *    - Hồ hắc hỏa rực cháy dưới đáy
 * 4. Duy trì vĩnh cửu không bao giờ tắt cho đến khi dùng Phong Sát Kĩ Năng
 */
public final class ClientHellflareRenderer {

    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.fromNamespaceAndPath("weapons", "atomic_energy");

    public static class PrisonInstance {
        public final UUID casterUuid;
        public final Vec3 startPos;
        public final Vec3 targetPos;
        public final float maxRadius;
        public final long startTick;
        public boolean expanded = false;
        public boolean dismissing = false;
        public long dismissTick = 0;

        public PrisonInstance(UUID casterUuid, Vec3 startPos, Vec3 targetPos, float maxRadius, long startTick) {
            this.casterUuid = casterUuid;
            this.startPos = startPos;
            this.targetPos = targetPos;
            this.maxRadius = maxRadius;
            this.startTick = startTick;
        }
    }

    private static final List<PrisonInstance> INSTANCES = Collections.synchronizedList(new ArrayList<>());
    private static ClientLevel world = null;

    public static void start(UUID casterUuid, Vec3 startPos, Vec3 targetPos, float radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        world = mc.level;

        synchronized (INSTANCES) {
            INSTANCES.removeIf(inst -> inst.casterUuid.equals(casterUuid));
            INSTANCES.add(new PrisonInstance(casterUuid, startPos, targetPos, radius, world.getGameTime()));
        }
    }

    public static void triggerExpand(UUID casterUuid, Vec3 targetPos, float radius) {
        synchronized (INSTANCES) {
            for (PrisonInstance inst : INSTANCES) {
                if (inst.casterUuid.equals(casterUuid)) {
                    inst.expanded = true;
                    return;
                }
            }
        }
    }

    public static void dismiss(Vec3 targetPos) {
        Minecraft mc = Minecraft.getInstance();
        long now = (mc.level != null) ? mc.level.getGameTime() : 0;

        synchronized (INSTANCES) {
            for (PrisonInstance inst : INSTANCES) {
                if (!inst.dismissing) {
                    inst.dismissing = true;
                    inst.dismissTick = now;
                }
            }
        }
        CustomImpactFrameOverlay.trigger();
    }

    public static void dismiss() {
        dismiss(Vec3.ZERO);
    }

    public static void reset() {
        synchronized (INSTANCES) {
            INSTANCES.clear();
        }
        world = null;
    }

    public static boolean hasActivePrisons() {
        return !INSTANCES.isEmpty();
    }

    public static void render(PoseStack matrices, Camera camera, float delta) {
        if (INSTANCES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.level != world) {
            world = mc.level;
        }

        long gameTime = mc.level.getGameTime();
        Vec3 camPos = camera.getPosition();

        AtomicRenderer.ensureEnergyTexture();

        synchronized (INSTANCES) {
            Iterator<PrisonInstance> it = INSTANCES.iterator();
            while (it.hasNext()) {
                PrisonInstance inst = it.next();
                double age = (gameTime - inst.startTick) + delta;

                if (inst.dismissing) {
                    double dismissAge = (gameTime - inst.dismissTick) + delta;
                    if (dismissAge >= 15.0) {
                        it.remove();
                        continue;
                    }
                }

                if (camPos.distanceToSqr(inst.targetPos) > 320 * 320) continue;

                renderDarkFlameInstance(matrices, camPos, inst, age, delta, gameTime);
            }
        }
    }

    private static void renderDarkFlameInstance(PoseStack matrices, Vec3 camPos, PrisonInstance inst,
                                                double age, float delta, long gameTime) {
        // =========================================================================
        // GIAI ĐOẠN 1: QUẢ CẦU LỬA ĐEN (Age 0 -> 42)
        // =========================================================================
        if (age < 42.0) {
            Vec3 orbPos;
            float orbScale;

            if (age < 28.0) {
                float chargeP = Math.clamp((float) age / 25.0f, 0.0f, 1.0f);
                orbScale = 0.2f + chargeP * 1.4f;
                orbPos = inst.startPos;
            } else {
                float flyP = Math.clamp((float) (age - 28.0) / 12.0f, 0.0f, 1.0f);
                float easeFly = flyP * flyP;
                orbPos = lerpVec(inst.startPos, inst.targetPos, easeFly);
                orbScale = 1.8f;
            }

            renderTrueBlackFlameOrb(matrices, camPos, orbPos, orbScale, age, inst.startPos, inst.targetPos);
        }

        // =========================================================================
        // GIAI ĐOẠN 2 & 3: KHỐI LẬP PHƯƠNG KHÔNG GIAN ĐEN TUYỀN & HỎA NGỤC (Age >= 38)
        // =========================================================================
        if (age >= 38.0) {
            float currentRadius;

            if (inst.dismissing) {
                double dismissAge = (gameTime - inst.dismissTick) + delta;
                float dP = Math.clamp((float) dismissAge / 14.0f, 0.0f, 1.0f);
                float collapseEase = 1.0f - (dP * dP * dP);
                currentRadius = inst.maxRadius * collapseEase;
            } else if (age < 54.0) {
                float expandP = Math.clamp((float) (age - 38.0) / 16.0f, 0.0f, 1.0f);
                float ease = 1.0f - (float) Math.pow(1.0f - expandP, 3.0);
                currentRadius = 0.8f + ease * (inst.maxRadius - 0.8f);
            } else {
                currentRadius = inst.maxRadius;
            }

            if (currentRadius > 0.1f) {
                renderDarkFlamePrison(matrices, camPos, inst.targetPos, currentRadius, age, inst.dismissing);
            }
        }
    }

    /**
     * Render Quả Cầu Lửa Đen (True Black Fire Orb)
     */
    private static void renderTrueBlackFlameOrb(PoseStack matrices, Vec3 camPos, Vec3 orbPos,
                                                float scale, double age, Vec3 startPos, Vec3 targetPos) {
        matrices.pushPose();
        matrices.translate(orbPos.x - camPos.x, orbPos.y - camPos.y, orbPos.z - camPos.z);

        try {
            // Lớp 1: Lõi Lửa Đen Tuyền nuốt chửng ánh sáng (Standard Alpha Blend)
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

            var blackMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            // 4 đĩa lửa đen xoay chéo tạo khối cầu đen kịt
            for (int i = 0; i < 4; i++) {
                double rot = age * 0.14 + (i * Math.PI / 4.0);
                Vec3 u = new Vec3(Math.cos(rot), 0, Math.sin(rot)).scale(scale);
                Vec3 vDir = new Vec3(0, 1, 0).scale(scale);

                // Lõi đen tuyền 0x000000 nuốt trọn ánh sáng
                disc(blackMesh, mat, u.reverse().add(vDir.reverse()), u.add(vDir.reverse()), u.add(vDir), u.reverse().add(vDir), 0x000000, 0.96f);
            }

            var blackBuilt = blackMesh.build();
            if (blackBuilt != null) {
                BufferUploader.drawWithShader(blackBuilt);
            }

            // Lớp 2: Quầng lửa viền tím sẫm hắc ám & vệt sao chổi (Additive Blend)
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            var coronaMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (int i = 0; i < 3; i++) {
                double rot = age * 0.10 + (i * Math.PI / 3.0);
                Vec3 u = new Vec3(Math.cos(rot), 0, Math.sin(rot)).scale(scale * 1.15);
                Vec3 vDir = new Vec3(0, 1, 0).scale(scale * 1.15);
                disc(coronaMesh, mat, u.reverse().add(vDir.reverse()), u.add(vDir.reverse()), u.add(vDir), u.reverse().add(vDir), 0x3A004C, 0.70f);
            }

            // Vệt đuôi sao chổi hắc ám khi bay
            if (age >= 28.0) {
                Vec3 flightDir = targetPos.subtract(startPos).normalize();
                for (int t = 1; t <= 10; t++) {
                    double tailDist = t * 0.9;
                    Vec3 tailPos = flightDir.scale(-tailDist);
                    float tailScale = scale * (1.0f - (t / 11.0f));
                    float tailAlpha = 0.85f * (1.0f - (t / 11.0f));

                    Matrix4f tailMat = new Matrix4f(mat).translate((float) tailPos.x, (float) tailPos.y, (float) tailPos.z);
                    ring(coronaMesh, tailMat, 0.0f, tailScale * 0.9f, 0.18f, tailAlpha * 0.6f);
                }
            }

            // Các tia chớp hắc ám giật quanh quả cầu
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0 + age * 0.25;
                Vec3 p1 = polar(scale * 0.6, Math.sin(a * 2.0) * scale * 0.4, a);
                Vec3 p2 = polar(scale * 1.5, Math.cos(a * 3.0) * scale * 0.7, a + 0.35);
                stroke(coronaMesh, mat, p1, p2, 0.07f, 0.85f);
            }

            var coronaBuilt = coronaMesh.build();
            if (coronaBuilt != null) {
                BufferUploader.drawWithShader(coronaBuilt);
            }

        } finally {
            matrices.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * Render Khối Lập Phương Hắc Ám 24m & Hỏa Ngục Lửa Đen Nội Cảnh
     */
    private static void renderDarkFlamePrison(PoseStack matrices, Vec3 camPos, Vec3 center,
                                              float R, double age, boolean isDismissing) {
        matrices.pushPose();
        matrices.translate(center.x - camPos.x, center.y - camPos.y, center.z - camPos.z);

        try {
            Matrix4f mat = matrices.last().pose();

            // =========================================================================
            // PASS 1: HỎA NGỤC LỬA ĐEN NỘI CẢNH (True Black Fire Interior)
            // Vẽ ngọn lửa đen bên trong trước khi vẽ mặt ngoài hộp
            // =========================================================================
            if (age >= 48.0 && !isDismissing) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

                var fireMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                emitTrueBlackFireInferno(fireMesh, mat, R, age);

                var fireBuilt = fireMesh.build();
                if (fireBuilt != null) {
                    BufferUploader.drawWithShader(fireBuilt);
                }
            }

            // =========================================================================
            // PASS 2: 6 MẶT KHỐI LẬP PHƯƠNG ĐEN TUYỀN TUYỆT ĐỐI (100% Opaque Pitch-Black)
            // YÊU CẦU: BÊN NGOÀI HOÀN TOÀN KHÔNG NHÌN VÀO ĐƯỢC KHI XUẤT HIỆN!
            // depthMask(true) + alpha = 1.0f chặn 100% ánh sáng/geometry bên trong hộp!
            // =========================================================================
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true); // GHI ĐÈ DEPTH BUFFER ĐỂ CHẮN TẦM NHÌN BÊN NGOÀI
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

            var wallMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            emitOpaqueBlackWalls(wallMesh, mat, R, age);

            var wallBuilt = wallMesh.build();
            if (wallBuilt != null) {
                BufferUploader.drawWithShader(wallBuilt);
            }

            // =========================================================================
            // PASS 3: 12 VIỀN PHÁT QUANG MA TRẬN & 8 ĐỈNH LẬP PHƯƠNG (Additive Overlay)
            // Nổi bật hình khối lập phương 24m đen tuyền trên nền thế giới
            // =========================================================================
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.depthMask(false);

            var additiveMesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            // 1. 12 viền khung dây phát quang sắc tím Neon Obsidian/Violet
            emitCubeWireframe(additiveMesh, mat, R, age);

            // 2. 8 nút tinh thể ma trận tại 8 đỉnh
            emitCornerNodes(additiveMesh, mat, R, age);

            // 3. 3 đai phong tỏa xoay quanh các vách ngoài
            ring(additiveMesh, mat, -R * 0.7f, R * 1.03f, 0.20f, 0.85f);
            ring(additiveMesh, mat, 0.0f, R * 1.05f, 0.24f, 0.90f);
            ring(additiveMesh, mat, R * 0.7f, R * 1.03f, 0.20f, 0.85f);

            // Sóng xung kích khi Phong Sát giải trừ
            if (isDismissing) {
                ring(additiveMesh, mat, 0.0f, R * 1.5f, 0.50f, 0.95f);
            }

            var additiveBuilt = additiveMesh.build();
            if (additiveBuilt != null) {
                BufferUploader.drawWithShader(additiveBuilt);
            }

        } finally {
            matrices.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * Vẽ 6 mặt hộp đen tuyền: Lúc mới nở (tick 38) trong suốt, khi nở to ra biến thành ĐEN TUYỀN 100% OPAQUE
     * Bên ngoài hoàn toàn không nhìn vào được!
     */
    private static void emitOpaqueBlackWalls(VertexConsumer v, Matrix4f m, float R, double age) {
        // Độ mờ đục: lúc 1m là 0.45, khi bành trướng đạt 1.0f (ĐEN KỊT HOÀN TOÀN)
        float opacityProgress = Math.clamp((float) (age - 38.0) / 12.0f, 0.0f, 1.0f);
        float alpha = Mth.lerp(opacityProgress, 0.45f, 1.0f);
        int color = 0x000000; // Đen tuyền tuyệt đối

        Vec3 c000 = new Vec3(-R, -R, -R);
        Vec3 c001 = new Vec3(-R, -R, +R);
        Vec3 c010 = new Vec3(-R, +R, -R);
        Vec3 c011 = new Vec3(-R, +R, +R);
        Vec3 c100 = new Vec3(+R, -R, -R);
        Vec3 c101 = new Vec3(+R, -R, +R);
        Vec3 c110 = new Vec3(+R, +R, -R);
        Vec3 c111 = new Vec3(+R, +R, +R);

        // Mặt Đáy & Mặt Nắp
        quadFace(v, m, c000, c100, c101, c001, color, alpha, age);
        quadFace(v, m, c010, c011, c111, c110, color, alpha, age);

        // 4 Mặt Bên
        quadFace(v, m, c000, c010, c110, c100, color, alpha, age);
        quadFace(v, m, c001, c101, c111, c011, color, alpha, age);
        quadFace(v, m, c000, c001, c011, c010, color, alpha, age);
        quadFace(v, m, c100, c110, c111, c101, color, alpha, age);
    }

    private static void quadFace(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                 int color, float alpha, double age) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        float uOffset = (float) (age * 0.012) % 0.25f;
        // UV map vào vùng ma trận năng lượng để có vân gợn sóng đen tuyền chuyển động
        energyVertex(v, m, a, color, opacity, 0.5f + uOffset, 0.05f);
        energyVertex(v, m, b, color, opacity, 0.75f + uOffset, 0.05f);
        energyVertex(v, m, c, color, opacity, 0.75f + uOffset, 0.95f);
        energyVertex(v, m, d, color, opacity, 0.5f + uOffset, 0.95f);
    }

    /**
     * Render LỬA ĐEN THẬT SỰ (TRUE BLACK FLAME INFERNO)
     * Chất lửa đen tuyền cuộn xoáy, nuốt chửng ánh sáng với viền tím sẫm hắc ám
     */
    private static void emitTrueBlackFireInferno(VertexConsumer v, Matrix4f m, float R, double age) {
        // 1. Lốc Xoáy Hắc Viêm 3 Tầng Khổng Lồ (3-Layer Pitch-Black Fire Vortex)
        int segments = 28;
        double rCore = R * 0.35;
        double rMid = R * 0.65;
        double rOuter = R * 0.92;

        for (int i = 0; i < segments; i++) {
            // Tầng 1: Lõi Lửa Đen Tuyền Cuộn Xoáy (Core Jet Black Flames)
            double a1 = i * (2.0 * Math.PI / segments) + (age * 0.09);
            double a2 = (i + 1) * (2.0 * Math.PI / segments) + (age * 0.09);

            double wave1 = Math.sin((-R + 0.3) * 0.4 + age * 0.15) * 0.5;
            double wave2 = Math.sin((+R - 0.3) * 0.4 + age * 0.15) * 0.5;

            Vec3 c1Bot = polar(rCore + wave1, -R + 0.3, a1);
            Vec3 c2Bot = polar(rCore + wave1, -R + 0.3, a2);
            Vec3 c1Top = polar(rCore * 0.8 + wave2, +R - 0.3, a1 + 0.8);
            Vec3 c2Top = polar(rCore * 0.8 + wave2, +R - 0.3, a2 + 0.8);

            // Lửa đen tuyền 0x000000 nuốt trọn không gian bên trong
            quad(v, m, c1Bot, c2Bot, c2Top, c1Top, 0x000000, 0.95f);
            // Lớp viền mờ tím sẫm huyền bí
            quad(v, m, c1Bot.scale(0.92), c2Bot.scale(0.92), c2Top.scale(0.92), c1Top.scale(0.92), 0x1A0028, 0.65f);

            // Tầng 2: Vỏ Lửa Đen Cuộn Ngược Chiều (Outer Black Fire Sheet)
            double b1 = i * (2.0 * Math.PI / segments) - (age * 0.06);
            double b2 = (i + 1) * (2.0 * Math.PI / segments) - (age * 0.06);

            Vec3 m1Bot = polar(rMid, -R + 0.3, b1);
            Vec3 m2Bot = polar(rMid, -R + 0.3, b2);
            Vec3 m1Top = polar(rMid * 0.9, +R - 0.3, b1 - 0.7);
            Vec3 m2Top = polar(rMid * 0.9, +R - 0.3, b2 - 0.7);

            quad(v, m, m1Bot, m2Bot, m2Top, m1Top, 0x020005, 0.88f);
        }

        // 2. 12 Dải Lửa Đen Bốc Cuộn Dọc Vách Hộp (Rising Black Flame Ribbons)
        for (int i = 0; i < 12; i++) {
            double angle = i * (Math.PI / 6.0) + (age * 0.02);
            double ribbonWave = Math.sin(age * 0.12 + i * 1.5) * 0.6;
            Vec3 rBot = polar(rOuter, -R + 0.3, angle);
            Vec3 rTop = polar(rOuter * 0.95, +R - 0.3, angle + 0.4 + ribbonWave * 0.1);

            strokeBlackFlame(v, m, rBot, rTop, 0.55f, 0.92f);
        }

        // 3. Hồ Hắc Hỏa Rực Cháy Dưới Sàn (Floor of Black Magma & Fire Pool)
        float floorRadius = R * 0.94f;
        ringBlack(v, m, -R + 0.2f, floorRadius, 0.35f, 0.95f);
        ringBlack(v, m, -R + 0.2f, floorRadius * 0.65f, 0.28f, 0.90f);
        ringBlack(v, m, -R + 0.2f, floorRadius * 0.35f, 0.22f, 0.85f);

        for (int i = 0; i < 12; i++) {
            double spokeAngle = i * (Math.PI / 6.0) + (age * 0.015);
            Vec3 sp1 = polar(floorRadius * 0.1, -R + 0.2f, spokeAngle);
            Vec3 sp2 = polar(floorRadius * 0.98, -R + 0.2f, spokeAngle);
            strokeBlackFlame(v, m, sp1, sp2, 0.20f, 0.90f);
        }
    }

    /**
     * Dải lửa đen chuẩn: Lõi đen tuyền 0x000000 + viền tím sẫm 0x2A0035
     */
    private static void strokeBlackFlame(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, float alpha) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 0.0001) return;
        Vec3 norm = dir.scale(1.0 / len);

        Vec3 up = Math.abs(norm.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = norm.cross(up).normalize();

        Vec3 n = side.scale(width);
        // Lõi đen tuyền tuyệt đối
        quad(v, m, a.subtract(n), a.add(n), b.add(n), b.subtract(n), 0x000000, alpha);
        // Viền tím sẫm mờ
        Vec3 nOuter = side.scale(width * 1.6);
        quad(v, m, a.subtract(nOuter), a.add(nOuter), b.add(nOuter), b.subtract(nOuter), 0x220030, alpha * 0.45f);
    }

    private static void ringBlack(VertexConsumer v, Matrix4f m, float y, float radius, float thickness, float alpha) {
        int segs = 32;
        for (int i = 0; i < segs; i++) {
            double a1 = i * (2.0 * Math.PI / segs);
            double a2 = (i + 1) * (2.0 * Math.PI / segs);

            Vec3 in1 = polar(radius - thickness, y, a1);
            Vec3 out1 = polar(radius + thickness, y, a1);
            Vec3 out2 = polar(radius + thickness, y, a2);
            Vec3 in2 = polar(radius - thickness, y, a2);

            disc(v, m, in1, out1, out2, in2, 0x000000, alpha);
        }
    }

    /**
     * 12 Viền phát quang khung lập phương (Sắc tím Neon Obsidian)
     */
    private static void emitCubeWireframe(VertexConsumer v, Matrix4f m, float R, double age) {
        Vec3 c000 = new Vec3(-R, -R, -R);
        Vec3 c001 = new Vec3(-R, -R, +R);
        Vec3 c010 = new Vec3(-R, +R, -R);
        Vec3 c011 = new Vec3(-R, +R, +R);
        Vec3 c100 = new Vec3(+R, -R, -R);
        Vec3 c101 = new Vec3(+R, -R, +R);
        Vec3 c110 = new Vec3(+R, +R, -R);
        Vec3 c111 = new Vec3(+R, +R, +R);

        float edgeWidth = 0.32f;
        float alpha = 0.95f;

        // 4 cạnh đáy
        stroke(v, m, c000, c100, edgeWidth, alpha);
        stroke(v, m, c100, c101, edgeWidth, alpha);
        stroke(v, m, c101, c001, edgeWidth, alpha);
        stroke(v, m, c001, c000, edgeWidth, alpha);

        // 4 cạnh nắp
        stroke(v, m, c010, c110, edgeWidth, alpha);
        stroke(v, m, c110, c111, edgeWidth, alpha);
        stroke(v, m, c111, c011, edgeWidth, alpha);
        stroke(v, m, c011, c010, edgeWidth, alpha);

        // 4 cột đứng
        stroke(v, m, c000, c010, edgeWidth, alpha);
        stroke(v, m, c100, c110, edgeWidth, alpha);
        stroke(v, m, c101, c111, edgeWidth, alpha);
        stroke(v, m, c001, c011, edgeWidth, alpha);
    }

    private static void emitCornerNodes(VertexConsumer v, Matrix4f m, float R, double age) {
        float nodeRadius = 0.85f;
        double sign[] = {-1.0, 1.0};

        for (double sx : sign) {
            for (double sy : sign) {
                for (double sz : sign) {
                    Vec3 pos = new Vec3(sx * R, sy * R, sz * R);
                    Matrix4f nodeMat = new Matrix4f(m).translate((float) pos.x, (float) pos.y, (float) pos.z);

                    for (int i = 0; i < 3; i++) {
                        double rot = age * 0.08 + (i * Math.PI / 3.0);
                        Vec3 u = new Vec3(Math.cos(rot), 0, Math.sin(rot)).scale(nodeRadius);
                        Vec3 vDir = new Vec3(0, 1, 0).scale(nodeRadius);
                        disc(v, nodeMat, u.reverse().add(vDir.reverse()), u.add(vDir.reverse()), u.add(vDir), u.reverse().add(vDir), 0x8A2BE2, 0.85f);
                    }
                }
            }
        }
    }

    // =========================================================================
    // TIỆN ÍCH HÌNH HỌC OPENGL
    // =========================================================================

    private static void stroke(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, float alpha) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 0.0001) return;
        Vec3 norm = dir.scale(1.0 / len);

        Vec3 up = Math.abs(norm.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = norm.cross(up).normalize();
        Vec3 other = norm.cross(side).normalize();

        for (int layer = 2; layer >= 0; layer--) {
            double w = width * (layer == 2 ? 3.5 : layer == 1 ? 1.8 : 0.85);
            float opacity = alpha * (layer == 2 ? 0.08f : layer == 1 ? 0.32f : 0.95f);
            int color = layer == 0 ? 0xFFE0FF : (layer == 1 ? 0xDA70D6 : 0x8A2BE2);

            for (int axis = 0; axis < 2; axis++) {
                Vec3 n = (axis == 0 ? side : other).scale(w);
                quad(v, m, a.subtract(n), a.add(n), b.add(n), b.subtract(n), color, opacity);
            }
        }
    }

    private static void ring(VertexConsumer v, Matrix4f m, float y, float radius, float thickness, float alpha) {
        int segs = 36;
        for (int i = 0; i < segs; i++) {
            double a1 = i * (2.0 * Math.PI / segs);
            double a2 = (i + 1) * (2.0 * Math.PI / segs);

            Vec3 in1 = polar(radius - thickness, y, a1);
            Vec3 out1 = polar(radius + thickness, y, a1);
            Vec3 out2 = polar(radius + thickness, y, a2);
            Vec3 in2 = polar(radius - thickness, y, a2);

            disc(v, m, in1, out1, out2, in2, 0x8A2BE2, alpha);
        }
    }

    private static void disc(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        energyVertex(v, m, a, color, opacity, 0.0078125f, 0.984375f);
        energyVertex(v, m, b, color, opacity, 0.4921875f, 0.984375f);
        energyVertex(v, m, c, color, opacity, 0.4921875f, 0.015625f);
        energyVertex(v, m, d, color, opacity, 0.0078125f, 0.015625f);
    }

    private static void quad(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        energyVertex(v, m, a, color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, b, color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, c, color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, d, color, opacity, 0.875f, 0.5f);
    }

    private static void energyVertex(VertexConsumer v, Matrix4f m, Vec3 p, int color, int opacity, float u, float texV) {
        v.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, opacity)
                .setUv(u, texV);
    }

    private static Vec3 polar(double radius, double y, double angle) {
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static Vec3 lerpVec(Vec3 a, Vec3 b, float t) {
        return new Vec3(
                Mth.lerp(t, a.x, b.x),
                Mth.lerp(t, a.y, b.y),
                Mth.lerp(t, a.z, b.z)
        );
    }
}
