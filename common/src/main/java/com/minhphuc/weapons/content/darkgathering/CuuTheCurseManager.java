package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QUẢN LÝ ÁN TỬ CỦA CỨU THẾ QUYỀN HIỆN:
 * Hỗ trợ 3 trạng thái đoạt:
 * 1. HEART (Đoạt Tim): 20 giây (400 ticks) - Cần Phép Màu Của Thần Linh hoặc Thị Thực để hóa giải.
 * 2. BRAIN (Đoạt Não): 15 giây (300 ticks) - Mù lòa, choáng váng - Cần Bí Ẩn Đức Tin & Ân Sủng hoặc Thị Thực để hóa giải.
 * 3. DUAL (Đoạt Cả Tim Lẫn Não): 15 giây (300 ticks) - Án tử kép cực kỳ đáng sợ.
 * 
 * TỐI ƯU HÓA HIỆU NĂNG TỐI ĐA (ZERO LAG): O(1) thoát tức thì khi rỗng.
 */
public class CuuTheCurseManager {

    public enum CurseType {
        HEART, BRAIN, DUAL
    }

    public static class ActiveCuuTheCurse {
        public final ServerLevel level;
        public final UUID victimUuid;
        public final String victimName;
        public CurseType type;
        public int ticksRemaining;

        public ActiveCuuTheCurse(ServerLevel level, UUID victimUuid, String victimName, CurseType type) {
            this.level = level;
            this.victimUuid = victimUuid;
            this.victimName = victimName;
            this.type = type;
            this.ticksRemaining = (type == CurseType.HEART) ? 400 : 300; // 20s cho tim, 15s cho não hoặc song trùng
        }
    }

    private static final Map<UUID, ActiveCuuTheCurse> ACTIVE_CURSES = new ConcurrentHashMap<>();

    public static boolean isImmune(LivingEntity entity) {
        return entity instanceof VelzardEntity || entity instanceof MilimEntity || entity instanceof VelgryndEntity;
    }

    public static boolean hasCurse(LivingEntity entity) {
        if (entity == null) return false;
        return ACTIVE_CURSES.containsKey(entity.getUUID());
    }

    public static void applyCurse(ServerLevel level, LivingEntity victim) {
        applyCurse(level, victim, CurseType.HEART);
    }

    public static void applyCurse(ServerLevel level, LivingEntity victim, CurseType type) {
        if (victim == null || !victim.isAlive() || isImmune(victim)) return;

        ACTIVE_CURSES.put(victim.getUUID(), new ActiveCuuTheCurse(level, victim.getUUID(), victim.getName().getString(), type));

        if (victim instanceof ServerPlayer playerVictim) {
            playerVictim.connection.send(new ClientboundSetTitlesAnimationPacket(0, 50, 15));

            switch (type) {
                case HEART -> {
                    playerVictim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l[ĐOẠT MẤT TRÁI TIM]")));
                    playerVictim.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cÁn tử: 20 giây để dùng Phép Màu Của Thần Linh!")));
                    playerVictim.displayClientMessage(
                            Component.literal("§4§l⚠️ BẠN ĐÃ BỊ ĐOẠT MẤT TRÁI TIM! §cSẽ tử vong sau 20.0s nếu không dùng Phép Màu Của Thần Linh hoặc Thị Thực!"),
                            false
                    );
                    playerVictim.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 0.6F);
                }
                case BRAIN -> {
                    playerVictim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§5§l[ĐOẠT MẤT BỘ NÃO]")));
                    playerVictim.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cÁn tử: 15 giây để dùng Bí Ẩn Đức Tin & Ân Sủng!")));
                    playerVictim.displayClientMessage(
                            Component.literal("§5§l⚠️ BẠN ĐÃ BỊ ĐOẠT MẤT BỘ NÃO! §cSẽ tử vong sau 15.0s nếu không dùng Bí Ẩn Đức Tin hoặc Thị Thực!"),
                            false
                    );
                    playerVictim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, true));
                    playerVictim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1, false, false, true));
                    playerVictim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, false, true));
                    playerVictim.playNotifySound(SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.8F);
                }
                case DUAL -> {
                    playerVictim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l[ĐOẠT CẢ TIM LẪN NÃO]")));
                    playerVictim.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§4⚠️ Tuyệt Mệnh Song Trùng: Tử vong sau 15 giây!")));
                    playerVictim.displayClientMessage(
                            Component.literal("§4§l⚠️ BẠN ĐÃ BỊ CẮT LÌA CẢ TIM LẪN NÃO! §cÁn tử kép 15.0s! Cần thần quang hoặc Thị Thực cấp cứu ngay!"),
                            false
                    );
                    playerVictim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, true));
                    playerVictim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, false, false, true));
                    playerVictim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, false, true));
                    playerVictim.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.2F, 0.5F);
                }
            }
        }
    }

    /**
     * Giải trừ án tử bằng Phép Màu Của Thần Linh, Bí Ẩn Đức Tin hoặc Thị Thực
     */
    public static boolean cure(LivingEntity victim) {
        return cure(victim, "Thần Lực Thanh Tẩy");
    }

    public static boolean cure(LivingEntity victim, String sourceName) {
        if (victim == null) return false;
        ActiveCuuTheCurse curse = ACTIVE_CURSES.get(victim.getUUID());
        if (curse == null) return false;

        boolean isSeerFlesh = sourceName.contains("Thị Thực") || sourceName.contains("Thần Lực Thanh Tẩy");
        boolean isDivineMiracle = sourceName.contains("Phép Màu Của Thần Linh");
        boolean isFaithAndGrace = sourceName.contains("Bí Ẩn Của Đức Tin") || sourceName.contains("Bí Mật Của Đức Tin");

        if (curse.type == CurseType.DUAL) {
            if (isSeerFlesh) {
                // Thị Thực thanh tẩy cả 2
                ACTIVE_CURSES.remove(victim.getUUID());
                notifyCureSuccess(victim, sourceName, "toàn bộ Trái Tim và Bộ Não");
                return true;
            } else if (isDivineMiracle) {
                // Phép màu thần linh phục hồi tim -> còn lại não
                curse.type = CurseType.BRAIN;
                curse.ticksRemaining = Math.min(curse.ticksRemaining, 200); // 10s còn lại
                notifyPartialCure(victim, sourceName, "Trái Tim", "Bộ Não");
                return true;
            } else if (isFaithAndGrace) {
                // Bí ẩn đức tin phục hồi não -> còn lại tim
                curse.type = CurseType.HEART;
                curse.ticksRemaining = Math.min(curse.ticksRemaining, 200); // 10s còn lại
                notifyPartialCure(victim, sourceName, "Bộ Não", "Trái Tim");
                return true;
            } else {
                ACTIVE_CURSES.remove(victim.getUUID());
                notifyCureSuccess(victim, sourceName, "toàn bộ thân thể");
                return true;
            }
        } else if (curse.type == CurseType.HEART) {
            if (isDivineMiracle || isSeerFlesh || isFaithAndGrace) {
                ACTIVE_CURSES.remove(victim.getUUID());
                notifyCureSuccess(victim, sourceName, "Trái Tim");
                return true;
            }
        } else if (curse.type == CurseType.BRAIN) {
            if (isFaithAndGrace || isSeerFlesh || isDivineMiracle) {
                ACTIVE_CURSES.remove(victim.getUUID());
                notifyCureSuccess(victim, sourceName, "Bộ Não");
                return true;
            }
        }

        ACTIVE_CURSES.remove(victim.getUUID());
        notifyCureSuccess(victim, sourceName, "thân thể");
        return true;
    }

    private static void notifyPartialCure(LivingEntity victim, String sourceName, String restoredPart, String remainingPart) {
        if (victim instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 10));
            sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l[TÁI TẠO MỘT PHẦN]")));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§aĐã hồi phục " + restoredPart + "! Vẫn còn bị đoạt " + remainingPart + "!")));
            sp.displayClientMessage(
                    Component.literal("§6§l[CỨU THẾ PHỤC SINH] §f" + sourceName + " đã tái tạo " + restoredPart + "! Mau giải trừ nốt " + remainingPart + "!"),
                    false
            );
            sp.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 1.2F);
        }
    }

    private static void notifyCureSuccess(LivingEntity victim, String sourceName, String partName) {
        if (victim instanceof ServerPlayer playerVictim) {
            playerVictim.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 10));
            playerVictim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§l[ĐÃ THANH TẨY]")));
            playerVictim.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§f" + sourceName + " đã tái tạo lại " + partName + "! ✨")));
            playerVictim.displayClientMessage(
                    Component.literal("§a§l[CỨU THẾ PHỤC SINH] §f" + sourceName + " đã phục hồi " + partName + " và hóa giải hoàn toàn án tử của Cứu Thế Quyền Hiện!"),
                    false
            );
            playerVictim.removeEffect(MobEffects.BLINDNESS);
            playerVictim.removeEffect(MobEffects.CONFUSION);
            playerVictim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            playerVictim.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 1.5F);
        }
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE_CURSES.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveCuuTheCurse>> it = ACTIVE_CURSES.entrySet().iterator();
        while (it.hasNext()) {
            ActiveCuuTheCurse curse = it.next().getValue();
            if (curse.level != level) continue;

            Entity ent = level.getEntity(curse.victimUuid);
            if (!(ent instanceof LivingEntity victim) || !victim.isAlive()) {
                it.remove();
                continue;
            }

            curse.ticksRemaining--;

            // Cập nhật debuff liên tục cho trạng thái mất Não
            if ((curse.type == CurseType.BRAIN || curse.type == CurseType.DUAL) && curse.ticksRemaining % 20 == 0) {
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 35, 0, false, false, false));
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 45, 0, false, false, false));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 2, false, false, false));
            }

            // Cập nhật cảnh báo định kỳ mỗi 1 giây (20 ticks)
            if (curse.ticksRemaining % 20 == 0) {
                int secondsLeft = curse.ticksRemaining / 20;
                if (victim instanceof ServerPlayer sp) {
                    String partDesc = switch (curse.type) {
                        case HEART -> "TIM §7(Dùng Phép Màu Của Thần Linh)";
                        case BRAIN -> "NÃO §7(Dùng Bí Ẩn Đức Tin & Ân Sủng)";
                        case DUAL -> "CẢ TIM LẪN NÃO §4§l(CẦN CẤP CỨU KHẨN CẤP!)";
                    };
                    sp.displayClientMessage(
                            Component.literal("§4§l⚠️ BẠN ĐÃ BỊ ĐOẠT " + partDesc + " ÁN TỬ CÒN: §e§l" + secondsLeft + "s"),
                            true
                    );
                    sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.8F);
                }
            }

            // Hết thời gian mà chưa được điều trị -> 100% Tử Vong!
            if (curse.ticksRemaining <= 0) {
                it.remove();
                if (victim instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 10));
                    sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l[TỬ VONG]")));
                    sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cKhông kịp điều trị phần cơ thể bị Cứu Thế Quyền Hiện đoạt!")));
                    level.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§4§l[Án Tử] §f" + sp.getName().getString() + " §cđã tử vong do không kịp điều trị vết thương đoạt mệnh của Cứu Thế Quyền Hiện!"),
                            false
                    );
                }
                victim.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }
    }

    public static void cleanup(ServerLevel level) {
        if (ACTIVE_CURSES.isEmpty()) return;
        ACTIVE_CURSES.entrySet().removeIf(entry -> entry.getValue().level == level);
    }
}
