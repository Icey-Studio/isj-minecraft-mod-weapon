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
            new AvailableSkill("dl_purification", 4, "4. Phép Màu Của Thần Linh", "Miracle of the Gods", "Quang hoàn cứu rỗi, hồi phục vạn vật và chiếu sáng vĩnh viễn", 0x55FFAA),
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
            new AvailableSkill("dl_material_creation", 15, "15. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí", "Material Creation", "Tạo ra khoáng thạch và thần khí", 0xFFD700),
            new AvailableSkill("dl_anti_magic_barrier", 16, "16. Kháng Ma Kết Giới", "Anti-Magic Barrier", "Mái vòm cấm quái bảo hộ vĩnh viễn", 0x00FF7F),
            new AvailableSkill("dl_multilayer_barrier", 17, "17. Đa Trùng Kết Giới", "Multilayer Barrier", "Tường chắn và khối cầu bất khả xâm phạm", 0x00FFFF),
            new AvailableSkill("dl_zone_track", 18, "18. Granit Xuyên Phá (Zone Track)", "Zone Track", "Đại pháo Granit bạch kim hủy diệt không hồi chiêu", 0xFFFFFF)
    );

    /**
     * Dữ liệu đồng bộ cho máy khách (Client side cache)
     */
    public static final Set<String> CLIENT_CONSUMED_SKILLS = Collections.synchronizedSet(new HashSet<>());
    public static int CLIENT_EVOLVED_SKILL_ID = -1;
    public static int CLIENT_EVOLVED_SKILL_TIER = 0;
    public static final int[] CLIENT_EVOLVED_SKILL_TIERS = new int[4];

    public static void setClientState(List<String> consumed, int skillId, int tier, int[] tiers) {
        CLIENT_CONSUMED_SKILLS.clear();
        if (consumed != null) {
            CLIENT_CONSUMED_SKILLS.addAll(consumed);
        }
        CLIENT_EVOLVED_SKILL_ID = skillId;
        CLIENT_EVOLVED_SKILL_TIER = tier;
        if (tiers != null) {
            System.arraycopy(tiers, 0, CLIENT_EVOLVED_SKILL_TIERS, 0, Math.min(tiers.length, 4));
        } else if (skillId >= 0 && skillId < 4) {
            CLIENT_EVOLVED_SKILL_TIERS[skillId] = tier;
        }
    }

    public static void setClientState(List<String> consumed, int skillId, int tier) {
        setClientState(consumed, skillId, tier, null);
    }

    /**
     * Lấy toàn bộ danh mục kỹ năng hợp lệ của người chơi (không lọc qua trạng thái tiêu hao).
     */
    public static List<AvailableSkill> getAllPossibleSkillsForPlayer(Player player) {
        List<AvailableSkill> result = new ArrayList<>();
        if (player == null) return result;

        CompoundTag tag = EntityDataHelper.getCustomData(player);
        boolean isTrueDemonLord = tag.getBoolean("TensuraTrueDemonLord");
        boolean hasCreation = tag.getBoolean("TensuraMaterialCreation");
        boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
        DemonType primordialType = PrimordialPlayerDataHelper.getPrimordialType(player);

        if (isTrueDemonLord) {
            for (AvailableSkill skill : ALL_DEMON_LORD_SKILLS) {
                if (!hasCreation && skill.key().equals("dl_material_creation")) {
                    continue;
                }
                result.add(skill);
            }
        }

        if (isPrimordial && primordialType != null) {
            int baseRank = isTrueDemonLord ? 19 : 1;
            for (int idx = 0; idx < 6; idx++) {
                String pKey = "primordial_" + primordialType.name().toLowerCase() + "_" + idx;
                String nameVi = (baseRank + idx) + ". " + PrimordialPlayerDataHelper.getSkillName(primordialType, idx);
                int color = PrimordialPlayerDataHelper.getDemonColor(primordialType);
                result.add(new AvailableSkill(pKey, baseRank + idx, nameVi, "Primordial Skill", "Kỹ năng độc bản của " + primordialType.name(), color));
            }
        }

        if (result.isEmpty() && (player.isCreative() || com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player))) {
            for (AvailableSkill skill : ALL_DEMON_LORD_SKILLS) {
                result.add(skill);
            }
        }

        return result;
    }

    public static AvailableSkill getSkillByKey(Player player, String key) {
        if (player == null || key == null) return null;
        for (AvailableSkill s : getAllPossibleSkillsForPlayer(player)) {
            if (s.key().equals(key)) return s;
        }
        return null;
    }

    /**
     * Quét toàn bộ kỹ năng người chơi đang sở hữu (không tính kỹ năng kiếm).
     * Khi dung hợp, các kỹ năng nguyên liệu KHÔNG bị mất và vẫn có thể sử dụng bình thường.
     */
    public static List<AvailableSkill> getPlayerAvailableSkills(Player player) {
        if (player == null) return new ArrayList<>();
        return getAllPossibleSkillsForPlayer(player);
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

    public static final String NBT_EVOLVED_SKILLS_TIERS = "EvolvedSkillsTiers";

    public static boolean hasEvolvedSkill(Player player) {
        return !getOwnedEvolvedSkills(player).isEmpty();
    }

    public static int getEvolvedSkillTier(Player player, int skillId) {
        if (player == null || skillId < 0 || skillId >= 4) return 0;
        if (player.level().isClientSide()) {
            return CLIENT_EVOLVED_SKILL_TIERS[skillId];
        }
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        if (tag.contains(NBT_EVOLVED_SKILLS_TIERS)) {
            int[] tiers = tag.getIntArray(NBT_EVOLVED_SKILLS_TIERS);
            if (tiers.length > skillId) {
                return tiers[skillId];
            }
        }
        if (tag.getBoolean(NBT_HAS_EVOLVED_SKILL) && tag.getInt(NBT_EVOLVED_SKILL_ID) == skillId) {
            return Math.max(1, tag.getInt(NBT_EVOLVED_SKILL_TIER));
        }
        return 0;
    }

    public static int[] getEvolvedSkillTiers(Player player) {
        int[] result = new int[4];
        if (player == null) return result;
        for (int i = 0; i < 4; i++) {
            result[i] = getEvolvedSkillTier(player, i);
        }
        return result;
    }

    public static List<Integer> getOwnedEvolvedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        if (player == null) return list;
        for (int i = 0; i < 4; i++) {
            if (getEvolvedSkillTier(player, i) > 0) {
                list.add(i);
            }
        }
        return list;
    }

    public static int getEvolvedSkillId(Player player) {
        if (player == null) return -1;
        List<Integer> owned = getOwnedEvolvedSkills(player);
        return owned.isEmpty() ? -1 : owned.get(0);
    }

    public static int getEvolvedSkillTier(Player player) {
        int id = getEvolvedSkillId(player);
        return id == -1 ? 1 : getEvolvedSkillTier(player, id);
    }

    public static void setEvolvedSkillTier(Player player, int skillId, int tier) {
        if (player == null || skillId < 0 || skillId >= 4) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        int[] tiers = new int[4];
        if (tag.contains(NBT_EVOLVED_SKILLS_TIERS)) {
            int[] existing = tag.getIntArray(NBT_EVOLVED_SKILLS_TIERS);
            System.arraycopy(existing, 0, tiers, 0, Math.min(existing.length, 4));
        } else if (tag.getBoolean(NBT_HAS_EVOLVED_SKILL)) {
            int oldId = tag.getInt(NBT_EVOLVED_SKILL_ID);
            int oldTier = tag.getInt(NBT_EVOLVED_SKILL_TIER);
            if (oldId >= 0 && oldId < 4) {
                tiers[oldId] = Math.max(1, oldTier);
            }
        }
        tiers[skillId] = Math.max(1, Math.min(3, tier));
        tag.putIntArray(NBT_EVOLVED_SKILLS_TIERS, tiers);

        tag.putBoolean(NBT_HAS_EVOLVED_SKILL, true);
        tag.putInt(NBT_EVOLVED_SKILL_ID, skillId);
        tag.putInt(NBT_EVOLVED_SKILL_TIER, tiers[skillId]);
    }

    public static void setEvolvedSkill(Player player, int skillId, int tier) {
        setEvolvedSkillTier(player, skillId, tier);
    }

    public static int getSelectedEvolvedSkillId(Player player) {
        if (player == null) return -1;
        List<Integer> owned = getOwnedEvolvedSkills(player);
        if (owned.isEmpty()) return -1;

        CompoundTag tag = EntityDataHelper.getCustomData(player);
        int selectedSkill = tag.getInt("TensuraDemonLordSkill");
        boolean isTrueDemonLord = tag.getBoolean("TensuraTrueDemonLord");
        boolean hasCreation = tag.getBoolean("TensuraMaterialCreation");
        boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
        boolean isWearingDivineArmor = com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player);
        int lordSkills = hasCreation ? 18 : 17;

        if (isTrueDemonLord && isPrimordial) {
            int evolvedBase = lordSkills + 7;
            if (selectedSkill >= evolvedBase && selectedSkill < evolvedBase + owned.size()) {
                return owned.get(selectedSkill - evolvedBase);
            }
        } else if (isTrueDemonLord) {
            int evolvedBase = lordSkills;
            if (selectedSkill >= evolvedBase && selectedSkill < evolvedBase + owned.size()) {
                return owned.get(selectedSkill - evolvedBase);
            }
        } else if (isPrimordial) {
            int evolvedBase = 7;
            if (selectedSkill >= evolvedBase && selectedSkill < evolvedBase + owned.size()) {
                return owned.get(selectedSkill - evolvedBase);
            }
        } else if (isWearingDivineArmor) {
            int evolvedBase = 14;
            if (selectedSkill >= evolvedBase && selectedSkill < evolvedBase + owned.size()) {
                return owned.get(selectedSkill - evolvedBase);
            }
        } else {
            if (selectedSkill >= 0 && selectedSkill < owned.size()) {
                return owned.get(selectedSkill);
            }
        }
        return -1;
    }

    public static void removeEvolvedSkill(Player player) {
        if (player == null) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        tag.remove(NBT_EVOLVED_SKILLS_TIERS);
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
