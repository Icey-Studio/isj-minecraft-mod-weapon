package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TUYỆT KỸ: HẮC VIÊM NGỤC (HELLFLARE - 獄炎環 / BENIMARU - TENSURA)
 * 1. Khóa Không Gian Khối Lập Phương (Spatial Containment Cube - Tick 0 -> 25):
 *    - Khối không gian kính đen trong suốt lập tức bao bọc mục tiêu.
 *    - Người bên ngoài nhìn vào THẤY RÕ mob bị giam cầm bất động bên trong!
 * 2. Hắc Viêm Bùng Nổ & Bao Trùm Hoàn Toàn (Roaring Black Fire Surge - Tick 26 -> 79):
 *    - Lửa Đen Thật (Black Fire) bùng lên cuồn cuộn bao phủ toàn bộ 6 mặt khối vuông.
 *    - Khối chuyển sang màu đen tuyền tuyệt đối che khuất 100% tầm nhìn từ bên ngoài.
 *    - Mọi sinh vật bên trong bị thiêu đốt liên tục (bỏ qua kháng lửa).
 * 3. Bế Mạc Sụp Đổ Nén Hư Vô (Final Vacuum Implosion - Tick 80):
 *    - Nổ nén chân không cực hạn (70 sát thương).
 *    - Quy tắc miễn trừ chuẩn:
 *      + Long Chủng (Velgrynd, Velzard): 0% sát thương.
 *      + Guy (Rouge), Blanc, Noir: 0% sát thương.
 *      + Milim, Marbas: 0% sát thương.
 *      + Cứu Thế Quyền Hiện: 30% sát thương.
 *      + Mọi thực thể khác: 100% sát thương.
 * 4. Tối Ưu Hóa ZERO-LAG:
 *    - Điều khiển qua ItemDisplay, dọn dẹp sạch sẽ khi nổ, O(1) kiểm tra người chơi gần.
 */
public class HellflareAbility {

    public static class ActiveHellflare {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 startPos;
        public final Vec3 center;
        public final int flightDuration;
        public Display.ItemDisplay projectileDisplay;
        public Display.ItemDisplay sphereDisplay;
        public Display.ItemDisplay fireDisplay;
        public int ticksAlive = 0;
        public final int maxTicks;

        public ActiveHellflare(ServerLevel level, ServerPlayer caster, Vec3 startPos, Vec3 center, int flightDuration) {
            this.level = level;
            this.caster = caster;
            this.startPos = startPos;
            this.center = center;
            this.flightDuration = Math.max(2, flightDuration);
            this.maxTicks = this.flightDuration + 80;
        }

        public void cleanup() {
            if (projectileDisplay != null && projectileDisplay.isAlive()) {
                projectileDisplay.discard();
                projectileDisplay = null;
            }
            if (sphereDisplay != null && sphereDisplay.isAlive()) {
                sphereDisplay.discard();
                sphereDisplay = null;
            }
            if (fireDisplay != null && fireDisplay.isAlive()) {
                fireDisplay.discard();
                fireDisplay = null;
            }
        }
    }

    private static final List<ActiveHellflare> ACTIVE_HELLFLARES = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(20.0D));

        // Ưu tiên raycast trúng thẳng vào mob mục tiêu nếu có
        AABB reachBox = player.getBoundingBox().inflate(24.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reachBox, ent -> ent != player && ent.isAlive())) {
            AABB mobBox = e.getBoundingBox().inflate(0.5D);
            var opt = mobBox.clip(eyePos, eyePos.add(lookVec.scale(24.0D)));
            if (opt.isPresent()) {
                targetPos = e.position().add(0, e.getBbHeight() * 0.5D, 0);
                break;
            }
        }

        BlockHitResult hit = level.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            targetPos = hit.getLocation();
        }

        double distance = eyePos.distanceTo(targetPos);
        int flightTicks = Math.max(3, (int) (distance / 1.6D));

        ActiveHellflare instance = new ActiveHellflare(level, player, eyePos, targetPos, flightTicks);

        // GIAI ĐOẠN 0: BẮN RA CẦU LỬA ĐEN (PROJECTILE PHASE)
        Display.ItemDisplay projDisplay = EntityType.ITEM_DISPLAY.create(level);
        if (projDisplay != null) {
            projDisplay.moveTo(eyePos.x, eyePos.y, eyePos.z, 0.0F, 0.0F);
            ItemDisplayAccessor piAcc = (ItemDisplayAccessor) projDisplay;
            DisplayAccessor pdAcc = (DisplayAccessor) projDisplay;

            piAcc.weapons$setItemStack(new ItemStack(ModItems.BLACK_FIRE.get()));
            piAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            pdAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            pdAcc.weapons$setViewRange(8.0F);
            pdAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(1.4F, 1.4F, 1.4F),
                    null
            ));
            level.addFreshEntity(projDisplay);
            instance.projectileDisplay = projDisplay;
        }

        ACTIVE_HELLFLARES.add(instance);

        if (com.minhphuc.weapons.config.SkillVisualConfig.isCinematicEnabled(com.minhphuc.weapons.config.SkillVisualConfig.SKILL_HELLFLARE)) {
            com.minhphuc.weapons.network.ModMessages.sendToPlayers(level.players(), new com.minhphuc.weapons.network.ClientboundCinematicSkillPacket(
                    com.minhphuc.weapons.config.SkillVisualConfig.SKILL_HELLFLARE, player.getUUID(), targetPos, lookVec, instance.maxTicks, com.minhphuc.weapons.network.ClientboundCinematicSkillPacket.ACTION_START, 1.0f
            ));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.5F, 0.8F);

        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l【 HẮC VIÊM NGỤC: HELLFLARE 】")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c✦ Phóng Cầu Hắc Hỏa ➔ Khóa Không Gian ✦")));

        player.displayClientMessage(
                Component.literal("§4§l[BENIMARU - HELLFLARE] §cĐã khai hỏa cầu Hắc Viêm! Bắn tới mục tiêu rồi phong tỏa thành khối không gian giam hãm! 🔥⬛"),
                true
        );
    }

    public static void castFromDemon(ServerLevel level, com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity demon, Vec3 eyePos, Vec3 targetPos, ServerPlayer owner) {
        if (level == null || demon == null) return;
        double distance = eyePos.distanceTo(targetPos);
        int flightTicks = Math.max(3, (int) (distance / 1.6D));

        ActiveHellflare instance = new ActiveHellflare(level, owner, eyePos, targetPos, flightTicks);

        Display.ItemDisplay projDisplay = EntityType.ITEM_DISPLAY.create(level);
        if (projDisplay != null) {
            projDisplay.moveTo(eyePos.x, eyePos.y, eyePos.z, 0.0F, 0.0F);
            ItemDisplayAccessor piAcc = (ItemDisplayAccessor) projDisplay;
            DisplayAccessor pdAcc = (DisplayAccessor) projDisplay;

            piAcc.weapons$setItemStack(new ItemStack(com.minhphuc.weapons.init.ModItems.BLACK_FIRE.get()));
            piAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            pdAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            pdAcc.weapons$setViewRange(8.0F);
            pdAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(1.4F, 1.4F, 1.4F),
                    null
            ));
            level.addFreshEntity(projDisplay);
            instance.projectileDisplay = projDisplay;
        }

        ACTIVE_HELLFLARES.add(instance);

        level.playSound(null, eyePos.x, eyePos.y, eyePos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.5F, 0.8F);
    }

    public static void tickHellflares(ServerLevel level) {
        if (ACTIVE_HELLFLARES.isEmpty()) return;

        synchronized (ACTIVE_HELLFLARES) {
            Iterator<ActiveHellflare> it = ACTIVE_HELLFLARES.iterator();
            while (it.hasNext()) {
                ActiveHellflare hf = it.next();
                if (hf.level != level) continue;

                hf.ticksAlive++;
                Vec3 center = hf.center;
                double boxRadius = 3.5D;

                // =====================================================================
                // GIAI ĐOẠN 0: ĐẠN CẦU LỬA ĐEN BAY VỀ PHÍA MỤC TIÊU (PROJECTILE FLIGHT)
                // =====================================================================
                if (hf.ticksAlive < hf.flightDuration) {
                    float fProg = (float) hf.ticksAlive / (float) hf.flightDuration;
                    Vec3 curPos = hf.startPos.lerp(center, fProg);

                    if (hf.projectileDisplay != null && hf.projectileDisplay.isAlive()) {
                        hf.projectileDisplay.moveTo(curPos.x, curPos.y, curPos.z, 0.0F, 0.0F);
                    }

                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, curPos.x, curPos.y, curPos.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
                    level.sendParticles(ParticleTypes.SQUID_INK, curPos.x, curPos.y, curPos.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
                    level.sendParticles(ParticleTypes.SMOKE, curPos.x, curPos.y, curPos.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                    continue;
                }

                // =====================================================================
                // ĐẠN CHẠM ĐÍCH: KÍCH HOẠT KHỐI GIAO HÃM NHỎ (SCALE 1.5)
                // =====================================================================
                if (hf.ticksAlive == hf.flightDuration) {
                    if (hf.projectileDisplay != null && hf.projectileDisplay.isAlive()) {
                        hf.projectileDisplay.discard();
                        hf.projectileDisplay = null;
                    }

                    Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
                    if (display != null) {
                        display.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
                        ItemDisplayAccessor iAcc = (ItemDisplayAccessor) display;
                        DisplayAccessor dAcc = (DisplayAccessor) display;

                        iAcc.weapons$setItemStack(new ItemStack(Blocks.BLACK_STAINED_GLASS));
                        iAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
                        dAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
                        dAcc.weapons$setViewRange(8.0F);

                        dAcc.weapons$setTransformation(new Transformation(
                                new Vector3f(0.0F, 0.0F, 0.0F),
                                new Quaternionf(),
                                new Vector3f(1.5F, 1.5F, 1.5F),
                                null
                        ));

                        level.addFreshEntity(display);
                        hf.sphereDisplay = display;
                    }

                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 0.8F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.5F, 1.4F);
                }

                // =====================================================================
                // GIAI ĐOẠN 1: KHỐI LẬP PHƯƠNG KHÔNG GIAN PHÓNG TO MƯỢT MÀ (SCALE 1.5 ➔ 7.0)
                // =====================================================================
                int expandTicks = 15;
                int expandEndTick = hf.flightDuration + expandTicks;
                if (hf.ticksAlive > hf.flightDuration && hf.ticksAlive <= expandEndTick) {
                    float expandProg = (float) (hf.ticksAlive - hf.flightDuration) / (float) expandTicks;
                    float curScale = 1.5F + 5.5F * expandProg;

                    if (hf.sphereDisplay != null && hf.sphereDisplay.isAlive()) {
                        DisplayAccessor dAcc = (DisplayAccessor) hf.sphereDisplay;
                        dAcc.weapons$setTransformation(new Transformation(
                                new Vector3f(0.0F, 0.0F, 0.0F),
                                new Quaternionf(),
                                new Vector3f(curScale, curScale, curScale),
                                null
                        ));
                    }
                }

                // =====================================================================
                // GIAI ĐOẠN 2: LỬA ĐEN THẬT BÙNG NỔ BAO TRÙM (KHÔNG THỂ NHÌN TỪ NGOÀI VÀO)
                // =====================================================================
                int fireSurgeTick = expandEndTick + 1;
                if (hf.ticksAlive == fireSurgeTick) {
                    // 1. Chuyển khối kính sang bê tông đen tuyền 100% che khuất tầm nhìn bên trong
                    if (hf.sphereDisplay != null && hf.sphereDisplay.isAlive()) {
                        ItemDisplayAccessor iAcc = (ItemDisplayAccessor) hf.sphereDisplay;
                        iAcc.weapons$setItemStack(new ItemStack(Blocks.BLACK_CONCRETE));
                    }

                    // 2. Tạo lớp hiển thị Ngọn Lửa Đen Thật (Black Fire) bao bọc toàn bộ khối vuông
                    Display.ItemDisplay fireDisp = EntityType.ITEM_DISPLAY.create(level);
                    if (fireDisp != null) {
                        fireDisp.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
                        ItemDisplayAccessor fiAcc = (ItemDisplayAccessor) fireDisp;
                        DisplayAccessor fdAcc = (DisplayAccessor) fireDisp;

                        fiAcc.weapons$setItemStack(new ItemStack(ModItems.BLACK_FIRE.get()));
                        fiAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
                        fdAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
                        fdAcc.weapons$setViewRange(8.0F);

                        fdAcc.weapons$setTransformation(new Transformation(
                                new Vector3f(0.0F, 0.0F, 0.0F),
                                new Quaternionf(),
                                new Vector3f(7.5F, 7.5F, 7.5F),
                                null
                        ));

                        level.addFreshEntity(fireDisp);
                        hf.fireDisplay = fireDisp;
                    }

                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 3.5F, 0.6F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 3.0F, 0.7F);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 2.5F, 0.8F);

                    if (level.hasNearbyAlivePlayer(center.x, center.y, center.z, 48.0D)) {
                        for (int a = 0; a < 360; a += 30) {
                            double rad = Math.toRadians(a);
                            double px = center.x + Math.cos(rad) * 3.6D;
                            double pz = center.z + Math.sin(rad) * 3.6D;
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, center.y, pz, 4, 0.3D, 0.8D, 0.3D, 0.08D);
                            level.sendParticles(ParticleTypes.SQUID_INK, px, center.y + 0.5D, pz, 3, 0.2D, 0.5D, 0.2D, 0.05D);
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, px, center.y, pz, 2, 0.2D, 0.5D, 0.2D, 0.02D);
                        }
                    }
                }

                // =====================================================================
                // HIỆU ỨNG HẠT ĐỊNH KỲ:
                // - Trước khi bùng lửa đen: Hạt không gian tím/đen viền khối vuông (Thấy mob bên trong)
                // - Sau khi bùng lửa đen: Lửa đen bốc cháy ngùn ngụt che kín bên trong
                // =====================================================================
                if (hf.ticksAlive % 4 == 0 && level.hasNearbyAlivePlayer(center.x, center.y, center.z, 48.0D)) {
                    if (hf.ticksAlive < fireSurgeTick) {
                        // Hạt viền kết giới không gian
                        for (int a = 0; a < 360; a += 60) {
                            double rad = Math.toRadians(a);
                            double px = center.x + Math.cos(rad) * boxRadius;
                            double pz = center.z + Math.sin(rad) * boxRadius;
                            level.sendParticles(ParticleTypes.PORTAL, px, center.y + level.random.nextDouble() * 3.0D - 1.5D, pz, 2, 0.1D, 0.2D, 0.1D, 0.05D);
                            level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, center.y + 0.5D, pz, 1, 0.05D, 0.1D, 0.05D, 0.02D);
                        }
                    } else {
                        // Ngọn lửa đen thật bùng cháy quanh bề mặt khối vuông
                        for (int a = 0; a < 360; a += 45) {
                            double rad = Math.toRadians(a);
                            double px = center.x + Math.cos(rad) * boxRadius;
                            double pz = center.z + Math.sin(rad) * boxRadius;
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, center.y + level.random.nextDouble() * 4.0D - 2.0D, pz, 2, 0.2D, 0.4D, 0.2D, 0.05D);
                            level.sendParticles(ParticleTypes.SQUID_INK, px, center.y + 0.5D, pz, 1, 0.1D, 0.3D, 0.1D, 0.02D);
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, px, center.y + level.random.nextDouble() * 4.0D - 2.0D, pz, 1, 0.1D, 0.2D, 0.1D, 0.02D);
                        }
                    }
                }

                // =====================================================================
                // GIAM HÃM VÀ THIÊU ĐỐT LIÊN TỤC MỤC TIÊU BÊN TRONG (MỖI 5 TICKS = 0.25S)
                // =====================================================================
                if (hf.ticksAlive % 5 == 0 && hf.ticksAlive >= hf.flightDuration) {
                    AABB box = new AABB(center.x - boxRadius, center.y - boxRadius, center.z - boxRadius,
                                        center.x + boxRadius, center.y + boxRadius, center.z + boxRadius);
                    List<LivingEntity> trapped = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);

                    for (LivingEntity e : trapped) {
                        if (e == hf.caster) continue;

                        // Kéo ngược về tâm nếu cố gắng di chuyển ra ngoài ranh giới khối vuông
                        double distSq = e.position().distanceToSqr(center);
                        if (distSq > 2.8D * 2.8D) {
                            Vec3 pull = center.subtract(e.position()).normalize().scale(0.85D);
                            e.setDeltaMovement(pull.x, 0.1D, pull.z);
                            e.hasImpulse = true;
                        }

                        // Sau khi bùng lửa đen: Thiêu đốt liên tục bằng lửa đen thật (bỏ qua kháng lửa)
                        if (hf.ticksAlive >= fireSurgeTick) {
                            e.hurt(level.damageSources().inFire(), 4.0F); // 16 damage/s
                            e.setRemainingFireTicks(100);
                        }
                    }
                }

                // =====================================================================
                // ĐÒN KẾT THÚC: BẾ MẠC SỤP ĐỔ NÉN HƯ VÔ (FINAL IMPLOSION AT TICK 80)
                // =====================================================================
                if (hf.ticksAlive >= hf.maxTicks) {
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 3, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 2, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 4, 0, 0, 0, 0);
                    level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.2F);
                    level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 0.8F);

                    AABB blastBox = new AABB(center.x - 5.5D, center.y - 5.5D, center.z - 5.5D,
                                             center.x + 5.5D, center.y + 5.5D, center.z + 5.5D);
                    List<LivingEntity> blastVictims = level.getEntitiesOfClass(LivingEntity.class, blastBox, LivingEntity::isAlive);

                    float baseFinalDamage = 70.0F;

                    for (LivingEntity victim : blastVictims) {
                        if (victim == hf.caster) continue;

                        // QUY TẮC MIỄN TRỪ SÁT THƯƠNG KẾT THÚC:
                        // 1. Long Chủng: Velgrynd, Velzard -> 0%
                        if (victim instanceof VelgryndEntity || victim instanceof VelzardEntity) {
                            continue;
                        }
                        // 2. Thủy Tổ: Guy (Rouge), Blanc, Noir -> 0%
                        if (victim instanceof PrimordialDemonEntity demon) {
                            DemonType type = demon.getDemonType();
                            if (type == DemonType.ROUGE || type == DemonType.BLANC || type == DemonType.NOIR) {
                                continue;
                            }
                        }
                        // 3. Milim -> 0%
                        if (victim instanceof MilimEntity) {
                            continue;
                        }
                        // 4. Marbas -> 0%
                        if (victim instanceof MarbasEntity) {
                            continue;
                        }
                        // 5. Cứu Thế Quyền Hiện -> Chịu 30% sát thương
                        if (victim instanceof CuuTheQuyenHienEntity) {
                            victim.hurt(level.damageSources().magic(), baseFinalDamage * 0.30F);
                            continue;
                        }

                        // Mọi thực thể khác: Chịu 100% sát thương cực hạn
                        victim.hurt(level.damageSources().magic(), baseFinalDamage);
                        victim.setRemainingFireTicks(160);
                    }

                    hf.cleanup();
                    it.remove();
                }
            }
        }
    }

    public static void cancelAll(ServerLevel level) {
        synchronized (ACTIVE_HELLFLARES) {
            Iterator<ActiveHellflare> it = ACTIVE_HELLFLARES.iterator();
            while (it.hasNext()) {
                ActiveHellflare hf = it.next();
                if (hf.level == level) {
                    hf.cleanup();
                    it.remove();
                }
            }
        }
    }
}

