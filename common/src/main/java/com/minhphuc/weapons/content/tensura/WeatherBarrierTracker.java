package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.client.renderer.ClientCinematicSkillRenderer;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trình điều phối và kiểm tra vùng bảo hộ thời tiết của Kháng Ma Kết Giới và Long Giam Vô Hạn.
 * Khi ở trong kết giới:
 * - Thời tiết trở nên trong lành, bầu trời quang đãng, nắng ấm, không còn âm thanh mưa hay sấm sét.
 * - Mưa, tuyết, bão tố không thể lọt vào bên trong kết giới.
 * - Sét đánh và đóng băng/tuyết rơi tự nhiên bị ngăn chặn hoàn toàn.
 */
public class WeatherBarrierTracker {

    public record ClientBarrier(UUID id, Vec3 center, double radius, double height, double skirtDepth, boolean isVillager) {}

    private static final Map<UUID, ClientBarrier> CLIENT_BARRIERS = new ConcurrentHashMap<>();

    public static void addClientBarrier(UUID id, Vec3 center, double radius, double height, double skirtDepth, boolean isVillager) {
        CLIENT_BARRIERS.put(id, new ClientBarrier(id, center, radius, height, skirtDepth, isVillager));
    }

    public static void removeClientBarrier(UUID id) {
        CLIENT_BARRIERS.remove(id);
    }

    public static void clearClientBarriers() {
        CLIENT_BARRIERS.clear();
    }

    /**
     * Lấy độ cao mái vòm cao nhất tại (x, z) để chặn đứng mọi mưa/tuyết ngay tại mặt vòm bên trên.
     */
    public static int getProtectedRoofY(Level level, int x, int z) {
        if (level == null) return Integer.MIN_VALUE;
        int maxRoofY = Integer.MIN_VALUE;

        // 1. Kiểm tra trên ServerLevel hoặc IntegratedServer
        if (level instanceof ServerLevel sl) {
            maxRoofY = Math.max(maxRoofY, AntiMagicBarrierManager.getDomeRoofY(sl, x, z));
            maxRoofY = Math.max(maxRoofY, InfiniteDragonPrisonAbility.getPrisonRoofY(sl, x, z));
        }

        // 2. Kiểm tra trên ClientLevel qua CLIENT_BARRIERS
        for (ClientBarrier barrier : CLIENT_BARRIERS.values()) {
            double dx = (x + 0.5) - barrier.center.x;
            double dz = (z + 0.5) - barrier.center.z;
            double distSq = dx * dx + dz * dz;
            double R = barrier.radius;
            if (distSq <= R * R) {
                double normR = Math.sqrt(distSq) / R;
                double roof = barrier.center.y + barrier.height * Math.sqrt(Math.max(0.0, 1.0 - normR * normR));
                maxRoofY = Math.max(maxRoofY, (int) Math.ceil(roof));
            }
        }

        // 3. Client Infinite Dragon Prison
        maxRoofY = Math.max(maxRoofY, ClientCinematicSkillRenderer.getPrisonRoofY(x, z));

        // 4. Singleplayer fallback
        if (maxRoofY == Integer.MIN_VALUE) {
            maxRoofY = Math.max(maxRoofY, AntiMagicBarrierManager.getDomeRoofY(level, x, z));
        }

        return maxRoofY;
    }

    public static boolean isWeatherProtected(Level level, BlockPos pos) {
        if (pos == null) return false;
        return isWeatherProtected(level, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public static boolean isWeatherProtected(Level level, Vec3 pos) {
        if (pos == null) return false;

        // 1. Kiểm tra trên ServerLevel
        if (level instanceof ServerLevel sl) {
            if (AntiMagicBarrierManager.isInBarrier(sl, pos)) return true;
            if (InfiniteDragonPrisonAbility.isInPrison(sl, pos)) return true;
            return false;
        }

        // 2. Kiểm tra trên ClientLevel qua danh sách kết giới đã đồng bộ
        for (ClientBarrier barrier : CLIENT_BARRIERS.values()) {
            double dx = pos.x - barrier.center.x;
            double dz = pos.z - barrier.center.z;
            double distSq = dx * dx + dz * dz;
            double maxBound = barrier.radius + 1.5;
            if (distSq <= maxBound * maxBound) {
                double minY = barrier.center.y - barrier.skirtDepth - 5.0;
                double maxY = barrier.center.y + barrier.height + 3.0;
                if (pos.y >= minY && pos.y <= maxY) {
                    return true;
                }
            }
        }

        // 3. Kiểm tra Lồng Giam Vô Hạn trên Client qua ClientCinematicSkillRenderer
        if (ClientCinematicSkillRenderer.isInsidePrison(pos)) {
            return true;
        }

        // 4. Dự phòng khi chơi Singleplayer (Integrated Server cùng chia sẻ memory)
        if (level != null) {
            if (AntiMagicBarrierManager.isInBarrier(level, pos)) return true;
            if (InfiniteDragonPrisonAbility.isInPrison(level, pos)) return true;
        }

        return false;
    }

    /**
     * Kiểm tra xem góc nhìn Camera của người chơi hiện tại có đang nằm trong kết giới hay không.
     */
    public static boolean isClientCameraProtected() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Camera camera = mc.gameRenderer != null ? mc.gameRenderer.getMainCamera() : null;
        if (camera == null) return false;
        return isWeatherProtected(mc.level, camera.getPosition());
    }

    /**
     * Dùng cho mixin độc lập không có sẵn tham chiếu Level ở client side.
     */
    public static boolean isPosWeatherProtected(BlockPos pos) {
        if (pos == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return isWeatherProtected(mc.level, pos);
        }
        return false;
    }
}
