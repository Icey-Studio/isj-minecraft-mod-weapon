package com.minhphuc.weapons.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * GÓI TIN ĐỒNG BỘ HIỆU ỨNG ĐIỆN ẢNH & CAMERA CHO TOÀN BỘ KỸ NĂNG (CLIENTBOUND)
 * Truyền tải trạng thái từ Server sang toàn bộ Client theo thời gian thực để kích hoạt:
 * 1. Client OpenGL 3D Geometry Renderer (Không spawn entity rác ở Server).
 * 2. Client Cinematic Camera Cutscene & Widescreen Letterbox HUD.
 */
public class ClientboundCinematicSkillPacket {

    public static final int ACTION_START = 0;
    public static final int ACTION_STOP = 1;
    public static final int ACTION_IMPACT = 2;

    private final int skillId;
    private final UUID casterUuid;
    private final double posX, posY, posZ;
    private final double dirX, dirY, dirZ;
    private final int totalTicks;
    private final int action;
    private final float powerMultiplier;

    public ClientboundCinematicSkillPacket(int skillId, UUID casterUuid, Vec3 pos, Vec3 dir, int totalTicks, int action, float powerMultiplier) {
        this.skillId = skillId;
        this.casterUuid = casterUuid;
        this.posX = pos.x;
        this.posY = pos.y;
        this.posZ = pos.z;
        this.dirX = (dir != null) ? dir.x : 0.0;
        this.dirY = (dir != null) ? dir.y : 1.0;
        this.dirZ = (dir != null) ? dir.z : 0.0;
        this.totalTicks = totalTicks;
        this.action = action;
        this.powerMultiplier = powerMultiplier;
    }

    public ClientboundCinematicSkillPacket(int skillId, UUID casterUuid, Vec3 pos, int totalTicks) {
        this(skillId, casterUuid, pos, new Vec3(0, 1, 0), totalTicks, ACTION_START, 1.0f);
    }

    public ClientboundCinematicSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readInt();
        this.casterUuid = buf.readUUID();
        this.posX = buf.readDouble();
        this.posY = buf.readDouble();
        this.posZ = buf.readDouble();
        this.dirX = buf.readDouble();
        this.dirY = buf.readDouble();
        this.dirZ = buf.readDouble();
        this.totalTicks = buf.readInt();
        this.action = buf.readInt();
        this.powerMultiplier = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.skillId);
        buf.writeUUID(this.casterUuid);
        buf.writeDouble(this.posX);
        buf.writeDouble(this.posY);
        buf.writeDouble(this.posZ);
        buf.writeDouble(this.dirX);
        buf.writeDouble(this.dirY);
        buf.writeDouble(this.dirZ);
        buf.writeInt(this.totalTicks);
        buf.writeInt(this.action);
        buf.writeFloat(this.powerMultiplier);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Vec3 pos = new Vec3(this.posX, this.posY, this.posZ);
            Vec3 dir = new Vec3(this.dirX, this.dirY, this.dirZ);

            if (this.action == ACTION_START) {
                com.minhphuc.weapons.client.renderer.ClientCinematicSkillRenderer.receive(
                        this.skillId, this.casterUuid, pos, dir, this.totalTicks, this.powerMultiplier
                );
                if (this.skillId == com.minhphuc.weapons.config.SkillVisualConfig.SKILL_SPATIAL_MARK) {
                    // dirZ >= 0.5 là đồng bộ ngầm (khi bấm G / đăng nhập / cập nhật lại slot sau khi xóa) -> không chiếu lại góc máy mở ma pháp
                    if (this.dirZ < 0.5) {
                        com.minhphuc.weapons.client.ClientCinematicSkillHandler.startCinematic(
                                this.skillId, this.casterUuid, pos, dir, Math.min(this.totalTicks, 45), this.powerMultiplier
                        );
                    }
                } else if (this.skillId == com.minhphuc.weapons.config.SkillVisualConfig.SKILL_SPATIAL_TELEPORT) {
                    int mode = (int) Math.round(this.dirX);
                    int sub = (int) Math.round(this.dirY);
                    if (mode == 2) {
                        // Chế độ Ma Pháp Trận cố định: chỉ đổi góc quay 35 ticks khi mới tạo vòng trận (sub == 0)
                        if (sub == 0 && !com.minhphuc.weapons.client.ClientCinematicSkillHandler.isActive()) {
                            com.minhphuc.weapons.client.ClientCinematicSkillHandler.startCinematic(
                                    this.skillId, this.casterUuid, pos, dir, 35, this.powerMultiplier
                            );
                        }
                    } else if ((mode == 1 || mode == 3) && sub < 0) {
                        // Gửi sinh vật/vật phẩm tới Ấn Ký -> chỉ vẽ Ma Pháp Trận tại điểm đi / tia sáng ở đích, KHÔNG khóa camera player
                    } else {
                        com.minhphuc.weapons.client.ClientCinematicSkillHandler.startCinematic(
                                this.skillId, this.casterUuid, pos, dir, this.totalTicks, this.powerMultiplier
                        );
                    }
                } else if (this.skillId == com.minhphuc.weapons.config.SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON) {
                    // dirZ >= 1.5 là đồng bộ/gia hạn ngầm lồng giam 3D định kỳ (hoặc hang Veldora) -> KHÔNG đổi góc máy camera
                    if (this.dirZ < 1.5) {
                        com.minhphuc.weapons.client.ClientCinematicSkillHandler.startCinematic(
                                this.skillId, this.casterUuid, pos, dir, Math.min(this.totalTicks, 55), this.powerMultiplier
                        );
                    }
                } else if (this.skillId != com.minhphuc.weapons.config.SkillVisualConfig.SKILL_MULTILAYER_BARRIER
                        && this.skillId != com.minhphuc.weapons.config.SkillVisualConfig.SKILL_DIMENSIONAL_SANCTUARY) {
                    com.minhphuc.weapons.client.ClientCinematicSkillHandler.startCinematic(
                            this.skillId, this.casterUuid, pos, dir, this.totalTicks, this.powerMultiplier
                     );
                }
            } else if (this.action == ACTION_STOP) {
                com.minhphuc.weapons.client.renderer.ClientCinematicSkillRenderer.stop(this.skillId, pos);
                com.minhphuc.weapons.client.ClientCinematicSkillHandler.stopCinematic(this.skillId);
            } else if (this.action == ACTION_IMPACT) {
                com.minhphuc.weapons.client.renderer.ClientCinematicSkillRenderer.triggerImpact(this.skillId, pos);
                com.minhphuc.weapons.client.CustomImpactFrameOverlay.trigger();
            }
        });
    }
}
