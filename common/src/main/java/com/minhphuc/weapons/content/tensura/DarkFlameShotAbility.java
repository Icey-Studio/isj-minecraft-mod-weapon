package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.network.ClientboundHellflarePacket;
import com.minhphuc.weapons.network.ModMessages;
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
 * TUYỆT KỸ ĐIỆN ẢNH TỐI THƯỢNG:
 * 1. Khởi tạo: Người chơi bay bổng lên cao (+6 blocks), camera lia rộng thấy toàn cảnh ngưng tụ ma hỏa hắc ám.
 * 2. Phóng Quả Cầu Lửa Đen (Black Flame Orb): Camera góc vai giật chấn động, hỏa cầu bay xé gió xuống chiến trường.
 * 3. Điểm va chạm: Xuất hiện hình hộp nhỏ 1m trong suốt, bành trướng thành khối lập phương khổng lồ 24m (Bán kính 12m).
 *    - TUYỆT ĐỐI KHÔNG BAO GỒM NGƯỜI CHƠI (Tâm ngục tự động dịch chuyển đảm bảo người chơi luôn đứng ngoài).
 * 4. Niêm phong tuyệt đối: 6 mặt lập phương trở thành MÀN CHẮN ĐEN TUYỀN 100% BÊN NGOÀI KHÔNG NHÌN VÀO ĐƯỢC!
 * 5. Địa Ngục Lửa Đen Bất Diệt (True Black Fire):
 *    - 100% GPU OpenGL Client, render Lửa Đen tuyền chân thực cuộn trào bên trong.
 *    - DUY TRÌ VĨNH CỬU KHÔNG BAO GIỜ BIẾN MẤT trừ khi dùng Phong Sát Kĩ Năng!
 * 6. Sụp Đổ Chân Không (Vacuum Implosion) khi dùng Phong Sát Kĩ Năng gây 70 sát thương cực hạn.
 */
public class DarkFlameShotAbility {

    public static class ActiveDarkFlamePrison {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final UUID casterUuid;
        public final Vec3 startPos;
        public final Vec3 targetPos;
        public final float prisonRadius = 12.0f; // Khối 24x24x24m
        public int ticksAlive = 0;

        public ActiveDarkFlamePrison(ServerLevel level, ServerPlayer caster, Vec3 startPos, Vec3 targetPos) {
            this.level = level;
            this.caster = caster;
            this.casterUuid = (caster != null) ? caster.getUUID() : UUID.randomUUID();
            this.startPos = startPos;
            this.targetPos = targetPos;
        }
    }

    private static final List<ActiveDarkFlamePrison> ACTIVE_PRISONS = Collections.synchronizedList(new ArrayList<>());

    /**
     * Kích hoạt tuyệt kỹ Hắc Viêm Hỏa Cầu điện ảnh
     */
    public static void cast(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 maxTarget = eyePos.add(lookVec.scale(48.0D));

        // 1. Raycast tìm đối thủ hoặc khối địa hình
        BlockHitResult blockHit = level.clip(new ClipContext(eyePos, maxTarget,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 hitPos = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : maxTarget;

        // Quét tìm Entity trong đường raycast
        AABB scanBox = new AABB(eyePos, hitPos).inflate(2.0D);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != player && e.isAlive() && !e.isSpectator());

        LivingEntity hitEntity = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity target : potentialTargets) {
            AABB mobBox = target.getBoundingBox().inflate(0.5D);
            var clipOpt = mobBox.clip(eyePos, hitPos);
            if (clipOpt.isPresent()) {
                double dSq = eyePos.distanceToSqr(clipOpt.get());
                if (dSq < closestDistSq) {
                    closestDistSq = dSq;
                    hitEntity = target;
                }
            }
        }

        Vec3 targetPos;
        if (hitEntity != null) {
            targetPos = hitEntity.position().add(0, hitEntity.getBbHeight() * 0.5D, 0);
        } else {
            targetPos = hitPos;
        }

        // =========================================================================
        // YÊU CẦU CỐT LÕI: TUYỆT ĐỐI KHÔNG BAO GỒM NGƯỜI CHƠI TRONG HỘP
        // Nếu khoảng cách từ người chơi tới tâm ngục nhỏ hơn (Bán kính 12m + 4.5m an toàn),
        // tự động đẩy tâm ngục về phía trước theo hướng nhìn để người chơi đứng ngoài hoàn toàn!
        // =========================================================================
        float safeMargin = 12.0f + 4.5f;
        double playerDist = player.position().distanceTo(targetPos);
        if (playerDist < safeMargin) {
            Vec3 horizLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
            if (horizLook.lengthSqr() < 0.01) horizLook = new Vec3(0, 0, 1);
            targetPos = player.position().add(horizLook.scale(safeMargin));
        }

        ActiveDarkFlamePrison instance = new ActiveDarkFlamePrison(level, player, eyePos, targetPos);

        // Hủy Dark Flame Prison cũ của cùng caster nếu có
        cancelCaster(level, player.getUUID());
        ACTIVE_PRISONS.add(instance);

        // 2. Gửi Packet tới toàn bộ người chơi xung quanh để đồng bộ đồ họa OpenGL & Camera Điện Ảnh
        ModMessages.sendToPlayers(level.players(),
                new ClientboundHellflarePacket(player.getUUID(), eyePos, targetPos, instance.prisonRadius, ClientboundHellflarePacket.PHASE_START));

        // 3. Âm thanh khởi đầu ma thuật
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.5F, 0.8F);
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0F, 0.65F);

        // Thông báo
        player.displayClientMessage(
                Component.literal("§4§l[BENIMARU - HẮC VIÊM HỎA CẦU] §cKhởi tạo Hắc Viêm Hỏa Cầu! Chuẩn bị phóng kết giới địa ngục 24m... 🔥⬛"),
                true
        );
    }

    public static void castFromDemon(ServerLevel level, com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity demon, Vec3 eyePos, Vec3 targetPos, ServerPlayer owner) {
        if (level == null || demon == null) return;
        ActiveDarkFlamePrison instance = new ActiveDarkFlamePrison(level, owner, eyePos, targetPos);
        cancelCaster(level, demon.getUUID());
        ACTIVE_PRISONS.add(instance);

        ModMessages.sendToPlayers(level.players(),
                new com.minhphuc.weapons.network.ClientboundHellflarePacket(demon.getUUID(), eyePos, targetPos, instance.prisonRadius, com.minhphuc.weapons.network.ClientboundHellflarePacket.PHASE_START));

        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.5F, 0.8F);
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0F, 0.65F);
    }

    /**
     * Server Tick định kỳ cho toàn bộ các thực thể Dark Flame Prison (gọi từ TensuraEvents)
     */
    public static void tick(ServerLevel level) {
        if (ACTIVE_PRISONS.isEmpty()) return;

        synchronized (ACTIVE_PRISONS) {
            Iterator<ActiveDarkFlamePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActiveDarkFlamePrison hf = it.next();
                if (hf.level != level) continue;

                hf.ticksAlive++;

                // 1. Giai đoạn Người chơi bay bổng lên cao ngưng tụ ma hỏa (Ticks 0 -> 25)
                if (hf.ticksAlive < 26) {
                    if (hf.caster != null && hf.caster.isAlive() && hf.caster.level() == level) {
                        if (hf.ticksAlive < 20) {
                            hf.caster.setDeltaMovement(0, 0.22D, 0);
                        } else {
                            hf.caster.setDeltaMovement(0, 0.01D, 0);
                        }
                        hf.caster.fallDistance = 0.0F;
                        hf.caster.hurtMarked = true;
                    }
                }

                // 2. Giai đoạn Phóng Quả Cầu Lửa Đen (Tick 28)
                if (hf.ticksAlive == 28) {
                    level.playSound(null, hf.startPos.x, hf.startPos.y, hf.startPos.z,
                            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 3.5F, 0.6F);
                    level.playSound(null, hf.startPos.x, hf.startPos.y, hf.startPos.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 1.6F);
                }

                // 3. Quả cầu va chạm & Hình thành hộp nhỏ 1m rồi bành trướng (Tick 38)
                if (hf.ticksAlive == 38) {
                    Vec3 center = hf.targetPos;
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 0.75F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.1F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 3.0F, 0.8F);

                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 50, 1.0D, 1.0D, 1.0D, 0.15D);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 40, 1.2D, 1.2D, 1.2D, 0.08D);
                    level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 60, 1.5D, 1.5D, 1.5D, 0.1D);
                }

                // 4. Hắc Viêm bùng nổ cực đại tại Tick 55
                if (hf.ticksAlive == 55) {
                    Vec3 center = hf.targetPos;
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0F, 0.7F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.5F, 0.85F);

                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 4, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 80, 2.0D, 2.0D, 2.0D, 0.15D);

                    // Sát thương bộc phát ban đầu cho toàn bộ mục tiêu bên trong
                    dealPrisonDamage(level, hf, 40.0F);
                }

                // =====================================================================
                // 5. DUY TRÌ HỎA NGỤC HẮC VIÊM VĨNH CỬU (Ticks >= 55)
                // Thiêu đốt liên tục mỗi 10 ticks (0.5s), kéo giam mục tiêu vào tâm ngục
                // KHÔNG BAO GIỜ TỰ ĐỘNG BIẾN MẤT trừ khi dùng Phong Sát Kĩ Năng!
                // =====================================================================
                if (hf.ticksAlive >= 55) {
                    if (hf.ticksAlive % 10 == 0) {
                        dealPrisonDamage(level, hf, 8.0F); // 16 sát thương mỗi giây
                    }

                    if (hf.ticksAlive % 40 == 0) {
                        Vec3 center = hf.targetPos;
                        level.playSound(null, center.x, center.y, center.z,
                                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 2.5F, 0.8F);
                    }
                }
            }
        }
    }

    /**
     * Gây sát thương và giam cầm toàn bộ mục tiêu bên trong hộp (LOẠI TRỪ NGƯỜI CHƠI)
     */
    private static void dealPrisonDamage(ServerLevel level, ActiveDarkFlamePrison hf, float damage) {
        Vec3 center = hf.targetPos;
        float R = hf.prisonRadius;

        AABB box = new AABB(center.x - R, center.y - R, center.z - R,
                            center.x + R, center.y + R, center.z + R);

        List<LivingEntity> trapped = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);

        for (LivingEntity victim : trapped) {
            // TUYỆT ĐỐI KHÔNG GÂY HẠI CHO NGƯỜI THI TRIỂN
            if (victim == hf.caster) continue;

            // Miễn nhiễm với các Boss Thần Thoại tương ứng
            if (victim instanceof VelgryndEntity || victim instanceof VelzardEntity) continue;
            if (victim instanceof PrimordialDemonEntity demon) {
                DemonType type = demon.getDemonType();
                if (type == DemonType.ROUGE || type == DemonType.BLANC || type == DemonType.NOIR) continue;
            }
            if (victim instanceof MilimEntity || victim instanceof MarbasEntity) continue;
            if (victim instanceof CuuTheQuyenHienEntity) {
                victim.hurt(level.damageSources().magic(), damage * 0.35F);
                continue;
            }

            // Gây sát thương ma pháp xuyên giáp
            victim.hurt(level.damageSources().magic(), damage);
            victim.setRemainingFireTicks(140);

            // Khóa chặt: Lực kéo chân không không gian về tâm ngục nếu mục tiêu cố bỏ chạy
            if (victim.position().distanceToSqr(center) > 2.5D * 2.5D) {
                Vec3 pull = center.subtract(victim.position()).normalize().scale(0.65D);
                victim.setDeltaMovement(pull.x, 0.08D, pull.z);
                victim.hasImpulse = true;
            }
        }
    }

    /**
     * Giải trừ toàn bộ Dark Flame Prison khi thi triển PHONG SÁT KĨ NĂNG
     * Kích hoạt Sụp Đổ Chân Không Cực Hạn (Final Vacuum Implosion) 70 sát thương
     */
    public static void cancelAll(ServerLevel level) {
        if (level == null || ACTIVE_PRISONS.isEmpty()) return;

        synchronized (ACTIVE_PRISONS) {
            Iterator<ActiveDarkFlamePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActiveDarkFlamePrison hf = it.next();
                if (hf.level == level) {
                    Vec3 center = hf.targetPos;

                    // Âm thanh và hạt sụp đổ không gian
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 4.0F, 0.5F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.4F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.7F);

                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 3, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 2, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 4, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 50, 1.5D, 1.5D, 1.5D, 0.1D);

                    // Sát thương sụp đổ chân không ma pháp 70.0F
                    dealPrisonDamage(level, hf, 70.0F);

                    // Gửi Packet PHASE_DISMISS để client render hiệu ứng co rút và dọn dẹp
                    ModMessages.sendToPlayers(level.players(),
                            new ClientboundHellflarePacket(hf.casterUuid, hf.startPos, hf.targetPos, hf.prisonRadius, ClientboundHellflarePacket.PHASE_DISMISS));

                    it.remove();
                }
            }
        }
    }

    private static void cancelCaster(ServerLevel level, UUID casterUuid) {
        synchronized (ACTIVE_PRISONS) {
            ACTIVE_PRISONS.removeIf(hf -> hf.level == level && hf.casterUuid.equals(casterUuid));
        }
    }
}
