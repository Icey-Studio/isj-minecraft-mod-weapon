package com.minhphuc.weapons.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * CUSTOM 3D BLOCKBENCH-STYLE MODEL: ĐẦU ĐẠN TẤN CÔNG QUỸ ĐẠO SIÊU THANH (ORBITAL HYPERSONIC KINETIC PROJECTILE)
 * Hỗ trợ 3 hình thái tương ứng với 3 chế độ oanh tạc:
 * 1. MODE 0 (SS-01 - Rods from God): Trụ Vonfram Siêu Trọng Trường (Heavy Tungsten Penetrator) với 4 cánh ổn định khí động học Delta Fins và động cơ hãm quỹ đạo.
 * 2. MODE 1 (SS-03 - Divine Lance Gungnir): Thần Thương Gungnir Thần Đạo rực lửa, lưỡi thương mở rộng, ngạnh chữ thập cổ ngữ Bắc Âu.
 * 3. MODE 2 (SS-04 - The Seven Stars): Mũi Tinh Tiên Pha Lê Bắc Đẩu (Astral Star Needle) với các phiến tinh thể sắc bén.
 */
public class OrbitalKineticProjectileModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "orbital_projectile"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/orbital_projectile.png");

    public final ModelPart root;
    public final ModelPart tungstenRod;
    public final ModelPart gungnirLance;
    public final ModelPart astralNeedle;

    public OrbitalKineticProjectileModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.tungstenRod = root.getChild("tungsten_rod");
        this.gungnirLance = root.getChild("gungnir_lance");
        this.astralNeedle = root.getChild("astral_needle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ==========================================
        // 1. MODE 0: TRỤ VONFRAM SIÊU TRỌNG TRƯỜNG (TUNGSTEN ROD)
        // ==========================================
        PartDefinition rod = root.addOrReplaceChild("tungsten_rod",
                CubeListBuilder.create()
                        // Thân trụ hợp kim Vonfram đặc (Dày 4x4, dài 40)
                        .texOffs(0, 0)
                        .addBox(-2.0F, -20.0F, -2.0F, 4.0F, 40.0F, 4.0F, new CubeDeformation(0.0F))
                        // Rãnh tản nhiệt khí động học chạy dọc thân
                        .texOffs(16, 0)
                        .addBox(-2.5F, -16.0F, -1.0F, 5.0F, 32.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 0)
                        .addBox(-1.0F, -16.0F, -2.5F, 2.0F, 32.0F, 5.0F, new CubeDeformation(0.0F))
                        // Chóp mũi xuyên giáp siêu cứng (Ogive Penetrator Tip)
                        .texOffs(30, 0)
                        .addBox(-1.5F, 20.0F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 8)
                        .addBox(-1.0F, 25.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 14)
                        .addBox(-0.5F, 29.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        // Động cơ đẩy hãm quỹ đạo đuôi (De-orbit Booster Nozzle)
                        .texOffs(0, 44)
                        .addBox(-2.5F, -23.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 4 Cánh lái khí động học Delta Fins ở đuôi đạn
        rod.addOrReplaceChild("fins",
                CubeListBuilder.create()
                        // Cánh +X
                        .texOffs(38, 0)
                        .addBox(2.0F, -20.0F, -0.5F, 5.0F, 14.0F, 1.0F, new CubeDeformation(0.0F))
                        // Cánh -X
                        .texOffs(38, 0)
                        .addBox(-7.0F, -20.0F, -0.5F, 5.0F, 14.0F, 1.0F, new CubeDeformation(0.0F))
                        // Cánh +Z
                        .texOffs(50, 0)
                        .addBox(-0.5F, -20.0F, 2.0F, 1.0F, 14.0F, 5.0F, new CubeDeformation(0.0F))
                        // Cánh -Z
                        .texOffs(50, 0)
                        .addBox(-0.5F, -20.0F, -7.0F, 1.0F, 14.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // ==========================================
        // 2. MODE 1: THẦN THƯƠNG GUNGNIR THẦN ĐẠO (DIVINE LANCE)
        // ==========================================
        PartDefinition gungnir = root.addOrReplaceChild("gungnir_lance",
                CubeListBuilder.create()
                        // Thân thương mạ vàng kim linh lực
                        .texOffs(0, 52)
                        .addBox(-1.0F, -24.0F, -1.0F, 2.0F, 48.0F, 2.0F, new CubeDeformation(0.0F))
                        // Cán cầm bọc da thần thú và đai kim loại
                        .texOffs(8, 52)
                        .addBox(-1.5F, -10.0F, -1.5F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                        // Ngạnh chữ thập Cổ Ngữ Bắc Âu (Runic Crossguard)
                        .texOffs(20, 52)
                        .addBox(-6.0F, 16.0F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        // Lưỡi Thần Thương Gungnir rộng bản sắc bén (Razor Spearhead)
                        .texOffs(20, 58)
                        .addBox(-3.5F, 19.0F, -0.8F, 7.0F, 16.0F, 1.6F, new CubeDeformation(0.0F))
                        // Mũi nhọn đâm xuyên thần thánh
                        .texOffs(38, 58)
                        .addBox(-2.0F, 35.0F, -0.5F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 58)
                        .addBox(-0.5F, 43.0F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        // Chuôi thương đối trọng quả cầu tinh tú
                        .texOffs(8, 71)
                        .addBox(-2.0F, -28.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // ==========================================
        // 3. MODE 2: MŨI TINH TIÊN BẮC ĐẨU (ASTRAL STAR NEEDLE)
        // ==========================================
        root.addOrReplaceChild("astral_needle",
                CubeListBuilder.create()
                        // Lõi phi đao tinh tú bát giác
                        .texOffs(0, 102)
                        .addBox(-1.0F, -18.0F, -1.0F, 2.0F, 36.0F, 2.0F, new CubeDeformation(0.0F))
                        // Vành cánh tinh thể đối xứng sắc nhọn
                        .texOffs(8, 102)
                        .addBox(-4.0F, -8.0F, 0.0F, 8.0F, 20.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 102)
                        .addBox(0.0F, -8.0F, -4.0F, 0.0F, 20.0F, 8.0F, new CubeDeformation(0.0F))
                        // Mũi kim cương tinh tú
                        .texOffs(24, 102)
                        .addBox(-0.5F, 18.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 128);
    }

    /**
     * BẬT TẮT CHẾ ĐỘ HIỂN THỊ TƯƠNG ỨNG VÀ HOẠT ẢNH XOAY TRỤC KHÍ ĐỘNG HỌC
     */
    public void setupMode(int mode, float age, float spinSpeed) {
        this.tungstenRod.visible = (mode == 0);
        this.gungnirLance.visible = (mode == 1);
        this.astralNeedle.visible = (mode == 2);

        // Xoay ổn định khí động học quanh trục Y (Spin-stabilization dọc đường đạn)
        float spin = age * spinSpeed;
        if (mode == 0) {
            this.tungstenRod.yRot = spin;
        } else if (mode == 1) {
            this.gungnirLance.yRot = spin * 1.5F;
        } else {
            this.astralNeedle.yRot = spin * 2.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
