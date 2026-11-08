package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * THỰC THỂ TỐI THƯỢNG: MA VƯƠNG KHÔNG VONG (COSMIC DEITY KUUBOU)
 * - Tiến hóa tối thượng từ Không Vong khi hấp thụ đủ 64 Linh Hồn hoặc qua Trứng Triệu Hồi.
 * - Ngoại hình:
 *   + Đầu là khối cầu vũ trụ đen khổng lồ với vành nhật hoa xúc tu squiggly lơ lửng, con mắt nhật thực rực sáng ở trung tâm.
 *   + Thân dưới là dải tinh vân vũ trụ gradient Hồng - Tím - Cam xoắn lượn, hai càng cua vũ trụ vươn rộng.
 * - Sức mạnh gấp 20 lần sinh vật thông thường:
 *   + Máu: 520 HP. Sát thương: 65. Giáp: 20.
 *   + Khả năng bay lượn tự do, miễn nhiễm dung nham và lửa.
 *   + Tuyệt chiêu: Nhật Thực Hư Vô (Eclipse Beam) & Tinh Vân Lực Trường (Cosmic Gravity Pulse).
 */
public class DemonLordKuboEntity extends Monster {

    private final ServerBossEvent bossEvent =
            (ServerBossEvent) new ServerBossEvent(
                    Component.literal("§5§lMa Vương Không Vong §d[Thần Hư Không Vũ Trụ]"),
                    BossEvent.BossBarColor.PURPLE,
                    BossEvent.BossBarOverlay.NOTCHED_12
            ).setDarkenScreen(true);

    private int attackCooldown = 40;
    private int pulseCooldown = 100;

    public DemonLordKuboEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setCustomName(Component.literal("§5§lMa Vương Không Vong"));
        this.setCustomNameVisible(true);
        this.xpReward = 1500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 520.0D)
                .add(Attributes.ATTACK_DAMAGE, 65.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.FLYING_SPEED, 0.45D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CosmicFlightGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> !(e instanceof DemonLordKuboEntity) && !(e instanceof KuboEntity)));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level().isClientSide) {
            // Hiệu ứng hạt bụi sao và hào quang nhật thực
            Vec3 center = this.position().add(0, 1.8D, 0);
            for (int i = 0; i < 3; i++) {
                double rx = (this.random.nextDouble() - 0.5D) * 2.2D;
                double ry = (this.random.nextDouble() - 0.5D) * 2.2D;
                double rz = (this.random.nextDouble() - 0.5D) * 2.2D;
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, center.x + rx, center.y + ry, center.z + rz, 0, 0, 0);
            }
            if (this.random.nextFloat() < 0.3F) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 0, 0.05, 0);
            }
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        // 1. Tuyệt chiêu: Tinh Vân Lực Trường (Cosmic Gravity Pulse)
        if (--pulseCooldown <= 0) {
            pulseCooldown = 120 + this.random.nextInt(60);
            executeNebulaPulse(serverLevel);
        }

        // 2. Tấn công mục tiêu: Bắn Tia Nhật Thực Hư Vô (Eclipse Beam)
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = this.distanceToSqr(target);

            if (--attackCooldown <= 0 && distSq <= 40.0D * 40.0D) {
                attackCooldown = 50 + this.random.nextInt(30);
                fireEclipseBeam(serverLevel, target);
            }
        }
    }

    private void executeNebulaPulse(ServerLevel serverLevel) {
        Vec3 center = this.position().add(0, 1.5D, 0);
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 0.8F);
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.6F);

        // Hút và gây sát thương diện rộng 18m
        AABB pulseBox = this.getBoundingBox().inflate(18.0D);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, pulseBox,
                e -> e.isAlive() && e != this && !(e instanceof DemonLordKuboEntity));

        for (LivingEntity v : victims) {
            Vec3 toCenter = center.subtract(v.position());
            double dist = toCenter.length();
            if (dist > 1.0D) {
                v.setDeltaMovement(v.getDeltaMovement().add(toCenter.normalize().scale(0.4D)).add(0, 0.1D, 0));
                v.hurtMarked = true;
            }
            v.hurt(serverLevel.damageSources().mobAttack(this), 35.0F);
        }

        // Vòng sóng xung kích hạt
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10.0D);
            double px = center.x + Math.cos(angle) * 12.0D;
            double pz = center.z + Math.sin(angle) * 12.0D;
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, center.y, pz, 1, 0, 0.1, 0, 0.05);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, px, center.y, pz, 2, 0, 0.05, 0, 0.02);
        }
    }

    private void fireEclipseBeam(ServerLevel serverLevel, LivingEntity target) {
        Vec3 eyePos = this.position().add(0, 2.0D, 0);
        Vec3 targetPos = target.getEyePosition();
        Vec3 dir = targetPos.subtract(eyePos).normalize();
        double distance = Math.min(35.0D, eyePos.distanceTo(targetPos));

        serverLevel.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 1.4F);

        for (double d = 0.5D; d <= distance; d += 0.8D) {
            Vec3 point = eyePos.add(dir.scale(d));
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 3, 0.2, 0.2, 0.2, 0.05);
        }

        target.hurt(serverLevel.damageSources().magic(), 55.0F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.DEMON_LORD_SOUL.get(), 1));
        this.spawnAtLocation(new ItemStack(ModItems.DEMON_LORD_AWAKENING_ORB.get(), 1));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    private static class CosmicFlightGoal extends Goal {
        private final DemonLordKuboEntity entity;

        public CosmicFlightGoal(DemonLordKuboEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !entity.getMoveControl().hasWanted() || entity.getRandom().nextInt(30) == 0;
        }

        @Override
        public void tick() {
            LivingEntity target = entity.getTarget();
            if (target != null && target.isAlive()) {
                double tx = target.getX() + (entity.getRandom().nextDouble() - 0.5D) * 8.0D;
                double ty = target.getY() + 3.0D + entity.getRandom().nextDouble() * 3.0D;
                double tz = target.getZ() + (entity.getRandom().nextDouble() - 0.5D) * 8.0D;
                entity.getMoveControl().setWantedPosition(tx, ty, tz, 1.2D);
            } else {
                double rx = entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * 16.0D;
                double ry = entity.getY() + (entity.getRandom().nextDouble() - 0.5D) * 6.0D;
                double rz = entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * 16.0D;
                entity.getMoveControl().setWantedPosition(rx, ry, rz, 0.8D);
            }
        }
    }
}
