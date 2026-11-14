package com.minhphuc.weapons.content.tensura;

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
        return hasCreation ? 30 : 29;
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
                case 4 -> "§4§l5. Ngày Tận Thế (Apocalypse)";
                case 5 -> "§5§l6. Bạo thực vương (Beelzebuth)";
                case 6 -> "§c§l7. Phẫn Nộ Vương: Long Tinh Bộc Viêm Bá (Dragon Nova)";
                case 7 -> "§e§l8. Thần Nộ: Tuyệt Diệt Tinh Tú (Extinction Stars)";
                case 8 -> "§2§l9. Thị Thực (Seer Flesh)";
                case 9 -> "§3§l10. Đánh Dấu (Spatial Mark)";
                case 10 -> "§b§l11. Dịch Chuyển (Spatial Teleportation)";
                case 11 -> "§6§l12. Cú Bắn Granit (Granite Blast)";
                case 12 -> "§f§l13. Nghịch Thời Nhất Trảm (Reverse-Time Slash)";
                case 13 -> "§c§l14. Bạt Đao Thuật Vận Tốc Ngược (Reverse-Velocity Iaijutsu)";
                case 14 -> "§6§l15. Sáng Tạo Vật Chất: Ngưng Tụ Thần Khí (Material Creation)";
                case 15 -> "§a§l16. Kháng Ma Kết Giới (Anti-Magic Barrier)";
                case 16 -> "§b§l17. Đa Trùng Kết Giới (Multilayer Barrier)";
                case 17 -> "§f§l18. Zone Track (Pure White Beam)";
                case 18 -> "§b§l19. Cảm Nhận Vạn Năng (Universal Sense)";
                case 19 -> "§6§l20. Lồng Giam Vô Hạn (Infinite Dragon Prison)";
                case 20 -> "§e§l21. Phòng Ngự Tuyệt Đối Của Uriel (" + (urielActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 21 -> "§e§l22. Tiên Tri: Rykuzin Sinka (" + (rykuzinActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 22 -> "§4§l23. Hắc Viêm Ngục (Hellflare)";
                case 23 -> "§c§l24. Hắc Viêm Hỏa Cầu (Dark Flame Shot)";
                case 24 -> "§4§l25. Lời Chúc Tử Vong (Death Blessing)";
                case 25 -> "§6§l26. Siêu Tân Tinh (Supernova)";
                case 26 -> "§c§l27. Phong Sát Kĩ Năng (Skill Nullification)";
                case 27 -> "§5§l28. Thần Hư Không Azathoth (Void God Azathoth)";
                case 28 -> "§6§l29. Ban Danh Xưng (Naming Ability)";
                case 29 -> "§b§l30. Dị Giới Thần Môn (Dimensional Sanctuary)";
                default -> "§7Chưa chọn";
            };
        } else {
            return switch (index) {
                case 0 -> "§e§l1. Linh Tử Băng Hoại (Disintegration)";
                case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
                case 2 -> "§a§l3. Phép Màu Của Thần Linh (Miracle of the Gods)";
                case 3 -> "§d§l4. Bí Ẩn Của Đức Tin Và Ân Sủng (Faith and Grace)";
                case 4 -> "§4§l5. Ngày Tận Thế (Apocalypse)";
                case 5 -> "§5§l6. Bạo thực vương (Beelzebuth)";
                case 6 -> "§c§l7. Phẫn Nộ Vương: Long Tinh Bộc Viêm Bá (Dragon Nova)";
                case 7 -> "§e§l8. Thần Nộ: Tuyệt Diệt Tinh Tú (Extinction Stars)";
                case 8 -> "§2§l9. Thị Thực (Seer Flesh)";
                case 9 -> "§3§l10. Đánh Dấu (Spatial Mark)";
                case 10 -> "§b§l11. Dịch Chuyển (Spatial Teleportation)";
                case 11 -> "§6§l12. Cú Bắn Granit (Granite Blast)";
                case 12 -> "§f§l13. Nghịch Thời Nhất Trảm (Reverse-Time Slash)";
                case 13 -> "§c§l14. Bạt Đao Thuật Vận Tốc Ngược (Reverse-Velocity Iaijutsu)";
                case 14 -> "§a§l15. Kháng Ma Kết Giới (Anti-Magic Barrier)";
                case 15 -> "§b§l16. Đa Trùng Kết Giới (Multilayer Barrier)";
                case 16 -> "§f§l17. Zone Track (Pure White Beam)";
                case 17 -> "§b§l18. Cảm Nhận Vạn Năng (Universal Sense)";
                case 18 -> "§6§l19. Lồng Giam Vô Hạn (Infinite Dragon Prison)";
                case 19 -> "§e§l20. Phòng Ngự Tuyệt Đối Của Uriel (" + (urielActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 20 -> "§e§l21. Tiên Tri: Rykuzin Sinka (" + (rykuzinActive ? "§a§lBẬT§e§l" : "§c§lTẮT§e§l") + ")";
                case 21 -> "§4§l22. Hắc Viêm Ngục (Hellflare)";
                case 22 -> "§c§l23. Hắc Viêm Hỏa Cầu (Dark Flame Shot)";
                case 23 -> "§4§l24. Lời Chúc Tử Vong (Death Blessing)";
                case 24 -> "§6§l25. Siêu Tân Tinh (Supernova)";
                case 25 -> "§c§l26. Phong Sát Kĩ Năng (Skill Nullification)";
                case 26 -> "§5§l27. Thần Hư Không Azathoth (Void God Azathoth)";
                case 27 -> "§6§l28. Ban Danh Xưng (Naming Ability)";
                case 28 -> "§b§l29. Dị Giới Thần Môn (Dimensional Sanctuary)";
                default -> "§7Chưa chọn";
            };
        }
    }

    /**
     * LỌC KỸ NĂNG CÓ THỂ HOẠT ĐỘNG TRONG KHÁNG MA KẾT GIỚI:
     * - Kháng Ma Kết Giới triệt tiêu nồng độ Ma Tố (Magicules) của ma vật và ác ma.
     * - Các kỹ năng được phép sử dụng:
     *   1. Phong Sát Kỹ Năng (Skill Nullification) - Phá vỡ và giải trừ kết giới từ bên trong!
     *   2. Thánh Pháp Thuật Linh Tử (Disintegration, Jacob's Ladder, Miracle of the Gods, Faith & Grace) - Hạt Linh Tử nhỏ hơn ma tố, xuyên thủng kết giới theo chuẩn nguyên tác Tensura!
     *   3. Kiếm Thuật Thuần Vật Lý (Nghịch Thời Nhất Trảm, Bạt Đao Thuật Vận Tốc Ngược) - Động năng và thể lực cơ bắp thuần túy.
     *   4. Giác Quan & Tâm Trí (Tiên Tri Rykuzin Sinka, Cảm Nhận Vạn Năng) - Sóng não và nhận thức sinh học.
     *   5. Dị Giới Thần Môn & Sáng Tạo Vật Chất - Cổng không gian thoát hiểm khẩn cấp và đúc vũ khí.
     */
    public static boolean isSkillAllowedInAntiMagicBarrier(int index, boolean hasCreation) {
        // Nhóm 1: Thánh Thuật Linh Tử (Spiritrons)
        if (index >= 0 && index <= 3) return true;

        // Nhóm 2: Kiếm Thuật Vật Lý & Đao Pháp
        if (index == 12 || index == 13) return true;

        if (hasCreation) {
            if (index == 14) return true; // Sáng Tạo Vật Chất
            if (index == 18) return true; // Cảm Nhận Vạn Năng
            if (index == 21) return true; // Tiên Tri: Rykuzin Sinka
            if (index == 26) return true; // Phong Sát Kỹ Năng
            if (index == 29) return true; // Dị Giới Thần Môn
        } else {
            if (index == 17) return true; // Cảm Nhận Vạn Năng
            if (index == 20) return true; // Tiên Tri: Rykuzin Sinka
            if (index == 25) return true; // Phong Sát Kỹ Năng
            if (index == 28) return true; // Dị Giới Thần Môn
        }
        return false;
    }

    public static int getCooldownTicksForSkill(int index, boolean hasCreation) {
        int adj = (!hasCreation && index >= 14) ? index + 1 : index;
        return switch (adj) {
            case 9, 14, 20, 21, 28, 29 -> 0; // Đánh dấu, Sáng tạo, Toggle Uriel/Tiên tri, Ban danh, Cổng
            case 10, 18 -> 40; // Dịch chuyển, Cảm nhận vạn năng
            case 4, 6, 24, 25, 27 -> 120; // Tận thế, Dragon Nova, Death Blessing, Supernova, Azathoth
            case 0, 5, 19, 22, 26 -> 80; // Disintegration, Beelzebuth, Infinite Prison, Hellflare, Phong sát
            default -> 30;
        };
    }

    public static void executeSkill(ServerLevel level, ServerPlayer player, int index, boolean hasCreation) {
        // Kháng Ma Kết Giới (Anti-Magic Barrier) triệt tiêu ma pháp & ma tố:
        // LỌC KỸ NĂNG: Chỉ cho phép các kỹ năng KHÔNG dùng ma tố hoạt động:
        if (AntiMagicBarrierManager.isInBarrier(level, player.position())) {
            if (!isSkillAllowedInAntiMagicBarrier(index, hasCreation)) {
                player.displayClientMessage(
                        Component.literal("§c⚠️ [KHÁNG MA KẾT GIỚI] Kỹ năng này sử dụng Ma Tố nên bị phong ấn! Bạn chỉ có thể dùng: §ePhong Sát Kỹ Năng (phá kết giới), Thánh Thuật Linh Tử (Disintegration, Jacob), Kiếm Thuật Vật Lý (Bạt Đao, Nghịch Thời), Tiên Tri, hoặc Dị Giới Thần Môn!"),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                return;
            }
        }

        // Kiểm tra trạng thái bị Phong Sát Kỹ Năng (Skill Nullification Seal)
        if (SkillNullificationAbility.isPlayerSealed(player)) {
            // Khi bị Phong Sát, chỉ còn có thể dùng Kiếm Thuật Thuần Vật Lý
            if (index != 12 && index != 13) {
                player.displayClientMessage(
                        Component.literal("§c⚠️ Bạn đang bị Phong Sát Kỹ Năng! Toàn bộ ma pháp bị phong ấn, chỉ có thể sử dụng Kiếm Thuật Vật Lý!"),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                return;
            }
        }

        int rykuzinIndex = hasCreation ? 21 : 20;

        // Nếu đang bật Tiên Tri và dùng skill khác -> Tự động tắt Tiên Tri
        if (index != rykuzinIndex && RykuzinSinkaAbility.isActive(player)) {
            RykuzinSinkaAbility.deactivate(player);
            player.displayClientMessage(
                    Component.literal("§e[TIÊN TRI] §fKỹ năng Tiên Tri đã tự động tắt khi thi triển kỹ năng khác."),
                    true
            );
        }

        // Nếu đang kích hoạt Tuyệt Diệt Tinh Tú thì chỉ cho phép kết hợp kích hoạt Thị Thực (Chiêu 9 - index 8)
        if (index != 7 && index != 8 && TaisuiExtinctionStarsAbility.isTaisuiActive(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Đang trong trạng thái Tuyệt Diệt Tinh Tú! Chỉ có thể kết hợp kích hoạt Thị Thực!"),
                    true
            );
            return;
        }

        // Kiểm tra & kích hoạt Cooldown theo từng kỹ năng (ở chế độ Sinh Tồn)
        int cdTicks = getCooldownTicksForSkill(index, hasCreation);
        if (cdTicks > 0 && !TensuraSkillHelper.checkAndStartCooldown(player, "lord_" + index, cdTicks, getSkillName(player, index, hasCreation))) {
            return;
        }

        if (hasCreation) {
            switch (index) {
                case 0 -> SanctuaryDisintegrationAbility.cast(level, player, ItemStack.EMPTY);
                case 1 -> JacobsLadderAbility.cast(level, player, ItemStack.EMPTY);
                case 2 -> PurificationPillarAbility.cast(level, player, ItemStack.EMPTY);
                case 3 -> FaithAndGraceAbility.cast(level, player);
                case 4 -> ApocalypseAbility.cast(level, player);
                case 5 -> BeelzebuthAbility.executeBeelzebuth(level, player);
                case 6 -> DragonNovaAbility.cast(level, player);
                case 7 -> TaisuiExtinctionStarsAbility.cast(level, player);
                case 8 -> SeerFleshAbility.cast(level, player);
                case 9 -> SpatialTeleportAbility.castMark(level, player);
                case 10 -> SpatialTeleportAbility.castTeleport(level, player);
                case 11 -> HorizontalHolyBeamAbility.cast(level, player);
                case 12 -> TengaiKyuseiSwordAbility.castReverseTimeSlash(level, player);
                case 13 -> ReverseVelocityIaijutsuAbility.castReverseVelocityIai(level, player);
                case 14 -> ModMessages.sendToPlayer(new ClientboundOpenMaterialCreationPacket(), player);
                case 15 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
                case 16 -> MultilayerBarrierAbility.cast(level, player);
                case 17 -> ZoneTrackAbility.cast(level, player);
                case 18 -> UniversalSenseAbility.cast(level, player, 3);
                case 19 -> InfiniteDragonPrisonAbility.cast(level, player, 3);
                case 20 -> EvolvedSkillHelper.toggleUrielDefense(player);
                case 21 -> RykuzinSinkaAbility.toggle(level, player);
                case 22 -> HellflareAbility.cast(level, player);
                case 23 -> DarkFlameShotAbility.cast(level, player);
                case 24 -> NihilityCollapseAbility.cast(level, player);
                case 25 -> SupernovaAbility.cast(level, player);
                case 26 -> SkillNullificationAbility.cast(level, player);
                case 27 -> AzathothAbility.cast(level, player);
                case 28 -> TensuraNamingAbility.cast(level, player);
                case 29 -> DimensionalSanctuaryAbility.cast(level, player);
            }
        } else {
            switch (index) {
                case 0 -> SanctuaryDisintegrationAbility.cast(level, player, ItemStack.EMPTY);
                case 1 -> JacobsLadderAbility.cast(level, player, ItemStack.EMPTY);
                case 2 -> PurificationPillarAbility.cast(level, player, ItemStack.EMPTY);
                case 3 -> FaithAndGraceAbility.cast(level, player);
                case 4 -> ApocalypseAbility.cast(level, player);
                case 5 -> BeelzebuthAbility.executeBeelzebuth(level, player);
                case 6 -> DragonNovaAbility.cast(level, player);
                case 7 -> TaisuiExtinctionStarsAbility.cast(level, player);
                case 8 -> SeerFleshAbility.cast(level, player);
                case 9 -> SpatialTeleportAbility.castMark(level, player);
                case 10 -> SpatialTeleportAbility.castTeleport(level, player);
                case 11 -> HorizontalHolyBeamAbility.cast(level, player);
                case 12 -> TengaiKyuseiSwordAbility.castReverseTimeSlash(level, player);
                case 13 -> ReverseVelocityIaijutsuAbility.castReverseVelocityIai(level, player);
                case 14 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
                case 15 -> MultilayerBarrierAbility.cast(level, player);
                case 16 -> ZoneTrackAbility.cast(level, player);
                case 17 -> UniversalSenseAbility.cast(level, player, 3);
                case 18 -> InfiniteDragonPrisonAbility.cast(level, player, 3);
                case 19 -> EvolvedSkillHelper.toggleUrielDefense(player);
                case 20 -> RykuzinSinkaAbility.toggle(level, player);
                case 21 -> HellflareAbility.cast(level, player);
                case 22 -> DarkFlameShotAbility.cast(level, player);
                case 23 -> NihilityCollapseAbility.cast(level, player);
                case 24 -> SupernovaAbility.cast(level, player);
                case 25 -> SkillNullificationAbility.cast(level, player);
                case 26 -> AzathothAbility.cast(level, player);
                case 27 -> TensuraNamingAbility.cast(level, player);
                case 28 -> DimensionalSanctuaryAbility.cast(level, player);
            }
        }
    }
}
