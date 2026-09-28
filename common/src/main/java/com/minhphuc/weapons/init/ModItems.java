package com.minhphuc.weapons.init;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.content.infinitygauntlet.InfinityGauntletItem;
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
    // MODULE: INFINITY GAUNTLET & STONES
    // ==========================================
    public static final RegistrySupplier<Item> EMPTY_INFINITY_GAUNTLET = ITEMS.register("empty_infinity_gauntlet",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.EmptyInfinityGauntletItem(new Item.Properties()));

    public static final RegistrySupplier<Item> INFINITY_GAUNTLET = ITEMS.register("infinity_gauntlet",
            () -> new InfinityGauntletItem(new Item.Properties()));

    public static final RegistrySupplier<Item> POWER_STONE = ITEMS.register("power_stone",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem(com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem.StoneType.POWER));

    public static final RegistrySupplier<Item> SPACE_STONE = ITEMS.register("space_stone",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem(com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem.StoneType.SPACE));

    public static final RegistrySupplier<Item> REALITY_STONE = ITEMS.register("reality_stone",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem(com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem.StoneType.REALITY));

    public static final RegistrySupplier<Item> SOUL_STONE = ITEMS.register("soul_stone",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem(com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem.StoneType.SOUL));

    public static final RegistrySupplier<Item> TIME_STONE = ITEMS.register("time_stone",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem(com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem.StoneType.TIME));

    public static final RegistrySupplier<Item> MIND_STONE = ITEMS.register("mind_stone",
            () -> new com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem(com.minhphuc.weapons.content.infinitygauntlet.InfinityStoneItem.StoneType.MIND));

    // ==========================================
    // MODULE: TENSURA DEMON LORD EVOLUTION
    // ==========================================
    public static final RegistrySupplier<Item> DEMON_LORD_SEED = ITEMS.register("demon_lord_seed",
            () -> new com.minhphuc.weapons.content.tensura.DemonLordSeedItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DEMON_LORD_SOUL = ITEMS.register("demon_lord_soul",
            () -> new com.minhphuc.weapons.content.tensura.DemonLordSoulItem(new Item.Properties()));

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
    // MODULE: DARK GATHERING - KHÔNG VONG
    // ==========================================
    public static final RegistrySupplier<Item> KUBO_SPAWN_EGG = ITEMS.register("kubo_spawn_egg",
            () -> new dev.architectury.core.item.ArchitecturySpawnEggItem(com.minhphuc.weapons.entity.ModEntities.KUBO, 0x0A0512, 0x9333EA, new Item.Properties()));

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
    // MODULE: THẦN THIẾT MA THOẠI (MAGISTEEL SMARTPHONE)
    // ==========================================
    public static final RegistrySupplier<Item> MAGISTEEL_PHONE = ITEMS.register("magisteel_phone",
            () -> new com.minhphuc.weapons.content.tensura.MagisteelSmartphoneItem(new Item.Properties()));

    // ==========================================
    // MODULE: BỒN CHỨA & KHUNG XƯƠNG THỂ XÁC NHÂN TẠO
    // ==========================================
    public static final RegistrySupplier<Item> ARTIFICIAL_SKELETON = ITEMS.register("artificial_skeleton",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistrySupplier<Item> INCUBATION_CAPSULE = ITEMS.register("incubation_capsule",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.INCUBATION_CAPSULE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistrySupplier<Item> INCUBATION_CAPSULE_SKELETON = ITEMS.register("incubation_capsule_skeleton",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.INCUBATION_CAPSULE.get(), new Item.Properties().rarity(Rarity.EPIC)));

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
                    .icon(() -> new ItemStack(INFINITY_GAUNTLET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(EMPTY_INFINITY_GAUNTLET.get());
                        output.accept(INFINITY_GAUNTLET.get());
                        output.accept(POWER_STONE.get());
                        output.accept(SPACE_STONE.get());
                        output.accept(REALITY_STONE.get());
                        output.accept(SOUL_STONE.get());
                        output.accept(TIME_STONE.get());
                        output.accept(MIND_STONE.get());
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

                        output.accept(DIVINE_HELMET.get());
                        output.accept(DIVINE_CHESTPLATE.get());
                        output.accept(DIVINE_LEGGINGS.get());
                        output.accept(DIVINE_BOOTS.get());
                        output.accept(GUIDE_BOOK.get());
                        output.accept(MOONLIGHT_SWORD.get());
                        output.accept(SEER_FLESH_EYE.get());
                        output.accept(SEER_FLESH_ARM.get());
                        output.accept(EXTINCTION_STAR.get());


                        // Súng Lục Hoàng Kim & Ma Đạn Carrera
                        output.accept(GOLDEN_GUN.get());
                        output.accept(CARRERA_BULLET_JUDGEMENT.get());
                        output.accept(CARRERA_BULLET_ABYSS.get());
                        output.accept(CARRERA_BULLET_GRAVITY.get());
                        output.accept(CARRERA_BULLET_RAPID.get());

                        // Bồn Chứa & Khung Xương Thể Xác Nhân Tạo & 21 Khế Ước Tiến Hóa Độc Bản
                        output.accept(ARTIFICIAL_SKELETON.get());
                        output.accept(INCUBATION_CAPSULE.get());
                        output.accept(INCUBATION_CAPSULE_SKELETON.get());

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
                        output.accept(MILIM_DRAGON_CONTRACT.get());

                        // Vũ Khí Móng Vuốt & Thực Thể Dark Gathering
                        output.accept(ABYSSAL_CLAW_DESPAIR.get());
                        output.accept(VOID_CLAW_CALAMITY.get());
                        output.accept(KUBO_SPAWN_EGG.get());
                    })
            ));

    public static void register() {
        ARMOR_MATERIALS.register();
        ITEMS.register();
        CREATIVE_MODE_TABS.register();
    }
}
