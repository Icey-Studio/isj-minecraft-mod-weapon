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
* **Phím `[Z]` luân chuyển 18 tuyệt kỹ**:
  1. *Linh Tử Băng Hoại*
  2. *Tà Khứ Vũ Thê Tử (Jacob's Ladder)*
  3. *Phép Màu Của Thần Linh*
  4. *Bí Ẩn Của Đức Tin Và Ân Sủng* (Cột sáng khổng lồ từ trời giáng xuống 5s, bán kính 40x40 blocks)
  5. *Bạo thực vương (Beelzebuth)*
  6. *Long Tinh Bộc Viêm Bá (Dragon Nova)*
  7. *Thần Nộ: Tuyệt Diệt Tinh Tú*
  8. *Thị Thực (Seer Flesh)*
  9. *Diệt Thế Tà Tinh: Alkaid*
  10. *Cú Bắn Granit (Granite Blast)*
  11. *Nghịch Thời Nhất Trảm*
  12. *Sáng Tạo Vật Chất* (Hạ Noir)
  13. *Kháng Ma Kết Giới*
  14. *Đa Trùng Kết Giới*
  15. *Zone Track*
  16. *Cảm Nhận Vạn Năng (Cấp 3)*
  17. *Lồng Giam Vô Hạn (Cấp 3)*
  18. *Phòng Ngự Tuyệt Đối Của Uriel (BẬT / TẮT)*

---

### 👁️ 6. Dark Gathering — Không Vong & Thái Tuế Tinh Quân
* **Bạch Nhật Tà Thần — Không Vong (Kūbō)**: Đại boss 3 giai đoạn, nuốt chửng linh hồn tăng sức mạnh, AI né tránh đòn linh tử.
* **Thái Tuế Tinh Quân**: Tinh Tú Tuyệt Diệt, Thị Thực hồi sinh, Diệt Thế Tà Tinh Alkaid mũi khoan vũ trụ 14m xuyên thủng địa hình.
* **Thiên Ngoại Cứu Thế — Nghịch Thời Nhất Trảm (`Phím X`)**: Nhát chém đảo ngược thời gian.

---

### 🐉 7. Tensura — Thất Đại Thủy Tổ & Chân Long Chủng
* **7 Thủy Tổ Ác Ma**: *Noir, Rouge, Blanc, Jaune, Violet, Bleu, Vert* với Pháp Trận, Bồn Nuôi Cấy Thể Xác và Lãnh Địa riêng biệt.
* **Súng Lục Hoàng Kim Carrera (Golden Gun)**: Bắn 4 loại ma đạn tối thượng (*Judgement, Abyss Core, Gravity, Golden Rapid*).
* **Đại Boss Chân Long**: *Chước Nhiệt Long Velgrynd*, *Bạch Băng Long Velzard*, *Ma Vương Milim Nava* với hiệu ứng telegraph và chiêu thức bão lửa/băng tuyết cực hạn.

---

## 🎮 Bảng Phím Tắt & Thao Tác (Keybindings)

| Phím Tắt | Thao Tác | Chức Năng |
| :---: | :--- | :--- |
| **`Z`** | **Xoay Vòng Tuyệt Kỹ** | Chuyển đổi giữa 18 kỹ năng bản thể & bật/tắt Uriel |
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
