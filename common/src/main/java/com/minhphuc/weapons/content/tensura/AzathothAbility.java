package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.minhphuc.weapons.network.ClientboundAzathothPacket;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TUYỆT KỸ TỐI THƯỢNG: THẦN HƯ KHÔNG AZATHOTH (VOID GOD AZATHOTH)
 * - Khởi phát CỰ LONG PLASMA HƯ KHÔNG (Pure Plasma Energy Dragon) dài 50m:
 *   + Long Thân: 4 phân đoạn plasma chuyển động uốn lượn hình sin trong không gian 3D.
 *   + Long Thủ: Hàm trên và hàm dưới bằng các chùm plasma góc nhọn tạo thành miệng rồng há ngoác gầm thét,
 *     hai sừng rồng plasma vểnh ngược ra sau và hai mắt rồng phát quang cực quang.
 *   + Long Vây & Móng Vuốt: Các đĩa sóng xung kích plasma nghiêng dọc theo sống lưng rồng.
 * - Giai đoạn 1 (0 -> 15 ticks): Rồng Plasma phóng xé toạc không gian 50 block, nuốt chửng vạn vật.
 * - Giai đoạn 2 (16 -> 35 ticks): Miệng rồng ngậm lại, toàn bộ thân rồng plasma cuộn xoắn trôn ốc ngược lại
 *   và chui thẳng vào cơ thể người chơi (hấp thụ toàn bộ vào dạ dày hư không).
 * - Phân rã và xóa sổ vạn vật: Xóa sạch block, xóa sạch dung nham (Lava).
 * - Xóa sổ toàn bộ sinh vật (Milim, Long chủng, Ác ma thủy tổ, v.v.): biến mất vĩnh viễn, gắn cờ AzathothErased và discard()
 *   để TUYỆT ĐỐI KHÔNG KÍCH HOẠT nghi thức triệu hồi hay rơi đồ.
 * - TỐI ƯU ZERO-LAG: Quét khối không sinh rác item, cập nhật khối trực tiếp không gây cascade block update, duy trì 20 TPS.
 */
public class AzathothAbility {

    public static class ActiveAzathoth {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 startPos;
        public final Vec3 dir;
        public final Vec3 right;
        public final Vec3 up;
        public final double maxRange = 50.0D;
        public final double radius = 8.0D;
        public int currentTick = 0;
        public final int maxTicks = 35; // ~1.75 giây toàn bộ quá trình phóng và thu hồi

        public ActiveAzathoth(ServerLevel level, ServerPlayer caster, Vec3 startPos, Vec3 dir) {
            this.level = level;
            this.caster = caster;
            this.startPos = startPos;
            this.dir = dir.normalize();

            Vec3 r = this.dir.cross(new Vec3(0, 1, 0)).normalize();
            if (r.lengthSqr() < 0.01) {
                r = new Vec3(1, 0, 0);
            }
            this.right = r;
            this.up = this.right.cross(this.dir).normalize();
        }

        public void cleanup() {
            // Hoàn toàn render trực tiếp bằng GPU ClientAzathothDragonRenderer, 0 ItemDisplay
        }
    }

    public static final List<ActiveAzathoth> ACTIVE_BEAMS = Collections.synchronizedList(new ArrayList<>());

    public static void cast(ServerLevel level, ServerPlayer caster) {
        if (caster == null || level == null) return;

        Vec3 startPos = caster.getEyePosition();
        Vec3 dir = caster.getLookAngle();

        ActiveAzathoth beam = new ActiveAzathoth(level, caster, startPos, dir);
        ACTIVE_BEAMS.add(beam);

        // 1. Gửi Packet kích hoạt Hắc Long Plasma GPU & Cinematic Camera Lệch Vai
        ClientboundAzathothPacket packet = new ClientboundAzathothPacket(caster.getUUID(), startPos, dir, 35);
        ModMessages.sendToPlayer(packet, caster);
        for (ServerPlayer nearbyPlayer : level.players()) {
            if (nearbyPlayer != caster && nearbyPlayer.position().distanceToSqr(startPos) <= 128 * 128) {
                ModMessages.sendToPlayer(packet, nearbyPlayer);
            }
        }

        // 5. Âm thanh bộc phát Cự Long gầm thét xé toạc không gian
        level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 4.0F, 0.6F);
        level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 3.5F, 0.7F);
        level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 8.0F, 0.5F);
        level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 5.0F, 1.2F);

        caster.displayClientMessage(
                Component.literal("§5§l[THẦN HƯ KHÔNG] §dKhai mở Tuyệt Kỹ: Azathoth - Hắc Long Phệ Diệt!"),
                true
        );

        // Tiến hành xóa sổ vạn vật dọc theo đường bắn ngay tick đầu tiên (đường kính quét 16 block)
        executeVoidObliteration(level, caster, startPos, dir, 50.0D, 8.0D);
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE_BEAMS.isEmpty()) return;

        synchronized (ACTIVE_BEAMS) {
            Iterator<ActiveAzathoth> it = ACTIVE_BEAMS.iterator();
            while (it.hasNext()) {
                ActiveAzathoth b = it.next();
                if (b.level != level) continue;

                if (b.caster == null || !b.caster.isAlive() || b.caster.hasDisconnected()) {
                    b.cleanup();
                    it.remove();
                    continue;
                }

                b.currentTick++;

                // ==============================================================
                // GIAI ĐOẠN 1 (0 -> 15 ticks): HẮC LONG PHÓNG XÉ TOẠC KHÔNG GIAN
                // ==============================================================
                if (b.currentTick < 16) {
                    // Hạt hư vô và hơi thở rồng xoáy quanh thân rồng
                    if (b.level.hasNearbyAlivePlayer(b.startPos.x, b.startPos.y, b.startPos.z, 64.0)) {
                        for (int k = 0; k < 3; k++) {
                            double pDist = (b.level.random.nextDouble() * 45.0D) + 3.0D;
                            double pAngle = Math.toRadians(b.currentTick * 25.0 + k * 120.0);
                            Vec3 pPos = b.startPos.add(b.dir.scale(pDist))
                                    .add(b.right.scale(Math.cos(pAngle) * 2.2D))
                                    .add(b.up.scale(Math.sin(pAngle) * 2.2D));
                            b.level.sendParticles(ParticleTypes.REVERSE_PORTAL, pPos.x, pPos.y, pPos.z, 1, 0.05, 0.05, 0.05, 0.02);
                            b.level.sendParticles(ParticleTypes.DRAGON_BREATH, pPos.x, pPos.y, pPos.z, 1, 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }
                // ==============================================================
                // GIAI ĐOẠN 2 (16 -> 35 ticks): HẮC LONG NGẬM MIỆNG & CUỘN XOẮN THU HỒI
                // ==============================================================
                else {
                    float retractProgress = (b.currentTick - 16) / 19.0F; // 0.0 -> 1.0

                    // Âm thanh thu hồi vào người chơi
                    if (b.currentTick % 4 == 0) {
                        b.level.playSound(null, b.caster.getX(), b.caster.getY(), b.caster.getZ(),
                                SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.4F, 1.6F + (retractProgress * 0.5F));
                    }

                    // Hạt xoáy hút ngược vào người chơi
                    for (int i = 0; i < 4; i++) {
                        double dist = 1.5 + (b.level.random.nextDouble() * 5.0);
                        Vec3 particlePos = b.startPos.add(b.dir.scale(dist));
                        b.level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                                particlePos.x, particlePos.y, particlePos.z,
                                2, 0.2, 0.2, 0.2, 0.05);
                        b.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }

                if (b.currentTick >= b.maxTicks) {
                    b.cleanup();
                    b.level.playSound(null, b.caster.getX(), b.caster.getY(), b.caster.getZ(),
                            SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 3.0F, 0.8F);
                    it.remove();
                }
            }
        }
    }

    /**
     * XÓA SỔ HƯ VÔ TỐI THƯỢNG (Zero-Lag Block & Mob Disintegration)
     */
    private static void executeVoidObliteration(ServerLevel level, ServerPlayer caster, Vec3 startPos, Vec3 dir, double range, double radius) {
        dir = dir.normalize();

        // 1. Xóa sổ mọi sinh vật và thực thể (Long chủng, Milim, Ác ma, quái vật, vật phẩm, v.v.)
        // Tìm toàn bộ Entity trong hộp bao quanh tia bắn 50m
        Vec3 endPos = startPos.add(dir.scale(range));
        AABB sweepBox = new AABB(startPos, endPos).inflate(radius + 2.0D);
        List<net.minecraft.world.entity.Entity> entities = level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, sweepBox,
                e -> e != caster && !(e instanceof net.minecraft.world.entity.player.Player) && !isAllied(caster, e));

        for (net.minecraft.world.entity.Entity entity : entities) {
            Vec3 entityPos = entity.getEyePosition();
            Vec3 toEntity = entityPos.subtract(startPos);
            double projection = toEntity.dot(dir);
            if (projection >= 0 && projection <= range) {
                Vec3 closestPoint = startPos.add(dir.scale(projection));
                double distSq = closestPoint.distanceToSqr(entityPos);
                if (distSq <= radius * radius) {
                    // Gắn cờ AzathothErased để không bao giờ kích hoạt triệu hồi hay rơi đồ
                    entity.addTag("AzathothErased");
                    if (entity instanceof LivingEntity le) {
                        le.removeAllEffects();
                    }
                    level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + 0.5, entity.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

                    // Xóa thẳng khỏi thế giới (Discard)
                    entity.discard();
                }
            }
        }

        // 2. Xóa sổ khối và dung nham (Lava & Blocks)
        // Duyệt theo hình trụ dọc trục tia bắn để tối ưu hiệu năng
        int rInt = (int) Math.ceil(radius);
        int steps = (int) range;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int s = 0; s <= steps; s += 2) {
            Vec3 centerSlice = startPos.add(dir.scale(s));
            int cx = (int) Math.floor(centerSlice.x);
            int cy = (int) Math.floor(centerSlice.y);
            int cz = (int) Math.floor(centerSlice.z);

            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dy = -rInt; dy <= rInt; dy++) {
                    for (int dz = -rInt; dz <= rInt; dz++) {
                        if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                        mpos.set(cx + dx, cy + dy, cz + dz);

                        BlockState st = level.getBlockState(mpos);
                        if (st.isAir() || st.is(Blocks.BEDROCK)) continue;

                        // Xóa sạch dung nham và các khối thông thường không rơi vật phẩm (Zero Entity Items)
                        level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 50);
                    }
                }
            }
        }
    }



    public static void cancelAll(ServerLevel level) {
        synchronized (ACTIVE_BEAMS) {
            Iterator<ActiveAzathoth> it = ACTIVE_BEAMS.iterator();
            while (it.hasNext()) {
                ActiveAzathoth b = it.next();
                if (b.level == level) {
                    b.cleanup();
                    it.remove();
                }
            }
        }
    }

    private static boolean isAllied(ServerPlayer caster, net.minecraft.world.entity.Entity entity) {
        return entity.isAlliedTo(caster) || entity.getUUID().equals(caster.getUUID());
    }
}
