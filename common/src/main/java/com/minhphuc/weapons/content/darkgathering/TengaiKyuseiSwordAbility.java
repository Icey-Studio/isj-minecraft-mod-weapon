package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.network.ClientboundTengaiSlashEffectPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * BỘ KIẾM KỸ: THIÊN NGOẠI CỨU THẾ QUYỀN HIỆN (TENGAI GUZE GONGEN)
 * TỐI ƯU HÓA HIỆU NĂNG CAO ĐỘ (ZERO LAG):
 * - Kỹ năng: Nghịch Thời Nhất Trảm (-0.04s ➔ 0.00s)
 * - Cơ chế chuẩn nguyên tác Dark Gathering:
 *   1. Người chơi ĐỨNG YÊN TẠI CHỖ (không dịch chuyển, không dash).
 *   2. Tạo ra 1 PHÂN THÂN TÀN ẢNH mang SKIN CỦA CHÍNH NGƯỜI CHƠI lao nhanh tới chém mục tiêu.
 *   3. Toàn bộ thế giới chuyển sang TRẮNG ĐEN (Monochrome Manga Shader) trong lúc đếm ngược.
 *   4. Mục tiêu bị ĐÓNG BĂNG KHÔNG GIAN, KHÔNG NHẬN SÁT THƯƠNG trong lúc thế giới đen trắng.
 *   5. Đúng tại thời khắc 0.00s: MÀU SẮC THẾ GIỚI TRỞ LẠI -> SÁT THƯƠNG 150 MỚI PHÁT NỔ TUNG MỤC TIÊU!
 *   6. Không thời gian hồi chiêu (Zero Cooldown).
 */
public class TengaiKyuseiSwordAbility {

    public static class ActiveReverseSlash {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 startPos;
        public final Vec3 targetPos;
        public final LivingEntity targetEntity;
        public final ArmorStand shadowStand;
        public final float shadowYaw;
        public int currentTick = 0;
        public static final int TOTAL_TICKS = 16; // 16 ticks = 0.8s tương ứng đếm ngược -0.04s ➔ 0.00s

        public ActiveReverseSlash(ServerLevel level, ServerPlayer caster, Vec3 startPos, Vec3 targetPos,
                                  LivingEntity targetEntity, ArmorStand shadowStand, float shadowYaw) {
            this.level = level;
            this.caster = caster;
            this.startPos = startPos;
            this.targetPos = targetPos;
            this.targetEntity = targetEntity;
            this.shadowStand = shadowStand;
            this.shadowYaw = shadowYaw;
        }

        public void discard() {
            if (shadowStand != null && shadowStand.isAlive()) {
                shadowStand.discard();
            }
        }
    }

    public static final List<ActiveReverseSlash> ACTIVE_SLASHES = new ArrayList<>();

    public static boolean canUseTengaiTechniques(Player player) {
        if (player == null) return false;
        boolean isWearingDivine = DivineArmorItem.isWearingAnyPiece(player);
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        return isWearingDivine || isTrueDemonLord;
    }

    // =========================================================================
    // NGHỊCH THỜI NHẤT TRẢM (TIME-REVERSAL SLASH) - ZERO COOLDOWN
    // =========================================================================
    public static boolean castReverseTimeSlash(ServerLevel level, ServerPlayer player) {
        if (!canUseTengaiTechniques(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Yêu cầu trang bị Giáp Thần Linh hoặc thức tỉnh Chân Ma Vương để khai mở Nghịch Thời Nhất Trảm!"),
                    true
            );
            return false;
        }

        // Tìm mục tiêu quái vật/boss trong tầm nhìn 24 blocks
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = 24.0D;
        Vec3 endReach = eyePos.add(look.scale(reach));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0D);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator());

        LivingEntity chosenTarget = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity entity : potentialTargets) {
            AABB bb = entity.getBoundingBox().inflate(0.5D);
            Optional<Vec3> hit = bb.clip(eyePos, endReach);
            if (hit.isPresent()) {
                double distSq = eyePos.distanceToSqr(hit.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    chosenTarget = entity;
                }
            }
        }

        if (chosenTarget == null) {
            for (LivingEntity entity : potentialTargets) {
                Vec3 toTarget = entity.position().subtract(player.position()).normalize();
                if (toTarget.dot(look) > 0.65D) {
                    double distSq = player.distanceToSqr(entity);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        chosenTarget = entity;
                    }
                }
            }
        }

        Vec3 targetPos = (chosenTarget != null)
                ? chosenTarget.position().add(0, 0.5D, 0)
                : player.position().add(look.scale(16.0D));

        // 1. NGƯỜI CHƠI ĐỨNG YÊN HOÀN TOÀN TẠI CHỖ
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;

        // Tính góc xoay hướng về mục tiêu
        Vec3 dir = targetPos.subtract(player.position());
        float shadowYaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;

        // 2. TẠO ẢO ẢNH BÓNG MA (GHOST PHANTOM SILHOUETTE)
        ArmorStand shadow = EntityType.ARMOR_STAND.create(level);
        if (shadow != null) {
            shadow.moveTo(player.getX(), player.getY(), player.getZ(), shadowYaw, 0.0F);
            shadow.setNoGravity(true);
            shadow.setInvulnerable(true);
            shadow.setSilent(true);
            shadow.setShowArms(true);
            shadow.setNoBasePlate(true);
            shadow.setInvisible(true); // Ẩn thân gỗ để chỉ hiển thị hình dạng ảo ảnh bóng ma!
            shadow.setGlowingTag(true); // Hào quang trắng rực sáng manga bao bọc toàn bộ bóng ma!

            // Màu hắc ám bóng ma huyền bí (Deep Phantom Void)
            int phantomColor = 0x12141D;

            // Đầu lâu bóng ma hắc ám với ánh sáng holographic ảo ảnh
            ItemStack phantomSkull = new ItemStack(Items.WITHER_SKELETON_SKULL);
            phantomSkull.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            shadow.setItemSlot(EquipmentSlot.HEAD, phantomSkull);

            // Giáp ảo ảnh bóng ma phủ toàn thân (Có ánh sáng holographic huyền ảo)
            ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
            chest.set(DataComponents.DYED_COLOR, new DyedItemColor(phantomColor, false));
            chest.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            shadow.setItemSlot(EquipmentSlot.CHEST, chest);

            ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
            legs.set(DataComponents.DYED_COLOR, new DyedItemColor(phantomColor, false));
            legs.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            shadow.setItemSlot(EquipmentSlot.LEGS, legs);

            ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
            boots.set(DataComponents.DYED_COLOR, new DyedItemColor(phantomColor, false));
            boots.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            shadow.setItemSlot(EquipmentSlot.FEET, boots);

            // Vũ khí ảo ảnh đang cầm
            ItemStack weapon = player.getMainHandItem().copy();
            weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            shadow.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            shadow.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

            // Tạo dáng chém bạt đao lao nhanh về phía trước (Anime Slash Pose)
            shadow.setRightArmPose(new Rotations(-85.0F, -40.0F, 0.0F));
            shadow.setLeftArmPose(new Rotations(-20.0F, 20.0F, 0.0F));
            shadow.setBodyPose(new Rotations(18.0F, 0.0F, 0.0F)); // Nghiêng người lao tới
            shadow.setRightLegPose(new Rotations(-30.0F, 0.0F, 0.0F));
            shadow.setLeftLegPose(new Rotations(30.0F, 0.0F, 0.0F));

            level.addFreshEntity(shadow);
        }

        // 3. GỬI GÓI TIN CHUYỂN MÀN HÌNH THẾ GIỚI THÀNH TRẮNG ĐEN (MONOCHROME MANGA)
        // Gửi cho người tung chiêu và người chơi lân cận trong phạm vi 40 blocks
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceToSqr(player) <= 40 * 40) {
                ModMessages.sendToPlayer(new ClientboundTengaiSlashEffectPacket(ActiveReverseSlash.TOTAL_TICKS), sp);
            }
        }

        // 4. KHÓA ĐÓNG BĂNG MỤC TIÊU TRONG KHÔNG GIAN (CHƯA GÂY SÁT THƯƠNG)
        if (chosenTarget != null && chosenTarget.isAlive()) {
            chosenTarget.setDeltaMovement(Vec3.ZERO);
            chosenTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 255, false, false, false));
        }

        // 5. ĐÈN LED ĐỒNG HỒ ĐIỆN TỬ "-0.04"
        DigitalLedDisplayHelper.spawnLedText(level, targetPos.add(0, 1.8D, 0), "-0.04", 2.6F, 20);

        // Âm thanh ngưng đọng thời không
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 1.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.8F, 0.6F);

        // Hiển thị tiêu đề đếm ngược thời gian
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 18, 5));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§l天外救世権現の『剣技』")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.04s] §fThời Gian Ngưng Đọng...")));

        ActiveReverseSlash slash = new ActiveReverseSlash(level, player, player.position().add(0, 0.1D, 0), targetPos, chosenTarget, shadow, shadowYaw);
        ACTIVE_SLASHES.add(slash);
        return true;
    }

    private static void executeStageNegative002(ActiveReverseSlash slash) {
        ServerLevel level = slash.level;
        Vec3 midPos = slash.startPos.add(slash.targetPos).scale(0.5D);

        // Đèn LED 3D "-0.02"
        DigitalLedDisplayHelper.spawnLedText(level, midPos.add(0, 1.8D, 0), "-0.02", 2.6F, 16);

        // Cập nhật tiêu đề đếm ngược
        slash.caster.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e[-0.02s] §fVết Cắt Thời Gian...")));

        // Vệt chém Sweep Attack trắng và hạt nứt khối số hóa (TUYỆT ĐỐI CHƯA GÂY SÁT THƯƠNG)
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, midPos.x, midPos.y + 0.9D, midPos.z, 1, 0, 0, 0, 0.0D);
        level.sendParticles(SamuraiSpiritDisplayHelper.PURE_WHITE_DUST, midPos.x, midPos.y + 0.9D, midPos.z, 4, 0.2D, 0.3D, 0.2D, 0.02D);
        SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, slash.startPos, slash.targetPos);

        level.playSound(null, midPos.x, midPos.y, midPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 0.7F);
    }

    /**
     * GIAI ĐOẠN 0.00s:
     * - MÀU SẮC THẾ GIỚI TRỞ LẠI BÌNH THƯỜNG (Shader client hết hiệu lực ở tick 16).
     * - Tàn ảnh người chơi biến mất sau khi chém xuyên qua.
     * - SÁT THƯƠNG VÀ HIỆU ỨNG NỔ CHÉM BÂY GIỜ MỚI XUẤT HIỆN!
     */
    private static void executeStage000(ActiveReverseSlash slash) {
        ServerLevel level = slash.level;
        ServerPlayer player = slash.caster;
        Vec3 tPos = slash.targetPos;

        // Xóa tàn ảnh ArmorStand
        slash.discard();

        // Đèn LED 3D " 0.00"
        DigitalLedDisplayHelper.spawnLedText(level, tPos.add(0, 2.0D, 0), " 0.00", 2.6F, 16);

        // Tiếng tra kiếm đanh thép "Keng!"
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5F, 2.0F);
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 1.6F);
        level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.8F, 1.2F);

        // HIỆU ỨNG NỔ FLASH VÀ CHÉM CẮT ĐỊA HÌNH SỐ HÓA
        level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 1.0D, tPos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, tPos.x, tPos.y + 1.0D, tPos.z, 3, 0.4D, 0.4D, 0.4D, 0);
        level.sendParticles(SamuraiSpiritDisplayHelper.PURE_WHITE_DUST, tPos.x, tPos.y + 1.0D, tPos.z, 12, 0.5D, 0.5D, 0.5D, 0.08D);
        level.sendParticles(ParticleTypes.CRIT, tPos.x, tPos.y + 1.0D, tPos.z, 10, 0.4D, 0.4D, 0.4D, 0.15D);

        // SÁT THƯƠNG 150 CỰC ĐẠI VÀO MỤC TIÊU CHÍNH (BÂY GIỜ MỚI GÂY SÁT THƯƠNG)
        if (slash.targetEntity != null && slash.targetEntity.isAlive()) {
            if (slash.targetEntity instanceof com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity) {
                // Kháng 100% với Nghịch Thời Nhất Trảm và thông báo
                level.playSound(null, slash.targetEntity.getX(), slash.targetEntity.getY(), slash.targetEntity.getZ(),
                        SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 2.0F, 1.4F);
                level.playSound(null, slash.targetEntity.getX(), slash.targetEntity.getY(), slash.targetEntity.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.8F, 0.8F);
                level.sendParticles(ParticleTypes.CRIT, slash.targetEntity.getX(), slash.targetEntity.getY() + 1.2D, slash.targetEntity.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.15D);
                player.sendSystemMessage(Component.literal("§c§l[Báo cáo] §fCá thể Cứu Thế Quyền Hiện đã chặn đứng Nghịch Thời Nhất Trảm!"));
            } else {
                slash.targetEntity.invulnerableTime = 0;
                slash.targetEntity.hurt(level.damageSources().playerAttack(player), 150.0F);

                // Hất văng lùi lại phía sau
                Vec3 knockback = slash.targetPos.subtract(slash.startPos).normalize().scale(0.8D).add(0, 0.35D, 0);
                slash.targetEntity.setDeltaMovement(knockback);
            }
        }

        // Sát thương lan xung quanh mục tiêu trong phạm vi 5 blocks
        AABB blastBox = new AABB(tPos.x - 5, tPos.y - 2, tPos.z - 5, tPos.x + 5, tPos.y + 3, tPos.z + 5);
        List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, blastBox,
                e -> e != player && e != slash.targetEntity && e.isAlive() && !e.isSpectator()
                        && !(e instanceof com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity));

        for (LivingEntity mob : nearbyMobs) {
            mob.hurt(level.damageSources().playerAttack(player), 35.0F);
            Vec3 knockback = mob.position().subtract(tPos).normalize().scale(0.8D).add(0, 0.25D, 0);
            mob.setDeltaMovement(knockback);
        }

        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§l天外救世権現の『剣技』")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a[ 0.00s] §fTrảm Kích Hoàn Tất!")));
    }

    public static void tickAbilities(ServerLevel level) {
        DigitalLedDisplayHelper.tickLeds(level);
        SamuraiSpiritDisplayHelper.tickSpirits(level);

        if (!ACTIVE_SLASHES.isEmpty()) {
            Iterator<ActiveReverseSlash> it = ACTIVE_SLASHES.iterator();
            while (it.hasNext()) {
                ActiveReverseSlash slash = it.next();
                if (slash.level != level) continue;

                slash.currentTick++;

                // Giữ người chơi đứng yên trong suốt thời gian xuất chiêu
                if (slash.caster.isAlive()) {
                    slash.caster.setDeltaMovement(Vec3.ZERO);
                }

                // Cập nhật vị trí tàn ảnh lướt từ điểm xuất phát tới mục tiêu
                float progress = (float) slash.currentTick / (float) ActiveReverseSlash.TOTAL_TICKS;
                progress = Math.min(progress, 1.0F);
                Vec3 currentPos = slash.startPos.lerp(slash.targetPos, progress);

                if (slash.shadowStand != null && slash.shadowStand.isAlive()) {
                    slash.shadowStand.moveTo(currentPos.x, currentPos.y, currentPos.z, slash.shadowYaw, 0.0F);

                    // 1. Hào quang lửa ma quái trắng/xanh linh hồn quanh tàn ảnh (Manga flaming aura)
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, currentPos.x, currentPos.y + 1.1D, currentPos.z,
                            1, 0.12D, 0.25D, 0.12D, 0.02D);
                    level.sendParticles(SamuraiSpiritDisplayHelper.PURE_WHITE_DUST, currentPos.x, currentPos.y + 0.9D, currentPos.z,
                            2, 0.15D, 0.3D, 0.15D, 0.02D);

                    // 2. Các khối pixel số hóa vuông vức bay ra từ vết chém (Floating Digital Glitch Blocks giống trong truyện)
                    level.sendParticles(ParticleTypes.WAX_OFF, currentPos.x, currentPos.y + 0.8D, currentPos.z,
                            1, 0.15D, 0.15D, 0.15D, 0.02D);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, currentPos.x, currentPos.y + 0.8D, currentPos.z,
                            1, 0.1D, 0.2D, 0.1D, 0.02D);
                }

                // Đóng băng mục tiêu trong suốt thời gian chém
                if (slash.targetEntity != null && slash.targetEntity.isAlive()) {
                    slash.targetEntity.setDeltaMovement(Vec3.ZERO);
                }

                if (slash.currentTick == 8) {
                    // Giai đoạn -0.02s
                    executeStageNegative002(slash);
                } else if (slash.currentTick >= ActiveReverseSlash.TOTAL_TICKS) {
                    // Giai đoạn 0.00s (Màu sắc trở lại, sát thương nổ tung!)
                    executeStage000(slash);
                    it.remove();
                }
            }
        }
    }

    /**
     * Dọn dẹp sạch sẽ khi server tắt hoặc reload thế giới (Zero Memory Leak)
     */
    public static void cleanup(ServerLevel level) {
        if (!ACTIVE_SLASHES.isEmpty()) {
            Iterator<ActiveReverseSlash> it = ACTIVE_SLASHES.iterator();
            while (it.hasNext()) {
                ActiveReverseSlash slash = it.next();
                if (slash.level == level) {
                    slash.discard();
                    it.remove();
                }
            }
        }
    }
}
