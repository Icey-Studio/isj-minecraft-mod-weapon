package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.SageHudManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class ClientboundSageAlertPacket {

    private final String speaker;
    private final String message;
    private final int accentColor;
    private final int durationMs;
    private final boolean offerVeldoraWarp;
    private final int actionId;

    public ClientboundSageAlertPacket(String speaker, String message, int accentColor, int durationMs, boolean offerVeldoraWarp) {
        this(speaker, message, accentColor, durationMs, offerVeldoraWarp, offerVeldoraWarp ? ServerboundSageActionPacket.ACTION_WARP_VELDORA : 0);
    }

    public ClientboundSageAlertPacket(String speaker, String message, int accentColor, int durationMs, boolean offerVeldoraWarp, int actionId) {
        this.speaker = speaker;
        this.message = message;
        this.accentColor = accentColor;
        this.durationMs = durationMs;
        this.offerVeldoraWarp = offerVeldoraWarp;
        this.actionId = actionId;
    }

    public ClientboundSageAlertPacket(FriendlyByteBuf buf) {
        this.speaker = buf.readUtf(128);
        this.message = buf.readUtf(512);
        this.accentColor = buf.readInt();
        this.durationMs = buf.readInt();
        this.offerVeldoraWarp = buf.readBoolean();
        this.actionId = buf.readableBytes() >= 4 ? buf.readInt() : (offerVeldoraWarp ? ServerboundSageActionPacket.ACTION_WARP_VELDORA : 0);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(speaker, 128);
        buf.writeUtf(message, 512);
        buf.writeInt(accentColor);
        buf.writeInt(durationMs);
        buf.writeBoolean(offerVeldoraWarp);
        buf.writeInt(actionId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> SageHudManager.pushAlert(speaker, message, accentColor, durationMs, offerVeldoraWarp, actionId));
    }
}
