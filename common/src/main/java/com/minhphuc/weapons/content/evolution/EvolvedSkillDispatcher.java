package com.minhphuc.weapons.content.evolution;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class EvolvedSkillDispatcher {

    public static void castSkill(ServerLevel level, ServerPlayer player, int skillId) {
        switch (skillId) {
            case EvolvedSkillHelper.SKILL_URIEL -> EvolvedSkillHelper.toggleUrielDefense(player);
            case EvolvedSkillHelper.SKILL_UNIVERSAL_SENSE -> UniversalSenseAbility.cast(level, player, 3);
            case EvolvedSkillHelper.SKILL_INFINITE_PRISON -> InfiniteDragonPrisonAbility.cast(level, player, 3);
            default -> player.displayClientMessage(Component.literal("§c⚠️ Kỹ năng không xác định!"), true);
        }
    }
}
