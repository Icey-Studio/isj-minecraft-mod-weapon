package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.content.tensura.PrimordialPactItem;
import com.minhphuc.weapons.content.tensura.PrimordialSkillPool;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundAbyssalActionPacket;
import com.minhphuc.weapons.network.ServerboundDemonCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DemonCommandScreen extends Screen {

    private static final int PANEL_W = 540;
    private static final int PANEL_H = 264;

    public static final int TAB_COMBAT = 0;
    public static final int TAB_GOVERNANCE = 1;
    public static final int TAB_VAULT = 2;

    public static final int SUBTAB_INNATE = 0;
    public static final int SUBTAB_CONTRACT = 1;

    private final ItemStack pactStack;
    private final DemonType demonType;
    private int activeTab = TAB_COMBAT;
    private int combatSubTab = SUBTAB_INNATE;
    private int combatPage = 0;

    public static class SkillEntry {
        public final int index;
        public final String title;
        public final String desc;
        public final int colorHex;
        public final boolean locked;
        public final String lockReason;

        public SkillEntry(int index, String title, String desc, int colorHex, boolean locked, String lockReason) {
            this.index = index;
            this.title = title;
            this.desc = desc;
            this.colorHex = colorHex;
            this.locked = locked;
            this.lockReason = lockReason;
        }

        public SkillEntry(int index, String title, String desc, int colorHex) {
            this(index, title, desc, colorHex, false, "");
        }
    }

    private final List<SkillEntry> innateSkills = new ArrayList<>();
    private final List<SkillEntry> contractSkills = new ArrayList<>();
    private String statusFeedback = "";
    private int feedbackTimer = 0;

    public DemonCommandScreen(ItemStack pactStack) {
        super(Component.literal("Bảng Điều Khiển Ác Ma Thủy Tổ"));
        this.pactStack = pactStack;
        this.demonType = PrimordialPactItem.getDemonType(pactStack);
    }

    @Override
    protected void init() {
        super.init();
        innateSkills.clear();
        contractSkills.clear();

        // 1. KỸ NĂNG KHỞI NGUYÊN & BÍ THUẬT (INNATE SKILLS)
        innateSkills.add(new SkillEntry(0, "Cào Cấu Ma Giới", "Đòn cào xé móng vuốt cận chiến xé gió", 0xDDDDDD));
        innateSkills.add(new SkillEntry(1, PrimordialDemonEntity.getSkillOneName(demonType), "Kỹ năng khởi nguyên giai đoạn 1", 0xFFA500));
        innateSkills.add(new SkillEntry(2, PrimordialDemonEntity.getSkillTwoName(demonType), "Kỹ năng khởi nguyên giai đoạn 2", 0xFF6347));
        innateSkills.add(new SkillEntry(3, PrimordialDemonEntity.getSkillThreeName(demonType), "Đại ma pháp tối thượng giai đoạn 3", 0xDC143C));
        innateSkills.add(new SkillEntry(4, "Cột Sáng 7 Sắc Cầu Vồng", "Quang trụ Jacob bộc phá năng lượng", 0x00FFFF));
        innateSkills.add(new SkillEntry(5, "Linh Tử Băng Hoại", "Disintegration phân rã nguyên tử", 0xFFD700));
        innateSkills.add(new SkillEntry(6, "Linh Tử Bộc Phát", "Đại pháo Spiritron Outburst xuyên phá 56m", 0x38BDF8));
        innateSkills.add(new SkillEntry(7, PrimordialDemonEntity.getSignatureOneName(demonType), "Bí thuật Thủy Tổ độc bản cấp 1", 0xBA55D3));
        innateSkills.add(new SkillEntry(8, PrimordialDemonEntity.getSignatureTwoName(demonType), "Bí thuật Thủy Tổ độc bản cấp 2", 0x9932CC));
        innateSkills.add(new SkillEntry(9, PrimordialDemonEntity.getSignatureThreeName(demonType), "Bí thuật hủy diệt độc bản cấp 3", 0x8A2BE2));
        innateSkills.add(new SkillEntry(10, "Bành Trướng Lãnh Địa", "Triển khai kết giới lãnh địa 16m", 0x4B0082));

        // Nạp kỹ năng ngẫu nhiên đã thức tỉnh
        String savedSkills = ItemStackDataHelper.getString(pactStack, "PrimordialSkills");
        List<String> skillIds = new ArrayList<>();
        if (!savedSkills.isEmpty()) {
            skillIds.addAll(Arrays.asList(savedSkills.split(",")));
        } else {
            PrimordialDemonEntity demon = findClientDemon();
            if (demon != null) {
                for (var sk : demon.getRandomSkills()) {
                    skillIds.add(String.valueOf(sk.id()));
                }
            }
        }

        for (int i = 0; i < skillIds.size(); i++) {
            var entry = PrimordialSkillPool.getSkillById(skillIds.get(i).trim());
            if (entry != null) {
                innateSkills.add(new SkillEntry(20 + i, entry.displayNameVi(), entry.descVi(), entry.iconColorHex()));
            }
        }

        // 2. KỸ NĂNG NGƯỜI CHƠI TRAO QUYỀN (CONTRACT SKILLS 100 -> 112) - MỞ KHÓA TOÀN BỘ KHÔNG RÀNG BUỘC
        contractSkills.add(new SkillEntry(100, "Kháng Ma Kết Giới", "Phong ấn ma tố & cản phá quái (R:60m)", 0x22C55E));
        contractSkills.add(new SkillEntry(101, "Cú Bắn Granit", "Pháo nón hạt nhân 56m bộc phá siêu thanh", 0xFF6D00));
        contractSkills.add(new SkillEntry(102, "Zone Track (Granit Trắng)", "Đại pháo quang học bạch kim xuyên phá 50m", 0xF0F8FF));
        contractSkills.add(new SkillEntry(103, "Thị Thực: Thị Nhục Mạch Động", "Hồi phục 50% HP & xung kích hất tung 18m", 0xCC0022));
        contractSkills.add(new SkillEntry(104, "Phép Màu Của Thần Linh", "Cột thánh quang thanh tẩy & cứu rỗi (tạm thời)", 0x55FFAA));
        contractSkills.add(new SkillEntry(105, "Đa Trùng Kết Giới", "Cầu Geodesic Aegis 3D cản phá mọi sát thương", 0x38BDF8));
        contractSkills.add(new SkillEntry(106, "Lồng Giam Vô Hạn", "Lồng giam đa diện kim cương 3D khóa mục tiêu", 0xFBBF24));
        contractSkills.add(new SkillEntry(107, "Hắc Viêm Ngục (Hellflare)", "Giam hãm mục tiêu trong hộp tối & kích nổ", 0x880000));
        contractSkills.add(new SkillEntry(108, "Hắc Viêm Hỏa Cầu", "Cầu ma hỏa đen 48m thiêu đốt linh hồn", 0xEE3300));
        contractSkills.add(new SkillEntry(109, "Lời Chúc Tử Vong", "Ra lệnh Ác Ma bay lên trời giáng Lời Chúc Tử Vong (Ngước nhìn)", 0x9400D3));
        contractSkills.add(new SkillEntry(110, "Siêu Tân Tinh (Supernova)", "Cột Plasma vũ trụ hủy diệt (tạm thời)", 0xFFD700));
        contractSkills.add(new SkillEntry(111, "Dị Giới Thần Môn", "Cánh cửa không gian kết nối thẳng về nhà", 0x10B981));
        contractSkills.add(new SkillEntry(112, "Long Tinh Bộc Viêm Bá", "Đại pháo rồng hủy diệt Drago-Nova", 0xEC4899));
    }

    private PrimordialDemonEntity findClientDemon() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) return null;
        ClientLevel level = this.minecraft.level;

        String uuidStr = ItemStackDataHelper.getString(pactStack, "DemonUUID");
        if (!uuidStr.isEmpty()) {
            try {
                UUID id = UUID.fromString(uuidStr);
                for (PrimordialDemonEntity e : level.getEntitiesOfClass(
                        PrimordialDemonEntity.class,
                        new AABB(this.minecraft.player.getX() - 128, this.minecraft.player.getY() - 64, this.minecraft.player.getZ() - 128,
                                this.minecraft.player.getX() + 128, this.minecraft.player.getY() + 64, this.minecraft.player.getZ() + 128))) {
                    if (e.getUUID().equals(id) && e.isAlive()) {
                        return e;
                    }
                }
            } catch (Exception ignored) {}
        }

        List<PrimordialDemonEntity> list = level.getEntitiesOfClass(
                PrimordialDemonEntity.class,
                new AABB(this.minecraft.player.getX() - 128, this.minecraft.player.getY() - 64, this.minecraft.player.getZ() - 128,
                        this.minecraft.player.getX() + 128, this.minecraft.player.getY() + 64, this.minecraft.player.getZ() + 128),
                d -> d.isAlive() && d.getDemonType() == demonType && d.isOwnedBy(this.minecraft.player)
        );
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void tick() {
        super.tick();
        if (feedbackTimer > 0) feedbackTimer--;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // 1. Khung nền Dark Fantasy
        guiGraphics.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF140808);
        guiGraphics.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFFB22222);
        guiGraphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE0F0707);

        // Header Title
        String customName = ItemStackDataHelper.getString(pactStack, "CustomDemonName");
        String nameDisplay = !customName.isEmpty() ? customName : demonType.getColorName().toUpperCase();

        if (demonType == DemonType.ROUGE) {
            guiGraphics.drawCenteredString(this.font,
                    "§4§l✦ HIỆP ƯỚC TRỌNG TÀI MA GIỚI: §c§l" + nameDisplay + " §7(Vua Ác Ma - Đẳng Cấp Ngang Hàng) ✦",
                    left + PANEL_W / 2, top + 7, 0xFFFFFF);
        } else {
            guiGraphics.drawCenteredString(this.font,
                    "§c§l✦ MỆNH LỆNH THỦY TỔ ÁC MA: §e§l" + nameDisplay + " §7(" + demonType.getTitleVi() + ") ✦",
                    left + PANEL_W / 2, top + 7, 0xFFFFFF);
        }

        // 2. Thanh Máu & Trạng Thái Cơ Bản
        PrimordialDemonEntity demon = findClientDemon();
        float currentHp = demon != null ? demon.getHealth() : ItemStackDataHelper.getFloat(pactStack, "CurrentHp", (float) demonType.getMaxHealth());
        float maxHp = demon != null ? demon.getMaxHealth() : ItemStackDataHelper.getFloat(pactStack, "MaxHp", (float) demonType.getMaxHealth());
        if (maxHp <= 0) maxHp = (float) demonType.getMaxHealth();
        float hpRatio = Math.max(0.0F, Math.min(1.0F, currentHp / maxHp));

        int barX = left + 14;
        int barY = top + 21;
        int barW = demonType == DemonType.BLANC ? 148 : 200;
        int barH = 12;
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF330000);
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0505);
        int fillW = (int) (barW * hpRatio);
        int hpColor = hpRatio > 0.5F ? 0xFF22C55E : (hpRatio > 0.25F ? 0xFFEAB308 : 0xFFEF4444);
        guiGraphics.fill(barX, barY, barX + fillW, barY + barH, hpColor);
        String hpText = demonType == DemonType.BLANC
                ? String.format("❤ %.0f/%.0f", currentHp, maxHp)
                : String.format("❤ HP: %.0f / %.0f (%.0f%%)", currentHp, maxHp, hpRatio * 100);
        guiGraphics.drawCenteredString(this.font, "§f§l" + hpText, barX + barW / 2, barY + 2, 0xFFFFFF);

        // Nút lệnh Thế trận (Stance) bên phải thanh máu
        int btnStanceX = left + 218;
        int btnW = 58;
        int btnH = 16;
        if (demonType == DemonType.BLANC) {
            boolean massacreActive = demon != null && demon.isMassacreMode();
            renderButton(guiGraphics, left + 166, barY - 2, 50, btnH, massacreActive ? "§4☠ Dừng Sát" : "§c☠ Tàn Sát", mouseX, mouseY);
        }
        renderButton(guiGraphics, btnStanceX, barY - 2, btnW, btnH, "§aĐi Theo", mouseX, mouseY);
        renderButton(guiGraphics, btnStanceX + 62, barY - 2, btnW, btnH, "§eCanh Giữ", mouseX, mouseY);
        renderButton(guiGraphics, btnStanceX + 124, barY - 2, btnW, btnH, "§cThu Hồi", mouseX, mouseY);
        renderButton(guiGraphics, btnStanceX + 186, barY - 2, 68, btnH, "§5👁 Khán Giả", mouseX, mouseY);

        // Nút Cổng Về Căn Cứ Nhanh
        renderButton(guiGraphics, btnStanceX + 258, barY - 2, 54, btnH, "§b🏠 Về Nhà", mouseX, mouseY);

        // 3. THANH TAB CHỨC NĂNG (TAB BAR)
        int tabY = top + 38;
        int tabW = 166;
        int tabH = 18;

        String tab0Title = activeTab == TAB_COMBAT ? "§e📜 HỒ SƠ & BỘ KỸ NĂNG" : "§7📜 Hồ Sơ & Kỹ Năng";
        renderTab(guiGraphics, left + 14, tabY, tabW, tabH, tab0Title, activeTab == TAB_COMBAT, mouseX, mouseY);

        String tab1Title;
        if (demonType == DemonType.ROUGE) {
            tab1Title = activeTab == TAB_GOVERNANCE ? "§c🎲 VÁN CỜ MA GIỚI" : "§7🎲 Ván Cờ";
        } else {
            tab1Title = activeTab == TAB_GOVERNANCE ? "§d📜 NGHIỆP VỤ & TƯ NIỆM" : "§7📜 Nghiệp Vụ & Tư Niệm";
        }
        renderTab(guiGraphics, left + 14 + tabW + 8, tabY, tabW, tabH, tab1Title, activeTab == TAB_GOVERNANCE, mouseX, mouseY);

        String tab2Title;
        if (demonType == DemonType.ROUGE) {
            tab2Title = activeTab == TAB_VAULT ? "§4🛡️ TRỌNG TÀI CAN THIỆP" : "§7🛡️ Trọng Tài Can Thiệp";
        } else {
            tab2Title = activeTab == TAB_VAULT ? "§b📦 KHO CĂN CỨ & QUẢN GIA" : "§7📦 Kho Căn Cứ & Quản Gia";
        }
        renderTab(guiGraphics, left + 14 + (tabW + 8) * 2, tabY, tabW, tabH, tab2Title, activeTab == TAB_VAULT, mouseX, mouseY);

        // Phản hồi thao tác hoặc hướng dẫn
        if (feedbackTimer > 0 && !statusFeedback.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, statusFeedback, left + PANEL_W / 2, top + 59, 0xFFFF55);
        } else {
            if (activeTab == TAB_COMBAT) {
                guiGraphics.drawString(this.font, "§d▶ HỒ SƠ & BỘ KỸ NĂNG: Click trực tiếp vào Chiêu Thức để ra lệnh cho Ác Ma thi triển ngay lập tức!", left + 14, top + 59, 0xAAAAAA);
            } else if (activeTab == TAB_GOVERNANCE) {
                guiGraphics.drawString(this.font, "§d▶ Truyền Đạt Tư Niệm, Thấu Thị Khoáng Mạch & Mở Cổng Thứ Nguyên Di Tích:", left + 14, top + 59, 0xAAAAAA);
            } else {
                guiGraphics.drawString(this.font, "§d▶ Quản Gia Hư Không tự động dọn rác cất rương & Mạng lưới căn cứ:", left + 14, top + 59, 0xAAAAAA);
            }
        }

        // Đường kẻ phân cách
        guiGraphics.fill(left + 12, top + 70, left + PANEL_W - 12, top + 71, 0x44FFFFFF);

        // =========================================================================
        // NỘI DUNG TỪNG TAB
        // =========================================================================
        if (activeTab == TAB_COMBAT) {
            renderCombatTab(guiGraphics, left, top, mouseX, mouseY);
        } else if (activeTab == TAB_GOVERNANCE) {
            if (demonType == DemonType.ROUGE) {
                renderGuyGambitsTab(guiGraphics, left, top, mouseX, mouseY);
            } else {
                renderGovernanceTab(guiGraphics, left, top, mouseX, mouseY);
            }
        } else if (activeTab == TAB_VAULT) {
            if (demonType == DemonType.ROUGE) {
                renderGuyLifelineTab(guiGraphics, left, top, mouseX, mouseY);
            } else {
                renderVaultTab(guiGraphics, left, top, mouseX, mouseY);
            }
        }
    }

    private void renderCombatTab(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        // 1. THANH CHỌN PHÂN LOẠI KỸ NĂNG (SUB-TABS)
        int subY = top + 73;
        int subW = 250;
        int subH = 16;
        int sub1X = left + 14;
        int sub2X = left + 14 + subW + 12;

        renderSubTab(guiGraphics, sub1X, subY, subW, subH, "🔥 BÍ THUẬT THỦY TỔ & THỨC TỈNH", combatSubTab == SUBTAB_INNATE, mouseX, mouseY);
        renderSubTab(guiGraphics, sub2X, subY, subW, subH, "✨ ĐẠI MA PHÁP KHẾ ƯỚC NGƯỜI CHƠI", combatSubTab == SUBTAB_CONTRACT, mouseX, mouseY);

        List<SkillEntry> currentList = combatSubTab == SUBTAB_INNATE ? innateSkills : contractSkills;
        int totalItems = currentList.size();
        int maxPages = Math.max(1, (totalItems + 7) / 8);
        if (combatPage >= maxPages) combatPage = maxPages - 1;

        int gridX = left + 14;
        int gridY = top + 93;
        int colW = 250;
        int cardH = 34;
        int gapX = 12;
        int gapY = 4;

        int startIndex = combatPage * 8;
        int endIndex = Math.min(startIndex + 8, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            SkillEntry s = currentList.get(i);
            int slot = i - startIndex;
            int col = slot % 2;
            int row = slot / 2;
            int cardX = gridX + col * (colW + gapX);
            int cardY = gridY + row * (cardH + gapY);

            boolean hovered = mouseX >= cardX && mouseX <= cardX + colW && mouseY >= cardY && mouseY <= cardY + cardH;
            int bg = hovered ? (s.locked ? 0xFF281212 : 0xFF2A1515) : (s.locked ? 0xFF140A0A : 0xFF180A0A);
            int border = hovered ? (s.locked ? 0xFF882222 : 0xFFFF4444) : (s.locked ? 0xFF441818 : 0xFF5A1A1A);

            guiGraphics.fill(cardX, cardY, cardX + colW, cardY + cardH, bg);
            guiGraphics.renderOutline(cardX, cardY, colW, cardH, border);

            // Icon bar màu sắc
            int iconColor = s.locked ? 0xFF555555 : (0xFF000000 | s.colorHex);
            guiGraphics.fill(cardX + 4, cardY + 4, cardX + 8, cardY + cardH - 4, iconColor);

            // Tên chiêu thức (cắt chuẩn xác chống tràn viền tuyệt đối)
            String prefix = hovered ? "§e▶ " : (s.locked ? "§7[🔒] " : "§f");
            String titleDisplay = this.font.plainSubstrByWidth(s.title, colW - 24);
            guiGraphics.drawString(this.font, prefix + titleDisplay, cardX + 12, cardY + 5, s.locked ? 0x888888 : 0xFFFFFF);

            // Mô tả hoặc điều kiện mở khóa (cắt chuẩn xác chống tràn)
            String descDisplay = s.locked ? ("§c" + s.lockReason) : ("§7" + s.desc);
            descDisplay = this.font.plainSubstrByWidth(descDisplay, colW - 24);
            guiGraphics.drawString(this.font, descDisplay, cardX + 12, cardY + 18, 0x888888);

            // Tooltip khi hover: hiển thị thông tin đầy đủ không bao giờ bị cắt
            if (hovered) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal((s.locked ? "§c[🔒 KHÓA] " : "§6[CHIÊU THỨC] ") + s.title));
                tooltip.add(Component.literal("§7" + s.desc));
                if (s.locked) {
                    tooltip.add(Component.literal("§c⚠️ Điều kiện mở khóa: " + s.lockReason));
                } else {
                    tooltip.add(Component.literal("§a✔ Đã mở khóa. Click chuột trái để ra lệnh cho Ác Ma thi triển ngay lập tức!"));
                }
                guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }

        // Thanh phân trang nếu có nhiều hơn 1 trang
        if (maxPages > 1) {
            int pageY = top + 242;
            int pageBtnW = 75;
            int pageBtnH = 14;
            int centerX = left + PANEL_W / 2;

            renderButton(guiGraphics, centerX - 100, pageY, pageBtnW, pageBtnH, "◀ Trang Trước", mouseX, mouseY);
            guiGraphics.drawCenteredString(this.font, "§eTrang " + (combatPage + 1) + " / " + maxPages, centerX, pageY + 3, 0xFFFFFF);
            renderButton(guiGraphics, centerX + 25, pageY, pageBtnW, pageBtnH, "Trang Sau ▶", mouseX, mouseY);
        }
    }

    private void renderGovernanceTab(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int startX = left + 14;
        int startY = top + 74;
        int cardW = 248;
        int cardH = 82;
        int btnW = 224;
        int btnH = 18;

        String lockedTarget = ItemStackDataHelper.getString(pactStack, "Sensory_TargetName");
        boolean hasTarget = !lockedTarget.isEmpty();

        // 1. Card Truyền Đạt Tư Niệm
        renderOperationCard(guiGraphics, startX, startY, cardW, cardH,
                "§d✦ 1. TRUYỀN ĐẠT TƯ NIỆM",
                getDemonSensoryDesc(demonType),
                "§d[ ⚡ KÍCH HOẠT QUÉT TÂM TRÍ ]",
                startX + (cardW - btnW) / 2, startY + 56, btnW, btnH, mouseX, mouseY);

        // 2. Card Khai Thác Từ Xa
        String extractBtnText = hasTarget ? "§6[ 💥 BỘC PHÁ & THU VỀ ]" : "§8[ ✕ Chưa Khóa Mục Tiêu ]";
        String extractDesc = hasTarget ? "§aĐã khóa: §e" + lockedTarget + "\n§7Bấm để nổ quặng và hút thẳng về tay."
                : "§7Chưa có mục tiêu. Hãy bấm Quét Tâm Trí ở bên trái trước.";
        renderOperationCard(guiGraphics, startX + cardW + 16, startY, cardW, cardH,
                "§6✦ 2. BỘC PHÁ KHAI THÁC",
                extractDesc,
                extractBtnText,
                startX + cardW + 16 + (cardW - btnW) / 2, startY + 56, btnW, btnH, mouseX, mouseY);

        // 3. Card Cổng Thứ Nguyên (Không lặp lại)
        int row2Y = startY + cardH + 8;
        renderOperationCard(guiGraphics, startX, row2Y, cardW, cardH,
                "§5✦ 3. CỔNG MA GIỚI (DI TÍCH)",
                "§7Mở cổng an toàn đến di tích mới toanh. Chỗ nào đã đi sẽ không bao giờ lặp lại.",
                "§5[ 🚪 MỞ CỔNG DỊCH CHUYỂN ]",
                startX + (cardW - btnW) / 2, row2Y + 56, btnW, btnH, mouseX, mouseY);

        // 4. Card Đặc quyền riêng (Blanc: Lãnh địa làng / Others: Dịch chuyển căn cứ)
        if (demonType == DemonType.BLANC) {
            int pLevel = Math.max(1, ItemStackDataHelper.getInt(pactStack, "Protectorate_Level"));
            boolean hasProtectorate = ItemStackDataHelper.hasKey(pactStack, "ProtectedVillage_Pos");
            String cardTitle = hasProtectorate
                    ? "§f✦ 4. LÃNH ĐỊA BLANC (CẤP " + pLevel + "/5)"
                    : "§f✦ 4. ĐẠI LÃNH ĐỊA BẢO HỘ LÀNG";
            String cardDesc = "§7Kết giới 85m diệt Raid, triệu hồi Golem Bạch Viêm, nâng nghề Master, tự kiến thiết & thu cống phẩm.";
            renderOperationCard(guiGraphics, startX + cardW + 16, row2Y, cardW, cardH,
                    cardTitle,
                    cardDesc,
                    "§f[ 🏛️ THIẾT LẬP & CƯỜNG HÓA ]",
                    startX + cardW + 16 + (cardW - btnW) / 2, row2Y + 56, btnW, btnH, mouseX, mouseY);
        } else {
            renderOperationCard(guiGraphics, startX + cardW + 16, row2Y, cardW, cardH,
                    "§a✦ 4. CỔNG VỀ RƯƠNG CĂN CỨ",
                    "§7Xé rách không gian bay thẳng về Rương Căn Cứ đã liên kết an toàn tuyệt đối.",
                    "§a[ 🏠 DỊCH CHUYỂN VỀ NHÀ ]",
                    startX + cardW + 16 + (cardW - btnW) / 2, row2Y + 56, btnW, btnH, mouseX, mouseY);
        }
    }

    private void renderVaultTab(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int startX = left + 14;
        int startY = top + 76;

        boolean autoHoover = ItemStackDataHelper.getBoolean(pactStack, "AutoHoover_Enabled");
        boolean hasVault = ItemStackDataHelper.hasKey(pactStack, "BoundVault_Pos");
        BlockPos vPos = hasVault ? BlockPos.of(ItemStackDataHelper.getLong(pactStack, "BoundVault_Pos", 0L)) : null;

        guiGraphics.fill(startX, startY, startX + 512, startY + 70, 0xFF180A0A);
        guiGraphics.renderOutline(startX, startY, 512, 70, 0xFF661E1E);

        guiGraphics.drawString(this.font, "§e✦ QUẢN GIA HƯ KHÔNG (AUTO-HOOVER & LOGISTICS):", startX + 12, startY + 8, 0xFFFFFF);
        String hooverStatus = autoHoover ? "§a[ĐANG BẬT] - Tự động thu dọn item rơi vãi khi túi đồ đầy" : "§c[ĐANG TẮT] - Không tự động thu dọn";
        guiGraphics.drawString(this.font, "§7Chế độ thu dọn: " + hooverStatus, startX + 12, startY + 22, 0xCCCCCC);

        String vaultInfo = hasVault
                ? "§a✔ Rương căn cứ đã liên kết tại: §e[" + vPos.getX() + ", " + vPos.getY() + ", " + vPos.getZ() + "]"
                : "§c✕ Chưa liên kết rương căn cứ! (Cúi người Shift + Chuột Phải vào rương để đăng ký)";
        guiGraphics.drawString(this.font, vaultInfo, startX + 12, startY + 36, 0xCCCCCC);

        renderButton(guiGraphics, startX + 12, startY + 48, 160, 16, autoHoover ? "§c✕ Tắt Thu Dọn" : "§a✔ Bật Thu Dọn", mouseX, mouseY);
        renderButton(guiGraphics, startX + 180, startY + 48, 160, 16, "§b🏠 Dịch Chuyển Về Rương", mouseX, mouseY);

        // Hướng dẫn
        int guideY = startY + 80;
        guiGraphics.fill(startX, guideY, startX + 512, guideY + 90, 0xFF140808);
        guiGraphics.renderOutline(startX, guideY, 512, 90, 0xFF4A1818);

        guiGraphics.drawString(this.font, "§6📜 CẨM NANG NGHIỆP VỤ & QUẢN GIA HƯ KHÔNG:", startX + 12, guideY + 8, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§71. Cầm cuốn khế ước cúi người (Shift) + Chuột Phải vào bất kỳ Rương nào ở nhà để đăng ký.", startX + 12, guideY + 24, 0xAAAAAA);
        guiGraphics.drawString(this.font, "§72. Khi bạn đào khoáng hoặc chiến đấu mà túi đồ đầy, Ác Ma tự động hút item rơi cất về rương.", startX + 12, guideY + 38, 0xAAAAAA);
        guiGraphics.drawString(this.font, "§73. Tính năng Truyền Đạt Tư Niệm có thể quét xuyên tường các quặng quý và bộc phá từ xa.", startX + 12, guideY + 52, 0xAAAAAA);
        guiGraphics.drawString(this.font, "§74. Cổng Thứ Nguyên tìm kiếm cấu trúc chưa từng đến và đánh dấu đã chinh phục (chống lặp).", startX + 12, guideY + 66, 0xAAAAAA);
    }

    private void renderGuyGambitsTab(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int startX = left + 14;
        int startY = top + 76;

        guiGraphics.fill(startX, startY, startX + 512, startY + 44, 0xFF2A0808);
        guiGraphics.renderOutline(startX, startY, 512, 44, 0xFFFF3333);

        guiGraphics.drawString(this.font, "§4§l✦ VÁN CỜ MA GIỚI CỦA GUY CRIMSON ✦ §7(Đẳng cấp ngang hàng - Không nhận mệnh lệnh)", startX + 12, startY + 6, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§c\"Hmph... Ngươi nghĩ ngươi có quyền ra lệnh cho ta sao? Hãy chứng minh thực lực của ngươi đi!\"", startX + 12, startY + 20, 0xFFAAAA);
        guiGraphics.drawString(this.font, "§7Hoàn tất các thử thách vương giả bên dưới để Guy Crimson ban phát Thần Vật tối thượng:", startX + 12, startY + 32, 0x888888);

        // Thử thách 1: Hiến tế 16 linh hồn đổi Ngọc thức tỉnh
        int card1Y = startY + 52;
        renderOperationCard(guiGraphics, startX, card1Y, 512, 48,
                "§e🎲 THỬ THÁCH 1: HIẾN TẾ 16 LINH HỒN MA VƯƠNG",
                "Đem 16x Linh Hồn Ma Vương (Mob Soul) chứng minh thực lực để đổi lấy 1x [Ngọc Thức Tỉnh Ma Vương]!",
                "§e[ ĐỔI VIÊN NGỌC THỨC TỈNH ]",
                startX + 330, card1Y + 22, 170, 18, mouseX, mouseY);

        // Thử thách 2: Trọng lượng kim loại
        int card2Y = card1Y + 56;
        renderOperationCard(guiGraphics, startX, card2Y, 512, 48,
                "§6🎲 THỬ THÁCH 2: THỎI NETHERITE MA HỎA",
                "Đổi cống phẩm chiến trận để nhận ngay 4x Thỏi Netherite rực lửa ma giới từ Guy Crimson.",
                "§6[ NHẬN 4x THỎI NETHERITE ]",
                startX + 330, card2Y + 22, 170, 18, mouseX, mouseY);
    }

    private void renderGuyLifelineTab(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int startX = left + 14;
        int startY = top + 76;

        guiGraphics.fill(startX, startY, startX + 512, startY + 160, 0xFF2A0808);
        guiGraphics.renderOutline(startX, startY, 512, 160, 0xFFFF3333);

        guiGraphics.drawString(this.font, "§4§l✦ ĐẶC QUYỀN ĐỒNG MINH: TRỌNG TÀI GIÁNG LÂM (GUY'S LIFELINE) ✦", startX + 16, startY + 12, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§eTrạng thái can thiệp: §a[LUÔN SẴN SÀNG - TỰ ĐỘNG BẢO HỘ]", startX + 16, startY + 30, 0xCCCCCC);

        guiGraphics.drawString(this.font, "§c❖ Khi máu của bạn tụt xuống dưới 20% HP (hoặc đối mặt với sát chiêu nguy tử):", startX + 16, startY + 52, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§7  1. Guy Crimson lập tức xé rách không gian giáng lâm cứu viện tức khắc.", startX + 24, startY + 68, 0xCCCCCC);
        guiGraphics.drawString(this.font, "§7  2. Đẩy lùi toàn bộ quái vật và boss xung quanh 20m, thiêu đốt bằng lửa ma giới.", startX + 24, startY + 84, 0xCCCCCC);
        guiGraphics.drawString(this.font, "§7  3. Hồi phục ngay lập tức 80% máu tối đa, ban Kháng Cự Cực Hạn + Hồi Phục V.", startX + 24, startY + 100, 0xCCCCCC);
        guiGraphics.drawString(this.font, "§7  4. Thời gian hồi phục của can thiệp trọng tài: 1 ngày Minecraft (24,000 ticks).", startX + 24, startY + 116, 0xCCCCCC);

        guiGraphics.drawCenteredString(this.font, "§6\"Đừng có gục ngã ở nơi thảm hại này chứ, kẻ thú vị...\"", startX + 256, startY + 138, 0xFFD700);
    }

    private void renderOperationCard(GuiGraphics guiGraphics, int x, int y, int w, int h, String title, String desc, String btnText,
                                     int btnX, int btnY, int btnW, int btnH, int mouseX, int mouseY) {
        guiGraphics.fill(x, y, x + w, y + h, 0xFF1C0A0A);
        guiGraphics.renderOutline(x, y, w, h, 0xFF661E1E);

        guiGraphics.drawString(this.font, title, x + 8, y + 7, 0xFFFFFF);

        // Tính toán độ rộng bọc chữ an toàn:
        // Nếu nút bấm đặt bên phải (như ở Guy Gambits), ngắt dòng trước nút bấm để không đè lên nút
        int wrapW = (btnX > x + w / 2) ? Math.max(120, (btnX - x) - 16) : (w - 16);
        guiGraphics.drawWordWrap(this.font, Component.literal(desc), x + 8, y + 20, wrapW, 0xCCCCCC);

        renderButton(guiGraphics, btnX, btnY, btnW, btnH, btnText, mouseX, mouseY);
    }

    private String getDemonSensoryDesc(DemonType type) {
        return switch (type) {
            case JAUNE -> "§7Quét tìm quặng quý (Netherite, Kim Cương) xuyên tường.";
            case NOIR -> "§7Dò tìm rương kho báu ngầm và di tích bí mật xung quanh.";
            case BLANC -> "§7Dò tìm thôn làng, quần thể dân làng và mỏ ngọc lục bảo.";
            case VIOLET -> "§7Dò tìm Boss sử thi, quái tinh anh và sào huyệt nguy hiểm.";
            case BLEU -> "§7Dò tìm hang động thạch anh tím và tinh thể ma thuật.";
            case VERT -> "§7Dò tìm hệ thống kho bãi, rương chứa và cây trồng căn cứ.";
            case ROUGE -> "§7Cảm tri thực thể mang ma lực áp đảo (>100 HP) và Long Chủng.";
        };
    }

    private void renderTab(GuiGraphics guiGraphics, int x, int y, int w, int h, String text, boolean active, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = active ? 0xFF551818 : (hovered ? 0xFF330C0C : 0xFF1A0606);
        int border = active ? 0xFFFF7777 : (hovered ? 0xFFB22222 : 0xFF4A1010);
        guiGraphics.fill(x, y, x + w, y + h, bg);
        guiGraphics.renderOutline(x, y, w, h, border);
        guiGraphics.drawCenteredString(this.font, text, x + w / 2, y + 5, 0xFFFFFF);
    }

    private void renderSubTab(GuiGraphics guiGraphics, int x, int y, int w, int h, String text, boolean active, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = active ? 0xFF4A1818 : (hovered ? 0xFF2A0C0C : 0xFF160606);
        int border = active ? 0xFFFF6666 : (hovered ? 0xFFA02020 : 0xFF3D1010);
        guiGraphics.fill(x, y, x + w, y + h, bg);
        guiGraphics.renderOutline(x, y, w, h, border);
        guiGraphics.drawCenteredString(this.font, (active ? "§e§l" : "§7") + text, x + w / 2, y + 4, 0xFFFFFF);
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int w, int h, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hovered ? 0xFF441818 : 0xFF220C0C;
        int border = hovered ? 0xFFFF5555 : 0xFF661E1E;
        guiGraphics.fill(x, y, x + w, y + h, bg);
        guiGraphics.renderOutline(x, y, w, h, border);
        guiGraphics.drawCenteredString(this.font, text, x + w / 2, y + 4, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = (this.width - PANEL_W) / 2;
            int top = (this.height - PANEL_H) / 2;
            int barY = top + 21;
            int btnStanceX = left + 218;
            int btnW = 58;
            int btnH = 16;

            // Nút 0 (Blanc): Chế độ Tàn Sát Thôn Làng
            if (demonType == DemonType.BLANC && mouseX >= left + 166 && mouseX <= left + 166 + 50 && mouseY >= barY - 2 && mouseY <= barY - 2 + btnH) {
                playClick();
                ModMessages.sendToServer(new ServerboundDemonCommandPacket(ServerboundDemonCommandPacket.ACTION_TOGGLE_MASSACRE, 0));
                PrimordialDemonEntity demon = findClientDemon();
                boolean massacreNow = demon == null || !demon.isMassacreMode();
                showFeedback(massacreNow ? "§4☠ Đã phát lệnh: BLANC TÀN SÁT TOÀN BỘ DÂN LÀNG & IRON GOLEM!" : "§a✔ Đã dừng chế độ Tàn Sát của Blanc!");
                return true;
            }

            // Nút 1: Đi Theo
            if (mouseX >= btnStanceX && mouseX <= btnStanceX + btnW && mouseY >= barY - 2 && mouseY <= barY - 2 + btnH) {
                playClick();
                ModMessages.sendToServer(new ServerboundDemonCommandPacket(ServerboundDemonCommandPacket.ACTION_STANCE_FOLLOW, 0));
                showFeedback("§a✔ Đã ra lệnh: ĐI THEO BẢO VỆ CHỦ NHÂN!");
                return true;
            }

            // Nút 2: Canh Giữ
            if (mouseX >= btnStanceX + 62 && mouseX <= btnStanceX + 62 + btnW && mouseY >= barY - 2 && mouseY <= barY - 2 + btnH) {
                playClick();
                ModMessages.sendToServer(new ServerboundDemonCommandPacket(ServerboundDemonCommandPacket.ACTION_STANCE_GUARD, 0));
                showFeedback("§e✔ Đã ra lệnh: CANH GIỮ VỊ TRÍ NÀY!");
                return true;
            }

            // Nút 3: Thu Hồi
            if (mouseX >= btnStanceX + 124 && mouseX <= btnStanceX + 124 + btnW && mouseY >= barY - 2 && mouseY <= barY - 2 + btnH) {
                playClick();
                ModMessages.sendToServer(new ServerboundDemonCommandPacket(ServerboundDemonCommandPacket.ACTION_DISMISS, 0));
                this.onClose();
                return true;
            }

            // Nút 4: Khán Giả để nhập xác
            if (mouseX >= btnStanceX + 186 && mouseX <= btnStanceX + 186 + 68 && mouseY >= barY - 2 && mouseY <= barY - 2 + btnH) {
                playClick();
                if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.connection != null) {
                    this.minecraft.player.connection.sendCommand("gamemode spectator");
                    showFeedback("§5👁 Đã chuyển sang Khán Giả! Hãy click vào Ác Ma để nhập xác!");
                }
                this.onClose();
                return true;
            }

            // Nút 5: Về Nhà Nhanh
            if (mouseX >= btnStanceX + 258 && mouseX <= btnStanceX + 258 + 54 && mouseY >= barY - 2 && mouseY <= barY - 2 + btnH) {
                playClick();
                ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_WARP_HOME, 0));
                this.onClose();
                return true;
            }

            // CLICK TAB BAR
            int tabY = top + 38;
            int tabW = 166;
            int tabH = 18;
            if (mouseY >= tabY && mouseY <= tabY + tabH) {
                if (mouseX >= left + 14 && mouseX <= left + 14 + tabW) {
                    playClick();
                    activeTab = TAB_COMBAT;
                    return true;
                } else if (mouseX >= left + 14 + tabW + 8 && mouseX <= left + 14 + (tabW + 8) * 2 - 8) {
                    playClick();
                    activeTab = TAB_GOVERNANCE;
                    return true;
                } else if (mouseX >= left + 14 + (tabW + 8) * 2 && mouseX <= left + 14 + (tabW + 8) * 3) {
                    playClick();
                    activeTab = TAB_VAULT;
                    return true;
                }
            }

            // XỬ LÝ CLICK TRONG TAB TÁC CHIẾN
            if (activeTab == TAB_COMBAT) {
                int subY = top + 73;
                int subW = 250;
                int subH = 16;
                int sub1X = left + 14;
                int sub2X = left + 14 + subW + 12;

                // Click Sub-Tab 1: Bí Thuật Thủy Tổ
                if (mouseX >= sub1X && mouseX <= sub1X + subW && mouseY >= subY && mouseY <= subY + subH) {
                    playClick();
                    combatSubTab = SUBTAB_INNATE;
                    combatPage = 0;
                    return true;
                }

                // Click Sub-Tab 2: Đại Ma Pháp Khế Ước
                if (mouseX >= sub2X && mouseX <= sub2X + subW && mouseY >= subY && mouseY <= subY + subH) {
                    playClick();
                    combatSubTab = SUBTAB_CONTRACT;
                    combatPage = 0;
                    return true;
                }

                List<SkillEntry> currentList = combatSubTab == SUBTAB_INNATE ? innateSkills : contractSkills;
                int totalItems = currentList.size();
                int maxPages = Math.max(1, (totalItems + 7) / 8);

                // Click Nút Phân Trang
                if (maxPages > 1) {
                    int pageY = top + 242;
                    int pageBtnW = 75;
                    int pageBtnH = 14;
                    int centerX = left + PANEL_W / 2;

                    // Nút Trang Trước
                    if (mouseX >= centerX - 100 && mouseX <= centerX - 100 + pageBtnW && mouseY >= pageY && mouseY <= pageY + pageBtnH) {
                        if (combatPage > 0) {
                            playClick();
                            combatPage--;
                        }
                        return true;
                    }

                    // Nút Trang Sau
                    if (mouseX >= centerX + 25 && mouseX <= centerX + 25 + pageBtnW && mouseY >= pageY && mouseY <= pageY + pageBtnH) {
                        if (combatPage < maxPages - 1) {
                            playClick();
                            combatPage++;
                        }
                        return true;
                    }
                }

                // Click vào từng thẻ kỹ năng
                int gridX = left + 14;
                int gridY = top + 93;
                int colW = 250;
                int cardH = 34;
                int gapX = 12;
                int gapY = 4;

                int startIndex = combatPage * 8;
                int endIndex = Math.min(startIndex + 8, totalItems);

                for (int i = startIndex; i < endIndex; i++) {
                    SkillEntry s = currentList.get(i);
                    int slot = i - startIndex;
                    int col = slot % 2;
                    int row = slot / 2;
                    int cardX = gridX + col * (colW + gapX);
                    int cardY = gridY + row * (cardH + gapY);

                    if (mouseX >= cardX && mouseX <= cardX + colW && mouseY >= cardY && mouseY <= cardY + cardH) {
                        playClick();
                        if (s.locked) {
                            showFeedback("§c⚠️ Kỹ năng đang khóa! " + s.lockReason);
                        } else {
                            ModMessages.sendToServer(new ServerboundDemonCommandPacket(ServerboundDemonCommandPacket.ACTION_CAST_SKILL, s.index));
                            showFeedback("§a⚡ Đã ra lệnh cho Ác Ma thi triển: §e" + s.title);
                            if (s.index == 109) {
                                this.onClose();
                            }
                        }
                        return true;
                    }
                }
            }

            // XỬ LÝ CLICK TRONG TAB NGHIỆP VỤ & TƯ NIỆM
            if (activeTab == TAB_GOVERNANCE) {
                int startX = left + 14;
                int startY = top + 74;
                int cardW = 248;
                int cardH = 82;
                int opBtnW = 224;
                int opBtnH = 18;

                // Nút 1: Kích Hoạt Quét Tâm Trí
                int btn1X = startX + (cardW - opBtnW) / 2;
                int btn1Y = startY + 56;
                if (mouseX >= btn1X && mouseX <= btn1X + opBtnW && mouseY >= btn1Y && mouseY <= btn1Y + opBtnH) {
                    playClick();
                    ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_SENSORY_SCAN, 0));
                    showFeedback("§d⚡ Đã gửi tư niệm dò tìm khoáng mạch & mục tiêu xung quanh!");
                    return true;
                }

                // Nút 2: Bộc Phá & Thu Về
                int btn2X = startX + cardW + 16 + (cardW - opBtnW) / 2;
                int btn2Y = startY + 56;
                if (mouseX >= btn2X && mouseX <= btn2X + opBtnW && mouseY >= btn2Y && mouseY <= btn2Y + opBtnH) {
                    String lockedTarget = ItemStackDataHelper.getString(pactStack, "Sensory_TargetName");
                    if (lockedTarget.isEmpty()) {
                        playClick();
                        showFeedback("§c✕ Chưa có mục tiêu! Hãy bấm Quét Tâm Trí trước.");
                        return true;
                    }
                    playClick();
                    ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_REMOTE_EXTRACT, 0));
                    showFeedback("§6💥 Đang bộc phá mục tiêu và chuyển vật phẩm về túi đồ!");
                    return true;
                }

                // Nút 3: Mở Cổng Dịch Chuyển Di Tích (Không lặp lại)
                int row2Y = startY + cardH + 8;
                int btn3X = startX + (cardW - opBtnW) / 2;
                int btn3Y = row2Y + 56;
                if (mouseX >= btn3X && mouseX <= btn3X + opBtnW && mouseY >= btn3Y && mouseY <= btn3Y + opBtnH) {
                    playClick();
                    ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_PORTAL_WARP, 0));
                    showFeedback("§5🚪 Đang xé rách không gian mở Cổng Di Tích Mới...");
                    this.onClose();
                    return true;
                }

                // Nút 4: Đặc quyền riêng (Blanc: Lãnh địa làng / Others: Về nhà)
                int btn4X = startX + cardW + 16 + (cardW - opBtnW) / 2;
                int btn4Y = row2Y + 56;
                if (mouseX >= btn4X && mouseX <= btn4X + opBtnW && mouseY >= btn4Y && mouseY <= btn4Y + opBtnH) {
                    playClick();
                    if (demonType == DemonType.BLANC) {
                        ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_VILLAGE_PROTECTORATE, 0));
                        showFeedback("§f🏛️ Đang thiết lập Lãnh Địa Bảo Hộ Thôn Làng...");
                    } else {
                        ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_WARP_HOME, 0));
                        showFeedback("§a🏠 Đang mở cổng trở về Rương Căn Cứ...");
                    }
                    this.onClose();
                    return true;
                }
            }

            // XỬ LÝ CLICK TRONG TAB KHO CĂN CỨ & QUẢN GIA
            if (activeTab == TAB_VAULT) {
                int startX = left + 14;
                int startY = top + 76;

                // Nút 1: Bật / Tắt Quản Gia Hư Không
                if (mouseX >= startX + 12 && mouseX <= startX + 12 + 160 && mouseY >= startY + 48 && mouseY <= startY + 48 + 16) {
                    playClick();
                    ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_TOGGLE_AUTO_HOOVER, 0));
                    boolean current = ItemStackDataHelper.getBoolean(pactStack, "AutoHoover_Enabled");
                    ItemStackDataHelper.putBoolean(pactStack, "AutoHoover_Enabled", !current);
                    showFeedback(!current ? "§a✔ Đã BẬT Quản Gia Hư Không!" : "§c✕ Đã TẮT Quản Gia Hư Không!");
                    return true;
                }

                // Nút 2: Dịch chuyển về Rương
                if (mouseX >= startX + 180 && mouseX <= startX + 180 + 160 && mouseY >= startY + 48 && mouseY <= startY + 48 + 16) {
                    playClick();
                    ModMessages.sendToServer(new ServerboundAbyssalActionPacket(ServerboundAbyssalActionPacket.ACTION_WARP_HOME, 0));
                    this.onClose();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClick() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
        }
    }

    private void showFeedback(String msg) {
        this.statusFeedback = msg;
        this.feedbackTimer = 60; // 3 giây
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
