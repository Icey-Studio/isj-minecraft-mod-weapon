package com.minhphuc.weapons.content.darkgathering;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * HÌNH NHÂN GỖ BỒ ĐỀ (RỖNG) - BODHI EFFIGY (EMPTY)
 * Pháp khí phong ấn bằng gỗ Bồ Đề quấn kinh phù đỏ thẫm.
 * Dùng để thu phục và phong ấn Tốt Nghiệp Sinh Cấp S - Tà Kinh Đại Tăng (Dark Gathering).
 */
public class BodhiEffigyEmptyItem extends Item {

    public BodhiEffigyEmptyItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l[PHÁP KHÍ PHONG ẤN] §eHình Nhân Gỗ Bồ Đề (Rỗng)"));
        tooltip.add(Component.literal("§7Được chế tác từ gỗ Bồ Đề cổ thụ và quấn chỉ bùa đỏ thẫm."));
        tooltip.add(Component.literal("§7Dùng để phong ấn và giam giữ §4Tà Kinh Đại Tăng§7."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e📜 Hướng Dẫn Phong Ấn:"));
        tooltip.add(Component.literal("§71. Làm Tà Kinh Đại Tăng suy yếu xuống §c<= 20% HP§7 (<= 240 HP)"));
        tooltip.add(Component.literal("§7   hoặc làm choáng ngài bằng đòn tấn công chí mạng."));
        tooltip.add(Component.literal("§72. Cầm Hình Nhân Gỗ Rỗng nhấp §a[Chuột Phải]§7 vào ngài để phong ấn."));
    }
}

