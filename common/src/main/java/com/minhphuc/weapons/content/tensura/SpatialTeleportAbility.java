package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HỆ THỐNG MA PHÁP KHÔNG GIAN TENSURA (SLIME DATTA KEN):
 * 1. Kỹ năng 1 - ĐÁNH DẤU (Spatial Mark):
 *    - Tạo ra Ma Pháp Trận 3D màu xanh lam/cyan chuẩn nguyên tác Slime Datta Ken kèm hiệu ứng tia sáng bay lên
 *      và góc máy điện ảnh giống kỹ năng Dịch Chuyển, nhưng chỉ khắc ấn ký mà không dịch chuyển.
 *    - Có thể phá hủy bằng cách đập (chuột trái vào block hoặc đánh tay không).
 *    - Khi dùng Dịch Chuyển dịch chuyển đến đó xong thì dấu sẽ tự động biến mất.
 * 2. Kỹ năng 2 - DỊCH CHUYỂN (Spatial Teleportation):
 *    - Nếu ĐÃ CÓ ĐÁNH DẤU: Dịch chuyển chuẩn xác tới Ấn Ký.
 *    - Nếu CHƯA CÓ ĐÁNH DẤU: Dịch chuyển ngẫu nhiên tới địa điểm kỳ thú (Làng, Đền Warden, Địa Ngục, Thần Điện Dưới Biển, Di tích, Hoang dã).
 *    - Chuỗi diễn hoạt 2 giai đoạn hoàn hảo:
 *      Giai đoạn 1 (Điểm đi): Tạo ma pháp trận dưới chân -> tia sáng bay lên -> Người chơi BIẾN MẤT khỏi vòng trận.
 *      Giai đoạn 2 (Điểm đến): Nạp trước map & khóa góc quay đợi chunk load xong -> Tạo ma pháp trận ở điểm đến -> Người chơi HIỆN HÌNH -> 6 góc máy điện ảnh.
 */
public class SpatialTeleportAbility {

    public static final float MODE_MARK_CAST = 0.0f;
    public static final float MODE_DEPART_PERSONAL = 1.0f;
    public static final float MODE_FIXED_COUNTDOWN = 2.0f;
    public static final float MODE_ARRIVAL_CINEMATIC = 3.0f;
    public static final float MODE_COMBAT_BLINK = 4.0f;

    private static final DustParticleOptions CYAN_MAGIC_DUST = new DustParticleOptions(new Vector3f(0.0F, 0.9F, 1.0F), 1.6F);
    private static final DustParticleOptions BLUE_MAGIC_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.45F, 1.0F), 1.8F);
    private static final DustParticleOptions WHITE_LIGHT_DUST = new DustParticleOptions(new Vector3f(0.85F, 0.98F, 1.0F), 1.4F);

    public record SpatialMark(
            UUID ownerUuid,
            ResourceKey<Level> dimension,
            Vec3 pos,
            float yaw,
            long createdTick
    ) {}

    public record RandomDestResult(
            ServerLevel level,
            BlockPos safePos,
            String title,
            int envType // 0 = Overworld/Village, 1 = Warden Ancient City, 2 = Nether, 3 = Ocean/Underwater, 4 = Trial Chambers
    ) {}

    public static final int MAX_ANCHOR_MARKS = 5;

    public static class PendingPersonalTeleport {
        public final ServerLevel originLevel;
        public final UUID casterUuid;
        public final List<UUID> accompanyingUuids;
        public final Vec3 originCenter;
        public final SpatialMark targetMark; // null nếu là random
        public final ServerLevel targetLevel;
        public final Vec3 targetDestPos;
        public final float targetYaw;
        public final String destTitle;
        public final int envType;
        public final boolean isRandom;
        public final boolean consumeMark;
        public int ticksElapsed = 0;
        public final int totalCastTicks = 44;

        public PendingPersonalTeleport(ServerLevel originLevel, UUID casterUuid, List<UUID> accompanyingUuids, Vec3 originCenter,
                                       SpatialMark targetMark, ServerLevel targetLevel, Vec3 targetDestPos, float targetYaw,
                                       String destTitle, int envType, boolean isRandom, boolean consumeMark) {
            this.originLevel = originLevel;
            this.casterUuid = casterUuid;
            this.accompanyingUuids = accompanyingUuids != null ? accompanyingUuids : List.of();
            this.originCenter = originCenter;
            this.targetMark = targetMark;
            this.targetLevel = targetLevel;
            this.targetDestPos = targetDestPos;
            this.targetYaw = targetYaw;
            this.destTitle = destTitle;
            this.envType = envType;
            this.isRandom = isRandom;
            this.consumeMark = consumeMark;
        }
    }

    public static class PendingDemonTeleport {
        public final ServerLevel originLevel;
        public final UUID casterUuid;
        public final Vec3 originCenter;
        public final ServerLevel targetLevel;
        public final BlockPos safeDestPos;
        public final Vec3 exactDestPos;
        public final float targetYaw;
        public final String destTitle;
        public final int envType;
        public final boolean consumeMark;
        public final List<UUID> demonUuids;
        public int ticksElapsed = 0;
        public final int totalCastTicks = 44;

        public PendingDemonTeleport(ServerLevel originLevel, UUID casterUuid, Vec3 originCenter, ServerLevel targetLevel,
                                    BlockPos safeDestPos, String destTitle, List<UUID> demonUuids) {
            this(originLevel, casterUuid, originCenter, targetLevel, safeDestPos, null, 0.0F, destTitle, 0, false, demonUuids);
        }

        public PendingDemonTeleport(ServerLevel originLevel, UUID casterUuid, Vec3 originCenter, ServerLevel targetLevel,
                                    BlockPos safeDestPos, Vec3 exactDestPos, float targetYaw, String destTitle,
                                    int envType, boolean consumeMark, List<UUID> demonUuids) {
            this.originLevel = originLevel;
            this.casterUuid = casterUuid;
            this.originCenter = originCenter;
            this.targetLevel = targetLevel;
            this.safeDestPos = safeDestPos;
            this.exactDestPos = exactDestPos;
            this.targetYaw = targetYaw;
            this.destTitle = destTitle;
            this.envType = envType;
            this.consumeMark = consumeMark;
            this.demonUuids = demonUuids;
        }
    }

    public static class ActiveCountdownCircle {
        public final ServerLevel level;
        public final UUID casterUuid;
        public final Vec3 center;
        public final float radius = 4.5f;
        public int ageTicks = 0;
        public int insideCountdownTicks = 0; // 0 -> 80 (4 seconds: 4, 3, 2, 1)
        public int lastAnnouncedSecond = -1;
        public RandomDestResult randomDest = null;

        public ActiveCountdownCircle(ServerLevel level, UUID casterUuid, Vec3 center) {
            this.level = level;
            this.casterUuid = casterUuid;
            this.center = center;
        }
    }

    public static class ActiveArrivalSequence {
        public final ServerLevel level;
        public final UUID casterUuid;
        public final Vec3 destPos;
        public final boolean consumeMarkOnFinish;
        public int ticksElapsed = 0;
        public final int totalTicks = 100;

        public ActiveArrivalSequence(ServerLevel level, UUID casterUuid, Vec3 destPos, boolean consumeMarkOnFinish) {
            this.level = level;
            this.casterUuid = casterUuid;
            this.destPos = destPos;
            this.consumeMarkOnFinish = consumeMarkOnFinish;
        }
    }

    public static class PendingSendToAnchor {
        public final ServerLevel originLevel;
        public final UUID casterUuid;
        public final List<UUID> targetUuids;
        public final Vec3 sourceCenter;
        public final ServerLevel targetLevel;
        public final Vec3 destPos;
        public final int anchorSlot;
        public int ticksElapsed = 0;
        public final int totalTicks = 32;

        public PendingSendToAnchor(ServerLevel originLevel, UUID casterUuid, List<UUID> targetUuids, Vec3 sourceCenter,
                                   ServerLevel targetLevel, Vec3 destPos, int anchorSlot) {
            this.originLevel = originLevel;
            this.casterUuid = casterUuid;
            this.targetUuids = targetUuids;
            this.sourceCenter = sourceCenter;
            this.targetLevel = targetLevel;
            this.destPos = destPos;
            this.anchorSlot = anchorSlot;
        }
    }

    private record ClickedEntityRecord(UUID entityUuid, long gameTick) {}

    private static final Map<UUID, SpatialMark> ACTIVE_MARKS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<SpatialMark>> ANCHOR_MARKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SELECTED_ANCHOR_INDEX = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingPersonalTeleport> PENDING_PERSONAL = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingDemonTeleport> PENDING_DEMON_TELEPORTS = new ConcurrentHashMap<>();
    private static final List<PendingSendToAnchor> PENDING_SENDS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Map<UUID, ActiveCountdownCircle> ACTIVE_COUNTDOWN_CIRCLES = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveArrivalSequence> ACTIVE_ARRIVALS = new ConcurrentHashMap<>();
    private static final Map<UUID, ClickedEntityRecord> RECENT_CLICKED_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CAST_TIME = new ConcurrentHashMap<>();

    public static void recordClickedEntity(ServerPlayer player, Entity target) {
        if (player == null || target == null || !target.isAlive()) return;
        RECENT_CLICKED_ENTITIES.put(player.getUUID(), new ClickedEntityRecord(target.getUUID(), player.level().getGameTime()));
    }

    // =========================================================================
    // TÌM ĐỊA ĐIỂM NGẪU NHIÊN KHI CHƯA CÓ ĐÁNH DẤU (RANDOM DESTINATIONS)
    // =========================================================================

    public static BlockPos locateStructure(ServerLevel level, ResourceKey<Structure> key, BlockPos nearPos, int radius) {
        try {
            var holderSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
            if (holderSet.isPresent()) {
                var pair = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holderSet.get()), nearPos, radius, true);
                if (pair != null) {
                    return pair.getFirst();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static BlockPos locateTag(ServerLevel level, TagKey<Structure> tag, BlockPos nearPos, int radius) {
        try {
            return level.findNearestMapStructure(tag, nearPos, radius, true);
        } catch (Exception ignored) {}
        return null;
    }

    public static RandomDestResult resolveRandomDestination(ServerLevel level, ServerPlayer player) {
        int roll = level.random.nextInt(6);
        ServerLevel overworld = (level.dimension() == Level.OVERWORLD) ? level : (level.getServer() != null ? level.getServer().overworld() : level);
        ServerLevel nether = (level.getServer() != null) ? level.getServer().getLevel(Level.NETHER) : null;
        BlockPos originPos = player.blockPosition();

        if (roll == 0) { // 1. Ngôi Làng (Village)
            BlockPos village = locateTag(overworld, StructureTags.VILLAGE, originPos, 120);
            if (village != null) {
                BlockPos safe = AbyssalGovernanceManager.findSafeTeleportPos(overworld, village, false, false);
                return new RandomDestResult(overworld, safe, "Ngôi Làng Dân Cư Bình Yên", 0);
            }
        } else if (roll == 1) { // 2. Đền Warden / Thành Cổ Cổ Đại (Ancient City)
            BlockPos city = locateStructure(overworld, BuiltinStructures.ANCIENT_CITY, originPos, 150);
            if (city != null) {
                BlockPos safe = AbyssalGovernanceManager.findSafeTeleportPos(overworld, city, false, true);
                return new RandomDestResult(overworld, safe, "Thành Cổ Cổ Đại (Đền Warden - Deep Dark)", 1);
            }
        } else if (roll == 2 && nether != null) { // 3. Địa Ngục (Nether Fortress / Bastion)
            boolean fortress = level.random.nextBoolean();
            ResourceKey<Structure> key = fortress ? BuiltinStructures.FORTRESS : BuiltinStructures.BASTION_REMNANT;
            String name = fortress ? "Pháo Đài Địa Ngục (Nether Fortress)" : "Tàn Tích Pháo Đài Heo Vàng (Bastion)";
            BlockPos netherPos = locateStructure(nether, key, BlockPos.ZERO, 150);
            if (netherPos == null) {
                int rx = level.random.nextInt(1200) - 600;
                int rz = level.random.nextInt(1200) - 600;
                netherPos = new BlockPos(rx, 64, rz);
                name = "Vực Thẳm Dung Nham Địa Ngục";
            }
            BlockPos safe = AbyssalGovernanceManager.findSafeTeleportPos(nether, netherPos, true, false);
            return new RandomDestResult(nether, safe, name, 2);
        } else if (roll == 3) { // 4. Dưới Biển (Ocean Monument)
            BlockPos ocean = locateStructure(overworld, BuiltinStructures.OCEAN_MONUMENT, originPos, 150);
            if (ocean != null) {
                BlockPos safe = AbyssalGovernanceManager.findSafeTeleportPos(overworld, ocean, false, false);
                return new RandomDestResult(overworld, safe, "Thần Điện Đại Dương (Ocean Monument)", 3);
            }
        } else if (roll == 4) { // 5. Mật Viện Thử Thách (Trial Chambers)
            BlockPos trial = locateStructure(overworld, BuiltinStructures.TRIAL_CHAMBERS, originPos, 150);
            if (trial != null) {
                BlockPos safe = AbyssalGovernanceManager.findSafeTeleportPos(overworld, trial, false, true);
                return new RandomDestResult(overworld, safe, "Mật Viện Thử Thách (Trial Chambers)", 4);
            }
        }

        // 6. Fallback: Vùng Đất Mới Hoang Dã trên Overworld (cách 800m -> 3200m)
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double dist = 800.0 + level.random.nextDouble() * 2400.0;
        int targetX = (int) (originPos.getX() + Math.cos(angle) * dist);
        int targetZ = (int) (originPos.getZ() + Math.sin(angle) * dist);
        BlockPos rawPos = new BlockPos(targetX, 64, targetZ);
        BlockPos safe = AbyssalGovernanceManager.findSafeTeleportPos(overworld, rawPos, false, false);
        return new RandomDestResult(overworld, safe, "Vùng Đất Mới Khám Phá (Overworld)", 0);
    }

    public static void applyTeleportProtection(LivingEntity entity, int envType) {
        if (entity == null) return;
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 4, false, false));
        if (envType == 1) { // Ancient City
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
        } else if (envType == 2) { // Nether
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
        } else if (envType == 3) { // Ocean
            entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 6000, 0, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 6000, 0, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
        } else if (envType == 4) { // Trial Chambers
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
        }
    }

    // =========================================================================
    // KỸ NĂNG 1: ĐÁNH DẤU (SPATIAL MARK)
    // =========================================================================
    public static void castMark(ServerLevel level, ServerPlayer player) {
        castMark(level, player, player != null && player.isShiftKeyDown());
    }

    public static void castMark(ServerLevel level, ServerPlayer player, boolean asAnchor) {
        long now = System.currentTimeMillis();
        if (now - LAST_CAST_TIME.getOrDefault(player.getUUID(), 0L) < 250) return;
        LAST_CAST_TIME.put(player.getUUID(), now);

        // Xác định vị trí đặt dấu: nếu nhìn vào block trong tầm 24m thì đặt trên mặt block đó, ngược lại đặt dưới chân người chơi
        Vec3 markPos = resolveMarkPlacementPos(level, player, 24.0D);
        int anchorSlot = placeMarkAt(level, player, markPos, player.getYRot(), asAnchor);

        player.displayClientMessage(
                Component.literal(asAnchor
                        ? String.format("§6§l✦ [ẤN KÝ VĨNH VIỄN #%d/%d] §fĐã khắc tại §e[%.0f, %.0f, %.0f]§f! §7(Chuột phải Ấn Ký trên G để dịch chuyển / Shift+Click để gửi vật & sinh vật)",
                        anchorSlot + 1, MAX_ANCHOR_MARKS, markPos.x, markPos.y, markPos.z)
                        : String.format("§b§l✦ [ĐÁNH DẤU CHIẾN THUẬT] §fĐã khắc Ma Pháp Trận tại §e[%.0f, %.0f, %.0f]§f! §7(Giữ Shift để đặt Ấn Ký Vĩnh Viễn #%d/%d)",
                        markPos.x, markPos.y, markPos.z, Math.min(getAnchorMarks(player).size() + 1, MAX_ANCHOR_MARKS), MAX_ANCHOR_MARKS)),
                true
        );
    }

    public static void castMarkFromDemon(ServerLevel level, ServerPlayer owner, PrimordialDemonEntity demon) {
        if (level == null || owner == null || demon == null) return;

        Vec3 start = demon.position().add(0, 1.0D, 0);
        Vec3 end = demon.position().subtract(0, 32.0D, 0);
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, demon));
        Vec3 markPos = (hit.getType() == HitResult.Type.BLOCK)
                ? new Vec3(hit.getBlockPos().getX() + 0.5D, hit.getBlockPos().getY() + 1.02D, hit.getBlockPos().getZ() + 0.5D)
                : new Vec3(demon.getX(), demon.getY() + 0.02D, demon.getZ());

        placeMarkAt(level, owner, markPos, demon.getYRot(), false);

        owner.displayClientMessage(
                Component.literal(String.format("§b§l✦ [%s - ĐÁNH DẤU] §fĐã khắc Ma Pháp Trận tại §e[%.0f, %.0f, %.0f]§f!",
                        demon.getEffectiveDemonName().toUpperCase(), markPos.x, markPos.y, markPos.z)),
                true
        );
    }

    private static int placeMarkAt(ServerLevel level, ServerPlayer owner, Vec3 markPos, float yaw, boolean asAnchor) {
        SpatialMark newMark = new SpatialMark(owner.getUUID(), level.dimension(), markPos, yaw, level.getGameTime());
        int visualSlot = 0;
        int placedAnchorIdx = 0;

        if (!asAnchor) {
            SpatialMark oldMark = getMark(owner, false);
            stopMarkVisual(owner, oldMark, 0);
            saveMark(owner, newMark);
        } else {
            List<SpatialMark> anchors = new ArrayList<>(getAnchorMarks(owner));
            if (anchors.size() < MAX_ANCHOR_MARKS) {
                placedAnchorIdx = anchors.size();
                anchors.add(newMark);
            } else {
                placedAnchorIdx = Math.clamp(getSelectedAnchorIndex(owner), 0, MAX_ANCHOR_MARKS - 1);
                stopMarkVisual(owner, anchors.get(placedAnchorIdx), placedAnchorIdx + 1);
                anchors.set(placedAnchorIdx, newMark);
            }
            visualSlot = placedAnchorIdx + 1;
            saveAnchorMarks(owner, anchors, placedAnchorIdx);
        }

        TensuraSkillHelper.syncSkillStateToClient(owner);

        // Âm thanh & hạt ma pháp khởi tạo Ấn Ký Không Gian
        level.playSound(null, markPos.x, markPos.y, markPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.2F, 1.35F);
        level.playSound(null, markPos.x, markPos.y, markPos.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2.5F, 1.1F);
        level.playSound(null, markPos.x, markPos.y, markPos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0F, 1.2F);

        spawnMagicCircleParticles(level, markPos, 2.5D, 45);

        // Gửi gói tin kích hoạt Ma Pháp Trận 3D (tồn lưu trên mặt đất 72000 ticks)
        // dir.y = 0.0 -> Tactical Mark; 1.0..5.0 -> Anchor Mark #1..#5
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_MARK,
                owner.getUUID(),
                markPos,
                new Vec3(MODE_MARK_CAST, visualSlot, 0.0),
                72000,
                ClientboundCinematicSkillPacket.ACTION_START,
                2.4f
        ));

        return placedAnchorIdx;
    }

    // =========================================================================
    // KỸ NĂNG 2: DỊCH CHUYỂN (SPATIAL TELEPORTATION)
    // =========================================================================
    public static void castTeleport(ServerLevel level, ServerPlayer player) {
        if (player == null) return;
        if (player.isShiftKeyDown()) {
            castCombatBlink(level, player);
            return;
        }
        castTeleportToSlot(level, player, -1);
    }

    /**
     * slot: -1 = Tự động (Chiến Thuật -> Vĩnh Viễn đang chọn -> Ngẫu Nhiên),
     *       0 = Chiến Thuật, 1 = Vĩnh Viễn (slot đang chọn), 2 = Combat Blink, 3 = Ngẫu Nhiên,
     *       10..14 = Ấn Ký Vĩnh Viễn #1..#5
     */
    public static void castTeleportToSlot(ServerLevel level, ServerPlayer player, int slot) {
        if (player == null || level == null) return;
        if (slot == 2) {
            castCombatBlink(level, player);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - LAST_CAST_TIME.getOrDefault(player.getUUID(), 0L) < 250) return;
        LAST_CAST_TIME.put(player.getUUID(), now);

        if (PENDING_PERSONAL.containsKey(player.getUUID()) || PENDING_DEMON_TELEPORTS.containsKey(player.getUUID())) {
            return;
        }

        // 1. Xác định Ấn Ký đích theo slot (ưu tiên Ấn Ký Chiến Thuật -> Ấn Ký Vĩnh Viễn -> Ngẫu nhiên)
        SpatialMark tactical = (slot == -1 || slot == 0) ? getMark(player, false) : null;
        int specificAnchorIdx = (slot >= 10 && slot < 10 + MAX_ANCHOR_MARKS) ? (slot - 10) : -1;
        SpatialMark anchor = null;
        int usedAnchorSlot = -1;
        if (tactical == null) {
            if (specificAnchorIdx >= 0) {
                anchor = getAnchorMark(player, specificAnchorIdx);
                usedAnchorSlot = specificAnchorIdx;
                if (anchor != null) {
                    setSelectedAnchorIndex(player, specificAnchorIdx);
                }
            } else if (slot == -1 || slot == 1) {
                anchor = getActiveAnchorMark(player);
                usedAnchorSlot = getSelectedAnchorIndex(player);
            }
        }
        SpatialMark mark = (tactical != null) ? tactical : anchor;
        boolean consumeMark = (tactical != null);
        RandomDestResult randomDest = (mark == null) ? resolveRandomDestination(level, player) : null;

        // 2. Kiểm tra xem người chơi có vừa click/nhắm vào sinh vật hoặc vật phẩm/đối tượng hay không
        Entity aimedTarget = findClickedOrAimedTeleportable(level, player, 22.0D);

        // 3. Nếu không nhắm vào đối tượng, kiểm tra Raycast nhìn vào block cố định hay nhìn lên hư không / chọn trực tiếp từ menu
        BlockHitResult blockHit = raycastBlock(level, player, 28.0D);
        boolean lookingAtSkyOrVoid = (blockHit.getType() == HitResult.Type.MISS) || (slot >= 0);

        if (aimedTarget != null || lookingAtSkyOrVoid) {
            // =================================================================
            // CHẾ ĐỘ A: HƯỚNG LÊN HƯ KHÔNG HOẶC CLICK/NHẮM VÀO SINH VẬT / VẬT THỂ
            // -> Dịch chuyển người chơi cùng các sinh vật & vật phẩm lân cận!
            // =================================================================
            List<Entity> collected = new ArrayList<>();
            if (aimedTarget != null) {
                if (aimedTarget.distanceToSqr(player) > 3.2D * 3.2D) {
                    Vec3 pullPos = player.position().add(player.getLookAngle().multiply(1.4D, 0.0D, 1.4D));
                    aimedTarget.teleportTo(pullPos.x, player.getY() + 0.1D, pullPos.z);
                }
                collected.add(aimedTarget);
                for (Entity nearTarget : collectTeleportableEntities(level, aimedTarget.position(), 2.6D, 2.0D, 2.5D, player, false)) {
                    if (!collected.contains(nearTarget) && collected.size() < 32) {
                        collected.add(nearTarget);
                    }
                }
            }
            for (Entity nearPlayer : collectTeleportableEntities(level, player.position(), 3.0D, 2.0D, 3.0D, player, false)) {
                if (!collected.contains(nearPlayer) && collected.size() < 32) {
                    collected.add(nearPlayer);
                }
            }

            Vec3 circleCenter = player.position();
            if (!collected.isEmpty() && collected.get(0).distanceToSqr(player) <= 12.0D) {
                Entity primary = collected.get(0);
                circleCenter = new Vec3(
                        (player.getX() + primary.getX()) * 0.5D,
                        Math.min(player.getY(), primary.getY()),
                        (player.getZ() + primary.getZ()) * 0.5D
                );
            }

            List<UUID> accompanyingIds = new ArrayList<>(collected.size());
            for (Entity ent : collected) {
                accompanyingIds.add(ent.getUUID());
            }

            ServerLevel targetLevel;
            Vec3 targetDest;
            float targetYaw;
            String destTitle;
            int envType;
            boolean isRandom;

            if (mark != null) {
                targetLevel = level.getServer().getLevel(mark.dimension());
                if (targetLevel == null) targetLevel = level;
                targetDest = mark.pos();
                targetYaw = mark.yaw();
                destTitle = consumeMark
                        ? "Ấn Ký Chiến Thuật"
                        : "Ấn Ký Vĩnh Viễn #" + (Math.max(0, usedAnchorSlot) + 1);
                envType = 0;
                isRandom = false;
            } else {
                targetLevel = randomDest.level();
                targetDest = new Vec3(randomDest.safePos().getX() + 0.5D, randomDest.safePos().getY() + 0.05D, randomDest.safePos().getZ() + 0.5D);
                targetYaw = player.getYRot();
                destTitle = randomDest.title();
                envType = randomDest.envType();
                isRandom = true;
            }

            // NẠP TRƯỚC CHUNK TẠI ĐIỂM ĐẾN ĐỂ SẴN SÀNG KHÔNG BỊ TRỄ LOAD MAP
            BlockPos destBlock = BlockPos.containing(targetDest);
            targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(destBlock), 3, 150);

            PendingPersonalTeleport pending = new PendingPersonalTeleport(
                    level, player.getUUID(), accompanyingIds, circleCenter, mark, targetLevel, targetDest, targetYaw, destTitle, envType, isRandom, consumeMark
            );
            PENDING_PERSONAL.put(player.getUUID(), pending);

            // Hủy vòng đếm ngược cố định cũ nếu có
            removeCountdownCircle(player.getUUID());

            level.playSound(null, circleCenter.x, circleCenter.y, circleCenter.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.25F);
            level.playSound(null, circleCenter.x, circleCenter.y, circleCenter.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 2.2F, 1.1F);
            spawnMagicCircleParticles(level, circleCenter, 2.8D, 50);

            // Kích hoạt Ma Pháp Trận 3D + Góc quay camera thấy tạo ra Ma Pháp Trận 3D & tia sáng bay lên (44 ticks)
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                    player.getUUID(),
                    circleCenter,
                    new Vec3(MODE_DEPART_PERSONAL, 0.0, 0.0),
                    44,
                    ClientboundCinematicSkillPacket.ACTION_START,
                    2.8f
            ));

            String msg;
            if (isRandom) {
                msg = "§d§l✦ [DỊCH CHUYỂN HƯ KHÔNG] §fChưa có Đánh Dấu! Khởi tạo Ma Pháp Trận dịch chuyển ngẫu nhiên tới: §e" + destTitle + "§f!";
            } else if (!collected.isEmpty()) {
                msg = "§b§l✦ [DỊCH CHUYỂN KHÔNG GIAN] §fĐang đưa §e" + collected.size() + " sinh vật & vật thể §ftới §6" + destTitle + "§f!";
            } else {
                msg = "§b§l✦ [DỊCH CHUYỂN KHÔNG GIAN] §fDịch chuyển tới §e" + destTitle + "§f!";
            }
            player.displayClientMessage(Component.literal(msg), true);

        } else {
            // =================================================================
            // CHẾ ĐỘ B: HƯỚNG VÀO 1 NƠI CỐ ĐỊNH TRÊN MẶT ĐẤT
            // -> Tạo Ma Pháp Trận 3D cấp cao hơn. Đếm ngược 4 - 3 - 2 - 1 rồi dịch chuyển toàn bộ sinh vật!
            // =================================================================
            BlockPos hitPos = blockHit.getBlockPos();
            Vec3 fixedPos = new Vec3(hitPos.getX() + 0.5D, hitPos.getY() + 1.0D, hitPos.getZ() + 0.5D);
            if (!level.getBlockState(hitPos.above()).isAir() && level.getBlockState(hitPos).isAir()) {
                fixedPos = new Vec3(hitPos.getX() + 0.5D, hitPos.getY() + 0.05D, hitPos.getZ() + 0.5D);
            }

            removeCountdownCircle(player.getUUID());

            ActiveCountdownCircle circle = new ActiveCountdownCircle(level, player.getUUID(), fixedPos);
            circle.randomDest = randomDest;
            ACTIVE_COUNTDOWN_CIRCLES.put(player.getUUID(), circle);

            if (randomDest != null) {
                randomDest.level().getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(randomDest.safePos()), 3, 200);
            } else if (mark != null && level.getServer() != null) {
                ServerLevel tl = level.getServer().getLevel(mark.dimension());
                if (tl != null) {
                    tl.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(BlockPos.containing(mark.pos())), 3, 200);
                }
            }

            level.playSound(null, fixedPos.x, fixedPos.y, fixedPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.8F, 0.95F);
            level.playSound(null, fixedPos.x, fixedPos.y, fixedPos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 3.0F, 1.0F);
            spawnMagicCircleParticles(level, fixedPos, 4.5D, 75);

            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                    player.getUUID(),
                    fixedPos,
                    new Vec3(MODE_FIXED_COUNTDOWN, 0.0, 0.0),
                    1200,
                    ClientboundCinematicSkillPacket.ACTION_START,
                    4.5f
            ));

            String title = (randomDest != null) ? randomDest.title() : "Ấn Ký Đánh Dấu";
            String msg = (randomDest != null)
                    ? "§d§l✦ [ĐẠI MA PHÁP TRẬN DỊCH CHUYỂN HƯ KHÔNG] §fChưa có Đánh Dấu! Điểm đến ngẫu nhiên: §e" + title + "§f. Hãy bước vào trong vòng trận để bắt đầu đếm ngược 4 - 3 - 2 - 1!"
                    : "§b§l✦ [ĐẠI MA PHÁP TRẬN DỊCH CHUYỂN] §fĐã khai mở tại điểm cố định! §eHãy bước vào trong vòng trận để bắt đầu đếm ngược 4 - 3 - 2 - 1!";
            player.displayClientMessage(Component.literal(msg), true);
        }
    }

    /**
     * Dịch Chuyển Chiến Đấu (Combat Blink - Giữ Shift + Dịch Chuyển):
     * - Dịch chuyển tức thì ra sau lưng mục tiêu đang nhắm (tối đa 28m) hoặc lướt xuyên không gian 20m theo hướng nhìn.
     * - Không khóa góc máy cutscene để chiến đấu tốc độ cao, kèm vòng tròn pháp trận nhanh & xung kích không gian.
     */
    public static void castCombatBlink(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        if (!TensuraSkillHelper.checkAndStartCooldown(player, "combat_blink", 20, "Dịch Chuyển Chiến Đấu")) {
            return;
        }

        Vec3 origin = player.position();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity aimed = findClickedOrAimedCreature(level, player, 28.0D);

        Vec3 dest;
        float destYaw = player.getYRot();
        if (aimed != null && TensuraSkillHelper.isValidSkillTarget(player, aimed)) {
            Vec3 targetLook = Vec3.directionFromRotation(0.0F, aimed.getYRot()).normalize();
            dest = aimed.position().subtract(targetLook.scale(2.2D));
            destYaw = aimed.getYRot();
        } else {
            BlockHitResult hit = raycastBlock(level, player, 22.0D);
            if (hit.getType() == HitResult.Type.BLOCK) {
                dest = hit.getLocation().subtract(look.scale(1.2D));
            } else {
                dest = origin.add(look.scale(18.0D));
            }
        }

        BlockPos safePos = AbyssalGovernanceManager.findSafeTeleportPos(level, BlockPos.containing(dest), level.dimension() == Level.NETHER, true);
        Vec3 finalDest = (aimed != null) ? dest : Vec3.atBottomCenterOf(safePos);

        spawnMagicCircleParticles(level, origin, 1.8D, 24);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.35F);

        player.teleportTo(level, finalDest.x, finalDest.y + 0.05D, finalDest.z, destYaw, player.getXRot());
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 2, false, false));

        spawnMagicCircleParticles(level, finalDest, 2.0D, 32);
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                player.getUUID(),
                finalDest,
                new Vec3(MODE_COMBAT_BLINK, 0.0, 0.0),
                20,
                ClientboundCinematicSkillPacket.ACTION_START,
                1.8f
        ));
        level.playSound(null, finalDest.x, finalDest.y, finalDest.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.2F, 1.4F);

        if (aimed != null && TensuraSkillHelper.isValidSkillTarget(player, aimed)) {
            float blinkDamage = PrimordialPossessionManager.isPossessing(player)
                    ? Math.max(120.0F, com.minhphuc.weapons.data.EntityDataHelper.getCustomData(player).getFloat(PrimordialPossessionManager.NBT_DEMON_ATK) * 2.2F)
                    : 120.0F;
            aimed.hurt(player.damageSources().magic(), blinkDamage);
            aimed.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
            player.displayClientMessage(Component.literal("§b§l⚡ [KHÔNG GIAN ĐỘT KÍCH] §fĐã áp sát sau lưng §e" + aimed.getName().getString() + "§f!"), true);
        } else {
            player.displayClientMessage(Component.literal("§b§l⚡ [KHÔNG GIAN TỨC THỜI] §fĐã lướt xuyên không gian!"), true);
        }
    }

    // =========================================================================
    // DỊCH CHUYỂN ĐIỆN ẢNH CHO TOÀN BỘ ÁC MA THỦY TỔ (KIẾM LÀNG / DI TÍCH / CĂN CỨ)
    // =========================================================================
    public static void executeCinematicDemonTeleport(ServerLevel level, ServerPlayer player, BlockPos safeDestPos, String destTitle) {
        if (level == null || player == null || safeDestPos == null) return;

        // 1. Tìm các Ác Ma Thủy Tổ thuộc sở hữu của người chơi đang đứng gần để đưa đi cùng
        List<PrimordialDemonEntity> ownedDemons = level.getEntitiesOfClass(
                PrimordialDemonEntity.class,
                player.getBoundingBox().inflate(32.0D),
                d -> d.isAlive() && d.isOwnedBy(player) && !d.isOrderedToSit() && d.getGuardCenterPos() == null
        );
        List<UUID> demonIds = new ArrayList<>();
        for (PrimordialDemonEntity d : ownedDemons) {
            demonIds.add(d.getUUID());
        }

        // 2. Nạp trước chunk tại điểm đến ngay lập tức trên Server để không bị giật lag khi chuyển cảnh
        ServerLevel targetLevel = level;
        targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(safeDestPos), 3, 150);

        Vec3 center = player.position();
        spawnMagicCircleParticles(level, center, 2.8D, 50);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.25F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 2.2F, 1.1F);

        // 3. Khởi chạy diễn hoạt điểm đi (44 ticks: tạo ma pháp trận dưới chân -> biến mất tại tick 24)
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                player.getUUID(),
                center,
                new Vec3(MODE_DEPART_PERSONAL, 0.0, 0.0),
                44,
                ClientboundCinematicSkillPacket.ACTION_START,
                2.8f
        ));

        // 4. Đăng ký tiến trình dịch chuyển Ác Ma
        PENDING_DEMON_TELEPORTS.put(player.getUUID(), new PendingDemonTeleport(
                level, player.getUUID(), center, targetLevel, safeDestPos, destTitle, demonIds
        ));
    }

    /**
     * Gửi trực tiếp sinh vật khác hoặc vật phẩm/đối tượng (ItemEntity, ExperienceOrb, thuyền, xe mỏ, giá giáp, projectile...)
     * tới Ấn Ký Vĩnh Viễn mà người chơi KHÔNG cần di chuyển theo.
     *
     * @param anchorSlot -1 = dùng Ấn Ký Vĩnh Viễn đang chọn; >= 0 = dùng slot cụ thể (0..MAX_ANCHOR_MARKS-1)
     */
    public static void castSendToAnchor(ServerLevel level, ServerPlayer player, int anchorSlot) {
        if (level == null || player == null) return;

        long now = System.currentTimeMillis();
        if (now - LAST_CAST_TIME.getOrDefault(player.getUUID(), 0L) < 250) return;
        LAST_CAST_TIME.put(player.getUUID(), now);

        List<SpatialMark> anchors = getAnchorMarks(player);
        if (anchors.isEmpty()) {
            player.displayClientMessage(Component.literal("§c⚠ Bạn chưa đặt Ấn Ký Vĩnh Viễn nào! Hãy [Đặt Ấn Ký Vĩnh Viễn] trước."), true);
            return;
        }

        int resolvedSlot = (anchorSlot >= 0 && anchorSlot < anchors.size())
                ? anchorSlot
                : getSelectedAnchorIndex(player);
        if (anchorSlot >= 0 && anchorSlot < anchors.size()) {
            setSelectedAnchorIndex(player, resolvedSlot);
        }

        SpatialMark mark = anchors.get(resolvedSlot);
        ServerLevel targetLevel = (level.getServer() != null) ? level.getServer().getLevel(mark.dimension()) : null;
        if (targetLevel == null) targetLevel = level;

        List<Entity> targets = new ArrayList<>();
        Vec3 sourceCenter = player.position();

        // 1. Ưu tiên đối tượng vừa click hoặc đang ngắm trong 24m (gửi kèm nhóm sinh vật/vật phẩm xung quanh nó)
        Entity aimed = findClickedOrAimedTeleportable(level, player, 24.0D);
        if (aimed != null) {
            sourceCenter = aimed.position();
            targets.add(aimed);
            for (Entity nearAimed : collectTeleportableEntities(level, sourceCenter, 2.8D, 2.0D, 3.0D, player, false)) {
                if (!targets.contains(nearAimed) && targets.size() < 32) {
                    targets.add(nearAimed);
                }
            }
        } else {
            // 2. Nếu đang ngắm vào một block trong 16m, gom sinh vật/vật phẩm xung quanh điểm ngắm đó
            BlockHitResult blockHit = raycastBlock(level, player, 16.0D);
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = blockHit.getBlockPos();
                Vec3 hitCenter = new Vec3(hitPos.getX() + 0.5D, hitPos.getY() + 1.02D, hitPos.getZ() + 0.5D);
                List<Entity> nearBlock = collectTeleportableEntities(level, hitCenter, 3.0D, 2.0D, 3.5D, player, false);
                if (!nearBlock.isEmpty()) {
                    sourceCenter = hitCenter;
                    for (Entity e : nearBlock) {
                        if (targets.size() < 32) targets.add(e);
                    }
                }
            }
        }

        // 3. Nếu vẫn trống -> gom tất cả sinh vật & vật phẩm quanh người chơi trong bán kính 4.0m
        if (targets.isEmpty()) {
            sourceCenter = player.position();
            for (Entity nearPlayer : collectTeleportableEntities(level, sourceCenter, 4.0D, 2.0D, 3.5D, player, false)) {
                if (targets.size() < 32) targets.add(nearPlayer);
            }
        }

        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("§e⚠ Không thấy sinh vật hay vật phẩm nào theo hướng ngắm hoặc quanh bạn để gửi tới Ấn Ký!"), true);
            return;
        }

        Vec3 dest = mark.pos();
        BlockPos destBlock = BlockPos.containing(dest);
        targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(destBlock), 3, 200);
        targetLevel.getChunk(destBlock);

        // Khởi tạo Ma Pháp Trận 3D & tia sáng tại điểm đi để nhìn thấy phép dịch chuyển hoạt động (không khóa camera)
        spawnMagicCircleParticles(level, sourceCenter, 2.4D, 42);
        level.playSound(null, sourceCenter.x, sourceCenter.y, sourceCenter.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.2F, 1.3F);
        level.playSound(null, sourceCenter.x, sourceCenter.y, sourceCenter.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 2.0F, 1.15F);
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                player.getUUID(),
                sourceCenter,
                new Vec3(MODE_DEPART_PERSONAL, -1.0, 0.0), // sub = -1.0 -> vẽ Ma Pháp Trận gửi đi, KHÔNG khóa camera player
                36,
                ClientboundCinematicSkillPacket.ACTION_START,
                2.4f
        ));

        List<UUID> targetUuids = new ArrayList<>(targets.size());
        for (Entity e : targets) {
            targetUuids.add(e.getUUID());
        }
        PENDING_SENDS.add(new PendingSendToAnchor(
                level, player.getUUID(), targetUuids, sourceCenter, targetLevel, dest, resolvedSlot
        ));

        player.displayClientMessage(Component.literal(String.format(
                "§6§l✦ [GỬI TỚI ẤN KÝ #%d] §fĐang mở Ma Pháp Trận đưa §e%d mục tiêu §ftới [%.0f, %.0f, %.0f]...",
                resolvedSlot + 1, targetUuids.size(), dest.x, dest.y, dest.z
        )), true);
    }

    private static void executeSendToAnchorArrival(ServerLevel originLevel, PendingSendToAnchor send) {
        ServerLevel targetLevel = send.targetLevel;
        Vec3 dest = send.destPos;
        BlockPos destBlock = BlockPos.containing(dest);
        targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(destBlock), 3, 200);
        targetLevel.getChunk(destBlock);

        originLevel.playSound(null, send.sourceCenter.x, send.sourceCenter.y, send.sourceCenter.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.2F, 1.25F);
        originLevel.sendParticles(ParticleTypes.POOF, send.sourceCenter.x, send.sourceCenter.y + 0.6D, send.sourceCenter.z,
                20, 0.5D, 0.5D, 0.5D, 0.05D);

        List<Entity> aliveTargets = new ArrayList<>();
        for (UUID id : send.targetUuids) {
            Entity e = originLevel.getEntity(id);
            if (e != null && e.isAlive()) {
                aliveTargets.add(e);
            }
        }

        int livingCount = 0;
        int objectCount = 0;
        int total = aliveTargets.size();
        for (int i = 0; i < total; i++) {
            Entity ent = aliveTargets.get(i);
            Vec3 offsetPos = computeDistributedOffsetPos(dest, i, total);
            if (ent instanceof LivingEntity) livingCount++;
            else objectCount++;
            teleportAnyEntity(ent, originLevel, targetLevel, offsetPos, 0);
        }

        targetLevel.playSound(null, dest.x, dest.y, dest.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.4F, 1.35F);
        spawnMagicCircleParticles(targetLevel, dest, 2.6D, 45);
        ModMessages.sendToPlayers(targetLevel.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                send.casterUuid,
                dest,
                new Vec3(MODE_ARRIVAL_CINEMATIC, -1.0, 2.0), // dir.y = -1.0 (KHÔNG khóa camera player); dir.z = 2.0 (skipGroundCircle)
                50,
                ClientboundCinematicSkillPacket.ACTION_START,
                2.4f
        ));

        ServerPlayer caster = originLevel.getServer() != null
                ? originLevel.getServer().getPlayerList().getPlayer(send.casterUuid)
                : null;
        if (caster != null) {
            caster.displayClientMessage(Component.literal(String.format(
                    "§6§l✦ [GỬI TỚI ẤN KÝ #%d] §fĐã dịch chuyển xong §e%d sinh vật §f& §b%d vật phẩm/đối tượng §ftới [%.0f, %.0f, %.0f]!",
                    send.anchorSlot + 1, livingCount, objectCount, dest.x, dest.y, dest.z
            )), true);
        }
    }

    public static void castTeleportFromDemon(ServerLevel level, ServerPlayer owner, PrimordialDemonEntity demon) {
        if (level == null || owner == null || demon == null) return;
        if (PENDING_PERSONAL.containsKey(owner.getUUID()) || PENDING_DEMON_TELEPORTS.containsKey(owner.getUUID())) {
            return;
        }

        SpatialMark tactical = getMark(owner, false);
        SpatialMark mark = (tactical != null) ? tactical : getActiveAnchorMark(owner);
        ServerLevel targetLevel;
        BlockPos safeDestPos;
        Vec3 exactDest;
        float targetYaw;
        String destTitle;
        int envType;
        boolean consumeMark;

        if (mark != null) {
            ServerLevel markedLevel = (level.getServer() != null) ? level.getServer().getLevel(mark.dimension()) : null;
            targetLevel = (markedLevel != null) ? markedLevel : level;
            exactDest = mark.pos();
            safeDestPos = BlockPos.containing(exactDest);
            targetYaw = mark.yaw();
            consumeMark = (tactical != null);
            destTitle = consumeMark ? "Ấn Ký Chiến Thuật" : "Ấn Ký Vĩnh Viễn #" + (getSelectedAnchorIndex(owner) + 1);
            envType = 0;
        } else {
            RandomDestResult randomDest = resolveRandomDestination(level, owner);
            targetLevel = randomDest.level();
            safeDestPos = randomDest.safePos();
            exactDest = new Vec3(safeDestPos.getX() + 0.5D, safeDestPos.getY() + 0.05D, safeDestPos.getZ() + 0.5D);
            targetYaw = owner.getYRot();
            destTitle = randomDest.title();
            envType = randomDest.envType();
            consumeMark = false;
        }

        List<PrimordialDemonEntity> ownedDemons = level.getEntitiesOfClass(
                PrimordialDemonEntity.class,
                owner.getBoundingBox().inflate(32.0D),
                d -> d.isAlive() && d.isOwnedBy(owner) && (d == demon || (!d.isOrderedToSit() && d.getGuardCenterPos() == null))
        );
        List<UUID> demonIds = new ArrayList<>();
        demonIds.add(demon.getUUID());
        for (PrimordialDemonEntity d : ownedDemons) {
            if (!demonIds.contains(d.getUUID())) {
                demonIds.add(d.getUUID());
            }
        }

        targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(safeDestPos), 3, 150);

        Vec3 center = owner.position();
        removeCountdownCircle(owner.getUUID());
        spawnMagicCircleParticles(level, center, 2.8D, 50);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.25F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 2.2F, 1.1F);

        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                owner.getUUID(),
                center,
                new Vec3(MODE_DEPART_PERSONAL, 0.0, 0.0),
                44,
                ClientboundCinematicSkillPacket.ACTION_START,
                2.8f
        ));

        owner.displayClientMessage(
                Component.literal(consumeMark
                        ? "§b§l✦ [" + demon.getEffectiveDemonName().toUpperCase() + " - DỊCH CHUYỂN] §fĐang dịch chuyển tới Ấn Ký Đánh Dấu!"
                        : "§d§l✦ [" + demon.getEffectiveDemonName().toUpperCase() + " - DỊCH CHUYỂN] §fChưa có Đánh Dấu! Mở trận dịch chuyển tới: §e" + destTitle + "§f!"),
                true
        );

        PENDING_DEMON_TELEPORTS.put(owner.getUUID(), new PendingDemonTeleport(
                level, owner.getUUID(), center, targetLevel, safeDestPos, exactDest, targetYaw, destTitle, envType, consumeMark, demonIds
        ));
    }

    // =========================================================================
    // VÒNG LẶP CẬP NHẬT SERVER (TICK)
    // =========================================================================
    public static void tick(ServerLevel level) {
        // 1. Duy trì hạt ánh sáng nhẹ cho các điểm Đánh Dấu (Chiến Thuật & Vĩnh Viễn) trong chiều không gian này
        if (level.getGameTime() % 10 == 0) {
            for (SpatialMark mark : ACTIVE_MARKS.values()) {
                emitAmbientMarkParticles(level, mark);
            }
            for (List<SpatialMark> list : ANCHOR_MARKS.values()) {
                for (SpatialMark anchor : list) {
                    emitAmbientMarkParticles(level, anchor);
                }
            }
        }

        // 1b. Xử lý các luồng Gửi sinh vật/vật phẩm tới Ấn Ký Vĩnh Viễn (chờ Ma Pháp Trận hiện lên ~1.6s rồi mới dịch chuyển)
        if (!PENDING_SENDS.isEmpty()) {
            for (PendingSendToAnchor send : PENDING_SENDS) {
                if (send.originLevel != level) continue;
                send.ticksElapsed++;
                if (send.ticksElapsed % 4 == 0) {
                    spawnMagicCircleParticles(level, send.sourceCenter, 2.3D, 16);
                }
                if (send.ticksElapsed >= send.totalTicks) {
                    PENDING_SENDS.remove(send);
                    executeSendToAnchorArrival(level, send);
                }
            }
        }

        // 2. Xử lý Chế độ A: Dịch chuyển cá nhân + sinh vật & vật thể (Sau khi quay cảnh tạo Ma Pháp Trận + tia sáng bay lên)
        if (!PENDING_PERSONAL.isEmpty()) {
            Iterator<Map.Entry<UUID, PendingPersonalTeleport>> it = PENDING_PERSONAL.entrySet().iterator();
            while (it.hasNext()) {
                PendingPersonalTeleport pending = it.next().getValue();
                if (pending.originLevel != level) continue;

                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(pending.casterUuid);
                if (caster == null || !caster.isAlive()) {
                    it.remove();
                    continue;
                }

                pending.ticksElapsed++;

                // Tại tick 24: Người chơi biến mất trên Client (phát âm thanh teleport & bùng hạt poof)
                if (pending.ticksElapsed == 24) {
                    level.playSound(null, pending.originCenter.x, pending.originCenter.y, pending.originCenter.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.5F, 1.2F);
                    level.sendParticles(ParticleTypes.POOF, pending.originCenter.x, pending.originCenter.y + 1.0D, pending.originCenter.z, 20, 0.4, 0.6, 0.4, 0.05);
                }

                // Giữ người chơi và sinh vật ổn định trong tâm tia sáng đang bay lên
                if (pending.ticksElapsed % 4 == 0) {
                    spawnMagicCircleParticles(level, pending.originCenter, 2.6D, 18);
                }

                if (pending.ticksElapsed >= pending.totalCastTicks) {
                    it.remove();
                    executePersonalArrival(level, caster, pending);
                }
            }
        }

        // 2b. Xử lý Chế độ Ác Ma Thủy Tổ (Dịch chuyển điện ảnh sau 44 ticks)
        if (!PENDING_DEMON_TELEPORTS.isEmpty()) {
            Iterator<Map.Entry<UUID, PendingDemonTeleport>> it = PENDING_DEMON_TELEPORTS.entrySet().iterator();
            while (it.hasNext()) {
                PendingDemonTeleport pending = it.next().getValue();
                if (pending.originLevel != level) continue;

                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(pending.casterUuid);
                if (caster == null || !caster.isAlive()) {
                    it.remove();
                    continue;
                }

                pending.ticksElapsed++;

                // Tại tick 24: Người chơi và Ác Ma biến mất trên Client (phát âm thanh teleport & bùng hạt poof)
                if (pending.ticksElapsed == 24) {
                    level.playSound(null, pending.originCenter.x, pending.originCenter.y, pending.originCenter.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.5F, 1.2F);
                    level.sendParticles(ParticleTypes.POOF, pending.originCenter.x, pending.originCenter.y + 1.0D, pending.originCenter.z, 20, 0.4, 0.6, 0.4, 0.05);
                }

                if (pending.ticksElapsed % 4 == 0) {
                    spawnMagicCircleParticles(level, pending.originCenter, 2.8D, 18);
                }

                if (pending.ticksElapsed >= pending.totalCastTicks) {
                    it.remove();
                    executeDemonArrival(level, caster, pending);
                }
            }
        }

        // 3. Xử lý Chế độ B: Ma Pháp Trận cố định đếm ngược 4 - 3 - 2 - 1 khi người chơi đứng vào trong
        if (!ACTIVE_COUNTDOWN_CIRCLES.isEmpty()) {
            Iterator<Map.Entry<UUID, ActiveCountdownCircle>> it = ACTIVE_COUNTDOWN_CIRCLES.entrySet().iterator();
            while (it.hasNext()) {
                ActiveCountdownCircle circle = it.next().getValue();
                if (circle.level != level) continue;

                circle.ageTicks++;
                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(circle.casterUuid);
                if (caster == null || !caster.isAlive() || circle.ageTicks > 1200) {
                    stopCountdownCircleVisual(circle);
                    it.remove();
                    continue;
                }

                // Kiểm tra điểm Đánh Dấu (Chiến thuật hoặc Vĩnh Viễn) hoặc điểm ngẫu nhiên
                SpatialMark mark = getPreferredMark(caster);
                if (mark == null && circle.randomDest == null) {
                    circle.randomDest = resolveRandomDestination(level, caster);
                    if (circle.randomDest != null) {
                        circle.randomDest.level().getChunkSource().addRegionTicket(
                                TicketType.POST_TELEPORT,
                                new ChunkPos(circle.randomDest.safePos()),
                                3,
                                200
                        );
                    }
                }

                // Kiểm tra người chơi có đang đứng bên trong Ma Pháp Trận Cấp Cao hay không
                double dx = caster.getX() - circle.center.x;
                double dz = caster.getZ() - circle.center.z;
                double horizDist = Math.hypot(dx, dz);
                double dy = Math.abs(caster.getY() - circle.center.y);
                boolean isPlayerInside = (caster.level() == level && horizDist <= circle.radius && dy <= 2.8D);

                if (isPlayerInside) {
                    circle.insideCountdownTicks++;
                    int remainingTicks = Math.max(0, 80 - circle.insideCountdownTicks);
                    int currentSecond = (remainingTicks / 20) + 1; // 4, 3, 2, 1

                    if (currentSecond >= 1 && currentSecond <= 4 && currentSecond != circle.lastAnnouncedSecond) {
                        circle.lastAnnouncedSecond = currentSecond;

                        // Hiển thị đếm ngược 4 - 3 - 2 - 1 rõ ràng trên màn hình + âm thanh ma thuật
                        String color = (currentSecond == 1) ? "§e§l" : "§b§l";
                        caster.connection.send(new ClientboundSetTitleTextPacket(Component.literal(color + "✦ [ " + currentSecond + " ] ✦")));
                        caster.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§fĐang hội tụ tia sáng dịch chuyển toàn bộ sinh vật trong trận...")));
                        caster.displayClientMessage(Component.literal("§b§l[MA PHÁP TRẬN DỊCH CHUYỂN] §fĐếm ngược: " + color + currentSecond + " giây..."), true);

                        float pitch = 1.0F + (4 - currentSecond) * 0.22F;
                        level.playSound(null, circle.center.x, circle.center.y, circle.center.z,
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 2.5F, pitch);
                        level.playSound(null, circle.center.x, circle.center.y, circle.center.z,
                                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2.5F, pitch);

                        // Đồng bộ số đếm ngược xuống Client Renderer để hiển thị số 3D và tăng mật độ tia sáng bay lên
                        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                                caster.getUUID(),
                                circle.center,
                                new Vec3(MODE_FIXED_COUNTDOWN, currentSecond, 0.0),
                                1200 - circle.ageTicks,
                                ClientboundCinematicSkillPacket.ACTION_START,
                                circle.radius
                        ));
                    }

                    if (circle.insideCountdownTicks % 3 == 0) {
                        spawnMagicCircleParticles(level, circle.center, circle.radius, 24);
                    }

                    // Khi đếm ngược 4 - 3 - 2 - 1 hoàn tất (80 ticks) -> Dịch chuyển người chơi và toàn bộ sinh vật trong trận!
                    if (circle.insideCountdownTicks >= 80) {
                        stopCountdownCircleVisual(circle);
                        it.remove();
                        executeGroupArrival(level, caster, circle, mark);
                    }
                } else {
                    // Người chơi bước ra ngoài -> Reset bộ đếm về 0
                    if (circle.insideCountdownTicks > 0) {
                        circle.insideCountdownTicks = 0;
                        circle.lastAnnouncedSecond = -1;
                        caster.displayClientMessage(Component.literal("§e⚠️ Bạn đã bước ra khỏi Ma Pháp Trận! Hãy đứng vào trong để bắt đầu đếm ngược 4 - 3 - 2 - 1."), true);
                        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                                caster.getUUID(),
                                circle.center,
                                new Vec3(MODE_FIXED_COUNTDOWN, 0.0, 0.0),
                                1200 - circle.ageTicks,
                                ClientboundCinematicSkillPacket.ACTION_START,
                                circle.radius
                        ));
                    }
                }
            }
        }

        // 4. Xử lý chuỗi 6 góc máy điện ảnh tại điểm đến (Arrival Sequence) & xóa dấu sau khi hoàn tất
        if (!ACTIVE_ARRIVALS.isEmpty()) {
            Iterator<Map.Entry<UUID, ActiveArrivalSequence>> it = ACTIVE_ARRIVALS.entrySet().iterator();
            while (it.hasNext()) {
                ActiveArrivalSequence arr = it.next().getValue();
                if (arr.level != level) continue;

                arr.ticksElapsed++;
                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(arr.casterUuid);

                // Tại tick 32 (khi chuyển sang Góc 3: Hiện hình người chơi giữa tia sáng): phát âm thanh dịch chuyển & bùng hạt cyan
                if (arr.ticksElapsed == 32) {
                    level.playSound(null, arr.destPos.x, arr.destPos.y, arr.destPos.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.5F, 1.15F);
                    level.playSound(null, arr.destPos.x, arr.destPos.y, arr.destPos.z,
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.5F, 1.4F);
                    spawnMagicCircleParticles(level, arr.destPos, 3.0D, 65);
                }

                // Tại tick 70 (Góc 5): Xóa Ma Pháp Trận Đánh Dấu khi người chơi đã hiện diện hoàn tất
                if (arr.ticksElapsed == 70 && arr.consumeMarkOnFinish && caster != null) {
                    consumeAndClearMark(caster, level, arr.destPos);
                }

                if (arr.ticksElapsed >= arr.totalTicks) {
                    if (arr.consumeMarkOnFinish && caster != null) {
                        consumeAndClearMark(caster, level, arr.destPos);
                    }
                    it.remove();
                }
            }
        }
    }

    private static void executePersonalArrival(ServerLevel originLevel, ServerPlayer player, PendingPersonalTeleport pending) {
        ServerLevel targetLevel = pending.targetLevel;
        Vec3 dest = pending.targetDestPos;

        // Dừng hiệu ứng ở điểm đi
        ModMessages.sendToPlayers(originLevel.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                player.getUUID(),
                pending.originCenter,
                Vec3.ZERO,
                0,
                ClientboundCinematicSkillPacket.ACTION_STOP,
                1.0f
        ));

        // Dịch chuyển người chơi tới điểm đích
        player.teleportTo(targetLevel, dest.x, dest.y + 0.05D, dest.z, pending.targetYaw, player.getXRot());
        player.fallDistance = 0.0F;
        applyTeleportProtection(player, pending.envType);

        // Dịch chuyển toàn bộ sinh vật & vật phẩm/đối tượng đi kèm
        int movedTotal = pending.accompanyingUuids.size();
        for (int i = 0; i < movedTotal; i++) {
            Entity ent = originLevel.getEntity(pending.accompanyingUuids.get(i));
            if (ent != null && ent.isAlive()) {
                Vec3 targetPos = computeDistributedOffsetPos(dest, i, movedTotal);
                teleportAnyEntity(ent, originLevel, targetLevel, targetPos, pending.envType);
            }
        }

        if (pending.isRandom) {
            player.displayClientMessage(
                    Component.literal("§d§l✦ [DỊCH CHUYỂN HƯ KHÔNG] §fĐã dịch chuyển ngẫu nhiên tới: §e§l" + pending.destTitle),
                    true
            );
        }

        // Bắt đầu chuỗi 6 góc máy điện ảnh tại điểm đến & tiêu thụ dấu ấn (chỉ xóa Ấn Ký Chiến Thuật, giữ Ấn Ký Vĩnh Viễn)
        startArrivalSequence(targetLevel, player, dest, pending.consumeMark);
    }

    private static void executeDemonArrival(ServerLevel originLevel, ServerPlayer player, PendingDemonTeleport pending) {
        ServerLevel targetLevel = pending.targetLevel;
        BlockPos safeDestPos = pending.safeDestPos;
        Vec3 destVec = pending.exactDestPos != null ? pending.exactDestPos : Vec3.atBottomCenterOf(safeDestPos);
        float yaw = pending.exactDestPos != null ? pending.targetYaw : player.getYRot();

        // Dừng hiệu ứng ở điểm đi
        ModMessages.sendToPlayers(originLevel.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                player.getUUID(),
                pending.originCenter,
                Vec3.ZERO,
                0,
                ClientboundCinematicSkillPacket.ACTION_STOP,
                1.0f
        ));

        // Dịch chuyển người chơi tới điểm an toàn
        player.teleportTo(targetLevel, destVec.x, destVec.y + 0.05D, destVec.z, yaw, player.getXRot());
        player.fallDistance = 0.0F;
        applyTeleportProtection(player, pending.envType);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 240, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));

        // Dịch chuyển các Ác Ma Thủy Tổ đi cùng
        int count = 0;
        for (UUID demonId : pending.demonUuids) {
            Entity ent = originLevel.getEntity(demonId);
            if (ent instanceof PrimordialDemonEntity demon && demon.isAlive()) {
                double angle = (count * (Math.PI * 2.0 / Math.max(1, pending.demonUuids.size())));
                double offsetX = Math.cos(angle) * 2.2D;
                double offsetZ = Math.sin(angle) * 2.2D;
                Vec3 demonTarget = new Vec3(destVec.x + offsetX, destVec.y + 0.05D, destVec.z + offsetZ);

                if (targetLevel == originLevel) {
                    demon.teleportTo(demonTarget.x, demonTarget.y, demonTarget.z);
                    demon.fallDistance = 0.0F;
                } else {
                    Entity moved = demon.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(
                            targetLevel,
                            demonTarget,
                            Vec3.ZERO,
                            demon.getYRot(),
                            demon.getXRot(),
                            net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING
                    ));
                    if (moved != null) moved.fallDistance = 0.0F;
                }
                count++;
            }
        }

        player.displayClientMessage(
                Component.literal("§5§l✦ [ÁC MA THỦY TỔ DỊCH CHUYỂN] §fĐã cùng §d" + count + " Ác Ma §fhạ cánh tại: §e§l" + pending.destTitle),
                true
        );

        // Kích hoạt chuỗi 6 góc máy điện ảnh tại điểm đến (thấy Ma Pháp Trận trước -> tia sáng -> người chơi hiện hình -> trả góc máy)
        startArrivalSequence(targetLevel, player, destVec, pending.consumeMark);
    }

    private static void executeGroupArrival(ServerLevel originLevel, ServerPlayer player, ActiveCountdownCircle circle, SpatialMark mark) {
        ServerLevel targetLevel;
        Vec3 dest;
        float targetYaw;
        int envType = 0;
        boolean isRandom = false;
        String destTitle;

        if (mark != null) {
            targetLevel = originLevel.getServer().getLevel(mark.dimension());
            if (targetLevel == null) targetLevel = originLevel;
            dest = mark.pos();
            targetYaw = mark.yaw();
            destTitle = "Ấn Ký Đánh Dấu";
        } else if (circle.randomDest != null) {
            targetLevel = circle.randomDest.level();
            dest = Vec3.atBottomCenterOf(circle.randomDest.safePos());
            targetYaw = player.getYRot();
            envType = circle.randomDest.envType();
            isRandom = true;
            destTitle = circle.randomDest.title();
        } else {
            return;
        }

        // Thu thập TOÀN BỘ sinh vật sống VÀ vật phẩm/đối tượng đang đứng trong Ma Pháp Trận Cấp Cao
        List<Entity> entitiesInCircle = collectTeleportableEntities(
                originLevel, circle.center, circle.radius + 0.3D, 2.0D, 4.0D, player, true
        );

        // Dịch chuyển người chơi tới điểm đích
        player.teleportTo(targetLevel, dest.x, dest.y + 0.05D, dest.z, targetYaw, player.getXRot());
        player.fallDistance = 0.0F;
        applyTeleportProtection(player, envType);

        // Dịch chuyển toàn bộ sinh vật & vật phẩm đứng trong vòng trận (giữ khoảng cách tương đối quanh tâm đích)
        int livingCount = 0;
        int objectCount = 0;
        for (Entity ent : entitiesInCircle) {
            double relX = Math.clamp(ent.getX() - circle.center.x, -3.2D, 3.2D);
            double relZ = Math.clamp(ent.getZ() - circle.center.z, -3.2D, 3.2D);
            Vec3 targetPos = new Vec3(dest.x + relX, dest.y + 0.05D, dest.z + relZ);
            if (ent instanceof LivingEntity) livingCount++;
            else objectCount++;
            teleportAnyEntity(ent, originLevel, targetLevel, targetPos, envType);
        }

        String summary = (objectCount > 0)
                ? String.format("§a§l✔ [DỊCH CHUYỂN HOÀN TẤT] §fĐã đưa bạn cùng §e%d sinh vật §f& §b%d vật thể §ftới §6§l%s§f!", livingCount, objectCount, destTitle)
                : String.format("§a§l✔ [DỊCH CHUYỂN HOÀN TẤT] §fĐã dịch chuyển bạn cùng §e%d sinh vật §ftới §6§l%s§f!", livingCount, destTitle);
        player.displayClientMessage(Component.literal(summary), true);

        // Bắt đầu chuỗi 6 góc máy điện ảnh tại điểm đến & tiêu thụ dấu ấn (chỉ xóa Ấn Ký Chiến Thuật, giữ Ấn Ký Vĩnh Viễn)
        boolean shouldConsume = (mark != null) && !isAnchorMarkMatch(player, mark);
        startArrivalSequence(targetLevel, player, dest, shouldConsume);
    }

    public static void startArrivalSequence(ServerLevel targetLevel, ServerPlayer player, Vec3 destPos, boolean consumeMark) {
        ActiveArrivalSequence seq = new ActiveArrivalSequence(targetLevel, player.getUUID(), destPos, consumeMark);
        ACTIVE_ARRIVALS.put(player.getUUID(), seq);

        targetLevel.playSound(null, destPos.x, destPos.y, destPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.15F);
        targetLevel.playSound(null, destPos.x, destPos.y, destPos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.8F, 1.3F);
        spawnMagicCircleParticles(targetLevel, destPos, 3.0D, 55);

        // Gửi gói tin kích hoạt Chuỗi 6 Góc Máy Điện Ảnh Dịch Chuyển Tới Đích (100 ticks)
        // dir = (MODE_ARRIVAL_CINEMATIC, consumeMark ? 1.0 : 0.0, 0), powerMultiplier khớp bán kính Đánh Dấu (2.4f) khi tới Ấn Ký
        float circleRadius = consumeMark ? 2.4f : 3.0f;
        ModMessages.sendToPlayers(targetLevel.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                player.getUUID(),
                destPos,
                new Vec3(MODE_ARRIVAL_CINEMATIC, consumeMark ? 1.0 : 0.0, 0.0),
                100,
                ClientboundCinematicSkillPacket.ACTION_START,
                circleRadius
        ));
    }

    // =========================================================================
    // ĐẬP PHÁ MA PHÁP TRẬN ĐÁNH DẤU (PUNCH TO DESTROY)
    // =========================================================================
    public static boolean dispelByPunch(ServerPlayer player, BlockPos clickedPos) {
        if (player == null || clickedPos == null || !(player.level() instanceof ServerLevel level)) return false;
        Vec3 hitVec = Vec3.atCenterOf(clickedPos);
        return tryShatterAt(level, player, hitVec, 3.8D);
    }

    public static boolean checkEmptyHandPunch(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        BlockHitResult hit = raycastBlock(level, player, 6.0D);
        Vec3 checkPos = hit.getLocation();
        if (tryShatterAt(level, player, checkPos, 3.8D)) {
            return true;
        }
        return tryShatterAt(level, player, player.position(), 3.2D);
    }

    private static boolean tryShatterAt(ServerLevel level, ServerPlayer puncher, Vec3 pos, double maxDist) {
        double maxDistSq = maxDist * maxDist;

        // 1. Kiểm tra phá hủy Ma Pháp Trận Đánh Dấu (Chiến thuật & các Ấn Ký Vĩnh Viễn #1..#5)
        if (tryShatterTacticalMarks(level, puncher, pos, maxDistSq)
                || tryShatterAnchorMarks(level, puncher, pos, maxDistSq)) {
            return true;
        }

        // 2. Kiểm tra phá hủy Ma Pháp Trận Cấp Cao đang chờ đếm ngược
        for (Map.Entry<UUID, ActiveCountdownCircle> entry : ACTIVE_COUNTDOWN_CIRCLES.entrySet()) {
            ActiveCountdownCircle circle = entry.getValue();
            if (circle.level == level && circle.center.distanceToSqr(pos) <= (circle.radius + 1.0D) * (circle.radius + 1.0D)) {
                stopCountdownCircleVisual(circle);
                ACTIVE_COUNTDOWN_CIRCLES.remove(entry.getKey());
                shatterEffect(level, circle.center, circle.radius);
                puncher.displayClientMessage(Component.literal("§e💥 Đã đập hủy Ma Pháp Trận Dịch Chuyển Cố Định!"), true);
                return true;
            }
        }

        return false;
    }

    private static boolean tryShatterTacticalMarks(ServerLevel level, ServerPlayer puncher, Vec3 pos, double maxDistSq) {
        for (Map.Entry<UUID, SpatialMark> entry : ACTIVE_MARKS.entrySet()) {
            SpatialMark mark = entry.getValue();
            if (mark.dimension().equals(level.dimension()) && mark.pos().distanceToSqr(pos) <= maxDistSq) {
                ACTIVE_MARKS.remove(entry.getKey());
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (owner != null) {
                    clearTacticalMarkNbt(owner);
                    TensuraSkillHelper.syncSkillStateToClient(owner);
                }

                shatterEffect(level, mark.pos(), 2.6D);
                ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                        SkillVisualConfig.SKILL_SPATIAL_MARK,
                        entry.getKey(),
                        mark.pos(),
                        new Vec3(MODE_MARK_CAST, 0.0, 0.0),
                        0,
                        ClientboundCinematicSkillPacket.ACTION_STOP,
                        1.0f
                ));

                puncher.displayClientMessage(Component.literal("§e💥 Đã đập vỡ Ma Pháp Trận Đánh Dấu Chiến Thuật!"), true);
                return true;
            }
        }
        return false;
    }

    private static boolean tryShatterAnchorMarks(ServerLevel level, ServerPlayer puncher, Vec3 pos, double maxDistSq) {
        for (Map.Entry<UUID, List<SpatialMark>> entry : ANCHOR_MARKS.entrySet()) {
            List<SpatialMark> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                SpatialMark mark = list.get(i);
                if (mark.dimension().equals(level.dimension()) && mark.pos().distanceToSqr(pos) <= maxDistSq) {
                    list.remove(i);
                    shatterEffect(level, mark.pos(), 2.6D);
                    ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                            SkillVisualConfig.SKILL_SPATIAL_MARK,
                            entry.getKey(),
                            mark.pos(),
                            new Vec3(MODE_MARK_CAST, i + 1.0, 0.0),
                            0,
                            ClientboundCinematicSkillPacket.ACTION_STOP,
                            1.0f
                    ));

                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (owner != null) {
                        int newSel = list.isEmpty() ? 0 : Math.min(getSelectedAnchorIndex(owner), list.size() - 1);
                        saveAnchorMarks(owner, list, newSel);
                        syncVisualMarksToPlayer(owner);
                        TensuraSkillHelper.syncSkillStateToClient(owner);
                    }

                    puncher.displayClientMessage(Component.literal("§e💥 Đã đập vỡ Ấn Ký Vĩnh Viễn #" + (i + 1) + "!"), true);
                    return true;
                }
            }
        }
        return false;
    }

    private static void consumeAndClearMark(ServerPlayer player, ServerLevel level, Vec3 markPos) {
        SpatialMark existing = ACTIVE_MARKS.remove(player.getUUID());
        clearTacticalMarkNbt(player);
        TensuraSkillHelper.syncSkillStateToClient(player);
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_MARK,
                player.getUUID(),
                existing != null ? existing.pos() : markPos,
                Vec3.ZERO,
                0,
                ClientboundCinematicSkillPacket.ACTION_STOP,
                1.0f
        ));
    }

    private static void removeCountdownCircle(UUID playerUuid) {
        ActiveCountdownCircle old = ACTIVE_COUNTDOWN_CIRCLES.remove(playerUuid);
        if (old != null) {
            stopCountdownCircleVisual(old);
        }
    }

    private static void stopCountdownCircleVisual(ActiveCountdownCircle circle) {
        ModMessages.sendToPlayers(circle.level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                circle.casterUuid,
                circle.center,
                Vec3.ZERO,
                0,
                ClientboundCinematicSkillPacket.ACTION_STOP,
                1.0f
        ));
    }

    private static void shatterEffect(ServerLevel level, Vec3 pos, double radius) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5F, 0.85F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.0F, 1.3F);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.5D, pos.z, 45, radius * 0.5D, 0.6D, radius * 0.5D, 0.15D);
        level.sendParticles(CYAN_MAGIC_DUST, pos.x, pos.y + 0.2D, pos.z, 60, radius * 0.6D, 0.3D, radius * 0.6D, 0.1D);
    }

    public static void spawnMagicCircleParticles(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (i * Math.PI * 2.0) / count;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(CYAN_MAGIC_DUST, px, center.y + 0.08D, pz, 1, 0.05D, 0.2D, 0.05D, 0.02D);
            if (i % 3 == 0) {
                level.sendParticles(BLUE_MAGIC_DUST, px, center.y + 0.15D, pz, 1, 0.05D, 0.4D, 0.05D, 0.03D);
            }
            if (i % 5 == 0) {
                level.sendParticles(WHITE_LIGHT_DUST, px, center.y + 0.4D, pz, 1, 0.02D, 0.8D, 0.02D, 0.05D);
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.5D, center.z, 18, radius * 0.4D, 1.2D, radius * 0.4D, 0.04D);
    }

    // =========================================================================
    // LƯU TRỮ & ĐỌC DỮ LIỆU ĐÁNH DẤU + ĐA ẤN KÝ VĨNH VIỄN (NBT + MEMORY)
    // =========================================================================
    private static CompoundTag serializeMark(SpatialMark mark) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", mark.dimension().location().toString());
        tag.putDouble("X", mark.pos().x);
        tag.putDouble("Y", mark.pos().y);
        tag.putDouble("Z", mark.pos().z);
        tag.putFloat("Yaw", mark.yaw());
        tag.putLong("CreatedTick", mark.createdTick());
        return tag;
    }

    private static SpatialMark deserializeMark(UUID ownerUuid, CompoundTag tag) {
        String dimStr = tag.getString("Dimension");
        if (dimStr.isEmpty()) return null;
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimStr));
        Vec3 pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        return new SpatialMark(ownerUuid, dimKey, pos, tag.getFloat("Yaw"), tag.getLong("CreatedTick"));
    }

    private static void saveMark(ServerPlayer player, SpatialMark mark) {
        ACTIVE_MARKS.put(player.getUUID(), mark);
        EntityDataHelper.getCustomData(player).put("TensuraSpatialMark", serializeMark(mark));
    }

    private static void clearTacticalMarkNbt(ServerPlayer player) {
        EntityDataHelper.getCustomData(player).remove("TensuraSpatialMark");
    }

    private static void saveAnchorMarks(ServerPlayer player, List<SpatialMark> anchors, int selectedIndex) {
        List<SpatialMark> bounded = new ArrayList<>();
        for (int i = 0; i < Math.min(anchors.size(), MAX_ANCHOR_MARKS); i++) {
            bounded.add(anchors.get(i));
        }
        ANCHOR_MARKS.put(player.getUUID(), bounded);
        int clampedSel = bounded.isEmpty() ? 0 : Math.clamp(selectedIndex, 0, bounded.size() - 1);
        SELECTED_ANCHOR_INDEX.put(player.getUUID(), clampedSel);

        CompoundTag root = EntityDataHelper.getCustomData(player);
        ListTag listTag = new ListTag();
        for (SpatialMark m : bounded) {
            listTag.add(serializeMark(m));
        }
        root.put("TensuraAnchorMarks", listTag);
        root.putInt("TensuraSelectedAnchorIndex", clampedSel);
        root.remove("TensuraAnchorMark"); // Dọn tag đơn cũ nếu đã migrate
    }

    public static SpatialMark getMark(ServerPlayer player) {
        return getMark(player, false);
    }

    public static SpatialMark getMark(ServerPlayer player, boolean isAnchor) {
        if (player == null) return null;
        if (isAnchor) {
            return getActiveAnchorMark(player);
        }
        SpatialMark cached = ACTIVE_MARKS.get(player.getUUID());
        if (cached != null) return cached;

        CompoundTag root = EntityDataHelper.getCustomData(player);
        if (!root.contains("TensuraSpatialMark")) return null;
        SpatialMark restored = deserializeMark(player.getUUID(), root.getCompound("TensuraSpatialMark"));
        if (restored != null) {
            ACTIVE_MARKS.put(player.getUUID(), restored);
        }
        return restored;
    }

    public static List<SpatialMark> getAnchorMarks(ServerPlayer player) {
        if (player == null) return List.of();
        List<SpatialMark> cached = ANCHOR_MARKS.get(player.getUUID());
        if (cached != null) return cached;

        CompoundTag root = EntityDataHelper.getCustomData(player);
        List<SpatialMark> loaded = new ArrayList<>();
        if (root.contains("TensuraAnchorMarks", Tag.TAG_LIST)) {
            ListTag listTag = root.getList("TensuraAnchorMarks", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(listTag.size(), MAX_ANCHOR_MARKS); i++) {
                SpatialMark m = deserializeMark(player.getUUID(), listTag.getCompound(i));
                if (m != null) loaded.add(m);
            }
        } else if (root.contains("TensuraAnchorMark")) {
            // Tự động nâng cấp Ấn Ký Vĩnh Viễn cũ lên danh sách đa Ấn Ký
            SpatialMark legacy = deserializeMark(player.getUUID(), root.getCompound("TensuraAnchorMark"));
            if (legacy != null) loaded.add(legacy);
        }

        int sel = root.contains("TensuraSelectedAnchorIndex") ? root.getInt("TensuraSelectedAnchorIndex") : 0;
        if (!loaded.isEmpty()) {
            saveAnchorMarks(player, loaded, sel);
        } else {
            ANCHOR_MARKS.put(player.getUUID(), loaded);
            SELECTED_ANCHOR_INDEX.put(player.getUUID(), 0);
        }
        return ANCHOR_MARKS.get(player.getUUID());
    }

    public static SpatialMark getAnchorMark(ServerPlayer player, int index) {
        List<SpatialMark> anchors = getAnchorMarks(player);
        if (index < 0 || index >= anchors.size()) return null;
        return anchors.get(index);
    }

    public static SpatialMark getActiveAnchorMark(ServerPlayer player) {
        List<SpatialMark> anchors = getAnchorMarks(player);
        if (anchors.isEmpty()) return null;
        int sel = getSelectedAnchorIndex(player);
        return anchors.get(sel);
    }

    public static int getSelectedAnchorIndex(ServerPlayer player) {
        if (player == null) return 0;
        List<SpatialMark> anchors = getAnchorMarks(player);
        if (anchors.isEmpty()) return 0;
        int sel = SELECTED_ANCHOR_INDEX.getOrDefault(player.getUUID(),
                EntityDataHelper.getCustomData(player).getInt("TensuraSelectedAnchorIndex"));
        int clamped = Math.clamp(sel, 0, anchors.size() - 1);
        SELECTED_ANCHOR_INDEX.put(player.getUUID(), clamped);
        return clamped;
    }

    private static void setSelectedAnchorIndex(ServerPlayer player, int index) {
        List<SpatialMark> anchors = getAnchorMarks(player);
        if (anchors.isEmpty()) return;
        int clamped = Math.clamp(index, 0, anchors.size() - 1);
        SELECTED_ANCHOR_INDEX.put(player.getUUID(), clamped);
        EntityDataHelper.getCustomData(player).putInt("TensuraSelectedAnchorIndex", clamped);
    }

    public static void selectAnchorSlot(ServerPlayer player, int slotIndex) {
        if (player == null) return;
        List<SpatialMark> anchors = getAnchorMarks(player);
        if (slotIndex < 0 || slotIndex >= anchors.size()) {
            player.displayClientMessage(Component.literal("§c⚠ Ô Ấn Ký Vĩnh Viễn #" + (slotIndex + 1) + " đang trống!"), true);
            return;
        }
        setSelectedAnchorIndex(player, slotIndex);
        SpatialMark mark = anchors.get(slotIndex);
        player.displayClientMessage(Component.literal(String.format(
                "§6§l✦ [CHỌN ẤN KÝ #%d] §fĐang kích hoạt điểm neo tại §e[%.0f, %.0f, %.0f]",
                slotIndex + 1, mark.pos().x, mark.pos().y, mark.pos().z
        )), true);
    }

    public static void removeAnchorSlot(ServerPlayer player, int slotIndex) {
        if (player == null) return;
        List<SpatialMark> anchors = new ArrayList<>(getAnchorMarks(player));
        if (slotIndex < 0 || slotIndex >= anchors.size()) return;
        SpatialMark removed = anchors.remove(slotIndex);
        stopMarkVisual(player, removed, slotIndex + 1);
        int nextSel = anchors.isEmpty() ? 0 : Math.min(getSelectedAnchorIndex(player), anchors.size() - 1);
        saveAnchorMarks(player, anchors, nextSel);
        syncVisualMarksToPlayer(player);
        player.displayClientMessage(Component.literal("§e💥 Đã xóa Ấn Ký Vĩnh Viễn #" + (slotIndex + 1) + "!"), true);
    }

    public static List<String> getAnchorLabels(ServerPlayer player) {
        List<SpatialMark> anchors = getAnchorMarks(player);
        if (anchors.isEmpty()) return List.of();
        List<String> labels = new ArrayList<>(anchors.size());
        for (int i = 0; i < anchors.size(); i++) {
            SpatialMark m = anchors.get(i);
            String dimPath = m.dimension().location().getPath();
            String shortDim = switch (dimPath) {
                case "overworld" -> "OW";
                case "the_nether" -> "NTH";
                case "the_end" -> "END";
                default -> dimPath.length() > 4 ? dimPath.substring(0, 4).toUpperCase() : dimPath.toUpperCase();
            };
            labels.add(String.format("#%d %s (%.0f,%.0f,%.0f)", i + 1, shortDim, m.pos().x, m.pos().y, m.pos().z));
        }
        return labels;
    }

    public static void syncVisualMarksToPlayer(ServerPlayer player) {
        if (player == null) return;
        SpatialMark tactical = getMark(player, false);
        if (tactical != null && tactical.dimension().equals(player.level().dimension())) {
            ModMessages.sendToPlayer(new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_SPATIAL_MARK,
                    player.getUUID(),
                    tactical.pos(),
                    new Vec3(MODE_MARK_CAST, 0.0, 1.0), // dir.z = 1.0 -> đồng bộ ngầm, không chiếu cutscene
                    72000,
                    ClientboundCinematicSkillPacket.ACTION_START,
                    2.4f
            ), player);
        }
        List<SpatialMark> anchors = getAnchorMarks(player);
        for (int i = 0; i < anchors.size(); i++) {
            SpatialMark a = anchors.get(i);
            if (a.dimension().equals(player.level().dimension())) {
                ModMessages.sendToPlayer(new ClientboundCinematicSkillPacket(
                        SkillVisualConfig.SKILL_SPATIAL_MARK,
                        player.getUUID(),
                        a.pos(),
                        new Vec3(MODE_MARK_CAST, i + 1.0, 1.0), // dir.z = 1.0 -> đồng bộ ngầm, không chiếu cutscene
                        72000,
                        ClientboundCinematicSkillPacket.ACTION_START,
                        2.4f
                ), player);
            }
        }
    }

    private static void stopMarkVisual(ServerPlayer owner, SpatialMark oldMark, int visualSlot) {
        if (oldMark == null || owner == null || owner.getServer() == null) return;
        ServerLevel oldLevel = owner.getServer().getLevel(oldMark.dimension());
        if (oldLevel != null) {
            ModMessages.sendToPlayers(oldLevel.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_SPATIAL_MARK, owner.getUUID(), oldMark.pos(),
                    new Vec3(MODE_MARK_CAST, visualSlot, 0.0),
                    0, ClientboundCinematicSkillPacket.ACTION_STOP, 1.0f
            ));
        }
    }

    public static SpatialMark getPreferredMark(ServerPlayer player) {
        SpatialMark tactical = getMark(player, false);
        return (tactical != null) ? tactical : getActiveAnchorMark(player);
    }

    public static boolean hasTacticalMark(ServerPlayer player) {
        return getMark(player, false) != null;
    }

    public static boolean hasAnchorMark(ServerPlayer player) {
        return !getAnchorMarks(player).isEmpty();
    }

    private static boolean isAnchorMarkMatch(ServerPlayer player, SpatialMark mark) {
        if (player == null || mark == null) return false;
        for (SpatialMark anchor : getAnchorMarks(player)) {
            if (anchor == mark || (anchor.dimension().equals(mark.dimension()) && anchor.pos().distanceToSqr(mark.pos()) < 0.01)) {
                return true;
            }
        }
        return false;
    }

    private static void emitAmbientMarkParticles(ServerLevel level, SpatialMark mark) {
        if (!mark.dimension().equals(level.dimension())) return;
        Vec3 p = mark.pos();
        if (!level.hasChunk((int) p.x >> 4, (int) p.z >> 4)) return;
        for (int i = 0; i < 4; i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double r = 2.2D * Math.sqrt(level.random.nextDouble());
            level.sendParticles(CYAN_MAGIC_DUST, p.x + Math.cos(a) * r, p.y + 0.1D, p.z + Math.sin(a) * r, 1, 0.0, 0.15, 0.0, 0.02);
        }
        if (level.getGameTime() % 20 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.2D, p.z, 3, 1.2, 0.8, 1.2, 0.02);
        }
    }

    // =========================================================================
    // THU THẬP & DỊCH CHUYỂN SINH VẬT + VẬT PHẨM / ĐỐI TƯỢNG KHÔNG GIAN
    // =========================================================================
    private static boolean isTeleportableNonLiving(Entity e) {
        return e != null && e.isAlive()
                && (e instanceof ItemEntity
                || e instanceof ExperienceOrb
                || e instanceof VehicleEntity
                || e instanceof ArmorStand
                || e instanceof Projectile);
    }

    private static boolean isTeleportableEntity(Entity e, ServerPlayer excludedCaster) {
        if (e == null || !e.isAlive() || e == excludedCaster || e.isSpectator()) return false;
        if (e instanceof LivingEntity) return true;
        return isTeleportableNonLiving(e);
    }

    private static List<Entity> collectTeleportableEntities(ServerLevel level, Vec3 center, double radius,
                                                            double heightDown, double heightUp,
                                                            ServerPlayer excludedCaster, boolean circular) {
        AABB box = new AABB(
                center.x - radius, center.y - heightDown, center.z - radius,
                center.x + radius, center.y + heightUp, center.z + radius
        );
        List<Entity> found = level.getEntities(excludedCaster, box, e -> {
            if (!isTeleportableEntity(e, excludedCaster)) return false;
            return !circular || Math.hypot(e.getX() - center.x, e.getZ() - center.z) <= radius;
        });
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return found;
    }

    private static Vec3 computeDistributedOffsetPos(Vec3 dest, int index, int total) {
        if (total <= 1 || index == 0) {
            return new Vec3(dest.x + 0.9D, dest.y + 0.08D, dest.z + 0.5D);
        }
        double angle = (index * Math.PI * 2.0D) / Math.max(1, total);
        double ringRadius = 1.0D + Math.min(2.2D, (total / 8.0D) * 0.6D);
        return new Vec3(
                dest.x + Math.cos(angle) * ringRadius,
                dest.y + 0.1D,
                dest.z + Math.sin(angle) * ringRadius
        );
    }

    private static void teleportAnyEntity(Entity entity, ServerLevel originLevel, ServerLevel targetLevel,
                                          Vec3 targetPos, int envType) {
        if (entity == null || !entity.isAlive()) return;
        BlockPos destBlock = BlockPos.containing(targetPos);
        targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(destBlock), 3, 200);
        targetLevel.getChunk(destBlock);

        if (entity.isPassenger()) {
            entity.stopRiding();
        }
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().stop();
        }

        Entity movedEntity = entity;
        if (targetLevel == originLevel) {
            entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            entity.fallDistance = 0.0F;
        } else if (entity instanceof ServerPlayer otherPlayer) {
            otherPlayer.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, otherPlayer.getYRot(), otherPlayer.getXRot());
            otherPlayer.fallDistance = 0.0F;
        } else {
            movedEntity = entity.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(
                    targetLevel,
                    targetPos,
                    Vec3.ZERO,
                    entity.getYRot(),
                    entity.getXRot(),
                    net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING
            ));
            if (movedEntity != null) {
                movedEntity.fallDistance = 0.0F;
            }
        }
        if (movedEntity instanceof LivingEntity le) {
            applyTeleportProtection(le, envType);
        }
    }

    // =========================================================================
    // RAYCAST TIỆN ÍCH (TÌM BLOCK, SINH VẬT & VẬT THỂ)
    // =========================================================================
    private static Vec3 resolveMarkPlacementPos(ServerLevel level, ServerPlayer player, double maxDist) {
        BlockHitResult hit = raycastBlock(level, player, maxDist);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos bpos = hit.getBlockPos();
            return new Vec3(bpos.getX() + 0.5D, bpos.getY() + 1.02D, bpos.getZ() + 0.5D);
        }
        return new Vec3(player.getX(), player.getY() + 0.02D, player.getZ());
    }

    private static BlockHitResult raycastBlock(ServerLevel level, ServerPlayer player, double maxDist) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(maxDist));
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    private static Entity findClickedOrAimedTeleportable(ServerLevel level, ServerPlayer player, double maxDist) {
        ClickedEntityRecord rec = RECENT_CLICKED_ENTITIES.remove(player.getUUID());
        if (rec != null && Math.abs(level.getGameTime() - rec.gameTick()) <= 8) {
            Entity clicked = level.getEntity(rec.entityUuid());
            if (isTeleportableEntity(clicked, player)) {
                return clicked;
            }
        }

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(maxDist));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.5D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, player, start, end, searchBox,
                e -> isTeleportableEntity(e, player),
                0.55F
        );
        return entityHit != null ? entityHit.getEntity() : null;
    }

    private static LivingEntity findClickedOrAimedCreature(ServerLevel level, ServerPlayer player, double maxDist) {
        Entity hit = findClickedOrAimedTeleportable(level, player, maxDist);
        return hit instanceof LivingEntity le ? le : null;
    }
}
