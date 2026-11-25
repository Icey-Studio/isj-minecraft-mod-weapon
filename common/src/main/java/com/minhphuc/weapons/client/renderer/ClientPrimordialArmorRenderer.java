package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.PrimordialDemonModel;
import com.minhphuc.weapons.content.tensura.PrimordialArmorItem;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.EnumSet;
import java.util.Set;

/**
 * Render 3D các bộ phận Trang Bị Ác Ma Thủy Tổ (Đầu 3D, Áo, Quần/Váy, Giày, Vuốt Noir phím X & Cánh Noir động khi bay)
 * trực tiếp trên người chơi khi mặc từng món hoặc đủ bộ 4 món.
 */
public class ClientPrimordialArmorRenderer {

    /**
     * Ẩn các phần cơ thể mặc định của PlayerModel đã được thay thế bởi mô hình 3D của Giáp Thủy Tổ
     * để tránh hiện tượng chồng hình (z-fighting) ở phần Đầu/Tóc/Thân.
     */
    public static void hideCoveredPlayerParts(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel) {
        if (player == null || playerModel == null) return;
        if (PrimordialArmorItem.getPieceDemonType(player, EquipmentSlot.HEAD) != null) {
            playerModel.head.visible = false;
            playerModel.hat.visible = false;
        }
        if (PrimordialArmorItem.getPieceDemonType(player, EquipmentSlot.CHEST) != null) {
            playerModel.body.visible = false;
            playerModel.jacket.visible = false;
            playerModel.rightArm.visible = false;
            playerModel.leftArm.visible = false;
            playerModel.rightSleeve.visible = false;
            playerModel.leftSleeve.visible = false;
        }
        if (PrimordialArmorItem.getPieceDemonType(player, EquipmentSlot.LEGS) != null
                || PrimordialArmorItem.getPieceDemonType(player, EquipmentSlot.FEET) != null) {
            playerModel.rightLeg.visible = false;
            playerModel.leftLeg.visible = false;
            playerModel.rightPants.visible = false;
            playerModel.leftPants.visible = false;
        }
    }

    /**
     * Render mô hình 3D của các mảnh Giáp Thủy Tổ đang mặc ở góc nhìn thứ 3 (F5) và cho người chơi xung quanh.
     */
    public static void renderEquippedPrimordialArmor(PlayerRenderer renderer, AbstractClientPlayer player,
                                                     float entityYaw, float partialTicks,
                                                     PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (player == null || !PrimordialArmorItem.isWearingAnyPiece(player)) return;

        PrimordialDemonModel<AbstractClientPlayer> model = ClientPossessionRenderer.getModel();
        if (model == null) return;

        // Tập hợp các hệ Ác Ma Thủy Tổ có mặt trên 4 khe trang bị hiện tại
        Set<DemonType> equippedTypes = EnumSet.noneOf(DemonType.class);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            DemonType dt = PrimordialArmorItem.getPieceDemonType(player, slot);
            if (dt != null) {
                equippedTypes.add(dt);
            }
        }
        if (equippedTypes.isEmpty()) return;

        boolean noirClawsActive = PrimordialArmorItem.isNoirClawsActive(player);

        float bodyYaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;
        float headPitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        float ageInTicks = player.tickCount + partialTicks;

        float limbSwing = 0.0F;
        float limbSwingAmount = 0.0F;
        if (!player.isPassenger() && player.isAlive()) {
            limbSwingAmount = player.walkAnimation.speed(partialTicks);
            limbSwing = player.walkAnimation.position(partialTicks);
            if (limbSwingAmount > 1.0F) {
                limbSwingAmount = 1.0F;
            }
        }

        for (DemonType type : equippedTypes) {
            boolean wearHead = PrimordialArmorItem.isWearingPiece(player, EquipmentSlot.HEAD, type);
            boolean wearChest = PrimordialArmorItem.isWearingPiece(player, EquipmentSlot.CHEST, type);
            boolean wearLegs = PrimordialArmorItem.isWearingPiece(player, EquipmentSlot.LEGS, type);
            boolean wearFeet = PrimordialArmorItem.isWearingPiece(player, EquipmentSlot.FEET, type);

            ResourceLocation texture = type.getBodyTextureLocation();

            poseStack.pushPose();

            model.attackTime = player.getAttackAnim(partialTicks);
            model.riding = player.isPassenger();
            model.young = player.isBaby();
            model.crouching = player.isCrouching();

            if (player.isCrouching()) {
                poseStack.translate(0.0D, -0.125D, 0.0D);
            }

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.scale(0.9375F, 0.9375F, 0.9375F);
            poseStack.translate(0.0F, -1.501F, 0.0F);

            model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
            model.setupAnimForArmor(player, type, wearHead, wearChest, wearLegs, wearFeet,
                    noirClawsActive, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
            model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

            // Nếu đang đội Đầu 3D của Thủy Tổ: Render thêm lớp mắt phát sáng đặc trưng
            if (wearHead) {
                ResourceLocation eyesTexture = type.getEyesTextureLocation();
                if (eyesTexture != null) {
                    VertexConsumer eyesConsumer = buffer.getBuffer(RenderType.eyes(eyesTexture));
                    model.head.render(poseStack, eyesConsumer, 15728640, OverlayTexture.NO_OVERLAY);
                }
            }

            poseStack.popPose();
        }
    }

    /**
     * Render bàn tay góc nhìn thứ nhất (First-Person Hand) khi mặc Áo của Thủy Tổ
     * (Đặc biệt hiển thị Móng Vuốt 3D của Diablo khi người chơi mặc bộ Noir và bấm phím X!).
     */
    public static boolean renderFirstPersonArmorHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                     AbstractClientPlayer player, boolean isRightHand) {
        DemonType chestType = PrimordialArmorItem.getPieceDemonType(player, EquipmentSlot.CHEST);
        if (chestType == null) return false;

        PrimordialDemonModel<AbstractClientPlayer> model = ClientPossessionRenderer.getModel();
        if (model == null) return false;

        ResourceLocation texture = chestType.getBodyTextureLocation();
        boolean isNoir = (chestType == DemonType.NOIR);
        boolean noirClaws = isNoir && PrimordialArmorItem.isNoirClawsActive(player);
        boolean isBlanc = (chestType == DemonType.BLANC);
        boolean isViolet = (chestType == DemonType.VIOLET);
        boolean isBleu = (chestType == DemonType.BLEU);

        model.noirClawsRight.visible = noirClaws;
        model.noirClawsLeft.visible = noirClaws;
        model.blancPuffRight.visible = isBlanc;
        model.blancPuffLeft.visible = isBlanc;
        model.violetSleeveRight.visible = isViolet;
        model.violetSleeveLeft.visible = isViolet;
        model.bleuFrillRight.visible = isBleu;
        model.bleuFrillLeft.visible = isBleu;

        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;

        ModelPart armPart = isRightHand ? model.rightArm : model.leftArm;
        armPart.visible = true;
        armPart.xRot = 0.0F;
        armPart.yRot = 0.0F;
        armPart.zRot = 0.0F;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        armPart.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        return true;
    }
}
