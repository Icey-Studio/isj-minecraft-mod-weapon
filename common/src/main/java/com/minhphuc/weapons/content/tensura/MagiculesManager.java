package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.world.VeldoraWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Random;

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
    private static final String KEY_MP_EXHAUSTED_DEATH = "TensuraMpExhaustedDeath";
    private static final String KEY_MP_EXHAUSTED_RECOVERY = "TensuraMpExhaustedRecovery";

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
        maxMp += JuraTempestManager.getBonusMaxMp(player);
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
        ep += JuraTempestManager.getBonusEp(player);
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

    public static void restoreMagicules(ServerPlayer player, long amount) {
        if (player == null || amount <= 0L) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long maxMp = getMaxMagicules(player);
        long cur = getCurrentMagicules(player);
        if (cur < maxMp) {
            data.putLong(KEY_MP, Math.min(maxMp, cur + amount));
        }
    }

    /**
     * Tính EP ngẫu nhiên nhưng cố định theo UUID của từng Chân Long (>= 700,000,000 EP).
     */
    public static long getTrueDragonEp(LivingEntity entity) {
        if (entity == null) return 700_000_000L;
        long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        Random rng = new Random(seed);
        if (entity instanceof VelzardEntity) {
            return 735_000_000L + (long) (rng.nextDouble() * 225_000_000L);
        }
        if (entity instanceof VelgryndEntity) {
            return 715_000_000L + (long) (rng.nextDouble() * 195_000_000L);
        }
        // Veldora hoặc Chân Long khác luôn >= 700 triệu
        return 700_000_000L + (long) (rng.nextDouble() * 190_000_000L);
    }

    /**
     * Kiểm tra Bạo Phong Long Veldora có đang ở trong người chơi (Không Gian Ảo Azathoth) hay không.
     */
    public static boolean hasVeldoraInside(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        VeldoraWorldData vd = VeldoraWorldData.get(player.getServer().overworld());
        if (vd.isVeldoraStoredInStomach() && data.getBoolean(AzathothStomachData.KEY_VELDORA_BEFRIENDED)) {
            return true;
        }
        ListTag stomach = data.getList(AzathothStomachData.KEY_STOMACH, Tag.TAG_COMPOUND);
        for (int i = 0; i < stomach.size(); i++) {
            if ("VELDORA".equals(stomach.getCompound(i).getString("SpecialKind"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tiêu hao Ma Tố cho nghi thức quan trọng (Ban Danh Xưng / Hồi Sinh Veldora).
     * - Nếu allowVeldoraAssist = true & Veldora đang ở trong người: ngẫu nhiên rút Ma Tố của Veldora (65%),
     *   hoặc rút của người chơi trước và khi gần cạn Veldora tự bù phần thiếu!
     * - Nếu không có Veldora bảo trợ: trả về {@code false} nếu hiện tại <= 0 MP; nếu đủ > 0 và bị trừ về <= 0
     *   thì vẫn cho nghi thức diễn ra nhưng đánh dấu tử vong vì kiệt quệ Ma Tố!
     */
    public static boolean canAttemptMagiculeSpend(ServerPlayer player) {
        if (player == null) return false;
        if (player.isCreative()) return true;
        return getCurrentMagicules(player) > 0L || hasVeldoraInside(player);
    }

    /**
     * @return true nếu người chơi vừa bị rút cạn sạch Ma Tố dẫn đến tử vong (tự hồi sinh với 0 MP và hồi từ từ).
     */
    public static boolean consumeMagiculesWithExhaustion(ServerPlayer player, long cost, boolean allowVeldoraAssist, String actionLabel) {
        if (player == null || player.isCreative() || cost <= 0L) return false;

        CompoundTag data = EntityDataHelper.getCustomData(player);
        boolean veldoraInside = allowVeldoraAssist && hasVeldoraInside(player);
        long cur = getCurrentMagicules(player);

        if (veldoraInside) {
            boolean useVeldoraDirectly = player.getRandom().nextFloat() < 0.65F;
            if (useVeldoraDirectly) {
                player.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"Kuahaha! Để Bạo Phong Long ta cho mượn Ma Tố " + actionLabel + " — cứ yên tâm dùng đi!\""), false);
                SageManager.sendCornerAlert(player, "Đang dùng Ma Tố của Veldora trong Không Gian Ảo cho " + actionLabel + " (0 tiêu hao MP bản thân)!", 0xFF38BDF8, 4500, false);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
                return false;
            }
            if (cur > cost) {
                data.putLong(KEY_MP, cur - cost);
                TensuraSkillHelper.syncSkillStateToClient(player);
                player.displayClientMessage(Component.literal("§d[MA TỐ] §7Đã tiêu hao §b" + formatCompact(cost) + " MP §7của bản thân để " + actionLabel + "."), true);
                return false;
            } else {
                // Bản thân sắp cạn sạch -> Veldora trong người lập tức bù phần Ma Tố còn thiếu
                data.putLong(KEY_MP, Math.max(1L, Math.min(cur, getMaxMagicules(player) / 20L)));
                TensuraSkillHelper.syncSkillStateToClient(player);
                player.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"Này bạn hiền, hết Ma Tố của ngươi rồi kìa! Để ta bơm phần còn lại cho!\""), false);
                SageManager.sendCornerAlert(player, "Ma Tố bản thân vừa chạm đáy — Veldora đã truyền Ma Tố bù vào cho bạn!", 0xFF38BDF8, 5000, false);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.3F);
                return false;
            }
        }

        // Không có Veldora trong người -> rút trực tiếp từ Ma Tố bản thân
        long next = cur - cost;
        if (next <= 0L) {
            data.putLong(KEY_MP, 0L);
            data.putBoolean(KEY_MP_EXHAUSTED_DEATH, true);
            data.putBoolean(KEY_MP_EXHAUSTED_RECOVERY, true);
            TensuraSkillHelper.syncSkillStateToClient(player);
            return true;
        }

        data.putLong(KEY_MP, next);
        TensuraSkillHelper.syncSkillStateToClient(player);
        return false;
    }

    public static void triggerMagiculeExhaustionDeath(ServerPlayer player, String reason) {
        if (player == null || player.isCreative()) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        data.putLong(KEY_MP, 0L);
        data.putBoolean(KEY_MP_EXHAUSTED_DEATH, true);
        data.putBoolean(KEY_MP_EXHAUSTED_RECOVERY, true);
        TensuraSkillHelper.syncSkillStateToClient(player);
        VoiceOfTheWorld.announce(player, "Cảnh báo khẩn cấp! Ma Tố đã CẠN KIỆT HOÀN TOÀN (" + reason + ")! Cơ thể sụp đổ do kiệt quệ Ma Tố!");
        player.displayClientMessage(Component.literal("§4§l[KIỆT QUỆ MA TỐ] §cBạn đã cạn sạch Ma Tố và tử vong! Ma Tố sẽ từ từ hồi phục sau khi sống lại."), false);
        player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
        if (player.isAlive()) {
            player.kill();
        }
    }

    public static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        if (oldPlayer == null || newPlayer == null) return;
        CompoundTag oldData = EntityDataHelper.getCustomData(oldPlayer);
        CompoundTag newData = EntityDataHelper.getCustomData(newPlayer);

        if (oldData.contains(KEY_MP_INIT)) {
            newData.putBoolean(KEY_MP_INIT, oldData.getBoolean(KEY_MP_INIT));
        }
        if (oldData.contains(KEY_BONUS_MAX_MP)) {
            newData.putLong(KEY_BONUS_MAX_MP, oldData.getLong(KEY_BONUS_MAX_MP));
        }
        if (oldData.contains(KEY_BONUS_EP)) {
            newData.putLong(KEY_BONUS_EP, oldData.getLong(KEY_BONUS_EP));
        }

        boolean exhaustedDeath = oldData.getBoolean(KEY_MP_EXHAUSTED_DEATH) || oldData.getLong(KEY_MP) <= 0L;
        if (exhaustedDeath) {
            newData.putLong(KEY_MP, 0L);
            newData.putBoolean(KEY_MP_INIT, true);
            newData.putBoolean(KEY_MP_EXHAUSTED_DEATH, false);
            newData.putBoolean(KEY_MP_EXHAUSTED_RECOVERY, true);
            newPlayer.displayClientMessage(Component.literal("§e[HỒI PHỤC MA TỐ] §7Linh hồn vừa tái tạo với §c0 Ma Tố§7. Nguồn ma tố đang từ từ hồi sinh..."), false);
        } else {
            long carry = oldData.getBoolean(KEY_MP_INIT) ? oldData.getLong(KEY_MP) : getMaxMagicules(newPlayer);
            newData.putLong(KEY_MP, Math.max(0L, Math.min(getMaxMagicules(newPlayer), carry)));
        }
        TensuraSkillHelper.syncSkillStateToClient(newPlayer);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        long tick = player.serverLevel().getGameTime();
        if (tick % 5L == 0L) {
            long maxMp = getMaxMagicules(player);
            long cur = getCurrentMagicules(player);
            if (cur < maxMp) {
                CompoundTag data = EntityDataHelper.getCustomData(player);
                boolean recoveringFromExhaustion = data.getBoolean(KEY_MP_EXHAUSTED_RECOVERY);
                boolean fastRegen = !recoveringFromExhaustion && (data.getBoolean("TensuraTrueDemonLord")
                        || PrimordialPlayerDataHelper.isPrimordial(player)
                        || EvolvedSkillHelper.isUrielDefenseActive(player));
                long divisor = recoveringFromExhaustion ? 300L : (fastRegen ? 70L : 120L);
                long regen = Math.max(250L, maxMp / divisor);
                long next = Math.min(maxMp, cur + regen);
                data.putLong(KEY_MP, next);
                if (recoveringFromExhaustion && next >= maxMp / 2L) {
                    data.putBoolean(KEY_MP_EXHAUSTED_RECOVERY, false);
                }
            } else {
                EntityDataHelper.getCustomData(player).putBoolean(KEY_MP_EXHAUSTED_RECOVERY, false);
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
