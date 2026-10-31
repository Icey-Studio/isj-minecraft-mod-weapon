package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.content.divine.FaithAndGraceAbility;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QUẢN LÝ ÁN TỬ KHÔNG THỜI GIAN CỦA THIÊN MA KIẾM (3 GIÂY = 60 TICKS):
 * - Khi bị chém bởi Tuyệt Kĩ Chém Xuyên Không Thời Gian:
 *   Sinh vật rơi đồng thời Trái Tim và Bộ Não.
 * - Sau khi thế giới trở lại màu sắc bình thường: Án tử 3 giây (60 ticks) bắt đầu đếm ngược.
 * - ĐIỀU KIỆN ĐIỀU TRỊ DUY NHẤT:
 *   + Đứng trong vùng Bí Mật Của Đức Tin Và Ân Sủng (FaithAndGraceAbility).
 * - Sau 3 giây nếu không được điều trị: 100% SINH VẬT SẼ CHẾT!
 * - TỐI ƯU HÓA CAO ĐỘ: Thoát tức thì O(1) khi không có án tử nào đang active.
 */
public class ThienMaCurseManager {

    public static class ActiveThienMaCurse {
        public final ServerLevel level;
        public final UUID victimUuid;
        public final ServerPlayer caster;
        public final String victimName;
        public int ticksRemaining; // 60 ticks = 3 giây

        public ActiveThienMaCurse(ServerLevel level, UUID victimUuid, ServerPlayer caster, String victimName) {
            this.level = level;
            this.victimUuid = victimUuid;
            this.caster = caster;
            this.victimName = victimName;
            this.ticksRemaining = 60;
        }
    }

    private static final Map<UUID, ActiveThienMaCurse> ACTIVE_CURSES = new ConcurrentHashMap<>();

    /**
     * CHỈ CÓ DUY NHẤT BẠCH BĂNG LONG (VELZARD) LÀ MIỄN NHIỄM VÀ TỰ PHÁ VỠ KỸ NĂNG NÀY
     */
    public static boolean isVelzardImmune(LivingEntity entity) {
        return entity instanceof VelzardEntity;
    }

    /**
     * ÁP DỤNG ÁN TỬ 3 GIÂY (60 TICKS) LÊN MỤC TIÊU BỊ ĐOẠT NÃO VÀ TIM
     */
    public static void applyCurse(ServerLevel level, LivingEntity victim, ServerPlayer caster) {
        if (victim == null || !victim.isAlive()) return;
        if (isVelzardImmune(victim)) return;

        ACTIVE_CURSES.put(victim.getUUID(), new ActiveThienMaCurse(level, victim.getUUID(), caster, victim.getName().getString()));

        if (victim instanceof ServerPlayer playerVictim) {
            playerVictim.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 10));
            playerVictim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l[MẤT NÃO & TIM]")));
            playerVictim.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cÁn tử: 3 giây để nhận Ân Sủng Thần Thánh!")));
            playerVictim.displayClientMessage(
                    Component.literal("§4§l⚠️ BẠN ĐÃ BỊ ĐOẠT MẤT NÃO VÀ TRÁI TIM! §cSẽ tử vong sau 3.0 giây nếu không có Bí Mật Của Đức Tin Và Ân Sủng!"),
                    false
            );
        }
    }

    /**
     * KIỂM TRA ĐIỀU KIỆN ĐIỀU TRỊ:
     * DUY NHẤT: Bí mật của đức tin và ân sủng (FaithAndGraceAbility)
     */
    public static boolean isTreated(LivingEntity victim) {
        if (victim == null || !victim.isAlive()) return false;
        return FaithAndGraceAbility.isNearActiveGrace(victim);
    }

    /**
     * TICK ĐẾM NGƯỢC 3 GIÂY VÀ THI HÀNH ÁN TỬ TRÊN SERVER (0 LAG KHI RỖNG)
     */
    public static void tick(ServerLevel level) {
        if (ACTIVE_CURSES.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveThienMaCurse>> it = ACTIVE_CURSES.entrySet().iterator();
        while (it.hasNext()) {
            ActiveThienMaCurse curse = it.next().getValue();
            if (curse.level != level) continue;

            Entity ent = level.getEntity(curse.victimUuid);
            if (!(ent instanceof LivingEntity victim) || !victim.isAlive()) {
                it.remove();
                continue;
            }

            // KIỂM TRA ĐIỀU TRỊ BẰNG ĐỨC TIN VÀ ÂN SỦNG
            if (isTreated(victim)) {
                it.remove();

                // Hiệu ứng thánh quang cứu rỗi
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.8F, 1.2F);
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.5F);

                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        victim.getX(), victim.getY() + 1.0D, victim.getZ(), 25, 0.4D, 0.5D, 0.4D, 0.2D);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        victim.getX(), victim.getY() + 1.0D, victim.getZ(), 15, 0.4D, 0.5D, 0.4D, 0.05D);

                if (victim instanceof ServerPlayer targetPlayer) {
                    targetPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 10));
                    targetPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§l[ĐÃ ĐIỀU TRỊ]")));
                    targetPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eÂn Sủng Thần Thánh đã tái tạo não và tim của bạn!")));
                    targetPlayer.displayClientMessage(
                            Component.literal("§a✨ Bạn đã được Bí Mật Đức Tin Và Ân Sủng chữa lành hoàn toàn khỏi Án Tử!"),
                            true
                    );
                }

                if (curse.caster != null && curse.caster.isAlive()) {
                    curse.caster.displayClientMessage(
                            Component.literal("§e⚠️ Mục tiêu " + curse.victimName + " đã nhận được Ân Sủng Thần Thánh hóa giải án tử!"),
                            true
                    );
                }
                continue;
            }

            // ĐẾM NGƯỢC 3 GIÂY (60 TICKS)
            curse.ticksRemaining--;

            // Nhịp tim đập dồn dập mỗi 15 ticks
            if (curse.ticksRemaining % 15 == 0) {
                float pitch = 1.0F + ((60 - curse.ticksRemaining) / 15.0F) * 0.25F;
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, pitch);

                level.sendParticles(ParticleTypes.SMOKE,
                        victim.getX(), victim.getY() + 0.8D, victim.getZ(), 5, 0.2D, 0.3D, 0.2D, 0.02D);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        victim.getX(), victim.getY() + 0.9D, victim.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.01D);

                if (victim instanceof ServerPlayer playerVictim) {
                    playerVictim.displayClientMessage(
                            Component.literal("§4§l[ÁNTỬ THIÊN MA] §cThời gian: " + String.format("%.1fs", curse.ticksRemaining / 20.0F) + " §7(Cần Bí Mật Đức Tin Và Ân Sủng!)"),
                            true
                    );
                }
            }

            // HẾT 3 GIÂY: 100% SINH VẬT SẼ CHẾT!
            if (curse.ticksRemaining <= 0) {
                it.remove();

                if (isTreated(victim)) {
                    continue;
                }

                victim.invulnerableTime = 0;
                // Sát thương tuyệt đối xuyên mọi lớp bảo vệ
                victim.hurt(level.damageSources().fellOutOfWorld(), 999999.0F);
                if (victim.isAlive()) {
                    victim.kill();
                }

                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.0F, 1.8F);
                level.sendParticles(ParticleTypes.SOUL,
                        victim.getX(), victim.getY() + 1.0D, victim.getZ(), 20, 0.5D, 0.6D, 0.5D, 0.08D);
                level.sendParticles(ParticleTypes.EXPLOSION,
                        victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.0D);

                if (curse.caster != null && curse.caster.isAlive()) {
                    curse.caster.displayClientMessage(
                            Component.literal("§4§l[ÁNTỬ HOÀN TẤT] §fSinh mệnh của §e" + curse.victimName + " §fđã bị Thiên Ma Kiếm đoạt tuyệt!"),
                            true
                    );
                }
            }
        }
    }

    public static void cleanup(ServerLevel level) {
        ACTIVE_CURSES.entrySet().removeIf(entry -> entry.getValue().level == level);
    }
}
