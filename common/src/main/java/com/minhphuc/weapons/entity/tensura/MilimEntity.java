package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.tensura.HorizontalHolyBeamAbility;
import com.minhphuc.weapons.content.tensura.LuciferReplicationAbility;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.TensuraDialogueManager;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * Ma Vương Cổ Đại Milim Nava (Destroyer - Dragonoid)
 * Đặc tính:
 * - Ban đầu thân thiện (Neutral), chỉ phản công khi bị tấn công trước.
 * - Kháng sát thương gần như tuyệt đối (giới hạn cứng theo cấp bậc người tấn công).
 * - Tốc độ tự phục hồi siêu tốc 5 tim (10 HP/s).
 * - Tuyệt kỹ độc quyền: Long Tinh Bộc Viêm Bá (Tụ lực 10s, có thể bị ngắt bởi 2 skill trúng, 2 dạng có/không phá đất).
 * - Kỹ năng mới: Cột Sáng Ngang (Tà Khứ Vũ Thê Tử phóng ngang xuyên thẳng).
 * - Có khả năng bay tự do trên không trung.
 */
public class MilimEntity extends Monster {

    public static final EntityDataAccessor<Integer> DATA_CASTING_STATE =
            SynchedEntityData.defineId(MilimEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_CHANNELING_TICKS =
            SynchedEntityData.defineId(MilimEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_FLYING =
            SynchedEntityData.defineId(MilimEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_ENRAGED =
            SynchedEntityData.defineId(MilimEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent;

    // Bộ đếm kỹ năng
    private int dragonNovaCooldown = 400; // 20s cooldown
    private int horizontalBeamCooldown = 120; // 6s cooldown
    private int flightHoverTicks = 0;

    // Quản lý tụ lực Long Tinh Bộc Viêm Bá (10s = 200 ticks)
    private Vec3 lockedTargetDirection = null;

    // Quản lý chào hỏi người chơi theo định kỳ
    private final Map<UUID, Long> playerGreetingCooldowns = new HashMap<>();

    // Particle linh tử Spiritrons tím / hồng
    private static final DustParticleOptions SPIRITRON_PINK =
            new DustParticleOptions(new Vector3f(1.0F, 0.4F, 0.8F), 1.8F);
    private static final DustParticleOptions SPIRITRON_PURPLE =
            new DustParticleOptions(new Vector3f(0.7F, 0.1F, 1.0F), 1.8F);

    public MilimEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§d§l[MA VƯƠNG CỔ ĐẠI] §c§lMILIM NAVA §7- §e[Bạo Chúa Hủy Diệt]"),
                BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        this.moveControl = new FlyingMoveControl(this, 15, true);
        this.setNoGravity(false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.ATTACK_DAMAGE, 60.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;

                // Tối ưu chống lag: Kiểm tra trong phạm vi 128 blocks nếu đã có Milim thì không spawn thêm
                AABB checkArea = new AABB(this.blockPosition()).inflate(128.0D);
                if (!sl.getEntitiesOfClass(MilimEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }

            BlockPos pos = this.blockPosition();
            if (!level.canSeeSky(pos) || pos.getY() < 60) {
                return false;
            }

            // Tỉ lệ spawn tương tự Velgrynd: Chỉ 5% cơ hội thành công
            if (this.random.nextFloat() > 0.05F) {
                return false;
            }
        }
        return super.checkSpawnRules(level, spawnType);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @org.jetbrains.annotations.Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);

        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            if (level.getLevel().dimension() != Level.OVERWORLD) {
                this.discard();
                return spawnData;
            }
            BlockPos pos = this.blockPosition();
            if (!level.canSeeSky(pos) || pos.getY() < 60 || this.random.nextFloat() > 0.05F) {
                this.discard();
                return spawnData;
            }
        }
        return spawnData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING_STATE, 0); // 0 = Idle, 1 = Dragon Nova, 2 = Horizontal Beam
        builder.define(DATA_CHANNELING_TICKS, 0);
        builder.define(DATA_IS_FLYING, false);
        builder.define(DATA_IS_ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MilimCombatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        // THÂN THIỆN: Tuyệt đối không tự ý tấn công, CHỈ phản đòn khi bị đánh trước!
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public int getCastingState() {
        return this.entityData.get(DATA_CASTING_STATE);
    }

    public void setCastingState(int state) {
        this.entityData.set(DATA_CASTING_STATE, state);
    }

    public int getChannelingTicks() {
        return this.entityData.get(DATA_CHANNELING_TICKS);
    }

    public void setChannelingTicks(int ticks) {
        this.entityData.set(DATA_CHANNELING_TICKS, ticks);
    }

    public boolean isFlyingAnim() {
        return this.entityData.get(DATA_IS_FLYING);
    }

    public void setFlyingAnim(boolean flying) {
        this.entityData.set(DATA_IS_FLYING, flying);
    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_IS_ENRAGED);
    }

    public void setEnraged(boolean enraged) {
        this.entityData.set(DATA_IS_ENRAGED, enraged);
    }

    public boolean isChannelingDragonNova() {
        return getCastingState() == 1;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;

        // Miễn nhiễm các loại sát thương môi trường
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) ||
            source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CACTUS) ||
            source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.STARVE)) {
            return false;
        }

        // =========================================================================
        // QUY TẮC BẤT HOẠI TUYỆT ĐỐI CỦA MILIM:
        // NGOÀI NGƯỜI CHƠI THỨC TỈNH MA VƯƠNG, ROUGE, CHƯỚC NHIỆT LONG -> HOÀN TOÀN MIỄN NHIỄM SÁT THƯƠNG
        // =========================================================================
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();

        // 1. Phản công ngay lập tức: Bất kể ai dám tấn công Milim đều bị Milim khóa mục tiêu
        LivingEntity actualAttacker = null;
        if (attacker instanceof LivingEntity le && le != this) {
            actualAttacker = le;
        } else if (direct instanceof LivingEntity dle && dle != this) {
            actualAttacker = dle;
        }

        if (actualAttacker != null && actualAttacker.isAlive()) {
            this.setTarget(actualAttacker);
            this.setLastHurtByMob(actualAttacker);
        }

        // 2. Kiểm tra 3 đối tượng duy nhất có thể gây sát thương:
        // (a) Người chơi đã thức tỉnh thành Ma Vương (True Demon Lord)
        boolean isPlayerDemonLord = attacker instanceof ServerPlayer sp && PrimordialPlayerDataHelper.isDemonLord(sp);

        // (b) Rouge (Guy Crimson - Ác ma Thủy Tổ Đỏ hoặc Người chơi mang bản ngã Rouge)
        boolean isRouge = (attacker instanceof PrimordialDemonEntity pde && pde.getDemonType() == DemonType.ROUGE)
                || (direct instanceof PrimordialDemonEntity pde2 && pde2.getDemonType() == DemonType.ROUGE)
                || (attacker instanceof ServerPlayer sp && PrimordialPlayerDataHelper.getPrimordialType(sp) == DemonType.ROUGE);

        // (c) Chước Nhiệt Long (Velgrynd)
        boolean isVelgrynd = (attacker instanceof VelgryndEntity) || (direct instanceof VelgryndEntity);

        if (!isPlayerDemonLord && !isRouge && !isVelgrynd) {
            // Hoàn toàn không thể gây sát thương cho Milim!
            ServerLevel sl = (ServerLevel) this.level();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0F, 0.8F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.2F, 1.6F);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.0D, this.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.2D);
            sl.sendParticles(SPIRITRON_PINK, this.getX(), this.getY() + 1.0D, this.getZ(), 15, 0.5D, 0.5D, 0.5D, 0.1D);

            if (attacker instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("§c[Milim Nava] Đòn tấn công hoàn toàn vô hiệu! Chỉ Ma Vương Thức Tỉnh, Rouge hoặc Chước Nhiệt Long mới có thể đả thương Milim!"), true);
            }
            return false;
        }

        // 3. Sát thương dành cho 3 thực thể được phép đả thương Milim
        float finalDamage;
        if (isPlayerDemonLord) {
            boolean holdsMythicSword = attacker instanceof ServerPlayer player &&
                    (player.getMainHandItem().is(ModItems.MOONLIGHT_SWORD.get()) ||
                     player.getOffhandItem().is(ModItems.MOONLIGHT_SWORD.get()));
            finalDamage = holdsMythicSword ? 10.0F : 8.0F; // 5 tim hoặc 4 tim
        } else if (isRouge) {
            finalDamage = 8.0F; // 4 tim (Rouge)
        } else {
            finalDamage = 8.0F; // 4 tim (Chước Nhiệt Long)
        }

        return super.hurt(source, finalDamage);
    }

    private void cancelDragonNova() {
        this.setCastingState(0);
        this.setChannelingTicks(0);
        this.lockedTargetDirection = null;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Hiệu ứng hạt linh tử khi tụ chiêu trên Client
            if (isChannelingDragonNova()) {
                double rad = 2.0D;
                for (int i = 0; i < 4; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2.0;
                    double px = this.getX() + Math.cos(angle) * rad;
                    double pz = this.getZ() + Math.sin(angle) * rad;
                    double py = this.getY() + 0.5D + this.random.nextDouble() * 1.5D;
                    this.level().addParticle(SPIRITRON_PINK, px, py, pz,
                            (this.getX() - px) * 0.15D, 0.05D, (this.getZ() - pz) * 0.15D);
                }
            }
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();

        // 1. Quản lý BossBar
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.getTarget() != null);

        // 2. Tốc độ hồi máu: 5 tim (10 HP) mỗi 20 ticks (1s)
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(10.0F);
            sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0D, this.getZ(),
                    3, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        // Cập nhật trạng thái Cuồng Nộ (máu <= 50%)
        boolean enraged = this.getHealth() <= (this.getMaxHealth() * 0.5F);
        if (enraged != isEnraged()) {
            setEnraged(enraged);
            if (enraged) {
                TensuraDialogueManager.sayMilim(this, "dialogue.weapons.milim.enraged");
                this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            }
        }

        // 3. Quản lý trạng thái bay
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            setFlyingAnim(true);
            this.setNoGravity(true);

            // Giữ độ cao lơ lửng trên không 3-5 blocks so với đối thủ
            double desiredY = target.getY() + 3.5D;
            if (this.getY() < desiredY) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05D, 0));
            }

            // Tiến trình Tụ Lực Long Tinh Bộc Viêm Bá
            if (isChannelingDragonNova()) {
                tickDragonNovaChanneling(sl);
            } else {
                // Cooldowns
                if (dragonNovaCooldown > 0) dragonNovaCooldown--;
                if (horizontalBeamCooldown > 0) horizontalBeamCooldown--;

                // Quyết định dùng kỹ năng
                if (dragonNovaCooldown <= 0 && this.distanceToSqr(target) <= 45.0D * 45.0D) {
                    startDragonNovaChanneling(sl);
                } else if (horizontalBeamCooldown <= 0 && this.distanceToSqr(target) <= 35.0D * 35.0D) {
                    executeHorizontalBeam(sl, target);
                }
            }
        } else {
            // Không trong giao chiến
            setFlyingAnim(false);
            this.setNoGravity(false);
            if (isChannelingDragonNova()) {
                cancelDragonNova();
            }

            // Tự động chào hỏi người chơi khi đi ngang qua (mỗi 40 ticks kiểm tra 1 lần)
            if (this.tickCount % 40 == 0) {
                checkPlayerGreeting(sl);
            }
        }
    }

    private void startDragonNovaChanneling(ServerLevel sl) {
        this.setCastingState(1);
        this.setChannelingTicks(0);
        LivingEntity t = this.getTarget();
        if (t != null) {
            this.lockedTargetDirection = t.position().add(0, t.getEyeHeight() * 0.5, 0).subtract(this.position().add(0, 1.5, 0)).normalize();
        }

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5F, 1.4F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.8F);

        TensuraDialogueManager.sayMilim(this, "dialogue.weapons.milim.cast_dragon_nova");
        LuciferReplicationAbility.recordSkillObserved(sl, this.position(), "DRAGON_NOVA", "Long Tinh Bộc Viêm Bá (Milim Nava)");

        // Broadcast cảnh báo tới người chơi trong phạm vi 64 blocks
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
            p.displayClientMessage(
                    Component.literal("§d§l[MILIM NAVA] §c§lĐang ngưng tụ Long Tinh Bộc Viêm Bá! Đếm ngược 4 giây (Tuyệt đối không thể ngăn cản)!"),
                    false
            );
        }
    }

    private void tickDragonNovaChanneling(ServerLevel sl) {
        int currentTicks = getChannelingTicks() + 1;
        setChannelingTicks(currentTicks);

        // Khóa vị trí lơ lửng ổn định
        this.setDeltaMovement(0, 0.01D, 0);

        // Cập nhật hướng bắn hướng về phía đối thủ
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.lockedTargetDirection = target.position().add(0, target.getEyeHeight() * 0.5, 0).subtract(this.position().add(0, 1.5, 0)).normalize();
        }

        // Âm thanh tụ lực dồn dập mỗi giây (20 ticks)
        if (currentTicks % 20 == 0) {
            int secondsLeft = (80 - currentTicks) / 20;
            if (secondsLeft > 0) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE, 2.0F, 0.6F + (currentTicks / 80.0F) * 0.8F);

                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(48.0D))) {
                    p.displayClientMessage(
                            Component.literal("§d§l[LONG TINH BỘC VIÊM BÁ] §cTụ lực: " + secondsLeft + "s §e[TUYỆT ĐỐI KHÔNG THỂ NGĂN CẢN]!"),
                            true
                    );
                }
            }
        }

        // Hạt tụ lực xoay quanh Milim cuốn xoáy dữ dội
        for (int i = 0; i < 4; i++) {
            double angle = (currentTicks * 18.0 + i * 90.0) * Math.PI / 180.0;
            double rad = Math.max(0.5D, 3.2D * (1.0D - (currentTicks / 90.0D)));
            double px = this.getX() + Math.cos(angle) * rad;
            double pz = this.getZ() + Math.sin(angle) * rad;
            double py = this.getY() + 1.2D + Math.sin(currentTicks * 0.25 + i) * 0.35D;
            sl.sendParticles(SPIRITRON_PINK, px, py, pz, 1, 0, 0, 0, 0);
            sl.sendParticles(SPIRITRON_PURPLE, px, py, pz, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0, 0, 0, 0);
        }

        // Đạt mốc 80 ticks (4s) -> Khai hỏa Long Tinh Bộc Viêm Bá!
        if (currentTicks >= 80) {
            fireDragonNova(sl);
        }
    }

    private void fireDragonNova(ServerLevel sl) {
        this.setCastingState(0);
        this.setChannelingTicks(0);
        this.dragonNovaCooldown = 360; // Reset hồi chiêu 18s

        Vec3 startPos = this.position().add(0, 1.5D, 0);
        Vec3 dir = this.lockedTargetDirection != null ? this.lockedTargetDirection : this.getLookAngle();
        dir = dir.normalize();

        TensuraDialogueManager.sayMilim(this, "dialogue.weapons.milim.fire_dragon_nova_destroy");

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 1.0F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 5.0F, 0.7F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.5F, 1.4F);

        // Hiển thị Cột Sáng 3D đa tầng (Giống Cú Bắn Granit nhưng với quy mô Long Tinh Bộc Viêm Bá cực đại)
        double maxDistance = 70.0D;
        HorizontalHolyBeamAbility.spawn3DBeamDisplay(sl, startPos, dir, maxDistance, 4.5F, 0xFF55DD, 8.0F, 0xFF0044, 40);

        // Bắn tia quét hủy diệt
        double step = 1.0D;
        Vec3 currentPos = startPos;

        for (double d = 0; d < maxDistance; d += step) {
            currentPos = currentPos.add(dir.scale(step));

            // 1. Hiệu ứng hạt dọc theo thân cột pháo năng lượng cực đại
            if (Math.round(d) % 6 == 0) {
                sl.sendParticles(ParticleTypes.SONIC_BOOM, currentPos.x, currentPos.y, currentPos.z, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.FLASH, currentPos.x, currentPos.y, currentPos.z, 1, 0.4, 0.4, 0.4, 0);
            sl.sendParticles(SPIRITRON_PINK, currentPos.x, currentPos.y, currentPos.z, 4, 1.2, 1.2, 1.2, 0.08);
            sl.sendParticles(SPIRITRON_PURPLE, currentPos.x, currentPos.y, currentPos.z, 3, 1.0, 1.0, 1.0, 0.08);
            sl.sendParticles(ParticleTypes.FLAME, currentPos.x, currentPos.y, currentPos.z, 4, 1.5, 1.5, 1.5, 0.1);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, currentPos.x, currentPos.y, currentPos.z, 3, 1.0, 1.0, 1.0, 0.05);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, currentPos.x, currentPos.y, currentPos.z, 2, 0.8, 0.8, 0.8, 0.1);

            // 2. Sát thương thực thể (Xuyên mọi giáp & xóa sạch buff)
            AABB hitBox = new AABB(currentPos.x - 4.5, currentPos.y - 4.5, currentPos.z - 4.5,
                    currentPos.x + 4.5, currentPos.y + 4.5, currentPos.z + 4.5);

            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e.isAlive());
            for (LivingEntity v : victims) {
                v.removeEffect(MobEffects.REGENERATION);
                v.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                v.removeEffect(MobEffects.FIRE_RESISTANCE);
                v.hurt(this.damageSources().magic(), 1800.0F);
                v.setRemainingFireTicks(400); // Thiêu đốt 20s
                Vec3 knockback = dir.scale(2.5D).add(0, 0.4D, 0);
                v.setDeltaMovement(knockback);
                v.hasImpulse = true;
            }

            // 3. Phá hủy địa hình mạnh mẽ (khoét rãnh hầm bán kính 4 block)
            BlockPos centerBlock = BlockPos.containing(currentPos);
            int tunnelRadius = 4;
            int tunnelRadiusSq = tunnelRadius * tunnelRadius;
            for (int dx = -tunnelRadius; dx <= tunnelRadius; dx++) {
                for (int dy = -tunnelRadius; dy <= tunnelRadius; dy++) {
                    for (int dz = -tunnelRadius; dz <= tunnelRadius; dz++) {
                        if (dx * dx + dy * dy + dz * dz <= tunnelRadiusSq) {
                            BlockPos targetPos = centerBlock.offset(dx, dy, dz);
                            BlockState state = sl.getBlockState(targetPos);
                            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK && state.getDestroySpeed(sl, targetPos) >= 0) {
                                sl.destroyBlock(targetPos, false);
                            }
                        }
                    }
                }
            }

            // 4. Gây cháy lớn cho địa hình xung quanh (vùng ngoại vi bán kính 7 blocks)
            int fireRadius = 7;
            int fireRadiusSq = fireRadius * fireRadius;
            for (int fx = -fireRadius; fx <= fireRadius; fx++) {
                for (int fz = -fireRadius; fz <= fireRadius; fz++) {
                    if (fx * fx + fz * fz <= fireRadiusSq && sl.random.nextFloat() < 0.35F) {
                        for (int fy = -2; fy <= 3; fy++) {
                            BlockPos firePos = centerBlock.offset(fx, fy, fz);
                            BlockState fireState = sl.getBlockState(firePos);
                            if (fireState.is(Blocks.SNOW) || fireState.is(Blocks.SNOW_BLOCK) || fireState.is(Blocks.ICE)) {
                                sl.destroyBlock(firePos, false);
                            } else if (fireState.isAir() && sl.getBlockState(firePos.below()).isSolid()) {
                                sl.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 5. Vụ nổ kết thúc khổng lồ tại điểm cuối (Grand Crater Explosion)
        BlockPos impactCenter = BlockPos.containing(currentPos);
        int craterRadius = 10;
        int craterRadiusSq = craterRadius * craterRadius;
        for (int cx = -craterRadius; cx <= craterRadius; cx++) {
            for (int cz = -craterRadius; cz <= craterRadius; cz++) {
                for (int cy = -6; cy <= 8; cy++) {
                    if (cx * cx + cz * cz + cy * cy <= craterRadiusSq) {
                        BlockPos craterPos = impactCenter.offset(cx, cy, cz);
                        BlockState cState = sl.getBlockState(craterPos);
                        if (!cState.isAir() && cState.getBlock() != Blocks.BEDROCK && cState.getDestroySpeed(sl, craterPos) >= 0) {
                            sl.destroyBlock(craterPos, false);
                        }
                    }
                }
            }
        }

        // Vành đai biển lửa bao phủ mặt đất bán kính 16m
        int impactFireRadius = 16;
        for (int ix = -impactFireRadius; ix <= impactFireRadius; ix++) {
            for (int iz = -impactFireRadius; iz <= impactFireRadius; iz++) {
                if (ix * ix + iz * iz <= impactFireRadius * impactFireRadius && sl.random.nextFloat() < 0.55F) {
                    BlockPos floorPos = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, impactCenter.offset(ix, 0, iz));
                    if (sl.getBlockState(floorPos).isAir() && sl.getBlockState(floorPos.below()).isSolid()) {
                        sl.setBlockAndUpdate(floorPos, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
        }

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, currentPos.x, currentPos.y, currentPos.z, 12, 4.0, 3.0, 4.0, 0);
        sl.sendParticles(ParticleTypes.FLASH, currentPos.x, currentPos.y, currentPos.z, 4, 2.0, 2.0, 2.0, 0);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, currentPos.x, currentPos.y, currentPos.z, 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.LAVA, currentPos.x, currentPos.y, currentPos.z, 50, 6.0, 4.0, 6.0, 0.2);
        sl.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 7.0F, 0.4F);
    }

    private void executeHorizontalBeam(ServerLevel sl, LivingEntity target) {
        this.setCastingState(2);
        this.horizontalBeamCooldown = 140;

        Vec3 startPos = this.position().add(0, 1.4D, 0);
        Vec3 dir = target.position().add(0, target.getEyeHeight() * 0.5, 0).subtract(startPos).normalize();

        TensuraDialogueManager.sayMilim(this, "dialogue.weapons.milim.cast_horizontal_beam");
        LuciferReplicationAbility.recordSkillObserved(sl, this.position(), "HORIZONTAL_BEAM", "Cột Sáng Ngang (Milim Nava)");

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 1.3F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5F, 1.4F);

        // Bắn luồng sáng vuông ngang dài 40m
        double maxDist = 40.0D;
        double step = 1.2D;
        Vec3 cur = startPos;

        for (double d = 0; d < maxDist; d += step) {
            cur = cur.add(dir.scale(step));

            // Hiệu ứng cột sáng thánh quang phóng ngang
            sl.sendParticles(ParticleTypes.END_ROD, cur.x, cur.y, cur.z, 2, 0.3, 0.3, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cur.x, cur.y, cur.z, 2, 0.4, 0.4, 0.4, 0.04);
            sl.sendParticles(ParticleTypes.WAX_OFF, cur.x, cur.y, cur.z, 1, 0.2, 0.2, 0.2, 0.01);

            AABB box = new AABB(cur.x - 1.5, cur.y - 1.5, cur.z - 1.5,
                    cur.x + 1.5, cur.y + 1.5, cur.z + 1.5);

            for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
                v.hurt(this.damageSources().mobAttack(this), 120.0F);
                v.setRemainingFireTicks(160); // Đốt cháy
            }

            // Đốt cháy nhẹ các bề mặt đất xung quanh vệt bắn ngang
            BlockPos groundCheck = BlockPos.containing(cur).below();
            if (sl.getBlockState(groundCheck).isSolid() && sl.getBlockState(groundCheck.above()).isAir()) {
                if (sl.random.nextFloat() < 0.25F) {
                    sl.setBlockAndUpdate(groundCheck.above(), Blocks.FIRE.defaultBlockState());
                }
            }
        }

        // Trở về idle sau khi phóng beam
        this.setCastingState(0);
    }

    private void checkPlayerGreeting(ServerLevel sl) {
        long currentTick = sl.getGameTime();
        List<ServerPlayer> nearbyPlayers = sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(10.0D));

        for (ServerPlayer player : nearbyPlayers) {
            UUID pId = player.getUUID();
            long lastGreet = playerGreetingCooldowns.getOrDefault(pId, 0L);

            // Cooldown 60s (1200 ticks) giữa các lần chào
            if (currentTick - lastGreet >= 1200L) {
                playerGreetingCooldowns.put(pId, currentTick);

                // Nhận diện thân phận người chơi
                DemonType demonType = PrimordialPlayerDataHelper.getPrimordialType(player);
                boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(player);

                if (isDemonLord) {
                    TensuraDialogueManager.sayMilimGreeting(this, player, "dialogue.weapons.milim.greet_demon_lord");
                } else if (demonType != null) {
                    TensuraDialogueManager.sayMilimGreeting(this, player, "dialogue.weapons.milim.greet_" + demonType.name().toLowerCase());
                } else {
                    TensuraDialogueManager.sayMilimGreeting(this, player, "dialogue.weapons.milim.greet_human");
                }
                break; // Chỉ chào 1 người mỗi lần
            }
        }
    }

    // =========================================================================
    // KỸ NĂNG ĐẤM THƯỜNG CỦA MILIM: CÚ ĐẤM BẠO CHÚA (ĐẤM VĂNG XA + NỔ LỚN + KHÓI MÙ MỊT)
    // =========================================================================
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level().isClientSide()) return false;
        ServerLevel sl = (ServerLevel) this.level();

        Vec3 punchOrigin = this.position().add(0, this.getEyeHeight() * 0.8D, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        Vec3 dir = targetCenter.subtract(punchOrigin).normalize();
        if (dir.lengthSqr() < 1.0E-4) {
            dir = this.getLookAngle();
        }

        // 1. Phá giáp chắn khiên nếu mục tiêu đang đỡ đòn
        if (target instanceof Player player && player.isBlocking()) {
            player.disableShield();
        }

        // 2. Gây sát thương uy lực cực đại (60.0F = 30 tim)
        boolean hurt = target.hurt(this.damageSources().mobAttack(this), 60.0F);

        // 3. Đấm mục tiêu bay cực xa (Knockback siêu mạnh)
        Vec3 knockback = new Vec3(dir.x * 4.2D, 1.35D, dir.z * 4.2D);
        target.setDeltaMovement(knockback);
        target.hurtMarked = true;
        target.hasImpulse = true;
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(target));
        }

        // 4. Hiệu ứng bộc phá và khói nổ mù mịt cực lớn
        double hitX = target.getX();
        double hitY = targetCenter.y;
        double hitZ = target.getZ();

        // Nổ lớn và sóng xung kích âm thanh
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitX, hitY, hitZ, 3, 0.3D, 0.3D, 0.3D, 0.0D);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, hitX, hitY, hitZ, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        sl.sendParticles(ParticleTypes.FLASH, hitX, hitY, hitZ, 2, 0, 0, 0, 0);
        sl.sendParticles(SPIRITRON_PINK, hitX, hitY, hitZ, 120, 2.5D, 2.0D, 2.5D, 0.4D);

        // Khói mù mịt dày đặc lan tỏa rộng
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hitX, hitY, hitZ, 80, 2.0D, 1.5D, 2.0D, 0.08D);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, hitX, hitY, hitZ, 60, 1.8D, 1.2D, 1.8D, 0.12D);

        // Âm thanh bộc phá vang dội
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.8F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.2F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.5F, 0.6F);

        // 5. Xung lực chấn động thổi bay các thực thể xung quanh (Shockwave 7 blocks)
        AABB shockBox = target.getBoundingBox().inflate(7.0D);
        for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, shockBox)) {
            if (nearby != this && nearby != target) {
                Vec3 away = nearby.position().subtract(target.position()).normalize();
                if (away.lengthSqr() < 1.0E-4) {
                    away = dir;
                }
                nearby.setDeltaMovement(away.x * 2.5D, 0.9D, away.z * 2.5D);
                nearby.hurtMarked = true;
                nearby.hasImpulse = true;
                if (nearby instanceof ServerPlayer nsp) {
                    nsp.connection.send(new ClientboundSetEntityMotionPacket(nearby));
                }
                nearby.hurt(this.damageSources().mobAttack(this), 25.0F);
            }
        }

        return hurt;
    }

    static class MilimCombatGoal extends Goal {
        private final MilimEntity milim;
        private int attackCooldown = 0;
        private int dashCooldown = 0;
        private boolean isDashing = false;
        private int dashTicks = 0;

        public MilimCombatGoal(MilimEntity milim) {
            this.milim = milim;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.milim.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = this.milim.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity t = this.milim.getTarget();
            if (t == null) return;

            if (attackCooldown > 0) attackCooldown--;
            if (dashCooldown > 0) dashCooldown--;

            this.milim.getLookControl().setLookAt(t, 60.0F, 60.0F);

            // Khi đang tụ lực Dragon Nova: Đứng yên lơ lửng tụ lực
            if (this.milim.isChannelingDragonNova()) {
                this.milim.getNavigation().stop();
                this.isDashing = false;
                return;
            }

            double distSq = this.milim.distanceToSqr(t);
            double dist = Math.sqrt(distSq);

            // Xử lý trạng thái đang lao tới (Supersonic Dash)
            if (isDashing) {
                dashTicks++;
                if (this.milim.level() instanceof ServerLevel sl) {
                    // Vệt khói và linh tử kéo dài theo đường lao của Milim
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.milim.getX(), this.milim.getY() + 0.5D, this.milim.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.02D);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, this.milim.getX(), this.milim.getY() + 0.5D, this.milim.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.05D);
                    sl.sendParticles(SPIRITRON_PINK, this.milim.getX(), this.milim.getY() + 0.5D, this.milim.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.1D);
                }

                // Nếu chạm vào mục tiêu hoặc gần sát (<= 3.5 blocks)
                if (dist <= 3.5D || distSq <= 12.25D) {
                    this.isDashing = false;
                    this.milim.doHurtTarget(t);
                    this.milim.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    this.attackCooldown = 25; // 1.25s hồi đòn đấm
                    this.dashCooldown = 40;  // 2s trước lần bứt tốc tiếp theo
                    return;
                }

                // Nếu lướt quá 15 ticks mà chưa chạm mục tiêu thì kết thúc dash
                if (dashTicks > 15) {
                    this.isDashing = false;
                }
                return;
            }

            // Kích hoạt chiêu lao thẳng tới khi mục tiêu trong cự ly 3.5m - 32m
            if (dashCooldown <= 0 && dist >= 3.5D && dist <= 32.0D) {
                Vec3 targetPos = t.position().add(0, t.getBbHeight() * 0.5D, 0);
                Vec3 milimPos = this.milim.position().add(0, this.milim.getBbHeight() * 0.5D, 0);
                Vec3 dashDir = targetPos.subtract(milimPos).normalize();

                // Bứt tốc cực đại 2.4 blocks/tick
                this.milim.setDeltaMovement(dashDir.scale(2.4D));
                this.milim.hasImpulse = true;
                this.isDashing = true;
                this.dashTicks = 0;

                if (this.milim.level() instanceof ServerLevel sl) {
                    // Âm thanh bùng nổ khi bứt tốc lao thẳng đi
                    sl.playSound(null, this.milim.getX(), this.milim.getY(), this.milim.getZ(),
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 1.8F);
                    sl.playSound(null, this.milim.getX(), this.milim.getY(), this.milim.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 1.6F);
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, this.milim.getX(), this.milim.getY() + 0.8D, this.milim.getZ(), 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.milim.getX(), this.milim.getY() + 0.5D, this.milim.getZ(), 25, 0.8D, 0.8D, 0.8D, 0.05D);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, this.milim.getX(), this.milim.getY() + 0.5D, this.milim.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.08D);
                }
                return;
            }

            // Di chuyển tiếp cận thông thường
            if (dist > 3.0D) {
                this.milim.getNavigation().moveTo(t.getX(), t.getY() + 1.0D, t.getZ(), 1.4D);
            } else {
                // Đủ gần để tung cú đấm
                if (attackCooldown <= 0 && this.milim.attackAnim == 0) {
                    this.milim.doHurtTarget(t);
                    this.milim.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    this.attackCooldown = 25;
                    this.dashCooldown = 30;
                }
            }
        }
    }
}
