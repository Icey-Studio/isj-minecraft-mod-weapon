package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.mixin.TextDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PrimordialSummonRitual {

    public record SacrificeRequirement(Item item, int count, String displayName) {}

    public enum SummonState {
        WAITING_SACRIFICE,
        RISING
    }

    public static class ActiveSummon {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public final DemonType demonType;
        public final boolean isWinged;
        public final SacrificeRequirement sacrifice;

        public SummonState state;
        public Display.ItemDisplay magicCircle;
        public Display.TextDisplay textDisplay;
        public PrimordialDemonEntity demon;
        public int ticksRemaining;
        public final int totalTicks;
        public LivingEntity initialTarget;

        public ActiveSummon(ServerLevel level, ServerPlayer caster, Vec3 center,
                            DemonType demonType, boolean isWinged, SacrificeRequirement sacrifice,
                            SummonState state, int totalTicks) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.demonType = demonType;
            this.isWinged = isWinged;
            this.sacrifice = sacrifice;
            this.state = state;
            this.ticksRemaining = totalTicks;
            this.totalTicks = totalTicks;
        }

        public void cleanup() {
            if (magicCircle != null && magicCircle.isAlive()) {
                magicCircle.discard();
                magicCircle = null;
            }
            if (textDisplay != null && textDisplay.isAlive()) {
                textDisplay.discard();
                textDisplay = null;
            }
        }
    }

    private static final List<ActiveSummon> ACTIVE_SUMMONS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static SacrificeRequirement rollSacrifice(DemonType type) {
        DemonType.SacrificeTier tier = type.getSacrificeTier();
        return switch (tier) {
            case EASY -> {
                // 10% cơ hội yêu cầu 1 Linh Hồn Ma Vương, 90% đồ cơ bản vanilla
                if (RANDOM.nextFloat() < 0.10F) {
                    yield new SacrificeRequirement(ModItems.DEMON_LORD_SOUL.get(), 1, "§bLinh Hồn Ma Vương");
                }
                Item[] easyPool = new Item[]{
                        Items.APPLE, Items.BREAD, Items.IRON_INGOT, Items.COAL,
                        Items.COOKED_BEEF, Items.OAK_LOG, Items.WHEAT, Items.FEATHER,
                        Items.STRING, Items.BONE, Items.GUNPOWDER, Items.SNOWBALL, Items.ICE
                };
                Item chosen = easyPool[RANDOM.nextInt(easyPool.length)];
                int count = (chosen == Items.OAK_LOG || chosen == Items.WHEAT || chosen == Items.SNOWBALL) ? 16 :
                        (chosen == Items.COAL || chosen == Items.BREAD ? 8 : 4);
                yield new SacrificeRequirement(chosen, count, chosen.getName(ItemStack.EMPTY).getString());
            }
            case MEDIUM -> {
                // 25% cơ hội yêu cầu 1 Linh Hồn Ma Vương, 75% đồ ma pháp/giả kim
                if (RANDOM.nextFloat() < 0.25F) {
                    yield new SacrificeRequirement(ModItems.DEMON_LORD_SOUL.get(), 1, "§bLinh Hồn Ma Vương");
                }
                Item[] medPool = new Item[]{
                        Items.AMETHYST_SHARD, Items.POISONOUS_POTATO, Items.SPIDER_EYE,
                        Items.FERMENTED_SPIDER_EYE, Items.SLIME_BALL, Items.HONEY_BOTTLE,
                        Items.ENDER_PEARL, Items.BLAZE_POWDER, Items.GHAST_TEAR, Items.PHANTOM_MEMBRANE, Items.BREWING_STAND
                };
                Item chosen = medPool[RANDOM.nextInt(medPool.length)];
                int count = (chosen == Items.SPIDER_EYE || chosen == Items.AMETHYST_SHARD || chosen == Items.SLIME_BALL) ? 4 :
                        (chosen == Items.BLAZE_POWDER || chosen == Items.ENDER_PEARL || chosen == Items.FERMENTED_SPIDER_EYE ? 2 : 1);
                yield new SacrificeRequirement(chosen, count, chosen.getName(ItemStack.EMPTY).getString());
            }
            case HARD -> {
                // 40% cơ hội yêu cầu 2-3 Linh Hồn Ma Vương, 60% khoáng sản quý/vật phẩm Nether
                if (RANDOM.nextFloat() < 0.40F) {
                    int sc = 2 + RANDOM.nextInt(2);
                    yield new SacrificeRequirement(ModItems.DEMON_LORD_SOUL.get(), sc, "§bLinh Hồn Ma Vương");
                }
                Item[] hardPool = new Item[]{
                        Items.DIAMOND, Items.GOLD_BLOCK, Items.OBSIDIAN, Items.CRYING_OBSIDIAN,
                        Items.DRAGON_BREATH, Items.ECHO_SHARD, Items.NETHERITE_SCRAP, Items.EMERALD_BLOCK,
                        Items.GOLDEN_APPLE, Items.ANVIL
                };
                Item chosen = hardPool[RANDOM.nextInt(hardPool.length)];
                int count = (chosen == Items.OBSIDIAN || chosen == Items.CRYING_OBSIDIAN) ? 6 :
                        (chosen == Items.DIAMOND || chosen == Items.GOLD_BLOCK || chosen == Items.GOLDEN_APPLE || chosen == Items.EMERALD_BLOCK ? 2 : 1);
                yield new SacrificeRequirement(chosen, count, chosen.getName(ItemStack.EMPTY).getString());
            }
            case HARDEST -> {
                // 50% cơ hội yêu cầu 4-6 Linh Hồn Ma Vương, 50% báu vật cực phẩm tối thượng
                if (RANDOM.nextFloat() < 0.50F) {
                    int sc = 4 + RANDOM.nextInt(3);
                    yield new SacrificeRequirement(ModItems.DEMON_LORD_SOUL.get(), sc, "§bLinh Hồn Ma Vương");
                }
                Item[] hardestPool = new Item[]{
                        Items.NETHER_STAR, Items.NETHERITE_INGOT, Items.ENCHANTED_GOLDEN_APPLE,
                        Items.WITHER_SKELETON_SKULL, Items.TOTEM_OF_UNDYING, Items.HEART_OF_THE_SEA,
                        Items.HEAVY_CORE, Items.BEACON
                };
                Item chosen = hardestPool[RANDOM.nextInt(hardestPool.length)];
                int count = (chosen == Items.WITHER_SKELETON_SKULL ? 2 : 1);
                yield new SacrificeRequirement(chosen, count, chosen.getName(ItemStack.EMPTY).getString());
            }
        };
    }

    /**
     * Khởi tạo Pháp Trận Chờ Vật Tế (Gọi từ Pháp Điển Sách)
     */
    public static void startWaitingRitual(ServerLevel level, ServerPlayer caster, Vec3 groundPos, DemonType demonType, boolean isWinged) {
        if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(level, groundPos)) {
            if (caster != null) {
                caster.displayClientMessage(
                        Component.literal("§c⚠️ Không thể tiến hành nghi thức triệu hồi Ác Ma bên trong Kháng Ma Kết Giới! Nồng độ ma tử đã bị triệt tiêu!"),
                        true
                );
            }
            return;
        }

        Vec3 center = findGroundBelow(level, groundPos);
        SacrificeRequirement sacrifice = rollSacrifice(demonType);

        // Chờ vật tế trong tối đa 5 phút (6000 tick)
        ActiveSummon ritual = new ActiveSummon(level, caster, center, demonType, isWinged, sacrifice, SummonState.WAITING_SACRIFICE, 6000);

        // 1. Tạo Pháp Trận Triệu Hồi Ma Giới xoay chậm trên mặt đất
        Display.ItemDisplay circleDisplay = EntityType.ITEM_DISPLAY.create(level);
        if (circleDisplay != null) {
            circleDisplay.moveTo(center.x, center.y + 0.04D, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) circleDisplay;
            DisplayAccessor dispAcc = (DisplayAccessor) circleDisplay;

            itemAcc.weapons$setItemStack(new ItemStack(ModItems.DEMON_SUMMONING_CIRCLE.get()));
            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            circleDisplay.setGlowingTag(true);
            dispAcc.weapons$setGlowColorOverride(demonType.getGlowColor());
            dispAcc.weapons$setViewRange(8.0F);

            Quaternionf rot = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rot,
                    new Vector3f(9.0F, 9.0F, 0.01F),
                    null
            ));

            level.addFreshEntity(circleDisplay);
            ritual.magicCircle = circleDisplay;
        }

        // 2. Tạo Hologram Text Display lơ lửng hiển thị rõ vật tế yêu cầu
        Display.TextDisplay textDisplay = EntityType.TEXT_DISPLAY.create(level);
        if (textDisplay != null) {
            textDisplay.moveTo(center.x, center.y + 1.8D, center.z, 0.0F, 0.0F);
            DisplayAccessor dispAcc = (DisplayAccessor) textDisplay;
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            dispAcc.weapons$setViewRange(10.0F);

            Component text = Component.literal(
                    "§c§l✦ PHÁP TRẬN HIỆU TRIỆU: §e§l" + demonType.getColorName().toUpperCase() + " ✦\n" +
                    "§fVật tế yêu cầu: §6§l" + sacrifice.displayName() + " x" + sacrifice.count() + " §7(" + demonType.getSacrificeTier().getDisplayName() + "§7)"
            );
            ((TextDisplayAccessor) textDisplay).weapons$setText(text);
            level.addFreshEntity(textDisplay);
            ritual.textDisplay = textDisplay;
        }

        ACTIVE_SUMMONS.add(ritual);

        // Âm thanh khởi động pháp trận
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0F, 1.0F);

        caster.displayClientMessage(
                Component.literal("§c§l[PHÁP TRẬN ĐÃ MỞ] §eYêu cầu vật tế: §6" + sacrifice.displayName() + " x" + sacrifice.count() + "§e. Buộc dâng nộp!"),
                false
        );
    }

    /**
     * Triệu hồi khẩn cấp / Huyết tế (Từ Dân làng, Phù thủy, Kẻ cướp tự hy sinh)
     */
    public static void startImmediate(ServerLevel level, Vec3 groundPos, DemonType demonType, LivingEntity target) {
        if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(level, groundPos)) {
            return; // Kháng Ma Kết Giới triệt tiêu hoàn toàn nghi thức huyết tế triệu hồi Ác Ma
        }

        Vec3 center = findGroundBelow(level, groundPos);
        ActiveSummon ritual = new ActiveSummon(level, null, center, demonType, demonType == DemonType.NOIR, null, SummonState.RISING, 90);
        ritual.initialTarget = target;

        Display.ItemDisplay circleDisplay = EntityType.ITEM_DISPLAY.create(level);
        if (circleDisplay != null) {
            circleDisplay.moveTo(center.x, center.y + 0.04D, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) circleDisplay;
            DisplayAccessor dispAcc = (DisplayAccessor) circleDisplay;

            itemAcc.weapons$setItemStack(new ItemStack(ModItems.DEMON_SUMMONING_CIRCLE.get()));
            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            circleDisplay.setGlowingTag(true);
            dispAcc.weapons$setGlowColorOverride(demonType.getGlowColor());
            dispAcc.weapons$setViewRange(6.0F);

            Quaternionf rot = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rot,
                    new Vector3f(9.0F, 9.0F, 0.01F),
                    null
            ));

            level.addFreshEntity(circleDisplay);
            ritual.magicCircle = circleDisplay;
        }

        PrimordialDemonEntity demon = ModEntities.PRIMORDIAL_DEMON.get().create(level);
        if (demon != null) {
            demon.moveTo(center.x, center.y - 2.4D, center.z, target != null ? target.getYRot() + 180.0F : 0.0F, 0.0F);
            demon.setDemonType(demonType);
            demon.setWinged(demonType == DemonType.NOIR);
            demon.setRising(true);
            demon.setRisingProgress(0.0F);
            demon.setTame(false, false);
            if (target != null) {
                demon.setTarget(target);
            }
            level.addFreshEntity(demon);
            ritual.demon = demon;
        }

        ACTIVE_SUMMONS.add(ritual);

        level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.0F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 1.0F);
    }

    /**
     * Kích hoạt Ác Ma trồi lên khi đã nhận đủ vật tế
     */
    private static void activateRisingDemon(ActiveSummon r, ServerPlayer player) {
        // Xóa bảng chữ hologram
        if (r.textDisplay != null && r.textDisplay.isAlive()) {
            r.textDisplay.discard();
            r.textDisplay = null;
        }

        // Kích hoạt giai đoạn trồi lên của Ác Ma!
        r.state = SummonState.RISING;
        r.ticksRemaining = 90;

        // Tạo Ác Ma dưới lòng đất (-2.4m)
        PrimordialDemonEntity demon = ModEntities.PRIMORDIAL_DEMON.get().create(r.level);
        if (demon != null) {
            float yRot = player != null ? player.getYRot() + 180.0F : 0.0F;
            demon.moveTo(r.center.x, r.center.y - 2.4D, r.center.z, yRot, 0.0F);
            demon.setDemonType(r.demonType);
            demon.setWinged(r.isWinged);
            demon.setRising(true);
            demon.setRisingProgress(0.0F);
            demon.setTame(false, false);
            if (player != null) {
                demon.setTarget(player);
            }
            r.level.addFreshEntity(demon);
            r.demon = demon;
        }

        // Hiệu ứng hiến tế thành công bùng nổ
        r.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, r.center.x, r.center.y + 0.5D, r.center.z, 50, 0.8D, 0.5D, 0.8D, 0.08D);
        r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.center.x, r.center.y + 0.8D, r.center.z, 30, 0.6D, 0.6D, 0.6D, 0.05D);
        r.level.playSound(null, r.center.x, r.center.y, r.center.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.5F, 0.8F);
        r.level.playSound(null, r.center.x, r.center.y, r.center.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.5F, 1.0F);

        Component announceMsg = Component.literal("§a§l[VẬT TẾ ĐÃ ĐƯỢC DÂNG NỘP] §eÁc Ma Thủy Tổ §6" + r.demonType.getColorName() + " §eđáp lời hiến tế và bắt đầu thức tỉnh!");
        if (player != null) {
            player.displayClientMessage(announceMsg, true);
        }
        for (ServerPlayer sp : r.level.players()) {
            if (sp.distanceToSqr(r.center) <= 30.0 * 30.0) {
                sp.displayClientMessage(announceMsg, false);
            }
        }
    }

    /**
     * Tự động kiểm tra vật phẩm hiến tế ném/rơi vào pháp trận (ItemEntity)
     */
    private static void checkThrownSacrifice(ActiveSummon r) {
        if (r.sacrifice == null || r.state != SummonState.WAITING_SACRIFICE) return;
        AABB box = new AABB(r.center.x - 4.5D, r.center.y - 1.5D, r.center.z - 4.5D,
                r.center.x + 4.5D, r.center.y + 2.5D, r.center.z + 4.5D);
        List<ItemEntity> items = r.level.getEntitiesOfClass(ItemEntity.class, box,
                ie -> ie.isAlive() && ie.getItem().is(r.sacrifice.item()));

        if (items.isEmpty()) return;

        int totalFound = 0;
        for (ItemEntity ie : items) {
            totalFound += ie.getItem().getCount();
        }

        if (totalFound >= r.sacrifice.count()) {
            int needed = r.sacrifice.count();
            for (ItemEntity ie : items) {
                ItemStack stack = ie.getItem();
                int take = Math.min(stack.getCount(), needed);
                stack.shrink(take);
                needed -= take;
                if (stack.isEmpty()) {
                    ie.discard();
                } else {
                    ie.setItem(stack);
                }
                if (needed <= 0) break;
            }

            activateRisingDemon(r, null);
        }
    }

    /**
     * Kiểm tra khi người chơi click nộp vật tế vào pháp trận
     */
    public static boolean offerSacrifice(ServerPlayer player, InteractionHand hand, Vec3 clickPos) {
        if (ACTIVE_SUMMONS.isEmpty()) return false;

        ItemStack held = player.getItemInHand(hand);

        for (ActiveSummon r : ACTIVE_SUMMONS) {
            if (r.level != player.serverLevel()) continue;
            if (r.state != SummonState.WAITING_SACRIFICE) continue;

            double distSq = Math.min(r.center.distanceToSqr(clickPos), r.center.distanceToSqr(player.position()));
            if (distSq <= 7.0D * 7.0D) {
                SacrificeRequirement req = r.sacrifice;
                if (req == null) continue;

                if (held.is(req.item())) {
                    if (held.getCount() >= req.count()) {
                        held.shrink(req.count());
                        activateRisingDemon(r, player);
                        return true;
                    } else {
                        player.displayClientMessage(
                                Component.literal("§c§l[CHƯA ĐỦ VẬT TẾ] §ePháp trận đòi hỏi §6" + req.count() + " " + req.displayName() + "§e! Hiện tại bạn chỉ cầm §c" + held.getCount() + "§e!"),
                                true
                        );
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return true;
                    }
                } else if (!held.isEmpty()) {
                    player.displayClientMessage(
                            Component.literal("§c§l[SAI VẬT TẾ] §ePháp trận của §6" + r.demonType.getColorName() + " §eyêu cầu: §6" + req.displayName() + " x" + req.count() + "§e! (buộc cống nộp)"),
                            true
                    );
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                    return true;
                } else {
                    // Tay không click vào pháp trận -> Nhắc nhở vật tế
                    player.displayClientMessage(
                            Component.literal("§e§l[PHÁP TRẬN HIỆU TRIỆU] §fYêu cầu vật tế: §6" + req.displayName() + " x" + req.count() + "§e. buộc phải dâng"),
                            true
                    );
                    return true;
                }
            }
        }
        return false;
    }

    public static void tickRituals(ServerLevel serverLevel) {
        if (ACTIVE_SUMMONS.isEmpty()) return;

        Iterator<ActiveSummon> it = ACTIVE_SUMMONS.iterator();
        while (it.hasNext()) {
            ActiveSummon r = it.next();
            if (r.level != serverLevel) continue;

            r.ticksRemaining--;

            // GIAI ĐOẠN 1: CHỜ VẬT TẾ (WAITING_SACRIFICE)
            if (r.state == SummonState.WAITING_SACRIFICE) {
                // Tự động kiểm tra vật phẩm hiến tế ném/rơi vào pháp trận
                checkThrownSacrifice(r);
                if (r.state != SummonState.WAITING_SACRIFICE) {
                    continue;
                }

                // Xoay nhẹ pháp trận
                int elapsed = r.totalTicks - r.ticksRemaining;
                if (r.magicCircle != null && r.magicCircle.isAlive()) {
                    DisplayAccessor dispAcc = (DisplayAccessor) r.magicCircle;
                    Quaternionf rot = new Quaternionf()
                            .rotateX((float) Math.toRadians(90.0F))
                            .rotateZ((float) Math.toRadians(elapsed * 0.8F));
                    dispAcc.weapons$setTransformation(new Transformation(
                            new Vector3f(0.0F, 0.0F, 0.0F),
                            rot,
                            new Vector3f(9.0F, 9.0F, 0.01F),
                            null
                    ));
                }

                // Hạt lửa ma giới bốc lên nhẹ nhàng quanh vành pháp trận
                if (elapsed % 4 == 0) {
                    double angle = (elapsed * 3.0D) % 360.0D;
                    double rad = Math.toRadians(angle);
                    double cx = r.center.x + Math.cos(rad) * 4.2D;
                    double cz = r.center.z + Math.sin(rad) * 4.2D;
                    r.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, r.center.y + 0.2D, cz, 1, 0, 0.02D, 0, 0.01D);
                }

                // Hết hạn 5 phút mà không nộp vật tế -> Hủy pháp trận
                if (r.ticksRemaining <= 0) {
                    r.level.playSound(null, r.center.x, r.center.y, r.center.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2.0F, 1.0F);
                    r.level.sendParticles(ParticleTypes.SMOKE, r.center.x, r.center.y + 0.5D, r.center.z, 30, 0.5D, 0.5D, 0.5D, 0.05D);
                    r.cleanup();
                    it.remove();
                }
                continue;
            }

            // GIAI ĐOẠN 2: ĐÃ CÓ VẬT TẾ -> ÁC MA TRỒI LÊN TỪ LÒNG ĐẤT (RISING)
            int elapsed = 90 - r.ticksRemaining;
            float progress = (float) elapsed / 90.0F;

            // 1. ÁC MA TỪ TỪ TRỒI TỪ DƯỚI ĐẤT LÊN
            if (r.demon != null && r.demon.isAlive()) {
                double currentY = r.center.y - 2.4D + (2.4D * progress);
                r.demon.moveTo(r.center.x, currentY, r.center.z, (float) (elapsed * 2.5), 0.0F);
                r.demon.setRisingProgress(progress);
                r.demon.setDeltaMovement(0, 0, 0);

                r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.center.x, currentY + 0.5D, r.center.z, 8, 0.4D, 0.4D, 0.4D, 0.02D);
                r.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, r.center.x, currentY + 0.8D, r.center.z, 4, 0.3D, 0.5D, 0.3D, 0.02D);
            }

            // 2. PHÁP TRẬN XOAY NHANH DẦN
            if (r.magicCircle != null && r.magicCircle.isAlive()) {
                DisplayAccessor dispAcc = (DisplayAccessor) r.magicCircle;
                Quaternionf rot = new Quaternionf()
                        .rotateX((float) Math.toRadians(90.0F))
                        .rotateZ((float) Math.toRadians(elapsed * 2.5F));
                dispAcc.weapons$setTransformation(new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        rot,
                        new Vector3f(9.0F, 9.0F, 0.01F),
                        null
                ));
            }

            // 3. 16 NGỌN NẾN ĐỎ BỐC CHÁY RỰC LỬA TRÊN VÀNH PHÁP TRẬN
            double candleRadius = 4.2D;
            for (int i = 0; i < 16; i++) {
                double angle = (i * 2.0D * Math.PI) / 16.0D + Math.toRadians(elapsed * 2.0D);
                double cx = r.center.x + Math.cos(angle) * candleRadius;
                double cz = r.center.z + Math.sin(angle) * candleRadius;

                if (elapsed % 3 == 0) {
                    r.level.sendParticles(ParticleTypes.FLAME, cx, r.center.y + 0.15D, cz, 1, 0.02D, 0.05D, 0.02D, 0.01D);
                    r.level.sendParticles(ParticleTypes.SMOKE, cx, r.center.y + 0.25D, cz, 1, 0.01D, 0.05D, 0.01D, 0.01D);
                }
            }

            if (elapsed % 20 == 0) {
                r.level.playSound(null, r.center.x, r.center.y, r.center.z,
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.5F, 0.7F + (progress * 0.4F));
            }

            // GIAI ĐOẠN 3: HOÀN TẤT TRIỆU HỒI & BÙNG NỔ
            if (elapsed >= 90) {
                LivingEntity enemyTarget = r.caster != null ? r.caster : r.initialTarget;

                if (r.demon != null && r.demon.isAlive()) {
                    r.demon.setRising(false);
                    r.demon.setRisingProgress(1.0F);
                    r.demon.moveTo(r.center.x, r.center.y, r.center.z, enemyTarget != null ? enemyTarget.getYRot() + 180.0F : 0.0F, 0.0F);
                    if (enemyTarget != null) {
                        r.demon.setTarget(enemyTarget);
                    }
                }

                r.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, r.center.x, r.center.y + 1.0D, r.center.z, 1, 0, 0, 0, 0);
                r.level.sendParticles(ParticleTypes.FLASH, r.center.x, r.center.y + 1.5D, r.center.z, 2, 0, 0, 0, 0);
                r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.center.x, r.center.y + 1.0D, r.center.z, 60, 1.5D, 1.0D, 1.5D, 0.1D);
                r.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, r.center.x, r.center.y + 1.0D, r.center.z, 40, 1.2D, 0.8D, 1.2D, 0.08D);

                r.level.playSound(null, r.center.x, r.center.y, r.center.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 0.9F);
                r.level.playSound(null, r.center.x, r.center.y, r.center.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 1.2F);
                r.level.playSound(null, r.center.x, r.center.y, r.center.z,
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.8F);

                if (enemyTarget instanceof ServerPlayer sp && sp.connection != null) {
                    sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§l★ THỬ THÁCH MA GIỚI ★")));
                    sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + r.demonType.getColorName() + " §7đang khiêu chiến! Hãy đánh bại nó để thu phục!")));
                    VoiceOfTheWorld.announce(sp, "Báo cáo. Thủy Tổ Ác Ma: " + r.demonType.getColorName() + " (" + r.demonType.getTitleVi() + ") đang khiêu chiến bạn! Hãy đánh bại nó để khiến nó quy phục!");
                    sp.displayClientMessage(
                            Component.literal("§d§l[" + r.demonType.getColorName().toUpperCase() + "] §f\"" + r.demonType.getSummonDialogue() + "\""),
                            false
                    );
                }

                r.cleanup();
                it.remove();
            }
        }
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
}
