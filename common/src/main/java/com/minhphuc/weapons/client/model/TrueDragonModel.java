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
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
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
        ModelPart body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.wingLeft = body.getChild("wing_left");
        this.wingRight = body.getChild("wing_right");
        this.frontLegLeft = body.getChild("front_leg_left");
        this.frontLegRight = body.getChild("front_leg_right");
        this.hindLegLeft = body.getChild("hind_leg_left");
        this.hindLegRight = body.getChild("hind_leg_right");
        this.tailBase = body.getChild("tail_base");
        this.tailTip = this.tailBase.getChild("tail_tip");
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.neck.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.45F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.55F;
        this.head.xRot = 0.25F + headPitch * ((float) Math.PI / 180F) * 0.55F;

        boolean casting = isCastingPred.test(entity);
        this.jaw.xRot = casting ? 0.58F : 0.10F + 0.06F * Mth.sin(ageInTicks * 0.1F);

        boolean flying = isFlyingPred.test(entity) || !entity.onGround();
        float flapSpeed = flying ? 0.36F : 0.08F;
        float flapAmp = flying ? 0.62F : 0.12F;
        float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmp;
        this.wingLeft.zRot = -0.25F + flap;
        this.wingRight.zRot = 0.25F - flap;

        this.frontLegLeft.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
        this.hindLegRight.xRot = Mth.cos(limbSwing * 0.6F) * 0.7F * limbSwingAmount;
        this.frontLegRight.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;
        this.hindLegLeft.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.7F * limbSwingAmount;

        this.tailBase.yRot = Mth.sin(ageInTicks * 0.14F) * 0.22F;
        this.tailTip.yRot = Mth.sin(ageInTicks * 0.14F - 0.8F) * 0.30F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
