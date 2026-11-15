package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CharybdisRenderer extends MobRenderer<CharybdisEntity, CharybdisModel<CharybdisEntity>> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/charybdis.png");

    public CharybdisRenderer(EntityRendererProvider.Context context) {
        super(context, new CharybdisModel<>(context.bakeLayer(CharybdisModel.LAYER_LOCATION)), 4.5F);
    }

    @Override
    protected void scale(CharybdisEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(11.2F, 11.2F, 11.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(CharybdisEntity entity) {
        return TEX;
    }
}
