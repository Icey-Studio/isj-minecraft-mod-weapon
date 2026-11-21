package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện Khởi Nguyên Thần Thư (GuideBookScreen).
 * Tự động xuống dòng chuẩn tiếng Việt theo chiều rộng khung sách, căn hàng gọn gàng,
 * có Mục Lục nhảy trang nhanh và bổ sung đầy đủ toàn bộ hệ thống tính năng trong mod.
 */
public class GuideBookScreen extends Screen {

    private int currentPage = 0;
    private static final int TOTAL_PAGES = 31;
    private static final int PAGE_TOC = 1;
    private static final int PAGE_ARMOR = 4;
    private static final int PAGE_AUTHOR = TOTAL_PAGES - 1;

    private static final ResourceLocation DRAGON_NOVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/dragon_nova_art.png");
    private static final ResourceLocation TAISUI_ART_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/taisui_art.png");
    private static final ResourceLocation SEER_FLESH_ART_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/seer_flesh_art.png");
    private static final ResourceLocation SEER_ARM_COMBO_ART_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/seer_arm_combo_art.png");
    private static final ResourceLocation CARRERA_ART_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/carrera_art.png");

    private static final int MAX_BOOK_WIDTH = 446;
    private static final int MAX_BOOK_HEIGHT = 252;
    private static final int LINE_HEIGHT = 10;

    private Button prevButton;
    private Button tocButton;
    private Button closeButton;
    private Button nextButton;

    // Nút chọn mảnh giáp (Trang Thần Linh Vũ Trang)
    private Button armorHelmBtn;
    private Button armorChestBtn;
    private Button armorLegsBtn;
    private Button armorBootsBtn;
    private int selectedArmorPiece = 1; // 0: Mũ, 1: Áo, 2: Quần, 3: Giày

    // Nút nhảy trang trên Mục Lục
    private final List<Button> tocJumpButtons = new ArrayList<>();

    // Nút trang Tác Giả
    private Button openGithubBtn;
    private Button copyGithubBtn;
    private static final String GITHUB_URL = "https://github.com/MinhPhuc00ne/isj-minecraft-mod-weapon";

    private ItemStack hoveredTooltipStack = null;

    // Cache lưới công thức 3x3
    private final ItemStack[][] moonlightSwordRecipe = new ItemStack[3][3];
    private final ItemStack[][] demonLordSeedRecipe = new ItemStack[3][3];
    private final ItemStack[][][] divineArmorRecipes = new ItemStack[4][3][3];

    public GuideBookScreen() {
        super(Component.translatable("gui.weapons.guide.screen_title"));
        initRecipes();
    }

    private int getBookWidth() {
        return Math.min(MAX_BOOK_WIDTH, Math.max(340, this.width - 12));
    }

    private int getBookHeight() {
        return Math.min(MAX_BOOK_HEIGHT, Math.max(216, this.height - 12));
    }

    private void initRecipes() {
        moonlightSwordRecipe[0][0] = new ItemStack(Items.DIAMOND);
        moonlightSwordRecipe[0][1] = new ItemStack(Items.NETHER_STAR);
        moonlightSwordRecipe[0][2] = new ItemStack(Items.DIAMOND);
        moonlightSwordRecipe[1][0] = new ItemStack(Items.ENDER_EYE);
        moonlightSwordRecipe[1][1] = new ItemStack(Items.GOLDEN_APPLE);
        moonlightSwordRecipe[1][2] = new ItemStack(Items.ENDER_EYE);
        moonlightSwordRecipe[2][0] = new ItemStack(Items.GOLD_INGOT);
        moonlightSwordRecipe[2][1] = new ItemStack(Items.DIAMOND_SWORD);
        moonlightSwordRecipe[2][2] = new ItemStack(Items.GOLD_INGOT);

        demonLordSeedRecipe[0][0] = new ItemStack(Items.OBSIDIAN);
        demonLordSeedRecipe[0][1] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[0][2] = new ItemStack(Items.OBSIDIAN);
        demonLordSeedRecipe[1][0] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[1][1] = new ItemStack(Items.NETHER_STAR);
        demonLordSeedRecipe[1][2] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[2][0] = new ItemStack(Items.OBSIDIAN);
        demonLordSeedRecipe[2][1] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[2][2] = new ItemStack(Items.OBSIDIAN);

        ItemStack[] netheriteArmors = new ItemStack[]{
                new ItemStack(Items.NETHERITE_HELMET),
                new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS),
                new ItemStack(Items.NETHERITE_BOOTS)
        };

        for (int p = 0; p < 4; p++) {
            divineArmorRecipes[p][0][0] = new ItemStack(Items.GOLD_BLOCK);
            divineArmorRecipes[p][0][1] = new ItemStack(Items.DIAMOND);
            divineArmorRecipes[p][0][2] = new ItemStack(Items.GOLD_BLOCK);
            divineArmorRecipes[p][1][0] = new ItemStack(Items.ENDER_EYE);
            divineArmorRecipes[p][1][1] = netheriteArmors[p];
            divineArmorRecipes[p][1][2] = new ItemStack(Items.ENDER_EYE);
            divineArmorRecipes[p][2][0] = new ItemStack(Items.GOLD_BLOCK);
            divineArmorRecipes[p][2][1] = new ItemStack(Items.DIAMOND);
            divineArmorRecipes[p][2][2] = new ItemStack(Items.GOLD_BLOCK);
        }
    }

    @Override
    protected void init() {
        super.init();
        tocJumpButtons.clear();

        int bookW = getBookWidth();
        int bookH = getBookHeight();
        int left = (this.width - bookW) / 2;
        int top = (this.height - bookH) / 2;
        int navY = top + bookH - 25;

        prevButton = Button.builder(Component.translatable("gui.weapons.guide.btn.prev"), b -> {
            if (currentPage > 0) currentPage--;
            updateButtonState();
        }).bounds(left + 12, navY, 88, 18).build();

        tocButton = Button.builder(Component.translatable("gui.weapons.guide.btn.toc"), b -> {
            currentPage = PAGE_TOC;
            updateButtonState();
        }).bounds(left + bookW / 2 - 66, navY, 64, 18).build();

        closeButton = Button.builder(Component.translatable("gui.weapons.guide.btn.close"), b -> {
            this.onClose();
        }).bounds(left + bookW / 2 + 2, navY, 64, 18).build();

        nextButton = Button.builder(Component.translatable("gui.weapons.guide.btn.next"), b -> {
            if (currentPage < TOTAL_PAGES - 1) currentPage++;
            updateButtonState();
        }).bounds(left + bookW - 100, navY, 88, 18).build();

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(tocButton);
        this.addRenderableWidget(closeButton);
        this.addRenderableWidget(nextButton);

        // Các nút chuyển giáp (Trang Thần Linh Vũ Trang)
        int armorBtnY = top + 76;
        armorHelmBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_helm"), b -> selectedArmorPiece = 0)
                .bounds(left + 132, armorBtnY, 44, 16).build();
        armorChestBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_chest"), b -> selectedArmorPiece = 1)
                .bounds(left + 180, armorBtnY, 44, 16).build();
        armorLegsBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_legs"), b -> selectedArmorPiece = 2)
                .bounds(left + 228, armorBtnY, 44, 16).build();
        armorBootsBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_boots"), b -> selectedArmorPiece = 3)
                .bounds(left + 276, armorBtnY, 44, 16).build();

        this.addRenderableWidget(armorHelmBtn);
        this.addRenderableWidget(armorChestBtn);
        this.addRenderableWidget(armorLegsBtn);
        this.addRenderableWidget(armorBootsBtn);

        // Các nút Mục Lục nhảy trang nhanh (2 cột x 7 hàng)
        addTocButton(left, top, bookW, 0, "01 · Tổng Quan & Phím Tắt", 0);
        addTocButton(left, top, bookW, 1, "03 · Nguyệt Quang Thần Tế Kiếm", 2);
        addTocButton(left, top, bookW, 2, "04 · Thức Tỉnh Chân Ma Vương", 3);
        addTocButton(left, top, bookW, 3, "05 · Thần Linh Vũ Trang (Uriel)", 4);
        addTocButton(left, top, bookW, 4, "06 · Cấm Thuật: Dragon Nova", 5);
        addTocButton(left, top, bookW, 5, "07 · Đại Tuyệt Kỹ Ma Vương", 6);
        addTocButton(left, top, bookW, 6, "08 · Kỹ Năng Tiến Hóa & Kết Giới", 7);

        addTocButton(left, top, bookW, 7, "09 · Sáng Tạo Vật Chất & Danh Xưng", 8);
        addTocButton(left, top, bookW, 8, "10 · Không Gian & 5 Ấn Ký Vĩnh Viễn", 9);
        addTocButton(left, top, bookW, 9, "11 · Dị Giới Thần Môn 2.0 (Howl)", 10);
        addTocButton(left, top, bookW, 10, "12 · Thái Tuế Tinh Quân & Thị Nhục", 11);
        addTocButton(left, top, bookW, 11, "15 · Súng Hoàng Kim Carrera", 14);
        addTocButton(left, top, bookW, 12, "16 · Thất Thủy Tổ, Thể Xác & Nhập Xác", 15);
        addTocButton(left, top, bookW, 13, "30 · Kẻ Tham Lam & Cố Vấn 1-Chạm", 29);

        // Nút Tác Giả & GitHub
        int authorBtnY = top + bookH - 52;
        openGithubBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.open_github"), b -> {
            try {
                Util.getPlatform().openUri(URI.create(GITHUB_URL));
            } catch (Exception ignored) {
            }
        }).bounds(left + bookW / 2 - 135, authorBtnY, 130, 18).build();

        copyGithubBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.copy_github"), b -> {
            if (this.minecraft != null) {
                this.minecraft.keyboardHandler.setClipboard(GITHUB_URL);
                if (this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.translatable("gui.weapons.guide.copy_success"),
                            true
                    );
                }
            }
        }).bounds(left + bookW / 2 + 5, authorBtnY, 130, 18).build();

        this.addRenderableWidget(openGithubBtn);
        this.addRenderableWidget(copyGithubBtn);

        updateButtonState();
    }

    private void addTocButton(int left, int top, int bookW, int idx, String label, int targetPage) {
        int col = idx / 7;
        int row = idx % 7;
        int gap = 8;
        int colW = (bookW - 36 - gap) / 2;
        int btnX = left + 18 + col * (colW + gap);
        int btnY = top + 52 + row * 21;

        Button btn = Button.builder(Component.literal("§f" + label), b -> {
            currentPage = targetPage;
            updateButtonState();
        }).bounds(btnX, btnY, colW, 18).build();

        tocJumpButtons.add(btn);
        this.addRenderableWidget(btn);
    }

    private void updateButtonState() {
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < TOTAL_PAGES - 1;
        if (tocButton != null) tocButton.active = currentPage != PAGE_TOC;

        boolean isTocPage = (currentPage == PAGE_TOC);
        for (Button btn : tocJumpButtons) {
            btn.visible = isTocPage;
        }

        boolean isArmorPage = (currentPage == PAGE_ARMOR);
        if (armorHelmBtn != null) armorHelmBtn.visible = isArmorPage;
        if (armorChestBtn != null) armorChestBtn.visible = isArmorPage;
        if (armorLegsBtn != null) armorLegsBtn.visible = isArmorPage;
        if (armorBootsBtn != null) armorBootsBtn.visible = isArmorPage;

        boolean isAuthorPage = (currentPage == PAGE_AUTHOR);
        if (openGithubBtn != null) openGithubBtn.visible = isAuthorPage;
        if (copyGithubBtn != null) copyGithubBtn.visible = isAuthorPage;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int bookW = getBookWidth();
        int bookH = getBookHeight();
        int left = (this.width - bookW) / 2;
        int top = (this.height - bookH) / 2;
        int maxY = top + bookH - 30;
        int fullW = bookW - 32;

        hoveredTooltipStack = null;

        // Nền bìa sách & giấy cổ
        guiGraphics.fill(left - 3, top - 3, left + bookW + 3, top + bookH + 3, 0xFF140D05);
        guiGraphics.fill(left - 1, top - 1, left + bookW + 1, top + bookH + 1, 0xFFD4AF37);
        guiGraphics.fill(left, top, left + bookW, top + bookH, 0xFF1B1622);

        // Thanh tiêu đề trên
        guiGraphics.fill(left + 8, top + 6, left + bookW - 8, top + 26, 0xFF282033);
        guiGraphics.fill(left + 8, top + 25, left + bookW - 8, top + 26, 0xFFD4AF37);

        // Vẽ nút widget sau nền (tránh đè nền che chữ nút)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        switch (currentPage) {
            case 0 -> renderPageOverview(guiGraphics, left, top, bookW, maxY, fullW);
            case 1 -> renderPageToc(guiGraphics, left, top, bookW, maxY, fullW);
            case 2 -> renderPageMoonlightSword(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 3 -> renderPageDemonLord(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 4 -> renderPageDivineArmor(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 5 -> renderPageDragonNova(guiGraphics, left, top, bookW, maxY, fullW);
            case 6 -> renderPageSupremeDemonLordSkills(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 7 -> renderPageEvolutionAndBarriers(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 8 -> renderPageMaterialCreationAndNaming(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 9 -> renderPageSpatialMagic(guiGraphics, left, top, bookW, maxY, fullW);
            case 10 -> renderPageDimensionalSanctuary(guiGraphics, left, top, bookW, maxY, fullW);
            case 11 -> renderPageTaisui(guiGraphics, left, top, bookW, maxY, fullW);
            case 12 -> renderPageSeerFlesh(guiGraphics, left, top, bookW, maxY, fullW);
            case 13 -> renderPageSeerArmCombo(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 14 -> renderPageGoldenGun(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 15 -> renderPagePrimordialsOverview(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 16 -> renderPageCapsuleAndClaws(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 17 -> renderPagePossessionAndGovernance(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 18 -> renderPagePrimordialsPowerAndDomain(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 19 -> renderPageTrueDragons(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 20 -> renderPageKuboVariants(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 21 -> renderPageSoulsAndPhone(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 22 -> renderPageStellarRemote(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 23 -> renderPageCuuTheCombat(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 24 -> renderPageCuuTheSealingAndCurse(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 25 -> renderPageTaKinhAndMarbas(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 26 -> renderPageUnderworldAndDemons(guiGraphics, left, top, bookW, maxY, fullW);
            case 27 -> renderPageVeldoraSageAzathoth(guiGraphics, left, top, bookW, maxY, fullW);
            case 28 -> renderPageTempestAndLabyrinth(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 29 -> renderPageGreedAndTacticalCoPilot(guiGraphics, left, top, bookW, maxY, fullW, mouseX, mouseY);
            case 30 -> renderPageAuthor(guiGraphics, left, top, bookW, maxY, fullW);
        }

        if (hoveredTooltipStack != null && !hoveredTooltipStack.isEmpty()) {
            guiGraphics.renderTooltip(font, hoveredTooltipStack, mouseX, mouseY);
        }
    }

    // =========================================================================
    // HELPERS: CHỮ TỰ XUỐNG DÒNG, CĂN HÀNG & KHÔNG BAO GIỜ TRÀN KHUNG
    // =========================================================================

    private void drawHeaderTitle(GuiGraphics g, int left, int top, int bookW, String title, int color) {
        String pageStr = Component.translatable("gui.weapons.guide.page_indicator", currentPage + 1, TOTAL_PAGES).getString();
        int pageW = font.width(pageStr);
        g.drawString(font, "§7" + pageStr, left + bookW - pageW - 14, top + 11, 0xFFFFFFFF, false);

        int maxTitleW = Math.max(80, bookW - pageW - 36);
        String safeTitle = font.width(title) > maxTitleW
                ? font.plainSubstrByWidth(title, Math.max(20, maxTitleW - font.width("..."))) + "..."
                : title;
        g.drawString(font, safeTitle, left + 14, top + 11, color, false);
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int maxY) {
        return drawWrapped(g, text, x, y, maxWidth, maxY, LINE_HEIGHT);
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int maxY, int lineStep) {
        if (text == null || text.isEmpty() || maxWidth <= 20) return y;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), maxWidth);
        for (FormattedCharSequence line : lines) {
            if (y + 9 > maxY) break;
            g.drawString(font, line, x, y, 0xFFFFFFFF, false);
            y += lineStep;
        }
        return y;
    }

    private void drawIconRow(GuiGraphics g, int startX, int y, int mouseX, int mouseY, ItemStack... stacks) {
        for (int i = 0; i < stacks.length; i++) {
            drawSingleItemSlot(g, startX + i * 21, y, stacks[i], mouseX, mouseY);
        }
    }

    private void drawSingleItemSlot(GuiGraphics guiGraphics, int slotX, int slotY, ItemStack item, int mouseX, int mouseY) {
        guiGraphics.fill(slotX, slotY, slotX + 19, slotY + 19, 0xFF373737);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 18, slotY + 18, 0xFF8B8B8B);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF2A2A2A);

        if (item != null && !item.isEmpty()) {
            guiGraphics.renderItem(item, slotX + 2, slotY + 2);
            if (mouseX >= slotX && mouseX <= slotX + 19 && mouseY >= slotY && mouseY <= slotY + 19) {
                hoveredTooltipStack = item;
            }
        }
    }

    private void drawCraftingGrid(GuiGraphics guiGraphics, int startX, int startY, ItemStack[][] grid, ItemStack output, int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + col * 19;
                int slotY = startY + row * 19;

                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF373737);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 16, slotY + 16, 0xFF313131);

                ItemStack item = grid[row][col];
                if (item != null && !item.isEmpty()) {
                    guiGraphics.renderItem(item, slotX + 1, slotY + 1);
                    if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
                        hoveredTooltipStack = item;
                    }
                }
            }
        }

        int arrowX = startX + 3 * 19 + 4;
        int arrowY = startY + 22;
        guiGraphics.drawString(font, "§6➔", arrowX, arrowY, 0xFFFFAA00, false);

        int outX = arrowX + 14;
        int outY = startY + 16;
        guiGraphics.fill(outX, outY, outX + 26, outY + 26, 0xFFFFD700);
        guiGraphics.fill(outX + 1, outY + 1, outX + 25, outY + 25, 0xFF2A2A2A);

        if (output != null && !output.isEmpty()) {
            guiGraphics.renderItem(output, outX + 5, outY + 5);
            if (mouseX >= outX && mouseX <= outX + 26 && mouseY >= outY && mouseY <= outY + 26) {
                hoveredTooltipStack = output;
            }
        }
    }

    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y + 1, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    // =========================================================================
    // CÁC TRANG NỘI DUNG (27 TRANG ĐẦY ĐỦ)
    // =========================================================================

    private void renderPageOverview(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ KHỞI NGUYÊN THẦN THƯ · TỔNG QUAN", 0xFFFFAA00);

        int x = left + 16;
        int y = top + 34;

        y = drawWrapped(g, "§eChào mừng! §fThần thư lưu truyền toàn bộ công thức, lộ trình thức tỉnh Chân Ma Vương, Thất Đại Thủy Tổ, Song Long, Không Gian & Boss Dark Gathering.", x, y, fullW, maxY);
        y += 5;

        y = drawWrapped(g, "§b⚡ Hệ Thống Phím Tắt & Đặc Quyền Mới:", x, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §e• [G] Bánh Xe / [Z] Đổi Chiêu / [Y]/[N] Cố Vấn: §fChọn nhanh chiêu; [Y]/[N] nhận lệnh chiến thuật 1-chạm Đại Hiền Giả. §a(0s Cooldown!)", x + 2, y, fullW - 4, maxY);
        y = drawWrapped(g, " §b• [B] Dạ Dày Azathoth / [X] Tự Động Raphael: §f[B] mở Không Gian Ảo (120 ô, click túi cất đồ); [X] bật/tắt Raphael Auto-Battle!", x + 2, y, fullW - 4, maxY);
        y = drawWrapped(g, " §3• [V] Hang Phong Ấn / [K] Nghịch Thời Trảm: §f[V] dịch chuyển tới Hang Veldora (Shift+V quét 24m); [K] chém Nghịch Thời.", x + 2, y, fullW - 4, maxY);
        y = drawWrapped(g, " §a• [Chuột Phải] Thi Triển / [Page Up] Đạn Súng: §fTung kỹ năng đang chọn (tay không / Giáp Thần / Nhập Xác).", x + 2, y, fullW - 4, maxY);
        y = drawWrapped(g, " §7• HUD Ma Tố (MP/EP) & Mini-HUD: §fTheo dõi Ma Tố tái sinh liên tục, Chỉ số EP, Safe Mode & 5 Neo Không Gian.", x + 2, y, fullW - 4, maxY);
        y += 4;
        drawWrapped(g, "§6▸ Bấm nút [✦ Mục Lục] ở dưới hoặc [Trang Sau ▶] để tra cứu chi tiết từng mục!", x, y, fullW, maxY);
    }

    private void renderPageToc(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§e§l✦ MỤC LỤC TRA CỨU NHANH", 0xFFFFDD00);
        int y = drawWrapped(g, "§7Nhấp vào bất kỳ chương nào bên dưới để nhảy thẳng tới trang hướng dẫn:", left + 16, top + 34, fullW, maxY);
        drawWrapped(g, "§8(Mỗi chương lớn gồm 1 đến 3 trang, dùng [◀ Trang Trước] / [Trang Sau ▶] để lật tiếp)", left + 16, y, fullW, maxY);
    }

    private void renderPageMoonlightSword(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ NGUYỆT QUANG THẦN TẾ KIẾM", 0xFFFFAA00);

        drawCraftingGrid(g, left + 16, top + 34, moonlightSwordRecipe, new ItemStack(ModItems.MOONLIGHT_SWORD.get()), mouseX, mouseY);

        int descX = left + 132;
        int descW = bookW - 148;
        int dy = top + 34;
        dy = drawWrapped(g, "§6§lNguyệt Quang Thần Tế Kiếm", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§7Bảo kiếm hộ vệ thánh điện, mang sức mạnh thánh quang và hắc thiểm.", descX, dy, descW, maxY);
        dy = drawWrapped(g, "§a✔ Độ bền vĩnh cửu, không thể phá hủy.", descX, dy, descW, maxY);
        drawWrapped(g, "§b▸ Dùng [Z] hoặc [G] để xoay nhanh kho chiêu thức.", descX, dy, descW, maxY);

        int y = top + 100;
        y = drawWrapped(g, "§e⚡ Kỹ Năng Nguyệt Quang Kiếm & Bản Thể:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f1. §4Trảm Kích Hắc Thiểm Bá Vương: §7Đánh cận chiến [Chuột Trái] kích nổ sét đen.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f2. §bLinh Tử Băng Hoại (Disintegration): §7[Chuột Phải] phóng cầu 7 màu tầm xa 50 blocks, gây sát thương xuyên giáp.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f3. §eKho Chiêu Thức Đa Hệ ([Z] / [G]): §7Bao gồm Tà Khứ Vũ Thê Tử (Jacob's Ladder), Phép Màu Thần Linh, Bí Ẩn Đức Tin & Ân Sủng, Bạo Thực Vương, Thị Thực, Nghịch Thời Nhất Trảm, Zone Track, Đa Trùng Kết Giới...", left + 16, y, fullW, maxY);
        drawWrapped(g, " §64. §dKết Hợp Chân Ma Vương: §7Tăng uy lực toàn diện các chiêu thánh ma.", left + 16, y, fullW, maxY);
    }

    private void renderPageDemonLord(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§d§l✦ THỨC TỈNH CHÂN MA VƯƠNG", 0xFFFF55FF);

        drawCraftingGrid(g, left + 16, top + 34, demonLordSeedRecipe, new ItemStack(ModItems.DEMON_LORD_SEED.get()), mouseX, mouseY);

        int descX = left + 132;
        int descW = bookW - 148;
        int dy = top + 34;
        dy = drawWrapped(g, "§d§lHạt Giống Ma Vương §7& §6Ngọc Thức Tỉnh", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§a• Sinh tồn: §fĐạt §e10+ EXP§f, tiêu diệt quái để thu thập §d10.000 Linh Hồn§f, sau đó [Chuột Phải] Hạt Giống kích hoạt Lễ Hội Thu Hoạch.", descX, dy, descW, maxY);
        dy = drawWrapped(g, "§e• Đốt giai đoạn: §fDùng §dNgọc Thức Tỉnh Ma Vương§f để thăng cấp trực tiếp kèm tối đa 3 kỹ năng tiến hóa.", descX, dy, descW, maxY);

        int iconY = top + 98;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.DEMON_LORD_SEED.get()),
                new ItemStack(ModItems.DEMON_LORD_AWAKENING_ORB.get()),
                new ItemStack(ModItems.DEMON_LORD_SOUL.get()));
        drawWrapped(g, "§7Hạt Giống · Ngọc Thức Tỉnh · Linh Hồn Ma Vương", left + 82, iconY + 5, bookW - 96, maxY);

        int y = top + 123;
        y = drawWrapped(g, "§d⚡ Đặc Quyền Sau Khi Tiến Hóa Chân Ma Vương:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f• §a40 Tim (80 HP)§f, Speed I & Resistance I vĩnh viễn + §bQuyền Bay Lượn§f & §b0s Hồi Chiêu§f.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §eBảo Toàn & Ma Tố: §fTự khôi phục bay/máu khi hồi sinh; Ma Tố & EP tăng mạnh (+5M MP) theo thức tỉnh.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §5Kho 30+ Tuyệt Kỹ (§b[Z] / [G]§5): §fTay không tung Azathoth, Long Giam Vô Hạn, Dị Giới Thần Môn & Sáng Tạo Vật Chất.", left + 16, y, fullW, maxY);
    }

    private void renderPageDivineArmor(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ THẦN LINH VŨ TRANG (DIVINE)", 0xFFFFAA00);

        ItemStack outputArmor = switch (selectedArmorPiece) {
            case 0 -> new ItemStack(ModItems.DIVINE_HELMET.get());
            case 1 -> new ItemStack(ModItems.DIVINE_CHESTPLATE.get());
            case 2 -> new ItemStack(ModItems.DIVINE_LEGGINGS.get());
            default -> new ItemStack(ModItems.DIVINE_BOOTS.get());
        };

        String armorPieceName = switch (selectedArmorPiece) {
            case 0 -> Component.translatable("gui.weapons.guide.p4.piece_helm").getString();
            case 1 -> Component.translatable("gui.weapons.guide.p4.piece_chest").getString();
            case 2 -> Component.translatable("gui.weapons.guide.p4.piece_legs").getString();
            default -> Component.translatable("gui.weapons.guide.p4.piece_boots").getString();
        };

        drawCraftingGrid(g, left + 16, top + 34, divineArmorRecipes[selectedArmorPiece], outputArmor, mouseX, mouseY);

        int descX = left + 132;
        int descW = bookW - 148;
        int dy = top + 34;
        dy = drawWrapped(g, "§e§l" + armorPieceName, descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§7Nâng cấp từ Giáp Netherite tương ứng cùng Vàng khối, Kim cương & Mắt Ender.", descX, dy, descW, maxY);
        drawWrapped(g, "§a✔ Độ bền bất tử, chọn 4 nút bên dưới để xem công thức:", descX, dy, descW, maxY);

        int y = top + 102;
        y = drawWrapped(g, "§6⚡ Phòng Ngự Tuyệt Đối Của Uriel:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §a✔ Mặc định [BẬT]: §fMiễn nhiễm 100% sát thương vật lý và phép thuật thông thường.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §e✔ Chủ Động Bật/Tắt: §fDùng chiêu Uriel trên thanh kỹ năng ([Z] / [G]) để bật tắt.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §d★ Chân Ma Vương: §fSở hữu nội tại Uriel vĩnh viễn kể cả khi đã tháo giáp.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §c✘ Điểm Yếu Hiếm: §fĐòn xuyên thấu linh tử, hư vô hoặc Long Chủng cấp cao vẫn gây xuyên giáp.", left + 16, y, fullW, maxY);
    }

    private void renderPageDragonNova(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§5§l✦ CẤM THUẬT: LONG TINH BỘC VIÊM BÁ", 0xFFFF55FF);

        int imgX = left + 16;
        int imgY = top + 34;
        int imgW = 118;
        int imgH = 86;

        g.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFFD4AF37);
        g.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        g.blit(DRAGON_NOVA_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        int descX = imgX + imgW + 10;
        int descW = bookW - (descX - left) - 14;
        int dy = top + 34;
        dy = drawWrapped(g, "§5§lLong Tinh Bộc Viêm Bá §d(Dragon Nova)", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§7Cấm thuật diệt rồng tối cổ từng thanh tẩy Tinh Linh Long hóa điên, bốc hơi vạn vật thành bình địa.", descX, dy, descW, maxY);
        drawWrapped(g, "§e▸ Điều kiện: §fThức Tỉnh Chân Ma Vương + đủ 4 món Giáp Thần Linh (hoặc tắt Safe Mode nếu muốn phá địa hình).", descX, dy, descW, maxY);

        int y = top + 128;
        y = drawWrapped(g, "§6⚡ Uy Lực Hủy Diệt & Cơ Chế:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f• §bLinh Tử Hội Tụ: §fHút sạch linh tử tinh khiết từ không gian, phóng chùm tia cực đại.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §dSát Thương Khủng Khiếp: §fTạo hố nổ siêu tân tinh, rút thẳng §c88% HP Đại Boss§f & khắc chế tử huyệt Velgrynd!", left + 16, y, fullW, maxY);
        drawWrapped(g, " §f• §aHỗ trợ Safe Mode: §fKhi bật Safe Mode trong Bánh Xe [G], chùm tia giữ sát thương nhưng §ekhông phá block§f.", left + 16, y, fullW, maxY);
    }

    private void renderPageSupremeDemonLordSkills(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§4§l✦ ĐẠI TUYỆT KỸ MA VƯƠNG TỐI THƯỢNG", 0xFFFF5555);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModBlocks.BLACK_FIRE.get()),
                new ItemStack(ModItems.FAKE_SOUL.get()),
                new ItemStack(ModItems.DEMON_LORD_SOUL.get()));
        drawWrapped(g, "§6[Hắc Viêm Ngục · Ngày Tận Thế · Azathoth · Lời Chúc Tử Vong · Siêu Tân Tinh]", left + 82, iconY + 5, bookW - 96, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §4• Ngày Tận Thế (Apocalypse): §fMở Đại Ma Trận bán kính 64m, rút hồn & tiêu diệt kẻ thù; tự cấy §aLinh Hồn Giả§f để rút hồn sinh vật hiền lành mà §akhông làm chết chúng§f!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §c• Hắc Viêm Ngục (Hellflare): §fKhóa mục tiêu trong khối không gian rồi bùng nổ Lửa Đen bỏ qua kháng lửa; bế mạc nổ nén 70 DMG.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §5• Thần Hư Không Azathoth: §fTriệu hồi Cự Long Plasma dài 50m xé không gian và nuốt trọn địa hình/sinh vật vào hư vô.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §4• Lời Chúc Tử Vong (Death Blessing): §fBay cao +28m, nghịch chuyển trọng lực hút quái 70m và giáng Cột Sáng Phán Quyết tất sát.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §6• Siêu Tân Tinh (Supernova): §fTạo hố đen ở độ cao +20m hút xoáy 55m, nổ sóng chấn động 96m (2,500 DMG) & dựng Cột Sáng Vực Thẳm 384m.", left + 16, y, fullW, maxY);
    }

    private void renderPageEvolutionAndBarriers(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§b§l✦ KỸ NĂNG TIẾN HÓA & KẾT GIỚI KHÁNG MA", 0xFF55FFFF);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModBlocks.DRAGON_PRISON_BARRIER.get()),
                new ItemStack(ModBlocks.ANTI_MAGIC_BARRIER.get()),
                new ItemStack(ModBlocks.MULTILAYER_BARRIER.get()));
        drawWrapped(g, "§e[Long Giam Vô Hạn · Kháng Ma Kết Giới · Đa Trùng Kết Giới]", left + 82, iconY + 5, bookW - 96, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §6• Lồng Giam Vô Hạn: §fDựng mái vòm địa cầu hoàng kim bán kính tới 20m giam vĩnh viễn Boss, Ác Ma, Không Vong (Milim/Velgrynd 3 phút). Khi bị nhốt, quái hóa điên cắn xé nhau. §eĐấm tay không vào khối lồng để giải trừ.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §b• Cảm Nhận Vạn Năng 8x: §fQuét tâm thức cực xa tới 384m xuyên đất tường, ưu tiên phát sáng mục tiêu thù địch kể cả Long Chủng.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §a• Kháng Ma Kết Giới (Bán kính 60m): §fChặn 100% Boss/Quái, §achặn mưa bão & sương lạnh (bảo hộ thời tiết)§f, làm tan băng tuyết, §dthanh tẩy Dân Làng Zombie & hồi sinh Làng hoang trù phú§f!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §b• Đa Trùng Kết Giới & Tiên Tri Rykuzin Sinka: §fGiáp ma lực đa tầng & mắt Tiên Tri phá giải bùa chú.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §c• Phong Sát Kỹ Năng (Skill Nullification): §fKhóa cứng năng lực của kẻ địch trong khu vực.", left + 16, y, fullW, maxY);
    }

    private void renderPageMaterialCreationAndNaming(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ SÁNG TẠO VẬT CHẤT & BAN DANH XƯNG", 0xFFFFDD00);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.ABYSSAL_CLAW_DESPAIR.get()),
                new ItemStack(ModItems.VOID_CLAW_CALAMITY.get()),
                new ItemStack(ModItems.GOLDEN_GUN.get()),
                new ItemStack(ModItems.MILIM_DRAGON_CONTRACT.get()));
        drawWrapped(g, "§e[Ngưng Tụ Thần Khí · Súng · Đạn · Giao Ước]", left + 104, iconY + 5, bookW - 118, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §61. Sáng Tạo Vật Chất (Chiêu 15 Chân Ma Vương):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fMở giao diện ngưng tụ trực tiếp §5Vuốt Hắc Ám Tuyệt Vọng§f, §dVuốt Hư Vô Tai Ương§f, §6Súng Lục Hoàng Kim§f và cả 4 loại §eMa Đạn Carrera§f (tiêu hao một phần HP thay nguyên liệu).", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §b2. Ban Danh Xưng (Naming Ability - Chiêu 29):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fĐặt tên ban sức mạnh cho đồng minh/sinh vật để nâng tầm chỉ số, cấp giáp và tiến hóa trung thành theo chuẩn Tensura.", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §d3. Giao Ước Milim Nava & Bàn Tay Rồng:", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §fDùng Khế Ước Rồng để giao tiếp/gọi hỗ trợ từ Ma Vương Milim Nava; dùng lệnh §e/skillvisual§f để bật tắt cutscene 3D nếu máy yếu.", left + 16, y, fullW, maxY);
    }

    private void renderPageSpatialMagic(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§3§l✦ KHÔNG GIAN & 5 ẤN KÝ VĨNH VIỄN", 0xFF00E5FF);

        int y = top + 34;
        y = drawWrapped(g, "§bChiêu 10: Đánh Dấu (Spatial Mark)  |  Chiêu 11: Dịch Chuyển (Spatial Teleportation)", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §f• §eĐánh Dấu Chiến Thuật (Tạm Thời): §f[Chuột Phải] tạo ấn ký dùng 1 lần. Đập vỡ nếu muốn hủy (không chiếu cutscene).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §65 Ấn Ký Vĩnh Viễn (#1 -> #5): §fMở §bBánh Xe [G]§f để chuyển ô #1..#5, đặt tối đa 5 mốc cố định xuyên chiều (Overworld / Nether / End). Tái sử dụng mãi mãi không mất!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §aDịch Chuyển Toàn Diện: §fĐưa bản thân cùng toàn bộ §esinh vật, vật phẩm rơi, thuyền/xe, khung tên...§f đứng quanh bạn sang ấn ký đích.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §dGửi Vật & Sinh Vật Từ Xa + Ma Pháp Trận: §fChọn ô đích #1..#5 ở thanh dưới cùng Bánh Xe [G], ngắm vào vật/quái rồi bấm [Gửi Tới Ấn Ký]: mở ma pháp trận dịch chuyển chúng đi ngay (không khóa camera).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §bCombat Blink (Né Tức Thời): §fNút bấm nhanh trong Bánh Xe [G] giúp lướt không gian 12m theo hướng nhìn.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §f• §7Đặt ấn dưới chân cố định sẽ dựng Đại Ma Pháp Trận đếm ngược §e4-3-2-1§7 trước khi dịch chuyển tập thể.", left + 16, y, fullW, maxY);
    }

    private void renderPageDimensionalSanctuary(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ DỊ GIỚI THẦN MÔN 3.0 (THÀNH TRÌ JURA TEMPEST)", 0xFFFFD54F);

        int y = top + 34;
        y = drawWrapped(g, "§6Chiêu 30 Chân Ma Vương: §eDị Giới Thần Môn -> Chiều Không Gian Riêng Jura Tempest", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f1. §6Thành Trì Đá Trắng & Vàng Hoàng Kim: §fBước qua Cổng §e[0]§f lần đầu sẽ kiến tạo trọn bộ Cung Điện Ngai Vàng, Bàn Tròn Hội Đồng Thủy Tổ, Kho Hoàng Gia (tự gom 100% đồ từ rương cũ), Khu Onsen (hồi 3x Ma Tố) & Đấu Trường Vô Cực.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f2. §aCư Dân Nhộn Nhịp & Bảng Điều Khiển: §fTự có sẵn 5 Thợ Rèn/Vu Nữ, 4 Thủy Tổ Ác Ma & 3 Tinh Linh; nhấn §dChuột Phải Bệ Quản Trị§f (hoặc nút §6♛ Thành Trì§f trong [B]) để Mời Đồng Minh, Ban Danh Xưng (+MP/EP), đặt Bia Thử DMG & gọi 8 Boss!", left + 16, y, fullW, maxY);
        drawWrapped(g, " §f3. §bLa Bàn Thần Môn [0..9]: §f[Chuột Phải] cửa để xoay kênh màu; [Shift+Chuột Phải] mở GUI lưu 9 tọa độ xuyên chiều.", left + 16, y, fullW, maxY);
    }

    private void renderPageTaisui(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§e§l✦ THÁI TUẾ TINH QUÂN (TAISAI SEIKUN)", 0xFFFFCC00);

        int imgX = left + 16;
        int imgY = top + 34;
        int imgW = 118;
        int imgH = 86;

        g.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFFE5C158);
        g.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        g.blit(TAISUI_ART_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        int descX = imgX + imgW + 10;
        int descW = bookW - (descX - left) - 14;
        int dy = top + 34;
        dy = drawWrapped(g, "§e§lThái Tuế Tinh Quân §6(Dark Gathering)", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§7Vị thần thống trị các vì tinh tú, mang quyền năng Tuyệt Diệt Tinh Tú.", descX, dy, descW, maxY);
        dy = drawWrapped(g, "§d• Ngôi Sao Tuyệt Diệt: §f[Chuột Phải] bắn chùm sao tất sát 1-Hit & phá block (hoặc dùng trong Safe Mode).", descX, dy, descW, maxY);
        drawWrapped(g, "§b• Cộng Hưởng Song Tuyệt: §fKích hoạt cùng Thị Nhục để hợp thể Ngôi Sao Tuyệt Diệt & Thị Thực Bọc Tay.", descX, dy, descW, maxY);

        int y = top + 128;
        y = drawWrapped(g, "§6⚡ Quy Tắc Vận Hành:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f• §e5 Ngôi Sao Tối Đa: §fBắn hết 5 sao sẽ tự động kết thúc trạng thái cộng hưởng.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §aThần Tính Hộ Thể: §fKháng sát thương & không tốn thời gian hồi chiêu (Zero Cooldown).", left + 16, y, fullW, maxY);
        drawWrapped(g, " §f• §cĐiều Kiện: §fThức Tỉnh Chân Ma Vương (chuyển chiêu bằng [Z] hoặc [G]).", left + 16, y, fullW, maxY);
    }

    private void renderPageSeerFlesh(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§c§l✦ THÁI TUẾ TINH QUÂN: THỊ NHỤC", 0xFFFF4444);

        int imgX = left + 16;
        int imgY = top + 34;
        int imgW = 118;
        int imgH = 86;

        g.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFFCC0022);
        g.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        g.blit(SEER_FLESH_ART_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        int descX = imgX + imgW + 10;
        int descW = bookW - (descX - left) - 14;
        int dy = top + 34;
        dy = drawWrapped(g, "§c§lThị Thực (Seer Flesh)", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§7Khối thịt u linh bất tử 12 mắt (tan rã nếu đi xa quá 5m).", descX, dy, descW, maxY);
        dy = drawWrapped(g, "§a• [Chuột Phải]: §fNuốt 1 mắt hồi 100% HP & xóa mọi độc tố.", descX, dy, descW, maxY);
        drawWrapped(g, "§c• [Chuột Trái]: §fRạch lấy [Con Mắt Thị Nhục] bỏ túi mang theo.", descX, dy, descW, maxY);

        int y = top + 128;
        y = drawWrapped(g, "§6⚡ Công Dụng Thần Dị Của Con Mắt Thị Nhục:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f• §aTự Cứu Khẩn Cấp: §fNuốt mắt để lập tức đầy 100% máu, đầy thanh no & tẩy sạch Khô héo, Độc, Lửa, Bóng tối.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §f• §eCứu Rỗi Đồng Đội & Dân Làng: §fCầm Con Mắt [Chuột Phải] vào đồng minh/dân làng để hồi 100% máu kèm Hồi Sinh & Khiên Máu II.", left + 16, y, fullW, maxY);
    }

    private void renderPageSeerArmCombo(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§2§l✦ CỘNG HƯỞNG: THỊ NHỤC & TINH TÚ", 0xFFFFAA00);

        int imgX = left + 16;
        int imgY = top + 34;
        int imgW = 122;
        int imgH = 88;

        g.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFF2E5A36);
        g.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        g.blit(SEER_ARM_COMBO_ART_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        int descX = imgX + imgW + 10;
        int descW = bookW - (descX - left) - 14;
        int dy = top + 34;
        dy = drawWrapped(g, "§2§lThị Thực Bọc Tay §f& §eNgôi Sao Tuyệt Diệt", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§7Hình thái song tuyệt tối thượng khi bật đồng thời Thị Nhục và Tuyệt Diệt Tinh Tú.", descX, dy, descW, maxY);
        drawSingleItemSlot(g, descX, dy + 2, new ItemStack(ModItems.SEER_FLESH_ARM.get()), mouseX, mouseY);
        drawSingleItemSlot(g, descX + 24, dy + 2, new ItemStack(ModItems.EXTINCTION_STAR.get()), mouseX, mouseY);
        drawWrapped(g, "§eCặp Thần Khí Cộng Hưởng", descX + 48, dy + 7, descW - 50, maxY);

        int y = top + 128;
        y = drawWrapped(g, "§6⚡ Uy Lực Song Tuyệt:", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §f• §aThị Nhục (Tay Trái): §fMang 3 mắt ngọc bích, [Chuột Phải] hồi 100% HP bản thân hoặc đồng đội/dân làng (mắt đóng 3 -> 0).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §eTinh Tú (Tay Phải): §fTự ngưng tụ khi tay trống, [Chuột Trái/Phải] phóng tia tất sát & phá block.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §f• §dThần Tính Hộ Thể: §fQuái áp sát bị phản dội 50% HP; kéo dài tối đa 3 phút hoặc 5 sao.", left + 16, y, fullW, maxY);
    }

    private void renderPageGoldenGun(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ SÚNG LỤC HOÀNG KIM & 4 MA ĐẠN", 0xFFFFAA00);

        int artX = left + 16;
        int artY = top + 34;
        int artSize = 86;
        g.fill(artX - 1, artY - 1, artX + artSize + 1, artY + artSize + 1, 0xFFD4AF37);
        g.blit(CARRERA_ART_TEXTURE, artX, artY, 0, 0, artSize, artSize, artSize, artSize);

        drawIconRow(g, artX, artY + artSize + 6, mouseX, mouseY,
                new ItemStack(ModItems.GOLDEN_GUN.get()),
                new ItemStack(ModItems.CARRERA_BULLET_JUDGEMENT.get()),
                new ItemStack(ModItems.CARRERA_BULLET_ABYSS.get()),
                new ItemStack(ModItems.CARRERA_BULLET_GRAVITY.get()));
        drawSingleItemSlot(g, artX + 84, artY + artSize + 6, new ItemStack(ModItems.CARRERA_BULLET_RAPID.get()), mouseX, mouseY);

        int descX = artX + artSize + 12;
        int descW = bookW - (descX - left) - 14;
        int dy = top + 34;
        dy = drawWrapped(g, "§e§lHoàng Sắc Thủy Tổ Carrera §7(Abaddon)", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, "§a▸ Cầm súng nhấn [Page Up]: §fMở ổ xoay chọn 4 loại đạn (cần ít nhất 1 viên trong túi đồ; chế từ [Sáng Tạo Vật Chất]).", descX, dy, descW, maxY);
        dy += 2;
        dy = drawWrapped(g, " §e1. Thần Tốc Phán Quyết: §cPhá lõi linh hồn, BẮN 1 PHÁT TẤT SÁT + Giọng Nói Thế Giới thông báo.", descX, dy, descW, maxY);
        dy = drawWrapped(g, " §d2. Hạch Thâm Uyên: §fHố đen trọng lực nuốt chửng & kích nổ lớn.", descX, dy, descW, maxY);
        dy = drawWrapped(g, " §93. Trọng Lực Sụp Đổ: §fÉp dẹt và khóa cứng chuyển động mục tiêu.", descX, dy, descW, maxY);
        drawWrapped(g, " §64. Hoàng Kim Xạ Kích: §fBắn liên thanh tốc độ cao xuyên giáp.", descX, dy, descW, maxY);
    }

    private void renderPagePrimordialsOverview(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§c§l✦ THẤT ĐẠI THỦY TỔ: KHẾ ƯỚC & CHUYỂN SINH", 0xFFFFAA00);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.PRIMORDIAL_GRIMOIRE.get()),
                new ItemStack(ModItems.PRIMORDIAL_REBIRTH_TOME.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_NOIR.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_ROUGE.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_BLANC.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_JAUNE.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_VIOLET.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_BLEU.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_VERT.get()));
        drawWrapped(g, "§ePháp Điển · Sách Cổ Thủy Tổ · 7 Cuộn Khế Ước", left + 208, iconY + 5, bookW - 222, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §61. Triệu Hồi & Thu Phục: §fDùng §cPháp Điển Khế Ước§f triệu hồi thử thách 1 trong 7 Thủy Tổ (§cRouge§f, §7Noir§f, §fBlanc§f, §eJaune§f, §5Violet§f, §9Bleu§f, §aVert§f). Đánh bại họ để nhận Cuộn Khế Ước tương ứng.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §a2. Điều Khiển Đồng Hành: §f[Chuột Phải] Khế Ước để triệu hồi hoặc cho lui đi. §d[Shift + Chuột Phải]§f vào Khế Ước (hoặc vào Ác Ma) để mở bảng lệnh & đổi chế độ.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §53. Chuyển Sinh Thành Thủy Tổ (Sách Cổ Khởi Nguyên Thủy Tổ): §f[Chuột Phải] vào §dPrimordial Rebirth Tome§f để tự mình chuyển sinh mang hình hài & bộ kỹ năng độc bản của 1 trong 7 Thủy Tổ!", left + 16, y, fullW, maxY);
        drawWrapped(g, " §7• Chọn Noir nhận kèm §5Móng Vuốt Huyền Thoại§7; chọn Jaune nhận §6Súng Lục Hoàng Kim§7 khi hóa Ma Vương.", left + 16, y, fullW, maxY);
    }

    private void renderPageCapsuleAndClaws(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§5§l✦ BAN THỂ XÁC & MÓNG VUỐT HUYỀN THOẠI", 0xFFFF77FF);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(net.minecraft.world.item.Items.BONE),
                new ItemStack(ModItems.ARTIFICIAL_SKELETON.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_NOIR_AWAKENED.get()),
                new ItemStack(ModItems.ABYSSAL_CLAW_DESPAIR.get()),
                new ItemStack(ModItems.VOID_CLAW_CALAMITY.get()));
        drawWrapped(g, "§e[Xương · Khung Xương · Khế Ước Tiến Hóa · 2 Vũ Khí Vuốt]", left + 124, iconY + 5, bookW - 138, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §b1. Ban Thể Xác Vật Lý Trực Tiếp (Không cần Bồn Ấp):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fCầm §eXương§f (hoặc §7Khung Xương Ma Thiết§f) §e[Chuột Phải] §fvào Ác Ma đã thuần phục để ban ngay §eThể Xác Vật Lý§f! Ác ma hoang dã cũng có thể hấp thụ Dân Làng hoặc xác mob chết.", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §52. Móng Vuốt Huyền Thoại (Thủy Tổ Noir / Sáng Tạo Vật Chất):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §dVuốt Hắc Ám Tuyệt Vọng §f& §5Vuốt Hư Vô Tai Ương§f sở hữu 3 chiêu tối thượng (§e[Shift + Chuột Phải]§f hoặc §e[Z]§f để đổi):", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §f• [1] Tử Hắc Ma Trụ Vực Thẳm  • [2] Hư Không Trảo Hồn  • [3] Hắc Hạch Bạo Diệt (Nổ khu vực).", left + 16, y, fullW, maxY);
    }

    private void renderPagePossessionAndGovernance(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§d§l✦ NHẬP XÁC ÁC MA & TRỊ THẾ THÂM UYÊN", 0xFFFF55FF);

        int y = top + 34;
        y = drawWrapped(g, "§6Bảng Điều Khiển Khế Ước [DemonCommandScreen] ([Shift + Chuột Phải] vào Khế Ước):", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §d• Nhập Xác Thủy Tổ (Possession Mode): §fBiến đổi hoàn toàn thành chính Ác Ma đã ký khế ước! Điều khiển trực tiếp di chuyển, bay, vung chiêu thức riêng bằng §e[Z]§f / §e[G]§f và đồng bộ HP chuẩn xác.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §b• Quản Gia Hư Không (Auto-Hoover): §fTrong tab Kho Căn Cứ, liên kết trực tiếp Khế Ước vào một chiếc Rương; khi túi đồ đầy, Ác Ma tự thu gom vật phẩm rơi trong 14m gửi thẳng vào rương căn cứ!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• Chế Độ Blanc (Testarossa): §fChuyển linh hoạt giữa §aBảo Hộ Làng & Dân Làng§f hoặc §cTàn Sát (Massacre)§f quét sạch hiểm họa.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §c• Giao Ước Kiêu Hãnh Guy Crimson & Dịch Chuyển Thứ Nguyên: §fTriệu hồi tiếp viện tức thời, bảo hộ đồng minh miễn sát thương lẫn nhau.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §a• Tương Tác Dân Làng: §fGiữ [Shift] để giao dịch bình thường; tấn công khi không giữ Shift vẫn gây sát thương chuẩn.", left + 16, y, fullW, maxY);
    }

    private void renderPagePrimordialsPowerAndDomain(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ SỨC MẠNH & LÃNH ĐỊA 7 THỦY TỔ", 0xFFFFAA00);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModBlocks.DOMAIN_BARRIER_NOIR.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_ROUGE.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_BLANC.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_JAUNE.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_VIOLET.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_BLEU.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_VERT.get()));
        drawWrapped(g, "§b7 Khối Kết Giới Lãnh Địa (+50% DMG & giam 16m)", left + 166, iconY + 5, bookW - 180, maxY);

        int y = top + 56;
        y = drawWrapped(g, "§e⚡ Xếp Hạng Chiến Đấu Cơ Bản:", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §c• Rouge & Noir: §e90% Diệt Boss §7(3600 HP | 160 DMG - Khắc tinh hủy diệt mọi mặt trận).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• Blanc (Testarossa): §a75% Diệt Boss §7(1800 HP | 80 DMG - Bạch Viêm Thánh Ma).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §e• Jaune (Carrera): §a65% Diệt Boss §7(1500 HP | 70 DMG - Hạt nhân & Súng Hoàng Kim).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §5• Violet (950 HP · 33%) §7| §aVert (900 HP · 30%) §7| §9Bleu (800 HP · 20%).", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, "§d⚡ Bành Trướng Lãnh Địa Thực Tế:", left + 16, y, fullW, maxY);
        drawWrapped(g, " §fKhi Ác Ma kích hoạt Tuyệt Kỹ 4 sẽ dựng vòm kết giới 16m: Ác Ma nhận +50% sát thương & tốc độ, kẻ địch bị giật ngược vào tâm. Sau 15 giây, kết giới §atự hoàn trả nguyên vẹn địa hình cũ§f.", left + 16, y, fullW, maxY);
    }

    private void renderPageTrueDragons(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§b§l✦ SONG LONG TỐI THƯỢNG: VELGRYND & VELZARD", 0xFF80DEEA);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.DRAGON_SEED.get()),
                new ItemStack(ModItems.SCORCH_DRAGON_CORE.get()),
                new ItemStack(ModItems.VELGRYND_REVERSE_SCALE.get()),
                new ItemStack(ModItems.VELGRYND_FEATHER_FAN.get()),
                new ItemStack(ModItems.FROST_DRAGON_CORE.get()),
                new ItemStack(ModItems.VELZARD_REVERSE_SCALE.get()),
                new ItemStack(ModItems.ETERNAL_FROST_CRYSTAL.get()));
        drawWrapped(g, "§eHạt Giống · Lõi Rồng · Nghịch Lân · Tinh Thể", left + 166, iconY + 5, bookW - 180, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §c1. Chước Nhiệt Long Velgrynd (Đệ Tam Long - 74,350,000 EP):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fDùng §eHạt Giống Long Chủng§f (tốn 90% HP thường / 30% HP Ma Vương) hoặc Lõi Chước Long triệu hồi. Có 4 tầng vảy miễn sát thương thường, đòn 3 xuyên 30% HP Giáp Thần. §4Tử huyệt: Dragon Nova!§f Hạ gục rơi §aNghịch Lân§f gọi Velgrynd Đồng Minh.", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §b2. Bạch Băng Long Velzard (Đệ Nhị Long - Eternal Frost):", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §fDùng §bLõi Băng Long (Frost Dragon Core)§f để triệu hồi Velzard hạ phàm với vầng hào quang băng giá tối thượng. Hạ gục rơi §bNghịch Lân Velzard§f, §9Linh Hồn Velzard§f và §3Tinh Thể Vĩnh Đông§f.", left + 16, y, fullW, maxY);
    }

    private void renderPageKuboVariants(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§5§l✦ KHÔNG VONG, MA VƯƠNG & VÔ NGUYỆT MINH PHÔI", 0xFFCC77FF);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.KUBO_SUMMON_CORE.get()),
                new ItemStack(ModItems.KUBO_SPAWN_EGG.get()),
                new ItemStack(ModItems.DEMON_LORD_KUBO_SPAWN_EGG.get()),
                new ItemStack(ModItems.VO_NGUYET_MINH_PHOI_SPAWN_EGG.get()));
        drawWrapped(g, "§d[Lõi Triệu Hồi Không Vong & 3 Thể Biến Hình]", left + 104, iconY + 5, bookW - 118, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §71. Không Vong (Kubo - Oán Linh Nuốt Hồn):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fChuyên hấp thụ linh hồn xung quanh để tiến hóa (Thường -> Ultimate -> Hoàn Thiện). §aBị Kháng Ma Kết Giới chặn đứng 100%!", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §d2. Ma Vương Không Vong (Cosmic Deity Kubo):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fTiến hóa tối thượng khi nuốt đủ 64 Linh Hồn (520 HP, 65 DMG, 20 Giáp). Khối cầu vũ trụ xúc tu, miễn lửa & dung nham, bắn Nhật Thực Hư Vô và Xung Lực Tinh Vân.", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §53. Vô Nguyệt Minh Phôi (Biến Thể Cầu Phôi Thai):", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §fDạng cầu dải ngân hà xoáy sâu với 16 ăng-ten bức xạ, kế thừa toàn bộ sức mạnh nuốt hồn & thăng cấp của Không Vong.", left + 16, y, fullW, maxY);
    }

    private void renderPageSoulsAndPhone(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§e§l✦ LINH HỒN THỰC THỂ, HỒN GIẢ & MA THOẠI", 0xFFFFDD00);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.FAKE_SOUL.get()),
                new ItemStack(ModItems.MOB_SOUL.get()),
                new ItemStack(ModItems.WARDEN_SOUL.get()),
                new ItemStack(ModItems.WITHER_SOUL.get()),
                new ItemStack(ModItems.ENDER_DRAGON_SOUL.get()),
                new ItemStack(ModItems.PRIMORDIAL_DEMON_SOUL.get()),
                new ItemStack(ModItems.VELGRYND_SOUL.get()),
                new ItemStack(ModItems.MILIM_SOUL.get()),
                new ItemStack(ModItems.MAGISTEEL_PHONE.get()));

        int y = top + 56;
        y = drawWrapped(g, " §d1. Linh Hồn Thực Thể (7 Cấp) & Linh Hồn Giả (Fake Soul):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §eLinh Hồn Giả: §f[Chuột Phải] vào bất kỳ sinh vật nào để tráo Linh Hồn Giả vào cơ thể họ và rút §bLinh Hồn Thật§f vào túi mà §aKHÔNG gây sát thương hay làm chết sinh vật đó§f! (Vô hiệu với Velgrynd & Milim).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §6Linh Hồn Thực Thể: §fRơi từ Boss/Mob (Quái, Warden, Wither, Rồng Ender, Thủy Tổ, Song Long, Milim) hoặc rút từ Ngày Tận Thế.", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §b2. Thần Thiết Ma Thoại (Magisteel Smartphone):", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §f[Chuột Phải] vào sinh vật hoặc tầm ngắm 32m (ngắm trống để tự soi bản thân): mở màn hình 3D xoay động đo §eMa Tố (EP)§f, HP, giáp, kỹ năng & bảng kháng tính chuẩn Tensura.", left + 16, y, fullW, maxY);
    }

    private void renderPageStellarRemote(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§b§l✦ THE STELLAR REMOTE (KHÔNG KÍCH QUỸ ĐẠO)", 0xFF55FFFF);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY, new ItemStack(ModItems.STELLAR_REMOTE.get()));
        drawWrapped(g, "§6[Thiết Bị Điều Khiển Vệ Tinh Vũ Trụ - 100% Zero Block Destruction]", left + 42, iconY + 5, bookW - 56, maxY);

        int y = top + 58;
        y = drawWrapped(g, "§e⚡ Thao Tác Điều Khiển:", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §a[Shift + Chuột Phải]: §fXoay đổi 3 chế độ vũ khí vệ tinh trên quỹ đạo.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §f• §c[Chuột Phải]: §fKhóa mục tiêu trong tầm 128m, bật góc quay vệ tinh vũ trụ & quân sự HUD rồi giáng đòn không kích!", left + 16, y, fullW, maxY);
        y += 4;
        y = drawWrapped(g, "§6⚡ 3 Chế Độ Hủy Diệt Không Gian (An Toàn 100% Cho Công Trình):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §b1. SS-01 · Railgun (Hồi 8s): §fBắn cột Plasma siêu trọng trường từ vệ tinh thẳng xuống tâm khóa.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §c2. SS-03 · Gungnir (Hồi 10s): §fPhóng Thần Thương Động Năng rơi thẳng từ quỹ đạo Sao Mộc.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §53. SS-04 · The Seven Stars (Hồi 13s): §fTriệu hồi tinh trận Bắc Đẩu Thất Tinh dội bão ánh sáng diện rộng.", left + 16, y, fullW, maxY);
    }

    private void renderPageCuuTheCombat(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§4§l✦ THIÊN NGOẠI CỨU THẾ QUYỀN HIỆN (1500 HP)", 0xFFFF5555);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.THIEN_MA_KATANA.get()),
                new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get()),
                new ItemStack(ModItems.CUU_THE_QUYEN_HIEN_SPAWN_EGG.get()));
        drawWrapped(g, "§6[Tốt Nghiệp Sinh Cấp S - Dark Gathering]", left + 82, iconY + 5, bookW - 96, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §e1. Hai Trạng Thái Chiến Đấu:", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §cDạng Kiếm: §7Cầm Thiên Ma Katana, tốc độ 0.30, chém 35 DMG.  §6• Võ Thần Tay Không: §7Đổi dạng khi bị Tiên Tri bẻ chiêu 2 lần (50 DMG, giảm 50% sát thương từ Player).", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §c2. Bộ 3 Kỹ Năng Mới:", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §bKhông Gian Trảm: §7Vung đao chém sóng chân không 18m hất tung.  §d• Thiên Ngoại Thần Áp: §7Dậm chấn động 10m ép rạp & cấm nhảy.  §a• Bát Nhã Kim Cang Thể: §7Phản 30% DMG, hồi 30 HP/s.", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §a3. Tương Quan & Kháng Tính:", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §fThắng 70% Ác Ma Thủy Tổ; kháng 90% Linh Tử Băng Hoại, 70% Jacob, 100% Phép Màu & Đức Tin. Chỉ thua tuyệt đối trước Milim & Long Chủng.", left + 16, y, fullW, maxY);
    }

    private void renderPageCuuTheSealingAndCurse(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§4§l✦ ĐOẠT MỆNH & PHONG ẤN SHIGARAKI", 0xFFFF5555);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.SEVERED_HEART.get()),
                new ItemStack(ModItems.SEVERED_BRAIN.get()),
                new ItemStack(ModItems.SHIGARAKI_TANUKI_EMPTY.get()),
                new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get()));
        drawWrapped(g, "§6[Tam Thức Cắt Rời Không Thời Gian & Tượng Gốm]", left + 104, iconY + 5, bookW - 118, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §c1. Tam Thức Đoạt Mệnh & Cách Phá Giải:", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §cThức 1 (Đoạt Tim - 20s): §7Rơi Tim; giải bằng Phép Màu Thần Linh.  §5• Thức 2 (Đoạt Não - 15s): §7Mù choáng; giải bằng Bí Ẩn Đức Tin.  §4• Thức 3: §7Song Trảm rơi cả Tim lẫn Não!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §a• PHÁ CHIÊU: §fBật Tiên Tri (Rykuzin Sinka) để nhìn thấy và chém tan linh hồn đao thức.", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §62. Khiêu Chiến, Phong Ấn & Thu Phục Tượng Shigaraki:", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• Cầm Tượng Rỗng §e[Shift + Chuột Phải]§f để gọi Boss. Khi Boss còn §c<= 20% HP (<= 300 HP)§f hoặc choáng, [Chuột Phải] Tượng Rỗng vào hắn để phong ấn!", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §f• [Chuột Phải] Tượng đã phong ấn để thả ra trợ chiến tối đa §e90 giây§f (cùng Kotodama 'Kiwamete'). Dùng Tượng Rỗng [Chuột Phải] thu hồi trước 90s kẻo hắn phản đồ sát!", left + 16, y, fullW, maxY);
    }

    private void renderPageTaKinhAndMarbas(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§4§l✦ TÀ KINH ĐẠI TĂNG & ĐẠI ÁC MA MARBAS", 0xFFFF5555);

        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.TA_KINH_DAI_TANG_SPAWN_EGG.get()),
                new ItemStack(ModItems.WARDING_EARPLUGS.get()),
                new ItemStack(ModItems.BODHI_EFFIGY_EMPTY.get()),
                new ItemStack(ModItems.BODHI_EFFIGY_MONK.get()),
                new ItemStack(ModItems.MARBAS_SPAWN_EGG.get()));
        drawWrapped(g, "§6[Hai Boss Tốt Nghiệp Sinh Cấp S Đỉnh Cao]", left + 124, iconY + 5, bookW - 138, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §c1. Tà Kinh Đại Tăng (1200 HP - Nhục Thân Bồ Tát Đọa Lạc):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §cTà Kinh Tuyệt Mệnh (20m): §7Nghe tiếng kinh quá 10s sẽ tử vong (Warden chịu x2.5 DMG). §aBẮT BUỘC đeo Bông Bịt Tai Phong Ấn để miễn nhiễm 100%!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• Đánh xuống <= 20% HP (<= 240 HP) rồi dùng Hình Nhân Gỗ Rỗng phong ấn; thả ra chiến đấu tối đa §e60s§f (hãy nhớ đeo Bông Bịt Tai!).", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §f2. Đại Ác Ma Marbas (1500 HP · 22 Giáp - Chúa Tể Giải Phẫu Học):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §bLãnh Địa Bàn Mổ (30m): §7Nếu sprint/nhảy liên tục sẽ dính §cÉp Tim Tử Tuyệt§7; Sóng Dao Mổ đảo lộn giác quan & tự hấp thụ sinh mạng mob tử trận để hồi máu!", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §f• §eChiến Lợi Phẩm: §fHạ gục rơi trọn bộ nội tạng thần linh (Tim, Não, Cánh Tay, Con Mắt Thị Nhục).", left + 16, y, fullW, maxY);
    }

    private void renderPageUnderworldAndDemons(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§c§l✦ MA GIỚI (UNDERWORLD) & ÁC MA MA GIỚI", 0xFFFF5577);
        int y = top + 34;
        y = drawWrapped(g, " §41. Địa Ngục -> Ma Giới (Underworld):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §fBước qua Cổng Nether để đặt chân tới §dMa Giới bầu trời tím đỏ rộng mở§f với 4 quần xã (Hẻm Huyết Nham, Đảo Bay Cột Trắng, Băng Vực U Minh, Thành Trì Thủy Tổ) nhưng vẫn giữ nguyên Fortress & Bastion.", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §52. Chủng Tộc Ác Ma Ma Giới (Hạ Vị -> Trung Vị -> Thượng Vị):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• Đa dạng hình hài (Nhân Dạng, Ác Thú, Quái Dị) cùng 7 dòng huyết thống; biết bay & niệm phép (từ Trung Vị trở lên). Phù Thủy / Evoker đôi khi triệu hồi Hạ Vị Ác Ma.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §eThể Xác: §fCầm Xương click Ác Ma hoặc để chúng hấp thụ xác mob/Dân Làng. Có thể Ban Tên (Demon Peer) và tiến hóa Ma Vương (§a30% kiểm soát§f / §c70% phản chủ§f, luôn yếu hơn Thủy Tổ).", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §f• Nhấn Chuột Phải vào Ác Ma đã thuần phục để đổi lệnh (§aTheo Hộ Vệ§f / §eTự Săn§f / §7Canh Gác§f).", left + 16, y, fullW, maxY);
    }

    private void renderPageVeldoraSageAzathoth(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, "§b§l✦ BẠO PHONG LONG, RAPHAEL & AZATHOTH", 0xFF38BDF8);
        int y = top + 34;
        y = drawWrapped(g, " §b1. Tam Đại Long Chủng (Veldora / Velgrynd / Velzard):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• Nhấn §a[V] §fđến Hang Veldora, giải phong ấn bằng §dAzathoth§f. Cả 3 Long Chủng đều §cphá khối khi bay (dạng rồng)§f & dùng §echiêu Ma Vương uy lực 1.4x§f (Veldora người biết bay).", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §5Phong Sát Kỹ Năng§f vô hiệu mọi chiêu thức của Boss/Ác Ma/Người chơi (ưu tiên mục tiêu đang nhìn hoặc 32m).", left + 16, y, fullW, maxY);
        y += 2;
        y = drawWrapped(g, " §b2. Thông Tuệ Vương Raphael ([G] & Auto-Battle [X]):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §e50 Kỹ Năng Chủ Động§f + §bAuto-Battle [X]§f tự bay né đòn, bật Uriel khi nguy cấp & xả chiêu 0.6s/lần ([X] / [Shift] trả quyền).", left + 16, y, fullW, maxY);
        drawWrapped(g, " §d3. Dạ Dày Vô Hạn Azathoth ([B]): §f120 ô chứa đồ, §eClick Túi Đồ cất trực tiếp§f, §dAuto-Loot Hư Không§f & Chuyển Hóa tăng vĩnh viễn §bMa Tố / EP§f!", left + 16, y, fullW, maxY);
    }

    private void renderPageTempestAndLabyrinth(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§6§l✦ ĐẠI THẢM HỌA, TINH LINH & MÊ CUNG 100 TẦNG", 0xFFFFD54F);
        int iconY = top + 32;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.SPIRIT_SHRINE.get()),
                new ItemStack(ModItems.LABYRINTH_GATEWAY.get()));
        drawWrapped(g, "§e[Tế Đàn Tinh Linh (Hỏa/Phong/Thổ/Thủy/Quang/Ám) · Cổng Mê Cung 100 Tầng]", left + 70, iconY + 5, bookW - 84, maxY);

        int y = top + 56;
        y = drawWrapped(g, " §3• Charybdis & Orc Disaster Geld: §fCharybdis tự xuất hiện ở Biển/Overworld đêm hoặc triệu hồi; đập Vây Lưng trên cao để vỡ khiên phép 80%!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §c• Phẫn Nộ Chi Vương Satanel: §fQuyền năng ĐỘC QUYỀN của §dMilim Nava§f khi lâm chiến — ma tố tăng vô tận.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §a• Tinh Linh & Dân Làng Tempest: §fĐặt Tế Đàn Tinh Linh + xúc tác nguyên tố để triệu hồi 6 Tinh Linh; Dân làng nhận Chúc Phúc thành Công Dân Tempest!", left + 16, y, fullW, maxY);
        y = drawWrapped(g, " §5• Mê Cung 100 Tầng Ramiris & Chronoa: §fBấm §bRaphael #8§f trong [G] để quét tọa độ Cổng Mê Cung gần nhất! Thanh tẩy Chronoa bằng Táo Vàng khi <30% HP.", left + 16, y, fullW, maxY);
        drawWrapped(g, " §4• Di Tích Tâm Linh Cấp S: §fĐập vỡ Tà Thần Tượng trước khi Lời Nguyền Đếm Ngược đạt 100%!", left + 16, y, fullW, maxY);
    }

    private void renderPageGreedAndTacticalCoPilot(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW, int mouseX, int mouseY) {
        drawHeaderTitle(g, left, top, bookW, "§d§l✦ KẺ THAM LAM & CỐ VẤN TÁC CHIẾN 1-CHẠM", 0xFFFF66CC);

        int iconY = top + 28;
        drawIconRow(g, left + 16, iconY, mouseX, mouseY,
                new ItemStack(ModItems.GREED_AWAKENING_ORB.get()),
                new ItemStack(ModItems.MARIABELL_SOUL.get()));
        drawWrapped(g, "§d[Ngọc Thức Tỉnh Kẻ Tham Lam & Tinh Hồn Mariabell Rosso]", left + 70, iconY + 5, bookW - 84, maxY);

        int y = top + 52;
        y = drawWrapped(g, " §d1. Kỹ Năng Độc Nhất: Kẻ Tham Lam (Greedy One - 强欲者):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §6Thức tỉnh: §fLà Chân Ma Vương, thanh tẩy Hồn Mariabell hoặc dùng §dNgọc Thức Tỉnh Tham Lam§f.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §c1. Khống Chế [Crouch]: §7Tẩy não quái 22m thành hộ vệ mắt đỏ trung thành.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §d2. Xiềng Xích [Chuột Phải]: §7Xích hồng trói, hút 25% máu tối đa & thể lực về bản thân.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §43. Bùng Nổ [Shift+Phải]: §7Gây 85-180 sát thương chuẩn tâm trí cực đại.", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• §eNội tại: §7+50% EXP khi hạ quái, 35% tước đoạt vũ khí kẻ địch vào Dạ Dày Azathoth.", left + 16, y, fullW, maxY);
        y += 3;
        y = drawWrapped(g, " §b2. Cố Vấn Tác Chiến 1-Chạm Đại Hiền Giả ([Y] / [N]):", left + 16, y, fullW, maxY);
        y = drawWrapped(g, "   §f• Đại Hiền Giả tự động cảnh báo nguy cấp (máu yếu, void, lava, quái đông, đồ rơi, Veldora giận dữ...).", left + 16, y, fullW, maxY);
        drawWrapped(g, "   §f• Bấm §a[Y] (Đồng ý)§f thi triển tức thì trong 1 tick (Cấp cứu, Né void, Hóa đá lava, Hút khoáng, Trấn an rồng). Bấm §c[N]§f để từ chối.", left + 16, y, fullW, maxY);
    }

    private void renderPageAuthor(GuiGraphics g, int left, int top, int bookW, int maxY, int fullW) {
        drawHeaderTitle(g, left, top, bookW, Component.translatable("gui.weapons.guide.p9.title").getString(), 0xFFFFAA00);

        int x = left + 16;
        int y = top + 34;
        y = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.author").getString(), x, y, fullW, maxY, 13);
        y = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.studio").getString(), x, y, fullW, maxY, 13);
        y = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.university").getString(), x, y, fullW, maxY, 13);
        y = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.mod_name").getString(), x, y, fullW, maxY, 13);
        y = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.license").getString(), x, y, fullW, maxY, 13);

        y += 4;
        int quoteBoxW = bookW - 32;
        g.fill(x, y - 2, x + quoteBoxW, y + 40, 0xFF231B30);
        drawBorder(g, x, y - 2, quoteBoxW, 42, 0xFFD4AF37);

        int qy = y + 3;
        qy = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.quote1").getString(), x + 6, qy, quoteBoxW - 12, maxY, 11);
        qy = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.quote2").getString(), x + 6, qy, quoteBoxW - 12, maxY, 11);
        drawWrapped(g, Component.translatable("gui.weapons.guide.p9.quote3").getString(), x + 6, qy, quoteBoxW - 12, maxY, 11);

        y += 46;
        y = drawWrapped(g, Component.translatable("gui.weapons.guide.p9.github").getString(), x, y, fullW, maxY, 11);
        drawWrapped(g, "§9§n" + GITHUB_URL, x, y, fullW, maxY, 11);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
