package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.ElementalSpiritEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.world.VeldoraWorldData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
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
    private static final String KEY_OVERDRIVE = "TensuraMagiculeOverdrive";
    private static final String KEY_AEGIS = "TensuraMagiculeAegis";
    public static final String TAG_SOUL_CORRIDOR_OVERDRIVE = "SoulCorridorOverdrive";
    private static final DustParticleOptions OVERDRIVE_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.15F), 1.4F);
    private static final DustParticleOptions CORRIDOR_CYAN = new DustParticleOptions(new Vector3f(0.15F, 0.90F, 1.0F), 1.2F);

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

    public static boolean isMagiculeOverdriveActive(ServerPlayer player) {
        return player != null && EntityDataHelper.getCustomData(player).getBoolean(KEY_OVERDRIVE);
    }

    public static boolean isMagiculeAegisActive(ServerPlayer player) {
        return player != null && EntityDataHelper.getCustomData(player).getBoolean(KEY_AEGIS);
    }

    public static void toggleMagiculeOverdrive(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        boolean next = !data.getBoolean(KEY_OVERDRIVE);
        if (next) {
            long maxMp = getMaxMagicules(player);
            long cur = getCurrentMagicules(player);
            if (cur <= (long) (maxMp * 0.12D)) {
                player.displayClientMessage(Component.literal("§c[MA TỐ QUÁ TẢI] §7Ma tố dưới 12%, không đủ để kích hoạt quá tải!"), true);
                return;
            }
            data.putBoolean(KEY_OVERDRIVE, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.3F, 1.4F);
            player.displayClientMessage(Component.literal("§6§l[MA TỐ QUÁ TẢI: BẬT] §e+50% Sát thương Phép & Cộng hưởng Hành Lang Linh Hồn!"), false);
        } else {
            data.putBoolean(KEY_OVERDRIVE, false);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.1F);
            player.displayClientMessage(Component.literal("§7[MA TỐ QUÁ TẢI: TẮT] §fĐã thu hồi ma tố tỏa ra."), false);
        }
        TensuraSkillHelper.syncSkillStateToClient(player);
    }

    public static void toggleMagiculeAegis(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        boolean next = !data.getBoolean(KEY_AEGIS);
        if (next) {
            if (getCurrentMagicules(player) <= 0L) {
                player.displayClientMessage(Component.literal("§c[KHIÊN MA TỐ CHUYỂN HÓA] §7Không đủ Ma Tố để tạo kết giới!"), true);
                return;
            }
            data.putBoolean(KEY_AEGIS, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 1.2F);
            player.displayClientMessage(Component.literal("§b§l[KHIÊN MA TỐ CHUYỂN HÓA: BẬT] §fChuyển hóa §b100 MP = 1 DMG §fsát thương nhận vào!"), false);
        } else {
            data.putBoolean(KEY_AEGIS, false);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.9F);
            player.displayClientMessage(Component.literal("§7[KHIÊN MA TỐ CHUYỂN HÓA: TẮT] §fĐã đóng khiên chuyển hóa."), false);
        }
        TensuraSkillHelper.syncSkillStateToClient(player);
    }

    /**
     * Khiên Ma Tố Chuyển Hóa: Hấp thụ sát thương trực tiếp bằng Ma Tố (100 MP / 1 DMG).
     * Trả về lượng sát thương còn dư nếu MP cạn giữa chừng.
     */
    public static float absorbDamageWithMagiculeAegis(ServerPlayer player, float incomingDamage) {
        if (player == null || incomingDamage <= 0.0F || !isMagiculeAegisActive(player)) {
            return incomingDamage;
        }
        CompoundTag data = EntityDataHelper.getCustomData(player);
        long cur = getCurrentMagicules(player);
        if (cur <= 0L) {
            data.putBoolean(KEY_AEGIS, false);
            return incomingDamage;
        }
        long neededMp = Math.max(1L, (long) Math.ceil(incomingDamage * 100.0D));
        ServerLevel sl = player.serverLevel();
        if (cur >= neededMp) {
            long remainingMp = cur - neededMp;
            data.putLong(KEY_MP, remainingMp);
            sl.sendParticles(CORRIDOR_CYAN, player.getX(), player.getY() + 1.1D, player.getZ(), 10, 0.45D, 0.55D, 0.45D, 0.04D);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.6F);
            if (remainingMp <= 0L) {
                data.putBoolean(KEY_AEGIS, false);
                player.displayClientMessage(Component.literal("§c[KHIÊN MA TỐ] §7Ma tố cạn kiệt — Khiên Chuyển Hóa tạm tắt!"), true);
            }
            return 0.0F;
        }

        float absorbedDamage = (float) (cur / 100.0D);
        data.putLong(KEY_MP, 0L);
        data.putBoolean(KEY_AEGIS, false);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(), 16, 0.5D, 0.6D, 0.5D, 0.08D);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.1F, 1.25F);
        player.displayClientMessage(Component.literal("§c[KHIÊN MA TỐ] §7Ma tố cạn kiệt sau khi gánh §b" + String.format(Locale.US, "%.1f", absorbedDamage) + " §7sát thương!"), true);
        return Math.max(0.0F, incomingDamage - absorbedDamage);
    }

    /**
     * Ma Vương Bá Khí (Demon Lord's Haki): So sánh EP để nghiền nát kẻ yếu và trấn áp Boss mạnh trong 28m.
     */
    public static void castDemonLordHaki(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        onSkillCastPulse(player, 35_000L);
        long playerEp = getTotalEp(player);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.2F, 0.65F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.4F, 1.35F);

        double radius = 28.0D;
        for (int r = 2; r <= 26; r += 4) {
            int points = 18;
            for (int i = 0; i < points; i++) {
                double rad = (Math.PI * 2.0D * i) / points;
                double px = player.getX() + Math.cos(rad) * r;
                double pz = player.getZ() + Math.sin(rad) * r;
                level.sendParticles(OVERDRIVE_GOLD, px, player.getY() + 0.6D, pz, 1, 0.1D, 0.2D, 0.1D, 0.02D);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, player.getY() + 0.3D, pz, 1, 0.1D, 0.1D, 0.1D, 0.02D);
            }
        }

        int crushedCount = 0;
        int stunnedCount = 0;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && TensuraSkillHelper.isValidSkillTarget(player, e));

        for (LivingEntity target : targets) {
            long targetEp = AllOfCreationAbility.estimateEP(target);
            Vec3 away = target.position().subtract(player.position());
            Vec3 knock = away.lengthSqr() > 1.0e-4D ? away.normalize().scale(1.25D) : new Vec3(0.0D, 0.4D, 1.0D);

            if (targetEp * 10L <= playerEp) {
                if (target instanceof Enemy) {
                    target.hurt(player.damageSources().indirectMagic(player, player), Math.max(target.getHealth() + 50.0F, 9999.0F));
                    level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.9D, target.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.05D);
                    crushedCount++;
                } else {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 5));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 2));
                    target.push(knock.x * 1.4D, 0.45D, knock.z * 1.4D);
                    stunnedCount++;
                }
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                target.hurt(player.damageSources().indirectMagic(player, player), 60.0F);
                target.push(knock.x * 0.7D, 0.28D, knock.z * 0.7D);
                stunnedCount++;
            }
        }

        player.displayClientMessage(Component.literal("§6§l[MA VƯƠNG BÁ KHÍ] §fÁp đảo EP (§e" + formatCompact(playerEp)
                + "§f) — Bốc hơi §c" + crushedCount + " §fkẻ yếu, chấn áp §d" + stunnedCount + " §fđối thủ!"), false);
        TensuraSkillHelper.syncSkillStateToClient(player);
    }

    /**
     * Hành Lang Linh Hồn: Tìm các thuộc hạ / Chân Long / Ác Ma / Tinh Linh đồng minh trong bán kính.
     */
    public static List<LivingEntity> getSoulCorridorAllies(ServerPlayer player, double range) {
        List<LivingEntity> result = new ArrayList<>();
        if (player == null || !(player.level() instanceof ServerLevel sl)) return result;
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!e.isAlive() || e == player) continue;
            if (e instanceof VeldoraEntity v && v.isSealed()) continue;
            boolean eligibleType = (e instanceof VeldoraEntity)
                    || (e instanceof VelgryndEntity)
                    || (e instanceof PrimordialDemonEntity)
                    || (e instanceof UnderworldDemonEntity)
                    || (e instanceof ElementalSpiritEntity);
            if (eligibleType && TensuraSkillHelper.isAllied(player, e)) {
                result.add(e);
            }
        }
        return result;
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        ServerLevel sl = player.serverLevel();
        long tick = sl.getGameTime();
        CompoundTag data = EntityDataHelper.getCustomData(player);
        boolean overdrive = data.getBoolean(KEY_OVERDRIVE);

        if (tick % 5L == 0L) {
            long maxMp = getMaxMagicules(player);
            long cur = getCurrentMagicules(player);

            if (overdrive) {
                long drainPer5Ticks = Math.max(500L, maxMp / 200L); // ~2% MaxMP/giây
                long afterDrain = Math.max(0L, cur - drainPer5Ticks);
                data.putLong(KEY_MP, afterDrain);
                cur = afterDrain;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 35, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 35, 1, false, false, true));
                sl.sendParticles(OVERDRIVE_GOLD, player.getX(), player.getY() + 1.0D, player.getZ(), 5, 0.45D, 0.65D, 0.45D, 0.02D);

                if (cur <= (long) (maxMp * 0.10D)) {
                    data.putBoolean(KEY_OVERDRIVE, false);
                    overdrive = false;
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§e[MA TỐ QUÁ TẢI] §7Ma tố xuống dưới 10% — Tự động thu hồi!"), true);
                }
            } else if (cur < maxMp) {
                boolean recoveringFromExhaustion = data.getBoolean(KEY_MP_EXHAUSTED_RECOVERY);
                boolean fastRegen = !recoveringFromExhaustion && (data.getBoolean("TensuraTrueDemonLord")
                        || PrimordialPlayerDataHelper.isPrimordial(player)
                        || EvolvedSkillHelper.isUrielDefenseActive(player));
                long divisor = recoveringFromExhaustion ? 300L : (fastRegen ? 70L : 120L);
                long regen = Math.max(250L, maxMp / divisor);

                // Hành Lang Linh Hồn: Hồi Ma Tố x2.5 khi MP < 30% và có đồng minh linh hồn trong 48m
                if (!recoveringFromExhaustion && cur < (long) (maxMp * 0.30D) && !getSoulCorridorAllies(player, 48.0D).isEmpty()) {
                    regen = Math.round(regen * 2.5D);
                    sl.sendParticles(CORRIDOR_CYAN, player.getX(), player.getY() + 1.1D, player.getZ(), 4, 0.35D, 0.45D, 0.35D, 0.03D);
                }

                long next = Math.min(maxMp, cur + regen);
                data.putLong(KEY_MP, next);
                if (recoveringFromExhaustion && next >= maxMp / 2L) {
                    data.putBoolean(KEY_MP_EXHAUSTED_RECOVERY, false);
                }
            } else {
                data.putBoolean(KEY_MP_EXHAUSTED_RECOVERY, false);
            }
        }

        // Hành Lang Linh Hồn: Chia sẻ Hào Quang Quá Tải cho thuộc hạ trong 32m
        if (tick % 10L == 0L) {
            if (overdrive) {
                for (LivingEntity ally : getSoulCorridorAllies(player, 32.0D)) {
                    if (!ally.getTags().contains(TAG_SOUL_CORRIDOR_OVERDRIVE)) {
                        ally.addTag(TAG_SOUL_CORRIDOR_OVERDRIVE);
                    }
                    ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, true));
                    sl.sendParticles(OVERDRIVE_GOLD, ally.getX(), ally.getY() + 1.1D, ally.getZ(), 4, 0.4D, 0.6D, 0.4D, 0.02D);
                }
            } else {
                for (LivingEntity ally : getSoulCorridorAllies(player, 48.0D)) {
                    ally.removeTag(TAG_SOUL_CORRIDOR_OVERDRIVE);
                }
            }
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
