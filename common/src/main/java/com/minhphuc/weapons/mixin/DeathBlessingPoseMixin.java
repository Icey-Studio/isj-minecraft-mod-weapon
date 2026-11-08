package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.config.SkillVisualConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
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
        if (ClientCinematicSkillHandler.isCasterActive(entity.getUUID())) {
            int skillId = ClientCinematicSkillHandler.getActiveSkillId();
            float cAge = ClientCinematicSkillHandler.getAge();
            if (skillId == SkillVisualConfig.SKILL_DRAGON_NOVA && cAge >= 0 && cAge < 75) {
                applyDragonNovaPose(entity, rightArm, leftArm, body, head, hat, cAge);
                return;
            }
        }

        if (ClientHellflareCinematicHandler.isCasterActive(entity.getUUID())) {
            float hfAge = ClientHellflareCinematicHandler.getAge();
            if (hfAge >= 0 && hfAge < 75) {
                ClientHellflareCinematicHandler.applyPose(entity, rightArm, leftArm, body, head, hat, hfAge);
                return;
            }
        }

        if (ClientAzathothCinematicHandler.isCasterActive(entity.getUUID())) {
            float azAge = ClientAzathothCinematicHandler.getAge();
            if (azAge >= 0 && azAge < 35) {
                if (azAge < 16) {
                    float t = Math.min(1.0f, azAge / 4.0f);
                    // Bắn Cự Long Plasma: Hai tay đẩy thẳng ra trước kết ấn dẫn luồng năng lượng
                    rightArm.xRot = -1.55f * t;
                    rightArm.yRot = -0.15f * t;
                    rightArm.zRot = 0.05f;

                    leftArm.xRot = -1.55f * t;
                    leftArm.yRot = 0.15f * t;
                    leftArm.zRot = -0.05f;

                    body.xRot = 0.10f * t; // Thân người hơi ngửa sau chống giật
                    head.xRot = -0.10f * t;
                    hat.copyFrom(head);
                } else {
                    float returnP = (azAge - 16.0f) / 19.0f;
                    float blend = 1.0f - returnP;
                    // Thu hồi năng lượng: Hai tay co dần về lồng ngực
                    rightArm.xRot = -1.55f * blend + (-0.35f * (1.0f - blend));
                    rightArm.yRot = -0.15f * blend;
                    rightArm.zRot = 0.25f * (1.0f - blend);

                    leftArm.xRot = -1.55f * blend + (-0.35f * (1.0f - blend));
                    leftArm.yRot = 0.15f * blend;
                    leftArm.zRot = -0.25f * (1.0f - blend);

                    body.xRot = 0.10f * blend;
                    head.xRot = -0.10f * blend;
                    hat.copyFrom(head);
                }
                return;
            }
        }

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

    /**
     * Tư thế 3D cho Long Tinh Bộc Viêm Bá (Dragon Nova):
     * - Hồi 1 (0 -> 30 ticks): Tay phải vươn thẳng tuyệt đối ra trước, lòng bàn tay mở ngưng tụ linh tử
     * - Hồi 2 (30 -> 55 ticks): Giữ vững tư thế khai hỏa luồng pháo 75m với độ rung giật ma lực cực hạn
     * - Hồi 3 (55 -> 75 ticks): Hạ dần cánh tay mượt mà trở về bình thường
     */
    private void applyDragonNovaPose(LivingEntity entity, ModelPart rightArm, ModelPart leftArm,
                                     ModelPart body, ModelPart head, ModelPart hat, float age) {
        if (age < 30.0f) {
            float t = Math.min(1.0f, age / 8.0f);
            float ease = t * t * (3.0f - 2.0f * t);

            // Tay phải duỗi thẳng ra trước (-90 độ), hơi khép vào tâm lồng ngực
            rightArm.xRot = -1.55f * ease;
            rightArm.yRot = -0.12f * ease;
            rightArm.zRot = 0.05f * ease;

            // Tay trái co lại gần ngực/hông thế thủ uy nghiêm
            leftArm.xRot = -0.45f * ease;
            leftArm.yRot = 0.20f * ease;
            leftArm.zRot = -0.25f * ease;

            // Thân người thẳng, ngẩng đầu kiêu hãnh
            head.xRot = -0.08f * ease;
            hat.copyFrom(head);
        } else if (age < 55.0f) {
            // Khai hỏa cực hạn: Tay phải giữ thẳng tuyệt đối dẫn luồng pháo 75m, rung giật ma lực
            float recoil = (float) Math.sin(age * 2.5f) * 0.035f;
            rightArm.xRot = -1.55f + recoil;
            rightArm.yRot = -0.12f;
            rightArm.zRot = 0.05f;

            // Tay trái giang nhẹ hỗ trợ cân bằng áp lực
            leftArm.xRot = -0.65f;
            leftArm.yRot = 0.35f;
            leftArm.zRot = -0.35f;

            // Thân người hơi ngả sau chống giật ma lực
            body.xRot = 0.08f;
            head.xRot = -0.10f;
            hat.copyFrom(head);
        } else {
            // Hồi vị mượt mà (55 -> 75 ticks)
            float returnP = Math.clamp((age - 55.0f) / 20.0f, 0.0f, 1.0f);
            float blend = 1.0f - returnP;
            float ease = blend * blend * (3.0f - 2.0f * blend);

            rightArm.xRot = -1.55f * ease;
            rightArm.yRot = -0.12f * ease;
            rightArm.zRot = 0.05f * ease;

            leftArm.xRot = -0.45f * ease;
            leftArm.yRot = 0.20f * ease;
            leftArm.zRot = -0.25f * ease;

            body.xRot = 0.08f * ease;
            head.xRot = -0.10f * ease;
            hat.copyFrom(head);
        }
    }
}
