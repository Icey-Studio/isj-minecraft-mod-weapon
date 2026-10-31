package com.minhphuc.weapons.entity;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

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

        // Tối ưu hóa hiệu năng tối đa (Zero Lag):
        // Toàn bộ các Đại Boss, Ác Ma Thủy Tổ và Tốt Nghiệp Sinh Cấp S được triệu hồi thông qua nghi lễ / vật phẩm khế ước,
        // hoặc khiêu chiến thông qua Vật Phẩm Phong Ấn (Shift + Chuột Phải) và Trứng Triệu Hồi (Spawn Eggs).
        // Tuyệt đối không spawn tự nhiên ngẫu nhiên trong Overworld để bảo vệ tuyệt đối CPU, FPS và ngăn chặn tình trạng nghẽn chunk/tràn RAM.
    }
}
