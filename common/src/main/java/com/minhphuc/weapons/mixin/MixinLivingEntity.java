package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "hasLineOfSight", at = @At("HEAD"), cancellable = true)
    private void weapons$multilayerBarrierLineOfSight(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MultilayerBarrierAbility.hasLineOfSightThroughBarrier((LivingEntity) (Object) this, entity)) {
            cir.setReturnValue(true);
        }
    }
}
