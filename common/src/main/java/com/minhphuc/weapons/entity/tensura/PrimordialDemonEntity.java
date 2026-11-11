package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.divine.PurificationPillarAbility;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager;
import com.minhphuc.weapons.content.tensura.DarkFlameShotAbility;
import com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility;
import com.minhphuc.weapons.content.tensura.DragonNovaAbility;
import com.minhphuc.weapons.content.tensura.HellflareAbility;
import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import com.minhphuc.weapons.content.tensura.NihilityCollapseAbility;
import com.minhphuc.weapons.content.tensura.SupernovaAbility;
import com.minhphuc.weapons.content.tensura.VoiceOfTheWorld;
import com.minhphuc.weapons.content.tensura.PrimordialPactItem;
import com.minhphuc.weapons.content.tensura.PrimordialSkillPool;
import com.minhphuc.weapons.content.tensura.HorizontalHolyBeamAbility;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.init.ModSoundEvents;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class PrimordialDemonEntity extends TamableAnimal {

    private static final EntityDataAccessor<Integer> DATA_DEMON_TYPE =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_WINGED =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_RISING =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_RISING_PROGRESS =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_COMBAT_CLAWS =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BODY =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_NAMED =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_EVOLUTION_TIER =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_CUSTOM_NAME =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_MASSACRE_MODE =
            SynchedEntityData.defineId(PrimordialDemonEntity.class, EntityDataSerializers.BOOLEAN);

    private final List<PrimordialSkillPool.SkillEntry> randomSkills = new ArrayList<>();
    private BlockPos guardCenterPos = null;

    private int skillCooldownTicks = 0;
    private int sealedTicks = 0; // Thời gian bị Phong Sát Kỹ Năng (Skill Nullification Seal)
    public float wingFlapAngle = 0.0F;
    public float prevWingFlapAngle = 0.0F;

    // Hỗ trợ triệu hồi tổ đội (Rouge gọi Rain & Misery)
    private boolean isSupportMinion = false;
    private boolean hasSummonedMinions = false;

    public boolean isSupportMinion() {
        return this.isSupportMinion;
    }

    public void setSupportMinion(boolean support) {
        this.isSupportMinion = support;
    }

    // Lãnh Địa (Domain Expansion)
    public int activeDomainTicks = 0;
    public Vec3 domainCenter = null;
    public DemonType activeDomainDemonType = null;
    private final List<BlockPos> domainBarrierBlocks = new ArrayList<>();

    // Quản lý các ma trận xoay động (ItemDisplay)
    private final List<ActiveDisplayCircle> activeCircles = new ArrayList<>();

    public static class ActiveDisplayCircle {
        public final Display.ItemDisplay display;
        public int ticksRemaining;
        public float currentAngle;
        public final float rotationSpeed;
        public final float scale;
        public final boolean isHorizontal;
        public final Vec3 relativeOffset;

        public ActiveDisplayCircle(Display.ItemDisplay display, int ticksRemaining, float rotationSpeed, float scale, boolean isHorizontal, Vec3 relativeOffset) {
            this.display = display;
            this.ticksRemaining = ticksRemaining;
            this.rotationSpeed = rotationSpeed;
            this.scale = scale;
            this.isHorizontal = isHorizontal;
            this.relativeOffset = relativeOffset;
            this.currentAngle = 0.0F;
        }
    }

    public PrimordialDemonEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setTame(false, false);
        this.moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 18, true);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        net.minecraft.world.entity.ai.navigation.FlyingPathNavigation nav = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            } else {
                this.moveRelative(this.getSpeed(), travelVector);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.91D));
            }
        }
        this.calculateEntityAnimation(false);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 3600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.ATTACK_DAMAGE, 160.0D)
                .add(Attributes.ARMOR, 24.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D)
                .add(Attributes.FOLLOW_RANGE, 56.0D);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType) {
        if (spawnType == net.minecraft.world.entity.MobSpawnType.NATURAL || spawnType == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) {
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) {
                    return false;
                }
                AABB checkArea = new AABB(this.blockPosition()).inflate(64.0D);
                if (!sl.getEntitiesOfClass(PrimordialDemonEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }
            BlockPos pos = this.blockPosition();
            if (pos.getY() < 50) {
                return false;
            }
        }
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && super.checkSpawnRules(level, spawnType);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEMON_TYPE, DemonType.NOIR.ordinal());
        builder.define(DATA_IS_WINGED, false);
        builder.define(DATA_IS_RISING, false);
        builder.define(DATA_RISING_PROGRESS, 0.0F);
        builder.define(DATA_COMBAT_CLAWS, false);
        builder.define(DATA_HAS_BODY, false);
        builder.define(DATA_IS_NAMED, false);
        builder.define(DATA_EVOLUTION_TIER, 0);
        builder.define(DATA_CUSTOM_NAME, "");
        builder.define(DATA_MASSACRE_MODE, false);
    }

    public boolean isMassacreMode() {
        return this.entityData.get(DATA_MASSACRE_MODE);
    }

    public void setMassacreMode(boolean active) {
        this.entityData.set(DATA_MASSACRE_MODE, active);
        if (active) {
            this.setOrderedToSit(false);
            this.guardCenterPos = null;
        } else if (this.getTarget() != null && isFriendlyCreature(this.getTarget())) {
            this.setTarget(null);
        }
    }

    public BlockPos getGuardCenterPos() {
        return this.guardCenterPos;
    }

    public void setGuardMode(boolean guarding, @org.jetbrains.annotations.Nullable BlockPos center) {
        this.setOrderedToSit(guarding);
        this.guardCenterPos = guarding ? (center != null ? center : this.blockPosition()) : null;
        if (guarding) {
            this.setMassacreMode(false);
            this.setPersistenceRequired();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        if (this.isTame() || this.isOrderedToSit() || this.guardCenterPos != null || this.isPersistenceRequired()) {
            return false;
        }
        return super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType spawnType,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);

        // Random DemonType khi spawn tự nhiên (không phải triệu hồi)
        if (spawnType == net.minecraft.world.entity.MobSpawnType.NATURAL
                || spawnType == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) {
            // Tỷ lệ spawn theo độ hiếm (tổng = 100):
            // Rouge: 5%  | Noir: 5%   (Hiếm nhất - Thủy Tổ mạnh nhất)
            // Blanc: 10% | Jaune: 10% (Hiếm - Thủy Tổ mạnh)
            // Violet: 20%             (Trung bình)
            // Bleu: 25%  | Vert: 25%  (Phổ biến nhất - Thủy Tổ yếu hơn)
            int roll = this.random.nextInt(100);
            DemonType chosen;
            if (roll < 5) {
                chosen = DemonType.ROUGE;       // 0-4:   5%
            } else if (roll < 10) {
                chosen = DemonType.NOIR;        // 5-9:   5%
            } else if (roll < 20) {
                chosen = DemonType.BLANC;       // 10-19: 10%
            } else if (roll < 30) {
                chosen = DemonType.JAUNE;       // 20-29: 10%
            } else if (roll < 50) {
                chosen = DemonType.VIOLET;      // 30-49: 20%
            } else if (roll < 75) {
                chosen = DemonType.BLEU;        // 50-74: 25%
            } else {
                chosen = DemonType.VERT;        // 75-99: 25%
            }
            this.setDemonType(chosen);
            this.setWinged(true); // Toàn bộ Thủy Tổ Ác Ma đều sở hữu cánh và khả năng bay

            // 25% cơ hội Thủy Tổ tự nhiên giáng thế đã có Thể Xác Vật Lý (nhưng chưa tiến hóa Ma Vương)
            if (this.random.nextFloat() < 0.25F) {
                this.setPhysicalBody(true);
                this.setEvolutionTier(1);
                this.updateAttributesForType(chosen);
                this.setHealth(this.getMaxHealth());
                this.updateDemonNametag();
                if (chosen == DemonType.JAUNE) {
                    this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOLDEN_GUN.get()));
                }
                ServerLevel sl = level.getLevel();
                if (sl != null) {
                    BlockPos p = this.blockPosition();
                    String vMsg = "Cảnh báo. Phát hiện Thủy Tổ Ác Ma " + chosen.getColorName() + " (" + chosen.getTitleVi() + ") đã hiện thân cùng Thể Xác Vật Lý tại tọa độ [" + p.getX() + ", " + p.getY() + ", " + p.getZ() + "]!";
                    for (ServerPlayer sp : sl.players()) {
                        VoiceOfTheWorld.announce(sp, vMsg);
                    }
                }
            }
        }

        return spawnData;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PrimordialBreakSpawnerGoal(this));
        this.goalSelector.addGoal(2, new BlancMassacreVillageGoal(this));
        this.goalSelector.addGoal(3, new PrimordialSkillGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(5, new PrimordialPatrolGuardGoal(this));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.25D, 7.0F, 2.5F) {
            @Override
            public boolean canUse() {
                if (PrimordialDemonEntity.this.isOrderedToSit()
                        || PrimordialDemonEntity.this.guardCenterPos != null
                        || PrimordialDemonEntity.this.isMassacreMode()) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                if (PrimordialDemonEntity.this.isOrderedToSit()
                        || PrimordialDemonEntity.this.guardCenterPos != null
                        || PrimordialDemonEntity.this.isMassacreMode()) {
                    return false;
                }
                return super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new PrimordialVillageServiceGoal(this));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        // THỦY TỔ CHƯA THU PHỤC: ƯU TIÊN SỐ 1 TẤN CÔNG ÁC MA ĐÃ KÝ KHẾ ƯỚC CỦA NGƯỜI CHƠI TRƯỚC!
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, PrimordialDemonEntity.class, 10, true, false,
                e -> !this.isTame() && e instanceof PrimordialDemonEntity pde && pde.isTame() && pde.isAlive()));

        // Ác ma ĐÃ KHẾ ƯỚC: Tự động khóa và bảo vệ chủ nhân trước các Ác Ma hoang dã
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, PrimordialDemonEntity.class, 10, true, false,
                e -> this.isTame() && e instanceof PrimordialDemonEntity pde && !pde.isTame() && pde.isAlive()));

        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        // Khi CHƯA THU PHỤC: Thử thách người chơi
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !this.isTame()));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> e instanceof Enemy));
    }

    public DemonType getDemonType() {
        int idx = this.entityData.get(DATA_DEMON_TYPE);
        return DemonType.byIndex(idx);
    }

    public void setDemonType(DemonType type) {
        this.entityData.set(DATA_DEMON_TYPE, type.ordinal());
        updateAttributesForType(type);
        updateDemonNametag();
    }

    public void updateAttributesForType(DemonType type) {
        if (type == null) return;
        // 1. Thể xác vật lý: Sức mạnh tăng 30 lần, Máu tăng 4 lần
        double hpMul = hasPhysicalBody() ? 4.0D : 1.0D;
        double dmgMul = hasPhysicalBody() ? 30.0D : 1.0D;

        // 2. Ban Danh Xưng (Tensura Naming): Máu x30, Sức mạnh x30
        if (isNamed() || this.getTags().contains("TensuraNamedMob")) {
            hpMul *= 30.0D;
            dmgMul *= 30.0D;
        }

        // 3. Tiến hóa Ma Vương (Awakened Demon Lord): Máu x20, Sức mạnh x20
        boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
        if (isDemonLord) {
            hpMul *= 20.0D;
            dmgMul *= 20.0D;
        }

        // Cánh Hắc Sắc Noir thưởng thêm
        if (isWinged() && type == DemonType.NOIR) {
            hpMul *= 1.2D;
            dmgMul *= 1.25D;
        }

        var maxHpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            double hp = type.getMaxHealth() * hpMul;
            maxHpAttr.setBaseValue(hp);
            if (this.getHealth() > (float) hp) {
                this.setHealth((float) hp);
            }
        }
        var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            double dmg = type.getAttackDamage() * dmgMul;
            dmgAttr.setBaseValue(dmg);
        }
    }

    public long getCalculatedEP() {
        DemonType type = getDemonType();
        long baseEp = switch (type) {
            case ROUGE -> 2800000L;
            case NOIR -> 2500000L;
            case BLANC, JAUNE, VIOLET -> 2000000L;
            case BLEU, VERT -> 1500000L;
            default -> 1500000L;
        };

        // Khi có thể xác vật lý: EP gấp 3 lần!
        double epMul = hasPhysicalBody() ? 3.0D : 1.0D;

        // Nếu có ban tên: EP nhân 2.5 lần
        if (isNamed() || this.getTags().contains("TensuraNamedMob")) {
            epMul *= 2.5D;
        }

        // Nếu tiến hóa Ma Vương: EP nhân 6.0 lần (hoặc đạt ngưỡng Chân Ma Vương tối thiểu 40,000,000 EP)
        boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
        if (isDemonLord) {
            epMul *= 6.0D;
        }

        long finalEp = (long) (baseEp * epMul);
        if (isDemonLord && finalEp < 40000000L) {
            finalEp = 40000000L + (hasPhysicalBody() ? 20000000L : 0L);
        }
        return finalEp;
    }

    public String getDemonRankTitle() {
        boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
        if (isDemonLord) {
            return "✦ Chân Ma Vương Thức Tỉnh";
        }
        return "★ Ma Tướng Thượng Cấp";
    }

    public void updateDemonNametag() {
        DemonType type = getDemonType();
        String colorPrefix = type != null ? type.getColorName() : "Thủy Tổ";
        boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
        String effectiveName = (getCustomDemonName() != null && !getCustomDemonName().isEmpty()) ? getCustomDemonName() : colorPrefix;

        if (isDemonLord) {
            this.setCustomName(Component.literal("§4§l✦ CHÂN MA VƯƠNG: §c§l" + effectiveName + " §4§l✦"));
        } else if (isNamed() || (getCustomDemonName() != null && !getCustomDemonName().isEmpty())) {
            if (hasPhysicalBody()) {
                this.setCustomName(Component.literal("§5§l✦ MA THẦN: §6§l" + effectiveName + " §5§l✦"));
            } else {
                this.setCustomName(Component.literal("§6§l✦ MA TƯỚNG: §e§l" + effectiveName + " §6§l✦"));
            }
        } else if (hasPhysicalBody()) {
            this.setCustomName(Component.literal("§c§l★ MA TƯỚNG [THỂ XÁC]: §f§l" + colorPrefix.toUpperCase() + " §c§l★"));
        } else {
            this.setCustomName(Component.literal("§7★ MA TƯỚNG THƯỢNG CẤP: §f" + colorPrefix + " §7★"));
        }
        this.setCustomNameVisible(true);
    }

    public String getEffectiveDemonName() {
        if (isNamed() && getCustomDemonName() != null && !getCustomDemonName().isEmpty()) {
            return getCustomDemonName();
        }
        return getDemonType() != null ? getDemonType().getColorName() : "Ác Ma";
    }

    @Override
    public Component getName() {
        if (isNamed() && getCustomDemonName() != null && !getCustomDemonName().isEmpty()) {
            return Component.literal(getCustomDemonName());
        }
        return super.getName();
    }

    public boolean hasPhysicalBody() {
        return this.entityData.get(DATA_HAS_BODY);
    }

    public void setPhysicalBody(boolean body) {
        this.entityData.set(DATA_HAS_BODY, body);
        updateAttributesForType(getDemonType());
        updateDemonNametag();
    }

    public boolean isNamed() {
        return this.entityData.get(DATA_IS_NAMED);
    }

    public void setNamed(boolean named) {
        this.entityData.set(DATA_IS_NAMED, named);
        updateAttributesForType(getDemonType());
        updateDemonNametag();
    }

    public int getEvolutionTier() {
        return this.entityData.get(DATA_EVOLUTION_TIER);
    }

    public void setEvolutionTier(int tier) {
        this.entityData.set(DATA_EVOLUTION_TIER, tier);
        updateAttributesForType(getDemonType());
        updateDemonNametag();
    }

    public String getCustomDemonName() {
        return this.entityData.get(DATA_CUSTOM_NAME);
    }

    public void setCustomDemonName(String name) {
        this.entityData.set(DATA_CUSTOM_NAME, name != null ? name : "");
        if (name != null && !name.isEmpty()) {
            this.entityData.set(DATA_IS_NAMED, true);
        }
        updateDemonNametag();
    }

    public List<PrimordialSkillPool.SkillEntry> getRandomSkills() {
        ensureRandomSkills();
        return this.randomSkills;
    }

    public void setRandomSkillIds(List<String> skillIds) {
        this.randomSkills.clear();
        for (String id : skillIds) {
            PrimordialSkillPool.SkillEntry entry = PrimordialSkillPool.getSkillById(id);
            if (entry != null) {
                this.randomSkills.add(entry);
            }
        }
    }

    public void ensureRandomSkills() {
        int targetCount = hasPhysicalBody() ? 4 : 2;
        if (this.randomSkills.size() < targetCount) {
            List<PrimordialSkillPool.SkillEntry> rolled = PrimordialSkillPool.rollRandomSkills(targetCount, this.random);
            this.randomSkills.clear();
            this.randomSkills.addAll(rolled);
        }
    }

    public double getDemonAttackDamage() {
        var a = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        return a != null ? a.getValue() : 160.0D;
    }

    public double getAttackDamage() {
        return getDemonAttackDamage();
    }

    public boolean isWinged() {
        return this.entityData.get(DATA_IS_WINGED);
    }

    public void setWinged(boolean winged) {
        this.entityData.set(DATA_IS_WINGED, winged);
        updateAttributesForType(getDemonType());
    }

    public boolean isRising() {
        return this.entityData.get(DATA_IS_RISING);
    }

    public void setRising(boolean rising) {
        this.entityData.set(DATA_IS_RISING, rising);
    }

    public float getRisingProgress() {
        return this.entityData.get(DATA_RISING_PROGRESS);
    }

    public void setRisingProgress(float progress) {
        this.entityData.set(DATA_RISING_PROGRESS, progress);
    }

    public boolean isCombatClawsActive() {
        return this.entityData.get(DATA_COMBAT_CLAWS);
    }

    public void setCombatClawsActive(boolean active) {
        this.entityData.set(DATA_COMBAT_CLAWS, active);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.isAlive()) {
            if (this.tickCount % 20 == 0) {
                if (getDemonType() == DemonType.JAUNE && hasPhysicalBody()) {
                    if (this.getMainHandItem().isEmpty() || !this.getMainHandItem().is(ModItems.GOLDEN_GUN.get())) {
                        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOLDEN_GUN.get()));
                    }
                }

                // Siêu Tốc Tái Sinh (Primordial Demon Ultraspeed Regeneration)
                if (this.getHealth() < this.getMaxHealth()) {
                    float regenPercent = hasPhysicalBody() ? 0.05F : 0.02F;
                    float healAmount = this.getMaxHealth() * regenPercent;
                    this.heal(healAmount);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 4, 0.3D, 0.5D, 0.3D, 0.02D);
                    }
                }
            }
        }
    }

    // =========================================================================
    // MIỄN NHIỄM TUYỆT ĐỐI CHO SINH VẬT HIỀN LÀNH (DÂN LÀNG, PET, IRON GOLEM) & NGƯỜI CHƠI BẢO HỘ
    // =========================================================================
    public static boolean isFriendlyCreature(LivingEntity target) {
        if (target == null) return false;
        if (target instanceof net.minecraft.world.entity.npc.Villager ||
            target instanceof net.minecraft.world.entity.npc.WanderingTrader ||
            target instanceof net.minecraft.world.entity.npc.Npc) {
            return true;
        }
        if (target instanceof net.minecraft.world.entity.animal.IronGolem) {
            return true;
        }
        if (target instanceof TamableAnimal tame && tame.isTame()) {
            return true;
        }
        if (target instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) {
            return true;
        }
        return false;
    }

    public static boolean isTargetImmune(LivingEntity target) {
        if (target == null) return false;
        if (isFriendlyCreature(target)) {
            return true; // Tuyệt đối không gây tử vong hoặc tổn hại cho dân làng & sinh vật hiền lành
        }
        if (target instanceof Player player) {
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.isDemonLord(player)) {
                return true;
            }
            for (ItemStack armor : player.getArmorSlots()) {
                if (armor.getItem() instanceof DivineArmorItem) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isContractedAllyOfPlayer(java.util.UUID playerUuid, net.minecraft.world.entity.Entity target) {
        if (target == null || playerUuid == null) return false;
        if (target.getUUID().equals(playerUuid)) return true;
        if (target instanceof PrimordialDemonEntity demon && demon.isTame()) {
            return playerUuid.equals(demon.getOwnerUUID());
        }
        if (target instanceof TamableAnimal tame && tame.isTame() && playerUuid.equals(tame.getOwnerUUID())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        if (other == null) return false;
        if (other == this) return true;
        if (this.isTame()) {
            if (this.getOwnerUUID() != null && this.getOwnerUUID().equals(other.getUUID())) {
                return true;
            }
            if (other instanceof PrimordialDemonEntity otherDemon && otherDemon.isTame()) {
                return true; // Các Ác Ma đã ký khế ước không bao giờ gây sát thương cho nhau
            }
            if (other instanceof TamableAnimal tame && tame.isTame() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(tame.getOwnerUUID())) {
                return true;
            }
        }
        return super.isAlliedTo(other);
    }

    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity target) {
        if (target != null && this.isTame() && this.isAlliedTo(target)) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    public boolean isImmuneOrFriendly(LivingEntity target) {
        if (target == null) return true;
        if (target == this) return true;
        if (this.isTame() && this.isAlliedTo(target)) return true;
        if (this.isMassacreMode() && (target instanceof net.minecraft.world.entity.npc.AbstractVillager || target instanceof net.minecraft.world.entity.animal.IronGolem || target instanceof net.minecraft.world.entity.animal.SnowGolem)) {
            return false; // Trong chế độ Tàn Sát, Dân Làng và Iron Golem KHÔNG được miễn nhiễm!
        }
        if (isTargetImmune(target)) return true;
        if (this.getOwner() != null && target == this.getOwner()) return true;
        return false;
    }

    /**
     * PHÁ KHỐI ZERO-LAG DÀNH RIÊNG CHO HOÀNG SẮC THỦY TỔ (CARRERA - JAUNE):
     * Mọi ma pháp của Carrera đều có thể bộc phá khối, giới hạn bán kính an toàn và không rớt item để đảm bảo 20 TPS.
     */
    public void applyCarreraBlockDestruction(Vec3 center, float radius) {
        if (!(this.level() instanceof ServerLevel sl) || center == null) return;
        int r = Math.min(3, Math.round(radius));
        BlockPos cPos = BlockPos.containing(center);
        int destroyed = 0;
        int maxLimit = 36;
        for (int dx = -r; dx <= r && destroyed < maxLimit; dx++) {
            for (int dy = -r; dy <= r && destroyed < maxLimit; dy++) {
                for (int dz = -r; dz <= r && destroyed < maxLimit; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    BlockPos p = cPos.offset(dx, dy, dz);
                    net.minecraft.world.level.block.state.BlockState state = sl.getBlockState(p);
                    if (state.isAir()) continue;
                    if (state.is(net.minecraft.world.level.block.Blocks.BEDROCK) || state.is(net.minecraft.world.level.block.Blocks.BARRIER)) continue;
                    if (state.getDestroySpeed(sl, p) < 0) continue;
                    sl.destroyBlock(p, false); // false = không rớt item để triệt tiêu 100% lag
                    destroyed++;
                }
            }
        }
        if (destroyed > 0) {
            sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.5, 0.5, 0.5, 0);
            sl.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.8F, 1.2F);
        }
    }

    /**
     * PHÁ HỦY MA PHÁP & VÒNG TRÒN PHÁP TRẬN ĐỐI NGHỊCH:
     * Kỹ năng của các ác ma khi có khế ước hay chưa đều phá tan vòng tròn ma pháp trận hoặc kết giới của nhau.
     */
    public void dispelOpposingMagic(Vec3 center, double radius) {
        if (!(this.level() instanceof ServerLevel sl) || center == null) return;
        com.minhphuc.weapons.content.tensura.ResidualMagicCircleManager.purgeCirclesInArea(sl, center, radius, radius * 2);
        com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.removeBarriersInArea(sl, center, radius, this.getOwner());
        com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.removeBarriersInArea(sl, center, radius, this.getOwner());
        com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.removePrisonsInArea(sl, center, radius);
    }

    public static boolean isBossTarget(LivingEntity target) {
        if (target == null) return false;
        if (target instanceof WitherBoss || target instanceof EnderDragon || target instanceof Warden) {
            return true;
        }
        return target.getMaxHealth() >= 250.0F;
    }

    private int lastRivalryShoutTick = 0;
    private int lastKillDialogueTick = 0;

    public void broadcastNearbyDialogue(String dialogue, double radius) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Component msg = Component.literal("§d§l[" + getEffectiveDemonName().toUpperCase() + "] §f\"" + dialogue + "\"");
        for (ServerPlayer p : sl.getPlayers(p -> p.distanceToSqr(this) <= radius * radius)) {
            p.sendSystemMessage(msg);
        }
    }

    public void triggerKillDialogue(LivingEntity victim) {
        if (this.tickCount - lastKillDialogueTick < 10) return;
        lastKillDialogueTick = this.tickCount;

        DemonType type = getDemonType();
        String demonSpeaker = getEffectiveDemonName().toUpperCase();
        if (victim instanceof Player) {
            String[] pool = type.getPlayerKillDialogues();
            if (pool.length > 0) {
                String d = pool[this.random.nextInt(pool.length)];
                broadcastNearbyDialogue(d, 48.0D);
                if (this.level() instanceof ServerLevel sl) {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.5F, 1.2F);
                }
            }
        } else {
            if (this.level() instanceof ServerLevel sl) {
                List<ServerPlayer> nearbyPlayers = sl.getPlayers(p -> p.distanceToSqr(this) <= 32.0D * 32.0D);
                if (!nearbyPlayers.isEmpty()) {
                    String[] pool = type.getMobKillDialogues();
                    if (pool.length > 0) {
                        String d = pool[this.random.nextInt(pool.length)];
                        for (ServerPlayer p : nearbyPlayers) {
                            p.sendSystemMessage(Component.literal("§d§l[" + demonSpeaker + "] §f\"" + d + "\""));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void awardKillScore(Entity victim, int score, DamageSource damageSource) {
        super.awardKillScore(victim, score, damageSource);
        if (victim instanceof LivingEntity living) {
            triggerKillDialogue(living);
            onMassacreKill(living);
        }
    }

    private void onMassacreKill(LivingEntity victim) {
        if (!this.isTame() || !this.isMassacreMode() || !(this.getOwner() instanceof ServerPlayer owner)) return;
        if (victim.getTags().contains("BlancMassacreCounted")) return;
        victim.addTag("BlancMassacreCounted");

        if (victim instanceof net.minecraft.world.entity.npc.AbstractVillager) {
            CompoundTag data = com.minhphuc.weapons.data.EntityDataHelper.getCustomData(owner);
            int sacrificed = data.getInt("TensuraVillagersSacrificed") + 1;
            data.putInt("TensuraVillagersSacrificed", sacrificed);
            ItemStack dmSoul = new ItemStack(ModItems.DEMON_LORD_SOUL.get());
            if (!owner.getInventory().add(dmSoul)) {
                owner.drop(dmSoul, false);
            }
        }
    }

    public void dealDemonicDamage(LivingEntity target, float amount) {
        if (isImmuneOrFriendly(target)) {
            // Không hiển thị tin nhắn găng tay/áo giáp theo yêu cầu người chơi
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0D, target.getZ(), 1, 0, 0, 0, 0);
            }
            return;
        }

        // Tăng thêm +50% sức mạnh khi đang trong Lãnh Địa của ác ma
        if (activeDomainTicks > 0) {
            amount *= 1.5F;
        }

        // CÂN BẰNG TỶ LỆ GIAO CHIẾN BOSS
        if (isBossTarget(target)) {
            DemonType type = getDemonType();
            float bonus = hasPhysicalBody() ? 1.25F : 1.0F;

            if (type == DemonType.ROUGE || type == DemonType.NOIR) {
                // 90% áp đảo diệt Boss, 10% sơ suất/chủ quan
                if (this.random.nextFloat() < (hasPhysicalBody() ? 0.02F : 0.05F)) {
                    amount *= 0.85F;
                } else {
                    amount *= (2.6F * bonus);
                }
            } else if (type == DemonType.BLANC) {
                amount *= (2.1F * bonus); // 75% -> 80% / 83%
            } else if (type == DemonType.JAUNE) {
                amount *= (1.9F * bonus); // 65% -> 70% / 73%
            } else if (type == DemonType.VIOLET) {
                amount *= (1.25F * bonus); // 33% -> 38% / 41%
            } else if (type == DemonType.VERT) {
                amount *= (1.15F * bonus); // 30% -> 35% / 38%
            } else if (type == DemonType.BLEU) {
                amount *= (0.85F * bonus); // 20% -> 25% / 28%
            }
        }

        target.hurt(this.level().damageSources().mobAttack(this), amount);
        if (target.isDeadOrDying() || target.getHealth() <= 0) {
            triggerKillDialogue(target);
            onMassacreKill(target);
        }
    }

    public void applyDemonicEffect(LivingEntity target, MobEffectInstance effect) {
        if (isImmuneOrFriendly(target)) return;
        target.addEffect(effect);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living) {
            if (isImmuneOrFriendly(living)) {
                // Không hiển thị tin nhắn găng tay/áo giáp theo yêu cầu người chơi
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                }
                return false;
            }
        }
        boolean res = super.doHurtTarget(target);
        if (res && target instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0)) {
            onMassacreKill(living);
        }
        return res;
    }

    @Override
    public void tick() {
        super.tick();

        // Đồng bộ thuộc tính theo DemonType nếu chưa gán
        if (!this.level().isClientSide() && this.tickCount == 1) {
            updateAttributesForType(getDemonType());
        }

        // Animation vỗ cánh & Kiểm soát bay lượn trên không
        this.prevWingFlapAngle = this.wingFlapAngle;
        if (isWinged()) {
            this.wingFlapAngle += 0.15F;
            if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
                this.fallDistance = 0.0F;
            }
        }

        // Kiểm soát bay lượn tự do & không trọng lực khi chiến đấu hoặc đi theo chủ nhân
        if (!this.level().isClientSide()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                this.setNoGravity(true);
                this.fallDistance = 0.0F;
                double desiredY = target.getY() + 2.5D;
                if (this.getY() < desiredY) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04D, 0));
                }
            } else if (this.getOwner() != null && this.getOwner().getY() > this.getY() + 2.0D) {
                this.setNoGravity(true);
                this.fallDistance = 0.0F;
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04D, 0));
            } else if (this.onGround()) {
                this.setNoGravity(false);
            }
        }

        // Cập nhật trạng thái bung móng vuốt khi có mục tiêu
        if (!this.level().isClientSide()) {
            boolean inCombat = this.getTarget() != null && this.getTarget().isAlive();
            if (this.isCombatClawsActive() != inCombat) {
                this.setCombatClawsActive(inCombat);
            }
        }

        if (this.level().isClientSide()) {
            // Hào quang ma tộc đặc trưng quanh người theo từng trạng thái tiến hóa
            boolean isAwakened = hasPhysicalBody() && isNamed();
            float particleChance = isAwakened ? 0.85F : (isWinged() ? 0.6F : 0.3F);
            if (this.random.nextFloat() < particleChance) {
                double ox = (this.random.nextDouble() - 0.5D) * (isAwakened ? 1.0D : 0.8D);
                double oy = this.random.nextDouble() * 1.9D;
                double oz = (this.random.nextDouble() - 0.5D) * (isAwakened ? 1.0D : 0.8D);

                DemonType type = getDemonType();
                if (isAwakened) {
                    // Ma Thần Tối Thượng: Hào quang cuồn cuộn với linh diễm và cánh cổng ma giới
                    this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.03, 0);
                    this.level().addParticle(ParticleTypes.PORTAL, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.02, 0);
                    if (this.random.nextFloat() < 0.2F) {
                        this.level().addParticle(ParticleTypes.TOTEM_OF_UNDYING, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.05, 0);
                    }
                } else if (hasPhysicalBody()) {
                    // Thể Xác Ma Thiết: Tia lửa kim loại
                    this.level().addParticle(ParticleTypes.CRIT, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.02, 0);
                } else if (isNamed()) {
                    // Đã Ban Danh Xưng: Bụi phù chú huyền bí
                    this.level().addParticle(ParticleTypes.ENCHANT, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.03, 0);
                }

                if (type == DemonType.ROUGE) {
                    this.level().addParticle(ParticleTypes.FLAME, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.02, 0);
                } else if (type == DemonType.NOIR) {
                    this.level().addParticle(ParticleTypes.DRAGON_BREATH, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.01, 0);
                } else if (type == DemonType.BLANC) {
                    this.level().addParticle(ParticleTypes.END_ROD, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.01, 0);
                } else if (type == DemonType.JAUNE) {
                    this.level().addParticle(ParticleTypes.CRIT, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.02, 0);
                    this.level().addParticle(ParticleTypes.LAVA, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.01, 0);
                } else if (type == DemonType.VIOLET) {
                    this.level().addParticle(ParticleTypes.WITCH, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.01, 0);
                } else if (type == DemonType.BLEU) {
                    this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.01, 0);
                } else if (type == DemonType.VERT) {
                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.02, 0);
                }
            }
            return;
        }

        // Tự động hồi phục sinh lực ma tộc từ từ (10 HP/s khi đã thuần phục, 3-6 HP/s khi hoang dã)
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            float healAmt = this.isTame() ? 10.0F : ((getDemonType() == DemonType.ROUGE || getDemonType() == DemonType.NOIR) ? 6.0F : 3.0F);
            this.heal(healAmt);
            if (this.isTame() && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2D, this.getZ(), 3, 0.3D, 0.4D, 0.3D, 0.02D);
            }
        }

        // THỦY TỔ CHƯA THU PHỤC: ƯU TIÊN SỐ 1 TẤN CÔNG ÁC MA ĐÃ THUẦN PHỤC CỦA NGƯỜI CHƠI TRƯỚC!
        if (!this.level().isClientSide() && !this.isTame() && this.tickCount % 10 == 0) {
            if (this.getTarget() instanceof Player || this.getTarget() == null) {
                List<PrimordialDemonEntity> tamedDemons = this.level().getEntitiesOfClass(
                        PrimordialDemonEntity.class,
                        this.getBoundingBox().inflate(36.0D),
                        pde -> pde.isAlive() && pde.isTame()
                );
                if (!tamedDemons.isEmpty()) {
                    PrimordialDemonEntity rival = tamedDemons.get(0);
                    if (this.getTarget() != rival) {
                        this.setTarget(rival);
                        if (this.tickCount - lastRivalryShoutTick > 200) {
                            lastRivalryShoutTick = this.tickCount;
                            String rivalryMsg = getDemonType().getRivalryDialogue(rival.getDemonType());
                            broadcastNearbyDialogue(rivalryMsg, 48.0D);
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 1.2F);
                        }
                    }
                }
            }
        }

        if (skillCooldownTicks > 0) {
            skillCooldownTicks--;
        }

        if (this.sealedTicks > 0) {
            this.sealedTicks--;
            if (this.level() instanceof ServerLevel sl && this.tickCount % 10 == 0) {
                // Hiệu ứng hắc khói và bùa chú phong sát quấn quanh ác ma
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 6, 0.35, 0.5, 0.35, 0.02);
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1.2, getZ(), 4, 0.25, 0.4, 0.25, 0.05);
            }
        }

        // Cập nhật và xoay các vòng tròn ma thuật ItemDisplay
        updateDisplayCircles();

        // Xử lý Lãnh Địa (Domain Expansion)
        tickActiveDomain();

        // Trạng thái trồi lên từ nghi thức triệu hồi
        if (isRising()) {
            this.setDeltaMovement(0, 0, 0);
            this.setNoGravity(true);
        } else {
            this.setNoGravity(false);
        }
    }

    private void updateDisplayCircles() {
        if (this.activeCircles.isEmpty()) return;

        List<ActiveDisplayCircle> toRemove = new ArrayList<>();
        for (ActiveDisplayCircle circle : this.activeCircles) {
            circle.ticksRemaining--;
            if (circle.ticksRemaining <= 0 || !circle.display.isAlive()) {
                if (circle.display.isAlive()) {
                    circle.display.discard();
                }
                toRemove.add(circle);
                continue;
            }

            // Cập nhật góc xoay
            circle.currentAngle += circle.rotationSpeed;

            // Tối ưu mạng: Cập nhật transformation mỗi 2 ticks thay vì mỗi tick để giảm 50% gói tin mạng
            if (this.tickCount % 2 == 0) {
                DisplayAccessor dispAcc = (DisplayAccessor) circle.display;

                Quaternionf rot;
                if (circle.isHorizontal) {
                    rot = new Quaternionf().rotateX((float) Math.toRadians(90.0F)).rotateZ((float) Math.toRadians(circle.currentAngle));
                } else {
                    rot = new Quaternionf().rotateY((float) Math.toRadians(this.getYRot())).rotateZ((float) Math.toRadians(circle.currentAngle));
                }

                dispAcc.weapons$setTransformation(new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        rot,
                        new Vector3f(circle.scale, circle.scale, 0.01F),
                        null
                ));
            }
        }
        this.activeCircles.removeAll(toRemove);
    }

    public boolean isDomainBarrierBlock(Block block) {
        return block == ModBlocks.DOMAIN_BARRIER_NOIR.get() ||
               block == ModBlocks.DOMAIN_BARRIER_ROUGE.get() ||
               block == ModBlocks.DOMAIN_BARRIER_BLANC.get() ||
               block == ModBlocks.DOMAIN_BARRIER_JAUNE.get() ||
               block == ModBlocks.DOMAIN_BARRIER_VIOLET.get() ||
               block == ModBlocks.DOMAIN_BARRIER_BLEU.get() ||
               block == ModBlocks.DOMAIN_BARRIER_VERT.get();
    }

    public void cleanDomainBarriers() {
        if (domainBarrierBlocks.isEmpty()) return;
        if (this.level() instanceof ServerLevel sl) {
            for (BlockPos p : domainBarrierBlocks) {
                if (isDomainBarrierBlock(sl.getBlockState(p).getBlock())) {
                    sl.setBlock(p, Blocks.AIR.defaultBlockState(), 50);
                }
            }
            if (domainCenter != null) {
                sl.playSound(null, domainCenter.x, domainCenter.y, domainCenter.z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.5F, 1.2F);
            }
        }
        domainBarrierBlocks.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        cleanDomainBarriers();
        super.remove(reason);
    }

    private void tickActiveDomain() {
        if (activeDomainTicks <= 0) {
            cleanDomainBarriers();
            return;
        }
        activeDomainTicks--;

        ServerLevel level = (ServerLevel) this.level();
        DemonType type = this.activeDomainDemonType != null ? this.activeDomainDemonType : getDemonType();
        double radius = 16.0D;

        // Vẽ chu vi biên giới hạt kết giới Lãnh Địa (mỗi 4 ticks, bước góc 24 độ để giảm 80% gói tin mạng)
        if (activeDomainTicks % 4 == 0) {
            for (int a = 0; a < 360; a += 24) {
                double rad = Math.toRadians(a);
                double px = domainCenter.x + Math.cos(rad) * radius;
                double pz = domainCenter.z + Math.sin(rad) * radius;

                if (type == DemonType.NOIR) {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, px, domainCenter.y + 0.5D, pz, 1, 0.2D, 1.0D, 0.2D, 0.02D);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, domainCenter.y + 1.2D, pz, 1, 0.1D, 0.5D, 0.1D, 0.01D);
                } else if (type == DemonType.ROUGE) {
                    level.sendParticles(ParticleTypes.FLAME, px, domainCenter.y + 0.5D, pz, 1, 0.2D, 1.2D, 0.2D, 0.02D);
                } else if (type == DemonType.BLANC) {
                    level.sendParticles(ParticleTypes.END_ROD, px, domainCenter.y + 0.8D, pz, 1, 0.1D, 0.8D, 0.1D, 0.01D);
                } else if (type == DemonType.JAUNE) {
                    level.sendParticles(ParticleTypes.CRIT, px, domainCenter.y + 0.5D, pz, 1, 0.2D, 0.5D, 0.2D, 0.05D);
                    level.sendParticles(ParticleTypes.LAVA, px, domainCenter.y + 1.0D, pz, 1, 0.1D, 0.5D, 0.1D, 0.02D);
                } else if (type == DemonType.VIOLET) {
                    level.sendParticles(ParticleTypes.WITCH, px, domainCenter.y + 0.5D, pz, 1, 0.2D, 0.8D, 0.2D, 0.02D);
                } else if (type == DemonType.BLEU) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, px, domainCenter.y + 0.5D, pz, 1, 0.2D, 0.8D, 0.2D, 0.01D);
                } else if (type == DemonType.VERT) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, domainCenter.y + 0.5D, pz, 1, 0.2D, 0.8D, 0.2D, 0.02D);
                }
            }
        }

        // Tác động lên các thực thể bên trong kết giới (chạy mỗi 4 ticks để giảm 75% tải tính toán thực thể)
        if (activeDomainTicks % 4 == 0) {
            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(domainCenter.x - 17, domainCenter.y - 6, domainCenter.z - 17, domainCenter.x + 17, domainCenter.y + 15, domainCenter.z + 17)
            );

            for (LivingEntity e : entities) {
                if (e == this || e == this.getOwner()) continue;
                if (isImmuneOrFriendly(e)) continue;

                double dist = e.distanceToSqr(domainCenter);

                // Giam hãm: Nếu chạm rìa kết giới (14m - 19m), giật ngược lại tâm kết giới
                if (dist >= 14.0D * 14.0D && dist <= 19.0D * 19.0D) {
                    Vec3 pull = domainCenter.subtract(e.position()).normalize().scale(0.85D);
                    e.setDeltaMovement(pull.x, 0.2D, pull.z);
                    e.hasImpulse = true;
                    level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + 1.0D, e.getZ(), 1, 0, 0, 0, 0);
                }

                // Áp chế suy yếu bên trong lãnh địa
                if (dist <= radius * radius) {
                    applyDemonicEffect(e, new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                    applyDemonicEffect(e, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));

                    if (this.tickCount % 20 < 4) {
                        dealDemonicDamage(e, 35.0F);
                    }
                }
            }
        }

        // Tăng cường +50% SỨC MẠNH cho Ác ma khi đứng trong Lãnh Địa
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2));
    }

    public void activateDomain(DemonType type, Vec3 center, String domainName, String voiceLine) {
        this.activeDomainTicks = 300; // 15 giây
        this.domainCenter = center;
        this.activeDomainDemonType = type;
        cleanDomainBarriers();

        ServerLevel level = (ServerLevel) this.level();
        Block barrierBlock = type.getDomainBarrierBlock().get();
        int radius = 16;
        BlockPos cPos = BlockPos.containing(center);

        // Tạo vòm kết giới khối thực tế theo màu ác ma
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 16; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + (y * 0.9) * (y * 0.9) + z * z);
                    if (dist >= radius - 0.9 && dist <= radius + 0.9) {
                        BlockPos bPos = cPos.offset(x, y, z);
                        if (level.getBlockState(bPos).isAir() || level.getBlockState(bPos).liquid()) {
                            level.setBlock(bPos, barrierBlock.defaultBlockState(), 50);
                            domainBarrierBlocks.add(bPos);
                        }
                    }
                }
            }
        }

        // Tạo vòng ma pháp khổng lồ 16m dưới chân Lãnh Địa
        ItemStack circleStack = (type == DemonType.JAUNE)
                ? new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get())
                : new ItemStack(type.getMagicCircleItem().get());
        spawnRotatingCircle(level, center.add(0, 0.05D, 0), 16.0F, 1.5F, 300, true, type.getGlowColor(), circleStack);

        // Thông báo toàn bộ người chơi trong bán kính 64 khối
        for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(this) <= 64.0D * 64.0D)) {
            sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§8§l【 LÃNH ĐỊA 】 §c§l" + domainName.toUpperCase())));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + type.getColorName() + " §7đã giải phóng Không Gian Tuyệt Đối! (+50% Sức Mạnh)")));
            sp.displayClientMessage(Component.literal("§4§l[LÃNH ĐỊA THỦY TỔ] §c" + type.getColorName() + ": §f\"" + voiceLine + "\""), false);
        }
    }

    public Display.ItemDisplay spawnRotatingCircle(ServerLevel level, Vec3 pos, float scale, float rotSpeed, int duration, boolean isHorizontal, int glowColor, ItemStack circleStack) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) display;
            DisplayAccessor dispAcc = (DisplayAccessor) display;

            itemAcc.weapons$setItemStack(circleStack != null && !circleStack.isEmpty() ? circleStack : new ItemStack(getDemonType().getMagicCircleItem().get()));
            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            display.addTag("DemonMagicCircle");
            dispAcc.weapons$setGlowColorOverride(glowColor);
            dispAcc.weapons$setViewRange(6.0F);

            Quaternionf rot;
            if (isHorizontal) {
                rot = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            } else {
                rot = new Quaternionf().rotateY((float) Math.toRadians(this.getYRot()));
            }

            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rot,
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
            this.activeCircles.add(new ActiveDisplayCircle(display, duration, rotSpeed, scale, isHorizontal, Vec3.ZERO));
            com.minhphuc.weapons.content.tensura.ResidualMagicCircleManager.registerCircle(display);
        }
        return display;
    }

    public Display.ItemDisplay spawnRotatingCircle(ServerLevel level, Vec3 pos, float scale, float rotSpeed, int duration, boolean isHorizontal, int glowColor) {
        return spawnRotatingCircle(level, pos, scale, rotSpeed, duration, isHorizontal, glowColor, new ItemStack(getDemonType().getMagicCircleItem().get()));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        if (this.isOwnedBy(player)) {
            ItemStack heldItem = player.getItemInHand(hand);

            // ==========================================
            // CẦM KHUNG XƯƠNG NHÂN TẠO → TIẾN HÓA THỂ XÁC VẬT LÝ
            // ==========================================
            if (heldItem.is(ModItems.ARTIFICIAL_SKELETON.get())) {
                if (this.level().isClientSide()) return InteractionResult.SUCCESS;
                if (hasPhysicalBody()) {
                    player.displayClientMessage(Component.literal("§c§l✦ " + getEffectiveDemonName() + " §cđã có thể xác vật lý rồi!"), true);
                    return InteractionResult.FAIL;
                }
                // Ban thể xác vật lý (Sức mạnh x30, Máu x4, EP x3)
                this.setPhysicalBody(true);
                int newTier = Math.max(this.getEvolutionTier(), 1);
                if (isNamed()) newTier = Math.max(newTier, 3);
                this.setEvolutionTier(newTier);
                boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
                this.setWinged(getDemonType() == DemonType.NOIR || isDemonLord);
                if (getDemonType() == DemonType.JAUNE) {
                    this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOLDEN_GUN.get()));
                }
                if (!player.isCreative()) heldItem.shrink(1);
                performEvolution(player, "BAN CƠ THỂ VẬT LÝ", false);
                return InteractionResult.SUCCESS;
            }

            // ==========================================
            // SHIFT + CLICK: TOGGLE CÁNH (CHỈ NOIR)
            // ==========================================
            if (player.isShiftKeyDown()) {
                if (this.getDemonType() == DemonType.NOIR) {
                    boolean nowWinged = !this.isWinged();
                    this.setWinged(nowWinged);

                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            nowWinged ? SoundEvents.ENDER_DRAGON_FLAP : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.8F, 0.9F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 1.2F);

                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.0D, this.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.08D);
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.05D);
                    }

                    if (nowWinged) {
                        player.displayClientMessage(Component.literal("§8§l[NOIR] §c§l★ GIẢI PHÓNG HẮC DỰC MA VƯƠNG ★\n§7\"Kufufufu... Ngài cho phép tôi giải phóng đôi cánh hắc ám sao? Thật vinh hạnh tột cùng!\""), false);
                    } else {
                        player.displayClientMessage(Component.literal("§8§l[NOIR] §f§l✦ THU HỒI CÁNH - DẠNG QUẢN GIA ✦\n§7\"Như người mong muốn, tôi sẽ giữ phong thái thanh lịch khi bên cạnh ngài.\""), false);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(Component.literal("§d§l[" + this.getDemonType().getColorName().toUpperCase() + "] §f" + this.getDemonType().getSummonDialogue()), false);
                    return InteractionResult.SUCCESS;
                }
            } else {
                // ==========================================
                // CLICK THƯỜNG: TOGGLE CANH GIỮ / ĐI THEO
                // ==========================================
                boolean newSitting = !this.isOrderedToSit();
                this.setGuardMode(newSitting, this.blockPosition());
                this.jumping = false;
                this.navigation.stop();

                if (newSitting) {
                    player.displayClientMessage(Component.literal("§e§l[" + this.getDemonType().getColorName() + "] §7đang §eTUẦN TRA CANH GIỮ KHU VỰC§7 này!"), true);
                } else {
                    player.displayClientMessage(Component.literal("§e§l[" + this.getDemonType().getColorName() + "] §aĐI THEO BẢO VỆ§7 chủ nhân!"), true);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            ItemStack heldItem = player.getItemInHand(hand);
            if (heldItem.is(ModItems.ARTIFICIAL_SKELETON.get())) {
                if (!this.level().isClientSide()) {
                    player.displayClientMessage(Component.literal("§c§l✦ Ác ma này chưa lập khế ước với bạn! Chỉ có chủ nhân đã ký khế ước mới có thể ban thể xác vật lý!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }
        return super.mobInteract(player, hand);
    }

    // =========================================================================
    // TIẾN HÓA ÁC MA: HIỆU ỨNG VÒNG TRÒN MA THUẬT + NÂNG CẤP SỨC MẠNH
    // =========================================================================
    private void performEvolution(Player player, String evolutionTitle, boolean isFullAwakening) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        DemonType type = getDemonType();
        String effectiveName = getEffectiveDemonName();

        // 1. Cập nhật attributes theo trạng thái mới
        updateAttributesForType(type);
        this.setHealth(this.getMaxHealth()); // Full HP sau tiến hóa

        // 2. Cập nhật kỹ năng ngẫu nhiên
        this.ensureRandomSkills();

        // 3. Spawn vòng tròn ma thuật xoay dưới chân ác ma
        ItemStack circleStack = new ItemStack(type.getMagicCircleItem().get());
        spawnRotatingCircle(sl, this.position().add(0, 0.05D, 0), 4.0F, 2.5F, 120, true, type.getGlowColor(), circleStack);

        // 4. Hiệu ứng particle hoành tráng
        sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.5D, this.getZ(), 5, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2D, this.getZ(), 80, 0.6D, 1.0D, 0.6D, 0.2D);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.5D, this.getZ(), 50, 0.8D, 0.8D, 0.8D, 0.1D);
        sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 60, 0.5D, 0.8D, 0.5D, 0.15D);
        sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.2D, this.getZ(), 40, 0.6D, 0.6D, 0.6D, 0.08D);

        if (isFullAwakening) {
            // Hiệu ứng đặc biệt cho thức tỉnh hoàn mỹ
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5D, this.getZ(), 2, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.0D, this.getZ(), 80, 0.8D, 1.2D, 0.8D, 0.1D);
        }

        // 5. Âm thanh hoành tráng
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 0.8F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 2.0F, 0.9F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.5F, 1.0F);
        if (isFullAwakening) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.9F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 2.5F, 1.2F);
        }

        // 6. Title + Subtitle cho chủ nhân
        if (player instanceof ServerPlayer sp && sp.connection != null) {
            String titleRank;
            if (hasPhysicalBody() && isNamed()) {
                titleRank = "MA THẦN TỐI THƯỢNG THỨC TỈNH";
            } else if (hasPhysicalBody()) {
                titleRank = "THỂ XÁC VẬT LÝ HÓA";
            } else {
                titleRank = "BAN DANH XƯNG";
            }
            sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l★ " + evolutionTitle + " ★")));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + effectiveName + " §7(" + type.getTitleVi() + ") §atiến hóa thành công!")));
            VoiceOfTheWorld.announce(sp, "Báo cáo. Ác ma " + effectiveName + " (" + type.getTitleVi() + ") đã " + evolutionTitle.toLowerCase() + "! Sức mạnh tăng lên đáng kể!");
        }

        // 7. Dialogue tiến hóa
        String dialogue = getEvolutionDialogue(type, isFullAwakening);
        broadcastNearbyDialogue(dialogue, 48.0D);

        // 8. Đồng bộ lại khế ước trong inventory chủ nhân
        syncToPactInOwnerInventory();
    }

    private String getEvolutionDialogue(DemonType type, boolean isFullAwakening) {
        if (isFullAwakening) {
            return switch (type) {
                case NOIR -> "Kufufufu... Sức mạnh tuyệt đối đang tràn ngập khắp cơ thể tôi! Từ nay, bất kỳ ai dám chạm vào ngài, đều sẽ bị tôi nghiền nát!";
                case ROUGE -> "Hmph! Cuối cùng ta cũng đã giải phóng toàn bộ sức mạnh! Ngọn lửa ta sẽ thiêu rụi cả thế giới nếu ngươi muốn!";
                case BLANC -> "Ara ara... Thể xác và danh xưng hoàn mỹ! Ta đã trở thành tồn tại tuyệt đối rồi~";
                case JAUNE -> "Hahaha! Sức mạnh của ma pháp hạt nhân giờ đã bùng nổ gấp bội! Thật tuyệt vời!";
                case VIOLET -> "Hihihi~ Em cảm nhận được sức mạnh mới dâng trào khắp cơ thể! Chủ nhân, cảm ơn ngài nha~";
                case BLEU -> "Cực hàn tuyệt đối... Sức mạnh này sẽ đóng băng cả linh hồn kẻ thù.";
                case VERT -> "Tôi xin đội ơn chủ nhân đã ban cho tôi sức mạnh hoàn mỹ. Mọi bão tố sẽ bảo vệ ngài tuyệt đối!";
            };
        } else {
            return switch (type) {
                case NOIR -> "Kufufufu... Tôi cảm nhận được sức mạnh mới đang tràn ngập! Xin ngài hãy tiếp tục ban ân huệ cho tôi!";
                case ROUGE -> "Hmph! Sức mạnh mới này... không tệ đâu. Ta sẽ dùng nó bảo vệ ngươi.";
                case BLANC -> "Ara ara... Ta cảm thấy mạnh mẽ hơn rất nhiều rồi. Cảm ơn nhé~";
                case JAUNE -> "Haha! Sức mạnh tăng lên rồi! Cho ta xài thử nào!";
                case VIOLET -> "Hihihi~ Cảm ơn chủ nhân! Em mạnh hơn rồi nè!";
                case BLEU -> "Sức mạnh mới... Tôi ghi nhận. Kẻ thù sẽ phải trả giá.";
                case VERT -> "Tôi xin cảm tạ ân huệ. Sức mạnh mới sẽ bảo vệ chủ nhân tốt hơn.";
            };
        }
    }

    // =========================================================================
    // ĐỒNG BỘ TRẠNG THÁI TIẾN HÓA VÀO KHẾ ƯỚC TRONG INVENTORY CHỦ NHÂN
    // =========================================================================
    public void syncToPactInOwnerInventory() {
        if (!(this.getOwner() instanceof ServerPlayer owner)) return;
        DemonType type = getDemonType();

        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack stack = owner.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PrimordialPactItem) {
                DemonType pactType = PrimordialPactItem.getDemonType(stack);
                String uuidStr = ItemStackDataHelper.getString(stack, "DemonUUID");
                if (pactType == type || this.getStringUUID().equals(uuidStr)) {
                    // Xác định tier tiến hóa hiện tại để chọn đúng item variant
                    int currentTier = getEvolutionTier();
                    if (currentTier == 0) {
                        if (hasPhysicalBody() && isNamed()) currentTier = 3;
                        else if (hasPhysicalBody()) currentTier = 1;
                        else if (isNamed()) currentTier = 2;
                    }

                    // Kiểm tra xem item hiện tại đã đúng variant chưa
                    net.minecraft.world.item.Item correctItem = type.getPactItemForTier(currentTier).get();
                    if (stack.getItem() != correctItem) {
                        // Tạo ItemStack mới với đúng variant tiến hóa
                        ItemStack newStack = new ItemStack(correctItem);

                        // Sao chép toàn bộ NBT data từ item cũ sang item mới
                        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                            newStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                    stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
                        }

                        // Cập nhật trạng thái tiến hóa lên khế ước mới
                        boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
                        ItemStackDataHelper.putString(newStack, "DemonType", type.name());
                        ItemStackDataHelper.putBoolean(newStack, "HasPhysicalBody", hasPhysicalBody());
                        ItemStackDataHelper.putBoolean(newStack, "IsNamed", isNamed());
                        ItemStackDataHelper.putString(newStack, "CustomDemonName", getCustomDemonName());
                        ItemStackDataHelper.putInt(newStack, "EvolutionTier", currentTier);
                        ItemStackDataHelper.putBoolean(newStack, "IsDemonLord", isDemonLord);
                        ItemStackDataHelper.putBoolean(newStack, "IsAwakenedDemonLord", isDemonLord);
                        ItemStackDataHelper.putString(newStack, "DemonRank", getDemonRankTitle());
                        ItemStackDataHelper.putLong(newStack, "CalculatedEP", getCalculatedEP());
                        ItemStackDataHelper.putFloat(newStack, "AttackDamage", (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        ItemStackDataHelper.putFloat(newStack, "CurrentHp", this.getHealth());
                        ItemStackDataHelper.putFloat(newStack, "MaxHp", this.getMaxHealth());
                        ItemStackDataHelper.putBoolean(newStack, "IsSummoned", ItemStackDataHelper.getBoolean(stack, "IsSummoned"));
                        ItemStackDataHelper.putString(newStack, "DemonUUID", ItemStackDataHelper.getString(stack, "DemonUUID"));
                        ItemStackDataHelper.putString(newStack, "OwnerUUID", ItemStackDataHelper.getString(stack, "OwnerUUID"));
                        ItemStackDataHelper.putString(newStack, "OwnerName", ItemStackDataHelper.getString(stack, "OwnerName"));

                        // Lưu kỹ năng
                        StringBuilder sb = new StringBuilder();
                        for (PrimordialSkillPool.SkillEntry sk : getRandomSkills()) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(sk.id());
                        }
                        ItemStackDataHelper.putString(newStack, "PrimordialSkills", sb.toString());

                        // Thay thế item trong inventory
                        owner.getInventory().setItem(i, newStack);
                    } else {
                        // Item đã đúng variant, chỉ cập nhật NBT data
                        boolean isDemonLord = this.getTags().contains("TensuraDemonLordMob");
                        ItemStackDataHelper.putBoolean(stack, "HasPhysicalBody", hasPhysicalBody());
                        ItemStackDataHelper.putBoolean(stack, "IsNamed", isNamed());
                        ItemStackDataHelper.putString(stack, "CustomDemonName", getCustomDemonName());
                        ItemStackDataHelper.putInt(stack, "EvolutionTier", currentTier);
                        ItemStackDataHelper.putBoolean(stack, "IsDemonLord", isDemonLord);
                        ItemStackDataHelper.putBoolean(stack, "IsAwakenedDemonLord", isDemonLord);
                        ItemStackDataHelper.putString(stack, "DemonRank", getDemonRankTitle());
                        ItemStackDataHelper.putLong(stack, "CalculatedEP", getCalculatedEP());
                        ItemStackDataHelper.putFloat(stack, "AttackDamage", (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        ItemStackDataHelper.putFloat(stack, "CurrentHp", this.getHealth());
                        ItemStackDataHelper.putFloat(stack, "MaxHp", this.getMaxHealth());

                        // Lưu kỹ năng
                        StringBuilder sb = new StringBuilder();
                        for (PrimordialSkillPool.SkillEntry sk : getRandomSkills()) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(sk.id());
                        }
                        ItemStackDataHelper.putString(stack, "PrimordialSkills", sb.toString());
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRising()) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return false;
        }

        // ÁC MA ĐÃ KÝ KHẾ ƯỚC: Miễn nhiễm hoàn toàn sát thương từ Chủ Nhân và từ các Ác Ma đã ký khế ước khác
        if (this.isTame()) {
            if (source.getEntity() != null && this.isAlliedTo(source.getEntity())) {
                return false;
            }
            if (source.getDirectEntity() != null && this.isAlliedTo(source.getDirectEntity())) {
                return false;
            }
            if (source.getEntity() instanceof Player p && (this.getOwnerUUID() != null && this.getOwnerUUID().equals(p.getUUID()))) {
                return false;
            }
        }

        // TÍNH TOÁN RỦI RO CHIẾN ĐẤU VỚI BOSS
        // Riêng Cứu Thế Quyền Hiện: Tỉ lệ thắng 70% áp chế Thủy Tổ, không bị giảm trừ bởi kháng boss thông thường
        if (source.getEntity() instanceof com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity
                || source.getEntity() instanceof com.minhphuc.weapons.entity.darkgathering.CuuTheSoulEntity) {
            return super.hurt(source, amount);
        }

        if (source.getEntity() instanceof LivingEntity attacker && isBossTarget(attacker)) {
            DemonType type = getDemonType();
            if (type == DemonType.ROUGE || type == DemonType.NOIR) {
                if (this.random.nextFloat() < 0.10F) {
                    // 10% "chủ quan vẫn sẽ thua"
                    amount *= 2.0F;
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.8D, this.getZ(), 6, 0.3D, 0.3D, 0.3D, 0);
                    }
                } else {
                    amount *= 0.25F; // 90% kháng sát thương boss
                }
            } else if (type == DemonType.BLANC) {
                amount *= 0.35F; // 75%
            } else if (type == DemonType.JAUNE) {
                amount *= 0.45F; // 65%
            } else if (type == DemonType.VIOLET) {
                amount *= 0.80F; // 33%
            } else if (type == DemonType.VERT) {
                amount *= 0.85F; // 30%
            } else if (type == DemonType.BLEU) {
                amount *= 1.15F; // 20%
            }
        }

        // Rouge (Guy Crimson) triệu hồi Rain & Misery trợ chiến khi máu tụt xuống <= 80%
        if (!this.level().isClientSide() && getDemonType() == DemonType.ROUGE && !this.isTame() && !this.hasSummonedMinions) {
            if (this.getHealth() <= this.getMaxHealth() * 0.80F || (this.getTarget() != null && this.getHealth() <= this.getMaxHealth() * 0.90F)) {
                this.hasSummonedMinions = true;
                summonSupportMinions();
            }
        }

        // Nếu là Rouge và có hầu cận Misery (Vert) bảo vệ trong 32 block -> Giảm 40% sát thương
        if (getDemonType() == DemonType.ROUGE && !this.isTame()) {
            boolean hasMiseryGuard = !this.level().getEntitiesOfClass(PrimordialDemonEntity.class, this.getBoundingBox().inflate(32.0D),
                    d -> d.isAlive() && d.getDemonType() == DemonType.VERT && d.isSupportMinion()).isEmpty();
            if (hasMiseryGuard) {
                amount *= 0.60F;
            }
        }

        // KHI BỊ ĐÁNH VỀ 0 MÁU
        if (amount >= this.getHealth()) {
            if (this.isSupportMinion) {
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.08D);
                    sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 20, 0.3D, 0.5D, 0.3D, 0.05D);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.5F, 1.4F);
                }
                this.discard();
                return false;
            }

            if (!this.isTame()) {
                Player conqueror = null;
                if (source.getEntity() instanceof Player p) {
                    conqueror = p;
                } else if (this.getLastHurtByMob() instanceof Player p) {
                    conqueror = p;
                } else if (this.getTarget() instanceof Player p) {
                    conqueror = p;
                } else if (this.level() instanceof ServerLevel sl) {
                    conqueror = sl.getNearestPlayer(this, 32.0D);
                }

                if (conqueror != null) {
                    // Rớt vật phẩm Khế Ước Thủy Tổ tương ứng của ác ma đó
                    ItemStack pactStack = PrimordialPactItem.createPact(getDemonType(), conqueror);
                    ItemEntity itemEntity = new ItemEntity(
                            this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), pactStack
                    );
                    itemEntity.setDefaultPickUpDelay();
                    this.level().addFreshEntity(itemEntity);

                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2D, this.getZ(), 45, 0.5D, 0.8D, 0.5D, 0.15D);
                        sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.5D, this.getZ(), 2, 0, 0, 0, 0);
                        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.5F, 1.0F);
                    }

                    conqueror.displayClientMessage(Component.literal("§6§l★ ĐÃ ĐÁNH BẠI: §e" + getDemonType().getColorName() + " (" + getDemonType().getTitleVi() + ")!"), false);
                    conqueror.displayClientMessage(Component.literal("§a✔ Rơi ra: §e" + pactStack.getHoverName().getString() + "§a. Bấm Chuột Phải vào cuộn khế ước để triệu hồi và họ sẽ phục tùng bạn!"), false);

                    this.discard();
                    return false;
                }
            } else {
                // Đã thuần phục: Phục tùng trừ khi bị ai đó khác giết chết
                cleanDomainBarriers();
                if (this.getOwner() instanceof ServerPlayer sp) {
                    destroyOwnerPact(sp);
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        cleanDomainBarriers();
        if (this.isTame() && this.getOwner() instanceof ServerPlayer sp) {
            destroyOwnerPact(sp);
        }
        if (getDemonType() == DemonType.NOIR) {
            ServerPlayer killer = null;
            if (cause.getEntity() instanceof ServerPlayer sp) killer = sp;
            else if (cause.getDirectEntity() instanceof ServerPlayer sp) killer = sp;
            else if (this.getLastHurtByMob() instanceof ServerPlayer sp) killer = sp;

            if (killer != null) {
                com.minhphuc.weapons.data.EntityDataHelper.getCustomData(killer).putBoolean("TensuraMaterialCreation", true);
                killer.level().playSound(null, killer.getX(), killer.getY(), killer.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.0F);
                VoiceOfTheWorld.announce(killer, "Báo cáo: Đã đánh bại Hắc Sắc Thủy Tổ Noir. Cá thể đã lĩnh hội thành công Kỹ Năng Tối Thượng: Sáng Tạo Vật Chất (Material Creation).");
            }
        }
        super.die(cause);
    }

    public void destroyOwnerPact(ServerPlayer owner) {
        DemonType type = getDemonType();
        boolean foundAndRemoved = false;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack stack = owner.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PrimordialPactItem) {
                DemonType pactType = PrimordialPactItem.getDemonType(stack);
                String uuidStr = com.minhphuc.weapons.data.ItemStackDataHelper.getString(stack, "DemonUUID");
                if (pactType == type || this.getStringUUID().equals(uuidStr)) {
                    owner.getInventory().setItem(i, ItemStack.EMPTY);
                    foundAndRemoved = true;
                }
            }
        }
        if (foundAndRemoved) {
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.5F, 0.8F);
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.0F, 0.9F);
            if (owner.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LAVA, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 25, 0.4D, 0.5D, 0.4D, 0.1D);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.05D);
            }
            owner.sendSystemMessage(Component.literal("§4§l✦ [KHẾ ƯỚC TAN VỠ] ✦\n§cÁc Ma Thủy Tổ §6" + getEffectiveDemonName() + " §cđã tử trận trên chiến trường!\n§7Khế ước linh hồn đã bị đứt gãy, cuộn khế ước đã hóa thành tro tàn và tiêu biến vĩnh viễn!"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DemonType", getDemonType().ordinal());
        tag.putBoolean("IsWinged", isWinged());
        tag.putBoolean("HasPhysicalBody", hasPhysicalBody());
        tag.putBoolean("IsNamed", isNamed());
        tag.putInt("EvolutionTier", getEvolutionTier());
        tag.putBoolean("IsDemonLord", this.getTags().contains("TensuraDemonLordMob"));
        tag.putString("CustomDemonName", getCustomDemonName());
        tag.putBoolean("IsSupportMinion", this.isSupportMinion);
        tag.putBoolean("HasSummonedMinions", this.hasSummonedMinions);
        tag.putBoolean("MassacreMode", isMassacreMode());
        if (this.guardCenterPos != null) {
            tag.putInt("GuardCenterX", this.guardCenterPos.getX());
            tag.putInt("GuardCenterY", this.guardCenterPos.getY());
            tag.putInt("GuardCenterZ", this.guardCenterPos.getZ());
        }

        net.minecraft.nbt.ListTag skillsList = new net.minecraft.nbt.ListTag();
        for (PrimordialSkillPool.SkillEntry entry : randomSkills) {
            skillsList.add(net.minecraft.nbt.StringTag.valueOf(String.valueOf(entry.id())));
        }
        tag.put("PrimordialSkills", skillsList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DemonType")) {
            setDemonType(DemonType.byIndex(tag.getInt("DemonType")));
        }
        if (tag.contains("IsWinged")) {
            setWinged(tag.getBoolean("IsWinged"));
        }
        if (tag.contains("HasPhysicalBody")) {
            setPhysicalBody(tag.getBoolean("HasPhysicalBody"));
        }
        if (tag.contains("IsNamed")) {
            setNamed(tag.getBoolean("IsNamed"));
        }
        if (tag.contains("EvolutionTier")) {
            setEvolutionTier(tag.getInt("EvolutionTier"));
        }
        if (tag.contains("CustomDemonName")) {
            String cName = tag.getString("CustomDemonName");
            setCustomDemonName(cName);
            if (!cName.isEmpty()) {
                setNamed(true);
            }
        }
        if (isNamed()) {
            this.addTag("TensuraNamedMob");
        }
        if (tag.getBoolean("IsDemonLord")) {
            this.addTag("TensuraDemonLordMob");
            this.addTag("TensuraDemonLordCalm");
        }
        if (tag.contains("IsSupportMinion")) {
            this.isSupportMinion = tag.getBoolean("IsSupportMinion");
        }
        if (tag.contains("HasSummonedMinions")) {
            this.hasSummonedMinions = tag.getBoolean("HasSummonedMinions");
        }
        if (tag.contains("MassacreMode")) {
            this.setMassacreMode(tag.getBoolean("MassacreMode"));
        }
        if (tag.contains("GuardCenterX")) {
            this.guardCenterPos = new BlockPos(tag.getInt("GuardCenterX"), tag.getInt("GuardCenterY"), tag.getInt("GuardCenterZ"));
        }
        if (tag.contains("PrimordialSkills", 9)) {
            net.minecraft.nbt.ListTag list = tag.getList("PrimordialSkills", 8);
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                ids.add(list.getString(i));
            }
            setRandomSkillIds(ids);
        }
        ensureRandomSkills();
        updateAttributesForType(getDemonType());
    }

    private void summonSupportMinions() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0F, 1.2F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.5F, 0.8F);

        // Báo hiệu triệu hồi tổ đội của Rouge
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceTo(this.position()) <= 48.0D) {
                p.displayClientMessage(
                        Component.literal("§c§l[ROUGE - GUY CRIMSON] §eRa đây, Bleu, Vert! Hãy cho kẻ này thấy thế nào là sức mạnh của Khởi Nguyên!"),
                        false
                );
            }
        }

        // 1. Triệu hồi Rain (Bleu) - Băng Cực Hỗ Trợ
        PrimordialDemonEntity rain = new PrimordialDemonEntity(com.minhphuc.weapons.entity.ModEntities.PRIMORDIAL_DEMON.get(), sl);
        rain.setDemonType(DemonType.BLEU);
        rain.setSupportMinion(true);
        rain.setPhysicalBody(true);
        rain.setWinged(true);
        rain.moveTo(this.getX() + 3.0D, this.getY(), this.getZ() + 3.0D, this.getYRot(), 0.0F);
        rain.finalizeSpawn(sl, sl.getCurrentDifficultyAt(this.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
        if (this.getTarget() != null) rain.setTarget(this.getTarget());
        sl.addFreshEntity(rain);

        // 2. Triệu hồi Misery (Vert) - Kết Giới Hỗ Trợ
        PrimordialDemonEntity misery = new PrimordialDemonEntity(com.minhphuc.weapons.entity.ModEntities.PRIMORDIAL_DEMON.get(), sl);
        misery.setDemonType(DemonType.VERT);
        misery.setSupportMinion(true);
        misery.setPhysicalBody(true);
        misery.setWinged(true);
        misery.moveTo(this.getX() - 3.0D, this.getY(), this.getZ() - 3.0D, this.getYRot(), 0.0F);
        misery.finalizeSpawn(sl, sl.getCurrentDifficultyAt(this.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
        if (this.getTarget() != null) misery.setTarget(this.getTarget());
        sl.addFreshEntity(misery);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    public boolean isSkillSealed() {
        return this.sealedTicks > 0;
    }

    public void sealSkills(int durationTicks) {
        this.sealedTicks = Math.max(this.sealedTicks, durationTicks);
        this.skillCooldownTicks = Math.max(this.skillCooldownTicks, durationTicks);
        this.cleanDomainBarriers();
        this.activeDomainTicks = 0;
        this.clearActiveCircles();
    }

    public void clearActiveCircles() {
        for (ActiveDisplayCircle circle : this.activeCircles) {
            if (circle.display != null && circle.display.isAlive()) {
                circle.display.discard();
            }
        }
        this.activeCircles.clear();
    }

    public boolean canCastSkill() {
        if (isSkillSealed()) {
            return false;
        }
        if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(this.level(), this.position())) {
            return false;
        }
        return skillCooldownTicks <= 0 && !isRising();
    }

    public void triggerSkillCooldown(int ticks) {
        this.skillCooldownTicks = ticks;
    }

    // =========================================================================
    // CỘT SÁNG & MA TRẬN PHÁP BẢO (ITEM DISPLAY HELPER)
    // =========================================================================
    public Display.ItemDisplay createPillarDisplay(ServerLevel level, Vec3 center,
                                                  float scaleX, float scaleY, float scaleZ,
                                                  int glowColor, int duration) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.JACOB_LIGHT_PILLAR.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(12.0F);

            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, scaleY / 2.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(scaleX, scaleY, scaleZ),
                    null
            ));

            level.addFreshEntity(display);
            this.activeCircles.add(new ActiveDisplayCircle(display, duration, 0.0F, scaleX, false, Vec3.ZERO));
        }
        return display;
    }

    public void announceSkill(String skillName) {
        if (this.level() instanceof ServerLevel level) {
            for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(this) <= 36.0D * 36.0D)) {
                sp.displayClientMessage(Component.literal("§d§l[" + getDemonType().getColorName().toUpperCase() + "] §6§l✦ " + skillName + " ✦"), true);
            }
        }
    }

    // =========================================================================
    // HỆ THỐNG KỸ NĂNG TÁC CHIẾN TOÀN DIỆN CHO 7 ÁC MA THỦY TỔ
    // =========================================================================

    public void executeNormalAttack(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();
        this.swing(InteractionHand.MAIN_HAND);

        if (type == DemonType.NOIR) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 0.9F);
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.8F, 1.3F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, tPos.x, tPos.y + 1.2D, tPos.z, 3, 0.4D, 0.4D, 0.4D, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tPos.x, tPos.y + 1.0D, tPos.z, 15, 0.3D, 0.3D, 0.3D, 0.05D);
            dealDemonicDamage(target, 160.0F);
        } else if (type == DemonType.ROUGE) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.FLAME, tPos.x, tPos.y + 1.0D, tPos.z, 20, 0.4D, 0.4D, 0.4D, 0.08D);
            dealDemonicDamage(target, 160.0F);
        } else if (type == DemonType.BLANC) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 2.0F, 1.8F);
            level.sendParticles(ParticleTypes.END_ROD, tPos.x, tPos.y + 1.0D, tPos.z, 15, 0.3D, 0.3D, 0.3D, 0.05D);
            dealDemonicDamage(target, 80.0F);
        } else if (type == DemonType.JAUNE) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.8F, 1.2F);
            level.sendParticles(ParticleTypes.CRIT, tPos.x, tPos.y + 1.0D, tPos.z, 25, 0.5D, 0.5D, 0.5D, 0.1D);
            dealDemonicDamage(target, 70.0F);
        } else if (type == DemonType.VIOLET) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 2.0F, 1.2F);
            level.sendParticles(ParticleTypes.WITCH, tPos.x, tPos.y + 1.0D, tPos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
            dealDemonicDamage(target, 40.0F);
        } else if (type == DemonType.BLEU) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.2F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, tPos.x, tPos.y + 1.0D, tPos.z, 20, 0.4D, 0.4D, 0.4D, 0.02D);
            dealDemonicDamage(target, 30.0F);
        } else {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 1.4F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tPos.x, tPos.y + 1.0D, tPos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
            dealDemonicDamage(target, 36.0F);
        }
    }

    public void executeSkillOne(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 dPos = this.position();
        Vec3 tPos = target.position();
        Vec3 look = this.getLookAngle();
        Vec3 circlePos = dPos.add(look.scale(1.8D)).add(0, 1.2D, 0);

        announceSkill(type.getColorName() + ": " + getSkillOneName(type));
        level.playSound(null, dPos.x, dPos.y, dPos.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.5F, 1.2F);

        if (type == DemonType.JAUNE) {
            spawnRotatingCircle(level, circlePos, 3.8F, 8.0F, 45, false, type.getGlowColor(), new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE.get()));
            spawnRotatingCircle(level, circlePos, 2.5F, -10.0F, 45, false, 0xFF4500, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get()));
        } else {
            spawnRotatingCircle(level, circlePos, 3.2F, 6.0F, 40, false, type.getGlowColor(), new ItemStack(type.getMagicCircleItem().get()));
            spawnRotatingCircle(level, circlePos, 2.2F, -8.0F, 40, false, type.getGlowColor(), new ItemStack(type.getMagicCircleItem().get()));
        }

        Vec3 dir = tPos.add(0, 0.8D, 0).subtract(circlePos).normalize();
        for (double d = 0.5D; d <= circlePos.distanceTo(tPos); d += 0.8D) {
            Vec3 p = circlePos.add(dir.scale(d));
            if (type == DemonType.NOIR) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 3, 0.1D, 0.1D, 0.1D, 0.01D);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0.02D);
            } else if (type == DemonType.ROUGE) {
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 4, 0.1D, 0.1D, 0.1D, 0.03D);
            } else if (type == DemonType.BLANC) {
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.05D, 0.05D, 0.05D, 0.01D);
            } else if (type == DemonType.JAUNE) {
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 4, 0.1D, 0.1D, 0.1D, 0.05D);
                level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0.02D);
            } else if (type == DemonType.VIOLET) {
                level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
            } else if (type == DemonType.BLEU) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
            } else {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
            }
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tPos.x, tPos.y + 1.0D, tPos.z, 1, 0, 0, 0, 0);
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.2F);

        float damage = (float) (type.getAttackDamage() * 1.5D);
        dealDemonicDamage(target, damage);
        dispelOpposingMagic(tPos, 14.0D);
        if (type == DemonType.JAUNE) {
            applyCarreraBlockDestruction(tPos, 2.6F);
        }
    }

    public void executeSkillTwo(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": " + getSkillTwoName(type));

        if (type == DemonType.NOIR) {
            Vec3 behind = tPos.add(target.getLookAngle().scale(-1.8D));
            this.teleportTo(behind.x, behind.y, behind.z);
            level.playSound(null, behind.x, behind.y, behind.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.4F);
            level.sendParticles(ParticleTypes.FLASH, behind.x, behind.y + 1.2D, behind.z, 2, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, tPos.x, tPos.y + 1.2D, tPos.z, 6, 0.5D, 0.5D, 0.5D, 0);
            dealDemonicDamage(target, 280.0F);
            applyDemonicEffect(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
        } else if (type == DemonType.ROUGE) {
            Vec3 dashPos = tPos.add(this.getLookAngle().scale(2.5D));
            this.teleportTo(dashPos.x, dashPos.y, dashPos.z);
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2.5F, 0.8F);
            level.sendParticles(ParticleTypes.LAVA, tPos.x, tPos.y + 1.0D, tPos.z, 30, 0.5D, 0.8D, 0.5D, 0.1D);
            dealDemonicDamage(target, 280.0F);
            target.setRemainingFireTicks(180);
        } else if (type == DemonType.BLANC) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.6F);
            Vec3 pull = this.position().subtract(tPos).normalize().scale(1.2D);
            target.setDeltaMovement(pull.add(0, 0.4D, 0));
            target.hasImpulse = true;
            dealDemonicDamage(target, 150.0F);
        } else if (type == DemonType.JAUNE) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.4F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0F, 1.6F);

            for (int ring = 0; ring < 4; ring++) {
                double offAng = ring * Math.PI / 2.0D;
                Vec3 ringPos = this.position().add(Math.cos(offAng) * 2.0D, 1.2D, Math.sin(offAng) * 2.0D);
                spawnRotatingCircle(level, ringPos, 1.5F, 15.0F, 30, false, 0xFFCC00, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get()));
            }

            for (int b = 0; b < 6; b++) {
                Vec3 bulletPos = tPos.add((this.random.nextDouble() - 0.5D) * 3.0D, 0.5D + b * 0.3D, (this.random.nextDouble() - 0.5D) * 3.0D);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, bulletPos.x, bulletPos.y, bulletPos.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, bulletPos.x, bulletPos.y, bulletPos.z, 20, 0.3D, 0.3D, 0.3D, 0.1D);
            }
            dealDemonicDamage(target, 140.0F);
            target.setDeltaMovement(0, 1.2D, 0);
            target.hasImpulse = true;
        } else if (type == DemonType.VIOLET) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 2.5F, 0.8F);
            level.sendParticles(ParticleTypes.WITCH, tPos.x, tPos.y + 1.0D, tPos.z, 50, 2.0D, 1.0D, 2.0D, 0.05D);
            dealDemonicDamage(target, 70.0F);
            applyDemonicEffect(target, new MobEffectInstance(MobEffects.POISON, 120, 2));
        } else if (type == DemonType.BLEU) {
            level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 2.5F, 1.0F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), tPos.x, tPos.y + 0.8D, tPos.z, 25, 0.3D, 0.8D, 0.3D, 0.05D);
            dealDemonicDamage(target, 55.0F);
            target.setTicksFrozen(240);
        } else {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.5F, 1.5F);
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX() + Math.cos(rad) * 3.0D, this.getY() + 1.0D, this.getZ() + Math.sin(rad) * 3.0D, 3, 0, 0, 0, 0.05D);
            }
            dealDemonicDamage(target, 60.0F);
            if (this.getOwner() instanceof LivingEntity owner) {
                owner.heal(25.0F);
            }
        }
        dispelOpposingMagic(tPos, 14.0D);
        if (type == DemonType.JAUNE) {
            applyCarreraBlockDestruction(tPos, 2.8F);
        }
    }

    public void executeSkillThree(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": " + getSkillThreeName(type));

        if (type == DemonType.JAUNE) {
            spawnRotatingCircle(level, tPos.add(0, 0.05D, 0), 8.5F, 4.0F, 80, true, 0xFFA500, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get()));
            spawnRotatingCircle(level, tPos.add(0, 7.0D, 0), 6.5F, -6.0F, 80, true, 0xFFFF00, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get()));
            spawnRotatingCircle(level, tPos.add(0, 14.0D, 0), 8.5F, 4.0F, 80, true, 0xFFCC00, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE.get()));
        } else {
            spawnRotatingCircle(level, tPos.add(0, 0.05D, 0), 7.0F, 4.0F, 70, true, type.getGlowColor(), new ItemStack(type.getMagicCircleItem().get()));
            spawnRotatingCircle(level, tPos.add(0, 14.0D, 0), 7.0F, -4.0F, 70, true, type.getGlowColor(), new ItemStack(type.getMagicCircleItem().get()));
        }

        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.5F);
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 0.8F);

        for (double y = 0.0D; y <= 14.0D; y += 0.5D) {
            for (int i = 0; i < 6; i++) {
                double angle = (i * Math.PI / 3.0D) + (y * 0.4D);
                double px = tPos.x + Math.cos(angle) * 1.6D;
                double pz = tPos.z + Math.sin(angle) * 1.6D;

                if (type == DemonType.NOIR) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, tPos.y + y, pz, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, px, tPos.y + y, pz, 1, 0.1D, 0.1D, 0.1D, 0.02D);
                } else if (type == DemonType.ROUGE) {
                    level.sendParticles(ParticleTypes.FLAME, px, tPos.y + y, pz, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                } else if (type == DemonType.BLANC) {
                    level.sendParticles(ParticleTypes.END_ROD, px, tPos.y + y, pz, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                } else if (type == DemonType.JAUNE) {
                    level.sendParticles(ParticleTypes.CRIT, px, tPos.y + y, pz, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                    level.sendParticles(ParticleTypes.LAVA, px, tPos.y + y, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
                } else if (type == DemonType.VIOLET) {
                    level.sendParticles(ParticleTypes.WITCH, px, tPos.y + y, pz, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                } else if (type == DemonType.BLEU) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, px, tPos.y + y, pz, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                } else {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, tPos.y + y, pz, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                }
            }
        }

        level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 2.0D, tPos.z, 2, 0, 0, 0, 0);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(tPos.x - 6, tPos.y - 2, tPos.z - 6, tPos.x + 6, tPos.y + 15, tPos.z + 6),
                e -> e != this && e != this.getOwner()
        );

        float damage = (float) (type.getAttackDamage() * 2.2D);
        if (type == DemonType.JAUNE) {
            damage = 240.0F;
        }
        for (LivingEntity e : targets) {
            dealDemonicDamage(e, damage);
        }
        dispelOpposingMagic(tPos, 16.0D);
        if (type == DemonType.JAUNE) {
            applyCarreraBlockDestruction(tPos, 3.2F);
        }
    }

    // =========================================================================
    // KỸ NĂNG MỚI 1: THẤT SẮC THẦN TRỤ • CỘT SÁNG CỰC ĐẠI
    // =========================================================================
    public void executeChromaticLightPillar(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": Thất Sắc Thần Trụ • Cột Sáng Cực Đại");

        // 1. Tạo Cột Sáng Thần Khí vươn cao chọc trời 60m (3D Cinematic hoặc Cũ)
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_CHROMATIC_PILLAR)) {
            float demonCode = (float) type.ordinal();
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_CHROMATIC_PILLAR,
                    this.getUUID(),
                    tPos, new Vec3(0, 1, 0), 80, ClientboundCinematicSkillPacket.ACTION_START, demonCode
            ));
        } else {
            createPillarDisplay(level, tPos, 6.5F, 60.0F, 6.5F, type.getGlowColor(), 80);
        }

        // 2. Pháp trận xoay dưới đất và trên đỉnh cột sáng
        spawnRotatingCircle(level, tPos.add(0, 0.05D, 0), 8.0F, 6.0F, 80, true, type.getGlowColor());
        spawnRotatingCircle(level, tPos.add(0, 16.0D, 0), 8.0F, -6.0F, 80, true, type.getGlowColor());

        // 3. Vòng xoáy 7 sắc cầu vồng bao quanh thân cột sáng
        for (double y = 0.0D; y <= 24.0D; y += 1.0D) {
            for (int c = 0; c < 7; c++) {
                double angle = (c * (2 * Math.PI / 7)) + (y * 0.5D);
                double px = tPos.x + Math.cos(angle) * 2.8D;
                double pz = tPos.z + Math.sin(angle) * 2.8D;

                switch (c) {
                    case 0 -> level.sendParticles(ParticleTypes.FLAME, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    case 1 -> level.sendParticles(ParticleTypes.LAVA, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    case 2 -> level.sendParticles(ParticleTypes.CRIT, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    case 3 -> level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    case 4 -> level.sendParticles(ParticleTypes.SNOWFLAKE, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    case 5 -> level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                    case 6 -> level.sendParticles(ParticleTypes.WITCH, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
                }
            }
        }

        level.playSound(null, tPos.x, tPos.y + 4.0D, tPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.3F);
        level.playSound(null, tPos.x, tPos.y + 4.0D, tPos.z, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 3.0F, 1.0F);
        level.playSound(null, tPos.x, tPos.y + 4.0D, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.1F);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(tPos.x - 7, tPos.y - 2, tPos.z - 7, tPos.x + 7, tPos.y + 20, tPos.z + 7),
                e -> e != this && e != this.getOwner()
        );

        for (LivingEntity e : targets) {
            dealDemonicDamage(e, 340.0F);
            e.setDeltaMovement(0, 0.6D, 0);
            e.hasImpulse = true;
        }
        dispelOpposingMagic(tPos, 20.0D);
        if (type == DemonType.JAUNE) {
            applyCarreraBlockDestruction(tPos, 3.5F);
        }
    }

    // =========================================================================
    // KỸ NĂNG MỚI 2: LINH TỬ BĂNG HOẠI (DISINTEGRATION)
    // =========================================================================
    public void executeDisintegration(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": Linh Tử Băng Hoại (Disintegration)");

        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DISINTEGRATION)) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DISINTEGRATION,
                    this.getUUID(),
                    tPos, new Vec3(0, 1, 0), 100, ClientboundCinematicSkillPacket.ACTION_START, 1.0f
            ));
        }

        // 1. Pháp trận đồng tâm đa tầng
        spawnRotatingCircle(level, tPos.add(0, 0.05D, 0), 8.5F, 6.0F, 70, true, 0xFFFFFF);
        spawnRotatingCircle(level, tPos.add(0, 4.0D, 0), 6.0F, -8.0F, 70, true, 0xF0F4F8);
        spawnRotatingCircle(level, tPos.add(0, 8.5D, 0), 7.0F, 4.0F, 70, false, 0xFFFFFF);

        // 2. Chùm tia linh tử phân rã trắng chói lòa
        for (double y = 0.0D; y <= 20.0D; y += 0.4D) {
            for (int i = 0; i < 8; i++) {
                double angle = (i * Math.PI / 4.0D) + (y * 0.3D);
                double px = tPos.x + Math.cos(angle) * 1.4D;
                double pz = tPos.z + Math.sin(angle) * 1.4D;
                level.sendParticles(ParticleTypes.END_ROD, px, tPos.y + y, pz, 1, 0, 0, 0, 0.02D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, tPos.y + y, pz, 1, 0, 0, 0, 0.01D);
            }
        }

        level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 2.0D, tPos.z, 3, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tPos.x, tPos.y + 1.0D, tPos.z, 2, 0, 0, 0, 0);

        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.2F);
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.6F);
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0F, 1.8F);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(tPos.x - 8, tPos.y - 2, tPos.z - 8, tPos.x + 8, tPos.y + 16, tPos.z + 8),
                e -> e != this && e != this.getOwner()
        );

        for (LivingEntity e : targets) {
            dealDemonicDamage(e, 420.0F);
            applyDemonicEffect(e, new MobEffectInstance(MobEffects.WEAKNESS, 160, 3));
            applyDemonicEffect(e, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        }
        dispelOpposingMagic(tPos, 20.0D);
        if (type == DemonType.JAUNE) {
            applyCarreraBlockDestruction(tPos, 3.5F);
        }
    }

    // =========================================================================
    // KỸ NĂNG MỚI: LINH TỬ BỘC PHÁT (SPIRITRON OUTBURST - GRANITE BLAST THEO MÀU)
    // =========================================================================
    public void executeSpiritronOutburst(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 eyePos = this.getEyePosition(1.0F);
        Vec3 tPos = target.getEyePosition(1.0F);
        Vec3 dir = tPos.subtract(eyePos).normalize();

        announceSkill(type.getColorName() + ": Linh Tử Bộc Phát (Spiritron Outburst)");

        // 1. Triệu hồi ma pháp trận xoay trước nòng bắn
        Vec3 circlePos = eyePos.add(dir.scale(1.8D));
        spawnRotatingCircle(level, circlePos, 5.0F, 14.0F, 35, false, type.getGlowColor());
        spawnRotatingCircle(level, circlePos.add(dir.scale(0.35D)), 3.8F, -18.0F, 35, false, 0xFFFFFF);

        // 2. Phát âm thanh đại bác siêu thanh và sấm rền
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.5F, 0.7F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 1.1F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.5F, 1.4F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.5F, 1.5F);

        // 3. Khai hỏa chùm pháo Granit / Linh Tử Bộc Phát theo màu sắc
        float dmg = hasPhysicalBody() ? 560.0F : 400.0F;
        HorizontalHolyBeamAbility.fireSpiritronOutburst(level, this, eyePos, dir, type, 56.0D, dmg);
        Vec3 impact = eyePos.add(dir.scale(28.0D));
        dispelOpposingMagic(impact, 18.0D);
        if (type == DemonType.JAUNE) {
            applyCarreraBlockDestruction(impact, 3.2F);
        }
    }

    // =========================================================================
    // KỸ NĂNG MỚI 3: CHIÊU ĐỘC BẢN 1 CỦA TỪNG ÁC MA
    // =========================================================================
    public void executeSignatureSkillOne(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": " + getSignatureOneName(type));

        switch (type) {
            case ROUGE -> {
                // Xích Diễm Hỏa Long Bộc Phá: Lượn sóng rồng lửa càn quét 20m
                Vec3 dir = target.position().subtract(this.position()).normalize();
                for (double d = 1.0D; d <= 20.0D; d += 0.8D) {
                    Vec3 p = this.position().add(dir.scale(d)).add(0, Math.sin(d * 0.8D) * 1.2D, 0);
                    level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 1.0D, p.z, 8, 0.4D, 0.4D, 0.4D, 0.05D);
                    level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 1.0D, p.z, 3, 0.2D, 0.2D, 0.2D, 0.02D);
                    if (d % 4.0D < 1.0D) {
                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 1.0D, p.z, 1, 0, 0, 0, 0);
                    }
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0F, 0.9F);
                dealDemonicDamage(target, 300.0F);
                target.setRemainingFireTicks(200);
            }
            case NOIR -> {
                // Hắc Ám Không Gian Phân Cắt: Cắt đứt không gian tạo hố đen nuốt chửng
                Vec3 mid = this.position().add(target.position()).scale(0.5D);
                for (double y = 0.0D; y <= 6.0D; y += 0.4D) {
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y + y, mid.z, 4, 0.2D, 0.2D, 0.2D, 0);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, mid.x, mid.y + y, mid.z, 6, 0.3D, 0.3D, 0.3D, 0.02D);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mid.x, mid.y + y, mid.z, 4, 0.2D, 0.2D, 0.2D, 0.02D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 1.5F);
                dealDemonicDamage(target, 320.0F);
                applyDemonicEffect(target, new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            }
            case BLANC -> {
                // Bạch Quang Tuyệt Diệt Tuyến: 4 gương ma pháp bắn chùm laser trắng hội tụ
                for (int i = 0; i < 4; i++) {
                    double ang = i * Math.PI / 2.0D;
                    Vec3 mPos = tPos.add(Math.cos(ang) * 5.0D, 2.5D, Math.sin(ang) * 5.0D);
                    spawnRotatingCircle(level, mPos, 2.0F, 10.0F, 40, false, 0xF0F4F8);

                    Vec3 bDir = tPos.subtract(mPos).normalize();
                    for (double d = 0.5D; d <= 5.0D; d += 0.5D) {
                        Vec3 bp = mPos.add(bDir.scale(d));
                        level.sendParticles(ParticleTypes.END_ROD, bp.x, bp.y, bp.z, 2, 0.02D, 0.02D, 0.02D, 0.01D);
                        level.sendParticles(ParticleTypes.FLASH, bp.x, bp.y, bp.z, 1, 0, 0, 0, 0);
                    }
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 3.0F, 1.8F);
                dealDemonicDamage(target, 280.0F);
            }
            case JAUNE -> {
                // Carrera Gatling Barrage: Xả 16 ma đạn hoàng kim liên hoàn
                for (int b = 0; b < 16; b++) {
                    Vec3 bp = tPos.add((this.random.nextDouble() - 0.5D) * 4.0D, this.random.nextDouble() * 2.0D, (this.random.nextDouble() - 0.5D) * 4.0D);
                    level.sendParticles(ParticleTypes.CRIT, bp.x, bp.y, bp.z, 15, 0.2D, 0.2D, 0.2D, 0.05D);
                    level.sendParticles(ParticleTypes.LAVA, bp.x, bp.y, bp.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.6F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0F, 1.4F);
                dealDemonicDamage(target, 300.0F);
            }
            case VIOLET -> {
                // Tử Hoa Mạn Đà La: Khai nở mandala hoa sen độc trên mặt đất 8m
                for (int a = 0; a < 360; a += 20) {
                    double rad = Math.toRadians(a);
                    double rx = tPos.x + Math.cos(rad) * 4.5D;
                    double rz = tPos.z + Math.sin(rad) * 4.5D;
                    level.sendParticles(ParticleTypes.WITCH, rx, tPos.y + 0.2D, rz, 4, 0.1D, 0.2D, 0.1D, 0.02D);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, rx, tPos.y + 0.4D, rz, 2, 0.1D, 0.1D, 0.1D, 0.01D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 3.0F, 0.8F);
                dealDemonicDamage(target, 200.0F);
                applyDemonicEffect(target, new MobEffectInstance(MobEffects.WITHER, 160, 3));
                applyDemonicEffect(target, new MobEffectInstance(MobEffects.POISON, 160, 3));
            }
            case BLEU -> {
                // Băng Cực Phong Ấn Vĩnh Cửu: Đóng băng mục tiêu trong kết giới hàn băng
                for (int a = 0; a < 360; a += 30) {
                    double rad = Math.toRadians(a);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                            tPos.x + Math.cos(rad) * 2.0D, tPos.y + 1.0D, tPos.z + Math.sin(rad) * 2.0D, 10, 0.2D, 0.5D, 0.2D, 0.02D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0F, 0.8F);
                target.setTicksFrozen(360);
                dealDemonicDamage(target, 180.0F);
                applyDemonicEffect(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
            }
            case VERT -> {
                // Lục Bảo Phong Bạo Cuồng Nộ: Lốc xoáy ngọc bích hất tung kẻ địch lên không
                for (double y = 0.0D; y <= 12.0D; y += 0.8D) {
                    double r = 1.0D + y * 0.3D;
                    for (int i = 0; i < 4; i++) {
                        double ang = (i * Math.PI / 2.0D) + (y * 0.8D);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tPos.x + Math.cos(ang) * r, tPos.y + y, tPos.z + Math.sin(ang) * r, 2, 0, 0, 0, 0.05D);
                    }
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 3.0F, 0.7F);
                target.setDeltaMovement(0, 1.4D, 0);
                target.hasImpulse = true;
                dealDemonicDamage(target, 200.0F);
            }
        }
    }

    // =========================================================================
    // KỸ NĂNG MỚI 4: CHIÊU ĐỘC BẢN 2 CỦA TỪNG ÁC MA
    // =========================================================================
    public void executeSignatureSkillTwo(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": " + getSignatureTwoName(type));

        switch (type) {
            case ROUGE -> {
                // Hỏa Ngục Toái Phiến Tiễn: 8 mũi giáo lửa giáng xuống từ vòng tròn trên không
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4.0D;
                    Vec3 spearPos = tPos.add(Math.cos(ang) * 4.0D, 8.0D, Math.sin(ang) * 4.0D);
                    level.sendParticles(ParticleTypes.FLAME, spearPos.x, spearPos.y, spearPos.z, 15, 0.2D, 1.0D, 0.2D, 0.05D);
                    level.sendParticles(ParticleTypes.LAVA, spearPos.x, spearPos.y, spearPos.z, 3, 0.1D, 0.5D, 0.1D, 0.02D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 3.0F, 1.2F);
                dealDemonicDamage(target, 280.0F);
            }
            case NOIR -> {
                // Phán Quyết Tuyệt Vọng: Máy chém bóng tối rơi từ thiên đỉnh
                Vec3 dropPos = tPos.add(0, 10.0D, 0);
                for (double y = 0.0D; y <= 10.0D; y += 0.5D) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tPos.x, tPos.y + y, tPos.z, 4, 0.3D, 0.2D, 0.3D, 0.02D);
                    level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + y, tPos.z, 1, 0, 0, 0, 0);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 0.7F);
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.5F, 1.2F);
                dealDemonicDamage(target, 350.0F);
            }
            case BLANC -> {
                // Bạch Viêm Huyết Vũ: Cơn mưa lửa trắng rơi từ đám mây 12m
                for (int d = 0; d < 25; d++) {
                    double ox = (this.random.nextDouble() - 0.5D) * 10.0D;
                    double oz = (this.random.nextDouble() - 0.5D) * 10.0D;
                    level.sendParticles(ParticleTypes.END_ROD, tPos.x + ox, tPos.y + 6.0D, tPos.z + oz, 3, 0.1D, 0.8D, 0.1D, 0.04D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.8F);
                dealDemonicDamage(target, 240.0F);
            }
            case JAUNE -> {
                // Trọng Lực Thâm Uyên Điểm Kỳ Dị: Hút mọi quái trong 16m về tâm rồi nổ tung
                List<LivingEntity> cluster = level.getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(16.0D),
                        e -> e != this && e != this.getOwner()
                );
                for (LivingEntity e : cluster) {
                    Vec3 pull = tPos.subtract(e.position()).normalize().scale(1.2D);
                    e.setDeltaMovement(pull.add(0, 0.3D, 0));
                    e.hasImpulse = true;
                    dealDemonicDamage(e, 270.0F);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 1.6F);
            }
            case VIOLET -> {
                // Huyết Độc Ma Trảo Tam Kích: Tốc biến 3 lần cào xé mục tiêu
                for (int k = 0; k < 3; k++) {
                    double ang = k * (2 * Math.PI / 3);
                    Vec3 dashP = tPos.add(Math.cos(ang) * 2.0D, 0.5D, Math.sin(ang) * 2.0D);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, dashP.x, dashP.y + 1.0D, dashP.z, 5, 0.3D, 0.3D, 0.3D, 0);
                    level.sendParticles(ParticleTypes.WITCH, dashP.x, dashP.y + 1.0D, dashP.z, 15, 0.3D, 0.3D, 0.3D, 0.05D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 3.0F, 1.5F);
                dealDemonicDamage(target, 260.0F);
            }
            case BLEU -> {
                // Bão Băng Ngưng Đọng: Cơn mưa gai băng cực hàn
                for (int b = 0; b < 20; b++) {
                    double ox = (this.random.nextDouble() - 0.5D) * 8.0D;
                    double oz = (this.random.nextDouble() - 0.5D) * 8.0D;
                    level.sendParticles(ParticleTypes.SNOWFLAKE, tPos.x + ox, tPos.y + 5.0D, tPos.z + oz, 5, 0.2D, 0.6D, 0.2D, 0.02D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 3.0F, 1.2F);
                dealDemonicDamage(target, 220.0F);
            }
            case VERT -> {
                // Lục Bảo Thánh Thuẫn: Ban giáp bảo hộ cho chủ nhân & ác ma
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 3.0F, 1.4F);
                spawnRotatingCircle(level, this.position().add(0, 1.0D, 0), 4.5F, 12.0F, 60, true, 0x22C55E);
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
                if (this.getOwner() instanceof LivingEntity owner) {
                    owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
                    owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
                    owner.heal(30.0F);
                }
            }
        }
    }

    // =========================================================================
    // KỸ NĂNG MỚI 5: CHIÊU ĐỘC BẢN 3 CỦA TỪNG ÁC MA
    // =========================================================================
    public void executeSignatureSkillThree(LivingEntity target, DemonType type) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 tPos = target.position();

        announceSkill(type.getColorName() + ": " + getSignatureThreeName(type));

        switch (type) {
            case ROUGE -> {
                // Hỏa Đế Trảm Không: Nhảy vút lên không rồi bổ xuống chém nứt mặt đất
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tPos.x, tPos.y + 1.0D, tPos.z, 2, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LAVA, tPos.x, tPos.y + 0.5D, tPos.z, 40, 1.5D, 0.5D, 1.5D, 0.1D);
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.5F, 0.8F);
                dealDemonicDamage(target, 360.0F);
            }
            case NOIR -> {
                // Hắc Linh Hồn Thôn Phệ: Hút cạn sinh lực kẻ thù xung quanh hồi máu cho Diablo
                List<LivingEntity> victims = level.getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(16.0D),
                        e -> e != this && e != this.getOwner()
                );
                for (LivingEntity v : victims) {
                    for (double d = 0.2D; d <= 1.0D; d += 0.2D) {
                        Vec3 p = v.position().lerp(this.position(), d);
                        level.sendParticles(ParticleTypes.SOUL, p.x, p.y + 1.0D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    dealDemonicDamage(v, 200.0F);
                }
                this.heal(60.0F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 3.0F, 1.4F);
            }
            case BLANC -> {
                // Thánh Ma Phân Rã Ba Động: 3 vòng sóng xung kích phân rã
                for (int ring = 1; ring <= 3; ring++) {
                    double r = ring * 3.0D;
                    for (int a = 0; a < 360; a += 15) {
                        double rad = Math.toRadians(a);
                        level.sendParticles(ParticleTypes.END_ROD, tPos.x + Math.cos(rad) * r, tPos.y + 0.5D, tPos.z + Math.sin(rad) * r, 2, 0, 0, 0, 0.02D);
                    }
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3.0F, 1.6F);
                dealDemonicDamage(target, 260.0F);
                applyDemonicEffect(target, new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
            }
            case JAUNE -> {
                // Hạch Tâm Quá Tải: Nấm mây vàng rực bộc phá
                level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 2.0D, tPos.z, 3, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tPos.x, tPos.y + 1.0D, tPos.z, 3, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, tPos.x, tPos.y + 1.5D, tPos.z, 60, 2.0D, 2.0D, 2.0D, 0.2D);
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0F, 0.6F);
                dealDemonicDamage(target, 350.0F);
            }
            case VIOLET -> {
                // Tử Khí Trầm Luân: Cột khói tím độc phun trào từ lòng đất
                for (double y = 0.0D; y <= 8.0D; y += 0.5D) {
                    level.sendParticles(ParticleTypes.WITCH, tPos.x, tPos.y + y, tPos.z, 10, 0.8D, 0.2D, 0.8D, 0.05D);
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 3.0F, 1.2F);
                dealDemonicDamage(target, 220.0F);
                applyDemonicEffect(target, new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            }
            case BLEU -> {
                // Băng Kính Khởi Nguyên: 3 ảnh phân thân băng bắn tia hàn băng hội tụ
                for (int i = 0; i < 3; i++) {
                    double ang = i * (2 * Math.PI / 3);
                    Vec3 mirPos = tPos.add(Math.cos(ang) * 4.0D, 1.5D, Math.sin(ang) * 4.0D);
                    spawnRotatingCircle(level, mirPos, 2.0F, 8.0F, 40, false, 0x2563EB);
                    for (double d = 0.5D; d <= 4.0D; d += 0.5D) {
                        Vec3 lp = mirPos.lerp(tPos, d / 4.0D);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, lp.x, lp.y, lp.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                    }
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0F, 1.4F);
                dealDemonicDamage(target, 250.0F);
            }
            case VERT -> {
                // Lục Lôi Ma Trận: 5 luồng sét lục bảo giáng xuống đồng loạt
                for (int s = 0; s < 5; s++) {
                    double ox = (this.random.nextDouble() - 0.5D) * 6.0D;
                    double oz = (this.random.nextDouble() - 0.5D) * 6.0D;
                    for (double y = 0.0D; y <= 16.0D; y += 0.8D) {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tPos.x + ox, tPos.y + y, tPos.z + oz, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                    }
                }
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.4F);
                dealDemonicDamage(target, 260.0F);
            }
        }
    }

    public void executeSkillFourDomain(LivingEntity target, DemonType type) {
        Vec3 center = this.position();
        String domainName = getDomainName(type);
        String voiceLine = getDomainVoiceLine(type);
        this.activateDomain(type, center, domainName, voiceLine);
    }

    // =========================================================================
    // LỆNH THI TRIỂN KỸ NĂNG TỪ XA (COMMAND EXECUTION TỪ CUỘN KHẾ ƯỚC)
    // =========================================================================
    public void executeCommandedSkill(int skillIndex, LivingEntity target) {
        if (target == null || !target.isAlive() || isImmuneOrFriendly(target)) {
            target = this.getTarget();
            if (target != null && (!target.isAlive() || isImmuneOrFriendly(target))) {
                target = null;
            }
        }
        if (target == null) {
            List<LivingEntity> nearbyHostiles = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(48.0D),
                    e -> e != this && !isImmuneOrFriendly(e) && e.isAlive() && (e instanceof Enemy || (isMassacreMode() && (e instanceof net.minecraft.world.entity.npc.AbstractVillager || e instanceof net.minecraft.world.entity.animal.IronGolem)))
            );
            if (!nearbyHostiles.isEmpty()) {
                nearbyHostiles.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
                target = nearbyHostiles.get(0);
            } else {
                List<LivingEntity> anyValid = this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(48.0D),
                        e -> e != this && !isImmuneOrFriendly(e) && e.isAlive()
                );
                if (!anyValid.isEmpty()) {
                    anyValid.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
                    target = anyValid.get(0);
                }
            }
        }

        if (target != null) {
            this.setTarget(target);
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // Toàn bộ kỹ năng Khế Ước (100 -> 112) được mở khóa hoàn toàn, không cần ràng buộc Tên hay Thể Xác!
        if (skillIndex >= 100) {
            switch (skillIndex) {
                case 100 -> executeContractAntiMagicBarrier(target);
                case 101 -> executeContractGraniteBlast(target);
                case 102 -> executeContractZoneTrack(target);
                case 103 -> executeContractSeerFlesh(target);
                case 104 -> executeContractMiracleOfGods(target);
                case 105 -> executeContractMultilayerBarrier(target);
                case 106 -> executeContractInfiniteDragonPrison(target);
                case 107 -> executeContractHellflare(target);
                case 108 -> executeContractDarkFlameShot(target);
                case 109 -> executeContractDeathBlessing(target);
                case 110 -> executeContractSupernova(target);
                case 111 -> executeContractDimensionalSanctuary(target);
                case 112 -> executeContractDragonNova(target);
            }
            this.triggerSkillCooldown(30);
            return;
        }

        DemonType type = getDemonType();
        if (skillIndex == 10) {
            executeSkillFourDomain(target, type);
            this.triggerSkillCooldown(40);
            return;
        }

        if (skillIndex >= 20) {
            int randomSkillIdx = skillIndex - 20;
            List<PrimordialSkillPool.SkillEntry> rSkills = getRandomSkills();
            if (randomSkillIdx >= 0 && randomSkillIdx < rSkills.size()) {
                if (target != null) {
                    PrimordialSkillPool.executeSkill(this, target, rSkills.get(randomSkillIdx));
                    this.triggerSkillCooldown(30);
                } else if (this.getOwner() instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.literal("§e[" + getEffectiveDemonName() + "] §7Chưa có mục tiêu thù địch trong bán kính 48m để thi triển chiêu này!"), true);
                }
                return;
            }
        }

        if (target == null) {
            if (skillIndex == 6 && this.level() instanceof ServerLevel sl) {
                Vec3 eyePos = this.getEyePosition(1.0F);
                Vec3 dir = this.getLookAngle();
                announceSkill(type.getColorName() + ": Linh Tử Bộc Phát (Spiritron Outburst)");
                float dmg = hasPhysicalBody() ? 560.0F : 400.0F;
                HorizontalHolyBeamAbility.fireSpiritronOutburst(sl, this, eyePos, dir, type, 56.0D, dmg);
                this.triggerSkillCooldown(30);
                return;
            }
            if (this.getOwner() instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("§e[" + getEffectiveDemonName() + "] §7Chưa có mục tiêu thù địch trong bán kính 48m!"), true);
            }
            return;
        }

        switch (skillIndex) {
            case 0 -> executeNormalAttack(target, type);
            case 1 -> executeSkillOne(target, type);
            case 2 -> executeSkillTwo(target, type);
            case 3 -> executeSkillThree(target, type);
            case 4 -> executeChromaticLightPillar(target, type);
            case 5 -> executeDisintegration(target, type);
            case 6 -> executeSpiritronOutburst(target, type);
            case 7 -> executeSignatureSkillOne(target, type);
            case 8 -> executeSignatureSkillTwo(target, type);
            case 9 -> executeSignatureSkillThree(target, type);
            case 10 -> executeSkillFourDomain(target, type);
            default -> executeSpiritronOutburst(target, type);
        }
        this.triggerSkillCooldown(30);
    }

    // =========================================================================
    // CÁC KỸ NĂNG NGƯỜI CHƠI TRAO QUYỀN CHO ÁC MA KHẾ ƯỚC (INDEX 100 -> 112)
    // =========================================================================
    public void executeContractAntiMagicBarrier(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 pos = this.position();
        announceSkill("Kháng Ma Kết Giới (Anti-Magic Barrier)");
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        AntiMagicBarrierManager.spawnDemonBarrier(sl, owner, pos, 60.0, 40.0);
        broadcastNearbyDialogue("Kháng Ma Kết Giới đã được dựng lên! Mọi ma pháp thù địch sẽ bị phong ấn triệt để!", 32.0D);
    }

    public void executeContractGraniteBlast(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 eyePos = this.getEyePosition(1.0F);
        Vec3 dir = target != null ? target.getEyePosition(1.0F).subtract(eyePos).normalize() : this.getLookAngle();
        announceSkill("Cú Bắn Granit (Granite Blast)");
        HorizontalHolyBeamAbility.fireGraniteBlast(sl, this, eyePos, dir, 56.0D, 1200.0F);
        Vec3 impact = eyePos.add(dir.scale(28.0D));
        dispelOpposingMagic(impact, 16.0D);
        if (this.getDemonType() == DemonType.JAUNE) {
            applyCarreraBlockDestruction(impact, 3.0F);
        }
    }

    public void executeContractZoneTrack(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 eyePos = this.getEyePosition(1.0F);
        Vec3 dir = target != null ? target.getEyePosition(1.0F).subtract(eyePos).normalize() : this.getLookAngle();
        announceSkill("Zone Track: Đại Pháo Bạch Kim");

        ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_ZONE_TRACK, this.getUUID(), eyePos, dir, 20, ClientboundCinematicSkillPacket.ACTION_START, 1.0f
        ));
        sl.playSound(null, eyePos.x, eyePos.y, eyePos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.7F);

        double maxDist = 48.0D;
        for (double d = 1.0; d <= maxDist; d += 1.5) {
            Vec3 cur = eyePos.add(dir.scale(d));
            AABB box = new AABB(cur, cur).inflate(2.5D);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && !isImmuneOrFriendly(e));
            for (LivingEntity v : victims) {
                dealDemonicDamage(v, 360.0F);
                v.setDeltaMovement(dir.scale(1.8D).add(0, 0.4D, 0));
                v.hurtMarked = true;
            }
        }
        Vec3 finalHit = eyePos.add(dir.scale(Math.min(30.0, maxDist)));
        dispelOpposingMagic(finalHit, 14.0D);
        if (this.getDemonType() == DemonType.JAUNE) {
            applyCarreraBlockDestruction(finalHit, 3.0F);
        }
    }

    public void executeContractSeerFlesh(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 pos = this.position();
        announceSkill("Thị Thực: Thị Nhục Mạch Động");

        this.heal(this.getMaxHealth() * 0.5F);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 4.0F, 0.8F);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.6F);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 1.0, pos.z, 2, 0, 0, 0, 0);

        AABB box = new AABB(pos, pos).inflate(18.0D);
        List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && !isImmuneOrFriendly(e));
        for (LivingEntity e : enemies) {
            dealDemonicDamage(e, 260.0F);
            Vec3 knock = e.position().subtract(pos).normalize().scale(2.0D).add(0, 0.5D, 0);
            e.setDeltaMovement(knock);
            e.hurtMarked = true;
        }
        dispelOpposingMagic(pos, 16.0D);
    }

    public void executeContractMiracleOfGods(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 targetPos = target != null ? target.position() : this.position();
        Vec3 groundVec = PurificationPillarAbility.findGroundBelow(sl, targetPos);
        announceSkill("Phép Màu Của Thần Linh: Đại Thánh Tẩy");
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        PurificationPillarAbility.spawnTemporaryPurification(sl, groundVec, 240, owner);
        dispelOpposingMagic(groundVec, 20.0D);
    }

    public void executeContractMultilayerBarrier(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 pos = this.position().add(0, 1.0D, 0);
        announceSkill("Đa Trùng Kết Giới (Multilayer Barrier)");
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        MultilayerBarrierAbility.spawnDemonBarrier(sl, owner, this.getUUID(), pos, 6.0, 600);
    }

    public void executeContractInfiniteDragonPrison(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 targetPos = target != null ? target.position().add(0, 1.0, 0) : this.position().add(this.getLookAngle().scale(8.0)).add(0, 1.0, 0);
        announceSkill("Lồng Giam Vô Hạn (Infinite Dragon Prison)");
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        InfiniteDragonPrisonAbility.spawnDemonPrison(sl, owner, this.getUUID(), targetPos, 8.0, 600);
    }

    public void executeContractHellflare(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 eyePos = this.getEyePosition(1.0F);
        Vec3 targetPos = target != null ? target.position().add(0, target.getBbHeight() * 0.5D, 0) : eyePos.add(this.getLookAngle().scale(16.0D));
        announceSkill("Hắc Viêm Ngục (Hellflare)");
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        HellflareAbility.castFromDemon(sl, this, eyePos, targetPos, owner);
        dispelOpposingMagic(targetPos, 16.0D);
        if (this.getDemonType() == DemonType.JAUNE) {
            applyCarreraBlockDestruction(targetPos, 3.2F);
        }
    }

    public void executeContractDarkFlameShot(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 eyePos = this.getEyePosition(1.0F);
        Vec3 targetPos = target != null ? target.position().add(0, target.getBbHeight() * 0.5D, 0) : eyePos.add(this.getLookAngle().scale(24.0D));
        announceSkill("Hắc Viêm Hỏa Cầu (Dark Flame Shot)");
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        DarkFlameShotAbility.castFromDemon(sl, this, eyePos, targetPos, owner);
        dispelOpposingMagic(targetPos, 16.0D);
        if (this.getDemonType() == DemonType.JAUNE) {
            applyCarreraBlockDestruction(targetPos, 3.2F);
        }
    }

    public void executeContractDeathBlessing(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        NihilityCollapseAbility.castFromDemon(sl, this, owner);
    }

    public void executeContractSupernova(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 forward = this.getLookAngle();
        Vec3 corePos = this.position().add(forward.scale(16.0)).add(0, 15.0, 0);
        announceSkill("Siêu Tân Tinh (Supernova)");
        broadcastNearbyDialogue("Hãy chiêm ngưỡng ánh sáng của sự hủy diệt nguyên sơ!", 48.0D);
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;

        SupernovaAbility.spawnTemporarySupernova(sl, corePos, 240, owner, this);
        dispelOpposingMagic(corePos, 32.0D);
    }

    public void executeContractDimensionalSanctuary(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        if (owner == null) return;
        announceSkill("Dị Giới Thần Môn: Cánh Cửa Về Căn Cứ");
        DimensionalSanctuaryAbility.spawnOuterGate(sl, owner, 0);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 3.0F, 1.0F);
        broadcastNearbyDialogue("Ta đã mở Dị Giới Thần Môn kết nối thẳng về nhà! Hãy bước qua cánh cửa!", 32.0D);
    }

    public void executeContractDragonNova(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        DragonNovaAbility.castFromDemon(sl, this, owner);
    }

    // =========================================================================
    // TÊN GỌI KỸ NĂNG CỦA 7 THỦY TỔ
    // =========================================================================
    public static String getSkillOneName(DemonType type) {
        return switch (type) {
            case NOIR -> "Pháp Trận Hắc Ma Cầu Thôn Phệ";
            case ROUGE -> "Ma Trận Xích Diễm Bộc Phá";
            case BLANC -> "Pháp Trận Bạch Viêm Khởi Nguyên";
            case JAUNE -> "Hoàng Kim Trọng Lực Ma Trận";
            case VIOLET -> "Pháp Trận Tử Độc Khởi Nguyên";
            case BLEU -> "Pháp Trận Băng Tiễn Cực Hàn";
            case VERT -> "Pháp Trận Phong Ma Tiễn";
        };
    }

    public static String getSkillTwoName(DemonType type) {
        return switch (type) {
            case NOIR -> "Vực Sâu Tận Cùng • Đoạt Mệnh Hắc Dực";
            case ROUGE -> "Hỏa Long Bão Tố Tận Diệt";
            case BLANC -> "Dải Lụa Thánh Ma Tử Thần";
            case JAUNE -> "Tuyệt Diệt Ma Pháo • Carrera Bullet Storm";
            case VIOLET -> "Vũ Điệu Bóng Ma Ăn Mòn";
            case BLEU -> "Lốc Xoáy Băng Tuyết Cực Hàn";
            case VERT -> "Bão Lốc Đẩy Lùi & Bảo Hộ";
        };
    }

    public static String getSkillThreeName(DemonType type) {
        return switch (type) {
            case NOIR -> "Song Trùng Trận • Cột Sáng Hủy Diệt";
            case ROUGE -> "Thiên Địa Ma Trận • Cột Hỏa Ngục";
            case BLANC -> "Thánh Ma Đồng Quy • Cột Sáng Phân Rã";
            case JAUNE -> "Tam Trùng Trận • Đại Ma Pháp Hạt Nhân";
            case VIOLET -> "Tam Trùng Ma Trận • Cột Sáng Ăn Mòn";
            case BLEU -> "Thiên Địa Hàn Băng • Cột Sáng Băng Cực";
            case VERT -> "Song Trùng Phong Lôi • Cột Sáng Lục Bảo";
        };
    }

    public static String getSignatureOneName(DemonType type) {
        return switch (type) {
            case ROUGE -> "Xích Diễm Hỏa Long Bộc Phá";
            case NOIR -> "Hắc Ám Không Gian Phân Cắt";
            case BLANC -> "Bạch Quang Tuyệt Diệt Tuyến";
            case JAUNE -> "Carrera Gatling Barrage";
            case VIOLET -> "Tử Hoa Mạn Đà La";
            case BLEU -> "Băng Cực Phong Ấn Vĩnh Cửu";
            case VERT -> "Lục Bảo Phong Bạo Cuồng Nộ";
        };
    }

    public static String getSignatureTwoName(DemonType type) {
        return switch (type) {
            case ROUGE -> "Hỏa Ngục Toái Phiến Tiễn";
            case NOIR -> "Phán Quyết Tuyệt Vọng";
            case BLANC -> "Bạch Viêm Huyết Vũ";
            case JAUNE -> "Trọng Lực Thâm Uyên Điểm Kỳ Dị";
            case VIOLET -> "Huyết Độc Ma Trảo Tam Kích";
            case BLEU -> "Bão Băng Ngưng Đọng";
            case VERT -> "Lục Bảo Thánh Thuẫn";
        };
    }

    public static String getSignatureThreeName(DemonType type) {
        return switch (type) {
            case ROUGE -> "Hỏa Đế Trảm Không";
            case NOIR -> "Hắc Linh Hồn Thôn Phệ";
            case BLANC -> "Thánh Ma Phân Rã Ba Động";
            case JAUNE -> "Hạch Tâm Quá Tải";
            case VIOLET -> "Tử Khí Trầm Luân";
            case BLEU -> "Băng Kính Khởi Nguyên";
            case VERT -> "Lục Lôi Ma Trận";
        };
    }

    public static String getDomainName(DemonType type) {
        return switch (type) {
            case NOIR -> "Thế Giới Cám Dỗ (Temptation World)";
            case ROUGE -> "Hỏa Ngục Ma Vương (Crimson Hell)";
            case BLANC -> "Bạch Viêm Tận Diệt (Absolute White)";
            case JAUNE -> "Hạch Tâm Thâm Uyên (Abyss Gravity & Nuclear Domain)";
            case VIOLET -> "Vườn Độc Tử Thần (Poison Blossom)";
            case BLEU -> "Hàn Băng Vĩnh Cửu (Absolute Zero)";
            case VERT -> "Bảo Hộ Lục Bảo & Bão Tố (Emerald Sanctuary)";
        };
    }

    public static String getDomainVoiceLine(DemonType type) {
        return switch (type) {
            case NOIR -> "Kufufufu... Chào mừng đến với Thế Giới Cám Dỗ của tôi. Tại nơi này, chân lý và sinh mệnh của các người... hoàn toàn thuộc về tôi!";
            case ROUGE -> "Hmph! Hãy nếm thử sức nóng thiêu rụi cả linh hồn trong Hỏa Ngục của ta!";
            case BLANC -> "Ara ara... Vùng đất trắng này sẽ là nấm mồ thanh nhã dành riêng cho các ngươi~";
            case JAUNE -> "Haha! Hãy thử chống cự lại đại ma pháp hạt nhân và trọng lực thâm uyên trong Lãnh Địa của ta xem nào!";
            case VIOLET -> "Hihihi~ Chào mừng đến với khu vườn đầy hoa độc của ta! Đừng chết sớm quá nhé~";
            case BLEU -> "Lạnh lắm đấy... Tốt nhất các người nên ngủ mãi mãi trong băng tuyết đi.";
            case VERT -> "Trong kết giới này, mọi hỗn loạn sẽ bị dẹp tan dưới trật tự tuyệt đối.";
        };
    }

    public static String getSkillDisplayName(DemonType type, int skillIndex) {
        return switch (skillIndex) {
            case 0 -> "Đòn Đánh Thường";
            case 1 -> getSkillOneName(type);
            case 2 -> getSkillTwoName(type);
            case 3 -> getSkillThreeName(type);
            case 4 -> "Thất Sắc Thần Trụ • Cột Sáng Cực Đại";
            case 5 -> "Linh Tử Băng Hoại (Disintegration)";
            case 6 -> getSignatureOneName(type);
            case 7 -> getSignatureTwoName(type);
            case 8 -> getSignatureThreeName(type);
            case 9 -> "Lãnh Địa: " + getDomainName(type);
            default -> "Kỹ Năng Ma Giới";
        };
    }

    // =========================================================================
    // AI CHIẾN ĐẤU THÔNG MINH & TỰ DO: LINH HOẠT THI TRIỂN TOÀN BỘ KỸ NĂNG
    // =========================================================================
    static class PrimordialSkillGoal extends Goal {
        private final PrimordialDemonEntity demon;
        private int warmUpTicks = 0;

        public PrimordialSkillGoal(PrimordialDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.demon.getTarget();
            if (target == null || !target.isAlive() || this.demon.isImmuneOrFriendly(target)) return false;
            return demon.canCastSkill() && demon.distanceToSqr(target) <= 48.0D * 48.0D;
        }

        @Override
        public void start() {
            this.warmUpTicks = 4; // Nhịp độ nhanh, phản xạ thần tốc (0.2s)
        }

        @Override
        public void tick() {
            LivingEntity target = this.demon.getTarget();
            if (target == null || !target.isAlive() || !this.demon.canCastSkill() || this.demon.isImmuneOrFriendly(target)) return;

            this.demon.getLookControl().setLookAt(target, 40.0F, 40.0F);
            this.warmUpTicks--;

            if (this.warmUpTicks <= 0) {
                executeDynamicSkill(target);
                // Thời gian hồi chiêu linh hoạt 20-35 ticks (1.0s - 1.75s) giúp ác ma ra đòn dồn dập
                this.demon.triggerSkillCooldown(20 + this.demon.getRandom().nextInt(15));
            }
        }

        private void executeDynamicSkill(LivingEntity target) {
            DemonType type = this.demon.getDemonType();
            double dist = this.demon.distanceTo(target);
            boolean isBoss = PrimordialDemonEntity.isBossTarget(target);
            var random = this.demon.getRandom();

            // 1. Kích hoạt Lãnh Địa (Domain Expansion):
            // Khi chưa bật lãnh địa và gặp Boss, hoặc gặp kẻ địch nguy hiểm, hoặc 15% ngẫu nhiên
            if (this.demon.activeDomainTicks <= 0 && (isBoss || (dist <= 16.0D && random.nextFloat() < 0.15F))) {
                demon.executeSkillFourDomain(target, type);
                return;
            }

            // 1.5. ÁC MA ĐÃ KÝ KHẾ ƯỚC: Toàn quyền tự động dùng các Đại Ma Pháp Khế Ước khi chiến đấu
            // (NGOẠI TRỪ Lời Chúc Tử Vong [109] bắt buộc phải do người chơi ra lệnh qua Khế Ước!)
            if (this.demon.isTame() && random.nextFloat() < 0.35F) {
                if (this.demon.getHealth() < this.demon.getMaxHealth() * 0.6F && random.nextFloat() < 0.5F) {
                    demon.executeContractSeerFlesh(target);
                    return;
                }
                int[] autonomousContractSkills = isBoss
                        ? new int[]{101, 102, 104, 106, 107, 108, 110, 112}
                        : new int[]{101, 102, 103, 104, 105, 106, 107, 108, 110, 112};
                int chosenContract = autonomousContractSkills[random.nextInt(autonomousContractSkills.length)];
                switch (chosenContract) {
                    case 101 -> demon.executeContractGraniteBlast(target);
                    case 102 -> demon.executeContractZoneTrack(target);
                    case 103 -> demon.executeContractSeerFlesh(target);
                    case 104 -> demon.executeContractMiracleOfGods(target);
                    case 105 -> demon.executeContractMultilayerBarrier(target);
                    case 106 -> demon.executeContractInfiniteDragonPrison(target);
                    case 107 -> demon.executeContractHellflare(target);
                    case 108 -> demon.executeContractDarkFlameShot(target);
                    case 110 -> demon.executeContractSupernova(target);
                    case 112 -> demon.executeContractDragonNova(target);
                }
                return;
            }

            // 2. Tự do thi triển Kỹ Năng Ngẫu Nhiên Thức Tỉnh (PrimordialSkillPool):
            // Tỷ lệ 45% nếu đã có kỹ năng mở khóa
            List<PrimordialSkillPool.SkillEntry> rSkills = this.demon.getRandomSkills();
            if (!rSkills.isEmpty() && random.nextFloat() < 0.45F) {
                // Ưu tiên hồi phục nếu ác ma hoặc chủ nhân máu dưới 50%
                boolean needHeal = this.demon.getHealth() < this.demon.getMaxHealth() * 0.5F;
                if (needHeal) {
                    for (var sk : rSkills) {
                        if (sk.id() == PrimordialSkillPool.SKILL_SOUL_DRAIN ||
                            sk.id() == PrimordialSkillPool.SKILL_SEER_FLESH_PULSE ||
                            sk.id() == PrimordialSkillPool.SKILL_BEELZEBUTH) {
                            PrimordialSkillPool.executeSkill(this.demon, target, sk);
                            return;
                        }
                    }
                }
                PrimordialSkillPool.SkillEntry chosen = rSkills.get(random.nextInt(rSkills.size()));
                PrimordialSkillPool.executeSkill(this.demon, target, chosen);
                return;
            }

            // 3. Phân nhánh chiến thuật theo Tình huống & Khoảng cách:
            if (isBoss) {
                // ƯU TIÊN SÁT THƯƠNG DIỆT BOSS
                float bossRoll = random.nextFloat();
                if (bossRoll < 0.25F) {
                    demon.executeSpiritronOutburst(target, type); // Linh tử bộc phát (Granite Blast uy lực cực đại)
                } else if (bossRoll < 0.45F) {
                    demon.executeDisintegration(target, type); // Linh tử băng hoại
                } else if (bossRoll < 0.65F) {
                    demon.executeChromaticLightPillar(target, type); // Thất sắc thần trụ
                } else if (bossRoll < 0.82F) {
                    demon.executeSignatureSkillThree(target, type); // Chiêu độc bản 3
                } else if (bossRoll < 0.92F) {
                    demon.executeSkillThree(target, type); // Song trùng / Tam trùng ma trận
                } else {
                    demon.executeSignatureSkillOne(target, type); // Chiêu độc bản 1
                }
                return;
            }

            // Đối đầu mục tiêu thường:
            if (dist > 12.0D) {
                // TẦM XA: Tung ma pháo, pháo Linh Tử Bộc Phát, tốc biến áp sát hoặc chùm tia
                float farRoll = random.nextFloat();
                if (farRoll < 0.28F) {
                    demon.executeSpiritronOutburst(target, type); // Khai hỏa đại pháo Linh Tử Bộc Phát xuyên tầm xa
                } else if (farRoll < 0.48F) {
                    demon.executeSkillOne(target, type); // Pháp trận ma pháo tầm xa
                } else if (farRoll < 0.68F) {
                    demon.executeSignatureSkillOne(target, type); // Chiêu độc bản 1 tầm xa
                } else if (farRoll < 0.85F) {
                    demon.executeSkillTwo(target, type); // Tốc biến / lướt áp sát mục tiêu
                } else {
                    demon.executeChromaticLightPillar(target, type); // Cột sáng 60m
                }
            } else if (dist <= 4.5D) {
                // TẦM CẬN CHIẾN: Cào cấu, bộc phá cận thân, chém xoay
                float meleeRoll = random.nextFloat();
                if (meleeRoll < 0.25F) {
                    demon.executeNormalAttack(target, type); // Đòn móng vuốt / chém
                } else if (meleeRoll < 0.50F) {
                    demon.executeSignatureSkillTwo(target, type); // Chiêu độc bản 2
                } else if (meleeRoll < 0.75F) {
                    demon.executeSignatureSkillThree(target, type); // Chiêu độc bản 3
                } else if (meleeRoll < 0.90F) {
                    demon.executeSkillTwo(target, type); // Tốc biến ra sau lưng / lốc xoáy
                } else {
                    demon.executeSkillOne(target, type); // Bộc phá cận cảnh
                }
            } else {
                // TẦM TRUNG (4.5m - 12m): Phối hợp toàn diện ngẫu nhiên
                int choice = random.nextInt(10);
                switch (choice) {
                    case 0 -> demon.executeSkillOne(target, type);
                    case 1 -> demon.executeSignatureSkillOne(target, type);
                    case 2 -> demon.executeSkillTwo(target, type);
                    case 3 -> demon.executeSignatureSkillTwo(target, type);
                    case 4 -> demon.executeSkillThree(target, type);
                    case 5 -> demon.executeSpiritronOutburst(target, type); // Linh tử bộc phát
                    case 6 -> demon.executeChromaticLightPillar(target, type);
                    case 7 -> demon.executeSignatureSkillThree(target, type);
                    case 8 -> demon.executeDisintegration(target, type);
                    default -> demon.executeNormalAttack(target, type);
                }
            }
        }
    }

    // =========================================================================
    // AI ƯU TIÊN PHÁ HỦY LỒNG TRIỆU HỒI (SPAWNER) CHO ÁC MA ĐÃ KÝ KHẾ ƯỚC
    // =========================================================================
    static class PrimordialBreakSpawnerGoal extends Goal {
        private final PrimordialDemonEntity demon;
        private BlockPos targetSpawner = null;
        private int scanCooldown = 0;
        private int approachTicks = 0;

        public PrimordialBreakSpawnerGoal(PrimordialDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.demon.isTame() || this.demon.isRising()) return false;
            if (this.targetSpawner != null && this.demon.level().getBlockState(this.targetSpawner).is(Blocks.SPAWNER)) {
                return true;
            }
            if (this.scanCooldown > 0) {
                this.scanCooldown--;
                return false;
            }
            this.scanCooldown = 15;
            this.targetSpawner = findNearestSpawner(32, 12);
            return this.targetSpawner != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.demon.isTame()
                    && this.targetSpawner != null
                    && this.demon.level().getBlockState(this.targetSpawner).is(Blocks.SPAWNER)
                    && this.demon.blockPosition().distSqr(this.targetSpawner) <= 40.0D * 40.0D;
        }

        @Override
        public void start() {
            this.approachTicks = 0;
            if (this.targetSpawner != null) {
                this.demon.getNavigation().moveTo(this.targetSpawner.getX() + 0.5D, this.targetSpawner.getY() + 0.5D, this.targetSpawner.getZ() + 0.5D, 1.5D);
            }
        }

        @Override
        public void stop() {
            this.targetSpawner = null;
            this.approachTicks = 0;
        }

        @Override
        public void tick() {
            if (this.targetSpawner == null || !(this.demon.level() instanceof ServerLevel sl)) return;
            if (!sl.getBlockState(this.targetSpawner).is(Blocks.SPAWNER)) {
                this.targetSpawner = null;
                return;
            }

            Vec3 center = Vec3.atCenterOf(this.targetSpawner);
            this.demon.getLookControl().setLookAt(center.x, center.y, center.z, 35.0F, 35.0F);
            this.approachTicks++;

            double distSqr = this.demon.distanceToSqr(center);
            if (distSqr > 5.0D * 5.0D) {
                if (this.approachTicks >= 25) {
                    // Tốc biến áp sát thẳng tới lồng spawn nếu bị vướng địa hình
                    this.demon.teleportTo(center.x, center.y + 1.0D, center.z);
                    sl.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 1.0D, center.z, 20, 0.4, 0.5, 0.4, 0.05);
                    sl.playSound(null, center.x, center.y, center.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.2F);
                } else if (this.approachTicks % 10 == 1) {
                    this.demon.getNavigation().moveTo(center.x, center.y, center.z, 1.5D);
                }
            }

            if (this.demon.distanceToSqr(center) <= 6.0D * 6.0D || this.approachTicks >= 30) {
                this.demon.swing(InteractionHand.MAIN_HAND);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 25, 0.5, 0.5, 0.5, 0.08);
                sl.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.1F);
                sl.destroyBlock(this.targetSpawner, true, this.demon);

                if (this.demon.getOwner() instanceof ServerPlayer owner) {
                    owner.displayClientMessage(Component.literal("§d§l[" + this.demon.getEffectiveDemonName() + "] §a✔ Đã ưu tiên phá hủy Lồng Triệu Hồi (Spawner) tại [" + this.targetSpawner.toShortString() + "]!"), true);
                }
                // Tìm ngay lồng spawn kế tiếp nếu còn xung quanh
                this.targetSpawner = findNearestSpawner(32, 12);
                this.approachTicks = 0;
            }
        }

        private BlockPos findNearestSpawner(int radiusXZ, int radiusY) {
            BlockPos origin = this.demon.blockPosition();
            Level level = this.demon.level();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;

            for (BlockPos p : BlockPos.betweenClosed(
                    origin.offset(-radiusXZ, -radiusY, -radiusXZ),
                    origin.offset(radiusXZ, radiusY, radiusXZ))) {
                if (level.getBlockState(p).is(Blocks.SPAWNER)) {
                    double d = origin.distSqr(p);
                    if (d < bestDist) {
                        bestDist = d;
                        best = p.immutable();
                    }
                }
            }
            return best;
        }
    }

    // =========================================================================
    // AI TÀN SÁT THÔN LÀNG DÀNH CHO BẠCH SẮC BLANC (TESTAROSSA)
    // =========================================================================
    static class BlancMassacreVillageGoal extends Goal {
        private final PrimordialDemonEntity demon;
        private LivingEntity currentVictim = null;
        private int emptyTicks = 0;

        public BlancMassacreVillageGoal(PrimordialDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.demon.isTame() && this.demon.isMassacreMode() && !this.demon.isRising();
        }

        @Override
        public boolean canContinueToUse() {
            return this.demon.isTame() && this.demon.isMassacreMode() && !this.demon.isRising();
        }

        @Override
        public void start() {
            this.emptyTicks = 0;
            this.currentVictim = findNextVictim();
            if (this.currentVictim != null) {
                this.demon.setTarget(this.currentVictim);
            }
        }

        @Override
        public void tick() {
            if (!(this.demon.level() instanceof ServerLevel sl)) return;

            // Hút toàn bộ vật phẩm & linh hồn rơi ra xung quanh về túi đồ chủ nhân
            if (this.demon.tickCount % 10 == 0 && this.demon.getOwner() instanceof ServerPlayer owner) {
                List<ItemEntity> drops = sl.getEntitiesOfClass(ItemEntity.class, this.demon.getBoundingBox().inflate(24.0D), Entity::isAlive);
                for (ItemEntity itemEntity : drops) {
                    ItemStack st = itemEntity.getItem().copy();
                    if (owner.getInventory().add(st)) {
                        itemEntity.discard();
                    }
                }
            }

            if (this.currentVictim == null || !this.currentVictim.isAlive()) {
                this.currentVictim = findNextVictim();
                if (this.currentVictim != null) {
                    this.emptyTicks = 0;
                    this.demon.setTarget(this.currentVictim);
                } else {
                    this.emptyTicks++;
                    if (this.emptyTicks >= 60) {
                        this.demon.setMassacreMode(false);
                        this.demon.setTarget(null);
                        if (this.demon.getOwner() instanceof ServerPlayer owner) {
                            owner.sendSystemMessage(Component.literal("§f§l[BLANC - TESTAROSSA] §c\"Ara ara~ Toàn bộ sinh mạng trong ngôi làng này đã bị xóa sổ sạch sẽ theo ý chủ nhân.\""));
                            owner.displayClientMessage(Component.literal("§4§l[TÀN SÁT HOÀN TẤT] §fBlanc đã tiêu diệt toàn bộ Dân Làng & Iron Golem trong phạm vi 100m!"), true);
                        }
                    }
                    return;
                }
            }

            this.demon.setTarget(this.currentVictim);
            this.demon.getLookControl().setLookAt(this.currentVictim, 40.0F, 40.0F);
            double dist = this.demon.distanceTo(this.currentVictim);

            if (dist > 18.0D || (dist > 5.0D && this.demon.tickCount % 25 == 0)) {
                Vec3 vPos = this.currentVictim.position();
                this.demon.teleportTo(vPos.x + (this.demon.getRandom().nextDouble() - 0.5D) * 2.5D, vPos.y + 0.5D, vPos.z + (this.demon.getRandom().nextDouble() - 0.5D) * 2.5D);
                sl.sendParticles(ParticleTypes.END_ROD, this.demon.getX(), this.demon.getY() + 1.0D, this.demon.getZ(), 15, 0.3D, 0.5D, 0.3D, 0.05D);
                sl.playSound(null, this.demon.getX(), this.demon.getY(), this.demon.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.4F);
            } else {
                this.demon.getNavigation().moveTo(this.currentVictim, 1.6D);
            }

            if (dist <= 4.5D && this.demon.tickCount % 10 == 0) {
                this.demon.executeNormalAttack(this.currentVictim, this.demon.getDemonType());
            } else if (this.demon.canCastSkill()) {
                int roll = this.demon.getRandom().nextInt(5);
                switch (roll) {
                    case 0 -> this.demon.executeSkillOne(this.currentVictim, this.demon.getDemonType());
                    case 1 -> this.demon.executeSignatureSkillOne(this.currentVictim, this.demon.getDemonType());
                    case 2 -> this.demon.executeSignatureSkillTwo(this.currentVictim, this.demon.getDemonType());
                    case 3 -> this.demon.executeDisintegration(this.currentVictim, this.demon.getDemonType());
                    default -> this.demon.executeSpiritronOutburst(this.currentVictim, this.demon.getDemonType());
                }
                this.demon.triggerSkillCooldown(18);
            }
        }

        private LivingEntity findNextVictim() {
            List<LivingEntity> victims = this.demon.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.demon.getBoundingBox().inflate(100.0D, 40.0D, 100.0D),
                    e -> e.isAlive() && (e instanceof net.minecraft.world.entity.npc.AbstractVillager
                            || e instanceof net.minecraft.world.entity.animal.IronGolem
                            || e instanceof net.minecraft.world.entity.animal.SnowGolem)
            );
            if (victims.isEmpty()) return null;
            // Ưu tiên giết Iron Golem bảo vệ làng trước hoặc mục tiêu gần nhất
            victims.sort((a, b) -> {
                boolean aGolem = a instanceof net.minecraft.world.entity.animal.IronGolem;
                boolean bGolem = b instanceof net.minecraft.world.entity.animal.IronGolem;
                if (aGolem != bGolem) return aGolem ? -1 : 1;
                return Double.compare(this.demon.distanceToSqr(a), this.demon.distanceToSqr(b));
            });
            return victims.get(0);
        }
    }

    // =========================================================================
    // AI TUẦN TRA CANH GIỮ VÙNG RỘNG (64-120M) KHÔNG ĐỨNG IM MỘT CHỖ
    // =========================================================================
    static class PrimordialPatrolGuardGoal extends Goal {
        private final PrimordialDemonEntity demon;
        private int patrolCooldown = 0;
        private Vec3 currentWaypoint = null;

        public PrimordialPatrolGuardGoal(PrimordialDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.demon.isTame() || !this.demon.isOrderedToSit() || this.demon.isMassacreMode()) {
                return false;
            }
            if (this.demon.guardCenterPos == null) {
                this.demon.guardCenterPos = this.demon.blockPosition();
                this.demon.setPersistenceRequired();
            }
            // Nếu đang giao chiến với mục tiêu trong bán kính 120m quanh tâm canh giữ, nhường quyền di chuyển cho AI chiến đấu
            LivingEntity target = this.demon.getTarget();
            if (target != null && target.isAlive()) {
                double distFromGuard = target.distanceToSqr(Vec3.atCenterOf(this.demon.guardCenterPos));
                if (distFromGuard <= 120.0D * 120.0D) {
                    return false;
                } else {
                    this.demon.setTarget(null);
                }
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.patrolCooldown = 10;
        }

        @Override
        public void tick() {
            if (this.demon.guardCenterPos == null || !(this.demon.level() instanceof ServerLevel sl)) return;
            Vec3 center = Vec3.atCenterOf(this.demon.guardCenterPos);

            // 1. Quét tìm quái vật xâm nhập trong phạm vi 96m quanh tâm canh giữ
            if (this.demon.tickCount % 20 == 0) {
                List<LivingEntity> intruders = sl.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(center, center).inflate(96.0D, 36.0D, 96.0D),
                        e -> e != this.demon && e.isAlive() && e instanceof Enemy && !this.demon.isImmuneOrFriendly(e)
                );
                if (!intruders.isEmpty()) {
                    intruders.sort(java.util.Comparator.comparingDouble(this.demon::distanceToSqr));
                    this.demon.setTarget(intruders.get(0));
                    return;
                }
            }

            // 2. Nếu đi quá xa tâm canh giữ (> 110m), quay về tâm
            double distFromCenterSqr = this.demon.distanceToSqr(center);
            if (distFromCenterSqr > 130.0D * 130.0D) {
                this.demon.teleportTo(center.x, center.y + 1.0D, center.z);
                return;
            } else if (distFromCenterSqr > 96.0D * 96.0D) {
                this.demon.getNavigation().moveTo(center.x, center.y, center.z, 1.35D);
                return;
            }

            // 3. Đi tuần tra tự nhiên quanh khu vực canh giữ (bán kính rộng tới 72m)
            if (this.patrolCooldown > 0) {
                this.patrolCooldown--;
            }
            if (this.patrolCooldown <= 0 || (this.currentWaypoint != null && this.demon.distanceToSqr(this.currentWaypoint) <= 6.0D)) {
                double angle = this.demon.getRandom().nextDouble() * Math.PI * 2.0D;
                double radius = 12.0D + this.demon.getRandom().nextDouble() * 60.0D; // Tuần tra rộng 12m - 72m
                int wx = this.demon.guardCenterPos.getX() + (int) (Math.cos(angle) * radius);
                int wz = this.demon.guardCenterPos.getZ() + (int) (Math.sin(angle) * radius);
                int wy = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                if (wy < sl.getMinBuildHeight() + 5) {
                    wy = this.demon.guardCenterPos.getY();
                }
                this.currentWaypoint = new Vec3(wx + 0.5D, wy + 0.5D, wz + 0.5D);
                this.demon.getNavigation().moveTo(this.currentWaypoint.x, this.currentWaypoint.y, this.currentWaypoint.z, 1.1D);
                this.patrolCooldown = 50 + this.demon.getRandom().nextInt(60);
            }
        }
    }

    // =========================================================================
    // AI ĐỜI SỐNG THÔN LÀNG & DỊCH VỤ THU PHÍ CỦA ÁC MA ĐÃ KÝ KHẾ ƯỚC
    // (Trò chuyện với dân làng, hồi máu Iron Golem, làm ruộng, lấp đất sửa đường -> Dân làng trả phí)
    // =========================================================================
    static class PrimordialVillageServiceGoal extends Goal {
        private final PrimordialDemonEntity demon;
        private int serviceCooldown = 80;

        public PrimordialVillageServiceGoal(PrimordialDemonEntity demon) {
            this.demon = demon;
        }

        @Override
        public boolean canUse() {
            if (!this.demon.isTame() || this.demon.isMassacreMode() || this.demon.isRising()) return false;
            if (this.demon.getTarget() != null && this.demon.getTarget().isAlive()) return false;
            if (this.serviceCooldown > 0) {
                this.serviceCooldown--;
                return false;
            }
            List<net.minecraft.world.entity.npc.Villager> villagers = this.demon.level().getEntitiesOfClass(
                    net.minecraft.world.entity.npc.Villager.class,
                    this.demon.getBoundingBox().inflate(48.0D, 20.0D, 48.0D),
                    Entity::isAlive
            );
            if (villagers.isEmpty()) {
                this.serviceCooldown = 100;
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            this.serviceCooldown = 120 + this.demon.getRandom().nextInt(80); // Mỗi 6-10 giây thực hiện 1 dịch vụ
            if (!(this.demon.level() instanceof ServerLevel sl)) return;

            List<net.minecraft.world.entity.npc.Villager> villagers = sl.getEntitiesOfClass(
                    net.minecraft.world.entity.npc.Villager.class,
                    this.demon.getBoundingBox().inflate(48.0D, 20.0D, 48.0D),
                    Entity::isAlive
            );
            if (villagers.isEmpty()) return;

            // 1. Ưu tiên Hồi máu & Cường hóa cho Người Sắt (Iron Golem) bị thương
            List<net.minecraft.world.entity.animal.IronGolem> golems = sl.getEntitiesOfClass(
                    net.minecraft.world.entity.animal.IronGolem.class,
                    this.demon.getBoundingBox().inflate(40.0D, 16.0D, 40.0D),
                    g -> g.isAlive() && g.getHealth() < g.getMaxHealth()
            );
            if (!golems.isEmpty()) {
                net.minecraft.world.entity.animal.IronGolem golem = golems.get(0);
                this.demon.getNavigation().moveTo(golem, 1.25D);
                this.demon.getLookControl().setLookAt(golem, 30.0F, 30.0F);
                golem.heal(golem.getMaxHealth());
                golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
                golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1));
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, golem.getX(), golem.getY() + 1.4D, golem.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.05D);
                sl.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 2.0F, 1.1F);
                payVillageFee(sl, villagers.get(0), "đã chữa trị & cường hóa Người Sắt bảo vệ làng", 3);
                return;
            }

            // 2. Làm ruộng: Thu hoạch lúa/hoa màu chín và gieo hạt trồng lại
            BlockPos cropFound = null;
            int harvestedCount = 0;
            BlockPos origin = this.demon.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-20, -4, -20), origin.offset(20, 4, 20))) {
                net.minecraft.world.level.block.state.BlockState state = sl.getBlockState(p);
                if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop.isMaxAge(state)) {
                    if (cropFound == null) cropFound = p.immutable();
                    List<ItemStack> drops = Block.getDrops(state, sl, p, null, this.demon, ItemStack.EMPTY);
                    sl.setBlock(p, crop.getStateForAge(0), 3);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 5, 0.2D, 0.2D, 0.2D, 0.02D);
                    if (this.demon.getOwner() instanceof ServerPlayer owner) {
                        for (ItemStack drop : drops) {
                            if (!owner.getInventory().add(drop.copy())) {
                                owner.drop(drop.copy(), false);
                            }
                        }
                    }
                    harvestedCount++;
                    if (harvestedCount >= 6) break;
                }
            }
            if (harvestedCount > 0 && cropFound != null) {
                this.demon.getNavigation().moveTo(cropFound.getX() + 0.5D, cropFound.getY(), cropFound.getZ() + 0.5D, 1.2D);
                sl.playSound(null, cropFound.getX(), cropFound.getY(), cropFound.getZ(), SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.5F, 1.1F);
                payVillageFee(sl, villagers.get(0), "đã thu hoạch & gieo trồng lại " + harvestedCount + " ô ruộng", 2);
                return;
            }

            // 3. Lấp đất & Sửa chữa hố sâu / địa hình hư hại quanh làng
            int filledCount = 0;
            BlockPos fillTarget = null;
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-18, -4, -18), origin.offset(18, 2, 18))) {
                net.minecraft.world.level.block.state.BlockState st = sl.getBlockState(p);
                if (st.isAir() && sl.getBlockState(p.below()).isSolidRender(sl, p.below())) {
                    int solidNeighbors = 0;
                    boolean hasGrassOrDirt = false;
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        net.minecraft.world.level.block.state.BlockState nb = sl.getBlockState(p.relative(dir));
                        if (nb.isSolidRender(sl, p.relative(dir))) {
                            solidNeighbors++;
                            if (nb.is(Blocks.GRASS_BLOCK) || nb.is(Blocks.DIRT) || nb.is(Blocks.DIRT_PATH)) {
                                hasGrassOrDirt = true;
                            }
                        }
                    }
                    if (solidNeighbors >= 3 && hasGrassOrDirt && sl.getBlockState(p.above()).isAir()) {
                        Block fillBlock = sl.getBlockState(p.above()).isAir() ? Blocks.GRASS_BLOCK : Blocks.DIRT;
                        sl.setBlock(p, fillBlock.defaultBlockState(), 3);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5D, p.getY() + 1.0D, p.getZ() + 0.5D, 4, 0.2D, 0.2D, 0.2D, 0.02D);
                        if (fillTarget == null) fillTarget = p.immutable();
                        filledCount++;
                        if (filledCount >= 5) break;
                    }
                }
            }
            if (filledCount > 0 && fillTarget != null) {
                this.demon.getNavigation().moveTo(fillTarget.getX() + 0.5D, fillTarget.getY() + 1.0D, fillTarget.getZ() + 0.5D, 1.2D);
                sl.playSound(null, fillTarget.getX(), fillTarget.getY(), fillTarget.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.5F, 1.0F);
                payVillageFee(sl, villagers.get(0), "đã lấp đất & tu sửa địa hình thôn làng (" + filledCount + " khối)", 2);
                return;
            }

            // 4. Đứng xuống nói chuyện, ban phước lành & bảo vệ Dân Làng
            net.minecraft.world.entity.npc.Villager chosenVillager = villagers.get(this.demon.getRandom().nextInt(villagers.size()));
            this.demon.getNavigation().moveTo(chosenVillager, 1.15D);
            this.demon.getLookControl().setLookAt(chosenVillager, 30.0F, 30.0F);
            chosenVillager.getLookControl().setLookAt(this.demon, 30.0F, 30.0F);
            chosenVillager.heal(chosenVillager.getMaxHealth());
            chosenVillager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
            chosenVillager.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));

            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, chosenVillager.getX(), chosenVillager.getY() + 1.5D, chosenVillager.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.02D);
            sl.playSound(null, chosenVillager.getX(), chosenVillager.getY(), chosenVillager.getZ(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.5F, 1.0F);
            payVillageFee(sl, chosenVillager, "đã trò chuyện, bảo vệ & ban phước cho Dân Làng", 1 + this.demon.getRandom().nextInt(2));
        }

        private void payVillageFee(ServerLevel sl, net.minecraft.world.entity.npc.Villager villager, String serviceDesc, int emeraldCount) {
            if (!(this.demon.getOwner() instanceof ServerPlayer owner)) return;
            ItemStack emeraldFee = new ItemStack(Items.EMERALD, emeraldCount);
            boolean added = owner.getInventory().add(emeraldFee);
            if (!added) {
                owner.drop(emeraldFee, false);
            }
            if (this.demon.getRandom().nextFloat() < 0.35F) {
                ItemStack bonus = switch (this.demon.getRandom().nextInt(3)) {
                    case 0 -> new ItemStack(Items.GOLD_INGOT, 1 + this.demon.getRandom().nextInt(2));
                    case 1 -> new ItemStack(Items.GOLDEN_CARROT, 2);
                    default -> new ItemStack(Items.BREAD, 3);
                };
                if (!owner.getInventory().add(bonus)) {
                    owner.drop(bonus, false);
                }
            }
            sl.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
            owner.displayClientMessage(Component.literal("§6§l[Dịch Vụ Thôn Làng] §e" + this.demon.getEffectiveDemonName() + " §f" + serviceDesc + " §7→ Dân làng trả phí: §a+" + emeraldCount + " Emerald!"), true);
        }
    }
}
