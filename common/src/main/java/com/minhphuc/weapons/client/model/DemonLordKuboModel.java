package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model cho Ma Vương Không Vong (Cosmic Deity Kuubou):
 * - Đầu: Khối cầu vũ trụ đen khổng lồ (Core Sphere) với vành xúc tu squiggly và mắt nhật thực trung tâm.
 * - Thân: Tinh vân xoắn lượn gradient Hồng - Tím - Cam.
 * - Hai càng cua vũ trụ (Crab Claws) vươn ra hai bên.
 * - Bốn xúc tu tinh vân lơ lửng phía dưới không chân (Legless body).
 */
public class DemonLordKuboModel extends EntityModel<DemonLordKuboEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "demon_lord_kubo"), "main");

    public final ModelPart root;
    public final ModelPart coreSphere;
    public final ModelPart eclipseEye;
    public final ModelPart tendrilRing;
    public final ModelPart nebulaTorso;
    public final ModelPart leftClaw;
    public final ModelPart rightClaw;
    public final ModelPart tentacleFrontLeft;
    public final ModelPart tentacleFrontRight;
    public final ModelPart tentacleBackLeft;
    public final ModelPart tentacleBackRight;

    public DemonLordKuboModel(ModelPart root) {
        this.root = root;
        this.coreSphere = root.getChild("core_sphere");
        this.eclipseEye = coreSphere.getChild("eclipse_eye");
        this.tendrilRing = coreSphere.getChild("tendril_ring");

        this.nebulaTorso = root.getChild("nebula_torso");
        this.leftClaw = nebulaTorso.getChild("left_claw");
        this.rightClaw = nebulaTorso.getChild("right_claw");

        this.tentacleFrontLeft = nebulaTorso.getChild("tentacle_fl");
        this.tentacleFrontRight = nebulaTorso.getChild("tentacle_fr");
        this.tentacleBackLeft = nebulaTorso.getChild("tentacle_bl");
        this.tentacleBackRight = nebulaTorso.getChild("tentacle_br");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. KHỐI CẦU VŨ TRỤ ĐEN (16x16x16)
        PartDefinition core = root.addOrReplaceChild("core_sphere",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.0F, -24.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
                        // Lớp viền bo tròn tạo dáng khối cầu sao
                        .texOffs(0, 0).addBox(-7.0F, -25.0F, -7.0F, 14.0F, 18.0F, 14.0F, new CubeDeformation(-0.2F))
                        .texOffs(0, 0).addBox(-9.0F, -23.0F, -7.0F, 18.0F, 14.0F, 14.0F, new CubeDeformation(-0.2F)),
                PartPose.ZERO
        );

        // Con mắt nhật thực lơ lửng ở trung tâm
        core.addOrReplaceChild("eclipse_eye",
                CubeListBuilder.create()
                        .texOffs(32, 32).addBox(-3.5F, -19.5F, -9.2F, 7.0F, 7.0F, 1.5F, new CubeDeformation(0.1F)),
                PartPose.ZERO
        );

        // Vành nhật hoa xúc tu squiggly xung quanh khối cầu
        PartDefinition ring = core.addOrReplaceChild("tendril_ring", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 12; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 12.0);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            ring.addOrReplaceChild("tendril_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 96).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 6.0F, 1.0F),
                    PartPose.offsetAndRotation(cos * 9.5F, -16.0F + sin * 9.5F, 0.0F, 0.0F, 0.0F, angle + 1.57F)
            );
        }

        // 2. THÂN TINH VÂN XOẮN LƯỢN (Nebula Torso)
        PartDefinition torso = root.addOrReplaceChild("nebula_torso",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-5.0F, -8.0F, -4.0F, 10.0F, 10.0F, 8.0F, new CubeDeformation(0.1F))
                        .texOffs(24, 64).addBox(-4.0F, 2.0F, -3.0F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 3. HAI CÀNG CUA VŨ TRỤ (Crab Claws)
        torso.addOrReplaceChild("right_claw",
                CubeListBuilder.create()
                        // Bắp tay vươn sang phải
                        .texOffs(48, 64).addBox(-8.0F, -1.5F, -2.5F, 7.0F, 4.0F, 5.0F)
                        // Khớp càng quặp xuống
                        .texOffs(48, 73).addBox(-11.5F, 2.0F, -2.0F, 5.0F, 8.0F, 4.0F)
                        // Kẹp càng nhọn
                        .texOffs(48, 85).addBox(-10.0F, 9.5F, -1.5F, 3.5F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, -5.0F, 0.0F)
        );

        torso.addOrReplaceChild("left_claw",
                CubeListBuilder.create()
                        // Bắp tay vươn sang trái
                        .texOffs(64, 64).addBox(1.0F, -1.5F, -2.5F, 7.0F, 4.0F, 5.0F)
                        // Khớp càng quặp xuống
                        .texOffs(64, 73).addBox(6.5F, 2.0F, -2.0F, 5.0F, 8.0F, 4.0F)
                        // Kẹp càng nhọn
                        .texOffs(64, 85).addBox(6.5F, 9.5F, -1.5F, 3.5F, 5.0F, 3.0F),
                PartPose.offset(4.0F, -5.0F, 0.0F)
        );

        // 4. BỐN XÚC TU TINH VÂN LƠ LỬNG PHÍA DƯỚI (Legless Nebula Tentacles)
        torso.addOrReplaceChild("tentacle_fl",
                CubeListBuilder.create().texOffs(80, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-2.5F, 7.0F, -2.0F)
        );
        torso.addOrReplaceChild("tentacle_fr",
                CubeListBuilder.create().texOffs(80, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(2.5F, 7.0F, -2.0F)
        );
        torso.addOrReplaceChild("tentacle_bl",
                CubeListBuilder.create().texOffs(80, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-2.5F, 7.0F, 2.0F)
        );
        torso.addOrReplaceChild("tentacle_br",
                CubeListBuilder.create().texOffs(80, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(2.5F, 7.0F, 2.0F)
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(DemonLordKuboEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Lơ lửng nhấp nhô kiểu thần thánh vũ trụ (Levitation Sine Hover)
        float hoverBob = Mth.sin(ageInTicks * 0.1F) * 1.5F;
        this.coreSphere.y = hoverBob;
        this.nebulaTorso.y = hoverBob;

        // Vành xúc tu nhấp nhô gợn sóng quanh khối cầu
        this.tendrilRing.zRot = ageInTicks * 0.02F;

        // Càng cua vũ trụ co bóp nhịp nhàng
        float clawWave = Mth.sin(ageInTicks * 0.12F) * 0.1F;
        this.rightClaw.zRot = 0.1F + clawWave;
        this.leftClaw.zRot = -0.1F - clawWave;

        // Xúc tu tinh vân uốn éo phía dưới
        float tWave1 = Mth.sin(ageInTicks * 0.15F) * 0.25F;
        float tWave2 = Mth.cos(ageInTicks * 0.15F) * 0.25F;
        this.tentacleFrontLeft.xRot = 0.15F + tWave1;
        this.tentacleFrontRight.xRot = 0.15F - tWave1;
        this.tentacleBackLeft.xRot = -0.15F + tWave2;
        this.tentacleBackRight.xRot = -0.15F - tWave2;

        this.tentacleFrontLeft.zRot = tWave2 * 0.5F;
        this.tentacleFrontRight.zRot = -tWave2 * 0.5F;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.coreSphere.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.nebulaTorso.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
