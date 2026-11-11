package com.minhphuc.weapons.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * CLIENT AZATHOTH CINEMATIC HANDLER (THE VOID DRAGON CAMERA)
 * Quản lý góc quay điện ảnh lệch vai (Over-the-Shoulder) & Recoil Shake
 * cho tuyệt kỹ Thần Hư Không Azathoth.
 */
public class ClientAzathothCinematicHandler {

    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static boolean active = false;
    private static UUID casterUuid = null;
    private static Vec3 startEyePos = Vec3.ZERO;
    private static Vec3 fireDir = Vec3.ZERO;
    private static int totalDurationTicks = 35;
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

    public static void startCinematic(UUID caster, Vec3 startPos, Vec3 dir, int durationTicks) {
        casterUuid = caster;
        startEyePos = startPos;
        fireDir = dir.normalize();
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
        Vec3 lookVec = caster.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        if (rightVec.lengthSqr() < 0.01) {
            rightVec = new Vec3(1, 0, 0);
        }
        Vec3 upVec = rightVec.cross(lookVec).normalize();

        Vec3 camPos;
        Vec3 lookTarget;

        // Giai đoạn 1: Phóng Rồng Plasma (0 -> 16 ticks) - Camera lệch vai + Recoil Shake
        if (age < 16.0f) {
            float progress = age / 16.0f;

            // Recoil shake giật lùi theo hướng bắn
            float recoilStrength = (float) Math.max(0.0, 1.0 - (age / 8.0f));
            double shakeX = Math.sin(age * 3.8) * 0.15 * recoilStrength;
            double shakeY = Math.cos(age * 4.2) * 0.15 * recoilStrength;
            double kickback = -0.75 * recoilStrength;

            // Vị trí lệch vai: lùi 3.2m, lệch phải 1.2m, nâng 0.45m
            camPos = casterEye
                    .add(lookVec.scale(-3.2 + kickback))
                    .add(rightVec.scale(1.2 + shakeX))
                    .add(upVec.scale(0.45 + shakeY));

            // Camera nhìn bám theo hướng đầu rồng plasma phóng đi (cách 30m)
            lookTarget = casterEye.add(lookVec.scale(30.0D));
        }
        // Giai đoạn 2: Thu Hồi Rồng Plasma (16 -> 35 ticks) - Chuyển tiếp mượt về người chơi
        else {
            float returnProgress = (age - 16.0f) / 19.0f;
            float ease = returnProgress * returnProgress * (3.0f - 2.0f * returnProgress);

            Vec3 shotPos = casterEye
                    .add(lookVec.scale(-3.2))
                    .add(rightVec.scale(1.2))
                    .add(upVec.scale(0.45));

            camPos = shotPos.lerp(vanillaPos, ease);
            lookTarget = casterEye.add(lookVec.scale(30.0D * (1.0 - ease * 0.5)));
        }

        Vec3 dir = lookTarget.subtract(camPos);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));

        if (age >= 16.0f) {
            float returnProgress = (age - 16.0f) / 19.0f;
            float ease = returnProgress * returnProgress * (3.0f - 2.0f * returnProgress);
            targetYaw = Mth.rotLerp(ease, targetYaw, vanillaYaw);
            targetPitch = Mth.lerp(ease, targetPitch, vanillaPitch);
        }

        return new Pose(camPos, targetYaw, targetPitch);
    }
}
