package com.minhphuc.weapons.client;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.mixin.GameRendererAccessor;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Xử lý hiệu ứng Manga Trắng Đen (Monochrome / Grayscale Shader) cho chiêu Nghịch Thời Nhất Trảm:
 * - Khi kích hoạt (-0.04s ➔ 0.00s): Màn hình chuyển thành màu đen trắng tương phản cao chuẩn phong cách manga Dark Gathering.
 * - Khi đếm ngược về 0.00s: Shader tự động tắt, màu sắc thế giới bừng sáng trở lại và sát thương phát nổ!
 * - Tối ưu tuyệt đối (Zero Lag): Sử dụng pipeline color_convolve gốc của Minecraft engine.
 */
public class ClientTengaiMangaHandler {
    private static final ResourceLocation DESATURATE_SHADER = ResourceLocation.fromNamespaceAndPath("minecraft", "shaders/post/desaturate.json");
    private static final ResourceLocation MONOCHROME_SHADER = ResourceLocation.fromNamespaceAndPath("minecraft", "shaders/post/monochrome.json");
    private static final ResourceLocation WEAPONS_MONOCHROME_SHADER = ResourceLocation.fromNamespaceAndPath("weapons", "shaders/post/monochrome.json");

    private static int activeTicksRemaining = 0;
    private static boolean shaderActive = false;

    public static void triggerMangaEffect(int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        activeTicksRemaining = durationTicks;
        if (!shaderActive && mc.gameRenderer != null) {
            try {
                try {
                    ((GameRendererAccessor) mc.gameRenderer).weapons$loadEffect(MONOCHROME_SHADER);
                } catch (Throwable t1) {
                    try {
                        ((GameRendererAccessor) mc.gameRenderer).weapons$loadEffect(WEAPONS_MONOCHROME_SHADER);
                    } catch (Throwable t2) {
                        ((GameRendererAccessor) mc.gameRenderer).weapons$loadEffect(DESATURATE_SHADER);
                    }
                }
                shaderActive = true;
                WeaponsMod.LOGGER.info("[Tengai Manga Shader] Monochrome/Desaturate shader activated for {} ticks!", durationTicks);
            } catch (Throwable e) {
                WeaponsMod.LOGGER.error("[Tengai Manga Shader] Failed to load monochrome shader", e);
            }
        }
    }

    public static void stopMangaEffect() {
        activeTicksRemaining = 0;
        if (shaderActive) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameRenderer != null) {
                try {
                    mc.gameRenderer.shutdownEffect();
                    WeaponsMod.LOGGER.info("[Tengai Manga Shader] Monochrome shader deactivated.");
                } catch (Throwable ignored) {
                }
            }
            shaderActive = false;
        }
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.level == null) {
                if (shaderActive) {
                    stopMangaEffect();
                }
                return;
            }

            if (activeTicksRemaining > 0) {
                activeTicksRemaining--;
                if (activeTicksRemaining == 0) {
                    stopMangaEffect();
                }
            }
        });
    }
}
