# 📋 QUY TẮC NGHIỆP VỤ & CƠ CHẾ VẬN HÀNH (BUSINESS RULES)

## Bản Mod: `Weapons Mod — Mythical Armament & Tensura`

---

## 1. Quy Tắc Thời Gian Hồi Chiêu (Zero Cooldown Rule)

* **Nguyên tắc cốt lõi**: Toàn bộ kỹ năng của người chơi (Thần Linh Vũ Trang, Chân Ma Vương, Thủy Tổ Ác Ma, Thái Tuế Tinh Quân, Thần Khí Nguyệt Quang Kiếm) **hoàn toàn không có thời gian hồi chiêu (0 giây cooldown)**.
* **Mục đích**: Mang lại trải nghiệm chiến đấu bùng nổ, linh hoạt tối đa theo đúng phong cách nguyên tác Anime/Manga Tensura và Dark Gathering.
* **Cơ chế chống nghẽn**: Tối ưu thuật toán xử lý hạt (Particle Math) và giới hạn thực thể tính toán để đảm bảo dù thi triển liên tục vẫn giữ vững mức 20 TPS (Zero Lag).

---

## 2. Quy Tắc Thức Tỉnh Chân Ma Vương (True Demon Lord Awakening)

Người chơi có 2 lộ trình để tiến hóa thành **Chân Ma Vương (True Demon Lord)**:

### 2.1. Lộ trình Tức thì: Sử dụng Ngọc Thức Tỉnh Ma Vương (Demon Lord Awakening Orb)

* **Thuộc tính vật phẩm**:
  * Tỉ lệ xuất hiện: **2.0% (`0.02F`)** trong các rương tự nhiên của thế giới (Dungeon, Nether Fortress, End City, Ancient City, Bastion, Stronghold...).
  * Có sẵn trong Tab Sáng Tạo: `Vũ Khí Thần Thoại & Ma Vương`.
  * **Không thể chế tạo** bằng bất kỳ bàn chế tạo nào (Uncraftable).
* **Hiệu lực khi kích hoạt ([Chuột Phải])**:
  1. Thức tỉnh ngay lập tức thành Chân Ma Vương trong 1 tick duy nhất (không cần chờ Lễ Hội Thu Hoạch 20 giây, không dính hiệu ứng ngủ).
  2. Máu tối đa vĩnh viễn tăng vọt lên **40 Tim (80.0 HP)** và hồi đầy máu.
  3. Mở khóa toàn bộ Kỹ năng Tối thượng ở **Cấp 3 (Cấp tối đa)**.
  4. Mở khóa năng lực phi hành không gian (`Mayfly = true`).
  5. Giọng Nói Thế Giới (Voice of the World) phát âm thanh kèn chiến thắng và thông báo toàn máy chủ.

### 2.2. Lộ trình Cày cuốc: Nghi Lễ Thu Hoạch (Harvest Festival)

* Chế tạo **Hạt Giống Ma Vương (Demon Lord Seed)** và gom đủ 64 Linh Hồn Ma Vương thông qua tiêu diệt quái vật / Boss.
* Trải qua 4 giai đoạn biến đổi tâm trí, giấc ngủ tiến hóa 20 giây để đạt cảnh giới Chân Ma Vương.

---

## 3. Quy Tắc Phòng Ngự Tuyệt Đối Của Uriel (Uriel Absolute Defense)

* **Điều kiện sở hữu**:
  * Mặc đủ **4 Món Giáp Thần Linh (Full Divine Armor)**.
  * Hoặc đã thức tỉnh thành **Chân Ma Vương** (sở hữu vĩnh viễn trong danh sách phím `Z`, không mất đi ngay cả khi cởi bỏ giáp).
* **Trạng thái & Điều khiển**:
  * Mặc định khi vừa nhận giáp/thức tỉnh: **`[BẬT]` (ON)**.
  * Người chơi có thể bấm phím **`Z`** tìm đến kỹ năng *Phòng Ngự Tuyệt Đối Của Uriel* và nhấp chuột phải để chuyển đổi qua lại giữa **`[BẬT]`** và **`[TẮT]`**.
* **Hiệu lực phòng thủ**:
  * Khi **`[BẬT]`**: Miễn nhiễm 100% mọi nguồn sát thương thông thường (Cận chiến, Bắn cung, Nổ TNT, Rơi từ trên cao, Sét đánh, Lửa/Dung nham, Sonic Boom của Warden...).
  * Khi **`[TẮT]`**: Nhận sát thương bình thường như người chơi thông thường.
* **Cơ chế Bị Xuyên Giáp (Penetration Rule)**:
  * Các nguồn sát thương tối thượng (Linh Tử Băng Hoại, Không Vong Kūbō, Tà Tinh Hư Vô, Đòn Đánh Thứ Nguyên của Velgrynd) sẽ **xuyên thủng hoàn toàn** Phòng Ngự Tuyệt Đối.
  * Khi bị đánh xuyên thủng, hệ thống tự động cảnh báo lên Action Bar / Khung Chat:
    > `§e§l[CẢNH BÁO] §c§lBáo cáo: Phòng thủ tuyệt đối của Uriel không thể ngăn chặn tuyệt đối các linh tử.`
* **Cơ chế cởi giáp**:
  * Nếu người chơi chưa phải là Chân Ma Vương: Việc tháo bất kỳ món nào trong 4 món Giáp Thần Linh sẽ lập tức hủy bỏ trạng thái bảo hộ.

---

## 4. Quy Tắc Nguyệt Quang Thần Tế Kiếm (Moonlight Ritual Sword)

Đã được tái cấu trúc tinh gọn theo yêu cầu tác chiến tối cao, loại bỏ toàn bộ 5 chiêu thức cũ và chỉ giữ lại 2 kỹ năng:

| STT | Kỹ Năng | Thao Tác | Cơ Chế & Hiệu Lực |
| :---: | :--- | :--- | :--- |
| **1** | **Hắc Thiểm Bá Vương Trảm** | **Chuột Trái / Tấn Công** | Vết chém không gian rực lửa đen tuyền bộc phát sát thương bạo kích cực đại, bỏ qua kháng cự cơ bản của mục tiêu. |
| **2** | **Linh Tử Băng Hoại** | **Chuột Phải** | Bắn ra 1 viên cầu ánh sáng 7 màu (Rainbow Spiritron Orb) bay thẳng 50 blocks. Khi va chạm thực thể hoặc địa hình, kích nổ san phẳng một vùng và gây **30 Tim Sát Thương (60.0 HP)**. |

---

## 5. Quy Tắc Bảng Kỹ Năng Bản Thể (Player Innate Skills - Phím `Z`)

Toàn bộ các kỹ năng cổ xưa, kỹ năng tiến hóa từ sách cũ đã được gom chung vào một danh sách duy nhất luân chuyển bằng phím **`Z`**:

1. **Linh Tử Băng Hoại** (Sanctuary Disintegration): Phân rã mục tiêu thành linh tử trong vòng ma trận.
2. **Tà Khứ Vũ Thê Tử** (Jacob's Ladder): Cột sáng thiên đường thanh tẩy tà khí.
3. **Phép Màu Của Thần Linh**: Vùng hào quang thần thánh hồi phục sinh lực và giải trừ bùa hại.
4. **Bí Ẩn Của Đức Tin Và Ân Sủng**: Tuyệt kỹ giáng lâm ngay sau Phép Màu Thần Linh; tồn tại trong 5 giây, quét rộng 40x40 blocks, cột sáng khổng lồ hạ từ trên trời xuống ban phước và thanh tẩy tà ác.
5. **Bạo thực vương** (Beelzebuth): Thôn phệ thực thể, nuốt chửng linh hồn và ma tố.
6. **Long Tinh Bộc Viêm Bá** (Drago-Nova): Đòn hủy diệt tinh cầu của Milim.
7. **Thần Nộ: Tuyệt Diệt Tinh Tú**: Bắn 5 thiên thạch sao băng tiêu diệt mục tiêu.
8. **Thị Thực**: Khối thịt trường sinh của Thái Tuế, thu hoạch mắt hồi máu 100%.
9. **Diệt Thế Tà Tinh: Alkaid**: Mũi khoan vũ trụ 14m xuyên phá địa hình.
10. **Cú Bắn Granit** (Granite Blast).
11. **Nghịch Thời Nhất Trảm** (Tengai Kyusei Reverse-Time Slash).
12. **Bạt Đao Thuật Vận Tốc Ngược** (Reverse-Velocity Iaijutsu).
13. **Sáng Tạo Vật Chất** (Sau khi đánh bại Noir).
14. **Kháng Ma Kết Giới** (Anti-Magic Barrier).
15. **Đa Trùng Kết Giới** (Multilayer Barrier).
16. **Zone Track**: Cảm biến và định vị không gian mục tiêu.
17. **Cảm Nhận Vạn Năng** (Universal Sense - Cấp 3).
18. **Lồng Giam Vô Hạn** (Infinite Dragon Prison - Cấp 3).
19. **Phòng Ngự Tuyệt Đối Của Uriel** (BẬT / TẮT).

*Toàn bộ kỹ năng tiến hóa luôn mặc định ở **Cấp 3**, loại bỏ cơ chế tiêu hao cấp bậc.*

### 5.1. Quy Tắc Cơ Chế Tuyệt Kỹ: Nghịch Thời Nhất Trảm (Dark Gathering Style)

* **Thao tác**: Nhấn phím nóng **`X`** hoặc chọn chiêu số 11 qua menu phím **`Z`** rồi nhấn Chuột Phải.
* **Cơ chế thi triển bám sát nguyên tác**:
  1. **Người chơi đứng yên tuyệt đối**: Nhân vật không dịch chuyển hay lướt đi, giữ nguyên tọa độ góc nhìn để quan sát đòn đánh ngoạn mục.
  2. **Ảo ảnh bóng ma (Ghostly Phantom Silhouette) lao tới**: Không sử dụng màu da người thật nhiều màu sắc, mà tạo ra 1 phân thân ảo ảnh mang hình dạng humanoid bóng ma hắc ám (Đầu lâu Wither Skull kết hợp giáp bóng ma màu đen khói `0x12141D` có hiệu ứng holographic shimmer `ENCHANTMENT_GLINT_OVERRIDE`), tỏa hào quang trắng rực sáng manga (`GlowingTag`), bạt đao lao nhanh với vệt lửa linh hồn và các khối pixel số hóa vuông vức bay ra từ vết chém chuẩn theo trang truyện.
  3. **Thế giới chuyển sang Trắng Đen (Monochrome Manga)**: Toàn bộ thế giới lập tức chuyển sang hiệu ứng trắng đen tương phản cao (Manga Shader `desaturate.json`) trong 16 ticks (0.8s).
  4. **Đồng hồ LED đếm ngược thời gian**: Các khối đèn LED số 3D hiển thị thời gian số hóa chạy ngược: `-0.04` ➔ `-0.02` ➔ ` 0.00`.
  5. **Mục tiêu bị đóng băng & Sát thương trễ (Delayed Damage)**:
     * Trong suốt giai đoạn trắng đen (`-0.04s` đến `-0.02s`): Mục tiêu bị đóng băng không gian nhưng **hoàn toàn chưa phải nhận sát thương**.
     * **Đúng tại thời khắc `0.00s`**: Màn hình bừng sáng trở lại màu sắc tự nhiên -> Toàn bộ **150 sát thương cực đại** cùng hiệu ứng Flash, tiếng keng đanh thép và chém nổ diện rộng mới chính thức bộc phát!
  6. **Tối ưu hóa hiệu năng (Zero Lag)**: Phân thân tàn ảnh tự động hủy (`discard()`) sau 16 ticks, không để lại bất kỳ thực thể rác nào trên thế giới.

### 5.2. Quy Tắc Cơ Chế Tuyệt Kỹ: Bạt Đao Thuật Vận Tốc Ngược (Reverse-Velocity Iaijutsu)

* **Thao tác**: Chọn chiêu số 12 qua menu phím **`Z`** rồi nhấn Chuột Phải để kích hoạt.
* **Cơ chế thi triển bám sát nguyên tác Dark Gathering (Chapter 83)**:
  1. **Ngưng đọng thời gian & Manga Trắng Đen**: Thế giới chuyển sang màu trắng đen tương phản cao, mọi quái vật trong 48 blocks bị đóng băng tuyệt đối.
  2. **Thân xác vật lý đứng yên**: Một thực thể mang đúng 100% Skin/Profile, giáp và vật phẩm người chơi đang cầm trên tay đứng bất động tại tọa độ kích hoạt.
  3. **Linh hồn xuất khiếu di chuyển tự do**: Người chơi tự do di chuyển khám phá, quan sát xung quanh; khi nhấn **F5** sẽ thấy linh hồn ở dạng bán trong suốt (translucent ghost).
  4. **Chuỗi 4 lần nhấp chuột khắc dấu ấn**:
     * Nhấn #1: Mốc `[-0.04s]` (Hiện LED 3D, hạt vết cắt không gian, không gây sát thương).
     * Nhấn #2: Mốc `[-0.02s]` (Hiện LED 3D, không gây sát thương).
     * Nhấn #3: Mốc `[-0.01s]` (Hiện LED 3D, không gây sát thương).
     * Nhấn #4: Mốc `[ 0.00s]` (Hiện LED 3D, không gây sát thương).
  5. **Nhấn chuột lần 5 — Đòn Kết Liễu Điện Ảnh (~2.0s, Pacing chuẩn điện ảnh)**:
     * Thế giới bừng sáng trở lại màu sắc tự nhiên.
     * Đồng loạt xuất hiện 4 tàn ảnh phân thân tại đúng 4 vị trí đã đánh dấu.
     * Chuỗi âm thanh rút kiếm dồn dập "Keng! Keng! Keng! Keng!" nối tiếp nhau ➔ Tra kiếm vào vỏ.
     * Toàn bộ sát thương bộc phát (**75 sát thương/đòn**, dồn 4 đòn lên tới **300 sát thương cực đại**) kèm hạt Flash trắng lóa và vết chém xé toạc không gian.
     * Thể xác và linh hồn hòa làm một, dọn sạch toàn bộ thực thể rác (Zero Lag).

---

## 6. Danh Mục Tính Năng Bị Loại Bỏ Hoàn Toàn (Removed Features)

1. **Găng Tay Vô Cực & 6 Viên Đá Vô Cực** (Cùng sự kiện rơi đá từ Wither, Warden, Ender Dragon...).
2. **Bát Môn Thiên Phạt Trận** (Lồng giam 7 cột sáng cũ).
3. **Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm Thần Khóa)**.
4. **Quyển Thư Tiến Hóa Kỹ Năng (Skill Evolution Tome)**.
5. **Vương Quyền Chi Phối (Regalia Dominion)**.
