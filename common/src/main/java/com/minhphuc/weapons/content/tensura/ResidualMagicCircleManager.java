package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ResidualMagicCircleManager {

    private static final Set<Display.ItemDisplay> ACTIVE_CIRCLES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void registerCircle(Display.ItemDisplay display) {
        if (display != null) {
            ACTIVE_CIRCLES.add(display);
        }
    }

    public static void removeCircle(Display.ItemDisplay display) {
        if (display != null) {
            ACTIVE_CIRCLES.remove(display);
        }
    }

    public static boolean isMagicCircleItem(Item item) {
        return item == ModItems.MAGIC_CIRCLE_NOIR.get() ||
               item == ModItems.MAGIC_CIRCLE_ROUGE.get() ||
               item == ModItems.MAGIC_CIRCLE_BLANC.get() ||
               item == ModItems.MAGIC_CIRCLE_JAUNE.get() ||
               item == ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get() ||
               item == ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get() ||
               item == ModItems.MAGIC_CIRCLE_VIOLET.get() ||
               item == ModItems.MAGIC_CIRCLE_BLEU.get() ||
               item == ModItems.MAGIC_CIRCLE_VERT.get() ||
               item == ModItems.DEMON_SUMMONING_CIRCLE.get() ||
               item == ModItems.DISINTEGRATION_MAGIC_CIRCLE.get() ||
               item == ModItems.BEELZEBUTH_MAGIC_CIRCLE.get();
    }

    public static boolean isResidualMagicCircle(Display.ItemDisplay display) {
        if (!display.isAlive()) return false;
        if (display.getTags().contains("DemonMagicCircle") || display.getTags().contains("DemonResidualCircle")) {
            return true;
        }
        ItemStack stack = ((ItemDisplayAccessor) display).weapons$getItemStack();
        return !stack.isEmpty() && isMagicCircleItem(stack.getItem());
    }

    public static void tickResidualCircles(ServerLevel level) {
        if (ACTIVE_CIRCLES.isEmpty()) return;
        // Chạy mỗi 4 ticks để tối ưu hiệu năng
        if (level.getGameTime() % 4 != 0) return;

        Iterator<Display.ItemDisplay> it = ACTIVE_CIRCLES.iterator();
        while (it.hasNext()) {
            Display.ItemDisplay circle = it.next();
            if (circle == null || !circle.isAlive() || circle.level() != level) {
                if (circle != null && !circle.isAlive()) {
                    it.remove();
                }
                continue;
            }
            Vec3 cPos = circle.position();
            double triggerRadius = 2.4D;

            // Kiểm tra các sinh vật bước vào phạm vi ma pháp trận
            List<LivingEntity> steppers = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(cPos.x - triggerRadius, cPos.y - 1.2D, cPos.z - triggerRadius,
                             cPos.x + triggerRadius, cPos.y + 2.2D, cPos.z + triggerRadius),
                    e -> e.isAlive() && !(e instanceof Player p && (p.isSpectator() || p.isCreative()))
            );

            if (!steppers.isEmpty()) {
                LivingEntity victim = steppers.get(0);
                triggerTrap(level, circle, cPos, victim);
                circle.discard();
                it.remove();
            } else {
                // Hiệu ứng hạt nhấp nháy báo hiệu bẫy ma thuật còn hoạt động
                if (level.random.nextFloat() < 0.15F) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cPos.x, cPos.y + 0.1D, cPos.z, 2, 0.8D, 0.1D, 0.8D, 0.01D);
                    level.sendParticles(ParticleTypes.PORTAL, cPos.x, cPos.y + 0.1D, cPos.z, 2, 0.6D, 0.1D, 0.6D, 0.02D);
                }
            }
        }
    }

    private static void triggerTrap(ServerLevel level, Display.ItemDisplay circle, Vec3 pos, LivingEntity victim) {
        float roll = level.random.nextFloat(); // 0.0 -> 1.0

        if (roll < 0.80F) {
            // ==========================================
            // 80%: PHÁT NỔ BỘC PHÁ (EXPLOSION)
            // ==========================================
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 0.5D, pos.z, 2, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5D, pos.z, 35, 0.6D, 0.6D, 0.6D, 0.1D);
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 0.8D, pos.z, 2, 0, 0, 0, 0);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.9F);

            List<LivingEntity> inBlast = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(pos.x - 4.5D, pos.y - 2.0D, pos.z - 4.5D, pos.x + 4.5D, pos.y + 4.0D, pos.z + 4.5D),
                    LivingEntity::isAlive
            );
            for (LivingEntity e : inBlast) {
                if (PrimordialDemonEntity.isTargetImmune(e)) continue;
                e.hurt(level.damageSources().explosion(null, null), 120.0F);
                Vec3 knock = e.position().subtract(pos).normalize().scale(1.2D);
                e.setDeltaMovement(knock.x, 0.4D, knock.z);
                e.hasImpulse = true;
            }

            for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(pos) <= 32.0D * 32.0D)) {
                sp.displayClientMessage(Component.literal("§c§l💥 [BẪY MA TRẬN] §eMa pháp trận tàn dư đã phát nổ dữ dội!"), true);
            }

        } else if (roll < 0.85F) {
            // ==========================================
            // 5%: TRÚNG ĐỘC MA GIỚI (POISON)
            // ==========================================
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.6D, pos.z, 60, 1.2D, 0.8D, 1.2D, 0.05D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 0.6D, pos.z, 40, 1.0D, 0.6D, 1.0D, 0.03D);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 2.5F, 0.8F);

            List<LivingEntity> inPoison = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(pos.x - 3.5D, pos.y - 1.5D, pos.z - 3.5D, pos.x + 3.5D, pos.y + 3.0D, pos.z + 3.5D),
                    LivingEntity::isAlive
            );
            for (LivingEntity e : inPoison) {
                if (PrimordialDemonEntity.isTargetImmune(e)) continue;
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                e.hurt(level.damageSources().magic(), 45.0F);
            }

            for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(pos) <= 32.0D * 32.0D)) {
                sp.displayClientMessage(Component.literal("§5§l☠ [BẪY MA TRẬN] §dTử Độc Ma Giới đã kích hoạt từ tàn trận!"), true);
            }

        } else {
            // ==========================================
            // 15%: NGẪU NHIÊN 1 TRONG 2 SKILL TỐI THƯỢNG
            // ==========================================
            boolean useJacob = level.random.nextBoolean();

            if (useJacob) {
                // 1. TÀ KHỨ VŨ THÊ TỬ (JACOB'S LADDER)
                level.playSound(null, pos.x, pos.y + 4.0D, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.3F);
                level.playSound(null, pos.x, pos.y + 4.0D, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 1.1F);
                level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 2.0D, pos.z, 4, 0, 0, 0, 0);

                // Quang trụ 7 sắc 40m vươn cao
                for (double y = 0.0D; y <= 40.0D; y += 1.0D) {
                    for (int c = 0; c < 7; c++) {
                        double angle = (c * (2 * Math.PI / 7)) + (y * 0.4D);
                        double px = pos.x + Math.cos(angle) * 2.5D;
                        double pz = pos.z + Math.sin(angle) * 2.5D;
                        switch (c) {
                            case 0 -> level.sendParticles(ParticleTypes.FLAME, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                            case 1 -> level.sendParticles(ParticleTypes.END_ROD, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                            case 2 -> level.sendParticles(ParticleTypes.CRIT, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                            case 3 -> level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                            case 4 -> level.sendParticles(ParticleTypes.SNOWFLAKE, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                            case 5 -> level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                            case 6 -> level.sendParticles(ParticleTypes.WITCH, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                        }
                    }
                }

                List<LivingEntity> inPillar = level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos.x - 6.0D, pos.y - 2.0D, pos.z - 6.0D, pos.x + 6.0D, pos.y + 25.0D, pos.z + 6.0D),
                        LivingEntity::isAlive
                );
                for (LivingEntity e : inPillar) {
                    if (PrimordialDemonEntity.isTargetImmune(e)) continue;
                    e.hurt(level.damageSources().magic(), 300.0F);
                    e.setDeltaMovement(0, 1.4D, 0);
                    e.hasImpulse = true;
                }

                for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(pos) <= 48.0D * 48.0D)) {
                    sp.displayClientMessage(Component.literal("§b§l✦ [BẪY MA TRẬN] §eKích hoạt Bí Thuật Thần Thánh: §6§lTà Khứ Vũ Thê Tử (Jacob's Ladder)!"), true);
                }
            } else {
                // 2. LINH TỬ BĂNG HOẠI (DISINTEGRATION)
                level.playSound(null, pos.x, pos.y + 2.0D, pos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.2F);
                level.playSound(null, pos.x, pos.y + 2.0D, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.6F);
                level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 2.0D, pos.z, 5, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.0D, pos.z, 2, 0, 0, 0, 0);

                // Chùm tia phân rã linh tử trắng chói lòa
                for (double y = 0.0D; y <= 25.0D; y += 0.5D) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (i * Math.PI / 4.0D) + (y * 0.3D);
                        double px = pos.x + Math.cos(angle) * 1.8D;
                        double pz = pos.z + Math.sin(angle) * 1.8D;
                        level.sendParticles(ParticleTypes.END_ROD, px, pos.y + y, pz, 1, 0, 0, 0, 0.02D);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, pos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    }
                }

                List<LivingEntity> inDisint = level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos.x - 7.0D, pos.y - 2.0D, pos.z - 7.0D, pos.x + 7.0D, pos.y + 20.0D, pos.z + 7.0D),
                        LivingEntity::isAlive
                );
                for (LivingEntity e : inDisint) {
                    if (PrimordialDemonEntity.isTargetImmune(e)) continue;
                    e.hurt(level.damageSources().magic(), 360.0F);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
                }

                for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(pos) <= 48.0D * 48.0D)) {
                    sp.displayClientMessage(Component.literal("§e§l✦ [BẪY MA TRẬN] §dKích hoạt Cấm Thuật Phân Rã: §f§lLinh Tử Băng Hoại (Disintegration)!"), true);
                }
            }
        }
    }
}
