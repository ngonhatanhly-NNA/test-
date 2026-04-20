# 🎨 Cẩm Nang Design Patterns - Hệ Thống Đấu Giá Trực Tuyến

Tài liệu này tổng hợp các Design Patterns (Mẫu thiết kế) **đã triển khai trong code hiện tại** của dự án.

Mục tiêu: mô tả đúng “pattern nào đang dùng ở đâu”, tránh lẫn với các ý tưởng chưa implement.

---

## ✅ A) Các Design Patterns đã áp dụng (kèm class/package)

### A.1. Singleton Pattern
* **Ý nghĩa:** Một instance duy nhất, dùng chung toàn hệ thống.
* **Trong dự án:**
  * **Server:** `com.server.config.DBConnection` (DataSource/Hikari pool dùng chung)
  * **Client:**
    * `com.client.network.NetworkClient` (giữ `HttpClient` + `CookieManager` để giữ session)
    * `com.client.network.MyWebSocketClient` (một kết nối WS duy nhất qua biến `instance`)

### A.2. Factory (Simple Factory / Factory Method)
* **Ý nghĩa:** Đóng gói logic tạo object theo type/role.
* **Trong dự án:**
  * `com.server.model.ItemFactory`: tạo đúng subtype `Item` (`Electronics`/`Art`/`Vehicle`) từ `CreateItemRequestDTO`.
  * `com.server.DAO.UserRowMapperFactory`, `com.server.DAO.UserQueryFactory`: chọn mapper/query theo role.

### A.3. Command Pattern (API Command objects)
* **Ý nghĩa:** “1 API use-case = 1 command”, dễ mở rộng và test.
* **Trong dự án:** `com.server.controller.command.*`
  * Ví dụ: `CreateItemCommand`, `GetAllItemsCommand`, `CreateAuctionCommand`, `PlaceBidCommand`, `UpdateProfileCommand`, ...

### A.4. Template Method Pattern
* **Ý nghĩa:** Lớp cha định nghĩa sườn xử lý, lớp con chỉ tập trung phần nghiệp vụ.
* **Trong dự án:**
  * `BaseApiCommand#handle(ctx)` là sườn chung (try/catch + response lỗi chuẩn) và gọi `execute(ctx)`.

### A.5. Strategy Pattern
* **Ý nghĩa:** Nhiều thuật toán cùng interface, hoán đổi được.
* **Trong dự án (Server):**
  * Anti-sniping: `AntiSnipingStrategy` + `DefaultAntiSnipingStrategy`.
  * Bid validation: `BidValidationStrategy` + `BasicBidValidation`, `MinimumIncrementValidation` (và `ReservePriceValidation` placeholder).
  * Bid processing: `BidProcessor` + `ManualBidProcessor`, `AutoBidProcessor`.
* **Trong dự án (Client):**
  * Profile UI: `IProfileStrategy` + `AdminProfileStrategy`, `BidderProfileStrategy`, `SellerProfileStrategy`.

### A.6. Chain of Responsibility
* **Ý nghĩa:** Request đi qua chuỗi validator/handler.
* **Trong dự án:** `BidValidationChain` chạy nhiều `BidValidationStrategy`.

### A.7. Observer / Publish–Subscribe (Event Listener)
* **Ý nghĩa:** Khi state thay đổi, observers nhận update tự động.
* **Trong dự án:**
  * `AuctionService` phát event qua `AuctionEventListener`.
  * `com.server.websocket.Broadcaster` broadcast `AUCTION_UPDATE:{json}` tới các WS client.
  * `com.client.network.MyWebSocketClient` nhận `AUCTION_UPDATE:` và cập nhật UI.

### A.8. Repository/DAO + DTO (Architectural Patterns)
* **Repository/DAO:** `com.server.DAO.*Repository` đóng gói SQL + mapping.
* **DTO:** `shared/com.shared.dto.*` là hợp đồng dữ liệu giữa client/server.

### A.9. Facade (nhẹ) ở tầng Network/Service
* **Client:** `AuthNetwork` / `ItemNetwork` / `AuctionNetwork` che giấu chi tiết tạo request/parse response.
* **Server:** `AuctionService` gom một chuỗi bước (validate → process → persist → broadcast) trong `placeBid()`.

---

## 🧭 B) Luồng chạy (tóm tắt)

### B.1. Start Server
1. Chạy `com.server.ServerApp`.
2. Mở WebSocket `ws://localhost:8080`.
3. Mở REST `http://localhost:7070` và gắn route ở `ApiRouter`.

### B.2. Start Client
1. Chạy `com.client.Main`/`Launcher`.
2. `MyWebSocketClient.connectToServer()` để nhận realtime.
3. Load UI JavaFX (FXML).

### B.3. Login / Session cookie
1. UI gọi `POST /api/login`.
2. Server set session `ctx.sessionAttribute("username", ...)`.
3. Client giữ cookie nhờ `NetworkClient` (`CookieManager`).

### B.4. Bid + Realtime
1. UI gọi REST `POST /api/auctions/bid`.
2. `AuctionService.placeBid()` cập nhật DB và phát event.
3. `Broadcaster` gửi `AUCTION_UPDATE` qua WS.
4. Client nhận WS và cập nhật UI.

---

## 💡 C) Pattern đề xuất/mở rộng (chưa thấy triển khai rõ trong code hiện tại)

Các mục dưới đây là hướng phát triển, chỉ nên dùng khi có class/feature thật:

* **Builder Pattern**: tạo object nhiều tham số (Auction/Item/DTO) rõ ràng hơn.
* **Prototype Pattern**: “clone item/auction template”.
* **Adapter/Proxy/Mediator**: hữu ích khi tích hợp API bên thứ 3, lazy load ảnh, hoặc điều phối chat/room.
* **State Pattern (chuẩn)**: hiện Auction dùng enum `Status`; nếu cần hành vi theo state phức tạp có thể nâng cấp thành State pattern.
