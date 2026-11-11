package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.MarbasModel;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Lớp phát sáng thiên giới (Divine Emissive Layer) cho Marbas:
 * - Đôi mắt hoàng kim thần thánh phát sáng uy nghiêm trong bóng tối.
 */
public class MarbasGlowLayer extends RenderLayer<MarbasEntity, MarbasModel> {

    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/marbas_eyes.png");

    public MarbasGlowLayer(RenderLayerParent<MarbasEntity, MarbasModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       MarbasEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType renderType = RenderType.eyes(EYES_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, OverlayTexture.NO_OVERLAY);
    }
}
