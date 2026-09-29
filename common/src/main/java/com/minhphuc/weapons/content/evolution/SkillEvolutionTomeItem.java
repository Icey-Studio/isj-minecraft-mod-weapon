package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.client.gui.ClientSkillEvolutionOpener;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Quyển Thư Tiến Hóa Kỹ Năng (Tome of Skill Evolution)
 * Bảo vật tối cổ không thể chế tạo, chỉ tìm thấy trong rương thế giới hoặc sáng tạo.
 * Cho phép quét và dung hợp 2 kỹ năng của người chơi thành 1 Kỹ Năng Tối Thượng (Mức 1-3).
 */
public class SkillEvolutionTomeItem extends Item {

    public SkillEvolutionTomeItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.4F, 0.9F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 0.95F);

            com.minhphuc.weapons.network.ModMessages.sendToPlayer(
                    new com.minhphuc.weapons.network.ClientboundOpenSkillEvolutionScreenPacket(
                            new java.util.ArrayList<>(EvolvedSkillHelper.getConsumedSkills(sp)),
                            EvolvedSkillHelper.getEvolvedSkillId(sp),
                            EvolvedSkillHelper.getEvolvedSkillTier(sp),
                            EvolvedSkillHelper.getEvolvedSkillTiers(sp)
                    ), sp);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l✦ QUYỂN THƯ TIẾN HÓA KỸ NĂNG ✦"));
        tooltip.add(Component.literal("§7Bảo điển cổ xưa chứa đựng bí thuật dung hợp các Kỹ Năng Tối Thượng."));
        tooltip.add(Component.literal("§7(Độ hiếm cực cao - Chỉ tìm thấy trong các siêu di tích thế giới cổ đại)"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§e▶ Nhấn §a[Chuột Phải] §eđể mở giao diện dung hợp kỹ năng."));
        tooltip.add(Component.literal("§a✔ Các kỹ năng đem dung hợp §lKHÔNG BỊ MẤT§a và vẫn sử dụng được bình thường."));
        tooltip.add(Component.literal("§6★ Có thể sở hữu và nâng cấp toàn bộ 4 Kỹ Năng Tối Thượng lên Cấp 3!"));
        tooltip.add(Component.literal("§b• Phím §f[Z]§b: Luân chuyển nhanh giữa toàn bộ các kỹ năng đã thức tỉnh."));
        tooltip.add(Component.literal("§c⚠️ Kỹ năng tiến hóa sẽ mất nếu bạn tử vong trong trận chiến!"));
    }
}
