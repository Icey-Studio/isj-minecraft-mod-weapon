package com.minhphuc.weapons.entity.darkgathering;

import com.minhphuc.weapons.WeaponsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CursedIdolRenderer extends MobRenderer<CursedIdolEntity, CursedIdolModel<CursedIdolEntity>> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/darkgathering/cursed_idol.png");

    public CursedIdolRenderer(EntityRendererProvider.Context context) {
        super(context, new CursedIdolModel<>(context.bakeLayer(CursedIdolModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    protected void scale(CursedIdolEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.5F, 1.5F, 1.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CursedIdolEntity entity) {
        return TEX;
    }
}
