package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trung tâm tiện ích dùng chung cho toàn bộ hệ thống kỹ năng Tensura & Thủy Tổ:
 * 1. Bảo vệ địa hình (Safe Mode / No-Grief): Bật/tắt phá block cho cả người chơi và Ác Ma Khế Ước.
 * 2. Lọc mục tiêu hợp lệ & chống đánh nhầm đồng minh (Allies / Contracted Demons).
 * 3. Quản lý Cooldown theo từng kỹ năng và đồng bộ hiển thị.
 * 4. Lưu dấu mục tiêu Cảm Nhận Vạn Năng & cửa sổ Cộng Hưởng Kỹ Năng (Combo / Twin Cast).
 */
public class TensuraSkillHelper {

    public static final String NBT_SAFE_MODE = "TensuraSafeMode";
    public static final String NBT_FAVORITE_SKILLS = "TensuraFavoriteSkills";
    public static final String NBT_POSSESSION_FAVORITE_SKILLS = "PossessionFavoriteSkills";

    // Map<PlayerUUID, Map<SkillKey, ReadyAtGameTime>>
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    // Map<CasterUUID, Map<TargetUUID, ExpiresAtGameTime>>
    private static final Map<UUID, Map<UUID, Long>> SENSED_TARGETS = new ConcurrentHashMap<>();

    // Map<CasterUUID, SenseActiveUntilGameTime>
    private static final Map<UUID, Long> UNIVERSAL_SENSE_ACTIVE_UNTIL = new ConcurrentHashMap<>();

    // Map<OwnerUUID, Map<ComboTag, ExpiresAtGameTime>>
    private static final Map<UUID, Map<String, Long>> DEMON_COMBO_WINDOWS = new ConcurrentHashMap<>();

    // =========================================================================
    // 1. CHẾ ĐỘ BẢO VỆ ĐỊA HÌNH (SAFE MODE / NO-GRIEF)
    // =========================================================================
    public static boolean isSafeMode(Entity entity) {
        if (entity instanceof Player p) {
            return EntityDataHelper.getCustomData(p).getBoolean(NBT_SAFE_MODE);
        }
        if (entity instanceof PrimordialDemonEntity demon) {
            LivingEntity owner = demon.getOwner();
            if (owner != null) {
                return EntityDataHelper.getCustomData(owner).getBoolean(NBT_SAFE_MODE);
            }
        }
        return false;
    }

    public static void setSafeMode(ServerPlayer player, boolean enabled) {
        if (player == null) return;
        EntityDataHelper.getCustomData(player).putBoolean(NBT_SAFE_MODE, enabled);
    }

    public static boolean toggleSafeMode(ServerPlayer player) {
        boolean next = !isSafeMode(player);
        setSafeMode(player, next);
        player.displayClientMessage(
                Component.literal(next
                        ? "§a§l🛡 [BẢO VỆ ĐỊA HÌNH: BẬT] §fCác kỹ năng sẽ không phá hủy block xung quanh!"
                        : "§c§l💥 [BẢO VỆ ĐỊA HÌNH: TẮT] §fCác kỹ năng hủy diệt được phép phá vỡ địa hình!"),
                true
        );
        return next;
    }

    public static boolean canDestroyTerrain(Level level, Entity caster) {
        if (level == null) return false;
        return !isSafeMode(caster);
    }

    // =========================================================================
    // 2. LỌC MỤC TIÊU HỢP LỆ & CHỐNG ĐÁNH NHẦM ĐỒNG MINH
    // =========================================================================
    public static boolean isAllied(Entity caster, Entity target) {
        if (caster == null || target == null) return false;
        if (caster == target) return true;
        if (caster.isAlliedTo(target)) return true;

        UUID ownerUuid = resolveOwnerUuid(caster);
        if (ownerUuid != null) {
            if (ownerUuid.equals(target.getUUID())) return true;
            if (PrimordialDemonEntity.isContractedAllyOfPlayer(ownerUuid, target)) return true;
            if (target instanceof OwnableEntity ownable && ownerUuid.equals(ownable.getOwnerUUID())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidSkillTarget(Entity caster, Entity target) {
        if (!(target instanceof LivingEntity le) || !le.isAlive()) return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return !isAllied(caster, target);
    }

    private static UUID resolveOwnerUuid(Entity entity) {
        if (entity instanceof Player p) return p.getUUID();
        if (entity instanceof OwnableEntity ownable) return ownable.getOwnerUUID();
        return null;
    }

    // =========================================================================
    // 3. QUẢN LÝ COOLDOWN KỸ NĂNG
    // =========================================================================
    public static boolean checkAndStartCooldown(ServerPlayer player, String skillKey, int cooldownTicks, String displayName) {
        if (player == null) return false;
        if (player.isCreative() || cooldownTicks <= 0) {
            return true;
        }
        long now = player.level().getGameTime();
        Map<String, Long> map = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        long readyAt = map.getOrDefault(skillKey, 0L);
        if (now < readyAt) {
            double remainingSec = (readyAt - now) / 20.0D;
            player.displayClientMessage(
                    Component.literal(String.format("§c⏳ [%s] §fĐang hồi chiêu: §e%.1fs§f!", displayName, remainingSec)),
                    true
            );
            return false;
        }
        map.put(skillKey, now + cooldownTicks);
        return true;
    }

    public static int getRemainingCooldownTicks(ServerPlayer player, String skillKey) {
        if (player == null || player.isCreative()) return 0;
        Map<String, Long> map = COOLDOWNS.get(player.getUUID());
        if (map == null) return 0;
        long now = player.level().getGameTime();
        long readyAt = map.getOrDefault(skillKey, 0L);
        return (int) Math.max(0L, readyAt - now);
    }

    public static Map<String, Integer> getActiveCooldownTicks(ServerPlayer player) {
        Map<String, Integer> result = new HashMap<>();
        if (player == null || player.isCreative()) return result;
        Map<String, Long> map = COOLDOWNS.get(player.getUUID());
        if (map == null || map.isEmpty()) return result;
        long now = player.level().getGameTime();
        map.entrySet().removeIf(entry -> entry.getValue() <= now);
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            int rem = (int) (entry.getValue() - now);
            if (rem > 0) {
                result.put(entry.getKey(), rem);
            }
        }
        return result;
    }

    // =========================================================================
    // 4. CẢM NHẬN VẠN NĂNG & CỘNG HƯỞNG KỸ NĂNG (SYNERGY)
    // =========================================================================
    public static void activateUniversalSenseWindow(UUID casterUuid, long now, int durationTicks) {
        if (casterUuid == null) return;
        UNIVERSAL_SENSE_ACTIVE_UNTIL.put(casterUuid, now + durationTicks);
    }

    public static boolean isUniversalSenseActive(UUID casterUuid, long now) {
        if (casterUuid == null) return false;
        return UNIVERSAL_SENSE_ACTIVE_UNTIL.getOrDefault(casterUuid, 0L) > now;
    }

    public static void markSensedTarget(UUID casterUuid, UUID targetUuid, long expiresAtTick) {
        if (casterUuid == null || targetUuid == null) return;
        SENSED_TARGETS.computeIfAbsent(casterUuid, k -> new ConcurrentHashMap<>())
                .put(targetUuid, expiresAtTick);
    }

    public static boolean isTargetSensedBy(UUID casterUuid, LivingEntity target, long now) {
        if (casterUuid == null || target == null) return false;
        Map<UUID, Long> map = SENSED_TARGETS.get(casterUuid);
        if (map == null) return false;
        Long exp = map.get(target.getUUID());
        if (exp == null) return false;
        if (exp <= now) {
            map.remove(target.getUUID());
            return false;
        }
        return true;
    }

    public static List<UUID> getSensedTargetIds(UUID casterUuid, long now) {
        Map<UUID, Long> map = SENSED_TARGETS.get(casterUuid);
        if (map == null || map.isEmpty()) return Collections.emptyList();
        map.entrySet().removeIf(e -> e.getValue() <= now);
        return new ArrayList<>(map.keySet());
    }

    public static void recordDemonResonance(UUID ownerUuid, String skillTag, long now, int windowTicks) {
        if (ownerUuid == null || skillTag == null) return;
        DEMON_COMBO_WINDOWS.computeIfAbsent(ownerUuid, k -> new ConcurrentHashMap<>())
                .put(skillTag, now + windowTicks);
    }

    public static boolean consumeDemonResonance(ServerPlayer owner, String skillTag, long now) {
        if (owner == null || skillTag == null) return false;
        Map<String, Long> map = DEMON_COMBO_WINDOWS.get(owner.getUUID());
        if (map == null) return false;
        Long exp = map.remove(skillTag);
        return exp != null && exp > now;
    }

    // =========================================================================
    // 5. LƯU TRỮ 4 SLOT YÊU THÍCH
    // =========================================================================
    public static List<Integer> getFavoriteSkills(ServerPlayer player, boolean possessionMode) {
        List<Integer> list = new ArrayList<>();
        if (player == null) return list;
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        String key = possessionMode ? NBT_POSSESSION_FAVORITE_SKILLS : NBT_FAVORITE_SKILLS;
        if (!tag.contains(key)) return list;
        int[] arr = tag.getIntArray(key);
        for (int v : arr) {
            if (!list.contains(v)) {
                list.add(v);
            }
        }
        return list;
    }

    public static void toggleFavoriteSkill(ServerPlayer player, boolean possessionMode, int skillId) {
        if (player == null) return;
        List<Integer> list = getFavoriteSkills(player, possessionMode);
        if (list.contains(skillId)) {
            list.remove(Integer.valueOf(skillId));
        } else {
            if (list.size() >= 4) {
                list.remove(0);
            }
            list.add(skillId);
        }
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        String key = possessionMode ? NBT_POSSESSION_FAVORITE_SKILLS : NBT_FAVORITE_SKILLS;
        tag.putIntArray(key, list.stream().mapToInt(Integer::intValue).toArray());
    }

    public static void syncSkillStateToClient(ServerPlayer player) {
        if (player == null) return;
        CompoundTag root = EntityDataHelper.getCustomData(player);
        boolean possessing = PrimordialPossessionManager.isPossessing(player);
        boolean safeMode = isSafeMode(player);
        boolean hasTactical = SpatialTeleportAbility.hasTacticalMark(player);
        boolean hasAnchor = SpatialTeleportAbility.hasAnchorMark(player);
        boolean isTrueDemonLord = root.getBoolean("TensuraTrueDemonLord");
        boolean hasCreation = root.getBoolean("TensuraMaterialCreation");
        boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
        com.minhphuc.weapons.entity.tensura.DemonType type = possessing
                ? PrimordialPossessionManager.getPossessedType(player)
                : PrimordialPlayerDataHelper.getPrimordialType(player);
        int demonOrd = type != null ? type.ordinal() : 0;
        int lordIdx = root.getInt("TensuraDemonLordSkill");

        String currentName = "";
        if (possessing) {
            currentName = PrimordialPossessionManager.getSkillDisplayName(player, PrimordialPossessionManager.getSelectedSkillIndex(player));
        } else if (isTrueDemonLord || isPrimordial || com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player) || player.isCreative()) {
            int lordCount = TensuraSkillDispatcher.getSkillCount(hasCreation);
            if (lordIdx < lordCount) {
                currentName = TensuraSkillDispatcher.getSkillName(player, lordIdx, hasCreation);
            } else if (isPrimordial) {
                currentName = PrimordialPlayerDataHelper.getSkillName(type, lordIdx - lordCount);
            }
        }

        int selectedAnchor = SpatialTeleportAbility.getSelectedAnchorIndex(player);
        List<String> anchorLabels = SpatialTeleportAbility.getAnchorLabels(player);
        List<Integer> favorites = getFavoriteSkills(player, possessing);
        Map<String, Integer> cooldowns = getActiveCooldownTicks(player);

        com.minhphuc.weapons.network.ModMessages.sendToPlayer(
                new com.minhphuc.weapons.network.ClientboundSyncSkillStatePacket(
                        safeMode, hasTactical, hasAnchor, isTrueDemonLord, hasCreation, isPrimordial,
                        demonOrd, lordIdx, currentName, selectedAnchor, anchorLabels, favorites, cooldowns
                ),
                player
        );
    }
}
