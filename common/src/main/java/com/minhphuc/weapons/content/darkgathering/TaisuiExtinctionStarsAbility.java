package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.content.tensura.TensuraEvents;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import com.minhphuc.weapons.network.ClientboundSyncTaisuiPacket;
import com.minhphuc.weapons.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import com.minhphuc.weapons.content.tensura.DeathStreakAbility;
import com.minhphuc.weapons.content.tensura.PentagramCelestialPillarAbility;
import com.minhphuc.weapons.content.tensura.ResidualMagicCircleManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Kỹ Năng Thần Cấp: Phẫn Nộ Vương - Tuyệt Diệt Tinh Tú (Extinction Stars / 泰歳星君)
 * Sức mạnh phỏng theo Thần Tối Cao Thái Tuế Tinh Quân trong anime/manga Dark Gathering:
 * - Giai đoạn 1 (0 -> 40 ticks): Quả Cầu Mắt Khổng Lồ u ám xuất hiện sau lưng, co nhỏ dần vào người.
 * - Giai đoạn 2 (3 Phút / 3600 ticks): 5 Tinh Tú Trắng Siêu Tốc bay elip quanh người (Atomic Orbitals).
 *   + Hào quang hộ thể: Mob chạm vào trong 1.5 blocks mất 50% Máu Hiện Tại + đẩy lùi.
 *   + Bắn Tinh Tú: Nhấn Chuột Trái phóng viên tinh tú (Tất sát mọi quái vật, phá hủy 1 block trúng phải).
 *   + Khóa mọi kỹ năng khác trong suốt 3 phút duy trì!
 */
public class TaisuiExtinctionStarsAbility {

    public static class ActiveTaisuiState {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public int chargeTicks = 0;
        public boolean isCharged = false;
        public int remainingTicks = 3600; // 3 phút duy trì
        public int starsRemaining = 5;

        public Display.ItemDisplay eyePlanetDisplay = null;

        public ActiveTaisuiState(ServerLevel level, ServerPlayer caster) {
            this.level = level;
            this.caster = caster;
        }

        public void cleanupDisplay() {
            if (eyePlanetDisplay != null && eyePlanetDisplay.isAlive()) {
                eyePlanetDisplay.discard();
                eyePlanetDisplay = null;
            }
        }
    }

    public static final Map<UUID, ActiveTaisuiState> ACTIVE_TAISUI = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final DustParticleOptions STARLIGHT_WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.2F);
    private static final DustParticleOptions NEON_MAGENTA_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.55F), 1.8F);
    private static final DustParticleOptions VOID_BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.0F, 0.1F), 2.0F);

    /**
     * Kiểm tra người chơi có đang trong trạng thái Tuyệt Diệt Tinh Tú không
     */
    public static boolean isTaisuiActive(Player player) {
        if (player == null) return false;
        return ACTIVE_TAISUI.containsKey(player.getUUID());
    }

    /**
     * Kích hoạt Phẫn Nộ Vương: Tuyệt Diệt Tinh Tú
     */
    public static boolean cast(ServerLevel level, ServerPlayer player) {
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        if (!isTrueDemonLord) {
            player.displayClientMessage(
                Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Yêu cầu Thức Tỉnh Chân Ma Vương để khai phóng Tuyệt Diệt Tinh Tú!"),
                true
            );
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.5F);
            return false;
        }

        if (ACTIVE_TAISUI.containsKey(player.getUUID())) {
            player.displayClientMessage(
                Component.literal("§c⚠️ Tuyệt Diệt Tinh Tú đang trong trạng thái kích hoạt!"),
                true
            );
            return false;
        }

        ActiveTaisuiState state = new ActiveTaisuiState(level, player);

        // 1. Tạo Quả Cầu Mắt 3D Khổng Lồ phía sau lưng người chơi
        Vec3 look = player.getLookAngle();
        Vec3 spawnPos = player.position().add(look.scale(-4.0D)).add(0, 3.2D, 0);

        Display.ItemDisplay planet = EntityType.ITEM_DISPLAY.create(level);
        if (planet != null) {
            planet.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) planet;
            DisplayAccessor dispAcc = (DisplayAccessor) planet;

            itemAcc.weapons$setItemStack(new ItemStack(ModItems.TAISUI_EYE_PLANET.get()));
            itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            planet.setGlowingTag(true);
            dispAcc.weapons$setGlowColorOverride(0xFF0055);
            dispAcc.weapons$setViewRange(4.0F);

            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(8.0F, 8.0F, 8.0F),
                    null
            ));

            level.addFreshEntity(planet);
            state.eyePlanetDisplay = planet;
        }

        ACTIVE_TAISUI.put(player.getUUID(), state);

        // 2. Gửi Title & Subtitle
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§l✦ TUYỆT DIỆT TINH TÚ ✦")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e§l5 Tinh Tú Tuyệt Diệt Hộ Thể")));
        }

        // 3. Âm thanh nhịp tim đập kinh dị & tiếng hú hư không
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 5.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 3.0F, 1.2F);

        // Kiểm tra kích hoạt trạng thái Cộng Hưởng Thái Tuế nếu Thị Nhục đang bật
        checkAndApplySynergy(level, player);

        return true;
    }

    /**
     * Vòng lặp Server Tick cập nhật hoạt ảnh và trạng thái Tinh Tú
     */
    public static void tickTaisuiStates(ServerLevel level) {
        if (ACTIVE_TAISUI.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveTaisuiState>> it = ACTIVE_TAISUI.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveTaisuiState> entry = it.next();
            ActiveTaisuiState state = entry.getValue();

            if (state.level != level) continue;

            if (state.caster == null || !state.caster.isAlive() || state.caster.hasDisconnected()) {
                cleanupSynergy(state.caster);
                syncTaisuiToAll(level, entry.getKey(), false, 0);
                state.cleanupDisplay();
                it.remove();
                continue;
            }

            ServerPlayer caster = state.caster;

            // =========================================================================
            // GIAI ĐOẠN 1: QUẢ CẦU MẮT THU NHỎ VÀO CƠ THỂ (0 -> 40 ticks / 2.0s)
            // =========================================================================
            if (!state.isCharged) {
                state.chargeTicks++;
                float progress = (float) state.chargeTicks / 40.0F;

                if (state.eyePlanetDisplay != null && state.eyePlanetDisplay.isAlive()) {
                    Vec3 look = caster.getLookAngle();
                    // Di chuyển từ sau lưng 4m dần dần áp sát vào lưng người chơi
                    double distBack = 4.0D * (1.0D - progress) + 0.8D;
                    double height = 3.2D * (1.0D - progress) + 1.2D;
                    Vec3 currentPos = caster.position().add(look.scale(-distBack)).add(0, height, 0);
                    state.eyePlanetDisplay.moveTo(currentPos.x, currentPos.y, currentPos.z, 0.0F, 0.0F);

                    // Kích thước thu nhỏ từ 8.0m xuống 0.8m
                    float scale = 8.0F * (1.0F - progress) + 0.8F;
                    ((DisplayAccessor) state.eyePlanetDisplay).weapons$setTransformation(new Transformation(
                            new Vector3f(0.0F, 0.0F, 0.0F),
                            new Quaternionf().rotateY((float) Math.toRadians(state.chargeTicks * 9.0F)),
                            new Vector3f(scale, scale, scale),
                            null
                    ));
                }

                // Hạt ma tố mắt hư không xoáy tụ
                Vec3 chest = caster.position().add(0, 1.2D, 0);
                for (int i = 0; i < 6; i++) {
                    double theta = (state.chargeTicks * 0.4D) + (i * Math.PI / 3.0D);
                    double r = 3.5D * (1.0D - progress) + 0.5D;
                    double px = chest.x + Math.cos(theta) * r;
                    double py = chest.y + (RANDOM.nextDouble() - 0.5D) * 1.5D;
                    double pz = chest.z + Math.sin(theta) * r;
                    level.sendParticles(VOID_BLACK_DUST, px, py, pz, 1, 0, 0, 0, 0);
                    level.sendParticles(NEON_MAGENTA_DUST, px, py, pz, 1, 0, 0, 0, 0);
                }

                if (state.chargeTicks >= 40) {
                    state.isCharged = true;
                    state.cleanupDisplay();
                    syncTaisuiToAll(level, caster.getUUID(), true, 5);

                    level.sendParticles(ParticleTypes.FLASH, chest.x, chest.y, chest.z, 4, 1.0D, 1.0D, 1.0D, 0);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F, 1.6F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 3.0F, 1.2F);

                    caster.displayClientMessage(
                        Component.literal("§e§l✦ 5 Tinh Tú Tuyệt Diệt đã thành hình! §f(§e[Chuột Trái] §fđể phóng bắn)"),
                        true
                    );
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 2: 5 TINH TÚ TRẮNG QUAY QUANH NGƯỜI (Atomic Orbitals: 3 Phút)
            // =========================================================================
            else {
                state.remainingTicks--;

                // Nếu đang trong trạng thái Cộng Hưởng Thái Tuế
                if (EntityDataHelper.getCustomData(caster).getBoolean("TaisuiSynergyActive")) {
                    // Nếu tay chính đang trống và tay phụ có Thị Nhục -> tự ngưng tụ Ngôi Sao Tuyệt Diệt trên tay chính
                    if (caster.getMainHandItem().isEmpty() && caster.getOffhandItem().is(ModItems.SEER_FLESH_ARM.get())) {
                        caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXTINCTION_STAR.get()));
                        EntityDataHelper.getCustomData(caster).putBoolean("TaisuiSynergyGrantedMainhand", true);
                    }
                }

                // Kiểm tra nếu đã bắn hết 5 viên tinh tú -> Kết thúc ngay lập tức không cần chờ hết thời gian!
                if (state.starsRemaining <= 0) {
                    cleanupSynergy(caster);
                    syncTaisuiToAll(level, caster.getUUID(), false, 0);
                    it.remove();
                    continue;
                }

                // Hào quang hộ thể: Mob chạm vào hoặc cách 1.5 blocks mất 50% HP + đẩy lùi (chạy mỗi 4 ticks để giảm 75% tải getEntitiesOfClass)
                if (state.remainingTicks % 4 == 0) {
                    AABB auraBox = caster.getBoundingBox().inflate(1.6D);
                    List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, auraBox,
                            e -> e != caster && e.isAlive());

                    for (LivingEntity mob : nearbyMobs) {
                        if (mob.distanceTo(caster) <= 2.2D && mob.invulnerableTime <= 10) {
                            float damage = Math.max(25.0F, mob.getHealth() * 0.50F);
                            mob.hurt(level.damageSources().playerAttack(caster), damage);
                            mob.invulnerableTime = 20; // Tránh trừ máu quá dày mỗi tick

                            // Đẩy lùi mạnh ra xa
                            Vec3 push = mob.position().subtract(caster.position()).normalize().add(0, 0.4D, 0).scale(2.2D);
                            mob.setDeltaMovement(push);

                            level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.1D);
                            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F, 1.8F);
                        }
                    }

                    // Hào quang hộ thể phá vỡ các vòng ma thuật lơ lửng nếu người chơi áp sát trong phạm vi 3.5m
                    AABB circleAuraBox = caster.getBoundingBox().inflate(3.5D);
                    List<Entity> nearbyCircles = level.getEntities((Entity) null, circleAuraBox,
                            e -> e.isAlive() && e != caster && e != state.eyePlanetDisplay && isMagicCircleEntity(e));
                    for (Entity circle : nearbyCircles) {
                        shatterMagicCircle(level, circle, caster);
                    }
                }

                // Hết thời gian 3 phút
                if (state.remainingTicks <= 0) {
                    cleanupSynergy(caster);
                    syncTaisuiToAll(level, caster.getUUID(), false, 0);
                    caster.displayClientMessage(
                        Component.literal("§7✦ Tuyệt Diệt Tinh Tú đã tiêu tán năng lượng."),
                        true
                    );
                    it.remove();
                }
            }
        }
    }

    /**
     * Bắn 1 viên tinh tú tấn công (Tất sát quái vật, phá hủy 1 block)
     */
    public static boolean fireStar(ServerLevel level, ServerPlayer player) {
        ActiveTaisuiState state = ACTIVE_TAISUI.get(player.getUUID());
        boolean hasHeldStar = player.getMainHandItem().is(ModItems.EXTINCTION_STAR.get()) || player.getOffhandItem().is(ModItems.EXTINCTION_STAR.get());

        if (state == null || !state.isCharged || state.starsRemaining <= 0) {
            if (hasHeldStar) {
                fireStarBeam(level, player);
                return true;
            }
            return false;
        }

        state.starsRemaining--;
        fireStarBeam(level, player);

        // 5. Kiểm tra: Nếu bắn hết 5 viên thì kết thúc chiêu ngay lập tức không cần chờ 3 phút!
        if (state.starsRemaining <= 0) {
            cleanupSynergy(player);
            ACTIVE_TAISUI.remove(player.getUUID());
            syncTaisuiToAll(level, player.getUUID(), false, 0);
            player.displayClientMessage(
                Component.literal("§7✦ Đã giải phóng toàn bộ 5 Tinh Tú Tuyệt Diệt. Kỹ năng đã hoàn tất!"),
                true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.5F);
        } else {
            syncTaisuiToAll(level, player.getUUID(), true, state.starsRemaining);
            player.displayClientMessage(
                Component.literal("§e§l✦ Bắn Tinh Tú Tuyệt Diệt! §f(Còn lại: " + state.starsRemaining + "/5)"),
                true
            );
        }

        return true;
    }

    /**
     * Thi triển chùm tia Tinh Tú xuyên thấu địa hình và hủy diệt mục tiêu
     */
    /**
     * Thi triển chùm tia Tinh Tú xuyên thấu địa hình và hủy diệt mục tiêu & bắn phá vòng ma thuật lơ lửng
     */
    public static void fireStarBeam(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 90.0D;
        Vec3 targetEnd = eyePos.add(look.scale(maxDist));

        // Raycast kiểm tra va chạm block
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, targetEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        Vec3 hitPos = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : targetEnd;
        double travelDist = eyePos.distanceTo(hitPos);

        // 1. Đường bay của viên tinh tú (Starlight Trail)
        for (double d = 0.5D; d < travelDist; d += 0.5D) {
            Vec3 p = eyePos.add(look.scale(d));
            level.sendParticles(STARLIGHT_WHITE_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0.02D);
        }

        // 2. Phá hủy khối block bị trúng phải
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = blockHit.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (!bs.isAir() && bs.getDestroySpeed(level, bp) >= 0.0F) {
                level.destroyBlock(bp, false);
            }
        }

        // 3. Quét tất sát mọi sinh vật trúng phải trên đường đạn
        AABB rayBounds = new AABB(eyePos, hitPos).inflate(1.8D);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, rayBounds,
                e -> e != player && e.isAlive());

        for (LivingEntity victim : victims) {
            TensuraEvents.handleMobDeathDrop(player, victim);
            victim.hurt(level.damageSources().playerAttack(player), 99999.0F);
            if (victim.isAlive()) {
                victim.discard();
            }
            level.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0, 0, 0, 0);
        }

        // 4. BẮN PHÁ TOÀN BỘ VÒNG MA THUẬT LƠ LỬNG TRÊN ĐƯỜNG ĐẠN VÀ KHU VỰC TRÚNG
        destroyMagicCirclesOnPath(level, player, eyePos, targetEnd);

        // 5. Âm thanh phóng đạn tinh tú
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 3.5F, 1.8F);
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5F, 1.9F);
    }

    /**
     * Kiểm tra thực thể có phải là một vòng ma thuật lơ lửng hay không
     */
    public static boolean isMagicCircleEntity(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;

        // 1. Kiểm tra nếu là Display.ItemDisplay
        if (entity instanceof Display.ItemDisplay itemDisplay) {
            // Kiểm tra thẻ tag
            for (String tag : itemDisplay.getTags()) {
                String lower = tag.toLowerCase();
                if (lower.contains("circle") || lower.contains("demon") ||
                    lower.contains("streak") || lower.contains("disintegration") ||
                    lower.contains("pillar") || lower.contains("array") ||
                    lower.contains("halo") || lower.contains("magic")) {
                    return true;
                }
            }

            ItemStack stack = ((ItemDisplayAccessor) itemDisplay).weapons$getItemStack();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item == ModItems.MAGIC_CIRCLE_NOIR.get() ||
                    item == ModItems.MAGIC_CIRCLE_ROUGE.get() ||
                    item == ModItems.MAGIC_CIRCLE_BLANC.get() ||
                    item == ModItems.MAGIC_CIRCLE_JAUNE.get() ||
                    item == ModItems.MAGIC_CIRCLE_JAUNE_DESTRUCTION.get() ||
                    item == ModItems.MAGIC_CIRCLE_JAUNE_NUCLEAR.get() ||
                    item == ModItems.MAGIC_CIRCLE_VIOLET.get() ||
                    item == ModItems.MAGIC_CIRCLE_BLEU.get() ||
                    item == ModItems.MAGIC_CIRCLE_VERT.get() ||
                    item == ModItems.DEMON_SUMMONING_CIRCLE.get() ||
                    item == ModItems.DISINTEGRATION_MAGIC_CIRCLE.get() ||
                    item == ModItems.BEELZEBUTH_MAGIC_CIRCLE.get() ||
                    item == ModItems.DISINTEGRATION_VERTICAL_CREST.get() ||
                    item == ModItems.DISINTEGRATION_LIGHT_BEAM.get() ||
                    item == ModItems.DISINTEGRATION_SHOCKWAVE.get() ||
                    item == ModItems.JACOB_LIGHT_PILLAR.get() ||
                    item == ModItems.LIUREN_MAGIC_ARRAY.get() ||
                    item == ModItems.BEELZEBUTH_DRAGON_MAW.get() ||
                    item == ModItems.ALKAID_SPHERE.get() ||
                    item == ModItems.ALKAID_VORTEX.get()) {
                    return true;
                }
                String path = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
                if (path.contains("circle") || path.contains("magic") ||
                    path.contains("pillar") || path.contains("array") ||
                    path.contains("crest") || path.contains("halo") ||
                    path.contains("disintegration") || path.contains("beam") ||
                    path.contains("streak")) {
                    return true;
                }
            }

            if (ResidualMagicCircleManager.isResidualMagicCircle(itemDisplay)) {
                return true;
            }
        }

        // 2. Kiểm tra nếu là bất kỳ Display nào mang nhãn ma thuật
        if (entity instanceof Display display) {
            for (String tag : display.getTags()) {
                String lower = tag.toLowerCase();
                if (lower.contains("circle") || lower.contains("magic") || lower.contains("demon") ||
                    lower.contains("disintegration") || lower.contains("streak") || lower.contains("pillar")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Bắn nổ tung và vỡ vụn vòng ma thuật
     */
    public static void shatterMagicCircle(ServerLevel level, Entity circle, ServerPlayer player) {
        if (circle == null || !circle.isAlive()) return;

        Vec3 pos = circle.position();

        // 1. Hiệu ứng hạt nổ tung vỡ nát ma pháp trận cực kỳ hoành tráng
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 2, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 45, 1.5D, 1.5D, 1.5D, 0.25D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 35, 1.2D, 1.2D, 1.2D, 0.15D);
        level.sendParticles(STARLIGHT_WHITE_DUST, pos.x, pos.y, pos.z, 60, 2.0D, 2.0D, 2.0D, 0.2D);
        level.sendParticles(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, 30, 1.0D, 1.0D, 1.0D, 0.2D);

        // 2. Âm thanh kính vỡ ma thuật và bộc nổ rền vang
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.5F, 0.6F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 1.4F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.8F);

        // 3. Huỷ thực thể trong các trình quản lý kỹ năng
        if (circle instanceof Display.ItemDisplay itemDisplay) {
            ResidualMagicCircleManager.removeCircle(itemDisplay);
            DeathStreakAbility.destroyCircleIfMatches(itemDisplay);
            PentagramCelestialPillarAbility.destroyCircleIfMatches(itemDisplay);
        }

        // 4. Xóa sổ entity khỏi thế giới
        circle.discard();
    }

    /**
     * Quét và bắn phá các vòng ma thuật trên đường bay của chùm tia Tinh Tú
     */
    public static int destroyMagicCirclesOnPath(ServerLevel level, ServerPlayer player, Vec3 eyePos, Vec3 targetEnd) {
        AABB searchBox = new AABB(eyePos, targetEnd).inflate(16.0D);
        List<Entity> entities = level.getEntities((Entity) null, searchBox, e -> e.isAlive() && e != player);

        Vec3 ab = targetEnd.subtract(eyePos);
        double abLenSq = ab.lengthSqr();
        if (abLenSq < 1.0E-4) return 0;

        List<Entity> directHits = new ArrayList<>();
        ActiveTaisuiState casterState = ACTIVE_TAISUI.get(player.getUUID());

        for (Entity e : entities) {
            if (casterState != null && e == casterState.eyePlanetDisplay) continue;

            if (isMagicCircleEntity(e)) {
                Vec3 p = e.position();
                Vec3 ap = p.subtract(eyePos);
                double t = ap.dot(ab) / abLenSq;
                t = Math.max(0.0, Math.min(1.0, t));
                Vec3 closestPoint = eyePos.add(ab.scale(t));
                double dist = closestPoint.distanceTo(p);

                // Nếu tia đạn bắn qua trong cự ly 8.5m quanh tâm vòng ma thuật
                if (dist <= 8.5D) {
                    directHits.add(e);
                }
            }
        }

        if (directHits.isEmpty()) return 0;

        // Kích hoạt phản ứng nổ liên hoàn: Vòng bị bắn vỡ sẽ phát nổ lan sang các vòng ma thuật lân cận trong bán kính 12m
        Set<Entity> allCirclesToDestroy = new HashSet<>(directHits);
        for (Entity c : directHits) {
            AABB chainBox = c.getBoundingBox().inflate(12.0D);
            List<Entity> neighbors = level.getEntities((Entity) null, chainBox,
                    ne -> ne.isAlive() && ne != player && (casterState == null || ne != casterState.eyePlanetDisplay));
            for (Entity ne : neighbors) {
                if (isMagicCircleEntity(ne)) {
                    allCirclesToDestroy.add(ne);
                }
            }
        }

        int count = 0;
        for (Entity circle : allCirclesToDestroy) {
            shatterMagicCircle(level, circle, player);
            count++;
        }

        player.displayClientMessage(
            Component.literal("§e§l✦ Tuyệt Diệt Tinh Tú đã bắn tan nát " + count + " Ma Pháp Trận!"),
            true
        );

        return count;
    }

    /**
     * Kích hoạt hoặc kiểm tra trạng thái Cộng Hưởng: Thị Nhục Bọc Tay + Tuyệt Diệt Tinh Tú
     */
    public static void checkAndApplySynergy(ServerLevel level, ServerPlayer player) {
        if (isTaisuiActive(player) && SeerFleshAbility.isFleshActive(player)) {
            CompoundTag data = EntityDataHelper.getCustomData(player);
            if (!data.getBoolean("TaisuiSynergyActive")) {
                data.putBoolean("TaisuiSynergyActive", true);

                // Trang bị Thị Nhục Bọc Tay cho tay phụ nếu chưa cầm
                ItemStack offhand = player.getOffhandItem();
                if (!offhand.is(ModItems.SEER_FLESH_ARM.get())) {
                    if (!offhand.isEmpty()) {
                        data.put("TaisuiSavedOffhand", offhand.save(player.level().registryAccess()));
                    }
                    ItemStack fleshArm = new ItemStack(ModItems.SEER_FLESH_ARM.get());
                    SeerFleshArmItem.setEyes(fleshArm, 3);
                    player.setItemInHand(InteractionHand.OFF_HAND, fleshArm);
                    data.putBoolean("TaisuiSynergyGrantedOffhand", true);
                }

                // Trang bị Ngôi Sao Tuyệt Diệt cho tay chính nếu đang để trống
                if (player.getMainHandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXTINCTION_STAR.get()));
                    data.putBoolean("TaisuiSynergyGrantedMainhand", true);
                }

                // Hiệu ứng bùng nổ cộng hưởng thần tính
                if (player.connection != null) {
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§2§l✦ CỘNG HƯỞNG THÁI TUẾ ✦")));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§aThị Nhục 3 Mắt §f& §eTuyệt Diệt Tinh Tú")));
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.2F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 2.5F, 1.4F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 3.5F, 0.8F);

                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2D, player.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.2D);
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2D, player.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);

                player.displayClientMessage(
                        Component.literal("§a§l✦ CỘNG HƯỞNG THÁI TUẾ THÀNH CÔNG! §fTay trái bọc §2Thị Nhục 3 Mắt §f(Chuột phải hồi máu), tay phải ngưng tụ §eNgôi Sao Tuyệt Diệt!"),
                        false
                );
            }
        }
    }

    /**
     * Dọn dẹp trạng thái cộng hưởng khi kỹ năng Tuyệt Diệt Tinh Tú kết thúc
     */
    public static void cleanupSynergy(ServerPlayer player) {
        if (player == null) return;
        CompoundTag data = EntityDataHelper.getCustomData(player);
        if (data.getBoolean("TaisuiSynergyActive")) {
            data.putBoolean("TaisuiSynergyActive", false);

            if (data.getBoolean("TaisuiSynergyGrantedOffhand")) {
                if (player.getOffhandItem().is(ModItems.SEER_FLESH_ARM.get())) {
                    if (data.contains("TaisuiSavedOffhand")) {
                        CompoundTag saved = data.getCompound("TaisuiSavedOffhand");
                        ItemStack restored = ItemStack.parseOptional(player.level().registryAccess(), saved);
                        player.setItemInHand(InteractionHand.OFF_HAND, restored);
                        data.remove("TaisuiSavedOffhand");
                    } else {
                        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    }
                }
                data.remove("TaisuiSynergyGrantedOffhand");
            }

            if (data.getBoolean("TaisuiSynergyGrantedMainhand")) {
                if (player.getMainHandItem().is(ModItems.EXTINCTION_STAR.get())) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                data.remove("TaisuiSynergyGrantedMainhand");
            }
        }
    }

    public static void syncTaisuiToAll(ServerLevel level, UUID playerUuid, boolean active, int starsRemaining) {
        ClientboundSyncTaisuiPacket packet = new ClientboundSyncTaisuiPacket(playerUuid, active, starsRemaining);
        ModMessages.sendToPlayers(level.players(), packet);
    }
}
