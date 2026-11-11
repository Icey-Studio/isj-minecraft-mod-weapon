package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.minhphuc.weapons.client.ClientPossessionHandler;
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
}
