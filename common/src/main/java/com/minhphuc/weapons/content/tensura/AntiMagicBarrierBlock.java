package com.minhphuc.weapons.content.tensura;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Khối Kháng Ma Kết Giới (Anti-Magic Barrier Block):
 * - Không xuất hiện trong Creative Tab, chỉ được tạo ra thông qua Kỹ Năng Ma Vương hoặc Dân Làng.
 * - Cho phép người chơi, dân làng, iron golem và sinh vật vô hại tự do ra vào.
 * - Chặn đứng hoàn toàn hoặc làm tan biến các mob/thực thể gây hại và boss.
 * - Người chơi đấm tay không để hóa giải kết giới.
 */
public class AntiMagicBarrierBlock extends Block {

    public AntiMagicBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() != null) {
            Entity entity = ecc.getEntity();
            if (AntiMagicBarrierManager.canPass(entity, pos)) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            AntiMagicBarrierManager.dispelByPunch(sp, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            AntiMagicBarrierManager.dispelByPunch(sp, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (AntiMagicBarrierManager.dispelByPunch(sp, pos)) {
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
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

