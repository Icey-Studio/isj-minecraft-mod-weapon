package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
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

/**
 * Mô hình 3D Dạng Rồng Khổng Lồ của Bạo Phong Long Veldora Tempest.
 * Dựa theo bảng thiết kế: Thân rồng phương Tây 4 chân rắn chắc, giáp bụng thép lam,
 * 4 sừng bạc vểnh ngược ra sau, mắt trắng phát sáng và đôi cánh dơi khổng lồ viền xương đen.
 */
public class VeldoraDragonModel extends EntityModel<VeldoraEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "veldora_dragon"), "main");

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

    public VeldoraDragonModel(ModelPart root) {
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

        // Thân rồng thanh thoát, eo gọn, ức ngực nở oai vệ (tỉ lệ chiến long thay vì hình hộp mập)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Eo và sống lưng thon dài (9 x 8.5 x 24)
                        .texOffs(0, 0).addBox(-4.5F, -4.5F, -9.0F, 9.0F, 8.5F, 24.0F, new CubeDeformation(0.0F))
                        // Ức ngực giáp vảy rộng nở phía trước
                        .texOffs(0, 0).addBox(-5.5F, -5.2F, -12.5F, 11.0F, 9.5F, 10.0F, new CubeDeformation(0.1F))
                        // Dãy gai sống lưng sắc nhọn từ cổ tới đuôi
                        .texOffs(60, 92).addBox(-0.8F, -8.5F, -10.5F, 1.6F, 3.8F, 3.0F)
                        .texOffs(60, 92).addBox(-0.8F, -8.2F, -4.5F, 1.6F, 4.0F, 3.0F)
                        .texOffs(60, 92).addBox(-0.8F, -7.8F, 1.5F, 1.6F, 3.6F, 3.0F)
                        .texOffs(60, 92).addBox(-0.8F, -7.2F, 7.5F, 1.6F, 3.2F, 3.0F)
                        .texOffs(60, 92).addBox(-0.8F, -6.6F, 12.5F, 1.6F, 2.8F, 2.5F),
                PartPose.offset(0.0F, 10.5F, 0.0F));

        // Cổ rồng dài vươn cao đầy uy nghi (S-curve)
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-2.8F, -3.0F, -16.0F, 5.6F, 6.0F, 16.5F)
                        .texOffs(60, 92).addBox(-0.7F, -5.5F, -6.5F, 1.4F, 2.8F, 2.8F)
                        .texOffs(60, 92).addBox(-0.7F, -5.5F, -12.0F, 1.4F, 2.8F, 2.8F),
                PartPose.offsetAndRotation(0.0F, -2.5F, -11.8F, -0.45F, 0.0F, 0.0F));

        // Đầu rồng gân guốc, mõm dài & 4 cặp sừng bạc vểnh ngược mạnh mẽ
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-3.8F, -3.5F, -9.5F, 7.6F, 6.2F, 10.0F)
                        .texOffs(48, 40).addBox(-2.6F, -1.8F, -17.0F, 5.2F, 3.4F, 8.0F)
                        // 2 sừng chính dài uốn cong ra sau
                        .texOffs(48, 56).addBox(-3.6F, -5.2F, -2.0F, 1.5F, 1.8F, 12.5F)
                        .texOffs(48, 56).addBox(2.1F, -5.2F, -2.0F, 1.5F, 1.8F, 12.5F)
                        // 2 sừng hàm phụ sắc bén
                        .texOffs(48, 56).addBox(-4.6F, -1.6F, -1.0F, 1.4F, 1.4F, 8.0F)
                        .texOffs(48, 56).addBox(3.2F, -1.6F, -1.0F, 1.4F, 1.4F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -0.8F, -15.2F, 0.45F, 0.0F, 0.0F));

        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(48, 40).addBox(-2.2F, 0.0F, -7.8F, 4.4F, 2.2F, 8.0F),
                PartPose.offset(0.0F, 1.4F, -8.8F));

        // Đôi cánh dơi 2 khớp sải rộng hơn 84px (oai vệ, uốn vòm tự nhiên)
        PartDefinition wingLeft = body.addOrReplaceChild("wing_left",
                CubeListBuilder.create()
                        .texOffs(60, 64).addBox(0.0F, -1.0F, -1.0F, 21.0F, 2.0F, 2.0F)
                        .texOffs(0, 88).addBox(0.0F, 0.0F, 0.5F, 21.0F, 0.15F, 16.0F),
                PartPose.offsetAndRotation(4.5F, -4.2F, -7.5F, 0.0F, -0.08F, -0.35F));
        wingLeft.addOrReplaceChild("wing_outer",
                CubeListBuilder.create()
                        .texOffs(60, 64).addBox(0.0F, -0.8F, -0.8F, 21.0F, 1.6F, 1.6F)
                        .texOffs(0, 88).addBox(0.0F, 0.0F, 0.4F, 21.0F, 0.15F, 17.0F),
                PartPose.offsetAndRotation(21.0F, 0.0F, 0.0F, 0.0F, 0.24F, 0.42F));

        PartDefinition wingRight = body.addOrReplaceChild("wing_right",
                CubeListBuilder.create()
                        .texOffs(60, 64).mirror().addBox(-21.0F, -1.0F, -1.0F, 21.0F, 2.0F, 2.0F)
                        .texOffs(0, 88).mirror().addBox(-21.0F, 0.0F, 0.5F, 21.0F, 0.15F, 16.0F),
                PartPose.offsetAndRotation(-4.5F, -4.2F, -7.5F, 0.0F, 0.08F, 0.35F));
        wingRight.addOrReplaceChild("wing_outer",
                CubeListBuilder.create()
                        .texOffs(60, 64).mirror().addBox(-21.0F, -0.8F, -0.8F, 21.0F, 1.6F, 1.6F)
                        .texOffs(0, 88).mirror().addBox(-21.0F, 0.0F, 0.4F, 21.0F, 0.15F, 17.0F),
                PartPose.offsetAndRotation(-21.0F, 0.0F, 0.0F, 0.0F, -0.24F, -0.42F));

        // 4 chân săn chắc có vai/đùi thon gọn & vuốt trắng
        body.addOrReplaceChild("front_leg_left",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(0, 64).addBox(-1.5F, 5.5F, -1.5F, 3.0F, 8.0F, 3.0F)
                        .texOffs(48, 56).addBox(-1.5F, 12.2F, -3.0F, 3.0F, 1.3F, 2.0F),
                PartPose.offset(4.8F, 0.0F, -8.5F));
        body.addOrReplaceChild("front_leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 64).mirror().addBox(-2.0F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(0, 64).mirror().addBox(-1.5F, 5.5F, -1.5F, 3.0F, 8.0F, 3.0F)
                        .texOffs(48, 56).addBox(-1.5F, 12.2F, -3.0F, 3.0F, 1.3F, 2.0F),
                PartPose.offset(-4.8F, 0.0F, -8.5F));
        body.addOrReplaceChild("hind_leg_left",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-2.0F, -1.5F, -2.5F, 4.0F, 7.5F, 5.0F)
                        .texOffs(0, 64).addBox(-1.5F, 5.5F, -1.2F, 3.0F, 8.0F, 3.0F)
                        .texOffs(48, 56).addBox(-1.5F, 12.2F, -2.8F, 3.0F, 1.3F, 2.0F),
                PartPose.offset(4.8F, 0.0F, 10.5F));
        body.addOrReplaceChild("hind_leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 64).mirror().addBox(-2.0F, -1.5F, -2.5F, 4.0F, 7.5F, 5.0F)
                        .texOffs(0, 64).mirror().addBox(-1.5F, 5.5F, -1.2F, 3.0F, 8.0F, 3.0F)
                        .texOffs(48, 56).addBox(-1.5F, 12.2F, -2.8F, 3.0F, 1.3F, 2.0F),
                PartPose.offset(-4.8F, 0.0F, 10.5F));

        // Đuôi rồng thon dài 2 khúc uyển chuyển (34px)
        PartDefinition tailBase = body.addOrReplaceChild("tail_base",
                CubeListBuilder.create()
                        .texOffs(24, 64).addBox(-2.2F, -2.0F, 0.0F, 4.4F, 4.2F, 16.0F)
                        .texOffs(60, 92).addBox(-0.6F, -4.5F, 4.0F, 1.2F, 2.8F, 2.8F)
                        .texOffs(60, 92).addBox(-0.6F, -4.2F, 10.5F, 1.2F, 2.5F, 2.6F),
                PartPose.offset(0.0F, 0.5F, 14.5F));

        tailBase.addOrReplaceChild("tail_tip",
                CubeListBuilder.create()
                        .texOffs(24, 64).addBox(-1.4F, -1.4F, 0.0F, 2.8F, 2.8F, 18.0F)
                        .texOffs(60, 92).addBox(-0.5F, -3.5F, 7.0F, 1.0F, 2.2F, 3.0F)
                        .texOffs(60, 92).addBox(-0.5F, -2.8F, 14.0F, 1.0F, 2.5F, 4.5F),
                PartPose.offset(0.0F, 0.0F, 15.5F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(VeldoraEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.neck.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.45F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.55F;
        this.head.xRot = 0.45F + headPitch * ((float) Math.PI / 180F) * 0.55F;

        boolean roaring = entity.getCastingState() > 0;
        this.jaw.xRot = roaring ? 0.62F : 0.10F + 0.06F * Mth.sin(ageInTicks * 0.11F);

        boolean sealed = entity.isSealed();
        boolean flying = !sealed && (!entity.onGround() || entity.isFlyingNow());
        float flapSpeed = flying ? 0.30F : 0.08F;
        float flapAmp = flying ? 0.65F : (sealed ? 0.05F : 0.14F);
        float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmp;
        float outerFlap = Mth.sin(ageInTicks * flapSpeed - 0.65F) * (flying ? 0.45F : 0.10F);

        this.wingLeft.zRot = (sealed ? -0.52F : -0.30F) + flap;
        this.wingRight.zRot = (sealed ? 0.52F : 0.30F) - flap;
        this.wingLeftOuter.zRot = (sealed ? 0.85F : 0.42F) + outerFlap;
        this.wingRightOuter.zRot = (sealed ? -0.85F : -0.42F) - outerFlap;

        // Nhịp lượn cơ thể & thu gọn chân khi bay
        this.body.y = flying ? (10.5F + Mth.cos(ageInTicks * flapSpeed) * 1.4F) : 10.5F;
        this.body.xRot = flying ? -0.06F + Mth.sin(ageInTicks * flapSpeed) * 0.05F : 0.0F;

        if (flying) {
            float tuck = 0.52F + 0.10F * Mth.sin(ageInTicks * 0.15F);
            this.frontLegLeft.xRot = tuck;
            this.frontLegRight.xRot = tuck;
            this.hindLegLeft.xRot = tuck + 0.15F;
            this.hindLegRight.xRot = tuck + 0.15F;
        } else {
            this.frontLegLeft.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
            this.hindLegRight.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
            this.frontLegRight.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;
            this.hindLegLeft.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;
        }

        this.tailBase.yRot = Mth.sin(ageInTicks * 0.16F) * 0.24F;
        this.tailBase.xRot = flying ? 0.08F * Mth.cos(ageInTicks * flapSpeed) : -0.08F;
        this.tailTip.yRot = Mth.sin(ageInTicks * 0.16F - 0.9F) * 0.34F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
