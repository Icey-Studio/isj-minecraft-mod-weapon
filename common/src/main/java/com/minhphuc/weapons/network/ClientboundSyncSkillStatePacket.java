package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientSkillStateHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.function.Supplier;

public class ClientboundSyncSkillStatePacket {

    private final boolean safeMode;
    private final boolean hasTacticalMark;
    private final boolean hasAnchorMark;
    private final boolean isTrueDemonLord;
    private final boolean hasCreation;
    private final boolean isPrimordial;
    private final int demonTypeOrd;
    private final int lordSelectedIndex;
    private final String currentSkillName;
    private final int selectedAnchorIndex;
    private final List<String> anchorLabels;
    private final List<Integer> favoriteIds;
    private final Map<String, Integer> cooldownTicks;
    private final boolean raphaelAutoBattle;

    public ClientboundSyncSkillStatePacket(
            boolean safeMode,
            boolean hasTacticalMark,
            boolean hasAnchorMark,
            boolean isTrueDemonLord,
            boolean hasCreation,
            boolean isPrimordial,
            int demonTypeOrd,
            int lordSelectedIndex,
            String currentSkillName,
            int selectedAnchorIndex,
            List<String> anchorLabels,
            List<Integer> favoriteIds,
            Map<String, Integer> cooldownTicks,
            boolean raphaelAutoBattle
    ) {
        this.safeMode = safeMode;
        this.hasTacticalMark = hasTacticalMark;
        this.hasAnchorMark = hasAnchorMark;
        this.isTrueDemonLord = isTrueDemonLord;
        this.hasCreation = hasCreation;
        this.isPrimordial = isPrimordial;
        this.demonTypeOrd = demonTypeOrd;
        this.lordSelectedIndex = lordSelectedIndex;
        this.currentSkillName = currentSkillName != null ? currentSkillName : "";
        this.selectedAnchorIndex = selectedAnchorIndex;
        this.anchorLabels = anchorLabels != null ? new ArrayList<>(anchorLabels) : List.of();
        this.favoriteIds = favoriteIds != null ? new ArrayList<>(favoriteIds) : List.of();
        this.cooldownTicks = cooldownTicks != null ? new HashMap<>(cooldownTicks) : Map.of();
        this.raphaelAutoBattle = raphaelAutoBattle;
    }

    public ClientboundSyncSkillStatePacket(FriendlyByteBuf buf) {
        this.safeMode = buf.readBoolean();
        this.hasTacticalMark = buf.readBoolean();
        this.hasAnchorMark = buf.readBoolean();
        this.isTrueDemonLord = buf.readBoolean();
        this.hasCreation = buf.readBoolean();
        this.isPrimordial = buf.readBoolean();
        this.demonTypeOrd = buf.readInt();
        this.lordSelectedIndex = buf.readInt();
        this.currentSkillName = buf.readUtf();
        this.selectedAnchorIndex = buf.readVarInt();

        int anchorCount = buf.readVarInt();
        List<String> anchors = new ArrayList<>(anchorCount);
        for (int i = 0; i < anchorCount; i++) {
            anchors.add(buf.readUtf());
        }
        this.anchorLabels = anchors;

        int favCount = buf.readVarInt();
        List<Integer> favs = new ArrayList<>(favCount);
        for (int i = 0; i < favCount; i++) {
            favs.add(buf.readVarInt());
        }
        this.favoriteIds = favs;

        int cdCount = buf.readVarInt();
        Map<String, Integer> cds = new HashMap<>(cdCount);
        for (int i = 0; i < cdCount; i++) {
            cds.put(buf.readUtf(), buf.readVarInt());
        }
        this.cooldownTicks = cds;
        this.raphaelAutoBattle = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.safeMode);
        buf.writeBoolean(this.hasTacticalMark);
        buf.writeBoolean(this.hasAnchorMark);
        buf.writeBoolean(this.isTrueDemonLord);
        buf.writeBoolean(this.hasCreation);
        buf.writeBoolean(this.isPrimordial);
        buf.writeInt(this.demonTypeOrd);
        buf.writeInt(this.lordSelectedIndex);
        buf.writeUtf(this.currentSkillName);
        buf.writeVarInt(this.selectedAnchorIndex);

        buf.writeVarInt(this.anchorLabels.size());
        for (String label : this.anchorLabels) {
            buf.writeUtf(label);
        }

        buf.writeVarInt(this.favoriteIds.size());
        for (int id : this.favoriteIds) {
            buf.writeVarInt(id);
        }

        buf.writeVarInt(this.cooldownTicks.size());
        for (Map.Entry<String, Integer> entry : this.cooldownTicks.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeBoolean(this.raphaelAutoBattle);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> ClientSkillStateHandler.handleSync(
                this.safeMode,
                this.hasTacticalMark,
                this.hasAnchorMark,
                this.isTrueDemonLord,
                this.hasCreation,
                this.isPrimordial,
                this.demonTypeOrd,
                this.lordSelectedIndex,
                this.currentSkillName,
                this.selectedAnchorIndex,
                this.anchorLabels,
                this.favoriteIds,
                this.cooldownTicks,
                this.raphaelAutoBattle
        ));
    }
}
