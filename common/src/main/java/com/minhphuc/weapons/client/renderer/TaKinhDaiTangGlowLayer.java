package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.TaKinhDaiTangModel;
import com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Glow layer cho Tà Kinh Đại Tăng (Sinsai-seikun - The Evil Sutra Monk):
 * - Đôi mắt trắng dã vô hồn không con ngươi phát sáng trong đêm (Glowing blank white hollow eyes).
 * - Chuỗi tràng hạt Juzu màu hồng phát quang rực rỡ vắt chéo qua thân (Glowing pink juzu prayer beads).
 */
public class TaKinhDaiTangGlowLayer extends RenderLayer<TaKinhDaiTangEntity, TaKinhDaiTangModel> {

    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/ta_kinh_dai_tang_glow.png");

    public TaKinhDaiTangGlowLayer(RenderLayerParent<TaKinhDaiTangEntity, TaKinhDaiTangModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       TaKinhDaiTangEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType renderType = RenderType.eyes(GLOW_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, OverlayTexture.NO_OVERLAY);
    }
}
