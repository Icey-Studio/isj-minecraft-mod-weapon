package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.client.gui.ClientPhoneOpener;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagisteelSmartphoneItem extends Item {

    public MagisteelSmartphoneItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
        if (player.level().isClientSide()) {
            ClientPhoneOpener.openScreen(interactionTarget);
        }
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (!context.getLevel().isClientSide() && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp && context.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
            AllOfCreationAbility.appraiseBlock(sl, sp, context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            LivingEntity target = findTargetEntity(player, 32.0D);
            if (target == null) {
                target = player;
            }
            ClientPhoneOpener.openScreen(target);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer sp && level instanceof net.minecraft.server.level.ServerLevel sl) {
            AllOfCreationAbility.appraiseTarget(sl, sp);
        }
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private LivingEntity findTargetEntity(Player player, double maxDist) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(viewVec.scale(maxDist));
        AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(maxDist)).inflate(1.5D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eyePos,
                reachVec,
                searchBox,
                e -> e instanceof LivingEntity && !e.isSpectator()
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.weapons.magisteel_phone.title"));
        tooltip.add(Component.translatable("tooltip.weapons.magisteel_phone.desc1"));
        tooltip.add(Component.translatable("tooltip.weapons.magisteel_phone.desc2"));
        tooltip.add(Component.translatable("tooltip.weapons.magisteel_phone.desc3"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.weapons.magisteel_phone.usage"));
    }
}
