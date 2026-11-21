package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.AzathothStomachData;
import com.minhphuc.weapons.content.tensura.SummonVeldoraAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ServerboundAzathothActionPacket {

    public static final int ACTION_OPEN_GUI = 0;
    public static final int ACTION_SWALLOW = 1;
    public static final int ACTION_ANALYZE_UNSEAL_VELDORA = 2;
    public static final int ACTION_RELEASE = 3;
    public static final int ACTION_CONVERT_MAGICULES = 4;
    public static final int ACTION_RECONSTITUTE_ENTITY = 5;
    public static final int ACTION_SUMMON_VELDORA_DRAGON = 6;
    public static final int ACTION_SUMMON_VELDORA_HUMAN = 7;
    public static final int ACTION_RECALL_OR_REVIVE_VELDORA = 8;
    public static final int ACTION_DEPOSIT_SLOT = 9;
    public static final int ACTION_DEPOSIT_BACKPACK = 10;
    public static final int ACTION_TOGGLE_AUTOLOOT = 11;
    public static final int ACTION_REBUILD_VELDORA_CAVE = 12;

    private final int action;
    private final int index;

    public ServerboundAzathothActionPacket(int action, int index) {
        this.action = action;
        this.index = index;
    }

    public ServerboundAzathothActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.index = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(index);
    }

    public static void sendOpenGui(ServerPlayer sp) {
        boolean unlocked = sp.isCreative() || EntityDataHelper.getCustomData(sp).getBoolean(AzathothStomachData.KEY_VELDORA_BEFRIENDED);
        boolean autoLoot = AzathothStomachData.isAutoLootEnabled(sp);
        ModMessages.sendToPlayer(new ClientboundOpenAzathothPacket(AzathothStomachData.getEntries(sp), unlocked, autoLoot), sp);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (!(ctx.get().getPlayer() instanceof ServerPlayer sp)) return;
            var level = sp.serverLevel();
            switch (action) {
                case ACTION_OPEN_GUI -> sendOpenGui(sp);
                case ACTION_SWALLOW -> {
                    AzathothStomachData.swallowTarget(level, sp);
                    sendOpenGui(sp);
                }
                case ACTION_ANALYZE_UNSEAL_VELDORA -> {
                    AzathothStomachData.analyzeOrUnsealVeldora(level, sp);
                    sendOpenGui(sp);
                }
                case ACTION_RELEASE -> {
                    AzathothStomachData.releaseEntry(level, sp, index);
                    sendOpenGui(sp);
                }
                case ACTION_CONVERT_MAGICULES -> {
                    AzathothStomachData.convertToMagicules(level, sp, index);
                    sendOpenGui(sp);
                }
                case ACTION_RECONSTITUTE_ENTITY -> {
                    AzathothStomachData.reconstituteEntity(level, sp, index);
                    sendOpenGui(sp);
                }
                case ACTION_SUMMON_VELDORA_DRAGON -> SummonVeldoraAbility.summon(level, sp, false);
                case ACTION_SUMMON_VELDORA_HUMAN -> SummonVeldoraAbility.summon(level, sp, true);
                case ACTION_RECALL_OR_REVIVE_VELDORA -> SummonVeldoraAbility.recallOrRevive(level, sp);
                case ACTION_DEPOSIT_SLOT -> {
                    AzathothStomachData.depositInventorySlot(sp, index);
                    sendOpenGui(sp);
                }
                case ACTION_DEPOSIT_BACKPACK -> {
                    AzathothStomachData.depositAllBackpack(sp);
                    sendOpenGui(sp);
                }
                case ACTION_TOGGLE_AUTOLOOT -> {
                    AzathothStomachData.toggleAutoLoot(sp);
                    sendOpenGui(sp);
                }
                case ACTION_REBUILD_VELDORA_CAVE -> com.minhphuc.weapons.world.VeldoraWorldData.rebuildCaveForPlayer(level.getServer(), sp);
            }
        });
    }
}
