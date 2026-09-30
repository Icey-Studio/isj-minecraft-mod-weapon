package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.VelzardModel;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class VelzardGlowLayer extends RenderLayer<VelzardEntity, VelzardModel> {

    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/velzard_eyes.png");

    public VelzardGlowLayer(RenderLayerParent<VelzardEntity, VelzardModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       VelzardEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType renderType = RenderType.eyes(EYES_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, OverlayTexture.NO_OVERLAY);
    }
}
