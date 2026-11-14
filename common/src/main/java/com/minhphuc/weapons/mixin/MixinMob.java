package com.minhphuc.weapons.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MixinMob {

    /**
     * Vô hiệu hóa việc bị mặt trời thiêu đốt (cháy nắng) đối với các quái vật Undead
     * (Zombie, Skeleton, Phantom...) khi được ban tên hoặc tiến hóa Ma Vương.
     */
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void weapons$preventSunburnTick(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self.getTags().contains("TensuraNamedMob") || self.getTags().contains("TensuraDemonLordMob")) {
            cir.setReturnValue(false);
        }
    }
}
