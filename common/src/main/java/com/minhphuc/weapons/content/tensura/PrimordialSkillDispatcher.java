package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class PrimordialSkillDispatcher {

    public static void castSkill(ServerLevel level, ServerPlayer player, int skillIndex) {
        DemonType type = PrimordialPlayerDataHelper.getPrimordialType(player);
        if (type == null) return;

        // Skill 0 (Skill 1): Tà Khứ Vũ Thê Tử (Chung cho 7 Ác Ma)
        if (skillIndex == 0) {
            DeathStreakAbility.cast(level, player);
            return;
        }

        // Skill 5 (Skill 6): Ngũ Trọng Ma Trận Cột Sáng Thiên Khấu (Chung cho 7 Ác Ma)
        if (skillIndex == 5) {
            PentagramCelestialPillarAbility.cast(level, player);
            return;
        }

        boolean hasBody = PrimordialPlayerDataHelper.hasPhysicalBody(player);
        boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(player);

        switch (type) {
            case NOIR -> castNoir(level, player, skillIndex, hasBody, isDemonLord);
            case ROUGE -> castRouge(level, player, skillIndex, hasBody, isDemonLord);
            case BLANC -> castBlanc(level, player, skillIndex, hasBody, isDemonLord);
            case JAUNE -> castJaune(level, player, skillIndex, hasBody, isDemonLord);
            case VIOLET -> castViolet(level, player, skillIndex, hasBody, isDemonLord);
            case BLEU -> castBleu(level, player, skillIndex, hasBody, isDemonLord);
            case VERT -> castVert(level, player, skillIndex, hasBody, isDemonLord);
        }
    }

    private static void dealDamage(ServerPlayer player, LivingEntity victim, float baseDmg, boolean hasBody, boolean isDemonLord) {
        if (victim == null || !victim.isAlive() || victim == player) return;
        boolean isBoss = (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                || (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                || (victim instanceof net.minecraft.world.entity.monster.warden.Warden)
                || (victim instanceof net.minecraft.world.entity.animal.IronGolem)
                || (victim instanceof net.minecraft.world.entity.monster.ElderGuardian);

        if (hasBody || isDemonLord) {
            if (isBoss) {
                victim.hurt(player.damageSources().magic(), victim.getMaxHealth() * 0.52F);
            } else if (!(victim instanceof VelgryndEntity)) {
                victim.hurt(player.damageSources().magic(), victim.getMaxHealth() * 3.0F);
            }
        } else {
            if (isBoss) {
                victim.hurt(player.damageSources().magic(), baseDmg * 0.7F);
            } else {
                victim.hurt(player.damageSources().magic(), baseDmg);
            }
        }
    }

    // ==========================================
    // 1. NOIR (DIABLO) - HẮC SẮC
    // ==========================================
    private static void castNoir(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Thế Giới Cám Dỗ (Temptation World)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.0F, 0.6F);
            level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.0, pos.z, 80, 5.0, 1.0, 5.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(14.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
                dealDamage(player, e, 40.0F, hasBody, isDemonLord);
            }
            player.displayClientMessage(Component.literal("§8§l[NOIR] §5Thế Giới Cám Dỗ (Temptation World) đã giam cầm ý thức kẻ địch!"), true);
        } else if (skill == 2) { // Vũ Điệu Móng Vuốt (End of Despair)
            Vec3 look = player.getLookAngle();
            player.teleportTo(pos.x + look.x * 6.0, pos.y, pos.z + look.z * 6.0);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.5F, 0.5F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 10, 1.0, 1.0, 1.0, 0.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                dealDamage(player, e, 65.0F, hasBody, isDemonLord);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 2));
            }
        } else if (skill == 3) { // Nghịch Chuyển Sinh Tử (Reversal of Life & Death)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1.0, pos.z, 50, 0.8, 1.0, 0.8, 0.1);
            player.setHealth(player.getMaxHealth());
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 4));
            player.displayClientMessage(Component.literal("§8§l[NOIR] §dKhởi tạo Nghịch Chuyển Sinh Tử: Hồi phục hoàn toàn sinh mạng!"), true);
        } else if (skill == 4) { // Hắc Hạch Hư Vô Sụp Đổ (Black Hole)
            Vec3 target = pos.add(player.getLookAngle().scale(10.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.5F, 0.8F);
            level.sendParticles(ParticleTypes.PORTAL, target.x, target.y + 1.0, target.z, 150, 2.0, 2.0, 2.0, 0.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 12, target.y - 4, target.z - 12, target.x + 12, target.y + 10, target.z + 12), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                Vec3 pull = target.subtract(e.position()).normalize().scale(1.5);
                e.setDeltaMovement(pull);
                dealDamage(player, e, 90.0F, hasBody, isDemonLord);
            }
        }
    }

    // ==========================================
    // 2. ROUGE (GUY CRIMSON) - XÍCH SẮC
    // ==========================================
    private static void castRouge(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Hỏa Ngục Bộc Viêm (Prominence Flare)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 0.8F);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5, pos.z, 120, 4.0, 1.0, 4.0, 0.2);
            level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 40, 3.0, 1.0, 3.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setRemainingFireTicks(200);
                e.setDeltaMovement(0, 1.2, 0);
                dealDamage(player, e, 55.0F, hasBody, isDemonLord);
            }
        } else if (skill == 2) { // Xích Hồng Ma Trảm (Crimson Severance)
            Vec3 look = player.getLookAngle();
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.5F, 1.2F);
            for (double d = 2.0; d <= 25.0; d += 2.0) {
                Vec3 p = pos.add(look.scale(d));
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 1.2, p.z, 15, 0.5, 0.5, 0.5, 0.05);
                List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 2, p.y - 1, p.z - 2, p.x + 2, p.y + 3, p.z + 2), e -> e != player && e.isAlive());
                for (LivingEntity e : hit) {
                    dealDamage(player, e, 75.0F, hasBody, isDemonLord);
                    e.setRemainingFireTicks(140);
                }
            }
        } else if (skill == 3) { // Bức Tường Hỏa Ma (Infernal Bastion)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 1.0, pos.z, 60, 2.5, 1.5, 2.5, 0.05);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                Vec3 push = e.position().subtract(pos).normalize().scale(1.8);
                e.setDeltaMovement(push.x, 0.5, push.z);
                e.setRemainingFireTicks(100);
            }
        } else if (skill == 4) { // Lôi Hỏa Diệt Thế (Lucifer's Judgment)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.5F, 0.9F);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(18.0), e -> e != player && e.isAlive());
            int bolts = 0;
            for (LivingEntity e : targets) {
                if (bolts >= 8) break;
                level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + 1.0, e.getZ(), 2, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LAVA, e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                dealDamage(player, e, 110.0F, hasBody, isDemonLord);
                bolts++;
            }
        }
    }

    // ==========================================
    // 3. BLANC (TESTAROSSA) - BẠCH SẮC
    // ==========================================
    private static void castBlanc(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Bạch Viêm Tuyệt Diệt (White Flare)
            Vec3 target = pos.add(player.getLookAngle().scale(12.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.5F, 1.5F);
            level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 1.0, target.z, 100, 3.0, 3.0, 3.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 8, target.y - 3, target.z - 8, target.x + 8, target.y + 6, target.z + 8), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                dealDamage(player, e, 65.0F, hasBody, isDemonLord);
            }
        } else if (skill == 2) { // Mị Hoặc Tinh Thần (Mind Domination)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.5F, 1.2F);
            level.sendParticles(ParticleTypes.HEART, pos.x, pos.y + 1.2, pos.z, 30, 4.0, 1.0, 4.0, 0.05);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 2));
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
                dealDamage(player, e, 30.0F, hasBody, isDemonLord);
            }
        } else if (skill == 3) { // Lãnh Băng Hồ Điệp (Frost Butterflies)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 2.0F, 1.4F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z, 90, 5.0, 1.5, 5.0, 0.05);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setTicksFrozen(200);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5));
                dealDamage(player, e, 45.0F, hasBody, isDemonLord);
            }
        } else if (skill == 4) { // Trắng Xóa Hư Vô (Absolute Annihilation)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3.0F, 1.0F);
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.5, pos.z, 5, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z, 200, 8.0, 2.0, 8.0, 0.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                dealDamage(player, e, 120.0F, hasBody, isDemonLord);
            }
        }
    }

    // ==========================================
    // 4. JAUNE (CARRERA) - HOÀNG SẮC
    // ==========================================
    private static void castJaune(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Sụp Đổ Trọng Lực (Gravity Collapse)
            Vec3 target = pos.add(player.getLookAngle().scale(10.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.5F, 0.7F);
            level.sendParticles(ParticleTypes.CRIT, target.x, target.y + 1.0, target.z, 120, 5.0, 1.0, 5.0, 0.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 8, target.y - 3, target.z - 8, target.x + 8, target.y + 5, target.z + 8), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setDeltaMovement(0, -2.0, 0);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 6));
                dealDamage(player, e, 50.0F, hasBody, isDemonLord);
            }
        } else if (skill == 2) { // Tia Sáng Hoàng Kim (Golden Breaker)
            Vec3 look = player.getLookAngle();
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5F, 1.5F);
            for (double d = 2.0; d <= 30.0; d += 2.0) {
                Vec3 p = pos.add(look.scale(d));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1.2, p.z, 12, 0.4, 0.4, 0.4, 0.1);
                List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 2, p.y - 1, p.z - 2, p.x + 2, p.y + 3, p.z + 2), e -> e != player && e.isAlive());
                for (LivingEntity e : hit) {
                    dealDamage(player, e, 80.0F, hasBody, isDemonLord);
                }
            }
        } else if (skill == 3) { // Tập Trung Xạ Kích (Abaddon Focus)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.8F);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2));
            player.displayClientMessage(Component.literal("§6§l[JAUNE] §eTập Trung Xạ Kích: Tăng cường toàn bộ tốc độ và uy lực ma pháp!"), true);
        } else if (skill == 4) { // Pháo Hạt Nhân Khởi Nguyên (Nuclear Cannon)
            Vec3 target = pos.add(player.getLookAngle().scale(15.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.5F, 0.6F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y + 1.0, target.z, 8, 2.0, 2.0, 2.0, 0.0);
            level.sendParticles(ParticleTypes.LAVA, target.x, target.y + 1.0, target.z, 80, 4.0, 2.0, 4.0, 0.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 12, target.y - 4, target.z - 12, target.x + 12, target.y + 8, target.z + 12), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                dealDamage(player, e, 130.0F, hasBody, isDemonLord);
            }
        }
    }

    // ==========================================
    // 5. VIOLET (ULTIMA) - TỬ SẮC
    // ==========================================
    private static void castViolet(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Tử Độc Nở Rộ (Toxic Bloom)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 1.0, pos.z, 100, 5.0, 1.5, 5.0, 0.08);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 3));
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 3));
                dealDamage(player, e, 40.0F, hasBody, isDemonLord);
            }
        } else if (skill == 2) { // Hắc Tử Xuyên Tâm (Shadow Poison Ray)
            Vec3 look = player.getLookAngle();
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 2.0F, 1.6F);
            for (double d = 2.0; d <= 25.0; d += 2.0) {
                Vec3 p = pos.add(look.scale(d));
                level.sendParticles(ParticleTypes.WITCH, p.x, p.y + 1.2, p.z, 10, 0.4, 0.4, 0.4, 0.05);
                List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 2, p.y - 1, p.z - 2, p.x + 2, p.y + 3, p.z + 2), e -> e != player && e.isAlive());
                for (LivingEntity e : hit) {
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3));
                    dealDamage(player, e, 65.0F, hasBody, isDemonLord);
                }
            }
        } else if (skill == 3) { // Huyễn Ảnh Tốc Biến (Violet Phantom Dash)
            Vec3 look = player.getLookAngle();
            player.teleportTo(pos.x + look.x * 8.0, pos.y, pos.z + look.z * 8.0);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.2F);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 1.0, pos.z, 60, 1.5, 1.5, 1.5, 0.05);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.x - 4, pos.y - 2, pos.z - 4, pos.x + 4, pos.y + 3, pos.z + 4), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                dealDamage(player, e, 50.0F, hasBody, isDemonLord);
            }
        } else if (skill == 4) { // Mưa Ăn Mòn Tuyệt Tự (Corrosive Ruin)
            Vec3 target = pos.add(player.getLookAngle().scale(10.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 2.5F, 0.8F);
            level.sendParticles(ParticleTypes.WITCH, target.x, target.y + 4.0, target.z, 150, 6.0, 3.0, 6.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 10, target.y - 3, target.z - 10, target.x + 10, target.y + 6, target.z + 10), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                dealDamage(player, e, 105.0F, hasBody, isDemonLord);
            }
        }
    }

    // ==========================================
    // 6. BLEU (REIN) - LAM SẮC
    // ==========================================
    private static void castBleu(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Băng Tinh Vũ Bão (Absolute Zero Shards)
            Vec3 look = player.getLookAngle();
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.4F);
            for (double d = 2.0; d <= 20.0; d += 2.0) {
                Vec3 p = pos.add(look.scale(d));
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, p.x, p.y + 1.0, p.z, 15, 0.6, 0.6, 0.6, 0.1);
                List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 2, p.y - 1, p.z - 2, p.x + 2, p.y + 3, p.z + 2), e -> e != player && e.isAlive());
                for (LivingEntity e : hit) {
                    e.setTicksFrozen(160);
                    dealDamage(player, e, 50.0F, hasBody, isDemonLord);
                }
            }
        } else if (skill == 2) { // Hàn Băng Pháo Đài (Glacial Bastion)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z, 80, 3.0, 1.5, 3.0, 0.05);
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 10.0F));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                Vec3 push = e.position().subtract(pos).normalize().scale(1.5);
                e.setDeltaMovement(push.x, 0.4, push.z);
            }
        } else if (skill == 3) { // Lồng Giam Không Gian (Dimensional Canvas)
            Vec3 target = pos.add(player.getLookAngle().scale(8.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            level.sendParticles(ParticleTypes.SOUL, target.x, target.y + 1.0, target.z, 60, 3.0, 2.0, 3.0, 0.05);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 5, target.y - 2, target.z - 5, target.x + 5, target.y + 4, target.z + 5), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setDeltaMovement(0, 0, 0);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 6));
                dealDamage(player, e, 45.0F, hasBody, isDemonLord);
            }
        } else if (skill == 4) { // Bão Tuyết Vĩnh Cửu (Eternal Blizzard)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.6F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.5, pos.z, 200, 8.0, 3.0, 8.0, 0.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setTicksFrozen(300);
                dealDamage(player, e, 110.0F, hasBody, isDemonLord);
            }
        }
    }

    // ==========================================
    // 7. VERT (MISERY) - LỤC SẮC
    // ==========================================
    private static void castVert(ServerLevel level, ServerPlayer player, int skill, boolean hasBody, boolean isDemonLord) {
        Vec3 pos = player.position();
        if (skill == 1) { // Lục Phong Tiễu Sát (Emerald Tempest Blades)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.5F, 1.2F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1.0, pos.z, 80, 4.0, 1.0, 4.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setDeltaMovement(0, 0.8, 0);
                dealDamage(player, e, 55.0F, hasBody, isDemonLord);
            }
        } else if (skill == 2) { // Hấp Thụ Sinh Khí (Abyssal Vitality Drain)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.COMPOSTER, pos.x, pos.y + 1.0, pos.z, 50, 4.0, 1.0, 4.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0), e -> e != player && e.isAlive());
            float absorbed = 0;
            for (LivingEntity e : targets) {
                dealDamage(player, e, 35.0F, hasBody, isDemonLord);
                absorbed += 4.0F;
            }
            if (absorbed > 0) {
                player.setAbsorptionAmount(Math.min(40.0F, player.getAbsorptionAmount() + absorbed));
                player.displayClientMessage(Component.literal("§a§l[VERT] §2Hấp thụ sinh khí: Gia tăng " + (int) absorbed + " Máu Lá Chắn!"), true);
            }
        } else if (skill == 3) { // Kết Giới Lục Thần (Emerald Barrier Gale)
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 1.0, pos.z, 70, 3.0, 1.5, 3.0, 0.1);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                Vec3 push = e.position().subtract(pos).normalize().scale(2.0);
                e.setDeltaMovement(push.x, 0.6, push.z);
            }
        } else if (skill == 4) { // Cuồng Phong Tai Ương (Calamity Maelstrom)
            Vec3 target = pos.add(player.getLookAngle().scale(10.0));
            level.playSound(null, target.x, target.y, target.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.2F);
            level.sendParticles(ParticleTypes.EXPLOSION, target.x, target.y + 2.0, target.z, 15, 3.0, 4.0, 3.0, 0.1);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(target.x - 10, target.y - 3, target.z - 10, target.x + 10, target.y + 10, target.z + 10), e -> e != player && e.isAlive());
            for (LivingEntity e : targets) {
                e.setDeltaMovement(0, 1.8, 0);
                dealDamage(player, e, 115.0F, hasBody, isDemonLord);
            }
        }
    }
}
