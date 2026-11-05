package com.minhphuc.weapons.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Xử lý chuỗi Camera Điện Ảnh 5 Góc Quay cho Tuyệt Kỹ: Hư Vô Băng Hoại (Nihility Collapse)
 * - Shot 1 (0 -> 25 ticks / 1.25s): Orbit lướt vòng cung tốc độ cao quanh người chơi từ góc dưới lên.
 * - Shot 2 (25 -> 50 ticks / 1.25s): Đặt sát mặt đất nhìn chéo lên, thấy vùng tối và kết giới bao trùm bầu trời.
 * - Shot 3 (50 -> 110 ticks / 3.0s): Bird's Eye View từ trên tầng mây (Cosmic View) nhìn xuống mặt đất chìm trong bóng tối và mob bị hút lơ lửng.
 * - Shot 4 (110 -> 140 ticks / 1.5s): Zoom kịch tính cực cận cảnh vào nửa khuôn mặt / vũ khí của người chơi ("I AM ATOMIC").
 * - Shot 5 (140 -> 180 ticks / 2.0s): Extreme Wide Impact Shot từ xa với camera shake rung lắc cực mạnh khi cột sáng dội xuống.
 * - Trả góc nhìn mượt mà (180 -> 200 ticks / 1.0s) về góc nhìn thứ nhất bình thường của người chơi.
 * - Hoàn toàn xử lý phía Client, ZERO-LAG Server!
 */
public class ClientNihilityCinematicHandler {

    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static boolean active = false;
    private static UUID casterUuid = null;
    private static Vec3 centerPos = Vec3.ZERO;
    private static int totalDurationTicks = 200;
    private static long startWorldTick = 0;
    private static boolean skipped = false;

    public static void startCinematic(UUID caster, Vec3 center, int durationTicks) {
        casterUuid = caster;
        centerPos = center;
        totalDurationTicks = durationTicks;
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

    public static boolean isActive() {
        return active && !skipped;
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

        long currentTick = mc.level.getGameTime();
        float age = (float) (currentTick - startWorldTick) + partialTicks;

        if (age < 0 || age >= totalDurationTicks) {
            return null;
        }

        Entity caster = null;
        if (casterUuid != null) {
            caster = mc.level.getPlayerByUUID(casterUuid);
        }
        if (caster == null) {
            caster = mc.player;
        }

        Vec3 casterEye = caster.getEyePosition(partialTicks);
        Vec3 casterCenter = caster.getPosition(partialTicks).add(0, caster.getBbHeight() * 0.5, 0);
        float casterYaw = caster.getViewYRot(partialTicks);

        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 25.0f) {
            // === SHOT 1 (0 -> 25): Cận cảnh xoay quanh (Orbit) ===
            float progress = age / 25.0f;
            float orbitAngle = (float) Math.toRadians(casterYaw + age * 10.0f);
            double dist = 4.0;
            double offsetX = -Math.sin(orbitAngle) * dist;
            double offsetZ = Math.cos(orbitAngle) * dist;
            double offsetY = 0.5 + progress * 1.2;

            camPos = casterCenter.add(offsetX, offsetY, offsetZ);
            lookTarget = casterEye;
        } else if (age < 50.0f) {
            // === SHOT 2 (25 -> 50): Góc thấp sát đất nhìn lên trời ===
            float progress = (age - 25.0f) / 25.0f;
            float backAngle = (float) Math.toRadians(casterYaw + 180.0f);
            double dist = 4.2;
            double offsetX = -Math.sin(backAngle) * dist;
            double offsetZ = Math.cos(backAngle) * dist;

            camPos = caster.getPosition(partialTicks).add(offsetX, 0.4 + progress * 0.3, offsetZ);
            lookTarget = casterCenter.add(0, 8.0 + progress * 12.0, 0); // Nhìn hất lên bầu trời tối
        } else if (age < 110.0f) {
            // === SHOT 3 (50 -> 110): Đại Cảnh Vũ Trụ từ Tầng Mây (Bird's Eye / Cosmic View) ===
            float progress = (age - 50.0f) / 60.0f;
            float slowOrbit = (float) Math.toRadians(casterYaw + progress * 35.0f);
            double dist = 45.0;
            double offsetX = -Math.sin(slowOrbit) * dist;
            double offsetZ = Math.cos(slowOrbit) * dist;
            double height = centerPos.y + 48.0 + progress * 8.0;

            camPos = new Vec3(centerPos.x + offsetX, height, centerPos.z + offsetZ);
            lookTarget = centerPos.add(0, 15.0, 0); // Nhìn vào tâm vết nứt trên trời và mặt đất
        } else if (age < 140.0f) {
            // === SHOT 4 (110 -> 140): Cận Cảnh Kịch Tính Nửa Khuôn Mặt ("I AM ATOMIC") ===
            Vec3 forward = caster.getLookAngle();
            Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

            // Đặt góc máy cách khuôn mặt 0.9 block chếch nhẹ về bên phải
            camPos = casterEye.add(forward.scale(0.85)).add(right.scale(0.28)).add(0, 0.05, 0);
            lookTarget = casterEye;
        } else if (age < 180.0f) {
            // === SHOT 5 (140 -> 180): Đại Xung Kích Từ Xa & Camera Shake ===
            float blastAge = age - 140.0f;
            float backAngle = (float) Math.toRadians(casterYaw + 135.0f);
            double dist = 60.0;
            double offsetX = -Math.sin(backAngle) * dist;
            double offsetZ = Math.cos(backAngle) * dist;

            // Rung lắc màn hình (Shake) giảm dần
            float shakeStrength = (float) (Math.max(0.0, 1.0 - blastAge / 40.0) * 1.6);
            double shakeX = Math.sin(blastAge * 2.7) * 0.4 * shakeStrength;
            double shakeY = Math.cos(blastAge * 3.3) * 0.4 * shakeStrength;

            camPos = new Vec3(centerPos.x + offsetX + shakeX, centerPos.y + 22.0 + shakeY, centerPos.z + offsetZ);
            lookTarget = centerPos.add(0, 4.0, 0);
        } else {
            // === TRẢ GÓC NHÌN MƯỢT MÀ (180 -> 200) ===
            float blend = (age - 180.0f) / 20.0f;
            float backAngle = (float) Math.toRadians(casterYaw + 135.0f);
            double offsetX = -Math.sin(backAngle) * 60.0;
            double offsetZ = Math.cos(backAngle) * 60.0;
            Vec3 shot5Pos = new Vec3(centerPos.x + offsetX, centerPos.y + 22.0, centerPos.z + offsetZ);

            camPos = shot5Pos.lerp(vanillaPos, blend);
            lookTarget = centerPos;
        }

        // Tính góc xoay Yaw & Pitch hướng về lookTarget
        Vec3 dir = lookTarget.subtract(camPos);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));

        if (age >= 180.0f) {
            float blend = (age - 180.0f) / 20.0f;
            targetYaw = Mth.rotLerp(blend, targetYaw, vanillaYaw);
            targetPitch = Mth.lerp(blend, targetPitch, vanillaPitch);
        }

        return new Pose(camPos, targetYaw, targetPitch);
    }
}
