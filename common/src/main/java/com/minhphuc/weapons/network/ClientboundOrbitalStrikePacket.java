package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.minhphuc.weapons.client.renderer.ClientOrbitalStrikeRenderer;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class ClientboundOrbitalStrikePacket {
    public static final int PHASE_START = 0;
    public static final int PHASE_END = 1;

    private final double x;
    private final double y;
    private final double z;
    private final int mode;
    private final int phase;

    public ClientboundOrbitalStrikePacket(Vec3 target, int mode, int phase) {
        this.x = target.x;
        this.y = target.y;
        this.z = target.z;
        this.mode = mode;
        this.phase = phase;
    }

    public ClientboundOrbitalStrikePacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.mode = buf.readInt();
        this.phase = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeInt(this.mode);
        buf.writeInt(this.phase);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (this.phase == PHASE_START) {
                Vec3 target = new Vec3(this.x, this.y, this.z);
                ClientOrbitalStrikeRenderer.receive(target, this.mode);
                int duration = switch (this.mode) {
                    case ClientOrbitalStrikeRenderer.MODE_RAILGUN -> 65;
                    case ClientOrbitalStrikeRenderer.MODE_GUNGNIR -> 75;
                    case ClientOrbitalStrikeRenderer.MODE_SEVEN_STARS -> 100;
                    default -> 70;
                };
                ClientOrbitalCinematicHandler.startCinematic(target, this.mode, duration);
            } else {
                ClientOrbitalStrikeRenderer.reset();
                ClientOrbitalCinematicHandler.stopCinematic();
            }
        });
    }
}
