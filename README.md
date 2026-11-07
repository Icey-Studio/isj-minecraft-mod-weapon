# ⚔️ Weapons Mod — Mythical Armament & Tensura (Minecraft 1.21.1 Fabric)

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg?style=flat-square&logo=minecraft)](https://minecraft.net)
[![Fabric Loader](https://img.shields.io/badge/Loader-Fabric-blue.svg?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-red.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Architectury API](https://img.shields.io/badge/API-Architectury-orange.svg?style=flat-square)](https://modrinth.com/mod/architectury-api)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-181717.svg?style=flat-square&logo=github)](https://github.com/MinhPhuc00ne/isj-minecraft-mod-weapon)

**Weapons Mod** là bản mod đại chiến vũ khí thần thoại, boss sử thi và sức mạnh kỳ ảo tối thượng dành cho **Minecraft 1.21.1 Fabric** (phát triển trên nền tảng **Architectury API** và **Java 21**).

Bản mod hòa quyện thế giới kỳ ảo đồ sộ giữa **Tensura (That Time I Got Reincarnated as a Slime)**, **Dark Gathering (Ám Tập Thần Linh)** và **Hệ Thống Thần Khí Thần Thoại (Divine Armaments)**.

---

## 📚 Hệ Thống Tài Liệu Kỹ Thuật (Architecture & Documentation)

Dự án được xây dựng với tiêu chuẩn kỹ thuật phần mềm cao cấp, tối ưu hóa triệt để hiệu năng (**Zero Lag / 20 TPS**). Xem chi tiết tài liệu kiến trúc tại:

* 🏛️ [**Mô Hình Kiến Trúc Hệ Thống (ARCHITECTURE_MODEL.md)**](docs/ARCHITECTURE_MODEL.md): Kiến trúc phân tầng, luồng mạng C2S/S2C, quản lý vòng đời NBT và Client-Server authority.
* 📋 [**Quy Tắc Nghiệp Vụ & Cơ Chế (BUSINESS_RULES.md)**](docs/BUSINESS_RULES.md): Luật sát thương, cơ chế bất tử Uriel, quy tắc thức tỉnh Ma Vương, danh mục kỹ năng và tính năng.
* 🎨 [**Mẫu Thiết Kế Phần Mềm (DESIGN_PATTERNS.md)**](docs/DESIGN_PATTERNS.md): Dispatcher, Factory, Strategy, Observer, Facade, Accessor Mixin.
* 🧼 [**Quy Chuẩn Mã Nguồn Sạch & Zero Lag (CLEAN_CODE.md)**](docs/CLEAN_CODE.md): Tối ưu hình học hạt không dùng entity, bảo vệ đa luồng, dọn dẹp bộ nhớ.
* 📐 [**Nguyên Tắc Thiết Kế SOLID & KISS (DESIGN_PRINCIPLES.md)**](docs/DESIGN_PRINCIPLES.md): Đơn trách nhiệm, mở rộng linh hoạt, quy chuẩn KISS, DRY, YAGNI.
* 🏷️ [**Quy Ước Đặt Tên Chuẩn Mực (NAMING_CONVENTIONS.md)**](docs/NAMING_CONVENTIONS.md): Chuẩn Package, Class, NBT, ResourceLocation và Network Packets.
* 🤝 [**Hướng Dẫn Đóng Góp (CONTRIBUTING.md)**](CONTRIBUTING.md): Hướng dẫn thiết lập môi trường và quy trình Pull Request.

---

## 🌟 Các Phân Hệ Tính Năng Chính (Core Modules)

### 📜 1. Khởi Nguyên Thần Thư (Celestial Tome — Sách Hướng Dẫn In-Game)
* **Tra cứu trực quan**: Cầm sách và **Click Chuột Phải** để mở giao diện lật trang phong cách thần thoại cổ xưa.
* Hiển thị toàn bộ công thức rèn đúc, bảng phím tắt, bí kíp thức tỉnh Chân Ma Vương và lore chi tiết của từng trang bị.

---

### 👑 2. Con Đường Thức Tỉnh Chân Ma Vương (True Demon Lord Awakening)
* **Viên Ngọc Thức Tỉnh Ma Vương (`Demon Lord Awakening Orb`)**:
  * Tỉ lệ xuất hiện **2.0%** trong rương dungeon tự nhiên hoặc lấy trực tiếp trong Creative Tab (Không thể chế tạo).
  * **[Chuột Phải]**: 1 phát thức tỉnh ngay lập tức thành Chân Ma Vương mà không cần qua Lễ Hội Thu Hoạch hay thời gian ngủ.
  * Tăng vọt máu tối đa vĩnh viễn lên **40 Tim (80.0 HP)**, kích hoạt năng lực bay lượn và mở khóa toàn bộ Kỹ Năng Tối Thượng ở **Cấp 3**.
* **Lễ Hội Thu Hoạch Truyền Thống**: Chế tác Hạt Giống Ma Vương và thu thập 64 Linh Hồn để thức tỉnh.

---

### ⚔️ 3. Nguyệt Quang Thần Tế Kiếm (Moonlight Ritual Sword)
Bảo kiếm hộ vệ thánh điện được tinh gọn thành 2 chiêu thức chiến đấu tối thượng:
1. **Hắc Thiểm Bá Vương Trảm (`Chuột Trái / Cận Chiến`)**: Vết chém rực lửa đen không gian bộc phát sát thương cực hạn.
2. **Linh Tử Băng Hoại (`Chuột Phải`)**: Bắn ra viên cầu ánh sáng 7 màu bay thẳng 50 blocks, gây nổ địa hình và sát thương **30 Tim (60.0 HP)**.

---

### 🛡️ 4. Thần Linh Vũ Trang & Phòng Ngự Tuyệt Đối Của Uriel
* **Trạng thái phòng ngự tuyệt đối**:
  * Mặc định **`[BẬT]` (ON)** khi mặc đủ 4 món Giáp Thần Linh hoặc khi đã là Chân Ma Vương.
  * Miễn nhiễm **100% mọi nguồn sát thương thông thường**.
* **Chủ động chuyển đổi**: Nhấn phím **`[Z]`** để chuyển sang kỹ năng Uriel và nhấp chuột phải để bật/tắt: `[BẬT]` 🛡️ / `[TẮT]` ✕.
* **Cơ chế xuyên thấu**: Các đòn đánh hư vô hoặc linh tử (Linh Tử Băng Hoại, Không Vong) sẽ xuyên thủng giáp và hiển thị cảnh báo:
  > `§c§lBáo cáo: Phòng thủ tuyệt đối của Uriel không thể ngăn chặn tuyệt đối các linh tử.`
* **Kế thừa Ma Vương**: Người chơi đã là Chân Ma Vương vẫn giữ được kỹ năng Uriel ngay cả khi tháo giáp.

---

### ⚡ 5. Hệ Thống Tuyệt Kỹ Bản Thể & Không Hồi Chiêu (Zero Cooldown)
* **Toàn bộ kỹ năng hoàn toàn không mất thời gian hồi chiêu (0s Cooldown)**, thi triển liên tục và mượt mà.
* **Phím `[Z]` luân chuyển 23 tuyệt kỹ**:
  1. *Linh Tử Băng Hoại (Sanctuary Disintegration)*
  2. *Tà Khứ Vũ Thê Tử (Jacob's Ladder)*
  3. *Phép Màu Của Thần Linh (Miracle of the Gods)* — Cải tà quy chính mob thù địch thành dân làng / thú cưng, hồi sinh từ linh hồn
  4. *Bí Ẩn Của Đức Tin Và Ân Sủng (Faith and Grace)* — Cột sáng thánh 40x40m giáng từ trời, đại phục hưng làng hoang phế
  5. *Bạo Thực Vương (Beelzebuth)*
  6. *Long Tinh Bộc Viêm Bá (Dragon Nova)*
  7. *Thần Nộ: Tuyệt Diệt Tinh Tú (Taisui Extinction Stars)*
  8. *Thị Thực (Seer Flesh)*
  9. *Diệt Thế Tà Tinh: Alkaid* (`alkaid_sphere` - Quả cầu tinh tú 14m bốc hơi vạn vật)
  10. *Cú Bắn Granit (Granite Blast / Holy Beam)*
  11. *Nghịch Thời Nhất Trảm (Reverse Time Slash)*
  12. *Bạt Đao Thuật Vận Tốc Ngược (Reverse Velocity Iaijutsu)*
  13. *Sáng Tạo Vật Chất (Material Creation)* (Khi thức tỉnh Noir)
  14. *Kháng Ma Kết Giới (Anti-Magic Barrier)* — Mái vòm bán kính 110m (cao 70m) bảo hộ tuyệt đối, thanh tẩy quái vật
  15. *Kết Giới Đa Tầng (Multilayer Barrier)*
  16. *Cảm Tri Không Gian (Zone Track)*
  17. *Vạn Năng Cảm Tri (Universal Sense - Cấp 3)*
  18. *Long Giam Vô Hạn (Infinite Dragon Prison - Cấp 3)*
  19. *Phòng Ngự Tuyệt Đối Uriel (BẬT / TẮT)*
  20. *Lục Thần Tiến Hóa (Rykuzin Sinka)*
  21. *Hắc Viêm Ngục (Hellflare - Benimaru)* — Giam cầm mục tiêu trong khối lập phương không gian rồi bộc phát lửa đen thiêu cháy
  22. *Hắc Viêm Hỏa Cầu (Dark Flame Shot - Benimaru)* — Bắn cầu lửa đen bất diệt không thể bị dập tắt bởi nước hoặc mưa
  23. *Lời Chúc Tử Vong (Death Blessing)* — Tuyệt kỹ đại hợp nhất 4 hồi điện ảnh, lồng giam 70m, ma pháp trận Long Tinh Bộc Viêm Bá, bay lên cao, instant death và typography tiếng Việt

---

### 🔥 6. Benimaru — Hắc Viêm & Lửa Đen Bất Diệt (Black Flame)
* **Khối Lửa Đen Thật (`black_fire`)**: Mô hình ngọn lửa 3D của Minecraft với texture hoạt họa ngọn lửa đen tuyền ma thuật (`black_fire.png`), thiêu đốt vạn vật.
* **Chiêu 1: Hắc Viêm Ngục (Hellflare)**:
  * *Giai đoạn 1 (0 -> 25 ticks)*: Khối lập phương kính đen trong suốt lập tức khóa chặt mob mục tiêu. Người bên ngoài nhìn vào **thấy rõ mob bị nhốt hoàn toàn bên trong**.
  * *Giai đoạn 2 (26 -> 79 ticks)*: Lửa Đen Thật bùng cháy dữ dội bao trùm toàn bộ khối vuông, chuyển sang đen tuyền che khuất hoàn toàn tầm nhìn, thiêu đốt liên tục (16 DPS).
  * *Giai đoạn 3 (Tick 80)*: Bế mạc nén chân không bộc phá 70 sát thương cực hạn. Miễn trừ 100% cho Long Chủng (Velgrynd, Velzard), Thủy Tổ (Guy, Blanc, Noir), Milim, Marbas; Cứu Thế chịu 30%.
* **Chiêu 2: Hắc Viêm Hỏa Cầu (Dark Flame Shot)**:
  * Phóng hỏa cầu hắc ám tầm xa 32m, phát nổ diện rộng 35 sát thương và đặt khối Hắc Viêm trên mặt đất.
  * Gieo rắc Hắc Viêm Bất Diệt trong 8 giây: ngâm mình xuống nước hay trời mưa cũng không thể dập tắt!

---

### 🌌 7. Tuyệt Kỹ Đại Hợp Nhất: Lời Chúc Tử Vong (Death Blessing — Authentic Cinematic & Shaders Engine)
Kỹ năng thứ 23 kết nối toàn bộ tinh hoa tuyệt kỹ của mod thành một bản giao hưởng hủy diệt 4 hồi liên hoàn (10 giây), được nâng cấp toàn diện với **Engine Đồ Họa & Âm Thanh Chuẩn God Wand**:
* **Bộ Âm Thanh Gốc Siêu Thực**:
  * `atomic_charge.ogg`: Lời thì thầm ma mị bí ẩn cùng tiếng gầm trầm hum bass tăng dần theo nhịp tích tụ năng lượng.
  * `atomic_blast.ogg`: Tiếng nổ hủy diệt cực nặng kèm chấn động sóng hạ âm làm rung chuyển không gian.
* **Ma Pháp Trận 3D Phát Quang & Bay Lên Cao**:
  * Người chơi bay bổng lên cao (+28m) lơ lửng trên không trung nhìn xuống toàn cảnh chiến trường.
  * Ma Pháp Trận Long Tinh Bộc Viêm Bá (`BEELZEBUTH_MAGIC_CIRCLE`) xoay 3D phát quang tím thẫm bành trướng từ 3.5m lên 40m phủ kín mặt đất.
* **Đồ Họa Năng Lượng OpenGL Additive Blending (`AtomicRenderer`)**:
  * Loại bỏ hoàn toàn khối display thô ráp; thay bằng hình học năng lượng trực tiếp GPU: Vành hào quang đồng tâm, mạch năng lượng phân nhánh, motes năng lượng hội tụ, pháo đài plasma và vết nứt nổ đất phát sáng lan tỏa 98 blocks.
  * Cột plasma kép (Lõi trắng `0xF5DFFF` + Vành tím `0x932BFF`) vươn cao 1024m với 40 vết sét đa giác nứt rách không gian.
* **Chuỗi Shaders Post-Processing GLSL (`AtomicPostFx`)**:
  * Tích hợp pipeline shader chân thực: Bloom tỏa sáng đa tầng, sắc sai tán sắc (Chromatic Aberration), Darkening triệt tiêu bầu trời và hội tụ ánh sáng thể tích (Volumetric Light Beam).
* **4 Khung Hình Va Chạm Manga / Anime (`CustomImpactFrameOverlay`)**:
  * 4 Impact Frames đen-trắng âm bản xuất hiện chớp nhoáng tại thời khắc kích nổ (Tick 141), nối tiếp bởi flash trắng dịu êm bộc phát thành cột sáng.
* **Widescreen Cinematic Letterbox (21:9) & Typography Tiếng Việt Phát Sáng**:
  * Dải đen điện ảnh tự động mở rộng, làm mờ viền màn hình, hiển thị Typography phát quang tiếng Việt: **`LỜI CHÚC...`** -> phóng to **`TỬ VONG`** rực lửa ma thuật đồng bộ với góc máy cận cảnh.
* **Phán Quyết Tuyệt Đối (Instant Death)**:
  * Tại Tick 141, xóa sạch mọi hiệu ứng phòng ngự, one-hit tiêu diệt lập tức mọi sinh vật trong 70m, bốc hơi rớt Linh Hồn (`Mob Soul`).
* **Choreography 5 Góc Máy Điện Ảnh**:
  * *Shot 1 (0 -> 25 ticks)*: Orbit lướt vòng cung quanh người chơi từ dưới nâng lên.
  * *Shot 2 (25 -> 50 ticks)*: Dolly-Out lùi xa bay lên cao (21m) bao quát người chơi và ma pháp trận bành trướng dưới chân.
  * *Shot 3 (50 -> 110 ticks)*: Bird's Eye View từ trên tầng mây nhìn toàn cảnh lực hút nghịch chuyển hút mob bay lơ lửng.
  * *Shot 4 (110 -> 141 ticks)*: Zoom cận cảnh kịch tính vào khuôn mặt / vũ khí người chơi.
  * *Shot 5 (141 -> 180 ticks)*: Cực đại góc rộng từ xa kèm Camera Shake rung chấn cực mạnh khi cột năng lượng dội xuống.
  * *180 -> 200 ticks & Kết Thúc*: Giữ nguyên vị thế người chơi trên không trung, chuyển tiếp camera mượt mà về tầm nhìn trên cao bao quát toàn bộ bình địa bị san phẳng.
  * Hỗ trợ phím tắt **`[ESC]`** hoặc **`[F5]`** để bỏ qua cutscene bất cứ lúc nào!

---

### 👁️ 8. Dark Gathering — Thần Linh, Boss Tốt Nghiệp Sinh & Tà Tăng
* **Đại Ác Ma Marbas (Goetia President Marbas - Chap 52)**:
  * Boss quỷ trắng khổng lồ cao 4.2m với sừng cừu và đôi cánh trắng, máu 1500 HP, giáp 22, kháng bật lùi 1.0.
  * **Lãnh Địa Bàn Mổ (Surgical Theater)**: Bóp nát tim kẻ nào sprint/nhảy (sát thương chuẩn), Sóng Dao Mổ tước đoạt não bộ gây mù và suy yếu, Tái Cấu Trúc Sinh Thể hồi máu.
  * Rơi trọn bộ nội tạng thần thoại: Tim, Não, Cánh Tay, Mắt.
* **Tà Kinh Đại Tăng (Jankyōbun Daisōjō)**:
  * Có thể phong ấn vào **Hình Nhân Gỗ Bồ Đề (`bodhi_effigy_monk`)** để triệu hồi làm thức thần hộ mệnh trong 60 giây. Bắt buộc người chơi phải đeo **Bông Bịt Tai Phong Ấn (`warding_earplugs`)**!
* **Bạch Nhật Tà Thần — Không Vong (Kūbō)**: Đại boss 3 giai đoạn, hố đen trọng lực, nuốt chửng linh hồn.
* **Thái Tuế Tinh Quân**: Tinh Tú Tuyệt Diệt, Thị Thực hồi sinh, Diệt Thế Tà Tinh Alkaid (`alkaid_sphere`) 14m xuyên thủng địa hình.
* **Thiên Ngoại Cứu Thế — Nghịch Thời Nhất Trảm (`Phím X`)**: Nhát chém đảo ngược thời gian.

---

### 🐉 8. Tensura — Thất Đại Thủy Tổ & Chân Long Chủng
* **Guy Crimson (Rouge) & Tổ Đội Hỗ Trợ**: Khi máu dưới 80%, Rouge triệu hồi **Rain (Bleu)** và **Misery (Vert)** ra hỗ trợ chiến đấu; Misery giảm 40% sát thương cho Rouge. Quái triệu hồi không rơi khế ước ác ma chống exploit.
* **7 Thủy Tổ Ác Ma**: *Noir, Rouge, Blanc, Jaune, Violet, Bleu, Vert* với Pháp Trận, Bồn Nuôi Cấy Thể Xác và Lãnh Địa riêng biệt.
* **Súng Lục Hoàng Kim Carrera (Golden Gun)**: Bắn 4 loại ma đạn tối thượng (*Judgement, Abyss Core, Gravity, Golden Rapid*).
* **Đại Boss Chân Long**: *Chước Nhiệt Long Velgrynd*, *Bạch Băng Long Velzard*, *Ma Vương Milim Nava* với hiệu ứng telegraph và chiêu thức bão lửa/băng tuyết cực hạn.

---

## 🎮 Bảng Phím Tắt & Thao Tác (Keybindings)

| Phím Tắt | Thao Tác | Chức Năng |
| :---: | :--- | :--- |
| **`Z`** | **Xoay Vòng Tuyệt Kỹ** | Chuyển đổi giữa 22 kỹ năng bản thể & bật/tắt Uriel |
| **`Chuột Phải`** | **Kích Hoạt Kỹ Năng** | Thi triển chiêu thức đã chọn / Bắn Cầu Linh Tử 7 màu (Kiếm) / Mở sách |
| **`Chuột Trái`** | **Tấn Công / Hắc Thiểm** | Trảm Kích Hắc Thiểm Bá Vương khi cầm Nguyệt Quang Kiếm |
| **`Page Up`** | **Chọn Ma Đạn Hoàng Kim** | Mở bảng GUI chọn 4 loại ma đạn khi cầm Súng Hoàng Kim |
| **`X`** | **Nghịch Thời Nhất Trảm** | Kích hoạt nhát chém đảo ngược thời gian |

---

## 👨‍💻 Tác Giả & Bản Quyền

* **Tác Giả Sáng Lập**: **MinhPhuc00ne / Jos**
* **Đơn Vị Phát Triển**: **Icey Studio**
* **Trường / Học Viện**: **FPT University**
* **Mã Nguồn**: [GitHub Repository](https://github.com/MinhPhuc00ne/isj-minecraft-mod-weapon)
* **Bản Quyền**: [MIT License](LICENSE)
