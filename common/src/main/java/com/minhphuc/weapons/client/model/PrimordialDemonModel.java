package com.minhphuc.weapons.client.model;

import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PrimordialDemonModel<T extends net.minecraft.world.entity.LivingEntity> extends HumanoidModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("weapons", "primordial_demon"), "main");

    // Noir parts (Diablo)
    public final ModelPart noirBangs;
    public final ModelPart noirTails;
    public final ModelPart noirSashKnot;
    public final ModelPart wings;
    public final ModelPart leftWing;
    public final ModelPart rightWing;
    public final ModelPart noirClawsRight;
    public final ModelPart noirClawsLeft;

    // Rouge parts (Guy Crimson)
    public final ModelPart rougeHorns;
    public final ModelPart rougeMane;
    public final ModelPart rougeCollar;
    public final ModelPart rougeCloak;

    // Blanc parts (Testarossa)
    public final ModelPart blancLongHair;
    public final ModelPart blancRibbon;
    public final ModelPart blancCoat;
    public final ModelPart blancPuffRight;
    public final ModelPart blancPuffLeft;

    // Jaune parts (Carrera)
    public final ModelPart jauneTwintailLeft;
    public final ModelPart jauneTwintailRight;
    public final ModelPart jauneEpaulets;
    public final ModelPart jauneBangs;
    public final ModelPart jauneSkirt;

    // Violet parts (Ultima)
    public final ModelPart violetBuns;
    public final ModelPart violetSkirt;
    public final ModelPart violetSleeveRight;
    public final ModelPart violetSleeveLeft;

    // Bleu parts (Rain)
    public final ModelPart bleuMaidBand;
    public final ModelPart bleuSkirt;
    public final ModelPart bleuFrillRight;
    public final ModelPart bleuFrillLeft;

    // Vert parts (Misery)
    public final ModelPart vertHair;
    public final ModelPart vertMaidBand;
    public final ModelPart vertSkirt;

    // Feminine silhouette for female demons (Blanc, Jaune, Violet, Bleu, Vert)
    public final ModelPart femaleBust;

    public PrimordialDemonModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");

        // Feminine silhouette
        this.femaleBust = body.getChild("female_bust");

        // Noir
        this.noirBangs = head.getChild("noir_bangs");
        this.noirTails = body.getChild("noir_tails");
        this.noirSashKnot = body.getChild("noir_sash_knot");
        this.wings = body.getChild("wings");
        this.leftWing = this.wings.getChild("left_wing");
        this.rightWing = this.wings.getChild("right_wing");
        this.noirClawsRight = this.rightArm.getChild("noir_claws_right");
        this.noirClawsLeft = this.leftArm.getChild("noir_claws_left");

        // Rouge
        this.rougeHorns = head.getChild("rouge_horns");
        this.rougeMane = head.getChild("rouge_mane");
        this.rougeCollar = body.getChild("rouge_collar");
        this.rougeCloak = body.getChild("rouge_cloak");

        // Blanc
        this.blancLongHair = head.getChild("blanc_long_hair");
        this.blancRibbon = head.getChild("blanc_ribbon");
        this.blancCoat = body.getChild("blanc_coat");
        this.blancPuffRight = this.rightArm.getChild("blanc_puff_right");
        this.blancPuffLeft = this.leftArm.getChild("blanc_puff_left");

        // Jaune
        this.jauneTwintailLeft = head.getChild("jaune_twintail_left");
        this.jauneTwintailRight = head.getChild("jaune_twintail_right");
        this.jauneEpaulets = body.getChild("jaune_epaulets");
        this.jauneBangs = head.getChild("jaune_bangs");
        this.jauneSkirt = body.getChild("jaune_skirt");

        // Violet
        this.violetBuns = head.getChild("violet_buns");
        this.violetSkirt = body.getChild("violet_skirt");
        this.violetSleeveRight = this.rightArm.getChild("violet_sleeve_right");
        this.violetSleeveLeft = this.leftArm.getChild("violet_sleeve_left");

        // Bleu
        this.bleuMaidBand = head.getChild("bleu_maid_band");
        this.bleuSkirt = body.getChild("bleu_skirt");
        this.bleuFrillRight = this.rightArm.getChild("bleu_frill_right");
        this.bleuFrillLeft = this.leftArm.getChild("bleu_frill_left");

        // Vert
        this.vertHair = head.getChild("vert_hair");
        this.vertMaidBand = head.getChild("vert_maid_band");
        this.vertSkirt = body.getChild("vert_skirt");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");

        // Standard 64x64 separate left arm & left leg mapping (Player model standard)
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        // --- 1. NOIR (Diablo) ---
        // 3D Overhanging Bangs & Fringe with distinct red streak fringe on character's left bangs
        head.addOrReplaceChild("noir_bangs", CubeListBuilder.create()
                .texOffs(40, 8).addBox(-4.0F, -8.0F, -4.35F, 8.0F, 5.0F, 0.45F), PartPose.ZERO);

        // Sleek Butler Tailcoat Tails with swallowtail split behind the legs
        body.addOrReplaceChild("noir_tails", CubeListBuilder.create()
                .texOffs(32, 36).addBox(-3.8F, 11.5F, 1.8F, 3.6F, 9.5F, 0.5F)
                .texOffs(36, 36).addBox(0.2F, 11.5F, 1.8F, 3.6F, 9.5F, 0.5F),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        // Center Purple Waist Sash Knot / Flap hanging down in front
        body.addOrReplaceChild("noir_sash_knot", CubeListBuilder.create()
                .texOffs(20, 36).addBox(-1.5F, 10.5F, -2.35F, 3.0F, 5.0F, 0.5F), PartPose.ZERO);

        // Large Dark Navy Blue Demon Bat Wings (Banpresto figure blocky 3D voxel wings)
        PartDefinition wings = body.addOrReplaceChild("wings", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 2.0F));
        wings.addOrReplaceChild("left_wing", CubeListBuilder.create()
                // Base bone connection from spine
                .texOffs(0, 32).addBox(0.0F, -1.0F, 0.0F, 3.0F, 2.0F, 1.0F)
                // Arching upper spar reaching up to knuckle
                .texOffs(0, 32).addBox(2.5F, -7.0F, -0.5F, 2.0F, 7.0F, 1.2F)
                // Knuckle elbow spike (sharp upward point)
                .texOffs(0, 32).addBox(3.0F, -9.0F, 0.0F, 1.2F, 2.2F, 0.8F)
                // Outer downward spar
                .texOffs(0, 32).addBox(4.0F, -6.0F, -0.5F, 2.0F, 8.0F, 1.2F)
                // 3 Tiered Downward Rib Spines with jagged ends
                .texOffs(0, 34).addBox(1.5F, 1.0F, 0.0F, 1.2F, 7.0F, 0.8F)   // Inner rib
                .texOffs(0, 34).addBox(3.2F, 2.0F, 0.0F, 1.2F, 10.0F, 0.8F)  // Middle rib
                .texOffs(0, 34).addBox(5.0F, 1.5F, 0.0F, 1.2F, 13.0F, 0.8F)  // Long outer rib
                // Voxel bat wing webbing membranes (solid 0.35F thickness)
                .texOffs(4, 34).addBox(0.5F, -5.0F, 0.2F, 5.0F, 6.0F, 0.35F) // Upper membrane
                .texOffs(4, 38).addBox(1.2F, 1.0F, 0.2F, 4.5F, 10.0F, 0.35F),// Lower membrane
                PartPose.offsetAndRotation(1.2F, 0.0F, 0.5F, 0.18F, 0.32F, 0.08F));

        wings.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 32).mirror().addBox(-3.0F, -1.0F, 0.0F, 3.0F, 2.0F, 1.0F)
                .texOffs(0, 32).mirror().addBox(-4.5F, -7.0F, -0.5F, 2.0F, 7.0F, 1.2F)
                .texOffs(0, 32).mirror().addBox(-4.2F, -9.0F, 0.0F, 1.2F, 2.2F, 0.8F)
                .texOffs(0, 32).mirror().addBox(-6.0F, -6.0F, -0.5F, 2.0F, 8.0F, 1.2F)
                .texOffs(0, 34).mirror().addBox(-2.7F, 1.0F, 0.0F, 1.2F, 7.0F, 0.8F)
                .texOffs(0, 34).mirror().addBox(-4.4F, 2.0F, 0.0F, 1.2F, 10.0F, 0.8F)
                .texOffs(0, 34).mirror().addBox(-6.2F, 1.5F, 0.0F, 1.2F, 13.0F, 0.8F)
                .texOffs(4, 34).mirror().addBox(-5.5F, -5.0F, 0.2F, 5.0F, 6.0F, 0.35F)
                .texOffs(4, 38).mirror().addBox(-5.7F, 1.0F, 0.2F, 4.5F, 10.0F, 0.35F),
                PartPose.offsetAndRotation(-1.2F, 0.0F, 0.5F, 0.18F, -0.32F, -0.08F));

        // Noir Right Demon Claw Hand (Gunmetal Gauntlet with 5 Long Articulated Silver Bone Claws)
        PartDefinition rightArm = partdefinition.getChild("right_arm");
        rightArm.addOrReplaceChild("noir_claws_right", CubeListBuilder.create()
                // Dark iron wrist gauntlet band with crimson demon gem
                .texOffs(56, 16).addBox(-3.3F, 7.5F, -2.3F, 4.6F, 2.8F, 4.6F, new CubeDeformation(0.15F))
                // Palm knuckle plate
                .texOffs(56, 24).addBox(-3.1F, 9.8F, -2.1F, 4.2F, 1.5F, 4.2F)
                // 5 Articulated Skeletal Claw Fingers
                // Thumb (curving inward)
                .texOffs(56, 26).addBox(0.4F, 9.5F, -1.0F, 0.7F, 4.5F, 0.7F)
                .texOffs(56, 29).addBox(0.4F, 13.8F, -1.8F, 0.5F, 3.5F, 0.5F)
                // Index Finger
                .texOffs(56, 26).addBox(-0.4F, 10.0F, -1.2F, 0.7F, 5.5F, 0.7F)
                .texOffs(56, 29).addBox(-0.4F, 15.2F, -2.2F, 0.5F, 4.5F, 0.5F)
                // Middle Finger (Longest)
                .texOffs(56, 26).addBox(-1.3F, 10.0F, -1.2F, 0.7F, 6.5F, 0.7F)
                .texOffs(56, 29).addBox(-1.3F, 16.2F, -2.4F, 0.5F, 5.0F, 0.5F)
                // Ring Finger
                .texOffs(56, 26).addBox(-2.1F, 10.0F, -1.0F, 0.7F, 5.5F, 0.7F)
                .texOffs(56, 29).addBox(-2.1F, 15.2F, -2.0F, 0.5F, 4.5F, 0.5F)
                // Pinky
                .texOffs(56, 26).addBox(-2.8F, 9.5F, -0.8F, 0.6F, 4.5F, 0.6F)
                .texOffs(56, 29).addBox(-2.8F, 13.8F, -1.6F, 0.5F, 3.5F, 0.5F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.12F, 0.0F, 0.0F));

        // Blanc Puffy Shoulder Sleeve Right (Ball gown puff sleeves)
        rightArm.addOrReplaceChild("blanc_puff_right", CubeListBuilder.create()
                .texOffs(40, 32).addBox(-3.5F, -2.5F, -2.5F, 4.8F, 4.8F, 5.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO);

        // Violet Detached Ruffled Sleeve Right
        rightArm.addOrReplaceChild("violet_sleeve_right", CubeListBuilder.create()
                .texOffs(40, 32).addBox(-3.3F, 3.5F, -2.3F, 4.6F, 5.5F, 4.6F, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        // Bleu Shoulder Frill Right
        rightArm.addOrReplaceChild("bleu_frill_right", CubeListBuilder.create()
                .texOffs(40, 32).addBox(-3.2F, -2.2F, -2.2F, 4.4F, 3.0F, 4.4F, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        // Left hand demon claw gauntlet (mirrored for dual-claw Noir possession & first-person left hand)
        PartDefinition leftArm = partdefinition.getChild("left_arm");
        leftArm.addOrReplaceChild("noir_claws_left", CubeListBuilder.create()
                .texOffs(56, 16).mirror().addBox(-1.3F, 7.5F, -2.3F, 4.6F, 2.8F, 4.6F, new CubeDeformation(0.15F))
                .texOffs(56, 24).mirror().addBox(-1.1F, 9.8F, -2.1F, 4.2F, 1.5F, 4.2F)
                .texOffs(56, 26).mirror().addBox(-1.1F, 9.5F, -1.0F, 0.7F, 4.5F, 0.7F)
                .texOffs(56, 29).mirror().addBox(-0.9F, 13.8F, -1.8F, 0.5F, 3.5F, 0.5F)
                .texOffs(56, 26).mirror().addBox(-0.3F, 10.0F, -1.2F, 0.7F, 5.5F, 0.7F)
                .texOffs(56, 29).mirror().addBox(-0.1F, 15.2F, -2.2F, 0.5F, 4.5F, 0.5F)
                .texOffs(56, 26).mirror().addBox(0.6F, 10.0F, -1.2F, 0.7F, 6.5F, 0.7F)
                .texOffs(56, 29).mirror().addBox(0.8F, 16.2F, -2.4F, 0.5F, 5.0F, 0.5F)
                .texOffs(56, 26).mirror().addBox(1.4F, 10.0F, -1.0F, 0.7F, 5.5F, 0.7F)
                .texOffs(56, 29).mirror().addBox(1.6F, 15.2F, -2.0F, 0.5F, 4.5F, 0.5F)
                .texOffs(56, 26).mirror().addBox(2.2F, 9.5F, -0.8F, 0.6F, 4.5F, 0.6F)
                .texOffs(56, 29).mirror().addBox(2.3F, 13.8F, -1.6F, 0.5F, 3.5F, 0.5F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.12F, 0.0F, 0.0F));

        // Blanc Puffy Shoulder Sleeve Left (Ball gown puff sleeves)
        leftArm.addOrReplaceChild("blanc_puff_left", CubeListBuilder.create()
                .texOffs(40, 32).mirror().addBox(-1.3F, -2.5F, -2.5F, 4.8F, 4.8F, 5.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO);

        // Violet Detached Ruffled Sleeve Left
        leftArm.addOrReplaceChild("violet_sleeve_left", CubeListBuilder.create()
                .texOffs(40, 32).mirror().addBox(-1.3F, 3.5F, -2.3F, 4.6F, 5.5F, 4.6F, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        // Bleu Shoulder Frill Left
        leftArm.addOrReplaceChild("bleu_frill_left", CubeListBuilder.create()
                .texOffs(40, 32).mirror().addBox(-1.2F, -2.2F, -2.2F, 4.4F, 3.0F, 4.4F, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        // --- 2. ROUGE (Guy Crimson) ---
        // Small black demon horns protruding from top head
        head.addOrReplaceChild("rouge_horns", CubeListBuilder.create()
                .texOffs(56, 12).addBox(-3.5F, -9.8F, -2.5F, 1.4F, 2.2F, 1.4F)
                .texOffs(56, 12).mirror().addBox(2.1F, -9.8F, -2.5F, 1.4F, 2.2F, 1.4F),
                PartPose.ZERO);
        // Messy crimson hair with long spiky ponytail down the back & side locks
        head.addOrReplaceChild("rouge_mane", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-3.0F, -8.5F, 3.8F, 6.0F, 15.0F, 1.6F)
                .texOffs(32, 0).addBox(-4.5F, -8.0F, -3.5F, 0.8F, 8.5F, 7.0F)
                .texOffs(32, 0).mirror().addBox(3.7F, -8.0F, -3.5F, 0.8F, 8.5F, 7.0F),
                PartPose.rotation(0.06F, 0.0F, 0.0F));
        // Open crimson coat collar & shoulder lapels framing bare muscular chest
        body.addOrReplaceChild("rouge_collar", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-4.8F, -0.6F, -2.6F, 2.5F, 4.0F, 5.2F, new CubeDeformation(0.2F))
                .texOffs(28, 32).addBox(2.3F, -0.6F, -2.6F, 2.5F, 4.0F, 5.2F, new CubeDeformation(0.2F))
                .texOffs(20, 32).addBox(-4.5F, -0.6F, 1.8F, 9.0F, 3.5F, 1.2F, new CubeDeformation(0.2F)),
                PartPose.ZERO);
        // Long crimson coat hem with swallowtail split on the back & open front
        body.addOrReplaceChild("rouge_cloak", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.5F, 11.0F, -2.2F, 1.0F, 5.5F, 4.4F)
                .texOffs(28, 36).addBox(3.5F, 11.0F, -2.2F, 1.0F, 5.5F, 4.4F)
                .texOffs(32, 36).addBox(-4.2F, 11.0F, 1.8F, 3.8F, 9.5F, 0.6F)
                .texOffs(36, 36).addBox(0.4F, 11.0F, 1.8F, 3.8F, 9.5F, 0.6F),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        // --- 3. BLANC (Testarossa) ---
        // Long flowing platinum white hair down the back past the waist
        head.addOrReplaceChild("blanc_long_hair", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-4.0F, -1.5F, 3.8F, 8.0F, 16.0F, 1.0F),
                PartPose.rotation(0.08F, 0.0F, 0.0F));
        // Small beige gold hair ornament clip on right side of bangs
        head.addOrReplaceChild("blanc_ribbon", CubeListBuilder.create()
                .texOffs(34, 10).addBox(-3.5F, -7.5F, -4.3F, 2.5F, 2.0F, 0.6F),
                PartPose.ZERO);
        // Noble pure white gown with floor-length flowing skirt
        body.addOrReplaceChild("blanc_coat", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.8F, 11.0F, -2.8F, 9.6F, 13.0F, 5.6F, new CubeDeformation(0.3F)),
                PartPose.rotation(0.05F, 0.0F, 0.0F));

        // --- 4. JAUNE (Carrera) ---
        // 3D Straight bangs & side bob hair framing face
        head.addOrReplaceChild("jaune_bangs", CubeListBuilder.create()
                .texOffs(40, 8).addBox(-4.0F, -8.0F, -4.35F, 8.0F, 4.5F, 0.45F), PartPose.ZERO);
        head.addOrReplaceChild("jaune_twintail_left", CubeListBuilder.create()
                .texOffs(48, 8).addBox(3.6F, -7.5F, -3.5F, 0.7F, 7.5F, 6.5F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.02F, 0.0F, -0.04F));
        head.addOrReplaceChild("jaune_twintail_right", CubeListBuilder.create()
                .texOffs(32, 8).mirror().addBox(-4.3F, -7.5F, -3.5F, 0.7F, 7.5F, 6.5F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.02F, 0.0F, 0.04F));
        // Fluffy fur-trimmed winter collar wrapping shoulders & back of neck/hood
        body.addOrReplaceChild("jaune_epaulets", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-5.2F, -0.9F, -3.2F, 10.4F, 3.6F, 6.4F, new CubeDeformation(0.25F))
                .texOffs(16, 32).addBox(-4.2F, -1.2F, 2.2F, 8.4F, 4.2F, 1.8F, new CubeDeformation(0.2F)),
                PartPose.ZERO);
        // Pleated dark grey mini skirt
        body.addOrReplaceChild("jaune_skirt", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.5F, 10.5F, -2.5F, 9.0F, 4.5F, 5.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);

        // --- 5. VIOLET (Ultima) ---
        // Side ponytail with cute black ribbon on head left side (viewer right in front)
        head.addOrReplaceChild("violet_buns", CubeListBuilder.create()
                // Black ribbon bow
                .texOffs(33, 2).addBox(3.5F, -9.0F, -1.2F, 2.8F, 2.8F, 2.8F)
                // Flowing high side ponytail dropping past shoulder
                .texOffs(57, 2).addBox(4.0F, -8.0F, -0.5F, 2.4F, 11.0F, 2.5F),
                PartPose.rotation(0.05F, 0.0F, 0.06F));
        // Layered gothic lolita ruffles skirt
        body.addOrReplaceChild("violet_skirt", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.8F, 10.5F, -2.8F, 9.6F, 7.0F, 5.6F, new CubeDeformation(0.35F)),
                PartPose.ZERO);

        // --- 6. BLEU (Rain) ---
        // White frill maid headband
        head.addOrReplaceChild("bleu_maid_band", CubeListBuilder.create()
                .texOffs(39, 0).addBox(-4.5F, -9.0F, -2.0F, 9.0F, 2.2F, 4.0F, new CubeDeformation(0.22F)),
                PartPose.ZERO);
        // Flared maid dress with white apron and big white ribbon bow on back
        body.addOrReplaceChild("bleu_skirt", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.6F, 11.0F, -2.6F, 9.2F, 8.0F, 5.2F, new CubeDeformation(0.25F))
                // Big white ribbon bow on lower back of waist
                .texOffs(34, 10).addBox(-3.0F, 10.5F, 2.4F, 6.0F, 3.5F, 1.4F),
                PartPose.ZERO);

        // --- 7. VERT (Misery) ---
        // Long wavy emerald hair down back + 2 front locks framing chest
        head.addOrReplaceChild("vert_hair", CubeListBuilder.create()
                // Back hair
                .texOffs(56, 0).addBox(-4.0F, -2.0F, 3.6F, 8.0F, 15.0F, 1.2F)
                // Front chest locks
                .texOffs(56, 0).addBox(-3.8F, -1.0F, -4.2F, 1.5F, 8.0F, 0.8F)
                .texOffs(56, 0).mirror().addBox(2.3F, -1.0F, -4.2F, 1.5F, 8.0F, 0.8F),
                PartPose.rotation(0.06F, 0.0F, 0.0F));
        head.addOrReplaceChild("vert_maid_band", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-4.5F, -8.8F, -2.0F, 9.0F, 2.2F, 4.0F, new CubeDeformation(0.22F)),
                PartPose.ZERO);
        // Floor-length head maid skirt
        body.addOrReplaceChild("vert_skirt", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.5F, 11.0F, -2.5F, 9.0F, 13.0F, 5.0F, new CubeDeformation(0.3F))
                // White apron back bow
                .texOffs(34, 10).addBox(-2.5F, 10.5F, 2.4F, 5.0F, 3.0F, 1.2F),
                PartPose.ZERO);

        // --- 8. FEMININE SILHOUETTE FOR FEMALE DEMONS (Blanc, Jaune, Violet, Bleu, Vert) ---
        body.addOrReplaceChild("female_bust", CubeListBuilder.create()
                .texOffs(20, 21).addBox(-3.5F, 1.8F, -2.8F, 7.0F, 3.5F, 1.8F, new CubeDeformation(0.02F)),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        DemonType type = (entity instanceof PrimordialDemonEntity pde) ? pde.getDemonType() : DemonType.NOIR;
        if (type == null) type = DemonType.NOIR;

        boolean isNoir = (type == DemonType.NOIR);
        boolean isRouge = (type == DemonType.ROUGE);
        boolean isBlanc = (type == DemonType.BLANC);
        boolean isJaune = (type == DemonType.JAUNE);
        boolean isViolet = (type == DemonType.VIOLET);
        boolean isBleu = (type == DemonType.BLEU);
        boolean isVert = (type == DemonType.VERT);

        // Female silhouette active only for the 5 female demons (Blanc, Jaune, Violet, Bleu, Vert)
        this.femaleBust.visible = type.isFemale();

        // Noir
        this.noirBangs.visible = isNoir;
        this.noirTails.visible = isNoir;
        this.noirSashKnot.visible = isNoir;
        this.wings.visible = isNoir;
        this.noirClawsRight.visible = isNoir;
        this.noirClawsLeft.visible = false;

        // Rouge
        this.rougeHorns.visible = isRouge;
        this.rougeMane.visible = isRouge;
        this.rougeCollar.visible = isRouge;
        this.rougeCloak.visible = isRouge;

        // Blanc
        this.blancLongHair.visible = isBlanc;
        this.blancRibbon.visible = isBlanc;
        this.blancCoat.visible = isBlanc;
        this.blancPuffRight.visible = isBlanc;
        this.blancPuffLeft.visible = isBlanc;

        // Jaune
        this.jauneBangs.visible = isJaune;
        this.jauneTwintailLeft.visible = isJaune;
        this.jauneTwintailRight.visible = isJaune;
        this.jauneEpaulets.visible = isJaune;
        this.jauneSkirt.visible = isJaune;

        // Violet
        this.violetBuns.visible = isViolet;
        this.violetSkirt.visible = isViolet;
        this.violetSleeveRight.visible = isViolet;
        this.violetSleeveLeft.visible = isViolet;

        // Bleu
        this.bleuMaidBand.visible = isBleu;
        this.bleuSkirt.visible = isBleu;
        this.bleuFrillRight.visible = isBleu;
        this.bleuFrillLeft.visible = isBleu;

        // Vert
        this.vertHair.visible = isVert;
        this.vertMaidBand.visible = isVert;
        this.vertSkirt.visible = isVert;

        // --- ANIMATIONS ---
        if (this.wings.visible) {
            boolean isMovingInAir = entity.isFallFlying() || !entity.onGround();
            float speed = isMovingInAir ? 0.30F : 0.16F;
            float flap = Mth.sin(ageInTicks * speed) * (isMovingInAir ? 0.32F : 0.18F);
            float flapZ = Mth.cos(ageInTicks * speed) * (isMovingInAir ? 0.18F : 0.08F);
            this.leftWing.yRot = 0.32F + flap;
            this.leftWing.zRot = 0.08F + flapZ;
            this.rightWing.yRot = -0.32F - flap;
            this.rightWing.zRot = -0.08F - flapZ;
        }

        // Jaune side bob hair & pleated skirt natural movement
        if (isJaune) {
            float sway = Mth.sin(limbSwing * 0.6662F) * 0.06F * limbSwingAmount;
            this.jauneTwintailLeft.zRot = -0.04F - sway;
            this.jauneTwintailRight.zRot = 0.04F + sway;
            this.jauneSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }

        // Blanc long hair drifting animation
        if (isBlanc) {
            this.blancLongHair.xRot = 0.08F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }

        // Vert long wavy hair
        if (isVert) {
            this.vertHair.xRot = 0.06F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        }

        // Violet side ponytail swaying animation
        if (isViolet) {
            this.violetBuns.zRot = 0.05F + Mth.sin(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }

        // Coat tails and skirts natural movement
        if (isNoir) {
            this.noirTails.xRot = 0.08F + Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
        } else if (isRouge) {
            this.rougeCloak.xRot = 0.08F + Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
            this.rougeMane.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        } else if (isBlanc) {
            this.blancCoat.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        } else if (isBleu) {
            this.bleuSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        } else if (isVert) {
            this.vertSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        } else if (isViolet) {
            this.violetSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }

        // Ritual rising pose
        if (entity instanceof PrimordialDemonEntity pde && pde.isRising()) {
            this.head.xRot = -0.2F;
            this.leftArm.xRot = 0.1F;
            this.rightArm.xRot = 0.1F;
            this.leftLeg.xRot = 0.0F;
            this.rightLeg.xRot = 0.0F;
        }
    }

    public void setupAnimForPossessed(DemonType type, boolean hasBody, boolean isWinged, T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Chạy đầy đủ hoạt ảnh HumanoidModel gốc (bước đi, vung tay đánh, cúi người) VÀ kích hoạt DeathBlessingPoseMixin cho các chiêu dài!
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (type == null) type = DemonType.NOIR;

        boolean isNoir = (type == DemonType.NOIR);
        boolean isRouge = (type == DemonType.ROUGE);
        boolean isBlanc = (type == DemonType.BLANC);
        boolean isJaune = (type == DemonType.JAUNE);
        boolean isViolet = (type == DemonType.VIOLET);
        boolean isBleu = (type == DemonType.BLEU);
        boolean isVert = (type == DemonType.VERT);

        // Female silhouette active only for the 5 female demons
        this.femaleBust.visible = type.isFemale();

        // Noir
        this.noirBangs.visible = isNoir;
        this.noirTails.visible = isNoir;
        this.noirSashKnot.visible = isNoir;
        this.wings.visible = isWinged || isNoir;
        this.noirClawsRight.visible = isNoir;
        this.noirClawsLeft.visible = isNoir;

        // Rouge
        this.rougeHorns.visible = isRouge;
        this.rougeMane.visible = isRouge;
        this.rougeCollar.visible = isRouge;
        this.rougeCloak.visible = isRouge;

        // Blanc
        this.blancLongHair.visible = isBlanc;
        this.blancRibbon.visible = isBlanc;
        this.blancCoat.visible = isBlanc;
        this.blancPuffRight.visible = isBlanc;
        this.blancPuffLeft.visible = isBlanc;

        // Jaune
        this.jauneBangs.visible = isJaune;
        this.jauneTwintailLeft.visible = isJaune;
        this.jauneTwintailRight.visible = isJaune;
        this.jauneEpaulets.visible = isJaune;
        this.jauneSkirt.visible = isJaune;

        // Violet
        this.violetBuns.visible = isViolet;
        this.violetSkirt.visible = isViolet;
        this.violetSleeveRight.visible = isViolet;
        this.violetSleeveLeft.visible = isViolet;

        // Bleu
        this.bleuMaidBand.visible = isBleu;
        this.bleuSkirt.visible = isBleu;
        this.bleuFrillRight.visible = isBleu;
        this.bleuFrillLeft.visible = isBleu;

        // Vert
        this.vertHair.visible = isVert;
        this.vertMaidBand.visible = isVert;
        this.vertSkirt.visible = isVert;

        // --- ANIMATIONS ---
        if (this.wings.visible) {
            boolean isMovingInAir = player.isFallFlying() || !player.onGround();
            float speed = isMovingInAir ? 0.30F : 0.16F;
            float flap = Mth.sin(ageInTicks * speed) * (isMovingInAir ? 0.32F : 0.18F);
            float flapZ = Mth.cos(ageInTicks * speed) * (isMovingInAir ? 0.18F : 0.08F);
            this.leftWing.yRot = 0.32F + flap;
            this.leftWing.zRot = 0.08F + flapZ;
            this.rightWing.yRot = -0.32F - flap;
            this.rightWing.zRot = -0.08F - flapZ;
        }

        if (isJaune) {
            float sway = Mth.sin(limbSwing * 0.6662F) * 0.06F * limbSwingAmount;
            this.jauneTwintailLeft.zRot = -0.04F - sway;
            this.jauneTwintailRight.zRot = 0.04F + sway;
            this.jauneSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }
        if (isBlanc) {
            this.blancLongHair.xRot = 0.08F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }
        if (isVert) {
            this.vertHair.xRot = 0.06F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        }
        if (isViolet) {
            this.violetBuns.zRot = 0.05F + Mth.sin(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }
        if (isNoir) {
            this.noirTails.xRot = 0.08F + Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
        } else if (isRouge) {
            this.rougeCloak.xRot = 0.08F + Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
            this.rougeMane.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        } else if (isBlanc) {
            this.blancCoat.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        } else if (isBleu) {
            this.bleuSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        } else if (isVert) {
            this.vertSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.1F * limbSwingAmount;
        } else if (isViolet) {
            this.violetSkirt.xRot = 0.05F + Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        }
    }
}
