package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.tensura.AzathothStomachData;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundAzathothActionPacket;
import com.minhphuc.weapons.network.ServerboundSageActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Giao diện [Dạ Dày Vô Hạn & Không Gian Ảo - Thần Hư Không Azathoth].
 */
public class AzathothStomachScreen extends Screen {

    private final List<AzathothStomachData.StomachEntry> entries;
    private final boolean veldoraUnlocked;
    private int selectedIndex = 0;

    public AzathothStomachScreen(List<AzathothStomachData.StomachEntry> entries, boolean veldoraUnlocked) {
        super(Component.literal("§5§l[DẠ DÀY VÔ HẠN & KHÔNG GIAN ẢO]"));
        this.entries = entries;
        this.veldoraUnlocked = veldoraUnlocked;
    }

    @Override
    protected void init() {
        int left = (this.width - 380) / 2;
        int top = (this.height - 230) / 2;

        // Cột nút điều khiển bên phải
        int bx = left + 225;
        addRenderableWidget(Button.builder(Component.literal("§dNuốt Mục Tiêu / Vật Phẩm"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_SWALLOW, 0)))
                .bounds(bx, top + 28, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§bPhân Tích & Giải Ấn Veldora"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_ANALYZE_UNSEAL_VELDORA, selectedIndex)))
                .bounds(bx, top + 51, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§eGiải Phóng Ra Ngoài"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_RELEASE, selectedIndex)))
                .bounds(bx, top + 74, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§aChuyển Hóa Ma Tố (Hồi Máu)"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_CONVERT_MAGICULES, selectedIndex)))
                .bounds(bx, top + 97, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§6Auto-Craft: Táo Vàng (Potion)"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_AUTO_CRAFT, 0)))
                .bounds(bx, top + 120, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§5Auto-Craft: Tinh Thể Netherite"),
                b -> ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_AUTO_CRAFT, 1)))
                .bounds(bx, top + 143, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§3Gọi (Rồng)"),
                b -> {
                    onClose();
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_SUMMON_VELDORA_DRAGON, 0));
                }).bounds(bx, top + 168, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§bGọi (Người)"),
                b -> {
                    onClose();
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_SUMMON_VELDORA_HUMAN, 0));
                }).bounds(bx + 74, top + 168, 71, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§cThu Hồi / Hồi Sinh Veldora"),
                b -> {
                    onClose();
                    ModMessages.sendToServer(new ServerboundAzathothActionPacket(ServerboundAzathothActionPacket.ACTION_RECALL_OR_REVIVE_VELDORA, 0));
                }).bounds(bx, top + 191, 145, 20).build());

        // Hàng dưới bên trái: Hành động Đại Hiền Giả / Raphael
        addRenderableWidget(Button.builder(Component.literal("§3Quét Xuyên Tường"),
                b -> ModMessages.sendToServer(new ServerboundSageActionPacket(ServerboundSageActionPacket.ACTION_SCAN_AREA)))
                .bounds(left + 10, top + 196, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§aGiải Mã Nguyền Rủa"),
                b -> ModMessages.sendToServer(new ServerboundSageActionPacket(ServerboundSageActionPacket.ACTION_DECODE_CURSE)))
                .bounds(left + 115, top + 196, 102, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - 380) / 2;
        int top = (this.height - 230) / 2;
        int listX = left + 12;
        int listY = top + 30;
        if (mouseX >= listX && mouseX <= listX + 206 && mouseY >= listY && mouseY <= listY + 158) {
            int clicked = (int) ((mouseY - listY) / 14);
            if (clicked >= 0 && clicked < entries.size()) {
                selectedIndex = clicked;
                return true;
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
        int left = (this.width - 380) / 2;
        int top = (this.height - 230) / 2;

        g.fill(left, top, left + 380, top + 230, 0xEE0B0E18);
        g.fill(left, top, left + 380, top + 2, 0xFFA855F7);
        g.drawString(font, "§5§l✦ THẦN HƯ KHÔNG AZATHOTH: KHÔNG GIAN ẢO ✦", left + 12, top + 10, 0xFFFFFFFF, true);

        g.fill(left + 10, top + 28, left + 218, top + 190, 0xCC131826);
        if (entries.isEmpty()) {
            g.drawString(font, "§7Không Gian Ảo hiện đang trống.", left + 18, top + 40, 0xFF94A3B8, false);
            g.drawString(font, "§7Bấm 'Nuốt Mục Tiêu' để hấp thụ", left + 18, top + 54, 0xFF94A3B8, false);
            g.drawString(font, "§7quái vật / vật phẩm bị nguyền.", left + 18, top + 66, 0xFF94A3B8, false);
        } else {
            int maxRows = Math.min(entries.size(), 11);
            for (int i = 0; i < maxRows; i++) {
                var e = entries.get(i);
                int ry = top + 32 + i * 14;
                if (i == selectedIndex) {
                    g.fill(left + 12, ry - 1, left + 216, ry + 11, 0x669333EA);
                }
                String prefix = "ENTITY".equals(e.kind()) ? "§c[Quái] " : "§b[Đồ] ";
                g.drawString(font, prefix + "§f" + e.name(), left + 16, ry, 0xFFFFFFFF, false);
            }
        }

        String vStatus = veldoraUnlocked ? "§a✔ Đã Giải Phong Ấn (Có thể Triệu Hồi)" : "§e⚠ Chưa Giải Ấn (Tìm Hang Phong Ấn)";
        g.drawString(font, "§7Trạng thái Veldora: " + vStatus, left + 10, top + 218, 0xFFFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
