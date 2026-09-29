package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.divine.HeavenlyJudgmentArrayAbility;
import com.minhphuc.weapons.content.divine.JacobsLadderAbility;
import com.minhphuc.weapons.content.divine.PurificationPillarAbility;
import com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility;
import com.minhphuc.weapons.content.tensura.BeelzebuthAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ServerboundCastBeelzebuthPacket {

    public ServerboundCastBeelzebuthPacket() {
    }

    public ServerboundCastBeelzebuthPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
            boolean isPrimordial = com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.isPrimordial(player);
            boolean isWearingDivineArmor = com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player);
            boolean hasEvolved = com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.hasEvolvedSkill(player);

            if (isTrueDemonLord || isPrimordial || isWearingDivineArmor || hasEvolved) {
                int selectedSkill = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
                ServerLevel serverLevel = (ServerLevel) player.level();
                boolean hasCreation = EntityDataHelper.getCustomData(player).getBoolean("TensuraMaterialCreation");
                int lordSkills = hasCreation ? 15 : 14;

                // Kiểm tra thi triển Kỹ Năng Tiến Hóa Tối Thượng
                boolean isCastingEvolved = false;
                if (hasEvolved) {
                    if (selectedSkill == 99) {
                        isCastingEvolved = true;
                    } else if (isTrueDemonLord && isPrimordial && selectedSkill == lordSkills + 5) {
                        isCastingEvolved = true;
                    } else if (isTrueDemonLord && selectedSkill == lordSkills) {
                        isCastingEvolved = true;
                    } else if (isPrimordial && selectedSkill == 5) {
                        isCastingEvolved = true;
                    } else if (isWearingDivineArmor && selectedSkill == 14) {
                        isCastingEvolved = true;
                    }
                }

                if (isCastingEvolved) {
                    com.minhphuc.weapons.content.evolution.EvolvedSkillDispatcher.cast(serverLevel, player);
                    return;
                }

                // Kỹ năng Long Tinh Bộc Viêm Bá (Chiêu 6 - index 5): Không cần hồi năng lượng / ma lực
                if (selectedSkill != 5) {
                    // Kiểm tra hồi chiêu kỹ năng (1.5 giây)
                    if (player.getCooldowns().isOnCooldown(com.minhphuc.weapons.init.ModItems.DEMON_LORD_SEED.get())) {
                        return;
                    }
                    player.getCooldowns().addCooldown(com.minhphuc.weapons.init.ModItems.DEMON_LORD_SEED.get(), 25);
                }

                if (isTrueDemonLord && isPrimordial) {
                    if (selectedSkill >= lordSkills) {
                        // Kỹ năng của Thủy Tổ Ác Ma
                        int primordialSkillIdx = selectedSkill - lordSkills;
                        com.minhphuc.weapons.content.tensura.PrimordialSkillDispatcher.castSkill(serverLevel, player, primordialSkillIdx);
                        return;
                    }
                } else if (!isTrueDemonLord && isPrimordial) {
                    // Chưa là Ma Vương nhưng là Thủy Tổ Ác Ma
                    int primordialSkillIdx = com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.getSelectedSkillIndex(player);
                    com.minhphuc.weapons.content.tensura.PrimordialSkillDispatcher.castSkill(serverLevel, player, primordialSkillIdx);
                    return;
                }

                // Thi triển Kỹ Năng (Không cần cầm kiếm)

                // Nếu đang kích hoạt Tuyệt Diệt Tinh Tú, chỉ cho phép kích hoạt thêm Thị Nhục (Chiêu 9 - index 8) hoặc Diệt Thế Tà Tinh (Chiêu 10 - index 9)
                if (selectedSkill != 6 && selectedSkill != 8 && selectedSkill != 9 && com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.isTaisuiActive(player)) {
                    player.displayClientMessage(
                        Component.literal("§c⚠️ Đang trong trạng thái Tuyệt Diệt Tinh Tú! Chỉ có thể kết hợp kích hoạt Thị Nhục hoặc Diệt Thế Tà Tinh (Alkaid)!"),
                        true
                    );
                    return;
                }

                if (hasCreation && selectedSkill == 14) {
                    // Chiêu 15: Sáng Tạo Vật Chất - Mở Giao Diện Ngưng Tụ Thần Khí
                    ModMessages.sendToPlayer(new ClientboundOpenMaterialCreationPacket(), player);
                } else if (selectedSkill == 13) {
                    // Chiêu 14: Kiêu Ngạo Vương Lucifer - Sao Chép Tuyệt Kỹ
                    com.minhphuc.weapons.content.tensura.LuciferReplicationAbility.cast(serverLevel, player);
                } else if (selectedSkill == 12) {
                    // Chiêu 13: Trí Huệ Chi Vương - Thẩm Định Vạn Vật
                    com.minhphuc.weapons.content.tensura.AllOfCreationAbility.cast(serverLevel, player);
                } else if (selectedSkill == 11) {
                    // Chiêu 12: Trí Huệ Chi Vương - Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo
                    com.minhphuc.weapons.content.tensura.ThoughtAccelerationAbility.cast(serverLevel, player);
                } else if (selectedSkill == 10) {
                    // Chiêu 11: Cú Bắn Granit (Granite Blast)
                    com.minhphuc.weapons.content.tensura.HorizontalHolyBeamAbility.cast(serverLevel, player);
                } else if (selectedSkill == 9) {
                    // Chiêu 10: Diệt Thế Tà Tinh - Alkaid (Yêu cầu đang bật Tuyệt Diệt Tinh Tú)
                    com.minhphuc.weapons.content.darkgathering.AlkaidAbility.cast(serverLevel, player);
                } else if (selectedSkill == 8) {
                    // Chiêu 9: Thị Nhục - Nhục Thể Bất Tử Thái Tuế (Seer Flesh)
                    com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.cast(serverLevel, player);
                } else if (selectedSkill == 7) {
                    // Chiêu 8: Lục Nhậm Thần Khóa - Trận Đồ Cưỡng Chế Tai Ương (Bật / Tắt chủ động)
                    com.minhphuc.weapons.content.darkgathering.LiuRenBarrierAbility.toggleBarrier(serverLevel, player);
                } else if (selectedSkill == 6) {
                    // Chiêu 7: Phẫn Nộ Vương - Tuyệt Diệt Tinh Tú (Thái Tuế Tinh Quân)
                    com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.cast(serverLevel, player);
                } else if (selectedSkill == 5) {
                    // Chiêu 6: Long Tinh Bộc Viêm Bá: Dragon Nova (Yêu cầu Giáp Thần Linh)
                    com.minhphuc.weapons.content.tensura.DragonNovaAbility.cast(serverLevel, player);
                } else if (selectedSkill == 4) {
                    // Chiêu 5: Bạo Thực Vương - Thôn Phệ
                    BeelzebuthAbility.executeBeelzebuth(serverLevel, player);
                } else if (selectedSkill == 3) {
                    // Chiêu 4: Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Không cần cầm kiếm)
                    PurificationPillarAbility.cast(serverLevel, player, ItemStack.EMPTY);
                } else if (selectedSkill == 2) {
                    // Chiêu 3: Bát Môn Thiên Phạt Trận (Không cần cầm kiếm)
                    HeavenlyJudgmentArrayAbility.cast(serverLevel, player, ItemStack.EMPTY);
                } else if (selectedSkill == 1) {
                    // Chiêu 2: Tà Khứ Vũ Thê Tử (Jacob's Ladder) (Không cần cầm kiếm)
                    JacobsLadderAbility.cast(serverLevel, player, ItemStack.EMPTY);
                } else {
                    // Chiêu 1: Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Không cần cầm kiếm)
                    SanctuaryDisintegrationAbility.cast(serverLevel, player, ItemStack.EMPTY);
                }
            } else {
                player.displayClientMessage(
                    Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Cá thể chưa thức tỉnh thành Thủy Tổ Ác Ma hoặc Chân Ma Vương!"),
                    true
                );
            }
        });
    }
}
