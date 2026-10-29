package com.minhphuc.weapons.client;

import com.minhphuc.weapons.init.ModKeyBindings;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundCastBeelzebuthPacket;
import com.minhphuc.weapons.network.ServerboundCycleSkillPacket;
import com.minhphuc.weapons.network.ServerboundFireTaisuiStarPacket;
import com.minhphuc.weapons.network.ServerboundTengaiSwordSkillPacket;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ClientInputEvents {
    private static long lastTaisuiFireTime = 0;
    private static long lastRightClickCastTime = 0;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.player == null) return;

            if (ModKeyBindings.SELECT_BULLET_KEY.consumeClick()) {
                if (mc.screen == null) {
                    ItemStack mainItem = mc.player.getMainHandItem();
                    ItemStack offItem = mc.player.getOffhandItem();
                    boolean holdingGun = (mainItem.getItem() instanceof com.minhphuc.weapons.content.tensura.GoldenGunItem)
                            || (offItem.getItem() instanceof com.minhphuc.weapons.content.tensura.GoldenGunItem);

                    if (holdingGun) {
                        if (com.minhphuc.weapons.content.tensura.GoldenGunItem.hasAnyBullet(mc.player)) {
                            mc.setScreen(new com.minhphuc.weapons.client.gui.GoldenGunBulletSelectScreen());
                        } else {
                            mc.player.displayClientMessage(
                                    Component.literal("§c§l[SÚNG HOÀNG KIM] §eBạn cần có ít nhất 1 viên ma đạn trên tay hoặc trong túi đồ để mở giao diện chọn đạn! ⚠️"),
                                    true
                            );
                            mc.player.playSound(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.2F, 1.6F);
                        }
                    }
                }
            }

            if (ModKeyBindings.BEELZEBUTH_KEY.consumeClick()) {
                // Nhấn phím Z: Chuyển đổi skill cho Nguyệt Quang Thần Tế Kiếm, Chân Ma Vương và Thần Linh Vũ Trang
                ModMessages.sendToServer(new ServerboundCycleSkillPacket());
            }

            if (ModKeyBindings.TENGAI_SLASH_KEY.consumeClick()) {
                // Nhấn phím X: Thi triển Nghịch Thời Nhất Trảm
                ModMessages.sendToServer(new ServerboundTengaiSwordSkillPacket(ServerboundTengaiSwordSkillPacket.ACTION_REVERSE_SLASH));
            }
        });

        // Chuột phải vào không khí: Thi triển kỹ năng đang chọn (tay không hoặc khi mặc Giáp Thần Linh)
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            if (player == null || hand != InteractionHand.MAIN_HAND) return;

            ItemStack heldStack = player.getItemInHand(hand);
            boolean isWearingDivine = com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player);

            if (heldStack.isEmpty() || isWearingDivine) {
                long now = System.currentTimeMillis();
                if (now - lastRightClickCastTime >= 100) {
                    lastRightClickCastTime = now;
                    ModMessages.sendToServer(new ServerboundCastBeelzebuthPacket());
                }
            }
        });

        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            if (player == null || hand != InteractionHand.MAIN_HAND) return;

            // Bắn Tinh Tú Tuyệt Diệt khi nhấn Chuột Trái (Chỉ gửi nếu có trạng thái Thái Tuế Tinh Tú)
            if (ClientTaisuiHandler.hasTaisui(player.getUUID())) {
                long now = System.currentTimeMillis();
                if (now - lastTaisuiFireTime >= 100) { // Cooldown nhẹ 100ms tránh spam mạng
                    lastTaisuiFireTime = now;
                    ModMessages.sendToServer(new ServerboundFireTaisuiStarPacket());
                }
            }
        });
    }
}
