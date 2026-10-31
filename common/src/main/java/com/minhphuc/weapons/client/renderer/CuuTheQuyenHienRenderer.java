package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.model.CuuTheQuyenHienModel;
import com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class CuuTheQuyenHienRenderer extends MobRenderer<CuuTheQuyenHienEntity, CuuTheQuyenHienModel<CuuTheQuyenHienEntity>> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/cuu_the_quyen_hien.png");

    public CuuTheQuyenHienRenderer(EntityRendererProvider.Context context) {
        super(context, new CuuTheQuyenHienModel<>(context.bakeLayer(CuuTheQuyenHienModel.LAYER_LOCATION)), 0.65F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CuuTheGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CuuTheQuyenHienEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(CuuTheQuyenHienEntity entity, PoseStack poseStack, float partialTickTime) {
        // Tỉ lệ to lớn và đô con hơn người chơi (1.22x) tạo cảm giác đồ sộ uy áp
        float scale = 1.22F;
        poseStack.scale(scale, scale, scale);
    }
}
