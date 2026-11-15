package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.ElementalSpiritEntity;
import com.minhphuc.weapons.entity.tensura.ElementalType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Tế Đàn Tinh Linh (Spirit Shrine):
 * - Dùng chất xúc tác nguyên tố (Blaze Powder, Feather, Amethyst Shard, Prismarine Shard, Glowstone Dust, Obsidian/Ender Pearl)
 *   để triệu hồi Tinh Linh Thượng Vị đồng hành.
 * - Dân làng ở gần sẽ tự động tới cầu nguyện và nhận Chúc Phúc Tinh Linh (Tempest Guardian).
 */
public class SpiritShrineBlock extends Block {

    public SpiritShrineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§b[Tế Đàn Tinh Linh] §7Dùng vật phẩm nguyên tố: §6Bột Quỷ Lửa (Hỏa)§7, §aLông Vũ (Phong)§7, §eThạch Anh Tím (Thổ)§7, §bMảnh Đại Dương (Thủy)§7, §fBột Đá Sáng (Quang)§7, §5Hắc Diện Thạch / Ngọc Ender (Ám)§7."
            ), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        ElementalType type = resolveCatalyst(stack);
        if (type == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            List<ElementalSpiritEntity> existing = sl.getEntitiesOfClass(
                    ElementalSpiritEntity.class,
                    new AABB(pos).inflate(48.0D),
                    s -> sp.getUUID().equals(s.getOwnerUUID())
            );
            if (existing.size() >= 6) {
                sp.displayClientMessage(Component.literal("§e[Tế Đàn Tinh Linh] §7Bạn đã triệu hồi tối đa 6 Tinh Linh quanh khu vực này!"), true);
                return ItemInteractionResult.CONSUME;
            }

            if (!player.isCreative()) {
                stack.shrink(1);
            }

            ElementalSpiritEntity spirit = ModEntities.ELEMENTAL_SPIRIT.get().create(sl);
            if (spirit != null) {
                spirit.setElementalType(type);
                spirit.tame(sp);
                spirit.setPos(pos.getX() + 0.5D, pos.getY() + 1.35D, pos.getZ() + 0.5D);
                spirit.setHomeShrinePos(pos);
                sl.addFreshEntity(spirit);

                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 45, 0.7, 0.9, 0.7, 0.25);
                sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 25, 0.4, 0.6, 0.4, 0.08);
                sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.4F, 1.25F);

                VoiceOfTheWorld.announce(sp, "§b[Triệu Hồi Tinh Linh] " + type.colorCode + type.displayName + " §fđã giáng thế đáp lại lời khẩn cầu!");
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static ElementalType resolveCatalyst(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(Items.BLAZE_POWDER) || stack.is(Items.MAGMA_CREAM) || stack.is(Items.FIRE_CHARGE)) return ElementalType.FIRE;
        if (stack.is(Items.FEATHER) || stack.is(Items.WIND_CHARGE) || stack.is(Items.BREEZE_ROD)) return ElementalType.WIND;
        if (stack.is(Items.AMETHYST_SHARD) || stack.is(Items.EMERALD) || stack.is(Items.IRON_INGOT)) return ElementalType.EARTH;
        if (stack.is(Items.PRISMARINE_SHARD) || stack.is(Items.PRISMARINE_CRYSTALS) || stack.is(Items.HEART_OF_THE_SEA)) return ElementalType.WATER;
        if (stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.GOLD_INGOT) || stack.is(Items.NETHER_STAR)) return ElementalType.LIGHT;
        if (stack.is(Items.OBSIDIAN) || stack.is(Items.CRYING_OBSIDIAN) || stack.is(Items.ENDER_PEARL)) return ElementalType.DARK;
        return null;
    }
}
