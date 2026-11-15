package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class UnderworldDemonModel extends EntityModel<UnderworldDemonEntity> {

    public static final ModelLayerLocation HUMANOID_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "underworld_humanoid"), "main");
    public static final ModelLayerLocation BEAST_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "underworld_beast"), "main");
    public static final ModelLayerLocation FIEND_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "underworld_fiend"), "main");

    private final UnderworldDemonEntity.DemonMorph morph;
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingOuter;
    private final ModelPart rightWingOuter;
    private final ModelPart armorPlate;

    public UnderworldDemonModel(ModelPart root, UnderworldDemonEntity.DemonMorph morph) {
        this.root = root;
        this.morph = morph;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.tail = body.hasChild("tail") ? body.getChild("tail") : null;
        this.leftWing = body.hasChild("left_wing") ? body.getChild("left_wing") : null;
        this.rightWing = body.hasChild("right_wing") ? body.getChild("right_wing") : null;
        this.leftWingOuter = (leftWing != null && leftWing.hasChild("wing_outer")) ? leftWing.getChild("wing_outer") : null;
        this.rightWingOuter = (rightWing != null && rightWing.hasChild("wing_outer")) ? rightWing.getChild("wing_outer") : null;
        this.armorPlate = body.hasChild("armor_plate") ? body.getChild("armor_plate") : null;
    }

    public static LayerDefinition createHumanoidLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -7.0F, -1.0F, -0.35F, 0.0F, -0.35F));
        head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -7.0F, -1.0F, -0.35F, 0.0F, 0.35F));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("armor_plate",
                CubeListBuilder.create().texOffs(0, 46).addBox(-4.5F, 0.5F, -2.5F, 9.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -0.35F, 0.0F, 0.0F));
        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(24, 32).addBox(-12.0F, -4.0F, 0.0F, 12.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 3.0F, 2.2F, 0.1F, 0.45F, 0.0F));
        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(24, 32).mirror().addBox(0.0F, -4.0F, 0.0F, 12.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 3.0F, 2.2F, 0.1F, -0.45F, 0.0F));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(56, 16).addBox(-2.0F, 10.0F, -2.0F, 1.0F, 3.0F, 2.0F),
                PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(56, 16).mirror().addBox(1.0F, 10.0F, -2.0F, 1.0F, 3.0F, 2.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createBeastLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -6.0F, -8.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 9.0F, -5.0F));
        head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(-4.0F, -5.0F, -2.0F, -0.55F, 0.0F, -0.45F));
        head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(4.0F, -5.0F, -2.0F, -0.55F, 0.0F, 0.45F));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 11.5F, 1.0F, 0.28F, 0.0F, 0.0F));
        body.addOrReplaceChild("spine",
                CubeListBuilder.create().texOffs(45, 28).addBox(-1.0F, -3.0F, -3.5F, 2.0F, 3.0F, 7.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(34, 39).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, -0.25F, 0.0F, 0.0F));
        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(45, 18).addBox(-8.0F, -4.0F, 0.0F, 8.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(-3.5F, -2.5F, -1.0F, 0.3F, 0.5F, 0.0F));
        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(45, 18).mirror().addBox(0.0F, -4.0F, 0.0F, 8.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(3.5F, -2.5F, -1.0F, 0.3F, -0.5F, 0.0F));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 39).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(-5.5F, 10.0F, -4.0F));
        rightArm.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(0, 57).addBox(-1.0F, 12.0F, -3.5F, 2.0F, 4.0F, 3.0F),
                PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 39).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(5.5F, 10.0F, -4.0F));
        leftArm.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(0, 57).mirror().addBox(-1.0F, 12.0F, -3.5F, 2.0F, 4.0F, 3.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(16, 39).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 10.0F, 5.0F),
                PartPose.offset(-3.5F, 14.0F, 5.5F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 39).mirror().addBox(-2.0F, 0.0F, -2.5F, 4.0F, 10.0F, 5.0F),
                PartPose.offset(3.5F, 14.0F, 5.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createFiendLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 9.0F, 8.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));
        head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-3.2F, -8.0F, -1.0F, -0.25F, 0.0F, -0.40F));
        head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(3.2F, -8.0F, -1.0F, -0.25F, 0.0F, 0.40F));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 17).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 13.0F, 6.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));
        body.addOrReplaceChild("tasset",
                CubeListBuilder.create().texOffs(32, 17).addBox(-5.0F, 13.0F, -3.0F, 10.0F, 6.0F, 6.0F),
                PartPose.ZERO);

        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(32, 36).addBox(-10.0F, -5.0F, 0.0F, 10.0F, 12.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 3.0F, 3.2F, 0.08F, 0.42F, 0.0F));
        rightWing.addOrReplaceChild("wing_outer",
                CubeListBuilder.create().texOffs(32, 49).addBox(-12.0F, -6.0F, 0.0F, 12.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(-10.0F, 0.0F, 0.0F, 0.0F, 0.25F, -0.10F));

        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(32, 36).mirror().addBox(0.0F, -5.0F, 0.0F, 10.0F, 12.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, 3.0F, 3.2F, 0.08F, -0.42F, 0.0F));
        leftWing.addOrReplaceChild("wing_outer",
                CubeListBuilder.create().texOffs(32, 49).mirror().addBox(0.0F, -6.0F, 0.0F, 12.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(10.0F, 0.0F, 0.0F, 0.0F, -0.25F, 0.10F));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 36).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(-7.0F, 1.0F, 0.0F));
        rightArm.addOrReplaceChild("pauldron_r",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, -3.5F, -3.0F, 5.0F, 5.0F, 6.0F),
                PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 36).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(7.0F, 1.0F, 0.0F));
        leftArm.addOrReplaceChild("pauldron_l",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.0F, -3.5F, -3.0F, 5.0F, 5.0F, 6.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(16, 36).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.5F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 36).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.5F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(UnderworldDemonEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);

        if (this.armorPlate != null) {
            this.armorPlate.visible = entity.hasPhysicalBody();
        }

        boolean showWings = entity.isWinged() || entity.getRank().innateWings
                || entity.getMorph() == UnderworldDemonEntity.DemonMorph.WINGED_FIEND || entity.isDemonLord();
        if (this.leftWing != null && this.rightWing != null) {
            this.leftWing.visible = showWings;
            this.rightWing.visible = showWings;
            if (showWings) {
                float flapSpeed = entity.isFlyingNow() ? 0.34F : 0.08F;
                float flapAmp = entity.isFlyingNow() ? 0.65F : 0.14F;
                float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmp;
                this.rightWing.yRot = 0.45F + flap;
                this.leftWing.yRot = -0.45F - flap;
                if (this.rightWingOuter != null && this.leftWingOuter != null) {
                    this.rightWingOuter.yRot = 0.25F + flap * 0.6F;
                    this.leftWingOuter.yRot = -0.25F - flap * 0.6F;
                }
            }
        }

        if (this.tail != null) {
            this.tail.yRot = Mth.cos(ageInTicks * 0.14F) * 0.28F;
        }

        if (entity.isOrderedToSit()) {
            this.rightLeg.xRot = -1.2F;
            this.leftLeg.xRot = -1.2F;
            this.rightArm.xRot = -0.5F;
            this.leftArm.xRot = -0.5F;
            return;
        }

        if (morph == UnderworldDemonEntity.DemonMorph.BEAST) {
            // Dáng dị thú vồ mồi 4 chi
            this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount - 0.18F;
            this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.1F * limbSwingAmount - 0.18F;
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.0F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbSwingAmount;
        } else if (entity.isFlyingNow()) {
            // Tư thế bay lượn trên không
            this.rightLeg.xRot = 0.35F + Mth.sin(ageInTicks * 0.12F) * 0.08F;
            this.leftLeg.xRot = 0.42F - Mth.sin(ageInTicks * 0.12F) * 0.08F;
            this.rightArm.xRot = -0.25F;
            this.leftArm.xRot = -0.25F;
        } else {
            this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbSwingAmount;
        }

        // Vung tay chém hoặc giơ tay thi triển ma pháp
        if (this.attackTime > 0.0F) {
            float swing = Mth.sin(this.attackTime * (float) Math.PI);
            this.rightArm.xRot = -1.6F * swing;
            this.leftArm.xRot = -0.9F * swing;
        } else if (entity.isCastingMagic()) {
            this.rightArm.xRot = -1.75F + Mth.sin(ageInTicks * 0.4F) * 0.15F;
            this.leftArm.xRot = -1.75F - Mth.sin(ageInTicks * 0.4F) * 0.15F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
