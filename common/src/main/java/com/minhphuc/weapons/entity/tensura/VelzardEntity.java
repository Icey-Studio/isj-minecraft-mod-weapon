package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import com.minhphuc.weapons.content.tensura.SkillNullificationAbility;
import com.minhphuc.weapons.entity.ModEntities;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

/**
 * Bạch Băng Long Velzard (White Ice Dragon Velzard / Velzado)
 * Long Chủng Tối Thượng Cổ Đại (Tensura LN/Anime) - Mạnh gấp 3 lần Chước Nhiệt Long Velgrynd:
 * - Sinh lực 15,000 HP (3x Velgrynd), Công kích 165 (3x Velgrynd), Hồi phục 150-300 HP/s, 12 Tầng Vảy Băng Long.
 * - Đánh xuyên Giáp Thần (Divine Armor) & Phòng Ngự Tuyệt Đối Uriel mạnh gấp 3 lần Velgrynd (mỗi đòn xuyên 30% HP tối đa, đòn thứ 3 xuyên 90% HP tối đa).
 * - Khi xuất hiện & chiến đấu tạo ra Bão Tuyết Cực Hàn dày đặc, đóng băng vạn vật thành Băng/Băng Nén/Băng Xanh, phủ ngập tuyết và dựng lên Nhà Tuyết (Igloo/Snow House), Tháp Băng & Khối Tuyết.
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
    public static final EntityDataAccessor<Integer> DATA_DRAGON_LAYERS =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_CLONE =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_DRAGON_FORM =
            SynchedEntityData.defineId(VelzardEntity.class, EntityDataSerializers.BOOLEAN);

    // Casting states
    public static final int CAST_NONE = 0;
    public static final int CAST_SNOW_CRYSTAL = 1;      // Băng Tinh Bất Hoại (Gabriel)
    public static final int CAST_KINETIC_CESSATION = 2; // Ngưng Trệ Động Năng (Cthulhu)
    public static final int CAST_ICICLE_GATLING = 3;    // Cụm Gai Băng Kim Cương
    public static final int CAST_DRAGON_BREATH = 4;     // Hơi Thở Bạch Băng Long
    public static final int CAST_GLACIAL_PRISON = 5;    // Lãnh Ngục Địa Tinh

    public static final int MAX_DRAGON_LAYERS = 12; // Mạnh gấp 3 lần 4 tầng của Velgrynd

    private final ServerBossEvent bossEvent;

    // Cooldowns
    private int snowCrystalCooldown = 260;
    private int kineticCessationCooldown = 180;
    private int icicleGatlingCooldown = 90;
    private int dragonBreathCooldown = 280;
    private int glacialPrisonCooldown = 150;
    private int frostBloomCooldown = 140;
    private int mirrorDecoyCooldown = 0;
    private int snowStructureCooldown = 80;
    private int iceCannonCooldown = 85;
    private int skillSealCooldown = 360;
    private int dragonFormCooldown = 260;

    // Active casting timers
    private int activeSkillTicks = 0;
    private int currentCastingType = CAST_NONE;
    private Vec3 castingTargetPos = null;

    // Blizzard on spawn timer (90s = 1800 ticks, và duy trì liên tục khi giao chiến)
    private int blizzardTicksRemaining = 1800;
    private boolean hasGeneratedInitialStructures = false;

    // Stagger / Kiệt sức mechanism (khi vỡ 3 ấn ký hoa tuyết)
    private float damageTakenForSigil = 0.0F;
    private int staggerTicks = 0;

    // Permafrost standing trackers (UUID -> standing ticks)
    private final Map<UUID, Integer> standingTickCounters = new HashMap<>();

    // Bộ đếm đòn đánh xuyên Giáp Thần (Mạnh gấp 3 lần Velgrynd: đòn 1-2 xuyên 30% HP, đòn 3 xuyên 90% HP)
    private final Map<UUID, Integer> playerArmorHitCounters = new HashMap<>();
    private final Map<UUID, Long> recentAttackers = new HashMap<>();
    private UUID boundTargetUuid = null;
    private VelzardEntity activeClone = null;

    // Particle mộng ảo sương tuyết
    private static final DustParticleOptions FROST_CYAN =
            new DustParticleOptions(new Vector3f(0.35F, 0.92F, 1.0F), 1.8F);
    private static final DustParticleOptions FROST_WHITE =
            new DustParticleOptions(new Vector3f(0.95F, 0.98F, 1.0F), 2.1F);

    public VelzardEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§b§l[BẠCH BĂNG LONG] §f§lVELZARD §7- §9[Nữ Hoàng Băng Giá Tối Thượng]"),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.NOTCHED_12
        );
        this.moveControl = new FlyingMoveControl(this, 18, true);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15000.0D)       // Gấp 3 lần Velgrynd (5,000 -> 15,000 HP)
                .add(Attributes.MOVEMENT_SPEED, 0.44D)
                .add(Attributes.FLYING_SPEED, 0.62D)
                .add(Attributes.ATTACK_DAMAGE, 165.0D)      // Gấp 3 lần Velgrynd (55 -> 165 ATK)
                .add(Attributes.ARMOR, 45.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 35.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 72.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING_STATE, CAST_NONE);
        builder.define(DATA_SIGIL_COUNT, 3);
        builder.define(DATA_IS_FLYING, false);
        builder.define(DATA_IS_ENRAGED, false);
        builder.define(DATA_DRAGON_LAYERS, MAX_DRAGON_LAYERS);
        builder.define(DATA_IS_CLONE, false);
        builder.define(DATA_DRAGON_FORM, false);
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new VelzardCombatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.55D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PrimordialDemonEntity.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        this.blizzardTicksRemaining = 1800;
        ServerLevel sl = level.getLevel();
        if (sl != null && !this.isClone()) {
            this.hasGeneratedInitialStructures = true;
            spawnGlacialArrivalWorldEffects(sl);
        }
        return spawnData;
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            player.displayClientMessage(Component.literal("§b§l╔════════════════════════════════════════════════════╗"), false);
            player.displayClientMessage(Component.literal("§b§l║      §f§l✦ BẠCH BĂNG LONG VELZARD (VELZADO) ✦      §b§l║"), false);
            player.displayClientMessage(Component.literal("§b§l╠════════════════════════════════════════════════════╣"), false);
            player.displayClientMessage(Component.literal("§e  Chủng Tộc: §fLong Chủng Tối Thượng (True Dragon - Đệ Nhị Long)"), false);
            player.displayClientMessage(Component.literal("§c  Sinh Lực (HP): §a15,000 / 15,000 HP §7(" + this.getDragonLayers() + "/" + MAX_DRAGON_LAYERS + " Tầng Vảy Băng Long)"), false);
            player.displayClientMessage(Component.literal("§d  Ma Tố Lượng (EP): §b223,050,000 EP §7(Mạnh gấp 3 lần Chước Nhiệt Long Velgrynd)"), false);
            player.displayClientMessage(Component.literal("§3  Quyền Năng: §bNgưng Trệ Động Năng (Cthulhu), Băng Tinh Vĩnh Cửu (Gabriel), Bão Tuyết Cực Hàn"), false);
            player.displayClientMessage(Component.literal("§4  Cơ Chế: §cĐánh xuyên Giáp Thần & Uriel (-30% HP mỗi đòn, đòn thứ 3 xuyên -90% Máu Tối Đa)!"), false);
            player.displayClientMessage(Component.literal("§b§l╚════════════════════════════════════════════════════╝"), false);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.8F, 1.3F);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
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

    public int getDragonLayers() {
        return this.entityData.get(DATA_DRAGON_LAYERS);
    }

    public void setDragonLayers(int layers) {
        this.entityData.set(DATA_DRAGON_LAYERS, Math.max(0, Math.min(MAX_DRAGON_LAYERS, layers)));
        float progress = Math.max(0.0F, Math.min(1.0F, this.entityData.get(DATA_DRAGON_LAYERS) / (float) MAX_DRAGON_LAYERS));
        this.bossEvent.setProgress(progress);
    }

    public boolean isClone() {
        return this.entityData.get(DATA_IS_CLONE);
    }

    public void setClone(boolean clone) {
        this.entityData.set(DATA_IS_CLONE, clone);
        if (clone) {
            this.bossEvent.setName(Component.literal("§b§l[TỒN TẠI SONG SONG] §f§lVELZARD §7- §9[Phân Thân Băng Long]"));
        }
    }

    public void setBoundTarget(LivingEntity target) {
        if (target != null) {
            this.boundTargetUuid = target.getUUID();
            this.setTarget(target);
        }
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

    public boolean isDragonForm() {
        return this.entityData.get(DATA_DRAGON_FORM);
    }

    public void setDragonForm(boolean dragon) {
        boolean changed = this.entityData.get(DATA_DRAGON_FORM) != dragon;
        this.entityData.set(DATA_DRAGON_FORM, dragon);
        this.refreshDimensions();
        if (changed && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.5D, this.getZ(), 60, 1.8, 1.8, 1.8, 0.12);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.8F, dragon ? 0.9F : 1.35F);
        }
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return isDragonForm() ? net.minecraft.world.entity.EntityDimensions.scalable(7.0F, 7.8F) : super.getDefaultDimensions(pose);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;

                var biome = sl.getBiome(this.blockPosition());
                if (!biome.value().coldEnoughToSnow(this.blockPosition())) {
                    return false;
                }

                AABB checkArea = new AABB(this.blockPosition()).inflate(64.0D);
                if (!sl.getEntitiesOfClass(VelzardEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }
        }
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && super.checkSpawnRules(level, spawnType);
    }

    public void broadcastDialogue(String text) {
        if (this.level() instanceof ServerLevel sl) {
            Component msg = Component.literal("§b§l[BẠCH BĂNG LONG VELZARD] §f\"" + text + "\"");
            AABB notifyBox = this.getBoundingBox().inflate(64.0D);
            for (ServerPlayer player : sl.getEntitiesOfClass(ServerPlayer.class, notifyBox)) {
                player.displayClientMessage(msg, false);
            }
        }
    }

    /**
     * Xử lý sát thương xuyên Giáp Thần & Phòng Ngự Tuyệt Đối Uriel của Velzard (Mạnh gấp 3 lần Velgrynd):
     * - Với người chơi mặc Giáp Thần / Uriel:
     *   + Đòn 1 & 2: Xuyên thấu gây ngay 30% Máu Tối Đa (thay vì bị chặn hoàn toàn như Velgrynd).
     *   + Đòn thứ 3: Bộc phát Hàn Khí Tử Vong xuyên phá gây 90% Máu Tối Đa (Gấp 3 lần mức 30% của Velgrynd)!
     * - Với Ác Ma Thủy Tổ:
     *   + Nghiền nát các Ác Ma thông thường với 9,000 sát thương (3x Velgrynd).
     *   + Đánh với Guy Crimson thức tỉnh (Rouge Thể Xác & Tên): Gây 150 sát thương (3x Velgrynd).
     */
    public void applyDamageToTarget(LivingEntity victim, float baseDamage) {
        if (victim == null || !victim.isAlive() || this.level().isClientSide()) return;

        if (victim instanceof ServerPlayer player) {
            boolean hasDivineOrUriel = DivineArmorItem.isWearingFullSet(player)
                    || com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.isUrielDefenseActive(player)
                    || com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.isDemonLord(player);

            if (hasDivineOrUriel) {
                UUID pid = player.getUUID();
                int currentHits = playerArmorHitCounters.getOrDefault(pid, 0) + 1;

                if (currentHits < 3) {
                    playerArmorHitCounters.put(pid, currentHits);
                    float pierce30Percent = Math.max(baseDamage * 0.5F, player.getMaxHealth() * 0.30F);

                    player.addTag("VelzardPenetrationDamage");
                    player.hurt(this.level().damageSources().magic(), pierce30Percent);
                    player.removeTag("VelzardPenetrationDamage");
                    player.setTicksFrozen(player.getTicksRequiredToFreeze() + 120);

                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.9F);
                    ((ServerLevel) this.level()).sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.08D);
                    player.displayClientMessage(
                            Component.literal("§b§l[HÀN KHÍ TUYỆT ĐỐI] §cĐòn đánh của Bạch Băng Long Velzard đã xuyên qua Thánh Giáp! (-30% Máu) §e[" + currentHits + "/3]"),
                            true
                    );
                    return;
                } else {
                    // Đòn thứ 3: Gấp 3 lần Chước Nhiệt Long -> Xuyên phá 90% Máu Tối Đa!
                    playerArmorHitCounters.put(pid, 0);
                    float damage90Percent = Math.max(baseDamage, player.getMaxHealth() * 0.90F);

                    player.addTag("VelzardPenetrationDamage");
                    player.hurt(this.level().damageSources().magic(), damage90Percent);
                    player.removeTag("VelzardPenetrationDamage");
                    player.setTicksFrozen(player.getTicksRequiredToFreeze() + 240);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5, false, false));

                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.5F, 0.6F);
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.2F);
                    ((ServerLevel) this.level()).sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(), 50, 0.6D, 0.8D, 0.6D, 0.15D);

                    player.displayClientMessage(
                            Component.literal("§4§l[CẢNH BÁO TỬ VONG] §b§lHàn Khí Bạch Băng Long Velzard đã đóng băng và xuyên thủng hoàn toàn Thánh Giáp & Uriel! (-90% Máu Tối Đa)"),
                            false
                    );
                    return;
                }
            }
        }

        // Tương tác với Ác Ma Thủy Tổ (Mạnh gấp 3 lần Velgrynd)
        if (victim instanceof PrimordialDemonEntity demon) {
            boolean isGuyCrimsonAwakened = (demon.getDemonType() == DemonType.ROUGE && demon.hasPhysicalBody() && demon.isNamed());
            if (isGuyCrimsonAwakened) {
                float guyDamage = 135.0F + this.random.nextFloat() * 45.0F; // 3x Velgrynd's 45-60
                demon.hurt(this.level().damageSources().mobAttack(this), guyDamage);
                demon.setTicksFrozen(demon.getTicksFrozen() + 120);
                this.broadcastDialogue("Guy... Ngay cả ngươi cũng phải dè chừng trước Băng Giá Vĩnh Cửu của ta đấy!");
                return;
            } else {
                // Toàn bộ các ác ma khác: Bị Velzard đóng băng và nghiền nát với 9,000 sát thương (3x Velgrynd)!
                demon.setTicksFrozen(demon.getTicksRequiredToFreeze() + 240);
                demon.hurt(this.level().damageSources().mobAttack(this), 9000.0F);
                this.broadcastDialogue("Ác ma nguyên thủy sao? Trước mặt Bạch Băng Long, tất cả chỉ là những bức tượng băng dễ vỡ!");
                return;
            }
        }

        if (victim instanceof Player p) {
            p.addTag("VelzardPenetrationDamage");
            victim.hurt(this.level().damageSources().mobAttack(this), baseDamage);
            p.removeTag("VelzardPenetrationDamage");
        } else {
            victim.hurt(this.level().damageSources().mobAttack(this), baseDamage);
        }
        victim.setTicksFrozen(victim.getTicksFrozen() + 120);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level().isClientSide()) return false;
        ServerLevel sl = (ServerLevel) this.level();

        if (target instanceof Player player && player.isBlocking()) {
            player.disableShield();
        }

        Vec3 strikeOrigin = this.position().add(0, this.getEyeHeight() * 0.8D, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        Vec3 dir = targetCenter.subtract(strikeOrigin).normalize();
        if (dir.lengthSqr() < 1.0E-4) {
            dir = this.getLookAngle();
        }

        if (target instanceof LivingEntity livingTarget) {
            applyDamageToTarget(livingTarget, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }

        // Hất văng mục tiêu kèm bão tuyết nổ tung
        Vec3 knockback = new Vec3(dir.x * 4.5D, 1.4D, dir.z * 4.5D);
        target.setDeltaMovement(knockback);
        target.hurtMarked = true;
        target.hasImpulse = true;
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(target));
        }

        double hitX = target.getX();
        double hitY = targetCenter.y;
        double hitZ = target.getZ();

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitX, hitY, hitZ, 3, 0.3D, 0.3D, 0.3D, 0.0D);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, hitX, hitY, hitZ, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        sl.sendParticles(ParticleTypes.FLASH, hitX, hitY, hitZ, 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, hitX, hitY, hitZ, 120, 2.2D, 1.6D, 2.2D, 0.30D);
        sl.sendParticles(FROST_CYAN, hitX, hitY, hitZ, 70, 1.8D, 1.2D, 1.8D, 0.20D);
        sl.sendParticles(FROST_WHITE, hitX, hitY, hitZ, 60, 1.8D, 1.2D, 1.8D, 0.15D);

        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.9F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.5F, 0.7F);
        sl.playSound(null, hitX, hitY, hitZ, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.3F);

        // Đóng băng mặt đất tại điểm va chạm và dựng cụm gai băng
        freezeGroundPatch(sl, BlockPos.containing(hitX, target.getY(), hitZ), 4);

        // Sóng xung kích hàn băng 8 blocks
        AABB shockBox = target.getBoundingBox().inflate(8.0D);
        for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, shockBox)) {
            if (nearby != this && nearby != target && nearby.isAlive()) {
                Vec3 away = nearby.position().subtract(target.position()).normalize();
                if (away.lengthSqr() < 1.0E-4) away = dir;
                nearby.setDeltaMovement(away.x * 2.6D, 0.95D, away.z * 2.6D);
                nearby.hurtMarked = true;
                nearby.hasImpulse = true;
                if (nearby instanceof ServerPlayer nsp) {
                    nsp.connection.send(new ClientboundSetEntityMotionPacket(nearby));
                }
                applyDamageToTarget(nearby, 75.0F);
            }
        }

        return true;
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

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity le) {
            recentAttackers.put(le.getUUID(), this.level().getGameTime());
        }

        // BĂNG TINH BẤT HOẠI (SNOW CRYSTAL - GABRIEL): Kháng 100% khi đang bật khiên
        if (getCastingState() == CAST_SNOW_CRYSTAL) {
            boolean isDisintegration = source.is(DamageTypes.MAGIC) && amount >= 500.0F;
            if (!isDisintegration) {
                if (source.getEntity() instanceof LivingEntity livAttacker && this.distanceToSqr(livAttacker) <= 25.0D) {
                    applyDamageToTarget(livAttacker, Math.max(60.0F, amount * 0.40F));
                }

                ServerLevel sl = (ServerLevel) this.level();
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.5F, 1.4F);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0D, this.getZ(), 12, 0.5D, 0.5D, 0.5D, 0.05D);
                return false;
            } else {
                this.setCastingState(CAST_NONE);
                this.activeSkillTicks = 0;
                broadcastDialogue("Phòng ngự tuyệt đối của Gabriel bị xé rách?! Không thể nào!");
            }
        }

        // THẾ THÂN GƯƠNG BĂNG (MIRROR ICE DECOY): Dưới 50% HP, khi nhận đòn chí mạng
        if (this.getHealth() <= this.getMaxHealth() * 0.5F && mirrorDecoyCooldown <= 0 && amount >= 25.0F) {
            triggerMirrorDecoy((ServerLevel) this.level());
            return false;
        }

        // CƠ CHẾ 12 TẦNG VẢY RỒNG BẠCH BĂNG (Mạnh gấp 3 lần 4 tầng của Velgrynd)
        if (attacker instanceof Player) {
            int currentLayers = this.getDragonLayers();
            if (currentLayers > 1) {
                this.setDragonLayers(currentLayers - 1);

                if (currentLayers == MAX_DRAGON_LAYERS) {
                    this.broadcastDialogue("Hừ! Ngươi nghĩ đòn tấn công đó có thể làm trầy xước vảy rồng của Bạch Băng Long sao?");
                } else if (currentLayers == 8) {
                    this.broadcastDialogue("Khá lắm! Nhưng hàn khí của ta vẫn còn nguyên vẹn!");
                } else if (currentLayers == 4) {
                    this.broadcastDialogue("Dám ép ta đến mức này... Toàn bộ thế giới này sẽ chôn vùi trong băng tuyết!");
                }

                ServerLevel sl = (ServerLevel) this.level();
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2D, this.getZ(), 2, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.2D, this.getZ(), 25, 0.6D, 0.6D, 0.6D, 0.1D);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 1.4F);

                // Giữ lượng máu tương ứng với số tầng vảy băng long (12 tầng)
                this.setHealth(this.getMaxHealth() * (this.getDragonLayers() / (float) MAX_DRAGON_LAYERS));
                return false;
            } else if (currentLayers == 1) {
                this.setDragonLayers(0);
                this.broadcastDialogue("Không thể nào... Băng giá vĩnh hằng của ta... lại bị phá vỡ sao?! Guy...!");
                this.bossEvent.removeAllPlayers();
                return super.hurt(source, 30000.0F);
            }
        }

        // TAM TẦNG BĂNG HOA ẤN KÝ (TRI-FROST SIGILS): Giảm sát thương nhận vào từ các thực thể khác
        int sigils = getSigilCount();
        float damageReduction = sigils * 0.20F;
        float finalAmount = amount * (1.0F - damageReduction);

        this.damageTakenForSigil += finalAmount;
        if (this.damageTakenForSigil >= 3600.0F && sigils > 0) {
            this.damageTakenForSigil = 0.0F;
            setSigilCount(sigils - 1);
            ServerLevel sl = (ServerLevel) this.level();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.9F);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.1D);

            if (getSigilCount() == 0) {
                this.staggerTicks = 50;
                this.setDeltaMovement(0, -0.4D, 0);
                broadcastDialogue("Ấn ký băng tuyết đã vỡ... Hự!");
            }
        }

        return super.hurt(source, finalAmount);
    }

    private void triggerMirrorDecoy(ServerLevel sl) {
        this.mirrorDecoyCooldown = 700;
        Vec3 curPos = this.position();

        sl.playSound(null, curPos.x, curPos.y, curPos.z,
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 4.0F, 1.2F);
        sl.playSound(null, curPos.x, curPos.y, curPos.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 3.0F, 1.0F);

        sl.sendParticles(ParticleTypes.SNOWFLAKE, curPos.x, curPos.y + 1.0D, curPos.z, 60, 0.8D, 1.0D, 0.8D, 0.15D);
        sl.sendParticles(ParticleTypes.FLASH, curPos.x, curPos.y + 1.0D, curPos.z, 1, 0, 0, 0, 0);

        // Để lại một ngôi nhà tuyết / tượng băng tại vị trí cũ
        buildSnowMound(sl, BlockPos.containing(curPos), 3);

        Vec3 backward = this.getLookAngle().reverse().scale(12.0D);
        this.moveTo(curPos.x + backward.x, curPos.y + 2.0D, curPos.z + backward.z);
        this.setDeltaMovement(0, 0.2D, 0);

        sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 20, () -> {
            sl.playSound(null, curPos.x, curPos.y, curPos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.5F, 1.2F);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, curPos.x, curPos.y + 1.0D, curPos.z, 2, 0.5D, 0.5D, 0.5D, 0);
            AABB decoyBox = new AABB(curPos.x - 5.0D, curPos.y - 1.0D, curPos.z - 5.0D,
                    curPos.x + 5.0D, curPos.y + 4.0D, curPos.z + 5.0D);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, decoyBox, e -> e != this && e.isAlive());
            for (LivingEntity v : victims) {
                applyDamageToTarget(v, 105.0F); // 3x 35
            }
        }));

        broadcastDialogue("Ngươi chỉ đang chém vào ảo ảnh băng tuyết của ta mà thôi!");
    }

    private void triggerParallelExistence(ServerLevel sLevel, LivingEntity secondaryTarget) {
        this.broadcastDialogue("TỒN TẠI SONG SONG — Bản sao Băng Tinh của ta cũng dư sức đóng băng toàn bộ các ngươi!");
        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 1.3F);
        sLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0D, this.getZ(), 80, 0.8D, 1.2D, 0.8D, 0.12D);

        VelzardEntity clone = new VelzardEntity(ModEntities.VELZARD.get(), sLevel);
        clone.moveTo(this.getX() + 3.0D, this.getY(), this.getZ() + 3.0D, this.getYRot(), this.getXRot());
        clone.setClone(true);
        clone.setDragonLayers(Math.max(4, this.getDragonLayers() / 2));
        clone.setBoundTarget(secondaryTarget);
        sLevel.addFreshEntity(clone);
        this.activeClone = clone;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Client particles bão tuyết quanh thân Velzard
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(FROST_CYAN,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.6D,
                        this.getY() + this.random.nextDouble() * 2.2D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.6D,
                        0, 0.03D, 0);
                this.level().addParticle(ParticleTypes.SNOWFLAKE,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 2.2D,
                        this.getY() + this.random.nextDouble() * 2.4D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 2.2D,
                        (this.random.nextDouble() - 0.5D) * 0.08D, -0.04D, (this.random.nextDouble() - 0.5D) * 0.08D);
            }
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();

        // 0. Nếu vừa được summon bằng trứng/lệnh mà chưa tạo cấu trúc băng tuyết khởi đầu -> Tạo ngay!
        if (!hasGeneratedInitialStructures && !this.isClone() && this.tickCount == 2) {
            this.hasGeneratedInitialStructures = true;
            spawnGlacialArrivalWorldEffects(sl);
        }

        // 1. Cập nhật BossBar & Siêu Tốc Tái Sinh (150 HP/s khi chiến đấu, 300 HP/s khi ngoài giao tranh - Gấp 3 lần Velgrynd)
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(true);

        if (this.tickCount % 20 == 0 && this.isAlive() && this.getHealth() < this.getMaxHealth()) {
            float regenAmount = (this.getTarget() == null) ? 300.0F : 150.0F;
            this.heal(regenAmount);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.2D, this.getZ(), 10, 0.5D, 0.6D, 0.5D, 0.05D);
        }

        // Kiểm tra phân thân Tồn Tại Song Song
        if (this.isClone()) {
            if (this.boundTargetUuid != null) {
                Entity targetEntity = sl.getEntity(this.boundTargetUuid);
                if (targetEntity == null || !targetEntity.isAlive() || targetEntity.distanceToSqr(this) > 64.0D * 64.0D) {
                    this.bossEvent.removeAllPlayers();
                    this.discard();
                    return;
                }
            }
        } else if (this.tickCount % 20 == 0 && (this.activeClone == null || !this.activeClone.isAlive())) {
            long now = sl.getGameTime();
            recentAttackers.entrySet().removeIf(e -> (now - e.getValue()) > 200L);
            List<LivingEntity> validAttackers = new ArrayList<>();
            for (UUID uid : recentAttackers.keySet()) {
                Entity e = sl.getEntity(uid);
                if (e instanceof LivingEntity le && le.isAlive() && le.distanceToSqr(this) <= 45.0D * 45.0D) {
                    validAttackers.add(le);
                }
            }
            if (validAttackers.size() >= 2) {
                triggerParallelExistence(sl, validAttackers.get(1));
            }
        }

        // 2. BÃO TUYẾT DÀY ĐẶC & TẠO BĂNG TUYẾT / NHÀ TUYẾT LIÊN TỤC
        if (this.getTarget() != null && this.blizzardTicksRemaining < 200) {
            this.blizzardTicksRemaining = 600; // Duy trì bão tuyết dày đặc suốt toàn bộ trận chiến
        }
        if (blizzardTicksRemaining > 0) {
            blizzardTicksRemaining--;
            tickGlacialBlizzard(sl);
        }

        // Định kỳ kiến tạo thêm Nhà Tuyết (Igloo), Tháp Băng hoặc Khối Tuyết xung quanh chiến trường
        if (snowStructureCooldown > 0) {
            snowStructureCooldown--;
        } else if (!this.isClone() && (this.getTarget() != null || blizzardTicksRemaining > 0)) {
            snowStructureCooldown = 140 + this.random.nextInt(100);
            spawnRandomSnowOrIceStructureNearby(sl);
        }

        // 3. Xử lý Stagger (Kiệt Sức)
        if (staggerTicks > 0) {
            staggerTicks--;
            this.setDeltaMovement(0, -0.05D, 0);
            if (this.tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5D, this.getZ(), 5, 0.4D, 0.4D, 0.4D, 0.05D);
            }
            if (staggerTicks == 0) {
                setSigilCount(3);
                broadcastDialogue("Ấn ký băng hoa đã tái sinh... Trận chiến chỉ vừa mới bắt đầu!");
            }
            return;
        }

        // 4. Quản lý trạng thái Cuồng Nộ (Phase 2 Awakening < 40% HP)
        boolean enraged = this.getHealth() <= (this.getMaxHealth() * 0.40F) || this.getDragonLayers() <= 4;
        if (enraged != isEnraged()) {
            setEnraged(enraged);
            if (enraged) {
                this.bossEvent.setColor(BossEvent.BossBarColor.WHITE);
                broadcastDialogue("Băng Thần Giáng Lâm... Tuyệt Vọng Tuyệt Đối sẽ nhấn chìm thế giới này trong Kỷ Băng Hà!");
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 1.2F);
                spawnGlacialArrivalWorldEffects(sl);
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
        if (iceCannonCooldown > 0) iceCannonCooldown--;
        if (skillSealCooldown > 0) skillSealCooldown--;
        if (dragonFormCooldown > 0) dragonFormCooldown--;

        // 6. Mặt Đất Băng Vĩnh Cửu (Permafrost Field Aura - Bán kính 20m)
        if (this.tickCount % 20 == 0) {
            tickPermafrostAura(sl);
        }

        // Chặn toàn bộ kỹ năng nếu bị Phong Sát Kỹ Năng!
        if (SkillNullificationAbility.isEntitySkillSealed(this)) {
            this.activeSkillTicks = 0;
            this.setCastingState(CAST_NONE);
            if (this.tickCount % 15 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.5D, this.getZ(), 6, 0.5, 0.7, 0.5, 0.02);
            }
            return;
        }

        // Tự chuyển đổi giữa Dạng Người <-> Chân Thân Bạch Băng Long
        LivingEntity curTarget = this.getTarget();
        if (curTarget != null && curTarget.isAlive() && dragonFormCooldown <= 0) {
            dragonFormCooldown = 280;
            if (!isDragonForm() && (isEnraged() || this.getHealth() < this.getMaxHealth() * 0.85F || this.random.nextFloat() < 0.65F)) {
                setDragonForm(true);
                broadcastDialogue("Chân thân Bạch Băng Long! Hãy đóng băng trong tuyệt vọng đi!");
            } else if (isDragonForm() && !isEnraged() && this.random.nextFloat() < 0.30F) {
                setDragonForm(false);
            }
        }

        // 7. Quản lý kỹ năng đang thi triển
        if (activeSkillTicks > 0) {
            tickActiveSkill(sl);
        } else {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                setFlyingAnim(true);
                this.setNoGravity(true);

                double desiredY = target.getY() + 3.2D;
                if (this.getY() < desiredY) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.045D, 0));
                }

                double distSq = this.distanceToSqr(target);
                chooseSkillToCast(sl, target, distSq);
            } else {
                setFlyingAnim(false);
                this.setNoGravity(false);
            }
        }
    }

    /**
     * Khi Bạch Băng Long Velzard giáng thế:
     * - Đóng băng mặt đất và mặt nước trong bán kính rộng thành ICE, PACKED_ICE, BLUE_ICE, SNOW_BLOCK.
     * - Dựng lên Nhà Tuyết (Snow Houses / Igloos), Tháp Băng (Glacial Spires) và các Đụn Tuyết dày đặc.
     */
    private void spawnGlacialArrivalWorldEffects(ServerLevel sl) {
        broadcastDialogue("Kỷ Băng Hà Vĩnh Cửu... Hãy phủ trắng toàn bộ thế giới này!");
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 0.85F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0F, 1.1F);

        BlockPos center = this.blockPosition();
        // 1. Đóng băng địa hình và tạo tuyết dày quanh chỗ xuất hiện (bán kính 14m)
        freezeGroundPatch(sl, center, 14);

        // 2. Dựng 2 Ngôi Nhà Tuyết (Snow Houses / Igloos) và 4 Tháp Băng Cực Hàn xung quanh vị trí xuất hiện
        for (int i = 0; i < 2; i++) {
            double angle = (i * Math.PI) + (this.random.nextDouble() * 0.8D);
            int dist = 10 + this.random.nextInt(6);
            int hx = center.getX() + (int) Math.round(Math.cos(angle) * dist);
            int hz = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int hy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hx, hz);
            buildSnowHouse(sl, new BlockPos(hx, hy, hz));
        }

        for (int i = 0; i < 4; i++) {
            double angle = (i * Math.PI / 2.0D) + 0.4D;
            int dist = 8 + this.random.nextInt(8);
            int sx = center.getX() + (int) Math.round(Math.cos(angle) * dist);
            int sz = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int sy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            buildGlacialSpire(sl, new BlockPos(sx, sy, sz), 6 + this.random.nextInt(5));
        }
    }

    private void spawnRandomSnowOrIceStructureNearby(ServerLevel sl) {
        if (!sl.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 56.0D)) return;
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        int dist = 8 + this.random.nextInt(14);
        int sx = this.blockPosition().getX() + (int) Math.round(Math.cos(angle) * dist);
        int sz = this.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * dist);
        int sy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
        BlockPos base = new BlockPos(sx, sy, sz);

        int roll = this.random.nextInt(3);
        if (roll == 0) {
            buildSnowHouse(sl, base);
        } else if (roll == 1) {
            buildGlacialSpire(sl, base, 5 + this.random.nextInt(5));
        } else {
            buildSnowMound(sl, base, 3 + this.random.nextInt(2));
        }
    }

    /**
     * Xây dựng Ngôi Nhà Tuyết / Lâu Đài Tuyết nhỏ (Snow House / Glacial Igloo) bằng SNOW_BLOCK, PACKED_ICE và BLUE_ICE
     */
    private void buildSnowHouse(ServerLevel sl, BlockPos center) {
        if (center.getY() <= sl.getMinBuildHeight() + 4 || center.getY() >= sl.getMaxBuildHeight() - 10) return;

        int radius = 3; // Nhà tuyết vòm 7x7x5
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double horizDistSq = dx * dx + dz * dz;
                if (horizDistSq > (radius + 0.5D) * (radius + 0.5D)) continue;

                // Sàn nhà bằng Băng Nén & Băng Xanh
                BlockPos floorPos = center.offset(dx, -1, dz);
                if (canReplaceWithIceOrSnow(sl, floorPos)) {
                    Block floorBlock = (horizDistSq <= 2.0D) ? Blocks.BLUE_ICE : Blocks.PACKED_ICE;
                    sl.setBlock(floorPos, floorBlock.defaultBlockState(), 2);
                }

                // Vòm nhà tuyết cao 4 tầng
                for (int dy = 0; dy <= 4; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!canReplaceWithIceOrSnow(sl, p)) continue;

                    double domeDist = Math.sqrt(horizDistSq + (dy * 0.85D) * (dy * 0.85D));
                    // Cửa ra vào ở hướng +Z (dz == radius, dx == 0, dy == 0..1)
                    boolean isDoorway = (dz >= radius - 1 && Math.abs(dx) <= 0 && dy <= 1);
                    if (isDoorway) {
                        sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        continue;
                    }

                    if (domeDist >= radius - 0.65D && domeDist <= radius + 0.45D) {
                        // Tường và mái vòm bằng Khối Tuyết, xen kẽ cửa sổ Băng Xanh
                        boolean isWindow = (dy == 1 && (Math.abs(dx) == radius && dz == 0 || Math.abs(dz) == radius && dx == 0));
                        Block wallBlock = isWindow ? Blocks.BLUE_ICE : (dy == 0 && (Math.abs(dx) == Math.abs(dz)) ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK);
                        sl.setBlock(p, wallBlock.defaultBlockState(), 2);
                    } else if (domeDist < radius - 0.65D) {
                        sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                // Phủ lớp tuyết trên nóc nhà tuyết
                BlockPos roofTop = center.offset(dx, 5, dz);
                if (sl.getBlockState(roofTop.below()).is(Blocks.SNOW_BLOCK) && sl.getBlockState(roofTop).isAir()) {
                    sl.setBlock(roofTop, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3 + this.random.nextInt(4)), 2);
                }
            }
        }

        // Đặt đèn linh hồn băng giá hoặc khối băng phát sáng bên trong nhà tuyết
        BlockPos lanternPos = center.above(3);
        if (sl.getBlockState(lanternPos).isAir()) {
            sl.setBlock(lanternPos, Blocks.SEA_LANTERN.defaultBlockState(), 2);
        }
        sl.sendParticles(ParticleTypes.SNOWFLAKE, center.getX() + 0.5D, center.getY() + 2.0D, center.getZ() + 0.5D, 35, 2.5D, 1.5D, 2.5D, 0.05D);
    }

    /**
     * Xây dựng Tháp Băng Cực Hàn (Glacial Spire) bằng BLUE_ICE, PACKED_ICE và SNOW_BLOCK
     */
    private void buildGlacialSpire(ServerLevel sl, BlockPos base, int height) {
        if (base.getY() <= sl.getMinBuildHeight() + 4 || base.getY() >= sl.getMaxBuildHeight() - height - 2) return;
        for (int y = -1; y <= height; y++) {
            int r = (y < height / 3) ? 1 : 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 1 && y > 1) continue;
                    BlockPos p = base.offset(dx, y, dz);
                    if (canReplaceWithIceOrSnow(sl, p)) {
                        Block b = (y >= height - 1) ? Blocks.BLUE_ICE
                                : ((y % 2 == 0) ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK);
                        sl.setBlock(p, b.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /**
     * Tạo Khối Tuyết & Đụn Tuyết Dày (Snow Mound & Powder Snow)
     */
    private void buildSnowMound(ServerLevel sl, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int h = Math.max(1, radius - (int) Math.sqrt(dx * dx + dz * dz));
                for (int dy = 0; dy < h; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (canReplaceWithIceOrSnow(sl, p)) {
                        Block b = (this.random.nextFloat() < 0.25F) ? Blocks.POWDER_SNOW
                                : ((this.random.nextFloat() < 0.30F) ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK);
                        sl.setBlock(p, b.defaultBlockState(), 2);
                    }
                }
                BlockPos top = center.offset(dx, h, dz);
                if (sl.getBlockState(top).isAir() && sl.getBlockState(top.below()).isSolid()) {
                    sl.setBlock(top, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2 + this.random.nextInt(5)), 2);
                }
            }
        }
    }

    /**
     * Đóng băng mặt nước, dung nham và phủ băng/tuyết lên mặt đất trong bán kính cho trước
     */
    private void freezeGroundPatch(ServerLevel sl, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (radius > 6 && this.random.nextFloat() > 0.45F) continue;

                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX() + dx, center.getZ() + dz);
                BlockPos groundPos = new BlockPos(center.getX() + dx, y - 1, center.getZ() + dz);
                BlockPos abovePos = groundPos.above();

                BlockState groundState = sl.getBlockState(groundPos);
                if (groundState.is(Blocks.WATER)) {
                    Block iceType = (this.random.nextFloat() < 0.3F) ? Blocks.BLUE_ICE
                            : ((this.random.nextFloat() < 0.6F) ? Blocks.PACKED_ICE : Blocks.ICE);
                    sl.setBlock(groundPos, iceType.defaultBlockState(), 2);
                } else if (groundState.is(Blocks.LAVA)) {
                    sl.setBlock(groundPos, Blocks.PACKED_ICE.defaultBlockState(), 2);
                } else if (groundState.isSolidRender(sl, groundPos) && canReplaceWithIceOrSnow(sl, groundPos)) {
                    float roll = this.random.nextFloat();
                    if (roll < 0.20F) {
                        sl.setBlock(groundPos, Blocks.PACKED_ICE.defaultBlockState(), 2);
                    } else if (roll < 0.32F) {
                        sl.setBlock(groundPos, Blocks.BLUE_ICE.defaultBlockState(), 2);
                    } else if (roll < 0.55F) {
                        sl.setBlock(groundPos, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                    }
                }

                BlockState aboveState = sl.getBlockState(abovePos);
                if (aboveState.isAir() && sl.getBlockState(groundPos).isSolidRender(sl, groundPos)) {
                    if (this.random.nextFloat() < 0.15F) {
                        sl.setBlock(abovePos, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                    } else if (this.random.nextFloat() < 0.12F) {
                        sl.setBlock(abovePos, Blocks.POWDER_SNOW.defaultBlockState(), 2);
                    } else {
                        int layers = 2 + this.random.nextInt(6);
                        sl.setBlock(abovePos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), 2);
                    }
                } else if (aboveState.is(Blocks.SNOW)) {
                    int curLayers = aboveState.getValue(SnowLayerBlock.LAYERS);
                    if (curLayers < 7) {
                        sl.setBlock(abovePos, aboveState.setValue(SnowLayerBlock.LAYERS, Math.min(8, curLayers + 2)), 2);
                    } else {
                        sl.setBlock(abovePos, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private boolean canReplaceWithIceOrSnow(ServerLevel sl, BlockPos pos) {
        BlockState state = sl.getBlockState(pos);
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER) || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.END_PORTAL)) {
            return false;
        }
        if (sl.getBlockEntity(pos) != null) {
            return false;
        }
        return state.getDestroySpeed(sl, pos) >= 0.0F;
    }

    private void tickGlacialBlizzard(ServerLevel sl) {
        if (!sl.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 56.0D)) {
            return;
        }

        // BÃO TUYẾT DÀY ĐẶC (Dense Whiteout Blizzard): Mưa tuyết cuồn cuộn diện rộng 40m
        if (this.tickCount % 2 == 0) {
            for (int i = 0; i < 18; i++) {
                double rx = this.getX() + (this.random.nextDouble() - 0.5D) * 42.0D;
                double rz = this.getZ() + (this.random.nextDouble() - 0.5D) * 42.0D;
                double ry = this.getY() + 0.5D + this.random.nextDouble() * 14.0D;
                sl.sendParticles(ParticleTypes.SNOWFLAKE, rx, ry, rz, 2, -0.45D, -0.35D, -0.45D, 0.12D);
                if (i % 3 == 0) {
                    sl.sendParticles(ParticleTypes.WHITE_ASH, rx, ry, rz, 2, 0.4D, 0.2D, 0.4D, 0.08D);
                }
                if (i % 5 == 0) {
                    sl.sendParticles(FROST_WHITE, rx, ry * 0.8D, rz, 1, 0.3D, 0.2D, 0.3D, 0.04D);
                }
            }
        }

        // Liên tục đóng băng nước, tạo băng & phủ tuyết dày xung quanh Velzard mỗi 10 ticks
        if (this.tickCount % 10 == 0) {
            int offsetX = this.random.nextInt(25) - 12;
            int offsetZ = this.random.nextInt(25) - 12;
            freezeGroundPatch(sl, this.blockPosition().offset(offsetX, 0, offsetZ), 4);
        }

        // Âm thanh gió bão tuyết gầm rú mỗi 35 ticks
        if (this.tickCount % 35 == 0) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.WEATHER, 2.5F, 0.75F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 1.8F, 0.6F);
        }

        // Đóng băng nhiều & gây hạ thân nhiệt diện rộng cho thực thể trong bão tuyết (mỗi 15 ticks)
        if (this.tickCount % 15 == 0) {
            AABB blizzardBox = this.getBoundingBox().inflate(32.0D);
            List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, blizzardBox, e -> e != this && e.isAlive());
            for (LivingEntity e : list) {
                if (!(e instanceof VelzardEntity)) {
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 140, e.getTicksFrozen() + 70));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2, false, false));
                }
            }
        }
    }

    private void tickPermafrostAura(ServerLevel sl) {
        AABB box = this.getBoundingBox().inflate(20.0D);
        List<Player> nearbyPlayers = sl.getEntitiesOfClass(Player.class, box, Player::isAlive);

        for (Player p : nearbyPlayers) {
            Vec3 motion = p.getDeltaMovement();
            boolean isMoving = (motion.x * motion.x + motion.z * motion.z) > 0.002D;

            UUID pid = p.getUUID();
            int standing = standingTickCounters.getOrDefault(pid, 0);

            if (!isMoving) {
                standing += 2; // Đóng băng nhanh gấp đôi trong Lãnh Vực Vĩnh Cửu
                standingTickCounters.put(pid, standing);

                if (standing >= 8) {
                    standingTickCounters.put(pid, 0);
                    p.setTicksFrozen(p.getTicksRequiredToFreeze() + 160);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255, false, false));
                    // Đóng băng tạo khối băng bao quanh chân người chơi đứng yên
                    BlockPos feet = p.blockPosition();
                    if (canReplaceWithIceOrSnow(sl, feet)) {
                        sl.setBlock(feet, Blocks.POWDER_SNOW.defaultBlockState(), 2);
                    }
                    p.displayClientMessage(Component.literal("§b§l[LÃNH KHÍ VĨNH CỬU] §cBạn đã bị đóng băng cứng do đứng yên trước Bạch Băng Long Velzard!"), true);
                } else if (standing >= 4) {
                    p.displayClientMessage(Component.literal("§b§l[LÃNH KHÍ VĨNH CỬU] §eHàn khí đang đóng băng cơ thể bạn! Hãy di chuyển ngay (" + standing + "/8)!"), true);
                }
            } else {
                if (standing > 0) {
                    standingTickCounters.put(pid, Math.max(0, standing - 2));
                }
            }
        }
    }

    private void chooseSkillToCast(ServerLevel sl, LivingEntity target, double distSq) {
        int rate = isEnraged() ? 2 : 1;

        // 0. Phong Sát Kỹ Năng (White Ice Dragon Nullification)
        if (skillSealCooldown <= 0 && distSq <= 30.0D * 30.0D) {
            SkillNullificationAbility.castBossSkillSeal(sl, this, 32.0F, 180);
            skillSealCooldown = 360;
            return;
        }

        // 0.5 Băng Pháo Bão Tuyết (Cryo Ice Sphere Cannon Volley)
        if (iceCannonCooldown <= 0 && distSq <= 36.0D * 36.0D) {
            castIceCannonVolley(sl, target);
            iceCannonCooldown = 85 / rate;
            return;
        }

        // 1. Hơi Thở Bạch Băng Long (Frost Dragon Breath)
        if (dragonBreathCooldown <= 0 && distSq <= 30.0D * 30.0D) {
            startSkill(CAST_DRAGON_BREATH, 40, target.position());
            dragonBreathCooldown = 280 / rate;
            return;
        }

        // 2. Ngưng Trệ Động Năng (Kinetic Cessation - Cthulhu)
        if (kineticCessationCooldown <= 0 && distSq <= 24.0D * 24.0D) {
            startSkill(CAST_KINETIC_CESSATION, 28, target.position());
            kineticCessationCooldown = 180 / rate;
            return;
        }

        // 3. Lãnh Ngục Địa Tinh (Glacial Prison Trap)
        if (glacialPrisonCooldown <= 0 && distSq <= 26.0D * 26.0D) {
            startSkill(CAST_GLACIAL_PRISON, 22, target.position());
            glacialPrisonCooldown = 150 / rate;
            return;
        }

        // 4. Cụm Gai Băng Kim Cương (Diamond Icicle Gatling)
        if (icicleGatlingCooldown <= 0 && distSq <= 32.0D * 32.0D) {
            startSkill(CAST_ICICLE_GATLING, 24, target.position());
            icicleGatlingCooldown = 95 / rate;
            return;
        }

        // 5. Băng Tinh Bất Hoại (Snow Crystal - Gabriel)
        if (!isEnraged() && snowCrystalCooldown <= 0 && this.getHealth() <= this.getMaxHealth() * 0.85F) {
            startSkill(CAST_SNOW_CRYSTAL, 80, this.position());
            snowCrystalCooldown = 280;
            return;
        }

        // 6. Phấn Hoa Tuyết Trắng (Whiteout Frost Bloom)
        if (frostBloomCooldown <= 0 && distSq <= 25.0D * 25.0D) {
            castFrostBloom(sl, target);
            frostBloomCooldown = 140 / rate;
        }
    }

    private void castIceCannonVolley(ServerLevel sl, LivingEntity target) {
        Vec3 start = this.getEyePosition();
        Vec3 dir = target.getEyePosition().subtract(start).normalize();
        for (int i = -2; i <= 2; i++) {
            Vec3 aim = dir.add(i * 0.14D, 0.04D, -i * 0.14D).normalize();
            var sb = new net.minecraft.world.entity.projectile.Snowball(sl, this);
            sb.setPos(start.x + aim.x * 2.5D, start.y + aim.y * 1.5D, start.z + aim.z * 2.5D);
            sb.shoot(aim.x, aim.y, aim.z, 2.2F, 1.0F);
            sl.addFreshEntity(sb);
        }
        for (double d = 2.0D; d <= Math.min(32.0D, this.distanceTo(target) + 2.0D); d += 1.4D) {
            Vec3 p = start.add(dir.scale(d));
            sl.sendParticles(FROST_CYAN, p.x, p.y, p.z, 6, 0.5, 0.5, 0.5, 0.04);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 5, 0.4, 0.4, 0.4, 0.02);
        }
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 3.0F, 1.3F);
        target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 180, target.getTicksFrozen() + 120));
        target.hurt(this.damageSources().freeze(), 68.0F);
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
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.5F, 1.4F);
            }
            case CAST_KINETIC_CESSATION -> {
                if (targetPos != null) {
                    sl.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 3.0F, 1.2F);
                    for (int i = 0; i < 16; i++) {
                        double angle = (i * 22.5D) * Math.PI / 180.0D;
                        double px = targetPos.x + Math.cos(angle) * 4.5D;
                        double pz = targetPos.z + Math.sin(angle) * 4.5D;
                        sl.sendParticles(FROST_CYAN, px, targetPos.y + 0.2D, pz, 2, 0, 0.05D, 0, 0);
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
                this.setDeltaMovement(0, 0.01D, 0);
                if (this.tickCount % 4 == 0) {
                    sl.sendParticles(FROST_WHITE, this.getX(), this.getY() + 1.0D, this.getZ(), 10, 1.2D, 1.2D, 1.2D, 0.05D);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0D, this.getZ(), 8, 1.0D, 1.0D, 1.0D, 0.02D);
                }
            }
            case CAST_KINETIC_CESSATION -> {
                if (castingTargetPos != null && activeSkillTicks % 2 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (i * 30.0D) * Math.PI / 180.0D;
                        double px = castingTargetPos.x + Math.cos(angle) * 4.5D;
                        double pz = castingTargetPos.z + Math.sin(angle) * 4.5D;
                        sl.sendParticles(FROST_CYAN, px, castingTargetPos.y + 0.1D, pz, 1, 0, 0.02D, 0, 0);
                    }
                }
                if (activeSkillTicks == 1 && castingTargetPos != null) {
                    sl.playSound(null, castingTargetPos.x, castingTargetPos.y, castingTargetPos.z,
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 1.4F);
                    sl.sendParticles(ParticleTypes.FLASH, castingTargetPos.x, castingTargetPos.y + 1.0D, castingTargetPos.z, 2, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, castingTargetPos.x, castingTargetPos.y + 1.0D, castingTargetPos.z, 60, 4.5D, 2.0D, 4.5D, 0.12D);

                    // Đóng băng mặt đất và nhốt mục tiêu trong khối băng
                    freezeGroundPatch(sl, BlockPos.containing(castingTargetPos), 6);

                    AABB freezeBox = new AABB(castingTargetPos.x - 6.0D, castingTargetPos.y - 2.0D, castingTargetPos.z - 6.0D,
                            castingTargetPos.x + 6.0D, castingTargetPos.y + 5.0D, castingTargetPos.z + 6.0D);
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, freezeBox, e -> e != this && e.isAlive());
                    for (LivingEntity e : list) {
                        e.setDeltaMovement(Vec3.ZERO);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false));
                        e.setTicksFrozen(e.getTicksRequiredToFreeze() + 200);
                        applyDamageToTarget(e, 195.0F); // Gấp 3 lần sát thương Time Collapse của Velgrynd (65 * 3 = 195)
                    }
                }
            }
            case CAST_ICICLE_GATLING -> {
                if (activeSkillTicks <= 16 && activeSkillTicks % 2 == 0) {
                    LivingEntity t = this.getTarget();
                    if (t != null && t.isAlive()) {
                        Vec3 start = this.getEyePosition();
                        Vec3 dir = t.getEyePosition().subtract(start).normalize()
                                .add((this.random.nextDouble() - 0.5D) * 0.14D, (this.random.nextDouble() - 0.5D) * 0.1D, (this.random.nextDouble() - 0.5D) * 0.14D).normalize();

                        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 2.5F, 1.8F);

                        for (double d = 1.0D; d < 30.0D; d += 1.4D) {
                            Vec3 pt = start.add(dir.scale(d));
                            sl.sendParticles(ParticleTypes.SNOWFLAKE, pt.x, pt.y, pt.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                            sl.sendParticles(FROST_CYAN, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);

                            AABB hitBox = new AABB(pt.x - 1.2D, pt.y - 1.2D, pt.z - 1.2D, pt.x + 1.2D, pt.y + 1.2D, pt.z + 1.2D);
                            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e.isAlive());
                            if (!victims.isEmpty()) {
                                for (LivingEntity v : victims) {
                                    applyDamageToTarget(v, 84.0F); // Gấp 3 lần Heat Blades của Velgrynd (28 * 3 = 84)
                                }
                                break;
                            }
                        }
                    }
                }
            }
            case CAST_GLACIAL_PRISON -> {
                if (activeSkillTicks == 1 && castingTargetPos != null) {
                    sl.playSound(null, castingTargetPos.x, castingTargetPos.y, castingTargetPos.z,
                            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 4.0F, 0.7F);
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, castingTargetPos.x, castingTargetPos.y + 1.0D, castingTargetPos.z, 2, 0.5D, 0.5D, 0.5D, 0);

                    // Dựng ngục băng / nhà băng bao vây mục tiêu
                    BlockPos prisonPos = BlockPos.containing(castingTargetPos);
                    buildGlacialSpire(sl, prisonPos.offset(2, 0, 0), 6);
                    buildGlacialSpire(sl, prisonPos.offset(-2, 0, 0), 6);
                    buildGlacialSpire(sl, prisonPos.offset(0, 0, 2), 6);
                    buildGlacialSpire(sl, prisonPos.offset(0, 0, -2), 6);
                    freezeGroundPatch(sl, prisonPos, 5);

                    AABB trapBox = new AABB(castingTargetPos.x - 4.5D, castingTargetPos.y - 1.5D, castingTargetPos.z - 4.5D,
                            castingTargetPos.x + 4.5D, castingTargetPos.y + 4.5D, castingTargetPos.z + 4.5D);
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, trapBox, e -> e != this && e.isAlive());
                    for (LivingEntity e : list) {
                        applyDamageToTarget(e, 135.0F); // Gấp 3 lần Meteor Rain của Velgrynd (45 * 3 = 135)
                        e.setTicksFrozen(e.getTicksRequiredToFreeze() + 160);
                    }
                }
            }
            case CAST_DRAGON_BREATH -> {
                if (activeSkillTicks <= 20) {
                    Vec3 look = this.getLookAngle();
                    Vec3 eye = this.getEyePosition();

                    sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 3.5F, 0.6F);

                    for (double d = 2.0D; d < 26.0D; d += 2.0D) {
                        double spread = d * 0.28D;
                        Vec3 centerPt = eye.add(look.scale(d));

                        sl.sendParticles(ParticleTypes.SNOWFLAKE, centerPt.x, centerPt.y, centerPt.z, 12, spread, spread * 0.5D, spread, 0.1D);
                        sl.sendParticles(FROST_CYAN, centerPt.x, centerPt.y, centerPt.z, 6, spread, spread * 0.5D, spread, 0.05D);

                        // Phá hủy Đa Trùng Kết Giới trong luồng thở
                        MultilayerBarrierAbility.shatterBarrierNear(sl, centerPt, spread + 2.5D, "Hơi Thở Bạch Băng Long Velzard");

                        // Đóng băng mặt đất dọc theo luồng hơi thở Bạch Băng Long
                        if (activeSkillTicks % 4 == 0) {
                            freezeGroundPatch(sl, BlockPos.containing(centerPt), 3);
                        }

                        AABB coneBox = new AABB(centerPt.x - spread, centerPt.y - spread * 0.6D, centerPt.z - spread,
                                centerPt.x + spread, centerPt.y + spread * 0.6D, centerPt.z + spread);
                        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, coneBox, e -> e != this && e.isAlive());
                        for (LivingEntity victim : list) {
                            applyDamageToTarget(victim, 300.0F); // Gấp 3 lần Cardinal Acceleration của Velgrynd (100 * 3 = 300)
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

        for (int i = 0; i < 6; i++) {
            double angle = (i * 60.0D) * Math.PI / 180.0D;
            double px = this.getX() + Math.cos(angle) * 3.0D;
            double pz = this.getZ() + Math.sin(angle) * 3.0D;
            double py = this.getY() + 1.5D;

            sl.sendParticles(FROST_WHITE, px, py, pz, 12, 0.2D, 0.2D, 0.2D, 0.02D);
            sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 3, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        if (target != null && target.isAlive()) {
            applyDamageToTarget(target, 120.0F);
            buildSnowMound(sl, target.blockPosition(), 3);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (this.isClone()) return;

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
        compound.putInt("DragonLayers", getDragonLayers());
        compound.putBoolean("IsEnraged", isEnraged());
        compound.putBoolean("IsClone", isClone());
        compound.putInt("BlizzardTicks", blizzardTicksRemaining);
        compound.putBoolean("GeneratedStructures", hasGeneratedInitialStructures);
        compound.putBoolean("DragonForm", isDragonForm());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SigilCount")) {
            setSigilCount(compound.getInt("SigilCount"));
        }
        if (compound.contains("DragonLayers")) {
            setDragonLayers(compound.getInt("DragonLayers"));
        }
        if (compound.contains("IsEnraged")) {
            setEnraged(compound.getBoolean("IsEnraged"));
        }
        if (compound.contains("IsClone")) {
            setClone(compound.getBoolean("IsClone"));
        }
        if (compound.contains("BlizzardTicks")) {
            this.blizzardTicksRemaining = compound.getInt("BlizzardTicks");
        }
        if (compound.contains("GeneratedStructures")) {
            this.hasGeneratedInitialStructures = compound.getBoolean("GeneratedStructures");
        }
        if (compound.contains("DragonForm")) {
            setDragonForm(compound.getBoolean("DragonForm"));
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
            if (distSq > 12.0D * 12.0D) {
                velzard.getNavigation().moveTo(target, 1.45D);
            } else if (distSq < 4.5D * 4.5D) {
                velzard.doHurtTarget(target);
                Vec3 away = velzard.position().subtract(target.position()).normalize().scale(7.0D);
                velzard.getNavigation().moveTo(velzard.getX() + away.x, velzard.getY() + 1.5D, velzard.getZ() + away.z, 1.15D);
            }
        }
    }
}
