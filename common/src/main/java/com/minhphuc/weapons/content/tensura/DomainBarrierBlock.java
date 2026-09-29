package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Khối Kết Giới Lãnh Địa Của Ác Ma Thủy Tổ:
 * - Không thể phá bằng công cụ thông thường (Bedrock-like).
 * - Cú đấm trực tiếp của Người Chơi (đặc biệt khi là Ma Vương hoặc đấm tay không/vũ khí)
 *   sẽ đập nát hoàn toàn Lãnh Địa, làm Ác ma bị phản phệ và choáng váng!
 */
public class DomainBarrierBlock extends Block {

    public DomainBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            shatterDomain(sp, (ServerLevel) level, pos);
        }
    }

    public static boolean isAnyDomainBarrier(Block block) {
        return block == ModBlocks.DOMAIN_BARRIER_NOIR.get() ||
               block == ModBlocks.DOMAIN_BARRIER_ROUGE.get() ||
               block == ModBlocks.DOMAIN_BARRIER_BLANC.get() ||
               block == ModBlocks.DOMAIN_BARRIER_JAUNE.get() ||
               block == ModBlocks.DOMAIN_BARRIER_VIOLET.get() ||
               block == ModBlocks.DOMAIN_BARRIER_BLEU.get() ||
               block == ModBlocks.DOMAIN_BARRIER_VERT.get();
    }

    public static void shatterDomain(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Tìm kiếm các Ác Ma Thủy Tổ đang duy trì Lãnh Địa trong bán kính 36 block
        AABB searchBox = new AABB(pos).inflate(36.0);
        List<PrimordialDemonEntity> nearbyDemons = level.getEntitiesOfClass(PrimordialDemonEntity.class, searchBox);

        boolean shattered = false;
        for (PrimordialDemonEntity demon : nearbyDemons) {
            demon.cleanDomainBarriers();
            demon.activeDomainTicks = 0;
            // Ác ma bị phản phệ: choáng váng (Stun) 3 giây
            demon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
            demon.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, demon.getX(), demon.getY() + 1.0, demon.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
            shattered = true;
        }

        // Hoàn nguyên dọn dẹp các khối lãnh địa (chỉ chạy dự phòng khi không tìm thấy ác ma)
        if (!shattered) {
            int r = 16;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    for (int dy = -6; dy <= 12; dy++) {
                        BlockPos p = pos.offset(dx, dy, dz);
                        if (isAnyDomainBarrier(level.getBlockState(p).getBlock())) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // Âm thanh vỡ tan uy lực
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 3.5F, 0.7F);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 3.0F, 1.2F);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 2.0F, 1.4F);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0, 0, 0, 0);

        player.displayClientMessage(
                Component.literal("§c§l⚡ [PHÁ TAN LÃNH ĐỊA] §eCú đấm của bạn đã đập vỡ nát hoàn toàn Lãnh Địa của Ác Ma!"),
                true
        );
        player.sendSystemMessage(
                Component.literal("§6✦ Lãnh Địa Ma Thuật đã sụp đổ hoàn toàn trước uy áp của bạn! Ác ma bị phản phệ rơi vào trạng thái choáng váng.")
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
