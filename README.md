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


| Thành viên          | Nhiệm vụ chính & Thiết kế                                                                                                                                                                                                                                                                  | Giải pháp/Kiến trúc thực hiện                                                                                                                                                                                                                                                                                                                         | Tính năng, chức năng đã hoàn thành thực tế                                                                                                                                                                                                                                                                                                                    |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Ngô Nhật Ánh - 25020030**    | - Thiết kế tổng thể kiến trúc hệ thống (multi-layer, design pattern) <br> - Backend: Đấu giá (Auction), Auth, User, Networking, Security <br> - Set up DB, cấu hình môi trường, CI/CD <br> - Tối ưu concurrency, realtime socket <br> - Viết, tối ưu, review code chính cho các module khó | - Phân lớp N-Tier, chia module rõ ràng: shared/server/client <br> - Áp dụng các design patterns (Command, Observer, Strategy, State, Singleton...) <br> - Đảm nhiệm xây dựng AuctionService, AuthService, WebSocket Broadcaster <br> - Viết/áp dụng Anti-sniping, Auto-bid, xử lý đồng thời, bảo mật JWT                                              | - Hệ thống đấu giá đa phiên: Đặt giá, auto-bid, sniping <br> - Realtime cập nhật giá, broadcast tới nhiều user qua Websocket <br> - Đăng ký/đăng nhập/xác thực tài khoản (Hash mật khẩu, JWT) <br> - Quản lý user, phân quyền (Bidder/Seller/Admin) <br> - CI/CD Github Actions, Unit test chính <br> - Tối ưu truy vấn DB, rollback, kiểm soát race condition |
| **Đinh Anh Vũ - 25020432**      | - Xây dựng module User, Item <br> - Frontend UI JavaFX, kiểm thử (Blackbox)                                                                                                                                                                                                                | - Tham gia code UserService, ItemService, code giao diện, refactor data truyền mạng, logic đấu giá <br> - Đóng gói dữ liệu DTO, xử lý update Profile, xác thực<br> - Áp dụng các designs patterns (Singelton, Builder, Factory, Command...)<br> - Fix broken code ở commit hỏng/ Refactor tuân thủ OCP, smell code Obfuscated Code | - CRUD User, Item <br> - Giao diện + Backend quản lý danh sách + tạo sản phẩm, Giao diện danh sách và tạo phiên đấu giá, tài khoản <br> - Kiểm thử, seed database, đóng góp cấu hình DB/DTO<br> - Fix logic đặt giá không thực tế, Refactor                                                                                                                   |
| **Hoàng Văn Tưởng - 25020383 **  | - Backend Admin, Seller, Bidder <br> - Xử lý logic, review code <br> - Unit test                                                                                                                                                                                                           | - Viết các controller và service riêng cho từng loại user <br> - Quản lý, kiểm tra nghiệp vụ phân quyền <br> - Viết Unit test, tham gia review các flow                                                                                                                                                                                               | - Module quản trị: Admin có thể thao tác trên danh sách User/Seller/Bidder <br> - Kiểm soát hành vi user, khóa mở tài khoản, kiểm thử logic phức tạp                                                                                                                                                                                                          |
| **Mai Thế Phong - 25020310**    | - Interface quản trị (Admin), Seller, Bidder <br> - Backend <br> - UI/UX User                                                                                                                                                                                                              | - Kết nối UI JavaFX với backend, xử lý giao diện đa vai trò <br> - Code xử lý các chức năng cho admin, seller, bidder trong controller/service <br> - Refactor UI chuyển cảnh                                                                                                                                                                         | - Màn hình quản trị hiện đại <br> - Giao diện User, Seller, Bidder đầy đủ chức năng <br> - Liên kết mượt giữa UI <-> Service <br> - Quản lý tài khoản & cập nhật thông tin                                                                                                                                                                                    |


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
2. Tạo một schema mới cho dự án (vd: auction_db).
3. Chạy file script SQL được đính kèm trong source code: `script_db.sql`.
4. Chạy file `TEST_DATA.sql` để tạo tài khoản và các dữ liệu thử ban đầu
5. Tạo file application.properties trong Bidding Server và ghi các thông tin về tài khoản sql
```
db.url=jdbc:mysql://localhost:3306/tên_database_của_bạn
db.user=root
db.password=mật_khẩu_của_bạn
```
### 3. Build Dự án với Maven
Mở terminal tại thư mục gốc của dự án (nơi chứa file `pom.xml` tổng) và chạy lệnh:
```bash
mvn clean install -DskipTests
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
  * **Network :** * Thiết lập kết nối, đóng gói và gửi/nhận các gói dữ liệu JSON thông qua **Socket TCP** tới Server.
  * **Util : ** *Hỗ trợ các chức năng phụ như chuyển scene, chuyển panel, và visualize của ui, game.
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
Các chức năng đã hoàn thành
## 1. Nhóm chức năng Bắt buộc (Core Features)

### Quản lý người dùng
* **Bảo mật:** Hệ thống đăng ký và đăng nhập tài khoản an toàn với cơ chế băm mật khẩu (**BCrypt**).
* **Xác thực:** Sử dụng **JWT Token** để duy trì phiên làm việc.
* **Phân quyền:** Hỗ trợ phân quyền chặt chẽ cho 3 vai trò:
    * **Admin:** Quản trị viên hệ thống.
    * **Seller:** Người bán hàng.
    * **Bidder:** Người tham gia đấu giá.

### Quản lý sản phẩm & Phiên đấu giá
* **Nghiệp vụ Seller:** Thực hiện CRUD (Thêm/Sửa/Xóa) sản phẩm và cấu hình chi tiết phiên đấu giá (giá khởi điểm, bước giá, thời gian).
* **Vòng đời phiên:** Tự động quản lý trạng thái (`OPEN` -> `RUNNING` -> `FINISHED`).
* **Xử lý kết thúc:** Tự động xác định người thắng cuộc ngay khi phiên đấu giá đóng lại.

### Nghiệp vụ đấu giá
* **Kiểm tra hợp lệ:** Hệ thống kiểm tra logic đặt giá (giá mới phải cao hơn giá hiện tại + bước giá).
* **Exception Hierarchy:** Xây dựng hệ thống phân cấp ngoại lệ để xử lý triệt để các lỗi logic:
    * Đặt giá thấp hơn giá hiện tại.
    * Đấu giá khi phiên đã kết thúc.
    * Lỗi kết nối hoặc tranh chấp dữ liệu.

---

## 2. Kỹ thuật & Concurrency (Bắt buộc)

### Xử lý đấu giá đồng thời (Concurrent Bidding)
* **An toàn dữ liệu:** Áp dụng các kỹ thuật khóa (Locking) để ngăn chặn triệt để tình trạng **Lost Update** và **Rollback**.
* **Race Condition:** Đảm bảo tính nhất quán khi có hàng ngàn người cùng đặt giá tại một thời điểm.

### Cập nhật thời gian thực (Realtime Update)
* **Design Pattern:** Sử dụng **Observer Pattern** để quản lý các đối tượng theo dõi phiên.
* **Công nghệ:** Kết nối **WebSocket** để broadcast (phát sóng) giá mới tới toàn bộ Client ngay lập tức mà không cần tải lại trang (Zero Refresh).

---

## 3. Nhóm chức năng Nâng cao (Advanced Features)

* **Auto-Bidding:** Tính năng tự động trả giá thay người dùng dựa trên `maxBid` và `increment`. Sử dụng **PriorityQueue** để quản lý thứ tự ưu tiên đặt giá.
* **Anti-sniping:** Thuật toán tự động gia hạn thêm thời gian cho phiên đấu giá nếu phát hiện có lượt bid mới trong những giây cuối cùng để chống chiến thuật "bắn tỉa".
* **Bid History Visualization:** Hiển thị biểu đồ đường (**Line Chart**) theo thời gian thực ngay trên giao diện **JavaFX** để người dùng theo dõi biến động giá trực quan.

### Chức năng Giải trí & Tương tác (Tính năng Sáng tạo - Bonus)
Bên cạnh các nghiệp vụ cốt lõi của một sàn đấu giá, hệ thống còn tích hợp các tính năng giải trí độc đáo nhằm nâng cao trải nghiệm người dùng (UI/UX) và giữ chân người dùng trong thời gian chờ đợi:
* **Game Corner (Góc Mini-game):** Tích hợp khu vực giải trí ngay trong ứng dụng (tích hợp qua `GameCornerController` và `MiniGameManager`). Người dùng có thể tham gia các trò chơi nhỏ với hiệu ứng hình ảnh động (Sprite Animation) mượt mà được tối ưu hóa trên JavaFX.
* **Trợ lý Bidding Pet (Pet Ảo):** Tích hợp một thú cưng ảo trực tiếp trên giao diện (`BiddingPet`). Pet không chỉ là một yếu tố trang trí giúp giao diện bớt khô khan, mà còn có khả năng tương tác với người dùng, và đóng vai trò như một "trợ lý ảo" đồng hành trong suốt quá trình đấu giá.
---
LINK PDF và video
