package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.content.divine.JacobsLadderAbility;
import com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility;
import com.minhphuc.weapons.content.tensura.HorizontalHolyBeamAbility;
import com.minhphuc.weapons.content.soul.EntitySoulItem;
import com.minhphuc.weapons.content.soul.SoulType;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.minhphuc.weapons.content.divine.DivineArmorItem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Không Vong (Kūbō / The Black Sun God Embryo) - Dark Gathering.
 * - 3 Dạng Thể:
 *   + Dạng Thường (FORM_NORMAL): Phôi thai ban đầu (26 HP, 35 DMG). Nuốt linh hồn thường tăng +30% HP & DMG, nuốt linh hồn ác ma tăng +60% HP & DMG.
 *   + Dạng Tối Thượng (FORM_ULTIMATE): Kích hoạt khi nuốt linh hồn Milim hoặc Velgrynd (+500% HP & DMG, kháng 95% sát thương người chơi, miễn nhiễm Uriel/Regalia).
 *   + Dạng Hoàn Chỉnh (FORM_COMPLETE - Bạch Nhật Tà Thần): Kích hoạt khi hấp thụ Hạt Giống Ma Vương (Màu trắng, kháng 100% mọi đòn trừ Linh Tử Băng Hoại).
 * - Tên hiển thị: Luôn giữ nguyên tên thuần túy "Không Vong", không hiển thị % trên tên (dùng Thẩm Định Vạn Vật để quét).
 * - AI thông minh: Nhận biết Linh Tử Băng Hoại & Nấc Thang Jacob để tháo chạy né tránh.
 * - Đánh xuyên qua Giáp Thần Thoại & Xé rách cả Trận Đồ Cưỡng Chế Tai Ương khi ở dạng Tối Thượng/Hoàn Chỉnh.
 */
public class KuboEntity extends Monster {

    public static final int FORM_NORMAL = 0;
    public static final int FORM_ULTIMATE = 1;
    public static final int FORM_COMPLETE = 2;

    public static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_FORM =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_MOB_SOULS_COUNT =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_DEMON_SOULS_COUNT =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_SPECIFIC_DEMON =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_ABSORBED_SOUL_TYPE =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> DATA_ABSORBED_SOUL_NAME =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_ABSORBED_SKILL_NAME =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> DATA_BONUS_HP =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_BONUS_ARMOR =
            SynchedEntityData.defineId(KuboEntity.class, EntityDataSerializers.FLOAT);

    // Trạng thái State machine
    public static final int STATE_IDLE = 0;
    public static final int STATE_SWOOPING = 1;
    public static final int STATE_BEAMING = 2;
    public static final int STATE_FLEEING = 3;
    public static final int STATE_ABSORBED_SKILL = 4;

    private int beamCooldown = 100; // 5s cooldown
    private int absorbedSkillCooldown = 120; // 6s cooldown
    private int voidScytheCooldown = 140; // 7s cooldown (Trảm Khí Hư Vô)
    private int gravityWellCooldown = 280; // 14s cooldown (Hố Đen Hút Linh Hồn)
    private int screechCooldown = 400; // 20s cooldown (Tiếng Hét Vô Tận)
    private int screechTelegraphTicks = 0; // 24 ticks (1.2s telegraph)

    public static class ActiveGravityWell {
        public final ServerLevel level;
        public final Vec3 center;
        public int ticksRemaining = 60; // 3s
        public boolean disrupted = false;

        public ActiveGravityWell(ServerLevel level, Vec3 center) {
            this.level = level;
            this.center = center;
        }
    }
    private final List<ActiveGravityWell> activeWells = new ArrayList<>();

    public KuboEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        updateCustomDisplayName();
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D) // Máu ngang zombie
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.ATTACK_DAMAGE, 35.0D) // Sát thương khá đau
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_IDLE);
        builder.define(DATA_FORM, FORM_NORMAL);
        builder.define(DATA_MOB_SOULS_COUNT, 0);
        builder.define(DATA_DEMON_SOULS_COUNT, 0);
        builder.define(DATA_SPECIFIC_DEMON, -1);
        builder.define(DATA_ABSORBED_SOUL_TYPE, -1);
        builder.define(DATA_ABSORBED_SOUL_NAME, "");
        builder.define(DATA_ABSORBED_SKILL_NAME, "");
        builder.define(DATA_BONUS_HP, 0.0F);
        builder.define(DATA_BONUS_ARMOR, 0.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        // Ưu tiên 0: Nhận biết Linh Tử Băng Hoại & Nấc Thang Jacob để tháo chạy né tránh
        this.goalSelector.addGoal(0, new KuboDodgeSanctuaryGoal(this));
        // Ưu tiên 1: Tìm và nuốt chửng linh hồn / Hạt Giống Ma Vương trên chiến trường
        this.goalSelector.addGoal(1, new KuboSeekSoulGoal(this));
        // Ưu tiên 2: Bỏ chạy khi máu dưới 30%
        this.goalSelector.addGoal(2, new KuboFleeGoal(this));
        // Ưu tiên 3: Tác chiến thông minh (xà xuống đánh hoặc bay cao dùng skill)
        this.goalSelector.addGoal(3, new KuboTacticalCombatGoal(this));
        // Ưu tiên 4: Bay tuần tra trên cao
        this.goalSelector.addGoal(4, new KuboHighCruiseGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> !(e instanceof KuboEntity) && e.isAlive()));
    }

    public int getForm() {
        return this.getEntityData().get(DATA_FORM);
    }

    public boolean isNormal() {
        return getForm() == FORM_NORMAL;
    }

    public boolean isUltimate() {
        return getForm() == FORM_ULTIMATE;
    }

    public boolean isComplete() {
        return getForm() == FORM_COMPLETE;
    }

    public boolean isUltimateOrComplete() {
        return getForm() >= FORM_ULTIMATE;
    }

    public int getMobSoulsCount() {
        return this.getEntityData().get(DATA_MOB_SOULS_COUNT);
    }

    public int getDemonSoulsCount() {
        return this.getEntityData().get(DATA_DEMON_SOULS_COUNT);
    }

    public int getSpecificDemon() {
        return this.getEntityData().get(DATA_SPECIFIC_DEMON);
    }

    public void updateCustomDisplayName() {
        if (isComplete()) {
            this.setCustomName(Component.literal("§f§l[KHÔNG VONG HOÀN CHỈNH] §e§lKūbō"));
        } else if (isUltimate()) {
            this.setCustomName(Component.literal("§4§l[KHÔNG VONG TỐI THƯỢNG] §0§lKūbō"));
        } else {
            this.setCustomName(Component.literal("§0§l[KHÔNG VONG] §4§lKūbō"));
        }
    }

    /**
     * Tái tính toán toàn bộ thuộc tính HP, DMG, Giáp theo số lượng linh hồn và thể trạng tiến hóa
     */
    public void recalculateAttributes() {
        double mobMult = getMobSoulsCount() * 0.30D;
        double demonMult = getDemonSoulsCount() * 0.60D;
        double formMult = isComplete() ? 6.00D : (isUltimate() ? 5.00D : 0.0D);
        double totalMult = 1.0D + mobMult + demonMult + formMult;

        double baseHp = 26.0D * totalMult + this.getEntityData().get(DATA_BONUS_HP);
        double baseDmg = 35.0D * totalMult;
        double baseArmor = 4.0D + (isUltimateOrComplete() ? 25.0D : 0.0D) + this.getEntityData().get(DATA_BONUS_ARMOR);
        double baseToughness = 2.0D + (isUltimateOrComplete() ? 15.0D : 0.0D);

        var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(baseHp);
        }
        var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.setBaseValue(baseDmg);
        }
        var armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(baseArmor);
        }
        var toughAttr = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughAttr != null) {
            toughAttr.setBaseValue(baseToughness);
        }

        updateCustomDisplayName();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);

        if (beamCooldown > 0) beamCooldown--;
        if (absorbedSkillCooldown > 0) absorbedSkillCooldown--;
        if (voidScytheCooldown > 0) voidScytheCooldown--;
        if (gravityWellCooldown > 0) gravityWellCooldown--;
        if (screechCooldown > 0) screechCooldown--;

        // Xử lý báo hiệu Tiếng Hét Vô Tận
        if (screechTelegraphTicks > 0) {
            screechTelegraphTicks--;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.0D, this.getZ(), 8, 0.5D, 0.5D, 0.5D, 0.1D);
                if (screechTelegraphTicks == 0) {
                    finishEldritchScreech(sl);
                }
            }
        }

        // Xử lý Hố Đen Hút Linh Hồn
        if (!activeWells.isEmpty()) {
            Iterator<ActiveGravityWell> it = activeWells.iterator();
            while (it.hasNext()) {
                ActiveGravityWell well = it.next();
                well.ticksRemaining--;

                // Hút thực thể trong 10m về tâm
                AABB pullBox = new AABB(well.center.x - 10.0D, well.center.y - 4.0D, well.center.z - 10.0D,
                        well.center.x + 10.0D, well.center.y + 6.0D, well.center.z + 10.0D);
                List<LivingEntity> pulled = well.level.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != this && e.isAlive());
                for (LivingEntity e : pulled) {
                    Vec3 toCenter = well.center.subtract(e.position()).normalize().scale(0.18D);
                    e.setDeltaMovement(e.getDeltaMovement().add(toCenter));
                }

                if (this.tickCount % 2 == 0) {
                    well.level.sendParticles(ParticleTypes.SQUID_INK, well.center.x, well.center.y + 1.0D, well.center.z, 6, 0.4D, 0.4D, 0.4D, 0.05D);
                    well.level.sendParticles(ParticleTypes.REVERSE_PORTAL, well.center.x, well.center.y + 1.0D, well.center.z, 8, 0.5D, 0.5D, 0.5D, 0.05D);
                }

                // Kiểm tra tên / đạn ma thuật bắn vào tâm hố đen (< 2.5m)
                List<net.minecraft.world.entity.projectile.Projectile> projList = well.level.getEntitiesOfClass(
                        net.minecraft.world.entity.projectile.Projectile.class,
                        new AABB(well.center.x - 2.5D, well.center.y - 1.5D, well.center.z - 2.5D,
                                well.center.x + 2.5D, well.center.y + 3.0D, well.center.z + 2.5D));
                if (!projList.isEmpty()) {
                    well.disrupted = true;
                    well.ticksRemaining = 0;
                }

                if (well.ticksRemaining <= 0) {
                    if (well.disrupted) {
                        well.level.playSound(null, well.center.x, well.center.y, well.center.z,
                                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 3.0F, 1.4F);
                        well.level.sendParticles(ParticleTypes.POOF, well.center.x, well.center.y + 1.0D, well.center.z, 20, 0.5D, 0.5D, 0.5D, 0.1D);
                        for (Player p : well.level.getEntitiesOfClass(Player.class, pullBox)) {
                            p.displayClientMessage(Component.literal("§a§l[HỐ ĐEN BỊ PHÁ HỦY] §fĐã bắn trúng tâm hố đen triệt tiêu vụ nổ!"), true);
                        }
                    } else {
                        well.level.playSound(null, well.center.x, well.center.y, well.center.z,
                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.8F);
                        well.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, well.center.x, well.center.y + 1.0D, well.center.z, 2, 0.5D, 0.5D, 0.5D, 0);
                        AABB dmgBox = new AABB(well.center.x - 5.0D, well.center.y - 2.0D, well.center.z - 5.0D,
                                well.center.x + 5.0D, well.center.y + 4.0D, well.center.z + 5.0D);
                        for (LivingEntity v : well.level.getEntitiesOfClass(LivingEntity.class, dmgBox, e -> e != this && e.isAlive())) {
                            v.hurt(well.level.damageSources().magic(), 60.0F);
                        }
                    }
                    it.remove();
                }
            }
        }

        // Hiệu ứng hạt hư vô màu đen tím toả ra từ mặt trời đen (Throttled & chỉ sinh khi có người chơi gần 48m)
        if (this.level() instanceof ServerLevel sl && this.tickCount % 4 == 0) {
            if (sl.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 48.0D)) {
                double r = isUltimateOrComplete() ? 2.2D : 1.4D;
                double ox = (this.random.nextDouble() - 0.5D) * r * 2.0D;
                double oy = (this.random.nextDouble() - 0.5D) * r * 2.0D;
                double oz = (this.random.nextDouble() - 0.5D) * r * 2.0D;

                if (isComplete()) {
                    sl.sendParticles(ParticleTypes.END_ROD, this.getX() + ox, this.getY() + 1.2D + oy, this.getZ() + oz, 1, 0, 0, 0, 0.02D);
                } else if (isUltimate()) {
                    sl.sendParticles(ParticleTypes.SQUID_INK, this.getX() + ox, this.getY() + 1.2D + oy, this.getZ() + oz, 2, 0, 0, 0, 0.02D);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + ox, this.getY() + 1.2D + oy, this.getZ() + oz, 1, 0, 0, 0, 0.02D);
                } else {
                    sl.sendParticles(ParticleTypes.SQUID_INK, this.getX() + ox, this.getY() + 1.2D + oy, this.getZ() + oz, 1, 0, 0, 0, 0.01D);
                }

                int state = this.getEntityData().get(DATA_STATE);
                if (state == STATE_SWOOPING) {
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType) {
        if (spawnType == net.minecraft.world.entity.MobSpawnType.NATURAL || spawnType == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) {
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;
                // Chống lag tuyệt đối: Không spawn nếu trong bán kính 160 blocks đã có một Không Vong khác
                AABB checkArea = new AABB(this.blockPosition()).inflate(160.0D);
                if (!sl.getEntitiesOfClass(KuboEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }
        }
        return super.checkSpawnRules(level, spawnType);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // =========================================================================
        // 1. DẠNG HOÀN CHỈNH (FORM_COMPLETE - BẠCH NHẬT TÀ THẦN):
        // KHÁNG 100% MỌI ĐÒN TẤN CÔNG, CHỈ DUY NHẤT LINH TỬ BĂNG HOẠI MỚI GIẾT ĐƯỢC!
        // =========================================================================
        if (isComplete()) {
            boolean isDisintegration = this.getTags().contains("DisintegrationDamage");
            if (isDisintegration) {
                // Tiêu diệt tức thì bởi Linh Tử Băng Hoại
                this.playSound(SoundEvents.WITHER_DEATH, 3.0F, 1.2F);
                this.setHealth(0.0F);
                return super.hurt(source, 100000.0F);
            }
            // Kháng mọi đòn tấn công khác
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.5F, 1.8F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.0D, this.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.1D);
            }
            return false;
        }

        // =========================================================================
        // 2. DẠNG TỐI THƯỢNG (FORM_ULTIMATE):
        // - Miễn nhiễm sát thương của Milim, Long Chủng, Ác Ma, Boss, quái khác.
        // - Sát thương từ người chơi chỉ nhận 5% (5% tỷ lệ nhận 100%).
        // - Đại Thánh Tẩy / Jacob / Disintegration -> Instakill! Dragon Nova -> Full DMG.
        // =========================================================================
        if (isUltimate()) {
            Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();

            // Miễn nhiễm sát thương từ các nhân vật Milim, Velgrynd, Long chủng, Ác ma, Boss
            if (attacker != null && !(attacker instanceof Player)) {
                return false;
            }

            // Điểm yếu tuyệt đối: Cột Sáng Đại Thánh Tẩy (Instakill)
            if (this.getTags().contains("PurificationDamage")) {
                this.setHealth(0.0F);
                return super.hurt(source, 100000.0F);
            }

            // Điểm yếu: Nấc Thang Jacob (Instakill)
            if (this.getTags().contains("JacobsLadderDamage")) {
                this.setHealth(0.0F);
                return super.hurt(source, 100000.0F);
            }

            // Điểm yếu: Linh Tử Băng Hoại (Instakill)
            if (this.getTags().contains("DisintegrationDamage")) {
                this.setHealth(0.0F);
                return super.hurt(source, 100000.0F);
            }

            // Dragon Nova của Milim: Nhận đủ 100% sát thương chuẩn
            if (this.getTags().contains("DragonNovaDamage")) {
                return super.hurt(source, amount);
            }

            // Sát thương từ người chơi: 95% chỉ nhận 5% DMG, 5% cơ hội trúng điểm yếu nhận 100% DMG
            boolean isCriticalWeakspot = this.getRandom().nextFloat() < 0.05F;
            float finalDamage = isCriticalWeakspot ? amount : (amount * 0.05F);

            if (!isCriticalWeakspot) {
                this.playSound(SoundEvents.SHIELD_BLOCK, 1.2F, 1.4F);
            }

            float prevHealth = this.getHealth();
            boolean res = super.hurt(source, finalDamage);
            if (res && this.isAlive()) {
                float maxHp = this.getMaxHealth();
                float currHp = this.getHealth();
                if ((prevHealth > maxHp * 0.75F && currHp <= maxHp * 0.75F) ||
                    (prevHealth > maxHp * 0.50F && currHp <= maxHp * 0.50F) ||
                    (prevHealth > maxHp * 0.25F && currHp <= maxHp * 0.25F)) {
                    this.startEldritchScreech();
                }
            }
            return res;
        }

        float prevHealth = this.getHealth();
        boolean res = super.hurt(source, amount);
        if (res && this.isAlive()) {
            float maxHp = this.getMaxHealth();
            float currHp = this.getHealth();
            if ((prevHealth > maxHp * 0.75F && currHp <= maxHp * 0.75F) ||
                (prevHealth > maxHp * 0.50F && currHp <= maxHp * 0.50F) ||
                (prevHealth > maxHp * 0.25F && currHp <= maxHp * 0.25F)) {
                this.startEldritchScreech();
            }
        }
        return res;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living) {
            living.addTag("KuboPenetrationDamage");
            boolean hurt = super.doHurtTarget(target);
            living.removeTag("KuboPenetrationDamage");

            if (hurt) {
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 1));
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
                this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.8F, 0.6F);

                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.EXPLOSION, living.getX(), living.getY() + 1.0D, living.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
                }
            }
            return hurt;
        }
        return super.doHurtTarget(target);
    }

    // =========================================================================
    // HỆ THỐNG GÂY SÁT THƯƠNG TRỌNG THƯƠNG CỦA KHÔNG VONG
    // =========================================================================
    public void applyKuboAttackDamage(LivingEntity target, float damage) {
        if (target == null || !target.isAlive() || this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();

        target.addTag("KuboPenetrationDamage");
        target.removeEffect(MobEffects.REGENERATION);
        target.removeEffect(MobEffects.ABSORPTION);
        target.removeEffect(MobEffects.DAMAGE_RESISTANCE);

        if (target instanceof ServerPlayer sp) {
            // 1. Phá vỡ giơ khiên lập tức
            if (sp.isBlocking()) {
                sp.disableShield();
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 2.0F, 0.8F);
                sl.sendParticles(ParticleTypes.CRIT, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.1D);
            }

            // 2. Xuyên qua Giáp Thần Thoại (Divine Armor) hoặc gây sát thương ma pháp xuyên thấu
            if (DivineArmorItem.isWearingFullSet(sp)) {
                float divinePercent = isComplete() ? 0.60F : (isUltimate() ? 0.45F : 0.30F);
                float trueDmg = sp.getMaxHealth() * divinePercent;
                sp.hurt(sl.damageSources().magic(), trueDmg);
            } else {
                sp.hurt(sl.damageSources().magic(), damage);
            }

            // 3. Hiệu ứng TRỌNG THƯƠNG
            sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 1, false, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, isUltimateOrComplete() ? 2 : 1, false, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false, true));
            sp.displayClientMessage(Component.literal("§4§l[KHÔNG VONG] §cĐòn đánh Hư Vô đã bắn trọng thương ngươi! (-" + (int)damage + " HP)"), true);
        } else {
            target.hurt(sl.damageSources().magic(), damage);
        }
        target.removeTag("KuboPenetrationDamage");
    }

    // =========================================================================
    // KỸ NĂNG BẮN CỘT SÁNG HƯ VÔ (MILIM STYLE BEAM - KHÔNG PHÁ KHỐI)
    // =========================================================================
    public void fireMilimStyleBeam(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || target == null || !target.isAlive()) return;

        this.getEntityData().set(DATA_STATE, STATE_BEAMING);
        Vec3 eyePos = this.position().add(0, 1.2D, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        Vec3 dir = targetCenter.subtract(eyePos).normalize();
        double maxDist = isUltimateOrComplete() ? 75.0D : 60.0D;

        int innerColor = isComplete() ? 0xFFFFFF : (isUltimate() ? 0x330011 : 0x0A0014);
        int outerColor = isComplete() ? 0xFFE066 : (isUltimate() ? 0x990022 : 0x550088);
        float beamRadius = isUltimateOrComplete() ? 5.0F : 3.8F;

        HorizontalHolyBeamAbility.spawn3DBeamDisplay(sl, eyePos, dir, maxDist, beamRadius, innerColor, beamRadius * 1.8F, outerColor, 35);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.5F, 0.6F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.8F);

        double step = 1.0D;
        Vec3 cur = eyePos;
        float damage = isComplete() ? 320.0F : (isUltimate() ? 250.0F : 140.0F);

        Set<UUID> hitEntities = new HashSet<>();

        for (double d = 0; d < maxDist; d += step) {
            cur = cur.add(dir.scale(step));

            if (Math.round(d) % 5 == 0) {
                sl.sendParticles(ParticleTypes.SONIC_BOOM, cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(isComplete() ? ParticleTypes.END_ROD : ParticleTypes.SQUID_INK, cur.x, cur.y, cur.z, 3, 0.5D, 0.5D, 0.5D, 0.05D);
            sl.sendParticles(ParticleTypes.FLASH, cur.x, cur.y, cur.z, 1, 0.2D, 0.2D, 0.2D, 0.0D);

            AABB hitBox = new AABB(cur.x - 3.5D, cur.y - 3.5D, cur.z - 3.5D, cur.x + 3.5D, cur.y + 3.5D, cur.z + 3.5D);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e.isAlive());
            for (LivingEntity v : victims) {
                if (hitEntities.contains(v.getUUID())) continue;
                hitEntities.add(v.getUUID());

                applyKuboAttackDamage(v, damage);

                Vec3 kb = dir.scale(2.0D).add(0, 0.35D, 0);
                v.setDeltaMovement(kb);
                v.hasImpulse = true;
            }
        }

        this.beamCooldown = 100;
    }

    // =========================================================================
    // KỸ NĂNG NÂNG CẤP MỚI CỦA KHÔNG VONG (TELEGraphed & CƠ CHẾ NÉ TRÁNH)
    // =========================================================================

    public void castVoidScythe(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || target == null || !target.isAlive()) return;
        this.voidScytheCooldown = isUltimateOrComplete() ? 100 : 140;

        Vec3 look = target.getLookAngle();
        Vec3 strikePos = target.position().subtract(look.scale(3.0D)).add(0, 0.5D, 0);
        this.teleportTo(strikePos.x, strikePos.y, strikePos.z);
        this.getLookControl().setLookAt(target, 180.0F, 180.0F);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0F, 1.4F);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3.0F, 0.5F);

        for (int i = -4; i <= 4; i++) {
            double angle = Math.toRadians(target.getYRot() + i * 20.0);
            double px = target.getX() + Math.cos(angle) * 3.0D;
            double pz = target.getZ() + Math.sin(angle) * 3.0D;
            sl.sendParticles(ParticleTypes.SQUID_INK, px, target.getY() + 1.2D, pz, 3, 0.1D, 0.1D, 0.1D, 0.02D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, target.getY() + 1.2D, pz, 1, 0, 0, 0, 0.01D);
        }

        if (target instanceof Player player) {
            player.displayClientMessage(Component.literal("§5§l[TRẢM KHÍ HƯ VÔ] §dKūbō đang vung Lưỡi Hái Hư Vô! Cúi người (Shift) đỡ đòn hoặc né ngay!"), true);
        }

        boolean isCrouching = target.isCrouching();
        float baseDmg = isComplete() ? 280.0F : (isUltimate() ? 200.0F : 110.0F);
        if (isCrouching) {
            baseDmg *= 0.3F;
            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 2.5F, 1.2F);
            sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.1D);
        }

        applyKuboAttackDamage(target, baseDmg);
    }

    public void castGravityWell(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || target == null || !target.isAlive()) return;
        this.gravityWellCooldown = isUltimateOrComplete() ? 200 : 260;

        Vec3 center = target.position();
        activeWells.add(new ActiveGravityWell(sl, center));

        sl.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 3.5F, 0.7F);
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);

        if (target instanceof Player player) {
            player.displayClientMessage(Component.literal("§8§l[HỐ ĐEN HÚT LINH HỒN] §fBắn tên/đạn vào tâm hố đen để triệt tiêu hoặc thoát khỏi vùng hút!"), true);
        }
    }

    public void startEldritchScreech() {
        if (!(this.level() instanceof ServerLevel sl) || screechCooldown > 0 || screechTelegraphTicks > 0) return;
        this.screechCooldown = 400;
        this.screechTelegraphTicks = 24;

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.6F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 3.5F, 0.5F);

        for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(30.0D))) {
            p.displayClientMessage(Component.literal("§5§l[TIẾNG HÉT VÔ TẬN] §dKūbō đang gầm rú năng lượng Hư Vô! Hãy che chắn hoặc lùi xa!"), true);
        }
    }

    public void finishEldritchScreech(ServerLevel sl) {
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 0.7F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.6F);

        double radius = 18.0D;
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10.0);
            double px = this.getX() + Math.cos(angle) * 6.0D;
            double pz = this.getZ() + Math.sin(angle) * 6.0D;
            sl.sendParticles(ParticleTypes.SONIC_BOOM, px, this.getY() + 1.0D, pz, 1, 0, 0, 0, 0);
        }

        AABB screamBox = this.getBoundingBox().inflate(radius);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, screamBox, e -> e != this && e.isAlive());
        for (LivingEntity v : victims) {
            if (v instanceof ServerPlayer sp) {
                if (sp.isBlocking()) {
                    sp.disableShield();
                    sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 2.5F, 0.8F);
                }
                sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false, true));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, false, true));
                sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, true));
            }
            applyKuboAttackDamage(v, isComplete() ? 180.0F : (isUltimate() ? 120.0F : 60.0F));
            Vec3 push = v.position().subtract(this.position()).normalize().scale(1.8D).add(0, 0.4D, 0);
            v.setDeltaMovement(push);
            v.hasImpulse = true;
        }
    }

    // =========================================================================
    // KỸ NĂNG HẤP THỤ & THI TRIỂN TỪ LINH HỒN CÁC THỰC THỂ
    // =========================================================================
    public void castAbsorbedSkill(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || target == null || !target.isAlive()) return;
        int soulTypeIdx = this.getEntityData().get(DATA_ABSORBED_SOUL_TYPE);
        int specificDemon = getSpecificDemon();

        this.getEntityData().set(DATA_STATE, STATE_ABSORBED_SKILL);
        Vec3 eyePos = this.position().add(0, 1.2D, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        Vec3 dir = targetCenter.subtract(eyePos).normalize();

        // 1. Kỹ năng Thủy Tổ Ác Ma chuyên biệt
        if (specificDemon >= 0 && specificDemon < DemonType.values().length) {
            DemonType dt = DemonType.values()[specificDemon];
            switch (dt) {
                case BLEU -> { // Rain - Băng Cực Ma Trận (Đóng băng)
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.5F, 0.6F);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 3.0F, 0.8F);
                    for (int i = 0; i < 25; i++) {
                        double ox = (random.nextDouble() - 0.5D) * 2.5D;
                        double oy = random.nextDouble() * 2.5D;
                        double oz = (random.nextDouble() - 0.5D) * 2.5D;
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX() + ox, target.getY() + oy, target.getZ() + oz, 8, 0, 0, 0, 0.05D);
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX() + ox, target.getY() + oy, target.getZ() + oz, 3, 0, 0, 0, 0.02D);
                    }
                    target.setTicksFrozen(240);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 255, false, false, true));
                    applyKuboAttackDamage(target, 220.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
                case ROUGE -> { // Guy Crimson - Hỏa Ngục Thủy Tổ
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 3.5F, 0.8F);
                    for (int i = 0; i < 30; i++) {
                        double ox = (random.nextDouble() - 0.5D) * 3.0D;
                        double oz = (random.nextDouble() - 0.5D) * 3.0D;
                        sl.sendParticles(ParticleTypes.FLAME, target.getX() + ox, target.getY() + 0.5D, target.getZ() + oz, 10, 0, 0.4D, 0, 0.1D);
                        sl.sendParticles(ParticleTypes.LAVA, target.getX() + ox, target.getY() + 0.5D, target.getZ() + oz, 5, 0, 0, 0, 0);
                    }
                    target.setRemainingFireTicks(240);
                    applyKuboAttackDamage(target, 350.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
                case JAUNE -> { // Carrera - Súng Ma Đạn Hạt Nhân
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.5F, 1.2F);
                    for (int b = 0; b < 5; b++) {
                        Vec3 spread = dir.add((random.nextDouble() - 0.5D) * 0.25D, (random.nextDouble() - 0.5D) * 0.25D, (random.nextDouble() - 0.5D) * 0.25D).normalize();
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, eyePos.x + spread.x * 2.0D, eyePos.y + spread.y * 2.0D, eyePos.z + spread.z * 2.0D, 1, 0, 0, 0, 0);
                    }
                    applyKuboAttackDamage(target, 320.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
                case NOIR -> { // Diablo - Móng Vuốt Tuyệt Vọng
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3.0F, 0.5F);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0D, target.getZ(), 6, 0.4D, 0.4D, 0.4D, 0);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3));
                    applyKuboAttackDamage(target, 280.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
                case BLANC -> { // Testarossa - Bạch Viêm Diệt Tuyệt
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 1.2F);
                    sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.05D);
                    applyKuboAttackDamage(target, 260.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
                case VIOLET -> { // Ultima - Tử Độc Khởi Nguyên
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 3.0F, 0.7F);
                    sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0D, target.getZ(), 25, 0.6D, 0.6D, 0.6D, 0.05D);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 3));
                    applyKuboAttackDamage(target, 200.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
                case VERT -> { // Misery - Bão Lốc Lục Bảo
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 3.0F, 1.0F);
                    target.setDeltaMovement(0, 1.4D, 0);
                    target.hasImpulse = true;
                    applyKuboAttackDamage(target, 190.0F);
                    this.absorbedSkillCooldown = 120;
                    return;
                }
            }
        }

        // 2. Kỹ năng theo SoulType cơ bản
        if (soulTypeIdx >= 0 && soulTypeIdx < SoulType.values().length) {
            SoulType soulType = SoulType.values()[soulTypeIdx];
            switch (soulType) {
                case MILIM -> {
                    HorizontalHolyBeamAbility.spawn3DBeamDisplay(sl, eyePos, dir, 70.0D, 4.5F, 0x220022, 8.5F, 0x880055, 40);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.5F, 0.8F);
                    AABB beamBox = this.getBoundingBox().expandTowards(dir.scale(60.0D)).inflate(4.0D);
                    for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, beamBox, e -> e != this && e.isAlive())) {
                        applyKuboAttackDamage(v, 1260.0F);
                    }
                }
                case VELGRYND -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 3.0F, 1.2F);
                    target.setRemainingFireTicks(300);
                    applyKuboAttackDamage(target, 350.0F);
                }
                case VELZARD -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.5F, 0.6F);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 3.0F, 1.4F);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0D, target.getZ(), 40, 1.5D, 1.5D, 1.5D, 0.1D);
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + 80);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                    applyKuboAttackDamage(target, 420.0F);
                }
                case PRIMORDIAL_DEMON -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 3.0F, 0.7F);
                    applyKuboAttackDamage(target, 280.0F);
                }
                case WARDEN -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0D, target.getZ(), 1, 0, 0, 0, 0);
                    applyKuboAttackDamage(target, 90.0F);
                }
                case ENDER_DRAGON -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.5F, 0.9F);
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + 1.0D, target.getZ(), 40, 1.2D, 1.2D, 1.2D, 0.05D);
                    applyKuboAttackDamage(target, 120.0F);
                }
                case WITHER -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.5F, 1.0F);
                    WitherSkull skull = new WitherSkull(sl, this, dir);
                    skull.setPos(eyePos.x, eyePos.y, eyePos.z);
                    sl.addFreshEntity(skull);
                }
                case MOB -> {
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.5F);
                    applyKuboAttackDamage(target, 45.0F);
                }
            }
        }

        this.absorbedSkillCooldown = 120;
    }

    // =========================================================================
    // TIẾN HÓA & HẤP THỤ LINH HỒN
    // =========================================================================

    public void absorbMobSoul() {
        int count = getMobSoulsCount() + 1;
        this.getEntityData().set(DATA_MOB_SOULS_COUNT, count);
        recalculateAttributes();
        this.heal(8.0F);

        this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5F, 0.8F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 15, 0.4D, 0.4D, 0.4D, 0.05D);
        }
    }

    public void absorbDemonSoul(DemonType demonType) {
        int count = getDemonSoulsCount() + 1;
        this.getEntityData().set(DATA_DEMON_SOULS_COUNT, count);
        if (demonType != null) {
            this.getEntityData().set(DATA_SPECIFIC_DEMON, demonType.ordinal());
            this.getEntityData().set(DATA_ABSORBED_SKILL_NAME, demonType.getSkillNameVi());
            this.getEntityData().set(DATA_ABSORBED_SOUL_NAME, demonType.getTitleVi());
        }
        this.getEntityData().set(DATA_ABSORBED_SOUL_TYPE, SoulType.PRIMORDIAL_DEMON.ordinal());

        recalculateAttributes();
        this.heal(50.0F);

        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.7F);
        this.playSound(SoundEvents.WARDEN_ROAR, 2.0F, 1.2F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.2D, this.getZ(), 30, 0.6D, 0.6D, 0.6D, 0.05D);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.04D);
        }
    }

    public void evolveToUltimate(SoulType catalystSoul) {
        this.getEntityData().set(DATA_FORM, FORM_ULTIMATE);
        this.getEntityData().set(DATA_ABSORBED_SOUL_TYPE, catalystSoul.ordinal());
        this.getEntityData().set(DATA_ABSORBED_SOUL_NAME, catalystSoul.getEntityName());
        this.getEntityData().set(DATA_ABSORBED_SKILL_NAME, catalystSoul.getSkillName());

        recalculateAttributes();
        this.setHealth(this.getMaxHealth()); // Hồi đầy máu khi tiến hóa

        this.playSound(SoundEvents.WITHER_SPAWN, 3.5F, 0.5F);
        this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0F, 0.8F);
        this.playSound(SoundEvents.WARDEN_ROAR, 3.5F, 0.6F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5D, this.getZ(), 4, 0.5D, 0.5D, 0.5D, 0);
            sl.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.5D, this.getZ(), 60, 1.5D, 1.5D, 1.5D, 0.1D);
            sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.5D, this.getZ(), 3, 0.2D, 0.2D, 0.2D, 0);
        }
    }

    public void evolveToComplete() {
        this.getEntityData().set(DATA_FORM, FORM_COMPLETE);
        recalculateAttributes();
        this.setHealth(this.getMaxHealth());

        this.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 3.0F, 1.0F);
        this.playSound(SoundEvents.BEACON_ACTIVATE, 4.0F, 0.5F);
        this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 1.0F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.5D, this.getZ(), 5, 0.2D, 0.2D, 0.2D, 0);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5D, this.getZ(), 100, 2.0D, 2.0D, 2.0D, 0.3D);
            sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5D, this.getZ(), 60, 1.8D, 1.8D, 1.8D, 0.1D);
        }
    }

    public void absorbSoul(SoulType soulType) {
        if (soulType == SoulType.MILIM || soulType == SoulType.VELGRYND || soulType == SoulType.VELZARD) {
            evolveToUltimate(soulType);
            return;
        }
        if (soulType == SoulType.PRIMORDIAL_DEMON) {
            DemonType randomDemon = DemonType.values()[this.random.nextInt(DemonType.values().length)];
            absorbDemonSoul(randomDemon);
            return;
        }
        if (soulType == SoulType.MOB) {
            absorbMobSoul();
            return;
        }

        float bonusHp = soulType.getBonusHp70();
        float bonusArmor = soulType.getBonusArmor70();

        this.getEntityData().set(DATA_ABSORBED_SOUL_TYPE, soulType.ordinal());
        this.getEntityData().set(DATA_ABSORBED_SOUL_NAME, soulType.getEntityName());
        this.getEntityData().set(DATA_ABSORBED_SKILL_NAME, soulType.getSkillName());
        this.getEntityData().set(DATA_BONUS_HP, bonusHp);
        this.getEntityData().set(DATA_BONUS_ARMOR, bonusArmor);

        recalculateAttributes();
        this.heal(bonusHp);

        this.playSound(SoundEvents.WARDEN_ROAR, 2.5F, 1.4F);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.8F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.2D, this.getZ(), 35, 0.8D, 0.8D, 0.8D, 0.08D);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.2D, this.getZ(), 25, 0.6D, 0.6D, 0.6D, 0.05D);
            sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.04D);
        }
    }

    public boolean hasAbsorbedSoul() {
        return this.getEntityData().get(DATA_ABSORBED_SOUL_TYPE) >= 0 || getSpecificDemon() >= 0;
    }

    public String getAbsorbedEntityName() {
        return this.getEntityData().get(DATA_ABSORBED_SOUL_NAME);
    }

    public String getAbsorbedSkillName() {
        return this.getEntityData().get(DATA_ABSORBED_SKILL_NAME);
    }

    public float getBonusHp() {
        return this.getEntityData().get(DATA_BONUS_HP);
    }

    public float getBonusArmor() {
        return this.getEntityData().get(DATA_BONUS_ARMOR);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KuboForm", getForm());
        tag.putInt("MobSoulsCount", getMobSoulsCount());
        tag.putInt("DemonSoulsCount", getDemonSoulsCount());
        tag.putInt("SpecificDemon", getSpecificDemon());
        tag.putInt("AbsorbedSoulType", this.getEntityData().get(DATA_ABSORBED_SOUL_TYPE));
        tag.putString("AbsorbedSoulName", this.getEntityData().get(DATA_ABSORBED_SOUL_NAME));
        tag.putString("AbsorbedSkillName", this.getEntityData().get(DATA_ABSORBED_SKILL_NAME));
        tag.putFloat("BonusHp", this.getEntityData().get(DATA_BONUS_HP));
        tag.putFloat("BonusArmor", this.getEntityData().get(DATA_BONUS_ARMOR));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("KuboForm")) {
            this.getEntityData().set(DATA_FORM, tag.getInt("KuboForm"));
        } else if (tag.getBoolean("IsComplete")) {
            this.getEntityData().set(DATA_FORM, FORM_COMPLETE);
        } else if (tag.getBoolean("IsUltimate")) {
            this.getEntityData().set(DATA_FORM, FORM_ULTIMATE);
        }

        if (tag.contains("MobSoulsCount")) {
            this.getEntityData().set(DATA_MOB_SOULS_COUNT, tag.getInt("MobSoulsCount"));
        }
        if (tag.contains("DemonSoulsCount")) {
            this.getEntityData().set(DATA_DEMON_SOULS_COUNT, tag.getInt("DemonSoulsCount"));
        }
        if (tag.contains("SpecificDemon")) {
            this.getEntityData().set(DATA_SPECIFIC_DEMON, tag.getInt("SpecificDemon"));
        }
        if (tag.contains("AbsorbedSoulType")) {
            this.getEntityData().set(DATA_ABSORBED_SOUL_TYPE, tag.getInt("AbsorbedSoulType"));
        }
        if (tag.contains("AbsorbedSoulName")) {
            this.getEntityData().set(DATA_ABSORBED_SOUL_NAME, tag.getString("AbsorbedSoulName"));
        }
        if (tag.contains("AbsorbedSkillName")) {
            this.getEntityData().set(DATA_ABSORBED_SKILL_NAME, tag.getString("AbsorbedSkillName"));
        }
        if (tag.contains("BonusHp")) {
            this.getEntityData().set(DATA_BONUS_HP, tag.getFloat("BonusHp"));
        }
        if (tag.contains("BonusArmor")) {
            this.getEntityData().set(DATA_BONUS_ARMOR, tag.getFloat("BonusArmor"));
        }

        recalculateAttributes();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    public static boolean isStrongTarget(LivingEntity target) {
        if (target instanceof Player player) {
            return EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        }
        if (target instanceof com.minhphuc.weapons.entity.tensura.MilimEntity ||
            target instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity ||
            target instanceof com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity ||
            target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon ||
            target instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
            target instanceof net.minecraft.world.entity.monster.warden.Warden) {
            return true;
        }
        return target.getMaxHealth() >= 80.0F;
    }

    // =========================================================================
    // AI GOAL 0: NHẬN BIẾT & NÉ TRÁNH LINH TỬ BĂNG HOẠI VÀ NẤC THANG JACOB
    // =========================================================================
    static class KuboDodgeSanctuaryGoal extends Goal {
        private final KuboEntity kubo;
        private Vec3 threatCenter;

        public KuboDodgeSanctuaryGoal(KuboEntity kubo) {
            this.kubo = kubo;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(kubo.level() instanceof ServerLevel sl)) return false;
            // Kiểm tra mỗi 6 ticks để phản ứng cực nhanh
            if (kubo.tickCount % 6 != 0) return false;

            Vec3 pos = kubo.position();
            // 1. Dò tìm Ma Pháp Trận Linh Tử Băng Hoại trong phạm vi 30m
            if (SanctuaryDisintegrationAbility.isNearActiveSanctuary(sl, pos, 30.0D)) {
                this.threatCenter = SanctuaryDisintegrationAbility.getClosestSanctuaryCenter(sl, pos, 30.0D);
                return this.threatCenter != null;
            }

            // 2. Dò tìm Nấc Thang Jacob trong phạm vi 28m
            if (JacobsLadderAbility.isNearActiveLadder(sl, pos, 28.0D)) {
                this.threatCenter = JacobsLadderAbility.getClosestLadderCenter(sl, pos, 28.0D);
                return this.threatCenter != null;
            }

            return false;
        }

        @Override
        public void start() {
            kubo.getEntityData().set(DATA_STATE, STATE_FLEEING);
            kubo.playSound(SoundEvents.GHAST_SCREAM, 2.5F, 1.2F);
            kubo.playSound(SoundEvents.PHANTOM_BITE, 2.0F, 0.7F);
        }

        @Override
        public void tick() {
            if (threatCenter != null) {
                Vec3 fleeDir = kubo.position().subtract(threatCenter).normalize();
                if (fleeDir.lengthSqr() < 0.01D) {
                    fleeDir = new Vec3(1, 0, 0);
                }
                // Bay ngược hướng với tốc độ x2.2 vút lên không trung để thoát khỏi vùng bán kính 30m
                Vec3 targetPos = kubo.position().add(fleeDir.x * 35.0D, 15.0D, fleeDir.z * 35.0D);
                kubo.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 2.2D);
            }
        }
    }

    // =========================================================================
    // AI GOAL 1: TÌM & NUỐT LINH HỒN / HẠT GIỐNG MA VƯƠNG (CHỐNG LAG TỐI ĐA)
    // =========================================================================
    static class KuboSeekSoulGoal extends Goal {
        private final KuboEntity kubo;
        private ItemEntity targetItem;
        private int scanCooldown = 0;

        public KuboSeekSoulGoal(KuboEntity kubo) {
            this.kubo = kubo;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Chỉ quét tìm item mỗi 20 ticks (1s) để đảm bảo KHÔNG LAG server
            if (scanCooldown > 0) {
                scanCooldown--;
                return targetItem != null && targetItem.isAlive();
            }
            scanCooldown = 20;

            AABB searchBox = kubo.getBoundingBox().inflate(36.0D);
            List<ItemEntity> items = kubo.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                    it -> it.isAlive() && (it.getItem().getItem() instanceof EntitySoulItem || it.getItem().is(ModItems.DEMON_LORD_SEED.get())));

            if (!items.isEmpty()) {
                ItemEntity closest = null;
                double closestDist = Double.MAX_VALUE;
                for (ItemEntity it : items) {
                    // Ưu tiên Hạt Giống Ma Vương trước
                    if (it.getItem().is(ModItems.DEMON_LORD_SEED.get())) {
                        closest = it;
                        break;
                    }
                    double d = kubo.distanceToSqr(it);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = it;
                    }
                }
                this.targetItem = closest;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return targetItem != null && targetItem.isAlive();
        }

        @Override
        public void tick() {
            if (targetItem == null || !targetItem.isAlive()) return;

            kubo.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);
            kubo.getMoveControl().setWantedPosition(targetItem.getX(), targetItem.getY() + 0.6D, targetItem.getZ(), 1.5D);

            double distSq = kubo.distanceToSqr(targetItem);
            if (distSq <= 6.0D) {
                if (targetItem.getItem().is(ModItems.DEMON_LORD_SEED.get())) {
                    kubo.evolveToComplete();
                    targetItem.getItem().shrink(1);
                    if (targetItem.getItem().isEmpty()) {
                        targetItem.discard();
                    }
                } else if (targetItem.getItem().getItem() instanceof EntitySoulItem soulItem) {
                    kubo.absorbSoul(soulItem.getSoulType());
                    targetItem.getItem().shrink(1);
                    if (targetItem.getItem().isEmpty()) {
                        targetItem.discard();
                    }
                }
                targetItem = null;
            }
        }
    }

    // =========================================================================
    // AI GOAL 2: BỎ CHẠY KHI MÁU DƯỚI 30% HOẶC BỊ ĐÁNH QUÁ ĐAU
    // =========================================================================
    static class KuboFleeGoal extends Goal {
        private final KuboEntity kubo;
        private Vec3 fleeTargetPos;

        public KuboFleeGoal(KuboEntity kubo) {
            this.kubo = kubo;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (kubo.getHealth() < kubo.getMaxHealth() * 0.3F) {
                LivingEntity attacker = kubo.getLastHurtByMob();
                if (attacker == null) attacker = kubo.getTarget();

                Vec3 fromPos = (attacker != null) ? attacker.position() : kubo.position().add(0, -1, 0);
                Vec3 fleeDir = kubo.position().subtract(fromPos).normalize();
                this.fleeTargetPos = kubo.position().add(fleeDir.x * 28.0D, 20.0D, fleeDir.z * 28.0D);
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            kubo.getEntityData().set(DATA_STATE, STATE_FLEEING);
            kubo.playSound(SoundEvents.GHAST_WARN, 2.0F, 0.6F);
        }

        @Override
        public void tick() {
            if (fleeTargetPos != null) {
                kubo.getMoveControl().setWantedPosition(fleeTargetPos.x, fleeTargetPos.y, fleeTargetPos.z, 1.9D);
            }
        }
    }

    // =========================================================================
    // AI GOAL 3: TÁC CHIẾN THÔNG MINH
    // =========================================================================
    static class KuboTacticalCombatGoal extends Goal {
        private final KuboEntity kubo;
        private int swoopCooldown = 0;
        private boolean isDiving = false;

        public KuboTacticalCombatGoal(KuboEntity kubo) {
            this.kubo = kubo;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = kubo.getTarget();
            return target != null && target.isAlive() && kubo.getHealth() >= kubo.getMaxHealth() * 0.3F;
        }

        @Override
        public void tick() {
            LivingEntity target = kubo.getTarget();
            if (target == null || !target.isAlive()) return;

            kubo.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = kubo.distanceToSqr(target);
            boolean strong = isStrongTarget(target);
            boolean lowHp = kubo.getHealth() < kubo.getMaxHealth() * 0.5F;
            boolean isPlayer = target instanceof Player;

            // TRƯỜNG HỢP 1: NGƯỜI CHƠI, THỰC THỂ MẠNH, MÁU DƯỚI 50% HOẶC Ở DẠNG TỐI THƯỢNG/HOÀN CHỈNH -> TÁC CHIẾN TẦM XA BẮN BEAM / SKILL
            if (isPlayer || strong || lowHp || kubo.isUltimateOrComplete()) {
                Vec3 targetPos = target.position();
                double hoverX = targetPos.x + (kubo.getX() > targetPos.x ? 12.0D : -12.0D);
                double hoverY = targetPos.y + 10.0D;
                double hoverZ = targetPos.z + (kubo.getZ() > targetPos.z ? 12.0D : -12.0D);
                kubo.getMoveControl().setWantedPosition(hoverX, hoverY, hoverZ, 1.25D);

                if (kubo.voidScytheCooldown <= 0 && distSq <= 20.0D * 20.0D) {
                    kubo.castVoidScythe(target);
                } else if (kubo.gravityWellCooldown <= 0 && distSq <= 35.0D * 35.0D) {
                    kubo.castGravityWell(target);
                } else if (kubo.hasAbsorbedSoul() && kubo.absorbedSkillCooldown <= 0 && distSq <= 55.0D * 55.0D) {
                    kubo.castAbsorbedSkill(target);
                } else if (kubo.beamCooldown <= 0 && distSq <= 65.0D * 65.0D) {
                    kubo.fireMilimStyleBeam(target);
                }
                return;
            }

            // TRƯỜNG HỢP 2: QUÁI THƯỜNG (MÁU >= 50%) -> BAY TRÊN CAO BẮN BEAM HOẶC XÀ XUỐNG ĐÁNH
            if (swoopCooldown > 0) swoopCooldown--;

            if (!isDiving) {
                Vec3 targetPos = target.position();
                kubo.getMoveControl().setWantedPosition(targetPos.x, targetPos.y + 10.0D, targetPos.z, 1.1D);

                if (kubo.voidScytheCooldown <= 0 && distSq <= 16.0D * 16.0D) {
                    kubo.castVoidScythe(target);
                } else if (kubo.beamCooldown <= 0 && distSq <= 50.0D * 50.0D) {
                    kubo.fireMilimStyleBeam(target);
                }

                if (swoopCooldown <= 0 && distSq <= 25.0D * 25.0D) {
                    isDiving = true;
                    kubo.getEntityData().set(DATA_STATE, STATE_SWOOPING);
                    kubo.playSound(SoundEvents.PHANTOM_SWOOP, 2.0F, 0.7F);
                }
            } else {
                kubo.getMoveControl().setWantedPosition(target.getX(), target.getY() + 0.5D, target.getZ(), 1.7D);

                if (distSq <= 10.0D) {
                    kubo.doHurtTarget(target);
                    isDiving = false;
                    swoopCooldown = 50;
                    kubo.getEntityData().set(DATA_STATE, STATE_IDLE);
                    kubo.setDeltaMovement(0, 0.8D, 0);
                } else if (kubo.getY() <= target.getY() - 1.0D) {
                    isDiving = false;
                    swoopCooldown = 30;
                    kubo.getEntityData().set(DATA_STATE, STATE_IDLE);
                }
            }
        }
    }

    // =========================================================================
    // AI GOAL 4: TUẦN TRA TRÊN CAO TỰ NHIÊN
    // =========================================================================
    static class KuboHighCruiseGoal extends Goal {
        private final KuboEntity kubo;

        public KuboHighCruiseGoal(KuboEntity kubo) {
            this.kubo = kubo;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !kubo.getMoveControl().hasWanted() && kubo.getRandom().nextInt(10) == 0 && kubo.getTarget() == null;
        }

        @Override
        public void start() {
            Vec3 pos = kubo.position();
            double tx = pos.x + (kubo.getRandom().nextDouble() - 0.5D) * 24.0D;
            BlockPos ground = BlockPos.containing(pos.x, pos.y, pos.z);
            while (kubo.level().isEmptyBlock(ground) && ground.getY() > kubo.level().getMinBuildHeight() + 2) {
                ground = ground.below();
            }
            double targetY = ground.getY() + 14.0D + (kubo.getRandom().nextDouble() * 6.0D);
            double tz = pos.z + (kubo.getRandom().nextDouble() - 0.5D) * 24.0D;

            kubo.getMoveControl().setWantedPosition(tx, targetY, tz, 0.8D);
            kubo.getEntityData().set(DATA_STATE, STATE_IDLE);
        }
    }
}
