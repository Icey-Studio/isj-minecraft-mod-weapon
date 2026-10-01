package com.minhphuc.weapons.content.tensura;


import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class PrimordialGrimoireItem extends Item {

    public PrimordialGrimoireItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            com.minhphuc.weapons.client.gui.ClientPrimordialGuiOpener.openSummonScreen();
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.9F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§c§l✦ PHÁP ĐIỂN KHẾ ƯỚC THỦY TỔ ✦"));
        tooltip.add(Component.literal("§7Bảo vật chứa đựng cổ tự giao ước của 7 Ác Ma Nguyên Thủy."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e▶ Nhấn §a[Chuột Phải] §eđể mở giao diện Triệu Hồi:"));
        tooltip.add(Component.literal("§c• Rouge §7(Hồng Sắc)"));
        tooltip.add(Component.literal("§8• Noir §7(Hắc Sắc - Quản gia & Hắc Dực)"));
        tooltip.add(Component.literal("§f• Blanc §7(Bạch Sắc)"));
        tooltip.add(Component.literal("§e• Jaune §7(Hoàng Sắc)"));
        tooltip.add(Component.literal("§5• Violet §7(Tử Sắc)"));
        tooltip.add(Component.literal("§9• Bleu §7(Lam Sắc)"));
        tooltip.add(Component.literal("§a• Vert §7(Lục Sắc)"));
    }
}
