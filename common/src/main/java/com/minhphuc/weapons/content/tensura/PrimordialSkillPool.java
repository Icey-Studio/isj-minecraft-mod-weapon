package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class PrimordialSkillPool {

    public static final int SKILL_BEELZEBUTH = 0;
    public static final int SKILL_INFINITY_SNAP = 1;
    public static final int SKILL_POWER_BEAM = 2;
    public static final int SKILL_SPACE_TELEPORT = 3;
    public static final int SKILL_REALITY_ILLUSION = 4;
    public static final int SKILL_TIME_FREEZE = 5;
    public static final int SKILL_SOUL_DRAIN = 6;
    public static final int SKILL_MIND_BLAST = 7;
    public static final int SKILL_TAISUI_STARS = 8;
    public static final int SKILL_ALKAID_CHAOS = 9;
    public static final int SKILL_JACOB_LADDER = 10;
    public static final int SKILL_DISINTEGRATION = 11;
    public static final int SKILL_HEAVENLY_JUDGMENT = 12;
    public static final int SKILL_BLACK_FLASH = 13;
    public static final int SKILL_DRAGON_NOVA = 14;
    public static final int SKILL_SEER_FLESH_PULSE = 15;

    public static final int TOTAL_SKILLS = 16;

    public record SkillEntry(int id, String key, String displayNameVi, String displayNameEn, String descVi, String category, String color, int iconColorHex) {}

    private static final List<SkillEntry> ALL_SKILLS = new ArrayList<>();

    static {
        ALL_SKILLS.add(new SkillEntry(SKILL_BEELZEBUTH, "beelzebuth", "Bạo Thực Chi Vương: Beelzebuth", "Gluttonous King Beelzebuth", "Thôn phệ sinh lực mục tiêu và hồi máu cho ác ma", "Ma Vương", "§5", 0x8B008B));
        ALL_SKILLS.add(new SkillEntry(SKILL_INFINITY_SNAP, "snap", "Cú Búng Tay Vô Cực (Snap)", "Infinity Snap", "Kích nổ nguyên tử hư không xóa sổ kẻ thù", "Găng Tay", "§6", 0xFFD700));
        ALL_SKILLS.add(new SkillEntry(SKILL_POWER_BEAM, "power_beam", "Tia Phân Rã Sức Mạnh (Power Beam)", "Power Beam", "Phóng chùm năng lượng tím phá hủy sinh mệnh", "Đá Vô Cực", "§d", 0xDA70D6));
        ALL_SKILLS.add(new SkillEntry(SKILL_SPACE_TELEPORT, "space_warp", "Xuyên Thấu Không Gian (Space Warp)", "Space Warp", "Dịch chuyển tức thời ra sau lưng mục tiêu và kích sát", "Đá Vô Cực", "§9", 0x4169E1));
        ALL_SKILLS.add(new SkillEntry(SKILL_REALITY_ILLUSION, "reality_clones", "Ảo Ảnh Thực Tại (Reality Clones)", "Reality Clones", "Tàng hình và tăng vọt tốc độ chiến đấu", "Đá Vô Cực", "§c", 0xDC143C));
        ALL_SKILLS.add(new SkillEntry(SKILL_TIME_FREEZE, "time_freeze", "Ngưng Đọng Thời Gian (Time Freeze)", "Time Freeze", "Đóng băng chuyển động của mục tiêu trong bán kính 6m", "Đá Vô Cực", "§a", 0x32CD32));
        ALL_SKILLS.add(new SkillEntry(SKILL_SOUL_DRAIN, "soul_drain", "Hấp Huyết Linh Hồn (Soul Drain)", "Soul Drain", "Rút cạn linh hồn kẻ địch để cường hóa sinh lực", "Đá Vô Cực", "§6", 0xFF8C00));
        ALL_SKILLS.add(new SkillEntry(SKILL_MIND_BLAST, "mind_blast", "Sóng Xung Kích Tâm Trí (Mind Blast)", "Mind Blast", "Tấn công sóng âm gây mù và choáng váng diện rộng", "Đá Vô Cực", "§e", 0xFFD700));
        ALL_SKILLS.add(new SkillEntry(SKILL_TAISUI_STARS, "taisui_stars", "Tuyệt Diệt Tinh Tú (Extinction Stars)", "Extinction Stars", "Phóng chùm sao Thái Tuế bắn phá hủy diệt", "Thái Tuế", "§b", 0x00FFFF));
        ALL_SKILLS.add(new SkillEntry(SKILL_ALKAID_CHAOS, "spatial_teleport", "Dịch Chuyển Không Gian: Pháp Trận Vượt Chiều", "Spatial Teleportation", "Mở pháp trận không gian 3D dịch chuyển tức thời đến ấn ký đã đánh dấu", "Không Gian", "§b", 0x00E5FF));
        ALL_SKILLS.add(new SkillEntry(SKILL_JACOB_LADDER, "jacob_ladder", "Nấc Thang Jacob (Tà Khứ Vũ Thê Tử)", "Jacob's Ladder", "Quang trụ 60m bộc phá năng lượng thần thánh", "Nguyệt Quang", "§b", 0x00E5FF));
        ALL_SKILLS.add(new SkillEntry(SKILL_DISINTEGRATION, "disintegration", "Linh Tử Băng Hoại (Disintegration)", "Disintegration", "Tam tầng pháp trận diệt nguyên tử tuyệt đối", "Thánh Ma", "§e", 0xFFEA00));
        ALL_SKILLS.add(new SkillEntry(SKILL_HEAVENLY_JUDGMENT, "heavenly_judgment", "Bát Môn Thiên Phạt Trận", "Heavenly Judgment", "7 Cột trụ giam hãm và phán quyết kẻ địch", "Thánh Điển", "§6", 0xFFA500));
        ALL_SKILLS.add(new SkillEntry(SKILL_BLACK_FLASH, "black_flash", "Trảm Kích Hắc Thiểm Bá Vương", "Black Flash Conqueror", "Trảm kích sét đen nổ tung linh hồn", "Vũ Khí", "§4", 0x222222));
        ALL_SKILLS.add(new SkillEntry(SKILL_DRAGON_NOVA, "dragon_nova", "Long Tinh Bộc Viêm Bá (Dragon Nova)", "Dragon Nova", "Cấm thuật diệt rồng cổ đại san phẳng bình địa", "Cấm Thuật", "§d", 0xFF00FF));
        ALL_SKILLS.add(new SkillEntry(SKILL_SEER_FLESH_PULSE, "seer_flesh_pulse", "Thị Nhục Mạch Động (Seer Pulse)", "Seer Flesh Pulse", "Hồi phục toàn diện và phóng xung lực hất tung kẻ địch", "Thái Tuế", "§2", 0x2E8B57));
    }

    public static List<SkillEntry> getAllSkills() {
        return ALL_SKILLS;
    }

    public static SkillEntry getSkillById(int id) {
        if (id >= 0 && id < ALL_SKILLS.size()) {
            return ALL_SKILLS.get(id);
        }
        return ALL_SKILLS.get(0);
    }

    public static SkillEntry getSkillByKey(String key) {
        for (SkillEntry s : ALL_SKILLS) {
            if (s.key().equalsIgnoreCase(key)) return s;
        }
        return ALL_SKILLS.get(0);
    }

    public static SkillEntry getSkillById(String str) {
        try {
            int id = Integer.parseInt(str);
            return getSkillById(id);
        } catch (NumberFormatException e) {
            return getSkillByKey(str);
        }
    }

    public static List<SkillEntry> rollRandomSkills(int count, RandomSource random) {
        List<SkillEntry> copy = new ArrayList<>(ALL_SKILLS);
        Collections.shuffle(copy);
        int finalCount = Math.min(count, copy.size());
        return copy.subList(0, finalCount);
    }

    public static String getSkillName(int id) {
        return getSkillById(id).displayNameVi();
    }

    public static String getSkillDescription(int id) {
        return getSkillById(id).descVi();
    }

    public static int getSkillColor(int id) {
        return getSkillById(id).iconColorHex();
    }

    public static SkillEntry castRandomTrueDragonSkill(
            ServerLevel level,
            Mob dragon,
            @Nullable LivingEntity target,
            double basePower,
            float dragonMultiplier,
            @Nullable BiConsumer<LivingEntity, Float> damageFn
    ) {
        if (level == null || dragon == null || !dragon.isAlive()) return ALL_SKILLS.get(0);
        SkillEntry chosen = ALL_SKILLS.get(dragon.getRandom().nextInt(ALL_SKILLS.size()));
        executeSkillInternal(chosen.id(), level, dragon, target, basePower, dragonMultiplier, true, damageFn);
        return chosen;
    }

    public static void executeSkill(PrimordialDemonEntity demon, LivingEntity target, SkillEntry skill) {
        if (demon == null || !demon.isAlive() || skill == null) return;
        if (demon.level() instanceof ServerLevel serverLevel) {
            executeSkill(skill.id(), serverLevel, demon, target);
        }
    }

    public static void executeSkill(int id, ServerLevel level, PrimordialDemonEntity demon, LivingEntity target) {
        if (demon == null || !demon.isAlive()) return;
        executeSkillInternal(id, level, demon, target, demon.getAttackDamage(), 1.0F, false,
                (victim, dmg) -> demon.dealDemonicDamage(victim, dmg));
    }

    private static void applyPoolDamage(
            Mob caster,
            LivingEntity target,
            float baseDmg,
            boolean isTrueDragon,
            @Nullable BiConsumer<LivingEntity, Float> damageFn
    ) {
        if (target == null || !target.isAlive() || !TensuraSkillHelper.isValidSkillTarget(caster, target)) return;
        float finalDmg = baseDmg;
        if (isTrueDragon && target instanceof PrimordialDemonEntity) {
            finalDmg *= 1.45F;
        }
        if (damageFn != null) {
            damageFn.accept(target, finalDmg);
        } else {
            target.hurt(caster.damageSources().mobAttack(caster), finalDmg);
        }
    }

    private static boolean canAffectPoolTarget(Mob caster, LivingEntity living) {
        return living != caster && living.isAlive() && !caster.isAlliedTo(living) && TensuraSkillHelper.isValidSkillTarget(caster, living);
    }

    private static void executeSkillInternal(
            int id,
            ServerLevel level,
            Mob caster,
            @Nullable LivingEntity target,
            double attackPower,
            float powerMultiplier,
            boolean isTrueDragon,
            @Nullable BiConsumer<LivingEntity, Float> damageFn
    ) {
        if (caster == null || !caster.isAlive()) return;

        Vec3 tPos = target != null ? target.position() : caster.position().add(caster.getLookAngle().scale(8.0D));
        double power = Math.max(40.0D, attackPower) * Math.max(1.0F, powerMultiplier);
        double aoeInflate = isTrueDragon ? 3.5D : 0.0D;

        switch (id) {
            case SKILL_BEELZEBUTH -> {
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
                level.sendParticles(ParticleTypes.DRAGON_BREATH, tPos.x, tPos.y + 1.0D, tPos.z, 60, 1.4, 1.4, 1.4, 0.1);
                if (isTrueDragon) {
                    AABB box = new AABB(tPos, tPos).inflate(7.0D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 2.5D), true, damageFn);
                    }
                } else {
                    applyPoolDamage(caster, target, (float) (power * 2.5D), false, damageFn);
                }
                caster.heal(80.0F * powerMultiplier);
            }
            case SKILL_INFINITY_SNAP -> {
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.5F, 1.8F);
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.8F);
                level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 1.0D, tPos.z, 3, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tPos.x, tPos.y + 1.0D, tPos.z, isTrueDragon ? 4 : 2, 0.8, 0.8, 0.8, 0);
                if (isTrueDragon) {
                    AABB box = new AABB(tPos, tPos).inflate(7.5D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 3.0D), true, damageFn);
                    }
                } else {
                    applyPoolDamage(caster, target, (float) (power * 3.0D), false, damageFn);
                }
            }
            case SKILL_POWER_BEAM -> {
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.8F);
                level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 1.0D, tPos.z, 2, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tPos.x, tPos.y + 1.0D, tPos.z, 45, 0.9, 0.9, 0.9, 0.1);
                if (isTrueDragon) {
                    Vec3 eye = caster.getEyePosition();
                    Vec3 aim = tPos.add(0, 1.0D, 0).subtract(eye);
                    if (aim.lengthSqr() > 0.01D) {
                        Vec3 dir = aim.normalize();
                        for (double d = 2.0D; d <= Math.min(28.0D, eye.distanceTo(tPos) + 3.0D); d += 1.2D) {
                            Vec3 p = eye.add(dir.scale(d));
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 4, 0.3, 0.3, 0.3, 0.03);
                        }
                    }
                    AABB box = new AABB(tPos, tPos).inflate(5.5D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 2.2D), true, damageFn);
                    }
                } else {
                    applyPoolDamage(caster, target, (float) (power * 2.0D), false, damageFn);
                }
            }
            case SKILL_SPACE_TELEPORT -> {
                if (target != null) {
                    Vec3 behind = target.position().subtract(target.getLookAngle().scale(isTrueDragon ? 3.5D : 2.0D));
                    caster.teleportTo(behind.x, behind.y + (isTrueDragon ? 0.5D : 0.0D), behind.z);
                    level.playSound(null, behind.x, behind.y, behind.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.2F);
                    level.sendParticles(ParticleTypes.PORTAL, behind.x, behind.y + 1.0D, behind.z, 40, 0.6, 0.8, 0.6, 0.1);
                    applyPoolDamage(caster, target, (float) (power * 2.0D), isTrueDragon, damageFn);
                }
            }
            case SKILL_REALITY_ILLUSION -> {
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 2.0F, 1.0F);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, caster.getX(), caster.getY() + 1.0D, caster.getZ(), 40, 1.5, 0.5, 1.5, 0.1);
                caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, isTrueDragon ? 140 : 100, 0, false, false));
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, isTrueDragon ? 160 : 100, isTrueDragon ? 3 : 2, false, false));
                if (isTrueDragon) {
                    caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 2, false, false));
                }
            }
            case SKILL_TIME_FREEZE -> {
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 0.6F);
                level.sendParticles(ParticleTypes.END_ROD, tPos.x, tPos.y + 1.0D, tPos.z, 55, 2.5, 1.8, 2.5, 0.05);
                double r = 6.0D + aoeInflate;
                AABB box = new AABB(tPos.x - r, tPos.y - 4, tPos.z - r, tPos.x + r, tPos.y + 4, tPos.z + r);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, isTrueDragon ? 120 : 80, 255, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, isTrueDragon ? 120 : 80, 5, false, false));
                    if (isTrueDragon) {
                        applyPoolDamage(caster, e, (float) (power * 1.4D), true, damageFn);
                    }
                }
            }
            case SKILL_SOUL_DRAIN -> {
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.0F, 1.2F);
                level.sendParticles(ParticleTypes.SOUL, tPos.x, tPos.y + 1.0D, tPos.z, 40, 0.8, 1.2, 0.8, 0.1);
                if (target != null) {
                    float drained = (float) (power * 1.5D);
                    applyPoolDamage(caster, target, drained, isTrueDragon, damageFn);
                    caster.heal(drained * (isTrueDragon ? 0.65F : 0.5F));
                }
            }
            case SKILL_MIND_BLAST -> {
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 1.5F);
                level.sendParticles(ParticleTypes.SONIC_BOOM, tPos.x, tPos.y + 1.0D, tPos.z, isTrueDragon ? 4 : 2, 0.4, 0.4, 0.4, 0);
                double r = 8.0D + aoeInflate;
                AABB box = new AABB(tPos.x - r, tPos.y - 4, tPos.z - r, tPos.x + r, tPos.y + 4, tPos.z + r);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, false, false));
                    applyPoolDamage(caster, e, (float) (power * 1.5D), isTrueDragon, damageFn);
                }
            }
            case SKILL_TAISUI_STARS -> {
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.5F);
                level.sendParticles(ParticleTypes.END_ROD, tPos.x, tPos.y + 1.0D, tPos.z, 55, 0.9, 0.9, 0.9, 0.12);
                if (isTrueDragon) {
                    AABB box = new AABB(tPos, tPos).inflate(7.5D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 2.1D), true, damageFn);
                    }
                } else {
                    applyPoolDamage(caster, target, (float) (power * 2.0D), false, damageFn);
                }
            }
            case SKILL_ALKAID_CHAOS -> {
                Vec3 origin = caster.position();
                SpatialTeleportAbility.spawnMagicCircleParticles(level, origin, isTrueDragon ? 3.8D : 2.2D, 32);
                level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.2F);

                Vec3 dest = tPos;
                if (target != null) {
                    Vec3 targetLook = Vec3.directionFromRotation(0.0F, target.getYRot()).normalize();
                    dest = target.position().subtract(targetLook.scale(isTrueDragon ? 4.0D : 2.5D));
                }
                caster.teleportTo(dest.x, dest.y + (isTrueDragon ? 0.5D : 0.0D), dest.z);
                caster.fallDistance = 0.0F;

                float portalScale = isTrueDragon ? 4.0F : 2.2F;
                SpatialTeleportAbility.spawnMagicCircleParticles(level, dest, portalScale, 40);
                ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                        SkillVisualConfig.SKILL_SPATIAL_TELEPORT,
                        caster.getUUID(),
                        dest,
                        new Vec3(SpatialTeleportAbility.MODE_DEPART_PERSONAL, 0.0D, 0.0D),
                        24,
                        ClientboundCinematicSkillPacket.ACTION_START,
                        portalScale
                ));
                level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.2F, 1.35F);
                level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 0.9F);
                applyPoolDamage(caster, target, (float) (power * 2.5D), isTrueDragon, damageFn);
            }
            case SKILL_JACOB_LADDER, SKILL_HEAVENLY_JUDGMENT -> {
                if (caster instanceof PrimordialDemonEntity demon) {
                    demon.executeChromaticLightPillar(target, demon.getDemonType());
                } else {
                    level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.5F, 0.85F);
                    for (int y = 0; y < 28; y += 2) {
                        level.sendParticles(ParticleTypes.END_ROD, tPos.x, tPos.y + y, tPos.z, 10, 1.6, 0.4, 1.6, 0.04);
                        level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + y, tPos.z, 1, 0, 0, 0, 0);
                    }
                    double r = 7.5D;
                    AABB box = new AABB(tPos, tPos).inflate(r);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 2.4D), isTrueDragon, damageFn);
                    }
                }
            }
            case SKILL_DISINTEGRATION -> {
                if (caster instanceof PrimordialDemonEntity demon) {
                    demon.executeDisintegration(target, demon.getDemonType());
                } else {
                    level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.5F, 1.15F);
                    level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 1.0D, tPos.z, 4, 0.5, 0.5, 0.5, 0);
                    for (int step = 0; step < 24; step++) {
                        double ang = step * (Math.PI * 2.0D / 24.0D);
                        double rad = 5.5D;
                        level.sendParticles(ParticleTypes.END_ROD, tPos.x + Math.cos(ang) * rad, tPos.y + 0.5D, tPos.z + Math.sin(ang) * rad, 3, 0, 0.8, 0, 0.03);
                    }
                    AABB box = new AABB(tPos, tPos).inflate(6.5D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 2.8D), isTrueDragon, damageFn);
                    }
                }
            }
            case SKILL_BLACK_FLASH -> {
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5F, 0.8F);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tPos.x, tPos.y + 1.0D, tPos.z, 80, 0.7, 0.9, 0.7, 0.1);
                level.sendParticles(ParticleTypes.FLASH, tPos.x, tPos.y + 1.0D, tPos.z, 3, 0, 0, 0, 0);
                if (isTrueDragon) {
                    AABB box = new AABB(tPos, tPos).inflate(5.0D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 2.8D), true, damageFn);
                    }
                } else {
                    applyPoolDamage(caster, target, (float) (power * 2.8D), false, damageFn);
                }
            }
            case SKILL_DRAGON_NOVA -> {
                level.playSound(null, tPos.x, tPos.y, tPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.5F, 0.6F);
                level.sendParticles(ParticleTypes.DRAGON_BREATH, tPos.x, tPos.y + 1.0D, tPos.z, 110, 1.8, 1.8, 1.8, 0.2);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tPos.x, tPos.y + 1.0D, tPos.z, isTrueDragon ? 6 : 4, 1.0, 1.0, 1.0, 0);
                if (isTrueDragon) {
                    AABB box = new AABB(tPos, tPos).inflate(9.0D);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, living -> canAffectPoolTarget(caster, living))) {
                        applyPoolDamage(caster, e, (float) (power * 3.5D), true, damageFn);
                    }
                } else {
                    applyPoolDamage(caster, target, (float) (power * 3.5D), false, damageFn);
                }
            }
            case SKILL_SEER_FLESH_PULSE -> {
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.5F, 1.0F);
                level.sendParticles(ParticleTypes.SCULK_SOUL, caster.getX(), caster.getY() + 1.0D, caster.getZ(), 50, 1.2, 0.8, 1.2, 0.1);
                caster.heal(120.0F * powerMultiplier);
                double r = 8.0D + aoeInflate;
                AABB pulseBox = new AABB(caster.getX() - r, caster.getY() - 4, caster.getZ() - r, caster.getX() + r, caster.getY() + 4, caster.getZ() + r);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pulseBox, living -> canAffectPoolTarget(caster, living))) {
                    e.knockback(isTrueDragon ? 2.8D : 2.0D, caster.getX() - e.getX(), caster.getZ() - e.getZ());
                    applyPoolDamage(caster, e, (float) (power * 1.5D), isTrueDragon, damageFn);
                }
            }
        }
    }
}
