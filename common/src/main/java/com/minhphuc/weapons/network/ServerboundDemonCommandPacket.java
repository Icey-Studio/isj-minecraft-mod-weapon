package com.minhphuc.weapons.network;

import com.minhphuc.weapons.content.tensura.PrimordialPactItem;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Supplier;

public class ServerboundDemonCommandPacket {
    public static final int ACTION_CAST_SKILL = 0;
    public static final int ACTION_STANCE_FOLLOW = 1;
    public static final int ACTION_STANCE_GUARD = 2;
    public static final int ACTION_DISMISS = 3;

    private final int actionType;
    private final int skillIndex;

    public ServerboundDemonCommandPacket(int actionType, int skillIndex) {
        this.actionType = actionType;
        this.skillIndex = skillIndex;
    }

    public ServerboundDemonCommandPacket(FriendlyByteBuf buf) {
        this.actionType = buf.readInt();
        this.skillIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.actionType);
        buf.writeInt(this.skillIndex);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            // Tìm ác ma thuộc quyền sở hữu của người chơi trong phạm vi lân cận
            List<PrimordialDemonEntity> demons = level.getEntitiesOfClass(
                    PrimordialDemonEntity.class,
                    new AABB(player.getX() - 128, player.getY() - 64, player.getZ() - 128, player.getX() + 128, player.getY() + 64, player.getZ() + 128),
                    d -> d.isAlive() && d.isOwnedBy(player)
            );

            if (demons.isEmpty()) {
                // Hỗ trợ Thu Hồi Từ Xa (Remote Dismiss) khi Ác ma ở chunk xa đã bị unload
                if (actionType == ACTION_DISMISS) {
                    boolean found = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack st = player.getInventory().getItem(i);
                        if (st.getItem() instanceof PrimordialPactItem && ItemStackDataHelper.getBoolean(st, "IsSummoned")) {
                            ItemStackDataHelper.putBoolean(st, "IsSummoned", false);
                            ItemStackDataHelper.putString(st, "DemonUUID", "");
                            found = true;
                        }
                    }
                    if (found) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 0.8F);
                        player.displayClientMessage(Component.literal("§a✔ Đã triệu hồi Ác Ma từ phương xa trở về nghỉ ngơi trong Ấn Chú!"), true);
                        return;
                    }
                }

                player.displayClientMessage(Component.literal("§e[Khế Ước] Ác Ma đang ở xa (khu vực chưa nạp). Hãy bấm §6Shift + Chuột Phải §evào Khế Ước để Hiệu Triệu Ác Ma tới ngay cạnh bạn!"), true);
                return;
            }

            PrimordialDemonEntity demon = demons.get(0);

            switch (actionType) {
                case ACTION_CAST_SKILL -> {
                    LivingEntity target = demon.getTarget();
                    demon.executeCommandedSkill(skillIndex, target);
                    player.displayClientMessage(Component.literal("§6§l[Mệnh Lệnh] §eĐã ra lệnh cho §c§l" + demon.getDemonType().getColorName().toUpperCase() + " §ethực hiện kỹ năng!"), true);
                }
                case ACTION_STANCE_FOLLOW -> {
                    demon.setOrderedToSit(false);
                    player.displayClientMessage(Component.literal("§a§l[Mệnh Lệnh] §e" + demon.getDemonType().getColorName() + " §aĐANG ĐI THEO BẢO VỆ CHỦ NHÂN!"), true);
                }
                case ACTION_STANCE_GUARD -> {
                    demon.setOrderedToSit(true);
                    demon.getNavigation().stop();
                    player.displayClientMessage(Component.literal("§c§l[Mệnh Lệnh] §e" + demon.getDemonType().getColorName() + " §cĐANG ĐỨNG CANH GIỮ VỊ TRÍ!"), true);
                }
                case ACTION_DISMISS -> {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.1D);
                    level.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 0.8F);
                    player.displayClientMessage(Component.literal("§a✔ Đã cho §e" + demon.getDemonType().getColorName() + " §alui về nghỉ ngơi trong Ấn Chú!"), true);

                    // Cập nhật và lưu toàn bộ trạng thái tiến hóa, Ma Vương, máu, tên vào pact item trong túi đồ
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack st = player.getInventory().getItem(i);
                        if (st.getItem() instanceof PrimordialPactItem) {
                            if (PrimordialPactItem.getDemonType(st) == demon.getDemonType() || demon.getStringUUID().equals(ItemStackDataHelper.getString(st, "DemonUUID"))) {
                                ItemStackDataHelper.putBoolean(st, "IsSummoned", false);
                                ItemStackDataHelper.putString(st, "DemonUUID", "");
                                ItemStackDataHelper.putFloat(st, "CurrentHp", demon.getHealth());
                                ItemStackDataHelper.putFloat(st, "MaxHp", demon.getMaxHealth());
                                ItemStackDataHelper.putInt(st, "EvolutionTier", demon.getEvolutionTier());
                                ItemStackDataHelper.putBoolean(st, "HasPhysicalBody", demon.hasPhysicalBody());
                                ItemStackDataHelper.putBoolean(st, "IsNamed", demon.isNamed());
                                ItemStackDataHelper.putString(st, "CustomDemonName", demon.getCustomDemonName());

                                boolean isDemonLord = demon.getTags().contains("TensuraDemonLordMob") || demon.getEvolutionTier() >= 2;
                                ItemStackDataHelper.putBoolean(st, "IsDemonLord", isDemonLord);
                                ItemStackDataHelper.putBoolean(st, "IsAwakenedDemonLord", isDemonLord);

                                StringBuilder sb = new StringBuilder();
                                for (com.minhphuc.weapons.content.tensura.PrimordialSkillPool.SkillEntry sk : demon.getRandomSkills()) {
                                    if (sb.length() > 0) sb.append(",");
                                    sb.append(sk.id());
                                }
                                ItemStackDataHelper.putString(st, "PrimordialSkills", sb.toString());
                                break;
                            }
                        }
                    }

                    demon.discard();
                }
            }
        });
    }
}
