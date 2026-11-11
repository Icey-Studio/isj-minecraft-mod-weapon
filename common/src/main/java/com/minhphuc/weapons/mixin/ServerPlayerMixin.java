package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.PrimordialPossessionManager;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "setCamera", at = @At("HEAD"), cancellable = true)
    private void weapons$onSpectatorClickDemon(Entity entity, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (entity instanceof PrimordialDemonEntity demon && demon.isAlive()) {
            if (demon.isTame() && demon.isOwnedBy(player)) {
                PrimordialPossessionManager.startPossession(player, demon);
                ci.cancel();
            }
        }
    }

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void weapons$blockCreativeWhilePossessed(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (PrimordialPossessionManager.isPossessing(player) && gameType == GameType.CREATIVE) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ [KHẾ ƯỚC ÁC MA] Bạn đang nhập xác Ác Ma Thủy Tổ, không thể chuyển sang Chế Độ Sáng Tạo! Bấm [Shift] để thoát khỏi xác trước."),
                    true
            );
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void weapons$restoreModDataOnRespawn(ServerPlayer oldPlayer, boolean keepInventory, CallbackInfo ci) {
        ServerPlayer newPlayer = (ServerPlayer) (Object) this;
        CompoundTag oldData = EntityDataHelper.getCustomData(oldPlayer);
        if (!oldData.isEmpty()) {
            CompoundTag newData = EntityDataHelper.getCustomData(newPlayer);
            newData.merge(oldData.copy());

            if (PrimordialPlayerDataHelper.isDemonLord(newPlayer)) {
                var attr = newPlayer.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null && attr.getBaseValue() < 80.0D) {
                    attr.setBaseValue(80.0D);
                    newPlayer.setHealth((float) attr.getValue());
                }
                newPlayer.getAbilities().mayfly = true;
                newPlayer.getAbilities().setFlyingSpeed(0.08F);
                newPlayer.onUpdateAbilities();
            } else if (PrimordialPlayerDataHelper.isPrimordial(newPlayer)) {
                newPlayer.getAbilities().mayfly = true;
                newPlayer.getAbilities().setFlyingSpeed(0.08F);
                newPlayer.onUpdateAbilities();
            }
        }
    }
}
