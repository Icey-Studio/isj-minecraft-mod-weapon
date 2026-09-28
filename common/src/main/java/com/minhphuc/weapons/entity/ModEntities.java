package com.minhphuc.weapons.entity;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(WeaponsMod.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<PrimordialDemonEntity>> PRIMORDIAL_DEMON =
            ENTITY_TYPES.register("primordial_demon", () ->
                    EntityType.Builder.of(PrimordialDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("primordial_demon")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.tensura.VelgryndEntity>> VELGRYND =
            ENTITY_TYPES.register("velgrynd", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.tensura.VelgryndEntity::new, MobCategory.MONSTER)
                            .sized(0.65F, 2.1F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("velgrynd")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.KuboEntity>> KUBO =
            ENTITY_TYPES.register("kubo", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.KuboEntity::new, MobCategory.MONSTER)
                            .sized(2.4F, 2.4F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("kubo")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.tensura.MilimEntity>> MILIM =
            ENTITY_TYPES.register("milim", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.tensura.MilimEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.85F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("milim")
            );

    public static void register() {
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(PRIMORDIAL_DEMON, PrimordialDemonEntity::createAttributes);
        EntityAttributeRegistry.register(VELGRYND, com.minhphuc.weapons.entity.tensura.VelgryndEntity::createAttributes);
        EntityAttributeRegistry.register(KUBO, com.minhphuc.weapons.entity.darkgathering.KuboEntity::createAttributes);
        EntityAttributeRegistry.register(MILIM, com.minhphuc.weapons.entity.tensura.MilimEntity::createAttributes);

        BiomeModifications.addProperties(
                b -> b.hasTag(BiomeTags.IS_OVERWORLD),
                (ctx, mutable) -> mutable.getSpawnProperties().addSpawn(
                        MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(PRIMORDIAL_DEMON.get(), 5, 1, 1)
                )
        );

        // Tỷ lệ xuất hiện tự nhiên của Chước Nhiệt Long Velgrynd: Siêu hiếm, chỉ xuất hiện ở Overworld
        BiomeModifications.addProperties(
                b -> b.hasTag(BiomeTags.IS_OVERWORLD),
                (ctx, mutable) -> mutable.getSpawnProperties().addSpawn(
                        MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(VELGRYND.get(), 1, 1, 1)
                )
        );

        // Tỷ lệ xuất hiện tự nhiên của Ma Vương Milim Nava: Tương đương Chước Nhiệt Long để giảm lag
        BiomeModifications.addProperties(
                b -> b.hasTag(BiomeTags.IS_OVERWORLD),
                (ctx, mutable) -> mutable.getSpawnProperties().addSpawn(
                        MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(MILIM.get(), 1, 1, 1)
                )
        );
    }
}
