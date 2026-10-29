package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.InteractionAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Tuyệt Kỹ Thần Cấp: Thị Nhục (Seer Flesh / 視肉) - Thái Tuế Tinh Quân (Dark Gathering Tập 18)
 *
 * 1. Triệu hồi khối thịt nhớp nháp khổng lồ màu đỏ thẫm với hàng chục con mắt trắng lồi.
 * 2. Đứng yên kế bên người triệu hồi. Nếu người triệu hồi di chuyển cách xa QUÁ 5 BLOCKS -> Tự động tan biến.
 * 3. Hoạt ảnh lúc nhúc: Co bóp, rung giật, thở sinh học bất đối xứng liên tục.
 * 4. Tương tác Chuột Phải: 1 con mắt đổi màu/nhắm lại -> Hồi phục 100% Máu & Xóa sạch mọi độc tố/hiệu ứng xấu.
 * 5. Tương tác Tấn Công: Rạch thịt thu được 1 vật phẩm [Con Mắt Thị Nhục] dùng để chữa lành mọi sinh vật.
 */
public class SeerFleshAbility {

    public static class ActiveSeerFlesh {
        public final UUID ownerUuid;
        public final ServerLevel level;
        public Display.ItemDisplay display;
        public Interaction interaction;
        public Vec3 spawnPos;
        public int eyeCount = 12; // 12 con mắt sẵn sàng khai thác / trị liệu
        public long squirmTick = 0;
        public long lastHarvestGameTime = 0;
        public long lastHealGameTime = 0;

        public ActiveSeerFlesh(UUID ownerUuid, ServerLevel level, Display.ItemDisplay display, Interaction interaction, Vec3 spawnPos) {
            this.ownerUuid = ownerUuid;
            this.level = level;
            this.display = display;
            this.interaction = interaction;
            this.spawnPos = spawnPos;
        }

        public void cleanup() {
            if (display != null && display.isAlive()) {
                display.discard();
                display = null;
            }
            if (interaction != null && interaction.isAlive()) {
                interaction.discard();
                interaction = null;
            }
        }
    }

    public static final Map<UUID, ActiveSeerFlesh> ACTIVE_FLESHES = new HashMap<>();

    public static boolean isFleshActive(Player player) {
        if (player == null) return false;
        return ACTIVE_FLESHES.containsKey(player.getUUID());
    }

    /**
     * Kích hoạt tuyệt kỹ Thị Nhục
     */
    public static void cast(ServerLevel level, ServerPlayer player) {
        // Nếu người chơi đã có 1 Thị Nhục đang hoạt động -> Dọn dẹp cái cũ trước
        if (ACTIVE_FLESHES.containsKey(player.getUUID())) {
            ActiveSeerFlesh oldFlesh = ACTIVE_FLESHES.remove(player.getUUID());
            if (oldFlesh != null) {
                oldFlesh.cleanup();
            }
        }

        Vec3 look = player.getLookAngle();
        Vec3 forwardHorizontal = new Vec3(look.x, 0, look.z).normalize().scale(1.8D);
        Vec3 targetPos = player.position().add(forwardHorizontal);
        Vec3 groundPos = findGroundBelow(level, targetPos);

        // 1. Tạo Display Item hiển thị khối thịt 3D
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) return;

        display.moveTo(groundPos.x, groundPos.y, groundPos.z, player.getYRot() + 180.0F, 0.0F);
        ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) display;
        DisplayAccessor dispAcc = (DisplayAccessor) display;

        itemAcc.weapons$setItemStack(new ItemStack(ModItems.TAISUI_SEER_FLESH.get()));
        itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
        dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
        display.setGlowingTag(true);
        dispAcc.weapons$setGlowColorOverride(0xCC0022); // Đỏ thẫm thịt tươi
        dispAcc.weapons$setViewRange(4.0F);

        dispAcc.weapons$setTransformation(new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Quaternionf(),
                new Vector3f(2.2F, 2.2F, 2.2F),
                null
        ));
        level.addFreshEntity(display);

        // 2. Tạo Interaction Entity làm hitbox nhận diện click chuột phải và đánh chuột trái
        Interaction interaction = EntityType.INTERACTION.create(level);
        if (interaction != null) {
            interaction.moveTo(groundPos.x, groundPos.y, groundPos.z, 0.0F, 0.0F);
            InteractionAccessor intAcc = (InteractionAccessor) interaction;
            intAcc.weapons$setWidth(1.8F);
            intAcc.weapons$setHeight(2.2F);
            intAcc.weapons$setResponse(true);
            interaction.addTag("SeerFlesh_" + player.getUUID());
            level.addFreshEntity(interaction);
        }

        ActiveSeerFlesh flesh = new ActiveSeerFlesh(player.getUUID(), level, display, interaction, groundPos);
        ACTIVE_FLESHES.put(player.getUUID(), flesh);

        // 3. Âm thanh và hiệu ứng xuất hiện
        level.playSound(null, groundPos.x, groundPos.y, groundPos.z,
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 3.5F, 0.7F);
        level.playSound(null, groundPos.x, groundPos.y, groundPos.z,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 2.5F, 0.8F);
        level.playSound(null, groundPos.x, groundPos.y, groundPos.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 3.0F, 0.6F);

        level.sendParticles(ParticleTypes.ITEM_SLIME, groundPos.x, groundPos.y + 1.0D, groundPos.z, 35, 0.6D, 0.6D, 0.6D, 0.1D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, groundPos.x, groundPos.y + 0.8D, groundPos.z, 15, 0.4D, 0.4D, 0.4D, 0.03D);

        player.displayClientMessage(
                Component.literal("§c§l[THỊ THỰC] §fĐã triệu hồi Thị Thực! §a[Chuột Phải] §fhồi 100% máu, §c[Chuột Trái] §frạch lấy Mắt! §7(Sẽ tan biến nếu đi xa quá 5m)"),
                true
        );

        // Kiểm tra kích hoạt trạng thái Cộng Hưởng Thái Tuế nếu Tuyệt Diệt Tinh Tú đang bật
        TaisuiExtinctionStarsAbility.checkAndApplySynergy(level, player);
    }

    private static Vec3 findGroundBelow(ServerLevel level, Vec3 pos) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(
                Math.floor(pos.x),
                Math.floor(pos.y),
                Math.floor(pos.z)
        );

        int upLimit = 0;
        while (isSolid(level, mpos) && upLimit < 6 && mpos.getY() < level.getMaxBuildHeight()) {
            mpos.move(Direction.UP);
            upLimit++;
        }

        int downLimit = 0;
        while (!isSolid(level, mpos) && downLimit < 30 && mpos.getY() > level.getMinBuildHeight()) {
            mpos.move(Direction.DOWN);
            downLimit++;
        }

        return new Vec3(pos.x, mpos.getY() + 1.0D, pos.z);
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.blocksMotion();
    }

    /**
     * Vòng lặp Server Tick cập nhật hoạt ảnh lúc nhúc và kiểm tra khoảng cách 5m
     */
    public static void tickFleshes(ServerLevel level) {
        if (ACTIVE_FLESHES.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveSeerFlesh>> it = ACTIVE_FLESHES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveSeerFlesh> entry = it.next();
            ActiveSeerFlesh flesh = entry.getValue();

            if (flesh.level != level) continue;

            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(flesh.ownerUuid);

            // 1. Kiểm tra người triệu hồi còn sống hay không
            if (owner == null || !owner.isAlive() || owner.hasDisconnected()) {
                flesh.cleanup();
                it.remove();
                continue;
            }

            // 2. KIỂM TRA PHẠM VI 5 BLOCKS: Nếu đi xa quá 5m -> Tự động tan biến!
            double dist = owner.position().distanceTo(flesh.spawnPos);
            if (dist > 5.0D) {
                level.playSound(null, flesh.spawnPos.x, flesh.spawnPos.y + 1.0D, flesh.spawnPos.z,
                        SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 2.5F, 0.6F);
                level.playSound(null, flesh.spawnPos.x, flesh.spawnPos.y + 1.0D, flesh.spawnPos.z,
                        SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 2.0F, 0.7F);

                level.sendParticles(ParticleTypes.ITEM_SLIME, flesh.spawnPos.x, flesh.spawnPos.y + 1.0D, flesh.spawnPos.z, 30, 0.5D, 0.5D, 0.5D, 0.08D);
                level.sendParticles(ParticleTypes.SMOKE, flesh.spawnPos.x, flesh.spawnPos.y + 1.0D, flesh.spawnPos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);

                owner.displayClientMessage(
                        Component.literal("§c§l[THỊ NHỤC] §7Thị Nhục đã tan biến do bạn đã di chuyển cách xa quá 5 block!"),
                        true
                );

                flesh.cleanup();
                it.remove();
                continue;
            }

            // 3. HOẠT ẢNH LÚC NHÚC (Squirming / Throbbing Organic Animation)
            flesh.squirmTick++;
            long t = flesh.squirmTick;

            if (flesh.display != null && flesh.display.isAlive()) {
                // Co bóp bất đối xứng trên cả 3 trục X, Y, Z
                float squirmX = 2.2F + (float) Math.sin(t * 0.18F) * 0.14F + (float) Math.cos(t * 0.31F) * 0.07F;
                float squirmY = 2.2F + (float) Math.cos(t * 0.14F) * 0.16F + (float) Math.sin(t * 0.27F) * 0.05F;
                float squirmZ = 2.2F + (float) Math.sin(t * 0.22F) * 0.14F + (float) Math.cos(t * 0.19F) * 0.07F;

                // Lắc lư nghiêng ngả hữu cơ
                float tiltZ = (float) Math.sin(t * 0.12F) * 3.0F;
                float tiltX = (float) Math.cos(t * 0.15F) * 2.5F;
                Quaternionf rot = new Quaternionf()
                        .rotateZ((float) Math.toRadians(tiltZ))
                        .rotateX((float) Math.toRadians(tiltX));

                ((DisplayAccessor) flesh.display).weapons$setTransformation(new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        rot,
                        new Vector3f(squirmX, squirmY, squirmZ),
                        null
                ));
            }

            // Giọt chất nhầy / máu tươi rơi xuống sàn
            if (t % 4 == 0) {
                double rx = flesh.spawnPos.x + (level.random.nextDouble() - 0.5D) * 1.2D;
                double rz = flesh.spawnPos.z + (level.random.nextDouble() - 0.5D) * 1.2D;
                level.sendParticles(ParticleTypes.DRIPPING_HONEY, rx, flesh.spawnPos.y + 1.2D, rz, 1, 0, -0.05D, 0, 0);
            }

            // Âm thanh nhóp nhép định kỳ
            if (t % 25 == 0) {
                float pitch = 0.8F + (level.random.nextFloat() * 0.3F);
                level.playSound(null, flesh.spawnPos.x, flesh.spawnPos.y + 0.8D, flesh.spawnPos.z,
                        SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 1.2F, pitch);
            }
        }
    }

    /**
     * Bắt sự kiện chuột phải vào Thị Nhục: Tiêu thụ 1 con mắt -> Hồi 100% máu & Xóa sạch toàn bộ debuff
     */
    public static EventResult onInteract(Player player, Entity target, InteractionHand hand) {
        if (target instanceof Interaction interaction) {
            String tagPrefix = "SeerFlesh_";
            Optional<String> tagOpt = interaction.getTags().stream().filter(t -> t.startsWith(tagPrefix)).findFirst();
            if (tagOpt.isPresent()) {
                String ownerUuidStr = tagOpt.get().substring(tagPrefix.length());
                try {
                    UUID ownerUuid = UUID.fromString(ownerUuidStr);
                    ActiveSeerFlesh flesh = ACTIVE_FLESHES.get(ownerUuid);

                    if (flesh != null) {
                        // Nếu đang cầm Con Mắt Thị Nhục trên tay, không thao tác nuốt từ khối thịt
                        if (player.getItemInHand(hand).getItem() instanceof SeerFleshEyeItem) {
                            return EventResult.pass();
                        }

                        long gameTime = player.level().getGameTime();
                        if (gameTime - flesh.lastHealGameTime < 10) {
                            return EventResult.interruptTrue();
                        }
                        flesh.lastHealGameTime = gameTime;

                        if (flesh.eyeCount > 0) {
                            flesh.eyeCount--;

                            // 1. Hồi 100% Máu & No nê
                            player.setHealth(player.getMaxHealth());
                            player.getFoodData().setFoodLevel(20);
                            player.getFoodData().setSaturation(20.0F);

                            // 2. Xóa sạch mọi hiệu ứng xấu độc hại
                            SeerFleshEyeItem.cleanseHarmfulEffects(player);

                            // 3. Phước lành sinh mệnh
                            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2, false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, false, true));

                            // 4. Âm thanh và hạt
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 2.0F, 1.2F);
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 1.1F);
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.6F);

                            if (player.level() instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2D, player.getZ(), 10, 0.4D, 0.4D, 0.4D, 0.05D);
                                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.3D, 0.5D, 0.3D, 0.15D);
                            }

                            player.displayClientMessage(
                                    Component.literal("§c§l[THỊ NHỤC] §aĐã nuốt 1 con mắt! Sinh lực hồi phục 100% & Xóa sạch toàn bộ độc tố! §e(Còn " + flesh.eyeCount + " con mắt) ✨👁️"),
                                    true
                            );

                            // Nếu đã tiêu thụ hết 12 con mắt -> Khối thịt tan biến
                            if (flesh.eyeCount <= 0) {
                                player.displayClientMessage(
                                        Component.literal("§c§l[THỊ NHỤC] §7Thị Nhục đã cạn kiệt sinh lực và tan biến thành tro bụi."),
                                        true
                                );
                                flesh.cleanup();
                                ACTIVE_FLESHES.remove(ownerUuid);
                            }

                            return EventResult.interruptTrue();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return EventResult.pass();
    }

    /**
     * Bắt sự kiện tấn công (chuột trái) vào Thị Nhục: Rạch thịt thu được 1 vật phẩm [Con Mắt Thị Nhục]
     */
    public static EventResult onAttack(Player player, Level level, Entity target, InteractionHand hand, HitResult hitResult) {
        if (target instanceof Interaction interaction) {
            String tagPrefix = "SeerFlesh_";
            Optional<String> tagOpt = interaction.getTags().stream().filter(t -> t.startsWith(tagPrefix)).findFirst();
            if (tagOpt.isPresent()) {
                String ownerUuidStr = tagOpt.get().substring(tagPrefix.length());
                try {
                    UUID ownerUuid = UUID.fromString(ownerUuidStr);
                    ActiveSeerFlesh flesh = ACTIVE_FLESHES.get(ownerUuid);

                    if (flesh != null) {
                        long gameTime = level.getGameTime();
                        if (gameTime - flesh.lastHarvestGameTime < 10) {
                            return EventResult.interruptFalse();
                        }
                        flesh.lastHarvestGameTime = gameTime;

                        if (flesh.eyeCount > 0) {
                            flesh.eyeCount--;

                            // Trao tặng 1 vật phẩm Con Mắt Thị Nhục
                            ItemStack eyeStack = new ItemStack(ModItems.SEER_FLESH_EYE.get());
                            if (!player.getInventory().add(eyeStack)) {
                                player.drop(eyeStack, false);
                            }

                            level.playSound(null, target.getX(), target.getY() + 1.0D, target.getZ(),
                                    SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 2.0F, 0.85F);
                            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.5F, 1.2F);

                            if (level instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.1D);
                                sl.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.3D, 0.4D, 0.3D, 0.1D);
                            }

                            player.displayClientMessage(
                                    Component.literal("§c§l[THỊ NHỤC] §eBạn đã rạch lấy được 1 [Con Mắt Thị Nhục]! (Còn " + flesh.eyeCount + " con mắt) 👁️✨"),
                                    true
                            );

                            // Nếu đã rạch hết mắt -> Khối thịt tan biến
                            if (flesh.eyeCount <= 0) {
                                player.displayClientMessage(
                                        Component.literal("§c§l[THỊ NHỤC] §7Thị Nhục đã bị rạch hết toàn bộ mắt và tan biến."),
                                        true
                                );
                                flesh.cleanup();
                                ACTIVE_FLESHES.remove(ownerUuid);
                            }

                            return EventResult.interruptFalse();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return EventResult.pass();
    }
}
