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
    public static final int ACTION_TOGGLE_MASSACRE = 4;

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

                // TỰ ĐỘNG HIỆU TRIỆU XUYÊN KHÔNG GIAN KHI BẤM "ĐI THEO" HOẶC "RA LỆNH KỸ NĂNG"
                ItemStack pact = player.getMainHandItem();
                if (!(pact.getItem() instanceof PrimordialPactItem)) pact = player.getOffhandItem();
                if (!(pact.getItem() instanceof PrimordialPactItem)) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack st = player.getInventory().getItem(i);
                        if (st.getItem() instanceof PrimordialPactItem && ItemStackDataHelper.getBoolean(st, "IsSummoned")) {
                            pact = st;
                            break;
                        }
                    }
                }

                if (!pact.isEmpty() && pact.getItem() instanceof PrimordialPactItem) {
                    boolean isGuarding = ItemStackDataHelper.getBoolean(pact, "IsGuarding");
                    if (actionType == ACTION_STANCE_FOLLOW || (!isGuarding && actionType == ACTION_CAST_SKILL)) {
                        PrimordialDemonEntity remoteDemon = PrimordialPactItem.summonRemoteDemon(level, player, pact);
                        if (remoteDemon != null) {
                            if (actionType == ACTION_STANCE_FOLLOW) {
                                remoteDemon.setGuardMode(false, null);
                                ItemStackDataHelper.putBoolean(pact, "IsGuarding", false);
                            } else if (actionType == ACTION_CAST_SKILL) {
                                remoteDemon.executeCommandedSkill(skillIndex, remoteDemon.getTarget());
                            }
                            return;
                        }
                    } else if (isGuarding) {
                        player.displayClientMessage(Component.literal("§e[Khế Ước] Ác Ma đang tuần tra canh giữ ở khu vực đã chỉ định. Bấm nút §a[Đi Theo] §enếu muốn triệu tập Ác Ma về cạnh bạn!"), true);
                        return;
                    }
                }

                player.displayClientMessage(Component.literal("§e[Khế Ước] Ác Ma đang ở xa. Hãy bấm §6Shift + Chuột Phải §evào Khế Ước để Hiệu Triệu Ác Ma tới ngay cạnh bạn!"), true);
                return;
            }

            PrimordialDemonEntity demon = demons.get(0);
            ItemStack heldPact = player.getMainHandItem().getItem() instanceof PrimordialPactItem ? player.getMainHandItem()
                    : (player.getOffhandItem().getItem() instanceof PrimordialPactItem ? player.getOffhandItem() : ItemStack.EMPTY);
            if (!heldPact.isEmpty()) {
                var targetType = PrimordialPactItem.getDemonType(heldPact);
                for (PrimordialDemonEntity d : demons) {
                    if (d.getDemonType() == targetType) {
                        demon = d;
                        break;
                    }
                }
            }

            switch (actionType) {
                case ACTION_CAST_SKILL -> {
                    LivingEntity target = demon.getTarget();
                    demon.executeCommandedSkill(skillIndex, target);
                    player.displayClientMessage(Component.literal("§6§l[Mệnh Lệnh] §eĐã ra lệnh cho §c§l" + demon.getDemonType().getColorName().toUpperCase() + " §ethực hiện kỹ năng!"), true);
                }
                case ACTION_STANCE_FOLLOW -> {
                    demon.setGuardMode(false, null);
                    demon.setMassacreMode(false);
                    if (!heldPact.isEmpty()) {
                        ItemStackDataHelper.putBoolean(heldPact, "IsGuarding", false);
                    }
                    // Dịch chuyển ác ma đến cạnh chủ nhân nếu đang cách xa > 4 block
                    if (demon.distanceToSqr(player) > 16.0D) {
                        demon.teleportTo(player.getX() + player.getLookAngle().x * 2.0D, player.getY(), player.getZ() + player.getLookAngle().z * 2.0D);
                        level.sendParticles(ParticleTypes.PORTAL, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 20, 0.3D, 0.5D, 0.3D, 0.05D);
                        level.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2F, 1.0F);
                    }
                    player.displayClientMessage(Component.literal("§a§l[Mệnh Lệnh] §e" + demon.getDemonType().getColorName() + " §aĐANG ĐI THEO BẢO VỆ CHỦ NHÂN!"), true);
                }
                case ACTION_STANCE_GUARD -> {
                    demon.setGuardMode(true, demon.blockPosition());
                    demon.getNavigation().stop();
                    if (!heldPact.isEmpty()) {
                        ItemStackDataHelper.putBoolean(heldPact, "IsGuarding", true);
                    }
                    player.displayClientMessage(Component.literal("§e§l[Mệnh Lệnh] §6" + demon.getDemonType().getColorName() + " §eĐANG TUẦN TRA CANH GIỮ KHU VỰC RỘNG QUANH ĐÂY!"), true);
                }
                case ACTION_TOGGLE_MASSACRE -> {
                    boolean nowMassacre = !demon.isMassacreMode();
                    demon.setMassacreMode(nowMassacre);
                    if (nowMassacre) {
                        level.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5F, 1.3F);
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.08D);
                        demon.broadcastNearbyDialogue("Ara ara~ Theo mệnh lệnh của chủ nhân, ngôi làng này sẽ nhuốm sắc trắng của cái chết!", 48.0D);
                        player.displayClientMessage(Component.literal("§4§l[CHẾ ĐỘ TÀN SÁT] §cBlanc đã bắt đầu tự động truy sát toàn bộ Dân Làng & Iron Golem trong bán kính 100m!"), true);
                    } else {
                        demon.broadcastNearbyDialogue("Ta sẽ thu tay lại theo ý ngài, chủ nhân~", 32.0D);
                        player.displayClientMessage(Component.literal("§a§l[Mệnh Lệnh] §fĐã dừng chế độ Tàn Sát của Blanc!"), true);
                    }
                }
                case ACTION_DISMISS -> {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, demon.getX(), demon.getY() + 1.0D, demon.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.1D);
                    level.playSound(null, demon.getX(), demon.getY(), demon.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 0.8F);
                    player.displayClientMessage(Component.literal("§a✔ Đã cho §e" + demon.getEffectiveDemonName() + " §alui về nghỉ ngơi trong Ấn Chú!"), true);

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

                                String nameToSave = demon.getCustomDemonName();
                                boolean isNamed = demon.isNamed() || (nameToSave != null && !nameToSave.isEmpty());
                                ItemStackDataHelper.putBoolean(st, "IsNamed", isNamed);
                                ItemStackDataHelper.putString(st, "CustomDemonName", nameToSave != null ? nameToSave : "");

                                boolean isDemonLord = demon.getTags().contains("TensuraDemonLordMob");
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
