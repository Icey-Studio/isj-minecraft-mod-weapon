package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.gui.RaphaelSkillScreen;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class ClientboundOpenRaphaelPacket {

    private final boolean autoBattle;

    public ClientboundOpenRaphaelPacket(boolean autoBattle) {
        this.autoBattle = autoBattle;
    }

    public ClientboundOpenRaphaelPacket(FriendlyByteBuf buf) {
        this.autoBattle = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoBattle);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> Minecraft.getInstance().setScreen(new RaphaelSkillScreen(autoBattle)));
    }
}
