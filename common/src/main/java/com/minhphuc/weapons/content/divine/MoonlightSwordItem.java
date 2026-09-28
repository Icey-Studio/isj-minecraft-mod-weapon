package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.content.tensura.BeelzebuthAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Unbreakable;

public class MoonlightSwordItem extends Item {
    public static final String NBT_SKILL = "ActiveSkill";

    public MoonlightSwordItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false; // Bất tử, không bao giờ mất độ bền
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Hào quang Nguyệt Quang Thần Thoại
    }

    public static String getSkillName(int skillOrdinal) {
        return switch (skillOrdinal) {
            case 0 -> "§e§l1. Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Multi-Tier Disintegration)";
            case 1 -> "§b§l2. Tà Khứ Vũ Thê Tử (Jacob's Ladder)";
            case 2 -> "§6§l3. Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)";
            case 3 -> "§a§l4. Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification)";
            case 4 -> "§d§l5. Bạo Thực Vương: Thôn Phệ (Predator)";
            case 5 -> "§d§l6. Long Tinh Bộc Viêm Bá: Dragon Nova (竜星爆炎覇)";
            case 6 -> "§e§l7. Phẫn Nộ Vương: Tuyệt Diệt Tinh Tú (Extinction Stars)";
            case 7 -> "§b§l8. Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm Thần Khóa)";
            case 8 -> "§c§l9. Thị Nhục - Nhục Thể Bất Tử Thái Tuế (Seer Flesh)";
            case 9 -> "§4§l10. Diệt Thế Tà Tinh: Alkaid (ALKAID)";
            case 10 -> "§6§l11. Cú Bắn Granit (Granite Blast)";
            case 11 -> "§b§l12. Trí Huệ Chi Vương: Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo (Ciel)";
            case 12 -> "§a§l13. Trí Huệ Chi Vương: Thẩm Định Vạn Vật (All of Creation)";
            case 13 -> "§4§l14. Kiêu Ngạo Vương Lucifer: Sao Chép Tuyệt Kỹ (Replication)";
            default -> "§7Chưa chọn";
        };
    }

    public static void cycleSkill(ServerPlayer player, ItemStack stack) {
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        int maxSkills = isTrueDemonLord ? 14 : 4;

        int current = ItemStackDataHelper.getInt(stack, NBT_SKILL);
        int next = (current + 1) % maxSkills;
        ItemStackDataHelper.putInt(stack, NBT_SKILL, next);

        player.displayClientMessage(
            Component.literal("§6§l[NGUYỆT QUANG THẦN TẾ KIẾM] §fBáo cáo. Chế độ: " + getSkillName(next) + " §7(Chuột Phải để dùng)"),
            true
        );

        float pitch = 1.0F + (next * 0.15F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, pitch);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            int skill = ItemStackDataHelper.getInt(stack, NBT_SKILL);

            // Kiểm tra nếu đang kích hoạt Tuyệt Diệt Tinh Tú thì chỉ cho phép kết hợp kích hoạt Thị Nhục (Chiêu 9 - index 8) hoặc Alkaid (Chiêu 10 - index 9)
            if (skill != 6 && skill != 8 && skill != 9 && com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.isTaisuiActive(serverPlayer)) {
                serverPlayer.displayClientMessage(
                    Component.literal("§c⚠️ Đang trong trạng thái Tuyệt Diệt Tinh Tú! Chỉ có thể kết hợp kích hoạt Thị Nhục hoặc Diệt Thế Tà Tinh (Alkaid)!"),
                    true
                );
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            if (skill == 0) {
                // Chiêu 1: Tam Trọng Thánh Giới - Linh Tử Băng Hoại
                SanctuaryDisintegrationAbility.cast(serverLevel, serverPlayer, stack);
            } else if (skill == 1) {
                // Chiêu 2: Tà Khứ Vũ Thê Tử (Jacob's Ladder)
                JacobsLadderAbility.cast(serverLevel, serverPlayer, stack);
            } else if (skill == 2) {
                // Chiêu 3: Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)
                HeavenlyJudgmentArrayAbility.cast(serverLevel, serverPlayer, stack);
            } else if (skill == 3) {
                // Chiêu 4: Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification)
                PurificationPillarAbility.cast(serverLevel, serverPlayer, stack);
            } else if (skill == 4) {
                // Chiêu 5: Bạo Thực Vương: Thôn Phệ (Dành cho Chân Ma Vương)
                BeelzebuthAbility.executeBeelzebuth(serverLevel, serverPlayer);
                player.getCooldowns().addCooldown(this, 30);
            } else if (skill == 5) {
                // Chiêu 6: Long Tinh Bộc Viêm Bá: Dragon Nova (Yêu cầu Chân Ma Vương + Giáp Thần Linh)
                com.minhphuc.weapons.content.tensura.DragonNovaAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 6) {
                // Chiêu 7: Phẫn Nộ Vương - Tuyệt Diệt Tinh Tú (Extinction Stars - Thái Tuế Tinh Quân)
                com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 7) {
                // Chiêu 8: Lớp Phòng Ngự Lục Nhậm Thần Khóa (Bật / Tắt chủ động)
                com.minhphuc.weapons.content.darkgathering.LiuRenBarrierAbility.toggleBarrier(serverLevel, serverPlayer);
            } else if (skill == 8) {
                // Chiêu 9: Thị Nhục (Seer Flesh) - Thái Tuế Tinh Quân
                com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 9) {
                // Chiêu 10: Diệt Thế Tà Tinh - Alkaid (Yêu cầu đang bật Tuyệt Diệt Tinh Tú)
                com.minhphuc.weapons.content.darkgathering.AlkaidAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 10) {
                // Chiêu 11: Cú Bắn Granit (Granite Blast)
                com.minhphuc.weapons.content.tensura.HorizontalHolyBeamAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 11) {
                // Chiêu 12: Trí Huệ Chi Vương - Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo
                com.minhphuc.weapons.content.tensura.ThoughtAccelerationAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 12) {
                // Chiêu 13: Trí Huệ Chi Vương - Thẩm Định Vạn Vật
                com.minhphuc.weapons.content.tensura.AllOfCreationAbility.cast(serverLevel, serverPlayer);
            } else if (skill == 13) {
                // Chiêu 14: Kiêu Ngạo Vương Lucifer - Sao Chép Tuyệt Kỹ
                com.minhphuc.weapons.content.tensura.LuciferReplicationAbility.cast(serverLevel, serverPlayer);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int currentSkill = ItemStackDataHelper.getInt(stack, NBT_SKILL);

        tooltip.add(Component.literal("§6§l[VŨ KHÍ THẦN THOẠI]"));
        tooltip.add(Component.literal("§b§lNguyệt Quang Thần Tế Kiếm (Moonlight Ritual Sword)"));
        tooltip.add(Component.literal("§7Bảo kiếm hộ vệ thánh điện của Thánh Kỵ sĩ Hinata."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Đặc Tính Thần Thoại:"));
        tooltip.add(Component.literal("§7- §aChém thường 1 phát kết liễu quái thường & phá vỡ toàn bộ trang bị đối thủ"));
        tooltip.add(Component.literal("§7- §aTrảm Boss (Rồng Ender, Wither, Warden): Đòn 1 rút 80% HP, đòn 2 tất sát"));
        tooltip.add(Component.literal("§7- §aĐộ bền bất tử, không bao giờ bị phá hủy"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6⚡ Kỹ Năng Đang Chọn: " + getSkillName(currentSkill)));
        tooltip.add(Component.literal("§7- Nhấn phím §e[Z] §7để chuyển đổi thứ tự chiêu thức:"));
        tooltip.add(Component.literal("§7   + §e1. Tam Trọng Thánh Giới: §fMa Pháp Trận 3 tầng giam cầm & Cột Thiên Phạt phân rã"));
        tooltip.add(Component.literal("§7   + §b2. Tà Khứ Vũ Thê Tử: §fCột sáng 4x4 chọc trời, phá hủy địa hình & diệt trừ nguyền rủa"));
        tooltip.add(Component.literal("§7   + §63. Bát Môn Thiên Phạt Trận: §fMa trận 7 cột sáng vây hãm, lốc xoáy & kích nổ hủy diệt"));
        tooltip.add(Component.literal("§7   + §a4. Đại Thánh Tẩy: §fThánh trụ cứu rỗi, hồi máu toàn diện, chuyển hóa Zombie/Witch thành Dân Làng"));
        tooltip.add(Component.literal("§7   + §d5..8. Kỹ Năng Tối Thượng Ma Vương: §fBeelzebuth, Dragon Nova, Tinh Tú, Lục Nhậm Thần Khóa"));
        tooltip.add(Component.literal("§7   + §c9. Thị Nhục (Seer Flesh): §fKhối thịt lúc nhúc 12 mắt, hồi 100% HP, xóa độc & rạch mắt trị liệu"));
        tooltip.add(Component.literal("§7   + §410. Diệt Thế Tà Tinh (Alkaid): §fTất sát tà tinh tụ 8s tạo đại cầu phân rã!"));
        tooltip.add(Component.literal("§7   + §611. Cú Bắn Granit (Granite Blast): §fĐại pháo năng lượng chấn thiên bắn ngang xuyên toạc không gian!"));
        tooltip.add(Component.literal("§7- Nhấn §a[Chuột Phải] §7để thi triển kỹ năng đã chọn"));
    }
}
