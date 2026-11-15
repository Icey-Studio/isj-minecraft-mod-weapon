package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.tensura.AzathothStomachData;
import com.minhphuc.weapons.content.tensura.MagiculesManager;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundAzathothActionPacket;
import com.minhphuc.weapons.network.ServerboundSageActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Giao diện [Dạ Dày Vô Hạn & Không Gian Ảo - Thần Hư Không Azathoth].
 * Hỗ trợ tới 120 ô phân trang, Auto-Loot Hư Không, Chuyển Hóa tăng Ma Tố/EP vĩnh viễn,
 * và click trực tiếp 36 ô Túi Đồ để cất thẳng vào Không Gian Ảo!
 */
public class AzathothStomachScreen extends Screen {

    private static final int PANEL_W = 424;
    private static final int PANEL_H = 266;
    private static final int ROWS_PER_PAGE = 7;

    private static int savedPage = 0;

    private final List<AzathothStomachData.StomachEntry> entries;
    private final boolean veldoraUnlocked;
    private final boolean autoLoot;
    private int selectedIndex = 0;
    private int currentPage = 0;

    public AzathothStomachScreen(List<AzathothStomachData.StomachEntry> entries, boolean veldoraUnlocked, boolean autoLoot) {
        super(Component.literal("§5§l[DẠ DÀY VÔ HẠN & KHÔNG GIAN ẢO]"));
        this.entries = entries;
        this.veldoraUnlocked = veldoraUnlocked;
        this.autoLoot = autoLoot;
        int totalPages = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        this.currentPage = Math.max(0, Math.min(savedPage, totalPages - 1));
        this.selectedIndex = entries.isEmpty() ? 0 : Math.min(this.currentPage * ROWS_PER_PAGE, entries.size() - 1);
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Phân trang danh sách Dạ Dày (dưới khung danh sách bên trái)
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (currentPage > 0) {
                currentPage--;
                savedPage = currentPage;
                selectedIndex = currentPage * ROWS_PER_PAGE;
            }
        }).bounds(left + 10, top + 132, 22, 16).build());

        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            int totalPages = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                savedPage = currentPage;
                selectedIndex = currentPage * ROWS_PER_PAGE;
            }
        }).bounds(left + 196, top + 132, 22, 16).build());

        // Nút Cất Hết Ba-Lô & Quét / Giải Mã
        addRenderableWidget(Button.builder(Component.literal("§dCất Ba-Lô (27 Ô)"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_DEPOSIT_BACKPACK, 0)))
                .bounds(left + 10, top + 151, 102, 17).build());

        addRenderableWidget(Button.builder(Component.literal("§3Quét 24m"),
                b -> ModMessages.sendToServer(new ServerboundSageActionPacket(ServerboundSageActionPacket.ACTION_SCAN_AREA)))
                .bounds(left + 115, top + 151, 50, 17).build());

        addRenderableWidget(Button.builder(Component.literal("§aThanh Tẩy"),
                b -> ModMessages.sendToServer(new ServerboundSageActionPacket(ServerboundSageActionPacket.ACTION_DECODE_CURSE)))
                .bounds(left + 168, top + 151, 50, 17).build());

        // Cột nút điều khiển bên phải (186px wide)
        int bx = left + 228;
        int bw = 186;
        addRenderableWidget(Button.builder(Component.literal("§dNuốt Mục Tiêu / Vật Rơi (Tầm Nhìn)"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_SWALLOW, 0)))
                .bounds(bx, top + 26, bw, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§bPhân Tích & Giải Ấn Veldora"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_ANALYZE_UNSEAL_VELDORA, selectedIndex)))
                .bounds(bx, top + 46, bw, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§eGiải Phóng Về Túi Đồ"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_RELEASE, selectedIndex)))
                .bounds(bx, top + 66, bw, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§aChuyển Hóa -> Ma Tố (+MP/EP)"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_CONVERT_MAGICULES, selectedIndex)))
                .bounds(bx, top + 86, bw, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§6Craft: Táo Vàng"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_AUTO_CRAFT, 0)))
                .bounds(bx, top + 106, 91, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§5Craft: Netherite"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_AUTO_CRAFT, 1)))
                .bounds(bx + 95, top + 106, 91, 18).build());

        String autoLootLabel = autoLoot ? "§dAuto-Loot Hư Không: §aBẬT" : "§7Auto-Loot Hư Không: §cTẮT";
        addRenderableWidget(Button.builder(Component.literal(autoLootLabel),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_TOGGLE_AUTOLOOT, 0)))
                .bounds(bx, top + 126, bw, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§3Gọi Veldora (Rồng)"),
                b -> {
                    onClose();
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_SUMMON_VELDORA_DRAGON, 0));
                }).bounds(bx, top + 148, 91, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§bGọi Veldora (Người)"),
                b -> {
                    onClose();
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_SUMMON_VELDORA_HUMAN, 0));
                }).bounds(bx + 95, top + 148, 91, 18).build());

        addRenderableWidget(Button.builder(Component.literal("§cThu Hồi / Hồi Sinh Veldora"),
                b -> {
                    onClose();
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_RECALL_OR_REVIVE_VELDORA, 0));
                }).bounds(bx, top + 168, bw, 18).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Click chọn mục trong danh sách Không Gian Ảo
        int listX = left + 12;
        int listY = top + 28;
        if (mouseX >= listX && mouseX <= listX + 206 && mouseY >= listY && mouseY <= listY + ROWS_PER_PAGE * 14) {
            int row = (int) ((mouseY - listY) / 14);
            int idx = currentPage * ROWS_PER_PAGE + row;
            if (idx >= 0 && idx < entries.size()) {
                selectedIndex = idx;
                return true;
            }
        }

        // Click bất kỳ ô nào trong 36 ô Túi Đồ để đưa thẳng vào Dạ Dày Vô Hạn
        if (this.minecraft != null && this.minecraft.player != null) {
            int invStartX = left + 14;
            int invStartY = top + 182;
            for (int r = 0; r < 4; r++) {
                int yOffset = invStartY + r * 18 + (r == 3 ? 3 : 0);
                for (int c = 0; c < 9; c++) {
                    int sx = invStartX + c * 18;
                    if (mouseX >= sx && mouseX < sx + 17 && mouseY >= yOffset && mouseY < yOffset + 17) {
                        int invSlot = (r < 3) ? (9 + r * 9 + c) : c;
                        ItemStack stack = this.minecraft.player.getInventory().getItem(invSlot);
                        if (!stack.isEmpty()) {
                            ModMessages.sendToServer(new ServerboundAzathothActionPacket(
                                    ServerboundAzathothActionPacket.ACTION_DEPOSIT_SLOT, invSlot));
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Override vanilla 1.21.1 blur pass so stomach list & text stay razor-sharp
        g.fill(0, 0, this.width, this.height, 0x99060913);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE0B0E18);
        g.fill(left, top, left + PANEL_W, top + 2, 0xFFA855F7);
        g.renderOutline(left, top, PANEL_W, PANEL_H, 0x66A855F7);

        g.drawString(font, "§5§l✦ THẦN HƯ KHÔNG AZATHOTH: KHÔNG GIAN ẢO §7(" + entries.size() + "/" + AzathothStomachData.MAX_SLOTS + " ô)",
                left + 12, top + 9, 0xFFFFFFFF, true);

        // Khung danh sách bên trái
        g.fill(left + 10, top + 26, left + 218, top + 129, 0xCC131826);
        int totalPages = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        if (entries.isEmpty()) {
            g.drawString(font, "§7Không Gian Ảo hiện đang trống.", left + 18, top + 38, 0xFF94A3B8, false);
            g.drawString(font, "§7• Click bất kỳ ô Túi Đồ bên dưới", left + 18, top + 52, 0xFFCBD5E1, false);
            g.drawString(font, "§7  để cất thẳng vào Dạ Dày!", left + 18, top + 64, 0xFFCBD5E1, false);
            g.drawString(font, "§7• Hoặc bật Auto-Loot Hư Không.", left + 18, top + 78, 0xFFD8B4FE, false);
        } else {
            int start = currentPage * ROWS_PER_PAGE;
            int end = Math.min(entries.size(), start + ROWS_PER_PAGE);
            for (int i = start; i < end; i++) {
                var e = entries.get(i);
                int row = i - start;
                int ry = top + 30 + row * 14;
                if (i == selectedIndex) {
                    g.fill(left + 12, ry - 1, left + 216, ry + 11, 0x669333EA);
                }
                String prefix = "ENTITY".equals(e.kind()) ? "§c[Quái] " : "§b[Đồ] ";
                String label = font.plainSubstrByWidth(prefix + "§f" + e.name(), 196);
                g.drawString(font, label, left + 15, ry, 0xFFFFFFFF, false);
            }
        }
        g.drawCenteredString(font, "§7Trang " + (currentPage + 1) + "/" + totalPages, left + 114, top + 136, 0xFFCBD5E1);

        // Thanh Túi Đồ Cá Nhân (gồm 27 ô Ba-lô + 9 ô Hotbar)
        g.drawString(font, "§e✦ Click ô trong Túi Đồ để cất vào Không Gian Ảo:", left + 14, top + 171, 0xFFFFEA80, false);
        ItemStack hoveredStack = ItemStack.EMPTY;
        if (this.minecraft != null && this.minecraft.player != null) {
            int invStartX = left + 14;
            int invStartY = top + 182;
            for (int r = 0; r < 4; r++) {
                int yOffset = invStartY + r * 18 + (r == 3 ? 3 : 0);
                for (int c = 0; c < 9; c++) {
                    int sx = invStartX + c * 18;
                    int invSlot = (r < 3) ? (9 + r * 9 + c) : c;
                    ItemStack st = this.minecraft.player.getInventory().getItem(invSlot);
                    boolean hov = mouseX >= sx && mouseX < sx + 17 && mouseY >= yOffset && mouseY < yOffset + 17;
                    g.fill(sx, yOffset, sx + 17, yOffset + 17, hov ? 0xFF4C1D95 : (r == 3 ? 0xFF261E38 : 0xFF1A1F2E));
                    g.renderOutline(sx, yOffset, 17, 17, hov ? 0xFFD8B4FE : 0xFF334155);
                    if (!st.isEmpty()) {
                        g.renderItem(st, sx + 1, yOffset + 1);
                        g.renderItemDecorations(font, st, sx + 1, yOffset + 1);
                        if (hov) hoveredStack = st;
                    }
                }
            }
        }

        // Bảng Thông Tin Chi Tiết Ô Đang Chọn & Veldora ở góc phải dưới
        int infoX = left + 186;
        int infoY = top + 190;
        g.fill(infoX, infoY, left + PANEL_W - 10, top + PANEL_H - 10, 0xAA111626);
        g.renderOutline(infoX, infoY, (left + PANEL_W - 10) - infoX, (top + PANEL_H - 10) - infoY, 0x5538BDF8);
        if (!entries.isEmpty() && selectedIndex >= 0 && selectedIndex < entries.size()) {
            var sel = entries.get(selectedIndex);
            g.drawString(font, "§dĐang chọn: §f" + font.plainSubstrByWidth(sel.name(), 162), infoX + 6, infoY + 6, 0xFFFFFFFF, false);
            g.drawString(font, "§6EP: §e" + MagiculesManager.formatCompact(sel.epValue()) + "  §8(" + sel.kind() + ")", infoX + 6, infoY + 18, 0xFFFFFFFF, false);
            g.drawString(font, "§bPhân tích: §7" + font.plainSubstrByWidth(sel.analyzedSkill(), 166), infoX + 6, infoY + 30, 0xFFFFFFFF, false);
        } else {
            g.drawString(font, "§7Chưa chọn vật chất / linh thể nào.", infoX + 6, infoY + 14, 0xFF94A3B8, false);
        }
        String vStatus = veldoraUnlocked ? "§a✔ Veldora: Đã Giải Ấn" : "§e⚠ Veldora: Chưa Giải Ấn";
        g.drawString(font, vStatus, infoX + 6, infoY + 48, 0xFFFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick);

        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
