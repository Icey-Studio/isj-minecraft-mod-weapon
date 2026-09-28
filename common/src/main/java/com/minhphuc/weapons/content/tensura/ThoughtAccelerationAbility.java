package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Trí Huệ Chi Vương (Raphael / Ciel) - Gia Tốc Tư Duy & Dự Đoán Quỹ Đạo Đòn Đánh
 * (Thought Acceleration & Future Attack Prediction):
 * - Gia Tốc Tư Duy 1,000,000 lần: Giảm tốc toàn bộ đối thủ xung quanh (Slowness IV).
 * - Dự Đoán Quỹ Đạo Đòn Đánh: Vẽ các tia laser holographic dự báo đường đạn / đòn tấn công.
 * - Tự Động Phản Xạ Lướt Né (Auto-Evade / Instant Dodge): Khi bị tấn công, tự động lướt né triệt tiêu sát thương.
 */
public class ThoughtAccelerationAbility {

    public static final String NBT_THOUGHT_ACCEL = "TensuraThoughtAcceleration";
    public static final String NBT_LAST_DODGE = "TensuraLastDodgeTick";

    private static final DustParticleOptions LASER_CYAN =
            new DustParticleOptions(new Vector3f(0.0F, 0.95F, 1.0F), 1.5F);
    private static final DustParticleOptions LASER_RED =
            new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.2F), 1.6F);

    public static boolean isActive(ServerPlayer player) {
        if (player == null) return false;
        return EntityDataHelper.getCustomData(player).getInt(NBT_THOUGHT_ACCEL) > 0;
    }

    public static boolean cast(ServerLevel level, ServerPlayer player) {
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        if (!isTrueDemonLord) {
            player.displayClientMessage(
                    Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Yêu cầu thức tỉnh Chân Ma Vương để sử dụng quyền năng Trí Huệ Chi Vương!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }

        // Hồi chiêu 25 giây
        if (player.getCooldowns().isOnCooldown(ModItems.MOONLIGHT_SWORD.get())
                || player.getCooldowns().isOnCooldown(ModItems.DEMON_LORD_SEED.get())) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Quyền năng Trí Huệ Chi Vương đang trong thời gian hồi phục!"),
                    true
            );
            return false;
        }

        player.getCooldowns().addCooldown(ModItems.MOONLIGHT_SWORD.get(), 500);
        player.getCooldowns().addCooldown(ModItems.DEMON_LORD_SEED.get(), 500);

        // Kích hoạt trong 15 giây (300 ticks)
        EntityDataHelper.getCustomData(player).putInt(NBT_THOUGHT_ACCEL, 300);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.5F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 2.0F, 1.6F);

        player.displayClientMessage(
                Component.literal("§e§l╔════════════════════════════════════════════════╗"),
                false
        );
        player.displayClientMessage(
                Component.literal("§e§l║ §b§l✦ TRÍ HUỆ CHI VƯƠNG (RAPHAEL / CIEL) ✦ §e§l║"),
                false
        );
        player.displayClientMessage(
                Component.literal("§e§l╠════════════════════════════════════════════════╣"),
                false
        );
        player.displayClientMessage(
                Component.literal("§a  Báo cáo: Đã kích hoạt [Gia Tốc Tư Duy 1,000,000 lần]!"),
                false
        );
        player.displayClientMessage(
                Component.literal("§b  Hiệu lực: Dự đoán trước toàn bộ quỹ đạo đòn đánh & Tự động né tránh!"),
                false
        );
        player.displayClientMessage(
                Component.literal("§e§l╚════════════════════════════════════════════════╝"),
                false
        );

        return true;
    }

    public static void tickPlayer(ServerPlayer player) {
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        int remainingTicks = tag.getInt(NBT_THOUGHT_ACCEL);
        if (remainingTicks <= 0) return;

        tag.putInt(NBT_THOUGHT_ACCEL, remainingTicks - 1);
        ServerLevel sl = player.serverLevel();

        // Buff gia tốc phản xạ thần thánh cho người chơi
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 25, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 1, false, false, false));

        // Hào quang ma trận holographic cyan quanh người chơi
        if (player.tickCount % 2 == 0) {
            double angle = (player.tickCount * 15.0D) * Math.PI / 180.0D;
            double px = player.getX() + Math.cos(angle) * 1.5D;
            double pz = player.getZ() + Math.sin(angle) * 1.5D;
            sl.sendParticles(LASER_CYAN, px, player.getY() + 1.0D, pz, 1, 0, 0.02D, 0, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, player.getY() + 1.2D, pz, 1, 0, 0.05D, 0, 0.01D);
        }

        // 1. Time Dilation: Làm chậm 80% mọi kẻ thù xung quanh trong 18m
        AABB slowBox = player.getBoundingBox().inflate(18.0D);
        List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, slowBox,
                e -> e != player && e.isAlive() && (e instanceof Enemy || (e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player)));
        for (LivingEntity enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false, false));
        }

        // 2. Dự Đoán Quỹ Đạo Đòn Đánh: Vẽ các tia laser holographic dự báo từ quái vật / đạn đạo
        Vec3 playerHead = player.getEyePosition();

        // (a) Dự đoán đường đạn projectile bay trong không gian
        AABB projBox = player.getBoundingBox().inflate(28.0D);
        List<Projectile> projectiles = sl.getEntitiesOfClass(Projectile.class, projBox, p -> p.isAlive());
        for (Projectile proj : projectiles) {
            Vec3 vel = proj.getDeltaMovement();
            if (vel.lengthSqr() > 0.01D) {
                Vec3 pStart = proj.position();
                for (double d = 0.5D; d <= 12.0D; d += 1.0D) {
                    Vec3 beamPoint = pStart.add(vel.normalize().scale(d));
                    sl.sendParticles(LASER_RED, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0, 0, 0, 0);
                }
            }
        }

        // (b) Dự đoán đòn tấn công của kẻ địch đang ngắm người chơi
        for (LivingEntity enemy : enemies) {
            Vec3 enemyEye = enemy.getEyePosition();
            Vec3 aimDir = playerHead.subtract(enemyEye).normalize();
            double dist = enemyEye.distanceTo(playerHead);

            // Vẽ tia laser đỏ cảnh báo nguy cơ tấn công
            for (double d = 0.8D; d < dist; d += 1.5D) {
                Vec3 laserPoint = enemyEye.add(aimDir.scale(d));
                sl.sendParticles(LASER_RED, laserPoint.x, laserPoint.y, laserPoint.z, 1, 0, 0, 0, 0);
            }
        }

        // Hiển thị Actionbar đếm ngược
        if (remainingTicks % 20 == 0) {
            int secs = remainingTicks / 20;
            player.displayClientMessage(
                    Component.literal("§b§l[GIA TỐC TƯ DUY - CIEL] §aDự đoán quỹ đạo đang hoạt động §e(" + secs + "s)"),
                    true
            );
        }
    }

    /**
     * Tự Động Phản Xạ Lướt Né (Auto-Evade / Instant Dodge) khi có đòn đánh sắp trúng
     */
    public static boolean handleIncomingAttack(ServerPlayer player, DamageSource source, float amount) {
        if (!isActive(player)) return false;

        ServerLevel sl = player.serverLevel();
        long now = sl.getGameTime();
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        long lastDodge = tag.getLong(NBT_LAST_DODGE);

        // Giới hạn né đòn: 1 lần mỗi 8 ticks (0.4s) để tránh spam teleport
        if (now - lastDodge < 8L) return false;
        tag.putLong(NBT_LAST_DODGE, now);

        // Tính hướng lướt né (né lệch sang một bên 90 độ hoặc giật lùi về sau)
        Vec3 look = player.getLookAngle();
        Vec3 sideDir = new Vec3(-look.z, 0, look.x).normalize();
        if (sl.random.nextBoolean()) {
            sideDir = sideDir.scale(-1.0D);
        }

        double dodgeDist = 2.8D;
        Vec3 targetPos = player.position().add(sideDir.scale(dodgeDist));
        BlockPos targetBp = BlockPos.containing(targetPos);

        // Kiểm tra vị trí né có an toàn không
        if (sl.getBlockState(targetBp).isAir() && sl.getBlockState(targetBp.above()).isAir()) {
            // Để lại tàn ảnh tại vị trí cũ
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.8D, 0.4D, 0.05D);
            sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(LASER_CYAN, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 1.0D, 0.5D, 0.02D);

            player.teleportTo(targetPos.x, targetPos.y, targetPos.z);

            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.4F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.8F);

            player.displayClientMessage(
                    Component.literal("§e§l[CIEL] §aĐã tự động lướt né đòn tấn công theo quỹ đạo dự đoán! §7(Triệt tiêu sát thương)"),
                    true
            );

            return true; // Triệt tiêu đòn đánh thành công!
        }

        return false;
    }
}
