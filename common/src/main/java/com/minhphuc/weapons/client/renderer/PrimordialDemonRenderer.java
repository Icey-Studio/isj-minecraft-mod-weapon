package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.PrimordialDemonModel;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PrimordialDemonRenderer extends MobRenderer<PrimordialDemonEntity, PrimordialDemonModel<PrimordialDemonEntity>> {

    public PrimordialDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimordialDemonModel<>(context.bakeLayer(PrimordialDemonModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new DemonEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PrimordialDemonEntity entity) {
        com.minhphuc.weapons.entity.tensura.DemonType type = entity.getDemonType();
        if (type == null) type = com.minhphuc.weapons.entity.tensura.DemonType.NOIR;

        if (entity.hasPhysicalBody()) {
            return type.getBodyTextureLocation();
        }
        return type.getTextureLocation();
    }

    @Override
    protected void scale(PrimordialDemonEntity entity, PoseStack poseStack, float partialTickTime) {
        float baseScale = entity.getDemonType() != null ? entity.getDemonType().getScale() : 1.0F;
        if (entity.hasPhysicalBody()) {
            baseScale *= 1.1F; // Dạng có thể xác vật lý cao lớn hơn
        }
        poseStack.scale(baseScale, baseScale, baseScale);
    }
}
