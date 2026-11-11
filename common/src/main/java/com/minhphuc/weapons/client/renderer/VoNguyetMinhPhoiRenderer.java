package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.VoNguyetMinhPhoiModel;
import com.minhphuc.weapons.entity.darkgathering.VoNguyetMinhPhoiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VoNguyetMinhPhoiRenderer extends MobRenderer<VoNguyetMinhPhoiEntity, VoNguyetMinhPhoiModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/vo_nguyet_minh_phoi.png");

    public VoNguyetMinhPhoiRenderer(EntityRendererProvider.Context context) {
        super(context, new VoNguyetMinhPhoiModel(context.bakeLayer(VoNguyetMinhPhoiModel.LAYER_LOCATION)), 1.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoNguyetMinhPhoiEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(VoNguyetMinhPhoiEntity entity, PoseStack poseStack, float partialTickTime) {
        float s = 1.65F;
        if (entity.isComplete()) {
            s = 2.2F;
        } else if (entity.isUltimate()) {
            s = 2.0F;
        }
        poseStack.scale(s, s, s);
    }
}
