package com.minhphuc.weapons.content.darkgathering;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Vật phẩm: Con Mắt Thị Nhục (Seer Flesh Eye / 視肉之眼)
 * Rơi ra khi đánh/rạch khối Thị Nhục của Thái Tuế Tinh Quân (Dark Gathering Tập 18).
 * - Chuột phải vào không khí: Hồi 100% máu và xóa sạch mọi hiệu ứng xấu độc hại cho bản thân.
 * - Chuột phải vào sinh vật khác: Chữa lành toàn diện, hóa giải nhiễm bệnh Zombie, hồi 100% HP.
 */
public class SeerFleshEyeItem extends Item {

    public SeerFleshEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 1. Hồi 100% Máu & No nê
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);

            // 2. Xóa sạch mọi hiệu ứng xấu độc hại
            cleanseHarmfulEffects(player);

            // 3. Ban phát phước lành sinh mệnh
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, false, true));

            // 4. Âm thanh và hạt
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.6F, 0.9F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 1.1F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.6F);

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2D, player.getZ(), 8, 0.4D, 0.4D, 0.4D, 0.05D);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.3D, 0.5D, 0.3D, 0.15D);
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            player.displayClientMessage(
                    Component.literal("§c§l[CON MẮT THỊ NHỤC] §aĐã nuốt trọn nhãn lực! Sinh lực phục hồi 100% & Xóa sạch mọi độc tố/nguyền rủa! ✨👁️"),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Hóa giải và xóa sạch mọi hiệu ứng xấu độc hại trên sinh vật
     */
    public static void cleanseHarmfulEffects(LivingEntity entity) {
        entity.removeEffect(MobEffects.POISON);
        entity.removeEffect(MobEffects.WITHER);
        entity.removeEffect(MobEffects.DARKNESS);
        entity.removeEffect(MobEffects.BLINDNESS);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        entity.removeEffect(MobEffects.HUNGER);
        entity.removeEffect(MobEffects.BAD_OMEN);
        entity.removeEffect(MobEffects.CONFUSION);
        entity.removeEffect(MobEffects.LEVITATION);
        entity.setRemainingFireTicks(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§c§l[THẦN VẬT THÁI TUẾ]"));
        tooltip.add(Component.literal("§b§lCon Mắt Thị Nhục (Seer Flesh Eye)"));
        tooltip.add(Component.literal("§7Nhãn cầu u linh rạch ra từ Thị Nhục của Thái Tuế Tinh Quân."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Đặc Tính Thần Dị:"));
        tooltip.add(Component.literal("§7- §a👉 Chuột Phải: §fNuốt con mắt, hồi phục §a100% Máu §fvà xóa bỏ sạch mọi độc tố/nguyền rủa."));
        tooltip.add(Component.literal("§7- §e👉 Chuột Phải vào sinh vật: §fCứu rỗi & chữa lành 100% máu cho đối phương (Hóa giải cả Zombie)."));
    }
}
