package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

public class TomeLootManager {

    /**
     * Tự động kiểm tra và chèn Sách Cổ Khởi Nguyên Thủy Tổ vào các rương kho báu thế giới
     * khi người chơi mở rương tự nhiên lần đầu tiên.
     */
    public static void onOpenContainer(ServerPlayer player, BlockPos pos) {
        if (player.level().isClientSide()) return;

        BlockEntity be = player.level().getBlockEntity(pos);
        if (be == null) return;

        // Chỉ kiểm tra rương tự nhiên (RandomizableContainerBlockEntity hoặc ChestBlockEntity)
        if (be instanceof RandomizableContainerBlockEntity rcbe) {
            // Nếu rương có LootTable, tức là rương cấu trúc tự nhiên chưa unpack!
            ResourceKey<LootTable> lootTable = rcbe.getLootTable();
            if (lootTable != null) {
                String path = lootTable.location().getPath();
                // Rương kho báu tự nhiên (chests/...)
                if (path.contains("chest") || path.contains("dungeon") || path.contains("fortress")
                        || path.contains("stronghold") || path.contains("city") || path.contains("temple")
                        || path.contains("mineshaft") || path.contains("bastion") || path.contains("ruin")) {

                    boolean isRareStructure = path.contains("ancient_city") || path.contains("end_city")
                            || path.contains("bastion") || path.contains("stronghold") || path.contains("mansion");

                    float chance = isRareStructure ? 0.45F : 0.25F;

                    if (player.level().random.nextFloat() <= chance) {
                        // Unpack rương trước nếu chưa unpack
                        rcbe.unpackLootTable(player);
                        insertTomesIntoContainer(rcbe, player.level().random, isRareStructure);
                    }
                }
            }
        }
    }

    private static void insertTomesIntoContainer(Container container, net.minecraft.util.RandomSource random, boolean isRareStructure) {
        boolean hasRebirth = false;
        boolean hasEvolution = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(ModItems.PRIMORDIAL_REBIRTH_TOME.get())) hasRebirth = true;
            if (stack.is(ModItems.SKILL_EVOLUTION_TOME.get())) hasEvolution = true;
        }

        // 1. Chèn Sách Cổ Khởi Nguyên Thủy Tổ
        if (!hasRebirth) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, new ItemStack(ModItems.PRIMORDIAL_REBIRTH_TOME.get()));
                    break;
                }
            }
        }

        // 2. Chèn Quyển Thư Tiến Hóa Kỹ Năng (Tăng độ hiếm: Di tích cổ đại 25%, rương thường chỉ 8%)
        float evoChance = isRareStructure ? 0.25F : 0.08F;
        if (!hasEvolution && random.nextFloat() <= evoChance) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, new ItemStack(ModItems.SKILL_EVOLUTION_TOME.get()));
                    break;
                }
            }
        }
    }
}
