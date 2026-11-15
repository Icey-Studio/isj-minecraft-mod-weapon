package com.minhphuc.weapons.world;

import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

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
        return tag;
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

    public void ensureCaveGenerated(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        BlockPos center = getOrRollCavePos(level);
        if (caveGenerated) return;

        // Nạp chunk tại vị trí Hang Động Phong Ấn & chạm rỗng không gian đá ma thuật
        level.getChunk(center);
        int rx = 14;
        int ry = 11;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rx; dz <= rx; dz++) {
                double horiz = Math.hypot(dx, dz);
                if (horiz > rx) continue;
                for (int dy = -2; dy <= ry; dy++) {
                    mPos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (dy == -2) {
                        level.setBlock(mPos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                    } else if (horiz >= rx - 1.5 || dy == ry) {
                        level.setBlock(mPos, (dx + dy + dz) % 7 == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Đặt điểm đánh dấu Quả Cầu Ánh Sáng & Bệ Phong Ấn ở chính giữa
        level.setBlock(center.below(), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(center.offset(8, 0, 0), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(center.offset(-8, 0, 0), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 0, 8), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 0, -8), Blocks.SEA_LANTERN.defaultBlockState(), 2);

        // Sinh Bạo Phong Long Veldora đang bị phong ấn
        if (!veldoraUnsealed && level.getEntitiesOfClass(VeldoraEntity.class, new net.minecraft.world.phys.AABB(center).inflate(16)).isEmpty()) {
            VeldoraEntity veldora = ModEntities.VELDORA.get().create(level);
            if (veldora != null) {
                veldora.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
                veldora.setSealed(true);
                veldora.setHumanForm(false);
                veldora.setCustomName(Component.literal("§b§l[PHONG ẤN] §f§lBạo Phong Long Veldora"));
                level.addFreshEntity(veldora);
            }
        }

        // Đặt vòm Long Giam Vô Hạn bao quanh Veldora
        InfiniteDragonPrisonAbility.castForVeldoraCave(level, Vec3.atBottomCenterOf(center));
        caveGenerated = true;
        setDirty();
    }
}
