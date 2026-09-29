package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.gui.ClientSkillEvolutionOpener;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundOpenSkillEvolutionScreenPacket {

    private final List<String> consumedSkills;
    private final int evolvedSkillId;
    private final int evolvedSkillTier;

    public ClientboundOpenSkillEvolutionScreenPacket(List<String> consumedSkills, int evolvedSkillId, int evolvedSkillTier) {
        this.consumedSkills = consumedSkills != null ? consumedSkills : new ArrayList<>();
        this.evolvedSkillId = evolvedSkillId;
        this.evolvedSkillTier = evolvedSkillTier;
    }

    public ClientboundOpenSkillEvolutionScreenPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.consumedSkills = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.consumedSkills.add(buf.readUtf());
        }
        this.evolvedSkillId = buf.readVarInt();
        this.evolvedSkillTier = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.consumedSkills.size());
        for (String s : this.consumedSkills) {
            buf.writeUtf(s);
        }
        buf.writeVarInt(this.evolvedSkillId);
        buf.writeVarInt(this.evolvedSkillTier);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            EvolvedSkillHelper.setClientState(this.consumedSkills, this.evolvedSkillId, this.evolvedSkillTier);
            ClientSkillEvolutionOpener.openScreen();
        });
    }
}
