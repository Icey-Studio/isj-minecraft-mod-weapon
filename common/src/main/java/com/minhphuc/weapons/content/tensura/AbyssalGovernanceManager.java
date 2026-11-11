package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý Toàn Diện Nghiệp Vụ Ma Giới, Truyền Đạt Tư Niệm, Dịch Chuyển Thứ Nguyên
 * và Giao Ước Kiêu Hãnh Guy Crimson (Zero-Lag 20 TPS).
 */
public class AbyssalGovernanceManager {

    // =========================================================================
    // 1. LIÊN KẾT RƯƠNG CĂN CỨ & QUẢN GIA HƯ KHÔNG (AUTO-HOOVER)
    // =========================================================================

    public static void bindVault(ServerPlayer player, ItemStack pactStack, BlockPos pos) {
        if (player == null || pactStack == null || pos == null) return;
        ServerLevel level = player.serverLevel();

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container)) {
            player.displayClientMessage(Component.literal("§c[Khế Ước] Khối này không phải là Rương hoặc Hòm chứa đồ hợp lệ!"), true);
            return;
        }

        ItemStackDataHelper.putLong(pactStack, "BoundVault_Pos", pos.asLong());
        ItemStackDataHelper.putString(pactStack, "BoundVault_Dim", level.dimension().location().toString());
        ItemStackDataHelper.putBoolean(pactStack, "AutoHoover_Enabled", true);

        // Hiệu ứng hạt vòng xoáy hư không tại rương
        level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 35, 0.4D, 0.5D, 0.4D, 0.1D);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.CHEST_LOCKED, SoundSource.PLAYERS, 1.2F, 1.0F);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

        DemonType type = PrimordialPactItem.getDemonType(pactStack);
        player.displayClientMessage(Component.literal("§a✔ [Khế Ước] Đã liên kết Rương Căn Cứ cho §6" + type.getColorName() + " §atại [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]!"), true);
        player.sendSystemMessage(Component.literal("§8[" + type.getColorName() + "] §7\"Kính cẩn tuân lệnh chủ nhân. Tôi sẽ tự động thu gom mọi chiến lợi phẩm rơi vãi khi túi đồ của ngài đầy và chuyển về chiếc rương này!\""));
    }

    public static void toggleAutoHoover(ServerPlayer player, ItemStack pactStack) {
        boolean current = ItemStackDataHelper.getBoolean(pactStack, "AutoHoover_Enabled");
        boolean next = !current;
        ItemStackDataHelper.putBoolean(pactStack, "AutoHoover_Enabled", next);
        String status = next ? "§a[BẬT] - Tự động thu dọn cất kho" : "§c[TẮT] - Ngừng thu dọn";
        player.displayClientMessage(Component.literal("§e✦ Chế độ Quản Gia Hư Không: " + status), true);
    }

    public static void handleAutoHooverTick(ServerPlayer player, ItemStack pactStack) {
        if (player == null || player.isRemoved() || pactStack == null) return;
        if (!ItemStackDataHelper.getBoolean(pactStack, "AutoHoover_Enabled")) return;
        if (!ItemStackDataHelper.hasKey(pactStack, "BoundVault_Pos")) return;

        ServerLevel level = player.serverLevel();
        // Kiểm tra xem túi đồ người chơi có đầy không hoặc có nhiều item vương vãi
        boolean isInvFull = player.getInventory().getFreeSlot() == -1;

        List<ItemEntity> droppedItems = level.getEntitiesOfClass(
                ItemEntity.class,
                player.getBoundingBox().inflate(14.0D),
                item -> item.isAlive() && !item.hasPickUpDelay()
        );

        if (droppedItems.isEmpty()) return;

        // Nếu túi đồ đầy hoặc có trên 2 item rơi vãi: Kích hoạt quản gia
        if (!isInvFull && droppedItems.size() < 3) return;

        BlockPos vaultPos = BlockPos.of(ItemStackDataHelper.getLong(pactStack, "BoundVault_Pos", 0L));
        String dimStr = ItemStackDataHelper.getString(pactStack, "BoundVault_Dim");

        Container vaultContainer = null;
        if (level.dimension().location().toString().equals(dimStr) && level.isLoaded(vaultPos)) {
            BlockEntity be = level.getBlockEntity(vaultPos);
            if (be instanceof Container c) {
                vaultContainer = c;
            }
        }

        int collectedCount = 0;
        for (ItemEntity itemEntity : droppedItems) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            if (vaultContainer != null) {
                // Nhét vào rương đã liên kết
                ItemStack remainder = insertIntoContainer(vaultContainer, stack.copy());
                if (remainder.getCount() < stack.getCount()) {
                    collectedCount += (stack.getCount() - remainder.getCount());
                    if (remainder.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(remainder);
                    }
                }
            } else {
                // Nếu rương ở chunk xa/khác chiều: Cho thẳng vào túi đồ nếu còn slot hoặc chuyển về khế ước
                if (player.getInventory().add(stack.copy())) {
                    collectedCount += stack.getCount();
                    itemEntity.discard();
                }
            }
        }

        if (collectedCount > 0) {
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.8D, player.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.1D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.7F);
            DemonType type = PrimordialPactItem.getDemonType(pactStack);
            player.displayClientMessage(Component.literal("§8[" + type.getColorName() + "] §7Đã thu dọn §e" + collectedCount + " §7vật phẩm rơi vãi chuyển về Căn Cứ!"), true);
        }
    }

    private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack inSlot = container.getItem(i);
            if (inSlot.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(inSlot, stack) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                int available = inSlot.getMaxStackSize() - inSlot.getCount();
                int toAdd = Math.min(available, stack.getCount());
                inSlot.grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    // =========================================================================
    // 2. TRUYỀN ĐẠT TƯ NIỆM & NHẬP HỒN GIÁC QUAN (SENSORY RESONANCE) - CHO TẤT CẢ ÁC MA
    // =========================================================================

    public static void activateSensoryLink(ServerPlayer player, ItemStack pactStack) {
        if (player == null || pactStack == null) return;
        ServerLevel level = player.serverLevel();
        DemonType type = PrimordialPactItem.getDemonType(pactStack);

        BlockPos center = player.blockPosition();
        BlockPos nearestPos = null;
        String detectedName = "";
        double nearestDistSq = Double.MAX_VALUE;

        // Phân loại tìm kiếm theo bản thể độc bản của từng Thủy Tổ
        switch (type) {
            case JAUNE -> { // Carrera: Dò khoáng thạch tầng sâu (Ancient Debris, Kim Cương, Vàng, Redstone)
                int radius = 32;
                for (int x = -radius; x <= radius; x += 2) {
                    for (int y = Math.max(level.getMinBuildHeight(), center.getY() - 24); y <= Math.min(level.getMaxBuildHeight(), center.getY() + 16); y += 2) {
                        for (int z = -radius; z <= radius; z += 2) {
                            BlockPos checkPos = center.offset(x, y - center.getY(), z);
                            BlockState st = level.getBlockState(checkPos);
                            if (st.is(Blocks.ANCIENT_DEBRIS) || st.is(Blocks.DIAMOND_ORE) || st.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                                    || st.is(Blocks.RAW_GOLD_BLOCK) || st.is(Blocks.DEEPSLATE_GOLD_ORE)) {
                                double dSq = center.distSqr(checkPos);
                                if (dSq < nearestDistSq) {
                                    nearestDistSq = dSq;
                                    nearestPos = checkPos;
                                    detectedName = st.getBlock().getName().getString();
                                }
                            }
                        }
                    }
                }
            }
            case NOIR -> { // Diablo: Dò tìm Hòm rương kho báu, mob drops, di tích cổ
                int radius = 40;
                for (int x = -radius; x <= radius; x += 3) {
                    for (int y = -16; y <= 16; y += 3) {
                        for (int z = -radius; z <= radius; z += 3) {
                            BlockPos checkPos = center.offset(x, y, z);
                            BlockEntity be = level.getBlockEntity(checkPos);
                            if (be instanceof Container && !checkPos.equals(BlockPos.of(ItemStackDataHelper.getLong(pactStack, "BoundVault_Pos", 0L)))) {
                                double dSq = center.distSqr(checkPos);
                                if (dSq < nearestDistSq) {
                                    nearestDistSq = dSq;
                                    nearestPos = checkPos;
                                    detectedName = "Rương Kho Báu Thứ Nguyên";
                                }
                            }
                        }
                    }
                }
            }
            case BLANC -> { // Blanc: Dò tìm Dân làng, Chuông làng hoặc Quặng Ngọc Lục Bảo
                List<Villager> villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64.0D), LivingEntity::isAlive);
                if (!villagers.isEmpty()) {
                    Villager v = villagers.get(0);
                    nearestPos = v.blockPosition();
                    nearestDistSq = center.distSqr(nearestPos);
                    detectedName = "Quần Thể Dân Làng & Thương Nhân (" + v.getVillagerData().getProfession().name() + ")";
                }
            }
            case VIOLET -> { // Ultima: Dò tìm Quái Tinh Anh, Spawner, Boss nguy hiểm
                List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0D),
                        e -> e.isAlive() && (e instanceof Enemy || e.getMaxHealth() >= 50.0F));
                if (!enemies.isEmpty()) {
                    LivingEntity target = enemies.get(0);
                    nearestPos = target.blockPosition();
                    nearestDistSq = center.distSqr(nearestPos);
                    detectedName = "Mục Tiêu Ám Sát: " + target.getName().getString() + " [HP: " + (int)target.getHealth() + "]";
                }
            }
            case BLEU -> { // Rain: Dò tìm Hang Động Tinh Thể Amethyst, Prismarine, Thạch Anh
                int radius = 36;
                for (int x = -radius; x <= radius; x += 3) {
                    for (int y = -20; y <= 20; y += 3) {
                        for (int z = -radius; z <= radius; z += 3) {
                            BlockPos checkPos = center.offset(x, y, z);
                            BlockState st = level.getBlockState(checkPos);
                            if (st.is(Blocks.AMETHYST_CLUSTER) || st.is(Blocks.BUDDING_AMETHYST) || st.is(Blocks.CALCITE)) {
                                double dSq = center.distSqr(checkPos);
                                if (dSq < nearestDistSq) {
                                    nearestDistSq = dSq;
                                    nearestPos = checkPos;
                                    detectedName = "Tinh Cầu Thạch Anh Tím Ma Đạo";
                                }
                            }
                        }
                    }
                }
            }
            case VERT -> { // Misery: Dò tìm kho bãi căn cứ, cây trồng lương thực
                int radius = 32;
                for (int x = -radius; x <= radius; x += 2) {
                    for (int y = -8; y <= 8; y += 2) {
                        for (int z = -radius; z <= radius; z += 2) {
                            BlockPos checkPos = center.offset(x, y, z);
                            BlockState st = level.getBlockState(checkPos);
                            if (st.is(Blocks.CHEST) || st.is(Blocks.BARREL) || st.is(Blocks.SHULKER_BOX)) {
                                double dSq = center.distSqr(checkPos);
                                if (dSq < nearestDistSq) {
                                    nearestDistSq = dSq;
                                    nearestPos = checkPos;
                                    detectedName = "Kho Lưu Trữ Vật Tư Căn Cứ";
                                }
                            }
                        }
                    }
                }
            }
            case ROUGE -> { // Guy Crimson: Cảm tri thực thể mang ma lực áp đảo (>100 HP) hoặc Boss
                List<LivingEntity> strongMobs = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(96.0D),
                        e -> e.isAlive() && e != player && e.getMaxHealth() >= 80.0F);
                if (!strongMobs.isEmpty()) {
                    LivingEntity boss = strongMobs.get(0);
                    nearestPos = boss.blockPosition();
                    nearestDistSq = center.distSqr(nearestPos);
                    detectedName = "Thực Thể Hùng Mạnh: " + boss.getName().getString() + " [HP: " + (int)boss.getHealth() + "]";
                }
            }
        }

        if (nearestPos != null) {
            ItemStackDataHelper.putLong(pactStack, "Sensory_TargetPos", nearestPos.asLong());
            ItemStackDataHelper.putString(pactStack, "Sensory_TargetName", detectedName);

            // Bắn hạt ma thuật dẫn đường
            double dx = (nearestPos.getX() - player.getX());
            double dy = (nearestPos.getY() - player.getY());
            double dz = (nearestPos.getZ() - player.getZ());
            double dist = Math.sqrt(nearestDistSq);

            for (double t = 1.0D; t < Math.min(dist, 16.0D); t += 1.0D) {
                double px = player.getX() + (dx / dist) * t;
                double py = player.getY() + 1.2D + (dy / dist) * t;
                double pz = player.getZ() + (dz / dist) * t;
                level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F);

            player.sendSystemMessage(Component.literal("§d§l[Truyền Đạt Tư Niệm] §e" + type.getColorName() + ": §f\"Chủ nhân! Tôi đã phát hiện §6"
                    + detectedName + " §ftại [" + nearestPos.getX() + ", " + nearestPos.getY() + ", " + nearestPos.getZ() + "] (cách " + (int)dist + "m)!\""));
            player.displayClientMessage(Component.literal("§a✦ Đã khóa mục tiêu: §e" + detectedName + " §7(Nhấn [Khai Thác Từ Xa] hoặc [Dịch Chuyển])"), true);
            announceVoiceOfTheWorld(player, "Phát hiện " + detectedName + " tại [" + nearestPos.getX() + ", " + nearestPos.getY() + ", " + nearestPos.getZ() + "], cách " + (int)dist + "m.");
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.sendSystemMessage(Component.literal("§d§l[Truyền Đạt Tư Niệm] §7" + type.getColorName() + ": \"Chủ nhân, trong bán kính hiện tại chưa cảm nhận thấy dao động ma lực đặc biệt nào!\""));
            announceVoiceOfTheWorld(player, "Báo cáo: Chưa phát hiện dao động ma lực khả nghi trong bán kính quét.");
        }
    }

    // =========================================================================
    // 3. KHAI THÁC & THU HOẠCH TỪ XA (REMOTE EXTRACTION)
    // =========================================================================

    public static void remoteExtractOrMine(ServerPlayer player, ItemStack pactStack) {
        if (player == null || pactStack == null) return;
        if (!ItemStackDataHelper.hasKey(pactStack, "Sensory_TargetPos")) {
            player.displayClientMessage(Component.literal("§c[Khế Ước] Chưa có mục tiêu nào được khóa! Hãy kích hoạt [Truyền Đạt Tư Niệm] trước."), true);
            announceVoiceOfTheWorld(player, "Cảnh báo: Chưa có mục tiêu nào được khóa để khai thác.");
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos targetPos = BlockPos.of(ItemStackDataHelper.getLong(pactStack, "Sensory_TargetPos", 0L));
        String targetName = ItemStackDataHelper.getString(pactStack, "Sensory_TargetName");
        DemonType type = PrimordialPactItem.getDemonType(pactStack);

        if (player.blockPosition().distSqr(targetPos) > 128 * 128) {
            player.displayClientMessage(Component.literal("§c[Khế Ước] Mục tiêu ở quá xa phạm vi tác động ma pháp (vượt quá 128m)!"), true);
            return;
        }

        BlockState state = level.getBlockState(targetPos);
        if (!state.isAir()) {
            List<ItemStack> drops = Block.getDrops(state, level, targetPos, level.getBlockEntity(targetPos), player, new ItemStack(Items.NETHERITE_PICKAXE));
            level.destroyBlock(targetPos, false);

            for (ItemStack drop : drops) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }

            level.sendParticles(ParticleTypes.EXPLOSION, targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D, 3, 0.2D, 0.2D, 0.2D, 0.05D);
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.3D, 0.5D, 0.3D, 0.1D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.2F, 1.2F);

            player.displayClientMessage(Component.literal("§6⚡ [" + type.getColorName() + "] §aĐã bộc phá và thu hồi thành công §e" + targetName + " §avề tay bạn!"), true);
            announceVoiceOfTheWorld(player, "Bộc phá từ xa thành công. Đã thu hồi " + targetName + " vào hành trang.");
            ItemStackDataHelper.remove(pactStack, "Sensory_TargetPos");
            ItemStackDataHelper.remove(pactStack, "Sensory_TargetName");
        } else {
            ItemStack rareReward = new ItemStack(ModItems.DEMON_LORD_SOUL.get(), 2);
            if (!player.getInventory().add(rareReward)) {
                player.drop(rareReward, false);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.displayClientMessage(Component.literal("§5✦ [" + type.getColorName() + "] §aĐã đột nhập ám sát và trích xuất thành công 2x Linh Hồn Ma Vương!"), true);
            announceVoiceOfTheWorld(player, "Ám sát từ xa thành công. Đã trích xuất 2x Linh Hồn Ma Vương.");
            ItemStackDataHelper.remove(pactStack, "Sensory_TargetPos");
            ItemStackDataHelper.remove(pactStack, "Sensory_TargetName");
        }
    }

    // =========================================================================
    // 4. CÁNH CỔNG THỨ NGUYÊN & DỊCH CHUYỂN KHÔNG LẶP LẠI (ABYSSAL PORTAL GATEWAY)
    // =========================================================================

    public static void findAndOpenPortal(ServerPlayer player, ItemStack pactStack) {
        if (player == null || pactStack == null) return;
        ServerLevel level = player.serverLevel();
        DemonType type = PrimordialPactItem.getDemonType(pactStack);

        String explored = ItemStackDataHelper.getString(pactStack, "ExploredDestinations");

        TagKey<Structure> targetTag = null;
        ResourceKey<Structure> targetKey = null;
        String destTitle = "Di Tích Thần Bí";
        boolean isUnderground = false;
        boolean isNether = level.dimension() == Level.NETHER;

        switch (type) {
            case BLANC -> {
                targetTag = net.minecraft.tags.StructureTags.VILLAGE;
                destTitle = "Thôn Làng Dân Cư";
                isUnderground = false;
            }
            case NOIR -> {
                targetKey = BuiltinStructures.ANCIENT_CITY;
                destTitle = "Thành Cổ Cổ Đại (Ancient City)";
                isUnderground = true;
            }
            case JAUNE -> {
                targetKey = BuiltinStructures.TRIAL_CHAMBERS;
                destTitle = "Mật Viện Thử Thách (Trial Chambers)";
                isUnderground = true;
            }
            case VIOLET -> {
                targetKey = BuiltinStructures.FORTRESS;
                destTitle = "Pháo Đài Địa Ngục (Nether Fortress)";
                isNether = true;
            }
            case BLEU -> {
                targetKey = BuiltinStructures.OCEAN_MONUMENT;
                destTitle = "Thần Điện Đại Dương (Ocean Monument)";
            }
            case VERT -> {
                warpHome(player, pactStack);
                return;
            }
            case ROUGE -> {
                targetKey = BuiltinStructures.BASTION_REMNANT;
                destTitle = "Tàn Tích Pháo Đài Heo Vàng (Bastion)";
                isNether = true;
            }
        }

        BlockPos foundPos = null;
        if (targetTag != null) {
            foundPos = level.findNearestMapStructure(targetTag, player.blockPosition(), 100, true);
        } else if (targetKey != null) {
            var holderSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(targetKey);
            if (holderSet.isPresent()) {
                var pair = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holderSet.get()), player.blockPosition(), 100, true);
                if (pair != null) {
                    foundPos = pair.getFirst();
                }
            }
        }

        if (foundPos == null) {
            foundPos = level.findNearestMapStructure(net.minecraft.tags.StructureTags.EYE_OF_ENDER_LOCATED, player.blockPosition(), 120, true);
            destTitle = "Pháo Đài Thư Viện Stronghold";
            isUnderground = true;
        }

        if (foundPos != null) {
            String coordKey = foundPos.getX() + ":" + foundPos.getZ();
            if (explored.contains(coordKey)) {
                player.displayClientMessage(Component.literal("§e[Khế Ước] Di tích gần nhất đã từng chinh phục! Đang mở rộng cảm tri tìm tọa độ mới toanh..."), true);
            }

            ItemStackDataHelper.putString(pactStack, "ExploredDestinations", explored + coordKey + ";");

            // TÌM TỌA ĐỘ AN TOÀN TUYỆT ĐỐI - KHÔNG BAO GIỜ BỊ KẸT TRONG ĐÁ
            BlockPos safePos = findSafeTeleportPos(level, foundPos, isNether, isUnderground);

            // Dịch chuyển tức thời
            player.teleportTo(safePos.getX() + 0.5D, safePos.getY() + 0.1D, safePos.getZ() + 0.5D);
            player.fallDistance = 0.0F;

            // Ban phát buff bảo vệ hạ cánh chống ngạt thở, chống dung nham & nhìn đêm
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));

            // Hiệu ứng hạt Cổng Ma Giới
            level.sendParticles(ParticleTypes.PORTAL, safePos.getX() + 0.5D, safePos.getY() + 1.0D, safePos.getZ() + 0.5D, 60, 0.6D, 1.0D, 0.6D, 0.2D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, safePos.getX() + 0.5D, safePos.getY() + 0.5D, safePos.getZ() + 0.5D, 30, 0.4D, 0.4D, 0.4D, 0.05D);
            level.playSound(null, safePos.getX(), safePos.getY(), safePos.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.0F);

            announceVoiceOfTheWorld(player, "Cổng Ma Giới đã kết nối thành công tới " + destTitle + " tại [" + safePos.getX() + ", " + safePos.getY() + ", " + safePos.getZ() + "]. Nơi này đã được đánh dấu chinh phục!");
        } else {
            player.displayClientMessage(Component.literal("§c[Khế Ước] Không tìm thấy điểm kết nối không gian khả dụng trong bán kính 10,000 blocks!"), true);
            announceVoiceOfTheWorld(player, "Cảnh báo: Không tìm thấy cấu trúc khả dụng trong bán kính quy định.");
        }
    }

    public static void warpHome(ServerPlayer player, ItemStack pactStack) {
        if (!ItemStackDataHelper.hasKey(pactStack, "BoundVault_Pos")) {
            player.displayClientMessage(Component.literal("§c[Khế Ước] Chưa liên kết Rương Căn Cứ! Cầm Khế Ước Shift + Chuột Phải vào rương ở nhà để đăng ký."), true);
            announceVoiceOfTheWorld(player, "Cảnh báo: Chưa đăng ký Rương Căn Cứ. Hãy cầm khế ước Shift + Chuột Phải vào rương.");
            return;
        }

        BlockPos vaultPos = BlockPos.of(ItemStackDataHelper.getLong(pactStack, "BoundVault_Pos", 0L));
        ServerLevel level = player.serverLevel();

        BlockPos safePos = findSafeTeleportPos(level, vaultPos, false, false);
        player.teleportTo(safePos.getX() + 0.5D, safePos.getY() + 0.1D, safePos.getZ() + 0.5D);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 4, false, false));

        level.sendParticles(ParticleTypes.PORTAL, safePos.getX() + 0.5D, safePos.getY() + 1.0D, safePos.getZ() + 0.5D, 40, 0.5D, 0.8D, 0.5D, 0.1D);
        level.playSound(null, safePos.getX(), safePos.getY(), safePos.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.0F);

        player.displayClientMessage(Component.literal("§a✔ [Cổng Không Gian] Đã xé rách thứ nguyên trở về Rương Căn Cứ an toàn!"), true);
        announceVoiceOfTheWorld(player, "Đã mở cổng thứ nguyên trở về Rương Căn Cứ an toàn.");
    }

    // =========================================================================
    // THUẬT TOÁN TÌM ĐIỂM DỊCH CHUYỂN AN TOÀN (CHỐNG KẸT TRONG ĐÁ 100%)
    // =========================================================================

    public static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos basePos, boolean isNether, boolean isUnderground) {
        int x = basePos.getX();
        int z = basePos.getZ();

        // 1. Kiểm tra bề mặt nếu là cấu trúc lộ thiên Overworld
        if (!isNether && !isUnderground) {
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (surfaceY > level.getMinBuildHeight() + 5 && surfaceY < level.getMaxBuildHeight() - 5) {
                BlockPos candidate = new BlockPos(x, surfaceY, z);
                if (isSafeStand(level, candidate)) {
                    return candidate;
                }
            }
        }

        // 2. Quét không gian rỗng / hang động an toàn xung quanh (Bán kính 20 blocks)
        int minY = isNether ? 32 : (isUnderground ? Math.max(level.getMinBuildHeight() + 4, -54) : Math.max(60, basePos.getY() - 15));
        int maxY = isNether ? 100 : (isUnderground ? Math.min(40, basePos.getY() + 30) : Math.min(level.getMaxBuildHeight() - 10, basePos.getY() + 30));

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int r = 0; r <= 24; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int curX = x + dx;
                    int curZ = z + dz;

                    for (int curY = maxY; curY >= minY; curY--) {
                        mpos.set(curX, curY, curZ);
                        if (isSafeStand(level, mpos)) {
                            return mpos.immutable();
                        }
                    }
                }
            }
        }

        // 3. Cơ Chế Bảo Hộ Tuyệt Đối: Tạo buồng ma pháp an toàn 3x3x3 nếu toàn đá đặc
        int fallbackY = isNether ? 64 : (isUnderground ? -42 : Math.max(64, basePos.getY()));
        BlockPos safePocket = new BlockPos(x, fallbackY, z);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = safePocket.offset(dx, -1, dz);
                if (!level.getBlockState(floor).isSolid()) {
                    level.setBlockAndUpdate(floor, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos air = safePocket.offset(dx, dy, dz);
                    if (level.getBlockState(air).isSolid()) {
                        level.setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        return safePocket;
    }

    private static boolean isSafeStand(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        boolean goodGround = ground.isSolid() && !ground.is(Blocks.LAVA) && !ground.is(Blocks.FIRE) && !ground.is(Blocks.MAGMA_BLOCK);
        boolean clearFeet = !feet.isSolid() && !feet.is(Blocks.LAVA);
        boolean clearHead = !head.isSolid() && !head.is(Blocks.LAVA);

        return goodGround && clearFeet && clearHead;
    }

    public static void announceVoiceOfTheWorld(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§e§l<< THÔNG BÁO: GIỌNG NÓI THẾ GIỚI >>"));
        player.sendSystemMessage(Component.literal("§f<< " + message + " >>"));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("§e<< " + message + " >>")));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("§6§lGIỌNG NÓI THẾ GIỚI")));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // =========================================================================
    // 5. BLANC (TESTAROSSA) — ĐẠI LÃNH ĐỊA BẢO HỘ THÔN LÀNG, KIẾN THIẾT & THU THUẾ (CẤP 1–5)
    // =========================================================================

    public static void establishVillageProtectorate(ServerPlayer player, ItemStack pactStack) {
        if (player == null || pactStack == null) return;
        ServerLevel level = player.serverLevel();

        BlockPos currentPos = player.blockPosition();
        ItemStackDataHelper.putLong(pactStack, "ProtectedVillage_Pos", currentPos.asLong());
        ItemStackDataHelper.putInt(pactStack, "Village_TributeTimer", 0);
        int currentLevel = ItemStackDataHelper.getInt(pactStack, "Protectorate_Level");
        if (currentLevel <= 0) {
            currentLevel = 1;
            ItemStackDataHelper.putInt(pactStack, "Protectorate_Level", 1);
        }

        // Nếu Blanc đang được triệu hồi gần đó -> Tự động chuyển Blanc sang chế độ Tuần Tra Canh Giữ Lãnh Địa
        List< com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity> blancs = level.getEntitiesOfClass(
                com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity.class,
                player.getBoundingBox().inflate(64.0D),
                d -> d.isAlive() && d.isOwnedBy(player) && d.getDemonType() == DemonType.BLANC
        );
        for (com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity blanc : blancs) {
            blanc.setMassacreMode(false);
            blanc.setGuardMode(true, currentPos);
            ItemStackDataHelper.putBoolean(pactStack, "IsGuarding", true);
            blanc.broadcastNearbyDialogue("Theo ý nguyện của Chủ nhân, ta sẽ biến ngôi làng này thành Thánh Đô Bạch Viêm bất khả xâm phạm!", 48.0D);
        }

        // 1. Thanh tẩy toàn bộ quái vật & Raider xung quanh 85m bằng Bạch Viêm Phán Quyết
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(85.0D),
                e -> e instanceof Enemy || e instanceof net.minecraft.world.entity.raid.Raider);
        for (LivingEntity hostile : hostiles) {
            hostile.igniteForSeconds(10.0F);
            hostile.hurt(level.damageSources().magic(), 80.0F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hostile.getX(), hostile.getY() + 1.0D, hostile.getZ(), 15, 0.3D, 0.5D, 0.3D, 0.05D);
        }

        // 2. Chữa lành & ban phước cho toàn bộ Dân Làng và Iron Golem trong bán kính 64m
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64.0D), LivingEntity::isAlive);
        for (Villager v : villagers) {
            v.setHealth(v.getMaxHealth());
            v.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1, false, false));
            v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1, false, false));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.5D, v.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        // 3. Triệu hồi & Cường hóa Quân Đoàn Hộ Vệ Bạch Viêm (Frost Guard Iron Golems)
        List<net.minecraft.world.entity.animal.IronGolem> golems = level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.IronGolem.class, player.getBoundingBox().inflate(64.0D), LivingEntity::isAlive);
        for (net.minecraft.world.entity.animal.IronGolem golem : golems) {
            buffFrostGuardGolem(golem);
        }
        if (golems.size() < 2) {
            int toSpawn = 2 - golems.size();
            for (int i = 0; i < toSpawn; i++) {
                net.minecraft.world.entity.animal.IronGolem newGolem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
                if (newGolem != null) {
                    BlockPos spawnPos = findSafeTeleportPos(level, currentPos.offset((i == 0 ? 3 : -3), 0, 3), false, false);
                    newGolem.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                    newGolem.setPlayerCreated(true);
                    buffFrostGuardGolem(newGolem);
                    level.addFreshEntity(newGolem);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, newGolem.getX(), newGolem.getY() + 1.0D, newGolem.getZ(), 25, 0.4D, 0.8D, 0.4D, 0.05D);
                }
            }
        }

        // 4. Ban Đặc Quyền Lãnh Chúa ngay lập tức cho người chơi
        player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 2400, 4, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1, false, true));

        level.sendParticles(ParticleTypes.END_ROD, currentPos.getX() + 0.5D, currentPos.getY() + 2.0D, currentPos.getZ() + 0.5D, 80, 1.5D, 1.5D, 1.5D, 0.15D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, currentPos.getX() + 0.5D, currentPos.getY() + 1.0D, currentPos.getZ() + 0.5D, 60, 2.0D, 0.5D, 2.0D, 0.08D);
        level.playSound(null, currentPos.getX(), currentPos.getY(), currentPos.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);

        player.sendSystemMessage(Component.literal(
                "§f§l✦ ĐẠI LÃNH ĐỊA BẢO HỘ BLANC (CẤP " + currentLevel + "/5) KHỞI TẠO ✦\n" +
                "§e• Kết Giới Bạch Viêm (85m): §fTự động thiêu rụi quái vật & Raid xâm lược.\n" +
                "§b• Quân Đoàn Hộ Vệ: §fCường hóa & triệu hồi Hộ Vệ Bạch Viêm Iron Golem.\n" +
                "§a• Kiến Thiết & Thịnh Vượng: §fTự động lấp hố, cắm Đèn Linh Hồn, tăng dân số & nâng cấp nghề Dân Làng lên Master.\n" +
                "§6• Đặc Quyền Lãnh Chúa: §fNhận Anh Hùng Thôn Làng V (giảm giá tối đa) & Cống phẩm đa tầng định kỳ!"
        ));
        player.displayClientMessage(Component.literal("§a✔ Đã thiết lập Đại Lãnh Địa Bảo Hộ Blanc (Cấp " + currentLevel + "/5)!"), true);
    }

    private static void buffFrostGuardGolem(net.minecraft.world.entity.animal.IronGolem golem) {
        golem.setHealth(golem.getMaxHealth());
        golem.setCustomName(Component.literal("§f§l⚔ Hộ Vệ Bạch Viêm Của Blanc ⚔"));
        golem.setCustomNameVisible(false);
        golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1, false, false));
        golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1, false, false));
        golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, false));
        golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, false, false));
    }

    public static void handleProtectorateTick(ServerPlayer player, ItemStack pactStack) {
        if (!ItemStackDataHelper.hasKey(pactStack, "ProtectedVillage_Pos")) return;

        ServerLevel level = player.serverLevel();
        BlockPos villagePos = BlockPos.of(ItemStackDataHelper.getLong(pactStack, "ProtectedVillage_Pos", 0L));
        int prosperityLevel = Math.max(1, Math.min(5, ItemStackDataHelper.getInt(pactStack, "Protectorate_Level")));
        int timer = ItemStackDataHelper.getInt(pactStack, "Village_TributeTimer");
        timer++;

        double distSqToVillage = player.blockPosition().distSqr(villagePos);
        boolean inProtectorate = distSqToVillage <= 85.0D * 85.0D;

        // =====================================================================
        // 1. KHI LÃNH CHÚA Ở TRONG ĐỊA BÀN BẢO HỘ (Mỗi 80 ticks = 4 giây)
        // =====================================================================
        if (inProtectorate && timer % 80 == 0) {
            // A. Đặc Quyền Lãnh Chúa: Hero of the Village V + Hồi phục + Kháng cự + Bão hòa thức ăn
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 300, 4, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 160, 0, false, false, false));

            AABB villageBox = new AABB(villagePos).inflate(85.0D, 32.0D, 85.0D);

            // B. Cơ Chế Chống Raid & Boss Tuyệt Đối (Cột Lửa Bạch Viêm Trừng Phạt)
            List<LivingEntity> invaders = level.getEntitiesOfClass(LivingEntity.class, villageBox,
                    e -> e.isAlive() && (e instanceof Enemy || e instanceof net.minecraft.world.entity.raid.Raider)
                            && !(e instanceof com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity d && d.isTame()));
            for (LivingEntity invader : invaders) {
                invader.igniteForSeconds(6.0F);
                invader.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                invader.hurt(level.damageSources().magic(), 35.0F + prosperityLevel * 10.0F);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, invader.getX(), invader.getY() + 1.0D, invader.getZ(), 10, 0.3D, 0.5D, 0.3D, 0.04D);
                level.sendParticles(ParticleTypes.END_ROD, invader.getX(), invader.getY() + 2.0D, invader.getZ(), 6, 0.2D, 0.8D, 0.2D, 0.02D);
            }

            // C. Bảo Hộ Dân Làng & Hành Lễ Kính Chào Lãnh Chúa
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, villageBox, LivingEntity::isAlive);
            for (Villager v : villagers) {
                if (v.getHealth() < v.getMaxHealth()) {
                    v.heal(4.0F);
                }
                v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, false));
                if (v.distanceToSqr(player) <= 64.0D && level.random.nextFloat() < 0.25F) {
                    v.getLookControl().setLookAt(player, 30.0F, 30.0F);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.6D, v.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.02D);
                }
            }

            // D. Duy trì & Cường Hóa Quân Đoàn Hộ Vệ Bạch Viêm (Frost Guard Iron Golems)
            List<net.minecraft.world.entity.animal.IronGolem> golems = level.getEntitiesOfClass(
                    net.minecraft.world.entity.animal.IronGolem.class, villageBox, LivingEntity::isAlive);
            for (net.minecraft.world.entity.animal.IronGolem golem : golems) {
                buffFrostGuardGolem(golem);
            }
        }

        // =====================================================================
        // 2. TỰ ĐỘNG KIẾN THIẾT, THẮP SÁNG & THĂNG CẤP DÂN CƯ (Mỗi 400 ticks = 20 giây)
        // =====================================================================
        if (inProtectorate && timer % 400 == 0) {
            AABB villageBox = new AABB(villagePos).inflate(72.0D, 28.0D, 72.0D);
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, villageBox, LivingEntity::isAlive);

            // A. Tăng dân số tự động theo cấp độ Thịnh Vượng (Tối đa 8..20 dân làng)
            int maxVillagers = 6 + prosperityLevel * 3;
            if (villagers.size() < maxVillagers && !villagers.isEmpty()) {
                Villager created = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                if (created != null) {
                    BlockPos spawnPos = findSafeTeleportPos(level, villagePos.offset(level.random.nextInt(16) - 8, 0, level.random.nextInt(16) - 8), false, false);
                    created.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                    level.addFreshEntity(created);
                    level.sendParticles(ParticleTypes.HEART, created.getX(), created.getY() + 1.5D, created.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.03D);
                }
            }

            // B. Thăng cấp nghề nghiệp của Dân Làng lên bậc Master (Level 5)
            for (Villager v : villagers) {
                var vData = v.getVillagerData();
                if (vData.getProfession() != net.minecraft.world.entity.npc.VillagerProfession.NONE && vData.getLevel() < 5) {
                    v.setVillagerData(vData.setLevel(vData.getLevel() + 1));
                    v.setVillagerXp(Math.max(v.getVillagerXp(), 250));
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.5D, v.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.05D);
                    break; // Mỗi chu kỳ thăng cấp 1 thương nhân
                }
            }

            // C. Bổ sung Hộ Vệ Bạch Viêm nếu thiếu (Tối đa 1 + prosperityLevel Golem)
            List<net.minecraft.world.entity.animal.IronGolem> golems = level.getEntitiesOfClass(
                    net.minecraft.world.entity.animal.IronGolem.class, villageBox, LivingEntity::isAlive);
            if (golems.size() < 1 + prosperityLevel) {
                net.minecraft.world.entity.animal.IronGolem newGolem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
                if (newGolem != null) {
                    BlockPos spawnPos = findSafeTeleportPos(level, villagePos.offset(level.random.nextInt(14) - 7, 0, level.random.nextInt(14) - 7), false, false);
                    newGolem.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                    newGolem.setPlayerCreated(true);
                    buffFrostGuardGolem(newGolem);
                    level.addFreshEntity(newGolem);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, newGolem.getX(), newGolem.getY() + 1.0D, newGolem.getZ(), 20, 0.4D, 0.8D, 0.4D, 0.05D);
                }
            }

            // D. Sửa chữa hố nổ & Thắp sáng thôn làng bằng Đèn Linh Hồn (Soul Lantern)
            for (int attempt = 0; attempt < 6; attempt++) {
                int rx = villagePos.getX() + level.random.nextInt(48) - 24;
                int rz = villagePos.getZ() + level.random.nextInt(48) - 24;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rx, rz);
                BlockPos surfacePos = new BlockPos(rx, surfaceY, rz);
                BlockPos groundPos = surfacePos.below();
                BlockState groundState = level.getBlockState(groundPos);

                if (groundState.is(Blocks.DIRT)) {
                    level.setBlockAndUpdate(groundPos, Blocks.GRASS_BLOCK.defaultBlockState());
                } else if (groundState.isSolid() && level.getBlockState(surfacePos).isAir()
                        && level.getMaxLocalRawBrightness(surfacePos) < 7) {
                    level.setBlockAndUpdate(surfacePos, Blocks.SOUL_LANTERN.defaultBlockState());
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, rx + 0.5D, surfaceY + 0.5D, rz + 0.5D, 8, 0.2D, 0.2D, 0.2D, 0.02D);
                    break;
                }
            }
        }

        // =====================================================================
        // 3. CỐNG NẠP ĐỊNH KỲ & THĂNG CẤP THỊNH VƯỢNG (Mỗi 12,000 ticks = Nửa ngày Minecraft)
        // =====================================================================
        if (timer >= 12000) {
            timer = 0;

            // Thăng cấp Thịnh Vượng Lãnh Địa (tối đa Cấp 5)
            if (prosperityLevel < 5) {
                prosperityLevel++;
                ItemStackDataHelper.putInt(pactStack, "Protectorate_Level", prosperityLevel);
                player.sendSystemMessage(Component.literal(
                        "§b§l✦ [THỊNH VƯỢNG THĂNG CẤP] §fLãnh Địa Bảo Hộ của Blanc đã phát triển lên §e§lCẤP " + prosperityLevel + "/5§f! Cống phẩm và Quân Đoàn Hộ Vệ được cường hóa mạnh mẽ!"
                ));
            }

            List<ItemStack> tributes = new ArrayList<>();
            tributes.add(new ItemStack(Items.EMERALD, 16 * prosperityLevel));
            tributes.add(new ItemStack(Items.GOLDEN_CARROT, 8 * prosperityLevel));
            tributes.add(new ItemStack(Items.IRON_INGOT, 8 * prosperityLevel));

            if (prosperityLevel >= 2) {
                tributes.add(new ItemStack(Items.GOLD_INGOT, 6 * prosperityLevel));
                tributes.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 6 * prosperityLevel));
            }
            if (prosperityLevel >= 3) {
                tributes.add(new ItemStack(Items.DIAMOND, 3 * prosperityLevel));
                tributes.add(new ItemStack(Items.GOLDEN_APPLE, 2 * prosperityLevel));
            }
            if (prosperityLevel >= 4) {
                tributes.add(new ItemStack(Items.EMERALD_BLOCK, 2 * prosperityLevel));
                tributes.add(new ItemStack(Items.NETHERITE_SCRAP, 2));
            }
            if (prosperityLevel >= 5) {
                tributes.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                tributes.add(new ItemStack(ModItems.DEMON_LORD_SOUL.get(), 2));
            }

            for (ItemStack reward : tributes) {
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }

            player.sendSystemMessage(Component.literal(
                    "§f§l[Thuế Lãnh Địa Blanc - Cấp " + prosperityLevel + "/5] §aThần dân Thánh Đô Bạch Viêm đã kính dâng cống phẩm định kỳ ("
                            + (16 * prosperityLevel) + "x Ngọc Lục Bảo, Kim Loại Quý, Khoáng Sản & Phẩm Vật Hoàng Gia) vào túi đồ của bạn!"
            ));
            player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);
        }

        ItemStackDataHelper.putInt(pactStack, "Village_TributeTimer", timer);
    }

    // =========================================================================
    // 6. ROUGE (GUY CRIMSON) — VÁN CỜ MA GIỚI & TRỌNG TÀI CỨU VIỆN
    // =========================================================================

    public static void checkGuyLifelineTick(ServerPlayer player, ItemStack pactStack) {
        if (player == null || player.isRemoved() || pactStack == null) return;
        if (PrimordialPactItem.getDemonType(pactStack) != DemonType.ROUGE) return;

        ServerLevel level = player.serverLevel();
        float currentHp = player.getHealth();
        float maxHp = player.getMaxHealth();

        // Kích hoạt khi máu dưới 20% (nguy cơ tử vong cao)
        if (currentHp <= maxHp * 0.20F || currentHp <= 8.0F) {
            long now = level.getGameTime();
            long cd = ItemStackDataHelper.getLong(pactStack, "Guy_InterventionCooldown", 0L);

            if (now >= cd) {
                // Thiết lập hồi chiêu 1 ngày in-game (24,000 ticks)
                ItemStackDataHelper.putLong(pactStack, "Guy_InterventionCooldown", now + 24000L);

                // Cứu viện khẩn cấp: Đẩy lùi toàn bộ quái vật
                List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20.0D), e -> e != player);
                for (LivingEntity m : nearbyMobs) {
                    double kx = m.getX() - player.getX();
                    double kz = m.getZ() - player.getZ();
                    m.knockback(2.5D, -kx, -kz);
                    m.igniteForSeconds(6.0F);
                }

                // Hồi máu và ban phát bất tử tạm thời
                player.setHealth(maxHp * 0.8F);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 4, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 3, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 3, false, true));

                // Hiệu ứng chấn động đỏ rực rách không gian
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.5D, player.getZ(), 50, 0.8D, 1.0D, 0.8D, 0.1D);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.8F);

                player.sendSystemMessage(Component.literal("§4§l✦ TRỌNG TÀI GIÁNG LÂM: GUY CRIMSON ✦ §c\"Hmph... Đừng có gục ngã ở cái nơi thảm hại này chứ, kẻ thú vị! Hãy đứng lên và kết liễu bọn chúng đi!\""));
            }
        }
    }

    public static void claimGuyGambit(ServerPlayer player, ItemStack pactStack, int gambitId) {
        if (player == null || pactStack == null) return;
        ServerLevel level = player.serverLevel();

        if (gambitId == 1) { // Thử thách 1: Hiến tế 16 Linh Hồn Ma Vương đổi lấy Ngọc Thức Tỉnh Ma Vương
            int soulCount = PrimordialPactItem.countSoulsInInventory(player);
            if (soulCount >= 16) {
                PrimordialPactItem.consumeSouls(player, 16);
                ItemStack orb = new ItemStack(ModItems.DEMON_LORD_AWAKENING_ORB.get());
                if (!player.getInventory().add(orb)) player.drop(orb, false);

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5F, 1.0F);
                player.sendSystemMessage(Component.literal("§4§l[Ván Cờ Ma Giới] §eGuy Crimson: §f\"Khá lắm! 16 linh hồn này đủ cho thấy bản lĩnh của ngươi. Cầm lấy Viên Ngọc Thức Tỉnh này đi!\""));
            } else {
                player.displayClientMessage(Component.literal("§c[Guy Crimson] Ngươi chưa đủ 16 Linh Hồn Ma Vương để hoàn tất ván cờ này!"), true);
            }
        } else if (gambitId == 2) { // Thử thách 2: Trọng lượng kim loại quý (1 Khối Netherite)
            ItemStack netheriteReward = new ItemStack(Items.NETHERITE_INGOT, 4);
            if (!player.getInventory().add(netheriteReward)) player.drop(netheriteReward, false);
            player.sendSystemMessage(Component.literal("§4§l[Ván Cờ Ma Giới] §eGuy Crimson: §f\"Đây là 4 Thỏi Netherite ma hỏa dành riêng cho ngươi.\""));
        }
    }
}
