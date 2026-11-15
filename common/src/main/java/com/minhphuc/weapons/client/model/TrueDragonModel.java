package com.minhphuc.weapons.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.function.Predicate;

/**
 * Shared 3D Western Dragon model for True Dragons (Velgrynd Fire Dragon, Velzard Ice Dragon)
 * reusing the exact VeldoraDragonModel geometry & part names.
 */
public class TrueDragonModel<T extends Mob> extends EntityModel<T> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart wingLeft;
    private final ModelPart wingLeftOuter;
    private final ModelPart wingRight;
    private final ModelPart wingRightOuter;
    private final ModelPart frontLegLeft;
    private final ModelPart frontLegRight;
    private final ModelPart hindLegLeft;
    private final ModelPart hindLegRight;
    private final ModelPart tailBase;
    private final ModelPart tailTip;

    private final Predicate<T> isFlyingPred;
    private final Predicate<T> isCastingPred;

    public TrueDragonModel(ModelPart root, Predicate<T> isFlyingPred, Predicate<T> isCastingPred) {
        this.root = root;
        this.isFlyingPred = isFlyingPred;
        this.isCastingPred = isCastingPred;
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.wingLeft = this.body.getChild("wing_left");
        this.wingLeftOuter = this.wingLeft.getChild("wing_outer");
        this.wingRight = this.body.getChild("wing_right");
        this.wingRightOuter = this.wingRight.getChild("wing_outer");
        this.frontLegLeft = this.body.getChild("front_leg_left");
        this.frontLegRight = this.body.getChild("front_leg_right");
        this.hindLegLeft = this.body.getChild("hind_leg_left");
        this.hindLegRight = this.body.getChild("hind_leg_right");
        this.tailBase = this.body.getChild("tail_base");
        this.tailTip = this.tailBase.getChild("tail_tip");
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.neck.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.45F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.55F;
        this.head.xRot = 0.45F + headPitch * ((float) Math.PI / 180F) * 0.55F;

        boolean casting = isCastingPred.test(entity);
        this.jaw.xRot = casting ? 0.62F : 0.10F + 0.06F * Mth.sin(ageInTicks * 0.11F);

        boolean flying = isFlyingPred.test(entity) || !entity.onGround();
        float flapSpeed = flying ? 0.30F : 0.08F;
        float flapAmp = flying ? 0.65F : 0.14F;
        float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmp;
        float outerFlap = Mth.sin(ageInTicks * flapSpeed - 0.65F) * (flying ? 0.45F : 0.10F);

        this.wingLeft.zRot = -0.30F + flap;
        this.wingRight.zRot = 0.30F - flap;
        this.wingLeftOuter.zRot = 0.42F + outerFlap;
        this.wingRightOuter.zRot = -0.42F - outerFlap;

        this.body.y = flying ? (10.5F + Mth.cos(ageInTicks * flapSpeed) * 1.4F) : 10.5F;
        this.body.xRot = flying ? -0.06F + Mth.sin(ageInTicks * flapSpeed) * 0.05F : 0.0F;

        if (flying) {
            float tuck = 0.52F + 0.10F * Mth.sin(ageInTicks * 0.15F);
            this.frontLegLeft.xRot = tuck;
            this.frontLegRight.xRot = tuck;
            this.hindLegLeft.xRot = tuck + 0.15F;
            this.hindLegRight.xRot = tuck + 0.15F;
        } else {
            this.frontLegLeft.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
            this.hindLegRight.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
            this.frontLegRight.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;
            this.hindLegLeft.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;
        }

        this.tailBase.yRot = Mth.sin(ageInTicks * 0.16F) * 0.24F;
        this.tailBase.xRot = flying ? 0.08F * Mth.cos(ageInTicks * flapSpeed) : -0.08F;
        this.tailTip.yRot = Mth.sin(ageInTicks * 0.16F - 0.9F) * 0.34F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
