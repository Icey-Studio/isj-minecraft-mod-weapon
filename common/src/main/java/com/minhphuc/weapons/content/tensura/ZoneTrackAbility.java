package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tuyệt Kỹ Ma Vương: Granit Xuyên Phá (Zone Track)
 * - Năng lượng đại pháo Granit màu trắng tinh khiết / bạch kim phát quang.
 * - Phạm vi: 25 - 26 block.
 * - Sát thương theo khoảng cách:
 *   + 1 - 5 block: 100% sát thương (120 HP ~ 60 tim).
 *   + 6 - 20 block: 50% sát thương (60 HP ~ 30 tim).
 *   + 21 - 26 block: 6% sát thương (~7.2 HP ~ 3.6 tim).
 * - Phá nát khối cản đường, tạo hiệu ứng mảnh vụn khối văng tung tóe.
 * - Gây cháy và kích hoạt vụ nổ hất văng mục tiêu cực xa như cú đấm của Milim.
 * - KHÔNG CÓ THỜI GIAN HỒI CHIÊU: Có thể xả đạn liên thanh liên tục!
 */
public class ZoneTrackAbility {

    public static void cast(ServerLevel level, ServerPlayer player) {
        if (!PrimordialPlayerDataHelper.isDemonLord(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Bạn phải là Chân Ma Vương để thi triển Granit Xuyên Phá (Zone Track)!"),
                    true
            );
            return;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 dir = player.getLookAngle().normalize();
        double maxRange = 26.0D;

        // 1. Khởi tạo chùm sáng 3D đa tầng màu trắng tuyết bạch kim cực kỳ tráng lệ
        HorizontalHolyBeamAbility.spawn3DBeamDisplay(
                level, eyePos, dir, maxRange,
                2.0F, 0xFFFFFF,
                3.4F, 0xF0F8FF,
                12
        );

        // Âm thanh uy lực đại pháo nổ rung trời
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.7F);
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 1.2F);
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.8F);

        // 2. Phá hủy khối địa hình và tạo hiệu ứng mảnh vỡ khối (Block Debris)
        Set<BlockPos> brokenBlocks = new HashSet<>();
        for (double d = 1.0; d <= maxRange; d += 0.8) {
            Vec3 pt = eyePos.add(dir.scale(d));

            // Hiệu ứng hạt năng lượng trắng tuyết phát sáng dọc tia bắn
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 2, 0.2, 0.2, 0.2, 0.05);
            level.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);

            int cx = (int) Math.floor(pt.x);
            int cy = (int) Math.floor(pt.y);
            int cz = (int) Math.floor(pt.z);

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos bp = new BlockPos(cx + ox, cy + oy, cz + oz);
                        if (brokenBlocks.contains(bp)) continue;

                        double distToRaySq = pt.distanceToSqr(Vec3.atCenterOf(bp));
                        if (distToRaySq <= 2.8) {
                            brokenBlocks.add(bp);
                            BlockState state = level.getBlockState(bp);

                            // Không phá bedrock hoặc khối kết giới đặc biệt
                            if (state.getDestroySpeed(level, bp) >= 0 &&
                                !state.is(ModBlocks.DRAGON_PRISON_BARRIER.get()) &&
                                !state.is(ModBlocks.ANTI_MAGIC_BARRIER.get()) &&
                                !state.is(ModBlocks.MULTILAYER_BARRIER.get()) &&
                                !DomainBarrierBlock.isAnyDomainBarrier(state.getBlock())) {

                                // Hiệu ứng văng mảnh khối
                                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                        bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5,
                                        8, 0.3, 0.3, 0.3, 0.15);

                                level.destroyBlock(bp, true, player);

                                // Cơ hội để lại lửa thiêu đốt mặt đất
                                if (level.random.nextFloat() <= 0.25F && level.getBlockState(bp).isAir() && level.getBlockState(bp.below()).blocksMotion()) {
                                    level.setBlock(bp, Blocks.FIRE.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quét thực thể và áp dụng sát thương 3 bậc khoảng cách + Hất văng kiểu Milim
        AABB scanBox = new AABB(eyePos, eyePos.add(dir.scale(maxRange))).inflate(3.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, scanBox, e -> e.isAlive() && e != player);

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
            Vec3 toTarget = targetCenter.subtract(eyePos);
            double proj = toTarget.dot(dir);

            if (proj < 0.5 || proj > maxRange) continue;

            double perpDist = toTarget.subtract(dir.scale(proj)).length();
            if (perpDist > 2.8) continue;

            // Tính toán sát thương theo khoảng cách:
            // 1 - 5 block: 100% (120 HP)
            // 6 - 20 block: 50% (60 HP)
            // 21 - 26 block: 6% (7.2 HP)
            float damageFactor;
            if (proj <= 5.0) {
                damageFactor = 1.0F;
            } else if (proj <= 20.0) {
                damageFactor = 0.5F;
            } else {
                damageFactor = 0.06F;
            }
            float finalDamage = 120.0F * damageFactor;

            // Phá vỡ khiên nếu đối thủ đang giơ khiên đỡ
            if (target instanceof Player pTarget && pTarget.isBlocking()) {
                pTarget.disableShield();
            }

            // Gây sát thương uy lực
            target.hurt(level.damageSources().playerAttack(player), finalDamage);

            // Gây cháy thiêu đốt 8 giây
            target.setRemainingFireTicks(160);

            // 4. Hất văng mục tiêu bay cực xa lên trời như Cú Đấm Milim (Knockback siêu mạnh)
            Vec3 knockback = new Vec3(dir.x * 4.2D, 1.35D, dir.z * 4.2D);
            target.setDeltaMovement(knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
            if (target instanceof ServerPlayer tsp) {
                tsp.connection.send(new ClientboundSetEntityMotionPacket(target));
            }

            // 5. Hiệu ứng bộc phá nổ tung và khói dày đặc mù mịt tại vị trí trúng đòn
            double hitX = target.getX();
            double hitY = targetCenter.y;
            double hitZ = target.getZ();

            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitX, hitY, hitZ, 2, 0.2, 0.2, 0.2, 0);
            level.sendParticles(ParticleTypes.SONIC_BOOM, hitX, hitY, hitZ, 2, 0.1, 0.1, 0.1, 0);
            level.sendParticles(ParticleTypes.FLASH, hitX, hitY, hitZ, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hitX, hitY, hitZ, 30, 1.2, 1.2, 1.2, 0.08);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, hitX, hitY, hitZ, 20, 1.0, 1.0, 1.0, 0.1);
        }

        player.displayClientMessage(
                Component.literal("§f§l⚡ [ZONE TRACK] §bGranit Xuyên Phá Bạch Kim đã bắn ra!"),
                true
        );
    }
}
