package com.minhphuc.weapons.content.evolution;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class EvolvedSkillDispatcher {

    public static void cast(ServerLevel level, ServerPlayer player) {
        if (!EvolvedSkillHelper.hasEvolvedSkill(player)) {
            player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa sở hữu Kỹ Năng Tiến Hóa!"), true);
            return;
        }

        int skillId = EvolvedSkillHelper.getEvolvedSkillId(player);
        int tier = EvolvedSkillHelper.getEvolvedSkillTier(player);

        switch (skillId) {
            case EvolvedSkillHelper.SKILL_URIEL -> UrielCovenantAbility.cast(level, player, tier);
            case EvolvedSkillHelper.SKILL_REGALIA_DOMINION -> RegaliaDominionAbility.cast(level, player, tier);
            case EvolvedSkillHelper.SKILL_UNIVERSAL_SENSE -> UniversalSenseAbility.cast(level, player, tier);
            case EvolvedSkillHelper.SKILL_INFINITE_PRISON -> InfiniteDragonPrisonAbility.cast(level, player, tier);
            default -> player.displayClientMessage(Component.literal("§c⚠️ Kỹ năng tiến hóa không xác định!"), true);
        }
    }
}
