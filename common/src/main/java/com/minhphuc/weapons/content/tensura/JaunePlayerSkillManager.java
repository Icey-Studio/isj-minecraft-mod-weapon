package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Quản lý toàn bộ 5 Kỹ Năng Độc Bản & Hoạt Ảnh 3D của Hoàng Sắc Thủy Tổ - Carrera (Jaune)
 * khi người chơi biến thân / chuyển sinh thành Ác Ma Hoàng Sắc.
 */
public class JaunePlayerSkillManager {

    private static final DustParticleOptions GOLD_DIVINE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.1F), 2.5F);
    private static final DustParticleOptions NUCLEAR_ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.35F, 0.0F), 2.2F);
    private static final DustParticleOptions GRAVITY_DARK_DUST = new DustParticleOptions(new Vector3f(0.25F, 0.0F, 0.35F), 2.0F);

    // =========================================================================
    // 1. ACTIVE BARRAGE (KỸ NĂNG 1: BÃO ĐẠN MA ĐẠN HẠT NHÂN)
    // =========================================================================
    public static class ActiveBarrage {
        public final ServerPlayer player;
        public final ServerLevel level;
        public final boolean hasBody;
        public final boolean isDemonLord;
        public int ticksRemaining = 30; // 1.5 giây
        public int bulletsFired = 0;
        public Display.ItemDisplay leftCircle;
        public Display.ItemDisplay rightCircle;
        public float currentAngle = 0.0F;

        public ActiveBarrage(ServerPlayer player, ServerLevel level, boolean hasBody, boolean isDemonLord) {
            this.player = player;
            this.level = level;
            this.hasBody = hasBody;
            this.isDemonLord = isDemonLord;

            // Triệu hồi 2 Ma Pháp Trận Hủy Diệt 3D ở 2 bên sườn
            Vec3 pos = player.position();
            Vec3 look = player.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize();

            this.leftCircle = spawnCircle(level, pos.add(side.scale(1.2D)).add(0, 1.2D, 0), 1.8F, 0xFF4500, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get()));
            this.rightCircle = spawnCircle(level, pos.add(side.scale(-1.2D)).add(0, 1.2D, 0), 1.8F, 0xFFCC00, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE.get()));
        }

        public boolean tick() {
            if (!player.isAlive()) {
                cleanup();
                return true;
            }

            ticksRemaining--;
            currentAngle += 18.0F;

            Vec3 pos = player.position();
            Vec3 look = player.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize();

            // Cập nhật vị trí và góc xoay 2 ma pháp trận bám theo người chơi
            updateCircle(leftCircle, pos.add(side.scale(1.2D)).add(0, 1.2D, 0), 1.8F, currentAngle, player.getYRot());
            updateCircle(rightCircle, pos.add(side.scale(-1.2D)).add(0, 1.2D, 0), 1.8F, -currentAngle, player.getYRot());

            // Mỗi 3 ticks khai hỏa 1 viên Ma Đạn Hạt Nhân
            if (ticksRemaining % 3 == 0 && bulletsFired < 10) {
                bulletsFired++;
                boolean isLeft = (bulletsFired % 2 != 0);
                Vec3 muzzlePos = pos.add(isLeft ? side.scale(1.2D) : side.scale(-1.2D)).add(0, 1.2D, 0);

                fireNuclearBullet(level, player, muzzlePos, look, hasBody, isDemonLord);
            }

            if (ticksRemaining <= 0 || bulletsFired >= 10) {
                cleanup();
                return true;
            }
            return false;
        }

        public void cleanup() {
            if (leftCircle != null && leftCircle.isAlive()) leftCircle.discard();
            if (rightCircle != null && rightCircle.isAlive()) rightCircle.discard();
        }
    }

    private static void fireNuclearBullet(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 dir, boolean hasBody, boolean isDemonLord) {
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.5F, 1.4F);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8F, 1.8F);

        double maxDist = 45.0D;
        Vec3 end = origin.add(dir.scale(maxDist));
        LivingEntity hitEntity = null;

        for (double d = 0.5D; d <= maxDist; d += 1.2D) {
            Vec3 p = origin.add(dir.scale(d));
            level.sendParticles(GOLD_DIVINE_DUST, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.05D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.05D);

            AABB box = new AABB(p.x - 1.2D, p.y - 1.2D, p.z - 1.2D, p.x + 1.2D, p.y + 1.2D, p.z + 1.2D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
            if (!targets.isEmpty()) {
                hitEntity = targets.get(0);
                end = p;
                break;
            }
        }

        // Điểm nổ khi chạm trúng quái hoặc hết tầm
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y + 0.5D, end.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LAVA, end.x, end.y + 0.5D, end.z, 15, 0.3D, 0.3D, 0.3D, 0.1D);

        if (hitEntity != null) {
            dealDemonicDamage(player, hitEntity, 35.0F, hasBody, isDemonLord);
            hitEntity.setDeltaMovement(hitEntity.getDeltaMovement().add(0, 0.55D, 0).add(dir.scale(0.8D)));
            hitEntity.hurtMarked = true;
        }
    }

    // =========================================================================
    // 2. ACTIVE GRAVITY HOLE (KỸ NĂNG 2: HỐ ĐEN TRỌNG LỰC SỤP ĐỔ)
    // =========================================================================
    public static class ActiveGravityHole {
        public final ServerPlayer player;
        public final ServerLevel level;
        public final Vec3 center;
        public final boolean hasBody;
        public final boolean isDemonLord;
        public int ticksRemaining = 60; // 3 giây
        public Display.ItemDisplay groundCircle;
        public float angle = 0.0F;

        public ActiveGravityHole(ServerPlayer player, ServerLevel level, Vec3 center, boolean hasBody, boolean isDemonLord) {
            this.player = player;
            this.level = level;
            this.center = center;
            this.hasBody = hasBody;
            this.isDemonLord = isDemonLord;

            this.groundCircle = spawnHorizontalCircle(level, center.add(0, 0.05D, 0), 9.0F, 0xFACC15, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE.get()));
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.2F);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 0.6F);
        }

        public boolean tick() {
            ticksRemaining--;
            angle -= 8.0F;

            // Cập nhật ma pháp trận dán đất xoay tròn
            updateHorizontalCircle(groundCircle, center.add(0, 0.05D, 0), 9.0F, angle);

            // Hút toàn bộ kẻ thù trong bán kính 15m vào tâm & đè bẹp xuống đất
            AABB pullBox = new AABB(center.x - 15, center.y - 6, center.z - 15, center.x + 15, center.y + 10, center.z + 15);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != player && e.isAlive());

            for (LivingEntity e : targets) {
                Vec3 toCenter = center.subtract(e.position()).normalize().scale(0.55D);
                e.setDeltaMovement(e.getDeltaMovement().add(toCenter.x, -2.5D, toCenter.z));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 3, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }

            // Hạt xoáy trọng lực
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians((System.currentTimeMillis() % 3600) / 10.0D + (i * 45.0D));
                double r = 2.0D + (ticksRemaining % 25) * 0.25D;
                double px = center.x + Math.cos(a) * r;
                double pz = center.z + Math.sin(a) * r;
                level.sendParticles(GRAVITY_DARK_DUST, px, center.y + 0.1D, pz, 1, 0, 0, 0, 0.02D);
                level.sendParticles(ParticleTypes.PORTAL, px, center.y + 0.2D, pz, 1, 0, 0, 0, 0.05D);
            }

            if (ticksRemaining % 15 == 0) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.0F, 0.7F);
            }

            // Kết thúc 3s: Hố đen nổ tung cực đại!
            if (ticksRemaining <= 0) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0F, 0.7F);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 3.0F, 1.2F);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0D, center.z, 3, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0D, center.z, 4, 0.5D, 0.5D, 0.5D, 0);

                for (LivingEntity e : targets) {
                    dealDemonicDamage(player, e, 220.0F, hasBody, isDemonLord);
                    Vec3 blastAway = e.position().subtract(center).normalize().scale(2.2D).add(0, 1.4D, 0);
                    e.setDeltaMovement(blastAway);
                    e.hurtMarked = true;
                }

                if (groundCircle != null && groundCircle.isAlive()) groundCircle.discard();
                return true;
            }
            return false;
        }
    }

    // =========================================================================
    // 3. ACTIVE SUPERNOVA (KỸ NĂNG 5: CẤM THUẬT TẬN DIỆT HẠT NHÂN)
    // =========================================================================
    public static class ActiveSupernova {
        public final ServerPlayer player;
        public final ServerLevel level;
        public final Vec3 targetGround;
        public final boolean hasBody;
        public final boolean isDemonLord;
        public int ticksRemaining = 35; // ~1.8 giây tụ lực rồi nổ
        public Display.ItemDisplay giantGroundCircle;
        public Display.ItemDisplay skyCircle;

        public ActiveSupernova(ServerPlayer player, ServerLevel level, Vec3 targetGround, boolean hasBody, boolean isDemonLord) {
            this.player = player;
            this.level = level;
            this.targetGround = targetGround;
            this.hasBody = hasBody;
            this.isDemonLord = isDemonLord;

            // Nâng người chơi bay lơ lửng 4m
            player.setDeltaMovement(0, 0.5D, 0);
            player.hurtMarked = true;

            // Triệu hồi 2 Ma Pháp Trận Hạt Nhân khổng lồ
            this.giantGroundCircle = spawnHorizontalCircle(level, targetGround.add(0, 0.05D, 0), 18.0F, 0xFFFF00, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get()));
            this.skyCircle = spawnHorizontalCircle(level, targetGround.add(0, 18.0D, 0), 14.0F, 0xFFA500, new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get()));

            level.playSound(null, targetGround.x, targetGround.y + 5.0D, targetGround.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 0.5F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 3.5F, 1.4F);

            player.displayClientMessage(
                    Component.literal("§6§l[JAUNE - ABADDON] §eKhởi phóng Cấm Thuật Hạt Nhân: §4§lTẬN DIỆT SUPERNOVA!"),
                    true
            );
        }

        public boolean tick() {
            ticksRemaining--;

            // Hạt tụ lực trên đầu người chơi
            Vec3 headPos = player.position().add(0, player.getBbHeight() + 0.8D, 0);
            level.sendParticles(ParticleTypes.END_ROD, headPos.x, headPos.y, headPos.z, 15, 0.4D, 0.4D, 0.4D, 0.05D);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, headPos.x, headPos.y, headPos.z, 20, 0.5D, 0.5D, 0.5D, 0.1D);
            level.sendParticles(GOLD_DIVINE_DUST, headPos.x, headPos.y, headPos.z, 12, 0.3D, 0.3D, 0.3D, 0.05D);

            // NỔ NẤM MÂY HẠT NHÂN KHỔNG LỒ KHI HẾT GIỜ TỤ LỰC
            if (ticksRemaining <= 0) {
                level.playSound(null, targetGround.x, targetGround.y + 2.0D, targetGround.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0F, 0.5F);
                level.playSound(null, targetGround.x, targetGround.y + 2.0D, targetGround.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 0.8F);
                level.playSound(null, targetGround.x, targetGround.y + 2.0D, targetGround.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 4.0F, 1.0F);

                // Nấm mây hạt nhân đa tầng
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetGround.x, targetGround.y + 2.0D, targetGround.z, 15, 3.0D, 3.0D, 3.0D, 0);
                level.sendParticles(ParticleTypes.FLASH, targetGround.x, targetGround.y + 3.0D, targetGround.z, 8, 1.0D, 1.0D, 1.0D, 0);
                level.sendParticles(ParticleTypes.LAVA, targetGround.x, targetGround.y + 2.0D, targetGround.z, 140, 8.0D, 3.0D, 8.0D, 0.3D);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, targetGround.x, targetGround.y + 4.0D, targetGround.z, 100, 6.0D, 6.0D, 6.0D, 0.1D);
                level.sendParticles(ParticleTypes.DRAGON_BREATH, targetGround.x, targetGround.y + 3.0D, targetGround.z, 80, 7.0D, 4.0D, 7.0D, 0.1D);

                AABB blastBox = new AABB(targetGround.x - 26, targetGround.y - 6, targetGround.z - 26, targetGround.x + 26, targetGround.y + 16, targetGround.z + 26);
                List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e != player && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));

                for (LivingEntity v : victims) {
                    dealDemonicDamage(player, v, 360.0F, hasBody, isDemonLord);
                    Vec3 push = v.position().subtract(targetGround).normalize().scale(3.0D).add(0, 1.6D, 0);
                    v.setDeltaMovement(push);
                    v.hurtMarked = true;
                    v.setRemainingFireTicks(240);
                }

                if (giantGroundCircle != null && giantGroundCircle.isAlive()) giantGroundCircle.discard();
                if (skyCircle != null && skyCircle.isAlive()) skyCircle.discard();
                return true;
            }
            return false;
        }
    }

    public static final List<ActiveBarrage> ACTIVE_BARRAGES = new ArrayList<>();
    public static final List<ActiveGravityHole> ACTIVE_GRAVITY_HOLES = new ArrayList<>();
    public static final List<ActiveSupernova> ACTIVE_SUPERNOVAS = new ArrayList<>();

    public static void tick(ServerLevel level) {
        ACTIVE_BARRAGES.removeIf(ActiveBarrage::tick);
        ACTIVE_GRAVITY_HOLES.removeIf(ActiveGravityHole::tick);
        ACTIVE_SUPERNOVAS.removeIf(ActiveSupernova::tick);
    }

    // =========================================================================
    // THI TRIỂN CÁC KỸ NĂNG CỦA JAUNE
    // =========================================================================

    /**
     * Kỹ năng 1: Bão Đạn Ma Đạn Hạt Nhân
     */
    public static void castSkillOne(ServerLevel level, ServerPlayer player, boolean hasBody, boolean isDemonLord) {
        ACTIVE_BARRAGES.add(new ActiveBarrage(player, level, hasBody, isDemonLord));
        player.displayClientMessage(Component.literal("§6§l[JAUNE] §eKhai hỏa Bão Đạn Hạt Nhân Abaddon liên thanh!"), true);
    }

    /**
     * Kỹ năng 2: Hố Đen Trọng Lực Sụp Đổ
     */
    public static void castSkillTwo(ServerLevel level, ServerPlayer player, boolean hasBody, boolean isDemonLord) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(look.scale(30.0D)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getLocation();

        ACTIVE_GRAVITY_HOLES.add(new ActiveGravityHole(player, level, target, hasBody, isDemonLord));
        player.displayClientMessage(Component.literal("§5§l[JAUNE] §dHố Đen Trọng Lực đã phong tỏa toàn bộ không gian mặt đất!"), true);
    }

    /**
     * Kỹ năng 3: Cự Pháo Hạt Nhân Xuyên Thấu: Abaddon Piercer
     */
    public static void castSkillThree(ServerLevel level, ServerPlayer player, boolean hasBody, boolean isDemonLord) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 60.0D;

        // 1. Âm thanh bộc phá khai pháo
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 1.6F);

        // 2. Chùm Đại Pháo 3D Beam vàng kim rực lửa 60 mét
        HorizontalHolyBeamAbility.spawn3DBeamDisplay(level, eyePos, look, maxDist, 3.5F, 0xFFFF00, 7.0F, 0xFF4500, 40);

        // 3. Quét sát thương dọc chùm pháo
        Vec3 cur = eyePos;
        double step = 1.5D;
        for (double d = 0; d < maxDist; d += step) {
            cur = cur.add(look.scale(step));
            level.sendParticles(ParticleTypes.LAVA, cur.x, cur.y, cur.z, 2, 0.4D, 0.4D, 0.4D, 0.1D);
            if (Math.round(d) % 6 == 0) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
            }

            AABB hitBox = new AABB(cur.x - 3.0D, cur.y - 3.0D, cur.z - 3.0D, cur.x + 3.0D, cur.y + 3.0D, cur.z + 3.0D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e.isAlive());
            for (LivingEntity v : targets) {
                dealDemonicDamage(player, v, 280.0F, hasBody, isDemonLord);
                Vec3 push = look.scale(1.8D).add(0, 0.4D, 0);
                v.setDeltaMovement(push);
                v.hurtMarked = true;
                v.setRemainingFireTicks(160);
            }
        }

        player.displayClientMessage(Component.literal("§c§l[JAUNE] §4§lĐẠI PHÁO HẠT NHÂN ABADDON §exuyên thủng mọi hàng phòng ngự!"), true);
    }

    /**
     * Kỹ năng 4: Thần Kiếm Trọng Lực: Bứt Tốc Trảm
     */
    public static void castSkillFour(ServerLevel level, ServerPlayer player, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();
        double dashDist = 12.0D;
        Vec3 dest = pos.add(look.scale(dashDist));

        // Kiểm tra block va chạm để tránh kẹt tường
        BlockHitResult blockHit = level.clip(new ClipContext(pos, dest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            dest = blockHit.getLocation().subtract(look.scale(0.8D));
        }

        // Lướt chớp nhoáng (Flash Dash)
        player.teleportTo(dest.x, dest.y, dest.z);

        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 3.0F, 0.6F);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5F, 1.4F);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0F, 1.8F);

        // Hiệu ứng vệt chém bán nguyệt vàng kim chói lòa
        for (double d = 0; d <= pos.distanceTo(dest); d += 1.0D) {
            Vec3 p = pos.add(look.scale(d));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y + 1.0D, p.z, 2, 0.5D, 0.5D, 0.5D, 0);
            level.sendParticles(GOLD_DIVINE_DUST, p.x, p.y + 1.0D, p.z, 3, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, dest.x, dest.y + 1.0D, dest.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLASH, dest.x, dest.y + 1.0D, dest.z, 2, 0, 0, 0, 0);

        // Quét chém toàn bộ kẻ địch xung quanh điểm đến
        AABB cleaveBox = new AABB(dest.x - 7.0D, dest.y - 3.0D, dest.z - 7.0D, dest.x + 7.0D, dest.y + 5.0D, dest.z + 7.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, cleaveBox, e -> e != player && e.isAlive());

        for (LivingEntity v : targets) {
            dealDemonicDamage(player, v, 160.0F, hasBody, isDemonLord);
            Vec3 push = v.position().subtract(dest).normalize().scale(1.8D).add(0, 0.6D, 0);
            v.setDeltaMovement(push);
            v.hurtMarked = true;
            v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3));
        }

        player.displayClientMessage(Component.literal("§e§l[JAUNE] §6Thần Kiếm Trọng Lực: Bứt Tốc Trảm xé rách không gian!"), true);
    }

    /**
     * Kỹ năng 5: Cấm Thuật Tận Diệt Hạt Nhân (Supernova)
     */
    public static void castSkillFive(ServerLevel level, ServerPlayer player, boolean hasBody, boolean isDemonLord) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(look.scale(35.0D)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getLocation();

        ACTIVE_SUPERNOVAS.add(new ActiveSupernova(player, level, target, hasBody, isDemonLord));
    }

    // =========================================================================
    // HỖ TRỢ XỬ LÝ SÁT THƯƠNG & MA PHÁP TRẬN DISPLAY 3D
    // =========================================================================

    public static void dealDemonicDamage(ServerPlayer player, LivingEntity victim, float baseDmg, boolean hasBody, boolean isDemonLord) {
        if (victim == null || !victim.isAlive() || victim == player) return;
        boolean isBoss = (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                || (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                || (victim instanceof net.minecraft.world.entity.monster.warden.Warden)
                || (victim instanceof net.minecraft.world.entity.animal.IronGolem)
                || (victim instanceof net.minecraft.world.entity.monster.ElderGuardian);

        if (hasBody || isDemonLord) {
            if (isBoss) {
                victim.hurt(player.damageSources().magic(), victim.getMaxHealth() * 0.52F);
            } else if (!(victim instanceof VelgryndEntity)) {
                victim.hurt(player.damageSources().magic(), victim.getMaxHealth() * 3.0F);
            }
        } else {
            if (isBoss) {
                victim.hurt(player.damageSources().magic(), baseDmg * 0.7F);
            } else {
                victim.hurt(player.damageSources().magic(), baseDmg);
            }
        }
    }

    public static Display.ItemDisplay spawnCircle(ServerLevel level, Vec3 pos, float scale, int glowColor, ItemStack stack) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) display;
            DisplayAccessor dispAcc = (DisplayAccessor) display;

            itemAcc.weapons$setItemStack(stack);
            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            dispAcc.weapons$setGlowColorOverride(glowColor);
            dispAcc.weapons$setViewRange(8.0F);

            level.addFreshEntity(display);
        }
        return display;
    }

    public static Display.ItemDisplay spawnHorizontalCircle(ServerLevel level, Vec3 pos, float scale, int glowColor, ItemStack stack) {
        Display.ItemDisplay display = spawnCircle(level, pos, scale, glowColor, stack);
        if (display != null) {
            DisplayAccessor dispAcc = (DisplayAccessor) display;
            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf().rotateX((float) Math.toRadians(90.0F)),
                    new Vector3f(scale, scale, 0.01F),
                    null
            ));
        }
        return display;
    }

    public static void updateCircle(Display.ItemDisplay display, Vec3 pos, float scale, float rotZ, float playerYaw) {
        if (display == null || !display.isAlive()) return;
        display.setPos(pos.x, pos.y, pos.z);
        DisplayAccessor dispAcc = (DisplayAccessor) display;
        Quaternionf rot = new Quaternionf().rotateY((float) Math.toRadians(-playerYaw)).rotateZ((float) Math.toRadians(rotZ));
        dispAcc.weapons$setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                rot,
                new Vector3f(scale, scale, 0.01F),
                null
        ));
    }

    public static void updateHorizontalCircle(Display.ItemDisplay display, Vec3 pos, float scale, float rotZ) {
        if (display == null || !display.isAlive()) return;
        display.setPos(pos.x, pos.y, pos.z);
        DisplayAccessor dispAcc = (DisplayAccessor) display;
        Quaternionf rot = new Quaternionf().rotateX((float) Math.toRadians(90.0F)).rotateZ((float) Math.toRadians(rotZ));
        dispAcc.weapons$setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                rot,
                new Vector3f(scale, scale, 0.01F),
                null
        ));
    }
}
