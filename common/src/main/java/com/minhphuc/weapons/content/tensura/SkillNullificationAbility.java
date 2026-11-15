package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KỸ NĂNG: PHONG SÁT KĨ NĂNG (SKILL NULLIFICATION / ANTI-SKILL SEAL)
 * - Người chơi thi triển sẽ VÔ HIỆU HÓA TOÀN BỘ KỸ NĂNG & KẾT GIỚI dù đó là ai:
 *   Long Chủng (Veldora, Velgrynd, Velzard), Milim, Chronoa, Charybdis, Orc Disaster Geld,
 *   Ác Ma Thủy Tổ, Ác Ma Ma Giới, Tinh Linh, Boss khác và Người Chơi khác!
 * - Ngược lại, Long Chủng (Veldora, Velgrynd, Velzard) và Ác Ma (Thủy Tổ & Đại Ác Ma)
 *   cũng có thể kích hoạt Phong Sát Kỹ Năng ngược lại lên đối thủ!
 */
public class SkillNullificationAbility {

    private static final Map<UUID, Long> SEALED_ENTITIES = new ConcurrentHashMap<>();

    public static boolean isEntitySkillSealed(LivingEntity entity) {
        if (entity == null) return false;
        Long until = SEALED_ENTITIES.get(entity.getUUID());
        if (until == null) return false;
        if (entity.level().getGameTime() >= until) {
            SEALED_ENTITIES.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    public static long getRemainingSealSeconds(LivingEntity entity) {
        if (entity == null) return 0;
        Long until = SEALED_ENTITIES.get(entity.getUUID());
        if (until == null) return 0;
        long rem = until - entity.level().getGameTime();
        return rem > 0 ? Math.max(1L, (rem + 19L) / 20L) : 0L;
    }

    public static void sealEntitySkills(LivingEntity entity, int durationTicks) {
        if (entity == null || !entity.isAlive()) return;
        SEALED_ENTITIES.put(entity.getUUID(), entity.level().getGameTime() + durationTicks);
        entity.removeAllEffects();

        if (entity instanceof MilimEntity m) {
            m.cancelDragonNova();
            m.setCastingState(0);
        } else if (entity instanceof VelgryndEntity vg) {
            vg.setCastingState(0);
        } else if (entity instanceof VelzardEntity vz) {
            vz.setCastingState(0);
        } else if (entity instanceof VeldoraEntity vd) {
            vd.setCastingState(0);
        } else if (entity instanceof PrimordialDemonEntity pd) {
            pd.sealSkills(durationTicks);
        } else if (entity instanceof ServerPlayer sp) {
            if (RykuzinSinkaAbility.isActive(sp)) RykuzinSinkaAbility.deactivate(sp);
        }
    }

    public static boolean isPlayerSealed(ServerPlayer player) {
        return isEntitySkillSealed(player);
    }

    public static void sealPlayer(ServerPlayer player, int durationTicks) {
        sealEntitySkills(player, durationTicks);
    }

    public static void cast(ServerLevel level, ServerPlayer caster) {
        if (level == null || caster == null) return;

        // 0. Gửi gói tin hiệu ứng phong sát điện ảnh 3D
        if (SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_NULLIFICATION)) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_NULLIFICATION, caster.getUUID(), caster.position(), caster.getLookAngle(), 40, ClientboundCinematicSkillPacket.ACTION_START, 1.0f
            ));
        }

        // Hóa giải các thánh trận Linh Tử Băng Hoại và Thang Trời Jacob
        com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility.clearAllSanctuaries();
        com.minhphuc.weapons.content.divine.JacobsLadderAbility.clearAllLadders();

        // 1. Giải trừ tất cả kết giới (Barriers)
        AntiMagicBarrierManager.cleanAllBarriers(level);
        MultilayerBarrierAbility.cleanAllBarriers(level);
        InfiniteDragonPrisonAbility.cleanAllPrisons(level);

        // 2. Vô hiệu hóa Siêu Tân Tinh (Supernova) và Cột Sáng Vĩnh Cửu
        SupernovaAbility.dismissAll(level);

        // 3. Xóa sổ toàn bộ ma pháp trận tàn dư và ma trận hạt nhân
        ResidualMagicCircleManager.clearAllResidualCircles(level);
        DeathStreakAbility.destroyAllCircles(level);
        HorizontalHolyBeamAbility.clearAllBeams();
        PentagramCelestialPillarAbility.clearAllPillars();
        JaunePlayerSkillManager.ACTIVE_BARRAGES.clear();
        JaunePlayerSkillManager.ACTIVE_GRAVITY_HOLES.clear();

        // 4. Hủy các cấm thuật diện rộng đang thi triển
        NihilityCollapseAbility.cancelAll(level);
        AzathothAbility.cancelAll(level);
        HellflareAbility.cancelAll(level);
        DarkFlameShotAbility.cancelAll(level);

        // 5. Xóa sổ toàn bộ đạn ma pháp / cầu lửa / thuốc ném đang bay trong 96m
        AABB projBox = caster.getBoundingBox().inflate(96.0D);
        for (Projectile proj : level.getEntitiesOfClass(Projectile.class, projBox)) {
            if (proj.getOwner() != caster) {
                level.sendParticles(ParticleTypes.SMOKE, proj.getX(), proj.getY(), proj.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                proj.discard();
            }
        }

        // 6. PHONG SÁT TOÀN DIỆN MỌI THỰC THỂ (Dù là Long Chủng, Milim, Chronoa, Charybdis, Geld, Ác Ma, Tinh Linh hay Người Chơi!)
        AABB searchBox = caster.getBoundingBox().inflate(128.0D);
        int sealedCount = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (!target.isAlive() || target == caster) continue;
            if (target instanceof TamableAnimal ta && ta.isTame() && ta.isOwnedBy(caster)) continue;
            if (target instanceof VelgryndEntity vg && vg.isAllied() && caster.getUUID().equals(vg.getParsedOwnerUUID())) continue;

            sealEntitySkills(target, 600); // 30 giây vô hiệu toàn bộ skill bất kể là ai!
            sealedCount++;
            level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.2, target.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 22, 0.4, 0.7, 0.4, 0.05);

            if (target instanceof ServerPlayer enemy) {
                enemy.displayClientMessage(
                        Component.literal("§c§l[PHONG SÁT KĨ NĂNG] §eToàn bộ kỹ năng & ma pháp của bạn đã bị §f" + caster.getScoreboardName() + " §evô hiệu hóa hoàn toàn trong 30 giây!"),
                        true
                );
            }
        }

        // 7. Hiệu ứng âm thanh và sóng xung kích Phong Sát cực đại
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 4.0F, 0.6F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 4.0F, 0.5F);

        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10.0);
            double px = caster.getX() + Math.cos(angle) * 10.0;
            double pz = caster.getZ() + Math.sin(angle) * 10.0;
            level.sendParticles(ParticleTypes.SONIC_BOOM, px, caster.getY() + 1.0, pz, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FLASH, px, caster.getY() + 1.0, pz, 1, 0, 0, 0, 0);
        }

        caster.displayClientMessage(
                Component.literal("§c§l[PHONG SÁT KĨ NĂNG] §aĐã vô hiệu hóa TOÀN BỘ kỹ năng, kết giới & buff của " + sealedCount + " mục tiêu trong 30 giây!"),
                true
        );
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(caster) <= 128.0D * 128.0D) {
                p.displayClientMessage(
                        Component.literal("§5§l[PHONG SÁT] §eKết giới & mọi kỹ năng đã bị vô hiệu hóa hoàn toàn (" + sealedCount + " thực thể bị khóa chiêu 30s)!"),
                        false
                );
            }
        }
    }

    /**
     * Dành cho Long Chủng (Veldora, Velgrynd, Velzard) & Ác Ma (Thủy Tổ, Ma Giới Cấp Cao)
     * kích hoạt PHONG SÁT KỸ NĂNG lên người chơi / đối thủ.
     */
    public static void castBossSkillSeal(ServerLevel level, LivingEntity boss, float radius, int durationTicks) {
        if (level == null || boss == null || !boss.isAlive()) return;
        if (isEntitySkillSealed(boss)) return; // Chính boss cũng không thể niệm Phong Sát nếu đang bị Phong Sát!

        AntiMagicBarrierManager.cleanAllBarriers(level);
        MultilayerBarrierAbility.cleanAllBarriers(level);
        SupernovaAbility.dismissAll(level);

        AABB box = boss.getBoundingBox().inflate(radius);
        int count = 0;
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!hit.isAlive() || hit == boss) continue;
            if (boss instanceof UnderworldDemonEntity && hit instanceof UnderworldDemonEntity) continue;
            if (boss instanceof PrimordialDemonEntity && hit instanceof PrimordialDemonEntity) continue;
            if (boss instanceof TamableAnimal ta && ta.isTame() && ta.getOwner() == hit) continue;
            if (boss instanceof VelgryndEntity vg && vg.isAllied() && hit.getUUID().equals(vg.getParsedOwnerUUID())) continue;
            if (hit instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) continue;

            sealEntitySkills(hit, durationTicks);
            count++;
            level.sendParticles(ParticleTypes.SMOKE, hit.getX(), hit.getY() + 1.2, hit.getZ(), 18, 0.4, 0.7, 0.4, 0.05);
            if (hit instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("§4§l[PHONG SÁT KĨ NĂNG] §c" + boss.getName().getString() + " §eđã phong ấn toàn bộ kỹ năng của bạn trong §f" + (durationTicks / 20) + " giây!"), true);
                SageManager.sendCornerAlert(sp, "Cảnh Báo Đỏ! " + boss.getName().getString() + " đã kích hoạt [Phong Sát Kỹ Năng]! Mọi chiêu thức tạm bị khóa!", 0xFFEF4444, 5000, true);
            }
        }

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.65F);
        for (int i = 0; i < 24; i++) {
            double ang = Math.toRadians(i * 15.0);
            level.sendParticles(ParticleTypes.SONIC_BOOM, boss.getX() + Math.cos(ang) * 4.5, boss.getY() + 1.2, boss.getZ() + Math.sin(ang) * 4.5, 1, 0, 0, 0, 0);
        }
        if (count > 0) {
            for (ServerPlayer sp : level.players()) {
                if (sp.distanceToSqr(boss) <= 64.0D * 64.0D) {
                    sp.displayClientMessage(Component.literal("§c§l[" + boss.getName().getString() + "] §4\"[PHONG SÁT KỸ NĂNG] — Đừng hòng thi triển ma pháp trước mặt ta!\""), false);
                }
            }
        }
    }
}
