package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.content.tensura.MagiculesManager;
import com.minhphuc.weapons.content.tensura.MobMeleeSkillSystem;
import com.minhphuc.weapons.data.EntityDataHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bộ Thánh Giáp Hoàng Kim Bạch Kim Huyền Thoại của Hinata Sakaguchi (4 món).
 * Đủ tính năng 1..5 & cộng hưởng tính năng 10 với Kiếm Dead End Rainbow.
 */
public class HinataArmorItem extends ArmorItem {

    private static final ResourceLocation BOOTS_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("weapons", "hinata_boots_speed");
    private static final ResourceLocation FULL_ATK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("weapons", "hinata_thought_accel_as");

    public HinataArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
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

    public static boolean isWearingFullSet(Player player) {
        if (player == null) return false;
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HinataArmorItem
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HinataArmorItem
                && player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof HinataArmorItem
                && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof HinataArmorItem;
    }

    public static boolean hasHelmet(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HinataArmorItem;
    }

    public static boolean hasChestplate(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HinataArmorItem;
    }

    public static boolean hasLeggings(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof HinataArmorItem;
    }

    public static boolean hasBoots(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof HinataArmorItem;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) return;
        if (sp.getItemBySlot(this.type.getSlot()) != stack) return;

        // #2 Áo Giáp: Kháng cháy & đẩy lùi
        if (this.type == Type.CHESTPLATE) {
            sp.clearFire();
        }

        // #3 Quần: Thanh Lọc Thánh Điện mỗi 3 giây + tăng hồi Ma Tố
        if (this.type == Type.LEGGINGS && sp.tickCount % 60 == 0) {
            sp.removeEffect(MobEffects.POISON);
            sp.removeEffect(MobEffects.WITHER);
            sp.removeEffect(MobEffects.WEAKNESS);
            sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            sp.setTicksFrozen(0);
            MagiculesManager.restoreMagicules(sp, Math.max(4_000L, MagiculesManager.getMaxMagicules(sp) / 80L));
        }

        // #4 Giày: Miễn rơi + Shift + Nhảy lướt tàn ảnh 3D
        if (this.type == Type.BOOTS) {
            sp.resetFallDistance();
            var speedAttr = sp.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && !speedAttr.hasModifier(BOOTS_SPEED_MOD)) {
                speedAttr.addTransientModifier(new AttributeModifier(BOOTS_SPEED_MOD, 0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if (sp.isShiftKeyDown() && !sp.onGround() && sp.getDeltaMovement().y > 0.25D) {
                long now = sp.serverLevel().getGameTime();
                long lastDash = EntityDataHelper.getCustomData(sp).getLong("HinataBootDashTick");
                if (now - lastDash >= 30L) {
                    EntityDataHelper.getCustomData(sp).putLong("HinataBootDashTick", now);
                    Vec3 look = sp.getLookAngle().multiply(1.0D, 0.25D, 1.0D).normalize();
                    sp.setDeltaMovement(look.scale(1.65D).add(0, 0.2D, 0));
                    sp.hurtMarked = true;
                    MobMeleeSkillSystem.sendCqc3d(sp.serverLevel(), sp, sp.position(), look, 9, 0, 1.4F, 16);
                    sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.6F);
                }
            }
        }

        // #5 Full Set 4 món: Vận Tốc Suy Nghĩ (+35% Tốc độ đánh)
        if (this.type == Type.HELMET && isWearingFullSet(sp)) {
            var asAttr = sp.getAttribute(Attributes.ATTACK_SPEED);
            if (asAttr != null && !asAttr.hasModifier(FULL_ATK_SPEED_MOD)) {
                asAttr.addTransientModifier(new AttributeModifier(FULL_ATK_SPEED_MOD, 0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    /**
     * Dọn dẹp modifier khi người chơi tháo Giày hoặc Mũ.
     */
    public static void cleanupDetachedModifiers(ServerPlayer sp) {
        if (!hasBoots(sp)) {
            var speedAttr = sp.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.removeModifier(BOOTS_SPEED_MOD);
        }
        if (!isWearingFullSet(sp)) {
            var asAttr = sp.getAttribute(Attributes.ATTACK_SPEED);
            if (asAttr != null) asAttr.removeModifier(FULL_ATK_SPEED_MOD);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l[HUYỀN THOẠI • THÁNH KỴ SĨ]"));
        tooltip.add(Component.literal("§f§lBộ Thánh Giáp Hinata Sakaguchi"));
        tooltip.add(Component.literal(""));
        switch (this.type) {
            case HELMET -> tooltip.add(Component.literal("§e• Mắt Dự Đoán: §7Giảm §a25% §7sát thương cận chiến nhận vào."));
            case CHESTPLATE -> tooltip.add(Component.literal("§e• Thánh Y Bất Hoại: §7Kháng lửa, giảm §a55% §7sát thương & cộng hưởng Linh Tử."));
            case LEGGINGS -> tooltip.add(Component.literal("§e• Thanh Lọc Thánh Điện: §7Tự giải Độc/Khô Héo/Suy Yếu mỗi 3s + §b+20% §7hồi Ma Tố."));
            case BOOTS -> tooltip.add(Component.literal("§e• Ảo Bộ Thánh Kỵ Sĩ: §7+35% Tốc chạy, miễn rơi, §bShift + Nhảy §7lướt tàn ảnh 3D."));
            default -> {}
        }
        tooltip.add(Component.literal("§b✦ Đủ Bộ 4 Món (Vận Tốc Suy Nghĩ): §f+35% Tốc Đánh & Đỡ kiếm giảm 75% DMG."));
    }
}
