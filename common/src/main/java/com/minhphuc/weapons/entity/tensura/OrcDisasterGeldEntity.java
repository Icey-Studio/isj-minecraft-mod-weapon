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
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Chúa Tể Orc Đói Khát - ORC DISASTER GELD:
 * - Hành quân cùng Binh Đoàn Đói Khát ("OrcHorde" - Piglin Brute bọc giáp sắt miễn nhiễm hóa thây ma).
 * - Unique Skill: Đói Khát (Starved): Khi lính Orc hoặc sinh vật xung quanh ngã xuống, Geld nuốt chửng để tích lũy
 *   sức mạnh vĩnh viễn (+sát thương, hồi phục HP cực đại, phình to vóc dáng).
 * - Pha 2 (<50% HP): Hỗn Độn Thực Giả (Chaos Eater) - Bắn chùm tia miasma đen tím ăn mòn giáp và gây Héo Úa.
 */
public class OrcDisasterGeldEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_STARVED_STACKS =
            SynchedEntityData.defineId(OrcDisasterGeldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHAOS_EATER =
            SynchedEntityData.defineId(OrcDisasterGeldEntity.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions CHAOS_PURPLE =
            new DustParticleOptions(new Vector3f(0.45F, 0.08F, 0.65F), 1.9F);
    private static final DustParticleOptions STARVED_RED =
            new DustParticleOptions(new Vector3f(0.88F, 0.16F, 0.12F), 1.7F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4§l[TAI ƯƠNG ĐÓI KHÁT] §6§lORC DISASTER GELD §7- §c[Binh Đoàn Tàn Sát]"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private int summonHordeCooldown = 80;
    private int chaosEaterCooldown = 60;

    public OrcDisasterGeldEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 3600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 38.0D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STARVED_STACKS, 0);
        builder.define(DATA_CHAOS_EATER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    public int getStarvedStacks() {
        return this.entityData.get(DATA_STARVED_STACKS);
    }

    public boolean isChaosEaterActive() {
        return this.entityData.get(DATA_CHAOS_EATER);
    }

    public void absorbPrey(ServerLevel sl, float healAmt) {
        int stacks = Math.min(25, getStarvedStacks() + 1);
        this.entityData.set(DATA_STARVED_STACKS, stacks);
        this.heal(healAmt + stacks * 8.0F);
        sl.sendParticles(STARVED_RED, this.getX(), this.getY() + 1.6, this.getZ(), 18, 0.8, 0.8, 0.8, 0.08);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.8F, 0.65F);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            BlockPos pos = this.blockPosition();
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD && sl.dimension() != Level.NETHER) return false;
                if (!sl.getEntitiesOfClass(OrcDisasterGeldEntity.class, new AABB(pos).inflate(96.0D)).isEmpty()) {
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
            if (isChaosEaterActive() && this.tickCount % 3 == 0) {
                this.level().addParticle(CHAOS_PURPLE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.8,
                        this.getY() + 1.2 + this.random.nextDouble(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.8, 0, 0.04, 0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.getTarget() != null || !sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(40.0D)).isEmpty());

        if (!isChaosEaterActive() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.entityData.set(DATA_CHAOS_EATER, true);
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            this.bossEvent.setName(Component.literal("§5§l[HỖN ĐỘN THỰC GIẢ - CHAOS EATER] §6§lORC DISASTER GELD §7(Đói Khát x" + getStarvedStacks() + ")"));
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.8F, 0.55F);
            for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(48.0D))) {
                sp.displayClientMessage(Component.literal("§5[Orc Disaster Geld] §cGeld đã thức tỉnh Hỗn Độn Thực Giả (Chaos Eater) — Miasma ăn mòn đang lan tỏa!"), false);
            }
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (summonHordeCooldown > 0) summonHordeCooldown--;
            if (chaosEaterCooldown > 0) chaosEaterCooldown--;

            if (summonHordeCooldown <= 0) {
                summonStarvingHorde(sl, target);
                summonHordeCooldown = 240;
            }
            if (isChaosEaterActive() && chaosEaterCooldown <= 0 && this.distanceToSqr(target) <= 24.0D * 24.0D) {
                castChaosEaterBeam(sl, target);
                chaosEaterCooldown = 75;
            }
        }
    }

    private void summonStarvingHorde(ServerLevel sl, LivingEntity target) {
        List<PiglinBrute> horde = sl.getEntitiesOfClass(PiglinBrute.class, this.getBoundingBox().inflate(40.0D),
                p -> p.getTags().contains("OrcHorde"));
        if (horde.size() >= 5) {
            for (PiglinBrute p : horde) {
                if (p.getTarget() == null) p.setTarget(target);
            }
            return;
        }
        int needed = Math.min(3, 5 - horde.size());
        for (int i = 0; i < needed; i++) {
            PiglinBrute soldier = EntityType.PIGLIN_BRUTE.create(sl);
            if (soldier != null) {
                soldier.setImmuneToZombification(true);
                soldier.addTag("OrcHorde");
                soldier.setCustomName(Component.literal("§6[Binh Đoàn Đói Khát] §cChiến Binh Orc"));
                soldier.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                soldier.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
                soldier.setTarget(target);
                soldier.setPos(this.getX() + (this.random.nextDouble() - 0.5) * 6.0, this.getY(), this.getZ() + (this.random.nextDouble() - 0.5) * 6.0);
                sl.addFreshEntity(soldier);
            }
        }
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PIGLIN_BRUTE_ANGRY, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    private void castChaosEaterBeam(ServerLevel sl, LivingEntity target) {
        Vec3 start = this.position().add(0, 1.5, 0);
        Vec3 end = target.position().add(0, 1.0, 0);
        Vec3 diff = end.subtract(start);
        for (int i = 1; i <= 14; i++) {
            Vec3 pt = start.add(diff.scale(i / 14.0D));
            sl.sendParticles(CHAOS_PURPLE, pt.x, pt.y, pt.z, 3, 0.25, 0.25, 0.25, 0.03);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, pt.x, pt.y, pt.z, 1, 0.15, 0.15, 0.15, 0.02);
        }
        float chaosDmg = 36.0F + getStarvedStacks() * 3.5F;
        target.hurt(this.damageSources().wither(), chaosDmg);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 2));
        this.heal(18.0F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.8F, 0.65F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel sl, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, source, recentlyHit);
        sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.5, this.getZ(), new ItemStack(Items.NETHERITE_SCRAP, 6)));
        sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.5, this.getZ(), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1)));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        SageManager.pushAlert(player, "ORC_DISASTER_SPAWN",
                "Phát hiện Chúa Tể Orc Đói Khát! Đừng để Geld nuốt chửng Binh Đoàn Orc để tích lũy Đói Khát.",
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
        tag.putInt("StarvedStacks", getStarvedStacks());
        tag.putBoolean("ChaosEater", isChaosEaterActive());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_STARVED_STACKS, tag.getInt("StarvedStacks"));
        this.entityData.set(DATA_CHAOS_EATER, tag.getBoolean("ChaosEater"));
    }
}
