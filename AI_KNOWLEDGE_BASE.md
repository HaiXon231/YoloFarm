# YoloFarm Project - Knowledge Base & Architecture Context

Tài liệu này đóng vai trò là "Bộ nhớ dài hạn" (Long-term Memory) đúc kết toàn bộ bức tranh kiến trúc, luồng đi của dữ liệu, cũng như các giới hạn/đặc thù (quirks) đã được xử lý trong dự án YoloFarm.
Mục tiêu là giúp bất kỳ một phiên làm việc (Workspace) mới nào của AI Agent hoặc Lập trình viên mới có thể lập tức nắm bắt ngữ cảnh, tránh việc đi vào các "vết xe đổ" hoặc phá vỡ các luồng Logic đã được tinh chỉnh cấu trúc phức tạp.

---

## 1. Tổng quan hệ thống (System Overview)

YoloFarm là một hệ thống IoT Nông nghiệp Thông minh toàn diện (Smart Farming) bao gồm 3 khối kiến trúc chính:
- **Frontend (Web App):** Trải nghiệm người dùng (SPA) bằng React, Typescript, Vite, TailwindCSS.
- **Backend (API + Xử lý trung tâm):** Spring Boot 3 (Java), PostgreSQL, phân tích sự kiện mạng, bảo mật và tương tác MQTT.
- **Digital-Twin (Simulator):** Script Python độc lập mô phỏng phần cứng IoT (Thiết bị Cảm biến / Bơm / Đèn), đồng bộ thẳng với CSDL PostgreSQL và tương tác với Broker MQTT.
- **Broker (Bên thứ 3):** Adafruit IO (Dịch vụ MQTT Broker Cloud).

---

## 2. Các khối Kiến trúc Chi tiết

### 2.1 Backend (Spring Boot 3)
Đóng vai trò là linh hồn của hệ thống, xử lý song song các tác vụ Web API thuần túy (HTTP) và tác vụ Real-time thông qua WebSockets / MQTT.

**A. Core Stack & Database:**
- Database Schema tự động tạo bằng Hibernate (ddl-auto=update). PostgreSQL (`yolofarm_db`).
- Khóa chính (Primary Key) của hầu hết các thực thể (Users, Farms, Devices, Rules...) sử dụng chuẩn **UUID** để bảo mật và mở rộng quy mô.

**B. Bảo mật (Security & JWT):**
- Sử dụng Stateless Session Policy với **JWT**. 
- Quản lý phân quyền dựa trên `RoleEnum` (`ADMIN`, `FARMER`).
- Các hàm / API nhạy cảm ở `AdminController` được quản lý chặt chẽ theo `hasRole('ADMIN')`.
- Không sử dụng mật khẩu/đăng nhập tự động cứng (hard-coded seeders) trên môi trường thật, việc đăng ký diễn ra thủ công trên trang React `/register`.

**C. Mẫu thiết kế cốt lõi (Design Patterns):**
1. **Quản lý sự kiện MQTT (Observer Pattern):** 
   - `MqttReceiverService` đóng vai trò là **Subject** nhận luồng tin hiệu từ Adafruit.
   - Các logic hậu kỳ (Post-processing) được chia nhỏ thành các **Observer** và đính kèm (attach) tự động:
     - `DatabaseLoggerObserver`: Lưu lịch sử dội liệu vào PostgreSQL.
     - `RuleEngineObserver`: Não bộ tự động hóa, kiểm tra và kích hoạt máy bơm.
     - `WebSocketNotifierObserver`: Dẩy dữ liệu thẳng lên trình duyệt React cho người dùng.
2. **Bộ máy Tự động hóa (Strategy Pattern):**
   - Được vận hành bởi `IrrigationContext`. Chấp nhận các Strategy nhúng chéo như `ManualStrategy`, `AutoThresholdStrategy`, `ScheduledStrategy` tùy theo mệnh lệnh (Tay, Cảm biến, hoặc Hẹn giờ).

**D. Background Jobs (Spring `@Scheduled`):**
- **RuleSchedulerService:** Quét mỗi phút/giây. Đã được thiết kế để **tự khắc phục lỗi** cho phép Cron Format 5 tham số kiểu UNIX (tự động bổ sung "0" giây) để tương thích việc Frontend gửi Data lên.
- **DeviceHeartbeatService:** Quét mỗi 1 phút. Dìm tất cả thiết bị không gửi tín hiệu quá 5 phút về mức `OFFLINE` ở column `connection_status`.

### 2.2 Frontend (React Vite)
- **State Management:** Zustand để lưu context người dùng sau khi giải mã JWT đăng nhập.
- **Websockets:** `sockjs-client` + `@stomp/stompjs`. Kênh chốt cho mỗi Farm để nhận tín hiệu realtime từ Java Backend: `/topic/farm/{farmId}/telemetry`.
- **UI System:** Không có thư viện Component nặng, sử dụng TailwindCSS tinh giản. Thiết kế Dashboard giám sát realtime có khả năng nhấp nháy đèn báo (Flashing indicator) dựa theo lưu lượng dội socket.

### 2.3 Simulator (Digital-Twin trong Python)
- Quét DB (Postgres) mỗi N giây. Chắt lọc các Devices nào có trang thái `STATUS='ACTIVE'` VÀ `adafruit_feed_key IS NOT NULL`.
- Bỏ qua tương tác API Web, chạy độc lập để giảm tải.
- Sử dụng mô hình Pool Connection để kết nối cơ sở dữ liệu (`try..finally` fix) – Đảm bảo quá trình thiết lập dài hạn không bị lỗi **Exhausted Database Connections/Connection Leak**.

---

## 3. Các điểm kỹ thuật phức tạp (Contextual Quirks & Gotchas)
⚠️ **CẢNH BÁO QUAN TRỌNG CHO AI/DEV ĐI SAU CẦN ĐỌC KỸ**

1. **Quirk của Adafruit IO (The MQTT Reconnect Bug)**
   - Adafruit broker thường xuyên ngắt kết nối (Disconnect) nếu kết nối lâu để tiết kiệm tài nguyên.
   - Do Paho `cleanSession=true`, nếu reconnect, toàn bộ các gói Subscription bị Adafruit xóa trắng. 
   - Đồng thời, Wildcard của Adafruit `+/feeds/+` **chỉ có giá trị với các feed ĐÃ TỒN TẠI** tại lúc tạo kết nối. Nếu thêm thiết bị mới giữa chừng, thiết bị đó KHÔNG rơi vào cái rổ Wildcard này.
   - **Giải pháp đang áp dụng:** Bên trong Java `MqttReceiverService`, ở hàm `subscribeIfConnected`, Backend sẽ lặp nguyên bảng Devices, lấy từng `feedKey` và Subscribe Explicit 1-1 cho độ chính xác cao nhất mỗi khi `connectComplete()`. Đồng thời trong `DeviceService.approveDevice()`, một event Dynamic Subscribe cũng được chèn vào. Tuyệt đối không chỉnh sửa cấu trúc Reconnect này nếu không phải cấu trúc lại toàn bộ Broker!

2. **Quy tắc tạo Feed Key Adafruit**
   - Không đặt bừa Feed Key. Hệ thống sử dụng thuật toán nối chuỗi tạo Uniform Feed Key chống trùng lắp ở `DeviceService.generateAutoFeedKey(Device)`:
   - Cú pháp: `u{owner_uuid_8_char}-d{device_uuid_12_char}-{metric_name_lowercase}`
   - *Ví dụ:* `u12345678-d123456781234-temperature`

3. **Vấn đề chống N+1 & LazyInitializationException**
   - Vì hàm callback `messageArrived` của MQTT Client chạy trên thread tẻo (Ngoại vi - Outside Transactional context của Http Request), các lệnh tải cấu trúc quan hệ (FK) chậm (Lazy Load) như `rule.getFarm().getOwner()` sẽ lập tức Crash Java Runtime.
   - **Giải pháp:** Sử dụng đặc thù `JOIN FETCH` trong các interface Repository (`@Query("SELECT r FROM Rule r JOIN FETCH r.farm JOIN FETCH...")...`). Luôn tra vào đó nếu cần truy xuất dữ liệu dính chuỗi liên quan (Relations chaining).

4. **Cách thức Xóa Toàn bộ Database (Soft Reset)**
   - Không được drop bảng. Vì `models` (Device Models) được seeding chết làm khung xương để thiết bị quy chiếu.
   - Chỉ được can thiệp xóa bằng SQL: `TRUNCATE TABLE users CASCADE;`
   - Lệnh này sẽ tuột dây chuyền làm bốc hơi toàn bộ Nông trại, Thiết bị con, Rule, và Telemetry. Dọn dẹp sạch sẽ để nhường chỗ test người dùng mới.

---

## 4. Work in Progress / Next Evolutions (Nếu có thể)
- Cập nhật cơ sở dữ liệu `models` trở nên phong phú hơn, hỗ trợ chia trang (pagination).
- Đóng gói toàn bộ nền tảng bằng Docker Compose với Mosquitto nội bộ (thoát lý giới hạn của Adafruit IO cho sản xuất độc lập).

---
*Tài liệu này được tạo bởi AI Agent dựa trên quá trình tương tác và triển khai với USER. Luôn view lại tài liệu để xác định Context.*
