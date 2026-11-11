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

    public static ItemStack createSoulFor(net.minecraft.world.entity.LivingEntity victim) {
        if (victim == null) return ItemStack.EMPTY;
        net.minecraft.world.item.Item soulItem;
        if (victim instanceof com.minhphuc.weapons.entity.tensura.MilimEntity) {
            soulItem = com.minhphuc.weapons.init.ModItems.MILIM_SOUL.get();
        } else if (victim instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity) {
            soulItem = com.minhphuc.weapons.init.ModItems.VELGRYND_SOUL.get();
        } else if (victim instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity) {
            soulItem = com.minhphuc.weapons.init.ModItems.VELZARD_SOUL.get();
        } else if (victim instanceof com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity) {
            soulItem = com.minhphuc.weapons.init.ModItems.PRIMORDIAL_DEMON_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.monster.warden.Warden) {
            soulItem = com.minhphuc.weapons.init.ModItems.WARDEN_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            soulItem = com.minhphuc.weapons.init.ModItems.ENDER_DRAGON_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            soulItem = com.minhphuc.weapons.init.ModItems.WITHER_SOUL.get();
        } else {
            soulItem = com.minhphuc.weapons.init.ModItems.MOB_SOUL.get();
        }

        ItemStack soulStack = new ItemStack(soulItem);
        String entityTypeId = net.minecraft.world.entity.EntityType.getKey(victim.getType()).toString();
        com.minhphuc.weapons.data.ItemStackDataHelper.putString(soulStack, "EntityTypeId", entityTypeId);

        boolean isBaby = false;
        if (victim instanceof net.minecraft.world.entity.AgeableMob ageable) {
            isBaby = ageable.isBaby();
        }
        com.minhphuc.weapons.data.ItemStackDataHelper.putBoolean(soulStack, "IsBaby", isBaby);

        if (victim.hasCustomName() && victim.getCustomName() != null) {
            com.minhphuc.weapons.data.ItemStackDataHelper.putString(soulStack, "CustomName", victim.getCustomName().getString());
        }

        String entityName = victim.getName().getString();
        com.minhphuc.weapons.data.ItemStackDataHelper.putString(soulStack, "EntityDisplayName", entityName);
        soulStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("§b§lLinh Hồn: §e" + entityName + (isBaby ? " §d(Con Nhỏ)" : " §a(Trưởng Thành)")));
        return soulStack;
    }
}
