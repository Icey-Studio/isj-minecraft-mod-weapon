package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager;
import com.minhphuc.weapons.content.tensura.MobMeleeSkillSystem;
import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Boss Tối Thượng: Hỗn Độn Long (Chaos Dragon - Slime Isekai).
 * - Thân rồng Obsidian tím khổng lồ, vảy nứt năng lượng hồng phát sáng, giáp bụng oải hương trắng,
 *   vai mọc cụm tinh thể gai góc, chóp đuôi rực cháy lửa neon.
 * - Chỉ tồn tại ở dạng rồng (không có dạng người).
 * - Kích thước khổng lồ x5 (Scale 14.0F).
 * - Miễn nhiễm sát thủ Azathoth (chỉ mất tối đa 40% máu), tông vỡ mọi kết giới.
 * - Khóa mọi kỹ năng dịch chuyển trong phạm vi 100m.
 * - Tử huyệt duy nhất: Bị kết liễu bởi Long Tinh Bạo Viêm Phá (Dragon Nova).
 */
public class ChaosDragonEntity extends PathfinderMob {

    public static final EntityDataAccessor<Integer> DATA_CASTING_SKILL =
            SynchedEntityData.defineId(ChaosDragonEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_COCOON_ACTIVE =
            SynchedEntityData.defineId(ChaosDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent;
    private int skillTimer = 0;
    private int windupChargeTimer = 0;
    private int windupBreathTimer = 0;
    private int cocoonTimer = 0;
    private boolean cocoonUsed = false;

    public ChaosDragonEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§5§lHỖN ĐỘN LONG §d« CHAOS DRAGON »"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_20
        );
        this.bossEvent.setVisible(true);
        this.xpReward = 10000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16000.0D) // Bằng Chước Nhiệt Long Velgrynd
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 350.0D)
                .add(Attributes.ARMOR, 55.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 45.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING_SKILL, 0);
        builder.define(DATA_COCOON_ACTIVE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();
    }

    public int getCastingSkill() {
        return this.entityData.get(DATA_CASTING_SKILL);
    }

    public void setCastingSkill(int skill) {
        this.entityData.set(DATA_CASTING_SKILL, skill);
    }

    public boolean isCocoonActive() {
        return this.entityData.get(DATA_COCOON_ACTIVE);
    }

    public void setCocoonActive(boolean active) {
        this.entityData.set(DATA_COCOON_ACTIVE, active);
    }

    public Vec3 getMouthPos() {
        Vec3 look = this.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() > 1e-4) {
            lookFlat = lookFlat.normalize();
        } else {
            lookFlat = look;
        }
        // Thân Hỗn Độn Long khổng lồ Scale 14x: miệng nằm ở đỉnh đầu vươn ra trước 7.5m và cao 11.5m
        return this.position().add(0, 11.5D, 0)
                .add(lookFlat.scale(7.5D))
                .add(0, look.y * 3.0D, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Hiệu ứng đuôi lửa neon tím & vết nứt năng lượng
            Vec3 tail = this.position().subtract(this.getLookAngle().scale(6.0D)).add(0, 3.5D, 0);
            DustParticleOptions neonPink = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.85F), 2.0F);
            this.level().addParticle(neonPink, tail.x, tail.y, tail.z, 0, 0.1, 0);
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, tail.x, tail.y, tail.z, 0, 0.05, 0);
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();
        if (this.isDeadOrDying() || !this.isAlive()) {
            this.bossEvent.setVisible(false);
            this.bossEvent.removeAllPlayers();
            return;
        }

        // Tự động dọn dẹp người chơi rời xa > 96 block
        if (this.tickCount % 20 == 0) {
            for (ServerPlayer p : new java.util.ArrayList<>(this.bossEvent.getPlayers())) {
                if (p.isRemoved() || !p.isAlive() || p.distanceToSqr(this) > 96.0D * 96.0D) {
                    this.bossEvent.removePlayer(p);
                }
            }
            boolean hasNearby = !sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(96.0D)).isEmpty();
            this.bossEvent.setVisible(hasNearby);
        }

        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()), 0.0F, 1.0F));

        // 1. PHÁ VỠ MỌI KẾT GIỚI KHI DI CHUYỂN
        AntiMagicBarrierManager.shatterBarrierAt(this.level(), this.blockPosition());
        MultilayerBarrierAbility.shatterBarrierAt(this.level(), this.blockPosition());

        // 2. TỤ LỰC HỖN ĐỘN TÔNG KÍCH (5s)
        if (windupChargeTimer > 0) {
            windupChargeTimer--;
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 3.0D, this.getZ(), 20, 3.0D, 3.0D, 3.0D, 0.1D);
            if (windupChargeTimer == 0) {
                executeChaosTrample(sl);
            }
            return;
        }

        // 3. TỤ LỰC HỖN ĐỘN DIỆT TINH TỨC (8s)
        if (windupBreathTimer > 0) {
            windupBreathTimer--;
            Vec3 mouthPos = this.getMouthPos();
            DustParticleOptions darkMagenta = new DustParticleOptions(new Vector3f(0.85F, 0.0F, 0.9F), 3.5F);
            DustParticleOptions neonPink = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.8F), 2.5F);
            // Hạt ma tố tím hồng xoắn ốc hội tụ CHÍNH XÁC vào đầu / miệng rồng, không tụ ở bụng
            for (int i = 0; i < 14; i++) {
                double ang = this.random.nextDouble() * Math.PI * 2.0D;
                double r = 3.5D + this.random.nextDouble() * 7.5D;
                double px = mouthPos.x + Math.cos(ang) * r;
                double pz = mouthPos.z + Math.sin(ang) * r;
                double py = mouthPos.y + (this.random.nextDouble() - 0.5D) * 4.5D;
                double vx = (mouthPos.x - px) * 0.14D;
                double vy = (mouthPos.y - py) * 0.14D;
                double vz = (mouthPos.z - pz) * 0.14D;
                sl.sendParticles(darkMagenta, px, py, pz, 0, vx, vy, vz, 1.0D);
                sl.sendParticles(neonPink, px, py, pz, 0, vx, vy, vz, 1.0D);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0, vx, vy, vz, 1.0D);
            }
            if (windupBreathTimer == 0) {
                executeStarBusterBreath(sl);
            }
            return;
        }

        // 4. KÉN TINH THỂ PHỤC HỒI DƯỚI 20% MÁU
        if (!cocoonUsed && this.getHealth() <= this.getMaxHealth() * 0.20F) {
            cocoonUsed = true;
            this.setCocoonActive(true);
            cocoonTimer = 100; // 5 giây
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 4.0F, 0.6F);
        }

        if (this.isCocoonActive()) {
            cocoonTimer--;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 4.0D, this.getZ(), 30, 4.0D, 5.0D, 4.0D, 0.2D);
            if (cocoonTimer <= 0) {
                this.setCocoonActive(false);
                this.heal(this.getMaxHealth() * 0.15F); // Hồi 15% máu
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 3.0F, 0.8F);
            }
            return;
        }

        // 5. QUẢN LÝ KỸ NĂNG TẤN CÔNG THƯỜNG NIÊN
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            skillTimer++;
            if (skillTimer >= 140) { // Mỗi 7 giây tung 1 tuyệt kỹ
                skillTimer = 0;
                int roll = this.random.nextInt(10);
                switch (roll) {
                    case 0 -> startChaosTrample(sl);
                    case 1 -> castChaosThunderstrike(sl, target);
                    case 2 -> castDarkPlasmaOrbs(sl, target);
                    case 3 -> startStarBusterBreath(sl);
                    case 4 -> castCrystalFeatherBarrage(sl, target);
                    case 5 -> castDarkDragonAura(sl);
                    case 6 -> castVoidRoar(sl);
                    case 7 -> castAbyssalStomp(sl);
                    case 8 -> castVoidClaw(sl, target);
                    case 9 -> castMagiculeCorrosiveMist(sl);
                }
            }
        }
    }

    private void startChaosTrample(ServerLevel sl) {
        this.windupChargeTimer = 100; // 5s tụ lực
        this.setCastingSkill(1);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 0.5F);

        for (ServerPlayer sp : sl.players()) {
            if (sp.distanceToSqr(this) <= 100.0D * 100.0D) {
                sp.displayClientMessage(
                        Component.literal("§c§l« CẢNH BÁO RAPHAEL »\n§ePhòng Ngự Tuyệt Đối của Uriel không thể ngăn chặn đòn [Hỗn Độn Tông Kích]! Hãy né tránh ngay lập tức!"),
                        false
                );
            }
        }
    }

    private void executeChaosTrample(ServerLevel sl) {
        this.setCastingSkill(0);
        LivingEntity target = this.getTarget();
        Vec3 dir = target != null ? target.position().subtract(this.position()).normalize() : this.getLookAngle();
        Vec3 targetDest = this.position().add(dir.scale(40.0D));

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.5F, 0.6F);

        // Lao thẳng 40m
        for (double d = 0; d <= 40.0D; d += 2.0D) {
            Vec3 p = this.position().add(dir.scale(d));
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 2.0D, p.z, 20, 2.5D, 2.5D, 2.5D, 0.1D);
            List<LivingEntity> hitList = sl.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 4, p.y - 2, p.z - 4, p.x + 4, p.y + 4, p.z + 4), e -> e != this && e.isAlive());
            for (LivingEntity e : hitList) {
                // Gây sát thương 20% Max HP xuyên qua Uriel Absolute Defense
                float damage = e.getMaxHealth() * 0.20F;
                e.addTag("UrielBypassDamage");
                e.hurt(this.damageSources().mobAttack(this), damage);
                e.removeTag("UrielBypassDamage");
                e.setDeltaMovement(dir.x * 2.5D, 1.2D, dir.z * 2.5D);
            }
        }
        this.teleportTo(targetDest.x, targetDest.y, targetDest.z);
    }

    private void castChaosThunderstrike(ServerLevel sl, LivingEntity target) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.5F, 0.8F);
        for (int i = 0; i < 6; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 16.0D;
            double oz = (this.random.nextDouble() - 0.5D) * 16.0D;
            BlockPos lp = BlockPos.containing(target.getX() + ox, target.getY(), target.getZ() + oz);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(lp));
                sl.addFreshEntity(bolt);
            }
        }
    }

    private void castDarkPlasmaOrbs(ServerLevel sl, LivingEntity target) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.8F, 0.7F);
        Vec3 src = this.position().add(0, 5.0D, 0);
        Vec3 dir = target.position().add(0, 1.0D, 0).subtract(src).normalize();

        for (int i = 0; i < 3; i++) {
            double spreadX = (this.random.nextDouble() - 0.5D) * 0.4D;
            double spreadZ = (this.random.nextDouble() - 0.5D) * 0.4D;
            Vec3 orbDir = dir.add(spreadX, 0, spreadZ).normalize();

            for (double d = 2.0D; d <= 25.0D; d += 2.0D) {
                Vec3 p = src.add(orbDir.scale(d));
                sl.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 10, 0.5, 0.5, 0.5, 0.05);
            }
            sl.explode(this, target.getX(), target.getY(), target.getZ(), 3.5F, Level.ExplosionInteraction.NONE);
        }
    }

    private void startStarBusterBreath(ServerLevel sl) {
        this.windupBreathTimer = 160; // 8s tụ lực
        this.setCastingSkill(2);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.5F, 0.5F);

        for (ServerPlayer sp : sl.players()) {
            if (sp.distanceToSqr(this) <= 120.0D * 120.0D) {
                sp.displayClientMessage(
                        Component.literal("§5§l« BÁO CÁO RAPHAEL: HỖN ĐỘN DIỆT TINH TỨC »\n§dHỗn Độn Long đang ngửa cổ tụ năng lượng plasma hủy diệt! Không gian đang sụp đổ, hãy tìm kiếm vật cản kiên cố!"),
                        false
                );
            }
        }
    }

    private void executeStarBusterBreath(ServerLevel sl) {
        this.setCastingSkill(0);
        LivingEntity target = this.getTarget();
        Vec3 dir = target != null ? target.position().subtract(this.position()).normalize() : this.getLookAngle();
        Vec3 mouthPos = this.getMouthPos();

        sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 6.0F, 0.65F);
        sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5.0F, 0.45F);
        sl.playSound(null, mouthPos.x, mouthPos.y, mouthPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 4.5F, 0.7F);

        // PHÁT GÓI CINEMATIC 3D PLASMA: Chùm tia plasma 3D tròn xoay đa tầng (loại bỏ cột vuông ItemDisplay)
        ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_CHAOS_STAR_BUSTER, this.getUUID(), mouthPos, dir, 48, ClientboundCinematicSkillPacket.ACTION_START, 1.35f
        ));

        DustParticleOptions plasma = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.95F), 4.0F);

        for (double d = 3.0D; d <= 80.0D; d += 2.5D) {
            Vec3 p = mouthPos.add(dir.scale(d));
            sl.sendParticles(plasma, p.x, p.y, p.z, 25, 1.8D, 1.8D, 1.8D, 0.08D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 12, 1.2D, 1.2D, 1.2D, 0.1D);
            sl.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);

            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 5, p.y - 5, p.z - 5, p.x + 5, p.y + 5, p.z + 5), e -> e != this && e.isAlive());
            for (LivingEntity v : victims) {
                // 50% Sát thương Max HP cho người chơi & phá tan mọi kết giới
                float dmg = v instanceof Player ? (v.getMaxHealth() * 0.50F) : 3500.0F;
                v.addTag("UrielBypassDamage");
                v.hurt(this.damageSources().magic(), dmg);
                v.removeTag("UrielBypassDamage");
            }
        }
    }

    private void castCrystalFeatherBarrage(ServerLevel sl, LivingEntity target) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.5F, 1.5F);
        for (int i = 0; i < 8; i++) {
            Vec3 p = target.position().add((this.random.nextDouble() - 0.5D) * 8.0D, 0, (this.random.nextDouble() - 0.5D) * 8.0D);
            sl.sendParticles(ParticleTypes.SOUL, p.x, p.y + 0.5D, p.z, 15, 0.4, 0.8, 0.4, 0.1);
            target.hurt(this.damageSources().mobAttack(this), 65.0F);
        }
    }

    private void castDarkDragonAura(ServerLevel sl) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 3.0F, 0.8F);
        List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(30.0D), e -> e != this && e.isAlive());
        for (LivingEntity e : enemies) {
            e.removeEffect(MobEffects.REGENERATION);
            e.removeEffect(MobEffects.MOVEMENT_SPEED);
            e.removeEffect(MobEffects.DAMAGE_BOOST);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
        }
    }

    private void castVoidRoar(ServerLevel sl) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.5F, 0.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.5F, 0.7F);
        for (int i = 0; i < 30; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2.0D;
            double r = 3.0D + this.random.nextDouble() * 15.0D;
            sl.sendParticles(ParticleTypes.SONIC_BOOM, this.getX() + Math.cos(ang) * r, this.getY() + 2.0D, this.getZ() + Math.sin(ang) * r, 1, 0, 0, 0, 0);
        }
        List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(35.0D), e -> e != this && e.isAlive());
        for (LivingEntity e : enemies) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            Vec3 knock = e.position().subtract(this.position()).normalize().scale(1.8D);
            e.push(knock.x, 0.4D, knock.z);
            e.hurtMarked = true;
        }
    }

    private void castAbyssalStomp(ServerLevel sl) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 3.5F, 0.6F);
        DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.6F, 0.0F, 0.8F), 2.5F);
        sl.sendParticles(purple, this.getX(), this.getY() + 0.5D, this.getZ(), 80, 8.0D, 0.5D, 8.0D, 0.1D);

        List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(15.0D), e -> e != this && e.isAlive());
        for (LivingEntity e : enemies) {
            e.hurt(this.damageSources().mobAttack(this), 120.0F);
            e.setDeltaMovement(0, 1.25D, 0);
        }
    }

    private void castVoidClaw(ServerLevel sl, LivingEntity target) {
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.5F, 0.7F);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0D, target.getZ(), 5, 0.5, 0.5, 0.5, 0.05);
        target.hurt(this.damageSources().mobAttack(this), 180.0F);
    }

    private void castMagiculeCorrosiveMist(ServerLevel sl) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 2.5F, 0.8F);
        sl.sendParticles(ParticleTypes.EFFECT, this.getX(), this.getY() + 2.0D, this.getZ(), 100, 12.0D, 2.0D, 12.0D, 0.05D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return super.hurt(source, amount);
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return super.hurt(source, amount);

        // TỬ HUYỆT DUY NHẤT: BỊ KẾT LIỄU BỞI LONG TINH BẠO VIÊM PHÁ (DRAGON NOVA)
        if (source.getMsgId().contains("dragon_nova") || source.getEntity() != null && source.getEntity().getTags().contains("CastingDragonNova")) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5.0F, 0.5F);
            return super.hurt(source, 99999.0F); // One-shot tiêu diệt
        }

        // HƯ KHÔNG THẦN AZATHOTH: KHÔNG THỂ 1 PHÁT TIÊU DIỆT, CHỈ GÂY TỐI ĐA 40% MÁU
        boolean isAzathoth = source.getMsgId().contains("azathoth") || source.getMsgId().contains("nihility");
        if (isAzathoth) {
            float maxDamage = this.getMaxHealth() * 0.40F;
            float cappedDamage = Math.min(amount, maxDamage);

            if (source.getEntity() instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("§c§l« CẢNH BÁO RAPHAEL »\n§eThể ma tố của Hỗn Độn Long chứa nghịch lý không gian hỗn mang, Thần Hư Không Azathoth không thể thôn phệ toàn bộ bản thể! Tối đa chỉ có thể hấp thụ 40% sinh lực mục tiêu!"),
                        false
                );
            }
            return super.hurt(source, cappedDamage);
        }

        // Miễn nhiễm lực hút / sát thương Siêu Tân Tinh (Supernova)
        if (source.getMsgId().contains("supernova")) {
            return false;
        }

        // Phản chấn hỗn mang: 50% sát thương đòn đánh tầm xa thông thường phản lại người bắn
        if (source.is(DamageTypes.ARROW) || source.is(DamageTypes.MOB_PROJECTILE)) {
            if (source.getEntity() instanceof LivingEntity shooter) {
                shooter.hurt(this.damageSources().thorns(this), amount * 0.50F);
            }
            return super.hurt(source, amount * 0.50F);
        }

        return super.hurt(source, amount);
    }
}
