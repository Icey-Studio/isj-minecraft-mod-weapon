package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundGateDialActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GIAO DIỆN LA BÀN DỊ GIỚI THẦN MÔN (0 -> 9) — LẤY CẢM HỨNG TỪ LÂU ĐÀI DI ĐỘNG CỦA HOWL
 * - Kênh 0: Dẫn vào Đảo Nổi Biệt Thự Gỗ Sân Vườn (hoặc quay về vị trí cũ nếu đang ở trong Đảo).
 * - Kênh 1..9: Lưu tối đa 9 điểm dịch chuyển xuyên chiều không gian, tự đổi màu cổng & số hiệu 3D.
 */
public class SanctuaryGateDialScreen extends Screen {

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 236;

    private final boolean insideSanctuary;
    private int activeChannel;
    private final boolean hasCapsule;
    private final Map<Integer, DimensionalSanctuaryAbility.GateWaypoint> waypointMap = new HashMap<>();

    private EditBox nameInput;

    public SanctuaryGateDialScreen(boolean insideSanctuary, int activeChannel, boolean hasCapsule,
                                   List<DimensionalSanctuaryAbility.GateWaypoint> waypoints) {
        super(Component.literal("La Bàn Dị Giới Thần Môn"));
        this.insideSanctuary = insideSanctuary;
        this.activeChannel = Math.clamp(activeChannel, 0, 9);
        this.hasCapsule = hasCapsule;
        if (waypoints != null) {
            for (DimensionalSanctuaryAbility.GateWaypoint wp : waypoints) {
                this.waypointMap.put(wp.slot(), wp);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        String prevText = this.nameInput != null ? this.nameInput.getValue() : "";
        this.nameInput = new EditBox(this.font, left + 86, top + 32, 206, 16, Component.literal("Tên điểm dịch chuyển"));
        this.nameInput.setMaxLength(36);
        this.nameInput.setValue(prevText);
        this.addWidget(this.nameInput);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        int themeColor = 0xFF000000 | DimensionalSanctuaryAbility.getChannelPrimaryColor(this.activeChannel);
        String activeColorCode = DimensionalSanctuaryAbility.getChannelColorCode(this.activeChannel);
        String activeGem = DimensionalSanctuaryAbility.getChannelGemName(this.activeChannel);

        // 1. Khung nền Gỗ Tối & Tinh Vân Hoàng Kim + Viền Đổi Màu Theo Kênh Cổng Hiện Tại
        graphics.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF0B0812);
        graphics.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, themeColor);
        graphics.fillGradient(left, top, left + PANEL_W, top + PANEL_H, 0xF214101E, 0xF20B0912);
        graphics.renderOutline(left + 2, top + 2, PANEL_W - 4, PANEL_H - 4, 0x44FFD54F);

        // 2. Tiêu đề & Trạng thái Kim La Bàn đang trỏ
        graphics.drawCenteredString(this.font,
                "§6§l✦ LA BÀN DỊ GIỚI THẦN MÔN • MẠNG LƯỚI CỔNG 0–9 ✦",
                left + PANEL_W / 2, top + 7, 0xFFFFFF);

        String activeSummary = buildActiveSummary(activeColorCode, activeGem);
        graphics.drawCenteredString(this.font, activeSummary, left + PANEL_W / 2, top + 19, 0xDDDDDD);

        // 3. Thanh đặt tên gợi nhớ cho Waypoint mới
        graphics.drawString(this.font, "§eTên mốc mới:", left + 14, top + 36, 0xFFFFFF);
        this.nameInput.render(graphics, mouseX, mouseY, partialTick);
        if (this.nameInput.getValue().isEmpty() && !this.nameInput.isFocused()) {
            graphics.drawString(this.font, "§8Để trống tự lấy tên vùng...", left + 90, top + 36, 0x888888, false);
        }

        String rightHint = this.insideSanctuary
                ? "§b🏡 Đang ở Đảo Biệt Thự"
                : "§7Click ô trống = §aLưu";
        graphics.drawString(this.font, rightHint, left + 298, top + 36, 0xCCCCCC);

        // 4. Lưới 10 Thẻ Kênh Thần Môn (0 -> 9) chia 2 cột × 5 hàng
        String pendingTooltip = null;
        int gridStartY = top + 53;
        int cardW = 192;
        int cardH = 28;
        int rowStep = 31;

        for (int slot = 0; slot <= 9; slot++) {
            int col = slot % 2;
            int row = slot / 2;
            int cardX = left + 14 + col * (cardW + 8);
            int cardY = gridStartY + row * rowStep;

            String tip = renderSlotCard(graphics, slot, cardX, cardY, cardW, cardH, mouseX, mouseY);
            if (tip != null) {
                pendingTooltip = tip;
            }
        }

        // 5. Thanh tiện ích dưới đáy
        int bottomY = top + 211;
        if (!this.insideSanctuary) {
            // Nút 1: Lều dã ngoại 5x5
            int btn1X = left + 14;
            int btnW = 192;
            int btnH = 18;
            boolean h1 = isInside(mouseX, mouseY, btn1X, bottomY, btnW, btnH);
            graphics.fill(btn1X, bottomY, btn1X + btnW, bottomY + btnH, h1 ? 0xFF3E2A14 : 0xFF24180C);
            graphics.renderOutline(btn1X, bottomY, btnW, btnH, h1 ? 0xFFFFD54F : 0xFF8D6E63);
            String capLabel = this.hasCapsule
                    ? "§6⛺ Thu Hồi Lều Dã Ngoại (5×5)"
                    : "§e⛺ Dựng Nhanh Lều Gỗ Tạm (5×5)";
            graphics.drawCenteredString(this.font, capLabel, btn1X + btnW / 2, bottomY + 5, 0xFFFFFF);
            if (h1) {
                pendingTooltip = "§eBiến ra hoặc thu hồi lều gỗ dã ngoại 5×5 ngay tại chỗ (hoàn trả địa hình cũ 100%).";
            }

            // Nút 2: Đóng cánh cổng đang mở
            int btn2X = left + 214;
            boolean h2 = isInside(mouseX, mouseY, btn2X, bottomY, btnW, btnH);
            graphics.fill(btn2X, bottomY, btn2X + btnW, bottomY + btnH, h2 ? 0xFF42151B : 0xFF260D11);
            graphics.renderOutline(btn2X, bottomY, btnW, btnH, h2 ? 0xFFFF5252 : 0xFF8E2430);
            graphics.drawCenteredString(this.font, "§c🚪 Đóng Cánh Cổng Đang Mở", btn2X + btnW / 2, bottomY + 5, 0xFFFFFF);
            if (h2) {
                pendingTooltip = "§cThu hồi Cánh Cửa Không Gian 3D đang mở ngoài thế giới.";
            }
        } else {
            graphics.drawCenteredString(this.font,
                    "§7Chọn số cổng để xoay La Bàn, sau đó bước qua §bCổng Sân Vườn Anh Đào §7để dịch chuyển!",
                    left + PANEL_W / 2, bottomY + 5, 0xAAAAAA);
        }

        if (pendingTooltip != null) {
            graphics.renderTooltip(this.font, this.font.split(Component.literal(pendingTooltip), 235), mouseX, mouseY);
        }
    }

    private String buildActiveSummary(String activeColorCode, String activeGem) {
        if (this.activeChannel == 0) {
            if (this.insideSanctuary) {
                return "§7Kim La Bàn đang trỏ: §b§l[CỔNG #0 • LAM NGỌC] §f↩ Quay Lại Chỗ Vừa Vào";
            }
            return "§7Kim La Bàn đang trỏ: §b§l[CỔNG #0 • LAM NGỌC] §e🏡 Đảo Nổi Biệt Thự Gỗ";
        }
        DimensionalSanctuaryAbility.GateWaypoint wp = this.waypointMap.get(this.activeChannel);
        String dest = wp != null ? wp.name() : "Chưa đánh dấu";
        return "§7Kim La Bàn đang trỏ: " + activeColorCode + "§l[CỔNG #" + this.activeChannel + " • " + activeGem.toUpperCase() + "] §f" + dest;
    }

    private String renderSlotCard(GuiGraphics graphics, int slot, int x, int y, int w, int h, int mouseX, int mouseY) {
        int rgb = DimensionalSanctuaryAbility.getChannelPrimaryColor(slot);
        String cc = DimensionalSanctuaryAbility.getChannelColorCode(slot);
        boolean isSelected = (slot == this.activeChannel);
        boolean cardHover = isInside(mouseX, mouseY, x, y, w, h);

        int bg = isSelected ? blendDark(rgb, 0.32f, 0xEE000000)
                : (cardHover ? blendDark(rgb, 0.20f, 0xDD000000) : 0xCC13111C);
        int border = isSelected ? (0xFF000000 | rgb)
                : (cardHover ? (0xCC000000 | rgb) : 0xFF332D42);

        graphics.fill(x, y, x + w, y + h, bg);
        graphics.renderOutline(x, y, w, h, border);
        if (isSelected) {
            graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, 0x88FFFFFF);
        }

        // Huy hiệu số hiệu cổng (0 -> 9) bên trái
        int badgeX = x + 3;
        int badgeY = y + 3;
        int badgeS = 22;
        graphics.fill(badgeX, badgeY, badgeX + badgeS, badgeY + badgeS, blendDark(rgb, 0.45f, 0xFF000000));
        graphics.renderOutline(badgeX, badgeY, badgeS, badgeS, 0xFF000000 | rgb);
        graphics.drawCenteredString(this.font, "§l" + slot, badgeX + badgeS / 2, badgeY + 7, 0xFFFFFF);

        int textX = x + 29;
        if (slot == 0) {
            if (this.insideSanctuary) {
                graphics.drawString(this.font, "§b§lQuay Lại Chỗ Vừa Vào", textX, y + 5, 0xFFFFFF);
                graphics.drawString(this.font, "§7Trở về tọa độ trước khi vào đảo", textX, y + 16, 0xAAAAAA);
                return cardHover ? "§b§l[Cổng #0] §fXoay cổng sân vườn về vị trí bạn vừa bước vào Biệt Thự." : null;
            } else {
                graphics.drawString(this.font, "§b§lBiệt Thự Gỗ Sân Vườn", textX, y + 5, 0xFFFFFF);
                graphics.drawString(this.font, "§7Đảo Anh Đào • Suối Nóng • Kho", textX, y + 16, 0xAAAAAA);
                return cardHover ? "§b§l[Cổng #0] §fMở lối vào Đảo Nổi Biệt Thự Gỗ Sân Vườn cá nhân (An toàn 100%, đưa cả thú cưng vào)." : null;
            }
        }

        DimensionalSanctuaryAbility.GateWaypoint wp = this.waypointMap.get(slot);
        if (wp == null) {
            graphics.drawString(this.font, cc + "Cổng #" + slot + " §8(" + DimensionalSanctuaryAbility.getChannelGemName(slot) + ")", textX, y + 5, 0xCCCCCC);
            String sub = this.insideSanctuary ? "§8Ra thế giới ngoài để lưu mốc" : "§a+ Click để đánh dấu tại đây";
            graphics.drawString(this.font, sub, textX, y + 16, 0x888888);
            if (cardHover) {
                return this.insideSanctuary
                        ? "§eHãy ra thế giới bên ngoài để đánh dấu tọa độ vào Cổng #" + slot + "."
                        : cc + "§l[Cổng #" + slot + " Trống] §aClick để lưu vị trí đang đứng làm điểm dịch chuyển!";
            }
            return null;
        }

        // Ô đã có Waypoint: Hiển thị tên + tọa độ + 2 nút phụ [Ghi đè ↻] và [Xóa ✕]
        int maxTitleWidth = this.insideSanctuary ? 138 : 118;
        String safeTitle = this.font.plainSubstrByWidth(wp.name(), maxTitleWidth);
        graphics.drawString(this.font, cc + "§l" + safeTitle, textX, y + 5, 0xFFFFFF);

        String coordLine = String.format("§7%s (%d, %d, %d)",
                shortDimBadge(wp.dimension()),
                Math.round(wp.x()), Math.round(wp.y()), Math.round(wp.z()));
        graphics.drawString(this.font, this.font.plainSubstrByWidth(coordLine, maxTitleWidth + 4), textX, y + 16, 0xAAAAAA);

        String buttonTooltip = null;

        // Nút Ghi đè [↻] (chỉ hiện khi ở ngoài thế giới thực)
        if (!this.insideSanctuary) {
            int owX = x + w - 37;
            int owY = y + 6;
            boolean owHover = isInside(mouseX, mouseY, owX, owY, 16, 16);
            graphics.fill(owX, owY, owX + 16, owY + 16, owHover ? 0xFF1B5E20 : 0xFF142818);
            graphics.renderOutline(owX, owY, 16, 16, owHover ? 0xFF69F0AE : 0xFF2E7D32);
            graphics.drawCenteredString(this.font, "§a↻", owX + 8, owY + 4, 0xFFFFFF);
            if (owHover) {
                buttonTooltip = "§aGhi đè vị trí hiện tại vào Cổng #" + slot;
            }
        }

        // Nút Xóa [✕]
        int delX = x + w - 19;
        int delY = y + 6;
        boolean delHover = isInside(mouseX, mouseY, delX, delY, 16, 16);
        graphics.fill(delX, delY, delX + 16, delY + 16, delHover ? 0xFF7F0000 : 0xFF2A1014);
        graphics.renderOutline(delX, delY, 16, 16, delHover ? 0xFFFF5252 : 0xFF8E2430);
        graphics.drawCenteredString(this.font, "§c✕", delX + 8, delY + 4, 0xFFFFFF);
        if (delHover) {
            buttonTooltip = "§cXóa điểm đánh dấu Cổng #" + slot;
        }

        if (buttonTooltip != null) {
            return buttonTooltip;
        }
        if (cardHover) {
            return cc + "§l[Cổng #" + slot + " • " + DimensionalSanctuaryAbility.getChannelGemName(slot) + "] §f"
                    + wp.name() + "\n§7Click để xoay Cánh Cổng sang số " + slot + " rồi bước qua là tới ngay!";
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = (this.width - PANEL_W) / 2;
            int top = (this.height - PANEL_H) / 2;

            // Click vào ô nhập tên
            if (isInside(mouseX, mouseY, left + 86, top + 32, 206, 16)) {
                this.setFocused(this.nameInput);
                this.nameInput.setFocused(true);
                return this.nameInput.mouseClicked(mouseX, mouseY, button);
            } else {
                this.nameInput.setFocused(false);
            }

            int gridStartY = top + 53;
            int cardW = 192;
            int cardH = 28;
            int rowStep = 31;
            String customName = this.nameInput.getValue().trim();

            for (int slot = 0; slot <= 9; slot++) {
                int col = slot % 2;
                int row = slot / 2;
                int cardX = left + 14 + col * (cardW + 8);
                int cardY = gridStartY + row * rowStep;

                if (!isInside(mouseX, mouseY, cardX, cardY, cardW, cardH)) continue;

                if (slot == 0) {
                    playClick(1.25F);
                    ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                            ServerboundGateDialActionPacket.ACTION_SELECT_CHANNEL, 0, ""));
                    this.onClose();
                    return true;
                }

                DimensionalSanctuaryAbility.GateWaypoint wp = this.waypointMap.get(slot);
                if (wp == null) {
                    if (this.insideSanctuary) {
                        playFail();
                        return true;
                    }
                    playClick(1.35F);
                    ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                            ServerboundGateDialActionPacket.ACTION_SAVE_WAYPOINT, slot, customName));
                    this.onClose();
                    return true;
                }

                // Kiểm tra click nút Xóa [✕]
                int delX = cardX + cardW - 19;
                int delY = cardY + 6;
                if (isInside(mouseX, mouseY, delX, delY, 16, 16)) {
                    playClick(0.9F);
                    this.waypointMap.remove(slot);
                    if (this.activeChannel == slot) {
                        this.activeChannel = 0;
                    }
                    ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                            ServerboundGateDialActionPacket.ACTION_DELETE_WAYPOINT, slot, ""));
                    return true;
                }

                // Kiểm tra click nút Ghi đè [↻]
                if (!this.insideSanctuary) {
                    int owX = cardX + cardW - 37;
                    int owY = cardY + 6;
                    if (isInside(mouseX, mouseY, owX, owY, 16, 16)) {
                        playClick(1.35F);
                        ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                                ServerboundGateDialActionPacket.ACTION_SAVE_WAYPOINT, slot, customName));
                        this.onClose();
                        return true;
                    }
                }

                // Click vào phần chính của thẻ -> Xoay Cổng sang kênh này!
                playClick(1.25F);
                ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                        ServerboundGateDialActionPacket.ACTION_SELECT_CHANNEL, slot, ""));
                this.onClose();
                return true;
            }

            // Nút tiện ích dưới đáy
            if (!this.insideSanctuary) {
                int bottomY = top + 211;
                if (isInside(mouseX, mouseY, left + 14, bottomY, 192, 18)) {
                    playClick(1.15F);
                    ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                            ServerboundGateDialActionPacket.ACTION_TOGGLE_CAPSULE, 0, ""));
                    this.onClose();
                    return true;
                }
                if (isInside(mouseX, mouseY, left + 214, bottomY, 192, 18)) {
                    playClick(0.85F);
                    ModMessages.sendToServer(new ServerboundGateDialActionPacket(
                            ServerboundGateDialActionPacket.ACTION_CLOSE_GATE, 0, ""));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int blendDark(int rgb, float ratio, int alphaMask) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * ratio) + 12);
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * ratio) + 10);
        int b = Math.min(255, Math.round((rgb & 0xFF) * ratio) + 16);
        return alphaMask | (r << 16) | (g << 8) | b;
    }

    private static String shortDimBadge(String dimId) {
        if (dimId == null) return "OW";
        if (dimId.contains("the_nether")) return "Nether";
        if (dimId.contains("the_end")) return "End";
        if (dimId.contains("overworld")) return "Overworld";
        int idx = dimId.indexOf(':');
        return idx >= 0 && idx + 1 < dimId.length() ? dimId.substring(idx + 1) : dimId;
    }

    private void playClick(float pitch) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.85F, pitch);
        }
    }

    private void playFail() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.DISPENSER_FAIL, 0.9F, 1.2F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
