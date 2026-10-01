package com.minhphuc.weapons.client;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.init.ModKeyBindings;

public class WeaponsModClient {

    public static void initClient() {
        ModKeyBindings.register();
        ClientInputEvents.register();
        com.minhphuc.weapons.client.ClientTaisuiHandler.init();
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

        WeaponsMod.LOGGER.info("Weapons Mod Client Setup complete!");
    }
}
