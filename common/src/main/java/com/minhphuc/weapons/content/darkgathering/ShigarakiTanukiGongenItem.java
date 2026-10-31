package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * TƯỢNG GỐM TANUKI: CỨU THẾ QUYỀN HIỆN (ĐÃ PHONG ẤN)
 * (信楽焼の狸: 天外救世権現 - SHIGARAKI WARE TANUKI: TENGAI GUZE GONGEN)
 * - Tốt Nghiệp Sinh Đệ Nhất Khóa (1期生) của Hozuki Yayoi trong Dark Gathering.
 * - Chuột phải xuống đất để Giải Phóng (解放) với Ngôn Linh (Kotodama) 「窮めて」(Kiwamete).
 * - Cứu Thế Quyền Hiện xuất hiện trợ chiến tiêu diệt kẻ thù cho chủ nhân trong 90 giây an toàn.
 * - ⚠️ CƠ CHẾ CON DAO HAI LƯỠI: Nếu sau 90 giây không dùng Tượng Lửng Gốm Shigaraki (Rỗng) thu hồi kịp thời,
 *   hắn sẽ phản phệ, mất kiểm soát và quay sang đồ sát chính chủ nhân!
 */
public class ShigarakiTanukiGongenItem extends Item {

    public ShigarakiTanukiGongenItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel sl = (ServerLevel) context.getLevel();
        BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spawnVec = Vec3.atBottomCenterOf(targetPos);

        // Kiểm tra tránh spam triệu hồi nhiều Cứu Thế Quyền Hiện cùng lúc của 1 người chơi
        AABB checkArea = new AABB(spawnVec, spawnVec).inflate(64.0D);
        List<CuuTheQuyenHienEntity> existing = sl.getEntitiesOfClass(CuuTheQuyenHienEntity.class, checkArea,
                e -> e.isAlive() && e.isOwnedBy(player));
        if (!existing.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c⚠️ Bạn đã có một Cứu Thế Quyền Hiện đang được giải phóng gần đây! Hãy dùng Tượng Rỗng thu hồi trước khi triệu hồi thêm."));
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.7F);
            return InteractionResult.FAIL;
        }

        // 1. TRIỆU HỒI CỨU THẾ QUYỀN HIỆN
        CuuTheQuyenHienEntity gongen = new CuuTheQuyenHienEntity(ModEntities.CUU_THE_QUYEN_HIEN.get(), sl);
        gongen.moveTo(spawnVec.x, spawnVec.y, spawnVec.z, player.getYRot() + 180.0F, 0.0F);
        gongen.finalizeSpawn(sl, sl.getCurrentDifficultyAt(targetPos), MobSpawnType.TRIGGERED, null);
        gongen.initSummon(player.getUUID(), 90 * 20); // 90 giây kiểm soát an toàn (1800 ticks)
        sl.addFreshEntity(gongen);

        // 2. ÂM THANH & HOẠT CẢNH GIẢI PHÓNG NGUYÊN TÁC DARK GATHERING
        sl.playSound(null, spawnVec.x, spawnVec.y, spawnVec.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.5F, 1.6F);
        sl.playSound(null, spawnVec.x, spawnVec.y, spawnVec.z, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);
        sl.playSound(null, spawnVec.x, spawnVec.y, spawnVec.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.8F, 1.9F);
        sl.playSound(null, spawnVec.x, spawnVec.y, spawnVec.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 1.2F);

        // Bùng nổ hạt chém kiếm và linh hồn
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spawnVec.x, spawnVec.y + 1.2D, spawnVec.z, 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.SOUL, spawnVec.x, spawnVec.y + 1.0D, spawnVec.z, 30, 0.5D, 0.8D, 0.5D, 0.08D);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, spawnVec.x, spawnVec.y + 1.2D, spawnVec.z, 6, 0.8D, 0.8D, 0.8D, 0.0D);

        // 3. GỬI TITLE CHUẨN NGUYÊN TÁC
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
            sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l【 卒業生、解放 】")));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c§l✦ 言霊:「窮めて」✦")));
            sp.sendSystemMessage(Component.literal("§4§l[TỐT NGHIỆP SINH: GIẢI PHÓNG] §fThiên Ngoại Cứu Thế Quyền Hiện đã xuất thế! Khởi phát ngôn linh: §c「窮めて」(Kiwamete)! §e(90 giây kiểm soát)"));
        }

        // 4. CHUYỂN ĐỔI ITEM TRÊN TAY THÀNH TƯỢNG RỖNG ĐỂ CÓ THỂ THU HỒI
        if (!player.isCreative()) {
            context.getItemInHand().shrink(1);
        }

        ItemStack emptyStatue = new ItemStack(ModItems.SHIGARAKI_TANUKI_EMPTY.get());
        if (!player.getInventory().add(emptyStatue)) {
            player.drop(emptyStatue, false);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof CuuTheQuyenHienEntity gongen && gongen.isAlive()) {
            if (player.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            // Nếu người chơi chuột phải trực tiếp vào Cứu Thế Quyền Hiện của mình -> Thu hồi
            if (gongen.isOwnedBy(player) && !gongen.isRampaging()) {
                gongen.recall(player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§8Tốt Nghiệp Sinh Đệ Nhất Khóa — Dark Gathering Chap 83.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§4§l[TỐT NGHIỆP SINH: ĐÃ PHONG ẤN]"));
        tooltip.add(Component.literal("§c§lThiên Ngoại Cứu Thế Quyền Hiện (天外救世権現)"));
        tooltip.add(Component.literal("§7Ngôn Linh (Kotodama): §e「窮めて」(Kiwamete - Đến Tận Cùng)"));
        tooltip.add(Component.literal("§8Vật chứa: Tượng Lửng Gốm Shigaraki cổ."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e👉 Chuột phải xuống đất: §fGiải phóng Cứu Thế Quyền Hiện!"));
        tooltip.add(Component.literal("§6⏳ Thời gian kiểm soát an toàn: §e90 giây §7(Tàn sát kẻ thù xung quanh bạn)"));
        tooltip.add(Component.literal("§a👉 Cầm Tượng Rỗng chuột phải vào hắn: §fThu Hồi An Toàn về tượng!"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§4⚠️ CẢNH BÁO NGUYÊN TÁC:"));
        tooltip.add(Component.literal("§c- Cứu Thế Quyền Hiện là cỗ máy cuồng chiến không phân biệt!"));
        tooltip.add(Component.literal("§c- Sau 90 giây nếu không thu hồi, hắn sẽ BẠO TẨU và quay sang ĐỒ SÁT BẠN!"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

