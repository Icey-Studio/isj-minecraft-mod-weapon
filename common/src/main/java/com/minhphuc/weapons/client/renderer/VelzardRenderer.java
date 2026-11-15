package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.TrueDragonModel;
import com.minhphuc.weapons.client.model.VeldoraDragonModel;
import com.minhphuc.weapons.client.model.VelzardModel;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VelzardRenderer extends MobRenderer<VelzardEntity, EntityModel<VelzardEntity>> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/velzard.png");
    public static final ResourceLocation DRAGON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/tensura/velzard_dragon.png");

    private final VelzardModel humanModel;
    private final TrueDragonModel<VelzardEntity> dragonModel;

    public VelzardRenderer(EntityRendererProvider.Context context) {
        super(context, new VelzardModel(context.bakeLayer(VelzardModel.LAYER_LOCATION)), 0.5F);
        this.humanModel = (VelzardModel) this.model;
        this.dragonModel = new TrueDragonModel<>(
                context.bakeLayer(VeldoraDragonModel.LAYER_LOCATION),
                VelzardEntity::isFlyingAnim,
                e -> e.getCastingState() > 0
        );
        this.addLayer(new VelzardGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VelzardEntity entity) {
        return entity.isDragonForm() ? DRAGON_TEXTURE : TEXTURE;
    }

    @Override
    public void render(VelzardEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = entity.isDragonForm() ? dragonModel : humanModel;
        this.shadowRadius = entity.isDragonForm() ? 8.2F : 0.5F;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(VelzardEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = entity.isDragonForm() ? 12.6F : (entity.isAwakened() ? 1.15F : 1.05F);
        poseStack.scale(scale, scale, scale);
    }
}
