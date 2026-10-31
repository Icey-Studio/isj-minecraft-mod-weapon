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
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0.2F), 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // 1. DẢI KINH PHÙ PHẠN TỰ BAY LƠ LỬNG QUANH ĐẦU VÀ VAI
        head.addOrReplaceChild("sutra_bands", CubeListBuilder.create()
                // Dải bùa thả sau lưng
                .texOffs(0, 32).addBox(-3.5F, -10.0F, 4.2F, 7.0F, 18.0F, 0.5F)
                // Dải bùa rủ qua vai trái
                .texOffs(16, 32).addBox(-6.0F, -8.0F, -3.5F, 1.0F, 16.0F, 4.0F)
                // Dải bùa rủ qua vai phải
                .texOffs(16, 32).mirror().addBox(5.0F, -8.0F, -3.5F, 1.0F, 16.0F, 4.0F),
                PartPose.ZERO
        );

        // 2. VÒNG TRÀNG HẠT ĐẦU LÂU PHẬT MÔN
        body.addOrReplaceChild("prayer_beads", CubeListBuilder.create()
                // Hạt vắt trước ngực
                .texOffs(28, 32).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.2F))
                // Hạt vắt sau lưng
                .texOffs(28, 32).mirror().addBox(-4.0F, 0.0F, 2.2F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO
        );

        // 3. VẠT ÁO CÀ SA CỔ PHỦ TỚI GỐI (KASAYA ROBE)
        body.addOrReplaceChild("kasaya_robe", CubeListBuilder.create()
                .texOffs(0, 52).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 8.0F, 5.0F, new CubeDeformation(0.35F)),
                PartPose.ZERO
        );

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

