package com.minhphuc.weapons.fabric;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.init.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class WeaponsModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.minhphuc.weapons.client.WeaponsModClient.initClient();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INCUBATION_CAPSULE.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRAGON_PRISON_BARRIER.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_NOIR.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_ROUGE.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_BLANC.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_JAUNE.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_VIOLET.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_BLEU.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DOMAIN_BARRIER_VERT.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ANTI_MAGIC_BARRIER.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MULTILAYER_BARRIER.get(), RenderType.translucent());
    }
}
