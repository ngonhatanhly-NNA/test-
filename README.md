# 🏷️ Team 13 Bidding System - Enterprise Online Auction Platform

Một hệ thống đấu giá trực tuyến được xây dựng dựa trên kiến trúc **N-Tier (Đa tầng)** và **Multi-module Maven**, tuân thủ nghiêm ngặt các nguyên tắc thiết kế phần mềm hướng đối tượng (OOP), SOLID và vận dụng đa dạng các Design Patterns. Dự án tập trung vào khả năng mở rộng, quản lý trạng thái thời gian thực (real-time), và bảo mật luồng dữ liệu.

## 👥 Thành viên nhóm 13

| STT | Họ và Tên | Mã Sinh Viên | Vai trò (Role) |
|:---:|:---|:---|:---|
| 1 | Ngô Nhật Ánh | 25020030 | Main FrontEnd - Backend Auth, Auction, User - DTO net, DB set up and Network setup - Configuration |
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
## Bảng Đánh Giá Chi Tiết Dự Án

| Nội dung đánh giá | Điểm | Mức | Thành viên tham gia                                                                       | Mức độ đóng góp (%) |
| :--- | :---: | :---: |:------------------------------------------------------------------------------------------| :---: |
| **1. Thiết kế lớp và cây kế thừa** | | |                                                                                           | |
| Xác định và triển khai các lớp chính (User, Bidder/Seller/Admin, Item, Auction, BidTransaction,...) | 0.5 | Bắt buộc | Cả nhóm                                                                                   | |
| Áp dụng đúng các nguyên tắc OOP (Encapsulation, Inheritance, Polymorphism, Abstraction) | 1.0 | Bắt buộc | Cả nhóm                                                                                   | |
| Áp dụng design pattern phù hợp | 1.0 | Bắt buộc | Cả nhóm                                                                                   | |
| **2. Chức năng chính** | | |                                                                                           | |
| Quản lý người dùng, sản phẩm | 1.0 | Bắt buộc | Phong (chính), Tưởng (chính), Vũ (chính), Ánh                                             | |
| Chức năng đấu giá | 1.0 | Bắt buộc | Ánh (chính), Vũ                                                                           | |
| Xử lý lỗi & ngoại lệ | 1.0 | Bắt buộc | Phong (chính), Tưởng (chính), Ánh (Auction exception), Vũ (Item - Auction (10%)Exception) | |
| **3. Kỹ thuật quan trọng & concurrency** | | |                                                                                           | |
| Xử lý đấu giá đồng thời an toàn (tránh lost update, rollback, race condition) | 1.0 | Bắt buộc | Ánh                                                                                       | |
| Realtime update (Observer/Socket): thông báo bid mới cho tất cả client | 0.5 | Bắt buộc | Ánh                                                                                       | |
| **4. Tích hợp, kiến trúc & chất lượng mã** | | |                                                                                           | |
| Thiết kế kiến trúc Client-Server rõ ràng | 0.5 | Bắt buộc | Ánh                                                                                       | |
| Áp dụng MVC (JavaFX + FXML cho client, Controller-Model-DAO cho server) | 0.5 | Bắt buộc | Ánh, Vũ (chính), Phong (UI User)                                                          | |
| Sử dụng Maven/Gradle, coding convention tốt, mã nguồn sạch | 0.5 | Bắt buộc | Ánh, Vũ                                                                                   | |
| Unit Test (JUnit) cho logic quan trọng | 0.5 | Bắt buộc | Phong, Tưởng, Vũ(Refactor một chút)                                                       | |
| Thiết lập CI/CD cơ bản (GitHub Actions + test tự động) | 0.5 | Bắt buộc | Ánh                                                                                       | |
| **5. Chức năng nâng cao (tối đa 1.5đ)** | | |                                                                                           | |
| Auto-Bidding (đấu giá tự động với maxBid, increment, PriorityQueue) | 0.5 | Tùy chọn | Ánh, Vũ (Sửa logic)                                                                       | |
| Gia hạn phiên đấu giá (Anti-sniping) khi bid cuối | 0.5 | Tùy chọn | Ánh                                                                                       | |
| Bid History Visualization: biểu đồ đường giá realtime (line chart) | 0.5 | Tùy chọn | Ánh, Vũ                                                                                   | |
| Các tính năng sáng tạo khác | 0.5 | Tùy chọn | Cả nhóm                                                                                   | |
| **TỔNG ĐIỂM** | **10 + 1** | |                                                                                           | **100%** |

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
* **Security: JWT, Bcrypt mã hóa token và phiên (mới mã hóa mật khẩu)
* **Config: Setup hệ thống ban đầu

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
* **State:** Quản lý vòng đời khép kín của một phiên đấu giá (SCHEDULED, ACTIVE, ENDED).

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
- [X] Hoàn thiện kết nối Socket 2 chiều Client - Server cho việc truyền tải DTO.
- [X] Tích hợp Observer Pattern để broadcast giá đấu thời gian thực.
- [X] Phát triển tính năng tạo phiên đấu giá (Create Item) và hiển thị danh sách live.

### Giai đoạn 4: Tối ưu & Mở rộng (Sắp tới)
- [X] Hoàn thiện thuật toán Anti-sniping và Auto-bidding.
- [ ] Kiểm thử đồng thời (Concurrency testing).
