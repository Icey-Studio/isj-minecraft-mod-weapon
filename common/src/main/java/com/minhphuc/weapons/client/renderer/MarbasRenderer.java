package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.MarbasModel;
import com.minhphuc.weapons.entity.darkgathering.MarbasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class MarbasRenderer extends MobRenderer<MarbasEntity, MarbasModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/marbas.png");

    public MarbasRenderer(EntityRendererProvider.Context context) {
        super(context, new MarbasModel(context.bakeLayer(MarbasModel.LAYER_LOCATION)), 0.75F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(MarbasEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(MarbasEntity entity, PoseStack poseStack, float partialTickTime) {
        // Vóc dáng khổng lồ, cao 4.2 block uy nghiêm (2.1x)
        float scale = 2.1F;
        poseStack.scale(scale, scale, scale);
    }
}
