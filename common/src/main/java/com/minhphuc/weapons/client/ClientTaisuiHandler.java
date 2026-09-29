package com.minhphuc.weapons.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xử lý hiển thị Client cho Phẫn Nộ Vương - Tuyệt Diệt Tinh Tú (Thái Tuế Tinh Quân):
 * - Tạo 1 LỚP MÀNG MỎNG mịn màng, thanh khiết, KHÔNG CHỚP CHỚP (Smooth Celestial Sphere Membrane).
 * - 5 Tinh tú trắng quay elip quanh người mượt mà, không dùng hạt flash chói mắt.
 * - QUY TẮC GÓC NHÌN:
 *   + Góc nhìn thứ nhất (First Person): KHÔNG HIỂN THỊ để màn hình thông thoáng, dễ di chuyển và quan sát.
 *   + Góc nhìn chuyển đổi (Third Person / F5) hoặc người khác nhìn vào: HIỂN THỊ ĐẦY ĐỦ lớp màng và tinh tú!
 */
public class ClientTaisuiHandler {
    private static final Map<UUID, Integer> ACTIVE_TAISUI_PLAYERS = new ConcurrentHashMap<>();

    // Hạt màng mỏng mịn màng (kích thước nhỏ 0.5F, màu ngọc lam - trắng thiên hà thanh khiết)
    private static final DustParticleOptions MEMBRANE_DUST = new DustParticleOptions(new Vector3f(0.80F, 0.92F, 1.0F), 0.5F);

    // Hạt lõi tinh tú trắng siêu sáng
    private static final DustParticleOptions STAR_CORE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.6F);

    public static boolean hasTaisui(UUID uuid) {
        if (uuid == null) return false;
        return ACTIVE_TAISUI_PLAYERS.getOrDefault(uuid, 0) > 0;
    }

    public static void setTaisuiState(UUID uuid, boolean active, int starsRemaining) {
        if (!active || starsRemaining <= 0) {
            ACTIVE_TAISUI_PLAYERS.remove(uuid);
        } else {
            ACTIVE_TAISUI_PLAYERS.put(uuid, starsRemaining);
        }
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.level == null) {
                ACTIVE_TAISUI_PLAYERS.clear();
                return;
            }

            if (ACTIVE_TAISUI_PLAYERS.isEmpty()) return;

            long gameTime = mc.level.getGameTime();
            // Throttling: Cập nhật mỗi 2 ticks để tăng 50% FPS cho máy cấu hình yếu
            if (gameTime % 2 != 0) return;

            ACTIVE_TAISUI_PLAYERS.forEach((uuid, starsRemaining) -> {
                Player player = mc.level.getPlayerByUUID(uuid);
                if (player == null || !player.isAlive()) return;

                // =========================================================================
                // QUY TẮC GÓC NHÌN:
                // Nếu là chính người chơi VÀ đang ở góc nhìn thứ nhất (First Person):
                // BỎ QUA hoàn toàn, không tạo hạt che mắt để dễ dàng di chuyển!
                // =========================================================================
                boolean isLocal = (player == mc.player);
                if (isLocal && mc.options.getCameraType().isFirstPerson()) {
                    return;
                }

                Vec3 center = player.position().add(0, 1.05D, 0);

                // 1. TẠO LỚP MÀNG MỎNG KHÔNG CHỚP CHỚP (Smooth Celestial Sphere Membrane)
                // Bán kính màng mỏng 1.95m ôm lấy thân thể (giảm số lượng hạt để bảo vệ FPS)
                double membraneRadius = 1.95D;
                double rotAngle = gameTime * 0.035D;

                // Dải xích đạo (Equator) - 8 hạt thay vì 14
                for (int i = 0; i < 8; i++) {
                    double theta = rotAngle + (i * Math.PI * 2.0D / 8.0D);
                    double px = center.x + membraneRadius * Math.cos(theta);
                    double pz = center.z + membraneRadius * Math.sin(theta);
                    mc.level.addParticle(MEMBRANE_DUST, px, center.y, pz, 0, 0, 0);
                }

                // Dải vĩ độ trên (+45 độ) - 6 hạt thay vì 10
                double rUpper = membraneRadius * 0.707D;
                double yUpper = center.y + membraneRadius * 0.707D;
                for (int i = 0; i < 6; i++) {
                    double theta = -rotAngle + (i * Math.PI * 2.0D / 6.0D);
                    double px = center.x + rUpper * Math.cos(theta);
                    double pz = center.z + rUpper * Math.sin(theta);
                    mc.level.addParticle(MEMBRANE_DUST, px, yUpper, pz, 0, 0, 0);
                }

                // Dải vĩ độ dưới (-45 độ) - 6 hạt thay vì 10
                double yLower = center.y - membraneRadius * 0.707D;
                for (int i = 0; i < 6; i++) {
                    double theta = -rotAngle + (i * Math.PI * 2.0D / 6.0D);
                    double px = center.x + rUpper * Math.cos(theta);
                    double pz = center.z + rUpper * Math.sin(theta);
                    mc.level.addParticle(MEMBRANE_DUST, px, yLower, pz, 0, 0, 0);
                }

                // 2. VẼ 5 VIÊN TINH TÚ TRẮNG QUAY XUNG QUANH (Atomic Orbitals: 2.25m)
                // Hoàn toàn KHÔNG dùng particle FLASH, tốc độ hạt = 0 để di chuyển êm dịu
                double orbitRadius = 2.25D;
                for (int i = 0; i < starsRemaining; i++) {
                    double inclination = (i * Math.PI / 5.0D) + 0.25D;
                    double orbitAngle = (gameTime * 0.15D) + (i * (Math.PI * 2.0D / 5.0D));

                    double ox = orbitRadius * Math.cos(orbitAngle);
                    double oy = orbitRadius * Math.sin(orbitAngle) * Math.sin(inclination);
                    double oz = orbitRadius * Math.sin(orbitAngle) * Math.cos(inclination);

                    double sx = center.x + ox;
                    double sy = center.y + oy;
                    double sz = center.z + oz;

                    mc.level.addParticle(STAR_CORE_DUST, sx, sy, sz, 0, 0, 0);
                    mc.level.addParticle(ParticleTypes.END_ROD, sx, sy, sz, 0, 0, 0);
                }
            });
        });
    }
}
