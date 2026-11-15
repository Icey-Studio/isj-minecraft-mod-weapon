package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.client.model.VeldoraDragonModel;
import com.minhphuc.weapons.client.model.VeldoraHumanModel;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer chuyển đổi linh hoạt giữa Dạng Rồng Khổng Lồ (scale 2.8) và Dạng Người của Veldora.
 */
public class VeldoraRenderer extends MobRenderer<VeldoraEntity, EntityModel<VeldoraEntity>> {

    private static final ResourceLocation DRAGON_TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/veldora_dragon.png");
    private static final ResourceLocation HUMAN_TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/veldora_human.png");

    private final VeldoraDragonModel dragonModel;
    private final VeldoraHumanModel humanModel;

    public VeldoraRenderer(EntityRendererProvider.Context context) {
        super(context, new VeldoraDragonModel(context.bakeLayer(VeldoraDragonModel.LAYER_LOCATION)), 1.6F);
        this.dragonModel = (VeldoraDragonModel) this.model;
        this.humanModel = new VeldoraHumanModel(context.bakeLayer(VeldoraHumanModel.LAYER_LOCATION));
    }

    @Override
    public void render(VeldoraEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = entity.isHumanForm() ? humanModel : dragonModel;
        this.shadowRadius = entity.isHumanForm() ? 0.5F : (entity.isSealed() ? 4.5F : 9.2F);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(VeldoraEntity entity, PoseStack poseStack, float partialTicks) {
        if (entity.isHumanForm()) {
            poseStack.scale(1.06F, 1.06F, 1.06F);
        } else if (entity.isSealed()) {
            poseStack.scale(6.0F, 6.0F, 6.0F);
        } else {
            // To gấp ~5 lần bản trước (2.85 -> 13.5) — tầm cỡ Cự Long Bạo Phong
            poseStack.scale(13.5F, 13.5F, 13.5F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VeldoraEntity entity) {
        return entity.isHumanForm() ? HUMAN_TEX : DRAGON_TEX;
    }
}
