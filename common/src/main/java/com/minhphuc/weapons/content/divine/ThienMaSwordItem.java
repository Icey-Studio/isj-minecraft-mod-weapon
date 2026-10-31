package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

public class ThienMaSwordItem extends Item {

    public ThienMaSwordItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            ThienMaSwordAbility.castSpaceTimeSlash(serverLevel, serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§l[THẦN KHÍ THIÊN MA]"));
        tooltip.add(Component.literal("§f§lThiên Ma Kiếm (Heavenly Demon Katana)"));
        tooltip.add(Component.literal("§7Thanh katana huyền thoại ẩn giấu nơi hầm mỏ cổ xưa."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Đặc Tính Thần Binh:"));
        tooltip.add(Component.literal("§7- §aĐộ bền bất tử, vĩnh viễn không hư hại"));
        tooltip.add(Component.literal("§7- §aLưỡi kiếm phát quang năng lượng số hóa"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6⚡ Kỹ Năng Độc Quyền: §c§lTuyệt Kĩ Chém Xuyên Không Thời Gian"));
        tooltip.add(Component.literal("§7- Nhấn §a[Chuột Phải] §7để xuất khiếu và ngưng đọng thời không"));
        tooltip.add(Component.literal("§7- Chuỗi 3 nhịp: §c[-0.02s] §7➔ §e[-0.01s] §7➔ §a[ 0.00s]"));
        tooltip.add(Component.literal("§7- Chém đoạt đồng thời §dBộ Não §7và §cTrái Tim §7của kẻ thù"));
        tooltip.add(Component.literal("§c⚠️ Kẻ mất não & tim sẽ tử vong sau §43 giây"));
        tooltip.add(Component.literal("§cnếu không được điều trị bằng Bí Mật Của Đức Tin Và Ân Sủng!"));
    }
}
