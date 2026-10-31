package com.minhphuc.weapons.client;

/**
 * Quản lý trạng thái client-side của Kỹ Năng Tiên Tri (Rykuzin Sinka):
 * - Được đồng bộ tức thì từ server khi bật/tắt (Zero Lag, không tốn tick loop).
 * - Xác định xem client có nhìn thấy được Linh Hồn của Cứu Thế Quyền Hiện hay không.
 */
public class ClientRykuzinHandler {
    private static boolean rykuzinActive = false;

    public static boolean isActive() {
        return rykuzinActive;
    }

    public static void setActive(boolean active) {
        rykuzinActive = active;
    }
}
