package com.minhphuc.weapons.content.darkgathering;

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
 * TUYỆT KĨ: CHÉM XUYÊN KHÔNG THỜI GIAN (SPACE-TIME PIERCING SLASH) - THIÊN MA KIẾM
 * CHUỖI 3 NHỊP CHUẨN XÁC:
 * 1. Khởi đầu: Thế giới đơn sắc Trắng Đen Monochrome, linh hồn xuất khiếu di chuyển tự do.
 * 2. Nhịp 1 [-0.02s]:
 *    - ĐOẠT NÃO VÀ TIM: Sinh vật lập tức rơi ra cả Trái Tim Bị Cắt và Bộ Não Bị Cắt mang tên nó.
 *    - Hiển thị LED 3D [-0.02].
 *    - NGOẠI LỆ DUY NHẤT: Chỉ riêng Bạch Băng Long (Velzard) lập tức cử động được và bẻ gãy không gian,
 *      thế giới trở lại bình thường và hủy chiêu, không rơi não/tim, không dính án tử!
 * 3. Nhịp 2 [-0.01s]:
 *    - Hiển thị LED 3D [-0.01]. Rạn nứt không gian số hóa.
 * 4. Nhịp 3 [ 0.00s] (Đòn Quyết Định):
 *    - Hiển thị LED 3D [ 0.00]. Màu sắc thế giới trở lại bình thường ngay lập tức.
 *    - Tra kiếm vào vỏ ("Keng!"). Sinh vật chưa nhận sát thương ngay.
 *    - KÍCH HOẠT ÁN TỬ 3 GIÂY (60 ticks):
 *      Nếu không đứng trong vùng "Bí Mật Của Đức Tin Và Ân Sủng", sau 3 giây 100% SẼ CHẾT!
 */
public class ThienMaSwordAbility {

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

    public static class ActiveThienMaSession {
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
        public int timeoutTicks = 600;

        public ActiveThienMaSession(ServerLevel level, ServerPlayer caster, Vec3 physicalBodyPos, float physicalBodyYaw, ArmorStand physicalBodyStand) {
            this.level = level;
            this.caster = caster;
            this.physicalBodyPos = physicalBodyPos;
            this.physicalBodyYaw = physicalBodyYaw;
            this.physicalBodyStand = physicalBodyStand;
        }

        public void cleanup() {
            if (physicalBodyStand != null && physicalBodyStand.isAlive()) {
                physicalBodyStand.discard();
            }

            for (ArmorStand clone : finisherClones) {
                if (clone != null && clone.isAlive()) {
                    clone.discard();
                }
            }
            finisherClones.clear();

            for (Display.TextDisplay led : activeLedDisplays) {
                if (led != null && led.isAlive()) {
                    DigitalLedDisplayHelper.remove(led);
                    led.discard();
                }
            }
            activeLedDisplays.clear();

            // Gỡ bỏ Slowness tức thì cho toàn bộ quái đã bị khóa
            for (UUID uid : frozenEntities) {
                Entity ent = level.getEntity(uid);
                if (ent instanceof LivingEntity living && living.isAlive()) {
                    living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
            }
            frozenEntities.clear();
        }
    }

    public static final Map<UUID, ActiveThienMaSession> ACTIVE_SESSIONS = new HashMap<>();

    public static boolean isPlayerInSession(UUID playerUuid) {
        return !ACTIVE_SESSIONS.isEmpty() && ACTIVE_SESSIONS.containsKey(playerUuid);
    }

    public static boolean isEntityFrozenInSession(LivingEntity entity) {
        if (ACTIVE_SESSIONS.isEmpty()) return false;
        if (entity == null || !entity.isAlive()) return false;
        if (ThienMaCurseManager.isVelzardImmune(entity)) return false;

        UUID entityUuid = entity.getUUID();
        for (ActiveThienMaSession session : ACTIVE_SESSIONS.values()) {
            if (!session.inFinisher) {
                if (session.frozenEntities.contains(entityUuid) || session.caster.distanceToSqr(entity) <= 48.0D * 48.0D) {
                    return true;
                }
            }
        }
        return false;
    }

    public static EventResult onPlayerAttackEntity(Player player, Level level, Entity target, InteractionHand hand, HitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && isPlayerInSession(player.getUUID())) {
            Vec3 pos = (target != null) ? target.position().add(0, 0.5D, 0) : player.position().add(player.getLookAngle().scale(3.0D));
            int targetId = (target != null) ? target.getId() : -1;
            handleClick(serverPlayer, pos, targetId);
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    public static void onPlayerQuit(ServerPlayer player) {
        if (player == null || ACTIVE_SESSIONS.isEmpty()) return;
        ActiveThienMaSession session = ACTIVE_SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.cleanup();
        }
    }

    /**
     * KÍCH HOẠT TUYỆT KỸ: CHÉM XUYÊN KHÔNG THỜI GIAN
     */
    public static boolean castSpaceTimeSlash(ServerLevel level, ServerPlayer player) {
        if (ACTIVE_SESSIONS.containsKey(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("§e⚠️ Đang trong trạng thái Chém Xuyên Không Thời Gian! Hãy hoàn tất chuỗi trảm kích!"),
                    true
            );
            return false;
        }

        Vec3 bodyPos = player.position();
        float bodyYaw = player.getYRot();

        // 1. TẠO THÂN XÁC VẬT LÝ ĐỨNG YÊN
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

            ItemStack playerSkull = new ItemStack(Items.PLAYER_HEAD);
            playerSkull.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            bodyStand.setItemSlot(EquipmentSlot.HEAD, playerSkull);

            bodyStand.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
            bodyStand.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
            bodyStand.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());

            bodyStand.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            bodyStand.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem().copy());

            bodyStand.setRightArmPose(new Rotations(-15.0F, 10.0F, 0.0F));
            bodyStand.setLeftArmPose(new Rotations(-10.0F, -10.0F, 0.0F));
            bodyStand.setBodyPose(new Rotations(5.0F, 0.0F, 0.0F));

            level.addFreshEntity(bodyStand);
        }

        ActiveThienMaSession session = new ActiveThienMaSession(level, player, bodyPos, bodyYaw, bodyStand);

        // 2. KHÓA ĐÓNG BĂNG 48 BLOCKS (TRỪ BẠCH BĂNG LONG)
        AABB freezeBox = player.getBoundingBox().inflate(48.0D);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                e -> e != player && e.isAlive() && !e.isSpectator());

        for (LivingEntity target : nearbyEntities) {
            if (ThienMaCurseManager.isVelzardImmune(target)) continue;
            session.frozenEntities.add(target.getUUID());
            target.setDeltaMovement(Vec3.ZERO);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 255, false, false, false));
        }

        // 3. ĐỒNG BỘ SHADER VÀ HIỂN THỊ TIÊU ĐỀ
        ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_START, 600), player);

        level.playSound(null, bodyPos.x, bodyPos.y, bodyPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);
        level.playSound(null, bodyPos.x, bodyPos.y, bodyPos.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.5F);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§lTUYỆT KĨ CHÉM XUYÊN KHÔNG THỜI GIAN")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.02s] §fNhấp chuột lần 1 để chém đoạt Não và Tim...")));
        player.displayClientMessage(Component.literal("§e👉 [1/3] Nhấp chuột [-0.02s] vào sinh vật để đoạt lấy Bộ Não và Trái Tim!"), true);

        ACTIVE_SESSIONS.put(player.getUUID(), session);
        return true;
    }

    /**
     * XỬ LÝ NHẤP CHUỘT: CHUỖI 3 BƯỚC CHUẨN XÁC:
     * - Bước 1: [-0.02s] (Đoạt Não & Tim, hiển thị LED -0.02)
     * - Bước 2: [-0.01s] (Vết chém không gian, hiển thị LED -0.01)
     * - Bước 3: [ 0.00s] (Kết liễu, thu kiếm Keng!, màu sắc trở lại, kích hoạt án tử 3 giây)
     */
    public static void handleClick(ServerPlayer player, Vec3 clickPos, int targetEntityId) {
        ActiveThienMaSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || session.inFinisher) return;

        long now = System.currentTimeMillis();
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

        Vec3 targetPos = (hitEntity != null) ? hitEntity.position().add(0, 0.6D, 0) : clickPos;
        float slashYaw = player.getYRot();
        double ledYOffset = (hitEntity != null) ? (hitEntity.getBbHeight() + 0.35D) : 1.3D;
        Vec3 ledPos = (hitEntity != null) ? hitEntity.position().add(0, ledYOffset, 0) : targetPos.add(0, 1.2D, 0);

        switch (session.clickCount) {
            case 1 -> {
                // =========================================================================
                // BƯỚC 1: [-0.02s] - ĐOẠT NÃO & TIM
                // =========================================================================
                // NGOẠI LỆ DUY NHẤT: BẠCH BĂNG LONG (VELZARD) LẬP TỨC PHÁ VỠ KỸ NĂNG!
                if (hitEntity != null && ThienMaCurseManager.isVelzardImmune(hitEntity)) {
                    hitEntity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    hitEntity.setDeltaMovement(new Vec3(0, 0.25D, 0));

                    level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0F, 0.8F);
                    level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.2F);
                    level.sendParticles(ParticleTypes.FLASH, hitEntity.getX(), hitEntity.getY() + 1.0D, hitEntity.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.0D);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitEntity.getX(), hitEntity.getY() + 1.0D, hitEntity.getZ(), 1, 0, 0, 0, 0);

                    ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_END, 0), player);
                    session.cleanup();
                    ACTIVE_SESSIONS.remove(player.getUUID());

                    player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 50, 15));
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§lKHÔNG GIAN BỊ PHÁ VỠ!")));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c" + hitEntity.getName().getString() + " §cđã miễn nhiễm và bẻ gãy Thiên Ma Kiếm!")));
                    player.displayClientMessage(
                            Component.literal("§c⚠️ [CẢNH BÁO TỐI CAO] §eBạch Băng Long Velzard §cđã lập tức cử động được và phá tan không gian ngưng đọng thời gian!"),
                            false
                    );
                    return;
                }

                session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, "-0.02s", slashYaw));

                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, "-0.02", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.0F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.9F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
                SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

                // RƠI ĐỒNG THỜI CẢ 2 VẬT PHẨM: TRÁI TIM VÀ BỘ NÃO
                if (hitEntity != null) {
                    ItemStack heartStack = SeveredHeartItem.createForEntity(hitEntity);
                    ItemStack brainStack = SeveredBrainItem.createForEntity(hitEntity);

                    ItemEntity heartDrop = new ItemEntity(level, hitEntity.getX() - 0.2D, hitEntity.getY() + 0.6D, hitEntity.getZ(), heartStack);
                    ItemEntity brainDrop = new ItemEntity(level, hitEntity.getX() + 0.2D, hitEntity.getY() + 0.6D, hitEntity.getZ(), brainStack);
                    heartDrop.setDefaultPickUpDelay();
                    brainDrop.setDefaultPickUpDelay();

                    level.addFreshEntity(heartDrop);
                    level.addFreshEntity(brainDrop);

                    level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.8F, 1.2F);
                    player.displayClientMessage(
                            Component.literal("§c§l[-0.02s] §fĐã đoạt §e" + heartStack.getHoverName().getString() + " §fvà §d" + brainStack.getHoverName().getString() + "§f! §a👉 Nhấp tiếp [-0.01s]!"),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.literal("§c§l[-0.02s] §fĐã chém xé không gian! §a👉 Nhấp tiếp [-0.01s]!"),
                            true
                    );
                }

                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.02s] §fNhấp tiếp [-0.01s]...")));
            }
            case 2 -> {
                // =========================================================================
                // BƯỚC 2: [-0.01s] - VẾT CHÉM KHÔNG GIAN
                // =========================================================================
                session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, "-0.01s", slashYaw));

                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, "-0.01", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.2F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.8F, 2.0F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
                SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

                player.displayClientMessage(
                        Component.literal("§e§l[-0.01s] §fKhắc dấu vết chém không thời gian! §a👉 Nhấp lần nữa để KẾT LIỄU [ 0.00s]!"),
                        true
                );
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e[-0.01s] §fNhấp lần nữa để KẾT LIỄU [ 0.00s]")));
            }
            default -> {
                // =========================================================================
                // BƯỚC 3: [ 0.00s] - KÍCH HOẠT FINISHER & ÁP DỤNG ÁN TỬ 3 GIÂY
                // =========================================================================
                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, " 0.00", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                triggerFinisher(session);
            }
        }
    }

    private static void triggerFinisher(ActiveThienMaSession session) {
        session.inFinisher = true;
        session.finisherTick = 0;
        ServerLevel level = session.level;
        ServerPlayer player = session.caster;

        // 1. TẮT SHADER MÀU TRẮNG ĐEN -> MÀU SẮC THẾ GIỚI PHỤC HỒI TỨC THÌ
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

                ItemStack skull = new ItemStack(Items.WITHER_SKELETON_SKULL);
                skull.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                clone.setItemSlot(EquipmentSlot.HEAD, skull);

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

                clone.setRightArmPose(new Rotations(-85.0F, -40.0F, 0.0F));
                clone.setLeftArmPose(new Rotations(-20.0F, 20.0F, 0.0F));
                clone.setBodyPose(new Rotations(15.0F, 0.0F, 0.0F));

                level.addFreshEntity(clone);
                session.finisherClones.add(clone);
            }
        }

        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 45, 15));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§lTUYỆT KĨ CHÉM XUYÊN KHÔNG THỜI GIAN")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a[ 0.00s] §fThu Kiếm Vào Vỏ!")));
    }

    public static void tickAbilities(ServerLevel level) {
        if (ACTIVE_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveThienMaSession>> it = ACTIVE_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            ActiveThienMaSession session = it.next().getValue();
            if (session.level != level) continue;

            if (session.caster.hasDisconnected() || !session.caster.isAlive()) {
                session.cleanup();
                it.remove();
                continue;
            }

            if (!session.inFinisher) {
                session.timeoutTicks--;

                // Quét làm mới khóa thực thể mỗi 10 ticks (0.5s)
                if (session.timeoutTicks % 10 == 0) {
                    AABB freezeBox = session.caster.getBoundingBox().inflate(48.0D);
                    List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                            e -> e != session.caster && e != session.physicalBodyStand && e.isAlive() && !e.isSpectator());
                    for (LivingEntity ent : nearby) {
                        if (ThienMaCurseManager.isVelzardImmune(ent)) continue;
                        session.frozenEntities.add(ent.getUUID());
                        ent.setDeltaMovement(Vec3.ZERO);
                        if (!ent.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                            ent.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 255, false, false, false));
                        }
                    }
                }

                if (session.timeoutTicks <= 0) {
                    triggerFinisher(session);
                }
            } else {
                session.finisherTick++;

                // Nhịp chém visual tại tick 4 và 8
                if (session.finisherTick == 4 && session.markedSlashes.size() >= 1) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(0), 1);
                } else if (session.finisherTick == 8 && session.markedSlashes.size() >= 2) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(1), 2);
                }

                // Tick 12: TRA KIẾM VÀO VỎ & phát tiếng Keng!
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

                // Tick 18: KÍCH HOẠT ÁN TỬ 3 GIÂY (CHƯA GÂY SÁT THƯƠNG NGAY)
                if (session.finisherTick == 18) {
                    ServerPlayer caster = session.caster;

                    for (MarkedSlash mark : session.markedSlashes) {
                        Vec3 pos = mark.markPos;
                        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.0D, pos.z, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1.0D, pos.z, 2, 0.3D, 0.3D, 0.3D, 0);

                        if (mark.hitEntity != null && mark.hitEntity.isAlive()) {
                            ThienMaCurseManager.applyCurse(level, mark.hitEntity, caster);
                        }
                    }

                    caster.displayClientMessage(
                            Component.literal("§6§l[TRẢM KÍCH THIÊN MA] §fMục tiêu sẽ §4tử vong sau 3 giây §fnếu không có Bí Mật Của Đức Tin Và Ân Sủng điều trị!"),
                            true
                    );
                }

                // Tick 26: Dọn dẹp session và tái nhập thân xác
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

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.2F + (index * 0.2F));
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 2.0F);
    }

    public static void cleanup(ServerLevel level) {
        for (ActiveThienMaSession session : ACTIVE_SESSIONS.values()) {
            if (session.level == level) {
                session.cleanup();
            }
        }
        ACTIVE_SESSIONS.clear();
    }
}
