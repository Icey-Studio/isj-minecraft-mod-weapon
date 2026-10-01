package com.minhphuc.weapons.content.tensura;


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
 * Sách Cổ Khởi Nguyên Thủy Tổ (Tome of the Primordials)
 * Bảo vật phong ấn cấm thuật tối cổ, cho phép người chơi chọn chuyển sinh thành 1 trong 7 Ác Ma Thủy Tổ.
 */
public class PrimordialRebirthTomeItem extends Item {

    public PrimordialRebirthTomeItem(Properties properties) {
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
            com.minhphuc.weapons.client.gui.ClientPrimordialGuiOpener.openRebirthScreen();
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.4F, 0.8F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 0.9F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l✦ SÁCH CỔ KHỞI NGUYÊN THỦY TỔ ✦"));
        tooltip.add(Component.literal("§7Bảo điển chứa đựng tàn dư linh hồn của Thất Đại Thủy Tổ Ác Ma."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e▶ Nhấn §a[Chuột Phải] §eđể dung hợp và trở thành Thủy Tổ:"));
        tooltip.add(Component.literal("§c• Rouge §7(Xích Sắc - Guy Crimson)"));
        tooltip.add(Component.literal("§8• Noir §7(Hắc Sắc - Diablo) §d[Kèm Móng Vuốt]"));
        tooltip.add(Component.literal("§f• Blanc §7(Bạch Sắc - Testarossa)"));
        tooltip.add(Component.literal("§e• Jaune §7(Hoàng Sắc - Carrera) §6[Súng khi hóa Ma Vương]"));
        tooltip.add(Component.literal("§5• Violet §7(Tử Sắc - Ultima)"));
        tooltip.add(Component.literal("§9• Bleu §7(Lam Sắc - Rein)"));
        tooltip.add(Component.literal("§a• Vert §7(Lục Sắc - Misery)"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§4⚠ Cảnh báo: §7Khi ngã xuống (tử trận), sức mạnh Thủy Tổ sẽ tan biến!"));
    }
}
