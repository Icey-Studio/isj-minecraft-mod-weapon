package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.content.tensura.MagiculesManager;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.content.tensura.MobMeleeSkillSystem;
import com.minhphuc.weapons.content.tensura.PrimordialSkillPool;
import com.minhphuc.weapons.content.tensura.SkillNullificationAbility;
import com.minhphuc.weapons.content.tensura.TensuraSkillHelper;
import com.minhphuc.weapons.entity.darkgathering.CursedIdolEntity;
import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity;
import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import java.util.Set;
import java.util.UUID;

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
    public static final EntityDataAccessor<Boolean> DATA_GREED_CONTROLLED =
            SynchedEntityData.defineId(VeldoraEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§b§l[BẠO PHONG LONG] §f§lVELDORA TEMPEST §7- §3[True Dragon]"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private int skillCooldown = 30;
    private int formSwitchCooldown = 260;
    private int castingTicks = 0;
    private int namingCooldown = 240;
    private int skillSealCooldown = 360;
    private int basicComboIndex = 0;
    private int tempestChargeTicks = 0;
    private boolean usingFlyingNav = false;
    private int raphaelPierceCooldown = 0;

    public void notifyRaphaelAttack(String skillName) {
        if (this.level() instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.players()) {
                if (sp.distanceToSqr(this) <= 64.0D * 64.0D) {
                    com.minhphuc.weapons.content.tensura.SageManager.sendCornerAlert(sp, "« Báo cáo »: « Cá thể [Bạo Phong Long Veldora] » đang chuẩn bị tấn công bằng [" + skillName + "]!", 0xFF00E5FF, 3500, false);
                }
            }
        }
    }

    public VeldoraEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 2500;
        this.moveControl = new MoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 147000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.48D)
                .add(Attributes.FLYING_SPEED, 0.78D)
                .add(Attributes.ATTACK_DAMAGE, 1715.0D)
                .add(Attributes.ARMOR, 84.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUMAN_FORM, false);
        builder.define(DATA_SEALED, false);
        builder.define(DATA_ALLIED, false);
        builder.define(DATA_CASTING_STATE, 0);
        builder.define(DATA_FLYING_NOW, false);
        builder.define(DATA_GREED_CONTROLLED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(2, new DragonFlightGoal(this, 1.35D, () -> !isSealed(), this::getOwner));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.3D, 10.0F, 4.0F) {
            @Override public boolean canUse() { return isHumanForm() && !isSealed() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isHumanForm() && !isSealed() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> {
            if (isSealed() || e == this || !e.isAlive()) return false;
            if (isGreedControlled()) {
                return (e instanceof Player p && !p.isCreative() && !p.isSpectator());
            }
            if (isAllied()) {
                return isHostileToAlliedVeldora(e);
            } else {
                return (e instanceof Player p && !p.isCreative() && !p.isSpectator())
                        || e instanceof PrimordialDemonEntity
                        || e instanceof VelgryndEntity;
            }
        }));
    }

    private boolean isHostileToAlliedVeldora(LivingEntity e) {
        if (e == null || !e.isAlive() || e == this) return false;
        if (!TensuraSkillHelper.isValidSkillTarget(this, e)) return false;
        if (e instanceof Player) return false;
        if (e instanceof AbstractVillager || e instanceof net.minecraft.world.entity.animal.IronGolem) return false;
        if (this.getOwner() != null && e instanceof TamableAnimal ta && this.getOwnerUUID() != null && this.getOwnerUUID().equals(ta.getOwnerUUID())) {
            return false;
        }
        if (e instanceof Enemy || e.getTags().contains("TensuraDemonLordBerserk")) return true;
        if (e instanceof HinataEntity || e instanceof ChronoaEntity || e instanceof VelzardEntity
                || e instanceof VelgryndEntity || e instanceof MilimEntity || e instanceof CharybdisEntity
                || e instanceof OrcDisasterGeldEntity || e instanceof PrimordialDemonEntity
                || e instanceof UnderworldDemonEntity || e instanceof KuboEntity
                || e instanceof DemonLordKuboEntity || e instanceof TaKinhDaiTangEntity
                || e instanceof MarbasEntity || e instanceof CuuTheQuyenHienEntity
                || e instanceof CursedIdolEntity) {
            return true;
        }
        return e instanceof Mob m && m.getTarget() != null && m.getTarget() == this.getOwner();
    }

    public boolean isHumanForm() {
        return this.entityData.get(DATA_HUMAN_FORM);
    }

    public Vec3 getMouthPos() {
        Vec3 look = this.getLookAngle();
        if (isHumanForm()) {
            return this.getEyePosition().add(look.scale(0.5D));
        }
        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() > 1e-4) {
            lookFlat = lookFlat.normalize();
        } else {
            lookFlat = look;
        }
        // Ở Dạng Rồng khổng lồ scale 14x: miệng nằm ở đỉnh đầu vươn ra trước 7.5m và cao 11.5m
        return this.position().add(0, 11.5D, 0)
                .add(lookFlat.scale(7.5D))
                .add(0, look.y * 3.0D, 0);
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
        // Nâng cấp x7 toàn diện cho cả Dạng Rồng lẫn Dạng Người
        double hp = human ? 105000.0D : 147000.0D;
        double atk = human ? 1260.0D : 1715.0D;
        double armor = human ? 64.0D : 84.0D;
        double toughness = human ? 36.0D : 48.0D;
        double speed = human ? 0.56D : 0.46D;

        if (getAttribute(Attributes.MAX_HEALTH) != null) getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
        if (getAttribute(Attributes.ARMOR) != null) getAttribute(Attributes.ARMOR).setBaseValue(armor);
        if (getAttribute(Attributes.ARMOR_TOUGHNESS) != null) getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(toughness);
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        this.setHealth((float) (hp * pct));
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (isHumanForm()) return EntityDimensions.scalable(0.75F, 2.0F);
        return isSealed() ? EntityDimensions.scalable(4.2F, 4.0F) : EntityDimensions.scalable(7.5F, 8.2F);
    }

    public boolean isSealed() {
        return this.entityData.get(DATA_SEALED);
    }

    public void setSealed(boolean sealed) {
        this.entityData.set(DATA_SEALED, sealed);
        this.setNoAi(sealed);
        this.setInvulnerable(sealed);
        this.refreshDimensions();
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

    public boolean isGreedControlled() {
        return this.entityData.get(DATA_GREED_CONTROLLED);
    }

    public void setGreedControlled(boolean controlled) {
        this.entityData.set(DATA_GREED_CONTROLLED, controlled);
        if (controlled) {
            this.setAllied(false);
            this.setTame(false, false);
            this.setOwnerUUID(null);
            this.addTag("GreedControlled");
        } else {
            this.removeTag("GreedControlled");
            this.setAllied(true);
        }
    }

    public boolean tryPurifyFromGreed(Player player) {
        if (!this.isGreedControlled()) return false;

        // Kiểm tra bước 1: Máu < 50%
        if (this.getHealth() > this.getMaxHealth() * 0.50F) {
            player.displayClientMessage(
                    Component.literal("§c« Báo Cáo Raphael »: Thất bại! Mạch ma tố Tham Lam còn quá kiên cố. Cần bào mòn sinh lực Veldora xuống dưới 50% trước!"),
                    true
            );
            return false;
        }

        // Kiểm tra bước 2: Đang bị nhốt trong Lồng Giam Vô Hạn hoặc bị phong ấn
        boolean isTrapped = InfiniteDragonPrisonAbility.isInPrison(this.level(), this.position()) || this.isSealed();
        if (!isTrapped) {
            player.displayClientMessage(
                    Component.literal("§c« Báo Cáo Raphael »: Thất bại! Cần thi triển [Lồng Giam Vô Hạn] để cô lập hoàn toàn mạch liên kết ma tố trước khi thanh tẩy!"),
                    true
            );
            return false;
        }

        // Bước 3: Hóa giải thành công!
        this.setGreedControlled(false);
        this.heal(this.getMaxHealth() * 0.40F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 3.0D, this.getZ(), 80, 2.0D, 3.0D, 2.0D, 0.2D);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.2F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.4F);
        }

        player.sendSystemMessage(
                Component.literal("§6§l[BẠO PHONG LONG VELDORA] §e« Khà khà khà! Rimuru, làm tốt lắm! Cảm ơn cậu đã thức tỉnh ta khỏi cơn ma pháp bẩn thỉu của con nhóc đó! Giờ thì ta cùng cậu quét sạch chúng nó! »")
        );
        player.displayClientMessage(
                Component.literal("§a§l« BÁO CÁO RAPHAEL: GIẢI CỨU THÀNH CÔNG »\n§fMạch linh hồn của Bạo Phong Long Veldora đã được thanh tẩy hoàn toàn!"),
                false
        );
        return true;
    }

    private void syncNavigationMode() {
        if (this.level().isClientSide()) return;
        // Cả Dạng Rồng lẫn Dạng Người của Veldora đều có thể tự do bay lượn khi không bị phong ấn
        boolean shouldFly = !isSealed();
        this.setNoGravity(shouldFly);
        if (shouldFly != usingFlyingNav) {
            usingFlyingNav = shouldFly;
            if (shouldFly) {
                this.moveControl = new FlyingMoveControl(this, 20, true);
                this.navigation = new FlyingPathNavigation(this, this.level());
            } else {
                this.moveControl = new MoveControl(this);
                this.navigation = new GroundPathNavigation(this, this.level());
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
            this.setNoGravity(false);
            this.entityData.set(DATA_FLYING_NOW, false);
            this.setDeltaMovement(Vec3.ZERO);
            if (this.tickCount % 15 == 0 && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.8D, this.getZ(), 10, 1.5, 1.5, 1.5, 0.02);
            }
            return;
        }

        // TỤ BÃO PLASMA HỦY DIỆT: Tụ năng lượng 8 giây (160 ticks)
        if (tempestChargeTicks > 0) {
            tempestChargeTicks--;
            this.setDeltaMovement(0, 0.02D, 0);
            if (this.level() instanceof ServerLevel sl) {
                Vec3 mouthPos = this.getMouthPos();
                // Không áp Darkness để người chơi nhìn rõ toàn bộ hoạt ảnh tụ bão hoành tráng
                // Hạt năng lượng ma tố lôi điện rực sáng xoắn ốc hội tụ CHÍNH XÁC vào đầu / miệng Veldora
                for (int i = 0; i < 14; i++) {
                    double ang = this.random.nextDouble() * Math.PI * 2.0D;
                    double r = 3.5D + this.random.nextDouble() * 7.5D;
                    double px = mouthPos.x + Math.cos(ang) * r;
                    double pz = mouthPos.z + Math.sin(ang) * r;
                    double py = mouthPos.y + (this.random.nextDouble() - 0.5D) * 4.5D;
                    double vx = (mouthPos.x - px) * 0.14D;
                    double vy = (mouthPos.y - py) * 0.14D;
                    double vz = (mouthPos.z - pz) * 0.14D;
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0, vx, vy, vz, 1.0D);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, vx, vy, vz, 1.0D);
                    sl.sendParticles(ParticleTypes.GLOW, px, py, pz, 1, 0, 0, 0, 0.02D);
                }
                if (tempestChargeTicks % 20 == 0) {
                    sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.5F, 0.7F + (160 - tempestChargeTicks) * 0.005F);
                }
                // Hết 8 giây -> Bắn Đại Pháo Plasma Bạo Phong 3D khổng lồ chuẩn điện ảnh như Long Tinh Bạo Viêm Phá!
                if (tempestChargeTicks == 0) {
                    sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.45F);
                    sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 6.0F, 0.65F);
                    sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 5.0F, 0.7F);
                    Vec3 look = this.getLookAngle();

                    // PHÁT GÓI CINEMATIC 3D PLASMA: Khai hỏa trực tiếp từ miệng rồng ở đầu
                    ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                            SkillVisualConfig.SKILL_VELDORA_TEMPEST_CANNON, this.getUUID(), mouthPos, look, 48, ClientboundCinematicSkillPacket.ACTION_START, 1.25f
                    ));

                    for (double d = 2.0D; d <= 80.0D; d += 3.0D) {
                        Vec3 pt = mouthPos.add(look.scale(d));
                        sl.sendParticles(ParticleTypes.FLASH, pt.x, pt.y, pt.z, 2, 0.5D, 0.5D, 0.5D, 0);
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 25, 1.5D, 1.5D, 1.5D, 0.18D);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 15, 1.2D, 1.2D, 1.2D, 0.15D);
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                        AABB hitBox = new AABB(pt.x - 6.0D, pt.y - 6.0D, pt.z - 6.0D, pt.x + 6.0D, pt.y + 6.0D, pt.z + 6.0D);
                        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && !TensuraSkillHelper.isAllied(this, e))) {
                            le.addTag("UrielBypassDamage");
                            le.hurt(this.damageSources().magic(), 7500.0F);
                            le.removeTag("UrielBypassDamage");
                        }
                    }
                    sl.explode(this, mouthPos.x + look.x * 24.0D, mouthPos.y, mouthPos.z + look.z * 24.0D, 8.0F, Level.ExplosionInteraction.MOB);
                }
            }
            return;
        }

        if (this.raphaelPierceCooldown > 0) {
            this.raphaelPierceCooldown--;
        }

        syncNavigationMode();
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()), 0.0F, 1.0F));
        this.entityData.set(DATA_FLYING_NOW, !isSealed() && (!this.onGround() || this.getDeltaMovement().lengthSqr() > 0.001D));

        if (!isHumanForm() && this.level() instanceof ServerLevel sl) {
            DragonFlightGoal.breakBlockingTerrain(sl, this);
        }

        if (this.isGreedControlled() && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getEyeY() + 0.5D, this.getZ(), 3, 0.4, 0.4, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getEyeY(), this.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
        }

        // Siêu hồi phục Long Chủng x7 (+420 HP/s Dragon, +280 HP/s Human)
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(isHumanForm() ? 280.0F : 420.0F);
        }

        if (castingTicks > 0) {
            castingTicks--;
            if (castingTicks == 0) setCastingState(0);
        }

        // Hỗ trợ chủ nhân đánh nhau tích cực & ban buff Bạo Phong Long
        if (isAllied() && this.level() instanceof ServerLevel sl) {
            assistAlliedOwner(sl);
        }
        // Tất cả mob/boss thù địch trong tầm luôn ưu tiên dồn đòn tấn công để ngăn chặn Veldora
        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel sl) {
            enforceMobStopVeldoraAggro(sl);
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
        if (skillSealCooldown > 0) skillSealCooldown--;

        if (SkillNullificationAbility.isEntitySkillSealed(this)) {
            if (this.tickCount % 15 == 0 && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.5D, this.getZ(), 6, 0.5, 0.7, 0.5, 0.02);
            }
            return;
        }

        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Chuyển đổi hình thái tự động chỉ áp dụng khi Veldora hoang dã (!isAllied); khi là đồng minh luôn giữ đúng dạng người chơi chọn
            if (!isAllied()) {
                if (formSwitchCooldown > 0) {
                    formSwitchCooldown--;
                } else {
                    formSwitchCooldown = 300 + this.random.nextInt(180);
                    boolean facingTitan = target instanceof PrimordialDemonEntity || target instanceof VelgryndEntity || target instanceof VelzardEntity || target instanceof MilimEntity;
                    if (isHumanForm() && (facingTitan || this.getHealth() < this.getMaxHealth() * 0.7F)) {
                        setHumanForm(false);
                        broadcastNearby("§b§l[VELDORA] §fKuahaha! Để ta cho ngươi nếm mùi Chân Thân Bạo Phong Long!");
                    } else if (!isHumanForm() && !facingTitan && this.random.nextFloat() < 0.35F) {
                        setHumanForm(true);
                        broadcastNearby("§b§l[VELDORA] §fThử xem Veldora-Style Killing Arts của ta đây!");
                    }
                }
            }

            // Áp sát linh hoạt để không đứng nhìn từ xa
            double distSq = this.distanceToSqr(target);
            if (isHumanForm() && distSq > 6.5D * 6.5D && distSq <= 38.0D * 38.0D && this.tickCount % 32 == 0
                    && this.level() instanceof ServerLevel sl) {
                Vec3 dir = target.position().subtract(this.position()).normalize();
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 9, 1, 1.85F, 14);
                Vec3 blink = target.position().subtract(dir.scale(2.1D));
                this.teleportTo(blink.x, target.getY() + 0.15D, blink.z);
                this.fallDistance = 0.0F;
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.1F, 1.35F);
            } else if (!isHumanForm() && distSq > 16.0D * 16.0D && this.tickCount % 10 == 0) {
                Vec3 dir = target.position().add(0, 2.5D, 0).subtract(this.position()).normalize();
                this.setDeltaMovement(this.getDeltaMovement().scale(0.65D).add(dir.scale(0.45D)));
                this.hurtMarked = true;
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

    private void assistAlliedOwner(ServerLevel sl) {
        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) return;

        // 1. Bảo vệ & truy kích mục tiêu đang giao chiến với chủ nhân
        if (this.tickCount % 8 == 0) {
            LivingEntity cur = this.getTarget();
            if (cur == null || !cur.isAlive() || !isHostileToAlliedVeldora(cur)) {
                LivingEntity threat = owner.getLastHurtByMob();
                if (threat != null && owner.tickCount - owner.getLastHurtByMobTimestamp() <= 160
                        && isHostileToAlliedVeldora(threat) && this.distanceToSqr(threat) <= 64.0D * 64.0D) {
                    this.setTarget(threat);
                } else {
                    LivingEntity hunted = owner.getLastHurtMob();
                    if (hunted != null && owner.tickCount - owner.getLastHurtMobTimestamp() <= 160
                            && isHostileToAlliedVeldora(hunted) && this.distanceToSqr(hunted) <= 64.0D * 64.0D) {
                        this.setTarget(hunted);
                    } else {
                        List<LivingEntity> threats = sl.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(32.0D),
                                e -> isHostileToAlliedVeldora(e) && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == owner)));
                        if (!threats.isEmpty()) {
                            threats.sort((a, b) -> Double.compare(a.distanceToSqr(owner), b.distanceToSqr(owner)));
                            this.setTarget(threats.get(0));
                        }
                    }
                }
            }
        }

        // 2. Hộ tống dịch chuyển về bên cạnh nếu chủ nhân đi quá xa (> 72m)
        if (this.tickCount % 20 == 0 && owner.level() == sl) {
            LivingEntity cur = this.getTarget();
            boolean engagedClose = cur != null && cur.isAlive() && cur.distanceToSqr(owner) <= 45.0D * 45.0D;
            if (!engagedClose && this.distanceToSqr(owner) > 72.0D * 72.0D) {
                Vec3 back = owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(isHumanForm() ? -3.5D : -7.5D));
                this.teleportTo(back.x, owner.getY() + (isHumanForm() ? 0.2D : 2.0D), back.z);
                this.setDeltaMovement(Vec3.ZERO);
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), owner.getLookAngle(), 6, 2, isHumanForm() ? 1.8F : 3.6F, 16);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.3F, 1.1F);
            }
        }

        // 3. Hào quang Bạo Phong Long Gia Trì cho chủ nhân (mỗi 3s)
        if (this.tickCount % 60 == 0 && owner.level() == sl && this.distanceToSqr(owner) <= 28.0D * 28.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, false, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false, true));
            if (owner.getHealth() < owner.getMaxHealth()) {
                owner.heal(4.0F);
            }
            if (owner instanceof ServerPlayer sp) {
                MagiculesManager.onSkillCastPulse(sp, 12_000L);
            }
        }
    }

    private void enforceMobStopVeldoraAggro(ServerLevel sl) {
        if (!this.isAlive() || this.isSealed()) return;
        net.minecraft.world.phys.AABB stopBox = this.getBoundingBox().inflate(56.0D);
        List<Mob> nearbyMobs = sl.getEntitiesOfClass(Mob.class, stopBox, m -> m != this && m.isAlive());
        for (Mob mob : nearbyMobs) {
            if (!shouldMobPrioritizeStoppingVeldora(mob)) continue;
            var followAttr = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (followAttr != null && followAttr.getBaseValue() < 56.0D) {
                followAttr.setBaseValue(56.0D);
            }
            if (mob.getTarget() != this) {
                mob.setTarget(this);
            }
            if (mob instanceof net.minecraft.world.entity.PathfinderMob pMob
                    && this.tickCount % 20 == 0
                    && pMob.distanceToSqr(this) > 9.0D
                    && pMob.getNavigation().isDone()) {
                pMob.getNavigation().moveTo(this, 1.25D);
            }
        }
    }

    private boolean shouldMobPrioritizeStoppingVeldora(Mob mob) {
        if (mob == null || !mob.isAlive() || mob == this) return false;
        if (TensuraSkillHelper.isAllied(this, mob)) return false;
        if (mob instanceof AbstractVillager || mob.getTags().contains("DemonLordVillager")) return false;
        if (mob instanceof MilimEntity) return false;
        if (this.isAllied()) {
            UUID ownerUuid = this.getOwnerUUID();
            if (ownerUuid != null && mob instanceof TamableAnimal ta && ownerUuid.equals(ta.getOwnerUUID())) {
                return false;
            }
            if (mob instanceof VelgryndEntity vg && vg.isAllied()) return false;
            if (mob instanceof PrimordialDemonEntity pde && pde.isTame()) return false;
            if (mob instanceof ElementalSpiritEntity es && es.isTame()) return false;
            if (mob instanceof UnderworldDemonEntity ude && ude.isTame() && !ude.isBerserkDemonLord()) return false;
            if (mob instanceof CuuTheQuyenHienEntity cqh && cqh.isCalmControlledBy(ownerUuid)) return false;
            if (mob instanceof TaKinhDaiTangEntity tk && tk.isCalmControlledBy(ownerUuid)) return false;
            if (mob instanceof net.minecraft.world.entity.animal.IronGolem) return false;
        }
        if (mob instanceof Enemy || mob.getTags().contains("TensuraDemonLordBerserk")) return true;
        if (!this.isAllied() && mob instanceof net.minecraft.world.entity.animal.IronGolem) return true;
        if (mob instanceof VelzardEntity) return true;
        if (mob instanceof VelgryndEntity vg && !vg.isAllied()) return true;
        if (mob instanceof HinataEntity) return true;
        if (mob instanceof ChronoaEntity c && c.isCorrupted()) return true;
        if (mob instanceof CharybdisEntity) return true;
        if (mob instanceof OrcDisasterGeldEntity) return true;
        if (mob instanceof PrimordialDemonEntity pde && !pde.isTame()) return true;
        return mob instanceof UnderworldDemonEntity ude && (!ude.isTame() || ude.isBerserkDemonLord());
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

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel sl)) return false;
        if (!(target instanceof LivingEntity victim) || !TensuraSkillHelper.isValidSkillTarget(this, victim)) {
            return false;
        }
        this.swing(InteractionHand.MAIN_HAND);
        if (victim instanceof Player p && p.isBlocking()) {
            p.disableShield();
        }

        Vec3 dir = victim.getEyePosition().subtract(this.getEyePosition());
        if (dir.lengthSqr() < 1e-4) dir = this.getLookAngle();
        dir = dir.normalize();

        float atk = (float) Math.max(400.0D, this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        int move = basicComboIndex;

        if (isHumanForm()) {
            basicComboIndex = (basicComboIndex + 1) % 6;
            float scale = 2.05F;
            switch (move) {
                case 0 -> { // Hổ Quyền Xung Kích (Sonic Punch Cone)
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 4, 1, scale, 14);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1.8F, 0.9F);
                    applyVeldoraStrike(sl, victim, atk * 1.15F, dir.scale(1.1D), 0.25D, false);
                }
                case 1 -> { // Long Trảo Song Trảm (X-Rend)
                    MobMeleeSkillSystem.sendCqc3d(sl, this, victim.position(), dir, 3, 2, scale, 14);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 1.8F, 1.15F);
                    applyVeldoraStrike(sl, victim, atk * 1.25F, dir.scale(0.6D), 0.2D, true);
                }
                case 2 -> { // Đá Quét Bão Tố 360
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 5, 2, scale * 1.25F, 16);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 2.0F, 0.8F);
                    damageStormArea(sl, this.position(), 5.5D, atk * 1.10F, false);
                }
                case 3 -> { // Thăng Long Cước (Uppercut Sky Tornado)
                    MobMeleeSkillSystem.sendCqc3d(sl, this, victim.position(), dir, 7, 1, scale, 16);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1.9F, 1.1F);
                    applyVeldoraStrike(sl, victim, atk * 1.20F, new Vec3(dir.x * 0.4D, 1.05D, dir.z * 0.4D), 1.05D, false);
                }
                case 4 -> { // Liên Hoàn Thủ Đao (Multi-Pierce Beams)
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position().add(dir.scale(0.8D)), dir, 1, 2, scale * 1.1F, 14);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 1.3F, 1.4F);
                    applyVeldoraStrike(sl, victim, atk * 1.35F, dir.scale(0.85D), 0.2D, true);
                }
                default -> { // Chấn Địa Bạo Quyền (Seismic Shock)
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 2, 1, scale * 1.2F, 16);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.6F, 0.85F);
                    damageStormArea(sl, this.position(), 6.0D, atk * 1.25F, true);
                }
            }
        } else {
            basicComboIndex = (basicComboIndex + 1) % 5;
            float scale = 4.6F;
            switch (move) {
                case 0 -> { // Long Trảo Xé Không
                    MobMeleeSkillSystem.sendCqc3d(sl, this, victim.position(), dir, 3, 2, scale, 18);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 2.2F, 1.1F);
                    damageStormArea(sl, victim.position(), 7.5D, atk * 1.15F, true);
                }
                case 1 -> { // Quật Đuôi Bão Phong
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 5, 1, scale * 1.25F, 20);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 2.5F, 0.65F);
                    damageStormArea(sl, this.position(), 11.0D, atk * 1.10F, false);
                }
                case 2 -> { // Húc Đầu Siêu Thanh
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 4, 2, scale, 18);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 2.0F, 0.9F);
                    applyVeldoraStrike(sl, victim, atk * 1.35F, dir.scale(2.4D), 0.7D, true);
                }
                case 3 -> { // Vỗ Cánh Cuồng Phong
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 10, 2, scale * 1.15F, 22);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.NEUTRAL, 2.4F, 0.65F);
                    damageStormArea(sl, this.position(), 12.0D, atk * 1.05F, false);
                }
                default -> { // Dậm Chân Long Địa Chấn
                    MobMeleeSkillSystem.sendCqc3d(sl, this, victim.position(), dir, 2, 1, scale * 1.2F, 20);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 2.4F, 0.6F);
                    damageStormArea(sl, victim.position(), 9.5D, atk * 1.28F, true);
                }
            }
        }
        return true;
    }

    private void executeStormSkill(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || !TensuraSkillHelper.isValidSkillTarget(this, target)) return;
        double dist = this.distanceTo(target);
        boolean againstPrimordial = target instanceof PrimordialDemonEntity;

        if (this.random.nextFloat() < 0.28F) {
            setCastingState(1);
            castingTicks = 16;
            skillCooldown = isHumanForm() ? 28 : 32;
            PrimordialSkillPool.castRandomTrueDragonSkill(
                    sl,
                    this,
                    target,
                    isHumanForm() ? 1260.0D : 1820.0D,
                    isHumanForm() ? 1.90F : 2.50F,
                    (victim, dmg) -> applyVeldoraStrike(sl, victim, dmg, Vec3.ZERO, 0.15D, true)
            );
            return;
        }

        if (!isHumanForm()) {
            // DẠNG RỒNG: Bộ đòn bắn phá 3D tầm xa + hủy diệt x7
            setCastingState(1);
            castingTicks = 18;
            skillCooldown = 30;

            if (skillSealCooldown <= 0 && !isAllied() && dist <= 34.0D) {
                SkillNullificationAbility.castBossSkillSeal(sl, this, 34.0F, 180);
                skillSealCooldown = 360;
                return;
            }

            Vec3 eye = this.getEyePosition();
            Vec3 aim = target.getEyePosition().subtract(eye);
            Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : this.getLookAngle();

            int roll = this.random.nextInt(5);
            if (roll == 0) {
                // 1. Vạn Lôi Quy Tông (Cataclysmic Tempest Bolt)
                notifyRaphaelAttack("Vạn Lôi Quy Tông");
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 3.2F, 0.65F);
                Vec3 center = target.position();
                for (int i = 0; i < 5; i++) {
                    double ang = i * (Math.PI * 2.0D / 5.0D);
                    double r = (i == 0) ? 0.0D : 4.2D;
                    Vec3 strikePos = center.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
                    MobMeleeSkillSystem.sendCqc3d(sl, this, strikePos, new Vec3(0, 1, 0), 8, 9, 3.2F, 22);
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(strikePos.x, strikePos.y, strikePos.z);
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                }
                MobMeleeSkillSystem.sendCqc3d(sl, this, center, dir, 2, 1, 4.8F, 24);
                damageStormArea(sl, center, 10.5D, againstPrimordial ? 3600.0F : 2250.0F, true);
            } else if (roll == 1) {
                // 2. Cuồng Long Bạo Phong Hống (Thổi Bay Địa Hình & Sinh Vật)
                notifyRaphaelAttack("Cuồng Long Bạo Phong Hống");
                double beamLen = Math.min(36.0D, Math.max(18.0D, dist + 6.0D));
                for (double d = 4.0D; d <= beamLen; d += 6.0D) {
                    Vec3 pt = eye.add(dir.scale(d));
                    MobMeleeSkillSystem.sendCqc3d(sl, this, pt, dir, 4, 2, 3.8F, 20);
                    sl.sendParticles(STORM_CYAN, pt.x, pt.y, pt.z, 16, 0.8, 0.8, 0.8, 0.08);
                }
                MobMeleeSkillSystem.sendCqc3d(sl, this, eye, dir, 1, 1, 5.2F, 24);
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 2, 2, 4.6F, 24);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 3.4F, 0.8F);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 3.0F, 0.9F);
                damageStormArea(sl, target.position(), 10.5D, againstPrimordial ? 4200.0F : 2550.0F, true);

                // Phá hủy địa hình: bẻ gãy khối, cây cối, nhà cửa
                BlockPos centerPos = target.blockPosition();
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -1; dy <= 3; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            BlockPos bp = centerPos.offset(dx, dy, dz);
                            BlockState bs = sl.getBlockState(bp);
                            if (!bs.isAir() && bs.getDestroySpeed(sl, bp) >= 0.0F && bs.getDestroySpeed(sl, bp) <= 4.0F) {
                                sl.destroyBlock(bp, false);
                            }
                        }
                    }
                }
                sl.explode(this, target.getX(), target.getY(), target.getZ(), 3.8F, Level.ExplosionInteraction.MOB);
            } else if (roll == 2) {
                // 3. Hắc Lôi Long Tức (Không Phá Địa Hình, Quét Bốc Hơi Quái Yếu)
                notifyRaphaelAttack("Hắc Lôi Long Tức");
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 2.8F, 0.9F);
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 8, 2, 5.2F, 24);
                for (double d = 2.0D; d <= 28.0D; d += 3.5D) {
                    Vec3 p = eye.add(dir.scale(d));
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 8, 0.5D, 0.5D, 0.5D, 0.05D);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 6, 0.4D, 0.4D, 0.4D, 0.08D);
                }
                AABB breathBox = new AABB(target.position(), target.position()).inflate(8.0D);
                for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, breathBox, e -> e != this && !TensuraSkillHelper.isAllied(this, e))) {
                    if (le.getHealth() <= 100.0F) {
                        le.hurt(this.damageSources().magic(), 9999.0F); // Bốc hơi quái yếu
                    } else {
                        applyVeldoraStrike(sl, le, againstPrimordial ? 3500.0F : 2100.0F, dir.scale(0.8D), 0.2D, true);
                    }
                }
            } else if (roll == 3) {
                // 4. Long Trảo Liệt Không (Vết Nứt Không Gian Nổ Tung)
                notifyRaphaelAttack("Long Trảo Liệt Không");
                for (int claw = -1; claw <= 1; claw++) {
                    Vec3 offset = dir.cross(new Vec3(0, 1, 0)).scale(claw * 2.2D);
                    MobMeleeSkillSystem.sendCqc3d(sl, this, target.position().add(offset), dir, 3, 2, 4.2F, 20);
                }
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 2.8F, 1.2F);
                damageStormArea(sl, target.position(), 8.0D, againstPrimordial ? 3200.0F : 2000.0F, true);
            } else {
                // 5. Tụ Bão Plasma Hủy Diệt (Kích hoạt Tụ Chiêu 8 Giây)
                if (tempestChargeTicks == 0) {
                    notifyRaphaelAttack("TỤ BÃO PLASMA HỦY DIỆT (Đang Tụ Chiêu 8 Giây)");
                    tempestChargeTicks = 160;
                    skillCooldown = 180;
                }
            }
        } else {
            // DẠNG NGƯỜI: 6 Ma Pháp Bạo Phong & Veldora-Style Killing Arts 3D!
            setCastingState(1);
            castingTicks = 14;
            skillCooldown = 26;

            if (skillSealCooldown <= 0 && !isAllied() && dist <= 24.0D) {
                SkillNullificationAbility.castBossSkillSeal(sl, this, 24.0F, 160);
                skillSealCooldown = 360;
                return;
            }

            Vec3 eye = this.getEyePosition();
            Vec3 aim = target.getEyePosition().subtract(eye);
            Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : this.getLookAngle();

            int roll = this.random.nextInt(6);
            switch (roll) {
                case 0 -> { // 1. Ba Động Quyền: Hadouken 3D (Nổ Uy Lực, Không Phá Khối, Chống Lag)
                    notifyRaphaelAttack("Ba Động Quyền: Hadouken 3D");
                    double reach = Math.min(26.0D, Math.max(12.0D, dist + 4.0D));
                    for (double d = 3.0D; d <= reach; d += 5.0D) {
                        MobMeleeSkillSystem.sendCqc3d(sl, this, eye.add(dir.scale(d)), dir, 4, 2, 2.5F, 18);
                    }
                    MobMeleeSkillSystem.sendCqc3d(sl, this, eye, dir, 1, 1, 2.8F, 20);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 2.2F, 1.15F);
                    damageStormArea(sl, target.position(), 6.5D, againstPrimordial ? 2600.0F : 1750.0F, true);
                }
                case 1 -> { // 2. Khí Công Đạn Liên Thanh 3D (8 Khối Cầu Ma Tố 3D)
                    notifyRaphaelAttack("Khí Công Đạn Liên Thanh 3D");
                    for (int orb = -2; orb <= 2; orb++) {
                        Vec3 spread = dir.yRot(orb * 0.15F).normalize();
                        MobMeleeSkillSystem.sendCqc3d(sl, this, eye.add(spread.scale(2.0D)), spread, 5, 9, 2.0F, 16);
                    }
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.NEUTRAL, 2.0F, 1.2F);
                    damageStormArea(sl, target.position(), 7.0D, againstPrimordial ? 2400.0F : 1600.0F, false);
                }
                case 2 -> { // 3. Bản Năng Chân Long Né Đòn & Tốc Biến Trảm 3D
                    notifyRaphaelAttack("Bản Năng Chân Long: Tốc Biến Trảm 3D");
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 9, 2, 2.2F, 14);
                    Vec3 dashPos = target.position().subtract(target.getLookAngle().scale(2.0D));
                    this.teleportTo(dashPos.x, target.getY() + 0.15D, dashPos.z);
                    this.fallDistance = 0.0F;
                    MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 3, 1, 2.9F, 18);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 2.0F, 1.35F);
                    applyVeldoraStrike(sl, target, againstPrimordial ? 2750.0F : 1900.0F, dir.scale(1.1D), 0.35D, true);
                }
                case 3 -> { // 4. Long Trụy Oanh Tạc: Dragon Drop (Nhảy Cao Dậm Gót Sấm Sét)
                    notifyRaphaelAttack("Long Trụy Oanh Tạc: Dragon Drop");
                    this.teleportTo(target.getX(), target.getY() + 8.0D, target.getZ());
                    this.setDeltaMovement(0, -1.8D, 0);
                    MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), new Vec3(0, -1, 0), 2, 1, 3.6F, 22);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 2.5F, 0.75F);
                    damageStormArea(sl, target.position(), 8.5D, againstPrimordial ? 2800.0F : 1950.0F, true);
                }
                case 4 -> { // 5. Bá Khí Long Linh (Tê Liệt & Kinh Hãi / Chết Sốc)
                    notifyRaphaelAttack("Bá Khí Long Linh");
                    MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 6, 1, 3.2F, 24);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.NEUTRAL, 2.5F, 1.0F);
                    AABB roarBox = this.getBoundingBox().inflate(14.0D);
                    for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, roarBox, e -> e != this && !TensuraSkillHelper.isAllied(this, e))) {
                        le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                        le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3));
                        if (le.getHealth() <= 80.0F && this.random.nextFloat() < 0.60F) {
                            le.hurt(this.damageSources().magic(), 999.0F); // Chết sốc
                        }
                    }
                }
                default -> { // 6. Tối Thượng Quyền: Tuyệt Kỹ Veldora 3D
                    notifyRaphaelAttack("Tối Thượng Quyền: Tuyệt Kỹ Veldora 3D");
                    MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 1, 1, 3.5F, 24);
                    MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 2, 2, 3.0F, 20);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 2.8F, 0.9F);
                    damageStormArea(sl, target.position(), 7.5D, againstPrimordial ? 3200.0F : 2200.0F, true);
                }
            }
        }
    }

    private void applyVeldoraStrike(ServerLevel sl, LivingEntity victim, float amount, Vec3 knock, double yBoost, boolean armorPierce) {
        if (victim == null || !victim.isAlive() || !TensuraSkillHelper.isValidSkillTarget(this, victim)) return;
        victim.invulnerableTime = 0;

        // Đòn đánh của Veldora chứa Chân Long Ma Tố có khả năng xuyên thấu toàn bộ giáp trụ & Tuyệt Đối Phòng Ngự Uriel
        victim.addTag("UrielBypassDamage");
        if (armorPierce) {
            victim.hurt(this.damageSources().magic(), amount * 0.55F);
            victim.hurt(this.damageSources().mobAttack(this), amount * 0.65F);
        } else {
            victim.hurt(this.damageSources().magic(), amount * 0.35F);
            victim.hurt(this.damageSources().mobAttack(this), amount * 0.80F);
        }
        victim.removeTag("UrielBypassDamage");

        if (victim instanceof ServerPlayer sp && this.raphaelPierceCooldown <= 0) {
            this.raphaelPierceCooldown = 120; // 6s gửi thông báo 1 lần tránh spam
            com.minhphuc.weapons.content.tensura.SageManager.sendCornerAlert(sp,
                    "« Cảnh báo »: « Cá thể [Bạo Phong Long Veldora] » sở hữu Chân Long Ma Tố có khả năng xuyên thấu toàn bộ giáp trụ và phòng ngự!",
                    0xFFEF4444, 4500, false);
        }

        if (knock.lengthSqr() > 1e-4 || yBoost != 0.0D) {
            victim.push(knock.x, yBoost, knock.z);
            victim.hurtMarked = true;
        }
        if (victim instanceof PrimordialDemonEntity) {
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
        }
    }

    private void damageStormArea(ServerLevel sl, Vec3 center, double radius, float amount, boolean armorPierce) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
            if (!TensuraSkillHelper.isValidSkillTarget(this, hit)) continue;
            Vec3 away = hit.position().subtract(center);
            Vec3 knock = away.lengthSqr() > 1e-4 ? away.normalize().scale(0.85D) : Vec3.ZERO;
            applyVeldoraStrike(sl, hit, amount, knock, 0.25D, armorPierce);
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
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FREEZE) || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }

        Entity attacker = source.getEntity();
        if (isAllied() && attacker != null && TensuraSkillHelper.isAllied(this, attacker)) {
            return false;
        }

        // Miễn nhiễm sát thương bởi vũ khí thường và chiêu thức phổ thông của quái vật
        if (attacker instanceof LivingEntity livingAttacker) {
            boolean isMundane = !(livingAttacker instanceof PrimordialDemonEntity)
                    && !(livingAttacker instanceof VelgryndEntity)
                    && !(livingAttacker instanceof VelzardEntity)
                    && !(livingAttacker instanceof MilimEntity)
                    && !(livingAttacker instanceof Player)
                    && livingAttacker.getMaxHealth() <= 120.0F;

            if (isMundane || source.is(DamageTypes.ARROW)) {
                if (this.tickCount % 35 == 0 && this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.5D, this.getZ(), 12, 0.4D, 0.6D, 0.4D, 0.1D);
                    for (ServerPlayer sp : sl.players()) {
                        if (sp.distanceToSqr(this) <= 48.0D * 48.0D) {
                            com.minhphuc.weapons.content.tensura.SageManager.sendCornerAlert(sp, "« Báo cáo »: « Cá thể [Bạo Phong Long Veldora] » sở hữu [Hộ Thể Chân Long Lân] - Đòn tấn công phổ thông bị triệt tiêu hoàn toàn!", 0xFF38BDF8, 3000, false);
                        }
                    }
                }
                return false;
            }
        }
        float adjusted = amount;
        if (!isHumanForm()) {
            // Sức bền Dạng Rồng x7
            adjusted *= 0.14F;
            if (attacker instanceof PrimordialDemonEntity) {
                adjusted *= 0.06F;
            }
        } else {
            // Sức bền Dạng Người Bạo Phong x7
            adjusted *= 0.22F;
            if (attacker instanceof PrimordialDemonEntity) {
                adjusted *= 0.10F;
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
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            com.minhphuc.weapons.world.VeldoraWorldData vd = com.minhphuc.weapons.world.VeldoraWorldData.get(sl.getServer().overworld());
            vd.setVeldoraDead(true, this.isHumanForm());
            if (!isSealed()) {
                broadcastNearby("§c§l[BẠO PHONG LONG] §fVeldora Tempest đã ngã xuống! Dùng §bGiải Phóng Bạo Phong Long §f(hoặc nút §cThu Hồi / Hồi Sinh Veldora§f, tiêu hao 70% HP & Ma Tố) để hồi sinh ông ấy!");
            }
        }
        super.die(source);
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
