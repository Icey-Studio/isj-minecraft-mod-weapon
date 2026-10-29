package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Thị Nhục Bọc Tay (Seer Flesh Gauntlet / 視肉の腕) - Thái Tuế Tinh Quân (Dark Gathering)
 * - Bao bọc cánh tay bằng lớp thịt u linh màu xanh lá rêu.
 * - Mang 3 con mắt sinh học u ám (1 mắt lớn trung tâm, 2 mắt bên hông).
 * - Chuột phải: Hồi phục 100% máu, no nê, giải sạch độc tố/nguyền rủa.
 * - Mỗi lần hồi máu, 1 con mắt sẽ nhắm lại (3 -> 2 -> 1 -> 0 mắt).
 * - Khi hết mắt, chuyển thành trạng thái Thị Nhục Không Mắt.
 */
public class SeerFleshArmItem extends Item {

    public static final String NBT_EYES = "EyesRemaining";

    public SeerFleshArmItem(Properties properties) {
        super(properties);
    }

    public static int getEyes(ItemStack stack) {
        return ItemStackDataHelper.getInt(stack, NBT_EYES);
    }

    public static void setEyes(ItemStack stack, int count) {
        ItemStackDataHelper.putInt(stack, NBT_EYES, Math.max(0, Math.min(3, count)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            int eyes = getEyes(stack);
            if (eyes > 0) {
                int nextEyes = eyes - 1;
                setEyes(stack, nextEyes);

                // 1. Hồi phục 100% máu & thanh thức ăn
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0F);

                // 2. Xóa sạch toàn bộ hiệu ứng độc hại
                SeerFleshEyeItem.cleanseHarmfulEffects(player);

                // 3. Ban phát phước lành sinh mệnh
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 2, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, false, true));

                // 4. Âm thanh & hạt hào quang
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 2.0F, 0.8F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 1.1F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.5F);

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2D, player.getZ(), 10, 0.4D, 0.4D, 0.4D, 0.05D);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.3D, 0.5D, 0.3D, 0.15D);
                }

                if (nextEyes > 0) {
                    player.displayClientMessage(
                            Component.literal("§2§l[THỊ NHỤC BỌC TAY] §a1 Con mắt đã nhắm lại! Hồi 100% Máu & Xóa bỏ toàn bộ độc tố! §e(Còn " + nextEyes + " mắt mở) 👁️✨"),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.literal("§c§l[THỊ NHỤC BỌC TAY] §7Tất cả 3 con mắt đã nhắm nghiền! Chuyển thành trạng thái Thị Nhục Không Mắt. 👁️✕"),
                            true
                    );
                }

                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                player.displayClientMessage(
                        Component.literal("§c⚠️ Tất cả 3 con mắt trên bàn tay đã nhắm lại, không thể tiếp tục trị liệu!"),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 1.2F, 0.6F);
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide()) {
            int eyes = getEyes(stack);
            if (eyes > 0) {
                int nextEyes = eyes - 1;
                setEyes(stack, nextEyes);

                target.setHealth(target.getMaxHealth());
                SeerFleshEyeItem.cleanseHarmfulEffects(target);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2, false, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, false, true));

                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 1.1F);
                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.6F);

                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0D, target.getZ(), 10, 0.4D, 0.4D, 0.4D, 0.05D);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1.0D, target.getZ(), 25, 0.3D, 0.5D, 0.3D, 0.15D);
                }

                player.displayClientMessage(
                        Component.literal("§2§l[THỊ NHỤC BỌC TAY] §aĐã dùng nhãn lực cứu sống " + target.getName().getString() + "! (Còn " + nextEyes + " mắt mở) ✨👁️"),
                        true
                );

                return InteractionResult.sidedSuccess(player.level().isClientSide());
            } else {
                player.displayClientMessage(
                        Component.literal("§c⚠️ không còn đủ thị nhục, không thể tiếp tục trị liệu!"),
                        true
                );
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && level.getGameTime() % 40 == 0 && entity instanceof LivingEntity living) {
            if (isSelected || living.getOffhandItem() == stack) {
                // Định kỳ nhỏ giọt chất nhầy u linh xanh lá
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ITEM_SLIME, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0.2D, 0.2D, 0.2D, 0.01D);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int eyes = getEyes(stack);
        tooltip.add(Component.literal("§2§l[THÁI TUẾ BẢO VẬT]"));
        tooltip.add(Component.literal("§a§lThị Nhục"));
        tooltip.add(Component.literal("§7tích trữ được 3 nhãn cầu."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6⚡ Trạng Thái Nhãn Lực: " + (eyes > 0 ? "§a" + eyes + " / 3 Con Mắt Mở" : "§7Đã Nhắm Nghiền (Không Mắt)")));
        tooltip.add(Component.literal("§7- §a👉 Chuột Phải: §fNuốt nhãn lực, hồi phục §a100% Máu §fvà xóa bỏ sạch mọi độc tố/nguyền rủa."));
        tooltip.add(Component.literal("§7- §e👉 Mỗi lần hồi máu: §f1 Con mắt sẽ nhắm lại. Hết 3 mắt chuyển sang trạng thái không mắt."));
        tooltip.add(Component.literal("§7- §b👉 Chuột Phải vào sinh vật: §fChữa lành 100% máu cho đối phương."));
    }
}
