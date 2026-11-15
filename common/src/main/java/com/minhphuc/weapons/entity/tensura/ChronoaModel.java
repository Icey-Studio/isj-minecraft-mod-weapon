package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class ChronoaModel extends HumanoidModel<ChronoaEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "chronoa"), "main");
    public static final ModelLayerLocation INNER_ARMOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "chronoa"), "inner_armor");
    public static final ModelLayerLocation OUTER_ARMOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "chronoa"), "outer_armor");

    public ChronoaModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
    }

    public static LayerDefinition createInnerArmorLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0F), 64, 32);
    }

    public static LayerDefinition createOuterArmorLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F), 64, 32);
    }
}
