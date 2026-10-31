package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.content.divine.MoonlightSwordItem;
import com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper;
import com.minhphuc.weapons.content.tensura.TensuraSkillDispatcher;
import com.minhphuc.weapons.data.EntityDataHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ServerboundCycleSkillPacket {

    public ServerboundCycleSkillPacket() {
    }

    public ServerboundCycleSkillPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            ItemStack clawStack = mainHand.getItem() instanceof com.minhphuc.weapons.content.tensura.PrimordialClawItem
                    ? mainHand
                    : (offHand.getItem() instanceof com.minhphuc.weapons.content.tensura.PrimordialClawItem ? offHand : ItemStack.EMPTY);
            ItemStack fanStack = mainHand.getItem() instanceof com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem
                    ? mainHand
                    : (offHand.getItem() instanceof com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem ? offHand : ItemStack.EMPTY);
            ItemStack swordStack = mainHand.getItem() instanceof MoonlightSwordItem
                    ? mainHand
                    : (offHand.getItem() instanceof MoonlightSwordItem ? offHand : ItemStack.EMPTY);

            if (!clawStack.isEmpty()) {
                // Chuyển đổi skill cho Móng Vuốt Vực Thẳm / Hư Không
                com.minhphuc.weapons.content.tensura.PrimordialClawItem.cycleSkill(player, clawStack);
            } else if (!fanStack.isEmpty()) {
                // Chuyển đổi skill cho Quạt Lông Vũ Velgrynd
                com.minhphuc.weapons.content.tensura.VelgryndFeatherFanItem.cycleSkill(player, fanStack);
            } else if (!swordStack.isEmpty()) {
                // 1. Chuyển đổi skill cho Nguyệt Quang Thần Tế Kiếm (Chỉ có 2 skill)
                MoonlightSwordItem.cycleSkill(player, swordStack);
            } else {
                // 2. Chuyển đổi skill cho Chân Ma Vương & Thần Linh Vũ Trang & Thủy Tổ Ác Ma
                boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
                boolean hasCreation = EntityDataHelper.getCustomData(player).getBoolean("TensuraMaterialCreation");
                boolean isWearingDivine = DivineArmorItem.isWearingAnyPiece(player);
                boolean isPrimordial = PrimordialPlayerDataHelper.isPrimordial(player);
                com.minhphuc.weapons.entity.tensura.DemonType demonType = PrimordialPlayerDataHelper.getPrimordialType(player);

                boolean isCreative = player.isCreative();

                if (isTrueDemonLord || isWearingDivine || isPrimordial || isCreative) {
                    int lordSkills = TensuraSkillDispatcher.getSkillCount(hasCreation);
                    int totalSkills = lordSkills + (isPrimordial ? 7 : 0);

                    int current = EntityDataHelper.getCustomData(player).getInt("TensuraDemonLordSkill");
                    int next = (current + 1) % totalSkills;
                    EntityDataHelper.getCustomData(player).putInt("TensuraDemonLordSkill", next);

                    String skillName;
                    if (next < lordSkills) {
                        skillName = TensuraSkillDispatcher.getSkillName(player, next, hasCreation);
                    } else {
                        int primordialIdx = next - lordSkills;
                        skillName = "§6§l" + (next + 1) + ". " + PrimordialPlayerDataHelper.getSkillName(demonType, primordialIdx);
                    }

                    String roleTag = isTrueDemonLord ? "§d§l[CHÂN MA VƯƠNG]" : (isWearingDivine ? "§6§l[THẦN LINH VŨ TRANG]" : (isPrimordial ? "§5§l[THỦY TỔ ÁC MA]" : "§b§l[KỸ NĂNG NGƯỜI CHƠI]"));

                    player.displayClientMessage(
                        Component.literal(roleTag + " §fKỹ năng: " + skillName + " §7(Chuột Phải để thi triển)"),
                        true
                    );

                    float pitch = 1.0F + ((next % 10) * 0.15F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, pitch);
                } else {
                    player.displayClientMessage(
                        Component.literal("§c§l[THÔNG BÁO] §fBáo cáo: Cá thể chưa thức tỉnh thành Chân Ma Vương hoặc chưa trang bị Giáp Thần Linh!"),
                        true
                    );
                }
            }
        });
    }
}
