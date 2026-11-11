package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundAzathothPacket {
    public static final int PHASE_START = 0;
    public static final int PHASE_END = 1;

    private final UUID casterUuid;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double dirX;
    private final double dirY;
    private final double dirZ;
    private final int totalTicks;
    private final int phase;

    public ClientboundAzathothPacket(UUID casterUuid, Vec3 startPos, Vec3 dir, int totalTicks, int phase) {
        this.casterUuid = casterUuid;
        this.startX = startPos.x;
        this.startY = startPos.y;
        this.startZ = startPos.z;
        this.dirX = dir.x;
        this.dirY = dir.y;
        this.dirZ = dir.z;
        this.totalTicks = totalTicks;
        this.phase = phase;
    }

    public ClientboundAzathothPacket(UUID casterUuid, Vec3 startPos, Vec3 dir, int totalTicks) {
        this(casterUuid, startPos, dir, totalTicks, PHASE_START);
    }

    public ClientboundAzathothPacket(FriendlyByteBuf buf) {
        this.casterUuid = buf.readUUID();
        this.startX = buf.readDouble();
        this.startY = buf.readDouble();
        this.startZ = buf.readDouble();
        this.dirX = buf.readDouble();
        this.dirY = buf.readDouble();
        this.dirZ = buf.readDouble();
        this.totalTicks = buf.readInt();
        this.phase = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.casterUuid);
        buf.writeDouble(this.startX);
        buf.writeDouble(this.startY);
        buf.writeDouble(this.startZ);
        buf.writeDouble(this.dirX);
        buf.writeDouble(this.dirY);
        buf.writeDouble(this.dirZ);
        buf.writeInt(this.totalTicks);
        buf.writeInt(this.phase);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (this.phase == PHASE_START) {
                com.minhphuc.weapons.client.renderer.ClientAzathothDragonRenderer.receive(
                        this.casterUuid,
                        new Vec3(this.startX, this.startY, this.startZ),
                        new Vec3(this.dirX, this.dirY, this.dirZ),
                        this.totalTicks
                );
                ClientAzathothCinematicHandler.startCinematic(
                        this.casterUuid,
                        new Vec3(this.startX, this.startY, this.startZ),
                        new Vec3(this.dirX, this.dirY, this.dirZ),
                        this.totalTicks
                );
            } else {
                com.minhphuc.weapons.client.renderer.ClientAzathothDragonRenderer.reset();
                ClientAzathothCinematicHandler.stopCinematic();
            }
        });
    }
}
