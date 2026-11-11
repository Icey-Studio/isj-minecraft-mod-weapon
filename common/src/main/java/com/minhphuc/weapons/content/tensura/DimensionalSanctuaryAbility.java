package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ClientboundOpenGateDialPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KỸ NĂNG MA VƯƠNG: DỊ GIỚI THẦN MÔN 2.0 (DIMENSIONAL SANCTUARY & HOWL'S PORTAL DIAL)
 * 1. Đảo Nổi Biệt Thự Gỗ Sân Vườn Anh Đào (33x33 Floating Sky Island):
 *    - Sân vườn đón nắng tự nhiên, cây Anh Đào rụng cánh hoa hồng, Suối Nước Nóng Lộ Thiên viền gỗ thông bốc khói ấm.
 *    - Biệt thự gỗ Sồi & Thông rộng rãi (23x15, trần vòm cao 8m có giếng trời kính), lò sưởi gạch, phòng ngủ đôi,
 *      tủ thùng gỗ + rương đôi + rương Ender, thư phòng phù phép chuẩn Cấp 30 (26 kệ sách khoảng cách chuẩn), bếp & xưởng rèn.
 *    - Bảo toàn 100% vật phẩm từ nhà cũ khi nâng cấp lên bản 2.0.
 *    - An toàn tuyệt đối: Không bao giờ spawn quái thù địch, rào chắn vô hình quanh mép đảo bảo vệ người chơi & thú cưng.
 * 2. Hệ Thống La Bàn Thần Môn Đánh Số (0 -> 9) (Howl's Moving Castle Chromatic Star-Gates):
 *    - Kênh [0]: Dẫn vào Đảo Nổi Biệt Thự Gỗ (hoặc Quay về chỗ vừa rời đi khi đứng ở cổng sân vườn).
 *    - Kênh [1..9]: 9 Điểm Đánh Dấu (Waypoints) đa sắc xuyên không gian (Overworld <-> Nether <-> End).
 *    - Chuột phải vào Cánh Cửa 3D: Xoay kênh đổi màu cổng & chữ số phát sáng 3D tức thời.
 *    - Shift + Chuột phải vào Cánh Cửa (hoặc Shift + Skill cạnh cửa): Mở Giao Diện La Bàn Thần Môn (0-9).
 */
public class DimensionalSanctuaryAbility {

    public static final int SANCTUARY_VERSION = 2;
    public static final int MAX_WAYPOINT_SLOTS = 9;

    public record GateWaypoint(
            int slot,
            String name,
            String dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {}

    public static class ActiveGate {
        public final UUID casterUuid;
        public final ServerLevel level;
        public final Vec3 pos;
        public final Vec3 facing;
        public final AABB triggerBox;
        public final long createTime;
        public int channel;

        public ActiveGate(ServerLevel level, UUID casterUuid, Vec3 pos, Vec3 facing, int channel) {
            this.level = level;
            this.casterUuid = casterUuid;
            this.pos = pos;
            this.facing = facing != null && facing.lengthSqr() > 1e-4 ? facing.normalize() : new Vec3(0, 0, 1);
            this.createTime = level.getGameTime();
            this.channel = Math.clamp(channel, 0, MAX_WAYPOINT_SLOTS);

            Vec3 right = new Vec3(-this.facing.z, 0, this.facing.x).normalize();
            double halfW = 1.8;
            double halfH = 4.8;
            double halfD = 1.1;

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
                level.setBlock(entry.getKey(), entry.getValue(), 3);
            }
            replacedBlocks.clear();
        }
    }

    private static final Map<UUID, ActiveGate> ACTIVE_GATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SANCTUARY_GATE_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<UUID, ReturnPoint> PLAYER_RETURN_POINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveCapsule> ACTIVE_CAPSULES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TELEPORT_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_INTERACT_MS = new ConcurrentHashMap<>();

    // =========================================================================
    // BẢNG MÀU & TÊN CHỦ ĐỀ CHO 10 KÊNH THẦN MÔN (0 -> 9)
    // =========================================================================
    public static int getChannelPrimaryColor(int channel) {
        return switch (Math.clamp(channel, 0, 9)) {
            case 0 -> 0x00E5FF; // Lam Ngọc (Biệt Thự Gỗ / Quay Về)
            case 1 -> 0xFF1744; // Đỏ Hồng Ngọc (Crimson Ruby)
            case 2 -> 0xFF9100; // Cam Hổ Phách (Solar Amber)
            case 3 -> 0xFFD600; // Vàng Hoàng Kim (Celestial Gold)
            case 4 -> 0x00E676; // Lục Bảo (Emerald Jade)
            case 5 -> 0x2979FF; // Xanh Đại Dương (Royal Sapphire)
            case 6 -> 0xAA00FF; // Tím Thạch Anh (Mystic Amethyst)
            case 7 -> 0xFF4081; // Hồng Anh Đào (Sakura Pink)
            case 8 -> 0xE0F7FA; // Trắng Bạch Kim (Starlight Platinum)
            case 9 -> 0xFFAB00; // Hắc Kim Tinh Vân (Obsidian Gold)
            default -> 0x00E5FF;
        };
    }

    public static int getChannelSecondaryColor(int channel) {
        return switch (Math.clamp(channel, 0, 9)) {
            case 0 -> 0xFFD700;
            case 1 -> 0xFF8A80;
            case 2 -> 0xFFE57F;
            case 3 -> 0xFFFF8D;
            case 4 -> 0xB9F6CA;
            case 5 -> 0x82B1FF;
            case 6 -> 0xEA80FC;
            case 7 -> 0xFF80AB;
            case 8 -> 0x80D8FF;
            case 9 -> 0x7C4DFF;
            default -> 0xFFD700;
        };
    }

    public static int getChannelNebulaColor(int channel) {
        return switch (Math.clamp(channel, 0, 9)) {
            case 0 -> 0x102040;
            case 1 -> 0x3B000E;
            case 2 -> 0x3E1C00;
            case 3 -> 0x382E00;
            case 4 -> 0x002E18;
            case 5 -> 0x05163B;
            case 6 -> 0x260040;
            case 7 -> 0x3B0822;
            case 8 -> 0x1C2E36;
            case 9 -> 0x120824;
            default -> 0x102040;
        };
    }

    public static String getChannelColorCode(int channel) {
        return switch (Math.clamp(channel, 0, 9)) {
            case 0 -> "§b";
            case 1 -> "§c";
            case 2 -> "§6";
            case 3 -> "§e";
            case 4 -> "§a";
            case 5 -> "§9";
            case 6 -> "§5";
            case 7 -> "§d";
            case 8 -> "§f";
            case 9 -> "§6";
            default -> "§b";
        };
    }

    public static String getChannelGemName(int channel) {
        return switch (Math.clamp(channel, 0, 9)) {
            case 0 -> "Lam Ngọc";
            case 1 -> "Hồng Ngọc";
            case 2 -> "Hổ Phách";
            case 3 -> "Hoàng Kim";
            case 4 -> "Lục Bảo";
            case 5 -> "Lam Bảo";
            case 6 -> "Thạch Anh Tím";
            case 7 -> "Anh Đào";
            case 8 -> "Bạch Kim";
            case 9 -> "Hắc Kim";
            default -> "Lam Ngọc";
        };
    }

    // =========================================================================
    // QUẢN LÝ LƯU TRỮ WAYPOINTS (1 -> 9) TRONG DỮ LIỆU NHÂN VẬT
    // =========================================================================
    public static List<GateWaypoint> getWaypoints(ServerPlayer player) {
        List<GateWaypoint> list = new ArrayList<>();
        CompoundTag root = EntityDataHelper.getCustomData(player);
        if (!root.contains("TensuraGateWaypoints")) return list;

        CompoundTag wpRoot = root.getCompound("TensuraGateWaypoints");
        for (int slot = 1; slot <= MAX_WAYPOINT_SLOTS; slot++) {
            String key = "Slot" + slot;
            if (wpRoot.contains(key)) {
                CompoundTag tag = wpRoot.getCompound(key);
                if (tag.getBoolean("Active")) {
                    list.add(new GateWaypoint(
                            slot,
                            tag.getString("Name"),
                            tag.getString("Dimension"),
                            tag.getDouble("X"),
                            tag.getDouble("Y"),
                            tag.getDouble("Z"),
                            tag.getFloat("Yaw"),
                            tag.getFloat("Pitch")
                    ));
                }
            }
        }
        return list;
    }

    public static GateWaypoint getWaypoint(ServerPlayer player, int slot) {
        if (slot < 1 || slot > MAX_WAYPOINT_SLOTS) return null;
        CompoundTag root = EntityDataHelper.getCustomData(player);
        if (!root.contains("TensuraGateWaypoints")) return null;
        CompoundTag wpRoot = root.getCompound("TensuraGateWaypoints");
        String key = "Slot" + slot;
        if (!wpRoot.contains(key)) return null;
        CompoundTag tag = wpRoot.getCompound(key);
        if (!tag.getBoolean("Active")) return null;
        return new GateWaypoint(
                slot,
                tag.getString("Name"),
                tag.getString("Dimension"),
                tag.getDouble("X"),
                tag.getDouble("Y"),
                tag.getDouble("Z"),
                tag.getFloat("Yaw"),
                tag.getFloat("Pitch")
        );
    }

    public static void saveWaypointAtCurrentPos(ServerPlayer player, int slot, String customName) {
        if (slot < 1 || slot > MAX_WAYPOINT_SLOTS) return;
        if (isInsideSanctuary(player.level(), player.position())) {
            player.displayClientMessage(
                    Component.literal("§e[LA BÀN THẦN MÔN] §fKhông cần đánh dấu bên trong Biệt Thự Gỗ (Cổng số 0 luôn dẫn về đây)! Hãy ra thế giới bên ngoài để đánh dấu tọa độ."),
                    true
            );
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
            return;
        }

        String finalName = customName != null ? customName.trim() : "";
        if (finalName.isEmpty()) {
            finalName = "Cổng #" + slot + " (" + getPrettyBiomeOrDimName(player) + ")";
        } else if (finalName.length() > 36) {
            finalName = finalName.substring(0, 36);
        }

        CompoundTag root = EntityDataHelper.getCustomData(player);
        CompoundTag wpRoot = root.contains("TensuraGateWaypoints") ? root.getCompound("TensuraGateWaypoints") : new CompoundTag();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", true);
        tag.putString("Name", finalName);
        tag.putString("Dimension", player.level().dimension().location().toString());
        tag.putDouble("X", player.getX());
        tag.putDouble("Y", player.getY());
        tag.putDouble("Z", player.getZ());
        tag.putFloat("Yaw", player.getYRot());
        tag.putFloat("Pitch", player.getXRot());

        wpRoot.put("Slot" + slot, tag);
        root.put("TensuraGateWaypoints", wpRoot);

        // Chuyển ngay cổng đang mở (nếu có) sang kênh vừa đánh dấu
        switchPlayerActiveGateChannel(player, slot);

        ServerLevel sl = (ServerLevel) player.level();
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 2.0F, 1.2F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.4F);
        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 25, 0.5, 0.8, 0.5, 0.08);

        String cc = getChannelColorCode(slot);
        player.displayClientMessage(
                Component.literal(cc + "§l[ĐÃ LƯU CỔNG SỐ " + slot + "] §f" + finalName + String.format(" §7(%d, %d, %d)", player.getBlockX(), player.getBlockY(), player.getBlockZ())),
                true
        );
    }

    public static void deleteWaypoint(ServerPlayer player, int slot) {
        if (slot < 1 || slot > MAX_WAYPOINT_SLOTS) return;
        CompoundTag root = EntityDataHelper.getCustomData(player);
        if (!root.contains("TensuraGateWaypoints")) return;
        CompoundTag wpRoot = root.getCompound("TensuraGateWaypoints");
        wpRoot.remove("Slot" + slot);
        root.put("TensuraGateWaypoints", wpRoot);

        // Nếu cổng đang trỏ tới kênh vừa xóa thì trả về kênh 0
        ActiveGate gate = ACTIVE_GATES.get(player.getUUID());
        if (gate != null && gate.channel == slot) {
            switchPlayerActiveGateChannel(player, 0);
        }
        if (SANCTUARY_GATE_CHANNELS.getOrDefault(player.getUUID(), 0) == slot) {
            SANCTUARY_GATE_CHANNELS.put(player.getUUID(), 0);
            if (isInsideSanctuary(player.level(), player.position())) {
                syncSanctuaryCourtyardGate((ServerLevel) player.level(), player, true);
            }
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2F, 1.5F);
        player.displayClientMessage(Component.literal("§7[LA BÀN THẦN MÔN] Đã xóa mốc tọa độ ở Cổng số " + slot + "."), true);
    }

    private static String getPrettyBiomeOrDimName(ServerPlayer player) {
        Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
        String biomePath = biomeHolder.unwrapKey().map(k -> k.location().getPath()).orElse("unknown");
        String[] parts = biomePath.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : "Thế Giới";
    }

    // =========================================================================
    // KÍCH HOẠT KỸ NĂNG & XOAY LA BÀN THẦN MÔN (HOWL'S PORTAL DIAL)
    // =========================================================================
    public static void cast(ServerLevel level, ServerPlayer player) {
        boolean insideSanctuary = isInsideSanctuary(level, player.position());
        if (insideSanctuary) {
            openGateDialGui(player);
            return;
        }

        ActiveGate existing = ACTIVE_GATES.get(player.getUUID());
        boolean nearExistingGate = existing != null && existing.level == level && existing.pos.distanceTo(player.position()) <= 16.0;

        if (player.isShiftKeyDown() || player.isCrouching()) {
            // Shift + Skill: Mở ngay Cổng (nếu chưa có) và bật Bảng Chọn La Bàn Cổng 0-9!
            if (!nearExistingGate) {
                spawnOuterGate(level, player, 0);
            }
            openGateDialGui(player);
            return;
        }

        if (nearExistingGate) {
            closeGate(level, player);
            player.displayClientMessage(
                    Component.literal("§b§l[DỊ GIỚI THẦN MÔN] §fĐã thu hồi Cánh Cửa Không Gian!"),
                    true
            );
            return;
        }

        spawnOuterGate(level, player, 0);
    }

    public static void spawnOuterGate(ServerLevel level, ServerPlayer player, int initialChannel) {
        ActiveGate old = ACTIVE_GATES.remove(player.getUUID());
        if (old != null) {
            ModMessages.sendToPlayers(old.level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, player.getUUID(), old.pos, Vec3.ZERO, 0, ClientboundCinematicSkillPacket.ACTION_STOP, 0f
            ));
        }

        Vec3 look = player.getLookAngle();
        Vec3 lookHoriz = new Vec3(look.x, 0, look.z).normalize();
        if (lookHoriz.lengthSqr() < 1e-4) lookHoriz = new Vec3(0, 0, 1);
        Vec3 gatePos = player.position().add(lookHoriz.scale(3.0));

        int ch = Math.clamp(initialChannel, 0, MAX_WAYPOINT_SLOTS);
        ActiveGate gate = new ActiveGate(level, player.getUUID(), gatePos, lookHoriz, ch);
        ACTIVE_GATES.put(player.getUUID(), gate);

        broadcastGateVisual(level, player.getUUID(), gate.pos, gate.facing, ch, false, 72000);

        level.playSound(null, gatePos.x, gatePos.y, gatePos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.2F, 1.2F);
        level.playSound(null, gatePos.x, gatePos.y, gatePos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.2F, 1.0F);
        level.sendParticles(ParticleTypes.PORTAL, gatePos.x, gatePos.y + 2.2, gatePos.z, 55, 1.0, 1.6, 1.0, 0.4);
        level.sendParticles(ParticleTypes.END_ROD, gatePos.x, gatePos.y + 2.2, gatePos.z, 25, 0.8, 1.4, 0.8, 0.05);

        player.displayClientMessage(
                Component.literal(formatChannelStatusBanner(player, ch, false) + " §7(Chuột phải vào cổng: Đổi số | Shift+Chuột phải: Mở La Bàn)"),
                true
        );
    }

    private static void broadcastGateVisual(ServerLevel level, UUID casterUuid, Vec3 pos, Vec3 facing, int channel, boolean isSwitchOnly, int ticks) {
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY)) {
            float encodedPower = (isSwitchOnly ? 100.0f : 0.0f) + Math.clamp(channel, 0, 9);
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, casterUuid, pos, facing, ticks, ClientboundCinematicSkillPacket.ACTION_START, encodedPower
            ));
        }
    }

    public static void openGateDialGui(ServerPlayer player) {
        boolean inside = isInsideSanctuary(player.level(), player.position());
        int currentChannel;
        if (inside) {
            currentChannel = SANCTUARY_GATE_CHANNELS.getOrDefault(player.getUUID(), 0);
        } else {
            ActiveGate gate = ACTIVE_GATES.get(player.getUUID());
            currentChannel = gate != null ? gate.channel : 0;
        }
        boolean hasCapsule = ACTIVE_CAPSULES.containsKey(player.getUUID());
        List<GateWaypoint> waypoints = getWaypoints(player);
        ModMessages.sendToPlayer(new ClientboundOpenGateDialPacket(inside, currentChannel, hasCapsule, waypoints), player);
    }

    /**
     * Khi người chơi nhìn vào Cánh Cửa 3D và nhấn Chuột Phải:
     * - Không giữ Shift: Xoay nhanh kênh La Bàn kiểu Howl's Moving Castle (0 -> các cổng đã lưu -> 0).
     *   Nếu chưa lưu cổng nào (1..9 đều trống) thì mở luôn Bảng La Bàn để người chơi đánh dấu điểm đầu tiên!
     * - Giữ Shift: Mở Bảng La Bàn Thần Môn (0 -> 9).
     */
    public static boolean tryInteractWithGateBlockClick(ServerPlayer player, BlockPos clickedPos, boolean shiftDown) {
        if (player == null || clickedPos == null) return false;
        if (player.level().getBlockEntity(clickedPos) instanceof Container) return false;

        Vec3 hitCenter = Vec3.atCenterOf(clickedPos);
        if (isInsideSanctuary(player.level(), player.position())) {
            Vec3 courtyardGatePos = getSanctuaryGatePos(getSanctuaryCenter(player.getUUID()));
            if (Math.hypot(hitCenter.x - courtyardGatePos.x, hitCenter.z - courtyardGatePos.z) <= 3.5
                    && Math.abs(hitCenter.y - courtyardGatePos.y) <= 5.5) {
                return tryInteractWithGate(player, shiftDown);
            }
            return false;
        }

        ActiveGate gate = ACTIVE_GATES.get(player.getUUID());
        if (gate != null && gate.level == player.level()) {
            if (Math.hypot(hitCenter.x - gate.pos.x, hitCenter.z - gate.pos.z) <= 3.5
                    && Math.abs(hitCenter.y - gate.pos.y) <= 5.5) {
                return tryInteractWithGate(player, shiftDown);
            }
        }
        return false;
    }

    public static boolean tryInteractWithGate(ServerPlayer player, boolean shiftDown) {
        long now = System.currentTimeMillis();
        Long last = LAST_INTERACT_MS.get(player.getUUID());
        if (last != null && now - last < 160) {
            return true;
        }

        boolean inside = isInsideSanctuary(player.level(), player.position());
        if (inside) {
            BlockPos center = getSanctuaryCenter(player.getUUID());
            Vec3 courtyardGatePos = getSanctuaryGatePos(center);
            if (!isLookingNearGate(player, courtyardGatePos, 9.0)) {
                return false;
            }
            LAST_INTERACT_MS.put(player.getUUID(), now);
            if (shiftDown) {
                openGateDialGui(player);
            } else {
                cycleGateChannel(player, true);
            }
            return true;
        }

        // Ngoài thế giới thực: tìm cổng gần nhất mà người chơi đang nhìn vào
        ActiveGate gate = ACTIVE_GATES.get(player.getUUID());
        if (gate == null || gate.level != player.level() || !isLookingNearGate(player, gate.pos, 9.0)) {
            return false;
        }

        LAST_INTERACT_MS.put(player.getUUID(), now);
        if (shiftDown) {
            openGateDialGui(player);
        } else {
            cycleGateChannel(player, false);
        }
        return true;
    }

    private static boolean isLookingNearGate(ServerPlayer player, Vec3 gateBasePos, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 gateCenter = gateBasePos.add(0, 2.3, 0);
        double dist = eye.distanceTo(gateCenter);
        if (dist > maxDist) return false;
        if (dist < 1.8) return true;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 toGate = gateCenter.subtract(eye);
        double proj = toGate.dot(look);
        if (proj < 0) return false;

        Vec3 closestPoint = eye.add(look.scale(proj));
        double horizErr = Math.hypot(closestPoint.x - gateCenter.x, closestPoint.z - gateCenter.z);
        double vertErr = Math.abs(closestPoint.y - gateCenter.y);
        return horizErr <= 2.3 && vertErr <= 2.9;
    }

    public static void cycleGateChannel(ServerPlayer player, boolean insideSanctuary) {
        List<GateWaypoint> saved = getWaypoints(player);
        if (saved.isEmpty()) {
            // Chưa có cổng 1..9 nào được lưu -> Mở luôn GUI để người chơi lưu cực tiện!
            openGateDialGui(player);
            player.displayClientMessage(
                    Component.literal("§e✦ [LA BÀN THẦN MÔN] §fBạn chưa đánh dấu Cổng 1–9 nào! Hãy click chọn 1 ô số để lưu vị trí hiện tại."),
                    true
            );
            return;
        }

        List<Integer> availableChannels = new ArrayList<>();
        availableChannels.add(0);
        for (GateWaypoint wp : saved) {
            availableChannels.add(wp.slot());
        }

        int current = insideSanctuary
                ? SANCTUARY_GATE_CHANNELS.getOrDefault(player.getUUID(), 0)
                : (ACTIVE_GATES.containsKey(player.getUUID()) ? ACTIVE_GATES.get(player.getUUID()).channel : 0);

        int idx = availableChannels.indexOf(current);
        int nextChannel = availableChannels.get((idx + 1) % availableChannels.size());
        switchPlayerActiveGateChannel(player, nextChannel);
    }

    public static void switchPlayerActiveGateChannel(ServerPlayer player, int newChannel) {
        int ch = Math.clamp(newChannel, 0, MAX_WAYPOINT_SLOTS);
        ServerLevel level = (ServerLevel) player.level();

        if (isInsideSanctuary(level, player.position())) {
            SANCTUARY_GATE_CHANNELS.put(player.getUUID(), ch);
            syncSanctuaryCourtyardGate(level, player, true);
            Vec3 gPos = getSanctuaryGatePos(getSanctuaryCenter(player.getUUID()));
            playDialSwitchEffects(level, gPos);
            player.displayClientMessage(Component.literal(formatChannelStatusBanner(player, ch, true)), true);
            return;
        }

        ActiveGate gate = ACTIVE_GATES.get(player.getUUID());
        if (gate == null || gate.level != level || gate.pos.distanceTo(player.position()) > 20.0) {
            // Nếu chưa có cổng gần đó thì mở luôn cổng với kênh mới!
            spawnOuterGate(level, player, ch);
            return;
        }

        gate.channel = ch;
        broadcastGateVisual(level, player.getUUID(), gate.pos, gate.facing, ch, true, 72000);
        playDialSwitchEffects(level, gate.pos);
        player.displayClientMessage(Component.literal(formatChannelStatusBanner(player, ch, false)), true);
    }

    private static void playDialSwitchEffects(ServerLevel level, Vec3 gatePos) {
        level.playSound(null, gatePos.x, gatePos.y + 2.0, gatePos.z, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 2.0F, 1.35F);
        level.playSound(null, gatePos.x, gatePos.y + 2.0, gatePos.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.8F, 1.25F);
        level.playSound(null, gatePos.x, gatePos.y + 2.0, gatePos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.2F, 1.5F);
        level.sendParticles(ParticleTypes.ENCHANT, gatePos.x, gatePos.y + 2.5, gatePos.z, 35, 0.9, 1.5, 0.9, 0.6);
        level.sendParticles(ParticleTypes.END_ROD, gatePos.x, gatePos.y + 2.5, gatePos.z, 14, 0.7, 1.2, 0.7, 0.06);
    }

    private static String formatChannelStatusBanner(ServerPlayer player, int channel, boolean insideSanctuary) {
        String cc = getChannelColorCode(channel);
        if (channel == 0) {
            if (insideSanctuary) {
                return "§b§l✦ [CỔNG SỐ 0 • LAM NGỌC] §f↩ Quay Lại Thế Giới Thực (Chỗ Vừa Vào)";
            }
            return "§b§l✦ [CỔNG SỐ 0 • LAM NGỌC] §e🏡 Đảo Nổi Biệt Thự Gỗ Sân Vườn";
        }
        GateWaypoint wp = getWaypoint(player, channel);
        if (wp == null) {
            return cc + "§l✦ [CỔNG SỐ " + channel + " • " + getChannelGemName(channel).toUpperCase() + "] §7(Chưa đánh dấu tọa độ)";
        }
        return cc + "§l✦ [CỔNG SỐ " + channel + " • " + getChannelGemName(channel).toUpperCase() + "] §f"
                + wp.name() + String.format(" §7(%d, %d, %d)", Math.round(wp.x()), Math.round(wp.y()), Math.round(wp.z()));
    }

    public static void handleDialAction(ServerPlayer player, int action, int slot, String customName) {
        ServerLevel level = (ServerLevel) player.level();
        switch (action) {
            case 0 -> switchPlayerActiveGateChannel(player, slot); // SELECT_CHANNEL
            case 1 -> saveWaypointAtCurrentPos(player, slot, customName); // SAVE_WAYPOINT
            case 2 -> deleteWaypoint(player, slot); // DELETE_WAYPOINT
            case 3 -> toggleCapsuleShelter(level, player); // TOGGLE_CAPSULE
            case 4 -> { // CLOSE_GATE
                closeGate(level, player);
                player.displayClientMessage(Component.literal("§b§l[DỊ GIỚI THẦN MÔN] §fĐã đóng Cánh Cửa Không Gian!"), true);
            }
        }
    }

    public static void closeGate(ServerLevel level, ServerPlayer player) {
        ActiveGate gate = ACTIVE_GATES.remove(player.getUUID());
        if (gate != null) {
            ModMessages.sendToPlayers(gate.level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY, player.getUUID(), gate.pos, Vec3.ZERO, 0, ClientboundCinematicSkillPacket.ACTION_STOP, 0f
            ));
            gate.level.playSound(null, gate.pos.x, gate.pos.y, gate.pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            gate.level.sendParticles(ParticleTypes.POOF, gate.pos.x, gate.pos.y + 2.0, gate.pos.z, 30, 0.8, 1.2, 0.8, 0.1);
        }
    }

    public static boolean dispelByPunch(ServerPlayer player, BlockPos pos) {
        if (isInsideSanctuary(player.level(), player.position())) return false;
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
        if (isInsideSanctuary(player.level(), player.position())) return false;
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

    // =========================================================================
    // DỊCH CHUYỂN XUYÊN KHÔNG GIAN & VÀO/RA ĐẢO NỔI BIỆT THỰ GỖ
    // =========================================================================
    public static BlockPos getSanctuaryCenter(UUID playerUuid) {
        int slot = Math.abs(playerUuid.hashCode() % 10000);
        int baseX = 200000 + slot * 64;
        int baseY = 240;
        int baseZ = 200000 + (slot / 100) * 64;
        return new BlockPos(baseX, baseY, baseZ);
    }

    public static Vec3 getSanctuaryGatePos(BlockPos center) {
        return new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() - 12.0);
    }

    public static boolean isInsideSanctuary(Level level, Vec3 pos) {
        if (level == null || pos == null || level.getServer() == null || level != level.getServer().overworld()) return false;
        if (pos.y < 228 || pos.y > 275) return false;
        return pos.x >= 195000 && pos.z >= 195000;
    }

    private static void syncSanctuaryCourtyardGate(ServerLevel overworld, ServerPlayer player, boolean switchOnly) {
        BlockPos center = getSanctuaryCenter(player.getUUID());
        Vec3 gPos = getSanctuaryGatePos(center);
        int ch = SANCTUARY_GATE_CHANNELS.getOrDefault(player.getUUID(), 0);
        broadcastGateVisual(overworld, player.getUUID(), gPos, new Vec3(0, 0, 1), ch, switchOnly, 72000);
    }

    public static void handleStepIntoOuterGate(ServerPlayer player, ActiveGate gate) {
        ServerPlayer gateOwner = player.server.getPlayerList().getPlayer(gate.casterUuid);
        ServerPlayer waypointHolder = gateOwner != null ? gateOwner : player;

        if (gate.channel == 0) {
            enterSanctuary(player, waypointHolder.getUUID());
            return;
        }

        GateWaypoint wp = getWaypoint(waypointHolder, gate.channel);
        if (wp == null) {
            player.displayClientMessage(
                    Component.literal("§e[CỔNG SỐ " + gate.channel + "] §fCổng này chưa lưu vị trí! Hãy nhấn §bShift + Chuột Phải §fvào cổng để lưu điểm đến."),
                    true
            );
            return;
        }

        teleportToWaypoint(player, wp);
    }

    public static void teleportToWaypoint(ServerPlayer player, GateWaypoint wp) {
        ResourceLocation dimLoc = ResourceLocation.tryParse(wp.dimension());
        ResourceKey<Level> dimKey = dimLoc != null ? ResourceKey.create(Registries.DIMENSION, dimLoc) : Level.OVERWORLD;
        ServerLevel targetLevel = player.server.getLevel(dimKey);
        if (targetLevel == null) {
            targetLevel = player.server.overworld();
        }

        Vec3 dest = new Vec3(wp.x(), wp.y(), wp.z());
        List<Entity> pets = getNearbyPets(player, 12.0);

        player.teleportTo(targetLevel, dest.x, dest.y, dest.z, wp.yaw(), wp.pitch());
        teleportPetsTo(pets, targetLevel, dest);

        // Mở hiệu ứng Cổng Chào 3D phía sau lưng người chơi tại điểm đến trong 4 giây (80 ticks) rồi tự khép!
        Vec3 arrivalFacing = Vec3.directionFromRotation(0.0F, wp.yaw()).normalize();
        Vec3 arrivalGatePos = dest.subtract(arrivalFacing.scale(1.8));
        UUID arrivalVisualId = UUID.nameUUIDFromBytes(("arrival_gate_" + player.getUUID()).getBytes());
        broadcastGateVisual(targetLevel, arrivalVisualId, arrivalGatePos, arrivalFacing, wp.slot(), false, 80);

        targetLevel.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.4F, 1.25F);
        targetLevel.playSound(null, dest.x, dest.y, dest.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.1F);
        targetLevel.sendParticles(ParticleTypes.END_ROD, dest.x, dest.y + 1.0, dest.z, 35, 0.6, 0.9, 0.6, 0.06);

        String cc = getChannelColorCode(wp.slot());
        player.displayClientMessage(
                Component.literal(cc + "§l⚡ [DỊCH CHUYỂN CỔNG SỐ " + wp.slot() + "] §fĐã tới §e" + wp.name() + "§f an toàn cùng thú cưng!"),
                true
        );
    }

    public static void enterSanctuary(ServerPlayer player, UUID sanctuaryOwnerUuid) {
        ServerLevel overworld = player.server.overworld();
        BlockPos center = getSanctuaryCenter(sanctuaryOwnerUuid);

        // Xây dựng Đảo Nổi Biệt Thự Gỗ Sân Vườn (tự động nâng cấp & bảo toàn đồ đạc nếu đang ở bản cũ)
        buildSanctuaryIfAbsent(overworld, player, center);

        // Lưu điểm trở về ngoài đời thực (nếu người chơi bước vào từ thế giới ngoài)
        if (!isInsideSanctuary(player.level(), player.position())) {
            PLAYER_RETURN_POINTS.put(player.getUUID(), new ReturnPoint(
                    player.level().dimension(), player.position(), player.getYRot(), player.getXRot()
            ));
        }

        // Mặc định cổng sân vườn đặt về kênh 0 (Quay lại chỗ vừa vào)
        SANCTUARY_GATE_CHANNELS.put(player.getUUID(), 0);

        // Điểm xuất hiện: Trên lối đi đá cuội rêu giữa Vườn Anh Đào & Suối Nước Nóng, nhìn thẳng vào Biệt Thự Gỗ!
        Vec3 spawnPos = new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() - 8.5);
        List<Entity> pets = getNearbyPets(player, 12.0);

        player.teleportTo(overworld, spawnPos.x, spawnPos.y, spawnPos.z, 0.0F, -4.0F);
        teleportPetsTo(pets, overworld, spawnPos);

        // Đồng bộ hiển thị Cánh Cổng 3D ở hiên sân vườn
        syncSanctuaryCourtyardGate(overworld, player, false);

        overworld.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.4F, 1.2F);
        overworld.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.0F);
        overworld.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y + 1.0, spawnPos.z, 30, 0.5, 0.8, 0.5, 0.05);

        player.displayClientMessage(
                Component.literal("§6§l🏡 [BIỆT THỰ GỖ SÂN VƯỜN] §eChào mừng về nhà! Suối nước nóng bên phải, Vườn Anh Đào bên trái."),
                true
        );
    }

    public static void handleStepIntoSanctuaryGate(ServerPlayer player) {
        int ch = SANCTUARY_GATE_CHANNELS.getOrDefault(player.getUUID(), 0);
        if (ch >= 1 && ch <= MAX_WAYPOINT_SLOTS) {
            GateWaypoint wp = getWaypoint(player, ch);
            if (wp != null) {
                teleportToWaypoint(player, wp);
                return;
            }
        }
        exitSanctuary(player);
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
            returnPos = gate != null ? gate.pos.add(0, 0.1, 0) : new Vec3(0.5, 80.0, 0.5);
        }

        List<Entity> pets = getNearbyPets(player, 20.0);

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

    // =========================================================================
    // KIẾN TRÚC ĐẢO NỔI BIỆT THỰ GỖ SÂN VƯỜN ANH ĐÀO (33x33 FLOATING VILLA)
    // =========================================================================
    public static void buildSanctuaryIfAbsent(ServerLevel overworld, ServerPlayer player, BlockPos center) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        int builtVer = data.getInt("TensuraSanctuaryVersion");
        if (builtVer >= SANCTUARY_VERSION && !overworld.getBlockState(center).isAir()) {
            return;
        }

        // 0. CỨU HỘ 100% VẬT PHẨM TRONG RƯƠNG/THÙNG TỪ BẢN NHÀ ĐÁ CŨ (NẾU CÓ)
        List<ItemStack> rescuedItems = new ArrayList<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                for (int dy = -2; dy <= 10; dy++) {
                    mpos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = overworld.getBlockEntity(mpos);
                    if (be instanceof Container container) {
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack st = container.getItem(slot);
                            if (!st.isEmpty()) {
                                rescuedItems.add(st.copy());
                            }
                        }
                        container.clearContent();
                    }
                }
            }
        }

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        // 1. DỌN SẠCH KHÔNG GIAN 37x37x24 QUANH ĐẢO
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                for (int dy = -6; dy <= 16; dy++) {
                    mpos.set(cx + dx, cy + dy, cz + dz);
                    if (!overworld.getBlockState(mpos).isAir()) {
                        overworld.setBlock(mpos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 2. KIẾN TẠO ĐẢO NỔI SINH THÁI BO TRÒN TỰ NHIÊN (33x33) & TƯỜNG VÔ HÌNH CHỐNG RƠI
        for (int dx = -17; dx <= 17; dx++) {
            for (int dz = -17; dz <= 17; dz++) {
                double metric = Math.pow(Math.abs(dx) / 16.3, 2.6) + Math.pow(Math.abs(dz) / 16.3, 2.6);
                if (metric <= 1.0) {
                    int depth = Math.max(1, (int) Math.round((1.0 - metric) * 4.5));
                    // Mặt cỏ xanh tự nhiên ở dy = 0
                    BlockState topSurface = ((dx * 7 + dz * 13) % 11 == 0)
                            ? Blocks.MOSS_BLOCK.defaultBlockState()
                            : Blocks.GRASS_BLOCK.defaultBlockState();
                    overworld.setBlock(new BlockPos(cx + dx, cy, cz + dz), topSurface, 2);

                    for (int d = 1; d <= depth; d++) {
                        BlockState under = (d == depth)
                                ? (((dx + dz) % 3 == 0) ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState())
                                : Blocks.DIRT.defaultBlockState();
                        overworld.setBlock(new BlockPos(cx + dx, cy - d, cz + dz), under, 2);
                    }
                } else if (metric <= 1.22) {
                    // Tường rào vô hình chống rơi khỏi đảo cho người chơi và thú cưng, vẫn thoáng bầu trời 100%!
                    for (int by = 0; by <= 10; by++) {
                        overworld.setBlock(new BlockPos(cx + dx, cy + by, cz + dz), Blocks.BARRIER.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 3. SÂN VƯỜN PHÍA BẮC (-Z): LỐI ĐI ĐÁ CUỘI RÊU & KHUNG CỔNG GỖ TORII (dz = -13..-2)
        for (int dz = -13; dz <= -2; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockState pathBlock = ((dx + dz) % 4 == 0)
                        ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                        : (((dx * dz) % 3 == 0) ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                overworld.setBlock(new BlockPos(cx + dx, cy, cz + dz), pathBlock, 2);
            }
        }

        // Bệ đá và Cổng Gỗ Torii cho Cánh Cửa Thần Môn Sân Vườn tại dz = -12
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -13; dz <= -11; dz++) {
                overworld.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
            }
        }
        for (int dy = 1; dy <= 5; dy++) {
            overworld.setBlock(new BlockPos(cx - 2, cy + dy, cz - 12), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 2);
            overworld.setBlock(new BlockPos(cx + 2, cy + dy, cz - 12), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 2);
        }
        for (int dx = -3; dx <= 3; dx++) {
            overworld.setBlock(new BlockPos(cx + dx, cy + 6, cz - 12), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
            overworld.setBlock(new BlockPos(cx + dx, cy + 7, cz - 12), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }
        overworld.setBlock(new BlockPos(cx - 3, cy + 5, cz - 12), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        overworld.setBlock(new BlockPos(cx + 3, cy + 5, cz - 12), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);

        // 4. VƯỜN HOA ANH ĐÀO & BÀN TRÀ NGOÀI TRỜI (Góc Tây-Bắc: dx = -13..-5, dz = -11..-4)
        buildCherryBlossomGarden(overworld, cx, cy, cz);

        // 5. SUỐI NƯỚC NÓNG LỘ THIÊN VIỀN GỖ THÔNG (Góc Đông-Bắc: dx = +5..+12, dz = -10..-4)
        buildOutdoorWoodenOnsen(overworld, cx, cy, cz);

        // 6. BIỆT THỰ GỖ THÔNG & GỖ SỒI RỘNG RÃI (23x15: dx = -11..+11, dz = -1..+13)
        buildCozyWoodenVilla(overworld, cx, cy, cz, rescuedItems);

        // 7. HÀNG RÀO GỖ THÔNG & ĐÈN LỒNG QUANH MÉP ĐẢO (Đặt với flag 3 để tự nối liền mạch)
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                // Không đặt đè lên tường sau của biệt thự nếu sát mép
                if (dx >= -12 && dx <= 12 && dz >= -2 && dz <= 14) continue;
                double metric = Math.pow(Math.abs(dx) / 16.3, 2.6) + Math.pow(Math.abs(dz) / 16.3, 2.6);
                if (metric > 0.83 && metric <= 1.0) {
                    BlockPos fencePos = new BlockPos(cx + dx, cy + 1, cz + dz);
                    if (overworld.getBlockState(fencePos).isAir()) {
                        overworld.setBlock(fencePos, Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
                        if ((Math.abs(dx) + Math.abs(dz)) % 5 == 0) {
                            overworld.setBlock(fencePos.above(), Blocks.LANTERN.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        data.putBoolean("TensuraSanctuaryBuilt", true);
        data.putInt("TensuraSanctuaryVersion", SANCTUARY_VERSION);
    }

    private static void buildCherryBlossomGarden(ServerLevel level, int cx, int cy, int cz) {
        BlockState cherryLog = Blocks.CHERRY_LOG.defaultBlockState();
        BlockState cherryLeaves = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

        // Thân cây Anh Đào cổ thụ tại (-9, -8)
        for (int dy = 1; dy <= 5; dy++) {
            level.setBlock(new BlockPos(cx - 9, cy + dy, cz - 8), cherryLog, 2);
        }
        level.setBlock(new BlockPos(cx - 8, cy + 4, cz - 7), cherryLog, 2);
        level.setBlock(new BlockPos(cx - 10, cy + 5, cz - 9), cherryLog, 2);
        level.setBlock(new BlockPos(cx - 7, cy + 5, cz - 8), cherryLog, 2);

        // Tán hoa Anh Đào hồng rực (tự động thả hạt cánh hoa rơi bay trong gió trên Client!)
        for (int dx = -13; dx <= -5; dx++) {
            for (int dz = -12; dz <= -4; dz++) {
                for (int dy = 4; dy <= 8; dy++) {
                    double dist = Math.pow((dx + 9) / 3.8, 2) + Math.pow((dz + 8) / 3.8, 2) + Math.pow((dy - 6.0) / 2.3, 2);
                    if (dist <= 1.0) {
                        BlockPos lp = new BlockPos(cx + dx, cy + dy, cz + dz);
                        if (level.getBlockState(lp).isAir()) {
                            level.setBlock(lp, cherryLeaves, 2);
                        }
                    }
                }
            }
        }
        // Đèn nấm ẩn trong tán hoa giúp cây lung linh về đêm
        level.setBlock(new BlockPos(cx - 8, cy + 6, cz - 8), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx - 10, cy + 6, cz - 7), Blocks.SHROOMLIGHT.defaultBlockState(), 2);

        // Rải thảm cánh hoa Anh Đào & khóm hoa quanh bãi cỏ sân vườn
        int[][] petalOffsets = {
                {-7, -9}, {-8, -6}, {-10, -6}, {-11, -9}, {-6, -10}, {-9, -11}, {-5, -7}, {-4, -8}, {-3, -6}, {3, -7}, {4, -9}
        };
        for (int[] off : petalOffsets) {
            BlockPos gp = new BlockPos(cx + off[0], cy + 1, cz + off[1]);
            if (level.getBlockState(gp).isAir()) {
                level.setBlock(gp, Blocks.PINK_PETALS.defaultBlockState(), 2);
            }
        }
        level.setBlock(new BlockPos(cx - 11, cy + 1, cz - 7), Blocks.FLOWERING_AZALEA.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx - 4, cy + 1, cz - 10), Blocks.PINK_TULIP.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 3, cy + 1, cz - 10), Blocks.ALLIUM.defaultBlockState(), 2);

        // Bộ ghế gỗ & bàn trà ngắm hoa dưới gốc Anh Đào (-6..-5, -6..-5)
        level.setBlock(new BlockPos(cx - 6, cy + 1, cz - 6), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 3);
        level.setBlock(new BlockPos(cx - 6, cy + 1, cz - 5), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 3);
        level.setBlock(new BlockPos(cx - 5, cy + 1, cz - 6), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        level.setBlock(new BlockPos(cx - 5, cy + 2, cz - 6), Blocks.WHITE_CARPET.defaultBlockState(), 3);
    }

    private static void buildOutdoorWoodenOnsen(ServerLevel level, int cx, int cy, int cz) {
        // Sàn gỗ thông bao quanh hồ suối nước nóng (dx = +5..+11, dz = -10..-4)
        for (int dx = 5; dx <= 11; dx++) {
            for (int dz = -10; dz <= -4; dz++) {
                level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
            }
        }

        // Lòng hồ 5x5 ấm áp (dx = +6..+10, dz = -9..-5) âm xuống mặt sàn gỗ cực tự nhiên
        for (int dx = 6; dx <= 10; dx++) {
            for (int dz = -9; dz <= -5; dz++) {
                level.setBlock(new BlockPos(cx + dx, cy - 2, cz + dz), Blocks.STONE.defaultBlockState(), 2);
                boolean isLight = ((dx + dz) % 2 == 0);
                level.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz),
                        isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.WATER.defaultBlockState(), 2);
            }
        }

        // Đặt 3 bếp lửa trại ẩn dưới đáy hồ (cy - 2) để làn khói hơi nước ấm bốc lên lững lờ trên mặt suối!
        level.setBlock(new BlockPos(cx + 8, cy - 2, cz - 7), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        level.setBlock(new BlockPos(cx + 7, cy - 2, cz - 6), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        level.setBlock(new BlockPos(cx + 9, cy - 2, cz - 8), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);

        // Thành gỗ & Đá cuội tự nhiên + Đèn lồng suối khoáng
        for (int dx = 6; dx <= 11; dx++) {
            level.setBlock(new BlockPos(cx + dx, cy + 1, cz - 10), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }
        for (int dz = -10; dz <= -4; dz++) {
            level.setBlock(new BlockPos(cx + 11, cy + 1, cz + dz), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(cx + 11, cy + 1, cz - 10), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 11, cy + 2, cz - 10), Blocks.LANTERN.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 11, cy + 1, cz - 4), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 11, cy + 2, cz - 4), Blocks.LANTERN.defaultBlockState(), 2);

        // Nhánh Anh Đào nhỏ rủ bóng trên suối nước nóng
        for (int dy = 1; dy <= 4; dy++) {
            level.setBlock(new BlockPos(cx + 12, cy + dy, cz - 7), Blocks.CHERRY_LOG.defaultBlockState(), 2);
        }
        BlockState leaves = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = 10; dx <= 13; dx++) {
            for (int dz = -9; dz <= -5; dz++) {
                level.setBlock(new BlockPos(cx + dx, cy + 5, cz + dz), leaves, 2);
            }
        }
        level.setBlock(new BlockPos(cx + 12, cy + 1, cz - 5), Blocks.FLOWERING_AZALEA.defaultBlockState(), 2);
    }

    private static void buildCozyWoodenVilla(ServerLevel level, int cx, int cy, int cz, List<ItemStack> rescuedItems) {
        // --- A. NỀN NHÀ GỖ SỒI ẤM ÁP & HIÊN TRƯỚC (dx = -11..+11, dz = -1..+13) ---
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -1; dz <= 13; dz++) {
                boolean isOuterEdge = (Math.abs(dx) == 11 || dz == -1 || dz == 13);
                boolean isInnerTrim = (!isOuterEdge && (Math.abs(dx) == 9 || dz == 1 || dz == 11));
                BlockState floorState = isOuterEdge
                        ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                        : (isInnerTrim ? Blocks.BIRCH_PLANKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
                level.setBlock(new BlockPos(cx + dx, cy, cz + dz), floorState, 2);
            }
        }

        // Hiên gỗ có mái che phía trước (dx = -4..+4, dz = -3..-2)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= -2; dz++) {
                level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx + dx, cy + 4, cz + dz), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(new BlockPos(cx - 4, cy + dy, cz - 3), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(cx + 4, cy + dy, cz - 3), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        }
        for (int dx = -3; dx <= -2; dx++) {
            level.setBlock(new BlockPos(cx + dx, cy + 1, cz - 3), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(cx - dx, cy + 1, cz - 3), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(cx - 2, cy + 3, cz - 2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        level.setBlock(new BlockPos(cx + 2, cy + 3, cz - 2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);

        // --- B. TƯỜNG GỖ SỒI & KHUNG TRỤ GỖ VÂN SAM BÓC VỎ (dy = 1..4) ---
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -1; dz <= 13; dz++) {
                boolean onFrontOrBack = (dz == -1 || dz == 13);
                boolean onLeftOrRight = (Math.abs(dx) == 11);
                if (!onFrontOrBack && !onLeftOrRight) continue;

                // Lối vào vòm rộng 3m ở chính giữa mặt tiền (dx = -1..+1, dz = -1, dy = 1..3)
                boolean isMainEntrance = (dz == -1 && Math.abs(dx) <= 1);
                boolean isPillar = (Math.abs(dx) == 11 && (dz == -1 || dz == 6 || dz == 13))
                        || (onFrontOrBack && (Math.abs(dx) == 5 || Math.abs(dx) == 2));

                for (int dy = 1; dy <= 4; dy++) {
                    if (isMainEntrance && dy <= 3) continue;
                    BlockPos wp = new BlockPos(cx + dx, cy + dy, cz + dz);
                    if (isPillar) {
                        level.setBlock(wp, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 2);
                    } else if (dy == 1) {
                        level.setBlock(wp, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                    } else {
                        level.setBlock(wp, Blocks.OAK_PLANKS.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Vòm gỗ trang trí cổng chính
        level.setBlock(new BlockPos(cx - 1, cy + 3, cz - 1),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP), 3);
        level.setBlock(new BlockPos(cx + 1, cy + 3, cz - 1),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP), 3);

        // --- C. MÁI CHỮ A GỖ SỒI SẪM & GỖ THÔNG + GIẾNG TRỜI ĐÓN NẮNG (dy = 4..8) ---
        // Tường hồi tam giác 2 bên Đông & Tây (dx = -11, +11)
        for (int side : new int[]{-11, 11}) {
            for (int dz = 0; dz <= 12; dz++) {
                int distFromRidge = Math.abs(dz - 6);
                int maxWallY = Math.min(7, 8 - Math.max(0, distFromRidge - 3));
                for (int dy = 5; dy <= maxWallY; dy++) {
                    BlockState gableState = (dz == 6)
                            ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                            : Blocks.OAK_PLANKS.defaultBlockState();
                    level.setBlock(new BlockPos(cx + side, cy + dy, cz + dz), gableState, 2);
                }
            }
        }

        // Mái dốc (bao gồm diềm mái nhô ra ở dx = -12..+12 và dz = -2, +14)
        for (int dx = -12; dx <= 12; dx++) {
            boolean isBorderTrim = (Math.abs(dx) >= 11);
            // Diềm mái thấp nhất (dz = -2 và dz = +14, dy = 4)
            Block stairsMat = isBorderTrim ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS;
            level.setBlock(new BlockPos(cx + dx, cy + 4, cz - 2),
                    stairsMat.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
            level.setBlock(new BlockPos(cx + dx, cy + 4, cz + 14),
                    stairsMat.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);

            // 4 bậc mái dốc lên đỉnh (d = 0..3 -> dy = 5..8)
            for (int d = 0; d <= 3; d++) {
                int ry = 5 + d;
                int zNorth = -1 + d;
                int zSouth = 13 - d;
                level.setBlock(new BlockPos(cx + dx, cy + ry, cz + zNorth),
                        stairsMat.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
                level.setBlock(new BlockPos(cx + dx, cy + ry, cz + zSouth),
                        stairsMat.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);

                if (Math.abs(dx) <= 10 && d < 3) {
                    // Đỡ mặt dưới mái cho trần gỗ trong nhà liền mạch ấm áp
                    level.setBlock(new BlockPos(cx + dx, cy + ry - 1, cz + zNorth), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(cx + dx, cy + ry - 1, cz + zSouth), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                }
            }

            // Đỉnh vòm mái & Giếng trời pha lê đón nắng tự nhiên (dz = +3..+9, dy = 8)
            for (int dz = 3; dz <= 9; dz++) {
                BlockPos crownPos = new BlockPos(cx + dx, cy + 8, cz + dz);
                if (Math.abs(dx) == 12) {
                    level.setBlock(crownPos, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), 2);
                } else if (Math.abs(dx) <= 4 && dz >= 4 && dz <= 8 && dx != 0 && dz != 6) {
                    level.setBlock(crownPos, Blocks.GLASS.defaultBlockState(), 2);
                } else if (dx == 0 || dz == 6 || Math.abs(dx) == 5) {
                    level.setBlock(crownPos, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 2);
                } else {
                    level.setBlock(crownPos, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                }
            }
        }

        // 3 Đèn chùm gỗ ấm áp thả từ xà ngang giữa nhà (dy = 7 -> 6)
        for (int lx : new int[]{-6, 0, 6}) {
            level.setBlock(new BlockPos(cx + lx, cy + 7, cz + 6), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(cx + lx, cy + 6, cz + 6), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        }

        // --- D. LÒ SƯỞI GẠCH ĐỎ & ỐNG KHÓI ẤM CÚNG (Chính giữa tường Nam: dx = -1..+1, dz = +12..+13) ---
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 4; dy++) {
                level.setBlock(new BlockPos(cx + dx, cy + dy, cz + 13), Blocks.BRICKS.defaultBlockState(), 2);
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(new BlockPos(cx - 1, cy + dy, cz + 12), Blocks.BRICKS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(cx + 1, cy + dy, cz + 12), Blocks.BRICKS.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(cx, cy + 1, cz + 12), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        level.setBlock(new BlockPos(cx, cy + 2, cz + 12), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx, cy + 3, cz + 12), Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 2);
        // Lưới sắt chắn lửa an toàn + kệ lò sưởi decor
        level.setBlock(new BlockPos(cx, cy + 1, cz + 11), Blocks.IRON_BARS.defaultBlockState(), 3);
        level.setBlock(new BlockPos(cx - 1, cy + 4, cz + 12), Blocks.LANTERN.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 1, cy + 4, cz + 12), Blocks.POTTED_CHERRY_SAPLING.defaultBlockState(), 2);
        // Ống khói gạch vươn lên trên mái nhà
        for (int dy = 5; dy <= 9; dy++) {
            level.setBlock(new BlockPos(cx, cy + dy, cz + 13), Blocks.BRICKS.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(cx, cy + 10, cz + 13), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);

        // --- E. CỬA SỔ KÍNH LỚN 4 PHÍA & BỒN HOA BỆ CỬA SỔ ---
        // Mặt tiền (dz = -1) & Mặt hậu (dz = +13): dx = -9..-7 và +7..+9, dy = 2..3
        for (int wx : new int[]{-9, -8, -7, 7, 8, 9}) {
            for (int wy = 2; wy <= 3; wy++) {
                level.setBlock(new BlockPos(cx + wx, cy + wy, cz - 1), Blocks.GLASS_PANE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(cx + wx, cy + wy, cz + 13), Blocks.GLASS_PANE.defaultBlockState(), 3);
            }
            // Bồn hoa trước cửa sổ mặt tiền
            level.setBlock(new BlockPos(cx + wx, cy + 1, cz - 2), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            Block flower = (Math.abs(wx) == 8) ? Blocks.POTTED_CHERRY_SAPLING : Blocks.RED_TULIP;
            level.setBlock(new BlockPos(cx + wx, cy + 2, cz - 2), flower.defaultBlockState(), 2);
        }
        // Tường bên Tây (-11) & Đông (+11): dz = +2..+4 và +8..+10, dy = 2..3 + cửa sổ áp mái dy = 6
        for (int wz : new int[]{2, 3, 4, 8, 9, 10}) {
            for (int wy = 2; wy <= 3; wy++) {
                level.setBlock(new BlockPos(cx - 11, cy + wy, cz + wz), Blocks.GLASS_PANE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(cx + 11, cy + wy, cz + wz), Blocks.GLASS_PANE.defaultBlockState(), 3);
            }
        }
        for (int wz : new int[]{4, 5, 7, 8}) {
            level.setBlock(new BlockPos(cx - 11, cy + 6, cz + wz), Blocks.GLASS_PANE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(cx + 11, cy + 6, cz + wz), Blocks.GLASS_PANE.defaultBlockState(), 3);
        }

        // --- F. NỘI THẤT 4 PHÂN KHU RỘNG RÃI & TIỆN NGHI ---
        // 1. Phòng khách trung tâm: Thảm dệt Bắc Âu + Sofa gỗ hướng nhìn vào lò sưởi ấm
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 4; dz <= 10; dz++) {
                Block carpet = ((Math.abs(dx) + dz) % 2 == 0) ? Blocks.WHITE_CARPET : Blocks.ORANGE_CARPET;
                level.setBlock(new BlockPos(cx + dx, cy + 1, cz + dz), carpet.defaultBlockState(), 2);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(new BlockPos(cx + dx, cy + 1, cz + 7),
                    Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 3);
        }
        level.setBlock(new BlockPos(cx - 3, cy + 1, cz + 9),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 3);
        level.setBlock(new BlockPos(cx + 3, cy + 1, cz + 9),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 3);
        level.setBlock(new BlockPos(cx, cy + 1, cz + 9), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx, cy + 2, cz + 9), Blocks.POTTED_CHERRY_SAPLING.defaultBlockState(), 2);

        // 2. Phòng ngủ Master & Tủ lưu trữ lớn + Đệm cho thú cưng (Cánh Tây-Nam: dx = -10..-5, dz = +6..+12)
        for (int bx : new int[]{-8, -7}) {
            BlockPos foot = new BlockPos(cx + bx, cy + 1, cz + 10);
            BlockPos head = new BlockPos(cx + bx, cy + 1, cz + 11);
            level.setBlock(foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH), 2);
            level.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH), 2);
        }
        level.setBlock(new BlockPos(cx - 9, cy + 1, cz + 11), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        level.setBlock(new BlockPos(cx - 9, cy + 2, cz + 11), Blocks.LANTERN.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx - 6, cy + 1, cz + 11), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        level.setBlock(new BlockPos(cx - 6, cy + 2, cz + 11), Blocks.POTTED_BLUE_ORCHID.defaultBlockState(), 2);
        // Thảm êm cho thú cưng nằm cạnh giường và lò sưởi
        level.setBlock(new BlockPos(cx - 5, cy + 1, cz + 11), Blocks.YELLOW_CARPET.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx - 4, cy + 1, cz + 11), Blocks.CYAN_CARPET.defaultBlockState(), 2);

        // Hệ tủ kho đồ lớn sát tường Tây (dx = -10, dz = +6..+10):
        // Tầng 1: 4 Thùng gỗ (luôn mở được) + Rương Ender | Tầng 2: 2 Rương đôi lớn (trên là không khí nên luôn mở được!)
        List<BlockPos> storagePositions = new ArrayList<>();
        for (int sz = 6; sz <= 9; sz++) {
            BlockPos barrelPos = new BlockPos(cx - 10, cy + 1, cz + sz);
            level.setBlock(barrelPos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), 2);
            storagePositions.add(barrelPos);
        }
        // Rương đôi 1 (dz = 6..7, dy = 2) & Rương đôi 2 (dz = 8..9, dy = 2)
        BlockPos c1L = new BlockPos(cx - 10, cy + 2, cz + 6);
        BlockPos c1R = new BlockPos(cx - 10, cy + 2, cz + 7);
        BlockPos c2L = new BlockPos(cx - 10, cy + 2, cz + 8);
        BlockPos c2R = new BlockPos(cx - 10, cy + 2, cz + 9);
        level.setBlock(c1L, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST).setValue(ChestBlock.TYPE, ChestType.LEFT), 2);
        level.setBlock(c1R, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST).setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
        level.setBlock(c2L, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST).setValue(ChestBlock.TYPE, ChestType.LEFT), 2);
        level.setBlock(c2R, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST).setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
        storagePositions.add(c1L);
        storagePositions.add(c1R);
        storagePositions.add(c2L);
        storagePositions.add(c2R);

        level.setBlock(new BlockPos(cx - 10, cy + 1, cz + 10),
                Blocks.ENDER_CHEST.defaultBlockState().setValue(EnderChestBlock.FACING, Direction.EAST), 2);

        // Chuyển trả toàn bộ vật phẩm cứu hộ từ nhà cũ vào hệ thống rương/thùng mới
        int itemIdx = 0;
        for (BlockPos sp : storagePositions) {
            if (itemIdx >= rescuedItems.size()) break;
            BlockEntity be = level.getBlockEntity(sp);
            if (be instanceof Container cont) {
                for (int s = 0; s < cont.getContainerSize() && itemIdx < rescuedItems.size(); s++) {
                    cont.setItem(s, rescuedItems.get(itemIdx++));
                }
            }
        }

        // 3. Thư phòng Phù Phép Chuẩn Cấp 30 (Cánh Tây-Bắc: Bàn phù phép tại (-8, +1, +3), vòng kệ sách chuẩn khoảng cách 2 ô!)
        level.setBlock(new BlockPos(cx - 8, cy + 1, cz + 3), Blocks.ENCHANTING_TABLE.defaultBlockState(), 2);
        for (int bx = -10; bx <= -6; bx++) {
            for (int bz = 1; bz <= 5; bz++) {
                boolean isOuterRing = (bx == -10 || bz == 1 || bz == 5);
                if (!isOuterRing) continue;
                // Chừa lối đi rộng 3m ở phía Đông (bx == -6, bz = 2..4)
                for (int by = 1; by <= 2; by++) {
                    level.setBlock(new BlockPos(cx + bx, cy + by, cz + bz), Blocks.BOOKSHELF.defaultBlockState(), 2);
                }
                level.setBlock(new BlockPos(cx + bx, cy + 3, cz + bz), Blocks.CHISELED_BOOKSHELF.defaultBlockState(), 2);
            }
        }
        level.setBlock(new BlockPos(cx - 6, cy + 1, cz + 1),
                Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST), 2);
        level.setBlock(new BlockPos(cx - 8, cy + 3, cz + 1), Blocks.LANTERN.defaultBlockState(), 2);

        // 4. Bếp Gỗ & Xưởng Rèn Nghệ Nhân (Cánh Đông: dx = +7..+10, dz = +1..+11)
        // Bếp ăn (dz = +8..+11)
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 11),
                Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.WEST), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 10),
                Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 9),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 8), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 2, cz + 8), Blocks.BREWING_STAND.defaultBlockState(), 2);
        // Bàn ăn nhỏ ấm cúng bên cửa sổ nhìn ra suối nước nóng
        level.setBlock(new BlockPos(cx + 8, cy + 1, cz + 9),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 3);
        level.setBlock(new BlockPos(cx + 8, cy + 1, cz + 11),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(new BlockPos(cx + 8, cy + 1, cz + 10), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        level.setBlock(new BlockPos(cx + 8, cy + 2, cz + 10), Blocks.WHITE_CARPET.defaultBlockState(), 3);

        // Xưởng rèn gọn đẹp sát tường Đông (dz = +1..+6)
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 6), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 5),
                Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.NORTH), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 4), Blocks.SMITHING_TABLE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 3),
                Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlastFurnaceBlock.FACING, Direction.WEST), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 2), Blocks.GRINDSTONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 1, cz + 1),
                Blocks.STONECUTTER.defaultBlockState().setValue(StonecutterBlock.FACING, Direction.WEST), 2);
        level.setBlock(new BlockPos(cx + 9, cy + 1, cz + 1), Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 8, cy + 1, cz + 1), Blocks.LOOM.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 10, cy + 2, cz + 6), Blocks.LANTERN.defaultBlockState(), 2);
    }

    // =========================================================================
    // CAPSULE SHELTER 5x5 DÃ NGOẠI TẠI CHỖ
    // =========================================================================
    public static void toggleCapsuleShelter(ServerLevel level, ServerPlayer player) {
        if (isInsideSanctuary(level, player.position())) {
            player.displayClientMessage(Component.literal("§e[CAPSULE SHELTER] §fBạn đang ở ngay trong Biệt Thự Gỗ Sân Vườn rồi!"), true);
            return;
        }
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

        level.setBlock(center.offset(-1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        level.setBlock(center.offset(0, 1, 1), Blocks.FURNACE.defaultBlockState(), 2);
        level.setBlock(center.offset(1, 1, 1), Blocks.CHEST.defaultBlockState(), 2);
        level.setBlock(center.offset(-1, 1, -1), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(center.offset(-1, 1, 0), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(center.offset(0, h - 1, 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);

        ACTIVE_CAPSULES.put(player.getUUID(), capsule);

        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 2.0F, 1.0F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.0F, 1.2F);

        player.displayClientMessage(
                Component.literal("§6[CAPSULE SHELTER] §aĐã mở lều gỗ dã ngoại tiện nghi tại chỗ! Đấm vào lều để thu hồi."),
                true
        );
    }

    // =========================================================================
    // VÒNG LẶP SERVER TICK (ZERO-LAG)
    // =========================================================================
    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();

        // 1. Quét tương tác bước qua Cánh Cửa Không Gian ngoài đời thực
        if (!ACTIVE_GATES.isEmpty()) {
            for (Map.Entry<UUID, ActiveGate> entry : ACTIVE_GATES.entrySet()) {
                ActiveGate gate = entry.getValue();
                if (gate.level != level) continue;

                List<Entity> touching = level.getEntities((Entity) null, gate.triggerBox);
                for (Entity e : touching) {
                    if (e instanceof ServerPlayer sp) {
                        Long lastTp = LAST_TELEPORT_TIME.get(sp.getUUID());
                        if (lastTp == null || gameTime - lastTp > 45) {
                            LAST_TELEPORT_TIME.put(sp.getUUID(), gameTime);
                            handleStepIntoOuterGate(sp, gate);
                        }
                    } else if (e instanceof Monster || e instanceof Enemy) {
                        Vec3 push = e.position().subtract(gate.pos).normalize().scale(0.8);
                        e.setDeltaMovement(push.x, 0.4, push.z);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1.0, e.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }

        // 2. Xử lý bên trong Đảo Nổi Biệt Thự Gỗ Sân Vườn (Mỗi 5 ticks trên Overworld)
        if (level == level.getServer().overworld() && gameTime % 5 == 0) {
            for (ServerPlayer player : level.players()) {
                if (!isInsideSanctuary(level, player.position())) continue;

                BlockPos center = getSanctuaryCenter(player.getUUID());
                int cy = center.getY();
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();

                // Đồng bộ lại cổng sân vườn định kỳ phòng khi người chơi vừa đăng nhập lại bên trong đảo
                if (gameTime % 100 == 0) {
                    syncSanctuaryCourtyardGate(level, player, true);
                }

                // 2.1. Kiểm tra Suối Nước Nóng Lộ Thiên (dx = +5.5..+11.0, dz = -9.5..-4.0, py quanh mặt hồ cy)
                if (px >= center.getX() + 5.5 && px <= center.getX() + 11.0 &&
                    pz >= center.getZ() - 9.5 && pz <= center.getZ() - 4.0 &&
                    py >= cy - 0.8 && py <= cy + 2.0) {

                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false, true));

                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WITHER);
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.WEAKNESS);
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.CONFUSION);
                    player.removeEffect(MobEffects.BAD_OMEN);

                    level.sendParticles(ParticleTypes.SPLASH, px, py + 0.6, pz, 3, 0.3, 0.1, 0.3, 0.04);

                    if (gameTime % 25 == 0) {
                        player.displayClientMessage(
                                Component.literal("§b♨️ [SUỐI NƯỚC NÓNG ANH ĐÀO] §fNước khoáng ấm đang hồi phục sinh lực và thanh tẩy toàn diện..."),
                                true
                        );
                    }

                    for (Entity pet : getNearbyPets(player, 7.0)) {
                        if (pet instanceof LivingEntity le) {
                            le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false, true));
                        }
                    }
                }

                // 2.2. Kiểm tra bước vào Cánh Cổng Thần Môn Sân Vườn tại (cx + 0.5, cy + 1, cz - 12.0)
                if (Math.abs(px - (center.getX() + 0.5)) <= 1.6 &&
                    pz >= center.getZ() - 13.0 && pz <= center.getZ() - 11.0 &&
                    py >= cy && py <= cy + 5.0) {

                    Long lastTp = LAST_TELEPORT_TIME.get(player.getUUID());
                    if (lastTp == null || gameTime - lastTp > 45) {
                        LAST_TELEPORT_TIME.put(player.getUUID(), gameTime);
                        handleStepIntoSanctuaryGate(player);
                    }
                }

                // 2.3. BẢO ĐẢM TUYỆT ĐỐI 100% KHÔNG BAO GIỜ SPAWN QUÁI THÙ ĐỊCH TRÊN ĐẢO NỔI
                // (Giữ lại ác ma đã ký khế ước / thú cưng đã thuần phục và Velgrynd đồng minh)
                AABB islandBox = new AABB(center).inflate(22.0, 14.0, 22.0);
                List<Mob> hostiles = level.getEntitiesOfClass(Mob.class, islandBox, m ->
                        m.isAlive()
                                && !(m instanceof TamableAnimal tame && tame.isTame())
                                && !(m instanceof VelgryndEntity velgrynd && velgrynd.isAllied())
                                && (m instanceof Monster || m instanceof Enemy || m.getType().getCategory() == MobCategory.MONSTER)
                );
                for (Mob mob : hostiles) {
                    mob.discard();
                }
            }
        }
    }
}
