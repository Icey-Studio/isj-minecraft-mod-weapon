package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.ClientPossessionHandler;
import com.minhphuc.weapons.client.model.PrimordialDemonModel;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ClientPossessionRenderer {

    private static PrimordialDemonModel<AbstractClientPlayer> possessedModel = null;

    public static PrimordialDemonModel<AbstractClientPlayer> getModel() {
        if (possessedModel == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getEntityModels() != null) {
                possessedModel = new PrimordialDemonModel<>(mc.getEntityModels().bakeLayer(PrimordialDemonModel.LAYER_LOCATION));
            }
        }
        return possessedModel;
    }

    /**
     * Render thân xác 3D của Ác Ma Thủy Tổ khi người chơi ở góc nhìn F5
     */
    public static void renderPossessedDemon(PlayerRenderer renderer, AbstractClientPlayer player, float entityYaw, float partialTicks,
                                            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ClientPossessionHandler.PossessionData data = ClientPossessionHandler.getData(player);
        if (data == null) return;

        PrimordialDemonModel<AbstractClientPlayer> model = getModel();
        if (model == null) return;

        DemonType type = data.demonType() != null ? data.demonType() : DemonType.NOIR;
        boolean hasBody = data.hasBody();
        boolean isWinged = data.isWinged();

        ResourceLocation texture = hasBody ? type.getBodyTextureLocation() : type.getTextureLocation();

        poseStack.pushPose();

        // 1. Chuẩn bị trạng thái hoạt ảnh tương tự LivingEntityRenderer.render
        model.attackTime = player.getAttackAnim(partialTicks);
        model.riding = player.isPassenger();
        model.young = player.isBaby();
        model.crouching = player.isCrouching();

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

        if (player.isCrouching()) {
            poseStack.translate(0.0D, -0.125D, 0.0D);
        }

        // 2. Xoay hướng cơ thể và lật trục Y chuẩn của Minecraft EntityModel (sửa triệt để lỗi lộn ngược đầu)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        float baseScale = type.getScale();
        if (hasBody) baseScale *= 1.1F;
        poseStack.scale(baseScale, baseScale, baseScale);

        poseStack.translate(0.0F, -1.501F, 0.0F);

        // 3. Tính toán hoạt ảnh bước đi, vung tay cận chiến, vỗ cánh & tư thế kết ấn kỹ năng dài (DeathBlessingPoseMixin)
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        model.setupAnimForPossessed(type, hasBody, isWinged, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 4. Render mô hình Ác Ma Thủy Tổ
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        // 5. Render lớp mắt phát sáng (DemonEyesLayer)
        ResourceLocation eyesTexture = type.getEyesTextureLocation();
        if (eyesTexture != null) {
            VertexConsumer eyesConsumer = buffer.getBuffer(RenderType.eyes(eyesTexture));
            model.renderToBuffer(poseStack, eyesConsumer, 15728640, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }

    /**
     * Render bàn tay góc nhìn thứ nhất (First-Person Hand) với đúng Skin của Ác Ma đang nhập xác
     * và bộ Móng Vuốt 3D sắc bén riêng cho Noir (Diablo)
     */
    public static void renderFirstPersonDemonHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, boolean isRightHand) {
        ClientPossessionHandler.PossessionData data = ClientPossessionHandler.getData(player);
        if (data == null) return;

        PrimordialDemonModel<AbstractClientPlayer> model = getModel();
        if (model == null) return;

        DemonType type = data.demonType() != null ? data.demonType() : DemonType.NOIR;
        boolean hasBody = data.hasBody();
        ResourceLocation texture = hasBody ? type.getBodyTextureLocation() : type.getTextureLocation();

        // Thiết lập hiển thị phụ kiện cánh tay theo đúng hệ màu Ác Ma (riêng Noir bật móng vuốt 3D)
        boolean isNoir = (type == DemonType.NOIR);
        boolean isBlanc = (type == DemonType.BLANC);
        boolean isViolet = (type == DemonType.VIOLET);
        boolean isBleu = (type == DemonType.BLEU);

        model.noirClawsRight.visible = isNoir;
        model.noirClawsLeft.visible = isNoir;
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
    }

    public static ResourceLocation getPossessedTexture(AbstractClientPlayer player) {
        ClientPossessionHandler.PossessionData data = ClientPossessionHandler.getData(player);
        if (data != null && data.demonType() != null) {
            return data.hasBody() ? data.demonType().getBodyTextureLocation() : data.demonType().getTextureLocation();
        }
        return null;
    }
}
