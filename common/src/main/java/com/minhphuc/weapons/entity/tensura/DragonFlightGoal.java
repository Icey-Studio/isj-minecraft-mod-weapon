package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.content.tensura.TensuraSkillHelper;
import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Shared conscious aerial flight goal for True Dragons (Veldora, Velgrynd, Velzard).
 * Ensures dragons actively cruise, bank, escort allies, orbit/swoop targets, and never hover frozen.
 */
public class DragonFlightGoal extends Goal {

    private final Mob mob;
    private final double speed;
    private final BooleanSupplier shouldFly;
    private final Supplier<LivingEntity> ownerSupplier;

    private Vec3 waypoint = null;
    private int retargetTicks = 0;

    public DragonFlightGoal(Mob mob, double speed, BooleanSupplier shouldFly, @Nullable Supplier<LivingEntity> ownerSupplier) {
        this.mob = mob;
        this.speed = speed;
        this.shouldFly = shouldFly;
        this.ownerSupplier = ownerSupplier != null ? ownerSupplier : () -> null;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.isAlive() && shouldFly.getAsBoolean();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isAlive() && shouldFly.getAsBoolean();
    }

    @Override
    public void start() {
        pickNextWaypoint();
    }

    @Override
    public void stop() {
        waypoint = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        retargetTicks--;
        LivingEntity target = mob.getTarget();
        LivingEntity owner = ownerSupplier.get();

        if (target != null && target.isAlive()) {
            double dist = mob.distanceTo(target);
            if (retargetTicks <= 0 || waypoint == null || dist > 26.0D) {
                // Orbit / swoop smoothly at combat altitude above target
                double ang = (mob.tickCount * 0.08D) + (mob.getId() & 7);
                double orbitR = dist > 16.0D ? 6.0D : 11.0D;
                double desiredY = target.getY() + (dist > 10.0D ? 5.5D : 2.5D);
                waypoint = new Vec3(
                        target.getX() + Math.cos(ang) * orbitR,
                        Math.min(mob.level().getMaxBuildHeight() - 8, desiredY),
                        target.getZ() + Math.sin(ang) * orbitR
                );
                retargetTicks = 18;
            }
        } else if (owner != null && owner.isAlive() && mob.distanceToSqr(owner) > 22.0D * 22.0D) {
            // Bay về hộ tống chủ nhân từ trên cao
            double ang = (mob.tickCount * 0.05D);
            waypoint = new Vec3(
                    owner.getX() + Math.cos(ang) * 9.0D,
                    owner.getY() + 6.5D,
                    owner.getZ() + Math.sin(ang) * 9.0D
            );
            retargetTicks = 15;
        } else if (retargetTicks <= 0 || waypoint == null || mob.position().distanceToSqr(waypoint) < 16.0D) {
            pickNextWaypoint();
        }

        if (waypoint != null) {
            mob.setNoGravity(true);
            mob.getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, speed);
            mob.getLookControl().setLookAt(waypoint.x, waypoint.y, waypoint.z, 18.0F, 18.0F);

            // Đảm bảo rồng khổng lồ luôn lướt cánh bay thực sự (không đứng im khi FlyingMoveControl vướng hitbox lớn)
            Vec3 toTarget = waypoint.subtract(mob.position());
            double len = toTarget.length();
            if (len > 1.5D) {
                Vec3 dir = toTarget.scale(1.0D / len);
                double boost = (target != null ? 0.075D : 0.050D) * speed;
                Vec3 nextVel = mob.getDeltaMovement().scale(0.86D).add(dir.scale(boost));
                mob.setDeltaMovement(nextVel);
                mob.hurtMarked = true;
            }
        }
    }

    private void pickNextWaypoint() {
        LivingEntity anchor = ownerSupplier.get();
        double baseX = anchor != null && anchor.isAlive() ? anchor.getX() : mob.getX();
        double baseZ = anchor != null && anchor.isAlive() ? anchor.getZ() : mob.getZ();

        double ang = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double rad = 10.0D + mob.getRandom().nextDouble() * 18.0D;
        int wx = (int) Math.round(baseX + Math.cos(ang) * rad);
        int wz = (int) Math.round(baseZ + Math.sin(ang) * rad);
        int surfaceY = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(wx, mob.getY(), wz).getX(), wz);
        double minHover = Math.max(mob.level().getMinBuildHeight() + 8, surfaceY + 6.0D);
        double wy = minHover + mob.getRandom().nextDouble() * 10.0D;
        if (mob.level().dimension() == net.minecraft.world.level.Level.NETHER) {
            wy = Math.min(110.0D, Math.max(mob.getY() + (mob.getRandom().nextDouble() - 0.35D) * 10.0D, 38.0D));
        }
        waypoint = new Vec3(wx + 0.5D, wy, wz + 0.5D);
        retargetTicks = 65 + mob.getRandom().nextInt(55);
    }

    /**
     * Phá vỡ các khối địa hình cản đường khi Long Chủng ở Dạng Rồng bay/lao tới mục tiêu (tương tự Ender Dragon).
     * Tôn trọng GameRule mobGriefing, chế độ Bảo Vệ Địa Hình (Safe Mode), khối miễn nhiễm rồng và kết giới mod.
     */
    public static void breakBlockingTerrain(ServerLevel sl, Mob dragon) {
        if (sl == null || dragon == null || !dragon.isAlive() || dragon.tickCount % 2 != 0) return;
        if (!sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || !TensuraSkillHelper.canDestroyTerrain(sl, dragon)) {
            return;
        }
        Vec3 vel = dragon.getDeltaMovement();
        if (dragon.onGround() && vel.horizontalDistanceSqr() < 0.0015D && dragon.getTarget() == null) {
            return;
        }

        AABB box = dragon.getBoundingBox().inflate(0.55D, 0.20D, 0.55D);
        if (vel.lengthSqr() > 0.002D) {
            Vec3 ahead = vel.normalize().scale(1.15D);
            box = box.expandTowards(ahead.x, ahead.y, ahead.z);
        }

        int minX = Mth.floor(box.minX);
        int minY = Math.max(sl.getMinBuildHeight() + 1, Mth.floor(box.minY));
        if (dragon.onGround()) {
            minY = Math.max(minY, dragon.getBlockY());
        }
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Math.min(sl.getMaxBuildHeight() - 1, Mth.floor(box.maxY));
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int broken = 0;
        for (int x = minX; x <= maxX && broken < 64; x++) {
            for (int y = minY; y <= maxY && broken < 64; y++) {
                for (int z = minZ; z <= maxZ && broken < 64; z++) {
                    pos.set(x, y, z);
                    BlockState state = sl.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                    if (state.getDestroySpeed(sl, pos) < 0.0F) continue;
                    if (state.is(BlockTags.DRAGON_IMMUNE)) continue;
                    if (state.is(ModBlocks.DRAGON_PRISON_BARRIER.get()) || state.is(ModBlocks.DOMAIN_BARRIER_NOIR.get())) {
                        continue;
                    }
                    if (sl.destroyBlock(pos, false, dragon)) {
                        broken++;
                    }
                }
            }
        }
    }
}
