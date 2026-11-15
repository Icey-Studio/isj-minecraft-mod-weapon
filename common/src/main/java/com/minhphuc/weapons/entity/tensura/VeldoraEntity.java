package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.tensura.TensuraNamingAbility;
import com.minhphuc.weapons.content.tensura.TensuraSkillHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * BẠO PHONG LONG VELDORA TEMPEST (STORM DRAGON - TRUE DRAGON):
 * - Có 2 hình thái: Dạng Rồng Khổng Lồ (phòng thủ & sức mạnh x30 lần) và Dạng Người (linh hoạt, cận chiến & CÓ THỂ BAN DANH XƯNG cho mob khác).
 * - Có thể tự do chuyển đổi hình thái trong chiến đấu.
 * - Sức mạnh ngang hàng Velgrynd và mạnh hơn x5 lần so với Ác Ma Thủy Tổ (Ác Ma Thủy Tổ tối đa chỉ có ~8% cơ hội thắng).
 * - KHÔNG THỂ bị ban tên hay tiến hóa Ma Vương bởi người chơi.
 * - Xuất hiện duy nhất trong Hang Động Phong Ấn (Long Giam Vô Hạn) và có thể được Giải Phong Ấn bởi Azathoth/Raphael để triệu hồi.
 */
public class VeldoraEntity extends TamableAnimal {

    public static final long VELDORA_EP = 88_126_500L;
    private static final DustParticleOptions STORM_CYAN = new DustParticleOptions(new Vector3f(0.25F, 0.78F, 1.0F), 2.1F);
    private static final DustParticleOptions BLACK_LIGHTNING = new DustParticleOptions(new Vector3f(0.15F, 0.08F, 0.25F), 2.4F);

    private static final EntityDataAccessor<Boolean> DATA_HUMAN_FORM =
            SynchedEntityData.defineId(VeldoraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SEALED =
            SynchedEntityData.defineId(VeldoraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ALLIED =
            SynchedEntityData.defineId(VeldoraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CASTING_STATE =
            SynchedEntityData.defineId(VeldoraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLYING_NOW =
            SynchedEntityData.defineId(VeldoraEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§b§l[BẠO PHONG LONG] §f§lVELDORA TEMPEST §7- §3[True Dragon]"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private int skillCooldown = 40;
    private int formSwitchCooldown = 260;
    private int castingTicks = 0;
    private int namingCooldown = 240;
    private boolean usingFlyingNav = false;

    public VeldoraEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 2500;
        this.moveControl = new MoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.FLYING_SPEED, 0.65D)
                .add(Attributes.ATTACK_DAMAGE, 180.0D)
                .add(Attributes.ARMOR, 48.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 28.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 72.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUMAN_FORM, false);
        builder.define(DATA_SEALED, false);
        builder.define(DATA_ALLIED, false);
        builder.define(DATA_CASTING_STATE, 0);
        builder.define(DATA_FLYING_NOW, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.3D, 10.0F, 4.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> {
            if (isSealed() || e == this || !e.isAlive()) return false;
            if (isAllied()) {
                if (this.getOwner() != null && (e == this.getOwner() || (e instanceof TamableAnimal ta && ta.getOwnerUUID() != null && ta.getOwnerUUID().equals(this.getOwnerUUID())))) {
                    return false;
                }
                return e instanceof Enemy || e.getTags().contains("TensuraDemonLordBerserk");
            } else {
                return (e instanceof Player p && !p.isCreative() && !p.isSpectator())
                        || e instanceof PrimordialDemonEntity
                        || e instanceof VelgryndEntity;
            }
        }));
    }

    public boolean isHumanForm() {
        return this.entityData.get(DATA_HUMAN_FORM);
    }

    public void setHumanForm(boolean human) {
        boolean changed = this.entityData.get(DATA_HUMAN_FORM) != human;
        this.entityData.set(DATA_HUMAN_FORM, human);
        applyFormAttributes(human);
        this.refreshDimensions();
        syncNavigationMode();
        if (changed && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0.5, 0.5, 0.5, 0.05);
            sl.sendParticles(STORM_CYAN, this.getX(), this.getY() + 1.4D, this.getZ(), 45, 1.2, 1.5, 1.2, 0.12);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 2.0F, human ? 1.3F : 0.7F);
        }
    }

    private void applyFormAttributes(boolean human) {
        float pct = this.getMaxHealth() > 0 ? Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.05F, 1.0F) : 1.0F;
        // Dạng Rồng mạnh & phòng thủ cao hơn Dạng Người gấp 30 lần
        double hp = human ? 1800.0D : 20000.0D;
        double atk = human ? 42.0D : 240.0D;
        double armor = human ? 14.0D : 52.0D;
        double speed = human ? 0.45D : 0.36D;

        if (getAttribute(Attributes.MAX_HEALTH) != null) getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
        if (getAttribute(Attributes.ARMOR) != null) getAttribute(Attributes.ARMOR).setBaseValue(armor);
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        this.setHealth((float) (hp * pct));
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isHumanForm() ? EntityDimensions.scalable(0.75F, 2.0F) : EntityDimensions.scalable(3.2F, 3.0F);
    }

    public boolean isSealed() {
        return this.entityData.get(DATA_SEALED);
    }

    public void setSealed(boolean sealed) {
        this.entityData.set(DATA_SEALED, sealed);
        this.setNoAi(sealed);
        this.setInvulnerable(sealed);
    }

    public boolean isAllied() {
        return this.entityData.get(DATA_ALLIED);
    }

    public void setAllied(boolean allied) {
        this.entityData.set(DATA_ALLIED, allied);
        this.setTame(allied, false);
        this.bossEvent.setColor(allied ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.BLUE);
    }

    public int getCastingState() {
        return this.entityData.get(DATA_CASTING_STATE);
    }

    public void setCastingState(int state) {
        this.entityData.set(DATA_CASTING_STATE, state);
    }

    public boolean isFlyingNow() {
        return this.entityData.get(DATA_FLYING_NOW);
    }

    private void syncNavigationMode() {
        if (this.level().isClientSide()) return;
        boolean shouldFly = !isHumanForm() && !isSealed();
        if (shouldFly != usingFlyingNav) {
            usingFlyingNav = shouldFly;
            if (shouldFly) {
                this.moveControl = new FlyingMoveControl(this, 18, false);
                this.navigation = new FlyingPathNavigation(this, this.level());
                this.setNoGravity(false);
            } else {
                this.moveControl = new MoveControl(this);
                this.navigation = new GroundPathNavigation(this, this.level());
                this.setNoGravity(false);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount % 4 == 0) {
                this.level().addParticle(STORM_CYAN,
                        this.getX() + (this.random.nextDouble() - 0.5D) * (isHumanForm() ? 1.2 : 3.5),
                        this.getY() + this.random.nextDouble() * (isHumanForm() ? 2.0 : 3.2),
                        this.getZ() + (this.random.nextDouble() - 0.5D) * (isHumanForm() ? 1.2 : 3.5),
                        0, 0.04, 0);
            }
            return;
        }

        if (isSealed()) {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.tickCount % 15 == 0 && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.8D, this.getZ(), 10, 1.5, 1.5, 1.5, 0.02);
            }
            return;
        }

        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()), 0.0F, 1.0F));
        this.entityData.set(DATA_FLYING_NOW, !isHumanForm() && !this.onGround());

        // Siêu hồi phục Long Chủng (+60 HP/s Dragon, +20 HP/s Human)
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(isHumanForm() ? 20.0F : 60.0F);
        }

        if (castingTicks > 0) {
            castingTicks--;
            if (castingTicks == 0) setCastingState(0);
        }

        // Dạng Người Veldora có thể tự ban danh xưng cho mob đồng minh/thường gần kề!
        if (isHumanForm() && isAllied()) {
            if (namingCooldown > 0) {
                namingCooldown--;
            } else {
                namingCooldown = 360;
                tryAutoBestowNameNearby();
            }
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            // Trong chiến đấu Veldora tự do chuyển đổi giữa Rồng <-> Người
            if (formSwitchCooldown > 0) {
                formSwitchCooldown--;
            } else {
                formSwitchCooldown = 300 + this.random.nextInt(180);
                // Ưu tiên hóa Rồng khi máu dưới 60% hoặc gặp boss mạnh (Primordial/Velgrynd/Milim), linh hoạt Dạng Người khi truy kích cận chiến
                boolean facingTitan = target instanceof PrimordialDemonEntity || target instanceof VelgryndEntity || target instanceof VelzardEntity || target instanceof MilimEntity;
                if (isHumanForm() && (facingTitan || this.getHealth() < this.getMaxHealth() * 0.7F)) {
                    setHumanForm(false);
                    broadcastNearby("§b§l[VELDORA] §fKuahaha! Để ta cho ngươi nếm mùi Chân Thân Bạo Phong Long!");
                } else if (!isHumanForm() && !facingTitan && this.random.nextFloat() < 0.35F) {
                    setHumanForm(true);
                    broadcastNearby("§b§l[VELDORA] §fThử xem Veldora-Style Killing Arts của ta đây!");
                }
            }

            if (skillCooldown > 0) {
                skillCooldown--;
            } else {
                executeStormSkill(target);
            }
        } else if (skillCooldown > 0) {
            skillCooldown--;
        }
    }

    private void tryAutoBestowNameNearby() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        List<Mob> candidates = sl.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(10.0D),
                m -> m.isAlive()
                        && !(m instanceof VeldoraEntity)
                        && !(m instanceof VelgryndEntity)
                        && !(m instanceof VelzardEntity)
                        && !(m instanceof MilimEntity)
                        && !m.getTags().contains("TensuraNamedMob")
                        && (m instanceof AbstractVillager || (m instanceof TamableAnimal ta && ta.isTame())));
        if (candidates.isEmpty()) return;

        Mob lucky = candidates.get(this.random.nextInt(candidates.size()));
        String bestowed = "Storm-" + lucky.getType().getDescription().getString().replace(" ", "") + "-" + (10 + this.random.nextInt(89));
        if (lucky instanceof UnderworldDemonEntity uDemon) {
            uDemon.applyNameEvolution(bestowed, this.getOwner() instanceof Player p ? p : null);
        } else {
            lucky.addTag("TensuraNamedMob");
            lucky.setCustomName(Component.literal("§b§l✦ " + bestowed + " ✦ §7[Veldora Ban Tên]"));
            lucky.setCustomNameVisible(true);
            var hp = lucky.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(Math.min(1800.0D, hp.getBaseValue() * 12.0D));
                lucky.setHealth((float) hp.getValue());
            }
            var atk = lucky.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                atk.setBaseValue(Math.min(120.0D, Math.max(4.0D, atk.getBaseValue()) * 12.0D));
            }
        }
        sl.sendParticles(STORM_CYAN, lucky.getX(), lucky.getY() + 1.2D, lucky.getZ(), 28, 0.6, 0.8, 0.6, 0.08);
        sl.playSound(null, lucky.getX(), lucky.getY(), lucky.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.5F, 1.4F);
        broadcastNearby("§b§l[VELDORA] §eKuahaha! Ngươi khá lắm, ta đã ban danh xưng §f" + bestowed + " §echo ngươi!");
    }

    private void executeStormSkill(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        double dist = this.distanceTo(target);
        boolean againstPrimordial = target instanceof PrimordialDemonEntity;

        if (!isHumanForm()) {
            // DẠNG RỒNG: Mạnh gấp 5 lần Ác Ma Thủy Tổ & ngang ngửa Velgrynd!
            setCastingState(1);
            castingTicks = 18;
            skillCooldown = 48;

            int roll = this.random.nextInt(3);
            if (roll == 0) {
                // Hắc Lôi Bạo Táp (Black Lightning Storm)
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0F, 0.7F);
                sl.sendParticles(BLACK_LIGHTNING, target.getX(), target.getY() + 1.5D, target.getZ(), 45, 2.2, 2.5, 2.2, 0.15);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.5D, target.getZ(), 40, 2.0, 2.0, 2.0, 0.25);

                float dmg = againstPrimordial ? 1150.0F : 340.0F;
                damageStormArea(sl, target.position(), 7.5D, dmg);
            } else if (roll == 1) {
                // Bạo Phong Long Hống (Storm Dragon Roar Beam)
                Vec3 eye = this.getEyePosition();
                Vec3 dir = target.getEyePosition().subtract(eye).normalize();
                for (double d = 2.0D; d <= Math.min(34.0D, dist + 6.0D); d += 1.2D) {
                    Vec3 pt = eye.add(dir.scale(d));
                    sl.sendParticles(STORM_CYAN, pt.x, pt.y, pt.z, 6, 0.6, 0.6, 0.6, 0.05);
                    if (d % 4.0D < 1.3D) {
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    }
                }
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.85F);
                float dmg = againstPrimordial ? 1450.0F : 420.0F;
                damageStormArea(sl, target.position(), 6.0D, dmg);
                if (TensuraSkillHelper.canDestroyTerrain(sl, this)) {
                    sl.explode(this, target.getX(), target.getY(), target.getZ(), 2.5F, Level.ExplosionInteraction.MOB);
                }
            } else {
                // Tử Thần Cuồng Phong (Death Calling Wind Tornado)
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5F, 1.1F);
                for (int h = 0; h < 12; h++) {
                    double angle = h * 0.9D;
                    double rad = 1.5D + h * 0.35D;
                    sl.sendParticles(STORM_CYAN, target.getX() + Math.cos(angle) * rad, target.getY() + h * 0.8D,
                            target.getZ() + Math.sin(angle) * rad, 5, 0.2, 0.2, 0.2, 0.05);
                }
                float dmg = againstPrimordial ? 980.0F : 290.0F;
                damageStormArea(sl, target.position(), 9.0D, dmg);
            }
        } else {
            // DẠNG NGƯỜI: Linh hoạt Veldora-Style Killing Arts + Hadouken Ki Blast
            setCastingState(1);
            castingTicks = 12;
            skillCooldown = 36;

            if (dist > 6.5D) {
                // Tốc biến áp sát + Chưởng Sóng Kamehameha
                Vec3 dashPos = target.position().subtract(target.getLookAngle().scale(2.2D));
                this.teleportTo(dashPos.x, target.getY() + 0.2D, dashPos.z);
            }
            sl.sendParticles(STORM_CYAN, target.getX(), target.getY() + 1.0D, target.getZ(), 26, 0.8, 0.8, 0.8, 0.15);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 1.3F);
            float dmg = againstPrimordial ? 190.0F : 72.0F;
            damageStormArea(sl, target.position(), 4.5D, dmg);
        }
    }

    private void damageStormArea(ServerLevel sl, Vec3 center, double radius, float amount) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
            if (isAllied() && this.getOwner() != null && (hit == this.getOwner()
                    || (hit instanceof TamableAnimal ta && ta.getOwnerUUID() != null && ta.getOwnerUUID().equals(this.getOwnerUUID())))) {
                continue;
            }
            hit.hurt(this.damageSources().mobAttack(this), amount);
            if (hit instanceof PrimordialDemonEntity) {
                hit.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;
        if (isSealed()) {
            if (source.getEntity() instanceof Player p) {
                p.displayClientMessage(Component.literal("§e⚠ Veldora đang bị nhốt trong Long Giam Vô Hạn! Dùng §dAzathoth / Raphael §eđể Phân Tích & Giải Phong Ấn!"), true);
            }
            return false;
        }

        Entity attacker = source.getEntity();
        float adjusted = amount;
        if (!isHumanForm()) {
            // Dạng Rồng có vảy Long Chủng giảm sát thương gấp ~30 lần sức bền so với Dạng Người
            adjusted *= 0.22F;
            if (attacker instanceof PrimordialDemonEntity) {
                // Ác Ma Thủy Tổ tối đa chỉ có ~8% cơ hội hạ Veldora
                adjusted *= 0.12F;
            }
        } else {
            if (attacker instanceof PrimordialDemonEntity) {
                adjusted *= 0.45F;
            }
        }

        return super.hurt(source, adjusted);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.MAGISTEEL_PHONE.get())) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isSealed()) {
            player.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"Này người anh em! Dùng §dThần Hư Không Azathoth §fhoặc §bĐại Hiền Giả/Raphael §fđể giải phong ấn cho ta đi!\""), false);
            return InteractionResult.SUCCESS;
        }

        if (isAllied() && this.isOwnedBy(player)) {
            if (player.isShiftKeyDown()) {
                setHumanForm(!isHumanForm());
                player.displayClientMessage(Component.literal("§b§l[VELDORA] §fĐã chuyển sang " + (isHumanForm() ? "§eDạng Người (Linh Hoạt & Ban Tên)" : "§3Dạng Rồng (Sức Mạnh x30)") + "§f!"), true);
                return InteractionResult.SUCCESS;
            }
            if (isHumanForm()) {
                tryAutoBestowNameNearby();
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    public void broadcastNearby(String msg) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (ServerPlayer sp : sl.players()) {
            if (sp.distanceToSqr(this) <= 48.0D * 48.0D) {
                sp.displayClientMessage(Component.literal(msg), false);
            }
        }
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HumanForm", isHumanForm());
        tag.putBoolean("Sealed", isSealed());
        tag.putBoolean("Allied", isAllied());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHumanForm(tag.getBoolean("HumanForm"));
        setSealed(tag.getBoolean("Sealed"));
        setAllied(tag.getBoolean("Allied"));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
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
}
