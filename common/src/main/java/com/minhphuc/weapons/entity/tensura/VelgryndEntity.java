package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Chước Nhiệt Long Velgrynd (Scorch Dragon Velgrynd)
 * Long Chủng tối thượng thời cổ đại từ Tensei Shitara Slime Datta Ken:
 * - Vũ khí Quạt Lông Vũ Long Chủng xuất hiện theo xác suất khi tấn công.
 * - 4 Đại Tuyệt Kỹ: Thao Túng Thời Không, Phi Đao, Gia Tốc Chước Nhiệt Long, Tồn Tại Song Song (1v1).
 * - Cơ chế 4 đòn đánh để hạ gục (Trừ Long Tinh Bộc Viêm Bá diệt ngay lập tức).
 * - Cứ 3 đòn đánh trúng người chơi mặc Giáp Thần sẽ xuyên giáp gây 30% Máu Tối Đa.
 * - Cân bằng 50/50 độc nhất với Ma Thần Guy Crimson (Rouge Thể Xác & Tên), áp đảo hoàn toàn các ác ma khác.
 */
public class VelgryndEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_HOLDING_FAN =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CASTING_STATE =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DRAGON_LAYERS =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_CLONE =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_ALLIED =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> DATA_IS_FLYING =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_ENRAGED =
            SynchedEntityData.defineId(VelgryndEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent;

    // Bộ đếm hồi chiêu kỹ năng
    private int timeCollapseCooldown = 200;
    private int heatBladeCooldown = 80;
    private int cardinalAccelCooldown = 280;
    private int meteorRainCooldown = 260; // 13s cooldown (Thiên Hỏa Lạc Lôi)
    private int meteorTelegraphTicks = 0; // 30 ticks (1.5s telegraph)
    private final List<Vec3> meteorTargetPositions = new ArrayList<>();
    private int alliedSafeTicks = 0;
    private int alliedPlayerSkillCooldown = 60;

    // Quản lý trạng thái đang thi triển kỹ năng
    private int activeSkillTicks = 0;
    private int activeSkillId = 0; // 1 = Time, 2 = Blade, 3 = Accel, 5 = Meteor
    private Vec3 accelDirection = null;

    // Quản lý tấn công nhiều đối thủ & Tồn Tại Song Song
    private final Map<UUID, Long> recentAttackers = new HashMap<>();
    private UUID boundTargetUuid = null; // Dành cho bản sao Tồn Tại Song Song
    private VelgryndEntity activeClone = null; // Bản sao đang hoạt động của chân thân

    // Bộ đếm đòn đánh lên từng người chơi có giáp thần (mỗi 3 đòn -> 30% true damage)
    private final Map<UUID, Integer> playerArmorHitCounters = new HashMap<>();

    public VelgryndEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§c§l[CHƯỚC NHIỆT LONG] §6§lVELGRYND §7- §e[Long Chủng Tối Thượng]"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        this.moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 18, true);
        this.setNoGravity(false);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        net.minecraft.world.entity.ai.navigation.FlyingPathNavigation nav = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.ATTACK_DAMAGE, 55.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            // 1. TUYỆT ĐỐI KHÔNG spawn dưới Địa Ngục (Nether) hoặc chiều không gian không phải Overworld
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) {
                    return false;
                }
                // Tối ưu chống lag: Kiểm tra trong phạm vi 128 blocks nếu đã có Velgrynd thì không spawn thêm
                AABB checkArea = new AABB(this.blockPosition()).inflate(128.0D);
                if (!sl.getEntitiesOfClass(VelgryndEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }
            // 2. Phải là vị trí lộ thiên ngoài trời (thấy bầu trời, Y >= 60)
            BlockPos pos = this.blockPosition();
            if (!level.canSeeSky(pos) || pos.getY() < 60) {
                return false;
            }
            // 3. Tỷ lệ xuất hiện tự nhiên tương đương Ác Ma Thủy Tổ (4% cơ hội khi chọn)
            if (this.random.nextFloat() > 0.04F) {
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
        }
        return spawnData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HOLDING_FAN, true);
        builder.define(DATA_CASTING_STATE, 0);
        builder.define(DATA_DRAGON_LAYERS, 4); // 4 tầng hộ thể
        builder.define(DATA_IS_CLONE, false);
        builder.define(DATA_IS_ALLIED, false);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_IS_FLYING, false);
        builder.define(DATA_IS_ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new VelgryndCombatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !this.isAllied()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PrimordialDemonEntity.class, 10, true, false, e -> !this.isAllied()));
    }

    public boolean isAllied() {
        return this.entityData.get(DATA_IS_ALLIED);
    }

    public void setAllied(boolean allied) {
        this.entityData.set(DATA_IS_ALLIED, allied);
    }

    private UUID cachedOwnerUuid = null;

    public String getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    public UUID getParsedOwnerUUID() {
        if (this.cachedOwnerUuid == null) {
            String s = getOwnerUUID();
            if (s != null && !s.isEmpty()) {
                try {
                    this.cachedOwnerUuid = UUID.fromString(s);
                } catch (Exception ignored) {}
            }
        }
        return this.cachedOwnerUuid;
    }

    public void setOwnerUUID(String uuid) {
        this.entityData.set(DATA_OWNER_UUID, uuid != null ? uuid : "");
        this.cachedOwnerUuid = null;
    }

    public void setAlliedSummon(ServerPlayer owner) {
        this.setAllied(true);
        if (owner != null) {
            this.setOwnerUUID(owner.getStringUUID());
        }
        this.bossEvent.setName(Component.literal("§6§l[ĐỒNG MINH] §c§lVELGRYND §7- §e[Long Chủng Hộ Vệ]"));
        this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!this.level().isClientSide() && hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (this.isAllied()) {
                player.displayClientMessage(Component.literal("§6§l╔════════════════════════════════════════════════╗"), false);
                player.displayClientMessage(Component.literal("§6§l║      §c§l✦ CHƯỚC NHIỆT LONG VELGRYND (ĐỒNG MINH) ✦      §6§l║"), false);
                player.displayClientMessage(Component.literal("§6§l╠════════════════════════════════════════════════╣"), false);
                player.displayClientMessage(Component.literal("§e  Chủng Tộc: §fLong Chủng Tối Thượng (True Dragon - Đệ Tam Long)"), false);
                player.displayClientMessage(Component.literal("§c  Sinh Lực (HP): §a5,000 / 5,000 HP §7(Bất tử trước phàm nhân)"), false);
                player.displayClientMessage(Component.literal("§d  Ma Tố Lượng (EP): §b74,350,000 EP §7(Áp đảo Tuyệt Đối Ma Thần)"), false);
                player.displayClientMessage(Component.literal("§6  Quyền Năng Bản Thể: §eThao Túng Thời Không, Gia Tốc Cardinal, Tồn Tại Song Song"), false);
                player.displayClientMessage(Component.literal("§5  Quyền Năng Kế Thừa: §dLong Tinh Bộc Viêm Bá, Bạo Thực Vương, Tuyệt Diệt Tinh Tú"), false);
                player.displayClientMessage(Component.literal("§a  Trạng Thái: §fĐang bảo vệ Chủ Nhân và càn quét mọi hiểm họa xung quanh!"), false);
                player.displayClientMessage(Component.literal("§6§l╚════════════════════════════════════════════════╝"), false);

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.5F, 1.2F);
                return net.minecraft.world.InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.literal("§c§l╔════════════════════════════════════════════════╗"), false);
                player.displayClientMessage(Component.literal("§c§l║    §4§l✦ CHƯỚC NHIỆT LONG VELGRYND ✦    §c§l║"), false);
                player.displayClientMessage(Component.literal("§c§l╠════════════════════════════════════════════════╣"), false);
                player.displayClientMessage(Component.literal("§e  Chủng Tộc: §fLong Chủng Tối Thượng (True Dragon - Đệ Tam Long)"), false);
                player.displayClientMessage(Component.literal("§c  Sinh Lực (HP): §a5,000 / 5,000 HP §7(" + this.getDragonLayers() + "/4 Tầng Vảy Rồng Còn Lại)"), false);
                player.displayClientMessage(Component.literal("§d  Ma Tố Lượng (EP): §b74,350,000 EP §7(Đỉnh Cao Tensura)"), false);
                player.displayClientMessage(Component.literal("§6  Quyền Năng: §eThao Túng Thời Không, Gia Tốc Cardinal, Chước Liệt Tiệt Đoán, Tồn Tại Song Song"), false);
                player.displayClientMessage(Component.literal("§4  Cơ Chế: §cĐòn đánh thứ 3 xuyên 30% Máu Giáp Thần Thánh; Miễn nhiễm sát thương thường!"), false);
                player.displayClientMessage(Component.literal("§c§l╚════════════════════════════════════════════════╝"), false);

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.6F, 1.4F);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    public boolean isHoldingFan() {
        return this.entityData.get(DATA_HOLDING_FAN);
    }

    public void setHoldingFan(boolean holding) {
        this.entityData.set(DATA_HOLDING_FAN, holding);
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

    public int getCastingState() {
        return this.entityData.get(DATA_CASTING_STATE);
    }

    public void setCastingState(int state) {
        this.entityData.set(DATA_CASTING_STATE, state);
    }

    public int getDragonLayers() {
        return this.entityData.get(DATA_DRAGON_LAYERS);
    }

    public void setDragonLayers(int layers) {
        this.entityData.set(DATA_DRAGON_LAYERS, Math.max(0, layers));
        float progress = Math.max(0.0F, Math.min(1.0F, this.entityData.get(DATA_DRAGON_LAYERS) / 4.0F));
        this.bossEvent.setProgress(progress);
    }

    public boolean isClone() {
        return this.entityData.get(DATA_IS_CLONE);
    }

    public void setClone(boolean clone) {
        this.entityData.set(DATA_IS_CLONE, clone);
        if (clone) {
            this.bossEvent.setName(Component.literal("§c§l[TỒN TẠI SONG SONG] §6§lVELGRYND §7- §e[Bản Sao Long Chủng]"));
        }
    }

    public void setBoundTarget(LivingEntity target) {
        if (target != null) {
            this.boundTargetUuid = target.getUUID();
            this.setTarget(target);
        }
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

    public void broadcastDialogue(String message) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Component comp = Component.literal("§c§l[Chước Nhiệt Long Velgrynd] §f\"" + message + "\"");
            for (ServerPlayer p : serverLevel.players()) {
                if (p.distanceToSqr(this) <= 50.0D * 50.0D) {
                    p.displayClientMessage(comp, false);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            // Client flame aura particles
            if (this.random.nextFloat() <= 0.4F) {
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D,
                        this.getY() + this.random.nextDouble() * 1.8D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D,
                        0, 0.04D, 0);
            }
            int casting = this.getCastingState();
            if (casting > 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        this.getY() + this.random.nextDouble() * 2.0D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        0, 0.08D, 0);
            }
            if (casting == 4) {
                // Hiệu ứng tụ lực 7s cuộn xoáy trên Client
                double rad = 2.8D;
                for (int i = 0; i < 3; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2.0;
                    double px = this.getX() + Math.cos(angle) * rad;
                    double pz = this.getZ() + Math.sin(angle) * rad;
                    double py = this.getY() + 0.3D + this.random.nextDouble() * 2.2D;
                    this.level().addParticle(ParticleTypes.FLAME, px, py, pz,
                            (this.getX() - px) * 0.12D, 0.05D, (this.getZ() - pz) * 0.12D);
                    this.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz,
                            (this.getX() - px) * 0.1D, 0.04D, (this.getZ() - pz) * 0.1D);
                }
            }
            return;
        }

        ServerLevel sLevel = (ServerLevel) this.level();

        // Siêu Tốc Tái Sinh Long Chủng (True Dragon Ultraspeed Regeneration)
        if (this.tickCount % 20 == 0 && this.isAlive()) {
            if (this.getHealth() < this.getMaxHealth()) {
                float regenAmount = (this.getTarget() == null) ? 100.0F : 50.0F; // 50 HP/s in combat, 100 HP/s out of combat
                this.heal(regenAmount);
                sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2D, this.getZ(), 6, 0.4D, 0.6D, 0.4D, 0.05D);
            }
            // Long Chủng Hộ Vệ: Hồi phục sinh lực cho chủ nhân nếu là Đồng Minh
            if (this.isAllied() && getParsedOwnerUUID() != null) {
                try {
                    ServerPlayer owner = sLevel.getServer().getPlayerList().getPlayer(getParsedOwnerUUID());
                    if (owner != null && owner.isAlive() && owner.distanceToSqr(this) <= 16.0D * 16.0D && owner.getHealth() < owner.getMaxHealth()) {
                        owner.heal(5.0F);
                        sLevel.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 3, 0.3D, 0.5D, 0.3D, 0.05D);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Quản lý trạng thái bay lượn trên không trung
        LivingEntity curCombatTarget = this.getTarget();
        if (curCombatTarget != null && curCombatTarget.isAlive()) {
            setFlyingAnim(true);
            this.setNoGravity(true);
            double desiredY = curCombatTarget.getY() + 3.5D;
            if (this.getY() < desiredY) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04D, 0));
            }
        } else {
            setFlyingAnim(false);
            this.setNoGravity(false);
        }

        // Quản lý Nhiệt Huyết Bạo Tẩu (Phase 2 Enrage < 30% HP)
        boolean enraged = this.getHealth() <= (this.getMaxHealth() * 0.30F);
        if (enraged != isEnraged()) {
            setEnraged(enraged);
            if (enraged) {
                this.broadcastDialogue("Toàn bộ hỏa diễm bộc phát... Chước Nhiệt Thăng Hoa!");
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 1.2F);
            }
        }

        // 1. Kiểm tra bản sao Tồn Tại Song Song: Nếu mục tiêu bị ràng buộc đã chết/rời xa -> Tự tiêu biến
        if (this.isClone()) {
            if (this.boundTargetUuid != null) {
                Entity targetEntity = sLevel.getEntity(this.boundTargetUuid);
                if (targetEntity == null || !targetEntity.isAlive() || targetEntity.distanceToSqr(this) > 60.0D * 60.0D) {
                    this.broadcastDialogue("Kẻ ngáng đường đã biến mất. Nhiệm vụ phân thân đã hoàn tất!");
                    sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.05D);
                    sLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 80, 0.8D, 1.2D, 0.8D, 0.15D);
                    this.bossEvent.removeAllPlayers();
                    this.discard();
                    return;
                }
            }
        }

        // 1.5. Xử lý logic ĐỒNG MINH (Allied Summon từ Nghịch Lân)
        if (this.isAllied()) {
            ServerPlayer owner = null;
            UUID ownerUid = getParsedOwnerUUID();
            if (ownerUid != null) {
                try {
                    owner = sLevel.getServer().getPlayerList().getPlayer(ownerUid);
                } catch (Exception ignored) {}
            }

            // Tối ưu hiệu năng: Chỉ quét tìm mục tiêu mới mỗi 10 ticks hoặc khi mục tiêu hiện tại đã chết/null
            boolean needsTargetScan = (this.getTarget() == null || !this.getTarget().isAlive() || this.tickCount % 10 == 0);

            if (needsTargetScan) {
                Vec3 anchor = (owner != null) ? owner.position() : this.position();
                // Thu gọn phạm vi từ 96x40x96 xuống 56x24x56 để giảm 80% tải quét chunk
                AABB searchArea = new AABB(anchor.x - 28.0D, anchor.y - 12.0D, anchor.z - 28.0D,
                        anchor.x + 28.0D, anchor.y + 12.0D, anchor.z + 28.0D);

                final ServerPlayer finalOwner = owner;
                List<LivingEntity> hostileThreats = sLevel.getEntitiesOfClass(LivingEntity.class, searchArea, e -> {
                    if (e == this || e == finalOwner || !e.isAlive()) return false;
                    // TUYỆT ĐỐI KHÔNG TẤN CÔNG ÁC MA ĐÃ KÝ KHẾ ƯỚC CỦA CHỦ NHÂN
                    if (e instanceof PrimordialDemonEntity demon && demon.isTame()) {
                        if (finalOwner != null && demon.getOwnerUUID() != null && demon.getOwnerUUID().equals(finalOwner.getUUID())) {
                            return false;
                        }
                    }
                    return e instanceof Enemy || (finalOwner != null && ((e instanceof Mob mob && mob.getTarget() == finalOwner) || e.getLastHurtByMob() == finalOwner));
                });

                if (!hostileThreats.isEmpty()) {
                    this.alliedSafeTicks = 0;
                    this.setTarget(hostileThreats.get(0));
                } else if (this.getTarget() != null && !this.getTarget().isAlive()) {
                    this.setTarget(null);
                }
            }

            LivingEntity currentEnemy = this.getTarget();
            if (currentEnemy != null && currentEnemy.isAlive()) {
                this.alliedSafeTicks = 0;
                // Luân phiên thi triển kỹ năng tối thượng của người chơi
                if (this.alliedPlayerSkillCooldown > 0) {
                    this.alliedPlayerSkillCooldown--;
                } else if (this.getActiveSkillTicks() <= 0) {
                    this.alliedPlayerSkillCooldown = 75;
                    int roll = this.random.nextInt(3);
                    if (roll == 0) {
                        castAlliedDragonNova(currentEnemy);
                    } else if (roll == 1) {
                        castAlliedBeelzebuth(currentEnemy);
                    } else {
                        castAlliedExtinctionStars(currentEnemy);
                    }
                }
            } else {
                // Không có bất kỳ mối nguy hại nào quanh chủ nhân
                this.alliedSafeTicks++;
                if (this.alliedSafeTicks >= 100) { // 5 giây an toàn
                    this.broadcastDialogue("Toàn bộ mối nguy hại xung quanh ngươi đã bị ta tiêu diệt sạch sẽ! Lời thề Nghịch Lân kết thúc tại đây. Muốn gặp lại ta, hãy tìm Hạt Giống Long Chủng và đánh thắng ta một lần nữa!");
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 4.0F, 1.0F);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 3.0F, 1.0F);
                    sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.2D, this.getZ(), 100, 1.0D, 2.0D, 1.0D, 0.15D);
                    sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.2D, this.getZ(), 80, 1.0D, 2.0D, 1.0D, 0.1D);
                    this.bossEvent.removeAllPlayers();
                    this.discard();
                    return;
                }
            }
        }

        // 2. Dọn dẹp danh sách kẻ tấn công gần đây (>10 giây trước thì loại bỏ)
        long currentTickTime = this.level().getGameTime();
        recentAttackers.entrySet().removeIf(entry -> (currentTickTime - entry.getValue()) > 200L);

        // 3. Cơ chế TỒN TẠI SONG SONG (Nếu không phải bản sao và bị >= 2 đối thủ đánh - chạy mỗi 20 ticks thay vì mỗi tick)
        if (this.tickCount % 20 == 0 && !this.isClone() && (this.activeClone == null || !this.activeClone.isAlive())) {
            List<LivingEntity> validAttackers = new ArrayList<>();
            for (UUID uid : recentAttackers.keySet()) {
                Entity e = sLevel.getEntity(uid);
                if (e instanceof LivingEntity le && le.isAlive() && le.distanceToSqr(this) <= 40.0D * 40.0D) {
                    validAttackers.add(le);
                }
            }

            if (validAttackers.size() >= 2) {
                triggerParallelExistence(sLevel, validAttackers.get(1));
            }
        }

        // 4. Giảm hồi chiêu kỹ năng
        if (timeCollapseCooldown > 0) timeCollapseCooldown--;
        if (heatBladeCooldown > 0) heatBladeCooldown--;
        if (cardinalAccelCooldown > 0) cardinalAccelCooldown--;

        // 5. Cập nhật thi triển chiêu thức chủ động
        if (activeSkillTicks > 0) {
            tickActiveSkill(sLevel);
        }
    }

    private void triggerParallelExistence(ServerLevel sLevel, LivingEntity secondaryTarget) {
        this.broadcastDialogue("Dám ỷ đông hiếp yếu trước mặt Long Chủng? Nực cười! Hãy nếm trải TỒN TẠI SONG SONG để hiểu thế nào là 1 vs 1 công bằng!");
        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 1.2F);
        sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 2, 0.5D, 0.5D, 0.5D, 0.05D);
        sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.1D);

        VelgryndEntity clone = new VelgryndEntity(ModEntities.VELGRYND.get(), sLevel);
        clone.moveTo(this.getX() + 2.5D, this.getY(), this.getZ() + 2.5D, this.getYRot(), this.getXRot());
        clone.setClone(true);
        clone.setDragonLayers(this.getDragonLayers());
        clone.setBoundTarget(secondaryTarget);
        sLevel.addFreshEntity(clone);
        this.activeClone = clone;
    }

    private void tickActiveSkill(ServerLevel sLevel) {
        activeSkillTicks--;

        if (activeSkillId == 1) {
            // --- KỸ NĂNG 1: THAO TÚNG THỜI KHÔNG (SPACETIME COLLAPSE) ---
            if (activeSkillTicks == 45) {
                this.broadcastDialogue("Trước mặt Thời Không Thao Túng, vạn vật chỉ là tĩnh chỉ! Vỡ vụn đi!");
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 4.0F, 0.75F);
                com.minhphuc.weapons.content.tensura.LuciferReplicationAbility.recordSkillObserved(sLevel, this.position(), "SPACETIME_COLLAPSE", "Thao Túng Thời Không (Velgrynd)");
            }

            // Đóng băng thực thể trong 18m mỗi 8 ticks thay vì quét mỗi tick
            if (activeSkillTicks % 8 == 0) {
                AABB freezeBox = this.getBoundingBox().inflate(18.0D);
                List<LivingEntity> victims = sLevel.getEntitiesOfClass(LivingEntity.class, freezeBox, e -> e != this && e.isAlive());
                for (LivingEntity v : victims) {
                    v.setDeltaMovement(Vec3.ZERO);
                    v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, false));
                    v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 2, false, false));
                }
            }

            // Vết nứt không gian hạt ma thuật
            for (int i = 0; i < 6; i++) {
                double ang = this.random.nextDouble() * Math.PI * 2.0D;
                double dist = 3.0D + this.random.nextDouble() * 20.0D;
                double px = this.getX() + Math.cos(ang) * dist;
                double pz = this.getZ() + Math.sin(ang) * dist;
                sLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, px, this.getY() + 1.0D, pz, 4, 0.2D, 0.5D, 0.2D, 0.05D);
                sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, px, this.getY() + 1.0D, pz, 2, 0.2D, 0.5D, 0.2D, 0.02D);
            }

            // Kết thúc nứt vỡ không gian: Phá hủy block ngẫu nhiên và bộc phát chấn động âm thanh
            if (activeSkillTicks == 1) {
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 0.9F);
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 1.2F);
                sLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.2D, this.getZ(), 3, 0.5D, 0.5D, 0.5D, 0);

                // Phá hủy một số khối block quanh tâm vụ nổ không gian
                BlockPos centerBp = this.blockPosition();
                for (int bx = -4; bx <= 4; bx++) {
                    for (int by = -2; by <= 3; by++) {
                        for (int bz = -4; bz <= 4; bz++) {
                            if (bx * bx + by * by + bz * bz <= 16 && this.random.nextFloat() <= 0.65F) {
                                BlockPos bp = centerBp.offset(bx, by, bz);
                                BlockState st = sLevel.getBlockState(bp);
                                if (!st.isAir() && st.getDestroySpeed(sLevel, bp) >= 0.0F) {
                                    sLevel.destroyBlock(bp, false);
                                }
                            }
                        }
                    }
                }

                // Sát thương bạo liệt
                AABB blastBox = this.getBoundingBox().inflate(18.0D);
                List<LivingEntity> blastVictims = sLevel.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e != this && e.isAlive());
                for (LivingEntity v : blastVictims) {
                    v.hurt(sLevel.damageSources().mobAttack(this), 65.0F);
                    v.setDeltaMovement(new Vec3(0, 1.2D, 0));
                }
                this.setCastingState(0);
            }
        } else if (activeSkillId == 2) {
            // --- KỸ NĂNG 2: PHI ĐAO (SPATIAL HEAT BLADES) ---
            if (activeSkillTicks % 3 == 0 && this.getTarget() != null) {
                LivingEntity target = this.getTarget();
                Vec3 start = this.getEyePosition();
                Vec3 dir = target.getEyePosition().subtract(start).normalize()
                        .add((this.random.nextDouble() - 0.5D) * 0.25D, (this.random.nextDouble() - 0.5D) * 0.2D, (this.random.nextDouble() - 0.5D) * 0.25D).normalize();

                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 1.8F);

                // Bay xuyên và cắt block trên đường đi
                for (double d = 1.0D; d < 28.0D; d += 1.2D) {
                    Vec3 pt = start.add(dir.scale(d));
                    sLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    sLevel.sendParticles(ParticleTypes.FLAME, pt.x, pt.y, pt.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);

                    BlockPos bp = BlockPos.containing(pt);
                    BlockState st = sLevel.getBlockState(bp);
                    if (!st.isAir() && st.getDestroySpeed(sLevel, bp) >= 0.0F && this.random.nextFloat() <= 0.3F) {
                        sLevel.destroyBlock(bp, false);
                    }

                    // Va chạm kẻ thù
                    AABB box = new AABB(pt.x - 1.2D, pt.y - 1.2D, pt.z - 1.2D, pt.x + 1.2D, pt.y + 1.2D, pt.z + 1.2D);
                    List<LivingEntity> hit = sLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive());
                    for (LivingEntity victim : hit) {
                        applyDamageToTarget(victim, 28.0F);
                        victim.setRemainingFireTicks(80);
                    }
                }
            }

            if (activeSkillTicks == 1) {
                this.setCastingState(0);
            }
        } else if (activeSkillId == 3) {
            // --- KỸ NĂNG 3: GIA TỐC CHƯỚC NHIỆT LONG (CARDINAL ACCELERATION) ---
            if (activeSkillTicks > 25) {
                // === GIAI ĐOẠN 1: TỤ LỰC 7 GIÂY (140 TICKS) ===
                this.getNavigation().stop();
                this.setDeltaMovement(0, 0.02D, 0); // Lơ lửng ổn định

                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }

                int chargeTicksRemaining = activeSkillTicks - 25; // 140 -> 1

                // Hạt ma tố hỏa diễm xoáy cuộn hút vào tâm Velgrynd
                double chargeProgress = 1.0D - (chargeTicksRemaining / 140.0D); // 0.0 -> 1.0
                double radius = Math.max(0.6D, 3.8D * (1.0D - chargeProgress * 0.7D));
                for (int i = 0; i < 4; i++) {
                    double angle = (this.tickCount * 18.0D + i * 90.0D) * Math.PI / 180.0D;
                    double px = this.getX() + Math.cos(angle) * radius;
                    double pz = this.getZ() + Math.sin(angle) * radius;
                    double py = this.getY() + 0.4D + (this.random.nextDouble() * 2.0D);

                    sLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, (this.getX() - px) * 0.15D, 0.05D, (this.getZ() - pz) * 0.15D, 0.02D);
                    sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, (this.getX() - px) * 0.12D, 0.04D, (this.getZ() - pz) * 0.12D, 0.02D);
                    sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 1, 0, 0.02D, 0, 0.01D);
                }

                // Dưới chân bốc lửa và khói ma pháp
                if (this.tickCount % 4 == 0) {
                    sLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.2D, this.getZ(), 3, 0.5D, 0.2D, 0.5D, 0.05D);
                    sLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.2D, this.getZ(), 2, 0.3D, 0.2D, 0.3D, 0.02D);
                }

                // Đếm ngược mỗi giây (20 ticks) và phát thông báo
                if (chargeTicksRemaining % 20 == 0) {
                    int secondsLeft = chargeTicksRemaining / 20;

                    float pitch = 0.5F + (7 - secondsLeft) * 0.15F;
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE, 2.5F, pitch);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.8F, 0.8F + (7 - secondsLeft) * 0.1F);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.9F);

                    for (ServerPlayer p : sLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
                        p.displayClientMessage(
                                Component.literal("§c§l[GIA TỐC CHƯỚC NHIỆT LONG] §6Tụ lực: §e" + secondsLeft + "s §c[CỰC ĐỘ NGUY HIỂM]! Mau chóng né tránh!"),
                                true
                        );
                    }

                    if (secondsLeft == 3) {
                        this.broadcastDialogue("Sức nóng vượt qua giới hạn vật chất! Không một ai có thể trốn thoát!");
                    } else if (secondsLeft == 1) {
                        this.broadcastDialogue("Gia Tốc Tuyệt Đối... Tận diệt!");
                    }
                }

                // Khi hết 7s tụ lực -> Chuyển sang giai đoạn lao Mach 5
                if (activeSkillTicks == 26) {
                    this.setCastingState(3); // Model chuyển sang Mach 5 lao thẳng
                    LivingEntity t = this.getTarget();
                    if (t != null && t.isAlive()) {
                        this.accelDirection = t.position().add(0, t.getBbHeight() * 0.5D, 0).subtract(this.position().add(0, 1.0D, 0)).normalize();
                    } else {
                        this.accelDirection = this.getLookAngle();
                    }

                    com.minhphuc.weapons.content.tensura.LuciferReplicationAbility.recordSkillObserved(sLevel, this.position(), "CARDINAL_ACCEL", "Gia Tốc Chước Nhiệt Long (Velgrynd)");

                    this.broadcastDialogue("Thưởng thức cơn thịnh nộ tuyệt đối của Long Chủng đi — Gia Tốc Chước Nhiệt Long!");

                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 0.9F);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 4.0F, 1.2F);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.8F);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 1.4F);

                    for (ServerPlayer p : sLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
                        p.displayClientMessage(
                                Component.literal("§4§l[CẢNH BÁO TỐI CAO] §c§lGIA TỐC CHƯỚC NHIỆT LONG ĐÃ KHAI HỎA!"),
                                false
                        );
                    }
                }
            } else {
                // === GIAI ĐOẠN 2: LAO SIÊU THANH (25 TICKS) ===
                if (accelDirection != null) {
                    this.setDeltaMovement(accelDirection.scale(2.2D));
                    this.hurtMarked = true;

                    // Tạo hư ảnh rồng lửa
                    Vec3 curPos = this.position();
                    sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, curPos.x, curPos.y + 1.0D, curPos.z, 12, 1.2D, 1.0D, 1.2D, 0.05D);
                    sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, curPos.x, curPos.y + 1.0D, curPos.z, 15, 1.0D, 1.0D, 1.0D, 0.08D);
                    sLevel.sendParticles(ParticleTypes.LAVA, curPos.x, curPos.y + 0.5D, curPos.z, 8, 0.8D, 0.8D, 0.8D, 0.05D);
                    sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, curPos.x, curPos.y + 0.8D, curPos.z, 1, 0, 0, 0, 0);

                    // Phá hủy hoàn toàn Đa Trùng Kết Giới trên đường lao
                    com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.shatterBarrierNear(sLevel, curPos, 5.0D, "Gia Tốc Chước Nhiệt Long của Velgrynd");

                    // Đào hầm phá hủy block bán kính 2.5 blocks trên đường lao
                    BlockPos centerBp = BlockPos.containing(curPos);
                    int r = 2;
                    for (int bx = -r; bx <= r; bx++) {
                        for (int by = -1; by <= 3; by++) {
                            for (int bz = -r; bz <= r; bz++) {
                                if (bx * bx + bz * bz <= r * r + 1) {
                                    BlockPos bp = centerBp.offset(bx, by, bz);
                                    BlockState st = sLevel.getBlockState(bp);
                                    if (!st.isAir() && st.getDestroySpeed(sLevel, bp) >= 0.0F) {
                                        sLevel.destroyBlock(bp, false);
                                        // Tạo lửa trên mặt đất
                                        if (sLevel.getBlockState(bp.below()).isSolid() && this.random.nextFloat() <= 0.25F) {
                                            sLevel.setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Va chạm thực thể trên đường lao
                    AABB hitBox = this.getBoundingBox().inflate(3.0D);
                    List<LivingEntity> enemies = sLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e.isAlive());
                    for (LivingEntity v : enemies) {
                        applyDamageToTarget(v, 100.0F);
                        v.setDeltaMovement(accelDirection.scale(3.0D).add(0, 1.0D, 0));
                        v.setRemainingFireTicks(160);
                        if (v instanceof ServerPlayer sp) {
                            sp.connection.send(new ClientboundSetEntityMotionPacket(v));
                        }
                    }
                }

                if (activeSkillTicks == 1) {
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 5.0F, 0.8F);
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 1.2F);
                    sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 4, 1.0D, 1.0D, 1.0D, 0.05D);
                    this.setCastingState(0);
                    this.accelDirection = null;
                }
            }
        } else if (activeSkillId == 5) {
            // --- KỸ NĂNG 5: MƯA THIÊN HỎA (METEOR STORM RAIN) ---
            // Báo hiệu vòng tròn đỏ dưới mặt đất trong 30 ticks (1.5s)
            if (activeSkillTicks > 1 && !meteorTargetPositions.isEmpty()) {
                DustParticleOptions RED_TELEGRAPH = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 1.6F);
                for (Vec3 pos : meteorTargetPositions) {
                    for (int i = 0; i < 8; i++) {
                        double ang = (i * 45.0D) * Math.PI / 180.0D;
                        double px = pos.x + Math.cos(ang) * 3.5D;
                        double pz = pos.z + Math.sin(ang) * 3.5D;
                        sLevel.sendParticles(RED_TELEGRAPH, px, pos.y + 0.15D, pz, 1, 0, 0.02D, 0, 0);
                    }
                }
            }

            if (activeSkillTicks == 1) {
                // Khai hỏa thiên thạch rơi xuống các vị trí
                for (Vec3 pos : meteorTargetPositions) {
                    sLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.5F, 0.8F);
                    sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.0D, pos.z, 2, 0.5D, 0.5D, 0.5D, 0);
                    sLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5D, pos.z, 20, 1.5D, 0.5D, 1.5D, 0.1D);

                    AABB blastBox = new AABB(pos.x - 3.5D, pos.y - 1.0D, pos.z - 3.5D,
                            pos.x + 3.5D, pos.y + 3.0D, pos.z + 3.5D);
                    List<LivingEntity> list = sLevel.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e != this && e.isAlive());
                    for (LivingEntity v : list) {
                        applyDamageToTarget(v, 45.0F);
                        v.setRemainingFireTicks(120);
                    }
                }
                this.setCastingState(0);
                this.meteorTargetPositions.clear();
            }
        }
    }

    public void startSkillTimeCollapse() {
        this.activeSkillId = 1;
        this.activeSkillTicks = 50;
        this.timeCollapseCooldown = 280;
        this.setCastingState(1);
    }

    public void startSkillHeatBlades() {
        this.activeSkillId = 2;
        this.activeSkillTicks = 36;
        this.heatBladeCooldown = 90;
        this.setCastingState(2);
        this.broadcastDialogue("Cắt xé thành trăm mảnh đi — Chước Liệt Tiệt Đoán!");
    }

    public void startSkillMeteorRain(LivingEntity target) {
        if (target == null) return;
        this.activeSkillId = 5;
        this.activeSkillTicks = 30; // 1.5s telegraph
        this.meteorRainCooldown = isEnraged() ? 180 : 260;
        this.setCastingState(1);

        this.broadcastDialogue("Thiên Hỏa Lạc Lôi... Bầu trời này sẽ là mồ chôn của các ngươi!");
        if (this.level() instanceof ServerLevel sLevel) {
            sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.5F, 0.8F);
        }

        // Chọn 5 vị trí: tâm mục tiêu và 4 góc xung quanh
        this.meteorTargetPositions.clear();
        Vec3 tPos = target.position();
        this.meteorTargetPositions.add(tPos);
        this.meteorTargetPositions.add(tPos.add(-5.0D, 0, -5.0D));
        this.meteorTargetPositions.add(tPos.add(5.0D, 0, -5.0D));
        this.meteorTargetPositions.add(tPos.add(-5.0D, 0, 5.0D));
        this.meteorTargetPositions.add(tPos.add(5.0D, 0, 5.0D));
    }

    public void startSkillCardinalAcceleration(LivingEntity target) {
        this.activeSkillId = 3;
        this.activeSkillTicks = 165; // 140 ticks (7s tụ lực) + 25 ticks (lao siêu thanh)
        this.cardinalAccelCooldown = 400; // 20s cooldown
        this.setCastingState(4); // 4 = Charging Cardinal Acceleration
        this.accelDirection = null;
        this.getNavigation().stop();

        this.broadcastDialogue("Cả bầu trời này sắp bị thiêu rụi... Hãy cảm nhận sức nóng tột cùng của Gia Tốc Chước Nhiệt Long!");

        if (this.level() instanceof ServerLevel sLevel) {
            sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 0.7F);
            sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.5F, 1.3F);
            sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 1.1F);

            for (ServerPlayer p : sLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
                p.displayClientMessage(
                        Component.literal("§c§l[CHƯỚC NHIỆT LONG VELGRYND] §6§lĐang ngưng tụ Gia Tốc Chước Nhiệt Long (Cardinal Acceleration)! Đếm ngược 7 giây!"),
                        false
                );
            }
        }
    }

    /**
     * Kỹ Năng Người Chơi 1: Long Tinh Bộc Viêm Bá (Dragon Nova)
     */
    public void castAlliedDragonNova(LivingEntity target) {
        if (target == null || !target.isAlive() || !(this.level() instanceof ServerLevel sLevel)) return;
        this.broadcastDialogue("Hỡi Tinh Tú Bộc Phá — Long Tinh Bộc Viêm Bá (Dragon Nova)!");
        this.setCastingState(1);

        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);

        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 0.9F);
        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 1.2F);

        for (double d = 0.5D; d <= dist + 4.0D; d += 0.8D) {
            Vec3 pt = start.add(dir.scale(d));
            sLevel.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 2, 0.1D, 0.1D, 0.1D, 0.05D);
            sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, pt.x, pt.y, pt.z, 2, 0.2D, 0.2D, 0.2D, 0.02D);
            sLevel.sendParticles(ParticleTypes.FLASH, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }

        AABB damageArea = new AABB(end.x - 3.5D, end.y - 2.5D, end.z - 3.5D, end.x + 3.5D, end.y + 2.5D, end.z + 3.5D);
        List<LivingEntity> victims = sLevel.getEntitiesOfClass(LivingEntity.class, damageArea, e -> e != this && e.isAlive() && !(e instanceof Player));
        for (LivingEntity v : victims) {
            if (v instanceof PrimordialDemonEntity demon && demon.isTame()) continue;
            v.hurt(sLevel.damageSources().mobAttack(this), 220.0F);
            v.setRemainingFireTicks(120);
        }
    }

    /**
     * Kỹ Năng Người Chơi 2: Bạo Thực Vương Beelzebuth (Predator Vortex)
     */
    public void castAlliedBeelzebuth(LivingEntity target) {
        if (target == null || !target.isAlive() || !(this.level() instanceof ServerLevel sLevel)) return;
        this.broadcastDialogue("Hư Không Thôn Phệ — Bạo Thực Vương Beelzebuth!");
        this.setCastingState(2);

        Vec3 center = target.position();
        sLevel.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 1.4F);
        sLevel.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.5F, 1.2F);

        AABB pullBox = new AABB(center.x - 16.0D, center.y - 6.0D, center.z - 16.0D,
                center.x + 16.0D, center.y + 8.0D, center.z + 16.0D);
        List<LivingEntity> pulled = sLevel.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != this && e.isAlive() && !(e instanceof Player));

        for (LivingEntity v : pulled) {
            if (v instanceof PrimordialDemonEntity demon && demon.isTame()) continue;
            Vec3 pull = center.subtract(v.position()).normalize().scale(1.2D);
            v.setDeltaMovement(pull.add(0, 0.3D, 0));
            v.hurt(sLevel.damageSources().mobAttack(this), 150.0F);
            sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, v.getX(), v.getY() + 1.0D, v.getZ(), 8, 0.3D, 0.5D, 0.3D, 0.05D);
            sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, v.getX(), v.getY() + 1.0D, v.getZ(), 8, 0.3D, 0.5D, 0.3D, 0.05D);
        }
    }

    /**
     * Kỹ Năng Người Chơi 3: Tuyệt Diệt Tinh Tú (Extinction Stars)
     */
    public void castAlliedExtinctionStars(LivingEntity target) {
        if (target == null || !target.isAlive() || !(this.level() instanceof ServerLevel sLevel)) return;
        this.broadcastDialogue("Vạn Trượng Tinh Hỏa — Tuyệt Diệt Tinh Tú!");
        this.setCastingState(1);

        Vec3 center = target.position();
        sLevel.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 5.0F, 1.1F);

        for (int i = 0; i < 4; i++) {
            double ang = i * (Math.PI * 2.0D / 4.0D);
            double dist = 2.0D + this.random.nextDouble() * 4.0D;
            double sx = center.x + Math.cos(ang) * dist;
            double sz = center.z + Math.sin(ang) * dist;

            sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sx, center.y + 0.5D, sz, 2, 0.5D, 0.5D, 0.5D, 0);
            sLevel.sendParticles(ParticleTypes.LAVA, sx, center.y + 0.5D, sz, 12, 0.5D, 0.8D, 0.5D, 0.1D);
        }

        AABB blastArea = new AABB(center.x - 8.0D, center.y - 3.0D, center.z - 8.0D,
                center.x + 8.0D, center.y + 5.0D, center.z + 8.0D);
        List<LivingEntity> hit = sLevel.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != this && e.isAlive() && !(e instanceof Player));
        for (LivingEntity v : hit) {
            if (v instanceof PrimordialDemonEntity demon && demon.isTame()) continue;
            v.hurt(sLevel.damageSources().mobAttack(this), 180.0F);
            v.setDeltaMovement(new Vec3(0, 1.1D, 0));
        }
    }

    /**
     * Xử lý sát thương Velgrynd gây ra cho mục tiêu:
     * - Nếu là người chơi mặc Giáp Thần: Đòn 1 & 2 bị chặn, đòn 3 gây 30% Máu Tối Đa.
     * - Nếu là PrimordialDemonEntity: Quét sạch ác ma thường, nhưng cân bằng 50/50 với Guy Crimson Thể Xác & Tên!
     */
    public void applyDamageToTarget(LivingEntity victim, float baseDamage) {
        if (victim == null || !victim.isAlive() || this.level().isClientSide()) return;

        if (victim instanceof ServerPlayer player) {
            if (DivineArmorItem.isWearingFullSet(player)) {
                UUID pid = player.getUUID();
                int currentHits = playerArmorHitCounters.getOrDefault(pid, 0) + 1;

                if (currentHits < 3) {
                    playerArmorHitCounters.put(pid, currentHits);
                    // Đòn 1 & 2: Giáp thần phản chấn lại
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5F, 1.8F);
                    ((ServerLevel) this.level()).sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1.0D, player.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.05D);
                    player.displayClientMessage(
                            Component.literal("§6§l[THẦN LINH THÁNH GIÁP] §aĐã cản phá đòn đánh của Long Chủng! §e(" + currentHits + "/3)"),
                            true
                    );
                    return;
                } else {
                    // Đòn thứ 3: Xuyên phá giáp thần, gây 30% HP tối đa!
                    playerArmorHitCounters.put(pid, 0);
                    float damage30Percent = player.getMaxHealth() * 0.30F;

                    // Gây sát thương true damage (bỏ qua giáp)
                    player.addTag("VelgryndPenetrationDamage");
                    player.hurt(this.level().damageSources().magic(), damage30Percent);
                    player.removeTag("VelgryndPenetrationDamage");
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0F, 0.8F);
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.5F, 1.0F);

                    player.displayClientMessage(
                            Component.literal("§c§l[CẢNH BÁO] Hỏa diễm của Velgrynd đã xuyên phá Thánh Giáp! (-30% Máu)"),
                            false
                    );
                    return;
                }
            }
        }

        // Tương tác với Ác Ma Thủy Tổ
        if (victim instanceof PrimordialDemonEntity demon) {
            boolean isGuyCrimsonAwakened = (demon.getDemonType() == DemonType.ROUGE && demon.hasPhysicalBody() && demon.isNamed());
            if (isGuyCrimsonAwakened) {
                // Guy Crimson thức tỉnh: Tỉ lệ 50/50, đánh nhau cân tài cân sức!
                float balancedDamage = 45.0F + this.random.nextFloat() * 15.0F;
                demon.hurt(this.level().damageSources().mobAttack(this), balancedDamage);
                this.broadcastDialogue("Guy! Lâu lắm rồi mới có kẻ khiến ta hưng phấn như thế này!");
                return;
            } else {
                // Toàn bộ các ác ma khác: Bị Velgrynd nghiền nát lập tức!
                demon.hurt(this.level().damageSources().mobAttack(this), 3000.0F);
                this.broadcastDialogue("Thứ ác ma hạ đẳng chưa đủ tư cách đứng trước mặt ta!");
                return;
            }
        }

        victim.hurt(this.level().damageSources().mobAttack(this), baseDamage);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level().isClientSide()) return false;
        ServerLevel sl = (ServerLevel) this.level();

        // 50% xác suất đổi vũ khí: Quạt Lông Vũ hoặc Vuốt Rồng / Đòn Đánh Tay Không
        this.setHoldingFan(this.random.nextBoolean());

        if (this.isHoldingFan()) {
            this.broadcastDialogue("Biết thân biết phận một chút đi! Một cái phẩy quạt của ta cũng đủ biến ngươi thành tro bụi!");
        } else {
            this.broadcastDialogue("Vuốt rồng của Chước Nhiệt Long đủ sức xé toạc mọi ranh giới!");
        }

        // 1. Phá giáp chắn khiên nếu mục tiêu đang đỡ đòn (tương tự Milim)
        if (target instanceof Player player && player.isBlocking()) {
            player.disableShield();
        }

        // 2. Hướng lực đánh (Direction vector)
        Vec3 strikeOrigin = this.position().add(0, this.getEyeHeight() * 0.8D, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        Vec3 dir = targetCenter.subtract(strikeOrigin).normalize();
        if (dir.lengthSqr() < 1.0E-4) {
            dir = this.getLookAngle();
        }

        // 3. Gây sát thương theo cơ chế Long Chủng (Giáp thần xuyên đòn 3, Guy Crimson cân bằng, quái thường)
        if (target instanceof LivingEntity livingTarget) {
            applyDamageToTarget(livingTarget, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            livingTarget.setRemainingFireTicks(100);
        }

        // 4. Đánh mục tiêu bay cực xa (Knockback siêu mạnh tương tự Milim)
        Vec3 knockback = new Vec3(dir.x * 4.2D, 1.35D, dir.z * 4.2D);
        target.setDeltaMovement(knockback);
        target.hurtMarked = true;
        target.hasImpulse = true;
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(target));
        }

        // 5. Hiệu ứng bộc phá nổ lớn & khói mù mịt dày đặc
        double hitX = target.getX();
        double hitY = targetCenter.y;
        double hitZ = target.getZ();

        // Nổ lớn và sóng xung kích âm thanh
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitX, hitY, hitZ, 3, 0.3D, 0.3D, 0.3D, 0.0D);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, hitX, hitY, hitZ, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        sl.sendParticles(ParticleTypes.FLASH, hitX, hitY, hitZ, 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.FLAME, hitX, hitY, hitZ, 100, 2.0D, 1.5D, 2.0D, 0.35D);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hitX, hitY, hitZ, 80, 1.8D, 1.2D, 1.8D, 0.25D);
        sl.sendParticles(ParticleTypes.DRAGON_BREATH, hitX, hitY, hitZ, 60, 1.5D, 1.2D, 1.5D, 0.1D);

        // Khói mù mịt dày đặc lan tỏa rộng
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hitX, hitY, hitZ, 80, 2.0D, 1.5D, 2.0D, 0.08D);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, hitX, hitY, hitZ, 60, 1.8D, 1.2D, 1.8D, 0.12D);

        // Âm thanh bộc phá vang dội
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.8F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.2F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.5F, 0.6F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 1.4F);

        // 6. Xung lực chấn động thổi bay các thực thể xung quanh (Shockwave 7 blocks)
        AABB shockBox = target.getBoundingBox().inflate(7.0D);
        UUID ownerId = this.isAllied() ? this.getParsedOwnerUUID() : null;
        for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, shockBox)) {
            if (nearby != this && nearby != target && nearby.isAlive()) {
                if (this.isAllied()) {
                    if (ownerId != null && nearby.getUUID().equals(ownerId)) continue;
                    if (nearby instanceof PrimordialDemonEntity demon && demon.isTame() && ownerId != null && demon.getOwnerUUID() != null && demon.getOwnerUUID().equals(ownerId)) continue;
                }
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
                nearby.setRemainingFireTicks(60);
            }
        }

        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity le) {
            recentAttackers.put(le.getUUID(), this.level().getGameTime());

            // Tương tác khi đụng độ Guy Crimson thức tỉnh: Guy đánh Velgrynd cũng gây sát thương cân bằng 50/50
            if (attacker instanceof PrimordialDemonEntity pde && pde.getDemonType() == DemonType.ROUGE && pde.hasPhysicalBody() && pde.isNamed()) {
                if (this.random.nextFloat() <= 0.25F) {
                    this.broadcastDialogue("Đòn đánh tốt đấy, Guy! Nhưng đừng tưởng thế là đủ để hạ gục Long Chủng!");
                }
                // Giảm bớt 1 tầng bảo hộ nếu Guy đánh trúng đòn mạnh
                if (amount >= 30.0F && this.getDragonLayers() > 1) {
                    this.setDragonLayers(this.getDragonLayers() - 1);
                }
            }

            // Tương tác khi đụng độ Bạch Băng Long Velzard
            if (attacker instanceof VelzardEntity) {
                if (this.random.nextFloat() <= 0.25F) {
                    this.broadcastDialogue("Chị hai Velzard... Rốt cuộc chị cũng ra tay với em sao?!");
                }
                if (amount >= 30.0F && this.getDragonLayers() > 1) {
                    this.setDragonLayers(this.getDragonLayers() - 1);
                }
            }
        }

        // Tăng 25% sát thương nhận vào khi ở Phase 2 (Nhiệt Huyết Bạo Tẩu - Glass Cannon)
        float finalHurtAmount = isEnraged() ? amount * 1.25F : amount;

        // 1. KIỂM TRA ĐẶC BIỆT: LONG TINH BỘC VIÊM BÁ (DRAGON NOVA)
        // Năng lượng Tinh Tố bộc phá xuyên thủng vảy rồng và kết liễu ngay lập tức!
        boolean isDragonNova = amount >= 400.0F || source.getMsgId().contains("dragon_nova") || (attacker instanceof Player && amount >= 250.0F && this.level().getGameTime() % 2 == 0 && amount == 500.0F);
        if (isDragonNova) {
            this.broadcastDialogue("Không thể nào... Năng lượng Tinh Tố này... Là Milim sao...?! Rudra...!");
            this.setDragonLayers(0);
            this.bossEvent.removeAllPlayers();
            return super.hurt(source, 10000.0F);
        }

        // 2. CƠ CHẾ 4 LẦN ĐÁNH CỦA NGƯỜI CHƠI MỚI GIẾT ĐƯỢC
        if (attacker instanceof Player) {
            int currentLayers = this.getDragonLayers();
            if (currentLayers > 1) {
                this.setDragonLayers(currentLayers - 1);

                if (currentLayers == 4) {
                    this.broadcastDialogue("Hừ! Ngươi tưởng chạm vào được ta là thắng sao? Nực cười!");
                } else if (currentLayers == 3) {
                    this.broadcastDialogue("Đừng tưởng bở! Lớp vảy của Long Chủng không mỏng manh như ngươi nghĩ!");
                } else if (currentLayers == 2) {
                    this.broadcastDialogue("Ngươi... dám làm tổn thương chân thân của ta?! Ta sẽ không nương tay nữa!");
                }

                // Hiệu ứng tia lửa rồng chặn đòn
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0, 0, 0, 0);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 1.2D, this.getZ(), 10, 0.5D, 0.5D, 0.5D, 0.1D);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 1.6F);

                // Giữ lượng máu tương ứng với số tầng
                this.setHealth(this.getMaxHealth() * (this.getDragonLayers() / 4.0F));
                return false; // Chặn chết ở 3 đòn đầu
            } else if (currentLayers == 1) {
                // Đòn thứ 4: Hạ gục Velgrynd!
                this.setDragonLayers(0);
                this.broadcastDialogue("Không thể nào... Ta là Chước Nhiệt Long Velgrynd... Rudra...!");
                this.bossEvent.removeAllPlayers();
                return super.hurt(source, 10000.0F);
            }
        }

        return super.hurt(source, finalHurtAmount);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.bossEvent.removeAllPlayers();
        if (!this.level().isClientSide()) {
            ServerLevel sl = (ServerLevel) this.level();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 4.0F, 0.85F);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5D, this.getZ(), 5, 1.0D, 1.0D, 1.0D, 0.1D);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.5D, this.getZ(), 120, 1.5D, 1.5D, 1.5D, 0.1D);

            // Khi Boss hoang dã bị đánh bại: Rơi ra Nghịch Lân Chước Nhiệt: Lời Thề Long Chủng
            if (!this.isClone() && !this.isAllied()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        sl, this.getX(), this.getY() + 0.5D, this.getZ(),
                        new ItemStack(com.minhphuc.weapons.init.ModItems.VELGRYND_REVERSE_SCALE.get())
                );
                drop.setGlowingTag(true);
                sl.addFreshEntity(drop);

                for (ServerPlayer sp : sl.players()) {
                    if (sp.distanceToSqr(this) <= 50.0D * 50.0D) {
                        sp.displayClientMessage(
                                Component.literal("§6§l✦ LONG CHỦNG CÔNG NHẬN ✦ §eChước Nhiệt Long Velgrynd đã công nhận sức mạnh của ngươi! Rơi ra §c§l[Nghịch Lân Chước Nhiệt: Lời Thề Long Chủng]§e!"),
                                false
                        );
                    }
                }
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DragonLayers")) {
            this.setDragonLayers(tag.getInt("DragonLayers"));
        }
        if (tag.contains("IsClone")) {
            this.setClone(tag.getBoolean("IsClone"));
        }
        if (tag.contains("IsAllied")) {
            this.setAllied(tag.getBoolean("IsAllied"));
        }
        if (tag.contains("OwnerUUID")) {
            this.setOwnerUUID(tag.getString("OwnerUUID"));
        }
        if (tag.contains("HoldingFan")) {
            this.setHoldingFan(tag.getBoolean("HoldingFan"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DragonLayers", this.getDragonLayers());
        tag.putBoolean("IsClone", this.isClone());
        tag.putBoolean("IsAllied", this.isAllied());
        tag.putString("OwnerUUID", this.getOwnerUUID());
        tag.putBoolean("HoldingFan", this.isHoldingFan());
    }

    public int getTimeCollapseCooldown() { return timeCollapseCooldown; }
    public int getHeatBladeCooldown() { return heatBladeCooldown; }
    public int getCardinalAccelCooldown() { return cardinalAccelCooldown; }
    public int getMeteorRainCooldown() { return meteorRainCooldown; }
    public int getActiveSkillTicks() { return activeSkillTicks; }

    /**
     * AI Goal chuyên dụng điều phối chuỗi combo kỹ năng hủy diệt diện rộng tốc độ cao
     */
    static class VelgryndCombatGoal extends Goal {
        private final VelgryndEntity velgrynd;

        public VelgryndCombatGoal(VelgryndEntity velgrynd) {
            this.velgrynd = velgrynd;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return velgrynd.getTarget() != null && velgrynd.getTarget().isAlive() && velgrynd.getActiveSkillTicks() <= 0;
        }

        @Override
        public void tick() {
            LivingEntity target = velgrynd.getTarget();
            if (target == null) return;

            velgrynd.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = velgrynd.distanceToSqr(target);

            // 1. Thao túng thời không (Ưu tiên thi triển khi có sẵn)
            if (velgrynd.getTimeCollapseCooldown() <= 0 && distSq <= 24.0D * 24.0D) {
                velgrynd.startSkillTimeCollapse();
                return;
            }

            // 2. Gia tốc chước nhiệt long (Khoảng cách xa 10m - 32m)
            if (velgrynd.getCardinalAccelCooldown() <= 0 && distSq >= 10.0D * 10.0D && distSq <= 32.0D * 32.0D) {
                velgrynd.startSkillCardinalAcceleration(target);
                return;
            }

            // 3. Mưa Thiên Hỏa (Thiên thạch rơi - Tầm trung 10m - 35m)
            if (velgrynd.getMeteorRainCooldown() <= 0 && distSq <= 35.0D * 35.0D) {
                velgrynd.startSkillMeteorRain(target);
                return;
            }

            // 4. Phi đao hỏa diễm (Tầm trung 6m - 25m)
            if (velgrynd.getHeatBladeCooldown() <= 0 && distSq >= 6.0D * 6.0D) {
                velgrynd.startSkillHeatBlades();
                return;
            }

            // Tiến lại gần mục tiêu
            velgrynd.getNavigation().moveTo(target, 1.45D);
        }
    }
}
