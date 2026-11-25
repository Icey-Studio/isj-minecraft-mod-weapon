package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.TrueDragonModel;
import com.minhphuc.weapons.client.model.VeldoraDragonModel;
import com.minhphuc.weapons.client.model.VelgryndModel;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VelgryndRenderer extends MobRenderer<VelgryndEntity, EntityModel<VelgryndEntity>> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/velgrynd.png");
    public static final ResourceLocation DRAGON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/tensura/velgrynd_dragon.png");

    private final VelgryndModel humanModel;
    private final TrueDragonModel<VelgryndEntity> dragonModel;

    public VelgryndRenderer(EntityRendererProvider.Context context) {
        super(context, new VelgryndModel(context.bakeLayer(VelgryndModel.LAYER_LOCATION)), 0.5F);
        this.humanModel = (VelgryndModel) this.model;
        this.dragonModel = new TrueDragonModel<>(
                context.bakeLayer(VeldoraDragonModel.LAYER_LOCATION),
                VelgryndEntity::isFlyingAnim,
                e -> e.getCastingState() > 0
        );
        this.addLayer(new VelgryndGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VelgryndEntity entity) {
        return entity.isDragonForm() ? DRAGON_TEXTURE : TEXTURE;
    }

    @Override
    public void render(VelgryndEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = entity.isDragonForm() ? dragonModel : humanModel;
        this.shadowRadius = entity.isDragonForm() ? 8.0F : 0.5F;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(VelgryndEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = entity.isDragonForm() ? 14.0F : 1.08F;
        poseStack.scale(scale, scale, scale);
    }
}
