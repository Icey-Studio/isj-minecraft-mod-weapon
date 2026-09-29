package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.KuboModel;
import com.minhphuc.weapons.entity.darkgathering.KuboEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class KuboRenderer extends MobRenderer<KuboEntity, KuboModel> {

    public static final ResourceLocation TEXTURE_NORMAL =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/kubo.png");
    public static final ResourceLocation TEXTURE_ULTIMATE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/kubo_ultimate.png");
    public static final ResourceLocation TEXTURE_WHITE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/kubo_white.png");

    public KuboRenderer(EntityRendererProvider.Context context) {
        super(context, new KuboModel(context.bakeLayer(KuboModel.LAYER_LOCATION)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(KuboEntity entity) {
        if (entity.isComplete()) {
            return TEXTURE_WHITE;
        } else if (entity.isUltimate()) {
            return TEXTURE_ULTIMATE;
        }
        return TEXTURE_NORMAL;
    }

    @Override
    protected void scale(KuboEntity entity, PoseStack poseStack, float partialTickTime) {
        float s = 1.6F;
        if (entity.isComplete()) {
            s = 2.2F; // Dạng Bạch Nhật Hoàn Chỉnh khổng lồ uy nghiêm
        } else if (entity.isUltimate()) {
            s = 2.0F; // Dạng Tối Thượng to lớn cuồng nộ
        }
        poseStack.scale(s, s, s);
    }
}
