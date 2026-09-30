package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VelzardModel extends HumanoidModel<VelzardEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "velzard"), "main");

    // Tóc bạc & vương miện băng tuyết
    public final ModelPart iceTiara;
    public final ModelPart longHairBack;
    public final ModelPart sideHairLeft;
    public final ModelPart sideHairRight;

    // Trang phục dạ hội băng tuyết
    public final ModelPart furMantle;
    public final ModelPart dressSkirt;

    public VelzardModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        this.iceTiara = head.getChild("ice_tiara");
        this.longHairBack = head.getChild("long_hair_back");
        this.sideHairLeft = head.getChild("side_hair_left");
        this.sideHairRight = head.getChild("side_hair_right");

        this.furMantle = body.getChild("fur_mantle");
        this.dressSkirt = body.getChild("dress_skirt");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // --- 1. VƯƠNG MIỆN BĂNG TINH & TÓC BẠC HUYỀN ẢO ---
        // Vương miện pha lê băng nhọn
        head.addOrReplaceChild("ice_tiara", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.5F, 8.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        // Suối tóc bạc dài óng ả phủ qua eo
        head.addOrReplaceChild("long_hair_back", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-4.5F, -1.0F, 3.6F, 9.0F, 20.0F, 2.0F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.06F, 0.0F, 0.0F));

        // Lọn tóc mái hai bên má
        head.addOrReplaceChild("side_hair_left", CubeListBuilder.create()
                .texOffs(16, 8).addBox(3.8F, -1.0F, -3.8F, 1.2F, 9.0F, 1.5F),
                PartPose.ZERO);
        head.addOrReplaceChild("side_hair_right", CubeListBuilder.create()
                .texOffs(0, 8).mirror().addBox(-5.0F, -1.0F, -3.8F, 1.2F, 9.0F, 1.5F),
                PartPose.ZERO);

        // --- 2. ÁO CHOÀNG LÔNG THÚ & ĐẦM DẠ HỘI BĂNG TUYẾT ---
        // Khăn choàng lông thú trắng quanh vai
        body.addOrReplaceChild("fur_mantle", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.5F, -0.2F, -2.5F, 9.0F, 4.0F, 5.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);

        // Váy đầm băng tuyết xoè rộng
        body.addOrReplaceChild("dress_skirt", CubeListBuilder.create()
                .texOffs(0, 42).addBox(-4.5F, 10.0F, -2.5F, 9.0F, 11.0F, 5.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(VelzardEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Chuyển động nhẹ nhàng của suối tóc theo thời gian
        float hairWave = Mth.sin(ageInTicks * 0.08F) * 0.06F;
        this.longHairBack.xRot = 0.08F + hairWave;
        this.sideHairLeft.zRot = Mth.sin(ageInTicks * 0.08F + 1.0F) * 0.03F;
        this.sideHairRight.zRot = -Mth.sin(ageInTicks * 0.08F + 1.0F) * 0.03F;

        // Hoạt ảnh khi bay trên không (Floating / Flying stance)
        if (entity.isFlying()) {
            this.rightLeg.xRot = 0.18F + Mth.sin(ageInTicks * 0.05F) * 0.05F;
            this.leftLeg.xRot = 0.18F - Mth.sin(ageInTicks * 0.05F) * 0.05F;
            this.rightLeg.yRot = 0.05F;
            this.leftLeg.yRot = -0.05F;

            this.rightArm.zRot = 0.25F + Mth.cos(ageInTicks * 0.08F) * 0.05F;
            this.leftArm.zRot = -0.25F - Mth.cos(ageInTicks * 0.08F) * 0.05F;

            // Tóc bay tà tà ra phía sau khi lơ lửng
            this.longHairBack.xRot = 0.24F + hairWave;
        }

        // Hoạt ảnh thi triển Hơi Thở Băng Long
        if (entity.isCastingBreath()) {
            this.head.xRot = -0.15F;
            this.rightArm.xRot = -1.2F;
            this.leftArm.xRot = -1.2F;
            this.rightArm.yRot = -0.2F;
            this.leftArm.yRot = 0.2F;
        }
    }
}
