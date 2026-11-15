package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.SageManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundSageActionPacket {

    public static final int ACTION_WARP_VELDORA = 1;
    public static final int ACTION_TOGGLE_AUTO_BATTLE = 2;
    public static final int ACTION_SCAN_AREA = 3;
    public static final int ACTION_DECODE_CURSE = 4;

    private final int action;

    public ServerboundSageActionPacket(int action) {
        this.action = action;
    }

    public ServerboundSageActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(action);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (ctx.get().getPlayer() instanceof ServerPlayer sp) {
                SageManager.handleAction(sp, action);
            }
        });
    }
}
