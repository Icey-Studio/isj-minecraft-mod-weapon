package com.minhphuc.weapons.content.infinitygauntlet;

import com.minhphuc.weapons.network.ClientboundOpenSpaceTeleportScreenPacket;
import com.minhphuc.weapons.network.ModMessages;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import java.util.List;

public class SpaceStoneAbility {

    public static void executeSpaceStone(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        // Mở Menu Dịch Chuyển Đa Chiều & Địa Danh
        executeOpenTeleportMenu(level, player, gauntlet);
    }

    private static void executeOpenTeleportMenu(ServerLevel level, ServerPlayer player, ItemStack gauntlet) {
        ModMessages.sendToPlayer(new ClientboundOpenSpaceTeleportScreenPacket(), player);
    }
}
