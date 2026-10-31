package com.minhphuc.weapons.entity;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;

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

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.tensura.VelzardEntity>> VELZARD =
            ENTITY_TYPES.register("velzard", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.tensura.VelzardEntity::new, MobCategory.MONSTER)
                            .sized(0.65F, 2.0F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("velzard")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity>> CUU_THE_QUYEN_HIEN =
            ENTITY_TYPES.register("cuu_the_quyen_hien", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity::new, MobCategory.MONSTER)
                            .sized(0.75F, 2.35F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("cuu_the_quyen_hien")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.CuuTheSoulEntity>> CUU_THE_SOUL =
            ENTITY_TYPES.register("cuu_the_soul", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.CuuTheSoulEntity::new, MobCategory.MISC)
                            .sized(0.75F, 2.35F)
                            .clientTrackingRange(16)
                            .noSave()
                            .build("cuu_the_soul")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity>> TA_KINH_DAI_TANG =
            ENTITY_TYPES.register("ta_kinh_dai_tang", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.2F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("ta_kinh_dai_tang")
            );

    public static void register() {
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(PRIMORDIAL_DEMON, PrimordialDemonEntity::createAttributes);
        EntityAttributeRegistry.register(VELGRYND, com.minhphuc.weapons.entity.tensura.VelgryndEntity::createAttributes);
        EntityAttributeRegistry.register(KUBO, com.minhphuc.weapons.entity.darkgathering.KuboEntity::createAttributes);
        EntityAttributeRegistry.register(MILIM, com.minhphuc.weapons.entity.tensura.MilimEntity::createAttributes);
        EntityAttributeRegistry.register(VELZARD, com.minhphuc.weapons.entity.tensura.VelzardEntity::createAttributes);
        EntityAttributeRegistry.register(CUU_THE_QUYEN_HIEN, com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity::createAttributes);
        EntityAttributeRegistry.register(CUU_THE_SOUL, com.minhphuc.weapons.entity.darkgathering.CuuTheSoulEntity::createAttributes);
        EntityAttributeRegistry.register(TA_KINH_DAI_TANG, com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity::createAttributes);

        // Đăng ký quy tắc Spawn Placement cho các thực thể xuất hiện tự nhiên
        SpawnPlacementsRegistry.register(PRIMORDIAL_DEMON, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(level, pos, random) && Mob.checkMobSpawnRules(type, level, spawnType, pos, random));

        SpawnPlacementsRegistry.register(KUBO, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        SpawnPlacementsRegistry.register(MILIM, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        SpawnPlacementsRegistry.register(CUU_THE_QUYEN_HIEN, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        SpawnPlacementsRegistry.register(TA_KINH_DAI_TANG, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        SpawnPlacementsRegistry.register(VELGRYND, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        SpawnPlacementsRegistry.register(VELZARD, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        // Bổ sung vào Biome theo cấp độ hiếm chuẩn:
        // Cấp 1: Ác Ma Thủy Tổ & Không Vong (Ít hiếm nhất trong nhóm: weight 6 & 5)
        // Cấp 2: Milim & Cứu Thế Quyền Hiện (Hiếm hơn: weight 2)
        // Cấp 3: Long Chủng Velgrynd & Velzard (Hiếm nhất: weight 1)
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                var spawns = mutable.getSpawnProperties();
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(PRIMORDIAL_DEMON.get(), 6, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(KUBO.get(), 5, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MILIM.get(), 2, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(CUU_THE_QUYEN_HIEN.get(), 2, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(TA_KINH_DAI_TANG.get(), 2, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(VELGRYND.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(VELZARD.get(), 1, 1, 1));

                // Giới hạn spawn cost chống nghẽn / lag thế giới
                spawns.setSpawnCost(PRIMORDIAL_DEMON.get(), 0.7D, 0.15D);
                spawns.setSpawnCost(KUBO.get(), 0.8D, 0.15D);
                spawns.setSpawnCost(MILIM.get(), 1.0D, 0.15D);
                spawns.setSpawnCost(CUU_THE_QUYEN_HIEN.get(), 1.0D, 0.15D);
                spawns.setSpawnCost(TA_KINH_DAI_TANG.get(), 1.0D, 0.15D);
                spawns.setSpawnCost(VELGRYND.get(), 1.0D, 0.15D);
                spawns.setSpawnCost(VELZARD.get(), 1.0D, 0.15D);
            }
        });
    }
}
