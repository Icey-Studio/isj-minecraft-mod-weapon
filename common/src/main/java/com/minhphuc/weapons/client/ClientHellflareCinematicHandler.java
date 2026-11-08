package com.minhphuc.weapons.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * CLIENT HELLFLARE CINEMATIC HANDLER (HẮC VIÊM HỎA CẦU & HẮC VIÊM NGỤC)
 * Điều khiển toàn bộ camera điện ảnh hoành tráng đa góc độ:
 * - Hồi 1 (0..24 ticks): Wide Orbit Crane Shot - Người chơi bay bổng lên cao ngưng tụ ma hỏa
 * - Hồi 2 (25..37 ticks): Over-The-Shoulder Recoil Shot - Phóng hỏa cầu hắc ám xé toạc bầu trời
 * - Hồi 3 (38..54 ticks): Epic Ground Witness Shot - Không gian giam hãm từ 1m bành trướng thành khối lập phương 24m
 * - Hồi 4 (55..75 ticks): Dark Plasma Inferno Eruption & Seamless Return - Hắc hỏa bùng nổ và hoàn góc quay
 */
public class ClientHellflareCinematicHandler {

    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static boolean active = false;
    private static UUID casterUuid = null;
    private static Vec3 startEyePos = Vec3.ZERO;
    private static Vec3 targetPos = Vec3.ZERO;
    private static final int totalDurationTicks = 75;
    private static long startWorldTick = 0;
    private static boolean skipped = false;

    public static boolean isActive() {
        return active && !skipped;
    }

    public static boolean isCasterActive(UUID uuid) {
        return isActive() && casterUuid != null && casterUuid.equals(uuid);
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

    public static void startCinematic(UUID caster, Vec3 startEye, Vec3 target) {
        casterUuid = caster;
        startEyePos = startEye;
        targetPos = target;
        skipped = false;
        active = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            startWorldTick = mc.level.getGameTime();
        }
    }

    public static void stopCinematic() {
        active = false;
        skipped = false;
        casterUuid = null;
    }

    public static void skip() {
        skipped = true;
        stopCinematic();
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.level == null || mc.player == null) {
                if (active) stopCinematic();
                return;
            }

            if (!active || skipped) return;

            long currentTick = mc.level.getGameTime();
            long elapsed = currentTick - startWorldTick;
            if (elapsed >= totalDurationTicks) {
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

    public static Pose getCameraPose(float partialTicks, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        if (!isActive()) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        boolean isSelf = mc.player.getUUID().equals(casterUuid);
        if (!isSelf) return null;

        float age = getAge(partialTicks);
        if (age < 0 || age >= totalDurationTicks) return null;

        Vec3 casterPos = mc.player.getPosition(partialTicks);
        Vec3 casterEye = mc.player.getEyePosition(partialTicks);

        Vec3 dir = targetPos.subtract(startEyePos);
        double distHoriz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (distHoriz < 0.001) distHoriz = 0.001;
        Vec3 dirNorm = dir.normalize();
        Vec3 right = new Vec3(-dirNorm.z, 0, dirNorm.x).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        // =========================================================================
        // HỒI 1: BAY BỔNG LÊN CAO & NGƯNG TỤ MA HỎA (Ticks 0 -> 24)
        // Wide Orbit Crane Shot xoay quanh người chơi đang lơ lửng giữa trời
        // =========================================================================
        if (age < 24.0f) {
            float t = age / 24.0f;
            float ease = t * t * (3.0f - 2.0f * t);

            double baseAngle = Math.atan2(dirNorm.z, dirNorm.x) - Math.PI * 0.45;
            double currentAngle = baseAngle + ease * Math.PI * 0.4;
            double camDist = 6.8;
            double heightOffset = -1.2 + ease * 2.2;

            Vec3 camPos = new Vec3(
                    casterPos.x + Math.cos(currentAngle) * camDist,
                    casterPos.y + 1.2 + heightOffset,
                    casterPos.z + Math.sin(currentAngle) * camDist
            );

            Vec3 lookTarget = casterPos.add(0, 1.3, 0);
            return calculateLookPose(camPos, lookTarget);
        }

        // =========================================================================
        // HỒI 2: PHÓNG CẦU LỬA ĐEN (Ticks 24 -> 38)
        // Over-The-Shoulder Recoil Cam theo dõi đường đạn hỏa cầu bay xé gió
        // =========================================================================
        if (age < 38.0f) {
            float t = (age - 24.0f) / 14.0f;
            float shoulderBlend = Math.min(1.0f, (age - 24.0f) / 3.0f);

            Vec3 shoulderPos = casterEye
                    .subtract(dirNorm.scale(3.4))
                    .add(right.scale(1.25))
                    .add(up.scale(0.35));

            // Hiệu ứng giật camera khi bắn tại tick 28
            if (age >= 28.0f) {
                float recoilProgress = (age - 28.0f) / 8.0f;
                float recoilDecay = (float) Math.exp(-recoilProgress * 3.5f);
                float kick = (float) Math.sin((age - 28.0f) * 1.8f) * recoilDecay * 0.35f;
                shoulderPos = shoulderPos.add(0, kick, 0);
            }

            // Quả cầu lửa đang bay từ startEyePos tới targetPos
            float projProgress = Math.clamp((age - 28.0f) / 10.0f, 0.0f, 1.0f);
            Vec3 fireballPos = lerpVec(startEyePos, targetPos, projProgress);

            return calculateLookPose(shoulderPos, fireballPos);
        }

        // =========================================================================
        // HỒI 3: KHÔNG GIAN BÀNH TRƯỚNG 1M -> 24M (Ticks 38 -> 55)
        // Epic Ground Witness Shot ngắm nhìn khối lập phương khổng lồ khóa chặt mục tiêu
        // =========================================================================
        Vec3 witnessOffset = right.scale(18.0).subtract(dirNorm.scale(16.0)).add(up.scale(4.5));
        Vec3 witnessCamPos = targetPos.add(witnessOffset);

        if (age < 55.0f) {
            float expandProgress = (age - 38.0f) / 17.0f;
            // Camera hơi nâng nhẹ góc nhìn khi hộp nở rộng lên cao
            Vec3 lookCenter = targetPos.add(0, expandProgress * 4.0, 0);
            return calculateLookPose(witnessCamPos, lookCenter);
        }

        // =========================================================================
        // HỒI 4: HẮC VIÊM BẠO LIỆT & HOÀN GÓC QUAY (Ticks 55 -> 75)
        // Hỏa ngục bùng nổ, camera lướt mượt mà trở lại góc nhìn nhân vật
        // =========================================================================
        float returnProgress = (age - 55.0f) / 20.0f;
        float easeReturn = returnProgress * returnProgress * (3.0f - 2.0f * returnProgress);

        // Góc nhìn hồi 3 nhìn vào tâm ngục
        Pose witnessPose = calculateLookPose(witnessCamPos, targetPos.add(0, 4.0, 0));

        Vec3 blendedPos = lerpVec(witnessPose.position(), vanillaPos, easeReturn);
        float blendedYaw = Mth.rotLerp(easeReturn, witnessPose.yaw(), vanillaYaw);
        float blendedPitch = Mth.rotLerp(easeReturn, witnessPose.pitch(), vanillaPitch);

        return new Pose(blendedPos, blendedYaw, blendedPitch);
    }

    private static Pose calculateLookPose(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double distHoriz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Mth.atan2(delta.y, distHoriz) * (180.0 / Math.PI)));
        return new Pose(from, yaw, pitch);
    }

    private static Vec3 lerpVec(Vec3 a, Vec3 b, float t) {
        return new Vec3(
                Mth.lerp(t, a.x, b.x),
                Mth.lerp(t, a.y, b.y),
                Mth.lerp(t, a.z, b.z)
        );
    }

    /**
     * Tư thế hoạt ảnh 3D nhân vật cho DeathBlessingPoseMixin
     */
    public static void applyPose(LivingEntity entity, ModelPart rightArm, ModelPart leftArm,
                                 ModelPart body, ModelPart head, ModelPart hat, float age) {
        // Giai đoạn 1 (0 -> 24 ticks): Nâng người bay bổng, 2 tay giang rộng tụ ma hỏa
        if (age < 24.0f) {
            float t = Math.min(1.0f, age / 18.0f);
            float ease = t * t * (3.0f - 2.0f * t);

            rightArm.xRot = -1.25f * ease;
            rightArm.yRot = -0.2f * ease;
            rightArm.zRot = 0.35f * ease;

            leftArm.xRot = -1.25f * ease;
            leftArm.yRot = 0.2f * ease;
            leftArm.zRot = -0.35f * ease;

            head.xRot = -0.22f * ease;
            hat.copyFrom(head);
            body.xRot = -0.05f * ease;
        }
        // Giai đoạn 2 (24 -> 38 ticks): Bắn hỏa cầu! Hai tay đẩy mạnh ra phía trước
        else if (age < 38.0f) {
            float launchP = Math.min(1.0f, (age - 24.0f) / 4.0f);
            rightArm.xRot = -1.60f * launchP;
            rightArm.yRot = -0.1f * launchP;
            rightArm.zRot = 0.05f;

            leftArm.xRot = -1.60f * launchP;
            leftArm.yRot = 0.1f * launchP;
            leftArm.zRot = -0.05f;

            body.xRot = 0.12f * launchP; // Giật lùi chống phản lực
            head.xRot = -0.08f;
            hat.copyFrom(head);
        }
        // Giai đoạn 3 & 4 (38 -> 75 ticks): Tay phải chỉ định kết giới giam hãm, tay trái thu về ngực
        else {
            float endBlend = Math.clamp((age - 55.0f) / 20.0f, 0.0f, 1.0f);
            float hold = 1.0f - endBlend;

            rightArm.xRot = -1.45f * hold;
            rightArm.yRot = -0.15f * hold;
            rightArm.zRot = 0.15f * hold;

            leftArm.xRot = -0.65f * hold;
            leftArm.yRot = 0.25f * hold;
            leftArm.zRot = 0.45f * hold;

            body.xRot = 0.05f * hold;
            head.xRot = -0.12f * hold;
            hat.copyFrom(head);
        }
    }

    /**
     * Render HUD Overlay: Black Letterbox & Movie Typography
     */
    public static void renderCinematicOverlay(GuiGraphics graphics, float partialTicks) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(casterUuid)) return;

        float age = getAge(partialTicks);
        if (age < 0 || age >= totalDurationTicks) return;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // 1. Dải đen điện ảnh viền trên và viền dưới (Cinema Widescreen Letterbox)
        int barHeight = Math.max(26, screenHeight / 9);
        graphics.fill(0, 0, screenWidth, barHeight, 0xFF000000);
        graphics.fill(0, screenHeight - barHeight, screenWidth, screenHeight, 0xFF000000);

        // 2. Chữ điện ảnh theo từng giai đoạn
        String title = "";
        String subtitle = "";

        if (age < 24.0f) {
            title = "§4§l【 HẮC VIÊM: KHỞI TẠO MA ĐẠO 】";
            subtitle = "§7§oNgưng tụ ngọn lửa hắc ám bất diệt...";
        } else if (age < 38.0f) {
            title = "§c§l【 HẮC VIÊM HỎA CẦU 】";
            subtitle = "§f§oXé toạc bầu trời chiến trường!";
        } else if (age < 55.0f) {
            title = "§4§l【 HẮC VIÊM HỎA CẦU: KHÔNG GIAN GIAM HÃM 】";
            subtitle = "§e§oBành trướng kết giới lập phương đen tuyền 24m...";
        } else {
            title = "§c§l【 HẮC VIÊM BẠO LIỆT: ĐỊA NGỤC LỬA ĐEN 】";
            subtitle = "§4§oThiêu rụi mục tiêu trong biển lửa đen bất diệt!";
        }

        int titleWidth = mc.font.width(title);
        int subtitleWidth = mc.font.width(subtitle);

        graphics.drawString(mc.font, title, (screenWidth - titleWidth) / 2, barHeight - 18, 0xFFFFFFFF, true);
        graphics.drawString(mc.font, subtitle, (screenWidth - subtitleWidth) / 2, screenHeight - barHeight + 6, 0xFFE0E0E0, true);
    }
}
