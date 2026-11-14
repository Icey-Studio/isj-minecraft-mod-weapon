package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.tensura.*;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.Supplier;

public class ServerboundSkillActionPacket {

    public static final int ACTION_REQUEST_SYNC = 0;
    public static final int ACTION_SELECT_SKILL = 1;
    public static final int ACTION_CAST_SKILL = 2;
    public static final int ACTION_TOGGLE_SAFE_MODE = 3;
    public static final int ACTION_TOGGLE_FAVORITE = 4;
    public static final int ACTION_MARK_ANCHOR = 5;
    public static final int ACTION_TELEPORT_SLOT = 6;
    public static final int ACTION_CAST_FAVORITE_SLOT = 7;
    public static final int ACTION_SELECT_ANCHOR = 8;
    public static final int ACTION_REMOVE_ANCHOR = 9;
    public static final int ACTION_SEND_TO_ANCHOR = 10;

    private final int action;
    private final int param;

    public ServerboundSkillActionPacket(int action, int param) {
        this.action = action;
        this.param = param;
    }

    public ServerboundSkillActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.param = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
        buf.writeVarInt(this.param);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            boolean possessing = PrimordialPossessionManager.isPossessing(player);

            switch (this.action) {
                case ACTION_REQUEST_SYNC -> {
                    SpatialTeleportAbility.syncVisualMarksToPlayer(player);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_TOGGLE_SAFE_MODE -> {
                    TensuraSkillHelper.toggleSafeMode(player);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_TOGGLE_FAVORITE -> {
                    TensuraSkillHelper.toggleFavoriteSkill(player, possessing, this.param);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_SELECT_SKILL -> {
                    selectSkillForMode(player, possessing, this.param);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_CAST_SKILL -> {
                    selectSkillForMode(player, possessing, this.param);
                    castCurrentSkillForMode(level, player, possessing);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_MARK_ANCHOR -> {
                    SpatialTeleportAbility.castMark(level, player, true);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_TELEPORT_SLOT -> {
                    SpatialTeleportAbility.castTeleportToSlot(level, player, this.param);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_SELECT_ANCHOR -> {
                    SpatialTeleportAbility.selectAnchorSlot(player, this.param);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_REMOVE_ANCHOR -> {
                    SpatialTeleportAbility.removeAnchorSlot(player, this.param);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_SEND_TO_ANCHOR -> {
                    SpatialTeleportAbility.castSendToAnchor(level, player, this.param);
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
                case ACTION_CAST_FAVORITE_SLOT -> {
                    List<Integer> favs = TensuraSkillHelper.getFavoriteSkills(player, possessing);
                    if (this.param >= 0 && this.param < favs.size()) {
                        int targetSkill = favs.get(this.param);
                        selectSkillForMode(player, possessing, targetSkill);
                        castCurrentSkillForMode(level, player, possessing);
                    } else {
                        player.displayClientMessage(Component.literal("§7Ô kỹ năng nhanh này chưa được gán! Bấm §e[G] §7để gán kỹ năng yêu thích."), true);
                    }
                    TensuraSkillHelper.syncSkillStateToClient(player);
                }
            }
        });
    }

    private static void selectSkillForMode(ServerPlayer player, boolean possessing, int skillId) {
        if (possessing) {
            PrimordialPossessionManager.selectSkillById(player, skillId);
            return;
        }
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        boolean hasCreation = EntityDataHelper.getCustomData(player).getBoolean("TensuraMaterialCreation");
        boolean isWearingDivine = DivineArmorItem.isWearingAnyPiece(player);
        boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
        if (!(isTrueDemonLord || isWearingDivine || isPrimordial || player.isCreative())) return;

        int lordSkills = TensuraSkillDispatcher.getSkillCount(hasCreation);
        int totalSkills = lordSkills + (isPrimordial ? 7 : 0);
        if (skillId < 0 || skillId >= totalSkills) return;

        EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", skillId);
        String name = skillId < lordSkills
                ? TensuraSkillDispatcher.getSkillName(player, skillId, hasCreation)
                : "§6§l" + (skillId + 1) + ". " + PrimordialPlayerDataHelper.getSkillName(PrimordialPlayerDataHelper.getPrimordialType(player), skillId - lordSkills);
        player.displayClientMessage(Component.literal("§d§l[CHỌN NHANH] §f" + name), true);
    }

    private static void castCurrentSkillForMode(ServerLevel level, ServerPlayer player, boolean possessing) {
        if (possessing) {
            PrimordialPossessionManager.castSkill(player);
            return;
        }
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        boolean hasCreation = EntityDataHelper.getCustomData(player).getBoolean("TensuraMaterialCreation");
        boolean isWearingDivine = DivineArmorItem.isWearingAnyPiece(player);
        boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
        if (!(isTrueDemonLord || isWearingDivine || isPrimordial || player.isCreative())) return;

        int selected = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
        int lordSkills = TensuraSkillDispatcher.getSkillCount(hasCreation);
        if (selected < lordSkills) {
            TensuraSkillDispatcher.executeSkill(level, player, selected, hasCreation);
        } else if (isPrimordial) {
            PrimordialSkillDispatcher.castSkill(level, player, selected - lordSkills);
        }
    }
}
