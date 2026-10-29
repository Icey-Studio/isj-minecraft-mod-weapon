package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Gói tin gửi từ Client lên Server để thi triển Kiếm Kỹ Thiên Ngoại Cứu Thế:
 * - action = 1: Nghịch Thời Nhất Trảm
 */
public class ServerboundTengaiSwordSkillPacket {
    public static final int ACTION_REVERSE_SLASH = 1;

    private final int action;

    public ServerboundTengaiSwordSkillPacket(int action) {
        this.action = action;
    }

    public ServerboundTengaiSwordSkillPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.action);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;
            ServerLevel level = (ServerLevel) player.level();

            if (this.action == ACTION_REVERSE_SLASH) {
                TengaiKyuseiSwordAbility.castReverseTimeSlash(level, player);
            }
        });
    }
}
