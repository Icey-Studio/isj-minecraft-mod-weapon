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
 *  1. Mê Hoặc Dục Vọng (Desire Subjugation): Tẩy não đàn quái trong 20m biến thành tay sai mắt đỏ.
 *  2. Xiềng Xích Đoạt Mệnh (Chains of Greed): Phóng xích hồng neon khóa chân & hút 25% HP + Ma Tố.
 *  3. Bùng Nổ Tâm Trí (Desire Overload): Búng tay kích nổ quá tải dục vọng gây sát thương chuẩn nổ tung.
 */
public final class GreedAbility {

    public static final String TAG_GREED_UNLOCKED = "GreedSkillUnlocked";
    public static final String TAG_GREED_CONTROLLED = "GreedControlled";
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

            // Bắn cột hào quang ma trận hồng tím quanh người
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

    /**
     * THỨC 1: MÊ HOẶC DỤC VỌNG (DESIRE SUBJUGATION)
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
            // Không áp đặt được Boss cấp Chân Long hoặc Ma Vương tối cổ
            if (mob instanceof VeldoraEntity || mob instanceof VelgryndEntity || mob instanceof VelzardEntity || mob instanceof MilimEntity) {
                continue;
            }
            mob.addTag(TAG_GREED_CONTROLLED);
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 2, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 1, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 2, false, false));
            mob.setTarget(null);

            // Đổi mục tiêu của quái sang kẻ thù gần nhất của người chơi
            List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16.0D),
                    e -> e.isAlive() && e != player && e != mob && !e.getTags().contains(TAG_GREED_CONTROLLED));
            if (!enemies.isEmpty()) {
                mob.setTarget(enemies.get(0));
            }

            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
            level.sendParticles(NEON_PINK, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            controlledCount++;
        }

        player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fMê Hoặc Dục Vọng: Đã thao túng tâm trí §e" + controlledCount + " §fsinh vật thành con rối!"), true);
        SageManager.sendCornerAlert(player, "« Kẻ Tham Lam »: Đã mở rộng trường Dục Vọng — Chi phối thành công " + controlledCount + " mục tiêu làm bia đỡ đạn!", 0xFFF43F5E, 5000, false);
    }

    /**
     * THỨC 2: XIỀNG XÍCH THAM LAM (CHAINS OF GREED DRAIN)
     * Bắn chùm xích hồng neon trói cứng đối thủ trước mặt, hút 25% máu & toàn bộ Ma Tố nạp về bản thân.
     */
    public static void castChainsOfGreed(ServerLevel level, ServerPlayer player) {
        if (!isUnlocked(player)) {
            player.displayClientMessage(Component.literal("§c⚠️ Bạn chưa thức tỉnh Kỹ Năng Độc Nhất [Kẻ Tham Lam]!"), true);
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 24.0D;
        Vec3 reach = eye.add(look.scale(maxDist));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 2.0F, 1.3F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.5F);

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(3.5D),
                e -> e.isAlive() && e != player);

        int drainedCount = 0;
        float totalHealed = 0.0F;

        for (LivingEntity v : victims) {
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 9, false, false));
            v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3, false, false));
            v.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));

            float dmg = Math.max(30.0F, v.getMaxHealth() * 0.25F);
            v.hurt(level.damageSources().magic(), dmg);
            totalHealed += Math.min(20.0F, dmg * 0.5F);

            // Bắn tia hạt xích từ mục tiêu về người chơi
            Vec3 targetPos = v.position();
            Vec3 dir = player.position().subtract(targetPos);
            for (int i = 0; i <= 15; i++) {
                double progress = i / 15.0D;
                Vec3 p = targetPos.add(dir.scale(progress));
                level.sendParticles(NEON_PINK, p.x, p.y + 1.0D, p.z, 2, 0.05, 0.05, 0.05, 0.01);
            }
            drainedCount++;
        }

        if (drainedCount > 0) {
            player.heal(totalHealed);
            MagiculesManager.addAzathothConversion(player, 40_000L * drainedCount);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fXiềng Xích Đoạt Mệnh: Trói " + drainedCount + " mục tiêu & Hồi +" + (int) totalHealed + " HP!"), true);
        } else {
            player.displayClientMessage(Component.literal("§7[Kẻ Tham Lam] Không tìm thấy mục tiêu nào trong tầm ngắm 24m."), true);
        }
    }

    /**
     * THỨC 3: BÙNG NỔ DỤC VỌNG - SỤP ĐỔ TÂM TRÍ (DESIRE OVERLOAD)
     * Kích nổ não bộ & mạch năng lượng của toàn bộ quái vật bị dính Greed hoặc xung quanh.
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
            float baseDmg = wasControlled ? 180.0F : 85.0F; // Con rối bị nổ gấp đôi sát thương
            e.hurt(level.damageSources().magic(), baseDmg);
            e.setDeltaMovement(0, 0.8D, 0); // Hất tung

            level.sendParticles(NEON_PINK, e.getX(), e.getY() + 1.0D, e.getZ(), 30, 0.5, 0.6, 0.5, 0.15);
            level.sendParticles(ParticleTypes.ASH, e.getX(), e.getY() + 1.0D, e.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
            burstCount++;
        }

        player.displayClientMessage(Component.literal("§d§l[KẺ THAM LAM] §fQuá Tải Dục Vọng: Kích nổ tâm trí §c" + burstCount + " §fmục tiêu xung quanh!"), true);
    }
}
