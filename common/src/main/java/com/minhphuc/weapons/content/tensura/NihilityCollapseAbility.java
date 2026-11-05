package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundNihilityCinematicPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * KỸ NĂNG ĐẠI HỢP NHẤT: HƯ VÔ BĂNG HOẠI (NIHILITY COLLAPSE / GRAND CONVERGENCE)
 * Kết nối toàn bộ tinh hoa của các tuyệt kỹ trong mod vào một kịch bản 4 hồi liên hoàn:
 * - Hồi 1 (0 -> 50 ticks): Lãnh Địa Hắc Ám & Kết Giới Không Gian (Lồng Giam Vô Hạn + Hắc Viêm Ngục + Kháng Ma Kết Giới)
 *   Bán kính mở rộng 70 block, bầu trời tối sầm, phong tỏa không gian, mù & làm chậm mob.
 * - Hồi 2 (50 -> 110 ticks): Liệt Không Vũ Trụ & Nghịch Chuyển Trọng Lực (Dragon Nova + Alkaid + Bạo Thực Vương Beelzebuth)
 *   Vết nứt không gian vũ trụ trên trời độ cao 35m + Quả cầu Tinh tú Alkaid + Lực hút chân không kéo toàn bộ mob bay lơ lửng.
 * - Hồi 3 (110 -> 140 ticks): Điểm Tĩnh Lặng Tuyệt Đối ("I AM ATOMIC" + Tiên Tri Rykuzin)
 *   Đóng băng quái vật trên không, camera zoom cận cảnh kịch tính, tiêu đề và lời thì thầm: "I... AM... ATOMIC."
 * - Hồi 4 (140 -> 200 ticks): Đại Bộc Phát - Hủy Diệt Đa Tầng (Linh Tử Băng Hoại + Long Tinh Bộc Viêm Bá + Tuyệt Diệt Tinh Tú)
 *   Cột Sáng Linh Tử chọc trời dội xuống tâm đất, sóng xung kích quét sạch mọi quái vật với 3,000 sát thương ma pháp/hư vô.
 *   Kẻ địch bị tiêu diệt bốc hơi thành hạt linh tử và rơi ra Linh Hồn / Soul Stone.
 * - Tối Ưu Hóa ZERO-LAG:
 *   Sử dụng Display Entity GPU ma trận, đồng bộ Camera Client qua Packet, tính toán sát thương 1 lần duy nhất tại tick 141.
 */
public class NihilityCollapseAbility {

    private static final DustParticleOptions VOID_PURPLE = new DustParticleOptions(new Vector3f(0.55F, 0.0F, 0.9F), 2.2F);
    private static final DustParticleOptions SPIRITRON_CYAN = new DustParticleOptions(new Vector3f(0.2F, 0.9F, 1.0F), 2.0F);

    public static class ActiveCollapse {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public int currentTick = 0;
        public final int maxTicks = 200; // 10.0 giây

        // Display Entities phía Server cho hiệu ứng hình ảnh
        public Display.ItemDisplay barrierRing = null;
        public Display.ItemDisplay celestialRift = null;
        public Display.ItemDisplay verticalPillar = null;

        public ActiveCollapse(ServerLevel level, ServerPlayer caster, Vec3 center) {
            this.level = level;
            this.caster = caster;
            this.center = center;
        }

        public void cleanup() {
            if (barrierRing != null && barrierRing.isAlive()) {
                barrierRing.discard();
                barrierRing = null;
            }
            if (celestialRift != null && celestialRift.isAlive()) {
                celestialRift.discard();
                celestialRift = null;
            }
            if (verticalPillar != null && verticalPillar.isAlive()) {
                verticalPillar.discard();
                verticalPillar = null;
            }
        }
    }

    public static final List<ActiveCollapse> ACTIVE_COLLAPSES = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer caster) {
        if (caster == null || level == null) return;

        Vec3 center = caster.position();

        ActiveCollapse collapse = new ActiveCollapse(level, caster, center);
        ACTIVE_COLLAPSES.add(collapse);

        // Gửi Packet kích hoạt Chuỗi Camera Điện Ảnh 5 Góc Quay cho người dùng
        ModMessages.sendToPlayer(
                new ClientboundNihilityCinematicPacket(caster.getUUID(), center, 200),
                caster
        );

        // Âm thanh khởi động sấm rền hư vô
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 0.5F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.6F);

        caster.displayClientMessage(
                Component.literal("§8[HƯ VÔ CHI THẦN] §5§lKÍCH HOẠT ĐẠI HỢP NHẤT: HƯ VÔ BĂNG HOẠI!"),
                true
        );
    }

    public static void tickCollapses(ServerLevel level) {
        if (ACTIVE_COLLAPSES.isEmpty()) return;

        synchronized (ACTIVE_COLLAPSES) {
            Iterator<ActiveCollapse> iterator = ACTIVE_COLLAPSES.iterator();
            while (iterator.hasNext()) {
                ActiveCollapse c = iterator.next();
                if (c.level != level) continue;

                c.currentTick++;

                // ==============================================================
                // HỒI 1 (0 -> 50 ticks / 2.5s): LÃNH ĐỊA HẮC ÁM & KẾT GIỚI KHÔNG GIAN
                // ==============================================================
                if (c.currentTick <= 50) {
                    if (c.barrierRing == null) {
                        c.barrierRing = spawnItemDisplay(c.level, c.center.add(0, 1.5, 0),
                                new ItemStack(ModBlocks.BLACK_FIRE.get()), 70.0F, 0.3F, 70.0F, 0x1A0033);
                    }

                    // Khóa không gian & mù tạm thời cho toàn bộ mob trong bán kính 70 block
                    if (c.currentTick % 10 == 0) {
                        AABB zone = new AABB(c.center.x - 70, c.center.y - 20, c.center.z - 70,
                                c.center.x + 70, c.center.y + 60, c.center.z + 70);
                        List<LivingEntity> targets = c.level.getEntitiesOfClass(LivingEntity.class, zone,
                                e -> e.isAlive() && e != c.caster && !isAlliedToCaster(c.caster, e));

                        for (LivingEntity target : targets) {
                            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 1, false, false));
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5, false, false));
                        }
                    }

                    // Hạt hư vô đen-tím viền ngoài kết giới
                    if (c.currentTick % 4 == 0) {
                        for (int i = 0; i < 16; i++) {
                            double angle = Math.toRadians((i * 22.5) + (c.currentTick * 3.0));
                            double px = c.center.x + Math.cos(angle) * 35.0;
                            double pz = c.center.z + Math.sin(angle) * 35.0;
                            c.level.sendParticles(VOID_PURPLE, px, c.center.y + 1.0, pz, 1, 0.1, 0.5, 0.1, 0.02);
                        }
                    }
                }

                // ==============================================================
                // HỒI 2 (50 -> 110 ticks / 3.0s): LIỆT KHÔNG VŨ TRỤ & NGHỊCH CHUYỂN TRỌNG LỰC
                // ==============================================================
                else if (c.currentTick <= 110) {
                    Vec3 riftCenter = c.center.add(0, 35, 0);

                    if (c.celestialRift == null) {
                        c.celestialRift = spawnItemDisplay(c.level, riftCenter,
                                new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()), 14.0F, 14.0F, 14.0F, 0x9400D3);

                        c.level.playSound(null, riftCenter.x, riftCenter.y, riftCenter.z,
                                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 3.0F, 0.7F);
                    }

                    // Vết nứt xoay tròn trên bầu trời
                    if (c.celestialRift != null && c.celestialRift.isAlive()) {
                        float spin = (float) Math.toRadians(c.currentTick * 8.0);
                        Quaternionf rot = new Quaternionf().rotateY(spin).rotateX(0.35F);
                        Transformation transform = new Transformation(
                                new Vector3f(0, 0, 0),
                                rot,
                                new Vector3f(12.0F, 12.0F, 12.0F),
                                new Quaternionf()
                        );
                        ((DisplayAccessor) c.celestialRift).weapons$setTransformation(transform);
                    }

                    // Trọng lực nghịch đảo: Hút toàn bộ mob bay lơ lửng ngược lên bầu trời
                    AABB vacuumZone = new AABB(c.center.x - 55, c.center.y - 10, c.center.z - 55,
                            c.center.x + 55, c.center.y + 40, c.center.z + 55);
                    List<LivingEntity> vacuumTargets = c.level.getEntitiesOfClass(LivingEntity.class, vacuumZone,
                            e -> e.isAlive() && e != c.caster && !isAlliedToCaster(c.caster, e));

                    for (LivingEntity target : vacuumTargets) {
                        Vec3 toRift = riftCenter.subtract(target.position());
                        double dist = toRift.length();
                        if (dist > 1.5) {
                            Vec3 liftVelocity = toRift.normalize().scale(0.45).add(0, 0.28, 0);
                            target.setDeltaMovement(liftVelocity);
                            target.hurtMarked = true;
                        }
                    }

                    // Hạt tinh vân và plasma xoay tròn quanh vết rách
                    if (c.currentTick % 3 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.toRadians((i * 45.0) + (c.currentTick * 12.0));
                            double px = riftCenter.x + Math.cos(angle) * 8.0;
                            double pz = riftCenter.z + Math.sin(angle) * 8.0;
                            c.level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, riftCenter.y, pz, 2, 0.2, 0.2, 0.2, 0.05);
                            c.level.sendParticles(ParticleTypes.DRAGON_BREATH, px, riftCenter.y, pz, 1, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }

                // ==============================================================
                // HỒI 3 (110 -> 140 ticks / 1.5s): ĐIỂM TĨNH LẶNG TUYỆT ĐỐI ("I AM ATOMIC")
                // ==============================================================
                else if (c.currentTick <= 140) {
                    // Đóng băng toàn bộ mục tiêu đang lơ lửng trên không
                    AABB freezeZone = new AABB(c.center.x - 60, c.center.y - 10, c.center.z - 60,
                            c.center.x + 60, c.center.y + 50, c.center.z + 60);
                    List<LivingEntity> frozenTargets = c.level.getEntitiesOfClass(LivingEntity.class, freezeZone,
                            e -> e.isAlive() && e != c.caster && !isAlliedToCaster(c.caster, e));

                    for (LivingEntity target : frozenTargets) {
                        target.setDeltaMovement(0, 0.02, 0);
                        target.hurtMarked = true;
                    }

                    // Tiêu đề và âm vang thì thầm tại tick 115
                    if (c.currentTick == 115) {
                        c.caster.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 10));
                        c.caster.connection.send(new ClientboundSetTitleTextPacket(
                                Component.literal("§8[§5HƯ VÔ BĂNG HOẠI§8]")
                        ));
                        c.caster.connection.send(new ClientboundSetSubtitleTextPacket(
                                Component.literal("§d§lI... AM... ATOMIC.")
                        ));

                        c.level.playSound(null, c.center.x, c.center.y, c.center.z,
                                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.5F, 0.6F);
                        c.level.playSound(null, c.center.x, c.center.y, c.center.z,
                                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0F, 0.5F);
                    }
                }

                // ==============================================================
                // HỒI 4 (141 -> 200 ticks / 3.0s): ĐẠI BỘC PHÁT - HỦY DIỆT ĐA TẦNG
                // ==============================================================
                else {
                    // KÍCH NỔ CỰC ĐẠI TẠI TICK 141
                    if (c.currentTick == 141) {
                        // 1. Tạo Cột Sáng Linh Tử Chọc Trời 256m
                        c.verticalPillar = spawnItemDisplay(c.level, c.center.add(0, 60, 0),
                                new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()), 12.0F, 256.0F, 12.0F, 0xDF80FF);

                        // 2. Sóng Âm Thanh Chấn Động
                        c.level.playSound(null, c.center.x, c.center.y, c.center.z,
                                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 10.0F, 0.5F);
                        c.level.playSound(null, c.center.x, c.center.y, c.center.z,
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 8.0F, 0.7F);

                        // 3. Quét Sát Thương Tuyệt Đối (One-Shot Spatial AABB)
                        AABB blastZone = new AABB(c.center.x - 65, c.center.y - 15, c.center.z - 65,
                                c.center.x + 65, c.center.y + 60, c.center.z + 65);
                        List<LivingEntity> blastTargets = c.level.getEntitiesOfClass(LivingEntity.class, blastZone,
                                e -> e.isAlive() && e != c.caster && !isAlliedToCaster(c.caster, e));

                        for (LivingEntity target : blastTargets) {
                            float damage = 3000.0F; // 3,000 Sát thương hủy diệt linh tử / hư vô

                            // Quy tắc Lore miễn trừ chuẩn đã thống nhất
                            if (target instanceof VelgryndEntity || target instanceof VelzardEntity) {
                                damage = 0.0F; // Long chủng miễn nhiễm
                            } else if (target instanceof PrimordialDemonEntity) {
                                damage = 0.0F; // Thủy tổ Ác ma (Guy/Rouge, Blanc, Noir) miễn nhiễm
                            } else if (target instanceof MilimEntity || target instanceof MarbasEntity) {
                                damage = 0.0F; // Milim và Marbas miễn nhiễm
                            } else if (target instanceof CuuTheQuyenHienEntity) {
                                damage *= 0.30F; // Cứu Thế Quyền Hiện chịu 30% sát thương (900 damage)
                            }

                            if (damage > 0) {
                                target.hurt(c.level.damageSources().magic(), damage);

                                // Nếu bị tiêu diệt, bốc hơi và rơi Linh Hồn
                                if (!target.isAlive()) {
                                    c.level.sendParticles(ParticleTypes.SOUL,
                                            target.getX(), target.getY() + 0.5, target.getZ(),
                                            15, 0.4, 0.6, 0.4, 0.05);

                                    target.spawnAtLocation(new ItemStack(ModItems.MOB_SOUL.get()));
                                }
                            }
                        }

                        // 4. Sóng Xung Kích Hạt Nở Rộng 60m
                        for (int ring = 1; ring <= 6; ring++) {
                            double radius = ring * 10.0;
                            for (int i = 0; i < 24; i++) {
                                double angle = Math.toRadians(i * 15.0);
                                double px = c.center.x + Math.cos(angle) * radius;
                                double pz = c.center.z + Math.sin(angle) * radius;
                                c.level.sendParticles(SPIRITRON_CYAN, px, c.center.y + 0.5, pz, 2, 0.2, 0.2, 0.2, 0.05);
                                c.level.sendParticles(ParticleTypes.SONIC_BOOM, px, c.center.y + 1.0, pz, 1, 0, 0, 0, 0);
                            }
                        }
                    }

                    // Giảm dần độ dày cột sáng từ tick 170 đến 200
                    if (c.currentTick > 170 && c.verticalPillar != null && c.verticalPillar.isAlive()) {
                        float fade = (200 - c.currentTick) / 30.0F;
                        Transformation transform = new Transformation(
                                new Vector3f(0, 0, 0),
                                new Quaternionf(),
                                new Vector3f(12.0F * fade, 256.0F, 12.0F * fade),
                                new Quaternionf()
                        );
                        ((DisplayAccessor) c.verticalPillar).weapons$setTransformation(transform);
                    }
                }

                // Dọn dẹp kết thúc ở tick 200
                if (c.currentTick >= c.maxTicks) {
                    c.cleanup();
                    iterator.remove();
                }
            }
        }
    }

    public static void cleanupLevel(ServerLevel level) {
        synchronized (ACTIVE_COLLAPSES) {
            Iterator<ActiveCollapse> it = ACTIVE_COLLAPSES.iterator();
            while (it.hasNext()) {
                ActiveCollapse c = it.next();
                if (c.level == level) {
                    c.cleanup();
                    it.remove();
                }
            }
        }
    }

    private static boolean isAlliedToCaster(ServerPlayer caster, LivingEntity entity) {
        return entity.isAlliedTo(caster) || entity.getUUID().equals(caster.getUUID());
    }

    private static Display.ItemDisplay spawnItemDisplay(ServerLevel level, Vec3 pos, ItemStack item,
                                                        float scaleX, float scaleY, float scaleZ, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) return null;

        display.setPos(pos.x, pos.y, pos.z);
        ((ItemDisplayAccessor) display).weapons$setItemStack(item);
        ((ItemDisplayAccessor) display).weapons$setItemTransform(ItemDisplayContext.FIXED);

        Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(scaleX, scaleY, scaleZ),
                new Quaternionf()
        );
        ((DisplayAccessor) display).weapons$setTransformation(transform);

        display.setGlowingTag(true);
        ((DisplayAccessor) display).weapons$setGlowColorOverride(glowColor);

        level.addFreshEntity(display);
        return display;
    }
}
