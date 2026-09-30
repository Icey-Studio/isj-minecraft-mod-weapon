package com.minhphuc.weapons;

import com.minhphuc.weapons.client.ClientInputEvents;
import com.minhphuc.weapons.config.AIGeminiConfig;
import com.minhphuc.weapons.content.infinitygauntlet.InfinityGauntletEvents;
import com.minhphuc.weapons.content.tensura.TensuraEvents;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.init.ModKeyBindings;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class WeaponsMod {
    public static final String MOD_ID = "weapons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        // Đăng ký Blocks & Items
        com.minhphuc.weapons.init.ModBlocks.register();
        ModItems.register();

        // Đăng ký Entities
        com.minhphuc.weapons.entity.ModEntities.register();

        // Đăng ký Network Packets
        ModMessages.register();

        // Nạp cấu hình Gemini AI
        AIGeminiConfig.loadConfig();

        // Đăng ký các sự kiện Gameplay đa nền tảng
        InfinityGauntletEvents.register();
        TensuraEvents.register();
        com.minhphuc.weapons.content.divine.DivineWeaponEvents.register();

        LOGGER.info("Weapons Mod (Cross-Platform) initialized successfully!");
    }

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

        LOGGER.info("Weapons Mod Client Setup complete!");
    }
}
