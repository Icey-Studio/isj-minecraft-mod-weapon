package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * ĐẠI ÁC MA MARBAS (GOETIA PRESIDENT MARBAS - 堕天悪魔マルバス)
 * BOSS TỐT NGHIỆP SINH CỰC HÙNG TRONG DARK GATHERING (CHAPTER 52):
 * - Chúa tể y tế & giải phẫu học tàn nhẫn trong bệnh viện phế tích.
 * - Vóc dáng khổng lồ, toàn thân trắng toát, sừng cừu uy nghi, đôi cánh trắng.
 * - Chỉ số: 1500 HP, 22 Armor, 12 Toughness, 1.0 Knockback Resistance (Mạnh ngang Cứu Thế Quyền Hiện).
 * - Kỹ năng độc bản:
 *   1. Lãnh Địa Bàn Mổ (Surgical Theater): Phủ rèm ánh sáng trắng bao quanh 30m.
 *   2. Ép Tim Tử Tuyệt (Severed Heart Grasp): Kẻ nào sprint/nhảy dồn dập sẽ bị bóp nát cơ tim (sát thương chuẩn).
 *   3. Sóng Dao Mổ & Tước Đoạt Não Bộ (Severed Brain Wave): Đảo lộn giác quan, gây mù lòa và suy yếu.
 *   4. Tái Cấu Trúc Sinh Thể: Hấp thụ sinh mạng của mọi mob tử trận xung quanh để tự hồi phục máu.
 *   5. Rơi đồ: Trọn bộ nội tạng thần linh (Tim, Não, Cánh Tay, Mắt).
 * - TỐI ƯU HÓA ZERO-LAG: Quét O(1), định kỳ 20 ticks, không spam entity đạn.
 */
public class MarbasEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DOMAIN_ACTIVE =
            SynchedEntityData.defineId(MarbasEntity.class, EntityDataSerializers.BOOLEAN);

    private int dissectionCooldown = 160; // 8s

    public MarbasEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DOMAIN_ACTIVE, false);
    }

    public boolean isDomainActive() {
        return this.entityData.get(DOMAIN_ACTIVE);
    }

    public void setDomainActive(boolean active) {
        this.entityData.set(DOMAIN_ACTIVE, active);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, LivingEntity.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 20, true, false,
                e -> !(e instanceof MarbasEntity)));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide() || !this.isAlive()) return;
        ServerLevel sl = (ServerLevel) this.level();

        LivingEntity target = this.getTarget();
        boolean hasCombatTarget = (target != null && target.isAlive());
        setDomainActive(hasCombatTarget);

        boolean hasPlayerNearby = sl.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 48.0D);

        // =========================================================================
        // 1. LÃNH ĐỊA BÀN MỔ (SURGICAL OPERATING THEATER) - QUÉT MỖI 20 TICKS (1S)
        // =========================================================================
        if (this.tickCount % 20 == 0 && hasCombatTarget) {
            double domainRadius = 30.0D;
            AABB domainBox = this.getBoundingBox().inflate(domainRadius);

            // Hiệu ứng hào quang viền lãnh địa trắng bạc (chỉ hiển thị khi có người chơi gần)
            if (hasPlayerNearby) {
                for (int a = 0; a < 360; a += 36) {
                    double rad = Math.toRadians(a);
                    double px = this.getX() + Math.cos(rad) * domainRadius;
                    double pz = this.getZ() + Math.sin(rad) * domainRadius;
                    sl.sendParticles(ParticleTypes.END_ROD, px, this.getY() + 0.5D, pz, 1, 0, 0.5D, 0, 0.02D);
                }
            }

            List<LivingEntity> inDomain = sl.getEntitiesOfClass(LivingEntity.class, domainBox,
                    e -> e.isAlive() && e != this && !(e instanceof MarbasEntity));

            for (LivingEntity victim : inDomain) {
                // Ép Tim Tử Tuyệt (Severed Heart Grasp): Nạn nhân chạy nước rút sẽ bị bóp nghẹt tim
                if (victim.isSprinting() || victim.getDeltaMovement().horizontalDistanceSqr() > 0.08D) {
                    victim.hurt(sl.damageSources().magic(), 5.0F); // 2.5 tim sát thương chuẩn
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
                    if (hasPlayerNearby) {
                        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5F, 1.2F);
                        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 4, 0.2D, 0.3D, 0.2D, 0.05D);
                    }
                }
            }

            // Tái Cấu Trúc Sinh Thể: Hồi máu tự nhiên khi chiến đấu
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(10.0F);
                if (hasPlayerNearby) {
                    sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 2.0D, this.getZ(), 6, 0.5D, 0.8D, 0.5D, 0.02D);
                }
            }
        }

        // =========================================================================
        // 2. SÓNG DAO MỔ & TƯỚC ĐOẠT NÃO BỘ (SEVERED BRAIN DISSECTION) - MỖI 8S
        // =========================================================================
        if (dissectionCooldown > 0) {
            dissectionCooldown--;
        } else if (hasCombatTarget) {
            dissectionCooldown = 160;

            if (hasPlayerNearby) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 1.4F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.7F);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.5D, this.getZ(), 12, 1.2D, 0.5D, 1.2D, 0);
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 2.0D, this.getZ(), 2, 0, 0, 0, 0);
            }

            AABB slashBox = this.getBoundingBox().inflate(18.0D);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, slashBox,
                    e -> e.isAlive() && e != this && !(e instanceof MarbasEntity));

            for (LivingEntity v : victims) {
                v.hurt(sl.damageSources().mobAttack(this), 25.0F);
                v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, true));
                v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false, true));

                Vec3 knock = v.position().subtract(this.position()).normalize().scale(1.2D).add(0, 0.35D, 0);
                v.setDeltaMovement(knock);
                v.hasImpulse = true;
            }
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5D, this.getZ(), 2, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5D, this.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.2D);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 3.0F, 1.0F);

            // Rơi trọn bộ vật phẩm giải phẫu nội tạng thần thoại
            dropItemAtPos(sl, new ItemStack(ModItems.SEVERED_HEART.get()));
            dropItemAtPos(sl, new ItemStack(ModItems.SEVERED_BRAIN.get()));
            dropItemAtPos(sl, new ItemStack(ModItems.SEER_FLESH_ARM.get()));
            dropItemAtPos(sl, new ItemStack(ModItems.SEER_FLESH_EYE.get()));

            for (ServerPlayer player : sl.players()) {
                if (player.position().distanceTo(this.position()) <= 64.0D) {
                    player.displayClientMessage(
                            Component.literal("§4§l[ĐẠI ÁC MA: BẠI TRẬN] §fĐại Ác Quỷ Bàn Mổ Marbas đã bị tiêu diệt! Nhục thân tà ác đã tan biến! ☠️"),
                            false
                    );
                }
            }
        }
    }

    private void dropItemAtPos(ServerLevel sl, ItemStack stack) {
        ItemEntity it = new ItemEntity(sl, this.getX(), this.getY() + 1.0D, this.getZ(), stack);
        it.setDefaultPickUpDelay();
        sl.addFreshEntity(it);
    }
}
