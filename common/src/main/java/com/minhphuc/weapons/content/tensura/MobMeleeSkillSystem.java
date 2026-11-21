package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.entity.darkgathering.CursedIdolEntity;
import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity;
import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity;
import com.minhphuc.weapons.entity.darkgathering.VoNguyetMinhPhoiEntity;
import com.minhphuc.weapons.entity.tensura.CharybdisEntity;
import com.minhphuc.weapons.entity.tensura.ChronoaEntity;
import com.minhphuc.weapons.entity.tensura.ElementalSpiritEntity;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.OrcDisasterGeldEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hệ thống 10 Chiêu Cận Chiến / Phi Ma Pháp Riêng Cho Từng Mob & Boss (+1 Đòn Đánh Tay Gốc = 11)
 * Mọi chiêu đều phát gói tin render hiệu ứng 3D OpenGL (SKILL_CQC_TECHNIQUE).
 */
public final class MobMeleeSkillSystem {

    public static final String TAG_CQC_ACTIVE = "TensuraCqcSkillDamage";
    private static final Map<UUID, Long> NEXT_CQC_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_MOVE = new ConcurrentHashMap<>();

    private MobMeleeSkillSystem() {}

    /**
     * Gửi hiệu ứng 3D OpenGL cận chiến cho toàn bộ người chơi trong ServerLevel.
     */
    public static void sendCqc3d(ServerLevel sl, LivingEntity caster, Vec3 center, Vec3 dir,
                                 int styleId, int colorIdx, float scale, int durationTicks) {
        if (sl == null || caster == null || center == null) return;
        if (!SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_CQC_TECHNIQUE)) return;
        float clampedScale = Mth.clamp(scale, 0.9F, 9.5F);
        float packed = styleId * 100.0F + Mth.clamp(colorIdx, 0, 9) * 10.0F + clampedScale;
        ModMessages.sendToPlayers(sl.players(), new ClientboundCinematicSkillPacket(
                SkillVisualConfig.SKILL_CQC_TECHNIQUE,
                caster.getUUID(),
                center,
                (dir != null && dir.lengthSqr() > 1e-4) ? dir.normalize() : new Vec3(0, 0, 1),
                durationTicks,
                ClientboundCinematicSkillPacket.ACTION_START,
                packed
        ));
    }

    /**
     * Tick cận chiến chủ động cho toàn bộ các Mob/Boss thuộc mod quanh người chơi (mỗi 10 ticks).
     */
    public static void tickWorldMobs(ServerLevel sl) {
        if (sl == null || sl.players().isEmpty()) return;
        long now = sl.getGameTime();
        if (now % 10L != 0L) return;

        for (ServerPlayer player : sl.players()) {
            List<Mob> nearby = sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D),
                    m -> m.isAlive() && m.getTarget() != null && m.getTarget().isAlive() && isSupportedMob(m));
            for (Mob mob : nearby) {
                if (mob instanceof VeldoraEntity v && v.isSealed()) continue;
                Long next = NEXT_CQC_TICK.get(mob.getUUID());
                if (next != null && now < next) continue;

                LivingEntity target = mob.getTarget();
                if (target == null) continue;
                double maxRange = (mob instanceof CharybdisEntity) ? 24.0D : 14.0D;
                if (mob.distanceToSqr(target) <= maxRange * maxRange) {
                    executeSpeciesCqcSkill(sl, mob, target, now);
                }
            }
        }
        if (now % 400L == 0L) {
            NEXT_CQC_TICK.entrySet().removeIf(e -> e.getValue() + 600L < now);
        }
    }

    /**
     * Kích hoạt phản xạ cận chiến khi mob vừa chịu đòn (Sidestep / Counter) hoặc đánh cận chiến.
     */
    public static void onMobAttackedBy(Mob defender, LivingEntity attacker) {
        if (defender == null || attacker == null || !defender.isAlive() || !(defender.level() instanceof ServerLevel sl)) return;
        if (!isSupportedMob(defender) || !TensuraSkillHelper.isValidSkillTarget(defender, attacker)) return;
        if (defender instanceof VeldoraEntity v && v.isSealed()) return;

        long now = sl.getGameTime();
        Long next = NEXT_CQC_TICK.get(defender.getUUID());
        if (next != null && now < next) return;
        if (sl.random.nextFloat() < 0.28F) {
            executeSpeciesCqcSkill(sl, defender, attacker, now);
        }
    }

    private static boolean isSupportedMob(Mob mob) {
        return mob instanceof VeldoraEntity
                || mob instanceof VelzardEntity
                || mob instanceof VelgryndEntity
                || mob instanceof MilimEntity
                || mob instanceof CharybdisEntity
                || mob instanceof ChronoaEntity
                || mob instanceof PrimordialDemonEntity
                || mob instanceof OrcDisasterGeldEntity
                || mob instanceof DemonLordKuboEntity
                || mob instanceof KuboEntity
                || mob instanceof CuuTheQuyenHienEntity
                || mob instanceof TaKinhDaiTangEntity
                || mob instanceof MarbasEntity
                || mob instanceof CursedIdolEntity
                || mob instanceof VoNguyetMinhPhoiEntity
                || mob instanceof UnderworldDemonEntity
                || mob instanceof ElementalSpiritEntity;
    }

    private static void executeSpeciesCqcSkill(ServerLevel sl, Mob mob, LivingEntity target, long now) {
        int prev = LAST_MOVE.getOrDefault(mob.getUUID(), -1);
        int moveIdx = (prev + 1 + sl.random.nextInt(3)) % 10; // Luân phiên đủ 10 chiêu phi ma pháp
        LAST_MOVE.put(mob.getUUID(), moveIdx);

        boolean rage = mob.getHealth() < mob.getMaxHealth() * 0.45F;
        long cd = (mob instanceof VelgryndEntity || mob instanceof MilimEntity) ? (rage ? 26L : 38L) : (rage ? 34L : 48L);
        NEXT_CQC_TICK.put(mob.getUUID(), now + cd);

        Vec3 dir = target.position().add(0, 1.0, 0).subtract(mob.position().add(0, 1.0, 0));
        if (dir.lengthSqr() < 1e-4) dir = mob.getLookAngle();
        dir = dir.normalize();

        double baseAtk = mob.getAttribute(Attributes.ATTACK_DAMAGE) != null
                ? mob.getAttributeValue(Attributes.ATTACK_DAMAGE) : 45.0D;
        float dmg = (float) Math.max(18.0D, baseAtk);

        int colorIdx = resolveColorIndex(mob);
        float scale = resolveScale(mob);

        // Charybdis 4x sử dụng hình mẫu vĩ mô riêng
        if (mob instanceof CharybdisEntity) {
            runColossalCharybdisCqc(sl, mob, target, dir, moveIdx, dmg);
            return;
        }

        // 10 chiêu CQC riêng biệt được ánh xạ linh hoạt theo phong cách từng loài
        switch (moveIdx) {
            case 0 -> { // Đòn 1: Xung Kích Đâm / Quyền Thẳng (Sonic / Dragon Straight)
                mob.setDeltaMovement(dir.scale(1.35D).add(0, 0.15D, 0));
                mob.hurtMarked = true;
                sendCqc3d(sl, mob, mob.position(), dir, 4, colorIdx, scale, 16);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.6F, 0.9F);
                dealCqcStrike(sl, mob, target, dmg * 1.15F, dir.scale(1.1D), 0.25D, false);
            }
            case 1 -> { // Đòn 2: Móc Ngược / Thượng Trảm Hất Tung (Uppercut / Sky Slash)
                sendCqc3d(sl, mob, target.position(), dir, 7, colorIdx, scale, 18);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.6F, 1.1F);
                dealCqcStrike(sl, mob, target, dmg * 1.10F, new Vec3(dir.x * 0.4, 0.95D, dir.z * 0.4), 0.95D, false);
            }
            case 2 -> { // Đòn 3: Đòn Phá Khiên Nặng / Búa Tay (Guard Breaker)
                sendCqc3d(sl, mob, mob.position().add(dir.scale(1.2)), dir, 1, colorIdx, scale * 1.1F, 18);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.8F, 0.85F);
                if (target instanceof Player p) {
                    p.disableShield();
                }
                dealCqcStrike(sl, mob, target, dmg * 1.35F, dir.scale(0.95D), 0.2D, true);
            }
            case 3 -> { // Đòn 4: Quét Xoay 360 / Quật Đuôi (Spinning Tail / Weapon Sweep)
                double r = 4.8D + scale * 1.2D;
                sendCqc3d(sl, mob, mob.position(), dir, 5, colorIdx, scale * 1.25F, 20);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.9F, 0.8F);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(r), x -> TensuraSkillHelper.isValidSkillTarget(mob, x))) {
                    Vec3 away = e.position().subtract(mob.position()).normalize();
                    dealCqcStrike(sl, mob, e, dmg * 1.05F, away.scale(1.25D), 0.35D, false);
                }
            }
            case 4 -> { // Đòn 5: Dậm Đất / Nện Sàn Chấn Động (Ground Slam / Seismic Stomp)
                double r = 5.5D + scale;
                sendCqc3d(sl, mob, mob.position(), dir, 2, colorIdx, scale * 1.15F, 22);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.75F);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(r), x -> TensuraSkillHelper.isValidSkillTarget(mob, x))) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                    Vec3 away = e.position().subtract(mob.position()).normalize();
                    dealCqcStrike(sl, mob, e, dmg * 1.15F, away.scale(0.9D), 0.65D, false);
                }
            }
            case 5 -> { // Đòn 6: Xé Gió Chữ Thập / Liên Trảo / Dao Mổ (X-Rend)
                sendCqc3d(sl, mob, target.position(), dir, 3, colorIdx, scale * 1.15F, 16);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.8F, 1.2F);
                dealCqcStrike(sl, mob, target, dmg * 1.25F, dir.scale(0.45D), 0.15D, true);
            }
            case 6 -> { // Đòn 7: Né Lách Bên + Phản Công (Sidestep Counter)
                Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize().scale(sl.random.nextBoolean() ? 1.15D : -1.15D);
                sendCqc3d(sl, mob, mob.position(), dir, 9, colorIdx, scale, 18);
                mob.setDeltaMovement(side.add(0, 0.3D, 0));
                mob.hurtMarked = true;
                sendCqc3d(sl, mob, target.position(), dir.scale(-1), 0, colorIdx, scale, 14);
                dealCqcStrike(sl, mob, target, dmg * 1.15F, dir.scale(0.7D), 0.2D, false);
            }
            case 7 -> { // Đòn 8: Gồng Khí / Thủ Phản (Guard Dome & Counter Shock)
                sendCqc3d(sl, mob, mob.position(), dir, 6, colorIdx, scale * 1.15F, 22);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.7F, 0.9F);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(4.5D), x -> TensuraSkillHelper.isValidSkillTarget(mob, x))) {
                    Vec3 away = e.position().subtract(mob.position()).normalize();
                    dealCqcStrike(sl, mob, e, dmg * 0.95F, away.scale(1.4D), 0.45D, false);
                }
            }
            case 8 -> { // Đòn 9: Đuổi Bám Trên Không / Bổ Nhào (Aerial Dive Smash)
                if (target.getY() > mob.getY() + 1.5D) {
                    mob.setDeltaMovement(dir.x * 1.1D, 0.95D, dir.z * 1.1D);
                } else {
                    mob.setDeltaMovement(dir.x * 0.95D, -0.45D, dir.z * 0.95D);
                }
                mob.hurtMarked = true;
                sendCqc3d(sl, mob, target.position(), dir, 8, colorIdx, scale * 1.15F, 20);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F, 1.1F);
                dealCqcStrike(sl, mob, target, dmg * 1.30F, dir.scale(0.8D), -0.3D, false);
            }
            default -> { // Đòn 10: Liên Hoàn Cuồng Nộ Đa Nhịp (Frenzy Rush Flurry)
                sendCqc3d(sl, mob, mob.position().add(dir.scale(1.1)), dir, 0, colorIdx, scale * 1.2F, 18);
                sendCqc3d(sl, mob, target.position(), dir, 1, colorIdx, scale * 1.1F, 18);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.9F, 1.35F);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0));
                dealCqcStrike(sl, mob, target, dmg * 1.45F, dir.scale(0.7D), 0.25D, true);
            }
        }
    }

    private static void runColossalCharybdisCqc(ServerLevel sl, Mob mob, LivingEntity target, Vec3 dir, int moveIdx, float dmg) {
        float bigScale = 6.2F;
        double reach = 18.0D;
        switch (moveIdx % 5) {
            case 0 -> { // Ngoạm Hàm & Lắc Xé / Húc Đầu
                sendCqc3d(sl, mob, target.position(), dir, 3, 2, bigScale, 22);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.4F, 0.55F);
            }
            case 1 -> { // Quật Đuôi Đại Dương & Đập Đuôi Đôi
                sendCqc3d(sl, mob, mob.position(), dir, 5, 2, bigScale * 1.3F, 24);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.2F, 0.6F);
            }
            case 2 -> { // Nghiền Bụng Leviathan / Rơi Đè
                mob.setDeltaMovement(dir.scale(1.1D).add(0, -0.35D, 0));
                mob.hurtMarked = true;
                sendCqc3d(sl, mob, target.position(), dir, 2, 2, bigScale * 1.25F, 24);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case 3 -> { // Vỗ Vây Bão Gió Khổng Lồ
                sendCqc3d(sl, mob, mob.position(), dir, 10, 2, bigScale * 1.2F, 26);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 2.0F, 0.7F);
            }
            default -> { // Lăn Tròn Càn Quét / Hút Xoáy
                mob.setDeltaMovement(dir.scale(1.4D));
                mob.hurtMarked = true;
                sendCqc3d(sl, mob, mob.position(), dir, 4, 2, bigScale, 20);
                sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.2F, 0.5F);
            }
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(reach * 0.45D), x -> TensuraSkillHelper.isValidSkillTarget(mob, x))) {
            Vec3 away = e.position().subtract(mob.position()).normalize();
            dealCqcStrike(sl, mob, e, dmg * 1.20F, away.scale(1.35D), 0.55D, moveIdx >= 5);
        }
    }

    private static void dealCqcStrike(ServerLevel sl, Mob attacker, LivingEntity victim, float dmg, Vec3 knock, double yBoost, boolean armorPierce) {
        if (victim == null || !victim.isAlive()) return;
        victim.addTag(TAG_CQC_ACTIVE);
        try {
            victim.invulnerableTime = 0;
            if (armorPierce) {
                victim.hurt(attacker.damageSources().indirectMagic(attacker, attacker), dmg * 0.45F);
                victim.hurt(attacker.damageSources().mobAttack(attacker), dmg * 0.65F);
            } else {
                victim.hurt(attacker.damageSources().mobAttack(attacker), dmg);
            }
            victim.push(knock.x, yBoost, knock.z);
            victim.hurtMarked = true;
        } finally {
            victim.removeTag(TAG_CQC_ACTIVE);
        }
    }

    private static int resolveColorIndex(Mob mob) {
        if (mob instanceof VeldoraEntity || mob instanceof MilimEntity) return 1;
        if (mob instanceof CharybdisEntity) return 2;
        if (mob instanceof VelzardEntity) return 3;
        if (mob instanceof VelgryndEntity) return 4;
        if (mob instanceof ChronoaEntity) return 5;
        if (mob instanceof PrimordialDemonEntity || mob instanceof KuboEntity) return 6;
        if (mob instanceof MarbasEntity || mob instanceof ElementalSpiritEntity) return 7;
        if (mob instanceof OrcDisasterGeldEntity || mob instanceof TaKinhDaiTangEntity) return 8;
        if (mob instanceof DemonLordKuboEntity || mob instanceof VoNguyetMinhPhoiEntity || mob instanceof CursedIdolEntity) return 9;
        return 0;
    }

    private static float resolveScale(Mob mob) {
        if (mob instanceof VeldoraEntity vd) return vd.isHumanForm() ? 1.85F : 4.2F;
        if (mob instanceof VelzardEntity || mob instanceof VelgryndEntity) return 2.2F;
        if (mob instanceof OrcDisasterGeldEntity) return 2.0F;
        if (mob instanceof MilimEntity || mob instanceof DemonLordKuboEntity) return 1.8F;
        if (mob instanceof PrimordialDemonEntity || mob instanceof ChronoaEntity) return 1.55F;
        return 1.35F;
    }
}
