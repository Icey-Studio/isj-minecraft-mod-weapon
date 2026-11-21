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
5. **Bạo Thực Vương** (Beelzebuth):
   - **Lốc Xoáy Hư Không Anime**: Cột lốc xoáy màu tím đen cao 42m kết hợp 4 dải nước xoắn ốc lam ngọc quấn quanh và các đĩa cộng hưởng hư không nằm ngang.
   - **Hút Chất Lỏng & Sinh Vật Không Lag Biển (Ocean Safe)**: Hút nước biển, nước hồ, dung nham và sinh vật vào Dạ Dày Vô Hạn Azathoth. Cơ chế cấm kích hoạt chuỗi block update lan truyền giúp 100% không drop TPS khi dùng giữa đại dương. Tuyệt đối không làm tổn hại block rắn (cát, đá, san hô, công trình).
   - **Tự Động Thanh Lọc & Phân Tích**: Tách muối, tạp chất, chuyển hóa áp suất thủy lực và nhiệt năng tích lũy +80,000 Ma tố qua Cố Vấn Tác Chiến Raphael.
   - **Thủy Khí Trảm (Water Blade)**: [Shift + Nhìn Thẳng] tiêu hao nước trong Dạ Dày, chém sóng nước áp suất cao 36m (140 True DMG) xuyên thấu.
   - **Giải Phóng Thủy Triều (Tidal Release)**: [Shift + Nhìn Xuống] xả lũ áp suất cao 360° bán kính 12m, dập tắt biển lửa và làm nguội dung nham thành Hắc Diệu Thạch.
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

## 6. Kỹ Năng Độc Nhất: Kẻ Tham Lam (Greedy One — 强欲者)

* **Điều kiện thức tỉnh**: Người chơi bắt buộc phải đã thức tỉnh thành **Chân Ma Vương**.
* **Phương thức đạt được**:
  1. *Lộ trình nguyên tác*: Khiêu chiến tiêu diệt Mariabell Rosso để thu được **Tinh Hồn Mariabell Rosso (`mariabell_soul`)**, sau đó tiến hành thanh tẩy linh hồn để khắc sâu kỹ năng độc nhất vào linh hồn.
  2. *Lộ trình tức thì*: Sử dụng **Ngọc Thức Tỉnh Kẻ Tham Lam (`greed_awakening_orb`)** bằng cách click Chuột Phải để lập tức mở khóa kỹ năng.
* **Quyền năng tác chiến 3 Thức**:
  * **Thức 1 — Khống Chế Dục Vọng (`Crouch`)**: Vòng ma thuật hồng neon tỏa rộng 22m, cưỡng chế tẩy não toàn bộ quái vật không thuộc cấp Chân Long/Ma Vương Tối Cổ thành thuộc hạ trung thành (mắt đỏ, nhận buff Sức mạnh III, Tốc độ II, Hồi phục III và tự động tấn công kẻ địch của người chơi).
  * **Thức 2 — Xiềng Xích Tham Lam (`Chuột Phải`)**: Phóng sợi xích ma pháp màu hồng neon trói chân mục tiêu, hút 20% máu và **CƯỠNG CHẾ THAO TÚNG** biến mục tiêu thành Con Rối Hộ Vệ phục tùng (giữ lại máu, không giết luôn quái).
  * **Thức 3 — Bùng Nổ Dục Vọng (`Shift + Chuột Phải`)**: Kích nổ lòng tham trong tâm trí kẻ địch, gây **85 - 180 sát thương chuẩn** xuyên thấu mọi kết giới bảo vệ kèm sóng xung kích chấn động âm thanh. Toàn bộ con rối phát nổ bộc phát sát thương cực đại.
* **Nội tại linh hồn bất diệt & Cơ chế Con Rối**:
  * Lưu trữ vĩnh viễn trên dữ liệu người chơi, không bao giờ bị mất khi tử vong.
  * Con rối trung thành tự động theo sau bảo vệ chủ nhân (tự dịch chuyển nếu đi xa), tự động tấn công kẻ thù và không bao giờ đánh chủ.
  * Tăng **1.5x (thêm 50%) EXP** nhận được khi kết liễu bất kỳ sinh vật nào.
  * Tỉ lệ **35% tước đoạt vũ khí** của nạn nhân trực tiếp vào Không Gian Ảo Dạ Dày Azathoth.

---

## 7. Hệ Thống Cố Vấn Tác Chiến 1-Chạm Đại Hiền Giả (1-Tap Tactical Co-Pilot)

* **Nguyên lý hoạt động**: Đại Hiền Giả / Raphael liên tục giám sát trạng thái cơ thể, trang bị và môi trường xung quanh người chơi trong thời gian thực.
* **Tự động né tránh khung Chat**: Khi người chơi mở khung chat hoặc gõ lệnh (`/`), banner HUD của Raphael tự động nâng cao lên đỉnh màn hình để không bao giờ bị ô chat hay bảng gợi ý lệnh che khuất.
* **Cơ chế phản hồi 1-chạm**: Khi phát hiện nguy cấp hoặc cơ hội tác chiến, hệ thống gửi thông báo kèm mã hành động chiến thuật `[Y] Đồng ý / [N] Từ chối`:
  * `[Y] (Chấp thuận)`: Gửi gói tin C2S `ServerboundSageActionPacket` tới máy chủ để thực thi giải pháp tối ưu chỉ trong **1 tick duy nhất**:
    1. *Cấp cứu sinh mệnh*: Hồi phục khẩn cấp 10 Tim và thanh tẩy bùa hại khi HP < 25%.
    2. *Thoát ly Hư Không*: Kích hoạt ma pháp không gian đưa người chơi trở về mặt đất an toàn khi rơi Void.
    3. *Hóa đá dung nham*: Đóng băng biển dung nham thành đá hắc diện thạch / đá cuội khi ngã xuống Lava.
    4. *Đảo hướng đạn đạo*: Đổi hướng toàn bộ mũi tên / đạn ma pháp của quái vật quay ngược lại kẻ bắn.
    5. *Hút đồ Azathoth*: Hút sạch quặng, khoáng sản, vật phẩm rơi vãi xung quanh vào Dạ Dày Vô Hạn.
    6. *Sửa chữa vũ trang*: Khôi phục 250 độ bền vũ khí chính/phụ ngay giữa trận đánh.
    7. *Trấn an Veldora*: Sử dụng năng lượng Azathoth bình ổn tâm trí Bạo Phong Long khi rồng phát cuồng.
    8. *Kích hoạt Overdrive*: Tăng cường luân chuyển ma tố khi cạn kiệt năng lượng.
    9. *Tái sinh nhục thân bằng Nhãn Cầu Thị Thực*: Giải trừ 100% án tử moi Tim / Não của Cửu Thế Quyền Hiện, hồi đầy máu và cấp hiệu ứng hồi phục thần thánh.
  * `[N] (Từ chối)`: Đóng thông báo chiến thuật, nhường toàn quyền quyết định cho người chơi.

---

## 8. Danh Mục Tính Năng Bị Loại Bỏ Hoàn Toàn (Removed Features)

1. **Găng Tay Vô Cực & 6 Viên Đá Vô Cực** (Cùng sự kiện rơi đá từ Wither, Warden, Ender Dragon...).
2. **Bát Môn Thiên Phạt Trận** (Lồng giam 7 cột sáng cũ).
3. **Trận Đồ Cưỡng Chế Tai Ương (Lục Nhậm Thần Khóa)**.
4. **Quyển Thư Tiến Hóa Kỹ Năng (Skill Evolution Tome)**.
5. **Vương Quyền Chi Phối (Regalia Dominion)**.
