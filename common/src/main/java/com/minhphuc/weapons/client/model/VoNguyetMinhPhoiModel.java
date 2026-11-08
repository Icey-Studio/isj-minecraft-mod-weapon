package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.darkgathering.VoNguyetMinhPhoiEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model cho Vô Nguyệt Minh Phôi (Cosmic Eyeball Sphere Deity):
 * - Khối cầu vũ trụ đen khổng lồ (Core Sphere 18x18x18) với các lớp bo tròn voxel tạo dáng hình cầu chuẩn xác.
 * - Con mắt tâm hạt nhân rực sáng (Central Core Eye & Pupil) liếc nhìn chuyển động theo mục tiêu.
 * - 16 Ăng-ten xúc tu bức xạ mỏng (Radiating Tendril Antennas) nhô đều quanh chu vi khối cầu tỏa ra ngoài không gian.
 * - Tối ưu hóa tuyệt đối không sinh rác bộ nhớ (Zero-Lag).
 */
public class VoNguyetMinhPhoiModel extends EntityModel<VoNguyetMinhPhoiEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "vo_nguyet_minh_phoi"), "main");

    public final ModelPart root;
    public final ModelPart coreSphere;
    public final ModelPart eyeCore;
    public final ModelPart eyePupil;
    public final ModelPart[] antennas = new ModelPart[16];

    private final float[] baseRotZ = new float[16];
    private final float[] baseRotX = new float[16];

    public VoNguyetMinhPhoiModel(ModelPart root) {
        this.root = root;
        this.coreSphere = root.getChild("core_sphere");
        this.eyeCore = coreSphere.getChild("eye_core");
        this.eyePupil = eyeCore.getChild("eye_pupil");

        for (int i = 0; i < 16; i++) {
            this.antennas[i] = coreSphere.getChild("antenna_" + i);
            this.baseRotZ[i] = this.antennas[i].zRot;
            this.baseRotX[i] = this.antennas[i].xRot;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. KHỐI CẦU VŨ TRỤ ĐEN (Core Sphere 18x18x18 voxel low-poly)
        PartDefinition core = root.addOrReplaceChild("core_sphere",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-9.0F, -9.0F, -9.0F, 18.0F, 18.0F, 18.0F, new CubeDeformation(0.0F))
                        // Lớp viền bo tròn tạo dáng cầu hoàn mỹ
                        .texOffs(0, 0)
                        .addBox(-8.0F, -10.0F, -8.0F, 16.0F, 20.0F, 16.0F, new CubeDeformation(-0.15F))
                        .texOffs(0, 0)
                        .addBox(-10.0F, -8.0F, -8.0F, 20.0F, 16.0F, 16.0F, new CubeDeformation(-0.15F))
                        .texOffs(0, 0)
                        .addBox(-8.0F, -8.0F, -10.0F, 16.0F, 16.0F, 20.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );

        // 2. CON MẮT HẠT NHÂN NHẬT THỰC (Central Core Eye 8x8)
        PartDefinition eye = core.addOrReplaceChild("eye_core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -0.6F, 8.0F, 8.0F, 1.2F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, -9.6F)
        );

        // Con ngươi phát quang trung tâm (Pupil)
        eye.addOrReplaceChild("eye_pupil",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.5F, -1.5F, -0.8F, 3.0F, 3.0F, 1.2F, new CubeDeformation(0.15F)),
                PartPose.ZERO
        );

        // 3. 16 ĂNG-TEN XÚC TU BỨC XẠ (Radiating Tendril Antennas x16)
        // Bố trí đều 360 độ xung quanh đường xích đạo và rìa khối cầu (cứ mỗi 22.5 độ một ăng-ten)
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0D) * Math.PI * 2.0D;
            float px = (float) (Math.cos(angle) * 9.5D);
            float py = (float) (Math.sin(angle) * 9.5D);
            float pz = (i % 2 == 0) ? -1.2F : 1.2F;

            float rotZ = (float) angle - 1.5708F; // Hướng bức xạ thẳng ra phía ngoài tâm
            float rotX = (i % 4 == 0) ? 0.12F : ((i % 4 == 2) ? -0.12F : 0.0F);

            PartDefinition ant = core.addOrReplaceChild("antenna_" + i,
                    CubeListBuilder.create()
                            // Khớp nối chân ăng-ten
                            .texOffs(64, 0)
                            .addBox(-0.8F, 0.0F, -0.6F, 1.6F, 5.0F, 1.2F, new CubeDeformation(0.0F)),
                    PartPose.offsetAndRotation(px, py, pz, rotX, 0.0F, rotZ)
            );

            // Đốt nhọn gợn sóng thứ 2 vươn dài ra không gian
            ant.addOrReplaceChild("antenna_tip_" + i,
                    CubeListBuilder.create()
                            .texOffs(64, 8)
                            .addBox(-0.6F, 4.8F, -0.5F, 1.2F, 4.5F, 1.0F, new CubeDeformation(-0.1F)),
                    PartPose.rotation(0.0F, 0.0F, (i % 2 == 0 ? 0.1F : -0.1F))
            );
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(VoNguyetMinhPhoiEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // 1. Chuyển động lơ lửng nhấp nhô nhẹ nhàng (Levitation Hovering)
        float hoverBob = Mth.sin(ageInTicks * 0.08F) * 1.2F;
        this.coreSphere.y = 12.0F + hoverBob;

        // 2. Con ngươi liếc nhìn theo góc nhìn mục tiêu
        this.eyePupil.x = Mth.clamp(netHeadYaw * 0.035F, -1.8F, 1.8F);
        this.eyePupil.y = Mth.clamp(headPitch * 0.035F, -1.8F, 1.8F);

        // 3. 16 Ăng-ten bức xạ gợn sóng nhịp nhàng như vầng hào quang mặt trời đen
        for (int i = 0; i < 16; i++) {
            float wave = Mth.sin(ageInTicks * 0.12F + (i * 0.3927F)) * 0.08F;
            this.antennas[i].zRot = this.baseRotZ[i] + wave;
            this.antennas[i].xRot = this.baseRotX[i] + Mth.cos(ageInTicks * 0.1F + i) * 0.05F;
        }
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.coreSphere.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
