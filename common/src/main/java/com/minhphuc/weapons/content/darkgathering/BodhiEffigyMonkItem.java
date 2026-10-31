package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * HÌNH NHÂN GỖ BỒ ĐỀ (ĐÃ PHONG ẤN TÀ KINH ĐẠI TĂNG) - BODHI EFFIGY (MONK)
 * Dùng để giải phóng Tốt Nghiệp Sinh Cấp S - Tà Kinh Đại Tăng (Jankyōbun Daisōjō).
 * - Kiểm soát trong 60 giây (1200 ticks).
 * - CẢNH BÁO SỐNG CÒN: Chủ nhân BẮT BUỘC phải đeo Bông Bịt Tai Phong Ấn!
 */
public class BodhiEffigyMonkItem extends Item {

    public BodhiEffigyMonkItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockPos spawnPos = pos.relative(face);
        spawnMonk((ServerLevel) level, player, Vec3.atBottomCenterOf(spawnPos), context.getItemInHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(lookVec.x * 2.5D, 0.2D, lookVec.z * 2.5D);
        spawnMonk((ServerLevel) level, player, spawnPos, stack);
        return InteractionResultHolder.consume(stack);
    }

    private void spawnMonk(ServerLevel sl, Player player, Vec3 pos, ItemStack stack) {
        TaKinhDaiTangEntity monk = new TaKinhDaiTangEntity(ModEntities.TA_KINH_DAI_TANG.get(), sl);
        monk.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);

        // Khởi tạo trạng thái triệu hồi: 60 giây kiểm soát (1200 ticks)
        monk.initSummon(player.getUUID(), 1200);
        sl.addFreshEntity(monk);

        // Hiệu ứng âm thanh & hình ảnh giải phóng tà linh
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3.5F, 0.6F);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.5F, 0.8F);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.4F);

        for (int i = 0; i < 40; i++) {
            double ox = (sl.random.nextDouble() - 0.5D) * 2.2D;
            double oy = sl.random.nextDouble() * 2.5D;
            double oz = (sl.random.nextDouble() - 0.5D) * 2.2D;
            sl.sendParticles(ParticleTypes.SOUL, pos.x + ox, pos.y + oy, pos.z + oz, 2, 0, 0.1D, 0, 0.05D);
            sl.sendParticles(ParticleTypes.ENCHANT, pos.x + ox, pos.y + oy, pos.z + oz, 3, 0.2D, 0.2D, 0.2D, 0.1D);
        }

        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
            sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l【 邪経文大僧正・解放 】")));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c✦ Tà Kinh Đại Tăng giáng lâm! Hãy bịt tai ngay! ✦")));

            sp.sendSystemMessage(Component.literal("§4§l[GIẢI PHÓNG TÀ LINH] §cĐã giải phóng Tà Kinh Đại Tăng hỗ trợ chiến đấu trong §e60 giây§c!"));

            // Kiểm tra xem chủ nhân có đeo bông bịt tai không
            if (!WardingEarplugsItem.isProtected(sp)) {
                sp.sendSystemMessage(Component.literal("§4§l⚠️ NGUY HIỂM CHẾT NGƯỜI: §cBạn chưa đeo Bông Bịt Tai Phong Ấn! Tiếng tụng kinh của ngài sẽ độ hóa tử vong cả bạn!"));
                sp.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5F, 0.6F);
            } else {
                sp.sendSystemMessage(Component.literal("§a§l✔ [AN TOÀN] §fBông Bịt Tai Phong Ấn đang bảo vệ bạn khỏi tiếng tụng kinh của Đại Tăng!"));
            }
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§l[TỐT NGHIỆP SINH CẤP S] §cTà Kinh Đại Tăng"));
        tooltip.add(Component.literal("§7(邪経文大僧正 - Jankyōbun Daisōjō)"));
        tooltip.add(Component.literal("§7Phong ấn trong hình nhân gỗ Bồ Đề quấn kinh phù đỏ thẫm."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e[Chuột Phải]§7 để giải phóng Đại Tăng chiến đấu trong §660 giây§7."));
        tooltip.add(Component.literal("§fDùng Hình Nhân Gỗ Rỗng chuột phải vào ngài để thu hồi an toàn."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§4⚠️ CẢNH BÁO SỐNG CÒN:"));
        tooltip.add(Component.literal("§cChủ nhân BẮT BUỘC phải đeo §6Bông Bịt Tai Phong Ấn§c trước khi thả!"));
        tooltip.add(Component.literal("§cNếu không, tiếng tụng kinh của ngài sẽ độ hóa tử vong cả chính bạn!"));
    }
}

