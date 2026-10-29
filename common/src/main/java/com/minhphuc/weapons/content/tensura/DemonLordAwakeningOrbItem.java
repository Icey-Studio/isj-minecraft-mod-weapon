package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.data.EntityDataHelper;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DemonLordAwakeningOrbItem extends Item {

    public DemonLordAwakeningOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean isAlreadyDemonLord = EntityDataHelper.getCustomData(serverPlayer).getBoolean("TensuraTrueDemonLord");

            if (isAlreadyDemonLord) {
                serverPlayer.displayClientMessage(
                        Component.literal("§e§l[THÔNG BÁO] §fBáo cáo: Cá thể đã thức tỉnh thành Chân Ma Vương (True Demon Lord) từ trước!"),
                        true
                );
                return InteractionResultHolder.sidedSuccess(held, false);
            }

            // Thức tỉnh thành Chân Ma Vương ngay lập tức mà không cần qua các bước thu thập linh hồn hay giấc ngủ tiến hóa
            EntityDataHelper.getCustomData(serverPlayer).putBoolean("TensuraTrueDemonLord", true);
            EntityDataHelper.getCustomData(serverPlayer).putBoolean("TensuraAwakenedDemonLord", true);
            EntityDataHelper.getCustomData(serverPlayer).putInt("TensuraSoulsCollected", 20000);
            EntityDataHelper.getCustomData(serverPlayer).putBoolean("TensuraDemonLordEvolutionReady", false);

            // Mở khóa toàn bộ Kỹ Năng Tối Thượng ở cấp 3 (cấp tối đa)
            EvolvedSkillHelper.setEvolvedSkillTier(serverPlayer, EvolvedSkillHelper.SKILL_URIEL, 3);
            EvolvedSkillHelper.setEvolvedSkillTier(serverPlayer, EvolvedSkillHelper.SKILL_UNIVERSAL_SENSE, 3);
            EvolvedSkillHelper.setEvolvedSkillTier(serverPlayer, EvolvedSkillHelper.SKILL_INFINITE_PRISON, 3);

            // Tăng lượng máu tối đa vĩnh viễn lên 40 tim (80 HP)
            var attr = serverPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH); if (attr != null && attr.getBaseValue() < 80.0D) { attr.setBaseValue(80.0D); }
            serverPlayer.setHealth(serverPlayer.getMaxHealth());

            // Hiệu ứng và ban phước
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1, false, false, true));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1, false, false, true));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2, false, false, true));

            ServerLevel sl = (ServerLevel) level;
            sl.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.0F);
            sl.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 1.2F);
            sl.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 0.8F);

            sl.sendParticles(ParticleTypes.FLASH, serverPlayer.getX(), serverPlayer.getY() + 1.2, serverPlayer.getZ(), 5, 0.2, 0.2, 0.2, 0);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 80, 1.0, 1.0, 1.0, 0.1);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 60, 1.0, 1.5, 1.0, 0.1);

            VoiceOfTheWorld.announce(serverPlayer,
                    "Thông báo: Phát hiện Viên Ngọc Thức Tỉnh Ma Vương. Toàn bộ nghi thức và giới hạn ma lực đã được giải trừ. Cá thể tiến hóa thành công thành Chân Ma Vương (True Demon Lord)!");

            if (!serverPlayer.getAbilities().instabuild) {
                held.shrink(1);
            }

            return InteractionResultHolder.sidedSuccess(held, false);
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d§l[BẢO VẬT MA GIỚI VÔ GIÁ]"));
        tooltip.add(Component.literal("§fViên Ngọc Thức Tỉnh Ma Vương (Demon Lord Awakening Orb)"));
        tooltip.add(Component.literal("§7Kết tinh ma tố thuần khiết từ vực thẳm thời gian."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6⚡ Quyền Năng Tối Thượng:"));
        tooltip.add(Component.literal("§7- §a[Chuột Phải] §fđể lập tức thức tỉnh thành §d§lChân Ma Vương§f!"));
        tooltip.add(Component.literal("§7- §eBỏ qua toàn bộ yêu cầu thu thập linh hồn hay giấc ngủ tiến hóa."));
        tooltip.add(Component.literal("§7- §bLập tức lĩnh hội mọi Tuyệt Kỹ Tối Thượng ở cấp độ tối đa (Cấp 3)."));
        tooltip.add(Component.literal("§7- §cTỷ lệ 2% xuất hiện trong rương kho báu thế giới; không thể chế tạo!"));
    }
}
