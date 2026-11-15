package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.world.VeldoraWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dữ liệu & Tác vụ của [Dạ Dày Vô Hạn] & [Không Gian Ảo - Thần Hư Không Azathoth].
 * Lưu trực tiếp vào NBT người chơi (Zero lag):
 * - Nuốt chửng thực thể / vật phẩm bị nguyền vào Không Gian Ảo để xem danh sách, Phân tích kỹ năng, Giải phóng,
 *   Chuyển hóa thành Ma Tố hồi phục, hoặc Auto-Craft Potion / Magisteel.
 * - Dùng cùng luồng Phân Tích để Giải Phong Ấn Bạo Phong Long Veldora!
 */
public final class AzathothStomachData {

    private static final String KEY_STOMACH = "AzathothStomachList";
    public static final String KEY_VELDORA_BEFRIENDED = "VeldoraSummonUnlocked";
    public static final int MAX_SLOTS = 24;

    public record StomachEntry(String name, String kind, long epValue, String analyzedSkill) {}

    private AzathothStomachData() {}

    public static List<StomachEntry> getEntries(ServerPlayer player) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        List<StomachEntry> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new StomachEntry(
                    t.getString("Name"),
                    t.getString("Kind"),
                    t.getLong("Ep"),
                    t.getString("Skill")
            ));
        }
        return out;
    }

    public static boolean swallowTarget(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(12.0D));
        AABB search = new AABB(eye, reach).inflate(2.0D);

        List<VeldoraEntity> sealedSight = level.getEntitiesOfClass(VeldoraEntity.class, search, VeldoraEntity::isSealed);
        if (!sealedSight.isEmpty()) {
            unsealVeldoraViaAzathothBeam(level, player, sealedSight.get(0));
            return true;
        }

        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != player && e.isAlive() && !(e instanceof VeldoraEntity));
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (list.size() >= MAX_SLOTS) {
            player.displayClientMessage(Component.literal("§5⚠ Không Gian Ảo tạm đầy (" + MAX_SLOTS + " ô)! Hãy chuyển hóa bớt thành Ma Tố!"), true);
            return false;
        }

        if (!mobs.isEmpty()) {
            LivingEntity victim = mobs.get(0);
            CompoundTag entityNbt = new CompoundTag();
            if (!victim.saveAsPassenger(entityNbt)) {
                return false;
            }
            CompoundTag slot = new CompoundTag();
            slot.putString("Name", victim.getName().getString());
            slot.putString("Kind", "ENTITY");
            long ep = (long) (victim.getMaxHealth() * 400L);
            slot.putLong("Ep", ep);
            slot.putString("Skill", victim instanceof net.minecraft.world.entity.monster.Blaze ? "Hỏa Cầu Ma Tố" : "Ma Tố Cô Đọng");
            slot.put("EntityTag", entityNbt);
            list.add(slot);
            data.put(KEY_STOMACH, list);

            victim.discard();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.3F, 0.7F);
            player.displayClientMessage(Component.literal("§5§l[HƯ KHÔNG HẤP THỤ] §fĐã nuốt §d" + slot.getString("Name") + " §fvào Dạ Dày Vô Hạn!"), true);
            return true;
        }

        // Nuốt vật phẩm rơi trên đất (kể cả vật phẩm bị nguyền)
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, search);
        if (!drops.isEmpty()) {
            ItemEntity ie = drops.get(0);
            CompoundTag slot = new CompoundTag();
            slot.putString("Name", ie.getItem().getHoverName().getString() + " x" + ie.getItem().getCount());
            slot.putString("Kind", "ITEM");
            slot.putLong("Ep", 25000L * ie.getItem().getCount());
            slot.putString("Skill", "Vật Phẩm Cách Ly Không Gian Ảo");
            slot.put("ItemTag", ie.getItem().save(level.registryAccess()));
            list.add(slot);
            data.put(KEY_STOMACH, list);
            ie.discard();
            player.displayClientMessage(Component.literal("§5§l[DẠ DÀY VÔ HẠN] §fĐã cất giữ vật phẩm vào Không Gian Ảo!"), true);
            return true;
        }
        return false;
    }

    public static void recordVoidAbsorb(ServerPlayer player, LivingEntity victim) {
        if (player == null || victim == null) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (list.size() >= MAX_SLOTS) return;
        CompoundTag entityNbt = new CompoundTag();
        if (!victim.saveAsPassenger(entityNbt)) return;
        CompoundTag slot = new CompoundTag();
        slot.putString("Name", victim.getName().getString());
        slot.putString("Kind", "ENTITY");
        slot.putLong("Ep", (long) Math.max(1000.0F, victim.getMaxHealth() * 450L));
        slot.putString("Skill", "Hư Không Phân Rã (Azathoth)");
        slot.put("EntityTag", entityNbt);
        list.add(slot);
        data.put(KEY_STOMACH, list);
    }

    public static void unsealVeldoraViaAzathothBeam(ServerLevel level, ServerPlayer player, VeldoraEntity veldora) {
        ServerLevel ow = level.getServer().overworld();
        VeldoraWorldData caveData = VeldoraWorldData.get(ow);
        BlockPos cavePos = caveData.getOrRollCavePos(ow);

        com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.removeVeldoraCavePrison(ow, Vec3.atBottomCenterOf(cavePos));
        com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.removeVeldoraCavePrison(level, veldora.position());

        veldora.setSealed(false);
        veldora.setAllied(true);
        veldora.tame(player);
        veldora.setHumanForm(false);
        veldora.setCustomName(Component.literal("§b§l[BẠO PHONG LONG] §f§lVeldora Tempest"));
        veldora.setHealth(veldora.getMaxHealth());
        // Đặt an toàn gần người chơi để không tụt hố do tia Azathoth xóa đất
        Vec3 safePos = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(6.0D));
        veldora.moveTo(safePos.x, Math.max(player.getY(), level.getMinBuildHeight() + 5), safePos.z, player.getYRot() + 180.0F, 0.0F);

        caveData.markUnsealed(player.getUUID());
        CompoundTag data = EntityDataHelper.getCustomData(player);
        data.putBoolean(KEY_VELDORA_BEFRIENDED, true);

        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (list.size() < MAX_SLOTS) {
            CompoundTag slot = new CompoundTag();
            slot.putString("Name", "Long Giam Vô Hạn (Đã Giải Mã)");
            slot.putString("Kind", "ITEM");
            slot.putLong("Ep", 1_500_000L);
            slot.putString("Skill", "Long Chủng Phân Tích — Bạo Phong Long");
            slot.put("ItemTag", new ItemStack(Items.DRAGON_BREATH, 1).save(level.registryAccess()));
            list.add(slot);
            data.put(KEY_STOMACH, list);
        }

        SageManager.sendCornerAlert(player, "Thần Hư Không Azathoth đã nuốt chửng Long Giam Vô Hạn! Veldora được Giải Phong Ấn & mở khóa [Triệu Hồi Veldora]!", 0xFF38BDF8, 8500, false);
        player.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"KUAHAHAHA!! Azathoth đã nghiền nát Long Giam Vô Hạn rồi! Này bạn thân, hãy dùng [Triệu Hồi Veldora] bất cứ khi nào cần ta!\""), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0F, 0.9F);
    }

    public static void analyzeOrUnsealVeldora(ServerLevel level, ServerPlayer player) {
        ServerLevel ow = level.getServer().overworld();
        VeldoraWorldData caveData = VeldoraWorldData.get(ow);
        BlockPos cavePos = caveData.getOrRollCavePos(ow);

        // Kiểm tra xem Veldora bị phong ấn có ở gần hay trong Hang Phong Ấn
        List<VeldoraEntity> sealedList = level.getEntitiesOfClass(VeldoraEntity.class, player.getBoundingBox().inflate(36.0D), VeldoraEntity::isSealed);
        boolean atCave = level == ow && player.blockPosition().distSqr(cavePos) <= 45 * 45;

        if (!sealedList.isEmpty() || (atCave && !caveData.isVeldoraUnsealed())) {
            com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.removeVeldoraCavePrison(ow, Vec3.atBottomCenterOf(cavePos));
            if (sealedList.isEmpty()) {
                VeldoraEntity spawned = ModEntities.VELDORA.get().create(ow);
                if (spawned != null) {
                    Vec3 p = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(5.0D));
                    spawned.moveTo(p.x, player.getY() + 0.5, p.z, 0.0F, 0.0F);
                    spawned.setSealed(false);
                    spawned.setAllied(true);
                    spawned.tame(player);
                    spawned.setHumanForm(false);
                    spawned.setCustomName(Component.literal("§b§l[BẠO PHONG LONG] §f§lVeldora Tempest"));
                    ow.addFreshEntity(spawned);
                }
            } else {
                for (VeldoraEntity v : sealedList) {
                    v.setSealed(false);
                    v.setAllied(true);
                    v.tame(player);
                    v.setCustomName(Component.literal("§b§l[BẠO PHONG LONG] §f§lVeldora Tempest"));
                }
            }
            caveData.markUnsealed(player.getUUID());
            EntityDataHelper.getCustomData(player).putBoolean(KEY_VELDORA_BEFRIENDED, true);

            SageManager.sendCornerAlert(player, "Phân Tích & Giải Phong Ấn Hoàn Tất! Bạo Phong Long Veldora đã được tự do và mở khóa kỹ năng [Triệu Hồi Veldora]!", 0xFF38BDF8, 8000, false);
            player.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"KUAHAHAHA!! Cuối cùng ta cũng tự do rồi! Này bạn thân, bất cứ lúc nào cần hãy gọi ta tới!\""), false);
            ow.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.5F, 1.0F);
        } else {
            SageManager.sendCornerAlert(player, "Không thấy Long Chủng Veldora trong phạm vi phân tích. Dùng [V] để dịch chuyển tới Hang Phong Ấn trước!", 0xFFF59E0B, 5500, true);
        }
    }

    public static void releaseEntry(ServerLevel level, ServerPlayer player, int index) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) return;

        CompoundTag slot = list.getCompound(index);
        String kind = slot.getString("Kind");
        Vec3 dropPos = player.position().add(player.getLookAngle().scale(2.5D));

        if ("ENTITY".equals(kind) && slot.contains("EntityTag")) {
            Optional<Entity> restored = EntityType.create(slot.getCompound("EntityTag"), level);
            restored.ifPresent(ent -> {
                ent.moveTo(dropPos.x, dropPos.y + 0.5, dropPos.z, player.getYRot(), 0.0F);
                level.addFreshEntity(ent);
            });
        } else if ("ITEM".equals(kind) && slot.contains("ItemTag")) {
            ItemStack st = ItemStack.parseOptional(level.registryAccess(), slot.getCompound("ItemTag"));
            if (!st.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, dropPos.x, dropPos.y + 0.5, dropPos.z, st));
            }
        }
        list.remove(index);
        data.put(KEY_STOMACH, list);
        player.displayClientMessage(Component.literal("§d✦ Đã Giải Phóng: §f" + slot.getString("Name")), true);
    }

    public static void convertToMagicules(ServerLevel level, ServerPlayer player, int index) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) return;

        CompoundTag slot = list.getCompound(index);
        long ep = slot.getLong("Ep");
        float heal = Math.min(player.getMaxHealth(), Math.max(8.0F, ep / 3500.0F));
        player.heal(heal);
        player.getFoodData().eat(8, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));

        list.remove(index);
        data.put(KEY_STOMACH, list);
        SageManager.sendCornerAlert(player, "Chuyển Hóa Ma Tố: Đã phân rã [" + slot.getString("Name") + "] -> Hồi +" + (int) heal + " HP!", 0xFF34D399, 4500, false);
    }

    public static void autoCraftItem(ServerLevel level, ServerPlayer player, int craftKind) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            player.displayClientMessage(Component.literal("§c⚠ Dạ Dày Vô Hạn cần ít nhất 1 vật phẩm/linh thể làm nguyên liệu Ma Tố!"), true);
            return;
        }
        list.remove(0);
        data.put(KEY_STOMACH, list);

        ItemStack crafted = (craftKind == 0)
                ? new ItemStack(Items.GOLDEN_APPLE, 2)
                : new ItemStack(Items.NETHERITE_INGOT, 1);
        player.getInventory().placeItemBackInInventory(crafted);
        SageManager.sendCornerAlert(player, "Auto-Craft Hoàn Tất: Đã tổng hợp " + crafted.getHoverName().getString() + " x" + crafted.getCount() + " trong Không Gian Ảo!", 0xFF38BDF8, 4500, false);
    }
}
