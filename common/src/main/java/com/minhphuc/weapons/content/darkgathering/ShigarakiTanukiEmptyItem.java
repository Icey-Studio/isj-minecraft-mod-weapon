package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
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
 * TƯỢNG LỬNG GỐM SHIGARAKI (信楽焼の狸の置物 - SHIGARAKI WARE TANUKI FIGURINE - EMPTY):
 * - Vật chứa phong ấn Tốt Nghiệp Sinh Đệ Nhất Khóa (1期生) trong Dark Gathering Chap 83.
 * - Chuột phải vào Cứu Thế Quyền Hiện khi HP <= 20% (<= 300 HP) hoặc khi bị Choáng (Stun) để phong ấn linh hồn vào tượng.
 * - Sau khi phong ấn, chuyển hóa thành Tượng Gốm Tanuki: Cứu Thế Quyền Hiện (Đã Phong Ấn).
 */
public class ShigarakiTanukiEmptyItem extends Item {

    public ShigarakiTanukiEmptyItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof CuuTheQuyenHienEntity gongen && gongen.isAlive()) {
            if (player.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            ServerLevel sl = (ServerLevel) player.level();

            // 1. Nếu Cứu Thế Quyền Hiện này là thú triệu hồi của người chơi và chưa bạo tẩu -> Thu hồi an toàn
            if (gongen.isOwnedBy(player) && !gongen.isRampaging()) {
                gongen.recall(player);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }

            // 2. Nghi thức Phong Ấn Cứu Thế Quyền Hiện ngoài tự nhiên:
            // Điều kiện: HP <= 20% (<= 300 HP) hoặc đang bị Choáng (Stun) bởi Tiên Tri
            boolean eligible = gongen.getHealth() <= (gongen.getMaxHealth() * 0.20F) || gongen.isStunned();

            if (!eligible) {
                player.sendSystemMessage(Component.literal("§c⚠️ Cứu Thế Quyền Hiện còn quá mạnh (§e" + (int) gongen.getHealth() + "§c/§e" + (int) gongen.getMaxHealth() + " HP§c)! Cần làm suy yếu xuống dưới 20% HP (<= 300 HP) hoặc làm choáng bằng Tiên Tri để phong ấn!"));
                sl.playSound(null, gongen.getX(), gongen.getY(), gongen.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5F, 0.8F);
                return InteractionResult.FAIL;
            }

            // Thực hiện phong ấn thành công
            sl.playSound(null, gongen.getX(), gongen.getY(), gongen.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.2F);
            sl.playSound(null, gongen.getX(), gongen.getY(), gongen.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3.0F, 0.7F);
            sl.playSound(null, gongen.getX(), gongen.getY(), gongen.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.8F);
            sl.playSound(null, gongen.getX(), gongen.getY(), gongen.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.8F);

            // Xoáy lốc linh hồn đen trắng hút vào tượng
            for (int i = 0; i < 35; i++) {
                double ox = (sl.random.nextDouble() - 0.5D) * 2.0D;
                double oy = sl.random.nextDouble() * 2.5D;
                double oz = (sl.random.nextDouble() - 0.5D) * 2.0D;
                sl.sendParticles(ParticleTypes.SOUL, gongen.getX() + ox, gongen.getY() + oy, gongen.getZ() + oz, 2, 0, 0.1D, 0, 0.05D);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, gongen.getX(), gongen.getY() + 1.2D, gongen.getZ(), 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, gongen.getX() + ox, gongen.getY() + oy, gongen.getZ() + oz, 1, 0, 0.05D, 0, 0.02D);
            }

            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
                sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l【 封印完了 】")));
                sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a✦ Đã Phong Ấn Cứu Thế Quyền Hiện Vào Tượng Gốm Shigaraki! ✦")));
                sp.sendSystemMessage(Component.literal("§a§l[PHONG ẤN THÀNH CÔNG] §fLinh hồn Thiên Ngoại Cứu Thế Quyền Hiện đã bị thu phục và phong ấn vào Tượng Lửng Gốm Shigaraki!"));
            }

            // Tiêu biến boss và đổi item trên tay
            gongen.discard();

            if (!player.isCreative()) {
                stack.shrink(1);
            }

            ItemStack sealedItem = new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get());
            if (!player.getInventory().add(sealedItem)) {
                player.drop(sealedItem, false);
            }

            return InteractionResult.SUCCESS;
        }

        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§8Vật chứa phong ấn Tốt Nghiệp Sinh — Dark Gathering Chap 83.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§6§lTượng Lửng Gốm Shigaraki (信楽焼の狸の置物)"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e👉 Chuột phải vào Cứu Thế Quyền Hiện:"));
        tooltip.add(Component.literal("§7- §aYêu cầu làm suy yếu máu còn §e<= 20% §7(<= 300 HP)"));
        tooltip.add(Component.literal("§7- §aHoặc khi đang bị Choáng (§eStun§7) bởi kỹ năng Tiên Tri"));
        tooltip.add(Component.literal("§7- §fThu phục linh hồn và phong ấn hắn vào bụng bức tượng!"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§b👉 Khi đang triệu hồi Cứu Thế Quyền Hiện:"));
        tooltip.add(Component.literal("§7- Chuột phải vào hắn để §eThu Hồi An Toàn§7 trở lại tượng trước khi phản phệ!"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

