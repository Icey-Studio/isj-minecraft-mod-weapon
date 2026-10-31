package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientRykuzinHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/**
 * Gói tin đồng bộ trạng thái Tiên Tri (Rykuzin Sinka) từ server về client.
 * Cực nhẹ (chỉ truyền 1 boolean duy nhất, 0 lag mạng).
 */
public class ClientboundSyncRykuzinStatePacket {
    private final boolean active;

    public ClientboundSyncRykuzinStatePacket(boolean active) {
        this.active = active;
    }

    public ClientboundSyncRykuzinStatePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ClientRykuzinHandler.setActive(this.active);
        });
    }
}

