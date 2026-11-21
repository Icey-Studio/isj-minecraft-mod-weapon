package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.client.model.ChaosDragonModel;
import com.minhphuc.weapons.entity.tensura.ChaosDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer của Boss Hỗn Độn Long (Chaos Dragon) với kích thước khổng lồ x5 (Scale 14.0F).
 */
public class ChaosDragonRenderer extends MobRenderer<ChaosDragonEntity, ChaosDragonModel> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/chaos_dragon.png");

    public ChaosDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new ChaosDragonModel(context.bakeLayer(ChaosDragonModel.LAYER_LOCATION)), 3.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ChaosDragonEntity entity) {
        return TEX;
    }

    @Override
    protected void scale(ChaosDragonEntity entity, PoseStack poseStack, float partialTickTime) {
        // Kích thước khổng lồ gấp 5 lần nguyên bản (Scale 14.0F)
        poseStack.scale(14.0F, 14.0F, 14.0F);
    }
}
