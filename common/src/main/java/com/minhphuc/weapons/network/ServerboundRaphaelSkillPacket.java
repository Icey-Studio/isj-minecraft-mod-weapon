package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.RaphaelSkillRegistry;
import com.minhphuc.weapons.content.tensura.SkillNullificationAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundRaphaelSkillPacket {

    public static final int ACTION_OPEN = -1;
    private final int skillId;

    public ServerboundRaphaelSkillPacket(int skillId) {
        this.skillId = skillId;
    }

    public ServerboundRaphaelSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(skillId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (!(ctx.get().getPlayer() instanceof ServerPlayer sp)) return;
            if (skillId == ACTION_OPEN) {
                boolean auto = EntityDataHelper.getCustomData(sp).getBoolean("TensuraRaphaelAutoBattle");
                ModMessages.sendToPlayer(new ClientboundOpenRaphaelPacket(auto), sp);
                return;
            }
            if (SkillNullificationAbility.isEntitySkillSealed(sp)) {
                sp.displayClientMessage(Component.literal("§c⚠️ [PHONG SÁT KỸ NĂNG] Kỹ năng Raphael đang tạm bị vô hiệu hóa (còn "
                        + SkillNullificationAbility.getRemainingSealSeconds(sp) + "s)!"), true);
                return;
            }
            RaphaelSkillRegistry.execute(sp.serverLevel(), sp, skillId);
        });
    }
}
