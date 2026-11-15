package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class ChronoaRenderer extends HumanoidMobRenderer<ChronoaEntity, ChronoaModel> {
    private static final ResourceLocation CALM_TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/chronoa_calm.png");
    private static final ResourceLocation CORRUPTED_TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/chronoa_corrupted.png");

    public ChronoaRenderer(EntityRendererProvider.Context context) {
        super(context, new ChronoaModel(context.bakeLayer(ChronoaModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new ChronoaModel(context.bakeLayer(ChronoaModel.INNER_ARMOR)),
                new ChronoaModel(context.bakeLayer(ChronoaModel.OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(ChronoaEntity entity) {
        return entity.isCorrupted() ? CORRUPTED_TEX : CALM_TEX;
    }
}
