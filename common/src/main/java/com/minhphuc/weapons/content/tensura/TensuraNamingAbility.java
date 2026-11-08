package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineSalvationHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
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

        // 1. Kiểm tra nếu sinh vật đã từng được ban tên
        if (target.getTags().contains("TensuraNamedMob")) {
            player.displayClientMessage(
                    Component.literal("§c[BAN DANH XƯNG] Sinh vật này đã được ban danh xưng từ trước, không thể ban tên lần thứ hai!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return false;
        }

        boolean isDemonLord = target.getTags().contains("TensuraDemonLordMob");
        boolean isBoss = DivineSalvationHelper.isBoss(target) || (target instanceof PrimordialDemonEntity);

        // 2. Tính toán tiêu hao tài nguyên & Kiểm tra điều kiện người dùng
        if (isDemonLord) {
            // Trường hợp A: Ma Vương Đã Thức Tỉnh -> Tiêu hao 80% MÁU TỐI ĐA
            float costHp = player.getMaxHealth() * 0.80F;
            if (player.getHealth() <= costHp) {
                player.displayClientMessage(
                        Component.literal("§c[GIỌNG NÓI THẾ GIỚI] Cảnh báo: Ban danh xưng cho Ma Vương yêu cầu tiêu hao 80% sinh mệnh! Lượng máu hiện tại không đủ!"),
                        true
                );
                VoiceOfTheWorld.announce(player, "Cảnh báo khẩn cấp: Sinh mệnh hiện tại không đủ 80% để chịu tải ma tố khi ban tên cho Ma Vương!");
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 0.7F);
                return false;
            }

            // Trừ 80% máu
            player.setHealth(Math.max(1.0F, player.getHealth() - costHp));
            player.hurtMarked = true;

            // Hiệu ứng mệt mỏi ma tố (Slumber weakness)
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 400, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));

        } else if (isBoss) {
            // Trường hợp B: Boss hoặc Ác Ma -> Tiêu hao 35% MÁU TỐI ĐA
            float costHp = player.getMaxHealth() * 0.35F;
            if (player.getHealth() <= costHp) {
                player.displayClientMessage(
                        Component.literal("§c[BAN DANH XƯNG] Sinh mệnh hiện tại không đủ 35% để ban danh xưng cho Ác Ma / Đại Boss!"),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 0.8F);
                return false;
            }

            player.setHealth(Math.max(1.0F, player.getHealth() - costHp));
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false, true));

        } else {
            // Trường hợp C: Sinh vật thường -> Tiêu hao Thanh Đói (Hunger)
            boolean isBaby = (target instanceof AgeableMob ageable && ageable.isBaby());
            int costHunger = isBaby ? 4 : 8; // 2 đùi gà nếu là con nhỏ, 4 đùi gà nếu là con lớn

            if (player.getFoodData().getFoodLevel() < costHunger) {
                player.displayClientMessage(
                        Component.literal("§c[BAN DANH XƯNG] Cơ thể quá đói (" + player.getFoodData().getFoodLevel() + "/" + costHunger + " điểm đói), không đủ thể lực để ban danh xưng!"),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 0.9F);
                return false;
            }

            player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - costHunger);
            player.getFoodData().setSaturation(0.0F);
        }

        // 3. Ban Danh Xưng Ngẫu Nhiên
        String chosenName = FANTASY_NAMES[RANDOM.nextInt(FANTASY_NAMES.length)];
        String oldName = target.getName().getString();

        target.addTag("TensuraNamedMob");
        target.setCustomName(Component.literal("§6§l✦ " + chosenName + " ✦ §e[" + oldName + "]"));
        target.setCustomNameVisible(true);

        // 4. Sức mạnh tăng vọt 30 LẦN CẢ MÁU VÀ SỨC TẤN CÔNG
        var hpAttr = target.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(hpAttr.getBaseValue() * 30.0D);
            target.setHealth((float) hpAttr.getValue());
        }

        var atkAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.setBaseValue(Math.max(atkAttr.getBaseValue(), 4.0D) * 30.0D);
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

        return true;
    }
}
