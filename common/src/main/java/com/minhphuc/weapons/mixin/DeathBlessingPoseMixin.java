package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.config.SkillVisualConfig;
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

        // =========================================================================
        // LONG TINH BỘC VIÊM BÁ (DRAGON NOVA - TƯ THẾ TỤ CHƯỞNG & BẮN ĐẠI PHÁO PLASMA)
        // =========================================================================
        if (ClientCinematicSkillHandler.isCasterActive(entity.getUUID()) && ClientCinematicSkillHandler.getActiveSkillId() == SkillVisualConfig.SKILL_DRAGON_NOVA) {
            float dnAge = ClientCinematicSkillHandler.getAge();
            if (dnAge >= 0 && dnAge < 75) {
                if (dnAge < 30) {
                    float t = Math.min(1.0f, dnAge / 8.0f);
                    // Hồi 1: Tụ chưởng trước ngực (Tư thế tụ chưởng ma đạo kinh điển của Milim)
                    rightArm.xRot = -1.35f * t;
                    rightArm.yRot = -0.25f * t;
                    rightArm.zRot = 0.15f * t;

                    leftArm.xRot = -1.35f * t;
                    leftArm.yRot = 0.25f * t;
                    leftArm.zRot = -0.15f * t;

                    body.xRot = -0.06f * t;
                    head.xRot = -0.05f * t;
                    hat.copyFrom(head);
                } else if (dnAge < 55) {
                    float blastT = Math.min(1.0f, (dnAge - 30.0f) / 4.0f);
                    // Hồi 2: Khai hỏa đại pháo Plasma - Đẩy mạnh 2 tay về phía trước, thân ngửa sau chống giật
                    rightArm.xRot = -1.55f;
                    rightArm.yRot = -0.10f;
                    rightArm.zRot = 0.05f;

                    leftArm.xRot = -1.55f;
                    leftArm.yRot = 0.10f;
                    leftArm.zRot = -0.05f;

                    body.xRot = 0.10f * blastT;
                    head.xRot = -0.10f * blastT;
                    hat.copyFrom(head);
                } else {
                    // Hồi 3: Hạ tay từ tốn hoàn thế
                    float returnP = Math.min(1.0f, (dnAge - 55.0f) / 18.0f);
                    float blend = 1.0f - returnP;

                    rightArm.xRot = -1.55f * blend;
                    rightArm.yRot = -0.10f * blend;
                    rightArm.zRot = 0.05f * blend;

                    leftArm.xRot = -1.55f * blend;
                    leftArm.yRot = 0.10f * blend;
                    leftArm.zRot = -0.05f * blend;

                    body.xRot = 0.10f * blend;
                    head.xRot = -0.10f * blend;
                    hat.copyFrom(head);
                }
                return;
            }
        }

        if (!ClientNihilityCinematicHandler.isCasterActive(entity.getUUID())) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        float partialTicks = (mc.getTimer() != null) ? mc.getTimer().getGameTimeDeltaPartialTick(false) : 0.0f;
        float age = ClientNihilityCinematicHandler.getAge(partialTicks);
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
