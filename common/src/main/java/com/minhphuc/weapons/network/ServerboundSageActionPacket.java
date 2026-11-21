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
    public static final int ACTION_REPAIR_GEAR = 5;
    public static final int ACTION_HOOVER_TRASH = 6;
    public static final int ACTION_EMERGENCY_HEAL = 7;
    public static final int ACTION_REVERSE_PROJECTILES = 8;
    public static final int ACTION_OVERDRIVE_SKILL = 9;
    public static final int ACTION_RESET_COOLDOWN = 10;
    public static final int ACTION_ESCAPE_VOID = 11;
    public static final int ACTION_SOLIDIFY_LAVA = 12;
    public static final int ACTION_SWARM_GRAVITY = 13;
    public static final int ACTION_PACIFY_VELDORA = 14;
    public static final int ACTION_EXTRACT_VEIN = 15;
    public static final int ACTION_WATER_BREATHING = 16;
    public static final int ACTION_DISARM_TARGET = 17;

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
