1. Kiến trúc Routing & Quản lý State cốt lõi
Trước khi code bất kỳ trang nào, Frontend cần thiết lập bộ khung bảo vệ (Guards) để khớp với Security Stateless (JWT) của Backend.

Global State (Redux / Zustand / Context API):

authStore: Lưu access_token, role (ADMIN/FARMER), và thông tin UserProfile (id, username, email).

Routing Architecture:

PublicRoute: Chỉ dành cho khách chưa đăng nhập (Login, Register). Nếu đã có token, tự động đá văng vào Dashboard.

ProtectedRoute (Role-based): Bọc các trang bên trong. Lấy token gắn vào header Authorization: Bearer {token} của Axios. Nếu API trả về 401 Unauthorized, lập tức xóa token và đá về /login.

2. Các Component Layout (Dùng chung cho toàn App)
2.1. TopHeader Component
Vị trí: Thanh ngang trên cùng của app.

Nhiệm vụ: Hiển thị thông tin user và xử lý thông báo (Notifications).

Thành phần chi tiết:

NotificationBell (Cái chuông):

State: unreadCount (số lượng chưa đọc), notifications (mảng data).

Logic: Khi component mount, gọi GET /api/v1/notifications?page=1&size=10. Đếm số lượng is_read == false để hiện cục màu đỏ lên cái chuông.

UI Dropdown: Bấm vào chuông xổ ra list. Mỗi item in ra message và created_at (format lại dạng "2 giờ trước").

Action: Khi user click vào 1 item chưa đọc, lập tức gọi PUT /api/v1/notifications/{id}/read. Đợi API trả về 200 thì đổi style của item đó sang màu xám (đã đọc) và trừ unreadCount đi 1.

UserProfileMenu:

Logic: Gọi GET /api/v1/auth/me một lần khi load app để lấy UserProfile (username, email, role).

UI: Avatar mặc định + Tên username. Bấm vào xổ ra nút Đăng xuất (Clear authStore và LocalStorage -> redirect /login).

2.2. Sidebar Component
Vị trí: Cột điều hướng bên trái.

Logic động theo Role:

Nếu role === 'FARMER': Render các link Quản lý Nông trại, Yêu cầu thiết bị.

Nếu role === 'ADMIN': Render các link Duyệt thiết bị, Catalog Thiết bị.

3. Trang Xác thực (Public Pages)
3.1. Trang Đăng nhập (/login)
Giao diện: Một Form đặt giữa màn hình, thiết kế tối giản.

States nội bộ: username, password, isLoading (để quay spinner nút submit), errorMessage (hiển thị lỗi chung).

Component con nội bộ:

UsernameInput: Text input thường.

PasswordInput: Có icon con mắt (toggle type "password" <-> "text").

Flow gọi API (POST /api/v1/auth/login):

User bấm Submit -> Set isLoading = true.

Gửi payload { username, password } khớp với LoginRequest DTO.

Thành công (200): Nhận về LoginResponse (có access_token, role). Lưu vào Global State/LocalStorage. Chuyển hướng sang /farms (nếu FARMER) hoặc /admin/devices/requests (nếu ADMIN).

Thất bại (401): Bắt lỗi ErrorResponse. Rút trích trường details ("Tài khoản hoặc mật khẩu không chính xác") gán vào state errorMessage để in ra dòng chữ đỏ dưới nút Submit.

3.2. Trang Đăng ký (/register)
Giao diện: Form tương tự Login nhưng nhiều trường hơn.

States nội bộ: username, email, password, confirmPassword, formErrors (Object chứa lỗi từng field).

Validation Logic (Frontend check trước khi gọi API):

Cấm bỏ trống các trường.

email phải đúng định dạng Regex (vd: có đuôi @gmail.com).

password và confirmPassword phải giống nhau.

Flow gọi API (POST /api/v1/auth/register):

Vượt qua validation frontend -> Gọi API gửi RegisterRequest DTO.

Thành công (201): Bắn một Toast notification màu xanh: "Đăng ký thành công! Vui lòng đăng nhập". Tự động đẩy user về /login.

Thất bại (400 hoặc 409): Bắt ErrorResponse. Đọc mã code. Nếu 409 (Lỗi trùng lặp), lấy trường details ("Tên đăng nhập 'xyz' đã được sử dụng") và bôi đỏ đúng cái ô input Username.

PHẦN 2: CỔNG NÔNG DÂN (FARMER PORTAL - CORE FEATURES)
1. Trang Quản lý Nông trại (/farms)
Đây là trang chủ của Nông dân sau khi đăng nhập.

Mục đích: Liệt kê các vườn, cho phép Thêm/Sửa/Xóa.

States nội bộ: farms (mảng chứa FarmResponse), isLoading, isModalOpen, selectedFarm (dùng khi Sửa/Xóa).

Các Component chi tiết:

FarmList (Lưới danh sách): * Khi component vừa render (mount), gọi GET /api/v1/farms.

Render ra một lưới (Grid) các FarmCard. Nếu mảng rỗng, hiển thị một Empty State thân thiện: "Bạn chưa có nông trại nào. Bấm nút Thêm mới ngay!".

FarmCard (Thẻ Nông trại):

Hiển thị: Tên (name), Vị trí (location), Ngày tạo (format ngày tháng dễ nhìn).

Bấm vào phần thân thẻ -> Chuyển hướng sang trang chi tiết (/farms/{farmId}).

Góc phải thẻ có nút Menu (3 chấm) chứa 2 action: Sửa và Xóa.

FarmFormModal (Popup Thêm/Sửa):

Tái sử dụng form cho cả 2 mục đích.

Gồm 2 ô input: name (bắt buộc), location.

Xử lý lỗi 400 (Thiếu tên) từ API. Trả về Toast báo thành công và gọi lại API GET /farms để cập nhật lưới.

DeleteConfirmModal (Cực kỳ quan trọng để bắt lỗi 409):

Hỏi xác nhận: "Bạn có chắc chắn muốn xóa nông trại này không?".

Gọi DELETE /api/v1/farms/{farmId}.

Đặc biệt chú ý mã 409 (Conflict): Như API Docs mô tả, nếu vườn còn thiết bị hoặc Rule, Backend sẽ trả 409. Frontend phải bắt lỗi này và hiển thị Toast màu đỏ: "Không thể xóa: Nông trại hiện vẫn còn thiết bị hoặc tự động hóa đang liên kết".

2. Trang Chi tiết Nông trại (/farms/:farmId) - Container
Đây là trang mà Nông dân "sống" trên đó hàng ngày.

Kiến trúc: Bọc trong một Tabs Component (gồm 4 tab: Tổng quan, Biểu đồ, Tự động hóa, AI). farmId được lấy từ URL params (react-router).

3. Tab 1: Tổng quan & Thiết bị (Overview & Devices) - TRÁI TIM REALTIME
Nơi đây hiển thị tình trạng các cảm biến và máy bơm.

Khởi tạo State & Mạng lưới:

Gọi API GET /api/v1/farms/{farmId}/devices. Lưu vào state devices.

Khởi tạo WebSocket Connection: Ngay khi Tab này mở, Frontend tạo kết nối STOMP/SockJS lắng nghe kênh /topic/farm/{farmId}/telemetry.

Phân chia giao diện: Dùng hàm filter để tách devices thành 2 mảng: sensors (Cảm biến) và actuators (Máy bơm/Đèn), hiển thị thành 2 phần rõ rệt.

Component SensorCard (Cảm biến):

Giao diện nhỏ gọn. Có icon (Nhiệt kế, Giọt nước) tùy thuộc vào metric_type.

Badge trạng thái: ONLINE (Xanh lá) hoặc OFFLINE (Xám).

Logic Realtime: Khi có tin nhắn từ WebSocket bắn về khớp với deviceId của thẻ này, lập tức lấy value mới đè lên value cũ trên màn hình kèm theo hiệu ứng chớp sáng nhẹ (Flash effect) để user biết dữ liệu vừa nhảy.

Component ActuatorCard (Thiết bị thực thi - Bơm/Đèn):

Thẻ này phức tạp hơn. Nó gồm: Tên, Trạng thái Online/Offline, và Bảng điều khiển.

ModeToggle (Công tắc Tự động/Thủ công):

Switch Component (ON/OFF) đọc giá trị operating_mode.

Khi bấm, gọi API PATCH /api/v1/devices/{deviceId}/mode. Set isLoading cho cái switch trong lúc đợi API trả về 200.

CommandButton (Nút Bấm Gửi Lệnh ON/OFF):

Nút này to, rõ. Nút chuyển màu Xanh (khi đang ON), Xám (khi đang OFF).

Nếu operating_mode === 'AUTO', làm mờ (disable) nút này hoặc khi bấm vào hiển thị tooltip cảnh báo.

Khi bấm, gọi POST /api/v1/devices/{deviceId}/command với payload { "command": "ON" }.

Bắt lỗi 400 Bad Request: Đề phòng Nông dân cố tình dùng Postman hoặc lách UI bấm nút khi đang ở AUTO, Frontend phải bắt được mã 400 và văng thông báo: "Vui lòng chuyển sang Thủ Công (MANUAL) trước khi tự điều khiển".

Component AddDeviceModal (Xin cấp thiết bị):

Khi bấm "Thêm thiết bị", mở Popup.

Popup này phải gọi API GET /api/v1/device-models để lấy danh sách model, nhét vào một cái Select Dropdown.

Nông dân chọn Model -> Nhập Tên -> Submit gọi POST /api/v1/devices/requests.

Backend trả về 201 với status: "PENDING". Frontend load lại danh sách, thẻ thiết bị mới sẽ có nhãn "Đang chờ duyệt", không thể điều khiển được.

4. Tab 2: Biểu đồ Thống kê (Telemetry Analytics)
Nơi vẽ biểu đồ cho Cảm biến.

Component TelemetryFilterBar (Bộ lọc):

Dropdown 1 (Bắt buộc): Chọn Cảm biến (Chỉ lấy các device trong farms có device_type === 'SENSOR').

Dropdown 2: Bộ chọn thời gian (Start Time, End Time). Mặc định set là "24 giờ qua".

Dropdown 3: Độ chia (aggregate): 15 phút, 1 giờ, 1 ngày.

Nút "Xem Biểu đồ".

Flow lấy dữ liệu & Vẽ:

Khi bấm Xem biểu đồ, gọi API GET /api/v1/devices/{deviceId}/telemetry?start_time=...&end_time=...&aggregate=....

Dữ liệu trả về là một mảng: [{ time: "...", value: 32.5 }, ...].

Frontend sử dụng thư viện Chart.js (hoặc Recharts):

Map time vào trục X (Ox). Format lại thành dạng giờ/phút cho dễ nhìn.

Map value vào trục Y (Oy).

Xử lý lỗi 400: Nếu Nông dân chọn Start Time lớn hơn End Time, API trả 400. Frontend bắt details và báo Toast lỗi: "Thời gian bắt đầu không thể lớn hơn thời gian kết thúc."

1. Tab Tự động hóa (/farms/:farmId - Tab 3)
Nơi Nông dân thiết lập "bộ não" cho vườn của mình.

States nội bộ: rules (mảng chứa RuleResponse), isRuleModalOpen, editingRule (lưu data của Rule đang được sửa), devices (danh sách thiết bị - lấy từ Global State hoặc truyền từ component cha xuống).

Component RuleList (Bảng danh sách Luật):

Gọi API GET /api/v1/farms/{farmId}/rules khi mount.

Hiển thị Bảng (Table) với các cột: * Tên Luật (rule_name).

Điều kiện kích hoạt: Render logic bằng chữ cho user dễ đọc. (VD: Nếu rule_type === 'CONDITION' thì in ra: "Độ ẩm đất < 40%". Nếu SCHEDULE thì in ra: "Mỗi ngày lúc 06:00").

Hành động thực thi: "BẬT Máy bơm hồ 1".

Cột Trạng thái (Quan trọng nhất): Chứa Component RuleToggleSwitch.

Cột Hành động: Nút Sửa, Xóa.

Logic RuleToggleSwitch: Component công tắc (On/Off).

Khi user bấm, gọi API PATCH /api/v1/rules/{ruleId}/toggle với payload { "is_active": !currentStatus }.

UX Tip: Áp dụng "Optimistic Update" (Đổi màu công tắc trên UI ngay lập tức cho mượt, nếu API trả về lỗi 400/500 thì mới giật công tắc về lại vị trí cũ và báo lỗi).

Logic Xóa: Gọi DELETE /api/v1/rules/{ruleId}. Hiện Modal xác nhận trước khi xóa.

Component RuleFormModal (Dynamic Form - Đỉnh cao độ khó):

Modal dùng chung cho cả Tạo mới (POST /api/v1/rules) và Cập nhật (PUT /api/v1/rules/{ruleId}).

Trường cố định 1: rule_name (Text Input - Bắt buộc).

Trường cố định 2: rule_type (Radio Button hoặc Select: CONDITION (Theo cảm biến) hoặc SCHEDULE (Theo giờ)).

KHU VỰC ĐỘNG (Dynamic Fields) - Kích hoạt bởi rule_type:

Trường hợp chọn CONDITION: * Hiển thị Dropdown trigger_device_id. Frontend phải tự filter mảng devices chỉ lấy những thằng có device_type === 'SENSOR' cho user chọn.

Hiển thị Dropdown operator (>, <, ==).

Hiển thị Input threshold_value (Kiểu Number).

Trường hợp chọn SCHEDULE:

Ẩn toàn bộ 3 trường của CONDITION đi.

Hiển thị Input cron_expression. (UX Tip cao cấp: Thay vì bắt Nông dân nhập mã Cron khó hiểu như 0 6 * * *, Frontend nên code một Time Picker (Chọn giờ/phút) rồi ngầm tự build ra chuỗi Cron gửi xuống Backend).

KHU VỰC HÀNH ĐỘNG (Action Fields):

Dropdown action_device_id: Frontend filter mảng devices chỉ lấy device_type === 'ACTUATOR' (Máy bơm/Đèn).

Dropdown action_command: Chọn ON hoặc OFF.

Xử lý Validation & Lỗi: * Frontend khóa nút Submit nếu các trường bắt buộc bị trống.

Bắt lỗi 400 từ API: Nếu cố tình gửi thiết bị Action là loại SENSOR, backend sẽ chửi "Thiết bị thực thi bắt buộc phải là loại ACTUATOR". Frontend phải bắt dòng chữ này và hiện màu đỏ báo cho Nông dân.

2. Cổng Quản trị viên (Admin Portal)
Dành riêng cho User có role === 'ADMIN'. Sidebar của họ chỉ có 2 menu chính.

Trang 2.1: Quản lý Yêu cầu Thiết bị (/admin/device-requests)

Mục đích: Nơi cấp phát "Chìa khóa mạng" (Feed Key Adafruit) cho Nông dân.

Component PendingRequestsTable:

Gọi GET /api/v1/admin/devices/requests?status=PENDING.

Hiển thị dạng bảng. Mỗi dòng (Row) là một đơn xin thiết bị gồm: farm_id (Tên Nông trại), name (Tên Nông dân đặt), model_id (Loại mẫu).

Cột cuối cùng có 2 nút: Duyệt (Xanh) và Từ chối (Đỏ).

Component ApproveDeviceModal:

Khi bấm Duyệt, mở popup này.

Hiển thị thông tin thiết bị đang duyệt.

Trường nhập liệu: adafruit_feed_key (Text Input). Bắt buộc Admin phải lên Adafruit tạo Feed trước rồi paste tên vào đây (VD: yolofarm/feeds/pump-01).

Submit gọi POST /api/v1/admin/devices/{deviceId}/approve.

Thành công: API trả về 200, Frontend đóng popup, bắn Toast "Cấp phát thành công", và xóa dòng đó khỏi PendingRequestsTable.

Component RejectDeviceModal:

Khi bấm Từ chối, mở popup.

Trường nhập liệu: reject_reason (Text Area - Bắt buộc). Yêu cầu Admin giải thích lý do (VD: Hết hàng, Nông dân dùng gói Free không được thêm).

Submit gọi POST /api/v1/admin/devices/{deviceId}/reject.

Logic Phân quyền (Security Guard): Nếu Frontend phát hiện một user có role === 'FARMER' mà cố tình gõ URL /admin/... vào trình duyệt, lập tức đá văng ra màn hình /farms hoặc trang lỗi 403 Forbidden. Đón đầu bắt lỗi 403 từ API ("Access Denied").

Trang 2.2: Quản lý Catalog Thiết bị (/admin/device-models)

Mục đích: Nhập kho các loại Cảm biến/Bơm mới.

Component ModelTable:

Gọi API GET /api/v1/device-models. Liệt kê danh sách mẫu (VD: Cảm biến độ ẩm đất v1, Bơm chìm 220V...).

Component ModelFormModal:

Form chứa 4 trường: model_name (Text), device_type (Select: SENSOR/ACTUATOR), metric_type (Select: TEMP, SOIL_MOISTURE, PUMP...), manufacturer (Text).

Submit gọi API POST /api/v1/admin/device-models. Load lại bảng sau khi lưu thành công.