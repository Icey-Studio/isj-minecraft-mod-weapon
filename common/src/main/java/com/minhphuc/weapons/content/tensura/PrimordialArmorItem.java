package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.divine.DivineArmorItem;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.tensura.DemonType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 7 Bộ Trang Bị Huyền Thoại của Thất Đại Ác Ma Thủy Tổ (Primordial Demon Armor Sets - 28 món):
 * - Gồm đủ 4 món: Đầu (Mũ 3D), Áo, Quần, Giày cho cả 7 Thủy Tổ (Noir, Rouge, Blanc, Jaune, Violet, Bleu, Vert).
 * - Phẩm cấp: Huyền Thoại (Legendary) - Yếu hơn Giáp Thần Thoại (Divine Armor) 2 lần (50% giảm sát thương & 50% sát thương kỹ năng).
 * - Khi mặc đủ 4 món (Đầu + Áo + Quần + Giày) cùng bộ: Mở khóa toàn bộ Kỹ năng Chiến Đấu & Kỹ năng Tầm Soát đặc trưng của Thủy Tổ đó (Tìm Làng, Tìm Warden, Tìm Di Tích, Tìm Quặng...).
 * - Riêng Bộ của Noir (Diablo):
 *   + Bấm phím [X]: Không bật Auto-Battle của Raphael/Đại Hiền Giả mà hiện ra Móng Vuốt 3D giống Nhập Xác, sát thương đánh tay không bằng vuốt mạnh hơn gấp 2 lần (x2.0).
 *   + Khi bay: Hiện Cánh Ác Ma 3D giang rộng; khi đáp thì cánh thu vào; khi chạm đất thì mất cánh (6 Thủy Tổ còn lại bay không hiện cánh).
 */
public class PrimordialArmorItem extends ArmorItem {

    public static final String KEY_NOIR_CLAWS_ACTIVE = "NoirClawsActive";
    public static final String NBT_PLAYER_NOIR_CLAWS = "NoirArmorClawsActive";

    private final DemonType demonType;

    public PrimordialArmorItem(Holder<ArmorMaterial> material, DemonType demonType, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
        this.demonType = demonType;
    }

    public DemonType getDemonType() {
        return this.demonType;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static DemonType getPieceDemonType(Player player, EquipmentSlot slot) {
        if (player == null || slot == null) return null;
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.getItem() instanceof PrimordialArmorItem pai) {
            return pai.getDemonType();
        }
        return null;
    }

    public static boolean isWearingPiece(Player player, EquipmentSlot slot, DemonType type) {
        return type != null && getPieceDemonType(player, slot) == type;
    }

    public static boolean isWearingAnyPiece(Player player) {
        if (player == null) return false;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (player.getItemBySlot(slot).getItem() instanceof PrimordialArmorItem) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWearingAnyNoirArmor(Player player) {
        if (player == null) return false;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (getPieceDemonType(player, slot) == DemonType.NOIR) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWearingFullSet(Player player, DemonType type) {
        if (player == null || type == null) return false;
        return getPieceDemonType(player, EquipmentSlot.HEAD) == type
                && getPieceDemonType(player, EquipmentSlot.CHEST) == type
                && getPieceDemonType(player, EquipmentSlot.LEGS) == type
                && getPieceDemonType(player, EquipmentSlot.FEET) == type;
    }

    public static boolean isAnyPlayerWearingFullDemonSet(net.minecraft.server.MinecraftServer server, DemonType type) {
        if (server == null || type == null) return false;
        for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isWearingFullSet(p, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trả về DemonType nếu người chơi đang mặc đủ 4 món (Đầu + Áo + Quần + Giày) của cùng một Thủy Tổ.
     */
    public static DemonType getFullSetDemonType(Player player) {
        if (player == null) return null;
        DemonType head = getPieceDemonType(player, EquipmentSlot.HEAD);
        if (head == null) return null;
        if (getPieceDemonType(player, EquipmentSlot.CHEST) == head
                && getPieceDemonType(player, EquipmentSlot.LEGS) == head
                && getPieceDemonType(player, EquipmentSlot.FEET) == head) {
            return head;
        }
        return null;
    }

    public static boolean isWearingFullSetOfAnyDemon(Player player) {
        return getFullSetDemonType(player) != null;
    }

    /**
     * Kiểm tra trạng thái Móng Vuốt Tuyệt Vọng của Noir (bấm phím X khi mặc đồ Noir).
     * Đọc trực tiếp từ ItemStack CustomData trên mảnh giáp Noir đang mặc (được Minecraft tự động đồng bộ xuống mọi Client).
     */
    public static boolean isNoirClawsActive(Player player) {
        if (player == null || !isWearingAnyNoirArmor(player)) return false;
        for (EquipmentSlot slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack st = player.getItemBySlot(slot);
            if (st.getItem() instanceof PrimordialArmorItem pai && pai.getDemonType() == DemonType.NOIR) {
                if (ItemStackDataHelper.getBoolean(st, KEY_NOIR_CLAWS_ACTIVE)) {
                    return true;
                }
            }
        }
        return EntityDataHelper.getCustomData(player).getBoolean(NBT_PLAYER_NOIR_CLAWS);
    }

    /**
     * Bấm phím X khi đang mặc bộ đồ của Noir (Diablo):
     * - Chặn hoàn toàn tính năng Tự Động Chiến Đấu (Auto-Battle) của Raphael / Đại Hiền Giả.
     * - Bật / Tắt Móng Vuốt 3D giống Nhập Xác, tăng gấp 2 lần (x2.0) sát thương đánh tay không.
     */
    public static boolean toggleNoirClaws(ServerPlayer player) {
        if (player == null || !isWearingAnyNoirArmor(player)) return false;

        // Tắt ngay Auto-Battle của Raphael / Đại Hiền Giả nếu đang bật
        if (EntityDataHelper.getCustomData(player).getBoolean("TensuraRaphaelAutoBattle")) {
            EntityDataHelper.getCustomData(player).putBoolean("TensuraRaphaelAutoBattle", false);
            TensuraSkillHelper.syncSkillStateToClient(player);
        }

        boolean next = !isNoirClawsActive(player);
        EntityDataHelper.getCustomData(player).putBoolean(NBT_PLAYER_NOIR_CLAWS, next);

        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack st = player.getItemBySlot(slot);
            if (st.getItem() instanceof PrimordialArmorItem pai && pai.getDemonType() == DemonType.NOIR) {
                ItemStackDataHelper.putBoolean(st, KEY_NOIR_CLAWS_ACTIVE, next);
            }
        }

        ServerLevel sl = player.serverLevel();
        if (next) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 0.65F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.6F, 1.2F);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + player.getLookAngle().x * 1.2D,
                    player.getY() + 1.1D, player.getZ() + player.getLookAngle().z * 1.2D, 3, 0.25D, 0.2D, 0.25D, 0.0D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
                    25, 0.4D, 0.5D, 0.4D, 0.06D);
            player.displayClientMessage(
                    Component.literal("§8§l[DIABLO - NOIR] §d§l⚡ ĐÃ TRIỆU HỒI MÓNG VUỐT TUYỆT VỌNG! §e(Đánh tay không x2.0 sát thương | Vô hiệu Auto-Battle Raphael)"),
                    true
            );
        } else {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.2F, 0.9F);
            player.displayClientMessage(
                    Component.literal("§8§l[DIABLO - NOIR] §7Đã thu hồi Móng Vuốt Tuyệt Vọng."),
                    true
            );
        }
        return next;
    }

    /**
     * Tỷ lệ giảm sát thương của Giáp Thủy Tổ (Huyền Thoại):
     * - Yếu hơn Giáp Thần Thoại (Divine Armor ~100%) đúng 2 lần -> Đủ bộ 4 món giảm 50% sát thương!
     *   + Đầu (Helmet): 10%
     *   + Áo (Chestplate): 20%
     *   + Quần (Leggings): 12%
     *   + Giày (Boots): 8%
     */
    public static float getDamageReductionFraction(Player player) {
        if (player == null) return 0.0F;
        float reduction = 0.0F;
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof PrimordialArmorItem) reduction += 0.10F;
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PrimordialArmorItem) reduction += 0.20F;
        if (player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof PrimordialArmorItem) reduction += 0.12F;
        if (player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof PrimordialArmorItem) reduction += 0.08F;
        return Math.min(0.50F, reduction);
    }

    /**
     * Hệ số uy lực kỹ năng khi người chơi thi triển nhờ mặc Bộ Giáp Thủy Tổ (Huyền Thoại):
     * - Nếu người chơi chỉ mặc Bộ Giáp Thủy Tổ (chưa thức tỉnh Chân Ma Vương, không mặc Giáp Thần Thoại, không Nhập Xác, không phải Thủy Tổ Chuyển Sinh):
     *   Uy lực kỹ năng yếu hơn Áo Thần Thoại 2 lần -> nhân 0.5F (50% sát thương)!
     */
    public static float getSkillPowerMultiplier(Player player) {
        if (player == null) return 1.0F;
        if (PrimordialPossessionManager.isPossessing(player)) return 1.0F;
        if (DivineArmorItem.isWearingAnyPiece(player)) return 1.0F;
        if (EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord")) return 1.0F;
        if (PrimordialPlayerDataHelper.isInnatePrimordial(player)) return 1.0F;
        if (isWearingFullSetOfAnyDemon(player)) {
            return 0.50F; // Yếu hơn Áo Thần Thoại 2 lần
        }
        return 1.0F;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) return;
        if (sp.getItemBySlot(this.type.getSlot()) != stack) return;

        // 1. Dập lửa và miễn sát thương rơi
        sp.clearFire();
        sp.resetFallDistance();

        // 2. Nếu đang mặc đồ Noir (Diablo): Tự động chặn Auto-Battle của Raphael / Đại Hiền Giả
        if (this.demonType == DemonType.NOIR && EntityDataHelper.getCustomData(sp).getBoolean("TensuraRaphaelAutoBattle")) {
            EntityDataHelper.getCustomData(sp).putBoolean("TensuraRaphaelAutoBattle", false);
            TensuraSkillHelper.syncSkillStateToClient(sp);
            sp.displayClientMessage(
                    Component.literal("§8§l[DIABLO - NOIR] §cTrang phục Hắc Sắc Thủy Tổ từ chối sự tiếp quản tự động của Raphael/Đại Hiền Giả! (Bấm [X] để bật Móng Vuốt)"),
                    true
            );
        }

        // 3. Mặc đủ bộ 4 món: Hồi phục nhẹ Ma Tố & Sinh Lực mỗi 2 giây
        if (this.type == Type.CHESTPLATE && isWearingFullSet(sp, this.demonType) && sp.tickCount % 40 == 0) {
            if (sp.getHealth() < sp.getMaxHealth()) {
                sp.heal(2.0F);
            }
            MagiculesManager.restoreMagicules(sp, Math.max(2_500L, MagiculesManager.getMaxMagicules(sp) / 100L));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String colorCode = switch (this.demonType) {
            case NOIR -> "§8§l";
            case ROUGE -> "§c§l";
            case BLANC -> "§f§l";
            case JAUNE -> "§e§l";
            case VIOLET -> "§d§l";
            case BLEU -> "§9§l";
            case VERT -> "§a§l";
        };
        tooltip.add(Component.literal("§6§l[TRANG BỊ HUYỀN THOẠI • THỦY TỔ ÁC MA]"));
        tooltip.add(Component.literal(colorCode + PrimordialPlayerDataHelper.getDemonTitleVi(this.demonType)));
        tooltip.add(Component.literal("§7Trang phục hiện thân 3D của Ác Ma Thủy Tổ cổ xưa."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e⚡ Thuộc Tính Cấp Huyền Thoại (Bằng 1/2 Giáp Thần Thoại):"));
        tooltip.add(Component.literal("§7- §aĐộ bền bất tử, kháng lửa & miễn sát thương rơi từ trên cao."));
        tooltip.add(Component.literal("§7- §bMặc đủ 4 món (Đầu + Áo + Quần + Giày): §fGiảm §e50% §fsát thương & ban khả năng §bBay Lượn§f!"));
        tooltip.add(Component.literal("§7- §dMở khóa Toàn Bộ Kỹ Năng Thủy Tổ §7(Uy lực bằng §e50% §7so với Áo Thần Thoại):"));
        tooltip.add(Component.literal("§8  • Nhấn [Z] hoặc [G] để chọn: 9 Tuyệt Kỹ Chiến Đấu & Lãnh Địa của " + this.demonType.getColorName()));
        tooltip.add(Component.literal("§8  • Kèm 5 Kỹ Năng Tầm Soát: Tìm Làng, Tìm Warden, Tìm Di Tích, Tìm Quặng, Tìm Boss!"));
        if (this.demonType == DemonType.NOIR) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§5✦ Đặc Quyền Độc Bản Hắc Sắc Thủy Tổ (Diablo / Noir):"));
            tooltip.add(Component.literal("§7- §cPhím [X]: §fChặn Auto-Battle của Raphael/Đại Hiền Giả, thay bằng triệu hồi §d§lMóng Vuốt Tuyệt Vọng 3D§f!"));
            tooltip.add(Component.literal("§7- §6Sát thương Vuốt: §fĐánh tay không khi bật Vuốt gây §c§lGẤP 2 LẦN (x2.0) §fsát thương!"));
            tooltip.add(Component.literal("§7- §bCánh Ác Ma Động: §fKhi bay giang rộng cánh, khi đáp thu cánh vào và chạm đất tự ẩn cánh!"));
        }
    }
}
