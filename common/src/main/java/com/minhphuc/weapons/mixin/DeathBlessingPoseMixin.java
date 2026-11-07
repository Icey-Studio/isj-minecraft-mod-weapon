package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Điều chỉnh tư thế 3D cho mô hình nhân vật khi thi triển Lời Chúc Tử Vong:
 * - Hồi 1 & 2: Nâng vũ khí lên ngực và hướng lên bầu trời
 * - Hồi 3: Dang rộng 2 tay uy nghiêm sang 2 bên, ngực ưỡn kiêu hãnh, đầu hơi ngửa
 * - Hồi 4: Hạ mạnh cánh tay dội cột sáng hủy diệt xuống chiến trường
 */
@Mixin(HumanoidModel.class)
public abstract class DeathBlessingPoseMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart hat;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void weapons$applyDeathBlessingPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                               float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!ClientNihilityCinematicHandler.isCasterActive(entity.getUUID())) return;

        float age = ClientNihilityCinematicHandler.getAge();
        if (age < 0 || age >= 295) return;

        // Giai đoạn 1 & 2 (0 -> 110 ticks): Nâng vũ khí lên ngực và hướng lên trời
        if (age < 110) {
            float progress = age / 110.0f;
            rightArm.xRot = -0.4f - (progress * 1.4f);
            rightArm.yRot = -0.15f;
            rightArm.zRot = 0.2f;

            leftArm.xRot = -0.2f - (progress * 0.8f);
            leftArm.zRot = -0.2f;

            head.xRot = -0.1f - (progress * 0.2f);
            hat.copyFrom(head);
        }
        // Giai đoạn 3 (110 -> 225 ticks): Điểm Tĩnh Lặng Tuyệt Đối - Dang rộng 2 tay uy nghiêm
        else if (age < 225) {
            float t = Math.min(1.0f, (age - 110.0f) / 25.0f);
            float spreadEase = t * t * (3.0f - 2.0f * t);

            // Dang rộng 2 tay sang 2 bên
            rightArm.xRot = -0.35f;
            rightArm.yRot = 0.0f;
            rightArm.zRot = 0.2f + (spreadEase * 1.15f);

            leftArm.xRot = -0.35f;
            leftArm.yRot = 0.0f;
            leftArm.zRot = -0.2f - (spreadEase * 1.15f);

            body.xRot = -0.08f * spreadEase;
            head.xRot = -0.28f * spreadEase;
            hat.copyFrom(head);
        }
        // Giai đoạn 4 (225 -> 270 ticks): Dội cột sáng xuống đất
        else if (age < 270) {
            rightArm.xRot = 1.8f;
            rightArm.zRot = 0.1f;
            leftArm.xRot = 1.8f;
            leftArm.zRot = -0.1f;
            body.xRot = 0.15f;
            head.xRot = 0.35f;
            hat.copyFrom(head);
        }
        // Trả vị trí cánh tay về bình thường (270 -> 295 ticks)
        else {
            float blend = (295.0f - age) / 25.0f;
            rightArm.xRot *= blend;
            rightArm.zRot *= blend;
            leftArm.xRot *= blend;
            leftArm.zRot *= blend;
            body.xRot *= blend;
            head.xRot *= blend;
            hat.copyFrom(head);
        }
    }
}
