package com.minhphuc.weapons.command;

import com.minhphuc.weapons.config.SkillVisualConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LỆNH QUẢN TRỊ /SKILLVISUAL - HỆ THỐNG RUNTIME TOGGLE VÀ SAO LƯU DỰ PHÒNG
 * Cho phép chuyển đổi tức thì giữa chế độ 3D Điện Ảnh (Cinematic 3D) và Bản Cũ (Legacy Classic)
 * cho bất kỳ kỹ năng nào ngay trong game mà không cần reload hay khởi động lại.
 */
public class SkillVisualCommand {

    private static final Map<String, Integer> SKILL_MAP = new LinkedHashMap<>();

    static {
        SKILL_MAP.put("disintegration", SkillVisualConfig.SKILL_DISINTEGRATION);
        SKILL_MAP.put("jacobs_ladder", SkillVisualConfig.SKILL_JACOBS_LADDER);
        SKILL_MAP.put("purification_pillar", SkillVisualConfig.SKILL_PURIFICATION_PILLAR);
        SKILL_MAP.put("faith_and_grace", SkillVisualConfig.SKILL_FAITH_AND_GRACE);
        SKILL_MAP.put("beelzebuth", SkillVisualConfig.SKILL_BEELZEBUTH);
        SKILL_MAP.put("dragon_nova", SkillVisualConfig.SKILL_DRAGON_NOVA);
        SKILL_MAP.put("extinction_stars", SkillVisualConfig.SKILL_EXTINCTION_STARS);
        SKILL_MAP.put("seer_flesh", SkillVisualConfig.SKILL_SEER_FLESH);
        SKILL_MAP.put("alkaid", SkillVisualConfig.SKILL_ALKAID);
        SKILL_MAP.put("granite_blast", SkillVisualConfig.SKILL_GRANITE_BLAST);
        SKILL_MAP.put("reverse_time_slash", SkillVisualConfig.SKILL_REVERSE_TIME_SLASH);
        SKILL_MAP.put("reverse_velocity_iai", SkillVisualConfig.SKILL_REVERSE_VELOCITY_IAI);
        SKILL_MAP.put("infinite_dragon_prison", SkillVisualConfig.SKILL_INFINITE_DRAGON_PRISON);
        SKILL_MAP.put("multilayer_barrier", SkillVisualConfig.SKILL_MULTILAYER_BARRIER);
        SKILL_MAP.put("universal_sense", SkillVisualConfig.SKILL_UNIVERSAL_SENSE);
        SKILL_MAP.put("uriel_defense", SkillVisualConfig.SKILL_URIEL_DEFENSE);
        SKILL_MAP.put("rykuzin_sinka", SkillVisualConfig.SKILL_RYKUZIN_SINKA);
        SKILL_MAP.put("zone_track", SkillVisualConfig.SKILL_ZONE_TRACK);
        SKILL_MAP.put("nullification", SkillVisualConfig.SKILL_NULLIFICATION);
        SKILL_MAP.put("hellflare", SkillVisualConfig.SKILL_HELLFLARE);
        SKILL_MAP.put("primordial_demon", SkillVisualConfig.SKILL_PRIMORDIAL_DEMON);
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) -> {
            registerCommands(dispatcher);
        });
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("skillvisual")
                .executes(ctx -> showStatus(ctx.getSource()));

        root.then(Commands.literal("status")
                .executes(ctx -> showStatus(ctx.getSource())));

        root.then(Commands.literal("all")
                .then(Commands.literal("cinematic").executes(ctx -> {
                    SkillVisualConfig.setGlobalCinematic(true);
                    for (int id : SKILL_MAP.values()) {
                        SkillVisualConfig.setSkillCinematic(id, true);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§a§l[SKILL VISUAL] §fĐã chuyển TOÀN BỘ kỹ năng sang chế độ §e§lĐIỆN ẢNH 3D (Cinematic 3D)§f!"), true);
                    return 1;
                }))
                .then(Commands.literal("legacy").executes(ctx -> {
                    for (int id : SKILL_MAP.values()) {
                        SkillVisualConfig.setSkillCinematic(id, false);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§6§l[SKILL VISUAL] §fĐã chuyển TOÀN BỘ kỹ năng về chế độ §7§lBẢN CŨ NGUYÊN BẢN (Legacy Classic)§f!"), true);
                    return 1;
                })));

        root.then(Commands.literal("toggle")
                .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String name : SKILL_MAP.keySet()) {
                                if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                                    builder.suggest(name);
                                }
                            }
                            builder.suggest("all");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String skillName = StringArgumentType.getString(ctx, "skill").toLowerCase();
                            if (skillName.equals("all")) {
                                boolean newState = !SkillVisualConfig.isGlobalCinematicEnabled();
                                SkillVisualConfig.setGlobalCinematic(newState);
                                for (int id : SKILL_MAP.values()) {
                                    SkillVisualConfig.setSkillCinematic(id, newState);
                                }
                                String mode = newState ? "§e§lĐIỆN ẢNH 3D" : "§7§lBẢN CŨ NGUYÊN BẢN";
                                ctx.getSource().sendSuccess(() -> Component.literal("§a§l[SKILL VISUAL] §fToàn bộ kỹ năng đã chuyển sang: " + mode), true);
                                return 1;
                            }

                            Integer skillId = SKILL_MAP.get(skillName);
                            if (skillId == null) {
                                ctx.getSource().sendFailure(Component.literal("§cKhông tìm thấy kỹ năng: " + skillName + ". Dùng /skillvisual status để xem danh sách."));
                                return 0;
                            }

                            boolean isNowCinematic = SkillVisualConfig.toggleSkill(skillId);
                            String modeStr = isNowCinematic ? "§e§l⚡ ĐIỆN ẢNH 3D" : "§7§l🏛️ BẢN CŨ NGUYÊN BẢN (Legacy)";
                            ctx.getSource().sendSuccess(() -> Component.literal("§a§l[SKILL VISUAL] §b" + skillName + " §f➔ " + modeStr), true);
                            return 1;
                        })));

        dispatcher.register(root);
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6══════════════ §e§lHỆ THỐNG KỸ XẢO ĐIỆN ẢNH & SAO LƯU §6══════════════"), false);
        source.sendSuccess(() -> Component.literal("§7Trạng thái toàn cục: " + (SkillVisualConfig.isGlobalCinematicEnabled() ? "§aBẬT (Cinematic)" : "§cTẮT (Legacy)")), false);
        source.sendSuccess(() -> Component.literal("§7Sử dụng: §e/skillvisual toggle <tên_kỹ_năng> §7để đổi qua lại giữa bản 3D và bản cũ."), false);
        source.sendSuccess(() -> Component.literal("§8----------------------------------------------------------------"), false);

        for (Map.Entry<String, Integer> entry : SKILL_MAP.entrySet()) {
            boolean active = SkillVisualConfig.isCinematicEnabled(entry.getValue());
            String tag = active ? "§a[⚡ 3D Điện Ảnh]" : "§7[🏛️ Bản Cũ]";
            source.sendSuccess(() -> Component.literal(String.format(" §8• §b%-24s %s", entry.getKey(), tag)), false);
        }

        source.sendSuccess(() -> Component.literal("§6══════════════════════════════════════════════════════════"), false);
        return 1;
    }
}
