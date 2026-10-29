# 🧼 QUY CHUẨN MÃ NGUỒN SẠCH & HIỆU NĂNG TỐI CAO (CLEAN CODE)

## Bản Mod: `Weapons Mod — Mythical Armament & Tensura`

Tài liệu này định hình các tiêu chuẩn kỹ thuật bắt buộc nhằm giữ cho mã nguồn luôn trong sạch, dễ bảo trì và đảm bảo trải nghiệm chơi game hoàn toàn mượt mà (**Zero Lag & 20 TPS**).

---

## 1. Kỹ Thuật Tối Ưu Hạt & Hiệu Năng (Zero Lag Engineering)

Trong các bản mod chiến đấu phong cách Anime, nguyên nhân phổ biến nhất gây tụt FPS và lag server (TPS Drop) là việc **lạm dụng Entity (như Armor Stand) để render hiệu ứng**. Mod này áp dụng các giải pháp sạch sau:

### 1.1. Toán Học Hạt Không Cần Thực Thể (Pure Particle Math)

* Mọi ma pháp trận (Vòng tròn Linh Tử, Khế Ước Thủy Tổ, Cột Sáng Bí Ẩn Đức Tin) được tính toán hoàn toàn bằng hình học Vector 3D:

```java
// Ví dụ tính toán vòng tròn hạt không tạo entity
for (int i = 0; i < POINTS; i++) {
    double angle = 2.0 * Math.PI * i / POINTS;
    double px = centerX + RADIUS * Math.cos(angle);
    double pz = centerZ + RADIUS * Math.sin(angle);
    level.sendParticles(ParticleTypes.END_ROD, px, centerY, pz, 1, 0, 0, 0, 0);
}
```

* **Lợi ích**: Không tốn tài nguyên quản lý Entity ID, không phát sinh chi phí đồng bộ vị trí vật lý trên server tick.

### 1.2. Giới Hạn Phạm Vi Quét Mục Tiêu (AABB Distance Constraints)

* Tuyệt đối không duyệt toàn bộ danh sách thực thể của Level bằng `level.getEntities()`.
* Luôn sử dụng `level.getEntitiesOfClass(LivingEntity.class, boundingBox, filter)` với bán kính định sẵn và kiểm tra khoảng cách bình phương (`distanceToSqr <= radius * radius`) thay vì gọi hàm căn bậc hai `Math.sqrt()` tốn CPU.

### 1.3. Quản Lý Vòng Đời Ticking Triệt Để (No Memory Leaks)

* Các bộ quản lý ma trận kéo dài thời gian (như `FaithAndGraceAbility`, `InfiniteDragonPrisonAbility`) phải tự động loại bỏ các phiên bản hết hạn bằng Iterator:

```java
Iterator<RitualSession> it = ACTIVE_SESSIONS.iterator();
while (it.hasNext()) {
    RitualSession session = it.next();
    if (session.isExpired()) {
        session.cleanup();
        it.remove(); // Dọn sạch bộ nhớ ngay lập tức
    }
}
```

---

## 2. Phân Tách Biên Giới Client - Server (Network Hygiene)

1. **Phân quyền Server (Server Authority)**:
   * Client chỉ gửi tín hiệu ý định (Intent): Ví dụ `ServerboundCycleSkillPacket(offset)` hoặc `ServerboundCastBeelzebuthPacket`.
   * Server chịu trách nhiệm kiểm tra tính hợp lệ (Người chơi có phải là Chân Ma Vương không? Có mặc giáp không? Máu có đủ không?) trước khi thi triển và trừ tài nguyên.
2. **Không gọi mã Client trên Server**:
   * Các lớp như `Screen`, `KeyMapping`, `GuiGraphics` nằm độc quyền trong gói `client/` và chỉ được gọi khi môi trường kiểm tra `level.isClientSide()` là true hoặc trong Client Initializer.

---

## 3. Xử Lý Ngoại Lệ & Kiểm Tra Tính Hợp Lệ (Defensive Programming)

* Mọi hàm xử lý sự kiện hoặc nhận packet đều phải có bước kiểm tra bảo vệ (Guard Clause):

```java
// Chuẩn Clean Code: Thoát sớm (Early Return)
if (player == null || player.isRemoved() || level.isClientSide()) {
    return;
}
```

* Tránh lồng ghép quá nhiều khối `if-else` sâu (Arrow Anti-Pattern). Giữ độ sâu của hàm dưới 3 cấp thụt dòng.

---

## 4. Dọn Rác & Loại Bỏ Mã Chết (Dead Code Elimination)

* Khi một tính năng bị thay thế hoặc loại bỏ theo yêu cầu thiết kế (như Găng Tay Vô Cực, Bát Môn Thiên Phạt, Lục Nhậm Thần Khóa, Vương Quyền Chi Phối), toàn bộ:
  * Class tệp mã nguồn Java.
  * Đăng ký trong `ModItems`, `ModMessages`, `ModKeyBindings`.
  * Mô hình JSON (`models/item/`), Texture PNG (`textures/item/`).
  * Khóa ngôn ngữ (`vi_vn.json`, `en_us.json`).
  phải được xóa sạch triệt để, không để lại bất kỳ import rác hoặc tệp tài nguyên mồ côi nào trong bản build.
