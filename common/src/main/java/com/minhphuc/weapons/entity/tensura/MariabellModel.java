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

/**
 * 3D Low-Poly Voxel Model của Tiểu Thư Mariabell Rosso (Slime Isekai).
 * - Dáng thiếu nữ quý tộc nhỏ nhắn (cao 1.5 blocks).
 * - Tóc vàng óng mái bằng xõa dài ngang lưng.
 * - Đầm dạ hội Victoria màu đỏ rượu vang burgundy với nơ ren đen và trâm cài ngực sao vàng.
 * - Tay Alex slim (3px).
 */
public class MariabellModel extends HumanoidModel<MariabellEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "mariabell"), "main");

    private final ModelPart longHair;
    private final ModelPart skirt;
    private final ModelPart chestRibbon;

    public MariabellModel(ModelPart root) {
        super(root);
        this.longHair = this.head.hasChild("long_hair") ? this.head.getChild("long_hair") : null;
        this.skirt = this.body.hasChild("skirt") ? this.body.getChild("skirt") : null;
        this.chestRibbon = this.body.hasChild("chest_ribbon") ? this.body.getChild("chest_ribbon") : null;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // 1. Đầu & Tóc dài thẳng mái bằng phía sau
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("long_hair",
                CubeListBuilder.create().texOffs(24, 0).addBox(-4.0F, 0.0F, 3.5F, 8.0F, 12.0F, 1.5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 2. Thân đầm Victoria, áo khoác đỏ rượu vang, nơ ngực & gấu váy bèo trắng
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("chest_ribbon",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, 1.0F, -2.5F, 6.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(16, 36).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 6.0F, 5.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 3. Tay Alex slim (3px rộng)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
