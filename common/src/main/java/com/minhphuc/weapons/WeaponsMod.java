package com.minhphuc.weapons;

import com.minhphuc.weapons.content.tensura.TensuraEvents;
import com.minhphuc.weapons.init.ModItems;
import com.minhphuc.weapons.network.ModMessages;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class WeaponsMod {
    public static final String MOD_ID = "weapons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        // Đăng ký Blocks & Items & Sounds
        com.minhphuc.weapons.init.ModBlocks.register();
        ModItems.register();
        com.minhphuc.weapons.init.ModSoundEvents.register();

        // Đăng ký Entities
        com.minhphuc.weapons.entity.ModEntities.register();

        // Đăng ký Network Packets
        ModMessages.register();

        // Đăng ký các sự kiện Gameplay đa nền tảng
        TensuraEvents.register();
        com.minhphuc.weapons.content.divine.DivineWeaponEvents.register();

        // Đăng ký lệnh quản trị và chuyển đổi kĩ xảo điện ảnh 3D / Bản cũ
        com.minhphuc.weapons.command.SkillVisualCommand.register();

        LOGGER.info("Weapons Mod (Cross-Platform) initialized successfully!");
    }
}
