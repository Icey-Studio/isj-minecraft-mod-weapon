package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager;
import com.minhphuc.weapons.content.tensura.PrimordialSummonRitual;
import com.minhphuc.weapons.entity.tensura.DemonType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class ServerboundSummonDemonPacket {
    private final int demonId;
    private final boolean isWinged;
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    public ServerboundSummonDemonPacket(int demonId, boolean isWinged, double targetX, double targetY, double targetZ) {
        this.demonId = demonId;
        this.isWinged = isWinged;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public ServerboundSummonDemonPacket(FriendlyByteBuf buf) {
        this.demonId = buf.readInt();
        this.isWinged = buf.readBoolean();
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.demonId);
        buf.writeBoolean(this.isWinged);
        buf.writeDouble(this.targetX);
        buf.writeDouble(this.targetY);
        buf.writeDouble(this.targetZ);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            // Kiểm tra khoảng cách an toàn (tối đa 40 block)
            Vec3 target = new Vec3(targetX, targetY, targetZ);
            if (player.distanceToSqr(target) > 40.0D * 40.0D) {
                target = player.position().add(player.getLookAngle().scale(4.0D));
            }

            if (AntiMagicBarrierManager.isInBarrier(player.serverLevel(), target)) {
                player.displayClientMessage(
                        Component.literal("§c⚠️ Không thể triệu hồi Ác Ma bên trong Kháng Ma Kết Giới! Nồng độ ma tử đã bị phong ấn!"),
                        true
                );
                return;
            }

            DemonType demonType = DemonType.byIndex(demonId);
            PrimordialSummonRitual.startWaitingRitual(player.serverLevel(), player, target, demonType, isWinged);
        });
    }
}
