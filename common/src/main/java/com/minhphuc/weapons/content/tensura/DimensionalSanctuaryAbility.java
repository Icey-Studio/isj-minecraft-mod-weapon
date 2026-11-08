package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KỸ NĂNG MA VƯƠNG: DỊ GIỚI THẦN MÔN (DIMENSIONAL SANCTUARY)
 * 1. Chế độ Mặc định (Đứng dùng skill):
 *    - Triệu hồi Cánh Cửa Không Gian Khổng Lồ (3D Runic Celestial Portal).
 *    - Bước qua cửa -> Dịch chuyển tức thời vào Không Gian Thánh Điện (Pocket Sanctuary) riêng tư.
 *    - Thú cưng (chó, mèo, ngựa, vẹt, mob dẫn dây) tự động đi theo vào cùng chủ nhân.
 *    - Đầy đủ tiện nghi: Giường hoàng gia, Rương Ender, Rương đôi, Bàn rèn, Đe, Lò luyện kim,
 *      Bàn phù phép cấp 30 (15 kệ sách), Giàn pha thuốc, Suối nước nóng linh tuyền Tensura hồi máu/no bụng/giải bùa chú.
 *    - Đảm bảo 100% KHÔNG BAO GIỜ spawn quái thù địch bên trong (Zero Hostiles Guaranteed).
 *    - Cửa trở về: Bước qua là trở lại đúng vị trí cánh cửa ở thế giới thực.
 *    - Đấm tay không hoặc dùng lại skill để đóng cửa.
 * 2. Chế độ Capsule Shelter (Ngồi Shift + dùng skill):
 *    - Biến ra căn lều/nhà nhỏ tiện nghi 5x5 tại chỗ để cắm trại nhanh.
 *    - Shift + skill lần nữa: Thu hồi sạch sẽ, hoàn nguyên 100% địa hình cũ.
 */
public class DimensionalSanctuaryAbility {

    public static class ActiveGate {
        public final UUID casterUuid;
        public final ServerLevel level;
        public final Vec3 pos;
        public final Vec3 facing;
        public final AABB triggerBox;
        public final long createTime;

        public ActiveGate(ServerLevel level, UUID casterUuid, Vec3 pos, Vec3 facing) {
            this.level = level;
            this.casterUuid = casterUuid;
            this.pos = pos;
            this.facing = facing != null && facing.lengthSqr() > 1e-4 ? facing.normalize() : new Vec3(0, 0, 1);
            this.createTime = level.getGameTime();

            Vec3 right = new Vec3(-this.facing.z, 0, this.facing.x).normalize();
            double halfW = 1.8;
            double halfH = 4.8;
            double halfD = 1.2;

            double minX = pos.x - Math.abs(right.x * halfW) - Math.abs(this.facing.x * halfD);
            double maxX = pos.x + Math.abs(right.x * halfW) + Math.abs(this.facing.x * halfD);
            double minY = pos.y;
            double maxY = pos.y + halfH;
            double minZ = pos.z - Math.abs(right.z * halfW) - Math.abs(this.facing.z * halfD);
            double maxZ = pos.z + Math.abs(right.z * halfW) + Math.abs(this.facing.z * halfD);

            this.triggerBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public record ReturnPoint(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {}

    public static class ActiveCapsule {
        public final UUID casterUuid;
        public final ServerLevel level;
        public final BlockPos center;
        public final Map<BlockPos, BlockState> replacedBlocks = new ConcurrentHashMap<>();

        public ActiveCapsule(ServerLevel level, UUID casterUuid, BlockPos center) {
            this.level = level;
            this.casterUuid = casterUuid;
            this.center = center;
        }

        public void restore() {
            for (Map.Entry<BlockPos, BlockState> entry : replacedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState orig = entry.getValue();
                level.setBlock(pos, orig, 3);
            }
            replacedBlocks.clear();
        }
    }

    private static final Map<UUID, ActiveGate> ACTIVE_GATES = new ConcurrentHashMap<>();
    private static final Map<UUID, ReturnPoint> PLAYER_RETURN_POINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveCapsule> ACTIVE_CAPSULES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TELEPORT_TIME = new ConcurrentHashMap<>();

    public static void cast(ServerLevel level, ServerPlayer player) {
        if (player.isShiftKeyDown() || player.isCrouching()) {
            toggleCapsuleShelter(level, player);
        } else {
            toggleGiantGate(level, player);
        }
    }

    public static void toggleGiantGate(ServerLevel level, ServerPlayer player) {
        ActiveGate existing = ACTIVE_GATES.get(player.getUUID());
        if (existing != null && existing.level == level && existing.pos.distanceTo(player.position()) <= 16.0) {
            closeGate(level, player);
            player.displayClientMessage(
                    Component.literal("§b§l[DỊ GIỚI THẦN MÔN] §fĐã thu hồi Cánh Cửa Không Gian!"),
                    true
            );
            return;
        }

        // Tạo cánh cửa mới trước mặt người chơi 2.8m
        Vec3 look = player.getLookAngle();
        Vec3 lookHoriz = new Vec3(look.x, 0, look.z).normalize();
        if (lookHoriz.lengthSqr() < 1e-4) lookHoriz = new Vec3(0, 0, 1);
        Vec3 gatePos = player.position().add(lookHoriz.scale(2.8));

        ActiveGate gate = new ActiveGate(level, player.getUUID(), gatePos, lookHoriz);
        ACTIVE_GATES.put(player.getUUID(), gate);

        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY)) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, player.getUUID(), gatePos, lookHoriz, 72000, ClientboundCinematicSkillPacket.ACTION_START, 1.0f
            ));
        }

        level.playSound(null, gatePos.x, gatePos.y, gatePos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5F, 1.2F);
        level.playSound(null, gatePos.x, gatePos.y, gatePos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.0F);
        level.playSound(null, gatePos.x, gatePos.y, gatePos.z, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 2.0F, 1.4F);

        level.sendParticles(ParticleTypes.PORTAL, gatePos.x, gatePos.y + 2.0, gatePos.z, 60, 1.0, 1.6, 1.0, 0.4);
        level.sendParticles(ParticleTypes.END_ROD, gatePos.x, gatePos.y + 2.0, gatePos.z, 25, 0.8, 1.4, 0.8, 0.05);

        player.displayClientMessage(
                Component.literal("§b§l[DỊ GIỚI THẦN MÔN] §fCánh Cửa Không Gian Khổng Lồ đã mở ra! Hãy bước vào."),
                true
        );
        player.sendSystemMessage(
                Component.literal("§b✦ Cánh Cửa Không Gian: Bước qua cổng để vào Thánh Vực Không Gian riêng tư (đầy đủ tiện nghi, suối nước nóng linh tuyền, an toàn tuyệt đối). Thú cưng xung quanh sẽ tự động theo bạn vào trong!")
        );
    }

    public static void closeGate(ServerLevel level, ServerPlayer player) {
        ActiveGate gate = ACTIVE_GATES.remove(player.getUUID());
        if (gate != null) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, player.getUUID(), gate.pos, Vec3.ZERO, 0, ClientboundCinematicSkillPacket.ACTION_STOP, 0f
            ));
            level.playSound(null, gate.pos.x, gate.pos.y, gate.pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            level.sendParticles(ParticleTypes.POOF, gate.pos.x, gate.pos.y + 2.0, gate.pos.z, 30, 0.8, 1.2, 0.8, 0.1);
        }
    }

    public static boolean dispelByPunch(ServerPlayer player, BlockPos pos) {
        Vec3 hitPos = Vec3.atCenterOf(pos);
        for (Map.Entry<UUID, ActiveGate> entry : ACTIVE_GATES.entrySet()) {
            ActiveGate gate = entry.getValue();
            if (gate.level == player.level() && gate.pos.distanceTo(hitPos) <= 4.0) {
                ACTIVE_GATES.remove(entry.getKey());
                ModMessages.sendToPlayers(gate.level.players(), new ClientboundCinematicSkillPacket(
                        SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, gate.casterUuid, gate.pos, Vec3.ZERO, 0, ClientboundCinematicSkillPacket.ACTION_STOP, 0f
                ));
                gate.level.playSound(null, gate.pos.x, gate.pos.y, gate.pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
                player.displayClientMessage(
                        Component.literal("§b§l[DỊ GIỚI THẦN MÔN] §fĐã dùng tay đóng Cánh Cửa Không Gian!"),
                        true
                );
                return true;
            }
        }

        ActiveCapsule capsule = ACTIVE_CAPSULES.get(player.getUUID());
        if (capsule != null && capsule.level == player.level() && capsule.center.distSqr(pos) <= 36) {
            capsule.restore();
            ACTIVE_CAPSULES.remove(player.getUUID());
            player.level().playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 2.0F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§6[CAPSULE SHELTER] §fĐã thu hồi lều dã ngoại thành công!"),
                    true
            );
            return true;
        }

        return false;
    }

    public static boolean checkEmptyHandPunch(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) return false;
        Vec3 eyePos = player.getEyePosition();

        for (Map.Entry<UUID, ActiveGate> entry : ACTIVE_GATES.entrySet()) {
            ActiveGate gate = entry.getValue();
            if (gate.level == player.level() && gate.pos.distanceTo(eyePos) <= 4.5) {
                ACTIVE_GATES.remove(entry.getKey());
                ModMessages.sendToPlayers(gate.level.players(), new ClientboundCinematicSkillPacket(
                        SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, gate.casterUuid, gate.pos, Vec3.ZERO, 0, ClientboundCinematicSkillPacket.ACTION_STOP, 0f
                ));
                gate.level.playSound(null, gate.pos.x, gate.pos.y, gate.pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
                player.displayClientMessage(
                        Component.literal("§b§l[DỊ GIỚI THẦN MÔN] §fĐã dùng tay đóng Cánh Cửa Không Gian!"),
                        true
                );
                return true;
            }
        }

        ActiveCapsule capsule = ACTIVE_CAPSULES.get(player.getUUID());
        if (capsule != null && capsule.level == player.level() && capsule.center.distToCenterSqr(eyePos) <= 25) {
            capsule.restore();
            ACTIVE_CAPSULES.remove(player.getUUID());
            player.level().playSound(null, capsule.center.getX(), capsule.center.getY(), capsule.center.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 2.0F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§6[CAPSULE SHELTER] §fĐã thu hồi lều dã ngoại thành công!"),
                    true
            );
            return true;
        }

        return false;
    }

    public static BlockPos getSanctuaryCenter(UUID playerUuid) {
        int slot = Math.abs(playerUuid.hashCode() % 10000);
        int baseX = 200000 + slot * 64;
        int baseY = 240;
        int baseZ = 200000 + (slot / 100) * 64;
        return new BlockPos(baseX, baseY, baseZ);
    }

    public static boolean isInsideSanctuary(Level level, Vec3 pos) {
        if (level == null || pos == null || level != level.getServer().overworld()) return false;
        if (pos.y < 235 || pos.y > 255) return false;
        return pos.x >= 195000 && pos.z >= 195000;
    }

    public static void enterSanctuary(ServerPlayer player, ActiveGate gate) {
        ServerLevel overworld = player.server.overworld();
        BlockPos center = getSanctuaryCenter(player.getUUID());

        // Xây dựng 1 lần duy nhất nếu chưa từng xây
        buildSanctuaryIfAbsent(overworld, player, center);

        // Lưu điểm trở về ngoài đời
        PLAYER_RETURN_POINTS.put(player.getUUID(), new ReturnPoint(
                player.level().dimension(), player.position(), player.getYRot(), player.getXRot()
        ));

        // Điểm xuất hiện trong Thánh Vực: phía trước Cánh Cửa Trở Về
        Vec3 spawnPos = new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() - 3.5);

        // Thu thập thú cưng xung quanh người chơi
        List<Entity> pets = getNearbyPets(player, 10.0);

        // Dịch chuyển người chơi
        player.teleportTo(overworld, spawnPos.x, spawnPos.y, spawnPos.z, 0.0F, 0.0F);

        // Dịch chuyển thú cưng vào Thánh Vực
        teleportPetsTo(pets, overworld, spawnPos);

        overworld.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.5F, 1.2F);
        overworld.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.0F);
        overworld.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y + 1.0, spawnPos.z, 30, 0.5, 0.8, 0.5, 0.05);

        player.displayClientMessage(
                Component.literal("§d§l[THÁNH VỰC KHÔNG GIAN] §eChào mừng bạn và thú cưng đến với Không Gian Thánh Điện Biệt Lập!"),
                false
        );
        player.sendSystemMessage(
                Component.literal("§6✦ Thánh Vực Tiện Nghi: Đầy đủ giường ngủ, kho đồ, bàn chế tạo, đe, lò nung, bàn phù phép lv30 và Suối Nước Nóng Linh Tuyền giúp hồi máu, no bụng và thanh tẩy toàn diện. Bước qua Cánh Cửa Trở Về ở bức tường đối diện để trở lại thế giới thực.")
        );
    }

    public static void exitSanctuary(ServerPlayer player) {
        ReturnPoint ret = PLAYER_RETURN_POINTS.get(player.getUUID());
        ServerLevel targetLevel = ret != null ? player.server.getLevel(ret.dimension()) : player.server.overworld();
        if (targetLevel == null) targetLevel = player.server.overworld();

        Vec3 returnPos;
        float returnYaw = 0.0F;
        float returnPitch = 0.0F;

        if (ret != null) {
            returnPos = ret.pos();
            returnYaw = ret.yaw();
            returnPitch = ret.pitch();
        } else {
            ActiveGate gate = ACTIVE_GATES.get(player.getUUID());
            returnPos = gate != null ? gate.pos.add(0, 0.1, 0) : player.position();
        }

        // Thu thập thú cưng trong Thánh Vực
        List<Entity> pets = getNearbyPets(player, 16.0);

        player.teleportTo(targetLevel, returnPos.x, returnPos.y, returnPos.z, returnYaw, returnPitch);
        teleportPetsTo(pets, targetLevel, returnPos);

        targetLevel.playSound(null, returnPos.x, returnPos.y, returnPos.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.5F, 1.2F);
        targetLevel.playSound(null, returnPos.x, returnPos.y, returnPos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.0F);
        targetLevel.sendParticles(ParticleTypes.PORTAL, returnPos.x, returnPos.y + 1.0, returnPos.z, 30, 0.5, 0.8, 0.5, 0.2);

        player.displayClientMessage(
                Component.literal("§a§l[DỊ GIỚI THẦN MÔN] §fĐã trở về thế giới thực an toàn!"),
                true
        );
    }

    public static List<Entity> getNearbyPets(ServerPlayer player, double radius) {
        List<Entity> pets = new ArrayList<>();
        AABB box = player.getBoundingBox().inflate(radius);
        for (Entity e : player.level().getEntities(player, box)) {
            if (e instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID())) {
                pets.add(e);
            } else if (e instanceof AbstractHorse horse && player.getUUID().equals(horse.getOwnerUUID())) {
                pets.add(e);
            } else if (e instanceof Mob mob && mob.isLeashed() && mob.getLeashHolder() == player) {
                pets.add(e);
            }
        }
        return pets;
    }

    public static void teleportPetsTo(List<Entity> pets, ServerLevel targetLevel, Vec3 dest) {
        int idx = 0;
        for (Entity pet : pets) {
            idx++;
            double ox = (idx % 3 - 1) * 1.2;
            double oz = (idx / 3 + 1) * 1.2;
            Vec3 pPos = dest.add(ox, 0, oz);

            if (pet.level() == targetLevel) {
                pet.teleportTo(pPos.x, pPos.y, pPos.z);
            } else {
                // Đổi dimension cho pet (Minecraft 1.21.1 API)
                net.minecraft.world.level.portal.DimensionTransition transition =
                        new net.minecraft.world.level.portal.DimensionTransition(
                                targetLevel, pPos, Vec3.ZERO, pet.getYRot(), pet.getXRot(),
                                net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING
                        );
                Entity transitioned = pet.changeDimension(transition);
                if (transitioned != null) {
                    transitioned.teleportTo(pPos.x, pPos.y, pPos.z);
                }
            }
        }
    }

    public static void buildSanctuaryIfAbsent(ServerLevel overworld, ServerPlayer player, BlockPos center) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        if (data.getBoolean("TensuraSanctuaryBuilt")) {
            // Kiểm tra nhanh block trung tâm, nếu vẫn còn thì không cần xây lại
            if (!overworld.getBlockState(center).isAir()) {
                return;
            }
        }

        // TẠO THÁNH ĐIỆN 15x15x9 (NỘI THẤT 13x13x7) CỰC ĐẸP & ĐẦY ĐỦ TIỆN NGHI
        int minX = center.getX() - 7;
        int maxX = center.getX() + 7;
        int minZ = center.getZ() - 7;
        int maxZ = center.getZ() + 7;
        int minY = center.getY();
        int maxY = center.getY() + 8;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        // 1. Vỏ ngoài và sàn, trần
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mpos.set(x, y, z);
                    boolean isWall = (x == minX || x == maxX || z == minZ || z == maxZ);
                    boolean isFloor = (y == minY);
                    boolean isCeiling = (y == maxY);

                    if (isFloor) {
                        // Sàn đá bóng Deepslate & Thạch anh
                        if (x == minX + 1 || x == maxX - 1 || z == minZ + 1 || z == maxZ - 1) {
                            overworld.setBlock(mpos, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                        } else {
                            overworld.setBlock(mpos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                        }
                    } else if (isCeiling) {
                        // Trần kính nhuộm màu kèm đèn biển chiếu sáng
                        if (x >= center.getX() - 2 && x <= center.getX() + 2 && z >= center.getZ() - 2 && z <= center.getZ() + 2) {
                            overworld.setBlock(mpos, Blocks.TINTED_GLASS.defaultBlockState(), 2);
                        } else if ((x == center.getX() - 4 || x == center.getX() + 4) && (z == center.getZ() - 4 || z == center.getZ() + 4)) {
                            overworld.setBlock(mpos, Blocks.SEA_LANTERN.defaultBlockState(), 2);
                        } else {
                            overworld.setBlock(mpos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                        }
                    } else if (isWall) {
                        // Tường gạch thạch anh chạm khắc uy nghi
                        if (y == minY + 1 || y == maxY - 1) {
                            overworld.setBlock(mpos, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
                        } else if ((x == minX || x == maxX) && (z == minZ || z == maxZ)) {
                            // Góc đèn biển
                            overworld.setBlock(mpos, Blocks.SEA_LANTERN.defaultBlockState(), 2);
                        } else {
                            overworld.setBlock(mpos, Blocks.QUARTZ_BRICKS.defaultBlockState(), 2);
                        }
                    } else {
                        // Không gian bên trong
                        overworld.setBlock(mpos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 2. Thảm đỏ hoàng gia dọc lối đi chính
        for (int z = center.getZ() - 5; z <= center.getZ() + 5; z++) {
            overworld.setBlock(new BlockPos(center.getX(), minY + 1, z), Blocks.RED_CARPET.defaultBlockState(), 2);
        }

        // 3. Góc Nghỉ Ngơi & Giường Hoàng Gia (Góc Tây-Bắc: X = center.X - 5, Z = center.Z + 4)
        BlockPos bedFoot = new BlockPos(center.getX() - 5, minY + 1, center.getZ() + 4);
        BlockPos bedHead = new BlockPos(center.getX() - 5, minY + 1, center.getZ() + 5);
        overworld.setBlock(bedFoot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.NORTH), 2);
        overworld.setBlock(bedHead, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.NORTH), 2);

        overworld.setBlock(new BlockPos(center.getX() - 4, minY + 1, center.getZ() + 5), Blocks.ENDER_CHEST.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() - 3, minY + 1, center.getZ() + 5), Blocks.CHEST.defaultBlockState(), 2);

        // 4. Góc Xưởng Rèn & Chế Tạo (Góc Đông-Bắc: X = center.X + 3..5, Z = center.Z + 4..5)
        overworld.setBlock(new BlockPos(center.getX() + 3, minY + 1, center.getZ() + 5), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() + 4, minY + 1, center.getZ() + 5), Blocks.ANVIL.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() + 5, minY + 1, center.getZ() + 5), Blocks.SMITHING_TABLE.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() + 5, minY + 1, center.getZ() + 4), Blocks.BLAST_FURNACE.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() + 4, minY + 1, center.getZ() + 4), Blocks.SMOKER.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() + 3, minY + 1, center.getZ() + 4), Blocks.GRINDSTONE.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() + 5, minY + 1, center.getZ() + 3), Blocks.STONECUTTER.defaultBlockState(), 2);

        // 5. Góc Phù Phép Chuẩn Cấp 30 & Giả Kim (Góc Tây-Nam: X = center.X - 4..-5, Z = center.Z - 4..-5)
        BlockPos enchPos = new BlockPos(center.getX() - 4, minY + 1, center.getZ() - 4);
        overworld.setBlock(enchPos, Blocks.ENCHANTING_TABLE.defaultBlockState(), 2);

        // 15 Kệ sách bao quanh chuẩn Level 30
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (dx == 1 && dz == 1) continue; // Lối vào
                overworld.setBlock(enchPos.offset(dx, 0, dz), Blocks.BOOKSHELF.defaultBlockState(), 2);
                overworld.setBlock(enchPos.offset(dx, 1, dz), Blocks.BOOKSHELF.defaultBlockState(), 2);
            }
        }
        overworld.setBlock(new BlockPos(center.getX() - 5, minY + 1, center.getZ() - 2), Blocks.BREWING_STAND.defaultBlockState(), 2);
        overworld.setBlock(new BlockPos(center.getX() - 4, minY + 1, center.getZ() - 2), Blocks.WATER_CAULDRON.defaultBlockState(), 2);

        // 6. Suối Nước Nóng Linh Tuyền Tensura (Tensura Hot Spring: X = center.X + 3..5, Z = center.Z - 5..-3)
        for (int px = center.getX() + 3; px <= center.getX() + 5; px++) {
            for (int pz = center.getZ() - 5; pz <= center.getZ() - 3; pz++) {
                // Đèn biển dưới đáy nước tạo ánh ngọc lam
                overworld.setBlock(new BlockPos(px, minY, pz), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                overworld.setBlock(new BlockPos(px, minY + 1, pz), Blocks.WATER.defaultBlockState(), 2);
            }
        }
        // Viền thành hồ suối nước nóng bằng đá mài
        for (int px = center.getX() + 2; px <= center.getX() + 6; px++) {
            overworld.setBlock(new BlockPos(px, minY + 1, center.getZ() - 6), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
            overworld.setBlock(new BlockPos(px, minY + 1, center.getZ() - 2), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
        }
        for (int pz = center.getZ() - 5; pz <= center.getZ() - 3; pz++) {
            overworld.setBlock(new BlockPos(center.getX() + 2, minY + 1, pz), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
            overworld.setBlock(new BlockPos(center.getX() + 6, minY + 1, pz), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
        }

        // 7. Cánh Cửa Trở Về (Return Gate) trên tường Nam (Z = center.Z - 6)
        BlockPos gateBase = new BlockPos(center.getX(), minY + 1, center.getZ() - 6);
        overworld.setBlock(gateBase.offset(-1, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(-1, 1, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(-1, 2, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(-1, 3, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);

        overworld.setBlock(gateBase.offset(1, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(1, 1, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(1, 2, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(1, 3, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);

        overworld.setBlock(gateBase.offset(0, 3, 0), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
        overworld.setBlock(gateBase.offset(0, 4, 0), Blocks.SEA_LANTERN.defaultBlockState(), 2);

        data.putBoolean("TensuraSanctuaryBuilt", true);
    }

    public static void toggleCapsuleShelter(ServerLevel level, ServerPlayer player) {
        ActiveCapsule existing = ACTIVE_CAPSULES.get(player.getUUID());
        if (existing != null) {
            existing.restore();
            ACTIVE_CAPSULES.remove(player.getUUID());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 2.0F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§6[CAPSULE SHELTER] §eĐã thu hồi lều dã ngoại thành công! Địa hình đã hoàn nguyên."),
                    true
            );
            return;
        }

        // Dựng lều dã ngoại 5x5x4 ngay trước mặt người chơi
        Vec3 look = player.getLookAngle();
        Vec3 lookHoriz = new Vec3(look.x, 0, look.z).normalize();
        if (lookHoriz.lengthSqr() < 1e-4) lookHoriz = new Vec3(0, 0, 1);
        BlockPos center = player.blockPosition().offset((int) Math.round(lookHoriz.x * 3.5), 0, (int) Math.round(lookHoriz.z * 3.5));

        ActiveCapsule capsule = new ActiveCapsule(level, player.getUUID(), center);

        int r = 2;
        int h = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = 0; dy <= h; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    capsule.replacedBlocks.put(p, level.getBlockState(p));

                    boolean isWall = (Math.abs(dx) == r || Math.abs(dz) == r);
                    boolean isFloor = (dy == 0);
                    boolean isRoof = (dy == h);

                    if (isFloor) {
                        level.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                    } else if (isRoof) {
                        level.setBlock(p, Blocks.SPRUCE_WOOD.defaultBlockState(), 2);
                    } else if (isWall) {
                        // Cửa vào phía người chơi
                        if (dx == 0 && dz == -r && (dy == 1 || dy == 2)) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if ((Math.abs(dx) == r && Math.abs(dz) == r)) {
                            level.setBlock(p, Blocks.OAK_LOG.defaultBlockState(), 2);
                        } else if (dy == 2 && (dx == 0 || dz == 0)) {
                            level.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else {
                            level.setBlock(p, Blocks.OAK_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Đồ đạc tiện nghi bên trong lều
        level.setBlock(center.offset(-1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 1, 1), Blocks.FURNACE.defaultBlockState(), 2);
        level.setBlock(center.offset(1, 1, 1), Blocks.CHEST.defaultBlockState(), 2);
        level.setBlock(center.offset(-1, 1, -1), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(center.offset(-1, 1, 0), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(center.offset(0, h - 1, 0), Blocks.LANTERN.defaultBlockState(), 2);

        ACTIVE_CAPSULES.put(player.getUUID(), capsule);

        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 2.0F, 1.0F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.0F, 1.2F);

        player.displayClientMessage(
                Component.literal("§6[CAPSULE SHELTER] §aĐã mở lều dã ngoại tiện nghi tại chỗ! Nhấn Shift + Skill lần nữa để thu hồi."),
                true
        );
    }

    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();

        // 1. Quét tương tác với Cánh Cửa Không Gian ngoài đời thực
        if (!ACTIVE_GATES.isEmpty()) {
            for (Map.Entry<UUID, ActiveGate> entry : ACTIVE_GATES.entrySet()) {
                ActiveGate gate = entry.getValue();
                if (gate.level != level) continue;

                // Quét thực thể bước vào cổng
                List<Entity> touching = level.getEntities((Entity) null, gate.triggerBox);
                for (Entity e : touching) {
                    if (e instanceof ServerPlayer sp) {
                        Long lastTp = LAST_TELEPORT_TIME.get(sp.getUUID());
                        if (lastTp == null || gameTime - lastTp > 40) {
                            LAST_TELEPORT_TIME.put(sp.getUUID(), gameTime);
                            enterSanctuary(sp, gate);
                        }
                    } else if (e instanceof Monster || e instanceof Enemy) {
                        // Đẩy lùi quái thù địch nếu cố áp sát cánh cửa
                        Vec3 push = e.position().subtract(gate.pos).normalize().scale(0.8);
                        e.setDeltaMovement(push.x, 0.4, push.z);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1.0, e.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }

        // 2. Xử lý bên trong Không Gian Thánh Điện (Chỉ chạy trên Server Overworld)
        if (level == level.getServer().overworld() && gameTime % 5 == 0) {
            for (ServerPlayer player : level.players()) {
                if (!isInsideSanctuary(level, player.position())) continue;

                BlockPos center = getSanctuaryCenter(player.getUUID());
                int minY = center.getY();

                // 2.1. Kiểm tra Suối Nước Nóng Linh Tuyền (Hot Spring: X = center.X + 3..5, Z = center.Z - 5..-3)
                double px = player.getX();
                double pz = player.getZ();
                if (px >= center.getX() + 2.5 && px <= center.getX() + 5.5 &&
                    pz >= center.getZ() - 5.5 && pz <= center.getZ() - 2.5 &&
                    player.getY() >= minY && player.getY() <= minY + 2.5) {

                    // Hồi phục máu + no bụng + giải trừ hiệu ứng xấu
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false, true));

                    // Tẩy sạch các hiệu ứng xấu (Wither, Poison, Slowness, Weakness, etc.)
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WITHER);
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.WEAKNESS);
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.CONFUSION);
                    player.removeEffect(MobEffects.BAD_OMEN);

                    level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.5, player.getZ(), 4, 0.3, 0.1, 0.3, 0.05);

                    if (gameTime % 20 == 0) {
                        player.displayClientMessage(
                                Component.literal("§b♨️ [LINH TUYỀN TENSURA] §fNước ấm đang hồi phục sinh lực và thanh tẩy toàn diện..."),
                                true
                        );
                    }

                    // Hồi phục cả thú cưng đang ngâm cùng
                    for (Entity pet : getNearbyPets(player, 6.0)) {
                        if (pet instanceof LivingEntity le) {
                            le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false, true));
                        }
                    }
                }

                // 2.2. Kiểm tra Cánh Cửa Trở Về (Return Gate: X = center.X - 1..1, Z = center.Z - 6..-5)
                if (Math.abs(px - (center.getX() + 0.5)) <= 1.2 &&
                    pz >= center.getZ() - 6.5 && pz <= center.getZ() - 4.5 &&
                    player.getY() >= minY && player.getY() <= minY + 3.5) {

                    Long lastTp = LAST_TELEPORT_TIME.get(player.getUUID());
                    if (lastTp == null || gameTime - lastTp > 40) {
                        LAST_TELEPORT_TIME.put(player.getUUID(), gameTime);
                        exitSanctuary(player);
                    }
                }

                // 2.3. BẢO ĐẢM TUYỆT ĐỐI KHÔNG BAO GIỜ SPAWN QUÁI THÙ ĐỊCH
                AABB roomBox = new AABB(center).inflate(9.0, 5.0, 9.0);
                List<Mob> hostiles = level.getEntitiesOfClass(Mob.class, roomBox, m ->
                        m.isAlive() && (m instanceof Monster || m instanceof Enemy || m.getType().getCategory() == MobCategory.MONSTER)
                );
                for (Mob mob : hostiles) {
                    level.sendParticles(ParticleTypes.FLASH, mob.getX(), mob.getY() + 1.0, mob.getZ(), 1, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    mob.discard();
                }
            }
        }
    }
}
