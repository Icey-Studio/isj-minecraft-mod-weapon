package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.divine.SkillPowerRoll;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kỹ Năng Tối Thượng: Bạo Thực Vương Beelzebuth (Gluttonous King Beelzebuth - 暴食之王)
 * Lấy cảm hứng từ anime Chuyển Sinh Thành Slime:
 * - Triệu hồi Lốc Xoáy Hư Không Tím Đen khổng lồ cao 42m với 4 dải nước xoáy lam ngọc quấn quanh.
 * - Hút và nuốt chửng chất lỏng (nước biển, hồ, dung nham) và sinh vật vào Dạ Dày Vô Hạn Azathoth.
 * - An toàn tuyệt đối với địa hình khối cứng (không phá hỏng cát, đá, san hô, công trình).
 * - Chống lag tối ưu cho đại dương (Safe Draining, không tạo chuỗi cập nhật khối gây sụt giảm TPS).
 * - Tự động lọc sạch nước, phân tích hóa học tách muối/tạp chất và tích lũy ma tố.
 * - Cho phép xuất ngược chất lỏng đã lọc thành [Thủy Khí Trảm - Water Blade] hoặc [Giải Phóng Thủy Triều].
 */
public class BeelzebuthAbility {

    public static class ActiveBeelzebuthVortex {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 centerPos;
        public final int totalTicks;
        public int ticksRemaining;
        public final SkillPowerRoll powerRoll;
        public int absorbedWaterBlocks = 0;
        public int absorbedLavaBlocks = 0;
        public int swallowedEntities = 0;
        public int swallowedItems = 0;

        public ActiveBeelzebuthVortex(ServerLevel level, ServerPlayer caster, Vec3 centerPos, int totalTicks, SkillPowerRoll powerRoll) {
            this.level = level;
            this.caster = caster;
            this.centerPos = centerPos;
            this.totalTicks = totalTicks;
            this.ticksRemaining = totalTicks;
            this.powerRoll = powerRoll;
        }
    }

    public static final List<ActiveBeelzebuthVortex> ACTIVE_VORTICES = new ArrayList<>();

    public static class ActiveBeelzebuthDragon {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 startPos;
        public final Vec3 lookVec;
        public final float yaw;
        public final float pitch;
        public final Display.ItemDisplay displayEntity;
        public int ticksRemaining;
        public final int totalTicks;
        public final SkillPowerRoll powerRoll;

        public ActiveBeelzebuthDragon(ServerLevel level, ServerPlayer caster, Vec3 startPos, Vec3 lookVec,
                                      float yaw, float pitch, Display.ItemDisplay displayEntity, int durationTicks,
                                      SkillPowerRoll powerRoll) {
            this.level = level;
            this.caster = caster;
            this.startPos = startPos;
            this.lookVec = lookVec;
            this.yaw = yaw;
            this.pitch = pitch;
            this.displayEntity = displayEntity;
            this.ticksRemaining = durationTicks;
            this.totalTicks = durationTicks;
            this.powerRoll = powerRoll;
        }
    }

    public static final List<ActiveBeelzebuthDragon> ACTIVE_DRAGONS = new ArrayList<>();

    /**
     * Kích hoạt Bạo Thực Vương: Triệu hồi Cột Lốc Xoáy Hư Không Hút Nước & Nuốt Chửng Vạn Vật
     */
    public static void executeBeelzebuth(ServerLevel level, ServerPlayer player) {
        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Bạo Thực Vương: Thôn Phệ (Predator)");

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();

        // Tìm vị trí tâm lốc xoáy (bề mặt nước/đất mà người chơi nhìn vào hoặc 10m phía trước)
        HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(36.0D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        Vec3 center;
        if (hit.getType() != HitResult.Type.MISS) {
            center = hit.getLocation();
        } else {
            center = player.position().add(look.scale(10.0D));
        }

        // Âm thanh uy nghiêm của Bạo Thực Vương
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.5F, 0.55F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.5F, 0.65F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.70F);

        // Gửi gói hiệu ứng điện ảnh Anime Lốc Xoáy Hư Không cho tất cả người chơi lân cận
        int durationTicks = 80; // 4.0 giây
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_BEELZEBUTH, player.getUUID(), center, new Vec3(0, 1, 0), durationTicks, ClientboundCinematicSkillPacket.ACTION_START, roll.multiplier
        ));

        ACTIVE_VORTICES.add(new ActiveBeelzebuthVortex(level, player, center, durationTicks, roll));

        player.displayClientMessage(
                Component.literal("§5§l[BẠO THỰC VƯƠNG] §dĐã giải phóng Lốc Xoáy Hư Không Beelzebuth! Đang hút chất lỏng và nuốt chửng sinh vật..."),
                true
        );
    }

    /**
     * Vận hành Cột Lốc Xoáy Bạo Thực Vương mỗi tick:
     * - Hút xoắn ốc sinh vật lên cao và nuốt vào Dạ Dày Vô Hạn.
     * - Rút chất lỏng (nước/dung nham) an toàn không giật lag.
     * - Hoàn tất phân tích thanh lọc nước và cộng Ma Tố.
     */
    public static void tickVortices(ServerLevel serverLevel) {
        if (ACTIVE_VORTICES.isEmpty()) return;

        Iterator<ActiveBeelzebuthVortex> it = ACTIVE_VORTICES.iterator();
        while (it.hasNext()) {
            ActiveBeelzebuthVortex v = it.next();
            if (v.level != serverLevel) continue;

            v.ticksRemaining--;
            Vec3 c = v.centerPos;

            // Hiệu ứng hạt trung tâm
            v.level.sendParticles(ParticleTypes.PORTAL, c.x, c.y + 1.0D, c.z, 8, 1.2D, 1.0D, 1.2D, 0.05D);
            v.level.sendParticles(ParticleTypes.DRAGON_BREATH, c.x, c.y + 2.0D, c.z, 6, 0.8D, 1.5D, 0.8D, 0.02D);
            if (v.ticksRemaining % 4 == 0) {
                v.level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5D, c.z, 15, 2.0D, 0.5D, 2.0D, 0.1D);
                v.level.sendParticles(ParticleTypes.BUBBLE, c.x, c.y + 1.0D, c.z, 10, 1.5D, 1.0D, 1.5D, 0.08D);
            }

            // 1. LỰC HÚT XOẮN ỐC THỰC THỂ (Entity Suction Vortex)
            AABB vacuumBox = new AABB(c.x - 16.0D, c.y - 4.0D, c.z - 16.0D, c.x + 16.0D, c.y + 42.0D, c.z + 16.0D);
            List<Entity> suctionTargets = v.level.getEntities((Entity) null, vacuumBox,
                    e -> e != v.caster && e.isAlive() && (v.caster == null || !com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(v.caster.getUUID(), e)));

            for (Entity entity : suctionTargets) {
                double dx = c.x - entity.getX();
                double dz = c.z - entity.getZ();
                double dist = Math.hypot(dx, dz);

                // Lực hút vào trục tâm + lực xoay tròn tiếp tuyến + lực nâng bổng lên cột lốc xoáy
                double pullStrength = Math.max(0.25D, 1.25D - (dist / 16.0D));
                double angle = Math.atan2(dz, dx) + 0.55D; // Xoắn ốc tiếp tuyến
                double vx = Math.cos(angle) * pullStrength;
                double vz = Math.sin(angle) * pullStrength;
                double vy = 0.32D + (dist < 4.0D ? 0.38D : 0.08D);

                entity.setDeltaMovement(vx, vy, vz);
                entity.hasImpulse = true;

                // Nuốt chửng khi bị cuốn vào mắt bão (bán kính <= 4.2m)
                if (dist <= 4.2D && entity.getY() >= c.y - 2.0D && entity.getY() <= c.y + 40.0D) {
                    if (entity instanceof ItemEntity itemEntity) {
                        if (v.caster != null) {
                            AzathothStomachData.storeItemStackIntoStomach(v.caster, itemEntity.getItem().copy(), false);
                        }
                        itemEntity.discard();
                        v.swallowedItems++;
                    } else if (entity instanceof PrimedTnt || entity instanceof Creeper) {
                        entity.discard();
                    } else if (entity instanceof LivingEntity living) {
                        if (living.getHealth() <= 120.0F || living.getMaxHealth() <= 200.0F) {
                            v.level.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                            v.level.sendParticles(ParticleTypes.DRAGON_BREATH, living.getX(), living.getY() + 1.0D, living.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.05D);
                            if (v.caster != null) {
                                TensuraEvents.handleMobDeathDrop(v.caster, living);
                            }
                            living.discard();
                            v.swallowedEntities++;
                        } else {
                            // Boss hoặc quái khủng: chịu 45 sát thương chuẩn mỗi giây và hiệu ứng hư vô
                            DamageSource dmg = (v.caster != null) ? v.level.damageSources().playerAttack(v.caster) : v.level.damageSources().magic();
                            living.hurt(dmg, 45.0F * v.powerRoll.multiplier);
                            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 2));
                            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                        }
                    }
                }
            }

            // 2. HẤP THỤ CHẤT LỎNG AN TOÀN - CHỐNG LAG ĐẠI DƯƠNG (Safe Fluid Absorption)
            // Quét định kỳ mỗi 2 tick một đợt tối đa 32 khối chất lỏng xung quanh tâm
            if (v.ticksRemaining % 2 == 0) {
                int drainedInPulse = 0;
                int maxDrainPerPulse = 32;
                BlockPos centerPos = BlockPos.containing(c);

                for (int dy = 2; dy >= -8 && drainedInPulse < maxDrainPerPulse; dy--) {
                    for (int dx = -6; dx <= 6 && drainedInPulse < maxDrainPerPulse; dx++) {
                        for (int dz = -6; dz <= 6 && drainedInPulse < maxDrainPerPulse; dz++) {
                            if ((dx * dx + dz * dz) > 36) continue;

                            BlockPos p = centerPos.offset(dx, dy, dz);
                            BlockState state = v.level.getBlockState(p);

                            // Kiểm tra nước biển / hồ
                            if (state.is(Blocks.WATER) || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                                // Sử dụng Flag 2 (Block.UPDATE_CLIENTS) để không kích hoạt chuỗi cập nhật lan truyền lag
                                v.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedWaterBlocks++;
                                drainedInPulse++;
                            } else if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
                                // Khối bị ngập nước (san hô, phiến đá): chỉ rút nước, giữ nguyên khối rắn!
                                v.level.setBlock(p, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedWaterBlocks++;
                                drainedInPulse++;
                            } else if (state.is(Blocks.LAVA) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                                // Kiểm tra dung nham
                                v.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedLavaBlocks++;
                                drainedInPulse++;
                            }
                            // TUYỆT ĐỐI KHÔNG ẢNH HƯỞNG CÁC BLOCK RẮN (Đá, cát, sỏi, công trình đều nguyên vẹn)
                        }
                    }
                }
            }

            // 3. KẾT THÚC LỐC XOÁY: BÁO CÁO RAPHAEL & NẠP DỮ LIỆU
            if (v.ticksRemaining <= 0) {
                v.level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.5F, 0.8F);
                v.level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.7F);

                if (v.caster != null) {
                    CompoundTag data = EntityDataHelper.getCustomData(v.caster);
                    int curWater = data.getInt("BeelzebuthStoredWater");
                    int curLava = data.getInt("BeelzebuthStoredLava");
                    data.putInt("BeelzebuthStoredWater", curWater + v.absorbedWaterBlocks);
                    data.putInt("BeelzebuthStoredLava", curLava + v.absorbedLavaBlocks);

                    // Hồi phục Ma Tố & Thưởng EP từ quá trình phân tích
                    MagiculesManager.restoreMagicules(v.caster, 80_000L);
                    MagiculesManager.addAzathothConversion(v.caster, 80_000L);

                    // Thông báo Cố vấn Tác chiến Raphael
                    String report = String.format(Locale.ROOT,
                            "« Báo cáo. Đã kích hoạt Bạo Thực Vương Beelzebuth: Đã phân tích và hấp thụ %d khối nước tinh khiết, %d khối dung nham và %d sinh vật vào Dạ Dày Vô Hạn Azathoth. Thanh lọc tách muối/tạp chất, tích trữ áp suất thủy lực và nhiệt năng (+80,000 Ma tố). »",
                            v.absorbedWaterBlocks, v.absorbedLavaBlocks, v.swallowedEntities);
                    SageManager.sendCornerAlert(v.caster, report, 0x00FFFF, 7500, false);
                }

                it.remove();
            }
        }
    }

    /**
     * THỦY KHÍ TRẢM (Water Blade / High-Pressure Aqua Cutter)
     * Kích hoạt khi Sneak + Click nhìn về phía trước:
     * Tiêu hao nước tinh khiết đã lọc trong Dạ Dày Vô Hạn, phóng ra lưỡi dao nước áp suất cao chém ngọt mọi thứ phía trước.
     */
    public static void castWaterBlade(ServerLevel level, ServerPlayer player) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        int storedWater = data.getInt("BeelzebuthStoredWater");

        if (storedWater >= 20) {
            data.putInt("BeelzebuthStoredWater", storedWater - 20);
        } else if (!player.isCreative()) {
            player.displayClientMessage(Component.literal("§c§l[DẠ DÀY VÔ HẠN] §eChưa đủ nước tinh khiết tích lũy! Đang dùng Ma Tố cô đặc thay thế..."), true);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2.8F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.0F, 1.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.5F, 0.6F);

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        double range = 36.0D;

        // Bắn chùm hạt tia nước áp suất cao cực đẹp
        for (double d = 1.0D; d <= range; d += 1.2D) {
            Vec3 pos = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 4, 0.15D, 0.15D, 0.15D, 0.05D);
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 2, 0.08D, 0.08D, 0.08D, 0.02D);
            if ((int) d % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }

        // Quét sát thương trảm xuyên thấu (140.0F true damage)
        AABB beamBox = new AABB(eye, eye.add(look.scale(range))).inflate(2.2D);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, beamBox,
                e -> e != player && e.isAlive() && !com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(player.getUUID(), e));

        int hitCount = 0;
        for (LivingEntity victim : victims) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5D, 0).subtract(eye);
            double proj = toVictim.dot(look);
            if (proj >= 0 && proj <= range) {
                double perpSq = toVictim.lengthSqr() - (proj * proj);
                if (perpSq <= 4.0D) {
                    victim.hurt(level.damageSources().playerAttack(player), 140.0F);
                    victim.clearFire();
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    level.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 1, 0, 0, 0, 0);
                    hitCount++;
                }
            }
        }

        int remainingWater = data.getInt("BeelzebuthStoredWater");
        player.displayClientMessage(
                Component.literal("§b§l[THỦY KHÍ TRẢM] §fLưỡi Dao Nước Áp Suất Cao đã chém xuyên qua " + hitCount + " mục tiêu! (Nước tinh khiết còn: " + remainingWater + " khối)"),
                true
        );
    }

    /**
     * GIẢI PHÓNG THỦY TRIỀU (Purified Tidal Shockwave)
     * Kích hoạt khi Sneak + Cúi nhìn xuống đất:
     * Xả nước áp suất cao dạng sóng xung kích 360 độ dập tắt biển lửa, làm nguội dung nham thành Hắc Diệu Thạch và đẩy lùi kẻ thù.
     */
    public static void castTidalRelease(ServerLevel level, ServerPlayer player) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        int storedWater = data.getInt("BeelzebuthStoredWater");

        if (storedWater >= 60) {
            data.putInt("BeelzebuthStoredWater", storedWater - 60);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 2.5F, 1.0F);

        BlockPos center = player.blockPosition();
        int radius = 12;

        // Dập lửa và làm nguội dung nham xung quanh
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((x * x + z * z) > (radius * radius)) continue;
                    BlockPos p = center.offset(x, y, z);
                    BlockState bs = level.getBlockState(p);

                    if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    } else if (bs.is(Blocks.LAVA)) {
                        level.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Đẩy lùi kẻ thù xung quanh 360 độ
        AABB waveBox = new AABB(player.getX() - radius, player.getY() - 3, player.getZ() - radius,
                player.getX() + radius, player.getY() + 4, player.getZ() + radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, waveBox,
                e -> e != player && e.isAlive() && !com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(player.getUUID(), e));

        for (LivingEntity e : nearby) {
            Vec3 away = e.position().subtract(player.position()).normalize().scale(1.8D);
            e.setDeltaMovement(away.x, 0.45D, away.z);
            e.hasImpulse = true;
            e.hurt(level.damageSources().playerAttack(player), 80.0F);
            e.clearFire();
        }

        // Hạt sóng nước tung tóe tỏa rộng 360 độ
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            double px = player.getX() + Math.cos(rad) * 6.0D;
            double pz = player.getZ() + Math.sin(rad) * 6.0D;
            level.sendParticles(ParticleTypes.SPLASH, px, player.getY() + 0.5D, pz, 10, 0.8D, 0.3D, 0.8D, 0.1D);
        }

        player.displayClientMessage(
                Component.literal("§3§l[GIẢI PHÓNG THỦY TRIỀU] §fĐã xả lũ áp suất cao 360°, dập tắt toàn bộ hỏa hoạn và hóa đá dung nham xung quanh!"),
                true
        );
    }

    /**
     * Tích hợp với vòng lặp server tick (DivineWeaponEvents gọi tickDragons mỗi tick)
     */
    public static void tickDragons(ServerLevel serverLevel) {
        tickVortices(serverLevel);

        if (ACTIVE_DRAGONS.isEmpty()) return;

        Iterator<ActiveBeelzebuthDragon> it = ACTIVE_DRAGONS.iterator();
        while (it.hasNext()) {
            ActiveBeelzebuthDragon d = it.next();
            if (d.level != serverLevel) continue;

            d.ticksRemaining--;
            if (d.ticksRemaining <= 0) {
                if (d.displayEntity != null && d.displayEntity.isAlive()) {
                    d.displayEntity.discard();
                }
                it.remove();
            }
        }
    }

    /**
     * Kỹ Năng Tối Thượng: Bạo Thực Vương Beelzebuth - Phân nhánh 2: Hủ Hóa & Bạo Liệt
     */
    public static void executeCorrosion(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.8F, 0.6F);

        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Bạo Thực Vương: Hủ Hóa & Bạo Liệt");

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        double maxDist = 14.0D;

        Set<LivingEntity> hitEntities = new HashSet<>();
        Vec3 endPos = eyePos.add(look.scale(maxDist));
        AABB beamBox = new AABB(eyePos, endPos).inflate(2.5D);
        List<LivingEntity> potentialVictims = level.getEntitiesOfClass(LivingEntity.class, beamBox, e -> e != player && e.isAlive());

        for (LivingEntity victim : potentialVictims) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5D, 0).subtract(eyePos);
            double distAlongLook = toVictim.dot(look);

            if (distAlongLook >= 0 && distAlongLook <= maxDist) {
                double perpDistSq = toVictim.lengthSqr() - (distAlongLook * distAlongLook);
                double spreadAtDist = 0.7D + (distAlongLook * 0.15D);

                if (perpDistSq <= spreadAtDist * spreadAtDist) {
                    hitEntities.add(victim);

                    if (roll.isOverdrive()) {
                        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 5));
                        victim.hurt(level.damageSources().magic(), 1000.0F);
                    } else if (roll.isNormal()) {
                        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 3));
                        victim.hurt(level.damageSources().magic(), 200.0F * roll.multiplier);
                    } else {
                        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                        victim.hurt(level.damageSources().magic(), 50.0F * roll.multiplier);
                    }

                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
                    level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 3, 0.2D, 0.4D, 0.2D, 0.05D);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            }
        }

        for (double d = 1.0D; d <= maxDist; d += 2.0D) {
            Vec3 centerPos = eyePos.add(look.scale(d));
            double spread = 0.6D + (d * 0.12D);
            double ox = (level.random.nextDouble() - 0.5D) * spread;
            double oy = (level.random.nextDouble() - 0.5D) * spread;
            double oz = (level.random.nextDouble() - 0.5D) * spread;

            level.sendParticles(ParticleTypes.DRAGON_BREATH, centerPos.x + ox, centerPos.y + oy, centerPos.z + oz, 1, 0.02D, 0.02D, 0.02D, 0.02D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centerPos.x + ox, centerPos.y + oy, centerPos.z + oz, 1, 0.02D, 0.02D, 0.02D, 0.02D);
        }

        player.displayClientMessage(
                Component.literal("§d§l[BẠO THỰC VƯƠNG] §fHãy ăn sạch mọi thứ Beelzebuth!"),
                true
        );
    }
}
