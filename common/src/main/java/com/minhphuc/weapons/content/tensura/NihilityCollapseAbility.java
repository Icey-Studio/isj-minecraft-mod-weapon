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
import java.util.UUID;
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
        public final com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity demonCaster;
        public final Vec3 center;
        public final double targetElevY;
        public int currentTick = 0;
        public final int detonationTick;
        public final int maxTicks;

        // Ma Pháp Trận 3D Phát Quang Dưới Mặt Đất (Long Tinh Bộc Viêm Bá)
        public Display.ItemDisplay magicCircleDisplay = null;

        public ActiveCollapse(ServerLevel level, ServerPlayer caster, com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity demonCaster, Vec3 center, int detonationTick, int maxTicks) {
            this.level = level;
            this.caster = caster;
            this.demonCaster = demonCaster;
            this.center = center;
            this.detonationTick = detonationTick;
            this.maxTicks = maxTicks;

            // Xuyên lên mặt đất nếu người chơi thi triển trong hang / không gian kín
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) center.x, (int) center.z);
            if (surfaceY > center.y + 12) {
                this.targetElevY = Math.max(center.y + 28.0D, surfaceY + 12.0D);
            } else {
                this.targetElevY = center.y + 28.0D;
            }
        }

        public ActiveCollapse(ServerLevel level, ServerPlayer caster, Vec3 center) {
            this(level, caster, null, center, 225, 295);
        }

        public void cleanup() {
            if (magicCircleDisplay != null && magicCircleDisplay.isAlive()) {
                magicCircleDisplay.discard();
                magicCircleDisplay = null;
            }
            if (caster != null && caster.isAlive()) {
                caster.setNoGravity(false);
            }
            if (demonCaster != null && demonCaster.isAlive()) {
                demonCaster.setNoGravity(false);
            }
        }
    }

    public static final List<ActiveCollapse> ACTIVE_COLLAPSES = Collections.synchronizedList(new ArrayList<>());

    public static void castFromDemon(ServerLevel level, com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity demon, ServerPlayer owner) {
        if (demon == null || level == null) return;
        Vec3 center = demon.position();
        ActiveCollapse collapse = new ActiveCollapse(level, owner, demon, center, 35, 120);

        Vec3 groundPos = center.add(0, 0.05D, 0);
        collapse.magicCircleDisplay = createMagicCircleDisplay(level, groundPos, ModItems.BEELZEBUTH_MAGIC_CIRCLE.get(), 4.5F, 0x9400D3);
        ACTIVE_COLLAPSES.add(collapse);

        demon.setNoGravity(true);
        demon.setDeltaMovement(0, 0.35D, 0);

        ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                demon.getUUID(), center, collapse.targetElevY, 120, ClientboundNihilityCinematicPacket.PHASE_START
        );
        if (owner != null) ModMessages.sendToPlayer(packet, owner);
        for (ServerPlayer nearbyPlayer : level.players()) {
            if (nearbyPlayer != owner && nearbyPlayer.position().distanceToSqr(center) <= 128 * 128) {
                ModMessages.sendToPlayer(packet, nearbyPlayer);
            }
        }

        level.playSound(null, center.x, center.y, center.z,
                ModSoundEvents.ATOMIC_CHARGE.get(), SoundSource.PLAYERS, 3.0F, 1.0F);

        demon.announceSkill("LỜI CHÚC TỬ VONG (Death Blessing)");
        demon.broadcastNearbyDialogue("Vạn vật hãy cúi đầu trước phán quyết tuyệt đối của Quỷ Vương Thủy Tổ!", 48.0D);
    }

    public static void cast(ServerLevel level, ServerPlayer caster) {
        if (caster == null || level == null) return;

        Vec3 center = caster.position();

        ActiveCollapse collapse = new ActiveCollapse(level, caster, center);

        // 1. Khởi tạo Ma Pháp Trận 3D phát quang tím thẫm dưới mặt đất (Long Tinh Bộc Viêm Bá)
        Vec3 groundPos = center.add(0, 0.05D, 0);
        collapse.magicCircleDisplay = createMagicCircleDisplay(level, groundPos, ModItems.BEELZEBUTH_MAGIC_CIRCLE.get(), 3.5F, 0x9400D3);

        ACTIVE_COLLAPSES.add(collapse);

        // 2. Cung cấp trạng thái kháng sát thương và nâng trọng lực
        caster.setNoGravity(true);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 4, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));

        // 3. Gửi Packet kích hoạt Chuỗi Camera & Hiệu ứng Đồ Họa Đỉnh Cao cho người dùng và người xem (kèm targetElevY để client nội suy 144Hz)
        ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                caster.getUUID(), center, collapse.targetElevY, 295, ClientboundNihilityCinematicPacket.PHASE_START
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

                if (c.caster == null && c.demonCaster == null) {
                    c.cleanup();
                    iterator.remove();
                    continue;
                }
                if (c.caster != null && (!c.caster.isAlive() || c.caster.hasDisconnected())) {
                    c.cleanup();
                    iterator.remove();
                    continue;
                }
                if (c.demonCaster != null && (!c.demonCaster.isAlive() || c.demonCaster.isRemoved())) {
                    c.cleanup();
                    iterator.remove();
                    continue;
                }

                c.currentTick++;

                // 0. Cập nhật nâng ác ma (nếu ác ma thi triển) hoặc người chơi (nếu người chơi tự thi triển) bay lên cao
                double targetElevY = c.targetElevY;
                if (c.demonCaster != null) {
                    c.demonCaster.setNoGravity(true);
                    c.demonCaster.setDeltaMovement(0, 0, 0);
                    c.demonCaster.resetFallDistance();

                    if (c.currentTick <= 40) {
                        float elevProgress = c.currentTick / 40.0F;
                        float elevEase = elevProgress * elevProgress * (3.0F - 2.0F * elevProgress);
                        double currentY = c.center.y + ((targetElevY - c.center.y) * elevEase);
                        if (c.currentTick % 2 == 0 || c.currentTick == 40) {
                            c.demonCaster.teleportTo(c.center.x, currentY, c.center.z);
                        }
                    } else {
                        if (c.demonCaster.position().distanceToSqr(c.center.x, targetElevY, c.center.z) > 2.25) {
                            c.demonCaster.teleportTo(c.center.x, targetElevY, c.center.z);
                        }
                    }
                } else if (c.caster != null) {
                    c.caster.setNoGravity(true);
                    c.caster.setDeltaMovement(0, 0, 0);
                    c.caster.resetFallDistance();
                    c.caster.setAirSupply(c.caster.getMaxAirSupply());

                    if (c.currentTick <= 40) {
                        float elevProgress = c.currentTick / 40.0F;
                        float elevEase = elevProgress * elevProgress * (3.0F - 2.0F * elevProgress);
                        double currentY = c.center.y + ((targetElevY - c.center.y) * elevEase);
                        if (c.currentTick % 5 == 0 || c.currentTick == 40) {
                            c.caster.teleportTo(c.center.x, currentY, c.center.z);
                        }
                    } else {
                        if (c.caster.position().distanceToSqr(c.center.x, targetElevY, c.center.z) > 2.25) {
                            c.caster.teleportTo(c.center.x, targetElevY, c.center.z);
                        }
                    }
                }

                // 1. Cập nhật xoay & phóng đại Ma Pháp Trận 3D (Cập nhật mỗi 3 ticks + nội suy mượt trên client, giảm 67% packet metadata)
                if (c.magicCircleDisplay != null && c.magicCircleDisplay.isAlive()) {
                    if (c.currentTick % 3 == 0) {
                        float circleScale;
                        if (c.currentTick < c.detonationTick) {
                            float p = c.currentTick / (float) c.detonationTick;
                            circleScale = 3.5F + (p * 36.5F); // 3.5m -> 40.0m
                        } else {
                            circleScale = 40.0F;
                        }

                        Quaternionf rot = new Quaternionf()
                                .rotateX((float) Math.toRadians(90.0F))
                                .rotateZ((float) Math.toRadians(c.currentTick * 5.0F));
                        updateDisplayTransformation(c.magicCircleDisplay, circleScale, rot);
                    }
                }

                // ==============================================================
                // HỒI 1 & 2: LÃNH ĐỊA HẮC ÁM & HÚT MỤC TIÊU LƠ LỬNG
                // ==============================================================
                if (c.currentTick < c.detonationTick) {
                    Vec3 riftCenter = c.center.add(0, 25, 0);
                    if (c.currentTick % 4 == 0) {
                        AABB vacuumZone = new AABB(c.center.x - 65, c.center.y - 15, c.center.z - 65,
                                c.center.x + 65, c.center.y + 50, c.center.z + 65);
                        List<LivingEntity> vacuumTargets = c.level.getEntitiesOfClass(LivingEntity.class, vacuumZone,
                                e -> e.isAlive() && !isAlliedToCollapseCaster(c, e));

                        for (LivingEntity target : vacuumTargets) {
                            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 1, false, false));
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5, false, false));
                            Vec3 toRift = riftCenter.subtract(target.position());
                            double dist = toRift.length();
                            if (dist > 1.5) {
                                Vec3 liftVelocity = toRift.normalize().scale(0.55).add(0, 0.35, 0);
                                target.setDeltaMovement(liftVelocity);
                                target.hurtMarked = true;
                            }
                        }
                    }

                    if (c.currentTick % 3 == 0 && c.level.hasNearbyAlivePlayer(c.center.x, c.center.y, c.center.z, 64.0)) {
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.toRadians((i * 45.0) + (c.currentTick * 12.0));
                            double px = c.center.x + Math.cos(angle) * 12.0;
                            double pz = c.center.z + Math.sin(angle) * 12.0;
                            c.level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, c.center.y + 2.0, pz, 2, 0.2, 0.2, 0.2, 0.05);
                            c.level.sendParticles(ParticleTypes.DRAGON_BREATH, px, c.center.y + 2.0, pz, 1, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }

                // ==============================================================
                // HỒI 4: ĐẠI BỘC PHÁT - PHÁN QUYẾT TUYỆT ĐỐI (INSTANT DEATH & CỘT PLASMA TỬ VONG)
                // ==============================================================
                else {
                    // KÍCH NỔ CỰC ĐẠI TẠI TICK detonationTick
                    if (c.currentTick == c.detonationTick) {
                        UUID casterUuid = c.demonCaster != null ? c.demonCaster.getUUID() : (c.caster != null ? c.caster.getUUID() : UUID.randomUUID());
                        ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                                casterUuid, c.center, c.targetElevY, c.maxTicks, ClientboundNihilityCinematicPacket.PHASE_DETONATE
                        );
                        if (c.caster != null) ModMessages.sendToPlayer(packet, c.caster);
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
                                e -> e.isAlive() && !isAlliedToCollapseCaster(c, e));

                        for (LivingEntity target : blastTargets) {
                            target.removeAllEffects();
                            target.invulnerableTime = 0;
                            target.hurt(c.level.damageSources().magic(), Float.MAX_VALUE);
                            if (target.isAlive()) {
                                target.setHealth(0.0F);
                                target.die(c.level.damageSources().magic());
                            }

                            c.level.sendParticles(ParticleTypes.SOUL,
                                    target.getX(), target.getY() + 0.5, target.getZ(),
                                    8, 0.3, 0.4, 0.3, 0.03);
                            c.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    target.getX(), target.getY() + 0.5, target.getZ(),
                                    6, 0.2, 0.3, 0.2, 0.02);

                            target.spawnAtLocation(new ItemStack(ModItems.MOB_SOUL.get()));
                        }

                        // 4. Sóng Xung Kích Hạt Nở Rộng (2 vòng Sonic Boom sắc bén + Spiritron Cyan)
                        for (int ring = 1; ring <= 4; ring++) {
                            double radius = ring * 15.0;
                            for (int i = 0; i < 16; i++) {
                                double angle = Math.toRadians(i * 22.5);
                                double px = c.center.x + Math.cos(angle) * radius;
                                double pz = c.center.z + Math.sin(angle) * radius;
                                c.level.sendParticles(SPIRITRON_CYAN, px, c.center.y + 0.5, pz, 1, 0.1, 0.1, 0.1, 0.05);
                                if (ring <= 2 && i % 2 == 0) {
                                    c.level.sendParticles(ParticleTypes.SONIC_BOOM, px, c.center.y + 1.0, pz, 1, 0, 0, 0, 0);
                                }
                            }
                        }

                        if (c.demonCaster != null && c.demonCaster.getDemonType() == com.minhphuc.weapons.entity.tensura.DemonType.JAUNE) {
                            c.demonCaster.applyCarreraBlockDestruction(c.center, 4.0F);
                        }
                    }
                }

                // Dọn dẹp kết thúc
                if (c.currentTick >= c.maxTicks) {
                    if (c.demonCaster != null && c.demonCaster.isAlive()) {
                        c.demonCaster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0, false, false));
                        c.demonCaster.resetFallDistance();
                    } else if (c.caster != null) {
                        c.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, false, false));
                        c.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2, false, false));
                        c.caster.resetFallDistance();
                    }

                    UUID casterUuid = c.demonCaster != null ? c.demonCaster.getUUID() : (c.caster != null ? c.caster.getUUID() : UUID.randomUUID());
                    ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                            casterUuid, c.center, c.targetElevY, c.maxTicks, ClientboundNihilityCinematicPacket.PHASE_END
                    );
                    if (c.caster != null) ModMessages.sendToPlayer(packet, c.caster);
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
            dispAcc.weapons$setTransformationInterpolationDuration(3);

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

    public static void cancelAll(ServerLevel level) {
        synchronized (ACTIVE_COLLAPSES) {
            Iterator<ActiveCollapse> it = ACTIVE_COLLAPSES.iterator();
            while (it.hasNext()) {
                ActiveCollapse c = it.next();
                if (c.level == level) {
                    c.cleanup();
                    ClientboundNihilityCinematicPacket packet = new ClientboundNihilityCinematicPacket(
                            c.caster.getUUID(), c.center, 295, ClientboundNihilityCinematicPacket.PHASE_END
                    );
                    ModMessages.sendToPlayer(packet, c.caster);
                    it.remove();
                }
            }
        }
    }

    private static boolean isAlliedToCaster(ServerPlayer caster, LivingEntity entity) {
        if (caster == null || entity == null) return false;
        if (com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.isContractedAllyOfPlayer(caster.getUUID(), entity)) return true;
        return entity.isAlliedTo(caster) || entity.getUUID().equals(caster.getUUID());
    }

    private static boolean isAlliedToCollapseCaster(ActiveCollapse c, LivingEntity entity) {
        if (entity == null) return true;
        if (c.caster != null) {
            return isAlliedToCaster(c.caster, entity);
        }
        if (c.demonCaster != null) {
            return c.demonCaster.isImmuneOrFriendly(entity);
        }
        return false;
    }
}
