package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.gui.ClientGateDialGuiOpener;
import com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundOpenGateDialPacket {

    private final boolean insideSanctuary;
    private final int activeChannel;
    private final boolean hasCapsule;
    private final List<DimensionalSanctuaryAbility.GateWaypoint> waypoints;

    public ClientboundOpenGateDialPacket(boolean insideSanctuary, int activeChannel, boolean hasCapsule,
                                         List<DimensionalSanctuaryAbility.GateWaypoint> waypoints) {
        this.insideSanctuary = insideSanctuary;
        this.activeChannel = activeChannel;
        this.hasCapsule = hasCapsule;
        this.waypoints = waypoints != null ? waypoints : List.of();
    }

    public ClientboundOpenGateDialPacket(FriendlyByteBuf buf) {
        this.insideSanctuary = buf.readBoolean();
        this.activeChannel = buf.readInt();
        this.hasCapsule = buf.readBoolean();
        int size = buf.readVarInt();
        this.waypoints = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int slot = buf.readInt();
            String name = buf.readUtf(64);
            String dim = buf.readUtf(128);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();
            this.waypoints.add(new DimensionalSanctuaryAbility.GateWaypoint(slot, name, dim, x, y, z, yaw, pitch));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.insideSanctuary);
        buf.writeInt(this.activeChannel);
        buf.writeBoolean(this.hasCapsule);
        buf.writeVarInt(this.waypoints.size());
        for (DimensionalSanctuaryAbility.GateWaypoint wp : this.waypoints) {
            buf.writeInt(wp.slot());
            buf.writeUtf(wp.name(), 64);
            buf.writeUtf(wp.dimension(), 128);
            buf.writeDouble(wp.x());
            buf.writeDouble(wp.y());
            buf.writeDouble(wp.z());
            buf.writeFloat(wp.yaw());
            buf.writeFloat(wp.pitch());
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() != null) {
                ClientGateDialGuiOpener.open(this.insideSanctuary, this.activeChannel, this.hasCapsule, this.waypoints);
            }
        });
    }
}
