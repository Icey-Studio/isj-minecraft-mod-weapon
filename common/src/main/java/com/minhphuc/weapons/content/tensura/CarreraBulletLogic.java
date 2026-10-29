package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.tensura.CarreraBulletItem.BulletType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý hiệu ứng bắn và sát thương của 4 loại Ma Đạn Hoàng Kim.
 */
public class CarreraBulletLogic {

    private static final DustParticleOptions GOLD_DIVINE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.1F), 2.5F);
    private static final DustParticleOptions ABYSS_PURPLE_DUST = new DustParticleOptions(new Vector3f(0.5F, 0.0F, 0.8F), 2.2F);
    private static final DustParticleOptions GRAVITY_DARK_DUST = new DustParticleOptions(new Vector3f(0.2F, 0.0F, 0.3F), 2.0F);

    public static class ActiveAbyssVortex {
        public final ServerLevel level;
        public final Vec3 center;
        public final ServerPlayer caster;
        public int ticksRemaining = 60; // 3 giây duy trì

        public ActiveAbyssVortex(ServerLevel level, Vec3 center, ServerPlayer caster) {
            this.level = level;
            this.center = center;
            this.caster = caster;
        }

        public boolean tick() {
            ticksRemaining--;
            if (ticksRemaining > 0) {
                // Hút mọi sinh vật xung quanh vào tâm
                AABB pullBox = new AABB(center.x - 14, center.y - 8, center.z - 14, center.x + 14, center.y + 12, center.z + 14);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != caster && e.isAlive());
                for (LivingEntity target : targets) {
                    Vec3 toCenter = center.subtract(target.position()).normalize().scale(0.45D);
                    target.setDeltaMovement(target.getDeltaMovement().add(toCenter));
                    target.hurtMarked = true;
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
                }

                // Hiệu ứng vòng xoáy hắc ám
                for (int i = 0; i < 12; i++) {
                    double angle = (System.currentTimeMillis() % 3600) / 100.0D + (i * Math.PI / 6.0D);
                    double r = 1.5D + (ticksRemaining % 30) * 0.1D;
                    double px = center.x + Math.cos(angle) * r;
                    double py = center.y + (i % 4) * 0.4D;
                    double pz = center.z + Math.sin(angle) * r;
                    level.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 1, 0, 0, 0, 0.01);
                    level.sendParticles(ABYSS_PURPLE_DUST, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                }

                if (ticksRemaining % 10 == 0) {
                    level.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.5F, 1.6F);
                }
                return false;
            } else {
                // Phát nổ hắc ám hạt nhân cực đại
                level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.6F);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.0F, 1.2F);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0D, center.z, 2, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0D, center.z, 3, 0.5, 0.5, 0.5, 0.1);

                AABB blastBox = new AABB(center.x - 12, center.y - 6, center.z - 12, center.x + 12, center.y + 10, center.z + 12);
                List<LivingEntity> blastTargets = level.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e != caster && e.isAlive());
                DamageSource damageSource = level.damageSources().playerAttack(caster);
                for (LivingEntity target : blastTargets) {
                    target.hurt(damageSource, 280.0F);
                    Vec3 blastAway = target.position().subtract(center).normalize().scale(1.8D).add(0, 0.8D, 0);
                    target.setDeltaMovement(blastAway);
                    target.hurtMarked = true;
                }
                return true;
            }
        }
    }

    public static final List<ActiveAbyssVortex> ACTIVE_VORTICES = new ArrayList<>();

    public static void tickVortices() {
        if (ACTIVE_VORTICES.isEmpty()) return;
        ACTIVE_VORTICES.removeIf(ActiveAbyssVortex::tick);
    }

    /**
     * Bắn loại đạn tương ứng từ Súng Hoàng Kim
     */
    public static void fireBullet(ServerLevel level, ServerPlayer player, BulletType bulletType) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();

        switch (bulletType) {
            case JUDGEMENT -> fireJudgementBullet(level, player, eyePos, lookVec);
            case ABYSS_CORE -> fireAbyssCoreBullet(level, player, eyePos, lookVec);
            case GRAVITY -> fireGravityBullet(level, player, eyePos, lookVec);
            case RAPID -> fireRapidBullet(level, player, eyePos, lookVec);
        }
    }

    /**
     * 1. ĐẠN THẦN TỐC PHÁN QUYẾT: Bắn phát chết luôn + Thông báo Giọng Nói Thế Giới
     */
    private static void fireJudgementBullet(ServerLevel level, ServerPlayer player, Vec3 eyePos, Vec3 lookVec) {
        double maxDistance = 120.0D;
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

        // Âm thanh bắn sấm rền & thánh ca
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 1.4F);

        // Vẽ chùm tia laser vàng rực kéo dài xé rách không gian
        double step = 0.5D;
        int numSteps = (int) (maxDistance / step);
        LivingEntity hitEntity = null;

        for (int i = 0; i < numSteps; i++) {
            Vec3 currentPos = eyePos.add(lookVec.scale(i * step));

            level.sendParticles(GOLD_DIVINE_DUST, currentPos.x, currentPos.y, currentPos.z, 2, 0.04, 0.04, 0.04, 0.01);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, currentPos.x, currentPos.y, currentPos.z, 1, 0.02, 0.02, 0.02, 0.02);
            }
            if (i % 8 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, currentPos.x, currentPos.y, currentPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }

            // Kiểm tra va chạm sinh vật
            AABB searchBox = new AABB(currentPos.x - 1.5D, currentPos.y - 1.5D, currentPos.z - 1.5D,
                    currentPos.x + 1.5D, currentPos.y + 1.5D, currentPos.z + 1.5D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive());
            if (!targets.isEmpty()) {
                hitEntity = targets.get(0);
                endPos = currentPos;
                break;
            }
        }

        if (hitEntity != null) {
            // TẤT SÁT 100%: BẮN PHÁT CHẾT LUÔN!
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitEntity.getX(), hitEntity.getY() + 1.0D, hitEntity.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, hitEntity.getX(), hitEntity.getY() + 1.0D, hitEntity.getZ(), 50, 0.5, 0.8, 0.5, 0.3);
            level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.0F, 0.7F);

            // Gây sát thương chuẩn cực đại hủy diệt lõi linh hồn
            DamageSource source = level.damageSources().playerAttack(player);
            hitEntity.invulnerableTime = 0;
            hitEntity.hurt(source, 999999.0F);
            hitEntity.setHealth(0.0F);
            hitEntity.die(source);

            // Thông báo Giọng Nói Thế Giới
            VoiceOfTheWorld.announce(player, "§dBÁO CÁO. §aKhai phóng Kỹ Năng Tối Thượng: §6§lPHÁN QUYẾT (JUDGEMENT)§a! Đã tiêu diệt §e" +
                    hitEntity.getName().getString() + " §avà xóa sổ hoàn toàn hạch tâm linh hồn!");
        } else {
            // Không trúng ai
            player.displayClientMessage(
                    Component.literal("§e§l[SÚNG HOÀNG KIM] §6Tia Phán Quyết xé toạc không gian!"),
                    true
            );
        }
    }

    /**
     * 2. ĐẠN HẠCH THÂM UYÊN: Tạo hố đen nuốt chửng rồi phát nổ
     */
    private static void fireAbyssCoreBullet(ServerLevel level, ServerPlayer player, Vec3 eyePos, Vec3 lookVec) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2.0F, 0.6F);

        BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(lookVec.scale(60.0D)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPos = hit.getLocation();

        // Kích hoạt hố đen trọng lực tại điểm trúng
        ACTIVE_VORTICES.add(new ActiveAbyssVortex(level, impactPos, player));

        player.displayClientMessage(
                Component.literal("§5§l[HẠCH THÂM UYÊN] §dĐã nén ma pháp hạt nhân Abaddon! Hố đen trọng lực đang hình thành!"),
                true
        );
    }

    /**
     * 3. ĐẠN TRỌNG LỰC SỤP ĐỔ: Đè bẹp và khóa chân mục tiêu
     */
    private static void fireGravityBullet(ServerLevel level, ServerPlayer player, Vec3 eyePos, Vec3 lookVec) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.5F, 0.8F);

        BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(lookVec.scale(50.0D)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPos = hit.getLocation();

        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0F, 0.5F);

        // Hiệu ứng sóng trọng lực lan tỏa
        for (int i = 0; i < 30; i++) {
            double angle = i * (Math.PI * 2.0D / 30.0D);
            double rx = Math.cos(angle) * 8.0D;
            double rz = Math.sin(angle) * 8.0D;
            level.sendParticles(GRAVITY_DARK_DUST, impactPos.x + rx, impactPos.y + 0.2D, impactPos.z + rz, 3, 0.2, 0.2, 0.2, 0.05);
            level.sendParticles(ParticleTypes.PORTAL, impactPos.x + rx, impactPos.y + 0.5D, impactPos.z + rz, 2, 0.1, 0.1, 0.1, 0.1);
        }

        AABB box = new AABB(impactPos.x - 10, impactPos.y - 4, impactPos.z - 10, impactPos.x + 10, impactPos.y + 6, impactPos.z + 10);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            target.setDeltaMovement(0, -1.8D, 0);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 255, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 4, false, false));
            target.hurt(level.damageSources().playerAttack(player), 55.0F);
        }

        player.displayClientMessage(
                Component.literal("§9§l[TRỌNG LỰC SỤP ĐỔ] §bĐã đè bẹp không gian, khóa cứng di chuyển mục tiêu!"),
                true
        );
    }

    /**
     * 4. ĐẠN HOÀNG KIM XẠ KÍCH: Tốc độ cao, liên thanh, xuyên giáp
     */
    private static void fireRapidBullet(ServerLevel level, ServerPlayer player, Vec3 eyePos, Vec3 lookVec) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.0F, 1.4F);

        double maxDist = 70.0D;
        Vec3 end = eyePos.add(lookVec.scale(maxDist));

        for (double d = 0; d < maxDist; d += 1.0D) {
            Vec3 p = eyePos.add(lookVec.scale(d));
            level.sendParticles(GOLD_DIVINE_DUST, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
            if (d % 4 == 0) {
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
            }

            AABB box = new AABB(p.x - 1.0D, p.y - 1.0D, p.z - 1.0D, p.x + 1.0D, p.y + 1.0D, p.z + 1.0D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 42.0F);
                target.setDeltaMovement(target.getDeltaMovement().add(lookVec.scale(0.8D)));
                target.hurtMarked = true;
            }
        }
    }
}
