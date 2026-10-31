package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Gói tin gửi từ Client lên Server mỗi khi người chơi nhấp chuột trong lúc xuất khiếu Bạt Đao Thuật Vận Tốc Ngược
 */
public class ServerboundIaiClickPacket {
    private final Vec3 clickPos;
    private final int targetEntityId;

    public ServerboundIaiClickPacket(Vec3 clickPos, int targetEntityId) {
        this.clickPos = clickPos;
        this.targetEntityId = targetEntityId;
    }

    public ServerboundIaiClickPacket(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        this.clickPos = new Vec3(x, y, z);
        this.targetEntityId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.clickPos.x);
        buf.writeDouble(this.clickPos.y);
        buf.writeDouble(this.clickPos.z);
        buf.writeVarInt(this.targetEntityId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;
            if (com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility.isPlayerInSession(player.getUUID())) {
                com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility.handleClick(player, this.clickPos, this.targetEntityId);
            } else {
                ReverseVelocityIaijutsuAbility.handleClick(player, this.clickPos, this.targetEntityId);
            }
        });
    }
}

