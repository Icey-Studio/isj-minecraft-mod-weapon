package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class VelgryndGlowLayer extends RenderLayer<VelgryndEntity, EntityModel<VelgryndEntity>> {

    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/velgrynd_eyes.png");

    public VelgryndGlowLayer(RenderLayerParent<VelgryndEntity, EntityModel<VelgryndEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       VelgryndEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isDragonForm()) return;
        RenderType renderType = RenderType.eyes(EYES_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, OverlayTexture.NO_OVERLAY);
    }
}
