package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.tensura.SageManager;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Dũng Giả Thời Không - CHLOE / CHRONOA:
 * - Ban đầu là thiếu nữ bình thường hiền lành (trạng thái Calm).
 * - Khi bị tấn công, trúng sét/lời nguyền hoặc hấp thụ nhiễu loạn thời không -> Hắc Hóa thành CHRONOA!
 * - Khi Hắc Hóa: tự trang bị Giáp Thần Linh & Thánh Kiếm Ánh Trăng (~65% sức mạnh tối đa của người chơi),
 *   tấn công MỌI mob xung quanh bằng Ngưng Đọng Thời Gian & Đoạn Không Trảm.
 * - Có thể thanh tẩy cô ấy trở lại bình thường bằng Táo Vàng khi máu < 30%!
 */
public class ChronoaEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DATA_CORRUPTED =
            SynchedEntityData.defineId(ChronoaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PURIFIED =
            SynchedEntityData.defineId(ChronoaEntity.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions CHRONO_VIOLET =
            new DustParticleOptions(new Vector3f(0.65F, 0.25F, 0.95F), 1.6F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§5§l[DŨNG GIẢ THỜI KHÔNG] §c§lCHRONOA §7- §d[Hắc Hóa ~65% Thần Khí]"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private int timeStopCooldown = 140;
    private int slashCooldown = 45;
    private int chaosExposure = 0;

    public ChronoaEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 900;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 32.0D) // ~60-65% sức mạnh player dùng Thánh Kiếm/Giáp Thần
                .add(Attributes.ARMOR, 18.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CORRUPTED, false);
        builder.define(DATA_PURIFIED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Khi hắc hóa -> Tấn công MỌI Mob & Player trong tầm nhìn!
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                m -> this.isCorrupted() && m != this && m.isAlive()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true,
                p -> this.isCorrupted()));
    }

    public boolean isCorrupted() {
        return this.entityData.get(DATA_CORRUPTED);
    }

    public boolean isPurified() {
        return this.entityData.get(DATA_PURIFIED);
    }

    public void triggerCorruption(ServerLevel sl, String reason) {
        if (isCorrupted()) return;
        this.entityData.set(DATA_CORRUPTED, true);
        this.entityData.set(DATA_PURIFIED, false);
        this.setCustomName(Component.literal("§5§l[Hắc Hóa] §cChronoa §7(Ác Linh Thời Không)"));
        this.setCustomNameVisible(true);

        // Trang bị Bộ Giáp Thần Linh & Thánh Kiếm Ánh Trăng (nhưng chỉ số boss đã bị giới hạn ~65% player!)
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.DIVINE_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DIVINE_CHESTPLATE.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.DIVINE_LEGGINGS.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.DIVINE_BOOTS.get()));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MOONLIGHT_SWORD.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        this.setHealth(this.getMaxHealth());
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.5F, 0.55F);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.2, this.getZ(), 50, 0.8, 1.2, 0.8, 0.15);

        for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(48.0D))) {
            sp.displayClientMessage(Component.literal("§5§l[HẮC HÓA THỜI KHÔNG] §fChloe §cđã bị tha hóa (" + reason + ") trở thành §5Chronoa§c — Mệnh lệnh: Thanh tẩy mọi sinh linh!"), false);
            SageManager.pushAlert(sp, "CHRONOA_CORRUPT",
                    "Chronoa đã hắc hóa với Giáp Thần & Thánh Kiếm (~65% ngưỡng sức mạnh)! Có thể dùng Táo Vàng thanh tẩy khi máu < 30%.",
                    "", 200);
        }
    }

    public void purify(ServerLevel sl, ServerPlayer purifier) {
        this.entityData.set(DATA_CORRUPTED, false);
        this.entityData.set(DATA_PURIFIED, true);
        this.setTarget(null);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
        this.setCustomName(Component.literal("§b[Dũng Giả Thanh Tẩy] §fChloe Aubert"));
        this.setCustomNameVisible(true);
        this.setHealth(this.getMaxHealth() * 0.6F);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 2.0F, 1.3F);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0, this.getZ(), 45, 0.7, 1.0, 0.7, 0.12);
        purifier.displayClientMessage(Component.literal("§b[Thanh Tẩy Thành Công] §fBạn đã giải thoát §eChloe Aubert §fkhỏi vòng lặp thời không Chronoa!"), false);
        sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.8, this.getZ(), new ItemStack(Items.NETHER_STAR, 1)));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp && isCorrupted()) {
            if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                if (this.getHealth() <= this.getMaxHealth() * 0.30F || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                    if (!player.isCreative()) stack.shrink(1);
                    purify((ServerLevel) this.level(), sp);
                    return InteractionResult.SUCCESS;
                } else {
                    sp.displayClientMessage(Component.literal("§e[Chronoa] §7Lý trí chưa khuất phục — Hãy hạ máu xuống dưới §c30% §7trước khi thanh tẩy bằng Táo Vàng!"), true);
                    return InteractionResult.CONSUME;
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            BlockPos pos = this.blockPosition();
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;
                if (!sl.getEntitiesOfClass(ChronoaEntity.class, new AABB(pos).inflate(110.0D)).isEmpty()) return false;
            }
        }
        return level.getDifficulty() != Difficulty.PEACEFUL && super.checkSpawnRules(level, spawnType);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (isCorrupted() && this.tickCount % 3 == 0) {
                this.level().addParticle(CHRONO_VIOLET,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.1,
                        this.getY() + 0.9 + this.random.nextDouble(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.1, 0, 0.03, 0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(isCorrupted());

        if (!isCorrupted() && !isPurified()) {
            // Ban đầu là thiếu nữ bình thường; nếu ban đêm hoặc gần quái ma tôn/lời nguyền sẽ dần bị tha hóa
            if (this.tickCount % 40 == 0) {
                boolean nearDark = !sl.isDay() || !sl.getEntitiesOfClass(UnderworldDemonEntity.class, this.getBoundingBox().inflate(18.0D)).isEmpty();
                if (nearDark && ++chaosExposure >= 8) {
                    triggerCorruption(sl, "Nhiễu loạn Ma tố Thời Không ban đêm");
                }
            }
            return;
        }

        if (isCorrupted()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                if (timeStopCooldown > 0) timeStopCooldown--;
                if (slashCooldown > 0) slashCooldown--;

                if (timeStopCooldown <= 0 && this.distanceToSqr(target) <= 16.0D * 16.0D) {
                    castChronoFreeze(sl);
                    timeStopCooldown = 200;
                }
                if (slashCooldown <= 0 && this.distanceToSqr(target) <= 12.0D * 12.0D) {
                    castMoonlightDimensionalSlash(sl, target);
                    slashCooldown = 50;
                }
            }
        }
    }

    private void castChronoFreeze(ServerLevel sl) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 1.35F);
        sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2, this.getZ(), 40, 6.0, 1.5, 6.0, 0.02);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0D), x -> x != this && x.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 6));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 55, 1));
        }
    }

    private void castMoonlightDimensionalSlash(ServerLevel sl, LivingEntity target) {
        Vec3 start = this.position().add(0, 1.1, 0);
        Vec3 end = target.position().add(0, 1.0, 0);
        Vec3 diff = end.subtract(start);
        for (int i = 1; i <= 10; i++) {
            Vec3 p = start.add(diff.scale(i / 10.0D));
            sl.sendParticles(CHRONO_VIOLET, p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0.02);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        // Sát thương ~60-65% ngưỡng Thánh Kiếm player
        target.hurt(this.damageSources().mobAttack(this), 42.0F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.8F, 1.2F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)) return false;

        if (!isCorrupted() && source.getEntity() instanceof LivingEntity) {
            triggerCorruption((ServerLevel) this.level(), "Bị kích động xung đột");
            return false;
        }
        // Giáp Thần Linh giảm 35% sát thương nhận vào
        if (isCorrupted()) {
            amount *= 0.65F;
        }
        return super.hurt(source, amount);
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
        tag.putBoolean("Corrupted", isCorrupted());
        tag.putBoolean("Purified", isPurified());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_CORRUPTED, tag.getBoolean("Corrupted"));
        this.entityData.set(DATA_PURIFIED, tag.getBoolean("Purified"));
    }
}
