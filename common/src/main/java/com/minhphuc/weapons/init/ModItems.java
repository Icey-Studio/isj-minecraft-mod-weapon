package com.minhphuc.weapons.init;

import com.minhphuc.weapons.WeaponsMod;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(WeaponsMod.MOD_ID, Registries.ITEM);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(WeaponsMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    // ==========================================
    // MODULE: GUIDE BOOK (THÁNH THƯ THẦN KHÍ)
    // ==========================================
    public static final RegistrySupplier<Item> GUIDE_BOOK = ITEMS.register("celestial_tome",
            () -> new com.minhphuc.weapons.content.guide.GuideBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ==========================================
    // ==========================================
    // MODULE: TENSURA DEMON LORD AWAKENING
    // ==========================================
    public static final RegistrySupplier<Item> DEMON_LORD_AWAKENING_ORB = ITEMS.register("demon_lord_awakening_orb",
            () -> new com.minhphuc.weapons.content.tensura.DemonLordAwakeningOrbItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    // MODULE: TENSURA DEMON LORD EVOLUTION
    // ==========================================
    public static final RegistrySupplier<Item> DEMON_LORD_SEED = ITEMS.register("demon_lord_seed",
            () -> new com.minhphuc.weapons.content.tensura.DemonLordSeedItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DEMON_LORD_SOUL = ITEMS.register("demon_lord_soul",
            () -> new com.minhphuc.weapons.content.tensura.DemonLordSoulItem(new Item.Properties()));

    public static final RegistrySupplier<Item> MARIABELL_SOUL = ITEMS.register("mariabell_soul",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(16)));

    public static final RegistrySupplier<Item> GREED_AWAKENING_ORB = ITEMS.register("greed_awakening_orb",
            () -> new com.minhphuc.weapons.content.tensura.GreedAwakeningOrbItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistrySupplier<Item> MILIM_DRAGON_CONTRACT = ITEMS.register("milim_dragon_contract",
            () -> new com.minhphuc.weapons.content.tensura.MilimDragonContractItem(new Item.Properties()));

    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(WeaponsMod.MOD_ID, Registries.ARMOR_MATERIAL);

    // ==========================================
    // MODULE: DIVINE ARMOR & MOONLIGHT SWORD (THẦN THOẠI)
    // ==========================================
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> DIVINE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("divine", () -> {
        java.util.Map<net.minecraft.world.item.ArmorItem.Type, Integer> defense = java.util.Map.of(
                net.minecraft.world.item.ArmorItem.Type.BOOTS, 5,
                net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 8,
                net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 10,
                net.minecraft.world.item.ArmorItem.Type.HELMET, 5
        );
        return new net.minecraft.world.item.ArmorMaterial(
                defense,
                25,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "divine"))),
                4.0F,
                0.2F
        );
    });

    public static final RegistrySupplier<Item> DIVINE_HELMET = ITEMS.register("divine_helmet",
            () -> new com.minhphuc.weapons.content.divine.DivineArmorItem(DIVINE_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> DIVINE_CHESTPLATE = ITEMS.register("divine_chestplate",
            () -> new com.minhphuc.weapons.content.divine.DivineArmorItem(DIVINE_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> DIVINE_LEGGINGS = ITEMS.register("divine_leggings",
            () -> new com.minhphuc.weapons.content.divine.DivineArmorItem(DIVINE_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> DIVINE_BOOTS = ITEMS.register("divine_boots",
            () -> new com.minhphuc.weapons.content.divine.DivineArmorItem(DIVINE_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> MOONLIGHT_SWORD = ITEMS.register("moonlight_sword",
            () -> new com.minhphuc.weapons.content.divine.MoonlightSwordItem(new Item.Properties()));

    // ==========================================
    // MODULE: HINATA SAKAGUCHI LEGENDARY SET
    // ==========================================
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> HINATA_ARMOR_MATERIAL = ARMOR_MATERIALS.register("hinata", () -> {
        java.util.Map<net.minecraft.world.item.ArmorItem.Type, Integer> defense = java.util.Map.of(
                net.minecraft.world.item.ArmorItem.Type.BOOTS, 6,
                net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 9,
                net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 12,
                net.minecraft.world.item.ArmorItem.Type.HELMET, 6
        );
        return new net.minecraft.world.item.ArmorMaterial(
                defense,
                25,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "hinata"))),
                5.0F,
                0.25F
        );
    });

    public static final RegistrySupplier<Item> HINATA_HELMET = ITEMS.register("hinata_helmet",
            () -> new com.minhphuc.weapons.content.divine.HinataArmorItem(HINATA_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> HINATA_CHESTPLATE = ITEMS.register("hinata_chestplate",
            () -> new com.minhphuc.weapons.content.divine.HinataArmorItem(HINATA_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> HINATA_LEGGINGS = ITEMS.register("hinata_leggings",
            () -> new com.minhphuc.weapons.content.divine.HinataArmorItem(HINATA_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> HINATA_BOOTS = ITEMS.register("hinata_boots",
            () -> new com.minhphuc.weapons.content.divine.HinataArmorItem(HINATA_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> HINATA_SWORD = ITEMS.register("hinata_sword",
            () -> new com.minhphuc.weapons.content.divine.HinataSwordItem(new Item.Properties()));

    // ==========================================
    // MODULE: 7 BỘ TRANG BỊ ÁC MA THỦY TỔ HUYỀN THOẠI (28 MÓN)
    // ==========================================
    private static RegistrySupplier<net.minecraft.world.item.ArmorMaterial> registerPrimordialArmorMaterial(String name) {
        return ARMOR_MATERIALS.register("primordial_" + name, () -> {
            java.util.Map<net.minecraft.world.item.ArmorItem.Type, Integer> defense = java.util.Map.of(
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, 4,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 7,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 9,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, 4
            );
            return new net.minecraft.world.item.ArmorMaterial(
                    defense,
                    25,
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "primordial_" + name))),
                    3.5F,
                    0.15F
            );
        });
    }

    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_NOIR_MATERIAL = registerPrimordialArmorMaterial("noir");
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_ROUGE_MATERIAL = registerPrimordialArmorMaterial("rouge");
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_BLANC_MATERIAL = registerPrimordialArmorMaterial("blanc");
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_JAUNE_MATERIAL = registerPrimordialArmorMaterial("jaune");
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_VIOLET_MATERIAL = registerPrimordialArmorMaterial("violet");
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_BLEU_MATERIAL = registerPrimordialArmorMaterial("bleu");
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> PRIMORDIAL_VERT_MATERIAL = registerPrimordialArmorMaterial("vert");

    // 1. Bộ Noir (Diablo)
    public static final RegistrySupplier<Item> NOIR_HELMET = ITEMS.register("noir_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_NOIR_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.NOIR, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> NOIR_CHESTPLATE = ITEMS.register("noir_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_NOIR_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.NOIR, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> NOIR_LEGGINGS = ITEMS.register("noir_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_NOIR_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.NOIR, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> NOIR_BOOTS = ITEMS.register("noir_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_NOIR_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.NOIR, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // 2. Bộ Rouge (Guy Crimson)
    public static final RegistrySupplier<Item> ROUGE_HELMET = ITEMS.register("rouge_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_ROUGE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.ROUGE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> ROUGE_CHESTPLATE = ITEMS.register("rouge_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_ROUGE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.ROUGE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> ROUGE_LEGGINGS = ITEMS.register("rouge_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_ROUGE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.ROUGE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> ROUGE_BOOTS = ITEMS.register("rouge_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_ROUGE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.ROUGE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // 3. Bộ Blanc (Testarossa)
    public static final RegistrySupplier<Item> BLANC_HELMET = ITEMS.register("blanc_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLANC_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLANC, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> BLANC_CHESTPLATE = ITEMS.register("blanc_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLANC_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLANC, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> BLANC_LEGGINGS = ITEMS.register("blanc_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLANC_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLANC, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> BLANC_BOOTS = ITEMS.register("blanc_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLANC_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLANC, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // 4. Bộ Jaune (Carrera)
    public static final RegistrySupplier<Item> JAUNE_HELMET = ITEMS.register("jaune_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_JAUNE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.JAUNE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> JAUNE_CHESTPLATE = ITEMS.register("jaune_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_JAUNE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.JAUNE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> JAUNE_LEGGINGS = ITEMS.register("jaune_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_JAUNE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.JAUNE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> JAUNE_BOOTS = ITEMS.register("jaune_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_JAUNE_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.JAUNE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // 5. Bộ Violet (Ultima)
    public static final RegistrySupplier<Item> VIOLET_HELMET = ITEMS.register("violet_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VIOLET_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VIOLET, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> VIOLET_CHESTPLATE = ITEMS.register("violet_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VIOLET_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VIOLET, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> VIOLET_LEGGINGS = ITEMS.register("violet_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VIOLET_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VIOLET, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> VIOLET_BOOTS = ITEMS.register("violet_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VIOLET_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VIOLET, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // 6. Bộ Bleu (Rain)
    public static final RegistrySupplier<Item> BLEU_HELMET = ITEMS.register("bleu_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLEU_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLEU, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> BLEU_CHESTPLATE = ITEMS.register("bleu_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLEU_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLEU, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> BLEU_LEGGINGS = ITEMS.register("bleu_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLEU_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLEU, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> BLEU_BOOTS = ITEMS.register("bleu_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_BLEU_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.BLEU, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // 7. Bộ Vert (Misery)
    public static final RegistrySupplier<Item> VERT_HELMET = ITEMS.register("vert_helmet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VERT_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VERT, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> VERT_CHESTPLATE = ITEMS.register("vert_chestplate",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VERT_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VERT, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> VERT_LEGGINGS = ITEMS.register("vert_leggings",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VERT_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VERT, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> VERT_BOOTS = ITEMS.register("vert_boots",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialArmorItem(PRIMORDIAL_VERT_MATERIAL, com.minhphuc.weapons.entity.tensura.DemonType.VERT, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_MAGIC_CIRCLE = ITEMS.register("disintegration_magic_circle",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_GROUND_ARRAY = ITEMS.register("disintegration_ground_array",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_RUNE_RING = ITEMS.register("disintegration_rune_ring",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_CROWN_RING = ITEMS.register("disintegration_crown_ring",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_VERTICAL_CREST = ITEMS.register("disintegration_vertical_crest",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_LIGHT_BEAM = ITEMS.register("disintegration_light_beam",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DISINTEGRATION_SHOCKWAVE = ITEMS.register("disintegration_shockwave",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BEELZEBUTH_MAGIC_CIRCLE = ITEMS.register("beelzebuth_magic_circle",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BEELZEBUTH_DRAGON_MAW = ITEMS.register("beelzebuth_dragon_maw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> JACOB_LIGHT_PILLAR = ITEMS.register("jacob_light_pillar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TAISUI_EYE_PLANET = ITEMS.register("taisui_eye_planet",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIUREN_MAGIC_ARRAY = ITEMS.register("liuren_magic_array",
            () -> new Item(new Item.Properties()));

    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<Item>[] SHIKIGAMI_GUARDIANS = new RegistrySupplier[12];
    static {
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            SHIKIGAMI_GUARDIANS[idx] = ITEMS.register("shikigami_guardian_" + idx, () -> new Item(new Item.Properties()));
        }
    }

    public static final RegistrySupplier<Item> SHIKIGAMI_GUARDIAN = SHIKIGAMI_GUARDIANS[0];

    public static final RegistrySupplier<Item> TAISUI_SEER_FLESH = ITEMS.register("seer_flesh",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SEVERED_HEART = ITEMS.register("severed_heart",
            () -> new com.minhphuc.weapons.content.darkgathering.SeveredHeartItem(new Item.Properties()));

    public static final RegistrySupplier<Item> SEVERED_BRAIN = ITEMS.register("severed_brain",
            () -> new com.minhphuc.weapons.content.darkgathering.SeveredBrainItem(new Item.Properties()));

    public static final RegistrySupplier<Item> THIEN_MA_KATANA = ITEMS.register("thien_ma_katana",
            () -> new com.minhphuc.weapons.content.divine.ThienMaSwordItem(new Item.Properties()));

    public static final RegistrySupplier<Item> SEER_FLESH_EYE = ITEMS.register("seer_flesh_eye",
            () -> new com.minhphuc.weapons.content.darkgathering.SeerFleshEyeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(16)));

    public static final RegistrySupplier<Item> SEER_FLESH_ARM = ITEMS.register("seer_flesh_arm",
            () -> new com.minhphuc.weapons.content.darkgathering.SeerFleshArmItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1)));

    public static final RegistrySupplier<Item> EXTINCTION_STAR = ITEMS.register("extinction_star",
            () -> new com.minhphuc.weapons.content.darkgathering.ExtinctionStarItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1)));

    public static final RegistrySupplier<Item> ALKAID_SPHERE = ITEMS.register("alkaid_sphere",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ALKAID_VORTEX = ITEMS.register("alkaid_vortex",
            () -> new Item(new Item.Properties()));

    // ==========================================
    // MODULE: VŨ KHÍ MÓNG VUỐT HUYỀN THOẠI (LEGENDARY CLAWS)
    // ==========================================
    public static final RegistrySupplier<Item> ABYSSAL_CLAW_DESPAIR = ITEMS.register("abyssal_claw_despair",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialClawItem(new Item.Properties(), "Vuốt Hắc Ám Tuyệt Vọng", "Abyssal Claw of Despair"));

    public static final RegistrySupplier<Item> VOID_CLAW_CALAMITY = ITEMS.register("void_claw_calamity",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialClawItem(new Item.Properties(), "Vuốt Hư Vô Tai Ương", "Void Claw of Calamity"));

    // ==========================================
    // MODULE: DARK GATHERING - KHÔNG VONG & THỰC THỂ LINH HỒN
    // ==========================================
    public static final RegistrySupplier<Item> KUBO_SPAWN_EGG = ITEMS.register("kubo_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.KUBO, 0x0A0512, 0x9333EA, new Item.Properties()));

    public static final RegistrySupplier<Item> DEMON_LORD_KUBO_SPAWN_EGG = ITEMS.register("demon_lord_kubo_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.DEMON_LORD_KUBO, 0x0A0512, 0xF97316, new Item.Properties()));

    public static final RegistrySupplier<Item> VO_NGUYET_MINH_PHOI_SPAWN_EGG = ITEMS.register("vo_nguyet_minh_phoi_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.VO_NGUYET_MINH_PHOI, 0x06020E, 0x8B5CF6, new Item.Properties()));

    public static final RegistrySupplier<Item> CUU_THE_QUYEN_HIEN_SPAWN_EGG = ITEMS.register("cuu_the_quyen_hien_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.CUU_THE_QUYEN_HIEN, 0x111318, 0xE5E7EB, new Item.Properties()));

    public static final RegistrySupplier<Item> SHIGARAKI_TANUKI_EMPTY = ITEMS.register("shigaraki_tanuki_empty",
            () -> new com.minhphuc.weapons.content.darkgathering.ShigarakiTanukiEmptyItem(new Item.Properties()));

    public static final RegistrySupplier<Item> SHIGARAKI_TANUKI_GONGEN = ITEMS.register("shigaraki_tanuki_gongen",
            () -> new com.minhphuc.weapons.content.darkgathering.ShigarakiTanukiGongenItem(new Item.Properties()));

    public static final RegistrySupplier<Item> TA_KINH_DAI_TANG_SPAWN_EGG = ITEMS.register("ta_kinh_dai_tang_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.TA_KINH_DAI_TANG, 0x2A231C, 0xA855F7, new Item.Properties()));

    public static final RegistrySupplier<Item> MARBAS_SPAWN_EGG = ITEMS.register("marbas_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.MARBAS, 0xF8FAFC, 0xDC2626, new Item.Properties()));

    public static final RegistrySupplier<Item> WARDING_EARPLUGS = ITEMS.register("warding_earplugs",
            () -> new com.minhphuc.weapons.content.darkgathering.WardingEarplugsItem(new Item.Properties()));

    public static final RegistrySupplier<Item> BODHI_EFFIGY_EMPTY = ITEMS.register("bodhi_effigy_empty",
            () -> new com.minhphuc.weapons.content.darkgathering.BodhiEffigyEmptyItem(new Item.Properties()));

    public static final RegistrySupplier<Item> BODHI_EFFIGY_MONK = ITEMS.register("bodhi_effigy_monk",
            () -> new com.minhphuc.weapons.content.darkgathering.BodhiEffigyMonkItem(new Item.Properties()));

    public static final RegistrySupplier<Item> KUBO_SUMMON_CORE = ITEMS.register("kubo_summon_core",
            () -> new com.minhphuc.weapons.content.darkgathering.KuboSummonItem(new Item.Properties()));

    public static final RegistrySupplier<Item> MILIM_SOUL = ITEMS.register("milim_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.MILIM, new Item.Properties()));

    public static final RegistrySupplier<Item> VELGRYND_SOUL = ITEMS.register("velgrynd_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.VELGRYND, new Item.Properties()));

    public static final RegistrySupplier<Item> PRIMORDIAL_DEMON_SOUL = ITEMS.register("primordial_demon_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.PRIMORDIAL_DEMON, new Item.Properties()));

    public static final RegistrySupplier<Item> WARDEN_SOUL = ITEMS.register("warden_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.WARDEN, new Item.Properties()));

    public static final RegistrySupplier<Item> ENDER_DRAGON_SOUL = ITEMS.register("ender_dragon_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.ENDER_DRAGON, new Item.Properties()));

    public static final RegistrySupplier<Item> WITHER_SOUL = ITEMS.register("wither_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.WITHER, new Item.Properties()));

    public static final RegistrySupplier<Item> MOB_SOUL = ITEMS.register("mob_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.MOB, new Item.Properties()));

    public static final RegistrySupplier<Item> FAKE_SOUL = ITEMS.register("fake_soul",
            () -> new com.minhphuc.weapons.content.soul.FakeSoulItem(new Item.Properties()));

    // ==========================================
    // MODULE: CARRERA'S GOLDEN GUN & MA ĐẠN (TENSURA LN)
    // ==========================================
    public static final RegistrySupplier<Item> GOLDEN_GUN = ITEMS.register("golden_gun",
            () -> new com.minhphuc.weapons.content.tensura.GoldenGunItem(new Item.Properties()));

    public static final RegistrySupplier<Item> CARRERA_BULLET_JUDGEMENT = ITEMS.register("carrera_bullet_judgement",
            () -> new com.minhphuc.weapons.content.tensura.CarreraBulletItem(com.minhphuc.weapons.content.tensura.CarreraBulletItem.BulletType.JUDGEMENT));

    public static final RegistrySupplier<Item> CARRERA_BULLET_ABYSS = ITEMS.register("carrera_bullet_abyss",
            () -> new com.minhphuc.weapons.content.tensura.CarreraBulletItem(com.minhphuc.weapons.content.tensura.CarreraBulletItem.BulletType.ABYSS_CORE));

    public static final RegistrySupplier<Item> CARRERA_BULLET_GRAVITY = ITEMS.register("carrera_bullet_gravity",
            () -> new com.minhphuc.weapons.content.tensura.CarreraBulletItem(com.minhphuc.weapons.content.tensura.CarreraBulletItem.BulletType.GRAVITY));

    public static final RegistrySupplier<Item> CARRERA_BULLET_RAPID = ITEMS.register("carrera_bullet_rapid",
            () -> new com.minhphuc.weapons.content.tensura.CarreraBulletItem(com.minhphuc.weapons.content.tensura.CarreraBulletItem.BulletType.RAPID));

    // ==========================================
    // MODULE: PRIMORDIAL DEMONS (THẤT ĐẠI ÁC MA THỦY TỔ)
    // ==========================================
    public static final RegistrySupplier<Item> PRIMORDIAL_GRIMOIRE = ITEMS.register("primordial_grimoire",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialGrimoireItem(new Item.Properties()));

    public static final RegistrySupplier<Item> PRIMORDIAL_REBIRTH_TOME = ITEMS.register("primordial_rebirth_tome",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialRebirthTomeItem(new Item.Properties()));


    public static final RegistrySupplier<Item> DEMON_SUMMONING_CIRCLE = ITEMS.register("demon_summoning_circle",
            () -> new Item(new Item.Properties()));

    // 9 VÒNG TRÒN MA THUẬT ĐỘC BẢN CHO TỪNG THỦY TỔ & MA PHÁP HỦY DIỆT CỦA JAUNE
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_NOIR = ITEMS.register("magic_circle_noir",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_ROUGE = ITEMS.register("magic_circle_rouge",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_BLANC = ITEMS.register("magic_circle_blanc",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_JAUNE = ITEMS.register("magic_circle_jaune",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_JAUNE_DESTRUCTION = ITEMS.register("magic_circle_jaune_destruction",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_JAUNE_NUCLEAR = ITEMS.register("magic_circle_jaune_nuclear",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_VIOLET = ITEMS.register("magic_circle_violet",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_BLEU = ITEMS.register("magic_circle_bleu",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_CIRCLE_VERT = ITEMS.register("magic_circle_vert",
            () -> new Item(new Item.Properties()));

    // 7 VẬT PHẨM KHẾ ƯỚC THỦY TỔ RIÊNG BIỆT CHO TỪNG ÁC MA
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT = ITEMS.register("primordial_pact",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_NOIR = ITEMS.register("primordial_pact_noir",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_ROUGE = ITEMS.register("primordial_pact_rouge",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLANC = ITEMS.register("primordial_pact_blanc",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_JAUNE = ITEMS.register("primordial_pact_jaune",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VIOLET = ITEMS.register("primordial_pact_violet",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLEU = ITEMS.register("primordial_pact_bleu",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VERT = ITEMS.register("primordial_pact_vert",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties()));

    // 7 KHỐI KẾT GIỚI LÃNH ĐỊA (DOMAIN BARRIER BLOCKS)
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_NOIR = ITEMS.register("domain_barrier_noir",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_NOIR.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_ROUGE = ITEMS.register("domain_barrier_rouge",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_ROUGE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_BLANC = ITEMS.register("domain_barrier_blanc",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_BLANC.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_JAUNE = ITEMS.register("domain_barrier_jaune",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_JAUNE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_VIOLET = ITEMS.register("domain_barrier_violet",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_VIOLET.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_BLEU = ITEMS.register("domain_barrier_bleu",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_BLEU.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DOMAIN_BARRIER_VERT = ITEMS.register("domain_barrier_vert",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DOMAIN_BARRIER_VERT.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> DRAGON_PRISON_BARRIER = ITEMS.register("dragon_prison_barrier",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DRAGON_PRISON_BARRIER.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> SPIRIT_SHRINE = ITEMS.register("spirit_shrine",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_SHRINE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> LABYRINTH_GATEWAY = ITEMS.register("labyrinth_gateway",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LABYRINTH_GATEWAY.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> BLACK_FIRE = ITEMS.register("black_fire",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.BLACK_FIRE.get(), new Item.Properties()));

    // ==========================================
    // MODULE: CHƯỚC NHIỆT LONG VELGRYND (SCORCH DRAGON)
    // ==========================================
    public static final RegistrySupplier<Item> SCORCH_DRAGON_CORE = ITEMS.register("scorch_dragon_core",
            () -> new com.minhphuc.weapons.content.tensura.ScorchDragonCoreItem(new Item.Properties()));

    public static final RegistrySupplier<Item> VELGRYND_FEATHER_FAN = ITEMS.register("velgrynd_feather_fan",
            () -> new com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DRAGON_SEED = ITEMS.register("dragon_seed",
            () -> new com.minhphuc.weapons.content.tensura.DragonSeedItem(new Item.Properties()));

    public static final RegistrySupplier<Item> VELGRYND_REVERSE_SCALE = ITEMS.register("velgrynd_reverse_scale",
            () -> new com.minhphuc.weapons.content.tensura.VelgryndReverseScaleItem(new Item.Properties()));

    // ==========================================
    // MODULE: BẠCH BĂNG LONG VELZARD (WHITE ICE DRAGON)
    // ==========================================
    public static final RegistrySupplier<Item> FROST_DRAGON_CORE = ITEMS.register("frost_dragon_core",
            () -> new com.minhphuc.weapons.content.tensura.FrostDragonCoreItem(new Item.Properties()));

    public static final RegistrySupplier<Item> VELZARD_SPAWN_EGG = ITEMS.register("velzard_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.VELZARD, 0xE0F7FA, 0x80DEEA, new Item.Properties()));

    public static final RegistrySupplier<Item> VELZARD_SOUL = ITEMS.register("velzard_soul",
            () -> new com.minhphuc.weapons.content.soul.EntitySoulItem(com.minhphuc.weapons.content.soul.SoulType.VELZARD, new Item.Properties()));

    public static final RegistrySupplier<Item> VELZARD_REVERSE_SCALE = ITEMS.register("velzard_reverse_scale",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistrySupplier<Item> ETERNAL_FROST_CRYSTAL = ITEMS.register("eternal_frost_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // ==========================================
    // MODULE: THẦN THIẾT MA THOẠI (MAGISTEEL SMARTPHONE)
    // ==========================================
    public static final RegistrySupplier<Item> MAGISTEEL_PHONE = ITEMS.register("magisteel_phone",
            () -> new com.minhphuc.weapons.content.tensura.MagisteelSmartphoneItem(new Item.Properties()));

    // ==========================================
    // MODULE: THE SHOOTING STAR (THE STELLAR REMOTE)
    // ==========================================
    public static final RegistrySupplier<Item> STELLAR_REMOTE = ITEMS.register("stellar_remote",
            () -> new com.minhphuc.weapons.content.space.StellarRemoteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ==========================================
    // MODULE: KHUNG XƯƠNG THỂ XÁC NHÂN TẠO
    // ==========================================
    public static final RegistrySupplier<Item> ARTIFICIAL_SKELETON = ITEMS.register("artificial_skeleton",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // 21 VẬT PHẨM KHẾ ƯỚC TIẾN HÓA ĐỘC BẢN CHO 7 ÁC MA THỦY TỔ (7 Ác Ma x 3 Trạng Thái: Body, Named, Awakened)
    // 1. DẠNG THỂ XÁC (BODY) - 7 ÁC MA
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_NOIR_BODY = ITEMS.register("primordial_pact_noir_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_ROUGE_BODY = ITEMS.register("primordial_pact_rouge_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLANC_BODY = ITEMS.register("primordial_pact_blanc_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_JAUNE_BODY = ITEMS.register("primordial_pact_jaune_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VIOLET_BODY = ITEMS.register("primordial_pact_violet_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLEU_BODY = ITEMS.register("primordial_pact_bleu_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VERT_BODY = ITEMS.register("primordial_pact_vert_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));

    // 2. DẠNG BAN DANH XƯNG (NAMED) - 7 ÁC MA
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_NOIR_NAMED = ITEMS.register("primordial_pact_noir_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_ROUGE_NAMED = ITEMS.register("primordial_pact_rouge_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLANC_NAMED = ITEMS.register("primordial_pact_blanc_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_JAUNE_NAMED = ITEMS.register("primordial_pact_jaune_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VIOLET_NAMED = ITEMS.register("primordial_pact_violet_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLEU_NAMED = ITEMS.register("primordial_pact_bleu_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VERT_NAMED = ITEMS.register("primordial_pact_vert_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));

    // 3. DẠNG MA THẦN TỐI THƯỢNG THỨC TỈNH (AWAKENED - THỂ XÁC & TÊN) - 7 ÁC MA
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_NOIR_AWAKENED = ITEMS.register("primordial_pact_noir_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_ROUGE_AWAKENED = ITEMS.register("primordial_pact_rouge_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLANC_AWAKENED = ITEMS.register("primordial_pact_blanc_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_JAUNE_AWAKENED = ITEMS.register("primordial_pact_jaune_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VIOLET_AWAKENED = ITEMS.register("primordial_pact_violet_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BLEU_AWAKENED = ITEMS.register("primordial_pact_bleu_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_VERT_AWAKENED = ITEMS.register("primordial_pact_vert_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));

    // Fallbacks
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_BODY = ITEMS.register("primordial_pact_body",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_NAMED = ITEMS.register("primordial_pact_named",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> PRIMORDIAL_PACT_AWAKENED = ITEMS.register("primordial_pact_awakened",
            () -> new com.minhphuc.weapons.content.tensura.PrimordialPactItem(new Item.Properties().rarity(Rarity.EPIC)));

    // ==========================================
    // CREATIVE TAB
    // ==========================================
    public static final RegistrySupplier<CreativeModeTab> WEAPONS_TAB = CREATIVE_MODE_TABS.register("weapons_tab",
            () -> CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("itemGroup.weapons.weapons_tab"))
                    .icon(() -> new ItemStack(MOONLIGHT_SWORD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(DEMON_LORD_AWAKENING_ORB.get());
                        output.accept(DEMON_LORD_SEED.get());
                        output.accept(DEMON_LORD_SOUL.get());
                        output.accept(PRIMORDIAL_GRIMOIRE.get());
                        output.accept(PRIMORDIAL_REBIRTH_TOME.get());

                        // Khế Ước Thủy Tổ
                        output.accept(PRIMORDIAL_PACT_NOIR.get());
                        output.accept(PRIMORDIAL_PACT_ROUGE.get());
                        output.accept(PRIMORDIAL_PACT_BLANC.get());
                        output.accept(PRIMORDIAL_PACT_JAUNE.get());
                        output.accept(PRIMORDIAL_PACT_VIOLET.get());
                        output.accept(PRIMORDIAL_PACT_BLEU.get());
                        output.accept(PRIMORDIAL_PACT_VERT.get());

                        // Khối Kết Giới Lãnh Địa
                        output.accept(DOMAIN_BARRIER_NOIR.get());
                        output.accept(DOMAIN_BARRIER_ROUGE.get());
                        output.accept(DOMAIN_BARRIER_BLANC.get());
                        output.accept(DOMAIN_BARRIER_JAUNE.get());
                        output.accept(DOMAIN_BARRIER_VIOLET.get());
                        output.accept(DOMAIN_BARRIER_BLEU.get());
                        output.accept(DOMAIN_BARRIER_VERT.get());
                        output.accept(DRAGON_PRISON_BARRIER.get());

                        output.accept(DIVINE_HELMET.get());
                        output.accept(DIVINE_CHESTPLATE.get());
                        output.accept(DIVINE_LEGGINGS.get());
                        output.accept(DIVINE_BOOTS.get());
                        output.accept(HINATA_HELMET.get());
                        output.accept(HINATA_CHESTPLATE.get());
                        output.accept(HINATA_LEGGINGS.get());
                        output.accept(HINATA_BOOTS.get());
                        output.accept(HINATA_SWORD.get());

                        // 7 Bộ Trang Bị Ác Ma Thủy Tổ Huyền Thoại (28 món: Đầu, Áo, Quần, Giày)
                        output.accept(NOIR_HELMET.get());
                        output.accept(NOIR_CHESTPLATE.get());
                        output.accept(NOIR_LEGGINGS.get());
                        output.accept(NOIR_BOOTS.get());

                        output.accept(ROUGE_HELMET.get());
                        output.accept(ROUGE_CHESTPLATE.get());
                        output.accept(ROUGE_LEGGINGS.get());
                        output.accept(ROUGE_BOOTS.get());

                        output.accept(BLANC_HELMET.get());
                        output.accept(BLANC_CHESTPLATE.get());
                        output.accept(BLANC_LEGGINGS.get());
                        output.accept(BLANC_BOOTS.get());

                        output.accept(JAUNE_HELMET.get());
                        output.accept(JAUNE_CHESTPLATE.get());
                        output.accept(JAUNE_LEGGINGS.get());
                        output.accept(JAUNE_BOOTS.get());

                        output.accept(VIOLET_HELMET.get());
                        output.accept(VIOLET_CHESTPLATE.get());
                        output.accept(VIOLET_LEGGINGS.get());
                        output.accept(VIOLET_BOOTS.get());

                        output.accept(BLEU_HELMET.get());
                        output.accept(BLEU_CHESTPLATE.get());
                        output.accept(BLEU_LEGGINGS.get());
                        output.accept(BLEU_BOOTS.get());

                        output.accept(VERT_HELMET.get());
                        output.accept(VERT_CHESTPLATE.get());
                        output.accept(VERT_LEGGINGS.get());
                        output.accept(VERT_BOOTS.get());
                        output.accept(GUIDE_BOOK.get());
                        output.accept(MOONLIGHT_SWORD.get());
                        output.accept(THIEN_MA_KATANA.get());
                        output.accept(SEVERED_HEART.get());
                        output.accept(SEVERED_BRAIN.get());
                        output.accept(SEER_FLESH_EYE.get());
                        output.accept(SEER_FLESH_ARM.get());
                        output.accept(EXTINCTION_STAR.get());


                        // Súng Lục Hoàng Kim & Ma Đạn Carrera
                        output.accept(GOLDEN_GUN.get());
                        output.accept(CARRERA_BULLET_JUDGEMENT.get());
                        output.accept(CARRERA_BULLET_ABYSS.get());
                        output.accept(CARRERA_BULLET_GRAVITY.get());
                        output.accept(CARRERA_BULLET_RAPID.get());

                        // Khung Xương Thể Xác & 21 Khế Ước Tiến Hóa Độc Bản
                        output.accept(ARTIFICIAL_SKELETON.get());

                        // 7 Khế Ước Thể Xác (Body)
                        output.accept(PRIMORDIAL_PACT_ROUGE_BODY.get());
                        output.accept(PRIMORDIAL_PACT_NOIR_BODY.get());
                        output.accept(PRIMORDIAL_PACT_BLANC_BODY.get());
                        output.accept(PRIMORDIAL_PACT_JAUNE_BODY.get());
                        output.accept(PRIMORDIAL_PACT_VIOLET_BODY.get());
                        output.accept(PRIMORDIAL_PACT_BLEU_BODY.get());
                        output.accept(PRIMORDIAL_PACT_VERT_BODY.get());

                        // 7 Khế Ước Sắc Phong Danh Xưng (Named)
                        output.accept(PRIMORDIAL_PACT_ROUGE_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_NOIR_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_BLANC_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_JAUNE_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_VIOLET_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_BLEU_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_VERT_NAMED.get());

                        // 7 Khế Ước Ma Thần Tối Thượng (Awakened)
                        output.accept(PRIMORDIAL_PACT_ROUGE_AWAKENED.get());
                        output.accept(PRIMORDIAL_PACT_NOIR_AWAKENED.get());
                        output.accept(PRIMORDIAL_PACT_BLANC_AWAKENED.get());
                        output.accept(PRIMORDIAL_PACT_JAUNE_AWAKENED.get());
                        output.accept(PRIMORDIAL_PACT_VIOLET_AWAKENED.get());
                        output.accept(PRIMORDIAL_PACT_BLEU_AWAKENED.get());
                        output.accept(PRIMORDIAL_PACT_VERT_AWAKENED.get());

                        output.accept(PRIMORDIAL_PACT_BODY.get());
                        output.accept(PRIMORDIAL_PACT_NAMED.get());
                        output.accept(PRIMORDIAL_PACT_AWAKENED.get());

                        // Chước Nhiệt Long Velgrynd
                        output.accept(SCORCH_DRAGON_CORE.get());
                        output.accept(VELGRYND_FEATHER_FAN.get());
                        output.accept(DRAGON_SEED.get());
                        output.accept(VELGRYND_REVERSE_SCALE.get());
                        output.accept(MAGISTEEL_PHONE.get());
                        output.accept(STELLAR_REMOTE.get());
                        output.accept(MILIM_DRAGON_CONTRACT.get());
                        output.accept(SPIRIT_SHRINE.get());
                        output.accept(LABYRINTH_GATEWAY.get());

                        // Bạch Băng Long Velzard
                        output.accept(FROST_DRAGON_CORE.get());
                        output.accept(VELZARD_SPAWN_EGG.get());
                        output.accept(VELZARD_SOUL.get());
                        output.accept(VELZARD_REVERSE_SCALE.get());
                        output.accept(ETERNAL_FROST_CRYSTAL.get());

                        // Vũ Khí Móng Vuốt & Thực Thể Dark Gathering
                        output.accept(ABYSSAL_CLAW_DESPAIR.get());
                        output.accept(VOID_CLAW_CALAMITY.get());
                        output.accept(KUBO_SPAWN_EGG.get());
                        output.accept(DEMON_LORD_KUBO_SPAWN_EGG.get());
                        output.accept(VO_NGUYET_MINH_PHOI_SPAWN_EGG.get());
                        output.accept(KUBO_SUMMON_CORE.get());
                        output.accept(CUU_THE_QUYEN_HIEN_SPAWN_EGG.get());
                        output.accept(SHIGARAKI_TANUKI_EMPTY.get());
                        output.accept(SHIGARAKI_TANUKI_GONGEN.get());
                        output.accept(TA_KINH_DAI_TANG_SPAWN_EGG.get());
                        output.accept(MARBAS_SPAWN_EGG.get());
                        output.accept(WARDING_EARPLUGS.get());
                        output.accept(BODHI_EFFIGY_EMPTY.get());
                        output.accept(BODHI_EFFIGY_MONK.get());
                        output.accept(MILIM_SOUL.get());
                        output.accept(VELGRYND_SOUL.get());
                        output.accept(PRIMORDIAL_DEMON_SOUL.get());
                        output.accept(WARDEN_SOUL.get());
                        output.accept(ENDER_DRAGON_SOUL.get());
                        output.accept(WITHER_SOUL.get());
                        output.accept(MOB_SOUL.get());
                        output.accept(FAKE_SOUL.get());
                    })
            ));

    public static void register() {
        ARMOR_MATERIALS.register();
        ITEMS.register();
        CREATIVE_MODE_TABS.register();
    }
}
