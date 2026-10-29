package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TỐI ƯU HÓA HIỆU NĂNG 100% (ZERO LAG):
 * Quản lý Linh Hồn Samurai & Tàn Ảnh Chém Nghịch Thời Gian.
 * 
 * Các cải tiến triệt tiêu giật lag:
 * 1. KHÔNG CÓ entity bám đuôi người chơi lúc idle.
 * 2. Linh hồn Samurai chỉ xuất hiện chớp nhoáng 10 ticks (0.5s) khi tung chiêu rồi biến mất.
 * 3. Tàn ảnh sử dụng chùm hạt Sweep Attack + Electric Spark siêu nhẹ thay vì spawn nhiều ItemDisplay.
 * 4. Mật độ hạt nứt khối số hóa giảm 85% nhưng vẫn giữ độ tương phản đen-trắng cực mạnh.
 */
public class SamuraiSpiritDisplayHelper {

    public static final DustParticleOptions PURE_WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.2F);
    public static final DustParticleOptions PITCH_BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 2.0F);

    public static class TemporarySpirit {
        public final ServerLevel level;
        public final Display.ItemDisplay display;
        public int ticksRemaining;

        public TemporarySpirit(ServerLevel level, Display.ItemDisplay display, int ticksRemaining) {
            this.level = level;
            this.display = display;
            this.ticksRemaining = ticksRemaining;
        }

        public void discard() {
            if (display != null && display.isAlive()) {
                display.discard();
            }
        }
    }

    private static final List<TemporarySpirit> ACTIVE_SPIRITS = new ArrayList<>();

    /**
     * Triệu hồi Linh Hồn Samurai hào quang trắng xuất hiện chớp nhoáng 10 ticks (0.5s) khi tung chiêu
     */
    public static void spawnInstantSamuraiSpirit(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 behindPos = player.position()
                .subtract(look.x * 0.7D, 0, look.z * 0.7D)
                .add(0, 0.35D, 0);

        Display.ItemDisplay spirit = EntityType.ITEM_DISPLAY.create(level);
        if (spirit != null) {
            spirit.moveTo(behindPos.x, behindPos.y, behindPos.z, player.getYRot(), 0.0F);
            DisplayAccessor dispAcc = (DisplayAccessor) spirit;
            dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.HORIZONTAL);
            dispAcc.weapons$setViewRange(48.0F);
            dispAcc.weapons$setGlowColorOverride(0xFFFFFF); // Hào quang trắng
            dispAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0, 0.35f, 0),
                    new Quaternionf(),
                    new Vector3f(1.4f, 1.4f, 1.4f),
                    new Quaternionf()
            ));

            ItemDisplayAccessor itemAcc = (ItemDisplayAccessor) spirit;
            itemAcc.weapons$setItemStack(new ItemStack(ModItems.DIVINE_HELMET.get()));
            itemAcc.weapons$setItemTransform(ItemDisplayContext.HEAD);

            level.addFreshEntity(spirit);
            ACTIVE_SPIRITS.add(new TemporarySpirit(level, spirit, 10)); // Tồn tại 10 ticks (0.5s) rồi tự dọn
        }

        // Bờm tóc trắng rực sáng chớp nhoáng
        for (int i = 0; i < 3; i++) {
            level.sendParticles(ParticleTypes.END_ROD, behindPos.x, behindPos.y + 1.2D, behindPos.z,
                    2, 0.15D, 0.2D, 0.15D, 0.02D);
            level.sendParticles(PURE_WHITE_DUST, behindPos.x, behindPos.y + 1.0D, behindPos.z,
                    3, 0.2D, 0.2D, 0.2D, 0.02D);
        }
    }

    /**
     * Tàn ảnh đường chém siêu nhẹ (Particle-based Afterimages - Không spawn entity để chống lag)
     */
    public static void spawnAfterimagesAlongTrajectory(ServerLevel level, Vec3 startPos, Vec3 endPos, float yRot) {
        int steps = 5;
        Vec3 delta = endPos.subtract(startPos);

        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / (steps + 1);
            Vec3 pos = startPos.add(delta.scale(fraction));

            // Vệt chém Sweep Attack trắng
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.8D, pos.z, 1, 0, 0, 0, 0.0D);
            level.sendParticles(PURE_WHITE_DUST, pos.x, pos.y + 0.8D, pos.z, 3, 0.2D, 0.3D, 0.2D, 0.02D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 2, 0.15D, 0.15D, 0.15D, 0.04D);
        }
    }

    /**
     * Hiệu ứng rạn nứt không gian số hóa mô phỏng nứt khối (Tối ưu: bước nhảy 2.5m, ít hạt, không lag)
     * TUYỆT ĐỐI KHÔNG PHÁ VỠ BLOCK THẬT!
     */
    public static void spawnDigitalBlockCracks(ServerLevel level, Vec3 startPos, Vec3 endPos) {
        Vec3 dir = endPos.subtract(startPos);
        double dist = dir.length();
        if (dist <= 0.01D) return;

        Vec3 step = dir.normalize().scale(2.5D); // Bước nhảy lớn 2.5 blocks để tiết kiệm tài nguyên
        int count = Math.min((int) Math.ceil(dist / 2.5D), 8); // Tối đa 8 điểm nhấn chấn động

        Vec3 current = startPos;
        for (int i = 0; i < count; i++) {
            BlockPos groundPos = BlockPos.containing(current.x, current.y - 0.5D, current.z);
            BlockState state = level.getBlockState(groundPos);
            if (state.isAir()) {
                groundPos = groundPos.below();
                state = level.getBlockState(groundPos);
            }

            if (!state.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        current.x, current.y + 0.1D, current.z, 2, 0.15D, 0.05D, 0.15D, 0.05D);
            }
            level.sendParticles(ParticleTypes.WAX_OFF, current.x, current.y + 0.15D, current.z, 1, 0.1D, 0.05D, 0.1D, 0.02D);

            current = current.add(step);
        }
    }

    /**
     * Dọn dẹp linh hồn tạm thời khi hết 10 ticks
     */
    public static void tickSpirits(ServerLevel level) {
        Iterator<TemporarySpirit> it = ACTIVE_SPIRITS.iterator();
        while (it.hasNext()) {
            TemporarySpirit s = it.next();
            if (s.level == level) {
                s.ticksRemaining--;
                if (s.ticksRemaining <= 0 || !s.display.isAlive()) {
                    s.discard();
                    it.remove();
                }
            }
        }
    }

    public static void clearAll() {
        for (TemporarySpirit s : ACTIVE_SPIRITS) {
            s.discard();
        }
        ACTIVE_SPIRITS.clear();
    }
}
