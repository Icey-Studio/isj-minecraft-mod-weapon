package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.world.VeldoraWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

    public static final String KEY_STOMACH = "AzathothStomachList";
    public static final String KEY_VELDORA_BEFRIENDED = "VeldoraSummonUnlocked";
    public static final String KEY_AUTO_LOOT = "AzathothAutoLoot";
    public static final int MAX_SLOTS = 120;

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

        // Thu hồi Veldora duy nhất vào Không Gian Ảo Dạ Dày Vô Hạn nếu người chơi nhắm thẳng vào ông ta
        List<VeldoraEntity> liveVeldoras = level.getEntitiesOfClass(VeldoraEntity.class, search, e -> e.isAlive() && !e.isSealed());
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (list.size() >= MAX_SLOTS) {
            player.displayClientMessage(Component.literal("§5⚠ Không Gian Ảo tạm đầy (" + MAX_SLOTS + " ô)! Hãy chuyển hóa bớt thành Ma Tố!"), true);
            return false;
        }

        if (!liveVeldoras.isEmpty()) {
            VeldoraEntity targetV = liveVeldoras.get(0);
            if (targetV.isGreedControlled()) {
                player.displayClientMessage(Component.literal("§c« BÁO CÁO RAPHAEL: THẤT BẠI »\n§eMạch linh hồn của Veldora đang phản nghịch do bị Kẻ Tham Lam chi phối! Không thể thu nạp vào Không Gian Ảo mà không làm rách vỡ kết cấu linh hồn!"), false);
                return false;
            }
            removeVeldoraStomachEntries(list);
            CompoundTag slot = new CompoundTag();
            slot.putString("Name", "§bBạo Phong Long Veldora (" + (targetV.isHumanForm() ? "Dạng Người" : "Dạng Rồng") + ")");
            slot.putString("Kind", "ENTITY");
            slot.putString("SpecialKind", "VELDORA");
            slot.putBoolean("HumanForm", targetV.isHumanForm());
            slot.putLong("Ep", 95_000_000L);
            slot.putString("Skill", "Long Chủng Chân Thân — Ấn Định Tuyệt Đối");
            list.add(slot);
            data.put(KEY_STOMACH, list);

            VeldoraWorldData.get(level.getServer().overworld()).setVeldoraStoredInStomach(true, targetV.isHumanForm());
            targetV.discard();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.4F, 0.75F);
            player.displayClientMessage(Component.literal("§5§l[KHÔNG GIAN VÔ TẬN] §fĐã thu hồi duy nhất §bBạo Phong Long Veldora §fvào Dạ Dày Vô Hạn!"), true);
            return true;
        }

        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != player && e.isAlive() && !(e instanceof VeldoraEntity));

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
            ItemStack st = ie.getItem().copy();
            if (storeItemStackIntoStomach(player, st, true)) {
                if (st.isEmpty()) {
                    ie.discard();
                } else {
                    ie.setItem(st);
                }
                return true;
            }
        }
        return false;
    }

    public static void removeVeldoraStomachEntries(ListTag list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag slot = list.getCompound(i);
            if ("VELDORA".equals(slot.getString("SpecialKind"))) {
                list.remove(i);
            } else if (slot.contains("EntityTag") && "weapons:veldora".equals(slot.getCompound("EntityTag").getString("id"))) {
                list.remove(i);
            }
        }
    }

    public static void recordVoidAbsorb(ServerPlayer player, LivingEntity victim) {
        if (player == null || victim == null || victim instanceof VeldoraEntity) return;
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
        caveData.setActiveVeldoraUuid(veldora.getUUID());
        VeldoraWorldData.findSingletonVeldora(level.getServer());
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
            VeldoraEntity singleton = VeldoraWorldData.findSingletonVeldora(level.getServer());
            if (singleton == null && !sealedList.isEmpty()) {
                singleton = sealedList.get(0);
            }
            if (singleton == null) {
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
                    caveData.setActiveVeldoraUuid(spawned.getUUID());
                }
            } else {
                singleton.setSealed(false);
                singleton.setAllied(true);
                singleton.tame(player);
                singleton.setCustomName(Component.literal("§b§l[BẠO PHONG LONG] §f§lVeldora Tempest"));
                caveData.setActiveVeldoraUuid(singleton.getUUID());
                for (VeldoraEntity extra : sealedList) {
                    if (extra != singleton) extra.discard();
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

        if ("VELDORA".equals(slot.getString("SpecialKind"))
                || ("ENTITY".equals(kind) && slot.contains("EntityTag") && "weapons:veldora".equals(slot.getCompound("EntityTag").getString("id")))) {
            boolean humanForm = slot.getBoolean("HumanForm");
            removeVeldoraStomachEntries(list);
            data.put(KEY_STOMACH, list);
            VeldoraWorldData.get(level.getServer().overworld()).setVeldoraStoredInStomach(true, humanForm);
            SummonVeldoraAbility.releaseOrReviveSingle(level, player, humanForm, false);
            return;
        }

        if ("ENTITY".equals(kind) && slot.contains("EntityTag")) {
            Optional<Entity> restored = EntityType.create(slot.getCompound("EntityTag"), level);
            restored.ifPresent(ent -> {
                ent.moveTo(dropPos.x, dropPos.y + 0.5, dropPos.z, player.getYRot(), 0.0F);
                level.addFreshEntity(ent);
            });
        } else if ("ITEM".equals(kind) && slot.contains("ItemTag")) {
            ItemStack st = ItemStack.parseOptional(level.registryAccess(), slot.getCompound("ItemTag"));
            if (!st.isEmpty()) {
                // Trả thẳng về túi đồ người chơi (chỉ rơi ra đất nếu túi đầy hoàn toàn)
                player.getInventory().placeItemBackInInventory(st);
                player.containerMenu.broadcastChanges();
            }
        }
        list.remove(index);
        data.put(KEY_STOMACH, list);
        player.displayClientMessage(Component.literal("§d✦ Đã Giải Phóng Về Túi Đồ: §f" + slot.getString("Name")), true);
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
        MagiculesManager.addAzathothConversion(player, Math.max(20_000L, ep));

        String slotName = slot.getString("Name");
        String itemKind = slot.getString("Kind");
        if (slotName.contains("Mariabell") || itemKind.contains("mariabell")) {
            if (!player.getTags().contains("GreedSkillUnlocked")) {
                player.addTag("GreedSkillUnlocked");
                data.putBoolean("GreedSkillUnlocked", true);
                VoiceOfTheWorld.announce(player, "§d§lTHỨC TỈNH KỸ NĂNG ĐỘC NHẤT THÀNH CÔNG!\n" +
                        "§fSau khi chuyển hóa hoàn toàn Bản Thể Linh Hồn của Mariabell Rosso vào Không Gian Ảo Azathoth,\n" +
                        "§fCá thể đã giác ngộ Kỹ Năng Độc Nhất: §d§l[KẺ THAM LAM - GREEDY ONE]§f!\n" +
                        "§eQuyền năng chi phối dục vọng, thao túng vạn vật đã được khắc sâu vào linh hồn vĩnh cửu!");
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.2F);
            } else {
                player.displayClientMessage(Component.literal("§d[Kẻ Tham Lam] §fBạn đã sở hữu Kỹ Năng Độc Nhất này rồi! Linh hồn chỉ gia tăng lượng lớn Ma Tố dự trữ."), true);
            }
        }

        list.remove(index);
        data.put(KEY_STOMACH, list);
        TensuraSkillHelper.syncSkillStateToClient(player);
        SageManager.sendCornerAlert(player, "Chuyển Hóa Ma Tố: Đã phân rã [" + slot.getString("Name") + "] -> Hồi +" + (int) heal + " HP & Tăng Ma Tố/EP vĩnh viễn!", 0xFF34D399, 4500, false);
    }

    public static void reconstituteEntity(ServerLevel level, ServerPlayer player, int index) {
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) {
            player.displayClientMessage(Component.literal("§c⚠ Vui lòng chọn một ô vật phẩm hoặc linh hồn trong Không Gian Ảo để tái tạo!"), true);
            player.playNotifySound(SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        CompoundTag slot = list.getCompound(index);
        String kind = slot.getString("Kind");

        // Ràng buộc 1: Nếu trong không gian ảo đã là MOB thực thể sống
        if ("MOB".equals(kind)) {
            player.displayClientMessage(Component.literal("§c§l« BÁO CÁO RAPHAEL: Không thể tái tạo vì đây là thực thể nguyên vẹn trong Không Gian Ảo! Hãy dùng chức năng [Giải Phóng] để đưa ra thế giới! »"), false);
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.2F, 0.9F);
            return;
        }

        var registryAccess = player.level().registryAccess();
        ItemStack stack = ItemStack.parseOptional(registryAccess, slot.getCompound("ItemTag"));
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.literal("§c⚠ Vật phẩm không hợp lệ hoặc dữ liệu trống!"), true);
            return;
        }

        // Ràng buộc 2: Tuyệt đối không thể tái tạo Veldora từ linh hồn
        String itemNameLower = stack.getHoverName().getString().toLowerCase();
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
        if (itemNameLower.contains("veldora") || itemId.contains("veldora")) {
            player.displayClientMessage(Component.literal("§c§l« CẢNH BÁO RAPHAEL: BẢO MẬT CHÂN LONG TỐI THƯỢNG! »\n§eKhông thể tái tạo Bạo Phong Long Veldora từ linh hồn! Cá thể Chân Long độc nhất chỉ có thể triệu hồi hoặc hồi sinh qua Kỹ Năng [Triệu Hồi Bạo Phong Long]!"), false);
            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 0.5F);
            return;
        }

        // Xác định EntityType dựa trên vật phẩm và Biome
        EntityType<?> targetType = null;
        boolean customRestored = false;
        CompoundTag customEntityTag = null;

        // Nếu là vật phẩm chứa NBT linh hồn thực thể cụ thể
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("TensuraEntityData")) {
            customEntityTag = customData.copyTag().getCompound("TensuraEntityData");
            String storedType = customEntityTag.getString("id");
            if (!storedType.isEmpty()) {
                var optType = EntityType.byString(storedType);
                if (optType.isPresent()) {
                    targetType = optType.get();
                    customRestored = true;
                }
            }
        }

        if (targetType == null) {
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = level.getBiome(player.blockPosition());
            boolean isFrozenBiome = biomeHolder.unwrapKey().map(k -> {
                String p = k.location().getPath().toLowerCase();
                return p.contains("frozen") || p.contains("snow") || p.contains("ice") || p.contains("cold");
            }).orElse(false);

            if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.COOKED_COD) || stack.is(Items.COOKED_SALMON)) {
                if (isFrozenBiome) {
                    targetType = EntityType.POLAR_BEAR; // Ở vùng tuyết băng -> Tái tạo ra Gấu Bắc Cực!
                } else {
                    targetType = stack.is(Items.SALMON) || stack.is(Items.COOKED_SALMON) ? EntityType.SALMON : EntityType.COD;
                }
            } else if (stack.is(Items.ROTTEN_FLESH)) {
                targetType = EntityType.ZOMBIE;
            } else if (stack.is(Items.BONE)) {
                targetType = EntityType.SKELETON;
            } else if (stack.is(Items.STRING) || stack.is(Items.SPIDER_EYE) || stack.is(Items.FERMENTED_SPIDER_EYE)) {
                targetType = (player.getY() < 0) ? EntityType.CAVE_SPIDER : EntityType.SPIDER;
            } else if (stack.is(Items.GUNPOWDER)) {
                targetType = EntityType.CREEPER;
            } else if (stack.is(Items.ENDER_PEARL) || stack.is(Items.ENDER_EYE)) {
                targetType = EntityType.ENDERMAN;
            } else if (stack.is(Items.BLAZE_ROD) || stack.is(Items.BLAZE_POWDER)) {
                targetType = EntityType.BLAZE;
            } else if (stack.is(Items.GHAST_TEAR)) {
                targetType = EntityType.GHAST;
            } else if (stack.is(Items.SLIME_BALL)) {
                targetType = EntityType.SLIME;
            } else if (stack.is(Items.MAGMA_CREAM)) {
                targetType = EntityType.MAGMA_CUBE;
            } else if (stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF) || stack.is(Items.LEATHER)) {
                targetType = EntityType.COW;
            } else if (stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP)) {
                targetType = EntityType.PIG;
            } else if (stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.FEATHER) || stack.is(Items.EGG)) {
                targetType = EntityType.CHICKEN;
            } else if (stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON) || stack.is(net.minecraft.tags.ItemTags.WOOL)) {
                targetType = EntityType.SHEEP;
            } else if (stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT) || stack.is(Items.RABBIT_FOOT) || stack.is(Items.RABBIT_HIDE)) {
                targetType = EntityType.RABBIT;
            } else if (stack.is(Items.WITHER_SKELETON_SKULL)) {
                targetType = EntityType.WITHER_SKELETON;
            } else if (stack.is(Items.PHANTOM_MEMBRANE)) {
                targetType = EntityType.PHANTOM;
            } else if (stack.is(Items.SHULKER_SHELL)) {
                targetType = EntityType.SHULKER;
            } else if (stack.is(Items.ECHO_SHARD) || stack.is(Items.SCULK)) {
                targetType = EntityType.WARDEN;
            } else if (stack.is(Items.HONEYCOMB) || stack.is(Items.HONEY_BOTTLE)) {
                targetType = EntityType.BEE;
            } else if (stack.is(Items.TURTLE_SCUTE)) {
                targetType = EntityType.TURTLE;
            } else if (stack.is(Items.INK_SAC)) {
                targetType = EntityType.SQUID;
            } else if (stack.is(Items.GLOW_INK_SAC)) {
                targetType = EntityType.GLOW_SQUID;
            } else if (stack.is(Items.GOAT_HORN)) {
                targetType = EntityType.GOAT;
            } else if (stack.is(Items.FROGSPAWN)) {
                targetType = EntityType.FROG;
            } else if (stack.is(Items.ARMADILLO_SCUTE)) {
                targetType = EntityType.ARMADILLO;
            } else if (stack.is(Items.SNIFFER_EGG)) {
                targetType = EntityType.SNIFFER;
            } else if (stack.is(com.minhphuc.weapons.init.ModItems.DEMON_LORD_SOUL.get())) {
                targetType = com.minhphuc.weapons.entity.ModEntities.UNDERWORLD_DEMON.get();
            }
        }

        if (targetType == null) {
            player.displayClientMessage(Component.literal("§c[Báo Cáo Raphael] Vật phẩm [" + stack.getHoverName().getString() + "] không chứa đủ thông tin ma tố/tế bào sinh học để tái tạo thành sinh vật sống!"), true);
            player.playNotifySound(SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        // Vị trí triệu hồi trước mặt người chơi 2.5m
        Vec3 look = player.getLookAngle();
        double spawnX = player.getX() + look.x * 2.5D;
        double spawnY = player.getY() + 0.2D;
        double spawnZ = player.getZ() + look.z * 2.5D;

        Entity entity = targetType.create(level);
        if (entity == null) {
            player.displayClientMessage(Component.literal("§c⚠ Không thể khởi tạo bản thể sinh vật trong thế giới!"), true);
            return;
        }

        if (customRestored && customEntityTag != null) {
            entity.load(customEntityTag);
        }
        entity.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0.0F);

        // Hiệu ứng Ma Tố Tái Tạo Sinh Mệnh
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spawnX, spawnY + 1.0D, spawnZ, 45, 0.5D, 0.8D, 0.5D, 0.1D);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, spawnX, spawnY + 0.8D, spawnZ, 30, 0.4D, 0.6D, 0.4D, 0.15D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnX, spawnY + 0.5D, spawnZ, 20, 0.3D, 0.4D, 0.3D, 0.05D);
        level.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5F, 1.1F);
        level.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.4F);

        level.addFreshEntity(entity);

        // Tiêu hao 1 vật phẩm
        if (stack.getCount() > 1) {
            stack.shrink(1);
            slot.putString("Name", stack.getHoverName().getString() + " x" + stack.getCount());
            slot.putLong("Ep", 25000L * stack.getCount());
            slot.put("ItemTag", stack.save(registryAccess));
        } else {
            list.remove(index);
        }
        data.put(KEY_STOMACH, list);

        SageManager.sendCornerAlert(player, "« Báo cáo »: Đã phân tích ma tố từ [" + stack.getHoverName().getString() + "] -> Tái tạo thành công " + SageManager.formatIndividual(entity) + "!", 0xFF38BDF8, 5500, false);
    }

    public static boolean storeItemStackIntoStomach(ServerPlayer player, ItemStack stack, boolean notify) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(KEY_STOMACH, Tag.TAG_COMPOUND);
        var registryAccess = player.level().registryAccess();
        String itemName = stack.getHoverName().getString();
        boolean changed = false;

        // Gộp vào ô cùng loại nếu còn chỗ chồng (Stackable merge)
        for (int i = 0; i < list.size() && !stack.isEmpty(); i++) {
            CompoundTag slot = list.getCompound(i);
            if (!"ITEM".equals(slot.getString("Kind")) || !slot.contains("ItemTag")) continue;
            ItemStack existing = ItemStack.parseOptional(registryAccess, slot.getCompound("ItemTag"));
            if (!existing.isEmpty() && existing.getCount() < existing.getMaxStackSize()
                    && ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int move = Math.min(space, stack.getCount());
                if (move > 0) {
                    existing.grow(move);
                    stack.shrink(move);
                    slot.putString("Name", existing.getHoverName().getString() + " x" + existing.getCount());
                    slot.putLong("Ep", 25000L * existing.getCount());
                    slot.put("ItemTag", existing.save(registryAccess));
                    changed = true;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (list.size() >= MAX_SLOTS) {
                if (changed) {
                    data.put(KEY_STOMACH, list);
                }
                if (notify) {
                    player.displayClientMessage(Component.literal("§5⚠ Không Gian Ảo tạm đầy (" + MAX_SLOTS + " ô)! Hãy chuyển hóa bớt thành Ma Tố!"), true);
                }
                return changed;
            }
            CompoundTag slot = new CompoundTag();
            ItemStack saved = stack.copy();
            slot.putString("Name", saved.getHoverName().getString() + " x" + saved.getCount());
            slot.putString("Kind", "ITEM");
            slot.putLong("Ep", 25000L * saved.getCount());
            slot.putString("Skill", "Vật Phẩm Cách Ly Không Gian Ảo");
            slot.put("ItemTag", saved.save(registryAccess));
            list.add(slot);
            stack.setCount(0);
            changed = true;
        }

        if (changed) {
            data.put(KEY_STOMACH, list);
            if (notify) {
                player.displayClientMessage(Component.literal("§5§l[DẠ DÀY VÔ HẠN] §fĐã cất §d" + itemName + " §fvào Không Gian Ảo!"), true);
            }
        }
        return changed;
    }

    public static void depositInventorySlot(ServerPlayer player, int invSlot) {
        if (player == null || invSlot < 0 || invSlot >= 36) return;
        ItemStack stack = player.getInventory().getItem(invSlot);
        if (stack.isEmpty()) return;
        ItemStack moving = stack.copy();
        if (storeItemStackIntoStomach(player, moving, true)) {
            player.getInventory().setItem(invSlot, moving);
            player.containerMenu.broadcastChanges();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.35F);
        }
    }

    public static void depositAllBackpack(ServerPlayer player) {
        if (player == null) return;
        int movedStacks = 0;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack st = player.getInventory().getItem(slot);
            if (st.isEmpty() || st.is(com.minhphuc.weapons.init.ModItems.GUIDE_BOOK.get())) continue;
            ItemStack moving = st.copy();
            if (storeItemStackIntoStomach(player, moving, false)) {
                player.getInventory().setItem(slot, moving);
                movedStacks++;
            }
        }
        player.containerMenu.broadcastChanges();
        if (movedStacks > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.25F);
            player.displayClientMessage(Component.literal("§5§l[AZATHOTH] §fĐã thu nạp §e" + movedStacks + " §fngăn đồ từ ba-lô vào Không Gian Ảo!"), true);
        } else {
            player.displayClientMessage(Component.literal("§7Ba-lô trống hoặc Không Gian Ảo đã đầy (" + MAX_SLOTS + " ô)."), true);
        }
    }

    public static boolean isAutoLootEnabled(ServerPlayer player) {
        if (player == null) return false;
        return EntityDataHelper.getCustomData(player).getBoolean(KEY_AUTO_LOOT);
    }

    public static void toggleAutoLoot(ServerPlayer player) {
        if (player == null) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        boolean next = !data.getBoolean(KEY_AUTO_LOOT);
        data.putBoolean(KEY_AUTO_LOOT, next);
        TensuraSkillHelper.syncSkillStateToClient(player);
        SageManager.sendCornerAlert(player,
                next ? "§dAuto-Loot Hư Không: §aBẬT §f— Vật phẩm rơi xung quanh sẽ tự động hút vào Dạ Dày Vô Hạn!"
                     : "§dAuto-Loot Hư Không: §cTẮT §f— Ngừng tự động hút vật phẩm rơi.",
                0xFFA855F7, 4000, false);
    }

    public static void tickAutoLoot(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || player.isSpectator()) return;
        if (player.tickCount % 5 != 0 || !isAutoLootEnabled(player)) return;
        ServerLevel level = player.serverLevel();
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(7.5D),
                ie -> ie.isAlive() && !ie.hasPickUpDelay() && ie.getAge() >= 15);
        for (ItemEntity ie : drops) {
            ItemStack copy = ie.getItem().copy();
            if (storeItemStackIntoStomach(player, copy, false)) {
                if (copy.isEmpty()) {
                    ie.discard();
                } else {
                    ie.setItem(copy);
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        ie.getX(), ie.getY() + 0.25D, ie.getZ(), 4, 0.15, 0.15, 0.15, 0.01);
            }
        }
    }
}
