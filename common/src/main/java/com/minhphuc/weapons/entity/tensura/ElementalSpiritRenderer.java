package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class ElementalSpiritRenderer extends MobRenderer<ElementalSpiritEntity, ElementalSpiritModel<ElementalSpiritEntity>> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/block/spirit_shrine.png");

    public ElementalSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new ElementalSpiritModel<>(context.bakeLayer(ElementalSpiritModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public void render(ElementalSpiritEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ElementalType type = entity.getElementalType();
        poseStack.pushPose();
        this.model.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);
        int argb = FastColor.ARGB32.color(220, type.r, type.g, type.b);
        this.model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityTranslucent(TEX)),
                0x00F000F0,
                OverlayTexture.NO_OVERLAY,
                argb
        );
        poseStack.popPose();
        if (this.shouldShowName(entity)) {
            this.renderNameTag(entity, entity.getDisplayName(), poseStack, buffer, packedLight, partialTicks);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ElementalSpiritEntity entity) {
        return TEX;
    }
}
