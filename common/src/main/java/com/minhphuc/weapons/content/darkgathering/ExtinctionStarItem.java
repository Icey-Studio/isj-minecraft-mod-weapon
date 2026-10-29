package com.minhphuc.weapons.content.darkgathering;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ngôi Sao Tuyệt Diệt (Extinction Star in Hand / 滅亡星) - Thái Tuế Tinh Quân
 * - Lơ lửng trên bàn tay chói lòa ánh sáng như ảnh manga.
 * - Nhấp Chuột Trái hoặc Chuột Phải để phóng viên Tinh Tú hủy diệt (1-Hit Kill quái vật & phá hủy 1 block).
 */
public class ExtinctionStarItem extends Item {

    private static final DustParticleOptions STARLIGHT_WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.8F);

    public ExtinctionStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                // Kích hoạt Tuyệt Kỹ: Diệt Thế Tà Tinh - Alkaid
                AlkaidAbility.cast((ServerLevel) level, serverPlayer);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Phóng Tinh Tú Tuyệt Diệt
            TaisuiExtinctionStarsAbility.fireStar((ServerLevel) level, serverPlayer);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof net.minecraft.server.level.ServerPlayer serverPlayer && attacker.level() instanceof ServerLevel sl) {
            TaisuiExtinctionStarsAbility.fireStar(sl, serverPlayer);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof LivingEntity living && (isSelected || living.getOffhandItem() == stack)) {
            // Hiệu ứng hào quang chói lòa xung quanh bàn tay cầm ngôi sao
            if (level.isClientSide()) {
                double px = living.getX() + (level.random.nextDouble() - 0.5D) * 0.4D;
                double py = living.getY() + living.getEyeHeight() * 0.75D + (level.random.nextDouble() - 0.5D) * 0.3D;
                double pz = living.getZ() + (level.random.nextDouble() - 0.5D) * 0.4D;

                level.addParticle(STARLIGHT_WHITE_DUST, px, py, pz, 0, 0, 0);
                if (level.random.nextFloat() < 0.25F) {
                    level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0.02D, 0);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§e§l[TINH THẦN QUÂN BẢO KHÍ]"));
        tooltip.add(Component.literal("§f§lNgôi Sao Tuyệt Diệt (Extinction Star)"));
        tooltip.add(Component.literal("§7Tinh tú thiên thể u linh ngưng tụ trên lòng bàn tay, chói lòa hào quang."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6⚡ Thao Tác Hủy Diệt:"));
        tooltip.add(Component.literal("§7- §c👉 Chuột Trái / Chuột Phải: §fPhóng ngôi sao ra xa, tất sát 1-hit quái vật và phá hủy block trúng phải."));
        tooltip.add(Component.literal("§7- §4§l👉 Shift + Chuột Phải: §eThi triển Tuyệt Kỹ: Diệt Thế Tà Tinh (ALKAID) - Tụ 8s tạo cầu phân rã khổng lồ khoan xuyên địa hình!"));
        tooltip.add(Component.literal("§7- §e👉 Tỏa sáng chói lọi: §fChiếu rọi không gian xung quanh người cầm."));
    }
}
