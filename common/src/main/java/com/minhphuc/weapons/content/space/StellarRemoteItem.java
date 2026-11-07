package com.minhphuc.weapons.content.space;

import com.minhphuc.weapons.client.renderer.ClientOrbitalStrikeRenderer;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * THIẾT BỊ ĐIỀU KHIỂN QUỸ ĐẠO: THE STELLAR REMOTE (THE SHOOTING STAR DEMO)
 * "It probably does nothing... right?"
 * Cho phép người chơi gọi các đòn không kích từ vệ tinh không gian:
 * 1. SS-01 · Railgun: Cột Plasma siêu trọng trường
 * 2. SS-03 · Gungnir: Thần thương từ quỹ đạo Sao Mộc
 * 3. SS-04 · The Seven Stars: Tinh trận Bắc Đẩu Thất Tinh
 * ĐẶC BIỆT: 100% Zero Block Destruction - An toàn tuyệt đối cho kiến trúc!
 */
public class StellarRemoteItem extends Item {

    public StellarRemoteItem(Properties properties) {
        super(properties);
    }

    public static int getMode(ItemStack stack) {
        return Math.floorMod(ItemStackDataHelper.getInt(stack, "StellarMode"), 3);
    }

    public static void setMode(ItemStack stack, int mode) {
        ItemStackDataHelper.putInt(stack, "StellarMode", Math.floorMod(mode, 3));
    }

    public static Component getModeName(int mode) {
        return switch (mode) {
            case ClientOrbitalStrikeRenderer.MODE_RAILGUN -> Component.literal("§b§lSS-01 · Railgun");
            case ClientOrbitalStrikeRenderer.MODE_GUNGNIR -> Component.literal("§c§lSS-03 · Gungnir");
            case ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS -> Component.literal("§5§lSS-04 · The Seven Stars");
            default -> Component.literal("§7Không xác định");
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1. Chuyển chế độ bằng Shift + Chuột phải
        if (player.isShiftKeyDown()) {
            int nextMode = (getMode(stack) + 1) % 3;
            setMode(stack, nextMode);

            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.4F + nextMode * 0.2F);

            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.literal("§6[STELLAR REMOTE] §fĐã chuyển sang chế độ: ").append(getModeName(nextMode)),
                        true
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 2. Chuột phải thường: Khóa mục tiêu và gọi đòn tấn công quỹ đạo
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) level;

            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 reach = eye.add(look.scale(128.0));

            BlockHitResult hit = level.clip(new ClipContext(eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 targetPos = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation() : eye.add(look.scale(32.0));

            int mode = getMode(stack);
            OrbitalStrikeManager.triggerStrike(serverLevel, serverPlayer, targetPos, mode);

            // Thời gian hồi chiêu
            int cooldown = switch (mode) {
                case ClientOrbitalStrikeRenderer.MODE_RAILGUN -> 160; // 8s
                case ClientOrbitalStrikeRenderer.MODE_GUNGNIR -> 200; // 10s
                case ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS -> 260; // 13s
                default -> 160;
            };
            player.getCooldowns().addCooldown(this, cooldown);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7\"It probably does nothing... right?\"").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6Quỹ đạo vệ tinh: ").append(getModeName(getMode(stack))));
        tooltip.add(Component.literal("§e• Chuột phải: §fKhóa tọa độ & Giáng đòn quỹ đạo"));
        tooltip.add(Component.literal("§b• Shift + Chuột phải: §fChuyển đổi tần số (SS-01 / SS-03 / SS-04)"));
        tooltip.add(Component.literal("§a✔ 100% Zero Block Destruction §7(Không phá hủy địa hình)"));
    }
}
