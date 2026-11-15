package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.client.ClientPossessionHandler;
import com.minhphuc.weapons.client.ClientSkillStateHandler;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.PrimordialSkillPool;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.init.ModKeyBindings;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundAzathothActionPacket;
import com.minhphuc.weapons.network.ServerboundRaphaelSkillPacket;
import com.minhphuc.weapons.network.ServerboundSkillActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện Chọn Nhanh Kỹ Năng (Skill Wheel / Quick Selector - Phím G):
 * - Hoạt động cho cả Chế độ Nhập Xác Ác Ma lẫn Người chơi (Chân Ma Vương / Thần Linh / Thủy Tổ).
 * - Phân loại theo nhóm, hỗ trợ 4 slot Yêu Thích, hiển thị Cooldown, Bảo Vệ Địa Hình (Safe Mode)
 *   và điều khiển nhanh Ấn Ký Chiến Thuật / Ấn Ký Vĩnh Viễn / Combat Blink.
 */
public class SkillWheelScreen extends Screen {

    private enum Category {
        ALL("Tất cả", 0xE2E8F0),
        ATTACK("Tấn công", 0xF87171),
        DEFENSE("Phòng thủ", 0x38BDF8),
        SPATIAL("Không gian", 0x22D3EE),
        ULTIMATE("Tối thượng", 0xC084FC);

        final String label;
        final int color;

        Category(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private record SkillItem(int id, String name, Category category, String cooldownKey, int accentColor) {}

    private static final int PANEL_W = 440;
    private static final int PANEL_H = 288;
    private static final int COLS = 2;
    private static final int ROWS = 5;
    private static final int PER_PAGE = COLS * ROWS;

    private Category selectedCategory = Category.ALL;
    private int currentPage = 0;

    public SkillWheelScreen() {
        super(Component.literal("Bánh Xe Kỹ Năng Tensura"));
    }

    @Override
    protected void init() {
        super.init();
        ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_REQUEST_SYNC, 0));
    }

    private List<SkillItem> buildAllSkillsForMode() {
        List<SkillItem> list = new ArrayList<>();
        boolean possessing = ClientPossessionHandler.isLocalPlayerPossessing();

        if (possessing) {
            var pData = ClientPossessionHandler.getLocalData();
            DemonType dtype = (pData != null && pData.demonType() != null) ? pData.demonType() : DemonType.NOIR;
            list.add(new SkillItem(0, "Tà Khứ Vũ Thê Tử (Death Streak)", Category.ULTIMATE, "possess_0", 0xC084FC));
            list.add(new SkillItem(1, PrimordialPlayerDataHelper.getSkillName(dtype, 1), Category.ATTACK, "possess_1", dtype.getGlowColor()));
            list.add(new SkillItem(2, PrimordialPlayerDataHelper.getSkillName(dtype, 2), Category.ATTACK, "possess_2", dtype.getGlowColor()));
            list.add(new SkillItem(3, PrimordialPlayerDataHelper.getSkillName(dtype, 3), Category.ATTACK, "possess_3", dtype.getGlowColor()));
            list.add(new SkillItem(4, PrimordialPlayerDataHelper.getSkillName(dtype, 4), Category.ATTACK, "possess_4", dtype.getGlowColor()));
            list.add(new SkillItem(5, PrimordialPlayerDataHelper.getSkillName(dtype, 5), Category.ULTIMATE, "possess_5", 0xF59E0B));
            list.add(new SkillItem(6, "Linh Tử Bộc Phát (Spiritron Outburst)", Category.ATTACK, "possess_6", 0x38BDF8));
            list.add(new SkillItem(7, "Linh Tử Băng Hoại (Disintegration)", Category.ULTIMATE, "possess_7", 0xFACC15));
            list.add(new SkillItem(8, "Bành Trướng Lãnh Địa (Domain)", Category.DEFENSE, "possess_8", 0x818CF8));

            if (pData != null && pData.skillsString() != null && !pData.skillsString().isEmpty()) {
                String[] parts = pData.skillsString().split(",");
                for (int i = 0; i < parts.length; i++) {
                    var entry = PrimordialSkillPool.getSkillById(parts[i].trim());
                    if (entry != null) {
                        Category cat = (entry.id() == PrimordialSkillPool.SKILL_ALKAID_CHAOS || entry.id() == PrimordialSkillPool.SKILL_SPACE_TELEPORT)
                                ? Category.SPATIAL
                                : (entry.id() == PrimordialSkillPool.SKILL_DRAGON_NOVA || entry.id() == PrimordialSkillPool.SKILL_BEELZEBUTH
                                    ? Category.ULTIMATE : Category.ATTACK);
                        list.add(new SkillItem(20 + i, "[Thức Tỉnh] " + entry.displayNameVi(), cat, "possess_" + (20 + i), entry.iconColorHex()));
                    }
                }
            }

            // Contract skills 100..116
            addContractSkillItems(list, true);
        } else {
            boolean hasCreation = ClientSkillStateHandler.hasCreation();
            addLordSkillItems(list, hasCreation);
            if (ClientSkillStateHandler.isPrimordial()) {
                DemonType dtype = ClientSkillStateHandler.getDemonType();
                int offset = hasCreation ? 30 : 29;
                for (int i = 0; i < 7; i++) {
                    Category cat = (i == 0 || i == 5) ? Category.ULTIMATE : Category.ATTACK;
                    list.add(new SkillItem(offset + i, "[Thủy Tổ] " + PrimordialPlayerDataHelper.getSkillName(dtype, i), cat, "lord_" + (offset + i), dtype.getGlowColor()));
                }
            }
        }
        return list;
    }

    private void addContractSkillItems(List<SkillItem> list, boolean possession) {
        String pfx = possession ? "possess_" : "lord_";
        list.add(new SkillItem(100, "Kháng Ma Kết Giới", Category.DEFENSE, pfx + 100, 0x22C55E));
        list.add(new SkillItem(101, "Cú Bắn Granit", Category.ATTACK, pfx + 101, 0xFF6D00));
        list.add(new SkillItem(102, "Zone Track (Đại Pháo Bạch Kim)", Category.ATTACK, pfx + 102, 0xF0F8FF));
        list.add(new SkillItem(103, "Thị Thực: Thị Nhục Mạch Động", Category.DEFENSE, pfx + 103, 0xCC0022));
        list.add(new SkillItem(104, "Phép Màu Của Thần Linh", Category.DEFENSE, pfx + 104, 0x55FFAA));
        list.add(new SkillItem(105, "Đa Trùng Kết Giới", Category.DEFENSE, pfx + 105, 0x38BDF8));
        list.add(new SkillItem(106, "Lồng Giam Vô Hạn", Category.DEFENSE, pfx + 106, 0xFBBF24));
        list.add(new SkillItem(107, "Hắc Viêm Ngục (Hellflare)", Category.ATTACK, pfx + 107, 0x991B1B));
        list.add(new SkillItem(108, "Hắc Viêm Hỏa Cầu", Category.ATTACK, pfx + 108, 0xEE3300));
        list.add(new SkillItem(109, "Lời Chúc Tử Vong", Category.ULTIMATE, pfx + 109, 0x9400D3));
        list.add(new SkillItem(110, "Siêu Tân Tinh (Supernova)", Category.ULTIMATE, pfx + 110, 0xFFD700));
        list.add(new SkillItem(111, "Dị Giới Thần Môn", Category.SPATIAL, pfx + 111, 0x10B981));
        list.add(new SkillItem(112, "Long Tinh Bộc Viêm Bá", Category.ULTIMATE, pfx + 112, 0xEC4899));
        list.add(new SkillItem(113, "Đánh Dấu (Spatial Mark)", Category.SPATIAL, pfx + 113, 0x06B6D4));
        list.add(new SkillItem(114, "Dịch Chuyển (Spatial Teleport)", Category.SPATIAL, pfx + 114, 0x22D3EE));
        list.add(new SkillItem(115, "Cảm Nhận Vạn Năng", Category.SPATIAL, pfx + 115, 0xFACC15));
        list.add(new SkillItem(116, "Ngày Tận Thế (Apocalypse)", Category.ULTIMATE, pfx + 116, 0xE11D48));
    }

    private void addLordSkillItems(List<SkillItem> list, boolean hasCreation) {
        list.add(new SkillItem(0, "Linh Tử Băng Hoại (Disintegration)", Category.ULTIMATE, "lord_0", 0xFACC15));
        list.add(new SkillItem(1, "Tà Khứ Vũ Thê Tử (Jacob's Ladder)", Category.ATTACK, "lord_1", 0x22D3EE));
        list.add(new SkillItem(2, "Phép Màu Của Thần Linh", Category.DEFENSE, "lord_2", 0x4ADE80));
        list.add(new SkillItem(3, "Đức Tin Và Ân Sủng", Category.DEFENSE, "lord_3", 0xE879F9));
        list.add(new SkillItem(4, "Ngày Tận Thế (Apocalypse)", Category.ULTIMATE, "lord_4", 0xE11D48));
        list.add(new SkillItem(5, "Bạo Thực Vương (Beelzebuth)", Category.ULTIMATE, "lord_5", 0x9333EA));
        list.add(new SkillItem(6, "Long Tinh Bộc Viêm Bá (Dragon Nova)", Category.ULTIMATE, "lord_6", 0xF43F5E));
        list.add(new SkillItem(7, "Tuyệt Diệt Tinh Tú (Megiddo)", Category.ATTACK, "lord_7", 0xFBBF24));
        list.add(new SkillItem(8, "Thị Thực (Seer Flesh)", Category.ATTACK, "lord_8", 0x16A34A));
        list.add(new SkillItem(9, "Đánh Dấu (Shift=Vĩnh Viễn)", Category.SPATIAL, "lord_9", 0x06B6D4));
        list.add(new SkillItem(10, "Dịch Chuyển (Shift=Blink)", Category.SPATIAL, "lord_10", 0x22D3EE));
        list.add(new SkillItem(11, "Cú Bắn Granit", Category.ATTACK, "lord_11", 0xF97316));
        list.add(new SkillItem(12, "Nghịch Thời Nhất Trảm", Category.ATTACK, "lord_12", 0xF8FAFC));
        list.add(new SkillItem(13, "Bạt Đao Vận Tốc Ngược", Category.ATTACK, "lord_13", 0xEF4444));
        int off = 0;
        if (hasCreation) {
            list.add(new SkillItem(14, "Sáng Tạo Vật Chất", Category.DEFENSE, "lord_14", 0xF59E0B));
            off = 1;
        }
        list.add(new SkillItem(14 + off, "Kháng Ma Kết Giới", Category.DEFENSE, "lord_" + (14 + off), 0x22C55E));
        list.add(new SkillItem(15 + off, "Đa Trùng Kết Giới", Category.DEFENSE, "lord_" + (15 + off), 0x38BDF8));
        list.add(new SkillItem(16 + off, "Zone Track (Chùm Tia Trắng)", Category.ATTACK, "lord_" + (16 + off), 0xF8FAFC));
        list.add(new SkillItem(17 + off, "Cảm Nhận Vạn Năng", Category.SPATIAL, "lord_" + (17 + off), 0x22D3EE));
        list.add(new SkillItem(18 + off, "Lồng Giam Vô Hạn", Category.DEFENSE, "lord_" + (18 + off), 0xFBBF24));
        list.add(new SkillItem(19 + off, "Phòng Ngự Tuyệt Đối Uriel", Category.DEFENSE, "lord_" + (19 + off), 0xFDE047));
        list.add(new SkillItem(20 + off, "Tiên Tri: Rykuzin Sinka", Category.SPATIAL, "lord_" + (20 + off), 0xFACC15));
        list.add(new SkillItem(21 + off, "Hắc Viêm Ngục (Hellflare)", Category.ATTACK, "lord_" + (21 + off), 0x991B1B));
        list.add(new SkillItem(22 + off, "Hắc Viêm Hỏa Cầu", Category.ATTACK, "lord_" + (22 + off), 0xDC2626));
        list.add(new SkillItem(23 + off, "Lời Chúc Tử Vong", Category.ULTIMATE, "lord_" + (23 + off), 0x9400D3));
        list.add(new SkillItem(24 + off, "Siêu Tân Tinh (Supernova)", Category.ULTIMATE, "lord_" + (24 + off), 0xF59E0B));
        list.add(new SkillItem(25 + off, "Phong Sát Kỹ Năng", Category.DEFENSE, "lord_" + (25 + off), 0xF87171));
        list.add(new SkillItem(26 + off, "Thần Hư Không Azathoth", Category.ULTIMATE, "lord_" + (26 + off), 0x7E22CE));
        list.add(new SkillItem(27 + off, "Ban Danh Xưng", Category.SPATIAL, "lord_" + (27 + off), 0xFBBF24));
        list.add(new SkillItem(28 + off, "Dị Giới Thần Môn", Category.SPATIAL, "lord_" + (28 + off), 0x10B981));
        list.add(new SkillItem(29 + off, "Triệu Hồi Veldora", Category.SPATIAL, "lord_" + (29 + off), 0x38BDF8));
        list.add(new SkillItem(30 + off, "Trí Tuệ Vương Raphael (50)", Category.SPATIAL, "lord_" + (30 + off), 0x00E5FF));
        list.add(new SkillItem(31 + off, "Dạ Dày Vô Hạn Azathoth", Category.ULTIMATE, "lord_" + (31 + off), 0xA855F7));
    }

    private List<SkillItem> getFilteredSkills(List<SkillItem> all) {
        if (selectedCategory == Category.ALL) return all;
        List<SkillItem> filtered = new ArrayList<>();
        for (SkillItem item : all) {
            if (item.category() == selectedCategory) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Trong Minecraft 1.21, super.render() gọi renderBackground() (blur framebuffer);
        // phải gọi trước khi vẽ panel để giao diện không bị làm mờ đè lên.
        super.render(g, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        boolean possessing = ClientPossessionHandler.isLocalPlayerPossessing();
        boolean safeMode = ClientSkillStateHandler.isSafeMode();
        boolean hasTactical = ClientSkillStateHandler.hasTacticalMark();
        boolean hasAnchor = ClientSkillStateHandler.hasAnchorMark();

        // Nền & Viền Obsidian-Vàng
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF0A0612);
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, possessing ? 0xFFA855F7 : 0xFFD4AF37);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE120D1D);

        // Header
        String title = possessing
                ? "§5§l✦ NHẬP XÁC ÁC MA"
                : "§6§l✦ BÁNH XE KỸ NĂNG TENSURA";
        g.drawString(font, title, left + 10, top + 8, 0xFFFFFFFF, false);

        // Nút mở nhanh Trí Tuệ Vương Raphael (50 Kỹ Năng)
        drawActionButton(g, mouseX, mouseY, left + PANEL_W - 224, top + 5, 86, 16, "§b✦ Raphael (50)", 0xFF0B243B, 0xFF00E5FF);

        // Nút Safe Mode (Góc phải trên)
        int safeX = left + PANEL_W - 134;
        int safeY = top + 5;
        int safeW = 124;
        int safeH = 16;
        boolean hoverSafe = isInRect(mouseX, mouseY, safeX, safeY, safeW, safeH);
        g.fill(safeX, safeY, safeX + safeW, safeY + safeH, safeMode ? (hoverSafe ? 0xFF166534 : 0xFF14532D) : (hoverSafe ? 0xFF7F1D1D : 0xFF450A0A));
        g.drawCenteredString(font, safeMode ? "§a🛡 Safe Mode: BẬT" : "§c💥 Safe Mode: TẮT", safeX + safeW / 2, safeY + 4, 0xFFFFFFFF);

        // Thanh phím tắt Không Gian nhanh (Row 2)
        int barY = top + 25;
        drawActionButton(g, mouseX, mouseY, left + 10, barY, 98, 16, "§6✦ Đặt Ấn Ký Vĩnh Viễn", 0xFF292012, 0xFFD97706);
        drawActionButton(g, mouseX, mouseY, left + 111, barY, 88, 16, "§b⚡ Combat Blink", 0xFF0F2633, 0xFF06B6D4);
        drawActionButton(g, mouseX, mouseY, left + 202, barY, 88, 16,
                hasAnchor ? "§e🏠 Về Điểm Neo" : "§7🏠 Chưa có Neo",
                hasAnchor ? 0xFF27220D : 0xFF1B1924, hasAnchor ? 0xFFEAB308 : 0xFF475569);
        drawActionButton(g, mouseX, mouseY, left + 293, barY, 72, 16,
                hasAnchor ? "§6📦 Gửi Vật/SV" : "§7📦 Gửi Vật/SV",
                hasAnchor ? 0xFF2B1C0E : 0xFF1B1924, hasAnchor ? 0xFFF59E0B : 0xFF475569);
        drawActionButton(g, mouseX, mouseY, left + 368, barY, 62, 16,
                hasTactical ? "§b📍 ChiếnThuật" : "§d🎲 Ngẫu Nhiên",
                0xFF162436, 0xFF38BDF8);

        // Thanh Đa Ấn Ký Vĩnh Viễn #1..#5 (Row 2b)
        int anchorY = top + 45;
        List<String> anchorLabels = ClientSkillStateHandler.getAnchorLabels();
        int activeAnchorIdx = ClientSkillStateHandler.getSelectedAnchorIndex();
        String hoverTooltip = null;
        for (int i = 0; i < 5; i++) {
            int ax = left + 11 + i * 84;
            int aw = 82;
            boolean exists = i < anchorLabels.size();
            boolean isActive = exists && (i == activeAnchorIdx);
            boolean hoveredAnchor = isInRect(mouseX, mouseY, ax, anchorY, aw, 18);
            int bg = isActive ? 0xFF36280E : (exists ? (hoveredAnchor ? 0xFF2A2112 : 0xFF1F180D) : (hoveredAnchor ? 0xFF1E1B2B : 0xFF13111D));
            int border = isActive ? 0xFFFACC15 : (exists ? 0xFFD97706 : 0xFF334155);
            String label = exists ? "§6" + anchorLabels.get(i) : "§8[+" + (i + 1) + "] Trống";
            g.fill(ax, anchorY, ax + aw, anchorY + 18, bg);
            if (isActive) {
                g.fill(ax, anchorY, ax + aw, anchorY + 1, border);
                g.fill(ax, anchorY + 17, ax + aw, anchorY + 18, border);
            } else {
                g.fill(ax, anchorY + 17, ax + aw, anchorY + 18, border);
            }
            g.drawCenteredString(font, font.plainSubstrByWidth(label, aw - 4), ax + aw / 2, anchorY + 5, 0xFFFFFFFF);
            if (hoveredAnchor) {
                hoverTooltip = exists
                        ? "§eẤn Ký #" + (i + 1) + ": §fClick = Dịch chuyển tới | §6Shift+Click = Gửi SV/Vật tới | §cChuột phải = Xóa"
                        : "§7Ô Ấn Ký #" + (i + 1) + " trống: Click để khắc Ấn Ký Vĩnh Viễn mới tại điểm nhìn/chân bạn";
            }
        }

        // 5 Tab phân loại (Row 3)
        int tabY = top + 68;
        int tabW = 82;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            int tx = left + 10 + i * (tabW + 3);
            boolean active = (c == selectedCategory);
            boolean hovered = isInRect(mouseX, mouseY, tx, tabY, tabW, 15);
            g.fill(tx, tabY, tx + tabW, tabY + 15, active ? 0xFF3B2667 : (hovered ? 0xFF271D42 : 0xFF1A142B));
            if (active) {
                g.fill(tx, tabY + 14, tx + tabW, tabY + 15, 0xFF000000 | c.color);
            }
            g.drawCenteredString(font, c.label, tx + tabW / 2, tabY + 4, active ? (0xFF000000 | c.color) : 0xFFCBD5E1);
        }

        // Lưới danh sách kỹ năng (2 cột x 5 hàng)
        List<SkillItem> allSkills = buildAllSkillsForMode();
        List<SkillItem> filtered = getFilteredSkills(allSkills);
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PER_PAGE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int currentSelectedId = possessing
                ? (ClientPossessionHandler.getLocalData() != null ? ClientPossessionHandler.getLocalData().selectedSkillIndex() : 0)
                : ClientSkillStateHandler.getLordSelectedIndex();
        List<Integer> favs = ClientSkillStateHandler.getFavoriteSkillIds();

        int gridTop = top + 88;
        int cardW = 206;
        int cardH = 26;
        int startIdx = currentPage * PER_PAGE;

        for (int idx = 0; idx < PER_PAGE; idx++) {
            int dataIdx = startIdx + idx;
            if (dataIdx >= filtered.size()) break;
            SkillItem item = filtered.get(dataIdx);

            int col = idx % COLS;
            int row = idx / COLS;
            int cx = left + 10 + col * (cardW + 8);
            int cy = gridTop + row * (cardH + 4);

            boolean isSelected = (item.id() == currentSelectedId);
            boolean isFav = favs.contains(item.id());
            boolean hovered = isInRect(mouseX, mouseY, cx, cy, cardW, cardH);

            int bg = isSelected ? 0xFF2E1E52 : (hovered ? 0xFF231A3C : 0xFF181326);
            g.fill(cx, cy, cx + cardW, cy + cardH, bg);
            g.fill(cx, cy, cx + 3, cy + cardH, 0xFF000000 | item.accentColor());
            if (isSelected) {
                g.fill(cx, cy, cx + cardW, cy + 1, 0xFFFFD700);
                g.fill(cx, cy + cardH - 1, cx + cardW, cy + cardH, 0xFFFFD700);
            }

            // Tên skill (cắt gọn nếu quá dài)
            String dispName = font.plainSubstrByWidth(item.name(), cardW - 58);
            g.drawString(font, dispName, cx + 7, cy + 5, isSelected ? 0xFFFFEA00 : 0xFFF1F5F9, false);

            // Nút Yêu Thích ★ ở góc phải thẻ
            int starX = cx + cardW - 18;
            int starY = cy + 3;
            g.drawString(font, isFav ? "§e★" : "§7☆", starX + 4, starY + 2, 0xFFFFFFFF, false);

            // Cooldown hoặc Gợi ý loại
            int cdTicks = ClientSkillStateHandler.getRemainingCooldownTicks(item.cooldownKey());
            String sub = cdTicks > 0
                    ? String.format("§c⏳ %.1fs", cdTicks / 20.0D)
                    : "§7[" + item.category().label + "] §8(Click=Chọn | Shift+Click=Tung)";
            g.drawString(font, font.plainSubstrByWidth(sub, cardW - 26), cx + 7, cy + 15, 0xFF94A3B8, false);
        }

        // Phân trang
        int pageY = gridTop + ROWS * (cardH + 4) + 2;
        drawActionButton(g, mouseX, mouseY, left + 10, pageY, 54, 14, "◀ Trước", 0xFF1E1B2E, 0xFF64748B);
        g.drawCenteredString(font, "Trang " + (currentPage + 1) + "/" + totalPages + " (" + filtered.size() + " chiêu)", left + PANEL_W / 2, pageY + 3, 0xFFCBD5E1);
        drawActionButton(g, mouseX, mouseY, left + PANEL_W - 64, pageY, 54, 14, "Sau ▶", 0xFF1E1B2E, 0xFF64748B);

        // Thanh 4 Slot Yêu Thích ở đáy
        int favY = top + PANEL_H - 32;
        g.fill(left + 10, favY - 2, left + PANEL_W - 10, favY - 1, 0x55D4AF37);
        int favW = 102;
        for (int s = 0; s < 4; s++) {
            int fx = left + 10 + s * (favW + 4);
            boolean hoverFav = isInRect(mouseX, mouseY, fx, favY + 2, favW, 22);
            g.fill(fx, favY + 2, fx + favW, favY + 24, hoverFav ? 0xFF2D224A : 0xFF19132B);
            if (s < favs.size()) {
                int favId = favs.get(s);
                SkillItem found = allSkills.stream().filter(it -> it.id() == favId).findFirst().orElse(null);
                String lbl = found != null ? found.name() : ("Skill #" + favId);
                g.drawString(font, "§e[" + (s + 1) + "] §f" + font.plainSubstrByWidth(lbl, favW - 24), fx + 4, favY + 8, 0xFFFFFFFF, false);
            } else {
                g.drawString(font, "§8[" + (s + 1) + "] Bấm ☆ để gán", fx + 4, favY + 8, 0xFF64748B, false);
            }
        }

        if (hoverTooltip != null) {
            g.renderTooltip(font, Component.literal(hoverTooltip), mouseX, mouseY);
        }
    }

    private void drawActionButton(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h, String text, int bg, int border) {
        boolean hovered = isInRect(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, hovered ? (bg + 0x101010) : bg);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.drawCenteredString(font, font.plainSubstrByWidth(text, w - 6), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private boolean isInRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Mở nhanh Trí Tuệ Vương Raphael (50 Kỹ Năng)
        if (isInRect(mouseX, mouseY, left + PANEL_W - 224, top + 5, 86, 16)) {
            ModMessages.sendToServer(new ServerboundRaphaelSkillPacket(ServerboundRaphaelSkillPacket.ACTION_OPEN));
            playClick();
            return true;
        }

        // Safe Mode
        if (isInRect(mouseX, mouseY, left + PANEL_W - 134, top + 5, 124, 16)) {
            ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TOGGLE_SAFE_MODE, 0));
            playClick();
            return true;
        }

        // Spatial Quick Buttons (Row 2)
        int barY = top + 25;
        if (isInRect(mouseX, mouseY, left + 10, barY, 98, 16)) {
            ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_MARK_ANCHOR, 0));
            playClick();
            this.onClose();
            return true;
        }
        if (isInRect(mouseX, mouseY, left + 111, barY, 88, 16)) {
            ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TELEPORT_SLOT, 2));
            playClick();
            this.onClose();
            return true;
        }
        if (isInRect(mouseX, mouseY, left + 202, barY, 88, 16)) {
            if (ClientSkillStateHandler.hasAnchorMark()) {
                ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TELEPORT_SLOT, 1));
                playClick();
                this.onClose();
            }
            return true;
        }
        if (isInRect(mouseX, mouseY, left + 293, barY, 72, 16)) {
            if (ClientSkillStateHandler.hasAnchorMark()) {
                ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_SEND_TO_ANCHOR, -1));
                playClick();
                this.onClose();
            }
            return true;
        }
        if (isInRect(mouseX, mouseY, left + 368, barY, 62, 16)) {
            int slot = ClientSkillStateHandler.hasTacticalMark() ? 0 : 3;
            ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TELEPORT_SLOT, slot));
            playClick();
            this.onClose();
            return true;
        }

        // Đa Ấn Ký Vĩnh Viễn #1..#5 (Row 2b)
        int anchorY = top + 45;
        List<String> anchorLabels = ClientSkillStateHandler.getAnchorLabels();
        for (int i = 0; i < 5; i++) {
            int ax = left + 11 + i * 84;
            if (isInRect(mouseX, mouseY, ax, anchorY, 82, 18)) {
                boolean exists = i < anchorLabels.size();
                if (!exists) {
                    if (button == 0) {
                        ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_MARK_ANCHOR, 0));
                        playClick();
                        this.onClose();
                    }
                    return true;
                }
                if (button == 1) {
                    ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_REMOVE_ANCHOR, i));
                    playClick();
                    return true;
                }
                if (hasShiftDown()) {
                    ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_SEND_TO_ANCHOR, i));
                } else {
                    ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TELEPORT_SLOT, 10 + i));
                }
                playClick();
                this.onClose();
                return true;
            }
        }

        // Tabs
        int tabY = top + 68;
        int tabW = 82;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = left + 10 + i * (tabW + 3);
            if (isInRect(mouseX, mouseY, tx, tabY, tabW, 15)) {
                selectedCategory = cats[i];
                currentPage = 0;
                playClick();
                return true;
            }
        }

        // Grid cards
        List<SkillItem> allSkills = buildAllSkillsForMode();
        List<SkillItem> filtered = getFilteredSkills(allSkills);
        int gridTop = top + 88;
        int cardW = 206;
        int cardH = 26;
        int startIdx = currentPage * PER_PAGE;

        for (int idx = 0; idx < PER_PAGE; idx++) {
            int dataIdx = startIdx + idx;
            if (dataIdx >= filtered.size()) break;
            SkillItem item = filtered.get(dataIdx);
            int col = idx % COLS;
            int row = idx / COLS;
            int cx = left + 10 + col * (cardW + 8);
            int cy = gridTop + row * (cardH + 4);

            if (isInRect(mouseX, mouseY, cx, cy, cardW, cardH)) {
                // Click vào ngôi sao ★ hoặc chuột phải -> Gán/Bỏ Yêu Thích
                if (button == 1 || isInRect(mouseX, mouseY, cx + cardW - 22, cy, 22, cardH)) {
                    ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TOGGLE_FAVORITE, item.id()));
                    playClick();
                    return true;
                }
                if (item.name().contains("Trí Tuệ Vương Raphael")) {
                    ModMessages.sendToServer(new ServerboundRaphaelSkillPacket(ServerboundRaphaelSkillPacket.ACTION_OPEN));
                    playClick();
                    return true;
                }
                if (item.name().contains("Dạ Dày Vô Hạn Azathoth")) {
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_OPEN_GUI, 0));
                    playClick();
                    return true;
                }
                boolean shiftDown = hasShiftDown();
                ModMessages.sendToServer(new ServerboundSkillActionPacket(
                        shiftDown ? ServerboundSkillActionPacket.ACTION_CAST_SKILL : ServerboundSkillActionPacket.ACTION_SELECT_SKILL,
                        item.id()
                ));
                playClick();
                this.onClose();
                return true;
            }
        }

        // Pagination
        int pageY = gridTop + ROWS * (cardH + 4) + 2;
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PER_PAGE));
        if (isInRect(mouseX, mouseY, left + 10, pageY, 54, 14)) {
            currentPage = (currentPage - 1 + totalPages) % totalPages;
            playClick();
            return true;
        }
        if (isInRect(mouseX, mouseY, left + PANEL_W - 64, pageY, 54, 14)) {
            currentPage = (currentPage + 1) % totalPages;
            playClick();
            return true;
        }

        // Favorites bar
        int favY = top + PANEL_H - 32;
        int favW = 102;
        List<Integer> favs = ClientSkillStateHandler.getFavoriteSkillIds();
        for (int s = 0; s < 4; s++) {
            int fx = left + 10 + s * (favW + 4);
            if (isInRect(mouseX, mouseY, fx, favY + 2, favW, 22) && s < favs.size()) {
                int favId = favs.get(s);
                if (button == 1) {
                    ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_TOGGLE_FAVORITE, favId));
                } else {
                    ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_CAST_SKILL, favId));
                    this.onClose();
                }
                playClick();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.SKILL_WHEEL_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        // Nhấn phím 1..4 khi mở Bánh Xe để tung nhanh Slot Yêu Thích
        if (keyCode >= 49 && keyCode <= 52) {
            int slot = keyCode - 49;
            ModMessages.sendToServer(new ServerboundSkillActionPacket(ServerboundSkillActionPacket.ACTION_CAST_FAVORITE_SLOT, slot));
            playClick();
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void playClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 1.2F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
