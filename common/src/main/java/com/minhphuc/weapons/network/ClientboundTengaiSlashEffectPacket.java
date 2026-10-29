package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientTengaiMangaHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/**
 * Gói tin gửi từ Server về Client khi Nghịch Thời Nhất Trảm được kích hoạt:
 * - durationTicks: Số tick thế giới chuyển sang đen trắng (16 ticks = 0.8s)
 */
public class ClientboundTengaiSlashEffectPacket {
    private final int durationTicks;

    public ClientboundTengaiSlashEffectPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public ClientboundTengaiSlashEffectPacket(FriendlyByteBuf buf) {
        this.durationTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.durationTicks);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ClientTengaiMangaHandler.triggerMangaEffect(this.durationTicks);
        });
    }
}

