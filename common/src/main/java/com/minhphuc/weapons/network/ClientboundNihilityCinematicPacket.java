package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundNihilityCinematicPacket {
    public static final int PHASE_START = 0;
    public static final int PHASE_DETONATE = 1;
    public static final int PHASE_END = 2;

    private final UUID casterUuid;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double targetElevY;
    private final int totalTicks;
    private final int phase;

    public ClientboundNihilityCinematicPacket(UUID casterUuid, Vec3 center, double targetElevY, int totalTicks, int phase) {
        this.casterUuid = casterUuid;
        this.centerX = center.x;
        this.centerY = center.y;
        this.centerZ = center.z;
        this.targetElevY = targetElevY;
        this.totalTicks = totalTicks;
        this.phase = phase;
    }

    public ClientboundNihilityCinematicPacket(UUID casterUuid, Vec3 center, int totalTicks, int phase) {
        this(casterUuid, center, center.y + 28.0D, totalTicks, phase);
    }

    public ClientboundNihilityCinematicPacket(UUID casterUuid, Vec3 center, int totalTicks) {
        this(casterUuid, center, center.y + 28.0D, totalTicks, PHASE_START);
    }

    public ClientboundNihilityCinematicPacket(FriendlyByteBuf buf) {
        this.casterUuid = buf.readUUID();
        this.centerX = buf.readDouble();
        this.centerY = buf.readDouble();
        this.centerZ = buf.readDouble();
        this.targetElevY = buf.readDouble();
        this.totalTicks = buf.readInt();
        this.phase = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.casterUuid);
        buf.writeDouble(this.centerX);
        buf.writeDouble(this.centerY);
        buf.writeDouble(this.centerZ);
        buf.writeDouble(this.targetElevY);
        buf.writeInt(this.totalTicks);
        buf.writeInt(this.phase);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ClientNihilityCinematicHandler.handleCinematicPacket(
                    this.casterUuid,
                    new Vec3(this.centerX, this.centerY, this.centerZ),
                    this.targetElevY,
                    this.totalTicks,
                    this.phase
            );
        });
    }
}
