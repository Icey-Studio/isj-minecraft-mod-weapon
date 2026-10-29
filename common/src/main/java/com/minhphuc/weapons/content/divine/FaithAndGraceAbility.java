package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Tuyệt Kỹ: Bí Ẩn Của Đức Tin Và Ân Sủng (Mystery of Faith and Grace)
 * - Đứng ngay sau Phép Màu Của Thần Linh trong bảng kỹ năng.
 * - Hiệu ứng tương tự Phép Màu Của Thần Linh nhưng:
 *   + Không tồn tại vĩnh viễn: chỉ tồn tại 5 giây (100 ticks).
 *   + Khu vực cực rộng: 40x40 block (bán kính 20m).
 *   + Cột sáng cao chọc trời (~70 block).
 *   + Hiệu ứng chiếu sáng từ trên trời giáng xuống đất từ từ (descending beam) trong 15 ticks đầu.
 */
public class FaithAndGraceAbility {

    private static final DustParticleOptions GOLDEN_LIGHT = new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.4F), 1.8F);
    private static final DustParticleOptions CYAN_HOLY = new DustParticleOptions(new Vector3f(0.3F, 0.95F, 1.0F), 1.6F);

    public static class ActiveFaithAndGrace {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public Display.ItemDisplay pillarDisplay;
        public int ticksAlive = 0;
        public final int maxTicks = 100; // 5 giây
        public boolean fullyLanded = false;

        public ActiveFaithAndGrace(ServerLevel level, ServerPlayer caster, Vec3 center) {
            this.level = level;
            this.caster = caster;
            this.center = center;
        }

        public void cleanup() {
            if (pillarDisplay != null && pillarDisplay.isAlive()) {
                pillarDisplay.discard();
                pillarDisplay = null;
            }
        }
    }

    private static final List<ActiveFaithAndGrace> ACTIVE_INSTANCES = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetCenter = eyePos.add(lookVec.scale(8.0D));

        // Tìm mặt đất tại vị trí mục tiêu
        BlockPos groundPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(targetCenter));
        Vec3 groundCenter = new Vec3(targetCenter.x, groundPos.getY(), targetCenter.z);

        ActiveFaithAndGrace instance = new ActiveFaithAndGrace(level, player, groundCenter);
        ACTIVE_INSTANCES.add(instance);

        // Âm thanh khởi phát: Tiếng thánh ca thiên đường ngân vang
        level.playSound(null, groundCenter.x, groundCenter.y + 10.0D, groundCenter.z,
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 4.0F, 0.8F);
        level.playSound(null, groundCenter.x, groundCenter.y + 20.0D, groundCenter.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.6F);

        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l【 BÍ ẨN CỦA ĐỨC TIN VÀ ÂN SỦNG 】")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e✦ Thánh Quang Cứu Thế Giáng Lâm 40x40 Block (5s) ✦")));

        player.displayClientMessage(
                Component.literal("§6§l[ĐỨC TIN & ÂN SỦNG] §fThánh quang 40x40 block đang từ thiên giới giáng hạ cứu rỗi vạn vật! ✨🕊️"),
                true
        );
    }

    public static void tickFaithAndGraces(ServerLevel level) {
        synchronized (ACTIVE_INSTANCES) {
            Iterator<ActiveFaithAndGrace> it = ACTIVE_INSTANCES.iterator();
            while (it.hasNext()) {
                ActiveFaithAndGrace instance = it.next();
                if (instance.level != level) continue;

                instance.ticksAlive++;

                Vec3 center = instance.center;
                double areaRadius = 20.0D; // 40x40 block
                double maxHeight = 70.0D;

                // GIAI ĐOẠN 1: Chiếu sáng từ trên trời giáng xuống đất (15 ticks đầu)
                if (instance.ticksAlive <= 15) {
                    double progress = (double) instance.ticksAlive / 15.0D; // 0.0 -> 1.0
                    double currentBeamBottom = center.y + (1.0D - progress) * maxHeight;

                    // Hạt ánh sáng thánh khiết lao thẳng từ trời xuống
                    for (int i = 0; i < 8; i++) {
                        double ox = (level.random.nextDouble() - 0.5D) * 6.0D;
                        double oz = (level.random.nextDouble() - 0.5D) * 6.0D;
                        level.sendParticles(ParticleTypes.END_ROD, center.x + ox, currentBeamBottom + level.random.nextDouble() * 5.0D, center.z + oz, 1, 0, -1.5D, 0, 0.1D);
                        level.sendParticles(GOLDEN_LIGHT, center.x + ox, currentBeamBottom, center.z + oz, 1, 0, -1.0D, 0, 0.05D);
                    }

                    if (instance.ticksAlive == 15) {
                        // Tiếp đất! Tạo ItemDisplay cột sáng khổng lồ 40x40
                        instance.fullyLanded = true;
                        instance.pillarDisplay = createPillarDisplay(level, center, 20.0F, (float) maxHeight, 20.0F, 0xFFDF00);

                        level.playSound(null, center.x, center.y, center.z,
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.4F);
                        level.playSound(null, center.x, center.y, center.z,
                                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.5F, 1.1F);

                        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0D, center.z, 3, 0.5D, 0.5D, 0.5D, 0);
                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0D, center.z, 1, 0, 0, 0, 0);
                    }
                } else {
                    // GIAI ĐOẠN 2: Thánh Quang rực rỡ bao phủ toàn bộ vùng 40x40
                    if (instance.ticksAlive % 4 == 0) {
                        // Sinh hạt hào quang viền quanh vùng 40x40
                        double angle = level.random.nextDouble() * Math.PI * 2;
                        double r = level.random.nextDouble() * areaRadius;
                        double px = center.x + Math.cos(angle) * r;
                        double pz = center.z + Math.sin(angle) * r;
                        level.sendParticles(ParticleTypes.END_ROD, px, center.y + 0.2D, pz, 1, 0, 0.2D, 0, 0.02D);
                        level.sendParticles(CYAN_HOLY, px, center.y + 0.5D, pz, 1, 0, 0.1D, 0, 0.01D);
                    }

                    // Tác động lên mọi thực thể trong vùng 40x40 block (mỗi 10 ticks = 0.5s)
                    if (instance.ticksAlive % 10 == 0) {
                        AABB box = new AABB(
                                center.x - areaRadius, center.y - 10, center.z - areaRadius,
                                center.x + areaRadius, center.y + maxHeight, center.z + areaRadius
                        );
                        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);

                        for (LivingEntity e : entities) {
                            if (isIrredeemablyEvil(e)) {
                                // Boss tà ác bị cự tuyệt và đẩy lùi ra ngoài vùng thánh quang
                                Vec3 diff = e.position().subtract(center.x, center.y, center.z);
                                Vec3 repel = new Vec3(diff.x, 0, diff.z).normalize().scale(1.2D).add(0, 0.25D, 0);
                                e.setDeltaMovement(repel);
                                e.hasImpulse = true;
                                continue;
                            }

                            // Chuyển hóa Zombie / Witch
                            if (e instanceof ZombieVillager zv) {
                                zv.convertTo(EntityType.VILLAGER, true);
                                continue;
                            } else if (e instanceof Zombie z && !(z instanceof ZombifiedPiglin)) {
                                z.convertTo(EntityType.VILLAGER, false);
                                continue;
                            } else if (e instanceof Witch w) {
                                w.convertTo(EntityType.VILLAGER, false);
                                continue;
                            } else if (e instanceof ZombifiedPiglin zp) {
                                zp.convertTo(EntityType.PIGLIN, true);
                                continue;
                            } else if (e instanceof AbstractSkeleton sk) {
                                sk.discard();
                                continue;
                            }

                            // Cứu rỗi & ban phước cho Người chơi / Thú cưng / Dân làng / Đồng minh
                            e.removeEffect(MobEffects.WITHER);
                            e.removeEffect(MobEffects.POISON);
                            e.removeEffect(MobEffects.DARKNESS);
                            e.removeEffect(MobEffects.BLINDNESS);
                            e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                            e.removeEffect(MobEffects.WEAKNESS);
                            e.removeEffect(MobEffects.BAD_OMEN);
                            e.removeEffect(MobEffects.HUNGER);
                            e.setRemainingFireTicks(0);

                            e.heal(6.0F); // Hồi 3 tim mỗi nửa giây
                            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 3, false, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2, false, false, true));

                            if (e instanceof Player p) {
                                p.getFoodData().setFoodLevel(20);
                                p.getFoodData().setSaturation(20.0F);
                            }
                        }
                    }
                }

                // Kết thúc sau 100 ticks (5 giây)
                if (instance.ticksAlive >= instance.maxTicks) {
                    instance.cleanup();
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.5F);
                    it.remove();
                }
            }
        }
    }

    private static boolean isIrredeemablyEvil(LivingEntity e) {
        return e instanceof WitherBoss
                || e instanceof Warden
                || e instanceof EnderDragon
                || e instanceof ElderGuardian
                || (e.getMaxHealth() >= 200.0F && !(e instanceof net.minecraft.world.entity.animal.IronGolem));
    }

    private static Display.ItemDisplay createPillarDisplay(ServerLevel level, Vec3 center,
                                                          float scaleX, float scaleY, float scaleZ,
                                                          int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(ModItems.JACOB_LIGHT_PILLAR.get()));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(24.0F);

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
}
