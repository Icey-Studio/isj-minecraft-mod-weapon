package com.minhphuc.weapons.client;

import com.minhphuc.weapons.entity.tensura.DemonType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientPossessionHandler {

    public record PossessionData(
            boolean isPossessing,
            DemonType demonType,
            boolean hasBody,
            boolean isNamed,
            String customName,
            int tier,
            boolean isWinged,
            float currentHp,
            float maxHp,
            float attackDamage,
            int selectedSkillIndex,
            String skillsString
    ) {}

    private static final Map<UUID, PossessionData> POSSESSION_MAP = new HashMap<>();

    public static void handlePacket(UUID playerUuid, boolean isPossessing, int demonTypeOrdinal,
                                    boolean hasBody, boolean isNamed, String customName,
                                    int tier, boolean isWinged, float currentHp, float maxHp,
                                    float attackDamage, int selectedSkillIndex, String skillsString) {
        if (!isPossessing) {
            POSSESSION_MAP.remove(playerUuid);
        } else {
            DemonType type = DemonType.byIndex(demonTypeOrdinal);
            PossessionData data = new PossessionData(
                    true, type, hasBody, isNamed, customName, tier, isWinged,
                    currentHp, maxHp, attackDamage, selectedSkillIndex, skillsString
            );
            POSSESSION_MAP.put(playerUuid, data);
        }
    }

    public static boolean isPossessing(Player player) {
        if (player == null) return false;
        PossessionData data = POSSESSION_MAP.get(player.getUUID());
        return data != null && data.isPossessing();
    }

    public static boolean isLocalPlayerPossessing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return isPossessing(mc.player);
    }

    public static PossessionData getData(Player player) {
        if (player == null) return null;
        return POSSESSION_MAP.get(player.getUUID());
    }

    public static PossessionData getLocalData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return getData(mc.player);
    }
}
