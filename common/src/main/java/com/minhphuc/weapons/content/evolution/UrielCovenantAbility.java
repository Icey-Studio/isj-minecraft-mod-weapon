package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kỹ Năng Tối Thượng 1: Thế Ước Vương Uriel (Covenant King Uriel)
 * - Nhát chém hình chữ X màu vàng kim lấp lánh bay xa 30 block.
 * - Mức 1: Không phá đất, chém mob thường, gây 30% Max HP cho Ác ma.
 * - Mức 2: Không phá đất, instakill mob thường, gây 50% Max HP cho Ác ma, Không Vong, Long Chủng, Milim, Boss.
 * - Mức 3: Phá hủy địa hình đất đá; instakill mob thường, Boss, Ác ma, Không Vong; gây 80% Max HP cho Long Chủng và Milim.
 */
public class UrielCovenantAbility {

    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.1F), 1.8F);

    public static void cast(ServerLevel level, ServerPlayer player, int tier) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // Tính 2 vector vuông góc chéo để tạo chữ X
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        if (right.lengthSqr() < 1e-4) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 realUp = right.cross(look).normalize();

        // 2 nhánh chéo của chữ X
        Vec3 arm1 = right.add(realUp).normalize();
        Vec3 arm2 = right.subtract(realUp).normalize();

        // Âm thanh thánh chém vàng kim
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.8F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5F, 1.8F);

        Set<LivingEntity> hitEntities = new HashSet<>();
        double maxDist = 30.0;
        double step = 0.8;
        double armSpan = 2.4; // Độ rộng chữ X

        for (double d = 1.0; d <= maxDist; d += step) {
            Vec3 center = eyePos.add(look.scale(d));

            // Vẽ hạt chữ X lấp lánh vàng kim
            for (double s = -armSpan; s <= armSpan; s += 0.4) {
                Vec3 p1 = center.add(arm1.scale(s));
                Vec3 p2 = center.add(arm2.scale(s));

                level.sendParticles(GOLD_DUST, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0);
                level.sendParticles(GOLD_DUST, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0);

                if (level.random.nextFloat() <= 0.25F) {
                    level.sendParticles(ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0.05, 0.05, 0.05, 0.02);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p2.x, p2.y, p2.z, 1, 0.05, 0.05, 0.05, 0.05);
                }
            }

            // Phá hủy địa hình ở Mức 3 (Tier 3)
            if (tier >= 3) {
                BlockPos centerPos = BlockPos.containing(center);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            BlockPos targetPos = centerPos.offset(dx, dy, dz);
                            BlockState state = level.getBlockState(targetPos);
                            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK
                                    && state.getBlock() != Blocks.END_PORTAL_FRAME
                                    && state.getBlock() != Blocks.END_PORTAL
                                    && state.getBlock() != Blocks.NETHER_PORTAL) {
                                level.destroyBlock(targetPos, false, player);
                            }
                        }
                    }
                }
            }

            // Quét mục tiêu trúng đòn
            AABB box = new AABB(center.x - armSpan, center.y - armSpan, center.z - armSpan,
                    center.x + armSpan, center.y + armSpan, center.z + armSpan);
            List<LivingEntity> inBox = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);
            for (LivingEntity victim : inBox) {
                if (hitEntities.add(victim)) {
                    applyDamage(player, victim, tier);
                }
            }
        }

        player.displayClientMessage(
                Component.literal("§6§l[URIEL] §eThế Ước Vương Uriel (Mức " + tier + ") đã trảm kích chữ X 30 block!"),
                true
        );
    }

    private static void applyDamage(ServerPlayer player, LivingEntity victim, int tier) {
        boolean isPrimordial = victim instanceof PrimordialDemonEntity;
        boolean isKubo = victim instanceof KuboEntity;
        boolean isVelgrynd = victim instanceof VelgryndEntity;
        boolean isMilim = victim instanceof MilimEntity;
        boolean isBoss = (victim instanceof WitherBoss)
                || (victim instanceof EnderDragon)
                || (victim instanceof Warden)
                || (victim instanceof ElderGuardian);

        if (victim instanceof KuboEntity kubo && kubo.isUltimateOrComplete()) {
            // Không Vong Tối Thượng & Hoàn Chỉnh hoàn toàn kháng Thế Ước Vương Uriel
            player.displayClientMessage(Component.literal("§c⚠️ [Uriel] Không thể hủy diệt Tà Thần Không Vong Tối Thượng!"), true);
            return;
        }

        float maxHp = victim.getMaxHealth();

        if (tier == 1) {
            // Mức 1: Không phá đất, chém mob thường, 30% cho ác ma
            if (isPrimordial) {
                victim.hurt(player.damageSources().playerAttack(player), maxHp * 0.30F);
            } else if (!isVelgrynd && !isMilim && !isKubo && !isBoss) {
                victim.hurt(player.damageSources().playerAttack(player), 50.0F);
            }
        } else if (tier == 2) {
            // Mức 2: Mob thường chết luôn; gây 50% HP cho Ác ma, Không Vong, Long Chủng, Milim, Boss
            if (isPrimordial || isKubo || isVelgrynd || isMilim || isBoss) {
                victim.hurt(player.damageSources().playerAttack(player), maxHp * 0.50F);
            } else {
                victim.hurt(player.damageSources().playerAttack(player), maxHp * 10.0F);
            }
        } else {
            // Mức 3: Long Chủng và Milim chịu 80%; toàn bộ còn lại (kể cả boss, ác ma, không vong) chết luôn!
            if (isVelgrynd || isMilim) {
                victim.hurt(player.damageSources().playerAttack(player), maxHp * 0.80F);
            } else {
                victim.hurt(player.damageSources().playerAttack(player), maxHp * 10.0F);
            }
        }
    }
}
