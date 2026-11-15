package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Kỹ năng Triệu Hồi Bạo Phong Long Veldora (Dạng Rồng hoặc Dạng Người).
 * - Khi triệu hồi có 30% xác suất Veldora bận không tới -> Đại Hiền Giả / Raphael thông báo ở góc màn hình.
 * - 70% xác suất Veldora xuất hiện kèm Ma Pháp Trận, Lốc Xoáy & Sấm Sét!
 */
public final class SummonVeldoraAbility {

    private SummonVeldoraAbility() {}

    public static void summon(ServerLevel level, ServerPlayer player, boolean humanForm) {
        boolean unlocked = player.isCreative()
                || EntityDataHelper.getCustomData(player).getBoolean(AzathothStomachData.KEY_VELDORA_BEFRIENDED);
        if (!unlocked) {
            SageManager.sendCornerAlert(player, "Chưa thể triệu hồi! Hãy tìm Hang Động Phong Ấn & dùng Azathoth / Raphael giải phong ấn Veldora trước!", 0xFFF59E0B, 6000, true);
            return;
        }

        // 30% Veldora bận không tới
        if (level.random.nextFloat() < 0.30F) {
            String excuse = switch (level.random.nextInt(3)) {
                case 0 -> "Kuahaha... Chờ chút đi bạn hiền, ta đang đọc dở tập Manga!";
                case 1 -> "Xin lỗi nha, Ifrit vừa thách đấu cờ Shogi với ta trong dạ dày!";
                default -> "Bạo Phong Long hiện đang bận tập chiêu mới, lát nữa gọi lại nhé!";
            };
            SageManager.sendCornerAlert(player, "Triệu Hồi Veldora Thất Bại (30% Đang Bận): \"" + excuse + "\"", 0xFFFB7185, 6500, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        // 70% Veldora xuất hiện với Ma Pháp Trận + Bão Lốc Xoáy!
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(4.5D));
        SpatialTeleportAbility.spawnMagicCircleParticles(level, spawnPos, 4.5D, 75);
        for (int h = 0; h < 18; h++) {
            double ang = h * 0.65D;
            double r = 1.2D + h * 0.28D;
            level.sendParticles(ParticleTypes.CLOUD, spawnPos.x + Math.cos(ang) * r, spawnPos.y + h * 0.45D,
                    spawnPos.z + Math.sin(ang) * r, 4, 0.2, 0.2, 0.2, 0.04);
        }
        level.sendParticles(ParticleTypes.FLASH, spawnPos.x, spawnPos.y + 2.0D, spawnPos.z, 2, 0, 0, 0, 0);
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 0.9F);
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 1.0F);

        VeldoraEntity veldora = ModEntities.VELDORA.get().create(level);
        if (veldora != null) {
            veldora.moveTo(spawnPos.x, spawnPos.y + 0.5D, spawnPos.z, player.getYRot() + 180.0F, 0.0F);
            veldora.setAllied(true);
            veldora.tame(player);
            veldora.setHumanForm(humanForm);
            veldora.setCustomName(Component.literal("§b§l[BẠO PHONG LONG] §f§lVeldora Tempest"));
            level.addFreshEntity(veldora);
            SageManager.sendCornerAlert(player, "Triệu Hồi Thành Công! Veldora (" + (humanForm ? "Dạng Người" : "Dạng Rồng") + ") đã giáng lâm cùng Bão Tố!", 0xFF38BDF8, 5500, false);
        }
    }
}
