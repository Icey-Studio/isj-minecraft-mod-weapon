package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility;
import com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility;
import com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility;
import com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility;
import com.minhphuc.weapons.content.divine.JacobsLadderAbility;
import com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.darkgathering.CursedIdolEntity;
import com.minhphuc.weapons.entity.tensura.CharybdisEntity;
import com.minhphuc.weapons.entity.tensura.ChronoaEntity;
import com.minhphuc.weapons.entity.tensura.ElementalSpiritEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.OrcDisasterGeldEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.network.ClientboundSageAlertPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundSageActionPacket;
import com.minhphuc.weapons.world.LabyrinthWorldData;
import com.minhphuc.weapons.world.VeldoraWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

/**
 * Hệ thống Trí Tuệ [Đại Hiền Giả] -> [Thông Tuệ Vương Raphael]:
 * Quản lý & phát 50 loại Thông Báo Ngữ Cảnh theo thời gian thực ở góc màn hình.
 */
public final class SageManager {

    private static final String TAG_AUTO_BATTLE = "TensuraRaphaelAutoBattle";
    private static final String TAG_SIGNAL_WARNED = "VeldoraSignalWarnedTick";
    private static final String TAG_WEAPON_WARNED = "SageWeaponWarnTick";

    public static final List<String> NOTIFICATION_CATALOG = List.of(
            "1. Báo cáo: Cảm ứng được Dao Động Không Gian bất thường — Đề xuất Dịch Chuyển",
            "2. Cảnh báo: Mục tiêu sở hữu vũ khí cấp bậc Hiếm trở lên — Đề xuất tước đoạt",
            "3. Khẩn cấp: Tự động kích hoạt Kết Giới Khẩn Cấp & Siêu Hồi Phục khi sinh lực nguy kịch",
            "4. Cảnh báo: Kết Giới Đa Tầng đã bị phá vỡ cấu trúc bảo hộ",
            "5. Cảnh báo: Sinh lực chạm ngưỡng nguy kịch — Đề xuất kích hoạt Siêu Dược Liệu",
            "6. Báo cáo: Dưỡng khí cạn kiệt — Đề xuất kích hoạt Thủy Tinh Hô Hấp",
            "7. Cảnh báo: Nhiệt độ môi trường cực hạn — Đề xuất Đóng Băng Dung Nham",
            "8. Báo cáo: Phát hiện Độc Tố / Héo Mòn — Đề xuất Phân Giải thành Ma Tố",
            "9. Cảnh báo: Thân nhiệt hạ thấp nghiêm trọng dưới ngưỡng an toàn",
            "10. Báo cáo: Tính toàn vẹn cấu trúc Áo Giáp còn dưới 20% — Đề xuất Tái Cấu Trúc Nguyên Tử",
            "11. Báo cáo: Vũ khí sắp đứt gãy — Đề xuất Tái Cấu Trúc Nguyên Tử sửa chữa",
            "12. Báo cáo: Kho lưu trữ ba-lô gần đầy — Đề xuất chuyển vào Dạ Dày Azathoth",
            "13. Báo cáo: Tiến nhập Ác Ma Giới (Nether Dimension)",
            "14. Cảnh báo: Quần Xã Huyết Vực — Lãnh địa của Ác Ma dòng Xích Sắc",
            "15. Cảnh báo: Quần Xã Băng Vực — Lãnh địa của Ác Ma dòng Bạch Sắc",
            "16. Cảnh báo: Quần Xã Tro Tàn & Đồi Xương Trắng",
            "17. Báo cáo: Phát hiện Liên Kết Không Gian dẫn tới Hang Phong Ấn Veldora",
            "18. Báo cáo: Tiến nhập Tầng Đá Sâu Deepslate",
            "19. Cảnh báo: Đã xuống ngưỡng Địa Tầng Nền Đá Cổ Đại (Y < -45)",
            "20. Cảnh báo: Vượt ngưỡng Tầng Không (Y > 180) — Đề phòng Thủy Quái Charybdis",
            "21. Báo cáo: Hiện tượng Giông Bão & Sấm Sét tự nhiên",
            "22. Cảnh báo: Màn đêm buông xuống — Tần suất xuất hiện ma vật gia tăng",
            "23. Báo cáo: Rạng đông xuất hiện — Quang học tự nhiên phục hồi",
            "24. Phân tích: Bạo Phong Long Veldora Tempest (Dạng Rồng / Dạng Người)",
            "25. Báo cáo: Không nhận được sự đồng thuận từ Veldora — Triệu hồi tạm hoãn",
            "26. Cảnh báo: Cảm ứng Chước Nhiệt Long Velgrynd & Hỏa Lực Cực Hạn",
            "27. Cảnh báo: Cảm ứng Bạch Băng Long Velzard & Hàn Khí Tĩnh Chỉ",
            "28. Cảnh báo: Phát hiện Ma Vương Milim Nava — Phẫn Nộ Vương Satanael",
            "29. Phân tích: Ác Ma Thượng Cấp đã tồn tại qua nhiều kỷ nguyên",
            "30. Nhận diện: Ác Ma Ma Giới — Có thể ban nhục thể Ma Thiết để sắc phong",
            "31. Phân tích: Thủy Quái Charybdis sở hữu Hồi Phục Siêu Tốc & Vảy Ma Thần",
            "32. Phân tích: Chúa Tể Orc Geld & Kỹ năng Kẻ Đói Khát",
            "33. Phân tích: Thực thể Chronoa — Dao động thời gian tiệm cận Chân Long",
            "34. Nhận diện: Tinh Linh Nguyên Tố đang trấn giữ khu vực",
            "35. Cảnh báo: Nguồn tà khí ăn mòn cực đại phát ra từ Di Tích Cổ",
            "36. Cảnh báo: Đang bị bao vây bởi đàn quái — Đề xuất Trọng Lực Áp Chế",
            "37. Cảm quan: Phát hiện thực thể Tàng Hình — Đề xuất vạch trần ma trận",
            "38. Cảnh báo: Đang ở trong Kết Giới Đoạt Ma — Kỹ năng Ma Tố bị giới hạn",
            "39. Cảnh báo: Mục tiêu sở hữu năng lực Phong Sát Kỹ Năng",
            "40. Xác nhận: Phòng Ngự Tuyệt Đối của Uriel đang duy trì bảo hộ",
            "41. Xác nhận: Thuật Tiên Tri đang quét quỹ đạo chuyển động thời gian thực",
            "42. Xác nhận: Đã thiết lập Ấn Ký Tọa Độ Không Gian",
            "43. Cảnh báo: Dạ Dày Vô Hạn sắp đạt giới hạn dung lượng lưu trữ",
            "44. Xác nhận: Tiến Hóa thành công Đại Hiền Giả -> Thông Tuệ Vương Raphael",
            "45. Xác nhận: Mục tiêu đã được ban danh xưng và tiến hóa chủng tộc",
            "46. Cảnh báo: Ác Ma tiến hóa mất kiểm soát năng lượng",
            "47. Báo cáo: Hiệu suất bảo hộ cư dân Liên Bang Tempest đạt mức ổn định",
            "48. Xác nhận: Chế Độ Bảo Vệ Địa Hình (Safe Mode) đang hoạt động",
            "49. Cảm ứng Ma Lực: Phát hiện mạch Kim Cương / Ancient Debris kề bên",
            "50. Xác nhận: Giải Mã & Giải Phong Ấn thành công Long Giam Vô Hạn Veldora"
    );

    private SageManager() {}

    public static String getSpeakerLabel(ServerPlayer sp) {
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        return data.getBoolean("TensuraTrueDemonLord")
                ? "§b§l<< THÔNG TUỆ VƯƠNG RAPHAEL >>"
                : "§3§l<< ĐẠI HIỀN GIẢ (GREAT SAGE) >>";
    }

    /**
     * Chuẩn Tensura: Định danh toàn bộ sinh vật trong game dưới dạng « Cá thể [Tên] ».
     */
    public static String formatIndividual(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return "« Cá thể [Vô Danh] »";
        String name = entity.getDisplayName().getString();
        if (name.startsWith("« Cá thể [") && name.endsWith("] »")) return name;
        return "« Cá thể [" + name + "] »";
    }

    public static String formatIndividual(String name) {
        if (name == null || name.isBlank()) return "« Cá thể [Vô Danh] »";
        if (name.startsWith("« Cá thể [") && name.endsWith("] »")) return name;
        if (name.startsWith("Cá thể [") && name.endsWith("]")) return "« " + name + " »";
        return "« Cá thể [" + name + "] »";
    }

    public static void sendCornerAlert(ServerPlayer sp, String text, int accentColor, int durationMs, boolean offerWarp) {
        sendCornerAlert(sp, text, accentColor, durationMs, offerWarp, offerWarp ? ServerboundSageActionPacket.ACTION_WARP_VELDORA : 0);
    }

    public static void sendCornerAlert(ServerPlayer sp, String text, int accentColor, int durationMs, boolean offerWarp, int actionId) {
        ModMessages.sendToPlayer(new ClientboundSageAlertPacket(getSpeakerLabel(sp), text, accentColor, durationMs, offerWarp, actionId), sp);
    }

    public static void pushAlert(ServerPlayer sp, String text, String extra, int cooldownTicks) {
        pushAlert(sp, String.valueOf(text != null ? text.hashCode() : 0), text, extra, cooldownTicks);
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

    /**
     * Báo cáo sự cố kỹ thuật / lỗi thi triển kỹ năng theo thời gian thực qua góc HUD của Raphael.
     */
    public static void reportSystemAnomaly(ServerPlayer sp, String context, Throwable err) {
        String reason = (err != null && err.getMessage() != null && !err.getMessage().isBlank())
                ? err.getMessage()
                : (err != null ? err.getClass().getSimpleName() : "Lỗi không xác định");
        com.minhphuc.weapons.WeaponsMod.LOGGER.error("[Raphael Diagnostics] Lỗi tại '{}'", context, err);
        if (sp == null || sp.level().isClientSide()) return;
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        long now = sp.level().getGameTime();
        String key = "SageAnomalyTick_" + Math.abs(context.hashCode());
        long last = data.getLong(key);
        if (last != 0L && now - last < 60L) return;
        data.putLong(key, now);
        sendCornerAlert(sp,
                "Cảnh báo lỗi hệ thống [" + context + "]: " + reason + " — Hãy khởi động lại game nếu vừa cập nhật mod.",
                0xFFEF4444, 8000, false);
    }

    public static void tickPlayer(ServerPlayer sp) {
        if (sp == null || sp.level().isClientSide()) return;
        ServerLevel sl = sp.serverLevel();
        long now = sl.getGameTime();
        CompoundTag data = EntityDataHelper.getCustomData(sp);

        // CHẾ ĐỘ TỰ ĐỘNG (RAPHAEL AUTO-PILOT TAKEOVER): Nhấn Shift để trả lại quyền điều khiển
        if (data.getBoolean(TAG_AUTO_BATTLE)) {
            if (PrimordialArmorItem.isWearingAnyNoirArmor(sp)) {
                data.putBoolean(TAG_AUTO_BATTLE, false);
                TensuraSkillHelper.syncSkillStateToClient(sp);
                sendCornerAlert(sp, "Đang mặc bộ đồ Noir (Diablo) — Tính năng tự động của Raphael/Đại Hiền Giả bị vô hiệu hóa!", 0xFFA855F7, 4500, false);
            } else if (sp.isShiftKeyDown()) {
                data.putBoolean(TAG_AUTO_BATTLE, false);
                TensuraSkillHelper.syncSkillStateToClient(sp);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.3F, 1.4F);
                sendCornerAlert(sp, "Đã phát hiện [Shift] — Hoàn trả toàn bộ quyền điều khiển về chủ nhân!", 0xFF22C55E, 4500, false);
            } else {
                tickAutonomousRaphael(sl, sp, now, data);
            }
        }

        // N01, N17: Sinh & Phát tín hiệu Hang Veldora / Mê Cung Ramiris (Overworld)
        if (sl.dimension() == Level.OVERWORLD && now % 40L == 0L) {
            VeldoraWorldData caveData = VeldoraWorldData.get(sl);
            BlockPos cavePos = caveData.getOrRollCavePos(sl);
            if (!caveData.isVeldoraUnsealed() && sp.distanceToSqr(cavePos.getX(), sp.getY(), cavePos.getZ()) <= 140.0D * 140.0D) {
                boolean hadPrison = com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.hasVeldoraCavePrison(
                        sl, net.minecraft.world.phys.Vec3.atBottomCenterOf(cavePos.below()));
                caveData.ensureCaveGenerated(sl);
                if (!hadPrison || now % 200L == 0L) {
                    com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.syncPrisonsToPlayer(sp);
                }
            }
            if (!caveData.isVeldoraUnsealed()) {
                long lastSignal = data.getLong(TAG_SIGNAL_WARNED);
                if (now - lastSignal >= 3600L || lastSignal == 0L) {
                    data.putLong(TAG_SIGNAL_WARNED, now);
                    double dist = Math.hypot(cavePos.getX() - sp.getX(), cavePos.getZ() - sp.getZ());
                    String dir = getCardinalDirection(sp.getX(), sp.getZ(), cavePos.getX(), cavePos.getZ());
                    sendCornerAlert(sp,
                            "Báo cáo. phát hiện Dao Động không gian bất thường hướng " + dir + " (~" + (int) dist + "m). Có muốn kích hoạt Dịch Chuyển tới đó không?",
                            0xFF00E5FF, 9500, true);
                }
            }

            LabyrinthWorldData labData = LabyrinthWorldData.get(sl);
            BlockPos labPos = labData.getOrRollEntrancePos(sl);
            if (!labData.isEntranceBuilt() && sp.distanceToSqr(labPos.getX(), sp.getY(), labPos.getZ()) <= 112.0D * 112.0D) {
                labData.ensureEntranceGenerated(sl);
                pushAlert(sp,
                        "Đã phát hiện di tích Mê Cung cổ đại ở gần đây! (" + labPos.getX() + ", " + labPos.getZ() + ")",
                        "", 1200);
            }
        }

        // N02 & N24..N37: Quét Mob, Vũ Khí, Boss & Mối Đe Dọa quanh 24m
        if (now % 40L == 0L) {
            scanWeaponsAndThreats(sl, sp, now, data);
        }

        // N03..N12 & N38..N48: Giám sát Chỉ Số Sống, Trang Bị, Trạng Thái Kỹ Năng mỗi 20 ticks
        if (now % 20L == 0L) {
            monitorVitalsAndGear(sl, sp, now, data);
        }

        // N13..N23 & N49: Giám sát Địa Tầng, Quần Xã Ma Giới, Thời Tiết, Ngày/Đêm, Mỏ Quặng mỗi 80 ticks
        if (now % 80L == 0L) {
            monitorEnvironmentAndOres(sl, sp);
        }
    }

    private static void scanWeaponsAndThreats(ServerLevel sl, ServerPlayer sp, long now, CompoundTag data) {
        AABB box = sp.getBoundingBox().inflate(24.0D);
        List<LivingEntity> entities = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != sp && e.isAlive());
        int hostileCount = 0;
        boolean invisibleFound = false;

        for (LivingEntity le : entities) {
            if (le instanceof Enemy) hostileCount++;
            if (le.isInvisible() && !(le instanceof Player)) invisibleFound = true;

            if (le instanceof Mob mob && !mob.getMainHandItem().isEmpty() && now - data.getLong(TAG_WEAPON_WARNED) >= 240L) {
                ItemStack stack = mob.getMainHandItem();
                WeaponGradeHelper.WeaponGrade grade = WeaponGradeHelper.getGrade(stack);
                if (grade.ordinal() >= WeaponGradeHelper.WeaponGrade.RARE.ordinal()) {
                    data.putLong(TAG_WEAPON_WARNED, now);
                    sendCornerAlert(sp,
                            "« Cảnh báo »: " + formatIndividual(mob) + " đang sở hữu vũ khí " + grade.colorPrefix + "[" + grade.title + "]§f: " + stack.getHoverName().getString() + "! Đề xuất: Dùng [Bạo Thực Vương] tước đoạt vũ khí vào Dạ Dày.",
                            0xFFFF9800, 7500, false, ServerboundSageActionPacket.ACTION_DISARM_TARGET);
                }
            }

            if (le instanceof VeldoraEntity v) {
                pushAlert(sp, "DETECT_VELDORA", "« Báo cáo »: " + formatIndividual(v) + " (" + (v.isHumanForm() ? "Dạng Người" : "Dạng Rồng") + ") đang đồng hành kề bên!", "", 1200);
            } else if (le instanceof VelgryndEntity) {
                pushAlert(sp, "DETECT_VELGRYND", "« Cảnh báo »: Cảm ứng được sự hiện diện của [Chước Nhiệt Long Velgrynd] (74.35M EP) — Nguy cơ thiêu đốt cực hạn!", "", 1200);
            } else if (le instanceof VelzardEntity) {
                pushAlert(sp, "DETECT_VELZARD", "« Cảnh báo »: Cảm ứng được sự hiện diện của [Bạch Băng Long Velzard] (80M EP) — Nguy cơ ngưng trệ dòng chảy ma tố!", "", 1200);
            } else if (le instanceof MilimEntity) {
                pushAlert(sp, "DETECT_MILIM", "« Cảnh báo »: Cảm ứng được sự hiện diện của [Ma Vương Milim Nava] — Tuyệt kỹ Phẫn Nộ Vương Satanael vô hạn uy lực!", "", 1200);
            } else if (le instanceof PrimordialDemonEntity p) {
                pushAlert(sp, "DETECT_PRIMORDIAL", "« Cảnh báo »: Phát hiện thực thể " + formatIndividual(p) + " mang bản chất Thủy Tổ Ác Ma!", "", 1200);
            } else if (le instanceof UnderworldDemonEntity u) {
                pushAlert(sp, "DETECT_UNDERWORLD_DEMON", "« Báo cáo »: Phân tích Ác Ma " + formatIndividual(u) + " [" + u.getRank().title + "]. Có thể ban thể xác Ma Thiết để sắc phong.", "", 1800);
            } else if (le instanceof CharybdisEntity c) {
                pushAlert(sp, "DETECT_CHARYBDIS", "« Cảnh báo »: " + formatIndividual(c) + " sở hữu Hồi Phục Siêu Tốc & Vảy Ma Thần phản ma pháp!", "", 1200);
            } else if (le instanceof OrcDisasterGeldEntity g) {
                pushAlert(sp, "DETECT_GELD", "« Cảnh báo »: Chúa tể Orc " + formatIndividual(g) + " đang kích hoạt năng lực Thôn Phệ Kẻ Đói Khát!", "", 1200);
            } else if (le instanceof ChronoaEntity ch) {
                pushAlert(sp, "DETECT_CHRONOA", "« Báo cáo »: Cảm ứng dao động thời gian bất thường từ " + formatIndividual(ch) + " — Uy lực tiệm cận Chân Long!", "", 1800);
            } else if (le instanceof ElementalSpiritEntity spirit) {
                pushAlert(sp, "DETECT_SPIRIT", "« Báo cáo »: Tinh Linh Nguyên Tố " + formatIndividual(spirit) + " [" + spirit.getElementalType().displayName + "] đang bảo vệ khu vực.", "", 1800);
            } else if (le instanceof CursedIdolEntity ci) {
                pushAlert(sp, "DETECT_CURSED_IDOL", "« Cảnh báo »: Phát hiện " + formatIndividual(ci) + " tỏa ra uế khí ăn mòn sinh mệnh xung quanh!", "", 1200);
            } else if (le instanceof Villager v && v.getCustomName() != null && v.getCustomName().getString().contains("Tempest")) {
                pushAlert(sp, "DETECT_TEMPEST_CITIZEN", "« Báo cáo »: Cư dân " + formatIndividual(v) + " của Liên Bang Tempest đang được bảo hộ ổn định.", "", 2400);
            }
        }

        // Cảnh báo đạn đạo (Projectile Detection)
        List<net.minecraft.world.entity.projectile.Projectile> nearbyProjectiles = sl.getEntitiesOfClass(
                net.minecraft.world.entity.projectile.Projectile.class,
                sp.getBoundingBox().inflate(12.0D),
                p -> p.isAlive() && p.getOwner() != sp
        );
        if (!nearbyProjectiles.isEmpty() && now - data.getLong("SageProjWarnTick") >= 160L) {
            data.putLong("SageProjWarnTick", now);
            sendCornerAlert(sp,
                    "« Cảnh báo »: Phát hiện đạn đạo thù địch đang lao tới! Đề xuất: Kích hoạt [Đảo Chiều Không Gian] phản hồi quỹ đạo về kẻ địch.",
                    0xFF00E5FF, 6500, false, ServerboundSageActionPacket.ACTION_REVERSE_PROJECTILES);
        }

        if (hostileCount >= 5 && now - data.getLong("SageSwarmWarnTick") >= 200L) {
            data.putLong("SageSwarmWarnTick", now);
            sendCornerAlert(sp,
                    "« Cảnh báo »: Đang bị bao vây bởi " + hostileCount + " cá thể thù địch! Đề xuất: Kích hoạt [Trọng Lực Áp Chế] sụp đổ trọng trường giải vây.",
                    0xFFA855F7, 7000, false, ServerboundSageActionPacket.ACTION_SWARM_GRAVITY);
        }
        if (invisibleFound) {
            pushAlert(sp, "INVISIBLE_ENEMY", "« Cảm Quan Ma Lực »: Phát hiện thực thể Tàng Hình trong bán kính xung quanh. Dùng [Shift+V] để vạch trần!", "", 800);
        }
    }

    private static void monitorVitalsAndGear(ServerLevel sl, ServerPlayer sp, long now, CompoundTag data) {
        // N03: Auto-Battle Khẩn Cấp (< 30% HP)
        if (data.getBoolean(TAG_AUTO_BATTLE) && sp.getHealth() < sp.getMaxHealth() * 0.30F) {
            long lastEmergency = data.getLong("SageEmergencyTick");
            if (now - lastEmergency >= 200L) {
                data.putLong("SageEmergencyTick", now);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 2));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 2));
                consumeHealingItemIfAny(sp);
                sendCornerAlert(sp, "« Chế Độ Tác Chiến Auto-Battle »: Sinh lực suy giảm trầm trọng — Tự động kích hoạt Kết Giới Khẩn Cấp & Siêu Hồi Phục!", 0xFF22D3EE, 4500, false);
            }
        } else if (sp.getHealth() < sp.getMaxHealth() * 0.25F && now - data.getLong("SageCritHpTick") >= 200L) {
            data.putLong("SageCritHpTick", now);
            sendCornerAlert(sp,
                    "« NGUY CẤP »: Sinh lực chạm ngưỡng nguy kịch (< 25%)! Đề xuất: Kích hoạt [Siêu Dược Liệu Toàn Phần] hồi 100% HP & Thanh Tẩy.",
                    0xFFEF4444, 7500, false, ServerboundSageActionPacket.ACTION_EMERGENCY_HEAL);
        }

        if (sp.getFoodData().getFoodLevel() <= 5 && now - data.getLong("SageHungerTick") >= 300L) {
            data.putLong("SageHungerTick", now);
            sendCornerAlert(sp, "« Cảnh báo »: Thể lực suy kiệt trầm trọng! Đề xuất: Phục hồi năng lượng từ Dạ Dày Azathoth.", 0xFFF59E0B, 6000, false, ServerboundSageActionPacket.ACTION_EMERGENCY_HEAL);
        }
        if (sp.getAirSupply() < sp.getMaxAirSupply() * 0.35F && now - data.getLong("SageAirTick") >= 160L) {
            data.putLong("SageAirTick", now);
            sendCornerAlert(sp,
                    "« Báo cáo »: Dưỡng khí cạn kiệt! Đề xuất: Kích hoạt [Thủy Tinh Hô Hấp] duy trì hô hấp & tầm nhìn đại dương.",
                    0xFF00E5FF, 6500, false, ServerboundSageActionPacket.ACTION_WATER_BREATHING);
        }
        if ((sp.isOnFire() || sp.isInLava()) && !sp.hasEffect(MobEffects.FIRE_RESISTANCE) && now - data.getLong("SageLavaTick") >= 200L) {
            data.putLong("SageLavaTick", now);
            sendCornerAlert(sp,
                    "« Cảnh báo »: Nhiệt độ môi trường cực hạn! Đề xuất: Kích hoạt [Đóng Băng Dung Nham] tạo vùng đá an toàn & cấp Kháng Lửa.",
                    0xFFF97316, 7000, false, ServerboundSageActionPacket.ACTION_SOLIDIFY_LAVA);
        }
        if ((sp.hasEffect(MobEffects.POISON) || sp.hasEffect(MobEffects.WITHER) || sp.getTicksFrozen() > 60) && now - data.getLong("SageDebuffTick") >= 180L) {
            data.putLong("SageDebuffTick", now);
            sendCornerAlert(sp,
                    "« Báo cáo »: Phát hiện Độc Tố / Héo Mòn / Đóng Băng ăn mòn cơ thể! Đề xuất: Kích hoạt [Phân Giải Độc Tố] nạp +35.000 Ma Tố.",
                    0xFF34D399, 7000, false, ServerboundSageActionPacket.ACTION_DECODE_CURSE);
        }

        // N10, N11: Độ bền Giáp & Vũ khí
        ItemStack main = sp.getMainHandItem();
        boolean mainDamaged = (!main.isEmpty() && main.isDamageableItem() && main.getDamageValue() >= main.getMaxDamage() * 0.80F);
        boolean armorDamaged = false;
        for (ItemStack armor : sp.getArmorSlots()) {
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getDamageValue() >= armor.getMaxDamage() * 0.80F) {
                armorDamaged = true;
                break;
            }
        }
        if ((mainDamaged || armorDamaged) && now - data.getLong("SageDurabilityTick") >= 300L) {
            data.putLong("SageDurabilityTick", now);
            sendCornerAlert(sp,
                    "« Báo cáo »: Trang bị sắp đứt gãy (độ bền < 20%)! Đề xuất: Kích hoạt [Tái Cấu Trúc Nguyên Tử] phục hồi 100% độ bền.",
                    0xFFF59E0B, 7500, false, ServerboundSageActionPacket.ACTION_REPAIR_GEAR);
        }

        // N12: Hành trang gần đầy
        int usedSlots = 0;
        for (int i = 0; i < 36; i++) {
            if (!sp.getInventory().getItem(i).isEmpty()) usedSlots++;
        }
        if (usedSlots >= 33 && now - data.getLong("SageInventoryFullTick") >= 400L) {
            data.putLong("SageInventoryFullTick", now);
            sendCornerAlert(sp,
                    "« Báo cáo »: Ba-lô cá nhân gần đầy (" + usedSlots + "/36 ô)! Đề xuất: Kích hoạt [Quản Gia Hư Không] chuyển toàn bộ khoáng/đất vào Dạ Dày Azathoth.",
                    0xFFA855F7, 7500, false, ServerboundSageActionPacket.ACTION_HOOVER_TRASH);
        }

        // N38..N48
        if (AntiMagicBarrierManager.isInBarrier(sl, sp.position())) {
            pushAlert(sp, "Cảnh báo: Đang ở trong Kết Giới Đoạt Ma — Các kỹ năng phụ thuộc Ma Tố bị giới hạn nghiêm trọng!", "", 600);
        }
        if (SkillNullificationAbility.isEntitySkillSealed(sp)) {
            pushAlert(sp, "Cảnh báo: Toàn bộ kỹ năng đã bị vô hiệu do Phong Sát Kỹ Năng (còn " + SkillNullificationAbility.getRemainingSealSeconds(sp) + "s)!", "", 300);
        }
        if (EvolvedSkillHelper.isUrielDefenseActive(sp)) {
            pushAlert(sp, "Linh Tử Thao Túng có thể xuyên qua Phòng Ngự Tuyệt Đối của Uriel — Chú ý né tránh công kích thánh tích!", "", 2400);
        }
        if (RykuzinSinkaAbility.isActive(sp)) {
            pushAlert(sp, "Thuật Tiên Tri đang quét quỹ đạo tấn công thời gian thực.", "", 2400);
        }
        if (AzathothStomachData.getEntries(sp).size() >= 110) {
            pushAlert(sp, "Dạ Dày Vô Hạn sắp đầy (" + AzathothStomachData.getEntries(sp).size() + "/" + AzathothStomachData.MAX_SLOTS + " ô)! Hãy chuyển hóa bớt thành Ma Tố dự trữ.", "", 1800);
        }
        if (data.getBoolean("TensuraTrueDemonLord") && !data.getBoolean("RaphaelAscendedNotified")) {
            data.putBoolean("RaphaelAscendedNotified", true);
            sendCornerAlert(sp, "« Báo Cáo »: Tiến hóa thành công! Kỹ Năng Độc Nhất [Đại Hiền Giả] đã tiến hóa thành Kỹ Năng Tối Thượng [Thông Tuệ Vương Raphael]!", 0xFF00E5FF, 9000, false);
        }
        if (TensuraSkillHelper.isSafeMode(sp)) {
            pushAlert(sp, "Chế độ Bảo Vệ Địa Hình (Safe Mode) đang BẬT.", "", 3600);
        }
    }

    private static void monitorEnvironmentAndOres(ServerLevel sl, ServerPlayer sp) {
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        long now = sl.getGameTime();

        // Cảnh báo rơi xuống Hư Vô The End hoặc Overworld
        if (sp.getY() < -45.0D && (sl.dimension() == Level.END || sp.getY() < -62.0D)) {
            if (now - data.getLong("SageVoidWarnTick") >= 100L) {
                data.putLong("SageVoidWarnTick", now);
                sendCornerAlert(sp,
                        "« BÁO ĐỘNG HƯ VÔ »: Bản thân đang rơi vào Vực Thẳm Diệt Vong! Đề xuất: Kích hoạt [Hư Không Vị Diện Nhảy Vọt] đưa bản thân về đất liền an toàn ngay lập tức!",
                        0xFF00E5FF, 7500, false, ServerboundSageActionPacket.ACTION_ESCAPE_VOID);
            }
        }

        if (sl.dimension() == Level.NETHER) {
            String biomeId = sl.getBiome(sp.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (biomeId.contains("blood_gorge") || biomeId.contains("crimson")) {
                pushAlert(sp, "« Báo cáo »: Tiến nhập Quần Xã Huyết Vực thuộc Ác Ma Giới.", "", 2400);
            } else if (biomeId.contains("abyssal_frost")) {
                pushAlert(sp, "« Báo cáo »: Tiến nhập Quần Xã Băng Vực Ma Giới.", "", 2400);
            } else if (biomeId.contains("bone_wastes") || biomeId.contains("basalt")) {
                pushAlert(sp, "« Báo cáo »: Tiến nhập Quần Xã Đồi Xương Trắng & Tro Tàn.", "", 2400);
            }
        }

        double y = sp.getY();
        if (y < -45.0D && sl.dimension() == Level.OVERWORLD) {
            pushAlert(sp, "DEEP_BEDROCK_LAYER", "Đã chạm ngưỡng Địa Tầng Nền Đá (Y < -45): Mật độ Kim Cương & Dung Nham dày đặc!", "", 2400);
        } else if (y < 0.0D && sl.dimension() == Level.OVERWORLD) {
            pushAlert(sp, "DEEP_SLATE_LAYER", "Đã vào Tầng Đá Sâu Deepslate (Y < 0): Khuyến nghị dùng Raphael để quét mạch khoáng!", "", 2400);
        } else if (y > 180.0D) {
            pushAlert(sp, "HIGH_ALTITUDE_LAYER", "Đạt độ cao Tầng Không (Y > 180): Cẩn trọng Thủy Quái Charybdis phục kích!", "", 2400);
        }

        if (sl.isThundering()) {
            pushAlert(sp, "Cảnh báo Khí Tượng: Bão sấm dữ dội đang hoành hành — Tần suất sét đánh tự nhiên gia tăng!", "", 2400);
        }
        long dayTime = sl.getDayTime() % 24000L;
        if (dayTime >= 13000L && dayTime <= 13600L) {
            pushAlert(sp, "Màn đêm đã buông xuống — Khuyến nghị kích hoạt Cảm Quan Ma Lực cảnh giới ma vật.", "", 1200);
        } else if (dayTime >= 0L && dayTime <= 500L) {
            pushAlert(sp, "Rạng Đông đã lên — Tầm nhìn & ánh sáng tự nhiên đã phục hồi.", "", 1200);
        }

        // N49: Quét Kim Cương / Debris lộ thiên nếu đang cầm Cuốc
        if (sp.getMainHandItem().getItem() instanceof PickaxeItem && now - data.getLong("SageOreVeinTick") >= 240L) {
            BlockPos c = sp.blockPosition();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        Block b = sl.getBlockState(c.offset(dx, dy, dz)).getBlock();
                        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE || b == Blocks.ANCIENT_DEBRIS) {
                            data.putLong("SageOreVeinTick", now);
                            sendCornerAlert(sp,
                                    "« Cảm ứng Ma Lực »: Phát hiện mạch [" + b.getName().getString() + "] kề bên! Đề xuất: Dùng [Trảm Kích Không Gian] khai thác trực tiếp vào túi đồ.",
                                    0xFF38BDF8, 7000, false, ServerboundSageActionPacket.ACTION_EXTRACT_VEIN);
                            return;
                        }
                    }
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
                BlockPos standPos = cavePos.offset(0, -1, 18);
                if (sl != ow) {
                    sp.teleportTo(ow, standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, 180.0F, -10.0F);
                } else {
                    SpatialTeleportAbility.executeCinematicDemonTeleport(ow, sp, standPos, "Hang Động Phong Nơi Ấn Bạo Phong Long Veldora");
                }
                com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.syncPrisonsToPlayer(sp);
                sendCornerAlert(sp, "« Xác nhận »: Đã Dịch Chuyển Không Gian tới Hang Động Phong Ấn Bạo Phong Long Veldora!", 0xFF38BDF8, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_TOGGLE_AUTO_BATTLE -> {
                if (PrimordialArmorItem.isWearingAnyNoirArmor(sp)) {
                    PrimordialArmorItem.toggleNoirClaws(sp);
                    return;
                }
                boolean next = !data.getBoolean(TAG_AUTO_BATTLE);
                data.putBoolean(TAG_AUTO_BATTLE, next);
                TensuraSkillHelper.syncSkillStateToClient(sp);
                DustParticleOptions auraDust = new DustParticleOptions(new Vector3f(0.0F, 0.9F, 1.0F), 1.6F);
                for (int i = 0; i < 24; i++) {
                    double a = i * (Math.PI * 2.0D / 24.0D);
                    sl.sendParticles(auraDust, sp.getX() + Math.cos(a) * 1.4, sp.getY() + 1.0, sp.getZ() + Math.sin(a) * 1.4, 2, 0.05, 0.25, 0.05, 0.02);
                }
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        next ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 1.4F, next ? 1.5F : 1.2F);
                sendCornerAlert(sp, next
                        ? "§aBẬT §fChế Độ Tự Động — §bThông Tuệ Vương Raphael §fđang tiếp quản điều khiển! (Nhấn §eShift §fđể trở lại)"
                        : "§cTẮT §fChế Độ Tự Động — Đã hoàn trả quyền điều khiển cơ thể cho chủ nhân!", 0xFF00E5FF, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_SCAN_AREA -> {
                List<LivingEntity> scanned = sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(24.0D), e -> e != sp && e.isAlive());
                for (LivingEntity le : scanned) {
                    le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
                }

                Vec3 eye = sp.getEyePosition();
                Vec3 look = sp.getLookAngle();
                Vec3 reach = eye.add(look.scale(32.0D));
                LivingEntity focus = null;
                double bestDist = 32.0D;
                for (LivingEntity cand : sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().expandTowards(look.scale(32.0D)).inflate(2.5D), e -> e != sp && e.isAlive())) {
                    var hit = cand.getBoundingBox().inflate(0.4D).clip(eye, reach);
                    if (hit.isPresent()) {
                        double d = eye.distanceTo(hit.get());
                        if (d < bestDist) {
                            bestDist = d;
                            focus = cand;
                        }
                    }
                }
                if (focus == null && !scanned.isEmpty()) {
                    scanned.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(sp)));
                    if (scanned.get(0).distanceToSqr(sp) <= 16.0D * 16.0D) {
                        focus = scanned.get(0);
                    }
                }

                if (focus != null) {
                    AllOfCreationAbility.appraiseLivingEntity(sp, focus);
                    long focusEp = AllOfCreationAbility.estimateEP(focus);
                    String lineageNote = (focus instanceof com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity uDemon)
                            ? " §7| §dDòng dõi: §e" + uDemon.getDemonType().getTitleVi()
                            : "";
                    sendCornerAlert(sp, "Phân Tích: §e" + focus.getDisplayName().getString()
                            + " §7| §bEP: " + MagiculesManager.formatCompact(focusEp)
                            + lineageNote + " §7(+" + scanned.size() + " mục tiêu gần)", 0xFFA855F7, 5500, false);
                } else {
                    sendCornerAlert(sp, "Quét Vạn Vật tầm gần: Đã đánh dấu §e" + scanned.size() + " §fsinh vật trong bán kính gần!", 0xFFA855F7, 4500, false);
                }
            }
            case ServerboundSageActionPacket.ACTION_DECODE_CURSE -> {
                sp.removeEffect(MobEffects.WITHER);
                sp.removeEffect(MobEffects.POISON);
                sp.removeEffect(MobEffects.WEAKNESS);
                sp.removeEffect(MobEffects.DARKNESS);
                sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                sp.removeEffect(MobEffects.DIG_SLOWDOWN);
                sp.removeEffect(MobEffects.HUNGER);
                sp.clearFire();
                MagiculesManager.addAzathothConversion(sp, 35_000L);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.6F);
                sendCornerAlert(sp, "« Xác nhận »: Đã phân giải toàn bộ độc tố/nguyền rủa & nạp +35.000 Ma Tố dự trữ!", 0xFF34D399, 4500, false);
            }
            case ServerboundSageActionPacket.ACTION_REPAIR_GEAR -> {
                int repairedCount = 0;
                ItemStack mainHand = sp.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.isDamageableItem() && mainHand.isDamaged()) {
                    mainHand.setDamageValue(0);
                    repairedCount++;
                }
                for (ItemStack armor : sp.getArmorSlots()) {
                    if (!armor.isEmpty() && armor.isDamageableItem() && armor.isDamaged()) {
                        armor.setDamageValue(0);
                        repairedCount++;
                    }
                }
                sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.0, sp.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.1F, 1.4F);
                sendCornerAlert(sp, "« Xác nhận »: Kích hoạt [Tái Cấu Trúc Nguyên Tử] thành công! Đã phục hồi 100% độ bền cho " + repairedCount + " trang bị!", 0xFFF59E0B, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_HOOVER_TRASH -> {
                AzathothStomachData.depositAllBackpack(sp);
                List<ItemEntity> drops = sl.getEntitiesOfClass(ItemEntity.class, sp.getBoundingBox().inflate(12.0D), ItemEntity::isAlive);
                int absorbedDrops = 0;
                for (ItemEntity ie : drops) {
                    ItemStack st = ie.getItem().copy();
                    if (AzathothStomachData.storeItemStackIntoStomach(sp, st, false)) {
                        if (st.isEmpty()) ie.discard();
                        else ie.setItem(st);
                        absorbedDrops++;
                    }
                }
                sl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 0.8, sp.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2F, 1.3F);
                sendCornerAlert(sp, "« Xác nhận »: [Quản Gia Hư Không] đã dọn sạch kho đồ & thu nạp " + absorbedDrops + " vật phẩm rơi vào Dạ Dày Azathoth!", 0xFFA855F7, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_EMERGENCY_HEAL -> {
                sp.setHealth(sp.getMaxHealth());
                sp.getFoodData().setFoodLevel(20);
                sp.getFoodData().setSaturation(20.0F);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 3, false, false));
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sp.getX(), sp.getY() + 1.2, sp.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.3F, 1.2F);
                sendCornerAlert(sp, "« Xác nhận »: Đã giải phóng [Siêu Dược Liệu Toàn Phần] — Phục hồi 100% Sinh Lực & Thể Lực!", 0xFF22C55E, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_REVERSE_PROJECTILES -> {
                List<net.minecraft.world.entity.projectile.Projectile> projectiles = sl.getEntitiesOfClass(
                        net.minecraft.world.entity.projectile.Projectile.class,
                        sp.getBoundingBox().inflate(18.0D),
                        p -> p.isAlive() && p.getOwner() != sp
                );
                int count = 0;
                for (var p : projectiles) {
                    Vec3 motion = p.getDeltaMovement();
                    p.setDeltaMovement(motion.scale(-1.6D));
                    p.setOwner(sp);
                    count++;
                }
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, sp.getX(), sp.getY() + 1.0, sp.getZ(), 35, 0.6, 0.6, 0.6, 0.2);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.4F, 1.6F);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
                sendCornerAlert(sp, "« Xác nhận »: [Đảo Chiều Không Gian] thành công! Đã bẻ ngược quỹ đạo của " + count + " đạn đạo về phía kẻ địch!", 0xFF00E5FF, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_OVERDRIVE_SKILL -> {
                data.putBoolean("TensuraMagiculeOverdrive", true);
                TensuraSkillHelper.syncSkillStateToClient(sp);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 3, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2, false, false));
                sl.sendParticles(ParticleTypes.FLASH, sp.getX(), sp.getY() + 1.0, sp.getZ(), 2, 0, 0, 0, 0);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.3F);
                sendCornerAlert(sp, "« Xác nhận »: [Ma Tố Quá Tải - Overdrive] KÍCH HOẠT! Tăng 250% Uy Lực Tuyệt Kỹ trong 15 giây!", 0xFFEF4444, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_RESET_COOLDOWN -> {
                if (!sp.getMainHandItem().isEmpty()) {
                    sp.getCooldowns().removeCooldown(sp.getMainHandItem().getItem());
                }
                if (!sp.getOffhandItem().isEmpty()) {
                    sp.getCooldowns().removeCooldown(sp.getOffhandItem().getItem());
                }
                data.putLong("TensuraSkillCooldownGlobal", 0L);
                data.putLong("SageWeaponWarnTick", 0L);
                sl.sendParticles(ParticleTypes.WITCH, sp.getX(), sp.getY() + 1.0, sp.getZ(), 25, 0.4, 0.4, 0.4, 0.05);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.7F);
                sendCornerAlert(sp, "« Xác nhận »: Raphael đã cưỡng chế lập lại ma trận — Toàn bộ Hồi Chiêu Kỹ Năng đã được RESET về 0s!", 0xFF38BDF8, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_ESCAPE_VOID -> {
                BlockPos targetPos = null;
                BlockPos current = sp.blockPosition();
                for (int r = 1; r <= 80; r += 4) {
                    for (int dx = -r; dx <= r; dx += 8) {
                        for (int dz = -r; dz <= r; dz += 8) {
                            BlockPos check = new BlockPos(current.getX() + dx, 65, current.getZ() + dz);
                            BlockPos surface = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, check);
                            if (surface.getY() > 5 && !sl.getBlockState(surface.below()).isAir()) {
                                targetPos = surface;
                                break;
                            }
                        }
                        if (targetPos != null) break;
                    }
                    if (targetPos != null) break;
                }
                if (targetPos == null) targetPos = new BlockPos(0, 75, 0);

                sp.teleportTo(sl, targetPos.getX() + 0.5D, targetPos.getY() + 0.2D, targetPos.getZ() + 0.5D, Set.of(), sp.getYRot(), 0.0F);
                sp.setDeltaMovement(0, 0, 0);
                sp.fallDistance = 0.0F;
                sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false));
                sl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(), 40, 0.6, 0.8, 0.6, 0.2);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.4F, 1.2F);
                sendCornerAlert(sp, "« Xác nhận »: [Hư Không Vị Diện Nhảy Vọt] thành công! Đã kéo bản thân trở về đất liền an toàn!", 0xFF00E5FF, 6000, false);
            }
            case ServerboundSageActionPacket.ACTION_SOLIDIFY_LAVA -> {
                BlockPos base = sp.blockPosition();
                int solidified = 0;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -2; dy <= 1; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            BlockPos p = base.offset(dx, dy, dz);
                            if (sl.getBlockState(p).is(Blocks.LAVA)) {
                                sl.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
                                solidified++;
                            }
                        }
                    }
                }
                sp.clearFire();
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0, false, false));
                sl.sendParticles(ParticleTypes.SNOWFLAKE, sp.getX(), sp.getY() + 0.5, sp.getZ(), 35, 0.8, 0.4, 0.8, 0.05);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.4F, 1.1F);
                sendCornerAlert(sp, "« Xác nhận »: [Trường Hạ Nhiệt Tuyệt Đối] đã đóng băng " + solidified + " khối Dung Nham & cấp Kháng Lửa 1 phút!", 0xFF38BDF8, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_SWARM_GRAVITY -> {
                List<LivingEntity> enemies = sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(14.0D), e -> e != sp && e instanceof Enemy && e.isAlive());
                for (LivingEntity e : enemies) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 9, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, false));
                    e.hurt(sl.damageSources().magic(), 45.0F);
                    e.setDeltaMovement(0, -1.2D, 0);
                }
                sl.sendParticles(ParticleTypes.SONIC_BOOM, sp.getX(), sp.getY() + 0.5, sp.getZ(), 1, 0, 0, 0, 0);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.3F, 0.8F);
                sendCornerAlert(sp, "« Xác nhận »: [Trọng Lực Áp Chế] sụp đổ thành công! Đã đè sụp và khóa chân " + enemies.size() + " thực thể thù địch!", 0xFFA855F7, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_PACIFY_VELDORA -> {
                VeldoraEntity veldora = VeldoraWorldData.findSingletonVeldora(sl.getServer());
                if (veldora != null && veldora.isAlive()) {
                    veldora.heal(veldora.getMaxHealth() * 0.35F);
                    veldora.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2, false, false));
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, veldora.getX(), veldora.getY() + 1.5, veldora.getZ(), 30, 0.6, 0.6, 0.6, 0.2);
                    sl.playSound(null, veldora.getX(), veldora.getY(), veldora.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.6F, 1.1F);
                    sp.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"KUAHAHA! Đúng là bạn hiền tuyệt nhất, ta tràn đầy sức mạnh rồi!\""), false);
                    sendCornerAlert(sp, "« Xác nhận »: [Hành Lang Linh Hồn] truyền dẫn hoàn tất — Hồi phục 35% Sinh Lực & Cường Hóa Veldora!", 0xFF00E5FF, 5000, false);
                } else {
                    sendCornerAlert(sp, "« Báo cáo »: Hiện không có cá thể Veldora trên chiến trường để liên kết năng lượng.", 0xFFF59E0B, 4000, false);
                }
            }
            case ServerboundSageActionPacket.ACTION_EXTRACT_VEIN -> {
                BlockPos base = sp.blockPosition();
                int mined = 0;
                for (int dx = -5; dx <= 5; dx++) {
                    for (int dy = -4; dy <= 4; dy++) {
                        for (int dz = -5; dz <= 5; dz++) {
                            BlockPos p = base.offset(dx, dy, dz);
                            BlockState state = sl.getBlockState(p);
                            Block b = state.getBlock();
                            if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE || b == Blocks.ANCIENT_DEBRIS || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) {
                                sl.destroyBlock(p, true, sp);
                                mined++;
                            }
                        }
                    }
                }
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.1F, 1.6F);
                sendCornerAlert(sp, "« Xác nhận »: [Trảm Kích Không Gian] khai thác thành công " + mined + " khối khoáng sản quý thẳng vào túi đồ!", 0xFF38BDF8, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_WATER_BREATHING -> {
                sp.setAirSupply(sp.getMaxAirSupply());
                sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 12000, 0, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 12000, 0, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 6000, 0, false, false));
                sl.sendParticles(ParticleTypes.BUBBLE_POP, sp.getX(), sp.getY() + 1.0, sp.getZ(), 25, 0.3, 0.3, 0.3, 0.05);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.3F);
                sendCornerAlert(sp, "« Xác nhận »: [Thủy Tinh Hô Hấp] đã kích hoạt! Duy trì dưỡng khí vô hạn & tầm nhìn đại dương!", 0xFF00E5FF, 5000, false);
            }
            case ServerboundSageActionPacket.ACTION_DISARM_TARGET -> {
                Vec3 eye = sp.getEyePosition();
                Vec3 look = sp.getLookAngle();
                Vec3 reach = eye.add(look.scale(14.0D));
                LivingEntity victim = null;
                for (LivingEntity cand : sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().expandTowards(look.scale(14.0D)).inflate(2.0D), e -> e != sp && e.isAlive())) {
                    if (cand instanceof Mob m && !m.getMainHandItem().isEmpty()) {
                        victim = m;
                        break;
                    }
                }
                if (victim instanceof Mob m) {
                    ItemStack weapon = m.getMainHandItem().copy();
                    m.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    AzathothStomachData.storeItemStackIntoStomach(sp, weapon, false);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, m.getX(), m.getY() + 1.0, m.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                    sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.3F, 1.4F);
                    sendCornerAlert(sp, "« Xác nhận »: [Hàm Nuốt Bạo Thực Vương] đã tước đoạt thành công [" + weapon.getHoverName().getString() + "] vào Dạ Dày Azathoth!", 0xFFA855F7, 5000, false);
                } else {
                    sendCornerAlert(sp, "« Báo cáo »: Mục tiêu đã thoát khỏi tầm tước vũ khí của Bạo Thực Vương.", 0xFFF59E0B, 4000, false);
                }
            }
        }
    }

    private static void tickAutonomousRaphael(ServerLevel sl, ServerPlayer sp, long now, CompoundTag data) {
        if (sp.isSpectator()) return;

        // Vòng sáng ma thuật bao quanh khi Raphael đang điều khiển cơ thể
        if (now % 4L == 0L) {
            DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0.0F, 0.9F, 1.0F), 1.1F);
            double a = (now * 0.35D) % (Math.PI * 2.0D);
            sl.sendParticles(cyan, sp.getX() + Math.cos(a) * 0.9D, sp.getY() + 0.15D, sp.getZ() + Math.sin(a) * 0.9D, 1, 0, 0, 0, 0);
        }

        // Duy trì phòng hộ khẩn cấp, hồi phục & thanh tẩy trạng thái bất lợi (nhanh & thông minh)
        if (now % 15L == 0L) {
            sp.heal(2.5F);
            if (sp.getFoodData().getFoodLevel() < 18) sp.getFoodData().eat(6, 0.9F);
            if (sp.isOnFire()) sp.clearFire();
            sp.removeEffect(MobEffects.POISON);
            sp.removeEffect(MobEffects.WITHER);
            sp.removeEffect(MobEffects.DARKNESS);
            sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            sp.removeEffect(MobEffects.WEAKNESS);
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1, true, false));
        }
        // Khẩn cấp bật Đa Trùng Kết Giới / Hồi Máu ngay khi HP xuống dưới 45%
        if (sp.getHealth() < sp.getMaxHealth() * 0.45F && now % 20L == 0L) {
            sp.heal(8.0F);
            if (!EvolvedSkillHelper.isUrielDefenseActive(sp)) {
                MultilayerBarrierAbility.cast(sl, sp);
            }
            RaphaelSkillRegistry.execute(sl, sp, 10); // Siêu Hồi Phục Tế Bào
        } else if (now % 60L == 0L && !EvolvedSkillHelper.isUrielDefenseActive(sp)) {
            MultilayerBarrierAbility.cast(sl, sp);
        }

        // Tìm mục tiêu thù địch trong 48m & chấm điểm ưu tiên Boss / mục tiêu áp sát
        List<LivingEntity> hostiles = sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(48.0D),
                e -> e != sp && e.isAlive()
                        && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == sp))
                        && !(e instanceof Villager)
                        && !(e instanceof TamableAnimal ta && ta.isTame() && sp.getUUID().equals(ta.getOwnerUUID()))
                        && !(e instanceof VelgryndEntity vg && vg.isAllied() && sp.getUUID().equals(vg.getParsedOwnerUUID()))
                        && !(e instanceof PrimordialDemonEntity pd && sp.getUUID().equals(pd.getOwnerUUID())));

        if (hostiles.isEmpty()) {
            if (now % 30L == 0L) {
                sp.displayClientMessage(Component.literal("§b§l[RAPHAEL AUTO-PILOT] §7Đang kiểm tra không gian... §8(Nhấn §e[X] §8hoặc §e[SHIFT] §8để trả quyền)"), true);
            }
            return;
        }

        hostiles.sort((a, b) -> {
            double scoreA = a.distanceToSqr(sp) - (isHighPriorityBoss(a) ? 400.0D : 0.0D);
            double scoreB = b.distanceToSqr(sp) - (isHighPriorityBoss(b) ? 400.0D : 0.0D);
            return Double.compare(scoreA, scoreB);
        });
        LivingEntity target = hostiles.get(0);
        double dist = Math.sqrt(sp.distanceToSqr(target));

        // 1) Điều khiển di chuyển cơ thể tốc độ cao: bay áp sát trên cao (đánh Charybdis vây lưng) hoặc quần đảo né đòn
        sp.fallDistance = 0.0F;
        double desiredY = (target instanceof CharybdisEntity)
                ? (target.getY() + target.getBbHeight() + 1.5D)
                : target.getY();
        Vec3 diff = target.position().subtract(sp.position());
        Vec3 horiz = new Vec3(diff.x, 0.0D, diff.z);
        double hLen = horiz.length();
        Vec3 dirH = hLen > 0.01D ? horiz.scale(1.0D / hLen) : new Vec3(0, 0, 1);

        double vy = sp.getDeltaMovement().y;
        if (sp.onGround() && sp.horizontalCollision) {
            vy = 0.48D;
        } else if (desiredY > sp.getY() + 1.8D) {
            vy = Math.min(0.45D, (desiredY - sp.getY()) * 0.14D);
        } else if (desiredY < sp.getY() - 2.5D && !sp.onGround()) {
            vy = -0.25D;
        }

        Vec3 move;
        if (sp.getHealth() < sp.getMaxHealth() * 0.35F && dist < 8.0D) {
            // Né lùi khẩn cấp khi máu thấp & địch quá gần
            Vec3 tangent = new Vec3(-dirH.z, 0.0D, dirH.x).scale((now % 40L < 20L) ? 0.42D : -0.42D);
            move = new Vec3(-dirH.x * 0.38D + tangent.x, Math.max(vy, 0.18D), -dirH.z * 0.38D + tangent.z);
        } else if (dist > 8.0D) {
            double speed = Math.min(0.58D, 0.34D + dist * 0.014D);
            move = new Vec3(dirH.x * speed, vy, dirH.z * speed);
        } else if (dist < 3.4D) {
            Vec3 tangent = new Vec3(-dirH.z, 0.0D, dirH.x).scale((now % 60L < 30L) ? 0.36D : -0.36D);
            move = new Vec3(tangent.x - dirH.x * 0.10D, vy, tangent.z - dirH.z * 0.10D);
        } else {
            Vec3 tangent = new Vec3(-dirH.z, 0.0D, dirH.x).scale(0.24D);
            move = new Vec3(dirH.x * 0.22D + tangent.x, vy, dirH.z * 0.22D + tangent.z);
        }
        sp.setDeltaMovement(move);
        sp.hurtMarked = true;

        // Căn hướng nhìn chính xác về phía mục tiêu để tung kỹ năng
        Vec3 aimPoint = (target instanceof CharybdisEntity)
                ? target.position().add(0.0D, target.getBbHeight() * 0.85D, 0.0D)
                : target.getEyePosition();
        Vec3 eyeAim = aimPoint.subtract(sp.getEyePosition());
        double hDist = Math.max(0.01D, Math.hypot(eyeAim.x, eyeAim.z));
        float yaw = (float) (Math.atan2(-eyeAim.x, eyeAim.z) * (180.0D / Math.PI));
        float pitch = (float) (-Math.atan2(eyeAim.y, hDist) * (180.0D / Math.PI));
        sp.setYRot(yaw);
        sp.setXRot(pitch);
        sp.setYHeadRot(yaw);

        // Cận chiến liên hoàn tốc độ cao (mỗi 6 ticks)
        if (dist <= 4.8D && now % 6L == 0L) {
            sp.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            sp.attack(target);
        }

        // 2) Tác chiến thông minh không hồi chiêu (tốc độ tung chiêu gấp đôi: mỗi 12 ticks = 0.6s)
        long lastCast = data.getLong("RaphaelAutoLastCast");
        if (now - lastCast >= 12L) {
            data.putLong("RaphaelAutoLastCast", now);
            int rot = data.getInt("RaphaelAutoSkillRot");
            data.putInt("RaphaelAutoSkillRot", (rot + 1) % 14);

            if (dist > 18.0D && rot % 2 == 0) {
                SpatialTeleportAbility.castCombatBlink(sl, sp);
            } else {
                switch (rot) {
                    case 0 -> SpatialTeleportAbility.castCombatBlink(sl, sp);
                    case 1 -> SanctuaryDisintegrationAbility.cast(sl, sp, ItemStack.EMPTY);
                    case 2 -> JacobsLadderAbility.cast(sl, sp, ItemStack.EMPTY);
                    case 3 -> HellflareAbility.cast(sl, sp);
                    case 4 -> DarkFlameShotAbility.cast(sl, sp);
                    case 5 -> DragonNovaAbility.cast(sl, sp);
                    case 6 -> TaisuiExtinctionStarsAbility.cast(sl, sp);
                    case 7 -> {
                        if (dist <= 11.0D) ReverseVelocityIaijutsuAbility.castReverseVelocityIai(sl, sp);
                        else ZoneTrackAbility.cast(sl, sp);
                    }
                    case 8 -> {
                        if (dist <= 11.0D) TengaiKyuseiSwordAbility.castReverseTimeSlash(sl, sp);
                        else HorizontalHolyBeamAbility.cast(sl, sp);
                    }
                    case 9 -> {
                        if (hostiles.size() >= 3) SupernovaAbility.cast(sl, sp);
                        else RaphaelSkillRegistry.execute(sl, sp, 48);
                    }
                    case 10 -> {
                        if (target.getMaxHealth() >= 250.0F) InfiniteDragonPrisonAbility.cast(sl, sp, 1);
                        else RaphaelSkillRegistry.execute(sl, sp, 22);
                    }
                    case 11 -> {
                        if (target.getMaxHealth() >= 300.0F && !SkillNullificationAbility.isEntitySkillSealed(target)) {
                            SkillNullificationAbility.cast(sl, sp);
                        } else {
                            RaphaelSkillRegistry.execute(sl, sp, 24);
                        }
                    }
                    case 12 -> NihilityCollapseAbility.cast(sl, sp);
                    case 13 -> BeelzebuthAbility.executeBeelzebuth(sl, sp);
                }
            }
            sp.displayClientMessage(Component.literal("§b§l[RAPHAEL AUTO] §fĐang tác chiến: §e" + target.getName().getString() + " §8| §c[X] / [SHIFT] §8trả quyền"), true);
        }
    }

    private static boolean isHighPriorityBoss(LivingEntity e) {
        return e instanceof CharybdisEntity
                || e instanceof MilimEntity
                || e instanceof VelgryndEntity
                || e instanceof VelzardEntity
                || e instanceof OrcDisasterGeldEntity
                || e instanceof ChronoaEntity
                || e.getMaxHealth() >= 200.0F;
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
