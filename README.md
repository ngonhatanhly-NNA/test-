# 🏷️ Team 13 Bidding System - Enterprise Online Auction Platform

Một hệ thống đấu giá trực tuyến được xây dựng dựa trên kiến trúc **N-Tier (Đa tầng)** và **Multi-module Maven**, tuân thủ nghiêm ngặt các nguyên tắc thiết kế phần mềm hướng đối tượng (OOP), SOLID và vận dụng đa dạng các Design Patterns. Dự án tập trung vào khả năng mở rộng, quản lý trạng thái thời gian thực (real-time), và bảo mật luồng dữ liệu.

## 👥 Thành viên nhóm 13

| STT | Họ và Tên | Mã Sinh Viên | Vai trò (Role) |
|:---:|:---|:---|:---|
| 1 | Ngô Nhật Ánh | 25020030 | Main FrontEnd - Backend Auth, Auction, User - DTO net, DB set up and Net setup - Configuration |
| 2 | Đinh Anh Vũ | 25020432 | Frontend - Backend Item, User - DTO net - DB setup| 
| 3 | Hoàng Văn Tưởng | 25020383 | Backend Admin - Seller - Bidder |
| 4 | Mai Thế Phong | 25020310 |Admin FrontEnd -  Backend Admin - Seller - Bidder |

---

## Công nghệ sử dụng (Tech Stack)

* **Ngôn ngữ lõi:** Java 25
* **Giao diện Frontend:** JavaFX, FXML, CSS, SceneBuilder
* **Cơ sở dữ liệu:** MySQL 8.x, JDBC Driver (v9.6.0)
* **Giao tiếp mạng (Networking):** WebSocket, REST API (HTTP) (Javalin)
* **Quản lý dự án & Build tool:** Maven
* **Định dạng dữ liệu:** JSON (Đóng/mở gói DTO qua thư viện Gson)
* **IDE Khuyên dùng:** IntelliJ IDEA

---

## ⚙️ Hướng dẫn Cài đặt & Khởi chạy (Getting Started)

Dự án sử dụng mô hình Client-Server, do đó bạn cần khởi chạy Database, Server, sau đó mới bật Client.

### 1. Yêu cầu hệ thống 
* Java Development Kit (JDK) phiên bản 25 trở lên.
* Maven đã được cài đặt và cấu hình biến môi trường.
* MySQL Server (Chạy ở cổng mặc định `3306`).

### 2. Khởi tạo Cơ sở dữ liệu (Database Setup)
1. Mở MySQL Workbench hoặc CLI.
2. Tạo một schema mới cho dự án.
3. Chạy file script SQL được đính kèm trong source code: `script_db.sql` để tự động tạo cấu trúc các bảng (`Users`, `Bidder`, `Seller`, `Admin`, `Items`,...).
4. Mở file `Bidding_Server/src/main/java/com/server/config/DBConnection.java` và điều chỉnh lại `URL`, `USERNAME`, `PASSWORD` sao cho khớp với MySQL cục bộ của bạn.

### 3. Build Dự án với Maven
Mở terminal tại thư mục gốc của dự án (nơi chứa file `pom.xml` tổng) và chạy lệnh:
```bash
mvn clean install



```
 Server sẽ chạy ở:
- REST: http://localhost:7070
- WebSocket: ws://localhost:8080

## 📋 Bảng Theo Dõi Tiến Độ Công Việc (Team 13)

| Nhóm chức năng | Tên công việc / Tính năng | Trạng thái | Người phụ trách |
| :--- | :--- | :---: | :--- |
| **1. Khởi tạo & Kiến trúc** | Thiết lập kiến trúc Multi-module Maven (shared, client, server) | ✅ Hoàn thành | Ngô Nhật Ánh |
| | Thiết kế hệ thống Entity & Data Models cốt lõi | ✅ Hoàn thành | Ngô Nhật Ánh, Đinh Anh Vũ |
| | Thiết lập CSDL MySQL & cấu hình kết nối Singleton JDBC | ✅ Hoàn thành | Ngô Nhật Ánh, Đinh Anh Vũ |
| | App Config, các dữ liệu kiểu tài khoản, mật khẩu | ❌ Chưa làm | Ngô Nhật Ánh |
| **2. Xác thực & Người dùng** | UI/UX màn hình Đăng ký & Đăng nhập (JavaFX) | ✅ Hoàn thành | Ngô Nhật Ánh |
| | Xử lý logic kết nối DB cho Đăng ký/Đăng nhập qua class Base User | ✅ Hoàn thành | Ngô Nhật Ánh, Đinh Anh Vũ |
| | Middleware phân quyền (AuthZ) bảo vệ API (VD: bắt buộc login, check role) | ❌ Chưa làm | Phong Tưởng |
| | Logic Bidder, Admin, Service và Repository | ✅ Hoàn thành | Phong, Tưởng |
| | Quản lý ví (Wallet), nạp/rút tiền, và thanh toán/settlement | ⚠️ Đang làm | Phong Tưởng |
| | Tính năng phụ: Quên mật khẩu, xác thực email, Ban/Unban tài khoản | ❌ Chưa làm | Phong Tưởng |
| **3. Quản lý Sản phẩm (Item)**| Thiết kế giao diện Sảnh chính (Dashboard / Auction Menu) | ✅ Hoàn thành | Ngô Nhật Ánh, Đinh Anh Vũ |
| | Tính năng tạo phiên đấu giá (Create Item) & hiển thị danh sách Live | 🔄 Đang làm | Ngô Nhật Ánh, Đinh Anh Vũ |
| | UI nhập thuộc tính riêng biệt theo từng loại Item (Electronics, Art, Vehicle) | ❌ Chưa làm | Đinh Anh Vũ |
| | Quản lý upload và hiển thị ảnh thực tế (ImageController) | ❌ Chưa làm | Đinh Anh Vũ, Phong |
| **4. Nghiệp vụ Đấu giá** | Thiết lập Socket 2 chiều Client-Server để truyền tải DTO | 🔄 Đang làm | Ngô Nhật Ánh, Đinh Anh Vũ |
| | Tích hợp Observer Pattern để broadcast cập nhật giá Real-time | 🔄 Đang làm | Ngô Nhật Ánh |
| | Xử lý thuật toán Đấu giá tự động (Auto-bidding) & Chống cướp giờ chót | ❌ Chưa làm | Ngô Nhật Ánh |
| | Xử lý đấu giá đồng thời (Concurrent bidding) | ❌ Chưa làm | Ngô Nhật Ánh |
| | Code luật giá sàn (ReservePriceValidation) | ❌ Chưa làm | Ngô Nhật Ánh |
| | Cơ chế khôi phục trạng thái đấu giá (Cache vs DB) khi Server restart | ❌ Chưa làm | Ngô Nhật Ánh |
| **5. Chức năng Admin** | Xây dựng API lấy danh sách User (getAllUsers) và lịch sử hoạt động | ❌ Chưa làm | Đinh Anh Vũ |
| | Chức năng hủy phiên đấu giá (cancel auction) và duyệt người bán | ❌ Chưa làm | Phong Tưởng |
| | Parse dữ liệu và hiển thị lên giao diện AdminDashboardController | ❌ Chưa làm | Đinh Anh Vũ, Nhật Ánh, Phong, Tưởng |
| **6. Vận hành & CI/CD** | Cấu hình GitHub Actions (build-test, SonarCloud, Docker workflow) | 🔄 Đang làm | Ngô Nhật Ánh |
| | Sửa lỗi test Mockito, cấu hình JaCoCo report, bật test mặc định | ❌ Chưa làm | Phong Tưởng |
| | Bổ sung Logging đồng bộ (correlation id), check endpoint (/health) | ❌ Chưa làm | Ngô Nhật Ánh |
| | Đóng gói ứng dụng và chạy, kiểm thử: Docker, CI/CD, Github Actions | ❌ Chưa làm | Ngô Nhật Ánh |
| **7. JUnit + Refactor** | Thiết kế test cho chức năng đấu giá, Item | ⚠️ Nửa vời | Phong |
| | Refactor project tuân thủ Builder Pattern và Prototype Pattern | ⚠️ Nửa vời | Đinh Anh Vũ |

> **Chú thích trạng thái:**
> * ✅ **Hoàn thành:** Đã code xong và chạy ổn định.
> * 🔄 **Đang làm:** Đang trong quá trình phát triển.
> * ⚠️ **Nửa vời / Cần update:** Đã làm một phần nhưng chưa hoàn thiện hoặc cần fix thêm.
> * ❌ **Chưa làm:** Nằm trong kế hoạch nhưng chưa bắt đầu.
## Kiến trúc hệ thống

Dự án được chia thành 3 module độc lập để đảm bảo Tách biệt mối quan hệ**:

### 1️⃣ Module `shared` 
Đóng vai trò là thư viện dùng chung giữa Client và Server.
* **DTOs & Models:** Định nghĩa các lớp Entity cơ sở. Hệ thống quản lý tài khoản được xây dựng trên một lớp base là `User` (xử lý toàn bộ logic Đăng ký / Đăng nhập), từ đó phân cấp (downcasting) linh hoạt thành các role `Bidder`, `Seller`, và `Admin`.
* **Network Components:** Chứa các định dạng `Request` và `Response` chuẩn hóa để đóng gói dữ liệu truyền qua Socket/HTTP.

### 2️⃣ Module `Bidding_Server` (Backend & Database)
Trung tâm xử lý nghiệp vụ, giao tiếp với MySQL và quản lý kết nối đồng thời. Tuân thủ mô hình **MVC Server-side**.
* **Controller (Network Layer):** Nhận luồng byte/JSON từ Socket, phân giải thành Command/DTO và điều hướng.
* **Service (Business Logic):** Chứa các thuật toán cốt lõi độc lập với DB:
  * Xử lý đấu giá đồng thời (Concurrent Bidding).
  * Chống cướp giờ chót (Anti-sniping) và Đấu giá tự động (Auto-Bidding).
* **Repository/DAO :** Điểm duy nhất chứa SQL. Áp dụng cơ chế kết nối Singleton (`DBConnection`) để tối ưu hóa Connection Pool, chống quá tải hệ thống.
* **WebSocket/Broadcaster:** Quản lý danh sách Client trong phòng đấu giá, phát sóng (broadcast) trạng thái giá mới nhất theo thời gian thực (Real-time).

###  Module `Bidding_Client` (Frontend JavaFX)
Chịu trách nhiệm hiển thị giao diện và tương tác người dùng theo mô hình **MVC**.
* **Controller:** Bắt sự kiện UI (`LoginController`, `DashboardController`), xử lý luồng chuyển cảnh (`SceneController`) và hiệu ứng (`SpriteAnimation`).
* **Session Management:** `UserSession` lưu trữ ID phiên làm việc an toàn tại máy khách, tránh việc phải đăng nhập lại liên tục.
* **Network/Sender:** Trình đóng gói UI thành JSON, nén thành byte và gửi qua kênh Socket TCP đến Server. Đồng thời lắng nghe WebSocket để tự động cập nhật UI khi có người đặt giá.

---

## Design Patterns Áp Dụng

Hệ thống linh hoạt áp dụng các Mẫu thiết kế phần mềm để giải quyết các bài toán kỹ thuật phức tạp:
* **Singleton:** Quản lý duy nhất một luồng `DBConnection` và `Broadcaster`.
* **Observer:** Phát sóng thời gian thực cập nhật mức giá đến toàn bộ người dùng đang xem phiên đấu giá mà không cần tải lại trang.
* **Strategy:** Cho phép cấu hình động các thuật toán như luật chống snipe (`AntiSnipingStrategy`), xác thực giá thầu (`BidValidationStrategy`), và render UI theo Role.
* **Factory / Builder:** Khởi tạo các gói cấu hình phức tạp (Item, Auction) và DTO mạng.
* **Command:** Đóng gói các yêu cầu từ Client thành các đối tượng lệnh (e.g., `UpdateProfileCommand`) để xử lý tuần tự hoặc đa luồng trên Server.
* **State:** Quản lý vòng đời khép kín của một phiên đấu giá (PENDING, ACTIVE, ENDED).

---

## 🚀 Tiến độ dự án (Roadmap)

### Giai đoạn 1: Khởi tạo & Kiến trúc (Hoàn thành)
- [x] Thiết lập kiến trúc Multi-module Maven (`shared`, `client`, `server`).
- [x] Thiết kế hệ thống Entity & Data Models cốt lõi.
- [x] Xây dựng cơ sở dữ liệu MySQL và cấu hình Singleton JDBC.

### Giai đoạn 2: Xác thực & UI Cơ bản (Hoàn thành)
- [x] Xây dựng UI/UX màn hình Đăng nhập & Đăng ký (JavaFX).
- [x] Thiết kế giao diện Sảnh chính (Dashboard / Auction Menu).
- [x] Hoàn thiện luồng kết nối DB cho tính năng Register/Login qua Base `User`.

### Giai đoạn 3: Nghiệp vụ & Thời gian thực (Đang phát triển)
- [ ] Hoàn thiện kết nối Socket 2 chiều Client - Server cho việc truyền tải DTO.
- [ ] Tích hợp Observer Pattern để broadcast giá đấu thời gian thực.
- [ ] Phát triển tính năng tạo phiên đấu giá (Create Item) và hiển thị danh sách live.

### Giai đoạn 4: Tối ưu & Mở rộng (Sắp tới)
- [ ] Hoàn thiện thuật toán Anti-sniping và Auto-bidding.
- [ ] Kiểm thử đồng thời (Concurrency testing).
