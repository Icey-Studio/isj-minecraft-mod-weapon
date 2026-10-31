package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.content.divine.FaithAndGraceAbility;
import com.minhphuc.weapons.content.divine.PurificationPillarAbility;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QUẢN LÝ ÁN TỬ THỜI GIAN KHI BỊ CẮT TRÁI TIM (SEVERED HEART FATAL CURSE):
 * - Khi bị chém bởi Tuyệt Kĩ Chém Xuyên Thời Gian và thế giới trở lại màu sắc bình thường:
 *   Sinh vật KHÔNG nhận sát thương tức thì.
 * - Một án tử 5 giây (100 ticks) được áp dụng.
 * - Điều kiện điều trị:
 *   1. Đứng trong Phép Màu Của Thần Linh (PurificationPillarAbility)
 *   2. Cầm hoặc kích hoạt Vật Tổ Trường Sinh (Items.TOTEM_OF_UNDYING)
 *   3. Đứng trong Bí Ẩn Của Đức Tin Và Ân Sủng (FaithAndGraceAbility)
 * - Nếu sau 5 giây không được điều trị: 100% SINH VẬT SẼ CHẾT!
 * - Tối ưu hóa cực hạn: Nếu không có án tử nào đang active, thoát ngay lập tức (0 lag).
 */
public class SeveredHeartCurseManager {

    public static class ActiveCurse {
        public final ServerLevel level;
        public final UUID victimUuid;
        public final ServerPlayer caster;
        public final String victimName;
        public int ticksRemaining; // 100 ticks = 5 giây

        public ActiveCurse(ServerLevel level, UUID victimUuid, ServerPlayer caster, String victimName) {
            this.level = level;
            this.victimUuid = victimUuid;
            this.caster = caster;
            this.victimName = victimName;
            this.ticksRemaining = 100;
        }
    }

    private static final Map<UUID, ActiveCurse> ACTIVE_CURSES = new ConcurrentHashMap<>();

    /**
     * KIỂM TRA 4 THỰC THỂ TỐI CAO MIỄN NHIỄM VÀ TỰ PHÁ VỠ KỸ NĂNG:
     * - Milim Nava
     * - Chước Nhiệt Long Velgrynd
     * - Bạch Băng Long Velzard
     * - Ác Ma Thủy Tổ Rouge (Guy Crimson)
     */
    public static boolean isUltimateBossImmune(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof MilimEntity) return true;
        if (entity instanceof VelgryndEntity) return true;
        if (entity instanceof VelzardEntity) return true;
        if (entity instanceof PrimordialDemonEntity demon
                && demon.getDemonType() == DemonType.ROUGE) {
            return true;
        }
        if (entity instanceof Player player) {
            if (PrimordialPlayerDataHelper.getPrimordialType(player) == DemonType.ROUGE) {
                return true;
            }
        }
        return false;
    }

    /**
     * ÁP DỤNG ÁN TỬ 5 GIÂY LÊN MỤC TIÊU BỊ ĐOẠT TIM
     */
    public static void applyCurse(ServerLevel level, LivingEntity victim, ServerPlayer caster) {
        if (victim == null || !victim.isAlive()) return;
        if (isUltimateBossImmune(victim)) return;

        ACTIVE_CURSES.put(victim.getUUID(), new ActiveCurse(level, victim.getUUID(), caster, victim.getName().getString()));

        if (victim instanceof ServerPlayer playerVictim) {
            playerVictim.connection.send(new ClientboundSetTitlesAnimationPacket(0, 50, 15));
            playerVictim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l[MẤT TRÁI TIM]")));
            playerVictim.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cÁn tử thời gian: 5 giây để tìm Thánh Quang / Vật Tổ!")));
            playerVictim.displayClientMessage(
                    Component.literal("§4§l⚠️ BẠN ĐÃ BỊ ĐOẠT MẤT TRÁI TIM! §cSẽ tử vong sau 5.0 giây nếu không được điều trị!"),
                    false
            );
        }
    }

    /**
     * KIỂM TRA ĐIỀU KIỆN ĐIỀU TRỊ CỦA NẠN NHÂN:
     * 1. Phép màu của thần linh (PurificationPillarAbility)
     * 2. Vật tổ trường sinh (Items.TOTEM_OF_UNDYING)
     * 3. Bí mật của đức tin và ân sủng (FaithAndGraceAbility)
     */
    public static boolean isTreated(LivingEntity victim) {
        if (victim == null || !victim.isAlive()) return false;

        // 1. Vật tổ trường sinh (cầm trên tay chính hoặc tay phụ) - O(1) kiểm tra tức thì
        if (victim.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || victim.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return true;
        }

        // 2. Phép màu của thần linh (PurificationPillarAbility)
        if (PurificationPillarAbility.isNearActivePillar(victim)) {
            return true;
        }

        // 3. Bí mật của đức tin và ân sủng (FaithAndGraceAbility)
        if (FaithAndGraceAbility.isNearActiveGrace(victim)) {
            return true;
        }

        return false;
    }

    /**
     * TICK ĐẾM NGƯỢC VÀ KIỂM TRA ĐIỀU TRỊ MỖI TICK TRÊN SERVER
     */
    public static void tick(ServerLevel level) {
        // TỐI ƯU CỰC HẠN: Nếu không có án tử nào đang active, thoát ngay (0 lag, 0 overhead)
        if (ACTIVE_CURSES.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveCurse>> it = ACTIVE_CURSES.entrySet().iterator();
        while (it.hasNext()) {
            ActiveCurse curse = it.next().getValue();
            if (curse.level != level) continue;

            Entity ent = level.getEntity(curse.victimUuid);
            if (!(ent instanceof LivingEntity victim) || !victim.isAlive()) {
                it.remove();
                continue;
            }

            // KIỂM TRA ĐIỀU TRỊ CỨU SỐNG NGAY LẬP TỨC
            if (isTreated(victim)) {
                // Xóa án tử thành công!
                it.remove();

                // Hiệu ứng thanh tẩy hồi sinh
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.8F, 1.2F);
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.5F);

                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        victim.getX(), victim.getY() + 1.0D, victim.getZ(), 30, 0.5D, 0.6D, 0.5D, 0.2D);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        victim.getX(), victim.getY() + 1.0D, victim.getZ(), 15, 0.4D, 0.5D, 0.4D, 0.05D);

                if (victim instanceof ServerPlayer targetPlayer) {
                    targetPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 10));
                    targetPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§l[ĐÃ ĐIỀU TRỊ]")));
                    targetPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eThánh Quang & Vật Tổ đã tái tạo trái tim của bạn!")));
                    targetPlayer.displayClientMessage(
                            Component.literal("§a✨ Bạn đã được thánh quang chữa lành và hóa giải hoàn toàn Án Tử Thời Gian!"),
                            true
                    );
                }

                if (curse.caster != null && curse.caster.isAlive()) {
                    curse.caster.displayClientMessage(
                            Component.literal("§e⚠️ Mục tiêu " + curse.victimName + " đã nhận được điều trị và thoát khỏi Án Tử Thời Gian!"),
                            true
                    );
                }
                continue;
            }

            // ĐẾM NGƯỢC 5 GIÂY
            curse.ticksRemaining--;

            // Hiệu ứng tim đập dồn dập mỗi 20 ticks (1s)
            if (curse.ticksRemaining % 20 == 0) {
                float pitch = 0.8F + ((100 - curse.ticksRemaining) / 20.0F) * 0.2F;
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, pitch);

                level.sendParticles(ParticleTypes.SMOKE,
                        victim.getX(), victim.getY() + 0.8D, victim.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.02D);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        victim.getX(), victim.getY() + 0.9D, victim.getZ(), 3, 0.2D, 0.3D, 0.2D, 0.01D);

                if (victim instanceof ServerPlayer playerVictim) {
                    playerVictim.displayClientMessage(
                            Component.literal("§4§l[TIM ĐẬP DỒN DẬP] §cThời gian còn lại: " + String.format("%.1fs", curse.ticksRemaining / 20.0F) + " §7(Tìm Thánh Quang / Vật Tổ!)"),
                            true
                    );
                }
            }

            // HẾT 5 GIÂY: 100% SINH VẬT SẼ CHẾT!
            if (curse.ticksRemaining <= 0) {
                it.remove();

                // Kiểm tra lại lần cuối nếu người chơi vừa kịp cầm Totem
                if (isTreated(victim)) {
                    continue;
                }

                victim.invulnerableTime = 0;
                // Sát thương tuyệt đối xuyên giáp, xuyên kháng cự, xuyên bất tử
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
                            Component.literal("§4§l[ÁNTỬ HOÀN TẤT] §fTrái tim của §e" + curse.victimName + " §fđã ngừng đập vĩnh viễn!"),
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
