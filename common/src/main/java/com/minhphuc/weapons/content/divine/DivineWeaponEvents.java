package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.content.tensura.BeelzebuthAbility;
import com.minhphuc.weapons.data.EntityDataHelper;
import com.minhphuc.weapons.init.ModItems;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DivineWeaponEvents {

    public static void register() {
        EntityEvent.LIVING_HURT.register(DivineWeaponEvents::onLivingHurt);

        // Tick cập nhật các Thánh Giới Linh Tử Băng Hoại đang kích hoạt
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(SanctuaryDisintegrationAbility::tickSanctuaries);

        // Tick cập nhật Tà Khứ Vũ Thê Tử (Jacob's Ladder)
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(JacobsLadderAbility::tickLadders);

        // Tick cập nhật Bát Môn Thiên Phạt Trận (Heavenly Judgment Array)
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(HeavenlyJudgmentArrayAbility::tickArrays);

        // Tick cập nhật Đại Thánh Tẩy - Quang Minh Cứu Rỗi (Great Purification)
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(PurificationPillarAbility::tickPillars);

        // Tick cập nhật Đầu Rồng Hư Không Bạo Thực Vương Beelzebuth
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(BeelzebuthAbility::tickDragons);

        // Tick cập nhật Lễ Hội Thu Hoạch Thức Tỉnh Chân Ma Vương (Harvest Festival)
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(com.minhphuc.weapons.content.tensura.HarvestFestival::tickRituals);

        // Tick cập nhật Long Tinh Bộc Viêm Bá: Dragon Nova
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(com.minhphuc.weapons.content.tensura.DragonNovaAbility::tickDragonNovas);

        // Tick cập nhật Thái Tuế Tinh Quân: Tuyệt Diệt Tinh Tú
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility::tickTaisuiStates);

        // Tick cập nhật Thái Tuế Tinh Quân: Lớp Phòng Ngự Lục Nhậm Thần Khóa
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(com.minhphuc.weapons.content.darkgathering.LiuRenBarrierAbility::tickBarriers);

        // Tick cập nhật Thái Tuế Tinh Quân: Thị Nhục (Seer Flesh)
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(com.minhphuc.weapons.content.darkgathering.SeerFleshAbility::tickFleshes);

        // Tick cập nhật Thái Tuế Tinh Quân: Diệt Thế Tà Tinh - Alkaid
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(com.minhphuc.weapons.content.darkgathering.AlkaidAbility::tickAlkaids);

        // Đánh vào Thị Nhục bằng chuột trái: Rạch lấy Con Mắt Thị Nhục
        dev.architectury.event.events.common.PlayerEvent.ATTACK_ENTITY.register(com.minhphuc.weapons.content.darkgathering.SeerFleshAbility::onAttack);

        // Đồng bộ trạng thái Tuyệt Diệt Tinh Tú khi người chơi tham gia thế giới
        dev.architectury.event.events.common.PlayerEvent.PLAYER_JOIN.register(serverPlayer -> {
            com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.ACTIVE_TAISUI.forEach((uuid, state) -> {
                if (state.isCharged && state.starsRemaining > 0) {
                    com.minhphuc.weapons.network.ModMessages.sendToPlayer(
                            new com.minhphuc.weapons.network.ClientboundSyncTaisuiPacket(uuid, true, state.starsRemaining),
                            serverPlayer
                    );
                }
            });
        });

        // Tương tác chuột phải với sinh vật / thực thể
        InteractionEvent.INTERACT_ENTITY.register((player, target, hand) -> {
            if (player.level().isClientSide()) return EventResult.pass();

            // 1. Tương tác chuột phải với khối Thị Nhục (Hồi 100% HP & Giải trừ độc tố)
            EventResult fleshResult = com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.onInteract(player, target, hand);
            if (fleshResult.interruptsFurtherEvaluation()) {
                return fleshResult;
            }

            // 2. Cầm [Con Mắt Thị Nhục] chuột phải vào sinh vật khác: Chữa lành 100% HP & Xóa mọi hiệu ứng xấu
            if (target instanceof LivingEntity targetLiving) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof com.minhphuc.weapons.content.darkgathering.SeerFleshEyeItem) {
                    targetLiving.setHealth(targetLiving.getMaxHealth());
                    com.minhphuc.weapons.content.darkgathering.SeerFleshEyeItem.cleanseHarmfulEffects(targetLiving);
                    targetLiving.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 2, false, false, true));
                    targetLiving.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 300, 1, false, false, true));

                    player.level().playSound(null, targetLiving.getX(), targetLiving.getY(), targetLiving.getZ(),
                            SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 1.1F);
                    player.level().playSound(null, targetLiving.getX(), targetLiving.getY(), targetLiving.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5F, 1.6F);

                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, targetLiving.getX(), targetLiving.getY() + 1.0D, targetLiving.getZ(), 10, 0.4D, 0.4D, 0.4D, 0.05D);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, targetLiving.getX(), targetLiving.getY() + 1.0D, targetLiving.getZ(), 25, 0.3D, 0.5D, 0.3D, 0.15D);
                    }

                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }

                    player.displayClientMessage(
                            Component.literal("§c§l[CON MẮT THỊ NHỤC] §aĐã chữa lành toàn diện cho " + targetLiving.getName().getString() + "! Hồi 100% máu & hóa giải mọi nguyền rủa! ✨👁️"),
                            true
                    );

                    return EventResult.interruptTrue();
                }
            }

            // 3. Chuột phải vào sinh vật khi tay không: Thi triển Kỹ Năng Chân Ma Vương (Không cần cầm kiếm)
            if (player.getItemInHand(hand).isEmpty() && player instanceof ServerPlayer serverPlayer) {
                boolean isTrueDemonLord = EntityDataHelper.getCustomData(serverPlayer).getBoolean("TensuraTrueDemonLord");
                if (isTrueDemonLord) {
                    int selectedSkill = EntityDataHelper.getCustomData(serverPlayer).getInt("TensuraDemonLordSkill");
                    if (selectedSkill != 6 && selectedSkill != 8 && selectedSkill != 9 && com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.isTaisuiActive(serverPlayer)) {
                        serverPlayer.displayClientMessage(
                            Component.literal("§c⚠️ Đang trong trạng thái Tuyệt Diệt Tinh Tú! Chỉ có thể kết hợp kích hoạt Thị Nhục hoặc Diệt Thế Tà Tinh (Alkaid)!"),
                            true
                        );
                        return EventResult.interruptTrue();
                    }

                    if (serverPlayer.getCooldowns().isOnCooldown(ModItems.DEMON_LORD_SEED.get())) {
                        return EventResult.interruptTrue();
                    }
                    ServerLevel sl = (ServerLevel) serverPlayer.level();
                    if (selectedSkill == 13) {
                        com.minhphuc.weapons.content.tensura.LuciferReplicationAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 12) {
                        com.minhphuc.weapons.content.tensura.AllOfCreationAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 11) {
                        com.minhphuc.weapons.content.tensura.ThoughtAccelerationAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 10) {
                        com.minhphuc.weapons.content.tensura.HorizontalHolyBeamAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 9) {
                        com.minhphuc.weapons.content.darkgathering.AlkaidAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 8) {
                        com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 7) {
                        com.minhphuc.weapons.content.darkgathering.LiuRenBarrierAbility.toggleBarrier(sl, serverPlayer);
                    } else if (selectedSkill == 6) {
                        com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 5) {
                        com.minhphuc.weapons.content.tensura.DragonNovaAbility.cast(sl, serverPlayer);
                    } else if (selectedSkill == 4) {
                        BeelzebuthAbility.executeBeelzebuth(sl, serverPlayer);
                    } else if (selectedSkill == 3) {
                        PurificationPillarAbility.cast(sl, serverPlayer, ItemStack.EMPTY);
                    } else if (selectedSkill == 2) {
                        HeavenlyJudgmentArrayAbility.cast(sl, serverPlayer, ItemStack.EMPTY);
                    } else if (selectedSkill == 1) {
                        JacobsLadderAbility.cast(sl, serverPlayer, ItemStack.EMPTY);
                    } else {
                        SanctuaryDisintegrationAbility.cast(sl, serverPlayer, ItemStack.EMPTY);
                    }
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });
    }

    public static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        // =========================================================================
        // 0. LỚP PHÒNG NGỰ LỤC NHẬM THẦN KHÓA: Bất tử tuyệt đối 100% (kể cả Sonic Boom của Warden)!
        // =========================================================================
        if (victim instanceof Player player) {
            if (com.minhphuc.weapons.content.darkgathering.LiuRenBarrierAbility.isBarrierActive(player)) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.6F);
                return EventResult.interruptFalse();
            }
        }

        // =========================================================================
        // 1. THẦN LINH VŨ TRANG (DIVINE ARMOR): Kháng gần 100% mọi sát thương trừ Độc & Dưới Nước
        // =========================================================================
        if (victim instanceof Player player) {
            if (DivineArmorItem.isWearingFullSet(player)) {
                // Ngoại lệ 1: Sát thương trúng độc (Poison)
                boolean isPoison = source.is(DamageTypes.MAGIC) && player.hasEffect(MobEffects.POISON);
                // Ngoại lệ 2: Không thở được dưới nước -> Chết đuối (Drown)
                boolean isDrowning = source.is(DamageTypes.DROWN);
                // Ngoại lệ 3: Rơi ra khỏi thế giới (Void)
                boolean isVoid = source.is(DamageTypes.FELL_OUT_OF_WORLD);
                // Ngoại lệ 4: Hỏa diễm Chước Nhiệt Long Velgrynd xuyên phá Thánh Giáp (mỗi 3 đòn gây 30% HP)
                boolean isVelgryndPenetration = player.getTags().contains("VelgryndPenetrationDamage");

                if (!isPoison && !isDrowning && !isVoid && !isVelgryndPenetration) {
                    // Miễn nhiễm hoàn toàn mọi sát thương từ quái vật, người chơi, rơi, lửa, nổ, v.v.
                    return EventResult.interruptFalse();
                }
            }
        }

        // =========================================================================
        // 2. NGUYỆT QUANG THẦN TẾ KIẾM (MOONLIGHT SWORD): Vô Cực Sát Thương & Phá Hủy Giáp
        // =========================================================================
        Entity directAttacker = source.getEntity();
        if (directAttacker instanceof ServerPlayer attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof MoonlightSwordItem) {
                if (victim.level().isClientSide()) return EventResult.pass();

                // 2.1. Phá vỡ toàn bộ giáp & vũ khí đang cầm của mục tiêu trong đòn đánh đầu
                boolean shatteredEquipment = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack equipped = victim.getItemBySlot(slot);
                    if (!equipped.isEmpty()) {
                        victim.setItemSlot(slot, ItemStack.EMPTY);
                        shatteredEquipment = true;
                    }
                }
                if (shatteredEquipment) {
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0F, 0.8F);
                    if (victim.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 20, 0.3D, 0.5D, 0.3D, 0.1D);
                    }
                }

                // 2.2. Kiểm tra mục tiêu có phải là Boss hoặc Sinh vật cực mạnh
                boolean isBoss = victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                        || victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                        || victim instanceof net.minecraft.world.entity.monster.warden.Warden
                        || victim instanceof net.minecraft.world.entity.animal.IronGolem
                        || victim.getMaxHealth() >= 100.0F;

                if (isBoss) {
                    float maxHp = victim.getMaxHealth();
                    float currentHp = victim.getHealth();

                    // Đòn chém 1: Nếu máu còn trên 20% max HP -> Lấy đi 80% máu tối đa
                    if (currentHp > maxHp * 0.20F) {
                        float targetHp = Math.max(1.0F, maxHp * 0.20F);
                        victim.setHealth(targetHp);

                        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F, 1.2F);
                        if (victim.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0, 0, 0, 0);
                            sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 30, 0.5D, 0.5D, 0.5D, 0.2D);
                        }

                        attacker.displayClientMessage(
                            Component.literal("§6§l[NGUYỆT QUANG THẦN TẾ KIẾM] §eTrảm kích thần thoại đã tước đoạt 80% sinh lực của " + victim.getName().getString() + "! (Còn 20% HP)"),
                            true
                        );
                        return EventResult.interruptFalse();
                    } else {
                        // Đòn chém 2: Máu <= 80% max HP (đã qua đòn 1) -> Kết liễu chết luôn!
                        victim.hurt(attacker.level().damageSources().genericKill(), 100000.0F);
                        if (victim.isAlive()) {
                            victim.discard();
                        }
                        if (victim.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 1, 0, 0, 0, 0);
                        }
                        attacker.displayClientMessage(
                            Component.literal("§4§l[NGUYỆT QUANG THẦN TẾ KIẾM] §cTrảm sát kết liễu hoàn toàn " + victim.getName().getString() + "!"),
                            true
                        );
                        return EventResult.interruptFalse();
                    }
                } else {
                    // Quái vật bình thường: Nhất kiếm tất sát 1 hit chết luôn
                    victim.hurt(attacker.level().damageSources().genericKill(), 100000.0F);
                    if (victim.isAlive()) {
                        victim.discard();
                    }
                    if (victim.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 2, 0.2D, 0.2D, 0.2D, 0);
                    }
                    attacker.displayClientMessage(
                        Component.literal("§4§l[NGUYỆT QUANG THẦN TẾ KIẾM] §fNhất kiếm tất sát " + victim.getName().getString() + "!"),
                        true
                    );
                    return EventResult.interruptFalse();
                }
            }
        }

        return EventResult.pass();
    }
}
