package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.JuraTempestManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundTempestCitadelActionPacket {

    public static final int ACTION_OPEN_GUI = 99;

    private final int action;
    private final int param;

    public ServerboundTempestCitadelActionPacket(int action, int param) {
        this.action = action;
        this.param = param;
    }

    public ServerboundTempestCitadelActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.param = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(param);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (!(ctx.get().getPlayer() instanceof ServerPlayer sp)) return;
            if (action == ACTION_OPEN_GUI) {
                JuraTempestManager.openCitadelGui(sp);
            } else {
                JuraTempestManager.handleAction(sp, action, param);
            }
        });
    }
}
