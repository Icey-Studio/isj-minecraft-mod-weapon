package com.minhphuc.weapons.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class MixinCreeper {

    @Shadow private int explosionRadius;
    @Shadow public abstract boolean isPowered();
    @Shadow protected abstract void spawnLingeringCloud();
    @Shadow private int swell;
    @Shadow private int oldSwell;

    /**
     * Creeper khi được ban tên hoặc tiến hóa Ma Vương có thể kích nổ vô hạn lần mà không chết.
     */
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void weapons$infiniteExplosion(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        boolean isNamed = creeper.getTags().contains("TensuraNamedMob");
        boolean isDemonLord = creeper.getTags().contains("TensuraDemonLordMob");

        if (isNamed || isDemonLord) {
            if (!creeper.level().isClientSide) {
                float powerMult = this.isPowered() ? 2.5F : 1.5F;
                if (isNamed && isDemonLord) {
                    powerMult *= 2.0F; // Ma Vương Danh Xưng nổ với uy lực cực đại!
                }
                creeper.level().explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(),
                        (float) this.explosionRadius * powerMult, Level.ExplosionInteraction.MOB);
                this.spawnLingeringCloud();

                // Hồi phục trạng thái đếm nổ để có thể tiếp tục nạp năng lượng và nổ tiếp
                this.swell = 0;
                this.oldSwell = 0;
                creeper.setSwellDir(-1);

                if (creeper.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, creeper.getX(), creeper.getY() + 0.5, creeper.getZ(), 35, 0.5, 0.5, 0.5, 0.1);
                }
            }
            ci.cancel();
        }
    }
}
