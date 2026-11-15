package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
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

public class CharybdisModel<T extends CharybdisEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "charybdis"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public CharybdisModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Thân chính + vây lưng sắc nhọn (matches 128x128 UV)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, -6.0F, -11.0F, 14.0F, 12.0F, 22.0F)
                        .texOffs(80, 0).addBox(-1.0F, -12.0F, -7.0F, 2.0F, 6.0F, 6.0F)
                        .texOffs(80, 0).addBox(-1.0F, -11.0F, 2.0F, 2.0F, 5.0F, 6.0F)
                        .texOffs(0, 68).addBox(-8.5F, 3.0F, -7.0F, 4.0F, 9.0F, 4.0F)
                        .texOffs(0, 68).addBox(4.5F, 3.0F, -7.0F, 4.0F, 9.0F, 4.0F)
                        .texOffs(0, 68).addBox(-8.5F, 3.0F, 4.0F, 4.0F, 9.0F, 4.0F)
                        .texOffs(0, 68).addBox(4.5F, 3.0F, 4.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        // Đầu Cyclops khổng lồ với 1 mắt xanh sáng & 3 sừng
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-6.0F, -6.0F, -12.0F, 12.0F, 12.0F, 12.0F)
                        .texOffs(52, 40).addBox(-1.0F, -10.5F, -10.0F, 2.0F, 5.0F, 3.0F, new CubeDeformation(-0.1F))
                        .texOffs(52, 40).addBox(-6.5F, -11.0F, -5.0F, 2.0F, 6.0F, 4.0F)
                        .texOffs(52, 40).addBox(4.5F, -11.0F, -5.0F, 2.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 5.0F, -11.0F));

        // Đuôi dày có vây
        root.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(20, 68).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 14.0F)
                        .texOffs(80, 0).addBox(-1.0F, -6.5F, 3.0F, 2.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 7.0F, 11.0F));

        // Cánh Rồng Khổng Lồ (36px sải cánh mỗi bên)
        root.addOrReplaceChild("rightWing",
                CubeListBuilder.create()
                        .texOffs(60, 68).addBox(-20.0F, -1.0F, -1.0F, 20.0F, 2.0F, 2.0F)
                        .texOffs(0, 88).addBox(-36.0F, 0.0F, -12.0F, 36.0F, 0.1F, 24.0F),
                PartPose.offset(-7.0F, 3.0F, -2.0F));

        root.addOrReplaceChild("leftWing",
                CubeListBuilder.create()
                        .texOffs(60, 68).mirror().addBox(0.0F, -1.0F, -1.0F, 20.0F, 2.0F, 2.0F)
                        .texOffs(0, 88).mirror().addBox(0.0F, 0.0F, -12.0F, 36.0F, 0.1F, 24.0F),
                PartPose.offset(7.0F, 3.0F, -2.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.sin(ageInTicks * 0.22F) * 0.48F;
        this.rightWing.zRot = flap;
        this.leftWing.zRot = -flap;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.6F;
        this.head.xRot = headPitch * ((float) Math.PI / 180F) * 0.5F;
        this.tail.yRot = Mth.cos(ageInTicks * 0.15F) * 0.22F;
        this.tail.xRot = Mth.sin(ageInTicks * 0.15F) * 0.12F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
        head.render(poseStack, buffer, packedLight, packedOverlay, color);
        tail.render(poseStack, buffer, packedLight, packedOverlay, color);
        rightWing.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftWing.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
