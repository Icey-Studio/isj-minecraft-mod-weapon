package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.SkillPowerRoll;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Kỹ Năng Tối Thượng: Bạo Thực Vương Beelzebuth (Gluttonous King Beelzebuth - 暴食之王)
 * Triệu hồi Đầu Rồng Hư Không Voxel 3D khổng lồ (3D Dragon Maw) với mõm dài, hàm há ngoác,
 * răng nanh sắc bén, mắt trắng phát quang và râu rồng tím vươn dài.
 * Lao vút theo góc nhìn 3D của người chơi, tạo lực hút chân không nuốt chửng vạn vật và táp hàm kết liễu.
 */
public class BeelzebuthAbility {

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
     * Kích hoạt Bạo Thực Vương: Triệu hồi Đầu Rồng 3D Voxel lao vút về phía trước
     */
    public static void executeBeelzebuth(ServerLevel level, ServerPlayer player) {
        // Âm thanh Đầu Rồng Hư Không Bạo Thực Vương gầm thét vồ mồi
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.5F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.2F, 0.5F);

        // Gieo xúc xắc xuất lực ngẫu nhiên
        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Bạo Thực Vương: Thôn Phệ (Predator)");

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        double maxDist = 14.0D;

        // Triệu hồi Đầu Rồng Hư Không Bạo Thực Vương (3D Model Display Entity, FIXED orientation)
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            Vec3 spawnPos = start.add(look.scale(2.0D));
            display.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, yaw, pitch);
            display.setYRot(yaw);
            display.setXRot(pitch);

            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.BEELZEBUTH_DRAGON_MAW.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED); // Khối 3D cố định theo góc nhìn
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(0xAA00FF); // Hào quang tím neon ma vương
            displayAcc.weapons$setViewRange(10.0F);

            // Kích thước 3D ban đầu: rộng 3.5m, cao 3.5m, dài 4.2m
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(3.5F, 3.5F, 4.2F),
                    null
            ));

            level.addFreshEntity(display);
            ACTIVE_DRAGONS.add(new ActiveBeelzebuthDragon(level, player, start, look, yaw, pitch, display, 24, roll)); // 1.2 giây
        }

        int devouredEntities = 0;
        int devouredItems = 0;
        int devouredExplosives = 0;
        int devouredBlocks = 0;

        // 1. Tạo Chùm Xoáy Rồng Hư Không Tím dọc đường bay
        for (double d = 1.0D; d <= maxDist; d += 2.0D) {
            Vec3 centerPos = start.add(look.scale(d));
            double coneRadius = 0.5D + (d * 0.18D);

            for (int i = 0; i < 360; i += 120) {
                double rad = Math.toRadians(i + (d * 20.0D));
                double ox = Math.cos(rad) * coneRadius;
                double oy = Math.sin(rad) * coneRadius;

                Vec3 particlePos = centerPos.add(ox, oy, 0);
                level.sendParticles(ParticleTypes.DRAGON_BREATH, particlePos.x, particlePos.y, particlePos.z, 1, 0.02D, 0.02D, 0.02D, 0.01D);

                if (i == 0) {
                    level.sendParticles(ParticleTypes.PORTAL, particlePos.x, particlePos.y, particlePos.z, 1, 0.01D, 0.01D, 0.01D, 0.01D);
                }
            }

            if ((int) d % 6 == 0) {
                level.sendParticles(ParticleTypes.FLASH, centerPos.x, centerPos.y, centerPos.z, 1, 0, 0, 0, 0);
            }
        }

        // 2. Kéo & Nuốt Chửng Sinh Vật Ban Đầu
        Vec3 end = start.add(look.scale(maxDist));
        double maxConeRadius = 0.5D + (maxDist * 0.18D);
        AABB coneBox = new AABB(start, end).inflate(maxConeRadius + 0.5D);
        List<Entity> nearby = level.getEntities((Entity) null, coneBox, e -> e != player && e.isAlive());

        for (Entity entity : nearby) {
            Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5D, 0);
            Vec3 toEntity = entityPos.subtract(start);
            double distAlongLook = toEntity.dot(look);

            if (distAlongLook >= 0 && distAlongLook <= maxDist) {
                double perpDistSq = toEntity.lengthSqr() - (distAlongLook * distAlongLook);
                double coneRadiusAtDist = 0.7D + (distAlongLook * 0.22D);

                if (perpDistSq <= coneRadiusAtDist * coneRadiusAtDist) {
                    // Lực hút vầng xoáy kéo mục tiêu về phía họng rồng
                    Vec3 pullVec = start.subtract(entity.position()).normalize().scale(1.1D);
                    entity.setDeltaMovement(pullVec.x, 0.25D, pullVec.z);
                    entity.hasImpulse = true;

                    if (entity instanceof LivingEntity living) {
                        if (roll.isOverdrive()) {
                            // BẠO KÍCH: Nuốt chửng 100% mọi thực thể
                            level.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                            level.sendParticles(ParticleTypes.DRAGON_BREATH, living.getX(), living.getY() + 1.0D, living.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.03D);
                            TensuraEvents.handleMobDeathDrop(player, living);
                            living.discard();
                            devouredEntities++;
                        } else if (roll.isNormal()) {
                            // XUẤT LỰC CHUẨN: Nuốt chửng <= 60 HP hoặc máu < 50%; Boss cắn rách 250F * multiplier
                            if (living.getHealth() <= 60.0F || living.getHealth() <= living.getMaxHealth() * 0.5F) {
                                level.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                                level.sendParticles(ParticleTypes.DRAGON_BREATH, living.getX(), living.getY() + 1.0D, living.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.03D);
                                TensuraEvents.handleMobDeathDrop(player, living);
                                living.discard();
                                devouredEntities++;
                            } else {
                                living.hurt(level.damageSources().playerAttack(player), 250.0F * roll.multiplier);
                                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 3));
                            }
                        } else {
                            // ĐẦU RA THẤP: Nuốt <= 20 HP, quái to/Boss chỉ bị cắn 60F
                            if (living.getHealth() <= 20.0F) {
                                TensuraEvents.handleMobDeathDrop(player, living);
                                living.discard();
                                devouredEntities++;
                            } else {
                                living.hurt(level.damageSources().playerAttack(player), 60.0F * roll.multiplier);
                                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                            }
                        }
                    } else if (entity instanceof ItemEntity itemEntity) {
                        itemEntity.discard();
                        devouredItems++;
                    } else if (entity instanceof PrimedTnt tnt) {
                        tnt.discard();
                        devouredExplosives++;
                    } else if (entity instanceof Creeper creeper) {
                        if (creeper.isIgnited() || creeper.getSwellDir() > 0) {
                            creeper.discard();
                            devouredExplosives++;
                        }
                    }
                }
            }
        }

        // 3. Nuốt chửng Căn Nhà Dân Làng
        BlockPos targetBlockPos = player.blockPosition().relative(player.getDirection(), 3);
        if (isVillagerHouseStructure(level, targetBlockPos)) {
            devouredBlocks = devourHouseBlocks(level, targetBlockPos);
        }

        player.displayClientMessage(
            Component.literal("§d§l[BẠO THỰC VƯƠNG] §fBáo cáo. Đầu Rồng Hư Không 3D đã nuốt chửng " + devouredEntities + " cá thể, "
                    + devouredItems + " vật phẩm, " + devouredExplosives + " đạn nổ và " + devouredBlocks + " khối nhà dân! 🐲🌀"),
            true
        );
    }

    /**
     * Cập nhật Đầu Rồng Hư Không 3D đang lao về phía trước, há ngoác mồm và nuốt chửng vạn vật
     */
    public static void tickDragons(ServerLevel serverLevel) {
        if (ACTIVE_DRAGONS.isEmpty()) return;

        Iterator<ActiveBeelzebuthDragon> it = ACTIVE_DRAGONS.iterator();
        while (it.hasNext()) {
            ActiveBeelzebuthDragon d = it.next();
            if (d.level != serverLevel) continue;

            d.ticksRemaining--;
            int elapsed = d.totalTicks - d.ticksRemaining;
            float progress = (float) elapsed / d.totalTicks;

            // 1. Đầu rồng 3D lao vút về phía trước theo góc nhìn (2m -> 14m)
            double distance = 2.0D + (progress * 12.0D);
            Vec3 currentPos = d.startPos.add(d.lookVec.scale(distance));

            // 2. Kích thước 3D nở to dần theo đà lao (3.5m -> 5.5m), 4 tick cuối miệng táp sập lại
            float baseScale = 3.5F + (progress * 2.0F);
            float scaleX = baseScale;
            float scaleY = baseScale;
            float scaleZ = baseScale * 1.2F;

            if (d.ticksRemaining <= 4) {
                // Cú táp hàm cực đại: Chiều cao Y ép nhanh xuống (miệng cắn sập)
                float snap = Math.max(0.2F, d.ticksRemaining / 4.0F);
                scaleY = baseScale * snap;
            }

            if (d.displayEntity != null && d.displayEntity.isAlive()) {
                d.displayEntity.moveTo(currentPos.x, currentPos.y, currentPos.z, d.yaw, d.pitch);
                d.displayEntity.setYRot(d.yaw);
                d.displayEntity.setXRot(d.pitch);

                ((DisplayAccessor) d.displayEntity).weapons$setTransformation(new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        new Quaternionf(),
                        new Vector3f(scaleX, scaleY, scaleZ),
                        null
                ));
            }

            // 3. Hạt rồng hư không tím cuốn theo phía sau đầu rồng
            d.level.sendParticles(ParticleTypes.DRAGON_BREATH, currentPos.x, currentPos.y, currentPos.z, 3, 0.4D, 0.4D, 0.4D, 0.03D);
            d.level.sendParticles(ParticleTypes.PORTAL, currentPos.x, currentPos.y, currentPos.z, 2, 0.3D, 0.3D, 0.3D, 0.02D);
            if (elapsed % 4 == 0) {
                d.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, currentPos.x, currentPos.y, currentPos.z, 2, 0.2D, 0.2D, 0.2D, 0.02D);
            }

            // 4. Lực hút chân không ma vương kéo sinh vật vào miệng rồng (bán kính 4.5m)
            AABB pullBox = new AABB(currentPos.x - 4.5D, currentPos.y - 3.5D, currentPos.z - 4.5D,
                    currentPos.x + 4.5D, currentPos.y + 4.0D, currentPos.z + 4.5D);
            List<Entity> pullTargets = d.level.getEntities((Entity) null, pullBox, e -> e != d.caster && e.isAlive());
            for (Entity e : pullTargets) {
                Vec3 pull = currentPos.subtract(e.position()).normalize().scale(0.85D);
                e.setDeltaMovement(pull.x, pull.y * 0.4D + 0.15D, pull.z);
                e.hasImpulse = true;

                // Nuốt chửng khi chạm vào khoang miệng rồng (bán kính 3m)
                if (e.position().distanceToSqr(currentPos) <= 9.0D) {
                    if (e instanceof LivingEntity living) {
                        SkillPowerRoll roll = d.powerRoll;
                        if (roll != null && roll.isOverdrive()) {
                            d.level.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                            d.level.sendParticles(ParticleTypes.DRAGON_BREATH, living.getX(), living.getY() + 1.0D, living.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.03D);
                            if (d.caster != null) {
                                TensuraEvents.handleMobDeathDrop(d.caster, living);
                            }
                            living.discard();
                        } else if (roll != null && roll.isNormal()) {
                            if (living.getHealth() <= 60.0F || living.getHealth() <= living.getMaxHealth() * 0.5F) {
                                d.level.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                                d.level.sendParticles(ParticleTypes.DRAGON_BREATH, living.getX(), living.getY() + 1.0D, living.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.03D);
                                if (d.caster != null) {
                                    TensuraEvents.handleMobDeathDrop(d.caster, living);
                                }
                                living.discard();
                            } else {
                                DamageSource dmgSource = (d.caster != null) ? d.level.damageSources().playerAttack(d.caster) : d.level.damageSources().magic();
                                living.hurt(dmgSource, 200.0F * roll.multiplier);
                                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 3));
                                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                            }
                        } else {
                            if (living.getHealth() <= 20.0F) {
                                if (d.caster != null) {
                                    TensuraEvents.handleMobDeathDrop(d.caster, living);
                                }
                                living.discard();
                            } else {
                                DamageSource dmgSource = (d.caster != null) ? d.level.damageSources().playerAttack(d.caster) : d.level.damageSources().magic();
                                living.hurt(dmgSource, 50.0F * (roll != null ? roll.multiplier : 0.35F));
                            }
                        }
                    } else if (e instanceof ItemEntity || e instanceof PrimedTnt || e instanceof Creeper) {
                        e.discard();
                    }
                }
            }

            // 5. Kết thúc cú táp: Âm thanh chấn động hư không & nổ tung sóng xung kích
            if (d.ticksRemaining <= 0) {
                d.level.sendParticles(ParticleTypes.FLASH, currentPos.x, currentPos.y, currentPos.z, 3, 0.4D, 0.4D, 0.4D, 0);
                d.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, currentPos.x, currentPos.y, currentPos.z, 2, 0, 0, 0, 0);
                d.level.sendParticles(ParticleTypes.DRAGON_BREATH, currentPos.x, currentPos.y, currentPos.z, 25, 1.0D, 1.0D, 1.0D, 0.1D);

                d.level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 0.6F);
                d.level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                        SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.7F);
                d.level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.5F, 0.6F);

                if (d.displayEntity != null && d.displayEntity.isAlive()) {
                    d.displayEntity.discard();
                }
                it.remove();
            }
        }
    }

    /**
     * Kỹ Năng Tối Thượng: Bạo Thực Vương Beelzebuth - Phân nhánh 2: Hủ Hóa & Bạo Liệt (Corrosion & Disintegration)
     * Phóng thích chùm hắc hỏa rồng tím ăn mòn cực đại, phân rã và thiêu hủy mọi sinh vật phía trước.
     */
    public static void executeCorrosion(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.8F, 0.6F);

        // Gieo xúc xắc xuất lực ngẫu nhiên
        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Bạo Thực Vương: Hủ Hóa & Bạo Liệt");

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        double maxDist = 14.0D;

        Set<LivingEntity> hitEntities = new HashSet<>();

        // 1. Quét sát thương toàn bộ sinh vật nằm trong luồng hắc hỏa (1 query duy nhất)
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
                        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 300, 5));
                        victim.hurt(level.damageSources().magic(), 1000.0F);
                    } else if (roll.isNormal()) {
                        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200, 3));
                        victim.hurt(level.damageSources().magic(), 200.0F * roll.multiplier);
                    } else {
                        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 100, 1));
                        victim.hurt(level.damageSources().magic(), 50.0F * roll.multiplier);
                    }

                    victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 2));

                    level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 3, 0.2D, 0.4D, 0.2D, 0.05D);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            }
        }

        // 2. Hiệu ứng chùm hắc hỏa rực rỡ mượt mà (step = 2.0m, chỉ ~14 hạt, 0% lag)
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

    /**
     * Kiểm tra có phải căn nhà kín 4 bức tường hay không (Tối ưu quét bán kính 3)
     */
    private static boolean isVillagerHouseStructure(ServerLevel level, BlockPos center) {
        int solidCount = 0;
        int radius = 3;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos p1 = center.offset(x, 0, z);
                BlockPos p2 = center.offset(x, 2, z);
                if (!level.getBlockState(p1).isAir()) solidCount++;
                if (!level.getBlockState(p2).isAir()) solidCount++;
            }
        }
        return solidCount >= 14;
    }

    /**
     * Hấp thụ toàn bộ khối của căn nhà dân làng thành hư vô (Tối ưu bán kính 2: chỉ quét 100 khối, 0% drop FPS)
     */
    private static int devourHouseBlocks(ServerLevel level, BlockPos center) {
        int radius = 2;
        int destroyed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    BlockState state = level.getBlockState(p);

                    if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.BARRIER)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        destroyed++;

                        if (level.random.nextInt(8) == 0) {
                            level.sendParticles(ParticleTypes.DRAGON_BREATH, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 1, 0.05D, 0.05D, 0.05D, 0.01D);
                        }
                    }
                }
            }
        }

        level.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5F, 0.8F);

        return destroyed;
    }
}
