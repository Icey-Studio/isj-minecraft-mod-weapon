package com.minhphuc.weapons.entity;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

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

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity>> DEMON_LORD_KUBO =
            ENTITY_TYPES.register("demon_lord_kubo", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity::new, MobCategory.MONSTER)
                            .sized(2.8F, 3.2F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("demon_lord_kubo")
            );

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.VoNguyetMinhPhoiEntity>> VO_NGUYET_MINH_PHOI =
            ENTITY_TYPES.register("vo_nguyet_minh_phoi", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.VoNguyetMinhPhoiEntity::new, MobCategory.MONSTER)
                            .sized(2.4F, 2.4F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("vo_nguyet_minh_phoi")
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

    public static final RegistrySupplier<EntityType<com.minhphuc.weapons.entity.darkgathering.MarbasEntity>> MARBAS =
            ENTITY_TYPES.register("marbas", () ->
                    EntityType.Builder.of(com.minhphuc.weapons.entity.darkgathering.MarbasEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 4.0F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("marbas")
            );

    public static boolean checkBossSpawnRules(EntityType<? extends Mob> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (!Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) return false;
        if (level instanceof ServerLevel sl) {
            // Chống nghẽn & lag thế giới: Không cho phép spawn cụm boss cùng loại trong bán kính 96 block
            AABB checkArea = new AABB(pos).inflate(96.0D);
            if (!sl.getEntitiesOfClass(Mob.class, checkArea, e -> e.getType() == type).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void register() {
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(PRIMORDIAL_DEMON, PrimordialDemonEntity::createAttributes);
        EntityAttributeRegistry.register(VELGRYND, com.minhphuc.weapons.entity.tensura.VelgryndEntity::createAttributes);
        EntityAttributeRegistry.register(KUBO, com.minhphuc.weapons.entity.darkgathering.KuboEntity::createAttributes);
        EntityAttributeRegistry.register(DEMON_LORD_KUBO, com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity::createAttributes);
        EntityAttributeRegistry.register(VO_NGUYET_MINH_PHOI, com.minhphuc.weapons.entity.darkgathering.VoNguyetMinhPhoiEntity::createAttributes);
        EntityAttributeRegistry.register(MILIM, com.minhphuc.weapons.entity.tensura.MilimEntity::createAttributes);
        EntityAttributeRegistry.register(VELZARD, com.minhphuc.weapons.entity.tensura.VelzardEntity::createAttributes);
        EntityAttributeRegistry.register(CUU_THE_QUYEN_HIEN, com.minhphuc.weapons.entity.darkgathering.CuuTheQuyenHienEntity::createAttributes);
        EntityAttributeRegistry.register(CUU_THE_SOUL, com.minhphuc.weapons.entity.darkgathering.CuuTheSoulEntity::createAttributes);
        EntityAttributeRegistry.register(TA_KINH_DAI_TANG, com.minhphuc.weapons.entity.darkgathering.TaKinhDaiTangEntity::createAttributes);
        EntityAttributeRegistry.register(MARBAS, com.minhphuc.weapons.entity.darkgathering.MarbasEntity::createAttributes);

        // Đăng ký quy tắc Spawn Placement cho các thực thể thù địch xuất hiện tự nhiên (Kèm bộ lọc chống trùng cụm)
        SpawnPlacementsRegistry.register(PRIMORDIAL_DEMON, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(KUBO, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(MILIM, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(CUU_THE_QUYEN_HIEN, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(TA_KINH_DAI_TANG, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(MARBAS, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(VELGRYND, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        SpawnPlacementsRegistry.register(VELZARD, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::checkBossSpawnRules);

        // Bổ sung vào Biome Overworld với trọng số hiếm (1-2), tuyệt đối KHÔNG dùng setSpawnCost để tránh nghẽn CPU/TPS
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                var spawns = mutable.getSpawnProperties();
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(PRIMORDIAL_DEMON.get(), 2, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(KUBO.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(CUU_THE_QUYEN_HIEN.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(TA_KINH_DAI_TANG.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MARBAS.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MILIM.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(VELGRYND.get(), 1, 1, 1));
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(VELZARD.get(), 1, 1, 1));
            }
        });
    }
}
