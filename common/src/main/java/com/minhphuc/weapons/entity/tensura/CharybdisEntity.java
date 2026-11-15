package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.tensura.SageManager;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Bạo Phong Thủy Quái - CHARYBDIS:
 * - Sinh vật thảm họa sinh ra từ ma tố cô đặc của Veldora; bay lượn trên bầu trời như Rồng Biển khổng lồ.
 * - Vảy Kháng Ma Pháp (Magic Interference): Giảm 80% sát thương phép thuật cho đến khi Vây Lưng (Core Shield) bị phá
 *   hoặc bị Milim / Veldora tấn công trực tiếp!
 * - Triệu hồi Đàn Megalodon Bay hộ tống (Guardian bay theo hộ giá) + Phóng Vảy Bão Tố (Scale Volley) + Lốc Xoáy Hút Mục Tiêu.
 */
public class CharybdisEntity extends Monster {

    private static final EntityDataAccessor<Float> DATA_SCALE_SHIELD =
            SynchedEntityData.defineId(CharybdisEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_SHIELD_BROKEN =
            SynchedEntityData.defineId(CharybdisEntity.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions ABYSS_CYAN =
            new DustParticleOptions(new Vector3f(0.18F, 0.85F, 1.0F), 2.0F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§b§l[THẢM HỌA CẤP TAI ÁCH] §3§lCHARYBDIS §7- §9[Thủy Quái Bay]"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_12
    );

    private int scaleVolleyCooldown = 50;
    private int suctionCooldown = 140;
    private int escortSpawnCooldown = 180;

    public CharybdisEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1200;
        this.moveControl = new FlyingMoveControl(this, 16, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.44D)
                .add(Attributes.FLYING_SPEED, 0.64D)
                .add(Attributes.ATTACK_DAMAGE, 145.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 84.0D);
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
        builder.define(DATA_SCALE_SHIELD, 600.0F);
        builder.define(DATA_SHIELD_BROKEN, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isScaleShieldBroken() {
        return this.entityData.get(DATA_SHIELD_BROKEN);
    }

    public float getScaleShield() {
        return this.entityData.get(DATA_SCALE_SHIELD);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            BlockPos pos = this.blockPosition();
            if (!level.canSeeSky(pos) || pos.getY() < 65) return false;
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;
                if (!sl.getEntitiesOfClass(CharybdisEntity.class, new AABB(pos).inflate(96.0D)).isEmpty()) {
                    return false;
                }
            }
        }
        return level.getDifficulty() != Difficulty.PEACEFUL && super.checkSpawnRules(level, spawnType);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ABYSS_CYAN,
                        this.getX() + (this.random.nextDouble() - 0.5) * 4.0,
                        this.getY() + 1.5 + this.random.nextDouble() * 2.0,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 4.0,
                        0, -0.05, 0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.setNoGravity(true);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.getTarget() != null || !sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(48.0D)).isEmpty());

        // Tự hồi ma pháp khiên nếu thoát giao chiến lâu
        if (this.getTarget() == null && isScaleShieldBroken() && this.tickCount % 200 == 0) {
            this.entityData.set(DATA_SCALE_SHIELD, 600.0F);
            this.entityData.set(DATA_SHIELD_BROKEN, false);
            this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
        }

        // Cập nhật bay cho đàn Megalodon hộ tống
        for (Guardian g : sl.getEntitiesOfClass(Guardian.class, this.getBoundingBox().inflate(42.0D),
                e -> e.getTags().contains("CharybdisEscort"))) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                Vec3 dir = target.position().add(0, 2.5, 0).subtract(g.position());
                if (dir.lengthSqr() > 4.0D) {
                    g.setDeltaMovement(g.getDeltaMovement().scale(0.6).add(dir.normalize().scale(0.28D)));
                    g.setTarget(target);
                }
            }
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            // Duy trì độ cao bay lượn trên không tương xứng thân hình khổng lồ gấp 4 lần
            double desiredY = target.getY() + 12.0D;
            if (this.getY() < desiredY) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.08D, 0));
            } else if (this.getY() > desiredY + 6.0D) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04D, 0));
            }

            if (com.minhphuc.weapons.content.tensura.SkillNullificationAbility.isEntitySkillSealed(this)) {
                return;
            }

            if (scaleVolleyCooldown > 0) scaleVolleyCooldown--;
            if (suctionCooldown > 0) suctionCooldown--;
            if (escortSpawnCooldown > 0) escortSpawnCooldown--;

            if (escortSpawnCooldown <= 0) {
                spawnMegalodonEscorts(sl);
                escortSpawnCooldown = 280;
            }
            if (scaleVolleyCooldown <= 0 && this.distanceToSqr(target) <= 56.0D * 56.0D) {
                fireScaleVolley(sl, target);
                scaleVolleyCooldown = 95;
            }
            if (suctionCooldown <= 0 && this.distanceToSqr(target) <= 40.0D * 40.0D) {
                castTyphoonVortex(sl, target);
                suctionCooldown = 220;
            }
        }
    }

    private void spawnMegalodonEscorts(ServerLevel sl) {
        int count = sl.getEntitiesOfClass(Guardian.class, this.getBoundingBox().inflate(48.0D),
                e -> e.getTags().contains("CharybdisEscort")).size();
        if (count >= 4) return;
        for (int i = 0; i < Math.min(2, 4 - count); i++) {
            Guardian megalodon = EntityType.GUARDIAN.create(sl);
            if (megalodon != null) {
                megalodon.addTag("CharybdisEscort");
                megalodon.setNoGravity(true);
                megalodon.setCustomName(Component.literal("§3[Hộ Vệ Charybdis] §bMegalodon Bay"));
                megalodon.setPos(this.getX() + (i == 0 ? -4 : 4), this.getY() + 1.0, this.getZ() + 2);
                sl.addFreshEntity(megalodon);
            }
        }
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.8F, 0.7F);
    }

    private void fireScaleVolley(ServerLevel sl, LivingEntity target) {
        Vec3 start = this.position().add(0, 1.2, 0);
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(start).normalize();
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 2.2F, 0.65F);

        for (int b = -1; b <= 1; b++) {
            Vec3 spread = dir.yRot(b * 0.14F).normalize();
            for (double d = 2.0; d <= 34.0; d += 1.4) {
                Vec3 pt = start.add(spread.scale(d));
                sl.sendParticles(ABYSS_CYAN, pt.x, pt.y, pt.z, 2, 0.15, 0.15, 0.15, 0.02);
                sl.sendParticles(ParticleTypes.BUBBLE_POP, pt.x, pt.y, pt.z, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
        for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.5D),
                e -> e != this && !e.getTags().contains("CharybdisEscort") && e.isAlive())) {
            hit.hurt(this.damageSources().mobProjectile(this, this), 32.0F);
        }
    }

    private void castTyphoonVortex(ServerLevel sl, LivingEntity target) {
        Vec3 pullDir = this.position().subtract(target.position()).normalize();
        target.setDeltaMovement(pullDir.scale(1.1D).add(0, 0.75D, 0));
        target.hasImpulse = true;
        target.hurt(this.damageSources().mobAttack(this), 26.0F);
        sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 28, 1.5, 1.2, 1.5, 0.12);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 2.5F, 0.6F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)) {
            return false;
        }

        Entity attacker = source.getEntity();
        // Milim hoặc Veldora đánh trực diện -> Xé nát Vảy Kháng Ma Pháp tức thì!
        boolean brokenByGodTier = (attacker instanceof MilimEntity) || (attacker instanceof VeldoraEntity);
        if (brokenByGodTier && !isScaleShieldBroken()) {
            breakScaleShield((ServerLevel) this.level(), attacker);
        }

        if (!isScaleShieldBroken()) {
            // Vảy nhiễu loạn ma tố: Phép thuật bị giảm 80%, đòn vật lý hoặc bắn từ trên cao bào mòn giáp vảy!
            boolean isMagic = source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypeTags.WITCH_RESISTANT_TO);
            float shieldDmg = isMagic ? amount * 0.2F : amount;
            if (attacker != null && attacker.getY() > this.getY() + 0.5D) {
                shieldDmg *= 1.8F; // Đánh từ phía trên vào vây lưng yếu điểm
            }
            float nextShield = Math.max(0.0F, getScaleShield() - shieldDmg);
            this.entityData.set(DATA_SCALE_SHIELD, nextShield);

            if (nextShield <= 0.0F) {
                breakScaleShield((ServerLevel) this.level(), attacker);
            } else if (isMagic) {
                if (attacker instanceof ServerPlayer sp) {
                    SageManager.pushAlert(sp, "CHARYBDIS_MAGIC_JAM",
                            "Cảnh báo! Vảy Nhiễu Loạn Ma Pháp của Charybdis triệt tiêu 80% phép — Hãy đánh vào Vây Lưng trên cao!",
                            "", 100);
                }
                amount *= 0.20F;
            }
        }

        return super.hurt(source, amount);
    }

    private void breakScaleShield(ServerLevel sl, Entity breaker) {
        this.entityData.set(DATA_SCALE_SHIELD, 0.0F);
        this.entityData.set(DATA_SHIELD_BROKEN, true);
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 2.0, this.getZ(), 2, 1.0, 1.0, 1.0, 0.0);
        for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
            sp.displayClientMessage(Component.literal("§b[Charybdis] §eVảy Kháng Ma Pháp đã vỡ vụn! Lộ lõi yếu điểm toàn thân!"), false);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel sl, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, source, recentlyHit);
        ItemEntity core = new ItemEntity(sl, this.getX(), this.getY() + 1.0, this.getZ(), new ItemStack(Items.HEART_OF_THE_SEA, 2));
        sl.addFreshEntity(core);
        ItemEntity netherite = new ItemEntity(sl, this.getX(), this.getY() + 1.0, this.getZ(), new ItemStack(Items.NETHERITE_INGOT, 3));
        sl.addFreshEntity(netherite);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        SageManager.pushAlert(player, "CHARYBDIS_SPAWN",
                "Phát hiện Charybdis! Vảy Kháng Ma Pháp hoạt động — Cần đánh Vây Lưng hoặc nhờ Veldora/Milim phá giáp.",
                "", 140);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ScaleShield", getScaleShield());
        tag.putBoolean("ShieldBroken", isScaleShieldBroken());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ScaleShield")) this.entityData.set(DATA_SCALE_SHIELD, tag.getFloat("ScaleShield"));
        if (tag.contains("ShieldBroken")) this.entityData.set(DATA_SHIELD_BROKEN, tag.getBoolean("ShieldBroken"));
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }
}
