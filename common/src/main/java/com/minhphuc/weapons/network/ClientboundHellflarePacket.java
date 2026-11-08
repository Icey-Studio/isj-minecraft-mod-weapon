package com.minhphuc.weapons.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundHellflarePacket {
    public static final int PHASE_START = 0;       // Khởi đầu bay lên, tụ hắc hỏa và phóng cầu lửa đen
    public static final int PHASE_EXPAND = 1;      // Điểm va chạm tạo lập và bành trướng Hắc Viêm Ngục khổng lồ
    public static final int PHASE_DISMISS = 2;     // Giải trừ / Sụp đổ chân không ma pháp (Phong Sát Kĩ Năng)

    private final UUID casterUuid;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float radius;
    private final int phase;

    public ClientboundHellflarePacket(UUID casterUuid, Vec3 startPos, Vec3 targetPos, float radius, int phase) {
        this.casterUuid = casterUuid;
        this.startX = startPos.x;
        this.startY = startPos.y;
        this.startZ = startPos.z;
        this.targetX = targetPos.x;
        this.targetY = targetPos.y;
        this.targetZ = targetPos.z;
        this.radius = radius;
        this.phase = phase;
    }

    public ClientboundHellflarePacket(FriendlyByteBuf buf) {
        this.casterUuid = buf.readUUID();
        this.startX = buf.readDouble();
        this.startY = buf.readDouble();
        this.startZ = buf.readDouble();
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
        this.radius = buf.readFloat();
        this.phase = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.casterUuid);
        buf.writeDouble(this.startX);
        buf.writeDouble(this.startY);
        buf.writeDouble(this.startZ);
        buf.writeDouble(this.targetX);
        buf.writeDouble(this.targetY);
        buf.writeDouble(this.targetZ);
        buf.writeFloat(this.radius);
        buf.writeInt(this.phase);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Vec3 startPos = new Vec3(this.startX, this.startY, this.startZ);
            Vec3 targetPos = new Vec3(this.targetX, this.targetY, this.targetZ);

            if (this.phase == PHASE_START) {
                com.minhphuc.weapons.client.renderer.ClientHellflareRenderer.start(this.casterUuid, startPos, targetPos, this.radius);
                com.minhphuc.weapons.client.ClientHellflareCinematicHandler.startCinematic(this.casterUuid, startPos, targetPos);
            } else if (this.phase == PHASE_EXPAND) {
                com.minhphuc.weapons.client.renderer.ClientHellflareRenderer.triggerExpand(this.casterUuid, targetPos, this.radius);
            } else if (this.phase == PHASE_DISMISS) {
                com.minhphuc.weapons.client.renderer.ClientHellflareRenderer.dismiss(targetPos);
                com.minhphuc.weapons.client.ClientHellflareCinematicHandler.stopCinematic();
            }
        });
    }
}
