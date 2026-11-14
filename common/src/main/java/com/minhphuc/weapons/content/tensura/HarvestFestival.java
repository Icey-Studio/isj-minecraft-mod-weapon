package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.minhphuc.weapons.entity.tensura.DemonType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HarvestFestival {

    public static class ActiveRitual {
        public final ServerPlayer player;
        public final ServerLevel level;
        public int ticksRemaining;
        public final int totalTicks;

        public ActiveRitual(ServerPlayer player, ServerLevel level, int totalTicks) {
            this.player = player;
            this.level = level;
            this.ticksRemaining = totalTicks;
            this.totalTicks = totalTicks;
        }
    }

    private static final List<ActiveRitual> ACTIVE_RITUALS = new ArrayList<>();

    public static boolean isPlayerInRitual(ServerPlayer player) {
        for (ActiveRitual r : ACTIVE_RITUALS) {
            if (r.player == player) return true;
        }
        return false;
    }

    /**
     * Bắt đầu đại lễ hội thu hoạch thức tỉnh Chân Ma Vương (Harvest Festival)
     */
    public static void start(ServerPlayer player) {
        if (isPlayerInRitual(player)) return;

        ServerLevel level = (ServerLevel) player.level();
        // Tiêu hao hạt giống & linh hồn
        TensuraEvents.consumeSeedItemIfPresent(player);
        EntityDataHelper.getCustomData(player).putBoolean("TensuraHasSeed", true);
        TensuraEvents.consumeAllSouls(player);

        // Đăng ký ritual kéo dài 400 ticks (20 giây)
        ACTIVE_RITUALS.add(new ActiveRitual(player, level, 400));

        // Khởi đầu nghi thức: Trạng thái Sleep Mode (buồn ngủ không cưỡng lại được chuẩn Tensura)
        sendTitle(player, "§d§l[LỄ HỘI THU HOẠCH]", "§7Báo cáo. Cá thể bắt đầu tiến vào lễ hội thu hoạch");
        VoiceOfTheWorld.announce(player, "Báo cáo. Điều kiện thức tỉnh đã thỏa mãn. Bắt đầu Lễ Hội Thu Hoạch (Harvest Festival)!");

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.7F);
    }

    /**
     * Cập nhật từng giai đoạn của Lễ Hội Thu Hoạch mỗi tick (Tổng 20 giây = 400 ticks)
     */
    public static void tickRituals(ServerLevel serverLevel) {
        if (ACTIVE_RITUALS.isEmpty()) return;

        Iterator<ActiveRitual> it = ACTIVE_RITUALS.iterator();
        while (it.hasNext()) {
            ActiveRitual r = it.next();
            if (r.level != serverLevel) continue;
            if (r.player == null || !r.player.isAlive()) {
                it.remove();
                continue;
            }

            r.ticksRemaining--;
            int elapsed = r.totalTicks - r.ticksRemaining;

            // Khóa di chuyển & ban bất tử trong suốt 20 giây nghi thức
            r.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 255, false, false, false));
            r.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false, false));
            r.player.setDeltaMovement(0, 0, 0);

            // ============================================================
            // GIAI ĐOẠN 1 (0 -> 80 ticks, 0s -> 4s): Buồn ngủ, tối sầm, tim đập
            // ============================================================
            if (elapsed <= 80) {
                r.player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, false));
                r.player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, false));

                r.player.displayClientMessage(
                    Component.literal("§d§l[LỄ HỘI THU HOẠCH] §7Báo cáo. Lễ hội thu hoạch bắt đầu không thể dừng giữa chừng"),
                    true
                );

                if (elapsed == 40) {
                    VoiceOfTheWorld.announce(r.player, "Báo cáo. Toàn bộ chức năng sinh học của cá thể chuyển sang một chủng loài mới");
                }

                if (elapsed % 20 == 0) {
                    r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.8F);
                }

                // Hạt sương mù kén ma thuật tím bao bọc
                r.level.sendParticles(ParticleTypes.PORTAL, r.player.getX(), r.player.getY() + 1.0D, r.player.getZ(), 4, 0.4D, 0.4D, 0.4D, 0.05D);
                r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.player.getX(), r.player.getY() + 1.0D, r.player.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.02D);
            }

            // ============================================================
            // GIAI ĐOẠN 2 (80 -> 230 ticks, 4s -> 11.5s): Hiến tế linh hồn & Tái cấu trúc cơ thể
            // ============================================================
            if (elapsed == 80) {
                sendTitle(r.player, "§6§l[HIẾN TẾ 64 LINH HỒN]", "§fHạt Giống nảy mầm... Magicule tăng vọt!");
                VoiceOfTheWorld.announce(r.player, "§fBáo cáo. Đã hiến tế thành công 64 Linh Hồn! Năng lượng Magicule của cá thể vượt ngưỡng x10 lần!");
                r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.2F);
            }

            if (elapsed == 150) {
                VoiceOfTheWorld.announce(r.player, "§fBáo cáo. Bắt đầu tái cấu trúc vật chất cơ thể cá thể: Hai phần nhục thể và linh thể đã có thể biến đổi tự do theo tinh thần mong muốn!");
            }

            if (elapsed > 80 && elapsed <= 230) {
                // Xoáy tia sáng linh hồn màu xanh và cam hội tụ vào tim người chơi
                int percent = Math.min(100, ((elapsed - 80) * 100 / 140));
                r.player.displayClientMessage(
                    Component.literal("§5§l[TÁI CẤU TRÚC LINH HỒN] §fBáo cáo. Tế bào ma thuật của cá thể đang dung hợp... §6" + percent + "% 🧬"),
                    true
                );

                if (elapsed % 15 == 0) {
                    float pitch = 0.8F + ((elapsed - 80) / 150.0F) * 0.8F;
                    r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.5F, pitch);
                }

                // Vòng xoáy linh hồn bay vào ngực
                double angle = Math.toRadians((elapsed * 18) % 360);
                double radius = 1.8D;
                double px = r.player.getX() + Math.cos(angle) * radius;
                double pz = r.player.getZ() + Math.sin(angle) * radius;
                r.level.sendParticles(ParticleTypes.SOUL, px, r.player.getY() + 1.1D, pz, 1, 0, 0, 0, 0);
                r.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, r.player.getY() + 0.8D, pz, 1, 0, 0, 0, 0);
                r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.player.getX(), r.player.getY() + 1.0D, r.player.getZ(), 2, 0.2D, 0.3D, 0.2D, 0.02D);
            }

            // ============================================================
            // GIAI ĐOẠN 3 (230 -> 340 ticks, 11.5s -> 17s): Tiến hóa Kỹ Năng Bạo Thực Vương
            // ============================================================
            if (elapsed == 230) {
                VoiceOfTheWorld.announce(r.player, "§aBáo cáo. Tiến trình tái cấu trúc cơ thể cá thể hoàn tất!");
            }

            if (elapsed == 270) {
                sendTitle(r.player, "§c§l[TIẾN HÓA KỸ NĂNG]", "§dĐại Bộc Thực Giả §f-> §6Bạo Thực Vương Beelzebuth");
                VoiceOfTheWorld.announce(r.player, "§eBáo cáo. Bắt đầu phân tích và hợp nhất kỹ năng cá thể: [Kẻ Săn Mồi] + [Kẻ Hủ Hóa]...");
                r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0F, 1.0F);
            }

            if (elapsed == 330) {
                VoiceOfTheWorld.announce(r.player, "§a§lBáo cáo. Thành công! Cá thể đã mở khóa Kỹ Năng Tối Thượng: §6§l[BẠO THỰC VƯƠNG BEELZEBUTH]§a!");
                r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 2.0F, 0.9F);
            }

            if (elapsed > 270 && elapsed < 390) {
                r.player.displayClientMessage(
                    Component.literal("§6§l[TIẾN HÓA KỸ NĂNG] §fBáo cáo. §d§lBẠO THỰC VƯƠNG BEELZEBUTH §fcủa cá thể đang hình thành... 🌀"),
                    true
                );

                r.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, r.player.getX(), r.player.getY() + 1.2D, r.player.getZ(), 2, 0.3D, 0.4D, 0.3D, 0.05D);
                r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.player.getX(), r.player.getY() + 1.0D, r.player.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.02D);
            }

            // ============================================================
            // GIAI ĐOẠN 4 (390 -> 400 ticks, 19.5s -> 20s): Thức tỉnh hoàn tất, Bùng nổ Chân Ma Vương
            // ============================================================
            if (elapsed >= 395) {
                // Xóa hiệu ứng xấu
                r.player.removeEffect(MobEffects.DARKNESS);
                r.player.removeEffect(MobEffects.BLINDNESS);
                r.player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                // Thức tỉnh thành Chân Ma Vương
                EntityDataHelper.getCustomData(r.player).putBoolean("TensuraTrueDemonLord", true);
                EntityDataHelper.getCustomData(r.player).putBoolean("TensuraAwakenedDemonLord", true);

                // Tăng vĩnh viễn lượng máu tối đa lên 40 tim (80 HP)
                var hpAttr = r.player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (hpAttr != null && hpAttr.getBaseValue() < 80.0D) {
                    hpAttr.setBaseValue(80.0D);
                }
                r.player.setHealth(r.player.getMaxHealth());

                // Kích hoạt ngay quyền bay lượn trên không trung cho Chân Ma Vương
                r.player.getAbilities().mayfly = true;
                r.player.getAbilities().setFlyingSpeed(0.08F);
                r.player.onUpdateAbilities();

                // Nếu là Hoàng Sắc Thủy Tổ (Jaune), ban tặng Thần Khí Súng Lục Hoàng Kim
                if (PrimordialPlayerDataHelper.getPrimordialType(r.player) == DemonType.JAUNE) {
                    ItemStack gun = new ItemStack(com.minhphuc.weapons.init.ModItems.GOLDEN_GUN.get());
                    if (!r.player.getInventory().add(gun)) {
                        r.player.drop(gun, false);
                    }
                    VoiceOfTheWorld.announce(r.player, "§6§l[TIẾN HÓA MA VƯƠNG] §eBan tặng Thần Khí: §6§lSúng Lục Hoàng Kim (Golden Gun) §echo Hoàng Sắc Thủy Tổ!");
                }

                // Phát âm thanh và hiệu ứng bùng nổ cuối cùng
                r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.0F);
                r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.8F);
                r.level.playSound(null, r.player.getX(), r.player.getY(), r.player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.2F);

                r.level.sendParticles(ParticleTypes.FLASH, r.player.getX(), r.player.getY() + 1.5D, r.player.getZ(), 2, 0, 0, 0, 0);
                r.level.sendParticles(ParticleTypes.DRAGON_BREATH, r.player.getX(), r.player.getY() + 1.0D, r.player.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.08D);
                r.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, r.player.getX(), r.player.getY() + 1.2D, r.player.getZ(), 25, 0.6D, 0.8D, 0.6D, 0.1D);

                sendTitle(r.player, "§d§l★ CHÂN MA VƯƠNG ★", "§aBáo cáo. Cá thể đã thức tỉnh thành Chân Ma Vương!");
                VoiceOfTheWorld.announceEvolutionSuccess(r.player);

                it.remove();
            }
        }
    }

    private static void sendTitle(ServerPlayer player, String title, String subtitle) {
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }
}
