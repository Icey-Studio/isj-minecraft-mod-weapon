package com.minhphuc.weapons.network;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.content.infinitygauntlet.ServerboundCycleRealitySubModePacket;
import com.minhphuc.weapons.content.infinitygauntlet.ServerboundSelectModePacket;
import com.minhphuc.weapons.content.infinitygauntlet.ServerboundSpaceTeleportPacket;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModMessages {
    private static final NetworkChannel CHANNEL = NetworkChannel.create(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "messages"));

    public static void register() {
        CHANNEL.register(ServerboundSelectModePacket.class,
                ServerboundSelectModePacket::encode,
                ServerboundSelectModePacket::new,
                ServerboundSelectModePacket::handle);

        CHANNEL.register(ServerboundCycleRealitySubModePacket.class,
                ServerboundCycleRealitySubModePacket::encode,
                ServerboundCycleRealitySubModePacket::new,
                ServerboundCycleRealitySubModePacket::handle);

        CHANNEL.register(ClientboundOpenSpaceTeleportScreenPacket.class,
                ClientboundOpenSpaceTeleportScreenPacket::encode,
                ClientboundOpenSpaceTeleportScreenPacket::new,
                ClientboundOpenSpaceTeleportScreenPacket::handle);

        CHANNEL.register(ServerboundSpaceTeleportPacket.class,
                ServerboundSpaceTeleportPacket::encode,
                ServerboundSpaceTeleportPacket::new,
                ServerboundSpaceTeleportPacket::handle);

        CHANNEL.register(ServerboundCastBeelzebuthPacket.class,
                ServerboundCastBeelzebuthPacket::encode,
                ServerboundCastBeelzebuthPacket::new,
                ServerboundCastBeelzebuthPacket::handle);

        CHANNEL.register(ServerboundCycleSkillPacket.class,
                ServerboundCycleSkillPacket::encode,
                ServerboundCycleSkillPacket::new,
                ServerboundCycleSkillPacket::handle);

        CHANNEL.register(ServerboundFireTaisuiStarPacket.class,
                ServerboundFireTaisuiStarPacket::encode,
                ServerboundFireTaisuiStarPacket::new,
                ServerboundFireTaisuiStarPacket::handle);

        CHANNEL.register(ClientboundSyncTaisuiPacket.class,
                ClientboundSyncTaisuiPacket::encode,
                ClientboundSyncTaisuiPacket::new,
                ClientboundSyncTaisuiPacket::handle);

        CHANNEL.register(ServerboundExitLiuRenBarrierPacket.class,
                ServerboundExitLiuRenBarrierPacket::encode,
                ServerboundExitLiuRenBarrierPacket::new,
                ServerboundExitLiuRenBarrierPacket::handle);

        CHANNEL.register(ServerboundSelectBulletPacket.class,
                ServerboundSelectBulletPacket::encode,
                ServerboundSelectBulletPacket::new,
                ServerboundSelectBulletPacket::handle);

        CHANNEL.register(ServerboundSummonDemonPacket.class,
                ServerboundSummonDemonPacket::encode,
                ServerboundSummonDemonPacket::new,
                ServerboundSummonDemonPacket::handle);

        CHANNEL.register(ServerboundDemonCommandPacket.class,
                ServerboundDemonCommandPacket::encode,
                ServerboundDemonCommandPacket::new,
                ServerboundDemonCommandPacket::handle);

        CHANNEL.register(ClientboundOpenCapsuleScreenPacket.class,
                ClientboundOpenCapsuleScreenPacket::encode,
                ClientboundOpenCapsuleScreenPacket::new,
                ClientboundOpenCapsuleScreenPacket::handle);

        CHANNEL.register(ServerboundCapsuleEvolvePacket.class,
                ServerboundCapsuleEvolvePacket::encode,
                ServerboundCapsuleEvolvePacket::new,
                ServerboundCapsuleEvolvePacket::handle);

        CHANNEL.register(ClientboundOpenMaterialCreationPacket.class,
                ClientboundOpenMaterialCreationPacket::encode,
                ClientboundOpenMaterialCreationPacket::new,
                ClientboundOpenMaterialCreationPacket::handle);

        CHANNEL.register(ServerboundMaterialCreationPacket.class,
                ServerboundMaterialCreationPacket::encode,
                ServerboundMaterialCreationPacket::new,
                ServerboundMaterialCreationPacket::handle);

        CHANNEL.register(ServerboundSelectPrimordialRebirthPacket.class,
                ServerboundSelectPrimordialRebirthPacket::encode,
                ServerboundSelectPrimordialRebirthPacket::new,
                ServerboundSelectPrimordialRebirthPacket::handle);

        CHANNEL.register(ServerboundEvolveSkillPacket.class,
                ServerboundEvolveSkillPacket::encode,
                ServerboundEvolveSkillPacket::new,
                ServerboundEvolveSkillPacket::handle);

        CHANNEL.register(ClientboundSyncEvolutionPacket.class,
                ClientboundSyncEvolutionPacket::encode,
                ClientboundSyncEvolutionPacket::new,
                ClientboundSyncEvolutionPacket::handle);

        CHANNEL.register(ClientboundOpenSkillEvolutionScreenPacket.class,
                ClientboundOpenSkillEvolutionScreenPacket::encode,
                ClientboundOpenSkillEvolutionScreenPacket::new,
                ClientboundOpenSkillEvolutionScreenPacket::handle);
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.sendToPlayer(player, message);
    }

    public static <MSG> void sendToPlayers(Iterable<ServerPlayer> players, MSG message) {
        CHANNEL.sendToPlayers(players, message);
    }
}
