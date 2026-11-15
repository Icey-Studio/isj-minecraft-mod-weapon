package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.tensura.RaphaelSkillRegistry;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundRaphaelSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Giao diện Trí Tuệ Vương Raphael (50 Kỹ Năng chia 5 Nhóm × 10 Chiêu Thức).
 * Truy cập trực tiếp từ Bánh Xe Kỹ Năng [G].
 */
public class RaphaelSkillScreen extends Screen {

    private static final int PANEL_W = 464;
    private static final int PANEL_H = 268;
    private static int selectedTab = 0;
    private final boolean autoBattle;

    public RaphaelSkillScreen(boolean autoBattle) {
        super(Component.literal("Trí Tuệ Vương Raphael — 50 Kỹ Năng"));
        this.autoBattle = autoBattle;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Nền & Viền Lam-Vàng Trí Tuệ Vương
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF040B16);
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFF38BDF8);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0081326);

        // Header
        g.drawString(font, "§b§l<< TRÍ TUỆ VƯƠNG RAPHAEL >> §7(50 Kỹ Năng)", left + 10, top + 8, 0xFFFFFFFF, false);
        g.drawString(font, autoBattle ? "§a● Auto-Battle: BẬT" : "§7○ Auto-Battle: TẮT", left + PANEL_W - 148, top + 8, 0xFFFFFFFF, false);

        // Nút Quay lại [G]
        boolean hoverBack = isInRect(mouseX, mouseY, left + PANEL_W - 44, top + 5, 34, 15);
        g.fill(left + PANEL_W - 44, top + 5, left + PANEL_W - 10, top + 20, hoverBack ? 0xFF1E3A5F : 0xFF0F2540);
        g.drawCenteredString(font, "§e◀ [G]", left + PANEL_W - 27, top + 9, 0xFFFFFFFF);

        // 5 Category Tabs
        int tabY = top + 25;
        int tabW = 86;
        RaphaelSkillRegistry.Category[] cats = RaphaelSkillRegistry.Category.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = left + 10 + i * (tabW + 3);
            boolean active = (i == selectedTab);
            boolean hov = isInRect(mouseX, mouseY, tx, tabY, tabW, 18);
            int bg = active ? 0xFF163A5C : (hov ? 0xFF112942 : 0xFF0B192B);
            g.fill(tx, tabY, tx + tabW, tabY + 18, bg);
            g.fill(tx, tabY + 16, tx + tabW, tabY + 18, active ? (0xFF000000 | cats[i].color) : 0xFF334155);
            g.drawCenteredString(font, (active ? "§f§l" : "§7") + cats[i].title + " (10)", tx + tabW / 2, tabY + 5, 0xFFFFFFFF);
        }

        // 2x5 Grid of 10 skills for selected tab
        RaphaelSkillRegistry.Category curCat = cats[selectedTab];
        List<RaphaelSkillRegistry.RaphaelSkill> group = RaphaelSkillRegistry.SKILLS.stream()
                .filter(s -> s.category() == curCat)
                .toList();

        int gridTop = top + 49;
        int cardW = 216;
        int cardH = 36;
        String hoverTip = null;

        for (int i = 0; i < group.size() && i < 10; i++) {
            RaphaelSkillRegistry.RaphaelSkill sk = group.get(i);
            int col = i % 2;
            int row = i / 2;
            int cx = left + 10 + col * (cardW + 12);
            int cy = gridTop + row * (cardH + 5);
            boolean hov = isInRect(mouseX, mouseY, cx, cy, cardW, cardH);
            if (hov) {
                hoverTip = "§b" + sk.name() + "\n§f" + sk.description();
            }

            g.fill(cx, cy, cx + cardW, cy + cardH, hov ? 0xFF173659 : 0xFF0E2138);
            g.fill(cx, cy, cx + 3, cy + cardH, 0xFF000000 | curCat.color);

            String titleLine = font.plainSubstrByWidth(sk.name(), cardW - 14);
            String descLine = font.plainSubstrByWidth(sk.description(), cardW - 14);
            g.drawString(font, "§f" + titleLine, cx + 7, cy + 5, 0xFFFFFFFF, false);
            g.drawString(font, "§7" + descLine, cx + 7, cy + 19, 0xFF94A3B8, false);
        }

        // Footer hint
        g.drawCenteredString(font, "§8[Click vào kỹ năng để Trí Tuệ Vương Raphael thi triển tức thì • Chọn Tab để xem đủ 50 chiêu]", left + PANEL_W / 2, top + PANEL_H - 13, 0xFF64748B);

        if (hoverTip != null) {
            g.renderTooltip(font, Component.literal(hoverTip), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        if (isInRect(mouseX, mouseY, left + PANEL_W - 44, top + 5, 34, 15)) {
            playClick();
            Minecraft.getInstance().setScreen(new SkillWheelScreen());
            return true;
        }

        int tabY = top + 25;
        int tabW = 86;
        RaphaelSkillRegistry.Category[] cats = RaphaelSkillRegistry.Category.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = left + 10 + i * (tabW + 3);
            if (isInRect(mouseX, mouseY, tx, tabY, tabW, 18)) {
                selectedTab = i;
                playClick();
                return true;
            }
        }

        RaphaelSkillRegistry.Category curCat = cats[selectedTab];
        List<RaphaelSkillRegistry.RaphaelSkill> group = RaphaelSkillRegistry.SKILLS.stream()
                .filter(s -> s.category() == curCat)
                .toList();

        int gridTop = top + 49;
        int cardW = 216;
        int cardH = 36;
        for (int i = 0; i < group.size() && i < 10; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = left + 10 + col * (cardW + 12);
            int cy = gridTop + row * (cardH + 5);
            if (isInRect(mouseX, mouseY, cx, cy, cardW, cardH)) {
                ModMessages.sendToServer(new ServerboundRaphaelSkillPacket(group.get(i).id()));
                playClick();
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void playClick() {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 1.3F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
