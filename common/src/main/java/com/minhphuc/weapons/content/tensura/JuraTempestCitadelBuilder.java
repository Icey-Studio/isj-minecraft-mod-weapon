package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kiến trúc sư kiến tạo không gian riêng [THÀNH TRÌ JURA TEMPEST] (Bản 3.0):
 * - Chiều không gian riêng: weapons:jura_tempest (trời nắng vàng & cánh anh đào bay, 0 quái tự sinh).
 * - Kiến trúc Đá Thạch Anh Trắng & Vàng Hoàng Kim chuẩn Thủ Đô Rimuru:
 *   1. Quảng Trường Khải Hoàn & Cổng Dị Giới Thần Môn (0-9) + Đài Phun Nước Ma Tố.
 *   2. Đại Điện Ngai Vàng & Bàn Tròn Hội Đồng Thủy Tổ (với Bệ Quản Trị Thành Trì).
 *   3. Cánh Tây: Khu Onsen Hoàng Gia & Vườn Anh Đào Nhật Bản (hồi máu + hồi Ma Tố x3).
 *   4. Cánh Đông: Kho Hoàng Gia (cứu hộ 100% đồ từ bản cũ), Xưởng Thủ Công, Thư Phòng Phù Phép Lv.30 & Đền Tinh Linh.
 *   5. Cầu Pha Lê & Đảo Đấu Trường Tối Thượng (Colosseum Vô Cực tách biệt để test Boss & Bia Sát Thương).
 */
public final class JuraTempestCitadelBuilder {

    public static final ResourceKey<Level> DIMENSION_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "jura_tempest")
    );

    public static final int CITADEL_VERSION = 3;

    // Tọa độ tương đối từ tâm Thành Trì (cx, cy, cz)
    public static final int GATE_DX = 0, GATE_DY = 1, GATE_DZ = -28;
    public static final int SPAWN_DX = 0, SPAWN_DY = 1, SPAWN_DZ = -22;
    public static final int THRONE_LECTERN_DX = 0, THRONE_LECTERN_DY = 2, THRONE_LECTERN_DZ = 11;
    public static final int COUNCIL_TABLE_DX = 0, COUNCIL_TABLE_DY = 1, COUNCIL_TABLE_DZ = 0;
    public static final int ONSEN_MIN_DX = -29, ONSEN_MAX_DX = -21, ONSEN_MIN_DZ = -6, ONSEN_MAX_DZ = 2;
    public static final int SHRINE_DX = 27, SHRINE_DY = 1, SHRINE_DZ = 8;
    public static final int CRAFT_CENTER_DX = 27, CRAFT_CENTER_DY = 1, CRAFT_CENTER_DZ = -4;
    public static final int COLOSSEUM_PODIUM_DX = -3, COLOSSEUM_PODIUM_DY = 1, COLOSSEUM_PODIUM_DZ = 46;
    public static final int COLOSSEUM_CENTER_DX = 0, COLOSSEUM_CENTER_DY = 1, COLOSSEUM_CENTER_DZ = 63;
    public static final int COLOSSEUM_RADIUS = 17;

    private JuraTempestCitadelBuilder() {}

    public static ServerLevel resolveTargetLevel(MinecraftServer server) {
        if (server == null) return null;
        ServerLevel custom = server.getLevel(DIMENSION_KEY);
        return custom != null ? custom : server.overworld();
    }

    public static boolean isCustomDimension(Level level) {
        return level != null && DIMENSION_KEY.equals(level.dimension());
    }

    public static BlockPos getCitadelCenter(ServerLevel targetLevel, UUID playerUuid) {
        int slot = Math.abs(playerUuid.hashCode() % 100);
        if (isCustomDimension(targetLevel)) {
            return new BlockPos(slot * 256, 120, (slot / 10) * 256);
        }
        // Fallback an toàn cho world save cũ nếu chưa nạp được custom dimension
        return new BlockPos(260000 + slot * 256, 200, 260000 + (slot / 10) * 256);
    }

    public static BlockPos getLegacyOverworldCenter(UUID playerUuid) {
        int slot = Math.abs(playerUuid.hashCode() % 10000);
        return new BlockPos(200000 + slot * 64, 240, 200000 + (slot / 100) * 64);
    }

    public static Vec3 getPlazaGatePos(BlockPos center) {
        return new Vec3(center.getX() + 0.5, center.getY() + GATE_DY, center.getZ() + GATE_DZ + 0.5);
    }

    public static Vec3 getPlazaSpawnPos(BlockPos center) {
        return new Vec3(center.getX() + 0.5, center.getY() + SPAWN_DY, center.getZ() + SPAWN_DZ + 0.5);
    }

    public static Vec3 getColosseumCenterPos(BlockPos center) {
        return new Vec3(center.getX() + COLOSSEUM_CENTER_DX + 0.5, center.getY() + COLOSSEUM_CENTER_DY + 0.2, center.getZ() + COLOSSEUM_CENTER_DZ + 0.5);
    }

    public static boolean isInsideCitadelOrDimension(Level level, Vec3 pos) {
        if (level == null || pos == null) return false;
        if (isCustomDimension(level)) return true;
        MinecraftServer srv = level.getServer();
        if (srv != null && level == srv.overworld()) {
            // Hỗ trợ cả tọa độ fallback v3 và tọa độ v2 cũ
            if (pos.x >= 258000 && pos.z >= 258000 && pos.y >= 175 && pos.y <= 265) return true;
            return pos.x >= 195000 && pos.z >= 195000 && pos.y >= 220 && pos.y <= 275;
        }
        return false;
    }

    public static boolean isInsideColosseumZone(BlockPos center, Vec3 pos) {
        double dx = pos.x - (center.getX() + COLOSSEUM_CENTER_DX + 0.5);
        double dz = pos.z - (center.getZ() + COLOSSEUM_CENTER_DZ + 0.5);
        return (dx * dx + dz * dz) <= (COLOSSEUM_RADIUS + 4) * (COLOSSEUM_RADIUS + 4)
                && pos.y >= center.getY() - 5 && pos.y <= center.getY() + 28;
    }

    public static boolean isPodiumBlock(BlockPos center, BlockPos pos) {
        BlockPos throne = center.offset(THRONE_LECTERN_DX, THRONE_LECTERN_DY, THRONE_LECTERN_DZ);
        BlockPos arena = center.offset(COLOSSEUM_PODIUM_DX, COLOSSEUM_PODIUM_DY, COLOSSEUM_PODIUM_DZ);
        return pos.equals(throne) || pos.equals(arena);
    }

    public static void buildIfAbsent(ServerLevel targetLevel, ServerPlayer player, BlockPos center) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        int builtVer = data.getInt("TensuraSanctuaryVersion");
        BlockPos marker = center.offset(0, 0, -20);
        if (builtVer >= CITADEL_VERSION && !targetLevel.getBlockState(marker).isAir()) {
            JuraTempestManager.ensureInitialResidents(targetLevel, player, center);
            return;
        }

        // 1. Cứu hộ 100% vật phẩm từ rương/thùng của bản cũ (cả ở tâm mới và tọa độ đảo v2 Overworld)
        List<ItemStack> rescuedItems = new ArrayList<>();
        collectContainerItems(targetLevel, center, 24, rescuedItems);

        ServerLevel overworld = player.server.overworld();
        BlockPos legacyCenter = getLegacyOverworldCenter(player.getUUID());
        if (overworld != targetLevel || !legacyCenter.equals(center)) {
            // Đảm bảo nạp tạm 3x3 chunk ở tọa độ cũ để cứu đồ không bỏ sót ô nào
            int lcChunkX = legacyCenter.getX() >> 4;
            int lcChunkZ = legacyCenter.getZ() >> 4;
            for (int cxOff = -1; cxOff <= 1; cxOff++) {
                for (int czOff = -1; czOff <= 1; czOff++) {
                    overworld.getChunk(lcChunkX + cxOff, lcChunkZ + czOff);
                }
            }
            collectContainerItems(overworld, legacyCenter, 16, rescuedItems);
        }

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        // 2. Dọn sạch không gian vùng Thành Trì chính (79x65x26)
        clearRegion(targetLevel, cx - 39, cx + 39, cy - 8, cy + 20, cz - 37, cz + 25);

        // 3. Xây Nền Lục Địa Đá Thạch Anh Trắng & Vàng Hoàng Kim + Viền Barrier An Toàn
        buildMainFoundation(targetLevel, cx, cy, cz);

        // 4. Quảng Trường Khải Hoàn + Cổng Thần Môn + Đài Phun Nước Ma Tố
        buildTriumphalPlazaAndGate(targetLevel, cx, cy, cz);

        // 5. Đại Điện Ngai Vàng & Bàn Tròn Hội Đồng Thủy Tổ
        buildThronePalace(targetLevel, cx, cy, cz);

        // 6. Cánh Tây: Khu Onsen Hoàng Gia & Rừng Hoa Anh Đào
        buildRoyalOnsenAndSakuraGarden(targetLevel, cx, cy, cz);

        // 7. Cánh Đông: Kho Hoàng Gia (cất toàn bộ đồ cứu hộ) + Thư Phòng Phù Phép + Xưởng Thủ Công + Đền Tinh Linh
        buildEastCraftAndTreasuryWing(targetLevel, cx, cy, cz, rescuedItems);

        // 8. Cầu Pha Lê & Đảo Đấu Trường Tối Thượng (Colosseum Vô Cực)
        buildCrystalBridge(targetLevel, cx, cy, cz);
        buildColosseumIsland(targetLevel, center);

        data.putInt("TensuraSanctuaryVersion", CITADEL_VERSION);
        data.putBoolean("TempestCitadelEstablished", true);
        JuraTempestManager.ensureInitialResidents(targetLevel, player, center);
    }

    private static void collectContainerItems(ServerLevel level, BlockPos center, int radiusXZ, List<ItemStack> out) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
            for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                for (int dy = -3; dy <= 14; dy++) {
                    mpos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(mpos);
                    if (be instanceof Container cont) {
                        for (int i = 0; i < cont.getContainerSize(); i++) {
                            ItemStack st = cont.getItem(i);
                            if (!st.isEmpty()) {
                                out.add(st.copy());
                            }
                        }
                        cont.clearContent();
                    }
                }
            }
        }
    }

    private static void clearRegion(ServerLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mpos.set(x, y, z);
                    if (!level.getBlockState(mpos).isAir()) {
                        level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void buildMainFoundation(ServerLevel level, int cx, int cy, int cz) {
        // Thành Trì rộng dx = -36..+36, dz = -34..+23 (bán kính bo góc kiểu đảo nổi hoàng gia)
        for (int dx = -36; dx <= 36; dx++) {
            for (int dz = -34; dz <= 23; dz++) {
                double normX = Math.abs(dx) / 36.2;
                double normZ = (dz < -5) ? (Math.abs(dz + 5) / 29.2) : (Math.abs(dz - 5) / 18.8);
                double metric = Math.pow(normX, 3.2) + Math.pow(normZ, 3.2);
                if (metric <= 1.0) {
                    boolean isGoldBorder = (metric > 0.90) || (Math.abs(dx) == 17 && dz >= -18 && dz <= 18);
                    boolean isCentralAxis = (Math.abs(dx) <= 2 && dz >= -32 && dz <= 22);
                    BlockState surface;
                    if (isGoldBorder) {
                        surface = ((dx + dz) % 2 == 0) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
                    } else if (isCentralAxis) {
                        surface = (Math.abs(dx) == 2) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
                    } else if (dx <= -18 && dz >= -18 && dz <= 16) {
                        // Vườn Anh Đào phía Tây nền cỏ rêu tự nhiên
                        surface = ((dx * 5 + dz * 7) % 9 == 0) ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                    } else {
                        surface = ((dx + dz) % 4 == 0) ? Blocks.QUARTZ_BRICKS.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(cx + dx, cy, cz + dz), surface, 2);

                    int depth = Math.max(2, (int) Math.round((1.0 - metric) * 5.0) + 1);
                    for (int d = 1; d <= depth; d++) {
                        BlockState under = (d == 1)
                                ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                                : (d == depth ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.STONE.defaultBlockState());
                        level.setBlock(new BlockPos(cx + dx, cy - d, cz + dz), under, 2);
                    }
                } else if (metric <= 1.12) {
                    // Tường vô hình bảo vệ quanh mép Thành Trì (chừa cổng ra Cầu Pha Lê ở phía Nam dx=-3..3, dz > 20)
                    boolean bridgeExit = (dz > 18 && Math.abs(dx) <= 3);
                    if (!bridgeExit) {
                        for (int by = 1; by <= 6; by++) {
                            level.setBlock(new BlockPos(cx + dx, cy + by, cz + dz), Blocks.BARRIER.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private static void buildTriumphalPlazaAndGate(ServerLevel level, int cx, int cy, int cz) {
        // Cổng Khải Hoàn Đá Trắng & Vàng bọc Cổng Dị Giới Thần Môn tại dz = -28
        for (int side : new int[]{-3, 3}) {
            for (int dy = 1; dy <= 6; dy++) {
                BlockState s = (dy == 1 || dy == 6) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState();
                level.setBlock(new BlockPos(cx + side, cy + dy, cz + GATE_DZ), s, 2);
            }
            level.setBlock(new BlockPos(cx + side, cy + 7, cz + GATE_DZ), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        }
        for (int dx = -4; dx <= 4; dx++) {
            level.setBlock(new BlockPos(cx + dx, cy + 6, cz + GATE_DZ),
                    (Math.abs(dx) % 2 == 0) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
        }

        // Đài Phun Nước Ma Tố Trung Tâm Quảng Trường tại (0, 1, -18)
        int fz = -18;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double r = Math.hypot(dx, dz);
                BlockPos fp = new BlockPos(cx + dx, cy + 1, cz + fz + dz);
                if (r >= 2.6 && r <= 3.6) {
                    level.setBlock(fp, (Math.abs(dx) == 3 || Math.abs(dz) == 3) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_SLAB.defaultBlockState(), 2);
                } else if (r < 2.6) {
                    level.setBlock(new BlockPos(cx + dx, cy, cz + fz + dz), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                    level.setBlock(fp, Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(new BlockPos(cx, cy + dy, cz + fz),
                    dy == 3 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(cx, cy + 4, cz + fz), Blocks.WATER.defaultBlockState(), 2);

        // Hàng Đèn Lồng Hoàng Gia dọc trục Quảng Trường
        for (int dz : new int[]{-25, -21, -14}) {
            for (int sx : new int[]{-5, 5}) {
                level.setBlock(new BlockPos(cx + sx, cy + 1, cz + dz), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx + sx, cy + 2, cz + dz), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx + sx, cy + 3, cz + dz), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
            }
        }
    }

    private static void buildThronePalace(ServerLevel level, int cx, int cy, int cz) {
        // Đại Điện Hoàng Gia: dx = -15..+15, dz = -11..+19, trần cao dy = 1..13
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -11; dz <= 19; dz++) {
                // Thảm đỏ hoàng gia dẫn từ cổng điện vào Ngai Vàng
                if (Math.abs(dx) <= 1 && dz >= -10 && dz <= 11) {
                    level.setBlock(new BlockPos(cx + dx, cy + 1, cz + dz),
                            Math.abs(dx) == 0 ? Blocks.RED_CARPET.defaultBlockState() : Blocks.YELLOW_CARPET.defaultBlockState(), 2);
                }

                boolean onFrontBack = (dz == -11 || dz == 19);
                boolean onLeftRight = (Math.abs(dx) == 15);
                if (!onFrontBack && !onLeftRight) continue;

                // Lối vào chính diện (dz=-11, dx=-3..+3, dy=1..5) & lối thông sang Onsen/Đông/Cầu Pha Lê
                boolean isNorthGate = (dz == -11 && Math.abs(dx) <= 3);
                boolean isSouthBridgeGate = (dz == 19 && Math.abs(dx) <= 2);
                boolean isSideDoor = (onLeftRight && dz >= -2 && dz <= 2);
                boolean isPillar = ((Math.abs(dx) == 15 && (dz == -11 || dz == -3 || dz == 5 || dz == 12 || dz == 19))
                        || (onFrontBack && Math.abs(dx) % 5 == 0));

                for (int dy = 1; dy <= 10; dy++) {
                    if ((isNorthGate || isSouthBridgeGate || isSideDoor) && dy <= 5) continue;
                    BlockPos wp = new BlockPos(cx + dx, cy + dy, cz + dz);
                    if (isPillar) {
                        BlockState ps = (dy == 1 || dy == 10) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState();
                        level.setBlock(wp, ps, 2);
                    } else if (dy == 1 || dy == 9 || dy == 10) {
                        level.setBlock(wp, dy == 9 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState(), 2);
                    } else if (dy >= 3 && dy <= 7 && (Math.abs(dx) % 5 != 0)) {
                        level.setBlock(wp, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2);
                    } else {
                        level.setBlock(wp, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Mái Vòm Giếng Trời Hoàng Gia (dy = 11..13)
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -11; dz <= 19; dz++) {
                boolean skylight = (Math.abs(dx) <= 6 && dz >= -4 && dz <= 10);
                boolean goldRib = (Math.abs(dx) == 7 || dz == -5 || dz == 11 || Math.abs(dx) == 15 || dz == -11 || dz == 19);
                BlockState roof = skylight
                        ? Blocks.CYAN_STAINED_GLASS.defaultBlockState()
                        : (goldRib ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_SLAB.defaultBlockState());
                level.setBlock(new BlockPos(cx + dx, cy + 11, cz + dz), roof, 2);
                if (skylight && (Math.abs(dx) == 6 || dz == -4 || dz == 10)) {
                    level.setBlock(new BlockPos(cx + dx, cy + 11, cz + dz), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                }
            }
        }

        // Đèn chùm pha lê treo trần
        for (int dz : new int[]{-5, 2, 10}) {
            for (int dy = 9; dy <= 10; dy++) {
                level.setBlock(new BlockPos(cx, cy + dy, cz + dz), Blocks.END_ROD.defaultBlockState(), 2);
            }
            level.setBlock(new BlockPos(cx, cy + 8, cz + dz), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        }

        // Bàn Tròn Hội Đồng Thủy Tổ tại (0, 1, 0)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                BlockState top = (dx == 0 && dz == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
                level.setBlock(new BlockPos(cx + dx, cy + 1, cz + dz), top, 2);
            }
        }
        // Ghế Hội Đồng 4 hướng quanh Bàn Tròn
        level.setBlock(new BlockPos(cx - 3, cy + 1, cz), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
        level.setBlock(new BlockPos(cx + 3, cy + 1, cz), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
        level.setBlock(new BlockPos(cx, cy + 1, cz - 3), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(new BlockPos(cx, cy + 1, cz + 3), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);

        // Bệ Ngai Vàng Hoàng Kim (dz = +12..+17)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = 12; dz <= 17; dz++) {
                BlockState step = (dz == 12 || Math.abs(dx) == 4) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
                level.setBlock(new BlockPos(cx + dx, cy + 1, cz + dz), step, 2);
            }
        }
        // Tựa Ngai Vàng Rimuru
        for (int dy = 2; dy <= 5; dy++) {
            level.setBlock(new BlockPos(cx - 1, cy + dy, cz + 16), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(cx + 1, cy + dy, cz + 16), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(cx, cy + dy, cz + 16), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(cx, cy + 2, cz + 15),
                Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(new BlockPos(cx, cy + 6, cz + 16), Blocks.SEA_LANTERN.defaultBlockState(), 2);

        // BỆ QUẢN TRỊ THÀNH TRÌ (Lectern click để mở GUI điều hành toàn bộ Thành Trì!)
        level.setBlock(new BlockPos(cx, cy + 2, cz + THRONE_LECTERN_DZ),
                Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH), 2);
        level.setBlock(new BlockPos(cx - 1, cy + 2, cz + THRONE_LECTERN_DZ), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 1, cy + 2, cz + THRONE_LECTERN_DZ), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
    }

    private static void buildRoyalOnsenAndSakuraGarden(ServerLevel level, int cx, int cy, int cz) {
        // Hồ Onsen Hoàng Gia (dx = -29..-21, dz = -6..+2)
        for (int dx = ONSEN_MIN_DX - 1; dx <= ONSEN_MAX_DX + 1; dx++) {
            for (int dz = ONSEN_MIN_DZ - 1; dz <= ONSEN_MAX_DZ + 1; dz++) {
                boolean rim = (dx == ONSEN_MIN_DX - 1 || dx == ONSEN_MAX_DX + 1 || dz == ONSEN_MIN_DZ - 1 || dz == ONSEN_MAX_DZ + 1);
                if (rim) {
                    level.setBlock(new BlockPos(cx + dx, cy + 1, cz + dz),
                            ((dx + dz) % 3 == 0) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 2);
                } else {
                    level.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz),
                            ((dx + dz) % 4 == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
        // Đèn lồng bao quanh Onsen
        for (int[] lamp : new int[][]{{-30, -7}, {-20, -7}, {-30, 3}, {-20, 3}}) {
            level.setBlock(new BlockPos(cx + lamp[0], cy + 2, cz + lamp[1]), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        }

        // 4 Cây Hoa Anh Đào Nhật Bản nở rộ quanh vườn Tây
        int[][] treeRoots = {{-31, -13}, {-21, -13}, {-31, 9}, {-22, 10}};
        for (int[] root : treeRoots) {
            int tx = cx + root[0];
            int tz = cz + root[1];
            for (int dy = 1; dy <= 5; dy++) {
                level.setBlock(new BlockPos(tx, cy + dy, tz), Blocks.CHERRY_LOG.defaultBlockState(), 2);
            }
            for (int lx = -2; lx <= 2; lx++) {
                for (int lz = -2; lz <= 2; lz++) {
                    for (int ly = 4; ly <= 6; ly++) {
                        if (Math.abs(lx) + Math.abs(lz) + (ly == 6 ? 1 : 0) <= 3) {
                            BlockPos lp = new BlockPos(tx + lx, cy + ly, tz + lz);
                            if (level.getBlockState(lp).isAir()) {
                                level.setBlock(lp, Blocks.CHERRY_LEAVES.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void buildEastCraftAndTreasuryWing(ServerLevel level, int cx, int cy, int cz, List<ItemStack> rescuedItems) {
        // 1. KHO HOÀNG GIA (dx = +20..+32, dz = -14..-9): 8 Rương Đôi + 8 Thùng + 2 Ender Chest
        List<BlockPos> storageSlots = new ArrayList<>();
        for (int bz = -14; bz <= -11; bz++) {
            BlockPos bp = new BlockPos(cx + 31, cy + 1, cz + bz);
            level.setBlock(bp, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.WEST), 2);
            storageSlots.add(bp);
            BlockPos bp2 = new BlockPos(cx + 31, cy + 2, cz + bz);
            level.setBlock(bp2, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.WEST), 2);
            storageSlots.add(bp2);
        }
        // 4 Rương Đôi quay mặt về phía Đông (WEST side looking EAST at cx+29)
        for (int pair = 0; pair < 2; pair++) {
            int rzL = -14 + pair * 2;
            int rzR = rzL + 1;
            BlockPos cL = new BlockPos(cx + 28, cy + 1, cz + rzL);
            BlockPos cR = new BlockPos(cx + 28, cy + 1, cz + rzR);
            level.setBlock(cL, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST).setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
            level.setBlock(cR, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST).setValue(ChestBlock.TYPE, ChestType.LEFT), 2);
            storageSlots.add(cL);
            storageSlots.add(cR);

            BlockPos cL2 = new BlockPos(cx + 25, cy + 1, cz + rzL);
            BlockPos cR2 = new BlockPos(cx + 25, cy + 1, cz + rzR);
            level.setBlock(cL2, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST).setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
            level.setBlock(cR2, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST).setValue(ChestBlock.TYPE, ChestType.LEFT), 2);
            storageSlots.add(cL2);
            storageSlots.add(cR2);
        }
        level.setBlock(new BlockPos(cx + 31, cy + 1, cz - 10), Blocks.ENDER_CHEST.defaultBlockState().setValue(EnderChestBlock.FACING, Direction.WEST), 2);

        // Chuyển trả toàn bộ vật phẩm cũ vào Kho Hoàng Gia
        int itemIdx = 0;
        for (BlockPos sp : storageSlots) {
            if (itemIdx >= rescuedItems.size()) break;
            BlockEntity be = level.getBlockEntity(sp);
            if (be instanceof Container cont) {
                for (int s = 0; s < cont.getContainerSize() && itemIdx < rescuedItems.size(); s++) {
                    cont.setItem(s, rescuedItems.get(itemIdx++));
                }
            }
        }

        // 2. XƯỞNG RÈN MA THIẾT & THƯ PHÒNG PHÙ PHÉP LV.30 (quanh dz = -6..+2)
        level.setBlock(new BlockPos(cx + 27, cy + 1, cz - 4), Blocks.ENCHANTING_TABLE.defaultBlockState(), 2);
        for (int bx = 25; bx <= 29; bx++) {
            for (int bz = -6; bz <= -2; bz++) {
                if (bx == 25 && bz >= -5 && bz <= -3) continue; // Chừa lối vào từ phía Tây
                if (bx == 25 || bx == 29 || bz == -6 || bz == -2) {
                    level.setBlock(new BlockPos(cx + bx, cy + 1, cz + bz), Blocks.BOOKSHELF.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(cx + bx, cy + 2, cz + bz), Blocks.BOOKSHELF.defaultBlockState(), 2);
                }
            }
        }
        // Dãy bàn chế tác Nghệ Nhân Tempest (dz = 0..2)
        level.setBlock(new BlockPos(cx + 22, cy + 1, cz + 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 23, cy + 1, cz + 1), Blocks.ANVIL.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 24, cy + 1, cz + 1), Blocks.SMITHING_TABLE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 25, cy + 1, cz + 1), Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlastFurnaceBlock.FACING, Direction.NORTH), 2);
        level.setBlock(new BlockPos(cx + 26, cy + 1, cz + 1), Blocks.BREWING_STAND.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 27, cy + 1, cz + 1), Blocks.GRINDSTONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 28, cy + 1, cz + 1), Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.NORTH), 2);
        level.setBlock(new BlockPos(cx + 29, cy + 1, cz + 1), Blocks.STONECUTTER.defaultBlockState(), 2);

        // 3. ĐỀN THỜ TINH LINH NGUYÊN TỐ (cx + 27, cy + 1, cz + 8)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                if (corner) {
                    for (int dy = 1; dy <= 4; dy++) {
                        level.setBlock(new BlockPos(cx + SHRINE_DX + dx, cy + dy, cz + SHRINE_DZ + dz),
                                dy == 4 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                    }
                }
                level.setBlock(new BlockPos(cx + SHRINE_DX + dx, cy + 5, cz + SHRINE_DZ + dz),
                        (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.QUARTZ_SLAB.defaultBlockState(), 2);
            }
        }
        level.setBlock(new BlockPos(cx + SHRINE_DX, cy + 1, cz + SHRINE_DZ), ModBlocks.SPIRIT_SHRINE.get().defaultBlockState(), 2);
    }

    private static void buildCrystalBridge(ServerLevel level, int cx, int cy, int cz) {
        // Cầu Pha Lê dài nối Đại Điện (dz=20) sang Đấu Trường (dz=45)
        for (int dz = 20; dz <= 45; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                BlockState st;
                if (Math.abs(dx) == 3) {
                    st = (dz % 4 == 0) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState();
                    level.setBlock(new BlockPos(cx + dx, cy + 1, cz + dz), st, 2);
                    level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                } else {
                    st = (Math.abs(dx) == 2) ? Blocks.SMOOTH_QUARTZ.defaultBlockState()
                            : ((dz % 5 == 0 && dx == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.CYAN_STAINED_GLASS.defaultBlockState());
                    level.setBlock(new BlockPos(cx + dx, cy, cz + dz), st, 2);
                }
            }
        }
    }

    public static void buildColosseumIsland(ServerLevel level, BlockPos center) {
        int ax = center.getX() + COLOSSEUM_CENTER_DX;
        int ay = center.getY();
        int az = center.getZ() + COLOSSEUM_CENTER_DZ;

        // Xóa sạch không gian đấu trường trước khi dựng lại
        clearRegion(level, ax - 21, ax + 21, ay - 4, ay + 20, az - 21, az + 21);

        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                double dist = Math.hypot(dx, dz);
                if (dist <= COLOSSEUM_RADIUS + 2.2) {
                    // Đáy đấu trường chắc chắn bằng Gilded Blackstone + Quartz
                    BlockState floor;
                    if (dist <= 3.5) {
                        floor = (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SEA_LANTERN.defaultBlockState();
                    } else if (dist >= COLOSSEUM_RADIUS - 0.8 && dist <= COLOSSEUM_RADIUS + 0.8) {
                        floor = Blocks.GOLD_BLOCK.defaultBlockState();
                    } else if (((int) Math.round(dist)) % 5 == 0) {
                        floor = Blocks.QUARTZ_BRICKS.defaultBlockState();
                    } else {
                        floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(ax + dx, ay, az + dz), floor, 2);
                    level.setBlock(new BlockPos(ax + dx, ay - 1, az + dz), Blocks.BEDROCK.defaultBlockState(), 2);

                    // Tường thành đấu trường + Rào vô hình ngăn boss bay mất (chừa lối vào Cầu Pha Lê ở phía Bắc)
                    boolean northBridgeDoor = (dz < -COLOSSEUM_RADIUS + 2 && Math.abs(dx) <= 3);
                    if (dist >= COLOSSEUM_RADIUS + 0.8 && !northBridgeDoor) {
                        for (int dy = 1; dy <= 3; dy++) {
                            level.setBlock(new BlockPos(ax + dx, ay + dy, az + dz),
                                    (dy == 3 && ((dx + dz) % 2 == 0)) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState(), 2);
                        }
                        for (int dy = 4; dy <= 14; dy++) {
                            level.setBlock(new BlockPos(ax + dx, ay + dy, az + dz), Blocks.BARRIER.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // BỆ TRIỆU HỒI BOSS & BIA THỬ SÁT THƯƠNG ĐẤU TRƯỜNG
        BlockPos podiumPos = center.offset(COLOSSEUM_PODIUM_DX, COLOSSEUM_PODIUM_DY, COLOSSEUM_PODIUM_DZ);
        level.setBlock(podiumPos, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST), 2);
        level.setBlock(podiumPos.below(), Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
    }
}
