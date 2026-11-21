package com.minhphuc.weapons.client;

import com.minhphuc.weapons.client.renderer.ClientOrbitalStrikeRenderer;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * CLIENT ORBITAL CINEMATIC HANDLER (THE SHOOTING STAR CUTSCENE)
 * Quản lý chuyển cảnh camera ngoài không gian vũ trụ:
 * - Hồi 1 (0 -> 24t): Camera phóng ra ngoài quỹ đạo (Độ cao +420m), nhìn xuống Trái Đất từ tầng bình lưu
 * - Hồi 2 (20 -> 28t): Vệ tinh khai hỏa chùm năng lượng / thần thương
 * - Hồi 3 (28 -> 45t): Camera lao dốc siêu thanh (Hypersonic Dive) xuyên qua các tầng mây bám theo vũ khí
 * - Hồi 4 (45 -> 58t): Camera hạ thấp bao quát vụ nổ mặt đất và rung chấn
 * - Hồi 5 (58 -> 70t): Chuyển tiếp mượt mà (smoothstep) trả góc nhìn lại cho người chơi
 */
public class ClientOrbitalCinematicHandler {

    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static boolean active = false;
    private static Vec3 targetPos = Vec3.ZERO;
    private static int strikeMode = 0;
    private static int totalDurationTicks = 70;
    private static long startWorldTick = 0;
    private static boolean skipped = false;
    private static boolean impactTriggered = false;

    public static boolean isActive() {
        return active && !skipped;
    }

    public static Vec3 getTargetPos() {
        return targetPos;
    }

    public static int getStrikeMode() {
        return strikeMode;
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

    public static void startCinematic(Vec3 target, int mode, int durationTicks) {
        targetPos = target;
        strikeMode = mode;
        totalDurationTicks = Math.max(65, durationTicks);
        skipped = false;
        impactTriggered = false;
        active = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            startWorldTick = mc.level.getGameTime();
        }
    }

    public static void stopCinematic() {
        active = false;
        skipped = false;
    }

    public static void skip() {
        skipped = true;
        stopCinematic();
    }

    public static Pose getCameraPose(float partialTicks, Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        if (!isActive()) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        float age = (float) (mc.level.getGameTime() - startWorldTick) + partialTicks;
        if (age < 0 || age >= totalDurationTicks) {
            return null;
        }

        // Bỏ góc quay phóng ra ngoài không gian vũ trụ - Giữ nguyên góc nhìn người chơi (First/Third person)
        // Chỉ tạo rung chấn nhẹ khi vũ khí va chạm mặt đất (Ticks 45 -> 58)
        if (age >= 45.0f && age < 58.0f) {
            if (!impactTriggered) {
                impactTriggered = true;
                CustomImpactFrameOverlay.trigger();
            }
            float blastAge = age - 45.0f;
            float shakeStrength = (float) Math.max(0.0, 1.0 - blastAge / 13.0);
            double shakeX = Math.sin(blastAge * 4.2) * 0.15 * shakeStrength;
            double shakeY = Math.cos(blastAge * 3.7) * 0.18 * shakeStrength;
            double shakeZ = Math.sin(blastAge * 3.1) * 0.15 * shakeStrength;
            return new Pose(vanillaPos.add(shakeX, shakeY, shakeZ), vanillaYaw, vanillaPitch);
        }

        return null;
    }

    /**
     * RENDER HUD QUÂN SỰ VỆ TINH QUỸ ĐẠO (MILITARY SATELLITE HUD OVERLAY)
     */
    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float age = (float) (mc.level.getGameTime() - startWorldTick) + partialTick;
        if (age < 0 || age >= totalDurationTicks) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 1. Viền đen điện ảnh Widescreen 21:9 Letterbox (trên và dưới)
        int barHeight = Math.max(36, height / 8);
        graphics.fill(0, 0, width, barHeight, 0xE0000000);
        graphics.fill(0, height - barHeight, width, height, 0xE0000000);

        // 2. Đường viền khung mục tiêu công nghệ cao ở 4 góc màn hình
        int bracketSize = 24;
        int margin = 18;
        int bracketColor = 0x8000FFCC; // Cyan công nghệ

        // Góc trên - trái: ┌
        graphics.fill(margin, barHeight + margin, margin + bracketSize, barHeight + margin + 2, bracketColor);
        graphics.fill(margin, barHeight + margin, margin + 2, barHeight + margin + bracketSize, bracketColor);

        // Góc trên - phải: ┐
        graphics.fill(width - margin - bracketSize, barHeight + margin, width - margin, barHeight + margin + 2, bracketColor);
        graphics.fill(width - margin - 2, barHeight + margin, width - margin, barHeight + margin + bracketSize, bracketColor);

        // Góc dưới - trái: └
        graphics.fill(margin, height - barHeight - margin - 2, margin + bracketSize, height - barHeight - margin, bracketColor);
        graphics.fill(margin, height - barHeight - margin - bracketSize, margin + 2, height - barHeight - margin, bracketColor);

        // Góc dưới - phải: ┘
        graphics.fill(width - margin - bracketSize, height - barHeight - margin - 2, width - margin, height - barHeight - margin, bracketColor);
        graphics.fill(width - margin - 2, height - barHeight - margin - bracketSize, width - margin, height - barHeight - margin, bracketColor);

        // 3. Thông tin Telemetry Vệ Tinh (Góc trên trái)
        graphics.drawString(mc.font, "§b§l[KH-12 ORBITAL RECON SATELLITE]", margin + 4, barHeight + margin + 6, 0x00FFFF, false);
        graphics.drawString(mc.font, "§7STATUS: §aONLINE // TELEMETRY LINK 100%", margin + 4, barHeight + margin + 18, 0xAAAAAA, false);
        graphics.drawString(mc.font, String.format("§7ALTITUDE: §f%.0f KM (EXOSPHERE)", Math.max(12.0, 420.0 - (age > 28 ? (age - 28) * 22 : 0))), margin + 4, barHeight + margin + 28, 0xAAAAAA, false);

        // 4. Thông tin Hệ Thống Vũ Khí (Góc trên phải)
        String weaponName = switch (strikeMode) {
            case ClientOrbitalStrikeRenderer.MODE_RAILGUN -> "§b§lSS-01 · RAILGUN PLASMA";
            case ClientOrbitalStrikeRenderer.MODE_GUNGNIR -> "§c§lSS-03 · GUNGNIR SPEAR";
            case ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS -> "§5§lSS-04 · THE SEVEN STARS";
            default -> "§eORBITAL STRIKE";
        };
        String lockStatus = (age < 20.0f) ? "§e[LOCKING TARGET...]" :
                (age < 35.0f) ? "§c§l[DISCHARGING PAYLOAD]" : "§a§l[IMPACT CONFIRMED]";

        int rightX = width - margin - 200;
        graphics.drawString(mc.font, "§7SYSTEM: " + weaponName, rightX, barHeight + margin + 6, 0xFFFFFF, false);
        graphics.drawString(mc.font, String.format("§7TARGET: §f[%.0f, %.0f, %.0f]", targetPos.x, targetPos.y, targetPos.z), rightX, barHeight + margin + 18, 0xAAAAAA, false);
        graphics.drawString(mc.font, "§7STATUS: " + lockStatus, rightX, barHeight + margin + 28, 0xFFFFFF, false);

        // 5. Hồng tâm Radar trung tâm (Center Crosshair Reticle)
        int centerX = width / 2;
        int centerY = height / 2;
        int reticleColor = (age >= 20 && age <= 35) ? 0xFFFF0055 : 0x8000FFCC;

        // Vẽ 4 vạch ngắm
        graphics.fill(centerX - 18, centerY, centerX - 6, centerY + 1, reticleColor);
        graphics.fill(centerX + 6, centerY, centerX + 18, centerY + 1, reticleColor);
        graphics.fill(centerX, centerY - 18, centerX + 1, centerY - 6, reticleColor);
        graphics.fill(centerX, centerY + 6, centerX + 1, centerY + 18, reticleColor);

        // 6. Dòng chữ hướng dẫn ở đáy thanh Letterbox
        Component skipPrompt = Component.literal("§8Nhấn §7[ESC]§8 hoặc §7[F5]§8 để bỏ qua Cutscene vũ trụ");
        int textWidth = mc.font.width(skipPrompt);
        graphics.drawString(mc.font, skipPrompt, (width - textWidth) / 2, height - barHeight + (barHeight - 8) / 2, 0x888888, false);
    }
}
