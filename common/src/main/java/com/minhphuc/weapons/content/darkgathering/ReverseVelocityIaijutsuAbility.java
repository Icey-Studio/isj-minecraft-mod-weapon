package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
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
 * TUYỆT KỸ: BẠT ĐAO THUẬT VẬN TỐC NGƯỢC (REVERSE-VELOCITY IAIJUTSU)
 * CƠ CHẾ NGUYÊN TÁC DARK GATHERING (CHAPTER 83):
 * 1. Ngưng đọng thời không + Thế giới đổi sang màu Trắng Đen Manga.
 * 2. Thân xác vật lý: Đứng yên tại điểm xuất phát (100% đúng Skin, Giáp, và Vật phẩm người chơi đang cầm).
 * 3. Linh hồn xuất khiếu: Di chuyển tự do (Góc nhìn F5 thấy dạng bán trong suốt).
 * 4. Chuỗi 5 lần nhấn chuột:
 *    - Lần 1: [-0.04s] (Đánh dấu vết chém 1, hiện khung LED, chặn sát thương gốc)
 *    - Lần 2: [-0.02s] (Đánh dấu vết chém 2, hiện khung LED, chặn sát thương gốc)
 *    - Lần 3: [-0.01s] (Đánh dấu vết chém 3, hiện khung LED, chặn sát thương gốc)
 *    - Lần 4: [ 0.00s] (Đánh dấu vết chém 4, hiện khung LED, chặn sát thương gốc)
 *    - Lần 5: KÍCH HOÀT ĐÒN QUYẾT ĐỊNH (FINISHER):
 *      + Màu sắc trở lại tự nhiên.
 *      + 4 Tàn ảnh đồng loạt xuất hiện tại 4 vị trí đánh dấu.
 *      + Trải nghiệm điện ảnh ~2.0s: 4 nhịp keng liên tiếp ➔ Tra kiếm vào vỏ ➔ Nổ sát thương bạo kích 75/đòn!
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

        public int clickCount = 0;
        public long lastClickTimeMs = 0;
        public boolean inFinisher = false;
        public int finisherTick = 0;
        public int timeoutTicks = 600; // Tối đa 30s để người chơi thi triển 5 click

        public ActiveIaiSession(ServerLevel level, ServerPlayer caster, Vec3 physicalBodyPos, float physicalBodyYaw, ArmorStand physicalBodyStand) {
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
                    led.discard();
                }
            }
            activeLedDisplays.clear();
        }
    }

    public static final Map<UUID, ActiveIaiSession> ACTIVE_SESSIONS = new HashMap<>();

    public static boolean canUseTengaiTechniques(Player player) {
        if (player == null) return false;
        boolean isWearingDivine = DivineArmorItem.isWearingAnyPiece(player);
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        return isWearingDivine || isTrueDemonLord;
    }

    public static boolean isPlayerInIaiSession(UUID playerUuid) {
        return ACTIVE_SESSIONS.containsKey(playerUuid);
    }

    public static boolean isEntityFrozenInSession(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        for (ActiveIaiSession session : ACTIVE_SESSIONS.values()) {
            if (!session.inFinisher) {
                if (session.caster.distanceToSqr(entity) <= 48.0D * 48.0D) {
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
     * KÍCH HOẠT TUYỆT KỸ: BẠT ĐAO THUẬT VẬN TỐC NGƯỢC (TỪ MENU PHÍM Z)
     */
    public static boolean castReverseVelocityIai(ServerLevel level, ServerPlayer player) {
        if (!canUseTengaiTechniques(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Yêu cầu trang bị Giáp Thần Linh hoặc thức tỉnh Chân Ma Vương để khai mở Bạt Đao Thuật Vận Tốc Ngược!"),
                    true
            );
            return false;
        }

        if (ACTIVE_SESSIONS.containsKey(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("§e⚠️ Đang trong trạng thái Bạt Đao Thuật Vận Tốc Ngược! Hãy nhấp chuột để tiếp tục khắc dấu!"),
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

            // Cầm đúng item người chơi đang cầm trên 2 tay (không nhất thiết là kiếm)
            bodyStand.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            bodyStand.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem().copy());

            // Dáng đứng chuẩn bị bạt đao
            bodyStand.setRightArmPose(new Rotations(-15.0F, 10.0F, 0.0F));
            bodyStand.setLeftArmPose(new Rotations(-10.0F, -10.0F, 0.0F));
            bodyStand.setBodyPose(new Rotations(5.0F, 0.0F, 0.0F));

            level.addFreshEntity(bodyStand);
        }

        // 2. KHÓA ĐÓNG BĂNG MỌI SINH VẬT TRONG PHẠM VI 48 BLOCKS
        AABB freezeBox = player.getBoundingBox().inflate(48.0D);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                e -> e != player && e.isAlive() && !e.isSpectator());

        for (LivingEntity target : nearbyEntities) {
            target.setDeltaMovement(Vec3.ZERO);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 255, false, false, false));
        }

        // 3. GỬI GÓI TIN ĐỒNG BỘ: MÀU SẮC TRẮNG ĐEN & BẮT ĐẦU XUẤT KHIẾU LINH HỒN
        ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_START, 600), player);

        // Phát âm thanh ngưng đọng thời không
        level.playSound(null, bodyPos.x, bodyPos.y, bodyPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);
        level.playSound(null, bodyPos.x, bodyPos.y, bodyPos.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.5F);

        // Hiển thị tiêu đề
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§l天外救世権現・抜刀術『逆速度』")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.04s] §fNhấp chuột để khắc 4 dấu ấn thời gian...")));
        player.displayClientMessage(Component.literal("§e👉 Hãy nhấp chuột vào không khí hoặc sinh vật để khắc 4 dấu ấn thời gian!"), true);

        ActiveIaiSession session = new ActiveIaiSession(level, player, bodyPos, bodyYaw, bodyStand);
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        return true;
    }

    /**
     * XỬ LÝ CLICK TỪ NGƯỜI CHƠI (TỪ 1 ĐẾN 5) - CÓ DEBOUNCE CHỐNG SPAM
     */
    public static void handleClick(ServerPlayer player, Vec3 clickPos, int targetEntityId) {
        ActiveIaiSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || session.inFinisher) return;

        long now = System.currentTimeMillis();
        // Debounce tối thiểu 350ms giữa 2 lần nhấp chuột để tránh spam / click đúp ngoài ý muốn
        if (now - session.lastClickTimeMs < 350) {
            return;
        }
        session.lastClickTimeMs = now;

        ServerLevel level = session.level;

        // Tìm thực thể bị đánh (nếu có)
        LivingEntity hitEntity = null;
        if (targetEntityId != -1) {
            var ent = level.getEntity(targetEntityId);
            if (ent instanceof LivingEntity living && living.isAlive()) {
                hitEntity = living;
            }
        }

        // Quét tìm entity gần điểm click trong 3.5 blocks nếu không chỉ thẳng
        if (hitEntity == null) {
            AABB search = new AABB(clickPos.x - 3.0, clickPos.y - 3.0, clickPos.z - 3.0,
                    clickPos.x + 3.0, clickPos.y + 3.0, clickPos.z + 3.0);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, search,
                    e -> e != player && e != session.physicalBodyStand && e.isAlive() && !e.isSpectator());
            if (!near.isEmpty()) {
                hitEntity = near.get(0);
            }
        }

        Vec3 targetPos = (hitEntity != null) ? hitEntity.position().add(0, 0.6D, 0) : clickPos;
        float slashYaw = player.getYRot();
        double ledYOffset = (hitEntity != null) ? (hitEntity.getBbHeight() + 0.35D) : 1.3D;
        Vec3 ledPos = (hitEntity != null) ? hitEntity.position().add(0, ledYOffset, 0) : targetPos.add(0, 1.2D, 0);

        session.clickCount++;

        switch (session.clickCount) {
            case 1 -> {
                // Lần 1: [-0.04s]
                session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, "-0.04s", slashYaw));
                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, "-0.04", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 0.8F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.8F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
                SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

                player.displayClientMessage(Component.literal("§c§l[-0.04s] §fĐã khắc dấu vết chém [1/4]! §7(Chưa gây sát thương)"), true);
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c[-0.04s] §fVết chém [1/4]")));
            }
            case 2 -> {
                // Lần 2: [-0.02s]
                session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, "-0.02s", slashYaw));
                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, "-0.02", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.0F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.9F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
                SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

                player.displayClientMessage(Component.literal("§e§l[-0.02s] §fĐã khắc dấu vết chém [2/4]! §7(Chưa gây sát thương)"), true);
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e[-0.02s] §fVết chém [2/4]")));
            }
            case 3 -> {
                // Lần 3: [-0.01s]
                session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, "-0.01s", slashYaw));
                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, "-0.01", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.2F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 2.0F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
                SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

                player.displayClientMessage(Component.literal("§e§l[-0.01s] §fĐã khắc dấu vết chém [3/4]! §7(Chưa gây sát thương)"), true);
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e[-0.01s] §fVết chém [3/4]")));
            }
            case 4 -> {
                // Lần 4: [ 0.00s]
                session.markedSlashes.add(new MarkedSlash(targetPos, hitEntity, " 0.00s", slashYaw));
                var led = DigitalLedDisplayHelper.spawnLedText(level, ledPos, " 0.00", 2.6F, 600);
                if (led != null) session.activeLedDisplays.add(led);

                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 1.4F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.8F, 2.0F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 2, 0.2D, 0.2D, 0.2D, 0);
                SamuraiSpiritDisplayHelper.spawnDigitalBlockCracks(level, player.position(), targetPos);

                player.displayClientMessage(Component.literal("§a§l[ 0.00s] §fĐã khắc xong [4/4] vết chém! §d👉 Nhấp lần nữa để KẾT LIỄU!"), true);
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a[ 0.00s] §fNhấp chuột lần nữa để KẾT LIỄU!")));
            }
            default -> {
                // Lần 5: KÍCH HOÀT ĐÒN QUYẾT ĐỊNH (FINISHER)
                triggerFinisher(session);
            }
        }
    }

    /**
     * KHỞI ĐỘNG PHA KẾT LIỄU ĐIỆN ẢNH (CINEMATIC FINISHER SEQUENCE)
     */
    private static void triggerFinisher(ActiveIaiSession session) {
        session.inFinisher = true;
        session.finisherTick = 0;
        ServerLevel level = session.level;
        ServerPlayer player = session.caster;

        // 1. TẮT SHADER MÀU TRẮNG ĐEN -> MÀU SẮC THẾ GIỚI TRỞ LẠI BÌNH THƯỜNG
        ModMessages.sendToPlayer(new ClientboundIaiStatePacket(ClientboundIaiStatePacket.STATE_FINISHER_STARTED, 0), player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.2F);

        // 2. SINH ĐỒNG LOẠT 4 TÀN ẢNH PHÂN THÂN TẠI 4 ĐIỂM ĐÁNH DẤU
        int phantomColor = 0x12141D;
        ItemStack heldWeapon = player.getMainHandItem().copy();
        heldWeapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        for (int i = 0; i < session.markedSlashes.size(); i++) {
            MarkedSlash mark = session.markedSlashes.get(i);
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
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l天外救世権現・抜刀術『逆速度』")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c§l[QUYẾT ĐỊNH TRẢM] §fHội Tụ Tàn Ảnh!")));
    }

    /**
     * TICK CẬP NHẬT CÁC PHIÊN BẠT ĐAO THUẬT ĐANG HOẠT ĐỘNG
     */
    public static void tickAbilities(ServerLevel level) {
        if (ACTIVE_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveIaiSession>> it = ACTIVE_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            ActiveIaiSession session = it.next().getValue();
            if (session.level != level) continue;

            if (!session.inFinisher) {
                session.timeoutTicks--;

                // KHÓA CỨNG TOÀN BỘ QUÁI VẬT TRONG PHẠM VI 48 BLOCKS SUỐT THỜI GIAN ĐẾM NGƯỢC
                AABB freezeBox = session.caster.getBoundingBox().inflate(48.0D);
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                        e -> e != session.caster && e != session.physicalBodyStand && e.isAlive() && !e.isSpectator());
                for (LivingEntity ent : nearby) {
                    ent.setDeltaMovement(Vec3.ZERO);
                }

                // Hết thời gian mà chưa bấm xong 5 click -> Tự động kích hoạt finisher
                if (session.timeoutTicks <= 0) {
                    triggerFinisher(session);
                }
            } else {
                // TRẢI NGHIỆM ĐIỆN ẢNH (CINEMATIC PACING ~2.0 GIÂY = 40 TICKS)
                session.finisherTick++;

                // Nhịp chém staccato: Ticks 4, 8, 12, 16
                if (session.finisherTick == 4 && session.markedSlashes.size() >= 1) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(0), 1);
                } else if (session.finisherTick == 8 && session.markedSlashes.size() >= 2) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(1), 2);
                } else if (session.finisherTick == 12 && session.markedSlashes.size() >= 3) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(2), 3);
                } else if (session.finisherTick == 16 && session.markedSlashes.size() >= 4) {
                    triggerSlashHitVisual(level, session.markedSlashes.get(3), 4);
                }

                // Tick 24: Cả 4 phân thân đồng loạt đổi sang tư thế TRA KIẾM VÀO VỎ
                if (session.finisherTick == 24) {
                    for (ArmorStand clone : session.finisherClones) {
                        if (clone.isAlive()) {
                            clone.setRightArmPose(new Rotations(-15.0F, 10.0F, 0.0F));
                            clone.setLeftArmPose(new Rotations(-10.0F, -10.0F, 0.0F));
                            clone.setBodyPose(new Rotations(0.0F, 0.0F, 0.0F));
                        }
                    }
                    // Tiếng tra kiếm đanh thép chấn động
                    level.playSound(null, session.caster.getX(), session.caster.getY(), session.caster.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0F, 1.9F);
                    level.playSound(null, session.caster.getX(), session.caster.getY(), session.caster.getZ(),
                            SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 1.5F);

                    session.caster.displayClientMessage(Component.literal("§a[ 0.00s] §fThu Kiếm Vào Vỏ..."), true);
                }

                // Tick 30: ĐẠI BÙNG NỔ SÁT THƯƠNG 75 / ĐÒN (BÂY GIỜ MỚI GÂY SÁT THƯƠNG!)
                if (session.finisherTick == 30) {
                    ServerPlayer caster = session.caster;

                    for (MarkedSlash mark : session.markedSlashes) {
                        Vec3 pos = mark.markPos;

                        // Hiệu ứng nổ Flash và chém xé không gian
                        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.0D, pos.z, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1.0D, pos.z, 3, 0.4D, 0.4D, 0.4D, 0);
                        level.sendParticles(SamuraiSpiritDisplayHelper.PURE_WHITE_DUST, pos.x, pos.y + 1.0D, pos.z, 12, 0.5D, 0.5D, 0.5D, 0.08D);
                        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0D, pos.z, 12, 0.4D, 0.4D, 0.4D, 0.15D);

                        // Sát thương 75.0F cực đại vào mục tiêu chính
                        if (mark.hitEntity != null && mark.hitEntity.isAlive()) {
                            mark.hitEntity.invulnerableTime = 0;
                            mark.hitEntity.hurt(level.damageSources().playerAttack(caster), 75.0F);

                            // Hất văng
                            Vec3 knockback = pos.subtract(session.physicalBodyPos).normalize().scale(0.8D).add(0, 0.35D, 0);
                            mark.hitEntity.setDeltaMovement(knockback);
                        }

                        // Sát thương lan 35.0F xung quanh mỗi vết chém (bán kính 4 blocks)
                        AABB splashBox = new AABB(pos.x - 4, pos.y - 2, pos.z - 4, pos.x + 4, pos.y + 3, pos.z + 4);
                        List<LivingEntity> splashMobs = level.getEntitiesOfClass(LivingEntity.class, splashBox,
                                e -> e != caster && e != mark.hitEntity && e.isAlive() && !e.isSpectator());

                        for (LivingEntity mob : splashMobs) {
                            mob.hurt(level.damageSources().playerAttack(caster), 35.0F);
                        }
                    }

                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8F, 1.2F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0F, 0.7F);

                    caster.displayClientMessage(Component.literal("§6§l[BÙNG NỔ] §fTrảm Kích Hoàn Tất!"), true);
                }

                // Tick 42: DỌN DẸP TOÀN BỘ & KẾT THÚC
                if (session.finisherTick >= 42) {
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

        float pitch = 1.0F + (index * 0.25F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, pitch);
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
