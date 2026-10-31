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
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel) level;
                net.minecraft.world.phys.AABB checkArea = player.getBoundingBox().inflate(64.0D);
                if (!sl.getEntitiesOfClass(com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity.class, checkArea).isEmpty()) {
                    player.displayClientMessage(
                            Component.literal("§c⚠️ Đã có một Tà Kinh Đại Tăng đang ở gần! Hãy đánh bại hoặc phong ấn ngài trước!"),
                            true
                    );
                    return net.minecraft.world.InteractionResultHolder.fail(stack);
                }

                net.minecraft.world.phys.Vec3 spawnPos = player.position().add(player.getLookAngle().scale(6.0D));
                com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity boss =
                        new com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity(com.minhphuc.weapons.entity.ModEntities.TA_KINH_DAI_TANG.get(), sl);
                boss.moveTo(spawnPos.x, spawnPos.y + 0.5D, spawnPos.z, player.getYRot() + 180.0F, 0.0F);
                sl.addFreshEntity(boss);

                sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(), net.minecraft.sounds.SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.45F);
                sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);

                player.displayClientMessage(
                        Component.literal("§4§l【 KHIÊU CHIẾN 】 §cTà Kinh Đại Tăng đã giáng lâm! Hãy mau đeo Bông Bịt Tai và làm ngài suy yếu để phong ấn!"),
                        false
                );
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
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
        tooltip.add(Component.literal("§6⚡ [Shift + Chuột Phải]§f: Khiêu chiến gọi ra Tà Kinh Đại Tăng hoang dã."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e📜 Hướng Dẫn Phong Ấn:"));
        tooltip.add(Component.literal("§71. Làm Tà Kinh Đại Tăng suy yếu xuống §c<= 20% HP§7 (<= 240 HP)"));
        tooltip.add(Component.literal("§7   hoặc làm choáng ngài bằng đòn tấn công chí mạng."));
        tooltip.add(Component.literal("§72. Cầm Hình Nhân Gỗ Rỗng nhấp §a[Chuột Phải]§7 vào ngài để phong ấn."));
    }
}

