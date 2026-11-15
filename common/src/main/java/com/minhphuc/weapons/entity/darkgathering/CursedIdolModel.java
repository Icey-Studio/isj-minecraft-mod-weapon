package com.minhphuc.weapons.entity.darkgathering;

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

public class CursedIdolModel<T extends CursedIdolEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "cursed_idol"), "main");

    private final ModelPart monolith;

    public CursedIdolModel(ModelPart root) {
        this.monolith = root.getChild("monolith");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("monolith",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -22.0F, -5.0F, 10.0F, 22.0F, 10.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.monolith.yRot = ageInTicks * 0.04F;
        this.monolith.y = 20.5F + Mth.sin(ageInTicks * 0.12F) * 1.5F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        monolith.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
