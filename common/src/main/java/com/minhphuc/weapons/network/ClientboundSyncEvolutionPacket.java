package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundSyncEvolutionPacket {

    private final List<String> consumedSkills;
    private final int evolvedSkillId;
    private final int evolvedSkillTier;
    private final int[] tiers;

    public ClientboundSyncEvolutionPacket(List<String> consumedSkills, int evolvedSkillId, int evolvedSkillTier, int[] tiers) {
        this.consumedSkills = consumedSkills != null ? consumedSkills : new ArrayList<>();
        this.evolvedSkillId = evolvedSkillId;
        this.evolvedSkillTier = evolvedSkillTier;
        this.tiers = tiers != null ? tiers : new int[4];
    }

    public ClientboundSyncEvolutionPacket(List<String> consumedSkills, int evolvedSkillId, int evolvedSkillTier) {
        this(consumedSkills, evolvedSkillId, evolvedSkillTier, new int[4]);
    }

    public ClientboundSyncEvolutionPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.consumedSkills = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.consumedSkills.add(buf.readUtf());
        }
        this.evolvedSkillId = buf.readVarInt();
        this.evolvedSkillTier = buf.readVarInt();
        this.tiers = new int[4];
        for (int i = 0; i < 4; i++) {
            this.tiers[i] = buf.readVarInt();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.consumedSkills.size());
        for (String s : this.consumedSkills) {
            buf.writeUtf(s);
        }
        buf.writeVarInt(this.evolvedSkillId);
        buf.writeVarInt(this.evolvedSkillTier);
        for (int i = 0; i < 4; i++) {
            buf.writeVarInt(this.tiers[i]);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            EvolvedSkillHelper.setClientState(this.consumedSkills, this.evolvedSkillId, this.evolvedSkillTier, this.tiers);
            if (context.getPlayer() != null && (this.consumedSkills == null || this.consumedSkills.isEmpty())) {
                EvolvedSkillHelper.restoreAllConsumedSkills(context.getPlayer());
            }
        });
    }
}
