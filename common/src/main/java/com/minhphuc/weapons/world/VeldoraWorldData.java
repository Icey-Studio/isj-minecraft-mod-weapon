package com.minhphuc.weapons.world;

import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.content.tensura.SageManager;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lưu vị trí DUY NHẤT của Hang Động Phong Ấn Bạo Phong Long Veldora trong toàn bộ Overworld.
 * Tối ưu Zero-Lag: Chỉ tạo cấu trúc hang & đặt kết giới Long Giam Vô Hạn khi người chơi đến gần
 * hoặc dịch chuyển không gian tới.
 */
public class VeldoraWorldData extends SavedData {

    private static final String DATA_NAME = "weapons_veldora_cave_data";

    private int caveX;
    private int caveY;
    private int caveZ;
    private boolean coordinatesRolled = false;
    private boolean caveGenerated = false;
    private boolean veldoraUnsealed = false;
    private UUID unsealedByPlayer = null;
    private UUID prisonUuid = null;
    private UUID activeVeldoraUuid = null;
    private boolean veldoraDead = false;
    private boolean veldoraStoredInStomach = false;
    private boolean veldoraHumanForm = false;

    public static VeldoraWorldData get(ServerLevel overworld) {
        ServerLevel ow = overworld.getServer().overworld();
        return ow.getDataStorage().computeIfAbsent(
                new Factory<>(VeldoraWorldData::new, VeldoraWorldData::load, null),
                DATA_NAME
        );
    }

    public static VeldoraWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        VeldoraWorldData data = new VeldoraWorldData();
        data.caveX = tag.getInt("CaveX");
        data.caveY = tag.getInt("CaveY");
        data.caveZ = tag.getInt("CaveZ");
        data.coordinatesRolled = tag.getBoolean("CoordsRolled");
        data.caveGenerated = tag.getBoolean("CaveGenerated");
        data.veldoraUnsealed = tag.getBoolean("VeldoraUnsealed");
        if (tag.hasUUID("UnsealedBy")) data.unsealedByPlayer = tag.getUUID("UnsealedBy");
        if (tag.hasUUID("PrisonUuid")) data.prisonUuid = tag.getUUID("PrisonUuid");
        if (tag.hasUUID("ActiveVeldoraUuid")) data.activeVeldoraUuid = tag.getUUID("ActiveVeldoraUuid");
        data.veldoraDead = tag.getBoolean("VeldoraDead");
        data.veldoraStoredInStomach = tag.getBoolean("VeldoraStoredInStomach");
        data.veldoraHumanForm = tag.getBoolean("VeldoraHumanForm");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("CaveX", caveX);
        tag.putInt("CaveY", caveY);
        tag.putInt("CaveZ", caveZ);
        tag.putBoolean("CoordsRolled", coordinatesRolled);
        tag.putBoolean("CaveGenerated", caveGenerated);
        tag.putBoolean("VeldoraUnsealed", veldoraUnsealed);
        if (unsealedByPlayer != null) tag.putUUID("UnsealedBy", unsealedByPlayer);
        if (prisonUuid != null) tag.putUUID("PrisonUuid", prisonUuid);
        if (activeVeldoraUuid != null) tag.putUUID("ActiveVeldoraUuid", activeVeldoraUuid);
        tag.putBoolean("VeldoraDead", veldoraDead);
        tag.putBoolean("VeldoraStoredInStomach", veldoraStoredInStomach);
        tag.putBoolean("VeldoraHumanForm", veldoraHumanForm);
        return tag;
    }

    @org.jetbrains.annotations.Nullable
    public UUID getActiveVeldoraUuid() {
        return activeVeldoraUuid;
    }

    public void setActiveVeldoraUuid(@org.jetbrains.annotations.Nullable UUID uuid) {
        this.activeVeldoraUuid = uuid;
        if (uuid != null) {
            this.veldoraDead = false;
            this.veldoraStoredInStomach = false;
        }
        setDirty();
    }

    public boolean isVeldoraDead() {
        return veldoraDead;
    }

    public void setVeldoraDead(boolean dead, boolean humanForm) {
        this.veldoraDead = dead;
        this.veldoraHumanForm = humanForm;
        if (dead) {
            this.activeVeldoraUuid = null;
            this.veldoraStoredInStomach = false;
        }
        setDirty();
    }

    public boolean isVeldoraStoredInStomach() {
        return veldoraStoredInStomach;
    }

    public void setVeldoraStoredInStomach(boolean stored, boolean humanForm) {
        this.veldoraStoredInStomach = stored;
        this.veldoraHumanForm = humanForm;
        if (stored) {
            this.activeVeldoraUuid = null;
            this.veldoraDead = false;
        }
        setDirty();
    }

    public boolean isVeldoraHumanForm() {
        return veldoraHumanForm;
    }

    /**
     * Tìm cá thể Veldora DUY NHẤT trên toàn server; nếu tồn tại nhiều bản sao dư thừa thì tự động xóa bỏ bản thừa.
     */
    @org.jetbrains.annotations.Nullable
    public static VeldoraEntity findSingletonVeldora(net.minecraft.server.MinecraftServer server) {
        if (server == null) return null;
        VeldoraWorldData vd = get(server.overworld());
        java.util.List<VeldoraEntity> found = new java.util.ArrayList<>();
        for (ServerLevel sl : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity e : sl.getAllEntities()) {
                if (e instanceof VeldoraEntity v && v.isAlive()) {
                    found.add(v);
                }
            }
        }
        if (found.isEmpty()) return null;

        VeldoraEntity primary = null;
        if (vd.activeVeldoraUuid != null) {
            for (VeldoraEntity v : found) {
                if (v.getUUID().equals(vd.activeVeldoraUuid)) {
                    primary = v;
                    break;
                }
            }
        }
        if (primary == null) {
            for (VeldoraEntity v : found) {
                if (v.isAllied()) {
                    primary = v;
                    break;
                }
            }
        }
        if (primary == null) primary = found.get(0);

        for (VeldoraEntity v : found) {
            if (v != primary) {
                v.discard();
            }
        }
        if (!primary.getUUID().equals(vd.activeVeldoraUuid)) {
            vd.setActiveVeldoraUuid(primary.getUUID());
        }
        return primary;
    }

    public BlockPos getOrRollCavePos(ServerLevel ow) {
        if (!coordinatesRolled) {
            long seed = ow.getSeed();
            double angle = ((seed & 0xFFFF) / 65535.0D) * Math.PI * 2.0D;
            double dist = 420.0D + ((seed >> 16) & 0x1FF); // Cách (0,0) khoảng 420 ~ 930 block
            caveX = (int) Math.round(Math.cos(angle) * dist);
            caveZ = (int) Math.round(Math.sin(angle) * dist);
            caveY = 46;
            coordinatesRolled = true;
            setDirty();
        }
        return new BlockPos(caveX, caveY, caveZ);
    }

    public boolean isCaveGenerated() {
        return caveGenerated;
    }

    public boolean isVeldoraUnsealed() {
        return veldoraUnsealed;
    }

    public void markUnsealed(UUID playerUuid) {
        this.veldoraUnsealed = true;
        this.unsealedByPlayer = playerUuid;
        setDirty();
    }

    /**
     * Đảm bảo Hang Động Phong Ấn, Bạo Phong Long Veldora (nếu chưa giải ấn) và vòm 3D Lồng Giam Vô Hạn
     * luôn tồn tại đồng bộ (kể cả sau khi người chơi thoát ra vào lại thế giới).
     */
    public void ensurePrisonRegisteredIfSealed(ServerLevel level) {
        if (level == null || level.dimension() != Level.OVERWORLD || veldoraUnsealed || !caveGenerated) return;
        BlockPos center = getOrRollCavePos(level);
        InfiniteDragonPrisonAbility.castForVeldoraCave(level, Vec3.atBottomCenterOf(center.below()));
    }

    public void ensureCaveGenerated(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        BlockPos center = getOrRollCavePos(level);
        boolean needsCarve = !caveGenerated || (!veldoraUnsealed && level.getBlockState(center.above(16)).isSolid());
        if (needsCarve) {
            carveCaveStructure(level, center);
            caveGenerated = true;
            setDirty();
        }

        if (!veldoraUnsealed && !veldoraDead && !veldoraStoredInStomach) {
            // Sinh DUY NHẤT 1 Bạo Phong Long Veldora đang bị phong ấn nếu chưa tồn tại cá thể nào
            if (findSingletonVeldora(level.getServer()) == null) {
                VeldoraEntity veldora = ModEntities.VELDORA.get().create(level);
                if (veldora != null) {
                    veldora.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
                    veldora.setSealed(true);
                    veldora.setHumanForm(false);
                    veldora.setCustomName(Component.literal("§b§l[PHONG ẤN] §f§lBạo Phong Long Veldora"));
                    level.addFreshEntity(veldora);
                    setActiveVeldoraUuid(veldora.getUUID());
                }
            }
            // Đặt & duy trì vòm Lồng Giam Vô Hạn 3D cỡ đại (R=15m, H=16m)
            InfiniteDragonPrisonAbility.castForVeldoraCave(level, Vec3.atBottomCenterOf(center.below()));
        }
    }

    /**
     * Tái tạo hoàn chỉnh Hang Động Phong Ấn Bạo Phong Long Veldora (nếu người chơi lỡ tay phá hỏng).
     */
    public static BlockPos rebuildCaveForPlayer(MinecraftServer server, @Nullable ServerPlayer player) {
        ServerLevel ow = server.overworld();
        VeldoraWorldData vd = get(ow);
        BlockPos center = vd.getOrRollCavePos(ow);
        vd.carveCaveStructure(ow, center);
        vd.caveGenerated = true;
        vd.setDirty();

        // Nếu Veldora vẫn chưa được giải phong ấn thì phục hồi luôn kết giới & Veldora bị phong ấn
        if (!vd.veldoraUnsealed && !vd.veldoraDead && !vd.veldoraStoredInStomach) {
            vd.ensureCaveGenerated(ow);
        }

        ow.sendParticles(ParticleTypes.ENCHANTED_HIT, center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5, 80, 8.0, 4.0, 8.0, 0.08);
        ow.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.05F);

        if (player != null) {
            String coords = "(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")";
            SageManager.sendCornerAlert(player, "Đã Tái Tạo Hang Động Veldora tại Overworld " + coords + "!", 0xFF38BDF8, 6500, false);
            player.displayClientMessage(Component.literal("§b§l[KHÔNG GIAN ẢO] §aĐã tái tạo hoàn chỉnh Hang Động Bạo Phong Long Veldora tại §e" + coords + "§a! (Dùng §b[V] §ađể dịch chuyển tới ngay)"), false);
            if (player.level() instanceof ServerLevel pLevel) {
                pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.1F, 1.25F);
            }
        }
        return center;
    }

    private void carveCaveStructure(ServerLevel level, BlockPos center) {
        int rx = 26;
        int ry = 22;
        int minChunkX = (center.getX() - rx) >> 4;
        int maxChunkX = (center.getX() + rx) >> 4;
        int minChunkZ = (center.getZ() - rx) >> 4;
        int maxChunkZ = (center.getZ() + rx) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.getChunk(cx, cz);
            }
        }

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rx; dz <= rx; dz++) {
                double horiz = Math.hypot(dx, dz);
                if (horiz > rx) continue;
                for (int dy = -2; dy <= ry; dy++) {
                    mPos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (dy == -2) {
                        level.setBlock(mPos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                    } else if (dy == -1) {
                        boolean ringTile = ((Math.abs((int) Math.round(horiz))) % 5 == 0);
                        level.setBlock(mPos, ringTile ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
                    } else if (horiz >= rx - 1.5 || dy == ry) {
                        level.setBlock(mPos, (dx + dy + dz) % 7 == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Bốn trụ đá thạch anh ma thuật ở 4 góc ngoài ranh giới Lồng Giam 15m
        int[][] pillars = {{17, 17}, {-17, 17}, {17, -17}, {-17, -17}};
        for (int[] p : pillars) {
            for (int dy = 0; dy <= 16; dy++) {
                level.setBlock(center.offset(p[0], dy, p[1]), (dy % 4 == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Bệ phong ấn hoàng kim ở chính giữa & đèn biển chiếu sáng vòng ngoài Lồng Giam 15m
        level.setBlock(center.below(), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(center.offset(18, 0, 0), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(center.offset(-18, 0, 0), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 0, 18), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 0, -18), Blocks.SEA_LANTERN.defaultBlockState(), 2);
    }
}
