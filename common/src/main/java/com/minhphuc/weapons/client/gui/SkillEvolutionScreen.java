package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.evolution.EvolvedSkillHelper;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundEvolveSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class SkillEvolutionScreen extends Screen {

    private static final int PANEL_W = 440;
    private static final int PANEL_H = 240;

    private final List<EvolvedSkillHelper.AvailableSkill> availableSkills = new ArrayList<>();
    private EvolvedSkillHelper.AvailableSkill slotA = null;
    private EvolvedSkillHelper.AvailableSkill slotB = null;

    private int scrollOffset = 0;
    private static final int VISIBLE_ITEMS = 6;
    private static final int ITEM_HEIGHT = 26;

    private Button evolveButton;

    public SkillEvolutionScreen() {
        super(Component.literal("Quyển Thư Tiến Hóa Kỹ Năng"));
    }

    @Override
    protected void init() {
        super.init();
        refreshSkills();

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        evolveButton = Button.builder(Component.literal("§6§l⚡ TIẾN HÓA KỸ NĂNG ⚡"), btn -> {
            if (slotA != null && slotB != null && !slotA.key().equals(slotB.key())) {
                ModMessages.sendToServer(new ServerboundEvolveSkillPacket(slotA.key(), slotB.key()));
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.2F, 1.0F);
                }
                this.onClose();
            }
        }).bounds(left + 225, top + 198, 202, 24).build();
        evolveButton.active = false;
        this.addRenderableWidget(evolveButton);
    }

    private void refreshSkills() {
        availableSkills.clear();
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            availableSkills.addAll(EvolvedSkillHelper.getPlayerAvailableSkills(player));
        }
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Ghi đè rỗng: super.render() trong Minecraft 1.21 tự động gọi renderBackground()
        // Nếu để mặc định, super.render() ở cuối hàm sẽ chạy shader làm mờ (processBlurEffect)
        // và làm nhòe toàn bộ giao diện panel + chữ vừa vẽ!
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. Áp dụng hiệu ứng làm mờ hậu cảnh thế giới TRƯỚC TIÊN (chỉ mờ thế giới, không mờ UI)
        this.renderBlurredBackground(partialTick);
        gui.fill(0, 0, this.width, this.height, 0x88000000); // Phủ tối 50% nhẹ nhàng, sắc nét

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // 2. Khung Master Panel Đậm Đặc 100%, sắc nét tuyệt đối, viền Hoàng Kim
        gui.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF050308); // Đổ bóng viền
        gui.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFFDAA520); // Viền vàng kim
        gui.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF140D24); // Thân nền tím sẫm Obsidian

        // Thanh tiêu đề phía trên
        gui.fill(left, top, left + PANEL_W, top + 24, 0xFF24143D);
        gui.fill(left, top + 24, left + PANEL_W, top + 25, 0xFFFFD700);

        gui.drawString(font, "§6§l✦ QUYỂN THƯ TIẾN HÓA KỸ NĂNG TỐI THƯỢNG ✦", left + 12, top + 8, 0xFFFFAA00, true);

        // Nút Đóng [✕] ở góc trên bên phải
        boolean hoverClose = (mouseX >= left + PANEL_W - 22 && mouseX <= left + PANEL_W - 6 &&
                mouseY >= top + 5 && mouseY <= top + 21);
        gui.drawString(font, hoverClose ? "§c§l[✕]" : "§7[✕]", left + PANEL_W - 22, top + 8, 0xFFFFFFFF, true);

        // Đường kẻ chia đôi panel
        gui.vLine(left + 215, top + 26, top + PANEL_H - 8, 0x66FFD700);

        // ================= CỘT TRÁI: DANH SÁCH KỸ NĂNG QUÉT ĐƯỢC =================
        gui.drawString(font, "§eDanh Sách Kỹ Năng Đang Có: (" + availableSkills.size() + ")", left + 10, top + 30, 0xFFFFFF, true);
        gui.drawString(font, "§7(Nhấn để chọn vào ô dung hợp)", left + 10, top + 41, 0xAAAAAA, true);

        int listX = left + 10;
        int listY = top + 54;
        int listW = 195;

        if (availableSkills.isEmpty()) {
            gui.drawWordWrap(font, Component.literal("§c⚠️ Bạn chưa có kỹ năng nào khả dụng!\n\n§e(Hãy thức tỉnh Chân Ma Vương / Thủy Tổ Ác Ma, hoặc trang bị Áo Giáp Thần Thoại / Chế độ Sáng Tạo để thử nghiệm)."),
                    listX + 4, listY + 20, listW - 8, 0xFFFFFF);
        } else {
            for (int i = 0; i < VISIBLE_ITEMS; i++) {
                int index = scrollOffset + i;
                if (index >= availableSkills.size()) break;

                EvolvedSkillHelper.AvailableSkill skill = availableSkills.get(index);
                int itemY = listY + (i * ITEM_HEIGHT);

                boolean isHovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2;
                boolean isSelectedA = slotA != null && slotA.key().equals(skill.key());
                boolean isSelectedB = slotB != null && slotB.key().equals(skill.key());

                int bgColor = (isSelectedA || isSelectedB) ? 0xFF3D1F6D : (isHovered ? 0xFF2A194C : 0xFF1B1133);
                gui.fill(listX, itemY, listX + listW, itemY + ITEM_HEIGHT - 2, bgColor);

                int outlineColor = (isSelectedA || isSelectedB) ? 0xFF00FFCC : (isHovered ? 0xFFFFD700 : 0xFF4A346E);
                gui.renderOutline(listX, itemY, listW, ITEM_HEIGHT - 2, outlineColor);

                // Tên kỹ năng & Rank
                String rankTag = "§6[R" + skill.rank() + "] ";
                String name = font.plainSubstrByWidth(rankTag + skill.displayNameVi(), listW - 8);
                gui.drawString(font, name, listX + 4, itemY + 3, skill.colorHex(), true);

                String subDesc = font.plainSubstrByWidth(skill.descVi(), listW - 8);
                gui.drawString(font, "§7" + subDesc, listX + 4, itemY + 14, 0xCCCCCC, true);
            }

            // Thanh cuộn
            if (availableSkills.size() > VISIBLE_ITEMS) {
                int totalHeight = VISIBLE_ITEMS * ITEM_HEIGHT;
                int scrollbarY = listY + (int) ((float) scrollOffset / (availableSkills.size() - VISIBLE_ITEMS) * (totalHeight - 20));
                gui.fill(listX + listW + 2, listY, listX + listW + 5, listY + totalHeight, 0xFF0D061A);
                gui.fill(listX + listW + 2, scrollbarY, listX + listW + 5, scrollbarY + 20, 0xFFFFD700);
            }
        }

        // ================= CỘT PHẢI: KHU VỰC DUNG HỢP =================
        int rightX = left + 225;
        gui.drawString(font, "§e⚡ Khu Vực Dung Hợp (2 Kỹ Năng):", rightX, top + 30, 0xFFFFFF, true);

        // Slot 1
        int slot1Y = top + 44;
        boolean hoverSlotA = mouseX >= rightX && mouseX <= rightX + 202 && mouseY >= slot1Y && mouseY <= slot1Y + 22;
        gui.fill(rightX, slot1Y, rightX + 202, slot1Y + 22, hoverSlotA ? 0xFF2F1D54 : 0xFF1A1033);
        gui.renderOutline(rightX, slot1Y, 202, 22, slotA != null ? 0xFF00FFCC : 0xFF5A447E);
        if (slotA != null) {
            gui.drawString(font, "§bSlot 1: §f" + font.plainSubstrByWidth(slotA.displayNameVi(), 145), rightX + 6, slot1Y + 6, 0xFFFFFF, true);
            gui.drawString(font, "§c[✕]", rightX + 185, slot1Y + 6, 0xFF5555, true);
        } else {
            gui.drawString(font, "§7[ Nhấn kỹ năng bên trái để chọn Slot 1 ]", rightX + 6, slot1Y + 6, 0xBBBBBB, true);
        }

        // Dấu cộng dung hợp
        gui.drawCenteredString(font, "§6§l+ DUNG HỢP CÙNG +", rightX + 101, top + 70, 0xFFA500);

        // Slot 2
        int slot2Y = top + 82;
        boolean hoverSlotB = mouseX >= rightX && mouseX <= rightX + 202 && mouseY >= slot2Y && mouseY <= slot2Y + 22;
        gui.fill(rightX, slot2Y, rightX + 202, slot2Y + 22, hoverSlotB ? 0xFF2F1D54 : 0xFF1A1033);
        gui.renderOutline(rightX, slot2Y, 202, 22, slotB != null ? 0xFF00FFCC : 0xFF5A447E);
        if (slotB != null) {
            gui.drawString(font, "§bSlot 2: §f" + font.plainSubstrByWidth(slotB.displayNameVi(), 145), rightX + 6, slot2Y + 6, 0xFFFFFF, true);
            gui.drawString(font, "§c[✕]", rightX + 185, slot2Y + 6, 0xFF5555, true);
        } else {
            gui.drawString(font, "§7[ Nhấn kỹ năng bên trái để chọn Slot 2 ]", rightX + 6, slot2Y + 6, 0xBBBBBB, true);
        }

        // Khung thông tin dự đoán kết quả
        int infoY = top + 110;
        gui.fill(rightX, infoY, rightX + 202, infoY + 82, 0xFF120B22);
        gui.renderOutline(rightX, infoY, 202, 82, 0xFF4A346E);

        if (slotA != null && slotB != null && !slotA.key().equals(slotB.key())) {
            int potentialTier = EvolvedSkillHelper.calculateResultTier(slotA.rank(), slotB.rank());
            String tierColor = potentialTier == 1 ? "§aCấp 1" : (potentialTier == 2 ? "§eCấp 2" : "§6§lCấp 3");

            gui.drawString(font, "§6✦ Dự Đoán Uy Lực Tiềm Năng: " + tierColor, rightX + 6, infoY + 6, 0xFFFFFF, true);
            gui.drawString(font, "§a✔ Kỹ năng nguyên liệu §lKHÔNG BỊ TIÊU HAO§a!", rightX + 6, infoY + 18, 0x55FF55, true);

            int t0 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[0];
            int t1 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[1];
            int t2 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[2];
            int t3 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[3];

            gui.drawString(font, "§71. Uriel: " + (t0 > 0 ? "§6★ Cấp " + t0 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 30, 0xFFFFFF, true);
            gui.drawString(font, "§72. Vương Quyền: " + (t1 > 0 ? "§e★ Cấp " + t1 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 42, 0xFFFFFF, true);
            gui.drawString(font, "§73. Cảm Nhận Vạn Năng: " + (t2 > 0 ? "§b★ Cấp " + t2 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 54, 0xFFFFFF, true);
            gui.drawString(font, "§74. Lồng Giam Vô Hạn: " + (t3 > 0 ? "§d★ Cấp " + t3 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 66, 0xFFFFFF, true);

            evolveButton.active = true;
        } else {
            gui.drawString(font, "§6✦ Trạng Thái 4 Tuyệt Kỹ Tối Thượng:", rightX + 6, infoY + 6, 0xFFFFFF, true);

            int t0 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[0];
            int t1 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[1];
            int t2 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[2];
            int t3 = EvolvedSkillHelper.CLIENT_EVOLVED_SKILL_TIERS[3];

            gui.drawString(font, " §61. Uriel: " + (t0 > 0 ? "§6★ Cấp " + t0 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 18, 0xFFFFFF, true);
            gui.drawString(font, " §e2. Vương Quyền: " + (t1 > 0 ? "§e★ Cấp " + t1 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 28, 0xFFFFFF, true);
            gui.drawString(font, " §b3. Cảm Nhận: " + (t2 > 0 ? "§b★ Cấp " + t2 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 38, 0xFFFFFF, true);
            gui.drawString(font, " §d4. Lồng Giam: " + (t3 > 0 ? "§d★ Cấp " + t3 + "/3" : "§8(Chưa mở)"), rightX + 6, infoY + 48, 0xFFFFFF, true);

            gui.drawString(font, "§a✔ Dung hợp KHÔNG làm mất kỹ năng!", rightX + 6, infoY + 60, 0x55FF55, true);
            gui.drawString(font, "§6★ Có thể nâng toàn bộ 4 Tuyệt Kỹ lên Cấp 3!", rightX + 6, infoY + 70, 0xFFAA00, true);

            evolveButton.active = false;
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Click nút Đóng [✕] góc trên
        if (mouseX >= left + PANEL_W - 22 && mouseX <= left + PANEL_W - 6 &&
                mouseY >= top + 5 && mouseY <= top + 21) {
            this.onClose();
            return true;
        }

        // Click trên danh sách kỹ năng bên trái
        int listX = left + 10;
        int listY = top + 54;
        int listW = 195;

        for (int i = 0; i < VISIBLE_ITEMS; i++) {
            int index = scrollOffset + i;
            if (index >= availableSkills.size()) break;

            int itemY = listY + (i * ITEM_HEIGHT);
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2) {
                EvolvedSkillHelper.AvailableSkill clicked = availableSkills.get(index);
                if (slotA == null) {
                    slotA = clicked;
                    playSelectSound();
                } else if (slotB == null && !slotA.key().equals(clicked.key())) {
                    slotB = clicked;
                    playSelectSound();
                } else if (slotA.key().equals(clicked.key())) {
                    slotA = null;
                } else if (slotB != null && slotB.key().equals(clicked.key())) {
                    slotB = null;
                }
                return true;
            }
        }

        // Click hủy slot A hoặc B
        int rightX = left + 225;
        int slot1Y = top + 44;
        int slot2Y = top + 82;

        if (mouseX >= rightX && mouseX <= rightX + 202 && mouseY >= slot1Y && mouseY <= slot1Y + 22) {
            slotA = null;
            return true;
        }
        if (mouseX >= rightX && mouseX <= rightX + 202 && mouseY >= slot2Y && mouseY <= slot2Y + 22) {
            slotB = null;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (availableSkills.size() > VISIBLE_ITEMS) {
            if (scrollY > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (scrollY < 0) {
                scrollOffset = Math.min(availableSkills.size() - VISIBLE_ITEMS, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void playSelectSound() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8F, 1.2F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
