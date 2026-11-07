package com.minhphuc.weapons.content.space;

import com.minhphuc.weapons.client.renderer.ClientOrbitalStrikeRenderer;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ClientboundOrbitalStrikePacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * QUẢN LÝ CÁC ĐÒN TẤN CÔNG QUỸ ĐẠO VŨ TRỤ (THE SHOOTING STAR - STELLAR REMOTE)
 * Đảm bảo 100% ZERO BLOCK DESTRUCTION (Không phá khối địa hình, giữ FPS mượt mà)
 */
public class OrbitalStrikeManager {

    public static class ActiveStrike {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 targetPos;
        public final int mode;
        public int currentTick = 0;
        public final int maxTicks;

        public ActiveStrike(ServerLevel level, ServerPlayer caster, Vec3 targetPos, int mode) {
            this.level = level;
            this.caster = caster;
            this.targetPos = targetPos;
            this.mode = mode;
            this.maxTicks = switch (mode) {
                case ClientOrbitalStrikeRenderer.MODE_RAILGUN -> 60;
                case ClientOrbitalStrikeRenderer.MODE_GUNGNIR -> 70;
                case ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS -> 100;
                default -> 60;
            };
        }
    }

    public static final List<ActiveStrike> ACTIVE_STRIKES = Collections.synchronizedList(new ArrayList<>());

    public static void triggerStrike(ServerLevel level, ServerPlayer caster, Vec3 targetPos, int mode) {
        if (level == null || caster == null) return;

        ActiveStrike strike = new ActiveStrike(level, caster, targetPos, mode);
        ACTIVE_STRIKES.add(strike);

        // Phát packet đồ họa tới người chơi và tất cả người chơi trong phạm vi 192m
        ClientboundOrbitalStrikePacket packet = new ClientboundOrbitalStrikePacket(targetPos, mode, ClientboundOrbitalStrikePacket.PHASE_START);
        ModMessages.sendToPlayer(packet, caster);
        for (ServerPlayer p : level.players()) {
            if (p != caster && p.position().distanceToSqr(targetPos) <= 192 * 192) {
                ModMessages.sendToPlayer(packet, p);
            }
        }

        // Âm thanh kích hoạt bộ điều khiển vệ tinh
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 3.0F, 1.2F);

        // Thông báo chỉ huy vệ tinh
        Component msg = switch (mode) {
            case ClientOrbitalStrikeRenderer.MODE_RAILGUN ->
                    Component.literal(String.format("§b[SS-01 · RAILGUN] §fĐã khóa tọa độ [%.0f, %.0f, %.0f]! Vệ tinh quỹ đạo đang khai hỏa...", targetPos.x, targetPos.y, targetPos.z));
            case ClientOrbitalStrikeRenderer.MODE_GUNGNIR ->
                    Component.literal(String.format("§c[SS-03 · GUNGNIR] §fKhóa quỹ đạo Sao Mộc! Thần Thương Gungnir đang xuyên tầng khí quyển...", targetPos.x, targetPos.y, targetPos.z));
            case ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS ->
                    Component.literal(String.format("§5[SS-04 · THE SEVEN STARS] §fĐồng bộ chòm sao Bắc Đẩu: 7 mũi giáng thế công kích mục tiêu!", targetPos.x, targetPos.y, targetPos.z));
            default -> Component.literal("§e[ORBITAL] Đang khai hỏa...");
        };
        caster.displayClientMessage(msg, true);
    }

    public static void tickStrikes(ServerLevel level) {
        if (ACTIVE_STRIKES.isEmpty()) return;

        synchronized (ACTIVE_STRIKES) {
            Iterator<ActiveStrike> iterator = ACTIVE_STRIKES.iterator();
            while (iterator.hasNext()) {
                ActiveStrike s = iterator.next();
                if (s.level != level) continue;

                s.currentTick++;

                // ==========================================================
                // 1. SS-01 · RAILGUN
                // ==========================================================
                if (s.mode == ClientOrbitalStrikeRenderer.MODE_RAILGUN) {
                    if (s.currentTick < 20) {
                        // Tia ngắm sơ bộ
                        s.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                s.targetPos.x, s.targetPos.y + 0.5, s.targetPos.z,
                                4, 0.2, 0.5, 0.2, 0.05);
                        s.level.sendParticles(ParticleTypes.END_ROD,
                                s.targetPos.x, s.targetPos.y + 1.0, s.targetPos.z,
                                2, 0.1, 1.0, 0.1, 0.02);
                    } else if (s.currentTick == 20) {
                        // BÙNG NỔ CỘT PLASMA RAILGUN
                        s.level.playSound(null, s.targetPos.x, s.targetPos.y, s.targetPos.z,
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 16.0F, 0.7F);
                        s.level.playSound(null, s.targetPos.x, s.targetPos.y, s.targetPos.z,
                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 12.0F, 0.5F);

                        // Sát thương 2,500 ma pháp trong bán kính 22m (ZERO BLOCK DESTRUCTION)
                        applyRadialDamage(s.level, s.caster, s.targetPos, 22.0, 2500.0F);

                        // Hạt xung kích
                        for (int i = 0; i < 16; i++) {
                            double angle = Math.toRadians(i * 22.5);
                            s.level.sendParticles(ParticleTypes.SONIC_BOOM,
                                    s.targetPos.x + Math.cos(angle) * 8.0, s.targetPos.y + 1.0, s.targetPos.z + Math.sin(angle) * 8.0,
                                    1, 0, 0, 0, 0);
                        }
                        s.level.sendParticles(ParticleTypes.FLASH, s.targetPos.x, s.targetPos.y + 2.0, s.targetPos.z, 2, 0, 0, 0, 0);
                    }
                }

                // ==========================================================
                // 2. SS-03 · GUNGNIR
                // ==========================================================
                else if (s.mode == ClientOrbitalStrikeRenderer.MODE_GUNGNIR) {
                    if (s.currentTick < 25) {
                        // Quỹ đạo thần thương lao xuống
                        double t = s.currentTick / 25.0;
                        double curX = s.targetPos.x - 35.0 * (1.0 - t);
                        double curY = s.targetPos.y + 200.0 * (1.0 - t);
                        double curZ = s.targetPos.z - 35.0 * (1.0 - t);

                        s.level.sendParticles(ParticleTypes.FLAME, curX, curY, curZ, 8, 0.3, 0.3, 0.3, 0.05);
                        s.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, curX, curY, curZ, 4, 0.2, 0.2, 0.2, 0.05);
                    } else if (s.currentTick == 25) {
                        // THẦN THƯƠNG CẮM XUỐNG ĐẤT
                        s.level.playSound(null, s.targetPos.x, s.targetPos.y, s.targetPos.z,
                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 16.0F, 0.4F);
                        s.level.playSound(null, s.targetPos.x, s.targetPos.y, s.targetPos.z,
                                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 12.0F, 0.5F);

                        // Sát thương 3,000 ma pháp trong bán kính 26m
                        applyRadialDamage(s.level, s.caster, s.targetPos, 26.0, 3000.0F);

                        // Sóng chấn động hạt
                        for (int i = 0; i < 24; i++) {
                            double angle = Math.toRadians(i * 15.0);
                            s.level.sendParticles(ParticleTypes.LAVA,
                                    s.targetPos.x + Math.cos(angle) * 12.0, s.targetPos.y + 0.5, s.targetPos.z + Math.sin(angle) * 12.0,
                                    3, 0.2, 0.3, 0.2, 0.1);
                        }
                        s.level.sendParticles(ParticleTypes.FLASH, s.targetPos.x, s.targetPos.y + 1.5, s.targetPos.z, 3, 0, 0, 0, 0);
                    }
                }

                // ==========================================================
                // 3. SS-04 · THE SEVEN STARS (BẮC ĐẨU THẤT TINH)
                // ==========================================================
                else if (s.mode == ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS) {
                    for (int i = 0; i < 7; i++) {
                        int starTick = 10 + i * 8;
                        if (s.currentTick == starTick) {
                            double sx = s.targetPos.x + ClientOrbitalStrikeRenderer.BIG_DIPPER_OFFSETS[i][0];
                            double sz = s.targetPos.z + ClientOrbitalStrikeRenderer.BIG_DIPPER_OFFSETS[i][1];
                            Vec3 starPos = new Vec3(sx, s.targetPos.y, sz);

                            s.level.playSound(null, sx, s.targetPos.y, sz,
                                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 8.0F, 0.9F);
                            s.level.playSound(null, sx, s.targetPos.y, sz,
                                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 6.0F, 1.2F);

                            float damage = (i == 6) ? 2500.0F : 900.0F; // Ngôi sao thứ 7 (Alkaid) bạo kích cực đại
                            double rad = (i == 6) ? 18.0 : 10.0;
                            applyRadialDamage(s.level, s.caster, starPos, rad, damage);

                            s.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sx, s.targetPos.y + 0.5, sz, 1, 0, 0, 0, 0);
                            s.level.sendParticles(ParticleTypes.FLASH, sx, s.targetPos.y + 1.0, sz, 1, 0, 0, 0, 0);
                            s.level.sendParticles(ParticleTypes.DRAGON_BREATH, sx, s.targetPos.y + 0.5, sz, 15, 1.0, 0.5, 1.0, 0.1);
                        }
                    }
                }

                // Kết thúc Strike
                if (s.currentTick >= s.maxTicks) {
                    ClientboundOrbitalStrikePacket endPacket = new ClientboundOrbitalStrikePacket(s.targetPos, s.mode, ClientboundOrbitalStrikePacket.PHASE_END);
                    ModMessages.sendToPlayer(endPacket, s.caster);
                    for (ServerPlayer p : s.level.players()) {
                        if (p != s.caster && p.position().distanceToSqr(s.targetPos) <= 192 * 192) {
                            ModMessages.sendToPlayer(endPacket, p);
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    private static void applyRadialDamage(ServerLevel level, ServerPlayer caster, Vec3 center, double radius, float damage) {
        AABB zone = new AABB(center.x - radius, center.y - 15, center.z - radius,
                center.x + radius, center.y + 25, center.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, zone,
                e -> e.isAlive() && e != caster && !isAllied(caster, e));

        for (LivingEntity target : targets) {
            target.removeAllEffects();
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), damage);

            if (!target.isAlive()) {
                level.sendParticles(ParticleTypes.SOUL,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        12, 0.3, 0.5, 0.3, 0.05);
                target.spawnAtLocation(new ItemStack(ModItems.MOB_SOUL.get()));
            }
        }
    }

    public static void cleanupLevel(ServerLevel level) {
        synchronized (ACTIVE_STRIKES) {
            ACTIVE_STRIKES.removeIf(s -> s.level == level);
        }
    }

    private static boolean isAllied(ServerPlayer caster, LivingEntity entity) {
        return entity.isAlliedTo(caster) || entity.getUUID().equals(caster.getUUID());
    }
}
