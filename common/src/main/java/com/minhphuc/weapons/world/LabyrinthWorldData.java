package com.minhphuc.weapons.world;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.OrcDisasterGeldEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mê Cung 100 Tầng Của Ramiris (Ramiris's 100-Floor Labyrinth):
 * - Công trình cổ đại khổng lồ duy nhất trong Overworld (KHÔNG thuộc sở hữu người chơi).
 * - Tối ưu Zero-Lag: CHỈ stream & dựng đúng căn phòng của Tầng đang đặt chân tới (Lazy Floor Streaming).
 * - Tầng 1-20: Ma Thiết Cổ | 21-40: Rừng Độc | 41-60: Băng Ngục | 61-80: Hỏa Ngục | 81-99: Ác Ma | 100: Boss Hộ Vệ Thần Tầng 100.
 */
public class LabyrinthWorldData extends SavedData {

    private static final String DATA_NAME = "weapons_ramiris_labyrinth_data";
    public static final int MAX_FLOORS = 100;

    private int entranceX;
    private int entranceY;
    private int entranceZ;
    private boolean coordsRolled = false;
    private boolean entranceBuilt = false;
    private final Set<Integer> generatedFloors = new HashSet<>();

    public static LabyrinthWorldData get(ServerLevel level) {
        ServerLevel ow = level.getServer().overworld();
        return ow.getDataStorage().computeIfAbsent(
                new Factory<>(LabyrinthWorldData::new, LabyrinthWorldData::load, null),
                DATA_NAME
        );
    }

    public static LabyrinthWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        LabyrinthWorldData data = new LabyrinthWorldData();
        data.entranceX = tag.getInt("EntX");
        data.entranceY = tag.getInt("EntY");
        data.entranceZ = tag.getInt("EntZ");
        data.coordsRolled = tag.getBoolean("CoordsRolled");
        data.entranceBuilt = tag.getBoolean("EntBuilt");
        if (tag.contains("GenFloors")) {
            for (int f : tag.getIntArray("GenFloors")) {
                data.generatedFloors.add(f);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("EntX", entranceX);
        tag.putInt("EntY", entranceY);
        tag.putInt("EntZ", entranceZ);
        tag.putBoolean("CoordsRolled", coordsRolled);
        tag.putBoolean("EntBuilt", entranceBuilt);
        tag.put("GenFloors", new IntArrayTag(generatedFloors.stream().mapToInt(Integer::intValue).toArray()));
        return tag;
    }

    public BlockPos getOrRollEntrancePos(ServerLevel ow) {
        if (!coordsRolled) {
            long seed = Math.abs(ow.getSeed() ^ 0x52414D49524953L);
            double angle = ((seed % 360) * Math.PI) / 180.0D;
            double dist = 420.0D + (seed % 360);
            this.entranceX = (int) Math.round(Math.cos(angle) * dist);
            this.entranceZ = (int) Math.round(Math.sin(angle) * dist);
            this.entranceY = 72;
            this.coordsRolled = true;
            setDirty();
        }
        return new BlockPos(entranceX, entranceY, entranceZ);
    }

    public boolean isEntranceBuilt() {
        return entranceBuilt;
    }

    public void ensureEntranceGenerated(ServerLevel ow) {
        if (entranceBuilt) return;
        BlockPos approx = getOrRollEntrancePos(ow);
        int surfaceY = Math.max(63, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, approx.getX(), approx.getZ()));
        this.entranceY = surfaceY;
        BlockPos base = new BlockPos(entranceX, surfaceY, entranceZ);

        // Xây Cổng Mê Cung Cổ Đại Ramiris đồ sộ trên mặt đất (kiến trúc như Thành Cổ)
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean border = Math.abs(dx) == 7 || Math.abs(dz) == 7;
                boolean innerRing = Math.abs(dx) == 4 && Math.abs(dz) == 4;
                Block floor = border ? Blocks.DEEPSLATE_BRICKS : ((dx + dz) % 2 == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.CHISELED_DEEPSLATE);
                ow.setBlock(base.offset(dx, -1, dz), floor.defaultBlockState(), 2);
                for (int dy = 0; dy <= 5; dy++) {
                    ow.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
                if (innerRing) {
                    for (int dy = 0; dy <= 6; dy++) {
                        ow.setBlock(base.offset(dx, dy, dz), (dy == 6 ? Blocks.AMETHYST_BLOCK : Blocks.DEEPSLATE_TILES).defaultBlockState(), 2);
                    }
                }
            }
        }
        // Đài trung tâm & Khối Cổng Mê Cung Tầng 1
        ow.setBlock(base, ModBlocks.LABYRINTH_GATEWAY.get().defaultBlockState(), 3);
        ow.setBlock(base.offset(0, -1, 0), Blocks.BEACON.defaultBlockState(), 2);
        ow.setBlock(base.offset(2, 0, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        ow.setBlock(base.offset(-2, 0, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 2);

        this.entranceBuilt = true;
        setDirty();
    }

    /**
     * Tọa độ tầng f (1..100): trải lưới 10x10 sâu dưới lòng đất (Y = -48, cách nhau 44 block -> tuyệt đối không chồng lấn & không lag).
     */
    public BlockPos getFloorCenter(ServerLevel ow, int floor) {
        BlockPos ent = getOrRollEntrancePos(ow);
        int idx = Math.max(0, Math.min(MAX_FLOORS - 1, floor - 1));
        int col = idx % 10;
        int row = idx / 10;
        return new BlockPos(ent.getX() + col * 44, -48, ent.getZ() + row * 44);
    }

    public int detectFloorAt(ServerLevel ow, BlockPos pos) {
        BlockPos ent = getOrRollEntrancePos(ow);
        if (pos.getY() > -25 || pos.getY() < -60) return 0;
        for (int f = 1; f <= MAX_FLOORS; f++) {
            BlockPos fc = getFloorCenter(ow, f);
            if (Math.abs(pos.getX() - fc.getX()) <= 16 && Math.abs(pos.getZ() - fc.getZ()) <= 16) {
                return f;
            }
        }
        return 0;
    }

    public void enterFloor(ServerPlayer sp, int targetFloor) {
        ServerLevel ow = sp.getServer().overworld();
        if (sp.serverLevel() != ow) {
            sp.displayClientMessage(Component.literal("§e[Mê Cung Ramiris] §7Cổng Mê Cung chỉ hoạt động tại Thế Giới Chính (Overworld)!"), true);
            return;
        }
        if (targetFloor <= 0 || targetFloor > MAX_FLOORS) {
            BlockPos ent = getOrRollEntrancePos(ow);
            sp.teleportTo(ow, ent.getX() + 0.5, ent.getY() + 1.2, ent.getZ() + 2.5, sp.getYRot(), sp.getXRot());
            sp.displayClientMessage(Component.literal("§d[Mê Cung 100 Tầng Ramiris] §aĐã trở về Lối Vào Mặt Đất!"), false);
            return;
        }

        BlockPos center = getFloorCenter(ow, targetFloor);
        generateFloorIfNeeded(ow, targetFloor, center);
        sp.teleportTo(ow, center.getX() + 0.5, center.getY() + 1.0, center.getZ() - 7.5, 0.0F, 0.0F);
        ow.playSound(null, center, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.9F, 1.15F);
        sp.displayClientMessage(Component.literal("§5§l[MÊ CUNG RAMIRIS - TẦNG " + targetFloor + "/100] §e" + getFloorThemeTitle(targetFloor)), false);
    }

    private String getFloorThemeTitle(int f) {
        if (f == 100) return "Ngai Vàng Thần Long - Hộ Vệ Tối Thượng";
        if (f >= 81) return "Ma Giới Vực Sâu (Tầng 81-99)";
        if (f >= 61) return "Hỏa Ngục Dung Nham (Tầng 61-80)";
        if (f >= 41) return "Băng Ngục Vĩnh Cửu (Tầng 41-60)";
        if (f >= 21) return "Độc Lâm Huyễn Cảnh (Tầng 21-40)";
        return "Phế Tích Cổ Xưa (Tầng 1-20)";
    }

    private void generateFloorIfNeeded(ServerLevel ow, int floor, BlockPos center) {
        if (!generatedFloors.contains(floor)) {
            int half = 11;
            int h = 8;
            Block wall = getWallBlock(floor);
            Block floorBlk = getFloorBlock(floor);

            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    for (int dy = -1; dy <= h; dy++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        boolean isShell = (dy == -1 || dy == h || Math.abs(dx) == half || Math.abs(dz) == half);
                        if (isShell) {
                            Block b = (dy == h && (dx == 0 || dz == 0)) ? Blocks.SEA_LANTERN : (dy == -1 ? floorBlk : wall);
                            ow.setBlock(p, b.defaultBlockState(), 2);
                        } else {
                            ow.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            // Trụ cột & Cổng chuyển tầng
            for (int sx : new int[]{-5, 5}) {
                for (int sz : new int[]{-5, 5}) {
                    for (int dy = 0; dy < h; dy++) {
                        ow.setBlock(center.offset(sx, dy, sz), wall.defaultBlockState(), 2);
                    }
                }
            }
            // Cổng lên tầng tiếp theo (ở đầu phía Nam) & Cổng thoát (phía Bắc)
            ow.setBlock(center.offset(0, 0, 8), ModBlocks.LABYRINTH_GATEWAY.get().defaultBlockState(), 3);
            ow.setBlock(center.offset(0, 0, -9), Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
            ow.setBlock(center.offset(0, 1, -9), ModBlocks.LABYRINTH_GATEWAY.get().defaultBlockState(), 3);

            generatedFloors.add(floor);
            setDirty();
        }

        // Thả quái canh tầng nếu căn phòng hiện đang trống
        List<Mob> guardians = ow.getEntitiesOfClass(Mob.class, new AABB(center).inflate(12.0D),
                m -> m.isAlive() && m.getTags().contains("LabyrinthFloorMob"));
        if (guardians.isEmpty()) {
            spawnFloorGuardians(ow, floor, center);
        }
    }

    private Block getWallBlock(int floor) {
        if (floor == 100) return Blocks.OBSIDIAN;
        if (floor >= 81) return Blocks.CRYING_OBSIDIAN;
        if (floor >= 61) return Blocks.NETHER_BRICKS;
        if (floor >= 41) return Blocks.BLUE_ICE;
        if (floor >= 21) return Blocks.MOSSY_STONE_BRICKS;
        return Blocks.DEEPSLATE_BRICKS;
    }

    private Block getFloorBlock(int floor) {
        if (floor == 100) return Blocks.GILDED_BLACKSTONE;
        if (floor >= 81) return Blocks.POLISHED_BLACKSTONE;
        if (floor >= 61) return Blocks.MAGMA_BLOCK;
        if (floor >= 41) return Blocks.PACKED_ICE;
        if (floor >= 21) return Blocks.MOSS_BLOCK;
        return Blocks.DEEPSLATE_TILES;
    }

    private void spawnFloorGuardians(ServerLevel ow, int floor, BlockPos center) {
        if (floor == 100) {
            VeldoraEntity boss = ModEntities.VELDORA.get().create(ow);
            if (boss != null) {
                boss.addTag("LabyrinthFloorMob");
                boss.setHumanForm(true);
                boss.setCustomName(Component.literal("§6§l[TẦNG 100 - BÁ CHỦ MÊ CUNG] §b§lVeldora Avatar"));
                boss.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 4.0);
                ow.addFreshEntity(boss);
            }
            return;
        }
        if (floor % 10 == 0) {
            // Tầng Boss mỗi 10 tầng!
            if (floor >= 70) {
                UnderworldDemonEntity lord = ModEntities.UNDERWORLD_DEMON.get().create(ow);
                if (lord != null) {
                    lord.addTag("LabyrinthFloorMob");
                    lord.setRank(UnderworldDemonEntity.DemonRank.ARCHDEMON);
                    lord.setCustomName(Component.literal("§c§l[ĐẠI MA TƯỚNG TẦNG " + floor + "]"));
                    lord.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 4.0);
                    ow.addFreshEntity(lord);
                }
            } else {
                OrcDisasterGeldEntity boss = ModEntities.ORC_DISASTER_GELD.get().create(ow);
                if (boss != null) {
                    boss.addTag("LabyrinthFloorMob");
                    boss.setCustomName(Component.literal("§6§l[THỦ LĨNH TẦNG " + floor + "] §cChiến Tướng Orc"));
                    boss.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 4.0);
                    ow.addFreshEntity(boss);
                }
            }
            return;
        }

        int count = Math.min(6, 2 + floor / 18);
        for (int i = 0; i < count; i++) {
            Mob mob;
            if (floor >= 81) {
                mob = ModEntities.UNDERWORLD_DEMON.get().create(ow);
            } else if (floor >= 61) {
                mob = EntityType.WITHER_SKELETON.create(ow);
            } else if (floor >= 41) {
                mob = EntityType.STRAY.create(ow);
            } else if (floor >= 21) {
                mob = EntityType.CAVE_SPIDER.create(ow);
            } else {
                mob = EntityType.SKELETON.create(ow);
            }
            if (mob != null) {
                mob.addTag("LabyrinthFloorMob");
                mob.setPos(center.getX() + (ow.random.nextDouble() - 0.5) * 8.0, center.getY() + 0.5, center.getZ() + 2.5 + i * 0.5);
                ow.addFreshEntity(mob);
            }
        }
    }

    public static void rewardFloorClearIfBoss(ServerLevel ow, int floor, BlockPos pos) {
        if (floor == 100) {
            ow.addFreshEntity(new ItemEntity(ow, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ(), new ItemStack(Items.NETHERITE_INGOT, 8)));
            ow.addFreshEntity(new ItemEntity(ow, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ(), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3)));
        } else if (floor % 10 == 0) {
            ow.addFreshEntity(new ItemEntity(ow, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ(), new ItemStack(Items.DIAMOND, 6)));
        }
    }
}
