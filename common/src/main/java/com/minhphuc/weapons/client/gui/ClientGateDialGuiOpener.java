package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Helper cô lập lệnh mở Giao Diện La Bàn Thần Môn phía Client để tránh lỗi trên Dedicated Server.
 */
public class ClientGateDialGuiOpener {
    public static void open(boolean insideSanctuary, int activeChannel, boolean hasCapsule,
                            List<DimensionalSanctuaryAbility.GateWaypoint> waypoints) {
        Minecraft.getInstance().setScreen(new SanctuaryGateDialScreen(insideSanctuary, activeChannel, hasCapsule, waypoints));
    }
}
