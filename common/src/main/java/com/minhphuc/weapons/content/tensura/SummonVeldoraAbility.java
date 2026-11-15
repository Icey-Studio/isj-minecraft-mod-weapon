package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.VeldoraEntity;
import com.minhphuc.weapons.world.VeldoraWorldData;
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
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Kỹ năng Triệu Hồi & Giải Phóng Bạo Phong Long Veldora (Dạng Rồng hoặc Dạng Người).
 * - TUYỆT ĐỐI CHỈ TỒN TẠI 1 CÁ THỂ VELDORA DUY NHẤT trên toàn thế giới (gọi = dịch chuyển, không sinh bản sao).
 * - Nếu Veldora đang hiện diện ở dạng khác, PHẢI Thu Hồi trước rồi mới được triệu hồi dạng mới.
 * - Khi triệu hồi từ Không Gian Ảo / khoảng cách xa có 30% xác suất Veldora bận không tới -> Raphael thông báo ở góc màn hình.
 * - Nếu Veldora đã tử trận, dùng Giải Phóng Bạo Phong Long / Hồi Sinh sẽ tái sinh ông ta nhưng tiêu hao 70% HP & Ma Tố!
 */
public final class SummonVeldoraAbility {

    private SummonVeldoraAbility() {}

    public static void summon(ServerLevel level, ServerPlayer player, boolean humanForm) {
        releaseOrReviveSingle(level, player, humanForm, true);
    }

    public static void releaseOrReviveSingle(ServerLevel level, ServerPlayer player, boolean humanForm, boolean allowBusyRoll) {
        boolean unlocked = player.isCreative()
                || EntityDataHelper.getCustomData(player).getBoolean(AzathothStomachData.KEY_VELDORA_BEFRIENDED);
        if (!unlocked) {
            SageManager.sendCornerAlert(player, "Chưa thể triệu hồi! Hãy tìm Hang Động Phong Ấn & dùng Azathoth / Raphael giải phong ấn Veldora trước!", 0xFFF59E0B, 6000, true);
            return;
        }

        ServerLevel ow = level.getServer().overworld();
        VeldoraWorldData vd = VeldoraWorldData.get(ow);
        VeldoraEntity existing = VeldoraWorldData.findSingletonVeldora(level.getServer());

        // 1) ĐÃ CÓ VELDORA HIỆN DIỆN TRÊN THẾ GIỚI -> Chỉ 1 cá thể duy nhất!
        if (existing != null && existing.isAlive()) {
            if (existing.isSealed()) {
                SageManager.sendCornerAlert(player, "Bạo Phong Long Veldora vẫn đang bị Phong Ấn trong Hang! Hãy giải phong ấn trước khi triệu hồi!", 0xFFF59E0B, 5500, false);
                return;
            }
            if (existing.isHumanForm() != humanForm) {
                String currentForm = existing.isHumanForm() ? "Dạng Người" : "Dạng Rồng";
                String wantForm = humanForm ? "Dạng Người" : "Dạng Rồng";
                SageManager.sendCornerAlert(player, "Từ chối Triệu Hồi! Veldora duy nhất đang hiện diện ở [" + currentForm + "]. Hãy THU HỒI ông ta trước rồi mới gọi [" + wantForm + "]!", 0xFFFB7185, 6500, true);
                player.displayClientMessage(Component.literal("§c⚠ Veldora đang ở §e" + currentForm + "§c! Hãy bấm nút §6[Thu Hồi / Hồi Sinh Veldora] §ctrong Dạ Dày Azathoth trước khi gọi §b" + wantForm + "§c!"), false);
                return;
            }

            if (allowBusyRoll && rollBusyExcuse(level, player)) {
                return;
            }

            // Dịch chuyển cá thể DUY NHẤT đến bên cạnh người chơi (không bao giờ spawn thêm con thứ 2)
            Vec3 dest = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(humanForm ? 4.0D : 7.5D));
            double oldX = existing.getX(), oldY = existing.getY(), oldZ = existing.getZ();
            SpatialTeleportAbility.spawnMagicCircleParticles((ServerLevel) existing.level(), new Vec3(oldX, oldY + 0.5, oldZ), humanForm ? 3.5D : 11.0D, 60);
            if (existing.level() != level) {
                existing.teleportTo(level, dest.x, dest.y + 0.5D, dest.z, Set.of(), player.getYRot() + 180.0F, 0.0F);
            } else {
                existing.moveTo(dest.x, dest.y + 0.5D, dest.z, player.getYRot() + 180.0F, 0.0F);
            }
            existing.setDeltaMovement(Vec3.ZERO);
            existing.setAllied(true);
            existing.tame(player);
            vd.setActiveVeldoraUuid(existing.getUUID());
            playSummonStormFx(level, dest, humanForm);
            SageManager.sendCornerAlert(player, "Không Gian Chuyển Vị! Đã triệu hồi Veldora duy nhất (" + (humanForm ? "Dạng Người" : "Dạng Rồng") + ") tới chiến tuyến!", 0xFF38BDF8, 5500, false);
            return;
        }

        // 2) VELDORA ĐÃ TỬ TRẬN -> HỒI SINH VỚI GIÁ 70% HP & MA TỐ!
        if (vd.isVeldoraDead()) {
            if (!player.isCreative()) {
                float hpCost = player.getMaxHealth() * 0.70F;
                if (player.getHealth() <= 2.0F) {
                    SageManager.sendCornerAlert(player, "Sinh lực quá yếu để Giải Phóng & Hồi Sinh Bạo Phong Long (cần tối thiểu > 2 HP để chịu 70% tiêu hao)!", 0xFFEF4444, 6000, true);
                    return;
                }
                float remaining = Math.max(1.0F, player.getHealth() - hpCost);
                player.setHealth(remaining);
                int food = player.getFoodData().getFoodLevel();
                player.getFoodData().setFoodLevel(Math.max(1, food - 12));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
            vd.setVeldoraDead(false, humanForm);
            VeldoraEntity revived = spawnCanonicalVeldora(level, player, humanForm, vd);
            if (revived != null) {
                SageManager.sendCornerAlert(player, "Giải Phóng Bạo Phong Long Thành Công! Đã tiêu hao 70% HP & Ma Tố để HỒI SINH Veldora Tempest!", 0xFFF59E0B, 7000, false);
                player.displayClientMessage(Component.literal("§b§l[VELDORA] §f\"KUAHAHA!! Đa tạ bạn hiền đã rót Ma Tố hồi sinh ta từ Hư Không!\""), false);
            }
            return;
        }

        // 3) VELDORA ĐANG TRONG KHÔNG GIAN ẢO (DẠ DÀY VÔ HẠN) HOẶC CHƯA XUẤT HIỆN
        if (allowBusyRoll && rollBusyExcuse(level, player)) {
            return;
        }

        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(AzathothStomachData.KEY_STOMACH, Tag.TAG_COMPOUND);
        AzathothStomachData.removeVeldoraStomachEntries(list);
        data.put(AzathothStomachData.KEY_STOMACH, list);
        vd.setVeldoraStoredInStomach(false, humanForm);

        VeldoraEntity summoned = spawnCanonicalVeldora(level, player, humanForm, vd);
        if (summoned != null) {
            SageManager.sendCornerAlert(player, "Triệu Hồi Thành Công! Bạo Phong Long Veldora (" + (humanForm ? "Dạng Người" : "Dạng Rồng") + ") đã giáng lâm!", 0xFF38BDF8, 5500, false);
        }
    }

    public static void recallOrRevive(ServerLevel level, ServerPlayer player) {
        ServerLevel ow = level.getServer().overworld();
        VeldoraWorldData vd = VeldoraWorldData.get(ow);
        if (vd.isVeldoraDead()) {
            releaseOrReviveSingle(level, player, vd.isVeldoraHumanForm(), false);
            return;
        }

        VeldoraEntity existing = VeldoraWorldData.findSingletonVeldora(level.getServer());
        if (existing == null || !existing.isAlive()) {
            if (vd.isVeldoraStoredInStomach()) {
                SageManager.sendCornerAlert(player, "Veldora hiện đang nghỉ ngơi trong Không Gian Ảo Azathoth — có thể bấm Gọi (Rồng) hoặc Gọi (Người) ngay!", 0xFF38BDF8, 4500, false);
            } else {
                SageManager.sendCornerAlert(player, "Không thấy Veldora đang hoạt động ngoài thế giới để thu hồi!", 0xFFF59E0B, 4500, false);
            }
            return;
        }
        if (existing.isSealed()) {
            SageManager.sendCornerAlert(player, "Không thể thu hồi khi Veldora vẫn còn bị nhốt trong Lồng Giam Vô Hạn!", 0xFFF59E0B, 5000, false);
            return;
        }

        CompoundTag data = EntityDataHelper.getCustomData(player);
        ListTag list = data.getList(AzathothStomachData.KEY_STOMACH, Tag.TAG_COMPOUND);
        AzathothStomachData.removeVeldoraStomachEntries(list);
        if (list.size() < AzathothStomachData.MAX_SLOTS) {
            CompoundTag slot = new CompoundTag();
            slot.putString("Name", "§bBạo Phong Long Veldora (" + (existing.isHumanForm() ? "Dạng Người" : "Dạng Rồng") + ")");
            slot.putString("Kind", "ENTITY");
            slot.putString("SpecialKind", "VELDORA");
            slot.putBoolean("HumanForm", existing.isHumanForm());
            slot.putLong("Ep", 95_000_000L);
            slot.putString("Skill", "Long Chủng Chân Thân — Thu Hồi Chủ Động");
            list.add(slot);
            data.put(AzathothStomachData.KEY_STOMACH, list);
        }

        ServerLevel vLevel = (ServerLevel) existing.level();
        SpatialTeleportAbility.spawnMagicCircleParticles(vLevel, existing.position(), existing.isHumanForm() ? 3.5D : 11.0D, 65);
        vLevel.playSound(null, existing.getX(), existing.getY(), existing.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.8F, 0.75F);
        vd.setVeldoraStoredInStomach(true, existing.isHumanForm());
        existing.discard();

        SageManager.sendCornerAlert(player, "Đã Thu Hồi Veldora về Không Gian Ảo! Giờ bạn có thể triệu hồi dạng Rồng hoặc Dạng Người!", 0xFFA855F7, 5500, false);
    }

    private static boolean rollBusyExcuse(ServerLevel level, ServerPlayer player) {
        if (level.random.nextFloat() < 0.30F) {
            String excuse = switch (level.random.nextInt(3)) {
                case 0 -> "Kuahaha... Chờ chút đi bạn hiền, ta đang đọc dở tập Manga!";
                case 1 -> "Xin lỗi nha, Ifrit vừa thách đấu cờ Shogi với ta trong dạ dày!";
                default -> "Bạo Phong Long hiện đang bận tập chiêu mới, lát nữa gọi lại nhé!";
            };
            SageManager.sendCornerAlert(player, "Triệu Hồi Veldora Thất Bại (30% Đang Bận): \"" + excuse + "\"", 0xFFFB7185, 6500, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    private static VeldoraEntity spawnCanonicalVeldora(ServerLevel level, ServerPlayer player, boolean humanForm, VeldoraWorldData vd) {
        Vec3 spawnPos = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(humanForm ? 4.5D : 8.0D));
        playSummonStormFx(level, spawnPos, humanForm);
        VeldoraEntity veldora = ModEntities.VELDORA.get().create(level);
        if (veldora != null) {
            veldora.moveTo(spawnPos.x, spawnPos.y + 0.5D, spawnPos.z, player.getYRot() + 180.0F, 0.0F);
            veldora.setSealed(false);
            veldora.setAllied(true);
            veldora.tame(player);
            veldora.setHumanForm(humanForm);
            veldora.setCustomName(Component.literal("§b§l[BẠO PHONG LONG] §f§lVeldora Tempest"));
            veldora.setHealth(veldora.getMaxHealth());
            level.addFreshEntity(veldora);
            vd.setActiveVeldoraUuid(veldora.getUUID());
            VeldoraWorldData.findSingletonVeldora(level.getServer());
        }
        return veldora;
    }

    private static void playSummonStormFx(ServerLevel level, Vec3 spawnPos, boolean humanForm) {
        SpatialTeleportAbility.spawnMagicCircleParticles(level, spawnPos, humanForm ? 4.5D : 12.0D, 90);
        for (int h = 0; h < 20; h++) {
            double ang = h * 0.65D;
            double r = 1.4D + h * 0.34D;
            level.sendParticles(ParticleTypes.CLOUD, spawnPos.x + Math.cos(ang) * r, spawnPos.y + h * 0.5D,
                    spawnPos.z + Math.sin(ang) * r, 4, 0.25, 0.25, 0.25, 0.04);
        }
        level.sendParticles(ParticleTypes.FLASH, spawnPos.x, spawnPos.y + 2.0D, spawnPos.z, 2, 0, 0, 0, 0);
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 0.9F);
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 1.0F);
    }
}
