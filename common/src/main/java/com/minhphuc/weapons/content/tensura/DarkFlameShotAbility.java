package com.minhphuc.weapons.content.tensura;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * CHIÊU 2: HẮC VIÊM HỎA CẦU (DARK FLAME SHOT - BENIMARU)
 * 1. Bắn Quả Cầu Lửa Đen:
 *    - Phóng ra quả cầu Hắc Viêm theo hướng nhìn của người chơi.
 *    - Trúng đích gây 35 sát thương bộc phá Hắc Hỏa diện rộng (bán kính 3.5m).
 * 2. Hắc Viêm Bất Diệt (Unextinguishable Black Flame):
 *    - Ngọn lửa ma thuật không thể bị dập tắt bởi trời mưa hay ngâm mình dưới nước!
 *    - Liên tục thiêu đốt 3 sát thương chuẩn mỗi nửa giây trong 8 giây (160 ticks).
 * 3. Tối ưu ZERO-LAG:
 *    - Raycast tức thì + đường đạn hạt ánh sáng mượt mà, không sinh entity đạn thừa thãi.
 */
public class DarkFlameShotAbility {

    public static class BurningVictim {
        public final UUID entityUuid;
        public int ticksRemaining;

        public BurningVictim(UUID entityUuid, int ticksRemaining) {
            this.entityUuid = entityUuid;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final Map<UUID, Integer> UNEXTINGUISHABLE_FLAMES = Collections.synchronizedMap(new HashMap<>());

    public static void cast(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 maxTarget = eyePos.add(lookVec.scale(32.0D));

        BlockHitResult blockHit = level.clip(new ClipContext(eyePos, maxTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPos = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : maxTarget;

        // Vẽ vệt đường đạn quả cầu lửa đen từ mắt người chơi tới đích
        double distance = eyePos.distanceTo(impactPos);
        int steps = (int) (distance * 2);
        for (int i = 0; i < steps; i++) {
            double prog = (double) i / steps;
            Vec3 p = eyePos.add(lookVec.scale(distance * prog));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
        }

        // Âm thanh phóng hỏa cầu
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.2F);

        // Hiệu ứng nổ tại điểm va chạm
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactPos.x, impactPos.y, impactPos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, impactPos.x, impactPos.y, impactPos.z, 40, 0.8D, 0.8D, 0.8D, 0.1D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, impactPos.x, impactPos.y, impactPos.z, 20, 0.5D, 0.5D, 0.5D, 0.05D);

        // Gây sát thương và gieo rắc Hắc Viêm Bất Diệt (Bán kính 3.5m)
        AABB blastArea = new AABB(impactPos.x - 3.5D, impactPos.y - 3.5D, impactPos.z - 3.5D,
                                  impactPos.x + 3.5D, impactPos.y + 3.5D, impactPos.z + 3.5D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, blastArea, LivingEntity::isAlive);

        for (LivingEntity target : targets) {
            if (target == player) continue;

            target.hurt(level.damageSources().magic(), 35.0F);
            target.setRemainingFireTicks(160); // 8 giây
            UNEXTINGUISHABLE_FLAMES.put(target.getUUID(), 160);
        }

        player.displayClientMessage(
                Component.literal("§4§l[HẮC VIÊM HỎA CẦU] §fĐã phóng hỏa cầu hắc ám thiêu rụi mục tiêu! (Lửa đen bất diệt) 🔥⬛"),
                true
        );
    }

    /**
     * Duy trì Hắc Viêm Bất Diệt: Không thể bị dập tắt bởi trời mưa hay xuống nước.
     */
    public static void tick(ServerLevel level) {
        if (UNEXTINGUISHABLE_FLAMES.isEmpty()) return;

        synchronized (UNEXTINGUISHABLE_FLAMES) {
            Iterator<Map.Entry<UUID, Integer>> it = UNEXTINGUISHABLE_FLAMES.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                UUID uuid = entry.getKey();
                int remaining = entry.getValue() - 1;

                LivingEntity victim = (LivingEntity) level.getEntity(uuid);
                if (victim == null || !victim.isAlive() || remaining <= 0) {
                    it.remove();
                    continue;
                }

                entry.setValue(remaining);

                // Ép buộc duy trì lửa đen kể cả khi đang ngâm nước hoặc trời mưa
                victim.setRemainingFireTicks(60);

                // Mỗi 10 ticks (0.5 giây): Gây 3 sát thương chuẩn và sinh hiệu ứng lửa đen
                if (remaining % 10 == 0) {
                    victim.hurt(level.damageSources().inFire(), 3.0F);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.02D);
                    level.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(), 2, 0.2D, 0.3D, 0.2D, 0.01D);
                }
            }
        }
    }
}
