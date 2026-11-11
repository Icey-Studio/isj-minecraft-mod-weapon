package com.minhphuc.weapons.mixin;

import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow public abstract Vec3 getPosition();
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    @Shadow private boolean detached;

    @Inject(method = "setup", at = @At("RETURN"))
    private void weapons$onCameraSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo ci) {
        ClientCinematicSkillHandler.Pose cinematicSkillPose = ClientCinematicSkillHandler.getCameraPose(
                partialTicks,
                this.getPosition(),
                this.getYRot(),
                this.getXRot()
        );
        if (cinematicSkillPose != null) {
            this.detached = true;
            this.setRotation(cinematicSkillPose.yaw(), cinematicSkillPose.pitch());
            this.setPosition(cinematicSkillPose.position().x, cinematicSkillPose.position().y, cinematicSkillPose.position().z);
            return;
        }

        ClientHellflareCinematicHandler.Pose hellflarePose = ClientHellflareCinematicHandler.getCameraPose(
                partialTicks,
                this.getPosition(),
                this.getYRot(),
                this.getXRot()
        );
        if (hellflarePose != null) {
            this.detached = true;
            this.setRotation(hellflarePose.yaw(), hellflarePose.pitch());
            this.setPosition(hellflarePose.position().x, hellflarePose.position().y, hellflarePose.position().z);
            return;
        }

        ClientAzathothCinematicHandler.Pose azathothPose = ClientAzathothCinematicHandler.getCameraPose(
                partialTicks,
                this.getPosition(),
                this.getYRot(),
                this.getXRot()
        );
        if (azathothPose != null) {
            this.detached = true;
            this.setRotation(azathothPose.yaw(), azathothPose.pitch());
            this.setPosition(azathothPose.position().x, azathothPose.position().y, azathothPose.position().z);
            return;
        }

        ClientNihilityCinematicHandler.Pose pose = ClientNihilityCinematicHandler.getCameraPose(
                partialTicks,
                this.getPosition(),
                this.getYRot(),
                this.getXRot()
        );
        if (pose != null) {
            if (ClientNihilityCinematicHandler.isSelfCaster()) {
                this.detached = true;
            }
            this.setRotation(pose.yaw(), pose.pitch());
            this.setPosition(pose.position().x, pose.position().y, pose.position().z);
            return;
        }

        ClientOrbitalCinematicHandler.Pose orbitalPose = ClientOrbitalCinematicHandler.getCameraPose(
                partialTicks,
                this.getPosition(),
                this.getYRot(),
                this.getXRot()
        );
        if (orbitalPose != null) {
            this.detached = true;
            this.setRotation(orbitalPose.yaw(), orbitalPose.pitch());
            this.setPosition(orbitalPose.position().x, orbitalPose.position().y, orbitalPose.position().z);
        }
    }
}
