package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kỹ Năng Tối Thượng 4: Long Giam Vô Hạn (Infinite Dragon Prison)
 * - Mái vòm khối kết giới hình cầu kín hoàn toàn (bao trọn cả mặt đất/lòng đất) bằng khối kết giới hoàng kim lấp lánh (màu cột sáng jacob_light_pillar).
 * - Tồn tại vĩnh viễn cho đến khi người chơi đấm TAY KHÔNG vào khối lồng thì mới hóa giải.
 * - Người chơi tự do ra vào; mob có thể vào từ bên ngoài nhưng KHÔNG THỂ RA ngoài.
 * - Mob bị nhốt sẽ điên loạn tấn công cắn xé lẫn nhau (Infighting / Frenzy).
 * - Mức 1: Bán kính 6m, nhốt Boss & mob thường. Ác ma, Milim, Không Vong, Long Chủng phá được ngay.
 * - Mức 2: Bán kính 12m, nhốt Boss, mob thường, Ác ma. Milim, Không Vong, Long Chủng bị nhốt 30s rồi mới phá.
 * - Mức 3: Bán kính 20m, nhốt TẤT CẢ kể cả Milim, Long Chủng, Ác Ma, Không Vong và chúng TUYỆT ĐỐI KHÔNG THỂ PHÁ ĐƯỢC!
 */
public class InfiniteDragonPrisonAbility {

    private static final DustParticleOptions GOLD_BARRIER = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.15F), 1.8F);

    public static class ActivePrison {
        public final UUID id = UUID.randomUUID();
        public final ServerLevel level;
        public final UUID casterUuid;
        public final Vec3 center;
        public final double radius;
        public final int tier;
        public final long createTick;
        public int ticksAlive = 0;
        public final Map<BlockPos, BlockState> replacedBlocks = new ConcurrentHashMap<>();

        public ActivePrison(ServerLevel level, ServerPlayer caster, Vec3 center, double radius, int tier) {
            this.level = level;
            this.casterUuid = caster.getUUID();
            this.center = center;
            this.radius = radius;
            this.tier = tier;
            this.createTick = level.getGameTime();
        }
    }

    private static final List<ActivePrison> ACTIVE_PRISONS = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer player, int tier) {
        double radius = switch (tier) {
            case 1 -> 6.0;
            case 2 -> 12.0;
            default -> 20.0;
        };

        Vec3 center = player.position().add(0, 1.0, 0);

        // Hóa giải và hoàn nguyên khối lồng cũ của player này nếu có
        cleanPlayerPrisons(player.getUUID());

        ActivePrison prison = new ActivePrison(level, player, center, radius, tier);

        // Tạo kết giới vật lý hình cầu bao bọc hoàn toàn cả mặt đất và bên dưới
        BlockPos cPos = BlockPos.containing(center);
        int rInt = (int) Math.ceil(radius);
        BlockState barrierState = ModBlocks.DRAGON_PRISON_BARRIER.get().defaultBlockState();

        for (int x = -rInt; x <= rInt; x++) {
            for (int y = -rInt; y <= rInt; y++) {
                for (int z = -rInt; z <= rInt; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq >= (radius - 0.9) * (radius - 0.9) && distSq <= (radius + 0.9) * (radius + 0.9)) {
                        BlockPos bPos = cPos.offset(x, y, z);
                        BlockState orig = level.getBlockState(bPos);
                        // Không ghi đè Bedrock hoặc khối kết giới đã có
                        if (orig.getDestroySpeed(level, bPos) >= 0 && !orig.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                            prison.replacedBlocks.put(bPos, orig);
                            level.setBlock(bPos, barrierState, 2);
                        }
                    }
                }
            }
        }

        ACTIVE_PRISONS.add(prison);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0F, 1.2F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.0F);

        player.displayClientMessage(
                Component.literal("§6§l[LONG GIAM VÔ HẠN] §eĐã triển khai kết giới bao phủ (" + (int) radius + "m - Mức " + tier + ")!"),
                true
        );
        player.sendSystemMessage(Component.literal("§e✦ Long Giam Vô Hạn sẽ tồn tại vĩnh viễn! bạn có muốn phá giải nó."));
    }

    /**
     * Cho phép quái/thực thể bên ngoài tự do bước vào lồng giam, nhưng từ trong không thể đi ra.
     */
    public static boolean canEntityPass(Entity entity, BlockPos pos) {
        for (ActivePrison prison : ACTIVE_PRISONS) {
            if (prison.level == entity.level()) {
                double distMob = entity.position().distanceTo(prison.center);
                // Nếu sinh vật đang ở ngoài hoặc chạm mép ngoài, cho phép bước vào
                if (distMob >= prison.radius - 0.8) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Hoàn nguyên toàn bộ các khối ban đầu khi lồng bị phá hủy hoặc giải trừ.
     */
    public static void cleanPrison(ActivePrison prison) {
        if (prison == null) return;
        for (Map.Entry<BlockPos, BlockState> entry : prison.replacedBlocks.entrySet()) {
            if (prison.level.getBlockState(entry.getKey()).is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                prison.level.setBlock(entry.getKey(), entry.getValue(), 2);
            }
        }
        prison.replacedBlocks.clear();
    }

    public static void cleanPlayerPrisons(UUID playerUuid) {
        synchronized (ACTIVE_PRISONS) {
            Iterator<ActivePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActivePrison p = it.next();
                if (p.casterUuid.equals(playerUuid)) {
                    cleanPrison(p);
                    it.remove();
                }
            }
        }
    }

    public static void tickPrisons(ServerLevel level) {
        if (ACTIVE_PRISONS.isEmpty()) return;

        synchronized (ACTIVE_PRISONS) {
            Iterator<ActivePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActivePrison prison = it.next();
                if (prison.level != level) continue;

                prison.ticksAlive++;
                Vec3 center = prison.center;
                double r = prison.radius;

                // 1. Hiệu ứng hạt ánh sáng hoàng kim lấp lánh (mỗi 4 ticks)
                if (prison.ticksAlive % 4 == 0) {
                    for (int ang = 0; ang < 360; ang += 20) {
                        double rad = Math.toRadians(ang);
                        double x = center.x + Math.cos(rad) * r;
                        double z = center.z + Math.sin(rad) * r;

                        level.sendParticles(GOLD_BARRIER, x, center.y, z, 1, 0, 0, 0, 0);
                        level.sendParticles(GOLD_BARRIER, x, center.y + (r * 0.5), z, 1, 0, 0, 0, 0);
                        level.sendParticles(GOLD_BARRIER, x, center.y - (r * 0.5), z, 1, 0, 0, 0, 0);

                        if (level.random.nextFloat() <= 0.15F) {
                            level.sendParticles(ParticleTypes.END_ROD, x, center.y + (level.random.nextDouble() - 0.5) * r, z, 1, 0, 0, 0, 0.02);
                        }
                    }
                }

                // 2. Quét sinh vật trong vùng lồng giam
                AABB prisonBox = new AABB(center.x - r - 2, center.y - r - 2, center.z - r - 2,
                        center.x + r + 2, center.y + r + 2, center.z + r + 2);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, prisonBox, e -> e.isAlive() && !(e instanceof Player));

                boolean cageBroken = false;

                for (LivingEntity e : entities) {
                    double dist = e.position().distanceTo(center);

                    boolean isPrimordial = e instanceof PrimordialDemonEntity;
                    boolean isMilim = e instanceof MilimEntity;
                    boolean isKubo = e instanceof KuboEntity;
                    boolean isVelgrynd = e instanceof VelgryndEntity;

                    // Kiểm tra điều kiện phá lồng theo Mức (Tier)
                    if (dist <= r) {
                        if (prison.tier == 1) {
                            // Mức 1: Ác ma, Milim, Không Vong, Long Chủng phá được ngay
                            if (isPrimordial || isMilim || isKubo || isVelgrynd) {
                                cageBroken = true;
                                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.6F);
                                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                if (caster != null) {
                                    caster.sendSystemMessage(Component.literal("§c⚠️ " + e.getName().getString() + " đã phá vỡ Lồng Giam Vô Hạn (Mức 1)!"));
                                }
                                break;
                            }
                        } else if (prison.tier == 2) {
                            // Mức 2: Milim, Không Vong, Long Chủng bị nhốt 30s (600 ticks) rồi mới phá lồng
                            if ((isMilim || isKubo || isVelgrynd) && prison.ticksAlive >= 600) {
                                cageBroken = true;
                                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                        SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 3.0F, 0.7F);
                                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                if (caster != null) {
                                    caster.sendSystemMessage(Component.literal("§c⚠️ Sau 30s bị giam cầm, " + e.getName().getString() + " đã phá nát Long Giam Vô Hạn (Mức 2)!"));
                                }
                                break;
                            }
                        }
                        // Mức 3: Tuyệt đối không thể phá lồng!

                        // Đẩy lùi mob bên trong khi cố trườn qua biên giới (Quái chỉ có thể vào, không thể ra)
                        if (dist > r - 1.2) {
                            Vec3 toCenter = center.subtract(e.position()).normalize();
                            e.setDeltaMovement(toCenter.scale(0.8));
                            e.hurtMarked = true;
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1.0, e.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                        }

                        // Kích hoạt trạng thái điên loạn cắn xé lẫn nhau (Frenzy / Infighting)
                        if (prison.ticksAlive % 20 == 0 && e instanceof Mob mob) {
                            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1));
                            // Tìm mục tiêu khác trong lồng để cắn xé
                            for (LivingEntity other : entities) {
                                if (other != mob && other.isAlive()) {
                                    mob.setTarget(other);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (cageBroken) {
                    cleanPrison(prison);
                    it.remove();
                }
            }
        }
    }

    /**
     * Người chơi đấm tay không vào khối kết giới để hóa giải phong ấn.
     */
    public static boolean dispelByPunch(ServerPlayer player, BlockPos pos) {
        if (!player.getMainHandItem().isEmpty()) return false;

        ActivePrison found = null;
        synchronized (ACTIVE_PRISONS) {
            for (ActivePrison prison : ACTIVE_PRISONS) {
                if (prison.replacedBlocks.containsKey(pos) || prison.center.distanceTo(Vec3.atCenterOf(pos)) <= prison.radius + 3.0) {
                    found = prison;
                    break;
                }
            }
            if (found != null) {
                cleanPrison(found);
                ACTIVE_PRISONS.remove(found);
            }
        }

        if (found != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5F, 1.2F);

            player.displayClientMessage(
                    Component.literal("§6§l[LONG GIAM] §aBạn đã hóa giải thành công Lồng Giam Vô Hạn!"),
                    true
            );
            return true;
        }

        return false;
    }

    /**
     * Người chơi đấm tay không vào không khí sát ranh giới lồng để hóa giải phong ấn.
     */
    public static boolean checkEmptyHandPunch(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) return false;

        Vec3 eyePos = player.getEyePosition();
        ActivePrison found = null;

        synchronized (ACTIVE_PRISONS) {
            for (ActivePrison prison : ACTIVE_PRISONS) {
                if (prison.casterUuid.equals(player.getUUID())) {
                    double dist = eyePos.distanceTo(prison.center);
                    if (Math.abs(dist - prison.radius) <= 3.0 || dist <= prison.radius + 1.0) {
                        found = prison;
                        break;
                    }
                }
            }
            if (found != null) {
                cleanPrison(found);
                ACTIVE_PRISONS.remove(found);
            }
        }

        if (found != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5F, 1.2F);

            player.displayClientMessage(
                    Component.literal("§6§l[LONG GIAM] §aBạn đã hóa giải thành công Lồngg Giam Vô Hạn!"),
                    true
            );
            return true;
        }

        return false;
    }
}
