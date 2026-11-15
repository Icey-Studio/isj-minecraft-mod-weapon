package com.minhphuc.weapons.entity.tensura;

public enum ElementalType {
    FIRE("Hỏa Tinh Linh (Ifrit)", "§6", 0xFF5511, 255, 85, 17),
    WIND("Phong Tinh Linh (Sylphide)", "§a", 0x66FF99, 102, 255, 153),
    EARTH("Thổ Tinh Linh (Gnome)", "§e", 0xCC9944, 204, 153, 68),
    WATER("Thủy Tinh Linh (Undine)", "§b", 0x33CCFF, 51, 204, 255),
    LIGHT("Quang Tinh Linh", "§f", 0xFFF2AA, 255, 242, 170),
    DARK("Ám Tinh Linh", "§5", 0x8833CC, 136, 51, 204);

    public final String displayName;
    public final String colorCode;
    public final int hexColor;
    public final int r, g, b;

    ElementalType(String displayName, String colorCode, int hexColor, int r, int g, int b) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.hexColor = hexColor;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static ElementalType fromId(int id) {
        ElementalType[] vals = values();
        return vals[Math.floorMod(id, vals.length)];
    }
}
