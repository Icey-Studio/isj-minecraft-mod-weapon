package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Vật phẩm Triệu Hồi Không Vong (Kūbō - Hắc Nhật Phôi Thai).
 * Triệu hồi Không Vong giáng lâm từ bầu trời tăm tối.
 * LƯU Ý QUAN TRỌNG: Tuyệt đối KHÔNG sử dụng vòng tròn ma thuật (theo thiết lập Dark Gathering).
 */
public class KuboSummonItem extends Item {

    public KuboSummonItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§8Hắc Nhật Hư Vô — Phôi Thai Tà Thần Vực Thẳm.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§4[Chuột Phải]: §fTriệu hồi §0§lKhông Vong (Kūbō)§f giáng thế từ không gian tối!"));
        tooltip.add(Component.literal("§7• Bay lơ lửng trên trời cao và xà xuống săn mồi"));
        tooltip.add(Component.literal("§7• Sát thương cực lớn, đánh xuyên Giáp Thần Thoại"));
        tooltip.add(Component.literal("§7• Bắn cột sáng hư vô giống Milim khi dưới 50% HP"));
        tooltip.add(Component.literal("§7• Ưu tiên nuốt linh hồn các thực thể tử trận để hấp thu 70% sức mạnh"));
        tooltip.add(Component.literal("§c§l• LƯU Ý: Tuyệt đối không có vòng tròn ma thuật"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        Vec3 playerPos = player.position();

        // Vị trí triệu hồi trên cao khoảng 14 blocks phía trước người chơi
        Vec3 summonPos = playerPos.add(look.x * 6.0D, 14.0D, look.z * 6.0D);

        // Giới hạn số lượng Không Vong lân cận để tránh lag
        AABB checkArea = new AABB(summonPos, summonPos).inflate(64.0D);
        List<KuboEntity> existing = serverLevel.getEntitiesOfClass(KuboEntity.class, checkArea, e -> e.isAlive());
        if (existing.size() >= 3) {
            player.displayClientMessage(
                    Component.literal("§4§l[HẮC NHẬT] §cKhông gian xung quanh đã bị áp bức bởi nhiều Không Vong! Hãy tiêu diệt bớt trước khi triệu hồi thêm."),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 100); // 5 giây hồi chiêu

        // 1. HIỆU ỨNG TẬP TRUNG HƯ VÔ (TUYỆT ĐỐI KHÔNG DÙNG VÒNG TRÒN MA THUẬT)
        // Bùng nổ hạt mực đen, khói rồng hư vô và sóng xung kích âm thanh
        for (int i = 0; i < 40; i++) {
            double ox = (serverLevel.random.nextDouble() - 0.5D) * 6.0D;
            double oy = (serverLevel.random.nextDouble() - 0.5D) * 6.0D;
            double oz = (serverLevel.random.nextDouble() - 0.5D) * 6.0D;
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, summonPos.x + ox, summonPos.y + oy, summonPos.z + oz, 3, 0.1D, 0.1D, 0.1D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, summonPos.x + ox, summonPos.y + oy, summonPos.z + oz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, summonPos.x, summonPos.y, summonPos.z, 1, 0, 0, 0, 0);
        }

        // Âm thanh rung chuyển không gian
        serverLevel.playSound(null, summonPos.x, summonPos.y, summonPos.z, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.5F, 0.6F);
        serverLevel.playSound(null, summonPos.x, summonPos.y, summonPos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.5F);

        // 2. TẠO THỰC THỂ KHÔNG VONG
        KuboEntity kubo = ModEntities.KUBO.get().create(serverLevel);
        if (kubo != null) {
            kubo.moveTo(summonPos.x, summonPos.y, summonPos.z, player.getYRot(), 0.0F);
            kubo.setDeltaMovement(0, -0.1D, 0);
            serverLevel.addFreshEntity(kubo);

            player.displayClientMessage(
                    Component.literal("§0§l[HẮC NHẬT PHÔI THAI] §4§lKūbō - Không Vong đã giáng lâm từ hư vô vực thẳm!"),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
