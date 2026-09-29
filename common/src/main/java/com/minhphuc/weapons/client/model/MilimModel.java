package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.tensura.MilimEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MilimModel extends HumanoidModel<MilimEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "milim"), "main");

    // Tóc Milim
    public final ModelPart twintailLeft;
    public final ModelPart twintailRight;
    public final ModelPart frontBangs;
    public final ModelPart sideHairLeft;
    public final ModelPart sideHairRight;

    // Cánh rồng Dragonoid sau lưng
    public final ModelPart leftDragonWing;
    public final ModelPart rightDragonWing;

    // Vóc dáng nữ tính & Croptop Bikini
    public final ModelPart femaleBust;

    public MilimModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        this.twintailLeft = head.getChild("twintail_left");
        this.twintailRight = head.getChild("twintail_right");
        this.frontBangs = head.getChild("front_bangs");
        this.sideHairLeft = head.getChild("side_hair_left");
        this.sideHairRight = head.getChild("side_hair_right");

        this.leftDragonWing = body.getChild("left_dragon_wing");
        this.rightDragonWing = body.getChild("right_dragon_wing");
        this.femaleBust = body.getChild("female_bust");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // --- 1. TÓC HỒNG HAI BÍM MILIM (TWINTAILS) ---
        // Bím tóc trái vươn từ đỉnh đầu rủ dài xuống quá thắt lưng
        head.addOrReplaceChild("twintail_left", CubeListBuilder.create()
                // Nơ buộc tóc màu sẫm
                .texOffs(48, 0).addBox(3.5F, -8.0F, -0.5F, 2.0F, 2.0F, 2.0F)
                // Dải tóc bím dài uốn lượn
                .texOffs(48, 4).addBox(4.0F, -6.5F, 0.0F, 2.2F, 18.0F, 2.2F, new CubeDeformation(0.08F)),
                PartPose.rotation(0.05F, 0.0F, -0.15F));

        // Bím tóc phải đối xứng
        head.addOrReplaceChild("twintail_right", CubeListBuilder.create()
                .texOffs(48, 0).mirror().addBox(-5.5F, -8.0F, -0.5F, 2.0F, 2.0F, 2.0F)
                .texOffs(48, 4).mirror().addBox(-6.2F, -6.5F, 0.0F, 2.2F, 18.0F, 2.2F, new CubeDeformation(0.08F)),
                PartPose.rotation(0.05F, 0.0F, 0.15F));

        // Tóc mái trước trán
        head.addOrReplaceChild("front_bangs", CubeListBuilder.create()
                .texOffs(8, 8).addBox(-4.0F, -8.1F, -4.2F, 8.0F, 3.5F, 0.5F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        // Lọn tóc mai hai bên má
        head.addOrReplaceChild("side_hair_left", CubeListBuilder.create()
                .texOffs(16, 8).addBox(3.8F, -6.0F, -3.8F, 0.8F, 8.0F, 1.5F),
                PartPose.ZERO);
        head.addOrReplaceChild("side_hair_right", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-4.6F, -6.0F, -3.8F, 0.8F, 8.0F, 1.5F),
                PartPose.ZERO);

        // --- 2. ĐÔI CÁNH RỒNG DRAGONOID MINI SAU LƯNG ---
        body.addOrReplaceChild("left_dragon_wing", CubeListBuilder.create()
                .texOffs(0, 32).addBox(0.0F, -1.0F, 2.2F, 8.0F, 10.0F, 0.8F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.1F, 0.35F, 0.0F));

        body.addOrReplaceChild("right_dragon_wing", CubeListBuilder.create()
                .texOffs(0, 32).mirror().addBox(-8.0F, -1.0F, 2.2F, 8.0F, 10.0F, 0.8F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.1F, -0.35F, 0.0F));

        // --- 3. VÓC DÁNG NỮ TÍNH & BIKINI CROPTOP ---
        body.addOrReplaceChild("female_bust", CubeListBuilder.create()
                .texOffs(48, 40).addBox(-3.2F, 2.2F, -2.6F, 6.4F, 3.2F, 1.6F, new CubeDeformation(0.02F)),
                PartPose.rotation(0.05F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(MilimEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Chuyển động đung đưa tự nhiên của bím tóc
        float hairSway = Mth.sin(ageInTicks * 0.08F) * 0.08F;
        this.twintailLeft.zRot = -0.15F + hairSway;
        this.twintailRight.zRot = 0.15F - hairSway;
        this.twintailLeft.xRot = 0.05F + Mth.cos(ageInTicks * 0.07F) * 0.05F;
        this.twintailRight.xRot = 0.05F + Mth.cos(ageInTicks * 0.07F) * 0.05F;

        // Chuyển động vỗ cánh rồng Dragonoid
        float wingFlap = Mth.sin(ageInTicks * 0.25F) * 0.35F;
        this.leftDragonWing.yRot = 0.35F + wingFlap;
        this.rightDragonWing.yRot = -0.35F - wingFlap;

        // Trạng thái kỹ năng
        int castState = entity.getCastingState();
        if (castState == 1) {
            // 1: Tụ lực Long Tinh Bộc Viêm Bá (Hai tay chụm lại đẩy ra trước, bay lơ lửng, cánh giương rộng)
            this.rightArm.xRot = -1.5F;
            this.rightArm.yRot = -0.3F;
            this.rightArm.zRot = 0.2F;

            this.leftArm.xRot = -1.5F;
            this.leftArm.yRot = 0.3F;
            this.leftArm.zRot = -0.2F;

            this.leftDragonWing.yRot = 0.75F;
            this.rightDragonWing.yRot = -0.75F;

            this.head.xRot = -0.2F;
        } else if (castState == 2) {
            // 2: Bắn cột sáng ngang (Một tay đưa thẳng về trước chỉ vào đối thủ)
            this.rightArm.xRot = -1.55F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;

            this.leftArm.xRot = 0.3F;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = -0.4F; // Tay trái chống hông
        }
    }
}
