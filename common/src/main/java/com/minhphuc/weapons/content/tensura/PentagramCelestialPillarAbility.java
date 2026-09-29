package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tuyệt Kỹ Mới: Ngũ Trọng Ma Trận - Cột Sáng Thiên Khấu (Pentagram Celestial Pillar)
 * Đặc tính:
 * - 5 Tầng Vòng Tròn Ma Thuật có kích thước khác nhau được XẾP DỌC THEO ĐỘ CAO (Y) giống Linh Tử Băng Hoại.
 * - Tầng đáy cực đại rộng tới 28-30 block!
 * - Cột sáng thánh quang/ma pháp cực đại từ thiên không giáng xuyên suốt qua tâm cả 5 tầng ma trận xếp dọc.
 * - Dùng cho Skill mới và dùng làm hiệu ứng xuất hiện khi Thức Tỉnh Biến Thân thành Ác Ma.
 */
public class PentagramCelestialPillarAbility {

    public static class ActiveCelestialPillar {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public final DemonType demonType;
        public final boolean isDemonLord;
        public final boolean isPureVisual; // Dành cho lúc biến thân (không gây sát thương phá hoại)

        // 5 Tầng ma trận xếp DỌC theo độ cao (Y)
        public Display.ItemDisplay tier1Bottom;  // Y + 0.10m: CỰC LỚN 28 block
        public Display.ItemDisplay tier2Lower;   // Y + 4.80m: 20 block
        public Display.ItemDisplay tier3Middle;  // Y + 10.50m: 14 block
        public Display.ItemDisplay tier4Upper;   // Y + 16.50m: 9 block
        public Display.ItemDisplay tier5Top;     // Y + 23.50m: 5.5 block

        // Cột sáng cực đại chọc trời
        public Display.ItemDisplay lightBeam;

        public int ticksRemaining;
        public final int totalTicks; // 80 ticks (~4 giây)
        public float rotationAngle;
        public boolean beamTriggered = false;

        public ActiveCelestialPillar(ServerLevel level, ServerPlayer caster, Vec3 center, DemonType demonType,
                                     boolean isDemonLord, boolean isPureVisual,
                                     Display.ItemDisplay t1, Display.ItemDisplay t2, Display.ItemDisplay t3,
                                     Display.ItemDisplay t4, Display.ItemDisplay t5,
                                     int totalTicks) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.demonType = demonType;
            this.isDemonLord = isDemonLord;
            this.isPureVisual = isPureVisual;
            this.tier1Bottom = t1;
            this.tier2Lower = t2;
            this.tier3Middle = t3;
            this.tier4Upper = t4;
            this.tier5Top = t5;
            this.ticksRemaining = totalTicks;
            this.totalTicks = totalTicks;
        }

        public void cleanup() {
            if (tier1Bottom != null && tier1Bottom.isAlive()) tier1Bottom.discard();
            if (tier2Lower != null && tier2Lower.isAlive()) tier2Lower.discard();
            if (tier3Middle != null && tier3Middle.isAlive()) tier3Middle.discard();
            if (tier4Upper != null && tier4Upper.isAlive()) tier4Upper.discard();
            if (tier5Top != null && tier5Top.isAlive()) tier5Top.discard();
            if (lightBeam != null && lightBeam.isAlive()) lightBeam.discard();
        }
    }

    private static final List<ActiveCelestialPillar> ACTIVE_PILLARS = new ArrayList<>();

    public static boolean destroyCircleIfMatches(Display.ItemDisplay display) {
        if (display == null) return false;
        for (Iterator<ActiveCelestialPillar> it = ACTIVE_PILLARS.iterator(); it.hasNext(); ) {
            ActiveCelestialPillar p = it.next();
            if (p.tier1Bottom == display || p.tier2Lower == display || p.tier3Middle == display ||
                p.tier4Upper == display || p.tier5Top == display || p.lightBeam == display) {
                p.cleanup();
                it.remove();
                return true;
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

    /**
     * Kích hoạt kỹ năng chiến đấu của Ác Ma Thủy Tổ
     */
    public static void cast(ServerLevel level, ServerPlayer player) {
        DemonType type = PrimordialPlayerDataHelper.getPrimordialType(player);
        if (type == null) return;

        boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(player);

        Vec3 look = player.getLookAngle();
        Vec3 target = player.position().add(look.x * 14.0D, 0, look.z * 14.0D);

        BlockPos targetPos = BlockPos.containing(target);
        while (level.getBlockState(targetPos).isAir() && targetPos.getY() > level.getMinBuildHeight() + 2) {
            targetPos = targetPos.below();
        }
        Vec3 center = new Vec3(target.x, targetPos.getY() + 1.05D, target.z);

        spawnPillarAt(level, player, center, type, isDemonLord, false);

        String demonVi = PrimordialPlayerDataHelper.getDemonTitleVi(type);
        player.displayClientMessage(
                Component.literal("§6§l✦ THẦN CHÚ THỦY TỔ ✦ §e" + demonVi + " §bđang khai mở §d§lNGŨ TRỌNG MA TRẬN CỘT SÁNG THIÊN KHẤU!"),
                true
        );
    }

    /**
     * Triệu hồi Cột Sáng 5 Tầng Xếp Dọc (Dùng cho cả Biến Thân Rebirth và Kỹ Năng)
     */
    public static void spawnPillarAt(ServerLevel level, ServerPlayer player, Vec3 center, DemonType type, boolean isDemonLord, boolean isPureVisual) {
        Item circleItem = getCircleItem(type);

        // 5 Tầng ma trận XẾP DỌC theo chiều cao (Y), kích thước to nhỏ khác nhau:
        // Tầng 1 (Đáy mặt đất): Y + 0.10m - Cực lớn 28 block
        Display.ItemDisplay t1 = createDisplay(level, center, circleItem, 28.0F, 0.10D);
        // Tầng 2 (Lơ lửng tầm thấp): Y + 4.80m - Lớn 20 block
        Display.ItemDisplay t2 = createDisplay(level, center, circleItem, 20.0F, 4.80D);
        // Tầng 3 (Tầm trung): Y + 10.50m - Vừa 14 block
        Display.ItemDisplay t3 = createDisplay(level, center, circleItem, 14.0F, 10.50D);
        // Tầng 4 (Tầm cao): Y + 16.50m - Nhỏ 9 block
        Display.ItemDisplay t4 = createDisplay(level, center, circleItem, 9.0F, 16.50D);
        // Tầng 5 (Đỉnh mây trời): Y + 23.50m - Lõi đỉnh 5.5 block
        Display.ItemDisplay t5 = createDisplay(level, center, circleItem, 5.5F, 23.50D);

        ACTIVE_PILLARS.add(new ActiveCelestialPillar(level, player, center, type, isDemonLord, isPureVisual,
                t1, t2, t3, t4, t5, 80));

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.1F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 3.0F, 1.2F);
    }

    private static Display.ItemDisplay createDisplay(ServerLevel level, Vec3 center, Item item, float scale, double yOffset) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        display.setPos(center.x, center.y + yOffset, center.z);
        ((ItemDisplayAccessor) display).weapons$setItemStack(new ItemStack(item));
        ((ItemDisplayAccessor) display).weapons$setItemTransform(ItemDisplayContext.FIXED);

        Quaternionf rot = new Quaternionf().rotateX((float) Math.toRadians(90.0));
        Transformation t = new Transformation(new Vector3f(0, 0, 0), rot, new Vector3f(scale, scale, 0.05F), new Quaternionf());
        ((DisplayAccessor) display).weapons$setTransformation(t);

        display.addTag("PentagramPillarDisplay");
        level.addFreshEntity(display);
        return display;
    }

    public static void tickPillars(ServerLevel level) {
        if (ACTIVE_PILLARS.isEmpty()) return;

        Iterator<ActiveCelestialPillar> it = ACTIVE_PILLARS.iterator();
        while (it.hasNext()) {
            ActiveCelestialPillar p = it.next();
            if (p.level != level) continue;

            p.ticksRemaining--;
            int elapsed = p.totalTicks - p.ticksRemaining;
            p.rotationAngle += 4.0F;

            // Xoay 5 tầng ma trận xếp dọc ngược chiều nhau tạo chiều sâu không gian kỳ vĩ
            updateRotation(p.tier1Bottom, 28.0F, p.rotationAngle * 0.7F);
            updateRotation(p.tier2Lower, 20.0F, -p.rotationAngle * 1.1F);
            updateRotation(p.tier3Middle, 14.0F, p.rotationAngle * 1.5F);
            updateRotation(p.tier4Upper, 9.0F, -p.rotationAngle * 1.9F);
            updateRotation(p.tier5Top, 5.5F, p.rotationAngle * 2.4F);

            // Giai đoạn 1: Tụ lực linh tử dọc theo 5 tầng xếp cao
            if (elapsed < 30) {
                for (int i = 0; i < 5; i++) {
                    double y = p.center.y + (i * 5.0D) + 0.5D;
                    double rad = 4.0D + (4 - i) * 2.0D;
                    double ang = Math.toRadians((elapsed * 12 + i * 72) % 360);
                    double px = p.center.x + Math.cos(ang) * rad;
                    double pz = p.center.z + Math.sin(ang) * rad;
                    p.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, y, pz, 1, 0, 0, 0, 0);
                    p.level.sendParticles(ParticleTypes.END_ROD, px, y, pz, 1, 0, 0.05D, 0, 0.02D);
                }
            }

            // Giai đoạn 2: Cột Sáng Thiên Giáng chọc trời giáng thẳng xuống xuyên suốt qua 5 tầng!
            if (elapsed == 30 && !p.beamTriggered) {
                p.beamTriggered = true;

                // Tạo Cột Sáng 3D khổng lồ cao 60m giáng từ mây trời xuống xuyên tâm 5 tầng
                p.lightBeam = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, p.level);
                p.lightBeam.setPos(p.center.x, p.center.y + 25.0D, p.center.z);
                ((ItemDisplayAccessor) p.lightBeam).weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()));
                ((ItemDisplayAccessor) p.lightBeam).weapons$setItemTransform(ItemDisplayContext.FIXED);

                Transformation beamTrans = new Transformation(
                        new Vector3f(0, 0, 0),
                        new Quaternionf(),
                        new Vector3f(16.0F, 65.0F, 16.0F),
                        new Quaternionf()
                );
                ((DisplayAccessor) p.lightBeam).weapons$setTransformation(beamTrans);
                p.lightBeam.addTag("PentagramPillarDisplay");
                p.level.addFreshEntity(p.lightBeam);

                // Âm thanh thiên phạt chấn động
                p.level.playSound(null, p.center.x, p.center.y, p.center.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.5F, 0.85F);
                p.level.playSound(null, p.center.x, p.center.y, p.center.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.3F);

                p.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.center.x, p.center.y + 1.0D, p.center.z, 6, 1.2, 1.2, 1.2, 0);
                p.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.center.x, p.center.y + 2.0D, p.center.z, 200, 6.0, 15.0, 6.0, 0.25);

                // Sát thương nếu không phải pure visual
                if (!p.isPureVisual) {
                    dealDamage(p);
                }
            }

            // Giai đoạn duy trì (Tick 30..70)
            if (elapsed > 30 && elapsed < 70) {
                p.level.sendParticles(ParticleTypes.FLASH, p.center.x, p.center.y + 2.0D, p.center.z, 1, 0, 0, 0, 0);
            }

            // Kết thúc: Dọn dẹp an toàn tuyệt đối
            if (p.ticksRemaining <= 0) {
                p.cleanup();
                it.remove();
            }
        }
    }

    private static void updateRotation(Display.ItemDisplay display, float scale, float angleDeg) {
        if (display == null || !display.isAlive()) return;
        Quaternionf rot = new Quaternionf()
                .rotateX((float) Math.toRadians(90.0))
                .rotateZ((float) Math.toRadians(angleDeg));
        Transformation t = new Transformation(new Vector3f(0, 0, 0), rot, new Vector3f(scale, scale, 0.05F), new Quaternionf());
        ((DisplayAccessor) display).weapons$setTransformation(t);
    }

    private static void dealDamage(ActiveCelestialPillar p) {
        AABB box = new AABB(p.center.x - 14.0D, p.center.y - 4.0D, p.center.z - 14.0D,
                p.center.x + 14.0D, p.center.y + 60.0D, p.center.z + 14.0D);

        List<LivingEntity> targets = p.level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != p.caster);

        for (LivingEntity v : targets) {
            v.setRemainingFireTicks(240);
            if (p.caster != null) {
                v.hurt(p.caster.damageSources().magic(), 650.0F);
            } else {
                v.hurt(p.level.damageSources().magic(), 650.0F);
            }
        }
    }
}
