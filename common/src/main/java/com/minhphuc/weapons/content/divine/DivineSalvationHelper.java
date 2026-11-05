package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.content.soul.EntitySoulItem;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * BỘ HỖ TRỢ CỨU THẾ & ĐỨC TIN (DIVINE SALVATION HELPER)
 * Tối ưu hóa ZERO-LAG:
 * 1. Cải tà quy chính:
 *    - Mob thù địch đất (trừ Boss): Creeper, Skeleton, Zombie, Enderman... -> Dân Làng (Villager).
 *    - Mob bay, dơi, nhện: Bat, Spider, Phantom, Vex, Ghast... -> Random Mèo, Chó, Cá, hoặc Ong.
 * 2. Tái sinh linh hồn:
 *    - Item đại diện + Linh hồn nằm cạnh nhau -> Hồi sinh sinh vật tương ứng.
 * 3. Đại Phục Hưng Làng Bỏ Hoang:
 *    - Quét mạng nhện, dọn rêu phong, sửa đá, hồi sinh dân làng & Iron Golem.
 */
public class DivineSalvationHelper {

    private static final Random RANDOM = new Random();

    public static boolean isBoss(LivingEntity e) {
        if (e == null) return false;
        if (e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden || e instanceof ElderGuardian) {
            return true;
        }
        // Custom Bosses
        String className = e.getClass().getName();
        return className.contains("PrimordialDemon") ||
               className.contains("KuboEntity") ||
               className.contains("CuuTheQuyenHien") ||
               className.contains("TaKinhDaiTang") ||
               className.contains("MilimEntity") ||
               className.contains("VelgryndEntity") ||
               className.contains("VelzardEntity") ||
               className.contains("MarbasEntity");
    }

    /**
     * Chuyển hóa mob thù địch thành dân làng hoặc thú cưng thiện lương.
     */
    public static boolean tryConvertHostile(ServerLevel level, LivingEntity e, @Nullable Player caster) {
        if (e == null || !e.isAlive() || isBoss(e)) return false;

        boolean isFlyOrSpider = e instanceof Spider || e instanceof Bat || e instanceof Phantom ||
                e instanceof Vex || e instanceof Ghast || e instanceof FlyingMob;

        boolean isHostile = (e instanceof Monster || e instanceof Enemy || e instanceof Slime) && !(e instanceof Villager);

        if (!isFlyOrSpider && !isHostile) return false;

        Vec3 pos = e.position();
        float yRot = e.getYRot();

        if (isFlyOrSpider) {
            // Chuyển hóa mob bay / nhện / dơi thành Mèo, Chó, Cá, hoặc Ong ngẫu nhiên
            int choice = RANDOM.nextInt(4);
            EntityType<? extends LivingEntity> targetType = switch (choice) {
                case 0 -> EntityType.CAT;
                case 1 -> EntityType.WOLF;
                case 2 -> EntityType.TROPICAL_FISH;
                default -> EntityType.BEE;
            };

            LivingEntity converted = targetType.create(level);
            if (converted != null) {
                converted.moveTo(pos.x, pos.y, pos.z, yRot, 0.0F);
                if (converted instanceof TamableAnimal tamable && caster != null) {
                    tamable.tame(caster);
                }
                level.addFreshEntity(converted);
                playConversionVFX(level, pos);
                e.discard();
                return true;
            }
        } else {
            // Chuyển hóa mob thù địch đất (Creeper, Enderman, Skeleton, Zombie...) thành Dân Làng
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager != null) {
                villager.moveTo(pos.x, pos.y, pos.z, yRot, 0.0F);
                villager.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), MobSpawnType.CONVERSION, null);
                level.addFreshEntity(villager);
                playConversionVFX(level, pos);
                e.discard();
                return true;
            }
        }

        return false;
    }

    private static void playConversionVFX(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 0.8D, pos.z, 25, 0.4D, 0.5D, 0.4D, 0.12D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1.0D, pos.z, 15, 0.4D, 0.4D, 0.4D, 0.05D);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.0D, pos.z, 1, 0, 0, 0, 0);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 2.0F, 1.2F);
    }

    /**
     * Tái sinh sinh vật từ Cặp Item: Linh Hồn + Item Đại Diện nằm cạnh nhau trên mặt đất.
     */
    public static void processSoulResurrection(ServerLevel level, Vec3 center, double radius, @Nullable Player caster) {
        AABB searchBox = new AABB(center.x - radius, center.y - 4.0D, center.z - radius,
                                  center.x + radius, center.y + 8.0D, center.z + radius);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox, ItemEntity::isAlive);
        if (items.size() < 2) return;

        for (int i = 0; i < items.size(); i++) {
            ItemEntity soulEntity = items.get(i);
            ItemStack soulStack = soulEntity.getItem();
            if (!isSoulItem(soulStack)) continue;

            for (int j = 0; j < items.size(); j++) {
                if (i == j) continue;
                ItemEntity matEntity = items.get(j);
                ItemStack matStack = matEntity.getItem();

                if (soulEntity.distanceToSqr(matEntity) <= 3.0D * 3.0D) {
                    EntityType<?> targetEntity = mapItemToEntity(matStack.getItem());
                    if (targetEntity != null) {
                        // Tiêu hao 1 item từ mỗi stack
                        soulStack.shrink(1);
                        if (soulStack.isEmpty()) soulEntity.discard();
                        else soulEntity.setItem(soulStack);

                        matStack.shrink(1);
                        if (matStack.isEmpty()) matEntity.discard();
                        else matEntity.setItem(matStack);

                        // Triệu hồi sinh vật
                        Vec3 spawnPos = soulEntity.position();
                        net.minecraft.world.entity.Entity spawned = targetEntity.create(level);
                        if (spawned != null) {
                            spawned.moveTo(spawnPos.x, spawnPos.y + 0.2D, spawnPos.z, level.random.nextFloat() * 360.0F, 0.0F);
                            if (spawned instanceof Mob mob) {
                                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spawnPos)), MobSpawnType.EVENT, null);
                            }
                            if (spawned instanceof TamableAnimal tamable && caster != null) {
                                tamable.tame(caster);
                            }
                            level.addFreshEntity(spawned);

                            // Hiệu ứng bùng nổ thánh quang
                            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, spawnPos.x, spawnPos.y + 0.8D, spawnPos.z, 40, 0.5D, 0.7D, 0.5D, 0.15D);
                            level.sendParticles(ParticleTypes.FLASH, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z, 2, 0, 0, 0, 0);
                            level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.5F, 1.1F);

                            if (caster instanceof ServerPlayer sp) {
                                sp.displayClientMessage(
                                        Component.literal("§a§l[TÁI SINH THÁNH QUANG] §fĐã phục sinh thành công: §e" + spawned.getDisplayName().getString() + " §f✨🕊️"),
                                        true
                                );
                            }
                        }
                        return; // Xử lý 1 cặp mỗi chu kỳ quét để chống spam/lag
                    }
                }
            }
        }
    }

    public static boolean isSoulItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof EntitySoulItem) return true;
        if (stack.is(ModItems.DEMON_LORD_SOUL.get())) return true;
        String name = stack.getDescriptionId().toLowerCase();
        return name.contains("soul");
    }

    @Nullable
    private static EntityType<?> mapItemToEntity(Item item) {
        if (item == Items.FEATHER || item == Items.CHICKEN || item == Items.COOKED_CHICKEN || item == Items.EGG) {
            return EntityType.CHICKEN;
        } else if (item == Items.BEEF || item == Items.COOKED_BEEF || item == Items.LEATHER) {
            return EntityType.COW;
        } else if (item == Items.PORKCHOP || item == Items.COOKED_PORKCHOP) {
            return EntityType.PIG;
        } else if (item == Items.WHITE_WOOL || item == Items.MUTTON || item == Items.COOKED_MUTTON) {
            return EntityType.SHEEP;
        } else if (item == Items.BONE) {
            return EntityType.WOLF;
        } else if (item == Items.STRING) {
            return EntityType.CAT;
        } else if (item == Items.CARROT || item == Items.RABBIT || item == Items.RABBIT_FOOT) {
            return EntityType.RABBIT;
        } else if (item == Items.SADDLE || item == Items.HAY_BLOCK) {
            return EntityType.HORSE;
        } else if (item == Items.HONEYCOMB || item == Items.HONEY_BOTTLE) {
            return EntityType.BEE;
        } else if (item == Items.IRON_INGOT || item == Items.IRON_BLOCK) {
            return EntityType.IRON_GOLEM;
        } else if (item == Items.SNOWBALL || item == Items.SNOW_BLOCK) {
            return EntityType.SNOW_GOLEM;
        } else if (item == Items.EMERALD || item == Items.EMERALD_BLOCK || item == Items.BOOK) {
            return EntityType.VILLAGER;
        }
        return null;
    }

    /**
     * Tái tạo làng mạc bỏ hoang & hồi sinh dân làng (Tối ưu ZERO-LAG, tối đa 64 block/lần).
     */
    public static void restoreAbandonedVillage(ServerLevel level, BlockPos center, int radius, @Nullable Player caster) {
        int cleanedBlocks = 0;
        int maxChanges = 64;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius && cleanedBlocks < maxChanges; dx += 2) {
            for (int dz = -radius; dz <= radius && cleanedBlocks < maxChanges; dz += 2) {
                int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
                for (int dy = -3; dy <= 4 && cleanedBlocks < maxChanges; dy++) {
                    mpos.set(center.getX() + dx, groundY + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(mpos);

                    if (state.is(Blocks.COBWEB)) {
                        level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 3);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mpos.getX() + 0.5D, mpos.getY() + 0.5D, mpos.getZ() + 0.5D, 2, 0.2D, 0.2D, 0.2D, 0.02D);
                        cleanedBlocks++;
                    } else if (state.is(Blocks.MOSSY_COBBLESTONE)) {
                        level.setBlock(mpos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                        cleanedBlocks++;
                    } else if (state.is(Blocks.MOSSY_STONE_BRICKS)) {
                        level.setBlock(mpos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                        cleanedBlocks++;
                    }
                }
            }
        }

        // Nếu phát hiện có dấu vết làng hoang phế (đã dọn dẹp) và khu vực ít dân làng
        if (cleanedBlocks >= 3) {
            AABB villageCheck = new AABB(center).inflate(28.0D);
            List<Villager> existingVillagers = level.getEntitiesOfClass(Villager.class, villageCheck, LivingEntity::isAlive);

            if (existingVillagers.size() < 4) {
                int spawnCount = Math.min(5, 5 - existingVillagers.size());
                for (int k = 0; k < spawnCount; k++) {
                    double ox = (RANDOM.nextDouble() - 0.5D) * 12.0D;
                    double oz = (RANDOM.nextDouble() - 0.5D) * 12.0D;
                    BlockPos spawnP = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset((int)ox, 0, (int)oz));

                    Villager newVillager = EntityType.VILLAGER.create(level);
                    if (newVillager != null) {
                        newVillager.moveTo(spawnP.getX() + 0.5D, spawnP.getY(), spawnP.getZ() + 0.5D, RANDOM.nextFloat() * 360.0F, 0.0F);
                        newVillager.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnP), MobSpawnType.EVENT, null);
                        level.addFreshEntity(newVillager);
                        playConversionVFX(level, newVillager.position());
                    }
                }

                // Spawn 1 Iron Golem bảo vệ làng
                IronGolem golem = EntityType.IRON_GOLEM.create(level);
                if (golem != null) {
                    golem.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
                    level.addFreshEntity(golem);
                }

                level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3.5F, 0.8F);
                if (caster instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.literal("§a§l[ĐẠI PHỤC HƯNG] §6Thánh quang đã thanh tẩy tàn tích và hồi sinh dân làng trở lại ngôi làng! ✨🏘️"),
                            true
                    );
                }
            }
        }
    }
}
