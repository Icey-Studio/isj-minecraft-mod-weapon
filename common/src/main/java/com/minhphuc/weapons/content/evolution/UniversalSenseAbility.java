package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Kỹ Năng Tối Thượng 3: Cảm Nhận Vạn Năng (Universal Sense)
 * - Quét sóng tâm thức làm phát sáng (Glowing) các sinh vật xung quanh.
 * - Mức 1: Phạm vi 10 block, không nhìn xuyên đất (cần tầm nhìn thẳng), không thấy Milim & Long Chủng.
 * - Mức 2: Phạm vi 20 block, nhìn xuyên đất & tường, không thấy Milim & Long Chủng.
 * - Mức 3: Phạm vi 25 block, nhìn xuyên đất & tường, thấy TOÀN BỘ sinh vật kể cả Milim & Long Chủng.
 */
public class UniversalSenseAbility {

    public static void cast(ServerLevel level, ServerPlayer player, int tier) {
        double radius = switch (tier) {
            case 1 -> 10.0;
            case 2 -> 20.0;
            default -> 25.0;
        };

        boolean canSeeThroughGround = tier >= 2;
        boolean canSeeMilimAndDragon = tier >= 3;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2F, 1.8F);

        // Hiệu ứng sóng quét tâm thức xung quanh người chơi
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 80, radius * 0.3, 1.0, radius * 0.3, 0.05);

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);

        int detectedCount = 0;

        for (LivingEntity e : list) {
            double distSq = e.distanceToSqr(player);
            if (distSq > radius * radius) continue;

            boolean isMilim = e instanceof MilimEntity;
            boolean isVelgrynd = e instanceof VelgryndEntity;

            // Nếu chưa đạt Mức 3 thì không thể cảm nhận được Milim và Long Chủng
            if (!canSeeMilimAndDragon && (isMilim || isVelgrynd)) {
                continue;
            }

            // Nếu ở Mức 1 thì không nhìn được xuyên đất / vật cản
            if (!canSeeThroughGround && !player.hasLineOfSight(e)) {
                continue;
            }

            // Phát sáng 15 giây (300 ticks) viền sáng qua tường
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
            level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
            detectedCount++;
        }

        player.displayClientMessage(
                Component.literal("§b§l[CẢM NHẬN VẠN NĂNG] §e(Mức " + tier + ") §fĐã phát hiện §a" + detectedCount + " §fsinh vật trong phạm vi §b" + (int) radius + "m§f!"),
                true
        );
    }
}
