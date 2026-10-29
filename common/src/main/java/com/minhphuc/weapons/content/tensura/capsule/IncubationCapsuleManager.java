package com.minhphuc.weapons.content.tensura.capsule;

import com.minhphuc.weapons.content.tensura.PrimordialPactItem;
import com.minhphuc.weapons.content.tensura.PrimordialSkillPool;
import com.minhphuc.weapons.content.tensura.VoiceOfTheWorld;
import com.minhphuc.weapons.data.ItemStackDataHelper;
import com.minhphuc.weapons.entity.ModEntities;
import com.minhphuc.weapons.entity.tensura.DemonType;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.mixin.TextDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundOpenCapsuleScreenPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IncubationCapsuleManager {

    public enum CapsuleState {
        EMPTY,
        SKELETON_INSERTED,
        DEMON_INFUSED,
        EVOLVING,
        READY_RETRIEVAL
    }

    public static class ActiveCapsule {
        public final BlockPos pos;
        public final UUID ownerUuid;
        public CapsuleState state = CapsuleState.EMPTY;

        public Display.ItemDisplay capsuleDisplay;
        public Display.ItemDisplay contentDisplay;
        public Display.TextDisplay textDisplay;

        // Demon data inside
        public DemonType demonType = DemonType.NOIR;
        public boolean hasPhysicalBody = false;
        public boolean isNamed = false;
        public String customName = "";
        public float currentHp = 1000.0F;
        public float maxHp = 1000.0F;
        public int evolutionChoice = 0; // 1: Body, 2: Name, 3: Both
        public int animTicks = 0;
        public PrimordialDemonEntity demonEntityInside;
        public LivingEntity skeletonEntityInside;

        public ActiveCapsule(BlockPos pos, UUID ownerUuid) {
            this.pos = pos;
            this.ownerUuid = ownerUuid;
        }

        public void cleanup() {
            if (capsuleDisplay != null && capsuleDisplay.isAlive()) capsuleDisplay.discard();
            if (contentDisplay != null && contentDisplay.isAlive()) contentDisplay.discard();
            if (textDisplay != null && textDisplay.isAlive()) textDisplay.discard();
            if (demonEntityInside != null && demonEntityInside.isAlive()) demonEntityInside.discard();
            if (skeletonEntityInside != null && skeletonEntityInside.isAlive()) skeletonEntityInside.discard();
        }
    }

    private static final Map<BlockPos, ActiveCapsule> ACTIVE_CAPSULES = new ConcurrentHashMap<>();

    public static void createCapsule(ServerLevel level, BlockPos pos, Player placer) {
        if (!level.getBlockState(pos).is(ModBlocks.INCUBATION_CAPSULE.get())) {
            return;
        }
        removeCapsule(level, pos);
        level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5D),
                e -> e.getTags().contains("CapsuleSkeletonDummy") || e.getTags().contains("CapsuleDemonDummy")
        ).forEach(net.minecraft.world.entity.Entity::discard);

        ActiveCapsule capsule = new ActiveCapsule(pos, placer != null ? placer.getUUID() : UUID.randomUUID());
        Vec3 center = Vec3.atBottomCenterOf(pos);

        // 1. Text Display Status Indicator
        Display.TextDisplay textDisplay = EntityType.TEXT_DISPLAY.create(level);
        if (textDisplay != null) {
            textDisplay.moveTo(center.x, center.y + 2.35D, center.z, 0.0F, 0.0F);
            DisplayAccessor dispAcc = (DisplayAccessor) textDisplay;
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            dispAcc.weapons$setViewRange(12.0F);

            Component initialText = Component.literal(
                    "§b§l✦ BỒN CHỨA THỂ XÁC NHÂN TẠO ✦\n" +
                    "§7Trạng thái: §fChưa có Khung Xương\n" +
                    "§e▶ Cầm [Khung Xương Nhân Tạo] Chuột Phải để nạp vào bồn!"
            );
            ((TextDisplayAccessor) textDisplay).weapons$setText(initialText);
            level.addFreshEntity(textDisplay);
            capsule.textDisplay = textDisplay;
        }

        // 2. Content Display (Hiển thị Khung Xương Magisteel bên trong chất lỏng)
        Display.ItemDisplay contentDisplay = EntityType.ITEM_DISPLAY.create(level);
        if (contentDisplay != null) {
            contentDisplay.moveTo(center.x, center.y + 1.1D, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) contentDisplay;
            DisplayAccessor dispAcc = (DisplayAccessor) contentDisplay;

            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(1.5F, 1.8F, 1.5F),
                    null
            ));
            level.addFreshEntity(contentDisplay);
            capsule.contentDisplay = contentDisplay;
        }

        ACTIVE_CAPSULES.put(pos, capsule);

        level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.5F, 1.0F);
    }

    public static void removeCapsule(ServerLevel level, BlockPos pos) {
        ActiveCapsule capsule = ACTIVE_CAPSULES.remove(pos);
        if (capsule != null) {
            capsule.cleanup();
        }
    }

    public static boolean onInteract(Player player, InteractionHand hand, BlockPos pos) {
        if (player.level().isClientSide()) return false;
        if (!player.level().getBlockState(pos).is(ModBlocks.INCUBATION_CAPSULE.get())) {
            return false;
        }
        ActiveCapsule capsule = ACTIVE_CAPSULES.get(pos);
        if (capsule == null) {
            if (player.level() instanceof ServerLevel sl) {
                createCapsule(sl, pos, player);
                capsule = ACTIVE_CAPSULES.get(pos);
            }
        }
        if (capsule == null) return false;

        ServerLevel level = (ServerLevel) player.level();
        ItemStack held = player.getItemInHand(hand);

        switch (capsule.state) {
            case EMPTY -> {
                // Chỉ nhận Khung Xương Nhân Tạo
                if (held.is(ModItems.ARTIFICIAL_SKELETON.get())) {
                    if (!player.isCreative()) held.shrink(1);
                    capsule.state = CapsuleState.SKELETON_INSERTED;

                    // Xóa skeleton cũ nếu có
                    if (capsule.skeletonEntityInside != null && capsule.skeletonEntityInside.isAlive()) {
                        capsule.skeletonEntityInside.discard();
                    }

                    // Triệu hồi thực thể 3D Skeleton Magisteel vĩnh cửu đứng bên trong bồn chứa
                    Vec3 center = Vec3.atBottomCenterOf(pos);
                    net.minecraft.world.entity.decoration.ArmorStand stand = EntityType.ARMOR_STAND.create(level);
                    if (stand != null) {
                        stand.moveTo(center.x, center.y + 0.05D, center.z, 0.0F, 0.0F);
                        stand.setNoGravity(true);
                        stand.setInvulnerable(true);
                        stand.setSilent(true);
                        stand.setShowArms(true);
                        stand.setNoBasePlate(true);
                        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL));
                        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE));
                        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(net.minecraft.world.item.Items.CHAINMAIL_LEGGINGS));
                        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(net.minecraft.world.item.Items.CHAINMAIL_BOOTS));
                        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.BONE));
                        stand.noPhysics = true;
                        stand.addTag("CapsuleSkeletonDummy");
                        level.addFreshEntity(stand);
                        capsule.skeletonEntityInside = stand;
                    }

                    // Hiển thị thêm biểu tượng item 3D Khung Xương Magisteel bên trong bồn chứa
                    if (capsule.contentDisplay != null) {
                        ((ItemDisplayAccessor) capsule.contentDisplay).weapons$setItemStack(new ItemStack(ModItems.ARTIFICIAL_SKELETON.get()));
                    }

                    net.minecraft.world.level.block.state.BlockState curState = level.getBlockState(pos);
                    if (curState.is(ModBlocks.INCUBATION_CAPSULE.get())) {
                        level.setBlock(pos, curState.setValue(IncubationCapsuleBlock.CONTENT, IncubationCapsuleBlock.CapsuleContent.SKELETON), 3);
                    }

                    updateText(capsule,
                            "§b§l✦ BỒN CHỨA THỂ XÁC NHÂN TẠO ✦\n" +
                            "§a✔ Đã nạp 1 Khung Xương Magisteel\n" +
                            "§e▶ Hãy dùng [Khế Ước Ác Ma] Chuột Phải để nạp linh hồn vào bồn!"
                    );

                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.5F, 1.2F);
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.5F, 1.0F);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.05);

                    player.displayClientMessage(Component.literal("§a✔ Đã đặt 1 Khung Xương Nhân Tạo vào bồn chứa! Khung xương 3D đã hiện rõ trong bồn. Hãy dùng Khế Ước Ác Ma Chuột Phải để nạp ác ma."), true);
                    return true;
                } else if (held.getItem() instanceof PrimordialPactItem) {
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§cCần nạp Khung Xương Nhân Tạo trước để làm phôi nuôi cấy!"), true);
                    return true;
                } else {
                    player.displayClientMessage(Component.literal("§7[Bồn Chứa] Hãy cầm §eKhung Xương Nhân Tạo §7Chuột Phải để đặt vào bồn chứa!"), true);
                    return true;
                }
            }
            case SKELETON_INSERTED -> {
                if (held.is(ModItems.ARTIFICIAL_SKELETON.get())) {
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§cMỗi bồn chỉ nuôi cấy 1 bộ khung xương nhân tạo!"), true);
                    return true;
                } else if (held.getItem() instanceof PrimordialPactItem) {
                    insertDemonToCapsule(player, held, capsule, level);
                    return true;
                } else if (held.isEmpty() && !player.isShiftKeyDown()) {
                    // Tự động tìm Khế Ước Ác Ma trong túi đồ nếu click tay không
                    ItemStack foundPact = ItemStack.EMPTY;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack it = player.getInventory().getItem(i);
                        if (!it.isEmpty() && it.getItem() instanceof PrimordialPactItem) {
                            foundPact = it;
                            break;
                        }
                    }
                    if (!foundPact.isEmpty()) {
                        insertDemonToCapsule(player, foundPact, capsule, level);
                        return true;
                    }
                    player.displayClientMessage(Component.literal("§7[Bồn Chứa] Hãy cầm §cKhế Ước Ác Ma §7Chuột Phải vào bồn để nạp linh hồn! (Shift+Click tay không để lấy lại khung xương)"), true);
                    return true;
                } else if (player.isShiftKeyDown() && held.isEmpty()) {
                    // Lấy lại khung xương
                    capsule.state = CapsuleState.EMPTY;
                    if (capsule.skeletonEntityInside != null && capsule.skeletonEntityInside.isAlive()) {
                        capsule.skeletonEntityInside.discard();
                        capsule.skeletonEntityInside = null;
                    }
                    if (capsule.contentDisplay != null) {
                        ((ItemDisplayAccessor) capsule.contentDisplay).weapons$setItemStack(ItemStack.EMPTY);
                    }
                    net.minecraft.world.level.block.state.BlockState curState = level.getBlockState(pos);
                    if (curState.is(ModBlocks.INCUBATION_CAPSULE.get())) {
                        level.setBlock(pos, curState.setValue(IncubationCapsuleBlock.CONTENT, IncubationCapsuleBlock.CapsuleContent.EMPTY), 3);
                    }
                    updateText(capsule,
                            "§b§l✦ BỒN CHỨA THỂ XÁC NHÂN TẠO ✦\n" +
                            "§7Trạng thái: §fChưa có Khung Xương\n" +
                            "§e▶ Cầm [Khung Xương Nhân Tạo] Chuột Phải để nạp vào bồn!"
                    );
                    player.getInventory().add(new ItemStack(ModItems.ARTIFICIAL_SKELETON.get()));
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§eĐã lấy lại Khung Xương Nhân Tạo khỏi bồn chứa."), true);
                    return true;
                } else {
                    player.displayClientMessage(Component.literal("§7[Bồn Chứa] Hãy cầm §cKhế Ước Ác Ma §7Chuột Phải vào bồn để bắt đầu dung hợp! (Shift+Click tay không để lấy lại khung xương)"), true);
                    return true;
                }
            }
            case DEMON_INFUSED -> {
                // Mở GUI tiến hóa
                if (player instanceof ServerPlayer sp) {
                    ModMessages.sendToPlayer(new ClientboundOpenCapsuleScreenPacket(pos.getX(), pos.getY(), pos.getZ(), capsule.demonType.ordinal()), sp);
                }
                return true;
            }
            case EVOLVING -> {
                player.displayClientMessage(Component.literal("§e✦ Quá trình tái cấu trúc ma thể đang phát sáng chói lòa! Khế ước sẽ tự rớt ra khi hoàn tất..."), true);
                return true;
            }
            case READY_RETRIEVAL -> {
                return true;
            }
        }

        return false;
    }

    private static void insertDemonToCapsule(Player player, ItemStack held, ActiveCapsule capsule, ServerLevel level) {
        capsule.demonType = PrimordialPactItem.getDemonType(held);
        capsule.hasPhysicalBody = ItemStackDataHelper.getBoolean(held, "HasPhysicalBody");
        capsule.isNamed = ItemStackDataHelper.getBoolean(held, "IsNamed");
        capsule.customName = ItemStackDataHelper.getString(held, "CustomDemonName");
        capsule.currentHp = ItemStackDataHelper.getFloat(held, "CurrentHp", (float) capsule.demonType.getMaxHealth());
        capsule.maxHp = ItemStackDataHelper.getFloat(held, "MaxHp", (float) capsule.demonType.getMaxHealth());
        capsule.state = CapsuleState.DEMON_INFUSED;

        // Nếu ác ma đang xuất thế ngoài thế giới, tự động thu hồi vào bồn chứa
        String demonUuidStr = ItemStackDataHelper.getString(held, "DemonUUID");
        if (!demonUuidStr.isEmpty()) {
            try {
                UUID id = UUID.fromString(demonUuidStr);
                net.minecraft.world.entity.Entity e = level.getEntity(id);
                if (e instanceof PrimordialDemonEntity pde && pde.isAlive()) {
                    level.sendParticles(ParticleTypes.PORTAL, pde.getX(), pde.getY() + 1.0D, pde.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);
                    pde.discard();
                }
            } catch (Exception ignored) {}
        }
        for (PrimordialDemonEntity pde : level.getEntitiesOfClass(PrimordialDemonEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(64.0D),
                d -> d.isAlive() && d.isOwnedBy(player) && d.getDemonType() == capsule.demonType)) {
            level.sendParticles(ParticleTypes.PORTAL, pde.getX(), pde.getY() + 1.0D, pde.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);
            pde.discard();
        }

        if (!player.isCreative()) {
            held.shrink(1);
        }

        // Ẩn khung xương đi
        if (capsule.contentDisplay != null) {
            ((ItemDisplayAccessor) capsule.contentDisplay).weapons$setItemStack(ItemStack.EMPTY);
        }

        // Xóa khung xương nếu có
        if (capsule.skeletonEntityInside != null && capsule.skeletonEntityInside.isAlive()) {
            capsule.skeletonEntityInside.discard();
            capsule.skeletonEntityInside = null;
        }

        // Xóa thực thể cũ nếu có
        if (capsule.demonEntityInside != null && capsule.demonEntityInside.isAlive()) {
            capsule.demonEntityInside.discard();
        }

        // Tạo thực thể Ác Ma 3D lơ lửng ngay bên trong bồn chứa!
        Vec3 center = Vec3.atBottomCenterOf(capsule.pos);
        PrimordialDemonEntity demonInside = ModEntities.PRIMORDIAL_DEMON.get().create(level);
        if (demonInside != null) {
            demonInside.moveTo(center.x, center.y + 0.1D, center.z, 0.0F, 0.0F);
            demonInside.setDemonType(capsule.demonType);
            demonInside.setWinged(capsule.demonType == DemonType.NOIR);
            demonInside.setPhysicalBody(capsule.hasPhysicalBody);
            demonInside.setNamed(capsule.isNamed);
            if (capsule.isNamed && !capsule.customName.isEmpty()) {
                demonInside.setCustomDemonName(capsule.customName);
            }
            demonInside.setNoAi(true);
            demonInside.setInvulnerable(true);
            demonInside.setSilent(true);
            demonInside.setNoGravity(true);
            demonInside.addTag("CapsuleDemonDummy");
            level.addFreshEntity(demonInside);
            capsule.demonEntityInside = demonInside;
        }

        IncubationCapsuleBlock.CapsuleContent cc = switch (capsule.demonType) {
            case NOIR -> IncubationCapsuleBlock.CapsuleContent.NOIR;
            case ROUGE -> IncubationCapsuleBlock.CapsuleContent.ROUGE;
            case BLANC -> IncubationCapsuleBlock.CapsuleContent.BLANC;
            case JAUNE -> IncubationCapsuleBlock.CapsuleContent.JAUNE;
            case VIOLET -> IncubationCapsuleBlock.CapsuleContent.VIOLET;
            case BLEU -> IncubationCapsuleBlock.CapsuleContent.BLEU;
            case VERT -> IncubationCapsuleBlock.CapsuleContent.VERT;
        };
        net.minecraft.world.level.block.state.BlockState curState = level.getBlockState(capsule.pos);
        if (curState.is(ModBlocks.INCUBATION_CAPSULE.get())) {
            level.setBlock(capsule.pos, curState.setValue(IncubationCapsuleBlock.CONTENT, cc), 3);
        }

        updateText(capsule,
                "§b§l✦ BỒN CHỨA: §c§l" + capsule.demonType.getColorName().toUpperCase() + " ✦\n" +
                "§a✔ Linh hồn Ác Ma đã hòa nhập vào bồn chứa\n" +
                "§e▶ Chuột Phải vào bồn để thiết lập Tiến Hóa!"
        );

        level.playSound(null, capsule.pos.getX(), capsule.pos.getY(), capsule.pos.getZ(), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.5F, 1.2F);
        level.playSound(null, capsule.pos.getX(), capsule.pos.getY(), capsule.pos.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 2.0F, 1.0F);

        if (player instanceof ServerPlayer sp) {
            ModMessages.sendToPlayer(new ClientboundOpenCapsuleScreenPacket(capsule.pos.getX(), capsule.pos.getY(), capsule.pos.getZ(), capsule.demonType.ordinal()), sp);
        }
    }

    public static void startEvolution(ServerLevel level, BlockPos pos, int choice, String customName) {
        ActiveCapsule capsule = ACTIVE_CAPSULES.get(pos);
        if (capsule == null) return;

        capsule.state = CapsuleState.EVOLVING;
        capsule.evolutionChoice = choice;
        capsule.customName = customName != null && !customName.isEmpty() ? customName : capsule.demonType.getRandomCanonName(RandomSource.create());
        capsule.animTicks = 0;

        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.8F);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0F, 0.9F);
    }

    public static void tickCapsules(ServerLevel level) {
        if (ACTIVE_CAPSULES.isEmpty()) return;
        Iterator<Map.Entry<BlockPos, ActiveCapsule>> it = ACTIVE_CAPSULES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, ActiveCapsule> entry = it.next();
            BlockPos pos = entry.getKey();
            ActiveCapsule c = entry.getValue();

            if (!level.getBlockState(pos).is(ModBlocks.INCUBATION_CAPSULE.get())) {
                c.cleanup();
                it.remove();
                continue;
            }

            if (c.state == CapsuleState.EVOLVING) {
                c.animTicks++;
                double cx = c.pos.getX() + 0.5D;
                double cy = c.pos.getY() + 1.2D;
                double cz = c.pos.getZ() + 0.5D;

                // Hào quang chói lòa 4 giây (80 tick) che khuất hoàn toàn bên trong
                level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 4, 0.2, 0.5, 0.2, 0);
                level.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 25, 0.35, 0.8, 0.35, 0.08);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 20, 0.4, 0.8, 0.4, 0.15);
                level.sendParticles(ParticleTypes.PORTAL, cx, cy, cz, 15, 0.3, 0.5, 0.3, 0.1);

                if (c.animTicks % 15 == 0) {
                    level.playSound(null, cx, cy, cz, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2.0F, 1.2F);
                    level.playSound(null, cx, cy, cz, SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 2.0F, 1.4F);
                }

                updateText(c,
                        "§6§l✦ ĐANG DUNG HỢP NHỤC THỂ & TÁI CẤU TRÚC MA THẦN ✦\n" +
                        "§eTiến độ: §f" + (c.animTicks * 100 / 80) + "% §7(Còn " + String.format("%.1f", (80 - c.animTicks) / 20.0F) + "s)\n" +
                        "§dÁnh sáng ma pháp đang chói lòa!"
                );

                if (c.animTicks >= 80) {
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 3, 0.2, 0.4, 0.2, 0);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 60, 0.6, 1.0, 0.6, 0.1);
                    level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0F, 0.9F);
                    level.playSound(null, cx, cy, cz, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 2.5F, 1.0F);
                    level.playSound(null, cx, cy, cz, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 2.5F, 1.2F);

                    // 1. Tạo vật phẩm khế ước độc bản theo đúng 7 Ác Ma x 3 Trạng Thái (21 vật phẩm)
                    Item evolvedItem = c.demonType.getPactItemForTier(c.evolutionChoice).get();
                    ItemStack resultStack = new ItemStack(evolvedItem);
                    float multiplier = (c.evolutionChoice == 3) ? 8.0F : 5.0F;
                    float finalMaxHp = (float) c.demonType.getMaxHealth() * multiplier;
                    int numSkills = (c.evolutionChoice == 3) ? 5 : 3;

                    boolean givesBody = (c.evolutionChoice == 1 || c.evolutionChoice == 3);
                    boolean givesName = (c.evolutionChoice == 2 || c.evolutionChoice == 3);

                    ItemStackDataHelper.putString(resultStack, "DemonType", c.demonType.name());
                    ItemStackDataHelper.putBoolean(resultStack, "HasPhysicalBody", givesBody);
                    ItemStackDataHelper.putBoolean(resultStack, "IsNamed", givesName);
                    ItemStackDataHelper.putString(resultStack, "CustomDemonName", c.customName);
                    ItemStackDataHelper.putInt(resultStack, "EvolutionTier", c.evolutionChoice);
                    ItemStackDataHelper.putFloat(resultStack, "MaxHp", finalMaxHp);
                    ItemStackDataHelper.putFloat(resultStack, "CurrentHp", finalMaxHp);

                    List<PrimordialSkillPool.SkillEntry> skills = PrimordialSkillPool.rollRandomSkills(numSkills, level.getRandom());
                    StringBuilder sb = new StringBuilder();
                    for (PrimordialSkillPool.SkillEntry sk : skills) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(sk.id());
                    }
                    ItemStackDataHelper.putString(resultStack, "PrimordialSkills", sb.toString());

                    // 2. Rớt cuốn khế ước độc bản ra mặt đất trước bồn chứa
                    ItemEntity drop = new ItemEntity(level, cx, cy - 0.4D, cz, resultStack);
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);

                    // 3. Thông báo toàn cõi (Voice of the World)
                    for (ServerPlayer p : level.players()) {
                        if (p.distanceToSqr(cx, cy, cz) <= 64.0 * 64.0) {
                            VoiceOfTheWorld.announceDemonEvolution(p, c.demonType, c.customName, c.evolutionChoice, multiplier);
                        }
                    }

                    // 4. Xóa các thực thể dummy bên trong bồn chứa
                    if (c.demonEntityInside != null && c.demonEntityInside.isAlive()) {
                        c.demonEntityInside.discard();
                        c.demonEntityInside = null;
                    }
                    if (c.skeletonEntityInside != null && c.skeletonEntityInside.isAlive()) {
                        c.skeletonEntityInside.discard();
                        c.skeletonEntityInside = null;
                    }

                    // 5. Reset bồn chứa về trạng thái EMPTY để tiếp tục nuôi cấy lần sau
                    c.state = CapsuleState.EMPTY;
                    c.evolutionChoice = 0;
                    c.hasPhysicalBody = false;
                    c.isNamed = false;
                    c.customName = "";

                    if (c.contentDisplay != null) {
                        ((ItemDisplayAccessor) c.contentDisplay).weapons$setItemStack(ItemStack.EMPTY);
                    }

                    net.minecraft.world.level.block.state.BlockState curState = level.getBlockState(c.pos);
                    if (curState.is(ModBlocks.INCUBATION_CAPSULE.get())) {
                        level.setBlock(c.pos, curState.setValue(IncubationCapsuleBlock.CONTENT, IncubationCapsuleBlock.CapsuleContent.EMPTY), 3);
                    }

                    updateText(c,
                            "§b§l✦ BỒN CHỨA THỂ XÁC NHÂN TẠO ✦\n" +
                            "§7Trạng thái: §fChưa có Khung Xương\n" +
                            "§e▶ Cầm [Khung Xương Nhân Tạo] Chuột Phải để nạp vào bồn!"
                    );
                }
            }
        }
    }

    private static void updateText(ActiveCapsule capsule, String text) {
        if (capsule.textDisplay != null && capsule.textDisplay.isAlive()) {
            ((TextDisplayAccessor) capsule.textDisplay).weapons$setText(Component.literal(text));
        }
    }
}
