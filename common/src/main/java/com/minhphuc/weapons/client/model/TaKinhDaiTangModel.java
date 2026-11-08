package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model cho Tà Kinh Đại Tăng (Jankyōbun Daisōjō - 邪経文大僧正):
 * - Thân thể Nhục Thân Bồ Tát (Sokushinbutsu) khô gầy, hốc hác đầy tử khí.
 * - Áo Cà Sa Phật giáo rách nát cổ kính.
 * - Vòng Tràng Hạt Phật Môn vắt quanh cổ.
 * - Các dải Kinh Phù Phạn Tự lơ lửng quanh vai và lưng.
 */
public class TaKinhDaiTangModel extends HumanoidModel<TaKinhDaiTangEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "ta_kinh_dai_tang"), "main");

    public final ModelPart sutraBands;
    public final ModelPart prayerBeads;
    public final ModelPart kasayaRobe;

    public TaKinhDaiTangModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        this.sutraBands = head.getChild("sutra_bands");
        this.prayerBeads = body.getChild("prayer_beads");
        this.kasayaRobe = body.getChild("kasaya_robe");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 64x64 separate left arm & left leg mapping (Player standard)
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // Các bộ phận phụ giữ tương thích API mô hình
        head.addOrReplaceChild("sutra_bands", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("prayer_beads", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("kasaya_robe", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(TaKinhDaiTangEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Dải bùa bay lượn lờ nhẹ nhàng
        float bandSway = Mth.sin(ageInTicks * 0.15F) * 0.08F;
        this.sutraBands.xRot = 0.05F + bandSway;
        this.sutraBands.yRot = Mth.cos(ageInTicks * 0.12F) * 0.05F;

        // Trạng thái tụng kinh: Đầu và thân hơi cúi nhấp nhô nhịp nhàng theo câu niệm kinh
        if (entity.isChanting()) {
            float chantBob = Mth.sin(ageInTicks * 0.35F) * 0.12F;
            this.head.xRot += chantBob;
            this.body.xRot += chantBob * 0.4F;

            // Hai tay chắp trước ngực hoặc lần tràng hạt
            this.rightArm.xRot = -1.1F + chantBob * 0.5F;
            this.rightArm.yRot = -0.5F;
            this.leftArm.xRot = -1.1F + chantBob * 0.5F;
            this.leftArm.yRot = 0.5F;
        }

        // Hoạt ảnh kỹ năng đặc biệt qua Action State
        byte action = entity.getActionState();
        if (action == 1) { // Khắc Kinh Văn: Vung hai tay về phía trước phóng bùa
            this.rightArm.xRot = -1.45F;
            this.leftArm.xRot = -1.45F;
            this.rightArm.yRot = -0.2F;
            this.leftArm.yRot = 0.2F;
        } else if (action == 2) { // Hoàng Tuyền Hóa Độ: Ngửa đầu lên trời gầm vang
            this.head.xRot = -0.45F;
            this.rightArm.xRot = -0.6F;
            this.leftArm.xRot = -0.6F;
            this.rightArm.zRot = 0.8F;
            this.leftArm.zRot = -0.8F;
        }
    }
}

