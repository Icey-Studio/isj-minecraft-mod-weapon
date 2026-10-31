package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class SeveredHeartItem extends Item {

    public SeveredHeartItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        String entityName = ItemStackDataHelper.getString(stack, "EntityName");
        if (entityName != null && !entityName.isEmpty()) {
            return Component.literal("§c§lTrái Tim: §e" + entityName);
        }
        return Component.translatable(this.getDescriptionId(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§l[TRÁI TIM BỊ TÁCH RỜI]"));
        tooltip.add(Component.literal("§7Bị cắt lìa khỏi cơ thể bởi"));
        tooltip.add(Component.literal("§b§lTuyệt Kĩ Chém Xuyên Thời Gian§7."));
        tooltip.add(Component.literal(""));

        String entityName = ItemStackDataHelper.getString(stack, "EntityName");
        String entityType = ItemStackDataHelper.getString(stack, "EntityType");
        if (entityName != null && !entityName.isEmpty()) {
            tooltip.add(Component.literal("§fNguyên chủ: §e" + entityName));
        }
        if (entityType != null && !entityType.isEmpty()) {
            tooltip.add(Component.literal("§7Chủng loài: §b" + entityType));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§c⚠️ Sinh vật mất tim sẽ tử vong sau §45 giây"));
        tooltip.add(Component.literal("§cnếu không được điều trị bằng Thánh Quang hoặc Vật Tổ!"));
    }

    public static ItemStack createForEntity(LivingEntity entity) {
        ItemStack stack = new ItemStack(ModItems.SEVERED_HEART.get());
        String name = entity.getName().getString();
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        ItemStackDataHelper.putString(stack, "EntityName", name);
        ItemStackDataHelper.putString(stack, "EntityType", typeId);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID("EntityUUID", entity.getUUID());
        });
        return stack;
    }
}
