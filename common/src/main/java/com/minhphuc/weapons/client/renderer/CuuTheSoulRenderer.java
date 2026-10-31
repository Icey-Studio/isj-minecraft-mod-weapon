package com.minhphuc.weapons.client.renderer;

import com.minhphuc.weapons.client.ClientRykuzinHandler;
import com.minhphuc.weapons.client.model.CuuTheQuyenHienModel;
import com.minhphuc.weapons.entity.darkgathering.CuuTheSoulEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer cho Linh Hồn Cứu Thế Quyền Hiện:
 * - BÌNH THƯỜNG: VÔ HÌNH HOÀN TOÀN với người chơi không bật Tiên Tri.
 * - KHI BẬT TIÊN TRI: Hiển thị hình bóng linh hồn bán trong suốt huyền ảo rực sáng lao tới.
 */
public class CuuTheSoulRenderer extends MobRenderer<CuuTheSoulEntity, CuuTheQuyenHienModel<CuuTheSoulEntity>> {

    public static final ResourceLocation SOUL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/cuu_the_soul.png");

    public CuuTheSoulRenderer(EntityRendererProvider.Context context) {
        super(context, new CuuTheQuyenHienModel<>(context.bakeLayer(CuuTheQuyenHienModel.LAYER_LOCATION)), 0.65F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CuuTheSoulEntity entity) {
        return SOUL_TEXTURE;
    }

    @Override
    public void render(CuuTheSoulEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        boolean canSee = ClientRykuzinHandler.isActive() || (mc.player != null && mc.player.isSpectator());

        // Nếu người chơi KHÔNG bật Tiên Tri: Bỏ qua render (hoàn toàn vô hình)
        if (!canSee) {
            return;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(CuuTheSoulEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = 1.22F;
        poseStack.scale(scale, scale, scale);
    }
}
