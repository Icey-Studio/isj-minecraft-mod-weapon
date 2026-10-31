package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;

public class GuideBookScreen extends Screen {

    private int currentPage = 0;
    private static final int TOTAL_PAGES = 19;

    private static final net.minecraft.resources.ResourceLocation DRAGON_NOVA_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/dragon_nova_art.png");
    private static final net.minecraft.resources.ResourceLocation TAISUI_ART_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/taisui_art.png");
    private static final net.minecraft.resources.ResourceLocation SEER_FLESH_ART_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/seer_flesh_art.png");
    private static final net.minecraft.resources.ResourceLocation SEER_ARM_COMBO_ART_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/seer_arm_combo_art.png");
    private static final net.minecraft.resources.ResourceLocation CARRERA_ART_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("weapons", "textures/gui/carrera_art.png");

    // Kích thước khung sách rộng rãi 430px x 236px, tuyệt đối không bị tràn chữ ra màn hình
    private static final int BOOK_WIDTH = 430;
    private static final int BOOK_HEIGHT = 236;

    private Button prevButton;
    private Button nextButton;
    private Button closeButton;



    // Các nút chuyển giáp (Trang 5)
    private Button armorHelmBtn;
    private Button armorChestBtn;
    private Button armorLegsBtn;
    private Button armorBootsBtn;
    private int selectedArmorPiece = 1; // 0: Mũ, 1: Áo, 2: Quần, 3: Giày

    // Các nút trang tác giả (Trang 6)
    private Button openGithubBtn;
    private Button copyGithubBtn;
    private static final String GITHUB_URL = "https://github.com/MinhPhuc00ne/isj-minecraft-mod-weapon";

    private ItemStack hoveredTooltipStack = null;

    // Cache các lưới công thức 3x3
    private final ItemStack[][] moonlightSwordRecipe = new ItemStack[3][3];
    private final ItemStack[][] demonLordSeedRecipe = new ItemStack[3][3];

    private final ItemStack[][][] divineArmorRecipes = new ItemStack[4][3][3];

    public GuideBookScreen() {
        super(Component.translatable("gui.weapons.guide.screen_title"));
        initRecipes();
    }

    private void initRecipes() {
        // 1. Công thức Nguyệt Quang Thần Tế Kiếm
        moonlightSwordRecipe[0][0] = new ItemStack(Items.DIAMOND);
        moonlightSwordRecipe[0][1] = new ItemStack(Items.NETHER_STAR);
        moonlightSwordRecipe[0][2] = new ItemStack(Items.DIAMOND);

        moonlightSwordRecipe[1][0] = new ItemStack(Items.ENDER_EYE);
        moonlightSwordRecipe[1][1] = new ItemStack(Items.GOLDEN_APPLE);
        moonlightSwordRecipe[1][2] = new ItemStack(Items.ENDER_EYE);

        moonlightSwordRecipe[2][0] = new ItemStack(Items.GOLD_INGOT);
        moonlightSwordRecipe[2][1] = new ItemStack(Items.DIAMOND_SWORD);
        moonlightSwordRecipe[2][2] = new ItemStack(Items.GOLD_INGOT);

        // 2. Công thức Hạt Giống Ma Vương
        demonLordSeedRecipe[0][0] = new ItemStack(Items.OBSIDIAN);
        demonLordSeedRecipe[0][1] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[0][2] = new ItemStack(Items.OBSIDIAN);

        demonLordSeedRecipe[1][0] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[1][1] = new ItemStack(Items.NETHER_STAR);
        demonLordSeedRecipe[1][2] = new ItemStack(Items.GHAST_TEAR);

        demonLordSeedRecipe[2][0] = new ItemStack(Items.OBSIDIAN);
        demonLordSeedRecipe[2][1] = new ItemStack(Items.GHAST_TEAR);
        demonLordSeedRecipe[2][2] = new ItemStack(Items.OBSIDIAN);



        // 4. Công thức 4 Món Giáp Thần Linh (Mũ, Áo, Quần, Giày)
        ItemStack[] netheriteArmors = new ItemStack[] {
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

        int left = (this.width - BOOK_WIDTH) / 2;
        int top = (this.height - BOOK_HEIGHT) / 2;

        // Nút trang trước
        prevButton = Button.builder(Component.translatable("gui.weapons.guide.btn.prev"), b -> {
            if (currentPage > 0) currentPage--;
            updateButtonState();
        }).bounds(left + 16, top + BOOK_HEIGHT - 26, 95, 20).build();

        // Nút trang sau
        nextButton = Button.builder(Component.translatable("gui.weapons.guide.btn.next"), b -> {
            if (currentPage < TOTAL_PAGES - 1) currentPage++;
            updateButtonState();
        }).bounds(left + BOOK_WIDTH - 111, top + BOOK_HEIGHT - 26, 95, 20).build();

        // Nút Đóng
        closeButton = Button.builder(Component.translatable("gui.weapons.guide.btn.close"), b -> {
            this.onClose();
        }).bounds(left + BOOK_WIDTH / 2 - 30, top + BOOK_HEIGHT - 26, 60, 20).build();

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(closeButton);



        // --- Nút trang 5 (4 Món Giáp) ---
        int armorBtnY = top + 80;
        armorHelmBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_helm"), b -> selectedArmorPiece = 0)
                .bounds(left + 130, armorBtnY, 46, 18).build();
        armorChestBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_chest"), b -> selectedArmorPiece = 1)
                .bounds(left + 182, armorBtnY, 46, 18).build();
        armorLegsBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_legs"), b -> selectedArmorPiece = 2)
                .bounds(left + 234, armorBtnY, 46, 18).build();
        armorBootsBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.armor_boots"), b -> selectedArmorPiece = 3)
                .bounds(left + 286, armorBtnY, 46, 18).build();

        this.addRenderableWidget(armorHelmBtn);
        this.addRenderableWidget(armorChestBtn);
        this.addRenderableWidget(armorLegsBtn);
        this.addRenderableWidget(armorBootsBtn);

        // --- Nút trang 10 (Tác Giả & GitHub) ---
        int authorBtnY = top + BOOK_HEIGHT - 54;
        openGithubBtn = Button.builder(Component.translatable("gui.weapons.guide.btn.open_github"), b -> {
            try {
                Util.getPlatform().openUri(URI.create(GITHUB_URL));
            } catch (Exception ignored) {}
        }).bounds(left + BOOK_WIDTH / 2 - 135, authorBtnY, 130, 20).build();

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
        }).bounds(left + BOOK_WIDTH / 2 + 5, authorBtnY, 130, 20).build();

        this.addRenderableWidget(openGithubBtn);
        this.addRenderableWidget(copyGithubBtn);

        updateButtonState();
    }

    private void updateButtonState() {
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < TOTAL_PAGES - 1;

        boolean isArmorPage = (currentPage == 3);
        if (armorHelmBtn != null) armorHelmBtn.visible = isArmorPage;
        if (armorChestBtn != null) armorChestBtn.visible = isArmorPage;
        if (armorLegsBtn != null) armorLegsBtn.visible = isArmorPage;
        if (armorBootsBtn != null) armorBootsBtn.visible = isArmorPage;

        boolean isAuthorPage = (currentPage == 18);
        if (openGithubBtn != null) openGithubBtn.visible = isAuthorPage;
        if (copyGithubBtn != null) copyGithubBtn.visible = isAuthorPage;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = (this.width - BOOK_WIDTH) / 2;
        int top = (this.height - BOOK_HEIGHT) / 2;

        hoveredTooltipStack = null;

        // 1. Khung bìa sách cổ huyền bí
        guiGraphics.fill(left - 3, top - 3, left + BOOK_WIDTH + 3, top + BOOK_HEIGHT + 3, 0xFF140D05); // Viền da
        guiGraphics.fill(left - 1, top - 1, left + BOOK_WIDTH + 1, top + BOOK_HEIGHT + 1, 0xFFD4AF37); // Chỉ vàng
        guiGraphics.fill(left, top, left + BOOK_WIDTH, top + BOOK_HEIGHT, 0xFF1B1622); // Ruột giấy cổ

        // Tiêu đề thanh trên
        guiGraphics.fill(left + 8, top + 6, left + BOOK_WIDTH - 8, top + 26, 0xFF282033);
        guiGraphics.fill(left + 8, top + 25, left + BOOK_WIDTH - 8, top + 26, 0xFFD4AF37);

        // Số trang góc phải
        String pageStr = Component.translatable("gui.weapons.guide.page_indicator", currentPage + 1, TOTAL_PAGES).getString();
        guiGraphics.drawString(font, "§7" + pageStr, left + BOOK_WIDTH - font.width(pageStr) - 14, top + 11, 0xFFFFFFFF, false);

        // 2. Nội dung từng trang
        switch (currentPage) {
            case 0 -> renderPageOverview(guiGraphics, left, top, mouseX, mouseY);
            case 1 -> renderPageMoonlightSword(guiGraphics, left, top, mouseX, mouseY);
            case 2 -> renderPageDemonLord(guiGraphics, left, top, mouseX, mouseY);
            case 3 -> renderPageDivineArmor(guiGraphics, left, top, mouseX, mouseY);
            case 4 -> renderPageDragonNova(guiGraphics, left, top, mouseX, mouseY);
            case 5 -> renderPageTaisui(guiGraphics, left, top, mouseX, mouseY);
            case 6 -> renderPageSeerFlesh(guiGraphics, left, top, mouseX, mouseY);
            case 7 -> renderPageSeerArmCombo(guiGraphics, left, top, mouseX, mouseY);
            case 8 -> renderPageAlkaid(guiGraphics, left, top, mouseX, mouseY);
            case 9 -> renderPageGoldenGun(guiGraphics, left, top, mouseX, mouseY);
            case 10 -> renderPagePrimordialsOverview(guiGraphics, left, top, mouseX, mouseY);
            case 11 -> renderPagePrimordialsPowerBoss(guiGraphics, left, top, mouseX, mouseY);
            case 12 -> renderPagePrimordialsDomain(guiGraphics, left, top, mouseX, mouseY);
            case 13 -> renderPageVelgrynd(guiGraphics, left, top, mouseX, mouseY);
            case 14 -> renderPageMagisteelPhone(guiGraphics, left, top, mouseX, mouseY);
            case 15 -> renderPageCuuTheQuyenHien(guiGraphics, left, top, mouseX, mouseY);
            case 16 -> renderPageCuuTheSealingAndCurse(guiGraphics, left, top, mouseX, mouseY);
            case 17 -> renderPageTaKinhDaiTang(guiGraphics, left, top, mouseX, mouseY);
            case 18 -> renderPageAuthor(guiGraphics, left, top, mouseX, mouseY);
        }

        // 3. Render Tooltip nếu đang di chuột qua slot item
        if (hoveredTooltipStack != null && !hoveredTooltipStack.isEmpty()) {
            guiGraphics.renderTooltip(font, hoveredTooltipStack, mouseX, mouseY);
        }
    }

    /**
     * TRANG 1: MA ĐẠO THƯ
     */
    private void renderPageOverview(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.title").getString(), left + 14, top + 11, 0xFFFFAA00, false);

        int textY = top + 36;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.greeting").getString(), left + 14, textY, 0xFFFFFFFF, false);

        textY += 16;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.intro1").getString(), left + 14, textY, 0xFFDDDDDD, false);
        textY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.intro2").getString(), left + 14, textY, 0xFFDDDDDD, false);

        textY += 18;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.controls_title").getString(), left + 14, textY, 0xFF55FFFF, false);

        textY += 14;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.control1").getString(), left + 18, textY, 0xFFFFFFFF, false);
        textY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.control2").getString(), left + 18, textY, 0xFFFFFFFF, false);
        textY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.control3a").getString(), left + 18, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.control3b").getString(), left + 18, textY, 0xFFFFFFFF, false);

        textY += 20;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p0.footer").getString(), left + 14, textY, 0xFF888888, false);
    }

    /**
     * TRANG 2: NGUYỆT QUANG THẦN TẾ KIẾM
     */
    private void renderPageMoonlightSword(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.title").getString(), left + 14, top + 11, 0xFFFFAA00, false);

        int gridX = left + 16;
        int gridY = top + 34;

        drawCraftingGrid(guiGraphics, gridX, gridY, moonlightSwordRecipe, new ItemStack(ModItems.MOONLIGHT_SWORD.get()), mouseX, mouseY);

        int descX = left + 134;
        int descY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.name").getString(), descX, descY, 0xFFFFAA00, false);
        descY += 14;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.desc").getString(), descX, descY, 0xFFDDDDDD, false);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.durability").getString(), descX, descY, 0xFF55FF55, false);

        int skillY = top + 104;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skills_title").getString(), left + 14, skillY, 0xFFFFDD00, false);

        skillY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skill1").getString(), left + 18, skillY, 0xFFFFFFFF, false);
        skillY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skill2").getString(), left + 18, skillY, 0xFFFFFFFF, false);
        skillY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skill3").getString(), left + 18, skillY, 0xFFFFFFFF, false);
        skillY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skill3a").getString(), left + 18, skillY, 0xFFFFFFFF, false);
        skillY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skill3b").getString(), left + 18, skillY, 0xFFFFFFFF, false);
        skillY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p1.skill4").getString(), left + 18, skillY, 0xFFFFAA00, false);
    }

    /**
     * TRANG 3: CON ĐƯỜNG THỨC TỈNH CHÂN MA VƯƠNG
     */
    private void renderPageDemonLord(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.title").getString(), left + 14, top + 11, 0xFFFF55FF, false);

        int gridX = left + 16;
        int gridY = top + 34;

        drawCraftingGrid(guiGraphics, gridX, gridY, demonLordSeedRecipe, new ItemStack(ModItems.DEMON_LORD_SEED.get()), mouseX, mouseY);

        int descX = left + 134;
        int descY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.seed_name").getString(), descX, descY, 0xFFFF55FF, false);
        descY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.how_to_get").getString(), descX, descY, 0xFF55FF55, false);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.exp_note").getString(), descX, descY, 0xFFFFFFFF, false);
        descY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.soul_note").getString(), descX, descY, 0xFFFFFFFF, false);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.right_click").getString(), descX, descY, 0xFFFFAA00, false);
        descY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.harvest").getString(), descX, descY, 0xFFFFAA00, false);

        int awakenY = top + 104;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.perks_title").getString(), left + 14, awakenY, 0xFFFF55FF, false);

        awakenY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.perk1").getString(), left + 18, awakenY, 0xFFFFFFFF, false);
        awakenY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.perk2").getString(), left + 18, awakenY, 0xFFFFFFFF, false);
        awakenY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.perk3").getString(), left + 18, awakenY, 0xFFFFFFFF, false);
        awakenY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p2.perk4").getString(), left + 18, awakenY, 0xFFFF55FF, false);
    }



    /**
     * TRANG 5: THẦN LINH VŨ TRANG (DIVINE ARMOR)
     */
    private void renderPageDivineArmor(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.title").getString(), left + 14, top + 11, 0xFFFFAA00, false);

        int gridX = left + 16;
        int gridY = top + 34;

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

        drawCraftingGrid(guiGraphics, gridX, gridY, divineArmorRecipes[selectedArmorPiece], outputArmor, mouseX, mouseY);

        int descX = left + 134;
        int descY = top + 34;
        guiGraphics.drawString(font, "§e§l" + armorPieceName, descX, descY, 0xFFFFFF55, false);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.desc1").getString(), descX, descY, 0xFFFFFFFF, false);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.desc2").getString(), descX, descY, 0xFF55FF55, false);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.desc3").getString(), descX, descY, 0xFF888888, false);

        int effY = top + 104;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.imm_title").getString(), left + 14, effY, 0xFFFFAA00, false);

        effY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.imm1").getString(), left + 18, effY, 0xFFFFFFFF, false);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.imm2").getString(), left + 18, effY, 0xFFFFFFFF, false);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.imm3").getString(), left + 18, effY, 0xFFFF55FF, false);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p4.weakness").getString(), left + 18, effY, 0xFFFFFFFF, false);
    }

    /**
     * TRANG 6: CẤM THUẬT DIỆT RỒNG - LONG TINH BỘC VIÊM BÁ (DRAGON NOVA)
     */
    private void renderPageDragonNova(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.title").getString(), left + 14, top + 11, 0xFFFF55FF, false);

        int imgX = left + 14;
        int imgY = top + 34;
        int imgW = 120;
        int imgH = 88;

        // Viền vàng kim tinh xảo quanh tranh minh họa
        guiGraphics.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFFD4AF37);
        guiGraphics.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        guiGraphics.blit(DRAGON_NOVA_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        // Văn phong thần thoại mô tả trận trảm sát Tinh Long
        int descX = left + 144;
        int descY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.name1").getString(), descX, descY, 0xFFFF55FF, true);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.name2").getString(), descX, descY, 0xFFFF88FF, true);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.desc1").getString(), descX, descY, 0xFFDDDDDD, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.desc2").getString(), descX, descY, 0xFFDDDDDD, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.desc3").getString(), descX, descY, 0xFFFF6666, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.desc4").getString(), descX, descY, 0xFF55FF55, true);

        int effY = top + 128;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.power_title").getString(), left + 14, effY, 0xFFFFAA00, true);

        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.power1").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.power2").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.power3").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p5.power4").getString(), left + 18, effY, 0xFFFFFF55, true);
    }

    /**
     * TRANG 7: THÁI TUẾ TINH QUÂN - DARK GATHERING
     */
    private void renderPageTaisui(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.title").getString(), left + 14, top + 11, 0xFFFFCC00, true);

        int imgX = left + 14;
        int imgY = top + 34;
        int imgW = 120;
        int imgH = 88;

        // Viền hoàng kim cổ thần
        guiGraphics.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFFE5C158);
        guiGraphics.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        guiGraphics.blit(TAISUI_ART_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        // Mô tả Thái Tuế Tinh Quân
        int descX = left + 144;
        int descY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.name").getString(), descX, descY, 0xFFFFD700, true);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.desc").getString(), descX, descY, 0xFFDDDDDD, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.star_name").getString(), descX, descY, 0xFFFF88FF, true);
        descY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.star_click").getString(), descX, descY, 0xFFFFFFFF, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.array_name").getString(), descX, descY, 0xFF55FFFF, true);
        descY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.array_sub").getString(), descX, descY, 0xFFFFFFFF, true);

        int effY = top + 128;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.rules_title").getString(), left + 14, effY, 0xFFFFAA00, true);

        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.rule1").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.rule2").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.rule3").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p6.rule4").getString(), left + 18, effY, 0xFFFFFF55, true);
    }

    /**
     * TRANG 8: THÁI TUẾ TINH QUÂN - THỊ NHỤC (SEER FLESH)
     */
    private void renderPageSeerFlesh(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.title").getString(), left + 14, top + 11, 0xFFFF4444, true);

        int imgX = left + 14;
        int imgY = top + 34;
        int imgW = 120;
        int imgH = 88;

        // Viền đỏ thẫm huyết tế quanh tranh minh họa
        guiGraphics.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFFCC0022);
        guiGraphics.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        guiGraphics.blit(SEER_FLESH_ART_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        // Mô tả Thị Nhục
        int descX = left + 144;
        int descY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.name").getString(), descX, descY, 0xFFFF5555, true);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.desc").getString(), descX, descY, 0xFFDDDDDD, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.summon").getString(), descX, descY, 0xFFFFAA00, true);
        descY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.distance").getString(), descX, descY, 0xFF888888, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.harvest").getString(), descX, descY, 0xFF55FF55, true);
        descY += 10;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.attack").getString(), descX, descY, 0xFFFF8888, true);

        int effY = top + 128;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.effects_title").getString(), left + 14, effY, 0xFFFFAA00, true);

        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.eff1").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.eff2").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.eff3").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p7.eff4").getString(), left + 18, effY, 0xFFFFFF55, true);
    }

    /**
     * TRANG 9: CỘNG HƯỞNG THÁI TUẾ - THỊ NHỤC BỌC TAY & TUYỆT DIỆT TINH TÚ
     */
    private void renderPageSeerArmCombo(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.title").getString(), left + 14, top + 11, 0xFFFFAA00, false);

        // Ảnh minh họa chiến đấu Dark Gathering hoành tráng
        int imgX = left + 16;
        int imgY = top + 34;
        int imgW = 126;
        int imgH = 92;

        // Viền ngọc bích ma quái quanh tranh minh họa
        guiGraphics.fill(imgX - 2, imgY - 2, imgX + imgW + 2, imgY + imgH + 2, 0xFF2E5A36);
        guiGraphics.fill(imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, 0xFF1B1622);
        guiGraphics.blit(SEER_ARM_COMBO_ART_TEXTURE, imgX, imgY, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        // Mô tả hình thái cộng hưởng bên phải ảnh
        int descX = left + 150;
        int descY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.name").getString(), descX, descY, 0xFF55FF55, true);
        descY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.desc").getString(), descX, descY, 0xFFDDDDDD, true);
        descY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.combo_trigger").getString(), descX, descY, 0xFFFFAA00, true);
        descY += 12;

        // Hiển thị 2 vật phẩm preview trực quan có tooltip
        drawSingleItemSlot(guiGraphics, descX, descY, new ItemStack(ModItems.SEER_FLESH_ARM.get()), mouseX, mouseY);
        drawSingleItemSlot(guiGraphics, descX + 26, descY, new ItemStack(ModItems.EXTINCTION_STAR.get()), mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.item_note").getString(), descX + 54, descY + 6, 0xFFFFFF55, true);

        // Các quy tắc và uy lực chiến đấu
        int effY = top + 130;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.effects_title").getString(), left + 14, effY, 0xFFFFAA00, true);

        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.eff1").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.eff2").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.eff3").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 11;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p8.eff4").getString(), left + 18, effY, 0xFF55FFFF, true);
    }

    private void drawSingleItemSlot(GuiGraphics guiGraphics, int slotX, int slotY, ItemStack item, int mouseX, int mouseY) {
        guiGraphics.fill(slotX, slotY, slotX + 20, slotY + 20, 0xFF373737);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 19, slotY + 19, 0xFF8B8B8B);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 18, slotY + 18, 0xFF2A2A2A);

        if (item != null && !item.isEmpty()) {
            guiGraphics.renderItem(item, slotX + 2, slotY + 2);
            if (mouseX >= slotX && mouseX <= slotX + 20 && mouseY >= slotY && mouseY <= slotY + 20) {
                hoveredTooltipStack = item;
            }
        }
    }

    /**
     * TRANG 10: THÁI TUẾ TINH QUÂN - DIỆT THẾ TÀ TINH: ALKAID (LOW COMPRESSION DECOMPOSITION)
     */
    private void renderPageAlkaid(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.title").getString(), left + 14, top + 11, 0xFFFF2222, true);

        int textX = left + 16;
        int textY = top + 34;

        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.name").getString(), textX, textY, 0xFFFF5555, true);
        textY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.desc").getString(), textX, textY, 0xFFDDDDDD, true);
        textY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.condition").getString(), textX, textY, 0xFFFFAA00, true);
        textY += 13;

        // Preview 2 vật phẩm Alkaid: Tà Cầu Phân Rã & Xoáy Khoan Hủy Diệt
        drawSingleItemSlot(guiGraphics, textX, textY, new ItemStack(ModItems.ALKAID_SPHERE.get()), mouseX, mouseY);
        drawSingleItemSlot(guiGraphics, textX + 26, textY, new ItemStack(ModItems.ALKAID_VORTEX.get()), mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.item_note").getString(), textX + 54, textY + 6, 0xFFFFFF55, true);

        int effY = top + 92;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.effects_title").getString(), left + 14, effY, 0xFFFFAA00, true);

        effY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.eff1").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.eff2").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.eff3").getString(), left + 18, effY, 0xFFFFFFFF, true);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.eff4").getString(), left + 18, effY, 0xFF55FFFF, true);
        effY += 12;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p10.eff5").getString(), left + 18, effY, 0xFFFF5555, true);
    }

    /**
     * TRANG 11: TÁC GIẢ & BẢN QUYỀN DỰ ÁN
     */
    /**
     * TRANG 11: SÚNG LỤC HOÀNG KIM & MA ĐẠN (CARRERA - TENSURA LN)
     */
    private void renderPageGoldenGun(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§6§lSÚNG LỤC HOÀNG KIM & 4 LOẠI MA ĐẠN", left + 14, top + 11, 0xFFFFAA00, false);

        int artX = left + 14;
        int artY = top + 34;
        int artSize = 88;
        guiGraphics.fill(artX - 1, artY - 1, artX + artSize + 1, artY + artSize + 1, 0xFFD4AF37);
        guiGraphics.blit(CARRERA_ART_TEXTURE, artX, artY, 0, 0, artSize, artSize, artSize, artSize);

        int iconY = artY + artSize + 6;
        guiGraphics.renderItem(new ItemStack(ModItems.GOLDEN_GUN.get()), artX + 10, iconY);
        guiGraphics.drawString(font, "§eCarrera Gun", artX + 32, iconY + 4, 0xFFFFDD00, false);

        int descX = left + 110;
        int descY = top + 34;
        guiGraphics.drawString(font, "§e§lHoàng Sắc Thủy Tổ Carrera §7(Tensura)", descX, descY, 0xFFFFAA00, false);
        descY += 12;
        guiGraphics.drawString(font, "§7Kế thừa từ Kondou + Tối Thượng Abaddon", descX, descY, 0xFFCCCCCC, false);
        descY += 14;
        guiGraphics.drawString(font, "§a👉 Cầm súng ấn [PgUp]: §fMở ổ xoay chọn đạn.", descX, descY, 0xFF55FFFF, false);
        descY += 10;
        guiGraphics.drawString(font, "§c(Yêu cầu có ít nhất 1 viên ma đạn trong túi đồ)", descX, descY, 0xFFFF7777, false);

        descY += 14;
        guiGraphics.drawString(font, "§6⚡ 4 Chủng Loại Ma Đạn Nghệ Thuật:", descX, descY, 0xFFFFDD00, false);
        descY += 12;
        guiGraphics.drawString(font, "§e1. Thần Tốc Phán Quyết: §c§lBẮN PHÁT CHẾT LUÔN!", descX + 4, descY, 0xFFFFFFFF, false);
        descY += 10;
        guiGraphics.drawString(font, "§7   Phá hủy lõi linh hồn + Giọng Nói Thế Giới thông báo.", descX + 4, descY, 0xFFDDDDDD, false);
        descY += 11;
        guiGraphics.drawString(font, "§d2. Hạch Thâm Uyên: §fHố đen trọng lực nuốt & nổ lớn.", descX + 4, descY, 0xFFFFFFFF, false);
        descY += 11;
        guiGraphics.drawString(font, "§93. Trọng Lực Sụp Đổ: §fKhóa cứng di chuyển mục tiêu.", descX + 4, descY, 0xFFFFFFFF, false);
        descY += 11;
        guiGraphics.drawString(font, "§64. Hoàng Kim Xạ Kích: §fBắn liên thanh xuyên giáp.", descX + 4, descY, 0xFFFFFFFF, false);
    }

    /**
     * TRANG 12: THẤT ĐẠI THỦY TỔ ÁC MA (TENSURA PRIMORDIALS)
     */
    private void renderPagePrimordialsOverview(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§c§lTHẤT ĐẠI THỦY TỔ ÁC MA • MA GIỚI TENSURA", left + 14, top + 11, 0xFFFFAA00, false);

        int slotY = top + 32;
        int slotStartX = left + 18;
        ItemStack[] pacts = {
                new ItemStack(ModItems.PRIMORDIAL_PACT_NOIR.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_ROUGE.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_BLANC.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_JAUNE.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_VIOLET.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_BLEU.get()),
                new ItemStack(ModItems.PRIMORDIAL_PACT_VERT.get())
        };

        for (int i = 0; i < pacts.length; i++) {
            drawSingleItemSlot(guiGraphics, slotStartX + i * 22, slotY, pacts[i], mouseX, mouseY);
        }
        guiGraphics.drawString(font, "§e7 Cuộn Khế Ước Thủy Tổ", slotStartX + pacts.length * 22 + 6, slotY + 6, 0xFFFFDD00, false);

        int textY = top + 58;
        guiGraphics.drawString(font, "§6⚡ 1. Nguồn Gốc & Triệu Hồi Ban Đầu:", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• 7 Ác Ma Khởi Nguyên giáng thế từ Ma Giới: §cRouge§7, §8Noir§7, §fBlanc§7, §eJaune§7...", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Dùng §cPháp Điển Khế Ước §7để mở nghi thức triệu hồi thử thách sức mạnh.", left + 22, textY, 0xFFDDDDDD, false);

        textY += 14;
        guiGraphics.drawString(font, "§6⚔ 2. Đánh Bại, Rơi Khế Ước & Phục Tùng:", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• Khi bị đánh bại, mỗi người §erơi ra đúng cuộn Khế Ước Thủy Tổ §7của họ.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§a• Chuột Phải cuộn khế ước: §fTriệu hồi họ xuất thế (có thông báo & thoại).", left + 22, textY, 0xFF55FF55, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Họ sẽ §bphục tùng và bảo vệ chủ nhân §7trừ khi bị ai đó khác giết chết.", left + 22, textY, 0xFF55FFFF, false);

        textY += 14;
        guiGraphics.drawString(font, "§6✦ 3. Thu Hồi Nghỉ Ngơi (Cho Lui Đi):", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§e• Khi ác ma đang có mặt: §fBấm Chuột Phải thêm lần nữa để §6cho họ lui đi§f!", left + 22, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Họ sẽ đọc thoại tạm biệt và thu hồi an toàn vào ấn chú.", left + 22, textY, 0xFFCCCCCC, false);
        textY += 11;
        guiGraphics.drawString(font, "§d• Shift + Chuột Phải: §fDịch chuyển đến gần & Đổi lệnh §a(Đi Theo / Đứng Canh)§f.", left + 22, textY, 0xFFFFAAFF, false);
    }

    /**
     * TRANG 13: XẾP HẠNG SỨC MẠNH & TỶ LỆ DIỆT BOSS
     */
    private void renderPagePrimordialsPowerBoss(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§6§lXẾP HẠNG SỨC MẠNH & TỶ LỆ DIỆT BOSS", left + 14, top + 11, 0xFFFFAA00, false);

        int textY = top + 34;
        guiGraphics.drawString(font, "§c§l1. Rouge & Noir (Tối Thượng): §e§l90% DIỆT BOSS", left + 18, textY, 0xFFFF5555, false);
        textY += 12;
        guiGraphics.drawString(font, "§f   HP: 3600 | DMG: 160 §7(Ngang nhau, mạnh gấp 4 lần ác ma thường, gấp đôi Blanc).", left + 18, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§c   * Lưu ý: Nếu chủ quan / sơ suất vẫn có 10% rủi ro bị Boss hạ gục.", left + 18, textY, 0xFFFF8888, false);

        textY += 14;
        guiGraphics.drawString(font, "§f§l2. Blanc - Testarossa (Đại Thủy Tổ): §a§l75% DIỆT BOSS", left + 18, textY, 0xFFFFFFFF, false);
        textY += 12;
        guiGraphics.drawString(font, "§7   HP: 1800 | DMG: 80 §7(Bạch Viêm Thánh Ma, mạnh gấp đôi các ác ma thường).", left + 18, textY, 0xFFCCCCCC, false);

        textY += 14;
        guiGraphics.drawString(font, "§e§l3. Jaune - Carrera (Hủy Diệt Tối Cao): §a§l65% DIỆT BOSS", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7   HP: 1500 | DMG: 70 §7(Đại Ma Pháp Hạt Nhân Đa Vòng & Súng Carrera liên hoàn).", left + 18, textY, 0xFFDDDDDD, false);

        textY += 14;
        guiGraphics.drawString(font, "§5§l4. Violet - Ultima: §d§l33% DIỆT BOSS §7(HP: 950 | Tử Độc Khởi Nguyên)", left + 18, textY, 0xFFAAFF, false);
        textY += 12;
        guiGraphics.drawString(font, "§a§l5. Vert - Misery: §d§l30% DIỆT BOSS §7(HP: 900 | Bão Tố & Khiên Hồi Phục)", left + 18, textY, 0xFF55FF55, false);
        textY += 12;
        guiGraphics.drawString(font, "§9§l6. Bleu - Rain: §b§l20% DIỆT BOSS §7(HP: 800 | Hàn Băng - Yếu nhất trong 7 ác ma)", left + 18, textY, 0xFF55FFFF, false);
        textY += 13;
        guiGraphics.drawString(font, "§7(Tuy yếu hơn các ác ma khác nhưng Bleu vẫn mạnh hơn đa số quái thường của các mod).", left + 18, textY, 0xFF888888, false);
    }

    /**
     * TRANG 14: BÀNH TRƯỚNG LÃNH ĐỊA & KHỐI KẾT GIỚI
     */
    private void renderPagePrimordialsDomain(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§d§lBÀNH TRƯỚNG LÃNH ĐỊA & KHỐI KẾT GIỚI", left + 14, top + 11, 0xFFFFAA00, false);

        int slotY = top + 32;
        int slotStartX = left + 18;
        ItemStack[] barrierItems = {
                new ItemStack(ModBlocks.DOMAIN_BARRIER_NOIR.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_ROUGE.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_BLANC.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_JAUNE.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_VIOLET.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_BLEU.get()),
                new ItemStack(ModBlocks.DOMAIN_BARRIER_VERT.get())
        };

        for (int i = 0; i < barrierItems.length; i++) {
            drawSingleItemSlot(guiGraphics, slotStartX + i * 22, slotY, barrierItems[i], mouseX, mouseY);
        }
        guiGraphics.drawString(font, "§b7 Khối Kết Giới", slotStartX + barrierItems.length * 22 + 6, slotY + 6, 0xFF88CCFF, false);

        int textY = top + 58;
        guiGraphics.drawString(font, "§e⚡ 1. Khối Kết Giới Lãnh Địa Thực Tế:", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• Khi ác ma bộc phát Tuyệt Kỹ 4, kết giới vòm khối 16m được kiến tạo.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Khối kết giới phát quang mang sắc màu và hoa văn động riêng của từng người.", left + 22, textY, 0xFFDDDDDD, false);

        textY += 14;
        guiGraphics.drawString(font, "§c⚡ 2. Tăng Cường +50% Sức Mạnh Tuyệt Đối:", left + 18, textY, 0xFFFF5555, false);
        textY += 12;
        guiGraphics.drawString(font, "§a• Ác ma bên trong Lãnh Địa được ban tặng thêm +50% Sát Thương (Strength III)!", left + 22, textY, 0xFF55FF55, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Gia tăng tốc độ di chuyển và sức chống chịu bền bỉ trước mọi đòn công kích.", left + 22, textY, 0xFFCCCCCC, false);

        textY += 14;
        guiGraphics.drawString(font, "§6⚡ 3. Giam Hãm Kẻ Thù & Tự Động Hoàn Trả:", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• Quái vật bên trong bị giam cầm, giật ngược vào tâm khi chạm biên giới.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f• Sau 15 giây, toàn bộ khối kết giới §etự tan vỡ hoàn trả địa hình nguyên vẹn§f.", left + 22, textY, 0xFFFFFFFF, false);
    }

    private void renderPageVelgrynd(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§c§l✦ CHƯỚC NHIỆT LONG VELGRYND (ĐỆ TAM LONG) ✦", left + 14, top + 11, 0xFFFF5555, false);

        int iconY = top + 32;
        // Hiển thị 3 biểu tượng vật phẩm
        guiGraphics.renderItem(new ItemStack(ModItems.DRAGON_SEED.get()), left + 18, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.SCORCH_DRAGON_CORE.get()), left + 38, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.VELGRYND_REVERSE_SCALE.get()), left + 58, iconY);

        if (mouseX >= left + 18 && mouseX <= left + 36 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.DRAGON_SEED.get());
        } else if (mouseX >= left + 38 && mouseX <= left + 56 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.SCORCH_DRAGON_CORE.get());
        } else if (mouseX >= left + 58 && mouseX <= left + 76 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.VELGRYND_REVERSE_SCALE.get());
        }

        guiGraphics.drawString(font, "§6[Long Chủng Tối Thượng - Ma Tố 74,350,000 EP]", left + 82, iconY + 4, 0xFFFFAA00, false);

        int textY = top + 56;
        guiGraphics.drawString(font, "§e⚡ 1. Nghi Lễ Triệu Hồi (Hạt Giống Long Chủng):", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• Người thường dùng: Tiêu hao §c90% Sinh Lực§7 hiện tại.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Chân Ma Vương dùng: Chỉ tiêu hao §a30% Sinh Lực§7 hiện tại.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f• Nghi lễ 3D: Cột sáng 50m, sấm sét 8 phương và Chước Long hạ phàm.", left + 22, textY, 0xFFFFFFFF, false);

        textY += 14;
        guiGraphics.drawString(font, "§c🔥 2. Cơ Chế Chiến Đấu & Khắc Chế:", left + 18, textY, 0xFFFF5555, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• 4 Tầng Vảy Rồng: Miễn nhiễm hoàn toàn mọi sát thương thông thường.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Phá Giáp Thần Thánh: Đòn đánh thứ 3 xuyên §c30% Máu Tối Đa§7 Giáp Thần.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§4• TỬ HUYỆT: Bị tiêu diệt ngay lập tức nếu trúng Long Tinh Bộc Viêm Bá!", left + 22, textY, 0xFFFF5555, false);

        textY += 14;
        guiGraphics.drawString(font, "§a✨ 3. Nghịch Lân Chước Nhiệt (Đồng Minh Tuyệt Đối):", left + 18, textY, 0xFF55FF55, false);
        textY += 12;
        guiGraphics.drawString(font, "§f• Hạ gục Boss rơi Nghịch Lân triệu hồi Velgrynd Đồng Minh kế thừa toàn bộ", left + 22, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, "§ftuyệt kỹ Rimuru để bảo vệ chủ nhân và quét sạch hiểm họa xung quanh.", left + 22, textY, 0xFFFFFFFF, false);
    }

    private void renderPageMagisteelPhone(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§b§l✦ THẦN THIẾT MA THOẠI (MAGISTEEL SMARTPHONE) ✦", left + 14, top + 11, 0xFF55FFFF, false);

        int iconY = top + 32;
        guiGraphics.renderItem(new ItemStack(ModItems.MAGISTEEL_PHONE.get()), left + 18, iconY);
        if (mouseX >= left + 18 && mouseX <= left + 36 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.MAGISTEEL_PHONE.get());
        }

        guiGraphics.drawString(font, "§e[Thiết Bị Quét & Phân Tích Thực Thể Toàn Năng 3D]", left + 42, iconY + 4, 0xFFFFAA00, false);

        int textY = top + 56;
        guiGraphics.drawString(font, "§6📱 1. Hướng Dẫn Sử Dụng:", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• Cầm điện thoại nhấp §a[Chuột Phải]§7 trực tiếp vào bất kỳ sinh vật nào.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Nhấp chuột phải vào không khí để quét đối tượng trong tầm ngắm 32m.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f• Nếu không ngắm vào ai, thiết bị sẽ phân tích chính người dùng.", left + 22, textY, 0xFFFFFFFF, false);

        textY += 14;
        guiGraphics.drawString(font, "§d⚡ 2. Các Tính Năng Đo Lường Cấp Cao:", left + 18, textY, 0xFFFF77FF, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §bMô hình 3D Xoay Động:§7 Trực quan hóa sinh vật trên màn hình quét.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §eMa Tố Lượng (EP):§7 Phân tích EP thực thể chính xác chuẩn Tensura.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §cChỉ Số Chiến Đấu:§7 Tấn công, giáp thủ, tốc độ và tốc độ tái sinh.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §5Kỹ Năng Độc Bản:§7 Liệt kê toàn bộ quyền năng và tuyệt chiêu sinh vật.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f• §9Bảng Kháng Tính:§f Kháng lửa, rơi, đuối nước, nổ và ma lực cảm tri.", left + 22, textY, 0xFFFFFFFF, false);
    }

    private void renderPageCuuTheQuyenHien(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§4§l✦ THIÊN NGOẠI CỨU THẾ QUYỀN HIỆN (TENGAI GUZE GONGEN) ✦", left + 14, top + 11, 0xFFFF5555, false);

        int iconY = top + 32;
        guiGraphics.renderItem(new ItemStack(ModItems.THIEN_MA_KATANA.get()), left + 18, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get()), left + 38, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.CUU_THE_QUYEN_HIEN_SPAWN_EGG.get()), left + 58, iconY);

        if (mouseX >= left + 18 && mouseX <= left + 36 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.THIEN_MA_KATANA.get());
        } else if (mouseX >= left + 38 && mouseX <= left + 56 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get());
        } else if (mouseX >= left + 58 && mouseX <= left + 76 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.CUU_THE_QUYEN_HIEN_SPAWN_EGG.get());
        }

        guiGraphics.drawString(font, "§6[Oán Linh Cấp S - Tốt Nghiệp Sinh Đệ Nhất Khóa - 1500 HP]", left + 82, iconY + 4, 0xFFFFAA00, false);

        int textY = top + 56;
        guiGraphics.drawString(font, "§e⚔ 1. Hai Trạng Thái Chiến Đấu:", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §cDạng Kiếm:§7 Cầm Thiên Ma Katana, tốc độ 0.30, chém 35 sát thương.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §6Võ Thần Tay Không (Brawler):§7 Đổi dạng khi bị Tiên Tri bẻ chiêu 2 lần.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f  Tốc độ tăng 0.35, sát thương đấm 50, giảm 50% sát thương nhận từ Player.", left + 22, textY, 0xFFFFFFFF, false);

        textY += 14;
        guiGraphics.drawString(font, "§c⚡ 2. Bộ 3 Kỹ Năng Uy Lực Mới:", left + 18, textY, 0xFFFF5555, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §bKhông Gian Trảm:§7 Vung đao chém sóng chân không tầm xa 18m hất tung kẻ địch.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §dThiên Ngoại Thần Áp:§7 Dậm chân chấn động bán kính 10m ép rạp và cấm nhảy.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §aBát Nhã Kim Cang Thể:§7 Tăng giáp, phản 30% sát thương, siêu tốc hồi 30 HP/s.", left + 22, textY, 0xFFDDDDDD, false);

        textY += 14;
        guiGraphics.drawString(font, "§a🛡 3. Tương Quan Sức Mạnh & Kháng Tính:", left + 18, textY, 0xFF55FF55, false);
        textY += 12;
        guiGraphics.drawString(font, "§f• Thua 100% trước Milim/Long Chủng; §eThắng 70% áp đảo Ác Ma Thủy Tổ§f.", left + 22, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Kháng 90% Linh Tử Băng Hoại, 70% Jacob, 100% Phép màu & Đức tin.", left + 22, textY, 0xFFDDDDDD, false);
    }

    private void renderPageCuuTheSealingAndCurse(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§4§l✦ CHÉM XUYÊN THỜI GIAN & TƯỢNG GỐM SHIGARAKI ✦", left + 14, top + 11, 0xFFFF5555, false);

        int iconY = top + 32;
        guiGraphics.renderItem(new ItemStack(ModItems.SEVERED_HEART.get()), left + 18, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.SEVERED_BRAIN.get()), left + 38, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.SHIGARAKI_TANUKI_EMPTY.get()), left + 58, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get()), left + 78, iconY);

        if (mouseX >= left + 18 && mouseX <= left + 36 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.SEVERED_HEART.get());
        } else if (mouseX >= left + 38 && mouseX <= left + 56 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.SEVERED_BRAIN.get());
        } else if (mouseX >= left + 58 && mouseX <= left + 76 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.SHIGARAKI_TANUKI_EMPTY.get());
        } else if (mouseX >= left + 78 && mouseX <= left + 96 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.SHIGARAKI_TANUKI_GONGEN.get());
        }

        guiGraphics.drawString(font, "§6[Tuyệt Kĩ Cắt Rời Không Thời Gian & Pháp Khí Phong Ấn]", left + 102, iconY + 4, 0xFFFFAA00, false);

        int textY = top + 56;
        guiGraphics.drawString(font, "§c⏳ 1. Tam Thức Luân Chuyển Đoạt Mệnh:", left + 18, textY, 0xFFFF5555, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §cThức 1 (Đoạt Tim):§7 Rơi Trái Tim mục tiêu. Án tử 20s (Dùng Phép Màu Thần Linh).", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §5Thức 2 (Đoạt Não):§7 Rơi Não. Án tử 15s, mù lòa, choáng (Dùng Bí Ẩn Đức Tin).", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§4• Thức 3 (Song Trảm): Rơi CẢ TIM LẪN NÃO! Án tử kép cực kỳ nguy hiểm!", left + 22, textY, 0xFFFF5555, false);
        textY += 11;
        guiGraphics.drawString(font, "§a• PHÁ CHIÊU: Bật Tiên Tri (Rykuzin Sinka) để thấy và đánh tan linh hồn!", left + 22, textY, 0xFF55FF55, false);

        textY += 14;
        guiGraphics.drawString(font, "§6🏮 2. Khiêu Chiến, Phong Ấn & Triệu Hồi (Tượng Shigaraki):", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §6Khiêu Chiến:§7 Cầm Tượng Rỗng §e[Shift + Chuột Phải]§7 để gọi hắn giáng lâm.", left + 22, textY, 0xFFFFAA00, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §eĐiều Kiện Phong Ấn:§7 Làm Cứu Thế suy yếu <= 20% HP (<= 300 HP) hoặc làm choáng.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7  Cầm Tượng Rỗng [Chuột Phải] vào hắn để phong ấn thành công.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f• §aTriệu Hồi & Thu Hồi:§f Chuột phải để thả ra chiến đấu với Kotodama 'Kiwamete'.", left + 22, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, "§f  Kiểm soát tối đa 90 giây. Dùng Tượng Rỗng chuột phải vào hắn để thu hồi an toàn.", left + 22, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, "§c• CẢNH BÁO: Hết 90 giây mà chưa thu hồi, hắn sẽ bạo tẩu quay sang đồ sát bạn!", left + 22, textY, 0xFFFF5555, false);
    }

    private void renderPageTaKinhDaiTang(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, "§4§l✦ TÀ KINH ĐẠI TĂNG (JANKYŌBUN DAISŌJŌ) ✦", left + 14, top + 11, 0xFFFF5555, false);

        int iconY = top + 32;
        guiGraphics.renderItem(new ItemStack(ModItems.TA_KINH_DAI_TANG_SPAWN_EGG.get()), left + 18, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.WARDING_EARPLUGS.get()), left + 38, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.BODHI_EFFIGY_EMPTY.get()), left + 58, iconY);
        guiGraphics.renderItem(new ItemStack(ModItems.BODHI_EFFIGY_MONK.get()), left + 78, iconY);

        if (mouseX >= left + 18 && mouseX <= left + 36 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.TA_KINH_DAI_TANG_SPAWN_EGG.get());
        } else if (mouseX >= left + 38 && mouseX <= left + 56 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.WARDING_EARPLUGS.get());
        } else if (mouseX >= left + 58 && mouseX <= left + 76 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.BODHI_EFFIGY_EMPTY.get());
        } else if (mouseX >= left + 78 && mouseX <= left + 96 && mouseY >= iconY && mouseY <= iconY + 18) {
            hoveredTooltipStack = new ItemStack(ModItems.BODHI_EFFIGY_MONK.get());
        }

        guiGraphics.drawString(font, "§6[Tốt Nghiệp Sinh Cấp S - Nhục Thân Bồ Tát Đọa Lạc - 1200 HP]", left + 102, iconY + 4, 0xFFFFAA00, false);

        int textY = top + 56;
        guiGraphics.drawString(font, "§c🔔 1. Tà Kinh Tuyệt Mệnh (Deadly Sutra):", left + 18, textY, 0xFFFF5555, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• Đại Tăng gõ mõ tụng kinh trong bán kính §c20m§7. Bất kỳ ai nghe thấy tiếng kinh", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7  sẽ bị rút cạn sinh lực tăng dần, mù lòa, suy yếu và §4tử vong sau 10 giây§7!", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§a• PHÒNG VỆ SỐNG CÒN:§f Bắt buộc dùng §6Bông Bịt Tai Phong Ấn§f để miễn nhiễm 100%!", left + 22, textY, 0xFF55FF55, false);
        textY += 11;
        guiGraphics.drawString(font, "§e• Warden có thính giác siêu nhạy: Nhận x2.5 sát thương và tử nạn cực nhanh!", left + 22, textY, 0xFFFFDD00, false);

        textY += 14;
        guiGraphics.drawString(font, "§d📜 2. Khắc Kinh Văn & Hoàng Tuyền Hóa Độ:", left + 18, textY, 0xFFFF77FF, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §bKhắc Kinh:§7 Bắn dải bùa khóa kỹ năng, cấm hồi máu và làm chậm trong 6s.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• §5Hoàng Tuyền:§7 Dưới 50% HP mở kết giới tro tàn thiêu đốt sinh lực kẻ địch.", left + 22, textY, 0xFFDDDDDD, false);

        textY += 14;
        guiGraphics.drawString(font, "§6🏮 3. Khiêu Chiến, Phong Ấn & Triệu Hồi (Hình Nhân Gỗ):", left + 18, textY, 0xFFFFDD00, false);
        textY += 12;
        guiGraphics.drawString(font, "§7• §6Khiêu Chiến:§7 Cầm Hình Nhân Rỗng §e[Shift + Chuột Phải]§7 để gọi ngài giáng lâm.", left + 22, textY, 0xFFFFAA00, false);
        textY += 11;
        guiGraphics.drawString(font, "§7• Phong ấn khi Đại Tăng suy yếu §c<= 20% HP§7 (<= 240 HP) hoặc bị làm choáng.", left + 22, textY, 0xFFDDDDDD, false);
        textY += 11;
        guiGraphics.drawString(font, "§f• Chuột phải để thả ra chiến đấu trong §e60 giây§f. Hết giờ sẽ bạo tẩu phản phệ!", left + 22, textY, 0xFFFFFFFF, false);
        textY += 11;
        guiGraphics.drawString(font, "§4• CẢNH BÁO: Khi thả ra, CHỦ NHÂN PHẢI ĐEO BÔNG BỊT TAI nếu không sẽ chết!", left + 22, textY, 0xFFFF5555, false);
    }

    private void renderPageAuthor(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.title").getString(), left + 14, top + 11, 0xFFFFAA00, false);

        int textY = top + 34;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.author").getString(), left + 18, textY, 0xFFFFAA00, false);

        textY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.studio").getString(), left + 18, textY, 0xFF55FFFF, false);

        textY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.university").getString(), left + 18, textY, 0xFFFFAA00, false);

        textY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.mod_name").getString(), left + 18, textY, 0xFF55FF55, false);

        textY += 13;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.license").getString(), left + 18, textY, 0xFFFF55FF, false);

        textY += 15;
        // Khung trích dẫn hoành tráng
        int quoteBoxW = BOOK_WIDTH - 36;
        guiGraphics.fill(left + 18, textY - 2, left + 18 + quoteBoxW, textY + 38, 0xFF231B30);
        drawBorder(guiGraphics, left + 18, textY - 2, quoteBoxW, 40, 0xFFD4AF37);

        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.quote1").getString(), left + 24, textY + 3, 0xFFFFFF55, false);
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.quote2").getString(), left + 24, textY + 14, 0xFFFFFF55, false);
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.quote3").getString(), left + 24, textY + 25, 0xFFFFFF55, false);

        textY += 44;
        guiGraphics.drawString(font, Component.translatable("gui.weapons.guide.p9.github").getString(), left + 18, textY, 0xFFFFAA00, false);
        textY += 11;
        guiGraphics.drawString(font, "§9§n" + GITHUB_URL, left + 18, textY, 0xFF55FFFF, false);
    }

    /**
     * Hàm vẽ lưới Bàn Chế Tạo 3x3 và ô thành phẩm trực quan
     */
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

        // Mũi tên chế tạo ->
        int arrowX = startX + 3 * 19 + 5;
        int arrowY = startY + 22;
        guiGraphics.drawString(font, "§6§l➔", arrowX, arrowY, 0xFFFFAA00, false);

        // Ô Output to hơn viền vàng
        int outX = arrowX + 16;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
