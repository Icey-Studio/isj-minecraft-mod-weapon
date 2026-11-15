package com.minhphuc.weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * HUD góc màn hình phong cách Anime Slime cho [Đại Hiền Giả] & [Trí Tuệ Vương Raphael].
 * Hiển thị thông báo ngắn gọn, tự cuộn & tan biến mượt mà mà không che tầm nhìn chiến đấu.
 */
public final class SageHudManager {

    public record SageMessage(String speaker, String text, int accentColor, long expireAtMs, boolean offerVeldoraWarp) {}

    private static final Deque<SageMessage> QUEUE = new ArrayDeque<>();
    private static boolean hasPendingVeldoraWarp = false;

    private SageHudManager() {}

    public static void pushAlert(String speaker, String text, int accentColor, int durationMs, boolean offerVeldoraWarp) {
        long now = System.currentTimeMillis();
        synchronized (QUEUE) {
            if (QUEUE.size() >= 4) QUEUE.pollFirst();
            QUEUE.addLast(new SageMessage(speaker, text, accentColor, now + durationMs, offerVeldoraWarp));
        }
        if (offerVeldoraWarp) {
            hasPendingVeldoraWarp = true;
        }
    }

    public static boolean hasPendingVeldoraWarp() {
        return hasPendingVeldoraWarp;
    }

    public static void clearVeldoraWarp() {
        hasPendingVeldoraWarp = false;
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        long now = System.currentTimeMillis();
        SageMessage active;
        synchronized (QUEUE) {
            while (!QUEUE.isEmpty() && QUEUE.peekFirst().expireAtMs() < now) {
                QUEUE.pollFirst();
            }
            active = QUEUE.peekLast();
        }
        if (active == null) return;

        var font = mc.font;
        int screenW = guiGraphics.guiWidth();
        int boxW = Math.min(240, screenW / 2 - 12);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(net.minecraft.network.chat.Component.literal(active.text()), boxW - 16);
        int boxH = 22 + lines.size() * 10 + (active.offerVeldoraWarp() ? 12 : 0);

        int x = screenW - boxW - 10;
        int y = 10;

        float rem = Math.min(1.0F, (active.expireAtMs() - now) / 450.0F);
        int bgAlpha = (int) (185 * Mth.clamp(rem, 0.1F, 1.0F));
        int bgColor = (bgAlpha << 24) | 0x0A1020;

        guiGraphics.fill(x, y, x + boxW, y + boxH, bgColor);
        guiGraphics.fill(x, y, x + boxW, y + 2, active.accentColor());
        guiGraphics.fill(x, y + boxH - 1, x + boxW, y + boxH, active.accentColor());
        guiGraphics.fill(x, y, x + 2, y + boxH, active.accentColor());

        guiGraphics.drawString(font, active.speaker(), x + 8, y + 6, 0xFF7DF9FF, true);
        int ly = y + 18;
        for (var seq : lines) {
            guiGraphics.drawString(font, seq, x + 8, ly, 0xFFF1F5F9, false);
            ly += 10;
        }
        if (active.offerVeldoraWarp()) {
            guiGraphics.drawString(font, "§e✦ Bấm §a[V]§e để Dịch Chuyển tới Hang Phong Ấn!", x + 8, ly + 1, 0xFFFFE066, true);
        }
    }
}
