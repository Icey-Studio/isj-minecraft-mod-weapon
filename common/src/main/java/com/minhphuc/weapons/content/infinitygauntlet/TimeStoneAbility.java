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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TimeStoneAbility {

    public static void executeTimeStone(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        // Kỹ Năng: Lão Hóa Quái Vật & Thúc Đẩy Mùa Màng Sinh Trưởng
        executeAgeDecay(level, player, gauntlet);
    }

    /**
     * Chế độ: 🌿 Age Decay & Growth - Tua thời gian: Lão hóa quái vật & Thúc đẩy vạn vật sinh trưởng
     */
    private static void executeAgeDecay(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        BlockPos center = player.blockPosition();
        int radius = 10;

        // Lão hóa quái vật
        AABB area = new AABB(center).inflate(radius);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, Mob::isAlive);

        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, -255, false, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 255, false, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 4, false, false, true));
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 6, 0.3D, 0.5D, 0.3D, 0.05D);
        }

        // Thúc đẩy cây trồng & cây cối sinh trưởng 100% (bán kính 10 blocks)
        int grownCrops = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = center.offset(x, y, z);
                    if (center.distSqr(targetPos) <= radius * radius) {
                        BlockState state = level.getBlockState(targetPos);
                        if (state.getBlock() instanceof CropBlock crop) {
                            level.setBlock(targetPos, crop.getStateForAge(crop.getMaxAge()), 2);
                            grownCrops++;
                        } else if (state.getBlock() instanceof SaplingBlock sapling) {
                            sapling.advanceTree(level, targetPos, state, level.random);
                            grownCrops++;
                        }
                    }
                }
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.COMPOSTER_READY, SoundSource.PLAYERS, 1.5F, 1.0F);

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 1.5D, 1.0D, 1.5D, 0.05D);

        player.displayClientMessage(
            Component.literal("§a§l[ĐÁ THỜI GIAN - AGE DECAY & GROWTH] §fBáo cáo. Đã tua thời gian: Lão hóa " + mobs.size() + " cá thể quái vật & thúc đẩy " + grownCrops + " cây trồng sinh trưởng! 🌿"),
            true
        );

        player.getCooldowns().addCooldown(gauntlet.getItem(), 30);
    }
}
