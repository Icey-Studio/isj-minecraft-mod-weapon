package com.minhphuc.weapons.client;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.init.ModKeyBindings;

public class WeaponsModClient {

    public static void initClient() {
        ModKeyBindings.register();
        ClientInputEvents.register();
        com.minhphuc.weapons.client.ClientTaisuiHandler.init();
        com.minhphuc.weapons.client.ClientTengaiMangaHandler.init();
        com.minhphuc.weapons.client.ClientIaiHandler.init();
        com.minhphuc.weapons.client.ClientNihilityCinematicHandler.init();
        com.minhphuc.weapons.client.ClientAzathothCinematicHandler.init();
        com.minhphuc.weapons.client.ClientModelProperties.register();

        // Đăng ký Model Layers và Entity Renderers cho Ác Ma Thủy Tổ & Chước Nhiệt Long Velgrynd
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.PrimordialDemonModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.PrimordialDemonModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.PRIMORDIAL_DEMON,
                com.minhphuc.weapons.client.renderer.PrimordialDemonRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.VelgryndModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.VelgryndModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.VELGRYND,
                com.minhphuc.weapons.client.renderer.VelgryndRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.KuboModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.KuboModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.KUBO,
                com.minhphuc.weapons.client.renderer.KuboRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.DemonLordKuboModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.DemonLordKuboModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.DEMON_LORD_KUBO,
                com.minhphuc.weapons.client.renderer.DemonLordKuboRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.VoNguyetMinhPhoiModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.VoNguyetMinhPhoiModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.VO_NGUYET_MINH_PHOI,
                com.minhphuc.weapons.client.renderer.VoNguyetMinhPhoiRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.MilimModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.MilimModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.MILIM,
                com.minhphuc.weapons.client.renderer.MilimRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.VelzardModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.VelzardModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.VELZARD,
                com.minhphuc.weapons.client.renderer.VelzardRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.CuuTheQuyenHienModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.CuuTheQuyenHienModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.CUU_THE_QUYEN_HIEN,
                com.minhphuc.weapons.client.renderer.CuuTheQuyenHienRenderer::new
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.CUU_THE_SOUL,
                com.minhphuc.weapons.client.renderer.CuuTheSoulRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.TaKinhDaiTangModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.TaKinhDaiTangModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.TA_KINH_DAI_TANG,
                com.minhphuc.weapons.client.renderer.TaKinhDaiTangRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.MarbasModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.MarbasModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.MARBAS,
                com.minhphuc.weapons.client.renderer.MarbasRenderer::new
        );

        // Vệ Tinh Quỹ Đạo & Đầu Đạn Tấn Công 3D Models
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.OrbitalDefenseSatelliteModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.OrbitalDefenseSatelliteModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.OrbitalKineticProjectileModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.OrbitalKineticProjectileModel::createBodyLayer
        );

        // Ác Ma Ma Giới (Underworld Demons - 3 morphs)
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.UnderworldDemonModel.HUMANOID_LAYER,
                com.minhphuc.weapons.client.model.UnderworldDemonModel::createHumanoidLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.UnderworldDemonModel.BEAST_LAYER,
                com.minhphuc.weapons.client.model.UnderworldDemonModel::createBeastLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.UnderworldDemonModel.FIEND_LAYER,
                com.minhphuc.weapons.client.model.UnderworldDemonModel::createFiendLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.UNDERWORLD_DEMON,
                com.minhphuc.weapons.client.renderer.UnderworldDemonRenderer::new
        );

        // Bạo Phong Long Veldora Tempest (Dạng Rồng & Dạng Người)
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.VeldoraDragonModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.VeldoraDragonModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.VeldoraHumanModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.VeldoraHumanModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.VELDORA,
                com.minhphuc.weapons.client.renderer.VeldoraRenderer::new
        );

        // Phase 3: Charybdis, Orc Disaster Geld & Elemental Spirits
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.CharybdisModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.tensura.CharybdisModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.CHARYBDIS,
                com.minhphuc.weapons.entity.tensura.CharybdisRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.OrcDisasterGeldModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.tensura.OrcDisasterGeldModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.ORC_DISASTER_GELD,
                com.minhphuc.weapons.entity.tensura.OrcDisasterGeldRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.ElementalSpiritModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.tensura.ElementalSpiritModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.ELEMENTAL_SPIRIT,
                com.minhphuc.weapons.entity.tensura.ElementalSpiritRenderer::new
        );

        // Phase 4: Chronoa & Di Tích Tâm Linh Cấp S
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.ChronoaModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.tensura.ChronoaModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.ChronoaModel.INNER_ARMOR,
                com.minhphuc.weapons.entity.tensura.ChronoaModel::createInnerArmorLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.ChronoaModel.OUTER_ARMOR,
                com.minhphuc.weapons.entity.tensura.ChronoaModel::createOuterArmorLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.CHRONOA,
                com.minhphuc.weapons.entity.tensura.ChronoaRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.HinataModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.tensura.HinataModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.HinataModel.INNER_ARMOR,
                com.minhphuc.weapons.entity.tensura.HinataModel::createInnerArmorLayer
        );
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.HinataModel.OUTER_ARMOR,
                com.minhphuc.weapons.entity.tensura.HinataModel::createOuterArmorLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.HINATA,
                com.minhphuc.weapons.entity.tensura.HinataRenderer::new
        );

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.darkgathering.CursedIdolModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.darkgathering.CursedIdolModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.CURSED_IDOL,
                com.minhphuc.weapons.entity.darkgathering.CursedIdolRenderer::new
        );

        // Tiểu Thư Mariabell Rosso (Slime Isekai)
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.entity.tensura.MariabellModel.LAYER_LOCATION,
                com.minhphuc.weapons.entity.tensura.MariabellModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.MARIABELL,
                com.minhphuc.weapons.entity.tensura.MariabellRenderer::new
        );

        // Hỗn Độn Long (Chaos Dragon)
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                com.minhphuc.weapons.client.model.ChaosDragonModel.LAYER_LOCATION,
                com.minhphuc.weapons.client.model.ChaosDragonModel::createBodyLayer
        );
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.minhphuc.weapons.entity.ModEntities.CHAOS_DRAGON,
                com.minhphuc.weapons.client.renderer.ChaosDragonRenderer::new
        );

        WeaponsMod.LOGGER.info("Weapons Mod Client Setup complete!");
    }
}
