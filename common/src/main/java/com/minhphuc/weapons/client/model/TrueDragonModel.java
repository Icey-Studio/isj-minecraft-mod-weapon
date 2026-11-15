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
 * reusing the exact VeldoraDragonModel geometry & proportions.
 */
public class TrueDragonModel<T extends Mob> extends EntityModel<T> {

    private final ModelPart root;
    private final ModelPart headPivot;
    private final ModelPart leftWingPivot;
    private final ModelPart rightWingPivot;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart legFL;
    private final ModelPart legFR;
    private final ModelPart legBL;
    private final ModelPart legBR;

    private final Predicate<T> isFlyingPred;
    private final Predicate<T> isCastingPred;

    public TrueDragonModel(ModelPart root, Predicate<T> isFlyingPred, Predicate<T> isCastingPred) {
        this.root = root;
        this.isFlyingPred = isFlyingPred;
        this.isCastingPred = isCastingPred;
        ModelPart body = root.getChild("body");
        ModelPart neck = body.getChild("neck");
        this.headPivot = neck.getChild("head");
        this.leftWingPivot = body.getChild("left_wing");
        this.rightWingPivot = body.getChild("right_wing");
        this.tail1 = body.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.legFL = body.getChild("leg_fl");
        this.legFR = body.getChild("leg_fr");
        this.legBL = body.getChild("leg_bl");
        this.legBR = body.getChild("leg_br");
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.headPivot.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.65F;
        this.headPivot.xRot = headPitch * ((float) Math.PI / 180F) * 0.55F;

        boolean flying = isFlyingPred.test(entity) || !entity.onGround();
        boolean casting = isCastingPred.test(entity);

        if (flying) {
            float flap = Mth.sin(ageInTicks * 0.28F) * 0.48F;
            this.leftWingPivot.zRot = -0.22F + flap;
            this.rightWingPivot.zRot = 0.22F - flap;
            this.leftWingPivot.yRot = -0.15F;
            this.rightWingPivot.yRot = 0.15F;
        } else {
            float breath = Mth.sin(ageInTicks * 0.08F) * 0.06F;
            this.leftWingPivot.zRot = -0.58F + breath;
            this.rightWingPivot.zRot = 0.58F - breath;
            this.leftWingPivot.yRot = -0.45F;
            this.rightWingPivot.yRot = 0.45F;
        }

        if (casting) {
            this.headPivot.xRot -= 0.35F;
            this.leftWingPivot.zRot = 0.2F;
            this.rightWingPivot.zRot = -0.2F;
        }

        float sway = Mth.sin(ageInTicks * 0.12F) * 0.18F;
        this.tail1.yRot = sway;
        this.tail2.yRot = sway * 1.35F;

        this.legFL.xRot = Mth.cos(limbSwing * 0.5F) * 0.65F * limbSwingAmount;
        this.legFR.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.65F * limbSwingAmount;
        this.legBL.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.65F * limbSwingAmount;
        this.legBR.xRot = Mth.cos(limbSwing * 0.5F) * 0.65F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
