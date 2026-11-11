package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * KỸ NĂNG: PHONG SÁT KĨ NĂNG (SKILL NULLIFICATION / ANTI-SKILL SEAL)
 * - Vô hiệu hóa toàn diện mọi kết giới: Kháng Ma Kết Giới, Đa Trùng Kết Giới, Lồng Giam Vô Hạn, Lãnh Địa Ác Ma.
 * - Giải trừ hoàn toàn Cột Sáng Vĩnh Cửu của Siêu Tân Tinh (Supernova).
 * - Cắt đứt và buộc trạng thái ngưng tụ chiêu của Milim (Long Tinh Bộc Viêm Bá) và Long Chủng (Velgrynd, Velzard) phải làm lại từ đầu.
 * - Xóa sạch toàn bộ vòng tròn ma pháp trận, ma pháp ác ma và cấm thuật đang vận hành.
 * - TỐI ƯU ZERO-LAG: Quét dựa trên các danh sách đối tượng sẵn có trong bộ nhớ RAM, giải trừ tức thì trong 1 tick.
 */
public class SkillNullificationAbility {

    private static final java.util.Map<java.util.UUID, Long> SEALED_PLAYERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isPlayerSealed(ServerPlayer player) {
        if (player == null) return false;
        Long until = SEALED_PLAYERS.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static void sealPlayer(ServerPlayer player, int durationTicks) {
        if (player == null) return;
        SEALED_PLAYERS.put(player.getUUID(), player.level().getGameTime() + durationTicks);
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

        // 5. Cắt đứt và reset kỹ năng đang ngưng tụ của Milim, Long Chủng & Ác ma trong bán kính 128m
        AABB searchBox = caster.getBoundingBox().inflate(128.0D);

        // Milim: ngắt hoàn toàn Long Tinh Bộc Viêm Bá
        List<MilimEntity> milims = level.getEntitiesOfClass(MilimEntity.class, searchBox);
        for (MilimEntity m : milims) {
            if (m.isAlive()) {
                m.cancelDragonNova();
                m.setCastingState(0);
                level.sendParticles(ParticleTypes.SMOKE, m.getX(), m.getY() + 1.0, m.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
            }
        }

        // Velgrynd: ngắt Cardinal Acceleration và tuyệt kỹ
        List<VelgryndEntity> velgrynds = level.getEntitiesOfClass(VelgryndEntity.class, searchBox);
        for (VelgryndEntity vg : velgrynds) {
            if (vg.isAlive()) {
                vg.setCastingState(0);
                level.sendParticles(ParticleTypes.SMOKE, vg.getX(), vg.getY() + 1.0, vg.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
            }
        }

        // Velzard: ngắt Snow Crystal và Hơi Thở Rồng
        List<VelzardEntity> velzards = level.getEntitiesOfClass(VelzardEntity.class, searchBox);
        for (VelzardEntity vz : velzards) {
            if (vz.isAlive()) {
                vz.setCastingState(0);
                level.sendParticles(ParticleTypes.SMOKE, vz.getX(), vz.getY() + 1.0, vz.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
            }
        }

        // Ác ma Thủy Tổ: GIẢI TRỪ LÃNH ĐỊA & PHONG SÁT TOÀN BỘ KỸ NĂNG TRONG 30 GIÂY
        List<PrimordialDemonEntity> demons = level.getEntitiesOfClass(PrimordialDemonEntity.class, searchBox);
        int sealedDemonsCount = 0;
        for (PrimordialDemonEntity d : demons) {
            if (d.isAlive()) {
                d.sealSkills(600); // 30s (600 ticks) hoàn toàn không thể dùng skill ma pháp, chỉ có thể cận chiến
                sealedDemonsCount++;
                level.sendParticles(ParticleTypes.SONIC_BOOM, d.getX(), d.getY() + 1.2, d.getZ(), 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, d.getX(), d.getY() + 1.0, d.getZ(), 25, 0.4, 0.7, 0.4, 0.05);
            }
        }

        // Phong sát các người chơi đối địch mang sức mạnh ác ma trong bán kính 64m
        for (ServerPlayer enemy : level.getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(64.0D))) {
            if (enemy != caster) {
                sealPlayer(enemy, 600);
                enemy.displayClientMessage(
                        Component.literal("§c§l[PHONG SÁT KĨ NĂNG] §eToàn bộ kỹ năng & ma pháp của bạn đã bị phong ấn trong 30 giây!"),
                        true
                );
            }
        }

        // 6. Hiệu ứng âm thanh và sóng xung kích Phong Sát cực đại
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
                Component.literal("§c§l[PHONG SÁT KĨ NĂNG] §aĐã phá vỡ Kháng Ma Kết Giới, vô hiệu hóa mọi kết giới và phong ấn toàn bộ kỹ năng của Ác Ma trong 30 giây!"),
                true
        );
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(
                    Component.literal("§5§l[PHONG SÁT] §eToàn bộ kết giới đã bị phá vỡ! Kỹ năng của Ác Ma Thủy Tổ bị phong tỏa hoàn toàn (" + sealedDemonsCount + " ác ma bị khóa chiêu)!"),
                    false
            );
        }
    }
}
