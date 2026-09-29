package com.minhphuc.weapons.client.gui;

import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MagisteelPhoneScreen extends Screen {

    private final LivingEntity target;
    private static final int PHONE_WIDTH = 420;
    private static final int PHONE_HEIGHT = 240;
    private static final DecimalFormat EP_FORMAT = new DecimalFormat("#,###");

    private int activeTab = 0; // 0: Overview & Stats, 1: Skills & Powers, 2: Resistances & Perception
    private Button tabStatsBtn;
    private Button tabSkillsBtn;
    private Button tabResistBtn;
    private Button closeBtn;

    public MagisteelPhoneScreen(LivingEntity target) {
        super(Component.literal("Điện Thoại Ma Đạo Thần Thiết - Tensura Analyzer"));
        this.target = (target != null) ? target : Minecraft.getInstance().player;
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - PHONE_WIDTH) / 2;
        int top = (this.height - PHONE_HEIGHT) / 2;

        // Tabs selection on right panel
        int btnY = top + 10;
        tabStatsBtn = Button.builder(Component.literal("§6✦ Chỉ Số"), b -> activeTab = 0)
                .bounds(left + 165, btnY, 75, 18).build();
        tabSkillsBtn = Button.builder(Component.literal("§d⚡ Kỹ Năng"), b -> activeTab = 1)
                .bounds(left + 245, btnY, 75, 18).build();
        tabResistBtn = Button.builder(Component.literal("§b🛡 Kháng Tính"), b -> activeTab = 2)
                .bounds(left + 325, btnY, 80, 18).build();

        closeBtn = Button.builder(Component.literal("§c✕ Đóng"), b -> this.onClose())
                .bounds(left + PHONE_WIDTH - 65, top + PHONE_HEIGHT - 22, 55, 16).build();

        this.addRenderableWidget(tabStatsBtn);
        this.addRenderableWidget(tabSkillsBtn);
        this.addRenderableWidget(tabResistBtn);
        this.addRenderableWidget(closeBtn);

        // Sound on open
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.8F);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = (this.width - PHONE_WIDTH) / 2;
        int top = (this.height - PHONE_HEIGHT) / 2;

        // 1. Vỏ điện thoại Thần Thiết hiện đại (Sleek Smartphone Frame)
        guiGraphics.fill(left - 4, top - 4, left + PHONE_WIDTH + 4, top + PHONE_HEIGHT + 4, 0xFF0D1117); // Khung kim loại titan đen
        guiGraphics.fill(left - 2, top - 2, left + PHONE_WIDTH + 2, top + PHONE_HEIGHT + 2, 0xFF1F6FEB); // Viền LED neon xanh Tensura
        guiGraphics.fill(left, top, left + PHONE_WIDTH, top + PHONE_HEIGHT, 0xFF0B0E14); // Mặt kính cường lực đen mờ

        // 2. Cột bên trái: Màn hình 3D thực thể (3D Mob Viewport)
        int viewLeft = left + 10;
        int viewTop = top + 10;
        int viewWidth = 145;
        int viewHeight = 220;

        guiGraphics.fill(viewLeft, viewTop, viewLeft + viewWidth, viewTop + viewHeight, 0xFF161B22);
        guiGraphics.fill(viewLeft + 1, viewTop + 1, viewLeft + viewWidth - 1, viewTop + viewHeight - 1, 0xFF0D1117);
        guiGraphics.fill(viewLeft, viewTop, viewLeft + viewWidth, viewTop + 16, 0xFF21262D);
        guiGraphics.drawString(font, "§b§l✦ QUÉT THỰC THỂ 3D", viewLeft + 6, viewTop + 4, 0xFFFFFFFF, false);

        // Render 3D Entity
        if (target != null) {
            int entityBoxX1 = viewLeft + 5;
            int entityBoxY1 = viewTop + 22;
            int entityBoxX2 = viewLeft + viewWidth - 5;
            int entityBoxY2 = viewTop + 145;

            // Nền quét không gian số
            guiGraphics.fill(entityBoxX1, entityBoxY1, entityBoxX2, entityBoxY2, 0xFF0A0E14);
            guiGraphics.fill(entityBoxX1 + 1, entityBoxY1 + 1, entityBoxX2 - 1, entityBoxY2 - 1, 0xFF06090F);

            // Vẽ lưới tọa độ quét
            guiGraphics.fill(entityBoxX1 + 10, entityBoxY1 + 50, entityBoxX2 - 10, entityBoxY1 + 51, 0x3300FFFF);
            guiGraphics.fill(entityBoxX1 + 10, entityBoxY1 + 90, entityBoxX2 - 10, entityBoxY1 + 91, 0x3300FFFF);

            // Tính toán tỉ lệ scale phù hợp với kích thước mob
            float boundingHeight = Math.max(0.8F, target.getBbHeight());
            int renderScale = (int) (55.0F / boundingHeight);
            if (target instanceof VelgryndEntity) renderScale = 45;
            if (renderScale < 15) renderScale = 15;
            if (renderScale > 65) renderScale = 65;

            try {
                int entityCenterX = (entityBoxX1 + entityBoxX2) / 2;
                int entityCenterY = entityBoxY2 - 10;
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics,
                        entityBoxX1, entityBoxY1, entityBoxX2, entityBoxY2,
                        renderScale,
                        0.0625F,
                        (float) mouseX, (float) mouseY,
                        target
                );
            } catch (Exception ignored) {}

            // Thanh Máu (HP Bar) bên dưới mob
            int hpBarY = viewTop + 152;
            float hp = target.getHealth();
            float maxHp = target.getMaxHealth();
            float hpRatio = Math.max(0.0F, Math.min(1.0F, hp / maxHp));

            guiGraphics.fill(viewLeft + 6, hpBarY, viewLeft + viewWidth - 6, hpBarY + 8, 0xFF21262D);
            int barWidth = (int) ((viewWidth - 14) * hpRatio);
            int barColor = (hpRatio > 0.5F) ? 0xFF00FF88 : ((hpRatio > 0.25F) ? 0xFFFFBB00 : 0xFFFF3333);
            if (barWidth > 0) {
                guiGraphics.fill(viewLeft + 7, hpBarY + 1, viewLeft + 7 + barWidth, hpBarY + 7, barColor);
            }
            String hpText = String.format("§fHP: §a%.0f§7/§c%.0f", hp, maxHp);
            guiGraphics.drawString(font, hpText, viewLeft + 8, hpBarY + 11, 0xFFFFFFFF, false);

            // Ma Tố Lượng (EP)
            long ep = calculateEP(target);
            String epText = "§dEP: §e" + EP_FORMAT.format(ep);
            guiGraphics.drawString(font, epText, viewLeft + 8, hpBarY + 22, 0xFFFFAA00, false);

            // Cảnh báo đe dọa (Threat Level)
            String threatLevel = getThreatLevel(ep);
            guiGraphics.drawString(font, "§7Cấp độ: " + threatLevel, viewLeft + 8, hpBarY + 33, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, "§8ID: §7" + target.getType().getDescription().getString(), viewLeft + 8, hpBarY + 44, 0xFF888888, false);
        }

        // 3. Cột bên phải: Bảng thông tin chi tiết theo Tab
        int rightX = left + 165;
        int rightTop = top + 34;
        int rightWidth = PHONE_WIDTH - 175;
        int rightHeight = PHONE_HEIGHT - 62;

        guiGraphics.fill(rightX, rightTop, rightX + rightWidth, rightTop + rightHeight, 0xFF161B22);
        guiGraphics.fill(rightX + 1, rightTop + 1, rightX + rightWidth - 1, rightTop + rightHeight - 1, 0xFF0D1117);

        // Tiêu đề tên thực thể
        String targetName = getEntityFormattedName(target);
        guiGraphics.drawString(font, targetName, rightX + 8, rightTop + 8, 0xFFFFFFFF, false);
        String subTitle = getEntityClassification(target);
        guiGraphics.drawString(font, subTitle, rightX + 8, rightTop + 20, 0xFF888888, false);
        guiGraphics.fill(rightX + 8, rightTop + 31, rightX + rightWidth - 8, rightTop + 32, 0xFF30363D);

        // Nội dung theo Tab
        int contentY = rightTop + 38;
        switch (activeTab) {
            case 0 -> renderStatsTab(guiGraphics, rightX + 8, contentY, rightWidth - 16);
            case 1 -> renderSkillsTab(guiGraphics, rightX + 8, contentY, rightWidth - 16);
            case 2 -> renderResistancesTab(guiGraphics, rightX + 8, contentY, rightWidth - 16);
        }

        // Footer pin & thời gian ảo
        guiGraphics.drawString(font, "§a⚡ Pin 99% §7| §bVệ Tinh Tensura §7| §eVer 3.0", left + 14, top + PHONE_HEIGHT - 18, 0xFF888888, false);
    }

    private void renderStatsTab(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.drawString(font, "§6§l[BẢNG THUỘC TÍNH CHIẾN ĐẤU]", x, y, 0xFFFFAA00, false);
        y += 14;

        double attack = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double armor = target.getAttributeValue(Attributes.ARMOR);
        double armorToughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        guiGraphics.drawString(font, "§c🗡 Sức Tấn Công (Attack): §f" + String.format("%.1f", attack), x, y, 0xFFFFFFFF, false);
        y += 12;
        guiGraphics.drawString(font, "§9🛡 Giáp Phòng Ngự (Armor): §f" + String.format("%.1f", armor) + " §7(Kháng: " + String.format("%.1f", armorToughness) + ")", x, y, 0xFFFFFFFF, false);
        y += 12;
        guiGraphics.drawString(font, "§e⚡ Tốc Độ Di Chuyển: §f" + String.format("%.2f", speed * 10.0D) + " Mach/s", x, y, 0xFFFFFFFF, false);
        y += 12;

        // Tốc độ hồi phục
        String regenInfo = "§7Bình thường (0.1 HP/s)";
        if (target instanceof VelgryndEntity) {
            regenInfo = "§6Long Chủng Siêu Tốc (+50 ~ 100 HP/s)";
        } else if (target instanceof PrimordialDemonEntity demon) {
            if (demon.hasPhysicalBody() && demon.isNamed()) {
                regenInfo = "§5Ma Thần Tối Thượng (+6% HP/s)";
            } else if (demon.hasPhysicalBody() || demon.isNamed()) {
                regenInfo = "§dTiến Hóa Cấp Cao (+4% HP/s)";
            } else {
                regenInfo = "§cÁc Ma Thủy Tổ (+2% HP/s)";
            }
        }
        guiGraphics.drawString(font, "§a💚 Tốc Độ Hồi Phục: " + regenInfo, x, y, 0xFFFFFFFF, false);
        y += 14;

        // Phán định quan hệ
        String relation = "§cThù Địch (Hostile)";
        if (target instanceof Player) {
            relation = "§bBản Thân / Người Chơi";
        } else if (target instanceof VelgryndEntity velgrynd && velgrynd.isAllied()) {
            relation = "§6Đồng Minh Tối Thượng (Allied)";
        } else if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            relation = "§aĐã Ký Khế Ước / Đồng Minh";
        }
        guiGraphics.drawString(font, "§e✦ Quan Hệ Đối Tượng: " + relation, x, y, 0xFFFFFFFF, false);
        y += 12;

        if (target instanceof PrimordialDemonEntity demon && demon.isTame() && demon.getOwner() != null) {
            guiGraphics.drawString(font, "§7  Chủ nhân: §f" + demon.getOwner().getName().getString(), x, y, 0xFFDDDDDD, false);
        } else if (target instanceof VelgryndEntity velgrynd && velgrynd.isAllied()) {
            guiGraphics.drawString(font, "§7  Bảo hộ: §fChủ nhân Nghịch Lân Long Chủng", x, y, 0xFFDDDDDD, false);
        }
    }

    private void renderSkillsTab(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.drawString(font, "§d§l[QUYỀN NĂNG & KỸ NĂNG ĐỘC BẢN]", x, y, 0xFFFF77FF, false);
        y += 14;

        int maxBottom = (this.height - PHONE_HEIGHT) / 2 + PHONE_HEIGHT - 38;
        List<String> skills = getEntitySkills(target);
        for (String skill : skills) {
            guiGraphics.drawString(font, "§f• " + skill, x, y, 0xFFFFFFFF, false);
            y += 12;
            if (y > maxBottom) break;
        }
    }

    private void renderResistancesTab(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.drawString(font, "§b§l[KHÁNG TÍNH & THỊ THỰC]", x, y, 0xFF55FFFF, false);
        y += 14;

        boolean fireImmune = target.fireImmune();
        boolean fallImmune = (target instanceof VelgryndEntity || target instanceof PrimordialDemonEntity);
        boolean drownImmune = (target instanceof VelgryndEntity || target instanceof PrimordialDemonEntity || target.canBreatheUnderwater());

        guiGraphics.drawString(font, "§6🔥 Kháng Lửa/Tro Bụi: " + (fireImmune ? "§aMiễn Nhiễm Tuyệt Đối" : "§cKhông Kháng"), x, y, 0xFFFFFFFF, false);
        y += 12;
        guiGraphics.drawString(font, "§f🪂 Kháng Rơi Tự Do: " + (fallImmune ? "§aMiễn Nhiễm (Bay Lơ Lửng)" : "§7Chịu Sát Thương"), x, y, 0xFFFFFFFF, false);
        y += 12;
        guiGraphics.drawString(font, "§9💧 Kháng Đuối Nước: " + (drownImmune ? "§aMiễn Nhiễm (Thở Dưới Nước)" : "§7Bị Ngạt Nước"), x, y, 0xFFFFFFFF, false);
        y += 12;

        if (target instanceof VelgryndEntity) {
            guiGraphics.drawString(font, "§c🛡 Vảy Rồng Tuyệt Đối: §f4 Tầng Khiên Chặn Mọi Đòn Thường", x, y, 0xFFFFFFFF, false);
            y += 12;
            guiGraphics.drawString(font, "§4☠ Điểm Yếu: §eBị Diệt Ngay Bởi Long Tinh Bộc Viêm Bá", x, y, 0xFFFFAA00, false);
            y += 12;
        } else if (target instanceof PrimordialDemonEntity) {
            guiGraphics.drawString(font, "§5🌀 Linh Tử Kháng Tính: §fMiễn nhiễm Nguyền Rủa & Độc", x, y, 0xFFFFFFFF, false);
            y += 12;
            guiGraphics.drawString(font, "§e👁 Ma Lực Cảm Tri: §aPhát hiện mọi kẻ tàng hình 360°", x, y, 0xFFFFFFFF, false);
            y += 12;
        } else {
            guiGraphics.drawString(font, "§7⚡ Kháng Ma Pháp: §fCơ bản theo giáp thủ", x, y, 0xFFFFFFFF, false);
            y += 12;
            guiGraphics.drawString(font, "§e👁 Cảm Quan: §7Tầm nhìn sinh học thông thường", x, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }

    private String getEntityFormattedName(LivingEntity entity) {
        if (entity instanceof VelgryndEntity velgrynd) {
            return velgrynd.isAllied() ? "§6§lCHƯỚC NHIỆT LONG VELGRYND (ĐỒNG MINH)" : "§c§lCHƯỚC NHIỆT LONG VELGRYND (BOSS)";
        }
        if (entity instanceof PrimordialDemonEntity demon) {
            return "§d§l" + demon.getEffectiveDemonName().toUpperCase();
        }
        if (entity instanceof Player player) {
            return "§b§l" + player.getName().getString() + " §7[Nhà Du Hành]";
        }
        return "§f§l" + entity.getDisplayName().getString();
    }

    private String getEntityClassification(LivingEntity entity) {
        if (entity instanceof VelgryndEntity) {
            return "§eLong Chủng Tối Thượng (True Dragon - Đệ Tam Long)";
        }
        if (entity instanceof PrimordialDemonEntity demon) {
            String form = "Thể Linh Hồn";
            if (demon.hasPhysicalBody() && demon.isNamed()) form = "Ma Thần Tối Thượng [Thức Tỉnh]";
            else if (demon.hasPhysicalBody()) form = "Hóa Thân Thể Xác";
            else if (demon.isNamed()) form = "Danh Xưng Ban Tặng";
            return "§5Ác Ma Thủy Tổ §7- " + demon.getDemonType().getColorName() + " (" + form + ")";
        }
        if (entity instanceof Player) {
            return "§3Sinh Mệnh Thứ Nguyên / Người Kế Thừa Thần Khí";
        }
        if (entity instanceof WitherBoss || entity instanceof EnderDragon || entity instanceof Warden) {
            return "§4Cổ Thần Ma Thú / Boss Cấp Thế Giới";
        }
        return "§7Sinh Vật Tự Nhiên (Native Entity)";
    }

    private long calculateEP(LivingEntity entity) {
        if (entity instanceof VelgryndEntity) {
            return 74350000L;
        }
        if (entity instanceof PrimordialDemonEntity demon) {
            DemonType type = demon.getDemonType();
            boolean body = demon.hasPhysicalBody();
            boolean named = demon.isNamed();

            if (type == DemonType.ROUGE) {
                return (body && named) ? 40000000L : 2800000L;
            } else if (type == DemonType.NOIR) {
                return (body && named) ? 6666666L : 2500000L;
            } else if (type == DemonType.BLANC || type == DemonType.JAUNE || type == DemonType.VIOLET) {
                return (body && named) ? 10500000L : 2000000L;
            } else {
                return (body && named) ? 8500000L : 1500000L;
            }
        }
        if (entity instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity) {
            return 12500000L;
        }
        if (entity instanceof Player) {
            return 1250000L; // Player with advanced armaments
        }
        if (entity instanceof EnderDragon) return 1200000L;
        if (entity instanceof Warden) return 980000L;
        if (entity instanceof WitherBoss) return 650000L;

        // Generic mob calculation
        long base = (long) (entity.getMaxHealth() * 300);
        base += (long) (entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 200);
        base += (long) (entity.getAttributeValue(Attributes.ARMOR) * 1000);
        return Math.max(100L, base);
    }

    private String getThreatLevel(long ep) {
        if (ep >= 50000000L) return "§4§lTHẢM HỌA DIỆT THẾ (Cấp Catastrophe)";
        if (ep >= 10000000L) return "§c§lMA THẦN TỐI CAO (Cấp Disaster)";
        if (ep >= 2000000L) return "§6MA VƯƠNG THỨC TỈNH (Cấp Calamity)";
        if (ep >= 500000L) return "§eNGUY HIỂM CAO (Cấp Hazard)";
        return "§aAN TOÀN / CƠ BẢN";
    }

    private List<String> getEntitySkills(LivingEntity entity) {
        List<String> list = new ArrayList<>();
        if (entity instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity kubo) {
            list.add("§0§lHắc Nhật Quang Trụ: §5Cột sáng giống Milim 60m (Không phá khối)");
            list.add("§4Xuyên Giáp Thần Thoại: §cXuyên qua giáp Thần (Trừ Trận Đồ Cưỡng Chế)");
            list.add("§6Tập Tính Săn Mồi: §eBay cao xà xuống đánh / Dưới 30% bỏ chạy");
            if (kubo.hasAbsorbedSoul()) {
                list.add("§d§lDung Hợp Linh Hồn 70%: §e" + kubo.getAbsorbedSkillName());
                list.add("§aCường Hóa Thuộc Tính: §f+70% Máu & Kháng từ " + kubo.getAbsorbedEntityName());
            } else {
                list.add("§7Bản Năng Thôn Phệ: §fĐang tìm kiếm linh hồn rơi rớt để nuốt chửng");
            }
            return list;
        }
        if (entity instanceof VelgryndEntity) {
            list.add("§cThao Túng Thời Không: §eĐóng băng 3s & chém thứ nguyên");
            list.add("§6Gia Tốc Cardinal: §eMach 5 Hỏa Long đục thủng địa hình");
            list.add("§eChước Liệt Tiệt Đoán: §f12 lát cắt hỏa tiệt phi đao");
            list.add("§dTồn Tại Song Song: §bPhân thân ép về 1v1 tuyệt đối");
            list.add("§4Xuyên Giáp Thần Thánh: §cĐòn 3 xuyên 30% Máu giáp Thần");
            return list;
        }
        if (entity instanceof PrimordialDemonEntity demon) {
            DemonType type = demon.getDemonType();
            list.add("§5Bành Trướng Lãnh Địa: §dTạo kết giới +50% sức mạnh");
            list.add("§cChiêu Độc Bản 1: §fMa Pháo Thủy Tổ / Chùm Tia");
            list.add("§6Chiêu Độc Bản 2: §fTốc Biến Áp Sát & Trảm Kích");
            list.add("§eChiêu Độc Bản 3: §f" + (type == DemonType.JAUNE ? "Hoàng Kim Súng Đạn Hạch" : "Cột Sáng Hủy Diệt 60m"));
            list.add("§4Linh Tử Băng Hoại: §cPhá hủy cấu trúc linh hồn đối thủ");
            return list;
        }
        if (entity instanceof Player) {
            list.add("§bSức Mạnh Kế Thừa: §fSử dụng vũ khí & kỹ năng Mod");
            list.add("§6Khả Năng Chế Tạo: §fTạo lập trang bị thần thánh");
            list.add("§aBất Diệt Linh Hồn: §fKhả năng hồi sinh vô hạn");
            return list;
        }
        if (entity instanceof Warden) {
            list.add("§3Sóng Siêu Âm Sonic: §fTia xuyên tường tầm xa");
            list.add("§8Đánh Cận Thân Cực Đại: §fSát thương cận chiến khủng");
            return list;
        }
        if (entity instanceof WitherBoss) {
            list.add("§8Đầu Lâu Wither: §fBắn đạn gây hiệu ứng khô héo");
            list.add("§7Giáp Wither: §fKháng hoàn toàn cung tên khi máu dưới 50%");
            return list;
        }
        list.add("§7Tấn Công Sinh Học: §fĐòn cào cắn cơ bản");
        list.add("§7Tập Tính Bầy Đàn: §fHỗ trợ đồng loại khi bị tấn công");
        return list;
    }
}
