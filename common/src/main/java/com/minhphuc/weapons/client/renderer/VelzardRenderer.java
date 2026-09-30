package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.VelzardModel;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VelzardRenderer extends MobRenderer<VelzardEntity, VelzardModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/velzard.png");

    public VelzardRenderer(EntityRendererProvider.Context context) {
        super(context, new VelzardModel(context.bakeLayer(VelzardModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new VelzardGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VelzardEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(VelzardEntity entity, PoseStack poseStack, float partialTickTime) {
        // True Dragon majesty scale - slightly refined feminine scale
        float scale = entity.isAwakened() ? 1.15F : 1.05F;
        poseStack.scale(scale, scale, scale);
    }
}
