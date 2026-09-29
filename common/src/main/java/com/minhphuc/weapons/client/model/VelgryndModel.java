package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VelgryndModel extends HumanoidModel<VelgryndEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "velgrynd"), "main");

    // Head parts
    public final ModelPart odangoLeft;
    public final ModelPart odangoRight;
    public final ModelPart goldenPinLeft;
    public final ModelPart goldenPinRight;
    public final ModelPart longHairBack;
    public final ModelPart sideHairLeft;
    public final ModelPart sideHairRight;

    // Body parts
    public final ModelPart femaleBust;
    public final ModelPart cheongsamCollar;
    public final ModelPart sashRibbon;
    public final ModelPart cheongsamSkirt;

    // Weapon
    public final ModelPart dragonFan;

    public VelgryndModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        this.odangoLeft = head.getChild("odango_left");
        this.odangoRight = head.getChild("odango_right");
        this.goldenPinLeft = head.getChild("golden_pin_left");
        this.goldenPinRight = head.getChild("golden_pin_right");
        this.longHairBack = head.getChild("long_hair_back");
        this.sideHairLeft = head.getChild("side_hair_left");
        this.sideHairRight = head.getChild("side_hair_right");

        this.femaleBust = body.getChild("female_bust");
        this.cheongsamCollar = body.getChild("cheongsam_collar");
        this.sashRibbon = body.getChild("sash_ribbon");
        this.cheongsamSkirt = body.getChild("cheongsam_skirt");

        this.dragonFan = this.rightArm.getChild("dragon_fan");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");
        PartDefinition rightArm = partdefinition.getChild("right_arm");

        // --- 1. ODANGO TWIN BUNS & GOLDEN PINS ---
        head.addOrReplaceChild("odango_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(3.0F, -9.5F, -1.5F, 3.2F, 3.2F, 3.2F),
                PartPose.ZERO);
        head.addOrReplaceChild("odango_right", CubeListBuilder.create()
                .texOffs(0, 0).mirror().addBox(-6.2F, -9.5F, -1.5F, 3.2F, 3.2F, 3.2F),
                PartPose.ZERO);

        // Golden hairpins inserting through buns
        head.addOrReplaceChild("golden_pin_left", CubeListBuilder.create()
                .texOffs(40, 2).addBox(2.2F, -10.2F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.rotation(0.0F, 0.0F, 0.25F));
        head.addOrReplaceChild("golden_pin_right", CubeListBuilder.create()
                .texOffs(40, 2).mirror().addBox(-7.2F, -10.2F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.rotation(0.0F, 0.0F, -0.25F));

        // Long flowing azure hair extending down past the waist
        head.addOrReplaceChild("long_hair_back", CubeListBuilder.create()
                .texOffs(24, 8).addBox(-4.0F, -1.0F, 3.6F, 8.0F, 18.0F, 1.8F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        // Side tendrils framing cheekbones
        head.addOrReplaceChild("side_hair_left", CubeListBuilder.create()
                .texOffs(16, 8).addBox(3.8F, -1.0F, -3.5F, 1.0F, 8.0F, 1.5F),
                PartPose.ZERO);
        head.addOrReplaceChild("side_hair_right", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-4.8F, -1.0F, -3.5F, 1.0F, 8.0F, 1.5F),
                PartPose.ZERO);

        // --- 2. FEMININE QIPAO SILHOUETTE ---
        // Feminine bust
        body.addOrReplaceChild("female_bust", CubeListBuilder.create()
                .texOffs(20, 21).addBox(-3.5F, 1.8F, -2.8F, 7.0F, 3.6F, 1.8F, new CubeDeformation(0.02F)),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        // Cheongsam collar
        body.addOrReplaceChild("cheongsam_collar", CubeListBuilder.create()
                .texOffs(20, 16).addBox(-2.5F, -0.6F, -2.2F, 5.0F, 1.5F, 4.4F, new CubeDeformation(0.05F)),
                PartPose.ZERO);

        // Fluttering crimson sash ribbons from waist
        body.addOrReplaceChild("sash_ribbon", CubeListBuilder.create()
                .texOffs(24, 25).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 8.0F, 0.6F),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        // Cheongsam side-slit skirt
        body.addOrReplaceChild("cheongsam_skirt", CubeListBuilder.create()
                .texOffs(20, 36).addBox(-4.2F, 10.5F, -2.3F, 8.4F, 7.0F, 4.6F, new CubeDeformation(0.25F)),
                PartPose.ZERO);

        // --- 3. DRAGON FEATHER FAN (QUẠT LÔNG VŨ LONG CHỦNG) ---
        // Attached to right hand
        PartDefinition fan = rightArm.addOrReplaceChild("dragon_fan", CubeListBuilder.create()
                // Ebony handle
                .texOffs(40, 24).addBox(-0.5F, 8.0F, -1.0F, 1.0F, 5.0F, 1.0F)
                // Feather fan blades (flared golden-crimson arc)
                .texOffs(36, 16).addBox(-4.5F, 4.0F, -1.2F, 9.0F, 5.0F, 0.6F, new CubeDeformation(0.1F)),
                PartPose.rotation(0.35F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(VelgryndEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Hovering floating animation (Sine wave hovering)
        float hoverBob = Mth.sin(ageInTicks * 0.1F) * 0.05F;
        this.longHairBack.xRot = 0.12F + Mth.sin(ageInTicks * 0.08F) * 0.06F;
        this.sashRibbon.xRot = 0.1F + Mth.cos(ageInTicks * 0.08F) * 0.08F;
        this.sideHairLeft.zRot = Mth.sin(ageInTicks * 0.06F) * 0.03F;
        this.sideHairRight.zRot = -Mth.sin(ageInTicks * 0.06F) * 0.03F;

        // Quạt lông vũ: Hiện khi đang cầm quạt hoặc đang tấn công
        boolean showFan = entity.isHoldingFan() || entity.swingTime > 0;
        this.dragonFan.visible = showFan;

        if (showFan) {
            // Elegant fan posture
            this.rightArm.xRot = -0.55F + Mth.sin(ageInTicks * 0.08F) * 0.05F;
            this.rightArm.yRot = -0.25F;
            this.rightArm.zRot = 0.15F;
        }

        // Skill casting poses
        int castState = entity.getCastingState();
        if (castState == 1) {
            // 1: Thao túng thời không (Cả 2 tay giơ cao, tụ hội kết giới thời không)
            this.rightArm.xRot = -2.4F;
            this.rightArm.yRot = -0.3F;
            this.rightArm.zRot = 0.4F;

            this.leftArm.xRot = -2.4F;
            this.leftArm.yRot = 0.3F;
            this.leftArm.zRot = -0.4F;

            this.head.xRot = -0.3F; // Looking proudly upward
        } else if (castState == 2) {
            // 2: Phi đao (Vung quạt/tay chém liên hoàn)
            float slashSwing = Mth.sin(ageInTicks * 0.6F);
            this.rightArm.xRot = -1.2F + slashSwing * 0.8F;
            this.rightArm.yRot = -0.6F;
            this.leftArm.xRot = -0.4F;
            this.leftArm.yRot = 0.4F;
        } else if (castState == 3) {
            // 3: Gia tốc chước nhiệt long (Lao thẳng siêu thanh Mach 5)
            this.body.xRot = 0.75F;
            this.head.xRot = -0.7F;
            this.rightArm.xRot = 0.9F;
            this.leftArm.xRot = 0.9F;
            this.rightLeg.xRot = 0.8F;
            this.leftLeg.xRot = 0.8F;
            this.longHairBack.xRot = 0.65F;
            this.sashRibbon.xRot = 0.7F;
        } else if (castState == 4) {
            // 4: Tụ lực Gia tốc chước nhiệt long (Ngưng tụ hỏa long khí quyển - 7 giây)
            this.body.xRot = -0.15F;
            this.head.xRot = -0.25F;
            this.rightArm.xRot = -1.6F;
            this.rightArm.yRot = -0.35F;
            this.rightArm.zRot = 0.5F;
            this.leftArm.xRot = -1.6F;
            this.leftArm.yRot = 0.35F;
            this.leftArm.zRot = -0.5F;
            this.longHairBack.xRot = 0.3F + Mth.sin(ageInTicks * 0.2F) * 0.15F;
            this.sashRibbon.xRot = 0.25F + Mth.cos(ageInTicks * 0.2F) * 0.15F;
        }
    }
}
