package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.content.tensura.SageManager;
import com.minhphuc.weapons.content.tensura.VoiceOfTheWorld;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tà Thần Tượng - Lõi Di Tích Tâm Linh Cấp S (Dark Gathering S-Rank Haunted Spot):
 * - Xuất hiện hiếm trong Overworld ban đêm/rừng sâu; tỏa Lời Nguyền Đếm Ngược (0% -> 100%) trong bán kính 18 block.
 * - Dùng Bông Bịt Tai Trấn Ám (WARDING_EARPLUGS), Hình Nhân Bồ Đề (BODHI_EFFIGY_MONK) hoặc Shigaraki Tanuki để chống lại lời nguyền!
 * - Khi Lời Nguyền đạt 100%: Kích nổ sát thương Ám Hồn + Triệu hồi Tà Kinh Đại Tăng / Marbas phục kích.
 * - Phá hủy Tà Thần Tượng để giải trừ Di Tích Cấp S và thu chiến lợi phẩm hiếm.
 */
public class CursedIdolEntity extends Monster {

    private static final DustParticleOptions CURSE_RED =
            new DustParticleOptions(new Vector3f(0.85F, 0.06F, 0.16F), 1.8F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4§l[DI TÍCH TÂM LINH CẤP S] §c§lTÀ THẦN TƯỢNG §7- §5[Đếm Ngược Lời Nguyền]"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private final Map<UUID, Integer> curseGauge = new HashMap<>();
    private int guardianSpawnCooldown = 160;

    public CursedIdolEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 800;
        this.setCustomName(Component.literal("§4§l[Cấp S] §cTà Thần Tượng Nguyền Rủa"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 850.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            BlockPos pos = this.blockPosition();
            if (level instanceof ServerLevel sl) {
                if (sl.dimension() != Level.OVERWORLD || sl.isDay()) return false;
                if (!sl.getEntitiesOfClass(CursedIdolEntity.class, new AABB(pos).inflate(110.0D)).isEmpty()) {
                    return false;
                }
            }
        }
        return level.getDifficulty() != Difficulty.PEACEFUL && super.checkSpawnRules(level, spawnType);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, this.getDeltaMovement().y * 0.2, 0);
        if (this.level().isClientSide()) {
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(CURSE_RED,
                        this.getX() + (this.random.nextDouble() - 0.5) * 2.2,
                        this.getY() + 0.5 + this.random.nextDouble() * 2.0,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 2.2, 0, 0.04, 0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(!sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(24.0D)).isEmpty());

        if (guardianSpawnCooldown > 0) guardianSpawnCooldown--;

        // Đếm ngược Lời Nguyền Cấp S mỗi 20 ticks (1s)
        if (this.tickCount % 20 == 0) {
            for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(18.0D))) {
                boolean hasEarplugs = hasItem(sp, ModItems.WARDING_EARPLUGS.get());
                boolean hasBodhi = hasItem(sp, ModItems.BODHI_EFFIGY_MONK.get());
                boolean hasTanuki = hasItem(sp, ModItems.SHIGARAKI_TANUKI_GONGEN.get());

                int gain = 8;
                if (hasEarplugs) gain -= 4;
                if (hasBodhi || hasTanuki) gain = Math.max(1, gain - 4);

                int next = Math.min(100, curseGauge.getOrDefault(sp.getUUID(), 0) + gain);
                curseGauge.put(sp.getUUID(), next);

                String shieldInfo = (hasEarplugs || hasBodhi || hasTanuki) ? " §a(Bùa Hộ Mệnh Đang Cản)" : " §c(Dùng Nút Bịt Tai/Hình Nhân Hộ Mệnh!)";
                sp.displayClientMessage(Component.literal("§4[Di Tích Cấp S] §eĐộ Ăn Mòn Lời Nguyền: §c" + next + "%" + shieldInfo), true);

                if (next >= 100) {
                    curseGauge.put(sp.getUUID(), 0);
                    sp.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2));
                    sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                    sp.hurt(this.damageSources().wither(), hasBodhi ? 10.0F : 28.0F);
                    sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0F, 0.5F);
                    SageManager.pushAlert(sp, "S_RANK_CURSE_MAX",
                            "Cảnh báo! Lời Nguyền Di Tích Cấp S đã bùng phát 100%! Hãy phá hủy Tà Thần Tượng ngay!", "", 120);

                    if (guardianSpawnCooldown <= 0) {
                        var monk = sl.random.nextBoolean() ? ModEntities.TA_KINH_DAI_TANG.get().create(sl) : ModEntities.MARBAS.get().create(sl);
                        if (monk != null) {
                            monk.setPos(this.getX() + 3.0, this.getY(), this.getZ());
                            sl.addFreshEntity(monk);
                        }
                        guardianSpawnCooldown = 400;
                    }
                }
            }
        }
    }

    private boolean hasItem(ServerPlayer sp, net.minecraft.world.item.Item item) {
        if (sp.getMainHandItem().is(item) || sp.getOffhandItem().is(item)) return true;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel sl, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, source, recentlyHit);
        sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.8, this.getZ(), new ItemStack(ModItems.BODHI_EFFIGY_MONK.get(), 1)));
        sl.addFreshEntity(new ItemEntity(sl, this.getX(), this.getY() + 0.8, this.getZ(), new ItemStack(Items.NETHERITE_INGOT, 2)));
        for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0D))) {
            VoiceOfTheWorld.announce(sp, "§b[Thanh Tẩy Di Tích Cấp S] §fTà Thần Tượng đã tan rã — Vùng đất nguyền rủa đã được giải thoát!");
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        SageManager.pushAlert(player, "S_RANK_SPOT",
                "Phát hiện Di Tích Tâm Linh Cấp S! Mang Nút Bịt Tai Trấn Ám hoặc Hình Nhân Bồ Đề và phá hủy Tà Thần Tượng.",
                "", 160);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
}
