# 🏷️ QUY ƯỚC ĐẶT TÊN CHUẨN MỰC (NAMING CONVENTIONS)

## Bản Mod: `Weapons Mod — Mythical Armament & Tensura`

Nhằm đảm bảo tính đồng bộ, chuyên nghiệp và tránh xung đột tài nguyên trong hệ sinh thái Minecraft Fabric/Architectury, toàn bộ mã nguồn và tệp tài nguyên phải tuân thủ nghiêm ngặt các quy ước đặt tên dưới đây.

---

## 1. Quy Ước Mã Nguồn Java

### 1.1. Tên Gói (Package Naming)

* **Quy tắc**: Toàn bộ viết thường, phân cách bằng dấu chấm, bắt đầu bằng định danh tổ chức/tác giả:
  * `com.minhphuc.weapons` (Gói gốc)
  * `com.minhphuc.weapons.content.divine` (Vũ khí thần linh)
  * `com.minhphuc.weapons.content.tensura` (Hệ thống Tensura)
  * `com.minhphuc.weapons.content.darkgathering` (Hệ thống Dark Gathering)
  * `com.minhphuc.weapons.content.evolution` (Kỹ năng tối thượng)
  * `com.minhphuc.weapons.network` (Gói tin mạng)
  * `com.minhphuc.weapons.client.gui` (Giao diện đồ họa người dùng)

### 1.2. Tên Lớp & Giao Diện (Class & Interface Naming)

* **Quy tắc**: `PascalCase` với hậu tố ngữ nghĩa rõ ràng:
  * Kỹ năng: Hậu tố `Ability` (ví dụ: `FaithAndGraceAbility`, `BeelzebuthAbility`).
  * Vật phẩm: Hậu tố `Item` (ví dụ: `DemonLordAwakeningOrbItem`, `MoonlightSwordItem`).
  * Thực thể: Hậu tố `Entity` (ví dụ: `KuboEntity`, `PrimordialDemonEntity`).
  * Màn hình GUI: Hậu tố `Screen` (ví dụ: `GuideBookScreen`, `GoldenGunSelectScreen`).
  * Gói tin mạng C2S: Tiền tố `Serverbound` + Hậu tố `Packet` (ví dụ: `ServerboundCastBeelzebuthPacket`).
  * Gói tin mạng S2C: Tiền tố `Clientbound` + Hậu tố `Packet`.
  * Lớp tiện ích: Hậu tố `Helper` hoặc `Manager` (ví dụ: `EntityDataHelper`, `TomeLootManager`).

### 1.3. Tên Phương Thức & Biến (Method & Variable Naming)

* **Quy tắc**: `camelCase`, bắt đầu bằng động từ hoặc trợ từ thể hiện rõ mục đích:
  * `isUrielDefenseActive(Player player)`
  * `setUrielDefenseActive(Player player, boolean active)`
  * `dispatchSkill(ServerPlayer player)`
  * `executeReverseSlash(ServerPlayer player)`

### 1.4. Tên Hằng Số & Trường Tĩnh (Constants & Static Final)

* **Quy tắc**: `UPPER_SNAKE_CASE`:
  * `MOD_ID = "weapons"`
  * `NBT_URIEL_DEFENSE_ACTIVE = "UrielDefenseActive"`
  * `DEMON_LORD_AWAKENING_ORB`

---

## 2. Quy Ước Tài Nguyên Game (Assets & Data)

### 2.1. Mã Định Danh ResourceLocation & Tệp (Asset Files)

* **Quy tắc**: Toàn bộ viết thường, phân cách bằng dấu gạch dưới (`lower_snake_case`):
  * Item ID: `weapons:demon_lord_awakening_orb`, `weapons:moonlight_sword`, `weapons:celestial_tome`
  * Model JSON: `assets/weapons/models/item/demon_lord_awakening_orb.json`
  * Texture PNG: `assets/weapons/textures/item/demon_lord_awakening_orb.png`

### 2.2. Khóa Dịch Ngôn Ngữ (Translation Keys)

* **Quy tắc**: Phân cấp rõ ràng theo loại tài nguyên:
  * Vật phẩm: `item.<mod_id>.<item_id>` (ví dụ: `item.weapons.demon_lord_awakening_orb`)
  * Tooltip mô tả: `tooltip.<mod_id>.<item_id>.<sub_key>` (ví dụ: `tooltip.weapons.celestial_tome.desc1`)
  * Giao diện: `gui.<mod_id>.<screen_name>.<element>` (ví dụ: `gui.weapons.guide.p0.title`)
  * Phím bấm: `key.<mod_id>.<action>` (ví dụ: `key.weapons.tengai_slash`)
  * Nhóm sáng tạo: `itemGroup.<mod_id>.<tab_name>` (ví dụ: `itemGroup.weapons.weapons_tab`)

---

## 3. Quy Ước Thẻ Dữ Liệu Tùy Biến (Custom NBT Keys)

Nhằm đảm bảo không bị trùng lặp với NBT của Minecraft Vanilla hay các mod khác, các thẻ lưu trữ tùy biến sử dụng tiền tố hoặc tên rõ ràng trong `EntityDataHelper`:

* `TensuraTrueDemonLord`: Cờ boolean xác định người chơi là Chân Ma Vương.
* `TensuraCollectedSouls`: Số lượng linh hồn Ma Vương đã thu thập.
* `SelectedTensuraSkillIndex`: Chỉ số kỹ năng hiện tại được chọn bởi phím `[Z]`.
* `UrielDefenseActive`: Cờ boolean xác định trạng thái bật/tắt của Phòng Ngự Tuyệt Đối Uriel.
