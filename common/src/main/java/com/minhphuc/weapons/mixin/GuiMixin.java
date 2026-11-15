package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.minhphuc.weapons.client.ClientPossessionHandler;
import com.minhphuc.weapons.client.ClientSkillStateHandler;
import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.entity.tensura.DemonType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 1. Ẩn hồng tâm (Crosshair) khi camera đang ở chế độ điện ảnh (Cinematic Camera)
 *    hoặc khi Camera bị tách rời (Detached).
 * 2. Hiển thị Thanh Máu Thủy Tổ gọn đẹp (1 hàng duy nhất kèm chỉ số HP/MaxHP)
 *    khi Nhập Xác Ác Ma hoặc khi máu tối đa vượt quá 40 HP (tránh hàng chục dòng tim che màn hình).
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void weapons$hideCrosshairDuringCinematic(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ClientCinematicSkillHandler.isActive()
                || ClientHellflareCinematicHandler.isActive()
                || ClientAzathothCinematicHandler.isActive()
                || ClientNihilityCinematicHandler.isActive()
                || ClientOrbitalCinematicHandler.isActive()
                || (Minecraft.getInstance().gameRenderer != null
                && Minecraft.getInstance().gameRenderer.getMainCamera() != null
                && Minecraft.getInstance().gameRenderer.getMainCamera().isDetached())) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "renderArmor", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static int weapons$clampArmorHeartRows(int heartRows, GuiGraphics guiGraphics, Player player) {
        if (ClientPossessionHandler.isPossessing(player) || (player != null && player.getMaxHealth() > 40.0F)) {
            return 1;
        }
        return heartRows;
    }

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void weapons$renderCompactDemonHealthBar(
            GuiGraphics guiGraphics,
            Player player,
            int x,
            int y,
            int height,
            int offsetHeartIndex,
            float maxHealth,
            int currentHealth,
            int displayHealth,
            int absorptionAmount,
            boolean renderHighlight,
            CallbackInfo ci
    ) {
        boolean isPossessing = ClientPossessionHandler.isPossessing(player);
        if (!isPossessing && maxHealth <= 40.0F) {
            return;
        }

        ci.cancel();

        float realHp = player != null ? player.getHealth() : currentHealth;
        float realMaxHp = Math.max(1.0F, maxHealth);
        float ratio = Mth.clamp(realHp / realMaxHp, 0.0F, 1.0F);

        int barWidth = 81;
        int barHeight = 9;

        // Màu sắc thanh máu theo hệ Ác Ma (hoặc đỏ huyết ngọc mặc định)
        int fillColor = 0xFFE62E4D;
        int borderColor = 0xFFFFAA00;
        if (isPossessing) {
            ClientPossessionHandler.PossessionData data = ClientPossessionHandler.getData(player);
            if (data != null && data.demonType() != null) {
                DemonType t = data.demonType();
                fillColor = 0xFF000000 | t.getGlowColor();
                if (t == DemonType.NOIR) {
                    fillColor = 0xFF9B30FF; // Tím Hắc Ám nổi bật trên nền tối
                } else if (t == DemonType.BLANC) {
                    fillColor = 0xFF66E5FF; // Bạch Băng
                }
            }
        }

        // Khung nền tối & viền vàng kim mảnh
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xCC100C18);
        guiGraphics.fill(x, y, x + barWidth, y + 1, borderColor);
        guiGraphics.fill(x, y + barHeight - 1, x + barWidth, y + barHeight, borderColor);
        guiGraphics.fill(x, y, x + 1, y + barHeight, borderColor);
        guiGraphics.fill(x + barWidth - 1, y, x + barWidth, y + barHeight, borderColor);

        // Phần máu hiện tại
        int filledW = Math.round((barWidth - 2) * ratio);
        if (filledW > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + filledW, y + barHeight - 1, fillColor);
            // Hiệu ứng bóng sáng phía trên thanh máu
            guiGraphics.fill(x + 1, y + 1, x + 1 + filledW, y + 3, 0x44FFFFFF);
        }

        // Nếu có Giáp Ảo (Absorption), vẽ viền ánh kim
        if (absorptionAmount > 0) {
            float absRatio = Mth.clamp(absorptionAmount / realMaxHp, 0.0F, 1.0F);
            int absW = Math.round((barWidth - 2) * absRatio);
            guiGraphics.fill(x + 1, y + barHeight - 2, x + 1 + absW, y + barHeight - 1, 0xFFFFEA00);
        }

        // Chữ hiển thị số máu chính xác ngay trên thanh máu (scale 0.75F để sắc nét và vừa khít 9px)
        String hpText = "❤ " + Math.round(realHp) + " / " + Math.round(realMaxHp);
        if (absorptionAmount > 0) {
            hpText += " (+" + absorptionAmount + ")";
        }

        var font = Minecraft.getInstance().font;
        float scale = 0.75F;
        int textWidth = Math.round(font.width(hpText) * scale);
        int textX = x + (barWidth - textWidth) / 2;
        int textY = y + 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textX, textY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, hpText, 0, 0, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void weapons$renderSkillMiniHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        com.minhphuc.weapons.client.SageHudManager.render(guiGraphics);
        if (ClientCinematicSkillHandler.isActive()
                || ClientHellflareCinematicHandler.isActive()
                || ClientAzathothCinematicHandler.isActive()
                || ClientNihilityCinematicHandler.isActive()
                || ClientOrbitalCinematicHandler.isActive()) {
            return;
        }

        var font = mc.font;
        boolean possessing = ClientPossessionHandler.isLocalPlayerPossessing();
        boolean activeLord = ClientSkillStateHandler.isTrueDemonLord()
                || ClientSkillStateHandler.isPrimordial()
                || DivineArmorItem.isWearingAnyPiece(mc.player);

        int nextHudY = 8;
        if (possessing || activeLord) {
            String skillName = ClientSkillStateHandler.getCurrentSkillName();
            if (skillName != null && !skillName.isEmpty()) {
                String line1 = (possessing ? "§5§l[NHẬP XÁC] §f" : "§6§l[KỸ NĂNG] §f") + skillName + " §a⚡0s";

                StringBuilder line2 = new StringBuilder();
                line2.append(ClientSkillStateHandler.isSafeMode() ? "§a🛡 Safe:BẬT " : "§7💥 Safe:TẮT ");
                if (ClientSkillStateHandler.hasTacticalMark()) line2.append("§b📍ChiếnThuật ");
                List<String> anchors = ClientSkillStateHandler.getAnchorLabels();
                if (!anchors.isEmpty()) {
                    int activeIdx = Math.clamp(ClientSkillStateHandler.getSelectedAnchorIndex(), 0, anchors.size() - 1);
                    line2.append("§6🏠Neo ").append(anchors.size()).append("/5 [#").append(activeIdx + 1).append("] ");
                } else if (ClientSkillStateHandler.hasAnchorMark()) {
                    line2.append("§6🏠VĩnhViễn ");
                }
                line2.append("§8| §e[Z]Đổi [G]Wheel [B]DạDày");

                int w = Math.max(font.width(line1), font.width(line2.toString())) + 12;
                int x = 8;
                int y = nextHudY;
                guiGraphics.fill(x, y, x + w, y + 25, 0xAA0E0A16);
                guiGraphics.fill(x, y, x + 2, y + 25, possessing ? 0xFFA855F7 : 0xFFD4AF37);
                guiGraphics.drawString(font, line1, x + 6, y + 4, 0xFFFFFFFF, true);
                guiGraphics.drawString(font, line2.toString(), x + 6, y + 14, 0xFFE2E8F0, false);
                nextHudY += 28;
            }
        }

        // Thanh HUD Ma Tố (Magicules / EP) + Trạng thái Tự Động & 0s Cooldown
        long mp = ClientSkillStateHandler.getMagicules();
        long maxMp = Math.max(1L, ClientSkillStateHandler.getMaxMagicules());
        long ep = ClientSkillStateHandler.getEp();
        float mpRatio = Math.clamp((float) mp / (float) maxMp, 0.0F, 1.0F);
        String mpLine = "§b✦ Ma Tố: §f"
                + com.minhphuc.weapons.content.tensura.MagiculesManager.formatCompact(mp)
                + "§7/"
                + com.minhphuc.weapons.content.tensura.MagiculesManager.formatCompact(maxMp)
                + "  §6EP: §e"
                + com.minhphuc.weapons.content.tensura.MagiculesManager.formatCompact(ep);
        String subTags = (ClientSkillStateHandler.isAzathothAutoLoot() ? "§d🧲AutoLoot:ON " : "")
                + "§8[X]Auto [V]Veldora";

        int mpW = Math.max(178, Math.max(font.width(mpLine), font.width(subTags)) + 14);
        int mpX = 8;
        int mpY = nextHudY;
        guiGraphics.fill(mpX, mpY, mpX + mpW, mpY + 26, 0xAA08101C);
        guiGraphics.fill(mpX, mpY, mpX + 2, mpY + 26, 0xFF38BDF8);
        guiGraphics.drawString(font, mpLine, mpX + 6, mpY + 3, 0xFFFFFFFF, false);

        int barX = mpX + 6;
        int barY = mpY + 13;
        int barW = mpW - 12;
        guiGraphics.fill(barX, barY, barX + barW, barY + 4, 0xFF1E293B);
        int fillW = Math.max(2, Math.round(barW * mpRatio));
        int barColor = mpRatio > 0.75F ? 0xFF38BDF8 : (mpRatio > 0.30F ? 0xFFA855F7 : 0xFFEC4899);
        guiGraphics.fill(barX, barY, barX + fillW, barY + 4, barColor);
        guiGraphics.drawString(font, subTags, mpX + 6, mpY + 18, 0xFF94A3B8, false);

        // Hiệu ứng HUD & Đổi Quyền Điều Khiển khi Trí Tuệ Vương Raphael tiếp quản cơ thể
        boolean raphaelAuto = ClientSkillStateHandler.isRaphaelAutoBattle();
        float flash = ClientSkillStateHandler.getRaphaelTakeoverFlashProgress();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (flash > 0.01F) {
            int flashAlpha = (int) (flash * 95.0F) << 24;
            int flashColor = (raphaelAuto ? 0x00E5FF : 0x22C55E) | flashAlpha;
            guiGraphics.fill(0, 0, sw, 6, flashColor);
            guiGraphics.fill(0, sh - 6, sw, sh, flashColor);
            guiGraphics.fill(0, 0, 6, sh, flashColor);
            guiGraphics.fill(sw - 6, 0, sw, sh, flashColor);
        }

        if (raphaelAuto) {
            int pulse = (int) (145 + 95 * Math.sin(System.currentTimeMillis() / 180.0D));
            int borderColor = (pulse << 24) | 0x00E5FF;
            guiGraphics.fill(0, 0, sw, 2, borderColor);
            guiGraphics.fill(0, sh - 2, sw, sh, borderColor);
            guiGraphics.fill(0, 0, 2, sh, borderColor);
            guiGraphics.fill(sw - 2, 0, sw, sh, borderColor);

            String banner = "§b§l≪ TRÍ TUỆ VƯƠNG RAPHAEL ĐANG TIẾP QUẢN CƠ THỂ — NHẤN [X] / [SHIFT] ĐỂ TRẢ QUYỀN ≫";
            int bw = font.width(banner) + 18;
            int bx = (sw - bw) / 2;
            int by = 6;
            guiGraphics.fill(bx, by, bx + bw, by + 16, 0xCC04101D);
            guiGraphics.renderOutline(bx, by, bw, 16, borderColor);
            guiGraphics.drawCenteredString(font, banner, sw / 2, by + 4, 0xFFFFFFFF);
        }
    }
}
