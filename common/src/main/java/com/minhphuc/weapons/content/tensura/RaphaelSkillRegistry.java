package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility;
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
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
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.network.ServerboundAzathothActionPacket;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * TRÍ TUỆ VƯƠNG RAPHAEL (ULTIMATE SKILL: LORD OF WISDOM RAPHAEL)
 * Quản lý & thực thi 50 KỸ NĂNG TÍNH TOÁN & THAO TÚNG CHÂN THỰC (0..49), chia làm 5 Nhóm × 10 Kỹ Năng:
 *  - Nhóm 0 (0..9): Phân Tích & Thẩm Định
 *  - Nhóm 1 (10..19): Hỗ Trợ & Sinh Tồn
 *  - Nhóm 2 (20..29): Tác Chiến & Kiểm Soát
 *  - Nhóm 3 (30..39): Chế Tạo & Chuyển Hóa
 *  - Nhóm 4 (40..49): Thao Túng Pháp Tắc & Không Gian
 */
public final class RaphaelSkillRegistry {

    public static final int TOTAL_SKILLS = 50;

    public enum Category {
        ANALYSIS(0, "Phân Tích", 0x38BDF8),
        SURVIVAL(1, "Sinh Tồn", 0x22C55E),
        COMBAT(2, "Tác Chiến", 0xEF4444),
        CRAFTING(3, "Chế Tạo", 0xF59E0B),
        LAW_SPACE(4, "Pháp Tắc", 0xA855F7);

        public final int id;
        public final String title;
        public final int color;

        Category(int id, String title, int color) {
            this.id = id;
            this.title = title;
            this.color = color;
        }
    }

    public record RaphaelSkill(int id, Category category, String name, String description) {}

    public static final List<RaphaelSkill> SKILLS = List.of(
            // === NHÓM I: PHÂN TÍCH & THẨM ĐỊNH (0..9) ===
            new RaphaelSkill(0, Category.ANALYSIS, "1. Thẩm Định Vạn Vật (All of Creation)", "Phân tích HP, Giáp, EP, phẩm cấp vũ khí & điểm yếu mục tiêu"),
            new RaphaelSkill(1, Category.ANALYSIS, "2. Cảm Quan Ma Lực Cao Cấp (Magic Sense+)", "Định vị & phát sáng mọi sinh vật và Boss trong bán kính 48m"),
            new RaphaelSkill(2, Category.ANALYSIS, "3. Giải Mã Nguyền Rủa (Curse Decoder)", "Thanh tẩy toàn bộ Poison, Wither, Slowness, Darkness & lửa"),
            new RaphaelSkill(3, Category.ANALYSIS, "4. Truy Vết Long Chủng (Dragon Radar)", "Quét Hang Phong Ấn Veldora & các True Dragon gần nhất"),
            new RaphaelSkill(4, Category.ANALYSIS, "5. Quét Mạch Khoáng Cổ Đại (Ore Scan)", "Quét Kim Cương, Debris, Ngọc Lục Bảo & Vàng trong 18m"),
            new RaphaelSkill(5, Category.ANALYSIS, "6. Đọc Suy Nghĩ Thực Thể (Mind Scan)", "Phân tích trạng thái Aggro/Thuần Phục & làm choáng nhẹ địch 2s"),
            new RaphaelSkill(6, Category.ANALYSIS, "7. Tính Toán Quỹ Đạo Tuyệt Đối (Trajectory)", "Tăng Sức Mạnh II, Tốc Độ II & phát hiện kẻ tàng hình"),
            new RaphaelSkill(7, Category.ANALYSIS, "8. Phân Tích Mê Cung & Không Gian", "Kiểm tra Neo Không Gian, Mê Cung Ramiris & Kết Giới gần đây"),
            new RaphaelSkill(8, Category.ANALYSIS, "9. Ước Lượng Chỉ Số EP Tổng Hợp", "Tính tổng EP bản thân, vũ khí phẩm cấp & đồng minh kề bên"),
            new RaphaelSkill(9, Category.ANALYSIS, "10. Dự Đoán Nguy Hiểm (Danger Sense)", "Bật Giác Quan Nguy Hiểm: Absorption II + Resistance I (20s)"),

            // === NHÓM II: HỖ TRỢ & SINH TỒN (10..19) ===
            new RaphaelSkill(10, Category.SURVIVAL, "11. Siêu Tốc Tái Sinh Khẩn Cấp", "Hồi ngay 60% HP tối đa + Regeneration III trong 10 giây"),
            new RaphaelSkill(11, Category.SURVIVAL, "12. Tổng Hợp Dược Tề Toàn Phần", "Phục hồi 100% Máu, No Đủ & Hơi Thở Nước ngay lập tức"),
            new RaphaelSkill(12, Category.SURVIVAL, "13. Đa Trùng Kết Giới Tự Động", "Kích hoạt Đa Trùng Kết Giới + Absorption III bảo hộ"),
            new RaphaelSkill(13, Category.SURVIVAL, "14. Tối Ưu Dinh Dưỡng & Thể Lực", "Hồi đầy thanh Đói, Saturation V & Haste II trong 30 giây"),
            new RaphaelSkill(14, Category.SURVIVAL, "15. Quang Học Đêm & Thợ Lặn", "Night Vision 3p + Water Breathing 3p + Fire Resistance 60s"),
            new RaphaelSkill(15, Category.SURVIVAL, "16. Hồi Sinh Đồng Minh Toàn Diện", "Hồi +40 HP & giải trừ debuff cho Veldora, Ác Ma, Tinh Linh & Dân Làng"),
            new RaphaelSkill(16, Category.SURVIVAL, "17. Triệt Tiêu Chấn Động & Trọng Lực", "Cấp Resistance III + Slow Falling trong 15s miễn thương rơi"),
            new RaphaelSkill(17, Category.SURVIVAL, "18. Tái Cấu Trúc Cơ Bắp", "Tăng Speed II + Jump Boost II + Haste II suốt 45 giây"),
            new RaphaelSkill(18, Category.SURVIVAL, "19. Màng Lọc Nhiệt & Độc Tố", "Xóa độc, dập lửa & cấp Fire Resistance + Regeneration 45s"),
            new RaphaelSkill(19, Category.SURVIVAL, "20. Bật/Tắt Ủy Thác Auto-Battle", "Chuyển đổi chế độ Tự Động Phòng Ngự Khẩn Cấp khi < 30% HP"),

            // === NHÓM III: TÁC CHIẾN & KIỂM SOÁT (20..29) ===
            new RaphaelSkill(20, Category.COMBAT, "21. Biệt Lập Tư Duy (Thought Lock)", "Khóa chân kẻ địch trước mặt bằng Slowness X + Weakness III (6s)"),
            new RaphaelSkill(21, Category.COMBAT, "22. Cưỡng Đoạt Vận Tốc (Velocity Steal)", "Hất văng toàn bộ quái trong 12m & cướp thành Speed III cho bản thân"),
            new RaphaelSkill(22, Category.COMBAT, "23. Sóng Xung Kích Tư Duy (Mind Wave)", "Nổ sóng tâm linh 360° bán kính 10m gây 45 sát thương xuyên giáp"),
            new RaphaelSkill(23, Category.COMBAT, "24. Lưỡi Dao Gió Tính Toán (Wind Slash)", "Bắn tia khí nén chuẩn xác 28m gây 60 sát thương phép"),
            new RaphaelSkill(24, Category.COMBAT, "25. Cột Băng Cố Định Nhiệt (Thermal Lock)", "Đóng băng cứng mọi kẻ địch trong 10m suốt 8 giây"),
            new RaphaelSkill(25, Category.COMBAT, "26. Tính Toán Cơn Mưa Tên (Arrow Volley)", "Phóng loạt 12 mũi tên tính toán quỹ đạo hội tụ hướng nhìn"),
            new RaphaelSkill(26, Category.COMBAT, "27. Đánh Dấu Tử Huyệt (Fatal Mark)", "Áp Weakness IV + Glowing lên mục tiêu 18m trong 12 giây"),
            new RaphaelSkill(27, Category.COMBAT, "28. Khóa Cứng Trọng Lực (Gravity Pin)", "Dìm toàn bộ quái đang bay xuống mặt đất trong 20m"),
            new RaphaelSkill(28, Category.COMBAT, "29. Vòng Phản Chiếu Xung Động", "Đốt cháy & đánh bật mọi kẻ địch áp sát trong bán kính 8m"),
            new RaphaelSkill(29, Category.COMBAT, "30. Đoạn Tuyệt Ma Lực Cục Bộ", "Khóa kỹ năng đối thủ trong bán kính 14m suốt 8 giây"),

            // === NHÓM IV: CHẾ TẠO & CHUYỂN HÓA (30..39) ===
            new RaphaelSkill(30, Category.CRAFTING, "31. Tu Sửa Vật Phẩm Tuyệt Đối", "Hồi 100% độ bền Vũ Khí trên tay & toàn bộ 4 món Áo Giáp"),
            new RaphaelSkill(31, Category.CRAFTING, "32. Tinh Luyện Quặng Tức Thời", "Nung toàn bộ Raw Iron/Gold/Copper trong túi thành Thỏi"),
            new RaphaelSkill(32, Category.CRAFTING, "33. Tổng Hợp Kim Cương Từ Than", "Chuyển hóa 16 Coal/Charcoal trong túi thành 2 Kim Cương"),
            new RaphaelSkill(33, Category.CRAFTING, "34. Chuyển Hóa Ngọc Lục Bảo", "Đổi 8 Gold Ingot trong túi thành 2 Emerald + 8 Redstone"),
            new RaphaelSkill(34, Category.CRAFTING, "35. Kết Tinh Kinh Nghiệm Thành Táo Vàng", "Tiêu 100 điểm XP tạo 2 Golden Apple cấp tốc vào túi"),
            new RaphaelSkill(35, Category.CRAFTING, "36. Tổng Hợp Mảnh Cổ Vật Netherite", "Chuyển 4 Iron Ingot + 1 Diamond -> 1 Netherite Scrap"),
            new RaphaelSkill(36, Category.CRAFTING, "37. Kích Xung Vũ Khí (Weapon Overclock)", "Tăng Strength III + Tốc Đánh trong 20s để khuếch đại sát thương"),
            new RaphaelSkill(37, Category.CRAFTING, "38. Tái Chế Đá Cuội (Material Recycle)", "Chuyển 32 Cobblestone trong túi -> 6 Iron Nugget tinh luyện"),
            new RaphaelSkill(38, Category.CRAFTING, "39. Pha Chế Nước Thánh Hồi Máu II", "Tạo 1 Potion Instant Health II (cần 1 Gold Nugget / miễn phí Creative)"),
            new RaphaelSkill(39, Category.CRAFTING, "40. Mở Dạ Dày Vô Hạn Azathoth", "Mở thẳng Giao Diện Không Gian Ảo Azathoth để hấp thụ/chuyển hóa"),

            // === NHÓM V: THAO TÚNG PHÁP TẮC & KHÔNG GIAN (40..49) ===
            new RaphaelSkill(40, Category.LAW_SPACE, "41. Chuyển Vị Không Gian 16m (Blink)", "Dịch chuyển tức thì tối đa 16m theo hướng nhìn an toàn"),
            new RaphaelSkill(41, Category.LAW_SPACE, "42. Điều Khiển Thời Tiết (Weather Law)", "Xua tan bão sấm hoặc gọi mưa dập lửa tức thì"),
            new RaphaelSkill(42, Category.LAW_SPACE, "43. Gia Tốc Thời Gian Cá Nhân", "Cấp Speed IV + Haste IV trong 8s để di chuyển & đào siêu tốc"),
            new RaphaelSkill(43, Category.LAW_SPACE, "44. Vòng Xoáy Không Gian Gom Quái", "Hút toàn bộ kẻ địch & vật phẩm rơi trong 16m về trước mặt"),
            new RaphaelSkill(44, Category.LAW_SPACE, "45. Thanh Tẩy Nhiệt & Tuyết Bột", "Dập toàn bộ lửa & phá hủy Powder Snow quanh người chơi 8m"),
            new RaphaelSkill(45, Category.LAW_SPACE, "46. Hồi Quy Điểm Neo Không Gian", "Dịch chuyển tức thời về Ấn Ký Không Gian Vĩnh Viễn đang chọn"),
            new RaphaelSkill(46, Category.LAW_SPACE, "47. Giải Phong Ấn Veldora Khẩn Cấp", "Quét & Giải Phong Ấn Veldora nếu đang ở Hang Động Phong Ấn"),
            new RaphaelSkill(47, Category.LAW_SPACE, "48. Thoát Hiểm Hầm Ngục Lên Mặt Đất", "Dịch chuyển lên đỉnh địa hình thoáng cao nhất ngay phía trên"),
            new RaphaelSkill(48, Category.LAW_SPACE, "49. Cộng Hưởng Linh Tử Raphael", "Bắn tia Linh Tử xuyên giáp 32m gây 75 sát thương chuẩn"),
            new RaphaelSkill(49, Category.LAW_SPACE, "50. Khải Huyền Thống Lĩnh (Overdrive)", "Kích hoạt đồng thời 5 buff tối cao trong 20 giây!")
    );

    private RaphaelSkillRegistry() {}

    public static void execute(ServerLevel level, ServerPlayer sp, int skillId) {
        if (sp == null || level == null || skillId < 0 || skillId >= SKILLS.size()) return;
        RaphaelSkill skill = SKILLS.get(skillId);

        switch (skillId) {
            case 0 -> executeAppraisal(level, sp);
            case 1 -> executeMagicSensePlus(level, sp);
            case 2 -> SageManager.handleAction(sp, com.minhphuc.weapons.network.ServerboundSageActionPacket.ACTION_DECODE_CURSE);
            case 3 -> executeDragonRadar(level, sp);
            case 4 -> executeOreScan(level, sp);
            case 5 -> executeMindScan(level, sp);
            case 6 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(32.0D), e -> e != sp && e.isInvisible())) {
                    le.removeEffect(MobEffects.INVISIBILITY);
                    le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
                SageManager.sendCornerAlert(sp, "Tính Toán Quỹ Đạo: Strength II + Speed II & triệt tiêu tàng hình quanh 32m!", 0xFF38BDF8, 4500, false);
            }
            case 7 -> executeSpaceAnalysis(level, sp);
            case 8 -> executeEpAppraisal(level, sp);
            case 9 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
                SageManager.sendCornerAlert(sp, "Dự Đoán Nguy Hiểm: Đã dựng Lớp Đệm Hấp Thụ II + Kháng I (20s)!", 0xFF38BDF8, 4500, false);
            }

            case 10 -> {
                sp.heal(sp.getMaxHealth() * 0.6F);
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
                SageManager.sendCornerAlert(sp, "Siêu Tốc Tái Sinh: Đã hồi 60% Sinh Lực tối đa + Regeneration III!", 0xFF22C55E, 4500, false);
            }
            case 11 -> {
                sp.setHealth(sp.getMaxHealth());
                sp.getFoodData().eat(20, 2.0F);
                sp.setAirSupply(sp.getMaxAirSupply());
                SageManager.sendCornerAlert(sp, "Dược Tề Toàn Phần: Phục hồi 100% HP, Thức Ăn & Dưỡng Khí!", 0xFF22C55E, 4500, false);
            }
            case 12 -> {
                MultilayerBarrierAbility.cast(level, sp);
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 2));
                SageManager.sendCornerAlert(sp, "Raphael Đa Trùng Kết Giới: Khiên Đa Tầng đã kích hoạt bảo hộ!", 0xFF22C55E, 4500, false);
            }
            case 13 -> {
                sp.getFoodData().eat(16, 1.5F);
                sp.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 4));
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1));
                SageManager.sendCornerAlert(sp, "Tối Ưu Thể Lực: Bổ sung Dinh Dưỡng Tối Đa & Haste II (30s)!", 0xFF22C55E, 4500, false);
            }
            case 14 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0, true, false));
                sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0, true, false));
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0, true, false));
                SageManager.sendCornerAlert(sp, "Quang Học & Dưỡng Khí: Night Vision + Thở Nước (3p) + Kháng Lửa (60s)!", 0xFF22C55E, 4500, false);
            }
            case 15 -> executeAllyMedic(level, sp);
            case 16 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 2));
                sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0));
                SageManager.sendCornerAlert(sp, "Triệt Tiêu Chấn Động: Resistance III + Slow Falling (15s)!", 0xFF22C55E, 4500, false);
            }
            case 17 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 900, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 900, 1));
                SageManager.sendCornerAlert(sp, "Tái Cấu Trúc Cơ Bắp: Speed II + Jump II + Haste II (45s)!", 0xFF22C55E, 4500, false);
            }
            case 18 -> {
                sp.clearFire();
                sp.removeEffect(MobEffects.POISON);
                sp.removeEffect(MobEffects.WITHER);
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 900, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                SageManager.sendCornerAlert(sp, "Màng Lọc Nhiệt & Độc: Thanh tẩy độc/lửa & kích hoạt Kháng Lửa!", 0xFF22C55E, 4500, false);
            }
            case 19 -> SageManager.handleAction(sp, com.minhphuc.weapons.network.ServerboundSageActionPacket.ACTION_TOGGLE_AUTO_BATTLE);

            case 20 -> executeThoughtIsolation(level, sp);
            case 21 -> {
                int pushed = 0;
                for (LivingEntity e : getHostiles(level, sp, 12.0D)) {
                    Vec3 away = e.position().subtract(sp.position()).normalize().scale(2.1D);
                    e.setDeltaMovement(away.x, 0.55D, away.z);
                    e.hurtMarked = true;
                    pushed++;
                }
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
                SageManager.sendCornerAlert(sp, "Cưỡng Đoạt Vận Tốc: Đẩy lùi " + pushed + " mục tiêu & tăng Speed III!", 0xFFEF4444, 4500, false);
            }
            case 22 -> {
                int hit = 0;
                for (LivingEntity e : getHostiles(level, sp, 10.0D)) {
                    e.hurt(level.damageSources().magic(), 45.0F);
                    Vec3 away = e.position().subtract(sp.position()).normalize().scale(1.6D);
                    e.setDeltaMovement(away.x, 0.5D, away.z);
                    hit++;
                }
                level.sendParticles(ParticleTypes.SONIC_BOOM, sp.getX(), sp.getY() + 1.0, sp.getZ(), 2, 1.5, 0.5, 1.5, 0.0);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8F, 1.4F);
                SageManager.sendCornerAlert(sp, "Sóng Xung Kích Tư Duy: Đánh trúng " + hit + " mục tiêu (45 ST Chuẩn)!", 0xFFEF4444, 4500, false);
            }
            case 23 -> executeWindBlade(level, sp);
            case 24 -> {
                int frozen = 0;
                for (LivingEntity e : getHostiles(level, sp, 10.0D)) {
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 200, e.getTicksFrozen() + 180));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
                    frozen++;
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, sp.getX(), sp.getY() + 1.0, sp.getZ(), 50, 4.0, 1.0, 4.0, 0.05);
                SageManager.sendCornerAlert(sp, "Cột Băng Cố Định Nhiệt: Đóng băng " + frozen + " kẻ địch trong 10m!", 0xFFEF4444, 4500, false);
            }
            case 25 -> executeArrowVolley(level, sp);
            case 26 -> {
                int marked = 0;
                for (LivingEntity e : getHostiles(level, sp, 18.0D)) {
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
                    marked++;
                }
                SageManager.sendCornerAlert(sp, "Đánh Dấu Tử Huyệt: Áp Weakness IV lên " + marked + " kẻ địch (12s)!", 0xFFEF4444, 4500, false);
            }
            case 27 -> {
                int grounded = 0;
                for (LivingEntity e : getHostiles(level, sp, 20.0D)) {
                    e.setDeltaMovement(0.0D, -2.5D, 0.0D);
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                    grounded++;
                }
                SageManager.sendCornerAlert(sp, "Khóa Cứng Trọng Lực: Kéo sập " + grounded + " mục tiêu xuống đất!", 0xFFEF4444, 4500, false);
            }
            case 28 -> {
                int repelled = 0;
                for (LivingEntity e : getHostiles(level, sp, 8.0D)) {
                    e.igniteForSeconds(5);
                    e.hurt(level.damageSources().inFire(), 28.0F);
                    Vec3 away = e.position().subtract(sp.position()).normalize().scale(2.2D);
                    e.setDeltaMovement(away.x, 0.65D, away.z);
                    repelled++;
                }
                SageManager.sendCornerAlert(sp, "Vòng Phản Chiếu Xung Động: Đánh bật & thiêu đốt " + repelled + " kẻ thù!", 0xFFEF4444, 4500, false);
            }
            case 29 -> {
                int silenced = 0;
                for (LivingEntity e : getHostiles(level, sp, 14.0D)) {
                    SkillNullificationAbility.sealEntitySkills(e, 160);
                    silenced++;
                }
                SageManager.sendCornerAlert(sp, "Đoạn Tuyệt Ma Lực Cục Bộ: Phong ấn kỹ năng " + silenced + " kẻ thù trong 8s!", 0xFFEF4444, 4500, false);
            }

            case 30 -> executePerfectRepair(sp);
            case 31 -> executeInstantSmelt(sp);
            case 32 -> executeTransmute(sp, Items.COAL, 16, Items.DIAMOND, 2, "Than -> Kim Cương");
            case 33 -> executeTransmute(sp, Items.GOLD_INGOT, 8, Items.EMERALD, 2, "Vàng -> Ngọc Lục Bảo");
            case 34 -> {
                if (sp.isCreative() || sp.totalExperience >= 100 || sp.experienceLevel >= 3) {
                    if (!sp.isCreative()) sp.giveExperiencePoints(-100);
                    sp.getInventory().placeItemBackInInventory(new ItemStack(Items.GOLDEN_APPLE, 2));
                    SageManager.sendCornerAlert(sp, "Kết Tinh Kinh Nghiệm: Đã tạo 2 Táo Vàng (Golden Apple)!", 0xFFF59E0B, 4500, false);
                } else {
                    SageManager.sendCornerAlert(sp, "Không đủ XP (cần tối thiểu 100 điểm kinh nghiệm)!", 0xFFEF4444, 4500, false);
                }
            }
            case 35 -> {
                if (sp.isCreative() || (consumeItems(sp, Items.IRON_INGOT, 4) && consumeItems(sp, Items.DIAMOND, 1))) {
                    sp.getInventory().placeItemBackInInventory(new ItemStack(Items.NETHERITE_SCRAP, 1));
                    SageManager.sendCornerAlert(sp, "Tổng Hợp Vật Liệu: Đã tạo 1 Netherite Scrap!", 0xFFF59E0B, 4500, false);
                } else {
                    SageManager.sendCornerAlert(sp, "Cần 4 Iron Ingot + 1 Diamond trong túi để tổng hợp!", 0xFFEF4444, 4500, false);
                }
            }
            case 36 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2));
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, 2));
                SageManager.sendCornerAlert(sp, "Kích Xung Vũ Khí: Strength III + Haste III trong 20 giây!", 0xFFF59E0B, 4500, false);
            }
            case 37 -> executeTransmute(sp, Items.COBBLESTONE, 32, Items.IRON_NUGGET, 6, "32 Đá Cuội -> 6 Mảnh Sắt");
            case 38 -> {
                if (sp.isCreative() || consumeItems(sp, Items.GOLD_NUGGET, 1)) {
                    ItemStack pot = PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
                    sp.getInventory().placeItemBackInInventory(pot);
                    SageManager.sendCornerAlert(sp, "Pha Chế Tức Thời: Đã tạo Nước Thánh Hồi Máu II vào túi!", 0xFFF59E0B, 4500, false);
                } else {
                    SageManager.sendCornerAlert(sp, "Cần 1 Gold Nugget trong túi để pha chế Nước Thánh!", 0xFFEF4444, 4500, false);
                }
            }
            case 39 -> ServerboundAzathothActionPacket.sendOpenGui(sp);

            case 40 -> executeInstantBlink(level, sp);
            case 41 -> {
                if (level.isRaining() || level.isThundering()) {
                    level.setWeatherParameters(12000, 0, false, false);
                    SageManager.sendCornerAlert(sp, "Điều Khiển Thời Tiết: Đã xua tan giông bão, bầu trời quang đãng!", 0xFFA855F7, 4500, false);
                } else {
                    level.setWeatherParameters(0, 6000, true, false);
                    SageManager.sendCornerAlert(sp, "Điều Khiển Thời Tiết: Đã triệu hồi cơn mưa làm mát chiến trường!", 0xFFA855F7, 4500, false);
                }
            }
            case 42 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 3));
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 160, 3));
                SageManager.sendCornerAlert(sp, "Gia Tốc Thời Gian Cục Bộ: Speed IV + Haste IV (8s)!", 0xFFA855F7, 4500, false);
            }
            case 43 -> {
                Vec3 focal = sp.position().add(sp.getLookAngle().multiply(1, 0, 1).normalize().scale(6.0D));
                int pulled = 0;
                for (LivingEntity e : getHostiles(level, sp, 16.0D)) {
                    Vec3 pull = focal.subtract(e.position()).scale(0.65D);
                    e.setDeltaMovement(pull.x, 0.35D, pull.z);
                    e.hurtMarked = true;
                    pulled++;
                }
                for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, sp.getBoundingBox().inflate(16.0D))) {
                    ie.teleportTo(focal.x, focal.y + 0.5D, focal.z);
                }
                SageManager.sendCornerAlert(sp, "Vòng Xoáy Không Gian: Đã gom " + pulled + " mục tiêu & vật phẩm rơi!", 0xFFA855F7, 4500, false);
            }
            case 44 -> executeThermalSnowClean(level, sp);
            case 45 -> SpatialTeleportAbility.castTeleportToSlot(level, sp, 1);
            case 46 -> AzathothStomachData.analyzeOrUnsealVeldora(level, sp);
            case 47 -> {
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().getX(), sp.blockPosition().getZ());
                sp.teleportTo(sp.getX(), topY + 1.0D, sp.getZ());
                sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
                SageManager.sendCornerAlert(sp, "Thoát Hiểm Hầm Ngục: Đã dịch chuyển lên mặt đất tại Y=" + (topY + 1) + "!", 0xFFA855F7, 4500, false);
            }
            case 48 -> executeSpiritronBurst(level, sp);
            case 49 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.3F, 1.3F);
                SageManager.sendCornerAlert(sp, "Khải Huyền Thống Lĩnh (Overdrive): Kích hoạt 5 Buff Tối Cao suốt 20 giây!", 0xFFFFD700, 5500, false);
            }
        }
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    private static List<LivingEntity> getHostiles(ServerLevel level, ServerPlayer sp, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(radius),
                e -> e != sp && e.isAlive()
                        && !(e instanceof TamableAnimal ta && ta.isTame() && ta.isOwnedBy(sp))
                        && !(e instanceof Villager)
                        && !(e instanceof VelgryndEntity vg && vg.isAllied() && sp.getUUID().equals(vg.getParsedOwnerUUID())));
    }

    private static void executeAppraisal(ServerLevel level, ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(24.0D));
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(2.5D), x -> x != sp && x.isAlive())) {
            double d = e.distanceToSqr(sp);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        if (best != null) {
            WeaponGradeHelper.WeaponGrade g = WeaponGradeHelper.getGrade(best.getMainHandItem());
            String extra = best instanceof VeldoraEntity v ? (v.isHumanForm() ? "Dạng Người" : "Dạng Rồng x30")
                    : (best instanceof UnderworldDemonEntity u ? u.getRank().title + (u.hasPhysicalBody() ? " [Có Thể Xác]" : " [Linh Thể]") : "");
            SageManager.sendCornerAlert(sp,
                    "Thẩm Định [" + best.getName().getString() + "]: HP " + (int) best.getHealth() + "/" + (int) best.getMaxHealth()
                            + (extra.isEmpty() ? "" : " | " + extra)
                            + (g != WeaponGradeHelper.WeaponGrade.NORMAL ? " | Vũ khí " + g.title : ""),
                    0xFF38BDF8, 7000, false);
        } else {
            ItemStack main = sp.getMainHandItem();
            WeaponGradeHelper.WeaponGrade g = WeaponGradeHelper.getGrade(main);
            SageManager.sendCornerAlert(sp, "Thẩm Định Vật Phẩm: " + (main.isEmpty() ? "Tay Không" : main.getHoverName().getString() + " [" + g.title + "]"), 0xFF38BDF8, 5500, false);
        }
    }

    private static void executeMagicSensePlus(ServerLevel level, ServerPlayer sp) {
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(48.0D), e -> e != sp && e.isAlive());
        int bosses = 0;
        for (LivingEntity le : list) {
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0, false, false));
            if (le instanceof VeldoraEntity || le instanceof VelgryndEntity || le instanceof VelzardEntity
                    || le instanceof MilimEntity || le instanceof PrimordialDemonEntity || le instanceof CharybdisEntity
                    || le instanceof OrcDisasterGeldEntity || le instanceof ChronoaEntity) {
                bosses++;
            }
        }
        SageManager.sendCornerAlert(sp, "Cảm Quan Ma Lực 48m: Phát hiện " + list.size() + " sinh thể (" + bosses + " Boss cấp cao)!", 0xFF38BDF8, 6000, false);
    }

    private static void executeDragonRadar(ServerLevel level, ServerPlayer sp) {
        ServerLevel ow = level.getServer().overworld();
        VeldoraWorldData vd = VeldoraWorldData.get(ow);
        BlockPos cave = vd.getOrRollCavePos(ow);
        if (!vd.isVeldoraUnsealed()) {
            SageManager.sendCornerAlert(sp, "Truy Vết Long Chủng: Hang Veldora tại Overworld (" + cave.getX() + ", " + cave.getY() + ", " + cave.getZ() + ")", 0xFF00E5FF, 8000, true);
        } else {
            List<LivingEntity> dragons = level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(96.0D),
                    e -> e instanceof VeldoraEntity || e instanceof VelgryndEntity || e instanceof VelzardEntity);
            if (!dragons.isEmpty()) {
                LivingEntity d = dragons.get(0);
                SageManager.sendCornerAlert(sp, "Phát hiện Long Chủng [" + d.getName().getString() + "] cách " + (int) sp.distanceTo(d) + "m!", 0xFF00E5FF, 6500, false);
            } else {
                SageManager.sendCornerAlert(sp, "Hang Veldora đã giải ấn; không có Long Chủng nào khác trong 96m.", 0xFF38BDF8, 5000, false);
            }
        }
    }

    private static void executeOreScan(ServerLevel level, ServerPlayer sp) {
        int diamonds = 0, debris = 0, emeralds = 0, gold = 0;
        BlockPos c = sp.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -14; dx <= 14; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                for (int dz = -14; dz <= 14; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    Block b = level.getBlockState(m).getBlock();
                    if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) diamonds++;
                    else if (b == Blocks.ANCIENT_DEBRIS) debris++;
                    else if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) emeralds++;
                    else if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) gold++;
                }
            }
        }
        SageManager.sendCornerAlert(sp, "Quét Khoáng Sản 14m: Kim Cương=" + diamonds + ", Debris=" + debris + ", Lục Bảo=" + emeralds + ", Vàng=" + gold, 0xFF38BDF8, 6500, false);
    }

    private static void executeMindScan(ServerLevel level, ServerPlayer sp) {
        int count = 0;
        for (LivingEntity e : getHostiles(level, sp, 16.0D)) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            count++;
        }
        SageManager.sendCornerAlert(sp, "Đọc Suy Nghĩ: Đã làm khựng " + count + " kẻ địch trong 16m suốt 2 giây!", 0xFF38BDF8, 4500, false);
    }

    private static void executeSpaceAnalysis(ServerLevel level, ServerPlayer sp) {
        boolean inBarrier = AntiMagicBarrierManager.isInBarrier(level, sp.position());
        boolean hasAnchor = SpatialTeleportAbility.hasAnchorMark(sp);
        BlockPos lab = LabyrinthWorldData.get(level.getServer().overworld()).getOrRollEntrancePos(level.getServer().overworld());
        SageManager.sendCornerAlert(sp, "Không Gian: " + (inBarrier ? "[Trong Kháng Ma Kết Giới] " : "[Ổn định] ")
                + (hasAnchor ? "| Neo Vĩnh Viễn: Sẵn sàng" : "| Chưa đặt Neo")
                + " | Mê Cung Ramiris (" + lab.getX() + "," + lab.getZ() + ")", 0xFF38BDF8, 7000, false);
    }

    private static void executeEpAppraisal(ServerLevel level, ServerPlayer sp) {
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        boolean tdl = data.getBoolean("TensuraTrueDemonLord");
        long baseEp = tdl ? 12_500_000L : 850_000L;
        WeaponGradeHelper.WeaponGrade g = WeaponGradeHelper.getGrade(sp.getMainHandItem());
        long weaponEp = switch (g) {
            case ORIGIN -> 5_000_000L;
            case MYTHICAL -> 2_200_000L;
            case LEGENDARY -> 900_000L;
            case EPIC -> 350_000L;
            case RARE -> 120_000L;
            default -> 20_000L;
        };
        long total = baseEp + weaponEp + sp.experienceLevel * 12_000L;
        SageManager.sendCornerAlert(sp, "Chỉ Số Ma Tố Tổng Hợp (EP): ~" + String.format("%,d", total) + " EP (Vũ khí: " + g.title + ")!", 0xFF38BDF8, 6500, false);
    }

    private static void executeAllyMedic(ServerLevel level, ServerPlayer sp) {
        int healed = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(24.0D), x -> x != sp && x.isAlive())) {
            boolean ally = (e instanceof TamableAnimal ta && ta.isTame() && ta.isOwnedBy(sp))
                    || (e instanceof VelgryndEntity vg && vg.isAllied())
                    || e instanceof Villager
                    || e instanceof ElementalSpiritEntity;
            if (ally) {
                e.heal(40.0F);
                e.removeEffect(MobEffects.WITHER);
                e.removeEffect(MobEffects.POISON);
                healed++;
            }
        }
        SageManager.sendCornerAlert(sp, "Hồi Sinh Đồng Minh: Đã trị liệu +40 HP cho " + healed + " đồng minh & dân làng!", 0xFF22C55E, 5000, false);
    }

    private static void executeThoughtIsolation(ServerLevel level, ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(22.0D));
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(2.5D), x -> x != sp && x.isAlive());
        if (!hits.isEmpty()) {
            LivingEntity t = hits.get(0);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 9));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
            SageManager.sendCornerAlert(sp, "Biệt Lập Tư Duy: Khóa cứng [" + t.getName().getString() + "] trong 6 giây!", 0xFFEF4444, 4500, false);
        } else {
            SageManager.sendCornerAlert(sp, "Không có mục tiêu trong tầm nhìn 22m!", 0xFFF59E0B, 3500, false);
        }
    }

    private static void executeWindBlade(ServerLevel level, ServerPlayer sp) {
        Vec3 start = sp.getEyePosition();
        Vec3 dir = sp.getLookAngle().normalize();
        DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0.2F, 0.9F, 1.0F), 1.6F);
        int hits = 0;
        for (double d = 1.5D; d <= 28.0D; d += 1.0D) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(cyan, p.x, p.y, p.z, 3, 0.15, 0.15, 0.15, 0.02);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(1.8D), x -> x != sp && x.isAlive())) {
                e.hurt(level.damageSources().magic(), 60.0F);
                hits++;
            }
        }
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6F, 1.5F);
        SageManager.sendCornerAlert(sp, "Lưỡi Dao Gió Tính Toán: Xuyên qua " + hits + " mục tiêu (60 ST)!", 0xFFEF4444, 4500, false);
    }

    private static void executeArrowVolley(ServerLevel level, ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 dir = sp.getLookAngle().normalize();
        for (int i = 0; i < 12; i++) {
            Arrow arrow = new Arrow(level, sp, new ItemStack(Items.ARROW), null);
            double spreadX = (level.random.nextDouble() - 0.5D) * 0.12D;
            double spreadY = (level.random.nextDouble() - 0.5D) * 0.08D;
            double spreadZ = (level.random.nextDouble() - 0.5D) * 0.12D;
            arrow.setPos(eye.x + dir.x * 1.2D, eye.y + dir.y * 1.2D, eye.z + dir.z * 1.2D);
            arrow.shoot(dir.x + spreadX, dir.y + spreadY, dir.z + spreadZ, 3.2F, 1.0F);
            arrow.setBaseDamage(6.5D);
            level.addFreshEntity(arrow);
        }
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.4F, 1.3F);
        SageManager.sendCornerAlert(sp, "Cơn Mưa Tên Ma Pháp: Phóng 12 mũi tên tính toán quỹ đạo!", 0xFFEF4444, 4000, false);
    }

    private static void executePerfectRepair(ServerPlayer sp) {
        int fixed = 0;
        ItemStack main = sp.getMainHandItem();
        if (!main.isEmpty() && main.isDamageableItem() && main.getDamageValue() > 0) {
            main.setDamageValue(0);
            fixed++;
        }
        for (ItemStack armor : sp.getArmorSlots()) {
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getDamageValue() > 0) {
                armor.setDamageValue(0);
                fixed++;
            }
        }
        SageManager.sendCornerAlert(sp, "Tu Sửa Tuyệt Đối: Đã phục hồi 100% độ bền cho " + fixed + " trang bị!", 0xFFF59E0B, 4500, false);
    }

    private static void executeInstantSmelt(ServerPlayer sp) {
        int smelted = 0;
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(Items.RAW_IRON)) {
                int c = st.getCount();
                inv.setItem(i, new ItemStack(Items.IRON_INGOT, c));
                smelted += c;
            } else if (st.is(Items.RAW_GOLD)) {
                int c = st.getCount();
                inv.setItem(i, new ItemStack(Items.GOLD_INGOT, c));
                smelted += c;
            } else if (st.is(Items.RAW_COPPER)) {
                int c = st.getCount();
                inv.setItem(i, new ItemStack(Items.COPPER_INGOT, c));
                smelted += c;
            }
        }
        SageManager.sendCornerAlert(sp, "Tinh Luyện Quặng Tức Thời: Đã nung " + smelted + " quặng thô thành Thỏi!", 0xFFF59E0B, 4500, false);
    }

    private static void executeTransmute(ServerPlayer sp, Item in, int inCount, Item out, int outCount, String label) {
        if (sp.isCreative() || consumeItems(sp, in, inCount)) {
            sp.getInventory().placeItemBackInInventory(new ItemStack(out, outCount));
            SageManager.sendCornerAlert(sp, "Chuyển Hóa Vật Chất: Hoàn tất [" + label + "]!", 0xFFF59E0B, 4500, false);
        } else {
            SageManager.sendCornerAlert(sp, "Thiếu nguyên liệu (" + inCount + " " + in.getDescription().getString() + ")!", 0xFFEF4444, 4500, false);
        }
    }

    private static boolean consumeItems(ServerPlayer sp, Item item, int needed) {
        var inv = sp.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) total += inv.getItem(i).getCount();
        }
        if (total < needed) return false;
        int rem = needed;
        for (int i = 0; i < inv.getContainerSize() && rem > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(item)) {
                int take = Math.min(rem, st.getCount());
                st.shrink(take);
                rem -= take;
            }
        }
        return true;
    }

    private static void executeInstantBlink(ServerLevel level, ServerPlayer sp) {
        Vec3 start = sp.getEyePosition();
        Vec3 dir = sp.getLookAngle().normalize();
        BlockHitResult hit = level.clip(new ClipContext(start, start.add(dir.scale(16.0D)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 dest = (hit.getType() == HitResult.Type.MISS) ? start.add(dir.scale(15.0D)) : hit.getLocation().subtract(dir.scale(1.0D));
        level.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.3, 0.6, 0.3, 0.1);
        sp.teleportTo(dest.x, dest.y, dest.z);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
        SageManager.sendCornerAlert(sp, "Chuyển Vị Không Gian 16m: Hoàn tất!", 0xFFA855F7, 3500, false);
    }

    private static void executeThermalSnowClean(ServerLevel level, ServerPlayer sp) {
        int cleared = 0;
        BlockPos c = sp.blockPosition();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.POWDER_SNOW) || st.is(ModBlocks.BLACK_FIRE.get())) {
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        cleared++;
                    }
                }
            }
        }
        sp.clearFire();
        SageManager.sendCornerAlert(sp, "Thanh Tẩy Nhiệt & Bột Tuyết: Đã hóa giải " + cleared + " khối nguy hại!", 0xFFA855F7, 4500, false);
    }

    private static void executeSpiritronBurst(ServerLevel level, ServerPlayer sp) {
        Vec3 start = sp.getEyePosition();
        Vec3 dir = sp.getLookAngle().normalize();
        DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.25F), 1.8F);
        int hitCount = 0;
        for (double d = 1.5D; d <= 32.0D; d += 1.0D) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(gold, p.x, p.y, p.z, 4, 0.18, 0.18, 0.18, 0.02);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(1.8D), x -> x != sp && x.isAlive())) {
                e.hurt(level.damageSources().indirectMagic(sp, sp), 75.0F);
                hitCount++;
            }
        }
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);
        SageManager.sendCornerAlert(sp, "Cộng Hưởng Linh Tử Raphael: Đánh trúng " + hitCount + " mục tiêu (75 ST Xuyên Giáp)!", 0xFFA855F7, 4500, false);
    }
}
