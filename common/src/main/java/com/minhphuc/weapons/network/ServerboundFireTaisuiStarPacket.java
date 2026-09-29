package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundFireTaisuiStarPacket {

    public ServerboundFireTaisuiStarPacket() {
    }

    public ServerboundFireTaisuiStarPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            if (player.level() instanceof ServerLevel serverLevel) {
                if (com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.checkEmptyHandPunch(player)) {
                    return;
                }
                if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.checkEmptyHandPunch(player)) {
                    return;
                }
                if (com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.checkEmptyHandPunch(player)) {
                    return;
                }
                if (com.minhphuc.weapons.content.divine.PurificationPillarAbility.checkEmptyHandPunch(player)) {
                    return;
                }
                TaisuiExtinctionStarsAbility.fireStar(serverLevel, player);
            }
        });
    }
}
