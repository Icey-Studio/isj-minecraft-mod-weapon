package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientIaiHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/**
 * Gói tin đồng bộ trạng thái Bạt Đao Thuật Vận Tốc Ngược từ Server về Client:
 * - STATE_START: Bắt đầu xuất khiếu, ngưng đọng thời không, kích hoạt Manga Shader
 * - STATE_FINISHER_STARTED: Khởi động đòn kết liễu điện ảnh, tắt Manga Shader để bừng sáng màu sắc
 * - STATE_END: Kết thúc chiêu, dọn sạch trạng thái
 */
public class ClientboundIaiStatePacket {
    public static final int STATE_START = 1;
    public static final int STATE_FINISHER_STARTED = 2;
    public static final int STATE_END = 3;

    private final int state;
    private final int durationTicks;

    public ClientboundIaiStatePacket(int state, int durationTicks) {
        this.state = state;
        this.durationTicks = durationTicks;
    }

    public ClientboundIaiStatePacket(FriendlyByteBuf buf) {
        this.state = buf.readVarInt();
        this.durationTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.state);
        buf.writeVarInt(this.durationTicks);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ClientIaiHandler.handlePacket(this.state, this.durationTicks);
        });
    }
}

