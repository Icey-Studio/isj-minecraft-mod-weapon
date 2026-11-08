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
 * CUSTOM 3D BLOCKBENCH-STYLE MODEL: TRẠM PHÒNG THỦ KHÔNG GIAN QUỸ ĐẠO (ORBITAL DEFENSE SATELLITE PLATFORM)
 * Dựng trực tiếp qua hệ thống ModelPart / LayerDefinition của Minecraft:
 * 1. Thân chính bọc giáp Titan đa giác (Octagonal Heavy Hull Fuselage) + Khoang Avionics bọc vàng MLI (Multi-Layer Insulation Gold Foil).
 * 2. Hai giàn tản nhiệt hạt nhân nan quạt (Deployable Ribbed Thermal Radiator Fins).
 * 3. Chảo ăng-ten Parabol liên lạc quỹ đạo sâu (High-Gain Deep Space Comms Dish) có cần tiếp sóng Feed Horn.
 * 4. Tháp ngắm quang học / hồng ngoại / LiDAR khóa mục tiêu mặt đất (Targeting Turret Pod).
 * 5. Cặp cánh pin mặt trời quang điện siêu trường (Giant Articulated Solar Panel Arrays) sải cánh hơn 60 đơn vị.
 * 6. Nòng pháo điện từ siêu trọng trường (Heavy Kinetic Railgun Assembly) với 6 vòng cuộn cảm gia tốc từ trường.
 * 7. Hệ thống 4 cụm động cơ phản lực khí nén RCS (Attitude Reaction Control System Quads).
 */
public class OrbitalDefenseSatelliteModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "orbital_satellite"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/orbital_satellite.png");

    public final ModelPart root;
    public final ModelPart fuselage;
    public final ModelPart goldFoilModule;
    public final ModelPart commsDishBoom;
    public final ModelPart commsDish;
    public final ModelPart targetingTurret;
    public final ModelPart radiatorLeft;
    public final ModelPart radiatorRight;
    public final ModelPart leftSolarWing;
    public final ModelPart rightSolarWing;
    public final ModelPart railgunAssembly;
    public final ModelPart[] railgunCoils = new ModelPart[6];

    public OrbitalDefenseSatelliteModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.fuselage = root.getChild("fuselage");
        this.goldFoilModule = fuselage.getChild("gold_foil_module");
        this.commsDishBoom = fuselage.getChild("comms_dish_boom");
        this.commsDish = commsDishBoom.getChild("comms_dish");
        this.targetingTurret = fuselage.getChild("targeting_turret");
        this.radiatorLeft = fuselage.getChild("radiator_left");
        this.radiatorRight = fuselage.getChild("radiator_right");
        this.leftSolarWing = fuselage.getChild("left_solar_wing");
        this.rightSolarWing = fuselage.getChild("right_solar_wing");
        this.railgunAssembly = fuselage.getChild("railgun_assembly");

        for (int i = 0; i < 6; i++) {
            this.railgunCoils[i] = railgunAssembly.getChild("coil_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. THÂN CHÍNH VỆ TINH (Fuselage): Khung xương Titan bát giác
        PartDefinition fuselage = root.addOrReplaceChild("fuselage",
                CubeListBuilder.create()
                        // Khối thân lõi trung tâm (14x24x14)
                        .texOffs(0, 0)
                        .addBox(-7.0F, -12.0F, -7.0F, 14.0F, 24.0F, 14.0F, new CubeDeformation(0.0F))
                        // Giáp vát cạnh 4 góc tạo hình bát giác quân sự
                        .texOffs(56, 0)
                        .addBox(-9.0F, -11.0F, -5.0F, 2.0F, 22.0F, 10.0F, new CubeDeformation(0.0F)) // Trái
                        .texOffs(56, 0)
                        .addBox(7.0F, -11.0F, -5.0F, 2.0F, 22.0F, 10.0F, new CubeDeformation(0.0F))  // Phải
                        .texOffs(80, 0)
                        .addBox(-5.0F, -11.0F, -9.0F, 10.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)) // Trước
                        .texOffs(80, 0)
                        .addBox(-5.0F, -11.0F, 7.0F, 10.0F, 22.0F, 2.0F, new CubeDeformation(0.0F))  // Sau
                        // Nắp đỉnh tháp vòm radar và cảm biến
                        .texOffs(0, 38)
                        .addBox(-5.0F, -15.0F, -5.0F, 10.0F, 3.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 38)
                        .addBox(-2.0F, -19.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // 2. KHOANG AVIONICS BỌC VÀNG CÁCH NHIỆT MLI (Gold Foil Module)
        fuselage.addOrReplaceChild("gold_foil_module",
                CubeListBuilder.create()
                        .texOffs(56, 32)
                        .addBox(-6.0F, -6.0F, -10.5F, 12.0F, 12.0F, 3.0F, new CubeDeformation(0.1F))
                        // Bình chứa nhiên liệu áp lực cao hình trụ bọc vàng
                        .texOffs(0, 51)
                        .addBox(-4.0F, -8.0F, 7.5F, 8.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 3. GIÀN TẢN NHIỆT HẠT NHÂN (Deployable Thermal Radiator Fins)
        fuselage.addOrReplaceChild("radiator_left",
                CubeListBuilder.create()
                        .texOffs(24, 51)
                        .addBox(-1.0F, -14.0F, 0.0F, 2.0F, 28.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-6.5F, 0.0F, 6.0F, 0.15F, -0.45F, -0.1F)
        );
        fuselage.addOrReplaceChild("radiator_right",
                CubeListBuilder.create()
                        .texOffs(24, 51)
                        .addBox(-1.0F, -14.0F, 0.0F, 2.0F, 28.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.5F, 0.0F, 6.0F, 0.15F, 0.45F, 0.1F)
        );

        // 4. CHẢO ĂNG-TEN PARABOL LIÊN LẠC TẦM XA (High-Gain Parabolic Comms Dish)
        PartDefinition dishBoom = fuselage.addOrReplaceChild("comms_dish_boom",
                CubeListBuilder.create()
                        .texOffs(0, 71)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -10.0F, 7.0F, -0.4F, 0.5F, 0.0F)
        );
        dishBoom.addOrReplaceChild("comms_dish",
                CubeListBuilder.create()
                        // Vành ngoài chảo parabol
                        .texOffs(20, 71)
                        .addBox(-6.0F, -6.0F, 7.0F, 12.0F, 12.0F, 2.0F, new CubeDeformation(0.0F))
                        // Lòng chảo thu sóng
                        .texOffs(48, 71)
                        .addBox(-4.0F, -4.0F, 6.0F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                        // Cần thu sóng Feed Horn ở tiêu điểm
                        .texOffs(0, 81)
                        .addBox(-0.5F, -0.5F, 9.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 5. THÁP KHÓA MỤC TIÊU MẶT ĐẤT (Earth Optical / Targeting Turret Pod)
        fuselage.addOrReplaceChild("targeting_turret",
                CubeListBuilder.create()
                        // Khớp xoay Turret
                        .texOffs(12, 81)
                        .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                        // Khối ống kính quang học 3 tầng nhìn về Trái Đất
                        .texOffs(32, 81)
                        .addBox(-2.0F, 3.5F, -3.0F, 4.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 81)
                        .addBox(-1.5F, 7.5F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 12.0F, -4.0F)
        );

        // 6. CÁNH PIN MẶT TRỜI QUANG ĐIỆN BÊN TRÁI (Left Solar Wing)
        PartDefinition leftWing = fuselage.addOrReplaceChild("left_solar_wing",
                CubeListBuilder.create()
                        // Cần giàn không gian Titanium Truss vươn ra ngoài
                        .texOffs(0, 93)
                        .addBox(-12.0F, -1.0F, -1.0F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        // Động cơ xoay cánh Solar Rotary Drive
                        .texOffs(28, 93)
                        .addBox(-15.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.0F, 0.0F, 0.0F)
        );
        // Ba khoang pin mặt trời bên trái (Solar Panels)
        leftWing.addOrReplaceChild("panels",
                CubeListBuilder.create()
                        // Tấm pin 1
                        .texOffs(64, 47)
                        .addBox(-26.0F, -8.0F, -0.5F, 11.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        // Tấm pin 2
                        .texOffs(64, 64)
                        .addBox(-38.0F, -8.0F, -0.5F, 11.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        // Tấm pin 3
                        .texOffs(64, 81)
                        .addBox(-50.0F, -8.0F, -0.5F, 11.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        // Xương sống kết nối
                        .texOffs(0, 97)
                        .addBox(-51.0F, -0.5F, -0.8F, 36.0F, 1.0F, 1.6F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 7. CÁNH PIN MẶT TRỜI QUANG ĐIỆN BÊN PHẢI (Right Solar Wing)
        PartDefinition rightWing = fuselage.addOrReplaceChild("right_solar_wing",
                CubeListBuilder.create()
                        .texOffs(0, 93)
                        .addBox(0.0F, -1.0F, -1.0F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 93)
                        .addBox(12.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(7.0F, 0.0F, 0.0F)
        );
        rightWing.addOrReplaceChild("panels",
                CubeListBuilder.create()
                        .texOffs(64, 47)
                        .addBox(15.0F, -8.0F, -0.5F, 11.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 64)
                        .addBox(27.0F, -8.0F, -0.5F, 11.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 81)
                        .addBox(39.0F, -8.0F, -0.5F, 11.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 97)
                        .addBox(15.0F, -0.5F, -0.8F, 36.0F, 1.0F, 1.6F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 8. NÒNG PHÁO ĐIỆN TỪ SIÊU TRỌNG TRƯỜNG (Super-Heavy Kinetic Railgun Assembly)
        PartDefinition railgun = fuselage.addOrReplaceChild("railgun_assembly",
                CubeListBuilder.create()
                        // Khối bệ pháo kết nối đáy thân (Breach Assembly)
                        .texOffs(0, 100)
                        .addBox(-5.0F, 0.0F, -5.0F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
                        // Ray dẫn động điện từ Trái (Left Electromagnetic Rail)
                        .texOffs(40, 98)
                        .addBox(-3.5F, 6.0F, -2.0F, 2.0F, 44.0F, 4.0F, new CubeDeformation(0.0F))
                        // Ray dẫn động điện từ Phải (Right Electromagnetic Rail)
                        .texOffs(40, 98)
                        .addBox(1.5F, 6.0F, -2.0F, 2.0F, 44.0F, 4.0F, new CubeDeformation(0.0F))
                        // Giáp vòm giảm giật nòng thoát khí (Muzzle Shroud)
                        .texOffs(52, 98)
                        .addBox(-4.5F, 46.0F, -4.5F, 9.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );

        // 6 VÒNG CUỘN CẢM GIA TỐC TỪ TRƯỜNG DỌC NÒNG PHÁO (Magnetic Induction Coils)
        for (int i = 0; i < 6; i++) {
            float yPos = 8.0F + i * 6.5F;
            railgun.addOrReplaceChild("coil_" + i,
                    CubeListBuilder.create()
                            .texOffs(88, 98)
                            .addBox(-4.0F, -1.5F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.1F)),
                    PartPose.offset(0.0F, yPos, 0.0F)
            );
        }

        // 9. BỐN CỤM ĐỘNG CƠ PHẢN LỰC KHÍ NÉN RCS (Attitude Control System Quads)
        fuselage.addOrReplaceChild("rcs_quads",
                CubeListBuilder.create()
                        // RCS Trước
                        .texOffs(0, 116)
                        .addBox(-1.5F, -6.0F, -8.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        // RCS Sau
                        .texOffs(0, 116)
                        .addBox(-1.5F, -6.0F, 6.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        // RCS Trái
                        .texOffs(10, 116)
                        .addBox(-8.5F, -6.0F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        // RCS Phải
                        .texOffs(10, 116)
                        .addBox(6.5F, -6.0F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * THIẾT LẬP HOẠT ẢNH QUỸ ĐẠO & TRẠNG THÁI NẠP NĂNG LƯỢNG
     */
    public void setupAnim(float age, float chargingProgress, int mode) {
        // 1. Chuyển động lơ lửng ổn định vi trọng lực (Microgravity Floating)
        float floatBob = Mth.sin(age * 0.05F) * 0.35F;
        float floatRoll = Mth.cos(age * 0.04F) * 0.015F;
        float floatPitch = Mth.sin(age * 0.035F) * 0.012F;
        this.fuselage.y = floatBob;
        this.fuselage.zRot = floatRoll;
        this.fuselage.xRot = floatPitch;

        // 2. Hai cánh pin mặt trời nhẹ nhàng xoay theo ánh nắng Mặt Trời
        float solarAngle = age * 0.012F;
        this.leftSolarWing.xRot = Mth.sin(solarAngle) * 0.08F;
        this.rightSolarWing.xRot = Mth.sin(solarAngle) * 0.08F;

        // 3. Chảo ăng-ten parabol liên tục duy trì kết nối vệ tinh
        this.commsDishBoom.yRot = 0.5F + Mth.sin(age * 0.03F) * 0.06F;
        this.commsDish.xRot = Mth.cos(age * 0.025F) * 0.05F;

        // 4. Tháp ngắm khóa chặt tọa độ mặt đất
        this.targetingTurret.yRot = Mth.sin(age * 0.06F) * 0.04F;
        this.targetingTurret.xRot = 0.1F + Mth.cos(age * 0.05F) * 0.03F;

        // 5. Rung chấn từ trường cực hạn của các cuộn cảm khi sạc năng lượng
        if (chargingProgress > 0.0F) {
            float jitter = chargingProgress * 0.06F;
            for (int i = 0; i < 6; i++) {
                float freq = age * 1.5F + i * 1.0F;
                this.railgunCoils[i].x = Mth.sin(freq) * jitter;
                this.railgunCoils[i].z = Mth.cos(freq) * jitter;
            }
        } else {
            for (int i = 0; i < 6; i++) {
                this.railgunCoils[i].x = 0;
                this.railgunCoils[i].z = 0;
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
