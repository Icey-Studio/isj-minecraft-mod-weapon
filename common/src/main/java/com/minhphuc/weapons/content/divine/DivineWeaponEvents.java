package com.minhphuc.weapons.content.divine;

import com.minhphuc.weapons.content.tensura.BeelzebuthAbility;
import com.minhphuc.weapons.content.tensura.TensuraSkillDispatcher;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DivineWeaponEvents {

    public static void register() {
        EntityEvent.LIVING_HURT.register(DivineWeaponEvents::onLivingHurt);

        // Gom toan bo cac tick cap nhat ability vao 1 listener duy nhat de giam overhead dispatch
        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_POST.register(level -> {
            SanctuaryDisintegrationAbility.tickSanctuaries(level);
            JacobsLadderAbility.tickLadders(level);
            PurificationPillarAbility.tickPillars(level);
            FaithAndGraceAbility.tickFaithAndGraces(level);
            BeelzebuthAbility.tickDragons(level);
            com.minhphuc.weapons.content.tensura.HarvestFestival.tickRituals(level);
            com.minhphuc.weapons.content.tensura.DragonNovaAbility.tickDragonNovas(level);
            com.minhphuc.weapons.content.darkgathering.TaisuiExtinctionStarsAbility.tickTaisuiStates(level);
            com.minhphuc.weapons.content.darkgathering.SeerFleshAbility.tickFleshes(level);
            com.minhphuc.weapons.content.darkgathering.AlkaidAbility.tickAlkaids(level);
            com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility.tickAbilities(level);
            com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility.tickAbilities(level);
            com.minhphuc.weapons.content.darkgathering.SeveredHeartCurseManager.tick(level);
            com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility.tickAbilities(level);
            com.minhphuc.weapons.content.darkgathering.ThienMaCurseManager.tick(level);
            com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility.tick(level);
            com.minhphuc.weapons.content.darkgathering.CuuTheCurseManager.tick(level);
            com.minhphuc.weapons.content.darkgathering.WardingEarplugsItem.tickProtection(level);
            com.minhphuc.weapons.content.darkgathering.DigitalLedDisplayHelper.tickLeds(level);
            com.minhphuc.weapons.content.darkgathering.SamuraiSpiritDisplayHelper.tickSpirits(level);
        });

        // Dọn dẹp tàn ảnh khi thế giới dỡ bỏ để chống rò rỉ bộ nhớ
        dev.architectury.event.events.common.LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            com.minhphuc.weapons.content.darkgathering.TengaiKyuseiSwordAbility.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.SeveredHeartCurseManager.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.ThienMaCurseManager.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.CuuTheCurseManager.cleanup(level);
            com.minhphuc.weapons.content.darkgathering.DigitalLedDisplayHelper.clearAll();
            com.minhphuc.weapons.content.darkgathering.SamuraiSpiritDisplayHelper.clearAll();
        });

        // Dọn dẹp session khi người chơi thoát game
        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(serverPlayer -> {
            com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility.onPlayerQuit(serverPlayer);
            com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility.onPlayerQuit(serverPlayer);
            com.minhphuc.weapons.content.darkgathering.RykuzinSinkaAbility.onPlayerQuit(serverPlayer);
        });

        // Đánh vào Thị Thực bằng chuột trái: Rạch lấy Con Mắt Thị Thực
        dev.architectury.event.events.common.PlayerEvent.ATTACK_ENTITY.register(com.minhphuc.weapons.content.darkgathering.SeerFleshAbility::onAttack);
        dev.architectury.event.events.common.PlayerEvent.ATTACK_ENTITY.register(com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility::onPlayerAttackEntity);
        dev.architectury.event.events.common.PlayerEvent.ATTACK_ENTITY.register(com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility::onPlayerAttackEntity);

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

            // 1. Tương tác chuột phải với khối Thị Thực (Hồi 100% HP & Giải trừ độc tố)
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
                            Component.literal("§c§l[CON MẮT THỊ THỰC] §aĐã chữa lành toàn diện cho " + targetLiving.getName().getString() + "! Hồi 100% máu & hóa giải mọi nguyền rủa! ✨👁️"),
                            true
                    );

                    return EventResult.interruptTrue();
                }
            }

            // 3. Chuột phải vào sinh vật khi tay không: Thi triển Kỹ Năng Chân Ma Vương / Thần Linh Vũ Trang (Không có thời gian hồi chiêu)
            if (player.getItemInHand(hand).isEmpty() && player instanceof ServerPlayer serverPlayer) {
                boolean isTrueDemonLord = EntityDataHelper.getCustomData(serverPlayer).getBoolean("TensuraTrueDemonLord");
                boolean isWearingDivineArmor = DivineArmorItem.isWearingAnyPiece(serverPlayer);
                boolean isPrimordial = com.minhphuc.weapons.content.tensura.PrimordialPlayerDataHelper.isPrimordial(serverPlayer);

                if (isTrueDemonLord || isWearingDivineArmor || isPrimordial) {
                    int selectedSkill = EntityDataHelper.getCustomData(serverPlayer).getInt("TensuraDemonLordSkill");
                    boolean hasCreation = EntityDataHelper.getCustomData(serverPlayer).getBoolean("TensuraMaterialCreation");
                    int lordSkills = TensuraSkillDispatcher.getSkillCount(hasCreation);

                    ServerLevel sl = (ServerLevel) serverPlayer.level();
                    if (selectedSkill < lordSkills) {
                        TensuraSkillDispatcher.executeSkill(sl, serverPlayer, selectedSkill, hasCreation);
                    } else if (isPrimordial) {
                        int primordialSkillIdx = selectedSkill - lordSkills;
                        com.minhphuc.weapons.content.tensura.PrimordialSkillDispatcher.castSkill(sl, serverPlayer, primordialSkillIdx);
                    }
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });
    }

    public static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        // Kiểm tra nếu mục tiêu đang trong trạng thái ngưng đọng thời gian của Bạt Đao Thuật hoặc Thiên Ma Kiếm
        if (com.minhphuc.weapons.content.darkgathering.ThienMaSwordAbility.isEntityFrozenInSession(victim)
                || com.minhphuc.weapons.content.darkgathering.ReverseVelocityIaijutsuAbility.isEntityFrozenInSession(victim)) {
            return EventResult.interruptFalse();
        }

        // =========================================================================
        // 1. PHÒNG NGỰ TUYỆT ĐỐI CỦA URIEL & THẦN LINH VŨ TRANG (DIVINE ARMOR)
        // =========================================================================
        if (victim instanceof Player player) {
            boolean isWearingFullSet = DivineArmorItem.isWearingFullSet(player);
            boolean isTrueDemonLord = EntityDataHelper.getCustomData(player).getBoolean("TensuraTrueDemonLord");
            boolean hasUriel = isWearingFullSet || isTrueDemonLord;

            if (hasUriel && com.minhphuc.weapons.content.evolution.EvolvedSkillHelper.isUrielDefenseActive(player)) {
                // Kiểm tra các trường hợp bị đánh xuyên giáp (như Không Vong, Linh Tử, Void...)
                boolean isVoid = source.is(DamageTypes.FELL_OUT_OF_WORLD);
                boolean isKuboPenetration = source.getEntity() instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity
                        || source.getDirectEntity() instanceof com.minhphuc.weapons.entity.darkgathering.KuboEntity
                        || player.getTags().contains("KuboPenetrationDamage");
                boolean isDisintegration = player.getTags().contains("DisintegrationDamage");
                boolean isVelgryndPenetration = player.getTags().contains("VelgryndPenetrationDamage");

                if (isKuboPenetration || isDisintegration || isVoid || isVelgryndPenetration) {
                    player.displayClientMessage(
                            Component.literal("§c§l[BÁO CÁO] §fBáo cáo: Phòng thủ tuyệt đối của Uriel không thể ngăn chặn tuyệt đối các linh tử."),
                            true
                    );
                    // Để sát thương xuyên qua!
                } else {
                    // Miễn nhiễm hoàn toàn 100% mọi sát thương
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.6F);
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
                        // Đòn chém 2: Máu <= 80% max HP -> Kết liễu chết luôn!
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
