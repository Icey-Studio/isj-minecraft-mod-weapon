package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineSalvationHelper;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * KỸ NĂNG BAN DANH XƯNG (TENSURA NAMING ABILITY)
 * - Ban một danh xưng tiếng Anh ngẫu nhiên cho một sinh vật bất kỳ.
 * - Sức mạnh tăng vọt 30 LẦN cả MÁU và SỨC TẤN CÔNG (Max Health & Attack Damage x30).
 * - Tiêu hao tài nguyên người chơi theo cấp bậc sinh mệnh:
 *   + Sinh vật thường: Tiêu hao Thanh Đói (Hunger).
 *   + Boss / Ác ma: Tiêu hao Máu của người chơi (35% Max HP).
 *   + Ma Vương đã thức tỉnh: Tiêu hao 80% Máu của người chơi!
 * - Không bắt buộc thuần phục vĩnh viễn, mỗi sinh vật chỉ được ban tên 1 lần.
 */
public class TensuraNamingAbility {

    private static final Random RANDOM = new Random();

    private static final String[] FANTASY_NAMES = {
            "Ignis", "Zephyr", "Aurelius", "Kaelen", "Valkyrie", "Seraphina", "Obsidian", "Tempest",
            "Shadowfang", "Frostfall", "Solis", "Malakor", "Fenrir", "Sylph", "Zephyrus", "Astralis",
            "Vesper", "Nyx", "Orion", "Boreas", "Elysium", "Ragnar", "Cynthia", "Draken", "Lucian",
            "Valerius", "Solas", "Gryphon", "Chronos", "Ares", "Vane", "Titan", "Zenith", "Phoenix",
            "Ember", "Rune", "Blaze", "Stormbringer", "Nightshade", "Grimjaw", "Silverwind", "Ironfang",
            "Thunderstrike", "Voidcaller", "Dawnseeker", "Eclipse", "Duskblade", "Sunreaver", "Starlight", "Bloodmoon"
    };

    public static boolean cast(ServerLevel level, ServerPlayer player) {
        // Raycast quét tìm mục tiêu sinh vật trong phạm vi 12 blocks trước mắt
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        double maxDist = 12.0D;
        Vec3 reach = eyePos.add(look.scale(maxDist));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(2.0D);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && e != player);

        LivingEntity target = null;
        double closestDist = maxDist;

        for (LivingEntity e : list) {
            AABB bb = e.getBoundingBox().inflate(0.4D);
            var opt = bb.clip(eyePos, reach);
            if (opt.isPresent()) {
                double d = eyePos.distanceTo(opt.get());
                if (d < closestDist) {
                    closestDist = d;
                    target = e;
                }
            }
        }

        if (target == null) {
            player.displayClientMessage(
                    Component.literal("§c[BAN DANH XƯNG] Hãy nhìn thẳng vào sinh vật bạn muốn ban danh xưng (phạm vi 12 blocks)!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return false;
        }

        if (target instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity
                || target instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity
                || target instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity
                || target instanceof com.minhphuc.weapons.entity.tensura.MilimEntity) {
            player.displayClientMessage(
                    Component.literal("§c[BAN DANH XƯNG] Chân Long / Tối Cổ đã có chân danh tuyệt đối, không thể bị ban tên hay cưỡng ép tiến hóa!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return false;
        }

        // 1. Kiểm tra nếu sinh vật đã từng được ban tên
        if (target.getTags().contains("TensuraNamedMob")
                || (target instanceof PrimordialDemonEntity pde && pde.isNamed())
                || (target instanceof UnderworldDemonEntity ude && ude.isNamed())) {
            player.displayClientMessage(
                    Component.literal("§c[BAN DANH XƯNG] Sinh vật này đã được ban danh xưng từ trước, không thể ban tên lần thứ hai!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return false;
        }

        boolean isDemonLord = target.getTags().contains("TensuraDemonLordMob");
        boolean isBoss = DivineSalvationHelper.isBoss(target)
                || (target instanceof PrimordialDemonEntity)
                || (target instanceof UnderworldDemonEntity);

        // 2. Tính toán tiêu hao Ma Tố (Magicules) & Hỗ trợ từ Veldora trong người
        if (!MagiculesManager.canAttemptMagiculeSpend(player)) {
            player.displayClientMessage(
                    Component.literal("§c[GIỌNG NÓI THẾ GIỚI] Ma Tố bản thân đang ở mức 0! Vui lòng chờ Ma Tố hồi phục trước khi Ban Danh Xưng!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 0.8F);
            return false;
        }

        long maxMp = MagiculesManager.getMaxMagicules(player);
        boolean isBaby = (target instanceof AgeableMob ageable && ageable.isBaby());
        long mpCost;
        if (isDemonLord) {
            mpCost = Math.max(40_000L, Math.round(maxMp * 0.80D));
        } else if (isBoss) {
            mpCost = Math.max(20_000L, Math.round(maxMp * 0.35D));
        } else {
            mpCost = isBaby ? Math.max(5_000L, Math.round(maxMp * 0.05D)) : Math.max(10_000L, Math.round(maxMp * 0.10D));
        }

        boolean exhaustedDeath = MagiculesManager.consumeMagiculesWithExhaustion(player, mpCost, true, "Ban Danh Xưng");

        // 3. Ban Danh Xưng Ngẫu Nhiên
        String chosenName = FANTASY_NAMES[RANDOM.nextInt(FANTASY_NAMES.length)];
        String oldName = (target instanceof PrimordialDemonEntity pde && pde.getDemonType() != null)
                ? pde.getDemonType().getColorName()
                : target.getName().getString();

        target.addTag("TensuraNamedMob");

        if (target instanceof PrimordialDemonEntity demon) {
            demon.setNamed(true);
            demon.setCustomDemonName(chosenName);
            if (demon.getEvolutionTier() < 2) {
                demon.setEvolutionTier(demon.hasPhysicalBody() ? 3 : 2);
            }
            demon.updateAttributesForType(demon.getDemonType());
            demon.setHealth(demon.getMaxHealth());
            demon.updateDemonNametag();
            demon.syncToPactInOwnerInventory();
        } else if (target instanceof UnderworldDemonEntity demon) {
            demon.applyNameEvolution(chosenName, player);
        } else {
            target.setCustomName(Component.literal("§6§l✦ " + chosenName + " ✦ §e[" + oldName + "]"));
            target.setCustomNameVisible(true);

            // 4. Sức mạnh tăng vọt 30 LẦN CẢ MÁU VÀ SỨC TẤN CÔNG (cho mob thường)
            var hpAttr = target.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.setBaseValue(hpAttr.getBaseValue() * 30.0D);
                target.setHealth((float) hpAttr.getValue());
            }

            var atkAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atkAttr != null) {
                atkAttr.setBaseValue(Math.max(atkAttr.getBaseValue(), 4.0D) * 30.0D);
            }

            // Cung cấp khả năng tự vệ và chiến đấu cho các mob được ban tên (kể cả Dân Làng)
            if (target instanceof Mob mob) {
                com.minhphuc.weapons.mixin.MobAccessor mobAcc = (com.minhphuc.weapons.mixin.MobAccessor) mob;
                if (mob instanceof net.minecraft.world.entity.PathfinderMob pMob) {
                    mobAcc.weapons$getGoalSelector().addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(pMob, 1.35D, true));
                    mobAcc.weapons$getTargetSelector().addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(pMob));
                }
                if (mob instanceof net.minecraft.world.entity.npc.Villager) {
                    mob.addTag("DemonLordVillager");
                    mobAcc.weapons$getTargetSelector().addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob, net.minecraft.world.entity.monster.Zombie.class, true));
                }
            }
        }

        // 5. Hiệu ứng bùng nổ Ma Tố & Thánh Quang
        level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0D, target.getZ(), 3, 0.2, 0.2, 0.2, 0);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1.0D, target.getZ(), 60, 0.8D, 1.2D, 0.8D, 0.15D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0D, target.getZ(), 40, 0.6D, 1.0D, 0.6D, 0.08D);
        level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.2D, target.getZ(), 30, 0.5D, 0.8D, 0.5D, 0.05D);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.4F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.5F, 1.0F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.5F, 1.1F);

        // 6. Thông báo Giọng Nói Thế Giới
        VoiceOfTheWorld.announce(player, "Báo cáo: Hoàn tất nghi thức ban danh xưng. Cá thể " + oldName + " đã nhận tên gọi mới [✦ " + chosenName + " ✦]! Sinh mệnh và công kích tăng vọt 30 lần!");

        player.displayClientMessage(
                Component.literal("§6§l[BAN DANH XƯNG THÀNH CÔNG] §e" + oldName + " §fđã trở thành §6§l✦ " + chosenName + " ✦§f! (Máu & Sức mạnh x30 lần) ✨⚡"),
                false
        );

        if (exhaustedDeath) {
            MagiculesManager.triggerMagiculeExhaustionDeath(player, "Ban Danh Xưng");
        }

        return true;
    }
}
