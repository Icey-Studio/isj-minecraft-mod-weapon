package com.minhphuc.weapons.client;

import com.minhphuc.weapons.entity.tensura.DemonType;

import java.util.*;

/**
 * Lưu trữ trạng thái đồng bộ kỹ năng phía Client để hiển thị Mini-HUD và Bánh Xe Kỹ Năng (SkillWheelScreen).
 */
public class ClientSkillStateHandler {

    private static boolean safeMode = false;
    private static boolean hasTacticalMark = false;
    private static boolean hasAnchorMark = false;
    private static boolean isTrueDemonLord = false;
    private static boolean hasCreation = false;
    private static boolean isPrimordial = false;
    private static int demonTypeOrd = 0;
    private static int lordSelectedIndex = 0;
    private static String currentSkillName = "";
    private static int selectedAnchorIndex = 0;
    private static final List<String> anchorLabels = new ArrayList<>();
    private static final List<Integer> favoriteSkillIds = new ArrayList<>();
    private static final Map<String, Long> cooldownEndsMs = new HashMap<>();

    public static void handleSync(
            boolean safeModeIn,
            boolean hasTacticalIn,
            boolean hasAnchorIn,
            boolean isTrueDemonLordIn,
            boolean hasCreationIn,
            boolean isPrimordialIn,
            int demonTypeOrdIn,
            int lordSelectedIndexIn,
            String currentSkillNameIn,
            int selectedAnchorIndexIn,
            List<String> anchorLabelsIn,
            List<Integer> favoritesIn,
            Map<String, Integer> activeCooldownTicksIn
    ) {
        safeMode = safeModeIn;
        hasTacticalMark = hasTacticalIn;
        hasAnchorMark = hasAnchorIn;
        isTrueDemonLord = isTrueDemonLordIn;
        hasCreation = hasCreationIn;
        isPrimordial = isPrimordialIn;
        demonTypeOrd = demonTypeOrdIn;
        lordSelectedIndex = lordSelectedIndexIn;
        currentSkillName = currentSkillNameIn != null ? currentSkillNameIn : "";
        selectedAnchorIndex = Math.max(0, selectedAnchorIndexIn);

        anchorLabels.clear();
        if (anchorLabelsIn != null) {
            anchorLabels.addAll(anchorLabelsIn);
        }

        favoriteSkillIds.clear();
        if (favoritesIn != null) {
            favoriteSkillIds.addAll(favoritesIn);
        }

        cooldownEndsMs.clear();
        long nowMs = System.currentTimeMillis();
        if (activeCooldownTicksIn != null) {
            for (Map.Entry<String, Integer> entry : activeCooldownTicksIn.entrySet()) {
                cooldownEndsMs.put(entry.getKey(), nowMs + entry.getValue() * 50L);
            }
        }
    }

    public static boolean isSafeMode() {
        return safeMode;
    }

    public static boolean hasTacticalMark() {
        return hasTacticalMark;
    }

    public static boolean hasAnchorMark() {
        return !anchorLabels.isEmpty() || hasAnchorMark;
    }

    public static int getSelectedAnchorIndex() {
        return selectedAnchorIndex;
    }

    public static List<String> getAnchorLabels() {
        return Collections.unmodifiableList(anchorLabels);
    }

    public static boolean isTrueDemonLord() {
        return isTrueDemonLord;
    }

    public static boolean hasCreation() {
        return hasCreation;
    }

    public static boolean isPrimordial() {
        return isPrimordial;
    }

    public static DemonType getDemonType() {
        return DemonType.byIndex(demonTypeOrd);
    }

    public static int getLordSelectedIndex() {
        return lordSelectedIndex;
    }

    public static String getCurrentSkillName() {
        return currentSkillName;
    }

    public static List<Integer> getFavoriteSkillIds() {
        return Collections.unmodifiableList(favoriteSkillIds);
    }

    public static int getRemainingCooldownTicks(String key) {
        if (key == null) return 0;
        Long end = cooldownEndsMs.get(key);
        if (end == null) return 0;
        long remMs = end - System.currentTimeMillis();
        if (remMs <= 0) {
            cooldownEndsMs.remove(key);
            return 0;
        }
        return (int) Math.ceil(remMs / 50.0D);
    }
}
