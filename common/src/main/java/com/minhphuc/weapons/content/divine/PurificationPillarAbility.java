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
import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.network.ClientboundSyncEvolutionPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tuyệt kĩ: Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification - Holy Salvation)
 * Đặc tính:
 * 1. KHÔNG gây bất kỳ sát thương nào.
 * 2. Hồi phục toàn diện: Ban Regeneration, Health, Absorption, Saturation và xóa bỏ mọi hiệu ứng xấu cho Player/Pet/Villager/Friendly mobs.
 * 3. Cải tà quy chánh (Entity Conversion):
 *    - Zombie Villager, Zombie, Husk, Drowned -> Biến đổi hoàn lương trở lại thành Dân Làng (Villager).
 *    - Witch (Phù thủy) -> Thanh tẩy tà thuật, biến thành Villager.
 *    - Zombified Piglin -> Biến thành Piglin thuần lương.
 *    - Skeleton / Stray -> Giải thoát linh hồn trong ánh sáng thanh thản.
 * 4. Cự tuyệt quái vật tà ác:
 *    - Boss tà ác (Wither, Warden, Ender Dragon, Elder Guardian): Ánh sáng từ chối cứu rỗi, đẩy lùi ra ngoài vùng thánh quang!
 */
public class PurificationPillarAbility {

    public static class ActivePurification {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public Display.ItemDisplay pillarDisplay;
        public int ticksRemaining;
        public final int totalTicks;

        public ActivePurification(ServerLevel level, ServerPlayer caster, Vec3 center, int durationTicks) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.ticksRemaining = durationTicks;
            this.totalTicks = durationTicks;
        }

        public void cleanupDisplays() {
            if (pillarDisplay != null && pillarDisplay.isAlive()) {
                pillarDisplay.discard();
                pillarDisplay = null;
            }
        }
    }

    private static final List<ActivePurification> ACTIVE_PURIFICATIONS = new ArrayList<>();

    public static void cast(ServerLevel level, ServerPlayer player, ItemStack sword) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        double maxDistance = 26.0D;
        Vec3 traceEnd = eyePos.add(lookVec.scale(maxDistance));

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
                Vec3 forwardPos = eyePos.add(lookVec.scale(16.0D));
                targetCenter = findGroundBelow(level, forwardPos);
            }
        }

        // Tạo cột sáng thánh tẩy xanh ngọc bích cứu rỗi
        ActivePurification ap = new ActivePurification(level, player, targetCenter, 100); // 5 giây duy trì
        ap.pillarDisplay = createPillarDisplay(level, targetCenter, 5.0F, 50.0F, 5.0F, 0x55FFAA);

        ACTIVE_PURIFICATIONS.add(ap);

        // Thanh tẩy tức thì các vòng tròn ma thuật tàn dư tại vị trí chiếu tới
        com.minhphuc.weapons.content.tensura.ResidualMagicCircleManager.purgeCirclesInArea(level, targetCenter, 6.0D, 50.0D);

        // Âm thanh thánh tích cứu rỗi vang dội
        level.playSound(null, targetCenter.x, targetCenter.y + 5.0D, targetCenter.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.7F);
        level.playSound(null, targetCenter.x, targetCenter.y + 5.0D, targetCenter.z,
                SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 3.5F, 1.2F);
        level.playSound(null, targetCenter.x, targetCenter.y + 5.0D, targetCenter.z,
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 2.5F, 1.5F);

        player.displayClientMessage(
                Component.literal("§a§l[ĐẠI THÁNH TẨY] §fĐã triệu hồi Thánh Trụ Cứu Rỗi! Thanh tẩy tà niệm & Hồi sinh sinh linh! ✨🕊️"),
                true
        );

        // Hồi phục toàn bộ kỹ năng đã mất/tiêu hao do dung hợp cho người thi triển
        if (EvolvedSkillHelper.hasConsumedSkills(player)) {
            EvolvedSkillHelper.restoreAllConsumedSkills(player);
            ModMessages.sendToPlayer(
                    new ClientboundSyncEvolutionPacket(
                            new java.util.ArrayList<>(),
                            EvolvedSkillHelper.getEvolvedSkillId(player),
                            EvolvedSkillHelper.getEvolvedSkillTier(player)
                    ), player);

            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§l【 ĐẠI THÁNH TẨY CỨU RỖI 】")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e✦ Toàn bộ kỹ năng đã mất đã được hồi phục nguyên vẹn! ✦")));

            player.sendSystemMessage(Component.literal("§a══════════════════════════════════════════════════"));
            player.sendSystemMessage(Component.literal("§e✨ ĐẠI THÁNH TẨY: §aToàn bộ kỹ năng nguyên liệu đã tiêu hao đã được quang minh thanh tẩy & hồi phục nguyên vẹn!"));
            player.sendSystemMessage(Component.literal("§7(Giờ đây bạn có thể mở Sách Kết Hợp Kỹ Năng để tiếp tục sử dụng hoặc dung hợp)"));
            player.sendSystemMessage(Component.literal("§a══════════════════════════════════════════════════"));
        }

        player.getCooldowns().addCooldown(sword.getItem(), 160); // 8 giây cooldown
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

    public static void tickPillars(ServerLevel serverLevel) {
        if (ACTIVE_PURIFICATIONS.isEmpty()) return;

        Iterator<ActivePurification> it = ACTIVE_PURIFICATIONS.iterator();
        while (it.hasNext()) {
            ActivePurification p = it.next();
            if (p.level != serverLevel) continue;

            p.ticksRemaining--;
            int elapsed = p.totalTicks - p.ticksRemaining;
            double groundY = p.center.y;

            // Hạt thánh quang ngọc bích rơi từ trời xuống đất
            if (elapsed % 2 == 0) {
                for (int y = 0; y < 8; y++) {
                    double py = groundY + (y * 4.0D);
                    p.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.center.x, py, p.center.z, 2, 1.2D, 0.3D, 1.2D, 0.02D);
                    p.level.sendParticles(ParticleTypes.END_ROD, p.center.x, py, p.center.z, 1, 1.5D, 0.2D, 1.5D, 0.01D);
                }
            }

            // Quét và thực hiện cứu rỗi mỗi 8 ticks (0.4 giây)
            if (elapsed % 8 == 0) {
                // Thanh tẩy và xóa sạch toàn bộ các vòng tròn ma thuật tàn dư do mob để lại
                int purgedCircles = com.minhphuc.weapons.content.tensura.ResidualMagicCircleManager.purgeCirclesInArea(p.level, p.center, 5.5D, 40.0D);
                if (purgedCircles > 0 && p.caster != null) {
                    p.caster.displayClientMessage(
                            Component.literal("§a§l[ĐẠI THÁNH TẨY] §eĐã thanh tẩy & hóa giải " + purgedCircles + " vòng tròn ma thuật tàn dư của ma quái! ✨🕊️"),
                            true
                    );
                }

                AABB salvationBox = new AABB(p.center.x - 4.5D, groundY - 1.0D, p.center.z - 4.5D,
                        p.center.x + 4.5D, groundY + 40.0D, p.center.z + 4.5D);

                List<LivingEntity> entities = p.level.getEntitiesOfClass(LivingEntity.class, salvationBox, Entity::isAlive);

                for (LivingEntity e : entities) {
                    // =============================================================
                    // 1. CỰ TUYỆT SINH VẬT QUÁ TÀ ÁC (BOSSES TÀ ÁC)
                    // =============================================================
                    // 1.5. ĐẠI THÁNH TẨY TIÊU DIỆT TỨC THÌ KHÔNG VONG (INSTAKILL KŪBŌ)
                    // =============================================================
                    if (e instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity kubo) {
                        p.level.sendParticles(ParticleTypes.FLASH, kubo.getX(), kubo.getY() + 1.0D, kubo.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.0D);
                        p.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, kubo.getX(), kubo.getY() + 1.0D, kubo.getZ(), 50, 0.6D, 0.6D, 0.6D, 0.25D);
                        p.level.playSound(null, kubo.getX(), kubo.getY(), kubo.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.5F, 1.4F);
                        kubo.addTag("PurificationDamage");
                        kubo.hurt(p.caster != null ? p.level.damageSources().playerAttack(p.caster) : p.level.damageSources().magic(), kubo.getMaxHealth() * 3.0F);
                        kubo.removeTag("PurificationDamage");
                        if (kubo.isAlive()) {
                            kubo.discard();
                        }
                        if (p.caster != null) {
                            p.caster.displayClientMessage(
                                    Component.literal("§a§l[ĐẠI THÁNH TẨY] §6Ánh sáng thần thánh đã thanh tẩy tức thì Tà Thần Không Vong! ✨🕊️"),
                                    true
                            );
                        }
                        continue;
                    }

                    // =============================================================
                    if (isIrredeemablyEvil(e)) {
                        // Không hồi máu, đẩy lùi ra khỏi cột sáng
                        Vec3 diff = e.position().subtract(p.center.x, groundY, p.center.z);
                        Vec3 repel = new Vec3(diff.x, 0, diff.z).normalize().scale(0.85D).add(0, 0.2D, 0);
                        e.setDeltaMovement(repel);
                        e.hasImpulse = true;

                        p.level.sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY() + 1.0D, e.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);

                        if (elapsed % 24 == 0) {
                            p.level.playSound(null, e.getX(), e.getY(), e.getZ(),
                                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
                            if (p.caster != null) {
                                p.caster.displayClientMessage(
                                        Component.literal("§c§l[ĐẠI THÁNH TẨY] §7Tà niệm của " + e.getDisplayName().getString() + " quá sâu nặng, ánh sáng cự tuyệt cứu rỗi!"),
                                        true
                                );
                            }
                        }
                        continue;
                    }

                    // =============================================================
                    // 2. CẢI TÀ QUY CHÁNH: CHUYỂN HÓA CÁC XÁC SỐNG & PHÙ THỦY THÀNH DÂN LÀNG
                    // =============================================================
                    if (e instanceof ZombieVillager zv) {
                        Mob converted = zv.convertTo(EntityType.VILLAGER, true);
                        if (converted != null) {
                            p.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, converted.getX(), converted.getY() + 1.0D, converted.getZ(), 15, 0.3D, 0.4D, 0.3D, 0.05D);
                            p.level.playSound(null, converted.getX(), converted.getY(), converted.getZ(),
                                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 2.0F, 1.0F);
                        }
                        continue;
                    } else if (e instanceof Zombie z && !(z instanceof ZombifiedPiglin)) {
                        // Zombie thường, Husk, Drowned -> Hoàn lương thành Dân Làng
                        Mob converted = z.convertTo(EntityType.VILLAGER, false);
                        if (converted != null) {
                            p.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, converted.getX(), converted.getY() + 1.0D, converted.getZ(), 20, 0.3D, 0.4D, 0.3D, 0.1D);
                            p.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, converted.getX(), converted.getY() + 1.0D, converted.getZ(), 15, 0.3D, 0.4D, 0.3D, 0.05D);
                            p.level.playSound(null, converted.getX(), converted.getY(), converted.getZ(),
                                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 2.0F, 1.2F);
                        }
                        continue;
                    } else if (e instanceof Witch witch) {
                        // Phù Thủy -> Thanh tẩy tà thuật thành Dân Làng Mục Sư
                        Mob converted = witch.convertTo(EntityType.VILLAGER, false);
                        if (converted != null) {
                            p.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, converted.getX(), converted.getY() + 1.0D, converted.getZ(), 25, 0.3D, 0.5D, 0.3D, 0.1D);
                            p.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, converted.getX(), converted.getY() + 1.0D, converted.getZ(), 15, 0.3D, 0.4D, 0.3D, 0.05D);
                            p.level.playSound(null, converted.getX(), converted.getY(), converted.getZ(),
                                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 2.0F, 1.3F);
                        }
                        continue;
                    } else if (e instanceof ZombifiedPiglin zp) {
                        // Heo Thây Ma -> Thanh tẩy lây nhiễm thành Piglin thuần lương
                        Mob converted = zp.convertTo(EntityType.PIGLIN, true);
                        if (converted != null) {
                            p.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, converted.getX(), converted.getY() + 1.0D, converted.getZ(), 15, 0.3D, 0.4D, 0.3D, 0.1D);
                            p.level.playSound(null, converted.getX(), converted.getY(), converted.getZ(),
                                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 2.0F, 1.4F);
                        }
                        continue;
                    } else if (e instanceof AbstractSkeleton sk) {
                        // Giải thoát linh hồn cho Skeleton / Stray
                        p.level.sendParticles(ParticleTypes.SOUL, sk.getX(), sk.getY() + 1.0D, sk.getZ(), 12, 0.2D, 0.4D, 0.2D, 0.02D);
                        p.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sk.getX(), sk.getY() + 1.0D, sk.getZ(), 10, 0.2D, 0.3D, 0.2D, 0.1D);
                        p.level.playSound(null, sk.getX(), sk.getY(), sk.getZ(),
                                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 1.6F);
                        sk.discard();
                        continue;
                    }

                    // =============================================================
                    // 3. HỒI PHỤC TOÀN DIỆN CHO NGƯỜI CHƠI & SINH VẬT ĐƯỢC CHIẾU VÀO
                    // =============================================================
                    // Xóa bỏ mọi debuff nguyền rủa/độc hại
                    e.removeEffect(MobEffects.WITHER);
                    e.removeEffect(MobEffects.POISON);
                    e.removeEffect(MobEffects.DARKNESS);
                    e.removeEffect(MobEffects.BLINDNESS);
                    e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    e.removeEffect(MobEffects.WEAKNESS);
                    e.removeEffect(MobEffects.BAD_OMEN);
                    e.removeEffect(MobEffects.HUNGER);
                    e.setRemainingFireTicks(0);

                    // Ban phát phước lành sinh mệnh
                    e.heal(6.0F); // Hồi 3 tim mỗi 8 ticks
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 3, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 2, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 1, false, false, true));

                    p.level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3D, e.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.02D);

                }
            }

            // =========================================================================
            // GIAI ĐOẠN KẾT THÚC (Tick >= 100)
            // =========================================================================
            if (elapsed >= 100) {
                p.level.sendParticles(ParticleTypes.POOF, p.center.x, groundY + 1.0D, p.center.z, 10, 1.2D, 0.4D, 1.2D, 0.04D);
                p.level.playSound(null, p.center.x, groundY + 1.0D, p.center.z,
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);
                p.cleanupDisplays();
                it.remove();
            }
        }
    }

    /**
     * Xác định những sinh vật quá tà ác sẽ không được cứu rỗi
     */
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
            displayAcc.weapons$setViewRange(10.0F);

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
