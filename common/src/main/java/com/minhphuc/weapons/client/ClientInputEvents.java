package com.minhphuc.weapons.client;

import com.minhphuc.weapons.client.renderer.ClientCinematicSkillRenderer;
import com.minhphuc.weapons.init.ModKeyBindings;
import com.minhphuc.weapons.network.ModMessages;
import com.minhphuc.weapons.network.ServerboundCastBeelzebuthPacket;
import com.minhphuc.weapons.network.ServerboundCycleSkillPacket;
import com.minhphuc.weapons.network.ServerboundFireTaisuiStarPacket;
import com.minhphuc.weapons.network.ServerboundInteractGatePacket;
import com.minhphuc.weapons.network.ServerboundTengaiSwordSkillPacket;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ClientInputEvents {
    private static long lastTaisuiFireTime = 0;
    private static long lastRightClickCastTime = 0;
    private static net.minecraft.client.multiplayer.ClientLevel lastSyncedLevel = null;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.player == null || mc.level == null) {
                lastSyncedLevel = null;
                return;
            }

            if (lastSyncedLevel != mc.level) {
                lastSyncedLevel = mc.level;
                ModMessages.sendToServer(new com.minhphuc.weapons.network.ServerboundSkillActionPacket(
                        com.minhphuc.weapons.network.ServerboundSkillActionPacket.ACTION_REQUEST_SYNC, 0));
            }

            // Khi Trí Tuệ Vương Raphael tiếp quản Auto-Battle: xoay mượt góc nhìn bám theo mục tiêu thù địch gần nhất
            if (ClientSkillStateHandler.isRaphaelAutoBattle() && mc.screen == null && !mc.player.isShiftKeyDown()) {
                net.minecraft.world.entity.LivingEntity bestTarget = null;
                double bestD2 = 36.0D * 36.0D;
                for (net.minecraft.world.entity.Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(36.0D))) {
                    if (e instanceof net.minecraft.world.entity.monster.Enemy && e instanceof net.minecraft.world.entity.LivingEntity le && le.isAlive()) {
                        double d2 = mc.player.distanceToSqr(le);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestTarget = le;
                        }
                    }
                }
                if (bestTarget != null) {
                    net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
                    net.minecraft.world.phys.Vec3 aim = bestTarget.getEyePosition().subtract(eye);
                    double horiz = Math.hypot(aim.x, aim.z);
                    float targetYaw = (float) (Math.atan2(-aim.x, aim.z) * (180.0D / Math.PI));
                    float targetPitch = (float) (-Math.atan2(aim.y, Math.max(0.01D, horiz)) * (180.0D / Math.PI));
                    mc.player.setYRot(net.minecraft.util.Mth.rotLerp(0.18F, mc.player.getYRot(), targetYaw));
                    mc.player.setXRot(net.minecraft.util.Mth.lerp(0.18F, mc.player.getXRot(), net.minecraft.util.Mth.clamp(targetPitch, -65.0F, 65.0F)));
                }
            }

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
                // Nhấn Z (hoặc Shift+Z để lùi): Chuyển đổi skill cho Nguyệt Quang Thần Tế Kiếm, Chân Ma Vương, Thần Linh & Nhập Xác
                ModMessages.sendToServer(new ServerboundCycleSkillPacket(mc.player.isShiftKeyDown()));
            }

            if (ModKeyBindings.SKILL_WHEEL_KEY.consumeClick()) {
                if (mc.screen == null) {
                    boolean canOpen = ClientPossessionHandler.isLocalPlayerPossessing()
                            || ClientSkillStateHandler.isTrueDemonLord()
                            || ClientSkillStateHandler.isPrimordial()
                            || com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(mc.player)
                            || mc.player.isCreative();
                    if (canOpen) {
                        mc.setScreen(new com.minhphuc.weapons.client.gui.SkillWheelScreen());
                    } else {
                        mc.player.displayClientMessage(Component.literal("§c⚠️ Bạn cần thức tỉnh Chân Ma Vương, Thủy Tổ, mặc Giáp Thần Linh hoặc Nhập Xác Ác Ma để mở Bánh Xe Kỹ Năng!"), true);
                    }
                }
            }

            if (ModKeyBindings.TENGAI_SLASH_KEY.consumeClick()) {
                // Nhấn phím K: Thi triển Nghịch Thời Nhất Trảm
                ModMessages.sendToServer(new ServerboundTengaiSwordSkillPacket(ServerboundTengaiSwordSkillPacket.ACTION_REVERSE_SLASH));
            }

            if (ModKeyBindings.OPEN_STOMACH_KEY.consumeClick()) {
                if (mc.screen == null) {
                    // Nhấn phím B: Mở thẳng giao diện Dạ Dày Vô Hạn Azathoth
                    ModMessages.sendToServer(new com.minhphuc.weapons.network.ServerboundAzathothActionPacket(
                            com.minhphuc.weapons.network.ServerboundAzathothActionPacket.ACTION_OPEN_GUI, 0));
                }
            }

            if (ModKeyBindings.RAPHAEL_AUTO_KEY.consumeClick()) {
                if (mc.screen == null) {
                    // Nhấn phím X: Bật / Tắt Chế độ Tự Động Chiến Đấu Trí Tuệ Vương Raphael
                    ModMessages.sendToServer(new com.minhphuc.weapons.network.ServerboundSageActionPacket(
                            com.minhphuc.weapons.network.ServerboundSageActionPacket.ACTION_TOGGLE_AUTO_BATTLE));
                }
            }

            if (ModKeyBindings.SAGE_ACTION_KEY.consumeClick()) {
                if (mc.screen == null) {
                    if (mc.player.isShiftKeyDown()) {
                        ModMessages.sendToServer(new com.minhphuc.weapons.network.ServerboundAzathothActionPacket(
                                com.minhphuc.weapons.network.ServerboundAzathothActionPacket.ACTION_OPEN_GUI, 0));
                    } else if (SageHudManager.hasPendingVeldoraWarp()) {
                        SageHudManager.clearVeldoraWarp();
                        ModMessages.sendToServer(new com.minhphuc.weapons.network.ServerboundSageActionPacket(
                                com.minhphuc.weapons.network.ServerboundSageActionPacket.ACTION_WARP_VELDORA));
                    } else {
                        ModMessages.sendToServer(new com.minhphuc.weapons.network.ServerboundSageActionPacket(
                                com.minhphuc.weapons.network.ServerboundSageActionPacket.ACTION_TOGGLE_AUTO_BATTLE));
                    }
                }
            }
        });

        // Chuột phải vào không khí: Thi triển kỹ năng đang chọn (tay không hoặc khi mặc Giáp Thần Linh)
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            if (player == null || hand != InteractionHand.MAIN_HAND) return;

            if (ClientIaiHandler.isAstralActive()) {
                ClientIaiHandler.triggerClickFromInput();
                return;
            }

            // Nhìn vào Cánh Cửa Dị Giới 3D + Chuột Phải: Xoay kênh La Bàn 0–9 (hoặc Shift+Chuột Phải mở Bảng La Bàn)
            if (ClientCinematicSkillRenderer.isLookingAtDimensionalGate(player)) {
                long now = System.currentTimeMillis();
                if (now - lastRightClickCastTime >= 150) {
                    lastRightClickCastTime = now;
                    ModMessages.sendToServer(new ServerboundInteractGatePacket(player.isShiftKeyDown()));
                }
                return;
            }

            // Nếu đang giữ Shift và nhắm vào Dân Làng / Thương Nhân -> Ưu tiên mở giao diện Trao Đổi (Trade GUI), không tung skill
            if (player.isShiftKeyDown() && net.minecraft.client.Minecraft.getInstance().crosshairPickEntity instanceof net.minecraft.world.entity.npc.AbstractVillager) {
                return;
            }

            ItemStack heldStack = player.getItemInHand(hand);
            if (heldStack.is(com.minhphuc.weapons.init.ModItems.FAKE_SOUL.get()) && net.minecraft.client.Minecraft.getInstance().crosshairPickEntity instanceof net.minecraft.world.entity.LivingEntity) {
                return;
            }
            boolean isWearingDivine = com.minhphuc.weapons.content.divine.DivineArmorItem.isWearingAnyPiece(player);
            boolean isPossessing = com.minhphuc.weapons.client.ClientPossessionHandler.isLocalPlayerPossessing();

            if (heldStack.isEmpty() || isWearingDivine || isPossessing) {
                long now = System.currentTimeMillis();
                if (now - lastRightClickCastTime >= 100) {
                    lastRightClickCastTime = now;
                    ModMessages.sendToServer(new ServerboundCastBeelzebuthPacket());
                }
            }
        });

        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            if (player == null || hand != InteractionHand.MAIN_HAND) return;

            if (ClientIaiHandler.isAstralActive()) {
                ClientIaiHandler.triggerClickFromInput();
                return;
            }

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
