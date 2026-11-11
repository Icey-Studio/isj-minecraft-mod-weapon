package com.minhphuc.weapons.content.evolution;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kỹ Năng Tối Thượng 4: Long Giam Vô Hạn (Infinite Dragon Prison)
 * - Mái vòm khối kết giới hình cầu kín hoàn toàn (bao trọn cả mặt đất/lòng đất) bằng khối kết giới hoàng kim lấp lánh (màu cột sáng jacob_light_pillar).
 * - Tồn tại vĩnh viễn cho đến khi người chơi đấm TAY KHÔNG vào khối lồng thì mới hóa giải.
 * - Người chơi tự do ra vào; mob có thể vào từ bên ngoài nhưng KHÔNG THỂ RA ngoài.
 * - Mob bị nhốt sẽ điên loạn tấn công cắn xé lẫn nhau (Infighting / Frenzy).
 * - Mức 1: Bán kính 6m, nhốt Boss & mob thường. Ác ma, Milim, Không Vong, Long Chủng phá được ngay.
 * - Mức 2: Bán kính 12m, nhốt Boss, mob thường, Ác ma. Milim, Không Vong, Long Chủng bị nhốt 30s rồi mới phá.
 * - Mức 3: Bán kính 20m, nhốt vĩnh viễn Boss, Ác Ma, Không Vong. Riêng Milim và Chước Nhiệt Long (Velgrynd) chỉ nhốt được 3 phút (3600 ticks) rồi chúng sẽ phá vỡ lồng giam!
 */
public class InfiniteDragonPrisonAbility {

    private static final DustParticleOptions GOLD_BARRIER = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.15F), 1.8F);

    public static class ActivePrison {
        public final UUID id = UUID.randomUUID();
        public final ServerLevel level;
        public final UUID casterUuid;
        public final Vec3 center;
        public final double radius;
        public final double domeHeight;
        public final double skirtDepth;
        public final int tier;
        public final long createTick;
        public int ticksAlive = 0;
        public int maxDurationTicks = -1; // -1 = vĩnh cửu (người chơi), >0 = tạm thời (ác ma)
        public final Map<BlockPos, BlockState> replacedBlocks = new ConcurrentHashMap<>();

        public ActivePrison(ServerLevel level, ServerPlayer caster, Vec3 center, double radius, double domeHeight, double skirtDepth, int tier) {
            this.level = level;
            this.casterUuid = caster != null ? caster.getUUID() : UUID.randomUUID();
            this.center = center;
            this.radius = radius;
            this.domeHeight = Math.max(radius, domeHeight);
            this.skirtDepth = Math.max(3.0, skirtDepth);
            this.tier = tier;
            this.createTick = level.getGameTime();
        }
    }

    private static final List<ActivePrison> ACTIVE_PRISONS = Collections.synchronizedList(new ArrayList<>());

    /**
     * Dò tìm mặt đất thật sự bên dưới tọa độ (x, startY, z), xuyên qua toàn bộ tán lá cây, thân gỗ, cỏ, hoa, dây leo
     * để đảm bảo Lồng Giam Vô Hạn luôn chạm khít mặt đất mà không bị treo lơ lửng trên ngọn cây.
     */
    public static double findTrueGroundY(ServerLevel level, double x, double startY, double z) {
        int bx = net.minecraft.util.Mth.floor(x);
        int bz = net.minecraft.util.Mth.floor(z);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, net.minecraft.util.Mth.floor(startY) + 2);
        int minY = level.getMinBuildHeight();

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; y--) {
            mpos.set(bx, y, bz);
            BlockState state = level.getBlockState(mpos);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced()) continue;
            // Bỏ qua toàn bộ lá cây, thân gỗ, hoa, chồi cây, dây leo, mạng nhện
            if (state.is(net.minecraft.tags.BlockTags.LEAVES)
                    || state.is(net.minecraft.tags.BlockTags.LOGS)
                    || state.is(net.minecraft.tags.BlockTags.FLOWERS)
                    || state.is(net.minecraft.tags.BlockTags.SAPLINGS)
                    || state.is(net.minecraft.tags.BlockTags.CROPS)
                    || state.is(net.minecraft.world.level.block.Blocks.VINE)
                    || state.is(net.minecraft.world.level.block.Blocks.COBWEB)
                    || state.is(net.minecraft.world.level.block.Blocks.BAMBOO)) {
                continue;
            }
            if (state.isSolid()) {
                return y + 1.0D;
            }
        }
        return startY;
    }

    /**
     * Quét mặt đất thật sự quanh chu vi bán kính r để tìm độ sâu chân váy lồng giam (skirtDepth),
     * đảm bảo địa hình dốc hoặc thung lũng không bao giờ tạo ra khe hở ở đáy bán cầu.
     */
    public static double computePerimeterSkirtDepth(ServerLevel level, double centerX, double groundY, double centerZ, double radius) {
        double minPerimeterY = groundY;
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI * 2.0D / 12.0D;
            double px = centerX + Math.cos(ang) * radius;
            double pz = centerZ + Math.sin(ang) * radius;
            double py = findTrueGroundY(level, px, groundY + 8.0D, pz);
            if (py < minPerimeterY) {
                minPerimeterY = py;
            }
        }
        return Math.max(3.5D, Math.min(24.0D, (groundY - minPerimeterY) + 2.5D));
    }

    private static BlockState getOriginalBlockState(ServerLevel level, BlockPos pos) {
        synchronized (ACTIVE_PRISONS) {
            for (ActivePrison p : ACTIVE_PRISONS) {
                if (p.level == level && p.replacedBlocks.containsKey(pos)) {
                    BlockState s = p.replacedBlocks.get(pos);
                    if (!s.is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                        return s;
                    }
                }
            }
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    public static void cast(ServerLevel level, ServerPlayer player, int tier) {
        double radius = switch (tier) {
            case 1 -> 6.0;
            case 2 -> 12.0;
            default -> 20.0;
        };

        double castY = player.getY();
        double groundY = findTrueGroundY(level, player.getX(), castY, player.getZ());
        double skirtDepth = computePerimeterSkirtDepth(level, player.getX(), groundY, player.getZ(), radius);
        // Nếu người chơi đang bay trên cao hoặc đứng trên ngọn cây, vòm bán cầu tự động vươn cao từ mặt đất bao trọn cả người chơi & mục tiêu trên không
        double domeHeight = Math.max(radius, (castY - groundY) + radius * 0.85D);

        Vec3 center = new Vec3(player.getX(), groundY, player.getZ());
        Vec3 domeParams = new Vec3(domeHeight, skirtDepth, 1.0D);

        // Kích hoạt Lồng Giam Bán Cầu Đa Trùng 3D (chạm khít mặt đất, bỏ qua cây cối, ánh kim vàng & thất sắc trong suốt)
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON)) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON, player.getUUID(), center, domeParams, 72000, ClientboundCinematicSkillPacket.ACTION_START, (float) radius
            ));
        }

        // Cho phép người chơi tạo không giới hạn số lượng Long Giam Vô Hạn (kể cả chồng đè lên nhau)
        ActivePrison prison = new ActivePrison(level, player, center, radius, domeHeight, skirtDepth, tier);
        ACTIVE_PRISONS.add(prison);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0F, 1.2F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.0F);

        player.displayClientMessage(
                Component.literal("§6§l[LONG GIAM VÔ HẠN] §eĐã triển khai Lồng Giam Bán Cầu 3D (" + (int) radius + "m - Cao " + (int) domeHeight + "m - Mức " + tier + ")!"),
                true
        );
        player.sendSystemMessage(Component.literal("§e✦ Long Giam Vô Hạn: Lồng giam bán cầu đa diện 3D chạm khít mặt đất giam cầm vĩnh cửu. Đấm tay không để hóa giải!"));
    }

    public static ActivePrison spawnDemonPrison(ServerLevel level, ServerPlayer owner, UUID demonUuid, Vec3 rawCenter, double radius, int durationTicks) {
        double groundY = findTrueGroundY(level, rawCenter.x, rawCenter.y, rawCenter.z);
        double skirtDepth = computePerimeterSkirtDepth(level, rawCenter.x, groundY, rawCenter.z, radius);
        double domeHeight = Math.max(radius, (rawCenter.y - groundY) + radius * 0.85D);
        Vec3 center = new Vec3(rawCenter.x, groundY, rawCenter.z);
        Vec3 domeParams = new Vec3(domeHeight, skirtDepth, 1.0D);

        ActivePrison prison = new ActivePrison(level, owner, center, radius, domeHeight, skirtDepth, 2);
        prison.maxDurationTicks = durationTicks;
        ACTIVE_PRISONS.add(prison);

        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON)) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON, demonUuid, center, domeParams, durationTicks, ClientboundCinematicSkillPacket.ACTION_START, (float) radius
            ));
        }

        level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0F, 1.2F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.0F);
        return prison;
    }

    /**
     * Cho phép quái/thực thể bên ngoài tự do bước vào lồng giam, nhưng từ trong không thể đi ra.
     * Khi có nhiều lồng giam, chỉ áp dụng cho lồng chứa khối kết giới pos này.
     */
    public static boolean canEntityPass(Entity entity, BlockPos pos) {
        Vec3 posCenter = Vec3.atCenterOf(pos);
        synchronized (ACTIVE_PRISONS) {
            for (ActivePrison prison : ACTIVE_PRISONS) {
                if (prison.level == entity.level()) {
                    // Chỉ kiểm tra lồng giam chứa khối pos này
                    if (prison.replacedBlocks.containsKey(pos) || Math.abs(posCenter.distanceTo(prison.center) - prison.radius) <= 1.5) {
                        double distMob = entity.position().distanceTo(prison.center);
                        // Nếu sinh vật đang ở ngoài hoặc chạm mép ngoài, cho phép bước vào
                        if (distMob >= prison.radius - 0.8) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Hoàn nguyên toàn bộ các khối ban đầu khi lồng bị phá hủy hoặc giải trừ.
     * Khối chồng lấn với lồng giam khác sẽ không bị xóa, tránh làm thủng lồng giam còn lại.
     */
    public static void cleanPrison(ActivePrison prison) {
        if (prison == null) return;
        ServerLevel level = prison.level;

        // Tắt hiệu ứng 3D trên client
        ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON, prison.casterUuid, prison.center, Vec3.ZERO, 0, ClientboundCinematicSkillPacket.ACTION_STOP, 0f
        ));

        for (Map.Entry<BlockPos, BlockState> entry : prison.replacedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState orig = entry.getValue();

            // Kiểm tra xem vị trí này có còn nằm trong bất kỳ lồng giam nào KHÁC đang hoạt động không
            boolean stillUsed = false;
            synchronized (ACTIVE_PRISONS) {
                for (ActivePrison other : ACTIVE_PRISONS) {
                    if (other != prison && other.level == level && other.replacedBlocks.containsKey(pos)) {
                        stillUsed = true;
                        break;
                    }
                }
            }

            // Chỉ hoàn nguyên về khối gốc nếu không còn lồng giam nào khác sử dụng khối này
            if (!stillUsed) {
                if (level.getBlockState(pos).is(ModBlocks.DRAGON_PRISON_BARRIER.get())) {
                    level.setBlock(pos, orig, 50);
                }
            }
        }
        prison.replacedBlocks.clear();
    }

    public static void cleanPlayerPrisons(UUID playerUuid) {
        synchronized (ACTIVE_PRISONS) {
            Iterator<ActivePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActivePrison p = it.next();
                if (p.casterUuid.equals(playerUuid)) {
                    it.remove();
                    cleanPrison(p);
                }
            }
        }
    }

    public static void cleanAllPrisons(net.minecraft.world.level.Level level) {
        synchronized (ACTIVE_PRISONS) {
            Iterator<ActivePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActivePrison p = it.next();
                if (p.level == level) {
                    it.remove();
                    cleanPrison(p);
                }
            }
        }
    }

    public static void tickPrisons(ServerLevel level) {
        if (ACTIVE_PRISONS.isEmpty()) return;

        synchronized (ACTIVE_PRISONS) {
            Iterator<ActivePrison> it = ACTIVE_PRISONS.iterator();
            while (it.hasNext()) {
                ActivePrison prison = it.next();
                if (prison.level != level) continue;

                prison.ticksAlive++;
                if (prison.maxDurationTicks > 0 && prison.ticksAlive >= prison.maxDurationTicks) {
                    it.remove();
                    cleanPrison(prison);
                    continue;
                }
                Vec3 center = prison.center;
                double r = prison.radius;
                double h = prison.domeHeight;
                double skirt = prison.skirtDepth;

                // 1. Hiệu ứng hạt ánh sáng hoàng kim lấp lánh dọc vành đáy chạm đất & vòm bán cầu (mỗi 4 ticks)
                if (prison.ticksAlive % 4 == 0 && level.hasNearbyAlivePlayer(center.x, center.y, center.z, r + 24.0D)) {
                    for (int ang = 0; ang < 360; ang += 20) {
                        double rad = Math.toRadians(ang);
                        double x = center.x + Math.cos(rad) * r;
                        double z = center.z + Math.sin(rad) * r;

                        level.sendParticles(GOLD_BARRIER, x, center.y + 0.1D, z, 1, 0, 0, 0, 0);
                        level.sendParticles(GOLD_BARRIER, center.x + Math.cos(rad) * (r * 0.866D), center.y + (h * 0.5D), center.z + Math.sin(rad) * (r * 0.866D), 1, 0, 0, 0, 0);

                        if (level.random.nextFloat() <= 0.15F) {
                            level.sendParticles(ParticleTypes.END_ROD, x, center.y + level.random.nextDouble() * h * 0.6D, z, 1, 0, 0, 0, 0.02);
                        }
                    }
                }

                AABB prisonBox = new AABB(center.x - r - 2.0D, center.y - skirt - 2.0D, center.z - r - 2.0D,
                        center.x + r + 2.0D, center.y + h + 2.0D, center.z + r + 2.0D);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, prisonBox,
                        e -> e.isAlive() && !(e instanceof Player) && !PrimordialDemonEntity.isContractedAllyOfPlayer(prison.casterUuid, e));

                // 2. Giam cầm trường lực Lồng Bán Cầu Đa Diện 3D (Chạm đất + Bao quát trên không):
                // Quái chỉ có thể vào từ bên ngoài, KHÔNG THỂ RA ngoài qua vách bán cầu hoặc nóc bán cầu!
                for (LivingEntity e : entities) {
                    double dx = e.getX() - center.x;
                    double dz = e.getZ() - center.z;
                    double horizDist = Math.hypot(dx, dz);
                    double dy = e.getY() - center.y;

                    // Tính khoảng cách chuẩn hóa theo hình bán cầu (Ellipsoid trên mặt đất + Hình trụ chân váy âm xuống đất)
                    double normDist;
                    if (dy >= 0.0D) {
                        normDist = Math.hypot(horizDist / r, dy / h);
                    } else if (dy >= -skirt) {
                        normDist = horizDist / r;
                    } else {
                        normDist = 2.0D;
                    }

                    if (normDist <= 1.05D && normDist > 0.90D) {
                        double targetY = Math.clamp(e.getY(), center.y, center.y + h * 0.75D);
                        Vec3 toInterior = new Vec3(center.x - e.getX(), targetY - e.getY(), center.z - e.getZ());
                        if (toInterior.lengthSqr() > 1e-4D) {
                            toInterior = toInterior.normalize();
                        }
                        e.setDeltaMovement(toInterior.scale(0.85D));
                        if (normDist > 0.96D && horizDist > 0.1D) {
                            double maxHoriz = r * 0.88D * (dy > 0 ? Math.sqrt(Math.max(0.1D, 1.0D - Math.min(0.9D, (dy / h) * (dy / h)))) : 1.0D);
                            if (horizDist > maxHoriz) {
                                double scale = maxHoriz / horizDist;
                                e.setPos(center.x + dx * scale, Math.min(e.getY(), center.y + h * 0.88D), center.z + dz * scale);
                            } else if (dy > h * 0.88D) {
                                e.setPos(e.getX(), center.y + h * 0.85D, e.getZ());
                            }
                        }
                        e.hurtMarked = true;
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1.0, e.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
                    }

                    // Kích hoạt trạng thái điên loạn cắn xé lẫn nhau (Frenzy / Infighting mỗi 20 ticks)
                    if (prison.ticksAlive % 20 == 0 && normDist <= 1.0D && e instanceof Mob mob) {
                        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1));
                        for (LivingEntity other : entities) {
                            if (other != mob && other.isAlive()) {
                                mob.setTarget(other);
                                break;
                            }
                        }
                    }
                }

                // 3. Quét kiểm tra điều kiện phá vỡ lồng giam của Boss (Throttling mỗi 5 ticks)
                if (prison.ticksAlive % 5 == 0) {
                    boolean cageBroken = false;

                    for (LivingEntity e : entities) {
                        double horizDist = Math.hypot(e.getX() - center.x, e.getZ() - center.z);
                        double dy = e.getY() - center.y;
                        boolean insideDome = horizDist <= r && dy >= -skirt && dy <= h;

                        boolean isPrimordial = e instanceof PrimordialDemonEntity;
                        boolean isMilim = e instanceof MilimEntity;
                        boolean isKubo = e instanceof KuboEntity;
                        boolean isVelgrynd = e instanceof VelgryndEntity;

                        // Kiểm tra điều kiện phá lồng theo Mức (Tier)
                        if (insideDome) {
                            // Cứu Thế Quyền Hiện: Khi bị giam sẽ lập tức chuyển sang dạng võ thần tay không và đấm vỡ lồng giam
                            if (e instanceof com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity ct) {
                                ct.setBareHanded(true);
                                cageBroken = true;
                                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                        SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 3.5F, 0.7F);
                                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 0.9F);
                                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY() + 1.0D, e.getZ(), 2, 0, 0, 0, 0);
                                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                if (caster != null) {
                                    caster.sendSystemMessage(Component.literal("§c§l[Báo cáo] §fCá thể Cứu Thế Quyền Hiện đã chuyển sang dạng tay không và phá vỡ Lồng Giam Vô Hạn!"));
                                }
                                break;
                            }

                            if (prison.tier == 1) {
                                // Mức 1: Ác ma, Milim, Không Vong, Long Chủng phá được ngay
                                if (isPrimordial || isMilim || isKubo || isVelgrynd) {
                                    cageBroken = true;
                                    level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.6F);
                                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                                    ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                    if (caster != null) {
                                        caster.sendSystemMessage(Component.literal("§c⚠️ " + e.getName().getString() + " đã phá vỡ Lồng Giam Vô Hạn (Mức 1)!"));
                                    }
                                    break;
                                }
                            } else if (prison.tier == 2) {
                                // Mức 2: Milim, Không Vong, Long Chủng bị nhốt 30s (600 ticks) rồi mới phá lồng
                                if ((isMilim || isKubo || isVelgrynd) && prison.ticksAlive >= 600) {
                                    cageBroken = true;
                                    level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                            SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 3.0F, 0.7F);
                                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                                    ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                    if (caster != null) {
                                        caster.sendSystemMessage(Component.literal("§c⚠️ Sau 30s bị giam cầm, " + e.getName().getString() + " đã phá nát Long Giam Vô Hạn (Mức 2)!"));
                                    }
                                    break;
                                }
                            } else {
                                // Mức 3: Nhốt vĩnh viễn Boss, Ác Ma, Không Vong.
                                // Riêng Milim và Chước Nhiệt Long (Velgrynd) chỉ nhốt được 3 phút (3600 ticks) rồi chúng sẽ phá lồng!
                                if (isMilim || isVelgrynd) {
                                    // Cảnh báo khi còn 30 giây (tick 3000)
                                    if (prison.ticksAlive == 3000) {
                                        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                        if (caster != null) {
                                            caster.sendSystemMessage(Component.literal("§e⚠️ Sức mạnh của " + e.getName().getString() + " đang cuộn trào dữ dội! Long Giam Vô Hạn (Mức 3) sắp bị phá vỡ sau 30 giây!"));
                                        }
                                        level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5F, 1.2F);
                                    }

                                    if (prison.ticksAlive >= 3600) {
                                        cageBroken = true;
                                        level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.6F);
                                        level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.5F, 0.5F);
                                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY(), e.getZ(), 2, 0, 0, 0, 0);
                                        level.sendParticles(ParticleTypes.DRAGON_BREATH, e.getX(), e.getY() + 1.0, e.getZ(), 50, 1.2, 1.2, 1.2, 0.08);
                                        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(prison.casterUuid);
                                        if (caster != null) {
                                            caster.sendSystemMessage(Component.literal("§c⚠️ Sau 3 phút bị giam cầm, " + e.getName().getString() + " đã bộc phát thần lực / long uy phá tan Long Giam Vô Hạn (Mức 3)!"));
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (cageBroken) {
                        it.remove();
                        cleanPrison(prison);
                    }
                }
            }
        }
    }

    /**
     * Người chơi đấm tay không vào khối kết giới để hóa giải phong ấn.
     */
    public static boolean dispelByPunch(ServerPlayer player, BlockPos pos) {
        if (!player.getMainHandItem().isEmpty()) return false;

        ActivePrison found = null;
        double minDistance = Double.MAX_VALUE;

        synchronized (ACTIVE_PRISONS) {
            // Ưu tiên 1: Lồng giam chứa trực tiếp khối pos này trong replacedBlocks
            for (ActivePrison prison : ACTIVE_PRISONS) {
                if (prison.level == player.level() && prison.replacedBlocks.containsKey(pos)) {
                    double dist = prison.center.distanceTo(Vec3.atCenterOf(pos));
                    if (dist < minDistance) {
                        minDistance = dist;
                        found = prison;
                    }
                }
            }

            // Ưu tiên 2: Nếu không tìm thấy bằng replacedBlocks nhưng nằm trong phạm vi bán kính
            if (found == null) {
                for (ActivePrison prison : ACTIVE_PRISONS) {
                    if (prison.level == player.level() && prison.center.distanceTo(Vec3.atCenterOf(pos)) <= prison.radius + 3.0) {
                        double dist = prison.center.distanceTo(Vec3.atCenterOf(pos));
                        if (dist < minDistance) {
                            minDistance = dist;
                            found = prison;
                        }
                    }
                }
            }

            if (found != null) {
                ACTIVE_PRISONS.remove(found);
                cleanPrison(found);
            }
        }

        if (found != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5F, 1.2F);

            player.displayClientMessage(
                    Component.literal("§6§l[LONG GIAM] §aBạn đã hóa giải thành công Lồng Giam Vô Hạn!"),
                    true
            );
            return true;
        }

        return false;
    }

    /**
     * Người chơi đấm tay không vào không khí sát ranh giới lồng để hóa giải phong ấn.
     */
    public static boolean checkEmptyHandPunch(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) return false;

        Vec3 eyePos = player.getEyePosition();
        ActivePrison found = null;
        double minDiff = Double.MAX_VALUE;

        synchronized (ACTIVE_PRISONS) {
            for (ActivePrison prison : ACTIVE_PRISONS) {
                if (prison.level == player.level() && prison.casterUuid.equals(player.getUUID())) {
                    double horizDist = Math.hypot(eyePos.x - prison.center.x, eyePos.z - prison.center.z);
                    double dy = eyePos.y - prison.center.y;
                    double diff = Math.abs(horizDist - prison.radius);
                    if ((diff <= 4.0 || (horizDist <= prison.radius + 2.0 && dy >= -prison.skirtDepth - 2.0 && dy <= prison.domeHeight + 2.0)) && diff < minDiff) {
                        minDiff = diff;
                        found = prison;
                    }
                }
            }
            if (found != null) {
                ACTIVE_PRISONS.remove(found);
                cleanPrison(found);
            }
        }

        if (found != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5F, 1.2F);

            player.displayClientMessage(
                    Component.literal("§6§l[LONG GIAM] §aBạn đã hóa giải thành công Lồng Giam Vô Hạn!"),
                    true
            );
            return true;
        }

        return false;
    }

    public static int removePrisonsInArea(ServerLevel level, Vec3 center, double radius) {
        if (ACTIVE_PRISONS.isEmpty() || level == null || center == null) return 0;
        List<ActivePrison> toRemove = new ArrayList<>();
        synchronized (ACTIVE_PRISONS) {
            for (ActivePrison p : ACTIVE_PRISONS) {
                if (p.level == level && p.center.distanceTo(center) <= radius + p.radius) {
                    toRemove.add(p);
                }
            }
            for (ActivePrison p : toRemove) {
                ACTIVE_PRISONS.remove(p);
                cleanPrison(p);
            }
        }
        if (!toRemove.isEmpty()) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 1.2F);
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z, 2, 0.5, 0.5, 0.5, 0);
        }
        return toRemove.size();
    }
}
