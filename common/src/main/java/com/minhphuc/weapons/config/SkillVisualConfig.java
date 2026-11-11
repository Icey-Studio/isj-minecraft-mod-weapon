package com.minhphuc.weapons.config;

import java.util.BitSet;

/**
 * CONFIG & RUNTIME TOGGLE: HỆ THỐNG QUẢN LÝ HIỆU ỨNG ĐIỆN ẢNH & SAO LƯU DỰ PHÒNG
 * Cho phép người chơi bật/tắt chế độ Điện Ảnh (Cinematic 3D) hoặc quay về Bản Cũ (Legacy)
 * cho từng kỹ năng độc lập ngay trong khi chơi mà không cần restart hay build lại mod.
 */
public final class SkillVisualConfig {

    public static final int SKILL_DISINTEGRATION = 1;
    public static final int SKILL_JACOBS_LADDER = 2;
    public static final int SKILL_PURIFICATION_PILLAR = 3;
    public static final int SKILL_FAITH_AND_GRACE = 4;
    public static final int SKILL_BEELZEBUTH = 5;
    public static final int SKILL_DRAGON_NOVA = 6;
    public static final int SKILL_EXTINCTION_STARS = 7;
    public static final int SKILL_SEER_FLESH = 8;
    public static final int SKILL_ALKAID = 9;
    public static final int SKILL_GRANITE_BLAST = 10;
    public static final int SKILL_REVERSE_TIME_SLASH = 11;
    public static final int SKILL_REVERSE_VELOCITY_IAI = 12;
    public static final int SKILL_INFINITE_DRAGON_PRISON = 13;
    public static final int SKILL_MULTILAYER_BARRIER = 14;
    public static final int SKILL_UNIVERSAL_SENSE = 15;
    public static final int SKILL_URIEL_DEFENSE = 16;
    public static final int SKILL_RYKUZIN_SINKA = 17;
    public static final int SKILL_ZONE_TRACK = 18;
    public static final int SKILL_NULLIFICATION = 19;
    public static final int SKILL_HELLFLARE = 20;
    public static final int SKILL_PRIMORDIAL_DEMON = 21;
    public static final int SKILL_DIMENSIONAL_SANCTUARY = 22;
    public static final int SKILL_SPIRITRON_OUTBURST = 23;
    public static final int SKILL_CHROMATIC_PILLAR = 24;

    private static boolean globalCinematicEnabled = true;
    private static final BitSet disabledSkills = new BitSet(64);

    /**
     * Kiểm tra xem kỹ năng có đang bật chế độ Điện Ảnh 3D hay không
     */
    public static boolean isCinematicEnabled(int skillId) {
        if (!globalCinematicEnabled) return false;
        return !disabledSkills.get(skillId);
    }

    /**
     * Bật/Tắt chế độ Điện Ảnh 3D cho một kỹ năng cụ thể
     */
    public static void setSkillCinematic(int skillId, boolean enabled) {
        if (enabled) {
            disabledSkills.clear(skillId);
        } else {
            disabledSkills.set(skillId);
        }
    }

    /**
     * Đổi trạng thái (Toggle) giữa Cinematic 3D và Legacy Classic
     */
    public static boolean toggleSkill(int skillId) {
        boolean current = isCinematicEnabled(skillId);
        setSkillCinematic(skillId, !current);
        return !current;
    }

    /**
     * Bật/Tắt toàn bộ chế độ Điện Ảnh trên toàn mod
     */
    public static void setGlobalCinematic(boolean enabled) {
        globalCinematicEnabled = enabled;
    }

    public static boolean isGlobalCinematicEnabled() {
        return globalCinematicEnabled;
    }
}
