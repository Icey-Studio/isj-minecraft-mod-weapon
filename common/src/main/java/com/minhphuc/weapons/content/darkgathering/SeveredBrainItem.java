package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class SeveredBrainItem extends Item {

    public SeveredBrainItem(Properties properties) {
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
            return Component.literal("§d§lBộ Não: §e" + entityName);
        }
        return Component.translatable(this.getDescriptionId(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§l[BỘ NÃO BỊ TÁCH RỜI]"));
        tooltip.add(Component.literal("§7Bị cắt lìa khỏi cơ thể bởi"));
        tooltip.add(Component.literal("§b§lTuyệt Kĩ Chém Xuyên Không Thời Gian§7."));
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
        tooltip.add(Component.literal("§c⚠️ Sinh vật mất não sẽ tử vong sau §415 giây §c(người chơi) hoặc §47 giây §c(quái vật)"));
        tooltip.add(Component.literal("§cnếu không được điều trị bằng Bí Mật Của Đức Tin hoặc Thị Thực!"));
        tooltip.add(Component.literal("§e✦ Chuột phải khi cầm trên tay để tự cấy ghép lại não và hóa giải án tử! ✨"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (CuuTheCurseManager.hasCurse(player)) {
            if (!level.isClientSide()) {
                CuuTheCurseManager.cure(player, "Tự Cấy Ghép Lại Bộ Não");
                player.heal(player.getMaxHealth() * 0.50F);
                player.displayClientMessage(
                        Component.literal("§d§l[CẤY GHÉP THÀNH CÔNG] §fBạn đã kịp thời cấy ghép lại Bộ Não! Trí óc thanh tỉnh, xóa bỏ án tử & hồi 50% sinh lực! ✨🧠"),
                        true
                );
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2D, player.getZ(), 25, 0.4D, 0.4D, 0.4D, 0.1D);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.3D, 0.5D, 0.3D, 0.1D);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 1.0F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.4F);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    public static ItemStack createForEntity(LivingEntity entity) {
        ItemStack stack = new ItemStack(ModItems.SEVERED_BRAIN.get());
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
