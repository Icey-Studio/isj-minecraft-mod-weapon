package com.minhphuc.weapons.content.tensura;

import com.minhphuc.weapons.content.evolution.UniversalSenseAbility;
import com.minhphuc.weapons.content.soul.EntitySoulItem;
import com.minhphuc.weapons.content.soul.FakeSoulItem;
import com.minhphuc.weapons.entity.tensura.MilimEntity;
import com.minhphuc.weapons.entity.tensura.PrimordialDemonEntity;
import com.minhphuc.weapons.entity.tensura.VelgryndEntity;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tuyệt Kỹ: Ngày Tận Thế (Apocalypse - Soul Harvest Judgment)
 * - Triển khai Đại Ma Trận Phán Quyết Ngày Tận Thế đa tầng xoay ngược chiều trên bầu trời và mặt đất (Bán kính 64m).
 * - Rút toàn bộ Linh Hồn của các sinh vật xấu xa / thù địch đối với người chơi và giết chết chúng (trừ Chước Nhiệt Long Velgrynd & Milim Nava).
 * - Đối với các sinh vật hiền lành trong phạm vi: Tự động cấy Linh Hồn Giả thế chỗ rồi rút Linh Hồn thật ra để sinh vật đó KHÔNG bị chết.
 * - Riêng Ác Ma Thủy Tổ đã kí khế ước với người chơi hoàn toàn không bị ảnh hưởng.
 * - Đưa toàn bộ linh hồn thu hoạch được vào túi đồ người chơi; nếu túi đồ đầy sẽ thông báo rõ ràng và thả linh hồn dưới chân người chơi.
 */
public class ApocalypseAbility {

    public static final double APOCALYPSE_RADIUS = 64.0D;
    private static final List<ActiveApocalypse> ACTIVE_APOCALYPSES = new ArrayList<>();

    public static class ActiveApocalypse {
        public final ServerLevel level;
        public final ServerPlayer caster;
        public final Vec3 center;
        public int tick = 0;
        public final int maxTicks = 85;
        public float angle = 0.0F;
        public boolean judgmentExecuted = false;

        public Display.ItemDisplay groundCircle;
        public Display.ItemDisplay midSoulRing;
        public Display.ItemDisplay skyRuneRing;
        public Display.ItemDisplay verticalCrest;

        public ActiveApocalypse(ServerLevel level, ServerPlayer caster, Vec3 center,
                                Display.ItemDisplay groundCircle,
                                Display.ItemDisplay midSoulRing,
                                Display.ItemDisplay skyRuneRing,
                                Display.ItemDisplay verticalCrest) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.groundCircle = groundCircle;
            this.midSoulRing = midSoulRing;
            this.skyRuneRing = skyRuneRing;
            this.verticalCrest = verticalCrest;
        }

        public void cleanup() {
            if (groundCircle != null && groundCircle.isAlive()) groundCircle.discard();
            if (midSoulRing != null && midSoulRing.isAlive()) midSoulRing.discard();
            if (skyRuneRing != null && skyRuneRing.isAlive()) skyRuneRing.discard();
            if (verticalCrest != null && verticalCrest.isAlive()) verticalCrest.discard();
        }
    }

    public static void cast(ServerLevel level, ServerPlayer player) {
        Vec3 center = player.position();

        Display.ItemDisplay groundCircle = createFlatDisplay(level, center, 0.08D, 0.5F,
                ModItems.DISINTEGRATION_GROUND_ARRAY.get(), 0xFFCC0022);
        Display.ItemDisplay midSoulRing = createFlatDisplay(level, center, 5.5D, 0.5F,
                ModItems.MAGIC_CIRCLE_NOIR.get(), 0xFF9900FF);
        Display.ItemDisplay skyRuneRing = createFlatDisplay(level, center, 13.5D, 0.5F,
                ModItems.DISINTEGRATION_RUNE_RING.get(), 0xFFFF3300);
        Display.ItemDisplay verticalCrest = createVerticalDisplay(level, center, 17.5D, 0.5F,
                ModItems.DISINTEGRATION_VERTICAL_CREST.get(), 0xFFFF0044);

        ACTIVE_APOCALYPSES.add(new ActiveApocalypse(level, player, center,
                groundCircle, midSoulRing, skyRuneRing, verticalCrest));

        // Hiệu ứng âm thanh & thông báo mở màn Ngày Tận Thế
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.55F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 0.65F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.5F, 0.7F);

        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l✦ NGÀY TẬN THẾ ✦")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c§oPhán Quyết Linh Hồn Khởi Động...")));
        player.displayClientMessage(
                Component.literal("§4§l[NGÀY TẬN THẾ] §cĐại Ma Trận Phán Quyết Linh Hồn đã bao phủ bán kính §e64m§c!"),
                true
        );
    }

    public static void tickApocalypses(ServerLevel level) {
        if (ACTIVE_APOCALYPSES.isEmpty()) return;

        Iterator<ActiveApocalypse> it = ACTIVE_APOCALYPSES.iterator();
        while (it.hasNext()) {
            ActiveApocalypse ap = it.next();
            if (ap.level != level) continue;

            if (ap.caster == null || !ap.caster.isAlive() || ap.caster.hasDisconnected()) {
                ap.cleanup();
                it.remove();
                continue;
            }

            ap.tick++;
            ap.angle = (ap.angle + 4.5F) % 360.0F;

            // =====================================================================
            // GIAI ĐOẠN 1 (Ticks 1..30): ĐẠI MA TRẬN BUNG NỞ & KHÓA LINH HỒN
            // =====================================================================
            if (ap.tick <= 30) {
                float progress = ap.tick / 30.0F;
                float ease = 1.0F - (float) Math.pow(1.0F - progress, 3);

                updateFlatTransform(ap.groundCircle, 28.0F * ease, ap.angle);
                updateFlatTransform(ap.midSoulRing, 18.0F * ease, -ap.angle * 1.4F);
                updateFlatTransform(ap.skyRuneRing, 24.0F * ease, ap.angle * 1.2F);
                updateVerticalTransform(ap.verticalCrest, 11.0F * ease);

                // Vòng xoáy Hắc Hỏa & Linh Hồn dâng lên quanh người thi triển
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians((ap.tick * 15 + i * 22.5));
                    double r = 2.5D + (i % 4) * 3.5D * ease;
                    double px = ap.center.x + Math.cos(a) * r;
                    double pz = ap.center.z + Math.sin(a) * r;
                    double py = ap.center.y + 0.2D + (ap.tick * 0.25D) % 10.0D;
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0.05D, 0.15D, 0.05D, 0.02D);
                    if (i % 2 == 0) {
                        level.sendParticles(ParticleTypes.SOUL, px, py, pz, 1, 0.05D, 0.2D, 0.05D, 0.03D);
                    }
                }

                if (ap.tick % 10 == 0) {
                    level.playSound(null, ap.center.x, ap.center.y, ap.center.z,
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.5F, 0.6F + progress * 0.4F);
                }
            }
            // =====================================================================
            // GIAI ĐOẠN 2 (Ticks 31..59): CHIẾT XUẤT LINH HỒN & TIA CHỚP PHÁN QUYẾT
            // =====================================================================
            else if (ap.tick < 60) {
                float pulse = 1.0F + 0.05F * (float) Math.sin(ap.tick * 0.5F);
                updateFlatTransform(ap.groundCircle, 28.0F * pulse, ap.angle * 1.5F);
                updateFlatTransform(ap.midSoulRing, 18.0F * pulse, -ap.angle * 2.0F);
                updateFlatTransform(ap.skyRuneRing, 24.0F * pulse, ap.angle * 1.8F);
                updateVerticalTransform(ap.verticalCrest, 11.0F * pulse);

                if (ap.tick % 4 == 0) {
                    AABB box = new AABB(ap.center, ap.center).inflate(APOCALYPSE_RADIUS);
                    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && e != ap.caster);

                    Vec3 playerChest = ap.caster.position().add(0, 1.1D, 0);
                    int streamCount = 0;

                    for (LivingEntity target : targets) {
                        if (PrimordialDemonEntity.isContractedAllyOfPlayer(ap.caster.getUUID(), target)) continue;
                        if (target instanceof VelgryndEntity || target instanceof MilimEntity) continue;

                        boolean hostile = UniversalSenseAbility.isHostileThreat(target, ap.caster);
                        Vec3 tPos = target.position().add(0, target.getBbHeight() * 0.5D, 0);

                        if (hostile) {
                            // Kẻ địch xấu xa: Bị nhấc bổng nhẹ và cột lửa linh hồn trói buộc
                            target.setDeltaMovement(0, 0.08D, 0);
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tPos.x, tPos.y, tPos.z, 4, 0.3D, 0.4D, 0.3D, 0.03D);
                            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tPos.x, tPos.y + 0.5D, tPos.z, 2, 0.2D, 0.2D, 0.2D, 0.02D);
                        } else {
                            // Sinh vật hiền lành: Vòng bảo hộ Linh Hồn Giả màu lam ngọc bao bọc
                            level.sendParticles(ParticleTypes.END_ROD, tPos.x, tPos.y, tPos.z, 3, 0.3D, 0.4D, 0.3D, 0.02D);
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tPos.x, tPos.y + 0.4D, tPos.z, 2, 0.25D, 0.25D, 0.25D, 0.02D);
                        }

                        // Dòng chảy hạt linh hồn bay từ mục tiêu về ngực người chơi (giới hạn số tia mỗi tick để mượt)
                        if (streamCount < 20) {
                            streamCount++;
                            double progressStream = (ap.tick - 30) / 30.0D;
                            Vec3 streamPos = tPos.lerp(playerChest, progressStream);
                            level.sendParticles(ParticleTypes.SCULK_SOUL, streamPos.x, streamPos.y, streamPos.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
                            level.sendParticles(ParticleTypes.SOUL, streamPos.x, streamPos.y, streamPos.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
                        }
                    }

                    level.playSound(null, ap.center.x, ap.center.y, ap.center.z,
                            SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 2.0F, 0.7F + (ap.tick - 30) * 0.03F);
                }
            }
            // =====================================================================
            // GIAI ĐOẠN 3 (Tick 60): PHÁN QUYẾT CUỐI CÙNG & THU HOẠCH LINH HỒN
            // =====================================================================
            else if (ap.tick == 60 && !ap.judgmentExecuted) {
                ap.judgmentExecuted = true;
                executeApocalypseHarvest(level, ap.caster, ap.center);
            }
            // =====================================================================
            // GIAI ĐOẠN 4 (Ticks 61..85): THU HỒI ĐẠI MA TRẬN & DƯ CHẤN
            // =====================================================================
            else if (ap.tick > 60) {
                float fade = Math.max(0.01F, 1.0F - (ap.tick - 60) / 25.0F);
                updateFlatTransform(ap.groundCircle, 28.0F * fade, ap.angle * 2.0F);
                updateFlatTransform(ap.midSoulRing, 18.0F * fade, -ap.angle * 2.5F);
                updateFlatTransform(ap.skyRuneRing, 24.0F * fade, ap.angle * 2.2F);
                updateVerticalTransform(ap.verticalCrest, 11.0F * fade);

                for (int i = 0; i < 12; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0D;
                    double dist = level.random.nextDouble() * 14.0D;
                    double px = ap.center.x + Math.cos(angle) * dist;
                    double pz = ap.center.z + Math.sin(angle) * dist;
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, ap.center.y + 0.5D, pz, 1, 0.0D, 0.25D, 0.0D, 0.1D);
                }
            }

            if (ap.tick >= ap.maxTicks) {
                ap.cleanup();
                it.remove();
            }
        }
    }

    private static void executeApocalypseHarvest(ServerLevel level, ServerPlayer caster, Vec3 center) {
        AABB box = new AABB(center, center).inflate(APOCALYPSE_RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != caster);

        int hostileKilledCount = 0;
        int peacefulSwappedCount = 0;
        int totalSoulsHarvested = 0;
        boolean inventoryFull = false;
        boolean exemptedBossPresent = false;

        for (LivingEntity target : entities) {
            // 1. Không ảnh hưởng người chơi ở chế độ Sáng Tạo / Khán Giả
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
                continue;
            }

            // 2. Riêng Ác Ma đã kí khế ước với người chơi KHÔNG bị ảnh hưởng
            if (PrimordialDemonEntity.isContractedAllyOfPlayer(caster.getUUID(), target)) {
                continue;
            }

            // 3. Chước Nhiệt Long (Velgrynd) và Milim Nava được miễn trừ hoàn toàn
            if (target instanceof VelgryndEntity || target instanceof MilimEntity) {
                exemptedBossPresent = true;
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0D, target.getZ(),
                        25, 0.5D, 0.8D, 0.5D, 0.08D);
                continue;
            }

            boolean isHostile = UniversalSenseAbility.isHostileThreat(target, caster);

            if (isHostile) {
                // =================================================================
                // SINH VẬT XẤU XA / THÙ ĐỊCH: RÚT LINH HỒN & GIẾT CHẾT TỨC KHẮC
                // =================================================================
                ItemStack soulStack = EntitySoulItem.createSoulFor(target);
                if (!caster.getInventory().add(soulStack)) {
                    inventoryFull = true;
                    ItemEntity drop = new ItemEntity(level, caster.getX(), caster.getY() + 0.5D, caster.getZ(), soulStack);
                    drop.setNoPickUpDelay();
                    level.addFreshEntity(drop);
                }
                totalSoulsHarvested++;
                hostileKilledCount++;

                // Hiệu ứng cột phán quyết giáng xuống kẻ địch
                level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0D, target.getZ(),
                        20, 0.4D, 0.6D, 0.4D, 0.1D);
                level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0D, target.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);

                // Kích hoạt rơi linh hồn Ma Vương phụ trợ nếu có
                TensuraEvents.handleMobDeathDrop(caster, target);

                // Giết chết tức khắc
                target.invulnerableTime = 0;
                target.hurt(caster.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                if (target.isAlive()) {
                    target.setHealth(0.0F);
                    target.die(caster.damageSources().playerAttack(caster));
                }
            } else {
                // =================================================================
                // SINH VẬT HIỀN LÀNH: THAY LINH HỒN GIẢ VÀO VÀ RÚT LINH HỒN THẬT RA
                // ĐỂ SINH VẬT ĐÓ KHÔNG CHẾT!
                // =================================================================
                target.addTag(FakeSoulItem.TAG_HAS_FAKE_SOUL);

                ItemStack soulStack = EntitySoulItem.createSoulFor(target);
                if (!caster.getInventory().add(soulStack)) {
                    inventoryFull = true;
                    ItemEntity drop = new ItemEntity(level, caster.getX(), caster.getY() + 0.5D, caster.getZ(), soulStack);
                    drop.setNoPickUpDelay();
                    level.addFreshEntity(drop);
                }
                totalSoulsHarvested++;
                peacefulSwappedCount++;

                // Hiệu ứng bảo toàn sinh mạng bằng Linh Hồn Giả
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1.0D, target.getZ(),
                        15, 0.35D, 0.5D, 0.35D, 0.08D);
                level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 1.0D, target.getZ(),
                        10, 0.3D, 0.4D, 0.3D, 0.05D);
            }
        }

        // Hiệu ứng chấn động toàn vùng tại tâm thi triển
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0D, center.z, 3, 2.0D, 1.0D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 1.5D, center.z, 120, 6.0D, 2.0D, 6.0D, 0.15D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.6F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0F, 0.85F);

        // Thông báo tổng kết
        caster.sendSystemMessage(Component.literal(
                "§4§l✦ [NGÀY TẬN THẾ - PHÁN QUYẾT HOÀN TẤT] ✦\n" +
                "§c• Đã rút linh hồn & xóa sổ sinh vật thù địch: §e" + hostileKilledCount + "\n" +
                "§b• Đã tráo Linh Hồn Giả & rút linh hồn sinh vật hiền lành (không gây chết): §a" + peacefulSwappedCount + "\n" +
                "§d• Tổng số Linh Hồn thu hoạch vào túi đồ: §6§l" + totalSoulsHarvested + " Linh Hồn"
        ));

        if (exemptedBossPresent) {
            caster.sendSystemMessage(Component.literal(
                    "§6§l[MIỄN TRỪ TUYỆT ĐỐI] §eChước Nhiệt Long Velgrynd / Milim Nava sở hữu Bản Thể Tối Thượng nên không bị ảnh hưởng bởi Ngày Tận Thế!"
            ));
        }

        if (inventoryFull) {
            caster.sendSystemMessage(Component.literal(
                    "§c§l⚠️ [CẢNH BÁO TÚI ĐỒ ĐẦY] §eTúi đồ của bạn đã đầy! Một số Linh Hồn thu hoạch từ Ngày Tận Thế đã được thả rơi ngay dưới chân bạn!"
            ));
            caster.displayClientMessage(Component.literal(
                    "§c§l⚠️ TÚI ĐỒ ĐẦY! §eLinh hồn dư thừa đã rơi dưới chân bạn!"
            ), true);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.PLAYERS, 2.0F, 0.7F);
        } else {
            caster.displayClientMessage(Component.literal(
                    "§4§l[NGÀY TẬN THẾ] §aThu hoạch thành công §e" + totalSoulsHarvested + " Linh Hồn §7(Diệt " + hostileKilledCount + " kẻ địch | Bảo toàn " + peacefulSwappedCount + " sinh vật hiền lành)!"
            ), true);
        }
    }

    private static Display.ItemDisplay createFlatDisplay(ServerLevel level, Vec3 center, double yOffset,
                                                         float initialScale, Item item, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(item));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(90.0F));
            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(initialScale, initialScale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    private static Display.ItemDisplay createVerticalDisplay(ServerLevel level, Vec3 center, double yOffset,
                                                             float initialScale, Item item, int glowColor) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display != null) {
            display.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
            ItemDisplayAccessor itemDisplayAcc = (ItemDisplayAccessor) display;
            DisplayAccessor displayAcc = (DisplayAccessor) display;

            itemDisplayAcc.weapons$setItemStack(new ItemStack(item));
            itemDisplayAcc.weapons$setItemTransform(ItemDisplayContext.FIXED);
            displayAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
            display.setGlowingTag(true);
            displayAcc.weapons$setGlowColorOverride(glowColor);
            displayAcc.weapons$setViewRange(10.0F);

            displayAcc.weapons$setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(initialScale, initialScale, 0.01F),
                    null
            ));

            level.addFreshEntity(display);
        }
        return display;
    }

    private static void updateFlatTransform(Display.ItemDisplay display, float scale, float angleDegrees) {
        if (display == null || !display.isAlive()) return;
        DisplayAccessor displayAcc = (DisplayAccessor) display;
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(angleDegrees))
                .rotateX((float) Math.toRadians(90.0F));
        displayAcc.weapons$setTransformation(new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                rotation,
                new Vector3f(scale, scale, 0.01F),
                null
        ));
    }

    private static void updateVerticalTransform(Display.ItemDisplay display, float scale) {
        if (display == null || !display.isAlive()) return;
        DisplayAccessor displayAcc = (DisplayAccessor) display;
        displayAcc.weapons$setTransformation(new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Quaternionf(),
                new Vector3f(scale, scale, 0.01F),
                null
        ));
    }
}
