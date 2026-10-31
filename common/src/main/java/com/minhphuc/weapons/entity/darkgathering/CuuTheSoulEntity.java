package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.content.darkgathering.CuuTheCurseManager;
import com.minhphuc.weapons.content.darkgathering.SeveredBrainItem;
import com.minhphuc.weapons.content.darkgathering.SeveredHeartItem;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * LINH HỒN CỦA CỨU THẾ QUYỀN HIỆN (ASTRAL SOUL):
 * - Được phóng xuất khi Cứu Thế Quyền Hiện thi triển Tuyệt Kĩ Chém Xuyên Thời Gian.
 * - Hỗ trợ Tam Thức Luân Chuyển:
 *   + Mode 0: Đoạt Tâm Trảm (Rơi Trái Tim).
 *   + Mode 1: Đoạt Não Trảm (Rơi Bộ Não).
 *   + Mode 2: Tuyệt Mệnh Song Trảm (Rơi Cả Tim Lẫn Não).
 * - Tương tác Ác Ma Thủy Tổ: 35% kích hoạt Tái Sinh Thể Linh Hồn; 65% nhận 180 sát thương chí mạng.
 */
public class CuuTheSoulEntity extends PathfinderMob {

    private UUID casterUuid = null;
    private UUID targetUuid = null;
    private int strikeMode = 0; // 0 = HEART, 1 = BRAIN, 2 = DUAL
    private int lifetimeTicks = 45; // Tồn tại tối đa 2.25s

    public CuuTheSoulEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void initStrike(UUID casterUuid, UUID targetUuid) {
        initStrike(casterUuid, targetUuid, 0);
    }

    public void initStrike(UUID casterUuid, UUID targetUuid, int strikeMode) {
        this.casterUuid = casterUuid;
        this.targetUuid = targetUuid;
        this.strikeMode = strikeMode;
    }

    public int getStrikeMode() {
        return this.strikeMode;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        // Bị người chơi tấn công (hoặc đạn/mũi tên của người chơi)
        if (source.getEntity() instanceof Player attacker) {
            Level lvl = this.level();
            if (lvl instanceof ServerLevel sl) {
                // Hiệu ứng bẻ gãy không gian
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2D, this.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.2D, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.2F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.8F, 1.5F);

                if (attacker instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.literal("§a§l[TIÊN TRI] §fBạn đã đánh tan Linh Hồn của Cứu Thế Quyền Hiện, bẻ gãy tuyệt kỹ chém xuyên thời gian và bước vào vùng không gian nghịch chuyển! ✨"),
                            false
                    );
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2, false, false, true));
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, false, false, true));
                }

                // Choáng bản thể Cứu Thế Quyền Hiện trong 3s (60 ticks) và tích lũy số lần bị Tiên Tri vô hiệu
                if (casterUuid != null) {
                    var casterEnt = sl.getEntity(casterUuid);
                    if (casterEnt instanceof CuuTheQuyenHienEntity ct) {
                        ct.stun(60);
                        ct.recordSoulParried(attacker);
                    }
                }
            }

            this.discard();
            return true;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        this.lifetimeTicks--;
        if (this.lifetimeTicks <= 0) {
            this.discard();
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();
        LivingEntity target = null;
        if (this.targetUuid != null) {
            var ent = sl.getEntity(this.targetUuid);
            if (ent instanceof LivingEntity le && le.isAlive()) {
                target = le;
            }
        }

        if (target == null) {
            this.discard();
            return;
        }

        // Lao nhanh về phía mục tiêu
        Vec3 targetEye = target.getEyePosition();
        Vec3 diff = targetEye.subtract(this.position());
        double distSq = diff.lengthSqr();

        if (distSq > 0.01D) {
            Vec3 dir = diff.normalize();
            this.setDeltaMovement(dir.scale(1.25D));
            this.setYRot((float) (Math.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F);
        }

        // Khi tiếp cận mục tiêu trong cự ly chém (2.2 blocks)
        if (distSq <= 2.2D * 2.2D) {
            executeStrike(sl, target);
            this.discard();
        }
    }

    private void executeStrike(ServerLevel sl, LivingEntity target) {
        // Miễn nhiễm với các Thực thể Tối Cao (Milim, Velgrynd, Velzard)
        if (target instanceof VelzardEntity || target instanceof MilimEntity || target instanceof VelgryndEntity) {
            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5F, 1.8F);
            return;
        }

        // 1. Rơi vật phẩm theo Tam Thức Tuyệt Kĩ (Tim, Não, hoặc Cả Hai)
        if (this.strikeMode == 0) {
            // Mode 0: Đoạt Tim
            ItemStack heartStack = SeveredHeartItem.createForEntity(target);
            ItemEntity itemEnt = new ItemEntity(sl, target.getX(), target.getY() + 0.6D, target.getZ(), heartStack);
            itemEnt.setDeltaMovement(new Vec3(0, 0.25D, 0));
            sl.addFreshEntity(itemEnt);
        } else if (this.strikeMode == 1) {
            // Mode 1: Đoạt Não
            ItemStack brainStack = SeveredBrainItem.createForEntity(target);
            ItemEntity itemEnt = new ItemEntity(sl, target.getX(), target.getY() + 0.6D, target.getZ(), brainStack);
            itemEnt.setDeltaMovement(new Vec3(0, 0.25D, 0));
            sl.addFreshEntity(itemEnt);
        } else {
            // Mode 2: Tuyệt Mệnh Song Trảm (Cả Tim Lẫn Não văng 2 bên)
            ItemStack heartStack = SeveredHeartItem.createForEntity(target);
            ItemEntity heartEnt = new ItemEntity(sl, target.getX() + 0.2D, target.getY() + 0.6D, target.getZ(), heartStack);
            heartEnt.setDeltaMovement(new Vec3(0.18D, 0.32D, 0.0D));
            sl.addFreshEntity(heartEnt);

            ItemStack brainStack = SeveredBrainItem.createForEntity(target);
            ItemEntity brainEnt = new ItemEntity(sl, target.getX() - 0.2D, target.getY() + 0.6D, target.getZ(), brainStack);
            brainEnt.setDeltaMovement(new Vec3(-0.18D, 0.32D, 0.0D));
            sl.addFreshEntity(brainEnt);
        }

        // Hiệu ứng chém đoạt mệnh
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0D, target.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(), 25, 0.5, 0.5, 0.5, 0.2);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0F, 0.6F);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.9F);

        // 2. Tương tác đặc biệt với Ác Ma Thủy Tổ (Primordial Demons)
        if (target instanceof PrimordialDemonEntity demon) {
            if (demon.getRandom().nextFloat() < 0.35F) {
                // 35% Cơ hội kích hoạt Tái Sinh Thể Linh Hồn Thủy Tổ!
                demon.heal(demon.getMaxHealth() * 0.35F);
                sl.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.8F, 1.4F);
                sl.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.2F);
                sl.sendParticles(ParticleTypes.SOUL, demon.getX(), demon.getY() + 1.2D, demon.getZ(), 35, 0.5D, 0.8D, 0.5D, 0.1D);
                sl.sendParticles(ParticleTypes.FLASH, demon.getX(), demon.getY() + 1.5D, demon.getZ(), 2, 0, 0, 0, 0);

                // Đẩy lùi kẻ địch xung quanh 4 blocks
                AABB repelArea = demon.getBoundingBox().inflate(4.0D);
                for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, repelArea)) {
                    if (nearby != demon) {
                        Vec3 push = nearby.position().subtract(demon.position()).normalize().scale(1.25D).add(0, 0.35D, 0);
                        nearby.setDeltaMovement(push);
                        nearby.hasImpulse = true;
                    }
                }

                sl.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§c§l[THỦY TỔ MA LỰC] §e" + demon.getDemonType().getColorName() + " §fđã vận dụng Tái Sinh Thể Linh Hồn, hóa giải nhát chém đoạt mệnh của Cứu Thế Quyền Hiện!"),
                        false
                );
                return;
            } else {
                // 65% Thất bại: Chịu 180 sát thương chí mạng
                demon.hurt(sl.damageSources().mobAttack(this), 180.0F);
                return;
            }
        }

        // 3. Kích hoạt án tử cho người chơi
        if (target instanceof ServerPlayer sp) {
            CuuTheCurseManager.CurseType cType = switch (this.strikeMode) {
                case 1 -> CuuTheCurseManager.CurseType.BRAIN;
                case 2 -> CuuTheCurseManager.CurseType.DUAL;
                default -> CuuTheCurseManager.CurseType.HEART;
            };
            CuuTheCurseManager.applyCurse(sl, sp, cType);
        } else {
            // Sinh vật thông thường chịu sát thương cực lớn
            float dmg = (this.strikeMode == 2) ? 200.0F : 120.0F;
            target.hurt(sl.damageSources().mobAttack(this), dmg);
        }
    }
}
