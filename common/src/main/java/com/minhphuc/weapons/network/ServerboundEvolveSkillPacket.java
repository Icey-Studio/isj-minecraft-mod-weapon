package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServerboundEvolveSkillPacket {

    private final String skillKeyA;
    private final String skillKeyB;

    public ServerboundEvolveSkillPacket(String skillKeyA, String skillKeyB) {
        this.skillKeyA = skillKeyA;
        this.skillKeyB = skillKeyB;
    }

    public ServerboundEvolveSkillPacket(FriendlyByteBuf buf) {
        this.skillKeyA = buf.readUtf();
        this.skillKeyB = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.skillKeyA);
        buf.writeUtf(this.skillKeyB);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            if (skillKeyA.equals(skillKeyB)) {
                player.displayClientMessage(Component.literal("§c⚠️ Bạn phải chọn 2 kỹ năng khác nhau để dung hợp!"), true);
                return;
            }

            EvolvedSkillHelper.AvailableSkill skillA = EvolvedSkillHelper.getSkillByKey(player, skillKeyA);
            EvolvedSkillHelper.AvailableSkill skillB = EvolvedSkillHelper.getSkillByKey(player, skillKeyB);

            if (skillA == null || skillB == null) {
                player.displayClientMessage(Component.literal("§c⚠️ Kỹ năng được chọn không hợp lệ!"), true);
                return;
            }

            // Kiểm tra và tiêu hao Quyển Thư Tiến Hóa Kỹ Năng nếu không ở chế độ Sáng Tạo
            boolean hasTome = player.isCreative();
            if (!hasTome) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).getItem() instanceof com.minhphuc.weapons.content.evolution.SkillEvolutionTomeItem) {
                        hasTome = true;
                        break;
                    }
                }
            }

            if (!hasTome) {
                player.displayClientMessage(Component.literal("§c⚠️ Bạn cần sở hữu Quyển Thư Tiến Hóa Kỹ Năng để dung hợp!"), true);
                return;
            }

            // Tính Mức Uy Lực tiềm năng (Tier 1-3) dựa vào rank của 2 kỹ năng nguyên liệu
            int potentialTier = EvolvedSkillHelper.calculateResultTier(skillA.rank(), skillB.rank());

            // Lấy danh sách tier hiện tại của cả 4 kỹ năng tối thượng
            int[] currentTiers = EvolvedSkillHelper.getEvolvedSkillTiers(player);
            List<Integer> unmaxed = new ArrayList<>();
            List<Integer> locked = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                if (currentTiers[i] < 3) {
                    unmaxed.add(i);
                }
                if (currentTiers[i] == 0) {
                    locked.add(i);
                }
            }

            if (unmaxed.isEmpty()) {
                player.displayClientMessage(Component.literal("§6§l✦ CẢNH GIỚI TỐI CAO ✦ §fCá thể đã thức tỉnh toàn bộ 4 Kỹ Năng Tối Thượng ở Cấp 3 hoàn mỹ!"), true);
                return;
            }

            // Ưu tiên mở khóa kỹ năng chưa có (locked), sau đó đến kỹ năng chưa max cấp 3
            int evolvedSkillId;
            if (!locked.isEmpty()) {
                evolvedSkillId = locked.get(player.getRandom().nextInt(locked.size()));
            } else {
                evolvedSkillId = unmaxed.get(player.getRandom().nextInt(unmaxed.size()));
            }

            int curTier = currentTiers[evolvedSkillId];
            int newTier = Math.min(3, Math.max(curTier + 1, potentialTier));
            EvolvedSkillHelper.setEvolvedSkillTier(player, evolvedSkillId, newTier);

            // Tiêu hao 1 quyển sách tiến hóa trong túi đồ (nếu không ở Creative)
            if (!player.isCreative()) {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();
                if (main.getItem() instanceof com.minhphuc.weapons.content.evolution.SkillEvolutionTomeItem) {
                    main.shrink(1);
                } else if (off.getItem() instanceof com.minhphuc.weapons.content.evolution.SkillEvolutionTomeItem) {
                    off.shrink(1);
                } else {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = player.getInventory().getItem(i);
                        if (invStack.getItem() instanceof com.minhphuc.weapons.content.evolution.SkillEvolutionTomeItem) {
                            invStack.shrink(1);
                            break;
                        }
                    }
                }
            }

            String skillName = EvolvedSkillHelper.getEvolvedSkillNameVi(evolvedSkillId);
            String color = EvolvedSkillHelper.getEvolvedSkillColor(evolvedSkillId);

            // Âm thanh & hiệu ứng thăng hoa
            ServerLevel level = (ServerLevel) player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 0.8F);

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.5, 1.0, 0.5, 0.4);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 50, 0.6, 0.8, 0.6, 0.1);

            // Gửi Title & Thông báo
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l【 TIẾN HÓA KỸ NĂNG 】")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(color + "✦ " + skillName + " [Cấp " + newTier + "/3] ✦")));

            int[] updatedTiers = EvolvedSkillHelper.getEvolvedSkillTiers(player);
            int maxedCount = 0;
            for (int t : updatedTiers) {
                if (t >= 3) maxedCount++;
            }

            player.sendSystemMessage(Component.literal("§6══════════════════════════════════════════════════"));
            player.sendSystemMessage(Component.literal("§e⚡ §lDUNG HỢP THÀNH CÔNG KỸ NĂNG TỐI THƯỢNG:"));
            player.sendSystemMessage(Component.literal("   " + color + "▶ " + skillName + " §6§l(CẤP ĐỘ: " + newTier + "/3)"));
            player.sendSystemMessage(Component.literal("§a✔ Kỹ năng kết hợp (§f" + skillA.displayNameVi() + " §a& §f" + skillB.displayNameVi() + "§a) KHÔNG bị mất và vẫn sử dụng bình thường!"));
            player.sendSystemMessage(Component.literal("§6★ Tiến độ Tuyệt Kỹ: §e" + maxedCount + "/4 §6kỹ năng đã đạt Cấp 3 tối đa."));
            player.sendSystemMessage(Component.literal("§b• Nhấn phím §f[Z]§b để luân chuyển giữa tất cả kỹ năng thường và kỹ năng tiến hóa!"));
            player.sendSystemMessage(Component.literal("§c⚠️ Kỹ năng tiến hóa sẽ mất nếu bạn tử vong!"));
            player.sendSystemMessage(Component.literal("§6══════════════════════════════════════════════════"));

            // Đồng bộ trạng thái và toàn bộ mảng tiers về máy khách
            ModMessages.sendToPlayer(
                    new ClientboundSyncEvolutionPacket(
                            new java.util.ArrayList<>(),
                            evolvedSkillId,
                            newTier,
                            updatedTiers
                    ), player);
        });
    }
}
