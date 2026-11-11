package com.minhphuc.weapons.content.soul;

import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Vật phẩm: Linh Hồn Giả (Fake Soul)
 * Cho phép đặt Linh Hồn Giả vào một sinh vật để tráo đổi và rút Linh Hồn thật của sinh vật đó ra
 * đưa vào túi đồ người chơi mà KHÔNG gây sát thương hay làm chết sinh vật đó
 * (Ngoại trừ Chước Nhiệt Long Velgrynd và Milim Nava).
 */
public class FakeSoulItem extends Item {

    public static final String TAG_HAS_FAKE_SOUL = "HasFakeSoul";

    public FakeSoulItem(Properties properties) {
        super(properties.stacksTo(64).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Linh hồn nhân tạo được kết tinh từ ma tố thuần khiết.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§b• Công Dụng: §fChuột phải vào sinh vật để tráo Linh Hồn Giả vào cơ thể họ."));
        tooltip.add(Component.literal("§a• Hiệu Quả: §fRút lấy §eLinh Hồn Thật §fvào túi đồ mà §aKHÔNG gây chết §fsinh vật đó!"));
        tooltip.add(Component.literal("§c• Ngoại Lệ: §7Vô hiệu với §6Chước Nhiệt Long Velgrynd §7và §dMilim Nava§7."));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && player.level() instanceof ServerLevel sl) {
            if (performSoulSwap(sl, sp, interactionTarget, stack)) {
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    public static boolean performSoulSwap(ServerLevel level, ServerPlayer player, LivingEntity target, ItemStack heldStack) {
        if (target == null || !target.isAlive() || target == player) return false;

        if (target instanceof VelgryndEntity || target instanceof MilimEntity) {
            player.displayClientMessage(
                    Component.literal("§c§l[LINH HỒN GIẢ] §cKhông thể tráo đổi linh hồn của Chước Nhiệt Long Velgrynd hoặc Milim Nava!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.8F);
            return false;
        }

        if (target.getTags().contains(TAG_HAS_FAKE_SOUL)) {
            player.displayClientMessage(
                    Component.literal("§e§l[LINH HỒN GIẢ] §eSinh vật §f" + target.getName().getString() + " §eđã mang Linh Hồn Giả trong người rồi!"),
                    true
            );
            return false;
        }

        if (heldStack != null && !heldStack.isEmpty() && !player.isCreative()) {
            heldStack.shrink(1);
        }

        target.addTag(TAG_HAS_FAKE_SOUL);

        ItemStack extractedSoul = EntitySoulItem.createSoulFor(target);
        boolean added = player.getInventory().add(extractedSoul);
        if (!added) {
            player.drop(extractedSoul, false);
            player.displayClientMessage(
                    Component.literal("§c§l[CẢNH BÁO TÚI ĐỒ ĐẦY] §eTúi đồ của bạn đã đầy! Linh hồn của §f" + target.getName().getString() + " §eđã rơi dưới chân bạn!"),
                    false
            );
        }

        // Hiệu ứng tráo đổi linh hồn huyền ảo (không gây bất kỳ sát thương nào)
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.04D);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), 15, 0.3D, 0.4D, 0.3D, 0.03D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() * 0.7D, target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.5F, 1.4F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.2F);

        if (added) {
            player.displayClientMessage(
                    Component.literal("§b§l[LINH HỒN GIẢ] §a✔ Đã đặt Linh Hồn Giả và rút thành công §eLinh Hồn của " + target.getName().getString() + " §avào túi đồ mà không gây tổn hại!"),
                    true
            );
        }
        return true;
    }
}
