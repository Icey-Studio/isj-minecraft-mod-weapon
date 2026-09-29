package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Kháng Ma Kết Giới (Anti-Magic Barrier):
 * - Người chơi (Ma Vương): Bán kính 60m (+15m), Cao 40m. Tồn tại vĩnh viễn tới khi đấm giải trừ.
 *   Kéo dài chân kết giới cắm sâu xuống mặt đất/nước, bao phủ 100% không gian.
 *   Hỗ trợ tạo nhiều kết giới chồng đè không bị thủng lỗ.
 *   Kháng 100% toàn bộ mob gây hại, Boss, Ác ma, Không vong, Long chủng, Milim.
 * - Dân làng (Golem hy sinh): Bán kính 35m, Cao 28m.
 *   Cấm mob cơ bản, kháng hoàn toàn Không Vong. Không cấm được Boss/Long chủng/Milim.
 */
public class AntiMagicBarrierManager {

    private static final DustParticleOptions EMERALD_BARRIER =
            new DustParticleOptions(new Vector3f(0.1F, 1.0F, 0.4F), 1.6F);
    private static final DustParticleOptions LIME_BARRIER =
            new DustParticleOptions(new Vector3f(0.4F, 1.0F, 0.2F), 1.2F);

    public static class ActiveBarrier {
        public final UUID id = UUID.randomUUID();
        public final ServerLevel level;
        public final UUID casterUuid; // null nếu là của dân làng
        public final boolean isVillager;
        public final Vec3 center;
        public final double radius;
        public final double height;
        public final long createTick;
        public int ticksAlive = 0;
        public int durability = 100; // Độ bền cho kết giới dân làng
        public double lowestYOffset = 0; // Độ sâu tường kết giới cắm xuống lòng đất
        public final Map<BlockPos, BlockState> replacedBlocks = new ConcurrentHashMap<>();

        public ActiveBarrier(ServerLevel level, ServerPlayer caster, Vec3 center, double radius, double height, boolean isVillager) {
            this.level = level;
            this.casterUuid = caster != null ? caster.getUUID() : null;
            this.center = center;
            this.radius = radius;
            this.height = height;
            this.isVillager = isVillager;
            this.createTick = level.getGameTime();
        }
    }

    private static final List<ActiveBarrier> ACTIVE_BARRIERS = Collections.synchronizedList(new ArrayList<>());

    /**
     * Người chơi Chân Ma Vương thi triển Kháng Ma Kết Giới (Bán kính 60, Cao 40).
     */
    public static void castPlayerBarrier(ServerLevel level, ServerPlayer player) {
        if (!PrimordialPlayerDataHelper.isDemonLord(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Bạn phải là Chân Ma Vương để thi triển Kháng Ma Kết Giới!"),
                    true
            );
            return;
        }

        // Cho phép người chơi tạo không giới hạn số lượng Kháng Ma Kết Giới (kể cả chồng đè lên nhau)
        // Kết giới tồn tại vĩnh viễn và chỉ biến mất khi người chơi đấm vào nó

        Vec3 center = player.position();
        double radius = 60.0; // Mở rộng thêm 15 block (từ 45 lên 60)
        double height = 40.0; // Tăng chiều cao tương xứng giữ tỉ lệ vòm chuẩn

        ActiveBarrier barrier = new ActiveBarrier(level, player, center, radius, height, false);
        buildDome(barrier);
        ACTIVE_BARRIERS.add(barrier);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.0F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5F, 1.4F);

        player.displayClientMessage(
                Component.literal("§a§l[KHÁNG MA KẾT GIỚI] §eĐã triển khai mái vòm bảo hộ vĩnh viễn (R:60m, H:40m)!"),
                true
        );
        player.sendSystemMessage(
                Component.literal("§a✦ Kháng Ma Kết Giới bao bọc toàn diện chạm mặt đất, ngăn chặn & thanh tẩy 100% mọi mối đe dọa, boss, ác ma, không vong và long chủng. Đấm tay không hoặc đánh vào kết giới để hóa giải.")
        );
    }

    /**
     * Dân làng dựng kết giới khi Iron Golem ngã xuống (Bán kính 35, Cao 28).
     */
    public static void onIronGolemDeath(ServerLevel level, IronGolem golem) {
        Vec3 deathPos = golem.position();

        // 1. Kiểm tra chống spam & lag: trong phạm vi 80 block đã có kết giới nào chưa
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier b : ACTIVE_BARRIERS) {
                if (b.level == level && b.center.distanceTo(deathPos) <= 80.0) {
                    return; // Đã có kết giới trong khu vực, không tạo thêm
                }
            }
        }

        // 2. Kiểm tra xem có dân làng xung quanh (trong vòng 48 block) hay không
        AABB scan = new AABB(deathPos.x - 48, deathPos.y - 20, deathPos.z - 48,
                             deathPos.x + 48, deathPos.y + 20, deathPos.z + 48);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, scan, LivingEntity::isAlive);
        if (villagers.isEmpty()) {
            return;
        }

        // Dân làng hợp lực cầu nguyện dựng kết giới
        double radius = 35.0;
        double height = 28.0;

        ActiveBarrier barrier = new ActiveBarrier(level, null, deathPos, radius, height, true);
        buildDome(barrier);
        ACTIVE_BARRIERS.add(barrier);

        level.playSound(null, deathPos.x, deathPos.y, deathPos.z,
                SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 0.8F);
        level.playSound(null, deathPos.x, deathPos.y, deathPos.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.5F, 1.2F);

        // Báo tin cho người chơi xung quanh làng
        for (ServerPlayer sp : level.players()) {
            if (sp.position().distanceTo(deathPos) <= 96.0) {
                sp.displayClientMessage(
                        Component.literal("§a§l[LÀNG BẢO HỘ] §eDân làng đã dựng Kháng Ma Kết Giới sau khi Iron Golem ngã xuống!"),
                        true
                );
            }
        }
    }

    /**
     * Lấy khối gốc ban đầu trước khi có bất kỳ kết giới nào thay thế vị trí này.
     */
    private static BlockState getOriginalBlockState(ServerLevel level, BlockPos pos) {
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier b : ACTIVE_BARRIERS) {
                if (b.level == level && b.replacedBlocks.containsKey(pos)) {
                    BlockState s = b.replacedBlocks.get(pos);
                    if (!s.is(ModBlocks.ANTI_MAGIC_BARRIER.get())) {
                        return s;
                    }
                }
            }
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    /**
     * Xây dựng mái vòm bán cầu (Hemisphere Dome) và kéo dài chân tường kết giới cắm chạm mặt đất.
     */
    private static void buildDome(ActiveBarrier barrier) {
        ServerLevel level = barrier.level;
        Vec3 c = barrier.center;
        BlockPos cPos = BlockPos.containing(c);
        double R = barrier.radius;
        double H = barrier.height;
        int rInt = (int) Math.ceil(R);
        int hInt = (int) Math.ceil(H);

        BlockState barrierState = ModBlocks.ANTI_MAGIC_BARRIER.get().defaultBlockState();
        int maxDropDepth = 120; // Giới hạn kéo dài xuống lòng đất tối đa 120 block chống lag

        for (int x = -rInt; x <= rInt; x++) {
            for (int z = -rInt; z <= rInt; z++) {
                double horizSq = (x * x + z * z) / (R * R);
                if (horizSq > 1.08) continue;

                double minVertSq = Math.max(0.0, 0.85 - horizSq);
                double maxVertSq = 1.08 - horizSq;
                if (maxVertSq < 0) continue;

                int minY = (int) Math.floor(H * Math.sqrt(minVertSq));
                int maxY = (int) Math.ceil(H * Math.sqrt(maxVertSq));
                maxY = Math.min(hInt, maxY);

                // Nếu là viền ngoài của kết giới (Perimeter wall): kéo chân tường đâm thẳng xuống mặt đất
                if (horizSq >= 0.90 && horizSq <= 1.08) {
                    int worldX = cPos.getX() + x;
                    int worldZ = cPos.getZ() + z;

                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);

                    // Nếu gặp nước hoặc chất lỏng, dò tiếp xuống chạm đáy bùn/đá rắn
                    BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(worldX, surfaceY, worldZ);
                    while (checkPos.getY() > level.getMinBuildHeight() && !level.getFluidState(checkPos).isEmpty()) {
                        checkPos.move(Direction.DOWN);
                    }
                    int solidGroundY = checkPos.getY();
                    int targetGroundY = solidGroundY - cPos.getY();

                    // Kéo chân tường kết giới cắm sâu xuống mặt đất 1 block để không hở đáy
                    minY = Math.min(-1, targetGroundY - 1);
                    minY = Math.max(-maxDropDepth, minY);

                    int dropDistance = cPos.getY() - (cPos.getY() + minY);
                    if (dropDistance > barrier.lowestYOffset) {
                        barrier.lowestYOffset = dropDistance;
                    }
                } else if (horizSq >= 0.85) {
                    minY = -1;
                }

                for (int y = minY; y <= maxY; y++) {
                    double vertSq = (y >= 0) ? (y * y) / (H * H) : 0;
                    double totalSq = horizSq + vertSq;

                    // Khối thuộc vòm cung trên trời hoặc thuộc tường kéo dài chạm đất
                    boolean isDomeShell = (y >= 0 && totalSq >= 0.85 && totalSq <= 1.08);
                    boolean isGroundWall = (y < 0 && horizSq >= 0.90 && horizSq <= 1.08);

                    if (isDomeShell || isGroundWall) {
                        BlockPos bPos = cPos.offset(x, y, z);
                        BlockState orig = level.getBlockState(bPos);
                        if (orig.getDestroySpeed(level, bPos) >= 0 &&
                            !orig.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {

                            // Hỗ trợ tạo trùng kết giới: nếu khối đã là kết giới cũ, lấy block gốc ban đầu
                            BlockState realOrig = orig;
                            if (orig.is(ModBlocks.ANTI_MAGIC_BARRIER.get())) {
                                realOrig = getOriginalBlockState(level, bPos);
                            }

                            barrier.replacedBlocks.put(bPos, realOrig);
                            if (!orig.is(ModBlocks.ANTI_MAGIC_BARRIER.get())) {
                                level.setBlock(bPos, barrierState, 2);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra xem thực thể có được phép tự do đi xuyên qua kết giới hay không.
     */
    public static boolean canPass(Entity entity, BlockPos pos) {
        // Người chơi, dân làng, Iron Golem và động vật vô hại luôn được qua lại tự do
        if (entity instanceof Player ||
            entity instanceof Villager ||
            entity instanceof IronGolem ||
            (entity instanceof Animal && !(entity instanceof Enemy))) {
            return true;
        }

        // Kiểm tra xem vị trí này thuộc kết giới nào
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
                if (barrier.level == entity.level()) {
                    double dx = entity.getX() - barrier.center.x;
                    double dz = entity.getZ() - barrier.center.z;
                    double dist2D = Math.sqrt(dx * dx + dz * dz);
                    double minY = barrier.center.y - barrier.lowestYOffset - 5.0;
                    double maxY = barrier.center.y + barrier.height + 3.0;

                    if (dist2D <= barrier.radius + 3.0 && entity.getY() >= minY && entity.getY() <= maxY) {
                        if (barrier.isVillager) {
                            // Kết giới Dân Làng:
                            if (entity instanceof EnderDragon ||
                                entity instanceof WitherBoss ||
                                entity instanceof Warden ||
                                entity instanceof VelgryndEntity ||
                                entity instanceof MilimEntity ||
                                entity instanceof Player) {
                                return true;
                            }
                            if (entity instanceof PrimordialDemonEntity) {
                                return true;
                            }
                            if (entity instanceof KuboEntity || isThreateningEntity(entity)) {
                                return false;
                            }
                        } else {
                            // Kết giới Người Chơi (Ma Vương): Cấm 100% mọi sinh vật đe dọa!
                            if (isThreateningEntity(entity) ||
                                entity instanceof PrimordialDemonEntity ||
                                entity instanceof VelgryndEntity ||
                                entity instanceof MilimEntity ||
                                entity instanceof KuboEntity ||
                                entity instanceof Warden ||
                                entity instanceof WitherBoss ||
                                entity instanceof EnderDragon) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean isThreateningEntity(Entity entity) {
        return entity instanceof Enemy ||
               entity instanceof Monster ||
               entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    /**
     * Hoàn nguyên toàn bộ khối khi kết giới giải trừ hoặc vỡ vụn.
     * Khối chồng lấn với kết giới khác sẽ không bị xóa, tránh làm thủng kết giới còn lại.
     */
    public static void cleanBarrier(ActiveBarrier barrier) {
        if (barrier == null) return;
        ServerLevel level = barrier.level;

        for (Map.Entry<BlockPos, BlockState> entry : barrier.replacedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState orig = entry.getValue();

            // Kiểm tra xem vị trí này có còn nằm trong bất kỳ kết giới nào KHÁC đang hoạt động không
            boolean stillUsed = false;
            synchronized (ACTIVE_BARRIERS) {
                for (ActiveBarrier other : ACTIVE_BARRIERS) {
                    if (other != barrier && other.level == level && other.replacedBlocks.containsKey(pos)) {
                        stillUsed = true;
                        break;
                    }
                }
            }

            // Chỉ hoàn nguyên về khối gốc nếu không còn kết giới nào khác sử dụng khối này
            if (!stillUsed) {
                if (level.getBlockState(pos).is(ModBlocks.ANTI_MAGIC_BARRIER.get())) {
                    level.setBlock(pos, orig, 2);
                }
            }
        }
        barrier.replacedBlocks.clear();
    }

    public static void cleanPlayerBarriers(UUID playerUuid) {
        synchronized (ACTIVE_BARRIERS) {
            Iterator<ActiveBarrier> it = ACTIVE_BARRIERS.iterator();
            while (it.hasNext()) {
                ActiveBarrier b = it.next();
                if (playerUuid.equals(b.casterUuid)) {
                    it.remove();
                    cleanBarrier(b);
                }
            }
        }
    }

    /**
     * Cập nhật logic quét và bảo vệ mỗi tick.
     */
    public static void tickBarriers(ServerLevel level) {
        if (ACTIVE_BARRIERS.isEmpty()) return;

        List<ActiveBarrier> barriersToBreak = new ArrayList<>();

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
                if (barrier.level != level) continue;

                barrier.ticksAlive++;
                Vec3 center = barrier.center;
                double R = barrier.radius;
                double H = barrier.height;

                // 1. Hiệu ứng hạt ánh sáng xanh lục bảo dọc theo mái vòm (mỗi 8 ticks)
                if (barrier.ticksAlive % 8 == 0) {
                    for (int ang = 0; ang < 360; ang += 30) {
                        double rad = Math.toRadians(ang);
                        double x = center.x + Math.cos(rad) * R;
                        double z = center.z + Math.sin(rad) * R;

                        level.sendParticles(EMERALD_BARRIER, x, center.y + 0.2, z, 1, 0, 0, 0, 0);

                        double midR = R * 0.7;
                        double midX = center.x + Math.cos(rad) * midR;
                        double midZ = center.z + Math.sin(rad) * midR;
                        level.sendParticles(LIME_BARRIER, midX, center.y + (H * 0.5), midZ, 1, 0, 0, 0, 0);
                    }
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + H, center.z, 2, 0.4, 0.1, 0.4, 0);
                }

                // 2. Quét thực thể trong phạm vi kết giới từ mặt đất tới nóc vòm (Throttling: mỗi 6 ticks)
                if (barrier.ticksAlive % 6 == 0) {
                    double minY = Math.max(level.getMinBuildHeight(), center.y - barrier.lowestYOffset - 5.0);
                    double maxY = Math.min(level.getMaxBuildHeight(), center.y + H + 3.0);
                    AABB box = new AABB(center.x - R - 3, minY, center.z - R - 3,
                                        center.x + R + 3, maxY, center.z + R + 3);
                    List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);

                    boolean broken = false;

                    for (LivingEntity e : entities) {
                        if (e instanceof Player || e instanceof Villager || e instanceof IronGolem) continue;
                        if (e instanceof Animal && !(e instanceof Enemy)) continue;

                        double dx = (e.getX() - center.x) / R;
                        double dz = (e.getZ() - center.z) / R;
                        double horizDistSq = dx * dx + dz * dz;

                        // Nếu thực thể nằm trong bán kính trụ R và nằm trong phạm vi chiều cao kết giới
                        if (horizDistSq <= 1.08 && e.getY() >= minY && e.getY() <= maxY) {
                            if (!barrier.isVillager) {
                                // KẾT GIỚI NGƯỜI CHƠI (MA VƯƠNG):
                                boolean isThreat = isThreateningEntity(e) ||
                                                   e instanceof PrimordialDemonEntity ||
                                                   e instanceof VelgryndEntity ||
                                                   e instanceof MilimEntity ||
                                                   e instanceof KuboEntity ||
                                                   e instanceof Warden ||
                                                   e instanceof WitherBoss ||
                                                   e instanceof EnderDragon;

                                if (isThreat) {
                                    level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + 1.0, e.getZ(), 1, 0, 0, 0, 0);
                                    level.sendParticles(EMERALD_BARRIER, e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
                                    level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                            SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 2.0F, 1.2F);
                                    e.discard();
                                }
                            } else {
                                // KẾT GIỚI DÂN LÀNG
                                if (e instanceof KuboEntity || isThreateningEntity(e)) {
                                    if (!(e instanceof Warden) && !(e instanceof WitherBoss) && !(e instanceof EnderDragon)) {
                                        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.8, e.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
                                        e.discard();
                                    }
                                } else if (e instanceof PrimordialDemonEntity demon) {
                                    if (barrier.ticksAlive % 20 == 0) {
                                        barrier.durability -= 10;
                                        level.playSound(null, demon.getX(), demon.getY(), demon.getZ(),
                                                SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 2.0F, 0.8F);
                                        level.sendParticles(ParticleTypes.CRIT, demon.getX(), demon.getY() + 1.2, demon.getZ(), 15, 0.3, 0.3, 0.3, 0.1);

                                        if (barrier.durability <= 0) {
                                            broken = true;
                                            level.playSound(null, center.x, center.y, center.z,
                                                    SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 3.5F, 0.6F);
                                            level.playSound(null, center.x, center.y, center.z,
                                                    SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 2.5F, 0.8F);
                                            break;
                                        }
                                    }
                                } else if (e instanceof VelgryndEntity || e instanceof MilimEntity) {
                                    broken = true;
                                    level.playSound(null, center.x, center.y, center.z,
                                            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.8F);
                                    break;
                                }
                            }
                        }
                    }

                    if (broken) {
                        barriersToBreak.add(barrier);
                    }
                }
            }

            for (ActiveBarrier b : barriersToBreak) {
                ACTIVE_BARRIERS.remove(b);
                cleanBarrier(b);
            }
        }
    }

    /**
     * Người chơi đập tay không hoặc đánh vào khối kết giới để hóa giải.
     * Nhận diện chuẩn xác kết giới gần nhất khi có nhiều kết giới chồng đè.
     */
    public static boolean dispelByPunch(ServerPlayer player, BlockPos pos) {
        ActiveBarrier found = null;
        boolean isBarrierBlock = player.level().getBlockState(pos).is(ModBlocks.ANTI_MAGIC_BARRIER.get());

        synchronized (ACTIVE_BARRIERS) {
            Vec3 hitPos = Vec3.atCenterOf(pos);
            double minDistance = Double.MAX_VALUE;

            // 1. Tìm kết giới mà vị trí pos thuộc replacedBlocks
            for (ActiveBarrier b : ACTIVE_BARRIERS) {
                if (b.level == player.level() && b.replacedBlocks.containsKey(pos)) {
                    double dist = b.center.distanceTo(player.position());
                    if (dist < minDistance) {
                        minDistance = dist;
                        found = b;
                    }
                }
            }

            // 2. Nếu không tìm thấy bằng replacedBlocks nhưng khối đập vào ĐÚNG là khối kết giới
            // hoặc người chơi đập tay không gần rìa kết giới
            if (found == null && (isBarrierBlock || player.getMainHandItem().isEmpty())) {
                for (ActiveBarrier b : ACTIVE_BARRIERS) {
                    if (b.level == player.level()) {
                        double dx = hitPos.x - b.center.x;
                        double dz = hitPos.z - b.center.z;
                        double dist2D = Math.sqrt(dx * dx + dz * dz);
                        double minY = b.center.y - b.lowestYOffset - 5.0;
                        double maxY = b.center.y + b.height + 3.0;

                        if (dist2D <= b.radius + 4.0 && hitPos.y >= minY && hitPos.y <= maxY) {
                            double dist = b.center.distanceTo(player.position());
                            if (dist < minDistance) {
                                minDistance = dist;
                                found = b;
                            }
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
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.5F);

            player.displayClientMessage(
                    Component.literal("§a§l[KHÁNG MA KẾT GIỚI] §eĐã hóa giải thành công kết giới bảo hộ!"),
                    true
            );
            return true;
        }

        return false;
    }

    /**
     * Đấm tay không vào không khí gần biên giới kết giới để hóa giải.
     */
    public static boolean checkEmptyHandPunch(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) return false;

        Vec3 eyePos = player.getEyePosition();
        ActiveBarrier found = null;
        double minDistance = Double.MAX_VALUE;

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier b : ACTIVE_BARRIERS) {
                if (b.level == player.level()) {
                    double dx = eyePos.x - b.center.x;
                    double dz = eyePos.z - b.center.z;
                    double dist2D = Math.sqrt(dx * dx + dz * dz);
                    double minY = b.center.y - b.lowestYOffset - 5.0;
                    double maxY = b.center.y + b.height + 3.0;

                    if (dist2D <= b.radius + 3.0 && eyePos.y >= minY && eyePos.y <= maxY) {
                        double dist = eyePos.distanceTo(b.center);
                        if (dist < minDistance) {
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
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.5F);

            player.displayClientMessage(
                    Component.literal("§a§l[KHÁNG MA KẾT GIỚI] §eĐã hóa giải thành công kết giới bảo hộ!"),
                    true
            );
            return true;
        }

        return false;
    }
}

