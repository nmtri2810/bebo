# bebo — Better Boyfriend

## 1. Tổng quan dự án

**bebo** là tên viết tắt của **Better Boyfriend**. Đây là một web application đơn giản giúp người dùng:

1. Ghi nhận ngày bắt đầu chu kỳ gần nhất.
2. Dự đoán ngày bắt đầu chu kỳ tiếp theo.
3. Nhận thông báo trước ngày dự kiến qua Telegram.
4. Cập nhật lại dữ liệu khi một chu kỳ mới bắt đầu.

Ứng dụng lấy cảm hứng từ tính năng Cycle Tracking trên Apple Health nhưng được tối giản, tập trung vào hai mục tiêu chính:

- Xác định chu kỳ tiếp theo.
- Gửi thông báo cho người dùng.

### Ý nghĩa tên gọi

**bebo** được viết tắt từ **Better Boyfriend**. Tên gọi thể hiện mục tiêu của sản phẩm:
giúp người dùng chủ động ghi nhớ thời điểm chu kỳ dự kiến và quan tâm bạn đời tốt hơn,
thay vì dự đoán hoặc đánh giá cảm xúc của họ.

Dự án đồng thời được sử dụng để học và thực hành:

- Spring Boot
- Next.js (chỉ phục vụ mục đích có FE)
- PostgreSQL
- REST API
- Authentication
- Scheduled jobs
- Telegram Bot API
- Docker
- GitHub Actions
- CI/CD
- Deployment
- Kubernetes ở giai đoạn sau

---

## 2. Mục tiêu sản phẩm

### Mục tiêu chính

Xây dựng một hệ thống chạy thực tế trên production, trong đó người dùng có thể nhập dữ liệu chu kỳ và nhận thông báo tự động trước ngày dự kiến.

### Mục tiêu kỹ thuật

- Có môi trường development và production.
- Backend sử dụng Spring Boot.
- Frontend sử dụng Next.js.
- Database sử dụng PostgreSQL.
- Backend và frontend giao tiếp qua REST API.
- Có xác thực người dùng.
- Có scheduled job tự động kiểm tra reminder.
- Tích hợp Telegram để gửi thông báo.
- Đóng gói ứng dụng bằng Docker.
- Thiết lập CI/CD bằng GitHub Actions.
- Deploy ứng dụng lên server.
- Có thể triển khai Kubernetes sau khi phiên bản Docker hoạt động ổn định.

---

## 3. Đối tượng sử dụng

Phiên bản MVP hướng tới một người dùng cá nhân muốn theo dõi chu kỳ của người yêu hoặc bạn đời và nhận lời nhắc trước ngày dự kiến.

Ứng dụng không nhằm mục đích:

- Chẩn đoán sức khỏe.
- Dự đoán cảm xúc.
- Tư vấn tránh thai.
- Phát hiện bệnh lý.
- Thay thế tư vấn y tế.

Ngày chu kỳ tiếp theo chỉ là kết quả ước tính dựa trên dữ liệu người dùng đã nhập.

---

## 4. Phạm vi MVP

MVP chỉ bao gồm các chức năng cần thiết nhất.

### 4.1. Authentication

Người dùng có thể:

- Đăng ký tài khoản.
- Đăng nhập.
- Đăng xuất.
- Truy cập dữ liệu của chính mình.

Sử dụng JWT Authentication

### 4.2. Thiết lập chu kỳ

Người dùng nhập:

- Ngày bắt đầu kỳ gần nhất.
- Độ dài chu kỳ mặc định.
- Số ngày muốn được nhắc trước.
- Giờ muốn nhận thông báo.
- Múi giờ.

Ví dụ:

```text
Ngày bắt đầu gần nhất: 01/07/2026
Độ dài chu kỳ mặc định: 28 ngày
Nhắc trước: 3 ngày
Giờ nhận thông báo: 08:00
Múi giờ: Asia/Ho_Chi_Minh
```

Hệ thống tính:

```text
Kỳ tiếp theo dự kiến: 29/07/2026
Ngày gửi reminder: 26/07/2026
```

### 4.3. Ghi nhận chu kỳ mới

Khi một chu kỳ mới bắt đầu, người dùng nhập ngày bắt đầu mới.

Hệ thống sẽ:

1. Lưu bản ghi mới.
2. Tính độ dài của chu kỳ vừa kết thúc.
3. Tính lại độ dài chu kỳ trung bình.
4. Cập nhật ngày dự kiến tiếp theo.
5. Hủy hoặc bỏ qua reminder cũ chưa gửi.
6. Tạo reminder mới.

### 4.4. Dashboard

Dashboard hiển thị:

- Ngày bắt đầu kỳ gần nhất.
- Ngày dự kiến kỳ tiếp theo.
- Số ngày còn lại.
- Độ dài chu kỳ trung bình.
- Số ngày nhắc trước.
- Trạng thái kết nối Telegram.
- Nút cập nhật chu kỳ mới.
- Nút chỉnh sửa cài đặt.

Ví dụ:

```text
Kỳ tiếp theo dự kiến

29/07/2026

Còn 11 ngày

Chu kỳ trung bình: 28 ngày
Thông báo trước: 3 ngày
Telegram: Đã kết nối
```

### 4.5. Lịch sử chu kỳ

Hiển thị danh sách các ngày bắt đầu đã nhập.

| Lần | Ngày bắt đầu | Độ dài chu kỳ |
| --- | ------------ | ------------: |
| 1   | 01/06/2026   |       30 ngày |
| 2   | 01/07/2026   |       27 ngày |
| 3   | 28/07/2026   | Chưa xác định |

Không cần calendar phức tạp trong MVP.

### 4.6. Telegram notification

Người dùng có thể:

- Kết nối tài khoản với Telegram bot.
- Gửi thử một notification.
- Bật hoặc tắt Telegram.
- Nhận thông báo trước kỳ dự kiến.

Ví dụ:

```text
bebo

Kỳ tiếp theo được dự kiến bắt đầu sau khoảng 3 ngày, vào 29/07/2026.

Đây là ngày ước tính dựa trên các chu kỳ đã nhập.
```

Messenger chưa nằm trong MVP. Chỉ triển khai sau khi Telegram hoạt động ổn định.

---

## 5. Luồng sử dụng chính

```text
Đăng ký / đăng nhập
        ↓
Nhập ngày bắt đầu kỳ gần nhất
        ↓
Nhập độ dài chu kỳ mặc định
        ↓
Chọn số ngày muốn được nhắc trước
        ↓
Kết nối Telegram
        ↓
Xem ngày chu kỳ tiếp theo trên dashboard
        ↓
Nhận thông báo tự động
        ↓
Nhập ngày mới khi chu kỳ tiếp theo bắt đầu
```

---

## 6. Quy tắc tính chu kỳ

### 6.1. Khi chỉ có một bản ghi

Khi người dùng mới sử dụng và chỉ có một ngày bắt đầu:

```text
Ngày dự kiến tiếp theo =
Ngày bắt đầu gần nhất + Độ dài chu kỳ mặc định
```

Ví dụ:

```text
Ngày bắt đầu gần nhất: 01/07/2026
Độ dài mặc định: 28 ngày
Ngày dự kiến tiếp theo: 29/07/2026
```

### 6.2. Khi có nhiều bản ghi

Độ dài một chu kỳ được tính bằng số ngày giữa hai ngày bắt đầu liên tiếp.

```text
Độ dài chu kỳ =
Ngày bắt đầu hiện tại - Ngày bắt đầu trước đó
```

Ví dụ:

```text
01/06/2026 → 01/07/2026 = 30 ngày
01/07/2026 → 28/07/2026 = 27 ngày
```

Khi có nhiều dữ liệu, hệ thống lấy trung bình từ tối đa 6 chu kỳ gần nhất:

```text
Chu kỳ trung bình =
Tổng độ dài các chu kỳ / Số chu kỳ
```

Có thể làm tròn về số nguyên gần nhất.

```text
Ngày dự kiến tiếp theo =
Ngày bắt đầu gần nhất + Chu kỳ trung bình
```

### 6.3. Ngày gửi reminder

```text
Ngày reminder =
Ngày dự kiến tiếp theo - Số ngày nhắc trước
```

Ví dụ:

```text
Ngày dự kiến: 29/07/2026
Nhắc trước: 3 ngày
Ngày reminder: 26/07/2026
```

### 6.4. Nguyên tắc quan trọng

- Dữ liệu thực tế luôn ưu tiên hơn dữ liệu dự đoán.
- Khi người dùng nhập kỳ mới, prediction cũ phải được tính lại.
- Không gửi reminder nếu chu kỳ mới đã được ghi nhận.
- Không gửi cùng một reminder nhiều lần.
- Phải xử lý đúng timezone của người dùng.
- Prediction phải được ghi rõ là kết quả ước tính.

---

## 7. Kiến trúc hệ thống

```text
Next.js Frontend
        |
        | REST API / HTTPS
        v
Spring Boot Backend
        |
        +-- PostgreSQL
        |
        +-- Spring Security
        |
        +-- Spring Scheduler
        |
        +-- Telegram Bot API
```

### 7.1. Frontend

Công nghệ:

- Next.js
- TypeScript
- App Router
- Tailwind CSS
- Axios
- Zustand

Trách nhiệm:

- Authentication UI.
- Dashboard.
- Form nhập chu kỳ.
- Lịch sử chu kỳ.
- Cài đặt reminder.
- Kết nối Telegram.
- Gọi REST API từ backend.

### 7.2. Backend

Công nghệ:

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- Spring Scheduler
- Flyway
- PostgreSQL Driver
- JWT library

Trách nhiệm:

- Authentication.
- Quản lý người dùng.
- Quản lý cycle records.
- Tính chu kỳ trung bình.
- Tính prediction.
- Quản lý notification settings.
- Scheduled job.
- Gửi Telegram notification.
- Chống gửi trùng.
- Phân quyền dữ liệu theo user.

### 7.3. Database

Sử dụng PostgreSQL.

Migration được quản lý bằng Flyway.

### 7.4. Notification

MVP chỉ sử dụng Telegram Bot API.

Thiết kế code nên cho phép bổ sung Messenger hoặc email sau này nhưng không cần xây dựng ngay.

---

## 8. Cấu trúc backend đề xuất

```text
src/main/java/com/example/bebo
├── auth
│   ├── AuthController
│   ├── AuthService
│   └── dto
├── user
│   ├── User
│   ├── UserRepository
│   └── UserService
├── cycle
│   ├── CycleRecord
│   ├── CycleRecordController
│   ├── CycleRecordRepository
│   ├── CycleRecordService
│   ├── CyclePredictionService
│   └── dto
├── settings
│   ├── CycleSettings
│   ├── CycleSettingsController
│   ├── CycleSettingsRepository
│   └── CycleSettingsService
├── notification
│   ├── NotificationChannel
│   ├── NotificationLog
│   ├── NotificationScheduler
│   ├── NotificationService
│   └── telegram
│       ├── TelegramClient
│       ├── TelegramController
│       └── TelegramWebhookController
├── security
│   ├── SecurityConfig
│   ├── JwtAuthenticationFilter
│   └── JwtService
├── common
│   ├── exception
│   └── response
└── BeboApplication
```

Backend là modular monolith.

---

## 9. Database design

### 9.1. `users`

```text
id
email
password_hash
timezone
created_at
updated_at
```

Gợi ý kiểu dữ liệu:

```sql
id UUID PRIMARY KEY
email VARCHAR(255) UNIQUE NOT NULL
password_hash VARCHAR(255) NOT NULL
timezone VARCHAR(100) NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
```

### 9.2. `cycle_settings`

```text
id
user_id
default_cycle_length
reminder_days_before
notification_time
created_at
updated_at
```

Quy tắc:

- Mỗi user có một settings record.
- `default_cycle_length` có thể mặc định là 28.
- `reminder_days_before` có thể mặc định là 3.
- `notification_time` có thể mặc định là 08:00.

### 9.3. `cycle_records`

```text
id
user_id
start_date
cycle_length
created_at
updated_at
```

Quy tắc:

- Mỗi record biểu diễn một ngày bắt đầu chu kỳ.
- `cycle_length` được tính sau khi có bản ghi kế tiếp.
- User không được tạo hai record cùng ngày.
- Không cho phép truy cập record của user khác.

### 9.4. `notification_channels`

```text
id
user_id
channel_type
external_recipient_id
enabled
created_at
updated_at
```

Ví dụ:

```text
channel_type: TELEGRAM
external_recipient_id: Telegram chat ID
enabled: true
```

### 9.5. `notification_logs`

```text
id
user_id
cycle_record_id
notification_type
scheduled_for
sent_at
status
error_message
created_at
```

Mục đích:

- Chống gửi trùng.
- Theo dõi notification đã gửi.
- Lưu trạng thái lỗi.
- Hỗ trợ retry sau này.

---

## 10. API design

Base URL:

```text
/api
```

### 10.1. Authentication

#### Đăng ký

```http
POST /api/auth/register
```

Request:

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

#### Đăng nhập

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

Response:

```json
{
  "accessToken": "jwt-token"
}
```

### 10.2. Cycle records

#### Thêm chu kỳ mới

```http
POST /api/cycles
```

Request:

```json
{
  "startDate": "2026-07-28"
}
```

#### Xem lịch sử

```http
GET /api/cycles
```

#### Xóa một bản ghi

```http
DELETE /api/cycles/{id}
```

#### Xem prediction hiện tại

```http
GET /api/cycles/prediction
```

Response:

```json
{
  "lastPeriodStartDate": "2026-07-28",
  "averageCycleLength": 28,
  "expectedNextPeriodDate": "2026-08-25",
  "reminderDate": "2026-08-22",
  "daysRemaining": 38,
  "predictionSource": "AVERAGE_HISTORY"
}
```

Giá trị `predictionSource` có thể là:

- `DEFAULT_SETTING`
- `AVERAGE_HISTORY`

### 10.3. Settings

#### Xem settings

```http
GET /api/settings
```

#### Cập nhật settings

```http
PUT /api/settings
```

Request:

```json
{
  "defaultCycleLength": 28,
  "reminderDaysBefore": 3,
  "notificationTime": "08:00",
  "timezone": "Asia/Ho_Chi_Minh"
}
```

### 10.4. Telegram

#### Bắt đầu kết nối Telegram

```http
POST /api/notifications/telegram/connect
```

#### Xem trạng thái kết nối

```http
GET /api/notifications/telegram/status
```

#### Gửi notification thử

```http
POST /api/notifications/telegram/test
```

#### Ngắt kết nối

```http
DELETE /api/notifications/telegram
```

---

## 11. Telegram connection flow

Luồng đề xuất:

```text
Người dùng bấm Connect Telegram
        ↓
Backend tạo connection token tạm thời
        ↓
Frontend mở link Telegram Bot
        ↓
Người dùng bấm Start
        ↓
Bot nhận chat_id và connection token
        ↓
Backend liên kết chat_id với user
        ↓
Frontend hiển thị Telegram đã kết nối
```

Ví dụ deep link:

```text
https://t.me/<bot_username>?start=<connection_token>
```

Không lưu bot token trong source code.

Bot token phải được lưu trong environment variable.

---

## 12. Scheduled job

Scheduler có thể chạy mỗi giờ:

```java
@Scheduled(cron = "0 0 * * * *")
public void processCycleReminders() {
    reminderService.sendDueReminders();
}
```

Luồng xử lý:

```text
Lấy danh sách user đã bật Telegram
        ↓
Tính prediction hiện tại
        ↓
Chuyển thời gian hiện tại sang timezone của user
        ↓
Kiểm tra hôm nay có phải ngày reminder không
        ↓
Kiểm tra đã tới giờ gửi chưa
        ↓
Kiểm tra notification log để tránh gửi trùng
        ↓
Gửi Telegram message
        ↓
Lưu notification log
```

Trạng thái notification:

- `PENDING`
- `SENT`
- `FAILED`

Phiên bản đầu không cần message queue.

---

## 13. Business rules

### Cycle records

- `startDate` không được để trống.
- Không được tạo hai cycle records có cùng `startDate`.
- Không nên cho nhập ngày trong tương lai.
- Có thể cho phép sửa hoặc xóa dữ liệu nhập sai.
- Khi thêm hoặc xóa record, phải tính lại prediction.

### Settings

- `defaultCycleLength`: từ 15 đến 60 ngày.
- `reminderDaysBefore`: từ 0 đến 14 ngày.
- `notificationTime`: thời gian hợp lệ.
- `timezone`: phải là timezone hợp lệ.

### Notification

- Chỉ gửi khi channel đang enabled.
- Không gửi nếu chưa có cycle record.
- Không gửi nếu prediction đã thay đổi và reminder cũ không còn hợp lệ.
- Không gửi cùng loại reminder hai lần cho cùng một prediction.
- Nội dung phải ghi rõ đây là ngày dự kiến.

---

## 14. Giao diện frontend

### 14.1. Trang đăng nhập

- Email.
- Password.
- Nút đăng nhập.
- Link đăng ký.

### 14.2. Trang onboarding

Nếu người dùng chưa có dữ liệu:

1. Nhập ngày bắt đầu kỳ gần nhất.
2. Nhập độ dài chu kỳ mặc định.
3. Chọn số ngày nhắc trước.
4. Chọn giờ nhận notification.
5. Kết nối Telegram hoặc bỏ qua.

### 14.3. Dashboard

Các thành phần:

- Prediction card.
- Days remaining.
- Average cycle length.
- Notification status.
- Add new cycle button.
- Recent cycle history.
- Settings shortcut.

### 14.4. Cycle history

- Danh sách record theo thứ tự mới nhất.
- Hiển thị cycle length.
- Xóa record.
- Có thể thêm chỉnh sửa sau.

### 14.5. Settings

- Default cycle length.
- Reminder days before.
- Notification time.
- Timezone.
- Telegram status.
- Test notification.
- Disconnect Telegram.

---

## 15. Quyền riêng tư và bảo mật

Dữ liệu chu kỳ là dữ liệu riêng tư. MVP tối thiểu phải có:

- Password được hash bằng BCrypt hoặc Argon2.
- API yêu cầu authentication.
- Mỗi user chỉ truy cập dữ liệu của chính mình.
- Không ghi dữ liệu chu kỳ chi tiết vào application logs.
- Không commit secrets lên GitHub.
- Sử dụng HTTPS trên production.
- Có chức năng xóa cycle record.
- Có chức năng xóa tài khoản và toàn bộ dữ liệu ở phase sau.
- Notification phải ghi rõ là dự đoán.
- Có thể thêm chế độ nội dung notification riêng tư sau.

Environment variables dự kiến:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
TELEGRAM_BOT_TOKEN
FRONTEND_URL
BACKEND_URL
```

---

## 16. Môi trường phát triển

### Development

Có thể chạy bằng Docker Compose:

```text
frontend
backend
postgres
```

Có thể chạy frontend và backend trực tiếp trên máy, chỉ chạy PostgreSQL bằng Docker.

### Production

Các container:

```text
bebo-frontend
bebo-backend
bebo-postgres
reverse-proxy
```

Reverse proxy dùng Nginx.

---

## 17. Repository structure

Khuyến nghị sử dụng monorepo:

```text
bebo/
├── backend/
├── frontend/
├── infra/
│   ├── docker/
│   ├── nginx/
│   └── kubernetes/
├── .github/
│   └── workflows/
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example
└── README.md
```

---

## 18. CI/CD

### Pull request

GitHub Actions chạy:

- Backend unit tests.
- Backend build.
- Frontend lint.
- Frontend type check.
- Frontend build.

### Main branch

Khi merge vào `main`:

1. Chạy lại tests.
2. Build Docker images.
3. Push images lên container registry.
4. Deploy production.
5. Kiểm tra health endpoint.

Có thể sử dụng:

- GitHub Container Registry.
- GitHub Actions.
- VPS chạy Docker Compose.

Kubernetes chỉ thêm sau khi deployment Docker Compose ổn định.

---

## 19. Testing

### Backend unit tests

Cần test:

- Tính prediction khi chỉ có một record.
- Tính trung bình khi có nhiều records.
- Chỉ lấy tối đa 6 chu kỳ gần nhất.
- Tính reminder date.
- Không gửi reminder trùng.
- Tính toán đúng timezone.
- Tính lại prediction sau khi thêm hoặc xóa record.

### Integration tests

Cần test:

- Authentication.
- Tạo cycle record.
- Lấy prediction.
- Cập nhật settings.
- Phân quyền dữ liệu theo user.
- Notification log.

### Frontend tests

MVP có thể chỉ cần:

- Lint.
- Type check.
- Build test.
- Một số component tests quan trọng nếu còn thời gian.

---

## 20. Definition of Done cho MVP

MVP được xem là hoàn thành khi:

- Người dùng có thể đăng ký và đăng nhập.
- Người dùng có thể nhập ngày bắt đầu chu kỳ.
- Người dùng có thể xem lịch sử.
- Hệ thống tính được ngày chu kỳ tiếp theo.
- Người dùng có thể chỉnh độ dài chu kỳ và reminder.
- Người dùng có thể kết nối Telegram.
- Hệ thống gửi được notification tự động đúng ngày.
- Notification không bị gửi trùng.
- Ứng dụng chạy được bằng Docker Compose.
- Có môi trường development và production.
- GitHub Actions chạy test và build.
- Ứng dụng được deploy và truy cập qua HTTPS.
- Dữ liệu được lưu trong PostgreSQL.

---

## 21. Roadmap triển khai

### Phase 1: Project foundation

- Tạo Spring Boot project.
- Tạo Next.js project.
- Cấu hình PostgreSQL.
- Docker Compose cho development.
- Cấu hình Flyway.
- Tạo GitHub repository.

### Phase 2: Authentication

- User entity.
- Register API.
- Login API.
- JWT authentication.
- Login và register UI.

### Phase 3: Cycle tracking

- Cycle settings.
- Cycle records.
- CRUD APIs.
- Prediction service.
- Dashboard.
- Cycle history.

### Phase 4: Telegram

- Tạo Telegram bot.
- Kết nối bot với user.
- Gửi test message.
- Notification scheduler.
- Notification logs.
- Chống gửi trùng.

### Phase 5: Production

- Dockerfiles.
- Production Compose.
- Reverse proxy.
- HTTPS.
- GitHub Actions.
- Deploy lên VPS.
- Backup PostgreSQL.
- Health check.

### Phase 6: Kubernetes

Chỉ thực hiện sau khi ứng dụng production chạy ổn định:

- Kubernetes manifests.
- ConfigMap.
- Secrets.
- Deployments.
- Services.
- Ingress.
- Persistent Volume.
- CI/CD deploy lên cluster.

---

## 22. Các cải tiến sau MVP

Sau khi MVP ổn định, có thể bổ sung:

- Messenger notification.
- Calendar view.
- Retry notification.
- Monitoring và alerting.
- Kubernetes deployment.

---

## 23. Tóm tắt MVP

```text
1. Người dùng đăng ký và đăng nhập.
2. Người dùng nhập ngày bắt đầu kỳ gần nhất.
3. Người dùng nhập độ dài chu kỳ mặc định.
4. Hệ thống tính ngày chu kỳ tiếp theo.
5. Người dùng chọn số ngày muốn được nhắc trước.
6. Người dùng kết nối Telegram.
7. Scheduler tự động gửi notification.
8. Khi chu kỳ mới bắt đầu, người dùng nhập ngày mới.
9. Hệ thống tính lại prediction và reminder.
```

Nguyên tắc quan trọng nhất:

> Xây dựng phiên bản nhỏ, hoàn thành đầy đủ từ frontend, backend, database, notification, CI/CD đến production trước khi mở rộng tính năng.
