package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class FrostDragonCoreItem extends Item {

    public FrostDragonCoreItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            Player player = context.getPlayer();

            summonVelzard(serverLevel, spawnPos, player);

            if (player != null && !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos spawnPos = player.blockPosition().relative(player.getDirection(), 4);
            summonVelzard(serverLevel, spawnPos, player);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void summonVelzard(ServerLevel serverLevel, BlockPos pos, Player player) {
        VelzardEntity velzard = new VelzardEntity(ModEntities.VELZARD.get(), serverLevel);
        velzard.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player != null ? player.getYRot() + 180.0F : 0.0F, 0.0F);

        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 4.0F, 0.6F);
        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 5.0F, 1.2F);
        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 4.0F, 1.4F);
        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.5F, 1.5F);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 3, 0.5D, 0.5D, 0.5D, 0);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 80, 1.8D, 2.0D, 1.8D, 0.1D);
        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 60, 1.5D, 1.8D, 1.5D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 40, 1.2D, 1.5D, 1.2D, 0.15D);

        serverLevel.addFreshEntity(velzard);

        velzard.broadcastDialogue("Băng giá tuyệt đối sẽ đóng băng linh hồn các ngươi... Hãy khuất phục trước Bạch Băng Long Velzard!");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b§l[VẬT PHẨM TRIỆU HỒI LONG CHỦNG]"));
        tooltip.add(Component.literal("§f§lLõi Bạch Băng Long (Frost Dragon Core)"));
        tooltip.add(Component.literal("§7Kết tinh ma tố hàn băng tuyệt đối từ Bạch Băng Long Velzard."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Nhấn Chuột Phải để triệu hồi:"));
        tooltip.add(Component.literal("§b- Bạch Băng Long Velzard (Nữ Hoàng Băng Giá Tối Thượng)"));
        tooltip.add(Component.literal("§7- Kèm theo trận §fBão Tuyết Cực Hàn §7kéo dài 30 giây"));
        tooltip.add(Component.literal("§7- Sở hữu §b10 Tuyệt Kỹ Băng Tuyết §7và Phòng Ngự Tuyệt Đối Gabriel"));
        tooltip.add(Component.literal("§7- Phá hủy hoàn toàn §3Đa Trùng Kết Giới §7khi tung đòn tối thượng"));
    }
}
