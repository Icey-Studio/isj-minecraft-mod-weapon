package com.minhphuc.weapons.content.darkgathering;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BÔNG BỊT TAI PHONG ẤN (WARDING EARPLUGS)
 * Pháp khí phật môn giúp cách ly thính giác khỏi Tà Kinh Tuyệt Mệnh của Tà Kinh Đại Tăng (Dark Gathering).
 * - Có thể trang bị ở ô Mũ Giáp (Head slot).
 * - Hoặc nhấp [Chuột Phải] để nhét vào tai duy trì bảo vệ trong 5 phút (6000 ticks).
 * - Miễn nhiễm 100% với hiệu ứng độ hóa tử vong của tiếng tụng kinh.
 */
public class WardingEarplugsItem extends ArmorItem {

    // Quản lý thời gian hiệu lực bảo vệ của từng thực thể (UUID -> ticks còn lại)
    private static final Map<UUID, Integer> ACTIVE_EARPLUGS = new ConcurrentHashMap<>();

    public WardingEarplugsItem(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties.stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Kích hoạt bùa bịt tai 5 phút (6000 ticks)
            ACTIVE_EARPLUGS.put(player.getUUID(), 6000);

            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("§a§l[BÔNG BỊT TAI PHONG ẤN] §fĐã nhét bông bịt tai! Thính giác được bảo vệ khỏi Tà Kinh trong §e5 phút§f! 🔇"),
                        true
                );
                sp.playNotifySound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.5F, 1.2F);
                sp.playNotifySound(SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);

                ServerLevel sl = (ServerLevel) level;
                sl.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getEyeY(), sp.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.05D);
            }

            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Kiểm tra xem một thực thể có đang được bảo vệ khỏi Tà Kinh hay không
     */
    public static boolean isProtected(LivingEntity entity) {
        if (entity == null) return false;

        // 1. Đang đội Bông Bịt Tai ở ô Mũ Giáp
        ItemStack headStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.getItem() instanceof WardingEarplugsItem) {
            return true;
        }

        // 2. Đang cầm Bông Bịt Tai ở tay phụ
        ItemStack offhand = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        if (offhand.getItem() instanceof WardingEarplugsItem) {
            return true;
        }

        // 3. Có thời gian hiệu lực kích hoạt nhét tai
        Integer ticks = ACTIVE_EARPLUGS.get(entity.getUUID());
        return ticks != null && ticks > 0;
    }

    /**
     * Tối ưu hóa chu kỳ đếm ngược (Zero Lag):
     * Chỉ duyệt danh sách người dùng đang active (O(K) thay vì quét toàn bộ players trên server).
     * Chỉ chạy 1 lần duy nhất mỗi tick trên Overworld.
     */
    public static void tickProtection(ServerLevel level) {
        if (ACTIVE_EARPLUGS.isEmpty()) return;
        if (level.dimension() != Level.OVERWORLD) return;

        Iterator<Map.Entry<UUID, Integer>> it = ACTIVE_EARPLUGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                it.remove();
            } else {
                entry.setValue(ticks);

                // Cảnh báo khi sắp hết tác dụng (còn 10s = 200 ticks)
                if (ticks == 200) {
                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (sp != null) {
                        sp.displayClientMessage(
                                Component.literal("§c⚠️ [CẢNH BÁO] Bông Bịt Tai Phong Ấn sắp hết tác dụng sau 10 giây!"),
                                true
                        );
                        sp.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
                    }
                }
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6§l[PHÁP KHÍ PHẬT MÔN] §eBông Bịt Tai Phong Ấn"));
        tooltip.add(Component.literal("§7Được kết từ len bạch linh và kinh phù phong ấn thính giác."));
        tooltip.add(Component.literal("§7Cách ly tâm trí khỏi mọi tà âm đoạt mệnh."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§a✔ Miễn nhiễm 100% với Tà Kinh Tuyệt Mệnh của Tà Kinh Đại Tăng!"));
        tooltip.add(Component.literal("§e[Chuột Phải]§7 để nhét vào tai (duy trì 5 phút)"));
        tooltip.add(Component.literal("§7hoặc trang bị trực tiếp tại ô §b[Mũ Giáp]§7."));
    }
}
