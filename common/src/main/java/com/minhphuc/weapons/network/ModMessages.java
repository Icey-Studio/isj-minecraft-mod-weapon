package com.minhphuc.weapons.network;

import com.minhphuc.weapons.WeaponsMod;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModMessages {
    private static final NetworkChannel CHANNEL = NetworkChannel.create(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "messages"));

    public static void register() {
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

        CHANNEL.register(ServerboundTengaiSwordSkillPacket.class,
                ServerboundTengaiSwordSkillPacket::encode,
                ServerboundTengaiSwordSkillPacket::new,
                ServerboundTengaiSwordSkillPacket::handle);

        CHANNEL.register(ClientboundTengaiSlashEffectPacket.class,
                ClientboundTengaiSlashEffectPacket::encode,
                ClientboundTengaiSlashEffectPacket::new,
                ClientboundTengaiSlashEffectPacket::handle);

        CHANNEL.register(ServerboundIaiClickPacket.class,
                ServerboundIaiClickPacket::encode,
                ServerboundIaiClickPacket::new,
                ServerboundIaiClickPacket::handle);

        CHANNEL.register(ClientboundIaiStatePacket.class,
                ClientboundIaiStatePacket::encode,
                ClientboundIaiStatePacket::new,
                ClientboundIaiStatePacket::handle);

        CHANNEL.register(ClientboundSyncRykuzinStatePacket.class,
                ClientboundSyncRykuzinStatePacket::encode,
                ClientboundSyncRykuzinStatePacket::new,
                ClientboundSyncRykuzinStatePacket::handle);

        CHANNEL.register(ClientboundNihilityCinematicPacket.class,
                ClientboundNihilityCinematicPacket::encode,
                ClientboundNihilityCinematicPacket::new,
                ClientboundNihilityCinematicPacket::handle);

        CHANNEL.register(ClientboundSupernovaPacket.class,
                ClientboundSupernovaPacket::encode,
                ClientboundSupernovaPacket::new,
                ClientboundSupernovaPacket::handle);

        CHANNEL.register(ClientboundOrbitalStrikePacket.class,
                ClientboundOrbitalStrikePacket::encode,
                ClientboundOrbitalStrikePacket::new,
                ClientboundOrbitalStrikePacket::handle);

        CHANNEL.register(ClientboundAzathothPacket.class,
                ClientboundAzathothPacket::encode,
                ClientboundAzathothPacket::new,
                ClientboundAzathothPacket::handle);

        CHANNEL.register(ClientboundHellflarePacket.class,
                ClientboundHellflarePacket::encode,
                ClientboundHellflarePacket::new,
                ClientboundHellflarePacket::handle);

        CHANNEL.register(ClientboundCinematicSkillPacket.class,
                ClientboundCinematicSkillPacket::encode,
                ClientboundCinematicSkillPacket::new,
                ClientboundCinematicSkillPacket::handle);

        CHANNEL.register(ClientboundOpenGateDialPacket.class,
                ClientboundOpenGateDialPacket::encode,
                ClientboundOpenGateDialPacket::new,
                ClientboundOpenGateDialPacket::handle);

        CHANNEL.register(ServerboundGateDialActionPacket.class,
                ServerboundGateDialActionPacket::encode,
                ServerboundGateDialActionPacket::new,
                ServerboundGateDialActionPacket::handle);

        CHANNEL.register(ServerboundInteractGatePacket.class,
                ServerboundInteractGatePacket::encode,
                ServerboundInteractGatePacket::new,
                ServerboundInteractGatePacket::handle);

        CHANNEL.register(ServerboundAbyssalActionPacket.class,
                ServerboundAbyssalActionPacket::encode,
                ServerboundAbyssalActionPacket::new,
                ServerboundAbyssalActionPacket::handle);

        CHANNEL.register(ClientboundSyncPossessionPacket.class,
                ClientboundSyncPossessionPacket::encode,
                ClientboundSyncPossessionPacket::new,
                ClientboundSyncPossessionPacket::handle);
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
