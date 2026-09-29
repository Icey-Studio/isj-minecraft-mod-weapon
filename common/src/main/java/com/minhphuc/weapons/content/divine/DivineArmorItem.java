package com.minhphuc.weapons.content.divine;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Unbreakable;

public class DivineArmorItem extends ArmorItem {

    public DivineArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false))); // Ẩn tag unbreaking và bất tử
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false; // Bất tử, không bao giờ hiển thị thanh độ bền
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Hào quang ma thuật thần thoại
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Kiểm tra nếu người chơi đang mang bất kỳ mảnh giáp Thần Linh nào
            boolean isWearingThisPiece = player.getItemBySlot(this.type.getSlot()) == stack;
            if (isWearingThisPiece) {
                // 1. Dập tắt lửa lập tức (Kháng lửa)
                player.clearFire();

                // 2. Triệt tiêu khoảng cách rơi (Kháng rơi từ trên cao)
                player.resetFallDistance();

                // LƯU Ý THEO YÊU CẦU:
                // - Vẫn KHÔNG thở được dưới nước (không can thiệp AirSupply)
                // - Vẫn bị trúng Độc (Poison) bình thường
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    public static boolean isWearingAnyPiece(Player player) {
        if (player == null) return false;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (player.getItemBySlot(slot).getItem() instanceof DivineArmorItem) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWearingFullSet(Player player) {
        if (player == null) return false;
        // Chỉ cần mặc đủ Áo Giáp (CHEST) và Quần (LEGS) là nhận đủ 100% sức mạnh thần thoại
        // Mũ và Giày là trang bị làm đẹp bổ trợ, không bắt buộc
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof DivineArmorItem
                && player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof DivineArmorItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l[TRANG BỊ THẦN THOẠI]"));
        tooltip.add(Component.literal("§d§lThần Linh Vũ Trang (Divine Armament)"));
        tooltip.add(Component.literal("§7Thánh giáp hộ thể huyền thoại của Hiệp sĩ Thánh Điện."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Đặc Tính Thần Thánh:"));
        tooltip.add(Component.literal("§7- §aChỉ cần mặc đủ §eÁo Giáp §a& §eQuần §alà kích hoạt trọn vẹn 100% Thần Lực! (Mũ & Giày chỉ để làm đẹp)"));
        tooltip.add(Component.literal("§7- §aKháng gần như 100% sát thương từ sinh vật, ngã từ trên cao, lửa & dung nham"));
        tooltip.add(Component.literal("§7- §aĐộ bền bất tử, không bao giờ bị phá hủy"));
        tooltip.add(Component.literal("§7- §dKhai mở Tuyệt Kỹ: §eLong Tinh Bộc Viêm Bá (Dragon Nova) §fcủa Milim (Không cần hồi năng lượng)"));
        tooltip.add(Component.literal("§7- §cNhược điểm: Vẫn nhận sát thương trúng độc & không thể thở dưới nước"));
    }
}
