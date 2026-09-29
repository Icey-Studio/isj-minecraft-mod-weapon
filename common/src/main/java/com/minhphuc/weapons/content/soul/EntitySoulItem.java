package com.minhphuc.weapons.content.soul;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EntitySoulItem extends Item {

    private final SoulType soulType;

    public EntitySoulItem(SoulType soulType, Properties properties) {
        super(properties.stacksTo(64).rarity(Rarity.EPIC).fireResistant());
        this.soulType = soulType;
    }

    public SoulType getSoulType() {
        return soulType;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Linh hồn nguyên chất rơi ra từ thực thể tử trận.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§f• Nguồn Gốc: " + soulType.getColorCode() + soulType.getEntityName()));
        tooltip.add(Component.literal("§f• Kỹ Năng Kế Thừa: " + soulType.getColorCode() + soulType.getSkillName()));
        tooltip.add(Component.literal("§6• Khi Không Vong Nuốt Chửng:"));
        tooltip.add(Component.literal("   §a+70% Máu: §f+" + String.format("%.0f", soulType.getBonusHp70()) + " HP"));
        tooltip.add(Component.literal("   §9+70% Kháng/Giáp: §f+" + String.format("%.1f", soulType.getBonusArmor70()) + " Armor"));
        tooltip.add(Component.literal("   §d+ Mở khóa kỹ năng của thực thể ở mức 70% uy lực!"));
        tooltip.add(Component.literal("§7(Không Vong luôn ưu tiên bay đến thu thập linh hồn này trên chiến trường)"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.displayClientMessage(
                    Component.literal("§e§l[LINH HỒN THỰC THỂ] §fLinh hồn của " + soulType.getColorCode() + soulType.getEntityName() + "§f phát ra rung cảm kỳ bí... Không Vong đang khao khát nuốt chửng linh hồn này!"),
                    true
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
