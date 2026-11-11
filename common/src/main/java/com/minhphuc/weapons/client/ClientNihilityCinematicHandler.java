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
    private static double targetElevY = 0;
    private static double cachedSurfaceY = 0;
    private static int totalDurationTicks = 295;
    private static long startWorldTick = 0;
    private static boolean skipped = false;

    public static boolean isCasterActive(UUID uuid) {
        return isActive() && casterUuid != null && casterUuid.equals(uuid);
    }

    public static boolean isSelfCaster() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && casterUuid != null && casterUuid.equals(mc.player.getUUID());
    }

    public static float getAge() {
        return getAge(0.0f);
    }

    public static float getAge(float partialTicks) {
        if (!isActive()) return -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        return (float) (mc.level.getGameTime() - startWorldTick) + partialTicks;
    }

    public static void handleCinematicPacket(UUID caster, Vec3 center, double elevY, int durationTicks, int phase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getPlayerByUUID(caster);
        if (entity == null) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e != null && caster.equals(e.getUUID())) {
                    entity = e;
                    break;
                }
            }
        }
        int entId = entity != null ? entity.getId() : (mc.player != null ? mc.player.getId() : 0);

        if (phase == 0) { // PHASE_START
            casterEntityId = entId;
            startCinematic(caster, center, elevY, durationTicks);
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

    public static void handleCinematicPacket(UUID caster, Vec3 center, int durationTicks, int phase) {
        handleCinematicPacket(caster, center, center.y + 28.0D, durationTicks, phase);
    }

    public static void startCinematic(UUID caster, Vec3 center, double elevY, int durationTicks) {
        casterUuid = caster;
        centerPos = center;
        targetElevY = elevY;
        totalDurationTicks = durationTicks;
        skipped = false;
        active = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            startWorldTick = mc.level.getGameTime();
            int surfaceY = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) center.x, (int) center.z);
            cachedSurfaceY = (double) surfaceY;
        } else {
            cachedSurfaceY = center.y;
        }
    }

    public static void startCinematic(UUID caster, Vec3 center, int durationTicks) {
        startCinematic(caster, center, center.y + 28.0D, durationTicks);
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

        boolean isSelf = casterUuid != null && casterUuid.equals(mc.player.getUUID());

        float age = getAge(partialTicks);
        if (age < 0 || age >= totalDurationTicks) {
            return null;
        }

        double elevY = targetElevY > centerPos.y ? targetElevY : (centerPos.y + 28.0);
        double currentCasterY;
        if (age <= 40.0f) {
            float elevProgress = age / 40.0f;
            float elevEase = elevProgress * elevProgress * (3.0f - 2.0f * elevProgress);
            currentCasterY = centerPos.y + ((elevY - centerPos.y) * elevEase);
        } else {
            currentCasterY = elevY;
        }

        // KHI ÁC MA KHẾ ƯỚC DÙNG LỜI CHÚC TỬ VONG:
        // Người chơi đứng dưới đất tại chỗ của mình và ngước nhìn Ác Ma bay lên thi triển kỹ năng!
        if (!isSelf) {
            Vec3 demonTargetPos = new Vec3(centerPos.x, currentCasterY + 1.0, centerPos.z);
            Entity demonEnt = mc.level.getEntity(casterEntityId);
            if (demonEnt != null && demonEnt.isAlive()) {
                demonTargetPos = demonEnt.getPosition(partialTicks).add(0, demonEnt.getBbHeight() * 0.65, 0);
            }

            Vec3 camPos = vanillaPos;
            // Thêm rung chấn nhẹ khi Ác Ma kích nổ Lời Chúc Tử Vong
            if (age >= 35.0f && age < 80.0f) {
                float blastAge = age - 35.0f;
                float shakeStrength = (float) (Math.max(0.0, 1.0 - blastAge / 45.0) * 0.65);
                double shakeX = Math.sin(blastAge * 2.7) * 0.25 * shakeStrength;
                double shakeY = Math.sin(blastAge * 3.3) * 0.20 * shakeStrength;
                camPos = vanillaPos.add(shakeX, shakeY, 0);
            }

            Vec3 dirToDemon = demonTargetPos.subtract(camPos);
            double distH = Math.hypot(dirToDemon.x, dirToDemon.z);
            float lookYaw = (float) Math.toDegrees(Math.atan2(-dirToDemon.x, dirToDemon.z));
            float lookPitch = (float) -Math.toDegrees(Math.atan2(dirToDemon.y, distH));

            if (age >= totalDurationTicks - 20.0f) {
                float blend = (age - (totalDurationTicks - 20.0f)) / 20.0f;
                float ease = blend * blend * (3.0f - 2.0f * blend);
                lookYaw = Mth.rotLerp(ease, lookYaw, vanillaYaw);
                lookPitch = Mth.rotLerp(ease, lookPitch, vanillaPitch);
            }

            return new Pose(camPos, lookYaw, lookPitch);
        }

        Entity caster = mc.player;
        float casterYaw = caster.getViewYRot(partialTicks);

        Vec3 smoothCasterPos = new Vec3(centerPos.x, currentCasterY, centerPos.z);
        Vec3 smoothCasterEye = smoothCasterPos.add(0, caster.getEyeHeight(), 0);
        Vec3 smoothCasterCenter = smoothCasterPos.add(0, caster.getBbHeight() * 0.5, 0);

        Vec3 camPos;
        Vec3 lookTarget;

        if (age < 25.0f) {
            // === SHOT 1 (0 -> 25): Cận cảnh xoay quanh từ trước ra sau (Orbit 225° mượt mà nối tiếp Hồi 2) ===
            float progress = age / 25.0f;
            float ease = progress * progress * (3.0f - 2.0f * progress);
            float orbitAngle = (float) Math.toRadians(casterYaw - 45.0f + (ease * 225.0f));
            double dist = 4.5 - (ease * 0.3); // 4.5m -> 4.2m
            double offsetX = -Math.sin(orbitAngle) * dist;
            double offsetZ = Math.cos(orbitAngle) * dist;
            double offsetY = 0.4 + (ease * 1.3);

            camPos = smoothCasterCenter.add(offsetX, offsetY, offsetZ);
            lookTarget = smoothCasterEye.subtract(0, 0.15, 0);
        } else if (age < 50.0f) {
            // === SHOT 2 (25 -> 50): Cinematic Dolly-Out lùi xa bay lên cao bao quát ma pháp trận (Tiếp nối liền mạch góc 180°) ===
            float progress = (age - 25.0f) / 25.0f;
            float ease = progress * progress * (3.0f - 2.0f * progress);
            float backAngle = (float) Math.toRadians(casterYaw + 180.0f + ease * 25.0f);
            double dist = 4.2 + (ease * 17.8); // Lùi êm từ 4.2m ra 22m
            double offsetX = -Math.sin(backAngle) * dist;
            double offsetZ = Math.cos(backAngle) * dist;
            double height = 1.7 + (ease * 13.3); // Nâng từ 1.7m lên 15m

            camPos = smoothCasterPos.add(offsetX, height, offsetZ);
            lookTarget = smoothCasterCenter.add(0, -1.2 * ease, 0); // Nhìn chúc xuống bao quát người chơi và ma pháp trận
        } else if (age < 110.0f) {
            // === SHOT 3 (50 -> 110): Đại Cảnh Vũ Trụ từ Tầng Mây (Bird's Eye / Cosmic View) ===
            float progress = (age - 50.0f) / 60.0f;
            float slowOrbit = (float) Math.toRadians(casterYaw + 205.0f + progress * 35.0f);
            double dist = 22.0 + (progress * 23.0); // Chuyển từ 22m ra 45m liền mạch
            double offsetX = -Math.sin(slowOrbit) * dist;
            double offsetZ = Math.cos(slowOrbit) * dist;

            double baseHeight = cachedSurfaceY > centerPos.y + 12 ? Math.max(centerPos.y + 48.0, cachedSurfaceY + 16.0) : (centerPos.y + 48.0);
            double height = (centerPos.y + 15.0) + (progress * (baseHeight + 8.0 - (centerPos.y + 15.0)));

            camPos = new Vec3(centerPos.x + offsetX, height, centerPos.z + offsetZ);
            lookTarget = centerPos.add(0, 12.0 + progress * 3.0, 0);
        } else if (age < 225.0f) {
            // === SHOT 4 (110 -> 225): Cận Cảnh Kịch Tính Nửa Khuôn Mặt ("LỜI CHÚC TỬ VONG" - 5.75 giây) ===
            float progress = (age - 110.0f) / 115.0f;
            float yawRad = (float) Math.toRadians(casterYaw);
            // Hướng nhìn ngang chuẩn xác, triệt tiêu lỗi NaN khi người chơi nhìn xuống đất
            Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));

            // Zoom cận cảnh điện ảnh an toàn từ 1.60m lại gần 1.18m (đảm bảo không bị đâm xuyên đầu/nón)
            double dist = 1.60 - (0.42 * progress);
            camPos = smoothCasterEye
                    .add(forward.scale(dist))
                    .add(right.scale(0.38 * (1.0 - progress * 0.25)))
                    .add(0, 0.08, 0);
            lookTarget = smoothCasterEye.subtract(0, 0.04, 0);
        } else if (age < 270.0f) {
            // === SHOT 5 (225 -> 270): Đại Xung Kích Từ Xa & Camera Shake Cực Đại ===
            float blastAge = age - 225.0f;
            float backAngle = (float) Math.toRadians(casterYaw + 135.0f);
            double dist = 65.0;
            double offsetX = -Math.sin(backAngle) * dist;
            double offsetZ = Math.cos(backAngle) * dist;

            float shakeStrength = (float) (Math.max(0.0, 1.0 - blastAge / 45.0) * 1.4);
            // Damped harmonic shake (bắt đầu từ 0 để không bị nhảy khung hình)
            double shakeX = Math.sin(blastAge * 2.7) * 0.45 * shakeStrength;
            double shakeY = Math.sin(blastAge * 3.3) * 0.35 * shakeStrength;

            double baseBlastY = cachedSurfaceY > centerPos.y + 12 ? Math.max(centerPos.y + 24.0, cachedSurfaceY + 10.0) : (centerPos.y + 24.0);

            camPos = new Vec3(centerPos.x + offsetX + shakeX, baseBlastY + shakeY, centerPos.z + offsetZ);
            lookTarget = centerPos.add(0, 4.0, 0);
        } else {
            // === GIỮ GÓC NHÌN TRÊN CAO & CHUYỂN TIẾP VỀ NGƯỜI CHƠI TRÊN KHÔNG (270 -> 295) ===
            float blend = (age - 270.0f) / 25.0f;
            float ease = blend * blend * (3.0f - 2.0f * blend);
            float backAngle = (float) Math.toRadians(casterYaw + 135.0f);
            double offsetX = -Math.sin(backAngle) * 65.0;
            double offsetZ = Math.cos(backAngle) * 65.0;
            double baseBlastY = cachedSurfaceY > centerPos.y + 12 ? Math.max(centerPos.y + 24.0, cachedSurfaceY + 10.0) : (centerPos.y + 24.0);
            Vec3 shot5Pos = new Vec3(centerPos.x + offsetX, baseBlastY, centerPos.z + offsetZ);

            // Chuyển tiếp mượt mà từ góc nhìn từ xa về góc nhìn người chơi trên không
            camPos = shot5Pos.lerp(vanillaPos, ease);
            lookTarget = centerPos.add(0, 2.0, 0).lerp(vanillaPos.add(caster.getViewVector(partialTicks).scale(10.0)), ease);
        }

        Vec3 dir = lookTarget.subtract(camPos);
        double distHoriz = Math.hypot(dir.x, dir.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, distHoriz));

        if (age >= 270.0f) {
            float blend = (age - 270.0f) / 25.0f;
            float ease = blend * blend * (3.0f - 2.0f * blend);
            targetYaw = Mth.rotLerp(ease, targetYaw, vanillaYaw);
            // Chuyển tiếp 100% êm ái về vanillaPitch, triệt tiêu hoàn toàn cú giật góc quay 28° khi kết thúc
            targetPitch = Mth.rotLerp(ease, targetPitch, vanillaPitch);
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
        else if (age > 270.0f) envelope = Math.max(0.0f, (295.0f - age) / 25.0f);

        int bars = Math.round(height * 0.11f * envelope);
        if (bars > 0) {
            graphics.fill(0, 0, width, bars, 0xFF030109);
            graphics.fill(0, height - bars, width, height, 0xFF030109);
        }

        // 2. Blackout Vignette trong khoảnh khắc tĩnh lặng trước khi nổ (110 -> 225)
        if (age >= 110.0f && age < 225.0f) {
            float darkAlpha = (float) Math.sin(((age - 110.0f) / 115.0f) * Math.PI) * 0.78f;
            int a = Math.clamp(Math.round(darkAlpha * 255.0f), 0, 255);
            graphics.fill(0, bars, width, height - bars, (a << 24) | 0x05010A);
        }

        // 3. Typography Tiếng Việt: "LỜI CHÚC TỬ VONG" (Kéo dài ~5.75 giây)
        if (age >= 112.0f && age < 160.0f) {
            // Nhịp 1 (48 ticks / ~2.4s): "LỜI CHÚC..."
            float alpha = Math.min(1.0f, (age - 112.0f) / 8.0f);
            renderTitle(graphics, "LỜI CHÚC...", width * 0.5f, height * 0.82f, Math.min(2.0f, width / 180f), alpha, false);
        } else if (age >= 160.0f && age < 200.0f) {
            // Nhịp 2 (40 ticks / ~2.0s): "LỜI CHÚC TỬ VONG..."
            float alpha = Math.min(1.0f, (age - 160.0f) / 6.0f);
            renderTitle(graphics, "LỜI CHÚC TỬ VONG...", width * 0.5f, height * 0.82f, Math.min(2.0f, width / 180f), alpha, false);
        } else if (age >= 200.0f && age < 225.0f) {
            // Nhịp 3 (25 ticks / ~1.25s): Phóng to cực đại chữ "TỬ VONG" kèm Heartbeat pulse
            float alpha = Math.min(1.0f, (age - 200.0f) / 4.0f);
            float pulse = (float) (1.0 + 0.08 * Math.sin((age - 200.0) * 0.75));
            float scale = Math.min(width * 0.52f / mc.font.width("TỬ VONG"), 5.5f) * pulse;
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
