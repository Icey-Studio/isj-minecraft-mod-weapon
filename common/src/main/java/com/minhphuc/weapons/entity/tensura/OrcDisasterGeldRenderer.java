package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class OrcDisasterGeldRenderer extends MobRenderer<OrcDisasterGeldEntity, OrcDisasterGeldModel<OrcDisasterGeldEntity>> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/orc_disaster_geld.png");

    public OrcDisasterGeldRenderer(EntityRendererProvider.Context context) {
        super(context, new OrcDisasterGeldModel<>(context.bakeLayer(OrcDisasterGeldModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    protected void scale(OrcDisasterGeldEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = 1.65F + Math.min(0.65F, entity.getStarvedStacks() * 0.03F);
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(OrcDisasterGeldEntity entity) {
        return TEX;
    }
}
