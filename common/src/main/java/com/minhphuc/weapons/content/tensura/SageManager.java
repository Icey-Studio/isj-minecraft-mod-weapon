package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Hệ thống Trí Tuệ [Đại Hiền Giả] -> [Trí Tuệ Vương Raphael]:
 * Quản lý & phát 50 loại Thông Báo Ngữ Cảnh theo thời gian thực ở góc màn hình.
 */
public final class SageManager {

    private static final String TAG_AUTO_BATTLE = "TensuraRaphaelAutoBattle";
    private static final String TAG_SIGNAL_WARNED = "VeldoraSignalWarnedTick";
    private static final String TAG_WEAPON_WARNED = "SageWeaponWarnTick";

    public static final List<String> NOTIFICATION_CATALOG = List.of(
            "1. Bắt tín hiệu Dao Động Long Chủng Veldora & đề nghị Dịch Chuyển",
            "2. Cảnh báo Mob cầm vũ khí phẩm cấp Hiếm (Rare) trở lên",
            "3. Auto-Battle khẩn cấp: Tự bật Kết Giới & Hồi Phục khi < 30% HP",
            "4. Báo động Sinh Lực Nguy Kịch (< 20% HP)",
            "5. Cảnh báo Suy Kiệt Dinh Dưỡng (Độ no < 6)",
            "6. Cảnh báo Thiếu Dưỡng Khí dưới nước (< 35% Oxy)",
            "7. Cảnh báo Bỏng Nhiệt / Đứng trong Lửa & Dung Nham",
            "8. Cảnh báo Nhiễm Độc (Poison) hoặc Khô Héo (Wither)",
            "9. Cảnh báo Hạ Thân Nhiệt & Đóng Băng Thân Thể",
            "10. Cảnh báo Độ Bền Áo Giáp sắp hỏng (< 15%)",
            "11. Cảnh báo Độ Bền Vũ Khí cầm tay sắp gãy (< 15%)",
            "12. Báo Hành Trang Gần Đầy (>= 34/36 ô) — Đề nghị Dạ Dày Azathoth",
            "13. Thông báo Tiến Nhập Ma Giới (Underworld / Nether)",
            "14. Cảnh báo Quần Xã Huyết Vực (Crimson / Blood Gorge)",
            "15. Cảnh báo Quần Xã Băng Vực (Abyssal Frost)",
            "16. Cảnh báo Quần Xã Tro Tàn & Đồi Xương Trắng (Bone Wastes)",
            "17. Phát hiện Cổng Di Tích Mê Cung 100 Tầng Của Ramiris",
            "18. Báo cáo Tiến Nhập Địa Tầng Đá Sâu (Y < 0)",
            "19. Cảnh báo Ngưỡng Địa Tầng Nền Đá Cổ Đại (Y < -45)",
            "20. Cảnh báo Vượt Mực Tầng Không Cao (Y > 180)",
            "21. Thông báo Giông Bão & Sấm Sét Khí Quyển",
            "22. Thông báo Màn Đêm Buông Xuống & Nguy Cơ Ác Ma",
            "23. Thông báo Rạng Đông Mới & Tái Tạo Ánh Sáng",
            "24. Phân tích Bạo Phong Long Veldora (Dạng Rồng / Dạng Người)",
            "25. Báo cáo Tỷ Lệ 30% Veldora Đang Bận Khi Triệu Hồi",
            "26. Phân tích Chước Nhiệt Long Velgrynd & Đòn Xuyên Giáp Thứ 3",
            "27. Phân tích Bạch Băng Long Velzard & Lãnh Khí Vĩnh Cửu",
            "28. Cảnh báo Ma Vương Cổ Đại Milim Nava & Satanael",
            "29. Phân tích Ác Ma Thủy Tổ (Primordial) & Đề nghị Phong Sát",
            "30. Nhận diện Ác Ma Ma Giới (Hấp Thụ / Ban Xác bằng Xương)",
            "31. Phân tích Thủy Quái Bay Charybdis & Khiên Vảy Chống Phép",
            "32. Phân tích Orc Disaster Geld & Hỗn Độn Thực Giả",
            "33. Phân tích Anh Hùng Chronoa & Rủi Ro Tha Hóa",
            "34. Nhận diện Tinh Linh Nguyên Tố Hộ Vệ Khu Vực",
            "35. Cảnh báo Di Tích / Thực Thể Bị Nguyền Cấp S (Cursed Idol)",
            "36. Cảnh báo Vòng Vây Đông Đảo (>= 5 quái vật gần kề)",
            "37. Phát hiện Mục Tiêu Tàng Hình trong bán kính 24m",
            "38. Cảnh báo Đang Đứng Trong Kháng Ma Kết Giới (Triệt Ma Tố)",
            "39. Cảnh báo Đang Bị Dính Phong Sát Kỹ Năng",
            "40. Xác nhận Phòng Ngự Tuyệt Đối Uriel Đang Kích Hoạt",
            "41. Xác nhận Tiên Tri Rykuzin Sinka Đang Bật",
            "42. Xác nhận Đánh Dấu Ấn Ký Không Gian (Vĩnh Viễn / Chiến Thuật)",
            "43. Cảnh báo Dạ Dày Vô Hạn Azathoth Sắp Đầy (>= 20/24 ô)",
            "44. Chúc mừng Tiến Hóa Đại Hiền Giả -> Trí Tuệ Vương Raphael",
            "45. Xác nhận Ban Danh Xưng Nâng Cấp Sinh Vật",
            "46. Cảnh báo Đỏ: Ác Ma Tiến Hóa Ma Vương Mất Kiểm Soát (70% Phản Chủ)",
            "47. Báo cáo Hiệu Suất Hộ Vệ Dân Làng Liên Bang Tempest",
            "48. Thông báo Chế Độ Bảo Vệ Địa Hình (Safe Mode) Đang Hoạt Động",
            "49. Phát hiện Mỏ Kim Cương / Ancient Debris Lộ Thiên (Khi cầm Cuốc)",
            "50. Xác nhận Giải Phong Ấn Thành Công Bạo Phong Long Veldora"
    );

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
                            "Báo cáo. Bắt được Dao Động Long Chủng bất thường hướng " + dir + " (~" + (int) dist + "m). Có muốn kích hoạt Dịch Chuyển tới Hang Động Phong Ấn?",
                            0xFF00E5FF, 9500, true);
                }
            }

            LabyrinthWorldData labData = LabyrinthWorldData.get(sl);
            BlockPos labPos = labData.getOrRollEntrancePos(sl);
            if (!labData.isEntranceBuilt() && sp.distanceToSqr(labPos.getX(), sp.getY(), labPos.getZ()) <= 112.0D * 112.0D) {
                labData.ensureEntranceGenerated(sl);
                pushAlert(sp, "RAMIRIS_LABYRINTH_FOUND",
                        "Đã phát hiện di tích Mê Cung 100 Tầng Của Ramiris ở gần đây! (" + labPos.getX() + ", " + labPos.getZ() + ")",
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
                            "Cảnh báo! [" + mob.getName().getString() + "] đang cầm vũ khí " + grade.colorPrefix + "[" + grade.title + "]§f: " + stack.getHoverName().getString() + "!",
                            0xFFFF9800, 6500, false);
                }
            }

            if (le instanceof VeldoraEntity v) {
                pushAlert(sp, "DETECT_VELDORA", "Phân tích: Bạo Phong Long Veldora (" + (v.isHumanForm() ? "Dạng Người" : "Chân Thân Rồng x30") + ") đang hiện diện gần đây!", "", 1200);
            } else if (le instanceof VelgryndEntity) {
                pushAlert(sp, "DETECT_VELGRYND", "Cảnh báo Long Chủng! Chước Nhiệt Long Velgrynd (74.35M EP) — chú ý đòn thứ 3 xuyên Giáp Thần!", "", 1200);
            } else if (le instanceof VelzardEntity) {
                pushAlert(sp, "DETECT_VELZARD", "Báo động Đệ Nhị Long! Bạch Băng Long Velzard — giữ di chuyển liên tục để không bị đóng băng vĩnh cửu!", "", 1200);
            } else if (le instanceof MilimEntity) {
                pushAlert(sp, "DETECT_MILIM", "Cảnh báo Ma Vương Cổ Đại! Milim Nava (Satanael) — sẵn sàng Phong Sát Kỹ Năng khi tụ Long Tinh Bộc Viêm Bá!", "", 1200);
            } else if (le instanceof PrimordialDemonEntity) {
                pushAlert(sp, "DETECT_PRIMORDIAL", "Phát hiện Ác Ma Thủy Tổ (Primordial)! Không thể so sánh với ác ma thường — hãy cẩn trọng Lãnh Địa!", "", 1200);
            } else if (le instanceof UnderworldDemonEntity u) {
                pushAlert(sp, "DETECT_UNDERWORLD_DEMON", "Phân tích Ác Ma Ma Giới [" + u.getRank().title + "]. Cầm Xương để ban thể xác hoặc Điện Thoại Ma Thép để soi nguồn gốc!", "", 1800);
            } else if (le instanceof CharybdisEntity) {
                pushAlert(sp, "DETECT_CHARYBDIS", "Phân tích Charybdis: Đang kích hoạt Khiên Vảy Chống Phép — phá giáp trước khi tung đại chiêu!", "", 1200);
            } else if (le instanceof OrcDisasterGeldEntity) {
                pushAlert(sp, "DETECT_GELD", "Cảnh báo Tai Ương Orc Disaster Geld! Đừng để Binh Đoàn Đói Khát kéo dài giao tranh!", "", 1200);
            } else if (le instanceof ChronoaEntity) {
                pushAlert(sp, "DETECT_CHRONOA", "Phân tích Anh Hùng Chronoa: Cẩn thận nguy cơ Tha Hóa ban đêm với Giáp Thần Linh!", "", 1800);
            } else if (le instanceof ElementalSpiritEntity spirit) {
                pushAlert(sp, "DETECT_SPIRIT", "Nhận diện Tinh Linh Nguyên Tố [" + spirit.getElementalType().displayName + "] đang hộ vệ khu vực.", "", 1800);
            } else if (le instanceof CursedIdolEntity) {
                pushAlert(sp, "DETECT_CURSED_IDOL", "Phát hiện Thực Thể Bị Nguyền Cấp S! Nguyền lực Dark Gathering đang ăn mòn xung quanh!", "", 1200);
            } else if (le instanceof Villager && le.getCustomName() != null && le.getCustomName().getString().contains("Tempest")) {
                pushAlert(sp, "DETECT_TEMPEST_CITIZEN", "Đang ở gần cư dân Liên Bang Tempest — hiệu suất phòng hộ khu vực ổn định!", "", 2400);
            }
        }

        if (hostileCount >= 5) {
            pushAlert(sp, "SWARM_DANGER", "Cảnh báo! Phát hiện vòng vây " + hostileCount + " sinh vật thù địch quanh bạn — đề nghị bật Đa Trùng Kết Giới!", "", 600);
        }
        if (invisibleFound) {
            pushAlert(sp, "INVISIBLE_ENEMY", "Cảm Quan Ma Lực phát hiện thực thể Tàng Hình trong 24m! Dùng [Quét Vạn Vật] hoặc Raphael #7 để vạch trần!", "", 800);
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
                sendCornerAlert(sp, "Chế Độ Tác Chiến Auto-Battle: Sinh lực dưới 30%, đã kích hoạt Kết Giới Khẩn Cấp & Hồi Phục!", 0xFF22D3EE, 4500, false);
            }
        } else if (sp.getHealth() < sp.getMaxHealth() * 0.20F) {
            pushAlert(sp, "CRITICAL_HP", "Báo động Sinh Lực Nguy Kịch (< 20% HP)! Hãy dùng Skill #11 Raphael hoặc Dược Tề!", "", 300);
        }

        if (sp.getFoodData().getFoodLevel() <= 5) {
            pushAlert(sp, "LOW_HUNGER", "Cảnh báo Suy Kiệt Dinh Dưỡng: Thanh đói xuống thấp — kích hoạt Raphael #14 để bổ sung!", "", 800);
        }
        if (sp.getAirSupply() < sp.getMaxAirSupply() * 0.35F) {
            pushAlert(sp, "LOW_OXYGEN", "Cảnh báo Thiếu Dưỡng Khí! Dùng Raphael #15 (Thở Nước) trước khi ngạt!", "", 400);
        }
        if (sp.isOnFire() && !sp.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            pushAlert(sp, "ON_FIRE", "Cảnh báo Bỏng Nhiệt! Sử dụng Raphael #19 hoặc #45 để thanh tẩy lửa tức thì!", "", 500);
        }
        if (sp.hasEffect(MobEffects.POISON) || sp.hasEffect(MobEffects.WITHER)) {
            pushAlert(sp, "TOXIC_DEBUFF", "Phát hiện Độc Tố / Héo Mòn (Poison/Wither)! Đề nghị chạy Giải Mã Nguyền Rủa!", "", 450);
        }
        if (sp.getTicksFrozen() > 60) {
            pushAlert(sp, "HYPOTHERMIA", "Cảnh báo Hạ Thân Nhiệt! Cơ thể đang đóng băng, tránh đứng yên!", "", 400);
        }

        // N10, N11: Độ bền Giáp & Vũ khí
        ItemStack main = sp.getMainHandItem();
        if (!main.isEmpty() && main.isDamageableItem() && main.getDamageValue() >= main.getMaxDamage() * 0.85F) {
            pushAlert(sp, "WEAPON_LOW_DURA", "Vũ khí [" + main.getHoverName().getString() + "] sắp hỏng! Dùng Raphael #31 Tu Sửa Tuyệt Đối!", "", 900);
        }
        for (ItemStack armor : sp.getArmorSlots()) {
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getDamageValue() >= armor.getMaxDamage() * 0.85F) {
                pushAlert(sp, "ARMOR_LOW_DURA", "Áo Giáp [" + armor.getHoverName().getString() + "] còn dưới 15% độ bền!", "", 900);
                break;
            }
        }

        // N12: Hành trang gần đầy
        int usedSlots = 0;
        for (int i = 0; i < 36; i++) {
            if (!sp.getInventory().getItem(i).isEmpty()) usedSlots++;
        }
        if (usedSlots >= 34) {
            pushAlert(sp, "INVENTORY_NEAR_FULL", "Hành trang gần đầy (" + usedSlots + "/36 ô)! Đề nghị chuyển bớt vào Dạ Dày Vô Hạn Azathoth.", "", 1800);
        }

        // N38..N48
        if (AntiMagicBarrierManager.isInBarrier(sl, sp.position())) {
            pushAlert(sp, "IN_ANTI_MAGIC", "Cảnh báo: Bạn đang ở trong Kháng Ma Kết Giới! Chỉ dùng được Linh Tử, Vật Lý, Tiên Tri hoặc Phong Sát!", "", 600);
        }
        if (SkillNullificationAbility.isEntitySkillSealed(sp)) {
            pushAlert(sp, "SKILL_SEALED_SELF", "Cảnh báo! Toàn bộ kỹ năng của bạn đang bị Phong Sát (còn " + SkillNullificationAbility.getRemainingSealSeconds(sp) + "s)!", "", 300);
        }
        if (EvolvedSkillHelper.isUrielDefenseActive(sp)) {
            pushAlert(sp, "URIEL_ACTIVE_NOTE", "Phòng Ngự Tuyệt Đối Uriel đang hoạt động — lưu ý Bạch Băng Long Velzard & Linh Tử có thể xuyên giáp!", "", 2400);
        }
        if (RykuzinSinkaAbility.isActive(sp)) {
            pushAlert(sp, "RYKUZIN_ACTIVE_NOTE", "Tiên Tri Rykuzin Sinka đang quét quỹ đạo tấn công xung quanh bạn.", "", 2400);
        }
        if (AzathothStomachData.getEntries(sp).size() >= 20) {
            pushAlert(sp, "STOMACH_ALMOST_FULL", "Dạ Dày Vô Hạn Azathoth sắp đầy (" + AzathothStomachData.getEntries(sp).size() + "/24 ô)! Hãy chuyển hóa bớt thành Ma Tố.", "", 1800);
        }
        if (data.getBoolean("TensuraTrueDemonLord") && !data.getBoolean("RaphaelAscendedNotified")) {
            data.putBoolean("RaphaelAscendedNotified", true);
            sendCornerAlert(sp, "Thông báo Thăng Hoa! Unique Skill [Đại Hiền Giả] đã tiến hóa thành Ultimate Skill [Trí Tuệ Vương Raphael]!", 0xFF00E5FF, 8000, false);
        }
        if (TensuraSkillHelper.isSafeMode(sp)) {
            pushAlert(sp, "SAFE_MODE_REMINDER", "Safe Mode (Chống Phá Địa Hình) đang BẬT — các đại chiêu sẽ không hủy hoại công trình của bạn.", "", 3600);
        }
    }

    private static void monitorEnvironmentAndOres(ServerLevel sl, ServerPlayer sp) {
        if (sl.dimension() == Level.NETHER) {
            pushAlert(sp, "ENTER_UNDERWORLD", "Đã tiến nhập Ma Giới (Underworld)! Ác Ma dòng dõi Thủy Tổ đang ẩn náu ở đây.", "", 2400);
            String biomeId = sl.getBiome(sp.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (biomeId.contains("blood_gorge") || biomeId.contains("crimson")) {
                pushAlert(sp, "BIOME_BLOOD_GORGE", "Quần xã Huyết Vực Ma Giới: Dòng dõi Rouge (Đỏ) cực kỳ hiếu chiến tại đây!", "", 2400);
            } else if (biomeId.contains("abyssal_frost")) {
                pushAlert(sp, "BIOME_ABYSSAL_FROST", "Quần xã Băng Vực Ma Giới: Cẩn thận Ác Ma Băng Tuyết dòng Bleu/Blanc!", "", 2400);
            } else if (biomeId.contains("bone_wastes") || biomeId.contains("basalt")) {
                pushAlert(sp, "BIOME_BONE_WASTES", "Quần xã Đồi Xương Trắng & Tro Tàn: Ác Ma Dị Thú & Phi Hành đang tuần tra!", "", 2400);
            }
        }

        double y = sp.getY();
        if (y < -45.0D) {
            pushAlert(sp, "DEEP_BEDROCK_LAYER", "Đã xuống ngưỡng Địa Tầng Nền Đá (Y < -45): Mật độ Kim Cương & Dung Nham dày đặc!", "", 2400);
        } else if (y < 0.0D) {
            pushAlert(sp, "DEEP_SLATE_LAYER", "Đã vào Tầng Đá Sâu Deepslate (Y < 0): Dùng Raphael #5 để quét mạch khoáng quý!", "", 2400);
        } else if (y > 180.0D) {
            pushAlert(sp, "HIGH_ALTITUDE_LAYER", "Đạt độ cao Tầng Không (Y > 180): Chú ý tốc độ rơi & Thủy Quái Charybdis!", "", 2400);
        }

        if (sl.isThundering()) {
            pushAlert(sp, "WEATHER_THUNDER", "Cảnh báo Thời Tiết: Bão sấm dữ dội đang hoành hành trên bầu trời!", "", 2400);
        }
        long dayTime = sl.getDayTime() % 24000L;
        if (dayTime >= 13000L && dayTime <= 13600L) {
            pushAlert(sp, "NIGHT_FALL", "Màn đêm đã buông xuống — Ác Ma Ma Giới có xác suất xuất hiện tại Overworld!", "", 1200);
        } else if (dayTime >= 0L && dayTime <= 500L) {
            pushAlert(sp, "DAWN_BREAK", "Rạng Đông đã lên — tầm nhìn & ánh sáng tự nhiên đã phục hồi!", "", 1200);
        }

        // N49: Quét Kim Cương / Debris lộ thiên nếu đang cầm Cuốc
        if (sp.getMainHandItem().getItem() instanceof PickaxeItem) {
            BlockPos c = sp.blockPosition();
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -6; dz <= 6; dz++) {
                        Block b = sl.getBlockState(c.offset(dx, dy, dz)).getBlock();
                        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE || b == Blocks.ANCIENT_DEBRIS) {
                            pushAlert(sp, "NEARBY_RARE_ORE", "Phát hiện mạch " + b.getName().getString() + " ở ngay gần đây (trong 6m)!", "", 600);
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
                BlockPos standPos = cavePos.offset(0, -1, 10);
                if (sl != ow) {
                    sp.teleportTo(ow, standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, 180.0F, -10.0F);
                } else {
                    SpatialTeleportAbility.executeCinematicDemonTeleport(ow, sp, standPos, "Hang Động Phong Ấn Bạo Phong Long Veldora");
                }
                com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.syncPrisonsToPlayer(sp);
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
                sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                sp.removeEffect(MobEffects.DIG_SLOWDOWN);
                sp.removeEffect(MobEffects.HUNGER);
                sp.clearFire();
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
