package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kỹ Năng Tối Thượng 2: Vương Quyền Chi Phối (Regalia Dominion)
 * - Chi phối mob trở thành minion đi theo phụ đánh kẻ thù (không giới hạn số lượng).
 * - Mức 1: Không thể chi phối Long Chủng, Milim, Boss, Không Vong. Ác ma 30s, Mob thường vĩnh viễn.
 * - Mức 2: Long Chủng & Milim 30s. Boss, Không Vong, Ác ma, Mob thường vĩnh viễn.
 * - Mức 3: Long Chủng vĩnh viễn. Milim 60s (hết hạn sẽ Berserk +200% DMG tàn phá). Boss, Không Vong, Ác ma, Mob thường vĩnh viễn.
 */
public class RegaliaDominionAbility {

    public static final String NBT_DOMINION_MASTER = "RegaliaDominionMaster";
    public static final String NBT_EXPIRE_TICK = "RegaliaDominionExpireTick";
    public static final String NBT_IS_MILIM_BERSERK = "MilimBerserkActive";

    private static final DustParticleOptions GOLD_CROWN = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.2F), 1.4F);

    // Danh sách lưu trữ các sinh vật đang bị chi phối theo level
    private static final Set<UUID> DOMINATED_ENTITIES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void cast(ServerLevel level, ServerPlayer player, int tier) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // Tìm mục tiêu người chơi đang nhìn vào trong phạm vi 30 block
        LivingEntity target = null;
        double bestDist = 30.0;

        AABB searchBox = player.getBoundingBox().inflate(30.0);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && e != player);

        for (LivingEntity e : list) {
            Vec3 toE = e.getEyePosition().subtract(eyePos);
            double dist = toE.length();
            if (dist > 30.0) continue;

            Vec3 toENorm = toE.normalize();
            double dot = look.dot(toENorm);
            if (dot > 0.85 && dist < bestDist) {
                bestDist = dist;
                target = e;
            }
        }

        if (target == null) {
            player.displayClientMessage(Component.literal("§c⚠️ Không tìm thấy mục tiêu sinh vật trong tầm mắt (30 block)!"), true);
            return;
        }

        boolean isPrimordial = target instanceof PrimordialDemonEntity;
        boolean isKubo = target instanceof KuboEntity;
        boolean isVelgrynd = target instanceof VelgryndEntity;
        boolean isMilim = target instanceof MilimEntity;
        boolean isBoss = (target instanceof WitherBoss)
                || (target instanceof EnderDragon)
                || (target instanceof Warden)
                || (target instanceof ElderGuardian);

        long currentTick = level.getGameTime();
        long durationTicks = -1; // -1: vĩnh viễn

        if (target instanceof KuboEntity kubo && kubo.isUltimateOrComplete()) {
            player.displayClientMessage(Component.literal("§c⚠️ Không thể chi phối Tà Thần Không Vong Tối Thượng!"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.8F);
            return;
        }

        if (tier == 1) {
            if (isVelgrynd || isMilim || isBoss || isKubo) {
                player.displayClientMessage(Component.literal("§c⚠️ [Mức 1] Không thể chi phối Boss, Long Chủng, Milim hoặc Không Vong!"), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.8F);
                return;
            }
            if (isPrimordial) {
                durationTicks = 30 * 20L; // 30 giây
            }
        } else if (tier == 2) {
            if (isVelgrynd || isMilim) {
                durationTicks = 30 * 20L; // 30 giây
            }
            // Boss, Không Vong, Ác ma, mob thường: durationTicks = -1 (vĩnh viễn)
        } else {
            // Mức 3:
            if (isMilim) {
                durationTicks = 60 * 20L; // 60 giây, sau đó phát điên
            }
            // Long Chủng, Boss, Không Vong, Ác Ma, mob thường: durationTicks = -1 (vĩnh viễn)
        }

        CompoundTag tag = EntityDataHelper.getCustomData(target);
        tag.putUUID(NBT_DOMINION_MASTER, player.getUUID());
        tag.putLong(NBT_EXPIRE_TICK, durationTicks == -1 ? -1 : currentTick + durationTicks);

        DOMINATED_ENTITIES.add(target.getUUID());

        // Hiệu ứng đăng quang hoàng kim
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.8F, 1.4F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 1.0F);

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 40, 0.3, 0.5, 0.3, 0.2);

        String durStr = durationTicks == -1 ? "VĨNH VIỄN" : (durationTicks / 20) + " Giây";
        player.displayClientMessage(
                Component.literal("§6§l[VƯƠNG QUYỀN] §eĐã chi phối thành công §f" + target.getName().getString() + " §6(" + durStr + ")!"),
                true
        );
    }

    public static void tickDominatedEntities(ServerLevel level) {
        if (DOMINATED_ENTITIES.isEmpty()) return;

        long currentTick = level.getGameTime();
        Iterator<UUID> it = DOMINATED_ENTITIES.iterator();

        while (it.hasNext()) {
            UUID entityId = it.next();
            net.minecraft.world.entity.Entity entity = level.getEntity(entityId);

            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                it.remove();
                continue;
            }

            CompoundTag tag = EntityDataHelper.getCustomData(living);
            if (!tag.hasUUID(NBT_DOMINION_MASTER)) {
                it.remove();
                continue;
            }

            UUID masterUuid = tag.getUUID(NBT_DOMINION_MASTER);
            ServerPlayer master = level.getServer().getPlayerList().getPlayer(masterUuid);

            // Kiểm tra thời hạn hết hiệu lực
            long expireTick = tag.getLong(NBT_EXPIRE_TICK);
            if (expireTick != -1 && currentTick >= expireTick) {
                tag.remove(NBT_DOMINION_MASTER);
                tag.remove(NBT_EXPIRE_TICK);
                it.remove();

                // Nếu là Milim ở Tier 3: Phát điên (Berserk Mode)
                if (living instanceof MilimEntity milim) {
                    milim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 3)); // Sức mạnh cực đại
                    milim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 2));
                    milim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 6000, 0));

                    var attr = milim.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (attr != null) {
                        attr.setBaseValue(attr.getBaseValue() * 3.0); // +200% Sức mạnh
                    }

                    level.playSound(null, milim.getX(), milim.getY(), milim.getZ(),
                            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.6F);

                    if (master != null) {
                        master.sendSystemMessage(Component.literal("§4§l⚠️ CẢNH BÁO: Milim Nava đã thoát khỏi Vương Quyền Chi Phối và PHÁT ĐIÊN (+200% Sức Mạnh)!"));
                    }
                }
                continue;
            }

            // Vẽ vòng vương miện hoàng kim trên đầu minion mỗi 5 ticks
            if (currentTick % 5 == 0) {
                double topY = living.getY() + living.getBbHeight() + 0.35;
                for (int ang = 0; ang < 360; ang += 45) {
                    double rad = Math.toRadians(ang);
                    double px = living.getX() + Math.cos(rad) * 0.45;
                    double pz = living.getZ() + Math.sin(rad) * 0.45;
                    level.sendParticles(GOLD_CROWN, px, topY, pz, 1, 0, 0, 0, 0);
                }
            }

            // AI Hộ Vệ và Tấn Công Kẻ Thù của Master
            if (living instanceof Mob mob && master != null) {
                // Không tấn công Master
                if (mob.getTarget() == master) {
                    mob.setTarget(null);
                }

                // Ưu tiên tấn công mục tiêu mà Master đang đánh hoặc đang đánh Master
                LivingEntity enemy = master.getLastHurtMob();
                if (enemy == null || !enemy.isAlive() || enemy == mob) {
                    enemy = master.getLastHurtByMob();
                }

                if (enemy != null && enemy.isAlive() && enemy != mob && enemy != master) {
                    mob.setTarget(enemy);
                }

                // Đi theo Master nếu cách xa
                double distSq = mob.distanceToSqr(master);
                if (distSq > 256.0) { // > 16m
                    mob.getNavigation().moveTo(master, 1.3);
                }
                if (distSq > 1024.0) { // > 32m: Dịch chuyển tức thời lại gần Master
                    mob.teleportTo(master.getX() + (level.random.nextDouble() - 0.5) * 3,
                            master.getY(),
                            master.getZ() + (level.random.nextDouble() - 0.5) * 3);
                }
            }
        }
    }

    public static boolean isDominatedBy(LivingEntity entity, ServerPlayer player) {
        if (entity == null || player == null) return false;
        CompoundTag tag = EntityDataHelper.getCustomData(entity);
        return tag.hasUUID(NBT_DOMINION_MASTER) && tag.getUUID(NBT_DOMINION_MASTER).equals(player.getUUID());
    }
}
