package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Hệ thống Ma Tố (Magicules / MP) & Chỉ Số Tồn Tại (EP - Existence Value) cho người chơi.
 * Quyền năng đặc biệt: Người chơi không bị khóa hồi chiêu (0s Cooldown) và Ma Tố tái sinh liên tục;
 * có thể gia tăng vĩnh viễn giới hạn Ma Tố & EP thông qua Dạ Dày Vô Hạn Azathoth và thức tỉnh.
 */
public final class MagiculesManager {

    private static final String KEY_MP = "TensuraMagicules";
    private static final String KEY_MP_INIT = "TensuraMagiculesInit";
    private static final String KEY_BONUS_MAX_MP = "TensuraBonusMaxMp";
    private static final String KEY_BONUS_EP = "TensuraBonusEp";

    private MagiculesManager() {}

    public static long getMaxMagicules(ServerPlayer player) {
        if (player == null) return 100_000L;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long maxMp = 100_000L;
        if (data.getBoolean("TensuraTrueDemonLord")) maxMp += 5_000_000L;
        if (data.getBoolean("TensuraMaterialCreation")) maxMp += 2_500_000L;
        if (PrimordialPlayerDataHelper.isPrimordial(player)) maxMp += 8_000_000L;
        if (PrimordialPossessionManager.isPossessing(player)) maxMp += 6_000_000L;

        int divinePieces = countDivineArmorPieces(player);
        maxMp += divinePieces * 350_000L;
        if (isHoldingMoonlightSword(player)) maxMp += 500_000L;

        maxMp += Math.max(0L, data.getLong(KEY_BONUS_MAX_MP));
        return Math.max(100_000L, maxMp);
    }

    public static long getCurrentMagicules(ServerPlayer player) {
        if (player == null) return 100_000L;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long maxMp = getMaxMagicules(player);
        if (!data.getBoolean(KEY_MP_INIT)) {
            data.putBoolean(KEY_MP_INIT, true);
            data.putLong(KEY_MP, maxMp);
            return maxMp;
        }
        long cur = Math.max(0L, data.getLong(KEY_MP));
        if (cur > maxMp) {
            cur = maxMp;
            data.putLong(KEY_MP, cur);
        }
        return cur;
    }

    public static long getTotalEp(ServerPlayer player) {
        if (player == null) return 100_000L;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long ep = getMaxMagicules(player);
        ep += Math.round(player.getMaxHealth() * 10_000.0D);
        ep += player.getArmorValue() * 25_000L;
        ep += countDivineArmorPieces(player) * 2_500_000L;
        if (isHoldingMoonlightSword(player)) ep += 3_000_000L;
        ep += Math.max(0L, data.getLong(KEY_BONUS_EP));
        return Math.max(100_000L, ep);
    }

    public static void onSkillCastPulse(ServerPlayer player, long visualCost) {
        if (player == null || player.level().isClientSide()) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long maxMp = getMaxMagicules(player);
        long cur = getCurrentMagicules(player);
        // Phản hồi trực quan nhẹ trên thanh Ma Tố (không bao giờ chặn thi triển chiêu thức!)
        long drain = Math.max(500L, Math.min(visualCost, maxMp / 40L));
        long next = Math.max(0L, cur - drain);
        data.putLong(KEY_MP, next);
    }

    public static void addAzathothConversion(ServerPlayer player, long sourceEp) {
        if (player == null) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long addBonusMax = Math.max(2_500L, sourceEp / 80L);
        long addBonusEp = Math.max(5_000L, sourceEp / 40L);
        data.putLong(KEY_BONUS_MAX_MP, data.getLong(KEY_BONUS_MAX_MP) + addBonusMax);
        data.putLong(KEY_BONUS_EP, data.getLong(KEY_BONUS_EP) + addBonusEp);

        long maxMp = getMaxMagicules(player);
        long restore = Math.max(20_000L, sourceEp / 3L);
        long next = Math.min(maxMp, getCurrentMagicules(player) + restore);
        data.putLong(KEY_MP, next);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        long tick = player.serverLevel().getGameTime();
        if (tick % 5L == 0L) {
            long maxMp = getMaxMagicules(player);
            long cur = getCurrentMagicules(player);
            if (cur < maxMp) {
                CompoundTag data = EntityDataHelper.getCustomData(player);
                boolean fastRegen = data.getBoolean("TensuraTrueDemonLord")
                        || PrimordialPlayerDataHelper.isPrimordial(player)
                        || EvolvedSkillHelper.isUrielDefenseActive(player);
                long regen = Math.max(400L, maxMp / (fastRegen ? 70L : 120L));
                data.putLong(KEY_MP, Math.min(maxMp, cur + regen));
            }
        }
        if (tick % 10L == 0L) {
            TensuraSkillHelper.syncSkillStateToClient(player);
        }
    }

    public static String formatCompact(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.US, "%.2fB", value / 1_000_000_000.0D);
        }
        if (value >= 1_000_000L) {
            return String.format(Locale.US, "%.1fM", value / 1_000_000.0D);
        }
        if (value >= 1_000L) {
            return String.format(Locale.US, "%.1fK", value / 1_000.0D);
        }
        return String.valueOf(value);
    }

    private static int countDivineArmorPieces(ServerPlayer player) {
        int count = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (!armor.isEmpty() && armor.getItem() instanceof DivineArmorItem) {
                count++;
            }
        }
        return count;
    }

    private static boolean isHoldingMoonlightSword(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.MOONLIGHT_SWORD.get())
                || player.getOffhandItem().is(ModItems.MOONLIGHT_SWORD.get());
    }
}
