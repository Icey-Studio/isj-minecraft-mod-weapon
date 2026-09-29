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

            List<EvolvedSkillHelper.AvailableSkill> available = EvolvedSkillHelper.getPlayerAvailableSkills(player);
            EvolvedSkillHelper.AvailableSkill skillA = null;
            EvolvedSkillHelper.AvailableSkill skillB = null;

            for (EvolvedSkillHelper.AvailableSkill s : available) {
                if (s.key().equals(skillKeyA)) skillA = s;
                if (s.key().equals(skillKeyB)) skillB = s;
            }

            if (skillA == null || skillB == null) {
                player.displayClientMessage(Component.literal("§c⚠️ Kỹ năng được chọn không hợp lệ hoặc đã bị tiêu hao!"), true);
                return;
            }

            // Tiêu hao 2 kỹ năng nguyên liệu
            EvolvedSkillHelper.addConsumedSkill(player, skillA.key());
            EvolvedSkillHelper.addConsumedSkill(player, skillB.key());

            // Tính Mức Uy Lực (Tier 1-3)
            int tier = EvolvedSkillHelper.calculateResultTier(skillA.rank(), skillB.rank());

            // Random 1 trong 4 Kỹ Năng Tối Thượng
            int evolvedSkillId = player.getRandom().nextInt(4);
            EvolvedSkillHelper.setEvolvedSkill(player, evolvedSkillId, tier);

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
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(color + "✦ " + skillName + " [Mức " + tier + "] ✦")));

            player.sendSystemMessage(Component.literal("§6══════════════════════════════════════════════════"));
            player.sendSystemMessage(Component.literal("§e⚡ §lDUNG HỢP THÀNH CÔNG KỸ NĂNG TỐI THƯỢNG:"));
            player.sendSystemMessage(Component.literal("   " + color + "▶ " + skillName + " §6§l(CẤP MỨC: " + tier + ")"));
            player.sendSystemMessage(Component.literal("§7• Kỹ năng nguyên liệu đã tiêu hao: §f" + skillA.displayNameVi() + " §7& §f" + skillB.displayNameVi()));
            player.sendSystemMessage(Component.literal("§c⚠️ Kỹ năng tiến hóa sẽ mất hoàn toàn nếu bạn tử vong!"));
            player.sendSystemMessage(Component.literal("§a✚ Hãy dùng kỹ năng [Đại Thánh Tẩy] hoặc đến Ngôi Làng bước vào Cột Sáng Xanh để phục hồi lại các kỹ năng nguyên liệu."));
            player.sendSystemMessage(Component.literal("§6══════════════════════════════════════════════════"));

            // Đồng bộ danh sách kỹ năng đã tiêu hao về máy khách
            ModMessages.sendToPlayer(
                    new ClientboundSyncEvolutionPacket(
                            new java.util.ArrayList<>(EvolvedSkillHelper.getConsumedSkills(player)),
                            evolvedSkillId,
                            tier
                    ), player);
        });
    }
}
