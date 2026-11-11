package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientPossessionHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundSyncPossessionPacket {

    private final UUID playerUuid;
    private final boolean isPossessing;
    private final int demonTypeOrdinal;
    private final boolean hasBody;
    private final boolean isNamed;
    private final String customName;
    private final int tier;
    private final boolean isWinged;
    private final float currentHp;
    private final float maxHp;
    private final float attackDamage;
    private final int selectedSkillIndex;
    private final String skillsString;

    public ClientboundSyncPossessionPacket(UUID playerUuid, boolean isPossessing, int demonTypeOrdinal,
                                           boolean hasBody, boolean isNamed, String customName,
                                           int tier, boolean isWinged, float currentHp, float maxHp,
                                           float attackDamage, int selectedSkillIndex, String skillsString) {
        this.playerUuid = playerUuid;
        this.isPossessing = isPossessing;
        this.demonTypeOrdinal = demonTypeOrdinal;
        this.hasBody = hasBody;
        this.isNamed = isNamed;
        this.customName = customName != null ? customName : "";
        this.tier = tier;
        this.isWinged = isWinged;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.attackDamage = attackDamage;
        this.selectedSkillIndex = selectedSkillIndex;
        this.skillsString = skillsString != null ? skillsString : "";
    }

    public ClientboundSyncPossessionPacket(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.isPossessing = buf.readBoolean();
        this.demonTypeOrdinal = buf.readInt();
        this.hasBody = buf.readBoolean();
        this.isNamed = buf.readBoolean();
        this.customName = buf.readUtf();
        this.tier = buf.readInt();
        this.isWinged = buf.readBoolean();
        this.currentHp = buf.readFloat();
        this.maxHp = buf.readFloat();
        this.attackDamage = buf.readFloat();
        this.selectedSkillIndex = buf.readInt();
        this.skillsString = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerUuid);
        buf.writeBoolean(this.isPossessing);
        buf.writeInt(this.demonTypeOrdinal);
        buf.writeBoolean(this.hasBody);
        buf.writeBoolean(this.isNamed);
        buf.writeUtf(this.customName);
        buf.writeInt(this.tier);
        buf.writeBoolean(this.isWinged);
        buf.writeFloat(this.currentHp);
        buf.writeFloat(this.maxHp);
        buf.writeFloat(this.attackDamage);
        buf.writeInt(this.selectedSkillIndex);
        buf.writeUtf(this.skillsString);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> ClientPossessionHandler.handlePacket(
                this.playerUuid, this.isPossessing, this.demonTypeOrdinal, this.hasBody, this.isNamed,
                this.customName, this.tier, this.isWinged, this.currentHp, this.maxHp, this.attackDamage,
                this.selectedSkillIndex, this.skillsString
        ));
    }
}
