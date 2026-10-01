package com.minhphuc.weapons.content.infinitygauntlet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.world.item.Item.TooltipContext;
import java.util.List;

public class InfinityGauntletItem extends Item {
    public static final String NBT_MODE = "SelectedMode";

    public InfinityGauntletItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static String getModeName(int modeOrdinal) {
        return switch (modeOrdinal) {
            case 1 -> "§dĐá Sức Mạnh (Power Stone)";
            case 2 -> "§9Đá Không Gian (Space Stone)";
            case 3 -> "§cĐá Thực Tại (Reality Stone)";
            case 4 -> "§6Đá Linh Hồn (Soul Stone)";
            case 5 -> "§aĐá Thời Gian (Time Stone)";
            case 6 -> "§eĐá Tâm Trí (Mind Stone)";
            case 7 -> "§6§lSỨC MẠNH 6 VIÊN ĐÁ (BÚNG TAY / SNAP)";
            default -> "§7Chưa chọn (Nhấn PgUp)";
        };
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            boolean isHolding = player.getMainHandItem() == stack || player.getOffhandItem() == stack;

            if (isHolding) {
                // 1. Khôi phục sinh lực tức thì khi bị mất máu
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }

                // Dập lửa và làm đầy thanh dưỡng khí khi cần
                if (player.isOnFire()) {
                    player.clearFire();
                }
                if (player.getAirSupply() < player.getMaxAirSupply()) {
                    player.setAirSupply(player.getMaxAirSupply());
                }

                // 2. Chỉ cấp/làm mới hiệu ứng mỗi giây (20 ticks), tránh spam 200 packets/s
                if (player.tickCount % 20 == 0) {
                    int duration = 240; // 12 giây bộ đệm
                    applyOrRefreshEffect(player, MobEffects.DAMAGE_RESISTANCE, duration, 4);
                    applyOrRefreshEffect(player, MobEffects.REGENERATION, duration, 4);
                    applyOrRefreshEffect(player, MobEffects.FIRE_RESISTANCE, duration, 0);
                    applyOrRefreshEffect(player, MobEffects.WATER_BREATHING, duration, 0);
                    applyOrRefreshNightVision(player); // Vĩnh cửu 999999 ticks, không bao giờ nhấp nháy màn hình
                    applyOrRefreshEffect(player, MobEffects.DAMAGE_BOOST, duration, 9);
                    applyOrRefreshEffect(player, MobEffects.DIG_SPEED, duration, 4);
                    applyOrRefreshEffect(player, MobEffects.MOVEMENT_SPEED, duration, 1);
                    applyOrRefreshEffect(player, MobEffects.SATURATION, duration, 4);
                    applyOrRefreshEffect(player, MobEffects.ABSORPTION, duration, 4);
                }

                // Frost Walker: Tự động đóng băng nước dưới chân khi ở Chế độ Frozen của Đá Thực Tại (kiểm tra mỗi 4 ticks, dùng flag 2)
                int mode = ItemStackDataHelper.getInt(stack, NBT_MODE);
                int subMode = ItemStackDataHelper.getInt(stack, "RealitySubMode");
                if (mode == 3 && subMode == 1 && player.onGround() && player.tickCount % 4 == 0) {
                    net.minecraft.core.BlockPos feet = player.blockPosition();
                    net.minecraft.core.BlockPos below = feet.below();
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            net.minecraft.core.BlockPos checkPos = below.offset(x, 0, z);
                            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);
                            if (state.is(net.minecraft.world.level.block.Blocks.WATER)) {
                                level.setBlock(checkPos, net.minecraft.world.level.block.Blocks.FROSTED_ICE.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    private void applyOrRefreshNightVision(Player player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || !current.isInfiniteDuration()) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
        }
    }

    private void applyOrRefreshEffect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() <= 60) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int mode = ItemStackDataHelper.getInt(stack, NBT_MODE);

        if (mode == 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                    Component.literal("§c[Găng Tay Vô Cực] §fBáo cáo. Vui lòng nhấn nút PgUp để chọn viên đá hoặc chế độ 6 viên trước!"),
                    true
                );
                level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        if (mode == 1) {
            // Power Stone Mode
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                PowerStoneAbility.executePowerStone(serverLevel, serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (mode == 2) {
            // Space Stone Mode
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                SpaceStoneAbility.executeSpaceStone(serverLevel, serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (mode == 3) {
            // Reality Stone Mode
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                RealityStoneAbility.executeRealityStone(serverLevel, serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (mode == 4) {
            // Soul Stone Mode
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                SoulStoneAbility.executeSoulStone(serverLevel, serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (mode == 5) {
            // Time Stone Mode
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                TimeStoneAbility.executeTimeStone(serverLevel, serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (mode == 6) {
            // Mind Stone Mode
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                MindStoneAbility.executeMindStone(serverLevel, serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (mode == 7) {
            // SNAP (6 Stones Mode)
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                ServerPlayer serverPlayer = (ServerPlayer) player;

                // Nếu giữ Shift -> Bắn Chùm Laze Vũ Trụ 6 Sắc Màu
                if (player.isShiftKeyDown()) {
                    fireInfinityBeamArray(serverLevel, serverPlayer, stack);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }

                // Sound effects for snap
                serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN,
                    SoundSource.PLAYERS,
                    1.0F, 0.8F
                );
                serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
                );

                serverPlayer.displayClientMessage(
                    Component.literal("§d§l[BÁO CÁO] §fBáo cáo! kích hoạt sức mạnh vô cực! đã thành công"),
                    true
                );

                // Spatial scan & batch destruction executed safely on main server thread
                AABB boundingBox = player.getBoundingBox().inflate(100.0D);
                List<Entity> targetEntities = serverLevel.getEntities((Entity) null, boundingBox, entity -> {
                    if (entity == player) return false;
                    return (entity instanceof LivingEntity && entity.isAlive()) || (entity instanceof ItemEntity);
                });

                int killedMobs = 0;
                int removedItems = 0;

                for (Entity entity : targetEntities) {
                    if (!entity.isAlive() && !(entity instanceof ItemEntity)) continue;

                            if (entity instanceof LivingEntity living && entity != player) {
                                // Xử lý rơi Linh Hồn Ma Vương cho cú búng tay SNAP
                                com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(serverPlayer, living);
                                living.hurt(serverLevel.damageSources().playerAttack(serverPlayer), 100000.0F);
                                if (living.isAlive()) {
                                    living.discard();
                                }
                                killedMobs++;
                            } else if (entity instanceof ItemEntity itemEntity) {
                                itemEntity.discard();
                                removedItems++;
                            }
                        }

                        // Spawn snap visual particle effects around player
                        for (int i = 0; i < 360; i += 10) {
                            double rad = Math.toRadians(i);
                            double px = player.getX() + Math.cos(rad) * 3.0D;
                            double pz = player.getZ() + Math.sin(rad) * 3.0D;
                            serverLevel.sendParticles(
                                ParticleTypes.END_ROD,
                                px, player.getY() + 1.0D, pz,
                                2, 0.1D, 0.5D, 0.1D, 0.05D
                            );
                            serverLevel.sendParticles(
                                ParticleTypes.DRAGON_BREATH,
                                px, player.getY() + 0.5D, pz,
                                3, 0.2D, 0.2D, 0.2D, 0.02D
                            );
                        }

                        serverLevel.sendParticles(
                            ParticleTypes.FLASH,
                            player.getX(), player.getY() + 1.5D, player.getZ(),
                            1, 0, 0, 0, 0
                        );

                        serverPlayer.sendSystemMessage(
                            Component.literal("§a§l[SNAP SUCCESS] Đã quét sạch " + killedMobs + " sinh vật và xóa " + removedItems + " vật phẩm trong phạm vi 100 blocks!")
                        );

                player.getCooldowns().addCooldown(this, 40); // 2 second cooldown
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // Individual stone actions can be added here in future
        if (!level.isClientSide()) {
            player.displayClientMessage(
                Component.literal("§e[Găng Tay Vô Cực] Đã kích hoạt " + getModeName(mode) + "!"),
                true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Bắn Chùm Laze Vũ Trụ 6 Sắc Màu Xoắn Quẩy (Mode 7 Shift + Chuột Phải)
     */
    private static void fireInfinityBeamArray(ServerLevel level, ServerPlayer player, ItemStack stack) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.8F);

        net.minecraft.world.phys.Vec3 start = player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        double maxDist = 80.0D;

        java.util.Set<LivingEntity> killed = new java.util.HashSet<>();

        for (double d = 0; d <= maxDist; d += 0.6D) {
            net.minecraft.world.phys.Vec3 centerPos = start.add(look.scale(d));

            // Bắn 6 chùm tia với 6 sắc màu hạt khác nhau xoắn tròn quanh trục
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians((d * 40.0D) + (i * 60.0D));
                double offsetRadius = 0.6D;
                double ox = Math.cos(angle) * offsetRadius;
                double oy = Math.sin(angle) * offsetRadius;

                net.minecraft.core.particles.ParticleOptions particle = switch (i) {
                    case 0 -> ParticleTypes.DRAGON_BREATH; // Purple (Power)
                    case 1 -> ParticleTypes.PORTAL; // Blue (Space)
                    case 2 -> ParticleTypes.CRIMSON_SPORE; // Red (Reality)
                    case 3 -> ParticleTypes.SOUL_FIRE_FLAME; // Orange (Soul)
                    case 4 -> ParticleTypes.HAPPY_VILLAGER; // Green (Time)
                    default -> ParticleTypes.WAX_OFF; // Yellow (Mind)
                };

                level.sendParticles(particle, centerPos.x + ox, centerPos.y + oy, centerPos.z, 2, 0.04D, 0.04D, 0.04D, 0.01D);
            }

            if ((int)(d * 2) % 8 == 0) {
                level.sendParticles(ParticleTypes.FLASH, centerPos.x, centerPos.y, centerPos.z, 1, 0, 0, 0, 0);
            }

            AABB hitBox = new AABB(
                centerPos.x - 1.5D, centerPos.y - 1.5D, centerPos.z - 1.5D,
                centerPos.x + 1.5D, centerPos.y + 1.5D, centerPos.z + 1.5D
            );
            List<LivingEntity> hitEntities = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e.isAlive());

            for (LivingEntity entity : hitEntities) {
                if (!killed.contains(entity)) {
                    com.minhphuc.weapons.content.tensura.TensuraEvents.handleMobDeathDrop(player, entity);
                    entity.hurt(level.damageSources().playerAttack(player), 100000.0F);
                    if (entity.isAlive()) {
                        entity.discard();
                    }
                    killed.add(entity);
                }
            }
        }

        player.displayClientMessage(
            Component.literal("§6§l[INFINITY LAZER] §fBáo cáo. Đã bắn chùm laze vũ trụ 6 sắc màu! (Xóa sổ " + killed.size() + " cá thể) ✦"),
            true
        );

        player.getCooldowns().addCooldown(stack.getItem(), 15);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int mode = ItemStackDataHelper.getInt(stack, NBT_MODE);
        tooltip.add(Component.literal("§7Chế độ hiện tại: " + getModeName(mode)));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Sức Mạnh Thường Trực Khi Cầm:"));
        tooltip.add(Component.literal("§7- §aTự động hồi 100% máu lập tức"));
        tooltip.add(Component.literal("§7- §aKháng 100% sát thương từ sinh vật, ngã, lửa, lava & ngạt nước"));
        tooltip.add(Component.literal("§7- §aFull hiệu ứng tích cực (Sức mạnh X, Hấp thụ, Tốc độ, Nhìn đêm, Nhanh nhẹn...)"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7- Hướng dẫn: Nhấn phím §e[PgUp] §7để chọn chức năng."));
        tooltip.add(Component.literal("§7- §d🔮 Đá Sức Mạnh (Power Stone)§7: Bắn laze hủy diệt / Bộc phát xung giải thoát không gian kín."));
        tooltip.add(Component.literal("§7- §9🌌 Đá Không Gian (Space Stone)§7: Menu Dịch Chuyển Không Gian tức thời (Teleport Menu)."));
        tooltip.add(Component.literal("§7- §c🔴 Đá Thực Tại (Reality Stone)§7: Đóng băng địa hình & sinh vật (bị đánh vỡ vụn). Nhìn lên trời đổi thời tiết."));
        tooltip.add(Component.literal("§7- §6💀 Đá Linh Hồn (Soul Stone)§7: Gặt hái linh hồn (Hồi máu & sát thương). Shift+Chuột phải: Tách linh hồn (Soul Extraction)."));
        tooltip.add(Component.literal("§7- §a⌛ Đá Thời Gian (Time Stone)§7: Lão Hóa & Sinh Trưởng (Age Decay & Growth: Già hóa/suy yếu quái, thúc đẩy cây cối)."));
        tooltip.add(Component.literal("§7- §e🧠 Đá Tâm Trí (Mind Stone)§7: Bắn Laze Tâm Trí Vision. Shift+Chuột phải: Telekinesis nhấc ném mục tiêu."));
        tooltip.add(Component.literal("§7- §6✦ 6 Viên Đá (Snap)§7: Chuột phải búng tay diệt quái / Shift+Chuột phải bắn Laze Vũ Trụ 6 sắc màu. Gõ chat để Gemini AI thực thi!"));
    }
}


