package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "hasLineOfSight", at = @At("HEAD"), cancellable = true)
    private void weapons$multilayerBarrierLineOfSight(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MultilayerBarrierAbility.hasLineOfSightThroughBarrier((LivingEntity) (Object) this, entity)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Khắc phục triệt để lỗi sập server khi mob không có sẵn thuộc tính ATTACK_DAMAGE hoặc ATTACK_KNOCKBACK
     * (như Cá Hồi, Bò, Cừu, Gà, Dân Làng...) khi được ban tên hoặc tiến hóa Ma Vương.
     */
    @Inject(method = "getAttributeValue", at = @At("HEAD"), cancellable = true)
    private void weapons$fallbackMissingAttribute(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.getAttributes().hasAttribute(attribute)) {
            boolean isNamed = self.getTags().contains("TensuraNamedMob");
            boolean isDemonLord = self.getTags().contains("TensuraDemonLordMob");

            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                if (isNamed && isDemonLord) {
                    cir.setReturnValue(300.0D); // Được cả ban tên và ma vương: Sát thương tối thượng
                } else if (isDemonLord) {
                    cir.setReturnValue(150.0D); // Ma Vương cuồng nộ
                } else if (isNamed) {
                    cir.setReturnValue(80.0D); // Sinh vật được ban tên
                } else {
                    cir.setReturnValue(0.0D);
                }
            } else if (attribute.equals(Attributes.ATTACK_KNOCKBACK)) {
                if (isNamed && isDemonLord) {
                    cir.setReturnValue(2.0D);
                } else if (isDemonLord) {
                    cir.setReturnValue(1.5D);
                } else {
                    cir.setReturnValue(0.0D);
                }
            } else {
                cir.setReturnValue(0.0D);
            }
        }
    }

    /**
     * Sinh vật được ban tên hoặc tiến hóa ma vương có thể hô hấp vĩnh viễn dưới nước (không bao giờ chết đuối).
     */
    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void weapons$namedOrDemonLordBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getTags().contains("TensuraNamedMob") || self.getTags().contains("TensuraDemonLordMob")) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Không bao giờ giảm lượng khí thở (bảo vệ các loài sống dưới nước khi lên cạn và ngược lại).
     */
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void weapons$preventDecreaseAirSupply(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getTags().contains("TensuraNamedMob") || self.getTags().contains("TensuraDemonLordMob")) {
            cir.setReturnValue(self.getMaxAirSupply());
        }
    }

    /**
     * Duy trì bình dưỡng khí tối đa và dập tắt lửa tức thì nếu có cả 2 thể trạng Ma Vương & Ban Tên.
     */
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void weapons$namedOrDemonLordTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean isNamed = self.getTags().contains("TensuraNamedMob");
        boolean isDemonLord = self.getTags().contains("TensuraDemonLordMob");

        if (isNamed || isDemonLord) {
            if (self.getAirSupply() < self.getMaxAirSupply()) {
                self.setAirSupply(self.getMaxAirSupply());
            }
            // Nếu có cả 2 thể trạng Ma Vương và Ban Tên: Miễn nhiễm hoàn toàn với lửa / thiêu đốt
            if (isNamed && isDemonLord && self.isOnFire()) {
                self.clearFire();
            }
        }
    }
}
