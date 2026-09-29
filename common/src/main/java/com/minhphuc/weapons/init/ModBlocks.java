package com.minhphuc.weapons.init;

import com.minhphuc.weapons.WeaponsMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(WeaponsMod.MOD_ID, Registries.BLOCK);

    private static BlockBehaviour.Properties createBarrierProperties(MapColor color, int light) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .lightLevel(state -> light)
                .sound(SoundType.AMETHYST)
                .noOcclusion();
    }

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_NOIR = BLOCKS.register("domain_barrier_noir",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.COLOR_BLACK, 12)));

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_ROUGE = BLOCKS.register("domain_barrier_rouge",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.COLOR_RED, 15)));

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_BLANC = BLOCKS.register("domain_barrier_blanc",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.SNOW, 15)));

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_JAUNE = BLOCKS.register("domain_barrier_jaune",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.COLOR_YELLOW, 15)));

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_VIOLET = BLOCKS.register("domain_barrier_violet",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.COLOR_PURPLE, 13)));

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_BLEU = BLOCKS.register("domain_barrier_bleu",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.COLOR_BLUE, 14)));

    public static final RegistrySupplier<Block> DOMAIN_BARRIER_VERT = BLOCKS.register("domain_barrier_vert",
            () -> new com.minhphuc.weapons.content.tensura.DomainBarrierBlock(createBarrierProperties(MapColor.COLOR_LIGHT_GREEN, 14)));

    public static final RegistrySupplier<Block> DRAGON_PRISON_BARRIER = BLOCKS.register("dragon_prison_barrier",
            () -> new com.minhphuc.weapons.content.evolution.DragonPrisonBarrierBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_YELLOW)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .lightLevel(state -> 15)
                            .sound(SoundType.AMETHYST)
                            .noOcclusion()
            ));

    public static final RegistrySupplier<Block> ANTI_MAGIC_BARRIER = BLOCKS.register("anti_magic_barrier",
            () -> new com.minhphuc.weapons.content.tensura.AntiMagicBarrierBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GREEN)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .lightLevel(state -> 15)
                            .sound(SoundType.AMETHYST)
                            .noOcclusion()
            ));

    public static final RegistrySupplier<Block> MULTILAYER_BARRIER = BLOCKS.register("multilayer_barrier",
            () -> new com.minhphuc.weapons.content.tensura.MultilayerBarrierBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .lightLevel(state -> 14)
                            .sound(SoundType.AMETHYST)
                            .noOcclusion()
            ));

    public static final RegistrySupplier<Block> INCUBATION_CAPSULE = BLOCKS.register("incubation_capsule",
            () -> new com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 15.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .lightLevel(state -> 8)
            ));

    public static void register() {
        BLOCKS.register();
    }
}
