# 🎨 Cẩm Nang Design Patterns - Hệ Thống Đấu Giá Trực Tuyến

Tài liệu này tổng hợp các Design Patterns (Mẫu thiết kế) được đề xuất và áp dụng trong dự án Hệ thống Đấu giá Trực tuyến (Online Auction System), nhằm đảm bảo tính mở rộng (scalability), dễ bảo trì (maintainability) và tuân thủ các nguyên tắc SOLID.

---

## 🏗️ 1. Nhóm Creational (Khởi tạo đối tượng)

### 1.1. Singleton Pattern
* **Ý nghĩa:** Đảm bảo một class chỉ có duy nhất một instance (thể hiện) trong suốt vòng đời của ứng dụng và cung cấp điểm truy cập toàn cục đến nó.
* **Ứng dụng trong dự án:**
  * **`DBConnection`:** Quản lý kết nối đến cơ sở dữ liệu. Chỉ tạo một Connection Pool duy nhất để tránh cạn kiệt tài nguyên (Connection Leak).
  * **`Broadcaster` / WebSocket Manager:** Lớp quản lý các luồng socket kết nối đến các client để gửi thông báo realtime.

### 1.2. Factory Method / Abstract Factory
* **Ý nghĩa:** Cung cấp một interface để tạo đối tượng, nhưng để các class con quyết định class nào sẽ được tạo ra.
* **Ứng dụng trong dự án:**
  * **`ItemFactory`:** Trả về các đối tượng sản phẩm khác nhau (`Electronics`, `Art`, `Vehicle`) dựa trên input của người dùng khi tạo phiên đấu giá mới.
  * **`ProfileUIStrategyFactory`:** Trả về giao diện/logic profile tương ứng cho từng loại người dùng (`Admin`, `Bidder`, `Seller`).
  * **`PaymentFactory`:** Khởi tạo các cổng thanh toán khác nhau (Momo, VNPay, Credit Card).

### 1.3. Builder Pattern
* **Ý nghĩa:** Tách biệt quá trình khởi tạo một đối tượng phức tạp khỏi biểu diễn của nó, cho phép tạo ra các đối tượng với nhiều tham số tùy chọn mà không làm phình to Constructor (Telescoping Constructor).
* **Ứng dụng trong dự án:**
  * **Tạo đối tượng `Auction` / `Item`:** Một phiên đấu giá có rất nhiều tham số (giá khởi điểm, bước giá, thời gian bắt đầu/kết thúc, luật anti-sniping...). Builder giúp việc tạo đối tượng này dễ đọc hơn: `Auction.builder().setItem(item).setStartPrice(100).build();`
  * **Network `Request` / `Response` DTO:** Đóng gói dữ liệu gửi qua mạng giữa Client và Server.

### 1.4. Prototype Pattern
* **Ý nghĩa:** Tạo ra đối tượng mới bằng cách clone (sao chép) từ một đối tượng nguyên mẫu (prototype) đã tồn tại.
* **Ứng dụng trong dự án:**
  * **Nhân bản sản phẩm (Clone Item):** Khi một Seller muốn tạo nhanh 10 phiên đấu giá cho 10 chiếc điện thoại giống hệt nhau, tính năng "Tạo bản sao" sẽ dùng Prototype để copy dữ liệu thay vì bắt người dùng nhập lại từ đầu.

---

## 🔗 2. Nhóm Structural (Cấu trúc đối tượng)

### 2.1. Adapter Pattern
* **Ý nghĩa:** Cho phép các interface không liên quan làm việc cùng nhau (như một bộ chuyển đổi ổ cắm điện).
* **Ứng dụng trong dự án:**
  * **Tích hợp thanh toán:** Khi hệ thống cần kết nối với API ngân hàng hoặc ví điện tử bên thứ 3 (VD: PayPal, Stripe), ta tạo một `PaymentAdapter` để chuyển đổi chuẩn dữ liệu của hệ thống đấu giá sang chuẩn dữ liệu mà API bên ngoài yêu cầu.

### 2.2. Facade Pattern
* **Ý nghĩa:** Cung cấp một interface đơn giản, thống nhất cho một hệ thống con (subsystem) phức tạp.
* **Ứng dụng trong dự án:**
  * **`AuctionFacade`:** Khi người dùng bấm "Đặt giá", rất nhiều việc xảy ra bên dưới: Kiểm tra số dư (User DAO), Kiểm tra bước giá hợp lệ, Cập nhật Database (Auction DAO), Ghi log (Transaction DAO), Gửi thông báo WebSocket. Facade gộp tất cả logic này vào một hàm duy nhất `placeBid()` để Client gọi một cách đơn giản.

### 2.3. Proxy Pattern
* **Ý nghĩa:** Cung cấp một vật thay thế (surrogate) để kiểm soát quyền truy cập vào đối tượng gốc.
* **Ứng dụng trong dự án:**
  * **Virtual Proxy (Lazy Loading):** Trì hoãn việc tải hình ảnh độ phân giải cao của các mặt hàng đấu giá cho đến khi người dùng thực sự click vào xem chi tiết sản phẩm, giúp UI Client load nhanh hơn.
  * **Protection Proxy:** Kiểm tra quyền (Authorization) xem User có phải là `Admin` không trước khi cho phép gọi hàm `deleteAuction()`.

---

## 🧠 3. Nhóm Behavioral (Hành vi & Giao tiếp)

### 3.1. Observer Pattern 🌟 (Đặc biệt quan trọng)
* **Ý nghĩa:** Định nghĩa mối quan hệ 1-Nhiều. Khi đối tượng gốc thay đổi trạng thái, tất cả các đối tượng phụ thuộc (observers) sẽ được thông báo và cập nhật tự động.
* **Ứng dụng trong dự án:**
  * **Realtime Bidding (`AuctionEventListener`, `Broadcaster`):** Khi Server nhận được một mức giá mới, Server (Subject) sẽ broadcast thông báo cập nhật giá trị đến tất cả các Client (Observers) đang xem phiên đấu giá đó, giúp UI thay đổi ngay lập tức mà không cần F5.

### 3.2. Strategy Pattern 🌟
* **Ý nghĩa:** Định nghĩa một tập hợp các thuật toán, đóng gói từng thuật toán lại và làm cho chúng có thể thay thế lẫn nhau lúc runtime.
* **Ứng dụng trong dự án:**
  * **Luật Đấu giá (`AntiSnipingStrategy`):** Cho phép thay đổi linh hoạt luật chống "cướp giờ chót" (ví dụ: tự động cộng thêm 5 phút nếu có người bid trong 1 phút cuối).
  * **Xác thực giá thầu (`BidValidationStrategy`):** Các thuật toán kiểm tra giá trị thầu khác nhau (chặn giá quá thấp, chặn giá nhảy cóc...).
  * **Render UI (`IProfileUIStrategy`):** Hiển thị Dashboard khác nhau tùy theo role là `SellerProfileUIStrategy` hay `AdminProfileUIStrategy`.

### 3.3. Chain of Responsibility Pattern
* **Ý nghĩa:** Cho phép truyền một request qua một chuỗi các handler. Mỗi handler sẽ quyết định xử lý request đó hoặc đẩy cho handler tiếp theo.
* **Ứng dụng trong dự án:**
  * **Kiểm duyệt Đặt giá (`BidValidationChain`):** Khi một Bid request gửi lên, nó phải đi qua một chuỗi: (1) Kiểm tra phiên đấu giá còn mở không -> (2) Kiểm tra User có bị block không -> (3) Kiểm tra giá đưa ra có lớn hơn (giá hiện tại + bước giá) không. Nếu qua hết mới được ghi nhận.

### 3.4. Command Pattern
* **Ý nghĩa:** Đóng gói một request (yêu cầu) dưới dạng một object, cho phép tham số hóa các client với các request khác nhau, xếp hàng (queue) hoặc ghi log request.
* **Ứng dụng trong dự án:**
  * **Xử lý Network APIs (`BaseApiCommand`, `GetAllItemsCommand`):** Các yêu cầu từ UI Client gửi xuống Server được đóng gói thành các đối tượng Command. Server nhận Command, đẩy vào ThreadPool để xử lý đa luồng (tránh quá tải).

### 3.5. Mediator Pattern
* **Ý nghĩa:** Định nghĩa một đối tượng trung gian để đóng gói cách các đối tượng khác giao tiếp với nhau, giảm sự phụ thuộc trực tiếp (Coupling).
* **Ứng dụng trong dự án:**
  * **Phòng đấu giá (Auction Room / Session):** `Bidder` và `Seller` không bao giờ giao tiếp trực tiếp với nhau. Mọi thao tác chat, hỏi đáp, hay đặt giá đều gửi đến một class trung gian là `AuctionSessionMediator`. Mediator này sẽ phân phối luồng thông tin đến đúng người nhận.

### 3.6. Template Method Pattern
* **Ý nghĩa:** Định nghĩa bộ khung của một thuật toán trong một method, nhường việc triển khai một số bước cụ thể cho các subclass.
* **Ứng dụng trong dự án:**
  * **Xử lý quy trình báo cáo (Generate Report):** Lớp cha `ReportGenerator` có hàm `exportReport()` gồm các bước: *Lấy dữ liệu -> Format file -> Lưu file*. Class con `PdfReport` hay `ExcelReport` chỉ cần override lại bước *Format file*, còn luồng chính giữ nguyên.
  * **Base DAO / Repository:** Các tác vụ chuẩn (CRUD) với database.

### 3.7. State Pattern
* **Ý nghĩa:** Cho phép đối tượng thay đổi hành vi của nó khi trạng thái nội tại thay đổi.
* **Ứng dụng trong dự án:**
  * **Vòng đời phiên đấu giá (`Status`):** Một phiên đấu giá có các state: `PENDING` (Chờ duyệt), `ACTIVE` (Đang diễn ra), `ENDED` (Đã kết thúc), `CANCELLED` (Bị hủy). Thay vì dùng một đống `if/else`, mỗi state là một class. Khi ở trạng thái `ENDED`, nếu user gọi hàm `placeBid()`, state đó sẽ tự động ném ra lỗi (Exception).

---
*Tài liệu này không chỉ chứng minh khả năng áp dụng OOP/SOLID mà còn giải quyết triệt để các bài toán kỹ thuật thực tế trong hệ thống Client-Server đa luồng.*