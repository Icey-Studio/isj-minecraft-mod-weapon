package com.minhphuc.weapons.content.tensura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Trình thanh tẩy và hồi sinh ngôi làng của Kháng Ma Kết Giới (Anti-Magic Barrier):
 * 1. Xóa bỏ hiệu ứng xấu bên trong:
 *    - Tuyết (snow layers), Tuyết lún (powder snow) biến mất.
 *    - Băng (ice, frosted_ice) tan chảy thành nước trong lành.
 *    - Khử đóng băng (ticksFrozen = 0) cho toàn bộ người chơi và sinh vật thân thiện.
 * 2. Phục hồi làng hoang phế (Zombie / Abandoned Village):
 *    - Mạng nhện (cobweb) bị tiêu trừ thành không khí.
 *    - Đá rêu phong (mossy cobblestone, mossy stone bricks, moss carpet) biến thành đá sạch bóng.
 *    - Dân làng thây ma (Zombie Villager) được thanh tẩy thành Dân làng bình thường.
 *    - Tái lập ngôi làng trù phú: Triệu hồi Dân làng theo biome, Người Sắt (Iron Golem) bảo vệ, Mèo (Cat) thú cưng.
 * 3. Ban phước hoa màu (Crop Prosperity):
 *    - Đất cày (farmland) được cấp ẩm tối đa (moisture = 7).
 *    - Cây trồng (lúa mì, cà rốt, khoai tây, củ cải, dưa hấu, bí ngô, dâu ngọt) lớn nhanh nở rộ.
 */
public class AntiMagicVillageRestorer {

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> prop : from.getProperties()) {
            if (result.hasProperty(prop)) {
                result = copyProperty(from, result, prop);
            }
        }
        return result;
    }

    /**
     * Thực hiện thanh tẩy toàn bộ vùng đất bên trong Kháng Ma Kết Giới.
     */
    public static void purifyBarrierArea(ServerLevel level, AntiMagicBarrierManager.ActiveBarrier barrier) {
        Vec3 center = barrier.center;
        double radius = barrier.radius;

        int minChunkX = (int) Math.floor((center.x - radius) / 16.0);
        int maxChunkX = (int) Math.floor((center.x + radius) / 16.0);
        int minChunkZ = (int) Math.floor((center.z - radius) / 16.0);
        int maxChunkZ = (int) Math.floor((center.z + radius) / 16.0);

        int cobwebsRemoved = 0;
        int mossCleaned = 0;
        int snowCleared = 0;
        int iceMelted = 0;
        int cropsBoosted = 0;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;

                double chunkCenterX = (cx << 4) + 8.5;
                double chunkCenterZ = (cz << 4) + 8.5;
                double cdx = chunkCenterX - center.x;
                double cdz = chunkCenterZ - center.z;
                if ((cdx * cdx + cdz * cdz) > (radius + 16.0) * (radius + 16.0)) {
                    continue;
                }

                for (int x = 0; x < 16; x++) {
                    int worldX = (cx << 4) + x;
                    for (int z = 0; z < 16; z++) {
                        int worldZ = (cz << 4) + z;

                        double distSq = (worldX - center.x) * (worldX - center.x) + (worldZ - center.z) * (worldZ - center.z);
                        if (distSq > radius * radius) continue;

                        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                        int startY = Math.max(level.getMinBuildHeight(), surfaceY - 6);
                        int endY = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 9);

                        for (int y = startY; y <= endY; y++) {
                            mpos.set(worldX, y, worldZ);
                            BlockState state = level.getBlockState(mpos);
                            Block block = state.getBlock();

                            // 1. Mạng nhện
                            if (state.is(Blocks.COBWEB)) {
                                level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 3);
                                cobwebsRemoved++;
                            }
                            // 2. Đá cuội rêu phong
                            else if (state.is(Blocks.MOSSY_COBBLESTONE)) {
                                level.setBlock(mpos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSSY_COBBLESTONE_STAIRS)) {
                                level.setBlock(mpos, copyProperties(state, Blocks.COBBLESTONE_STAIRS.defaultBlockState()), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSSY_COBBLESTONE_SLAB)) {
                                level.setBlock(mpos, copyProperties(state, Blocks.COBBLESTONE_SLAB.defaultBlockState()), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSSY_COBBLESTONE_WALL)) {
                                level.setBlock(mpos, copyProperties(state, Blocks.COBBLESTONE_WALL.defaultBlockState()), 3);
                                mossCleaned++;
                            }
                            // 3. Gạch đá rêu phong
                            else if (state.is(Blocks.MOSSY_STONE_BRICKS)) {
                                level.setBlock(mpos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSSY_STONE_BRICK_STAIRS)) {
                                level.setBlock(mpos, copyProperties(state, Blocks.STONE_BRICK_STAIRS.defaultBlockState()), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSSY_STONE_BRICK_SLAB)) {
                                level.setBlock(mpos, copyProperties(state, Blocks.STONE_BRICK_SLAB.defaultBlockState()), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSSY_STONE_BRICK_WALL)) {
                                level.setBlock(mpos, copyProperties(state, Blocks.STONE_BRICK_WALL.defaultBlockState()), 3);
                                mossCleaned++;
                            }
                            // 4. Khối rêu & Thảm rêu
                            else if (state.is(Blocks.MOSS_BLOCK)) {
                                level.setBlock(mpos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                                mossCleaned++;
                            } else if (state.is(Blocks.MOSS_CARPET)) {
                                level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 3);
                                mossCleaned++;
                            }
                            // 5. Tuyết & Tuyết lún (Powder snow)
                            else if (state.is(Blocks.SNOW)) {
                                level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 3);
                                snowCleared++;
                            } else if (state.is(Blocks.POWDER_SNOW)) {
                                level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 3);
                                snowCleared++;
                            }
                            // 6. Băng tan chảy thành nước
                            else if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                                level.setBlock(mpos, Blocks.WATER.defaultBlockState(), 3);
                                iceMelted++;
                            }
                            // 7. Đất cày nông nghiệp (Farmland)
                            else if (state.is(Blocks.FARMLAND)) {
                                if (state.getValue(FarmBlock.MOISTURE) < 7) {
                                    level.setBlock(mpos, state.setValue(FarmBlock.MOISTURE, 7), 3);
                                }
                            }
                            // 8. Cây trồng (Crops) thúc đẩy phát triển rực rỡ
                            else if (block instanceof CropBlock crop) {
                                if (!crop.isMaxAge(state)) {
                                    level.setBlock(mpos, crop.getStateForAge(crop.getMaxAge()), 3);
                                    cropsBoosted++;
                                }
                            } else if (block instanceof StemBlock) {
                                level.setBlock(mpos, state.setValue(StemBlock.AGE, 7), 3);
                                cropsBoosted++;
                            } else if (block instanceof SweetBerryBushBlock) {
                                level.setBlock(mpos, state.setValue(SweetBerryBushBlock.AGE, 3), 3);
                                cropsBoosted++;
                            } else if (block instanceof CocoaBlock) {
                                level.setBlock(mpos, state.setValue(CocoaBlock.AGE, 2), 3);
                                cropsBoosted++;
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // THANH TẨY THỰC THỂ & HỒI SINH NGÔI LÀNG (ENTITY RESTORATION)
        // =========================================================================
        AABB box = new AABB(center.x - radius, center.y - barrier.lowestYOffset - 5, center.z - radius,
                center.x + radius, center.y + barrier.height + 5, center.z + radius);

        // 1. Giải trừ đông cứng (Reset Freeze)
        List<LivingEntity> friendlies = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && (e instanceof Player || e instanceof Villager || e instanceof IronGolem || e instanceof Animal));
        for (LivingEntity le : friendlies) {
            if (le.getTicksFrozen() > 0) {
                le.setTicksFrozen(0);
            }
        }

        // 2. Chữa lành Dân Làng Thây Ma (Zombie Villagers)
        List<ZombieVillager> zombieVillagers = level.getEntitiesOfClass(ZombieVillager.class, box,
                zv -> zv.isAlive() && barrier.center.distanceTo(zv.position()) <= radius);

        int convertedZombies = 0;
        for (ZombieVillager zv : zombieVillagers) {
            Villager villager = zv.convertTo(EntityType.VILLAGER, false);
            if (villager != null) {
                villager.finalizeSpawn(level, level.getCurrentDifficultyAt(villager.blockPosition()), MobSpawnType.CONVERSION, null);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.0, villager.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
                convertedZombies++;
            }
        }

        // 3. Phục hồi Dân cư, Người sắt và Thú cưng khi phát hiện làng bỏ hoang
        List<Villager> aliveVillagers = level.getEntitiesOfClass(Villager.class, box,
                v -> v.isAlive() && barrier.center.distanceTo(v.position()) <= radius);

        boolean isAbandonedVillage = (cobwebsRemoved >= 2 || mossCleaned >= 4 || convertedZombies > 0);

        if (isAbandonedVillage || aliveVillagers.size() < 6) {
            int neededVillagers = Math.max(5, 8 - aliveVillagers.size());
            Holder<Biome> biomeHolder = level.getBiome(BlockPos.containing(center));
            VillagerType vType = VillagerType.byBiome(biomeHolder);

            VillagerProfession[] professions = {
                    VillagerProfession.FARMER,
                    VillagerProfession.LIBRARIAN,
                    VillagerProfession.ARMORER,
                    VillagerProfession.WEAPONSMITH,
                    VillagerProfession.TOOLSMITH,
                    VillagerProfession.CLERIC,
                    VillagerProfession.FLETCHER,
                    VillagerProfession.BUTCHER
            };

            for (int i = 0; i < neededVillagers; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                double dist = 4.0 + level.random.nextDouble() * (Math.min(radius * 0.45, 28.0));
                int sx = (int) (center.x + Math.cos(angle) * dist);
                int sz = (int) (center.z + Math.sin(angle) * dist);
                int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);

                BlockPos spawnPos = new BlockPos(sx, sy, sz);
                if (sy > level.getMinBuildHeight() && level.getBlockState(spawnPos).isAir()) {
                    Villager v = EntityType.VILLAGER.create(level);
                    if (v != null) {
                        v.moveTo(sx + 0.5, sy, sz + 0.5, level.random.nextFloat() * 360F, 0.0F);
                        VillagerProfession prof = professions[i % professions.length];
                        v.setVillagerData(v.getVillagerData().setType(vType).setProfession(prof).setLevel(2));
                        level.addFreshEntity(v);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, sx + 0.5, sy + 1.0, sz + 0.5, 15, 0.4, 0.6, 0.4, 0.05);
                    }
                }
            }

            // Triệu hồi Người Sắt (Iron Golem) bảo hộ
            List<IronGolem> aliveGolems = level.getEntitiesOfClass(IronGolem.class, box,
                    g -> g.isAlive() && barrier.center.distanceTo(g.position()) <= radius);
            if (aliveGolems.isEmpty()) {
                int golemsToSpawn = 2;
                for (int i = 0; i < golemsToSpawn; i++) {
                    double angle = (i * Math.PI) + (level.random.nextDouble() * 0.4);
                    double dist = 8.0 + level.random.nextDouble() * 12.0;
                    int gx = (int) (center.x + Math.cos(angle) * dist);
                    int gz = (int) (center.z + Math.sin(angle) * dist);
                    int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
                    if (gy > level.getMinBuildHeight()) {
                        IronGolem golem = EntityType.IRON_GOLEM.create(level);
                        if (golem != null) {
                            golem.moveTo(gx + 0.5, gy, gz + 0.5, level.random.nextFloat() * 360F, 0.0F);
                            golem.setPlayerCreated(true);
                            level.addFreshEntity(golem);
                            level.sendParticles(ParticleTypes.EXPLOSION, gx + 0.5, gy + 1.0, gz + 0.5, 1, 0, 0, 0, 0);
                            level.playSound(null, gx, gy, gz, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 2.0F, 1.0F);
                        }
                    }
                }
            }

            // Triệu hồi Mèo (Cat) thú cưng sinh sống
            List<Cat> aliveCats = level.getEntitiesOfClass(Cat.class, box,
                    c -> c.isAlive() && barrier.center.distanceTo(c.position()) <= radius);
            if (aliveCats.size() < 3) {
                int catsToSpawn = 3 - aliveCats.size();
                for (int i = 0; i < catsToSpawn; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0;
                    double dist = 6.0 + level.random.nextDouble() * 16.0;
                    int cx = (int) (center.x + Math.cos(angle) * dist);
                    int cz = (int) (center.z + Math.sin(angle) * dist);
                    int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    if (cy > level.getMinBuildHeight()) {
                        Cat cat = EntityType.CAT.create(level);
                        if (cat != null) {
                            cat.moveTo(cx + 0.5, cy, cz + 0.5, level.random.nextFloat() * 360F, 0.0F);
                            cat.finalizeSpawn(level, level.getCurrentDifficultyAt(cat.blockPosition()), MobSpawnType.EVENT, null);
                            level.addFreshEntity(cat);
                            level.sendParticles(ParticleTypes.HEART, cx + 0.5, cy + 0.5, cz + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                }
            }

            level.playSound(null, center.x, center.y, center.z, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 3.5F, 1.0F);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 2.0F, 1.2F);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 2.0F, 1.1F);

            for (ServerPlayer sp : level.players()) {
                if (sp.position().distanceTo(center) <= radius + 48.0) {
                    sp.displayClientMessage(
                            Component.literal("§a§l[KHÁNG MA KẾT GIỚI] §eThánh quang kết giới đã thanh tẩy toàn bộ làng hoang tàn! Rêu phong mạng nhện tiêu tan, dân làng, người sắt và thú cưng đã trở lại!"),
                            false
                    );
                }
            }
        } else if (snowCleared > 0 || iceMelted > 0 || cropsBoosted > 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.5F, 1.2F);
            for (ServerPlayer sp : level.players()) {
                if (sp.position().distanceTo(center) <= radius + 32.0) {
                    sp.displayClientMessage(
                            Component.literal("§a§l[KHÁNG MA KẾT GIỚI] §eĐã xóa bỏ tuyết lún, làm tan băng giá và ban phước cho toàn bộ hoa màu phát triển mạnh mẽ!"),
                            true
                    );
                }
            }
        }
    }

    /**
     * Duy trì phước lành mỗi 40 ticks trong suốt thời gian kết giới tồn tại.
     */
    public static void tickBarrierBlessings(ServerLevel level, AntiMagicBarrierManager.ActiveBarrier barrier) {
        Vec3 center = barrier.center;
        double radius = barrier.radius;

        // 1. Giải phóng hoàn toàn trạng thái đóng băng cho người chơi và động vật
        AABB box = new AABB(center.x - radius, center.y - barrier.lowestYOffset - 5, center.z - radius,
                center.x + radius, center.y + barrier.height + 5, center.z + radius);
        List<LivingEntity> friendlies = level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!e.isAlive()) return false;
            double dSq = (e.getX() - center.x) * (e.getX() - center.x) + (e.getZ() - center.z) * (e.getZ() - center.z);
            return dSq <= radius * radius && (e instanceof Player || e instanceof Villager || e instanceof IronGolem || e instanceof Animal);
        });

        for (LivingEntity le : friendlies) {
            if (le.getTicksFrozen() > 0) {
                le.setTicksFrozen(0);
            }
        }

        // 2. Tăng tốc sinh trưởng hoa màu, giữ ẩm đất cày và tiêu biến ngay lập tức tuyết/băng giá
        for (int i = 0; i < 36; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double dist = level.random.nextDouble() * radius;
            int bx = (int) (center.x + Math.cos(angle) * dist);
            int bz = (int) (center.z + Math.sin(angle) * dist);

            // Tìm mặt đất thực sự bên dưới kết giới (bỏ qua khối kết giới ANTI_MAGIC_BARRIER trên cao)
            int startY = Math.min(level.getMaxBuildHeight() - 2, (int) (center.y + barrier.height));
            int minY = Math.max(level.getMinBuildHeight(), (int) (center.y - barrier.lowestYOffset - 5));
            BlockPos.MutableBlockPos curPos = new BlockPos.MutableBlockPos(bx, startY, bz);
            while (curPos.getY() > minY) {
                BlockState bs = level.getBlockState(curPos);
                if (bs.is(com.minhphuc.weapons.init.ModBlocks.ANTI_MAGIC_BARRIER.get())) {
                    curPos.move(net.minecraft.core.Direction.DOWN);
                    continue;
                }
                if (!bs.isAir()) {
                    break;
                }
                curPos.move(net.minecraft.core.Direction.DOWN);
            }

            BlockPos groundPos = curPos.immutable();
            // Kiểm tra cả groundPos và groundPos.above() và groundPos.below()
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos checkPos = groundPos.offset(0, dy, 0);
                BlockState state = level.getBlockState(checkPos);
                Block block = state.getBlock();

                if (state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW)) {
                    level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.POOF, checkPos.getX() + 0.5, checkPos.getY() + 0.2, checkPos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.02);
                } else if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                    level.setBlock(checkPos, Blocks.WATER.defaultBlockState(), 3);
                } else if (state.is(Blocks.FARMLAND)) {
                    if (state.getValue(FarmBlock.MOISTURE) < 7) {
                        level.setBlock(checkPos, state.setValue(FarmBlock.MOISTURE, 7), 2);
                    }
                } else if (block instanceof CropBlock crop) {
                    if (!crop.isMaxAge(state)) {
                        int newAge = Math.min(crop.getMaxAge(), crop.getAge(state) + 1);
                        level.setBlock(checkPos, crop.getStateForAge(newAge), 2);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, checkPos.getX() + 0.5, checkPos.getY() + 0.3, checkPos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
        }
    }
}
