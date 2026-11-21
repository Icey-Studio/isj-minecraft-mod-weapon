package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * KỸ NĂNG ĐỘC NHẤT: KẺ THAM LAM (GREEDY ONE - 强欲者)
 * Kế thừa nguyên bản quyền năng tà mị chi phối dục vọng của Mariabell Rosso:
 *  1. Mê Hoặc Dục Vọng (Desire Subjugation): Thao túng tâm trí diện rộng 22m biến bầy quái thành con rối phục tùng.
 *  2. Xiềng Xích Tham Lam (Chains of Greed): Phóng xích ma pháp trói mục tiêu, hút HP & CƯỠNG CHẾ THAO TÚNG con rối.
 *  3. Bùng Nổ Tâm Trí (Desire Overload): Búng tay kích nổ quá tải dục vọng toàn bộ con rối gây sát thương chuẩn nổ tung.
 */
public final class GreedAbility {

    public static final String TAG_GREED_UNLOCKED = "GreedSkillUnlocked";
    public static final String TAG_GREED_CONTROLLED = "GreedControlled";
    public static final String TAG_GREED_OWNER = "GreedOwnerUUID";

    private static final DustParticleOptions NEON_PINK = new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.85F), 2.2F);
    private static final DustParticleOptions DARK_MAGENTA = new DustParticleOptions(new Vector3f(0.7F, 0.05F, 0.9F), 1.8F);

    private GreedAbility() {}

    public static boolean isUnlocked(ServerPlayer player) {
        if (player == null) return false;
        return player.getTags().contains(TAG_GREED_UNLOCKED)
                || EntityDataHelper.getCustomData(player).getBoolean(TAG_GREED_UNLOCKED);
    }

    public static void unlockForPlayer(ServerPlayer player) {
        if (player == null) return;
        player.addTag(TAG_GREED_UNLOCKED);
        EntityDataHelper.getCustomData(player).putBoolean(TAG_GREED_UNLOCKED, true);
        TensuraSkillHelper.syncSkillStateToClient(player);

        VoiceOfTheWorld.announce(player, "§d§lTHỨC TỈNH KỸ NĂNG ĐỘC NHẤT THÀNH CÔNG!\n" +
                "§fCá thể đã hấp thu trọn vẹn bản thể dục vọng, chính thức giác ngộ:\n" +
                "§d§l[KẺ THAM LAM - GREEDY ONE (强欲者)]§f!\n" +
                "§eQuyền năng chi phối tâm trí, cưỡng đoạt sinh mệnh & thao túng vạn vật đã khắc sâu vào linh hồn!");

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.2F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5F, 1.5F);

            for (int i = 0; i < 360; i += 15) {
                double rad = Math.toRadians(i);
                double px = player.getX() + Math.cos(rad) * 2.8D;
                double pz = player.getZ() + Math.sin(rad) * 2.8D;
                sl.sendParticles(NEON_PINK, px, player.getY() + 0.5D, pz, 3, 0.1D, 0.4D, 0.1D, 0.02D);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, player.getY() + 1.2D, pz, 2, 0.05D, 0.3D, 0.05D, 0.01D);
                sl.sendParticles(ParticleTypes.ASH, px, player.getY() + 0.2D, pz, 2, 0.1D, 0.1D, 0.1D, 0.01D);
            }
        }
    }

    public static boolean canManipulate(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (entity instanceof ServerPlayer) return false;
        if (entity instanceof VeldoraEntity || entity instanceof VelgryndEntity || entity instanceof VelzardEntity || entity instanceof MilimEntity) {
            return false;
        }
        return entity instanceof Mob;
    }

    public static void subjugateMob(Mob mob, ServerPlayer player, ServerLevel level) {
        if (!canManipulate(mob)) return;

        mob.addTag(TAG_GREED_CONTROLLED);
        EntityDataHelper.getCustomData(mob).putString(TAG_GREED_OWNER, player.getStringUUID());

        // Đổi tên hiển thị con rối
        String rawName = mob.getType().getDescription().getString();
        mob.setCustomName(Component.literal("§d§l[Con Rối] §f" + rawName));
        mob.setCustomNameVisible(true);

        // Xóa hoàn toàn thù hận đối với người chơi
        mob.setTarget(null);

        // Ban phát sinh lực dục vọng cực đại
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 18000, 2, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 18000, 1, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 18000, 1, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 18000, 1, false, false));

        // Tự động tìm kẻ địch xung quanh người chơi để tấn công
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(18.0D),
                e -> e.isAlive() && e != player && e != mob && !e.getTags().contains(TAG_GREED_CONTROLLED));
        if (!enemies.isEmpty()) {
            mob.setTarget(enemies.get(0));
        }

        // VFX trái tim và hạt neon hồng
        level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
        level.sendParticles(NEON_PINK, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
    }

    /**
     * THỨC 1: MÊ HOẶC DỤC VỌNG (DESIRE SUBJUGATION) - Crouch
     * Thao túng tâm trí toàn bộ quái vật xung quanh trong bán kính 22m thành con rối trung thành.
     */
    public static void castDesireSubjugation(ServerLevel level, ServerPlayer player) {
        if (!isUnlocked(player)) {
            player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa thức tỉnh Kỹ Năng Độc Nhất [Kẻ Tham Lam]!"), true);
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 2.0F, 1.1F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.8F, 0.8F);

        // Vòng tròn ma trận dục vọng nở rộng
        for (double r = 2.0D; r <= 22.0D; r += 3.0D) {
            for (int a = 0; a < 360; a += 24) {
                double rad = Math.toRadians(a);
                double px = player.getX() + Math.cos(rad) * r;
                double pz = player.getZ() + Math.sin(rad) * r;
                level.sendParticles(NEON_PINK, px, player.getY() + 0.2D, pz, 1, 0.05, 0.1, 0.05, 0.01);
                level.sendParticles(ParticleTypes.ASH, px, player.getY() + 0.2D, pz, 1, 0.02, 0.05, 0.02, 0.01);
            }
        }

        List<Mob> targets = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(22.0D),
                m -> m.isAlive() && !m.getTags().contains(TAG_GREED_CONTROLLED));

        int controlledCount = 0;
        for (Mob mob : targets) {
            if (!canManipulate(mob)) continue;
            subjugateMob(mob, player, level);
            controlledCount++;
        }

        player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fMê Hoặc Dục Vọng: Đã thao túng tâm trí §e" + controlledCount + " §fsinh vật thành con rối trung thành!"), true);
        SageManager.sendCornerAlert(player, "« Kẻ Tham Lam »: Đã mở rộng trường Dục Vọng — Thao túng thành công " + controlledCount + " mục tiêu làm con rối bảo vệ!", 0xFFF43F5E, 5000, false);
    }

    /**
     * THỨC 2: XIỀNG XÍCH THAM LAM (CHAINS OF GREED DRAIN & MANIPULATION) - Chuột Phải
     * Bắn chùm xích hồng neon trói cứng đối thủ trước mặt, hút 20% HP & CƯỠNG CHẾ THAO TÚNG con rối (không giết luôn).
     */
    public static void castChainsOfGreed(ServerLevel level, ServerPlayer player) {
        if (!isUnlocked(player)) {
            player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa thức tỉnh Kỹ Năng Độc Nhất [Kẻ Tham Lam]!"), true);
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 26.0D;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 2.0F, 1.3F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.5F);

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(3.5D),
                e -> e.isAlive() && e != player);

        int controlledCount = 0;
        int drainedCount = 0;
        float totalHealed = 0.0F;

        for (LivingEntity v : victims) {
            // Hiệu ứng tia hạt xích từ mục tiêu về người chơi
            Vec3 targetPos = v.position();
            Vec3 dir = player.position().subtract(targetPos);
            for (int i = 0; i <= 15; i++) {
                double progress = i / 15.0D;
                Vec3 p = targetPos.add(dir.scale(progress));
                level.sendParticles(NEON_PINK, p.x, p.y + 1.0D, p.z, 2, 0.05, 0.05, 0.05, 0.01);
            }

            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 9, false, false));
            v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 3, false, false));

            float siphon = Math.min(v.getMaxHealth() * 0.20F, 30.0F);
            totalHealed += siphon;

            if (canManipulate(v) && v instanceof Mob mob) {
                // Giữ lại ít nhất 4 HP để KHÔNG GIẾT LUÔN quái, mà THAO TÚNG thành con rối
                if (mob.getHealth() - siphon < 4.0F) {
                    mob.setHealth(Math.max(4.0F, mob.getHealth() * 0.5F));
                } else {
                    mob.hurt(level.damageSources().magic(), siphon);
                }
                subjugateMob(mob, player, level);
                controlledCount++;
            } else {
                // Boss hoặc player không thể thao túng thì chịu sát thương trừng phạt nặng
                v.hurt(level.damageSources().magic(), Math.max(35.0F, v.getMaxHealth() * 0.25F));
            }
            drainedCount++;
        }

        if (drainedCount > 0) {
            player.heal(totalHealed);
            MagiculesManager.addAzathothConversion(player, 40_000L * drainedCount);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            if (controlledCount > 0) {
                player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fXiềng Xích Tham Lam: Đã trói buộc & THAO TÚNG §e" + controlledCount + " §fsinh vật làm con rối!"), true);
            } else {
                player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fXiềng Xích Tham Lam: Rút sinh lực " + drainedCount + " mục tiêu & Hồi +" + (int) totalHealed + " HP!"), true);
            }
        } else {
            player.displayClientMessage(Component.literal("§7[Kẻ Tham Lam] Không tìm thấy mục tiêu nào trong tầm ngắm 26m."), true);
        }
    }

    /**
     * THỨC 3: BÙNG NỔ DỤC VỌNG - SỤP ĐỔ TÂM TRÍ (DESIRE OVERLOAD) - Shift + Chuột Phải
     * Kích nổ não bộ & mạch năng lượng của toàn bộ con rối hoặc quái vật xung quanh gây sát thương chuẩn cực đại.
     */
    public static void castDesireOverload(ServerLevel level, ServerPlayer player) {
        if (!isUnlocked(player)) {
            player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa thức tỉnh Kỹ Năng Độc Nhất [Kẻ Tham Lam]!"), true);
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.6F);

        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 0.8D, player.getZ(), 2, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.2D, player.getZ(), 2, 0, 0, 0, 0);

        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(18.0D),
                e -> e.isAlive() && e != player);

        int burstCount = 0;
        for (LivingEntity e : enemies) {
            boolean wasControlled = e.getTags().contains(TAG_GREED_CONTROLLED);
            float baseDmg = wasControlled ? 180.0F : 85.0F; // Con rối bị kích nổ bộc phát gấp đôi sát thương
            e.hurt(level.damageSources().magic(), baseDmg);
            e.setDeltaMovement(0, 0.8D, 0);

            level.sendParticles(NEON_PINK, e.getX(), e.getY() + 1.0D, e.getZ(), 30, 0.5, 0.6, 0.5, 0.15);
            level.sendParticles(ParticleTypes.ASH, e.getX(), e.getY() + 1.0D, e.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
            burstCount++;
        }

        player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fQuá Tải Dục Vọng: Kích nổ tâm trí §c" + burstCount + " §fmục tiêu xung quanh!"), true);
    }

    /**
     * DUY TRÌ & ĐIỀU KHIỂN HỘ VỆ CON RỐI THEO THỜI GIAN THỰC (ZERO LAG)
     * Gọi định kỳ từ server tick: bảo vệ người chơi, bám theo chủ nhân, không bao giờ đánh chủ.
     */
    public static void tickPuppets(ServerLevel level) {
        if (level == null || level.isClientSide()) return;
        long gameTime = level.getGameTime();
        if (gameTime % 10L != 0L) return; // Throttle 0.5 giây / lần

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        for (ServerPlayer player : players) {
            if (!isUnlocked(player)) continue;

            String playerUuidStr = player.getStringUUID();
            List<Mob> puppets = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(36.0D),
                    m -> m.isAlive() && m.getTags().contains(TAG_GREED_CONTROLLED));

            for (Mob puppet : puppets) {
                // Kiểm tra chủ nhân
                String ownerUuid = EntityDataHelper.getCustomData(puppet).getString(TAG_GREED_OWNER);
                if (!ownerUuid.isEmpty() && !ownerUuid.equals(playerUuidStr)) {
                    continue; // Thuộc quyền người chơi khác
                }

                // 1. Không bao giờ tấn công chủ nhân
                if (puppet.getTarget() == player) {
                    puppet.setTarget(null);
                }

                // 2. Tự động hỗ trợ giao tranh
                LivingEntity attacker = player.getLastHurtByMob();
                LivingEntity target = player.getLastHurtMob();
                if (attacker != null && attacker.isAlive() && attacker != puppet && !attacker.getTags().contains(TAG_GREED_CONTROLLED)) {
                    puppet.setTarget(attacker);
                } else if (target != null && target.isAlive() && target != puppet && !target.getTags().contains(TAG_GREED_CONTROLLED)) {
                    puppet.setTarget(target);
                }

                // 3. Di chuyển bám theo chủ nhân
                double distSq = puppet.distanceToSqr(player);
                if (distSq > 14.0D * 14.0D && distSq <= 36.0D * 36.0D && puppet.getTarget() == null) {
                    puppet.getNavigation().moveTo(player, 1.35D);
                } else if (distSq > 36.0D * 36.0D) {
                    // Dịch chuyển tức thời lại gần chủ nhân nếu bị bỏ quá xa
                    puppet.teleportTo(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D);
                }

                // 4. Hiệu ứng hạt con rối định kỳ
                if (gameTime % 20L == 0L) {
                    level.sendParticles(NEON_PINK, puppet.getX(), puppet.getY() + puppet.getBbHeight() + 0.3D, puppet.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
    }
}
