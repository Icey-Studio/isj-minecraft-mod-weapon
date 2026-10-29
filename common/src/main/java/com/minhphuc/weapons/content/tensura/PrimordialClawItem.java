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
 * Vũ Khí Móng Vuốt Huyền Thoại (Legendary Primordial Claws)
 * - Vuốt Hắc Ám Tuyệt Vọng (Abyssal Claw of Despair)
 * - Vuốt Hư Vô Tai Ương (Void Claw of Calamity)
 * 
 * Tạo thành từ Năng Lực [Sáng Tạo Vật Chất] của Chân Ma Vương sau khi đánh bại Hắc Sắc Thủy Tổ Noir.
 * 3 Kỹ Năng Tối Thượng (Chuyển bằng phím Z):
 * - Skill 0: Tử Hắc Ma Trụ • Vực Thẳm Tuyệt Diệt (Abyssal Void Pillar).
 * - Skill 1: Hư Không Trảo Hồn • Ma Trận Không Gian (Void Claws Dimensional Rend).
 * - Skill 2: Hắc Hạch Bạo Diệt • Hư Vô Sụp Đổ (Cataclysmic Void Nova Explosion).
 */
public class PrimordialClawItem extends Item {

    public static final String NBT_SKILL = "ClawSelectedSkill";

    private final String clawTitleVi;
    private final String clawTitleEn;

    public PrimordialClawItem(Properties properties, String clawTitleVi, String clawTitleEn) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.clawTitleVi = clawTitleVi;
        this.clawTitleEn = clawTitleEn;
    }

    public static int getSkill(ItemStack stack) {
        return ItemStackDataHelper.getInt(stack, NBT_SKILL);
    }

    public static void setSkill(ItemStack stack, int skill) {
        ItemStackDataHelper.putInt(stack, NBT_SKILL, (skill % 3 + 3) % 3);
    }

    public static void cycleSkill(Player player, ItemStack stack) {
        int current = getSkill(stack);
        int next = (current + 1) % 3;
        setSkill(stack, next);

        String skillName = switch (next) {
            case 0 -> "§5§l[1] Tử Hắc Ma Trụ • Vực Thẳm Tuyệt Diệt";
            case 1 -> "§d§l[2] Hư Không Trảo Hồn • Ma Trận Không Gian";
            case 2 -> "§4§l[3] Hắc Hạch Bạo Diệt • Hư Vô Sụp Đổ (NỔ KHU VỰC)";
            default -> "§7Kỹ năng";
        };

        player.displayClientMessage(Component.literal("§5§l✦ VŨ KHÍ HUYỀN THOẠI ✦ " + skillName), true);
        player.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1.4F);
        player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
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

        switch (currentSkill) {
            case 0 -> {
                castSkill0Pillar(level, player);
                
            }
            case 1 -> {
                castSkill1DimensionalRend(level, player);
                
            }
            case 2 -> {
                castSkill2VoidNovaExplosion(level, player);
                
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // =========================================================================
    // SKILL 0: TỬ HẮC MA TRỤ (CỘT SÁNG MÀU TÍM ĐEN + VÒNG TRÒN MA THUẬT)
    // =========================================================================
    private void castSkill0Pillar(Level level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 targetPos = player.position().add(look.scale(8.0D));

        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 0.8F);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 3.0F, 0.6F);

        if (level instanceof ServerLevel sl) {
            // Vòng tròn ma thuật tím đen xoay tròn trên mặt đất
            for (int r = 1; r <= 3; r++) {
                for (int a = 0; a < 360; a += 15) {
                    double rad = Math.toRadians(a);
                    double px = targetPos.x + Math.cos(rad) * (r * 1.5D);
                    double pz = targetPos.z + Math.sin(rad) * (r * 1.5D);
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, px, targetPos.y + 0.1D, pz, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.WITCH, px, targetPos.y + 0.15D, pz, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, targetPos.y + 0.2D, pz, 1, 0, 0.05D, 0, 0.02D);
                }
            }

            // Cột sáng màu tím đen khổng lồ bốc lên trời cao 32 blocks
            for (double y = 0; y <= 32.0D; y += 0.8D) {
                for (int a = 0; a < 360; a += 60) {
                    double rad = Math.toRadians(a + y * 20.0D);
                    double px = targetPos.x + Math.cos(rad) * 1.8D;
                    double pz = targetPos.z + Math.sin(rad) * 1.8D;
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, px, targetPos.y + y, pz, 2, 0, 0.2D, 0, 0.05D);
                    sl.sendParticles(ParticleTypes.PORTAL, targetPos.x, targetPos.y + y, targetPos.z, 2, 0, 0.5D, 0, 0.1D);
                    sl.sendParticles(ParticleTypes.SQUID_INK, targetPos.x, targetPos.y + y, targetPos.z, 1, 0, 0.3D, 0, 0.05D);
                }
            }

            // Gây sát thương ma pháp + hút đối thủ vào cột sáng
            AABB pillarArea = new AABB(targetPos.x - 4.0D, targetPos.y - 2.0D, targetPos.z - 4.0D,
                    targetPos.x + 4.0D, targetPos.y + 32.0D, targetPos.z + 4.0D);
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, pillarArea,
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player));

            for (LivingEntity target : targets) {
                target.setDeltaMovement(0, 1.4D, 0); // Nâng bổng lên trời
                target.hurtMarked = true;
                target.hurt(sl.damageSources().mobAttack(player), 300.0F);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 160, 3));
            }
        }
    }

    // =========================================================================
    // SKILL 1: HƯ KHÔNG TRẢO HỒN MA TRẬN (ĐA TẦNG VÒNG TRÒN MA THUẬT + CHÉM XÉ KHÔNG GIAN)
    // =========================================================================
    private void castSkill1DimensionalRend(Level level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 2.5F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 0.6F);

        if (level instanceof ServerLevel sl) {
            // Vòng tròn ma thuật xoay trước mặt người chơi
            for (double d = 1.0D; d <= 4.0D; d += 1.5D) {
                Vec3 circleCenter = eyePos.add(look.scale(d));
                for (int a = 0; a < 360; a += 20) {
                    double rad = Math.toRadians(a);
                    double rx = Math.cos(rad) * (d * 0.8D);
                    double ry = Math.sin(rad) * (d * 0.8D);
                    sl.sendParticles(ParticleTypes.WITCH, circleCenter.x + rx, circleCenter.y + ry, circleCenter.z, 2, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, circleCenter.x + rx, circleCenter.y + ry, circleCenter.z, 1, 0, 0, 0, 0);
                }
            }

            // Quét các vệt chém móng vuốt tím đen xé toạc không gian
            for (double d = 2.0D; d <= 20.0D; d += 1.0D) {
                Vec3 p = eyePos.add(look.scale(d));
                double spread = d * 0.22D;
                for (int i = 0; i < 8; i++) {
                    double ox = (sl.random.nextDouble() - 0.5D) * spread;
                    double oy = (sl.random.nextDouble() - 0.5D) * spread;
                    double oz = (sl.random.nextDouble() - 0.5D) * spread;
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, p.x + ox, p.y + oy, p.z + oz, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, p.x + ox, p.y + oy, p.z + oz, 3, look.x * 0.5D, look.y * 0.5D, look.z * 0.5D, 0.05D);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x + ox, p.y + oy, p.z + oz, 1, 0, 0, 0, 0);
                }
            }

            AABB area = player.getBoundingBox().inflate(20.0D);
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player));

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(player.position());
                if (toTarget.lengthSqr() <= 20.0D * 20.0D) {
                    if (toTarget.normalize().dot(look) > 0.4D) {
                        target.hurt(sl.damageSources().mobAttack(player), 350.0F);
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 5));
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 3));
                        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(), 25, 0.3D, 0.3D, 0.3D, 0.1D);
                    }
                }
            }
        }
    }

    // =========================================================================
    // SKILL 2: HẮC HẠCH BẠO DIỆT (VÒNG TRÒN MA THUẬT + NỔ KHU VỰC CỰC MẠNH)
    // =========================================================================
    private void castSkill2VoidNovaExplosion(Level level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 targetCenter = player.position().add(look.scale(12.0D)).add(0, 1.0D, 0);

        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.5F, 0.7F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 1.2F);
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 2.0F, 0.9F);

        if (level instanceof ServerLevel sl) {
            // Vòng tròn ma thuật tím đen khổng lồ tụ lại tại tâm vụ nổ
            for (int r = 1; r <= 6; r++) {
                for (int a = 0; a < 360; a += 12) {
                    double rad = Math.toRadians(a);
                    double px = targetCenter.x + Math.cos(rad) * r;
                    double pz = targetCenter.z + Math.sin(rad) * r;
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, px, targetCenter.y + 0.1D, pz, 2, 0, 0.1D, 0, 0.02D);
                    sl.sendParticles(ParticleTypes.WITCH, px, targetCenter.y + 0.1D, pz, 1, 0, 0, 0, 0);
                }
            }

            // Sóng xung kích nổ khu vực (Cataclysmic Void Nova Blast)
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetCenter.x, targetCenter.y, targetCenter.z, 8, 2.0D, 1.0D, 2.0D, 0.0D);
            sl.sendParticles(ParticleTypes.FLASH, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, targetCenter.x, targetCenter.y, targetCenter.z, 4, 1.0D, 1.0D, 1.0D, 0.0D);

            for (int i = 0; i < 150; i++) {
                double vx = (sl.random.nextDouble() - 0.5D) * 1.5D;
                double vy = (sl.random.nextDouble() - 0.2D) * 1.2D;
                double vz = (sl.random.nextDouble() - 0.5D) * 1.5D;
                sl.sendParticles(ParticleTypes.DRAGON_BREATH, targetCenter.x, targetCenter.y, targetCenter.z, 1, vx, vy, vz, 0.2D);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, targetCenter.x, targetCenter.y, targetCenter.z, 1, vx * 0.8D, vy * 0.8D, vz * 0.8D, 0.15D);
            }

            // Gây sát thương nổ diện rộng 12 blocks và hất văng đối thủ
            AABB blastArea = new AABB(targetCenter.x - 12.0D, targetCenter.y - 6.0D, targetCenter.z - 12.0D,
                    targetCenter.x + 12.0D, targetCenter.y + 12.0D, targetCenter.z + 12.0D);
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, blastArea,
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player));

            for (LivingEntity target : targets) {
                Vec3 diff = target.position().subtract(targetCenter);
                double dist = diff.length();
                if (dist <= 12.0D) {
                    Vec3 push = dist > 0.01D ? diff.normalize() : new Vec3(0, 1, 0);
                    target.setDeltaMovement(push.x * 2.2D, 1.2D, push.z * 2.2D);
                    target.hurtMarked = true;
                    // Sát thương cực lớn
                    target.hurt(sl.damageSources().explosion(player, player), 500.0F);
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200, 4));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int current = getSkill(stack);
        tooltip.add(Component.literal("§6§l✦ CẤP ĐỘ: HUYỀN THOẠI (LEGENDARY) ✦"));
        tooltip.add(Component.literal("§e" + clawTitleVi + " §7(" + clawTitleEn + "§7)"));
        tooltip.add(Component.literal("§7Tuyệt kỹ móng vuốt hắc ám kết hợp ma đạo khí của Hắc Sắc Thủy Tổ."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§d§lKỹ năng hiện tại: " + switch (current) {
            case 0 -> "§5§l[1] Tử Hắc Ma Trụ (Cột Sáng Tím Đen)";
            case 1 -> "§d§l[2] Hư Không Trảo Hồn (Ma Trận Không Gian)";
            case 2 -> "§4§l[3] Hắc Hạch Bạo Diệt (Nổ Khu Vực Hư Vô)";
            default -> "§7Chưa chọn";
        }));
        tooltip.add(Component.literal("§7• §5Skill 1: §fTriệu hồi cột sáng tím đen khổng lồ nâng bổng và nghiền nát (300 DMG)."));
        tooltip.add(Component.literal("§7• §dSkill 2: §fĐa tầng ma trận tím đen xé toạc không gian hình chữ X (350 DMG)."));
        tooltip.add(Component.literal("§7• §4Skill 3: §fTụ hạch tâm nổ tung khu vực 12m cực mạnh (500 DMG)."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6👉 [Shift + Chuột Phải] hoặc nhấn [Z] để đổi Kỹ Năng!"));
    }
}

