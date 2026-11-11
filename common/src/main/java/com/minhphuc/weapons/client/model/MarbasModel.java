package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model 3D Minecraft hoàn thiện cho Đại Ác Ma Marbas (The Radiant Ascendant):
 * - Theo chuẩn voxel geometry thẩm mỹ thần linh (divine celestial aesthetic).
 * - Tóc trắng cắt ngắn với tóc mái rủ trán, làn da trắng bợt, biểu cảm tĩnh lặng uy nghiêm.
 * - Cặp sừng cừu xám cong uốn lượn ôm sát thái dương và vòng ra sau tai (Ram Horns).
 * - Ngực trần để lộ kết cấu lồng ngực gầy gò lộ xương (skeletal ribcage texture).
 * - Quần vải trắng tinh khôi, hai cổ chân khắc hoa văn hình xăm cổ ngữ ma thuật cổ xưa (ancient dark tribal tattoos).
 * - Đôi cánh thiên thần trắng muốt xếp lớp vĩ đại sau lưng với hoạt ảnh nhịp thở thiên giới (Majestic White Angel Wings).
 */
public class MarbasModel extends HumanoidModel<MarbasEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "marbas"), "main");

    public final ModelPart ramHorns;
    public final ModelPart leftWing;
    public final ModelPart rightWing;

    public MarbasModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        this.ramHorns = head.getChild("ram_horns");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0.25F), 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Tách riêng UV cho tay trái và chân trái chuẩn Minecraft hiện đại
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // 1. CẶP SỪNG CỪU XÁM CONG UỐN LƯỢN (CURVED GREY RAM HORNS)
        head.addOrReplaceChild("ram_horns", CubeListBuilder.create()
                // --- Sừng trái (uốn cong từ thái dương lên trên, vòng ra sau tai và chúc nhẹ về trước) ---
                // Gốc sừng vươn ra 2 bên thái dương
                .texOffs(0, 64).addBox(-6.5F, -7.5F, -2.5F, 3.0F, 3.0F, 3.0F)
                // Thân sừng uốn cong lên trên và hơi chếch ra ngoài
                .texOffs(12, 64).addBox(-8.5F, -10.0F, -1.0F, 3.0F, 3.5F, 4.0F)
                // Vòng sừng cuộn vòng ra phía sau tai và chúi xuống
                .texOffs(0, 72).addBox(-8.0F, -8.0F, 2.0F, 2.5F, 4.0F, 3.0F)
                // Chóp sừng nhọn uốn nhẹ về trước
                .texOffs(12, 72).addBox(-6.5F, -5.0F, 3.0F, 2.0F, 3.0F, 2.0F)

                // --- Sừng phải đối xứng hoàn hảo (Mirror) ---
                .texOffs(0, 64).mirror().addBox(3.5F, -7.5F, -2.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(12, 64).mirror().addBox(5.5F, -10.0F, -1.0F, 3.0F, 3.5F, 4.0F)
                .texOffs(0, 72).mirror().addBox(5.5F, -8.0F, 2.0F, 2.5F, 4.0F, 3.0F)
                .texOffs(12, 72).mirror().addBox(4.5F, -5.0F, 3.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO
        );

        // 2. ĐÔI CÁNH THIÊN THẦN TRẮNG MUỐT VĨ ĐẠI SAU LƯNG (MAJESTIC ANGEL WINGS)
        // Cánh trái (Left Wing) gắn tại vùng lưng trên
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                // Xương cánh trên vươn dài ngang
                .texOffs(32, 64).addBox(-14.0F, -8.0F, 0.0F, 14.0F, 3.0F, 1.0F)
                // Tầng lông vũ thứ 2 (Secondary Coverts)
                .texOffs(32, 70).addBox(-16.0F, -5.0F, 0.0F, 16.0F, 8.0F, 1.0F)
                // Tầng lông vũ chính dài rủ xuống (Primary Flight Feathers)
                .texOffs(32, 82).addBox(-18.0F, 3.0F, 0.0F, 16.0F, 12.0F, 1.0F)
                // Chóp đuôi lông vũ xếp nhọn vát nhọn (Wingtip Blade Feathers)
                .texOffs(32, 98).addBox(-14.0F, 15.0F, 0.0F, 10.0F, 6.0F, 1.0F),
                PartPose.offset(-1.5F, 1.0F, 2.5F)
        );

        // Cánh phải (Right Wing) gắn tại vùng lưng trên đối xứng
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                // Xương cánh trên vươn dài ngang
                .texOffs(64, 64).addBox(0.0F, -8.0F, 0.0F, 14.0F, 3.0F, 1.0F)
                // Tầng lông vũ thứ 2
                .texOffs(64, 70).addBox(0.0F, -5.0F, 0.0F, 16.0F, 8.0F, 1.0F)
                // Tầng lông vũ chính dài rủ xuống
                .texOffs(64, 82).addBox(2.0F, 3.0F, 0.0F, 16.0F, 12.0F, 1.0F)
                // Chóp đuôi lông vũ xếp nhọn
                .texOffs(64, 98).addBox(4.0F, 15.0F, 0.0F, 10.0F, 6.0F, 1.0F),
                PartPose.offset(1.5F, 1.0F, 2.5F)
        );

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(MarbasEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Hoạt ảnh nhịp thở thiên giới / vỗ cánh uy nghi thanh thoát (Divine Breathing & Wing Idle)
        float flap = Mth.sin(ageInTicks * 0.08F) * 0.06F;
        this.leftWing.yRot = 0.22F + flap;
        this.leftWing.zRot = -0.06F - flap * 0.5F;

        this.rightWing.yRot = -0.22F - flap;
        this.rightWing.zRot = 0.06F + flap * 0.5F;
    }
}
