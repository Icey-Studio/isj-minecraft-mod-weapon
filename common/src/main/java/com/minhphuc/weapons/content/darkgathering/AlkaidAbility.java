package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.minhphuc.weapons.content.tensura.TensuraEvents;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundCinematicSkillPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.math.Transformation;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Kỹ Năng Tất Sát Thần Cấp: Diệt Thế Tà Tinh - Alkaid (滅亡邪星・ALKAID / 搖光 Dao Quang)
 * Sức mạnh tối thượng của Thần Tối Cao Thái Tuế Tinh Quân (Dark Gathering Manga):
 * - Điều kiện: Đang kích hoạt Tuyệt Diệt Tinh Tú!
 * - Giai đoạn 1 (0 -> 160 ticks / 8 Giây): Tụ năng lượng các hạt tinh tú vào đỉnh đầu,
 *   tạo ra Quả Cầu Tinh Tú Khổng Lồ 14m ("Độ nén thấp phân rã") che rợp bầu trời.
 * - Giai đoạn 2 (161 -> 210 ticks): Khai hỏa ALKAID! Quả cầu lao xoáy ốc về phía trước,
 *   khoan thủng và bốc hơi hoàn toàn mọi khối block tạo rãnh lòng máng khổng lồ 12 blocks,
 *   tất sát vạn vật trên đường đi!
 * - Giai đoạn 3: Siêu Tân Tinh hủy diệt tại điểm cuối.
 */
public class AlkaidAbility {

    public static class ActiveAlkaid {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public int currentTick = 0;
        public final int chargeTicks = 160; // 8.0 giây tụ lực
        public int flightTicks = 0;
        public final int maxFlightTicks = 50; // Bay 50 ticks * 3.0 = 150 blocks

        public boolean isFiring = false;
        public Vec3 projectilePos = null;
        public Vec3 launchDir = null;

        public Display.ItemDisplay sphereDisplay = null;
        public Display.ItemDisplay vortexDisplay = null;

        public ActiveAlkaid(ServerLevel level, ServerPlayer caster) {
            this.level = level;
            this.caster = caster;
        }

        public void cleanup() {
            if (sphereDisplay != null && sphereDisplay.isAlive()) {
                sphereDisplay.discard();
                sphereDisplay = null;
            }
            if (vortexDisplay != null && vortexDisplay.isAlive()) {
                vortexDisplay.discard();
                vortexDisplay = null;
            }
        }
    }

    public static final List<ActiveAlkaid> ACTIVE_ALKAIDS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private static final DustParticleOptions STARLIGHT_WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.5F);
    private static final DustParticleOptions CYAN_COSMIC_DUST = new DustParticleOptions(new Vector3f(0.5F, 0.9F, 1.0F), 2.0F);
    private static final DustParticleOptions VOID_PURPLE_DUST = new DustParticleOptions(new Vector3f(0.15F, 0.05F, 0.25F), 2.2F);
    private static final DustParticleOptions GOLDEN_FLARE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.4F), 2.0F);

    /**
     * Kích hoạt chiêu tất sát Diệt Thế Tà Tinh: Alkaid
     */
    public static boolean cast(ServerLevel level, ServerPlayer player) {
        // 1. Kiểm tra điều kiện: Bắt buộc đang bật Tuyệt Diệt Tinh Tú!
        if (!TaisuiExtinctionStarsAbility.isTaisuiActive(player)) {
            player.displayClientMessage(
                    Component.literal("§c⚠️ Yêu cầu kích hoạt trạng thái Tuyệt Diệt Tinh Tú trước khi khai phóng Diệt Thế Tà Tinh: Alkaid!"),
                    true
            );
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.5F);
            return false;
        }

        // 2. Kiểm tra nếu người chơi đang tụ chiêu Alkaid
        for (ActiveAlkaid a : ACTIVE_ALKAIDS) {
            if (a.caster.getUUID().equals(player.getUUID())) {
                player.displayClientMessage(
                        Component.literal("§e⚠️ Diệt Thế Tà Tinh: Alkaid đang trong tiến trình khai hỏa!"),
                        true
                    );
                return false;
            }
        }

        ActiveAlkaid alkaid = new ActiveAlkaid(level, player);

        Vec3 spawnPos = player.position().add(0, 10.0D, 0);
        boolean cinematic = SkillVisualConfig.isCinematicEnabled(SkillVisualConfig.SKILL_ALKAID);
        if (cinematic) {
            ModMessages.sendToPlayers(level.players(), new ClientboundCinematicSkillPacket(
                    SkillVisualConfig.SKILL_ALKAID, player.getUUID(), spawnPos, player.getLookAngle(), 210, ClientboundCinematicSkillPacket.ACTION_START, 1.0f
            ));
            alkaid.sphereDisplay = null;
        } else {
            // 3. Tạo Quả Cầu Tinh Tú 3D nhỏ xuất hiện cao trên trời (Ảnh 1: 0.5m)
            Display.ItemDisplay sphere = EntityType.ITEM_DISPLAY.create(level);
            if (sphere != null) {
                sphere.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0.0F, 0.0F);
                ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) sphere;
                DisplayAccessor dispAcc = (DisplayAccessor) sphere;

                itemAcc.weapons$setItemStack(new ItemStack(ModItems.ALKAID_SPHERE.get()));
                itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
                dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
                sphere.setGlowingTag(true);
                dispAcc.weapons$setGlowColorOverride(0xFFFFFF); // Trắng sáng chói lọi
                dispAcc.weapons$setViewRange(6.0F);

                dispAcc.weapons$setTransformation(new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        new Quaternionf(),
                        new Vector3f(0.8F, 0.8F, 0.8F),
                        null
                ));

                level.addFreshEntity(sphere);
                alkaid.sphereDisplay = sphere;
            }
        }

        ACTIVE_ALKAIDS.add(alkaid);

        // 4. Title mở màn
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§l✦ DIỆT THẾ TÀ TINH... ✦")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e§lĐang tụ lực thiên thể (8 Giây)")));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 4.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 0.6F);

        player.displayClientMessage(
                Component.literal("§c§l[DIỆT THẾ TÀ TINH: ALKAID] §fĐang tụ lực các hạt tinh tú vào quả cầu đỉnh đầu! (8 Giây)"),
                true
        );

        return true;
    }

    /**
     * Vòng lặp Server Tick cập nhật tụ lực 8s, phóng xoáy và đào rãnh địa hình
     */
    public static void tickAlkaids(ServerLevel level) {
        if (ACTIVE_ALKAIDS.isEmpty()) return;

        Iterator<ActiveAlkaid> it = ACTIVE_ALKAIDS.iterator();
        while (it.hasNext()) {
            ActiveAlkaid alkaid = it.next();

            if (alkaid.level != level) continue;

            if (alkaid.caster == null || !alkaid.caster.isAlive() || alkaid.caster.hasDisconnected()) {
                alkaid.cleanup();
                it.remove();
                continue;
            }

            ServerPlayer caster = alkaid.caster;
            alkaid.currentTick++;
            int t = alkaid.currentTick;

            // =========================================================================
            // GIAI ĐOẠN 1: TỤ LỰC 8 GIÂY (0 -> 160 ticks) - Ảnh 1 & 2
            // =========================================================================
            if (!alkaid.isFiring) {
                // Giữ người chơi không bị quái ngắt chiêu
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 4, false, false, true));
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false, true));

                // Vị trí quả cầu lơ lửng ngay trên đỉnh đầu
                Vec3 spherePos = caster.position().add(0, 11.5D, 0);
                if (alkaid.sphereDisplay != null && alkaid.sphereDisplay.isAlive()) {
                    alkaid.sphereDisplay.moveTo(spherePos.x, spherePos.y, spherePos.z, 0.0F, 0.0F);

                    // Quy mô mở rộng: từ 0.8m lên tới 14.0m (Ảnh 2: Quả Cầu Khổng Lồ Che Kín Bầu Trời)
                    float progress = (float) t / (float) alkaid.chargeTicks;
                    float scale = 0.8F + (progress * progress) * 13.2F;

                    // Xoay tròn liên tục cuộn xoáy ma thuật
                    Quaternionf rot = new Quaternionf()
                            .rotateY((float) Math.toRadians(t * 12.0F))
                            .rotateX((float) Math.toRadians(t * 5.0F));

                    ((DisplayAccessor) alkaid.sphereDisplay).weapons$setTransformation(new Transformation(
                            new Vector3f(0.0F, 0.0F, 0.0F),
                            rot,
                            new Vector3f(scale, scale, scale),
                            null
                    ));
                }

                // Hàng vạn hạt tinh tú từ bán kính 25m bay cuồn cuộn tụ vào tâm quả cầu
                for (int p = 0; p < 14; p++) {
                    double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
                    double phi = RANDOM.nextDouble() * Math.PI;
                    double dist = 6.0D + RANDOM.nextDouble() * 20.0D;

                    double sx = spherePos.x + dist * Math.sin(phi) * Math.cos(theta);
                    double sy = spherePos.y + dist * Math.cos(phi);
                    double sz = spherePos.z + dist * Math.sin(phi) * Math.sin(theta);

                    double vx = (spherePos.x - sx) * 0.14D;
                    double vy = (spherePos.y - sy) * 0.14D;
                    double vz = (spherePos.z - sz) * 0.14D;

                    level.sendParticles(STARLIGHT_WHITE_DUST, sx, sy, sz, 1, vx, vy, vz, 0.15D);
                    if (p % 3 == 0) {
                        level.sendParticles(CYAN_COSMIC_DUST, sx, sy, sz, 1, vx, vy, vz, 0.1D);
                    }
                    if (p % 4 == 0) {
                        level.sendParticles(GOLDEN_FLARE_DUST, sx, sy, sz, 1, vx, vy, vz, 0.1D);
                    }
                }

                // Mốc âm thanh & thông báo tiến trình
                if (t == 40) {
                    level.playSound(null, spherePos.x, spherePos.y, spherePos.z,
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 4.0F, 0.7F);
                } else if (t == 80) {
                    level.playSound(null, spherePos.x, spherePos.y, spherePos.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 4.0F, 0.8F);
                    if (caster.connection != null) {
                        caster.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e§lĐộ Nén Thấp Phân Rã... (4s)")));
                    }
                } else if (t == 120) {
                    level.playSound(null, spherePos.x, spherePos.y, spherePos.z,
                            SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.5F, 1.2F);
                    level.playSound(null, spherePos.x, spherePos.y, spherePos.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 1.5F);
                    if (caster.connection != null) {
                        caster.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c§lQuả Cầu Tinh Tú Đã Đạt Cực Hạn! (6s)")));
                    }
                } else if (t == 150) {
                    level.playSound(null, spherePos.x, spherePos.y, spherePos.z,
                            SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 4.0F, 0.9F);
                    level.sendParticles(ParticleTypes.FLASH, spherePos.x, spherePos.y, spherePos.z, 3, 1.0D, 1.0D, 1.0D, 0);
                }

                // Chuyển sang giai đoạn bắn khi chạm mốc 160 ticks (8 giây)
                if (t >= alkaid.chargeTicks) {
                    alkaid.isFiring = true;
                    alkaid.launchDir = caster.getLookAngle().normalize();
                    alkaid.projectilePos = caster.getEyePosition().add(alkaid.launchDir.scale(3.0D));

                    // Tạo thêm phễu xoáy không gian bọc quanh quả cầu khi bắn
                    Display.ItemDisplay vortex = EntityType.ITEM_DISPLAY.create(level);
                    if (vortex != null) {
                        vortex.moveTo(alkaid.projectilePos.x, alkaid.projectilePos.y, alkaid.projectilePos.z, 0.0F, 0.0F);
                        ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) vortex;
                        DisplayAccessor dispAcc = (DisplayAccessor) vortex;

                        itemAcc.weapons$setItemStack(new ItemStack(ModItems.ALKAID_VORTEX.get()));
                        itemAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
                        dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
                        vortex.setGlowingTag(true);
                        dispAcc.weapons$setGlowColorOverride(0x80D8FF);
                        dispAcc.weapons$setViewRange(6.0F);

                        dispAcc.weapons$setTransformation(new Transformation(
                                new Vector3f(0.0F, 0.0F, 0.0F),
                                new Quaternionf(),
                                new Vector3f(14.0F, 14.0F, 14.0F),
                                null
                        ));

                        level.addFreshEntity(vortex);
                        alkaid.vortexDisplay = vortex;
                    }

                    // Title & âm thanh khai hỏa chấn động
                    if (caster.connection != null) {
                        caster.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§l✦ A L K A I D ✦")));
                        caster.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c§lĐại Khai Hỏa Diệt Thế")));
                    }

                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F, 0.5F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 0.6F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 4.0F, 0.5F);

                    caster.displayClientMessage(
                            Component.literal("§f§l[ALKAID] §cPhóng Quả Cầu Tinh Tú Xoắn Ốc! Khoan thủng mọi địa hình trên đường đi!"),
                            true
                    );
                }
            }

            // =========================================================================
            // GIAI ĐOẠN 2: BẮN THẲNG XOẮN ỐC & KHOAN RÃNH ĐỊA HÌNH (Ảnh 3 & 4)
            // =========================================================================
            else {
                alkaid.flightTicks++;
                double speed = 3.0D; // Bay 3.0 blocks mỗi tick
                Vec3 prevPos = alkaid.projectilePos;
                alkaid.projectilePos = alkaid.projectilePos.add(alkaid.launchDir.scale(speed));
                Vec3 currentPos = alkaid.projectilePos;

                // Cập nhật vị trí và xoay tròn của Quả Cầu & Vòng Xoáy Mũi Khoan
                float spinAngle = alkaid.flightTicks * 40.0F; // Xoay cực nhanh theo trục
                Quaternionf projectileRot = new Quaternionf()
                        .rotateY((float) -Math.atan2(alkaid.launchDir.x, alkaid.launchDir.z))
                        .rotateX((float) Math.asin(alkaid.launchDir.y))
                        .rotateZ((float) Math.toRadians(spinAngle));

                if (alkaid.sphereDisplay != null && alkaid.sphereDisplay.isAlive()) {
                    alkaid.sphereDisplay.moveTo(currentPos.x, currentPos.y, currentPos.z, 0.0F, 0.0F);
                    ((DisplayAccessor) alkaid.sphereDisplay).weapons$setTransformation(new Transformation(
                            new Vector3f(0.0F, 0.0F, 0.0F),
                            projectileRot,
                            new Vector3f(13.0F, 13.0F, 13.0F),
                            null
                    ));
                }

                if (alkaid.vortexDisplay != null && alkaid.vortexDisplay.isAlive()) {
                    alkaid.vortexDisplay.moveTo(currentPos.x, currentPos.y, currentPos.z, 0.0F, 0.0F);
                    ((DisplayAccessor) alkaid.vortexDisplay).weapons$setTransformation(new Transformation(
                            new Vector3f(0.0F, 0.0F, 0.0F),
                            projectileRot,
                            new Vector3f(15.0F, 15.0F, 15.0F),
                            null
                    ));
                }

                // Hệ tọa độ vuông góc cho dải hạt xoắn ốc (Corkscrew Helix VFX)
                Vec3 forward = alkaid.launchDir;
                Vec3 upRef = Math.abs(forward.y) > 0.95D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 right = forward.cross(upRef).normalize();
                Vec3 up = right.cross(forward).normalize();

                // Vẽ dải xoắn ốc đôi (Double Helix) bao quanh luồng bắn
                if (level.hasNearbyAlivePlayer(currentPos.x, currentPos.y, currentPos.z, 64.0D)) {
                    for (double step = 0.0D; step <= speed; step += 0.6D) {
                        Vec3 centerPt = prevPos.add(forward.scale(step));
                        double angle1 = (alkaid.flightTicks * 0.8D) + (step * 0.7D);
                        double angle2 = angle1 + Math.PI;

                        double helixR = 5.5D;
                        Vec3 h1 = centerPt.add(right.scale(Math.cos(angle1) * helixR)).add(up.scale(Math.sin(angle1) * helixR));
                        Vec3 h2 = centerPt.add(right.scale(Math.cos(angle2) * helixR)).add(up.scale(Math.sin(angle2) * helixR));

                        level.sendParticles(STARLIGHT_WHITE_DUST, h1.x, h1.y, h1.z, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.END_ROD, h1.x, h1.y, h1.z, 1, 0, 0, 0, 0.02D);
                        level.sendParticles(VOID_PURPLE_DUST, h2.x, h2.y, h2.z, 1, 0, 0, 0, 0);
                        level.sendParticles(CYAN_COSMIC_DUST, h2.x, h2.y, h2.z, 1, 0, 0, 0, 0);

                        // Sóng nén Sonic Boom dọc thân tia
                        if (RANDOM.nextFloat() < 0.25F) {
                            level.sendParticles(ParticleTypes.SONIC_BOOM, centerPt.x, centerPt.y, centerPt.z, 1, 0, 0, 0, 0);
                        }
                    }
                }

                // =====================================================================
                // ĐÀO RÃNH ĐỊA HÌNH HÌNH TRỤ BÁN KÍNH 6 BLOCKS (ĐƯỜNG KÍNH 12 BLOCKS)
                // Phỏng theo panel manga số 4: Khoan thủng núi đồi thành rãnh lòng máng
                // Tối ưu Flag 50 (UPDATE_CLIENTS | SUPPRESS_DROPS | KNOWN_SHAPE): Tránh sập TPS do bão cập nhật vật lý láng giềng
                // =====================================================================
                double carveRadius = 5.8D;
                double carveRadiusSq = carveRadius * carveRadius;
                double perimeterRadiusSq = (carveRadius + 1.2D) * (carveRadius + 1.2D);

                int minX = Mth.floor(currentPos.x - carveRadius);
                int maxX = Mth.floor(currentPos.x + carveRadius);
                int minY = Mth.floor(currentPos.y - carveRadius);
                int maxY = Mth.floor(currentPos.y + carveRadius);
                int minZ = Mth.floor(currentPos.z - carveRadius);
                int maxZ = Mth.floor(currentPos.z + carveRadius);

                BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
                boolean hasNearbyPlayer = level.hasNearbyAlivePlayer(currentPos.x, currentPos.y, currentPos.z, 48.0D);
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int by = minY; by <= maxY; by++) {
                        for (int bz = minZ; bz <= maxZ; bz++) {
                            double distSq = currentPos.distanceToSqr(bx + 0.5D, by + 0.5D, bz + 0.5D);
                            if (distSq <= carveRadiusSq) {
                                mpos.set(bx, by, bz);
                                BlockState bs = level.getBlockState(mpos);
                                if (!bs.isAir() && bs.getDestroySpeed(level, mpos) >= 0.0F) {
                                    level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 50); // Phá hủy sạch không vương drop, không kích hoạt chuỗi physics bão táp
                                }
                            } else if (distSq <= perimeterRadiusSq) {
                                // Vết xước vân xoáy trên thành hào
                                if (hasNearbyPlayer && RANDOM.nextFloat() < 0.08F) {
                                    level.sendParticles(ParticleTypes.FLASH, bx + 0.5D, by + 0.5D, bz + 0.5D, 1, 0, 0, 0, 0);
                                }
                            }
                        }
                    }
                }

                // =====================================================================
                // TẤT SÁT MỌI SINH VẬT TRÊN ĐƯỜNG ĐẠN (Bán kính 7.5m)
                // =====================================================================
                AABB hitBox = new AABB(
                        currentPos.x - 7.5D, currentPos.y - 7.5D, currentPos.z - 7.5D,
                        currentPos.x + 7.5D, currentPos.y + 7.5D, currentPos.z + 7.5D
                );
                List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                        e -> e != caster && e.isAlive());

                for (LivingEntity victim : victims) {
                    boolean isBoss = victim.getMaxHealth() >= 100.0F
                            || victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                            || victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                            || victim instanceof net.minecraft.world.entity.monster.warden.Warden;

                    if (isBoss) {
                        float bossDmg = Math.max(800.0F, victim.getHealth() * 0.90F);
                        victim.hurt(level.damageSources().playerAttack(caster), bossDmg);
                        victim.setDeltaMovement(alkaid.launchDir.scale(2.5D).add(0, 0.8D, 0));
                    } else {
                        TensuraEvents.handleMobDeathDrop(caster, victim);
                        victim.hurt(level.damageSources().playerAttack(caster), 99999.0F);
                        if (victim.isAlive()) {
                            victim.discard();
                        }
                    }

                    level.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0, 0, 0, 0);
                }

                // Âm thanh khoan xé không gian liên tục
                if (alkaid.flightTicks % 4 == 0) {
                    level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.5F, 1.6F);
                    level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5F, 1.8F);
                }

                // Điều kiện kết thúc: Bay hết tầm 150 blocks (50 ticks) hoặc rơi ra ngoài thế giới
                if (alkaid.flightTicks >= alkaid.maxFlightTicks || currentPos.y < level.getMinBuildHeight() || currentPos.y > level.getMaxBuildHeight()) {
                    // Vụ nổ Siêu Tân Tinh khép màn
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, currentPos.x, currentPos.y, currentPos.z, 5, 2.0D, 2.0D, 2.0D, 0);
                    level.sendParticles(ParticleTypes.FLASH, currentPos.x, currentPos.y, currentPos.z, 6, 3.0D, 3.0D, 3.0D, 0);

                    level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F, 0.6F);
                    level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 1.2F);

                    caster.displayClientMessage(
                            Component.literal("§e§l✦ Diệt Thế Tà Tinh: Alkaid đã hoàn tất đường bắn hủy diệt!"),
                            true
                    );

                    alkaid.cleanup();
                    it.remove();
                }
            }
        }
    }
}
