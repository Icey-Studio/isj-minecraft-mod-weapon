package com.minhphuc.weapons.client;

import com.minhphuc.weapons.client.renderer.AtomicRenderer;
import com.minhphuc.weapons.client.renderer.AtomicSequence;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Xử lý chuỗi Camera Điện Ảnh 5 Góc Quay & Cinematic Overlay (Letterbox + Typography)
 * kết hợp Engine Đồ Họa Đỉnh Cao OpenGL (AtomicRenderer & AtomicPostFx) từ God Wand.
 */
public class ClientNihilityCinematicHandler {

    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static boolean active = false;
    private static UUID casterUuid = null;
    private static int casterEntityId = 0;
    private static Vec3 centerPos = Vec3.ZERO;
    private static int totalDurationTicks = 200;
    private static long startWorldTick = 0;
    private static boolean skipped = false;

    public static void handleCinematicPacket(UUID caster, Vec3 center, int durationTicks, int phase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getPlayerByUUID(caster);
        int entId = entity != null ? entity.getId() : (mc.player != null ? mc.player.getId() : 0);

        if (phase == 0) { // PHASE_START
            casterEntityId = entId;
            startCinematic(caster, center, durationTicks);
            AtomicRenderer.receive(new AtomicRenderer.FxData(
                    entId,
                    mc.level.getGameTime(),
                    center.x, center.y, center.z,
                    AtomicRenderer.PHASE_CHARGE,
                    -1
            ));
        } else if (phase == 1) { // PHASE_DETONATE
            long now = mc.level.getGameTime();
            AtomicRenderer.receive(new AtomicRenderer.FxData(
                    entId,
                    now - AtomicSequence.DETONATION,
                    center.x, center.y, center.z,
                    AtomicRenderer.PHASE_BURST,
                    now
            ));
            CustomImpactFrameOverlay.trigger();
        } else if (phase == 2) { // PHASE_END
            stopCinematic();
            AtomicRenderer.reset();
        }
    }

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
        AtomicRenderer.reset();
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

            AtomicRenderer.tick(mc);

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
            double dist = 4.2;
            double offsetX = -Math.sin(orbitAngle) * dist;
            double offsetZ = Math.cos(orbitAngle) * dist;
            double offsetY = 0.5 + progress * 1.2;

            camPos = casterCenter.add(offsetX, offsetY, offsetZ);
            lookTarget = casterEye;
        } else if (age < 50.0f) {
            // === SHOT 2 (25 -> 50): Cinematic Dolly-Out lùi xa bay lên cao bao quát ma pháp trận ===
            float progress = (age - 25.0f) / 25.0f;
            float ease = progress * progress * (3.0f - 2.0f * progress);
            float backAngle = (float) Math.toRadians(casterYaw + 180.0f + ease * 30.0f);
            double dist = 5.0 + ease * 16.0; // Lùi từ 5m ra 21m
            double offsetX = -Math.sin(backAngle) * dist;
            double offsetZ = Math.cos(backAngle) * dist;
            double height = 2.0 + ease * 12.0; // Nâng từ 2m lên 14m

            camPos = caster.getPosition(partialTicks).add(offsetX, height, offsetZ);
            lookTarget = casterCenter.add(0, -1.0 * ease, 0); // Nhìn chúc xuống bao quát người chơi và ma pháp trận
        } else if (age < 110.0f) {
            // === SHOT 3 (50 -> 110): Đại Cảnh Vũ Trụ từ Tầng Mây (Bird's Eye / Cosmic View) ===
            float progress = (age - 50.0f) / 60.0f;
            float slowOrbit = (float) Math.toRadians(casterYaw + progress * 35.0f);
            double dist = 45.0;
            double offsetX = -Math.sin(slowOrbit) * dist;
            double offsetZ = Math.cos(slowOrbit) * dist;
            double height = centerPos.y + 48.0 + progress * 8.0;

            camPos = new Vec3(centerPos.x + offsetX, height, centerPos.z + offsetZ);
            lookTarget = centerPos.add(0, 15.0, 0);
        } else if (age < 141.0f) {
            // === SHOT 4 (110 -> 141): Cận Cảnh Kịch Tính Nửa Khuôn Mặt ("LỜI CHÚC TỬ VONG") ===
            Vec3 forward = caster.getLookAngle();
            Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

            camPos = casterEye.add(forward.scale(0.85)).add(right.scale(0.28)).add(0, 0.05, 0);
            lookTarget = casterEye;
        } else if (age < 180.0f) {
            // === SHOT 5 (141 -> 180): Đại Xung Kích Từ Xa & Camera Shake Cực Đại ===
            float blastAge = age - 141.0f;
            float backAngle = (float) Math.toRadians(casterYaw + 135.0f);
            double dist = 65.0;
            double offsetX = -Math.sin(backAngle) * dist;
            double offsetZ = Math.cos(backAngle) * dist;

            float shakeStrength = (float) (Math.max(0.0, 1.0 - blastAge / 39.0) * 1.8);
            double shakeX = Math.sin(blastAge * 2.7) * 0.5 * shakeStrength;
            double shakeY = Math.cos(blastAge * 3.3) * 0.5 * shakeStrength;

            camPos = new Vec3(centerPos.x + offsetX + shakeX, centerPos.y + 24.0 + shakeY, centerPos.z + offsetZ);
            lookTarget = centerPos.add(0, 4.0, 0);
        } else {
            // === GIỮ GÓC NHÌN TRÊN CAO & CHUYỂN TIẾP VỀ NGƯỜI CHƠI TRÊN KHÔNG (180 -> 200) ===
            float blend = (age - 180.0f) / 20.0f;
            float ease = blend * blend * (3.0f - 2.0f * blend);
            float backAngle = (float) Math.toRadians(casterYaw + 135.0f);
            double offsetX = -Math.sin(backAngle) * 65.0;
            double offsetZ = Math.cos(backAngle) * 65.0;
            Vec3 shot5Pos = new Vec3(centerPos.x + offsetX, centerPos.y + 24.0, centerPos.z + offsetZ);

            // Chuyển tiếp mượt mà từ góc nhìn từ xa về vị trí người chơi đang lơ lửng trên không
            camPos = shot5Pos.lerp(vanillaPos, ease);
            lookTarget = centerPos.add(0, 1.0, 0); // Nhìn xuống tâm chiến trường
        }

        Vec3 dir = lookTarget.subtract(camPos);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));

        if (age >= 180.0f) {
            float blend = (age - 180.0f) / 20.0f;
            float ease = blend * blend * (3.0f - 2.0f * blend);
            targetYaw = Mth.rotLerp(ease, targetYaw, vanillaYaw);
            // Giữ góc nhìn chúc nhẹ xuống mặt đất (28° - 35°) để ngắm toàn cảnh khu vực bị phá hủy
            float aerialPitch = Math.max(vanillaPitch, 28.0f);
            targetPitch = Mth.lerp(ease, targetPitch, aerialPitch);
        }

        return new Pose(camPos, targetYaw, targetPitch);
    }

    public static void renderCinematicOverlay(GuiGraphics graphics, float delta) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long currentTick = mc.level.getGameTime();
        float age = (float) (currentTick - startWorldTick) + delta;
        int width = graphics.guiWidth(), height = graphics.guiHeight();

        // 1. Black Bars Letterbox (Cinematic 21:9 Widescreen)
        float envelope = 1.0f;
        if (age < 20.0f) envelope = age / 20.0f;
        else if (age > 180.0f) envelope = Math.max(0.0f, (200.0f - age) / 20.0f);

        int bars = Math.round(height * 0.11f * envelope);
        if (bars > 0) {
            graphics.fill(0, 0, width, bars, 0xFF030109);
            graphics.fill(0, height - bars, width, height, 0xFF030109);
        }

        // 2. Blackout Vignette trong khoảnh khắc tĩnh lặng trước khi nổ (110 -> 141)
        if (age >= 110.0f && age < 141.0f) {
            float darkAlpha = (float) Math.sin(((age - 110.0f) / 31.0f) * Math.PI) * 0.75f;
            int a = Math.clamp(Math.round(darkAlpha * 255.0f), 0, 255);
            graphics.fill(0, bars, width, height - bars, (a << 24) | 0x05010A);
        }

        // 3. Typography Tiếng Việt: "LỜI CHÚC TỬ VONG"
        if (age >= 112.0f && age < 130.0f) {
            float alpha = Math.min(1.0f, (age - 112.0f) / 6.0f);
            String titleText = age < 120.0f ? "LỜI CHÚC..." : "LỜI CHÚC TỬ VONG...";
            renderTitle(graphics, titleText, width * 0.5f, height * 0.82f, Math.min(2.0f, width / 180f), alpha, false);
        } else if (age >= 130.0f && age < 141.0f) {
            float alpha = Math.min(1.0f, (age - 130.0f) / 4.0f);
            float scale = Math.min(width * 0.52f / mc.font.width("TỬ VONG"), 5.5f);
            renderTitle(graphics, "TỬ VONG", width * 0.5f, height * 0.40f, scale, alpha, true);
        }

        // Hint skip ở góc dưới
        if (age < 80.0f) {
            Component skipMsg = Component.literal("§7[ESC / F5] Bỏ qua Cinematic");
            graphics.drawString(mc.font, skipMsg, width - mc.font.width(skipMsg) - 10, height - 16, 0xFFB6A7C9, false);
        }
    }

    private static void renderTitle(GuiGraphics graphics, String text, float x, float y, float scale, float alpha, boolean impact) {
        if (alpha < 0.01f) return;
        var font = Minecraft.getInstance().font;
        Component letters = Component.literal(text);
        int a = Math.clamp(Math.round(alpha * 255.0f), 0, 255);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        float left = -font.width(letters) / 2.0f;
        graphics.pose().translate(left, -4, 0);

        if (impact) {
            graphics.drawString(font, letters, -1, 0, (Math.max(4, a / 3) << 24) | 0x9D0022, false);
            graphics.drawString(font, letters, 1, 0, (Math.max(4, a / 3) << 24) | 0xDA1155, false);
        }
        graphics.drawString(font, letters, 0, 0, (a << 24) | 0xFFF0F5, false);
        graphics.pose().popPose();
    }
}
