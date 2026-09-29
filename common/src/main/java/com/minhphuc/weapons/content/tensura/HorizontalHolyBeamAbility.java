package com.minhphuc.weapons.content.tensura;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;

/**
 * Tuyệt kỹ: Cú Bắn Granit (Granite Blast - Ryu Ishigori / Tà Khứ Vũ Thê Tử Bắn Ngang)
 * Pháo năng lượng cực đại bắn ngang từ ngực/tay của Caster theo đường thẳng trước mặt,
 * đâm xuyên qua mọi đối thủ, xé toạc không gian và thiêu đốt địa hình xung quanh.
 * Hiện thực bằng CỘT SÁNG 3D ĐA TẦNG NẰM NGANG (Lõi Bạch Kim + Vỏ Bọc Hoàng Kim) cực kỳ tráng lệ.
 */
public class HorizontalHolyBeamAbility {

    public static class ActiveHorizontalBeam {
        public final ServerLevel level;
        public final Display.ItemDisplay coreBeam;
        public final Display.ItemDisplay outerBeam;
        public int ticksRemaining;

        public ActiveHorizontalBeam(ServerLevel level, Display.ItemDisplay coreBeam, Display.ItemDisplay outerBeam, int duration) {
            this.level = level;
            this.coreBeam = coreBeam;
            this.outerBeam = outerBeam;
            this.ticksRemaining = duration;
        }

        public void discard() {
            if (coreBeam != null && coreBeam.isAlive()) coreBeam.discard();
            if (outerBeam != null && outerBeam.isAlive()) outerBeam.discard();
        }
    }

    private static final List<ActiveHorizontalBeam> ACTIVE_BEAMS = new ArrayList<>();

    public static void tickBeams(ServerLevel level) {
        if (ACTIVE_BEAMS.isEmpty()) return;
        Iterator<ActiveHorizontalBeam> it = ACTIVE_BEAMS.iterator();
        while (it.hasNext()) {
            ActiveHorizontalBeam b = it.next();
            if (b.level != level) continue;
            b.ticksRemaining--;
            if (b.ticksRemaining <= 0) {
                b.discard();
                it.remove();
            }
        }
    }

    /**
     * Tạo và khởi chạy hiển thị Cột Sáng 3D đa tầng nằm ngang (Lõi trong + Vỏ bọc ngoài)
     */
    public static void spawn3DBeamDisplay(ServerLevel level, Vec3 startPos, Vec3 dir, double range,
                                          float coreScale, int coreGlowColor,
                                          float outerScale, int outerGlowColor,
                                          int durationTicks) {
        dir = dir.normalize();
        Vec3 beamCenter = startPos.add(dir.scale(range * 0.5D));

        Vector3f upAxis = new Vector3f(0, 1.0F, 0);
        Vector3f targetDir = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        Quaternionf rot = new Quaternionf().rotationTo(upAxis, targetDir);

        // Lớp 1: Lõi năng lượng (Core Beam)
        Display.ItemDisplay coreDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        coreDisplay.moveTo(beamCenter.x, beamCenter.y, beamCenter.z, 0.0F, 0.0F);
        ((ItemDisplayAccessor) coreDisplay).weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()));
        ((ItemDisplayAccessor) coreDisplay).weapons$setItemTransform(ItemDisplayContext.FIXED);
        ((DisplayAccessor) coreDisplay).weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
        coreDisplay.setGlowingTag(true);
        ((DisplayAccessor) coreDisplay).weapons$setGlowColorOverride(coreGlowColor);
        ((DisplayAccessor) coreDisplay).weapons$setViewRange(14.0F);
        Transformation coreTrans = new Transformation(
                new Vector3f(0, 0, 0),
                rot,
                new Vector3f(coreScale, (float) range, coreScale),
                null
        );
        ((DisplayAccessor) coreDisplay).weapons$setTransformation(coreTrans);
        coreDisplay.addTag("HorizontalBeamDisplay");
        level.addFreshEntity(coreDisplay);

        // Lớp 2: Vỏ bọc năng lượng bộc hỏa (Outer Shroud Beam)
        Display.ItemDisplay outerDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        outerDisplay.moveTo(beamCenter.x, beamCenter.y, beamCenter.z, 0.0F, 0.0F);
        ((ItemDisplayAccessor) outerDisplay).weapons$setItemStack(new ItemStack(ModItems.DISINTEGRATION_LIGHT_BEAM.get()));
        ((ItemDisplayAccessor) outerDisplay).weapons$setItemTransform(ItemDisplayContext.FIXED);
        ((DisplayAccessor) outerDisplay).weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
        outerDisplay.setGlowingTag(true);
        ((DisplayAccessor) outerDisplay).weapons$setGlowColorOverride(outerGlowColor);
        ((DisplayAccessor) outerDisplay).weapons$setViewRange(14.0F);
        Transformation outerTrans = new Transformation(
                new Vector3f(0, 0, 0),
                rot,
                new Vector3f(outerScale, (float) range, outerScale),
                null
        );
        ((DisplayAccessor) outerDisplay).weapons$setTransformation(outerTrans);
        outerDisplay.addTag("HorizontalBeamDisplay");
        level.addFreshEntity(outerDisplay);

        ACTIVE_BEAMS.add(new ActiveHorizontalBeam(level, coreDisplay, outerDisplay, durationTicks));
    }

    /**
     * Kích hoạt Cú Bắn Granit (Granite Blast) cho Người Chơi khi là Ma Vương
     */
    public static boolean cast(ServerLevel level, ServerPlayer player) {
        if (!PrimordialPlayerDataHelper.isDemonLord(player)) {
            player.displayClientMessage(
                    Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Yêu cầu Thức Tỉnh Chân Ma Vương để khai mở Cú Bắn Granit!"),
                    true
            );
            return false;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();

        // Âm thanh đại pháo nổ rung trời
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.5F, 1.1F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 1.3F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.5F);

        player.displayClientMessage(
                Component.literal("§6§l[CHÂN MA VƯƠNG] §e§l💥 CÚ BẮN GRANIT (GRANITE BLAST) PHÁT HỎA!! ⚡✨"),
                true
        );

        // Hiệu ứng giật lùi nhẹ và tia sáng tụ ở nòng bắn
        Vec3 recoil = look.scale(-0.4D);
        player.setDeltaMovement(player.getDeltaMovement().add(recoil.x, 0.1D, recoil.z));
        player.hurtMarked = true;

        fireGraniteBlast(level, player, eyePos, look, 56.0D, 1200.0F);
        return true;
    }

    /**
     * Bắn chùm pháo sáng 3D đa tầng nằm ngang xuyên thẳng và nổ quét địa hình
     */
    public static void fireGraniteBlast(ServerLevel level, LivingEntity caster, Vec3 startPos, Vec3 dir, double range, float damage) {
        dir = dir.normalize();

        spawn3DBeamDisplay(level, startPos, dir, range, 3.2F, 0xFFFFFF, 5.5F, 0xFFAA00, 30);

        // 2. TÍNH TOÁN VA CHẠM, SÁT THƯƠNG & XÉ RÁCH ĐỊA HÌNH
        double step = 1.0D;
        Vec3 cur = startPos;

        for (double d = 0; d < range; d += step) {
            cur = cur.add(dir.scale(step));

            // Hiệu ứng hạt mật độ cao dọc thân cột pháo
            level.sendParticles(ParticleTypes.FLASH, cur.x, cur.y, cur.z, 1, 0.3, 0.3, 0.3, 0);
            level.sendParticles(ParticleTypes.FLAME, cur.x, cur.y, cur.z, 4, 1.2, 1.2, 1.2, 0.08);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.15);
            level.sendParticles(ParticleTypes.END_ROD, cur.x, cur.y, cur.z, 2, 0.6, 0.6, 0.6, 0.05);

            // Sóng xung kích cách mỗi 6m
            if (Math.round(d) % 6 == 0) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
            }

            AABB box = new AABB(cur.x - 3.2, cur.y - 3.2, cur.z - 3.2,
                    cur.x + 3.2, cur.y + 3.2, cur.z + 3.2);

            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive());
            for (LivingEntity victim : victims) {
                // Tước bỏ mọi phòng ngự
                victim.removeEffect(MobEffects.REGENERATION);
                victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                victim.removeEffect(MobEffects.FIRE_RESISTANCE);

                // Thiêu đốt & làm chậm
                victim.setRemainingFireTicks(240);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, false, true));

                // Lực đẩy lùi cực mạnh theo hướng bắn
                Vec3 knockback = dir.scale(2.2D).add(0, 0.35D, 0);
                victim.setDeltaMovement(knockback);
                victim.hasImpulse = true;

                if (caster instanceof ServerPlayer sp) {
                    victim.hurt(sp.damageSources().playerAttack(sp), damage);
                    TensuraEvents.handleMobDeathDrop(sp, victim);
                } else {
                    victim.hurt(level.damageSources().magic(), damage);
                }
            }

            // Thiêu đốt mặt đất và làm vỡ các khối yếu (lá, mạng nhện, băng...)
            BlockPos groundPos = BlockPos.containing(cur).below();
            if (level.getBlockState(groundPos).isSolid() && level.getBlockState(groundPos.above()).isAir()) {
                if (level.random.nextFloat() < 0.4F) {
                    level.setBlockAndUpdate(groundPos.above(), Blocks.FIRE.defaultBlockState());
                }
            }

            // Phá vỡ thực vật / mạng nhện / hoa cỏ cản đường luồng pháo
            BlockPos centerPos = BlockPos.containing(cur);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos checkPos = centerPos.offset(ox, oy, oz);
                        net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(checkPos);
                        if (bs.is(Blocks.COBWEB) || bs.is(Blocks.SHORT_GRASS) || bs.is(Blocks.TALL_GRASS)
                                || bs.is(Blocks.SEAGRASS) || bs.is(Blocks.KELP) || bs.is(Blocks.SNOW)
                                || bs.is(Blocks.ICE) || bs.is(Blocks.VINE)) {
                            level.destroyBlock(checkPos, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Bắn chùm pháo 3D "Linh Tử Bộc Phát" (Spiritron Outburst) dựa trên cơ chế Vụ Nổ Granit,
     * tự động điều chỉnh màu sắc Lõi, Vỏ Bọc và các loại hạt particle theo màu đặc trưng của 7 Thủy Tổ Ác Ma.
     */
    public static void fireSpiritronOutburst(ServerLevel level, LivingEntity caster, Vec3 startPos, Vec3 dir,
                                             DemonType type, double range, float damage) {
        dir = dir.normalize();

        // 1. Phối màu Lõi và Vỏ Bọc 3D Beam Display theo Thủy Tổ Ác Ma
        int coreColor;
        int outerColor;

        if (type == null) {
            coreColor = 0xFFFFFF;
            outerColor = 0xFFAA00;
        } else {
            switch (type) {
                case ROUGE -> { // Xích Sắc - Guy Crimson: Đỏ rực & Huyết diễm
                    coreColor = 0xFF3333;
                    outerColor = 0xAA0000;
                }
                case NOIR -> { // Hắc Sắc - Diablo: Đen tuyền & Tím hắc ám ma vực
                    coreColor = 0x1A1A1A;
                    outerColor = 0x4A0072;
                }
                case BLANC -> { // Bạch Sắc - Testarossa: Bạch quang tinh khiết & Băng lam phát sáng
                    coreColor = 0xFFFFFF;
                    outerColor = 0xCFE2FE;
                }
                case JAUNE -> { // Hoàng Sắc - Carrera: Hoàng kim rực rỡ & Cam nhiệt hạch
                    coreColor = 0xFFFF22;
                    outerColor = 0xFF6600;
                }
                case VIOLET -> { // Tử Sắc - Ultima: Tử quang kịch độc & Tím u tối
                    coreColor = 0xD440FF;
                    outerColor = 0x6E00B3;
                }
                case BLEU -> { // Lam Sắc - Rein: Băng lam cực hạn & Hải lam sâu thẳm
                    coreColor = 0x50D2FE;
                    outerColor = 0x0284C7;
                }
                case VERT -> { // Lục Sắc - Misery: Lục bảo bão táp & Lục sẫm rừng già
                    coreColor = 0x4ADE80;
                    outerColor = 0x15803D;
                }
                default -> {
                    coreColor = 0xFFFFFF;
                    outerColor = 0xFFAA00;
                }
            }
        }

        // Tạo 3D Beam Display với tầm bắn và độ dày hoành tráng
        spawn3DBeamDisplay(level, startPos, dir, range, 3.4F, coreColor, 5.8F, outerColor, 32);

        // 2. TÍNH TOÁN VA CHẠM, SÁT THƯƠNG & XÉ RÁCH KHÔNG GIAN THEO MÀU SẮC
        double step = 1.0D;
        Vec3 cur = startPos;

        for (double d = 0; d < range; d += step) {
            cur = cur.add(dir.scale(step));

            // Sinh các hiệu ứng hạt đặc trưng theo hệ màu của từng Thủy Tổ
            if (type == null) {
                level.sendParticles(ParticleTypes.FLASH, cur.x, cur.y, cur.z, 1, 0.3, 0.3, 0.3, 0);
                level.sendParticles(ParticleTypes.FLAME, cur.x, cur.y, cur.z, 3, 1.0, 1.0, 1.0, 0.08);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cur.x, cur.y, cur.z, 2, 0.6, 0.6, 0.6, 0.15);
            } else {
                switch (type) {
                    case ROUGE -> {
                        level.sendParticles(ParticleTypes.FLAME, cur.x, cur.y, cur.z, 5, 1.2, 1.2, 1.2, 0.08);
                        level.sendParticles(ParticleTypes.LAVA, cur.x, cur.y, cur.z, 2, 0.8, 0.8, 0.8, 0.05);
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cur.x, cur.y, cur.z, 2, 0.8, 0.8, 0.8, 0.05);
                    }
                    case NOIR -> {
                        level.sendParticles(ParticleTypes.SQUID_INK, cur.x, cur.y, cur.z, 5, 1.2, 1.2, 1.2, 0.08);
                        level.sendParticles(ParticleTypes.DRAGON_BREATH, cur.x, cur.y, cur.z, 3, 1.0, 1.0, 1.0, 0.05);
                        level.sendParticles(ParticleTypes.PORTAL, cur.x, cur.y, cur.z, 4, 1.0, 1.0, 1.0, 0.2);
                    }
                    case BLANC -> {
                        level.sendParticles(ParticleTypes.END_ROD, cur.x, cur.y, cur.z, 4, 1.0, 1.0, 1.0, 0.05);
                        level.sendParticles(ParticleTypes.FLASH, cur.x, cur.y, cur.z, 1, 0.3, 0.3, 0.3, 0);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.05);
                    }
                    case JAUNE -> {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cur.x, cur.y, cur.z, 5, 1.2, 1.2, 1.2, 0.15);
                        level.sendParticles(ParticleTypes.CRIT, cur.x, cur.y, cur.z, 4, 1.0, 1.0, 1.0, 0.1);
                        level.sendParticles(ParticleTypes.LAVA, cur.x, cur.y, cur.z, 2, 0.6, 0.6, 0.6, 0.05);
                    }
                    case VIOLET -> {
                        level.sendParticles(ParticleTypes.WITCH, cur.x, cur.y, cur.z, 5, 1.2, 1.2, 1.2, 0.08);
                        level.sendParticles(ParticleTypes.DRAGON_BREATH, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.05);
                        level.sendParticles(ParticleTypes.PORTAL, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.15);
                    }
                    case BLEU -> {
                        level.sendParticles(ParticleTypes.SNOWFLAKE, cur.x, cur.y, cur.z, 5, 1.2, 1.2, 1.2, 0.08);
                        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.05);
                        level.sendParticles(ParticleTypes.GLOW, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.05);
                    }
                    case VERT -> {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cur.x, cur.y, cur.z, 4, 1.2, 1.2, 1.2, 0.08);
                        level.sendParticles(ParticleTypes.COMPOSTER, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.05);
                        level.sendParticles(ParticleTypes.CLOUD, cur.x, cur.y, cur.z, 3, 0.8, 0.8, 0.8, 0.05);
                    }
                }
            }

            // Sóng xung kích siêu thanh và chớp sáng bộc phát cách mỗi 5m
            if (Math.round(d) % 5 == 0) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.FLASH, cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
            }

            AABB box = new AABB(cur.x - 3.4, cur.y - 3.4, cur.z - 3.4,
                    cur.x + 3.4, cur.y + 3.4, cur.z + 3.4);

            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive());
            for (LivingEntity victim : victims) {
                // Tước bỏ toàn bộ trạng thái phòng ngự
                victim.removeEffect(MobEffects.REGENERATION);
                victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                victim.removeEffect(MobEffects.FIRE_RESISTANCE);

                // Áp dụng trạng thái đặc trưng theo hệ ác ma
                if (type != null) {
                    switch (type) {
                        case ROUGE -> {
                            victim.setRemainingFireTicks(300);
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, false, true));
                        }
                        case NOIR -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2, false, false, true));
                            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
                        }
                        case BLANC -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, false, true));
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, false, true));
                        }
                        case JAUNE -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, false, true));
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, false, true));
                        }
                        case VIOLET -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 2, false, false, true));
                            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                        }
                        case BLEU -> {
                            victim.setTicksFrozen(260);
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, false, false, true));
                        }
                        case VERT -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 1, false, false, true));
                            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, false, true));
                        }
                    }
                } else {
                    victim.setRemainingFireTicks(240);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, false, true));
                }

                // Lực đẩy lùi cực mạnh theo hướng bắn của tia pháo
                Vec3 knockback = dir.scale(2.4D).add(0, 0.4D, 0);
                victim.setDeltaMovement(knockback);
                victim.hasImpulse = true;

                // Xử lý sát thương tương ứng với nguồn bắn
                if (caster instanceof ServerPlayer sp) {
                    victim.hurt(sp.damageSources().playerAttack(sp), damage);
                    TensuraEvents.handleMobDeathDrop(sp, victim);
                } else if (caster instanceof PrimordialDemonEntity pde) {
                    pde.dealDemonicDamage(victim, damage);
                } else {
                    victim.hurt(level.damageSources().magic(), damage);
                }
            }

            // Thiêu đốt mặt đất và làm vỡ các khối yếu
            BlockPos groundPos = BlockPos.containing(cur).below();
            if (level.getBlockState(groundPos).isSolid() && level.getBlockState(groundPos.above()).isAir()) {
                if (level.random.nextFloat() < 0.35F) {
                    level.setBlockAndUpdate(groundPos.above(), Blocks.FIRE.defaultBlockState());
                }
            }

            // Phá vỡ thực vật cản đường chùm tia
            BlockPos centerPos = BlockPos.containing(cur);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos checkPos = centerPos.offset(ox, oy, oz);
                        net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(checkPos);
                        if (bs.is(Blocks.COBWEB) || bs.is(Blocks.SHORT_GRASS) || bs.is(Blocks.TALL_GRASS)
                                || bs.is(Blocks.SEAGRASS) || bs.is(Blocks.KELP) || bs.is(Blocks.SNOW)
                                || bs.is(Blocks.ICE) || bs.is(Blocks.VINE)) {
                            level.destroyBlock(checkPos, false);
                        }
                    }
                }
            }
        }
    }
}
