package com.minhphuc.weapons.content.infinitygauntlet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SoulStoneAbility {

    public static void executeSoulStone(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        if (player.isShiftKeyDown()) {
            // Shift + Chuột phải: Tách Linh Hồn Mục Tiêu
            executeSoulExtraction(level, player, gauntlet);
        } else {
            // Chuột phải: Gặt Hái Linh Hồn
            executeSoulHarvest(level, player, gauntlet);
        }
    }


    /**
     * Chế độ 1: 🔥 Soul Harvest - Gặt hái linh hồn quái vật, hồi 100% máu & ban giáp ảo tối thượng
     */
    private static void executeSoulHarvest(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        BlockPos center = player.blockPosition();
        int radius = 15;

        AABB area = new AABB(center).inflate(radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

        int harvestedCount = 0;
        for (LivingEntity entity : entities) {
            com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(player, entity);
            entity.hurt(level.damageSources().playerAttack(player), 100000.0F);
            if (entity.isAlive()) {
                entity.discard();
            }
            harvestedCount++;

            // Hạt linh hồn cam bay về hướng người chơi
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 15, 0.3D, 0.5D, 0.3D, 0.05D);
            level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 10, 0.2D, 0.4D, 0.2D, 0.03D);
        }

        // Hồi 100% máu & cộng giáp ảo tối thượng + sức mạnh X
        player.setHealth(player.getMaxHealth());
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 9, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 9, false, false, true));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8F, 1.2F);

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 1.5D, 1.0D, 1.5D, 0.05D);

        player.displayClientMessage(
            Component.literal("§6§l[ĐÁ LINH HỒN - SOUL HARVEST] §fBáo cáo. Đã gặt hái linh hồn của " + harvestedCount + " cá thể, hồi 100% máu & ban giáp ảo tối thượng! 🔥"),
            true
        );

        player.getCooldowns().addCooldown(gauntlet.getItem(), 30);
    }

    /**
     * Chế độ 2: 👻 Soul Puppet - Chiêu hồn quái vật thành Binh Đoàn Hồn Ma Phụ Tá
     */
    private static void executeSoulPuppet(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        BlockPos center = player.blockPosition();
        int radius = 20;

        AABB area = new AABB(center).inflate(radius);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, Mob::isAlive);

        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false, true));

            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }

            List<Mob> enemies = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(15.0D), e -> e != mob && e.isAlive());
            if (!enemies.isEmpty()) {
                mob.setTarget(enemies.get(0));
            }

            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 10, 0.3D, 0.5D, 0.3D, 0.05D);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_SAND_BREAK, SoundSource.PLAYERS, 1.5F, 1.0F);

        player.displayClientMessage(
            Component.literal("§6§l[ĐÁ LINH HỒN - SOUL PUPPET] §fBáo cáo. Đã chiêu hồn " + mobs.size() + " cá thể thành Binh Đoàn Hồn Ma Phụ Tá! 👻"),
            true
        );

        player.getCooldowns().addCooldown(gauntlet.getItem(), 30);
    }

    /**
     * Chế độ 3: 💥 Soul Extraction - Nhắm mục tiêu & Tách linh hồn thiêu rụi mục tiêu tức thì
     */
    private static void executeSoulExtraction(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(30.0D));

        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(3.0D),
            e -> e != player && e.isAlive()
        );

        if (targets.isEmpty()) {
            player.displayClientMessage(
                Component.literal("§6[ĐÁ LINH HỒN - SOUL EXTRACTION] Báo cáo. Không tìm thấy mục tiêu cá thể phía trước!"),
                true
            );
            return;
        }

        LivingEntity target = targets.get(0);
        com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(player, target);
        target.hurt(level.damageSources().playerAttack(player), 100000.0F);
        if (target.isAlive()) {
            target.discard();
        }

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0D, target.getZ(), 50, 0.5D, 0.8D, 0.5D, 0.1D);
        level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0D, target.getZ(), 1, 0, 0, 0, 0);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0F, 1.2F);

        player.displayClientMessage(
            Component.literal("§6§l[ĐÁ LINH HỒN - SOUL EXTRACTION] §fBáo cáo. Đã tách và thiêu rụi linh hồn của cá thể " + target.getName().getString() + "! 💥"),
            true
        );

        player.getCooldowns().addCooldown(gauntlet.getItem(), 20);
    }
}
