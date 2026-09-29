package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Kỹ Năng Tối Thượng Thần Cấp: Long Tinh Bộc Viêm Bá - Dragon Nova (Drago-Nova / 竜星爆炎覇)
 * Tái hiện 100% uy lực, hiệu ứng và sát thương hủy diệt của Ma Vương Cổ Đại Milim Nava:
 * - Điều kiện: Chỉ cần mặc Áo Giáp Thần Thoại (Divine Armor), KHÔNG CẦN HỒI NĂNG LƯỢNG / MA LỰC.
 * - Giai đoạn 1 (0 -> 29 ticks): Tụ Ma Tố & Hội Tụ Hàng Vạn Linh Tử (Spiritrons) từ không gian bay vào ngực,
 *   Địa Ma Trận phát quang tím thẫm (0x9400D3) xoay phóng đại dưới chân.
 * - Giai đoạn 2 (30 -> 54 ticks): Khai hỏa Đại Pháo 3D Đa Tầng Cực Quang 75m (HorizontalHolyBeam 3D),
 *   PHÁ HỦY ĐỊA HÌNH KHOÉT RÃNH HẦM BÁN KÍNH 4 BLOCK, BIỂN LỬA 7 BLOCK,
 *   Xóa sạch mọi Buff Kháng & Hồi Máu, hút mục tiêu và giáng 2,000 sát thương ma pháp mỗi 2 ticks!
 * - Giai đoạn 3 (55 -> 75 ticks): Đại Vụ Nổ Siêu Tân Tinh (Grand Supernova Crater):
 *   Hố bom khổng lồ bán kính 10 blocks, biển lửa 16m, Cột Năng Lượng Tinh Trần chọc trời 50m,
 *   8 Vòng Sóng Xung Kích 28m, 16 Tia Sét Ma Thuật,
 *   Diệt thẳng Velgrynd (10,000 Damage), rút 90% HP Boss kèm phế sạch hiệu ứng, xóa sổ 1-hit quái thường!
 */
public class DragonNovaAbility {

    public static class ActiveDragonNova {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public int currentTick = 0;
        public final int totalTicks = 75;
        public Vec3 lastHitPos = null;

        // Ma Pháp Trận Tím 3D Dưới Chân
        public Display.ItemDisplay groundCircle = null;

        public ActiveDragonNova(ServerLevel level, ServerPlayer caster) {
            this.level = level;
            this.caster = caster;
        }

        public void cleanupDisplays() {
            if (groundCircle != null && groundCircle.isAlive()) {
                groundCircle.discard();
                groundCircle = null;
            }
        }
    }

    public static final List<ActiveDragonNova> ACTIVE_NOVAS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    // Hệ thống hạt bụi năng lượng cao cấp chuẩn Milim Nava
    private static final DustParticleOptions SPIRITRON_PINK = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.6F), 2.2F);
    private static final DustParticleOptions SPIRITRON_PURPLE = new DustParticleOptions(new Vector3f(0.6F, 0.0F, 1.0F), 2.0F);
    private static final DustParticleOptions NEON_MAGENTA_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.02F, 0.55F), 2.0F);
    private static final DustParticleOptions DEEP_PURPLE_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.0F, 0.95F), 1.8F);
    private static final DustParticleOptions ELECTRIC_CYAN_DUST = new DustParticleOptions(new Vector3f(0.0F, 0.95F, 1.0F), 1.8F);
    private static final DustParticleOptions STARLIGHT_WHITE_DUST = new DustParticleOptions(new Vector3f(0.95F, 1.0F, 1.0F), 2.2F);

    /**
     * Kiểm tra điều kiện mở khóa: Chỉ cần mặc Áo Giáp Thần Thoại (Divine Armor)
     * Không cần thức tỉnh Chân Ma Vương, không cần hồi năng lượng / ma lực
     */
    public static boolean canCast(ServerPlayer player) {
        if (player == null) return false;
        return DivineArmorItem.isWearingAnyPiece(player);
    }

    /**
     * Kích hoạt Long Tinh Bộc Viêm Bá
     */
    public static boolean cast(ServerLevel level, ServerPlayer player) {
        if (!canCast(player)) {
            player.displayClientMessage(
                Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Yêu cầu trang bị Áo Giáp Thần Thoại để khai mở Long Tinh Bộc Viêm Bá!"),
                true
            );
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.5F);
            return false;
        }

        // Chống đè hoạt cảnh nếu đang trong quá trình tụ/bắn
        if (ACTIVE_NOVAS.stream().anyMatch(n -> n.caster == player)) {
            player.displayClientMessage(
                Component.literal("§d§l[LONG TINH BỘC VIÊM BÁ] §eĐang trong quá trình thi triển, vui lòng chờ đòn đánh hoàn tất!"),
                true
            );
            return false;
        }

        // KHÔNG CẦN HỒI NĂNG LƯỢNG / KHÔNG CẦN HỒI CHIÊU: Có thể dùng liên tục khi hoàn tất mỗi lượt!

        ActiveDragonNova nova = new ActiveDragonNova(level, player);

        // 1. Tạo Ma Pháp Trận Tím Thẫm dưới chân
        Vec3 groundPos = player.position().add(0, 0.05D, 0);
        nova.groundCircle = createMagicCircleDisplay(level, groundPos, ModItems.BEELZEBUTH_MAGIC_CIRCLE.get(), 3.0F, 0x9400D3);

        ACTIVE_NOVAS.add(nova);

        // 2. Gửi Title chuẩn Kanji & Latin
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§d§l竜星爆炎覇")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6§l✦ LONG TINH BỘC VIÊM BÁ - DRAGO-NOVA ✦")));
        }

        // 3. Thông báo cảnh báo diện rộng
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(48.0D))) {
            p.displayClientMessage(
                Component.literal("§d§l[LONG TINH BỘC VIÊM BÁ] §c§lĐang ngưng tụ Long Tinh Bộc Viêm Bá! Tinh tử hội tụ, vạn vật quy phục (Tuyệt đối không thể ngăn cản)!"),
                false
            );
        }

        // 4. Âm thanh mở màn rền vang vũ trụ
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.5F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 4.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.5F, 0.7F);

        // 5. Trạng thái bất tử và lơ lửng khi tụ chiêu
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 85, 4, false, false));

        return true;
    }

    /**
     * Vòng lặp Server Tick cập nhật hoạt cảnh 3 giai đoạn siêu cấp giống hệt Milim Nava
     */
    public static void tickDragonNovas(ServerLevel level) {
        if (ACTIVE_NOVAS.isEmpty()) return;

        Iterator<ActiveDragonNova> iterator = ACTIVE_NOVAS.iterator();
        while (iterator.hasNext()) {
            ActiveDragonNova nova = iterator.next();

            if (nova.level != level) continue;

            if (nova.caster == null || !nova.caster.isAlive() || nova.caster.hasDisconnected()) {
                nova.cleanupDisplays();
                iterator.remove();
                continue;
            }

            ServerPlayer caster = nova.caster;
            int tick = nova.currentTick;

            // =========================================================================
            // GIAI ĐOẠN 1: TỤ MA TỐ & HỘI TỤ LINH TỬ (SPIRITRONS) (Ticks 0 -> 29: ~1.5 giây)
            // =========================================================================
            if (tick < 30) {
                float progress = (float) tick / 30.0F;

                // 1. Cập nhật xoay và phóng to Vòng Tròn Ma Thuật Tím Dưới Chân (3.0m -> 9.5m)
                if (nova.groundCircle != null && nova.groundCircle.isAlive()) {
                    Vec3 gPos = caster.position().add(0, 0.05D, 0);
                    nova.groundCircle.moveTo(gPos.x, gPos.y, gPos.z, 0.0F, 0.0F);
                    float groundScale = 3.0F + (progress * 6.5F);
                    Quaternionf gRot = new Quaternionf()
                            .rotateX((float) Math.toRadians(90.0F))
                            .rotateZ((float) Math.toRadians(tick * 7.5F));
                    updateDisplayTransformation(nova.groundCircle, groundScale, gRot);
                }

                // 2. HIỆU ỨNG TẬP HỢP CÁC LINH TỬ (SPIRITRONS GATHERING):
                // Hàng vạn đốm sáng linh tử tinh khiết từ quả cầu 7.5m không gian bay vút vào ngực người chơi
                Vec3 casterChest = caster.position().add(0, 1.2D, 0);
                for (int i = 0; i < 45; i++) {
                    double u = RANDOM.nextDouble();
                    double v = RANDOM.nextDouble();
                    double theta = u * 2.0D * Math.PI;
                    double phi = Math.acos(2.0D * v - 1.0D);
                    double r = 4.0D + RANDOM.nextDouble() * 3.5D;

                    double sx = casterChest.x + r * Math.sin(phi) * Math.cos(theta);
                    double sy = casterChest.y + r * Math.sin(phi) * Math.sin(theta);
                    double sz = casterChest.z + r * Math.cos(phi);

                    Vec3 spawnPos = new Vec3(sx, sy, sz);
                    Vec3 inwardVelocity = casterChest.subtract(spawnPos).normalize().scale(0.85D);

                    if (i % 3 == 0) {
                        level.sendParticles(SPIRITRON_PINK, spawnPos.x, spawnPos.y, spawnPos.z, 0,
                                inwardVelocity.x, inwardVelocity.y, inwardVelocity.z, 0.85D);
                    } else if (i % 3 == 1) {
                        level.sendParticles(SPIRITRON_PURPLE, spawnPos.x, spawnPos.y, spawnPos.z, 0,
                                inwardVelocity.x, inwardVelocity.y, inwardVelocity.z, 0.85D);
                    } else {
                        level.sendParticles(ELECTRIC_CYAN_DUST, spawnPos.x, spawnPos.y, spawnPos.z, 0,
                                inwardVelocity.x, inwardVelocity.y, inwardVelocity.z, 0.85D);
                    }
                }

                // 3. Hạt tụ lực xoay quanh người chơi cuốn xoáy dữ dội (chuẩn Milim)
                for (int i = 0; i < 4; i++) {
                    double angle = (tick * 18.0 + i * 90.0) * Math.PI / 180.0;
                    double rad = Math.max(0.6D, 3.2D * (1.0D - (tick / 35.0D)));
                    double px = caster.getX() + Math.cos(angle) * rad;
                    double pz = caster.getZ() + Math.sin(angle) * rad;
                    double py = caster.getY() + 1.2D + Math.sin(tick * 0.25 + i) * 0.35D;
                    level.sendParticles(SPIRITRON_PINK, px, py, pz, 1, 0, 0, 0, 0);
                    level.sendParticles(SPIRITRON_PURPLE, px, py, pz, 1, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 1, 0, 0.05D, 0, 0.01D);
                }

                // Âm thanh nén năng lượng & cộng hưởng linh tử tăng dần
                if (tick % 10 == 0) {
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 2.0F, 0.8F + (progress * 0.9F));
                }
                if (tick == 14) {
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3.5F, 1.3F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 3.0F, 1.2F);
                }

                // 4. Thập Tự Quang Mang (Cross Star Flare) rực sáng cực độ trước khi khai hỏa
                if (tick >= 24) {
                    Vec3 look = caster.getLookAngle();
                    Vec3 focusPos = caster.getEyePosition().add(look.scale(1.8D));
                    Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
                    Vec3 up = look.cross(right).normalize();
                    double flareArmLength = 5.5D;

                    for (double d = -flareArmLength; d <= flareArmLength; d += 0.4D) {
                        Vec3 pHoriz = focusPos.add(right.scale(d));
                        Vec3 pVert = focusPos.add(up.scale(d));
                        level.sendParticles(ParticleTypes.FLASH, pHoriz.x, pHoriz.y, pHoriz.z, 1, 0, 0, 0, 0);
                        level.sendParticles(STARLIGHT_WHITE_DUST, pHoriz.x, pHoriz.y, pHoriz.z, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.END_ROD, pVert.x, pVert.y, pVert.z, 1, 0, 0, 0, 0.02D);
                        level.sendParticles(ELECTRIC_CYAN_DUST, pVert.x, pVert.y, pVert.z, 1, 0, 0, 0, 0);
                    }

                    if (tick == 24) {
                        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 4.0F, 1.95F);
                        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.5F, 1.5F);
                    }
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 2: KHAI HỎA ĐẠI PHÁO 3D 75M & PHÁ HỦY BLOCK RÃNH HẦM 4M (Ticks 30 -> 54)
            // =========================================================================
            else if (tick < 55) {
                Vec3 origin = caster.getEyePosition();
                Vec3 look = caster.getLookAngle();
                double maxRange = 75.0D;

                if (tick == 30) {
                    nova.cleanupDisplays();

                    // CỘT SÁNG 3D ĐA TẦNG CỰC ĐẠI CHUẨN MILIM NAVA
                    // Lõi trong 4.5F (0xFF55DD - Tinh Tố Hồng), Vỏ bọc ngoài 8.0F (0xFF0044 - Long Hỏa Đỏ Tía)
                    HorizontalHolyBeamAbility.spawn3DBeamDisplay(level, origin, look, maxRange, 4.5F, 0xFF55DD, 8.0F, 0xFF0044, 40);

                    // THOẠI KINH ĐIỂN CỦA MILIM KHI KHAI HỎA
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(64.0D))) {
                        p.displayClientMessage(
                            Component.literal("§d§l[MILIM NAVA] §e«Drago-Nova cực hạn xuất lực!! Hủy diệt toàn bộ đất trời tan thành cát bụi!!»"),
                            false
                        );
                    }

                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, caster.getX(), caster.getY() + 1.2D, caster.getZ(), 2, 0.5D, 0.5D, 0.5D, 0);

                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 6.0F, 0.45F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 6.0F, 0.75F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 0.7F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.5F, 1.4F);

                    caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
                }

                Vec3 rayEnd = origin.add(look.scale(maxRange));
                BlockHitResult blockHit = level.clip(new ClipContext(
                        origin, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster
                ));

                Vec3 hitPos = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : rayEnd;
                nova.lastHitPos = hitPos;
                double beamDist = origin.distanceTo(hitPos);

                Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
                Vec3 up = look.cross(right).normalize();
                double helixRadius = 1.6D;

                // 1. Phá hủy địa hình mạnh mẽ (khoét rãnh hầm bán kính 4 block giống hệt Milim)
                int tunnelRadius = 4;
                int tunnelRadiusSq = tunnelRadius * tunnelRadius;
                for (double d = 1.0D; d < beamDist; d += 2.0D) {
                    Vec3 carvePt = origin.add(look.scale(d));
                    BlockPos centerBp = BlockPos.containing(carvePt);

                    for (int bx = -tunnelRadius; bx <= tunnelRadius; bx++) {
                        for (int by = -tunnelRadius; by <= tunnelRadius; by++) {
                            for (int bz = -tunnelRadius; bz <= tunnelRadius; bz++) {
                                if (bx * bx + by * by + bz * bz <= tunnelRadiusSq) {
                                    BlockPos bp = centerBp.offset(bx, by, bz);
                                    BlockState state = level.getBlockState(bp);
                                    if (!state.isAir() && state.getBlock() != Blocks.BEDROCK && state.getDestroySpeed(level, bp) >= 0.0F) {
                                        level.destroyBlock(bp, false);
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Gây cháy lớn cho địa hình xung quanh (ngoại vi bán kính 7 blocks giống hệt Milim)
                if (tick == 30 || tick == 42) {
                    int fireRadius = 7;
                    int fireRadiusSq = fireRadius * fireRadius;
                    for (double d = 2.0D; d < beamDist; d += 4.0D) {
                        Vec3 firePt = origin.add(look.scale(d));
                        BlockPos centerBp = BlockPos.containing(firePt);
                        for (int fx = -fireRadius; fx <= fireRadius; fx++) {
                            for (int fz = -fireRadius; fz <= fireRadius; fz++) {
                                if (fx * fx + fz * fz <= fireRadiusSq && RANDOM.nextFloat() < 0.35F) {
                                    for (int fy = -2; fy <= 3; fy++) {
                                        BlockPos firePos = centerBp.offset(fx, fy, fz);
                                        BlockState fireState = level.getBlockState(firePos);
                                        if (fireState.is(Blocks.SNOW) || fireState.is(Blocks.SNOW_BLOCK) || fireState.is(Blocks.ICE)) {
                                            level.destroyBlock(firePos, false);
                                        } else if (fireState.isAir() && level.getBlockState(firePos.below()).isSolid()) {
                                            level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Vẽ chùm tia hạt năng lượng cực đại chuẩn Milim
                for (double d = 1.0D; d < beamDist; d += 0.5D) {
                    Vec3 corePt = origin.add(look.scale(d));

                    if (Math.round(d) % 6 == 0) {
                        level.sendParticles(ParticleTypes.SONIC_BOOM, corePt.x, corePt.y, corePt.z, 1, 0, 0, 0, 0);
                    }
                    level.sendParticles(ParticleTypes.FLASH, corePt.x, corePt.y, corePt.z, 1, 0.4D, 0.4D, 0.4D, 0);
                    level.sendParticles(SPIRITRON_PINK, corePt.x, corePt.y, corePt.z, 3, 1.0D, 1.0D, 1.0D, 0.08D);
                    level.sendParticles(SPIRITRON_PURPLE, corePt.x, corePt.y, corePt.z, 2, 0.8D, 0.8D, 0.8D, 0.08D);
                    level.sendParticles(ParticleTypes.FLAME, corePt.x, corePt.y, corePt.z, 3, 1.2D, 1.2D, 1.2D, 0.1D);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, corePt.x, corePt.y, corePt.z, 2, 0.8D, 0.8D, 0.8D, 0.05D);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, corePt.x, corePt.y, corePt.z, 2, 0.8D, 0.8D, 0.8D, 0.1D);

                    // Xoắn kép năng lượng
                    double angle = (d * 1.6D) + (tick * 0.85D);
                    Vec3 offset1 = right.scale(Math.cos(angle) * helixRadius).add(up.scale(Math.sin(angle) * helixRadius));
                    Vec3 offset2 = right.scale(-Math.cos(angle) * helixRadius).add(up.scale(-Math.sin(angle) * helixRadius));
                    Vec3 h1 = corePt.add(offset1);
                    Vec3 h2 = corePt.add(offset2);

                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, h1.x, h1.y, h1.z, 1, 0, 0, 0, 0.03D);
                    level.sendParticles(NEON_MAGENTA_DUST, h1.x, h1.y, h1.z, 1, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, h2.x, h2.y, h2.z, 1, 0, 0, 0, 0.03D);
                    level.sendParticles(DEEP_PURPLE_DUST, h2.x, h2.y, h2.z, 1, 0, 0, 0, 0);
                }

                // Điểm va chạm bắn tung tóe chấn động
                level.sendParticles(ParticleTypes.EXPLOSION, hitPos.x, hitPos.y, hitPos.z, 3, 0.8D, 0.8D, 0.8D, 0.1D);
                level.sendParticles(ParticleTypes.FLASH, hitPos.x, hitPos.y, hitPos.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 4, 0.5D, 0.5D, 0.5D, 0.1D);

                // 4. SÁT THƯƠNG ĐẶC BIỆT CHUẨN MILIM:
                // Xuyên mọi giáp, Xóa sạch buff (Regen, Resistance, Fire Res, Absorption),
                // Lực hút chân không & Giáng 2,000 sát thương ma pháp mỗi 2 ticks!
                if (tick % 2 == 0) {
                    AABB beamBounds = new AABB(origin, hitPos).inflate(5.0D);
                    List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, beamBounds,
                            e -> e != caster && e.isAlive());

                    for (LivingEntity target : potentialTargets) {
                        Vec3 targetPos = target.getEyePosition();
                        double perpDistSq = getPerpendicularDistanceSq(origin, hitPos, targetPos);

                        if (perpDistSq <= 25.0D) { // Bán kính 5.0m
                            // Xóa sạch các buff phòng ngự bảo hộ
                            target.removeEffect(MobEffects.REGENERATION);
                            target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                            target.removeEffect(MobEffects.FIRE_RESISTANCE);
                            target.removeEffect(MobEffects.ABSORPTION);

                            // Lực hút chân không & Đẩy lùi theo phương chùm tia
                            Vec3 AB = hitPos.subtract(origin);
                            double t = Mth.clamp(targetPos.subtract(origin).dot(AB) / AB.lengthSqr(), 0.0D, 1.0D);
                            Vec3 beamCenter = origin.add(AB.scale(t));
                            Vec3 pull = beamCenter.subtract(targetPos).normalize().scale(0.8D).add(look.scale(1.2D));
                            target.setDeltaMovement(pull);
                            target.hasImpulse = true;

                            // Sát thương ma pháp xuyên giáp cực lớn
                            target.addTag("DragonNovaDamage");
                            target.hurt(level.damageSources().magic(), 500.0F);
                            target.hurt(level.damageSources().playerAttack(caster), 1500.0F);
                            target.removeTag("DragonNovaDamage");

                            // Thiêu đốt ngọn lửa ma long 20s không thể dập tắt
                            target.setRemainingFireTicks(400);

                            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(), 10, 0.4D, 0.4D, 0.4D, 0.15D);
                            level.sendParticles(SPIRITRON_PINK, target.getX(), target.getY() + 1.0D, target.getZ(), 6, 0.5D, 0.5D, 0.5D, 0.1D);
                        }
                    }
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 3: ĐẠI VỤ NỔ SIÊU TÂN TINH (GRAND CRATER EXPLOSION) (Tick 55)
            // =========================================================================
            else if (tick == 55) {
                Vec3 hit = nova.lastHitPos;
                if (hit == null) {
                    hit = caster.getEyePosition().add(caster.getLookAngle().scale(40.0D));
                }

                BlockPos impactCenter = BlockPos.containing(hit);

                // 1. Phá hủy địa hình tạo Hố Bom khổng lồ bán kính 10 blocks (sâu -6 đến 8) chuẩn Milim
                int craterRadius = 10;
                int craterRadiusSq = craterRadius * craterRadius;
                for (int cx = -craterRadius; cx <= craterRadius; cx++) {
                    for (int cz = -craterRadius; cz <= craterRadius; cz++) {
                        for (int cy = -6; cy <= 8; cy++) {
                            if (cx * cx + cz * cz + cy * cy <= craterRadiusSq) {
                                BlockPos craterPos = impactCenter.offset(cx, cy, cz);
                                BlockState cState = level.getBlockState(craterPos);
                                if (!cState.isAir() && cState.getBlock() != Blocks.BEDROCK && cState.getDestroySpeed(level, craterPos) >= 0) {
                                    level.destroyBlock(craterPos, false);
                                }
                            }
                        }
                    }
                }

                // 2. Vành đai biển lửa bao phủ mặt đất bán kính 16m chuẩn Milim
                int impactFireRadius = 16;
                for (int ix = -impactFireRadius; ix <= impactFireRadius; ix++) {
                    for (int iz = -impactFireRadius; iz <= impactFireRadius; iz++) {
                        if (ix * ix + iz * iz <= impactFireRadius * impactFireRadius && RANDOM.nextFloat() < 0.55F) {
                            BlockPos floorPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, impactCenter.offset(ix, 0, iz));
                            if (level.getBlockState(floorPos).isAir() && level.getBlockState(floorPos.below()).isSolid()) {
                                level.setBlockAndUpdate(floorPos, Blocks.FIRE.defaultBlockState());
                            }
                        }
                    }
                }

                // 3. Âm thanh bộc phá chấn động kinh hoàng
                level.playSound(null, hit.x, hit.y, hit.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 10.0F, 0.38F);
                level.playSound(null, hit.x, hit.y, hit.z,
                        SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 8.0F, 0.5F);
                level.playSound(null, hit.x, hit.y, hit.z,
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 7.0F, 0.55F);
                level.playSound(null, hit.x, hit.y, hit.z,
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 5.0F, 0.8F);

                // 4. Tâm nổ bộc phát chớp sáng mù lòa & dung nham tung tóe
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hit.x, hit.y, hit.z, 16, 4.0D, 3.0D, 4.0D, 0);
                level.sendParticles(ParticleTypes.FLASH, hit.x, hit.y, hit.z, 6, 2.0D, 2.0D, 2.0D, 0);
                level.sendParticles(ParticleTypes.SONIC_BOOM, hit.x, hit.y, hit.z, 4, 1.0D, 1.0D, 1.0D, 0);
                level.sendParticles(ParticleTypes.LAVA, hit.x, hit.y, hit.z, 60, 6.0D, 4.0D, 6.0D, 0.25D);

                // 5. Cột năng lượng chọc trời 50m (Cosmic Sky Pillar)
                for (double y = 0; y <= 50.0D; y += 1.5D) {
                    level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y + y, hit.z, 4, 1.0D, 0.4D, 1.0D, 0.06D);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, hit.x, hit.y + y, hit.z, 5, 1.4D, 0.4D, 1.4D, 0.03D);
                    level.sendParticles(SPIRITRON_PINK, hit.x, hit.y + y, hit.z, 4, 1.2D, 0.3D, 1.2D, 0.05D);
                    level.sendParticles(SPIRITRON_PURPLE, hit.x, hit.y + y, hit.z, 3, 1.0D, 0.3D, 1.0D, 0.05D);
                    level.sendParticles(ELECTRIC_CYAN_DUST, hit.x, hit.y + y, hit.z, 3, 1.0D, 0.3D, 1.0D, 0);
                    if (((int) y) % 5 == 0) {
                        level.sendParticles(ParticleTypes.FLASH, hit.x, hit.y + y, hit.z, 1, 0, 0, 0, 0);
                    }
                }

                // 6. 8 vòng sóng xung kích bán cầu khổng lồ mở rộng từ 3m đến 28m
                double[] shockwaveRadii = {3.0D, 6.0D, 9.0D, 12.0D, 16.0D, 20.0D, 24.0D, 28.0D};
                for (double r : shockwaveRadii) {
                    int ringParticles = 28;
                    for (int step = 0; step < ringParticles; step++) {
                        double ang = step * (Math.PI * 2.0D / ringParticles);
                        double wx = hit.x + Math.cos(ang) * r;
                        double wz = hit.z + Math.sin(ang) * r;

                        level.sendParticles(ParticleTypes.FLASH, wx, hit.y + 0.6D, wz, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.GLOW, wx, hit.y + 0.6D, wz, 1, 0, 0.15D, 0, 0.05D);
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, wx, hit.y + 0.6D, wz, 1, 0, 0.2D, 0, 0.03D);
                        level.sendParticles(SPIRITRON_PINK, wx, hit.y + 0.6D, wz, 1, 0, 0.1D, 0, 0);
                    }
                }

                // 7. 8 luồng Sonic Boom phóng tỏa ra 8 hướng từ tâm
                for (int dir = 0; dir < 8; dir++) {
                    double ang = dir * (Math.PI * 2.0D / 8.0D);
                    Vec3 boomPos = hit.add(Math.cos(ang) * 6.5D, 1.0D, Math.sin(ang) * 6.5D);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, boomPos.x, boomPos.y, boomPos.z, 1, 0, 0, 0, 0);
                }

                // 8. 16 tia sét ma thuật cắm từ tâm vụ nổ xuống mặt đất xung quanh
                for (int s = 0; s < 16; s++) {
                    double ang = s * (Math.PI * 2.0D / 16.0D);
                    double dist = 8.0D + RANDOM.nextDouble() * 14.0D;
                    Vec3 strikeTarget = hit.add(Math.cos(ang) * dist, 0.0D, Math.sin(ang) * dist);
                    drawJaggedLightning(level, hit.add(0, 10.0D, 0), strikeTarget, 0.6D);
                }

                // 9. SÁT THƯƠNG ĐẶC BIỆT ĐẠI VỤ NỔ (Bán kính 28m)
                AABB blastArea = new AABB(hit.x - 28.0D, hit.y - 14.0D, hit.z - 28.0D,
                                          hit.x + 28.0D, hit.y + 24.0D, hit.z + 28.0D);
                List<LivingEntity> blastVictims = level.getEntitiesOfClass(LivingEntity.class, blastArea,
                        e -> e != caster && e.isAlive());

                for (LivingEntity victim : blastVictims) {
                    double dist = victim.position().distanceTo(hit);
                    if (dist <= 28.0D) {
                        // Ngoại lệ đặc biệt: Chước Nhiệt Long Velgrynd bị diệt ngay lập tức
                        if (victim instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity velgrynd) {
                            velgrynd.broadcastDialogue("Không thể nào... Năng lượng Tinh Tố này... Là Milim sao...?! Rudra...!");
                            velgrynd.setDragonLayers(0);
                            velgrynd.hurt(level.damageSources().playerAttack(caster), 10000.0F);
                            continue;
                        }

                        // Xóa sạch mọi hiệu ứng phòng ngự của mục tiêu
                        victim.removeEffect(MobEffects.REGENERATION);
                        victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                        victim.removeEffect(MobEffects.FIRE_RESISTANCE);
                        victim.removeEffect(MobEffects.ABSORPTION);

                        boolean isBoss = victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                                || victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                                || victim instanceof net.minecraft.world.entity.monster.warden.Warden
                                || victim instanceof net.minecraft.world.entity.monster.ElderGuardian
                                || victim.getMaxHealth() >= 100.0F;

                        if (isBoss) {
                            // Rút 90% HP Boss hiện tại hoặc tối thiểu 3,000 sát thương
                            float bossDamage = Math.max(3000.0F, victim.getHealth() * 0.90F);
                            victim.hurt(level.damageSources().magic(), 1000.0F);
                            victim.hurt(level.damageSources().playerAttack(caster), bossDamage);

                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 3));
                            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 3));
                            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0));
                            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0));

                            victim.setDeltaMovement(new Vec3(0, 2.2D, 0));
                        } else {
                            // Quái thường: Nhận 5,000 sát thương, xóa sổ trực tiếp
                            TensuraEvents.handleMobDeathDrop(caster, victim);
                            victim.hurt(level.damageSources().playerAttack(caster), 5000.0F);
                            if (victim.isAlive()) {
                                victim.discard();
                            }
                        }

                        Vec3 blastDir = victim.position().subtract(hit).normalize().add(0, 1.0D, 0).scale(3.5D);
                        victim.setDeltaMovement(blastDir);
                        victim.hasImpulse = true;
                    }
                }

                caster.displayClientMessage(
                    Component.literal("§d§l[LONG TINH BỘC VIÊM BÁ] §fBáo cáo. Siêu Tân Tinh hủy diệt đã xóa sổ hoàn toàn thực thể mục tiêu và địa hình!"),
                    true
                );
            }

            // =========================================================================
            // DƯ CHẤN TÀN TRO TINH TRẦN (Ticks 56 -> 75)
            // =========================================================================
            else {
                if (nova.lastHitPos != null && tick % 3 == 0) {
                    Vec3 hit = nova.lastHitPos;
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hit.x, hit.y + 1.0D, hit.z, 3, 1.5D, 0.5D, 1.5D, 0.02D);
                    level.sendParticles(ParticleTypes.GLOW, hit.x, hit.y + 0.8D, hit.z, 3, 1.2D, 0.3D, 1.2D, 0.03D);
                    level.sendParticles(SPIRITRON_PINK, hit.x, hit.y + 0.5D, hit.z, 2, 1.0D, 0.2D, 1.0D, 0);
                    level.sendParticles(ELECTRIC_CYAN_DUST, hit.x, hit.y + 0.5D, hit.z, 2, 1.0D, 0.2D, 1.0D, 0);
                }
            }

            nova.currentTick++;
            if (nova.currentTick >= nova.totalTicks) {
                nova.cleanupDisplays();
                iterator.remove();
            }
        }
    }

    /**
     * Tạo thực thể ItemDisplay Ma Pháp Trận sắc nét phát quang
     */
    private static Display.ItemDisplay createMagicCircleDisplay(ServerLevel level, Vec3 pos, Item item, float initialScale, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) display;
            DisplayAccessor dispAcc = (DisplayAccessor) display;

            itemAcc.weapons$setItemStack(new ItemStack(item));
            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            dispAcc.weapons$setGlowColorOverride(glowColor);
            dispAcc.weapons$setViewRange(3.5F);

            Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(initialScale, initialScale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    /**
     * Cập nhật kích thước và góc xoay cho Ma Pháp Trận
     */
    private static void updateDisplayTransformation(Display.ItemDisplay display, float scale, Quaternionf rotation) {
        if (display != null && display.isAlive()) {
            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));
        }
    }

    /**
     * Sinh tia sét ma thuật ziczac phát sáng giữa 2 điểm
     */
    private static void drawJaggedLightning(ServerLevel level, Vec3 start, Vec3 end, double jitter) {
        int segments = 6;
        Vec3 prev = start;
        Vec3 dir = end.subtract(start);
        double segLen = dir.length() / segments;
        Vec3 segStep = dir.normalize().scale(segLen);

        for (int i = 1; i <= segments; i++) {
            Vec3 target = start.add(segStep.scale(i));
            if (i < segments) {
                double jx = (RANDOM.nextDouble() - 0.5D) * 2.0D * jitter;
                double jy = (RANDOM.nextDouble() - 0.5D) * 2.0D * jitter;
                double jz = (RANDOM.nextDouble() - 0.5D) * 2.0D * jitter;
                target = target.add(jx, jy, jz);
            }

            double subDist = prev.distanceTo(target);
            Vec3 subDir = target.subtract(prev).normalize();
            for (double d = 0; d < subDist; d += 0.35D) {
                Vec3 p = prev.add(subDir.scale(d));
                level.sendParticles(ELECTRIC_CYAN_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                level.sendParticles(NEON_MAGENTA_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0.01D);
            }
            prev = target;
        }
    }

    /**
     * Tính khoảng cách vuông góc từ điểm P đến đoạn thẳng nối từ A đến B (bình phương)
     */
    private static double getPerpendicularDistanceSq(Vec3 A, Vec3 B, Vec3 P) {
        Vec3 AB = B.subtract(A);
        Vec3 AP = P.subtract(A);
        double lenSq = AB.lengthSqr();
        if (lenSq < 1e-6) return AP.lengthSqr();

        double t = AP.dot(AB) / lenSq;
        t = Mth.clamp(t, 0.0D, 1.0D);
        Vec3 closest = A.add(AB.scale(t));
        return P.distanceToSqr(closest);
    }
}
