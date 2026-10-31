package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.content.darkgathering.WardingEarplugsItem;
import com.minhphuc.weapons.init.ModItems;
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
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TÀ KINH ĐẠI TĂNG (JANKYŌBUN DAISŌJŌ - 邪経文大僧正)
 * TỐT NGHIỆP SINH CẤP S (DARK GATHERING):
 * - Nhục Thân Bồ Tát (Sokushinbutsu) đọa lạc thành tà linh cuồng sát.
 * - Chỉ số: 1200 HP, 20 Armor, 10 Toughness, 1.0 Knockback Resistance.
 * - Kỹ Năng Độc Bản:
 *   1. Tà Kinh Tuyệt Mệnh (The Death Sutra): Tụng kinh bán kính 20m. Ai KHÔNG ĐEO BÔNG BỊT TAI PHONG ẤN sẽ tử vong sau 10s!
 *   2. Khắc Kinh Văn Phong Ma: Bắn 4 dải kinh phù khóa kỹ năng và cấm hồi máu trong 6s.
 *   3. Hoàng Tuyền Hóa Độ (Avici Domain): Kích hoạt khi HP <= 50%, tỏa kết giới tro tàn thiêu đốt sinh lực.
 *   4. Nhục Thân Kim Cang: Kháng lửa, rơi, đuối nước; giảm 50% sát thương đạn; điểm yếu trước đòn tay không của Cứu Thế Quyền Hiện.
 * - Phong ấn & Triệu hồi: Sử dụng Hình Nhân Gỗ Bồ Đề (Bodhi Effigy).
 * - TỐI ƯU HÓA ZERO-LAG: Quét định kỳ 20 ticks, tính khoảng cách O(1), không spam entity đạn.
 */
public class TaKinhDaiTangEntity extends Monster {

    private static final EntityDataAccessor<Boolean> CHANTING =
            SynchedEntityData.defineId(TaKinhDaiTangEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(TaKinhDaiTangEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Boolean> AVICI_ACTIVE =
            SynchedEntityData.defineId(TaKinhDaiTangEntity.class, EntityDataSerializers.BOOLEAN);

    private int stunTicks = 0;
    private int actionTicks = 0;
    private int sutraInscriptionCooldown = 120;
    private boolean aviciTriggered = false;

    // Quản lý thời gian tiếp xúc Tà Kinh của từng nạn nhân (UUID -> số ticks)
    private final Map<UUID, Integer> sutraExposureMap = new HashMap<>();

    // Cơ chế Phong Ấn & Triệu Hồi (Bodhi Effigy)
    private UUID ownerUuid = null;
    private int summonTicks = -1; // -1: Tự nhiên, > 0: Triệu hồi kiểm soát (60s), 0: Phản phệ bạo tẩu
    private boolean isRampaging = false;

    public TaKinhDaiTangEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHANTING, false);
        builder.define(ACTION_STATE, (byte) 0);
        builder.define(AVICI_ACTIVE, false);
    }

    public boolean isChanting() {
        return this.entityData.get(CHANTING);
    }

    public void setChanting(boolean chanting) {
        this.entityData.set(CHANTING, chanting);
    }

    public byte getActionState() {
        return this.entityData.get(ACTION_STATE);
    }

    public void setActionState(byte state, int durationTicks) {
        this.entityData.set(ACTION_STATE, state);
        this.actionTicks = durationTicks;
    }

    public boolean isAviciActive() {
        return this.entityData.get(AVICI_ACTIVE);
    }

    public void stun(int ticks) {
        this.stunTicks = Math.max(this.stunTicks, ticks);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public boolean isStunned() {
        return this.stunTicks > 0;
    }

    public void initSummon(UUID ownerUuid, int durationTicks) {
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

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, LivingEntity.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Tự động tìm diệt mọi sinh vật sống xung quanh
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                e -> {
                    if (e == this || !e.isAlive() || e instanceof TaKinhDaiTangEntity) return false;
                    if (this.ownerUuid != null && !this.isRampaging) {
                        if (e.getUUID().equals(this.ownerUuid)) return false;
                        if (e instanceof net.minecraft.world.entity.TamableAnimal ta && this.ownerUuid.equals(ta.getOwnerUUID())) return false;
                    }
                    return true;
                }
        ));
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            // Tỷ lệ xuất hiện tự nhiên hiếm tương đương Cứu Thế Quyền Hiện (3%) - Kiểm tra trước để tránh load chunks/entities
            if (this.random.nextFloat() > 0.03F) {
                return false;
            }

            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD) return false;

                AABB checkArea = new AABB(this.blockPosition()).inflate(64.0D);
                if (!sl.getEntitiesOfClass(TaKinhDaiTangEntity.class, checkArea).isEmpty()) {
                    return false;
                }
            }
        }
        return super.checkSpawnRules(level, spawnType);
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

        if (this.sutraInscriptionCooldown > 0) this.sutraInscriptionCooldown--;

        if (this.actionTicks > 0) {
            this.actionTicks--;
            if (this.actionTicks <= 0) {
                this.entityData.set(ACTION_STATE, (byte) 0);
            }
        }

        super.aiStep();

        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();

        // 0. Quản lý thời gian kiểm soát khi triệu hồi (60 giây)
        if (this.summonTicks > 0) {
            this.summonTicks--;

            if (this.tickCount % 20 == 0 && this.ownerUuid != null) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(this.ownerUuid);
                if (owner != null && owner.distanceToSqr(this) <= 64.0D * 64.0D) {
                    int secs = this.summonTicks / 20;
                    owner.displayClientMessage(
                            Component.literal("§6§l[KIỂM SOÁT TÀ KINH ĐẠI TĂNG]: §e" + secs + "s §7| Chuột phải Hình Nhân Gỗ Rỗng vào ngài để thu hồi!"),
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

        // Hỗ trợ chiến đấu cho chủ nhân
        if (this.ownerUuid != null && !this.isRampaging) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (owner != null && (this.getTarget() == null || !this.getTarget().isAlive())) {
                LivingEntity revenge = owner.getLastHurtByMob();
                if (revenge != null && revenge.isAlive() && revenge != this && !(revenge instanceof TaKinhDaiTangEntity)) {
                    this.setTarget(revenge);
                } else {
                    LivingEntity attacking = owner.getLastHurtMob();
                    if (attacking != null && attacking.isAlive() && attacking != this && !(attacking instanceof TaKinhDaiTangEntity)) {
                        this.setTarget(attacking);
                    }
                }
            }
        }

        LivingEntity target = this.getTarget();
        boolean hasCombatTarget = (target != null && target.isAlive());

        // 1. KỸ NĂNG 1: TÀ KINH TUYỆT MỆNH (THE DEATH SUTRA CHANTING)
        // Chạy định kỳ mỗi 20 ticks (1s) để tối ưu hiệu năng Zero-Lag
        if (hasCombatTarget && !this.isStunned()) {
            this.setChanting(true);

            if (this.tickCount % 20 == 0) {
                executeDeathSutraAura(sl);
            }
        } else {
            this.setChanting(false);
            if (!this.sutraExposureMap.isEmpty()) {
                this.sutraExposureMap.clear();
            }
        }

        // 2. KỸ NĂNG 2: KHẮC KINH VĂN PHONG MA (Sutra Inscription)
        if (hasCombatTarget && !this.isStunned() && this.sutraInscriptionCooldown <= 0) {
            double distSq = this.distanceToSqr(target);
            if (distSq >= 4.0D * 4.0D && distSq <= 16.0D * 16.0D && this.hasLineOfSight(target)) {
                executeSutraInscription(sl, target);
                this.sutraInscriptionCooldown = 160; // 8s hồi chiêu
            }
        }

        // 3. KỸ NĂNG 3: HOÀNG TUYỀN HÓA ĐỘ (Avici Domain)
        if (!this.aviciTriggered && this.getHealth() <= this.getMaxHealth() * 0.50F && !this.isStunned()) {
            executeAviciDomain(sl);
            this.aviciTriggered = true;
        }
    }

    /**
     * Kỹ năng 1: Vùng Tụng Kinh Tà Ác Đoạt Mệnh (Bán kính 20 blocks)
     */
    private void executeDeathSutraAura(ServerLevel sl) {
        boolean hasNearbyPlayer = sl.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 48.0D);

        // Chỉ phát âm thanh và hiệu ứng hạt khi có người chơi trong phạm vi để tiết kiệm tài nguyên
        if (hasNearbyPlayer) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.8F, 0.45F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5F, 0.75F);

            // Hiệu ứng hạt chữ Phạn / linh hồn bay xung quanh Đại Tăng
            for (int i = 0; i < 4; i++) {
                double angle = this.random.nextDouble() * 2 * Math.PI;
                double r = 1.0D + this.random.nextDouble() * 2.5D;
                double px = this.getX() + r * Math.cos(angle);
                double pz = this.getZ() + r * Math.sin(angle);
                sl.sendParticles(ParticleTypes.ENCHANT, px, this.getY() + 1.2D, pz, 1, 0, 0.05D, 0, 0.05D);
                sl.sendParticles(ParticleTypes.SOUL, px, this.getY() + 0.8D, pz, 1, 0, 0.02D, 0, 0.01D);
            }
        }

        AABB auraArea = this.getBoundingBox().inflate(20.0D, 8.0D, 20.0D);
        for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, auraArea, EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
            if (victim == this || !victim.isAlive() || victim instanceof TaKinhDaiTangEntity || victim instanceof CuuTheSoulEntity) {
                continue;
            }

            // KIỂM TRA ĐỐI KHÁNG NGUYÊN TÁC: BÔNG BỊT TAI PHONG ẤN
            if (WardingEarplugsItem.isProtected(victim)) {
                // Nạn nhân đã bịt tai: Miễn nhiễm 100%!
                if (victim instanceof ServerPlayer sp && this.tickCount % 60 == 0) {
                    sp.displayClientMessage(
                            Component.literal("§a§l[BÔNG BỊT TAI PHONG ẤN] §fĐã ngăn chặn tiếng tụng kinh tà ác của Đại Tăng! ✨"),
                            true
                    );
                }
                this.sutraExposureMap.remove(victim.getUUID());
                continue;
            }

            // NẠN NHÂN NGHE THẤY TIẾNG KINH: Bị rút cạn sinh lực và tăng dần sát thương
            int exposure = this.sutraExposureMap.getOrDefault(victim.getUUID(), 0) + 20;
            this.sutraExposureMap.put(victim.getUUID(), exposure);

            // Sát thương tăng tiến theo thời gian tiếp xúc
            float baseDmg = 4.0F + (exposure / 40.0F) * 3.5F;

            // Warden có thính giác siêu nhạy -> chịu gấp 2.5 lần sát thương!
            if (victim instanceof Warden) {
                baseDmg *= 2.5F;
            }

            victim.hurt(this.damageSources().magic(), baseDmg);

            // Hiệu ứng debuff tâm trí đọa lạc
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, false));

            if (victim instanceof ServerPlayer sp) {
                int secs = exposure / 20;
                int remaining = Math.max(0, 10 - secs);
                sp.displayClientMessage(
                        Component.literal("§4§l⚠️ BẠN ĐANG NGHE TÀ KINH! §cÁn tử Vãng Sanh sau: §e§l" + remaining + "s §7(Cần đeo Bông Bịt Tai Phong Ấn!)"),
                        true
                );
                sp.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2F, 1.8F);
            }

            // Sau 10 giây (200 ticks) nghe kinh liên tục -> BỊ ÉP VÃNG SANH (TỬ VONG NGAY LẬP TỨC!)
            if (exposure >= 200) {
                this.sutraExposureMap.remove(victim.getUUID());

                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0F, 0.6F);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0, 0, 0, 0);

                if (victim instanceof ServerPlayer sp) {
                    sl.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§4§l[ĐỘ HÓA] §f" + sp.getName().getString() + " §cđã bị Tà Kinh Đại Tăng độ hóa tử vong do nghe trọn lời Tà Kinh!"),
                            false
                    );
                }
                victim.hurt(sl.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }
    }

    /**
     * Kỹ năng 2: Khắc Kinh Văn Phong Ma (Bắn 4 dải kinh phù phong ấn kỹ năng & cấm hồi máu)
     */
    private void executeSutraInscription(ServerLevel sl, LivingEntity target) {
        this.setActionState((byte) 1, 18);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.6F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_SAND_BREAK, SoundSource.HOSTILE, 2.0F, 1.4F);

        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 dir = end.subtract(start).normalize();
        double maxDist = Math.min(16.0D, start.distanceTo(end) + 1.5D);

        // Raycast bước nhảy 1 block bắn dải kinh phù
        for (double d = 1.0D; d <= maxDist; d += 1.0D) {
            Vec3 pos = start.add(dir.scale(d));
            sl.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.05D);
            sl.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }

        // Tác động trực tiếp lên mục tiêu
        target.hurt(this.damageSources().mobAttack(this), 45.0F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false, true));

        // Xóa sạch hiệu ứng hồi máu
        target.removeEffect(MobEffects.REGENERATION);

        if (target instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§4§l[CẢNH BÁO] §cKinh Văn Phong Ma của Tà Kinh Đại Tăng đã khắc vào da thịt bạn! Kỹ năng và hồi máu bị phong tỏa trong 6s!"));
        }
    }

    /**
     * Kỹ năng 3: Hoàng Tuyền Hóa Độ (Avici Domain - Kết giới tro tàn)
     */
    private void executeAviciDomain(ServerLevel sl) {
        this.entityData.set(AVICI_ACTIVE, true);
        this.setActionState((byte) 2, 30);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5F, 0.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.5F, 0.6F);

        // Vòng tro tàn bán kính 14 blocks
        double r = 14.0D;
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double px = this.getX() + r * Math.cos(angle);
            double pz = this.getZ() + r * Math.sin(angle);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, this.getY() + 0.2D, pz, 2, 0, 0.1D, 0, 0.01D);
            sl.sendParticles(ParticleTypes.SOUL, px, this.getY() + 0.3D, pz, 1, 0, 0.05D, 0, 0.02D);
        }

        AABB domainArea = this.getBoundingBox().inflate(14.0D, 6.0D, 14.0D);
        for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, domainArea)) {
            if (victim == this || !victim.isAlive() || victim instanceof TaKinhDaiTangEntity) continue;
            if (this.ownerUuid != null && !this.isRampaging && victim.getUUID().equals(this.ownerUuid)) continue;

            victim.hurt(this.damageSources().magic(), 35.0F);
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, false, true));
        }
    }

    private void triggerRampage(ServerLevel sl) {
        this.isRampaging = true;
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 3.0F, 0.6F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0F, 0.5F);

        if (this.ownerUuid != null) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (owner != null) {
                owner.sendSystemMessage(Component.literal("§4§l⚠️ [PHẢN PHỆ TÀ LINH] §cHết 60 giây kiểm soát! Tà Kinh Đại Tăng đã mất kiểm soát và quay sang độ hóa bạn!"));
                this.setTarget(owner);
            }
        }
    }

    public boolean recall(Player player) {
        if (this.level().isClientSide()) return false;
        if (this.isRampaging) {
            player.sendSystemMessage(Component.literal("§c⚠️ Tà Kinh Đại Tăng đã bạo tẩu mất kiểm soát! Không thể thu hồi bằng hình nhân gỗ!"));
            return false;
        }

        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.5F, 1.2F);

        ItemStack sealed = new ItemStack(ModItems.BODHI_EFFIGY_MONK.get());
        if (!player.getInventory().add(sealed)) {
            player.drop(sealed, false);
        }
        player.sendSystemMessage(Component.literal("§a§l[THU HỒI AN TOÀN] §fĐã thu hồi Tà Kinh Đại Tăng trở lại Hình Nhân Gỗ Bồ Đề!"));
        this.discard();
        return true;
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Tương tác với Hình Nhân Gỗ Bồ Đề (Rỗng)
        if (stack.is(ModItems.BODHI_EFFIGY_EMPTY.get())) {
            if (this.level().isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;

            // Thu hồi an toàn nếu là thú triệu hồi của người chơi
            if (this.isOwnedBy(player) && !this.isRampaging()) {
                this.recall(player);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }

            // Phong ấn Đại Tăng ngoài tự nhiên: HP <= 20% (<= 240 HP) hoặc Stun
            boolean eligible = this.getHealth() <= (this.getMaxHealth() * 0.20F) || this.isStunned();
            if (!eligible) {
                player.sendSystemMessage(Component.literal("§c⚠️ Tà Kinh Đại Tăng còn quá mạnh (§e" + (int) this.getHealth() + "§c/§e" + (int) this.getMaxHealth() + " HP§c)! Cần làm suy yếu xuống dưới 20% HP (<= 240 HP) hoặc làm choáng để phong ấn!"));
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5F, 0.8F);
                return net.minecraft.world.InteractionResult.FAIL;
            }

            // Thực hiện phong ấn thành công
            ServerLevel sl = (ServerLevel) this.level();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.2F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3.0F, 0.7F);

            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§a§l[PHONG ẤN THÀNH CÔNG] §fĐã thu phục và phong ấn Tà Kinh Đại Tăng vào Hình Nhân Gỗ Bồ Đề!"));
            }

            this.discard();
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            ItemStack sealed = new ItemStack(ModItems.BODHI_EFFIGY_MONK.get());
            if (!player.getInventory().add(sealed)) {
                player.drop(sealed, false);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        // 1. Nhục Thân Kim Cang: Miễn nhiễm lửa, rơi, đuối nước
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
            return false;
        }

        // 2. Giảm 50% sát thương từ đạn tầm xa
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            amount *= 0.50F;
        }

        // 3. Điểm yếu: Đòn tấn công tay không của Cứu Thế Quyền Hiện gây thêm 50% sát thương
        Entity attacker = source.getEntity();
        if (attacker instanceof CuuTheQuyenHienEntity ct && ct.isBareHanded()) {
            amount *= 1.50F;
            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.8F);
            }
        }

        return super.hurt(source, amount);
    }
}

