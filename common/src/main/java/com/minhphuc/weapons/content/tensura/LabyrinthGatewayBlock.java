package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.world.LabyrinthWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Khối Cổng Mê Cung 100 Tầng Ramiris:
 * - Nhấp Chuột Phải trên mặt đất -> Tiến vào Tầng 1.
 * - Trong Mê Cung: nếu bên dưới là Emerald Block (cổng thoát), hoặc đang Ngồi (Shift) -> Thoát về Mặt Đất.
 * - Ngược lại -> Phải hạ gục hộ vệ tầng hiện tại để mở khóa lên Tầng kế tiếp (tối đa Tầng 100).
 */
public class LabyrinthGatewayBlock extends Block {

    public LabyrinthGatewayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        LabyrinthWorldData data = LabyrinthWorldData.get(sl);
        int currentFloor = data.detectFloorAt(sl, pos);

        if (currentFloor <= 0) {
            data.enterFloor(sp, 1);
            return InteractionResult.CONSUME;
        }

        if (sp.isShiftKeyDown() || sl.getBlockState(pos.below()).is(Blocks.EMERALD_BLOCK)) {
            data.enterFloor(sp, 0);
            return InteractionResult.CONSUME;
        }

        BlockPos center = data.getFloorCenter(sl, currentFloor);
        List<Mob> remaining = sl.getEntitiesOfClass(Mob.class, new AABB(center).inflate(14.0D),
                m -> m.isAlive() && m.getTags().contains("LabyrinthFloorMob"));
        if (!remaining.isEmpty()) {
            sp.displayClientMessage(Component.literal("§c[Mê Cung Ramiris] §eHãy tiêu diệt " + remaining.size() + " Hộ Vệ Tầng " + currentFloor + " trước khi lên tầng tiếp theo! (Giữ Shift + Click để thoát ra ngoài)"), true);
            return InteractionResult.CONSUME;
        }

        LabyrinthWorldData.rewardFloorClearIfBoss(sl, currentFloor, center);
        if (currentFloor >= LabyrinthWorldData.MAX_FLOORS) {
            VoiceOfTheWorld.announce(sp, "§6§l[CHINH PHỤC TẦNG 100] §e" + sp.getName().getString() + " §fđã phá đảo toàn bộ §dMê Cung 100 Tầng Của Ramiris§f!");
            data.enterFloor(sp, 0);
        } else {
            data.enterFloor(sp, currentFloor + 1);
        }
        return InteractionResult.CONSUME;
    }
}
