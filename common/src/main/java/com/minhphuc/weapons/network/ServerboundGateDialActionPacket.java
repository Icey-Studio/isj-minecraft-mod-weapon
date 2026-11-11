package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundGateDialActionPacket {

    public static final int ACTION_SELECT_CHANNEL = 0;
    public static final int ACTION_SAVE_WAYPOINT = 1;
    public static final int ACTION_DELETE_WAYPOINT = 2;
    public static final int ACTION_TOGGLE_CAPSULE = 3;
    public static final int ACTION_CLOSE_GATE = 4;

    private final int action;
    private final int slot;
    private final String customName;

    public ServerboundGateDialActionPacket(int action, int slot, String customName) {
        this.action = action;
        this.slot = slot;
        this.customName = customName != null ? customName : "";
    }

    public ServerboundGateDialActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
        this.slot = buf.readInt();
        this.customName = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.action);
        buf.writeInt(this.slot);
        buf.writeUtf(this.customName, 64);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                DimensionalSanctuaryAbility.handleDialAction(sp, this.action, this.slot, this.customName);
                if (this.action == ACTION_DELETE_WAYPOINT) {
                    DimensionalSanctuaryAbility.openGateDialGui(sp);
                }
            }
        });
    }
}
