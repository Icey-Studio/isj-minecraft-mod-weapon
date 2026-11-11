package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.DemonLordKuboModel;
import com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DemonLordKuboRenderer extends MobRenderer<DemonLordKuboEntity, DemonLordKuboModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon_lord_kubo.png");

    public DemonLordKuboRenderer(EntityRendererProvider.Context context) {
        super(context, new DemonLordKuboModel(context.bakeLayer(DemonLordKuboModel.LAYER_LOCATION)), 1.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(DemonLordKuboEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DemonLordKuboEntity entity, PoseStack poseStack, float partialTickTime) {
        float s = 1.85F;
        poseStack.scale(s, s, s);
    }
}
