package com.minhphuc.weapons.mixin;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class MixinVillager {

    /**
     * Dân làng khi bị mất kiểm soát (Cuồng Nộ) hoặc khi có mục tiêu chiến đấu sẽ không bị Brain
     * kích hoạt cơ chế hoảng loạn / bỏ chạy trước zombie, cho phép AI tấn công tiếp cận mục tiêu.
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void weapons$berserkOrFightingVillagerAi(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (villager.getTags().contains("TensuraDemonLordBerserk") || villager.getTarget() != null) {
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            villager.getBrain().eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
            villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
    }
}
