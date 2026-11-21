package com.minhphuc.weapons.entity.tensura;

import com.minhphuc.weapons.init.ModBlocks;
import com.minhphuc.weapons.init.ModItems;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum DemonType {
    ROUGE(
            "Rouge",
            "Hồng Sắc Thủy Tổ",
            "The Primordial Red",
            0xFF1E1E,
            0xD41515,
            "Hỏa Ngục Thủy Tổ",
            "Primordial Hellfire",
            "Hmph... Ta đã đáp lại lời triệu hồi của ngươi.",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/rouge.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_rouge.png"),
            1.16F,
            10800.0D,
            480.0D,
            0.90F
    ),
    NOIR(
            "Noir",
            "Hắc Sắc Thủy Tổ",
            "The Primordial Black",
            0x222226,
            0x111116,
            "Móng Vuốt Tuyệt Vọng",
            "Claws of Despair",
            "Kufufufu... Tôi đã chờ đợi giây phút được phụng sự ngài từ lâu lắm rồi!",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/noir.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_noir.png"),
            1.08F,
            10800.0D,
            480.0D,
            0.90F
    ),
    BLANC(
            "Blanc",
            "Bạch Sắc Thủy Tổ",
            "The Primordial White",
            0xF0F4F8,
            0xE2E8F0,
            "Bạch Viêm Diệt Tuyệt",
            "White Flare",
            "Ara ara... Ngươi là người đã đánh thức ta sao? Thú vị đấy.",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/blanc.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_blanc.png"),
            1.02F,
            5400.0D,
            240.0D,
            0.75F
    ),
    JAUNE(
            "Jaune",
            "Hoàng Sắc Thủy Tổ",
            "The Primordial Yellow",
            0xFACC15,
            0xEAB308,
            "Trọng Lực Tuyệt Diệt",
            "Gravity Annihilation",
            "Haha! Kẻ nào dám triệu hồi ta đó? Hãy cho ta một trận chiến ra trò nào!",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/jaune.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_jaune.png"),
            1.00F,
            4500.0D,
            210.0D,
            0.65F
    ),
    VIOLET(
            "Violet",
            "Tử Sắc Thủy Tổ",
            "The Primordial Purple",
            0xA855F7,
            0x9333EA,
            "Tử Độc Khởi Nguyên",
            "Primordial Toxic Nova",
            "Hihihi, gọi ta ra đây có gì vui không nào, chủ nhân?",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/violet.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_violet.png"),
            0.88F,
            2850.0D,
            120.0D,
            0.33F
    ),
    BLEU(
            "Bleu",
            "Lam Sắc Thủy Tổ",
            "The Primordial Blue",
            0x3B82F6,
            0x2563EB,
            "Băng Cực Ma Trận",
            "Absolute Zero Spikes",
            "Tôi đã tới theo lời giao ước... Ngài cần tôi giải quyết kẻ nào?",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/bleu.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_bleu.png"),
            0.96F,
            2400.0D,
            90.0D,
            0.20F
    ),
    VERT(
            "Vert",
            "Lục Sắc Thủy Tổ",
            "The Primordial Green",
            0x22C55E,
            0x16A34A,
            "Bão Tố Lục Bảo & Kết Giới",
            "Emerald Tempest & Aegis",
            "Mọi sự hỗn loạn sẽ được vãn hồi. Tôi xin kính cẩn tuân lệnh ngài.",
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/vert.png"),
            ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/eyes_vert.png"),
            1.00F,
            2700.0D,
            108.0D,
            0.30F
    );

    private final String colorName;
    private final String titleVi;
    private final String titleEn;
    private final int glowColor;
    private final int primaryColor;
    private final String skillNameVi;
    private final String skillNameEn;
    private final String summonDialogue;
    private final ResourceLocation textureLocation;
    private final ResourceLocation eyesTextureLocation;
    private final float scale;
    private final double maxHealth;
    private final double attackDamage;
    private final float bossWinRate;

    DemonType(String colorName, String titleVi, String titleEn, int glowColor, int primaryColor,
              String skillNameVi, String skillNameEn, String summonDialogue,
              ResourceLocation textureLocation, ResourceLocation eyesTextureLocation, float scale,
              double maxHealth, double attackDamage, float bossWinRate) {
        this.colorName = colorName;
        this.titleVi = titleVi;
        this.titleEn = titleEn;
        this.glowColor = glowColor;
        this.primaryColor = primaryColor;
        this.skillNameVi = skillNameVi;
        this.skillNameEn = skillNameEn;
        this.summonDialogue = summonDialogue;
        this.textureLocation = textureLocation;
        this.eyesTextureLocation = eyesTextureLocation;
        this.scale = scale;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.bossWinRate = bossWinRate;
    }

    public String getColorName() {
        return colorName;
    }

    public String getTitleVi() {
        return titleVi;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public int getGlowColor() {
        return glowColor;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public String getSkillNameVi() {
        return skillNameVi;
    }

    public String getSkillNameEn() {
        return skillNameEn;
    }

    public String getSummonDialogue() {
        return summonDialogue;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    public ResourceLocation getBodyTextureLocation() {
        return ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/" + name().toLowerCase() + "_body.png");
    }

    public ResourceLocation getNamedTextureLocation() {
        return ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/" + name().toLowerCase() + "_named.png");
    }

    public ResourceLocation getAwakenedTextureLocation() {
        return ResourceLocation.fromNamespaceAndPath("weapons", "textures/entity/demon/" + name().toLowerCase() + "_awakened.png");
    }

    public ResourceLocation getEvolvedTextureLocation() {
        return getAwakenedTextureLocation();
    }

    public String getRandomCanonName(net.minecraft.util.RandomSource random) {
        String[] pool = switch (this) {
            case ROUGE -> new String[]{"Guy Crimson", "Crimson Lord", "Bạo Vương Guy", "Chúa Tể Xích Sắc"};
            case NOIR -> new String[]{"Diablo", "Kuro", "Hắc Thần Diablo", "Quản Gia Hắc Sắc"};
            case BLANC -> new String[]{"Testarossa", "Shiro", "Bạch Cơ Testarossa", "Thánh Nữ Bạch Viêm"};
            case JAUNE -> new String[]{"Carrera", "Kiiro", "Đại Tướng Carrera", "Nữ Hoàng Ma Pháp"};
            case VIOLET -> new String[]{"Ultima", "Murasaki", "Tử Độc Ultima", "Tiểu Thư Mạn Đà La"};
            case BLEU -> new String[]{"Rain", "Ao", "Hầu Gái Rain", "Băng Cực Rain"};
            case VERT -> new String[]{"Misery", "Midori", "Trưởng Hầu Misery", "Phong Bão Misery"};
        };
        return pool[random.nextInt(pool.length)];
    }

    public ResourceLocation getEyesTextureLocation() {
        return eyesTextureLocation;
    }

    public float getScale() {
        return scale;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public float getBossWinRate() {
        return bossWinRate;
    }

    public RegistrySupplier<Item> getMagicCircleItem() {
        return switch (this) {
            case NOIR -> ModItems.MAGIC_CIRCLE_NOIR;
            case ROUGE -> ModItems.MAGIC_CIRCLE_ROUGE;
            case BLANC -> ModItems.MAGIC_CIRCLE_BLANC;
            case JAUNE -> ModItems.MAGIC_CIRCLE_JAUNE;
            case VIOLET -> ModItems.MAGIC_CIRCLE_VIOLET;
            case BLEU -> ModItems.MAGIC_CIRCLE_BLEU;
            case VERT -> ModItems.MAGIC_CIRCLE_VERT;
        };
    }

    public boolean isMale() {
        return this == ROUGE || this == NOIR;
    }

    public boolean isFemale() {
        return !isMale();
    }

    public RegistrySupplier<Item> getPactItem() {
        return switch (this) {
            case NOIR -> ModItems.PRIMORDIAL_PACT_NOIR;
            case ROUGE -> ModItems.PRIMORDIAL_PACT_ROUGE;
            case BLANC -> ModItems.PRIMORDIAL_PACT_BLANC;
            case JAUNE -> ModItems.PRIMORDIAL_PACT_JAUNE;
            case VIOLET -> ModItems.PRIMORDIAL_PACT_VIOLET;
            case BLEU -> ModItems.PRIMORDIAL_PACT_BLEU;
            case VERT -> ModItems.PRIMORDIAL_PACT_VERT;
        };
    }

    public RegistrySupplier<Item> getBodyPactItem() {
        return switch (this) {
            case NOIR -> ModItems.PRIMORDIAL_PACT_NOIR_BODY;
            case ROUGE -> ModItems.PRIMORDIAL_PACT_ROUGE_BODY;
            case BLANC -> ModItems.PRIMORDIAL_PACT_BLANC_BODY;
            case JAUNE -> ModItems.PRIMORDIAL_PACT_JAUNE_BODY;
            case VIOLET -> ModItems.PRIMORDIAL_PACT_VIOLET_BODY;
            case BLEU -> ModItems.PRIMORDIAL_PACT_BLEU_BODY;
            case VERT -> ModItems.PRIMORDIAL_PACT_VERT_BODY;
        };
    }

    public RegistrySupplier<Item> getNamedPactItem() {
        return switch (this) {
            case NOIR -> ModItems.PRIMORDIAL_PACT_NOIR_NAMED;
            case ROUGE -> ModItems.PRIMORDIAL_PACT_ROUGE_NAMED;
            case BLANC -> ModItems.PRIMORDIAL_PACT_BLANC_NAMED;
            case JAUNE -> ModItems.PRIMORDIAL_PACT_JAUNE_NAMED;
            case VIOLET -> ModItems.PRIMORDIAL_PACT_VIOLET_NAMED;
            case BLEU -> ModItems.PRIMORDIAL_PACT_BLEU_NAMED;
            case VERT -> ModItems.PRIMORDIAL_PACT_VERT_NAMED;
        };
    }

    public RegistrySupplier<Item> getAwakenedPactItem() {
        return switch (this) {
            case NOIR -> ModItems.PRIMORDIAL_PACT_NOIR_AWAKENED;
            case ROUGE -> ModItems.PRIMORDIAL_PACT_ROUGE_AWAKENED;
            case BLANC -> ModItems.PRIMORDIAL_PACT_BLANC_AWAKENED;
            case JAUNE -> ModItems.PRIMORDIAL_PACT_JAUNE_AWAKENED;
            case VIOLET -> ModItems.PRIMORDIAL_PACT_VIOLET_AWAKENED;
            case BLEU -> ModItems.PRIMORDIAL_PACT_BLEU_AWAKENED;
            case VERT -> ModItems.PRIMORDIAL_PACT_VERT_AWAKENED;
        };
    }

    public RegistrySupplier<Item> getPactItemForTier(int tier) {
        return switch (tier) {
            case 1 -> getBodyPactItem();
            case 2 -> getNamedPactItem();
            case 3 -> getAwakenedPactItem();
            default -> getPactItem();
        };
    }

    public RegistrySupplier<Block> getDomainBarrierBlock() {
        return switch (this) {
            case NOIR -> ModBlocks.DOMAIN_BARRIER_NOIR;
            case ROUGE -> ModBlocks.DOMAIN_BARRIER_ROUGE;
            case BLANC -> ModBlocks.DOMAIN_BARRIER_BLANC;
            case JAUNE -> ModBlocks.DOMAIN_BARRIER_JAUNE;
            case VIOLET -> ModBlocks.DOMAIN_BARRIER_VIOLET;
            case BLEU -> ModBlocks.DOMAIN_BARRIER_BLEU;
            case VERT -> ModBlocks.DOMAIN_BARRIER_VERT;
        };
    }

    public enum SacrificeTier {
        EASY("§aDễ", 1),
        MEDIUM("§eTrung Bình", 2),
        HARD("§6Khó", 3),
        HARDEST("§cTối Thượng (Khó Nhất)", 4);

        private final String displayName;
        private final int level;

        SacrificeTier(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }
    }

    public SacrificeTier getSacrificeTier() {
        return switch (this) {
            case BLEU, VERT -> SacrificeTier.EASY;
            case VIOLET -> SacrificeTier.MEDIUM;
            case JAUNE, BLANC -> SacrificeTier.HARD;
            case NOIR, ROUGE -> SacrificeTier.HARDEST;
        };
    }

    public String[] getPlayerKillDialogues() {
        return switch (this) {
            case ROUGE -> new String[]{
                    "Hmph... Quá nhàm chán. Ngươi chỉ chịu được bấy nhiêu thôi sao?",
                    "Biến thành tro tàn trước Xích Sắc Thủy Tổ là vinh dự lớn nhất đời ngươi rồi.",
                    "Linh hồn yếu ớt... Thậm chí còn chẳng xứng làm trò tiêu khiển của ta."
            };
            case NOIR -> new String[]{
                    "Kufufufu... Linh hồn bất kính này sẽ là món tráng miệng tuyệt vời!",
                    "Dám ngáng đường chủ nhân của tôi? Cái chết là ân huệ nhẹ nhàng nhất dành cho ngươi rồi!",
                    "Kufufufu... Thật tuyệt vọng làm sao! Sự vùng vẫy vô nghĩa của ngươi khiến tôi vô cùng thích thú!"
            };
            case BLANC -> new String[]{
                    "Ara ara... Kết thúc sớm vậy sao? Ta còn chưa kịp cảm thấy hứng thú nữa mà~",
                    "Ngươi đã bị ngọn lửa trắng thanh tẩy hoàn toàn. Thật là một cái chết thanh nhã.",
                    "Sự tồn tại của ngươi quá nhỏ bé trước sức mạnh của Bạch Sắc Thủy Tổ."
            };
            case JAUNE -> new String[]{
                    "Hahaha! Thấy uy lực của ma pháp hạt nhân chưa? Ngươi bốc hơi không còn một mẩu xương rồi!",
                    "Chán chết đi được! Ta mới bắn thử vài phát súng ma đạn mà đã ngã gục rồi à?",
                    "Haha! Đứng trước Hoàng Sắc Thủy Tổ mà không biết tự lượng sức mình!"
            };
            case VIOLET -> new String[]{
                    "Hihihi~ Độc của em ngấm vào tận tủy sống rồi đúng không? Chết thật là đau đớn nha~",
                    "Ai bảo dám chọc giận Tử Sắc Thủy Tổ cơ chứ? Hihihi, đáng đời ngươi lắm!",
                    "Chủ nhân ơi nhìn xem~ Em vừa biến kẻ này thành một đóa hoa độc tím ngắt rồi nè!"
            };
            case BLEU -> new String[]{
                    "Đóng băng và tan biến trong im lặng đi. Đừng làm phiền ta nữa.",
                    "Cực hàn đã phong ấn toàn bộ sự sống của ngươi. Kết thúc rồi.",
                    "Thật phiền toái... Lại phải dọn dẹp thêm một kẻ vô dụng."
            };
            case VERT -> new String[]{
                    "Trật tự đã được vãn hồi. Kẻ gây rối đã bị trừng phạt thích đáng.",
                    "Dưới cơn bão lục bảo, mọi sự bất kính đều phải trả giá bằng mạng sống.",
                    "Tôi xin dâng chiến thắng này để bảo hộ sự bình an cho chủ nhân."
            };
        };
    }

    public String[] getMobKillDialogues() {
        return switch (this) {
            case ROUGE -> new String[]{
                    "Rác rưởi ngáng đường, biến mất đi.",
                    "Lửa của ta đã thiêu rụi toàn bộ sinh mệnh của ngươi.",
                    "Thứ sinh vật hạ đẳng này cũng dám bén mảng tới gần ta?"
            };
            case NOIR -> new String[]{
                    "Kufufufu... Biến thành cát bụi trong bóng tối của tôi đi.",
                    "Chỉ là lũ sâu bọ không đáng bận tâm trước mắt chủ nhân.",
                    "Móng vuốt của tôi đã xé toạc linh hồn ngươi rồi, Kufufufu!"
            };
            case BLANC -> new String[]{
                    "Ara ara... Thật bẩn thỉu. Hãy biến mất không còn một hạt bụi nào đi.",
                    "Một đòn phân rã nhẹ nhàng đã giải quyết xong rồi sao?",
                    "Ngoan ngoãn tan biến trong ánh sáng trắng của ta nhé~"
            };
            case JAUNE -> new String[]{
                    "BÙM! Nổ tung đẹp mắt lắm! Tiếp theo là đứa nào nữa?",
                    "Haha! Bị nghiền nát dưới trọng lực tuyệt đối thì cảm giác thế nào?",
                    "Quá yếu ớt! Thậm chí chẳng chịu nổi một đợt đạn Carrera của ta!"
            };
            case VIOLET -> new String[]{
                    "Hihihi~ Tan rữa ra đi nào, tan rữa từ trong ra ngoài!",
                    "Thêm một con mồi đáng thương nếm trải kịch độc của ta rồi!",
                    "Chết lẹ vậy ta? Còn chưa kịp chơi đùa thỏa thích mà!"
            };
            case BLEU -> new String[]{
                    "Một khối băng vỡ vụn... Thật nhàm chán.",
                    "Cực hạn băng giá không để lại bất kỳ tàn tích nào.",
                    "Yên nghỉ trong lạnh giá đi."
            };
            case VERT -> new String[]{
                    "Sự hỗn loạn đã bị thanh trừng.",
                    "Bão tố đã quét sạch mọi mầm mống đe dọa.",
                    "Kính cẩn kết liễu theo trật tự tự nhiên."
            };
        };
    }

    public String getRivalryDialogue(DemonType rival) {
        return switch (this) {
            case ROUGE -> "Một Thủy Tổ như " + rival.getColorName() + " mà lại chịu cúi đầu trước loài người sao? Để ta thanh trừng ngươi trước!";
            case NOIR -> "Kufufufu... " + rival.getColorName() + ", ngươi dám ngáng đường chủ nhân vĩ đại của ta sao? Hãy để móng vuốt của tôi tiễn ngươi về Ma Giới!";
            case BLANC -> "Ara ara, " + rival.getColorName() + " thân mến, ngươi chọn nhầm chiến tuyến rồi đấy. Để ta thanh tẩy ngươi nhé~";
            case JAUNE -> "Haha! " + rival.getColorName() + " đó hả? Nhào vô đây! Xem đại ma pháp hạt nhân của ta nghiền nát ngươi!";
            case VIOLET -> "Hihihi~ " + rival.getColorName() + " kìa! Để xem độc của em có ăn mòn được cả Thủy Tổ đồng tộc không nào!";
            case BLEU -> rival.getColorName() + "... Ngươi thật phiền toái. Ta sẽ đóng băng ngươi lại cho trật tự.";
            case VERT -> "Hành vi đối đầu của " + rival.getColorName() + " đã vi phạm quy tắc. Tôi buộc phải can thiệp để vãn hồi trật tự.";
        };
    }

    public static DemonType byIndex(int index) {
        DemonType[] vals = values();
        if (index < 0 || index >= vals.length) return NOIR;
        return vals[index];
    }
}
