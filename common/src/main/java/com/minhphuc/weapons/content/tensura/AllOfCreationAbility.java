package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.client.gui.ClientPhoneOpener;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trí Huệ Chi Vương (Ciel / Raphael) - Thẩm Định Vạn Vật (All of Creation Appraisal):
 * - Thẩm định toàn năng từ VẬT VÔ TRI (Khối Block, Quặng, Rương Chứa Đồ, Item rơi)
 *   cho tới CÁC SINH VẬT (Quái vật, Boss, Người chơi, Ác ma, Long Chủng).
 * - Hiển thị bản đồ phân tích holographic của Giọng Nói Thế Giới.
 */
public class AllOfCreationAbility {

    public static final String NBT_HUD_TICKS = "TensuraAllOfCreationHud";
    private static final DecimalFormat NUM_FORMAT = new DecimalFormat("#,###");

    public static boolean cast(ServerLevel level, ServerPlayer player) {
        boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
        if (!isTrueDemonLord) {
            player.displayClientMessage(
                    Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI] §cBáo cáo. Yêu cầu thức tỉnh Chân Ma Vương để sử dụng Thẩm Định Vạn Vật!"),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }

        // Bật chế độ HUD quét thụ động trong 60 giây (1200 ticks)
        EntityDataHelper.getCustomData(player).putInt(NBT_HUD_TICKS, 1200);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 2.0F, 1.8F);

        boolean hit = appraiseTarget(level, player);
        if (!hit) {
            // Thông báo đã kích hoạt HUD Thụ Động
            player.displayClientMessage(
                    Component.literal("§e§l[GIỌNG NÓI THẾ GIỚI - CIEL] §aBáo cáo. [Thẩm Định Vạn Vật] đã kích hoạt! Hãy nhìn vào bất kỳ thực thể hoặc khối block nào để phân tích tự động (60s)!"),
                    false
            );
        }
        return true;
    }

    public static boolean appraiseTarget(ServerLevel level, ServerPlayer player) {
        // Raycast quét mục tiêu theo hướng nhìn 32 blocks
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 32.0D;
        Vec3 reach = eyePos.add(look.scale(maxDist));

        // 1. Quét thực thể (LivingEntity hoặc ItemEntity)
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(2.0D);
        List<Entity> entities = level.getEntities(player, searchBox, e -> e.isAlive() || e instanceof ItemEntity);
        Entity targetEntity = null;
        double closestDist = maxDist;

        for (Entity e : entities) {
            AABB bb = e.getBoundingBox().inflate(0.3D);
            var opt = bb.clip(eyePos, reach);
            if (opt.isPresent()) {
                double d = eyePos.distanceTo(opt.get());
                if (d < closestDist) {
                    closestDist = d;
                    targetEntity = e;
                }
            }
        }

        if (targetEntity != null) {
            // Hiệu ứng tia quét xanh ma đạo tới mục tiêu
            spawnScanBeam(level, eyePos, targetEntity.position().add(0, targetEntity.getBbHeight() * 0.5D, 0));

            if (targetEntity instanceof LivingEntity living) {
                appraiseLivingEntity(player, living);
                return true;
            } else if (targetEntity instanceof ItemEntity itemEntity) {
                appraiseItemEntity(player, itemEntity);
                return true;
            }
        }

        // 2. Nếu không trúng thực thể -> Quét Khối Block (Vật Vô Tri)
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = blockHit.getBlockPos();
            spawnScanBeam(level, eyePos, Vec3.atCenterOf(bp));
            appraiseBlock(level, player, bp);
            return true;
        }

        return false;
    }

    private static void spawnScanBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        for (double d = 0.5D; d <= dist; d += 0.8D) {
            Vec3 pt = start.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.GLOW, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Thẩm định SINH VẬT (Living Entity)
     */
    public static void appraiseLivingEntity(ServerPlayer player, LivingEntity target) {
        String name = target.getDisplayName().getString();
        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        double hpPct = (hp / maxHp) * 100.0D;

        long ep = estimateEP(target);
        String species = determineSpecies(target);
        String threatLevel = (target instanceof VelgryndEntity || target.getMaxHealth() >= 1000.0F)
                ? "§4§l[CẤM KỴ - THIÊN TAI ĐỈNH CAO]"
                : (target instanceof Enemy ? "§c§l[HIỂM HỌA CHIẾN ĐẤU]" : "§a§l[AN TOÀN / THÂN THIỆN]");

        player.displayClientMessage(Component.literal("§b§l╔════════════════════════════════════════════════╗"), false);
        player.displayClientMessage(Component.literal("§b§l║   §e§l✦ THẨM ĐỊNH VẠN VẬT: SINH MỆNH THỰC THỂ ✦   §b§l║"), false);
        player.displayClientMessage(Component.literal("§b§l╠════════════════════════════════════════════════╣"), false);
        player.displayClientMessage(Component.literal("§f  Danh Tính: §e" + name), false);
        player.displayClientMessage(Component.literal("§7  Chủng Tộc: §f" + species), false);
        player.displayClientMessage(Component.literal("§c  Sinh Lực (HP): §a" + String.format("%.1f / %.1f (%.0f%%)", hp, maxHp, hpPct)), false);
        player.displayClientMessage(Component.literal("§d  Ma Tố Lượng (EP): §b" + NUM_FORMAT.format(ep) + " EP"), false);
        player.displayClientMessage(Component.literal("§6  Mức Độ Đe Dọa: " + threatLevel), false);

        // Phân tích trạng thái / hiệu ứng
        if (!target.getActiveEffects().isEmpty()) {
            StringBuilder effects = new StringBuilder("§e  Hiệu Ứng: §7");
            for (MobEffectInstance eff : target.getActiveEffects()) {
                effects.append(Component.translatable(eff.getDescriptionId()).getString()).append(", ");
            }
            player.displayClientMessage(Component.literal(effects.substring(0, effects.length() - 2)), false);
        }

        // Nhược điểm / Khắc chế
        String weakness = target.fireImmune() ? "§cKháng Lửa (Nên dùng Sét hoặc Ma Pháp Thần Thánh)" : "§aYếu Lửa & Ma Pháp Bạo Thực";
        player.displayClientMessage(Component.literal("§a  Khắc Chế Chiến Thuật: §f" + weakness), false);

        // PHÂN TÍCH ĐẶC BIỆT DÀNH CHO KHÔNG VONG (KŪBŌ)
        if (target instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity kubo) {
            player.displayClientMessage(Component.literal("§0§l╠════════════════════════════════════════════════╣"), false);
            player.displayClientMessage(Component.literal("§4§l║   ✦ CHI TIẾT KỸ NĂNG & THUỘC TÍNH KHÔNG VONG ✦   §4§l║"), false);
            player.displayClientMessage(Component.literal("§0§l╠════════════════════════════════════════════════╣"), false);
            
            // 1. Thể trạng hiện tại
            if (kubo.isComplete()) {
                player.displayClientMessage(Component.literal("§f  Thể Trạng: §e§lKHÔNG VONG HOÀN CHỈNH (Bạch Nhật Tà Thần)"), false);
                player.displayClientMessage(Component.literal("§e  Nguồn Gốc: §fĐã hấp thụ Hạt Giống Ma Vương, đạt tới đỉnh cao thần thánh"), false);
            } else if (kubo.isUltimate()) {
                player.displayClientMessage(Component.literal("§4  Thể Trạng: §c§lKHÔNG VONG TỐI THƯỢNG (Vực Thẳm Tiến Hóa)"), false);
                player.displayClientMessage(Component.literal("§e  Nguồn Gốc: §fĐã nuốt chửng linh hồn Milim Nava hoặc Long Chủng Velgrynd"), false);
            } else {
                player.displayClientMessage(Component.literal("§7  Thể Trạng: §8Phôi Thai Hắc Nhật (Dạng Thường)"), false);
            }

            // 2. Thống kê linh hồn đã nuốt (Không hiện trên tên, chỉ hiện qua Thẩm Định Vạn Vật)
            int mobSouls = kubo.getMobSoulsCount();
            int demonSouls = kubo.getDemonSoulsCount();
            player.displayClientMessage(Component.literal("§d  Linh Hồn Thường Đã Nuốt: §a" + mobSouls + " §7(+ " + (mobSouls * 30) + "% HP & DMG)"), false);
            player.displayClientMessage(Component.literal("§5  Linh Hồn Ác Ma Đã Nuốt: §e" + demonSouls + " §7(+ " + (demonSouls * 60) + "% HP & DMG)"), false);

            // 3. Kỹ năng Thủy Tổ Ác Ma mở khóa
            int demonId = kubo.getSpecificDemon();
            if (demonId >= 0) {
                String demonSkill = switch (demonId) {
                    case 0 -> "Bleu (Rain) - Băng Cực Tuyệt Đối (Đóng băng & Làm chậm)";
                    case 1 -> "Rouge (Guy Crimson) - Hỏa Ngục Hồng Liên (Cột lửa hư vô)";
                    case 2 -> "Jaune (Carrera) - Súng Ma Đạn Hạt Nhân";
                    case 3 -> "Noir (Diablo) - Trảm Kích Hư Không Đoạt Mệnh";
                    case 4 -> "Blanc (Testarossa) - Bạch Viêm Thần Thánh";
                    case 5 -> "Violet (Ultima) - Hư Vô Tử Độc Ăn Mòn";
                    case 6 -> "Vert (Misery) - Phong Bạo Cuồng Phong";
                    default -> "Kỹ Năng Ác Ma Bí Ẩn";
                };
                player.displayClientMessage(Component.literal("§6  Tuyệt Kỹ Thủy Tổ Mở Khóa: §b" + demonSkill), false);
            }

            // 4. Kỹ năng cơ bản & Linh hồn dung hợp khác
            player.displayClientMessage(Component.literal("§e  Kỹ Năng Bản Thân: §fHắc Nhật Quang Trụ (Cột sáng hư vô 60m)"), false);
            if (kubo.hasAbsorbedSoul()) {
                player.displayClientMessage(Component.literal("§d  Linh Hồn Ký Sinh Bổ Trợ: §e" + kubo.getAbsorbedEntityName()), false);
                player.displayClientMessage(Component.literal("§6  Kỹ Năng Bổ Trợ (70%): §b" + kubo.getAbsorbedSkillName()), false);
                player.displayClientMessage(Component.literal("§a  Chỉ Số Cộng Thêm: §f+" + String.format("%.0f", kubo.getBonusHp()) + " HP §7| §f+" + String.format("%.1f", kubo.getBonusArmor()) + " Giáp"), false);
            }

            // 5. Kháng tính phòng ngự & Khắc chế chiến thuật
            if (kubo.isComplete()) {
                player.displayClientMessage(Component.literal("§c  Kháng Tính: §f§lKHÁNG 100% MỌI ĐÒN TẤN CÔNG §4(BẤT TỬ TUYỆT ĐỐI)"), false);
                player.displayClientMessage(Component.literal("§a  Khắc Chế Duy Nhất: §b§lLinh Tử Băng Hoại (Disintegration)"), false);
                player.displayClientMessage(Component.literal("§e  Cảnh Báo AI: §cThực thể có trí tuệ cao, sẽ tự giác tháo chạy né tránh khi pháp trận kích hoạt!"), false);
            } else if (kubo.isUltimate()) {
                player.displayClientMessage(Component.literal("§c  Kháng Tính: §4Miễn nhiễm sát thương Milim, Long Chủng, Ác Ma, Boss"), false);
                player.displayClientMessage(Component.literal("§c  Phòng Ngự: §7Kháng 95% sát thương người chơi (5% tỷ lệ trúng điểm yếu)"), false);
                player.displayClientMessage(Component.literal("§c  Đặc Tính Đòn Đánh: §4Xé rách Trận Đồ Cưỡng Chế & Đánh Xuyên Giáp Thần Thoại"), false);
                player.displayClientMessage(Component.literal("§a  Khắc Chế Chiến Thuật: §fĐại Thánh Tẩy§7 (1 hit), §dDragon Nova§7 (Full DMG), §bLinh Tử Băng Hoại§7 & §eNấc Thang Jacob§7 (1 hit nhưng nó sẽ né)"), false);
            } else {
                player.displayClientMessage(Component.literal("§c  Đặc Tính Đòn Đánh: §4XUYÊN GIÁP THẦN THOẠI §7(Chỉ bị chặn bởi Trận Đồ Cưỡng Chế)"), false);
                player.displayClientMessage(Component.literal("§a  Khắc Chế Chiến Thuật: §fĐại Thánh Tẩy hoặc tiêu diệt trước khi nó kịp ăn thêm linh hồn"), false);
            }
        }
        player.displayClientMessage(Component.literal("§b§l╚════════════════════════════════════════════════╝"), false);

        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.2F, 1.8F);
    }

    /**
     * Thẩm định VẬT VÔ TRI: Khối Block / Rương Chứa Đồ / Quặng Mỏ
     */
    public static void appraiseBlock(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        String blockName = block.getName().getString();
        float hardness = state.getDestroySpeed(level, pos);
        float blastRes = block.getExplosionResistance();

        BlockEntity be = level.getBlockEntity(pos);

        player.displayClientMessage(Component.literal("§a§l╔════════════════════════════════════════════════╗"), false);
        player.displayClientMessage(Component.literal("§a§l║   §e§l✦ THẨM ĐỊNH VẠN VẬT: CẤU TRÚC VÔ TRI ✦   §a§l║"), false);
        player.displayClientMessage(Component.literal("§a§l╠════════════════════════════════════════════════╣"), false);
        player.displayClientMessage(Component.literal("§f  Khối Block: §6" + blockName), false);
        player.displayClientMessage(Component.literal("§7  Tọa Độ: §eX: " + pos.getX() + ", Y: " + pos.getY() + ", Z: " + pos.getZ()), false);
        player.displayClientMessage(Component.literal("§b  Độ Cứng Khai Thác: §f" + hardness + " §7| §cKháng Nổ: §f" + blastRes), false);

        // Phân tích nếu là Thùng Chứa Đồ (Rương, Shulker, v.v.)
        if (be instanceof Container container) {
            int totalItems = 0;
            Map<String, Integer> valuableItems = new HashMap<>();

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack item = container.getItem(i);
                if (!item.isEmpty()) {
                    totalItems += item.getCount();
                    String iName = item.getHoverName().getString();
                    valuableItems.put(iName, valuableItems.getOrDefault(iName, 0) + item.getCount());
                }
            }

            player.displayClientMessage(Component.literal("§e  Phát Hiện Thùng Chứa Đồ: §a" + totalItems + " vật phẩm bên trong"), false);
            if (!valuableItems.isEmpty()) {
                int shown = 0;
                for (var entry : valuableItems.entrySet()) {
                    if (shown++ >= 4) {
                        player.displayClientMessage(Component.literal("    §7... và các vật phẩm khác"), false);
                        break;
                    }
                    player.displayClientMessage(Component.literal("    §f• §b" + entry.getKey() + " §7x" + entry.getValue()), false);
                }
            }
        } else if (state.getBlock().getDescriptionId().toLowerCase().contains("ore") || state.getBlock().getName().getString().toLowerCase().contains("quặng")) {
            player.displayClientMessage(Component.literal("§6  Phân Tích Địa Chất: §eQuặng Mỏ Tự Nhiên giàu khoáng thạch ma pháp"), false);
        }

        player.displayClientMessage(Component.literal("§a§l╚════════════════════════════════════════════════╝"), false);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.2F, 1.6F);
    }

    /**
     * Thẩm định VẬT VÔ TRI: ItemEntity rơi trên mặt đất
     */
    public static void appraiseItemEntity(ServerPlayer player, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        String itemName = stack.getHoverName().getString();
        int count = stack.getCount();
        int maxStack = stack.getMaxStackSize();
        int maxDamage = stack.getMaxDamage();
        int curDamage = stack.getDamageValue();

        player.displayClientMessage(Component.literal("§6§l╔════════════════════════════════════════════════╗"), false);
        player.displayClientMessage(Component.literal("§6§l║   §e§l✦ THẨM ĐỊNH VẬT PHẨM TỰ DO (ITEM) ✦   §6§l║"), false);
        player.displayClientMessage(Component.literal("§6§l╠════════════════════════════════════════════════╣"), false);
        player.displayClientMessage(Component.literal("§f  Vật Phẩm: §e" + itemName + " §7(Số lượng: " + count + "/" + maxStack + ")"), false);
        if (maxDamage > 0) {
            player.displayClientMessage(Component.literal("§c  Độ Bền: §a" + (maxDamage - curDamage) + " / " + maxDamage), false);
        }
        player.displayClientMessage(Component.literal("§b  Phẩm Cấp: §f" + stack.getRarity().name()), false);
        player.displayClientMessage(Component.literal("§6§l╚════════════════════════════════════════════════╝"), false);
    }

    public static void tickPlayer(ServerPlayer player) {
        CompoundTag tag = EntityDataHelper.getCustomData(player);
        int hudTicks = tag.getInt(NBT_HUD_TICKS);
        if (hudTicks <= 0) return;

        tag.putInt(NBT_HUD_TICKS, hudTicks - 1);

        // Mỗi 10 ticks quét nhanh tiêu điểm trước mặt 8 blocks
        if (player.tickCount % 10 == 0) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 reach = eye.add(look.scale(8.0D));

            // Quét thực thể trước
            AABB box = player.getBoundingBox().expandTowards(look.scale(8.0D)).inflate(1.0D);
            List<Entity> nearby = player.serverLevel().getEntities(player, box, e -> e.isAlive());
            Entity focused = null;
            for (Entity e : nearby) {
                if (e.getBoundingBox().inflate(0.3D).clip(eye, reach).isPresent()) {
                    focused = e;
                    break;
                }
            }

            if (focused instanceof LivingEntity living) {
                String name = living.getDisplayName().getString();
                long ep = estimateEP(living);
                player.displayClientMessage(
                        Component.literal("§b[THẨM ĐỊNH] §e" + name + " §7| §cHP: " + String.format("%.0f/%.0f", living.getHealth(), living.getMaxHealth()) + " §7| §dEP: " + NUM_FORMAT.format(ep)),
                        true
                );
            } else {
                // Quét block
                BlockHitResult bh = player.serverLevel().clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                if (bh.getType() == HitResult.Type.BLOCK) {
                    BlockState bs = player.serverLevel().getBlockState(bh.getBlockPos());
                    player.displayClientMessage(
                            Component.literal("§a[THẨM ĐỊNH] §fKhối: §6" + bs.getBlock().getName().getString() + " §7(Độ cứng: " + bs.getDestroySpeed(player.serverLevel(), bh.getBlockPos()) + ")"),
                            true
                    );
                }
            }
        }
    }

    private static String determineSpecies(LivingEntity entity) {
        if (entity instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity) return "Tà Thần Hư Vô (Dark Gathering)";
        if (entity instanceof VelgryndEntity) return "Long Chủng Tối Thượng (True Dragon)";
        if (entity instanceof PrimordialDemonEntity demon) return "Thủy Tổ Ác Ma (" + demon.getDemonType().name() + ")";
        if (entity instanceof Player) return "Nhân Loại Thức Tỉnh (Awakened Human / Demon Lord)";
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return "Thượng Cổ Hắc Long (Void Dragon)";
        if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss) return "Tử Linh Ma Tướng (Wither Sovereign)";
        if (entity instanceof net.minecraft.world.entity.monster.warden.Warden) return "Cổ Thần Vực Thẳm (Sculk Abomination)";
        if (entity instanceof Enemy) return "Ma Vật Thù Địch (Monster)";
        return "Sinh Mệnh Tự Nhiên (Natural Creature)";
    }

    private static long estimateEP(LivingEntity entity) {
        if (entity instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity) return 12500000L;
        if (entity instanceof VelgryndEntity) return 74350000L;
        if (entity instanceof PrimordialDemonEntity demon) {
            return (demon.hasPhysicalBody() && demon.isNamed()) ? 40000000L : 2800000L;
        }
        if (entity instanceof Player player) {
            boolean isLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
            return isLord ? 15000000L : 850000L;
        }
        return (long) (entity.getMaxHealth() * 1250L);
    }
}
