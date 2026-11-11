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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleManager;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.entity.tensura.VelzardEntity;
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
            }
            return EventResult.pass();
        });
        InteractionEvent.INTERACT_ENTITY.register(TensuraEvents::onInteractEntity);
        dev.architectury.event.events.common.PlayerEvent.PLAYER_JOIN.register(TensuraEvents::onPlayerJoin);
        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(player -> DEMON_GREETING_TIMESTAMPS.remove(player.getUUID()));
        dev.architectury.event.events.common.TickEvent.PLAYER_POST.register(TensuraEvents::onPlayerTick);
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(level -> {
            JaunePlayerSkillManager.tick(level);
            CarreraBulletLogic.tickVortices();
            PrimordialSummonRitual.tickRituals(level);
            VelgryndSummonRitual.tickRituals(level);
            IncubationCapsuleManager.tickCapsules(level);
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
            com.minhphuc.weapons.content.tensura.PrimordialPossessionManager.tickDomains(level);
            com.minhphuc.weapons.content.tensura.ApocalypseAbility.tickApocalypses(level);
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
                sp.onUpdateAbilities();
            }
            // Hồi phục sinh lực ma tộc 10 HP/s
            if (sp.tickCount % 20 == 0 && sp.getHealth() < sp.getMaxHealth()) {
                sp.heal(10.0F);
            }
        }

        // 1. CƠ CHẾ BAY (FLIGHT) CHO NGƯỜI CHƠI LÀ ÁC MA HOẶC MA VƯƠNG
        boolean canFlyTensura = PrimordialPlayerDataHelper.isPrimordial(sp) || PrimordialPlayerDataHelper.isDemonLord(sp);
        if (canFlyTensura) {
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
                sp.onUpdateAbilities();
            }
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

        // Nếu người chơi chết: Reset toàn bộ thân phận Thủy Tổ Ác Ma & Kỹ Năng Tiến Hóa theo luật chơi
        if (victim instanceof ServerPlayer deadPlayer) {
            com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.resetOnDeath(deadPlayer);
            if (PrimordialPlayerDataHelper.isPrimordial(deadPlayer) || PrimordialPlayerDataHelper.isDemonLord(deadPlayer)) {
                PrimordialPlayerDataHelper.resetOnDeath(deadPlayer);
                if (!deadPlayer.isCreative() && !deadPlayer.isSpectator()) {
                    deadPlayer.getAbilities().mayfly = false;
                    deadPlayer.getAbilities().flying = false;
                    deadPlayer.onUpdateAbilities();
                }
                VoiceOfTheWorld.announce(deadPlayer, "§c§l[TỬ TRẬN] §4Báo cáo. Cá thể đã tử trận! Toàn bộ căn nguyên Thủy Tổ Ác Ma & Ma Vương đã tan biến.");
            }
            return EventResult.pass();
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
        return EventResult.pass();
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

        // Chặn nếu bị Thần Hư Không Azathoth xóa sổ
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

                    // Loại bỏ Ác Ma trùng với loại mà người chơi trên server đang chuyển sinh thành
                    java.util.List<DemonType> availableDemons = new java.util.ArrayList<>(java.util.List.of(DemonType.values()));
                    for (ServerPlayer sp : serverLevel.players()) {
                        DemonType playerType = PrimordialPlayerDataHelper.getPrimordialType(sp);
                        if (playerType != null) {
                            availableDemons.remove(playerType);
                        }
                    }
                    DemonType randomDemon;
                    if (!availableDemons.isEmpty()) {
                        randomDemon = availableDemons.get(serverLevel.random.nextInt(availableDemons.size()));
                    } else {
                        randomDemon = DemonType.values()[serverLevel.random.nextInt(DemonType.values().length)];
                    }

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

        if (victim instanceof ServerPlayer player && PrimordialPlayerDataHelper.isPrimordial(player)) {
            // Miễn nhiễm hoàn toàn sát thương ngã (Fall damage)
            if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                return EventResult.interruptFalse();
            }

            boolean isDemonLord = PrimordialPlayerDataHelper.isDemonLord(player);
            boolean hasBody = PrimordialPlayerDataHelper.hasPhysicalBody(player);

            // GIAI ĐOẠN 3: Đã là Ma Vương + có thể xác -> BẤT TỬ TUYỆT ĐỐI, CHỈ chịu sát thương từ Chước Nhiệt Long Velgrynd & Không Vong (Kūbō)
            if (isDemonLord && hasBody) {
                boolean isFromVelgrynd = (source.getEntity() instanceof VelgryndEntity) || (source.getDirectEntity() instanceof VelgryndEntity);
                boolean isFromKubo = (source.getEntity() instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity)
                        || (source.getDirectEntity() instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity)
                        || player.getTags().contains("KuboPenetrationDamage");
                if (!isFromVelgrynd && !isFromKubo) {
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

                if (!isCreeper && !isSelf && !isRavager && !isDemon && !isBoss && !isVelgrynd && !isKubo) {
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
        // 3. CƠ CHẾ SÁT THƯƠNG CỦA NGƯỜI CHƠI THỦY TỔ ÁC MA KHI TẤN CÔNG (ATTACKER)
        // =========================================================================
        if (!HANDLING_PRIMORDIAL_HURT.get()
                && source.getEntity() instanceof ServerPlayer attackerPlayer
                && PrimordialPlayerDataHelper.isPrimordial(attackerPlayer)
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

        return EventResult.pass();
    }

    public static EventResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player instanceof ServerPlayer sp) {
            // Kiểm tra rương tự nhiên để chèn Sách Cổ Khởi Nguyên Thủy Tổ
            TomeLootManager.onOpenContainer(sp, pos);

            // 0. Tương tác La Bàn Thần Môn (Chuột phải đổi số cổng / Shift+Chuột phải mở GUI 0-9) khi đứng ngay cạnh Cổng 3D
            if (hand == InteractionHand.MAIN_HAND
                    && DimensionalSanctuaryAbility.tryInteractWithGateBlockClick(sp, pos, sp.isShiftKeyDown())) {
                return EventResult.interruptFalse();
            }

            // 1. Luôn ưu tiên hiến tế pháp trận triệu hồi ác ma trước
            if (PrimordialSummonRitual.offerSacrifice(sp, hand, Vec3.atCenterOf(pos))) {
                return EventResult.interruptFalse();
            }
            // 2. Tương tác Bồn Chứa Thể Xác Nhân Tạo (hỗ trợ cả nửa trên và nửa dưới)
            BlockPos targetPos = pos;
            if (player.level().getBlockState(targetPos).is(ModBlocks.INCUBATION_CAPSULE.get())) {
                var state = player.level().getBlockState(targetPos);
                if (state.hasProperty(com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock.HALF) &&
                        state.getValue(com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                    targetPos = targetPos.below();
                }
                if (IncubationCapsuleManager.onInteract(sp, hand, targetPos)) {
                    return EventResult.interruptFalse();
                }
            } else if (player.level().getBlockState(pos.below()).is(ModBlocks.INCUBATION_CAPSULE.get())) {
                if (IncubationCapsuleManager.onInteract(sp, hand, pos.below())) {
                    return EventResult.interruptFalse();
                }
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

                // Cầm Khung Xương Ma Thiết click vào sinh vật KHÔNG PHẢI Ác Ma Thủy Tổ
                if (held.is(ModItems.ARTIFICIAL_SKELETON.get()) && !(target instanceof PrimordialDemonEntity)) {
                    sp.displayClientMessage(Component.literal("§c§l✦ Sinh vật này không phải Ác Ma Thủy Tổ! Khung Xương Ma Thiết chỉ dùng để ban thể xác vật lý cho Ác Ma đã khế ước!"), true);
                    return EventResult.interruptFalse();
                }

                boolean isHoldingSoul = held.is(ModItems.MOB_SOUL.get()) || held.is(ModItems.DEMON_LORD_SOUL.get());
                int totalSouls = countTotalSouls(sp);

                // Nếu click vào Dân Làng / Thương Nhân khi đang giữ Shift (mà KHÔNG cầm Linh Hồn trên tay) -> Nhường cho tính năng Trao Đổi (Trade GUI)
                if (target instanceof net.minecraft.world.entity.npc.AbstractVillager && sp.isShiftKeyDown() && !isHoldingSoul) {
                    return EventResult.pass();
                }

                if (isHoldingSoul || sp.isShiftKeyDown()) {
                    if (totalSouls >= 64) {
                        if (target instanceof MilimEntity || target instanceof VelgryndEntity || target instanceof VelzardEntity) {
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

            // Click trúng dummy bên trong bồn chứa (Skeleton dummy hoặc Demon dummy)
            if (entity.getTags().contains("CapsuleSkeletonDummy") || entity.getTags().contains("CapsuleDemonDummy")) {
                BlockPos capsulePos = entity.blockPosition();
                if (!sp.level().getBlockState(capsulePos).is(ModBlocks.INCUBATION_CAPSULE.get())) {
                    capsulePos = capsulePos.below();
                }
                if (sp.level().getBlockState(capsulePos).is(ModBlocks.INCUBATION_CAPSULE.get())) {
                    var state = sp.level().getBlockState(capsulePos);
                    if (state.hasProperty(com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock.HALF) &&
                            state.getValue(com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                        capsulePos = capsulePos.below();
                    }
                    if (IncubationCapsuleManager.onInteract(sp, hand, capsulePos)) {
                        return EventResult.interruptFalse();
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

        // 3. Đối với MỌI SINH VẬT KHÁC (Mob, Dân Làng, Động Vật):
        // 90% Mất Kiểm Soát (Cuồng Nộ - Berserk) vs 10% Kiểm Soát Được (Calm)
        target.addTag("TensuraDemonLordMob");
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
                    mobAcc.weapons$getGoalSelector().addGoal(0, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(pMob, 1.45D, true));
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
