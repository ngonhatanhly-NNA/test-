# Team13_Bidding_System
* Bài tập lớp thiết kế hệ thống đấu giá 

## Thành viên nhóm 13:
* Ngô Nhật Ánh - 25020030 - Vai trò
* Đinh Anh Vũ - - 
* Hoàng Văn Tưởng - - 
* Mai Thế Phong - -
## I-Công nghệ sử dụng (Tech requirement)
*Ngôn ngữ: Java
*Giao diện(FE): JavaFX (FXML & CSS)
*Cơ sở dũ liệu(DB): MYSQL 
*Kết nối Database: JDBC Driver
*Quản lí dự án và thư viện: Maven
*Mạng(Networking): REST API, WebSocket (Javalin)
*Github

## II - Kiến trúc hệ thống: (Project structure)
## 🗂️ Kiến trúc hệ thống (Enterprise Multi-module Architecture)

Dự án được xây dựng theo kiến trúc N-Tier (Đa tầng) chuẩn Doanh nghiệp, phân tách nghiêm ngặt giữa giao diện (UI), xử lý mạng (Networking), logic nghiệp vụ (Business Logic) và truy xuất dữ liệu (Data Access). Hệ thống bao gồm 3 module độc lập:

### 1️⃣ Module `shared` (Khối Dữ liệu dùng chung)
** /dot: Dữ liệu truyền đi giữa User và Service:
** /network: Request, Response truyền đi 
### 2️⃣ Module `Bidding_Server` (Máy chủ Backend)
Đóng vai trò là trung tâm xử lý, giao tiếp trực tiếp với cơ sở dữ liệu MySQL và quản lý các kết nối Socket[cite: 130]. Tuân thủ kiến trúc Server MVC (Controller → Model → DAO).
* **`ServerApp.java`**: Điểm khởi chạy (Entry point) của Server, chịu trách nhiệm bật ServerSocket để lắng nghe Client.
* **`config/`**: Tầng cấu hình hệ thống.
    * *`DBConnection.java`*: Quản lý kết nối tới cơ sở dữ liệu MySQL, áp dụng **Singleton Pattern** để đảm bảo chỉ có duy nhất một kết nối được tạo ra và sử dụng chung[.
    * *Các file cấu hình khác*: `SecurityConfig`, `WebSocketConfig`, `OpenApiConfig`.
        - **Thiết lập Môi trường Database:** Cấu hình thành công cơ sở dữ liệu MySQL cục bộ và hoàn thiện lớp kết nối `DBConnection` (áp dụng Singleton Pattern ), nối với tầng DAO.
* **`controller/`**: Tầng giao tiếp mạng, và giao nhiệm vũ
    * *`AuthController`, `AuctionController`*: Chịu trách nhiệm tiếp nhận các luồng dữ liệu (Request) gửi lên từ Socket của Client, phân rã gói tin và điều hướng xuống tầng Service.
* **`service/`**: Tầng xử lý nghiệp vụ (Business Logic)
    * [cite_start]*`AuthService`, `AuctionService`, `UserService`, `SellerService`*: Nơi chứa các thuật toán phức tạp như xử lý đấu giá đồng thời (Concurrent Bidding) , gia hạn phiên (Anti-sniping) [cite: 89, 90][cite_start], và đấu giá tự động (Auto-Bidding).
    * `ItemService`: Tầng xử lý thông tin từ DAO đưa cho SeverApp (Biên dịch dữ liệu lấy từ DAO thành JSON để đưa cho ServerApp)
* **`repository/`**: Tầng truy xuất dữ liệu (DAO), trò chuyện với DB
    * *`UserRepository`, `BidRepository`*: Nơi duy nhất chứa các câu lệnh SQL (SELECT, INSERT, UPDATE) để tương tác với MySQL. Tầng Service sẽ gọi Repository để lấy/lưu dữ liệu.
    * *`ItemRepository`*: Chứa câu lệnh SQL để lưu và lấy dữ liêụ của items
      * Database: Dùng chiến thuật Single Table cho bảng items (Gom thuộc tính của Electronics, Art, Vehicle vào chung 1 bảng 15 cột). Dùng cột item_type để phân loại.
      * Connection: Chỉ tạo 1 đường ống kết nối duy nhất (Singleton DBConnection) để không sập Server.
* **`websocket/`**: Tầng cập nhật thời gian thực (Real-time).
    * *`Broadcaster.java`*: Áp dụng **Observer Pattern** để quản lý danh sách các Client đang xem phiên đấu giá. Khi có người đặt giá mới, class này sẽ tự động "bắn" thông báo (notify) cho toàn bộ Client cùng lúc, sử lí trong phiên đấu giá khi hoạt động.
* **`route/`**: (Tương lai) các liên kết api, luồng
### 3️⃣ Module `Bidding_Client` (Máy khách Frontend)Chịu trách nhiệm hiển thị giao diện đồ họa cho người dùng, tuân thủ mô hình MVC (JavaFX + FXML).
* **`controller/`**: Tầng điều khiển giao diện. Nhận các sự kiện click chuột, nhập phím từ người dùng.
    * *`auth/`*: Quản lý chức năng Đăng nhập (`LoginController`) và Đăng ký (`RegisterController`).
    * *`dashboard/`*: Quản lý màn hình chính (`DashboardController`) hiển thị danh sách sản phẩm và diễn biến đấu giá.
   
      - **Phát triển Giao diện Danh sách phiên đấu giá:** Thiết kế `ViewDashboard.fxml`, tự động co giãn rớt dòng, kèm thiết kế Thẻ Sản Phẩm (Item Card) trực quan.
* **`network/`**: Tầng giao tiếp với Server .
    * `SocketClient.java`, `RequestSender.java`*: Chịu trách nhiệm đóng gói dữ liệu từ UI thành các luồng byte/JSON và gửi qua TCP Socket tới Server.
* **`session/`**: Tầng quản lý trạng thái.(tránh phải login đi login lại)
    * *`UserSession.java`*: Lưu trữ thông tin (phiên đăng nhập) của người dùng hiện tại một cách an toàn ở bộ nhớ cục bộ để các màn hình khác nhau có thể sử dụng chung.
* **`util/`**: Tầng tiện ích hỗ trợ các chuyển động.
    * *`SceneController`*: Xử lý logic chuyển cảnh (kéo rèm) giữa các màn hình FXML.
    * *`SpriteAnimation`, `SmallAnimation`*: Xử lý các hiệu ứng đồ họa động (Animation).
* **`resources/`**: Thư mục chứa toàn bộ tài nguyên tĩnh của ứng dụng.
    * *`fxml/`*: Các file thiết kế giao diện bằng SceneBuilder.
    * *`images/`*, *`css/`*: Các file hình ảnh và định dạng phong cách cho UI.
                
## III - Tiến độ hiện tại (Progress)
* [x] Thiết lập kiến trúc Multi-module Maven.
* [x] Xây dựng xong các class Model cơ sở (`shared` module).
* [x] Hoàn thiện UI/UX màn hình Đăng nhập (Login) & Đăng ký (Register).
* [X] Thiết kế giao diện Sảnh chính / Dashboard (Auction Menu).
* [ ] Thiết lập kết nối JDBC tới MySQL (Đã kết nối xong với Register/Login còn Item, Auction).
* [ ] Xây dựng kết nối Socket giữa Client và Server (Đang phát triển).


DETAILS - ĐA LÀM

