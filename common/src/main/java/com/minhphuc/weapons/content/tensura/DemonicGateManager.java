package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * QUẢN LÝ CỔNG ÁC MA GIỚI KHỔNG LỒ (THE TOWERING DEMONIC GATE)
 * - Tự Xây Khung Lớn (7x9 -> 11x14) phong cách Gothic Spikes (theo Hình 2).
 * - Sinh Cổng Tự Nhiên: Cổng Bị Vỡ (Ruined Gate) & Cổng Nguyên Vẹn (Citadel Gate).
 * - Quỷ Binh Thâm Uyên (Underworld Demons) định kỳ bước qua cổng xâm lăng Overworld.
 */
public class DemonicGateManager {

    private static final Set<Long> GENERATED_CHUNKS = new HashSet<>();
    private static final List<BlockPos> TRACKED_ACTIVE_GATES = new ArrayList<>();

    public static boolean onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) return false;
        ItemStack held = player.getItemInHand(hand);

        // Kích hoạt bằng Bật Lửa, Tia Lửa Ma Giới hoặc Ma Khí
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)
                || held.getItem() instanceof PrimordialClawItem) {
            ServerLevel sl = (ServerLevel) player.level();
            BlockPos targetPos = pos.relative(face);
            if (tryIgniteDemonicGate(sl, targetPos, (ServerPlayer) player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra cấu trúc khung cổng lớn (7x9 trở lên) và kích hoạt nếu hợp lệ
     */
    public static boolean tryIgniteDemonicGate(ServerLevel level, BlockPos startPos, ServerPlayer player) {
        // Kiểm tra hai trục X và Z
        if (tryIgniteAxis(level, startPos, Direction.Axis.X, player)) return true;
        return tryIgniteAxis(level, startPos, Direction.Axis.Z, player);
    }

    private static boolean tryIgniteAxis(ServerLevel level, BlockPos startPos, Direction.Axis axis, ServerPlayer player) {
        Direction dir = (axis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;

        // Tìm đáy khung
        BlockPos bottomCenter = startPos;
        while (bottomCenter.getY() > level.getMinBuildHeight() && level.getBlockState(bottomCenter.below()).isAir()) {
            bottomCenter = bottomCenter.below();
        }

        // Quét chiều ngang lòng cổng
        int minOffset = 0;
        int maxOffset = 0;

        while (minOffset > -7 && !isGateFrameBlock(level.getBlockState(bottomCenter.relative(dir, minOffset - 1)))) {
            minOffset--;
        }
        while (maxOffset < 7 && !isGateFrameBlock(level.getBlockState(bottomCenter.relative(dir, maxOffset + 1)))) {
            maxOffset++;
        }

        int interiorWidth = maxOffset - minOffset + 1;
        if (interiorWidth < 5) return false; // Lòng trong tối thiểu 5 khối (tổng chiều ngang cổng >= 7 khối)

        // Quét chiều cao lòng cổng
        int minHeight = 16;
        for (int i = minOffset; i <= maxOffset; i++) {
            BlockPos colPos = bottomCenter.relative(dir, i);
            int h = 0;
            while (h < 15 && !isGateFrameBlock(level.getBlockState(colPos.above(h + 1)))) {
                h++;
            }
            minHeight = Math.min(minHeight, h);
        }

        if (minHeight < 7) return false; // Lòng trong tối thiểu 7 khối cao (tổng chiều cao cổng >= 9 khối)

        // Kiểm tra sàn đáy & trần đỉnh
        for (int i = minOffset; i <= maxOffset; i++) {
            BlockPos floorPos = bottomCenter.relative(dir, i);
            if (!isGateFrameBlock(level.getBlockState(floorPos))) return false;
            if (!isGateFrameBlock(level.getBlockState(floorPos.above(minHeight + 1)))) return false;
        }

        // Lấp đầy lòng cổng bằng Portal khối lớn
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
        for (int i = minOffset; i <= maxOffset; i++) {
            for (int y = 1; y <= minHeight; y++) {
                BlockPos p = bottomCenter.relative(dir, i).above(y);
                level.setBlock(p, portalState, 3);
            }
        }

        // Âm thanh và hiệu ứng bão ma lực
        Vec3 center = new Vec3(bottomCenter.getX() + (minOffset + maxOffset) * 0.5D * dir.getStepX(),
                bottomCenter.getY() + minHeight * 0.5D,
                bottomCenter.getZ() + (minOffset + maxOffset) * 0.5D * dir.getStepZ());

        level.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 2.5F, 0.7F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.8F, 1.4F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 80, interiorWidth * 0.4D, minHeight * 0.4D, interiorWidth * 0.4D, 0.1D);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 60, interiorWidth * 0.4D, minHeight * 0.4D, interiorWidth * 0.4D, 0.08D);

        TRACKED_ACTIVE_GATES.add(BlockPos.containing(center));

        if (player != null) {
            player.displayClientMessage(Component.literal("§5§l✦ CỔNG ÁC MA GIỚI KHỔNG LỒ ĐÃ KHAI MỞ! ✦\n§7Cửa ngõ thâm uyên nối liền Thế Giới Thực và Ác Ma Giới!"), false);
        }
        return true;
    }

    private static boolean isGateFrameBlock(BlockState state) {
        return state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                || state.is(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
                || state.is(Blocks.GILDED_BLACKSTONE);
    }

    /**
     * Tick định kỳ: Quản lý Quỷ Binh bước qua cổng & Sinh cấu trúc tự nhiên
     */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 100 != 0) return;

        // Cơ chế Quỷ Binh Thâm Uyên bước qua cổng vào Overworld (Demon Incursion)
        if (level.dimension() == Level.OVERWORLD && !TRACKED_ACTIVE_GATES.isEmpty()) {
            if (level.random.nextFloat() < 0.25F) {
                BlockPos gate = TRACKED_ACTIVE_GATES.get(level.random.nextInt(TRACKED_ACTIVE_GATES.size()));
                if (level.isLoaded(gate) && level.getBlockState(gate).is(Blocks.NETHER_PORTAL)) {
                    int spawnCount = 1 + level.random.nextInt(3);
                    for (int i = 0; i < spawnCount; i++) {
                        UnderworldDemonEntity demon = ModEntities.UNDERWORLD_DEMON.get().create(level);
                        if (demon != null) {
                            demon.moveTo(gate.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 2.0D,
                                    gate.getY(),
                                    gate.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 2.0D,
                                    level.random.nextFloat() * 360.0F, 0.0F);
                            level.addFreshEntity(demon);
                            level.sendParticles(ParticleTypes.SOUL, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.05D);
                        }
                    }
                    level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.8F);
                }
            }
        }

        // Sinh Cổng Tự Nhiên khi người chơi thám hiểm chunk mới
        for (ServerPlayer player : level.players()) {
            ChunkPos cp = player.chunkPosition();
            long key = ChunkPos.asLong(cp.x, cp.z);
            if (GENERATED_CHUNKS.add(key)) {
                // Tỉ lệ 1/55 chunk sinh Cổng Ác Ma Giới ở vùng hoang dã
                if (level.random.nextInt(55) == 0) {
                    BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, cp.getBlockAt(8, 0, 8));
                    if (surface.getY() > 50 && surface.getY() < 140) {
                        boolean isRuined = level.random.nextFloat() < 0.70F;
                        if (isRuined) {
                            generateRuinedGate(level, surface);
                        } else {
                            generateActiveCitadelGate(level, surface);
                        }
                    }
                }
            }
        }
    }

    /**
     * Tạo Cổng Bị Vỡ (Ruined Demonic Gate) sinh tự nhiên với Rương Di Vật
     */
    public static void generateRuinedGate(ServerLevel level, BlockPos origin) {
        int w = 7;
        int h = 9;

        // Bệ đá nứt nẻ
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos floor = origin.offset(x, 0, z);
                BlockState flState = (level.random.nextBoolean())
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                level.setBlock(floor, flState, 3);
            }
        }

        // Hai cột trụ tháp gothic với gai nhọn (Spikes) vươn cao
        for (int y = 1; y <= h; y++) {
            BlockState leftState = (y % 3 == 0 || level.random.nextFloat() < 0.25F)
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            BlockState rightState = (y % 4 == 0)
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

            level.setBlock(origin.offset(-3, y, 0), leftState, 3);
            level.setBlock(origin.offset(3, y, 0), rightState, 3);
        }

        // Gai nhọn trên đỉnh (Gothic Spikes - Hình 2)
        level.setBlock(origin.offset(-3, h + 1, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);
        level.setBlock(origin.offset(-3, h + 2, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, h + 1, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, h + 2, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);

        // Vòm cung cánh dơi ở đỉnh
        for (int x = -2; x <= 2; x++) {
            if (x != 0 || level.random.nextFloat() > 0.3F) {
                level.setBlock(origin.offset(x, h, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
            }
        }

        // Rương Kho Báu Di Tích Ma Giới
        BlockPos chestPos = origin.offset(2, 1, 1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Blocks.CRYING_OBSIDIAN, 2 + level.random.nextInt(3)));
            chest.setItem(1, new ItemStack(Items.FLINT_AND_STEEL));
            chest.setItem(2, new ItemStack(Items.GOLD_INGOT, 4 + level.random.nextInt(5)));
            chest.setItem(3, new ItemStack(Items.DIAMOND, 1 + level.random.nextInt(2)));
            chest.setItem(4, new ItemStack(ModItems.DEMON_LORD_SOUL.get(), 1));
            chest.setItem(5, new ItemStack(Items.ENDER_PEARL, 2 + level.random.nextInt(3)));
        }
    }

    /**
     * Tạo Cổng Nguyên Vẹn (Ancient Demonic Citadel Gate) đã kích hoạt sẵn cổng tím
     */
    public static void generateActiveCitadelGate(ServerLevel level, BlockPos origin) {
        int w = 7;
        int h = 9;

        // Bệ thềm hắc diện thạch
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos floor = origin.offset(x, 0, z);
                level.setBlock(floor, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
            }
        }

        // Hai cột trụ tháp gothic nguyên vẹn
        for (int y = 1; y <= h; y++) {
            level.setBlock(origin.offset(-3, y, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(origin.offset(3, y, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        }

        // Gai nhọn trên đỉnh (Gothic Spikes)
        level.setBlock(origin.offset(-3, h + 1, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);
        level.setBlock(origin.offset(-3, h + 2, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, h + 1, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, h + 2, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 3);

        // Vòm đỉnh & Đáy
        for (int x = -2; x <= 2; x++) {
            level.setBlock(origin.offset(x, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(origin.offset(x, h, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        }

        // Lấp đầy lòng cổng bằng Nether Portal
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X);
        for (int x = -2; x <= 2; x++) {
            for (int y = 1; y < h; y++) {
                level.setBlock(origin.offset(x, y, 0), portalState, 3);
            }
        }

        TRACKED_ACTIVE_GATES.add(origin.above(h / 2));

        // Hai Quỷ Binh Thâm Uyên canh gác cổng
        for (int offset : new int[]{-4, 4}) {
            UnderworldDemonEntity guard = ModEntities.UNDERWORLD_DEMON.get().create(level);
            if (guard != null) {
                guard.moveTo(origin.getX() + offset + 0.5D, origin.getY() + 1.0D, origin.getZ() + 1.5D, 0.0F, 0.0F);
                guard.setPersistenceRequired();
                level.addFreshEntity(guard);
            }
        }
    }
}
