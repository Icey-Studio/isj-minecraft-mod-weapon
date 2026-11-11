package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundInteractGatePacket {

    private final boolean shiftDown;

    public ServerboundInteractGatePacket(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public ServerboundInteractGatePacket(FriendlyByteBuf buf) {
        this.shiftDown = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.shiftDown);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                DimensionalSanctuaryAbility.tryInteractWithGate(sp, this.shiftDown);
            }
        });
    }
}
