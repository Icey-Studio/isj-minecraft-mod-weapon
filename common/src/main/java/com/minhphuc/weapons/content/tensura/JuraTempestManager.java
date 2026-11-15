package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.ElementalSpiritEntity;
import com.minhphuc.weapons.entity.tensura.ElementalType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ClientboundOpenTempestCitadelPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Quản lý vận hành & cư dân bên trong không gian riêng [THÀNH TRÌ JURA TEMPEST]:
 * - Tự động triệu hồi Cư Dân Tempest, Hội Đồng Thủy Tổ (Noir/Blanc/Jaune/Violet) & Tinh Linh Nguyên Tố.
 * - Mời đồng minh đã thuần phục từ thế giới ngoài về Thành Trì, Ban Danh Xưng tăng Ma Tố/EP vĩnh viễn.
 * - Tự động sản xuất vật tư cao cấp gửi thẳng vào Dạ Dày Vô Hạn Azathoth.
 * - Vận hành Đấu Trường Tối Thượng: Bia Thử Sát Thương Bất Tử (tính DMG + Combo) & Đài Triệu Hồi 8 Boss Ảo Ảnh.
 */
public final class JuraTempestManager {

    public static final String TAG_CITADEL_RESIDENT = "TempestCitadelResident";
    public static final String TAG_COLOSSEUM_BOSS = "TempestColosseumBoss";
    public static final String TAG_TRAINING_DUMMY = "TempestTrainingDummy";
    public static final String KEY_AUTO_PRODUCE = "TempestAutoProduce";
    public static final String KEY_BLESSED_COUNT = "TempestBlessedCount";
    public static final String KEY_CITADEL_ESTABLISHED = "TempestCitadelEstablished";

    private JuraTempestManager() {}

    public static long getBonusMaxMp(ServerPlayer player) {
        if (player == null) return 0L;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        if (!data.getBoolean(KEY_CITADEL_ESTABLISHED)) return 0L;
        int blessed = Math.max(0, data.getInt(KEY_BLESSED_COUNT));
        return 2_000_000L + blessed * 350_000L;
    }

    public static long getBonusEp(ServerPlayer player) {
        if (player == null) return 0L;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        if (!data.getBoolean(KEY_CITADEL_ESTABLISHED)) return 0L;
        int blessed = Math.max(0, data.getInt(KEY_BLESSED_COUNT));
        return 3_000_000L + blessed * 500_000L;
    }

    public static void ensureInitialResidents(ServerLevel level, ServerPlayer owner, BlockPos center) {
        if (level == null || owner == null) return;
        String ownerTag = "TempestOwner_" + owner.getUUID();
        AABB citadelBox = new AABB(center).inflate(52.0, 24.0, 52.0);

        // 1. 5 Công Dân Thủ Công & Hộ Vệ Tempest
        List<Villager> existingVillagers = level.getEntitiesOfClass(Villager.class, citadelBox,
                v -> v.isAlive() && v.getTags().contains(TAG_CITADEL_RESIDENT));
        if (existingVillagers.size() < 5) {
            VillagerProfession[] profs = {
                    VillagerProfession.ARMORER, VillagerProfession.WEAPONSMITH,
                    VillagerProfession.CLERIC, VillagerProfession.LIBRARIAN, VillagerProfession.TOOLSMITH
            };
            String[] names = {
                    "§a[Đại Sư Rèn Tempest] §fKaijin",
                    "§a[Thủ Lĩnh Tempest] §fRigurd",
                    "§d[Vu Nữ Tempest] §fShuna",
                    "§b[Học Giả Tempest] §fVesta",
                    "§e[Thợ Rèn Thần Khí] §fKurobe"
            };
            BlockPos[] spots = {
                    center.offset(24, 1, -2),
                    center.offset(-3, 1, -14),
                    center.offset(3, 1, -14),
                    center.offset(26, 1, -4),
                    center.offset(26, 1, 2)
            };
            for (int i = existingVillagers.size(); i < 5; i++) {
                Villager v = EntityType.VILLAGER.create(level);
                if (v != null) {
                    v.setPos(spots[i].getX() + 0.5, spots[i].getY() + 0.1, spots[i].getZ() + 0.5);
                    v.setVillagerData(v.getVillagerData().setProfession(profs[i]).setLevel(5));
                    v.setCustomName(Component.literal(names[i]));
                    v.setCustomNameVisible(true);
                    v.setPersistenceRequired();
                    v.addTag("TempestCitizen");
                    v.addTag(TAG_CITADEL_RESIDENT);
                    v.addTag(ownerTag);
                    level.addFreshEntity(v);
                }
            }
        }

        // 2. Đội Cận Vệ Ác Ma Hội Đồng (thuộc dòng dõi Noir/Blanc/Jaune/Violet - KHÔNG sinh trùng Thủy Tổ gốc!)
        // Dọn sạch bản sao Thủy Tổ cũ nếu từng được spawn làm cư dân Thành Trì
        for (PrimordialDemonEntity oldClone : level.getEntitiesOfClass(PrimordialDemonEntity.class, citadelBox,
                d -> d.isAlive() && d.getTags().contains(TAG_CITADEL_RESIDENT))) {
            oldClone.discard();
        }
        List<UnderworldDemonEntity> existingDemons = level.getEntitiesOfClass(UnderworldDemonEntity.class, citadelBox,
                d -> d.isAlive() && d.getTags().contains(TAG_CITADEL_RESIDENT));
        if (existingDemons.isEmpty()) {
            DemonType[] councilLineages = {DemonType.NOIR, DemonType.BLANC, DemonType.JAUNE, DemonType.VIOLET};
            String[] councilNames = {"Beretta (Dòng Dõi Hắc)", "Moss (Dòng Dõi Bạch)", "Esprit (Dòng Dõi Hoàng)", "Veyron (Dòng Dõi Tử)"};
            BlockPos[] seats = {
                    center.offset(-4, 1, 0),
                    center.offset(4, 1, 0),
                    center.offset(0, 1, -4),
                    center.offset(0, 1, 4)
            };
            BlockPos palaceGuard = center.offset(0, 1, 0);
            for (int i = 0; i < councilLineages.length; i++) {
                UnderworldDemonEntity demon = ModEntities.UNDERWORLD_DEMON.get().create(level);
                if (demon != null) {
                    demon.setPos(seats[i].getX() + 0.5, seats[i].getY() + 0.1, seats[i].getZ() + 0.5);
                    demon.tame(owner);
                    demon.setOwnerUUID(owner.getUUID());
                    demon.setDemonType(councilLineages[i]);
                    demon.setRank(UnderworldDemonEntity.DemonRank.ARCHDEMON);
                    demon.setPhysicalBody(true);
                    demon.setBodyOrigin("Thánh Thể Ma Cương Jura Tempest");
                    demon.setNamed(true);
                    demon.setCustomDemonName(councilNames[i]);
                    demon.setWinged(true);
                    demon.applyCalculatedStats(true);
                    demon.refreshDisplayTitle();
                    demon.setGuardAnchor(palaceGuard);
                    demon.setPersistenceRequired();
                    demon.addTag("TensuraNamedMob");
                    demon.addTag(TAG_CITADEL_RESIDENT);
                    demon.addTag(ownerTag);
                    level.addFreshEntity(demon);
                }
            }
        }

        // 3. 3 Tinh Linh Nguyên Tố hộ vệ Đền Thờ Tinh Linh (Cánh Đông)
        List<ElementalSpiritEntity> existingSpirits = level.getEntitiesOfClass(ElementalSpiritEntity.class, citadelBox,
                s -> s.isAlive() && s.getTags().contains(TAG_CITADEL_RESIDENT));
        if (existingSpirits.isEmpty()) {
            ElementalType[] types = {ElementalType.FIRE, ElementalType.WIND, ElementalType.LIGHT};
            BlockPos shrinePos = center.offset(JuraTempestCitadelBuilder.SHRINE_DX, JuraTempestCitadelBuilder.SHRINE_DY, JuraTempestCitadelBuilder.SHRINE_DZ);
            for (int i = 0; i < types.length; i++) {
                ElementalSpiritEntity sp = ModEntities.ELEMENTAL_SPIRIT.get().create(level);
                if (sp != null) {
                    sp.setPos(shrinePos.getX() + (i - 1) * 2.0 + 0.5, shrinePos.getY() + 2.0, shrinePos.getZ() + 0.5);
                    sp.tame(owner);
                    sp.setOwnerUUID(owner.getUUID());
                    sp.setElementalType(types[i]);
                    sp.setCommandMode(1);
                    sp.setHomeShrinePos(shrinePos);
                    sp.setPersistenceRequired();
                    sp.addTag(TAG_CITADEL_RESIDENT);
                    sp.addTag(ownerTag);
                    level.addFreshEntity(sp);
                }
            }
        }
    }

    public static void inviteAlliesIntoCitadel(ServerPlayer player) {
        ServerLevel targetLevel = JuraTempestCitadelBuilder.resolveTargetLevel(player.server);
        if (targetLevel == null) return;
        BlockPos center = JuraTempestCitadelBuilder.getCitadelCenter(targetLevel, player.getUUID());
        JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, player, center);

        int invited = 0;
        AABB scan = player.getBoundingBox().inflate(36.0D);
        List<Entity> candidates = player.level().getEntities(player, scan, e ->
                e.isAlive() && (
                        (e instanceof PrimordialDemonEntity d && d.isTame() && player.getUUID().equals(d.getOwnerUUID()))
                        || (e instanceof ElementalSpiritEntity s && s.isTame() && player.getUUID().equals(s.getOwnerUUID()))
                        || (e instanceof VelgryndEntity vg && vg.isAllied() && player.getUUID().equals(vg.getParsedOwnerUUID()))
                        || (e instanceof Villager v && v.getTags().contains("TempestCitizen"))
                ));

        Vec3 dest = JuraTempestCitadelBuilder.getPlazaSpawnPos(center);
        for (Entity e : candidates) {
            if (e.level() != targetLevel) {
                e.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(
                        targetLevel, dest, Vec3.ZERO, e.getYRot(), e.getXRot(),
                        net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING
                ));
            } else {
                e.teleportTo(dest.x + (invited % 3 - 1) * 1.5, dest.y, dest.z + 3.0);
            }
            if (e instanceof PrimordialDemonEntity d) {
                d.setGuardMode(true, center.offset(0, 1, 0));
            }
            e.addTag(TAG_CITADEL_RESIDENT);
            invited++;
        }

        ensureInitialResidents(targetLevel, player, center);
        player.displayClientMessage(Component.literal("§6§l[THÀNH TRÌ TEMPEST] §fĐã tập hợp & mời §e" + invited + " §fđồng minh/cư dân vào Thành Trì!"), true);
    }

    public static void blessCitadelResidents(ServerPlayer player) {
        ServerLevel targetLevel = JuraTempestCitadelBuilder.resolveTargetLevel(player.server);
        if (targetLevel == null) return;
        BlockPos center = JuraTempestCitadelBuilder.getCitadelCenter(targetLevel, player.getUUID());
        AABB box = new AABB(center).inflate(55.0, 25.0, 55.0);

        List<LivingEntity> residents = targetLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.getTags().contains(TAG_CITADEL_RESIDENT) && !e.getTags().contains("TempestBlessed"));

        if (residents.isEmpty()) {
            player.displayClientMessage(Component.literal("§eTất cả cư dân hiện có trong Thành Trì đều đã được Ban Danh Xưng & Chúc Phúc!"), true);
            return;
        }

        int count = 0;
        for (LivingEntity le : residents) {
            le.addTag("TempestBlessed");
            le.addTag("TensuraNamedMob");
            var hpAttr = le.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.setBaseValue(Math.max(hpAttr.getBaseValue(), 400.0D) * 2.0D);
                le.setHealth(le.getMaxHealth());
            }
            le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 72000, 1, true, false));
            le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 72000, 1, true, false));
            targetLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, le.getX(), le.getY() + 1.2, le.getZ(), 18, 0.4, 0.6, 0.4, 0.15);
            count++;
        }

        CompoundTag data = EntityDataHelper.getCustomData(player);
        data.putInt(KEY_BLESSED_COUNT, data.getInt(KEY_BLESSED_COUNT) + count);
        MagiculesManager.onSkillCastPulse(player, 25_000L);
        TensuraSkillHelper.syncSkillStateToClient(player);
        targetLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.1F, 1.3F);
        SageManager.sendCornerAlert(player, "Ban Danh Xưng Thành Trì: Đã ban phước " + count + " cư dân -> Tăng vĩnh viễn +Ma Tố & +EP Vương Quốc!", 0xFFFACC15, 5000, false);
    }

    public static void produceSupplies(ServerPlayer player, boolean manual) {
        if (player == null) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        int blessed = Math.max(1, data.getInt(KEY_BLESSED_COUNT));

        ItemStack[] bundle = {
                new ItemStack(Items.GOLDEN_APPLE, Math.min(8, 1 + blessed / 3)),
                new ItemStack(Items.NETHERITE_SCRAP, Math.min(4, 1 + blessed / 5)),
                new ItemStack(Items.DIAMOND, Math.min(6, 1 + blessed / 4)),
                new ItemStack(Items.EXPERIENCE_BOTTLE, Math.min(16, 4 + blessed * 2))
        };
        int added = 0;
        for (ItemStack st : bundle) {
            if (AzathothStomachData.storeItemStackIntoStomach(player, st, false)) {
                added++;
            }
        }
        if (added > 0 && manual) {
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.3F);
            SageManager.sendCornerAlert(player, "Phố Thủ Công Tempest vừa tinh luyện " + added + " gói vật tư gửi thẳng vào Dạ Dày Vô Hạn Azathoth!", 0xFF34D399, 4500, false);
        }
    }

    public static void spawnTrainingDummy(ServerLevel level, ServerPlayer player, BlockPos center) {
        AABB arenaBox = new AABB(center.offset(JuraTempestCitadelBuilder.COLOSSEUM_CENTER_DX, 1, JuraTempestCitadelBuilder.COLOSSEUM_CENTER_DZ)).inflate(22.0);
        for (IronGolem g : level.getEntitiesOfClass(IronGolem.class, arenaBox, e -> e.getTags().contains(TAG_TRAINING_DUMMY))) {
            g.discard();
        }
        IronGolem dummy = EntityType.IRON_GOLEM.create(level);
        if (dummy != null) {
            Vec3 pos = JuraTempestCitadelBuilder.getColosseumCenterPos(center);
            dummy.setPos(pos.x, pos.y + 0.2, pos.z);
            var hpAttr = dummy.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) hpAttr.setBaseValue(100_000.0D);
            dummy.setHealth(dummy.getMaxHealth());
            dummy.setNoAi(true);
            dummy.setPersistenceRequired();
            dummy.addTag(TAG_TRAINING_DUMMY);
            dummy.setCustomName(Component.literal("§e⚔ [BIA THỬ SÁT THƯƠNG BẤT TỬ] §7(Chém để đo DMG)"));
            dummy.setCustomNameVisible(true);
            level.addFreshEntity(dummy);
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.5, pos.z, 25, 0.5, 1.0, 0.5, 0.05);
        }
    }

    public static void handleTrainingDummyHurt(LivingEntity victim, float amount, Entity attacker) {
        if (!victim.getTags().contains(TAG_TRAINING_DUMMY)) return;
        CompoundTag tag = EntityDataHelper.getCustomData(victim);
        long now = victim.level().getGameTime();
        long lastHit = tag.getLong("DummyLastHitTick");
        double comboTotal = (now - lastHit <= 100L) ? (tag.getDouble("DummyComboDmg") + amount) : amount;
        tag.putLong("DummyLastHitTick", now);
        tag.putDouble("DummyComboDmg", comboTotal);

        victim.setHealth(victim.getMaxHealth());
        String msg = String.format(Locale.US, "§e⚔ Sát thương: §c%.1f §8| §6Tổng Combo: §a%.1f", amount, comboTotal);
        victim.setCustomName(Component.literal(msg));
        victim.setCustomNameVisible(true);
        if (attacker instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(msg), true);
        }
    }

    public static void summonColosseumBoss(ServerLevel level, ServerPlayer player, BlockPos center, int bossType) {
        Vec3 pos = JuraTempestCitadelBuilder.getColosseumCenterPos(center);
        Mob boss = switch (bossType) {
            case 0 -> ModEntities.CHARYBDIS.get().create(level);
            case 1 -> ModEntities.ORC_DISASTER_GELD.get().create(level);
            case 2 -> ModEntities.VELGRYND.get().create(level);
            case 3 -> ModEntities.VELZARD.get().create(level);
            case 4 -> ModEntities.MILIM.get().create(level);
            case 5 -> ModEntities.CHRONOA.get().create(level);
            case 6 -> ModEntities.KUBO.get().create(level);
            default -> ModEntities.DEMON_LORD_KUBO.get().create(level);
        };
        if (boss == null) return;
        boss.setPos(pos.x, pos.y + (bossType == 0 ? 5.0 : 0.3), pos.z);
        boss.setPersistenceRequired();
        boss.addTag(TAG_COLOSSEUM_BOSS);
        level.addFreshEntity(boss);

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.4F, 1.1F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.5, pos.z, 2, 0.5, 0.5, 0.5, 0.0);
        player.displayClientMessage(Component.literal("§c§l⚔ [ĐẤU TRƯỜNG VÔ CỰC] §fĐã triệu hồi đối thủ: §e" + boss.getName().getString() + "§f!"), true);
    }

    public static void resetColosseum(ServerLevel level, ServerPlayer player, BlockPos center) {
        AABB arenaBox = new AABB(center.offset(JuraTempestCitadelBuilder.COLOSSEUM_CENTER_DX, 1, JuraTempestCitadelBuilder.COLOSSEUM_CENTER_DZ)).inflate(28.0);
        for (Mob m : level.getEntitiesOfClass(Mob.class, arenaBox, e ->
                e.getTags().contains(TAG_COLOSSEUM_BOSS) || e.getTags().contains(TAG_TRAINING_DUMMY))) {
            m.discard();
        }
        JuraTempestCitadelBuilder.buildColosseumIsland(level, center);
        player.displayClientMessage(Component.literal("§a§l✔ [ĐẤU TRƯỜNG VÔ CỰC] §fĐã dọn sạch Boss & tái tạo 100% sàn Đấu Trường!"), true);
    }

    public static void openCitadelGui(ServerPlayer sp) {
        ServerLevel targetLevel = JuraTempestCitadelBuilder.resolveTargetLevel(sp.server);
        if (targetLevel == null) return;
        BlockPos center = JuraTempestCitadelBuilder.getCitadelCenter(targetLevel, sp.getUUID());
        AABB box = new AABB(center).inflate(58.0, 28.0, 58.0);
        int residentCount = targetLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.getTags().contains(TAG_CITADEL_RESIDENT)).size();
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        boolean autoProduce = !data.contains(KEY_AUTO_PRODUCE) || data.getBoolean(KEY_AUTO_PRODUCE);
        int blessedCount = data.getInt(KEY_BLESSED_COUNT);
        long dayTime = targetLevel.getDayTime() % 24000L;

        ModMessages.sendToPlayer(new ClientboundOpenTempestCitadelPacket(
                residentCount,
                blessedCount,
                getBonusEp(sp),
                autoProduce,
                dayTime,
                JuraTempestCitadelBuilder.isCustomDimension(sp.level()) || JuraTempestCitadelBuilder.isInsideCitadelOrDimension(sp.level(), sp.position())
        ), sp);
    }

    public static void handleAction(ServerPlayer sp, int action, int param) {
        ServerLevel targetLevel = JuraTempestCitadelBuilder.resolveTargetLevel(sp.server);
        if (targetLevel == null) return;
        BlockPos center = JuraTempestCitadelBuilder.getCitadelCenter(targetLevel, sp.getUUID());

        switch (action) {
            case 0 -> DimensionalSanctuaryAbility.enterSanctuary(sp, sp.getUUID()); // Plaza
            case 1 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                sp.teleportTo(targetLevel, center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 9.0, 0.0F, 0.0F);
                sp.displayClientMessage(Component.literal("§6✦ Đã tới Đại Điện Ngai Vàng & Hội Đồng Thủy Tổ!"), true);
            }
            case 2 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                sp.teleportTo(targetLevel, center.getX() - 25.0, center.getY() + 1.0, center.getZ() - 2.0, 90.0F, 0.0F);
                sp.displayClientMessage(Component.literal("§b♨ Đã tới Khu Onsen Hoàng Gia & Vườn Anh Đào!"), true);
            }
            case 3 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                Vec3 arenaEntry = new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 47.0);
                sp.teleportTo(targetLevel, arenaEntry.x, arenaEntry.y, arenaEntry.z, 0.0F, 0.0F);
                sp.displayClientMessage(Component.literal("§c⚔ Đã tới Đấu Trường Thử Nghiệm Tối Thượng!"), true);
            }
            case 4 -> DimensionalSanctuaryAbility.exitSanctuary(sp);
            case 5 -> {
                // Đặt thời gian Thành Trì: 0=Trưa (6000), 1=Hoàng Hôn (12500), 2=Đêm Sao (18000)
                long t = switch (param) {
                    case 1 -> 12500L;
                    case 2 -> 18000L;
                    default -> 6000L;
                };
                targetLevel.setDayTime(t);
                sp.displayClientMessage(Component.literal("§e☀ Đã đổi bầu trời Thành Trì Jura Tempest!"), true);
                openCitadelGui(sp);
            }
            case 6 -> {
                CompoundTag data = EntityDataHelper.getCustomData(sp);
                boolean current = !data.contains(KEY_AUTO_PRODUCE) || data.getBoolean(KEY_AUTO_PRODUCE);
                data.putBoolean(KEY_AUTO_PRODUCE, !current);
                openCitadelGui(sp);
            }
            case 7 -> {
                produceSupplies(sp, true);
                openCitadelGui(sp);
            }
            case 8 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                ensureInitialResidents(targetLevel, sp, center);
                inviteAlliesIntoCitadel(sp);
                openCitadelGui(sp);
            }
            case 9 -> {
                blessCitadelResidents(sp);
                openCitadelGui(sp);
            }
            case 10 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                spawnTrainingDummy(targetLevel, sp, center);
                sp.teleportTo(targetLevel, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 55.0, 0.0F, 0.0F);
            }
            case 11 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                summonColosseumBoss(targetLevel, sp, center, param);
                sp.teleportTo(targetLevel, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 51.0, 0.0F, 0.0F);
            }
            case 12 -> {
                JuraTempestCitadelBuilder.buildIfAbsent(targetLevel, sp, center);
                resetColosseum(targetLevel, sp, center);
                openCitadelGui(sp);
            }
        }
    }

    public static void tickBackgroundProduction(ServerPlayer sp) {
        if (sp == null || sp.level().isClientSide()) return;
        CompoundTag data = EntityDataHelper.getCustomData(sp);
        if (!data.getBoolean(KEY_CITADEL_ESTABLISHED)) return;
        boolean autoProduce = !data.contains(KEY_AUTO_PRODUCE) || data.getBoolean(KEY_AUTO_PRODUCE);
        if (!autoProduce) return;
        long now = sp.serverLevel().getGameTime();
        if (now % 600L == 0L) {
            produceSupplies(sp, false);
        }
    }
}
