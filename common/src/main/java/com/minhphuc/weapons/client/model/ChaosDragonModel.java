package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.ChaosDragonEntity;
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
 * Mô hình 3D Boss Hỗn Độn Long (Chaos Dragon - Slime Isekai).
 * - Thân rồng Obsidian tím, vảy nứt phát sáng hồng, giáp bụng oải hương trắng.
 * - Gai tinh thể nhọn hoắt mọc dày 2 bên bả vai.
 * - Sừng cong trắng muốt & sừng nhọn giữa sống mũi.
 * - Cánh dơi rách tơi tả lộ xương phát sáng hồng tím.
 * - Đuôi gai kết thúc bằng ngọn lửa neon hồng rực cháy.
 */
public class ChaosDragonModel extends EntityModel<ChaosDragonEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "chaos_dragon"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart wingLeft;
    private final ModelPart wingLeftOuter;
    private final ModelPart wingRight;
    private final ModelPart wingRightOuter;
    private final ModelPart frontLegLeft;
    private final ModelPart frontLegRight;
    private final ModelPart hindLegLeft;
    private final ModelPart hindLegRight;
    private final ModelPart tailBase;
    private final ModelPart tailTip;

    public ChaosDragonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.wingLeft = this.body.getChild("wing_left");
        this.wingLeftOuter = this.wingLeft.getChild("wing_outer");
        this.wingRight = this.body.getChild("wing_right");
        this.wingRightOuter = this.wingRight.getChild("wing_outer");
        this.frontLegLeft = this.body.getChild("front_leg_left");
        this.frontLegRight = this.body.getChild("front_leg_right");
        this.hindLegLeft = this.body.getChild("hind_leg_left");
        this.hindLegRight = this.body.getChild("hind_leg_right");
        this.tailBase = this.body.getChild("tail_base");
        this.tailTip = this.tailBase.getChild("tail_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Thân rồng & Giáp bụng oải hương trắng
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -5.5F, -12.0F, 12.0F, 11.0F, 24.0F)
                        // Giáp ngực / bụng có gân phiến
                        .texOffs(72, 0).addBox(-4.5F, 1.0F, -11.0F, 9.0F, 5.0F, 21.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));

        // Tinh thể gai nhọn 2 bên bả vai
        body.addOrReplaceChild("crystal_spikes_left",
                CubeListBuilder.create()
                        .texOffs(0, 48).addBox(0.0F, -6.0F, -4.0F, 5.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(5.5F, -4.0F, -8.0F, 0.2F, 0.0F, 0.35F));

        body.addOrReplaceChild("crystal_spikes_right",
                CubeListBuilder.create()
                        .texOffs(0, 48).mirror().addBox(-5.0F, -6.0F, -4.0F, 5.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(-5.5F, -4.0F, -8.0F, 0.2F, 0.0F, -0.35F));

        // 2. Cổ & Đầu rồng
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(0, 36).addBox(-3.5F, -3.5F, -16.0F, 7.0F, 7.0F, 17.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -10.5F, -0.28F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-4.0F, -4.0F, -10.0F, 8.0F, 7.0F, 10.0F)
                        .texOffs(48, 64).addBox(-3.0F, -2.0F, -18.0F, 6.0F, 4.0F, 9.0F)
                        // Sừng mũi nhọn hoắt
                        .texOffs(0, 0).addBox(-0.8F, -4.5F, -16.0F, 1.6F, 3.0F, 3.0F)
                        // 2 sừng cong dày màu trắng ngà
                        .texOffs(48, 80).addBox(-4.5F, -7.0F, -2.0F, 2.0F, 3.0F, 14.0F)
                        .texOffs(48, 80).addBox(2.5F, -7.0F, -2.0F, 2.0F, 3.0F, 14.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -15.0F, 0.42F, 0.0F, 0.0F));

        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(48, 64).addBox(-2.5F, 0.0F, -8.0F, 5.0F, 2.5F, 8.0F),
                PartPose.offset(0.0F, 1.5F, -9.0F));

        // 3. Đôi cánh rách tả tơi
        PartDefinition wingLeft = body.addOrReplaceChild("wing_left",
                CubeListBuilder.create()
                        .texOffs(80, 48).addBox(0.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                        .texOffs(0, 96).addBox(0.0F, 0.0F, 0.5F, 22.0F, 0.15F, 18.0F),
                PartPose.offsetAndRotation(5.0F, -4.0F, -7.0F, 0.0F, -0.08F, -0.32F));
        wingLeft.addOrReplaceChild("wing_outer",
                CubeListBuilder.create()
                        .texOffs(80, 48).addBox(0.0F, -0.8F, -0.8F, 22.0F, 1.6F, 1.6F)
                        .texOffs(0, 96).addBox(0.0F, 0.0F, 0.4F, 22.0F, 0.15F, 19.0F),
                PartPose.offsetAndRotation(22.0F, 0.0F, 0.0F, 0.0F, 0.22F, 0.40F));

        PartDefinition wingRight = body.addOrReplaceChild("wing_right",
                CubeListBuilder.create()
                        .texOffs(80, 48).mirror().addBox(-22.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                        .texOffs(0, 96).mirror().addBox(-22.0F, 0.0F, 0.5F, 22.0F, 0.15F, 18.0F),
                PartPose.offsetAndRotation(-5.0F, -4.0F, -7.0F, 0.0F, 0.08F, 0.32F));
        wingRight.addOrReplaceChild("wing_outer",
                CubeListBuilder.create()
                        .texOffs(80, 48).mirror().addBox(-22.0F, -0.8F, -0.8F, 22.0F, 1.6F, 1.6F)
                        .texOffs(0, 96).mirror().addBox(-22.0F, 0.0F, 0.4F, 22.0F, 0.15F, 19.0F),
                PartPose.offsetAndRotation(-22.0F, 0.0F, 0.0F, 0.0F, -0.22F, -0.40F));

        // 4. 4 Chân rồng săn chắc
        body.addOrReplaceChild("front_leg_left",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                        .texOffs(0, 64).addBox(-2.0F, 6.5F, -2.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(5.5F, 0.0F, -8.0F));
        body.addOrReplaceChild("front_leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 64).mirror().addBox(-2.5F, -1.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                        .texOffs(0, 64).mirror().addBox(-2.0F, 6.5F, -2.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(-5.5F, 0.0F, -8.0F));
        body.addOrReplaceChild("hind_leg_left",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-2.5F, -1.5F, -3.0F, 5.0F, 8.5F, 6.0F)
                        .texOffs(0, 64).addBox(-2.0F, 6.5F, -1.5F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(5.5F, 0.0F, 10.0F));
        body.addOrReplaceChild("hind_leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 64).mirror().addBox(-2.5F, -1.5F, -3.0F, 5.0F, 8.5F, 6.0F)
                        .texOffs(0, 64).mirror().addBox(-2.0F, 6.5F, -1.5F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(-5.5F, 0.0F, 10.0F));

        // 5. Đuôi gai kết thúc bằng ngọn lửa neon
        PartDefinition tailBase = body.addOrReplaceChild("tail_base",
                CubeListBuilder.create()
                        .texOffs(32, 36).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 18.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 11.5F, -0.15F, 0.0F, 0.0F));

        PartDefinition tailTip = tailBase.addOrReplaceChild("tail_tip",
                CubeListBuilder.create()
                        .texOffs(32, 36).addBox(-1.8F, -1.8F, 0.0F, 3.6F, 3.6F, 18.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 17.5F, 0.12F, 0.0F, 0.0F));

        tailTip.addOrReplaceChild("flame_tip",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -3.0F, 17.0F, 5.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ChaosDragonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.neck.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.4F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.6F;
        this.head.xRot = headPitch * ((float) Math.PI / 180F) * 0.6F;

        // Vỗ cánh uyển chuyển
        float flap = Mth.sin(ageInTicks * 0.15F);
        this.wingLeft.zRot = -0.32F + flap * 0.45F;
        this.wingLeftOuter.zRot = 0.40F + flap * 0.35F;
        this.wingRight.zRot = 0.32F - flap * 0.45F;
        this.wingRightOuter.zRot = -0.40F - flap * 0.35F;

        // Đung đưa đuôi
        float tailWag = Mth.cos(ageInTicks * 0.08F);
        this.tailBase.yRot = tailWag * 0.22F;
        this.tailTip.yRot = tailWag * 0.35F;

        // Đi bộ
        this.frontLegLeft.xRot = Mth.cos(limbSwing * 0.6F) * 0.6F * limbSwingAmount;
        this.frontLegRight.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.6F * limbSwingAmount;
        this.hindLegLeft.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.6F * limbSwingAmount;
        this.hindLegRight.xRot = Mth.cos(limbSwing * 0.6F) * 0.6F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
