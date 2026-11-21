package com.minhphuc.weapons.client;

import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundSageActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Giao diện Subtitle Dialogue Banner chuẩn Anime Tensura cho:
 *  - Kỹ Năng Độc Nhất « Đại Hiền Giả » (Great Sage)
 *  - Kỹ Năng Tối Thượng « Thông Tuệ Vương Raphael » (Lord of Wisdom Raphael)
 *
 * Tính năng:
 *  1. Hiển thị chữ từ từ (Typewriter Animation) mượt mà (~35ms/ký tự).
 *  2. Con trỏ ma pháp nhấp nháy ▌ & âm thanh gõ ma trận nhẹ nhàng.
 *  3. Thời gian tồn tại thông minh theo độ dài văn bản (người chơi đọc kịp 100%).
 *  4. Vị trí Subtitle Banner ở phía dưới giữa màn hình (hoàn toàn không che Boss Bar).
 *  5. Tương tác 1-chạm nhanh: Bấm [Y] Đồng Ý ngay, [N] Bỏ Qua.
 *  6. Biểu tượng Hologram 3D / Vòng Hào Quang Trí Tuệ xoay tròn có linh hồn.
 */
public final class SageHudManager {

    public record SageMessage(
            String speaker,
            String text,
            int accentColor,
            long createAtMs,
            long expireAtMs,
            boolean offerVeldoraWarp,
            int actionId
    ) {
        public boolean hasAction() {
            return offerVeldoraWarp || actionId > 0;
        }
    }

    private static final Deque<SageMessage> QUEUE = new ArrayDeque<>();
    private static SageMessage currentActive = null;
    private static long lastChimeTimeMs = 0L;

    private SageHudManager() {}

    public static void pushAlert(String speaker, String text, int accentColor, int durationMs, boolean offerVeldoraWarp) {
        pushAlert(speaker, text, accentColor, durationMs, offerVeldoraWarp, offerVeldoraWarp ? ServerboundSageActionPacket.ACTION_WARP_VELDORA : 0);
    }

    public static void pushAlert(String speaker, String text, int accentColor, int durationMs, boolean offerVeldoraWarp, int actionId) {
        long now = System.currentTimeMillis();
        // Thời gian tồn tại thông minh: đảm bảo tốc độ đọc chuẩn anime + thời gian lưu sau khi gõ xong
        int smartDuration = Math.max(durationMs, text.length() * 42 + 4200);

        synchronized (QUEUE) {
            if (QUEUE.size() >= 4) QUEUE.pollFirst();
            QUEUE.addLast(new SageMessage(speaker, text, accentColor, now, now + smartDuration, offerVeldoraWarp, actionId));
        }

        // Âm thanh khẽ gõ ma trận khi bắt đầu có lời thoại mới
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && now - lastChimeTimeMs > 400L) {
            lastChimeTimeMs = now;
            mc.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.45F, 1.8F);
        }
    }

    public static boolean hasPendingProposal() {
        return currentActive != null && currentActive.hasAction() && currentActive.expireAtMs() > System.currentTimeMillis();
    }

    public static void confirmPendingProposal() {
        if (!hasPendingProposal()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (currentActive.offerVeldoraWarp() || currentActive.actionId() == ServerboundSageActionPacket.ACTION_WARP_VELDORA) {
            ModMessages.sendToServer(new ServerboundSageActionPacket(ServerboundSageActionPacket.ACTION_WARP_VELDORA));
        } else if (currentActive.actionId() > 0) {
            ModMessages.sendToServer(new ServerboundSageActionPacket(currentActive.actionId()));
        }

        mc.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.75F, 1.7F);
        dismissCurrent();
    }

    public static void declinePendingProposal() {
        if (currentActive == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
        }
        dismissCurrent();
    }

    public static void dismissCurrent() {
        synchronized (QUEUE) {
            currentActive = null;
        }
    }

    public static boolean hasPendingVeldoraWarp() {
        return hasPendingProposal() && currentActive != null && currentActive.offerVeldoraWarp();
    }

    public static void clearVeldoraWarp() {
        dismissCurrent();
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        long now = System.currentTimeMillis();
        synchronized (QUEUE) {
            if (currentActive == null || currentActive.expireAtMs() <= now) {
                if (!QUEUE.isEmpty()) {
                    currentActive = QUEUE.pollFirst();
                } else {
                    currentActive = null;
                }
            }
        }

        SageMessage active = currentActive;
        if (active == null) return;

        var font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        // 1. TÍNH TOÁN HIỆU ỨNG GÕ CHỮ (TYPEWRITER ANIMATION)
        long elapsed = Math.max(0L, now - active.createAtMs());
        int totalChars = active.text().length();
        int charsRevealed = (int) Math.min(totalChars, elapsed / 34L);
        boolean isTyping = charsRevealed < totalChars;

        String visibleText = active.text().substring(0, charsRevealed);
        String cursor = isTyping ? ((now / 220L % 2L == 0L) ? "§b▌" : "§7▌") : "";

        // 2. TÍNH TOÁN FADE-IN & FADE-OUT
        float alpha = 1.0F;
        if (elapsed < 240L) {
            alpha = elapsed / 240.0F;
        } else if (active.expireAtMs() - now < 350L) {
            alpha = Math.max(0.0F, (active.expireAtMs() - now) / 350.0F);
        }
        alpha = Mth.clamp(alpha, 0.05F, 1.0F);

        // 3. KÍCH THƯỚC VÀ BỐ CỤC BANNER (PHÍA DƯỚI GIỮA MÀN HÌNH - ANIME SUBTITLE)
        boolean hasAction = active.hasAction();
        int maxBannerW = Math.min(430, screenW - 28);
        int avatarAreaW = 34;
        int actionAreaW = hasAction ? 76 : 0;
        int textAreaW = maxBannerW - avatarAreaW - actionAreaW - 18;

        List<FormattedCharSequence> lines = font.split(Component.literal(visibleText + cursor), textAreaW);
        int bannerH = Math.max(42, 22 + lines.size() * 10);

        boolean isChatOpen = (mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen);
        int bx = (screenW - maxBannerW) / 2;
        // Khi người chơi mở khung chat hoặc gõ lệnh (/), nâng banner lên trên cao (y = 28) để không bị ô chat hay menu gợi ý lệnh che khuất
        int by = isChatOpen ? 28 : (screenH - bannerH - 52);

        // 4. VẼ NỀN THỦY TINH ĐEN MA PHÁP & VIỀN HÀO QUANG
        int bgAlpha = (int) (210 * alpha);
        int bgColor = (bgAlpha << 24) | 0x050C16;
        int frameAlpha = (int) (230 * alpha);
        int borderColor = (frameAlpha << 24) | (active.accentColor() & 0x00FFFFFF);

        // Đổ bóng & Nền banner
        g.fill(bx, by, bx + maxBannerW, by + bannerH, bgColor);
        // Viền trên & dưới phát sáng
        g.fill(bx, by, bx + maxBannerW, by + 1, borderColor);
        g.fill(bx, by + bannerH - 1, bx + maxBannerW, by + bannerH, borderColor);
        g.fill(bx, by, bx + 1, by + bannerH, borderColor);
        g.fill(bx + maxBannerW - 1, by, bx + maxBannerW, by + bannerH, borderColor);

        // Viền trang trí góc Tensura Anime
        g.fill(bx + 1, by + 1, bx + 7, by + 2, 0xFF00E5FF);
        g.fill(bx + 1, by + 1, bx + 2, by + 7, 0xFF00E5FF);
        g.fill(bx + maxBannerW - 7, by + 1, bx + maxBannerW - 1, by + 2, 0xFF00E5FF);
        g.fill(bx + maxBannerW - 2, by + 1, bx + maxBannerW - 1, by + 7, 0xFF00E5FF);

        // 5. VẼ HÌNH CHIẾU HOLOGRAM 3D / VÒNG HÀO QUANG TRÍ TUỆ (BÊN TRÁI)
        int cx = bx + 18;
        int cy = by + bannerH / 2;
        renderWisdomHalo(g, cx, cy, active.speaker().contains("RAPHAEL"), now, alpha);

        // 6. VẼ TIÊU ĐỀ NGƯỜI NÓI (SPEAKER TITLE & TAG)
        int tx = bx + avatarAreaW + 6;
        int ty = by + 6;
        g.drawString(font, active.speaker(), tx, ty, active.accentColor(), true);

        // 7. VẼ CÁC DÒNG VĂN BẢN ĐỐI THOẠI (TYPEWRITER STREAM)
        int ly = ty + 12;
        for (var seq : lines) {
            g.drawString(font, seq, tx, ly, 0xFFF8FAFC, false);
            ly += 10;
        }

        // 8. VẼ NÚT PHẢN HỒI NHANH 1-CHẠM [Y] ĐỒNG Ý / [N] BỎ QUA (NẾU CÓ ĐỀ XUẤT)
        if (hasAction) {
            int actionX = bx + maxBannerW - actionAreaW - 6;
            int actionY = by + (bannerH - 30) / 2;

            // Nút [Y] Đồng ý (Nhấp nháy nhẹ)
            boolean yPulse = (now / 300L % 2L == 0L);
            int yBorder = yPulse ? 0xFF22C55E : 0xFF16A34A;
            g.fill(actionX, actionY, actionX + 70, actionY + 13, 0xD0052E16);
            g.renderOutline(actionX, actionY, 70, 13, yBorder);
            g.drawCenteredString(font, "§a[Y] §fĐồng ý", actionX + 35, actionY + 3, 0xFFFFFFFF);

            // Nút [N] Bỏ qua
            g.fill(actionX, actionY + 16, actionX + 70, actionY + 29, 0xC01E293B);
            g.renderOutline(actionX, actionY + 16, 70, 13, 0xFF475569);
            g.drawCenteredString(font, "§c[N] §7Bỏ qua", actionX + 35, actionY + 19, 0xFFE2E8F0);
        }
    }

    /**
     * Vẽ Vòng Hào Quang Trí Tuệ (Halo of Wisdom) / Quầng Sáng Ma Trận Biểu Tượng của Raphael và Đại Hiền Giả.
     */
    private static void renderWisdomHalo(GuiGraphics g, int cx, int cy, boolean isRaphael, long now, float alpha) {
        double rot = (now / 180.0D);
        int color = isRaphael ? 0xFF00E5FF : 0xFF38BDF8;

        // Vòng tròn trung tâm
        for (int i = 0; i < 8; i++) {
            double angle = rot + i * (Math.PI / 4.0D);
            int px = cx + (int) (Math.cos(angle) * 9.0D);
            int py = cy + (int) (Math.sin(angle) * 9.0D);
            g.fill(px, py, px + 2, py + 2, color);
        }

        if (isRaphael) {
            // Mắt đỏ rực tri thức (Ruby Gaze) của Raphael khi tính toán
            int eyeColor = (now / 350L % 2L == 0L) ? 0xFFE11D48 : 0xFFFB7185;
            g.fill(cx - 2, cy - 2, cx + 2, cy + 2, eyeColor);
            // Vành hào quang phụ
            for (int i = 0; i < 4; i++) {
                double a2 = -rot * 0.7D + i * (Math.PI / 2.0D);
                int ox = cx + (int) (Math.cos(a2) * 5.0D);
                int oy = cy + (int) (Math.sin(a2) * 5.0D);
                g.fill(ox, oy, ox + 1, oy + 1, 0xFFFACC15);
            }
        } else {
            // Lõi ma trận Đại Hiền Giả (Khối cyan logic)
            g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF38BDF8);
        }
    }
}
