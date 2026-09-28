package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Skill 1 Chung: Tà Khứ Vũ Thê Tử (Death Streak / Nuclear Magic)
 * - Tái hiện chuẩn xác độ tráng lệ và hùng vĩ của Tam Trọng Thánh Giới - Linh Tử Băng Hoại (Sanctuary Disintegration),
 *   nhưng với QUY MÔ VƯỢT TRỘI (Bán kính 18m, Đường kính 36m) và 5 TẦNG MA PHÁP TRẬN CHUYÊN BIỆT:
 *     1. Đại Địa Trận Ma Quỷ (Ground Array - Mặt đất Y+0.05m, Đường kính 36m).
 *     2. Nhẫn Ma Pháp Hạ Tầng (Lower Ring - Y+4.5m, Đường kính 24m).
 *     3. Nhẫn Cổ Ngữ Trung Tầng (Middle Rune Ring - Y+9.5m, Đường kính 18m).
 *     4. Nhẫn Vương Miện Thượng Tầng (Upper Crown Ring - Y+15.0m, Đường kính 12m).
 *     5. Đại Pháp Luân Vực Thẳm Thẳng Đứng (Vertical Sacred Crest - Y+22.0m, Đường kính 16m) đứng sừng sững trên đỉnh.
 * - 16 Cột Trụ Hào Quang Lồng Giam Vực Thẳm giam giữ triệt để mọi sinh vật.
 * - Cột Sáng Cực Đại 3 Lớp (Outer 26m, Body 16m, Core 8m, cao 120m) từ thiên không giáng xuống.
 * - Sóng xung kích phân rã (Shockwave Halos) liên tục cuộn xuống.
 * - Chưa là Ma Vương: Tuyệt đối KHÔNG phá hủy block.
 * - Đã là Ma Vương: Phá hủy toàn bộ block trong phạm vi 18 block tạo hố sâu hoang tàn!
 */
public class DeathStreakAbility {

    public static class ActiveDeathStreak {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public final DemonType demonType;
        public final boolean isDemonLord;
        public final int glowColor;

        // 5 Tầng Ma Pháp Trận
        public Display.ItemDisplay circleGround;
        public Display.ItemDisplay circleLower;
        public Display.ItemDisplay circleMiddle;
        public Display.ItemDisplay circleUpper;
        public Display.ItemDisplay circleVertical;

        // 16 Cột Trụ Hào Quang Lồng Giam
        public final Display.ItemDisplay[] cagePillars = new Display.ItemDisplay[16];
        public boolean cageSpawned = false;

        // Cột Sáng Cực Đại 3 Lớp
        public Display.ItemDisplay megaBeamOuter;
        public Display.ItemDisplay megaBeamBody;
        public Display.ItemDisplay megaBeamCore;

        // 2 Vòng Sóng Xung Kích
        public Display.ItemDisplay shockwave1;
        public Display.ItemDisplay shockwave2;

        public int ticksRemaining;
        public final int totalTicks; // 125 ticks (~6.25 giây)
        public float currentAngleDegrees;
        public boolean beamTriggered = false;
        public boolean damageDealt = false;
        public final Set<UUID> trappedVictimUuids = new HashSet<>();

        public ActiveDeathStreak(ServerLevel level, ServerPlayer caster, Vec3 center, DemonType demonType,
                                 boolean isDemonLord, int glowColor,
                                 Display.ItemDisplay circleGround,
                                 Display.ItemDisplay circleLower,
                                 Display.ItemDisplay circleMiddle,
                                 Display.ItemDisplay circleUpper,
                                 Display.ItemDisplay circleVertical,
                                 int totalTicks) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.demonType = demonType;
            this.isDemonLord = isDemonLord;
            this.glowColor = glowColor;
            this.circleGround = circleGround;
            this.circleLower = circleLower;
            this.circleMiddle = circleMiddle;
            this.circleUpper = circleUpper;
            this.circleVertical = circleVertical;
            this.ticksRemaining = totalTicks;
            this.totalTicks = totalTicks;
            this.currentAngleDegrees = 0.0F;
        }

        public void cleanupDisplays() {
            if (circleGround != null && circleGround.isAlive()) circleGround.discard();
            if (circleLower != null && circleLower.isAlive()) circleLower.discard();
            if (circleMiddle != null && circleMiddle.isAlive()) circleMiddle.discard();
            if (circleUpper != null && circleUpper.isAlive()) circleUpper.discard();
            if (circleVertical != null && circleVertical.isAlive()) circleVertical.discard();

            for (int i = 0; i < cagePillars.length; i++) {
                if (cagePillars[i] != null && cagePillars[i].isAlive()) {
                    cagePillars[i].discard();
                    cagePillars[i] = null;
                }
            }

            if (megaBeamOuter != null && megaBeamOuter.isAlive()) megaBeamOuter.discard();
            if (megaBeamBody != null && megaBeamBody.isAlive()) megaBeamBody.discard();
            if (megaBeamCore != null && megaBeamCore.isAlive()) megaBeamCore.discard();

            if (shockwave1 != null && shockwave1.isAlive()) shockwave1.discard();
            if (shockwave2 != null && shockwave2.isAlive()) shockwave2.discard();
        }
    }

    private static final List<ActiveDeathStreak> ACTIVE_STREAKS = new ArrayList<>();

    public static boolean destroyCircleIfMatches(Display.ItemDisplay display) {
        if (display == null) return false;
        for (Iterator<ActiveDeathStreak> it = ACTIVE_STREAKS.iterator(); it.hasNext(); ) {
            ActiveDeathStreak s = it.next();
            if (s.circleGround == display || s.circleLower == display || s.circleMiddle == display ||
                s.circleUpper == display || s.circleVertical == display ||
                s.megaBeamOuter == display || s.megaBeamBody == display || s.megaBeamCore == display ||
                s.shockwave1 == display || s.shockwave2 == display) {
                s.cleanupDisplays();
                it.remove();
                return true;
            }
            for (int i = 0; i < s.cagePillars.length; i++) {
                if (s.cagePillars[i] == display) {
                    s.cleanupDisplays();
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    public static Item getCircleItem(DemonType type) {
        if (type == null) return ModItems.MAGIC_CIRCLE_NOIR.get();
        return switch (type) {
            case ROUGE -> ModItems.MAGIC_CIRCLE_ROUGE.get();
            case NOIR -> ModItems.MAGIC_CIRCLE_NOIR.get();
            case BLANC -> ModItems.MAGIC_CIRCLE_BLANC.get();
            case JAUNE -> ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get();
            case VIOLET -> ModItems.MAGIC_CIRCLE_VIOLET.get();
            case BLEU -> ModItems.MAGIC_CIRCLE_BLEU.get();
            case VERT -> ModItems.MAGIC_CIRCLE_VERT.get();
        };
    }

    public static int getGlowColor(DemonType type) {
        if (type == null) return 0x9900FF;
        return switch (type) {
            case ROUGE -> 0xFF2200; // Đỏ thẫm Hỏa Ngục
            case NOIR -> 0x8800FF;  // Hư không Tím Đen
            case BLANC -> 0xFFFFFF; // Bạch quang tinh khiết
            case JAUNE -> 0xFFD700; // Hoàng Kim Hạt Nhân
            case VIOLET -> 0xAA00FF;// Tím Ma Pháp Hoàng Gia
            case BLEU -> 0x00E5FF;  // Lam Băng Tinh
            case VERT -> 0x00FF66;  // Lục Bảo Tinh
        };
    }

    public static ParticleOptions getPrimaryParticle(DemonType type) {
        if (type == null) return ParticleTypes.PORTAL;
        return switch (type) {
            case ROUGE -> ParticleTypes.FLAME;
            case NOIR -> ParticleTypes.PORTAL;
            case BLANC -> ParticleTypes.END_ROD;
            case JAUNE -> ParticleTypes.ELECTRIC_SPARK;
            case VIOLET -> ParticleTypes.WITCH;
            case BLEU -> ParticleTypes.SOUL_FIRE_FLAME;
            case VERT -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    public static void cast(ServerLevel level, ServerPlayer player) {
        DemonType type = PrimordialPlayerDataHelper.getPrimordialType(player);
        if (type == null) return;

        boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(player);

        // 1. Dò tìm mục tiêu: Ưu tiên entity trong tầm nhìn hoặc mặt đất
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        double maxDist = 32.0D;
        Vec3 traceEnd = eyePos.add(lookVec.scale(maxDist));

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, traceEnd,
                new AABB(eyePos, traceEnd).inflate(2.5D),
                e -> !e.isSpectator() && e.isPickable() && e != player,
                maxDist * maxDist
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

        Item circleItem = getCircleItem(type);
        int glowColor = getGlowColor(type);

        // 2. Khởi tạo 5 TẦNG MA PHÁP TRẬN CHUYÊN BIỆT
        // Tầng 1: Đại Địa Trận Ma Quỷ (Mặt đất Y+0.05m)
        Display.ItemDisplay circleGround = createItemDisplayFlat(level, targetCenter, 0.05D, 0.01F, circleItem, glowColor);

        // Tầng 2: Nhẫn Ma Pháp Hạ Tầng (Y+4.5m)
        Display.ItemDisplay circleLower = createItemDisplayFlat(level, targetCenter, 4.5D, 0.01F, circleItem, glowColor);

        // Tầng 3: Nhẫn Cổ Ngữ Trung Tầng (Y+9.5m)
        Display.ItemDisplay circleMiddle = createItemDisplayFlat(level, targetCenter, 9.5D, 0.01F, circleItem, glowColor);

        // Tầng 4: Nhẫn Vương Miện Thượng Tầng (Y+15.0m)
        Display.ItemDisplay circleUpper = createItemDisplayFlat(level, targetCenter, 15.0D, 0.01F, circleItem, glowColor);

        // Tầng 5: Đại Pháp Luân Vực Thẳm Thẳng Đứng (Y+22.0m, đứng sừng sững trên đỉnh)
        Display.ItemDisplay circleVertical = createVerticalCrestDisplay(level, targetCenter, 22.0D, 0.01F, circleItem, glowColor);

        ActiveDeathStreak streak = new ActiveDeathStreak(
                level, player, targetCenter, type, isDemonLord, glowColor,
                circleGround, circleLower, circleMiddle, circleUpper, circleVertical, 125
        );
        ACTIVE_STREAKS.add(streak);

        // Âm thanh uy nghiêm vang rền
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.5F, 0.7F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 4.0F, 0.6F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 3.5F, 0.8F);

        String demonVi = PrimordialPlayerDataHelper.getDemonTitleVi(type);
        player.displayClientMessage(
                Component.literal("§6§l✦ THẦN CHÚ THỦY TỔ ✦ §e" + demonVi + " §cđang khai mở Ma Trận 5 Tầng: §4§lTÀ KHỨ VŨ THÊ TỬ (DEATH STREAK)!"),
                true
        );

        lockAndAnchorVictims(streak);
    }

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

    public static void tickStreaks(ServerLevel serverLevel) {
        if (ACTIVE_STREAKS.isEmpty()) return;

        Iterator<ActiveDeathStreak> it = ACTIVE_STREAKS.iterator();
        while (it.hasNext()) {
            ActiveDeathStreak s = it.next();
            if (s.level != serverLevel) continue;

            s.ticksRemaining--;
            int elapsed = s.totalTicks - s.ticksRemaining;
            double groundY = s.center.y;

            // Tốc độ xoay theo từng giai đoạn
            float rotationSpeed = 4.0F;
            if (elapsed >= 45 && elapsed < 85) {
                rotationSpeed = 18.0F; // Giai đoạn 2: Cột sáng cực đại bùng nổ, xoay siêu tốc
            } else if (elapsed >= 85) {
                float fadeFraction = (elapsed - 85) / 40.0F;
                rotationSpeed = Math.max(1.0F, 18.0F * (1.0F - fadeFraction)); // Giai đoạn 3: Giảm tốc êm ái
            } else if (elapsed > 20) {
                rotationSpeed = 6.0F + (elapsed - 20) * 0.35F;
            }
            s.currentAngleDegrees += rotationSpeed;

            // =========================================================================
            // GIAI ĐOẠN 1: KHỞI TẠO 5 TẦNG MA PHÁP TRẬN & LỒNG GIAM 16 TRỤ (Tick 0..44)
            // =========================================================================
            if (elapsed < 45) {
                // Bung nở mượt mà của 4 tầng ma pháp trận nằm ngang:
                // Tầng 1: Địa Trận Ma Quỷ (Đường kính 36m), xoay thuận chiều
                float groundScale = Math.min(1.0F, elapsed / 16.0F) * 36.0F;
                updateFlatDisplayTransformation(s.circleGround, groundScale, s.currentAngleDegrees);

                // Tầng 2: Nhẫn Ma Pháp Hạ Tầng (Đường kính 24m), xoay ngược chiều
                float lowerScale = elapsed < 4 ? 0.01F : Math.min(1.0F, (elapsed - 4) / 14.0F) * 24.0F;
                updateFlatDisplayTransformation(s.circleLower, lowerScale, -s.currentAngleDegrees * 1.2F);

                // Tầng 3: Nhẫn Cổ Ngữ Trung Tầng (Đường kính 18m), xoay thuận chiều
                float middleScale = elapsed < 8 ? 0.01F : Math.min(1.0F, (elapsed - 8) / 14.0F) * 18.0F;
                updateFlatDisplayTransformation(s.circleMiddle, middleScale, s.currentAngleDegrees * 1.5F);

                // Tầng 4: Nhẫn Vương Miện Thượng Tầng (Đường kính 12m), xoay ngược chiều
                float upperScale = elapsed < 12 ? 0.01F : Math.min(1.0F, (elapsed - 12) / 14.0F) * 12.0F;
                updateFlatDisplayTransformation(s.circleUpper, upperScale, -s.currentAngleDegrees * 1.8F);

                // Tầng 5: Đại Pháp Luân Vực Thẳm Thẳng Đứng (Đường kính 16m sừng sững trên đỉnh)
                float crestScale = elapsed < 14 ? 0.01F : Math.min(1.0F, (elapsed - 14) / 16.0F) * 16.0F;
                updateVerticalDisplayTransformation(s.circleVertical, crestScale);

                // Lồng Giam 16 Trụ Hào Quang (Tick 10..44): Bán kính 16.5m bao bọc toàn bộ trận địa
                if (!s.cageSpawned && elapsed >= 10) {
                    s.cageSpawned = true;
                    double cageRadius = 16.5D;
                    for (int i = 0; i < 16; i++) {
                        double theta = i * (2.0 * Math.PI / 16.0);
                        double px = s.center.x + Math.cos(theta) * cageRadius;
                        double pz = s.center.z + Math.sin(theta) * cageRadius;
                        s.cagePillars[i] = createCagePillarDisplay(s.level, px, groundY + 0.05D, pz, s.glowColor);
                    }
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.4F);
                    s.level.playSound(null, s.center.x, groundY + 2.0D, s.center.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 4.0F, 1.0F);
                }

                if (s.cageSpawned) {
                    float cageHeight = Math.min(16.0F, (elapsed - 10) / 10.0F * 16.0F);
                    for (int i = 0; i < 16; i++) {
                        updateCagePillarHeight(s.cagePillars[i], cageHeight);
                    }
                }

                // Tụ năng lượng xoắn ốc hội tụ về quả cầu linh hồn tại đỉnh (Y+22m)
                ParticleOptions primParticle = getPrimaryParticle(s.demonType);
                double orbY = groundY + 22.0D;
                for (int arm = 0; arm < 4; arm++) {
                    double theta = (elapsed * 18.0D + arm * 90.0D) * Math.PI / 180.0D;
                    double r = Math.max(0.5D, 14.0D * (1.0D - (elapsed / 45.0D)));
                    double y = orbY + Math.sin(elapsed * 0.25D + arm) * 2.0D;
                    s.level.sendParticles(primParticle, s.center.x + Math.cos(theta) * r, y, s.center.z + Math.sin(theta) * r,
                            2, 0.05D, 0.05D, 0.05D, 0.02D);
                }

                // Khóa chặt các mục tiêu trong phạm vi 18m
                lockAndAnchorVictims(s);
            }

            // =========================================================================
            // GIAI ĐOẠN 2: CỘT SÁNG CỰC ĐẠI 3 LỚP TỪ THIÊN ĐỈNH GIÁNG XUỐNG (Tick 45..84)
            // =========================================================================
            if (elapsed >= 45 && elapsed < 85) {
                if (!s.beamTriggered) {
                    s.beamTriggered = true;

                    // 1. Tạo Cột Sáng Cực Đại 3 Lớp từ trời giáng xuống
                    float beamHeight = 120.0F;
                    s.megaBeamOuter = createMegaBeamDisplay(s.level, s.center, 26.0F, beamHeight, 26.0F, s.glowColor);
                    s.megaBeamBody = createMegaBeamDisplay(s.level, s.center, 16.0F, beamHeight, 16.0F, s.glowColor);
                    s.megaBeamCore = createMegaBeamDisplay(s.level, s.center, 8.0F, beamHeight, 8.0F, 0xFFFFFF);

                    // 2. Tạo 2 Vòng Sóng Xung Kích linh tử cuộn xuống
                    s.shockwave1 = createShockwaveDisplay(s.level, s.center, 40.0D, 24.0F, s.glowColor);
                    s.shockwave2 = createShockwaveDisplay(s.level, s.center, 80.0D, 24.0F, s.glowColor);

                    // Âm thanh chấn thiên động địa
                    s.level.playSound(null, s.center.x, groundY + 5.0D, s.center.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 0.7F);
                    s.level.playSound(null, s.center.x, groundY + 5.0D, s.center.z,
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0F, 0.5F);
                    s.level.playSound(null, s.center.x, groundY + 5.0D, s.center.z,
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 5.0F, 0.9F);
                    s.level.playSound(null, s.center.x, groundY + 5.0D, s.center.z,
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 4.0F, 0.6F);

                    // Xử lý Phá Hủy Block:
                    // NẾU LÀ MA VƯƠNG: Phá hủy toàn bộ block trong bán kính 18 block!
                    // NẾU CHƯA LÀ MA VƯƠNG: Tuyệt đối KHÔNG phá hủy block!
                    if (s.isDemonLord) {
                        destroyBlocksInRadius(s.level, BlockPos.containing(s.center), 18);
                    }
                }

                // Cập nhật sóng xung kích rơi xuống đất
                int beamTick = elapsed - 45;
                double shock1Y = Math.max(0.1D, 50.0D - (beamTick * 2.8D));
                double shock2Y = Math.max(0.1D, 90.0D - (beamTick * 2.8D));
                updateShockwavePos(s.shockwave1, s.center, shock1Y);
                updateShockwavePos(s.shockwave2, s.center, shock2Y);

                // Thi hành sát thương phân rã liên tục
                dealContinuousDeathStreakDamage(s, elapsed);

                // Khóa chặt quái vật
                lockAndAnchorVictims(s);

                // Hiệu ứng hạt cực đại
                s.level.sendParticles(ParticleTypes.FLASH, s.center.x, groundY + 2.0D, s.center.z, 2, 3.0D, 1.0D, 3.0D, 0);
                s.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.center.x, groundY + 1.0D, s.center.z, 2, 2.0D, 1.0D, 2.0D, 0);
                ParticleOptions p = getPrimaryParticle(s.demonType);
                s.level.sendParticles(p, s.center.x, groundY + 4.0D, s.center.z, 40, 8.0D, 12.0D, 8.0D, 0.15D);
            }

            // =========================================================================
            // GIAI ĐOẠN 3: TIÊU BIẾN & VÒI PHUN BỤI NGUYÊN THỦY THĂNG THIÊN (Tick 85..125)
            // =========================================================================
            if (elapsed >= 85) {
                // Biến mất cột sáng và lồng giam
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
                if (s.shockwave1 != null && s.shockwave1.isAlive()) {
                    s.shockwave1.discard();
                    s.shockwave1 = null;
                }
                if (s.shockwave2 != null && s.shockwave2.isAlive()) {
                    s.shockwave2.discard();
                    s.shockwave2 = null;
                }
                for (int i = 0; i < s.cagePillars.length; i++) {
                    if (s.cagePillars[i] != null && s.cagePillars[i].isAlive()) {
                        s.cagePillars[i].discard();
                        s.cagePillars[i] = null;
                    }
                }

                // Tiêu biến tuần tự các ma trận
                float fade = (elapsed - 85) / 40.0F; // 0.0 -> 1.0
                float curScale = Math.max(0.01F, 36.0F * (1.0F - fade));
                updateFlatDisplayTransformation(s.circleGround, curScale, s.currentAngleDegrees);
                updateFlatDisplayTransformation(s.circleLower, Math.max(0.01F, 24.0F * (1.0F - fade)), -s.currentAngleDegrees * 1.2F);
                updateFlatDisplayTransformation(s.circleMiddle, Math.max(0.01F, 18.0F * (1.0F - fade)), s.currentAngleDegrees * 1.5F);
                updateFlatDisplayTransformation(s.circleUpper, Math.max(0.01F, 12.0F * (1.0F - fade)), -s.currentAngleDegrees * 1.8F);
                updateVerticalDisplayTransformation(s.circleVertical, Math.max(0.01F, 16.0F * (1.0F - fade)));

                // Vòi phun hạt nguyên thủy thăng thiên lên trời
                ParticleOptions p = getPrimaryParticle(s.demonType);
                for (int k = 0; k < 12; k++) {
                    double rx = (s.level.random.nextDouble() - 0.5D) * 16.0D;
                    double rz = (s.level.random.nextDouble() - 0.5D) * 16.0D;
                    s.level.sendParticles(p, s.center.x + rx, groundY + 0.5D, s.center.z + rz,
                            1, 0, 0.6D + s.level.random.nextDouble() * 0.4D, 0, 0.2D);
                }
            }

            // Kết thúc
            if (s.ticksRemaining <= 0) {
                s.cleanupDisplays();
                it.remove();
            }
        }
    }

    private static void lockAndAnchorVictims(ActiveDeathStreak s) {
        AABB box = new AABB(s.center.x - 18.0D, s.center.y - 4.0D, s.center.z - 18.0D,
                s.center.x + 18.0D, s.center.y + 35.0D, s.center.z + 18.0D);
        List<LivingEntity> victims = s.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != s.caster);

        for (LivingEntity v : victims) {
            s.trappedVictimUuids.add(v.getUUID());
            // Kéo dần về tâm và làm chậm bất động
            Vec3 toCenter = s.center.subtract(v.position()).multiply(1, 0, 1);
            if (toCenter.lengthSqr() > 1.0D) {
                v.setDeltaMovement(toCenter.normalize().scale(0.12D).add(0, -0.05D, 0));
                v.hasImpulse = true;
            }
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, false, false, false));
        }
    }

    private static void dealContinuousDeathStreakDamage(ActiveDeathStreak s, int elapsed) {
        AABB box = new AABB(s.center.x - 18.0D, s.center.y - 6.0D, s.center.z - 18.0D,
                s.center.x + 18.0D, s.center.y + 60.0D, s.center.z + 18.0D);
        List<LivingEntity> targets = s.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != s.caster);

        boolean hasBody = PrimordialPlayerDataHelper.hasPhysicalBody(s.caster);
        boolean isDemonLord = s.isDemonLord;

        for (LivingEntity victim : targets) {
            victim.removeEffect(MobEffects.REGENERATION);
            victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            victim.removeEffect(MobEffects.FIRE_RESISTANCE);

            boolean isBoss = (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                    || (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                    || (victim instanceof net.minecraft.world.entity.monster.warden.Warden)
                    || (victim instanceof net.minecraft.world.entity.animal.IronGolem)
                    || (victim instanceof net.minecraft.world.entity.monster.ElderGuardian);

            if (elapsed == 45) {
                // Đòn đầu tiên
                if (hasBody || isDemonLord) {
                    if (isBoss) {
                        victim.hurt(s.caster.damageSources().magic(), victim.getMaxHealth() * 0.55F);
                    } else if (!(victim instanceof VelgryndEntity)) {
                        victim.hurt(s.caster.damageSources().magic(), victim.getMaxHealth() * 4.0F);
                    }
                } else {
                    victim.hurt(s.caster.damageSources().magic(), isBoss ? 80.0F : 180.0F);
                }
            } else if (elapsed == 75) {
                // Đòn bồi thứ hai (tất sát kết liễu Boss nếu là Ma Vương hoặc có thể xác)
                if (hasBody || isDemonLord) {
                    if (isBoss) {
                        victim.hurt(s.caster.damageSources().magic(), victim.getMaxHealth() * 0.60F);
                        if (victim.isAlive() && !(victim instanceof VelgryndEntity)) {
                            victim.hurt(s.caster.damageSources().magic(), 100000.0F);
                        }
                    } else if (!(victim instanceof VelgryndEntity)) {
                        victim.hurt(s.caster.damageSources().magic(), 100000.0F);
                    }
                } else {
                    victim.hurt(s.caster.damageSources().magic(), isBoss ? 80.0F : 180.0F);
                }
            } else if (elapsed % 4 == 0) {
                // Sát thương phân rã liên tục mỗi 4 ticks
                victim.hurt(s.caster.damageSources().magic(), (hasBody || isDemonLord) ? 40.0F : 15.0F);
            }
        }
    }

    private static void destroyBlocksInRadius(ServerLevel level, BlockPos center, int radius) {
        int rSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > rSq) continue;
                for (int dy = -4; dy <= 12; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir() && st.getBlock() != Blocks.BEDROCK) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

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
            displayAcc.weapons$setViewRange(12.0F);

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

    private static Display.ItemDisplay createVerticalCrestDisplay(ServerLevel level, Vec3 center, double yOffset,
                                                                  float initialScale, Item item, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(item));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(12.0F);

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
            displayAcc.weapons$setViewRange(12.0F);

            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.005F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(0.35F, 0.01F, 0.35F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    private static void updateCagePillarHeight(Display.ItemDisplay display, float height) {
        if (display != null && display.isAlive()) {
            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, height / 2.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(0.35F, height, 0.35F),
                    null
            ));
        }
    }

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
            displayAcc.weapons$setViewRange(12.0F);

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
            displayAcc.weapons$setViewRange(12.0F);

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

    private static void updateFlatDisplayTransformation(Display.ItemDisplay display, float scale, float angleDegrees) {
        if (display != null && display.isAlive()) {
            Quaternionf rotation = new Quaternionf()
                    .rotateX((float) Math.toRadians(90.0F))
                    .rotateZ((float) Math.toRadians(angleDegrees));
            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));
        }
    }

    private static void updateVerticalDisplayTransformation(Display.ItemDisplay display, float scale) {
        if (display != null && display.isAlive()) {
            ((DisplayAccessor) display).weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));
        }
    }

    private static void updateShockwavePos(Display.ItemDisplay display, Vec3 center, double yOffset) {
        if (display != null && display.isAlive()) {
            display.setPos(center.x, center.y + yOffset, center.z);
        }
    }
}
