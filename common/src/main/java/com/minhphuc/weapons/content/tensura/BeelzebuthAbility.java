package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.divine.SkillPowerRoll;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundSageActionPacket;
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
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
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
 * - Luân phiên 2 Hình Thái tác chiến:
 *   + Hình Thái 1: Vực Toàn Hư Không (Cột Lốc Xoáy Bão Tố 42m thẳng đứng hút nước/biển).
 *   + Hình Thái 2: Hắc Động Thôn Phệ (Phễu Hư Không khổng lồ vươn ngang 38m nuốt trọn không gian & quân đoàn).
 * - Tự động nhận diện Long Chủng (Veldora, Hỗn Độn Long, Velgrynd, Velzard) để bành trướng quy mô Cực Đại (Mega Titan Scale 105m).
 * - Chống lag đại dương tuyệt đối (Safe Draining), giữ 100% block rắn nguyên vẹn.
 * - Tích hợp Cố Vấn Tác Chiến Raphael 1-Chạm [Y]/[N]:
 *   + Thanh tẩy và hoàn trả nguồn nước tinh khiết vào môi trường.
 *   + Tái cấu trúc Zombie Dân Làng thành Dân Làng khỏe mạnh.
 * - Hỗ trợ Thủy Khí Trảm (Water Blade) và Giải Phóng Thủy Triều 360°.
 */
public class BeelzebuthAbility {

    public static class ActiveBeelzebuthVortex {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 centerPos;
        public final Vec3 dir;
        public final int form; // 0 = Vertical Tornado, 1 = Horizontal Funnel Maw
        public final int totalTicks;
        public int ticksRemaining;
        public final SkillPowerRoll powerRoll;
        public final float scaleMultiplier;
        public int absorbedWaterBlocks = 0;
        public int absorbedLavaBlocks = 0;
        public int absorbedDomainBlocks = 0;
        public int swallowedEntities = 0;
        public int swallowedItems = 0;
        public int swallowedZombies = 0;
        public int swallowedDemonsCount = 0;
        public String firstSwallowedDemonName = "";
        public final List<CompoundTag> swallowedDemonsData = new ArrayList<>();

        public ActiveBeelzebuthVortex(ServerLevel level, ServerPlayer caster, Vec3 centerPos, Vec3 dir, int form,
                                     int totalTicks, SkillPowerRoll powerRoll, float scaleMultiplier) {
            this.level = level;
            this.caster = caster;
            this.centerPos = centerPos;
            this.dir = dir;
            this.form = form;
            this.totalTicks = totalTicks;
            this.ticksRemaining = totalTicks;
            this.powerRoll = powerRoll;
            this.scaleMultiplier = scaleMultiplier;
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
     * Kích hoạt Bạo Thực Vương: Luân phiên Hình Thái 1 (Lốc Xoáy Bão Tố) và Hình Thái 2 (Phễu Hư Không Ngang)
     */
    public static void executeBeelzebuth(ServerLevel level, ServerPlayer player) {
        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Bạo Thực Vương: Thôn Phệ (Predator)");

        CompoundTag data = EntityDataHelper.getCustomData(player);
        int currentForm = data.getInt("BeelzebuthForm"); // 0 = Vertical Tornado, 1 = Horizontal Funnel
        data.putInt("BeelzebuthForm", currentForm == 0 ? 1 : 0); // Đổi lượt kế tiếp

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();

        // 1. Quét nhận diện Long Chủng xung quanh (Veldora, Hỗn Độn Long, Velgrynd, Velzard)
        boolean dragonDetected = false;
        AABB checkDragonBox = player.getBoundingBox().inflate(45.0D);
        List<LivingEntity> dragons = level.getEntitiesOfClass(LivingEntity.class, checkDragonBox, e -> e.isAlive() && (
                e instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity ||
                e instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity ||
                e instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity ||
                e instanceof com.minhphuc.weapons.entity.tensura.ChaosDragonEntity ||
                e.getType().getDescriptionId().toLowerCase().contains("dragon")
        ));
        if (!dragons.isEmpty()) {
            dragonDetected = true;
        }
        float scaleMult = dragonDetected ? 2.8F : roll.multiplier;

        Vec3 center;
        Vec3 dir;
        int durationTicks = 80; // 4.0 giây

        if (currentForm == 0) {
            // HÌNH THÁI 1: VỰC TOÀN HƯ KHÔNG (Cột lốc xoáy bão tố 42m thẳng đứng)
            HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(36.0D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
            if (hit.getType() != HitResult.Type.MISS) {
                center = hit.getLocation();
            } else {
                center = player.position().add(look.scale(10.0D));
            }
            dir = new Vec3(0, 1, 0);

            player.displayClientMessage(
                    Component.literal("§5§l[BẠO THỰC VƯƠNG] §dĐã giải phóng Vực Toàn Hư Không (Lốc Xoáy Bão Tố)! Đang hút chất lỏng và nuốt chửng sinh vật..."),
                    true
            );
        } else {
            // HÌNH THÁI 2: HẮC ĐỘNG THÔN PHỆ (Phễu Hư Không khổng lồ vươn ngang 38m từ bàn tay)
            center = eye.add(look.scale(1.2D));
            dir = look;

            player.displayClientMessage(
                    Component.literal("§5§l[BẠO THỰC VƯƠNG] §dĐã giải phóng Hắc Động Thôn Phệ (Phễu Hư Không Tiêu Hóa)! Đang nuốt trọn không gian và kẻ địch..."),
                    true
            );
        }

        if (dragonDetected) {
            player.displayClientMessage(
                    Component.literal("§6§l[BẠO THỰC VƯƠNG: ĐẠI LONG THÔN PHỆ] §ePhát hiện Long Chủng! Bành trướng quy mô Hư Không lên Cực Đại (Mega Titan Scale) để bao trọn Long Thể!"),
                    false
            );
        }

        // Âm thanh uy nghiêm của Bạo Thực Vương
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.5F, 0.55F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.5F, 0.65F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.70F);

        // Gửi gói tin đồng bộ hiệu ứng điện ảnh đến Client
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_BEELZEBUTH, player.getUUID(), center, dir, durationTicks, ClientboundCinematicSkillPacket.ACTION_START, scaleMult
        ));

        ACTIVE_VORTICES.add(new ActiveBeelzebuthVortex(level, player, center, dir, currentForm, durationTicks, roll, scaleMult));
    }

    /**
     * Vận hành Cột Lốc Xoáy & Phễu Hư Không Bạo Thực Vương mỗi tick
     */
    public static void tickVortices(ServerLevel serverLevel) {
        if (ACTIVE_VORTICES.isEmpty()) return;

        Iterator<ActiveBeelzebuthVortex> it = ACTIVE_VORTICES.iterator();
        while (it.hasNext()) {
            ActiveBeelzebuthVortex v = it.next();
            if (v.level != serverLevel) continue;

            v.ticksRemaining--;
            Vec3 c = v.centerPos;
            boolean isTitan = v.scaleMultiplier > 1.5F;

            // Hiệu ứng hạt trung tâm
            v.level.sendParticles(ParticleTypes.PORTAL, c.x, c.y + 1.0D, c.z, 8, 1.2D, 1.0D, 1.2D, 0.05D);
            v.level.sendParticles(ParticleTypes.DRAGON_BREATH, c.x, c.y + 2.0D, c.z, 6, 0.8D, 1.5D, 0.8D, 0.02D);
            if (v.ticksRemaining % 4 == 0) {
                v.level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5D, c.z, 15, 2.0D, 0.5D, 2.0D, 0.1D);
                v.level.sendParticles(ParticleTypes.BUBBLE, c.x, c.y + 1.0D, c.z, 10, 1.5D, 1.0D, 1.5D, 0.08D);
            }

            // 1. LỰC HÚT XOẮN ỐC THỰC THỂ (Entity Suction Vortex)
            double vacRadius = isTitan ? 36.0D : 16.0D;
            double vacHeight = isTitan ? 100.0D : 42.0D;
            AABB vacuumBox = new AABB(c.x - vacRadius, c.y - 6.0D, c.z - vacRadius, c.x + vacRadius, c.y + vacHeight, c.z + vacRadius);

            if (v.form == 1) {
                // Hình thái Phễu Ngang: Hộp quét theo hướng phóng
                double reach = (isTitan ? 80.0D : 38.0D);
                vacuumBox = new AABB(c, c.add(v.dir.scale(reach))).inflate(isTitan ? 28.0D : 14.0D);
            }

            List<Entity> suctionTargets = v.level.getEntities((Entity) null, vacuumBox,
                    e -> e != v.caster && e.isAlive() && (v.caster == null || !com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(v.caster.getUUID(), e)));

            for (Entity entity : suctionTargets) {
                double dist = entity.position().distanceTo(c);

                if (v.form == 0) {
                    // Hình thái 1: Hút xoắn ốc bão tố thẳng đứng
                    double dx = c.x - entity.getX();
                    double dz = c.z - entity.getZ();
                    double horizDist = Math.hypot(dx, dz);
                    double pullStrength = Math.max(0.3D, 1.3D - (horizDist / vacRadius));
                    double angle = Math.atan2(dz, dx) + 0.55D;
                    double vx = Math.cos(angle) * pullStrength;
                    double vz = Math.sin(angle) * pullStrength;
                    double vy = 0.35D + (horizDist < 5.0D ? 0.40D : 0.08D);

                    entity.setDeltaMovement(vx, vy, vz);
                    entity.hasImpulse = true;
                } else {
                    // Hình thái 2: Hút ngang theo phễu vào họng hư không
                    Vec3 toMouth = c.subtract(entity.position()).normalize().scale(1.25D);
                    entity.setDeltaMovement(toMouth.x, toMouth.y * 0.4D + 0.1D, toMouth.z);
                    entity.hasImpulse = true;
                }

                // Nuốt chửng khi bị cuốn vào mắt bão (bán kính <= 4.5m hoặc <= 10m đối với Titan Long)
                double swallowRadius = isTitan ? 10.0D : 4.5D;
                if (dist <= swallowRadius) {
                    if (entity instanceof ItemEntity itemEntity) {
                        if (v.caster != null) {
                            AzathothStomachData.storeItemStackIntoStomach(v.caster, itemEntity.getItem().copy(), false);
                        }
                        itemEntity.discard();
                        v.swallowedItems++;
                    } else if (entity instanceof PrimedTnt || entity instanceof Creeper) {
                        entity.discard();
                    } else if (entity instanceof ZombieVillager || entity instanceof Zombie) {
                        // Ghi nhận cá thể Zombie có thể tái cấu trúc
                        v.swallowedZombies++;
                        v.swallowedEntities++;
                        v.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                        entity.discard();
                    } else if (entity instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity veldora) {
                        // Thôn Phệ Bạo Phong Long Veldora vào Dạ Dày Vô Hạn
                        if (v.caster != null) {
                            EntityDataHelper.getCustomData(v.caster).putBoolean(AzathothStomachData.KEY_VELDORA_BEFRIENDED, true);
                            com.minhphuc.weapons.world.VeldoraWorldData.get(v.level.getServer().overworld()).setVeldoraStoredInStomach(true, false);
                            MagiculesManager.addAzathothConversion(v.caster, 500_000L);
                            TensuraEvents.handleMobDeathDrop(v.caster, veldora);
                        }
                        veldora.discard();
                        v.swallowedEntities++;
                        v.level.sendParticles(ParticleTypes.FLASH, veldora.getX(), veldora.getY() + 2.0D, veldora.getZ(), 3, 0, 0, 0, 0);
                        if (v.caster != null) {
                            SageManager.sendCornerAlert(v.caster, "« Báo cáo »: Đã thôn phệ Bạo Phong Long Veldora vào Dạ Dày Vô Hạn Azathoth! Phong ấn đang được phân tích giải trừ.", 0xFF38BDF8, 8000, false);
                        }
                    } else if (entity instanceof com.minhphuc.weapons.entity.tensura.ChaosDragonEntity chaosDragon) {
                        // Thôn Phệ & Thanh Tẩy Hỗn Độn Long
                        if (v.caster != null) {
                            TensuraEvents.handleMobDeathDrop(v.caster, chaosDragon);
                            MagiculesManager.addAzathothConversion(v.caster, 350_000L);
                        }
                        chaosDragon.discard();
                        v.swallowedEntities++;
                        v.level.sendParticles(ParticleTypes.FLASH, chaosDragon.getX(), chaosDragon.getY() + 2.0D, chaosDragon.getZ(), 3, 0, 0, 0, 0);
                        if (v.caster != null) {
                            SageManager.sendCornerAlert(v.caster, "« Báo cáo »: Đã thanh tẩy và thôn phệ Hỗn Độn Long vào Dạ Dày Vô Hạn Azathoth! (+350,000 EP)", 0xFFA855F7, 8000, false);
                        }
                    } else if (entity instanceof Display.ItemDisplay display) {
                        ItemStack dItem = ((ItemDisplayAccessor) display).weapons$getItemStack();
                        if (!dItem.isEmpty() && (dItem.is(ModItems.DISINTEGRATION_MAGIC_CIRCLE.get()) || dItem.is(ModItems.BEELZEBUTH_MAGIC_CIRCLE.get()) || dItem.getItem().getDescriptionId().contains("magic_circle"))) {
                            v.level.sendParticles(ParticleTypes.FLASH, display.getX(), display.getY() + 0.5D, display.getZ(), 2, 0, 0, 0, 0);
                            v.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, display.getX(), display.getY() + 0.5D, display.getZ(), 15, 0.4, 0.4, 0.4, 0.08);
                            display.discard();
                            v.absorbedDomainBlocks += 2;
                            if (v.caster != null) {
                                MagiculesManager.restoreMagicules(v.caster, 40_000L);
                                MagiculesManager.addAzathothConversion(v.caster, 40_000L);
                                v.caster.heal(15.0F);
                            }
                        }
                    } else if (entity instanceof PrimordialDemonEntity demon) {
                        // Thôn Phệ Ác Ma Thủy Tổ
                        if (v.caster == null || !demon.isOwnedBy(v.caster)) {
                            CompoundTag dTag = new CompoundTag();
                            dTag.putString("Type", "PRIMORDIAL");
                            dTag.putInt("DemonType", demon.getDemonType().ordinal());
                            dTag.putString("CustomName", demon.getCustomDemonName() != null ? demon.getCustomDemonName() : "");
                            dTag.putBoolean("IsNamed", demon.isNamed());
                            dTag.putBoolean("IsDemonLord", demon.getTags().contains("TensuraDemonLordMob"));
                            demon.saveWithoutId(dTag);
                            v.swallowedDemonsData.add(dTag);
                            v.swallowedDemonsCount++;
                            if (v.firstSwallowedDemonName.isEmpty()) {
                                v.firstSwallowedDemonName = demon.getEffectiveDemonName();
                            }
                            v.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 25, 0.4, 0.6, 0.4, 0.08);
                            v.level.sendParticles(ParticleTypes.FLASH, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 2, 0, 0, 0, 0);
                            demon.discard();
                            v.swallowedEntities++;
                        }
                    } else if (entity instanceof UnderworldDemonEntity demon) {
                        // Thôn Phệ Ác Ma Ma Giới
                        if (v.caster == null || !demon.isOwnedBy(v.caster)) {
                            CompoundTag dTag = new CompoundTag();
                            dTag.putString("Type", "UNDERWORLD");
                            dTag.putInt("DemonType", demon.getDemonType().ordinal());
                            dTag.putInt("Rank", demon.getRank().ordinal());
                            dTag.putInt("Morph", demon.getMorph().ordinal());
                            dTag.putString("CustomName", demon.getCustomDemonName() != null ? demon.getCustomDemonName() : "");
                            dTag.putBoolean("IsNamed", demon.isNamed());
                            dTag.putBoolean("IsDemonLord", demon.isDemonLord());
                            demon.saveWithoutId(dTag);
                            v.swallowedDemonsData.add(dTag);
                            v.swallowedDemonsCount++;
                            if (v.firstSwallowedDemonName.isEmpty()) {
                                v.firstSwallowedDemonName = demon.getEffectiveTitle();
                            }
                            v.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 25, 0.4, 0.6, 0.4, 0.08);
                            v.level.sendParticles(ParticleTypes.FLASH, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 2, 0, 0, 0, 0);
                            demon.discard();
                            v.swallowedEntities++;
                        }
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
                            DamageSource dmg = (v.caster != null) ? v.level.damageSources().playerAttack(v.caster) : v.level.damageSources().magic();
                            living.hurt(dmg, 45.0F * v.powerRoll.multiplier);
                            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 2));
                            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                        }
                    }
                }
            }

            // 2. HẤP THỤ CHẤT LỎNG & LÃNH ĐỊA ÁC MA, MA PHÁP TRẬN (Safe Fluid & Domain Barrier Absorption)
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

                            if (state.is(Blocks.WATER) || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                                v.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedWaterBlocks++;
                                drainedInPulse++;
                            } else if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
                                v.level.setBlock(p, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedWaterBlocks++;
                                drainedInPulse++;
                            } else if (state.is(Blocks.LAVA) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                                v.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedLavaBlocks++;
                                drainedInPulse++;
                            } else if (DomainBarrierBlock.isAnyDomainBarrier(state.getBlock())
                                    || state.is(ModBlocks.ANTI_MAGIC_BARRIER.get())
                                    || state.is(ModBlocks.MULTILAYER_BARRIER.get())
                                    || state.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                                v.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                                v.absorbedDomainBlocks++;
                                drainedInPulse++;
                                v.level.sendParticles(ParticleTypes.WITCH, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 4, 0.3, 0.3, 0.3, 0.05);
                                if (v.caster != null) {
                                    MagiculesManager.restoreMagicules(v.caster, 12_000L);
                                    MagiculesManager.addAzathothConversion(v.caster, 12_000L);
                                    v.caster.heal(3.5F);
                                }
                            }
                        }
                    }
                }

                // Hóa giải các Lãnh Địa Ác Ma Thủy Tổ đang hoạt động trong khu vực
                for (PrimordialDemonEntity pde : v.level.getEntitiesOfClass(PrimordialDemonEntity.class, vacuumBox, d -> d.activeDomainTicks > 0)) {
                    pde.cleanDomainBarriers();
                    pde.clearActiveCircles();
                    pde.activeDomainTicks = 0;
                    pde.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
                    pde.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
                    v.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pde.getX(), pde.getY() + 1.0D, pde.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
                    v.absorbedDomainBlocks += 5;
                    if (v.caster != null) {
                        MagiculesManager.restoreMagicules(v.caster, 80_000L);
                        MagiculesManager.addAzathothConversion(v.caster, 80_000L);
                        v.caster.heal(25.0F);
                    }
                }
                AntiMagicBarrierManager.removeBarriersInArea(v.level, v.centerPos, 24.0D, null);
            }

            // 3. KẾT THÚC CHIÊU THỨC: KÍCH HOẠT ĐỀ XUẤT RAPHAEL 1-CHẠM [Y] / [N]
            if (v.ticksRemaining <= 0) {
                v.level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.5F, 0.8F);
                v.level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.7F);

                if (v.caster != null) {
                    CompoundTag data = EntityDataHelper.getCustomData(v.caster);
                    int curWater = data.getInt("BeelzebuthStoredWater");
                    int curLava = data.getInt("BeelzebuthStoredLava");
                    data.putInt("BeelzebuthStoredWater", curWater + v.absorbedWaterBlocks);
                    data.putInt("BeelzebuthStoredLava", curLava + v.absorbedLavaBlocks);

                    MagiculesManager.restoreMagicules(v.caster, 80_000L);
                    MagiculesManager.addAzathothConversion(v.caster, 80_000L);

                    // Đề xuất 1: Thanh tẩy và hoàn trả nguồn nước tinh khiết
                    if (v.absorbedWaterBlocks > 0) {
                        data.putInt("BeelzebuthPendingPurifyWater", v.absorbedWaterBlocks);
                        data.putLong("BeelzebuthPendingCenterPos", BlockPos.containing(v.centerPos).asLong());
                        SageManager.sendCornerAlert(v.caster,
                                "« Báo cáo »: Đã hấp thụ " + v.absorbedWaterBlocks + " khối nước. Ngài có muốn Thông Tuệ Vương Raphael thanh tẩy và hoàn trả nguồn nước tinh khiết không? [Y] Đồng ý / [N] Giữ trong Dạ Dày",
                                0xFF00E5FF, 9000, false, ServerboundSageActionPacket.ACTION_PURIFY_RESTORE_WATER);
                    }

                    // Đề xuất 2: Tái cấu trúc Zombie Dân Làng
                    if (v.swallowedZombies > 0) {
                        data.putInt("BeelzebuthPendingZombieVillagers", v.swallowedZombies);
                        SageManager.sendCornerAlert(v.caster,
                                "« Báo cáo »: Phát hiện " + v.swallowedZombies + " cá thể Zombie Dân Làng trong Dạ Dày. Ngài có muốn giải trừ virus và tái cấu trúc thành Dân Làng không? [Y] Tái Cấu Trúc / [N] Tiêu Hóa",
                                0xFF22C55E, 9000, false, ServerboundSageActionPacket.ACTION_RECONSTRUCT_ZOMBIES);
                    }

                    // Đề xuất 3: Thôn phệ Lãnh Địa Ác Ma & Ma Pháp Trận
                    if (v.absorbedDomainBlocks > 0) {
                        SageManager.sendCornerAlert(v.caster,
                                "« Báo cáo »: Đã thôn phệ " + v.absorbedDomainBlocks + " cấu trúc Lãnh Địa & Ma Pháp Trận! Chuyển hóa toàn bộ thành Ma Tố & Hồi Phục Thể Trạng!",
                                0xFF38BDF8, 6500, false);
                    }

                    // Đề xuất 4: Hợp nhất Ác Ma vào Linh Hồn Giả Lập & Ban Thể Xác
                    if (v.swallowedDemonsCount > 0) {
                        net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
                        for (CompoundTag t : v.swallowedDemonsData) {
                            listTag.add(t);
                        }
                        data.put("BeelzebuthPendingDemonsList", listTag);
                        data.putInt("BeelzebuthPendingDemonsCount", v.swallowedDemonsCount);
                        SageManager.sendCornerAlert(v.caster,
                                "« Báo cáo »: Đã thôn phệ " + v.swallowedDemonsCount + " ác ma (" + v.firstSwallowedDemonName + ") vào Dạ Dày Vô Hạn. Ngài có muốn hợp nhất vào Linh Hồn Giả Lập, ban Thể Xác Vật Lý (không dùng xương) và nhận Khế Ước Ác Ma không? [Y] Ban Thể Xác & Khế Ước / [N] Giữ trong Dạ Dày",
                                0xFF8B5CF6, 9500, false, ServerboundSageActionPacket.ACTION_GRANT_DEMON_BODY_CONTRACT);
                    }
                }

                it.remove();
            }
        }
    }

    /**
     * THỦY KHÍ TRẢM (Water Blade / High-Pressure Aqua Cutter)
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

        for (double d = 1.0D; d <= range; d += 1.2D) {
            Vec3 pos = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 4, 0.15D, 0.15D, 0.15D, 0.05D);
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 2, 0.08D, 0.08D, 0.08D, 0.02D);
            if ((int) d % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }

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
