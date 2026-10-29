package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.tensura.CarreraBulletItem.BulletType;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Súng Lục Hoàng Kim (Golden Gun / God-grade Revolver) - Hoàng Sắc Thủy Tổ Carrera (Tensura LN).
 * Thần khí kế thừa từ Trung úy Kondou kết hợp Kỹ Năng Tối Thượng Phá Diệt Chi Vương Abaddon.
 */
public class GoldenGunItem extends Item {

    public static final String NBT_BULLET_TYPE = "GoldenGunBulletType";
    public static final String NBT_AMMO_COUNT = "GoldenGunAmmoCount";
    public static final int MAX_CYLINDER = 6;

    public GoldenGunItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static BulletType getSelectedBullet(ItemStack stack) {
        int id = ItemStackDataHelper.getInt(stack, NBT_BULLET_TYPE);
        return BulletType.fromId(id);
    }

    public static void setSelectedBullet(ItemStack stack, BulletType type) {
        ItemStackDataHelper.putInt(stack, NBT_BULLET_TYPE, type.id);
    }

    public static int getAmmo(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains(NBT_AMMO_COUNT)) {
            return cd.copyTag().getInt(NBT_AMMO_COUNT);
        }
        return MAX_CYLINDER;
    }

    public static void setAmmo(ItemStack stack, int count) {
        ItemStackDataHelper.putInt(stack, NBT_AMMO_COUNT, Math.max(0, Math.min(MAX_CYLINDER, count)));
    }

    /**
     * Kiểm tra người chơi có bất kỳ viên ma đạn nào trên tay hoặc trong túi đồ không
     */
    public static boolean hasAnyBullet(Player player) {
        if (player == null) return false;
        if (player.getOffhandItem().getItem() instanceof CarreraBulletItem) return true;
        if (player.getMainHandItem().getItem() instanceof CarreraBulletItem) return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof CarreraBulletItem) {
                return true;
            }
        }
        return false;
    }

    /**
     * Nạp đạn từ túi đồ vào súng
     */
    public static boolean reloadFromInventory(Player player, ItemStack gunStack, BulletType type) {
        int currentAmmo = getAmmo(gunStack);
        int needed = MAX_CYLINDER - currentAmmo;
        if (needed <= 0) return true;

        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof CarreraBulletItem bulletItem && bulletItem.getBulletType() == type) {
                int take = Math.min(needed - found, s.getCount());
                s.shrink(take);
                found += take;
                if (found >= needed) break;
            }
        }

        if (found > 0) {
            setAmmo(gunStack, currentAmmo + found);
            setSelectedBullet(gunStack, type);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            BulletType currentType = getSelectedBullet(stack);
            int ammo = getAmmo(stack);

            if (ammo > 0) {
                // Tiêu hao 1 viên đạn trong ổ xoay
                setAmmo(stack, ammo - 1);

                // Khai hỏa ma đạn
                if (level instanceof ServerLevel sl && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    CarreraBulletLogic.fireBullet(sl, sp, currentType);

                    // Hiệu ứng khói súng và tia lửa vàng
                    Vec3 eye = player.getEyePosition();
                    Vec3 look = player.getLookAngle();
                    Vec3 muzzle = eye.add(look.scale(0.8D));
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.02);
                    sl.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.08, 0.08, 0.08, 0.04);
                }

                // Hồi chiêu theo loại đạn
                int cooldown = switch (currentType) {
                    case JUDGEMENT -> 40; // 2 giây
                    case ABYSS_CORE -> 30; // 1.5 giây
                    case GRAVITY -> 20; // 1 giây
                    case RAPID -> 6; // Bắn liên thanh
                };
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                // Thử nạp đạn tự động nếu có đạn trong túi đồ
                if (reloadFromInventory(player, stack, currentType)) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.8F, 1.4F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.5F, 1.6F);

                    player.displayClientMessage(
                            Component.literal("§6§l[SÚNG HOÀNG KIM] §aĐã tự động nạp đầy 6 viên " + currentType.vietName + "§a vào ổ xoay! 🔄"),
                            true
                    );
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                } else {
                    // Tiếng click rỗng khi hết đạn
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.5F, 1.8F);
                    player.displayClientMessage(
                            Component.literal("§c§l[SÚNG HOÀNG KIM] §7Ổ đạn trống! Giữ ma đạn trong túi đồ để nạp hoặc bấm §b[PgUp] §7để đổi loại đạn! ⚠️"),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BulletType currentType = getSelectedBullet(stack);
        int ammo = getAmmo(stack);

        tooltip.add(Component.literal("§6§l[THẦN THOẠI CẤP] SÚNG LỤC HOÀNG KIM"));
        tooltip.add(Component.literal("§e§lHoàng Sắc Thủy Tổ Carrera §7(Tensura Light Novel)"));
        tooltip.add(Component.literal("§7Thần khí kế thừa từ Kondou kết hợp Kỹ Năng Tối Thượng Phá Diệt Chi Vương Abaddon."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6🎯 Đang nạp: " + currentType.vietName));
        tooltip.add(Component.literal("§b⚡ Ổ đạn: §f" + ammo + " / " + MAX_CYLINDER + " viên"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7- §a👉 Chuột Phải: §fKhai hỏa ma đạn đang nạp."));
        tooltip.add(Component.literal("§7- §b👉 Phím [PgUp]: §fMở giao diện tròn sáng chọn và nạp đạn (cần có đạn trên tay/túi đồ)."));
    }
}
