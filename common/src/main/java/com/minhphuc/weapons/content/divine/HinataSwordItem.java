package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.tensura.MagiculesManager;
import com.minhphuc.weapons.content.tensura.MobMeleeSkillSystem;
import com.minhphuc.weapons.content.tensura.TensuraSkillHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Thanh Kiếm Thất Thải — Moonlight Rapier (Dead End Rainbow) của Hinata Sakaguchi.
 * Bao gồm các tính năng #6 đến #10 trong Bộ 10 Tính Năng Huyền Thoại (tất cả có hiệu ứng 3D).
 */
public class HinataSwordItem extends SwordItem {

    public static final String KEY_PARRY_UNTIL = "HinataSwordParryUntil";
    public static final String TAG_RAINBOW_STACKS = "HinataRainbowStacks";
    public static final String KEY_MINI_DISINT_NEXT = "HinataMiniDisintNextTick";

    public HinataSwordItem(Properties properties) {
        super(Tiers.NETHERITE, properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 24, -1.2F)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer sp && attacker.level() instanceof ServerLevel sl) {
            Vec3 dir = target.position().subtract(sp.position()).normalize();
            MobMeleeSkillSystem.sendCqc3d(sl, sp, target.position(), dir, 0, 0, 1.35F, 12);

            // #10 Cộng Hưởng Linh Tử: mặc kèm Áo Giáp Hinata hồi Ma Tố mỗi nhát chém
            if (HinataArmorItem.hasChestplate(sp)) {
                MagiculesManager.restoreMagicules(sp, 6_000L);
            }

            // #6 Khắc Ấn Thất Thải (1..7) -> #7 Đòn thứ 7 kích nổ Dead End Rainbow 3D!
            CompoundTag tData = EntityDataHelper.getCustomData(target);
            int stacks = tData.getInt(TAG_RAINBOW_STACKS) + 1;
            if (stacks >= 7) {
                tData.putInt(TAG_RAINBOW_STACKS, 0);
                triggerDeadEndRainbowFinisher(sl, sp, target, dir);
            } else {
                tData.putInt(TAG_RAINBOW_STACKS, stacks);
                sp.displayClientMessage(Component.literal("§d✦ Ấn Thất Thải: §f" + stacks + "§7/7"), true);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            Vec3 look = sp.getLookAngle().normalize();

            // #9 Shift + Chuột Phải: Phản Kiếm Thánh Quang (1.2s thủ phản 3D)
            if (sp.isShiftKeyDown()) {
                long until = sl.getGameTime() + 24L;
                EntityDataHelper.getCustomData(sp).putLong(KEY_PARRY_UNTIL, until);
                MobMeleeSkillSystem.sendCqc3d(sl, sp, sp.position(), look, 6, 1, 1.6F, 24);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 1.5F);
                sp.displayClientMessage(Component.literal("§6§l[PHẢN KIẾM THÁNH QUANG] §fSẵn sàng đỡ & phản đòn (1.2s)!"), true);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            // #10 Cộng Hưởng Linh Tử: Khi mặc Áo Giáp Hinata và nhìn lên/nhấn Chuột Phải mỗi 15s (hoặc mục tiêu ở xa) -> Mini Disintegration
            long now = sl.getGameTime();
            long nextDisint = EntityDataHelper.getCustomData(sp).getLong(KEY_MINI_DISINT_NEXT);
            if (HinataArmorItem.hasChestplate(sp) && look.y < -0.35D && now >= nextDisint) {
                EntityDataHelper.getCustomData(sp).putLong(KEY_MINI_DISINT_NEXT, now + 300L);
                castMiniDisintegration(sl, sp, sp.position().add(look.scale(6.0D)));
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            // #8 Chuột Phải: Liên Hoàn Tam Đoạn Thích (Lướt 6m đâm 3 nhát 3D)
            sp.setDeltaMovement(look.x * 1.45D, Math.max(0.15D, look.y * 0.45D), look.z * 1.45D);
            sp.hurtMarked = true;
            MobMeleeSkillSystem.sendCqc3d(sl, sp, sp.position(), look, 1, 0, 1.75F, 18);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.8F, 1.45F);

            AABB box = sp.getBoundingBox().expandTowards(look.scale(7.5D)).inflate(1.8D);
            for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != sp && TensuraSkillHelper.isValidSkillTarget(sp, e))) {
                target.invulnerableTime = 0;
                target.hurt(sp.damageSources().playerAttack(sp), 75.0F);
                target.push(look.x * 0.65D, 0.2D, look.z * 0.65D);

                CompoundTag tData = EntityDataHelper.getCustomData(target);
                int stacks = tData.getInt(TAG_RAINBOW_STACKS) + 2;
                if (stacks >= 7) {
                    tData.putInt(TAG_RAINBOW_STACKS, 0);
                    triggerDeadEndRainbowFinisher(sl, sp, target, look);
                } else {
                    tData.putInt(TAG_RAINBOW_STACKS, stacks);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void triggerDeadEndRainbowFinisher(ServerLevel sl, ServerPlayer sp, LivingEntity target, Vec3 dir) {
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DEAD_END_RAINBOW)) {
            ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DEAD_END_RAINBOW, sp.getUUID(), target.position(), dir, 26,
                    ClientboundCinematicSkillPacket.ACTION_START, 1.45F));
        }
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.0F, 1.5F);
        target.invulnerableTime = 0;
        target.addTag("DisintegrationDamage");
        try {
            target.hurt(sp.damageSources().indirectMagic(sp, sp), 320.0F);
        } finally {
            target.removeTag("DisintegrationDamage");
        }
        sp.displayClientMessage(Component.literal("§d§l[THẤT THẢI CHUNG YÊN — DEAD END RAINBOW] §fKích nổ 7 Ấn xuyên linh hồn!"), true);
    }

    private void castMiniDisintegration(ServerLevel sl, ServerPlayer sp, Vec3 center) {
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_DISINTEGRATION)) {
            ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_DISINTEGRATION, sp.getUUID(), center, new Vec3(0, 1, 0), 45,
                    ClientboundCinematicSkillPacket.ACTION_START, 0.65F));
        }
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.5F);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(6.0D), x -> x != sp && TensuraSkillHelper.isValidSkillTarget(sp, x))) {
            e.addTag("DisintegrationDamage");
            try {
                e.hurt(sp.damageSources().indirectMagic(sp, sp), 240.0F);
            } finally {
                e.removeTag("DisintegrationDamage");
            }
        }
        sp.displayClientMessage(Component.literal("§e§l[LINH TỬ BĂNG HOẠI] §fPhóng chùm Linh Tử 3D xuyên giáp!"), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l[VŨ KHÍ HUYỀN THOẠI • THÁNH KỴ SĨ]"));
        tooltip.add(Component.literal("§b§lMoonlight Rapier — Dead End Rainbow"));
        tooltip.add(Component.literal("§7• §dKhắc Ấn Thất Thải: §7Đánh thường tích §e7 Ấn§7."));
        tooltip.add(Component.literal("§7• §dThất Thải Chung Yên: §7Đòn thứ 7 kích nổ §f320 DMG xuyên linh hồn§7."));
        tooltip.add(Component.literal("§7• §b[Chuột Phải]: §7Liên Hoàn Tam Đoạn Thích lướt 6m (3D)."));
        tooltip.add(Component.literal("§7• §6[Shift + Chuột Phải]: §7Phản Kiếm Thánh Quang 1.2s (phản 120 DMG)."));
        tooltip.add(Component.literal("§7• §e[Cộng Hưởng Áo Giáp]: §7Hồi MP mỗi nhát chém + nhìn xuống [Chuột Phải] phóng Linh Tử Băng Hoại."));
    }
}
