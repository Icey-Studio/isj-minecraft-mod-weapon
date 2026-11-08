package com.minhphuc.weapons.entity.darkgathering;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * THỰC THỂ KHÔNG VONG BIẾN THỂ: VÔ NGUYỆT MINH PHÔI (VO NGUYET MINH PHOI)
 * - Biến thể thể cầu thuần túy tối thượng của Không Vong (Kūbō).
 * - Kế thừa 100% toàn bộ cơ chế, chỉ số, AI, hấp thụ linh hồn, và các dạng biến đổi (Normal -> Ultimate -> Complete) của Không Vong gốc.
 * - Hình dạng: Khối cầu vũ trụ đen khổng lồ chứa dải ngân hà phôi thai xoáy sâu bên trong, con mắt nhật thực rực sáng ở tâm,
 *   được bao bọc bởi 16 ăng-ten xúc tu bức xạ tỏa đều ra các hướng.
 * - Đảm bảo tối ưu hóa tối đa, không giật lag.
 */
public class VoNguyetMinhPhoiEntity extends KuboEntity {

    public VoNguyetMinhPhoiEntity(EntityType<? extends KuboEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("§5§lVô Nguyệt Minh Phôi"));
        this.setCustomNameVisible(true);
    }

    @Override
    public void updateCustomDisplayName() {
        this.setCustomName(Component.literal("§5§lVô Nguyệt Minh Phôi"));
        this.setCustomNameVisible(true);
    }
}
