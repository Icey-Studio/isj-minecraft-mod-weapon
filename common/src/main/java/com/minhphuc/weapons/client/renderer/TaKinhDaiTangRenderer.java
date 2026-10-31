package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.TaKinhDaiTangModel;
import com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class TaKinhDaiTangRenderer extends MobRenderer<TaKinhDaiTangEntity, TaKinhDaiTangModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/ta_kinh_dai_tang.png");

    public TaKinhDaiTangRenderer(EntityRendererProvider.Context context) {
        super(context, new TaKinhDaiTangModel(context.bakeLayer(TaKinhDaiTangModel.LAYER_LOCATION)), 0.55F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(TaKinhDaiTangEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TaKinhDaiTangEntity entity, PoseStack poseStack, float partialTickTime) {
        // Vóc dáng cao gầy, hơi gù của vị Nhục Thân Bồ Tát (1.18x)
        float scale = 1.18F;
        poseStack.scale(scale, scale, scale);
    }
}

