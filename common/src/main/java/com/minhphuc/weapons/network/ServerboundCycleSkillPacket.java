package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.divine.MoonlightSwordItem;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ServerboundCycleSkillPacket {

    public ServerboundCycleSkillPacket() {
    }

    public ServerboundCycleSkillPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            ItemStack clawStack = mainHand.getItem() instanceof com.minhphuc.weapons.content.tensura.PrimordialClawItem
                    ? mainHand
                    : (offHand.getItem() instanceof com.minhphuc.weapons.content.tensura.PrimordialClawItem ? offHand : ItemStack.EMPTY);
            ItemStack fanStack = mainHand.getItem() instanceof com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem
                    ? mainHand
                    : (offHand.getItem() instanceof com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem ? offHand : ItemStack.EMPTY);
            ItemStack swordStack = mainHand.getItem() instanceof MoonlightSwordItem
                    ? mainHand
                    : (offHand.getItem() instanceof MoonlightSwordItem ? offHand : ItemStack.EMPTY);

            if (!clawStack.isEmpty()) {
                // Chuyển đổi skill cho Móng Vuốt Vực Thẳm / Hư Không
                com.minhphuc.weapons.content.tensura.PrimordialClawItem.cycleSkill(player, clawStack);
            } else if (!fanStack.isEmpty()) {
                // Chuyển đổi skill cho Quạt Lông Vũ Velgrynd
                com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem.cycleSkill(player, fanStack);
            } else if (!swordStack.isEmpty()) {
                // 1. Chuyển đổi skill cho Nguyệt Quang Thần Tế Kiếm
                MoonlightSwordItem.cycleSkill(player, swordStack);
            } else {
                // 2. Chuyển đổi skill cho Thủy Tổ Ác Ma & Chân Ma Vương (khi không cầm kiếm/vũ khí)
                boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
                boolean hasCreation = EntityDataHelper.getCustomData(player).getBoolean("TensuraMaterialCreation");
                boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
                com.minhphuc.weapons.entity.tensura.DemonType demonType = PrimordialPlayerDataHelper.getPrimordialType(player);

                boolean hasEvolved = com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.hasEvolvedSkill(player);
                int evolvedSkillId = hasEvolved ? com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.getEvolvedSkillId(player) : -1;
                int evolvedTier = hasEvolved ? com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.getEvolvedSkillTier(player) : 1;
                String evolvedSkillDisplay = hasEvolved ? com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.getEvolvedSkillColor(evolvedSkillId)
                        + "★ [TIẾN HÓA MỨC " + evolvedTier + "] " + com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.getEvolvedSkillNameVi(evolvedSkillId) : "";

                if (isTrueDemonLord && isPrimordial) {
                    int lordSkills = hasCreation ? 15 : 14;
                    int totalSkills = lordSkills + 7 + (hasEvolved ? 1 : 0);
                    int current = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
                    int next = (current + 1) % totalSkills;
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", next);

                    String skillName;
                    if (hasEvolved && next == totalSkills - 1) {
                        skillName = evolvedSkillDisplay;
                    } else if (next < lordSkills) {
                        skillName = switch (next) {
                            case 0 -> "§e§l1. Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Multi-Tier Disintegration)";
                            case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
                            case 2 -> "§6§l3. Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)";
                            case 3 -> "§a§l4. Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification)";
                            case 4 -> "§d§l5. Bạo Thực Vương: Thôn Phệ (Predator)";
                            case 5 -> "§d§l6. Long Tinh Bộc Viêm Bá: Dragon Nova (竜星爆炎覇)";
                            case 6 -> "§e§l7. Phẫn Nộ Vương: Tuyệt Diệt Tinh Tú";
                            case 7 -> "§b§l8. Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm Thần Khóa)";
                            case 8 -> "§c§l9. Thị Nhục - Nhục Thể Bất Tử Thái Tuế (Seer Flesh)";
                            case 9 -> "§4§l10. Diệt Thế Tà Tinh: Alkaid (ALKAID)";
                            case 10 -> "§6§l11. Cú Bắn Granit (Granite Blast)";
                            case 11 -> "§b§l12. Trí Huệ Chi Vương: Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo (Ciel)";
                            case 12 -> "§a§l13. Trí Huệ Chi Vương: Thẩm Định Vạn Vật (All of Creation)";
                            case 13 -> "§4§l14. Kiêu Ngạo Vương Lucifer: Sao Chép Tuyệt Kỹ (Replication)";
                            case 14 -> "§6§l15. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí (Material Creation)";
                            default -> "§7Chưa chọn";
                        };
                    } else {
                        int primordialSkillIdx = next - lordSkills;
                        skillName = "§6§l" + (next + 1) + ". " + PrimordialPlayerDataHelper.getSkillName(demonType, primordialSkillIdx);
                    }

                    player.displayClientMessage(
                        Component.literal("§d§l[MA VƯƠNG THỦY TỔ] §fKỹ năng: " + skillName + " §7(Chuột Phải để thi triển)"),
                        true
                    );
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F + (next * 0.15F));
                } else if (isTrueDemonLord) {
                    int lordSkills = hasCreation ? 15 : 14;
                    int maxSkills = lordSkills + (hasEvolved ? 1 : 0);
                    int current = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
                    int next = (current + 1) % maxSkills;
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", next);

                    String skillName;
                    if (hasEvolved && next == maxSkills - 1) {
                        skillName = evolvedSkillDisplay;
                    } else {
                        skillName = switch (next) {
                            case 0 -> "§e§l1. Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Multi-Tier Disintegration)";
                            case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
                            case 2 -> "§6§l3. Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)";
                            case 3 -> "§a§l4. Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification)";
                            case 4 -> "§d§l5. Bạo Thực Vương: Thôn Phệ (Predator)";
                            case 5 -> "§d§l6. Long Tinh Bộc Viêm Bá: Dragon Nova (竜星爆炎覇)";
                            case 6 -> "§e§l7. Phẫn Nộ Vương: Tuyệt Diệt Tinh Tú";
                            case 7 -> "§b§l8. Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm Thần Khóa)";
                            case 8 -> "§c§l9. Thị Nhục - Nhục Thể Bất Tử Thái Tuế (Seer Flesh)";
                            case 9 -> "§4§l10. Diệt Thế Tà Tinh: Alkaid (ALKAID)";
                            case 10 -> "§6§l11. Cú Bắn Granit (Granite Blast)";
                            case 11 -> "§b§l12. Trí Huệ Chi Vương: Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo (Ciel)";
                            case 12 -> "§a§l13. Trí Huệ Chi Vương: Thẩm Định Vạn Vật (All of Creation)";
                            case 13 -> "§4§l14. Kiêu Ngạo Vương Lucifer: Sao Chép Tuyệt Kỹ (Replication)";
                            case 14 -> "§6§l15. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí (Material Creation)";
                            default -> "§7Chưa chọn";
                        };
                    }

                    player.displayClientMessage(
                        Component.literal("§d§l[CHÂN MA VƯƠNG] §fKỹ năng: " + skillName + " §7(Chuột Phải để thi triển)"),
                        true
                    );

                    float pitch = 1.0F + (next * 0.25F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, pitch);
                } else if (isPrimordial) {
                    int total = 7 + (hasEvolved ? 1 : 0);
                    int current = PrimordialPlayerDataHelper.getSelectedSkillIndex(player);
                    int next = (current + 1) % total;
                    PrimordialPlayerDataHelper.setSelectedSkillIndex(player, next);
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", next);

                    String skillName = (hasEvolved && next == total - 1)
                            ? evolvedSkillDisplay
                            : PrimordialPlayerDataHelper.getSkillName(demonType, next);

                    player.displayClientMessage(
                        Component.literal("§6§l[" + demonType.name() + "] §eKỹ năng " + (next + 1) + ": §f" + skillName + " §7(Chuột Phải để thi triển)"),
                        true
                    );
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F + (next * 0.25F));
                } else if (com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player)) {
                    int lordSkills = 14;
                    int maxSkills = lordSkills + (hasEvolved ? 1 : 0);
                    int current = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
                    int next = (current + 1) % maxSkills;
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", next);

                    String skillName = (hasEvolved && next == maxSkills - 1)
                            ? evolvedSkillDisplay
                            : switch (next) {
                        case 0 -> "§e§l1. Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Multi-Tier Disintegration)";
                        case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
                        case 2 -> "§6§l3. Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)";
                        case 3 -> "§a§l4. Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification)";
                        case 4 -> "§d§l5. Bạo Thực Vương: Thôn Phệ (Predator)";
                        case 5 -> "§d§l6. Long Tinh Bộc Viêm Bá: Dragon Nova (竜星爆炎覇)";
                        case 6 -> "§e§l7. Phẫn Nộ Vương: Tuyệt Diệt Tinh Tú";
                        case 7 -> "§b§l8. Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm Thần Khóa)";
                        case 8 -> "§c§l9. Thị Nhục - Nhục Thể Bất Tử Thái Tuế (Seer Flesh)";
                        case 9 -> "§4§l10. Diệt Thế Tà Tinh: Alkaid (ALKAID)";
                        case 10 -> "§6§l11. Cú Bắn Granit (Granite Blast)";
                        case 11 -> "§b§l12. Trí Huệ Chi Vương: Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo (Ciel)";
                        case 12 -> "§a§l13. Trí Huệ Chi Vương: Thẩm Định Vạn Vật (All of Creation)";
                        case 13 -> "§4§l14. Kiêu Ngạo Vương Lucifer: Sao Chép Tuyệt Kỹ (Replication)";
                        default -> "§7Chưa chọn";
                    };

                    player.displayClientMessage(
                        Component.literal("§6§l[THẦN LINH VŨ TRANG] §fKỹ năng: " + skillName + " §7(Chuột Phải để thi triển)"),
                        true
                    );

                    float pitch = 1.0F + (next * 0.25F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, pitch);
                } else if (hasEvolved) {
                    // Người chơi bình thường nhưng đã sở hữu Kỹ Năng Tiến Hóa!
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", 99);
                    player.displayClientMessage(
                        Component.literal("§6§l[KỸ NĂNG TỐI THƯỢNG] §fKỹ năng: " + evolvedSkillDisplay + " §7(Chuột Phải để thi triển)"),
                        true
                    );
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);
                } else {
                    player.displayClientMessage(
                        Component.literal("§c§l[THÔNG BÁO] §fBáo cáo. Cá thể chưa thức tỉnh thành Thủy Tổ Ác Ma / Chân Ma Vương hoặc chưa sở hữu Kỹ Năng Tiến Hóa!"),
                        true
                    );
                }
            }
        });
    }
}
