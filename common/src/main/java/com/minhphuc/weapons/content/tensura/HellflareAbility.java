package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
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
 * 1. Khóa Không Gian (Spatial Lock):
 *    - Tạo ra một khối cầu kết giới Hắc Hỏa đen tuyền che khuất hoàn toàn tầm nhìn từ bên ngoài.
 *    - Người bên ngoài nhìn vào chỉ thấy một khối cầu lửa đen rực cháy, không thấy người bên trong.
 *    - Giam hãm mọi thực thể bên trong không thể chạy thoát trong 4 giây.
 * 2. Thiêu Đốt Tuyệt Nhiệt Bất Diệt:
 *    - Gây sát thương thiêu đốt liên tục, bỏ qua 100% kháng lửa của mục tiêu.
 * 3. Bế Mạc Sụp Đổ Nén Hư Vô (Final Implosion):
 *    - Nổ tung chân không cực hạn (70 sát thương).
 *    - Quy tắc miễn trừ chuẩn:
 *      + Long Chủng (Velgrynd, Velzard): 0% sát thương.
 *      + Guy (Rouge), Blanc, Noir: 0% sát thương.
 *      + Milim, Marbas: 0% sát thương.
 *      + Cứu Thế Quyền Hiện: chỉ chịu 30% sát thương.
 *      + Mọi thực thể khác: chịu 100% sát thương.
 * 4. Tối Ưu Hóa ZERO-LAG:
 *    - 1 ItemDisplay duy nhất, không đặt/phá block lửa trong thế giới, quét O(1).
 */
public class HellflareAbility {

    public static class ActiveHellflare {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public Display.ItemDisplay sphereDisplay;
        public int ticksAlive = 0;
        public final int maxTicks = 80; // 4 giây

        public ActiveHellflare(ServerLevel level, ServerPlayer caster, Vec3 center) {
            this.level = level;
            this.caster = caster;
            this.center = center;
        }

        public void cleanup() {
            if (sphereDisplay != null && sphereDisplay.isAlive()) {
                sphereDisplay.discard();
                sphereDisplay = null;
            }
        }
    }

    private static final List<ActiveHellflare> ACTIVE_HELLFLARES = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(20.0D));

        BlockHitResult hit = level.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            targetPos = hit.getLocation();
        }

        ActiveHellflare instance = new ActiveHellflare(level, player, targetPos);

        // Tạo màng chắn Display đen tuyền che kín hoàn toàn tầm nhìn từ bên ngoài (9x9x9 block)
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
            ItemDisplayAccessor iAcc = (ItemDisplayAccessor) display;
            DisplayAccessor dAcc = (DisplayAccessor) display;

            iAcc.weapons$setItemStack(new ItemStack(Blocks.BLACK_CONCRETE));
            iAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            dAcc.weapons$setViewRange(8.0F);

            // Scale 9 block che kín bên trong
            dAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(9.0F, 9.0F, 9.0F),
                    null
            ));

            level.addFreshEntity(display);
            instance.sphereDisplay = display;
        }

        ACTIVE_HELLFLARES.add(instance);

        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 3.5F, 0.6F);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.5F, 1.4F);

        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l【 HẮC VIÊM NGỤC: HELLFLARE 】")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c✦ Không Gian Hắc Hỏa Giam Hãm Tuyệt Đối (4s) ✦")));

        player.displayClientMessage(
                Component.literal("§4§l[BENIMARU - HELLFLARE] §cĐã phóng Hắc Viêm Ngục khóa kín không gian mục tiêu! 🔥⬛"),
                true
        );
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
                double radius = 4.5D;

                // 1. SINH HẠT LỬA ĐEN VÀ KHÓI ĐEN BAO PHỦ MẶT NGOÀI KẾT GIỚI (MỖI 2 TICKS)
                if (hf.ticksAlive % 2 == 0) {
                    for (int a = 0; a < 360; a += 45) {
                        double rad = Math.toRadians(a);
                        double px = center.x + Math.cos(rad) * radius;
                        double pz = center.z + Math.sin(rad) * radius;
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, center.y + level.random.nextDouble() * 4.0D - 2.0D, pz, 2, 0.2D, 0.4D, 0.2D, 0.05D);
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, px, center.y + level.random.nextDouble() * 4.0D - 2.0D, pz, 1, 0.1D, 0.2D, 0.1D, 0.02D);
                        level.sendParticles(ParticleTypes.DRAGON_BREATH, px, center.y + 0.5D, pz, 1, 0.1D, 0.3D, 0.1D, 0.01D);
                    }
                }

                // 2. GIAM HÃM VÀ THIÊU ĐỐT LIÊN TỤC MỤC TIÊU BÊN TRONG (MỖI 5 TICKS = 0.25S)
                if (hf.ticksAlive % 5 == 0) {
                    AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                                        center.x + radius, center.y + radius, center.z + radius);
                    List<LivingEntity> trapped = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);

                    for (LivingEntity e : trapped) {
                        if (e == hf.caster) continue;

                        // Kéo ngược về tâm nếu cố gắng chạy ra ngoài ranh giới
                        double distSq = e.position().distanceToSqr(center);
                        if (distSq > 3.6D * 3.6D) {
                            Vec3 pull = center.subtract(e.position()).normalize().scale(0.75D);
                            e.setDeltaMovement(pull.x, 0.1D, pull.z);
                            e.hasImpulse = true;
                        }

                        // Thiêu đốt liên tục (bỏ qua kháng lửa)
                        e.hurt(level.damageSources().inFire(), 4.0F); // 16 damage/s
                        e.setRemainingFireTicks(60);
                    }
                }

                // 3. ĐÒN KẾT THÚC: BẾ MẠC SỤP ĐỔ NÉN HƯ VÔ (FINAL IMPLOSION AT TICK 80)
                if (hf.ticksAlive >= hf.maxTicks) {
                    // Âm thanh và hiệu ứng bộc phá nén cực đại
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

                        // Mọi thực thể khác (Người chơi khác, quái vật thông thường): Chịu 100% sát thương cực hạn
                        victim.hurt(level.damageSources().magic(), baseFinalDamage);
                        victim.setRemainingFireTicks(160);
                    }

                    hf.cleanup();
                    it.remove();
                }
            }
        }
    }
}
