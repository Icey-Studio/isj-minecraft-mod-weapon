package com.minhphuc.weapons.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 4 Impact Frame cực mạnh chuẩn Manga/Anime kết hợp whiteout flash khi vụ nổ bộc phát.
 */
public class CustomImpactFrameOverlay {
    public static final ResourceLocation[] FRAMES = {
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/vfx/impact_frame_0.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/vfx/impact_frame_1.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/vfx/impact_frame_2.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/vfx/impact_frame_3.png")
    };

    public static final int IMPACT_FRAME_TICKS = 8;
    public static final int FADEOUT_TICKS = 6;
    public static final int TOTAL_DURATION = IMPACT_FRAME_TICKS + FADEOUT_TICKS;

    private static int currentTick = 0;
    private static boolean active = false;

    public static void trigger() {
        currentTick = 0;
        active = true;
    }

    public static void reset() {
        active = false;
        currentTick = 0;
    }

    public static void onClientTick() {
        if (active) {
            currentTick++;
            if (currentTick >= TOTAL_DURATION) {
                active = false;
                currentTick = 0;
            }
        }
    }

    public static void render(GuiGraphics context, float tickDelta) {
        if (!active) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        // Hiển thị lần lượt 4 khung hình va chạm âm bản cực đẹp
        if (currentTick < IMPACT_FRAME_TICKS) {
            int frameIndex = Math.min(FRAMES.length - 1, (currentTick * FRAMES.length) / IMPACT_FRAME_TICKS);
            ResourceLocation texture = FRAMES[frameIndex];

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.blit(texture, 0, 0, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight);
        }

        // Chớp trắng chuyển tiếp êm ái vào cột sáng
        if (currentTick >= IMPACT_FRAME_TICKS) {
            float fadeProgress = (float) (currentTick - IMPACT_FRAME_TICKS) / (float) FADEOUT_TICKS;
            fadeProgress = Mth.clamp(fadeProgress, 0.0f, 1.0f);

            float whiteAlpha = 1.0f - fadeProgress;
            whiteAlpha = 0.35f * whiteAlpha * whiteAlpha;

            if (whiteAlpha > 0.002f) {
                int alphaByte = Mth.clamp((int) (whiteAlpha * 255.0f), 0, 255);
                int whiteColor = (alphaByte << 24) | 0x00FFFFFF;
                context.fill(0, 0, screenWidth, screenHeight, whiteColor);
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
