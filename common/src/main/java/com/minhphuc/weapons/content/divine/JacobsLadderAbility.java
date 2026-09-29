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
 * Tuyệt kĩ: Tà Khứ Vũ Thê Tử (Jacob's Ladder - 邪去侮の梯子) - Chuẩn nguyên tác Jujutsu Kaisen
 * Kịch bản:
 * 1. Khởi xướng (Tick 0..14 - ~0.75s):
 *    - Vòng Hào Quang Thiên Sứ (Angel Halo) xuất hiện trên bầu trời cao rực rỡ, kèn hiệu thiên thần vang rền.
 *    - Khóa chân tức thì (Binding of Light): Kẻ địch trong phạm vi 4x4 lập tức bị ghim chặt xuống đất, không thể chạy thoát.
 * 2. Thiên Phạt Giáng Thế & Sát Thương Duy Nhất (Tick 15):
 *    - Cột Sáng 4x4 chọc trời giáng thẳng từ mây xuống mặt đất chuẩn xác.
 *    - Gây sát thương Xuất Lực Tối Đa (Maximum Output) đúng 1 lần duy nhất theo SkillPowerRoll.
 *    - Tước bỏ toàn bộ bùa chú có lợi, thiêu rụi tà thuật trong lửa thánh.
 * 3. Duy Trì Thánh Quang & Thanh Tẩy (Tick 16..65):
 *    - Cột sáng duy trì 2.5s triệt tiêu mọi đạn đạo (Projectile), không gây thêm bất kỳ sát thương lần 2 nào.
 * 4. Kết thúc (Tick >= 65): Tiêu biến trong thanh bình.
 */
public class JacobsLadderAbility {

    public static class ActiveLadder {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public final List<Display.ItemDisplay> beamDisplays = new ArrayList<>();
        public int ticksRemaining;
        public final int totalTicks;
        public boolean beamSpawned = false;
        public boolean damageDealt = false;

        public final SkillPowerRoll powerRoll;

        public ActiveLadder(ServerLevel level, ServerPlayer caster, Vec3 center, int durationTicks, SkillPowerRoll powerRoll) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.ticksRemaining = durationTicks;
            this.totalTicks = durationTicks;
            this.powerRoll = powerRoll;
        }

        public void cleanupDisplays() {
            for (Display.ItemDisplay d : beamDisplays) {
                if (d != null && d.isAlive()) {
                    d.discard();
                }
            }
            beamDisplays.clear();
        }
    }

    private static final List<ActiveLadder> ACTIVE_LADDERS = new ArrayList<>();

    public static boolean isNearActiveLadder(ServerLevel level, Vec3 pos, double radius) {
        double rSq = radius * radius;
        for (ActiveLadder l : ACTIVE_LADDERS) {
            if (l.level == level && l.center.distanceToSqr(pos) <= rSq) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 getClosestLadderCenter(ServerLevel level, Vec3 pos, double radius) {
        double rSq = radius * radius;
        double closestDist = Double.MAX_VALUE;
        Vec3 closest = null;
        for (ActiveLadder l : ACTIVE_LADDERS) {
            if (l.level == level) {
                double d = l.center.distanceToSqr(pos);
                if (d <= rSq && d < closestDist) {
                    closestDist = d;
                    closest = l.center;
                }
            }
        }
        return closest;
    }

    /**
     * Kích hoạt tuyệt kĩ: Tà Khứ Vũ Thê Tử (Jacob's Ladder)
     */
    public static void cast(ServerLevel level, ServerPlayer player, ItemStack sword) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        double maxDistance = 26.0D;
        Vec3 traceEnd = eyePos.add(lookVec.scale(maxDistance));

        // 1. Dò tìm mục tiêu: Ưu tiên bắt trúng Entity trong tầm ngắm
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, traceEnd,
                new AABB(eyePos, traceEnd).inflate(2.0D),
                e -> !e.isSpectator() && e.isPickable() && e != player,
                maxDistance * maxDistance
        );

        Vec3 targetCenter;
        if (entityHit != null && entityHit.getEntity() != null) {
            Entity targetEntity = entityHit.getEntity();
            targetCenter = findGroundBelow(level, targetEntity.position());
        } else {
            // 2. Nếu không trúng entity, dò tìm khối địa hình bằng block raycast
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
                // Nhắm vào không khí -> Chiếu thẳng xuống mặt sàn bên dưới
                Vec3 forwardAirPos = eyePos.add(lookVec.scale(16.0D));
                targetCenter = findGroundBelow(level, forwardAirPos);
            }
        }

        // Gieo xúc xắc xuất lực ngẫu nhiên
        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Tà Khứ Vũ Thê Tử (Jacob's Ladder)");

        // Tổng thời lượng 65 ticks (~3.25 giây: 15 ticks gọi thiên sứ + 50 ticks cột sáng)
        ActiveLadder ladder = new ActiveLadder(level, player, targetCenter, 65, roll);

        ACTIVE_LADDERS.add(ladder);

        // Âm thanh Kèn Hiệu Thiên Thần & Thánh Ca mở màn
        level.playSound(null, targetCenter.x, targetCenter.y + 15.0D, targetCenter.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 4.0F, 1.2F);
        level.playSound(null, targetCenter.x, targetCenter.y + 15.0D, targetCenter.z,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 3.5F, 1.6F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 3.0F, 1.5F);

        player.getCooldowns().addCooldown(sword.getItem(), 160); // 8 giây hồi chiêu
    }

    /**
     * Dò tìm mặt sàn cứng (solid block) thẳng đứng dưới điểm ngắm,
     * khắc phục triệt để lỗi getHeightmapPos bị văng lên nóc nhà/nóc hang/Nether.
     */
    private static Vec3 findGroundBelow(ServerLevel level, Vec3 pos) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(
                Math.floor(pos.x),
                Math.floor(pos.y),
                Math.floor(pos.z)
        );

        // Nếu điểm ngắm đang nằm sâu trong khối đặc, dời lên tìm khoảng không
        int upLimit = 0;
        while (isSolid(level, mpos) && upLimit < 10 && mpos.getY() < level.getMaxBuildHeight()) {
            mpos.move(Direction.UP);
            upLimit++;
        }

        // Dò thẳng xuống dưới tìm khối sàn đầu tiên
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

    /**
     * Cập nhật Cột Sáng Jacob mỗi tick
     */
    public static void tickLadders(ServerLevel serverLevel) {
        if (ACTIVE_LADDERS.isEmpty()) return;

        Iterator<ActiveLadder> it = ACTIVE_LADDERS.iterator();
        while (it.hasNext()) {
            ActiveLadder l = it.next();
            if (l.level != serverLevel) continue;

            l.ticksRemaining--;
            int elapsed = l.totalTicks - l.ticksRemaining;

            double halfSize = 2.0D;
            double minX = l.center.x - halfSize;
            double maxX = l.center.x + halfSize;
            double minZ = l.center.z - halfSize;
            double maxZ = l.center.z + halfSize;
            double groundY = l.center.y;
            double topY = l.center.y + 50.0D;
            AABB pillarBox = new AABB(minX, groundY, minZ, maxX, topY, maxZ);

            // =========================================================================
            // GIAI ĐOẠN 1: KHỞI XƯỚNG & KHÓA CHÂN THÁNH QUANG (Tick 0..14 - ~0.75s)
            // =========================================================================
            if (elapsed < 15) {
                // Luồng hạt thánh quang kết nối từ trời rọi thẳng xuống tâm đất
                for (int step = 0; step < 7; step++) {
                    double py = groundY + (step * 4.0D);
                    l.level.sendParticles(ParticleTypes.END_ROD, l.center.x, py, l.center.z, 2, 0.3D, 0.2D, 0.3D, 0.02D);
                }

                // Vòng sáng cảnh báo mặt đất
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians((elapsed * 24.0D + i * 60.0D) % 360.0D);
                    double gx = l.center.x + Math.cos(angle) * 2.2D;
                    double gz = l.center.z + Math.sin(angle) * 2.2D;
                    l.level.sendParticles(ParticleTypes.WAX_OFF, gx, groundY + 0.08D, gz, 1, 0, 0.01D, 0, 0.01D);
                }

                // KHÓA CHÂN TỨC THÌ (Light Binding): Ghim chặt mục tiêu không cho chạy thoát
                AABB bindZone = new AABB(l.center.x - 3.5D, groundY - 1.0D, l.center.z - 3.5D,
                        l.center.x + 3.5D, groundY + 30.0D, l.center.z + 3.5D);
                List<LivingEntity> boundVictims = l.level.getEntitiesOfClass(LivingEntity.class, bindZone, e -> e != l.caster && e.isAlive());
                for (LivingEntity v : boundVictims) {
                    v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 255, false, false, true));
                    v.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, -255, false, false, true));
                    v.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, false, false, true));

                    double dx = l.center.x - v.getX();
                    double dz = l.center.z - v.getZ();
                    v.setDeltaMovement(dx * 0.15D, -0.25D, dz * 0.15D);
                    v.hasImpulse = true;

                    l.level.sendParticles(ParticleTypes.WAX_OFF, v.getX(), v.getY() + 1.0D, v.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.02D);
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 2: CỘT SÁNG GIÁNG THẾ & SÁT THƯƠNG TỐI ĐA DUY NHẤT 1 LẦN (Tick 15)
            // =========================================================================
            if (elapsed >= 15 && !l.beamSpawned) {
                l.beamSpawned = true;

                // 1. Cột sáng vỏ ngoài hình vuông 4x4, cao 50m lên trời
                Display.ItemDisplay outerBeam = createBeamDisplay(l.level, l.center, 4.0F, 50.0F, 4.0F, 0xFFFFFF);
                if (outerBeam != null) l.beamDisplays.add(outerBeam);

                // 2. Lõi sáng trung tâm 2.6x2.6 trắng chói lòa
                Display.ItemDisplay innerCore = createBeamDisplay(l.level, l.center, 2.6F, 50.0F, 2.6F, 0xFFFFFF);
                if (innerCore != null) l.beamDisplays.add(innerCore);

                // Âm thanh thiên phạt chấn động thế giới
                l.level.playSound(null, l.center.x, groundY, l.center.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 0.95F);
                l.level.playSound(null, l.center.x, groundY, l.center.z,
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.4F);
                l.level.playSound(null, l.center.x, groundY, l.center.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.3F);
                l.level.playSound(null, l.center.x, groundY, l.center.z,
                        SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 3.5F, 0.6F);

                // Hiệu ứng chớp sáng cực đại từ trời giáng xuống đất
                for (double y = groundY; y <= groundY + 50.0D; y += 2.0D) {
                    l.level.sendParticles(ParticleTypes.FLASH, l.center.x, y, l.center.z, 1, 0.4D, 0.4D, 0.4D, 0);
                    l.level.sendParticles(ParticleTypes.END_ROD, l.center.x, y, l.center.z, 2, 0.8D, 0.2D, 0.8D, 0.05D);
                }
                l.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, l.center.x, groundY + 1.0D, l.center.z, 3, 0, 0, 0, 0);
                l.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, l.center.x, groundY + 1.5D, l.center.z, 60, 2.0D, 1.0D, 2.0D, 0.3D);

                // Thông báo trạng thái xuất lực
                if (l.caster != null) {
                    if (l.powerRoll != null && l.powerRoll.isOverdrive()) {
                        l.caster.displayClientMessage(
                                Component.literal("§6§l[TÀ KHỨ VŨ THÊ TỬ] §4§l⚡ XUẤT LỰC TỐI ĐA 200% (MAXIMUM OUTPUT)!! ⚡💥"),
                                true
                        );
                    } else if (l.powerRoll != null && l.powerRoll.isNormal()) {
                        l.caster.displayClientMessage(
                                Component.literal("§6§l[TÀ KHỨ VŨ THÊ TỬ] §b§lCột Sáng Bộc Phát Uy Lực Đỉnh Điểm! ✨"),
                                true
                        );
                    } else {
                        l.caster.displayClientMessage(
                                Component.literal("§6§l[TÀ KHỨ VŨ THÊ TỬ] §7Cột Sáng Bộc Phát (Xuất Lực Thấp 30%)..."),
                                true
                        );
                    }
                }

                // GÂY SÁT THƯƠNG TỐI ĐA DUY NHẤT 1 LẦN
                if (!l.damageDealt) {
                    l.damageDealt = true;
                    List<LivingEntity> strikeTargets = l.level.getEntitiesOfClass(LivingEntity.class, pillarBox, e -> e != l.caster && e.isAlive());
                    DamageSource dmgSource = (l.caster != null) ? l.level.damageSources().playerAttack(l.caster) : l.level.damageSources().magic();

                    for (LivingEntity victim : strikeTargets) {
                        // Tước bỏ toàn bộ bùa chú
                        victim.removeEffect(MobEffects.MOVEMENT_SPEED);
                        victim.removeEffect(MobEffects.DAMAGE_BOOST);
                        victim.removeEffect(MobEffects.REGENERATION);
                        victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                        victim.removeEffect(MobEffects.FIRE_RESISTANCE);
                        victim.removeEffect(MobEffects.INVISIBILITY);
                        victim.removeEffect(MobEffects.ABSORPTION);
                        victim.removeEffect(MobEffects.HEALTH_BOOST);

                        // Thiêu đốt trong lửa thánh & ghim chặt
                        victim.setRemainingFireTicks(160);
                        victim.setDeltaMovement(0, 0, 0);
                        victim.hasImpulse = true;
                        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255, false, false, true));
                        victim.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, -255, false, false, true));
                        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 255, false, false, true));

                        if (l.caster != null) {
                            com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(l.caster, victim);
                        }

                        victim.addTag("JacobsLadderDamage");
                        // Tính toán lượng sát thương Xuất Lực Tối Đa
                        if (l.powerRoll != null && l.powerRoll.isOverdrive()) {
                            // BẠO KÍCH CỰC ĐẠI: 5000 sát thương + xóa sổ thực thể Undead/Boss lớn
                            victim.hurt(dmgSource, 5000.0F);
                            if (victim.isAlive() && (victim.getMaxHealth() >= 100.0F || victim.isInvertedHealAndHarm())) {
                                victim.discard();
                            }
                        } else if (l.powerRoll != null && l.powerRoll.isNormal()) {
                            // XUẤT LỰC CHUẨN: 800 sát thương * multiplier
                            float damage = 800.0F * l.powerRoll.multiplier;
                            victim.hurt(dmgSource, damage);
                        } else {
                            // XUẤT LỰC THẤP: 300 sát thương * multiplier
                            float damage = 300.0F * (l.powerRoll != null ? l.powerRoll.multiplier : 0.35F);
                            victim.hurt(dmgSource, damage);
                        }
                        victim.removeTag("JacobsLadderDamage");

                        l.level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.05D);
                    }

                    // Phá hủy block địa hình tâm chấn & tạo sóng xung kích chấn động khu vực xung quanh
                    triggerBlockDestructionAndShockwave(l, groundY);
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 3: DUY TRÌ THÁNH QUANG & THANH TẨY (Tick 16..64)
            // (TUYỆT ĐỐI KHÔNG GÂY THÊM SÁT THƯƠNG LẦN 2)
            // =========================================================================
            if (l.beamSpawned && elapsed < 65) {
                // 1. Triệt tiêu toàn bộ đạn đạo bay vào cột sáng (Projectile Erasure)
                List<Projectile> projectiles = l.level.getEntitiesOfClass(Projectile.class, pillarBox);
                for (Projectile p : projectiles) {
                    l.level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
                    p.discard();
                }

                // 2. Viền lửa thánh chạy quanh chu vi hình vuông 4x4
                if (elapsed % 2 == 0) {
                    for (int p = 0; p < 6; p++) {
                        double t = ((elapsed * 0.6D + p * 2.5D) % 16.0D);
                        double px, pz;
                        if (t < 4.0D) {
                            px = minX + t;
                            pz = minZ;
                        } else if (t < 8.0D) {
                            px = maxX;
                            pz = minZ + (t - 4.0D);
                        } else if (t < 12.0D) {
                            px = maxX - (t - 8.0D);
                            pz = maxZ;
                        } else {
                            px = minX;
                            pz = maxZ - (t - 12.0D);
                        }
                        l.level.sendParticles(ParticleTypes.FLAME, px, groundY + 0.06D, pz, 1, 0.02D, 0.03D, 0.02D, 0.01D);
                        l.level.sendParticles(ParticleTypes.WAX_OFF, px, groundY + 0.08D, pz, 1, 0.01D, 0.02D, 0.01D, 0.02D);
                    }
                }

                // 3. Ngôi sao chữ thập 4 cánh thánh quang lơ lửng
                if (elapsed % 3 == 0) {
                    for (int s = 0; s < 3; s++) {
                        double starAngle = Math.toRadians((elapsed * 15.0D + s * 120.0D) % 360.0D);
                        double starDist = 2.3D + (s * 0.5D);
                        double starX = l.center.x + Math.cos(starAngle) * starDist;
                        double starZ = l.center.z + Math.sin(starAngle) * starDist;
                        double starY = groundY + 1.5D + ((s * 4.0D + elapsed * 0.5D) % 18.0D);

                        double d = 0.35D;
                        l.level.sendParticles(ParticleTypes.END_ROD, starX, starY, starZ, 1, 0, 0, 0, 0);
                        l.level.sendParticles(ParticleTypes.END_ROD, starX + d, starY, starZ, 1, 0, 0, 0, 0);
                        l.level.sendParticles(ParticleTypes.END_ROD, starX - d, starY, starZ, 1, 0, 0, 0, 0);
                        l.level.sendParticles(ParticleTypes.END_ROD, starX, starY + d, starZ, 1, 0, 0, 0, 0);
                        l.level.sendParticles(ParticleTypes.END_ROD, starX, starY - d, starZ, 1, 0, 0, 0, 0);
                    }
                }

                // Âm thanh thánh tích duy trì
                if (elapsed % 20 == 0) {
                    l.level.playSound(null, l.center.x, groundY + 2.0D, l.center.z,
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2.0F, 1.2F);
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 4: KẾT THÚC & DỌN DẸP (Tick >= 65)
            // =========================================================================
            if (elapsed >= 65) {
                l.level.sendParticles(ParticleTypes.POOF, l.center.x, groundY + 1.0D, l.center.z, 8, 1.2D, 0.4D, 1.2D, 0.04D);
                l.level.playSound(null, l.center.x, groundY + 1.0D, l.center.z,
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.6F);

                l.cleanupDisplays();
                it.remove();
            }
        }
    }

    /**
     * Tạo ItemDisplay cho Cột Sáng 3D bắt đầu từ mặt đất l.center.y trở lên
     */
    private static Display.ItemDisplay createBeamDisplay(ServerLevel level, Vec3 center,
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
            displayAcc.weapons$setViewRange(10.0F); // Hiển thị xa hơn 160 blocks

            // Tịnh tiến Y = scaleY / 2 để chân cột tiếp đất ngay tại Y=0.0
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

    /**
     * Phá hủy địa hình tại tâm chấn và tạo sóng xung kích chấn động ảnh hưởng khu vực xung quanh
     */
    private static void triggerBlockDestructionAndShockwave(ActiveLadder l, double groundY) {
        ServerLevel level = l.level;
        BlockPos centerBp = BlockPos.containing(l.center.x, groundY, l.center.z);

        // 1. Phá hủy block theo cấp độ xuất lực SkillPowerRoll
        int radiusXZ;
        int depth;
        float explodePower;
        if (l.powerRoll != null && l.powerRoll.isOverdrive()) {
            radiusXZ = 7;     // Bán kính khoét hố 7 blocks (đường kính ~14m)
            depth = 4;        // Sâu 4 blocks
            explodePower = 7.5F;
        } else if (l.powerRoll != null && l.powerRoll.isNormal()) {
            radiusXZ = 5;     // Bán kính khoét hố 5 blocks (đường kính ~10m)
            depth = 3;        // Sâu 3 blocks
            explodePower = 5.5F;
        } else {
            radiusXZ = 4;     // Bán kính 4 blocks
            depth = 2;        // Sâu 2 blocks
            explodePower = 4.0F;
        }

        // Kích hoạt vụ nổ phá block cấp độ thế giới (Văn minh Minecraft vanilla explosion)
        level.explode(l.caster, l.center.x, groundY, l.center.z, explodePower, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);

        // Đào sâu thêm lòng chảo hố thiên thạch thánh quang (đảm bảo hố sâu rõ rệt vượt ra ngoài cột sáng 4x4)
        for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
            for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq <= radiusXZ * radiusXZ) {
                    // Khoét sâu nhất ở tâm, nông dần ra mép
                    int localDepth = Math.max(1, (int) Math.round(depth * (1.0D - Math.sqrt(distSq) / (radiusXZ + 0.5D))));
                    for (int dy = -localDepth; dy <= 2; dy++) {
                        BlockPos bp = centerBp.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(bp);
                        // Tuyệt đối không phá Bedrock, Barrier, End Portal Frame
                        if (!state.isAir() && state.getDestroySpeed(level, bp) >= 0.0F) {
                            level.destroyBlock(bp, false); // false = không làm rơi item để chống lag
                        }
                    }

                    // Tỉ lệ đặt ngọn lửa thánh thiêu rụi xung quanh mép miệng hố
                    if (distSq >= (radiusXZ - 2) * (radiusXZ - 2) && level.random.nextFloat() < 0.4F) {
                        BlockPos firePos = centerBp.offset(dx, 0, dz);
                        if (level.getBlockState(firePos).isAir() && isSolid(level, firePos.below())) {
                            level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
        }

        // 2. Chấn động & Sóng xung kích hất tung khu vực xung quanh (Bán kính 10.0 blocks)
        double shockRadius = 10.0D;
        AABB shockZone = new AABB(l.center.x - shockRadius, groundY - 2.0D, l.center.z - shockRadius,
                l.center.x + shockRadius, groundY + 6.0D, l.center.z + shockRadius);
        List<LivingEntity> outerVictims = level.getEntitiesOfClass(LivingEntity.class, shockZone, e -> e != l.caster && e.isAlive());
        DamageSource shockDmgSource = (l.caster != null) ? level.damageSources().playerAttack(l.caster) : level.damageSources().magic();

        for (LivingEntity victim : outerVictims) {
            Vec3 diff = victim.position().subtract(l.center.x, groundY, l.center.z);
            double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
            // Những kẻ ở ngoài cột 4x4 (dist > 2.0m) nhưng trong bán kính sóng xung kích
            if (dist > 2.0D && dist <= shockRadius) {
                // Hất văng cực mạnh ra xa và tung lên không trung
                Vec3 knockback = new Vec3(diff.x, 0, diff.z).normalize().scale(1.35D).add(0, 0.55D, 0);
                victim.setDeltaMovement(knockback);
                victim.hasImpulse = true;

                // Sát thương chấn động giảm dần theo khoảng cách
                float shockDamage = (float) (250.0F * (l.powerRoll != null ? l.powerRoll.multiplier : 1.0F) * (1.0D - (dist / shockRadius)));
                victim.hurt(shockDmgSource, Math.max(20.0F, shockDamage));
                victim.setRemainingFireTicks(80);
            }
        }

        // 3. Hiệu ứng khói bụi đất đá cuộn tròn chấn động xung quanh
        level.playSound(null, l.center.x, groundY, l.center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F, 0.75F);

        for (int deg = 0; deg < 360; deg += 12) {
            double rad = Math.toRadians(deg);
            double dirX = Math.cos(rad);
            double dirZ = Math.sin(rad);
            double sx = l.center.x + dirX * 4.0D;
            double sz = l.center.z + dirZ * 4.0D;

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, sx, groundY + 0.3D, sz, 1, dirX * 0.25D, 0.1D, dirZ * 0.25D, 0.04D);
            level.sendParticles(ParticleTypes.EXPLOSION, sx, groundY + 0.5D, sz, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LAVA, sx, groundY + 0.2D, sz, 1, dirX * 0.1D, 0.1D, dirZ * 0.1D, 0.02D);
        }
    }
}
