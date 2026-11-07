package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ClientboundSupernovaPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TUYỆT KỸ THẦN CẤP: SIÊU TÂN TINH (SUPERNOVA)
 * Tái hiện 100% cơ chế Lỗ Đen & Siêu Tân Tinh từ God Wand 1.20.1:
 * - Giai đoạn 1 (0 -> 340 ticks): Vết nứt vũ trụ mở ra ở độ cao +20m, hình thành Lỗ Đen và Đĩa Bồi Tụ Plasma xoay tròn.
 *   Lực hút chân không kéo toàn bộ sinh vật trong 55m bay xoáy vào tâm lỗ đen.
 * - Giai đoạn 2 (Tick 340): Đại Vụ Nổ Siêu Tân Tinh chói lòa, sóng chấn động 96m giáng 2,500 sát thương ma pháp.
 * - TỐI ƯU HOÁ: 100% ZERO BLOCK DESTRUCTION - Không phá hủy bất kỳ khối đất nào, không sinh entity rác, FPS mượt mà!
 */
public class SupernovaAbility {

    public static class ActiveSupernova {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 corePos;
        public int currentTick = 0;
        public final int maxTicks = 380; // 19.0 giây

        public ActiveSupernova(ServerLevel level, ServerPlayer caster, Vec3 corePos) {
            this.level = level;
            this.caster = caster;
            this.corePos = corePos;
        }
    }

    public static final List<ActiveSupernova> ACTIVE_SUPERNOVAS = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer caster) {
        if (caster == null || level == null) return;

        // Xác định tâm lỗ đen ở phía trước người chơi 18 block, nâng cao +20m
        Vec3 forward = caster.getLookAngle();
        Vec3 groundTarget = caster.position().add(forward.scale(18.0));
        Vec3 corePos = groundTarget.add(0, 20.0, 0);

        ActiveSupernova supernova = new ActiveSupernova(level, caster, corePos);
        ACTIVE_SUPERNOVAS.add(supernova);

        // 1. Gửi packet khởi động bộ Render Đĩa Bồi Tụ GPU cho tất cả người chơi trong 128m
        ClientboundSupernovaPacket packet = new ClientboundSupernovaPacket(corePos, ClientboundSupernovaPacket.PHASE_START);
        ModMessages.sendToPlayer(packet, caster);
        for (ServerPlayer nearbyPlayer : level.players()) {
            if (nearbyPlayer != caster && nearbyPlayer.position().distanceToSqr(corePos) <= 128 * 128) {
                ModMessages.sendToPlayer(packet, nearbyPlayer);
            }
        }

        // 2. Âm thanh mở cổng không gian
        level.playSound(null, corePos.x, corePos.y, corePos.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 4.0F, 0.7F);
        level.playSound(null, corePos.x, corePos.y, corePos.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 0.5F);

        caster.displayClientMessage(
                Component.literal("§6§l[THẦN THOẠI] §eKhai mở Cấm Thuật Vũ Trụ: Siêu Tân Tinh (Supernova)!"),
                true
        );

        for (ServerPlayer p : level.players()) {
            if (p != caster && p.position().distanceToSqr(corePos) <= 96 * 96) {
                p.displayClientMessage(
                        Component.literal("§6§l[CẢNH BÁO KHÔNG GIAN] §eLỗ đen siêu trọng lực đang ngưng tụ! Mau rời khỏi bán kính 96m!"),
                        false
                );
            }
        }
    }

    public static void tickSupernovas(ServerLevel level) {
        if (ACTIVE_SUPERNOVAS.isEmpty()) return;

        synchronized (ACTIVE_SUPERNOVAS) {
            Iterator<ActiveSupernova> iterator = ACTIVE_SUPERNOVAS.iterator();
            while (iterator.hasNext()) {
                ActiveSupernova s = iterator.next();
                if (s.level != level) continue;

                if (s.caster == null || !s.caster.isAlive() || s.caster.hasDisconnected()) {
                    iterator.remove();
                    continue;
                }

                s.currentTick++;

                // ==============================================================
                // GIAI ĐOẠN 1 (0 -> 339 ticks): LỖ ĐEN & LỰC HÚT CHÂN KHÔNG
                // ==============================================================
                if (s.currentTick < 340) {
                    double progress = s.currentTick / 340.0;

                    // Hút các thực thể sống trong bán kính 55m xoáy vào tâm lỗ đen
                    AABB suctionZone = new AABB(s.corePos.x - 55, s.corePos.y - 25, s.corePos.z - 55,
                            s.corePos.x + 55, s.corePos.y + 35, s.corePos.z + 55);
                    List<LivingEntity> targets = s.level.getEntitiesOfClass(LivingEntity.class, suctionZone,
                            e -> e.isAlive() && e != s.caster && !isAlliedToCaster(s.caster, e));

                    for (LivingEntity target : targets) {
                        Vec3 towardCore = s.corePos.subtract(target.position());
                        double dist = towardCore.length();
                        if (dist > 1.2) {
                            double speed = Math.min(dist * 0.16, 0.8 + progress * 2.2);
                            Vec3 pull = towardCore.normalize().scale(speed);
                            target.setDeltaMovement(target.getDeltaMovement().lerp(pull, 0.4));
                            target.hurtMarked = true;
                        }
                    }

                    // Hạt xoắn ốc bồi tụ vào tâm lỗ đen
                    if (s.currentTick % 2 == 0) {
                        for (int i = 0; i < 6; i++) {
                            double angle = Math.toRadians((i * 60.0) + (s.currentTick * 14.0));
                            double r = 8.0 - (progress * 5.0);
                            double px = s.corePos.x + Math.cos(angle) * r;
                            double pz = s.corePos.z + Math.sin(angle) * r;
                            s.level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, s.corePos.y, pz, 1, 0.1, 0.1, 0.1, 0.05);
                            s.level.sendParticles(ParticleTypes.DRAGON_BREATH, px, s.corePos.y, pz, 1, 0.05, 0.05, 0.05, 0.02);
                        }
                    }
                }

                // ==============================================================
                // GIAI ĐOẠN 2 (Tick 340): SIÊU TÂN TINH BỘC PHÁT (DETONATION)
                // ==============================================================
                else if (s.currentTick == 340) {
                    // Âm thanh chấn động vũ trụ
                    s.level.playSound(null, s.corePos.x, s.corePos.y, s.corePos.z,
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 12.0F, 0.6F);
                    s.level.playSound(null, s.corePos.x, s.corePos.y, s.corePos.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 10.0F, 0.5F);

                    // Quét sát thương hủy diệt diện rộng 96m (KHÔNG PHÁ KHỐI)
                    AABB blastZone = new AABB(s.corePos.x - 96, s.corePos.y - 45, s.corePos.z - 96,
                            s.corePos.x + 96, s.corePos.y + 65, s.corePos.z + 96);
                    List<LivingEntity> blastTargets = s.level.getEntitiesOfClass(LivingEntity.class, blastZone,
                            e -> e.isAlive() && e != s.caster && !isAlliedToCaster(s.caster, e));

                    for (LivingEntity target : blastTargets) {
                        target.removeAllEffects();
                        target.invulnerableTime = 0;

                        // 2,500 Sát thương ma pháp Siêu Tân Tinh
                        target.hurt(s.level.damageSources().magic(), 2500.0F);

                        if (!target.isAlive()) {
                            s.level.sendParticles(ParticleTypes.SOUL,
                                    target.getX(), target.getY() + 0.5, target.getZ(),
                                    15, 0.4, 0.6, 0.4, 0.05);
                            target.spawnAtLocation(new ItemStack(ModItems.MOB_SOUL.get()));
                        }
                    }

                    // 8 vòng sóng xung kích hạt bán kính mở rộng 96m
                    for (int ring = 1; ring <= 8; ring++) {
                        double radius = ring * 12.0;
                        for (int i = 0; i < 32; i++) {
                            double angle = Math.toRadians(i * 11.25);
                            double px = s.corePos.x + Math.cos(angle) * radius;
                            double pz = s.corePos.z + Math.sin(angle) * radius;
                            s.level.sendParticles(ParticleTypes.SONIC_BOOM, px, s.corePos.y - 15.0, pz, 1, 0, 0, 0, 0);
                            s.level.sendParticles(ParticleTypes.FLASH, px, s.corePos.y, pz, 1, 0, 0, 0, 0);
                        }
                    }
                }

                // Kết thúc ở tick 380
                if (s.currentTick >= s.maxTicks) {
                    ClientboundSupernovaPacket packet = new ClientboundSupernovaPacket(s.corePos, ClientboundSupernovaPacket.PHASE_END);
                    ModMessages.sendToPlayer(packet, s.caster);
                    for (ServerPlayer nearbyPlayer : s.level.players()) {
                        if (nearbyPlayer != s.caster && nearbyPlayer.position().distanceToSqr(s.corePos) <= 128 * 128) {
                            ModMessages.sendToPlayer(packet, nearbyPlayer);
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    public static void cleanupLevel(ServerLevel level) {
        synchronized (ACTIVE_SUPERNOVAS) {
            ACTIVE_SUPERNOVAS.removeIf(s -> s.level == level);
        }
    }

    private static boolean isAlliedToCaster(ServerPlayer caster, LivingEntity entity) {
        return entity.isAlliedTo(caster) || entity.getUUID().equals(caster.getUUID());
    }
}
