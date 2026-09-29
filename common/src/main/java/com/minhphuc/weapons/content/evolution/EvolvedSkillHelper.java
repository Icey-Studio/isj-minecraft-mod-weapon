package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class EvolvedSkillHelper {

    public static final String NBT_HAS_EVOLVED_SKILL = "HasEvolvedSkill";
    public static final String NBT_EVOLVED_SKILL_ID = "EvolvedSkillId";
    public static final String NBT_EVOLVED_SKILL_TIER = "EvolvedSkillTier";
    public static final String NBT_CONSUMED_SKILLS = "EvolvedConsumedSkills";

    // 4 Kỹ Năng Tối Thượng Tiến Hóa
    public static final int SKILL_URIEL = 0;
    public static final int SKILL_REGALIA_DOMINION = 1;
    public static final int SKILL_UNIVERSAL_SENSE = 2;
    public static final int SKILL_INFINITE_PRISON = 3;

    public record AvailableSkill(String key, int rank, String displayNameVi, String displayNameEn, String descVi, int colorHex) {}

    public static final List<AvailableSkill> ALL_DEMON_LORD_SKILLS = List.of(
            new AvailableSkill("dl_disintegration", 1, "1. Tam Trọng Thánh Giới - Linh Tử Băng Hoại", "Multi-Tier Disintegration", "Pháp trận tam tầng phân rã linh tử", 0xFFEA00),
            new AvailableSkill("dl_jacob_ladder", 2, "2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)", "Jacob's Ladder", "Quang trụ thanh tẩy 60m", 0x00E5FF),
            new AvailableSkill("dl_heavenly_judgment", 3, "3. Bát Môn Thiên Phạt Trận", "Heavenly Judgment Array", "8 Cột trụ phong ấn và trừng phạt", 0xFFA500),
            new AvailableSkill("dl_purification", 4, "4. Đại Thánh Tẩy - Quang Minh Cứu Rỗi", "Great Purification", "Quang hoàn cứu rỗi và hóa giải tà khí", 0x55FF55),
            new AvailableSkill("dl_beelzebuth", 5, "5. Bạo Thực Vương: Thôn Phệ (Predator)", "Predator / Beelzebuth", "Nuốt chửng linh lực kẻ thù", 0x8B008B),
            new AvailableSkill("dl_dragon_nova", 6, "6. Long Tinh Bộc Viêm Bá (Dragon Nova)", "Dragon Nova", "Cấm thuật bộc phá long viêm diệt thế", 0xFF00FF),
            new AvailableSkill("dl_taisui_stars", 7, "7. Phẫn Nộ Vương: Tuyệt Diệt Tinh Tú", "Extinction Stars", "Phóng chùm sao Thái Tuế bắn phá hủy diệt", 0x00FFFF),
            new AvailableSkill("dl_liuren_barrier", 8, "8. Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm)", "Liu Ren Calamity Barrier", "12 Thức thần trấn thủ bất tử tuyệt đối", 0x4169E1),
            new AvailableSkill("dl_seer_flesh", 9, "9. Thị Nhục - Nhục Thể Bất Tử Thái Tuế", "Seer Flesh Immortality", "Hồi phục toàn vẹn và đẩy lùi kẻ địch", 0x2E8B57),
            new AvailableSkill("dl_alkaid", 10, "10. Diệt Thế Tà Tinh: Alkaid", "Alkaid Annihilation", "Cầu năng lượng tà ác xoắn ốc hủy diệt", 0x8B0000),
            new AvailableSkill("dl_granite_blast", 11, "11. Cú Bắn Granit (Granite Blast)", "Granite Blast", "Chùm năng lượng tầm xa phá vỡ", 0xFFA500),
            new AvailableSkill("dl_thought_acceleration", 12, "12. Trí Huệ Chi Vương: Gia Tốc Tư Duy", "Thought Acceleration", "Gia tốc ý thức và né tránh tuyệt hảo", 0x00BFFF),
            new AvailableSkill("dl_all_creation", 13, "13. Trí tuệ Chi Vương: Thẩm Định Vạn Vật", "All of Creation", "Thấu triệt và phân tích mọi hiện tượng", 0x20B2AA),
            new AvailableSkill("dl_lucifer_replication", 14, "14. Kiêu Ngạo Vương Lucifer: Sao Chép", "Replication", "Sao chép năng lực mục tiêu", 0xDC143C),
            new AvailableSkill("dl_material_creation", 15, "15. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí", "Material Creation", "Tạo ra khoáng thạch và thần khí", 0xFFD700)
    );

    /**
     * Dữ liệu đồng bộ cho máy khách (Client side cache)
     */
    public static final Set<String> CLIENT_CONSUMED_SKILLS = Collections.synchronizedSet(new HashSet<>());
    public static int CLIENT_EVOLVED_SKILL_ID = -1;
    public static int CLIENT_EVOLVED_SKILL_TIER = 0;

    public static void setClientState(List<String> consumed, int skillId, int tier) {
        CLIENT_CONSUMED_SKILLS.clear();
        if (consumed != null) {
            CLIENT_CONSUMED_SKILLS.addAll(consumed);
        }
        CLIENT_EVOLVED_SKILL_ID = skillId;
        CLIENT_EVOLVED_SKILL_TIER = tier;
    }

    /**
     * Quét toàn bộ kỹ năng người chơi đang sở hữu (không tính kỹ năng kiếm).
     * Loại trừ các kỹ năng đã tiêu hao do dung hợp.
     */
    public static List<AvailableSkill> getPlayerAvailableSkills(Player player) {
        List<AvailableSkill> result = new ArrayList<>();
        if (player == null) return result;

        CompoundTag tag = EntityDataHelper.getCustomData(player);
        Set<String> consumed = getConsumedSkills(player);
        consumed.addAll(CLIENT_CONSUMED_SKILLS);

        boolean isTrueDemonLord = tag.getBoolean("TensuraTrueDemonLord");
        boolean hasCreation = tag.getBoolean("TensuraMaterialCreation");
        boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
        DemonType primordialType = PrimordialPlayerDataHelper.getPrimordialType(player);

        if (isTrueDemonLord) {
            int max = hasCreation ? 15 : 14;
            for (int i = 0; i < max && i < ALL_DEMON_LORD_SKILLS.size(); i++) {
                AvailableSkill skill = ALL_DEMON_LORD_SKILLS.get(i);
                if (!consumed.contains(skill.key())) {
                    result.add(skill);
                }
            }
        }

        if (isPrimordial && primordialType != null) {
            // Thêm các kỹ năng Thủy Tổ Ác Ma (đánh số Rank tiếp nối hoặc Rank cao)
            int baseRank = isTrueDemonLord ? 16 : 1;
            for (int idx = 0; idx < 6; idx++) {
                String pKey = "primordial_" + primordialType.name().toLowerCase() + "_" + idx;
                if (!consumed.contains(pKey)) {
                    String nameVi = (baseRank + idx) + ". " + PrimordialPlayerDataHelper.getSkillName(primordialType, idx);
                    int color = PrimordialPlayerDataHelper.getDemonColor(primordialType);
                    result.add(new AvailableSkill(pKey, baseRank + idx, nameVi, "Primordial Skill", "Kỹ năng độc bản của " + primordialType.name(), color));
                }
            }
        }

        // Nếu người chơi ở chế độ Sáng Tạo (Creative Mode) hoặc mặc Áo Giáp Thần Thoại mà chưa thức tỉnh:
        // Cung cấp danh sách kỹ năng Chân Ma Vương để có thể thử nghiệm dung hợp ngay lập tức!
        if (result.isEmpty() && (player.isCreative() || com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player))) {
            for (AvailableSkill skill : ALL_DEMON_LORD_SKILLS) {
                if (!consumed.contains(skill.key())) {
                    result.add(skill);
                }
            }
        }

        return result;
    }

    public static Set<String> getConsumedSkills(Player player) {
        Set<String> set = new HashSet<>();
        if (player == null) return set;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        if (tag.contains(NBT_CONSUMED_SKILLS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_CONSUMED_SKILLS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                set.add(list.getString(i));
            }
        }
        return set;
    }

    public static void addConsumedSkill(Player player, String skillKey) {
        if (player == null || skillKey == null || skillKey.isEmpty()) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        ListTag list;
        if (tag.contains(NBT_CONSUMED_SKILLS, Tag.TAG_LIST)) {
            list = tag.getList(NBT_CONSUMED_SKILLS, Tag.TAG_STRING);
        } else {
            list = new ListTag();
            tag.put(NBT_CONSUMED_SKILLS, list);
        }
        // Tránh trùng lặp
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(skillKey)) return;
        }
        list.add(StringTag.valueOf(skillKey));
    }

    public static void restoreAllConsumedSkills(Player player) {
        if (player == null) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        tag.remove(NBT_CONSUMED_SKILLS);
    }

    public static boolean hasConsumedSkills(Player player) {
        return !getConsumedSkills(player).isEmpty();
    }

    /**
     * Tính toán Mức (Tier 1, Tier 2, Tier 3) dựa trên 2 kỹ năng đem kết hợp.
     * Quy tắc:
     * - Kỹ năng 1, 2: uy lực ở Mức 1
     * - Kỹ năng 3, 4: uy lực ở Mức 2
     * - Kỹ năng 5 trở lên: uy lực ở Mức 3
     * Khi kết hợp 2 kỹ năng, lấy thứ tự cao nhất của 2 kỹ năng.
     */
    public static int calculateResultTier(int rankA, int rankB) {
        int maxRank = Math.max(rankA, rankB);
        if (maxRank <= 2) return 1;
        if (maxRank <= 4) return 2;
        return 3;
    }

    public static boolean hasEvolvedSkill(Player player) {
        if (player == null) return false;
        return EntityDataHelper.getCustomData(player).getBoolean(NBT_HAS_EVOLVED_SKILL);
    }

    public static int getEvolvedSkillId(Player player) {
        if (player == null) return -1;
        return EntityDataHelper.getCustomData(player).getInt(NBT_EVOLVED_SKILL_ID);
    }

    public static int getEvolvedSkillTier(Player player) {
        if (player == null) return 1;
        int tier = EntityDataHelper.getCustomData(player).getInt(NBT_EVOLVED_SKILL_TIER);
        return Math.max(1, Math.min(3, tier));
    }

    public static void setEvolvedSkill(Player player, int skillId, int tier) {
        if (player == null) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        tag.putBoolean(NBT_HAS_EVOLVED_SKILL, true);
        tag.putInt(NBT_EVOLVED_SKILL_ID, skillId);
        tag.putInt(NBT_EVOLVED_SKILL_TIER, Math.max(1, Math.min(3, tier)));
    }

    public static void removeEvolvedSkill(Player player) {
        if (player == null) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        tag.remove(NBT_HAS_EVOLVED_SKILL);
        tag.remove(NBT_EVOLVED_SKILL_ID);
        tag.remove(NBT_EVOLVED_SKILL_TIER);
    }

    public static void resetOnDeath(Player player) {
        if (player == null) return;
        removeEvolvedSkill(player);
    }

    public static String getEvolvedSkillNameVi(int skillId) {
        return switch (skillId) {
            case SKILL_URIEL -> "Thế Ước Vương Uriel (Covenant King Uriel)";
            case SKILL_REGALIA_DOMINION -> "Vương Quyền Chi Phối (Regalia Dominion)";
            case SKILL_UNIVERSAL_SENSE -> "Cảm Nhận Vạn Năng (Universal Sense)";
            case SKILL_INFINITE_PRISON -> "Lồng Giam Vô Hạn (Infinite Dragon Prison)";
            default -> "Kỹ Năng Tối Thượng";
        };
    }

    public static String getEvolvedSkillColor(int skillId) {
        return switch (skillId) {
            case SKILL_URIEL -> "§6";
            case SKILL_REGALIA_DOMINION -> "§e";
            case SKILL_UNIVERSAL_SENSE -> "§b";
            case SKILL_INFINITE_PRISON -> "§6";
            default -> "§d";
        };
    }
}
