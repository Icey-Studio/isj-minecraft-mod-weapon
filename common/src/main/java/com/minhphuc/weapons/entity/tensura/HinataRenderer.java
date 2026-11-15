package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class HinataRenderer extends HumanoidMobRenderer<HinataEntity, HinataModel> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/hinata.png");

    public HinataRenderer(EntityRendererProvider.Context context) {
        super(context, new HinataModel(context.bakeLayer(HinataModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HinataModel(context.bakeLayer(HinataModel.INNER_ARMOR)),
                new HinataModel(context.bakeLayer(HinataModel.OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(HinataEntity entity) {
        return TEX;
    }
}
