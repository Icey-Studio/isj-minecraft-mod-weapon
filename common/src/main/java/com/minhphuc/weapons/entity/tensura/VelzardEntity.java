package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.divine.JacobsLadderAbility;
import com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility;
import com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager;
import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import com.minhphuc.weapons.content.tensura.TensuraDialogueManager;
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
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * Bạch Băng Long Velzard (White Ice Dragon Velzard / Velzado)
 * Long Chủng Tối Thượng Cổ Đại (Tensura LN/Anime):
 * - Giáng thế kèm Bão Tuyết Cực Hàn (Glacial Blizzard) kéo dài 30 giây (600 ticks).
 * - Hoàn toàn không bị ảnh hưởng bởi Kết Giới Kháng Ma (Anti-Magic Barrier).
 * - Đại Tuyệt Kỹ Hơi Thở Băng Long / Bão Tuyết Thâm Uyên công phá vỡ nát Đa Trùng Kết Giới.
 * - Khả năng bay lượn tự do trên không trung (Flying AI).
 * - 10 Tuyệt Kỹ Độc Quyền với cơ chế báo hiệu (Telegraphing) rõ ràng cho người chơi né tránh.
 * - Tối ưu hóa hiệu năng chống giật lag (Particle throttling, distanceToSqr, sound cooldowns).
 */
public class VelzardEntity extends Monster {

    public static final EntityDataAccessor<Integer> DATA_CASTING_STATE =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_SIGIL_COUNT =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_FLYING =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_ENRAGED =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.BOOLEAN);

    // Casting states
    public static final int CAST_NONE = 0;
    public static final int CAST_SNOW_CRYSTAL = 1;      // Băng Tinh Bất Hoại (Gabriel)
    public static final int CAST_KINETIC_CESSATION = 2; // Ngưng Trệ Động Năng (Cthulhu)
    public static final int CAST_ICICLE_GATLING = 3;    // Cụm Gai Băng Kim Cương
    public static final int CAST_DRAGON_BREATH = 4;     // Hơi Thở Bạch Băng Long
    public static final int CAST_GLACIAL_PRISON = 5;    // Lãnh Ngục Địa Tinh

    private final ServerBossEvent bossEvent;

    // Cooldowns
    private int snowCrystalCooldown = 300;     // 15s
    private int kineticCessationCooldown = 240;// 12s
    private int icicleGatlingCooldown = 140;   // 7s
    private int dragonBreathCooldown = 360;    // 18s
    private int glacialPrisonCooldown = 200;   // 10s
    private int frostBloomCooldown = 180;      // 9s
    private int mirrorDecoyCooldown = 0;       // 45s cooldown after use

    // Active casting timers
    private int activeSkillTicks = 0;
    private int currentCastingType = CAST_NONE;
    private Vec3 castingTargetPos = null;

    // Blizzard on spawn timer (30s = 600 ticks)
    private int blizzardTicksRemaining = 600;

    // Stagger / Kiệt sức mechanism (khi vỡ 3 ấn ký hoa tuyết)
    private float damageTakenForSigil = 0.0F;
    private int staggerTicks = 0;

    // Permafrost standing trackers (UUID -> standing ticks)
    private final Map<UUID, Integer> standingTickCounters = new HashMap<>();

    // Particle mộng ảo sương tuyết
    private static final DustParticleOptions FROST_CYAN =
            new DustParticleOptions(new Vector3f(0.4F, 0.9F, 1.0F), 1.6F);
    private static final DustParticleOptions FROST_WHITE =
            new DustParticleOptions(new Vector3f(0.95F, 0.98F, 1.0F), 1.8F);

    public VelzardEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§b§l[BẠCH BĂNG LONG] §f§lVELZARD §7- §9[Nữ Hoàng Băng Giá Tối Thượng]"),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        this.moveControl = new FlyingMoveControl(this, 18, true);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.40D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.ATTACK_DAMAGE, 50.0D)
                .add(Attributes.ARMOR, 35.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 25.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING_STATE, CAST_NONE);
        builder.define(DATA_SIGIL_COUNT, 3);
        builder.define(DATA_IS_FLYING, false);
        builder.define(DATA_IS_ENRAGED, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new VelzardCombatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getCastingState() {
        return this.entityData.get(DATA_CASTING_STATE);
    }

    public void setCastingState(int state) {
        this.entityData.set(DATA_CASTING_STATE, state);
    }

    public int getSigilCount() {
        return this.entityData.get(DATA_SIGIL_COUNT);
    }

    public void setSigilCount(int count) {
        this.entityData.set(DATA_SIGIL_COUNT, Math.max(0, Math.min(3, count)));
    }

    public boolean isFlyingAnim() {
        return this.entityData.get(DATA_IS_FLYING);
    }

    public boolean isFlying() {
        return isFlyingAnim();
    }

    public void setFlyingAnim(boolean flying) {
        this.entityData.set(DATA_IS_FLYING, flying);
    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_IS_ENRAGED);
    }

    public boolean isAwakened() {
        return isEnraged();
    }

    public boolean isCastingBreath() {
        return this.entityData.get(DATA_CASTING_STATE) == CAST_DRAGON_BREATH;
    }

    public void setEnraged(boolean enraged) {
        this.entityData.set(DATA_IS_ENRAGED, enraged);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;
                // Kiểm tra phạm vi 128 blocks nếu đã có Velzard thì không spawn thêm để chống lag
                AABB checkArea = new AABB(this.blockPosition()).inflate(128.0D);
                if (!sl.getEntitiesOfClass(VelzardEntity.class, checkArea).isEmpty()) {
                    return false;
                }
                // Chỉ xuất hiện ở các quần xã lạnh / tuyết
                var biome = sl.getBiome(this.blockPosition());
                if (!biome.value().coldEnoughToSnow(this.blockPosition())) {
                    return false;
                }
            }
        }
        return super.checkSpawnRules(level, spawnType);
    }

    public void broadcastDialogue(String text) {
        if (this.level() instanceof ServerLevel sl) {
            Component msg = Component.literal("§b§l[BẠCH BĂNG LONG VELZARD] §f" + text);
            AABB notifyBox = this.getBoundingBox().inflate(64.0D);
            for (ServerPlayer player : sl.getEntitiesOfClass(ServerPlayer.class, notifyBox)) {
                player.displayClientMessage(msg, false);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;

        // Miễn nhiễm sát thương môi trường lạnh và rơi
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FREEZE) ||
            source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL) ||
            source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return false;
        }

        // BĂNG TINH BẤT HOẠI (SNOW CRYSTAL - GABRIEL): Kháng 100% khi đang bật khiên
        if (getCastingState() == CAST_SNOW_CRYSTAL) {
            // Bị phá vỡ nếu trúng Disintegration hoặc Jacob's Ladder
            boolean isDisintegration = source.is(DamageTypes.MAGIC) && amount >= 500.0F;
            if (!isDisintegration) {
                // Phản hồi 20% sát thương cận chiến thành sát thương Băng
                if (source.getEntity() instanceof LivingEntity attacker && this.distanceToSqr(attacker) <= 16.0D) {
                    attacker.hurt(this.damageSources().magic(), amount * 0.20F);
                    attacker.setTicksFrozen(attacker.getTicksFrozen() + 60);
                }

                ServerLevel sl = (ServerLevel) this.level();
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.5F, 1.4F);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0D, this.getZ(), 8, 0.4D, 0.4D, 0.4D, 0.05D);
                return false;
            } else {
                // Bị phá khiên
                this.setCastingState(CAST_NONE);
                this.activeSkillTicks = 0;
                broadcastDialogue("Phòng ngự tuyệt đối bị xé rách?! Không thể nào!");
            }
        }

        // THẾ THÂN GƯƠNG BĂNG (MIRROR ICE DECOY): Dưới 50% HP, khi nhận đòn chí mạng
        if (this.getHealth() <= this.getMaxHealth() * 0.5F && mirrorDecoyCooldown <= 0 && amount >= 20.0F) {
            triggerMirrorDecoy((ServerLevel) this.level());
            return false;
        }

        // TAM TẦNG BĂNG HOA ẤN KÝ (TRI-FROST SIGILS): Giảm sát thương nhận vào
        int sigils = getSigilCount();
        float damageReduction = sigils * 0.15F;
        float finalAmount = amount * (1.0F - damageReduction);

        // Tích lũy sát thương để phá vỡ từng tầng ấn ký
        this.damageTakenForSigil += finalAmount;
        if (this.damageTakenForSigil >= 1200.0F && sigils > 0) {
            this.damageTakenForSigil = 0.0F;
            setSigilCount(sigils - 1);
            ServerLevel sl = (ServerLevel) this.level();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.9F);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.1D);

            if (getSigilCount() == 0) {
                // Toàn bộ 3 ấn ký bị phá -> Kiệt Sức (Stagger) 3.5s (70 ticks)
                this.staggerTicks = 70;
                this.setDeltaMovement(0, -0.4D, 0);
                broadcastDialogue("Ấn ký băng tuyết đã vỡ... Hự!");
            }
        }

        return super.hurt(source, finalAmount);
    }

    private void triggerMirrorDecoy(ServerLevel sl) {
        this.mirrorDecoyCooldown = 900; // 45s cooldown
        Vec3 curPos = this.position();

        sl.playSound(null, curPos.x, curPos.y, curPos.z,
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 4.0F, 1.2F);
        sl.playSound(null, curPos.x, curPos.y, curPos.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 3.0F, 1.0F);

        // Hạt gương vỡ
        sl.sendParticles(ParticleTypes.SNOWFLAKE, curPos.x, curPos.y + 1.0D, curPos.z, 50, 0.8D, 1.0D, 0.8D, 0.15D);
        sl.sendParticles(ParticleTypes.FLASH, curPos.x, curPos.y + 1.0D, curPos.z, 1, 0, 0, 0, 0);

        // Dịch chuyển lùi lại 12 blocks
        Vec3 backward = this.getLookAngle().reverse().scale(12.0D);
        this.moveTo(curPos.x + backward.x, curPos.y + 2.0D, curPos.z + backward.z);
        this.setDeltaMovement(0, 0.2D, 0);

        // Kích nổ tượng băng sau 1 giây (được xử lý thông qua đòn nổ tại vị trí cũ)
        sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 20, () -> {
            sl.playSound(null, curPos.x, curPos.y, curPos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 1.2F);
            sl.sendParticles(ParticleTypes.EXPLOSION, curPos.x, curPos.y + 1.0D, curPos.z, 2, 0.5D, 0.5D, 0.5D, 0);
            AABB decoyBox = new AABB(curPos.x - 4.0D, curPos.y - 1.0D, curPos.z - 4.0D,
                    curPos.x + 4.0D, curPos.y + 4.0D, curPos.z + 4.0D);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, decoyBox, e -> e != this && e.isAlive());
            for (LivingEntity v : victims) {
                v.hurt(sl.damageSources().mobAttack(this), 35.0F);
                v.setTicksFrozen(v.getTicksFrozen() + 100);
            }
        }));

        broadcastDialogue("Ngươi chỉ đang chém vào ảo ảnh băng tuyết của ta mà thôi!");
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Client particles nhịp nhàng
            if (this.tickCount % 4 == 0) {
                this.level().addParticle(FROST_CYAN,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        this.getY() + this.random.nextDouble() * 2.0D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        0, 0.02D, 0);
            }
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();

        // 1. Cập nhật BossBar
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.getTarget() != null);

        // 2. BÃO TUYẾT CỰC HÀN KHI XUẤT HIỆN (30 GIÂY = 600 TICKS)
        if (blizzardTicksRemaining > 0) {
            blizzardTicksRemaining--;
            tickGlacialBlizzard(sl);
        }

        // 3. Xử lý Stagger (Kiệt Sức)
        if (staggerTicks > 0) {
            staggerTicks--;
            this.setDeltaMovement(0, -0.05D, 0);
            if (this.tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5D, this.getZ(), 5, 0.4D, 0.4D, 0.4D, 0.05D);
            }
            if (staggerTicks == 0) {
                // Hồi phục lại 3 ấn ký
                setSigilCount(3);
                broadcastDialogue("Ấn ký băng hoa đã tái sinh... Trận chiến chỉ vừa mới bắt đầu!");
            }
            return; // Đang kiệt sức không xuất chiêu
        }

        // 4. Quản lý trạng thái Cuồng Nộ (Phase 2 Awakening < 35% HP)
        boolean enraged = this.getHealth() <= (this.getMaxHealth() * 0.35F);
        if (enraged != isEnraged()) {
            setEnraged(enraged);
            if (enraged) {
                this.bossEvent.setColor(BossEvent.BossBarColor.WHITE);
                broadcastDialogue("Băng Thần Giáng Lâm... Tuyệt Vọng Tuyệt Đối sẽ nhấn chìm thế giới này!");
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 1.2F);
            }
        }

        // 5. Cooldowns tick
        if (mirrorDecoyCooldown > 0) mirrorDecoyCooldown--;
        if (snowCrystalCooldown > 0) snowCrystalCooldown--;
        if (kineticCessationCooldown > 0) kineticCessationCooldown--;
        if (icicleGatlingCooldown > 0) icicleGatlingCooldown--;
        if (dragonBreathCooldown > 0) dragonBreathCooldown--;
        if (glacialPrisonCooldown > 0) glacialPrisonCooldown--;
        if (frostBloomCooldown > 0) frostBloomCooldown--;

        // 6. Mặt Đất Băng Vĩnh Cửu (Permafrost Field Aura - Bán kính 16m)
        if (this.tickCount % 20 == 0) {
            tickPermafrostAura(sl);
        }

        // 7. Quản lý kỹ năng đang thi triển
        if (activeSkillTicks > 0) {
            tickActiveSkill(sl);
        } else {
            // Kiểm tra thi triển chiêu thức khi có mục tiêu
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                setFlyingAnim(true);
                this.setNoGravity(true);

                // Giữ độ cao lơ lửng 3-5m so với mục tiêu
                double desiredY = target.getY() + 3.5D;
                if (this.getY() < desiredY) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04D, 0));
                }

                double distSq = this.distanceToSqr(target);
                chooseSkillToCast(sl, target, distSq);
            } else {
                setFlyingAnim(false);
                this.setNoGravity(false);
            }
        }
    }

    private void tickGlacialBlizzard(ServerLevel sl) {
        // Sinh hạt bão tuyết tiết kiệm tài nguyên (Throttling: mỗi 3 ticks sinh 4 hạt)
        if (this.tickCount % 3 == 0) {
            for (int i = 0; i < 4; i++) {
                double rx = this.getX() + (this.random.nextDouble() - 0.5D) * 36.0D;
                double rz = this.getZ() + (this.random.nextDouble() - 0.5D) * 36.0D;
                double ry = this.getY() + 2.0D + this.random.nextDouble() * 10.0D;
                sl.sendParticles(ParticleTypes.SNOWFLAKE, rx, ry, rz, 1, -0.3D, -0.4D, -0.3D, 0.05D);
            }
        }

        // Âm thanh gió tuyết gầm rú mỗi 40 ticks
        if (this.tickCount % 40 == 0) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.WEATHER, 2.0F, 0.8F);
        }

        // Gây hạ thân nhiệt cho thực thể xung quanh (mỗi 20 ticks)
        if (this.tickCount % 20 == 0) {
            AABB blizzardBox = this.getBoundingBox().inflate(28.0D);
            List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, blizzardBox, e -> e != this && e.isAlive());
            for (LivingEntity e : list) {
                if (!(e instanceof VelzardEntity) && !(e instanceof VelgryndEntity)) {
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 60, e.getTicksFrozen() + 40));
                }
            }
        }
    }

    private void tickPermafrostAura(ServerLevel sl) {
        AABB box = this.getBoundingBox().inflate(16.0D);
        List<Player> nearbyPlayers = sl.getEntitiesOfClass(Player.class, box, Player::isAlive);

        for (Player p : nearbyPlayers) {
            Vec3 motion = p.getDeltaMovement();
            boolean isMoving = (motion.x * motion.x + motion.z * motion.z) > 0.002D;

            UUID pid = p.getUUID();
            int standing = standingTickCounters.getOrDefault(pid, 0);

            if (!isMoving) {
                standing++;
                standingTickCounters.put(pid, standing);

                // 10 tầng đứng yên (10 giây) -> Đóng băng 2 giây
                if (standing >= 10) {
                    standingTickCounters.put(pid, 0);
                    p.setTicksFrozen(p.getTicksRequiredToFreeze() + 100);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false));
                    p.displayClientMessage(Component.literal("§b§l[LÃNH KHÍ] §cBạn đã bị đóng băng do đứng yên trong Lãnh Vực Vĩnh Cửu!"), true);
                } else if (standing >= 5) {
                    p.displayClientMessage(Component.literal("§b§l[LÃNH KHÍ] §eHàn khí đang ngưng tụ! Hãy liên tục di chuyển (" + standing + "/10)!"), true);
                }
            } else {
                // Di chuyển giải tỏa hàn khí
                if (standing > 0) {
                    standingTickCounters.put(pid, Math.max(0, standing - 2));
                }
            }
        }
    }

    private void chooseSkillToCast(ServerLevel sl, LivingEntity target, double distSq) {
        int rate = isEnraged() ? 2 : 1;

        // 1. Hơi Thở Bạch Băng Long (Frost Dragon Breath)
        if (dragonBreathCooldown <= 0 && distSq <= 28.0D * 28.0D) {
            startSkill(CAST_DRAGON_BREATH, 40, target.position()); // 2s channeling
            dragonBreathCooldown = 360 / rate;
            return;
        }

        // 2. Ngưng Trệ Động Năng (Kinetic Cessation - Cthulhu)
        if (kineticCessationCooldown <= 0 && distSq <= 20.0D * 20.0D) {
            startSkill(CAST_KINETIC_CESSATION, 30, target.position()); // 1.5s telegraph
            kineticCessationCooldown = 240 / rate;
            return;
        }

        // 3. Lãnh Ngục Địa Tinh (Glacial Prison Trap)
        if (glacialPrisonCooldown <= 0 && distSq <= 24.0D * 24.0D) {
            startSkill(CAST_GLACIAL_PRISON, 25, target.position()); // 1.25s telegraph
            glacialPrisonCooldown = 200 / rate;
            return;
        }

        // 4. Cụm Gai Băng Kim Cương (Diamond Icicle Gatling)
        if (icicleGatlingCooldown <= 0 && distSq <= 30.0D * 30.0D) {
            startSkill(CAST_ICICLE_GATLING, 20, target.position()); // 1s telegraph
            icicleGatlingCooldown = 140 / rate;
            return;
        }

        // 5. Băng Tinh Bất Hoại (Snow Crystal - Gabriel) - Chỉ khi không Enraged
        if (!isEnraged() && snowCrystalCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.8F) {
            startSkill(CAST_SNOW_CRYSTAL, 80, this.position()); // 4s shield
            snowCrystalCooldown = 320;
            return;
        }

        // 6. Phấn Hoa Tuyết Trắng (Whiteout Frost Bloom)
        if (frostBloomCooldown <= 0 && distSq <= 25.0D * 25.0D) {
            castFrostBloom(sl, target);
            frostBloomCooldown = 180 / rate;
        }
    }

    private void startSkill(int skillType, int durationTicks, Vec3 targetPos) {
        this.currentCastingType = skillType;
        this.activeSkillTicks = durationTicks;
        this.castingTargetPos = targetPos;
        this.setCastingState(skillType);

        ServerLevel sl = (ServerLevel) this.level();
        switch (skillType) {
            case CAST_DRAGON_BREATH -> {
                broadcastDialogue("Băng Long Hống... Tuyệt Vọng Tuyệt Đối!");
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 1.4F);
            }
            case CAST_KINETIC_CESSATION -> {
                // Vòng ma pháp xanh lam dưới chân mục tiêu báo trước 1.5s
                if (targetPos != null) {
                    sl.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 3.0F, 1.2F);
                    for (int i = 0; i < 16; i++) {
                        double angle = (i * 22.5D) * Math.PI / 180.0D;
                        double px = targetPos.x + Math.cos(angle) * 3.5D;
                        double pz = targetPos.z + Math.sin(angle) * 3.5D;
                        sl.sendParticles(FROST_CYAN, px, targetPos.y + 0.2D, pz, 1, 0, 0.05D, 0, 0);
                    }
                }
            }
            case CAST_GLACIAL_PRISON -> {
                if (targetPos != null) {
                    sl.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.HOSTILE, 3.0F, 0.6F);
                }
            }
            case CAST_ICICLE_GATLING -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 3.5F, 1.6F);
            }
            case CAST_SNOW_CRYSTAL -> {
                broadcastDialogue("Băng Tinh Vĩnh Cửu — Không một chuyển động nào có thể vượt qua!");
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.5F, 1.6F);
            }
        }
    }

    private void tickActiveSkill(ServerLevel sl) {
        activeSkillTicks--;

        switch (currentCastingType) {
            case CAST_SNOW_CRYSTAL -> {
                // Giữ lơ lửng, tạo vỏ bọc băng tinh
                this.setDeltaMovement(0, 0.01D, 0);
                if (this.tickCount % 4 == 0) {
                    sl.sendParticles(FROST_WHITE, this.getX(), this.getY() + 1.0D, this.getZ(), 8, 1.2D, 1.2D, 1.2D, 0.05D);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0D, this.getZ(), 6, 1.0D, 1.0D, 1.0D, 0.02D);
                }
            }
            case CAST_KINETIC_CESSATION -> {
                // Hiển thị vòng báo trước dưới chân mục tiêu
                if (castingTargetPos != null && activeSkillTicks % 2 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (i * 30.0D) * Math.PI / 180.0D;
                        double px = castingTargetPos.x + Math.cos(angle) * 3.5D;
                        double pz = castingTargetPos.z + Math.sin(angle) * 3.5D;
                        sl.sendParticles(FROST_CYAN, px, castingTargetPos.y + 0.1D, pz, 1, 0, 0.02D, 0, 0);
                    }
                }
                // Kích hoạt khi hết thời gian báo trước
                if (activeSkillTicks == 1 && castingTargetPos != null) {
                    sl.playSound(null, castingTargetPos.x, castingTargetPos.y, castingTargetPos.z,
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 1.6F);
                    sl.sendParticles(ParticleTypes.FLASH, castingTargetPos.x, castingTargetPos.y + 1.0D, castingTargetPos.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, castingTargetPos.x, castingTargetPos.y + 1.0D, castingTargetPos.z, 40, 3.5D, 1.5D, 3.5D, 0.1D);

                    AABB freezeBox = new AABB(castingTargetPos.x - 4.5D, castingTargetPos.y - 1.0D, castingTargetPos.z - 4.5D,
                            castingTargetPos.x + 4.5D, castingTargetPos.y + 4.0D, castingTargetPos.z + 4.5D);
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, freezeBox, e -> e != this && e.isAlive());
                    for (LivingEntity e : list) {
                        e.setDeltaMovement(Vec3.ZERO);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255, false, false));
                        e.setTicksFrozen(e.getTicksRequiredToFreeze() + 120);
                        e.hurt(sl.damageSources().magic(), 45.0F);
                    }
                }
            }
            case CAST_ICICLE_GATLING -> {
                // Bắn liên thanh các mũi gai băng khi hết 1s chuẩn bị
                if (activeSkillTicks <= 10 && activeSkillTicks % 2 == 0) {
                    LivingEntity t = this.getTarget();
                    if (t != null && t.isAlive()) {
                        Vec3 start = this.getEyePosition();
                        Vec3 dir = t.getEyePosition().subtract(start).normalize()
                                .add((this.random.nextDouble() - 0.5D) * 0.15D, (this.random.nextDouble() - 0.5D) * 0.1D, (this.random.nextDouble() - 0.5D) * 0.15D).normalize();

                        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 2.0F, 1.8F);

                        // Quỹ đạo đạn gai băng bay nhanh
                        for (double d = 1.0D; d < 28.0D; d += 1.5D) {
                            Vec3 pt = start.add(dir.scale(d));
                            sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);

                            AABB hitBox = new AABB(pt.x - 1.0D, pt.y - 1.0D, pt.z - 1.0D, pt.x + 1.0D, pt.y + 1.0D, pt.z + 1.0D);
                            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e.isAlive());
                            if (!victims.isEmpty()) {
                                for (LivingEntity v : victims) {
                                    v.hurt(sl.damageSources().mobAttack(this), 24.0F);
                                    v.setTicksFrozen(v.getTicksFrozen() + 40);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            case CAST_GLACIAL_PRISON -> {
                // Cột băng trồi lên nổ
                if (activeSkillTicks == 1 && castingTargetPos != null) {
                    sl.playSound(null, castingTargetPos.x, castingTargetPos.y, castingTargetPos.z,
                            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.5F, 0.7F);
                    sl.sendParticles(ParticleTypes.EXPLOSION, castingTargetPos.x, castingTargetPos.y + 1.0D, castingTargetPos.z, 2, 0.5D, 0.5D, 0.5D, 0);

                    AABB trapBox = new AABB(castingTargetPos.x - 3.0D, castingTargetPos.y - 1.0D, castingTargetPos.z - 3.0D,
                            castingTargetPos.x + 3.0D, castingTargetPos.y + 3.5D, castingTargetPos.z + 3.0D);
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, trapBox, e -> e != this && e.isAlive());
                    for (LivingEntity e : list) {
                        e.hurt(sl.damageSources().magic(), 40.0F);
                        e.setTicksFrozen(e.getTicksFrozen() + 80);
                    }
                }
            }
            case CAST_DRAGON_BREATH -> {
                // Hơi thở băng long quét hình nón 24 blocks & PHÁ HỦY ĐA TRÙNG KẾT GIỚI
                if (activeSkillTicks <= 20) {
                    Vec3 look = this.getLookAngle();
                    Vec3 eye = this.getEyePosition();

                    sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 3.0F, 0.6F);

                    for (double d = 2.0D; d < 24.0D; d += 2.0D) {
                        double spread = d * 0.25D;
                        Vec3 centerPt = eye.add(look.scale(d));

                        sl.sendParticles(ParticleTypes.SNOWFLAKE, centerPt.x, centerPt.y, centerPt.z, 8, spread, spread * 0.5D, spread, 0.08D);
                        sl.sendParticles(FROST_CYAN, centerPt.x, centerPt.y, centerPt.z, 4, spread, spread * 0.5D, spread, 0.05D);

                        // Phá hủy Đa Trùng Kết Giới trong luồng thở
                        MultilayerBarrierAbility.shatterBarrierNear(sl, centerPt, spread + 2.0D, "Hơi Thở Bạch Băng Long Velzard");

                        // Sát thương thực thể
                        AABB coneBox = new AABB(centerPt.x - spread, centerPt.y - spread * 0.5D, centerPt.z - spread,
                                centerPt.x + spread, centerPt.y + spread * 0.5D, centerPt.z + spread);
                        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, coneBox, e -> e != this && e.isAlive());
                        for (LivingEntity victim : list) {
                            victim.hurt(sl.damageSources().magic(), 30.0F);
                            victim.setTicksFrozen(victim.getTicksFrozen() + 60);
                        }
                    }
                }
            }
        }

        if (activeSkillTicks <= 0) {
            this.setCastingState(CAST_NONE);
            this.currentCastingType = CAST_NONE;
            this.castingTargetPos = null;
        }
    }

    private void castFrostBloom(ServerLevel sl, LivingEntity target) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.5F, 1.4F);

        // Triệu hồi 6 hoa tuyết phát sáng bay chậm
        for (int i = 0; i < 6; i++) {
            double angle = (i * 60.0D) * Math.PI / 180.0D;
            double px = this.getX() + Math.cos(angle) * 3.0D;
            double pz = this.getZ() + Math.sin(angle) * 3.0D;
            double py = this.getY() + 1.5D;

            sl.sendParticles(FROST_WHITE, px, py, pz, 10, 0.2D, 0.2D, 0.2D, 0.02D);
            sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        // 1. Linh hồn Bạch Băng Long
        level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY() + 0.5D, this.getZ(),
                new ItemStack(ModItems.VELZARD_SOUL.get())));

        // 2. Lõi Bạch Băng Long
        level.addFreshEntity(new ItemEntity(level, this.getX() + 0.3D, this.getY() + 0.5D, this.getZ(),
                new ItemStack(ModItems.FROST_DRAGON_CORE.get())));

        // 3. Vảy Ngược Bạch Băng Long
        level.addFreshEntity(new ItemEntity(level, this.getX() - 0.3D, this.getY() + 0.5D, this.getZ(),
                new ItemStack(ModItems.VELZARD_REVERSE_SCALE.get())));

        // 4. Băng Tinh Vĩnh Cửu
        level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY() + 0.5D, this.getZ() + 0.3D,
                new ItemStack(ModItems.ETERNAL_FROST_CRYSTAL.get())));

        broadcastDialogue("Băng giá vĩnh hằng... không bao giờ thực sự tan biến...");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SigilCount", getSigilCount());
        compound.putBoolean("IsEnraged", isEnraged());
        compound.putInt("BlizzardTicks", blizzardTicksRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SigilCount")) {
            setSigilCount(compound.getInt("SigilCount"));
        }
        if (compound.contains("IsEnraged")) {
            setEnraged(compound.getBoolean("IsEnraged"));
        }
        if (compound.contains("BlizzardTicks")) {
            this.blizzardTicksRemaining = compound.getInt("BlizzardTicks");
        }
    }

    // AI Combat Goal chuyên biệt
    private static class VelzardCombatGoal extends Goal {
        private final VelzardEntity velzard;

        public VelzardCombatGoal(VelzardEntity velzard) {
            this.velzard = velzard;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = velzard.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = velzard.getTarget();
            if (target == null) return;

            velzard.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distSq = velzard.distanceToSqr(target);
            if (distSq > 16.0D * 16.0D) {
                velzard.getNavigation().moveTo(target, 1.2D);
            } else if (distSq < 6.0D * 6.0D) {
                // Lùi lại giữ khoảng cách cự ly tầm trung
                Vec3 away = velzard.position().subtract(target.position()).normalize().scale(8.0D);
                velzard.getNavigation().moveTo(velzard.getX() + away.x, velzard.getY() + 1.5D, velzard.getZ() + away.z, 1.0D);
            }
        }
    }
}
