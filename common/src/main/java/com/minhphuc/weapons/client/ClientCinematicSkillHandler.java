package com.minhphuc.weapons.client;

import com.minhphuc.weapons.config.SkillVisualConfig;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * CLIENT CINEMATIC SKILL HANDLER (TRÌNH ĐIỀU PHỐI GÓC MÁY QUAY ĐIỆN ẢNH CHO TOÀN BỘ KỸ NĂNG)
 * Quản lý chuyển cảnh đa góc quay, Letterbox Widescreen (2.39:1 CinemaScope),
 * Typography tiêu đề tuyệt kỹ, và chuyển tiếp mượt mà (smoothstep) khi trả góc nhìn.
 */
public final class ClientCinematicSkillHandler {

    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static boolean active = false;
    private static int activeSkillId = 0;
    private static UUID casterUuid = null;
    private static Vec3 centerPos = Vec3.ZERO;
    private static Vec3 aimDir = new Vec3(0, 0, 1);
    private static int totalTicks = 80;
    private static long startWorldTick = 0;
    private static float powerMultiplier = 1.0f;
    private static boolean skipped = false;

    public static boolean isActive() {
        return active && !skipped;
    }

    public static boolean isCasterActive(UUID uuid) {
        return isActive() && casterUuid != null && casterUuid.equals(uuid);
    }

    public static int getActiveSkillId() {
        return activeSkillId;
    }

    public static float getAge() {
        if (!isActive()) return -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        return (float) (mc.level.getGameTime() - startWorldTick);
    }

    public static float getAge(float partialTicks) {
        if (!isActive()) return -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        return (float) (mc.level.getGameTime() - startWorldTick) + partialTicks;
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.level == null || mc.player == null) {
                if (active) stopCinematic();
                return;
            }

            if (!active || skipped) return;

            long elapsed = mc.level.getGameTime() - startWorldTick;
            if (elapsed >= totalTicks) {
                stopCinematic();
                return;
            }

            // Nhấn ESC hoặc F5 để bỏ qua cutscene
            if (mc.isWindowActive() && mc.screen == null) {
                long window = mc.getWindow().getWindow();
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F5) == GLFW.GLFW_PRESS) {
                    skip();
                }
            }
        });
    }

    public static void startCinematic(int skillId, UUID caster, Vec3 center, Vec3 dir, int durationTicks, float power) {
        if (!SkillVisualConfig.isCinematicEnabled(skillId)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Chỉ kích hoạt điều khiển camera cho chính người thi triển kỹ năng
        if (!caster.equals(mc.player.getUUID())) return;

        activeSkillId = skillId;
        casterUuid = caster;
        centerPos = center;
        aimDir = (dir != null && dir.lengthSqr() > 0.001) ? dir.normalize() : new Vec3(0, 0, 1);
        totalTicks = durationTicks;
        powerMultiplier = power;
        skipped = false;
        active = true;

        if (mc.level != null) {
            startWorldTick = mc.level.getGameTime();
        }
    }

    public static void stopCinematic(int skillId) {
        if (activeSkillId == skillId) {
            stopCinematic();
        }
    }

    public static void stopCinematic() {
        active = false;
        skipped = false;
        activeSkillId = 0;
    }

    public static void skip() {
        skipped = true;
        stopCinematic();
    }

    /**
     * TÍNH TOÁN VỊ TRÍ VÀ HƯỚNG MÁY QUAY THEO TỪNG KỸ NĂNG VÀ TỪNG HỒI
     */
    public static Pose getCameraPose(float partialTicks, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        if (!isActive()) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        float age = (float) (mc.level.getGameTime() - startWorldTick) + partialTicks;
        if (age < 0 || age >= totalTicks) return null;

        // Tính toán vị trí máy quay theo kịch bản của từng kỹ năng
        return switch (activeSkillId) {
            case SkillVisualConfig.SKILL_DISINTEGRATION -> getDisintegrationPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_JACOBS_LADDER -> getJacobsLadderPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_PURIFICATION_PILLAR -> getPurificationPillarPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_FAITH_AND_GRACE -> getFaithAndGracePose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_BEELZEBUTH -> getBeelzebuthPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_DRAGON_NOVA -> getDragonNovaPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_EXTINCTION_STARS -> getMegiddoPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_SEER_FLESH -> getSeerFleshPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_ALKAID -> getAlkaidPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_GRANITE_BLAST -> getGraniteBlastPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_REVERSE_TIME_SLASH -> getReverseTimePose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_REVERSE_VELOCITY_IAI -> getReverseVelocityPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON -> getDragonPrisonPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_MULTILAYER_BARRIER -> getMultilayerPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_UNIVERSAL_SENSE -> getUniversalSensePose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_URIEL_DEFENSE -> getUrielDefensePose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_RYKUZIN_SINKA -> getRykuzinPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_ZONE_TRACK -> getZoneTrackPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_NULLIFICATION -> getNullificationPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_SPIRITRON_OUTBURST -> getSpiritronOutburstPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            case SkillVisualConfig.SKILL_CHROMATIC_PILLAR -> getChromaticPillarPose(age, vanillaPos, vanillaYaw, vanillaPitch);
            default -> null;
        };
    }

    // =========================================================================
    // 1. LINH TỬ BĂNG HOẠI (DISINTEGRATION - 5 HỒI CHUẨN NGUYÊN TÁC HINATA)
    // =========================================================================
    private static Pose getDisintegrationPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        // Hồi 1 (0 -> 24t): Khởi xướng & Kết ấn Thánh Ngôn (Cận cảnh nửa người)
        if (age < 24.0f) {
            float progress = age / 24.0f;
            double offsetX = Math.cos(vanillaYaw * Mth.DEG_TO_RAD) * 2.8 + Math.sin(vanillaYaw * Mth.DEG_TO_RAD) * 1.0;
            double offsetZ = Math.sin(vanillaYaw * Mth.DEG_TO_RAD) * 2.8 - Math.cos(vanillaYaw * Mth.DEG_TO_RAD) * 1.0;
            camPos = vanillaPos.add(offsetX * (1.0 - progress * 0.2), 0.4 + progress * 0.3, offsetZ * (1.0 - progress * 0.2));
            lookTarget = vanillaPos.add(0, 1.2, 0);
        }
        // Hồi 2 (24 -> 48t): Thất Trọng Kết Giới Lục Lăng (Cam lùi xa bao quát trận địa)
        else if (age < 48.0f) {
            float progress = (age - 24.0f) / 24.0f;
            double dist = 28.0 + progress * 8.0;
            double angle = (age * 0.02) + 0.5;
            camPos = centerPos.add(Math.cos(angle) * dist, 14.0 + progress * 6.0, Math.sin(angle) * dist);
            lookTarget = centerPos.add(0, 6.0, 0);
        }
        // Hồi 3 (48 -> 72t): Thất Kiếm Trảm Không (Cam vút lên trời nhìn 7 vệt chém vẽ tinh trận)
        else if (age < 72.0f) {
            float progress = (age - 48.0f) / 24.0f;
            camPos = centerPos.add(12.0 * (1.0 - progress), 50.0 + progress * 20.0, 16.0 * (1.0 - progress));
            lookTarget = centerPos.add(0, 2.0, 0);
        }
        // Hồi 4 (72 -> 96t): Thất Sắc Hợp Nhất & Đại Băng Hoại (God-Eye Cam ngắm từ 120m)
        else if (age < 96.0f) {
            float progress = (age - 72.0f) / 24.0f;
            camPos = centerPos.add(24.0, 75.0 - progress * 15.0, 24.0);
            lookTarget = centerPos.add(0, 1.0, 0);
        }
        // Hồi 5 (96 -> 125t): Tinh thể vàng thăng thiên & Trả góc nhìn mượt mà
        else {
            float blend = Math.clamp((age - 96.0f) / 29.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 lastPos = centerPos.add(24.0, 60.0, 24.0);
            camPos = lastPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 96.0f, 125.0f);
    }

    // =========================================================================
    // 2. TÀ KHỨ VŨ THÊ TỬ (JACOB'S LADDER - ASCENSION SPIRAL TRACKING)
    // =========================================================================
    private static Pose getJacobsLadderPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 50.0f) {
            float progress = age / 50.0f;
            double spiralY = 6.0 + progress * 65.0; // Lượn xoắn ốc từ dưới lên trời
            double angle = progress * Math.PI * 2.5;
            double dist = 24.0 * (1.0 - progress * 0.3);
            camPos = centerPos.add(Math.cos(angle) * dist, spiralY, Math.sin(angle) * dist);
            lookTarget = centerPos.add(0, spiralY + 12.0, 0);
        } else {
            float blend = Math.clamp((age - 50.0f) / 25.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(0, 71.0, 16.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 50.0f, 75.0f);
    }

    // =========================================================================
    // 3. PHÉP MÀU CỦA THẦN LINH (PURIFICATION PILLAR - 360° DRONE ORBIT)
    // =========================================================================
    private static Pose getPurificationPillarPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 55.0f) {
            double angle = (age * 0.06);
            double dist = 22.0;
            camPos = centerPos.add(Math.cos(angle) * dist, 8.0 + Math.sin(age * 0.04) * 3.0, Math.sin(angle) * dist);
            lookTarget = centerPos.add(0, 4.0, 0);
        } else {
            float blend = Math.clamp((age - 55.0f) / 20.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(Math.cos(55.0 * 0.06) * 22.0, 8.0, Math.sin(55.0 * 0.06) * 22.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 55.0f, 75.0f);
    }

    // =========================================================================
    // 4. BÍ ẨN CỦA ĐỨC TIN VÀ ÂN SỦNG (FAITH AND GRACE - WIDE PULL-BACK)
    // =========================================================================
    private static Pose getFaithAndGracePose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 45.0f) {
            float progress = age / 45.0f;
            double dist = 12.0 + progress * 24.0;
            camPos = vanillaPos.add(-aimDir.x * dist, 4.0 + progress * 10.0, -aimDir.z * dist);
            lookTarget = vanillaPos.add(0, 2.0, 0);
        } else {
            float blend = Math.clamp((age - 45.0f) / 20.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = vanillaPos.add(-aimDir.x * 36.0, 14.0, -aimDir.z * 36.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = vanillaPos.add(0, 2.0, 0).lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 45.0f, 65.0f);
    }

    // =========================================================================
    // 5. BẠO THỰC VƯƠNG (BEELZEBUTH - STABLE MAJESTIC SHOULDER CAM, REDUCED SHAKE)
    // =========================================================================
    private static Pose getBeelzebuthPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 50.0f) {
            // Lệch vai phải ổn định (không rung lắc mạnh)
            Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
            double subtleShake = Math.sin(age * 1.2) * 0.08; // Rung siêu nhẹ
            camPos = vanillaPos.add(right.scale(2.2)).add(aimDir.scale(-4.2)).add(0, 1.2 + subtleShake, 0);
            lookTarget = centerPos.add(0, 1.0, 0);
        } else {
            float blend = Math.clamp((age - 50.0f) / 20.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 endPos = vanillaPos.add(right.scale(2.2)).add(aimDir.scale(-4.2)).add(0, 1.2, 0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 50.0f, 70.0f);
    }

    // =========================================================================
    // 6. LONG TINH BỘC VIÊM BÁ (DRAGON NOVA - CLOSE-UP HERO CHARGE TO HIGH-ALTITUDE PANORAMA)
    // =========================================================================
    private static Pose getDragonNovaPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.entity.Entity caster = (mc.level != null && casterUuid != null) ? mc.level.getPlayerByUUID(casterUuid) : null;
        Vec3 basePos = (caster != null) ? caster.position() : centerPos;

        Vec3 forward = (aimDir.lengthSqr() > 1e-4) ? aimDir.normalize() : new Vec3(0, 0, 1);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() < 1e-4) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }

        Vec3 camPos;
        Vec3 lookTarget;

        // HỒI 1: GIAI ĐOẠN TỤ LINH TỬ & QUẢ CẦU STARLIGHT (Ticks 0 -> 30)
        // Góc quay Hero Trung Cận Cảnh (Front-Quarter Low-Angle Hero Shot) để thấy rõ người chơi giơ tay ngưng tụ ma tố
        if (age < 30.0f) {
            float t = age / 30.0f;
            float ease = t * t * (3.0f - 2.0f * t);

            // Camera trôi nhẹ từ 5.2m về 4.4m ở phía trước chếch bên phải người chơi, hơi hất nhẹ lên
            double camDistForward = 5.2 - ease * 0.8;
            double camDistRight = 3.4 - ease * 0.6;
            double camHeight = 1.30 + ease * 0.15;

            camPos = basePos.add(forward.scale(camDistForward))
                    .add(right.scale(camDistRight))
                    .add(0, camHeight, 0);

            // Nhìn tập trung vào ngực người chơi - nơi quả cầu Starlight ngưng tụ
            lookTarget = basePos.add(0, 1.25, 0);
        }
        // HỒI 2: GIAI ĐOẠN KHAI HỎA CHÙM PLASMA ĐỎ 3D 75M (Ticks 30 -> 55)
        // Góc quay Siêu Toàn Cảnh Trên Cao (High-Altitude Majestic Flycam) nhìn bao quát toàn bộ 75m chùm tia và vụ nổ
        else if (age < 55.0f) {
            float beamProg = (age - 30.0f) / 25.0f;
            float ease = beamProg * beamProg * (3.0f - 2.0f * beamProg);

            // Nâng camera lên cao 19m -> 26m, lùi phía sau lưng người chơi 12m và chếch ngang 18m -> 22m
            double camDistBack = 12.0 - ease * 2.0;
            double camDistSide = 18.0 + ease * 4.0;
            double camHeight = 19.0 + ease * 7.0;

            camPos = basePos.subtract(forward.scale(camDistBack))
                    .add(right.scale(camDistSide))
                    .add(0, camHeight, 0);

            // Rung chấn Camera giật nổ uy lực lúc xuất lực cực hạn (Recoil & Thunder Shake Ticks 30..38)
            if (age < 38.0f) {
                float shakeDecay = 1.0f - (age - 30.0f) / 8.0f;
                double shakeX = Math.sin(age * 3.8) * 0.35 * shakeDecay;
                double shakeY = Math.cos(age * 4.2) * 0.25 * shakeDecay;
                camPos = camPos.add(right.scale(shakeX)).add(0, shakeY, 0);
            }

            // Điểm ngắm bao quát thân chùm plasma (35m -> 50m về phía trước)
            lookTarget = basePos.add(forward.scale(35.0 + ease * 15.0)).add(0, 3.0, 0);
        }
        // HỒI 3: ĐẠI VỤ NỔ SIÊU TÂN TINH & HOÀN GÓC QUAY MƯỢT MÀ (Ticks 55 -> 75)
        else {
            float blend = Math.clamp((age - 55.0f) / 20.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);

            Vec3 highCamPos = basePos.subtract(forward.scale(10.0))
                    .add(right.scale(22.0))
                    .add(0, 26.0, 0);

            camPos = highCamPos.lerp(vanillaPos, ease);
            lookTarget = basePos.add(forward.scale(50.0)).add(0, 3.0, 0)
                    .lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 58.0f, 75.0f);
    }

    // =========================================================================
    // 7. THẦN NỘ: TUYỆT DIỆT TINH TÚ (MEGIDDO - STRATOSPHERE TO SNIPER TRACKING)
    // =========================================================================
    private static Pose getMegiddoPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 30.0f) {
            // Tầng mây nhìn các thấu kính nước
            camPos = vanillaPos.add(10.0, 55.0, 10.0);
            lookTarget = vanillaPos.add(0, 1.0, 0);
        } else if (age < 65.0f) {
            // Cam bắn tỉa dội xuống mặt đất
            double t = (age - 30.0f) / 35.0f;
            camPos = centerPos.add(18.0, 35.0 - t * 15.0, 18.0);
            lookTarget = centerPos.add(0, 0, 0);
        } else {
            float blend = Math.clamp((age - 65.0f) / 20.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(18.0, 20.0, 18.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 65.0f, 85.0f);
    }

    // =========================================================================
    // 8. THỊ THỰC (SEER FLESH - MYSTERIOUS PANORAMIC DEMONIC CAM)
    // =========================================================================
    private static Pose getSeerFleshPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 45.0f) {
            double angle = (age * 0.04);
            camPos = vanillaPos.add(Math.cos(angle) * 8.5, 3.5, Math.sin(angle) * 8.5);
            lookTarget = vanillaPos.add(0, 1.5, 0);
        } else {
            float blend = Math.clamp((age - 45.0f) / 20.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = vanillaPos.add(Math.cos(45.0 * 0.04) * 8.5, 3.5, Math.sin(45.0 * 0.04) * 8.5);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = vanillaPos.add(0, 1.5, 0).lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 45.0f, 65.0f);
    }

    // =========================================================================
    // 9. DIỆT THẾ TÀ TINH (ALKAID - WORM-EYE VIEW OF COLOSSAL METEOROID)
    // =========================================================================
    private static Pose getAlkaidPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 60.0f) {
            // Hạ thấp góc nhìn ngước nhìn thiên thạch từ đỉnh trời
            camPos = centerPos.add(28.0, 2.5, 28.0);
            lookTarget = centerPos.add(0, 45.0 - (age / 60.0f) * 20.0, 0);
        } else {
            float blend = Math.clamp((age - 60.0f) / 25.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(28.0, 2.5, 28.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.add(0, 25.0, 0).lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 60.0f, 85.0f);
    }

    // =========================================================================
    // 10. CÚ BẮN GRANIT (GRANITE BLAST - HIP-FIRE SUPERSONIC CONE CAM)
    // =========================================================================
    private static Pose getGraniteBlastPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 35.0f) {
            Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
            double recoil = (age < 10.0f) ? (1.0f - age / 10.0f) * 0.6 : 0.0;
            camPos = vanillaPos.add(right.scale(1.8)).add(aimDir.scale(-2.8 - recoil)).add(0, 0.8, 0);
            lookTarget = vanillaPos.add(aimDir.scale(50.0));
        } else {
            float blend = Math.clamp((age - 35.0f) / 15.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 endPos = vanillaPos.add(right.scale(1.8)).add(aimDir.scale(-2.8)).add(0, 0.8, 0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = vanillaPos.add(aimDir.scale(50.0)).lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 35.0f, 50.0f);
    }

    // =========================================================================
    // 11. NGHỊCH THỜI NHẤT TRẢM (REVERSE-TIME SLASH - DARK GATHERING OVER-SHOULDER PERSPECTIVE)
    // =========================================================================
    private static Pose getReverseTimePose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        // Chuẩn nguyên tác Dark Gathering: KHÔNG XOAY GÓC MÁY QUAY LÀM MẤT TẦM NHÌN
        // Giữ góc nhìn vai hướng thẳng phía trước để người chơi thấy rõ phân thân tàn ảnh phóng từ cơ thể mình lao tới chém địch
        Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 camPos = vanillaPos.add(right.scale(0.7)).add(aimDir.scale(-2.0)).add(0, 0.4, 0);
        Vec3 lookTarget = vanillaPos.add(aimDir.scale(35.0));
        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 16.0f, 25.0f);
    }

    // =========================================================================
    // 12. BẠT ĐAO THUẬT VẬN TỐC NGƯỢC (REVERSE-VELOCITY IAI - FLASH-STEP CUT)
    // =========================================================================
    private static Pose getReverseVelocityPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 15.0f) {
            // Góc 1: Phía sau bạt đao
            camPos = vanillaPos.add(aimDir.scale(-3.5)).add(0, 1.1, 0);
            lookTarget = vanillaPos.add(aimDir.scale(10.0));
        } else if (age < 32.0f) {
            // Góc 2: Flash-Step trực diện kiếm tra vào bao
            camPos = centerPos.add(aimDir.scale(5.0)).add(0, 1.2, 0);
            lookTarget = centerPos.add(0, 1.2, 0);
        } else {
            float blend = Math.clamp((age - 32.0f) / 13.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(aimDir.scale(5.0)).add(0, 1.2, 0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 32.0f, 45.0f);
    }

    // =========================================================================
    // 13. LỒNG GIAM VÔ HẠN (INFINITE DRAGON PRISON - CRYSTAL ORBIT CAM)
    // =========================================================================
    private static Pose getDragonPrisonPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 40.0f) {
            double angle = (age * 0.05);
            camPos = centerPos.add(Math.cos(angle) * 16.0, 7.0, Math.sin(angle) * 16.0);
            lookTarget = centerPos.add(0, 3.0, 0);
        } else {
            float blend = Math.clamp((age - 40.0f) / 15.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(Math.cos(40.0 * 0.05) * 16.0, 7.0, Math.sin(40.0 * 0.05) * 16.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 40.0f, 55.0f);
    }

    // =========================================================================
    // 14. ĐA TRÙNG KẾT GIỚI (MULTILAYER BARRIER - DYNAMIC THIRD-PERSON SHIELD)
    // =========================================================================
    private static Pose getMultilayerPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos = vanillaPos.add(aimDir.scale(-4.5)).add(0, 1.5, 0);
        Vec3 lookTarget = vanillaPos.add(aimDir.scale(10.0));
        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 20.0f, 25.0f);
    }

    // =========================================================================
    // 15. CẢM NHẬN VẠN NĂNG (UNIVERSAL SENSE - CLAIRVOYANCE MULTI-TARGET TOUR)
    // =========================================================================
    private static Pose getUniversalSensePose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        double scanR = powerMultiplier > 0 ? powerMultiplier : 384.0;
        double scanRSq = scanR * scanR;

        // 1. Thu thập TẤT CẢ các sinh vật sống được phát hiện trong phạm vi (bỏ qua đồng minh khế ước)
        List<net.minecraft.world.entity.LivingEntity> detectedEntities = new ArrayList<>();
        for (net.minecraft.world.entity.Entity ent : mc.level.entitiesForRendering()) {
            if (ent instanceof net.minecraft.world.entity.LivingEntity le && ent != mc.player && le.isAlive()) {
                if (com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(mc.player.getUUID(), le)) {
                    continue;
                }
                double dSq = mc.player.distanceToSqr(ent);
                if (dSq <= scanRSq && dSq >= 2.5) {
                    detectedEntities.add(le);
                }
            }
        }

        // Sắp xếp thứ tự ưu tiên: ƯU TIÊN SỐ 1 các sinh vật thù địch có thể tấn công người chơi trước, sau đó theo khoảng cách gần nhất
        detectedEntities.sort((a, b) -> {
            boolean aHostile = com.minhphuc.weapons.content.evolution.UniversalSenseAbility.isHostileThreat(a, mc.player);
            boolean bHostile = com.minhphuc.weapons.content.evolution.UniversalSenseAbility.isHostileThreat(b, mc.player);
            if (aHostile != bHostile) {
                return aHostile ? -1 : 1;
            }
            return Double.compare(mc.player.distanceToSqr(a), mc.player.distanceToSqr(b));
        });

        // Giới hạn tối đa 6 mục tiêu để phân bổ góc lia camera chuẩn điện ảnh
        if (detectedEntities.size() > 6) {
            detectedEntities = detectedEntities.subList(0, 6);
        }

        if (detectedEntities.isEmpty()) {
            // Trường hợp không có sinh vật nào xung quanh: Lia tầm nhìn bao quát chân trời rồi quay về
            Vec3 forwardScout = vanillaPos.add(aimDir.scale(20.0)).add(0, 2.0, 0);
            float progress = Math.min(1.0f, age / Math.max(1.0f, (float) totalTicks));
            float ease = (float) Math.sin(progress * Math.PI);
            Vec3 camPos = vanillaPos.lerp(forwardScout, ease * 0.4f);
            Vec3 lookTarget = vanillaPos.add(aimDir.scale(40.0));
            return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, totalTicks * 0.6f, totalTicks);
        }

        int count = detectedEntities.size();
        float introDuration = 12.0f;
        float outroDuration = 14.0f;
        float tourTotalDuration = Math.max(10.0f, totalTicks - introDuration - outroDuration);
        float slicePerTarget = tourTotalDuration / count;

        Vec3 camPos;
        Vec3 lookTarget;

        if (age < introDuration) {
            // Giai đoạn Khởi đầu: Camera lướt nhanh từ người chơi tới Mục tiêu đầu tiên
            float t = age / introDuration;
            float ease = t * t * (3.0f - 2.0f * t);
            net.minecraft.world.entity.LivingEntity first = detectedEntities.get(0);
            Vec3 firstTargetPos = first.position().add(0, first.getBbHeight() * 0.65, 0);
            Vec3 toPlayer = vanillaPos.subtract(firstTargetPos);
            Vec3 dir = toPlayer.lengthSqr() > 0.01 ? toPlayer.normalize() : aimDir.scale(-1);
            Vec3 firstCamPos = firstTargetPos.add(dir.scale(3.5)).add(0, 1.2, 0);

            camPos = vanillaPos.lerp(firstCamPos, ease);
            lookTarget = vanillaPos.add(aimDir.scale(10.0)).lerp(firstTargetPos, ease);

            mc.player.displayClientMessage(
                    Component.literal("§b👁 [VẠN NĂNG THẦN NHÃN] §fĐang tiếp cận mục tiêu: §e" + first.getName().getString() + " §7(1/" + count + ")"),
                    true
            );
        } else if (age < totalTicks - outroDuration) {
            // Giai đoạn Tour Lần Lượt TẤT CẢ các sinh vật đã phát hiện
            float tourAge = age - introDuration;
            int targetIdx = Math.min(count - 1, (int) (tourAge / slicePerTarget));
            float targetProgress = (tourAge - targetIdx * slicePerTarget) / slicePerTarget;

            net.minecraft.world.entity.LivingEntity currentTarget = detectedEntities.get(targetIdx);
            Vec3 curTargetPos = currentTarget.position().add(0, currentTarget.getBbHeight() * 0.65, 0);
            Vec3 toPlayer = vanillaPos.subtract(curTargetPos);
            Vec3 dir = toPlayer.lengthSqr() > 0.01 ? toPlayer.normalize() : aimDir.scale(-1);
            Vec3 curCamPos = curTargetPos.add(dir.scale(3.5)).add(0, 1.2, 0);

            mc.player.displayClientMessage(
                    Component.literal("§b👁 [VẠN NĂNG THẦN NHÃN] §fMục tiêu: §e" + currentTarget.getName().getString()
                            + " §a[" + (int) currentTarget.getHealth() + "/" + (int) currentTarget.getMaxHealth() + " HP] §7(" + (targetIdx + 1) + "/" + count + ")"),
                    true
            );

            if (targetIdx < count - 1 && targetProgress > 0.65f) {
                // Đang lia chuyển giao mượt mà sang mục tiêu tiếp theo
                float transitionT = (targetProgress - 0.65f) / 0.35f;
                float ease = transitionT * transitionT * (3.0f - 2.0f * transitionT);
                net.minecraft.world.entity.LivingEntity nextTarget = detectedEntities.get(targetIdx + 1);
                Vec3 nextTargetPos = nextTarget.position().add(0, nextTarget.getBbHeight() * 0.65, 0);
                Vec3 nextToPlayer = vanillaPos.subtract(nextTargetPos);
                Vec3 nextDir = nextToPlayer.lengthSqr() > 0.01 ? nextToPlayer.normalize() : aimDir.scale(-1);
                Vec3 nextCamPos = nextTargetPos.add(nextDir.scale(3.5)).add(0, 1.2, 0);

                camPos = curCamPos.lerp(nextCamPos, ease);
                lookTarget = curTargetPos.lerp(nextTargetPos, ease);
            } else {
                // Quét quan sát rõ nét cận cảnh mục tiêu hiện tại với quỹ đạo lơ lửng nhẹ
                double orbitAngle = (targetProgress - 0.35f) * 0.35;
                Vec3 offset = curCamPos.subtract(curTargetPos);
                double distH = Math.hypot(offset.x, offset.z);
                double baseAngle = Math.atan2(offset.z, offset.x);
                double newAngle = baseAngle + orbitAngle;
                camPos = new Vec3(curTargetPos.x + Math.cos(newAngle) * distH, curCamPos.y, curTargetPos.z + Math.sin(newAngle) * distH);
                lookTarget = curTargetPos;
            }
        } else {
            // Giai đoạn Quay Về: Lướt mượt mà từ mục tiêu cuối cùng trở lại người chơi
            float outroAge = age - (totalTicks - outroDuration);
            float t = Math.clamp(outroAge / outroDuration, 0.0f, 1.0f);
            float ease = t * t * (3.0f - 2.0f * t);

            net.minecraft.world.entity.LivingEntity last = detectedEntities.get(count - 1);
            Vec3 lastTargetPos = last.position().add(0, last.getBbHeight() * 0.65, 0);
            Vec3 toPlayer = vanillaPos.subtract(lastTargetPos);
            Vec3 dir = toPlayer.lengthSqr() > 0.01 ? toPlayer.normalize() : aimDir.scale(-1);
            Vec3 lastCamPos = lastTargetPos.add(dir.scale(3.5)).add(0, 1.2, 0);

            camPos = lastCamPos.lerp(vanillaPos, ease);
            lookTarget = lastTargetPos.lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, totalTicks - outroDuration, totalTicks);
    }

    // =========================================================================
    // 16. PHÒNG NGỰ TUYỆT ĐỐI URIEL (SOLAR CORONA DEFENSE CAM)
    // =========================================================================
    private static Pose getUrielDefensePose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos = vanillaPos.add(aimDir.scale(-3.8)).add(0, 1.2, 0);
        Vec3 lookTarget = vanillaPos.add(aimDir.scale(8.0));
        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 15.0f, 20.0f);
    }

    // =========================================================================
    // 17. TIÊN TRI: RYKUZIN SINKA (PRE-COGNITION GHOST PERSPECTIVE)
    // =========================================================================
    private static Pose getRykuzinPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos = vanillaPos.add(aimDir.scale(-2.8)).add(0, 1.4, 0);
        Vec3 lookTarget = vanillaPos.add(aimDir.scale(12.0));
        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 15.0f, 20.0f);
    }

    // =========================================================================
    // 18. ZONE TRACK (PURE WHITE BEAM TRACKING CAM)
    // =========================================================================
    private static Pose getZoneTrackPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 camPos = vanillaPos.add(right.scale(1.5)).add(aimDir.scale(-2.5)).add(0, 0.9, 0);
        Vec3 lookTarget = vanillaPos.add(aimDir.scale(80.0));
        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 25.0f, 35.0f);
    }

    // =========================================================================
    // 19. PHONG SÁT KĨ NĂNG (VACUUM NULL SHOCKWAVE CAM)
    // =========================================================================
    private static Pose getNullificationPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos = vanillaPos.add(0, 4.0 + (age / 25.0f) * 6.0, -8.0);
        Vec3 lookTarget = vanillaPos.add(0, 1.0, 0);
        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 20.0f, 25.0f);
    }

    // =========================================================================
    // 23. LINH TỬ BỘC PHÁT (SPIRITRON OUTBURST - SHOULDER RECOIL CAM)
    // =========================================================================
    private static Pose getSpiritronOutburstPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 35.0f) {
            Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
            double recoil = (age < 8.0f) ? (1.0f - age / 8.0f) * 0.5 : 0.0;
            camPos = vanillaPos.add(right.scale(1.6)).add(aimDir.scale(-2.5 - recoil)).add(0, 0.7, 0);
            lookTarget = vanillaPos.add(aimDir.scale(50.0));
        } else {
            float blend = Math.clamp((age - 35.0f) / 10.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 right = aimDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 endPos = vanillaPos.add(right.scale(1.6)).add(aimDir.scale(-2.5)).add(0, 0.7, 0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = vanillaPos.add(aimDir.scale(50.0)).lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 35.0f, 45.0f);
    }

    // =========================================================================
    // 24. THẤT SẮC THẦN TRỤ (CHROMATIC LIGHT PILLAR - WIDE ASCENDING CAM)
    // =========================================================================
    private static Pose getChromaticPillarPose(float age, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 65.0f) {
            camPos = centerPos.add(22.0, 4.0, 22.0);
            lookTarget = centerPos.add(0, 20.0, 0);
        } else {
            float blend = Math.clamp((age - 65.0f) / 15.0f, 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            Vec3 endPos = centerPos.add(22.0, 4.0, 22.0);
            camPos = endPos.lerp(vanillaPos, ease);
            lookTarget = centerPos.add(0, 20.0, 0).lerp(vanillaPos.add(aimDir.scale(10.0)), ease);
        }

        return buildLookAtPose(camPos, lookTarget, vanillaYaw, vanillaPitch, age, 65.0f, 80.0f);
    }

    /**
     * HÀM PHỤ TRỢ: TÍNH TOÁN YAW/PITCH TỪ VỊ TRÍ MÁY QUAY ĐẾN ĐIỂM NHÌN & LERP MƯỢT MÀ
     */
    private static Pose buildLookAtPose(Vec3 camPos, Vec3 lookTarget, float vanillaYaw, float vanillaPitch, float age, float blendStart, float blendEnd) {
        Vec3 dir = lookTarget.subtract(camPos);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));

        if (age >= blendStart) {
            float blend = Math.clamp((age - blendStart) / (blendEnd - blendStart), 0.0f, 1.0f);
            float ease = blend * blend * (3.0f - 2.0f * blend);
            float finalYaw = net.minecraft.util.Mth.rotLerp(ease, targetYaw, vanillaYaw);
            float finalPitch = net.minecraft.util.Mth.lerp(ease, targetPitch, vanillaPitch);
            return new Pose(camPos, finalYaw, finalPitch);
        }

        return new Pose(camPos, targetYaw, targetPitch);
    }

    /**
     * RENDER HUD ĐIỆN ẢNH: WIDESCREEN LETTERBOX BARS (2.39:1) & TYPOGRAPHY TIÊU ĐỀ TUYỆT KỸ
     */
    public static void renderCinematicOverlay(GuiGraphics graphics, float partialTick) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        float age = getAge(partialTick);
        if (age < 0 || age >= totalTicks) return;

        // 1. Dải đen điện ảnh CinemaScope 2.39:1 trên và dưới màn hình
        int barHeight = Math.max(24, height / 8);
        float barAlpha = Math.min(1.0f, age / 8.0f) * Math.max(0.0f, 1.0f - (age - (totalTicks - 10)) / 10.0f);
        int blackColor = ((int) (barAlpha * 255.0f) << 24);

        graphics.fill(0, 0, width, barHeight, blackColor);
        graphics.fill(0, height - barHeight, width, height, blackColor);

        // 2. Typography tiêu đề Tuyệt Kỹ xuất hiện hoành tráng trong 40 ticks đầu
        if (age >= 4.0f && age <= 48.0f) {
            float titleFade = Math.min(1.0f, (age - 4.0f) / 8.0f) * Math.max(0.0f, 1.0f - (age - 38.0f) / 10.0f);
            int textAlpha = (int) (titleFade * 255.0f);
            if (textAlpha > 4) {
                String titleVi = getSkillTitleVi(activeSkillId);
                String titleJp = getSkillTitleJp(activeSkillId);
                int titleColor = getSkillColor(activeSkillId) | (textAlpha << 24);
                int subColor = 0xCCCCCC | (textAlpha << 24);

                graphics.pose().pushPose();
                float scale = 1.35f;
                graphics.pose().scale(scale, scale, 1.0f);
                int tx = (int) ((width / scale - mc.font.width(titleVi)) / 2);
                int ty = (int) ((barHeight * 0.35f) / scale);
                graphics.drawString(mc.font, titleVi, tx, ty, titleColor, true);
                graphics.pose().popPose();

                int jx = (width - mc.font.width(titleJp)) / 2;
                int jy = barHeight + 8;
                graphics.drawString(mc.font, titleJp, jx, jy, subColor, true);
            }
        }
    }

    private static String getSkillTitleVi(int skillId) {
        return switch (skillId) {
            case SkillVisualConfig.SKILL_DISINTEGRATION -> "LINH TỬ BĂNG HOẠI: THÁNH DIỆT QUANG TRỤ";
            case SkillVisualConfig.SKILL_JACOBS_LADDER -> "TÀ KHỨ VŨ THÊ TỬ: THANG TRỜI THIÊN THẦN";
            case SkillVisualConfig.SKILL_PURIFICATION_PILLAR -> "PHÉP MÀU THẦN LINH: QUANG TRỤ THANH TẨY";
            case SkillVisualConfig.SKILL_FAITH_AND_GRACE -> "ĐỨC TIN & ÂN SỦNG: THÁNH ĐƯỜNG BẢO HỘ";
            case SkillVisualConfig.SKILL_BEELZEBUTH -> "BẠO THỰC VƯƠNG: HẮC ĐỘNG THÔN PHỆ";
            case SkillVisualConfig.SKILL_DRAGON_NOVA -> "PHẪN NỘ VƯƠNG: LONG TINH BỘC VIÊM BÁ";
            case SkillVisualConfig.SKILL_EXTINCTION_STARS -> "THẦN NỘ: TUYỆT DIỆT TINH TÚ (MEGIDDO)";
            case SkillVisualConfig.SKILL_SEER_FLESH -> "THỊ THỰC: MA NHÃN THẤU THỊ VẠN GIỚI";
            case SkillVisualConfig.SKILL_ALKAID -> "DIỆT THẾ TÀ TINH: ĐẠI THIÊN THẠCH ALKAID";
            case SkillVisualConfig.SKILL_GRANITE_BLAST -> "CÚ BẮN GRANIT: PHÁO NÓN GIA TỐC HẠT";
            case SkillVisualConfig.SKILL_REVERSE_TIME_SLASH -> "NGHỊCH THỜI NHẤT TRẢM: THỜI GIAN NGƯNG ĐỌNG";
            case SkillVisualConfig.SKILL_REVERSE_VELOCITY_IAI -> "BẠT ĐAO THUẬT VẬN TỐC NGƯỢC: DƯ ẢNH TỐC BIẾN";
            case SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON -> "LỒNG GIAM VÔ HẠN: KHỐI KIM CƯƠNG BẤT BIẾN";
            case SkillVisualConfig.SKILL_MULTILAYER_BARRIER -> "ĐA TRÙNG KẾT GIỚI: BÁT TRÙNG HỘ THỂ";
            case SkillVisualConfig.SKILL_UNIVERSAL_SENSE -> "CẢM NHẬN VẠN NĂNG: SÓNG RADAR KHÔNG GIAN";
            case SkillVisualConfig.SKILL_URIEL_DEFENSE -> "PHÒNG NGỰ TUYỆT ĐỐI CỦA URIEL: THÁI DƯƠNG HỎA QUẦNG";
            case SkillVisualConfig.SKILL_RYKUZIN_SINKA -> "TIÊN TRI RYKUZIN: ẢO ẢNH DỰ ĐOÁN TƯƠNG LAI";
            case SkillVisualConfig.SKILL_ZONE_TRACK -> "ZONE TRACK: LƯỢNG TỬ TỬ QUANG";
            case SkillVisualConfig.SKILL_NULLIFICATION -> "PHONG SÁT KĨ NĂNG: CHÂN KHÔNG QUÉT PHẲNG";
            case SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY -> "DỊ GIỚI THẦN MÔN: THÁNH VỰC KHÔNG GIAN";
            case SkillVisualConfig.SKILL_SPIRITRON_OUTBURST -> "LINH TỬ BỘC PHÁT: ĐẠI PHÁO NĂNG LƯỢNG THỦY TỔ";
            case SkillVisualConfig.SKILL_CHROMATIC_PILLAR -> "THẤT SẮC THẦN TRỤ: CỘT SÁNG CỰC ĐẠI";
            default -> "TUYỆT KỸ MA VƯƠNG TENSURA";
        };
    }

    private static String getSkillTitleJp(int skillId) {
        return switch (skillId) {
            case SkillVisualConfig.SKILL_DISINTEGRATION -> "霊子崩壊 - DISINTEGRATION";
            case SkillVisualConfig.SKILL_JACOBS_LADDER -> "ヤコブの梯子 - JACOB'S LADDER";
            case SkillVisualConfig.SKILL_PURIFICATION_PILLAR -> "神の奇跡 - MIRACLE OF THE GODS";
            case SkillVisualConfig.SKILL_FAITH_AND_GRACE -> "信仰と恩寵の神秘 - FAITH AND GRACE";
            case SkillVisualConfig.SKILL_BEELZEBUTH -> "暴食之王 - BEELZEBUTH";
            case SkillVisualConfig.SKILL_DRAGON_NOVA -> "憤怒之王 - DRAGON NOVA";
            case SkillVisualConfig.SKILL_EXTINCTION_STARS -> "神之怒 - MEGIDDO";
            case SkillVisualConfig.SKILL_SEER_FLESH -> "視肉 - SEER FLESH";
            case SkillVisualConfig.SKILL_ALKAID -> "破軍星 - ALKAID";
            case SkillVisualConfig.SKILL_GRANITE_BLAST -> "グラニットブラスト - GRANITE BLAST";
            case SkillVisualConfig.SKILL_REVERSE_TIME_SLASH -> "逆時一斬 - REVERSE TIME SLASH";
            case SkillVisualConfig.SKILL_REVERSE_VELOCITY_IAI -> "逆速居合術 - REVERSE VELOCITY IAI";
            case SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON -> "無限牢獄 - INFINITE PRISON";
            case SkillVisualConfig.SKILL_MULTILAYER_BARRIER -> "多重結界 - MULTILAYER BARRIER";
            case SkillVisualConfig.SKILL_UNIVERSAL_SENSE -> "万能感知 - UNIVERSAL SENSE";
            case SkillVisualConfig.SKILL_URIEL_DEFENSE -> "誓約之王 - URIEL DEFENSE";
            case SkillVisualConfig.SKILL_RYKUZIN_SINKA -> "六壬神課 - RYKUZIN SINKA";
            case SkillVisualConfig.SKILL_ZONE_TRACK -> "ゾーントラック - ZONE TRACK";
            case SkillVisualConfig.SKILL_NULLIFICATION -> "能力封殺 - SKILL NULLIFICATION";
            case SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY -> "次元神門 - DIMENSIONAL SANCTUARY";
            case SkillVisualConfig.SKILL_SPIRITRON_OUTBURST -> "霊子暴発 - SPIRITRON OUTBURST";
            case SkillVisualConfig.SKILL_CHROMATIC_PILLAR -> "七色神柱 - CHROMATIC LIGHT PILLAR";
            default -> "ULTIMATE SKILL";
        };
    }

    private static int getSkillColor(int skillId) {
        return switch (skillId) {
            case SkillVisualConfig.SKILL_DISINTEGRATION -> 0xFFEA00; // Vàng thánh quang
            case SkillVisualConfig.SKILL_JACOBS_LADDER -> 0x00E5FF;  // Cyan thiên sứ
            case SkillVisualConfig.SKILL_PURIFICATION_PILLAR -> 0x76FF03; // Lục bảo
            case SkillVisualConfig.SKILL_FAITH_AND_GRACE -> 0xFF4081; // Hồng thánh đường
            case SkillVisualConfig.SKILL_BEELZEBUTH -> 0xAA00FF;     // Tím hắc ám
            case SkillVisualConfig.SKILL_DRAGON_NOVA -> 0xFF1744;     // Đỏ Starlight
            case SkillVisualConfig.SKILL_EXTINCTION_STARS -> 0xFFD600; // Vàng mặt trời
            case SkillVisualConfig.SKILL_SEER_FLESH -> 0xD50000;      // Đỏ thẫm ma nhãn
            case SkillVisualConfig.SKILL_ALKAID -> 0xC51162;          // Đỏ tím thiên thạch
            case SkillVisualConfig.SKILL_GRANITE_BLAST -> 0xFF6D00;   // Cam hạt nhân
            case SkillVisualConfig.SKILL_REVERSE_TIME_SLASH -> 0xE0E0E0; // Trắng bạc
            case SkillVisualConfig.SKILL_REVERSE_VELOCITY_IAI -> 0xFF5252; // Đỏ máu
            case SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON -> 0x64FFDA; // Xanh kim cương
            case SkillVisualConfig.SKILL_MULTILAYER_BARRIER -> 0x40C4FF; // Xanh khiên
            case SkillVisualConfig.SKILL_UNIVERSAL_SENSE -> 0x69F0AE; // Xanh ngọc sonar
            case SkillVisualConfig.SKILL_URIEL_DEFENSE -> 0xFFAB00;   // Vàng thái dương
            case SkillVisualConfig.SKILL_RYKUZIN_SINKA -> 0xFFFF00;   // Vàng tiên tri
            case SkillVisualConfig.SKILL_ZONE_TRACK -> 0xFFFFFF;      // Trắng tinh khiết
            case SkillVisualConfig.SKILL_NULLIFICATION -> 0x9E9E9E;   // Xám chân không
            case SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY -> 0x00E5FF; // Xanh lam dị giới
            case SkillVisualConfig.SKILL_SPIRITRON_OUTBURST -> 0xFFD600; // Hoàng kim linh tử
            case SkillVisualConfig.SKILL_CHROMATIC_PILLAR -> 0xFF1744;   // Thất sắc quang trụ
            default -> 0xFFFFFF;
        };
    }
}
