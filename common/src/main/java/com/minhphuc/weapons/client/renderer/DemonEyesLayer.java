package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.PrimordialDemonModel;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class DemonEyesLayer extends RenderLayer<PrimordialDemonEntity, PrimordialDemonModel<PrimordialDemonEntity>> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_noir.png");

    public DemonEyesLayer(RenderLayerParent<PrimordialDemonEntity, PrimordialDemonModel<PrimordialDemonEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       PrimordialDemonEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ResourceLocation eyesLoc = entity.getDemonType() != null
                ? entity.getDemonType().getEyesTextureLocation()
                : FALLBACK;
        RenderType renderType = RenderType.eyes(eyesLoc);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, OverlayTexture.NO_OVERLAY);
    }
}
