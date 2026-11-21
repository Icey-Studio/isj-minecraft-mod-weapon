package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.gui.TempestCitadelScreen;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class ClientboundOpenTempestCitadelPacket {

    private final int residentCount;
    private final int blessedCount;
    private final long bonusEp;
    private final boolean autoProduce;
    private final long dayTime;
    private final boolean insideCitadel;

    public ClientboundOpenTempestCitadelPacket(
            int residentCount,
            int blessedCount,
            long bonusEp,
            boolean autoProduce,
            long dayTime,
            boolean insideCitadel
    ) {
        this.residentCount = residentCount;
        this.blessedCount = blessedCount;
        this.bonusEp = bonusEp;
        this.autoProduce = autoProduce;
        this.dayTime = dayTime;
        this.insideCitadel = insideCitadel;
    }

    public ClientboundOpenTempestCitadelPacket(FriendlyByteBuf buf) {
        this.residentCount = buf.readVarInt();
        this.blessedCount = buf.readVarInt();
        this.bonusEp = buf.readVarLong();
        this.autoProduce = buf.readBoolean();
        this.dayTime = buf.readVarLong();
        this.insideCitadel = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(residentCount);
        buf.writeVarInt(blessedCount);
        buf.writeVarLong(bonusEp);
        buf.writeBoolean(autoProduce);
        buf.writeVarLong(dayTime);
        buf.writeBoolean(insideCitadel);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> Minecraft.getInstance().setScreen(new TempestCitadelScreen(
                residentCount, blessedCount, bonusEp, autoProduce, dayTime, insideCitadel
        )));
    }
}
