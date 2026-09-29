package com.minhphuc.weapons.content.evolution;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class EvolvedSkillDispatcher {

    public static void castSkill(ServerLevel level, ServerPlayer player, int skillId) {
        int tier = EvolvedSkillHelper.getEvolvedSkillTier(player, skillId);
        if (tier <= 0) {
            player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa sở hữu Kỹ Năng Tiến Hóa này!"), true);
            return;
        }

        switch (skillId) {
            case EvolvedSkillHelper.SKILL_URIEL -> UrielCovenantAbility.cast(level, player, tier);
            case EvolvedSkillHelper.SKILL_REGALIA_DOMINION -> RegaliaDominionAbility.cast(level, player, tier);
            case EvolvedSkillHelper.SKILL_UNIVERSAL_SENSE -> UniversalSenseAbility.cast(level, player, tier);
            case EvolvedSkillHelper.SKILL_INFINITE_PRISON -> InfiniteDragonPrisonAbility.cast(level, player, tier);
            default -> player.displayClientMessage(Component.literal("§c⚠️ Kỹ năng tiến hóa không xác định!"), true);
        }
    }

    public static void cast(ServerLevel level, ServerPlayer player) {
        int selected = EvolvedSkillHelper.getSelectedEvolvedSkillId(player);
        if (selected != -1) {
            castSkill(level, player, selected);
            return;
        }
        int firstOwned = EvolvedSkillHelper.getEvolvedSkillId(player);
        if (firstOwned != -1) {
            castSkill(level, player, firstOwned);
            return;
        }
        player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa sở hữu Kỹ Năng Tiến Hóa!"), true);
    }
}
