# Lộ trình 3 tuần hoàn thiện dự án (App + Backend + CI/CD)

> Mục tiêu: trong 3 tuần đưa dự án về trạng thái **ổn định – đầy đủ use-case chính – có CI/CD chuẩn trên GitHub Actions**.

## 1) Các tính năng còn thiếu / đang chưa hoàn thiện (gap list)

### 1.1. Admin (Server + Client)
- **`GET /api/admin/users`**: `AdminController#getAllUsers()` đang `TODO` (trả dữ liệu placeholder).
- **User activity log**: `GET /api/admin/users/:username/activity` đang `TODO`.
- **Cancel auction**: `POST /api/admin/auctions/cancel` đang `TODO`.
- **Approve seller**: hiện nhận `shopName/bankAccount` nhưng chưa thấy lưu rõ ràng theo payload (cần đồng bộ với `SellerRepository`).
- **Client Admin Dashboard** (`AdminDashboardController`) còn `TODO` parse & populate UI.

### 1.2. Auction/Bid
- **Reserve price**: `ReservePriceValidation` đang trống → thiếu luật giá sàn.
- **Phân quyền/authorization**:
  - Chưa có middleware bắt buộc login/role cho nhóm `/api/admin/*`, `/api/items` (create), `/api/auctions` (create)…
  - Hiện mới có session `ctx.sessionAttribute("username")` sau login, cần dùng để authorize.
- **Độ bền cache vs DB**: `AuctionService` có cache + scheduler; cần đảm bảo khi restart server thì phục hồi state phiên (đang ACTIVE/PENDING) từ DB và reschedule.

### 1.3. Item
- UI tạo item còn thiếu phần nhập thuộc tính riêng theo loại (Electronics/Art/Vehicle), hiện tạo `extraProps` rỗng và fix cứng condition.
- Quản lý ảnh: `ImageController` trống → chưa có upload/serve ảnh thật (hiện dùng url/list text).

### 1.4. User / Wallet / Payment
- DB có `walletBalance`, `creditCardInfo`, `bankAccountNumber`, `isVerified` nhưng **chưa có luồng nạp/rút tiền**, escrow, hoặc settlement khi auction end.
- Chưa có:
  - reset password / đổi mật khẩu UI,
  - email verification,
  - quản lý trạng thái tài khoản (ban/unban) thể hiện rõ trên client.

### 1.5. Observability & vận hành
- Logging còn rời rạc; chưa có correlation id / request id.
- Chưa có healthcheck endpoint (`/health`), readiness/liveness.

---

## 2) Gợi ý update trong tương lai (để “hoàn hảo” hơn)

### 2.1. Nâng cấp bảo mật
- Tách rõ **AuthN (login)** và **AuthZ (role check)**:
  - Middleware cho Javalin: `before("/api/admin/*", ...)` bắt buộc role ADMIN.
  - Trả lỗi chuẩn `Response(ERROR, errorCode=UNAUTHORIZED/FORBIDDEN)`.
- Hash mật khẩu: nếu chưa dùng BCrypt/Argon2 → nâng cấp.

### 2.2. Chuẩn hoá API contract
- Thống nhất response: luôn trả JSON `Response` kể cả khi lỗi 500.
- Chuẩn hoá DTO update profile: server lấy username từ session, client không phải truyền `username`/`id` nhạy cảm.

### 2.3. Auction engine
- Hoàn thiện:
  - reserve price,
  - anti-sniping config theo auction,
  - settlement khi kết thúc (winner, payment, transaction record),
  - auto-bid rule rõ ràng.

### 2.4. UI/UX
- Reconnect WebSocket + backoff.
- Loading state / retry / error detail rõ ràng.
- Admin dashboard có table/filter/sort.

---

## 3) Lộ trình 3 tuần (đề xuất)

### Tuần 1 — Ổn định nền tảng + CI/CD “xanh”
**Mục tiêu:** build/test pass trên local + GitHub Actions, có quality gate cơ bản.

1) **Fix build & test**
- Bật test mặc định (không để cấu hình skip test trong POM).
- Ổn định Mockito trên JDK mới (không phụ thuộc attach agent).
- Thiết lập JaCoCo report cho Server (và dần cho Client nếu có test).

2) **Chuẩn hoá GitHub Actions**
- 1 workflow chính: `build-test` (mvn clean verify, upload surefire + jacoco artifact).
- `code-quality` chạy SonarCloud (khi có `SONAR_TOKEN`).
- Docker workflow để `workflow_dispatch` cho đến khi có Dockerfiles và fat-jar.

3) **Dev ergonomics**
- Thêm `README` runbook: cách chạy DB + server + client.
- Thêm `.editorconfig`/formatting (tuỳ nhóm).

**Deliverables tuần 1**
- CI xanh (PR/push đều chạy), có artifact test + coverage.
- Có checklist “Definition of Done” cho PR.

### Tuần 2 — Hoàn thiện tính năng lõi (User/Item/Auction/Admin)
**Mục tiêu:** end-to-end use-case chạy hoàn chỉnh, dữ liệu thật.

1) **Admin features**
- Implement `getAllUsers`, `user activity`, `cancel auction`.
- Client AdminDashboard: parse response + bind table + actions.

2) **Auction**
- Implement `ReservePriceValidation` + test.
- Khôi phục state từ DB khi restart (load ACTIVE auctions và reschedule end-time).

3) **Item & Profile UI**
- UI nhập thuộc tính theo loại item.
- Đồng bộ session ở tất cả network client (không dùng HttpClient rời nếu cần cookie).

**Deliverables tuần 2**
- Demo scenario đầy đủ: đăng ký → login → tạo item → tạo auction → bid (manual/auto) → realtime update → admin can cancel.

### Tuần 3 — Hoàn thiện “production-like”: packaging, release, quality gate, deploy
**Mục tiêu:** có bản phát hành (release) + pipeline chuẩn, có thể deploy/test dễ.

1) **Packaging**
- Server: tạo **fat-jar** (maven-shade hoặc assembly) để chạy 1 file.
- Client: đóng gói JavaFX (jlink/jpackage) hoặc phát hành jar + hướng dẫn.

2) **Docker/Deploy (tuỳ chọn)**
- Thêm `Dockerfile.server` chạy fat-jar.
- (Client thường không chạy trong container; thay bằng build artifact release.)
- Optional: docker-compose cho MySQL + server.

3) **Quality gate**
- SonarCloud: bật quality gate, fail PR nếu coverage/bugs vượt ngưỡng.
- Thêm lint/static analysis: SpotBugs/Checkstyle/PMD hoặc Spotless.

4) **Release workflow**
- Khi tag `vX.Y.Z`:
  - build → test → package → upload artifacts (Server jar, Client package) → tạo GitHub Release.

**Deliverables tuần 3**
- Release “v1.0” có artifact đầy đủ.
- Tài liệu triển khai + demo script.


