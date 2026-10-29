package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoonlightSwordItem extends Item {
    public static final String NBT_SKILL = "ActiveSkill";

    public MoonlightSwordItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false; // Bất tử, không bao giờ mất độ bền
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Hào quang Nguyệt Quang Thần Thoại
    }

    public static String getSkillName(int skillOrdinal) {
        return switch (skillOrdinal) {
            case 0 -> "§4§l1. Hắc Thiểm Bá Vương Trảm (Black Flash)";
            case 1 -> "§e§l2. Linh Tử Băng Hoại (Rainbow Disintegration Orb)";
            default -> "§7Chưa chọn";
        };
    }

    public static void cycleSkill(ServerPlayer player, ItemStack stack) {
        int maxSkills = 2; // Chỉ giữ lại Hắc Thiểm Bá Vương Trảm & Linh Tử Băng Hoại

        int current = ItemStackDataHelper.getInt(stack, NBT_SKILL);
        int next = (current + 1) % maxSkills;
        ItemStackDataHelper.putInt(stack, NBT_SKILL, next);

        player.displayClientMessage(
            Component.literal("§6§l[NGUYỆT QUANG THẦN TẾ KIẾM] §fChế độ: " + getSkillName(next) + " §7(Chuột Phải để dùng)"),
            true
        );

        float pitch = 1.0F + (next * 0.3F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, pitch);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            int skill = ItemStackDataHelper.getInt(stack, NBT_SKILL);

            if (skill == 0) {
                // Chiêu 1: Hắc Thiểm Bá Vương Trảm (Black Flash Conqueror Slash)
                executeBlackFlashSlash(serverLevel, serverPlayer);
            } else {
                // Chiêu 2: Linh Tử Băng Hoại - Viên sáng 7 màu bay thẳng 50 block nổ địa hình & 30 tim
                shootRainbowDisintegrationOrb(serverLevel, serverPlayer);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Chiêu 1: Hắc Thiểm Bá Vương Trảm (Black Flash & ACoC)
     */
    public static void executeBlackFlashSlash(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        BlackFlashVFX.playBlackFlashSounds(level, player);
        BlackFlashVFX.spawnBlackFlashSlashVFX(level, player, eyePos, look, 14.0D);

        AABB slashBox = player.getBoundingBox().expandTowards(look.scale(14.0D)).inflate(3.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, slashBox, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            Vec3 toEntity = e.position().subtract(player.position()).normalize();
            if (toEntity.dot(look) > 0.45D) {
                e.invulnerableTime = 0;
                e.hurt(level.damageSources().playerAttack(player), 70.0F);
                Vec3 knockback = look.scale(1.5D).add(0, 0.3D, 0);
                e.setDeltaMovement(knockback);
            }
        }
    }

    /**
     * Chiêu 2: Linh Tử Băng Hoại - Viên sáng 7 màu bay thẳng 50 block, gây nổ địa hình và sát thương 30 tim (60 HP)
     */
    public static void shootRainbowDisintegrationOrb(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = 50.0D;
        double step = 0.5D;

        Vec3 hitPos = start.add(look.scale(maxDist));

        // 7 màu cầu vồng linh tử: Đỏ, Cam, Vàng, Lục, Lam, Chàm, Tím
        DustParticleOptions[] rainbow = new DustParticleOptions[]{
                new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.05F), 1.5F), // Đỏ
                new DustParticleOptions(new Vector3f(1.0F, 0.5F, 0.0F), 1.5F),   // Cam
                new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.1F), 1.5F),  // Vàng
                new DustParticleOptions(new Vector3f(0.1F, 1.0F, 0.2F), 1.5F),   // Lục
                new DustParticleOptions(new Vector3f(0.0F, 0.85F, 1.0F), 1.5F),  // Lam
                new DustParticleOptions(new Vector3f(0.1F, 0.25F, 1.0F), 1.5F),  // Chàm
                new DustParticleOptions(new Vector3f(0.85F, 0.1F, 1.0F), 1.5F)   // Tím
        };

        level.playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.5F, 1.8F);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.5F);

        for (double d = 1.0D; d <= maxDist; d += step) {
            Vec3 pos = start.add(look.scale(d));
            BlockPos bPos = BlockPos.containing(pos);

            // Sinh 7 hạt màu xoay quanh tâm đạn
            for (int c = 0; c < 7; c++) {
                double angle = (c * (Math.PI * 2.0D / 7.0D)) + (d * 0.4D);
                double r = 0.35D;
                level.sendParticles(rainbow[c], pos.x + Math.cos(angle) * r, pos.y + Math.sin(angle) * r, pos.z, 1, 0, 0, 0, 0);
            }
            if (d % 2.0D == 0) {
                level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.02);
            }

            // Kiểm tra va chạm khối cứng
            if (!level.getBlockState(bPos).isAir() && level.getBlockState(bPos).isSolid()) {
                hitPos = pos;
                break;
            }

            // Kiểm tra va chạm sinh vật
            AABB search = new AABB(pos.x - 0.7D, pos.y - 0.7D, pos.z - 0.7D, pos.x + 0.7D, pos.y + 0.7D, pos.z + 0.7D);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, search, e -> e != player && e.isAlive());
            if (!hits.isEmpty()) {
                hitPos = pos;
                break;
            }
        }

        // Kích nổ địa hình tại điểm va chạm
        level.explode(player, hitPos.x, hitPos.y, hitPos.z, 3.5F, Level.ExplosionInteraction.BLOCK);

        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.8F);
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5F, 1.4F);

        level.sendParticles(ParticleTypes.FLASH, hitPos.x, hitPos.y + 0.5D, hitPos.z, 3, 0.2, 0.2, 0.2, 0);
        for (DustParticleOptions dust : rainbow) {
            level.sendParticles(dust, hitPos.x, hitPos.y + 0.5D, hitPos.z, 15, 0.8, 0.8, 0.8, 0.2);
        }

        // Sát thương 30 tim = 60 HP cho mục tiêu trúng nổ
        AABB blastArea = new AABB(hitPos.x - 6.0D, hitPos.y - 4.0D, hitPos.z - 6.0D, hitPos.x + 6.0D, hitPos.y + 4.0D, hitPos.z + 6.0D);
        List<LivingEntity> blastTargets = level.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != player && e.isAlive());
        for (LivingEntity victim : blastTargets) {
            victim.invulnerableTime = 0;
            victim.hurt(level.damageSources().playerAttack(player), 60.0F); // 30 tim!
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int currentSkill = ItemStackDataHelper.getInt(stack, NBT_SKILL);

        tooltip.add(Component.literal("§6§l[VŨ KHÍ THẦN THOẠI]"));
        tooltip.add(Component.literal("§b§lNguyệt Quang Thần Tế Kiếm (Moonlight Ritual Sword)"));
        tooltip.add(Component.literal("§7Bảo kiếm hộ vệ thánh điện."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Đặc Tính Thần Thoại:"));
        tooltip.add(Component.literal("§7- §aĐộ bền bất tử, không bao giờ bị phá hủy"));
        tooltip.add(Component.literal("§7- §aTrảm kích phá giáp, đòn đánh uy lực tối thượng"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6⚡ Kỹ Năng Đang Chọn: " + getSkillName(currentSkill)));
        tooltip.add(Component.literal("§7- Nhấn phím §e[Z] §7để chuyển đổi:"));
        tooltip.add(Component.literal("§7   + §41. Hắc Thiểm Bá Vương Trảm: §fSét đen đỏ xé rách không gian"));
        tooltip.add(Component.literal("§7   + §e2. Linh Tử Băng Hoại: §fViên sáng 7 màu bay thẳng 50m, nổ địa hình, sát thương 30 tim"));
        tooltip.add(Component.literal("§7- Nhấn §a[Chuột Phải] §7để thi triển kỹ năng đã chọn (Không hồi chiêu)"));
    }
}
