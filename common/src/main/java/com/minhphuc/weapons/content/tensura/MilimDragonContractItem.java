package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
 * Vật phẩm: Khế Ước Long Ma Vương (Milim's Dragon Contract)
 * Dùng để triệu hồi Ma Vương Cổ Đại Milim Nava với nghi thức Sấm Sét liên hoàn & Vòng Tròn Ma Thuật.
 */
public class MilimDragonContractItem extends Item {

    public MilimDragonContractItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Hào quang ma pháp rực rỡ
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        Vec3 summonPos = player.position().add(look.x * 5.0D, 0, look.z * 5.0D);

        // Tìm độ cao mặt đất phù hợp
        BlockPos bPos = BlockPos.containing(summonPos);
        while (serverLevel.getBlockState(bPos).isAir() && bPos.getY() > serverLevel.getMinBuildHeight() + 2) {
            bPos = bPos.below();
        }
        Vec3 targetCenter = new Vec3(summonPos.x, bPos.getY() + 1.0D, summonPos.z);

        // Kiểm tra xem đã có Milim trong phạm vi 64 block hay chưa (chống lag và spam boss)
        AABB checkArea = new AABB(targetCenter, targetCenter).inflate(64.0D);
        List<MilimEntity> existing = serverLevel.getEntitiesOfClass(MilimEntity.class, checkArea, e -> e.isAlive());
        if (!existing.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("§d§l[MILIM NAVA] §e\"Wahahaha! Ta đã ở ngay đây rồi mà, gọi thêm làm gì nữa chứ!\""),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // Tiêu hao vật phẩm nếu không ở Creative
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 160); // 8 giây hồi chiêu

        // 1. KÍCH HOẠT VÒNG TRÒN MA THUẬT RỰC RỠ 5 TẦNG XẾP DỌC
        PentagramCelestialPillarAbility.spawnPillarAt(serverLevel, (ServerPlayer) player, targetCenter, DemonType.ROUGE, false, true);

        // 2. NGHI THỨC SẤM SÉT THIÊN LÔI ĐÁNH LIÊN HOÀN
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI * 2.0 / 4.0) * i;
            double lx = targetCenter.x + Math.cos(angle) * 3.5D;
            double lz = targetCenter.z + Math.sin(angle) * 3.5D;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(lx, targetCenter.y, lz);
                bolt.setVisualOnly(true); // Chỉ tạo hiệu ứng sấm sét chấn động không gây cháy nổ phá địa hình
                serverLevel.addFreshEntity(bolt);
            }
        }

        // 3. ÂM THANH RỀN VANG TRỜI ĐẤT
        serverLevel.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 0.8F);
        serverLevel.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.5F, 1.2F);
        serverLevel.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.5F);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetCenter.x, targetCenter.y + 1.0D, targetCenter.z, 5, 0.8, 0.8, 0.8, 0);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, targetCenter.x, targetCenter.y + 1.0D, targetCenter.z, 80, 2.0, 3.0, 2.0, 0.15);

        // 4. SPAWN MILIM NAVA
        MilimEntity milim = new MilimEntity(ModEntities.MILIM.get(), serverLevel);
        milim.moveTo(targetCenter.x, targetCenter.y + 0.5D, targetCenter.z, player.getYRot() + 180.0F, 0.0F);
        serverLevel.addFreshEntity(milim);

        // 5. THÔNG BÁO VÀ THOẠI CỦA MILIM
        TensuraDialogueManager.sayMilim(milim, "dialogue.weapons.milim.summon_greeting");

        String broadcast = "§6§l✦ LONG MA GIÁNG THẾ ✦ §eMa Vương Cổ Đại §c§lMilim Nava §eđã đáp lại Khế Ước và giáng lâm thế giới!";
        for (ServerPlayer p : serverLevel.players()) {
            if (p.distanceToSqr(targetCenter) <= 96.0D * 96.0D) {
                p.displayClientMessage(Component.literal(broadcast), false);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.literal("§d§l✦ TÍN VẬT KHỞI NGUYÊN LONG MA VƯƠNG ✦"));
        tooltip.add(Component.literal("§7Khế ước cổ đại chứa đựng tinh tủy ma pháp của §cMilim Nava§7."));
        tooltip.add(Component.literal("§eNhấn [Chuột Phải] §fvào không gian để triệu hồi:"));
        tooltip.add(Component.literal("§c • Trận đồ Ngũ Trọng Ma Thuật rực sáng"));
        tooltip.add(Component.literal("§b • Sấm sét thiên lôi oanh tạc"));
        tooltip.add(Component.literal("§d • Ma Vương Milim Nava giáng lâm"));
        tooltip.add(Component.literal("§7(Lưu ý: Cô ấy ban đầu thân thiện, chỉ đánh lại khi bị tấn công)"));
    }
}
