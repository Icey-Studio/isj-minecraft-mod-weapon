package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.UnderworldDemonModel;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class UnderworldDemonGlowLayer extends RenderLayer<UnderworldDemonEntity, UnderworldDemonModel> {

    private static final ResourceLocation HUMANOID_GLOW =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/underworld/underworld_humanoid_glow.png");
    private static final ResourceLocation BEAST_GLOW =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/underworld/underworld_beast_glow.png");
    private static final ResourceLocation FIEND_GLOW =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/underworld/underworld_fiend_glow.png");

    public UnderworldDemonGlowLayer(RenderLayerParent<UnderworldDemonEntity, UnderworldDemonModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       UnderworldDemonEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ResourceLocation tex = switch (entity.getMorph()) {
            case BEAST -> BEAST_GLOW;
            case WINGED_FIEND -> FIEND_GLOW;
            default -> HUMANOID_GLOW;
        };
        DemonType type = entity.getDemonType();
        int tintArgb = 0xFF000000 | (type != null ? type.getGlowColor() : 0xFF3333);
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(tex));
        this.getParentModel().renderToBuffer(poseStack, vc, 15728640, OverlayTexture.NO_OVERLAY, tintArgb);
    }
}
