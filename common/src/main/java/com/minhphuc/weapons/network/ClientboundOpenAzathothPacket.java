package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.gui.AzathothStomachScreen;
import com.minhphuc.weapons.content.tensura.AzathothStomachData;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundOpenAzathothPacket {

    private final List<AzathothStomachData.StomachEntry> entries;
    private final boolean veldoraUnlocked;
    private final boolean autoLoot;

    public ClientboundOpenAzathothPacket(List<AzathothStomachData.StomachEntry> entries, boolean veldoraUnlocked, boolean autoLoot) {
        this.entries = entries;
        this.veldoraUnlocked = veldoraUnlocked;
        this.autoLoot = autoLoot;
    }

    public ClientboundOpenAzathothPacket(FriendlyByteBuf buf) {
        this.veldoraUnlocked = buf.readBoolean();
        this.autoLoot = buf.readBoolean();
        int count = buf.readVarInt();
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.entries.add(new AzathothStomachData.StomachEntry(
                    buf.readUtf(128),
                    buf.readUtf(32),
                    buf.readVarLong(),
                    buf.readUtf(128)
            ));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(veldoraUnlocked);
        buf.writeBoolean(autoLoot);
        buf.writeVarInt(entries.size());
        for (var e : entries) {
            buf.writeUtf(e.name(), 128);
            buf.writeUtf(e.kind(), 32);
            buf.writeVarLong(e.epValue());
            buf.writeUtf(e.analyzedSkill(), 128);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> Minecraft.getInstance().setScreen(new AzathothStomachScreen(entries, veldoraUnlocked, autoLoot)));
    }
}
