package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.AbyssalGovernanceManager;
import com.minhphuc.weapons.content.tensura.PrimordialPactItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ServerboundAbyssalActionPacket {

    public static final int ACTION_SENSORY_SCAN = 1;
    public static final int ACTION_PORTAL_WARP = 2;
    public static final int ACTION_REMOTE_EXTRACT = 3;
    public static final int ACTION_TOGGLE_AUTO_HOOVER = 4;
    public static final int ACTION_VILLAGE_PROTECTORATE = 5;
    public static final int ACTION_GUY_GAMBIT = 6;
    public static final int ACTION_WARP_HOME = 7;

    private final int action;
    private final int extraParam;

    public ServerboundAbyssalActionPacket(int action, int extraParam) {
        this.action = action;
        this.extraParam = extraParam;
    }

    public ServerboundAbyssalActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
        this.extraParam = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.action);
        buf.writeInt(this.extraParam);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                ItemStack pact = player.getMainHandItem();
                if (!(pact.getItem() instanceof PrimordialPactItem)) {
                    pact = player.getOffhandItem();
                }
                if (!(pact.getItem() instanceof PrimordialPactItem)) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack st = player.getInventory().getItem(i);
                        if (st.getItem() instanceof PrimordialPactItem) {
                            pact = st;
                            break;
                        }
                    }
                }

                if (pact.isEmpty() || !(pact.getItem() instanceof PrimordialPactItem)) {
                    return;
                }

                switch (this.action) {
                    case ACTION_SENSORY_SCAN -> AbyssalGovernanceManager.activateSensoryLink(player, pact);
                    case ACTION_PORTAL_WARP -> AbyssalGovernanceManager.findAndOpenPortal(player, pact);
                    case ACTION_REMOTE_EXTRACT -> AbyssalGovernanceManager.remoteExtractOrMine(player, pact);
                    case ACTION_TOGGLE_AUTO_HOOVER -> AbyssalGovernanceManager.toggleAutoHoover(player, pact);
                    case ACTION_VILLAGE_PROTECTORATE -> AbyssalGovernanceManager.establishVillageProtectorate(player, pact);
                    case ACTION_GUY_GAMBIT -> AbyssalGovernanceManager.claimGuyGambit(player, pact, this.extraParam);
                    case ACTION_WARP_HOME -> AbyssalGovernanceManager.warpHome(player, pact);
                }
            }
        });
    }
}
