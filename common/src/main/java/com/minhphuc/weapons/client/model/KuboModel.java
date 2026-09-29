package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model cho Không Vong (Kūbō) - Dark Gathering.
 * - Khối cầu mặt trời đen lơ lửng khổng lồ.
 * - Con mắt trung tâm liếc nhìn qua lại và hướng về phía người chơi.
 * - 12 xúc tu / phần phụ uốn lượn liên tục quanh khối cầu (hiệu ứng "lúc nhúc lúc nhúc").
 */
public class KuboModel extends EntityModel<KuboEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "kubo"), "main");

    public final ModelPart root;
    public final ModelPart coreSphere;
    public final ModelPart eyeCenter;
    public final ModelPart eyePupil;
    public final ModelPart[] appendages = new ModelPart[12];

    private final float[] baseZRot = new float[12];
    private final float[] baseXRot = new float[12];
    private final float[] baseYRot = new float[12];

    public KuboModel(ModelPart root) {
        this.root = root;
        this.coreSphere = root.getChild("core_sphere");
        this.eyeCenter = coreSphere.getChild("eye_center");
        this.eyePupil = eyeCenter.getChild("eye_pupil");

        for (int i = 0; i < 12; i++) {
            this.appendages[i] = coreSphere.getChild("appendage_" + i);
            this.baseZRot[i] = this.appendages[i].zRot;
            this.baseXRot[i] = this.appendages[i].xRot;
            this.baseYRot[i] = this.appendages[i].yRot;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Khối cầu mặt trời đen (Core Sphere 18x18x18)
        PartDefinition core = root.addOrReplaceChild("core_sphere",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-9.0F, -9.0F, -9.0F, 18.0F, 18.0F, 18.0F, new CubeDeformation(0.0F))
                        // Lớp viền bo tròn tạo dáng cầu
                        .texOffs(0, 0)
                        .addBox(-8.0F, -10.0F, -8.0F, 16.0F, 20.0F, 16.0F, new CubeDeformation(-0.2F))
                        .texOffs(0, 0)
                        .addBox(-10.0F, -8.0F, -8.0F, 20.0F, 16.0F, 16.0F, new CubeDeformation(-0.2F))
                        .texOffs(0, 0)
                        .addBox(-8.0F, -8.0F, -10.0F, 16.0F, 16.0F, 20.0F, new CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );

        // 2. Con mắt trung tâm (Eye Center 8x8)
        PartDefinition eye = core.addOrReplaceChild("eye_center",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.0F, -4.0F, -0.5F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, -9.6F)
        );

        // Con ngươi liếc nhìn (Eye Pupil 3x3)
        eye.addOrReplaceChild("eye_pupil",
                CubeListBuilder.create()
                        .texOffs(40, 0)
                        .addBox(-1.5F, -1.5F, -0.6F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.15F)),
                PartPose.ZERO
        );

        // 3. 12 Xúc tu / vật thể xung quanh lúc nhúc lúc nhúc (Appendages)
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0D) * Math.PI * 2.0D;
            float px = (float) (Math.cos(angle) * 8.5D);
            float py = (float) (Math.sin(angle) * 8.5D);
            float pz = (i % 2 == 0) ? -2.0F : 2.0F;

            float rotZ = (float) angle;
            float rotX = (i % 3 == 0) ? 0.2F : -0.2F;
            float rotY = (i % 2 == 0) ? 0.3F : -0.3F;

            PartDefinition app = core.addOrReplaceChild("appendage_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 36)
                            .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                    PartPose.offsetAndRotation(px, py, pz, rotX, rotY, rotZ)
            );

            // Đốt thứ 2 của xúc tu để uốn lượn tự nhiên
            app.addOrReplaceChild("tip",
                    CubeListBuilder.create()
                            .texOffs(16, 36)
                            .addBox(-1.0F, 7.5F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
                    PartPose.ZERO
            );
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(KuboEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Nhấp nhô lơ lửng nhẹ nhàng
        this.coreSphere.y = 12.0F + Mth.sin(ageInTicks * 0.08F) * 2.0F;

        // Con mắt ở giữa liếc xung quanh
        float glanceX = Mth.sin(ageInTicks * 0.07F) * 1.8F;
        float glanceY = Mth.cos(ageInTicks * 0.05F) * 1.4F;

        if (entity.getTarget() != null) {
            // Hướng con ngươi nhìn thẳng mục tiêu
            glanceX += Mth.clamp(netHeadYaw * 0.05F, -2.2F, 2.2F);
            glanceY += Mth.clamp(headPitch * 0.04F, -1.8F, 1.8F);
        }
        this.eyePupil.x = glanceX;
        this.eyePupil.y = glanceY;

        // Tốc độ uốn lượn và xoay xúc tu tăng mạnh theo dạng thể
        boolean isUltimateOrComplete = entity.isUltimateOrComplete();
        float speedMult = isUltimateOrComplete ? 1.75F : 1.0F;
        float ampMult = isUltimateOrComplete ? 1.35F : 1.0F;

        // Các vật thể xung quanh uốn lượn lúc nhúc lúc nhúc (Sinusoidal wave writhing animation)
        for (int i = 0; i < 12; i++) {
            float phase = i * 0.52F;
            ModelPart app = this.appendages[i];

            // Dao động uốn éo theo 3 trục tạo chuyển động sống động ghê rợn
            app.zRot = this.baseZRot[i] + Mth.sin(ageInTicks * 0.14F * speedMult + phase) * (0.45F * ampMult);
            app.xRot = this.baseXRot[i] + Mth.cos(ageInTicks * 0.12F * speedMult + phase * 1.3F) * (0.40F * ampMult);
            app.yRot = this.baseYRot[i] + Mth.sin(ageInTicks * 0.16F * speedMult + phase * 0.8F) * (0.35F * ampMult);

            // Đốt đuôi xúc tu
            ModelPart tip = app.getChild("tip");
            tip.zRot = Mth.sin(ageInTicks * 0.20F * speedMult + phase * 1.5F) * (0.50F * ampMult);
            tip.xRot = Mth.cos(ageInTicks * 0.18F * speedMult + phase * 1.2F) * (0.40F * ampMult);
        }
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
