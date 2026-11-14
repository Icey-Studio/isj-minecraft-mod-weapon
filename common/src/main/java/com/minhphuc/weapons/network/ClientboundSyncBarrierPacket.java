package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.WeatherBarrierTracker;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gói tin đồng bộ trạng thái kết giới (Kháng Ma Kết Giới) từ Server tới Client
 * để Client xử lý thời tiết trong lành, ngăn mưa tuyết và bầu trời quang đãng.
 */
public class ClientboundSyncBarrierPacket {
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_CLEAR = 2;

    private final UUID id;
    private final int action;
    private final double posX, posY, posZ;
    private final double radius;
    private final double height;
    private final double skirtDepth;
    private final boolean isVillager;

    public ClientboundSyncBarrierPacket(UUID id, int action, Vec3 center, double radius, double height, double skirtDepth, boolean isVillager) {
        this.id = id != null ? id : UUID.randomUUID();
        this.action = action;
        this.posX = center != null ? center.x : 0;
        this.posY = center != null ? center.y : 0;
        this.posZ = center != null ? center.z : 0;
        this.radius = radius;
        this.height = height;
        this.skirtDepth = skirtDepth;
        this.isVillager = isVillager;
    }

    public ClientboundSyncBarrierPacket(FriendlyByteBuf buf) {
        this.id = buf.readUUID();
        this.action = buf.readInt();
        this.posX = buf.readDouble();
        this.posY = buf.readDouble();
        this.posZ = buf.readDouble();
        this.radius = buf.readDouble();
        this.height = buf.readDouble();
        this.skirtDepth = buf.readDouble();
        this.isVillager = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.id);
        buf.writeInt(this.action);
        buf.writeDouble(this.posX);
        buf.writeDouble(this.posY);
        buf.writeDouble(this.posZ);
        buf.writeDouble(this.radius);
        buf.writeDouble(this.height);
        buf.writeDouble(this.skirtDepth);
        buf.writeBoolean(this.isVillager);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (this.action == ACTION_ADD) {
                WeatherBarrierTracker.addClientBarrier(this.id, new Vec3(this.posX, this.posY, this.posZ), this.radius, this.height, this.skirtDepth, this.isVillager);
            } else if (this.action == ACTION_REMOVE) {
                WeatherBarrierTracker.removeClientBarrier(this.id);
            } else if (this.action == ACTION_CLEAR) {
                WeatherBarrierTracker.clearClientBarriers();
            }
        });
    }
}
