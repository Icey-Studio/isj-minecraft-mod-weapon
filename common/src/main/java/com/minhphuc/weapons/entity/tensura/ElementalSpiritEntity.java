package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.tensura.TensuraSkillHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Tinh Linh Nguyên Tố (Hỏa / Phong / Thổ / Thủy / Quang / Ám):
 * - Có thể triệu hồi tại Tế Đàn Tinh Linh, bay lượn bảo vệ chủ nhân & Liên Bang Tempest.
 * - Mỗi nguyên tố có phép thuật riêng (Hỏa cầu, Lốc Phong, Khiên Đá + hồi máu, Sóng Thủy + giải độc, Tia Sáng thanh tẩy, Hút Máu Bóng Tối).
 */
public class ElementalSpiritEntity extends TamableAnimal {

    private static final EntityDataAccessor<Integer> DATA_ELEMENT =
            SynchedEntityData.defineId(ElementalSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(ElementalSpiritEntity.class, EntityDataSerializers.INT);

    private BlockPos homeShrinePos = null;
    private int castCooldown = 30;

    public ElementalSpiritEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 220.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, 0);
        builder.define(DATA_MODE, 0); // 0 = Follow, 1 = Guard Shrine/Area
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.25D, 5.0F, 24.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> this.isTame() && TensuraSkillHelper.isValidSkillTarget(this.getOwner() != null ? this.getOwner() : this, e)));
    }

    public ElementalType getElementalType() {
        return ElementalType.fromId(this.entityData.get(DATA_ELEMENT));
    }

    public void setElementalType(ElementalType type) {
        this.entityData.set(DATA_ELEMENT, type.ordinal());
        this.setCustomName(Component.literal(type.colorCode + "✦ " + type.displayName));
        this.setCustomNameVisible(true);
    }

    public int getCommandMode() {
        return this.entityData.get(DATA_MODE);
    }

    public void setCommandMode(int mode) {
        this.entityData.set(DATA_MODE, Math.floorMod(mode, 2));
    }

    public void setHomeShrinePos(BlockPos pos) {
        this.homeShrinePos = pos;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp && sp.getUUID().equals(this.getOwnerUUID())) {
            if (hand == InteractionHand.MAIN_HAND) {
                int next = (getCommandMode() + 1) % 2;
                setCommandMode(next);
                this.setOrderedToSit(next == 1);
                String modeName = next == 0 ? "§aHộ Vệ Chủ Nhân" : "§eCanh Gác Tế Đàn / Lãnh Địa";
                sp.displayClientMessage(Component.literal(getElementalType().colorCode + "[" + getElementalType().displayName + "] §7Chế độ: " + modeName), true);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        ElementalType type = getElementalType();

        if (this.level().isClientSide()) {
            if (this.tickCount % 3 == 0) {
                DustParticleOptions dust = new DustParticleOptions(
                        new Vector3f(type.r / 255.0F, type.g / 255.0F, type.b / 255.0F), 1.15F);
                this.level().addParticle(dust,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.getY() + 0.6 + this.random.nextDouble() * 0.7,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel sl)) return;

        // Tự giữ độ cao bay mượt
        this.setNoGravity(true);
        if (this.getTarget() == null && homeShrinePos != null && getCommandMode() == 1) {
            if (this.distanceToSqr(Vec3.atCenterOf(homeShrinePos)) > 144.0D) {
                this.getNavigation().moveTo(homeShrinePos.getX() + 0.5, homeShrinePos.getY() + 1.5, homeShrinePos.getZ() + 0.5, 1.1D);
            }
        }

        // Hồi phục nhẹ và ban buff cho chủ / dân làng Tempest lân cận mỗi 5s
        if (this.tickCount % 100 == 0) {
            this.heal(5.0F);
            applyElementalBlessingAura(sl, type);
        }

        // Tấn công phép thuật nguyên tố
        if (castCooldown > 0) castCooldown--;
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && castCooldown <= 0 && this.distanceToSqr(target) <= 22.0D * 22.0D) {
            castElementalMagic(sl, type, target);
            castCooldown = 45;
        }
    }

    private void applyElementalBlessingAura(ServerLevel sl, ElementalType type) {
        LivingEntity owner = this.getOwner();
        if (owner != null && owner.isAlive() && owner.distanceToSqr(this) <= 196.0D) {
            switch (type) {
                case FIRE -> owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 140, 0, true, false));
                case WIND -> owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 0, true, false));
                case EARTH -> owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 0, true, false));
                case WATER -> {
                    owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
                    owner.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 140, 0, true, false));
                }
                case LIGHT -> owner.heal(3.0F);
                case DARK -> owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
            }
        }
        // Liên kết bảo vệ Dân làng gần đó nếu có
        for (Villager v : sl.getEntitiesOfClass(Villager.class, this.getBoundingBox().inflate(10.0D))) {
            v.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
        }
    }

    private void castElementalMagic(ServerLevel sl, ElementalType type, LivingEntity target) {
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(type.r / 255.0F, type.g / 255.0F, type.b / 255.0F), 1.4F);
        Vec3 start = this.position().add(0, 0.9, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 diff = end.subtract(start);
        for (int i = 1; i <= 10; i++) {
            Vec3 pt = start.add(diff.scale(i / 10.0D));
            sl.sendParticles(dust, pt.x, pt.y, pt.z, 2, 0.1, 0.1, 0.1, 0.02);
        }

        float baseDmg = 24.0F;
        switch (type) {
            case FIRE -> {
                target.setRemainingFireTicks(100);
                target.hurt(this.damageSources().inFire(), baseDmg + 8.0F);
                sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8, target.getZ(), 18, 0.5, 0.5, 0.5, 0.08);
            }
            case WIND -> {
                target.setDeltaMovement(target.getDeltaMovement().add(diff.normalize().scale(0.9)).add(0, 0.55, 0));
                target.hasImpulse = true;
                target.hurt(this.damageSources().magic(), baseDmg);
            }
            case EARTH -> {
                target.hurt(this.damageSources().mobAttack(this), baseDmg + 12.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case WATER -> {
                target.hurt(this.damageSources().magic(), baseDmg);
                if (this.getOwner() != null) this.getOwner().heal(4.0F);
            }
            case LIGHT -> {
                target.hurt(this.damageSources().magic(), baseDmg * (target instanceof net.minecraft.world.entity.monster.Enemy ? 1.5F : 1.0F));
                sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 14, 0.3, 0.4, 0.3, 0.05);
            }
            case DARK -> {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                target.hurt(this.damageSources().wither(), baseDmg + 4.0F);
                this.heal(6.0F);
            }
        }
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 1.0F, 1.1F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) return false;
        if (getElementalType() == ElementalType.FIRE && (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpiritElement", this.entityData.get(DATA_ELEMENT));
        tag.putInt("SpiritMode", this.entityData.get(DATA_MODE));
        if (homeShrinePos != null) {
            tag.putInt("HomeX", homeShrinePos.getX());
            tag.putInt("HomeY", homeShrinePos.getY());
            tag.putInt("HomeZ", homeShrinePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_ELEMENT, tag.getInt("SpiritElement"));
        this.entityData.set(DATA_MODE, tag.getInt("SpiritMode"));
        if (tag.contains("HomeX")) {
            homeShrinePos = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        }
    }
}
