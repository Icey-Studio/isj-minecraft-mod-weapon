package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.tensura.VoiceOfTheWorld;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.init.ModItems;
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
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Chủng tộc Ác Ma Ma Giới (Underworld Demons: Hạ Cấp -> Thượng Cấp -> Đại Ác Ma -> Công Tước / Ma Vương).
 * - Đa dạng 3 hình thái (Nhân Dạng, Dị Thú, Đại Ác Ma Phi Hành) + 7 dòng dõi Thủy Tổ.
 * - Có thể ban thể xác vật lý bằng Xương, hoặc tự hấp thụ Dân Làng / xác chết / được mob hiến thân.
 * - Có thể thuần phục, dẫn theo, ra lệnh, Ban Tên và Tiến Hóa Ma Vương (30% kiểm soát / 70% phản chủ),
 *   với trần sức mạnh luôn thấp hơn Ác Ma Thủy Tổ.
 */
public class UnderworldDemonEntity extends TamableAnimal {

    public enum DemonRank {
        LESSER("Hạ Cấp Ác Ma", 0.90F, 90.0D, 9.0D, 4.0D, 15_000L, false, false),
        GREATER("Thượng Cấp Ác Ma", 1.10F, 240.0D, 19.0D, 8.0D, 65_000L, true, true),
        ARCHDEMON("Đại Ác Ma", 1.35F, 680.0D, 38.0D, 14.0D, 240_000L, true, true);

        public final String title;
        public final float baseScale;
        public final double baseHp;
        public final double baseAtk;
        public final double baseArmor;
        public final long baseEp;
        public final boolean canCastMagic;
        public final boolean innateWings;

        DemonRank(String title, float baseScale, double baseHp, double baseAtk, double baseArmor,
                  long baseEp, boolean canCastMagic, boolean innateWings) {
            this.title = title;
            this.baseScale = baseScale;
            this.baseHp = baseHp;
            this.baseAtk = baseAtk;
            this.baseArmor = baseArmor;
            this.baseEp = baseEp;
            this.canCastMagic = canCastMagic;
            this.innateWings = innateWings;
        }
    }

    public enum DemonMorph {
        HUMANOID("Nhân Dạng"),
        BEAST("Dị Thú"),
        WINGED_FIEND("Ác Ma Phi Hành");

        public final String label;

        DemonMorph(String label) {
            this.label = label;
        }
    }

    // Chế độ mệnh lệnh khi đã thuần phục
    public static final int CMD_FOLLOW = 0;
    public static final int CMD_GUARD = 1;
    public static final int CMD_AGGRESSIVE = 2;
    public static final int CMD_PASSIVE = 3;

    private static final EntityDataAccessor<Integer> DATA_DEMON_TYPE =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RANK =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MORPH =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BODY =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_BODY_ORIGIN =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_NAMED =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_CUSTOM_NAME =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_DEMON_LORD =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BERSERK_LORD =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WINGED =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_FLYING =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CASTING =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CMD_MODE =
            SynchedEntityData.defineId(UnderworldDemonEntity.class, EntityDataSerializers.INT);

    private int magicCooldown = 40;
    private int castingTicks = 0;
    private int soulKills = 0;
    private BlockPos guardAnchorPos = null;
    private boolean usingFlyingNav = false;

    public UnderworldDemonEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 25;
        this.moveControl = new MoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 450.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.FLYING_SPEED, 0.52D)
                .add(Attributes.ATTACK_DAMAGE, 28.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 44.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEMON_TYPE, DemonType.NOIR.ordinal());
        builder.define(DATA_RANK, DemonRank.LESSER.ordinal());
        builder.define(DATA_MORPH, DemonMorph.HUMANOID.ordinal());
        builder.define(DATA_HAS_BODY, false);
        builder.define(DATA_BODY_ORIGIN, "Chưa có (Linh Thể)");
        builder.define(DATA_IS_NAMED, false);
        builder.define(DATA_CUSTOM_NAME, "");
        builder.define(DATA_IS_DEMON_LORD, false);
        builder.define(DATA_BERSERK_LORD, false);
        builder.define(DATA_IS_WINGED, false);
        builder.define(DATA_IS_FLYING, false);
        builder.define(DATA_CASTING, false);
        builder.define(DATA_CMD_MODE, CMD_FOLLOW);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.32D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.25D, 8.0F, 3.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());

        // Khi hoang dã hoặc Ma Vương Cuồng Nộ: tấn công sinh vật & dân làng (để hấp thụ thể xác nếu chưa có)
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> (!this.isTame() || this.isBerserkDemonLord())));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, 12, true, false,
                target -> (!this.isTame() && !this.hasPhysicalBody()) || this.isBerserkDemonLord()));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> {
                    if (this.isBerserkDemonLord()) {
                        return target != this && !(target instanceof UnderworldDemonEntity u && u.isBerserkDemonLord());
                    }
                    if (this.isTame() && this.getCommandMode() == CMD_AGGRESSIVE) {
                        return target instanceof Enemy && !this.isAlliedTo(target);
                    }
                    return false;
                }));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData);

        var rand = level.getRandom();
        DemonType[] types = DemonType.values();
        DemonType rolledType = types[rand.nextInt(types.length)];

        // Xác suất cấp bậc: 60% Hạ Cấp, 28% Thượng Cấp, 12% Đại Ác Ma
        float r = rand.nextFloat();
        DemonRank rolledRank = (r < 0.60F) ? DemonRank.LESSER : (r < 0.88F ? DemonRank.GREATER : DemonRank.ARCHDEMON);

        // Hình thái: Đại Ác Ma thường dạng Nhân Dạng hoặc Ác Ma Phi Hành; Hạ/Thượng cấp đủ cả 3 dạng
        DemonMorph rolledMorph;
        if (rolledRank == DemonRank.ARCHDEMON) {
            rolledMorph = rand.nextFloat() < 0.65F ? DemonMorph.WINGED_FIEND : DemonMorph.HUMANOID;
        } else {
            DemonMorph[] morphs = DemonMorph.values();
            rolledMorph = morphs[rand.nextInt(morphs.length)];
        }

        // Băng Vực ưu tiên dòng dõi Lam / Bạch
        if (level.getBiome(this.blockPosition()).is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "underworld_abyssal_frost"))) {
            rolledType = rand.nextBoolean() ? DemonType.BLEU : DemonType.BLANC;
        } else if (level.getBiome(this.blockPosition()).is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "underworld_blood_gorge"))) {
            if (rand.nextFloat() < 0.5F) rolledType = DemonType.ROUGE;
        }

        this.setDemonType(rolledType);
        this.setRank(rolledRank);
        this.setMorph(rolledMorph);
        this.setWinged(rolledRank.innateWings || rolledMorph == DemonMorph.WINGED_FIEND);
        this.applyCalculatedStats(true);
        this.refreshDisplayTitle();
        return data;
    }

    public static boolean checkUnderworldSpawnRules(EntityType<? extends TamableAnimal> type, LevelAccessor level,
                                                    MobSpawnType spawnType, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (level instanceof ServerLevel sl) {
            // Giới hạn mật độ chống lag: tối đa 4 Ác Ma thường trong bán kính 48m
            AABB box = new AABB(pos).inflate(48.0D);
            if (sl.getEntitiesOfClass(UnderworldDemonEntity.class, box).size() >= 4) {
                return false;
            }
            if (sl.dimension() == Level.OVERWORLD && random.nextFloat() > 0.25F) {
                return false; // Overworld xuất hiện hiếm vào ban đêm
            }
        }
        return true;
    }

    // =========================================================================
    // TRẠNG THÁI & THÔNG SỐ (LUÔN NHỎ HƠN ÁC MA THỦY TỔ)
    // =========================================================================
    public void applyCalculatedStats(boolean healFull) {
        DemonRank rank = getRank();
        double hp = rank.baseHp;
        double atk = rank.baseAtk;
        double armor = rank.baseArmor;

        if (hasPhysicalBody()) {
            hp *= 2.1D;
            atk *= 2.3D;
            armor += 8.0D;
        }
        if (isNamed()) {
            hp *= 1.8D;
            atk *= 1.9D;
            armor += 6.0D;
        }
        if (isDemonLord()) {
            hp *= 1.9D;
            atk *= 2.0D;
            armor += 8.0D;
        }

        // Khóa trần: Tuyệt đối không vượt qua Thủy Tổ (PrimordialDemonEntity tối thiểu 950-3600 base x nhiều tầng)
        hp = Math.min(hp, 2400.0D);
        atk = Math.min(atk, 125.0D);
        armor = Math.min(armor, 28.0D);

        var maxHpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) maxHpAttr.setBaseValue(hp);
        var atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) atkAttr.setBaseValue(atk);
        var armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) armorAttr.setBaseValue(armor);

        if (healFull) {
            this.setHealth((float) hp);
        }
        syncNavigationMode();
    }

    public long getCalculatedEP() {
        long ep = getRank().baseEp;
        if (hasPhysicalBody()) ep = (long) (ep * 2.4D);
        if (isNamed()) ep = (long) (ep * 2.0D);
        if (isDemonLord()) ep = (long) (ep * 2.2D);
        return Math.min(ep, 2_800_000L);
    }

    private void syncNavigationMode() {
        boolean canFly = isWinged() || getRank().innateWings || getMorph() == DemonMorph.WINGED_FIEND || isDemonLord();
        if (canFly != this.usingFlyingNav) {
            this.usingFlyingNav = canFly;
            if (canFly) {
                this.moveControl = new FlyingMoveControl(this, 18, true);
                this.navigation = new FlyingPathNavigation(this, this.level());
            } else {
                this.moveControl = new MoveControl(this);
                this.navigation = new GroundPathNavigation(this, this.level());
            }
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    // =========================================================================
    // AI TICK (THROTTLED CHỐNG LAG): HẤP THỤ XÁC CHẾT, BAY, MA PHÁP NGUYÊN TỐ
    // =========================================================================
    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide() && this.isAlive()) {
            boolean canFly = isWinged() || getRank().innateWings || getMorph() == DemonMorph.WINGED_FIEND || isDemonLord();
            boolean shouldFloat = canFly && !this.onGround() && !this.isOrderedToSit();
            if (this.entityData.get(DATA_IS_FLYING) != shouldFloat) {
                this.entityData.set(DATA_IS_FLYING, shouldFloat);
            }
            this.setNoGravity(canFly && !this.isOrderedToSit() && (this.getTarget() != null || !this.onGround()));

            // Mỗi 20 ticks (1s): kiểm tra hấp thụ xác/xương rơi, chế độ canh giữ, hồi phục nhẹ
            if (this.tickCount % 20 == 0) {
                if (!this.hasPhysicalBody() && !this.isOrderedToSit()) {
                    tryAbsorbNearbyRemains();
                }

                // Chế độ Canh Giữ (Guard): kéo về gần neo nếu đi quá xa (> 18m)
                if (this.isTame() && this.getCommandMode() == CMD_GUARD && this.guardAnchorPos != null && this.getTarget() == null) {
                    if (this.blockPosition().distSqr(this.guardAnchorPos) > 18 * 18) {
                        this.getNavigation().moveTo(guardAnchorPos.getX() + 0.5D, guardAnchorPos.getY(), guardAnchorPos.getZ() + 0.5D, 1.2D);
                    }
                }

                // Hồi phục nhẹ tự nhiên (khi có thể xác hoặc đang ở Ma Giới)
                if (this.getHealth() < this.getMaxHealth() && (this.hasPhysicalBody() || this.level().dimension() == Level.NETHER)) {
                    this.heal(this.getMaxHealth() * (this.hasPhysicalBody() ? 0.015F : 0.008F));
                }
            }

            // Bắn ma pháp nguyên tố & Phong Sát Kỹ Năng (Đại Ác Ma / Ma Vương)
            if (canUseMagic() && !com.minhphuc.weapons.content.tensura.SkillNullificationAbility.isEntitySkillSealed(this)) {
                if (castingTicks > 0) {
                    castingTicks--;
                    if (castingTicks == 0) {
                        this.entityData.set(DATA_CASTING, false);
                    }
                }
                if (magicCooldown > 0) magicCooldown--;
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive() && magicCooldown <= 0 && !this.isPassiveMode()) {
                    double distSq = this.distanceToSqr(target);
                    if (distSq >= 16.0D && distSq <= 484.0D && this.hasLineOfSight(target)) {
                        if ((getRank() == DemonRank.ARCHDEMON || isDemonLord()) && this.random.nextFloat() < 0.18F) {
                            com.minhphuc.weapons.content.tensura.SkillNullificationAbility.castBossSkillSeal(
                                    (ServerLevel) this.level(), this, 22.0F, 140);
                            magicCooldown = 140;
                        } else {
                            castLineageMagic(target);
                            magicCooldown = (getRank() == DemonRank.ARCHDEMON || isDemonLord()) ? 50 : 75;
                        }
                    }
                }
            }
        }
    }

    private void tryAbsorbNearbyRemains() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(5.5D),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isCorpseItem(ie.getItem()));
        if (!items.isEmpty()) {
            ItemEntity corpse = items.get(0);
            String origin = "Hấp Thụ Xác (" + corpse.getItem().getHoverName().getString() + ")";
            corpse.getItem().shrink(1);
            if (corpse.getItem().isEmpty()) corpse.discard();
            grantPhysicalBody(origin, null);
        }
    }

    private boolean isCorpseItem(ItemStack stack) {
        return stack.is(Items.BONE) || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SKELETON_SKULL)
                || stack.is(Items.ZOMBIE_HEAD) || stack.is(ModItems.ARTIFICIAL_SKELETON.get());
    }

    public boolean canUseMagic() {
        return getRank().canCastMagic || isNamed() || isDemonLord();
    }

    private void castLineageMagic(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        this.castingTicks = 12;
        this.entityData.set(DATA_CASTING, true);

        DemonType type = getDemonType();
        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 dir = end.subtract(start).normalize();
        double dist = Math.min(20.0D, start.distanceTo(end));

        int color = type.getGlowColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), 1.4F);

        for (double d = 0.8D; d <= dist; d += 0.8D) {
            Vec3 p = start.add(dir.scale(d));
            sl.sendParticles(dust, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.01);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.1, 0.1, 0.1, 0.0);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.9F);

        float magicDmg = (float) Math.max(8.0D, this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85D);
        if (target.hurt(this.damageSources().indirectMagic(this, this), magicDmg)) {
            if (type == DemonType.ROUGE) target.igniteForSeconds(4);
            else if (type == DemonType.BLEU || type == DemonType.BLANC) target.setTicksFrozen(100);
        }
    }

    // =========================================================================
    // HẤP THỤ DÂN LÀNG / XÁC MOB ĐỂ NHẬN THỂ XÁC & THĂNG CẤP
    // =========================================================================
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean res = super.killedEntity(level, victim);
        this.soulKills++;

        // Chưa có thể xác -> Hấp thụ Dân Làng hoặc Sinh vật vừa hạ làm thể xác vật lý!
        if (!this.hasPhysicalBody() && victim != null) {
            if (victim instanceof AbstractVillager) {
                grantPhysicalBody("Hấp Thụ Dân Làng (" + victim.getDisplayName().getString() + ")", null);
            } else if (!(victim instanceof UnderworldDemonEntity)) {
                grantPhysicalBody("Hấp Thụ Xác (" + victim.getType().getDescription().getString() + ")", null);
            }
        }

        // Thăng cấp tự nhiên trong Ma Giới: Hạ Cấp -> Thượng Cấp (3 kills), Thượng Cấp -> Đại Ác Ma (8 kills)
        if (!this.isDemonLord() && getRank() == DemonRank.LESSER && soulKills >= 3) {
            setRank(DemonRank.GREATER);
            setWinged(true);
            applyCalculatedStats(true);
            refreshDisplayTitle();
        } else if (!this.isDemonLord() && getRank() == DemonRank.GREATER && soulKills >= 8) {
            setRank(DemonRank.ARCHDEMON);
            setWinged(true);
            applyCalculatedStats(true);
            refreshDisplayTitle();
        }
        return res;
    }

    public void grantPhysicalBody(String origin, @Nullable Player byPlayer) {
        if (this.hasPhysicalBody()) return;
        this.setPhysicalBody(true);
        this.setBodyOrigin(origin);
        this.setWinged(true);
        this.applyCalculatedStats(true);
        this.refreshDisplayTitle();

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 30, 0.5, 0.7, 0.5, 0.06);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0D, this.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.NEUTRAL, 1.4F, 1.1F);
        }
        if (byPlayer != null) {
            byPlayer.displayClientMessage(Component.literal("§6§l✦ [BAN THỂ XÁC] §f" + getEffectiveTitle() + " §ađã nhận Thể Xác Vật Lý (§e" + origin + "§a)!"), true);
        }
    }

    // =========================================================================
    // TƯƠNG TÁC: BAN THỂ XÁC BẰNG XƯƠNG, THUẦN PHỤC, RA LỆNH
    // =========================================================================
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        // Để Điện Thoại Ma Thép xử lý trước nếu đang cầm
        if (held.is(ModItems.MAGISTEEL_PHONE.get())) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 1. Hoang dã hoặc Ma Vương cuồng nộ: Thuần phục / Áp chế bằng Hồn hoặc khi HP <= 35% (Hạ cấp <= 100%)
        if (!this.isTame() || this.isBerserkDemonLord()) {
            boolean usingSoul = held.is(ModItems.MOB_SOUL.get()) || held.is(ModItems.DEMON_LORD_SOUL.get());
            boolean lowHp = this.getHealth() <= this.getMaxHealth() * 0.35F || (!this.isBerserkDemonLord() && getRank() == DemonRank.LESSER && this.getHealth() <= this.getMaxHealth() * 0.60F);
            if (usingSoul || lowHp) {
                if (this.isBerserkDemonLord() && this.getHealth() > this.getMaxHealth() * 0.25F) {
                    player.displayClientMessage(Component.literal("§c⚠ Ma Vương Ác Ma Cuồng Nộ vẫn còn khỏe! Hãy đánh xuống dưới 25% HP để áp chế lại!"), true);
                    return InteractionResult.FAIL;
                }
                if (usingSoul && !player.isCreative()) held.shrink(1);
                this.tame(player);
                this.setBerserkDemonLord(false);
                this.removeTag("TensuraDemonLordBerserk");
                this.addTag("TensuraDemonLordCalm");
                this.setTarget(null);
                this.setCommandMode(CMD_FOLLOW);
                this.refreshDisplayTitle();
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.4D, this.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
                }
                player.displayClientMessage(Component.literal("§d§l✦ [KHẾ ƯỚC ÁC MA] §fĐã khuất phục §e" + getEffectiveTitle() + " §flàm thuộc hạ!"), true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.literal("§7Hãy đánh suy yếu dưới 35% máu (hoặc cầm Linh Hồn) để thu phục Ác Ma này!"), true);
                return InteractionResult.FAIL;
            }
        }

        // 2. Đã thuần phục bởi chủ nhân
        if (this.isOwnedBy(player)) {
            // Cầm Xương hoặc Khung Xương Ma Thiết -> Ban Thể Xác Vật Lý trực tiếp!
            if (held.is(Items.BONE) || held.is(ModItems.ARTIFICIAL_SKELETON.get())) {
                if (this.hasPhysicalBody()) {
                    player.displayClientMessage(Component.literal("§e✦ Ác ma này đã có Thể Xác Vật Lý rồi (§b" + getBodyOrigin() + "§e)!"), true);
                    return InteractionResult.FAIL;
                }
                if (!player.isCreative()) held.shrink(1);
                String sourceLabel = held.is(ModItems.ARTIFICIAL_SKELETON.get())
                        ? "Khung Xương Ma Thiết (" + player.getName().getString() + ")"
                        : "Xương Ban Tặng (" + player.getName().getString() + ")";
                grantPhysicalBody(sourceLabel, player);
                return InteractionResult.SUCCESS;
            }

            // Shift + Chuột phải: Bật/tắt chế độ bay / cánh hoặc báo trạng thái chi tiết
            if (player.isShiftKeyDown()) {
                boolean nextWings = !this.isWinged();
                this.setWinged(nextWings);
                syncNavigationMode();
                player.displayClientMessage(Component.literal("§5§l[" + getEffectiveTitle() + "] §fĐã " + (nextWings ? "§aGIẢI PHÓNG CÁNH BAY" : "§7THU HỒI CÁNH") + "§f!"), true);
                return InteractionResult.SUCCESS;
            }

            // Chuột phải thường: Xoay vòng 4 Mệnh Lệnh (Theo Sau -> Canh Giữ -> Tấn Công -> Đứng Yên)
            int nextCmd = (this.getCommandMode() + 1) % 4;
            this.setCommandMode(nextCmd);
            if (nextCmd == CMD_GUARD) {
                this.guardAnchorPos = this.blockPosition();
                this.setOrderedToSit(false);
            } else if (nextCmd == CMD_PASSIVE) {
                this.setOrderedToSit(true);
                this.setTarget(null);
            } else {
                this.setOrderedToSit(false);
            }
            player.displayClientMessage(Component.literal("§b§l[MỆNH LỆNH ÁC MA] §e" + getEffectiveTitle() + " §f-> " + getCommandModeLabel()), true);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    public String getCommandModeLabel() {
        return switch (getCommandMode()) {
            case CMD_GUARD -> "§eCanh Giữ Khu Vực";
            case CMD_AGGRESSIVE -> "§cChủ Động Tiêu Diệt Quái";
            case CMD_PASSIVE -> "§7Đứng Yên / Tự Vệ";
            default -> "§aĐi Theo Chủ Nhân";
        };
    }

    public boolean isPassiveMode() {
        return this.isTame() && getCommandMode() == CMD_PASSIVE;
    }

    public void applyNameEvolution(String chosenName, Player namer) {
        this.setNamed(true);
        this.setCustomDemonName(chosenName);
        this.addTag("TensuraNamedMob");
        // Ban tên nâng cấp lên Ác Ma Công Tước (tối thiểu Thượng Cấp nếu đang Hạ Cấp)
        if (getRank() == DemonRank.LESSER) {
            setRank(DemonRank.GREATER);
        }
        this.setWinged(true);
        this.applyCalculatedStats(true);
        this.refreshDisplayTitle();
        if (namer != null) {
            namer.displayClientMessage(Component.literal("§6§l✦ [BAN DANH XƯNG] §e" + chosenName + " §fđã tiến hóa thành §dÁc Ma Công Tước (" + getRank().title + ")§f!"), false);
        }
    }

    public void evolveToDemonLord(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        this.setDemonLord(true);
        this.addTag("TensuraDemonLordMob");
        this.setWinged(true);

        // 30% kiểm soát được, 70% mất kiểm soát phản chủ
        boolean controlled = sl.random.nextFloat() < 0.30F;
        if (controlled) {
            this.setBerserkDemonLord(false);
            this.addTag("TensuraDemonLordCalm");
            if (player != null && !this.isTame()) this.tame(player);
            if (player != null) {
                VoiceOfTheWorld.announce(player, "Báo cáo: Ác Ma " + getEffectiveTitle() + " đã thức tỉnh Ma Vương (30% Thành Công - Kiểm Soát Hoàn Toàn)!");
            }
        } else {
            this.setBerserkDemonLord(true);
            this.addTag("TensuraDemonLordBerserk");
            this.setTame(false, false);
            this.setOwnerUUID(null);
            this.setOrderedToSit(false);
            this.setTarget(player);
            if (player != null) {
                VoiceOfTheWorld.announce(player, "Cảnh báo! Ác Ma " + getEffectiveTitle() + " thức tỉnh Ma Vương nhưng ĐÃ MẤT KIỂM SOÁT (70% Phản Chủ)!");
            }
        }
        this.applyCalculatedStats(true);
        this.refreshDisplayTitle();

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5F, 0.85F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 45, 0.8, 1.0, 0.8, 0.08);
    }

    public void refreshDisplayTitle() {
        String name = getEffectiveTitle();
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(isNamed() || isTame() || isDemonLord());
    }

    public String getEffectiveTitle() {
        String prefix = isBerserkDemonLord() ? "§4§l[Ma Vương Cuồng Nộ] "
                : (isDemonLord() ? "§5§l[Ma Vương Ác Ma] "
                : (isNamed() ? "§6§l[Công Tước] " : "§c"));
        String bodyName = hasCustomDemonName() ? getCustomDemonName() : getRank().title;
        return prefix + bodyName + " §7(" + getDemonType().getColorName() + ")";
    }

    // =========================================================================
    // GETTERS / SETTERS & NBT PERSISTENCE
    // =========================================================================
    public DemonType getDemonType() {
        int idx = this.entityData.get(DATA_DEMON_TYPE);
        DemonType[] vals = DemonType.values();
        return vals[Mth.clamp(idx, 0, vals.length - 1)];
    }

    public void setDemonType(DemonType t) {
        this.entityData.set(DATA_DEMON_TYPE, (t != null ? t : DemonType.NOIR).ordinal());
    }

    public DemonRank getRank() {
        int idx = this.entityData.get(DATA_RANK);
        DemonRank[] vals = DemonRank.values();
        return vals[Mth.clamp(idx, 0, vals.length - 1)];
    }

    public void setRank(DemonRank r) {
        this.entityData.set(DATA_RANK, (r != null ? r : DemonRank.LESSER).ordinal());
    }

    public DemonMorph getMorph() {
        int idx = this.entityData.get(DATA_MORPH);
        DemonMorph[] vals = DemonMorph.values();
        return vals[Mth.clamp(idx, 0, vals.length - 1)];
    }

    public void setMorph(DemonMorph m) {
        this.entityData.set(DATA_MORPH, (m != null ? m : DemonMorph.HUMANOID).ordinal());
    }

    public boolean hasPhysicalBody() {
        return this.entityData.get(DATA_HAS_BODY);
    }

    public void setPhysicalBody(boolean val) {
        this.entityData.set(DATA_HAS_BODY, val);
    }

    public String getBodyOrigin() {
        return this.entityData.get(DATA_BODY_ORIGIN);
    }

    public void setBodyOrigin(String origin) {
        this.entityData.set(DATA_BODY_ORIGIN, origin != null ? origin : "Không rõ");
    }

    public boolean isNamed() {
        return this.entityData.get(DATA_IS_NAMED);
    }

    public void setNamed(boolean named) {
        this.entityData.set(DATA_IS_NAMED, named);
    }

    public boolean hasCustomDemonName() {
        return !this.entityData.get(DATA_CUSTOM_NAME).isEmpty();
    }

    public String getCustomDemonName() {
        return this.entityData.get(DATA_CUSTOM_NAME);
    }

    public void setCustomDemonName(String name) {
        this.entityData.set(DATA_CUSTOM_NAME, name != null ? name : "");
    }

    public boolean isDemonLord() {
        return this.entityData.get(DATA_IS_DEMON_LORD);
    }

    public void setDemonLord(boolean val) {
        this.entityData.set(DATA_IS_DEMON_LORD, val);
    }

    public boolean isBerserkDemonLord() {
        return this.entityData.get(DATA_BERSERK_LORD);
    }

    public void setBerserkDemonLord(boolean val) {
        this.entityData.set(DATA_BERSERK_LORD, val);
    }

    public boolean isWinged() {
        return this.entityData.get(DATA_IS_WINGED);
    }

    public void setWinged(boolean val) {
        this.entityData.set(DATA_IS_WINGED, val);
    }

    public boolean isFlyingNow() {
        return this.entityData.get(DATA_IS_FLYING);
    }

    public boolean isCastingMagic() {
        return this.entityData.get(DATA_CASTING);
    }

    public int getCommandMode() {
        return this.entityData.get(DATA_CMD_MODE);
    }

    public void setCommandMode(int mode) {
        this.entityData.set(DATA_CMD_MODE, Mth.clamp(mode, 0, 3));
    }

    public void setGuardAnchor(BlockPos anchor) {
        this.setCommandMode(CMD_GUARD);
        this.guardAnchorPos = anchor;
        this.setOrderedToSit(false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("DemonType", getDemonType().name());
        tag.putString("DemonRank", getRank().name());
        tag.putString("DemonMorph", getMorph().name());
        tag.putBoolean("HasPhysicalBody", hasPhysicalBody());
        tag.putString("BodyOrigin", getBodyOrigin());
        tag.putBoolean("IsNamed", isNamed());
        tag.putString("DemonCustomName", getCustomDemonName());
        tag.putBoolean("IsDemonLord", isDemonLord());
        tag.putBoolean("IsBerserkLord", isBerserkDemonLord());
        tag.putBoolean("IsWinged", isWinged());
        tag.putInt("CmdMode", getCommandMode());
        tag.putInt("SoulKills", soulKills);
        if (guardAnchorPos != null) {
            tag.putLong("GuardAnchor", guardAnchorPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DemonType")) {
            try { setDemonType(DemonType.valueOf(tag.getString("DemonType"))); } catch (Exception ignored) {}
        }
        if (tag.contains("DemonRank")) {
            try { setRank(DemonRank.valueOf(tag.getString("DemonRank"))); } catch (Exception ignored) {}
        }
        if (tag.contains("DemonMorph")) {
            try { setMorph(DemonMorph.valueOf(tag.getString("DemonMorph"))); } catch (Exception ignored) {}
        }
        setPhysicalBody(tag.getBoolean("HasPhysicalBody"));
        if (tag.contains("BodyOrigin")) setBodyOrigin(tag.getString("BodyOrigin"));
        setNamed(tag.getBoolean("IsNamed"));
        if (tag.contains("DemonCustomName")) setCustomDemonName(tag.getString("DemonCustomName"));
        setDemonLord(tag.getBoolean("IsDemonLord"));
        setBerserkDemonLord(tag.getBoolean("IsBerserkLord"));
        setWinged(tag.getBoolean("IsWinged"));
        setCommandMode(tag.getInt("CmdMode"));
        soulKills = tag.getInt("SoulKills");
        if (tag.contains("GuardAnchor")) {
            guardAnchorPos = BlockPos.of(tag.getLong("GuardAnchor"));
        }
        syncNavigationMode();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
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
    public boolean fireImmune() {
        return true;
    }
}
