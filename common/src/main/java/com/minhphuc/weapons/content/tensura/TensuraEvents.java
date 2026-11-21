package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.UnderworldDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
import net.minecraft.world.entity.Mob;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class TensuraEvents {

    public static void register() {
        EntityEvent.LIVING_DEATH.register(TensuraEvents::onLivingDeath);
        EntityEvent.LIVING_HURT.register(TensuraEvents::onLivingHurt);
        InteractionEvent.RIGHT_CLICK_BLOCK.register(TensuraEvents::onRightClickBlock);
        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (player instanceof ServerPlayer sp) {
                if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.dispelByPunch(sp, pos)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.dispelByPunch(sp, pos)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.checkEmptyHandPunch(sp)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.dispelByPunch(sp, pos)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.checkEmptyHandPunch(sp)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.checkEmptyHandPunch(sp)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.divine.PurificationPillarAbility.dispelByPunch(sp, pos)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.divine.PurificationPillarAbility.checkEmptyHandPunch(sp)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.DomainBarrierBlock.isAnyDomainBarrier(sp.level().getBlockState(pos).getBlock())) {
                    com.minhphuc.weapons.content.tensura.DomainBarrierBlock.shatterDomain(sp, (ServerLevel) sp.level(), pos);
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility.dispelByPunch(sp, pos)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility.checkEmptyHandPunch(sp)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.SpatialTeleportAbility.dispelByPunch(sp, pos)) {
                    return EventResult.interruptTrue();
                }
                if (com.minhphuc.weapons.content.tensura.SpatialTeleportAbility.checkEmptyHandPunch(sp)) {
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });
        InteractionEvent.INTERACT_ENTITY.register(TensuraEvents::onInteractEntity);
        dev.architectury.event.events.common.PlayerEvent.PLAYER_JOIN.register(TensuraEvents::onPlayerJoin);
        dev.architectury.event.events.common.PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            MagiculesManager.onPlayerRespawn(oldPlayer, newPlayer);
        });
        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(player -> DEMON_GREETING_TIMESTAMPS.remove(player.getUUID()));
        dev.architectury.event.events.common.TickEvent.PLAYER_POST.register(TensuraEvents::onPlayerTick);
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(level -> {
            JaunePlayerSkillManager.tick(level);
            CarreraBulletLogic.tickVortices();
            PrimordialSummonRitual.tickRituals(level);
            VelgryndSummonRitual.tickRituals(level);
            ResidualMagicCircleManager.tickResidualCircles(level);
            DeathStreakAbility.tickStreaks(level);
            PentagramCelestialPillarAbility.tickPillars(level);
            HorizontalHolyBeamAbility.tickBeams(level);
            com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.tickPrisons(level);
            com.minhphuc.weapons.content.evolution.VillageHealingCircleManager.tick(level);
            com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.tickBarriers(level);
            com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.tick(level);
            com.minhphuc.weapons.content.tensura.HellflareAbility.tickHellflares(level);
            com.minhphuc.weapons.content.tensura.DarkFlameShotAbility.tick(level);
            com.minhphuc.weapons.content.tensura.NihilityCollapseAbility.tickCollapses(level);
            com.minhphuc.weapons.content.tensura.SupernovaAbility.tickSupernovas(level);
            com.minhphuc.weapons.content.tensura.AzathothAbility.tick(level);
            com.minhphuc.weapons.content.space.OrbitalStrikeManager.tickStrikes(level);
            com.minhphuc.weapons.content.tensura.DimensionalSanctuaryAbility.tick(level);
            com.minhphuc.weapons.content.tensura.SpatialTeleportAbility.tick(level);
            com.minhphuc.weapons.content.tensura.PrimordialPossessionManager.tickDomains(level);
            com.minhphuc.weapons.content.tensura.ApocalypseAbility.tickApocalypses(level);
            com.minhphuc.weapons.content.tensura.DemonicGateManager.tick(level);
            tickEvokerWitchDemonSummons(level);
            tickAutonomousSpiritAndTempest(level);
            MobMeleeSkillSystem.tickWorldMobs(level);
            com.minhphuc.weapons.content.tensura.GreedAbility.tickPuppets(level);
        });
    }

    private static final java.util.Map<java.util.UUID, java.util.Map<java.util.UUID, Long>> DEMON_GREETING_TIMESTAMPS = new java.util.HashMap<>();

    public static void onPlayerTick(Player rawPlayer) {
        if (rawPlayer.level().isClientSide() || !(rawPlayer instanceof ServerPlayer sp)) return;

        // 0. XỬ LÝ NHẬP XÁC ÁC MA THỦY TỔ (BẤM SHIFT ĐỂ TÁCH XÁC + HỒI PHỤC SIÊU TỐC)
        if (PrimordialPossessionManager.isPossessing(sp)) {
            if (sp.isShiftKeyDown()) {
                PrimordialPossessionManager.stopPossession(sp, false);
                return;
            }
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
                sp.getAbilities().setFlyingSpeed(0.08F);
                sp.onUpdateAbilities();
            }
            // Hồi phục sinh lực ma tộc 10 HP/s
            if (sp.tickCount % 20 == 0 && sp.getHealth() < sp.getMaxHealth()) {
                sp.heal(10.0F);
            }
        }
        com.minhphuc.weapons.content.divine.HinataArmorItem.cleanupDetachedModifiers(sp);

        // 1. CƠ CHẾ BAY (FLIGHT) CHO NGƯỜI CHƠI LÀ ÁC MA HOẶC MA VƯƠNG
        boolean canFlyTensura = PrimordialPlayerDataHelper.isPrimordial(sp) || PrimordialPlayerDataHelper.isDemonLord(sp);
        if (canFlyTensura) {
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
                sp.getAbilities().setFlyingSpeed(0.08F);
                sp.onUpdateAbilities();
            } else if (sp.tickCount % 20 == 0) {
                // Đồng bộ định kỳ mỗi giây (20 ticks) đảm bảo Client không bị mất quyền bay
                sp.getAbilities().setFlyingSpeed(0.08F);
                sp.onUpdateAbilities();
            }
        } else if (!sp.isCreative() && !sp.isSpectator() && sp.getAbilities().mayfly) {
            sp.getAbilities().mayfly = false;
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }

        // 2. TƯƠNG TÁC CHÀO HỎI KHI NGƯỜI CHƠI BIẾN THÀNH ÁC MA GẶP CÁC ÁC MA KHÁC (Mỗi 40 ticks = 2s)
        if (sp.tickCount % 40 == 0 && PrimordialPlayerDataHelper.isPrimordial(sp)) {
            DemonType playerType = PrimordialPlayerDataHelper.getPrimordialType(sp);
            if (playerType != null && sp.level() instanceof ServerLevel sl) {
                List<PrimordialDemonEntity> nearbyDemons = sl.getEntitiesOfClass(
                        PrimordialDemonEntity.class,
                        sp.getBoundingBox().inflate(10.0D),
                        d -> d.isAlive() && d.getDemonType() != playerType
                );

                long gameTime = sl.getGameTime();
                for (PrimordialDemonEntity demon : nearbyDemons) {
                    var playerMap = DEMON_GREETING_TIMESTAMPS.computeIfAbsent(sp.getUUID(), k -> new java.util.HashMap<>());
                    long lastGreet = playerMap.getOrDefault(demon.getUUID(), 0L);

                    // Cooldown chào hỏi 60 giây (1200 ticks) tránh spam
                    if (gameTime - lastGreet >= 1200L) {
                        playerMap.put(demon.getUUID(), gameTime);
                        demon.getLookControl().setLookAt(sp, 30.0F, 30.0F);

                        String greetingKey = "dialogue.weapons.demon.greet_" + playerType.name().toLowerCase();
                        TensuraDialogueManager.sayDemonGreeting(demon, demon.getDemonType(), sp, greetingKey);
                        break; // Mỗi lần quét chỉ chào 1 câu
                    }
                }
            }
        }

        // 3. Đại Hiền Giả / Raphael: Quét Hang Động Phong Ấn, vũ khí phẩm cấp & Auto-Battle
        SageManager.tickPlayer(sp);
        MagiculesManager.tickPlayer(sp);
        AzathothStomachData.tickAutoLoot(sp);
        JuraTempestManager.tickBackgroundProduction(sp);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        CompoundTag playerData = EntityDataHelper.getCustomData(player);
        if (!playerData.getBoolean("ReceivedCelestialTome")) {
            playerData.putBoolean("ReceivedCelestialTome", true);
            ItemStack tome = new ItemStack(ModItems.GUIDE_BOOK.get());
            if (!player.getInventory().add(tome)) {
                player.drop(tome, false);
            }
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6§l[WEAPONS MOD] §eChào mừng bạn! Đã nhận §b§lKhởi Nguyên Thần Thư§e. Hãy cầm sách nhấn §a[Chuột Phải] §eđể xem toàn bộ công thức và chiêu thức! 📜✨"),
                false
            );
        }

        // Đồng bộ quyền bay và lượng máu tối đa cho Chân Ma Vương / Thủy Tổ Ác Ma khi đăng nhập lại thế giới
        boolean canFlyTensura = PrimordialPlayerDataHelper.isPrimordial(player) || PrimordialPlayerDataHelper.isDemonLord(player);
        if (canFlyTensura) {
            if (PrimordialPlayerDataHelper.isDemonLord(player)) {
                var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (attr != null && attr.getBaseValue() < 80.0D) {
                    attr.setBaseValue(80.0D);
                }
            }
            player.getAbilities().mayfly = true;
            player.getAbilities().setFlyingSpeed(0.08F);
            player.onUpdateAbilities();
        }

        // Đảm bảo Lồng Giam Vô Hạn 3D của Hang Động Veldora luôn được khôi phục & đồng bộ khi vào lại thế giới
        if (player.getServer() != null) {
            ServerLevel ow = player.getServer().overworld();
            com.minhphuc.weapons.world.VeldoraWorldData.get(ow).ensurePrisonRegisteredIfSealed(ow);
        }

        // Đồng bộ kết giới, lồng giam bảo hộ thời tiết, Ấn Ký Không Gian và trạng thái Nhập Xác
        AntiMagicBarrierManager.syncBarriersToPlayer(player);
        com.minhphuc.weapons.content.evolution.InfiniteDragonPrisonAbility.syncPrisonsToPlayer(player);
        SpatialTeleportAbility.syncVisualMarksToPlayer(player);
        PrimordialPossessionManager.syncToClient(player);
    }

    public static boolean hasSeedItemInInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.DEMON_LORD_SEED.get())) {
                return true;
            }
        }
        return false;
    }

    public static void consumeSeedItemIfPresent(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.DEMON_LORD_SEED.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static int countSoulsInInventory(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.DEMON_LORD_SOUL.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int getAvailableSouls(ServerPlayer player) {
        CompoundTag playerData = EntityDataHelper.getCustomData(player);
        int nbtSouls = playerData.getInt("TensuraCollectedSouls");
        int invSouls = countSoulsInInventory(player);
        return nbtSouls + invSouls;
    }

    public static void consumeAllSouls(ServerPlayer player) {
        // Xóa sạch toàn bộ vật phẩm Linh Hồn trên tay chính, tay phụ và trong túi đồ
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.DEMON_LORD_SOUL.get())) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        // Xóa bộ đếm trong dữ liệu NBT
        EntityDataHelper.getCustomData(player).putInt("TensuraCollectedSouls", 0);
    }

    public static void handleMobDeathDrop(ServerPlayer player, LivingEntity victim) {
        if (player == null || victim == null || victim instanceof Player) return;

        ServerLevel level = (ServerLevel) player.level();
        CompoundTag playerData = EntityDataHelper.getCustomData(player);
        boolean hasSeed = playerData.getBoolean("TensuraHasSeed") || hasSeedItemInInventory(player);
        boolean isTrueDemonLord = playerData.getBoolean("TensuraTrueDemonLord");

        // 1. Tỷ lệ 25% rớt Hạt Giống Ma Vương khi người chơi đạt cấp độ 10 trở lên và tiêu diệt quái vật thường
        boolean isMonster = victim instanceof net.minecraft.world.entity.monster.Monster;
        if (!hasSeed && !isTrueDemonLord && isMonster && player.experienceLevel >= 10 && level.random.nextFloat() <= 0.25F) {
            ItemEntity seedDrop = new ItemEntity(
                level, victim.getX(), victim.getY() + 0.5D, victim.getZ(),
                new ItemStack(ModItems.DEMON_LORD_SEED.get())
            );
            level.addFreshEntity(seedDrop);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.2F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, victim.getX(), victim.getY() + 0.5D, victim.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.1D);

            VoiceOfTheWorld.announce(player, "Báo cáo. Cá thể vừa thu nhận được Hạt Giống Ma Vương! Hãy nuốt nó để bắt đầu con đường thức tỉnh.");
            return;
        }

        // 2. Thu thập Linh Hồn Ma Vương nếu đã kích hoạt Hạt Giống (chưa thành Chân Ma Vương)
        // Áp dụng cho MỌI LOẠI VŨ KHÍ (Kiếm, Cung, Nỏ, Rìu, Găng tay, Búng tay SNAP...)
        if (hasSeed && !isTrueDemonLord) {
            // Rớt ra vật phẩm Linh Hồn Ma Vương dưới chân quái tử trận
            ItemEntity soulDrop = new ItemEntity(
                level, victim.getX(), victim.getY() + 0.5D, victim.getZ(),
                new ItemStack(ModItems.DEMON_LORD_SOUL.get())
            );
            level.addFreshEntity(soulDrop);

            int totalSouls = getAvailableSouls(player);
            if (totalSouls < 64) {
                int nbtSouls = playerData.getInt("TensuraCollectedSouls") + 1;
                playerData.putInt("TensuraCollectedSouls", nbtSouls);
                int totalAfter = getAvailableSouls(player);

                // Kiểm tra Thể Xác Vật Lý (40 Linh Hồn) cho người chơi Thủy Tổ Ác Ma
                if (PrimordialPlayerDataHelper.isPrimordial(player) && !PrimordialPlayerDataHelper.hasPhysicalBody(player) && totalAfter >= 40) {
                    PrimordialPlayerDataHelper.setPhysicalBody(player, true);
                    VoiceOfTheWorld.announce(player, "§d§l[GIỌNG NÓI THẾ GIỚI] §bBáo cáo. Cá thể đã thu thập đủ 40 Linh Hồn Ma Vương! Quá trình kiến tạo Thể Xác Vật Lý thành công.");
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
                }

                if (totalAfter % 10 == 0) {
                    VoiceOfTheWorld.announce(player, "Báo cáo. Tiến độ thu thập Linh Hồn Ma Vương: " + totalAfter + "/64.");
                }
            }
        }
        if (victim instanceof net.minecraft.world.entity.animal.IronGolem golem) {
            com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.onIronGolemDeath(level, golem);
        }
    }
    private static void handleSoulDrop(LivingEntity victim) {
        if (victim == null || victim.level().isClientSide()) return;
        if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(victim.level(), victim.position())) return;
        if (victim instanceof net.minecraft.world.entity.monster.Creeper) return; // Trừ Creeper theo yêu cầu
        if (victim instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity) return;

        net.minecraft.world.item.Item soulItem = null;
        if (victim instanceof com.minhphuc.weapons.entity.tensura.MilimEntity) {
            soulItem = ModItems.MILIM_SOUL.get();
        } else if (victim instanceof com.minhphuc.weapons.entity.tensura.MariabellEntity) {
            soulItem = ModItems.MARIABELL_SOUL.get();
        } else if (victim instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity) {
            soulItem = ModItems.VELGRYND_SOUL.get();
        } else if (victim instanceof com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity) {
            soulItem = ModItems.PRIMORDIAL_DEMON_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.monster.warden.Warden) {
            soulItem = ModItems.WARDEN_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            soulItem = ModItems.ENDER_DRAGON_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            soulItem = ModItems.WITHER_SOUL.get();
        } else if (victim instanceof net.minecraft.world.entity.monster.Monster || victim instanceof net.minecraft.world.entity.monster.Enemy) {
            soulItem = ModItems.MOB_SOUL.get();
        } else {
            // MỌI SINH VẬT KHÁC (Gấu tuyết, Cáo, Sói tuyết, Động vật, Dân làng...) đều rơi Linh Hồn
            soulItem = ModItems.MOB_SOUL.get();
        }

        if (soulItem != null) {
            ItemStack soulStack = new ItemStack(soulItem);
            String entityTypeId = net.minecraft.world.entity.EntityType.getKey(victim.getType()).toString();
            com.minhphuc.weapons.data.ItemStackDataHelper.putString(soulStack, "EntityTypeId", entityTypeId);

            boolean isBaby = false;
            if (victim instanceof net.minecraft.world.entity.AgeableMob ageable) {
                isBaby = ageable.isBaby();
            }
            com.minhphuc.weapons.data.ItemStackDataHelper.putBoolean(soulStack, "IsBaby", isBaby);

            if (victim.hasCustomName() && victim.getCustomName() != null) {
                com.minhphuc.weapons.data.ItemStackDataHelper.putString(soulStack, "CustomName", victim.getCustomName().getString());
            }

            String entityName = victim.getName().getString();
            com.minhphuc.weapons.data.ItemStackDataHelper.putString(soulStack, "EntityDisplayName", entityName);
            soulStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("§b§lLinh Hồn: §e" + entityName + (isBaby ? " §d(Con Nhỏ)" : " §a(Trưởng Thành)")));

            net.minecraft.world.entity.item.ItemEntity soulEntity = new net.minecraft.world.entity.item.ItemEntity(
                    victim.level(), victim.getX(), victim.getY() + 0.5D, victim.getZ(),
                    soulStack
            );
            soulEntity.setGlowingTag(true);
            victim.level().addFreshEntity(soulEntity);

            if (victim.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.8D, victim.getZ(), 12, 0.25D, 0.25D, 0.25D, 0.03D);
                sl.sendParticles(ParticleTypes.GLOW, victim.getX(), victim.getY() + 0.8D, victim.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
            }
        }
    }

    public static EventResult onLivingDeath(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) return EventResult.pass();

        // Nếu bị Thần Hư Không Azathoth xóa sổ: Tuyệt đối không rơi đồ hay triệu hồi ác ma
        if (victim.getTags().contains("AzathothErased")) {
            return EventResult.pass();
        }

        // Dân làng tử trận tuyệt đối KHÔNG triệu hồi ác ma

        // Nếu người chơi chết: Xử lý Thủy Tổ Ác Ma theo luật chơi (Giữ lại căn nguyên Ma Vương vĩnh cửu)
        if (victim instanceof ServerPlayer deadPlayer) {
            com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.resetOnDeath(deadPlayer);
            if (PrimordialPlayerDataHelper.isInnatePrimordial(deadPlayer)) {
                PrimordialPlayerDataHelper.resetOnDeath(deadPlayer);
                VoiceOfTheWorld.announce(deadPlayer, "§c§l[TỬ TRẬN] §4Báo cáo. Thân xác Thủy Tổ Ác Ma tạm thời tiêu biến!");
            }
            return EventResult.pass();
        }

        // Tụ Linh Đoạt Phách của Mariabell Rosso
        List<com.minhphuc.weapons.entity.tensura.MariabellEntity> nearbyMariabells = victim.level().getEntitiesOfClass(
                com.minhphuc.weapons.entity.tensura.MariabellEntity.class, victim.getBoundingBox().inflate(25.0D),
                com.minhphuc.weapons.entity.tensura.MariabellEntity::isAlive);
        for (com.minhphuc.weapons.entity.tensura.MariabellEntity m : nearbyMariabells) {
            m.onNearbyEntityDeath(victim);
        }

        ServerPlayer player = null;
        if (source.getEntity() instanceof ServerPlayer sp) {
            player = sp;
        } else if (source.getDirectEntity() instanceof ServerPlayer sp) {
            player = sp;
        } else if (source.getEntity() instanceof PrimordialDemonEntity pde && pde.getOwner() instanceof ServerPlayer sp) {
            player = sp;
        } else if (victim.getLastHurtByMob() instanceof ServerPlayer sp) {
            // Bao gồm quái bị người chơi đánh trúng rồi chết bởi cháy, rơi, hiệu ứng đòn quét...
            player = sp;
        }

        // Rơi Linh Hồn cho Không Vong & người chơi thu thập (Ác ma, Milim, Long chủng, Boss, Mob - Trừ Creeper)
        handleSoulDrop(victim);

        if (player != null) {
            handleMobDeathDrop(player, victim);
            ItemStack mainWeapon = player.getMainHandItem();
            WeaponGradeHelper.WeaponGrade upgraded = WeaponGradeHelper.recordWeaponKill(mainWeapon, victim);
            if (upgraded != null) {
                SageManager.sendCornerAlert(player, "Vũ khí [" + mainWeapon.getHoverName().getString() + "] vừa đột phá phẩm cấp " + upgraded.colorPrefix + upgraded.title + "§f!", 0xFFFACC15, 5500, false);
            }

            // Quyền năng Bị Động [Kẻ Tham Lam]: Thưởng EXP gấp đôi & cướp đoạt trang bị vào Dạ Dày Azathoth
            if (GreedAbility.isUnlocked(player)) {
                player.giveExperiencePoints(Math.max(5, (int)(victim.getExperienceReward(player.serverLevel(), player) * 1.5F)));
                if (victim instanceof net.minecraft.world.entity.Mob m && !m.getMainHandItem().isEmpty() && player.getRandom().nextFloat() < 0.35F) {
                    AzathothStomachData.storeItemStackIntoStomach(player, m.getMainHandItem().copy(), false);
                }
            }

            // Ghi nhận hiến tế Dân Làng (Villager Sacrifice) cho Ác Ma Thủy Tổ
            if (victim instanceof net.minecraft.world.entity.npc.Villager) {
                if (com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(player.serverLevel(), victim.position())) {
                    player.displayClientMessage(
                            Component.literal("§7[KHÁNG MA KẾT GIỚI] §cLinh hồn dân làng được bảo hộ bởi kết giới, không thể hiến tế cho Ác Ma!"),
                            true
                    );
                } else {
                    CompoundTag pData = EntityDataHelper.getCustomData(player);
                    int count = pData.getInt("TensuraVillagersSacrificed") + 1;
                    pData.putInt("TensuraVillagersSacrificed", count);
                    if (count < 10) {
                        VoiceOfTheWorld.announce(player, "§4Báo cáo. Đã thu hoạch linh hồn Dân Làng hiến tế: §e" + count + "/10§4. Tích đủ 10 linh hồn để thức tỉnh Thể Xác/Danh Xưng cho Ác Ma Thủy Tổ!");
                    } else if (count == 10) {
                        VoiceOfTheWorld.announce(player, "§6§l[TENSURA] §dĐã hoàn tất 10 linh hồn Dân Làng hiến tế! Khi triệu hồi Ác Ma tiếp theo bằng Khế Ước sẽ kích hoạt thức tỉnh!");
                    }
                }
            }
        }
        // Xác chết / Mob hiến thân cho Ác Ma Ma Giới chưa có thể xác trong phạm vi 10 block
        if (!(victim instanceof UnderworldDemonEntity) && !(victim instanceof PrimordialDemonEntity)
                && victim.level() instanceof ServerLevel slVictim
                && !com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(slVictim, victim.position())) {
            List<UnderworldDemonEntity> nearDemons = slVictim.getEntitiesOfClass(
                    UnderworldDemonEntity.class,
                    victim.getBoundingBox().inflate(10.0D),
                    d -> d.isAlive() && !d.hasPhysicalBody()
            );
            if (!nearDemons.isEmpty()) {
                UnderworldDemonEntity receiver = nearDemons.get(0);
                receiver.grantPhysicalBody("Xác Hiến Tế (" + victim.getName().getString() + ")", player);
            }
        }

        // Orc Disaster Geld: Kỹ Năng Đói Khát (Starved) tự nuốt chửng lính Orc & sinh vật gục ngã gần đó!
        if (!(victim instanceof com.minhphuc.weapons.entity.tensura.OrcDisasterGeldEntity) && victim.level() instanceof ServerLevel slOrc) {
            List<com.minhphuc.weapons.entity.tensura.OrcDisasterGeldEntity> gelds = slOrc.getEntitiesOfClass(
                    com.minhphuc.weapons.entity.tensura.OrcDisasterGeldEntity.class,
                    victim.getBoundingBox().inflate(18.0D),
                    LivingEntity::isAlive
            );
            for (var geld : gelds) {
                float healAmt = victim.getTags().contains("OrcHorde") ? 60.0F : 30.0F;
                geld.absorbPrey(slOrc, healAmt);
            }
        }

        return EventResult.pass();
    }

    private static void tickEvokerWitchDemonSummons(ServerLevel level) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        long now = level.getGameTime();
        if (now % 100L != 0L) return;

        for (ServerPlayer sp : level.players()) {
            AABB scanBox = sp.getBoundingBox().inflate(24.0D);
            List<Mob> casters = level.getEntitiesOfClass(Mob.class, scanBox,
                    m -> m.isAlive() && m.getTarget() != null
                            && (m instanceof net.minecraft.world.entity.monster.Evoker
                            || m instanceof net.minecraft.world.entity.monster.Witch));
            if (casters.isEmpty()) continue;

            int nearbyDemons = level.getEntitiesOfClass(UnderworldDemonEntity.class, scanBox).size();
            if (nearbyDemons >= 2) continue;

            for (Mob caster : casters) {
                CompoundTag tag = EntityDataHelper.getCustomData(caster);
                long lastSummon = tag.getLong("UnderworldSummonTick");
                if (now - lastSummon < 1200L) continue;
                if (level.random.nextFloat() < 0.14F) {
                    UnderworldDemonEntity summoned = com.minhphuc.weapons.entity.ModEntities.UNDERWORLD_DEMON.get().create(level);
                    if (summoned != null) {
                        double ox = caster.getX() + (level.random.nextDouble() - 0.5D) * 4.0D;
                        double oz = caster.getZ() + (level.random.nextDouble() - 0.5D) * 4.0D;
                        summoned.moveTo(ox, caster.getY() + 0.5D, oz, caster.getYRot(), 0.0F);
                        summoned.finalizeSpawn(level, level.getCurrentDifficultyAt(caster.blockPosition()),
                                net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
                        summoned.setRank(UnderworldDemonEntity.DemonRank.LESSER);
                        summoned.setTarget(caster.getTarget());
                        level.addFreshEntity(summoned);
                        level.sendParticles(ParticleTypes.WITCH, summoned.getX(), summoned.getY() + 1.0D, summoned.getZ(), 24, 0.5, 0.7, 0.5, 0.05);
                        level.playSound(null, summoned.getX(), summoned.getY(), summoned.getZ(),
                                SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.3F, 0.75F);
                        tag.putLong("UnderworldSummonTick", now);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Hệ thống Tinh Linh & Liên Bang Tempest tự vận hành (Chống Lag: mỗi 120 ticks quét quanh player):
     * - Dân làng gần Tế Đàn Tinh Linh tự đến cầu nguyện & nhận Chúc Phúc Tinh Linh -> trở thành Vệ Binh Tempest.
     * - Mob hoang dã gần vùng giàu Ma tố thỉnh thoảng dung hợp Tinh Linh -> thành Tinh Linh Quỷ Hóa (Elemental Elite).
     */
    private static void tickAutonomousSpiritAndTempest(ServerLevel level) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        long now = level.getGameTime();
        if (now % 120L != 0L) return;

        for (ServerPlayer sp : level.players()) {
            AABB area = sp.getBoundingBox().inflate(24.0D);
            List<net.minecraft.world.entity.npc.Villager> villagers = level.getEntitiesOfClass(
                    net.minecraft.world.entity.npc.Villager.class, area,
                    v -> v.isAlive() && !v.getTags().contains("SpiritBlessedVillager")
            );
            if (!villagers.isEmpty()) {
                // Kiểm tra nhanh có SpiritShrineBlock gần đó không (quét mỗi 4 block nhẹ nhàng quanh villager đầu tiên)
                net.minecraft.world.entity.npc.Villager chosen = villagers.get(0);
                BlockPos center = chosen.blockPosition();
                BlockPos foundShrine = null;
                for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -3, -8), center.offset(8, 3, 8))) {
                    if (level.getBlockState(p).is(com.minhphuc.weapons.init.ModBlocks.SPIRIT_SHRINE.get())) {
                        foundShrine = p.immutable();
                        break;
                    }
                }
                if (foundShrine != null) {
                    chosen.addTag("SpiritBlessedVillager");
                    chosen.addTag("TempestCitizen");
                    com.minhphuc.weapons.entity.tensura.ElementalType blessing =
                            com.minhphuc.weapons.entity.tensura.ElementalType.fromId(level.random.nextInt(6));
                    chosen.setCustomName(Component.literal(blessing.colorCode + "[Công Dân Tempest] §fCầu Nguyện " + blessing.displayName));
                    chosen.setCustomNameVisible(true);
                    chosen.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 24000, 1));
                    chosen.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 24000, 0));
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, chosen.getX(), chosen.getY() + 1.2, chosen.getZ(), 16, 0.5, 0.6, 0.5, 0.05);
                    level.sendParticles(ParticleTypes.END_ROD, foundShrine.getX() + 0.5, foundShrine.getY() + 1.2, foundShrine.getZ() + 0.5, 14, 0.4, 0.5, 0.4, 0.05);
                    SageManager.pushAlert(sp, "TEMPEST_VILLAGER_BLESS",
                            "Dân làng đã tự hành lễ tại Tế Đàn Tinh Linh và gia nhập Liên Bang Tempest!", "", 200);
                }
            }

            // Quái hoang dã ngẫu nhiên giao cảm Tinh Linh (hiếm ~6%, tối đa 2 con quanh player)
            if (level.random.nextFloat() < 0.06F) {
                int already = level.getEntitiesOfClass(Mob.class, area, m -> m.getTags().contains("SpiritPossessedElite")).size();
                if (already < 2) {
                    List<net.minecraft.world.entity.monster.Monster> monsters = level.getEntitiesOfClass(
                            net.minecraft.world.entity.monster.Monster.class, area,
                            m -> m.isAlive() && !m.getTags().contains("SpiritPossessedElite")
                                    && m.getMaxHealth() < 120.0F
                    );
                    if (!monsters.isEmpty()) {
                        Mob wild = monsters.get(level.random.nextInt(monsters.size()));
                        wild.addTag("SpiritPossessedElite");
                        com.minhphuc.weapons.entity.tensura.ElementalType eType =
                                com.minhphuc.weapons.entity.tensura.ElementalType.fromId(level.random.nextInt(6));
                        wild.setCustomName(Component.literal(eType.colorCode + "[Tinh Linh Nhập Thể] §f" + wild.getName().getString()));
                        wild.setCustomNameVisible(true);
                        var hp = wild.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                        if (hp != null) {
                            hp.setBaseValue(hp.getBaseValue() * 1.8D);
                            wild.setHealth((float) hp.getValue());
                        }
                        wild.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 24000, 1));
                        level.sendParticles(ParticleTypes.WITCH, wild.getX(), wild.getY() + 1.0, wild.getZ(), 18, 0.4, 0.6, 0.4, 0.05);
                    }
                }
            }
        }
    }

    public static void onPlayerWakeUp(Player rawPlayer) {
        if (rawPlayer.level().isClientSide()) return;
        if (!(rawPlayer instanceof ServerPlayer player)) return;

        CompoundTag playerData = EntityDataHelper.getCustomData(player);
        boolean isTrueDemonLord = playerData.getBoolean("TensuraTrueDemonLord");
        if (isTrueDemonLord) return;

        boolean hasSeed = playerData.getBoolean("TensuraHasSeed") || hasSeedItemInInventory(player);
        int totalSouls = getAvailableSouls(player);

        // Đủ Hạt Giống + ít nhất 64 Linh Hồn -> Kích hoạt Lễ Hội Thức Tỉnh Ma Vương khi thức dậy!
        if (hasSeed && totalSouls >= 64) {
            triggerDemonLordEvolution(player);
        } else if (totalSouls >= 64 && !hasSeed) {
            VoiceOfTheWorld.announce(player, "§cBáo cáo. Cá thể đã thu thập đủ §664 Linh Hồn §cnhưng §eCHƯA CÓ HẠT GIỐNG MA VƯƠNG§c! Cần sở hữu Hạt Giống Ma Vương để làm mầm mống thức tỉnh!");
        } else if (hasSeed && totalSouls > 0 && totalSouls < 64) {
            VoiceOfTheWorld.announce(player, "§7Báo cáo. Tiến độ thức tỉnh của cá thể chưa hoàn tất: §6" + totalSouls + "/64 Linh Hồn§7. Hãy tiêu diệt thêm cá thể để tích đủ!");
        }
    }

    public static boolean triggerDemonLordEvolution(ServerPlayer player) {
        CompoundTag playerData = EntityDataHelper.getCustomData(player);
        boolean isTrueDemonLord = playerData.getBoolean("TensuraTrueDemonLord");
        if (isTrueDemonLord || HarvestFestival.isPlayerInRitual(player)) return false;

        boolean hasSeed = playerData.getBoolean("TensuraHasSeed") || hasSeedItemInInventory(player);
        int totalSouls = getAvailableSouls(player);

        if (!hasSeed) {
            VoiceOfTheWorld.announce(player, "§cBáo cáo. Cá thể chưa sở hữu Hạt Giống Ma Vương, không thể tiến hóa!");
            return false;
        }

        if (totalSouls < 64) {
            VoiceOfTheWorld.announce(player, "§cBáo cáo. Chưa đủ Linh Hồn! Hiện có: §6" + totalSouls + "/64§c.");
            return false;
        }

        // Bắt đầu Lễ Hội Thu Hoạch 4 giai đoạn kịch tính & hoành tráng!
        HarvestFestival.start(player);
        return true;
    }

    private static final ThreadLocal<Boolean> HANDLING_PRIMORDIAL_HURT = ThreadLocal.withInitial(() -> false);

    public static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim == null || victim.level().isClientSide()) return EventResult.pass();

        // Con rối của Kẻ Tham Lam không bao giờ gây sát thương lên người chơi
        if (victim instanceof ServerPlayer && source.getEntity() instanceof net.minecraft.world.entity.Mob puppet
                && puppet.getTags().contains(com.minhphuc.weapons.content.tensura.GreedAbility.TAG_GREED_CONTROLLED)) {
            puppet.setTarget(null);
            return EventResult.interruptFalse();
        }

        // Chặn nếu bị Thần Hư Không Azathoth xóa sổ
        if (victim.getTags().contains(JuraTempestManager.TAG_TRAINING_DUMMY)) {
            JuraTempestManager.handleTrainingDummyHurt(victim, amount, source.getEntity());
            return EventResult.interruptFalse();
        }

        if (victim.getTags().contains("AzathothErased")) {
            return EventResult.interruptFalse();
        }

        // Đòn đánh của Dân Làng Ma Vương: Sát thương ma pháp cực đại trừng phạt quái vật
        if (source.getEntity() instanceof LivingEntity lordMob && lordMob.getTags().contains("DemonLordVillager")) {
            if (victim.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + 1.0, victim.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 1.5F, 0.8F);
            }
            victim.hurt(lordMob.damageSources().magic(), 120.0F);
        }

        // Chặn hoàn toàn nếu đòn đánh trúng Đa Trùng Kết Giới (Multilayer Barrier)
        if (com.minhphuc.weapons.content.tensura.MultilayerBarrierAbility.isDamageBlockedByBarrier(victim, source)) {
            return EventResult.interruptFalse();
        }

        // Tương tác nhận diện và hóa giải cá thể bị Kẻ Tham Lam chi phối
        if (source.getEntity() instanceof ServerPlayer sp && victim.getTags().contains("GreedControlled")) {
            boolean emptyHand = sp.getMainHandItem().isEmpty();
            if (emptyHand) {
                sp.displayClientMessage(
                        Component.literal("§d« Báo Cáo Raphael »: Phát hiện cá thể [" + victim.getName().getString() + "] đang chịu tác động chi phối từ ma tố [Kẻ Tham Lam] của Mariabell Rosso!"),
                        true
                );
            }
            if (com.minhphuc.weapons.content.tensura.SkillNullificationAbility.isEntitySkillSealed(sp)
                    || sp.getTags().contains("CastingSkillNullification")
                    || sp.getTags().contains("BeelzebuthAbsorbing")) {
                if (victim instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity veldora) {
                    veldora.tryPurifyFromGreed(sp);
                } else {
                    victim.removeTag("GreedControlled");
                    victim.removeTag("MariabellEnchanted");
                    if (victim.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
                    }
                    sp.displayClientMessage(
                            Component.literal("§a« Báo Cáo Raphael »: Đã dùng Phong Sát Kỹ Năng cắt đứt mạch ma tố Tham Lam, giải phóng [" + victim.getName().getString() + "]!"),
                            true
                    );
                }
            }
        }

        // CÂN BẰNG SINH TỒN: BẢO HIỂM SINH MỆNH (ONE-SHOT PROTECTION) & DAMAGE CAP CHO NGƯỜI CHƠI
        if (victim instanceof ServerPlayer playerVictim
                && !playerVictim.getTags().contains("OneShotHandled")
                && !source.is(DamageTypes.GENERIC_KILL)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && amount > 0.0F) {
            float maxHp = playerVictim.getMaxHealth();
            float curHp = playerVictim.getHealth();

            // Giới hạn sát thương tối đa trong 1 hit của Boss: 65% Max HP
            float cappedAmount = Math.min(amount, Math.max(10.0F, maxHp * 0.65F));

            // Bảo hiểm sinh mệnh: Nếu trên 50% máu mà nhận đòn chí mạng chết luôn
            if (curHp >= maxHp * 0.50F && cappedAmount >= curHp) {
                float survivableDamage = Math.max(0.0F, curHp - 1.0F); // Giữ lại đúng 1 HP (nửa tim)
                playerVictim.addTag("OneShotHandled");
                try {
                    playerVictim.hurt(source, survivableDamage);
                } finally {
                    playerVictim.removeTag("OneShotHandled");
                }
                playerVictim.invulnerableTime = 25; // 1.25s bất tử (I-Frames)
                playerVictim.level().playSound(null, playerVictim.getX(), playerVictim.getY(), playerVictim.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.6F);
                playerVictim.level().playSound(null, playerVictim.getX(), playerVictim.getY(), playerVictim.getZ(),
                        SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.8F, 1.2F);
                if (playerVictim.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, playerVictim.getX(), playerVictim.getY() + 1.0D, playerVictim.getZ(), 35, 0.5D, 0.8D, 0.5D, 0.1D);
                }
                SageManager.sendCornerAlert(playerVictim, "« Báo cáo »: Đã kích hoạt Bảo Hiểm Sinh Mệnh! Đòn đánh cực hạn đã bị triệt tiêu ở 1 HP!", 0xFFEF4444, 4500, false);
                return EventResult.interruptFalse();
            } else if (cappedAmount < amount) {
                playerVictim.addTag("OneShotHandled");
                try {
                    playerVictim.hurt(source, cappedAmount);
                } finally {
                    playerVictim.removeTag("OneShotHandled");
                }
                return EventResult.interruptFalse();
            }
        }

        // Phản xạ đánh tay (Sidestep / Counter) khi Mob/Boss bị tấn công
        if (victim instanceof net.minecraft.world.entity.Mob defenderMob
                && source.getEntity() instanceof LivingEntity attackerLiving
                && !attackerLiving.getTags().contains(MobMeleeSkillSystem.TAG_CQC_ACTIVE)) {
            MobMeleeSkillSystem.onMobAttackedBy(defenderMob, attackerLiving);
        }

        // Phản Kiếm Thánh Quang & Bộ Thánh Giáp Hinata (Huyền Thoại)
        if (victim instanceof ServerPlayer hinataSp
                && !hinataSp.getTags().contains("HinataArmorMitigated")
                && !source.is(DamageTypes.GENERIC_KILL)
                && amount > 0.0F) {
            long parryUntil = EntityDataHelper.getCustomData(hinataSp).getLong(com.minhphuc.weapons.content.divine.HinataSwordItem.KEY_PARRY_UNTIL);
            if (hinataSp.serverLevel().getGameTime() <= parryUntil
                    && hinataSp.getMainHandItem().getItem() instanceof com.minhphuc.weapons.content.divine.HinataSwordItem) {
                EntityDataHelper.getCustomData(hinataSp).putLong(com.minhphuc.weapons.content.divine.HinataSwordItem.KEY_PARRY_UNTIL, 0L);
                MobMeleeSkillSystem.sendCqc3d(hinataSp.serverLevel(), hinataSp, hinataSp.position(), hinataSp.getLookAngle(), 6, 1, 1.8F, 18);
                hinataSp.serverLevel().playSound(null, hinataSp.getX(), hinataSp.getY(), hinataSp.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.6F, 1.4F);
                if (source.getEntity() instanceof LivingEntity parryTarget && parryTarget != hinataSp) {
                    Vec3 pDir = parryTarget.position().subtract(hinataSp.position()).normalize();
                    MobMeleeSkillSystem.sendCqc3d(hinataSp.serverLevel(), hinataSp, parryTarget.position(), pDir, 0, 1, 1.8F, 16);
                    parryTarget.invulnerableTime = 0;
                    parryTarget.addTag("DisintegrationDamage");
                    try {
                        parryTarget.hurt(hinataSp.damageSources().indirectMagic(hinataSp, hinataSp), 120.0F);
                    } finally {
                        parryTarget.removeTag("DisintegrationDamage");
                    }
                }
                hinataSp.displayClientMessage(Component.literal("§6§l[PHẢN KIẾM THÁNH QUANG] §fVô hiệu hóa đòn & phản chém §c120 DMG§f!"), true);
                return EventResult.interruptFalse();
            }

            float mult = 1.0F;
            boolean isMelee = source.getDirectEntity() instanceof LivingEntity;
            if (com.minhphuc.weapons.content.divine.HinataArmorItem.hasHelmet(hinataSp) && isMelee) {
                mult *= 0.75F; // #1 Mắt Dự Đoán: -25% cận chiến
            }
            if (com.minhphuc.weapons.content.divine.HinataArmorItem.hasChestplate(hinataSp)) {
                mult *= 0.45F; // #2 Thánh Y Bất Hoại: -55% sát thương
            }
            if (com.minhphuc.weapons.content.divine.HinataArmorItem.isWearingFullSet(hinataSp)
                    && hinataSp.isBlocking()
                    && hinataSp.getMainHandItem().getItem() instanceof com.minhphuc.weapons.content.divine.HinataSwordItem) {
                mult *= 0.25F; // #5 Đủ bộ 4 món đỡ kiếm: -75%
            }
            if (mult < 0.99F) {
                hinataSp.addTag("HinataArmorMitigated");
                try {
                    hinataSp.hurt(source, amount * mult);
                } finally {
                    hinataSp.removeTag("HinataArmorMitigated");
                }
                return EventResult.interruptFalse();
            }
        }

        // Bộ Trang Bị Huyền Thoại Ác Ma Thủy Tổ (Primordial Armor):
        // - Kháng lửa, dung nham & miễn sát thương rơi.
        // - Phòng thủ yếu hơn Giáp Thần Thoại (Divine Armor ~100%) đúng 2 lần -> Đủ bộ 4 món giảm 50% sát thương!
        if (victim instanceof ServerPlayer primArmorSp
                && PrimordialArmorItem.isWearingAnyPiece(primArmorSp)
                && !primArmorSp.getTags().contains("PrimordialArmorMitigated")
                && !source.is(DamageTypes.GENERIC_KILL)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && amount > 0.0F) {
            if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_FIRE)
                    || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)
                    || source.is(DamageTypes.HOT_FLOOR)) {
                primArmorSp.clearFire();
                return EventResult.interruptFalse();
            }
            float reduction = PrimordialArmorItem.getDamageReductionFraction(primArmorSp);
            if (reduction > 0.01F) {
                primArmorSp.addTag("PrimordialArmorMitigated");
                try {
                    primArmorSp.hurt(source, amount * (1.0F - reduction));
                } finally {
                    primArmorSp.removeTag("PrimordialArmorMitigated");
                }
                return EventResult.interruptFalse();
            }
        }

        // Ma Tố Quá Tải: +50% sát thương phép xuyên giáp cho người chơi
        if (source.getEntity() instanceof ServerPlayer overdriveSp
                && overdriveSp != victim
                && MagiculesManager.isMagiculeOverdriveActive(overdriveSp)
                && !victim.getTags().contains("MagiculeOverdriveHit")
                && amount > 0.5F) {
            victim.addTag("MagiculeOverdriveHit");
            try {
                victim.invulnerableTime = 0;
                victim.hurt(overdriveSp.damageSources().indirectMagic(overdriveSp, overdriveSp), amount * 0.50F);
            } finally {
                victim.removeTag("MagiculeOverdriveHit");
            }
        }

        // Hành Lang Linh Hồn: Thuộc hạ đồng minh được chia sẻ Quá Tải gây +40% sát thương phép
        if (source.getEntity() instanceof LivingEntity allyAttacker
                && allyAttacker != victim
                && allyAttacker.getTags().contains(MagiculesManager.TAG_SOUL_CORRIDOR_OVERDRIVE)
                && !victim.getTags().contains("SoulCorridorHit")
                && amount > 0.5F) {
            victim.addTag("SoulCorridorHit");
            try {
                victim.invulnerableTime = 0;
                victim.hurt(allyAttacker.damageSources().magic(), amount * 0.40F);
            } finally {
                victim.removeTag("SoulCorridorHit");
            }
        }

        // Khiên Ma Tố Chuyển Hóa: Hấp thụ sát thương vào thanh Ma Tố (100 MP = 1 DMG)
        if (victim instanceof ServerPlayer aegisSp
                && !aegisSp.getTags().contains("MagiculeAegisPartial")
                && MagiculesManager.isMagiculeAegisActive(aegisSp)
                && !source.is(DamageTypes.GENERIC_KILL)
                && amount > 0.0F) {
            boolean urielBypassed = aegisSp.getTags().contains("KuboPenetrationDamage")
                    || aegisSp.getTags().contains("ChronoaPenetrationDamage")
                    || aegisSp.getTags().contains("DisintegrationDamage")
                    || aegisSp.getTags().contains("VelgryndPenetrationDamage")
                    || aegisSp.getTags().contains("VelzardPenetrationDamage")
                    || aegisSp.getTags().contains("UrielBypassDamage")
                    || source.getEntity() instanceof com.minhphuc.weapons.entity.tensura.ChaosDragonEntity
                    || source.getDirectEntity() instanceof com.minhphuc.weapons.entity.tensura.ChaosDragonEntity
                    || source.getEntity() instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity
                    || source.getDirectEntity() instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity
                    || source.getEntity() instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity
                    || source.getDirectEntity() instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity;
            if ((source.getEntity() instanceof com.minhphuc.weapons.entity.tensura.ChaosDragonEntity || aegisSp.getTags().contains("UrielBypassDamage")) && aegisSp.tickCount % 20 == 0) {
                aegisSp.displayClientMessage(
                        Component.literal("§c§l« CẢNH BÁO KHẨN CẤP RAPHAEL »\n§cNăng lượng Hỗn Độn phá hủy quy luật chiều không gian! Phòng Ngự Tuyệt Đối Uriel đã bị xuyên thủng!"),
                        true
                );
            }
            boolean urielBlocks = com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.isUrielDefenseActive(aegisSp)
                    && !urielBypassed
                    && !source.is(DamageTypes.FELL_OUT_OF_WORLD);
            if (!urielBlocks) {
                float remaining = MagiculesManager.absorbDamageWithMagiculeAegis(aegisSp, amount);
                if (remaining <= 0.01F) {
                    return EventResult.interruptFalse();
                } else if (remaining < amount - 0.01F) {
                    aegisSp.addTag("MagiculeAegisPartial");
                    try {
                        aegisSp.hurt(source, remaining);
                    } finally {
                        aegisSp.removeTag("MagiculeAegisPartial");
                    }
                    return EventResult.interruptFalse();
                }
            }
        }

        boolean isVictimNamed = victim.getTags().contains("TensuraNamedMob") || (victim instanceof PrimordialDemonEntity pde && pde.isNamed());
        boolean isVictimDemonLord = victim.getTags().contains("TensuraDemonLordMob");

        // =========================================================================
        // A. KHÁNG TÍNH KHI ĐƯỢC BAN TÊN HOẶC TIẾN HÓA MA VƯƠNG (BẤT KỲ TRẠNG THÁI NÀO)
        // =========================================================================
        if (isVictimNamed || isVictimDemonLord) {
            // 1. Không chết ngạt/đuối nước (dưới nước lên cạn không ngạt, trên cạn xuống nước không đuối)
            if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.DRY_OUT)) {
                return EventResult.interruptFalse();
            }

            // 2. Creeper khi nổ không chết (miễn nhiễm sát thương vụ nổ)
            if (victim instanceof net.minecraft.world.entity.monster.Creeper) {
                if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
                    return EventResult.interruptFalse();
                }
            }
        }

        // =========================================================================
        // B. KHÁNG TÍNH TỐI THƯỢNG KHI ĐƯỢC CẢ BAN TÊN VÀ TIẾN HÓA MA VƯƠNG
        // =========================================================================
        if (isVictimNamed && isVictimDemonLord) {
            // 1. Vô hiệu hóa cháy, dung nham, nổ, rơi, ngạt tường, va chạm, đói, gai...
            if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)
                    || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.CAMPFIRE)
                    || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)
                    || source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)
                    || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALLING_BLOCK)
                    || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.STARVE)
                    || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                    || source.is(DamageTypes.THORNS)) {
                victim.clearFire();
                return EventResult.interruptFalse();
            }

            // 2. Sát thương xuyên thấu không gian / Tuyệt đối tử vong (Void / Generic Kill)
            if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
                return EventResult.pass();
            }

            // 3. Kiểm tra xem kẻ tấn công có phải là Boss thật mạnh lớn hay không
            Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            boolean isAttackerBoss = false;
            if (attacker instanceof LivingEntity livingAttacker) {
                if (com.minhphuc.weapons.content.divine.DivineSalvationHelper.isBoss(livingAttacker)
                        || livingAttacker.getTags().contains("TensuraDemonLordMob")
                        || livingAttacker instanceof com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity
                        || livingAttacker instanceof com.minhphuc.weapons.entity.tensura.MilimEntity
                        || livingAttacker instanceof com.minhphuc.weapons.entity.tensura.VelgryndEntity
                        || livingAttacker instanceof com.minhphuc.weapons.entity.tensura.VelzardEntity
                        || livingAttacker instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity
                        || livingAttacker instanceof com.minhphuc.weapons.entity.tensura.HinataEntity
                        || livingAttacker instanceof com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity
                        || livingAttacker instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                        || livingAttacker instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                        || livingAttacker instanceof net.minecraft.world.entity.monster.warden.Warden) {
                    isAttackerBoss = true;
                }
            }

            if (isAttackerBoss) {
                return EventResult.pass(); // Cho phép sát thương từ Đại Boss!
            }

            // 4. Kiểm tra xem đòn đánh có phải là Skill / Ma pháp hay không
            boolean isSkillOrMagic = source.is(DamageTypes.MAGIC)
                    || source.is(DamageTypes.INDIRECT_MAGIC)
                    || source.is(DamageTypes.SONIC_BOOM)
                    || source.is(DamageTypes.DRAGON_BREATH)
                    || source.is(DamageTypes.LIGHTNING_BOLT)
                    || victim.getTags().contains("DisintegrationDamage")
                    || victim.getTags().contains("KuboPenetrationDamage")
                    || victim.getTags().contains("ChronoaPenetrationDamage")
                    || victim.getTags().contains("VelgryndPenetrationDamage")
                    || victim.getTags().contains("AzathothErased");

            if (!isSkillOrMagic && attacker instanceof ServerPlayer sp) {
                // Người chơi dùng vũ khí kỹ năng hoặc trạng thái Ma Vương Chân Thức Tỉnh
                boolean isPlayerTrueLord = EntityDataHelper.getCustomData(sp).getBoolean("TensuraTrueDemonLord");
                ItemStack held = sp.getMainHandItem();
                if (isPlayerTrueLord || held.getItem() instanceof com.minhphuc.weapons.content.divine.MoonlightSwordItem
                        || held.getItem() instanceof com.minhphuc.weapons.content.divine.ThienMaSwordItem
                        || held.getItem() instanceof com.minhphuc.weapons.content.tensura.PrimordialPactItem
                        || com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility.canUseTengaiTechniques(sp)) {
                    isSkillOrMagic = true;
                }
            }

            if (isSkillOrMagic) {
                return EventResult.pass(); // Cho phép sát thương từ Skill của Ác Ma / Người dùng skill!
            }

            // 5. Nếu KHÔNG PHẢI Boss và KHÔNG PHẢI Skill -> VÔ HIỆU HÓA HOÀN TOÀN MỌI ĐÒN ĐÁNH VẬT LÝ!
            if (victim.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 1.5F);
            }
            if (attacker instanceof Player p) {
                p.displayClientMessage(
                        Component.literal("§c§l[MA TỐ BẤT HOẠI] §7Sinh vật này sở hữu thể xác Chân Ma Vương Danh Xưng, miễn nhiễm hoàn toàn mọi đòn đánh vật lý thường!"),
                        true
                );
            }
            return EventResult.interruptFalse();
        }

        // 1. Phù thủy, Kẻ cướp (Raider) hy sinh triệu hồi ác ma khi máu còn dưới 20% (trừ khi đã thành Ma Vương, Dân làng tuyệt đối không triệu hồi)
        if (!victim.getTags().contains("TensuraDemonLordMob") && !(victim instanceof net.minecraft.world.entity.npc.Villager) &&
            (victim instanceof net.minecraft.world.entity.monster.Witch ||
             victim instanceof net.minecraft.world.entity.raid.Raider)) {

            // KHÁNG MA KẾT GIỚI: Không thể triệu hồi Ác Ma khi ở trong kết giới
            if (victim.level() instanceof ServerLevel sl && com.minhphuc.weapons.content.tensura.AntiMagicBarrierManager.isInBarrier(sl, victim.position())) {
                return EventResult.pass();
            }

            float currentHp = victim.getHealth();
            float maxHp = victim.getMaxHealth();
            if ((currentHp - amount) <= maxHp * 0.20F && !victim.getTags().contains("TensuraSacrificed")) {
                victim.addTag("TensuraSacrificed");

                if (victim.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
                    serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                            SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 1.2F);
                    serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0F, 0.9F);

                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 60, 0.8, 1.2, 0.8, 0.1);
                    serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 40, 0.6, 1.0, 0.6, 0.08);

                    String chant;
                    if (victim instanceof net.minecraft.world.entity.monster.Witch) {
                        chant = "§5§l[Phù Thủy] §c\"Hỡi Ác Ma từ đáy vực thẳm... Ta dâng hiến linh hồn và sinh mạng này, hãy giáng thế nghiền nát kẻ thù của ta!\"";
                    } else {
                        chant = "§4§l[Kẻ Cướp] §c\"Máu này... sinh mạng này dâng trọn cho Bạo Chúa Vực Sâu! Hãy hủy diệt chúng!\"";
                    }

                    for (ServerPlayer p : serverLevel.players()) {
                        if (p.distanceToSqr(victim) <= 40.0 * 40.0) {
                            p.displayClientMessage(Component.literal(chant), false);
                        }
                    }

                    LivingEntity attacker = null;
                    if (source.getEntity() instanceof LivingEntity le) {
                        attacker = le;
                    } else if (source.getDirectEntity() instanceof LivingEntity le) {
                        attacker = le;
                    }

                    // Loại bỏ Ác Ma trùng với loại mà người chơi đang chuyển sinh HOẶC đang giữ Khế Ước trong người
                    java.util.List<DemonType> availableDemons = new java.util.ArrayList<>(java.util.List.of(DemonType.values()));
                    availableDemons.removeIf(dType -> PrimordialPactItem.isDemonContractedInAnyPlayerInventory(serverLevel, dType));
                    for (ServerPlayer sp : serverLevel.players()) {
                        DemonType playerType = PrimordialPlayerDataHelper.getPrimordialType(sp);
                        if (playerType != null) {
                            availableDemons.remove(playerType);
                        }
                    }
                    if (availableDemons.isEmpty()) {
                        // Toàn bộ Ác Ma đều đang có Khế Ước trong người chơi -> Sinh vật không thể triệu hồi chúng được nữa!
                        return EventResult.pass();
                    }
                    DemonType randomDemon = availableDemons.get(serverLevel.random.nextInt(availableDemons.size()));

                    PrimordialSummonRitual.startImmediate(serverLevel, victim.position(), randomDemon, attacker);

                    victim.discard();
                    return EventResult.interruptFalse();
                }
            }
        }



        // XỬ LÝ SÁT THƯƠNG KHI NHẬP XÁC ÁC MA THỦY TỔ (BẢO VỆ PLAYER KHÔNG BỊ CHẾT)
        if (victim instanceof ServerPlayer player && PrimordialPossessionManager.isPossessing(player)) {
            if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL) || source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)) {
                return EventResult.interruptFalse();
            }
            if (amount >= player.getHealth()) {
                // Thân xác ác ma tử trận! Người chơi thoát xác an toàn về bản thể
                PrimordialPossessionManager.stopPossession(player, true);
                return EventResult.interruptFalse();
            }
        }

        if (victim instanceof ServerPlayer player && PrimordialPlayerDataHelper.isInnatePrimordial(player)) {
            // Miễn nhiễm hoàn toàn sát thương ngã (Fall damage)
            if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                return EventResult.interruptFalse();
            }

            boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(player);
            boolean hasBody = PrimordialPlayerDataHelper.hasPhysicalBody(player);

            // GIAI ĐOẠN 3: Đã là Ma Vương + có thể xác -> BẤT TỬ TUYỆT ĐỐI, CHỈ chịu sát thương từ Chước Nhiệt Long Velgrynd, Bạch Băng Long Velzard & Không Vong (Kūbō)
            if (isDemonLord && hasBody) {
                boolean isFromVelgrynd = (source.getEntity() instanceof VelgryndEntity) || (source.getDirectEntity() instanceof VelgryndEntity)
                        || player.getTags().contains("VelgryndPenetrationDamage");
                boolean isFromVeldora = (source.getEntity() instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity)
                        || (source.getDirectEntity() instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity)
                        || player.getTags().contains("VeldoraPenetrationDamage");
                boolean isFromVelzard = (source.getEntity() instanceof VelzardEntity) || (source.getDirectEntity() instanceof VelzardEntity)
                        || player.getTags().contains("VelzardPenetrationDamage");
                boolean isFromKubo = (source.getEntity() instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity)
                        || (source.getDirectEntity() instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity)
                        || player.getTags().contains("KuboPenetrationDamage");
                boolean isFromChronoa = (source.getEntity() instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity)
                        || (source.getDirectEntity() instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity)
                        || player.getTags().contains("ChronoaPenetrationDamage");
                if (!isFromVelgrynd && !isFromVelzard && !isFromVeldora && !isFromKubo && !isFromChronoa) {
                    return EventResult.interruptFalse();
                }
            } else if (!hasBody) {
                // GIAI ĐOẠN 1: Ác ma linh thể (chưa có thể xác)
                // Các sinh vật bình thường KHÔNG THỂ gây sát thương cho người chơi.
                // NGOẠI LỆ ĐƯỢC PHÉP GÂY SÁT THƯƠNG:
                // 1. Creeper
                // 2. Tự nổ/tự đánh bản thân
                // 3. Ravager (trâu của kẻ cắp)
                // 4. Các Ác ma khác (PrimordialDemonEntity)
                // 5. Boss (Wither, Warden, Ender Dragon, Iron Golem, Elder Guardian)
                // 6. Chước Nhiệt Long (VelgryndEntity)
                // 7. Không Vong (KuboEntity)
                Entity attacker = source.getEntity();
                Entity direct = source.getDirectEntity();

                boolean isCreeper = (attacker instanceof net.minecraft.world.entity.monster.Creeper) || (direct instanceof net.minecraft.world.entity.monster.Creeper);
                boolean isSelf = (attacker == player) || (direct == player);
                boolean isRavager = (attacker instanceof net.minecraft.world.entity.monster.Ravager) || (direct instanceof net.minecraft.world.entity.monster.Ravager);
                boolean isDemon = (attacker instanceof PrimordialDemonEntity) || (direct instanceof PrimordialDemonEntity);
                boolean isBoss = (attacker instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                        || (attacker instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                        || (attacker instanceof net.minecraft.world.entity.monster.warden.Warden)
                        || (attacker instanceof net.minecraft.world.entity.animal.IronGolem)
                        || (attacker instanceof net.minecraft.world.entity.monster.ElderGuardian);
                boolean isVelgrynd = (attacker instanceof VelgryndEntity) || (direct instanceof VelgryndEntity);
                boolean isKubo = (attacker instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity)
                        || (direct instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity)
                        || player.getTags().contains("KuboPenetrationDamage");
                boolean isChronoa = (attacker instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity)
                        || (direct instanceof com.minhphuc.weapons.entity.tensura.ChronoaEntity)
                        || player.getTags().contains("ChronoaPenetrationDamage");

                if (!isCreeper && !isSelf && !isRavager && !isDemon && !isBoss && !isVelgrynd && !isKubo && !isChronoa) {
                    // Chặn triệt để sát thương từ quái vật/sinh vật thường
                    return EventResult.interruptFalse();
                }
            }
            // Giai đoạn 2 (có thể xác nhưng chưa là Ma Vương): Quái thường có thể gây sát thương bình thường
        }

        // Sát thương cận chiến khi Người chơi đang Nhập Xác Ác Ma Thủy Tổ
        if (!HANDLING_PRIMORDIAL_HURT.get()
                && source.getEntity() instanceof ServerPlayer attackerPlayer
                && PrimordialPossessionManager.isPossessing(attackerPlayer)
                && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            boolean hasBody = PrimordialPossessionManager.hasPhysicalBody(attackerPlayer);
            DemonType type = PrimordialPossessionManager.getPossessedType(attackerPlayer);

            boolean isBoss = (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                    || (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                    || (victim instanceof net.minecraft.world.entity.monster.warden.Warden)
                    || (victim instanceof net.minecraft.world.entity.animal.IronGolem)
                    || (victim instanceof net.minecraft.world.entity.monster.ElderGuardian);

            float atkDamage = hasBody ? 160.0F * 10.0F : 160.0F;
            if (type == DemonType.NOIR && victim.level() instanceof ServerLevel sl) {
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 0.7F);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
            }
            float targetDmg = isBoss ? Math.max(amount, victim.getMaxHealth() * 0.25F) : Math.max(amount, atkDamage);
            if (amount < targetDmg) {
                try {
                    HANDLING_PRIMORDIAL_HURT.set(true);
                    victim.hurt(attackerPlayer.damageSources().mobAttack(attackerPlayer), targetDmg);
                } finally {
                    HANDLING_PRIMORDIAL_HURT.set(false);
                }
                return EventResult.interruptFalse();
            }
        }

        // =========================================================================
        // 3A. CƠ CHẾ VUỐT TUYỆT VỌNG CỦA BỘ ĐỒ NOIR (DIABLO - PHÍM X): ĐÁNH TAY KHÔNG X2 SÁT THƯƠNG
        // =========================================================================
        if (!HANDLING_PRIMORDIAL_HURT.get()
                && source.getEntity() instanceof ServerPlayer clawAttacker
                && !PrimordialPossessionManager.isPossessing(clawAttacker)
                && PrimordialArmorItem.isWearingAnyNoirArmor(clawAttacker)
                && clawAttacker.getMainHandItem().isEmpty()
                && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            boolean clawsActive = PrimordialArmorItem.isNoirClawsActive(clawAttacker);
            boolean fullNoirSet = PrimordialArmorItem.isWearingFullSet(clawAttacker, DemonType.NOIR);

            if (clawsActive || fullNoirSet) {
                if (clawsActive && victim.level() instanceof ServerLevel sl) {
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 0.7F);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1.0D, victim.getZ(),
                            4, 0.3D, 0.3D, 0.3D, 0.0D);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + 1.0D, victim.getZ(),
                            12, 0.35D, 0.4D, 0.35D, 0.05D);
                }

                // Sát thương cơ bản tay không của Giáp Thủy Tổ Huyền Thoại = 80.0F (yếu hơn Nhập Xác 160.0F đúng 2 lần).
                // Khi bật Vuốt Noir bằng phím [X]: Sát thương đánh tay không bằng vuốt mạnh hơn gấp 2 lần (x2.0 -> 160.0F)!
                float baseUnarmed = fullNoirSet ? 80.0F : 40.0F;
                float clawMult = clawsActive ? 2.0F : 1.0F;
                float targetDmg = Math.max(amount * clawMult, baseUnarmed * clawMult);

                if (amount < targetDmg) {
                    try {
                        HANDLING_PRIMORDIAL_HURT.set(true);
                        victim.hurt(clawAttacker.damageSources().mobAttack(clawAttacker), targetDmg);
                    } finally {
                        HANDLING_PRIMORDIAL_HURT.set(false);
                    }
                    return EventResult.interruptFalse();
                }
            }
        }

        // =========================================================================
        // 3B. CƠ CHẾ SÁT THƯƠNG CỦA NGƯỜI CHƠI THỦY TỔ ÁC MA KHI TẤN CÔNG (ATTACKER)
        // =========================================================================
        if (!HANDLING_PRIMORDIAL_HURT.get()
                && source.getEntity() instanceof ServerPlayer attackerPlayer
                && PrimordialPlayerDataHelper.isInnatePrimordial(attackerPlayer)
                && !PrimordialPossessionManager.isPossessing(attackerPlayer)
                && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(attackerPlayer);
            boolean hasBody = PrimordialPlayerDataHelper.hasPhysicalBody(attackerPlayer);

            boolean isBoss = (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                    || (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                    || (victim instanceof net.minecraft.world.entity.monster.warden.Warden)
                    || (victim instanceof net.minecraft.world.entity.animal.IronGolem)
                    || (victim instanceof net.minecraft.world.entity.monster.ElderGuardian);

            if (hasBody || isDemonLord) {
                // Giai đoạn 2 & 3: Cực mạnh - chỉ cần 2 lần tấn công là kết liễu Boss!
                if (isBoss) {
                    float halfBossHp = victim.getMaxHealth() * 0.52F;
                    if (amount < halfBossHp) {
                        victim.hurt(attackerPlayer.damageSources().magic(), halfBossHp);
                        return EventResult.interruptFalse();
                    }
                } else if (!(victim instanceof VelgryndEntity) && !(victim instanceof PrimordialDemonEntity)) {
                    // Quái thường lập tức tan biến
                    victim.hurt(attackerPlayer.damageSources().magic(), victim.getMaxHealth() * 3.0F);
                    return EventResult.interruptFalse();
                }
            } else {
                // Giai đoạn 1 (Linh thể): Mạnh hơn quái thường & Người Sắt (~50 sát thương), nhưng yếu hơn Boss
                if (!isBoss && !(victim instanceof VelgryndEntity) && !(victim instanceof PrimordialDemonEntity)) {
                    if (amount < 48.0F) {
                        victim.hurt(attackerPlayer.damageSources().magic(), 48.0F);
                        return EventResult.interruptFalse();
                    }
                }
            }
        }

        // =========================================================================
        // 4. MA VƯƠNG: ĐẤM THƯỜNG BỘC PHÁ SIÊU THANH & THỔI BAY KẺ ĐỊCH NHƯ MILIM
        // =========================================================================
        Entity rawAttacker = source.getEntity();
        if (rawAttacker instanceof ServerPlayer demonLordPlayer && !victim.getTags().contains("DemonLordExplosivePunch")) {
            boolean isTrueDemonLord = com.minhphuc.weapons.data.EntityDataHelper.getCustomData(demonLordPlayer).getBoolean("TensuraTrueDemonLord");
            boolean isPrimordialLord = com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.isDemonLord(demonLordPlayer);

            if (isTrueDemonLord || isPrimordialLord) {
                // Kiểm tra loại sát thương là đòn đánh trực tiếp của người chơi
                if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) || source.getDirectEntity() == demonLordPlayer) {
                    if (victim.level() instanceof ServerLevel sl) {
                        victim.addTag("DemonLordExplosivePunch");

                        // 1. ÂM THANH BỘC PHÁ UY LỰC MILIM
                        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 1.2F);
                        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5F, 1.6F);
                        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.8F);

                        // 2. HẠT BÙNG NỔ & SÓNG XUNG KÍCH SIÊU THANH
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(), 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, victim.getX(), victim.getY() + 0.8D, victim.getZ(), 2, 0.2D, 0.2D, 0.2D, 0);
                        sl.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0.1D, 0.1D, 0.1D, 0);
                        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, victim.getX(), victim.getY() + 0.5D, victim.getZ(), 25, 0.8D, 0.8D, 0.8D, 0.05D);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + 0.5D, victim.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.08D);
                        sl.sendParticles(ParticleTypes.DRAGON_BREATH, victim.getX(), victim.getY() + 0.8D, victim.getZ(), 35, 0.7D, 0.7D, 0.7D, 0.1D);

                        // 3. THỔI BAY MOB ĐI CỰC XA NHƯ CÚ ĐẤM MILIM
                        Vec3 punchDir = victim.position().subtract(demonLordPlayer.position());
                        if (punchDir.lengthSqr() < 0.001D) {
                            punchDir = demonLordPlayer.getLookAngle();
                        }
                        punchDir = punchDir.normalize();

                        // Hất văng cực mạnh với vận tốc 3.5 khối/tick và tung lên không trung
                        victim.setDeltaMovement(punchDir.scale(3.5D).add(0, 1.35D, 0));
                        victim.hasImpulse = true;

                        // 4. SÁT THƯƠNG NỔ CHẤN ĐỘNG & BỘC PHÁ DIỆN RỘNG (AOE) CHO CÁC QUÁI XUNG QUANH
                        AABB splashBox = victim.getBoundingBox().inflate(5.0D);
                        List<LivingEntity> nearby = sl.getEntitiesOfClass(
                                LivingEntity.class, splashBox,
                                e -> e != victim && e != demonLordPlayer && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        );
                        for (LivingEntity nearbyMob : nearby) {
                            Vec3 splashDir = nearbyMob.position().subtract(victim.position());
                            if (splashDir.lengthSqr() < 0.001D) splashDir = punchDir;
                            splashDir = splashDir.normalize();

                            nearbyMob.setDeltaMovement(splashDir.scale(2.0D).add(0, 0.8D, 0));
                            nearbyMob.hasImpulse = true;
                            nearbyMob.addTag("DemonLordExplosivePunch");
                            nearbyMob.hurt(demonLordPlayer.damageSources().explosion(demonLordPlayer, demonLordPlayer), 60.0F);
                            nearbyMob.removeTag("DemonLordExplosivePunch");
                        }

                        victim.removeTag("DemonLordExplosivePunch");
                    }
                }
            }
        }

        // =========================================================================
        // 5. CỘNG DỒN SÁT THƯƠNG PHẨM CẤP VŨ KHÍ (WEAPON GRADE EVOLUTION)
        // =========================================================================
        if (!HANDLING_PRIMORDIAL_HURT.get() && rawAttacker instanceof LivingEntity weaponUser && !victim.getTags().contains("WeaponGradeBonusHit")) {
            ItemStack mainHand = weaponUser.getMainHandItem();
            float gradeBonus = com.minhphuc.weapons.content.tensura.WeaponGradeHelper.getBonusDamage(mainHand, amount) - amount;
            if (gradeBonus >= 3.0F && !source.is(DamageTypes.MAGIC)) {
                victim.addTag("WeaponGradeBonusHit");
                try {
                    victim.hurt(weaponUser.damageSources().magic(), gradeBonus);
                } finally {
                    victim.removeTag("WeaponGradeBonusHit");
                }
            }
        }
        return EventResult.pass();
    }

    public static EventResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player instanceof ServerPlayer sp) {
            // Kiểm tra rương tự nhiên để chèn Sách Cổ Khởi Nguyên Thủy Tổ
            TomeLootManager.onOpenContainer(sp, pos);

            // 0. Tương tác Bệ Quản Trị Thành Trì Jura Tempest (Ngai Vàng hoặc Đấu Trường)
            if (hand == InteractionHand.MAIN_HAND
                    && JuraTempestCitadelBuilder.isInsideCitadelOrDimension(sp.level(), sp.position())
                    && JuraTempestCitadelBuilder.isPodiumBlock(
                            JuraTempestCitadelBuilder.getCitadelCenter(sp.serverLevel(), sp.getUUID()), pos)) {
                JuraTempestManager.openCitadelGui(sp);
                return EventResult.interruptFalse();
            }

            // 0.1. Tương tác La Bàn Thần Môn (Chuột phải đổi số cổng / Shift+Chuột phải mở GUI 0-9) khi đứng ngay cạnh Cổng 3D
            if (hand == InteractionHand.MAIN_HAND
                    && DimensionalSanctuaryAbility.tryInteractWithGateBlockClick(sp, pos, sp.isShiftKeyDown())) {
                return EventResult.interruptFalse();
            }

            // 0.2. Kích hoạt Cổng Ác Ma Giới Khổng Lồ (Gothic Demonic Gate)
            if (DemonicGateManager.onRightClickBlock(sp, hand, pos, direction)) {
                return EventResult.interruptFalse();
            }

            // 1. Luôn ưu tiên hiến tế pháp trận triệu hồi ác ma trước
            if (PrimordialSummonRitual.offerSacrifice(sp, hand, Vec3.atCenterOf(pos))) {
                return EventResult.interruptFalse();
            }
        }
        return EventResult.pass();
    }

    public static EventResult onInteractEntity(Player player, Entity entity, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            // Khi ở chế độ Khán Giả click vào Ác ma đã thuần phục của mình -> Bắt đầu Nhập Xác
            if (entity instanceof PrimordialDemonEntity demon && demon.isAlive() && demon.isTame() && demon.isOwnedBy(sp)) {
                if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
                    PrimordialPossessionManager.startPossession(sp, demon);
                    return EventResult.interruptTrue();
                }
            }

            if (PrimordialSummonRitual.offerSacrifice(sp, hand, entity.position())) {
                return EventResult.interruptFalse();
            }

            // Click trúng sinh vật để tiến hóa thành Ma Vương khi đủ 64 Linh Hồn
            if (hand == InteractionHand.MAIN_HAND && entity instanceof LivingEntity target && target.isAlive() && !(target instanceof Player)) {
                ItemStack held = sp.getItemInHand(hand);

                // Cầm Xương hoặc Khung Xương Ma Thiết click vào sinh vật KHÔNG PHẢI Ác Ma
                if ((held.is(net.minecraft.world.item.Items.BONE) || held.is(ModItems.ARTIFICIAL_SKELETON.get()))
                        && !(target instanceof PrimordialDemonEntity)
                        && !(target instanceof UnderworldDemonEntity)) {
                    if (held.is(ModItems.ARTIFICIAL_SKELETON.get())) {
                        sp.displayClientMessage(Component.literal("§c§l✦ Sinh vật này không phải Ác Ma! Xương / Khung Xương Ma Thiết chỉ dùng để ban thể xác vật lý cho Ác Ma!"), true);
                        return EventResult.interruptFalse();
                    }
                    // Với Xương thường (Items.BONE) dùng cho chó sói bình thường thì pass
                }

                boolean isHoldingSoul = held.is(ModItems.MOB_SOUL.get()) || held.is(ModItems.DEMON_LORD_SOUL.get());
                int totalSouls = countTotalSouls(sp);

                // Nếu click vào Dân Làng / Thương Nhân khi đang giữ Shift (mà KHÔNG cầm Linh Hồn trên tay) -> Nhường cho tính năng Trao Đổi (Trade GUI)
                if (target instanceof net.minecraft.world.entity.npc.AbstractVillager && sp.isShiftKeyDown() && !isHoldingSoul) {
                    return EventResult.pass();
                }

                if (isHoldingSoul || sp.isShiftKeyDown()) {
                    if (totalSouls >= 64) {
                        if (target instanceof MilimEntity
                                || target instanceof VelgryndEntity
                                || target instanceof VelzardEntity
                                || target instanceof com.minhphuc.weapons.entity.tensura.VeldoraEntity) {
                            VoiceOfTheWorld.announce(sp, "Báo cáo: Cá thể thuộc dòng dõi Tối Cổ / Long Chủng, ma tố đã vượt qua cảnh giới Ma Vương, không thể cưỡng ép thức tỉnh!");
                            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                            return EventResult.interruptTrue();
                        }

                        if (target.getTags().contains("TensuraDemonLordMob")) {
                            sp.displayClientMessage(Component.literal("§c⚠️ Sinh vật này đã thức tỉnh thành Ma Vương rồi!"), true);
                            return EventResult.interruptTrue();
                        }

                        if (consume64Souls(sp)) {
                            evolveToDemonLord(sp, target);
                            return EventResult.interruptTrue();
                        }
                    } else if (isHoldingSoul) {
                        sp.displayClientMessage(Component.literal("§c⚠️ Cần đủ 64 Linh Hồn để tiến hóa Ma Vương! (Hiện có: " + totalSouls + "/64)"), true);
                    }
                }
            }
        }
        return EventResult.pass();
    }

    public static int countTotalSouls(ServerPlayer player) {
        int invSouls = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.MOB_SOUL.get()) || stack.is(ModItems.DEMON_LORD_SOUL.get())) {
                invSouls += stack.getCount();
            }
        }
        CompoundTag data = EntityDataHelper.getCustomData(player);
        int nbtSouls = data.getInt("TensuraCollectedSouls");
        return invSouls + nbtSouls;
    }

    public static boolean consume64Souls(ServerPlayer player) {
        if (countTotalSouls(player) < 64) return false;
        int needed = 64;

        // 1. Tiêu hao trong túi đồ trước
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.MOB_SOUL.get()) || stack.is(ModItems.DEMON_LORD_SOUL.get())) {
                int take = Math.min(stack.getCount(), needed);
                stack.shrink(take);
                needed -= take;
                if (needed <= 0) break;
            }
        }

        // 2. Tiêu hao trong dữ liệu NBT nếu còn thiếu
        if (needed > 0) {
            CompoundTag data = EntityDataHelper.getCustomData(player);
            int current = data.getInt("TensuraCollectedSouls");
            data.putInt("TensuraCollectedSouls", Math.max(0, current - needed));
        }

        return true;
    }

    public static void evolveToDemonLord(ServerPlayer player, LivingEntity target) {
        ServerLevel sl = (ServerLevel) target.level();

        // 1. Nếu là Không Vong: Tiến hóa thành Ma Vương Không Vong (Cosmic Deity Kuubou)
        if (target instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity kubo) {
            com.minhphuc.weapons.entity.darkgathering.DemonLordKuboEntity lord =
                    com.minhphuc.weapons.entity.ModEntities.DEMON_LORD_KUBO.get().create(sl);
            if (lord != null) {
                lord.moveTo(kubo.getX(), kubo.getY(), kubo.getZ(), kubo.getYRot(), kubo.getXRot());
                sl.addFreshEntity(lord);
                kubo.discard();

                sl.playSound(null, lord.getX(), lord.getY(), lord.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.5F, 0.7F);
                sl.playSound(null, lord.getX(), lord.getY(), lord.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.5F, 0.8F);
                sl.playSound(null, lord.getX(), lord.getY(), lord.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 1.2F);

                for (int i = 0; i < 64; i++) {
                    double angle = Math.toRadians(i * 5.625);
                    double px = lord.getX() + Math.cos(angle) * 3.5;
                    double pz = lord.getZ() + Math.sin(angle) * 3.5;
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, px, lord.getY() + 1.5, pz, 4, 0.1, 0.2, 0.1, 0.05);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, lord.getY() + 1.0, pz, 3, 0.1, 0.1, 0.1, 0.05);
                }
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, lord.getX(), lord.getY() + 1.5, lord.getZ(), 2, 0, 0, 0, 0);

                VoiceOfTheWorld.announce(player, "§5§l[TIẾN HÓA THẦN VỰC] §dKhông Vong đã nuốt trọn 64 Linh Hồn, thức tỉnh thành §5§lMa Vương Không Vong [Thần Hư Không Vũ Trụ]§d!");
                return;
            }
        }

        // 2. Nếu là Ác Ma (Primordial Demon): 100% KIỂM SOÁT ĐƯỢC
        if (target instanceof PrimordialDemonEntity demon) {
            demon.addTag("TensuraDemonLordMob");
            demon.addTag("TensuraDemonLordCalm");
            demon.setEvolutionTier(Math.max(demon.getEvolutionTier(), 2)); // Awakened Demon Lord
            demon.setWinged(true);

            String oldName = demon.getEffectiveDemonName();
            demon.updateAttributesForType(demon.getDemonType());
            demon.setHealth(demon.getMaxHealth());
            demon.updateDemonNametag();
            demon.syncToPactInOwnerInventory();

            sl.playSound(null, demon.getX(), demon.getY(), demon.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 0.8F);
            sl.playSound(null, demon.getX(), demon.getY(), demon.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.2F);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, demon.getX(), demon.getY() + 1.0, demon.getZ(), 2, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, demon.getX(), demon.getY() + 1.0, demon.getZ(), 50, 1.0, 1.5, 1.0, 0.1);

            VoiceOfTheWorld.announce(player, "Báo cáo: Ác Ma Thủy Tổ " + oldName + " đã hấp thụ 64 Linh Hồn, thức tỉnh thành Chân Ma Vương (Awakened Demon Lord - 100% Kiểm Soát) và trung thành tuyệt đối!");
            player.displayClientMessage(
                    Component.literal("§5§l[THỨC TỈNH MA VƯƠNG] §eÁc Ma " + oldName + " §fđã thức tỉnh thành §4§lChân Ma Vương§f! (100% Kiểm soát, Máu & Công kích x20)"),
                    false
            );
            return;
        }

        // 2b. Nếu là Ác Ma Ma Giới (Underworld Demon): 30% kiểm soát được, 70% phản chủ
        if (target instanceof UnderworldDemonEntity uDemon) {
            uDemon.evolveToDemonLord(player);
            return;
        }

        // 3. Đối với MỌI SINH VẬT KHÁC (Mob, Dân Làng, Động Vật):
        // 90% Mất Kiểm Soát (Cuồng Nộ - Berserk) vs 10% Kiểm Soát Được (Calm)
        target.addTag("TensuraDemonLordMob");
        if (target instanceof net.minecraft.world.entity.npc.Villager) {
            target.addTag("DemonLordVillager");
        }
        String oldName = target.getName().getString();

        // Máu tăng x20 lần
        var hpAttr = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(hpAttr.getBaseValue() * 20.0D);
            target.setHealth((float) hpAttr.getValue());
        }

        // Sức mạnh tăng x20 lần
        var atkAttr = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.setBaseValue(Math.max(atkAttr.getBaseValue(), 6.0D) * 20.0D);
        }

        float roll = sl.random.nextFloat();
        boolean berserk = roll < 0.90f; // 90% mất kiểm soát

        if (berserk) {
            target.addTag("TensuraDemonLordBerserk");
            target.setCustomName(Component.literal("§4§l[Ma Vương Cuồng Nộ] §c" + oldName));
            target.setCustomNameVisible(true);

            // Tấn công TẤT CẢ mọi sinh vật khác (bao gồm cả Dân Làng, Player, Mob)
            if (target instanceof net.minecraft.world.entity.Mob mob) {
                com.minhphuc.weapons.mixin.MobAccessor mobAcc = (com.minhphuc.weapons.mixin.MobAccessor) mob;
                if (mob instanceof net.minecraft.world.entity.PathfinderMob pMob) {
                    mobAcc.weapons$getGoalSelector().addGoal(0, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(pMob, 1.55D, true));
                }
                mobAcc.weapons$getTargetSelector().addGoal(0, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                        mob, LivingEntity.class, 0, false, false,
                        e -> e != mob && e.isAlive() && !e.getTags().contains("TensuraDemonLordBerserk")
                ));
            }

            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.5F, 0.6F);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.8F);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + 1.0, target.getZ(), 80, 1.2, 1.5, 1.2, 0.1);

            String alertMsg = "§4§l[THẢM HỌA MA VƯƠNG - 90%] §c" + oldName + " §ckhông dung nạp nổi 64 Linh Hồn, đã rơi vào CUỒNG NỘ MA VƯƠNG (Mất kiểm soát) và đang tàn sát mọi sinh linh xung quanh!";
            for (ServerPlayer p : sl.players()) {
                p.displayClientMessage(Component.literal(alertMsg), false);
            }
            VoiceOfTheWorld.announce(player, "Cảnh báo khẩn cấp: Thức tỉnh thất bại do quá tải ma tố. Thực thể " + oldName + " đã rơi vào trạng thái Cuồng Bạo Ma Vương, mất hoàn toàn kiểm soát!");
        } else {
            // 10% Kiểm soát được (Kỳ tích): Giữ nguyên ý thức, KHÔNG ép phục tùng người chơi
            target.addTag("TensuraDemonLordCalm");
            target.setCustomName(Component.literal("§5§l[Chân Ma Vương] §e" + oldName));
            target.setCustomNameVisible(true);

            if (target instanceof net.minecraft.world.entity.Mob mob) {
                com.minhphuc.weapons.mixin.MobAccessor mobAcc = (com.minhphuc.weapons.mixin.MobAccessor) mob;
                if (mob instanceof net.minecraft.world.entity.PathfinderMob pMob) {
                    mobAcc.weapons$getGoalSelector().addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(pMob, 1.35D, true));
                    mobAcc.weapons$getTargetSelector().addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(pMob));
                }
                if (mob instanceof net.minecraft.world.entity.npc.Villager) {
                    mobAcc.weapons$getTargetSelector().addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob, net.minecraft.world.entity.monster.Zombie.class, true));
                }
            }

            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 0.9F);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.3F);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.5F, 1.0F);

            sl.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.2, 0.2, 0.2, 0);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1.0, target.getZ(), 50, 0.8, 1.2, 0.8, 0.15);
            sl.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.5, 1.0, 0.5, 0.05);

            String miracleMsg = "§6§l[KỲ TÍCH MA VƯƠNG - 10%] §eKỳ tích xảy ra! §f" + oldName + " §eđã dung hợp hoàn mỹ 64 Linh Hồn, giữ vững ý thức và thức tỉnh thành §5§lChân Ma Vương§e!";
            for (ServerPlayer p : sl.players()) {
                p.displayClientMessage(Component.literal(miracleMsg), false);
            }
            VoiceOfTheWorld.announce(player, "Thông báo: Kỳ tích ma tố. Thực thể " + oldName + " đã khống chế thành công ma hạch và thức tỉnh thành Chân Ma Vương!");
        }
    }

    public static void spawnGuyCrimsonOnVillagerDeath(ServerLevel sl, LivingEntity victim) {
        if (PrimordialPactItem.isDemonContractedInAnyPlayerInventory(sl, DemonType.ROUGE)
                || PrimordialPactItem.findActivePrimordial(sl.getServer(), DemonType.ROUGE, null) != null) {
            return;
        }
        PrimordialDemonEntity guy = new PrimordialDemonEntity(com.minhphuc.weapons.entity.ModEntities.PRIMORDIAL_DEMON.get(), sl);
        guy.setDemonType(DemonType.ROUGE);
        guy.setCustomDemonName("Guy Crimson");
        guy.setPhysicalBody(true);
        guy.setWinged(true);
        guy.setEvolutionTier(2); // Awakened Lord
        guy.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        guy.finalizeSpawn(sl, sl.getCurrentDifficultyAt(victim.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
        sl.addFreshEntity(guy);

        sl.playSound(null, guy.getX(), guy.getY(), guy.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 5.0F, 0.8F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, guy.getX(), guy.getY() + 1.0, guy.getZ(), 3, 1.0, 1.0, 1.0, 0);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, guy.getX(), guy.getY() + 1.0, guy.getZ(), 60, 1.2, 2.0, 1.2, 0.1);

        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(guy.position()) <= 96 * 96) {
                p.displayClientMessage(
                        Component.literal("§4§l[THỨC TỈNH THỦY TỔ] §eDân làng Ma Vương ngã xuống đã giải phóng Thủy Tổ Xích Sắc: §4§lGuy Crimson§e!"),
                        false
                );
            }
        }
    }
}
