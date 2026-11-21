package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.PrimordialSkillDispatcher;
import com.minhphuc.weapons.content.tensura.TensuraSkillDispatcher;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundCastBeelzebuthPacket {

    public ServerboundCastBeelzebuthPacket() {
    }

    public ServerboundCastBeelzebuthPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            if (com.minhphuc.weapons.content.tensura.PrimordialPossessionManager.isPossessing(player)) {
                com.minhphuc.weapons.content.tensura.PrimordialPossessionManager.castSkill(player);
                com.minhphuc.weapons.content.tensura.TensuraSkillHelper.syncSkillStateToClient(player);
                return;
            }

            boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
            boolean isInnatePrimordial = PrimordialPlayerDataHelper.isInnatePrimordial(player);
            boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
            boolean isWearingDivineArmor = DivineArmorItem.isWearingAnyPiece(player);
            boolean isCreative = player.isCreative();
            boolean hasLordSkills = isTrueDemonLord || isWearingDivineArmor || isInnatePrimordial || isCreative;

            if (hasLordSkills || isPrimordial) {
                int selectedSkill = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
                ServerLevel serverLevel = (ServerLevel) player.level();
                boolean hasCreation = EntityDataHelper.getCustomData(player).getBoolean("TensuraMaterialCreation");
                int lordSkills = hasLordSkills ? TensuraSkillDispatcher.getSkillCount(hasCreation) : 0;
                int totalSkills = lordSkills + (isPrimordial ? PrimordialPlayerDataHelper.TOTAL_PRIMORDIAL_SKILLS : 0);
                if (selectedSkill < 0 || selectedSkill >= totalSkills) {
                    selectedSkill = 0;
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", 0);
                }

                if (selectedSkill < lordSkills) {
                    TensuraSkillDispatcher.executeSkill(serverLevel, player, selectedSkill, hasCreation);
                } else if (isPrimordial) {
                    int primordialSkillIdx = selectedSkill - lordSkills;
                    PrimordialSkillDispatcher.castSkill(serverLevel, player, primordialSkillIdx);
                }
                com.minhphuc.weapons.content.tensura.TensuraSkillHelper.syncSkillStateToClient(player);
            }
        });
    }
}
