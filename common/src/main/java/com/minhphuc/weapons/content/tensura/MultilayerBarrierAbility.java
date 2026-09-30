package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kỹ Năng Ma Vương: Đa Trùng Kết Giới (Multilayer Barrier):
 * - Mob vẫn nhận ra người chơi ở bên trong thông qua kết giới (Line of sight xuyên qua kết giới).
 * - Mọi đòn đánh, kỹ năng, đạn ma thuật, tên bắn, Sonic Boom của Warden hoặc chiêu thức đánh trúng tấm kết giới đều bị chặn đứng 100%.
 * - Đứng dưới đất: Dựng tường kết giới trước mặt (ngang 3 block, cao 4 block).
 * - Bay trên trời / Lơ lửng: Dựng khối cầu bao quanh người chơi bán kính 6 block.
 */
public class MultilayerBarrierAbility {

    public static class ActiveMultilayer {
        public final UUID id = UUID.randomUUID();
        public final ServerLevel level;
        public final UUID casterUuid;
        public final boolean isSphere; // true nếu là khối cầu bay, false nếu là tường
        public final Vec3 center;
        public final double radius;
        public int ticksRemaining;
        public final Map<BlockPos, BlockState> replacedBlocks = new ConcurrentHashMap<>();

        public ActiveMultilayer(ServerLevel level, ServerPlayer caster, boolean isSphere, Vec3 center, double radius, int durationTicks) {
            this.level = level;
            this.casterUuid = caster.getUUID();
            this.isSphere = isSphere;
            this.center = center;
            this.radius = radius;
            this.ticksRemaining = durationTicks;
        }
    }

    private static final List<ActiveMultilayer> ACTIVE_BARRIERS = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Long> LAST_DEFLECT_MSG_TIME = new ConcurrentHashMap<>();

    public static boolean hasAnyActiveBarriers(Level level) {
        if (ACTIVE_BARRIERS.isEmpty()) return false;
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                if (b.level == level) return true;
            }
        }
        return false;
    }

    /**
     * Cho phép Mob và người chơi nhìn thấy nhau xuyên qua Đa Trùng Kết Giới.
     * Mọi khối vật lý khác (đá, đất, gỗ,...) vẫn cản tầm nhìn bình thường.
     */
    public static boolean hasLineOfSightThroughBarrier(LivingEntity watcher, Entity target) {
        if (watcher == null || target == null) return false;
        Level level = watcher.level();
        if (level.isClientSide() || level != target.level()) return false;
        if (!hasAnyActiveBarriers(level)) return false;

        Vec3 eyeWatcher = new Vec3(watcher.getX(), watcher.getEyeY(), watcher.getZ());
        Vec3 eyeTarget = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        double distSq = eyeWatcher.distanceToSqr(eyeTarget);
        if (distSq > 128.0 * 128.0) return false;

        // KIỂM TRA PHẠM VI KHÔNG GIAN (Spatial Proximity Check):
        // Chỉ tiến hành raycast tốn kém nếu watcher hoặc target ở gần một kết giới đang hoạt động.
        // Giúp loại bỏ 99.9% raycast thừa cho mob trên toàn thế giới!
        boolean nearAnyBarrier = false;
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                if (b.level == level) {
                    double maxDistSq = (b.radius + 12.0) * (b.radius + 12.0);
                    if (b.center.distanceToSqr(eyeWatcher) <= maxDistSq || b.center.distanceToSqr(eyeTarget) <= maxDistSq) {
                        nearAnyBarrier = true;
                        break;
                    }
                }
            }
        }
        if (!nearAnyBarrier) return false;

        Vec3 rayDir = eyeTarget.subtract(eyeWatcher);
        double totalDist = rayDir.length();
        if (totalDist < 1e-4) return true;
        Vec3 stepDir = rayDir.normalize().scale(0.2);

        Vec3 currentFrom = eyeWatcher;
        for (int step = 0; step < 12; step++) {
            ClipContext ctx = new ClipContext(currentFrom, eyeTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, watcher);
            BlockHitResult hit = level.clip(ctx);
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
            BlockPos hitPos = hit.getBlockPos();
            BlockState state = level.getBlockState(hitPos);
            if (state.is(ModBlocks.MULTILAYER_BARRIER.get())) {
                currentFrom = hit.getLocation().add(stepDir);
                if (eyeWatcher.distanceToSqr(currentFrom) >= distSq) {
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem đòn tấn công / skill / Sonic Boom / đạn bắn có bị tấm kết giới cản lại hay không.
     * Trả về true nếu bị chặn hoàn toàn.
     */
    public static boolean isDamageBlockedByBarrier(LivingEntity victim, DamageSource source) {
        if (victim == null || victim.level().isClientSide()) return false;
        if (ACTIVE_BARRIERS.isEmpty()) return false;

        Vec3 sourcePos = null;
        if (source.getDirectEntity() != null) {
            sourcePos = source.getDirectEntity().position();
        } else if (source.getEntity() != null) {
            sourcePos = source.getEntity().getEyePosition();
        } else if (source.getSourcePosition() != null) {
            sourcePos = source.getSourcePosition();
        }

        if (sourcePos == null) return false;
        Vec3 victimPos = victim.getEyePosition();
        Level level = victim.level();

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                if (b.level != level) continue;

                // Không chặn đòn tấn công của chính người thi triển kết giới bắn ra ngoài
                if (source.getEntity() != null && source.getEntity().getUUID().equals(b.casterUuid)) {
                    continue;
                }

                Vec3 hitPoint = null;

                if (b.isSphere) {
                    double distVictimToCenter = victimPos.distanceTo(b.center);
                    double distSourceToCenter = sourcePos.distanceTo(b.center);

                    // Trường hợp 1: Nạn nhân ở trong khối cầu, đòn đánh xuất phát từ ngoài khối cầu
                    if (distVictimToCenter <= b.radius + 0.6 && distSourceToCenter > b.radius - 0.2) {
                        Vec3 dir = sourcePos.subtract(b.center).normalize();
                        hitPoint = b.center.add(dir.scale(b.radius));
                    } else if (distVictimToCenter > b.radius && distSourceToCenter > b.radius) {
                        // Trường hợp 2: Cả 2 đều ở ngoài, nhưng tia tấn công cắt ngang qua khối cầu
                        Vec3 intersect = getSphereIntersection(sourcePos, victimPos, b.center, b.radius);
                        if (intersect != null) {
                            hitPoint = intersect;
                        }
                    }
                } else {
                    // Trường hợp tường chắn: kiểm tra tia tấn công có cắt trúng khối kết giới tường hay không
                    ClipContext ctx = new ClipContext(sourcePos, victimPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, victim);
                    BlockHitResult hit = level.clip(ctx);
                    if (hit.getType() == HitResult.Type.BLOCK && level.getBlockState(hit.getBlockPos()).is(ModBlocks.MULTILAYER_BARRIER.get())) {
                        hitPoint = hit.getLocation();
                    }
                }

                if (hitPoint != null) {
                    // Hiệu ứng chặn đòn va chạm kết giới
                    if (level instanceof ServerLevel sl) {
                        sl.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.8F, 1.2F);
                        sl.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.5F, 1.6F);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPoint.x, hitPoint.y, hitPoint.z, 15, 0.25, 0.25, 0.25, 0.15);
                        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, hitPoint.x, hitPoint.y, hitPoint.z, 10, 0.2, 0.2, 0.2, 0.1);
                    }

                    if (victim instanceof ServerPlayer sp) {
                        long now = sp.level().getGameTime();
                        Long lastMsg = LAST_DEFLECT_MSG_TIME.get(sp.getUUID());
                        if (lastMsg == null || now - lastMsg > 15) {
                            LAST_DEFLECT_MSG_TIME.put(sp.getUUID(), now);
                            String attackerName = source.getEntity() != null ? source.getEntity().getDisplayName().getString() : "kẻ địch";
                            sp.displayClientMessage(
                                    Component.literal("§b§l[ĐA TRÙNG KẾT GIỚI] §fĐã chặn đứng đòn tấn công từ §e" + attackerName + "§f!"),
                                    true
                            );
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static Vec3 getSphereIntersection(Vec3 p1, Vec3 p2, Vec3 center, double radius) {
        Vec3 d = p2.subtract(p1);
        Vec3 f = p1.subtract(center);
        double a = d.dot(d);
        double b = 2.0 * f.dot(d);
        double c = f.dot(f) - radius * radius;
        double discriminant = b * b - 4 * a * c;
        if (discriminant >= 0) {
            discriminant = Math.sqrt(discriminant);
            double t1 = (-b - discriminant) / (2 * a);
            double t2 = (-b + discriminant) / (2 * a);
            if (t1 >= 0 && t1 <= 1.0) {
                return p1.add(d.scale(t1));
            } else if (t2 >= 0 && t2 <= 1.0) {
                return p1.add(d.scale(t2));
            }
        }
        return null;
    }

    public static void cast(ServerLevel level, ServerPlayer player) {
        if (!PrimordialPlayerDataHelper.isDemonLord(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Bạn phải là Chân Ma Vương để thi triển Đa Trùng Kết Giới!"),
                    true
            );
            return;
        }

        // Cho phép triệu hồi bao nhiêu kết giới cũng được, không xóa kết giới cũ

        boolean isFlyingOrAir = player.isFallFlying() || player.getAbilities().flying || !player.onGround();

        if (isFlyingOrAir) {
            // TRẠNG THÁI 1: KHỐI CẦU ĐA TẦNG TRÊN KHÔNG / DƯỚI NƯỚC / DUNG NHAM (Bán kính 6 block)
            Vec3 center = player.position().add(0, 1.0, 0);
            double radius = 6.0;
            ActiveMultilayer barrier = new ActiveMultilayer(level, player, true, center, radius, -1); // Vĩnh viễn tới khi đập tay giải trừ
            buildSphere(barrier);
            ACTIVE_BARRIERS.add(barrier);

            // Bảo hộ môi trường dung nham và dưới nước tạm thời
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0, false, false, true));

            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.5F, 1.4F);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.0F);

            player.displayClientMessage(
                    Component.literal("§b§l[ĐA TRÙNG KẾT GIỚI] §fĐã kích hoạt Khối Cầu Đa Tầng Bất Khả Xâm Phạm (6m)!"),
                    true
            );
            player.sendSystemMessage(
                    Component.literal("§b✦ Khối Cầu Đa Tầng: Triệu hồi không giới hạn số lượng. Đấm tay không hoặc đánh vào để phá giải!")
            );
        } else {
            // TRẠNG THÁI 2: BỨC TƯỜNG ĐA TRÙNG TRƯỚC MẶT (Ngang 3 block, Cao 4 block)
            Vec3 look = player.getLookAngle();
            Vec3 lookHoriz = new Vec3(look.x, 0, look.z).normalize();
            if (lookHoriz.lengthSqr() < 1e-4) {
                lookHoriz = new Vec3(0, 0, 1);
            }

            // Vector vuông góc phương ngang
            Vec3 right = new Vec3(-lookHoriz.z, 0, lookHoriz.x).normalize();

            // Đặt tường cách người chơi 2.5 block phía trước
            Vec3 wallBase = player.position().add(lookHoriz.scale(2.5));
            BlockPos basePos = BlockPos.containing(wallBase);

            ActiveMultilayer barrier = new ActiveMultilayer(level, player, false, wallBase, 3.0, -1); // Vĩnh viễn tới khi đập tay giải trừ
            buildWall(barrier, basePos, right);
            ACTIVE_BARRIERS.add(barrier);

            level.playSound(null, wallBase.x, wallBase.y, wallBase.z,
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 2.5F, 0.8F);
            level.playSound(null, wallBase.x, wallBase.y, wallBase.z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2.5F, 1.2F);

            player.displayClientMessage(
                    Component.literal("§b§l[ĐA TRÙNG KẾT GIỚI] §fĐã dựng Tường Chắn Đa Tầng (3x4) cản phá mọi đòn tấn công!"),
                    true
            );
            player.sendSystemMessage(
                    Component.literal("§b✦ Bức Tường Đa Trùng: Triệu hồi không giới hạn số lượng. Đấm tay không hoặc đánh vào để phá giải!")
            );
        }
    }

    private static BlockState getOriginalBlockState(ServerLevel level, BlockPos pos) {
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                if (b.level == level && b.replacedBlocks.containsKey(pos)) {
                    BlockState s = b.replacedBlocks.get(pos);
                    if (s != null && !s.is(ModBlocks.MULTILAYER_BARRIER.get())) {
                        return s;
                    }
                }
            }
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    private static void buildSphere(ActiveMultilayer barrier) {
        ServerLevel level = barrier.level;
        Vec3 c = barrier.center;
        BlockPos cPos = BlockPos.containing(c);
        double R = barrier.radius;
        int rInt = (int) Math.ceil(R);
        BlockState barrierState = ModBlocks.MULTILAYER_BARRIER.get().defaultBlockState();

        for (int x = -rInt; x <= rInt; x++) {
            for (int y = -rInt; y <= rInt; y++) {
                for (int z = -rInt; z <= rInt; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq >= (R - 0.9) * (R - 0.9) && distSq <= (R + 0.9) * (R + 0.9)) {
                        BlockPos pos = cPos.offset(x, y, z);
                        BlockState orig = level.getBlockState(pos);
                        if (orig.getDestroySpeed(level, pos) >= 0 && !orig.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                            BlockState realOrig = orig;
                            if (orig.is(ModBlocks.MULTILAYER_BARRIER.get())) {
                                realOrig = getOriginalBlockState(level, pos);
                            }
                            barrier.replacedBlocks.put(pos, realOrig);
                            if (!orig.is(ModBlocks.MULTILAYER_BARRIER.get())) {
                                level.setBlock(pos, barrierState, 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void buildWall(ActiveMultilayer barrier, BlockPos basePos, Vec3 right) {
        ServerLevel level = barrier.level;
        BlockState barrierState = ModBlocks.MULTILAYER_BARRIER.get().defaultBlockState();

        // Ngang 3 block (offset -1, 0, +1 dọc vector right), Cao 4 block (dy 0..3)
        for (int w = -1; w <= 1; w++) {
            Vec3 offsetHoriz = right.scale(w);
            int ox = (int) Math.round(offsetHoriz.x);
            int oz = (int) Math.round(offsetHoriz.z);

            for (int dy = 0; dy < 4; dy++) {
                BlockPos pos = basePos.offset(ox, dy, oz);
                BlockState orig = level.getBlockState(pos);
                if (orig.getDestroySpeed(level, pos) >= 0 && !orig.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                    BlockState realOrig = orig;
                    if (orig.is(ModBlocks.MULTILAYER_BARRIER.get())) {
                        realOrig = getOriginalBlockState(level, pos);
                    }
                    barrier.replacedBlocks.put(pos, realOrig);
                    if (!orig.is(ModBlocks.MULTILAYER_BARRIER.get())) {
                        level.setBlock(pos, barrierState, 2);
                    }
                }
            }
        }
    }

    public static void cleanBarrier(ActiveMultilayer barrier) {
        if (barrier == null) return;
        ServerLevel level = barrier.level;
        for (Map.Entry<BlockPos, BlockState> entry : barrier.replacedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState orig = entry.getValue();

            // Kiểm tra xem vị trí này có còn nằm trong bất kỳ kết giới nào KHÁC đang hoạt động không
            boolean stillUsed = false;
            synchronized (ACTIVE_BARRIERS) {
                for (ActiveMultilayer other : ACTIVE_BARRIERS) {
                    if (other != barrier && other.level == level && other.replacedBlocks.containsKey(pos)) {
                        stillUsed = true;
                        break;
                    }
                }
            }

            // Chỉ hoàn nguyên về khối gốc nếu không còn kết giới nào khác sử dụng khối này
            if (!stillUsed) {
                if (level.getBlockState(pos).is(ModBlocks.MULTILAYER_BARRIER.get())) {
                    level.setBlock(pos, orig, 2);
                }
            }
        }
        barrier.replacedBlocks.clear();
    }

    public static void dismissPlayerBarriers(UUID playerUuid) {
        synchronized (ACTIVE_BARRIERS) {
            Iterator<ActiveMultilayer> it = ACTIVE_BARRIERS.iterator();
            while (it.hasNext()) {
                ActiveMultilayer b = it.next();
                if (b.casterUuid.equals(playerUuid)) {
                    cleanBarrier(b);
                    it.remove();
                }
            }
        }
    }

    /**
     * Người chơi đấm tay không hoặc đánh vào khối kết giới để phá giải.
     */
    public static boolean dispelByPunch(ServerPlayer player, BlockPos pos) {
        ActiveMultilayer found = null;
        boolean isBarrierBlock = player.level().getBlockState(pos).is(ModBlocks.MULTILAYER_BARRIER.get());

        synchronized (ACTIVE_BARRIERS) {
            Vec3 hitPos = Vec3.atCenterOf(pos);
            double minDistance = Double.MAX_VALUE;

            // 1. Tìm kết giới mà vị trí pos thuộc replacedBlocks
            for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                if (b.level == player.level() && b.replacedBlocks.containsKey(pos)) {
                    double dist = b.center.distanceTo(player.position());
                    if (dist < minDistance) {
                        minDistance = dist;
                        found = b;
                    }
                }
            }

            // 2. Nếu không tìm thấy bằng replacedBlocks nhưng khối đập vào ĐÚNG là khối kết giới
            // hoặc người chơi đập tay không gần kết giới
            if (found == null && (isBarrierBlock || player.getMainHandItem().isEmpty())) {
                for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                    if (b.level == player.level()) {
                        double dist = b.center.distanceTo(hitPos);
                        double threshold = b.isSphere ? (b.radius + 3.0) : 5.0;
                        if (dist <= threshold && dist < minDistance) {
                            minDistance = dist;
                            found = b;
                        }
                    }
                }
            }

            if (found != null) {
                ACTIVE_BARRIERS.remove(found);
                cleanBarrier(found);
            }
        }

        if (found != null) {
            ServerLevel sl = (ServerLevel) player.level();
            Vec3 center = found.center;
            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.3F);
            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 2.5F, 1.0F);
            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.5F);

            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 25, 0.5, 0.5, 0.5, 0.15);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, 20, 0.4, 0.4, 0.4, 0.1);

            player.displayClientMessage(
                    Component.literal("§b§l[ĐA TRÙNG KẾT GIỚI] §fĐã dùng tay phá giải thành công kết giới!"),
                    true
            );
            return true;
        }

        return false;
    }

    /**
     * Đấm tay không vào không khí gần biên giới kết giới để phá giải.
     */
    public static boolean checkEmptyHandPunch(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) return false;

        Vec3 eyePos = player.getEyePosition();
        ActiveMultilayer found = null;
        double minDistance = Double.MAX_VALUE;

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveMultilayer b : ACTIVE_BARRIERS) {
                if (b.level == player.level()) {
                    double dist = eyePos.distanceTo(b.center);
                    double checkRange = b.isSphere ? (b.radius + 2.5) : 4.0;
                    if (dist <= checkRange && dist < minDistance) {
                        minDistance = dist;
                        found = b;
                    }
                }
            }
            if (found != null) {
                ACTIVE_BARRIERS.remove(found);
                cleanBarrier(found);
            }
        }

        if (found != null) {
            ServerLevel sl = (ServerLevel) player.level();
            Vec3 center = found.center;
            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.3F);
            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 2.5F, 1.0F);
            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.5F);

            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 25, 0.5, 0.5, 0.5, 0.15);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, 20, 0.4, 0.4, 0.4, 0.1);

            player.displayClientMessage(
                    Component.literal("§b§l[ĐA TRÙNG KẾT GIỚI] §fĐã dùng tay phá giải thành công kết giới!"),
                    true
            );
            return true;
        }

        return false;
    }

    /**
     * Phá hủy toàn bộ Đa Trùng Kết Giới trong phạm vi bị công kích bởi đòn tối thượng.
     * Áp dụng khi Milim (Dragon Nova), Velgrynd (Cardinal Accel) hoặc Velzard tung đòn tối thượng.
     */
    public static boolean shatterBarrierNear(ServerLevel level, Vec3 pos, double range, String cause) {
        if (ACTIVE_BARRIERS.isEmpty()) return false;

        List<ActiveMultilayer> toShatter = new ArrayList<>();
        double rangeSq = (range + 6.0) * (range + 6.0);

        synchronized (ACTIVE_BARRIERS) {
            Iterator<ActiveMultilayer> it = ACTIVE_BARRIERS.iterator();
            while (it.hasNext()) {
                ActiveMultilayer b = it.next();
                if (b.level == level) {
                    double distSq = b.center.distanceToSqr(pos);
                    double checkDist = b.isSphere ? (b.radius + range) : (range + 4.0);
                    if (distSq <= checkDist * checkDist) {
                        toShatter.add(b);
                        it.remove();
                    }
                }
            }
        }

        if (toShatter.isEmpty()) return false;

        for (ActiveMultilayer b : toShatter) {
            cleanBarrier(b);
            Vec3 center = b.center;
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0F, 0.8F);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 3.0F, 0.9F);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.2F);

            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.5, 0.5, 0.5, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 40, 1.5, 1.5, 1.5, 0.25);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, 30, 1.2, 1.2, 1.2, 0.2);

            Player caster = level.getPlayerByUUID(b.casterUuid);
            if (caster != null) {
                caster.displayClientMessage(
                        Component.literal("§c§l[ĐA TRÙNG KẾT GIỚI] §4Đã bị công phá vỡ vụn bởi §e" + cause + "§4!"),
                        false
                );
            }
        }

        return true;
    }

    /**
     * Cập nhật đếm ngược thời gian, cảnh giới quái vật xung quanh và phản hồi/chặn đạn tên trong mỗi tick.
     */
    public static void tick(ServerLevel level) {
        if (ACTIVE_BARRIERS.isEmpty()) return;

        synchronized (ACTIVE_BARRIERS) {
            Iterator<ActiveMultilayer> it = ACTIVE_BARRIERS.iterator();
            while (it.hasNext()) {
                ActiveMultilayer b = it.next();
                if (b.level != level) continue;

                if (b.ticksRemaining > 0) {
                    b.ticksRemaining--;
                    if (b.ticksRemaining <= 0) {
                        cleanBarrier(b);
                        level.playSound(null, b.center.x, b.center.y, b.center.z,
                                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 1.4F);
                        it.remove();
                        continue;
                    }
                }

                Player caster = level.getPlayerByUUID(b.casterUuid);

                // 1. Quái vật xung quanh nhận ra người chơi ở trong và nhắm đánh người chơi
                if (level.getGameTime() % 10 == 0 && caster != null && caster.isAlive()) {
                    double aggroR = b.isSphere ? b.radius + 24.0 : 24.0;
                    AABB aggroBox = new AABB(b.center.x - aggroR, b.center.y - aggroR, b.center.z - aggroR,
                            b.center.x + aggroR, b.center.y + aggroR, b.center.z + aggroR);
                    List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, aggroBox, m -> m.isAlive() && (m instanceof Monster || m instanceof Enemy));
                    for (Mob mob : nearbyMobs) {
                        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                            if (hasLineOfSightThroughBarrier(mob, caster)) {
                                mob.setTarget(caster);
                            }
                        }
                    }
                }

                // 2. Triệt tiêu & phản hồi đạn bay trúng kết giới
                double scanR = b.isSphere ? b.radius + 2.5 : 4.0;
                AABB scanBox = new AABB(b.center.x - scanR, b.center.y - scanR, b.center.z - scanR,
                        b.center.x + scanR, b.center.y + scanR, b.center.z + scanR);
                List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, scanBox);
                for (Projectile p : projectiles) {
                    if (p.getOwner() == null || !p.getOwner().getUUID().equals(b.casterUuid)) {
                        boolean hitBarrier = false;
                        if (b.isSphere) {
                            double dist = p.position().distanceTo(b.center);
                            if (Math.abs(dist - b.radius) <= 1.2 || dist < b.radius) {
                                hitBarrier = true;
                            }
                        } else {
                            BlockPos pPos = BlockPos.containing(p.position());
                            if (b.replacedBlocks.containsKey(pPos) || p.position().distanceTo(b.center) <= 2.2) {
                                hitBarrier = true;
                            }
                        }

                        if (hitBarrier) {
                            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5F, 1.6F);
                            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.2F, 1.5F);
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(), p.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                            level.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY(), p.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                            p.setDeltaMovement(p.getDeltaMovement().scale(-1.2));
                        }
                    }
                }
            }
        }
    }
}
