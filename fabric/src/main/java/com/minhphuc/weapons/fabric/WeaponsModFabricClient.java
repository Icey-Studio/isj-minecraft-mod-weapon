package com.minhphuc.weapons.fabric;

import com.minhphuc.weapons.WeaponsMod;
import com.minhphuc.weapons.client.ClientAzathothCinematicHandler;
import com.minhphuc.weapons.client.ClientCinematicSkillHandler;
import com.minhphuc.weapons.client.ClientHellflareCinematicHandler;
import com.minhphuc.weapons.client.renderer.ClientCinematicSkillRenderer;
import com.minhphuc.weapons.client.renderer.ClientHellflareRenderer;
import com.minhphuc.weapons.client.ClientNihilityCinematicHandler;
import com.minhphuc.weapons.client.ClientOrbitalCinematicHandler;
import com.minhphuc.weapons.client.CustomImpactFrameOverlay;
import com.minhphuc.weapons.client.renderer.AtomicPostFx;
import com.minhphuc.weapons.client.renderer.AtomicRenderer;
import com.minhphuc.weapons.client.renderer.ClientAzathothDragonRenderer;
import com.minhphuc.weapons.client.renderer.ClientOrbitalSpaceRenderer;
import com.minhphuc.weapons.client.renderer.ClientOrbitalStrikeRenderer;
import com.minhphuc.weapons.client.renderer.ClientSupernovaPlasmaRenderer;
import com.minhphuc.weapons.client.renderer.SupernovaAccretionRenderer;
import com.minhphuc.weapons.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class WeaponsModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.minhphuc.weapons.client.WeaponsModClient.initClient();
        ClientOrbitalCinematicHandler.init();
        ClientHellflareCinematicHandler.init();
        ClientCinematicSkillHandler.init();

        // 1. Khai báo RenderType bán trong suốt cho các khối kết giới
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

        // 2. Tự động tải lại Shader khi Resource Reload
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "atomic_post_effects");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                AtomicPostFx.reload();
            }
        });

        // 3. Render năng lượng OpenGL Additive Blending (Sau khi các hạt và vật thể bán trong suốt đã vẽ xong)
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            PoseStack matrices = context.matrixStack();
            Camera camera = context.camera();
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
            AtomicRenderer.render(matrices, camera, tickDelta);
            SupernovaAccretionRenderer.render(matrices, camera, tickDelta);
            ClientOrbitalStrikeRenderer.render(matrices, camera, tickDelta);
            ClientAzathothDragonRenderer.render(matrices, camera, tickDelta);
            ClientSupernovaPlasmaRenderer.render(matrices, camera, tickDelta);
            ClientHellflareRenderer.render(matrices, camera, tickDelta);
            ClientCinematicSkillRenderer.render(matrices, camera, tickDelta);

            // Render Không gian vũ trụ, Địa Cầu uốn cong & Vệ tinh cho Tuyệt Kỹ Stellar Remote
            if (ClientOrbitalCinematicHandler.isActive()) {
                float spaceAge = ClientOrbitalCinematicHandler.getAge(tickDelta);
                if (spaceAge >= 0 && spaceAge < 46.0f) {
                    ClientOrbitalSpaceRenderer.render(matrices, camera, tickDelta,
                            ClientOrbitalCinematicHandler.getTargetPos(),
                            ClientOrbitalCinematicHandler.getStrikeMode(),
                            spaceAge);
                }
            }
        });

        // 4. Bắt ma trận và xử lý Shaders GLSL Post-Processing (Bloom, Aberration, Volumetric Beam)
        WorldRenderEvents.LAST.register(context -> {
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
            AtomicPostFx.capture(context.positionMatrix(), context.projectionMatrix(), tickDelta);
            AtomicPostFx.renderAfterWorld();
        });

        // 5. Render HUD Overlay: 4 Manga Impact Frames & Widescreen Letterbox + Typography
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            CustomImpactFrameOverlay.render(graphics, partialTick);
            ClientNihilityCinematicHandler.renderCinematicOverlay(graphics, partialTick);
            ClientOrbitalStrikeRenderer.renderHudOverlay(graphics, partialTick);
            ClientOrbitalCinematicHandler.renderOverlay(graphics, partialTick);
            ClientHellflareCinematicHandler.renderCinematicOverlay(graphics, partialTick);
            ClientCinematicSkillHandler.renderCinematicOverlay(graphics, partialTick);
        });

        // 6. Tự động dọn dẹp bộ nhớ đồ họa và trạng thái khi thoát thế giới hoặc tắt game
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AtomicRenderer.reset();
            AtomicPostFx.reset();
            CustomImpactFrameOverlay.reset();
            ClientNihilityCinematicHandler.stopCinematic();
            SupernovaAccretionRenderer.reset();
            ClientOrbitalStrikeRenderer.reset();
            ClientOrbitalCinematicHandler.stopCinematic();
            ClientAzathothDragonRenderer.reset();
            ClientAzathothCinematicHandler.stopCinematic();
            ClientSupernovaPlasmaRenderer.reset();
            ClientHellflareRenderer.reset();
            ClientHellflareCinematicHandler.stopCinematic();
            ClientCinematicSkillRenderer.reset();
            ClientCinematicSkillHandler.stopCinematic();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            AtomicRenderer.reset();
            AtomicPostFx.reset();
            CustomImpactFrameOverlay.reset();
            ClientNihilityCinematicHandler.stopCinematic();
            SupernovaAccretionRenderer.reset();
            ClientOrbitalStrikeRenderer.reset();
            ClientOrbitalCinematicHandler.stopCinematic();
            ClientAzathothDragonRenderer.reset();
            ClientAzathothCinematicHandler.stopCinematic();
            ClientSupernovaPlasmaRenderer.reset();
            ClientHellflareRenderer.reset();
            ClientHellflareCinematicHandler.stopCinematic();
            ClientCinematicSkillRenderer.reset();
            ClientCinematicSkillHandler.stopCinematic();
            ClientAzathothDragonRenderer.reset();
            ClientAzathothCinematicHandler.stopCinematic();
            ClientSupernovaPlasmaRenderer.reset();
            ClientHellflareRenderer.reset();
            ClientHellflareCinematicHandler.stopCinematic();
            ClientHellflareRenderer.reset();
            ClientHellflareCinematicHandler.stopCinematic();
        });
    }
}
