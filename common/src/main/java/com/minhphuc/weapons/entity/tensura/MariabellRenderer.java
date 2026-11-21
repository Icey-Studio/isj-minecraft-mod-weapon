package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.WeaponsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer của Tiểu Thư Mariabell Rosso với tỷ lệ quý tộc cao 1.5 blocks.
 */
public class MariabellRenderer extends HumanoidMobRenderer<MariabellEntity, MariabellModel> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "textures/entity/tensura/mariabell.png");

    public MariabellRenderer(EntityRendererProvider.Context context) {
        super(context, new MariabellModel(context.bakeLayer(MariabellModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(MariabellEntity entity) {
        return TEX;
    }

    @Override
    protected void scale(MariabellEntity entity, PoseStack poseStack, float partialTickTime) {
        // Thu nhỏ xuống 0.82F để tạo chiều cao chuẩn thiếu nữ quý tộc 1.5 blocks (1.8m * 0.82 = 1.48m)
        poseStack.scale(0.82F, 0.82F, 0.82F);
    }
}
