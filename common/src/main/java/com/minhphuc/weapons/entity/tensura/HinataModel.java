package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import net.minecraft.client.model.HumanoidModel;
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
 * Model 3D Low-Poly của Hinata Sakaguchi với áo choàng Thánh Kỵ Sĩ trắng viền vàng.
 */
public class HinataModel extends HumanoidModel<HinataEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "hinata"), "main");
    public static final ModelLayerLocation INNER_ARMOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "hinata"), "inner_armor");
    public static final ModelLayerLocation OUTER_ARMOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "hinata"), "outer_armor");

    private final ModelPart holyCape;

    public HinataModel(ModelPart root) {
        super(root);
        this.holyCape = this.body.hasChild("holy_cape") ? this.body.getChild("holy_cape") : null;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("holy_cape",
                CubeListBuilder.create().texOffs(64, 0).addBox(-7.0F, 0.0F, 0.0F, 14.0F, 20.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.5F, 2.2F, 0.14F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createInnerArmorLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0F), 64, 32);
    }

    public static LayerDefinition createOuterArmorLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F), 64, 32);
    }

    @Override
    public void setupAnim(HinataEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (this.holyCape != null) {
            float speedFlap = Mth.clamp(limbSwingAmount * 0.55F, 0.0F, 0.45F);
            this.holyCape.xRot = 0.14F + speedFlap + Mth.sin(ageInTicks * 0.12F) * 0.04F;
        }
    }
}
