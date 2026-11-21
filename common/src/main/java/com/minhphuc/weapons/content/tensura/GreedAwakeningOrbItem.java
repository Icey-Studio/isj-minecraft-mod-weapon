package com.minhphuc.weapons.content.tensura;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * VẬT PHẨM: BẢO NGỌC THAM LAM (GREED AWAKENING ORB)
 * Cho phép người chơi thức tỉnh trực tiếp Kỹ Năng Độc Nhất [Kẻ Tham Lam - Greedy One]
 * mà không cần phải tìm kiếm và đánh bại Mariabell Rosso.
 */
public class GreedAwakeningOrbItem extends Item {

    public GreedAwakeningOrbItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Ánh kim hào quang hồng tím
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (GreedAbility.isUnlocked(sp)) {
                sp.displayClientMessage(Component.literal("§d[Kẻ Tham Lam] §fBạn đã thức tỉnh Kỹ Năng Độc Nhất này rồi!"), true);
                return InteractionResultHolder.pass(stack);
            }

            GreedAbility.unlockForPlayer(sp);
            if (!sp.isCreative()) {
                stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d§l✦ BẢO NGỌC DỤC VỌNG ROSSO ✦"));
        tooltip.add(Component.literal("§fChứa đựng nguyên vẹn hạt giống bản thể của §eKỹ Năng Độc Nhất: Kẻ Tham Lam§f."));
        tooltip.add(Component.literal("§7Khi sử dụng, linh hồn sẽ lập tức đồng hóa và thức tỉnh:"));
        tooltip.add(Component.literal("  §f• §dMê Hoặc Dục Vọng: §7Tẩy não đàn quái trong 22m thành con rối."));
        tooltip.add(Component.literal("  §f• §dXiềng Xích Đoạt Mệnh: §7Khóa chân & hút 25% HP + Ma Tố."));
        tooltip.add(Component.literal("  §f• §dSụp Đổ Tâm Trí: §7Kích nổ quá tải dục vọng gây sát thương chuẩn."));
        tooltip.add(Component.literal("  §f• §6Nội Tại Tham Lam: §7x2 EXP & tăng tỷ lệ cướp trang bị vào Dạ Dày."));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§e[Chuột Phải] §ađể thức tỉnh ngay lập tức (Không cần đánh Boss)!"));
    }
}
