package com.minhphuc.weapons.network;

import com.minhphuc.weapons.client.renderer.SupernovaAccretionRenderer;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class ClientboundSupernovaPacket {
    public static final int PHASE_START = 0;
    public static final int PHASE_END = 1;
    public static final int PHASE_DETONATE = 2;

    private final double x;
    private final double y;
    private final double z;
    private final int phase;

    public ClientboundSupernovaPacket(Vec3 center, int phase) {
        this.x = center.x;
        this.y = center.y;
        this.z = center.z;
        this.phase = phase;
    }

    public ClientboundSupernovaPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.phase = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeInt(this.phase);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (this.phase == PHASE_START) {
                SupernovaAccretionRenderer.receive(new Vec3(this.x, this.y, this.z));
            } else if (this.phase == PHASE_DETONATE) {
                com.minhphuc.weapons.client.renderer.ClientSupernovaPlasmaRenderer.start(new Vec3(this.x, this.y, this.z));
                com.minhphuc.weapons.client.CustomImpactFrameOverlay.trigger();
            } else {
                SupernovaAccretionRenderer.reset();
                com.minhphuc.weapons.client.renderer.ClientSupernovaPlasmaRenderer.reset();
                com.minhphuc.weapons.client.renderer.AtomicRenderer.reset();
            }
        });
    }
}
