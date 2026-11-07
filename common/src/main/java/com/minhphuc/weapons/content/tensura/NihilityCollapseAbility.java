package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.init.ModSoundEvents;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundNihilityCinematicPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.math.Transformation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
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
 * KỸ NĂNG ĐẠI HỢP NHẤT: LỜI CHÚC TỬ VONG (DEATH BLESSING / GRAND CONVERGENCE)
 * Tuyệt kỹ thanh trừng linh hồn tối thượng:
 * - Hồi 1 (0 -> 50 ticks): Lãnh Địa Hắc Ám & Kết Giới Không Gian + Ma Pháp Trận Long Tinh Bộc Viêm Bá.
 *   Người chơi bay bổng lên cao +28m, kết giới lan tỏa 70m, ma pháp trận bành trướng xoay 3D.
 * - Hồi 2 (50 -> 110 ticks): Liệt Không Vũ Trụ & Nghịch Chuyển Trọng Lực.
 *   Vết nứt không gian vũ trụ + Hút toàn bộ mob bay lơ lửng trên không.
 * - Hồi 3 (110 -> 141 ticks): Điểm Tĩnh Lặng Tuyệt Đối ("LỜI CHÚC TỬ VONG").
 *   Đóng băng quái vật, camera zoom cận kịch tính, typography phát quang tiếng Việt.
 * - Hồi 4 (141 -> 200 ticks): Đại Bộc Phát - Phán Quyết Tuyệt Đối (Instant Death).
 *   Cột Sáng Hư Vô Plasma dội xuống, One-Hit tiêu diệt ngay lập tức mọi sinh vật trong 70m, bốc hơi rớt Linh Hồn.
 * - KẾT THÚC: Người chơi giữ nguyên vị thế trên không trung, quan sát toàn cảnh chiến trường đã bị san phẳng.
 */
public class NihilityCollapseAbility {

    private static final DustParticleOptions VOID_PURPLE = new DustParticleOptions(new Vector3f(0.55F, 0.0F, 0.9F), 2.2F);
    private static final DustParticleOptions SPIRITRON_CYAN = new DustParticleOptions(new Vector3f(0.2F, 0.9F, 1.0F), 2.0F);

    public static class ActiveCollapse {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public int currentTick = 0;
        public final int maxTicks = 295; // ~14.75 giây (Kéo dài hồi 3 cho typography chuẩn điện ảnh)

        // Ma Pháp Trận 3D Phát Quang Dưới Mặt Đất (Long Tinh Bộc Viêm Bá)
        public Display.ItemDisplay magicCircleDisplay = null;

        public ActiveCollapse(ServerLevel level, ServerPlayer caster, Vec3 center) {
            this.level = level;
            this.caster = caster;
            this.center = center;
        }

        public void cleanup() {
            if (magicCircleDisplay != null && magicCircleDisplay.isAlive()) {
                magicCircleDisplay.discard();
                magicCircleDisplay = null;
            }
        }
    }

    public static final List<ActiveCollapse> ACTIVE_COLLAPSES = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer caster) {
        if (caster == null || level == null) return;

        Vec3 center = caster.position();

        ActiveCollapse collapse = new ActiveCollapse(level, caster, center);

        // 1. Khởi tạo Ma Pháp Trận 3D phát quang tím thẫm dưới mặt đất (Long Tinh Bộc Viêm Bá)
        Vec3 groundPos = center.add(0, 0.05D, 0);
        collapse.magicCircleDisplay = createMagicCircleDisplay(level, groundPos, ModItems.BEELZEBUTH_MAGIC_CIRCLE.get(), 3.5F, 0x9400D3);

        ACTIVE_COLLAPSES.add(collapse);

        // 2. Cung cấp trạng thái kháng sát thương và chuẩn bị bay lơ lửng
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 4, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));

        // 3. Gửi Packet kích hoạt Chuỗi Camera & Hiệu ứng Đồ Họa Đỉnh Cao cho người dùng và người xem
        ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                caster.getUUID(), center, 295, ClientboundNihilityCinematicPacket.PHASE_START
        );
        ModMessages.sendToPlayer(packet, caster);
        for (ServerPlayer nearbyPlayer : level.players()) {
            if (nearbyPlayer != caster && nearbyPlayer.position().distanceToSqr(center) <= 128 * 128) {
                ModMessages.sendToPlayer(packet, nearbyPlayer);
            }
        }

        // 4. Phát âm thanh Atomic Charge (whisper + ominous hum)
        level.playSound(null, center.x, center.y, center.z,
                ModSoundEvents.ATOMIC_CHARGE.get(), SoundSource.PLAYERS, 2.5F, 1.0F);

        caster.displayClientMessage(
                Component.literal("§4[QUỶ VƯƠNG THỦY TỔ] §c§lKÍCH HOẠT TUYỆT KỸ: LỜI CHÚC TỬ VONG (DEATH BLESSING)!"),
                true
        );

        for (ServerPlayer p : level.players()) {
            if (p != caster && p.position().distanceToSqr(center) <= 128 * 128) {
                p.displayClientMessage(
                        Component.literal("§4§l[LỜI CHÚC TỬ VONG] §c§lBão tố ma thuật giáng lâm! Vạn vật trong 70m chuẩn bị chịu sự phán quyết tuyệt đối!"),
                        false
                );
            }
        }
    }

    public static void tickCollapses(ServerLevel level) {
        if (ACTIVE_COLLAPSES.isEmpty()) return;

        synchronized (ACTIVE_COLLAPSES) {
            Iterator<ActiveCollapse> iterator = ACTIVE_COLLAPSES.iterator();
            while (iterator.hasNext()) {
                ActiveCollapse c = iterator.next();
                if (c.level != level) continue;

                if (c.caster == null || !c.caster.isAlive() || c.caster.hasDisconnected()) {
                    c.cleanup();
                    iterator.remove();
                    continue;
                }

                c.currentTick++;

                // 0. Cập nhật nâng người chơi bay lên cao (+28m) quan sát toàn cảnh ma pháp
                double targetElevY = c.center.y + 28.0D;
                if (c.currentTick <= 40) {
                    float elevProgress = c.currentTick / 40.0F;
                    float elevEase = elevProgress * elevProgress * (3.0F - 2.0F * elevProgress);
                    double currentY = c.center.y + (28.0D * elevEase);
                    c.caster.teleportTo(c.center.x, currentY, c.center.z);
                } else {
                    c.caster.teleportTo(c.center.x, targetElevY, c.center.z);
                }
                c.caster.setDeltaMovement(0, 0, 0);
                c.caster.resetFallDistance();

                // 1. Cập nhật xoay & phóng đại Ma Pháp Trận 3D (Long Tinh Bộc Viêm Bá)
                if (c.magicCircleDisplay != null && c.magicCircleDisplay.isAlive()) {
                    float circleScale;
                    if (c.currentTick < 50) {
                        float p = c.currentTick / 50.0F;
                        circleScale = 3.5F + (p * 22.0F); // 3.5m -> 25.5m
                    } else if (c.currentTick < 225) {
                        float p = (c.currentTick - 50.0F) / 175.0F;
                        circleScale = 25.5F + (p * 14.5F); // 25.5m -> 40.0m
                    } else {
                        circleScale = 40.0F;
                    }

                    Quaternionf rot = new Quaternionf()
                            .rotateX((float) Math.toRadians(90.0F))
                            .rotateZ((float) Math.toRadians(c.currentTick * 5.0F));
                    updateDisplayTransformation(c.magicCircleDisplay, circleScale, rot);
                }

                // ==============================================================
                // HỒI 1 (0 -> 50 ticks / 2.5s): LÃNH ĐỊA HẮC ÁM & KẾT GIỚI KHÔNG GIAN
                // ==============================================================
                if (c.currentTick <= 50) {
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

                    // Hạt tinh vân và plasma xoay tròn quanh tâm hội tụ
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
                // HỒI 3 (110 -> 224 ticks / ~5.75s): ĐIỂM TĨNH LẶNG TUYỆT ĐỐI ("LỜI CHÚC TỬ VONG")
                // ==============================================================
                else if (c.currentTick <= 224) {
                    // Đóng băng toàn bộ mục tiêu đang lơ lửng trên không
                    AABB freezeZone = new AABB(c.center.x - 60, c.center.y - 10, c.center.z - 60,
                            c.center.x + 60, c.center.y + 50, c.center.z + 60);
                    List<LivingEntity> frozenTargets = c.level.getEntitiesOfClass(LivingEntity.class, freezeZone,
                            e -> e.isAlive() && e != c.caster && !isAlliedToCaster(c.caster, e));

                    for (LivingEntity target : frozenTargets) {
                        target.setDeltaMovement(0, 0.02, 0);
                        target.hurtMarked = true;
                    }
                }

                // ==============================================================
                // HỒI 4 (225 -> 295 ticks / ~3.5s): ĐẠI BỘC PHÁT - PHÁN QUYẾT TUYỆT ĐỐI (INSTANT DEATH)
                // ==============================================================
                else {
                    // KÍCH NỔ CỰC ĐẠI TẠI TICK 225
                    if (c.currentTick == 225) {
                        // 1. Gửi Packet Detonate kích hoạt Impact Frames & Plasma Beam Column phía Client
                        ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                                c.caster.getUUID(), c.center, 295, ClientboundNihilityCinematicPacket.PHASE_DETONATE
                        );
                        ModMessages.sendToPlayer(packet, c.caster);
                        for (ServerPlayer nearbyPlayer : c.level.players()) {
                            if (nearbyPlayer != c.caster && nearbyPlayer.position().distanceToSqr(c.center) <= 128 * 128) {
                                ModMessages.sendToPlayer(packet, nearbyPlayer);
                            }
                        }

                        // 2. Âm thanh Atomic Blast cực nặng sấm sét chấn động
                        c.level.playSound(null, c.center.x, c.center.y, c.center.z,
                                ModSoundEvents.ATOMIC_BLAST.get(), SoundSource.PLAYERS, 10.0F, 1.0F);

                        // 3. Quét Sát Tử Tuyệt Đối (Instant Death - One-Hit Kill mọi sinh vật trong 70m)
                        AABB blastZone = new AABB(c.center.x - 70, c.center.y - 25, c.center.z - 70,
                                c.center.x + 70, c.center.y + 70, c.center.z + 70);
                        List<LivingEntity> blastTargets = c.level.getEntitiesOfClass(LivingEntity.class, blastZone,
                                e -> e.isAlive() && e != c.caster && !isAlliedToCaster(c.caster, e));

                        for (LivingEntity target : blastTargets) {
                            // Xóa sạch mọi lớp kháng sát thương, hiệu ứng buff, bất tử
                            target.removeAllEffects();
                            target.invulnerableTime = 0;

                            // Tiêu diệt lập tức (One-Hit Kill)
                            target.hurt(c.level.damageSources().magic(), Float.MAX_VALUE);
                            if (target.isAlive()) {
                                target.setHealth(0.0F);
                                target.die(c.level.damageSources().magic());
                            }

                            // Hiệu ứng bốc hơi linh hồn & rơi vật phẩm Linh Hồn (Mob Soul)
                            c.level.sendParticles(ParticleTypes.SOUL,
                                    target.getX(), target.getY() + 0.5, target.getZ(),
                                    20, 0.4, 0.6, 0.4, 0.05);
                            c.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    target.getX(), target.getY() + 0.5, target.getZ(),
                                    15, 0.3, 0.5, 0.3, 0.04);

                            target.spawnAtLocation(new ItemStack(ModItems.MOB_SOUL.get()));
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
                }

                // Dọn dẹp kết thúc ở tick 295
                if (c.currentTick >= c.maxTicks) {
                    // Cung cấp Slow Falling và Kháng ngã kéo dài để người chơi tự do quan sát trên không hoặc đáp xuống an toàn
                    c.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, false, false));
                    c.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2, false, false));
                    c.caster.resetFallDistance();

                    ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                            c.caster.getUUID(), c.center, 295, ClientboundNihilityCinematicPacket.PHASE_END
                    );
                    ModMessages.sendToPlayer(packet, c.caster);
                    for (ServerPlayer nearbyPlayer : c.level.players()) {
                        if (nearbyPlayer != c.caster && nearbyPlayer.position().distanceToSqr(c.center) <= 128 * 128) {
                            ModMessages.sendToPlayer(packet, nearbyPlayer);
                        }
                    }
                    c.cleanup();
                    iterator.remove();
                }
            }
        }
    }

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
            dispAcc.weapons$setViewRange(4.5F);

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
}
