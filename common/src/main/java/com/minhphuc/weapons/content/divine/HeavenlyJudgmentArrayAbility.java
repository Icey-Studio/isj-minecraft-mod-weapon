package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tuyệt kĩ: Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)
 * Kịch bản:
 * 1. Khởi tạo (Tick 0..11): 6 Vòng sáng vệ tinh xuất hiện tạo Lục Giác Trận trên trời cao (bán kính 9m).
 * 2. Lồng Giam Thánh Quang (Tick 12): 6 Cột sáng vệ tinh đồng loạt giáng xuống giam cầm kẻ địch bên trong.
 * 3. Lốc Xoáy Thánh Quang (Tick 13..39): Ma trận xoay tròn, hút toàn bộ mục tiêu về phía tâm chấn.
 * 4. Đại Thiên Trụ Giáng Thế (Tick 40): Cột sáng trung tâm cực đại (6x6) giáng ầm xuống tâm trận,
 *    kích nổ toàn bộ ma trận tạo biển hạt sao và sát thương hủy diệt diện rộng!
 */
public class HeavenlyJudgmentArrayAbility {

    public static final int NUM_SATELLITES = 8; // Bát Môn: 8 cột sáng vệ tinh
    public static final int[] SATELLITE_COLORS = new int[]{
            0xFF2222, // 1. Đỏ Hỏa Ngục (Rouge)
            0xFF7700, // 2. Cam Viêm Dương
            0xFFD700, // 3. Vàng Kim Quang (Jaune)
            0x00FF66, // 4. Lục Phong Bão (Vert)
            0x00D0FF, // 5. Lam Băng Cực (Bleu)
            0x3366FF, // 6. Lam Sâu Ma Pháp
            0xAA00FF, // 7. Tử Độc Hư Không (Violet)
            0xF0F4F8  // 8. Bạch Sắc Quang Minh (Blanc)
    };

    public static class ActiveArray {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public final List<Display.ItemDisplay> satelliteDisplays = new ArrayList<>();
        public Display.ItemDisplay centralDisplay;
        public Display.ItemDisplay magicCircleDisplay; // Vòng tròn ma thuật to ở tâm trận
        public float circleAngle = 0.0F;
        public final ItemStack magicCircleStack;
        public final int magicCircleGlowColor;
        public final int centralBeamColor;
        public int ticksRemaining;
        public final int totalTicks;
        public float rotationAngleDegrees = 0.0F;
        public boolean satellitesSpawned = false;
        public boolean centralBeamSpawned = false;

        public final SkillPowerRoll powerRoll;

        public ActiveArray(ServerLevel level, ServerPlayer caster, Vec3 center, int durationTicks,
                           SkillPowerRoll powerRoll, ItemStack circleStack, int circleGlow, int centralColor) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.ticksRemaining = durationTicks;
            this.totalTicks = durationTicks;
            this.powerRoll = powerRoll;
            this.magicCircleStack = circleStack;
            this.magicCircleGlowColor = circleGlow;
            this.centralBeamColor = centralColor;
        }

        public void cleanupDisplays() {
            for (Display.ItemDisplay d : satelliteDisplays) {
                if (d != null && d.isAlive()) d.discard();
            }
            satelliteDisplays.clear();

            if (centralDisplay != null && centralDisplay.isAlive()) {
                centralDisplay.discard();
                centralDisplay = null;
            }

            if (magicCircleDisplay != null && magicCircleDisplay.isAlive()) {
                magicCircleDisplay.discard();
                magicCircleDisplay = null;
            }
        }
    }

    private static final List<ActiveArray> ACTIVE_ARRAYS = new ArrayList<>();

    public static void cast(ServerLevel level, ServerPlayer player, ItemStack sword) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        double maxDistance = 28.0D;
        Vec3 traceEnd = eyePos.add(lookVec.scale(maxDistance));

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, traceEnd,
                new AABB(eyePos, traceEnd).inflate(2.0D),
                e -> !e.isSpectator() && e.isPickable() && e != player,
                maxDistance * maxDistance
        );

        Vec3 targetCenter;
        if (entityHit != null && entityHit.getEntity() != null) {
            targetCenter = findGroundBelow(level, entityHit.getEntity().position());
        } else {
            BlockHitResult hitResult = level.clip(new ClipContext(
                    eyePos, traceEnd,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                if (hitResult.getDirection() == Direction.UP) {
                    BlockPos bp = hitResult.getBlockPos();
                    targetCenter = new Vec3(bp.getX() + 0.5D, bp.getY() + 1.0D, bp.getZ() + 0.5D);
                } else {
                    targetCenter = findGroundBelow(level, hitResult.getLocation());
                }
            } else {
                Vec3 forwardPos = eyePos.add(lookVec.scale(18.0D));
                targetCenter = findGroundBelow(level, forwardPos);
            }
        }

        // Xác định loại ác ma và vòng tròn ma thuật tương ứng
        ItemStack circleStack;
        int circleGlow;
        int centralColor;

        boolean isPrimordial = com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.isPrimordial(player);
        com.minhphuc.weapons.entity.tensura.DemonType demonType = isPrimordial
                ? com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.getPrimordialType(player)
                : null;

        if (demonType != null) {
            circleStack = new ItemStack(demonType.getMagicCircleItem().get());
            circleGlow = demonType.getGlowColor();
            centralColor = demonType.getGlowColor();
        } else {
            circleStack = new ItemStack(ModItems.DISINTEGRATION_MAGIC_CIRCLE.get());
            circleGlow = 0xFFD700;
            centralColor = 0xFFFF77;
        }

        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Bát Môn Thiên Phạt Trận (Judgment Array)");

        ActiveArray array = new ActiveArray(level, player, targetCenter, 75, roll, circleStack, circleGlow, centralColor);

        // Triệu hồi Vòng Tròn Ma Thuật TO ở giữa tâm trận ngay khi khai mở
        array.magicCircleDisplay = createMagicCircleDisplay(level, targetCenter.add(0, 0.08D, 0), 18.0F, circleGlow, circleStack);

        ACTIVE_ARRAYS.add(array);

        // Âm thanh thánh ca & ma pháp cộng hưởng khởi động
        level.playSound(null, targetCenter.x, targetCenter.y + 10.0D, targetCenter.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.6F);
        level.playSound(null, targetCenter.x, targetCenter.y + 10.0D, targetCenter.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 4.0F, 1.1F);
        level.playSound(null, targetCenter.x, targetCenter.y + 1.0D, targetCenter.z,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 3.5F, 0.8F);

        if (sword != null && !sword.isEmpty()) {
            player.getCooldowns().addCooldown(sword.getItem(), 200); // 10s cooldown
        }
    }

    private static Vec3 findGroundBelow(ServerLevel level, Vec3 pos) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(
                Math.floor(pos.x),
                Math.floor(pos.y),
                Math.floor(pos.z)
        );
        int upLimit = 0;
        while (isSolid(level, mpos) && upLimit < 10 && mpos.getY() < level.getMaxBuildHeight()) {
            mpos.move(Direction.UP);
            upLimit++;
        }
        int downLimit = 0;
        while (!isSolid(level, mpos) && downLimit < 60 && mpos.getY() > level.getMinBuildHeight()) {
            mpos.move(Direction.DOWN);
            downLimit++;
        }
        return new Vec3(pos.x, mpos.getY() + 1.0D, pos.z);
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.blocksMotion();
    }

    public static void tickArrays(ServerLevel serverLevel) {
        if (ACTIVE_ARRAYS.isEmpty()) return;

        Iterator<ActiveArray> it = ACTIVE_ARRAYS.iterator();
        while (it.hasNext()) {
            ActiveArray a = it.next();
            if (a.level != serverLevel) continue;

            a.ticksRemaining--;
            int elapsed = a.totalTicks - a.ticksRemaining;
            double groundY = a.center.y;
            double satelliteRadius = 9.0D;

            // =========================================================================
            // XOAY VÒNG TRÒN MA THUẬT Ở TÂM TRẬN
            // =========================================================================
            if (a.magicCircleDisplay != null && a.magicCircleDisplay.isAlive()) {
                a.circleAngle += 2.5F;
                if (elapsed % 2 == 0) {
                    DisplayAccessor dispAcc = (DisplayAccessor) a.magicCircleDisplay;
                    Quaternionf rot = new Quaternionf()
                            .rotateX((float) Math.toRadians(90.0F))
                            .rotateZ((float) Math.toRadians(a.circleAngle));
                    dispAcc.weapons$setTransformation(new Transformation(
                            new Vector3f(0.0F, 0.0F, 0.0F),
                            rot,
                            new Vector3f(18.0F, 18.0F, 0.01F),
                            null
                    ));
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 1: BÁO HIỆU 8 CỘT VỆ TINH TRÊN TRỜI (Tick 0..11)
            // =========================================================================
            if (elapsed < 12) {
                for (int i = 0; i < NUM_SATELLITES; i++) {
                    double angle = Math.toRadians(i * (360.0D / NUM_SATELLITES));
                    double px = a.center.x + Math.cos(angle) * satelliteRadius;
                    double pz = a.center.z + Math.sin(angle) * satelliteRadius;
                    for (int yStep = 0; yStep < 6; yStep++) {
                        a.level.sendParticles(ParticleTypes.END_ROD, px, groundY + (yStep * 4.0D), pz, 1, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 2: 8 CỘT SÁNG ĐA SẮC MÀU VỆ TINH GIÁNG XUỐNG TẠO LỒNG GIAM (Tick 12)
            // =========================================================================
            if (elapsed >= 12 && !a.satellitesSpawned) {
                a.satellitesSpawned = true;
                for (int i = 0; i < NUM_SATELLITES; i++) {
                    double angle = Math.toRadians(i * (360.0D / NUM_SATELLITES));
                    double px = a.center.x + Math.cos(angle) * satelliteRadius;
                    double pz = a.center.z + Math.sin(angle) * satelliteRadius;
                    int col = SATELLITE_COLORS[i % SATELLITE_COLORS.length];
                    Display.ItemDisplay sat = createPillarDisplay(a.level, new Vec3(px, groundY, pz), 2.2F, 40.0F, 2.2F, col);
                    if (sat != null) a.satelliteDisplays.add(sat);
                }

                a.level.playSound(null, a.center.x, groundY, a.center.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 1.4F);
                a.level.playSound(null, a.center.x, groundY, a.center.z,
                        SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 3.5F, 0.8F);
            }

            // =========================================================================
            // GIAI ĐOẠN 3: LỐC XOÁY THÁNH QUANG XOAY TRÒN & HÚT QUÁI (Tick 13..39)
            // =========================================================================
            if (elapsed >= 12 && elapsed < 40) {
                a.rotationAngleDegrees += 4.5F;

                // Cập nhật vị trí 8 cột sáng vệ tinh xoay quanh tâm
                for (int i = 0; i < a.satelliteDisplays.size(); i++) {
                    Display.ItemDisplay sat = a.satelliteDisplays.get(i);
                    if (sat != null && sat.isAlive()) {
                        double currentAngle = Math.toRadians((i * (360.0D / NUM_SATELLITES)) + a.rotationAngleDegrees);
                        double nx = a.center.x + Math.cos(currentAngle) * satelliteRadius;
                        double nz = a.center.z + Math.sin(currentAngle) * satelliteRadius;
                        sat.moveTo(nx, groundY, nz, 0.0F, 0.0F);

                        // Hạt nối các cột tạo thành mạng lưới bát giác ánh sáng
                        int nextIdx = (i + 1) % NUM_SATELLITES;
                        double nextAngle = Math.toRadians((nextIdx * (360.0D / NUM_SATELLITES)) + a.rotationAngleDegrees);
                        double nextX = a.center.x + Math.cos(nextAngle) * satelliteRadius;
                        double nextZ = a.center.z + Math.sin(nextAngle) * satelliteRadius;

                        if (elapsed % 3 == 0) {
                            for (double t = 0.2D; t <= 0.8D; t += 0.2D) {
                                double lx = nx + (nextX - nx) * t;
                                double lz = nz + (nextZ - nz) * t;
                                a.level.sendParticles(ParticleTypes.WAX_OFF, lx, groundY + 1.0D, lz, 1, 0, 0, 0, 0.02D);
                            }
                        }
                    }
                }

                // Lực hút lốc xoáy ánh sáng kéo toàn bộ kẻ địch về phía tâm chấn
                AABB cageZone = new AABB(a.center.x - 11.0D, groundY - 1.0D, a.center.z - 11.0D,
                        a.center.x + 11.0D, groundY + 25.0D, a.center.z + 11.0D);
                List<LivingEntity> trapped = a.level.getEntitiesOfClass(LivingEntity.class, cageZone, e -> e != a.caster && e.isAlive());

                for (LivingEntity v : trapped) {
                    double dx = a.center.x - v.getX();
                    double dz = a.center.z - v.getZ();

                    // Ghim và cuốn xoáy về tâm
                    Vec3 vortex = new Vec3(dx, 0, dz).normalize().scale(0.35D);
                    // Vectơ vuông góc tạo lực xoay
                    Vec3 tangent = new Vec3(-dz, 0, dx).normalize().scale(0.2D);
                    v.setDeltaMovement(vortex.add(tangent).add(0, -0.1D, 0));
                    v.hasImpulse = true;

                    v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 255, false, false, true));
                    v.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, -255, false, false, true));
                    v.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, false, false, true));
                }

                // Triệt tiêu đạn đạo bên trong lồng giam
                List<Projectile> projectiles = a.level.getEntitiesOfClass(Projectile.class, cageZone);
                for (Projectile p : projectiles) {
                    a.level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
                    p.discard();
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 4: ĐẠI THIÊN TRỤ GIÁNG THẾ & KÍCH NỔ TOÀN BỘ MA TRẬN (Tick 40)
            // =========================================================================
            if (elapsed >= 40 && !a.centralBeamSpawned) {
                a.centralBeamSpawned = true;

                // Triệu hồi Cột Sáng Trung Tâm Khổng Lồ (6x6) theo màu sắc của Ác Ma Thủy Tổ
                a.centralDisplay = createPillarDisplay(a.level, a.center, 6.0F, 50.0F, 6.0F, a.centralBeamColor);

                // Âm thanh đại thiên phạt
                a.level.playSound(null, a.center.x, groundY, a.center.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 0.85F);
                a.level.playSound(null, a.center.x, groundY, a.center.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 1.1F);
                a.level.playSound(null, a.center.x, groundY, a.center.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F, 0.7F);

                // Thông báo xuất chiêu
                if (a.caster != null) {
                    a.caster.displayClientMessage(
                            Component.literal("§6§l[BÁT MÔN THIÊN PHẠT TRẬN] §e§lĐại Thiên Trụ Kích Nổ Trận Đồ Tối Thượng! ⚡💥"),
                            true
                    );
                }

                // Phá hủy địa hình tâm chấn cực mạnh
                a.level.explode(a.caster, a.center.x, groundY, a.center.z, 7.0F, Level.ExplosionInteraction.BLOCK);

                // Sát thương hủy diệt diện rộng toàn bộ mục tiêu trong phạm vi trận đồ (Bán kính 12m)
                AABB detonationZone = new AABB(a.center.x - 12.0D, groundY - 2.0D, a.center.z - 12.0D,
                        a.center.x + 12.0D, groundY + 30.0D, a.center.z + 12.0D);
                List<LivingEntity> victims = a.level.getEntitiesOfClass(LivingEntity.class, detonationZone, e -> e != a.caster && e.isAlive());
                DamageSource dmgSource = (a.caster != null) ? a.level.damageSources().playerAttack(a.caster) : a.level.damageSources().magic();

                for (LivingEntity victim : victims) {
                    victim.removeEffect(MobEffects.MOVEMENT_SPEED);
                    victim.removeEffect(MobEffects.DAMAGE_BOOST);
                    victim.removeEffect(MobEffects.REGENERATION);
                    victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                    victim.removeEffect(MobEffects.FIRE_RESISTANCE);
                    victim.removeEffect(MobEffects.ABSORPTION);

                    victim.setRemainingFireTicks(180);

                    if (a.caster != null) {
                        com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(a.caster, victim);
                    }

                    if (a.powerRoll != null && a.powerRoll.isOverdrive()) {
                        victim.hurt(dmgSource, 8000.0F);
                        if (victim.isAlive() && (victim.getMaxHealth() >= 100.0F || victim.isInvertedHealAndHarm())) {
                            victim.discard();
                        }
                    } else if (a.powerRoll != null && a.powerRoll.isNormal()) {
                        victim.hurt(dmgSource, 2500.0F * a.powerRoll.multiplier);
                    } else {
                        victim.hurt(dmgSource, 800.0F * (a.powerRoll != null ? a.powerRoll.multiplier : 0.35F));
                    }

                    a.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.2D);
                }

                // Tiêu biến dần các cột vệ tinh sau vụ nổ lớn
                for (Display.ItemDisplay sat : a.satelliteDisplays) {
                    if (sat != null && sat.isAlive()) {
                        a.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sat.getX(), groundY + 1.0D, sat.getZ(), 1, 0, 0, 0, 0);
                        sat.discard();
                    }
                }
                a.satelliteDisplays.clear();
            }

            // =========================================================================
            // GIAI ĐOẠN 5: DỌN DẸP & KẾT THÚC (Tick >= 70)
            // =========================================================================
            if (elapsed >= 70) {
                a.level.sendParticles(ParticleTypes.POOF, a.center.x, groundY + 1.0D, a.center.z, 15, 2.0D, 0.5D, 2.0D, 0.05D);
                a.cleanupDisplays();
                it.remove();
            }
        }
    }

    private static Display.ItemDisplay createMagicCircleDisplay(ServerLevel level, Vec3 center,
                                                               float scale, int glowColor, ItemStack stack) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(stack);
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            display.addTag("DemonMagicCircle");
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(14.0F);

            Quaternionf rot = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rot,
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
            com.minhphuc.weapons.content.tensura.ResidualMagicCircleManager.registerCircle(display);
        }
        return display;
    }

    private static Display.ItemDisplay createPillarDisplay(ServerLevel level, Vec3 center,
                                                          float scaleX, float scaleY, float scaleZ,
                                                          int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.JACOB_LIGHT_PILLAR.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, scaleY / 2.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(scaleX, scaleY, scaleZ),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }
}
