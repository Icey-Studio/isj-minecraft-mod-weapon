package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundNihilityCinematicPacket {
    private final UUID casterUuid;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final int totalTicks;

    public ClientboundNihilityCinematicPacket(UUID casterUuid, Vec3 center, int totalTicks) {
        this.casterUuid = casterUuid;
        this.centerX = center.x;
        this.centerY = center.y;
        this.centerZ = center.z;
        this.totalTicks = totalTicks;
    }

    public ClientboundNihilityCinematicPacket(FriendlyByteBuf buf) {
        this.casterUuid = buf.readUUID();
        this.centerX = buf.readDouble();
        this.centerY = buf.readDouble();
        this.centerZ = buf.readDouble();
        this.totalTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.casterUuid);
        buf.writeDouble(this.centerX);
        buf.writeDouble(this.centerY);
        buf.writeDouble(this.centerZ);
        buf.writeInt(this.totalTicks);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ClientNihilityCinematicHandler.startCinematic(
                    this.casterUuid,
                    new Vec3(this.centerX, this.centerY, this.centerZ),
                    this.totalTicks
            );
        });
    }
}
