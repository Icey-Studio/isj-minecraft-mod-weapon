package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.config.SkillVisualConfig;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CLIENT OPENGL ADDITIVE BLENDING RENDERER: TỔNG KHO KỈ XẢO ĐIỆN ẢNH 3D CHO TOÀN BỘ KỸ NĂNG
 * 1. Linh Tử Băng Hoại (Disintegration): 5 Hồi chuẩn nguyên tác Hinata (Thất sắc quang kiếm, 7 cột plasma cầu vồng, siêu quang trụ 384m, bụi vàng thăng thiên).
 * 2. Tà Khứ Vũ Thê Tử (Jacob's Ladder): Thang trời xoắn ốc 256m, 12 cánh thiên thần ảo ảnh đập cánh, thánh quang hoàng kim.
 * 3. Phép Màu Thần Linh (Purification Pillar): Trụ pha lê bát giác 180m xoay tròn, 12 tơ ánh sáng DNA, hồ thánh thủy lục giác.
 * 4. Đức Tin & Ân Sủng (Faith and Grace): Mái vòm thánh đường ánh sáng Gothic 48m che chở toàn đội.
 * 5. Bạo Thực Vương (Beelzebuth): Hắc động thôn phệ 4 tầng, đĩa bồi tụ xoay tít, 8 xúc tu hư không cào xé không gian (ổn định, giảm rung lắc).
 * 6. Long Tinh Bộc Viêm Bá (Dragon Nova): Cầu plasma Starlight hạt nhân nén cực hạn rồi bung nổ sóng xung kích cầu 72m.
 * 7. Tuyệt Diệt Tinh Tú (Megiddo): Hàng trăm thấu kính nước tầng mây hội tụ chùm tia laser mặt trời bắn tỉa dồn dập.
 * 8. Thị Thực (Seer Flesh): Nâng cấp hình dạng cũ thành cấu trúc ma nhãn 3D phát quang lập thể đa chiều bí ẩn.
 * 9. Diệt Thế Tà Tinh (Alkaid): Đại thiên thạch 36m dung nham tím rực cháy đuôi sao băng 120m.
 * 10. Cú Bắn Granit (Granite Blast): Pháo nón năng lượng siêu thanh 100m kèm 6 vòng sóng nổ Mach Rings.
 * 11. Nghịch Thời Nhất Trảm: Không gian ngưng đọng trắng đen với vệt kiếm nứt vỡ không gian tím.
 * 12. Bạt Đao Thuật Vận Tốc Ngược: 3 dư ảnh kiếm trận tốc biến tam giác.
 * 13. Đa Trùng Kết Giới: Nâng cấp hình dạng yêu thích thành 8 lớp khiên lục lăng lồng nhau xoay so le 3D cực đẹp.
 * 14. Lồng Giam Vô Hạn: Khối đa diện kim cương 20 mặt lấp lánh cầu vồng giam cầm vĩnh cửu.
 * 15. Cảm Nhận Vạn Năng: Sóng Sonar Topology Radar quét xanh ngọc.
 * 16. Uriel Defense: Tam trùng vành đai thái dương hỏa quầng hộ thân.
 * 17. Rykuzin Sinka: Bóng ma dự đoán tương lai 3D xuất hiện trước 0.5s.
 * 18. Zone Track: Chùm tử quang lượng tử 150m xuyên thủng không gian.
 * 19. Phong Sát Kĩ Năng: Vòng tròn chân không 64m quét phẳng triệt tiêu mọi kết giới.
 */
public final class ClientCinematicSkillRenderer {

    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.fromNamespaceAndPath("weapons", "atomic_energy");

    public record ActiveSkillInstance(
            int skillId,
            UUID casterUuid,
            Vec3 center,
            Vec3 direction,
            int totalTicks,
            long startTick,
            float powerMultiplier
    ) {
        public float getAge(float partialTicks) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return -1;
            return (float) (mc.level.getGameTime() - startTick) + partialTicks;
        }

        public boolean isExpired(float partialTicks) {
            float age = getAge(partialTicks);
            return age < 0 || age >= totalTicks;
        }
    }

    public record SkillInstanceKey(int skillId, UUID casterUuid, int blockX, int blockY, int blockZ) {
        public static SkillInstanceKey of(int skillId, UUID caster, Vec3 pos) {
            if (skillId == SkillVisualConfig.SKILL_MULTILAYER_BARRIER || skillId == SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON) {
                return new SkillInstanceKey(skillId, null, (int) Math.round(pos.x), (int) Math.round(pos.y), (int) Math.round(pos.z));
            }
            return new SkillInstanceKey(skillId, caster, (int) Math.round(pos.x), (int) Math.round(pos.y), (int) Math.round(pos.z));
        }
    }

    private static final Map<SkillInstanceKey, ActiveSkillInstance> activeSkills = new ConcurrentHashMap<>();

    public static void receive(int skillId, UUID caster, Vec3 center, Vec3 dir, int durationTicks, float power) {
        if (!SkillVisualConfig.isCinematicEnabled(skillId)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        SkillInstanceKey key = SkillInstanceKey.of(skillId, caster, center);
        activeSkills.put(key, new ActiveSkillInstance(
                skillId, caster, center, dir, durationTicks, mc.level.getGameTime(), power
        ));
    }

    public static void stop(int skillId) {
        activeSkills.entrySet().removeIf(entry -> entry.getKey().skillId() == skillId);
    }

    public static void stop(int skillId, Vec3 pos) {
        if (pos == null || pos.lengthSqr() < 0.01) {
            stop(skillId);
            return;
        }
        activeSkills.entrySet().removeIf(entry -> {
            if (entry.getKey().skillId() != skillId) return false;
            return entry.getValue().center().distanceToSqr(pos) <= 64.0;
        });
    }

    public static void triggerImpact(int skillId, Vec3 impactPos) {
        // Impact hook
    }

    public static void reset() {
        activeSkills.clear();
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (activeSkills.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = camera.getPosition();
        AtomicRenderer.ensureEnergyTexture();

        // Dọn dẹp các instance đã hết hạn
        activeSkills.entrySet().removeIf(entry -> entry.getValue().isExpired(tickDelta));

        for (ActiveSkillInstance instance : activeSkills.values()) {
            float age = instance.getAge(tickDelta);
            if (age < 0 || age >= instance.totalTicks()) continue;

            renderSingleSkill(matrices, camPos, instance, age);
        }
    }

    private static void renderSingleSkill(PoseStack matrices, Vec3 camPos, ActiveSkillInstance skill, float age) {
        Vec3 pos = skill.center();
        matrices.pushPose();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ENERGY_TEXTURE);

        try {
            var mesh = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f mat = matrices.last().pose();

            switch (skill.skillId()) {
                case SkillVisualConfig.SKILL_DISINTEGRATION -> renderDisintegration(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_JACOBS_LADDER -> renderJacobsLadder(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_PURIFICATION_PILLAR -> renderPurificationPillar(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_FAITH_AND_GRACE -> renderFaithAndGrace(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_BEELZEBUTH -> renderBeelzebuth(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_DRAGON_NOVA -> renderDragonNova(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_EXTINCTION_STARS -> renderMegiddo(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_SEER_FLESH -> renderSeerFlesh(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_ALKAID -> renderAlkaid(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_GRANITE_BLAST -> renderGraniteBlast(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_REVERSE_TIME_SLASH -> renderReverseTimeSlash(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_REVERSE_VELOCITY_IAI -> renderReverseVelocityIai(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON -> renderDragonPrison(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_MULTILAYER_BARRIER -> renderMultilayerBarrier(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_UNIVERSAL_SENSE -> renderUniversalSense(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_URIEL_DEFENSE -> renderUrielDefense(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_RYKUZIN_SINKA -> renderRykuzinSinka(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_ZONE_TRACK -> renderZoneTrack(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_NULLIFICATION -> renderNullification(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_HELLFLARE -> renderHellflare(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_PRIMORDIAL_DEMON -> renderPrimordialDemon(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY -> renderDimensionalGate(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_SPIRITRON_OUTBURST -> renderSpiritronOutburst(mesh, mat, skill, age);
                case SkillVisualConfig.SKILL_CHROMATIC_PILLAR -> renderChromaticPillar(mesh, mat, skill, age);
            }

            var built = mesh.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            matrices.popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }

    // =========================================================================
    // 1. LINH TỬ BĂNG HOẠI (DISINTEGRATION - 5 HỒI & MA TRẬN VÒNG TRÒN XẾP CHỒNG NHAU)
    // =========================================================================
    private static void renderDisintegration(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float rMax = 18.0f * skill.powerMultiplier(); // Bán kính mở rộng lên 18m
        float hMax = 384.0f; // Cột quang xuyên thẳng tầng bình lưu

        // Hồi 1 & 2 (0 -> 48t): Địa trận Thánh Vực và CÁC VÒNG TRÒN MA THUẬT XẾP CHỒNG NHAU (STACKED RUNIC CIRCLES)
        if (age < 48.0f) {
            float progress = Math.min(1.0f, age / 30.0f);
            float curR = rMax * progress;

            // 5 Tầng Vòng Tròn Ma Thuật Xếp Chồng Nhau theo trục thẳng đứng (Y+0.08m -> Y+7.8m)
            float[] ringHeights = {0.08f, 1.8f, 3.8f, 5.8f, 7.8f};
            float[] ringRadii = {curR, curR * 0.88f, curR * 0.74f, curR * 0.60f, curR * 0.46f};
            for (int c = 0; c < ringHeights.length; c++) {
                float y = ringHeights[c];
                float r = ringRadii[c];
                float rot = (c % 2 == 0 ? 1 : -1) * (age * (0.035f + c * 0.008f));
                int color = (c % 2 == 0) ? 0xFFEA00 : 0xFFD700;
                renderHexagonRing(v, m, y, r, 0.35f, color, 0.85f * progress, rot);

                // Họa tiết cổ ngữ sao lục giác & thập nhị giác đan xen bên trong mỗi tầng vòng ma thuật
                if (c % 2 == 0) {
                    for (int st = 0; st < 6; st++) {
                        double a1 = st * Math.PI / 3.0 + rot;
                        double a2 = a1 + Math.PI * 2.0 / 3.0;
                        Vec3 sp1 = new Vec3(Math.cos(a1) * r, y, Math.sin(a1) * r);
                        Vec3 sp2 = new Vec3(Math.cos(a2) * r, y, Math.sin(a2) * r);
                        stroke(v, m, sp1, sp2, 0.12f, 0.75f * progress, 0xFFFFFF);
                    }
                }
            }

            // Các sợi tơ laser thánh quang thẳng đứng liên kết 5 tầng vòng tròn ma thuật thành lồng giam thánh vực
            for (int k = 0; k < 12; k++) {
                double a = k * Math.PI * 2.0 / 12.0;
                Vec3 bot = new Vec3(Math.cos(a) * curR, 0.08, Math.sin(a) * curR);
                Vec3 top = new Vec3(Math.cos(a) * ringRadii[4], ringHeights[4], Math.sin(a) * ringRadii[4]);
                stroke(v, m, bot, top, 0.16f, 0.80f * progress, 0xFFFFFF);
            }

            // 7 Màng kết giới lục lăng 3D mọc lên từ sàn đất giam cầm mục tiêu
            if (age >= 15.0f) {
                float cageProg = Math.min(1.0f, (age - 15.0f) / 25.0f);
                float cageH = 45.0f * cageProg;
                for (int i = 0; i < 7; i++) {
                    double angle = i * Math.PI * 2.0 / 7.0;
                    float pillarX = (float) (Math.cos(angle) * rMax * 0.95);
                    float pillarZ = (float) (Math.sin(angle) * rMax * 0.95);
                    cylinderOffset(v, m, pillarX, pillarZ, 0.35f, cageH, 0.85f * cageProg, 0xFFEA00);
                }
            }
        }
        // Hồi 3 (48 -> 72t): Thất Kiếm Trảm Không (7 Vệt chém Thất Sắc rạch trời vẽ Thất Tinh Trận)
        if (age >= 38.0f && age < 80.0f) {
            float slashProgress = Math.min(1.0f, (age - 38.0f) / 24.0f);
            int[] rainbowColors = {0xFF1744, 0xFF9100, 0xFFEA00, 0x00E676, 0x00E5FF, 0x2979FF, 0xD500F9};
            for (int i = 0; i < 7; i++) {
                double a = i * Math.PI * 2.0 / 7.0 + age * 0.02;
                Vec3 p1 = new Vec3(Math.cos(a) * (rMax * 1.8), 85.0 - slashProgress * 45.0, Math.sin(a) * (rMax * 1.8));
                Vec3 p2 = new Vec3(Math.cos(a + Math.PI) * (rMax * 0.4), 8.0, Math.sin(a + Math.PI) * (rMax * 0.4));
                stroke(v, m, p1, p2, 0.35f, 0.95f, rainbowColors[i]);
            }
        }
        // Hồi 4 (72 -> 100t): Thất Sắc Hợp Nhất & Siêu Quang Trụ Diệt Nguyên Tử 384m (32-Sided Smooth GPU Cylinder)
        if (age >= 68.0f && age < 105.0f) {
            float beamProg = Math.min(1.0f, (age - 68.0f) / 10.0f);
            float fade = (age < 95.0f) ? 1.0f : (float) Math.max(0.0, 1.0 - (age - 95.0f) / 10.0f);

            // 1. Cột lõi siêu năng lượng trắng chói lòa xóa sạch nguyên tử
            cylinder(v, m, rMax * 0.45f * beamProg, hMax, 0.98f * fade, 0xFFFFFF, age);
            // 2. Vỏ quang trụ vàng kim rực rỡ
            cylinder(v, m, rMax * 0.85f * beamProg, hMax, 0.75f * fade, 0xFFEA00, age + 5);
            // 3. Vầng hào quang Thất Sắc cầu vồng đa tầng
            cylinder(v, m, rMax * 1.35f * beamProg, hMax, 0.35f * fade, 0x00E5FF, age + 10);

            // 12 Đĩa sóng xung kích giáng lâm dồn dập từ trời cao
            for (int k = 0; k < 4; k++) {
                float ringY = ((age * 4.5f + k * 18.0f) % 65.0f);
                float ringR = rMax * (1.0f + (65.0f - ringY) / 65.0f * 0.6f);
                ring(v, m, ringY, ringR, 0.45f, 0xFFEA00, fade * 0.85f);
            }
        }
        // Hồi 5 (96 -> 125t): Tinh thể bụi vàng thăng thiên (Golden Spiritron Ascension)
        if (age >= 96.0f) {
            float spiritFade = (float) Math.max(0.0, 1.0 - (age - 96.0f) / 29.0f);
            for (int s = 0; s < 48; s++) {
                double a = s * 0.61803398875 * Math.PI * 2.0;
                double dist = (s % 5) * (rMax / 5.0) + (age - 96.0f) * 0.15;
                double spY = ((age - 96.0f) * 2.5 + s * 1.8) % 55.0;
                billboardDisc(v, m, new Vec3(Math.cos(a) * dist, spY, Math.sin(a) * dist), 0.45f, 0xFFD700, spiritFade * 0.8f);
            }
        }
    }



    // =========================================================================
    // 2. TÀ KHỨ VŨ THÊ TỬ (JACOB'S LADDER - SPIRAL STAIRWAY & 12 ANGEL WINGS)
    // =========================================================================
    private static void renderJacobsLadder(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float r = 24.0f * skill.powerMultiplier();
        float h = 256.0f;
        float fade = (age < skill.totalTicks() - 15) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 15.0f);

        // 1. Cột quang trụ thánh thiên ở tâm
        cylinder(v, m, 4.5f, h, 0.95f * fade, 0xFFFFFF, age);
        cylinder(v, m, 8.5f, h, 0.65f * fade, 0x00E5FF, age + 4);
        cylinder(v, m, 16.0f, h, 0.35f * fade, 0x76FF03, age + 8);

        // 2. Cầu thang ánh sáng xoắn ốc 3D vươn lên mây
        int steps = 40;
        for (int i = 0; i < steps; i++) {
            double angle = (i * 0.35) + age * 0.03;
            double stepY = i * (h / steps);
            Vec3 p1 = new Vec3(Math.cos(angle) * 3.5, stepY, Math.sin(angle) * 3.5);
            Vec3 p2 = new Vec3(Math.cos(angle) * 11.5, stepY, Math.sin(angle) * 11.5);
            stroke(v, m, p1, p2, 0.45f, 0.9f * fade, 0xFFD700);
        }

        // 3. 12 Cánh Thiên Thần ảo ảnh 3D đập cánh dọc thân thang
        for (int w = 0; w < 6; w++) {
            double wingY = 18.0 + w * 28.0;
            float wingFlap = (float) Math.sin(age * 0.25f + w * 1.2f) * 0.35f;
            // Cánh trái
            Vec3 wL1 = new Vec3(-4.0, wingY, 0);
            Vec3 wL2 = new Vec3(-18.0, wingY + 8.0 + wingFlap * 4.0, -4.0);
            stroke(v, m, wL1, wL2, 0.8f, 0.75f * fade, 0x00E5FF);
            // Cánh phải
            Vec3 wR1 = new Vec3(4.0, wingY, 0);
            Vec3 wR2 = new Vec3(18.0, wingY + 8.0 + wingFlap * 4.0, -4.0);
            stroke(v, m, wR1, wR2, 0.8f, 0.75f * fade, 0x00E5FF);
        }
    }

    // =========================================================================
    // 3. PHÉP MÀU THẦN LINH (PURIFICATION PILLAR - OCTAGONAL CRYSTAL & SACRED POOL)
    // =========================================================================
    private static void renderPurificationPillar(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float r = 20.0f * skill.powerMultiplier();
        float h = 180.0f;
        float fade = (age < skill.totalTicks() - 15) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 15.0f);

        // 1. Hồ thánh thủy lục giác phản xạ sóng gợn trên mặt đất
        for (int i = 0; i < 3; i++) {
            float poolR = ((age * 1.5f + i * 6.0f) % r);
            float poolAlpha = (1.0f - poolR / r) * fade * 0.75f;
            renderHexagonRing(v, m, 0.06f + i * 0.04f, poolR, 0.35f, 0x76FF03, poolAlpha, age * 0.02f);
        }

        // 2. Trụ pha lê bát giác 3D xoay tròn
        cylinder(v, m, 5.0f, h, 0.92f * fade, 0xFFFFFF, age);
        cylinder(v, m, 9.5f, h, 0.65f * fade, 0x76FF03, age + 5);
        cylinder(v, m, 15.0f, h, 0.30f * fade, 0x00E676, age + 10);

        // 3. 12 Sợi tơ ánh sáng bạc DNA xoắn quanh thân trụ
        for (int i = 0; i < 6; i++) {
            double a1 = i * Math.PI / 3.0 + age * 0.05;
            double a2 = a1 + Math.PI;
            for (int k = 0; k < 12; k++) {
                double y1 = k * (h / 12.0);
                double y2 = (k + 1) * (h / 12.0);
                Vec3 p1 = new Vec3(Math.cos(a1 + k * 0.4) * 9.8, y1, Math.sin(a1 + k * 0.4) * 9.8);
                Vec3 p2 = new Vec3(Math.cos(a1 + (k + 1) * 0.4) * 9.8, y2, Math.sin(a1 + (k + 1) * 0.4) * 9.8);
                stroke(v, m, p1, p2, 0.15f, 0.85f * fade, 0xFFFFFF);
            }
        }
    }



    // =========================================================================
    // 4. ĐỨC TIN & ÂN SỦNG (FAITH AND GRACE - 3D GRAND CATHEDRAL OF LIGHT)
    // =========================================================================
    private static void renderFaithAndGrace(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float r = 48.0f * skill.powerMultiplier();
        float h = 38.0f;
        float fade = (age < skill.totalTicks() - 15) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 15.0f);

        // 1. Hồ thánh quang trên sàn đất
        for (int i = 0; i < 4; i++) {
            float poolR = ((age * 1.8f + i * 12.0f) % r);
            float poolAlpha = (1.0f - poolR / r) * fade * 0.65f;
            renderHexagonRing(v, m, 0.08f, poolR, 0.45f, 0xFFD700, poolAlpha, age * 0.015f);
        }

        // 2. 4 Tháp chuông quang trụ thánh đường ở 4 góc chính (North, South, East, West) cao 38m
        float[] spireX = {r * 0.72f, -r * 0.72f, 0, 0};
        float[] spireZ = {0, 0, r * 0.72f, -r * 0.72f};
        for (int sp = 0; sp < 4; sp++) {
            cylinderOffset(v, m, spireX[sp], spireZ[sp], 1.4f, h, fade * 0.85f, 0xFFD700);
            cylinderOffset(v, m, spireX[sp], spireZ[sp], 0.6f, h + 6.0f, fade * 0.95f, 0xFFFFFF); // Mũi chóp nhọn
        }

        // 3. Vòm Gothic nối các tháp chuông
        for (int sp = 0; sp < 4; sp++) {
            int nextSp = (sp + 1) % 4;
            Vec3 p1 = new Vec3(spireX[sp], h * 0.75, spireZ[sp]);
            Vec3 p2 = new Vec3(spireX[nextSp], h * 0.75, spireZ[nextSp]);
            Vec3 apexArch = new Vec3((spireX[sp] + spireX[nextSp]) * 0.5, h, (spireZ[sp] + spireZ[nextSp]) * 0.5);
            stroke(v, m, p1, apexArch, 0.35f, fade * 0.80f, 0xFFD700);
            stroke(v, m, apexArch, p2, 0.35f, fade * 0.80f, 0xFFD700);
        }

        // 4. Mái vòm thánh đường ánh sáng Gothic
        int ribs = 12;
        for (int i = 0; i < ribs; i++) {
            double a = i * Math.PI * 2.0 / ribs;
            Vec3 base = new Vec3(Math.cos(a) * r, 0.1, Math.sin(a) * r);
            Vec3 mid = new Vec3(Math.cos(a) * (r * 0.75), h * 0.65, Math.sin(a) * (r * 0.75));
            Vec3 top = new Vec3(0, h, 0);
            stroke(v, m, base, mid, 0.28f, 0.75f * fade, 0xFF4081);
            stroke(v, m, mid, top, 0.28f, 0.85f * fade, 0xFFD700);
        }

        // 5. Thập Tự Giá Thánh Quang và Vòng Nhẫn Hào Quang trên đỉnh mái vòm
        ring(v, m, h, 6.5f, 0.40f, 0xFFD700, 0.95f * fade);
        ring(v, m, h + 2.5f, 3.8f, 0.30f, 0xFFFFFF, 0.98f * fade);
        // Thập tự giá
        stroke(v, m, new Vec3(0, h - 1.0, 0), new Vec3(0, h + 8.0, 0), 0.35f, fade * 0.98f, 0xFFFFFF);
        stroke(v, m, new Vec3(-3.0, h + 5.0, 0), new Vec3(3.0, h + 5.0, 0), 0.35f, fade * 0.98f, 0xFFFFFF);

        // 6. Vũ điệu lông vũ thánh quang và hạt thánh giá rơi lơ lửng bên trong
        for (int f = 0; f < 24; f++) {
            double fa = f * 1.618 * Math.PI * 2.0;
            double fDist = (f % 6) * (r / 6.5);
            double fy = ((age * 0.8f + f * 3.5f) % h);
            billboardDisc(v, m, new Vec3(Math.cos(fa) * fDist, h - fy, Math.sin(fa) * fDist), 0.55f, 0xFFFFFF, fade * 0.85f);
        }
    }

    // =========================================================================
    // 5. BẠO THỰC VƯƠNG (BEELZEBUTH - VOID DRAGON MAW & COSMIC ACCRETION CORONA)
    // =========================================================================
    private static void renderBeelzebuth(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float r = 36.0f * skill.powerMultiplier();
        float fade = (age < skill.totalTicks() - 10) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 10.0f);

        // 1. Khối cầu chân trời sự kiện hắc ám (Event Horizon Singularity)
        billboardDisc(v, m, new Vec3(0, 3.5, 0), 7.5f, 0x0A0012, fade);
        billboardDisc(v, m, new Vec3(0, 3.5, 0), 9.0f, 0x4A148C, fade * 0.75f);

        // 2. Đĩa bồi tụ Plasma xoay tít tốc độ cao (Accretion Disk)
        for (int ringIdx = 0; ringIdx < 5; ringIdx++) {
            float ringR = 8.0f + ringIdx * 6.0f;
            float rotSpeed = (5 - ringIdx) * 0.09f;
            int ringColor = (ringIdx % 2 == 0) ? 0x9C27B0 : 0xFF1744;
            renderHexagonRing(v, m, 3.5f, ringR, 0.40f, ringColor, 0.85f * fade, age * rotSpeed);
        }

        // 3. 3D VOID DRAGON MAW: HÀM RỒNG HƯ KHÔNG LAO TỚI THÔN PHỆ KHÔNG GIAN
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();
        Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = d.cross(up).normalize();
        Vec3 upVec = d.cross(right).normalize();

        Vec3 jawBase = new Vec3(0, 3.5, 0).add(d.scale(3.0));
        float biteAngle = (float) Math.sin(age * 0.32f) * 0.35f + 0.45f; // Hoạt ảnh hàm rồng ngoạm
        float jawLen = 14.0f;

        // Hàm trên (Upper Jaw)
        Vec3 upperTip = jawBase.add(d.scale(jawLen)).add(upVec.scale(jawLen * (float) Math.sin(biteAngle)));
        Vec3 upperLeft = jawBase.add(right.scale(4.5)).add(upVec.scale(2.5));
        Vec3 upperRight = jawBase.add(right.scale(-4.5)).add(upVec.scale(2.5));
        stroke(v, m, upperLeft, upperTip, 0.45f, fade * 0.95f, 0xBA68C8);
        stroke(v, m, upperRight, upperTip, 0.45f, fade * 0.95f, 0xBA68C8);

        // Hàm dưới (Lower Jaw)
        Vec3 lowerTip = jawBase.add(d.scale(jawLen)).add(upVec.scale(-jawLen * (float) Math.sin(biteAngle)));
        Vec3 lowerLeft = jawBase.add(right.scale(3.8)).add(upVec.scale(-2.0));
        Vec3 lowerRight = jawBase.add(right.scale(-3.8)).add(upVec.scale(-2.0));
        stroke(v, m, lowerLeft, lowerTip, 0.45f, fade * 0.95f, 0xBA68C8);
        stroke(v, m, lowerRight, lowerTip, 0.45f, fade * 0.95f, 0xBA68C8);

        // Nanh rồng hư không (Void Dragon Fangs)
        stroke(v, m, upperTip, upperTip.add(upVec.scale(-3.0)), 0.30f, fade * 0.98f, 0xFFFFFF);
        stroke(v, m, lowerTip, lowerTip.add(upVec.scale(2.5)), 0.30f, fade * 0.98f, 0xFFFFFF);

        // 4. Sấm sét thứ nguyên (Dimensional Lightning) xé rách không gian
        for (int l = 0; l < 8; l++) {
            double baseA = l * Math.PI / 4.0 + age * 0.05;
            Vec3 p0 = new Vec3(0, 3.5, 0);
            for (int seg = 1; seg <= 5; seg++) {
                double dist = seg * (r / 5.0);
                double jitterX = Math.sin(age * 0.8 + l * 4.0 + seg * 2.0) * 1.5;
                double jitterY = Math.cos(age * 0.6 + l * 3.0 + seg * 1.7) * 1.5;
                Vec3 pNext = new Vec3(Math.cos(baseA) * dist + jitterX, 3.5 + jitterY, Math.sin(baseA) * dist);
                stroke(v, m, p0, pNext, 0.28f, fade * 0.90f, 0xE1BEE7);
                p0 = pNext;
            }
        }
    }



    // =========================================================================
    // 6. LONG TINH BỘC VIÊM BÁ (DRAGON NOVA - 32-SIDED GPU STARLIGHT DRAGON CANNON)
    // =========================================================================
    private static void renderDragonNova(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float maxRange = 75.0f * skill.powerMultiplier();
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();

        // GIAI ĐOẠN 1: TỤ NĂNG LƯỢNG LONG TINH (Ticks 0 -> 30)
        if (age < 30.0f) {
            float progress = age / 30.0f;
            Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 right = d.cross(up).normalize();
            Vec3 upVec = d.cross(right).normalize();
            Vec3 handPos = new Vec3(0, 1.2, 0).add(d.scale(0.85)).add(right.scale(0.35));

            // Quả cầu ngưng tụ năng lượng Starlight trong lòng bàn tay vươn ra
            float pulse = (float) Math.sin(age * 0.8f) * 0.05f;
            float coreR = 0.35f + progress * 0.55f + pulse;

            // Các lớp cầu phát sáng linh tử tím & hồng neon
            billboardDisc(v, m, handPos, coreR * 2.2f, 0x9400D3, progress * 0.70f); // Tím huyền ảo
            billboardDisc(v, m, handPos, coreR * 1.5f, 0xFF0044, 0.85f);            // Đỏ tía Starlight
            billboardDisc(v, m, handPos, coreR, 0xFF1493, 0.95f);                   // Hồng neon
            billboardDisc(v, m, handPos, coreR * 0.45f, 0xFFFFFF, 1.0f);            // Lõi siêu nhiệt trắng

            // Ma pháp trận lục lăng xoay quanh cổ tay / bàn tay nén năng lượng
            renderHexagonRing(v, m, 1.2f, coreR * 1.6f, 0.22f, 0xFF1493, progress * 0.85f, age * 0.12f);
            renderHexagonRing(v, m, 1.2f, coreR * 2.2f, 0.18f, 0x9400D3, progress * 0.75f, -age * 0.09f);

            // Các vệt tia chớp linh tử mini hội tụ vào bàn tay
            for (int s = 0; s < 4; s++) {
                double a = s * Math.PI / 2.0 + age * 0.2;
                double sparkDist = (1.0 - (age * 0.06) % 1.0) * 1.6;
                Vec3 sparkPos = handPos.add(right.scale(Math.cos(a) * sparkDist)).add(upVec.scale(Math.sin(a) * sparkDist));
                stroke(v, m, sparkPos, handPos, 0.12f, 0.9f, 0xE1BEE7);
            }

            // Thập tự quang mang rực sáng trước khi khai hỏa (ticks 24 -> 30)
            if (age >= 24.0f) {
                double flareLen = 1.2 + (age - 24.0) * 0.4;
                stroke(v, m, handPos.add(right.scale(-flareLen)), handPos.add(right.scale(flareLen)), 0.15f, 0.98f, 0xFFFFFF);
                stroke(v, m, handPos.add(upVec.scale(-flareLen)), handPos.add(upVec.scale(flareLen)), 0.15f, 0.98f, 0xFFFFFF);
                stroke(v, m, handPos.add(right.scale(-flareLen * 0.7)), handPos.add(right.scale(flareLen * 0.7)), 0.25f, 0.85f, 0xFF1493);
                stroke(v, m, handPos.add(upVec.scale(-flareLen * 0.7)), handPos.add(upVec.scale(flareLen * 0.7)), 0.25f, 0.85f, 0x9400D3);
            }
        }
        // GIAI ĐOẠN 2: KHAI HỎA ĐẠI PHÁO LONG TINH 3D 75M (Ticks 30 -> 55) - LOẠI BỎ 100% CỘT VUÔNG
        else if (age < 55.0f) {
            float beamAge = age - 30.0f;
            float beamFade = (beamAge < 20.0f) ? 1.0f : (float) Math.max(0.0, 1.0 - (beamAge - 20.0f) / 5.0f);
            Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 right = d.cross(up).normalize();
            Vec3 upVec = d.cross(right).normalize();
            Vec3 origin = new Vec3(0, 1.2, 0).add(d.scale(0.85)).add(right.scale(0.35));

            // 1. CỘT NĂNG LƯỢNG 32-SIDED SMOOTH GPU CYLINDER ĐA TẦNG DÀI 75M
            // Lớp ngoài: Long Hỏa Đỏ Tía (Crimson Dragon Flame Aura - 6.5m)
            beamCylinder(v, m, origin, d, maxRange, 6.5f, 0xFF0044, beamFade * 0.45f, age);
            // Lớp giữa: Tinh Tố Hồng Neon (Neon Starlight Magenta Tube - 3.8m)
            beamCylinder(v, m, origin, d, maxRange, 3.8f, 0xFF1493, beamFade * 0.75f, age + 2);
            // Lớp lõi: Siêu Quang Tử Trắng Tinh Khiết (White Hot Laser Core - 1.5m)
            beamCylinder(v, m, origin, d, maxRange, 1.5f, 0xFFFFFF, beamFade * 0.98f, age + 4);

            // 2. XOẮN KÉP LINH TỬ DNA (TWIN STARLIGHT DNA HELIX) CHẠY DỌC THÂN PHÁO
            int helixNodes = 36;
            for (int h = 0; h < helixNodes - 1; h++) {
                double dist1 = h * (maxRange / helixNodes);
                double dist2 = (h + 1) * (maxRange / helixNodes);
                double rot1 = dist1 * 0.35 + age * 0.25;
                double rot2 = dist2 * 0.35 + age * 0.25;
                float helixR = 4.8f;

                // Dây xoắn 1
                Vec3 p1A = origin.add(d.scale(dist1)).add(right.scale(Math.cos(rot1) * helixR)).add(upVec.scale(Math.sin(rot1) * helixR));
                Vec3 p2A = origin.add(d.scale(dist2)).add(right.scale(Math.cos(rot2) * helixR)).add(upVec.scale(Math.sin(rot2) * helixR));
                stroke(v, m, p1A, p2A, 0.32f, beamFade * 0.90f, 0xFFFFFF);

                // Dây xoắn 2 (đối xứng 180 độ)
                Vec3 p1B = origin.add(d.scale(dist1)).add(right.scale(-Math.cos(rot1) * helixR)).add(upVec.scale(-Math.sin(rot1) * helixR));
                Vec3 p2B = origin.add(d.scale(dist2)).add(right.scale(-Math.cos(rot2) * helixR)).add(upVec.scale(-Math.sin(rot2) * helixR));
                stroke(v, m, p1B, p2B, 0.32f, beamFade * 0.90f, 0xFF55DD);
            }

            // 3. CÁC VÒNG SÓNG NỔ MACH RINGS PHÓNG DỌC ĐƯỜNG ĐẠN
            for (int k = 1; k <= 5; k++) {
                double machDist = ((beamAge * 4.0 + k * 14.0) % maxRange);
                Vec3 ringCenter = origin.add(d.scale(machDist));
                billboardDisc(v, m, ringCenter, 7.5f, 0xFF0044, beamFade * 0.65f);
                billboardDisc(v, m, ringCenter, 4.5f, 0xFFFFFF, beamFade * 0.85f);
            }
        }
        // GIAI ĐOẠN 3: ĐẠI VỤ NỔ SIÊU TÂN TINH (Ticks 55 -> 75: CRATER SUPERNOVA EXPLOSION)
        else {
            float blastAge = age - 55.0f;
            float progress = Math.min(1.0f, blastAge / 20.0f);
            float curR = 72.0f * progress;
            float fade = (float) Math.max(0.0, 1.0 - progress);
            Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 right = d.cross(up).normalize();
            Vec3 origin = new Vec3(0, 1.2, 0).add(d.scale(0.85)).add(right.scale(0.35));
            Vec3 impactCenter = origin.add(d.scale(maxRange));

            // Sóng nổ hình cầu đa tầng
            ring(v, m, 1.5f, curR, 0.65f, 0xFF0044, fade * 0.95f);
            ring(v, m, 2.5f, curR * 0.85f, 0.45f, 0xFF1493, fade * 0.85f);
            ring(v, m, 3.5f, curR * 0.65f, 0.35f, 0xFFFFFF, fade * 0.95f);
        }
    }

    // =========================================================================
    // 7. THẦN NỘ: TUYỆT DIỆT TINH TÚ (MEGIDDO - STRATOSPHERIC WATER LENSES & SOLAR LASER BARRAGE)
    // =========================================================================
    private static void renderMegiddo(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float r = 36.0f * skill.powerMultiplier();
        float fade = (age < skill.totalTicks() - 15) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 15.0f);

        // Chuẩn nguyên tác Tensura: Hàng chục thấu kính giọt nước (water droplet lenses) lơ lửng trên tầng mây hội tụ ánh sáng mặt trời
        int lenses = 20;
        for (int i = 0; i < lenses; i++) {
            double a = i * Math.PI * 2.0 / lenses + age * 0.008;
            double lensDist = 8.0 + (i % 4) * 7.5;
            double lensY = 48.0 + (i % 3) * 3.5;
            Vec3 lensPos = new Vec3(Math.cos(a) * lensDist, lensY, Math.sin(a) * lensDist);

            // Thấu kính giọt nước khúc xạ ánh sáng 3D
            billboardDisc(v, m, lensPos, 2.2f, 0x80DEEA, fade * 0.85f);
            billboardDisc(v, m, lensPos.add(0, 0.4, 0), 1.2f, 0xE0F7FA, fade * 0.95f);

            // Ánh sáng mặt trời hội tụ từ đỉnh trời chiếu vào thấu kính
            stroke(v, m, lensPos.add(0, 30.0, 0), lensPos, 0.22f, fade * 0.55f, 0xFFF59D);

            // Chùm tia laser nhiệt mặt trời bắn tỉa dồn dập xuống mặt đất theo chu kỳ (Pinpoint Solar Sniper Laser)
            if ((age + i * 3) % 10 < 4) {
                double targetA = a + 0.4;
                Vec3 groundHit = new Vec3(Math.cos(targetA) * (lensDist * 0.6), 0.08, Math.sin(targetA) * (lensDist * 0.6));
                stroke(v, m, lensPos, groundHit, 0.20f, fade * 0.98f, 0xFFFFFF);
                stroke(v, m, lensPos, groundHit, 0.45f, fade * 0.65f, 0xFFEB3B);
                ring(v, m, 0.12f, 2.2f, 0.22f, 0xFFEB3B, fade * 0.90f);
            }
        }
    }



    // =========================================================================
    // 8. THỊ THỰC (SEER FLESH - SỬ DỤNG LẠI THỊ THỰC CŨ SINH VẬT NHẦY NHỤA CHUẨN DARK GATHERING)
    // =========================================================================
    private static void renderSeerFlesh(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        // Giữ nguyên bản chất 1 sinh vật nhầy nhụa bằng mô hình Blockbench TAISUI_SEER_FLESH 3D
        // Không vẽ đè các khối hình đĩa nhân tạo làm mất bản chất sinh vật gốc
        float fade = (age < skill.totalTicks() - 10) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 10.0f);
        ring(v, m, 0.08f, 1.8f, 0.20f, 0x880015, fade * 0.50f);
    }

    // =========================================================================
    // 9. DIỆT THẾ TÀ TINH (ALKAID - COLOSSAL COSMIC ASTEROID DRILL & 120M STELLAR TAIL)
    // =========================================================================
    private static void renderAlkaid(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float totalT = skill.totalTicks();
        float progress = Math.min(1.0f, age / (totalT - 15.0f));
        float meteorY = 95.0f * (1.0f - progress * progress); // Rơi gia tốc trọng lực cực nhanh
        float meteorR = 18.0f * skill.powerMultiplier();

        Vec3 meteorCenter = new Vec3(0, meteorY + 4.0, 0);

        // 1. Khối thiên thạch hắc ám dung nham rực cháy
        billboardDisc(v, m, meteorCenter, meteorR, 0x4A148C, 0.98f);
        billboardDisc(v, m, meteorCenter, meteorR * 0.70f, 0xAD1457, 0.95f);
        billboardDisc(v, m, meteorCenter, meteorR * 0.40f, 0xFFD600, 1.0f);

        // 2. Tam trùng vành đai quỹ đạo 3D xoay trên 3 trục X, Y, Z (Tri-Axial Orbital Rings)
        renderHexagonRing(v, m, (float) meteorCenter.y, meteorR * 1.35f, 0.45f, 0xEA80FC, 0.85f, age * 0.08f);
        renderHexagonRing(v, m, (float) meteorCenter.y, meteorR * 1.65f, 0.35f, 0xFF1744, 0.75f, -age * 0.06f);

        // 3. Mũi khoan plasma xoắn ốc xé toạc không khí ở đầu thiên thạch
        Vec3 drillTip = meteorCenter.add(0, -meteorR * 1.2, 0);
        stroke(v, m, meteorCenter, drillTip, meteorR * 0.6f, 0.95f, 0xFFEA00);

        // 4. Đuôi sao băng plasma vũ trụ dài 120m rực sáng kéo lên trời
        Vec3 tailTop = meteorCenter.add(0, 120.0, 0);
        stroke(v, m, meteorCenter, tailTop, meteorR * 0.85f, 0.80f, 0xD500F9);
        stroke(v, m, meteorCenter, tailTop.add(3, 0, 3), meteorR * 0.45f, 0.70f, 0xFF1744);

        // 5. Sóng xung kích trọng lực lan tỏa bên dưới mặt đất khi thiên thạch lao xuống
        float groundWaveR = (1.0f - meteorY / 95.0f) * 32.0f;
        if (groundWaveR > 2.0f) {
            ring(v, m, 0.15f, groundWaveR, 0.55f, 0x7B1FA2, 0.85f * progress);
            ring(v, m, 0.25f, groundWaveR * 0.75f, 0.40f, 0xFF1744, 0.90f * progress);
        }
    }

    // =========================================================================
    // 10. CÚ BẮN GRANIT (GRANITE BLAST - JUJUTSU KAISEN RYU ISHIGORI CONCUSSIVE CANNON)
    // =========================================================================
    private static void renderGraniteBlast(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float len = 100.0f * skill.powerMultiplier();
        float fade = (age < 25.0f) ? 1.0f : (float) Math.max(0.0, 1.0 - (age - 25.0f) / 15.0f);
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();
        Vec3 origin = new Vec3(0, 1.5, 0);

        // Chuẩn nguyên tác Jujutsu Kaisen (Ryu Ishigori - Cú Bắn Granit):
        // 1. Quả cầu chú lực đậm đặc nén cao độ tại họng pháo
        if (age < 15.0f) {
            float chargeProg = age / 15.0f;
            billboardDisc(v, m, origin.add(d.scale(1.2)), 3.5f * chargeProg, 0xFF6D00, 0.95f);
            billboardDisc(v, m, origin.add(d.scale(1.2)), 2.0f * chargeProg, 0xFFFF00, 1.0f);
        }

        // 2. Chùm pháo đại bác xung lực 32-sided Smooth Cylinder 100m
        beamCylinder(v, m, origin, d, len, 3.8f, 0xFF6D00, fade * 0.55f, age);
        beamCylinder(v, m, origin, d, len, 2.2f, 0xFFD600, fade * 0.80f, age + 2);
        beamCylinder(v, m, origin, d, len, 1.0f, 0xFFFFFF, fade * 0.98f, age + 4);

        // 3. 6 Vòng sóng nổ Mach Rings hình xuyến (Toroidal Mach Shockwaves) phóng dọc đường bắn
        for (int k = 1; k <= 6; k++) {
            double ringDist = ((age * 4.5 + k * 16.0) % len);
            Vec3 ringCenter = origin.add(d.scale(ringDist));
            billboardDisc(v, m, ringCenter, 5.5f + k * 0.6f, 0xFF6D00, fade * 0.65f);
            billboardDisc(v, m, ringCenter, 3.2f, 0xFFFFFF, fade * 0.85f);
        }

        // 4. Tia sét chú lực dữ dội nhảy múa quanh thân pháo
        Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = d.cross(up).normalize();
        for (int arc = 0; arc < 8; arc++) {
            double aDist1 = arc * (len / 8.0);
            double aDist2 = (arc + 1) * (len / 8.0);
            double arcRot = arc * 1.5 + age * 0.4;
            Vec3 ap1 = origin.add(d.scale(aDist1)).add(right.scale(Math.cos(arcRot) * 3.2));
            Vec3 ap2 = origin.add(d.scale(aDist2)).add(right.scale(Math.cos(arcRot + 1.2) * 3.2));
            stroke(v, m, ap1, ap2, 0.22f, fade * 0.90f, 0xFFAB00);
        }
    }

    // =========================================================================
    // 11. NGHỊCH THỜI NHẤT TRẢM (REVERSE-TIME SLASH - MONOCHROME TIME RIFT)
    // =========================================================================
    private static void renderReverseTimeSlash(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < 25.0f) ? 1.0f : (float) Math.max(0.0, 1.0 - (age - 25.0f) / 10.0f);

        // Vết nứt không gian thời gian tím bạc
        Vec3 p1 = new Vec3(-8.0, 3.5, 0);
        Vec3 p2 = new Vec3(8.0, 0.5, 0);
        stroke(v, m, p1, p2, 0.45f, 0.95f * fade, 0xEDE7F6);
        stroke(v, m, p1, p2, 0.15f, 1.0f * fade, 0x7C4DFF);

        // Vành trăng lưỡi liềm chém xé không gian
        ring(v, m, 2.0f, 6.5f, 0.35f, 0xD1C4E9, fade * 0.85f);
    }

    // =========================================================================
    // 12. BẠT ĐAO THUẬT VẬN TỐC NGƯỢC (REVERSE-VELOCITY IAI - 3 AFTERIMAGES)
    // =========================================================================
    private static void renderReverseVelocityIai(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < 22.0f) ? 1.0f : (float) Math.max(0.0, 1.0 - (age - 22.0f) / 10.0f);

        // 3 Vệt chém tam giác kiếm trận đan chéo tốc độ ánh sáng
        Vec3 a = new Vec3(-5.0, 1.2, -5.0);
        Vec3 b = new Vec3(5.0, 1.2, -5.0);
        Vec3 c = new Vec3(0, 1.2, 5.0);

        stroke(v, m, a, b, 0.25f, fade * 0.9f, 0xFF5252);
        stroke(v, m, b, c, 0.25f, fade * 0.9f, 0xFF5252);
        stroke(v, m, c, a, 0.25f, fade * 0.9f, 0xFF5252);
    }

    // =========================================================================
    // 13. ĐA TRÙNG KẾT GIỚI (MULTILAYER BARRIER - 3D GEODESIC RUNIC AEGIS FORCEFIELD)
    // =========================================================================
    private static void renderMultilayerBarrier(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < skill.totalTicks() - 8) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 8.0f);

        if (skill.powerMultiplier() < 0) {
            // TRẠNG THÁI TƯỜNG KHIÊN: 3D Runic Wall Aegis phẳng 3.6m x 4.2m
            renderRunicWallAegis(v, m, skill.direction(), age, fade);
            return;
        }

        // TRẠNG THÁI CẦU HỘ THÂN: 3D Geodesic Runic Aegis Dome (Bán kính 6m)
        float r = skill.powerMultiplier() > 0 ? skill.powerMultiplier() : 6.0f;

        // HIỆU ỨNG BUNG KẾT GIỚI 1 LẦN DUY NHẤT TRONG 15 TICKS ĐẦU (One-shot Deploy)
        float deployProg = Math.min(1.0f, age / 15.0f);
        float currentR = r * (0.4f + 0.6f * deployProg);

        if (age < 15.0f) {
            float burstAlpha = (1.0f - deployProg) * fade * 0.9f;
            ring(v, m, 1.0f, currentR, 0.35f, 0xFFFFFF, burstAlpha);
            renderHexagonRing(v, m, 1.0f, currentR, 0.28f, 0x00E5FF, burstAlpha * 0.8f, age * 0.04f);
        }

        // KẾT GIỚI 3D TĨNH LẶNG & TRONG SUỐT (Static 3D Forcefield - Không chớp nháy, không xoay rối mắt)
        renderGeodesicDome(v, m, new Vec3(0, 0.1, 0), currentR, 0x00E5FF, fade * 0.50f, 0.0f);
        renderGeodesicDome(v, m, new Vec3(0, 0.1, 0), currentR * 0.75f, 0x40C4FF, fade * 0.30f, 0.0f);
        renderHexagonRing(v, m, 1.0f, currentR * 0.98f, 0.18f, 0x00E5FF, fade * 0.60f, 0.0f);
    }

    private static void renderRunicWallAegis(VertexConsumer v, Matrix4f m, Vec3 normal, float age, float fade) {
        Vec3 norm = normal != null && normal.lengthSqr() > 0.01 ? normal.normalize() : new Vec3(0, 0, 1);
        Vec3 right = new Vec3(-norm.z, 0, norm.x).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        int opacity = Math.clamp(Math.round(fade * 135.0f), 0, 255);

        // HIỆU ỨNG BUNG TƯỜNG 1 LẦN DUY NHẤT TRONG 15 TICKS ĐẦU (One-shot Deploy)
        if (age < 15.0f) {
            float deployProg = age / 15.0f;
            float ripAlpha = (1.0f - deployProg) * fade * 0.9f;
            float ripW = deployProg * 2.5f;
            float ripH = deployProg * 2.8f;

            Vec3 c = up.scale(1.8);
            Vec3 p1 = c.add(right.scale(-ripW)).add(up.scale(-ripH));
            Vec3 p2 = c.add(right.scale(ripW)).add(up.scale(-ripH));
            Vec3 p3 = c.add(right.scale(ripW)).add(up.scale(ripH));
            Vec3 p4 = c.add(right.scale(-ripW)).add(up.scale(ripH));

            stroke(v, m, p1, p2, 0.14f, ripAlpha, 0xFFFFFF);
            stroke(v, m, p2, p3, 0.14f, ripAlpha, 0xFFFFFF);
            stroke(v, m, p3, p4, 0.14f, ripAlpha, 0xFFFFFF);
            stroke(v, m, p4, p1, 0.14f, ripAlpha, 0xFFFFFF);
        }

        // 3 Tấm khiên lục lăng phát quang 3D tĩnh lặng (Không chớp giật liên tục)
        float[] xOffsets = {-1.2f, 0.0f, 1.2f};
        for (int i = 0; i < xOffsets.length; i++) {
            float xOff = xOffsets[i];
            Vec3 center = right.scale(xOff).add(up.scale(1.8));

            float hexR = 1.35f;
            Vec3[] pts = new Vec3[6];
            for (int k = 0; k < 6; k++) {
                double ang = k * Math.PI / 3.0 + Math.PI / 6.0;
                pts[k] = center.add(right.scale(Math.cos(ang) * hexR)).add(up.scale(Math.sin(ang) * hexR));
            }

            // Mặt phẳng khiên
            quad(v, m, pts[0], pts[1], pts[2], pts[3], 0x00E5FF, opacity);
            quad(v, m, pts[3], pts[4], pts[5], pts[0], 0x00E5FF, opacity);

            // Viền sáng cổ ngữ
            for (int k = 0; k < 6; k++) {
                stroke(v, m, pts[k], pts[(k + 1) % 6], 0.14f, fade * 0.90f, 0x64FFDA);
            }

            // Gân năng lượng hướng tâm
            for (int k = 0; k < 6; k++) {
                stroke(v, m, center, pts[k], 0.08f, fade * 0.55f, 0x80D8FF);
            }
        }
    }



    // =========================================================================
    // 14. LỒNG GIAM VÔ HẠN (INFINITE DRAGON PRISON - 20-SIDED DIAMOND BARRIER)
    // =========================================================================
    private static void renderDragonPrison(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float r = skill.powerMultiplier();
        if (r <= 0.1f) r = 12.0f;
        float fade = (age < skill.totalTicks() - 10) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 10.0f);
        int opacity = Math.clamp(Math.round(fade * 110.0f), 0, 255);

        // Khối đa diện kim cương 20 mặt (10 đỉnh xích đạo, 2 đỉnh cực)
        int sides = 10;
        Vec3 top = new Vec3(0, r * 0.95, 0);
        Vec3 bot = new Vec3(0, -r * 0.95, 0);

        for (int i = 0; i < sides; i++) {
            double a1 = i * Math.PI * 2.0 / sides + age * 0.015;
            double a2 = (i + 1) * Math.PI * 2.0 / sides + age * 0.015;
            Vec3 p1 = new Vec3(Math.cos(a1) * r, 0.0, Math.sin(a1) * r);
            Vec3 p2 = new Vec3(Math.cos(a2) * r, 0.0, Math.sin(a2) * r);

            // Mặt tam giác bán trong suốt hướng đỉnh trên (Vàng kim hoàng gia)
            quad(v, m, p1, p2, top, top, 0xFFD700, opacity);
            // Mặt tam giác bán trong suốt hướng đỉnh dưới (Lam ngọc thiên thanh)
            quad(v, m, p2, p1, bot, bot, 0x00E5FF, opacity);

            // Khung viền kim cương phát quang lập thể
            stroke(v, m, p1, p2, 0.22f, fade * 0.90f, 0xFFD700);
            stroke(v, m, p1, top, 0.18f, fade * 0.85f, 0x64FFDA);
            stroke(v, m, p1, bot, 0.18f, fade * 0.85f, 0x40C4FF);
        }

        // 3 Vành đai hoàng kim long ngữ xoay quanh xích đạo và 2 bán cầu
        renderHexagonRing(v, m, 0.0f, r * 1.02f, 0.28f, 0xFFD700, fade * 0.85f, age * 0.025f);
        renderHexagonRing(v, m, r * 0.45f, r * 0.72f, 0.22f, 0xFFAB00, fade * 0.75f, -age * 0.035f);
        renderHexagonRing(v, m, -r * 0.45f, r * 0.72f, 0.22f, 0xFFAB00, fade * 0.75f, -age * 0.035f);
    }

    // =========================================================================
    // 15. CẢM NHẬN VẠN NĂNG (UNIVERSAL SENSE - SONAR TOPOLOGY PULSE)
    // =========================================================================
    private static void renderUniversalSense(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float maxR = 48.0f * skill.powerMultiplier();
        float progress = Math.min(1.0f, age / 25.0f);
        float curR = maxR * progress;
        float fade = 1.0f - progress;

        ring(v, m, 1.2f, curR, 0.35f, 0x69F0AE, fade * 0.95f);
        ring(v, m, 1.2f, curR * 0.75f, 0.20f, 0x00E676, fade * 0.75f);
    }

    // =========================================================================
    // 16. PHÒNG NGỰ TUYỆT ĐỐI URIEL (SOLAR CORONA TRIPLE RINGS)
    // =========================================================================
    private static void renderUrielDefense(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < skill.totalTicks() - 5) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 5.0f);

        // 3 Vành đai hỏa ngục thái dương xoay nghiêng quanh người
        ring(v, m, 1.2f, 2.8f, 0.25f, 0xFFAB00, fade * 0.9f);
        ring(v, m, 1.6f, 3.2f, 0.20f, 0xFF6D00, fade * 0.8f);
    }

    // =========================================================================
    // 17. TIÊN TRI: RYKUZIN SINKA (PRE-COGNITION GOLDEN TRAIL)
    // =========================================================================
    private static void renderRykuzinSinka(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < skill.totalTicks() - 5) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 5.0f);
        ring(v, m, 0.1f, 3.5f, 0.25f, 0xFFFF00, fade * 0.85f);
    }

    // =========================================================================
    // 18. ZONE TRACK (SOUSOU NO FRIEREN - ZOLTRAAK QUANTUM SNIPER BEAM - 150M)
    // =========================================================================
    private static void renderZoneTrack(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float len = 150.0f * skill.powerMultiplier();
        float fade = (age < 20.0f) ? 1.0f : (float) Math.max(0.0, 1.0 - (age - 20.0f) / 10.0f);
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();
        Vec3 origin = new Vec3(0, 1.5, 0);

        // Chuẩn nguyên tác Sousou no Frieren (Ma Tộc Băng Diệt Ma Pháp - Zoltraak):
        // 1. Ma trận ma thuật phức hợp Qual / Frieren (Dual Concentric Geometric Magic Circles)
        billboardDisc(v, m, origin.add(d.scale(0.8)), 3.8f, 0x80D8FF, fade * 0.90f);
        billboardDisc(v, m, origin.add(d.scale(0.8)), 2.2f, 0xFFFFFF, fade * 0.98f);
        // Vành cổ ngữ xoay tít trước tay
        renderHexagonRing(v, m, 1.5f, 3.2f, 0.25f, 0x80D8FF, fade * 0.85f, age * 0.12f);
        renderHexagonRing(v, m, 1.5f, 4.5f, 0.20f, 0xFFFFFF, fade * 0.75f, -age * 0.08f);

        // 2. Chùm tia lượng tử trắng tinh khiết xuyên thủng không gian 150m (32-Sided Smooth Cylinder)
        beamCylinder(v, m, origin, d, len, 2.5f, 0x80D8FF, fade * 0.45f, age);
        beamCylinder(v, m, origin, d, len, 1.4f, 0x00E5FF, fade * 0.75f, age + 2);
        beamCylinder(v, m, origin, d, len, 0.7f, 0xFFFFFF, fade * 0.98f, age + 4);

        // 3. Các vòng sóng ma pháp hình trụ mở rộng (Cylindrical Shockwave Rings) trượt dọc thân tia
        for (int k = 1; k <= 7; k++) {
            double ringDist = ((age * 6.5 + k * 20.0) % len);
            Vec3 ringCenter = origin.add(d.scale(ringDist));
            billboardDisc(v, m, ringCenter, 4.2f, 0x80D8FF, fade * 0.65f);
            billboardDisc(v, m, ringCenter, 2.2f, 0xFFFFFF, fade * 0.85f);
        }
    }

    // =========================================================================
    // 19. PHONG SÁT KĨ NĂNG (VACUUM NULL EXPANDING SHOCKWAVE - 64M)
    // =========================================================================
    private static void renderNullification(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float maxR = 64.0f * skill.powerMultiplier();
        float progress = Math.min(1.0f, age / 20.0f);
        float curR = maxR * progress;
        float fade = 1.0f - progress;

        ring(v, m, 0.5f, curR, 0.45f, 0xE0E0E0, fade * 0.95f);
        ring(v, m, 0.5f, curR * 0.85f, 0.30f, 0xFFFFFF, fade * 0.80f);
    }

    // =========================================================================
    // 20. HẮC VIÊM NGỤC (HELLFLARE - 3D CONTAINMENT PRISON & OPAQUE BLACK FIRE)
    // =========================================================================
    private static void renderHellflare(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < skill.totalTicks() - 10) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 10.0f);
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);

        // Giai đoạn 0: Đạn lửa đen bay tới mục tiêu (age < 10)
        if (age < 10.0f) {
            float fProg = age / 10.0f;
            Vec3 projPos = new Vec3(0, 1.5, 0).lerp(dir.scale(16.0), fProg);
            billboardDisc(v, m, projPos, 2.2f, 0x11001C, 0.95f);
            billboardDisc(v, m, projPos, 1.4f, 0x7B1FA2, 0.98f);
            billboardDisc(v, m, projPos, 0.7f, 0x00E5FF, 1.0f);
            return;
        }

        // Giai đoạn 1 & 2: Khối lập phương không gian phóng to và bao trùm bởi ngọn lửa đen thật
        float boxAge = age - 10.0f;
        float expandProg = Math.min(1.0f, boxAge / 15.0f);
        float halfSize = 1.0f + expandProg * 2.5f; // Bán kính khối lập phương nở từ 1.0m lên 3.5m (kích thước 7x7x7)

        Vec3 boxCenter = new Vec3(0, 2.5, 0);

        // 1. Khối lập phương giam cầm (Opaque Black Containment Cube - Bên ngoài hoàn toàn không nhìn vào trong được)
        if (boxAge >= 15.0f) {
            renderCubeMesh(v, m, boxCenter, halfSize, 0x05000A, 0.98f * fade);
        } else {
            renderCubeMesh(v, m, boxCenter, halfSize, 0x212121, 0.55f * fade);
        }

        // 2. Vỏ lửa đen cuồn cuộn bao bọc 6 mặt khối vuông (Black Hellfire Shell)
        for (int side = 0; side < 6; side++) {
            float fireOffset = halfSize + 0.25f;
            Vec3 fPos = switch (side) {
                case 0 -> boxCenter.add(0, 0, fireOffset);
                case 1 -> boxCenter.add(0, 0, -fireOffset);
                case 2 -> boxCenter.add(fireOffset, 0, 0);
                case 3 -> boxCenter.add(-fireOffset, 0, 0);
                case 4 -> boxCenter.add(0, fireOffset, 0);
                default -> boxCenter.add(0, -fireOffset, 0);
            };
            billboardDisc(v, m, fPos, halfSize * 1.35f, 0x4A148C, fade * 0.75f);
            billboardDisc(v, m, fPos, halfSize * 0.85f, 0x000000, fade * 0.95f);
        }

        // 3. Vành đai hắc viêm xoay quanh khối hộp
        renderHexagonRing(v, m, 2.5f, halfSize * 1.6f, 0.35f, 0x7B1FA2, fade * 0.85f, age * 0.08f);
    }

    // =========================================================================
    // 21. KỸ NĂNG ÁC MA (PRIMORDIAL DEMON SKILLS 3D CINEMATIC EFFECT)
    // =========================================================================
    private static void renderPrimordialDemon(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float demonCode = skill.powerMultiplier();
        int demonType = ((int) demonCode) / 10;
        int skillIdx = ((int) demonCode) % 10;
        float fade = (age < skill.totalTicks() - 8) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 8.0f);
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();

        // 0: Rouge (Guy Crimson - Xích Sắc) hoặc 1: Noir (Diablo - Hắc Sắc), 2: Blanc (Testarossa - Bạch Sắc),
        // 3: Jaune (Carrera - Hoàng Sắc), 4: Violet (Ultima - Tử Sắc), 5: Bleu (Rein - Lam Sắc), 6: Vert (Misery - Lục Sắc)
        switch (demonType) {
            case 0 -> { // ROUGE: Hỏa Ngục Xích Sắc & Long Hỏa Diệt Thế
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 55.0f, 3.2f, 0xFF1744, fade * 0.85f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 55.0f, 1.2f, 0xFFFFFF, fade * 0.98f, age + 2);
                ring(v, m, 1.2f, 8.5f, 0.45f, 0xFF6D00, fade * 0.90f);
                for (int f = 0; f < 8; f++) {
                    double fa = f * Math.PI / 4.0 + age * 0.05;
                    stroke(v, m, new Vec3(0, 1.0, 0), new Vec3(Math.cos(fa) * 14.0, 4.0, Math.sin(fa) * 14.0), 0.35f, fade * 0.85f, 0xFF1744);
                }
            }
            case 1 -> { // NOIR: Vực Thẳm Ý Thức & Hư Vô Singularity
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 3.0f, 0x7B1FA2, fade * 0.85f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 1.2f, 0xEDE7F6, fade * 0.98f, age + 2);
                billboardDisc(v, m, new Vec3(0, 2.0, 0), 4.5f, 0x120024, fade * 0.95f);
                renderHexagonRing(v, m, 0.1f, 7.5f, 0.35f, 0x7B1FA2, fade * 0.85f, age * 0.06f);
                for (int claw = 0; claw < 4; claw++) {
                    double ca = claw * Math.PI / 2.0 + age * 0.04;
                    Vec3 cp1 = new Vec3(Math.cos(ca) * 6.5, 2.0, Math.sin(ca) * 6.5);
                    Vec3 cp2 = cp1.add(d.scale(8.0)).add(0, Math.sin(claw * 2.0) * 1.5, 0);
                    stroke(v, m, cp1, cp2, 0.30f, fade * 0.90f, 0xBA68C8);
                }
            }
            case 2 -> { // BLANC: Bạch Viêm & Băng Hồ Điệp Cực Hạn
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 55.0f, 2.8f, 0xE0F7FA, fade * 0.90f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 55.0f, 1.2f, 0xFFFFFF, fade * 0.98f, age + 2);
                for (int b = 0; b < 12; b++) {
                    double ba = b * Math.PI / 6.0 + age * 0.08;
                    Vec3 bPos = new Vec3(Math.cos(ba) * 5.5, 1.5 + Math.sin(b * 1.5 + age * 0.2) * 1.2, Math.sin(ba) * 5.5);
                    billboardDisc(v, m, bPos, 0.75f, 0x80DEEA, fade * 0.90f);
                }
            }
            case 3 -> { // JAUNE: Cự Pháo Hạt Nhân Abaddon & Trọng Lực Sụp Đổ
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 60.0f, 4.2f, 0xFFD600, fade * 0.80f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 60.0f, 1.5f, 0xFFFFFF, fade * 0.98f, age + 2);
                renderHexagonRing(v, m, 1.5f, 6.5f, 0.40f, 0xFFAB00, fade * 0.85f, age * 0.15f);
                billboardDisc(v, m, new Vec3(0, 1.5, 0).add(d.scale(2.0)), 4.0f, 0xFFFFFF, fade * 0.98f);
            }
            case 4 -> { // VIOLET: Tử Độc Nở Rộ & Miasma Hoa Sen
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 2.8f, 0xAA00FF, fade * 0.85f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 1.2f, 0xFFFFFF, fade * 0.98f, age + 2);
                for (int pet = 0; pet < 6; pet++) {
                    double pa = pet * Math.PI / 3.0 + age * 0.03;
                    Vec3 pTip = new Vec3(Math.cos(pa) * 6.5, 1.8 + Math.sin(pa * 3.0) * 0.8, Math.sin(pa) * 6.5);
                    stroke(v, m, new Vec3(0, 0.5, 0), pTip, 0.45f, fade * 0.85f, 0xAA00FF);
                }
                ring(v, m, 0.2f, 7.5f, 0.35f, 0x7B1FA2, fade * 0.75f);
            }
            case 5 -> { // BLEU: Băng Tuyệt Đối 0 Độ K & Gương Băng Cực Địa
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 3.0f, 0x00E5FF, fade * 0.85f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 1.2f, 0xE0F7FA, fade * 0.98f, age + 2);
                cylinder(v, m, 6.5f, 25.0f, fade * 0.65f, 0x00E5FF, age);
                renderGeodesicDome(v, m, new Vec3(0, 0.1, 0), 8.0f, 0x80D8FF, fade * 0.60f, age * 0.02f);
            }
            case 6 -> { // VERT: Cuồng Phong Lục Sắc & Lưỡi Hái Bão Tố
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 3.0f, 0x00E676, fade * 0.85f, age);
                beamCylinder(v, m, new Vec3(0, 1.5, 0), d, 50.0f, 1.2f, 0xE8F5E9, fade * 0.98f, age + 2);
                renderHexagonRing(v, m, 1.5f, 9.5f, 0.55f, 0x00E676, fade * 0.90f, age * 0.18f);
                for (int w = 0; w < 4; w++) {
                    double wa = w * Math.PI / 2.0 + age * 0.12;
                    Vec3 wp1 = new Vec3(Math.cos(wa) * 4.0, 1.5, Math.sin(wa) * 4.0);
                    Vec3 wp2 = new Vec3(Math.cos(wa + 0.8) * 11.0, 2.5, Math.sin(wa + 0.8) * 11.0);
                    stroke(v, m, wp1, wp2, 0.35f, fade * 0.95f, 0x76FF03);
                }
            }
        }
    }

    // =========================================================================
    // 22. DỊ GIỚI THẦN MÔN (DIMENSIONAL SANCTUARY GATEWAY - 3D RUNIC CELESTIAL PORTAL)
    // =========================================================================
    private static void renderDimensionalGate(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float fade = (age < skill.totalTicks() - 10) ? 1.0f : (float) Math.max(0.0, (skill.totalTicks() - age) / 10.0f);
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 forward = dir.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        // Hiệu ứng mở cổng 1 lần trong 15 ticks đầu (Rift expanding)
        float openProgress = Math.min(1.0f, age / 15.0f);
        float gateW = 1.7f * openProgress; // Bán kính ngang 1.7m (Rộng 3.4m)
        float gateH = 4.8f * openProgress; // Chiều cao 4.8m

        Vec3 botCenter = new Vec3(0, 0.05, 0);
        Vec3 topCenter = botCenter.add(up.scale(gateH));

        Vec3 botLeft = botCenter.add(right.scale(-gateW));
        Vec3 botRight = botCenter.add(right.scale(gateW));
        Vec3 topLeft = topCenter.add(right.scale(-gateW));
        Vec3 topRight = topCenter.add(right.scale(gateW));

        // 1. Khung cổng cổ ngữ hoàng kim & lam ngọc (Gold & Cyan Portal Archway)
        stroke(v, m, botLeft, topLeft, 0.18f, fade * 0.95f, 0xFFD700);
        stroke(v, m, botRight, topRight, 0.18f, fade * 0.95f, 0xFFD700);
        stroke(v, m, topLeft, topRight, 0.22f, fade * 0.95f, 0xFFD700);
        stroke(v, m, botLeft, botRight, 0.14f, fade * 0.80f, 0x00E5FF);

        // Đỉnh vòm cổng có hoa văn cổ ngữ Tensura
        Vec3 apex = topCenter.add(up.scale(0.8 * openProgress));
        stroke(v, m, topLeft, apex, 0.16f, fade * 0.90f, 0xFFEA00);
        stroke(v, m, topRight, apex, 0.16f, fade * 0.90f, 0xFFEA00);

        // 2. Màng không gian dị giới phát quang huyền bí bên trong khung (Translucent Nebula Horizon)
        int portalOpacity = Math.clamp(Math.round(fade * 170.0f * openProgress), 0, 255);
        quad(v, m, botLeft, botRight, topRight, topLeft, 0x1A0033, portalOpacity); // Tím thẳm ngân hà
        quad(v, m, topLeft, topRight, botRight, botLeft, 0x00E5FF, (int) (portalOpacity * 0.45f)); // Ánh xanh dị giới

        // 3. Tinh tú lơ lửng trong lòng cổng (Constellation Stars)
        for (int star = 0; star < 6; star++) {
            float sx = (float) Math.sin(star * 1.7 + age * 0.03f) * (gateW * 0.75f);
            float sy = 0.8f + (float) ((star * 0.7 + age * 0.04f) % (gateH * 0.85f));
            Vec3 starPos = botCenter.add(right.scale(sx)).add(up.scale(sy));
            billboardDisc(v, m, starPos, 0.28f, 0xFFFFFF, fade * 0.85f);
            billboardDisc(v, m, starPos, 0.50f, 0x80D8FF, fade * 0.50f);
        }

        // Làn sóng mở cổng bung 1 lần duy nhất lúc xuất hiện (age < 15)
        if (age < 15.0f) {
            float burstAlpha = (1.0f - age / 15.0f) * fade * 0.9f;
            stroke(v, m, botLeft.add(forward.scale(0.1)), botRight.add(forward.scale(0.1)), 0.35f, burstAlpha, 0xFFFFFF);
            stroke(v, m, topLeft.add(forward.scale(0.1)), topRight.add(forward.scale(0.1)), 0.35f, burstAlpha, 0xFFFFFF);
        }
    }

    // =========================================================================
    // 23. LINH TỬ BỘC PHÁT (SPIRITRON OUTBURST - 3D 32-SIDED GPU DEMON BEAM)
    // =========================================================================
    private static void renderSpiritronOutburst(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float maxRange = 64.0f;
        Vec3 dir = skill.direction();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();
        Vec3 origin = new Vec3(0, 1.2, 0);

        float totalT = skill.totalTicks();
        float fade = (age < totalT - 10.0f) ? 1.0f : (float) Math.max(0.0, (totalT - age) / 10.0f);

        // Xác định hệ màu của 7 Thủy Tổ Ác Ma (DemonType ordinal: 0: ROUGE, 1: NOIR, 2: BLANC, 3: JAUNE, 4: VIOLET, 5: BLEU, 6: VERT)
        int demonOrdinal = Math.round(skill.powerMultiplier());
        int outerColor;
        int coreColor;
        int laserColor = 0xFFFFFF;
        int helixColor;

        if (demonOrdinal == 0) { // ROUGE - Guy Crimson (Đỏ rực & Huyết diễm)
            outerColor = 0xAA0000;
            coreColor = 0xFF1744;
            helixColor = 0xFF9100;
        } else if (demonOrdinal == 1) { // NOIR - Diablo (Đen tuyền & Hư vô tím)
            outerColor = 0x311B92;
            coreColor = 0x7B1FA2;
            laserColor = 0xEDE7F6;
            helixColor = 0x00E5FF;
        } else if (demonOrdinal == 2) { // BLANC - Testarossa (Bạch quang & Băng lam)
            outerColor = 0x80DEEA;
            coreColor = 0xE0F7FA;
            helixColor = 0xCFE2FE;
        } else if (demonOrdinal == 3) { // JAUNE - Carrera (Hoàng kim rực rỡ & Cam nhiệt hạch)
            outerColor = 0xFF6D00;
            coreColor = 0xFFFF00;
            helixColor = 0xFFD700;
        } else if (demonOrdinal == 4) { // VIOLET - Ultima (Tử quang kịch độc & Tím u tối)
            outerColor = 0x4A148C;
            coreColor = 0xAA00FF;
            helixColor = 0xEA80FC;
        } else if (demonOrdinal == 5) { // BLEU - Rein (Băng lam cực hạn 0 độ K)
            outerColor = 0x01579B;
            coreColor = 0x00E5FF;
            helixColor = 0x80D8FF;
        } else if (demonOrdinal == 6) { // VERT - Misery (Lục bảo bão táp)
            outerColor = 0x1B5E20;
            coreColor = 0x00E676;
            helixColor = 0x76FF03;
        } else { // Generic / Holy
            outerColor = 0xFF6D00;
            coreColor = 0xFFD600;
            helixColor = 0xFFEA00;
        }

        // 1. CỘT SÁNG 32-SIDED SMOOTH GPU CYLINDER ĐA TẦNG 64M (CHUẨN MILIM & GRANITE BLAST)
        // Lớp ngoài: Hào quang năng lượng Thủy Tổ (Aura)
        beamCylinder(v, m, origin, d, maxRange, 4.8f, outerColor, fade * 0.45f, age);
        // Lớp giữa: Thân pháo năng lượng đậm đặc
        beamCylinder(v, m, origin, d, maxRange, 2.8f, coreColor, fade * 0.75f, age + 2);
        // Lớp lõi: Lõi plasma trắng tinh khiết xóa sổ vật chất
        beamCylinder(v, m, origin, d, maxRange, 1.2f, laserColor, fade * 0.98f, age + 4);

        // 2. XOẮN KÉP LINH TỬ DNA (TWIN SPIRITRON DNA HELIX) CHẠY DỌC THÂN PHÁO
        Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = d.cross(up).normalize();
        Vec3 upVec = d.cross(right).normalize();

        int helixNodes = 32;
        float helixR = 3.6f;
        for (int h = 0; h < helixNodes - 1; h++) {
            double dist1 = h * (maxRange / helixNodes);
            double dist2 = (h + 1) * (maxRange / helixNodes);
            double rot1 = dist1 * 0.35 + age * 0.25;
            double rot2 = dist2 * 0.35 + age * 0.25;

            // Dây xoắn 1 (Hào quang đặc trưng)
            Vec3 p1A = origin.add(d.scale(dist1)).add(right.scale(Math.cos(rot1) * helixR)).add(upVec.scale(Math.sin(rot1) * helixR));
            Vec3 p2A = origin.add(d.scale(dist2)).add(right.scale(Math.cos(rot2) * helixR)).add(upVec.scale(Math.sin(rot2) * helixR));
            stroke(v, m, p1A, p2A, 0.25f, fade * 0.90f, helixColor);

            // Dây xoắn 2 (Màu lõi)
            Vec3 p1B = origin.add(d.scale(dist1)).add(right.scale(-Math.cos(rot1) * helixR)).add(upVec.scale(-Math.sin(rot1) * helixR));
            Vec3 p2B = origin.add(d.scale(dist2)).add(right.scale(-Math.cos(rot2) * helixR)).add(upVec.scale(-Math.sin(rot2) * helixR));
            stroke(v, m, p1B, p2B, 0.25f, fade * 0.90f, coreColor);
        }

        // 3. CÁC VÒNG SÓNG NỔ MACH RINGS PHÓNG DỌC ĐƯỜNG BẮN
        for (int k = 1; k <= 5; k++) {
            double machDist = ((age * 4.2 + k * 13.0) % maxRange);
            Vec3 ringCenter = origin.add(d.scale(machDist));
            billboardDisc(v, m, ringCenter, 5.8f, outerColor, fade * 0.60f);
            billboardDisc(v, m, ringCenter, 3.2f, laserColor, fade * 0.85f);
        }

        // 4. MA PHÁP TRẬN XOAY Ở ĐẦU NÒNG (MUZZLE CIRCLE)
        Vec3 muzzle = origin.add(d.scale(1.2));
        billboardDisc(v, m, muzzle, 4.2f, coreColor, fade * 0.85f);
        billboardDisc(v, m, muzzle, 2.2f, laserColor, fade * 0.98f);
    }

    // =========================================================================
    // 24. THẤT SẮC THẦN TRỤ (CHROMATIC LIGHT PILLAR - 3D 32-SIDED GPU VERTICAL CYLINDER)
    // =========================================================================
    private static void renderChromaticPillar(VertexConsumer v, Matrix4f m, ActiveSkillInstance skill, float age) {
        float h = 65.0f;
        float totalT = skill.totalTicks();
        float fade = (age < totalT - 15.0f) ? 1.0f : (float) Math.max(0.0, (totalT - age) / 15.0f);

        int demonOrdinal = Math.round(skill.powerMultiplier());
        int outerColor;
        int coreColor;

        if (demonOrdinal == 0) { // ROUGE
            outerColor = 0xD50000;
            coreColor = 0xFF1744;
        } else if (demonOrdinal == 1) { // NOIR
            outerColor = 0x311B92;
            coreColor = 0x7B1FA2;
        } else if (demonOrdinal == 2) { // BLANC
            outerColor = 0x80DEEA;
            coreColor = 0xE0F7FA;
        } else if (demonOrdinal == 3) { // JAUNE
            outerColor = 0xFF6D00;
            coreColor = 0xFFD600;
        } else if (demonOrdinal == 4) { // VIOLET
            outerColor = 0x4A148C;
            coreColor = 0xAA00FF;
        } else if (demonOrdinal == 5) { // BLEU
            outerColor = 0x01579B;
            coreColor = 0x00E5FF;
        } else if (demonOrdinal == 6) { // VERT
            outerColor = 0x1B5E20;
            coreColor = 0x00E676;
        } else {
            outerColor = 0xFF6D00;
            coreColor = 0xFFD600;
        }

        // 1. CỘT SÁNG THẦN KHÍ 32-SIDED SMOOTH GPU CYLINDER CAO 65M CHỌC TRỜI
        cylinder(v, m, 6.8f, h, fade * 0.40f, outerColor, age);
        cylinder(v, m, 4.2f, h, fade * 0.70f, coreColor, age + 3);
        cylinder(v, m, 1.8f, h, fade * 0.98f, 0xFFFFFF, age + 6);

        // 2. PHÁP TRẬN LỤC LĂNG ĐỒNG TÂM XOAY DƯỚI ĐẤT
        renderHexagonRing(v, m, 0.08f, 8.5f, 0.45f, coreColor, fade * 0.85f, age * 0.04f);
        renderHexagonRing(v, m, 0.12f, 5.5f, 0.35f, outerColor, fade * 0.90f, -age * 0.06f);
        renderHexagonRing(v, m, 0.16f, 3.0f, 0.25f, 0xFFFFFF, fade * 0.95f, age * 0.08f);

        // 3. 7 DẢI LỤA SÁNG CẦU VỒNG THẤT SẮC XOẮN QUANH THÂN TRỤ (CHROMATIC ASCENDING HELIX)
        int[] rainbowColors = {0xFF1744, 0x7B1FA2, 0xFFFFFF, 0xFFD600, 0xAA00FF, 0x00E5FF, 0x00E676};
        for (int c = 0; c < 7; c++) {
            double baseAngle = c * (2.0 * Math.PI / 7.0) + age * 0.04;
            int col = rainbowColors[c];
            for (int k = 0; k < 14; k++) {
                double y1 = k * (h / 14.0);
                double y2 = (k + 1) * (h / 14.0);
                double a1 = baseAngle + k * 0.45;
                double a2 = baseAngle + (k + 1) * 0.45;
                Vec3 p1 = new Vec3(Math.cos(a1) * 5.2, y1, Math.sin(a1) * 5.2);
                Vec3 p2 = new Vec3(Math.cos(a2) * 5.2, y2, Math.sin(a2) * 5.2);
                stroke(v, m, p1, p2, 0.28f, fade * 0.85f, col);
            }
        }

        // 4. VÒNG SÓNG XUNG KÍCH GIÁNG LÂM TỪ ĐỈNH TRỜI CUỘN XUỐNG MẶT ĐẤT
        for (int k = 0; k < 4; k++) {
            float ringY = ((h - (age * 3.5f + k * 16.0f) % h) + h) % h;
            float ringR = 5.2f + (1.0f - ringY / h) * 2.8f;
            ring(v, m, ringY, ringR, 0.35f, coreColor, fade * 0.80f);
        }

        // 5. GỢN SÓNG THÁNH THỦY LAN TOẢ TRÊN MẶT ĐẤT
        for (int i = 0; i < 3; i++) {
            float poolR = ((age * 1.8f + i * 7.0f) % 16.0f);
            float poolAlpha = (1.0f - poolR / 16.0f) * fade * 0.65f;
            ring(v, m, 0.05f, poolR, 0.30f, outerColor, poolAlpha);
        }
    }

    // =========================================================================
    // TIỆN ÍCH HÌNH HỌC GPU (GPU GEOMETRY PRIMITIVES)
    // =========================================================================

    private static void beamCylinder(VertexConsumer v, Matrix4f m, Vec3 start, Vec3 dir, float length, float radius, int color, float alpha, float uvOffset) {
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 d = dir.normalize();
        Vec3 up = Math.abs(d.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = d.cross(up).normalize();
        Vec3 w = d.cross(u).normalize();

        int segs = 32;
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        Vec3 end = start.add(d.scale(length));

        for (int i = 0; i < segs; i++) {
            double a1 = i * Math.PI * 2.0 / segs;
            double a2 = (i + 1) * Math.PI * 2.0 / segs;

            Vec3 r1 = u.scale(Math.cos(a1) * radius).add(w.scale(Math.sin(a1) * radius));
            Vec3 r2 = u.scale(Math.cos(a2) * radius).add(w.scale(Math.sin(a2) * radius));

            Vec3 p1 = start.add(r1);
            Vec3 p2 = start.add(r2);
            Vec3 p3 = end.add(r2);
            Vec3 p4 = end.add(r1);

            energyVertex(v, m, p1, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p2, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p3, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p4, color, opacity, 0.875f, 0.5f);
        }
    }

    private static void renderCubeMesh(VertexConsumer v, Matrix4f m, Vec3 center, float halfSize, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        float s = halfSize;
        Vec3 c = center;

        Vec3 p000 = c.add(-s, -s, -s);
        Vec3 p100 = c.add( s, -s, -s);
        Vec3 p110 = c.add( s,  s, -s);
        Vec3 p010 = c.add(-s,  s, -s);
        Vec3 p001 = c.add(-s, -s,  s);
        Vec3 p101 = c.add( s, -s,  s);
        Vec3 p111 = c.add( s,  s,  s);
        Vec3 p011 = c.add(-s,  s,  s);

        quad(v, m, p000, p100, p110, p010, color, opacity);
        quad(v, m, p101, p001, p011, p111, color, opacity);
        quad(v, m, p001, p000, p010, p011, color, opacity);
        quad(v, m, p100, p101, p111, p110, color, opacity);
        quad(v, m, p010, p110, p111, p011, color, opacity);
        quad(v, m, p000, p001, p101, p100, color, opacity);
    }

    private static void renderGeodesicDome(VertexConsumer v, Matrix4f m, Vec3 center, float radius, int color, float alpha, float rot) {
        int rings = 6;
        int sectors = 12;
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);

        for (int r = 0; r < rings; r++) {
            double phi1 = (r * Math.PI * 0.5) / rings;
            double phi2 = ((r + 1) * Math.PI * 0.5) / rings;

            float y1 = (float) (Math.sin(phi1) * radius);
            float y2 = (float) (Math.sin(phi2) * radius);
            float rad1 = (float) (Math.cos(phi1) * radius);
            float rad2 = (float) (Math.cos(phi2) * radius);

            for (int s = 0; s < sectors; s++) {
                double theta1 = s * Math.PI * 2.0 / sectors + rot;
                double theta2 = (s + 1) * Math.PI * 2.0 / sectors + rot;

                Vec3 p1 = center.add(Math.cos(theta1) * rad1, y1, Math.sin(theta1) * rad1);
                Vec3 p2 = center.add(Math.cos(theta2) * rad1, y1, Math.sin(theta2) * rad1);
                Vec3 p3 = center.add(Math.cos(theta2) * rad2, y2, Math.sin(theta2) * rad2);
                Vec3 p4 = center.add(Math.cos(theta1) * rad2, y2, Math.sin(theta1) * rad2);

                quad(v, m, p1, p2, p3, p4, color, (int) (opacity * 0.45f));
                stroke(v, m, p1, p2, 0.12f, alpha * 0.95f, color);
                stroke(v, m, p2, p3, 0.12f, alpha * 0.95f, color);
                stroke(v, m, p3, p4, 0.12f, alpha * 0.95f, color);
                stroke(v, m, p4, p1, 0.12f, alpha * 0.95f, color);
            }
        }
    }



    private static void quad(VertexConsumer v, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int color, int opacity) {
        energyVertex(v, m, p1, color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, p2, color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, p3, color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, p4, color, opacity, 0.875f, 0.5f);
    }

    private static void cylinder(VertexConsumer v, Matrix4f m, float r, float h, float alpha, int color, float age) {
        int segs = 32;
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        for (int i = 0; i < segs; i++) {
            double a1 = i * Math.PI * 2.0 / segs;
            double a2 = (i + 1) * Math.PI * 2.0 / segs;

            Vec3 p1 = new Vec3(Math.cos(a1) * r, 0, Math.sin(a1) * r);
            Vec3 p2 = new Vec3(Math.cos(a2) * r, 0, Math.sin(a2) * r);
            Vec3 p3 = new Vec3(Math.cos(a2) * r, h, Math.sin(a2) * r);
            Vec3 p4 = new Vec3(Math.cos(a1) * r, h, Math.sin(a1) * r);

            energyVertex(v, m, p1, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p2, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p3, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p4, color, opacity, 0.875f, 0.5f);
        }
    }

    private static void cylinderOffset(VertexConsumer v, Matrix4f m, float ox, float oz, float r, float h, float alpha, int color) {
        int segs = 12;
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        for (int i = 0; i < segs; i++) {
            double a1 = i * Math.PI * 2.0 / segs;
            double a2 = (i + 1) * Math.PI * 2.0 / segs;

            Vec3 p1 = new Vec3(ox + Math.cos(a1) * r, 0, oz + Math.sin(a1) * r);
            Vec3 p2 = new Vec3(ox + Math.cos(a2) * r, 0, oz + Math.sin(a2) * r);
            Vec3 p3 = new Vec3(ox + Math.cos(a2) * r, h, oz + Math.sin(a2) * r);
            Vec3 p4 = new Vec3(ox + Math.cos(a1) * r, h, oz + Math.sin(a1) * r);

            energyVertex(v, m, p1, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p2, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p3, color, opacity, 0.875f, 0.5f);
            energyVertex(v, m, p4, color, opacity, 0.875f, 0.5f);
        }
    }

    private static void renderHexagonRing(VertexConsumer v, Matrix4f m, float y, float radius, float width, int color, float alpha, float rot) {
        int sides = 6;
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        for (int i = 0; i < sides; i++) {
            double a1 = i * Math.PI * 2.0 / sides + rot;
            double a2 = (i + 1) * Math.PI * 2.0 / sides + rot;

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

    private static void ring(VertexConsumer v, Matrix4f m, float y, float radius, float width, int color, float alpha) {
        int segs = 36;
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

    private static void stroke(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float width, float alpha, int color) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        Vec3 dir = b.subtract(a).normalize();
        Vec3 right = dir.cross(new Vec3(0, 1, 0)).normalize().scale(width);
        if (right.lengthSqr() < 0.001) right = new Vec3(width, 0, 0);

        energyVertex(v, m, a.subtract(right), color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, a.add(right), color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, b.add(right), color, opacity, 0.875f, 0.5f);
        energyVertex(v, m, b.subtract(right), color, opacity, 0.875f, 0.5f);
    }

    private static void billboardDisc(VertexConsumer v, Matrix4f m, Vec3 center, float size, int color, float alpha) {
        int opacity = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        Vec3 a = center.add(-size, -size, 0);
        Vec3 b = center.add(size, -size, 0);
        Vec3 c = center.add(size, size, 0);
        Vec3 d = center.add(-size, size, 0);

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
}
