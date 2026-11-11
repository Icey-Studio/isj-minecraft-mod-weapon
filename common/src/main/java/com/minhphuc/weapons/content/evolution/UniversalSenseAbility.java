package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
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

    public static boolean isHostileThreat(LivingEntity entity, net.minecraft.world.entity.player.Player player) {
        if (entity == null || player == null) return false;
        if (com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(player.getUUID(), entity)) {
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player) {
            return true;
        }
        if (entity instanceof com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity pde && !pde.isTame()) {
            return true;
        }
        if (entity instanceof net.minecraft.world.entity.monster.Enemy || entity instanceof net.minecraft.world.entity.monster.Monster) {
            return true;
        }
        if (entity instanceof MilimEntity || entity instanceof VelgryndEntity) {
            return true;
        }
        return false;
    }

    public static void cast(ServerLevel level, ServerPlayer player, int tier) {
        // Mở rộng phạm vi Cảm Nhận Vạn Năng gấp 8 lần theo yêu cầu
        double radius = switch (tier) {
            case 1 -> SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_UNIVERSAL_SENSE) ? 128.0 : 80.0;
            case 2 -> SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_UNIVERSAL_SENSE) ? 256.0 : 160.0;
            default -> SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_UNIVERSAL_SENSE) ? 384.0 : 200.0;
        };

        boolean canSeeThroughGround = tier >= 2;
        boolean canSeeMilimAndDragon = tier >= 3;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2F, 1.8F);

        // Hiệu ứng sóng quét tâm thức xung quanh người chơi
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 80, Math.min(24.0, radius * 0.15), 1.0, Math.min(24.0, radius * 0.15), 0.05);

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);

        // Ưu tiên nhận diện các sinh vật thù địch có thể tấn công người chơi trước, sau đó sắp xếp theo khoảng cách
        list.sort((a, b) -> {
            boolean aHostile = isHostileThreat(a, player);
            boolean bHostile = isHostileThreat(b, player);
            if (aHostile != bHostile) {
                return aHostile ? -1 : 1;
            }
            boolean aTargeting = (a instanceof net.minecraft.world.entity.Mob ma && ma.getTarget() == player);
            boolean bTargeting = (b instanceof net.minecraft.world.entity.Mob mb && mb.getTarget() == player);
            if (aTargeting != bTargeting) {
                return aTargeting ? -1 : 1;
            }
            return Double.compare(a.distanceToSqr(player), b.distanceToSqr(player));
        });

        int detectedCount = 0;
        int hostileCount = 0;

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

            boolean hostile = isHostileThreat(e, player);
            if (hostile) {
                hostileCount++;
            }

            // Phát sáng 20 giây (400 ticks) cho sinh vật thù địch, 15 giây (300 ticks) cho sinh vật thường
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, hostile ? 400 : 300, 0, false, false));
            level.sendParticles(hostile ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD,
                    e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
            detectedCount++;
        }

        // Tự động phân bổ thời lượng camera tour điện ảnh theo số lượng mục tiêu phát hiện (để lia qua hết từng con)
        int cutsceneTicks = (detectedCount == 0) ? 35 : ((detectedCount == 1) ? 45 : Math.min(120, 25 + detectedCount * 24));

        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_UNIVERSAL_SENSE)) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_UNIVERSAL_SENSE, player.getUUID(), player.position(), player.getLookAngle(), cutsceneTicks, ClientboundCinematicSkillPacket.ACTION_START, (float) radius
            ));
        }

        player.displayClientMessage(
                Component.literal("§b§l[CẢM NHẬN VẠN NĂNG] §e(Mức " + tier + " - " + (int) radius + "m) §fPhát hiện §c§l" + hostileCount + " Kẻ Địch Thù Địch §f/ Tổng §a" + detectedCount + " §fsinh vật!"),
                true
        );
    }
}
