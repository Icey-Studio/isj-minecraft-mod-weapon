package com.minhphuc.weapons.client;

import com.minhphuc.weapons.content.darkgathering.SamuraiSpiritDisplayHelper;
import com.minhphuc.weapons.network.ClientboundIaiStatePacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundIaiClickPacket;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Xử lý trạng thái Client cho Bạt Đao Thuật Vận Tốc Ngược:
 * - Kích hoạt/tắt Manga Shader trắng đen
 * - Quản lý trạng thái xuất khiếu linh hồn bán trong suốt (Astral Translucency)
 * - Tiếp nhận phím bấm chuột (Attack / Use) gửi lên Server với debounce chuẩn xác
 */
public class ClientIaiHandler {
    private static boolean astralActive = false;
    private static long lastClickTime = 0;

    public static boolean isAstralActive() {
        return astralActive;
    }

    public static boolean isAstralPlayer(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        return astralActive && mc.player != null && mc.player == entity;
    }

    public static void handlePacket(int state, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        switch (state) {
            case ClientboundIaiStatePacket.STATE_START -> {
                astralActive = true;
                ClientTengaiMangaHandler.triggerMangaEffect(durationTicks);
            }
            case ClientboundIaiStatePacket.STATE_FINISHER_STARTED -> {
                // Tắt shader ngay lập tức để màu sắc bừng sáng trở lại
                ClientTengaiMangaHandler.stopMangaEffect();
            }
            case ClientboundIaiStatePacket.STATE_END -> {
                astralActive = false;
                ClientTengaiMangaHandler.stopMangaEffect();
            }
        }
    }

    public static void triggerClickFromInput() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !astralActive) return;

        long now = System.currentTimeMillis();
        // Debounce 350ms ở client để chống gửi nhiều gói tin cùng lúc
        if (now - lastClickTime < 350) return;
        lastClickTime = now;

        Vec3 clickPos = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(3.5D));
        int targetId = -1;

        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult entHit) {
            Entity targetEntity = entHit.getEntity();
            targetId = targetEntity.getId();
            clickPos = targetEntity.position().add(0, 0.5D, 0);
        } else if (hit instanceof BlockHitResult blkHit) {
            clickPos = blkHit.getLocation();
        }

        ModMessages.sendToServer(new ServerboundIaiClickPacket(clickPos, targetId));
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.player == null || mc.level == null) {
                if (astralActive) {
                    astralActive = false;
                    ClientTengaiMangaHandler.stopMangaEffect();
                }
                return;
            }

            if (astralActive) {
                // Sinh các hạt lửa ma quái và bụi trắng linh hồn bao quanh người chơi
                double rx = (mc.player.getRandom().nextDouble() - 0.5D) * 0.8D;
                double ry = mc.player.getRandom().nextDouble() * 1.8D;
                double rz = (mc.player.getRandom().nextDouble() - 0.5D) * 0.8D;

                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        mc.player.getX() + rx, mc.player.getY() + ry, mc.player.getZ() + rz,
                        0.0D, 0.02D, 0.0D);
                mc.level.addParticle(SamuraiSpiritDisplayHelper.PURE_WHITE_DUST,
                        mc.player.getX() + rx, mc.player.getY() + ry, mc.player.getZ() + rz,
                        0.0D, 0.01D, 0.0D);

                // Lắng nghe phím Attack (Chuột trái) hoặc Use (Chuột phải)
                if (mc.options.keyAttack.consumeClick() || mc.options.keyUse.consumeClick()) {
                    triggerClickFromInput();
                }
            }
        });
    }
}
