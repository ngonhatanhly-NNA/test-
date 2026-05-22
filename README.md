# 🏷️ Team 13 Bidding System 

Một hệ thống đấu giá trực tuyến được xây dựng dựa trên kiến trúc **N-Tier (Đa tầng)** và **Multi-module Maven**, tuân thủ nghiêm ngặt các nguyên tắc thiết kế phần mềm hướng đối tượng (OOP), SOLID và vận dụng đa dạng các Design Patterns. Dự án tập trung vào khả năng mở rộng, quản lý trạng thái thời gian thực (real-time), và bảo mật luồng dữ liệu.

- Đấu giá realtime đa user, UI hiện kết quả tức thời
- Đăng nhập/Phân quyền (Bidder - Seller - Admin)
- Auto-bid, anti-sniping kéo dài phiên khi bid “last minute”
- Xử lý đồng thời, tránh race condition, rollback chuẩn nghiệp vụ
- DB script và auto seed data, CI/CD trên GitHub Actions
- Clean code, áp dụng design pattern, multi-module Maven
- Tích hợp test cho các nghiệp vụ quan trọng

## 👥 Thành viên nhóm 13


| Thành viên          | Nhiệm vụ chính & Thiết kế | Giải pháp/Kiến trúc thực hiện | Tính năng, chức năng đã hoàn thành thực tế |
|----------------------|-----------------------|-----------------------------|-------------------------------------|
| **Ngô Nhật Ánh - 25020030**    | - Thiết kế tổng thể kiến trúc hệ thống (multi-layer, design pattern) <br> - Backend: Đấu giá (Auction), Auth, User, Networking, Security <br> - Set up DB, cấu hình môi trường, CI/CD <br> - Tối ưu concurrency, realtime socket <br> - Viết, tối ưu, review code chính cho các module khó | - Phân lớp N-Tier, chia module rõ ràng: shared/server/client <br> - Áp dụng các design patterns (Command, Observer, Strategy, State, Singleton...) <br> - Đảm nhiệm xây dựng AuctionService, AuthService, WebSocket Broadcaster <br> - Viết/áp dụng Anti-sniping, Auto-bid, xử lý đồng thời, bảo mật JWT | - Hệ thống đấu giá đa phiên: Đặt giá, auto-bid, sniping <br> - Realtime cập nhật giá, broadcast tới nhiều user qua Websocket <br> - Đăng ký/đăng nhập/xác thực tài khoản (Hash mật khẩu, JWT) <br> - Quản lý user, phân quyền (Bidder/Seller/Admin) <br> - CI/CD Github Actions, Unit test chính <br> - Tối ưu truy vấn DB, rollback, kiểm soát race condition |
| **Đinh Anh Vũ - 25020432**      | - Xây dựng module User, Item <br> - Frontend UI JavaFX, kiểm thử | - Tham gia code UserService, ItemService, code giao diện, refactor data truyền mạng <br> - Đóng gói dữ liệu DTO, xử lý update Profile, xác thực | - CRUD User, Item <br> - Giao diện quản lý sản phẩm, tài khoản <br> - Kiểm thử, seed database, đóng góp cấu hình DB/DTO |
| **Hoàng Văn Tưởng - 25020383 **  | - Backend Admin, Seller, Bidder <br> - Xử lý logic, review code <br> - Unit test | - Viết các controller và service riêng cho từng loại user <br> - Quản lý, kiểm tra nghiệp vụ phân quyền <br> - Viết Unit test, tham gia review các flow | - Module quản trị: Admin có thể thao tác trên danh sách User/Seller/Bidder <br> - Kiểm soát hành vi user, khóa mở tài khoản, kiểm thử logic phức tạp |
| **Mai Thế Phong - 25020310**    | - Interface quản trị (Admin), Seller, Bidder <br> - Backend <br> - UI/UX User | - Kết nối UI JavaFX với backend, xử lý giao diện đa vai trò <br> - Code xử lý các chức năng cho admin, seller, bidder trong controller/service <br> - Refactor UI chuyển cảnh | - Màn hình quản trị hiện đại <br> - Giao diện User, Seller, Bidder đầy đủ chức năng <br> - Liên kết mượt giữa UI <-> Service <br> - Quản lý tài khoản & cập nhật thông tin |


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
4. Chạy file TEST để tạo tài khoản và các dữ liệu thử ban đầu
5. Tạo file application.properties trong Bidding Server và ghi các thông tin về tài khoản sql của bạn
### 3. Build Dự án với Maven
Mở terminal tại thư mục gốc của dự án (nơi chứa file `pom.xml` tổng) và chạy lệnh:
```bash
mvn clean install
```
 Server sẽ chạy ở:
- REST: http://localhost:7070
- WebSocket: ws://localhost:8080
`
### 4. Cách khởi động Server và Client
1. Mở cmd or git bash:
   ```
   mvn exec:java -pl Bidding_Server -Dexec.mainClass="com.server.ServerApp"
   ```
 2. Để chạy client, có thể tạo ra nhiều client khác nhau:
```

mvn exec:java -pl Bidding_Client -Dexec.mainClass="com.client.Launcher"
```

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

### 1. 📦 Module `shared` (Thư viện dùng chung)
* **Chức năng:** Đóng vai trò là một thư viện dùng chung (Common Library) cho cả hai phía Client và Server, đảm bảo tính đồng bộ dữ liệu toàn hệ thống.
* **Các thành phần chính:**
  * **DTOs & Models:** * Định nghĩa các lớp thực thể (Entity) cốt lõi như `User`, `Bidder`, `Seller`, `Admin`, v.v.
    * Thiết lập hệ thống quản lý tài khoản, xử lý đăng ký/đăng nhập và phân cấp đa dạng các vai trò (roles) của người dùng.
  * **Network Components:** * Đóng gói cấu trúc định dạng Request/Response tiêu chuẩn.
    * Chuẩn hóa dữ liệu trao đổi giữa Client và Server thông qua các giao thức Socket hoặc HTTP.
* **💡 Ý nghĩa:** Giúp tối ưu hóa mã nguồn, tránh lặp lại code (DRY - Don't Repeat Yourself). Khi có sự thay đổi về cấu trúc dữ liệu, chỉ cần cập nhật tại module này, giúp hệ thống dễ dàng bảo trì và mở rộng.

---

### 2. 🖥️ Module `Bidding_Server` (Backend & Database)
* **Chức năng:** Chịu trách nhiệm tiếp nhận yêu cầu, xử lý toàn bộ logic nghiệp vụ, tương tác trực tiếp với cơ sở dữ liệu MySQL và điều phối mọi hoạt động phía máy chủ.
* **Các thành phần chính:**
  * **Controller (Network Layer):** Tiếp nhận yêu cầu từ Client, thực hiện đọc/gửi dữ liệu định dạng JSON qua kết nối SOCKET, và điều hướng luồng xử lý dữ liệu.
  * **Service (Business Logic):** Nơi tập trung toàn bộ thuật toán nghiệp vụ cốt lõi, hoạt động độc lập với tầng Controller và DAO. Xử lý các bài toán phức tạp như: vận hành đồng thời nhiều phiên đấu giá (concurrency), thuật toán chống gian lận, hệ thống tự động đặt giá (auto-bidding),...
  * **Repository / DAO (Data Access Object):** Điểm truy cập duy nhất cho mọi thao tác liên quan tới cơ sở dữ liệu SQL. Áp dụng mẫu thiết kế **Singleton (`DBConnection`)** để tối ưu hóa connection pool, ngăn ngừa tình trạng quá tải và gia tăng hiệu năng hệ thống.
  * **WebSocket / Broadcaster:** Quản lý và phát sóng (broadcast) trạng thái của các phòng đấu giá theo thời gian thực (Real-time). Đảm bảo mọi Client luôn nhận được mức giá mới nhất một cách liên tục và tức thì.
  * **Security:** Tích hợp cơ chế mã hóa và băm mật khẩu mạnh mẽ bằng **BCrypt**, đồng thời quản lý và bảo mật token cho các phiên đăng nhập.
  * **Config:** Lưu trữ các tệp cấu hình hệ thống, thông số kết nối Database và các thiết lập khởi tạo Server ban đầu.
* **💡 Ý nghĩa:** Được ví như "não bộ" của toàn hệ thống, chịu trách nhiệm vận hành, kiểm soát, ra quyết định và phản hồi thông tin chính xác, kịp thời tới các Client.

---

### 3. 💻 Module `Bidding_Client` (Frontend JavaFX)
* **Chức năng:** Đảm nhận việc xây dựng giao diện người dùng (UI/UX), hiển thị thông tin trực quan và xử lý các tương tác trực tiếp từ phía người dùng cuối.
* **Các thành phần chính:**
  * **Controller (UI):** * Quản lý các màn hình chức năng (Đăng nhập, Đăng ký, Dashboard, Phòng đấu giá...).
    * Xử lý các hiệu ứng chuyển cảnh động và quản lý hoạt họa đồ họa (ví dụ: `SpriteAnimation`). Điều hướng luồng tương tác của người dùng trên giao diện.
  * **Session Management:** Lưu trữ thông tin phiên làm việc của người dùng hiện tại qua đối tượng `UserSession`. Giúp duy trì trạng thái đăng nhập và cá nhân hóa trải nghiệm người dùng xuyên suốt ứng dụng.
  * **Network / Sender:** * Thiết lập kết nối, đóng gói và gửi/nhận các gói dữ liệu JSON thông qua **Socket TCP** tới Server.
    * Đồng thời, liên tục lắng nghe các tín hiệu cập nhật từ WebSocket để cập nhật giao diện thời gian thực (như khi xuất hiện AI Bidder, bước giá mới, đếm ngược thời gian,...).
* **💡 Ý nghĩa:** Là nơi người dùng tương tác trực tiếp với hệ thống. Đảm bảo mọi luồng thông tin từ Server được hiển thị mượt mà, trực quan và cập nhật real-time theo từng mili-giây.

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
