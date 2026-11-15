package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.UnderworldDemonModel;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UnderworldDemonRenderer extends MobRenderer<UnderworldDemonEntity, UnderworldDemonModel> {

    private static final ResourceLocation HUMANOID_TEX =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/underworld/underworld_humanoid.png");
    private static final ResourceLocation BEAST_TEX =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/underworld/underworld_beast.png");
    private static final ResourceLocation FIEND_TEX =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/underworld/underworld_fiend.png");

    private final UnderworldDemonModel humanoidModel;
    private final UnderworldDemonModel beastModel;
    private final UnderworldDemonModel fiendModel;

    public UnderworldDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new UnderworldDemonModel(
                context.bakeLayer(UnderworldDemonModel.HUMANOID_LAYER),
                UnderworldDemonEntity.DemonMorph.HUMANOID), 0.55F);
        this.humanoidModel = this.model;
        this.beastModel = new UnderworldDemonModel(
                context.bakeLayer(UnderworldDemonModel.BEAST_LAYER),
                UnderworldDemonEntity.DemonMorph.BEAST);
        this.fiendModel = new UnderworldDemonModel(
                context.bakeLayer(UnderworldDemonModel.FIEND_LAYER),
                UnderworldDemonEntity.DemonMorph.WINGED_FIEND);
        this.addLayer(new UnderworldDemonGlowLayer(this));
    }

    @Override
    public void render(UnderworldDemonEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = switch (entity.getMorph()) {
            case BEAST -> this.beastModel;
            case WINGED_FIEND -> this.fiendModel;
            default -> this.humanoidModel;
        };
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(UnderworldDemonEntity entity) {
        return switch (entity.getMorph()) {
            case BEAST -> BEAST_TEX;
            case WINGED_FIEND -> FIEND_TEX;
            default -> HUMANOID_TEX;
        };
    }

    @Override
    protected void scale(UnderworldDemonEntity entity, PoseStack poseStack, float partialTickTime) {
        float s = entity.getRank().baseScale;
        if (entity.hasPhysicalBody()) s *= 1.08F;
        if (entity.isDemonLord()) s *= 1.15F;
        poseStack.scale(s, s, s);
    }
}
