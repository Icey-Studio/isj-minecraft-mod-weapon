package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.darkgathering.AlkaidAbility;
import com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility;
import com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility;
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
        return hasCreation ? 28 : 27;
    }

    public static String getSkillName(ServerPlayer player, int index, boolean hasCreation) {
        boolean urielActive = EvolvedSkillHelper.isUrielDefenseActive(player);
        boolean rykuzinActive = RykuzinSinkaAbility.isActive(player);
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
                case 11 -> "§c§l12. Bạt Đao Thuật Vận Tốc Ngược (Reverse-Velocity Iaijutsu)";
                case 12 -> "§6§l13. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí (Material Creation)";
                case 13 -> "§a§l14. Kháng Ma Kết Giới (Anti-Magic Barrier)";
                case 14 -> "§b§l15. Đa Trùng Kết Giới (Multilayer Barrier)";
                case 15 -> "§f§l16. Zone Track (Pure White Beam)";
                case 16 -> "§b§l17. Cảm Nhận Vạn Năng (Universal Sense)";
                case 17 -> "§6§l18. Lồng Giam Vô Hạn (Infinite Dragon Prison)";
                case 18 -> "§e§l19. Phòng Ngự Tuyệt Đối Của Uriel (" + (urielActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 19 -> "§e§l20. Tiên Tri: Rykuzin Sinka (" + (rykuzinActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 20 -> "§4§l21. Hắc Viêm Ngục (Hellflare)";
                case 21 -> "§c§l22. Hắc Viêm Hỏa Cầu (Dark Flame Shot)";
                case 22 -> "§4§l23. Lời Chúc Tử Vong (Death Blessing)";
                case 23 -> "§6§l24. Siêu Tân Tinh (Supernova)";
                case 24 -> "§c§l25. Phong Sát Kĩ Năng (Skill Nullification)";
                case 25 -> "§5§l26. Thần Hư Không Azathoth (Void God Azathoth)";
                case 26 -> "§6§l27. Ban Danh Xưng (Naming Ability)";
                case 27 -> "§b§l28. Dị Giới Thần Môn (Dimensional Sanctuary)";
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
                case 11 -> "§c§l12. Bạt Đao Thuật Vận Tốc Ngược (Reverse-Velocity Iaijutsu)";
                case 12 -> "§a§l13. Kháng Ma Kết Giới (Anti-Magic Barrier)";
                case 13 -> "§b§l14. Đa Trùng Kết Giới (Multilayer Barrier)";
                case 14 -> "§f§l15. Zone Track (Pure White Beam)";
                case 15 -> "§b§l16. Cảm Nhận Vạn Năng (Universal Sense)";
                case 16 -> "§6§l17. Lồng Giam Vô Hạn (Infinite Dragon Prison)";
                case 17 -> "§e§l18. Phòng Ngự Tuyệt Đối Của Uriel (" + (urielActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 18 -> "§e§l19. Tiên Tri: Rykuzin Sinka (" + (rykuzinActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 19 -> "§4§l20. Hắc Viêm Ngục (Hellflare)";
                case 20 -> "§c§l21. Hắc Viêm Hỏa Cầu (Dark Flame Shot)";
                case 21 -> "§4§l22. Lời Chúc Tử Vong (Death Blessing)";
                case 22 -> "§6§l23. Siêu Tân Tinh (Supernova)";
                case 23 -> "§c§l24. Phong Sát Kĩ Năng (Skill Nullification)";
                case 24 -> "§5§l25. Thần Hư Không Azathoth (Void God Azathoth)";
                case 25 -> "§6§l26. Ban Danh Xưng (Naming Ability)";
                case 26 -> "§b§l27. Dị Giới Thần Môn (Dimensional Sanctuary)";
                default -> "§7Chưa chọn";
            };
        }
    }

    public static void executeSkill(ServerLevel level, ServerPlayer player, int index, boolean hasCreation) {
        // Kháng Ma Kết Giới (Anti-Magic Barrier) triệt tiêu toàn bộ ma pháp & kỹ năng
        // Ác ma ở trong kết giới chỉ có thể cận chiến bằng tay không
        if (AntiMagicBarrierManager.isInBarrier(level, player.position())) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Bạn đang nằm trong Kháng Ma Kết Giới! Nồng độ ma tử bằng 0, ác ma chỉ có thể đánh cận chiến bằng tay không, toàn bộ ma pháp bị phong ấn!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            return;
        }

        int rykuzinIndex = hasCreation ? 19 : 18;

        // Nếu đang bật Tiên Tri và dùng skill khác -> Tự động tắt Tiên Tri
        if (index != rykuzinIndex && RykuzinSinkaAbility.isActive(player)) {
            RykuzinSinkaAbility.deactivate(player);
            player.displayClientMessage(
                    Component.literal("§e[TIÊN TRI] §fKỹ năng Tiên Tri đã tự động tắt khi thi triển kỹ năng khác."),
                    true
            );
        }

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
                case 11 -> ReverseVelocityIaijutsuAbility.castReverseVelocityIai(level, player);
                case 12 -> ModMessages.sendToPlayer(new ClientboundOpenMaterialCreationPacket(), player);
                case 13 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
                case 14 -> MultilayerBarrierAbility.cast(level, player);
                case 15 -> ZoneTrackAbility.cast(level, player);
                case 16 -> UniversalSenseAbility.cast(level, player, 3);
                case 17 -> InfiniteDragonPrisonAbility.cast(level, player, 3);
                case 18 -> EvolvedSkillHelper.toggleUrielDefense(player);
                case 19 -> RykuzinSinkaAbility.toggle(level, player);
                case 20 -> HellflareAbility.cast(level, player);
                case 21 -> DarkFlameShotAbility.cast(level, player);
                case 22 -> NihilityCollapseAbility.cast(level, player);
                case 23 -> SupernovaAbility.cast(level, player);
                case 24 -> SkillNullificationAbility.cast(level, player);
                case 25 -> AzathothAbility.cast(level, player);
                case 26 -> TensuraNamingAbility.cast(level, player);
                case 27 -> DimensionalSanctuaryAbility.cast(level, player);
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
                case 11 -> ReverseVelocityIaijutsuAbility.castReverseVelocityIai(level, player);
                case 12 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
                case 13 -> MultilayerBarrierAbility.cast(level, player);
                case 14 -> ZoneTrackAbility.cast(level, player);
                case 15 -> UniversalSenseAbility.cast(level, player, 3);
                case 16 -> InfiniteDragonPrisonAbility.cast(level, player, 3);
                case 17 -> EvolvedSkillHelper.toggleUrielDefense(player);
                case 18 -> RykuzinSinkaAbility.toggle(level, player);
                case 19 -> HellflareAbility.cast(level, player);
                case 20 -> DarkFlameShotAbility.cast(level, player);
                case 21 -> NihilityCollapseAbility.cast(level, player);
                case 22 -> SupernovaAbility.cast(level, player);
                case 23 -> SkillNullificationAbility.cast(level, player);
                case 24 -> AzathothAbility.cast(level, player);
                case 25 -> TensuraNamingAbility.cast(level, player);
                case 26 -> DimensionalSanctuaryAbility.cast(level, player);
            }
        }
    }
}
