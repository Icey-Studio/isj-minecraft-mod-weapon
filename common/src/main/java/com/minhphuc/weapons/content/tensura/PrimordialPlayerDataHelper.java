package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PrimordialPlayerDataHelper {

    public static final String NBT_PRIMORDIAL_TYPE = "PrimordialDemonType";
    public static final String NBT_PHYSICAL_BODY = "PrimordialHasPhysicalBody";
    public static final String NBT_SELECTED_SKILL = "PrimordialSelectedSkillIndex";

    public static DemonType getPrimordialType(Player player) {
        if (player == null) return null;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        if (!tag.contains(NBT_PRIMORDIAL_TYPE)) return null;
        String typeName = tag.getString(NBT_PRIMORDIAL_TYPE);
        if (typeName.isEmpty()) return null;
        try {
            return DemonType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setPrimordialType(Player player, DemonType type) {
        if (player == null) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        if (type == null) {
            tag.remove(NBT_PRIMORDIAL_TYPE);
            tag.remove(NBT_PHYSICAL_BODY);
            tag.remove(NBT_SELECTED_SKILL);
        } else {
            tag.putString(NBT_PRIMORDIAL_TYPE, type.name());
            tag.putInt(NBT_SELECTED_SKILL, 0);
        }
    }

    public static boolean isPrimordial(Player player) {
        return getPrimordialType(player) != null;
    }

    public static boolean hasPhysicalBody(Player player) {
        if (player == null) return false;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        // Có thể xác nếu NBT đánh dấu true hoặc tổng số linh hồn đạt 40 trở lên
        if (tag.getBoolean(NBT_PHYSICAL_BODY)) return true;
        int souls = tag.getInt("TensuraCollectedSouls");
        if (souls >= 40) {
            tag.putBoolean(NBT_PHYSICAL_BODY, true);
            return true;
        }
        return false;
    }

    public static void setPhysicalBody(Player player, boolean hasBody) {
        if (player == null) return;
        EntityDataHelper.getCustomData(player).putBoolean(NBT_PHYSICAL_BODY, hasBody);
    }

    public static boolean isDemonLord(Player player) {
        if (player == null) return false;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        return tag.getBoolean("TensuraTrueDemonLord");
    }

    public static int getSelectedSkillIndex(Player player) {
        if (player == null) return 0;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        return Math.max(0, Math.min(5, tag.getInt(NBT_SELECTED_SKILL)));
    }

    public static void setSelectedSkillIndex(Player player, int skillIndex) {
        if (player == null) return;
        EntityDataHelper.getCustomData(player).putInt(NBT_SELECTED_SKILL, Math.max(0, Math.min(5, skillIndex)));
    }

    public static int cycleSkill(Player player) {
        int current = getSelectedSkillIndex(player);
        int next = (current + 1) % 6;
        setSelectedSkillIndex(player, next);
        return next;
    }

    public static void resetOnDeath(Player player) {
        if (player == null) return;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        tag.remove(NBT_PRIMORDIAL_TYPE);
        tag.remove(NBT_PHYSICAL_BODY);
        tag.remove(NBT_SELECTED_SKILL);
        // Reset luôn trạng thái Ma Vương khi chết theo luật chơi
        tag.remove("TensuraTrueDemonLord");
        tag.remove("TensuraHasSeed");
        tag.remove("TensuraCollectedSouls");
    }

    public static int getDemonColor(DemonType type) {
        if (type == null) return 0xFFFFFF;
        return switch (type) {
            case ROUGE -> 0xFF1E1E;
            case NOIR -> 0x222226;
            case BLANC -> 0xF0F4F8;
            case JAUNE -> 0xFACC15;
            case VIOLET -> 0xA855F7;
            case BLEU -> 0x3B82F6;
            case VERT -> 0x22C55E;
        };
    }

    public static String getDemonTitleVi(DemonType type) {
        if (type == null) return "Người Phàm";
        return switch (type) {
            case ROUGE -> "Xích Sắc Thủy Tổ - Guy Crimson";
            case NOIR -> "Hắc Sắc Thủy Tổ - Diablo";
            case BLANC -> "Bạch Sắc Thủy Tổ - Testarossa";
            case JAUNE -> "Hoàng Sắc Thủy Tổ - Carrera";
            case VIOLET -> "Tử Sắc Thủy Tổ - Ultima";
            case BLEU -> "Lam Sắc Thủy Tổ - Rein";
            case VERT -> "Lục Sắc Thủy Tổ - Misery";
        };
    }

    public static String getSkillName(DemonType type, int skillIndex) {
        if (skillIndex == 0) {
            return "Tà Khứ Vũ Thê Tử (Death Streak)";
        }
        if (skillIndex == 5) {
            return "Ngũ Trọng Ma Trận: Cột Sáng Thiên Khấu (Pentagram Celestial Pillar)";
        }
        if (type == null) return "Chưa thức tỉnh";
        return switch (type) {
            case NOIR -> switch (skillIndex) {
                case 1 -> "Thế Giới Cám Dỗ (Temptation World)";
                case 2 -> "Vũ Điệu Móng Vuốt Vực Thẳm (End of Despair)";
                case 3 -> "Nghịch Chuyển Sinh Tử (Reversal of Life & Death)";
                case 4 -> "Hắc Hạch Hư Vô Sụp Đổ (Cataclysmic Black Hole)";
                default -> "Kỹ Năng Thủy Tổ";
            };
            case ROUGE -> switch (skillIndex) {
                case 1 -> "Hỏa Ngục Bộc Viêm (Prominence Flare)";
                case 2 -> "Xích Hồng Ma Trảm (Crimson Severance)";
                case 3 -> "Bức Tường Hỏa Ma (Infernal Bastion)";
                case 4 -> "Lôi Hỏa Diệt Thế (Lucifer's Judgment)";
                default -> "Kỹ Năng Thủy Tổ";
            };
            case BLANC -> switch (skillIndex) {
                case 1 -> "Bạch Viêm Tuyệt Diệt (White Flare)";
                case 2 -> "Mị Hoặc Tinh Thần (Mind Domination)";
                case 3 -> "Lãnh Băng Hồ Điệp (Frost Butterflies)";
                case 4 -> "Trắng Xóa Hư Vô (Absolute Annihilation)";
                default -> "Kỹ Năng Thủy Tổ";
            };
            case JAUNE -> switch (skillIndex) {
                case 1 -> "Sụp Đổ Trọng Lực (Gravity Collapse)";
                case 2 -> "Tia Sáng Hoàng Kim (Golden Breaker)";
                case 3 -> "Tập Trung Xạ Kích (Abaddon Focus)";
                case 4 -> "Pháo Hạt Nhân Khởi Nguyên (Nuclear Cannon)";
                default -> "Kỹ Năng Thủy Tổ";
            };
            case VIOLET -> switch (skillIndex) {
                case 1 -> "Tử Độc Nở Rộ (Toxic Bloom)";
                case 2 -> "Hắc Tử Xuyên Tâm (Shadow Poison Ray)";
                case 3 -> "Huyễn Ảnh Tốc Biến (Violet Phantom Dash)";
                case 4 -> "Mưa Ăn Mòn Tuyệt Tự (Corrosive Ruin)";
                default -> "Kỹ Năng Thủy Tổ";
            };
            case BLEU -> switch (skillIndex) {
                case 1 -> "Băng Tinh Vũ Bão (Absolute Zero Shards)";
                case 2 -> "Hàn Băng Pháo Đài (Glacial Bastion)";
                case 3 -> "Lồng Giam Không Gian (Dimensional Canvas)";
                case 4 -> "Bão Tuyết Vĩnh Cửu (Eternal Blizzard)";
                default -> "Kỹ Năng Thủy Tổ";
            };
            case VERT -> switch (skillIndex) {
                case 1 -> "Lục Phong Tiễu Sát (Emerald Tempest Blades)";
                case 2 -> "Hấp Thụ Sinh Khí (Abyssal Vitality Drain)";
                case 3 -> "Kết Giới Lục Thần (Emerald Barrier Gale)";
                case 4 -> "Cuồng Phong Tai Ương (Calamity Maelstrom)";
                default -> "Kỹ Năng Thủy Tổ";
            };
        };
    }
}
