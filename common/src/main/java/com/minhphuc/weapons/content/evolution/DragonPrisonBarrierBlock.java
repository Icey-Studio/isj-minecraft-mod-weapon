package com.minhphuc.weapons.content.evolution;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

public class DragonPrisonBarrierBlock extends Block {

    public DragonPrisonBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() != null) {
            net.minecraft.world.entity.Entity entity = ecc.getEntity();
            // Người chơi tự do ra vào lồng giam xuyên qua khối
            if (entity instanceof Player) {
                return Shapes.empty();
            }
            // Mobs bên ngoài có thể tự do bước vào trong lồng
            if (InfiniteDragonPrisonAbility.canEntityPass(entity, pos)) {
                return Shapes.empty();
            }
        }
        // Toàn bộ mob bên trong bị chặn hoàn toàn không thể ra ngoài
        return Shapes.block();
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // Đấm tay không vào khối lồng giam để hóa giải toàn bộ lồng
        if (!level.isClientSide() && player instanceof ServerPlayer sp && sp.getMainHandItem().isEmpty()) {
            InfiniteDragonPrisonAbility.dispelByPunch(sp, pos);
        }
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
