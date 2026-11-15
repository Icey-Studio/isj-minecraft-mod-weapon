package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

/**
 * Hệ thống Phẩm Cấp & Tiến Hóa cho MỌI Vũ Khí trong game (Common -> Origin).
 * - Khi mob cầm vũ khí từ Hiếm trở lên, Đại Hiền Giả / Raphael sẽ tự động cảnh báo ở góc màn hình.
 * - Khi người chơi dùng vũ khí hạ sát quái/Boss, vũ khí tích điểm hồn để thăng cấp phẩm chất + tăng sát thương.
 */
public final class WeaponGradeHelper {

    public enum WeaponGrade {
        NORMAL("Thường", "§7", 1.0F, 0),
        RARE("Hiếm", "§9§l", 1.15F, 15),
        EPIC("Sử Thi", "§5§l", 1.35F, 45),
        LEGENDARY("Huyền Thoại", "§6§l", 1.65F, 110),
        MYTHICAL("Thần Thoại", "§c§l", 2.10F, 240),
        ORIGIN("Khởi Nguyên", "§d§l✦ ", 2.75F, 500);

        public final String title;
        public final String colorPrefix;
        public final float damageMultiplier;
        public final int killsRequired;

        WeaponGrade(String title, String colorPrefix, float damageMultiplier, int killsRequired) {
            this.title = title;
            this.colorPrefix = colorPrefix;
            this.damageMultiplier = damageMultiplier;
            this.killsRequired = killsRequired;
        }
    }

    private static final String KEY_GRADE = "TensuraWeaponGrade";
    private static final String KEY_KILLS = "TensuraWeaponKills";

    private WeaponGradeHelper() {}

    public static boolean isWeaponLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof TridentItem
                || stack.has(DataComponents.ATTRIBUTE_MODIFIERS)
                || getInnateGrade(stack) != WeaponGrade.NORMAL;
    }

    public static WeaponGrade getInnateGrade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponGrade.NORMAL;
        if (stack.is(ModItems.THIEN_MA_KATANA.get())) return WeaponGrade.ORIGIN;
        if (stack.is(ModItems.MOONLIGHT_SWORD.get())) return WeaponGrade.MYTHICAL;
        if (stack.is(ModItems.GOLDEN_GUN.get())) return WeaponGrade.LEGENDARY;
        String desc = stack.getItem().getDescriptionId().toLowerCase();
        if (desc.contains("divine") || desc.contains("origin") || desc.contains("azathoth") || desc.contains("velgrynd")) {
            return WeaponGrade.MYTHICAL;
        }
        if (desc.contains("netherite") || desc.contains("demon") || desc.contains("crimson") || desc.contains("abyss")) {
            return WeaponGrade.EPIC;
        }
        if (desc.contains("diamond")) return WeaponGrade.RARE;
        return WeaponGrade.NORMAL;
    }

    public static WeaponGrade getGrade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponGrade.NORMAL;
        WeaponGrade innate = getInnateGrade(stack);
        String saved = ItemStackDataHelper.getString(stack, KEY_GRADE);
        WeaponGrade stored = WeaponGrade.NORMAL;
        if (saved != null && !saved.isEmpty()) {
            try {
                stored = WeaponGrade.valueOf(saved);
            } catch (Exception ignored) {}
        }
        return stored.ordinal() > innate.ordinal() ? stored : innate;
    }

    public static float getBonusDamage(ItemStack stack, float baseDamage) {
        WeaponGrade grade = getGrade(stack);
        if (grade == WeaponGrade.NORMAL) return baseDamage;
        return baseDamage * grade.damageMultiplier;
    }

    /**
     * Ghi nhận hạ sát để vũ khí tiến hóa tự nhiên theo thời gian. Trả về phẩm cấp mới nếu vừa đột phá!
     */
    public static WeaponGrade recordWeaponKill(ItemStack stack, LivingEntity victim) {
        if (!isWeaponLike(stack)) return null;
        int gain = (victim != null && victim.getMaxHealth() >= 250.0F) ? 20 : 1;
        int total = ItemStackDataHelper.getInt(stack, KEY_KILLS) + gain;
        ItemStackDataHelper.putInt(stack, KEY_KILLS, total);

        WeaponGrade current = getGrade(stack);
        WeaponGrade next = current;
        for (WeaponGrade g : WeaponGrade.values()) {
            if (total >= g.killsRequired && g.ordinal() > next.ordinal()) {
                next = g;
            }
        }
        if (next != current) {
            ItemStackDataHelper.putString(stack, KEY_GRADE, next.name());
            return next;
        }
        return null;
    }

    public static Component formatGradeBadge(ItemStack stack) {
        WeaponGrade grade = getGrade(stack);
        return Component.literal(grade.colorPrefix + "[" + grade.title + "]");
    }
}
