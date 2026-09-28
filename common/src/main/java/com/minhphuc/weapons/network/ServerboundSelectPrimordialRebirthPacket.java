package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.VoiceOfTheWorld;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.init.ModItems;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class ServerboundSelectPrimordialRebirthPacket {

    private final String demonTypeName;

    public ServerboundSelectPrimordialRebirthPacket(DemonType type) {
        this.demonTypeName = type != null ? type.name() : "";
    }

    public ServerboundSelectPrimordialRebirthPacket(FriendlyByteBuf buf) {
        this.demonTypeName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.demonTypeName);
    }

    public void handle(java.util.function.Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            ServerLevel level = (ServerLevel) player.level();

            DemonType chosenType;
            try {
                chosenType = DemonType.valueOf(this.demonTypeName);
            } catch (IllegalArgumentException e) {
                return;
            }

            // Tiêu hao sách trong tay hoặc túi đồ nếu không ở Creative
            boolean hasTome = false;
            if (player.isCreative()) {
                hasTome = true;
            } else {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack st = player.getInventory().getItem(i);
                    if (st.is(ModItems.PRIMORDIAL_REBIRTH_TOME.get())) {
                        st.shrink(1);
                        hasTome = true;
                        break;
                    }
                }
            }

            if (!hasTome) {
                player.displayClientMessage(Component.literal("§c§l[CẢNH BÁO] §eBạn không có Sách Cổ Khởi Nguyên Thủy Tổ để thực hiện dung hợp!"), true);
                return;
            }

            // Thiết lập thân phận Thủy Tổ
            PrimordialPlayerDataHelper.setPrimordialType(player, chosenType);

            // Nếu chọn Noir: Mặc định tặng ngay Móng Vuốt Thủy Tổ (Abyssal Claw of Despair)
            if (chosenType == DemonType.NOIR) {
                ItemStack claw = new ItemStack(ModItems.ABYSSAL_CLAW_DESPAIR.get());
                if (!player.getInventory().add(claw)) {
                    player.drop(claw, false);
                }
            }

            // Kích hoạt Cột Sáng Thiên Không & 5 Tầng Vòng Tròn Ma Thuật Nhỏ To Xếp DỌC
            com.minhphuc.weapons.content.tensura.PentagramCelestialPillarAbility.spawnPillarAt(
                    level, player, player.position().add(0, 0.05, 0), chosenType, false, true
            );

            // Hiệu ứng âm thanh & hình ảnh chấn động
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.8F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.9F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.7F);

            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.8, 1.2, 0.8, 0.2);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.6, 1.0, 0.6, 0.1);

            String titleVi = PrimordialPlayerDataHelper.getDemonTitleVi(chosenType);
            VoiceOfTheWorld.announce(player, "Báo cáo. Cá thể đã hoàn tất dung hợp linh hồn với " + titleVi + "! Trạng thái Ác Ma Linh Thể đã được thiết lập.");

            String broadcast = "§6§l✦ THỨC TỈNH THỦY TỔ ✦ §eNgười chơi §f" + player.getName().getString() + " §eđã thừa kế sức mạnh của §c" + titleVi + "§e!";
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(player) <= 64.0 * 64.0) {
                    p.displayClientMessage(Component.literal(broadcast), false);
                }
            }
        });
    }
}
