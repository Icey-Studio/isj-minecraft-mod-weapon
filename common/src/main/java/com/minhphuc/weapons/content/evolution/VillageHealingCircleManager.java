package com.minhphuc.weapons.content.evolution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Ma Pháp Trận Chữa Lành Tại Làng (Village Healing Circle):
 * - Khi người chơi có kỹ năng đã bị tiêu hao (tiến hóa), tại Làng sẽ xuất hiện ma pháp trận có cột sáng xanh lam.
 * - Người chơi bước vào vòng tròn: Hồi phục toàn bộ kỹ năng đã tiêu hao.
 * - Kỹ năng Tuyệt Diệt Tinh Tú (Extinction Stars) của Thái Tuế có thể phá hủy ma pháp trận này!
 */
public class VillageHealingCircleManager {

    private static final DustParticleOptions CYAN_DUST = new DustParticleOptions(new Vector3f(0.15F, 0.85F, 1.0F), 1.8F);

    public static class ActiveHealingCircle {
        public final BlockPos pos;
        public final Vec3 center;
        public final double radius = 4.0;
        public int ticksAlive = 0;

        public ActiveHealingCircle(BlockPos pos) {
            this.pos = pos;
            this.center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
        }
    }

    private static final Map<BlockPos, ActiveHealingCircle> ACTIVE_CIRCLES = new ConcurrentHashMap<>();

    public static void tick(ServerLevel level) {
        // Kiểm tra xem có người chơi nào cần hồi kỹ năng không
        List<ServerPlayer> playersWithConsumed = new ArrayList<>();
        for (ServerPlayer sp : level.players()) {
            if (EvolvedSkillHelper.hasConsumedSkills(sp)) {
                playersWithConsumed.add(sp);
            }
        }

        // Nếu có người chơi cần hồi chiêu, tự động phát hiện làng xung quanh họ để dựng Ma Pháp Trận
        for (ServerPlayer player : playersWithConsumed) {
            if (level.getGameTime() % 100 == 0) { // Quét mỗi 5 giây
                detectAndSpawnCircleNearPlayer(level, player);
            }
        }

        if (ACTIVE_CIRCLES.isEmpty()) return;

        Iterator<Map.Entry<BlockPos, ActiveHealingCircle>> it = ACTIVE_CIRCLES.entrySet().iterator();
        while (it.hasNext()) {
            ActiveHealingCircle circle = it.next().getValue();
            circle.ticksAlive++;
            Vec3 c = circle.center;

            // 1. Vẽ cột sáng xanh lam chọc trời (Cyan Beacon Pillar) & Vòng tròn ma thuật mặt đất mỗi 8 ticks (giảm 65% tải mạng)
            if (circle.ticksAlive % 8 == 0) {
                // Cột sáng thẳng đứng lên 45 block
                for (double y = 0; y < 45.0; y += 3.0) {
                    level.sendParticles(CYAN_DUST, c.x, c.y + y, c.z, 1, 0.1, 0.1, 0.1, 0);
                    if (y % 6.0 < 1.0) {
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + y, c.z, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                // Vòng ma trận mặt đất bán kính 4.0 block
                for (int ang = 0; ang < 360; ang += 45) {
                    double rad = Math.toRadians(ang);
                    double x = c.x + Math.cos(rad) * circle.radius;
                    double z = c.z + Math.sin(rad) * circle.radius;
                    level.sendParticles(CYAN_DUST, x, c.y, z, 1, 0, 0, 0, 0);
                    if (circle.ticksAlive % 16 == 0) {
                        level.sendParticles(ParticleTypes.GLOW, x, c.y + 0.1, z, 1, 0.02, 0.05, 0.02, 0.01);
                    }
                }
            }

            // 2. Âm thanh thánh ca ma trận mỗi 80 ticks
            if (circle.ticksAlive % 80 == 0) {
                level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.2F, 1.4F);
            }

            // 3. Kiểm tra người chơi bước vào Ma Pháp Trận để hồi phục
            for (ServerPlayer player : level.players()) {
                if (player.position().distanceTo(c) <= circle.radius + 0.5) {
                    if (EvolvedSkillHelper.hasConsumedSkills(player)) {
                        EvolvedSkillHelper.restoreAllConsumedSkills(player);

                        com.minhphuc.weapons.network.ModMessages.sendToPlayer(
                                new com.minhphuc.weapons.network.ClientboundSyncEvolutionPacket(
                                        new java.util.ArrayList<>(),
                                        EvolvedSkillHelper.getEvolvedSkillId(player),
                                        EvolvedSkillHelper.getEvolvedSkillTier(player)
                                ), player);

                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.6F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.5F, 1.2F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 1.2F);

                        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.6, 1.0, 0.6, 0.3);
                        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 0.8, 0.5, 0.05);

                        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§b§l【 MA PHÁP TRẬN CHỮA LÀNH 】")));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a✦ Đã khôi phục toàn bộ kỹ năng đã tiêu hao! ✦")));

                        player.sendSystemMessage(Component.literal("§a══════════════════════════════════════════════════"));
                        player.sendSystemMessage(Component.literal("§b✦ MA PHÁP TRẬN LÀNG: §aToàn bộ kỹ năng nguyên liệu đã được hồi phục nguyên vẹn!"));
                        player.sendSystemMessage(Component.literal("§7(Giờ đây bạn có thể tiếp tục sử dụng hoặc đem đi dung hợp tiếp)"));
                        player.sendSystemMessage(Component.literal("§a══════════════════════════════════════════════════"));
                    }
                }
            }
        }
    }

    private static void detectAndSpawnCircleNearPlayer(ServerLevel level, ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        // 1. Tìm chuông làng (Meeting POI) trong bán kính 64 block
        PoiManager poiManager = level.getPoiManager();
        Optional<BlockPos> meetingPoi = poiManager.find(
                poi -> poi.is(PoiTypes.MEETING),
                p -> true,
                playerPos,
                64,
                PoiManager.Occupancy.ANY
        );

        if (meetingPoi.isPresent()) {
            BlockPos bellPos = meetingPoi.get();
            if (!ACTIVE_CIRCLES.containsKey(bellPos)) {
                ACTIVE_CIRCLES.put(bellPos, new ActiveHealingCircle(bellPos));
                level.playSound(null, bellPos.getX(), bellPos.getY(), bellPos.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 1.2F);
            }
            return;
        }

        // 2. Nếu không có chuông, tìm khu nhà dân làng (HOME POI) trong bán kính 32 block (chỉ quét các chunk đã nạp)
        Optional<BlockPos> homePoi = poiManager.find(
                poi -> poi.is(PoiTypes.HOME),
                p -> true,
                playerPos,
                32,
                PoiManager.Occupancy.ANY
        );

        if (homePoi.isPresent()) {
            BlockPos homePos = homePoi.get();
            BlockPos groundPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, homePos);
            if (!ACTIVE_CIRCLES.containsKey(groundPos)) {
                ACTIVE_CIRCLES.put(groundPos, new ActiveHealingCircle(groundPos));
                level.playSound(null, groundPos.getX(), groundPos.getY(), groundPos.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 1.2F);
            }
        }
    }

    /**
     * Bị phá hủy bởi Tuyệt Diệt Tinh Tú (Extinction Stars / TaisuiExtinctionStarsAbility)
     */
    public static boolean destroyCirclesOnPath(ServerLevel level, ServerPlayer player, Vec3 eyePos, Vec3 targetEnd) {
        if (ACTIVE_CIRCLES.isEmpty()) return false;

        Vec3 ab = targetEnd.subtract(eyePos);
        double abLenSq = ab.lengthSqr();
        if (abLenSq < 1e-4) return false;

        boolean anyDestroyed = false;
        Iterator<Map.Entry<BlockPos, ActiveHealingCircle>> it = ACTIVE_CIRCLES.entrySet().iterator();

        while (it.hasNext()) {
            ActiveHealingCircle circle = it.next().getValue();
            Vec3 p = circle.center;

            Vec3 ap = p.subtract(eyePos);
            double t = ap.dot(ab) / abLenSq;
            t = Math.max(0.0, Math.min(1.0, t));
            Vec3 closestPoint = eyePos.add(ab.scale(t));

            if (closestPoint.distanceTo(p) <= 6.5) {
                it.remove();
                anyDestroyed = true;

                // Hiệu ứng vỡ nát cột sáng chữa lành
                level.playSound(null, p.x, p.y, p.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.5F, 0.5F);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 1.2F);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.6F);

                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 2.0, p.z, 80, 2.0, 5.0, 2.0, 0.2);
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y + 1.0, p.z, 2, 0, 0, 0, 0);

                player.sendSystemMessage(Component.literal("§4§l⚡ TUYỆT DIỆT TINH TÚ: §cĐã bắn nổ phá hủy Ma Pháp Trận Chữa Lành của Ngôi Làng!"));
            }
        }

        return anyDestroyed;
    }
}
