package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.network.ClientboundSageAlertPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundSageActionPacket;
import com.minhphuc.weapons.world.VeldoraWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Hệ thống Trí Tuệ [Đại Hiền Giả] -> [Trí Tuệ Vương Raphael]:
 * 1. Bắt tín hiệu Dao Động Long Chủng bất thường -> Chỉ hướng & đề nghị Dịch Chuyển Không Gian tới Hang Phong Ấn Veldora.
 * 2. Cảnh báo mob cầm vũ khí Hiếm trở lên (theo anime Slime).
 * 3. Tự động chiến đấu & bảo hộ khẩn cấp (Né / Đa Trùng Kết Giới / Bơm Dược Tề khi máu < 25%).
 * 4. Giải mã nguyền rủa, quét tàng hình/xuyên tường tầm ngắn, phân tích nhược điểm, báo Veldora bận 30%.
 */
public final class SageManager {

    private static final String TAG_AUTO_BATTLE = "TensuraRaphaelAutoBattle";
    private static final String TAG_SIGNAL_WARNED = "VeldoraSignalWarnedTick";
    private static final String TAG_WEAPON_WARNED = "SageWeaponWarnTick";

    private SageManager() {}

    public static String getSpeakerLabel(ServerPlayer sp) {
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        return data.getBoolean("TensuraTrueDemonLord")
                ? "§b§l<< TRÍ TUỆ VƯƠNG RAPHAEL >>"
                : "§3§l<< ĐẠI HIỀN GIẢ (GREAT SAGE) >>";
    }

    public static void sendCornerAlert(ServerPlayer sp, String text, int accentColor, int durationMs, boolean offerWarp) {
        ModMessages.sendToPlayer(new ClientboundSageAlertPacket(getSpeakerLabel(sp), text, accentColor, durationMs, offerWarp), sp);
    }

    public static void pushAlert(ServerPlayer sp, String alertId, String text, String extra, int cooldownTicks) {
        if (sp == null || sp.level().isClientSide()) return;
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        long now = sp.level().getGameTime();
        String key = "SageAlertTick_" + alertId;
        long last = data.getLong(key);
        if (last != 0L && now - last < cooldownTicks) return;
        data.putLong(key, now);
        sendCornerAlert(sp, text + (extra != null && !extra.isEmpty() ? " " + extra : ""), 0xFF38BDF8, 6500, false);
    }

    public static void tickPlayer(ServerPlayer sp) {
        if (sp == null || sp.level().isClientSide()) return;
        ServerLevel sl = sp.serverLevel();
        long now = sl.getGameTime();
        CompoundTag data = EntityDataHelper.getCustomData(sp);

        // 1. Sinh Hang Động Phong Ấn Veldora khi người chơi tới gần trong Overworld (Zero-lag)
        if (sl.dimension() == Level.OVERWORLD && now % 40L == 0L) {
            VeldoraWorldData caveData = VeldoraWorldData.get(sl);
            BlockPos cavePos = caveData.getOrRollCavePos(sl);
            if (!caveData.isCaveGenerated() && sp.distanceToSqr(cavePos.getX(), sp.getY(), cavePos.getZ()) <= 112.0D * 112.0D) {
                caveData.ensureCaveGenerated(sl);
            }

            // Bắt tín hiệu Dao Động Long Chủng định kỳ mỗi 180 giây (nếu Veldora chưa được giải phong ấn)
            if (!caveData.isVeldoraUnsealed()) {
                long lastSignal = data.getLong(TAG_SIGNAL_WARNED);
                if (now - lastSignal >= 3600L || lastSignal == 0L) {
                    data.putLong(TAG_SIGNAL_WARNED, now);
                    double dist = Math.hypot(cavePos.getX() - sp.getX(), cavePos.getZ() - sp.getZ());
                    String dir = getCardinalDirection(sp.getX(), sp.getZ(), cavePos.getX(), cavePos.getZ());
                    sendCornerAlert(sp,
                            "Báo cáo. Bắt được Dao Động Long Chủng bất thường hướng " + dir + " (~" + (int) dist + "m). Có muốn kích hoạt Dịch Chuyển tới Hang Động Phong Ấn?",
                            0xFF00E5FF, 9500, true);
                }
            }

            // 1b. Sinh Cổng Mê Cung 100 Tầng Ramiris khi người chơi tới gần
            com.minhphuc.weapons.world.LabyrinthWorldData labData = com.minhphuc.weapons.world.LabyrinthWorldData.get(sl);
            BlockPos labPos = labData.getOrRollEntrancePos(sl);
            if (!labData.isEntranceBuilt() && sp.distanceToSqr(labPos.getX(), sp.getY(), labPos.getZ()) <= 112.0D * 112.0D) {
                labData.ensureEntranceGenerated(sl);
                pushAlert(sp, "RAMIRIS_LABYRINTH_FOUND",
                        "Đã phát hiện di tích Mê Cung 100 Tầng Của Ramiris ở gần đây! (" + labPos.getX() + ", " + labPos.getZ() + ")",
                        "", 600);
            }
        }

        // 2. Phát hiện Mob gần đó đang cầm vũ khí phẩm cấp HIẾM trở lên
        if (now % 40L == 0L && now - data.getLong(TAG_WEAPON_WARNED) >= 240L) {
            AABB box = sp.getBoundingBox().inflate(22.0D);
            List<Mob> mobs = sl.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && !m.getMainHandItem().isEmpty());
            for (Mob mob : mobs) {
                ItemStack stack = mob.getMainHandItem();
                WeaponGradeHelper.WeaponGrade grade = WeaponGradeHelper.getGrade(stack);
                if (grade.ordinal() >= WeaponGradeHelper.WeaponGrade.RARE.ordinal()) {
                    data.putLong(TAG_WEAPON_WARNED, now);
                    sendCornerAlert(sp,
                            "Cảnh báo! [" + mob.getName().getString() + "] đang cầm vũ khí " + grade.colorPrefix + "[" + grade.title + "]§f: " + stack.getHoverName().getString() + "!",
                            0xFFFF9800, 6500, false);
                    break;
                }
            }
        }

        // 3. Chế độ Tự Động Chiến Đấu (Auto-Battle Mode của Đại Hiền Giả / Raphael)
        if (data.getBoolean(TAG_AUTO_BATTLE) && now % 20L == 0L) {
            if (sp.getHealth() < sp.getMaxHealth() * 0.30F) {
                // Tự dùng Full Potion / Golden Apple / bật Khiên Khẩn Cấp nếu đang tụt máu nặng
                long lastEmergency = data.getLong("SageEmergencyTick");
                if (now - lastEmergency >= 200L) {
                    data.putLong("SageEmergencyTick", now);
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 2));
                    sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 2));
                    consumeHealingItemIfAny(sp);
                    sendCornerAlert(sp, "Chế Độ Tác Chiến Auto-Battle: Sinh lực dưới 30%, đã kích hoạt Kết Giới Khẩn Cấp & Hồi Phục!", 0xFF22D3EE, 4500, false);
                }
            }
        }
    }

    private static void consumeHealingItemIfAny(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack st = sp.getInventory().getItem(i);
            if (st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                st.shrink(1);
                sp.heal(10.0F);
                return;
            }
        }
    }

    public static void handleAction(ServerPlayer sp, int action) {
        ServerLevel sl = sp.serverLevel();
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        switch (action) {
            case ServerboundSageActionPacket.ACTION_WARP_VELDORA -> {
                ServerLevel ow = sl.getServer().overworld();
                VeldoraWorldData caveData = VeldoraWorldData.get(ow);
                caveData.ensureCaveGenerated(ow);
                BlockPos cavePos = caveData.getOrRollCavePos(ow);
                BlockPos standPos = cavePos.offset(0, 1, 9);
                if (sl != ow) {
                    sp.teleportTo(ow, standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, sp.getYRot(), sp.getXRot());
                } else {
                    SpatialTeleportAbility.executeCinematicDemonTeleport(ow, sp, standPos, "Hang Động Phong Ấn Bạo Phong Long Veldora");
                }
                sendCornerAlert(sp, "Đã khởi chạy Không Gian Chuyển Vị tới Hang Động Phong Ấn Bạo Phong Long Veldora!", 0xFF38BDF8, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_TOGGLE_AUTO_BATTLE -> {
                boolean next = !data.getBoolean(TAG_AUTO_BATTLE);
                data.putBoolean(TAG_AUTO_BATTLE, next);
                sendCornerAlert(sp, "Đã " + (next ? "§aBẬT" : "§cTẮT") + " §fchế độ §bỦy Thác Chiến Đấu (Auto-Battle Assist)§f!", 0xFF00E5FF, 4000, false);
            }
            case ServerboundSageActionPacket.ACTION_SCAN_AREA -> {
                List<LivingEntity> scanned = sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(24.0D), e -> e != sp && e.isAlive());
                for (LivingEntity le : scanned) {
                    le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
                }
                sendCornerAlert(sp, "Quét Vạn Vật tầm gần: Đã đánh dấu §e" + scanned.size() + " §fsinh thể trong bán kính 24m!", 0xFFA855F7, 4500, false);
            }
            case ServerboundSageActionPacket.ACTION_DECODE_CURSE -> {
                sp.removeEffect(MobEffects.WITHER);
                sp.removeEffect(MobEffects.POISON);
                sp.removeEffect(MobEffects.WEAKNESS);
                sp.removeEffect(MobEffects.DARKNESS);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
                sendCornerAlert(sp, "Giải Mã & Thanh Tẩy Nguyên Tố: Đã hóa giải toàn bộ hiệu ứng nguyền rủa bất lợi!", 0xFF34D399, 4500, false);
            }
        }
    }

    private static String getCardinalDirection(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        String ns = dz < -10 ? "Bắc" : (dz > 10 ? "Nam" : "");
        String ew = dx > 10 ? "Đông" : (dx < -10 ? "Tây" : "");
        if (!ns.isEmpty() && !ew.isEmpty()) return ns + "-" + ew;
        return !ns.isEmpty() ? ns : (!ew.isEmpty() ? ew : "Ngay dưới chân");
    }
}
