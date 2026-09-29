package com.minhphuc.weapons.content.tensura;

import net.minecraft.core.BlockPos;
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
 * Khối Đa Trùng Kết Giới (Multilayer Barrier Block):
 * - Bán trong suốt nhìn xuyên thấu như kính pha lê ma thuật.
 * - Kháng hoàn toàn sát thương, tên bắn, tia năng lượng, Sonic Boom của Warden.
 * - Cho phép người chơi di chuyển tự do qua lại.
 */
public class MultilayerBarrierBlock extends Block {

    public MultilayerBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() != null) {
            Entity entity = ecc.getEntity();
            if (entity instanceof Player) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            MultilayerBarrierAbility.dispelByPunch(sp, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            MultilayerBarrierAbility.dispelByPunch(sp, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            if (MultilayerBarrierAbility.dispelByPunch(sp, pos)) {
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
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
