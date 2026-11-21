package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.tensura.MobMeleeSkillSystem;
import com.minhphuc.weapons.content.tensura.SkillNullificationAbility;
import com.minhphuc.weapons.content.tensura.TensuraSkillHelper;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss Bậc 2: Thánh Kỵ Sĩ Đội Trưởng Hinata Sakaguchi.
 * - Bộ 16 kỹ năng Kiếm Thuật & Thể Thuật CQC + 1 đòn đánh tay Rapier cơ bản (= 17), toàn bộ render 3D OpenGL.
 * - Ma pháp đặc trưng giới hạn: Linh Tử Băng Hoại (tụ lực) & Vận Tốc Suy Nghĩ Phase 2 (< 50% HP).
 */
public class HinataEntity extends PathfinderMob {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§f§l[THÁNH KỴ SĨ ĐỘI TRƯỞNG] §b§lHINATA SAKAGUCHI §7- §e[Vận Tốc Suy Nghĩ]"),
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.NOTCHED_12
    );

    private int cqcCooldown = 25;
    private int moveCursor = 0;
    private int parryTicks = 0;
    private int disintegrationChargeTicks = 0;
    private int disintegrationCooldown = 260;
    private int rainbowStacks = 0;
    private boolean phase2Activated = false;

    public HinataEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 1800;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.ATTACK_DAMAGE, 435.0D)
                .add(Attributes.ARMOR, 42.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 28.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.90D)
                .add(Attributes.FOLLOW_RANGE, 56.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, VeldoraEntity.class, 5, false, false, v -> v instanceof VeldoraEntity vd && vd.isAlive() && !vd.isSealed()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnData) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HINATA_SWORD.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setCustomName(Component.literal("§f§lHinata Sakaguchi"));
        this.setCustomNameVisible(true);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            BlockPos pos = this.blockPosition();
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;
                if (!sl.getEntitiesOfClass(HinataEntity.class, new AABB(pos).inflate(140.0D)).isEmpty()) return false;
            }
        }
        return level.getDifficulty() != Difficulty.PEACEFUL && super.checkSpawnRules(level, spawnType);
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
    public boolean removeWhenFarAway(double distSq) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel sl)) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.isAlive());

        if (this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HINATA_SWORD.get()));
        }

        if (parryTicks > 0) parryTicks--;
        if (cqcCooldown > 0) cqcCooldown--;
        if (disintegrationCooldown > 0) disintegrationCooldown--;

        // Phase 2: Vận Tốc Suy Nghĩ khi HP < 50%
        if (!phase2Activated && this.getHealth() <= this.getMaxHealth() * 0.50F) {
            phase2Activated = true;
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 1, false, false));
            sendHinataThoughtAccel(sl);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.5F);
            for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0D))) {
                sp.displayClientMessage(Component.literal("§f§l[HINATA SAKAGUCHI] §eKích hoạt §bVận Tốc Suy Nghĩ§e — Đừng hòng theo kịp đường kiếm của ta!"), false);
            }
        }

        if (SkillNullificationAbility.isEntitySkillSealed(this)) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        // Tụ lực Linh Tử Băng Hoại (Ma pháp duy nhất cast chậm)
        if (disintegrationChargeTicks > 0) {
            disintegrationChargeTicks--;
            sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 4, 0.7, 0.7, 0.7, 0.04);
            if (disintegrationChargeTicks == 0) {
                castDisintegration(sl, target);
            }
            return;
        }

        double distSq = this.distanceToSqr(target);
        if (disintegrationCooldown <= 0 && distSq >= 64.0D && distSq <= 484.0D && sl.random.nextFloat() < 0.25F) {
            disintegrationChargeTicks = 32;
            disintegrationCooldown = 340;
            sendHinataThoughtAccel(sl);
            for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0D))) {
                sp.displayClientMessage(Component.literal("§e[HINATA] §fĐang tụ lực §6Linh Tử Băng Hoại (Disintegration)§f — Hãy né ngay!"), true);
            }
            return;
        }

        // Luân phiên 16 Chiêu Kiếm Thuật & Thể Thuật 3D
        if (cqcCooldown <= 0 && distSq <= 14.0D * 14.0D) {
            executeHinata16Skill(sl, target);
            cqcCooldown = phase2Activated ? 18 : 26;
        }
    }

    /**
     * Đòn đánh tay Rapier cơ bản (Chiêu thứ 17 cùng với 16 kỹ năng CQC).
     */
    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity victim && this.level() instanceof ServerLevel sl) {
            Vec3 dir = victim.position().subtract(this.position()).normalize();
            MobMeleeSkillSystem.sendCqc3d(sl, this, victim.position(), dir, 0, 0, 1.3F, 12);
            incrementRainbowAndBurst(sl, victim, dir);
        }
        return hit;
    }

    private void executeHinata16Skill(ServerLevel sl, LivingEntity target) {
        int move = moveCursor;
        moveCursor = (moveCursor + 1) % 16;

        Vec3 dir = target.position().add(0, 1.0, 0).subtract(this.position().add(0, 1.0, 0));
        if (dir.lengthSqr() < 1e-4) dir = this.getLookAngle();
        dir = dir.normalize();
        float atk = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        switch (move) {
            case 0 -> { // 1. Tam Liên Thích (Triple Thrust)
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 1, 0, 1.5F, 16);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.6F, 1.4F);
                strike(sl, target, atk * 1.15F, dir.scale(0.5D), 0.1D, false);
            }
            case 1 -> { // 2. Ảo Bộ Trượt (Flash Step Sidestep)
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 9, 0, 1.4F, 16);
                Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize().scale(sl.random.nextBoolean() ? 3.5D : -3.5D);
                this.teleportTo(target.getX() + side.x * 0.65D, target.getY(), target.getZ() + side.z * 0.65D);
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir.scale(-1), 0, 0, 1.35F, 14);
                strike(sl, target, atk * 0.95F, dir.scale(0.4D), 0.1D, false);
            }
            case 2 -> { // 3. Hoành Tảo Kiếm Khí (Crescent Arc 160)
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 0, 0, 1.9F, 16);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.8F, 1.3F);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.5D), x -> TensuraSkillHelper.isValidSkillTarget(this, x))) {
                    strike(sl, e, atk * 1.05F, dir.scale(0.75D), 0.2D, false);
                }
            }
            case 3 -> { // 4. Đâm Điểm Yếu (Vital Point 60% Armor Pierce)
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 1, 1, 1.6F, 16);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                strike(sl, target, atk * 1.30F, dir.scale(0.4D), 0.1D, true);
            }
            case 4 -> { // 5. Phản Kiếm Tước Khiên (Parry Stance 0.8s)
                parryTicks = 16;
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 6, 1, 1.5F, 18);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.2F, 1.7F);
            }
            case 5 -> { // 6. Thúc Chỏ & Chuôi Kiếm (Pommel & Elbow Stun)
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 2, 1, 1.2F, 14);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 4));
                strike(sl, target, atk * 0.90F, dir.scale(0.6D), 0.15D, false);
            }
            case 6 -> { // 7. Thượng Tiêu Trảm (Rising Sky Slash)
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 7, 0, 1.6F, 18);
                strike(sl, target, atk * 1.15F, new Vec3(dir.x * 0.3, 0.9D, dir.z * 0.3), 0.9D, false);
            }
            case 7 -> { // 8. Không Trung Liên Đâm (Aerial Dive Pierce)
                this.setDeltaMovement(dir.x * 1.2D, (target.getY() > this.getY() ? 0.8D : 0.2D), dir.z * 1.2D);
                this.hurtMarked = true;
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 8, 0, 1.6F, 18);
                strike(sl, target, atk * 1.25F, dir.scale(0.8D), -0.2D, false);
            }
            case 8 -> { // 9. Đá Quét Hạ Bộ (Low Sweep Kick)
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 5, 1, 1.35F, 15);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 3));
                strike(sl, target, atk * 0.95F, dir.scale(0.85D), 0.4D, false);
            }
            case 9 -> { // 10. Kiếm Vũ 5 Nhát (Blade Waltz)
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 3, 0, 1.7F, 18);
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 0, 1, 1.7F, 18);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.8F, 1.5F);
                strike(sl, target, atk * 1.35F, dir.scale(0.5D), 0.2D, false);
            }
            case 10 -> { // 11. Gót Chân Phá Thế (Axe Kick Guard Break)
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), dir, 2, 1, 1.5F, 18);
                if (target instanceof Player p) p.disableShield();
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.8F, 1.0F);
                strike(sl, target, atk * 1.25F, dir.scale(0.7D), 0.1D, true);
            }
            case 11 -> { // 12. Xung Kích Trường Kiếm (9m Lunging Dash)
                this.setDeltaMovement(dir.scale(1.85D).add(0, 0.15D, 0));
                this.hurtMarked = true;
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 4, 0, 1.8F, 18);
                strike(sl, target, atk * 1.30F, dir.scale(1.1D), 0.25D, false);
            }
            case 12 -> { // 13. Đoạn Hồn Đột Kích (Ghost Step Backstab)
                Vec3 back = target.position().subtract(target.getLookAngle().normalize().scale(2.0D));
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 9, 5, 1.5F, 18);
                this.teleportTo(back.x, target.getY(), back.z);
                MobMeleeSkillSystem.sendCqc3d(sl, this, target.position(), target.getLookAngle(), 3, 5, 1.5F, 16);
                strike(sl, target, atk * 1.40F, target.getLookAngle().scale(0.7D), 0.15D, true);
            }
            case 13 -> { // 14. Hỏa Hoa Kiếm Bích (Whirling Deflect)
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 5, 0, 1.8F, 18);
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 6, 0, 1.5F, 18);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.2D), x -> TensuraSkillHelper.isValidSkillTarget(this, x))) {
                    Vec3 away = e.position().subtract(this.position()).normalize();
                    strike(sl, e, atk * 0.95F, away.scale(1.3D), 0.35D, false);
                }
            }
            case 14 -> { // 15. Thất Thải Chuỗi 6 Nhát (6-Hit Rainbow Prep)
                sendHinataThoughtAccel(sl);
                MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 1, 0, 1.85F, 20);
                rainbowStacks = Math.min(6, rainbowStacks + 3);
                strike(sl, target, atk * 1.45F, dir.scale(0.5D), 0.2D, true);
            }
            default -> { // 16. Thất Thải Chung Yên - Dead End Rainbow!
                rainbowStacks = 7;
                triggerDeadEndRainbow(sl, target, dir);
            }
        }
    }

    private void incrementRainbowAndBurst(ServerLevel sl, LivingEntity victim, Vec3 dir) {
        rainbowStacks++;
        if (rainbowStacks >= 7) {
            triggerDeadEndRainbow(sl, victim, dir);
        }
    }

    private void triggerDeadEndRainbow(ServerLevel sl, LivingEntity victim, Vec3 dir) {
        rainbowStacks = 0;
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DEAD_END_RAINBOW)) {
            ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DEAD_END_RAINBOW, this.getUUID(), victim.position(), dir, 26,
                    ClientboundCinematicSkillPacket.ACTION_START, 1.4F));
        }
        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 2.2F, 1.45F);
        // Xóa 1 buff tích cực & chém xuyên giáp/Uriel
        victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        strike(sl, victim, 780.0F, dir.scale(0.9D), 0.45D, true);
        if (victim instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("§d§l[DEAD END RAINBOW] §fHinata đã tích đủ 7 Sắc Cầu Vồng — Đòn kết liễu xuyên linh hồn!"), true);
        }
    }

    private void castDisintegration(ServerLevel sl, LivingEntity target) {
        Vec3 pos = target.position();
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DISINTEGRATION)) {
            ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DISINTEGRATION, this.getUUID(), pos, new Vec3(0, 1, 0), 70,
                    ClientboundCinematicSkillPacket.ACTION_START, 0.85F));
        }
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5F, 1.4F);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(7.5D), x -> TensuraSkillHelper.isValidSkillTarget(this, x))) {
            e.addTag("DisintegrationDamage");
            try {
                e.hurt(this.damageSources().indirectMagic(this, this), 660.0F);
            } finally {
                e.removeTag("DisintegrationDamage");
            }
        }
    }

    private void strike(ServerLevel sl, LivingEntity victim, float dmg, Vec3 knock, double yBoost, boolean pierce) {
        if (victim == null || !victim.isAlive()) return;
        victim.invulnerableTime = 0;
        if (pierce) {
            victim.addTag("ChronoaPenetrationDamage");
            try {
                victim.hurt(this.damageSources().indirectMagic(this, this), dmg);
            } finally {
                victim.removeTag("ChronoaPenetrationDamage");
            }
        } else {
            victim.hurt(this.damageSources().mobAttack(this), dmg);
        }
        victim.push(knock.x, yBoost, knock.z);
        victim.hurtMarked = true;
    }

    private void sendHinataThoughtAccel(ServerLevel sl) {
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_HINATA_THOUGHT_ACCEL)) {
            ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_HINATA_THOUGHT_ACCEL, this.getUUID(), this.position(), this.getLookAngle(), 22,
                    ClientboundCinematicSkillPacket.ACTION_START, 1.3F));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)) return false;

        if (parryTicks > 0 && source.getEntity() instanceof LivingEntity attacker && attacker != this && this.level() instanceof ServerLevel sl) {
            parryTicks = 0;
            Vec3 dir = attacker.position().subtract(this.position()).normalize();
            MobMeleeSkillSystem.sendCqc3d(sl, this, this.position(), dir, 6, 1, 1.8F, 18);
            MobMeleeSkillSystem.sendCqc3d(sl, this, attacker.position(), dir, 0, 1, 1.8F, 16);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0F, 1.5F);
            strike(sl, attacker, 135.0F, dir.scale(1.2D), 0.35D, true);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel sl, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, source, recentlyHit);
        List<ItemStack> armorPool = new ArrayList<>();
        armorPool.add(new ItemStack(ModItems.HINATA_HELMET.get()));
        armorPool.add(new ItemStack(ModItems.HINATA_CHESTPLATE.get()));
        armorPool.add(new ItemStack(ModItems.HINATA_LEGGINGS.get()));
        armorPool.add(new ItemStack(ModItems.HINATA_BOOTS.get()));
        ItemStack dropArmor = armorPool.get(sl.random.nextInt(armorPool.size()));
        sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.6D, this.getZ(), dropArmor));
        if (sl.random.nextFloat() < 0.65F) {
            sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.8D, this.getZ(), new ItemStack(ModItems.HINATA_SWORD.get())));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RainbowStacks", rainbowStacks);
        tag.putBoolean("Phase2", phase2Activated);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rainbowStacks = tag.getInt("RainbowStacks");
        phase2Activated = tag.getBoolean("Phase2");
    }
}
