package com.minhphuc.weapons.content.tensura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * KHỐI HẮC VIÊM BẤT DIỆT (BLACK FIRE BLOCK - BENIMARU)
 * - Mô hình lửa chéo 3D của Minecraft với texture ngọn lửa hoạt họa đen tuyền ma thuật.
 * - Gây sát thương thiêu đốt khi dẫm phải và duy trì hiệu ứng lửa đen.
 */
public class BlackFireBlock extends Block {

    public BlackFireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && living.isAlive()) {
            living.hurt(level.damageSources().inFire(), 3.0F);
            living.setRemainingFireTicks(80);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    0.8F + random.nextFloat() * 0.4F, random.nextFloat() * 0.5F + 0.5F, false);
        }

        double px = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D;
        double py = pos.getY() + 0.3D + random.nextDouble() * 0.6D;
        double pz = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D;
        level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0D, 0.04D, 0.0D);
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.02D, 0.0D);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
