package com.minhphuc.weapons.content.soul;

public enum SoulType {
    MILIM(
            "Milim Nava",
            "Long Tinh Bộc Viêm Bá: Dragon Nova (Bản Không Vong 70%)",
            5000.0F,
            30.0F,
            0xFF55DD,
            "§d"
    ),
    VELGRYND(
            "Chước Nhiệt Long Velgrynd",
            "Chước Liệt Hỏa Long Bộc Phá (Bản Không Vong 70%)",
            2000.0F,
            24.0F,
            0xFF4500,
            "§6"
    ),
    VELZARD(
            "Bạch Băng Long Velzard",
            "Bão Băng Thâm Uyên Cthulhu (Bản Không Vong 70%)",
            2500.0F,
            26.0F,
            0x66CCFF,
            "§b"
    ),
    PRIMORDIAL_DEMON(
            "Ác Ma Thủy Tổ",
            "Linh Tử Băng Hoại (Bản Không Vong 70%)",
            1000.0F,
            20.0F,
            0x8800FF,
            "§5"
    ),
    WARDEN(
            "Giám Sát Cổ Đại Warden",
            "Sóng Siêu Âm Hư Vô Tối Thượng (Bản Không Vong 70%)",
            500.0F,
            10.0F,
            0x00AAAA,
            "§3"
    ),
    ENDER_DRAGON(
            "Long Vương The End",
            "Long Tức Hư Vô Hủy Diệt (Bản Không Vong 70%)",
            200.0F,
            8.0F,
            0xAA00AA,
            "§d"
    ),
    WITHER(
            "Chúa Tể Wither",
            "Đầu Lâu Wither Hư Vô Hắc Ám (Bản Không Vong 70%)",
            300.0F,
            6.0F,
            0x222222,
            "§8"
    ),
    MOB(
            "Quái Vật Thường",
            "Linh Hồn Trảo Kích Hư Vô (Bản Không Vong 70%)",
            20.0F,
            2.0F,
            0x55FFFF,
            "§b"
    );

    private final String entityName;
    private final String skillName;
    private final float baseHp;
    private final float baseArmor;
    private final int glowColor;
    private final String colorCode;

    SoulType(String entityName, String skillName, float baseHp, float baseArmor, int glowColor, String colorCode) {
        this.entityName = entityName;
        this.skillName = skillName;
        this.baseHp = baseHp;
        this.baseArmor = baseArmor;
        this.glowColor = glowColor;
        this.colorCode = colorCode;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getSkillName() {
        return skillName;
    }

    public float getBaseHp() {
        return baseHp;
    }

    public float getBaseArmor() {
        return baseArmor;
    }

    public int getGlowColor() {
        return glowColor;
    }

    public String getColorCode() {
        return colorCode;
    }

    public float getBonusHp70() {
        return baseHp * 0.7F;
    }

    public float getBonusArmor70() {
        return baseArmor * 0.7F;
    }
}
