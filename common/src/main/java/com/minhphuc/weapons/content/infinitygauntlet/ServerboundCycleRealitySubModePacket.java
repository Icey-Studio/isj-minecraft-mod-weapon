package com.minhphuc.weapons.content.infinitygauntlet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import com.minhphuc.weapons.data.ItemStackDataHelper;
import java.util.function.Supplier;

public class ServerboundCycleRealitySubModePacket {

    public ServerboundCycleRealitySubModePacket() {
    }

    public ServerboundCycleRealitySubModePacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack heldStack = player.getMainHandItem();
            if (!(heldStack.getItem() instanceof InfinityGauntletItem)) {
                heldStack = player.getOffhandItem();
            }

            if (heldStack.getItem() instanceof InfinityGauntletItem) {
                int mainMode = ItemStackDataHelper.getInt(heldStack, InfinityGauntletItem.NBT_MODE);
                if (mainMode == 3) { // Đá Thực Tại (Reality Stone)
                    int currentSubMode = ItemStackDataHelper.getInt(heldStack, "RealitySubMode");
                    int nextSubMode = (currentSubMode + 1) % 3;
                    ItemStackDataHelper.putInt(heldStack, "RealitySubMode", nextSubMode);

                    String subModeMessage = switch (nextSubMode) {
                        case 0 -> "§c[ĐÁ THỰC TẠI] §fBáo cáo. Chế độ phụ: §e1. Cấu Trúc Thực Tại (Normal)";
                        case 1 -> "§c[ĐÁ THỰC TẠI] §fBáo cáo. Chế độ phụ: §b2. Đóng Băng Thực Tại (Frozen)";
                        case 2 -> "§c[ĐÁ THỰC TẠI] §fBáo cáo. Chế độ phụ: §a3. Phục Hồi Sự Sống (Life)";
                        default -> "";
                    };

                    player.displayClientMessage(Component.literal(subModeMessage), true);

                    float pitch = 0.8F + (nextSubMode * 0.3F);
                    player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        1.0F, pitch
                    );
                } else if (mainMode == 4) { // Đá Linh Hồn (Soul Stone)
                    int currentSubMode = ItemStackDataHelper.getInt(heldStack, "SoulSubMode");
                    int nextSubMode = (currentSubMode + 1) % 3;
                    ItemStackDataHelper.putInt(heldStack, "SoulSubMode", nextSubMode);

                    String subModeMessage = switch (nextSubMode) {
                        case 0 -> "§6[ĐÁ LINH HỒN] §fBáo cáo. Chế độ phụ: §e1. 🔥 Soul Harvest (Gặt Hái Linh Hồn)";
                        case 1 -> "§6[ĐÁ LINH HỒN] §fBáo cáo. Chế độ phụ: §b2. 👻 Soul Puppet (Chiêu Hồn Phụ Tá)";
                        case 2 -> "§6[ĐÁ LINH HỒN] §fBáo cáo. Chế độ phụ: §c3. 💥 Soul Extraction (Tách & Thiêu Rụi Linh Hồn)";
                        default -> "";
                    };

                    player.displayClientMessage(Component.literal(subModeMessage), true);

                    float pitch = 0.8F + (nextSubMode * 0.3F);
                    player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SOUL_ESCAPE,
                        SoundSource.PLAYERS,
                        1.0F, pitch
                    );
                } else if (mainMode == 5) { // Đá Thời Gian (Time Stone)
                    ItemStackDataHelper.putInt(heldStack, "TimeSubMode", 0);
                    String subModeMessage = "§a[ĐÁ THỜI GIAN] §fBáo cáo. Chức năng: §b🌿 Age Decay & Growth (Lão Hóa & Sinh Trưởng)";

                    player.displayClientMessage(Component.literal(subModeMessage), true);

                    player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_POWER_SELECT,
                        SoundSource.PLAYERS,
                        1.0F, 1.2F
                    );
                } else if (mainMode == 6) { // Đá Tâm Trí (Mind Stone)
                    int currentSubMode = ItemStackDataHelper.getInt(heldStack, "MindSubMode");
                    int nextSubMode = (currentSubMode + 1) % 3;
                    ItemStackDataHelper.putInt(heldStack, "MindSubMode", nextSubMode);

                    String subModeMessage = switch (nextSubMode) {
                        case 0 -> "§e[ĐÁ TÂM TRÍ] §fBáo cáo. Chế độ phụ: §61. 👑 Vương Quyền Chi Phối (Hypnosis)";
                        case 1 -> "§e[ĐÁ TÂM TRÍ] §fBáo cáo. Chế độ phụ: §b2. 🌀 Telekinesis (Thao Túng Vật Lý)";
                        case 2 -> "§e[ĐÁ TÂM TRÍ] §fBáo cáo. Chế độ phụ: §c3. ⚡ Mind Beam Laser (Vision)";
                        default -> "";
                    };

                    player.displayClientMessage(Component.literal(subModeMessage), true);

                    float pitch = 0.9F + (nextSubMode * 0.3F);
                    player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        1.0F, pitch
                    );
                }
            }
        });
    }
}
