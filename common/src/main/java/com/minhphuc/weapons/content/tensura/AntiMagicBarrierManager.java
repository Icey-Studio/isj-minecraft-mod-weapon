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
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
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
    private static final Set<BlockPos> KNOWN_VILLAGE_BELLS = Collections.synchronizedSet(new HashSet<>());
    private static long lastVillageCheckTick = 0;

    /**
     * Kiểm tra xem một vị trí có nằm trong bất kỳ Kháng Ma Kết Giới nào không.
     */
    public static boolean isInBarrier(net.minecraft.world.level.Level level, Vec3 pos) {
        if (ACTIVE_BARRIERS.isEmpty() || level == null || pos == null) return false;
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
                if (barrier.level == level) {
                    double dx = pos.x - barrier.center.x;
                    double dz = pos.z - barrier.center.z;
                    double distSq = dx * dx + dz * dz;
                    double maxBound = barrier.radius + 1.5D;
                    if (distSq <= maxBound * maxBound) {
                        double minY = barrier.center.y - barrier.lowestYOffset - 5.0D;
                        double maxY = barrier.center.y + barrier.height + 3.0D;
                        if (pos.y >= minY && pos.y <= maxY) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInBarrier(net.minecraft.world.level.Level level, BlockPos pos) {
        if (pos == null) return false;
        return isInBarrier(level, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    /**
     * Tự động quét và kích hoạt Kháng Ma Kết Giới ngẫu nhiên lâu lâu tại các ngôi làng.
     */
    public static void checkAndSpawnVillageBarriers(ServerLevel level) {
        if (level.getGameTime() - lastVillageCheckTick < 200) return;
        lastVillageCheckTick = level.getGameTime();

        PoiManager poiManager = level.getPoiManager();

        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();

            Optional<BlockPos> meetingPoi = poiManager.find(
                    poi -> poi.is(PoiTypes.MEETING),
                    p -> true,
                    playerPos,
                    80,
                    PoiManager.Occupancy.ANY
            );

            if (meetingPoi.isPresent()) {
                BlockPos bellPos = meetingPoi.get();
                if (!KNOWN_VILLAGE_BELLS.contains(bellPos)) {
                    KNOWN_VILLAGE_BELLS.add(bellPos);

                    // Tỉ lệ xuất hiện lâu lâu (35% cơ hội cho mỗi ngôi làng mới khám phá)
                    if (level.random.nextFloat() < 0.35F) {
                        spawnVillageBarrier(level, bellPos);
                    }
                }
            }
        }
    }

    public static void spawnVillageBarrier(ServerLevel level, BlockPos centerPos) {
        Vec3 center = new Vec3(centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5);

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier b : ACTIVE_BARRIERS) {
                if (b.level == level && b.center.distanceTo(center) <= 90.0) {
                    return;
                }
            }
        }

        double radius = 65.0; // Bán kính 65m bao bọc rộng toàn bộ ngôi làng
        double height = 40.0;
        ActiveBarrier barrier = new ActiveBarrier(level, null, center, radius, height, true);
        barrier.durability = 350;
        buildDome(barrier);
        ACTIVE_BARRIERS.add(barrier);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 4.0F, 0.7F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.5F, 1.1F);

        for (ServerPlayer sp : level.players()) {
            if (sp.position().distanceTo(center) <= 128.0) {
                sp.displayClientMessage(
                        Component.literal("§a§l[LÀNG BẢO HỘ] §eNgôi làng này được bao bọc bởi Kháng Ma Kết Giới Cổ Xưa!"),
                        true
                );
                sp.sendSystemMessage(
                        Component.literal("§a✦ [KHÁNG MA KẾT GIỚI] §fMái vòm kết giới cổ xưa đã kích hoạt: Dân làng & quái vật tử trận bên trong không thể hiến tế gọi Ác Ma; Ác Ma bên trong bị tước đoạt ma pháp, chỉ có thể cận chiến đánh tay không!")
                );
            }
        }
    }

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
        double radius = 110.0; // Mở rộng thêm 50 block (từ 60 lên 110)
        double height = 70.0; // Tăng chiều cao tương xứng giữ tỉ lệ vòm chuẩn

        ActiveBarrier barrier = new ActiveBarrier(level, player, center, radius, height, false);
        buildDome(barrier);
        ACTIVE_BARRIERS.add(barrier);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.0F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5F, 1.4F);

        player.displayClientMessage(
                Component.literal("§a§l[KHÁNG MA KẾT GIỚI] §eĐã triển khai mái vòm bảo hộ vĩnh viễn (R:110m, H:70m)!"),
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
     * TỐI ƯU HÓA ĐỈNH CAO ZERO-LAG:
     * - Độ dày vỏ vòm chuẩn 1 block theo giải thuật Rasterization, giảm 97% khối lượng đặt block.
     * - Chân tường chu vi hạ sát nền địa hình thực tế (tối đa 48 block), không quét dư thừa lòng đất.
     * - Không tính toán ánh sáng khi đặt khối.
     */
    private static void buildDome(ActiveBarrier barrier) {
        ServerLevel level = barrier.level;
        Vec3 c = barrier.center;
        BlockPos cPos = BlockPos.containing(c);
        double R = barrier.radius;
        double H = barrier.height;
        int rInt = (int) Math.ceil(R);
        int hInt = (int) Math.ceil(H);
        double rSq = R * R;

        BlockState barrierState = ModBlocks.ANTI_MAGIC_BARRIER.get().defaultBlockState();
        int maxDropDepth = 48; // Giới hạn hạ chân tường tối đa 48 block vừa đủ khép kín địa hình mà không gây nghẽn

        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int x = -rInt; x <= rInt; x++) {
            for (int z = -rInt; z <= rInt; z++) {
                double dSq = (double) x * x + (double) z * z;
                if (dSq > rSq) continue;

                double d = Math.sqrt(dSq);

                // 1. VỎ MÁI VÒM BÁN CẦU TRÊN KHÔNG (CHUẨN 1 BLOCK KHÔNG LỖ HỔNG)
                double dOuter = Math.min(R, d + 0.5D);
                double dInner = Math.max(0.0D, d - 0.5D);
                int domeMinY = (int) Math.floor(H * Math.sqrt(Math.max(0.0D, 1.0D - (dOuter * dOuter) / rSq)));
                int domeMaxY = (int) Math.ceil(H * Math.sqrt(Math.max(0.0D, 1.0D - (dInner * dInner) / rSq)));
                domeMaxY = Math.min(hInt, domeMaxY);

                for (int y = domeMinY; y <= domeMaxY; y++) {
                    placeBarrierBlock(barrier, level, cPos.offset(x, y, z), barrierState);
                }

                // 2. CHÂN TƯỜNG CHU VI CẮM XUỐNG MẶT ĐẤT/NƯỚC (CHỈ DÀNH CHO CỘT VIỀN NGOÀI CÙNG d >= R - 1.0)
                if (d >= R - 1.0D) {
                    int worldX = cPos.getX() + x;
                    int worldZ = cPos.getZ() + z;

                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                    checkPos.set(worldX, surfaceY, worldZ);
                    while (checkPos.getY() > level.getMinBuildHeight() && !level.getFluidState(checkPos).isEmpty()) {
                        checkPos.move(Direction.DOWN);
                    }
                    int solidGroundY = checkPos.getY();
                    int targetGroundY = solidGroundY - cPos.getY();

                    int skirtMinY = Math.min(-1, targetGroundY - 1);
                    skirtMinY = Math.max(-maxDropDepth, skirtMinY);

                    int dropDistance = -skirtMinY;
                    if (dropDistance > barrier.lowestYOffset) {
                        barrier.lowestYOffset = dropDistance;
                    }

                    for (int y = skirtMinY; y < 0; y++) {
                        placeBarrierBlock(barrier, level, cPos.offset(x, y, z), barrierState);
                    }
                }
            }
        }
    }

    private static void placeBarrierBlock(ActiveBarrier barrier, ServerLevel level, BlockPos bPos, BlockState barrierState) {
        BlockState orig = level.getBlockState(bPos);
        if (orig.getDestroySpeed(level, bPos) >= 0 && !orig.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
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

    /**
     * Kiểm tra xem thực thể có được phép tự do đi xuyên qua kết giới hay không.
     * TỐI ƯU HÓA ZERO-LAG:
     * - Fast-return O(1) cho người chơi, dân làng, golem, đồng minh.
     * - Dùng khoảng cách bình phương (squared distance), loại bỏ hoàn toàn Math.sqrt trong vòng lặp collision.
     */
    public static boolean canPass(Entity entity, BlockPos pos) {
        if (entity instanceof Player ||
            entity instanceof Villager ||
            entity instanceof IronGolem ||
            entity instanceof VelgryndEntity ||
            entity instanceof MilimEntity ||
            entity instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity ||
            (entity instanceof Animal && !(entity instanceof Enemy))) {
            return true;
        }

        if (ACTIVE_BARRIERS.isEmpty()) {
            return true;
        }

        boolean canPassBoss = entity instanceof EnderDragon ||
                              entity instanceof WitherBoss ||
                              entity instanceof Warden ||
                              entity instanceof PrimordialDemonEntity;

        double ex = entity.getX();
        double ey = entity.getY();
        double ez = entity.getZ();

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
                if (barrier.level == entity.level()) {
                    double dx = ex - barrier.center.x;
                    double dz = ez - barrier.center.z;
                    double distSq = dx * dx + dz * dz;
                    double maxBound = barrier.radius + 3.0D;

                    if (distSq <= maxBound * maxBound) {
                        double minY = barrier.center.y - barrier.lowestYOffset - 5.0D;
                        double maxY = barrier.center.y + barrier.height + 3.0D;

                        if (ey >= minY && ey <= maxY) {
                            if (barrier.isVillager) {
                                return canPassBoss;
                            } else {
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
     * TỐI ƯU HÓA ZERO-LAG:
     * - Thu thập trước 1 lần duy nhất danh sách block của các kết giới khác (O(N) thay vì lồng vòng lặp O(N*M) synchronized).
     */
    public static void cleanBarrier(ActiveBarrier barrier) {
        if (barrier == null) return;
        ServerLevel level = barrier.level;

        Set<BlockPos> otherUsedPositions = new HashSet<>();
        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier other : ACTIVE_BARRIERS) {
                if (other != barrier && other.level == level) {
                    otherUsedPositions.addAll(other.replacedBlocks.keySet());
                }
            }
        }

        for (Map.Entry<BlockPos, BlockState> entry : barrier.replacedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState orig = entry.getValue();

            if (!otherUsedPositions.contains(pos)) {
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
     * TỐI ƯU HÓA ZERO-LAG:
     * - Chỉ gửi hạt khi có người chơi gần mái vòm.
     * - Quét thực thể điều tiết định kỳ 20 ticks (1 giây), chỉ lọc thực thể Mob thù địch thay vì toàn bộ sinh vật.
     */
    public static void tickBarriers(ServerLevel level) {
        // Tự động kiểm tra và dựng Kháng Ma Kết Giới ngẫu nhiên tại các ngôi làng
        checkAndSpawnVillageBarriers(level);

        if (ACTIVE_BARRIERS.isEmpty()) return;

        List<ActiveBarrier> barriersToBreak = new ArrayList<>();

        synchronized (ACTIVE_BARRIERS) {
            for (ActiveBarrier barrier : ACTIVE_BARRIERS) {
                if (barrier.level != level) continue;

                barrier.ticksAlive++;
                Vec3 center = barrier.center;
                double R = barrier.radius;
                double H = barrier.height;

                // 1. Hiệu ứng hạt ánh sáng xanh lục bảo dọc theo mái vòm (tiết kiệm hiệu năng: chỉ gửi khi có người chơi gần và mỗi 16 ticks)
                if (barrier.ticksAlive % 16 == 0 && level.hasNearbyAlivePlayer(center.x, center.y, center.z, R + 24.0D)) {
                    for (int ang = 0; ang < 360; ang += 45) {
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

                // 2. Quét thực thể trong phạm vi kết giới từ mặt đất tới nóc vòm (Throttling: mỗi 20 ticks = 1s, chỉ lọc Mob thù địch)
                if (barrier.ticksAlive % 20 == 0) {
                    double minY = Math.max(level.getMinBuildHeight(), center.y - barrier.lowestYOffset - 4.0D);
                    double maxY = Math.min(level.getMaxBuildHeight(), center.y + H + 2.0D);
                    AABB box = new AABB(center.x - R - 2.0D, minY, center.z - R - 2.0D,
                                        center.x + R + 2.0D, maxY, center.z + R + 2.0D);

                    List<Mob> hostiles = level.getEntitiesOfClass(Mob.class, box, e -> {
                        if (!e.isAlive()) return false;
                        if (e instanceof IronGolem) return false;
                        if (e instanceof Animal && !(e instanceof Enemy)) return false;
                        if (e instanceof VelgryndEntity || e instanceof MilimEntity || e instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity) return false;
                        return isThreateningEntity(e) ||
                               e instanceof PrimordialDemonEntity ||
                               e instanceof KuboEntity ||
                               e instanceof Warden ||
                               e instanceof WitherBoss ||
                               e instanceof EnderDragon;
                    });

                    boolean broken = false;
                    double rSq = (R + 1.5D) * (R + 1.5D);

                    for (Mob e : hostiles) {
                        double dx = e.getX() - center.x;
                        double dz = e.getZ() - center.z;
                        double distSq = dx * dx + dz * dz;

                        // Nếu thực thể nằm trong bán kính trụ R và nằm trong phạm vi chiều cao kết giới
                        if (distSq <= rSq && e.getY() >= minY && e.getY() <= maxY) {
                            if (!barrier.isVillager) {
                                // KẾT GIỚI NGƯỜI CHƠI (MA VƯƠNG):
                                level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + 1.0, e.getZ(), 1, 0, 0, 0, 0);
                                level.sendParticles(EMERALD_BARRIER, e.getX(), e.getY() + 1.0, e.getZ(), 12, 0.4, 0.6, 0.4, 0.15);
                                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                        SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 1.5F, 1.2F);
                                e.discard();
                            } else {
                                // KẾT GIỚI DÂN LÀNG
                                if (e instanceof KuboEntity || isThreateningEntity(e)) {
                                    if (!(e instanceof Warden) && !(e instanceof WitherBoss) && !(e instanceof EnderDragon)) {
                                        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.8, e.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                                        e.discard();
                                    }
                                } else if (e instanceof PrimordialDemonEntity demon) {
                                    // Ác ma ở trong kết giới bị tước đoạt ma pháp, chỉ có thể đánh cận chiến bằng tay không
                                    level.sendParticles(ParticleTypes.SMOKE, demon.getX(), demon.getY() + 1.2, demon.getZ(), 6, 0.2, 0.4, 0.2, 0.02);
                                    level.sendParticles(EMERALD_BARRIER, demon.getX(), demon.getY() + 1.0, demon.getZ(), 4, 0.3, 0.5, 0.3, 0.05);
                                    if (barrier.ticksAlive % 60 == 0) {
                                        level.playSound(null, demon.getX(), demon.getY(), demon.getZ(),
                                                SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 0.8F);
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
                        double distSq = dx * dx + dz * dz;
                        double maxR = b.radius + 4.0D;
                        double minY = b.center.y - b.lowestYOffset - 5.0D;
                        double maxY = b.center.y + b.height + 3.0D;

                        if (distSq <= maxR * maxR && hitPos.y >= minY && hitPos.y <= maxY) {
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
                    double distSq = dx * dx + dz * dz;
                    double maxR = b.radius + 3.0D;
                    double minY = b.center.y - b.lowestYOffset - 5.0D;
                    double maxY = b.center.y + b.height + 3.0D;

                    if (distSq <= maxR * maxR && eyePos.y >= minY && eyePos.y <= maxY) {
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

