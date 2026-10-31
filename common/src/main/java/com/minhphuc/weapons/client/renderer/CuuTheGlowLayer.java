package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.CuuTheQuyenHienModel;
import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Lớp phát sáng emissive cho Cứu Thế Quyền Hiện:
 * - Đôi mắt rực sáng trắng và hàm răng sắc nhọn phát quang trong bóng đêm.
 * - Biểu tượng Trăng Khuyết (Crescent Moon) phát sáng uy áp.
 */
public class CuuTheGlowLayer extends RenderLayer<CuuTheQuyenHienEntity, CuuTheQuyenHienModel<CuuTheQuyenHienEntity>> {

    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/cuu_the_quyen_hien_eyes.png");

    public CuuTheGlowLayer(RenderLayerParent<CuuTheQuyenHienEntity, CuuTheQuyenHienModel<CuuTheQuyenHienEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CuuTheQuyenHienEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType renderType = RenderType.eyes(EYES_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, OverlayTexture.NO_OVERLAY);
    }
}
