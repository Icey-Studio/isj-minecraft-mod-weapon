package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Đại Tu Tuyệt Kỹ: Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Multi-Tier Sanctuary Disintegration)
 * Tái hiện 100% chuẩn xác tuyệt kĩ tối thượng của Hinata Sakaguchi trong Tensura (theo video Dulimo):
 *
 * 1. Giai đoạn 1 - Khởi Tạo Thánh Giới, Lồng Giam & Tụ Cầu Linh Tử (Tick 0..44):
 *    - Địa Trận Thánh Vực (Ground Array Y+0.05m, bung nở 0 -> 12m).
 *    - Nhẫn Vương Miện Lồng Trong (Inner Crown Ring Y+3.8m, Scale 7.5m, xoay ngược chiều).
 *    - Nhẫn Cổ Ngữ Hồng Ngọc (Sacred Rune Ring Y+4.2m, Scale 9.0m, xoay thuận chiều).
 *    - 12 Cột Trụ Thánh Quang (Light Cage Pillars) mọc lên từ sàn nối Địa Trận lên Nhẫn Cổ Ngữ lúc Tick 12..20, giam cầm tuyệt đối.
 *    - Đại Pháp Luân Thánh Tích Thẳng Đứng (Vertical Sacred Crest Y+7.5m, Scale 7.5m, đứng sừng sững trên đỉnh).
 *    - Quả Cầu Linh Tử (Spiritron Core Orb) tại tâm pháp luân (Y+7.5m), phát triển tụ sáng kèm các tia chớp xoắn ốc hội tụ.
 *
 * 2. Giai đoạn 2 - Cột Sáng Cực Đại & Linh Tử Băng Hoại (Tick 45..84 - Duy trì 2 giây):
 *    - Cột sáng khổng lồ 3 lớp (Lớp hào quang 11.5m, Lớp thân 8.0m, Lớp lõi 4.0m trắng tinh khiết) giáng thẳng từ thiên đỉnh.
 *    - Vòng sóng xung kích linh tử (Shockwave Halos) liên tục gợn xuống dọc theo thân cột sáng.
 *    - Âm thanh chấn động: Sấm thánh phạt, sóng âm Warden, kính vỡ không gian, nổ vang trời.
 *    - Xóa sổ đạn đạo và thực hiện sát thương phân rã linh tử (Disintegration Strike).
 *
 * 3. Giai đoạn 3 - Tiêu Biến Tuần Tự & Vòi Phun Bụi Vàng Thăng Thiên (Tick 85..125):
 *    - Cột sáng và lồng giam biến mất tức thì.
 *    - Các tầng ma trận tiêu biến tuần tự (Pháp luân đứng -> Nhẫn Cổ Ngữ & Vương Miện -> Địa Trận).
 *    - Vòi Phun Bụi Vàng (Golden Spiritron Ascension Fountain): Hàng ngàn hạt bụi vàng thăng thiên lên bầu trời.
 */
public class SanctuaryDisintegrationAbility {

    public static class ActiveSanctuary {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;

        // 4 Bộ Ma Pháp Trận chuyên biệt
        public Display.ItemDisplay groundDisplay;
        public Display.ItemDisplay crownRingDisplay;
        public Display.ItemDisplay runeRingDisplay;
        public Display.ItemDisplay verticalCrestDisplay;

        // 12 Cột Trụ Thánh Quang tạo Lồng Giam (Tick 12..84)
        public final Display.ItemDisplay[] cagePillars = new Display.ItemDisplay[12];
        public boolean cageSpawned = false;

        // Cột Sáng Cực Đại 3 Lớp (Tick 45..84)
        public Display.ItemDisplay megaBeamOuter;
        public Display.ItemDisplay megaBeamBody;
        public Display.ItemDisplay megaBeamCore;

        // 2 Vòng Sóng Xung Kích Linh Tử (Tick 45..84)
        public Display.ItemDisplay shockwaveDisplay1;
        public Display.ItemDisplay shockwaveDisplay2;

        public int ticksRemaining;
        public final int totalTicks; // 125 ticks (~6.25 giây)
        public float currentAngleDegrees;
        public boolean beamSpawned = false;
        public boolean damageDealt = false;
        public final Set<UUID> trappedVictimUuids = new HashSet<>();

        public final SkillPowerRoll powerRoll;

        public ActiveSanctuary(ServerLevel level, ServerPlayer caster, Vec3 center,
                               Display.ItemDisplay groundDisplay,
                               Display.ItemDisplay crownRingDisplay,
                               Display.ItemDisplay runeRingDisplay,
                               Display.ItemDisplay verticalCrestDisplay,
                               int durationTicks,
                               SkillPowerRoll powerRoll) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.groundDisplay = groundDisplay;
            this.crownRingDisplay = crownRingDisplay;
            this.runeRingDisplay = runeRingDisplay;
            this.verticalCrestDisplay = verticalCrestDisplay;
            this.ticksRemaining = durationTicks;
            this.totalTicks = durationTicks;
            this.currentAngleDegrees = 0.0F;
            this.powerRoll = powerRoll;
        }

        public void cleanupDisplays() {
            if (groundDisplay != null && groundDisplay.isAlive()) {
                groundDisplay.discard();
                groundDisplay = null;
            }
            if (crownRingDisplay != null && crownRingDisplay.isAlive()) {
                crownRingDisplay.discard();
                crownRingDisplay = null;
            }
            if (runeRingDisplay != null && runeRingDisplay.isAlive()) {
                runeRingDisplay.discard();
                runeRingDisplay = null;
            }
            if (verticalCrestDisplay != null && verticalCrestDisplay.isAlive()) {
                verticalCrestDisplay.discard();
                verticalCrestDisplay = null;
            }
            for (int i = 0; i < cagePillars.length; i++) {
                if (cagePillars[i] != null && cagePillars[i].isAlive()) {
                    cagePillars[i].discard();
                    cagePillars[i] = null;
                }
            }
            if (megaBeamOuter != null && megaBeamOuter.isAlive()) {
                megaBeamOuter.discard();
                megaBeamOuter = null;
            }
            if (megaBeamBody != null && megaBeamBody.isAlive()) {
                megaBeamBody.discard();
                megaBeamBody = null;
            }
            if (megaBeamCore != null && megaBeamCore.isAlive()) {
                megaBeamCore.discard();
                megaBeamCore = null;
            }
            if (shockwaveDisplay1 != null && shockwaveDisplay1.isAlive()) {
                shockwaveDisplay1.discard();
                shockwaveDisplay1 = null;
            }
            if (shockwaveDisplay2 != null && shockwaveDisplay2.isAlive()) {
                shockwaveDisplay2.discard();
                shockwaveDisplay2 = null;
            }
        }
    }

    private static final List<ActiveSanctuary> ACTIVE_SANCTUARIES = new ArrayList<>();

    public static boolean isNearActiveSanctuary(ServerLevel level, Vec3 pos, double radius) {
        double rSq = radius * radius;
        for (ActiveSanctuary s : ACTIVE_SANCTUARIES) {
            if (s.level == level && s.center.distanceToSqr(pos) <= rSq) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 getClosestSanctuaryCenter(ServerLevel level, Vec3 pos, double radius) {
        double rSq = radius * radius;
        double closestDist = Double.MAX_VALUE;
        Vec3 closest = null;
        for (ActiveSanctuary s : ACTIVE_SANCTUARIES) {
            if (s.level == level) {
                double d = s.center.distanceToSqr(pos);
                if (d <= rSq && d < closestDist) {
                    closestDist = d;
                    closest = s.center;
                }
            }
        }
        return closest;
    }

    public static void clearAllSanctuaries() {
        for (ActiveSanctuary s : ACTIVE_SANCTUARIES) {
            s.cleanupDisplays();
        }
        ACTIVE_SANCTUARIES.clear();
    }

    /**
     * Kích hoạt tuyệt kĩ: Tam Trọng Thánh Giới - Linh Tử Băng Hoại
     */
    public static void cast(ServerLevel level, ServerPlayer player, ItemStack sword) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        double maxDistance = 28.0D;
        Vec3 traceEnd = eyePos.add(lookVec.scale(maxDistance));

        // 1. Dò tìm mục tiêu: Ưu tiên bắt trúng Entity trong tầm ngắm
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, traceEnd,
                new AABB(eyePos, traceEnd).inflate(2.0D),
                e -> !e.isSpectator() && e.isPickable() && e != player,
                maxDistance * maxDistance
        );

        Vec3 targetCenter;
        if (entityHit != null && entityHit.getEntity() != null) {
            targetCenter = findGroundBelow(level, entityHit.getEntity().position());
        } else {
            BlockHitResult hitResult = level.clip(new ClipContext(
                    eyePos, traceEnd,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                if (hitResult.getDirection() == Direction.UP) {
                    BlockPos bp = hitResult.getBlockPos();
                    targetCenter = new Vec3(bp.getX() + 0.5D, bp.getY() + 1.0D, bp.getZ() + 0.5D);
                } else {
                    targetCenter = findGroundBelow(level, hitResult.getLocation());
                }
            } else {
                Vec3 forwardAirPos = eyePos.add(lookVec.scale(16.0D));
                targetCenter = findGroundBelow(level, forwardAirPos);
            }
        }

        // 2. Khởi tạo 4 bộ ma pháp trận chuyên biệt với độ phân giải cao:
        // Tầng 1: Địa Trận Thánh Vực (Y + 0.05m, Vàng Hoàng Kim 0xFFE066)
        Display.ItemDisplay groundDisplay = createItemDisplayFlat(level, targetCenter, 0.05D, 0.01F,
                ModItems.DISINTEGRATION_GROUND_ARRAY.get(), 0xFFE066);

        // Tầng 2: Nhẫn Vương Miện Lồng Trong (Y + 3.80m, Hoàng Kim Rực Sáng 0xFFD700)
        Display.ItemDisplay crownRingDisplay = createItemDisplayFlat(level, targetCenter, 3.80D, 0.01F,
                ModItems.DISINTEGRATION_CROWN_RING.get(), 0xFFD700);

        // Tầng 3: Nhẫn Cổ Ngữ Hồng Ngọc (Y + 4.20m, Hồng Ngọc / Ruby Hoàng Kim 0xFF3366)
        Display.ItemDisplay runeRingDisplay = createItemDisplayFlat(level, targetCenter, 4.20D, 0.01F,
                ModItems.DISINTEGRATION_RUNE_RING.get(), 0xFF3366);

        // Đại Pháp Luân Thánh Tích Thẳng Đứng (Y + 7.50m, Bạch Kim Hoàng Kim 0xFFFFEE)
        Display.ItemDisplay verticalCrestDisplay = createVerticalCrestDisplay(level, targetCenter, 7.50D, 0.01F, 0xFFFFEE);

        // Gieo xúc xắc xuất lực ngẫu nhiên
        SkillPowerRoll roll = SkillPowerRoll.roll();
        roll.announceAndPlayEffects(player, "Tam Trọng Thánh Giới - Linh Tử Băng Hoại");

        // Tổng thời lượng: 125 ticks (~6.25 giây, chuẩn video Dulimo / Tensura)
        ActiveSanctuary sanctuary = new ActiveSanctuary(level, player, targetCenter,
                groundDisplay, crownRingDisplay, runeRingDisplay, verticalCrestDisplay, 125, roll);
        ACTIVE_SANCTUARIES.add(sanctuary);

        // Âm thanh khởi nguyên thánh giới uy nghiêm
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.25F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 3.5F, 1.1F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 3.0F, 1.5F);

        // Khóa mục tiêu ban đầu ngay lập tức
        lockAndAnchorVictimsEveryTick(sanctuary);

        player.getCooldowns().addCooldown(sword.getItem(), 180); // 9 giây hồi chiêu
    }

    /**
     * Dò tìm mặt sàn cứng (solid block) thẳng đứng dưới điểm ngắm
     */
    private static Vec3 findGroundBelow(ServerLevel level, Vec3 pos) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(
                Math.floor(pos.x),
                Math.floor(pos.y),
                Math.floor(pos.z)
        );

        int upLimit = 0;
        while (isSolid(level, mpos) && upLimit < 10 && mpos.getY() < level.getMaxBuildHeight()) {
            mpos.move(Direction.UP);
            upLimit++;
        }

        int downLimit = 0;
        while (!isSolid(level, mpos) && downLimit < 60 && mpos.getY() > level.getMinBuildHeight()) {
            mpos.move(Direction.DOWN);
            downLimit++;
        }

        return new Vec3(pos.x, mpos.getY() + 1.0D, pos.z);
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.blocksMotion();
    }

    /**
     * Cập nhật các thánh trận đang hoạt động mỗi tick
     */
    public static void tickSanctuaries(ServerLevel serverLevel) {
        if (ACTIVE_SANCTUARIES.isEmpty()) return;

        Iterator<ActiveSanctuary> it = ACTIVE_SANCTUARIES.iterator();
        while (it.hasNext()) {
            ActiveSanctuary s = it.next();
            if (s.level != serverLevel) continue;

            s.ticksRemaining--;
            int elapsed = s.totalTicks - s.ticksRemaining;
            double groundY = s.center.y;

            // =========================================================================
            // TÍNH TOÁN TỐC ĐỘ XOAY THEO TỪNG GIAI ĐOẠN
            // =========================================================================
            float rotationSpeed = 3.5F;
            if (elapsed >= 45 && elapsed < 85) {
                rotationSpeed = 16.0F; // Giai đoạn 2: Cột sáng cực đại bùng nổ, xoay siêu tốc
            } else if (elapsed >= 85) {
                float fadeFraction = (elapsed - 85) / 40.0F;
                rotationSpeed = Math.max(1.0F, 16.0F * (1.0F - fadeFraction)); // Giai đoạn 3: Giảm tốc êm ái
            } else if (elapsed > 20) {
                rotationSpeed = 6.0F + (elapsed - 20) * 0.25F; // Giai đoạn 1: Gia tốc khi tụ năng lượng
            }
            s.currentAngleDegrees += rotationSpeed;

            // =========================================================================
            // GIAI ĐOẠN 1: KHỞI TẠO THÁNH GIỚI, LỒNG GIAM & TỤ CẦU LINH TỬ (Tick 0..44)
            // =========================================================================
            if (elapsed < 45) {
                // 1. Kích thước bung nở mượt mà của 3 tầng ma pháp trận nằm ngang
                // Địa Trận Thánh Vực (Y+0.05m): Bung nở 0 -> 12m trong 16 ticks, xoay thuận chiều
                float groundScale = Math.min(1.0F, elapsed / 16.0F) * 12.0F;
                updateFlatDisplayTransformation(s.groundDisplay, groundScale, s.currentAngleDegrees);

                // Nhẫn Vương Miện Lồng Trong (Y+3.80m): Xuất hiện từ tick 6 (Scale 7.5m), xoay NGƯỢC chiều
                float crownScale = elapsed < 6 ? 0.01F : Math.min(1.0F, (elapsed - 6) / 14.0F) * 7.5F;
                updateFlatDisplayTransformation(s.crownRingDisplay, crownScale, -s.currentAngleDegrees * 1.2F);

                // Nhẫn Cổ Ngữ Hồng Ngọc (Y+4.20m): Xuất hiện từ tick 8 (Scale 9.0m), xoay THUẬN chiều
                float runeScale = elapsed < 8 ? 0.01F : Math.min(1.0F, (elapsed - 8) / 14.0F) * 9.0F;
                updateFlatDisplayTransformation(s.runeRingDisplay, runeScale, s.currentAngleDegrees * 1.5F);

                // Đại Pháp Luân Thánh Tích Thẳng Đứng (Y+7.50m): Xuất hiện từ tick 14 (Scale 7.5m)
                float crestScale = elapsed < 14 ? 0.01F : Math.min(1.0F, (elapsed - 14) / 16.0F) * 7.5F;
                updateVerticalDisplayTransformation(s.verticalCrestDisplay, crestScale);

                // 2. Lồng Giam 12 Trụ Thánh Quang (Tick 12..44): Nối từ Địa Trận lên Nhẫn Cổ Ngữ
                if (!s.cageSpawned && elapsed >= 12) {
                    s.cageSpawned = true;
                    double cageRadius = 4.2D;
                    for (int i = 0; i < 12; i++) {
                        double theta = i * (2.0 * Math.PI / 12.0);
                        double px = s.center.x + Math.cos(theta) * cageRadius;
                        double pz = s.center.z + Math.sin(theta) * cageRadius;
                        s.cagePillars[i] = createCagePillarDisplay(s.level, px, groundY + 0.05D, pz, 0xFFEAA0);
                    }
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.6F);
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 3.5F, 1.2F);
                }

                if (s.cageSpawned) {
                    // Trụ thánh quang vọt lên từ đất lên Y+4.15m trong 8 ticks (Tick 12..20)
                    float cageHeight = Math.min(4.15F, (elapsed - 12) / 8.0F * 4.15F);
                    for (int i = 0; i < 12; i++) {
                        updateCagePillarHeight(s.cagePillars[i], cageHeight);
                        if (elapsed % 3 == 0 && s.cagePillars[i] != null) {
                            double theta = i * (2.0 * Math.PI / 12.0);
                            double px = s.center.x + Math.cos(theta) * 4.2D;
                            double pz = s.center.z + Math.sin(theta) * 4.2D;
                            s.level.sendParticles(ParticleTypes.END_ROD, px, groundY + (s.level.random.nextFloat() * cageHeight), pz, 1, 0.02D, 0.05D, 0.02D, 0.01D);
                        }
                    }
                }

                // 3. Tụ Cầu Linh Tử (Spiritron Core Orb) tại tâm Đại Pháp Luân Đứng (Y+7.50m)
                Vec3 crestCore = new Vec3(s.center.x, groundY + 7.5D, s.center.z);
                if (elapsed >= 20) {
                    float orbProgress = (elapsed - 20) / 24.0F; // 0.0 -> 1.0

                    // Quả cầu sáng rực ở tâm với tia chớp và thánh quang dày đặc
                    s.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, crestCore.x, crestCore.y, crestCore.z,
                            4 + (int) (orbProgress * 6), 0.35D, 0.35D, 0.35D, 0.08D);
                    s.level.sendParticles(ParticleTypes.END_ROD, crestCore.x, crestCore.y, crestCore.z,
                            3 + (int) (orbProgress * 4), 0.3D, 0.3D, 0.3D, 0.04D);
                    if (elapsed % 3 == 0) {
                        s.level.sendParticles(ParticleTypes.FLASH, crestCore.x, crestCore.y, crestCore.z, 1, 0.1D, 0.1D, 0.1D, 0);
                    }

                    // 6 Luồng ánh sáng xoắn ốc từ vòng ngoài hút vào quả cầu linh tử
                    for (int stream = 0; stream < 6; stream++) {
                        double spiralRad = Math.toRadians((elapsed * 22.0D + stream * 60.0D) % 360.0D);
                        double radius = 4.5D * (1.0D - orbProgress * 0.35D);
                        double sx = s.center.x + Math.cos(spiralRad) * radius;
                        double sz = s.center.z + Math.sin(spiralRad) * radius;
                        double sy = groundY + 0.5D + ((elapsed * 0.32D + stream * 1.25D) % 7.2D);
                        s.level.sendParticles(ParticleTypes.WAX_OFF, sx, sy, sz, 1, 0, 0.04D, 0, 0.01D);
                        if (stream % 2 == 0) {
                            s.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sx, sy, sz, 1, 0, 0.03D, 0, 0.02D);
                        }
                    }

                    // Âm thanh tụ năng lượng dồn dập với pitch tăng dần
                    if (elapsed % 6 == 0) {
                        float pitch = 0.85F + orbProgress * 1.1F;
                        s.level.playSound(null, crestCore.x, crestCore.y, crestCore.z,
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 2.2F, pitch);
                    }
                    if (elapsed % 12 == 0) {
                        s.level.playSound(null, crestCore.x, crestCore.y, crestCore.z,
                                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2.0F, 1.4F);
                    }
                    if (elapsed == 40) {
                        // Âm thanh nạp năng lượng cực hạn trước khi giáng thế
                        s.level.playSound(null, crestCore.x, crestCore.y, crestCore.z,
                                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3.5F, 1.3F);
                    }
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 2: CỘT SÁNG CỰC ĐẠI 3 LỚP & LINH TỬ BĂNG HOẠI (Tick 45..84 - 2 giây)
            // =========================================================================
            if (elapsed >= 45 && elapsed < 85) {
                // Khởi sinh Cột Sáng Cực Đại 3 Lớp & Vòng Sóng Xung Kích tại Tick 45
                if (!s.beamSpawned) {
                    s.beamSpawned = true;

                    // 1. Lớp Hào Quang Ngoài (Outer Aura): 11.5m x 70m x 11.5m, Vàng Kim Thánh Quang 0xFFE066
                    s.megaBeamOuter = createMegaBeamDisplay(s.level, s.center, 11.5F, 70.0F, 11.5F, 0xFFE066);

                    // 2. Lớp Thân Cột Sáng (Beam Body): 8.0m x 70m x 8.0m, Vàng Trắng Bạch Kim 0xFFFFBB
                    s.megaBeamBody = createMegaBeamDisplay(s.level, s.center, 8.0F, 70.0F, 8.0F, 0xFFFFBB);

                    // 3. Lớp Lõi Thánh Quang (Pure Core): 4.0m x 70m x 4.0m, Trắng Tinh Khiết Chói Lòa 0xFFFFFF
                    s.megaBeamCore = createMegaBeamDisplay(s.level, s.center, 4.0F, 70.0F, 4.0F, 0xFFFFFF);

                    // 4. Khởi tạo 2 vòng sóng xung kích linh tử (Shockwave Halos)
                    s.shockwaveDisplay1 = createShockwaveDisplay(s.level, s.center, 0.4D, 6.0F, 0xFFFFEE);
                    s.shockwaveDisplay2 = createShockwaveDisplay(s.level, s.center, 0.4D, 6.0F, 0xFFFFEE);

                    // BÙNG NỔ ÂM THANH THIÊN PHẠT VANG DỘI KHÔNG GIAN
                    s.level.playSound(null, s.center.x, groundY, s.center.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.5F, 0.85F);
                    s.level.playSound(null, s.center.x, groundY, s.center.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 1.1F);
                    s.level.playSound(null, s.center.x, groundY, s.center.z,
                            SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 4.0F, 0.5F);
                    s.level.playSound(null, s.center.x, groundY, s.center.z,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.5F, 0.7F);
                    s.level.playSound(null, s.center.x, groundY, s.center.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 0.6F);

                    // Chớp sáng cực đại xé rách bầu trời suốt 70 block chiều cao
                    for (double y = groundY; y <= groundY + 65.0D; y += 2.0D) {
                        s.level.sendParticles(ParticleTypes.FLASH, s.center.x, y, s.center.z, 2, 0.6D, 0.6D, 0.6D, 0);
                        s.level.sendParticles(ParticleTypes.END_ROD, s.center.x, y, s.center.z, 4, 1.5D, 0.2D, 1.5D, 0.06D);
                    }
                    s.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.center.x, groundY + 1.5D, s.center.z, 5, 0, 0, 0, 0);
                    s.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, s.center.x, groundY + 2.0D, s.center.z, 120, 4.0D, 2.5D, 4.0D, 0.4D);
                }

                // DUY TRÌ: Các tầng ma pháp trận tiếp tục xoay siêu tốc quanh cột sáng 11.5m
                updateFlatDisplayTransformation(s.groundDisplay, 12.0F, s.currentAngleDegrees);
                updateFlatDisplayTransformation(s.crownRingDisplay, 7.5F, -s.currentAngleDegrees * 1.2F);
                updateFlatDisplayTransformation(s.runeRingDisplay, 9.0F, s.currentAngleDegrees * 1.5F);
                updateVerticalDisplayTransformation(s.verticalCrestDisplay, 7.5F);

                // Cập nhật hoạt ảnh gợn sóng của 2 Vòng Sóng Xung Kích linh tử (descending shockwaves)
                int beamTick = elapsed - 45;
                updateDescendingShockwave(s.shockwaveDisplay1, s.center, groundY, (beamTick) % 18, 18, s.level);
                updateDescendingShockwave(s.shockwaveDisplay2, s.center, groundY, (beamTick + 9) % 18, 18, s.level);

                // Lồng giam 12 trụ thánh quang duy trì ánh sáng rực rỡ
                if (s.cageSpawned) {
                    for (int i = 0; i < 12; i++) {
                        updateCagePillarHeight(s.cagePillars[i], 4.15F);
                        if (elapsed % 2 == 0 && s.cagePillars[i] != null) {
                            double theta = i * (2.0 * Math.PI / 12.0);
                            double px = s.center.x + Math.cos(theta) * 4.2D;
                            double pz = s.center.z + Math.sin(theta) * 4.2D;
                            s.level.sendParticles(ParticleTypes.END_ROD, px, groundY + (s.level.random.nextFloat() * 4.15F), pz, 1, 0.02D, 0.05D, 0.02D, 0.01D);
                        }
                    }
                }

                // Triệt tiêu mọi đạn đạo bay vào cột sáng (Vùng Linh Tử Băng Hoại xóa sổ đạn đạo)
                AABB beamBox = new AABB(s.center.x - 6.0D, groundY - 1.0D, s.center.z - 6.0D,
                        s.center.x + 6.0D, groundY + 70.0D, s.center.z + 6.0D);
                List<Projectile> projectiles = s.level.getEntitiesOfClass(Projectile.class, beamBox);
                for (Projectile p : projectiles) {
                    s.level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
                    p.discard();
                }

                // Sát thương Linh Tử Băng Hoại (Thực hiện đúng 1 lần tại tick 45 khi cột giáng thế)
                if (!s.damageDealt) {
                    s.damageDealt = true;
                    executeDisintegrationStrike(s.level, s.center, 6.0D, s.caster, s.powerRoll);
                }

                // Tiếng rền của thánh quang và sóng xung kích duy trì
                if (elapsed % 10 == 0) {
                    s.level.playSound(null, s.center.x, groundY + 3.0D, s.center.z,
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 3.2F, 1.4F);
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.8F);
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 3: TIÊU BIẾN TUẦN TỰ & VÒI PHUN BỤI VÀNG THĂNG THIÊN (Tick 85..125)
            // =========================================================================
            if (elapsed >= 85) {
                // 1. Loại bỏ cột sáng 3 lớp, sóng xung kích và lồng giam ngay tại tick 85
                if (s.megaBeamOuter != null && s.megaBeamOuter.isAlive()) {
                    s.megaBeamOuter.discard();
                    s.megaBeamOuter = null;
                }
                if (s.megaBeamBody != null && s.megaBeamBody.isAlive()) {
                    s.megaBeamBody.discard();
                    s.megaBeamBody = null;
                }
                if (s.megaBeamCore != null && s.megaBeamCore.isAlive()) {
                    s.megaBeamCore.discard();
                    s.megaBeamCore = null;
                }
                if (s.shockwaveDisplay1 != null && s.shockwaveDisplay1.isAlive()) {
                    s.shockwaveDisplay1.discard();
                    s.shockwaveDisplay1 = null;
                }
                if (s.shockwaveDisplay2 != null && shockwaveDisplay2Alive(s)) {
                    s.shockwaveDisplay2.discard();
                    s.shockwaveDisplay2 = null;
                }
                for (int i = 0; i < s.cagePillars.length; i++) {
                    if (s.cagePillars[i] != null && s.cagePillars[i].isAlive()) {
                        s.cagePillars[i].discard();
                        s.cagePillars[i] = null;
                    }
                }

                if (elapsed == 85) {
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 3.5F, 1.5F);
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 3.0F, 1.2F);
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 3.0F, 1.1F);
                }

                // 2. TIÊU BIẾN TUẦN TỰ (Sequential Dissipation):
                // A. Đại Pháp Luân Đứng: Tiêu biến trước trong Tick 85..95 (10 ticks)
                if (elapsed < 95) {
                    float crestMult = Math.max(0.01F, 1.0F - (elapsed - 85) / 10.0F);
                    updateVerticalDisplayTransformation(s.verticalCrestDisplay, 7.5F * crestMult);
                } else if (s.verticalCrestDisplay != null && s.verticalCrestDisplay.isAlive()) {
                    s.verticalCrestDisplay.discard();
                    s.verticalCrestDisplay = null;
                }

                // B. Nhẫn Cổ Ngữ & Nhẫn Vương Miện: Tiêu biến kế tiếp trong Tick 95..108 (13 ticks)
                if (elapsed >= 95 && elapsed < 108) {
                    float ringMult = Math.max(0.01F, 1.0F - (elapsed - 95) / 13.0F);
                    updateFlatDisplayTransformation(s.crownRingDisplay, 7.5F * ringMult, -s.currentAngleDegrees * 1.2F);
                    updateFlatDisplayTransformation(s.runeRingDisplay, 9.0F * ringMult, s.currentAngleDegrees * 1.5F);
                } else if (elapsed >= 108) {
                    if (s.crownRingDisplay != null && s.crownRingDisplay.isAlive()) {
                        s.crownRingDisplay.discard();
                        s.crownRingDisplay = null;
                    }
                    if (s.runeRingDisplay != null && s.runeRingDisplay.isAlive()) {
                        s.runeRingDisplay.discard();
                        s.runeRingDisplay = null;
                    }
                }

                // C. Địa Trận Dưới Đất: Tiêu biến cuối cùng trong Tick 108..125 (17 ticks)
                if (elapsed >= 108 && elapsed < 125) {
                    float groundMult = Math.max(0.01F, 1.0F - (elapsed - 108) / 17.0F);
                    updateFlatDisplayTransformation(s.groundDisplay, 12.0F * groundMult, s.currentAngleDegrees);
                } else if (elapsed >= 125) {
                    if (s.groundDisplay != null && s.groundDisplay.isAlive()) {
                        s.groundDisplay.discard();
                        s.groundDisplay = null;
                    }
                }

                // 3. VÒI PHUN BỤI VÀNG LINH TỬ THĂNG THIÊN (Golden Spiritron Ascension Fountain)
                // Khớp 100% video Dulimo / Tensura: Hàng ngàn hạt bụi vàng phun trào thăng thiên lên bầu trời
                for (int p = 0; p < 18; p++) {
                    double angle = Math.toRadians(s.level.random.nextDouble() * 360.0D);
                    double dist = s.level.random.nextDouble() * 5.8D;
                    double px = s.center.x + Math.cos(angle) * dist;
                    double pz = s.center.z + Math.sin(angle) * dist;
                    double py = groundY + 0.05D + s.level.random.nextDouble() * 1.8D;

                    // Hạt bụi vàng Totem thăng thiên mạnh mẽ
                    s.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, 1,
                            (s.level.random.nextDouble() - 0.5D) * 0.03D,
                            0.28D + s.level.random.nextDouble() * 0.25D,
                            (s.level.random.nextDouble() - 0.5D) * 0.03D,
                            0.12D);

                    // Hạt sáp vàng Wax-Off lơ lửng
                    s.level.sendParticles(ParticleTypes.WAX_OFF, px, py, pz, 1,
                            (s.level.random.nextDouble() - 0.5D) * 0.02D,
                            0.22D + s.level.random.nextDouble() * 0.18D,
                            (s.level.random.nextDouble() - 0.5D) * 0.02D,
                            0.08D);

                    if (s.level.random.nextFloat() < 0.4F) {
                        s.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 1,
                                0.01D, 0.18D + s.level.random.nextDouble() * 0.15D, 0.01D, 0.05D);
                    }
                    if (s.level.random.nextFloat() < 0.25F) {
                        s.level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1,
                                0.01D, 0.20D + s.level.random.nextDouble() * 0.15D, 0.01D, 0.04D);
                    }
                }

                if (elapsed % 10 == 0) {
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.3F + ((elapsed - 85) / 40.0F) * 0.4F);
                }
            }

            // Khóa mục tiêu tuyệt đối trong suốt quá trình chuẩn bị và giáng thế (Tick 0..84)
            if (elapsed < 85) {
                lockAndAnchorVictimsEveryTick(s);
            }

            // Kết thúc hoàn toàn khi hết thời lượng 125 ticks
            if (s.ticksRemaining <= 0) {
                s.cleanupDisplays();
                it.remove();
            }
        }
    }

    private static boolean shockwaveDisplay2Alive(ActiveSanctuary s) {
        return s.shockwaveDisplay2 != null && s.shockwaveDisplay2.isAlive();
    }

    /**
     * Tạo thực thể ItemDisplay hiển thị Ma Pháp Trận nằm ngang sắc nét
     */
    private static Display.ItemDisplay createItemDisplayFlat(ServerLevel level, Vec3 center, double yOffset,
                                                             float initialScale, Item item, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(item));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F); // Tầm nhìn xa 160 blocks

            Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(initialScale, initialScale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    /**
     * Tạo Đại Pháp Luân Thánh Tích đứng thẳng sừng sững trên đỉnh (Vertical Crest Display)
     */
    private static Display.ItemDisplay createVerticalCrestDisplay(ServerLevel level, Vec3 center, double yOffset,
                                                                  float initialScale, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_VERTICAL_CREST.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            // VERTICAL billboard: Giữ huy hiệu luôn đứng thẳng và hướng trọn vẹn về phía người xem
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            Quaternionf rotation = new Quaternionf();
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(initialScale, initialScale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    /**
     * Tạo Cột Trụ Thánh Quang thuộc Lồng Giam 12 Trụ (Cage Pillar Display)
     */
    private static Display.ItemDisplay createCagePillarDisplay(ServerLevel level, double x, double y, double z, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(x, y, z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            // Bắt đầu với chiều cao nhỏ 0.01F, chân trụ tiếp đất chuẩn xác
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.005F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(0.28F, 0.01F, 0.28F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    /**
     * Cập nhật chiều cao vươn lên của Trụ Thánh Quang Lồng Giam
     */
    private static void updateCagePillarHeight(Display.ItemDisplay display, float height) {
        if (display != null && display.isAlive()) {
            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, height / 2.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(0.28F, height, 0.28F),
                    null
            ));
        }
    }

    /**
     * Tạo Cột Sáng Cực Đại giáng từ thiên đỉnh xuống đất (Mega Beam Display)
     */
    private static Display.ItemDisplay createMegaBeamDisplay(ServerLevel level, Vec3 center,
                                                             float scaleX, float scaleY, float scaleZ,
                                                             int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + 0.05D, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            // Tịnh tiến Y = scaleY / 2 để chân cột tiếp đất chuẩn xác tại mặt đất
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, scaleY / 2.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(scaleX, scaleY, scaleZ),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    /**
     * Tạo Vòng Sóng Xung Kích Linh Tử (Shockwave Halo Display)
     */
    private static Display.ItemDisplay createShockwaveDisplay(ServerLevel level, Vec3 center, double yOffset,
                                                              float initialScale, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_SHOCKWAVE.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(initialScale, initialScale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    /**
     * Cập nhật chuyển động gợn sóng từ trên cao xuống đất của Vòng Sóng Xung Kích
     */
    private static void updateDescendingShockwave(Display.ItemDisplay display, Vec3 center, double groundY,
                                                  int currentTick, int cycleTicks, ServerLevel level) {
        if (display != null && display.isAlive()) {
            float progress = (float) currentTick / (float) cycleTicks; // 0.0 -> 1.0
            float shockwaveScale = 6.0F + progress * 7.5F; // Mở rộng từ 6m -> 13.5m
            double currentY = groundY + (1.0F - progress) * 32.0D + 0.35D; // Rơi từ Y+32 xuống đất

            Quaternionf rotation = new Quaternionf()
                    .rotateX((float) Math.toRadians(90.0F))
                    .rotateZ((float) Math.toRadians(progress * 180.0F));

            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, (float) (currentY - (groundY + 0.4D)), 0.0F),
                    rotation,
                    new Vector3f(shockwaveScale, shockwaveScale, 0.01F),
                    null
            ));

            // Khi chạm đất tạo gợn sóng xung kích mặt đất
            if (currentTick == cycleTicks - 1) {
                level.sendParticles(ParticleTypes.WAX_OFF, center.x, groundY + 0.2D, center.z, 20, 3.5D, 0.1D, 3.5D, 0.05D);
                level.playSound(null, center.x, groundY + 0.5D, center.z,
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.8F, 1.6F);
            }
        }
    }

    /**
     * Cập nhật kích thước và góc xoay cho tầng ma pháp trận nằm ngang
     */
    private static void updateFlatDisplayTransformation(Display.ItemDisplay display, float scale, float angleDeg) {
        if (display != null && display.isAlive()) {
            Quaternionf rotation = new Quaternionf()
                    .rotateX((float) Math.toRadians(90.0F))
                    .rotateZ((float) Math.toRadians(angleDeg));

            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));
        }
    }

    /**
     * Cập nhật kích thước cho Đại Pháp Luân Thánh Tích đứng thẳng trên đỉnh
     */
    private static void updateVerticalDisplayTransformation(Display.ItemDisplay display, float scale) {
        if (display != null && display.isAlive()) {
            Quaternionf rotation = new Quaternionf();

            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));
        }
    }

    /**
     * Khóa mục tiêu tuyệt đối mỗi tick - Triệt tiêu di chuyển, nhảy, đẩy lùi, lơ lửng và chặn dịch chuyển
     */
    private static void lockAndAnchorVictimsEveryTick(ActiveSanctuary s) {
        double radius = 5.8D;
        AABB box = new AABB(
                s.center.x - radius, s.center.y - 2.0D, s.center.z - radius,
                s.center.x + radius, s.center.y + 9.5D, s.center.z + radius
        );

        List<LivingEntity> inArea = s.level.getEntitiesOfClass(LivingEntity.class, box, e -> e != s.caster && e.isAlive());
        for (LivingEntity e : inArea) {
            double distSq = e.position().distanceToSqr(s.center);
            if (distSq <= radius * radius) {
                s.trappedVictimUuids.add(e.getUUID());
            }
        }

        if (s.trappedVictimUuids.isEmpty()) return;

        Vec3 lockCenter = new Vec3(s.center.x, s.center.y + 1.8D, s.center.z);
        Iterator<UUID> uuidIt = s.trappedVictimUuids.iterator();
        while (uuidIt.hasNext()) {
            UUID uuid = uuidIt.next();
            Entity entity = s.level.getEntity(uuid);
            if (!(entity instanceof LivingEntity victim) || !victim.isAlive() || victim.isRemoved()) {
                continue;
            }

            Vec3 victimPos = victim.position();
            double horizontalDist = Math.sqrt(
                    Math.pow(victimPos.x - s.center.x, 2) + Math.pow(victimPos.z - s.center.z, 2)
            );
            double verticalDist = Math.abs(victimPos.y - (s.center.y + 1.8D));

            // Nếu xuất lực thấp (hụt lực), chỉ làm chậm nhẹ chứ không giam giữ đơ cứng hoàn toàn
            if (s.powerRoll.isLow()) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false, true));
                Vec3 pullVec = lockCenter.subtract(victimPos);
                if (pullVec.length() > 0.5D) {
                    victim.setDeltaMovement(pullVec.normalize().scale(0.12D));
                    victim.hasImpulse = true;
                }
                continue;
            }

            // CHỐNG ĐÀO THOÁT BẰNG DỊCH CHUYỂN TỨC THỜI (Anti-Teleport Snap-Back)
            if (horizontalDist > 5.8D || verticalDist > 4.5D) {
                victim.teleportTo(lockCenter.x, lockCenter.y, lockCenter.z);
                victim.setDeltaMovement(0, 0, 0);
                s.level.sendParticles(ParticleTypes.REVERSE_PORTAL, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.05D);
            } else {
                Vec3 pullVec = lockCenter.subtract(victimPos);
                double pullDist = pullVec.length();
                if (pullDist > 0.15D) {
                    victim.setDeltaMovement(pullVec.normalize().scale(Math.min(pullDist * 0.3D, 0.35D)));
                } else {
                    victim.setDeltaMovement(0, 0, 0);
                }
            }

            victim.hasImpulse = true;
            victim.fallDistance = 0.0F;

            // Áp dụng các hiệu ứng phong ấn toàn diện
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, -255, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 255, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 255, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, true));

            if (victim instanceof ServerPlayer playerVictim) {
                playerVictim.getCooldowns().addCooldown(Items.ENDER_PEARL, 40);
                playerVictim.getCooldowns().addCooldown(Items.CHORUS_FRUIT, 40);
                playerVictim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, false));
            }
        }
    }

    /**
     * Cột Thiên Phạt Linh Tử Băng Hoại giáng xuống xóa sổ toàn bộ mục tiêu theo cấp độ xuất lực
     */
    private static void executeDisintegrationStrike(ServerLevel level, Vec3 center, double radius, ServerPlayer caster, SkillPowerRoll roll) {
        AABB box = new AABB(
                center.x - radius, center.y - 2.0D, center.z - radius,
                center.x + radius, center.y + 14.0D, center.z + radius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster);
        List<String> victimNames = new ArrayList<>();
        int destroyedCount = 0;
        DamageSource dmgSource = (caster != null) ? level.damageSources().playerAttack(caster) : level.damageSources().genericKill();

        for (LivingEntity target : targets) {
            double distSq = target.position().distanceToSqr(center);
            if (distSq <= radius * radius) {
                victimNames.add("cá thể " + target.getDisplayName().getString());
                if (caster != null) {
                    com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(caster, target);
                }
                level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0D, target.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.02D);

                target.addTag("DisintegrationDamage");
                if (roll != null && roll.isOverdrive()) {
                    // BẠO KÍCH CỰC HẠN (20%): Phân rã 100% linh tử, tất sát cả Boss
                    target.hurt(dmgSource, 100000.0F);
                    if (target.isAlive()) {
                        target.discard();
                    }
                } else if (roll != null && roll.isNormal()) {
                    // XUẤT LỰC CHUẨN (50%): 1,200 sát thương * multiplier, quái thường bốc hơi, Boss rút máu nặng
                    float damage = 1200.0F * roll.multiplier;
                    target.hurt(dmgSource, damage);
                } else {
                    // ĐẦU RA THẤP (30%): 350 sát thương * multiplier, làm choáng và thiêu đốt
                    float damage = 350.0F * (roll != null ? roll.multiplier : 0.35F);
                    target.hurt(dmgSource, damage);
                    target.setRemainingFireTicks(120);
                }
                target.removeTag("DisintegrationDamage");
                destroyedCount++;
            }
        }

        // Tiêu biến các item rớt đất trong vùng phân rã linh tử
        List<ItemEntity> droppedItems = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.position().distanceToSqr(center) <= radius * radius);
        for (ItemEntity item : droppedItems) {
            item.discard();
        }

        if (caster != null) {
            String killMessage;
            if (destroyedCount == 1) {
                killMessage = "Báo cáo: Cá thể " + victimNames.get(0).substring(7) + " đã bị phân rã hoàn toàn bởi Linh Tử Băng Hoại! ⚡✨";
            } else if (destroyedCount > 1 && destroyedCount <= 3) {
                killMessage = "Báo cáo: " + String.join(", ", victimNames) + " đã bị phân rã bởi Linh Tử Băng Hoại! ⚡✨";
            } else if (destroyedCount > 3) {
                killMessage = "Báo cáo: " + victimNames.get(0) + " và " + (destroyedCount - 1) + " cá thể khác đã bị phân rã! ⚡✨";
            } else {
                killMessage = "Báo cáo: Không có cá thể nào trong phạm vi thánh trận.";
            }
            caster.displayClientMessage(
                    Component.literal("§e§l[TAM TRỌNG THÁNH GIỚI] §f" + killMessage),
                    true
            );
        }
    }
}
