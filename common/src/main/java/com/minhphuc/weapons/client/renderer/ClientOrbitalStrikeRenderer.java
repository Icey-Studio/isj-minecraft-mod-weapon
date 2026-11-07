package com.minhphuc.weapons.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * CLIENT RENDERER CHO MOD THE SHOOTING STAR (STELLAR REMOTE)
 * Hiệu ứng quỹ đạo vũ trụ thời gian thực:
 * 1. SS-01 · Railgun: Cột lôi điện Plasma siêu trọng trường xuyên thủng tầng khí quyển
 * 2. SS-03 · Gungnir: Thần thương Gungnir rực lửa phóng chéo từ quỹ đạo Sao Mộc
 * 3. SS-04 · The Seven Stars: Tinh đồ Bắc Đẩu Thất Tinh kết nối bằng laser thiên hà
 */
public final class ClientOrbitalStrikeRenderer {

    public static final int MODE_RAILGUN = 0;
    public static final int MODE_GUNGNIR = 1;
    public static final int MODE_SEVEN_STARS = 2;

    private static ClientLevel world;
    private static Vec3 targetPos = Vec3.ZERO;
    private static int strikeMode = 0;
    private static long startTick = 0;
    private static boolean active = false;

    // Tọa độ 7 sao Bắc Đẩu tương đối
    public static final double[][] BIG_DIPPER_OFFSETS = {
            {-10.0, -12.0}, // Dubhe (Thiên Xu)
            {-14.0, -6.0},  // Merak (Thiên Toàn)
            {-8.0, 4.0},    // Phecda (Thiên Cơ)
            {-1.0, 2.0},    // Megrez (Thiên Quyền)
            {6.0, 7.0},     // Alioth (Ngọc Hành)
            {12.0, 11.0},   // Mizar (Khai Dương)
            {0.0, 0.0}      // Alkaid (Dao Quang - Tâm điểm)
    };

    public static void reset() {
        active = false;
        world = null;
        startTick = 0;
        targetPos = Vec3.ZERO;
    }

    public static void receive(Vec3 target, int mode) {
        reset();
        Minecraft mc = Minecraft.getInstance();
        world = mc.level;
        if (world == null) return;
        targetPos = target;
        strikeMode = mode;
        startTick = world.getGameTime();
        active = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static double getAge(float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.level != world) return -1;
        long elapsed = mc.level.getGameTime() - startTick;
        int maxAge = switch (strikeMode) {
            case MODE_RAILGUN -> 60;
            case MODE_GUNGNIR -> 70;
            case MODE_SEVEN_STARS -> 100;
            default -> 60;
        };
        if (elapsed > maxAge) return -1;
        return elapsed + delta;
    }

    public static void render(PoseStack matrices, Camera cameraObj, float tickDelta) {
        double age = getAge(tickDelta);
        if (age < 0) return;

        Vec3 cam = cameraObj.getPosition();
        matrices.pushPose();
        matrices.translate(targetPos.x - cam.x, targetPos.y - cam.y, targetPos.z - cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        try {
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f mat = matrices.last().pose();

            if (strikeMode == MODE_RAILGUN) {
                renderRailgun(vertices, mat, age);
            } else if (strikeMode == MODE_GUNGNIR) {
                renderGungnir(vertices, mat, age);
            } else if (strikeMode == MODE_SEVEN_STARS) {
                renderSevenStars(vertices, mat, age);
            }

            var built = vertices.build();
            if (built != null) {
                BufferUploader.drawWithShader(built);
            }
        } finally {
            matrices.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /**
     * SS-01: Railgun Cột Năng Lượng Thẳng Đứng Siêu Khủng
     */
    private static void renderRailgun(VertexConsumer buf, Matrix4f mat, double age) {
        // Ticks 0..20: Tia ngắm khóa mục tiêu mỏng
        if (age < 20.0) {
            float alpha = (float) (0.3 + 0.7 * Math.sin(age * 0.8));
            renderVerticalCylinder(buf, mat, 0, 0, -64, 320, 0.25f, 0.0f, 0.9f, 1.0f, alpha * 0.8f);
        }
        // Ticks 20..45: Cột Plasma Railgun bùng nổ cực đại
        else if (age < 45.0) {
            double blastAge = age - 20.0;
            float radiusCore = (float) Math.max(0.1, 4.0 * (1.0 - blastAge / 25.0));
            float radiusGlow = radiusCore * 2.2f;
            float alphaCore = (float) (1.0 - (blastAge / 25.0) * 0.5);
            float alphaGlow = (float) (0.6 - (blastAge / 25.0) * 0.6);

            // Cột tâm trắng chói
            renderVerticalCylinder(buf, mat, 0, 0, -64, 340, radiusCore, 0.9f, 1.0f, 1.0f, alphaCore);
            // Vỏ ngoài Cyan điện từ
            renderVerticalCylinder(buf, mat, 0, 0, -64, 340, radiusGlow, 0.0f, 0.85f, 1.0f, alphaGlow);

            // Vòng sóng chấn xung kích lan tỏa trên mặt đất
            float ringRadius = (float) (blastAge * 1.6);
            if (ringRadius < 36.0f) {
                renderGroundRing(buf, mat, 0, 0, 0.1f, ringRadius, 0.8f, 0.0f, 0.9f, 1.0f, (float)(1.0 - ringRadius / 36.0f));
            }
        }
    }

    /**
     * SS-03: Gungnir Thần Thương Lao Chéo 75 Độ Từ Quỹ Đạo Sao Mộc
     */
    private static void renderGungnir(VertexConsumer buf, Matrix4f mat, double age) {
        if (age < 25.0) {
            // Mũi thương lao xuống từ (-30, 180, -30) xuống (0, 0, 0)
            double t = age / 25.0;
            double posX = -35.0 * (1.0 - t);
            double posY = 200.0 * (1.0 - t);
            double posZ = -35.0 * (1.0 - t);

            // Vẽ thân thương Gungnir dạng hình nón nhọn màu vàng kim - đỏ
            renderSpear(buf, mat, (float) posX, (float) posY, (float) posZ, (float) -35.0, (float) 200.0, (float) -35.0);
        } else if (age < 50.0) {
            // Vụ nổ mặt đất khi thương cắm xuống
            double blastAge = age - 25.0;
            float ringRadius = (float) (blastAge * 1.5);
            if (ringRadius < 42.0f) {
                renderGroundRing(buf, mat, 0, 0, 0.15f, ringRadius, 1.2f, 1.0f, 0.3f, 0.0f, (float)(1.0 - ringRadius / 42.0f));
                renderGroundRing(buf, mat, 0, 0, 0.2f, ringRadius * 0.7f, 1.5f, 1.0f, 0.9f, 0.1f, (float)(1.0 - ringRadius / 42.0f));
            }
        }
    }

    /**
     * SS-04: The Seven Stars (Bắc Đẩu Thất Tinh) 7 Tia Năng Lượng Tuần Tự
     */
    private static void renderSevenStars(VertexConsumer buf, Matrix4f mat, double age) {
        // Vẽ 7 vị trí sao
        for (int i = 0; i < 7; i++) {
            double starStartTick = 10.0 + i * 8.0;
            double starAge = age - starStartTick;

            double sx = BIG_DIPPER_OFFSETS[i][0];
            double sz = BIG_DIPPER_OFFSETS[i][1];

            // Tia ngắm sơ bộ
            if (age < starStartTick) {
                renderVerticalCylinder(buf, mat, (float) sx, (float) sz, -32, 280, 0.15f, 0.8f, 0.2f, 1.0f, 0.4f);
            }
            // Thiên thạch/laser giáng xuống
            else if (starAge >= 0 && starAge < 20.0) {
                float rad = (float) Math.max(0.1, 2.5 * (1.0 - starAge / 20.0));
                float alpha = (float) (1.0 - starAge / 20.0);
                renderVerticalCylinder(buf, mat, (float) sx, (float) sz, -64, 300, rad, 0.7f, 0.3f, 1.0f, alpha);

                float rRing = (float) (starAge * 1.2);
                if (rRing < 16.0f) {
                    renderGroundRing(buf, mat, (float) sx, (float) sz, 0.1f, rRing, 0.6f, 0.9f, 0.4f, 1.0f, (float)(1.0 - rRing / 16.0f));
                }
            }
        }

        // Vẽ đường nối chòm sao Bắc Đẩu trên bầu trời (ở độ cao y = 140)
        if (age >= 10.0 && age <= 75.0) {
            float lineAlpha = (float) (0.5 * Math.min(1.0, (age - 10.0) / 10.0) * (1.0 - Math.max(0.0, (age - 60.0) / 15.0)));
            for (int i = 0; i < 6; i++) {
                double x1 = BIG_DIPPER_OFFSETS[i][0];
                double z1 = BIG_DIPPER_OFFSETS[i][1];
                double x2 = BIG_DIPPER_OFFSETS[i + 1][0];
                double z2 = BIG_DIPPER_OFFSETS[i + 1][1];
                renderSkyBeam(buf, mat, (float) x1, 140.0f, (float) z1, (float) x2, 140.0f, (float) z2, 0.3f, 0.8f, 0.4f, 1.0f, lineAlpha);
            }
        }
    }

    private static void renderVerticalCylinder(VertexConsumer buf, Matrix4f mat, float cx, float cz, float minY, float maxY, float radius, float r, float g, float b, float a) {
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = cx + (float) (Math.cos(a1) * radius);
            float z1 = cz + (float) (Math.sin(a1) * radius);
            float x2 = cx + (float) (Math.cos(a2) * radius);
            float z2 = cz + (float) (Math.sin(a2) * radius);

            buf.addVertex(mat, x1, minY, z1).setColor(r, g, b, a);
            buf.addVertex(mat, x2, minY, z2).setColor(r, g, b, a);
            buf.addVertex(mat, x2, maxY, z2).setColor(r, g, b, a);
            buf.addVertex(mat, x1, maxY, z1).setColor(r, g, b, a);
        }
    }

    private static void renderGroundRing(VertexConsumer buf, Matrix4f mat, float cx, float cz, float y, float radius, float width, float r, float g, float b, float a) {
        int segments = 24;
        float rInner = Math.max(0.0f, radius - width);
        float rOuter = radius + width;

        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            buf.addVertex(mat, cx + cos1 * rInner, y, cz + sin1 * rInner).setColor(r, g, b, 0.0f);
            buf.addVertex(mat, cx + cos1 * rOuter, y, cz + sin1 * rOuter).setColor(r, g, b, a);
            buf.addVertex(mat, cx + cos2 * rOuter, y, cz + sin2 * rOuter).setColor(r, g, b, a);
            buf.addVertex(mat, cx + cos2 * rInner, y, cz + sin2 * rInner).setColor(r, g, b, 0.0f);
        }
    }

    private static void renderSpear(VertexConsumer buf, Matrix4f mat, float px, float py, float pz, float dirX, float dirY, float dirZ) {
        float length = 18.0f;
        float tipX = px;
        float tipY = py;
        float tipZ = pz;

        float tailX = px - (dirX / 200.0f) * length;
        float tailY = py - (dirY / 200.0f) * length;
        float tailZ = pz - (dirZ / 200.0f) * length;

        renderSkyBeam(buf, mat, tailX, tailY, tailZ, tipX, tipY, tipZ, 0.8f, 1.0f, 0.8f, 0.1f, 0.95f);
        renderSkyBeam(buf, mat, tailX, tailY, tailZ, tipX, tipY, tipZ, 1.8f, 1.0f, 0.3f, 0.0f, 0.6f);
    }

    private static void renderSkyBeam(VertexConsumer buf, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a) {
        buf.addVertex(mat, x1 - thickness, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1 + thickness, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x2 + thickness, y2, z2).setColor(r, g, b, a);
        buf.addVertex(mat, x2 - thickness, y2, z2).setColor(r, g, b, a);

        buf.addVertex(mat, x1, y1 - thickness, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1 + thickness, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2 + thickness, z2).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2 - thickness, z2).setColor(r, g, b, a);
    }

    /**
     * HUD Overlay: Hiệu ứng Radar Vệ Tinh Quỹ Đạo khi đang khóa mục tiêu
     */
    public static void renderHudOverlay(GuiGraphics graphics, float partialTick) {
        if (!active) return;
        double age = getAge(partialTick);
        if (age < 0) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Nếu trong khoảnh khắc va chạm (Impact frame): Chớp sáng màn hình
        boolean impact = false;
        if (strikeMode == MODE_RAILGUN && age >= 20.0 && age <= 24.0) impact = true;
        if (strikeMode == MODE_GUNGNIR && age >= 25.0 && age <= 29.0) impact = true;
        if (strikeMode == MODE_SEVEN_STARS && ((age >= 10 && age <= 13) || (age >= 18 && age <= 21) || (age >= 58 && age <= 63))) impact = true;

        if (impact) {
            int flashColor = ((int)(0xAA * (1.0 - (age % 4) / 4.0)) << 24) | 0xFFFFFF;
            graphics.fill(0, 0, screenWidth, screenHeight, flashColor);
        }
    }
}
