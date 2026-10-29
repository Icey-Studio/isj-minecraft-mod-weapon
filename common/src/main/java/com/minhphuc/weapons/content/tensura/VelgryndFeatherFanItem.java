package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Quạt Lông Vũ Long Chủng (Velgrynd Feather Fan)
 * Bảo vật tối thượng của Chước Nhiệt Long Velgrynd:
 * - Skill 1: Gió Cuồng Nộ Thổi Bay Đối Thủ (Tempest Blast).
 * - Skill 2: Gió Chước Nhiệt Tiêu Diệt Đối Thủ (Scorching Annihilation Tornado).
 * - Bấm [Shift + Chuột Phải] hoặc [Z] để chuyển đổi skill, [Chuột Phải] để thi triển.
 */
public class VelgryndFeatherFanItem extends Item {

    public static final String NBT_SKILL = "FanSkill";

    public VelgryndFeatherFanItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static int getSkill(ItemStack stack) {
        return ItemStackDataHelper.getInt(stack, NBT_SKILL);
    }

    public static void setSkill(ItemStack stack, int skill) {
        ItemStackDataHelper.putInt(stack, NBT_SKILL, skill % 2);
    }

    public static void cycleSkill(Player player, ItemStack stack) {
        int current = getSkill(stack);
        int next = (current + 1) % 2;
        setSkill(stack, next);

        String name = (next == 0)
                ? "§b§l[SKILL 1] Gió Cuồng Nộ - Thổi Bay Đối Thủ"
                : "§c§l[SKILL 2] Chước Nhiệt Bão Tố - Tiêu Diệt Đối Thủ";
        player.displayClientMessage(Component.literal("§6§l✦ QUẠT LONG CHỦNG ✦ " + name), true);
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.5F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleSkill(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        int currentSkill = getSkill(stack);

        if (currentSkill == 0) {
            // SKILL 1: GIÓ THỔI BAY ĐỐI THỦ
            castBlowAwaySkill(level, player);
            
        } else {
            // SKILL 2: GIÓ TIÊU DIỆT ĐỐI THỦ
            castAnnihilationWindSkill(level, player);
            
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void castBlowAwaySkill(Level level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.5F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8F, 1.5F);

        if (level instanceof ServerLevel sl) {
            // Tạo luồng sóng gió cuồng nộ hình nón
            for (double d = 1.0D; d <= 16.0D; d += 0.8D) {
                Vec3 p = eyePos.add(look.scale(d));
                double spread = d * 0.25D;
                for (int i = 0; i < 8; i++) {
                    double ox = (sl.random.nextDouble() - 0.5D) * spread;
                    double oy = (sl.random.nextDouble() - 0.5D) * spread;
                    double oz = (sl.random.nextDouble() - 0.5D) * spread;
                    sl.sendParticles(ParticleTypes.CLOUD, p.x + ox, p.y + oy, p.z + oz, 2, look.x * 0.8D, look.y * 0.8D, look.z * 0.8D, 0.15D);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x + ox, p.y + oy, p.z + oz, 1, 0, 0, 0, 0);
                }
            }

            // Tìm và thổi bay tất cả kẻ địch phía trước
            AABB area = player.getBoundingBox().inflate(16.0D);
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !e.isAlliedTo(player));

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(player.position());
                if (toTarget.lengthSqr() <= 16.0D * 16.0D) {
                    Vec3 dir = toTarget.normalize();
                    double dot = dir.dot(look);
                    if (dot > 0.35D) { // Nằm trong góc nhìn quạt
                        // Hất văng cực mạnh
                        target.setDeltaMovement(look.x * 2.8D, 1.2D, look.z * 2.8D);
                        target.hurtMarked = true;
                        target.hurt(sl.damageSources().mobAttack(player), 25.0F);
                        sl.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 1.0D, target.getZ(), 15, 0.4D, 0.4D, 0.4D, 0.1D);
                    }
                }
            }
        }
    }

    private void castAnnihilationWindSkill(Level level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.5F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.2F);

        if (level instanceof ServerLevel sl) {
            // Lốc xoáy chém rách không gian và thiêu đốt
            for (double d = 1.0D; d <= 14.0D; d += 0.6D) {
                Vec3 p = eyePos.add(look.scale(d));
                double spread = d * 0.28D;
                for (int i = 0; i < 10; i++) {
                    double ox = (sl.random.nextDouble() - 0.5D) * spread;
                    double oy = (sl.random.nextDouble() - 0.5D) * spread;
                    double oz = (sl.random.nextDouble() - 0.5D) * spread;
                    sl.sendParticles(ParticleTypes.FLAME, p.x + ox, p.y + oy, p.z + oz, 3, 0.1D, 0.1D, 0.1D, 0.05D);
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, p.x + ox, p.y + oy, p.z + oz, 2, 0.08D, 0.08D, 0.08D, 0.03D);
                    sl.sendParticles(ParticleTypes.LAVA, p.x + ox, p.y + oy, p.z + oz, 1, 0, 0, 0, 0);
                }
            }

            AABB area = player.getBoundingBox().inflate(14.0D);
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !e.isAlliedTo(player));

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(player.position());
                if (toTarget.lengthSqr() <= 14.0D * 14.0D) {
                    Vec3 dir = toTarget.normalize();
                    if (dir.dot(look) > 0.3D) {
                        target.setRemainingFireTicks(200); // Đốt cháy 10 giây
                        target.hurt(sl.damageSources().mobAttack(player), 200.0F); // Sát thương tiêu diệt cực lớn
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 1.0D, target.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int current = getSkill(stack);
        tooltip.add(Component.literal("§c§l[THẦN KHÍ LONG CHỦNG - CHƯỚC NHIỆT LONG VELGRYND]"));
        tooltip.add(Component.literal("§7Bảo vật quạt lông vũ điều khiển phong bạo & hỏa diễm long chủng."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e§lChế độ hiện tại: " + (current == 0
                ? "§b§l[1] Gió Cuồng Nộ (Thổi Bay Đối Thủ)"
                : "§c§l[2] Chước Nhiệt Bão Tố (Tiêu Diệt Đối Thủ)")));
        tooltip.add(Component.literal("§7• §bSkill 1: §fThổi bay toàn bộ mục tiêu trong 16m hất tung lên trời."));
        tooltip.add(Component.literal("§7• §cSkill 2: §fLốc xoáy hỏa phong tiêu diệt đối thủ (200 Sát Thương)."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6👉 [Shift + Chuột Phải] hoặc nhấn [Z] để đổi Skill!"));
    }
}

