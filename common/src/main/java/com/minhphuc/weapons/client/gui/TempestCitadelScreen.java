package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundTempestCitadelActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện điều khiển Toàn Quyền Thành Trì Jura Tempest:
 * - Dịch chuyển tức thời (Quảng Trường, Ngai Vàng, Onsen, Đấu Trường, Rời Thành)
 * - Chỉnh bầu trời riêng (Trưa / Hoàng Hôn / Đêm Sao)
 * - Quản lý Cư Dân & Thuộc Hạ (Mời Đồng Minh, Ban Danh Xưng +MP/+EP, Auto Sản Xuất Azathoth)
 * - Điều khiển Đấu Trường (Bia Thử DMG Bất Tử, Triệu Hồi 8 Boss Ảo Ảnh, Tái Tạo Sàn)
 */
public class TempestCitadelScreen extends Screen {

    private static final int PANEL_W = 440;
    private static final int PANEL_H = 264;

    private final int residentCount;
    private final int blessedCount;
    private final long bonusEp;
    private final boolean autoProduce;
    private final long dayTime;
    private final boolean insideCitadel;

    private record Btn(int x, int y, int w, int h, String label, String tip, int color, int action, int param) {}

    private final List<Btn> buttons = new ArrayList<>();

    public TempestCitadelScreen(
            int residentCount,
            int blessedCount,
            long bonusEp,
            boolean autoProduce,
            long dayTime,
            boolean insideCitadel
    ) {
        super(Component.literal("Thành Trì Jura Tempest — Bảng Điều Khiển Hoàng Gia"));
        this.residentCount = residentCount;
        this.blessedCount = blessedCount;
        this.bonusEp = bonusEp;
        this.autoProduce = autoProduce;
        this.dayTime = dayTime;
        this.insideCitadel = insideCitadel;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Nút Đóng [X]
        buttons.add(new Btn(left + PANEL_W - 26, top + 6, 18, 16, "§c✕", "Đóng bảng điều khiển", 0xFF7F1D1D, -1, 0));

        // CỘT TRÁI (Điều hướng + Bầu trời + Quản trị Thành Trì)
        int lx = left + 12;
        int colW = 202;

        // 1. Dịch chuyển nội khu (5 nút)
        int y = top + 56;
        buttons.add(new Btn(lx, y, 99, 20, "§b⛩ Quảng Trường", "Về Cổng Thần Môn & Quảng Trường Chiến Thắng", 0xFF0E7490, 0, 0));
        buttons.add(new Btn(lx + 103, y, 99, 20, "§6♛ Ngai Vàng", "Tới Cung Điện Đá Trắng & Hội Đồng Thủy Tổ", 0xFFB45309, 1, 0));
        y += 23;
        buttons.add(new Btn(lx, y, 99, 20, "§d♨ Khu Onsen", "Ngâm suối hồi 3x Magicules & Vườn Anh Đào", 0xFF9D174D, 2, 0));
        buttons.add(new Btn(lx + 103, y, 99, 20, "§c⚔ Đấu Trường", "Sang Đảo Đấu Trường Tối Thượng", 0xFF991B1B, 3, 0));
        y += 23;
        buttons.add(new Btn(lx, y, colW, 18, insideCitadel ? "§e🚪 Rời Thành Trì (Về Thế Giới Ngoài)" : "§a✨ Tiến Vào Thành Trì Jura Tempest",
                insideCitadel ? "Quay về tọa độ thực trước khi bước vào" : "Mở Thần Môn tiến thẳng vào Thành Trì",
                insideCitadel ? 0xFF334155 : 0xFF047857, insideCitadel ? 4 : 0, 0));

        // 2. Bầu trời Thành Trì (3 nút)
        y += 32;
        buttons.add(new Btn(lx, y, 65, 19, "§e☀ Trưa", "Chỉnh bầu trời Thành Trì sang Ban Trưa rực rỡ", 0xFF854D0E, 5, 0));
        buttons.add(new Btn(lx + 68, y, 66, 19, "§6🌇 Hôn", "Chỉnh bầu trời sang Hoàng Hôn vàng đỏ", 0xFF9A3412, 5, 1));
        buttons.add(new Btn(lx + 137, y, 65, 19, "§9🌙 Đêm", "Chỉnh bầu trời sang Đêm Sao huyền ảo", 0xFF1E3A8A, 5, 2));

        // 3. Quốc Chính & Thuộc Hạ (4 nút)
        y += 33;
        buttons.add(new Btn(lx, y, colW, 20,
                autoProduce ? "§a⚙ Auto Kho Azathoth: ĐANG BẬT" : "§7⚙ Auto Kho Azathoth: ĐANG TẮT",
                "Phố Thủ Công tự tinh luyện vật tư gửi thẳng vào Dạ Dày Azathoth",
                autoProduce ? 0xFF065F46 : 0xFF374151, 6, 0));
        y += 23;
        buttons.add(new Btn(lx, y, 99, 20, "§b📦 Tinh Luyện Ngay", "Nhận 1 lượt vật tư thủ công vào Azathoth", 0xFF0369A1, 7, 0));
        buttons.add(new Btn(lx + 103, y, 99, 20, "§d👥 Mời Đồng Minh", "Đưa đồng minh & ác ma quanh bạn vào Thành", 0xFF6D28D9, 8, 0));
        y += 23;
        buttons.add(new Btn(lx, y, colW, 21, "§6✦ Ban Danh Xưng Toàn Thành (+MP/EP)",
                "Ban phúc cho cư dân chưa có tên để tăng vĩnh viễn +350k Max MP & +500k EP mỗi người",
                0xFFD97706, 9, 0));

        // CỘT PHẢI (Đấu Trường Vô Cực & 8 Boss Ảo Ảnh)
        int rx = left + 226;
        int rw = 202;
        int ry = top + 56;

        buttons.add(new Btn(rx, ry, rw, 22, "§e🎯 Đặt Bia Thử Sát Thương Bất Tử",
                "Triệu hồi Bia Thử DMG bất tử đo Sát Thương & Tổng Combo 5s", 0xFFA16207, 10, 0));

        ry += 36;
        String[] bossNames = {
                "Charybdis", "Orc Disaster Geld",
                "Velgrynd (Chân Long)", "Velzard (Bạch Long)",
                "Ma Vương Milim", "Dũng Giả Chronoa",
                "Ác Ma Kubo", "Ma Vương Kubo"
        };
        int[] bossColors = {
                0xFF1E40AF, 0xFF78350F,
                0xFFB91C1C, 0xFF0E7490,
                0xFFBE185D, 0xFFA16207,
                0xFF581C87, 0xFF7F1D1D
        };
        for (int i = 0; i < bossNames.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = rx + col * 102;
            int by = ry + row * 24;
            buttons.add(new Btn(bx, by, 100, 21, "§f" + bossNames[i],
                    "Triệu hồi " + bossNames[i] + " tại tâm Đấu Trường Vô Cực", bossColors[i], 11, i));
        }

        ry += 4 * 24 + 8;
        buttons.add(new Btn(rx, ry, rw, 23, "§a♻ Dọn Sạch Boss & Tái Tạo Sàn Đấu Trường",
                "Xóa toàn bộ Boss đang đấu và khôi phục 100% nền Đấu Trường", 0xFF15803D, 12, 0));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x88030712);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Khung Đá Trắng & Vàng Rimuru
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF071120);
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFFF59E0B);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF50B1628);

        // Header Banner
        g.fill(left, top, left + PANEL_W, top + 34, 0xFF11233D);
        g.fill(left, top + 34, left + PANEL_W, top + 35, 0xFFF59E0B);
        g.drawString(font, "§6§l♛ THÀNH TRÌ JURA TEMPEST", left + 12, top + 7, 0xFFFFFFFF, false);
        String timeStr = (dayTime < 10000) ? "☀ Ban Trưa" : (dayTime < 14000 ? "🌇 Hoàng Hôn" : "🌙 Đêm Sao");
        String status = String.format("§7Cư dân: §b%d §8| §7Danh Xưng: §e%d §8| §7EP Thành Trì: §a+%,d §8| §f%s",
                residentCount, blessedCount, bonusEp, timeStr);
        g.drawString(font, status, left + 12, top + 20, 0xFFFFFFFF, false);

        // Tiêu đề các mục
        g.drawString(font, "§b§l[ ĐIỀU HƯỚNG THÀNH TRÌ ]", left + 12, top + 43, 0xFFFFFFFF, false);
        g.drawString(font, "§e§l[ THỜI GIAN ĐỘC QUYỀN ]", left + 12, top + 129, 0xFFFFFFFF, false);
        g.drawString(font, "§6§l[ QUỐC CHÍNH & THUỘC HẠ ]", left + 12, top + 163, 0xFFFFFFFF, false);

        // Thanh phân cách dọc
        g.fill(left + 219, top + 40, left + 220, top + PANEL_H - 10, 0xFF1E3A5F);

        g.drawString(font, "§c§l[ ĐẤU TRƯỜNG VÔ CỰC ]", left + 226, top + 43, 0xFFFFFFFF, false);
        g.drawString(font, "§c§l[ TRIỆU HỒI 8 BOSS ẢO ẢNH ]", left + 226, top + 82, 0xFFFFFFFF, false);

        String hoveredTip = null;
        for (Btn b : buttons) {
            boolean hov = isInRect(mouseX, mouseY, b.x, b.y, b.w, b.h);
            int border = hov ? 0xFFFDE68A : 0xFF1E293B;
            g.fill(b.x - 1, b.y - 1, b.x + b.w + 1, b.y + b.h + 1, border);
            g.fill(b.x, b.y, b.x + b.w, b.y + b.h, hov ? brighten(b.color) : b.color);
            String txt = font.plainSubstrByWidth(b.label, b.w - 6);
            g.drawCenteredString(font, txt, b.x + b.w / 2, b.y + (b.h - 8) / 2, 0xFFFFFFFF);
            if (hov && b.tip != null && !b.tip.isEmpty()) {
                hoveredTip = b.tip;
            }
        }

        if (hoveredTip != null) {
            g.renderTooltip(font, Component.literal(hoveredTip), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        for (Btn b : buttons) {
            if (isInRect(mouseX, mouseY, b.x, b.y, b.w, b.h)) {
                playClick();
                if (b.action == -1) {
                    this.onClose();
                } else {
                    ModMessages.sendToServer(new ServerboundTempestCitadelActionPacket(b.action, b.param));
                    // Các hành động dịch chuyển hoặc triệu hồi đóng màn hình ngay để thao tác chiến đấu
                    if (b.action <= 4 || b.action == 10 || b.action == 11) {
                        this.onClose();
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int brighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + 28);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + 28);
        int b = Math.min(255, (argb & 0xFF) + 28);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private boolean isInRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void playClick() {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 1.25F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
