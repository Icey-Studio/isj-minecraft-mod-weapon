package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.client.gui.ClientDemonCommandOpener;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.minhphuc.weapons.content.tensura.PrimordialSkillPool;
import com.minhphuc.weapons.data.EntityDataHelper;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PrimordialPactItem extends Item {

    public PrimordialPactItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static ItemStack createPact(DemonType type, Player owner) {
        Item pactItem = type != null ? type.getPactItem().get() : ModItems.PRIMORDIAL_PACT.get();
        ItemStack stack = new ItemStack(pactItem);
        ItemStackDataHelper.putString(stack, "DemonType", type != null ? type.name() : "NOIR");
        ItemStackDataHelper.putBoolean(stack, "IsSummoned", false);
        if (owner != null) {
            ItemStackDataHelper.putString(stack, "OwnerUUID", owner.getStringUUID());
            ItemStackDataHelper.putString(stack, "OwnerName", owner.getName().getString());
        }
        return stack;
    }

    public static DemonType getDemonType(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.PRIMORDIAL_PACT_NOIR.get() || item == ModItems.PRIMORDIAL_PACT_NOIR_BODY.get() || item == ModItems.PRIMORDIAL_PACT_NOIR_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_NOIR_AWAKENED.get()) return DemonType.NOIR;
        if (item == ModItems.PRIMORDIAL_PACT_ROUGE.get() || item == ModItems.PRIMORDIAL_PACT_ROUGE_BODY.get() || item == ModItems.PRIMORDIAL_PACT_ROUGE_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_ROUGE_AWAKENED.get()) return DemonType.ROUGE;
        if (item == ModItems.PRIMORDIAL_PACT_BLANC.get() || item == ModItems.PRIMORDIAL_PACT_BLANC_BODY.get() || item == ModItems.PRIMORDIAL_PACT_BLANC_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_BLANC_AWAKENED.get()) return DemonType.BLANC;
        if (item == ModItems.PRIMORDIAL_PACT_JAUNE.get() || item == ModItems.PRIMORDIAL_PACT_JAUNE_BODY.get() || item == ModItems.PRIMORDIAL_PACT_JAUNE_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_JAUNE_AWAKENED.get()) return DemonType.JAUNE;
        if (item == ModItems.PRIMORDIAL_PACT_VIOLET.get() || item == ModItems.PRIMORDIAL_PACT_VIOLET_BODY.get() || item == ModItems.PRIMORDIAL_PACT_VIOLET_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_VIOLET_AWAKENED.get()) return DemonType.VIOLET;
        if (item == ModItems.PRIMORDIAL_PACT_BLEU.get() || item == ModItems.PRIMORDIAL_PACT_BLEU_BODY.get() || item == ModItems.PRIMORDIAL_PACT_BLEU_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_BLEU_AWAKENED.get()) return DemonType.BLEU;
        if (item == ModItems.PRIMORDIAL_PACT_VERT.get() || item == ModItems.PRIMORDIAL_PACT_VERT_BODY.get() || item == ModItems.PRIMORDIAL_PACT_VERT_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_VERT_AWAKENED.get()) return DemonType.VERT;

        String name = ItemStackDataHelper.getString(stack, "DemonType");
        if (name == null || name.isEmpty()) {
            return DemonType.NOIR;
        }
        try {
            return DemonType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return DemonType.NOIR;
        }
    }

    public static boolean hasPhysicalBody(ItemStack stack) {
        if (ItemStackDataHelper.getBoolean(stack, "HasPhysicalBody")) return true;
        Item item = stack.getItem();
        return item == ModItems.PRIMORDIAL_PACT_BODY.get() || item == ModItems.PRIMORDIAL_PACT_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_NOIR_BODY.get() || item == ModItems.PRIMORDIAL_PACT_NOIR_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_ROUGE_BODY.get() || item == ModItems.PRIMORDIAL_PACT_ROUGE_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_BLANC_BODY.get() || item == ModItems.PRIMORDIAL_PACT_BLANC_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_JAUNE_BODY.get() || item == ModItems.PRIMORDIAL_PACT_JAUNE_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_VIOLET_BODY.get() || item == ModItems.PRIMORDIAL_PACT_VIOLET_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_BLEU_BODY.get() || item == ModItems.PRIMORDIAL_PACT_BLEU_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_VERT_BODY.get() || item == ModItems.PRIMORDIAL_PACT_VERT_AWAKENED.get();
    }

    public static boolean isNamed(ItemStack stack) {
        if (ItemStackDataHelper.getBoolean(stack, "IsNamed")) return true;
        String cName = ItemStackDataHelper.getString(stack, "CustomDemonName");
        if (cName != null && !cName.isEmpty()) return true;
        Item item = stack.getItem();
        return item == ModItems.PRIMORDIAL_PACT_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_NOIR_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_NOIR_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_ROUGE_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_ROUGE_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_BLANC_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_BLANC_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_JAUNE_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_JAUNE_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_VIOLET_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_VIOLET_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_BLEU_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_BLEU_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_VERT_NAMED.get() || item == ModItems.PRIMORDIAL_PACT_VERT_AWAKENED.get();
    }

    public static boolean isAwakened(ItemStack stack) {
        if (hasPhysicalBody(stack) && isNamed(stack)) return true;
        Item item = stack.getItem();
        return item == ModItems.PRIMORDIAL_PACT_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_NOIR_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_ROUGE_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_BLANC_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_JAUNE_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_VIOLET_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_BLEU_AWAKENED.get() ||
                item == ModItems.PRIMORDIAL_PACT_VERT_AWAKENED.get();
    }

    public static boolean isSummoned(ItemStack stack) {
        return ItemStackDataHelper.getBoolean(stack, "IsSummoned");
    }

    @Override
    public Component getName(ItemStack stack) {
        DemonType type = getDemonType(stack);
        boolean hasBody = hasPhysicalBody(stack);
        boolean named = isNamed(stack);
        String customName = ItemStackDataHelper.getString(stack, "CustomDemonName");
        boolean isDemonLord = ItemStackDataHelper.getBoolean(stack, "IsDemonLord") || ItemStackDataHelper.getBoolean(stack, "IsAwakenedDemonLord");

        String displayName = !customName.isEmpty() ? customName : type.getColorName();
        if (isDemonLord) {
            return Component.literal("§4§lKhế Ước Chân Ma Vương: §c§l" + displayName + " §e[Thức Tỉnh]");
        } else if (hasBody && named) {
            return Component.literal("§5§lKhế Ước Ma Tướng Thượng Cấp: §6§l" + displayName + " §e[Thể Xác & Tên]");
        } else if (hasBody) {
            return Component.literal("§b§lKhế Ước Ma Tướng Thượng Cấp: §e§l" + displayName + " §7(Có Thể Xác)");
        } else if (named) {
            return Component.literal("§a§lKhế Ước Ma Tướng Thượng Cấp: §6§l" + displayName + " §7(Đã Ban Tên)");
        }
        return Component.literal("§4§lKhế Ước Ma Tướng Thượng Cấp: §c§l" + type.getColorName() + " §7(" + type.getTitleVi() + ")");
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

    public static void consumeSouls(ServerPlayer player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.DEMON_LORD_SOUL.get())) {
                int count = stack.getCount();
                if (count <= remaining) {
                    remaining -= count;
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                } else {
                    stack.shrink(remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) return;

        boolean summoned = isSummoned(stack);
        DemonType type = getDemonType(stack);

        if (summoned) {
            String demonUuidStr = ItemStackDataHelper.getString(stack, "DemonUUID");
            PrimordialDemonEntity targetDemon = null;

            if (!demonUuidStr.isEmpty()) {
                try {
                    UUID id = UUID.fromString(demonUuidStr);
                    Entity e = serverLevel.getEntity(id);
                    if (e instanceof PrimordialDemonEntity pde && pde.isAlive()) {
                        targetDemon = pde;
                    }
                } catch (Exception ignored) {}
            }

            if (targetDemon == null) {
                List<PrimordialDemonEntity> nearby = serverLevel.getEntitiesOfClass(
                        PrimordialDemonEntity.class,
                        new AABB(player.getX() - 128, player.getY() - 64, player.getZ() - 128, player.getX() + 128, player.getY() + 64, player.getZ() + 128),
                        d -> d.isAlive() && d.isOwnedBy(player) && d.getDemonType() == type
                );
                if (!nearby.isEmpty()) {
                    targetDemon = nearby.get(0);
                }
            }

            if (targetDemon != null) {
                ItemStackDataHelper.putFloat(stack, "CurrentHp", targetDemon.getHealth());
                ItemStackDataHelper.putFloat(stack, "MaxHp", targetDemon.getMaxHealth());
                ItemStackDataHelper.putFloat(stack, "AttackDamage", (float) targetDemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                ItemStackDataHelper.putLong(stack, "CalculatedEP", targetDemon.getCalculatedEP());
                ItemStackDataHelper.putString(stack, "DemonRank", targetDemon.getDemonRankTitle());
                String status = targetDemon.getTarget() != null
                        ? "§cĐang giao chiến: " + targetDemon.getTarget().getDisplayName().getString()
                        : (targetDemon.isOrderedToSit() ? "§eĐang canh giữ vị trí" : "§aĐi theo bảo vệ chủ nhân");
                ItemStackDataHelper.putString(stack, "CombatState", status);

                boolean isDemonLord = targetDemon.getTags().contains("TensuraDemonLordMob");
                ItemStackDataHelper.putBoolean(stack, "IsDemonLord", isDemonLord);
                ItemStackDataHelper.putBoolean(stack, "IsAwakenedDemonLord", isDemonLord);

                if (targetDemon.hasPhysicalBody()) {
                    ItemStackDataHelper.putBoolean(stack, "HasPhysicalBody", true);
                }

                if (targetDemon.isNamed() || (targetDemon.getCustomDemonName() != null && !targetDemon.getCustomDemonName().isEmpty())) {
                    ItemStackDataHelper.putBoolean(stack, "IsNamed", true);
                    if (targetDemon.getCustomDemonName() != null && !targetDemon.getCustomDemonName().isEmpty()) {
                        ItemStackDataHelper.putString(stack, "CustomDemonName", targetDemon.getCustomDemonName());
                    }
                }
            }
        } else {
            // Khi đang nghỉ trong khế ước: Hồi phục sinh lực tự nhiên
            if (entity.tickCount % 20 == 0) {
                boolean hasBody = hasPhysicalBody(stack);
                boolean named = isNamed(stack);
                boolean isDemonLord = ItemStackDataHelper.getBoolean(stack, "IsDemonLord") || ItemStackDataHelper.getBoolean(stack, "IsAwakenedDemonLord");
                double hpMul = (hasBody ? 4.0D : 1.0D) * (named ? 30.0D : 1.0D) * (isDemonLord ? 20.0D : 1.0D);
                if (type == DemonType.NOIR) hpMul *= 1.2D;
                float maxHp = (float) (type.getMaxHealth() * hpMul);
                float currentHp = ItemStackDataHelper.getFloat(stack, "CurrentHp", maxHp);
                if (currentHp < maxHp) {
                    float newHp = Math.min(maxHp, currentHp + Math.max(10.0F, maxHp * 0.01F));
                    ItemStackDataHelper.putFloat(stack, "CurrentHp", newHp);
                    ItemStackDataHelper.putFloat(stack, "MaxHp", maxHp);
                }
            }
        }

        // Ticking nghiệp vụ ma giới: Quản gia hư không, Lãnh địa Blanc & Trọng tài Guy Crimson (Mỗi 40 ticks = 2s, Zero Lag)
        if (entity.tickCount % 40 == 0 && !level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
            AbyssalGovernanceManager.handleAutoHooverTick(serverPlayer, stack);
            AbyssalGovernanceManager.handleProtectorateTick(serverPlayer, stack);
            AbyssalGovernanceManager.checkGuyLifelineTick(serverPlayer, stack);
        }
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // SHIFT + CHUỘT PHẢI VÀO RƯƠNG: LIÊN KẾT RƯƠNG CĂN CỨ CHO QUẢN GIA HƯ KHÔNG
        if (player instanceof ServerPlayer sp && player.isShiftKeyDown()) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof net.minecraft.world.Container) {
                AbyssalGovernanceManager.bindVault(sp, context.getItemInHand(), pos);
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.is(com.minhphuc.weapons.init.ModBlocks.INCUBATION_CAPSULE.get())) {
            if (player instanceof ServerPlayer sp) {
                BlockPos basePos = state.hasProperty(com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock.HALF)
                        && state.getValue(com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
                        ? pos.below() : pos;
                com.minhphuc.weapons.content.tensura.capsule.IncubationCapsuleManager.onInteract(sp, context.getHand(), basePos);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        DemonType type = getDemonType(stack);
        boolean summoned = isSummoned(stack);

        if (summoned && !player.isShiftKeyDown()) {
            // Khi đã triệu hồi và chuột phải thường: Mở Bảng Điều Khiển Mệnh Lệnh
            if (level.isClientSide()) {
                ClientDemonCommandOpener.openScreen(stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // Tìm ác ma đã được triệu hồi trước đó nếu có
        String demonUuidStr = ItemStackDataHelper.getString(stack, "DemonUUID");
        PrimordialDemonEntity targetDemon = null;

        if (!demonUuidStr.isEmpty()) {
            try {
                UUID id = UUID.fromString(demonUuidStr);
                Entity e = serverLevel.getEntity(id);
                if (e instanceof PrimordialDemonEntity pde && pde.isAlive()) {
                    targetDemon = pde;
                }
            } catch (Exception ignored) {}
        }

        if (targetDemon == null) {
            List<PrimordialDemonEntity> nearby = serverLevel.getEntitiesOfClass(
                    PrimordialDemonEntity.class,
                    new AABB(player.getX() - 64, player.getY() - 32, player.getZ() - 64, player.getX() + 64, player.getY() + 32, player.getZ() + 64),
                    d -> d.isOwnedBy(player) && d.getDemonType() == type
            );
            if (!nearby.isEmpty()) {
                targetDemon = nearby.get(0);
            }
        }

        if (player.isShiftKeyDown()) {
            // ==========================================
            // SHIFT + CHUỘT PHẢI: DỊCH CHUYỂN & ĐỔI NHANH CHẾ ĐỘ
            // ==========================================
            if (targetDemon != null) {
                targetDemon.teleportTo(player.getX() + player.getLookAngle().x * 2.0D, player.getY(), player.getZ() + player.getLookAngle().z * 2.0D);
                boolean newSitting = !targetDemon.isOrderedToSit();
                targetDemon.setGuardMode(newSitting, targetDemon.blockPosition());
                ItemStackDataHelper.putBoolean(stack, "IsGuarding", newSitting);
                targetDemon.getNavigation().stop();

                serverLevel.sendParticles(ParticleTypes.PORTAL, targetDemon.getX(), targetDemon.getY() + 1.0D, targetDemon.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);
                serverLevel.playSound(null, targetDemon.getX(), targetDemon.getY(), targetDemon.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.2F);

                String effName = (ItemStackDataHelper.getBoolean(stack, "IsNamed") && !ItemStackDataHelper.getString(stack, "CustomDemonName").isEmpty())
                        ? ItemStackDataHelper.getString(stack, "CustomDemonName") : type.getColorName();
                if (newSitting) {
                    player.displayClientMessage(Component.literal("§e§l[" + effName + "] §6ĐÃ ĐƯỢC ĐẶT Ở CHẾ ĐỘ: §eTUẦN TRA CANH GIỮ KHU VỰC RỘNG!"), true);
                } else {
                    player.displayClientMessage(Component.literal("§e§l[" + effName + "] §6ĐÃ ĐƯỢC ĐẶT Ở CHẾ ĐỘ: §aĐI THEO BẢO VỆ CHỦ NHÂN!"), true);
                }
                return InteractionResultHolder.consume(stack);
            } else if (summoned) {
                // ==========================================
                // HIỆU TRIỆU XUYÊN KHÔNG GIAN KHI ĐI XA (CHUNK UNLOADED)
                // ==========================================
                PrimordialDemonEntity remoteDemon = summonRemoteDemon(serverLevel, (ServerPlayer) player, stack);
                if (remoteDemon != null) {
                    return InteractionResultHolder.consume(stack);
                }
            } else {
                String effName = (ItemStackDataHelper.getBoolean(stack, "IsNamed") && !ItemStackDataHelper.getString(stack, "CustomDemonName").isEmpty())
                        ? ItemStackDataHelper.getString(stack, "CustomDemonName") : type.getColorName();
                player.displayClientMessage(Component.literal("§7Ác ma §e" + effName + " §7hiện đang nghỉ ngơi trong ấn chú. Bấm Chuột Phải thường để triệu hồi xuất thế!"), true);
                return InteractionResultHolder.fail(stack);
            }
        } else {
            // ==========================================
            // CHUỘT PHẢI THƯỜNG KHI CHƯA TRIỆU HỒI: TRIỆU HỒI XUẤT THẾ!
            // ==========================================
            PrimordialDemonEntity newDemon = ModEntities.PRIMORDIAL_DEMON.get().create(serverLevel);
            if (newDemon != null) {
                double spawnX = player.getX() + player.getLookAngle().x * 2.5D;
                double spawnY = player.getY();
                double spawnZ = player.getZ() + player.getLookAngle().z * 2.5D;

                newDemon.moveTo(spawnX, spawnY, spawnZ, player.getYRot() + 180.0F, 0.0F);
                newDemon.setDemonType(type);
                newDemon.tame(player);

                // Đọc trạng thái tiến hóa sẵn có
                boolean hasBody = hasPhysicalBody(stack);
                boolean isNamed = isNamed(stack);
                String customName = ItemStackDataHelper.getString(stack, "CustomDemonName");
                int tier = ItemStackDataHelper.getInt(stack, "EvolutionTier");
                if (tier == 0) {
                    if (hasBody && isNamed) tier = 3;
                    else if (hasBody) tier = 1;
                    else if (isNamed) tier = 2;
                }

                boolean isDemonLord = ItemStackDataHelper.getBoolean(stack, "IsDemonLord") || ItemStackDataHelper.getBoolean(stack, "IsAwakenedDemonLord");

                newDemon.setWinged(type == DemonType.NOIR || isDemonLord);
                newDemon.setPhysicalBody(hasBody);
                newDemon.setNamed(isNamed);
                newDemon.setEvolutionTier(tier);
                newDemon.setCustomDemonName(customName);

                if (isNamed) {
                    newDemon.addTag("TensuraNamedMob");
                }

                if (isDemonLord) {
                    newDemon.addTag("TensuraDemonLordMob");
                    newDemon.addTag("TensuraDemonLordCalm");
                }

                newDemon.updateAttributesForType(type);
                newDemon.updateDemonNametag();
                newDemon.setCustomNameVisible(true);

                // Đọc và khôi phục kỹ năng ngẫu nhiên đã lưu
                String savedSkills = ItemStackDataHelper.getString(stack, "PrimordialSkills");
                if (!savedSkills.isEmpty()) {
                    newDemon.setRandomSkillIds(Arrays.asList(savedSkills.split(",")));
                }
                newDemon.ensureRandomSkills();

                // Lưu danh sách kỹ năng lại vào stack
                StringBuilder sb = new StringBuilder();
                for (PrimordialSkillPool.SkillEntry sk : newDemon.getRandomSkills()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(sk.id());
                }
                ItemStackDataHelper.putString(stack, "PrimordialSkills", sb.toString());
                ItemStackDataHelper.putBoolean(stack, "HasPhysicalBody", hasBody);
                ItemStackDataHelper.putBoolean(stack, "IsNamed", isNamed);
                ItemStackDataHelper.putString(stack, "CustomDemonName", customName);
                ItemStackDataHelper.putInt(stack, "EvolutionTier", tier);
                if (isDemonLord) {
                    ItemStackDataHelper.putBoolean(stack, "IsDemonLord", true);
                    ItemStackDataHelper.putBoolean(stack, "IsAwakenedDemonLord", true);
                }

                // Khôi phục lượng máu đã lưu trong ấn chú
                float savedHp = ItemStackDataHelper.getFloat(stack, "CurrentHp", newDemon.getMaxHealth());
                if (savedHp > 0) newDemon.setHealth(Math.min(savedHp, newDemon.getMaxHealth()));
                newDemon.setOrderedToSit(false);

                serverLevel.addFreshEntity(newDemon);

                // Hiệu ứng triệu hồi
                serverLevel.sendParticles(ParticleTypes.FLASH, spawnX, spawnY + 1.5D, spawnZ, 2, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, spawnX, spawnY + 1.2D, spawnZ, 50, 0.6D, 0.8D, 0.6D, 0.15D);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnX, spawnY + 1.0D, spawnZ, 35, 0.5D, 0.5D, 0.5D, 0.08D);

                serverLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.4F);
                serverLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 1.0F);
                serverLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 1.0F);

                String effectiveName = (isNamed && !customName.isEmpty()) ? customName : type.getColorName();
                String titleRank;
                if (isDemonLord) {
                    titleRank = "CHÂN MA VƯƠNG THỨC TỈNH";
                } else if (hasBody && isNamed) {
                    titleRank = "MA THẦN TỐI THƯỢNG";
                } else if (isNamed) {
                    titleRank = "THỦY TỔ ĐƯỢC BAN DANH";
                } else if (hasBody) {
                    titleRank = "THỦY TỔ NHỤC THỂ";
                } else {
                    titleRank = "THỦY TỔ ÁC MA";
                }

                if (player instanceof ServerPlayer sp && sp.connection != null) {
                    sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l★ " + titleRank + " XUẤT THẾ ★")));
                    sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + effectiveName + " §7(" + type.getTitleVi() + ") §ađáp lời hiệu triệu!")));
                    VoiceOfTheWorld.announce(sp, "Báo cáo. " + titleRank + ": " + effectiveName + " (" + type.getTitleVi() + ") đã xuất thế và phục tùng chủ nhân tuyệt đối!");
                }

                String summonGreet = getSummonGreeting(type);
                player.displayClientMessage(Component.literal("§d§l[" + effectiveName.toUpperCase() + "] §f\"" + summonGreet + "\""), false);
                player.displayClientMessage(Component.literal("§6★ TRIỆU HỒI THÀNH CÔNG: §e" + effectiveName + " §ađã phục tùng! Bấm Chuột Phải để mở Bảng Mệnh Lệnh!"), true);

                ItemStackDataHelper.putBoolean(stack, "IsSummoned", true);
                ItemStackDataHelper.putString(stack, "DemonUUID", newDemon.getStringUUID());
                return InteractionResultHolder.consume(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    private static String getFarewellMessage(DemonType type) {
        switch (type) {
            case ROUGE:
                return "Hmph! Khi nào có kẻ địch xứng tầm thì hãy triệu gọi ta.";
            case NOIR:
                return "Kufufufu... Tôi xin tạm lui về ấn chú. Bất cứ khi nào ngài cần, tôi sẽ lập tức xuất hiện phụng sự ngài, Master!";
            case BLANC:
                return "Ta sẽ nghỉ ngơi trong ấn chú. Đừng để ta đợi quá lâu nhé~";
            case JAUNE:
                return "Haha! Nhớ gọi ta ra đánh nhau sớm đấy nhé!";
            case VIOLET:
                return "Tạm biệt chủ nhân yêu quý~ Nhớ triệu hồi em sớm nha~";
            case BLEU:
                return "Tôi xin phép lui đi nghỉ ngơi...";
            case VERT:
                return "Tôi xin tạm lui. Chúc ngài vạn sự bình an.";
            default:
                return "Tôi xin tạm lui về ấn chú.";
        }
    }

    private static String getSummonGreeting(DemonType type) {
        switch (type) {
            case ROUGE:
                return "Haha! Kẻ nào dám làm phiền chủ nhân của ta? Ta sẽ thiêu rụi chúng thành tro bụi!";
            case NOIR:
                return "Kufufufu... Tôi đã có mặt theo tiếng gọi của ngài, Master! Mọi kẻ ngáng đường ngài đều sẽ bị móng vuốt của tôi xé toạc!";
            case BLANC:
                return "Ara ara... Đã đến lúc ta thanh tẩy những kẻ bất kính này rồi sao?";
            case JAUNE:
                return "Haha! Tuyệt vời! Hãy xem ta dùng trọng lực và ma pháp hủy diệt nghiền nát bọn chúng!";
            case VIOLET:
                return "Hihihi~ Em đến giúp chủ nhân đây! Bọn chúng sẽ nếm trải tử độc của em!";
            case BLEU:
                return "Tôi đã tới. Mọi mục tiêu sẽ bị đóng băng vĩnh viễn.";
            case VERT:
                return "Tôi xin kính cẩn tuân lệnh ngài. Kết giới bão tố đã sẵn sàng bảo hộ ngài chu toàn.";
            default:
                return "Tôi đã xuất thế theo lệnh ngài!";
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DemonType type = getDemonType(stack);
        boolean summoned = isSummoned(stack);
        boolean hasBody = hasPhysicalBody(stack);
        boolean isNamed = isNamed(stack);
        String customName = ItemStackDataHelper.getString(stack, "CustomDemonName");
        boolean isDemonLord = ItemStackDataHelper.getBoolean(stack, "IsDemonLord") || ItemStackDataHelper.getBoolean(stack, "IsAwakenedDemonLord");

        // 1. Tính toán các chỉ số nhân dồn
        double hpMul = (hasBody ? 4.0D : 1.0D);
        double dmgMul = (hasBody ? 30.0D : 1.0D);
        if (isNamed) {
            hpMul *= 30.0D;
            dmgMul *= 30.0D;
        }
        if (isDemonLord) {
            hpMul *= 20.0D;
            dmgMul *= 20.0D;
        }
        if (type == DemonType.NOIR) {
            hpMul *= 1.2D;
            dmgMul *= 1.25D;
        }

        float maxHp = ItemStackDataHelper.getFloat(stack, "MaxHp", (float) (type.getMaxHealth() * hpMul));
        float currentHp = ItemStackDataHelper.getFloat(stack, "CurrentHp", maxHp);
        float hpRatio = maxHp > 0 ? Math.max(0.0F, Math.min(1.0F, currentHp / maxHp)) : 1.0F;

        int dmg = (int) ItemStackDataHelper.getFloat(stack, "AttackDamage", (float) (type.getAttackDamage() * dmgMul));

        // Ma Tố Lượng (EP)
        long baseEp = switch (type) {
            case ROUGE -> 2800000L;
            case NOIR -> 2500000L;
            case BLANC, JAUNE, VIOLET -> 2000000L;
            case BLEU, VERT -> 1500000L;
            default -> 1500000L;
        };
        double epMul = (hasBody ? 3.0D : 1.0D) * (isNamed ? 2.5D : 1.0D) * (isDemonLord ? 6.0D : 1.0D);
        long calcEp = (long) (baseEp * epMul);
        if (isDemonLord && calcEp < 40000000L) {
            calcEp = 40000000L + (hasBody ? 20000000L : 0L);
        }
        long ep = ItemStackDataHelper.getLong(stack, "CalculatedEP", calcEp);
        java.text.DecimalFormat epFmt = new java.text.DecimalFormat("#,###");

        // Line 1: TÊN ÁC MA
        String titleName = !customName.isEmpty() ? customName : type.getColorName().toUpperCase();
        tooltip.add(Component.literal("§6§l✦ TÊN: §e§l" + titleName + " §7(" + type.getTitleVi() + ")"));

        // Line 2: CẤP BẬC MA TỘC (Nếu chưa là ma vương thì mặc định là Ma Tướng Thượng Cấp)
        if (isDemonLord) {
            tooltip.add(Component.literal("§4§l✦ CẤP BẬC: §c§lCHÂN MA VƯƠNG THỨC TỈNH §4(Thần Cấp)"));
        } else {
            tooltip.add(Component.literal("§5§l✦ CẤP BẬC: §d§lMA TƯỚNG THƯỢNG CẤP §7(Archdemon)"));
        }

        // Line 3: THỂ XÁC VẬT LÝ
        if (hasBody) {
            tooltip.add(Component.literal("§b§l✦ THỂ XÁC: §a✔ ĐÃ CÓ THỂ XÁC VẬT LÝ §e(DMG x30, HP x4, EP x3)"));
        } else {
            tooltip.add(Component.literal("§b§l✦ THỂ XÁC: §7✖ CHƯA CÓ (Linh Thể Thuần Khiết)"));
        }

        // Line 4: THANH MÁU (HP BAR)
        int barLength = 12;
        int filled = (int) Math.round(hpRatio * barLength);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                if (hpRatio > 0.5F) bar.append("§a█");
                else if (hpRatio > 0.25F) bar.append("§e█");
                else bar.append("§c█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        tooltip.add(Component.literal("§c❤ " + bar + " §f" + (int) currentHp + "/" + (int) maxHp + " HP §7(" + (int)(hpRatio * 100) + "%)"));

        // Line 5: SỨC MẠNH & MA TỐ LƯỢNG (EP)
        tooltip.add(Component.literal("§e⚔ Sức Mạnh: §f" + dmg + " DMG §7| §d✦ EP: §b" + epFmt.format(ep) + " EP"));

        // Line 6: KHÁNG TÍNH
        tooltip.add(Component.literal("§3🛡 Kháng Tính: §fKháng Lửa, Rơi, Ngạt Thở & Miễn Nguyền Rủa/Độc"));

        // Line 7: KỸ NĂNG THỨC TỈNH NGẪU NHIÊN
        String savedSkills = ItemStackDataHelper.getString(stack, "PrimordialSkills");
        if (!savedSkills.isEmpty()) {
            String[] skillIds = savedSkills.split(",");
            StringBuilder skList = new StringBuilder("§d⚡ Kỹ Năng: ");
            for (int i = 0; i < skillIds.length; i++) {
                PrimordialSkillPool.SkillEntry entry = PrimordialSkillPool.getSkillById(skillIds[i].trim());
                if (entry != null) {
                    if (i > 0) skList.append("§7, ");
                    skList.append(entry.color()).append(entry.displayNameVi());
                }
            }
            tooltip.add(Component.literal(skList.toString()));
        }

        // Line 8: TÌNH TRẠNG HOẠT ĐỘNG
        if (summoned) {
            String combatState = ItemStackDataHelper.getString(stack, "CombatState");
            if (combatState.isEmpty()) combatState = "§aĐi theo bảo vệ chủ nhân";
            tooltip.add(Component.literal("§a● Tình Trạng: " + combatState));
            tooltip.add(Component.literal("  §8[Chuột Phải: Bảng Lệnh & Nghiệp Vụ | Shift + Chuột Phải: Dịch chuyển/Canh]"));
        } else {
            tooltip.add(Component.literal("§7● Tình Trạng: §eĐang nghỉ ngơi trong ấn chú (+10 HP/s)"));
            tooltip.add(Component.literal("  §8[Chuột Phải để Triệu Hồi Xuất Thế]"));
        }

        // Line 9: QUẢN GIA HƯ KHÔNG & RƯƠNG CĂN CỨ
        if (ItemStackDataHelper.hasKey(stack, "BoundVault_Pos")) {
            BlockPos vPos = BlockPos.of(ItemStackDataHelper.getLong(stack, "BoundVault_Pos", 0L));
            boolean hoover = ItemStackDataHelper.getBoolean(stack, "AutoHoover_Enabled");
            tooltip.add(Component.literal("§b📦 Rương Căn Cứ: §e[" + vPos.getX() + ", " + vPos.getY() + ", " + vPos.getZ() + "] " + (hoover ? "§a[Quản Gia: Bật]" : "§c[Quản Gia: Tắt]")));
        } else {
            tooltip.add(Component.literal("§8[Shift + Chuột Phải vào Rương: Đăng Ký Quản Gia Cất Đồ]"));
        }
    }

    public static PrimordialDemonEntity summonRemoteDemon(ServerLevel serverLevel, ServerPlayer player, ItemStack stack) {
        if (serverLevel == null || player == null || stack == null) return null;
        DemonType type = getDemonType(stack);
        if (type == null) return null;

        double spawnX = player.getX() + player.getLookAngle().x * 2.0D;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + player.getLookAngle().z * 2.0D;

        PrimordialDemonEntity remoteDemon = ModEntities.PRIMORDIAL_DEMON.get().create(serverLevel);
        if (remoteDemon != null) {
            remoteDemon.moveTo(spawnX, spawnY, spawnZ, player.getYRot() + 180.0F, 0.0F);
            remoteDemon.setDemonType(type);
            remoteDemon.tame(player);

            boolean hasBody = hasPhysicalBody(stack);
            boolean isNamed = isNamed(stack);
            String customName = ItemStackDataHelper.getString(stack, "CustomDemonName");
            int tier = ItemStackDataHelper.getInt(stack, "EvolutionTier");
            boolean isDemonLord = ItemStackDataHelper.getBoolean(stack, "IsDemonLord") || ItemStackDataHelper.getBoolean(stack, "IsAwakenedDemonLord");

            remoteDemon.setPhysicalBody(hasBody);
            remoteDemon.setNamed(isNamed);
            remoteDemon.setEvolutionTier(tier);
            remoteDemon.setCustomDemonName(customName);
            remoteDemon.setWinged(type == DemonType.NOIR || isDemonLord);

            if (isNamed) {
                remoteDemon.addTag("TensuraNamedMob");
            }

            if (isDemonLord) {
                remoteDemon.addTag("TensuraDemonLordMob");
                remoteDemon.addTag("TensuraDemonLordCalm");
            }

            remoteDemon.updateAttributesForType(type);
            remoteDemon.updateDemonNametag();
            remoteDemon.setCustomNameVisible(true);

            String savedSkills = ItemStackDataHelper.getString(stack, "PrimordialSkills");
            if (!savedSkills.isEmpty()) {
                remoteDemon.setRandomSkillIds(Arrays.asList(savedSkills.split(",")));
            }
            remoteDemon.ensureRandomSkills();

            float savedHp = ItemStackDataHelper.getFloat(stack, "CurrentHp", remoteDemon.getMaxHealth());
            if (savedHp > 0) remoteDemon.setHealth(Math.min(savedHp, remoteDemon.getMaxHealth()));
            remoteDemon.setOrderedToSit(false);

            serverLevel.addFreshEntity(remoteDemon);
            ItemStackDataHelper.putBoolean(stack, "IsSummoned", true);
            ItemStackDataHelper.putString(stack, "DemonUUID", remoteDemon.getStringUUID());

            serverLevel.sendParticles(ParticleTypes.PORTAL, spawnX, spawnY + 1.0D, spawnZ, 50, 0.5D, 0.8D, 0.5D, 0.15D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnX, spawnY + 1.0D, spawnZ, 30, 0.4D, 0.6D, 0.4D, 0.05D);
            serverLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.0F);

            String effName = (!customName.isEmpty()) ? customName : type.getColorName();
            player.displayClientMessage(Component.literal("§d§l[HIỆU TRIỆU KHÔNG GIAN] §e" + effName + " §ađã xé rách không gian, lập tức xuất hiện bên cạnh bạn!"), true);
            return remoteDemon;
        }
        return null;
    }
}
