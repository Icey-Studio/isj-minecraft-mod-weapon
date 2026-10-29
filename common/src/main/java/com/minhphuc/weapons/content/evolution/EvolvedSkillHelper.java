package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.data.EntityDataHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class EvolvedSkillHelper {

    public static final String NBT_URIEL_DEFENSE_ACTIVE = "UrielDefenseActive";

    // 3 Kỹ Năng Tối Thượng Tiến Hóa (Đã loại bỏ hoàn toàn Vương Quyền Chi Phối)
    public static final int SKILL_URIEL = 0;
    public static final int SKILL_UNIVERSAL_SENSE = 1;
    public static final int SKILL_INFINITE_PRISON = 2;

    public static boolean isUrielDefenseActive(Player player) {
        if (player == null) return false;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        if (!tag.contains(NBT_URIEL_DEFENSE_ACTIVE)) {
            // Mặc định BẬT khi mới nhận giáp thần linh hoặc ma vương
            tag.putBoolean(NBT_URIEL_DEFENSE_ACTIVE, true);
            return true;
        }
        return tag.getBoolean(NBT_URIEL_DEFENSE_ACTIVE);
    }

    public static void setUrielDefenseActive(Player player, boolean active) {
        if (player == null) return;
        EntityDataHelper.getCustomData(player).putBoolean(NBT_URIEL_DEFENSE_ACTIVE, active);
    }

    public static void toggleUrielDefense(Player player) {
        if (player == null) return;
        boolean next = !isUrielDefenseActive(player);
        setUrielDefenseActive(player, next);

        if (next) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.6F);
            player.displayClientMessage(
                    Component.literal("§6§l[URIEL] §fPhòng Ngự Tuyệt Đối: §a§l[BẬT] 🛡️ §7(Miễn nhiễm 100% sát thương thông thường)"),
                    true
            );
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§6§l[URIEL] §fPhòng Ngự Tuyệt Đối: §c§l[TẮT] ✕ §7(Hủy trạng thái bất tử 100%)"),
                    true
            );
        }
    }

    /**
     * Mọi kỹ năng tiến hóa đều đạt cấp độ tối đa: Cấp 3 (bỏ phân cấp).
     */
    public static int getEvolvedSkillTier(Player player, int skillId) {
        return 3;
    }

    public static void setEvolvedSkillTier(Player player, int skillId, int tier) {
        // Luôn luôn là Cấp 3
    }

    public static boolean hasConsumedSkills(Player player) {
        return false;
    }

    public static void restoreAllConsumedSkills(Player player) {
    }

    public static void resetOnDeath(Player player) { if (player == null) return; setUrielDefenseActive(player, true); }

    public static String getEvolvedSkillNameVi(int skillId) {
        return switch (skillId) {
            case SKILL_URIEL -> "Phòng Ngự Tuyệt Đối Của Uriel";
            case SKILL_UNIVERSAL_SENSE -> "Cảm Nhận Vạn Năng (Universal Sense)";
            case SKILL_INFINITE_PRISON -> "Lồng Giam Vô Hạn (Infinite Dragon Prison)";
            default -> "Kỹ Năng Tối Thượng";
        };
    }
}
