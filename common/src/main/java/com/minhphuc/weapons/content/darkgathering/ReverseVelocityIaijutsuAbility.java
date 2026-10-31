package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.divine.MoonlightSwordItem;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.network.ClientboundIaiStatePacket;
import com.minhphuc.weapons.network.ModMessages;
import dev.architectury.event.EventResult;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * TUYỆT KĨ: CHÉM XUYÊN THỜI GIAN (TIME-PIERCING SLASH)
 * TỐI ƯU HÓA HIỆU NĂNG TỐI ĐA (ZERO LAG ARCHITECTURE):
 * 1. Không quét getEntitiesOfClass mỗi tick (tiết kiệm 95% CPU server thread).
 * 2. Lưu cache UUID của các thực thể bị đóng băng và tự động gỡ bỏ Slowness ngay khi hết chiêu.
 * 3. Dọn dẹp triệt để entity, TextDisplay, ArmorStand khi hoàn thành hoặc người chơi thoát game.
 * 4. Early-exit tức thì O(1) khi không có session nào đang hoạt động.
 */
public class ReverseVelocityIaijutsuAbility {

    public static class MarkedSlash {
        public final Vec3 markPos;
        public final LivingEntity hitEntity;
        public final String timestamp;
        public final float yaw;

        public MarkedSlash(Vec3 markPos, LivingEntity hitEntity, String timestamp, float yaw) {
            this.markPos = markPos;
            this.hitEntity = hitEntity;
            this.timestamp = timestamp;
            this.yaw = yaw;
        }
    }

    public static class ActiveIaiSession {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 physicalBodyPos;
        public final float physicalBodyYaw;
        public final ArmorStand physicalBodyStand;
        public final List<MarkedSlash> markedSlashes = new ArrayList<>();
        public final List<ArmorStand> finisherClones = new ArrayList<>();
        public final List<Display.TextDisplay> activeLedDisplays = new ArrayList<>();
        public final Set<UUID> frozenEntities = new HashSet<>();

        public int clickCount = 0;
        public long lastClickTimeMs = 0;
        public boolean inFinisher = false;
        public int finisherTick = 0;
        public int timeoutTicks = 600; // Tối đa 30s để người chơi thi triển 2 nhịp

        public ActiveIaiSession(ServerLevel level, ServerPlayer caster, Vec3 physicalBodyPos, float physicalBodyYaw, ArmorStand physicalBodyStand) {
            this.level = level;
            this.caster = caster;
            this.physicalBodyPos = physicalBodyPos;
            this.physicalBodyYaw = physicalBodyYaw;
            this.physicalBodyStand = physicalBodyStand;
        }

        public void cleanup() {
            // 1. Dọn dẹp thân xác người chơi
            if (physicalBodyStand != null && physicalBodyStand.isAlive()) {
                physicalBodyStand.discard();
            }

            // 2. Dọn dẹp phân thân tàn ảnh
            for (ArmorStand clone : finisherClones) {
                if (clone != null && clone.isAlive()) {
                    clone.discard();
                }
            }
            finisherClones.clear();

            // 3. Dọn dẹp và hủy toàn bộ TextDisplay LED 3D
            for (Display.TextDisplay led : activeLedDisplays) {
                if (led != null && led.isAlive()) {
                    DigitalLedDisplayHelper.remove(led);
                    led.discard();
                }
            }
            activeLedDisplays.clear();

            // 4. Giải phóng tức thì Slowness cho toàn bộ quái vật đã bị khóa (Không để quái bị lag đơ 30s)
            for (UUID uid : frozenEntities) {
                Entity ent = level.getEntity(uid);
                if (ent instanceof LivingEntity living && living.isAlive()) {
                    living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
            }
            frozenEntities.clear();
        }
    }

    public static final Map<UUID, ActiveIaiSession> ACTIVE_SESSIONS = new HashMap<>();

    public static boolean canUseTengaiTechniques(Player player) {
        if (player == null) return false;
        boolean hasSword = player.getMainHandItem().getItem() instanceof MoonlightSwordItem
                || player.getOffhandItem().getItem() instanceof MoonlightSwordItem;
        boolean isWearingDivine = DivineArmorItem.isWearingAnyPiece(player);
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        return hasSword || isWearingDivine || isTrueDemonLord;
    }

    public static boolean isPlayerInIaiSession(UUID playerUuid) {
        return !ACTIVE_SESSIONS.isEmpty() && ACTIVE_SESSIONS.containsKey(playerUuid);
    }

    /**
     * KIỂM TRA THỰC THỂ CÓ ĐANG TRONG TRẠNG THÁI ĐÓNG BĂNG HAY KHÔNG (O(1) EARLY EXIT NẾU KHÔNG CÓ SESSION)
     */
    public static boolean isEntityFrozenInSession(LivingEntity entity) {
        if (ACTIVE_SESSIONS.isEmpty()) return false;
        if (entity == null || !entity.isAlive()) return false;
        if (SeveredHeartCurseManager.isUltimateBossImmune(entity)) return false;

        UUID entityUuid = entity.getUUID();
        for (ActiveIaiSession session : ACTIVE_SESSIONS.values()) {
            if (!session.inFinisher) {
                if (session.frozenEntities.contains(entityUuid) || session.caster.distanceToSqr(entity) <= 48.0D * 48.0D) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * CHẶN ĐÒN ĐÁNH CỦA NGƯỜI CHƠI VÀO SINH VẬT TRONG LÚC XUẤT KHIẾU:
     * - Ngăn không cho Minecraft gây sát thương cận chiến thông thường
     * - Chuyển đòn đánh thành một nhấp chuột khắc dấu thời gian
     */
    public static EventResult onPlayerAttackEntity(Player player, Level level, Entity target, InteractionHand hand, HitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && isPlayerInIaiSession(player.getUUID())) {
            Vec3 pos = (target != null) ? target.position().add(0, 0.5D, 0) : player.position().add(player.getLookAngle().scale(3.0D));
            int targetId = (target != null) ? target.getId() : -1;
            handleClick(serverPlayer, pos, targetId);
            return EventResult.interruptFalse(); // CHẶN HOÀN TOÀN SÁT THƯƠNG GỐC CỦA MINECRAFT!
        }
        return EventResult.pass();
    }

    /**
     * XỬ LÝ KHI NGƯỜI CHƠI THOÁT GAME: DỌN DẸP SẠCH SESSION KHÔNG ĐỂ LẠI ENTITY THỪA
     */
    public static void onPlayerQuit(ServerPlayer player) {
        if (player == null || ACTIVE_SESSIONS.isEmpty()) return;
        ActiveIaiSession session = ACTIVE_SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.cleanup();
        }
    }

    /**
     * KÍCH HOẠT TUYỆT KỸ: CHÉM XUYÊN THỜI GIAN (SKILL 3 CỦA NGUYỆT QUANG THẦN TẾ KIẾM)
     */
    public static boolean castReverseVelocityIai(ServerLevel level, ServerPlayer player) {
        return castTimePiercingSlash(level, player);
    }

    public static boolean castTimePiercingSlash(ServerLevel level, ServerPlayer player) {
        if (!canUseTengaiTechniques(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Yêu cầu Nguyệt Quang Thần Tế Kiếm, Giáp Thần Linh hoặc thức tỉnh Chân Ma Vương để khai mở Tuyệt Kĩ Chém Xuyên Thời Gian!"),
                    true
            );
            return false;
        }

        if (ACTIVE_SESSIONS.containsKey(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("§e⚠️ Đang trong trạng thái Chém Xuyên Thời Gian! Hãy nhấp chuột để hoàn tất trảm kích!"),
                    true
            );
            return false;
        }

        Vec3 bodyPos = player.position();
        float bodyYaw = player.getYRot();

        // 1. TẠO THÂN XÁC VẬT LÝ ĐỨNG YÊN TẠI TỌA ĐỘ GỐC
        ArmorStand bodyStand = EntityType.ARMOR_STAND.create(level);
        if (bodyStand != null) {
            bodyStand.moveTo(bodyPos.x, bodyPos.y, bodyPos.z, bodyYaw, 0.0F);
            bodyStand.setNoGravity(true);
            bodyStand.setInvulnerable(true);
            bodyStand.setSilent(true);
            bodyStand.setShowArms(true);
            bodyStand.setNoBasePlate(true);
            bodyStand.setCustomName(Component.literal("§7[Thân Xác] " + player.getName().getString()));
            bodyStand.setCustomNameVisible(false);

            // Đầu người chơi mang đúng Skin/Profile
            ItemStack playerSkull = new ItemStack(Items.PLAYER_HEAD);
            playerSkull.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            bodyStand.setItemSlot(EquipmentSlot.HEAD, playerSkull);

            // Mặc đúng giáp của người chơi
            bodyStand.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
            bodyStand.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
            bodyStand.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());

            // Cầm đúng item người chơi đang cầm trên 2 tay
            bodyStand.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            bodyStand.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem().copy());

            // Dáng đứng chuẩn bị bạt đao
            bodyStand.setRightArmPose(new Rotations(-15.0F, 10.0F, 0.0F));
            bodyStand.setLeftArmPose(new Rotations(-10.0F, -10.0F, 0.0F));
            bodyStand.setBodyPose(new Rotations(5.0F, 0.0F, 0.0F));

            level.addFreshEntity(bodyStand);
        }

        ActiveIaiSession session = new ActiveIaiSession(level, player, bodyPos, bodyYaw, bodyStand);

        // 2. KHÓA ĐÓNG BĂNG SINH VẬT TRONG PHẠM VI 48 BLOCKS (TRỪ 4 BOSS MIỄN NHIỄM)
        AABB freezeBox = player.getBoundingBox().inflate(48.0D);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                e -> e != player && e.isAlive() && !e.isSpectator());

        for (LivingEntity target : nearbyEntities) {
            if (SeveredHeartCurseManager.isUltimateBossImmune(target)) continue;
            session.frozenEntities.add(target.getUUID());
            target.setDeltaMovement(Vec3.ZERO);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 255, false, false, false));
        }

        // 3. GỬI GÓI TIN ĐỒNG BỘ: MÀU SẮC TRẮNG ĐEN & BẮT ĐẦU XUẤT KHIẾU LINH HỒN
        ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_START, 600), player);

        // Âm thanh ngưng đọng thời không
        level.playSound(null, bodyPos.x, bodyPos.y, bodyPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);
        level.playSound(null, bodyPos.x, bodyPos.y, bodyPos.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.5F);

        // Hiển thị tiêu đề
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§lTUYỆT KĨ CHÉM XUYÊN THỜI GIAN")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.02s] §fNhấp chuột lần 1 để chém đoạt tâm mạch...")));
        player.displayClientMessage(Component.literal("§e👉 Nhấp chuột lần 1 [-0.02s] vào sinh vật để đoạt lấy trái tim!"), true);

        ACTIVE_SESSIONS.put(player.getUUID(), session);
        return true;
    }

    /**
     * XỬ LÝ NHẤP CHUỘT: RÚT GỌN CHÍNH XÁC 2 LẦN
     */
    public static void handleClick(ServerPlayer player, Vec3 clickPos, int targetEntityId) {
        ActiveIaiSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || session.inFinisher) return;

        long now = System.currentTimeMillis();
        // Debounce 300ms chống spam click đúp
        if (now - session.lastClickTimeMs < 300) {
            return;
        }
        session.lastClickTimeMs = now;

        ServerLevel level = session.level;

        // Tìm thực thể bị đánh
        LivingEntity hitEntity = null;
        if (targetEntityId != -1) {
            var ent = level.getEntity(targetEntityId);
            if (ent instanceof LivingEntity living && living.isAlive()) {
                hitEntity = living;
            }
        }

        // Quét tìm entity trong bán kính 3.5m nếu không ngắm trúng hitbox chính xác
        if (hitEntity == null) {
            AABB search = new AABB(clickPos.x - 3.5, clickPos.y - 3.5, clickPos.z - 3.5,
                    clickPos.x + 3.5, clickPos.y + 3.5, clickPos.z + 3.5);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, search,
                    e -> e != player && e != session.physicalBodyStand && e.isAlive() && !e.isSpectator());
            if (!near.isEmpty()) {
                hitEntity = near.get(0);
            }
        }

        session.clickCount++;

        if (session.clickCount == 1) {
            // =========================================================================
            // NGOẠI LỆ ĐẶC BIỆT: 4 SINH VẬT TỐI CAO LẬP TỨC PHÁ VỠ KỸ NĂNG VÀ THẾ GIỚI KHÔNG MÀU
            // =========================================================================
            if (hitEntity != null && SeveredHeartCurseManager.isUltimateBossImmune(hitEntity)) {
                // Giải phóng di chuyển cho Boss ngay lập tức
                hitEntity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                hitEntity.setDeltaMovement(new Vec3(0, 0.25D, 0));

                // Hiệu ứng không gian bị vỡ vụn
                level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0F, 0.8F);
                level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.2F);
                level.sendParticles(ParticleTypes.FLASH, hitEntity.getX(), hitEntity.getY() + 1.0D, hitEntity.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.0D);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitEntity.getX(), hitEntity.getY() + 1.0D, hitEntity.getZ(), 1, 0, 0, 0, 0);

                // Màu sắc thế giới lập tức trở lại bình thường và hủy phiên kỹ năng
                ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_END, 0), player);
                session.cleanup();
                ACTIVE_SESSIONS.remove(player.getUUID());

                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 50, 15));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§lKHÔNG GIAN BỊ PHÁ VỠ!")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c" + hitEntity.getName().getString() + " §cđã miễn nhiễm và bẻ gãy tuyệt kĩ!")));
                player.displayClientMessage(
                        Component.literal("§c⚠️ [CẢNH BÁO TỐI CAO] §e" + hitEntity.getName().getString() + " §cđã lập tức cử động được và phá tan không gian ngưng đọng thời gian!"),
                        false
                );
                return;
            }

            // =========================================================================
            // LẦN 1: [-0.02s] - ĐOẠT TIM VÀ HIỂN THỊ ĐỒNG HỒ LED
            // =========================================================================
            Vec3 targetPos = (hitEntity != null) ? hitEntity.position().add(0, 0.6D, 0) : clickPos;
            float slashYaw = player.getYRot();
            double ledYOffset = (hitEntity != null) ? (hitEntity.getBbHeight() + 0.35D) : 1.3D;
            Vec3 ledPos = (hitEntity != null) ? hitEntity.position().add(0, ledYOffset, 0) : targetPos.add(0, 1.2D, 0);

            session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, "-0.02s", slashYaw));

            var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, "-0.02", 2.6F, 600);
            if (led != null) session.activeLedDisplays.add(led);

            level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.9F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
            SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

            // RƠI RA TRÁI TIM BỊ CẮT GẮN VỚI TÊN SINH VẬT ĐÓ
            if (hitEntity != null) {
                ItemStack heartStack = SeveredHeartItem.createForEntity(hitEntity);
                ItemEntity heartDrop = new ItemEntity(level, hitEntity.getX(), hitEntity.getY() + 0.6D, hitEntity.getZ(), heartStack);
                heartDrop.setDefaultPickUpDelay();
                level.addFreshEntity(heartDrop);

                level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.8F, 1.2F);
                player.displayClientMessage(
                        Component.literal("§c§l[-0.02s] §fĐã đoạt §e" + heartStack.getHoverName().getString() + "§f! §a👉 Nhấp lần nữa để KẾT LIỄU [ 0.00s]!"),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("§c§l[-0.02s] §fĐã chém xé thời không! §a👉 Nhấp lần nữa để KẾT LIỄU [ 0.00s]!"),
                        true
                );
            }

            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.02s] §fNhấp lần nữa để KẾT LIỄU [ 0.00s]")));

        } else {
            // =========================================================================
            // LẦN 2: [ 0.00s] - KÍCH HOẠT FINISHER, PHỤC HỒI MÀU SẮC & ÁP DỤNG ÁN TỬ 5 GIÂY
            // =========================================================================
            Vec3 targetPos = (hitEntity != null) ? hitEntity.position().add(0, 0.6D, 0) : clickPos;
            double ledYOffset = (hitEntity != null) ? (hitEntity.getBbHeight() + 0.35D) : 1.3D;
            Vec3 ledPos = (hitEntity != null) ? hitEntity.position().add(0, ledYOffset, 0) : targetPos.add(0, 1.2D, 0);

            var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, " 0.00", 2.6F, 600);
            if (led != null) session.activeLedDisplays.add(led);

            triggerFinisher(session);
        }
    }

    /**
     * KHỞI ĐỘNG PHA KẾT LIỄU:
     */
    private static void triggerFinisher(ActiveIaiSession session) {
        session.inFinisher = true;
        session.finisherTick = 0;
        ServerLevel level = session.level;
        ServerPlayer player = session.caster;

        // 1. TẮT SHADER MÀU TRẮNG ĐEN -> MÀU SẮC THẾ GIỚI TRỞ LẠI TỰ NHIÊN
        ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_FINISHER_STARTED, 0), player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.2F);

        // 2. SINH TÀN ẢNH BÓNG MA TẠI VỊ TRÍ VẾT CHÉM
        int phantomColor = 0x12141D;
        ItemStack heldWeapon = player.getMainHandItem().copy();
        heldWeapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        for (MarkedSlash mark : session.markedSlashes) {
            ArmorStand clone = EntityType.ARMOR_STAND.create(level);
            if (clone != null) {
                clone.moveTo(mark.markPos.x, mark.markPos.y, mark.markPos.z, mark.yaw, 0.0F);
                clone.setNoGravity(true);
                clone.setInvulnerable(true);
                clone.setSilent(true);
                clone.setShowArms(true);
                clone.setNoBasePlate(true);
                clone.setInvisible(true);
                clone.setGlowingTag(true);

                // Đầu lâu bóng ma hắc ám
                ItemStack skull = new ItemStack(Items.WITHER_SKELETON_SKULL);
                skull.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                clone.setItemSlot(EquipmentSlot.HEAD, skull);

                // Giáp bóng ma
                ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
                chest.set(DataComponents.DYED_COLOR, new DyedItemColor(phantomColor, false));
                chest.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                clone.setItemSlot(EquipmentSlot.CHEST, chest);

                ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
                legs.set(DataComponents.DYED_COLOR, new DyedItemColor(phantomColor, false));
                legs.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                clone.setItemSlot(EquipmentSlot.LEGS, legs);

                ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
                boots.set(DataComponents.DYED_COLOR, new DyedItemColor(phantomColor, false));
                boots.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                clone.setItemSlot(EquipmentSlot.FEET, boots);

                clone.setItemSlot(EquipmentSlot.MAINHAND, heldWeapon);

                // Dáng chém bạt đao
                clone.setRightArmPose(new Rotations(-85.0F, -40.0F, 0.0F));
                clone.setLeftArmPose(new Rotations(-20.0F, 20.0F, 0.0F));
                clone.setBodyPose(new Rotations(15.0F, 0.0F, 0.0F));

                level.addFreshEntity(clone);
                session.finisherClones.add(clone);
            }
        }

        // Tiêu đề
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 45, 15));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§lTUYỆT KĨ CHÉM XUYÊN THỜI GIAN")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a[ 0.00s] §fThu Kiếm Vào Vỏ!")));
    }

    /**
     * TICK CẬP NHẬT PHIÊN KỸ NĂNG VÀ ÁN TỬ (TỐI ƯU HÓA CAO ĐỘ)
     */
    public static void tickAbilities(ServerLevel level) {
        if (ACTIVE_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveIaiSession>> it = ACTIVE_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            ActiveIaiSession session = it.next().getValue();
            if (session.level != level) continue;

            // Kiểm tra an toàn: nếu người chơi đã ngắt kết nối hoặc chết, dọn dẹp ngay
            if (session.caster.hasDisconnected() || !session.caster.isAlive()) {
                session.cleanup();
                it.remove();
                continue;
            }

            if (!session.inFinisher) {
                session.timeoutTicks--;

                // TỐI ƯU CỰC HẠN: Chỉ quét bổ sung thực thể mới mỗi 10 ticks (0.5s), không quét mỗi tick
                if (session.timeoutTicks % 10 == 0) {
                    AABB freezeBox = session.caster.getBoundingBox().inflate(48.0D);
                    List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                            e -> e != session.caster && e != session.physicalBodyStand && e.isAlive() && !e.isSpectator());
                    for (LivingEntity ent : nearby) {
                        if (SeveredHeartCurseManager.isUltimateBossImmune(ent)) continue;
                        session.frozenEntities.add(ent.getUUID());
                        ent.setDeltaMovement(Vec3.ZERO);
                        if (!ent.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                            ent.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 255, false, false, false));
                        }
                    }
                }

                // Hết thời gian 30s mà chưa bấm click 2 -> Tự động kết thúc an toàn
                if (session.timeoutTicks <= 0) {
                    triggerFinisher(session);
                }
            } else {
                session.finisherTick++;

                // Nhịp chém visual tại tick 4
                if (session.finisherTick == 4 && !session.markedSlashes.isEmpty()) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(0), 1);
                }

                // Tick 12: Đổi dáng sang TRA KIẾM VÀO VỎ & phát tiếng Keng!
                if (session.finisherTick == 12) {
                    for (ArmorStand clone : session.finisherClones) {
                        if (clone.isAlive()) {
                            clone.setRightArmPose(new Rotations(-15.0F, 10.0F, 0.0F));
                            clone.setLeftArmPose(new Rotations(-10.0F, -10.0F, 0.0F));
                            clone.setBodyPose(new Rotations(0.0F, 0.0F, 0.0F));
                        }
                    }
                    level.playSound(null, session.caster.getX(), session.caster.getY(), session.caster.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0F, 1.9F);
                    level.playSound(null, session.caster.getX(), session.caster.getY(), session.caster.getZ(),
                            SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 1.5F);

                    session.caster.displayClientMessage(Component.literal("§a[ 0.00s] §fTra Kiếm Vào Vỏ..."), true);
                }

                // Tick 18: KHÔNG GÂY SÁT THƯƠNG NGAY MÀ ÁP DỤNG ÁN TỬ 5 GIÂY (100 TICKS)
                if (session.finisherTick == 18) {
                    ServerPlayer caster = session.caster;

                    for (MarkedSlash mark : session.markedSlashes) {
                        Vec3 pos = mark.markPos;
                        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.0D, pos.z, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1.0D, pos.z, 2, 0.3D, 0.3D, 0.3D, 0);

                        // ÁP DỤNG ÁN TỬ VÀO SEVERED HEART CURSE MANAGER (CHƯA NHẬN SÁT THƯƠNG NGAY)
                        if (mark.hitEntity != null && mark.hitEntity.isAlive()) {
                            SeveredHeartCurseManager.applyCurse(level, mark.hitEntity, caster);
                        }
                    }

                    caster.displayClientMessage(
                            Component.literal("§6§l[TRẢM KÍCH HOÀN TẤT] §fMục tiêu sẽ §4tử vong sau 5 giây §fnếu không có Thánh Quang / Vật Tổ điều trị!"),
                            true
                    );
                }

                // Tick 26: DỌN DẸP TOÀN BỘ PHIÊN VÀ TÁI NHẬP THÂN XÁC
                if (session.finisherTick >= 26) {
                    session.cleanup();
                    ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_END, 0), session.caster);
                    it.remove();
                }
            }
        }
    }

    private static void triggerSlashHitVisual(ServerLevel level, MarkedSlash mark, int index) {
        Vec3 pos = mark.markPos;
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.9D, pos.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.0D, pos.z, 4, 0.2D, 0.2D, 0.2D, 0.03D);
        level.sendParticles(SamuraiSpiritDisplayHelper.PURE_WHITE_DUST, pos.x, pos.y + 0.9D, pos.z, 4, 0.2D, 0.3D, 0.2D, 0.02D);

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.2F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 2.0F);
    }

    public static void cleanup(ServerLevel level) {
        for (ActiveIaiSession session : ACTIVE_SESSIONS.values()) {
            if (session.level == level) {
                session.cleanup();
            }
        }
        ACTIVE_SESSIONS.clear();
    }
}
