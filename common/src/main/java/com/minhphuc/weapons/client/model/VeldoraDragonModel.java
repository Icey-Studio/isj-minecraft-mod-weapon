package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Mô hình 3D Dạng Rồng Khổng Lồ của Bạo Phong Long Veldora Tempest.
 * Dựa theo bảng thiết kế: Thân rồng phương Tây 4 chân rắn chắc, giáp bụng thép lam,
 * 4 sừng bạc vểnh ngược ra sau, mắt trắng phát sáng và đôi cánh dơi khổng lồ viền xương đen.
 */
public class VeldoraDragonModel extends EntityModel<VeldoraEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "veldora_dragon"), "main");

    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart frontLegLeft;
    private final ModelPart frontLegRight;
    private final ModelPart hindLegLeft;
    private final ModelPart hindLegRight;
    private final ModelPart tailBase;
    private final ModelPart tailTip;

    public VeldoraDragonModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.wingLeft = body.getChild("wing_left");
        this.wingRight = body.getChild("wing_right");
        this.frontLegLeft = body.getChild("front_leg_left");
        this.frontLegRight = body.getChild("front_leg_right");
        this.hindLegLeft = body.getChild("hind_leg_left");
        this.hindLegRight = body.getChild("hind_leg_right");
        this.tailBase = body.getChild("tail_base");
        this.tailTip = this.tailBase.getChild("tail_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Thân rồng chính (tỉ lệ oai vệ, khi scale 2.8 sẽ khổng lồ như Ender Dragon)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.0F, -7.0F, -12.0F, 16.0F, 14.0F, 24.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 92).addBox(-1.0F, -10.5F, -9.0F, 2.0F, 4.0F, 4.0F)
                        .texOffs(60, 92).addBox(-1.0F, -11.0F, -2.0F, 2.0F, 4.0F, 4.0F)
                        .texOffs(60, 92).addBox(-1.0F, -10.5F, 5.0F, 2.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        // Cổ rồng dài vươn về trước
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-4.0F, -4.0F, -12.0F, 8.0F, 8.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, -11.0F, -0.25F, 0.0F, 0.0F));

        // Đầu rồng dữ tợn + 4 sừng bạc vểnh ngược ra sau
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-5.0F, -4.5F, -12.0F, 10.0F, 8.0F, 12.0F)
                        .texOffs(48, 40).addBox(-4.0F, -2.5F, -19.0F, 8.0F, 4.0F, 8.0F)
                        // 2 sừng lớn trên
                        .texOffs(48, 56).addBox(-4.5F, -6.5F, -2.0F, 2.0F, 2.0F, 10.0F)
                        .texOffs(48, 56).addBox(2.5F, -6.5F, -2.0F, 2.0F, 2.0F, 10.0F)
                        // 2 sừng phụ hai bên hàm
                        .texOffs(48, 56).addBox(-6.0F, -2.5F, -1.0F, 2.0F, 2.0F, 8.0F)
                        .texOffs(48, 56).addBox(4.0F, -2.5F, -1.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -11.5F, 0.25F, 0.0F, 0.0F));

        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(48, 40).addBox(-3.5F, 0.0F, -8.5F, 7.0F, 3.0F, 9.0F),
                PartPose.offset(0.0F, 1.8F, -9.5F));

        // Cánh dơi khổng lồ trái & phải (xương đen + màng thép lam)
        body.addOrReplaceChild("wing_left",
                CubeListBuilder.create()
                        .texOffs(60, 64).addBox(0.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                        .texOffs(0, 88).addBox(0.0F, 0.0F, 1.0F, 34.0F, 0.1F, 22.0F),
                PartPose.offsetAndRotation(7.5F, -5.0F, -6.0F, 0.0F, -0.1F, -0.25F));

        body.addOrReplaceChild("wing_right",
                CubeListBuilder.create()
                        .texOffs(60, 64).mirror().addBox(-22.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                        .texOffs(0, 88).mirror().addBox(-34.0F, 0.0F, 1.0F, 34.0F, 0.1F, 22.0F),
                PartPose.offsetAndRotation(-7.5F, -5.0F, -6.0F, 0.0F, 0.1F, 0.25F));

        // 4 chân rắn chắc
        body.addOrReplaceChild("front_leg_left",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 13.0F, 5.0F),
                PartPose.offset(6.5F, 0.0F, -8.0F));
        body.addOrReplaceChild("front_leg_right",
                CubeListBuilder.create().texOffs(0, 64).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 13.0F, 5.0F),
                PartPose.offset(-6.5F, 0.0F, -8.0F));
        body.addOrReplaceChild("hind_leg_left",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 13.0F, 5.0F),
                PartPose.offset(6.5F, 0.0F, 8.5F));
        body.addOrReplaceChild("hind_leg_right",
                CubeListBuilder.create().texOffs(0, 64).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 13.0F, 5.0F),
                PartPose.offset(-6.5F, 0.0F, 8.5F));

        // Đuôi rồng 2 đoạn có gai
        PartDefinition tailBase = body.addOrReplaceChild("tail_base",
                CubeListBuilder.create()
                        .texOffs(24, 64).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 14.0F)
                        .texOffs(60, 92).addBox(-1.0F, -5.0F, 4.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 1.5F, 11.5F));

        tailBase.addOrReplaceChild("tail_tip",
                CubeListBuilder.create()
                        .texOffs(24, 64).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 14.0F)
                        .texOffs(60, 92).addBox(-1.0F, -4.5F, 6.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 13.5F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(VeldoraEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.neck.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.45F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.55F;
        this.head.xRot = 0.25F + headPitch * ((float) Math.PI / 180F) * 0.55F;

        boolean roaring = entity.getCastingState() > 0;
        this.jaw.xRot = roaring ? 0.58F : 0.10F + 0.06F * Mth.sin(ageInTicks * 0.1F);

        boolean flying = !entity.onGround() || entity.isFlyingNow();
        float flapSpeed = flying ? 0.36F : 0.08F;
        float flapAmp = flying ? 0.62F : 0.12F;
        float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmp;
        this.wingLeft.zRot = -0.25F + flap;
        this.wingRight.zRot = 0.25F - flap;

        this.frontLegLeft.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
        this.hindLegRight.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
        this.frontLegRight.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;
        this.hindLegLeft.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;

        this.tailBase.yRot = Mth.sin(ageInTicks * 0.14F) * 0.22F;
        this.tailTip.yRot = Mth.sin(ageInTicks * 0.14F - 0.8F) * 0.30F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
