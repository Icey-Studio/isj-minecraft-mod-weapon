package com.minhphuc.weapons.entity.darkgathering;

import java.util.UUID;

import com.minhphuc.weapons.content.darkgathering.CuuTheCurseManager;
import com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility;
import com.minhphuc.weapons.content.darkgathering.SeerFleshAbility;
import com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility;
import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ClientboundTengaiSlashEffectPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * THIÊN NGOẠI CỨU THẾ QUYỀN HIỆN (TENGAI GUZE GONGEN)
 * TỐI ƯU HÓA HIỆU NĂNG TỐI ĐA (ZERO LAG):
 * - Vóc dáng cao lớn, đô con, uy nghi hơn người chơi.
 * - Tay cầm Thiên Ma Kiếm ở Dạng 1; chuyển sang Tay Không (Brawler Mode) ở Dạng 2.
 * - Tương quan sức mạnh:
 *   + 100% thua trước: Milim, Velgrynd, Velzard.
 *   + 70% Thắng trước Thất Đại Ác Ma Thủy Tổ (Primordial Demon), kháng 70% skill ác ma, hồi máu nhanh.
 * - Tam Thức Luân Chuyển:
 *   + Đoạt Tim -> Đoạt Não -> Tuyệt Mệnh Song Trảm (Cả Tim Lẫn Não).
 * - Kỹ Năng Mới & Animation Mượt Mà:
 *   + Không Gian Trảm (Void Severance - Raycast Vector chém tầm xa).
 *   + Thiên Ngoại Thần Áp (Gravitational Crush - Dậm chân chấn động đè bẹp đối thủ).
 *   + Bát Nhã Kim Cang Thể (Vajra Asura Aegis - Thủ thế hộ thể tăng giáp và phản đòn).
 * - Cơ chế Kháng Kỹ Năng & Đối Kháng Chiến Thuật:
 *   + Kháng 90% Linh Tử Băng Hoại (90% phá vỡ pháp trận ánh sáng, 10% dính).
 *   + Kháng 70% Tà Khứ Vũ Thê Tử (Jacob's Ladder).
 *   + Kháng 100% Phép Màu Của Thần Linh & Bí Ẩn Đức Tin (tự do đi qua).
 *   + Kháng 0% Long Tinh Bộc Viêm Bá nhưng nhận diện từ xa và nhảy né.
 *   + Kháng 0% Diệt Thế Tà Tinh: Alkaid.
 *   + Kháng 30% Cú Bắn Granit & Zone Track kèm ưu tiên né tránh.
 *   + Kháng 100% Nghịch Thời Nhất Trảm, Hắc Thiểm Bá Vương Trảm, Linh Tử Cầu.
 *   + Lồng Giam Vô Hạn: Chuyển sang dạng tay không và phá vỡ lồng.
 *   + Đa Trùng Kết Giới: Dạng tay không đấm 3 lần không vỡ -> chuyển sang kiếm 1 chém là vỡ.
 *   + Không chiến (Anti-Air): Kéo giật các thực thể bay trên không rớt xuống đất.
 *   + Ưu tiên chém chết Thị Thực của người chơi với 1 nhát chém duy nhất.
 */
public class CuuTheQuyenHienEntity extends Monster {

    private static final EntityDataAccessor<Boolean> BARE_HANDED =
            SynchedEntityData.defineId(CuuTheQuyenHienEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(CuuTheQuyenHienEntity.class, EntityDataSerializers.BYTE);

    private int stunTicks = 0;
    private int timeSlashCooldown = 160; // 8s đầu sau khi spawn
    private int soulParriedCount = 0;
    private int barrierPunchCount = 0;
    private int antiAirCooldown = 0;

    // Quản lý kỹ năng mới & Animation State
    private int actionTicks = 0;
    private int slashStrikeMode = 0; // 0 = HEART, 1 = BRAIN, 2 = DUAL
    private int spatialSlashCooldown = 120;
    private int gravitationalCrushCooldown = 200;
    private int vajraAegisCooldown = 360;
    private int vajraAegisDuration = 0;

    // Cơ chế Phong Ấn & Triệu Hồi (Dark Gathering - Tốt Nghiệp Sinh Đệ Nhất Khóa)
    private java.util.UUID ownerUuid = null;
    private int summonTicks = -1; // -1 = Tự nhiên, > 0 = Đang được triệu hồi kiểm soát, 0 = Bạo tẩu phản phệ
    private boolean isRampaging = false;

    public CuuTheQuyenHienEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 35.0D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BARE_HANDED, false);
        builder.define(ACTION_STATE, (byte) 0);
    }

    public boolean isBareHanded() {
        return this.entityData.get(BARE_HANDED);
    }

    public byte getActionState() {
        return this.entityData.get(ACTION_STATE);
    }

    public void setActionState(byte state, int durationTicks) {
        this.entityData.set(ACTION_STATE, state);
        this.actionTicks = durationTicks;
    }

    public int getSlashStrikeMode() {
        return this.slashStrikeMode;
    }

    public void cycleSlashStrikeMode() {
        this.slashStrikeMode = (this.slashStrikeMode + 1) % 3;
    }

    public void setBareHanded(boolean bareHanded) {
        this.entityData.set(BARE_HANDED, bareHanded);
        if (bareHanded) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) dmgAttr.setBaseValue(50.0D);
            var spdAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spdAttr != null) spdAttr.setBaseValue(0.35D);

            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.5F, 0.8F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.0F, 1.2F);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.2D, this.getZ(), 25, 0.6D, 0.6D, 0.6D, 0.2D);
            }
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.THIEN_MA_KATANA.get()));
            var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) dmgAttr.setBaseValue(35.0D);
            var spdAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spdAttr != null) spdAttr.setBaseValue(0.30D);
        }
    }

    public void recordSoulParried(Player parrier) {
        this.soulParriedCount++;
        if (this.soulParriedCount >= 2 && !this.isBareHanded()) {
            this.setBareHanded(true);
            if (parrier instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c§l[CHIẾN Ý BỘC PHÁ] §fĐòn chém bị Tiên Tri hóa giải 2 lần! Cứu Thế Quyền Hiện chuyển sang trạng thái Võ Thần Tay Không (Brawler Mode)!"));
            }
        }
    }

    public void initSummon(java.util.UUID ownerUuid, int durationTicks) {
        this.ownerUuid = ownerUuid;
        this.summonTicks = durationTicks;
        this.isRampaging = false;
    }

    public boolean isOwnedBy(Player player) {
        return this.ownerUuid != null && this.ownerUuid.equals(player.getUUID());
    }

    public boolean isRampaging() {
        return this.isRampaging;
    }

    public java.util.UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public boolean recall(Player player) {
        if (this.level().isClientSide()) return false;
        if (this.isRampaging) {
            player.sendSystemMessage(Component.literal("§c⚠️ Cứu Thế Quyền Hiện đã bạo tẩu mất kiểm soát! Không thể thu hồi bằng tượng!"));
            return false;
        }
        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.8F);
        sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.4, 0.4, 0.4, 0.05);

        ItemStack sealed = new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get());
        if (!player.getInventory().add(sealed)) {
            player.drop(sealed, false);
        }
        player.sendSystemMessage(Component.literal("§a§l[THU HỒI AN TOÀN] §fĐã thu hồi Cứu Thế Quyền Hiện trở lại Tượng Lửng Gốm Shigaraki!"));
        this.discard();
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TimeSlashGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, LivingEntity.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Căm thù tất cả sinh vật sống (quét mỗi 20 ticks = 1s để giảm tải CPU)
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 20, true, false,
                e -> {
                    if (e instanceof CuuTheQuyenHienEntity || e instanceof CuuTheSoulEntity || !e.isAlive()) return false;
                    // Khi đang được người chơi triệu hồi kiểm soát (chưa bạo tẩu)
                    if (this.ownerUuid != null && !this.isRampaging) {
                        // Tuyệt đối không tấn công chủ nhân
                        if (e.getUUID().equals(this.ownerUuid)) return false;
                        // Không tấn công thú cưng của chủ nhân
                        if (e instanceof net.minecraft.world.entity.TamableAnimal ta && this.ownerUuid.equals(ta.getOwnerUUID())) return false;
                    }
                    return true;
                }
        ));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.THIEN_MA_KATANA.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
        return data;
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            // Tỷ lệ xuất hiện tự nhiên hiếm (3%) - Kiểm tra trước để tránh quét thực thể/chunks nặng nề
            if (this.random.nextFloat() > 0.03F) {
                return false;
            }

            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;

                AABB checkArea = new AABB(this.blockPosition()).inflate(64.0D);
                if (!sl.getEntitiesOfClass(CuuTheQuyenHienEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }
        }
        return super.checkSpawnRules(level, spawnType);
    }

    public void stun(int ticks) {
        this.stunTicks = Math.max(this.stunTicks, ticks);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public boolean isStunned() {
        return this.stunTicks > 0;
    }

    @Override
    public void aiStep() {
        if (this.stunTicks > 0) {
            this.stunTicks--;
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide() && this.random.nextFloat() < 0.25F) {
                this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY() + 2.0D, this.getZ(), 0, 0.1, 0);
            }
            return;
        }

        if (this.timeSlashCooldown > 0) this.timeSlashCooldown--;
        if (this.antiAirCooldown > 0) this.antiAirCooldown--;
        if (this.spatialSlashCooldown > 0) this.spatialSlashCooldown--;
        if (this.gravitationalCrushCooldown > 0) this.gravitationalCrushCooldown--;
        if (this.vajraAegisCooldown > 0) this.vajraAegisCooldown--;
        if (this.vajraAegisDuration > 0) this.vajraAegisDuration--;

        if (this.actionTicks > 0) {
            this.actionTicks--;
            if (this.actionTicks <= 0) {
                this.entityData.set(ACTION_STATE, (byte) 0);
            }
        }

        super.aiStep();

        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();

        // 0. Quản lý thời gian kiểm soát khi được người chơi triệu hồi (90 giây)
        if (this.summonTicks > 0) {
            this.summonTicks--;

            if (this.tickCount % 20 == 0 && this.ownerUuid != null) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(this.ownerUuid);
                if (owner != null && owner.distanceToSqr(this) <= 64.0D * 64.0D) {
                    int secs = this.summonTicks / 20;
                    owner.displayClientMessage(
                            Component.literal("§6§l[KIỂM SOÁT CỨU THẾ]: §e" + secs + "s §7| Chuột phải Tượng Rỗng vào hắn để thu hồi!"),
                            true
                    );
                    if (secs <= 10 && secs > 0) {
                        owner.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.2F, 1.8F);
                    }
                }
            }

            if (this.summonTicks == 0) {
                triggerRampage(sl);
            }
        }

        // Hỗ trợ chiến đấu cho chủ nhân khi chưa bạo tẩu
        if (this.ownerUuid != null && !this.isRampaging) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (owner != null && (this.getTarget() == null || !this.getTarget().isAlive())) {
                LivingEntity revenge = owner.getLastHurtByMob();
                if (revenge != null && revenge.isAlive() && revenge != this && !(revenge instanceof CuuTheQuyenHienEntity)) {
                    this.setTarget(revenge);
                } else {
                    LivingEntity attacking = owner.getLastHurtMob();
                    if (attacking != null && attacking.isAlive() && attacking != this && !(attacking instanceof CuuTheQuyenHienEntity)) {
                        this.setTarget(attacking);
                    }
                }
            }
        }

        // 1. NỘI TẠI PHẬT MA TỰ DŨ (Hồi phục siêu tốc mỗi giây khi bị mất máu)
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            float healAmount = (this.vajraAegisDuration > 0) ? 30.0F : 15.0F;
            this.heal(healAmount);
            if (this.random.nextFloat() < 0.45F) {
                sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.2D, this.getZ(), 5, 0.4D, 0.6D, 0.4D, 0.02D);
                sl.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.2D, this.getZ(), 8, 0.4D, 0.6D, 0.4D, 0.05D);
            }
        }

        // 2. Ưu tiên chém Thị Thực trong phạm vi 24 blocks
        if (this.tickCount % 10 == 0) {
            var nearbyFlesh = SeerFleshAbility.getNearbyFlesh(sl, this.position(), 24.0D);
            if (nearbyFlesh != null) {
                double distFleshSq = this.position().distanceToSqr(nearbyFlesh.spawnPos);
                if (distFleshSq <= 3.8D * 3.8D) {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.8F);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, nearbyFlesh.spawnPos.x, nearbyFlesh.spawnPos.y + 1.0D, nearbyFlesh.spawnPos.z, 4, 0.3D, 0.3D, 0.3D, 0.0D);
                    SeerFleshAbility.destroyFlesh(nearbyFlesh);
                } else {
                    this.getNavigation().moveTo(nearbyFlesh.spawnPos.x, nearbyFlesh.spawnPos.y, nearbyFlesh.spawnPos.z, 1.35D);
                }
            }
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double distSq = this.distanceToSqr(target);

            // 3. Kỹ năng Không Chiến (Anti-Air): Kéo giật các sinh vật bay rớt xuống đất
            if (this.antiAirCooldown <= 0) {
                double dy = target.getY() - this.getY();
                double distHorizSq = (target.getX() - this.getX()) * (target.getX() - this.getX()) + (target.getZ() - this.getZ()) * (target.getZ() - this.getZ());
                if (dy >= 2.0D && dy <= 16.0D && distHorizSq <= 14.0D * 14.0D) {
                    executeAntiAirPullDown(sl, target);
                    this.antiAirCooldown = 40;
                }
            }

            // 4. Bát Nhã Kim Cang Thể: Kích hoạt khi HP <= 55% hoặc ở dạng tay không
            if (this.vajraAegisCooldown <= 0 && this.vajraAegisDuration <= 0 && (this.getHealth() <= this.getMaxHealth() * 0.55F || this.isBareHanded())) {
                activateVajraAegis(sl);
                this.vajraAegisCooldown = 360;
            }
            // 5. Thiên Ngoại Thần Áp: Kích hoạt khi cận chiến (distSq <= 10x10)
            else if (this.gravitationalCrushCooldown <= 0 && distSq <= 10.0D * 10.0D) {
                executeGravitationalCrush(sl);
                this.gravitationalCrushCooldown = 240;
            }
            // 6. Không Gian Trảm: Vung đao chém sóng chân không tầm xa (6 - 18 blocks)
            else if (this.spatialSlashCooldown <= 0 && distSq >= 6.0D * 6.0D && distSq <= 18.0D * 18.0D && this.hasLineOfSight(target)) {
                executeSpatialSlash(sl, target);
                this.spatialSlashCooldown = 160;
            }
        }
    }

    public void activateVajraAegis(ServerLevel sl) {
        this.setActionState((byte) 3, 30);
        this.vajraAegisDuration = 120; // Duy trì 6s

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5F, 1.4F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2.5F, 1.2F);

        sl.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.2D, this.getZ(), 30, 0.6D, 0.8D, 0.6D, 0.1D);
        sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 1.2D, this.getZ(), 15, 0.5D, 0.7D, 0.5D, 0.05D);

        LivingEntity target = this.getTarget();
        if (target instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§e§l[CHIẾN Ý] §6Cứu Thế Quyền Hiện kích hoạt Bát Nhã Kim Cang Thể! Giáp tăng vọt, phản sát thương và siêu tốc hồi phục!"));
        }
    }

    public void executeGravitationalCrush(ServerLevel sl) {
        this.setActionState((byte) 2, 22);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.5F, 0.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0F, 0.7F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.8F);

        // Vòng sóng xung kích trọng lực tối ưu (3 vòng, mỗi vòng 8 điểm = 24 hạt nhẹ nhàng)
        double[] radii = {3.5D, 7.0D, 10.0D};
        for (double r : radii) {
            int points = 8;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double px = this.getX() + r * Math.cos(angle);
                double pz = this.getZ() + r * Math.sin(angle);
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, this.getY() + 0.1D, pz, 1, 0, 0.05D, 0, 0.01D);
            }
        }

        AABB crushArea = this.getBoundingBox().inflate(10.0D, 4.0D, 10.0D);
        for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, crushArea)) {
            if (victim == this || victim instanceof CuuTheSoulEntity || !victim.isAlive()) continue;
            if (this.ownerUuid != null && !this.isRampaging && victim.getUUID().equals(this.ownerUuid)) continue;

            victim.hurt(this.damageSources().mobAttack(this), 40.0F);
            // Kéo ép rạp xuống đất, không thể nhảy
            victim.setDeltaMovement(victim.getDeltaMovement().x * 0.1D, -1.2D, victim.getDeltaMovement().z * 0.1D);
            victim.hasImpulse = true;
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 128, false, false, false));

            if (victim instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§4§l[CẢNH BÁO] §cThiên Ngoại Thần Áp giáng lâm! Bạn bị đè rạp xuống mặt đất không thể nhảy!"));
            }
        }
    }

    public void executeSpatialSlash(ServerLevel sl, LivingEntity target) {
        this.setActionState((byte) 1, 16);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.6F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.6F, 1.8F);

        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 dir = end.subtract(start).normalize();
        double maxDist = Math.min(18.0D, start.distanceTo(end) + 2.0D);

        // Hiệu ứng hạt dọc quỹ đạo trảm kích (bước nhảy 2 blocks tiết kiệm CPU)
        for (double d = 1.0D; d <= maxDist; d += 2.0D) {
            Vec3 pos = start.add(dir.scale(d));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0.1D, 0.1D, 0.1D, 0.02D);
        }

        // Tối ưu hóa: 1 truy vấn AABB duy nhất dọc quỹ đạo thay vì 18 lần lặp
        AABB lineBox = new AABB(start, start.add(dir.scale(maxDist))).inflate(1.5D);
        java.util.Set<UUID> hitIds = new java.util.HashSet<>();
        for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, lineBox)) {
            if (hit == this || hit instanceof CuuTheSoulEntity || !hit.isAlive()) continue;
            if (this.ownerUuid != null && !this.isRampaging && hit.getUUID().equals(this.ownerUuid)) continue;
            if (!hitIds.add(hit.getUUID())) continue;

            hit.hurt(this.damageSources().mobAttack(this), 55.0F);
            // Hất tung lên trời
            hit.setDeltaMovement(dir.x * 0.15D, 0.65D, dir.z * 0.15D);
            hit.hasImpulse = true;
            hit.hurtMarked = true;

            if (hit instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c§l[CẢNH BÁO] §fBạn bị Không Gian Trảm của Cứu Thế Quyền Hiện hất tung!"));
            }
        }
    }

    private void executeAntiAirPullDown(ServerLevel sl, LivingEntity target) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5F, 1.2F);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0F, 1.6F);

        Vec3 diff = target.position().subtract(this.position());
        double steps = 8.0D;
        for (int i = 0; i <= steps; i++) {
            Vec3 p = this.position().add(diff.scale(i / steps)).add(0, 1.2D, 0);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }

        float dmg = isBareHanded() ? 50.0F : 38.0F;
        target.hurt(this.damageSources().mobAttack(this), dmg);

        target.setDeltaMovement(target.getDeltaMovement().x * 0.2D, -1.35D, target.getDeltaMovement().z * 0.2D);
        target.hurtMarked = true;
        target.hasImpulse = true;

        if (target instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§c§l[CẢNH BÁO] §fTrảm kích chân không của Cứu Thế Quyền Hiện kéo bạn rơi thẳng xuống đất!"));
        }
    }

    private void triggerRampage(ServerLevel sl) {
        this.isRampaging = true;
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5F, 0.6F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.2D, this.getZ(), 30, 0.5D, 0.5D, 0.5D, 0.1D);

        if (this.ownerUuid != null) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (owner != null) {
                owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 50, 15));
                owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("§4§l【 CẢNH BÁO PHẢN PHỆ 】")));
                owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("§cCứu Thế Quyền Hiện đã mất kiểm soát và đồ sát bạn!")));
                owner.sendSystemMessage(Component.literal("§4§l⚠️ [PHẢN PHỆ NGUYÊN TÁC] §cHết thời gian kiểm soát 90 giây! Cứu Thế Quyền Hiện đã bạo tẩu cuồng nộ và quay sang đồ sát bạn!"));
                owner.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5F, 0.8F);
                this.setTarget(owner);
            }
        }
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(ModItems.SHIGARAKI_TANUKI_EMPTY.get())) {
            if (this.level().isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;

            if (this.isOwnedBy(player) && !this.isRampaging()) {
                this.recall(player);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }

            boolean eligible = this.getHealth() <= (this.getMaxHealth() * 0.20F) || this.isStunned();
            if (!eligible) {
                player.sendSystemMessage(Component.literal("§c⚠️ Cứu Thế Quyền Hiện còn quá mạnh (§e" + (int) this.getHealth() + "§c/§e" + (int) this.getMaxHealth() + " HP§c)! Cần làm suy yếu xuống dưới 20% HP (<= 300 HP) hoặc làm choáng bằng Tiên Tri để phong ấn!"));
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5F, 0.8F);
                return net.minecraft.world.InteractionResult.FAIL;
            }

            ServerLevel sl = (ServerLevel) this.level();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.2F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3.0F, 0.7F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.8F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.8F);

            for (int i = 0; i < 35; i++) {
                double ox = (sl.random.nextDouble() - 0.5D) * 2.0D;
                double oy = sl.random.nextDouble() * 2.5D;
                double oz = (sl.random.nextDouble() - 0.5D) * 2.0D;
                sl.sendParticles(ParticleTypes.SOUL, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 2, 0, 0.1D, 0, 0.05D);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.2D, this.getZ(), 1, 0, 0, 0, 0);
            }

            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 50, 15));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("§4§l【 封印完了 】")));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("§a✦ Đã Phong Ấn Cứu Thế Quyền Hiện Vào Tượng Gốm Shigaraki! ✦")));
                sp.sendSystemMessage(Component.literal("§a§l[PHONG ẤN THÀNH CÔNG] §fLinh hồn Thiên Ngoại Cứu Thế Quyền Hiện đã bị thu phục và phong ấn vào Tượng Lửng Gốm Shigaraki!"));
            }

            this.discard();
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            ItemStack sealed = new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get());
            if (!player.getInventory().add(sealed)) {
                player.drop(sealed, false);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BareHanded", this.isBareHanded());
        tag.putInt("SummonTicks", this.summonTicks);
        tag.putBoolean("IsRampaging", this.isRampaging);
        tag.putInt("SlashStrikeMode", this.slashStrikeMode);
        if (this.ownerUuid != null) {
            tag.putUUID("OwnerUuid", this.ownerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BareHanded")) {
            this.setBareHanded(tag.getBoolean("BareHanded"));
        }
        if (tag.contains("SummonTicks")) {
            this.summonTicks = tag.getInt("SummonTicks");
        }
        if (tag.contains("IsRampaging")) {
            this.isRampaging = tag.getBoolean("IsRampaging");
        }
        if (tag.contains("SlashStrikeMode")) {
            this.slashStrikeMode = tag.getInt("SlashStrikeMode");
        }
        if (tag.hasUUID("OwnerUuid")) {
            this.ownerUuid = tag.getUUID("OwnerUuid");
        }
    }

    public void tryDodgeDragonNova(Vec3 casterPos, Vec3 lookVec) {
        if (this.isStunned()) return;
        Vec3 perp = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        double side = (this.random.nextBoolean() ? 1.8D : -1.8D);
        Vec3 dodgeVec = perp.scale(side).add(0, 0.45D, 0);
        this.setDeltaMovement(dodgeVec);
        this.hasImpulse = true;

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0D, this.getZ(), 4, 0.5D, 0.5D, 0.5D, 0.0D);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.05D);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.8F, 1.4F);
        }
    }

    public void tryDodgeGranite(Vec3 origin, Vec3 dir) {
        if (this.isStunned()) return;
        Vec3 perp = dir.cross(new Vec3(0, 1, 0)).normalize();
        double side = (this.random.nextBoolean() ? 1.6D : -1.6D);
        Vec3 dodgeVec = perp.scale(side).add(0, 0.35D, 0);
        this.setDeltaMovement(dodgeVec);
        this.hasImpulse = true;

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.05D);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 1.6F);
        }
    }

    public boolean handleExtinctionStarHit(ServerPlayer shooter, Vec3 origin, Vec3 look) {
        Level lvl = this.level();
        if (!(lvl instanceof ServerLevel sl)) return false;

        if (!this.isBareHanded()) {
            Vec3 bossLook = this.getLookAngle();
            Vec3 toShooter = origin.subtract(this.position()).normalize();
            double dot = bossLook.dot(toShooter);

            if (dot > 0.0D) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 2.5F, 1.5F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.0F, 0.6F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0F, 1.2F);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.2D, this.getZ(), 6, 0.6D, 0.6D, 0.6D, 0.0D);
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2D, this.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
                shooter.sendSystemMessage(Component.literal("§c§l[Báo cáo] §fCá thể Cứu Thế Quyền Hiện đã chặn được đòn tấn công!"));
                return true;
            } else {
                return false;
            }
        } else {
            Vec3 perp = look.cross(new Vec3(0, 1, 0)).normalize();
            double side = (this.random.nextBoolean() ? 2.2D : -2.2D);
            this.setDeltaMovement(perp.scale(side).add(0, 0.5D, 0));
            this.hasImpulse = true;

            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 1.5F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 1.8F);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.1D);
            sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
            shooter.sendSystemMessage(Component.literal("§c§l[Báo cáo] §fCá thể Cứu Thế Quyền Hiện đã né tránh được đòn tấn công!"));
            return true;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        Entity attacker = source.getEntity();

        // 1. Tuyệt đối đánh không lại Milim, Velgrynd, Velzard: Chịu sát thương x5
        if (attacker instanceof MilimEntity || attacker instanceof VelgryndEntity || attacker instanceof VelzardEntity) {
            amount *= 5.0F;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 1.4F);
            }
            return super.hurt(source, amount);
        }

        // 2. Kháng 70% sát thương từ Ác Ma Thủy Tổ (Primordial Demon) -> chỉ chịu 30% ảnh hưởng!
        if (attacker instanceof PrimordialDemonEntity) {
            amount *= 0.30F;
        }

        // 3. Kháng 90% Linh Tử Băng Hoại (Disintegration)
        if (this.getTags().contains("DisintegrationDamage")) {
            if (this.random.nextFloat() < 0.90F) {
                if (this.level() instanceof ServerLevel sl) {
                    SanctuaryDisintegrationAbility.cancelNearbySanctuaries(sl, this.position(), 16.0D);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0F, 0.7F);
                }
                return false;
            }
        }

        // 4. Kháng 70% Tà Khứ Vũ Thê Tử (Jacob's Ladder)
        if (this.getTags().contains("JacobsLadderDamage")) {
            amount *= 0.30F;
        }

        // 5. Kháng 30% Cú Bắn Granit & Zone Track
        if (this.getTags().contains("GraniteBlastDamage") || this.getTags().contains("ZoneTrackDamage")) {
            amount *= 0.70F;
        }

        // 6. Ở Dạng Tay Không (Brawler Form): Giảm 50% sát thương từ người chơi
        if (this.isBareHanded() && attacker instanceof Player) {
            amount *= 0.50F;
        }

        // 7. Bát Nhã Kim Cang Thể: Giảm 50% sát thương và phản chấn 30% sát thương cận chiến
        if (this.vajraAegisDuration > 0) {
            amount *= 0.50F;
            if (attacker instanceof LivingEntity livingAttacker && attacker != this) {
                livingAttacker.hurt(this.damageSources().thorns(this), amount * 0.30F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.playSound(null, livingAttacker.getX(), livingAttacker.getY(), livingAttacker.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.5F, 1.5F);
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Tuyệt đối không thể gây sát thương đánh bại Milim, Velgrynd, Velzard
        if (target instanceof MilimEntity || target instanceof VelgryndEntity || target instanceof VelzardEntity) {
            this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.5F, 1.6F);
            return false;
        }

        // Đấu với Ác Ma Thủy Tổ: Tỉ lệ thắng 70% áp đảo
        if (target instanceof PrimordialDemonEntity demon) {
            float demonDamage = 65.0F + (this.random.nextFloat() * 20.0F); // 65 - 85 damage
            this.heal(demonDamage * 0.10F); // Hút 10% sinh lực
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, demon.getX(), demon.getY() + 1.2D, demon.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.1D);
                sl.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.8F, 0.8F);
            }
            return demon.hurt(this.damageSources().mobAttack(this), demonDamage);
        }

        // Hồi phục 10% sinh lực khi tấn công trúng mục tiêu
        this.heal(3.5F);

        // Tương tác với Đa Trùng Kết Giới (Multilayer Barrier):
        if (target instanceof LivingEntity livingTarget && this.level() instanceof ServerLevel sl) {
            if (MultilayerBarrierAbility.isDamageBlockedByBarrier(livingTarget, this.damageSources().mobAttack(this))) {
                if (this.isBareHanded()) {
                    this.barrierPunchCount++;
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.8F, 0.8F);
                    sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2D, target.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.1D);

                    if (this.barrierPunchCount >= 3) {
                        this.setBareHanded(false);
                        this.barrierPunchCount = 0;
                        MultilayerBarrierAbility.shatterBarrierNear(sl, target.position(), 5.0D, "Thiên Ma Kiếm của Cứu Thế Quyền Hiện");
                        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 2.5F, 0.8F);
                        if (target instanceof ServerPlayer sp) {
                            sp.sendSystemMessage(Component.literal("§c§l[CẢNH BÁO] §fCứu Thế Quyền Hiện đã rút kiếm và một đao chém vỡ Đa Trùng Kết Giới!"));
                        }
                        return livingTarget.hurt(this.damageSources().mobAttack(this), 55.0F);
                    }
                    return false;
                } else {
                    MultilayerBarrierAbility.shatterBarrierNear(sl, target.position(), 5.0D, "Thiên Ma Kiếm của Cứu Thế Quyền Hiện");
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 2.5F, 0.8F);
                    if (target instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§c§l[CẢNH BÁO] §fCứu Thế Quyền Hiện đã một đao chém vỡ Đa Trùng Kết Giới!"));
                    }
                    return livingTarget.hurt(this.damageSources().mobAttack(this), 45.0F);
                }
            }
        }

        return super.doHurtTarget(target);
    }

    // =========================================================================
    // AI GOAL: TUYỆT KĨ CHÉM XUYÊN THỜI GIAN (TIME-SLASH GOAL)
    // =========================================================================
    public static class TimeSlashGoal extends Goal {
        private final CuuTheQuyenHienEntity mob;
        private int castTicks = 0;
        private static final int CAST_DURATION = 35; // Bản thể đứng yên 1.75s

        public TimeSlashGoal(CuuTheQuyenHienEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.isStunned() || mob.timeSlashCooldown > 0) return false;
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;
            double distSq = mob.distanceToSqr(target);
            return distSq >= 3.0D * 3.0D && distSq <= 24.0D * 24.0D && mob.hasLineOfSight(target);
        }

        @Override
        public void start() {
            this.castTicks = CAST_DURATION;
            mob.getNavigation().stop();
            mob.setDeltaMovement(Vec3.ZERO);

            Level level = mob.level();
            if (!(level instanceof ServerLevel sl)) return;

            LivingEntity target = mob.getTarget();
            if (target == null) return;

            sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.0F, 1.8F);
            sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5F, 0.6F);

            // 1. Phóng xuất Linh Hồn với Tam Thức Luân Chuyển
            int mode = mob.getSlashStrikeMode();
            CuuTheSoulEntity soul = new CuuTheSoulEntity(ModEntities.CUU_THE_SOUL.get(), level);
            soul.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
            soul.initStrike(mob.getUUID(), target.getUUID(), mode);
            sl.addFreshEntity(soul);
            mob.cycleSlashStrikeMode();

            // 2. Tương tác đặc biệt với Tiên Tri:
            for (ServerPlayer player : sl.players()) {
                if (player.distanceToSqr(mob) <= 30.0D * 30.0D) {
                    if (RykuzinSinkaAbility.isActive(player)) {
                        ModMessages.sendToPlayer(new ClientboundTengaiSlashEffectPacket(60), player);
                        String modeDesc = switch (mode) {
                            case 1 -> "§5Đoạt Não Trảm! (Cẩn thận mù lòa, choáng váng!)";
                            case 2 -> "§4§lTuyệt Mệnh Song Trảm (Cả Tim Lẫn Não)!";
                            default -> "§cĐoạt Tâm Trảm (Rơi Tim)!";
                        };
                        player.displayClientMessage(
                                Component.literal("§e§l[TIÊN TRI] §c⚠️ Phát hiện Linh Hồn Cứu Thế Quyền Hiện xuất chiêu " + modeDesc + " Hãy né hoặc đánh tan linh hồn!"),
                                true
                        );
                        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.4F, 1.6F);
                    }
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.isStunned() && this.castTicks > 0;
        }

        @Override
        public void tick() {
            this.castTicks--;
            mob.setDeltaMovement(Vec3.ZERO);
            mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            mob.timeSlashCooldown = 300; // 15s hồi chiêu
        }
    }
}
