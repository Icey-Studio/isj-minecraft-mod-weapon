package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Model cho Đại Ác Ma Marbas (Goetia President Marbas):
 * - Toàn thân trắng toát, cặp sừng cừu khổng lồ xoắn cong trên đầu.
 * - Đôi cánh ác ma trắng dang rộng sau lưng.
 */
public class MarbasModel extends HumanoidModel<MarbasEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "marbas"), "main");

    public final ModelPart ramHorns;
    public final ModelPart wings;

    public MarbasModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        this.ramHorns = head.getChild("ram_horns");
        this.wings = body.getChild("wings");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0.25F), 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // 1. CẶP SỪNG CỪU XOẮN KHỔNG LỒ (RAM HORNS)
        head.addOrReplaceChild("ram_horns", CubeListBuilder.create()
                // Sừng trái vươn lên và xoắn
                .texOffs(0, 32).addBox(-6.0F, -11.0F, -2.0F, 3.0F, 4.0F, 4.0F)
                .texOffs(0, 40).addBox(-9.0F, -14.0F, -1.0F, 3.0F, 5.0F, 3.0F)
                .texOffs(0, 48).addBox(-8.0F, -17.0F, 1.0F, 2.0F, 4.0F, 3.0F)
                // Sừng phải đối xứng
                .texOffs(0, 32).mirror().addBox(3.0F, -11.0F, -2.0F, 3.0F, 4.0F, 4.0F)
                .texOffs(0, 40).mirror().addBox(6.0F, -14.0F, -1.0F, 3.0F, 5.0F, 3.0F)
                .texOffs(0, 48).mirror().addBox(6.0F, -17.0F, 1.0F, 2.0F, 4.0F, 3.0F),
                PartPose.ZERO
        );

        // 2. ĐÔI CÁNH ÁC MA TRẮNG SAU LƯNG (WINGS)
        body.addOrReplaceChild("wings", CubeListBuilder.create()
                // Cánh trái
                .texOffs(32, 32).addBox(-16.0F, -6.0F, 2.5F, 16.0F, 18.0F, 1.0F)
                // Cánh phải
                .texOffs(32, 32).mirror().addBox(0.0F, -6.0F, 2.5F, 16.0F, 18.0F, 1.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
