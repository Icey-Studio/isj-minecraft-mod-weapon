package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.network.ClientboundSyncPossessionPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.minhphuc.weapons.init.ModItems;

public class PrimordialPossessionManager {

    public static final String NBT_IS_POSSESSING = "IsPossessingDemon";
    public static final String NBT_DEMON_TYPE = "PossessedDemonType";
    public static final String NBT_DEMON_UUID = "PossessedDemonUUID";
    public static final String NBT_DEMON_NAME = "PossessedDemonName";
    public static final String NBT_HAS_BODY = "PossessedHasBody";
    public static final String NBT_IS_NAMED = "PossessedIsNamed";
    public static final String NBT_EVOLUTION_TIER = "PossessedEvolutionTier";
    public static final String NBT_IS_WINGED = "PossessedIsWinged";
    public static final String NBT_IS_DEMON_LORD = "PossessedIsDemonLord";
    public static final String NBT_SKILLS = "PossessedSkills";
    public static final String NBT_SELECTED_SKILL = "PossessedSelectedSkill";
    public static final String NBT_PRE_GAMEMODE = "PrePossessGameMode";
    public static final String NBT_PRE_HP = "PrePossessHp";
    public static final String NBT_DEMON_CURRENT_HP = "PossessedCurrentHp";
    public static final String NBT_DEMON_MAX_HP = "PossessedMaxHp";
    public static final String NBT_DEMON_ATK = "PossessedAtk";

    public static boolean isPossessing(Player player) {
        if (player == null) return false;
        return EntityDataHelper.getCustomData(player).getBoolean(NBT_IS_POSSESSING);
    }

    public static DemonType getPossessedType(Player player) {
        if (!isPossessing(player)) return null;
        String typeName = EntityDataHelper.getCustomData(player).getString(NBT_DEMON_TYPE);
        try {
            return DemonType.valueOf(typeName);
        } catch (Exception e) {
            return DemonType.NOIR;
        }
    }

    public static boolean hasPhysicalBody(Player player) {
        if (!isPossessing(player)) return false;
        return EntityDataHelper.getCustomData(player).getBoolean(NBT_HAS_BODY);
    }

    public static boolean isNamed(Player player) {
        if (!isPossessing(player)) return false;
        return EntityDataHelper.getCustomData(player).getBoolean(NBT_IS_NAMED);
    }

    public static String getCustomDemonName(Player player) {
        if (!isPossessing(player)) return "";
        return EntityDataHelper.getCustomData(player).getString(NBT_DEMON_NAME);
    }

    public static int getEvolutionTier(Player player) {
        if (!isPossessing(player)) return 0;
        return EntityDataHelper.getCustomData(player).getInt(NBT_EVOLUTION_TIER);
    }

    public static boolean isWinged(Player player) {
        if (!isPossessing(player)) return false;
        return EntityDataHelper.getCustomData(player).getBoolean(NBT_IS_WINGED);
    }

    public static boolean isDemonLord(Player player) {
        if (!isPossessing(player)) {
            return EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord")
                    || EntityDataHelper.getCustomData(player).getBoolean("TensuraAwakenedDemonLord");
        }
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        return tag.getBoolean(NBT_IS_DEMON_LORD)
                || tag.getBoolean("TensuraTrueDemonLord")
                || tag.getBoolean("TensuraAwakenedDemonLord");
    }

    public static int getSelectedSkillIndex(Player player) {
        if (!isPossessing(player)) return 0;
        return EntityDataHelper.getCustomData(player).getInt(NBT_SELECTED_SKILL);
    }

    /**
     * Bắt đầu quá trình Nhập Xác Ác Ma Thủy Tổ (gọi khi Người chơi Khán Giả click vào Ác Ma đã thuần phục của mình)
     */
    public static void startPossession(ServerPlayer player, PrimordialDemonEntity demon) {
        if (player == null || demon == null || !demon.isAlive()) return;
        ServerLevel level = player.serverLevel();

        DemonType type = demon.getDemonType();
        boolean hasBody = demon.hasPhysicalBody();
        boolean isNamed = demon.isNamed();
        String customName = demon.getCustomDemonName();
        int tier = demon.getEvolutionTier();
        boolean isWinged = demon.isWinged();
        boolean isDemonLord = demon.getTags().contains("TensuraDemonLordMob");
        float currentHp = demon.getHealth();
        float maxHp = demon.getMaxHealth();
        float atk = (float) demon.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // Lưu danh sách kỹ năng ngẫu nhiên đã thức tỉnh
        StringBuilder sbSkills = new StringBuilder();
        for (PrimordialSkillPool.SkillEntry sk : demon.getRandomSkills()) {
            if (sbSkills.length() > 0) sbSkills.append(",");
            sbSkills.append(sk.id());
        }

        CompoundTag tag = EntityDataHelper.getCustomData(player);
        tag.putBoolean(NBT_IS_POSSESSING, true);
        tag.putString(NBT_DEMON_TYPE, type.name());
        tag.putString(NBT_DEMON_UUID, demon.getStringUUID());
        tag.putString(NBT_DEMON_NAME, customName != null ? customName : "");
        tag.putBoolean(NBT_HAS_BODY, hasBody);
        tag.putBoolean(NBT_IS_NAMED, isNamed);
        tag.putInt(NBT_EVOLUTION_TIER, tier);
        tag.putBoolean(NBT_IS_WINGED, isWinged);
        tag.putBoolean(NBT_IS_DEMON_LORD, isDemonLord);
        tag.putString(NBT_SKILLS, sbSkills.toString());
        tag.putInt(NBT_SELECTED_SKILL, 0);
        tag.putFloat(NBT_DEMON_CURRENT_HP, currentHp);
        tag.putFloat(NBT_DEMON_MAX_HP, maxHp);
        tag.putFloat(NBT_DEMON_ATK, atk);

        // Lưu chế độ chơi & lượng máu gốc của người chơi
        tag.putString(NBT_PRE_GAMEMODE, player.gameMode.getGameModeForPlayer().name());
        tag.putFloat(NBT_PRE_HP, player.getHealth());

        // Đặt người chơi tại đúng vị trí của Ác Ma và chuyển sang Survival (để nhận sát thương & điều khiển chuẩn)
        player.teleportTo(demon.getX(), demon.getY(), demon.getZ());
        player.setYRot(demon.getYRot());
        player.setXRot(demon.getXRot());
        player.setGameMode(GameType.SURVIVAL);

        // Ban khả năng bay lượn tự do của Ác Ma Thủy Tổ
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        // Gán máu & lực chiến theo thân xác Ác Ma
        var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(maxHp);
        }
        var atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.setBaseValue(Math.max(atk, 48.0D));
        }
        player.setHealth(Math.min(currentHp, maxHp));

        // Tạm thời thu hồi entity Ác Ma vào người chơi để chống lag (0% AI tải)
        demon.discard();

        // Đồng bộ dữ liệu Nhập Xác xuống Client
        syncToClient(player);

        // Hiệu ứng âm thanh & Title
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.9F);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.2D, player.getZ(), 3, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 45, 0.5D, 0.8D, 0.5D, 0.15D);

        String effName = (!customName.isEmpty()) ? customName : type.getColorName();
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l★ NHẬP XÁC: " + effName.toUpperCase() + " ★")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§aToàn quyền dùng thân xác & kỹ năng! (Bấm [Shift] để tách xác)")));

        String firstSkill = getSkillDisplayName(player, 0);
        player.displayClientMessage(
                Component.literal("§5§l[NHẬP XÁC: " + effName + "] §fKỹ năng: §e§l" + firstSkill + " §7(Phím Z: Đổi skill | Chuột Phải: Tung chiêu | Shift: Tách xác)"),
                true
        );
    }

    /**
     * Tách khỏi thân xác Ác Ma (khi bấm Shift hoặc khi Ác Ma tử trận)
     */
    public static void stopPossession(ServerPlayer player, boolean isDemonDeath) {
        if (!isPossessing(player)) return;
        ServerLevel level = player.serverLevel();
        CompoundTag tag = EntityDataHelper.getCustomData(player);

        DemonType type = getPossessedType(player);
        boolean hasBody = tag.getBoolean(NBT_HAS_BODY);
        boolean isNamed = tag.getBoolean(NBT_IS_NAMED);
        String customName = tag.getString(NBT_DEMON_NAME);
        int tier = tag.getInt(NBT_EVOLUTION_TIER);
        boolean isWinged = tag.getBoolean(NBT_IS_WINGED);
        boolean isDemonLord = tag.getBoolean(NBT_IS_DEMON_LORD);
        String savedSkills = tag.getString(NBT_SKILLS);
        float remainingHp = player.getHealth();

        String preGameModeStr = tag.getString(NBT_PRE_GAMEMODE);
        GameType preGameMode = GameType.SURVIVAL;
        try {
            preGameMode = GameType.valueOf(preGameModeStr);
        } catch (Exception ignored) {}

        // Nếu chế độ chơi trước đó là Spectator (do người chơi đổi sang để click nhập xác)
        // thì khi tách xác phải khôi phục về SURVIVAL để chơi bình thường, tránh kẹt trong Spectator!
        if (preGameMode == GameType.SPECTATOR) {
            preGameMode = GameType.SURVIVAL;
        }

        float preHp = tag.getFloat(NBT_PRE_HP);
        if (preHp <= 0) preHp = 20.0F;

        // Xóa cờ nhập xác
        tag.putBoolean(NBT_IS_POSSESSING, false);
        tag.remove(NBT_DEMON_TYPE);
        tag.remove(NBT_DEMON_UUID);
        tag.remove(NBT_DEMON_NAME);
        tag.remove(NBT_HAS_BODY);
        tag.remove(NBT_IS_NAMED);
        tag.remove(NBT_EVOLUTION_TIER);
        tag.remove(NBT_IS_WINGED);
        tag.remove(NBT_IS_DEMON_LORD);
        tag.remove(NBT_SKILLS);
        tag.remove(NBT_SELECTED_SKILL);
        tag.remove(NBT_PRE_GAMEMODE);
        tag.remove(NBT_PRE_HP);
        tag.remove(NBT_DEMON_CURRENT_HP);
        tag.remove(NBT_DEMON_MAX_HP);
        tag.remove(NBT_DEMON_ATK);

        // Khôi phục lượng máu & sát thương gốc của Player (đặc biệt Chân Ma Vương có 80 HP vĩnh viễn)
        boolean isLord = PrimordialPlayerDataHelper.isDemonLord(player);
        double originalMaxHp = isLord ? 80.0D : 20.0D;
        var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(originalMaxHp);
        }
        var atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.setBaseValue(isLord ? 8.0D : 1.0D);
        }
        player.setHealth(Math.min(preHp > 0 ? preHp : (float) originalMaxHp, player.getMaxHealth()));

        // Trả lại gamemode gốc
        player.setGameMode(preGameMode);
        boolean canFly = PrimordialPlayerDataHelper.isPrimordial(player) || isLord;
        if (preGameMode != GameType.CREATIVE) {
            player.getAbilities().mayfly = canFly;
            player.getAbilities().flying = false;
            if (canFly) {
                player.getAbilities().setFlyingSpeed(0.08F);
            }
            player.onUpdateAbilities();
        }

        // Đồng bộ giải trừ nhập xác xuống Client
        syncToClient(player);

        if (!isDemonDeath) {
            // Tách xác chủ động (bấm Shift): Spawns lại Ác Ma tại vị trí người chơi
            PrimordialDemonEntity newDemon = ModEntities.PRIMORDIAL_DEMON.get().create(level);
            if (newDemon != null) {
                newDemon.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                newDemon.setDemonType(type);
                newDemon.tame(player);
                newDemon.setPhysicalBody(hasBody);
                newDemon.setNamed(isNamed);
                newDemon.setCustomDemonName(customName);
                newDemon.setEvolutionTier(tier);
                newDemon.setWinged(isWinged);
                if (isNamed) newDemon.addTag("TensuraNamedMob");
                if (isDemonLord) {
                    newDemon.addTag("TensuraDemonLordMob");
                    newDemon.addTag("TensuraDemonLordCalm");
                }
                newDemon.updateAttributesForType(type);
                newDemon.updateDemonNametag();
                newDemon.setCustomNameVisible(true);

                if (!savedSkills.isEmpty()) {
                    newDemon.setRandomSkillIds(Arrays.asList(savedSkills.split(",")));
                }
                newDemon.ensureRandomSkills();
                newDemon.setHealth(Math.max(1.0F, remainingHp));
                newDemon.setOrderedToSit(false);

                level.addFreshEntity(newDemon);

                // Cập nhật lại DemonUUID vào cuộn khế ước trong túi đồ
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack st = player.getInventory().getItem(i);
                    if (st.getItem() instanceof PrimordialPactItem && PrimordialPactItem.getDemonType(st) == type) {
                        com.minhphuc.weapons.data.ItemStackDataHelper.putString(st, "DemonUUID", newDemon.getStringUUID());
                        com.minhphuc.weapons.data.ItemStackDataHelper.putFloat(st, "CurrentHp", remainingHp);
                        break;
                    }
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.8F, 1.1F);
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);
            player.displayClientMessage(Component.literal("§e✦ ĐÃ TÁCH KHỎI THÂN XÁC ÁC MA! ✦"), true);
        } else {
            // Ác ma tử trận: Người chơi thoát hiểm an toàn, cuộn khế ước bị vỡ/thu hồi
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0F, 0.7F);
            level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1.0D, player.getZ(), 35, 0.5D, 0.6D, 0.5D, 0.15D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.05D);

            // Xóa khế ước tương ứng của ác ma tử trận
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (st.getItem() instanceof PrimordialPactItem && PrimordialPactItem.getDemonType(st) == type) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    break;
                }
            }

            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l✦ THÂN XÁC ÁC MA TỬ TRẬN ✦")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cLinh hồn bạn đã quay trở về bản thể bình thường!")));
            player.sendSystemMessage(Component.literal("§4§l✦ [KHẾ ƯỚC TAN VỠ] ✦\n§cThân xác Ác Ma Thủy Tổ §6" + type.getColorName() + " §cđã bị hủy diệt hoàn toàn trên chiến trường!\n§7Linh hồn bạn đã thoát hiểm an toàn, cuộn khế ước đã hóa thành tro tàn!"));
        }
    }

    /**
     * Danh sách toàn bộ các ID kỹ năng mà thân xác Ác Ma này sở hữu
     */
    public static List<Integer> getAvailableSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        if (!isPossessing(player)) return list;

        DemonType type = getPossessedType(player);
        boolean hasBody = hasPhysicalBody(player);
        boolean isNamed = isNamed(player);

        // 1. Kỹ năng bản năng & Độc bản (0..8)
        for (int i = 0; i <= 8; i++) {
            list.add(i);
        }

        // 2. Kỹ năng ngẫu nhiên thức tỉnh (20..29)
        String skillsStr = EntityDataHelper.getCustomData(player).getString(NBT_SKILLS);
        if (!skillsStr.isEmpty()) {
            String[] rSkills = skillsStr.split(",");
            for (int i = 0; i < rSkills.length; i++) {
                if (!rSkills[i].trim().isEmpty()) {
                    list.add(20 + i); // 20+i đại diện cho random skill thứ i
                }
            }
        }

        // 3. Đại ma pháp khế ước (100..112) - Mở khóa toàn bộ không cần ràng buộc
        for (int i = 100; i <= 112; i++) {
            list.add(i);
        }

        return list;
    }

    public static String getSkillDisplayName(Player player, int skillId) {
        DemonType type = getPossessedType(player);
        if (type == null) type = DemonType.NOIR;

        if (skillId >= 0 && skillId <= 6) {
            return PrimordialPlayerDataHelper.getSkillName(type, skillId);
        }

        return switch (skillId) {
            case 7 -> "Tam Trọng Thánh Giới: Linh Tử Băng Hoại (Disintegration)";
            case 8 -> "Bành Trướng Lãnh Địa (Domain Expansion)";
            case 100 -> "Kháng Ma Kết Giới (Anti-Magic Barrier)";
            case 101 -> "Cú Bắn Granit (Granite Blast)";
            case 102 -> "Zone Track: Đại Pháo Bạch Kim";
            case 103 -> "Thị Thực: Thị Nhục Mạch Động";
            case 104 -> "Phép Màu Của Thần Linh (Holy Purification)";
            case 105 -> "Đa Trùng Kết Giới (Multilayer Barrier)";
            case 106 -> "Lồng Giam Vô Hạn (Infinite Prison)";
            case 107 -> "Hắc Viêm Ngục (Hellflare)";
            case 108 -> "Hắc Viêm Hỏa Cầu (Dark Flame Shot)";
            case 109 -> "Lời Chúc Tử Vong (Death Blessing)";
            case 110 -> "Siêu Tân Tinh (Supernova)";
            case 111 -> "Dị Giới Thần Môn (Sanctuary Gate)";
            case 112 -> "Long Tinh Bộc Viêm Bá (Dragon Nova)";
            default -> {
                if (skillId >= 20 && skillId < 30) {
                    int rIdx = skillId - 20;
                    String skillsStr = EntityDataHelper.getCustomData(player).getString(NBT_SKILLS);
                    String[] rSkills = skillsStr.split(",");
                    if (rIdx < rSkills.length) {
                        var entry = PrimordialSkillPool.getSkillById(rSkills[rIdx].trim());
                        if (entry != null) yield entry.displayNameVi();
                    }
                }
                yield "Kỹ Năng Thủy Tổ";
            }
        };
    }

    /**
     * Bấm phím Z: Chuyển đổi skill đang chọn
     */
    public static void cycleSkill(ServerPlayer player) {
        if (!isPossessing(player)) return;
        List<Integer> available = getAvailableSkills(player);
        if (available.isEmpty()) return;

        int currentSkill = getSelectedSkillIndex(player);
        int currentPos = available.indexOf(currentSkill);
        int nextPos = (currentPos + 1) % available.size();
        int nextSkill = available.get(nextPos);

        EntityDataHelper.getCustomData(player).putInt(NBT_SELECTED_SKILL, nextSkill);
        syncToClient(player);

        String name = getSkillDisplayName(player, nextSkill);
        String demonName = getPossessedType(player) != null ? getPossessedType(player).getColorName() : "THỦY TỔ";
        player.displayClientMessage(
                Component.literal("§5§l[" + demonName.toUpperCase() + "] §fKỹ năng [" + (nextPos + 1) + "/" + available.size() + "]: §e§l" + name),
                true
        );

        float pitch = 1.0F + ((nextPos % 10) * 0.12F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, pitch);
    }

    /**
     * Chuột Phải: Thi triển skill đang chọn
     */
    public static void castSkill(ServerPlayer player) {
        if (!isPossessing(player)) return;
        ServerLevel level = player.serverLevel();
        int skill = getSelectedSkillIndex(player);

        // Kỹ năng Bản năng (0..6)
        if (skill >= 0 && skill <= 6) {
            PrimordialSkillDispatcher.castSkill(level, player, skill);
            return;
        }

        switch (skill) {
            case 7 -> com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility.cast(level, player, ItemStack.EMPTY);
            case 8 -> castPossessedDomainExpansion(level, player);
            case 100 -> AntiMagicBarrierManager.castPlayerBarrier(level, player);
            case 101 -> HorizontalHolyBeamAbility.cast(level, player);
            case 102 -> ZoneTrackAbility.cast(level, player);
            case 103 -> com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.cast(level, player);
            case 104 -> com.minhphuc.weapons.content.divine.PurificationPillarAbility.cast(level, player, ItemStack.EMPTY);
            case 105 -> com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.cast(level, player);
            case 106 -> com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.cast(level, player, 3);
            case 107 -> com.minhphuc.weapons.content.tensura.HellflareAbility.cast(level, player);
            case 108 -> com.minhphuc.weapons.content.tensura.DarkFlameShotAbility.cast(level, player);
            case 109 -> com.minhphuc.weapons.content.tensura.NihilityCollapseAbility.cast(level, player);
            case 110 -> com.minhphuc.weapons.content.tensura.SupernovaAbility.cast(level, player);
            case 111 -> com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility.cast(level, player);
            case 112 -> com.minhphuc.weapons.content.tensura.DragonNovaAbility.cast(level, player);
            default -> {
                // Kỹ năng ngẫu nhiên thức tỉnh (20..29)
                if (skill >= 20 && skill < 30) {
                    int rIdx = skill - 20;
                    String skillsStr = EntityDataHelper.getCustomData(player).getString(NBT_SKILLS);
                    String[] rSkills = skillsStr.split(",");
                    if (rIdx < rSkills.length) {
                        var entry = PrimordialSkillPool.getSkillById(rSkills[rIdx].trim());
                        if (entry != null) {
                            executeAwakenedRandomSkill(level, player, entry);
                        }
                    }
                }
            }
        }
    }

    private static void executeAwakenedRandomSkill(ServerLevel level, ServerPlayer player, PrimordialSkillPool.SkillEntry entry) {
        if (entry == null) return;
        player.displayClientMessage(Component.literal("§6§l[THỨC TỈNH] §eThi triển: " + entry.displayNameVi() + "!"), true);
        switch (entry.id()) {
            case PrimordialSkillPool.SKILL_BEELZEBUTH -> BeelzebuthAbility.executeBeelzebuth(level, player);
            case PrimordialSkillPool.SKILL_INFINITY_SNAP -> {
                net.minecraft.world.phys.Vec3 targetPos = player.position().add(player.getLookAngle().scale(8.0D));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.5F, 1.8F);
                level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.8F);
                level.sendParticles(ParticleTypes.FLASH, targetPos.x, targetPos.y + 1.0D, targetPos.z, 3, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetPos.x, targetPos.y + 1.0D, targetPos.z, 3, 1.0, 1.0, 1.0, 0);
                for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(targetPos, targetPos).inflate(10.0D), ent -> ent != player && ent.isAlive())) {
                    e.hurt(player.damageSources().magic(), 800.0F);
                }
            }
            case PrimordialSkillPool.SKILL_POWER_BEAM -> HorizontalHolyBeamAbility.cast(level, player);
            case PrimordialSkillPool.SKILL_SPACE_TELEPORT -> {
                net.minecraft.world.phys.Vec3 dest = player.position().add(player.getLookAngle().scale(12.0D));
                player.teleportTo(dest.x, dest.y, dest.z);
                level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.2F);
                level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0D, dest.z, 50, 0.6, 0.8, 0.6, 0.1);
                for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(5.0D), ent -> ent != player && ent.isAlive())) {
                    e.hurt(player.damageSources().magic(), 450.0F);
                }
            }
            case PrimordialSkillPool.SKILL_REALITY_ILLUSION -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 2.0F, 1.0F);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 1.5, 0.5, 1.5, 0.1);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 160, 0, false, false));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 3, false, false));
            }
            case PrimordialSkillPool.SKILL_TIME_FREEZE -> com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility.castReverseTimeSlash(level, player);
            case PrimordialSkillPool.SKILL_SOUL_DRAIN -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.0F, 1.2F);
                level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 50, 3.0, 1.2, 3.0, 0.1);
                float totalHealed = 0;
                for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(10.0D), ent -> ent != player && ent.isAlive())) {
                    e.hurt(player.damageSources().magic(), 350.0F);
                    totalHealed += 50.0F;
                }
                if (totalHealed > 0) player.heal(totalHealed);
            }
            case PrimordialSkillPool.SKILL_MIND_BLAST -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 1.5F);
                level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0D, player.getZ(), 3, 1.0, 0.5, 1.0, 0);
                for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(12.0D), ent -> ent != player && ent.isAlive())) {
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 160, 0));
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 160, 1));
                    e.hurt(player.damageSources().magic(), 400.0F);
                }
            }
            case PrimordialSkillPool.SKILL_TAISUI_STARS -> com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.cast(level, player);
            case PrimordialSkillPool.SKILL_ALKAID_CHAOS -> com.minhphuc.weapons.content.darkgathering.AlkaidAbility.cast(level, player);
            case PrimordialSkillPool.SKILL_JACOB_LADDER -> com.minhphuc.weapons.content.divine.JacobsLadderAbility.cast(level, player, ItemStack.EMPTY);
            case PrimordialSkillPool.SKILL_DISINTEGRATION -> com.minhphuc.weapons.content.divine.SanctuaryDisintegrationAbility.cast(level, player, ItemStack.EMPTY);
            case PrimordialSkillPool.SKILL_HEAVENLY_JUDGMENT -> PentagramCelestialPillarAbility.cast(level, player);
            case PrimordialSkillPool.SKILL_BLACK_FLASH -> com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility.castReverseVelocityIai(level, player);
            case PrimordialSkillPool.SKILL_DRAGON_NOVA -> DragonNovaAbility.cast(level, player);
            case PrimordialSkillPool.SKILL_SEER_FLESH_PULSE -> com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.cast(level, player);
            default -> PrimordialSkillDispatcher.castSkill(level, player, 6);
        }
    }

    /**
     * Đồng bộ dữ liệu Nhập Xác của player đến toàn bộ client xung quanh và chính player
     */
    public static void syncToClient(ServerPlayer player) {
        if (player == null) return;
        boolean possessing = isPossessing(player);
        DemonType type = getPossessedType(player);
        int typeOrd = type != null ? type.ordinal() : 0;
        boolean hasBody = hasPhysicalBody(player);
        boolean isNamed = isNamed(player);
        String customName = getCustomDemonName(player);
        int tier = getEvolutionTier(player);
        boolean isWinged = isWinged(player);
        float hp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float atk = EntityDataHelper.getCustomData(player).getFloat(NBT_DEMON_ATK);
        int selectedSkill = getSelectedSkillIndex(player);
        String skillsStr = EntityDataHelper.getCustomData(player).getString(NBT_SKILLS);

        ClientboundSyncPossessionPacket packet = new ClientboundSyncPossessionPacket(
                player.getUUID(), possessing, typeOrd, hasBody, isNamed, customName, tier, isWinged, hp, maxHp, atk, selectedSkill, skillsStr
        );

        // Gửi cho chính người chơi
        ModMessages.sendToPlayer(packet, player);
        // Gửi cho người chơi xung quanh để họ cũng thấy đúng Skin Ác Ma của player này
        ModMessages.sendToPlayers(player.serverLevel().players(), packet);
    }

    // =========================================================================
    // BÀNH TRƯỚNG LÃNH ĐỊA KHI NHẬP XÁC THỦY TỔ (DOMAIN EXPANSION)
    // =========================================================================
    public static class ActivePossessedDomain {
        public final ServerLevel level;
        public final java.util.UUID casterUuid;
        public final DemonType type;
        public final net.minecraft.world.phys.Vec3 center;
        public int ticksRemaining;
        public final List<net.minecraft.core.BlockPos> barrierBlocks = new ArrayList<>();
        public net.minecraft.world.entity.Display.ItemDisplay circleDisplay;
        public float angle = 0.0F;

        public ActivePossessedDomain(ServerLevel level, java.util.UUID casterUuid, DemonType type, net.minecraft.world.phys.Vec3 center, int ticks) {
            this.level = level;
            this.casterUuid = casterUuid;
            this.type = type;
            this.center = center;
            this.ticksRemaining = ticks;
        }
    }

    private static final List<ActivePossessedDomain> ACTIVE_POSSESSED_DOMAINS = new ArrayList<>();

    public static void castPossessedDomainExpansion(ServerLevel level, ServerPlayer player) {
        DemonType type = getPossessedType(player);
        if (type == null) type = PrimordialPlayerDataHelper.getPrimordialType(player);
        if (type == null) type = DemonType.NOIR;

        // Dọn lãnh địa cũ của người chơi này nếu đang bật
        Iterator<ActivePossessedDomain> it = ACTIVE_POSSESSED_DOMAINS.iterator();
        while (it.hasNext()) {
            ActivePossessedDomain old = it.next();
            if (old.casterUuid.equals(player.getUUID())) {
                cleanPossessedDomain(old);
                it.remove();
            }
        }

        net.minecraft.world.phys.Vec3 center = player.position();
        ActivePossessedDomain domain = new ActivePossessedDomain(level, player.getUUID(), type, center, 300);

        net.minecraft.world.level.block.Block barrierBlock = type.getDomainBarrierBlock().get();
        int radius = 16;
        net.minecraft.core.BlockPos cPos = net.minecraft.core.BlockPos.containing(center);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 16; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + (y * 0.9) * (y * 0.9) + z * z);
                    if (dist >= radius - 0.9 && dist <= radius + 0.9) {
                        net.minecraft.core.BlockPos bPos = cPos.offset(x, y, z);
                        if (level.getBlockState(bPos).isAir() || level.getBlockState(bPos).liquid()) {
                            level.setBlock(bPos, barrierBlock.defaultBlockState(), 50);
                            domain.barrierBlocks.add(bPos);
                        }
                    }
                }
            }
        }

        // Vòng ma pháp 16m dưới chân Lãnh Địa
        net.minecraft.world.entity.Display.ItemDisplay display = net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + 0.05D, center.z, 0.0F, 0.0F);
            com.minhphuc.weapons.mixin.ItemDisplayAccessor itemAcc = (com.minhphuc.weapons.mixin.ItemDisplayAccessor) display;
            com.minhphuc.weapons.mixin.DisplayAccessor dispAcc = (com.minhphuc.weapons.mixin.DisplayAccessor) display;
            ItemStack circleStack = (type == DemonType.JAUNE)
                    ? new ItemStack(ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get())
                    : new ItemStack(type.getMagicCircleItem().get());
            itemAcc.weapons$setItemStack(circleStack);
            itemAcc.weapons$setItemTransform(net.minecraft.world.item.ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(net.minecraft.world.entity.Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            display.addTag("DemonMagicCircle");
            dispAcc.weapons$setGlowColorOverride(type.getGlowColor());
            dispAcc.weapons$setViewRange(6.0F);
            org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateX((float) Math.toRadians(90.0F));
            dispAcc.weapons$setTransformation(new com.mojang.math.Transformation(
                    new org.joml.Vector3f(0.0F, 0.0F, 0.0F),
                    rot,
                    new org.joml.Vector3f(16.0F, 16.0F, 0.01F),
                    null
            ));
            level.addFreshEntity(display);
            domain.circleDisplay = display;
            ResidualMagicCircleManager.registerCircle(display);
        }

        ACTIVE_POSSESSED_DOMAINS.add(domain);

        String domainName = switch (type) {
            case NOIR -> "Thế Giới Cám Dỗ (Temptation World)";
            case ROUGE -> "Kiêu Ngạo Chi Ngục (Prideful Inferno)";
            case BLANC -> "Tử Khúc Băng Tuyết (Whiteout Requiem)";
            case JAUNE -> "Vực Thẳm Hạt Nhân (Abaddon Horizon)";
            case VIOLET -> "Tử Độc Huyết Giới (Poisonous Nightmare)";
            case BLEU -> "Vĩnh Cửu Hàn Băng (Eternal Glacier)";
            case VERT -> "Thiên Phạt Phong Bạo (Tempest Sanctuary)";
        };

        level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.2F);

        for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(center) <= 64.0D * 64.0D)) {
            sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§8§l【 LÃNH ĐỊA 】 §c§l" + domainName.toUpperCase())));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + type.getColorName() + " §7đã giải phóng Không Gian Tuyệt Đối! (+50% Sức Mạnh)")));
            sp.displayClientMessage(Component.literal("§5§l[BÀNH TRƯỚNG LÃNH ĐỊA] §dTriển khai kết giới ma giới tối thượng: §e§l" + domainName + "!"), true);
        }
    }

    private static void cleanPossessedDomain(ActivePossessedDomain domain) {
        if (domain.circleDisplay != null && domain.circleDisplay.isAlive()) {
            domain.circleDisplay.discard();
        }
        for (net.minecraft.core.BlockPos p : domain.barrierBlocks) {
            net.minecraft.world.level.block.Block b = domain.level.getBlockState(p).getBlock();
            if (b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_NOIR.get() ||
                b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_ROUGE.get() ||
                b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_BLANC.get() ||
                b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_JAUNE.get() ||
                b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_VIOLET.get() ||
                b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_BLEU.get() ||
                b == com.minhphuc.weapons.init.ModBlocks.DOMAIN_BARRIER_VERT.get()) {
                domain.level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 50);
            }
        }
        domain.barrierBlocks.clear();
        domain.level.playSound(null, domain.center.x, domain.center.y, domain.center.z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.5F, 1.2F);
    }

    public static void tickDomains(ServerLevel level) {
        if (ACTIVE_POSSESSED_DOMAINS.isEmpty()) return;
        Iterator<ActivePossessedDomain> it = ACTIVE_POSSESSED_DOMAINS.iterator();
        while (it.hasNext()) {
            ActivePossessedDomain d = it.next();
            if (d.level != level) continue;

            d.ticksRemaining--;
            if (d.ticksRemaining <= 0) {
                cleanPossessedDomain(d);
                it.remove();
                continue;
            }

            if (d.circleDisplay != null && d.circleDisplay.isAlive() && d.ticksRemaining % 2 == 0) {
                d.angle += 3.0F;
                com.minhphuc.weapons.mixin.DisplayAccessor dispAcc = (com.minhphuc.weapons.mixin.DisplayAccessor) d.circleDisplay;
                org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateX((float) Math.toRadians(90.0F)).rotateZ((float) Math.toRadians(d.angle));
                dispAcc.weapons$setTransformation(new com.mojang.math.Transformation(
                        new org.joml.Vector3f(0.0F, 0.0F, 0.0F),
                        rot,
                        new org.joml.Vector3f(16.0F, 16.0F, 0.01F),
                        null
                ));
            }

            if (d.ticksRemaining % 4 == 0) {
                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(d.casterUuid);
                if (caster != null && caster.level() == level) {
                    caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 1));
                    caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 1));
                    caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 40, 2));
                }

                double radius = 16.0D;
                for (int a = 0; a < 360; a += 24) {
                    double rad = Math.toRadians(a);
                    double px = d.center.x + Math.cos(rad) * radius;
                    double pz = d.center.z + Math.sin(rad) * radius;
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, d.center.y + 1.0D, pz, 1, 0.2D, 0.8D, 0.2D, 0.01D);
                }

                List<net.minecraft.world.entity.LivingEntity> entities = level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        new net.minecraft.world.phys.AABB(d.center.x - 17, d.center.y - 6, d.center.z - 17, d.center.x + 17, d.center.y + 15, d.center.z + 17)
                );
                for (net.minecraft.world.entity.LivingEntity e : entities) {
                    if (e.getUUID().equals(d.casterUuid)) continue;
                    if (PrimordialDemonEntity.isContractedAllyOfPlayer(d.casterUuid, e)) continue;

                    double dist = e.distanceToSqr(d.center);
                    if (dist >= 14.0D * 14.0D && dist <= 19.0D * 19.0D) {
                        net.minecraft.world.phys.Vec3 pull = d.center.subtract(e.position()).normalize().scale(0.85D);
                        e.setDeltaMovement(pull.x, 0.2D, pull.z);
                        e.hasImpulse = true;
                    }
                    if (dist <= radius * radius) {
                        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 40, 0));
                        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                        if (d.ticksRemaining % 20 == 0) {
                            e.hurt(caster != null ? level.damageSources().playerAttack(caster) : level.damageSources().magic(), 65.0F);
                        }
                    }
                }
            }
        }
    }
}
