package com.minhphuc.weapons.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Model cho Thiên Ngoại Cứu Thế Quyền Hiện (Tengai Guze Gongen):
 * - Giáp Samurai O-yoroi đen bóng với đường viền kim loại.
 * - Mũ giáp Kabuto gắn biểu tượng Trăng Lưỡi Liềm Khổng Lồ (Maedate Crescent Moon).
 * - Bờm tóc trắng vĩ đại (Giant White Hair Mane) rực sáng xõa dài từ sau đầu xuống lưng và qua vai.
 * - Chuỗi Tràng Hạt Phật Pháp Lớn (Juzu Mala Beads) vắt chéo ngực từ vai trái xuống hông phải.
 * - Mặt nạ Quỷ (Oni Menpo) với răng nanh nhọn hoắt và mắt phát quang.
 * - Giáp vai O-sode chữ nhật to lớn, giáp hông Kusazuri vững chãi.
 */
public class CuuTheQuyenHienModel<T extends LivingEntity> extends HumanoidModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "cuu_the_quyen_hien"), "main");

    // Các bộ phận đặc trưng
    public final ModelPart crescentMoon;
    public final ModelPart hairMane;
    public final ModelPart prayerBeads;
    public final ModelPart leftSode;
    public final ModelPart rightSode;
    public final ModelPart hipArmor;

    public CuuTheQuyenHienModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");
        ModelPart leftArm = root.getChild("left_arm");
        ModelPart rightArm = root.getChild("right_arm");

        this.crescentMoon = head.getChild("crescent_moon");
        this.hairMane = head.getChild("hair_mane");
        this.prayerBeads = body.getChild("prayer_beads");
        this.hipArmor = body.getChild("hip_armor");
        this.leftSode = leftArm.getChild("left_sode");
        this.rightSode = rightArm.getChild("right_sode");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0.25F), 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");
        PartDefinition leftArm = partdefinition.getChild("left_arm");
        PartDefinition rightArm = partdefinition.getChild("right_arm");

        // 1. MŨ GIÁP KABUTO & BIỂU TƯỢNG TRĂNG LƯỠI LIỀM KHỔNG LỒ (CRESCENT MOON)
        // Vành mũ Kabuto và tai giáp Shikoro
        head.addOrReplaceChild("kabuto_brim", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.5F, -9.0F, -5.0F, 9.0F, 2.0F, 10.0F, new CubeDeformation(0.2F))
                // Cánh tai giáp 2 bên mũ (Fukikaeshi)
                .texOffs(28, 32).addBox(-6.0F, -8.0F, -4.0F, 1.5F, 4.0F, 6.0F)
                .texOffs(28, 32).mirror().addBox(4.5F, -8.0F, -4.0F, 1.5F, 4.0F, 6.0F),
                PartPose.ZERO
        );

        // Biểu tượng Trăng Khuyết (Maedate Crescent Moon) lớn trên trán
        head.addOrReplaceChild("crescent_moon", CubeListBuilder.create()
                // Trung tâm mặt trăng gắn vào trán
                .texOffs(44, 32).addBox(-1.5F, -14.5F, -5.5F, 3.0F, 5.0F, 1.0F)
                // Cánh sừng trăng bên trái uốn cong lên cao
                .texOffs(52, 32).addBox(-4.5F, -17.0F, -5.5F, 3.0F, 5.0F, 1.0F)
                .texOffs(56, 32).addBox(-7.0F, -20.0F, -5.5F, 2.5F, 5.0F, 1.0F)
                // Cánh sừng trăng bên phải uốn cong lên cao
                .texOffs(52, 32).mirror().addBox(1.5F, -17.0F, -5.5F, 3.0F, 5.0F, 1.0F)
                .texOffs(56, 32).mirror().addBox(4.5F, -20.0F, -5.5F, 2.5F, 5.0F, 1.0F),
                PartPose.ZERO
        );

        // 2. BỜM TÓC TRẮNG VĨ ĐẠI (GIANT WHITE FLOWING MANE)
        // Bờm tóc dày đặc cuồn cuộn phủ kín lưng và tràn qua hai bên vai
        head.addOrReplaceChild("hair_mane", CubeListBuilder.create()
                // Lớp tóc bờm trung tâm rủ dài sau đầu xuống lưng
                .texOffs(0, 44).addBox(-5.5F, -9.0F, 4.2F, 11.0F, 16.0F, 4.0F, new CubeDeformation(0.3F))
                // Lớp tóc bờm sau gáy tỏa rộng
                .texOffs(30, 44).addBox(-7.0F, -7.0F, 3.5F, 14.0F, 18.0F, 2.5F, new CubeDeformation(0.2F))
                // Tóc bờm phủ mé bên trái
                .texOffs(0, 64).addBox(-6.5F, -6.0F, -1.0F, 2.5F, 14.0F, 5.0F)
                // Tóc bờm phủ mé bên phải
                .texOffs(0, 64).mirror().addBox(4.0F, -6.0F, -1.0F, 2.5F, 14.0F, 5.0F),
                PartPose.ZERO
        );

        // 3. VÒNG TRÀNG HẠT PHẬT PHÁP LỚN (JUZU MALA BEADS)
        // Chuỗi hạt cầu to tròn vắt chéo từ vai trái xuống hông phải
        body.addOrReplaceChild("prayer_beads", CubeListBuilder.create()
                // Hạt trên vai trái
                .texOffs(16, 64).addBox(-4.8F, -1.0F, -3.5F, 3.0F, 3.0F, 3.0F)
                // Hạt giữa ngực
                .texOffs(16, 64).addBox(-2.5F, 1.5F, -3.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(16, 64).addBox(-0.5F, 4.0F, -3.5F, 3.0F, 3.0F, 3.0F)
                // Hạt xuống hông phải
                .texOffs(16, 64).addBox(1.5F, 6.5F, -3.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(16, 64).addBox(3.2F, 9.0F, -3.2F, 3.0F, 3.0F, 3.0F)
                // Hạt vắt vòng ra sau lưng
                .texOffs(16, 64).addBox(-3.5F, 0.5F, 2.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(16, 64).addBox(-0.5F, 4.0F, 2.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(16, 64).addBox(2.2F, 7.5F, 2.5F, 3.0F, 3.0F, 3.0F),
                PartPose.ZERO
        );

        // Giáp hông Kusazuri (Tấm giáp che đùi của Samurai)
        body.addOrReplaceChild("hip_armor", CubeListBuilder.create()
                .texOffs(28, 64).addBox(-4.5F, 10.0F, -2.5F, 9.0F, 6.0F, 5.0F, new CubeDeformation(0.4F)),
                PartPose.ZERO
        );

        // 4. GIÁP VAI SAMURAI O-SODE (SHOULDER SHIELDS)
        // Giáp vai bên trái
        leftArm.addOrReplaceChild("left_sode", CubeListBuilder.create()
                .texOffs(48, 64).addBox(-0.5F, -3.0F, -3.0F, 2.0F, 9.0F, 6.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO
        );

        // Giáp vai bên phải
        rightArm.addOrReplaceChild("right_sode", CubeListBuilder.create()
                .texOffs(48, 64).mirror().addBox(-1.5F, -3.0F, -3.0F, 2.0F, 9.0F, 6.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Đung đưa nhẹ bờm tóc trắng theo chuyển động tạo cảm giác sống động như nguyên tác manga
        float maneSway = Mth.sin(ageInTicks * 0.12F) * 0.05F;
        this.hairMane.xRot = 0.1F + maneSway;
        this.hairMane.yRot = Mth.cos(ageInTicks * 0.09F) * 0.04F;

        // Giáp vai O-sode nghiêng theo cánh tay
        this.leftSode.zRot = -this.leftArm.zRot * 0.3F;
        this.rightSode.zRot = -this.rightArm.zRot * 0.3F;

        // Hoạt ảnh kỹ năng đặc biệt đồng bộ từ Server qua Action State
        if (entity instanceof com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity ct) {
            byte action = ct.getActionState();
            if (action == 1) { // SLASH (Không Gian Trảm - Vung đao chém ngang cực mạnh)
                float swingCycle = Mth.sin(ageInTicks * 0.55F);
                this.rightArm.xRot = -1.2F + swingCycle * 1.5F;
                this.rightArm.yRot = -0.7F + swingCycle * 1.1F;
                this.body.yRot = swingCycle * 0.4F;
                this.hairMane.yRot = -swingCycle * 0.3F;
            } else if (action == 2) { // STOMP (Thiên Ngoại Thần Áp - Dậm chân chấn động uy nghi)
                this.rightLeg.xRot = -0.75F;
                this.leftArm.xRot = -0.45F;
                this.rightArm.xRot = -0.45F;
                this.leftArm.zRot = -0.75F;
                this.rightArm.zRot = 0.75F;
                this.head.xRot = -0.35F; // Ngửa đầu gầm vang tiếng chuông
            } else if (action == 3) { // GUARD (Bát Nhã Kim Cang Thể - Hai tay đan chéo thủ thế hộ thể)
                this.rightArm.xRot = -1.15F;
                this.rightArm.yRot = -0.55F;
                this.leftArm.xRot = -1.15F;
                this.leftArm.yRot = 0.55F;
                this.head.xRot = 0.15F;
            }
        }
    }
}
