package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.network.ClientboundSyncRykuzinStatePacket;
import com.minhphuc.weapons.network.ClientboundTengaiSlashEffectPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KỸ NĂNG: TIÊN TRI (RYKUZIN SINKA / ROKUJIN SHINKA)
 * TỐI ƯU HÓA HIỆU SUẤT TỐI ĐA (ZERO LAG):
 * - Chỉ có 2 trạng thái: BẬT và TẮT.
 * - Khi bật: Khóa các skill khác. Nếu người chơi dùng skill khác thì tự động tắt ngay.
 * - Chỉ kết thúc khi người chơi tắt hoặc bật/sài skill khác.
 * - Khi bật: Nếu có sinh vật có ý định tấn công -> Hiện cảnh báo + đổi thế giới sang màu Trắng Đen trong 3s (60 ticks) để né đòn.
 * - Không giới hạn sinh vật nào (tất cả quái vật, boss, người chơi khác, sinh vật sống).
 * - O(1) thoát tức thì nếu không có người chơi nào bật Tiên Tri.
 */
public class RykuzinSinkaAbility {

    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_ALERT_TIME = new ConcurrentHashMap<>();

    public static boolean isActive(Player player) {
        if (player == null) return false;
        return ACTIVE_PLAYERS.contains(player.getUUID());
    }

    public static void toggle(ServerLevel level, ServerPlayer player) {
        if (player == null) return;
        UUID uuid = player.getUUID();

        if (ACTIVE_PLAYERS.contains(uuid)) {
            deactivate(player);
            player.displayClientMessage(
                    Component.literal("§e§l[TIÊN TRI - RYKUJIN SINKA] §c§lĐÃ TẮT §7(Đã ngắt kết nối thấu thị tương lai)."),
                    true
            );
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.7F);
        } else {
            ACTIVE_PLAYERS.add(uuid);
            ModMessages.sendToPlayer(new ClientboundSyncRykuzinStatePacket(true), player);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e§l[TIÊN TRI: BẬT]")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§fDự đoán sát khí & Chuyển đổi không thời gian khi bị nhắm tới")));

            player.displayClientMessage(
                    Component.literal("§e§l[TIÊN TRI - RYKUJIN SINKA] §a§lĐÃ BẬT §7(Khóa kỹ năng khác. Tự động dự đoán sát khí & đổi màu thế giới 3s khi có kẻ tấn công)."),
                    false
            );
            player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2F, 1.5F);
            player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.8F);
        }
    }

    public static void deactivate(ServerPlayer player) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        if (ACTIVE_PLAYERS.remove(uuid)) {
            LAST_ALERT_TIME.remove(uuid);
            ModMessages.sendToPlayer(new ClientboundSyncRykuzinStatePacket(false), player);
        }
    }

    public static void onPlayerQuit(ServerPlayer player) {
        if (player != null) {
            ACTIVE_PLAYERS.remove(player.getUUID());
            LAST_ALERT_TIME.remove(player.getUUID());
        }
    }

    public static void cleanup(ServerLevel level) {
        // Chỉ dọn dẹp các player thuộc level này
        if (ACTIVE_PLAYERS.isEmpty()) return;
        Iterator<UUID> it = ACTIVE_PLAYERS.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(uuid);
            if (sp == null || sp.serverLevel() == level) {
                it.remove();
                LAST_ALERT_TIME.remove(uuid);
            }
        }
    }

    /**
     * Quét sát khí theo chu kỳ 4 ticks (0.2s) - Zero Lag:
     * - Chỉ quét khi có người chơi bật Tiên Tri.
     * - Tầm quét cục bộ 20 blocks.
     */
    public static void tick(ServerLevel level) {
        if (ACTIVE_PLAYERS.isEmpty()) return;
        if (level.getGameTime() % 4 != 0) return;

        long now = System.currentTimeMillis();

        for (UUID uuid : ACTIVE_PLAYERS) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || player.serverLevel() != level || !player.isAlive() || player.isSpectator()) {
                continue;
            }

            Long lastAlert = LAST_ALERT_TIME.get(uuid);
            if (lastAlert != null && (now - lastAlert) < 3200) {
                // Đang trong thời gian 3s đổi màu thế giới, tránh spam cảnh báo
                continue;
            }

            // Quét các sinh vật sống xung quanh người chơi
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(20.0D),
                    e -> e != player && e.isAlive() && !e.isSpectator()
            );

            LivingEntity hostileEntity = null;
            for (LivingEntity e : nearby) {
                if (e instanceof Mob mob && mob.getTarget() == player) {
                    hostileEntity = e;
                    break;
                }
                // Nếu sinh vật vừa đánh trúng hoặc nhắm vào người chơi
                if (e.getLastHurtByMob() == player && e.distanceToSqr(player) <= 16.0D * 16.0D) {
                    hostileEntity = e;
                    break;
                }
            }

            if (hostileEntity != null) {
                LAST_ALERT_TIME.put(uuid, now);

                // 1. Gửi thông báo cảnh báo sát khí
                String enemyName = hostileEntity.getName().getString();
                player.displayClientMessage(
                        Component.literal("§e§l[TIÊN TRI] §c⚠️ Sát khí từ §f" + enemyName + "§c! Thế giới đảo màu 3s để né đòn!"),
                        true
                );

                // 2. Kích hoạt hiệu ứng Trắng Đen (Monochrome Manga) trong 3s (60 ticks)
                ModMessages.sendToPlayer(new ClientboundTengaiSlashEffectPacket(60), player);

                // 3. Gia tốc né tránh (Speed II + Jump Boost nhẹ trong 3s)
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false, true));

                // 4. Âm thanh ngưng đọng thời gian phản xạ
                player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 1.8F);
                player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
        }
    }
}
