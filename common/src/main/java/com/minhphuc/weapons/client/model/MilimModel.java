package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.tensura.MilimEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model 3D Minecraft hoàn thiện cho Ma Vương Milim Nava (Milim Nava Showcase Skin):
 * - Vóc dáng cô gái anime dễ thương (Cute anime girl voxel geometry).
 * - Mái tóc hồng nổi bật buộc hai bên (Long pink twin-tails) rủ dài quá thắt lưng.
 * - Sợi tóc ngố cong hình móc câu đặc trưng trên đỉnh đầu (Single curved ahoge hair strand).
 * - Đôi mắt xanh ngọc bích rực rỡ và nụ cười rạng rỡ khoe hàm răng trắng (Toothy grin).
 * - Áo croptop đen trễ vai viền vàng, áo lót bra trắng bên trong với hạt chuỗi đỏ/xanh.
 * - Huy hiệu đá quý ngọc lục bảo 3D trên vai phải viền vàng (Green gemstone brooch).
 * - Quần đùi ngắn màu đen, tất đùi kẻ sọc hồng trắng (Striped pink & white thigh-high socks).
 * - Đôi bốt đỏ cao cổ (Tall red boots).
 * - Chiếc đuôi rồng đen nhỏ nhắn lắc lư sau thắt lưng (Small black dragon tail).
 */
public class MilimModel extends HumanoidModel<MilimEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "milim"), "main");

    // Tóc Milim & Ahoge
    public final ModelPart ahoge;
    public final ModelPart twintailLeft;
    public final ModelPart twintailRight;
    public final ModelPart frontBangs;
    public final ModelPart sideHairLeft;
    public final ModelPart sideHairRight;

    // Chi tiết trang phục & Phụ kiện 3D
    public final ModelPart greenBrooch;
    public final ModelPart femaleBust;
    public final ModelPart dragonTail;
    public final ModelPart waistRibbon;

    // Cánh rồng Dragonoid sau lưng (khi tụ lực Dragon Nova)
    public final ModelPart leftDragonWing;
    public final ModelPart rightDragonWing;

    public MilimModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");
        ModelPart rightArm = root.getChild("right_arm");

        this.ahoge = head.getChild("ahoge");
        this.twintailLeft = head.getChild("twintail_left");
        this.twintailRight = head.getChild("twintail_right");
        this.frontBangs = head.getChild("front_bangs");
        this.sideHairLeft = head.getChild("side_hair_left");
        this.sideHairRight = head.getChild("side_hair_right");

        this.greenBrooch = rightArm.getChild("green_pauldron");
        this.femaleBust = body.getChild("female_bust");
        this.dragonTail = body.getChild("dragon_tail");
        this.waistRibbon = body.getChild("waist_ribbon");
        this.leftDragonWing = body.getChild("left_dragon_wing");
        this.rightDragonWing = body.getChild("right_dragon_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Alex slim 3px arms model
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.5F, 0.0F));
        PartDefinition rightArm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.5F, 0.0F));

        // Hai chân cân đối chuẩn Humanoid
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // --- 1. SỢI TÓC NGỐ AHOGE ĐỈNH ĐẦU (CURVED AHOGE STRAND) ---
        head.addOrReplaceChild("ahoge", CubeListBuilder.create()
                // Gốc tóc vươn thẳng lên đỉnh đầu
                .texOffs(64, 0).addBox(-0.5F, -11.0F, -2.5F, 1.0F, 3.0F, 1.0F)
                // Đoạn giữa uốn cong cong về phía trước
                .texOffs(68, 0).addBox(-0.5F, -12.0F, -1.5F, 1.0F, 1.0F, 2.0F)
                // Ngọn tóc hình móc câu
                .texOffs(74, 0).addBox(-0.5F, -11.0F, 0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        // --- 2. TÓC HỒNG HAI BÍM TWINTAILS ---
        // Bím tóc trái vươn dài từ đỉnh đầu xuống quá hông
        head.addOrReplaceChild("twintail_left", CubeListBuilder.create()
                // Nơ buộc tóc màu đen sẫm
                .texOffs(64, 16).addBox(3.5F, -8.0F, -0.5F, 2.0F, 2.0F, 2.0F)
                // Lọn tóc bím dài bồng bềnh
                .texOffs(64, 20).addBox(4.0F, -6.5F, -0.5F, 2.5F, 19.0F, 2.5F, new CubeDeformation(0.08F)),
                PartPose.rotation(0.05F, 0.0F, -0.15F));

        // Bím tóc phải đối xứng
        head.addOrReplaceChild("twintail_right", CubeListBuilder.create()
                .texOffs(88, 16).mirror().addBox(-5.5F, -8.0F, -0.5F, 2.0F, 2.0F, 2.0F)
                .texOffs(88, 20).mirror().addBox(-6.5F, -6.5F, -0.5F, 2.5F, 19.0F, 2.5F, new CubeDeformation(0.08F)),
                PartPose.rotation(0.05F, 0.0F, 0.15F));

        // Tóc mái trước trán
        head.addOrReplaceChild("front_bangs", CubeListBuilder.create()
                .texOffs(8, 8).addBox(-4.0F, -8.0F, -4.2F, 8.0F, 3.5F, 0.5F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        // Lọn tóc mai hai bên má
        head.addOrReplaceChild("side_hair_left", CubeListBuilder.create()
                .texOffs(16, 8).addBox(3.8F, -6.0F, -3.8F, 0.8F, 7.5F, 1.5F),
                PartPose.ZERO);
        head.addOrReplaceChild("side_hair_right", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-4.6F, -6.0F, -3.8F, 0.8F, 7.5F, 1.5F),
                PartPose.ZERO);

        // --- 3. HUY HIỆU ĐÁ QUÝ NGỌC LỤC BẢO TRÊN VAI PHẢI (OVAL GREEN EMERALD PAULDRON) ---
        rightArm.addOrReplaceChild("green_pauldron", CubeListBuilder.create()
                // Tấm đệm giáp vai viền vàng
                .texOffs(24, 64).addBox(-2.5F, -2.6F, -2.4F, 4.0F, 2.4F, 4.8F, new CubeDeformation(0.05F))
                // Viên ngọc lục bảo hình oval lồi 3D trên vai phải
                .texOffs(0, 84).addBox(-2.2F, -3.4F, -1.8F, 3.4F, 1.2F, 3.6F),
                PartPose.ZERO);

        // --- 4. VÓC DÁNG NỮ TÍNH & WHITE BRA CROPTOP (FEMALE BUST) ---
        body.addOrReplaceChild("female_bust", CubeListBuilder.create()
                .texOffs(40, 64).addBox(-3.2F, 2.2F, -2.5F, 6.4F, 3.2F, 1.5F, new CubeDeformation(0.02F)),
                PartPose.rotation(0.05F, 0.0F, 0.0F));

        // --- 5. DẢI RUY BĂNG ĐEN UỐN LƯỢN BÊN HÔNG PHẢI (LONG WAVY BLACK RIBBON) ---
        body.addOrReplaceChild("waist_ribbon", CubeListBuilder.create()
                // Nút thắt bên hông phải
                .texOffs(0, 64).addBox(-4.8F, 10.0F, -1.0F, 1.4F, 1.5F, 2.0F)
                // Dải lụa trên uốn lượn
                .texOffs(0, 68).addBox(-4.6F, 11.2F, -0.8F, 1.0F, 5.0F, 1.6F)
                // Dải lụa dưới rủ mềm xuống đùi
                .texOffs(0, 75).addBox(-4.8F, 16.0F, -0.4F, 1.0F, 6.0F, 1.6F),
                PartPose.ZERO);

        // --- 6. ĐUÔI RỒNG ĐEN NHỎ NHẮN SAU LƯNG (SMALL BLACK DRAGON TAIL) ---
        body.addOrReplaceChild("dragon_tail", CubeListBuilder.create()
                // Gốc đuôi gắn tại thắt lưng dưới
                .texOffs(16, 64).addBox(-0.8F, 0.0F, 0.0F, 1.6F, 1.6F, 2.5F)
                // Thân đuôi uốn chúc xuống
                .texOffs(16, 69).addBox(-0.8F, 1.2F, 2.0F, 1.6F, 3.5F, 1.6F)
                // Đuôi vểnh cong sang bên hình chữ S
                .texOffs(16, 75).addBox(-0.4F, 3.5F, 2.5F, 1.6F, 3.5F, 1.6F),
                PartPose.offset(0.0F, 10.0F, 1.8F));

        // --- 7. CÁNH RỒNG DRAGONOID SAU LƯNG (KHI KÍCH HOẠT KỸ NĂNG) ---
        body.addOrReplaceChild("left_dragon_wing", CubeListBuilder.create()
                .texOffs(64, 64).addBox(0.0F, -1.0F, 2.2F, 8.0F, 10.0F, 0.8F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.1F, 0.35F, 0.0F));

        body.addOrReplaceChild("right_dragon_wing", CubeListBuilder.create()
                .texOffs(64, 64).mirror().addBox(-8.0F, -1.0F, 2.2F, 8.0F, 10.0F, 0.8F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.1F, -0.35F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(MilimEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Chuyển động đung đưa tự nhiên của bím tóc hai bên
        float hairSway = Mth.sin(ageInTicks * 0.08F) * 0.08F;
        this.twintailLeft.zRot = -0.15F + hairSway;
        this.twintailRight.zRot = 0.15F - hairSway;
        this.twintailLeft.xRot = 0.05F + Mth.cos(ageInTicks * 0.07F) * 0.05F;
        this.twintailRight.xRot = 0.05F + Mth.cos(ageInTicks * 0.07F) * 0.05F;

        // Hoạt ảnh nhún nhảy dễ thương của sợi tóc ngố Ahoge
        this.ahoge.xRot = Mth.sin(ageInTicks * 0.12F) * 0.08F;
        this.ahoge.zRot = Mth.cos(ageInTicks * 0.10F) * 0.06F;

        // Chiếc đuôi rồng đen nhỏ nhắn vẫy nhẹ nhàng tự nhiên
        this.dragonTail.yRot = Mth.sin(ageInTicks * 0.14F) * 0.25F;
        this.dragonTail.xRot = 0.1F + Mth.cos(ageInTicks * 0.12F) * 0.08F;

        // Dải ruy băng đen bên hông phải đung đưa mềm mại theo nhịp bước
        this.waistRibbon.zRot = 0.08F + Mth.sin(ageInTicks * 0.1F) * 0.08F;
        this.waistRibbon.xRot = 0.05F + Mth.cos(ageInTicks * 0.08F) * 0.06F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;

        // Trạng thái kỹ năng:
        int castState = entity.getCastingState();

        // Cánh rồng chỉ bung xòe khi tụ lực Dragon Nova (castState == 1), bình thường ẩn để đúng nguyên tác
        boolean wingsVisible = (castState == 1);
        this.leftDragonWing.visible = wingsVisible;
        this.rightDragonWing.visible = wingsVisible;

        if (castState == 1) {
            // 1: Tụ lực Long Tinh Bộc Viêm Bá (Hai tay chụm lại đẩy ra trước, cánh giương rộng)
            this.rightArm.xRot = -1.5F;
            this.rightArm.yRot = -0.3F;
            this.rightArm.zRot = 0.2F;

            this.leftArm.xRot = -1.5F;
            this.leftArm.yRot = 0.3F;
            this.leftArm.zRot = -0.2F;

            float wingFlap = Mth.sin(ageInTicks * 0.25F) * 0.35F;
            this.leftDragonWing.yRot = 0.75F + wingFlap;
            this.rightDragonWing.yRot = -0.75F - wingFlap;

            this.head.xRot = -0.2F;
        } else if (castState == 2) {
            // 2: Bắn cột sáng ngang (Một tay đưa thẳng về trước chỉ vào đối thủ)
            this.rightArm.xRot = -1.55F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;

            this.leftArm.xRot = 0.3F;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = -0.4F; // Tay trái chống hông kiêu hãnh
        }
    }
}
