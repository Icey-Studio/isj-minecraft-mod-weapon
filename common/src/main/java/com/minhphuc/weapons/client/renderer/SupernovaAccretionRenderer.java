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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Direct GPU vertex rendering of the Supernova Black Hole Accretion Disk and Shockwaves.
 * Ported from God Wand 1.20.1 (HyIsNoob/god-wand).
 * 100% Client-side OpenGL Additive Blending - Zero Entity Spam & Zero Block Destruction!
 */
public final class SupernovaAccretionRenderer {
    private static ClientLevel world;
    private static Vec3 center = Vec3.ZERO;
    private static long startWorldTick = 0;
    private static boolean active = false;

    private static final float[] EDGES = {-1.0f, -0.22f, 0.22f, 1.0f};
    private static Vec3 currentCameraRel = Vec3.ZERO;

    public static void reset() {
        active = false;
        world = null;
        startWorldTick = 0;
        currentCameraRel = Vec3.ZERO;
    }

    public static void receive(Vec3 centerPos) {
        reset();
        Minecraft mc = Minecraft.getInstance();
        world = mc.level;
        if (world == null) return;
        center = centerPos;
        startWorldTick = world.getGameTime();
        active = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static double getAge(float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.level != world) return -1;
        long elapsed = mc.level.getGameTime() - startWorldTick;
        if (elapsed > 380) return -1;
        return elapsed + delta;
    }

    public static void render(PoseStack matrices, Camera cameraObj, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double age = getAge(tickDelta);
        if (age < 0 || age >= 380) return;

        boolean preImpactWave = age >= 330 && age <= 342;
        if (!preImpactWave && SupernovaSequence.ribbonVisibility(age) <= 0) return;

        Vec3 camera = cameraObj.getPosition();
        currentCameraRel = camera.subtract(center);

        matrices.pushPose();
        matrices.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        try {
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            emit(vertices, matrices.last().pose(), age);
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

    private static double streamPhase(double age, int strand) {
        double travel = age * (0.006 + SupernovaSequence.pressure(age) * 0.009) + strand * 0.618034;
        return travel - Math.floor(travel);
    }

    public static Vec3 streamPoint(double age, int strand, double tail) {
        double pressure = SupernovaSequence.pressure(age);
        double collapse = SupernovaSequence.swallow(age, 0.5);
        double travel = streamPhase(age, strand) - tail * 0.30;
        travel = Math.max(0, travel);
        double core = SupernovaSequence.coreRadius(age);
        double radius = core * 0.65 + Math.pow(1 - travel, 1.8) * (15 + 30 * pressure) * (1 - collapse * 0.80);
        double angle = strand * 2.399963 + travel * (3.2 + pressure * 4.5) + age * 0.012;
        double elevation = Math.sin(strand * 1.7 + travel * 2.5) * 0.42;
        return new Vec3(Math.cos(angle) * radius, Math.sin(elevation) * radius, Math.sin(angle) * radius);
    }

    static void emit(VertexConsumer v, Matrix4f m, double age) {
        if (age >= 330 && age <= 342) {
            emitPreImpactShockwave(v, m, age);
        }
        double visible = SupernovaSequence.ribbonVisibility(age);
        if (visible <= 0) return;
        double pressure = SupernovaSequence.pressure(age);

        // 12 dải ribbon plasma xoáy vào tâm lỗ đen
        for (int strand = 0; strand < 12; strand++) {
            for (int segment = 0; segment < 48; segment++) {
                Vec3 segCenter = streamPoint(age, strand, (segment + 0.5) / 48.0);
                if (segCenter.distanceTo(currentCameraRel) < 1.4) continue;

                for (int lane = 0; lane < 3; lane++) {
                    for (int corner = 0; corner < 4; corner++) {
                        double t = (segment + (corner == 1 || corner == 2 ? 1 : 0)) / 48.0;
                        double edge = EDGES[lane + (corner >= 2 ? 1 : 0)];
                        Vec3 point = streamPoint(age, strand, t);
                        Vec3 width = new Vec3(Math.sin(strand * 2.4), 0.8, Math.cos(strand * 2.4)).normalize();
                        double feather = Math.abs(edge) > 0.9 ? 0 : 1;
                        double taper = Math.pow(Math.sin(t * Math.PI), 0.8);
                        double cycle = streamPhase(age, strand);
                        double lifetime = SupernovaSequence.phase(cycle, 0, 0.12) * (1.0 - SupernovaSequence.phase(cycle, 0.86, 1.0));
                        point = point.add(width.scale(edge * (0.40 + pressure * 0.80) * taper));

                        vertex(v, m, point,
                                lane == 1 ? 255 : 220,
                                lane == 1 ? 170 : 55,
                                lane == 1 ? 225 : 200,
                                (int) (visible * feather * taper * lifetime * (125 + pressure * 85)));
                    }
                }
            }
        }

        // Vành đai bồi tụ phát quang (Accretion Orbits)
        double collapse = SupernovaSequence.swallow(age, 0);
        for (int orbit = 0; orbit < 3; orbit++) {
            double radius = SupernovaSequence.coreRadius(age) * (1.45 + (0.65 + orbit * 0.48) * (1.0 - collapse));
            arc(v, m, radius, age * (0.015 + pressure * 0.045) + orbit * 2.4, 0.25 + orbit * 0.42, 0,
                    visible * 0.85, 0.28 + pressure * 0.25, 235, 65, 205);
        }

        emitInfallSparks(v, m, age, visible, pressure);
    }

    private static void emitInfallSparks(VertexConsumer v, Matrix4f m, double age, double visible, double pressure) {
        for (int spark = 0; spark < 24; spark++) {
            double cycle = streamPhase(age, spark + 20);
            double life = SupernovaSequence.phase(cycle, 0, 0.12) * (1.0 - SupernovaSequence.phase(cycle, 0.86, 1.0));
            for (int segment = 0; segment < 5; segment++) {
                double t = segment / 45.0;
                Vec3 a = streamPoint(age, spark + 20, t);
                Vec3 b = streamPoint(age, spark + 20, t + 1 / 45.0);
                if (a.distanceTo(currentCameraRel) < 1.4) continue;
                Vec3 width = b.subtract(a).cross(new Vec3(0.3, 1, 0.2)).normalize()
                        .scale((0.05 + pressure * 0.08) * (1.0 - segment / 5.0));
                int alpha = (int) (230 * visible * life * (1.0 - segment / 5.0));
                vertex(v, m, a.subtract(width), 255, 190, 230, alpha);
                vertex(v, m, b.subtract(width), 235, 65, 205, alpha);
                vertex(v, m, b.add(width), 235, 65, 205, alpha);
                vertex(v, m, a.add(width), 255, 190, 230, alpha);
            }
        }
    }

    private static void arc(VertexConsumer v, Matrix4f m, double radius, double rotation, double tilt, double y,
                            double strength, double width, int red, int green, int blue) {
        for (int segment = 0; segment < 64; segment++) {
            double angleMid = rotation + (segment + 0.5) / 64.0 * 2.4;
            Vec3 arcMid = new Vec3(Math.cos(angleMid) * radius, Math.sin(angleMid) * radius * Math.sin(tilt) + y, Math.sin(angleMid) * radius * Math.cos(tilt));
            if (arcMid.distanceTo(currentCameraRel) < 1.4) continue;
            for (int lane = 0; lane < 3; lane++) {
                for (int corner = 0; corner < 4; corner++) {
                    double t = (segment + (corner == 1 || corner == 2 ? 1 : 0)) / 64.0;
                    double edge = EDGES[lane + (corner >= 2 ? 1 : 0)];
                    double angle = rotation + t * 2.4;
                    double r = radius + edge * width;
                    Vec3 point = new Vec3(Math.cos(angle) * r, Math.sin(angle) * r * Math.sin(tilt) + y, Math.sin(angle) * r * Math.cos(tilt));
                    vertex(v, m, point, red, green, blue, (int) (220 * strength * Math.sin(t * Math.PI) * (Math.abs(edge) > 0.9 ? 0 : 1)));
                }
            }
        }
    }

    private static void emitPreImpactShockwave(VertexConsumer v, Matrix4f m, double age) {
        double waveT = Math.clamp((age - 330.0) / 10.0, 0.0, 1.0);
        float alpha = (float) Math.sin(waveT * Math.PI);
        if (alpha <= 0.001f) return;

        double radius = 1.5 + Math.pow(waveT, 0.55) * 32.0;
        double width = 0.8 + waveT * 2.5;

        emitShockwaveRing(v, m, radius, width, 0.0, 0.0, alpha);
        emitShockwaveRing(v, m, radius * 0.92, width * 0.85, 0.28, waveT * 0.5, alpha * 0.75f);
    }

    private static void emitShockwaveRing(VertexConsumer v, Matrix4f m, double radius, double width, double tilt, double rotation, float alpha) {
        int segments = 64;
        for (int seg = 0; seg < segments; seg++) {
            double angleMid = rotation + ((seg + 0.5) * 2.0 * Math.PI) / segments;
            Vec3 ringMid = new Vec3(Math.cos(angleMid) * radius, Math.sin(angleMid) * radius * Math.sin(tilt), Math.sin(angleMid) * radius * Math.cos(tilt));
            if (ringMid.distanceTo(currentCameraRel) < 1.4) continue;

            double a1 = rotation + (seg * 2.0 * Math.PI) / segments;
            double a2 = rotation + ((seg + 1) * 2.0 * Math.PI) / segments;

            double rIn = Math.max(0.1, radius - width);
            double rMid = radius;
            double rOut = radius + width;

            Vec3 p1In = new Vec3(Math.cos(a1) * rIn, Math.sin(a1) * rIn * Math.sin(tilt), Math.sin(a1) * rIn * Math.cos(tilt));
            Vec3 p2In = new Vec3(Math.cos(a2) * rIn, Math.sin(a2) * rIn * Math.sin(tilt), Math.sin(a2) * rIn * Math.cos(tilt));
            Vec3 p1Mid = new Vec3(Math.cos(a1) * rMid, Math.sin(a1) * rMid * Math.sin(tilt), Math.sin(a1) * rMid * Math.cos(tilt));
            Vec3 p2Mid = new Vec3(Math.cos(a2) * rMid, Math.sin(a2) * rMid * Math.sin(tilt), Math.sin(a2) * rMid * Math.cos(tilt));
            Vec3 p1Out = new Vec3(Math.cos(a1) * rOut, Math.sin(a1) * rOut * Math.sin(tilt), Math.sin(a1) * rOut * Math.cos(tilt));
            Vec3 p2Out = new Vec3(Math.cos(a2) * rOut, Math.sin(a2) * rOut * Math.sin(tilt), Math.sin(a2) * rOut * Math.cos(tilt));

            int aIn = (int) (60 * alpha);
            int aMid = (int) (235 * alpha);
            int aOut = 0;

            vertex(v, m, p1In, 210, 45, 230, aIn);
            vertex(v, m, p2In, 210, 45, 230, aIn);
            vertex(v, m, p2Mid, 255, 140, 250, aMid);
            vertex(v, m, p1Mid, 255, 140, 250, aMid);

            vertex(v, m, p1Mid, 255, 140, 250, aMid);
            vertex(v, m, p2Mid, 255, 140, 250, aMid);
            vertex(v, m, p2Out, 220, 30, 210, aOut);
            vertex(v, m, p1Out, 220, 30, 210, aOut);
        }
    }

    private static void vertex(VertexConsumer v, Matrix4f m, Vec3 point, int r, int g, int b, int alpha) {
        double dist = point.distanceTo(currentCameraRel);
        if (dist < 1.4) {
            alpha = 0;
        } else if (dist < 2.8) {
            alpha = (int) (alpha * Math.clamp((dist - 1.4) / 1.4, 0.0, 1.0));
        }
        v.addVertex(m, (float) point.x, (float) point.y, (float) point.z).setColor(r, g, b, Math.clamp(alpha, 0, 255));
    }
}
