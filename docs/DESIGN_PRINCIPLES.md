# 📐 NGUYÊN TẮC THIẾT KẾ PHẦN MỀM (DESIGN PRINCIPLES)

## Bản Mod: `Weapons Mod — Mythical Armament & Tensura`

Bộ nguyên tắc thiết kế đóng vai trò kim chỉ nam trong suốt quá trình phát triển mã nguồn, giúp hệ thống luôn linh hoạt, nhất quán và bền vững trước các đợt cập nhật phiên bản Minecraft.

---

## 1. Bộ Nguyên Tắc SOLID

### 1.1. S — Single Responsibility Principle (Đơn Trách Nhiệm)

Mỗi lớp trong hệ thống chỉ đảm nhận một trách nhiệm duy nhất:

* [`DemonLordAwakeningOrbItem`](file:///d:/Users/MinhPhuc00ne/Projects/mod-weapon/common/src/main/java/com/minhphuc/weapons/content/tensura/DemonLordAwakeningOrbItem.java): Chỉ chịu trách nhiệm quản lý vòng đời kích hoạt và điều kiện sử dụng của viên ngọc thức tỉnh.
* [`TensuraSkillDispatcher`](file:///d:/Users/MinhPhuc00ne/Projects/mod-weapon/common/src/main/java/com/minhphuc/weapons/content/tensura/TensuraSkillDispatcher.java): Chỉ chịu trách nhiệm điều phối và gọi đúng kỹ năng dựa trên chỉ số người chơi chọn.
* [`TomeLootManager`](file:///d:/Users/MinhPhuc00ne/Projects/mod-weapon/common/src/main/java/com/minhphuc/weapons/content/tensura/TomeLootManager.java): Chỉ chịu trách nhiệm can thiệp vào bảng rơi đồ (Loot Tables) của rương tự nhiên.

### 1.2. O — Open / Closed Principle (Mở Rộng nhưng Đóng Sửa Đổi)

Hệ thống được thiết kế mở để dễ dàng bổ sung kỹ năng hoặc trang bị mới mà không cần can thiệp viết lại tầng mạng hoặc vòng lặp sự kiện:

* Để thêm một kỹ năng mới cho Ma Vương, lập trình viên chỉ cần tạo lớp `NewAbility` và đăng ký thêm một `case` trong `TensuraSkillDispatcher` mà không cần sửa đổi `ModMessages` hay `ClientInputEvents`.

### 1.3. L — Liskov Substitution Principle (Thay Thế Liskov)

Tất cả các vật phẩm và thực thể tùy biến đều kế thừa hoàn hảo từ các lớp cơ sở của Minecraft:

* `DemonLordAwakeningOrbItem` kế thừa `Item`, tuân thủ đúng phương thức `use(Level, Player, InteractionHand)` và trả về `InteractionResultHolder<ItemStack>`.
* Các bộ phận Giáp Thần Linh (`DivineArmorItem`) kế thừa `ArmorItem`, hoạt động tương thích hoàn toàn với hệ thống giáp, enchantment và mending của Minecraft Vanilla.

### 1.4. I — Interface Segregation Principle (Phân Tách Giao Diện)

Không ép buộc bất kỳ lớp nào phải thực thi những phương thức mà nó không sử dụng. Các tương tác client/server được tách rời thông qua các giao diện mạng chuyên biệt.

### 1.5. D — Dependency Inversion Principle (Đảo Ngược Phụ Thuộc)

Logic của bản mod phụ thuộc vào các tầng trừu tượng cấp cao của Minecraft API (`Level`, `ServerPlayer`, `DamageSource`, `RegistrySupplier`) thay vì gắn chặt vào các implementation cụ thể của một modloader duy nhất.

---

## 2. Nguyên Tắc KISS (Keep It Simple, Stupid)

* **Đơn giản hóa trải nghiệm**:
  * Thay vì bắt người chơi phải mở nhiều GUI phức tạp để nâng cấp từng bậc 1, 2, 3 cho kỹ năng tiến hóa, toàn bộ kỹ năng tiến hóa được gán mặc định luôn ở **Cấp 3 (Cấp Tối Đa)**.
  * Thao tác chọn chiêu thức được quy về một phím duy nhất: **`[Z]`** để xoay vòng và **`[Chuột Phải]`** để tung chiêu.
  * Phòng ngự tuyệt đối của Uriel chỉ có 2 trạng thái rõ ràng: **`[BẬT]`** hoặc **`[TẮT]`**.

---

## 3. Nguyên Tắc DRY (Don't Repeat Yourself)

* **Tập trung hóa truy xuất NBT**: Không viết đi viết lại mã đọc/ghi NBT rải rác trong từng class; toàn bộ được tập trung tại `EntityDataHelper`.
* **Tập trung hóa hệ thống âm thanh & thông báo**: Sử dụng lớp `VoiceOfTheWorld` cho toàn bộ các thông báo mang phong cách "Giọng Nói Thế Giới" của Tensura, đảm bảo format màu sắc và tiền tố luôn đồng nhất.

---

## 4. Nguyên Tắc YAGNI (You Aren't Gonna Need It)

* Tuyệt đối không giữ lại các đoạn mã "để dành cho tương lai" nếu nó không phục vụ cho tính năng thực tế. Khi loại bỏ Găng Tay Vô Cực và Sách Tiến Hóa Kỹ Năng, toàn bộ mã liên quan đã được dỡ bỏ hoàn toàn khỏi dự án, giúp bản mod nhẹ, sạch và tốc độ build cực nhanh.
