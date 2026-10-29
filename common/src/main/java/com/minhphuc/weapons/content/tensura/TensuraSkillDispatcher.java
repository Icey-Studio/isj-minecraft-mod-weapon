package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.darkgathering.AlkaidAbility;
import com.minhphuc.weapons.content.darkgathering.SeerFleshAbility;
import com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility;
import com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility;
import com.minhphuc.weapons.content.divine.FaithAndGraceAbility;
import com.minhphuc.weapons.content.divine.JacobsLadderAbility;
import com.minhphuc.weapons.content.divine.PurificationPillarAbility;
import com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.content.evolution.UniversalSenseAbility;
import com.minhphuc.weapons.network.ClientboundOpenMaterialCreationPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class TensuraSkillDispatcher {

    public static int getSkillCount(boolean hasCreation) {
        return hasCreation ? 18 : 17;
    }

    public static String getSkillName(ServerPlayer player, int index, boolean hasCreation) {
        boolean urielActive = EvolvedSkillHelper.isUrielDefenseActive(player);
        if (hasCreation) {
            return switch (index) {
                case 0 -> "§e§l1. Linh Tử Băng Hoại (Disintegration)";
                case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
                case 2 -> "§a§l3. Phép Màu Của Thần Linh (Miracle of the Gods)";
                case 3 -> "§d§l4. Bí Ẩn Của Đức Tin Và Ân Sủng (Faith and Grace)";
                case 4 -> "§5§l5. Bạo thực vương (Beelzebuth)";
                case 5 -> "§c§l6. Phẫn Nộ Vương: Long Tinh Bộc Viêm Bá (Dragon Nova)";
                case 6 -> "§e§l7. Thần Nộ: Tuyệt Diệt Tinh Tú (Extinction Stars)";
                case 7 -> "§2§l8. Thị Thực (Seer Flesh)";
                case 8 -> "§4§l9. Diệt Thế Tà Tinh: Alkaid (ALKAID)";
                case 9 -> "§6§l10. Cú Bắn Granit (Granite Blast)";
                case 10 -> "§f§l11. Nghịch Thời Nhất Trảm (Reverse-Time Slash)";
                case 11 -> "§6§l12. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí (Material Creation)";
                case 12 -> "§a§l13. Kháng Ma Kết Giới (Anti-Magic Barrier)";
                case 13 -> "§b§l14. Đa Trùng Kết Giới (Multilayer Barrier)";
                case 14 -> "§f§l15. Zone Track (Pure White Beam)";
                case 15 -> "§b§l16. Cảm Nhận Vạn Năng (Universal Sense)";
                case 16 -> "§6§l17. Lồng Giam Vô Hạn (Infinite Dragon Prison)";
                case 17 -> "§e§l18. Phòng Ngự Tuyệt Đối Của Uriel (" + (urielActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                default -> "§7Chưa chọn";
            };
        } else {
            return switch (index) {
                case 0 -> "§e§l1. Linh Tử Băng Hoại (Disintegration)";
                case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
                case 2 -> "§a§l3. Phép Màu Của Thần Linh (Miracle of the Gods)";
                case 3 -> "§d§l4. Bí Ẩn Của Đức Tin Và Ân Sủng (Faith and Grace)";
                case 4 -> "§5§l5. Bạo thực vương (Beelzebuth)";
                case 5 -> "§c§l6. Phẫn Nộ Vương: Long Tinh Bộc Viêm Bá (Dragon Nova)";
                case 6 -> "§e§l7. Thần Nộ: Tuyệt Diệt Tinh Tú (Extinction Stars)";
                case 7 -> "§2§l8. Thị Thực (Seer Flesh)";
                case 8 -> "§4§l9. Diệt Thế Tà Tinh: Alkaid (ALKAID)";
                case 9 -> "§6§l10. Cú Bắn Granit (Granite Blast)";
                case 10 -> "§f§l11. Nghịch Thời Nhất Trảm (Reverse-Time Slash)";
                case 11 -> "§a§l12. Kháng Ma Kết Giới (Anti-Magic Barrier)";
                case 12 -> "§b§l13. Đa Trùng Kết Giới (Multilayer Barrier)";
                case 13 -> "§f§l14. Zone Track (Pure White Beam)";
                case 14 -> "§b§l15. Cảm Nhận Vạn Năng (Universal Sense)";
                case 15 -> "§6§l16. Lồng Giam Vô Hạn (Infinite Dragon Prison)";
                case 16 -> "§e§l17. Phòng Ngự Tuyệt Đối Của Uriel (" + (urielActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                default -> "§7Chưa chọn";
            };
        }
    }

    public static void executeSkill(ServerLevel level, ServerPlayer player, int index, boolean hasCreation) {
        // Nếu đang kích hoạt Tuyệt Diệt Tinh Tú thì chỉ cho phép kết hợp kích hoạt Thị Thực (Chiêu 8 - index 7) hoặc Alkaid (Chiêu 9 - index 8)
        if (index != 6 && index != 7 && index != 8 && TaisuiExtinctionStarsAbility.isTaisuiActive(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Đang trong trạng thái Tuyệt Diệt Tinh Tú! Chỉ có thể kết hợp kích hoạt Thị Thực hoặc Diệt Thế Tà Tinh (Alkaid)!"),
                    true
            );
            return;
        }

        if (hasCreation) {
            switch (index) {
                case 0 -> SanctuaryDisintegrationAbility.cast(level, player, ItemStack.EMPTY);
                case 1 -> JacobsLadderAbility.cast(level, player, ItemStack.EMPTY);
                case 2 -> PurificationPillarAbility.cast(level, player, ItemStack.EMPTY);
                case 3 -> FaithAndGraceAbility.cast(level, player);
                case 4 -> BeelzebuthAbility.executeBeelzebuth(level, player);
                case 5 -> DragonNovaAbility.cast(level, player);
                case 6 -> TaisuiExtinctionStarsAbility.cast(level, player);
                case 7 -> SeerFleshAbility.cast(level, player);
                case 8 -> AlkaidAbility.cast(level, player);
                case 9 -> HorizontalHolyBeamAbility.cast(level, player);
                case 10 -> TengaiKyuseiSwordAbility.castReverseTimeSlash(level, player);
                case 11 -> ModMessages.sendToPlayer(new ClientboundOpenMaterialCreationPacket(), player);
                case 12 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
                case 13 -> MultilayerBarrierAbility.cast(level, player);
                case 14 -> ZoneTrackAbility.cast(level, player);
                case 15 -> UniversalSenseAbility.cast(level, player, 3);
                case 16 -> InfiniteDragonPrisonAbility.cast(level, player, 3);
                case 17 -> EvolvedSkillHelper.toggleUrielDefense(player);
            }
        } else {
            switch (index) {
                case 0 -> SanctuaryDisintegrationAbility.cast(level, player, ItemStack.EMPTY);
                case 1 -> JacobsLadderAbility.cast(level, player, ItemStack.EMPTY);
                case 2 -> PurificationPillarAbility.cast(level, player, ItemStack.EMPTY);
                case 3 -> FaithAndGraceAbility.cast(level, player);
                case 4 -> BeelzebuthAbility.executeBeelzebuth(level, player);
                case 5 -> DragonNovaAbility.cast(level, player);
                case 6 -> TaisuiExtinctionStarsAbility.cast(level, player);
                case 7 -> SeerFleshAbility.cast(level, player);
                case 8 -> AlkaidAbility.cast(level, player);
                case 9 -> HorizontalHolyBeamAbility.cast(level, player);
                case 10 -> TengaiKyuseiSwordAbility.castReverseTimeSlash(level, player);
                case 11 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
                case 12 -> MultilayerBarrierAbility.cast(level, player);
                case 13 -> ZoneTrackAbility.cast(level, player);
                case 14 -> UniversalSenseAbility.cast(level, player, 3);
                case 15 -> InfiniteDragonPrisonAbility.cast(level, player, 3);
                case 16 -> EvolvedSkillHelper.toggleUrielDefense(player);
            }
        }
    }
}
