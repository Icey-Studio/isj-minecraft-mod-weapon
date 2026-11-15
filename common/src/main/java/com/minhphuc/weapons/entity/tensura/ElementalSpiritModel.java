package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Mô hình Tinh Linh Nguyên Tố thanh thoát (lõi tinh thể bay + đôi cánh ánh sáng + hào quang xoay).
 */
public class ElementalSpiritModel<T extends ElementalSpiritEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "elemental_spirit"), "main");

    private final ModelPart core;
    private final ModelPart halo;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ElementalSpiritModel(ModelPart root) {
        this.core = root.getChild("core");
        this.halo = root.getChild("halo");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        root.addOrReplaceChild("halo",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-5.0F, -1.0F, -5.0F, 10.0F, 2.0F, 10.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));

        root.addOrReplaceChild("rightWing",
                CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-8.0F, -4.0F, 0.0F, 8.0F, 9.0F, 1.0F),
                PartPose.offset(-3.0F, 12.0F, 2.0F));

        root.addOrReplaceChild("leftWing",
                CubeListBuilder.create()
                        .texOffs(24, 0).mirror().addBox(0.0F, -4.0F, 0.0F, 8.0F, 9.0F, 1.0F),
                PartPose.offset(3.0F, 12.0F, 2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float hover = Mth.sin(ageInTicks * 0.16F) * 1.4F;
        this.core.y = 13.0F + hover;
        this.halo.y = 6.5F + hover;
        this.halo.yRot = ageInTicks * 0.08F;
        this.rightWing.y = 12.0F + hover;
        this.leftWing.y = 12.0F + hover;
        float flap = Mth.sin(ageInTicks * 0.35F) * 0.5F;
        this.rightWing.yRot = 0.35F + flap;
        this.leftWing.yRot = -0.35F - flap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        core.render(poseStack, buffer, packedLight, packedOverlay, color);
        halo.render(poseStack, buffer, packedLight, packedOverlay, color);
        rightWing.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftWing.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
