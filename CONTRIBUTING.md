# 🤝 HƯỚNG DẪN ĐÓNG GÓP PHÁT TRIỂN (CONTRIBUTING GUIDELINES)
## Bản Mod: `Weapons Mod — Mythical Armament & Tensura`

Cảm ơn bạn đã quan tâm và muốn đóng góp phát triển cho **Weapons Mod**! Dự án luôn chào đón các ý tưởng sáng tạo, tối ưu hóa hiệu năng, thêm boss sử thi và kỹ năng Anime đỉnh cao.

---

## 🛠️ Yêu Cầu Môi Trường (Prerequisites)

* **JDK**: Java Development Kit 21 (OpenJDK / Eclipse Temurin 21 khuyến nghị).
* **IDE**: IntelliJ IDEA (khuyến nghị có cài đặt Minecraft Development plugin) hoặc VS Code.
* **Build System**: Gradle 8.8 (sử dụng `./gradlew` tích hợp sẵn trong repo).
* **Nền tảng**: Architectury Loom (Fabric 1.21.1).

---

## 🚀 Quy Trình Đóng Góp (Contribution Workflow)

1. **Fork Repository**:
   Nhấn nút **Fork** ở góc trên cùng bên phải giao diện GitHub.
2. **Clone mã nguồn về máy**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/isj-minecraft-mod-weapon.git
   cd isj-minecraft-mod-weapon
   ```
3. **Tạo nhánh phát triển mới**:
   ```bash
   git checkout -b feat/ten-tinh-nang-moi
   ```
4. **Quy chuẩn lập trình bắt buộc**:
   * Tuân thủ các nguyên tắc thiết kế và kiến trúc được ghi chép chi tiết trong thư mục `docs/`:
     * [Mô Hình Kiến Trúc](docs/ARCHITECTURE_MODEL.md)
     * [Quy Tắc Nghiệp Vụ](docs/BUSINESS_RULES.md)
     * [Mẫu Thiết Kế](docs/DESIGN_PATTERNS.md)
     * [Quy Chuẩn Clean Code & Zero Lag](docs/CLEAN_CODE.md)
     * [Nguyên Tắc Thiết Kế](docs/DESIGN_PRINCIPLES.md)
     * [Quy Ước Đặt Tên](docs/NAMING_CONVENTIONS.md)
   * Không spawn Entity bừa bãi khi tạo hiệu ứng kỹ năng; sử dụng tính toán hình học hạt trực tiếp.
5. **Kiểm tra biên dịch & Build**:
   Trước khi commit, bắt buộc kiểm tra xem dự án có biên dịch 100% không có lỗi:
   ```bash
   ./gradlew compileJava
   ```
6. **Commit & Push**:
   Sử dụng [Conventional Commits](https://www.conventionalcommits.org/):
   * `feat: ...` (Tính năng mới)
   * `fix: ...` (Sửa lỗi)
   * `perf: ...` (Tối ưu hiệu năng, giảm lag)
   * `docs: ...` (Tài liệu)
   ```bash
   git add .
   git commit -m "feat: implement new tensura awakening skill"
   git push origin feat/ten-tinh-nang-moi
   ```
7. **Tạo Pull Request (PR)**:
   Mở PR trên GitHub về nhánh chính của repository gốc, kèm mô tả chi tiết tính năng và hình ảnh/video minh họa nếu có.

---

## 🐞 Báo Lỗi & Góp Ý Ý Tưởng

Mọi vấn đề hoặc ý tưởng nâng cấp xin vui lòng mở Issue tại:
👉 [GitHub Issues](https://github.com/MinhPhuc00ne/isj-minecraft-mod-weapon/issues)
