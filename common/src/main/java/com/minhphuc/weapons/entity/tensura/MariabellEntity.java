package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.content.evolution.UniversalSenseAbility;
import com.minhphuc.weapons.content.tensura.AzathothStomachData;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.SkillNullificationAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Boss Thực Thể: Tiểu Thư Mariabell Rosso (Rosso Family - Slime Isekai).
 * - Dáng vóc quý tộc 1.5 blocks, 32x32 crisp voxel pixel art.
 * - Nắm giữ Kỹ Năng Độc Nhất [Kẻ Tham Lam - Greedy One].
 * - Cơ chế nhận diện độc quyền Raphael (Đại Hiền Giả không thể phát hiện).
 * - Chi phối tâm trí sinh vật biến thành con rối mắt đỏ.
 * - Cơ chế tẩu thoát 30% khi < 30% HP trích xuất Veldora từ Không Gian Ảo làm bia đỡ đạn.
 * - Khi chết: Triệu hồi Hỗn Độn Long (Chaos Dragon) bằng cột plasma 3D khổng lồ xé toạc trời mây!
 */
public class MariabellEntity extends PathfinderMob {

    public static final EntityDataAccessor<Boolean> DATA_CASTING_GREED =
            SynchedEntityData.defineId(MariabellEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent;
    private final Set<UUID> alertedPlayersStage1 = new HashSet<>();
    private final Set<UUID> alertedPlayersStage2 = new HashSet<>();
    private int stage2Timer = -1;
    private ServerPlayer pendingPlayer = null;

    private int skillCooldownTicks = 0;
    private int trapCooldownTicks = 0;

    public MariabellEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§d§lTIỂU THƯ MARIABELL ROSSO §e« KẺ THAM LAM »"),
                BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        this.bossEvent.setVisible(true);
        this.xpReward = 1500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2800.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 75.0D)
                .add(Attributes.ARMOR, 32.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING_GREED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
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

    public boolean isCastingGreed() {
        return this.entityData.get(DATA_CASTING_GREED);
    }

    public void setCastingGreed(boolean casting) {
        this.entityData.set(DATA_CASTING_GREED, casting);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            if (this.isCastingGreed()) {
                // Hiệu ứng luồng ma tố hồng tím kèm bụi than đen cuộn quanh tay/thân thể (đúng anime)
                DustParticleOptions magentaDust = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.8F), 1.5F);
                for (int i = 0; i < 4; i++) {
                    double ox = (this.random.nextDouble() - 0.5D) * 0.8D;
                    double oy = this.random.nextDouble() * 1.4D;
                    double oz = (this.random.nextDouble() - 0.5D) * 0.8D;
                    this.level().addParticle(magentaDust, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.05, 0);
                    this.level().addParticle(ParticleTypes.ASH, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.02, 0);
                }
            }
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();
        if (this.isDeadOrDying() || !this.isAlive()) {
            this.bossEvent.setVisible(false);
            this.bossEvent.removeAllPlayers();
            return;
        }

        // Tự động dọn dẹp người chơi rời xa > 64 block hoặc không còn hợp lệ
        if (this.tickCount % 20 == 0) {
            for (ServerPlayer p : new java.util.ArrayList<>(this.bossEvent.getPlayers())) {
                if (p.isRemoved() || !p.isAlive() || p.distanceToSqr(this) > 64.0D * 64.0D) {
                    this.bossEvent.removePlayer(p);
                }
            }
            boolean hasNearby = !sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D)).isEmpty();
            this.bossEvent.setVisible(hasNearby);
        }

        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()), 0.0F, 1.0F));

        // 1. NHẬN DIỆN ĐỘC QUYỀN RAPHAEL (2 LẦN THÔNG BÁO)
        List<ServerPlayer> nearbyPlayers = sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(30.0D));
        for (ServerPlayer sp : nearbyPlayers) {
            boolean hasRaphael = PrimordialPlayerDataHelper.isDemonLord(sp)
                    || sp.getTags().contains("TensuraTrueDemonLord")
                    || sp.getTags().contains("TensuraAwakenedDemonLord");

            if (hasRaphael && !alertedPlayersStage1.contains(sp.getUUID())) {
                alertedPlayersStage1.add(sp.getUUID());
                pendingPlayer = sp;
                stage2Timer = 35; // 1.75s sau phát thông báo 2

                sp.displayClientMessage(
                        Component.literal("« Cảnh báo »: Phát hiện « Cá thể [Mariabell Rosso] » đang ẩn nặc gần đây. Bản chất linh hồn khá đen tối, nghi ngờ cá thể sở hữu 1 kỹ năng độc nhất trở lên!"),
                        false
                );
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.8F, 1.2F);
            }
        }

        if (stage2Timer > 0) {
            stage2Timer--;
            if (stage2Timer == 0 && pendingPlayer != null && pendingPlayer.isAlive()) {
                alertedPlayersStage2.add(pendingPlayer.getUUID());
                pendingPlayer.displayClientMessage(
                        Component.literal("« Báo cáo »: Sau khi phân tích thì phát hiện đó là kỹ năng độc nhất §c§l[Kẻ Tham Lam - Greedy One] §fcực kỳ nguy hiểm với chủ nhân!"),
                        false
                );
                sl.playSound(null, pendingPlayer.getX(), pendingPlayer.getY(), pendingPlayer.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.6F);
            }
        }

        // 2. MÊ HOẶC THƯƠNG VỤ & THAO TÚNG DÂN LÀNG / QUÁI VẬT XUNG QUANH
        if (this.tickCount % 40 == 0) {
            List<LivingEntity> villagers = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20.0D),
                    e -> (e instanceof Villager || e instanceof WanderingTrader || e instanceof Mob) && e != this);
            for (LivingEntity v : villagers) {
                if (!v.getTags().contains("GreedControlled")) {
                    v.addTag("GreedControlled");
                    v.addTag("MariabellEnchanted");
                    sl.sendParticles(ParticleTypes.SOUL, v.getX(), v.getY() + 1.0D, v.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.02D);
                }
            }
        }

        // 3. VÒNG TRÒN BẪY MA PHÁP DỤC VỌNG GẦN THÔN LÀNG
        if (trapCooldownTicks > 0) {
            trapCooldownTicks--;
        } else {
            trapCooldownTicks = 500; // Mỗi 25 giây đặt bẫy
            placeGreedMagicTrap(sl);
        }

        // 4. SÓNG RUNG ĐỘNG DỤC VỌNG (GREED SHOCKWAVE)
        if (skillCooldownTicks > 0) {
            skillCooldownTicks--;
        } else if (this.getTarget() != null && this.distanceToSqr(this.getTarget()) < 400.0D) {
            skillCooldownTicks = 260; // 13 giây
            castGreedShockwave(sl);
        }
    }

    private void placeGreedMagicTrap(ServerLevel sl) {
        Vec3 trapPos = this.position().add((this.random.nextDouble() - 0.5D) * 16.0D, 0, (this.random.nextDouble() - 0.5D) * 16.0D);
        DustParticleOptions pink = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.8F), 1.2F);
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            double rx = trapPos.x + Math.cos(rad) * 2.5D;
            double rz = trapPos.z + Math.sin(rad) * 2.5D;
            sl.sendParticles(pink, rx, trapPos.y + 0.15D, rz, 2, 0.05, 0.05, 0.05, 0.01);
            sl.sendParticles(ParticleTypes.ASH, rx, trapPos.y + 0.15D, rz, 1, 0.02, 0.02, 0.02, 0.01);
        }
        sl.playSound(null, trapPos.x, trapPos.y, trapPos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.4F, 1.3F);

        // Bẫy kích hoạt bắt giữ sinh vật bước qua
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, new AABB(trapPos.x - 3, trapPos.y - 1, trapPos.z - 3, trapPos.x + 3, trapPos.y + 2, trapPos.z + 3),
                e -> e != this && e.isAlive());
        for (LivingEntity v : victims) {
            if (!v.getTags().contains("GreedControlled")) {
                v.addTag("GreedControlled");
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, v.getX(), v.getY() + 1.2D, v.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    private void castGreedShockwave(ServerLevel sl) {
        this.setCastingGreed(true);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 2.5F, 1.2F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);

        DustParticleOptions magentaDust = new DustParticleOptions(new Vector3f(0.95F, 0.15F, 0.75F), 2.2F);
        for (double r = 1.0D; r <= 20.0D; r += 2.5D) {
            for (int a = 0; a < 360; a += 20) {
                double rad = Math.toRadians(a);
                double px = this.getX() + Math.cos(rad) * r;
                double pz = this.getZ() + Math.sin(rad) * r;
                sl.sendParticles(magentaDust, px, this.getY() + 0.3D, pz, 1, 0.05, 0.1, 0.05, 0.01);
                sl.sendParticles(ParticleTypes.ASH, px, this.getY() + 0.3D, pz, 1, 0.02, 0.05, 0.02, 0.01);
            }
        }

        List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20.0D),
                e -> e != this && e.isAlive() && !e.getTags().contains("GreedControlled"));
        for (LivingEntity e : enemies) {
            e.hurt(this.damageSources().magic(), 120.0F);
            e.setDeltaMovement(e.getDeltaMovement().multiply(0.2D, 0.2D, 0.2D));
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1.0D, e.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        }

        // Tắt casting sau 15 ticks
        this.level().getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 15, () -> {
            if (this.isAlive()) this.setCastingGreed(false);
        }));
    }

    /**
     * Tụ Linh Đoạt Phách: Hồi phục 30% HP tối đa khi có sinh vật chết gần đó.
     */
    public void onNearbyEntityDeath(LivingEntity entity) {
        if (!this.isAlive() || entity == this) return;
        float healAmount = this.getMaxHealth() * 0.30F;
        this.heal(healAmount);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.5D, this.getZ(), 12, 0.5, 0.5, 0.5, 0.1);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 1.5F, 1.1F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return super.hurt(source, amount);
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return super.hurt(source, amount);

        // Miễn nhiễm khống chế tinh thần & mọi loại Bá Khí (Long Uy, Hắc Ám Long Uy, Ma Vương Bá Khí)
        if (source.getMsgId().contains("aura") || source.getMsgId().contains("intimidation") || source.getMsgId().contains("tyranny")) {
            return false;
        }

        ServerLevel sl = (ServerLevel) this.level();

        // HIẾN TẾ THẾ MẠNG: Truyền sát thương chí mạng sang con rối bị chi phối gần nhất
        if (this.getHealth() - amount <= 0.0F) {
            List<LivingEntity> minions = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(30.0D),
                    e -> e != this && e.isAlive() && e.getTags().contains("GreedControlled"));
            if (!minions.isEmpty()) {
                LivingEntity sacrifice = minions.get(0);
                sacrifice.hurt(sl.damageSources().genericKill(), 9999.0F);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sacrifice.getX(), sacrifice.getY() + 1.0D, sacrifice.getZ(), 25, 0.4, 0.6, 0.4, 0.08);
                sl.playSound(null, sacrifice.getX(), sacrifice.getY(), sacrifice.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.2F, 1.4F);

                this.setHealth(this.getMaxHealth() * 0.40F); // Phục hồi 40% máu sau khi thế mạng

                if (source.getEntity() instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.literal("§c« BÁO CÁO RAPHAEL »\n§eCá thể Mariabell Rosso vừa kích hoạt cơ chế [Hiến Tế Thế Mạng], chuyển toàn bộ sát thương tử vong sang con rối bị chi phối!"),
                            false
                    );
                }
                return false;
            }
        }

        // KẾ SÁCH ĐÀO TẨU 30% KHI MÁU DƯỚI 30% HP & TRÍCH XUẤT VELDORA LÀM BIA ĐỠ ĐẠN
        if (this.getHealth() < this.getMaxHealth() * 0.30F && this.random.nextFloat() < 0.30F) {
            ServerPlayer targetPlayer = source.getEntity() instanceof ServerPlayer sp ? sp : (pendingPlayer != null ? pendingPlayer : null);
            if (targetPlayer != null) {
                // Kiểm tra xem người chơi có Veldora trong kết nối linh hồn / dạ dày không
                boolean hasVeldora = targetPlayer.getTags().contains("VeldoraSummonUnlocked")
                        || EntityDataHelper.getCustomData(targetPlayer).getBoolean(AzathothStomachData.KEY_VELDORA_BEFRIENDED);

                if (hasVeldora) {
                    // Lôi Veldora ra ngoài và áp đặt Tham Lam biến mắt đỏ cuồng loạn
                    VeldoraEntity veldora = ModEntities.VELDORA.get().create(sl);
                    if (veldora != null) {
                        veldora.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        veldora.setGreedControlled(true);
                        sl.addFreshEntity(veldora);

                        targetPlayer.displayClientMessage(
                                Component.literal("§4§l« BÁO ĐỘNG ĐỎ RAPHAEL: CHÂN LONG BỊ CƯỠNG CHẾ CHI PHỐI! »\n§cẢ nhóc Mariabell Rosso đã sử dụng tà thuật Tham Lam can thiệp vào mạch liên kết, lôi Bạo Phong Long Veldora ra ngoài làm bia đỡ đạn và trốn thoát!"),
                                false
                        );

                        // Chỉ dẫn 3 bước chiến thuật cứu Veldora
                        targetPlayer.sendSystemMessage(
                                Component.literal("§e§l==================================================\n" +
                                        "§6§l« CHỈ DẪN CHIẾN THUẬT TỪ RAPHAEL: QUY TRÌNH HÓA GIẢI CHÂN LONG »\n" +
                                        "§f1. Giai đoạn 1: Bào mòn sinh lực cá thể Veldora xuống dưới 50% để làm rạn nứt kết giới Tham Lam!\n" +
                                        "§f2. Giai đoạn 2: Lập tức thi triển §b§l[Lồng Giam Vô Hạn]§f để cô lập hoàn toàn mạch ma tố Chân Long khỏi tà thuật ngoại giới trong 6 giây!\n" +
                                        "§f3. Giai đoạn 3: Áp sát bản thể, tung đòn §d§l[Phong Sát Kỹ Năng]§f hoặc kích hoạt §6§l[Bạo Thực Vương]§f để triệt tiêu hoàn toàn mầm mống ký sinh!\n" +
                                        "§e§l==================================================")
                        );
                    }
                } else {
                    targetPlayer.displayClientMessage(
                            Component.literal("§c« Báo Cáo Raphael »: Cá thể Mariabell Rosso đã sử dụng ma thuật không gian dịch chuyển tẩu thoát khỏi khu vực!"),
                            true
                    );
                }

                // Dịch chuyển Mariabell đi xa 180 block
                this.bossEvent.removeAllPlayers();
                double escapeX = this.getX() + (this.random.nextBoolean() ? 1 : -1) * (150 + this.random.nextInt(40));
                double escapeZ = this.getZ() + (this.random.nextBoolean() ? 1 : -1) * (150 + this.random.nextInt(40));
                BlockPos targetPos = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(escapeX, 100, escapeZ));
                this.teleportTo(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 1.0F);
                return false;
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();
        if (this.level().isClientSide()) return;

        ServerLevel sl = (ServerLevel) this.level();

        // 1. Rơi vật phẩm Linh Hồn Băng Hoại của Mariabell
        ItemStack soulStack = new ItemStack(ModItems.MARIABELL_SOUL.get());
        soulStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("§d§l[Linh Hồn Băng Hoại Của Mariabell Rosso]"));
        this.spawnAtLocation(soulStack);

        Vec3 pos = this.position();

        // 2. TRẬN ĐỊA VÒNG TRÒN MA PHÁP KHỔNG LỒ DƯỚI ĐẤT (Bán kính 16m)
        // 3 Vành ma pháp trận tím thẫm & hồng neon rực rỡ + 6 nhánh cổ tự
        DustParticleOptions neonPink = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.85F), 3.0F);
        DustParticleOptions darkMagenta = new DustParticleOptions(new Vector3f(0.65F, 0.0F, 0.85F), 3.0F);
        DustParticleOptions voidPurple = new DustParticleOptions(new Vector3f(0.35F, 0.0F, 0.65F), 3.5F);

        for (int i = 0; i < 96; i++) {
            double angle = i * Math.PI * 2.0D / 96.0D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // Vành ngoài R=16m
            sl.sendParticles(neonPink, pos.x + cos * 16.0D, pos.y + 0.15D, pos.z + sin * 16.0D, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x + cos * 16.0D, pos.y + 0.2D, pos.z + sin * 16.0D, 1, 0, 0.05, 0, 0.02D);

            // Vành giữa R=10m
            sl.sendParticles(darkMagenta, pos.x + cos * 10.0D, pos.y + 0.15D, pos.z + sin * 10.0D, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x + cos * 10.0D, pos.y + 0.2D, pos.z + sin * 10.0D, 1, 0, 0.1, 0, 0.05D);

            // Vành trong R=5m
            sl.sendParticles(voidPurple, pos.x + cos * 5.0D, pos.y + 0.15D, pos.z + sin * 5.0D, 1, 0, 0, 0, 0);
        }

        // 6 Nhánh hoa văn ma pháp trận xuyên tâm
        for (int arm = 0; arm < 6; arm++) {
            double armAngle = arm * Math.PI / 3.0D;
            double cos = Math.cos(armAngle);
            double sin = Math.sin(armAngle);
            for (double d = 1.0D; d <= 16.0D; d += 1.0D) {
                sl.sendParticles(neonPink, pos.x + cos * d, pos.y + 0.15D, pos.z + sin * d, 1, 0, 0, 0, 0);
            }
        }

        // 3. LỬA RỰC CHÁY KHẮP TẾ ĐÀN
        for (int f = 0; f < 8; f++) {
            double fa = f * Math.PI / 4.0D;
            BlockPos firePos = BlockPos.containing(pos.x + Math.cos(fa) * 8.0D, pos.y, pos.z + Math.sin(fa) * 8.0D);
            BlockPos above = firePos.above();
            if (sl.getBlockState(firePos).isAir()) {
                sl.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            } else if (sl.getBlockState(above).isAir() && sl.getBlockState(firePos).isSolid()) {
                sl.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
            }
        }
        sl.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5D, pos.z, 150, 8.0D, 1.0D, 8.0D, 0.15D);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.5D, pos.z, 100, 10.0D, 1.5D, 10.0D, 0.12D);
        sl.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 1.0D, pos.z, 80, 6.0D, 2.0D, 6.0D, 0.08D);

        // 4. HÀNG LOẠT SẤM SÉT ĐÁNH XUỐNG XUNG QUANH
        sl.setWeatherParameters(0, 12000, true, true);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 6.0F, 0.6F);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 0.5F);

        for (int s = 0; s < 10; s++) {
            double sa = s * (Math.PI * 2.0D / 10.0D) + (this.random.nextDouble() - 0.5D) * 0.4D;
            double sDist = 6.0D + this.random.nextDouble() * 10.0D;
            BlockPos boltPos = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    BlockPos.containing(pos.x + Math.cos(sa) * sDist, pos.y, pos.z + Math.sin(sa) * sDist));
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(boltPos));
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
        }

        // 5. CỘT TIA PLASMA 3D BẮN THẲNG LÊN TRỜI CAO XÉ RÁCH MÂY
        Vec3 upDir = new Vec3(0, 1, 0);
        ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_CHAOS_STAR_BUSTER, this.getUUID(), pos.add(0, 0.5D, 0), upDir, 65, ClientboundCinematicSkillPacket.ACTION_START, 1.50f
        ));

        for (double dy = 0; dy <= 100.0D; dy += 4.0D) {
            sl.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + dy, pos.z, 2, 0.5D, 0.5D, 0.5D, 0);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + dy, pos.z, 1, 0, 0, 0, 0);
        }

        // 6. HỖN ĐỘN LONG (CHAOS DRAGON) GIÁNG LÂM TỪ CỘT PLASMA
        ChaosDragonEntity chaosDragon = ModEntities.CHAOS_DRAGON.get().create(sl);
        if (chaosDragon != null) {
            chaosDragon.moveTo(pos.x, pos.y + 6.0D, pos.z, this.getYRot(), 0.0F);
            sl.addFreshEntity(chaosDragon);
            sl.playSound(null, pos.x, pos.y + 6.0D, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 5.0F, 0.5F);
        }

        // 7. THÔNG BÁO RAPHAEL RUNG CHUYỂN MÀN HÌNH
        for (ServerPlayer sp : sl.players()) {
            sp.sendSystemMessage(
                    Component.literal("§4§l==================================================\n" +
                            "§c§l« CẢNH BÁO TỐI THƯỢNG RAPHAEL: DỊ TƯỢNG PHONG ẤN THƯỢNG CỔ SỤP ĐỔ »\n" +
                            "§eCá thể [Mariabell Rosso] đã dùng toàn bộ tàn dư sinh mạng và ma tố Tham Lam để hiến tế!\n" +
                            "§5Trận địa ma pháp đã mở ra cánh cổng Hỗn Mang xé rách bầu trời!\n" +
                            "§4§l« BÁO ĐỘNG ĐỎ: HỖN ĐỘN LONG (CHAOS DRAGON) ĐÃ GIÁNG THẾ! »\n" +
                            "§cKhông gian trong bán kính 100m đã bị phong tỏa hoàn toàn! Hãy chuẩn bị nghênh chiến!\n" +
                            "§4§l==================================================")
            );
            sp.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }
}
