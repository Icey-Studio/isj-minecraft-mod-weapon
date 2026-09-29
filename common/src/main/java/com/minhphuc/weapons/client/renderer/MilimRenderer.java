package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.MilimModel;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MilimRenderer extends MobRenderer<MilimEntity, MilimModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/milim.png");

    public MilimRenderer(EntityRendererProvider.Context context) {
        super(context, new MilimModel(context.bakeLayer(MilimModel.LAYER_LOCATION)), 0.45F);
        this.addLayer(new MilimGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MilimEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(MilimEntity entity, PoseStack poseStack, float partialTickTime) {
        // Vóc dáng Milim nhỏ nhắn nhưng mạnh mẽ (tỉ lệ chuẩn anime)
        float scale = 0.95F;
        poseStack.scale(scale, scale, scale);
    }
}
