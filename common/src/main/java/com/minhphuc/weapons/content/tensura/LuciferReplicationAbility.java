package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kiêu Ngạo Vương Lucifer (Lord of Pride Lucifer - 傲慢之王):
 * - Quyền năng tối thượng của Ma Thần Guy Crimson (Rouge).
 * - Tự động ghi nhớ và sao chép tuyệt kỹ tối thượng gần nhất của đối thủ (Velgrynd, Milim, Warden, Wither...).
 * - Tái kích hoạt lại với 100% uy lực và hiệu ứng hủy diệt nguyên bản!
 */
public class LuciferReplicationAbility {

    public static final String NBT_REPLICATED_SKILL = "TensuraLuciferSkill";
    public static final String NBT_REPLICATED_NAME = "TensuraLuciferSkillName";

    /**
     * Ghi nhận kỹ năng khi kẻ địch tung chiêu trong thế giới
     */
    public static void recordSkillObserved(ServerLevel level, Vec3 sourcePos, String skillId, String skillDisplayName) {
        AABB notifyBox = new AABB(sourcePos.x - 48.0D, sourcePos.y - 32.0D, sourcePos.z - 48.0D,
                                  sourcePos.x + 48.0D, sourcePos.y + 32.0D, sourcePos.z + 48.0D);

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, notifyBox);
        for (ServerPlayer player : players) {
            boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
            if (isTrueDemonLord) {
                CompoundTag tag = EntityDataHelper.getCustomData(player);
                tag.putString(NBT_REPLICATED_SKILL, skillId);
                tag.putString(NBT_REPLICATED_NAME, skillDisplayName);

                player.displayClientMessage(
                        Component.literal("§6§l✦ KIÊU NGẠO VƯƠNG (LUCIFER) ✦ §eĐã sao chép thành công: §c" + skillDisplayName + "§e!"),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.8F);
            }
        }
    }

    public static boolean cast(ServerLevel level, ServerPlayer player) {
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        if (!isTrueDemonLord) {
            player.displayClientMessage(
                    Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Yêu cầu thức tỉnh Chân Ma Vương để vận dụng quyền năng Kiêu Ngạo Vương Lucifer!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }

        // Hồi chiêu 20 giây
        if (player.getCooldowns().isOnCooldown(ModItems.MOONLIGHT_SWORD.get())
                || player.getCooldowns().isOnCooldown(ModItems.DEMON_LORD_SEED.get())) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Kỹ năng Kiêu Ngạo Vương Lucifer đang trong thời gian hồi chiêu!"),
                    true
            );
            return false;
        }

        CompoundTag tag = EntityDataHelper.getCustomData(player);
        String replicatedSkill = tag.getString(NBT_REPLICATED_SKILL);
        String skillName = tag.getString(NBT_REPLICATED_NAME);

        if (replicatedSkill.isEmpty()) {
            // Mặc định: Tuyệt kỹ bản thể của Guy Crimson - Diệt Thế Hỏa (Abyss Core)
            replicatedSkill = "ABYSS_CORE";
            skillName = "Diệt Thế Hỏa (Abyss Core - Hỏa Long Guy Crimson)";
        }

        player.getCooldowns().addCooldown(ModItems.MOONLIGHT_SWORD.get(), 400);
        player.getCooldowns().addCooldown(ModItems.DEMON_LORD_SEED.get(), 400);

        player.displayClientMessage(
                Component.literal("§4§l[KIÊU NGẠO VƯƠNG LUCIFER] §c\"Quyền năng của ngươi... đối với ta chỉ là trò trẻ con! Hãy nếm trải chính tuyệt kỹ của ngươi!\""),
                false
        );

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.0F, 0.85F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0F, 1.1F);

        // Hiệu ứng hào quang đỏ thẫm Guy Crimson
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 80, 0.8D, 1.5D, 0.8D, 0.1D);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.2D, player.getZ(), 2, 0, 0, 0, 0);

        // THI TRIỂN KỸ NĂNG ĐƯỢC SAO CHÉP
        switch (replicatedSkill) {
            case "CARDINAL_ACCEL" -> executeReplicatedCardinalAccel(level, player);
            case "DRAGON_NOVA" -> DragonNovaAbility.cast(level, player);
            case "SPACETIME_COLLAPSE" -> executeReplicatedSpacetime(level, player);
            case "HORIZONTAL_BEAM" -> HorizontalHolyBeamAbility.cast(level, player);
            case "SONIC_BOOM" -> executeReplicatedSonicBoom(level, player);
            default -> executeReplicatedAbyssCore(level, player);
        }

        return true;
    }

    /**
     * 1. Sao Chép Gia Tốc Chước Nhiệt Long (Velgrynd - Cardinal Acceleration)
     */
    private static void executeReplicatedCardinalAccel(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(2.5D).add(0, 0.2D, 0));
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 1.2F);

        // Quét sát thương và đào phá hầm trên đường lao
        Vec3 curPos = player.position();
        for (double d = 1.0D; d <= 20.0D; d += 1.5D) {
            Vec3 pt = curPos.add(look.scale(d));
            level.sendParticles(ParticleTypes.DRAGON_BREATH, pt.x, pt.y + 1.0D, pt.z, 8, 0.6D, 0.6D, 0.6D, 0.05D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y + 1.0D, pt.z, 10, 0.8D, 0.8D, 0.8D, 0.08D);
            level.sendParticles(ParticleTypes.LAVA, pt.x, pt.y + 0.5D, pt.z, 4, 0.5D, 0.5D, 0.5D, 0.02D);

            BlockPos bp = BlockPos.containing(pt);
            BlockState st = level.getBlockState(bp);
            if (!st.isAir() && st.getDestroySpeed(level, bp) >= 0.0F && level.random.nextFloat() <= 0.6F) {
                level.destroyBlock(bp, false);
            }

            AABB box = new AABB(pt.x - 2.5D, pt.y - 1.5D, pt.z - 2.5D, pt.x + 2.5D, pt.y + 2.5D, pt.z + 2.5D);
            for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
                v.hurt(level.damageSources().playerAttack(player), 120.0F);
                v.setDeltaMovement(look.scale(2.2D).add(0, 0.8D, 0));
                v.setRemainingFireTicks(160);
            }
        }
    }

    /**
     * 2. Sao Chép Thao Túng Thời Không (Velgrynd - Spacetime Collapse)
     */
    private static void executeReplicatedSpacetime(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(18.0D);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());

        for (LivingEntity v : victims) {
            v.setDeltaMovement(Vec3.ZERO);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255));
            v.hurt(level.damageSources().playerAttack(player), 90.0F);
            v.setDeltaMovement(new Vec3(0, 1.5D, 0));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 4.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 0.9F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.2D, player.getZ(), 3, 0.5D, 0.5D, 0.5D, 0);
    }

    /**
     * 3. Sao Chép Sóng Âm Vực Thẳm (Warden Sonic Boom)
     */
    private static void executeReplicatedSonicBoom(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        double maxDist = 30.0D;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 5.0F, 1.0F);

        for (double d = 1.0D; d <= maxDist; d += 1.0D) {
            Vec3 pt = start.add(dir.scale(d));
            level.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);

            AABB box = new AABB(pt.x - 1.5D, pt.y - 1.5D, pt.z - 1.5D, pt.x + 1.5D, pt.y + 1.5D, pt.z + 1.5D);
            for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
                v.hurt(level.damageSources().playerAttack(player), 150.0F);
                v.setDeltaMovement(dir.scale(2.5D).add(0, 0.8D, 0));
            }
        }
    }

    /**
     * 4. Bản Thể Guy Crimson: Diệt Thế Hỏa (Abyss Core Crimson Nova)
     */
    private static void executeReplicatedAbyssCore(ServerLevel level, ServerPlayer player) {
        Vec3 targetPos = player.position().add(player.getLookAngle().scale(8.0D));

        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F, 0.7F);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 1.2F);

        // Cột lửa đỏ chọc trời 25m
        for (double y = 0; y <= 25.0D; y += 1.0D) {
            level.sendParticles(ParticleTypes.FLAME, targetPos.x, targetPos.y + y, targetPos.z, 20, 2.5D, 0.5D, 2.5D, 0.2D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, targetPos.x, targetPos.y + y, targetPos.z, 15, 2.0D, 0.5D, 2.0D, 0.15D);
            level.sendParticles(ParticleTypes.LAVA, targetPos.x, targetPos.y + y, targetPos.z, 8, 1.8D, 0.5D, 1.8D, 0.1D);
        }

        AABB blastArea = new AABB(targetPos.x - 12.0D, targetPos.y - 4.0D, targetPos.z - 12.0D,
                                  targetPos.x + 12.0D, targetPos.y + 16.0D, targetPos.z + 12.0D);
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != player && e.isAlive())) {
            v.hurt(level.damageSources().playerAttack(player), 180.0F);
            v.setRemainingFireTicks(200);
            v.setDeltaMovement(new Vec3(0, 1.4D, 0));
        }
    }
}
