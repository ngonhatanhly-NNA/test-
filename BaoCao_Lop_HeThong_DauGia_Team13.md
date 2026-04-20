# BÁO CÁO LỚP HỆ THỐNG ĐẤU GIÁ (TEAM 13)

> Phạm vi: thống kê các lớp **đã có** trong toàn bộ hệ thống (Client/Server/Shared), mô tả **chức năng**, **tầng kiến trúc** (Controller/Command/Service/DAO/Repository/Model/DTO), và **giao tiếp mạng** (HTTP REST + WebSocket).

---

## 1) Tổng quan kiến trúc

Hệ thống là **multi-module Maven** gồm 3 module:

1. **`shared/`**: các DTO + “hợp đồng” dữ liệu trao đổi, wrapper `Request/Response`.
2. **`Bidding_Server/`**: REST API bằng **Javalin** (cổng **7070**) + WebSocket (cổng **8080**), kết nối **MySQL** qua **HikariCP**.
3. **`Bidding_Client/`**: ứng dụng **JavaFX**, gọi REST bằng `java.net.http.HttpClient` và nhận realtime update qua `org.java_websocket.client.WebSocketClient`.

### 1.1 Luồng dữ liệu chính (tổng quát)

**Client (JavaFX UI)**
→ `network/*Network` (HTTP)
→ **Server** `route/ApiRouter` (định tuyến endpoint)
→ `controller/*` **hoặc** `controller/command/*Command` (Handler)
→ `service/*Service` (business logic)
→ `DAO/*Repository` (SQL + mapping)
→ **MySQL**

**Realtime Auction**
→ Client thao tác chính qua **REST** (tạo phiên / đặt giá / auto-bid) đến Javalin `:7070`
→ `AuctionService` xử lý + cập nhật DB
→ `websocket/Broadcaster` broadcast `AUCTION_UPDATE:<json>` qua WebSocket `:8080` cho mọi client đang xem realtime

> Ghi chú: Server có hỗ trợ nhận message WebSocket theo format `TYPE:JSON` (xem `ServerApp`), nhưng client hiện tại chủ yếu **chỉ lắng nghe** `AUCTION_UPDATE:`.

### 1.2 Design Patterns đã dùng (kèm vị trí lớp)

Các pattern dưới đây được suy ra trực tiếp từ cấu trúc package/lớp đang có trong source:

1) **Layered Architecture / (MVC ở mức module)**
   - **Client**: JavaFX `controller/*` (UI Controller) → `network/*Network` (gọi API) → DTO (`shared`) → render UI.
   - **Server**: `route/ApiRouter` (Router) → `controller/*` hoặc `controller/command/*` (API Handler) → `service/*` (business) → `DAO/*Repository` (persistence) → `model/*` (domain).

2) **Command Pattern** (đóng gói “1 endpoint = 1 command handler”)
   - `com.server.controller.command.*`:
     - `CreateAuctionCommand`, `GetActiveAuctionsCommand`, `PlaceBidCommand`, `UpdateProfileCommand`, ...
   - Lợi ích: tách từng use-case, dễ gắn router, dễ test độc lập.

3) **Template Method** (chuẩn hoá flow xử lý API + try/catch)
   - `BaseApiCommand.handle(ctx)` gọi sườn chung (validate/try-catch/serialize lỗi) và delegate phần lõi sang `execute(ctx)`.

4) **Strategy Pattern**
   - **Auction engine (Server)**
     - Anti-sniping: `AntiSnipingStrategy` + `DefaultAntiSnipingStrategy`.
     - Bid validation: `BidValidationStrategy` + các strategy cụ thể `BasicBidValidation`, `MinimumIncrementValidation`, (`ReservePriceValidation` placeholder).
     - Bid processing: `BidProcessor` + `ManualBidProcessor`, `AutoBidProcessor`.
   - **Profile UI (Client)**
     - `IProfileStrategy` + `AdminProfileStrategy`, `BidderProfileStrategy`, `SellerProfileStrategy`.
     - `ProfileUpdateStrategy` là lớp phối hợp để chọn strategy theo role.

5) **Chain of Responsibility**
   - `BidValidationChain` gom nhiều `BidValidationStrategy` theo thứ tự để kiểm tra đặt giá.

6) **Factory (Simple Factory/Factory Method)**
   - `ItemFactory`: tạo đúng subtype `Item` (`Electronics`/`Art`/`Vehicle`) từ `CreateItemRequestDTO`.
   - `UserRowMapperFactory`, `UserQueryFactory`: chọn mapper/query theo role (Admin/Bidder/Seller).

7) **Singleton**
   - `DBConnection`: quản lý pool kết nối (một nguồn DataSource dùng chung).
   - `NetworkClient` (Client): `HttpClient` dùng chung + `CookieManager` để giữ session.
   - `MyWebSocketClient`: `instance` duy nhất cho 1 kết nối WS.
   - (Một số controller UI cũng có mô hình “instance getter” như `ViewLiveAuctions.getInstance()` để tiện push update).

8) **Observer / Publish–Subscribe (Event Listener)**
   - `AuctionEventListener` + `Broadcaster`:
     - `AuctionService` phát sự kiện cập nhật giá/phiên.
     - `Broadcaster` là subscriber, phát WebSocket message `AUCTION_UPDATE:` tới các client.

9) **Repository/DAO Pattern**
   - `com.server.DAO.*Repository`: đóng gói SQL + mapping (User/Item/Auction/Bid/AutoBid/Log).

10) **DTO Pattern** (hợp đồng dữ liệu giữa client/server)
   - Tập trung trong `shared/com.shared.dto` (ví dụ: `CreateAuctionDTO`, `BidRequestDTO`, `AuctionUpdateDTO`, ...).

11) **Scheduler + Concurrency patterns** (phục vụ realtime/đa người dùng)
   - `AuctionService` dùng:
     - `ConcurrentHashMap` cache phiên.
     - `ReentrantLock` theo auction để tránh race-condition khi đặt bid.
     - `ScheduledExecutorService` để schedule kết thúc phiên.
     - `LinkedBlockingQueue` (producer/consumer) để ghi bid bất đồng bộ (tuỳ phần triển khai hiện có).

### 1.3 Các luồng chạy chính (end-to-end)

#### Luồng A — Khởi động hệ thống
1. **Start Server** (`com.server.ServerApp`)
   - Start WebSocket server tại `ws://localhost:8080`.
   - Khởi tạo `*Repository` → `*Service` → `*Controller`.
   - Start Javalin REST tại `http://localhost:7070`.
   - Gắn route trong `ApiRouter.setupRoutes(app)`.
2. **Start Client** (`com.client.Main` / `com.client.Launcher`)
   - Kết nối WebSocket qua `MyWebSocketClient.connectToServer()`.
   - Load FXML, mở scene UI.

#### Luồng B — Register
1. UI `RegisterController` thu dữ liệu.
2. `AuthNetwork.register()` → `POST /api/register`.
3. Server `AuthController.processRegisterRest()` → `AuthService.register()`.
4. `UserRepository.save(...)` + lưu theo role.
5. Trả `Response(SUCCESS/ERROR)` về client.

#### Luồng C — Login + tạo session cookie
1. UI `LoginController` gửi `AuthNetwork.login()` → `POST /api/login`.
2. Server `AuthController.processLoginRest()` xác thực `AuthService.login()`.
3. Nếu thành công: `ctx.sessionAttribute("username", ...)`.
4. Client nhận `Response` + lưu vào `ClientSession/UserSession`.
5. `NetworkClient` giữ cookie session nhờ `CookieManager` (các request sau không cần gửi lại username/password).

#### Luồng D — View Profile
1. UI `UserProfileController` gọi `AuthNetwork.getProfile()` → `GET /api/users/profile?username=...`.
2. Server `AuthController.getUserProfile()` → lấy user và map `UserProfileResponseDTO`.
3. Client render profile theo role.

#### Luồng E — Update Profile (đúng bản chất “không mất kết nối”)
1. UI thu dữ liệu update theo role:
   - `ProfileUpdateStrategy` chọn `IProfileStrategy` phù hợp, build DTO `*ProfileUpdateDTO`.
2. Client gọi `AuthNetwork.updateProfile()` → `PUT /api/users/update`.
3. Server `UpdateProfileCommand.execute()` → `UserService.updateProfile(...)`.
4. DAO `UserRepository.updateFullProfile(dto, role, userId)` update bảng `users` + bảng role.
5. Client parse `Response`:
   - Nếu server trả HTML/500 hoặc body không phải JSON: client trả `Response(ERROR, "HTTP ... - Response không phải JSON ...")` thay vì hiển thị nhầm “mất kết nối”.

#### Luồng F — Tạo Item
1. UI `CreateItemController` → `ItemNetwork.create...` (REST `POST /api/items`).
2. Server `CreateItemCommand` → `ItemService.createNewItem()`.
3. `ItemFactory` tạo subtype, `ItemRepository.saveItem()` lưu DB.

#### Luồng G — Tạo phiên Auction
1. UI gọi `POST /api/auctions` (qua `CreateAuctionCommand`).
2. `AuctionService.createAuction()`:
   - Lưu DB (`AuctionRepository.create/save`).
   - Đưa vào cache.
   - Schedule task kết thúc (executor).
3. Trả `Response(SUCCESS)`.

#### Luồng H — Xem phiên đang hoạt động
1. `AuctionNetwork.getActiveAuctions()` → `GET /api/auctions/active`.
2. `GetActiveAuctionsCommand` → `AuctionService.getActiveAuctions()`.
3. Client parse list `AuctionDetailDTO` và hiển thị.

#### Luồng I — Đặt giá (manual bid / auto-bid) + Realtime update
1. UI `ViewLiveAuctions` gọi REST:
   - Manual: `POST /api/auctions/bid` với `BidRequestDTO(auctionId,bidderId,amount)`.
   - Auto-bid: `POST /api/auctions/bid` với `enableAutoBid=true, maxAutoBidAmount=...`.
2. Server `PlaceBidCommand` → `AuctionService.placeBid()`:
   - Lock theo auction.
   - Validate: `BidValidationChain`.
   - Xử lý: `ManualBidProcessor` / `AutoBidProcessor`.
   - Anti-sniping: `AntiSnipingStrategy`.
   - Persist `BidTransactionRepository` + cập nhật `AuctionRepository`.
   - Emit event → `Broadcaster` broadcast `AUCTION_UPDATE:{AuctionUpdateDTO}`.
3. Client nhận realtime:
   - `MyWebSocketClient.onMessage()` parse `AuctionUpdateDTO`.
   - `Platform.runLater()` gọi `ViewLiveAuctions.updatePriceRealtime()`.

---

## 2) Module `shared/` (DTO + Network Contract)

### 2.1 `com.shared.network`
- `Request`: gói tin request dạng `{ action, payload(JSON string) }` (ghi chú: comment cho thấy từng dùng WebSocket).
- `Response`: envelope trả về cho client: `status`, `errorCode`, `message`, `data`.

### 2.2 `com.shared.dto` (DTO dùng cho REST/WebSocket)
- Auth/User:
  - `LoginRequestDTO`, `RegisterRequestDTO`
  - `UserProfileResponseDTO`
  - `IUserProfileDTO` (interface)
  - `BaseProfileUpdateDTO` + phân nhánh theo role:
    - `AdminProfileUpdateDTO`, `BidderProfileUpdateDTO`, `SellerProfileUpdateDTO`
- Item:
  - `CreateItemRequestDTO`
  - `ItemResponseDTO`
- Auction/Bidding:
  - `CreateAuctionDTO`
  - `AuctionDetailDTO`, `AuctionUpdateDTO`
  - `BidRequestDTO`, `BidHistoryDTO`
  - Auto-bid:
    - `AutoBidCancelDTO`, `AutoBidUpdateDTO`

### 2.3 `com.shared.Utils`
- `Utils`: tiện ích dùng chung (tùy nội dung hiện có trong file).

---

## 3) Module `Bidding_Server/` (REST API + WebSocket + DB)

### 3.1 Entry point
- `com.server.ServerApp`
  - Chạy **WebSocket** trên `localhost:8080`.
  - Chạy **REST API (Javalin)** trên `localhost:7070`.
  - Lắp ráp dependency thủ công (DI đơn giản): tạo `*Repository`, `*Service`, `*Controller`, sau đó `ApiRouter.setupRoutes(app)`.
  - Nhận message WebSocket theo format: `PLACE_BID:JSON`, `CANCEL_AUTO_BID:JSON`, `UPDATE_AUTO_BID:JSON`.

### 3.2 Cấu hình / Hạ tầng
- `com.server.config.DBConnection`
  - Singleton + HikariCP pool kết nối MySQL `auction_db`.
- `com.server.config.WebSocketConfig`
  - Lớp cấu hình (hiện có file).

### 3.3 Router (REST endpoints)
- `com.server.route.ApiRouter`: khai báo endpoint chính (đang dùng).
- `com.server.route.LoginRoute`, `com.server.route.RegisterRoute`: wrapper dạng `Handler` (có thể là route kiểu cũ/để tham khảo).

**Danh sách endpoint đang khai báo trong `ApiRouter`:**

**Auth/User**
- `POST /api/login` → `AuthController.processLoginRest`
- `POST /api/register` → `AuthController.processRegisterRest`
- `GET /api/users/profile?username=...` → `AuthController.getUserProfile`
- `PUT /api/users/update` → `UpdateProfileCommand` (gọi `UserService`)

**Item**
- `GET /api/items` → `GetAllItemsCommand` → `ItemService.getAllItemsDTO()`
- `POST /api/items` → `CreateItemCommand` → `ItemService.createNewItem()`

**Auction**
- `POST /api/auctions` → `CreateAuctionCommand` → `AuctionService.createAuction()`
- `GET /api/auctions/active` → `GetActiveAuctionsCommand` → `AuctionService.getActiveAuctions()`
- `POST /api/auctions/bid` → `PlaceBidCommand` → `AuctionService.placeBid()`
- `GET /api/auctions/:auctionId` → `GetAuctionDetailCommand` → `AuctionService.getAuctionDetail()`
- `GET /api/auctions/:auctionId/bids` → `GetBidHistoryCommand` → `AuctionService.getBidHistory()`
- Auto-bid:
  - `POST /api/auctions/:auctionId/auto-bid/cancel` → `CancelAutoBidCommand`
  - `PUT /api/auctions/:auctionId/auto-bid/update` → `UpdateAutoBidAmountCommand`

**Admin**
- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `GET /api/admin/users/search/:username`
- `POST /api/admin/users/ban`
- `POST /api/admin/users/unban`
- `POST /api/admin/sellers/approve`
- `GET /api/admin/items/analytics`
- `GET /api/admin/finance/revenue-estimate`
- `GET /api/admin/users/:username/activity`
- `POST /api/admin/auctions/cancel`

### 3.4 Controller layer
- `com.server.controller.AuthController`
  - Register/Login (REST) + session `ctx.sessionAttribute("username", ...)`.
  - Lấy profile theo query param `username`.
- `com.server.controller.AuctionController`
  - CRUD/Query liên quan đấu giá: tạo phiên, list active, đặt giá, chi tiết, lịch sử bid, cancel/update auto-bid.
- `com.server.controller.AdminController`
  - Dashboard, tìm user, ban/unban, approve seller, analytics, estimate revenue… (nhiều API trả placeholder/TODO).
- `com.server.controller.UserController`, `ImageController`, `TransactionController`
  - Có file class (một số có thể là khung mở rộng).
- `com.server.controller.ClientHandler`
  - File hiện tại **trống** (placeholder cho hướng Socket/TCP cũ).

### 3.5 Command layer (Template Method)
Nhóm command dùng để nhét thẳng vào router Javalin (implements `Handler`).

- `com.server.controller.command.BaseApiCommand`
  - Template method: `handle()` chuẩn hóa try/catch → gọi `execute(ctx)`.
  - Dùng `ResponseUtils` để đóng gói JSON lỗi.

Các command nghiệp vụ:
- `CreateItemCommand`, `GetAllItemsCommand`
- `CreateAuctionCommand`, `GetActiveAuctionsCommand`, `PlaceBidCommand`
- `GetAuctionDetailCommand`, `GetBidHistoryCommand`
- Auto-bid: `CancelAutoBidCommand`, `UpdateAutoBidAmountCommand`
- User: `UpdateProfileCommand`

### 3.6 Service layer
- `AuthService`
  - Validate input (username/password/email/fullName).
  - `register()` tạo `Bidder` + lưu DB qua `UserRepository`.
  - `login()` trả về `UserProfileResponseDTO`.
  - `getUserProfile()`.
- `UserService`
  - OCP mapping theo role (Admin/Bidder/Seller) → map trả về `Response.data` dạng `Map`.
  - `updateProfile()` dùng `UserRepository.updateFullProfile()`.
  - `changePassword()`.
- `ItemService`
  - `getAllItemsDTO()` chuyển `Item` → `ItemResponseDTO`.
  - `createNewItem()` dùng `ItemFactory` để dựng đúng subtype rồi lưu qua `ItemRepository`.
- `AuctionService` (trung tâm nghiệp vụ đấu giá)
  - Cache phiên đấu giá đang chạy (`auctionCache`), lock theo auction (`ReentrantLock`).
  - `placeBid()`:
    - Validate bằng `BidValidationChain`.
    - Process manual bid qua `BidProcessor`.
    - Anti-sniping (`AntiSnipingStrategy`) để gia hạn giờ nếu bid sát giờ.
    - Auto-bid: đăng ký + `AutoBidProcessor` xử lý phản bid.
    - Gửi realtime update qua `AuctionEventListener` (`Broadcaster`).
  - `getActiveAuctions()`, `getAuctionDetail()`, `getBidHistory()`.
  - `createAuction()` tạo phiên mới, lưu DB, đưa vào cache và schedule kết thúc.
- `AdminService`, `SellerService`, `BidderService`
  - Các service hỗ trợ nghiệp vụ quản trị/role (một số phương thức đang dạng demo/TODO).

#### 3.6.1 Auction sub-layer (strategy/processor/logging)
- `com.server.service.auction.strategy`
  - `BidValidationChain` + các validation strategies:
    - `BidValidationStrategy`
    - `BasicBidValidation`
    - `MinimumIncrementValidation`
    - `ReservePriceValidation` *(hiện file đang trống/placeholder)*
- `com.server.service.auction.processor`
  - `BidProcessor` (interface/abstraction)
  - `ManualBidProcessor`
  - `AutoBidProcessor`
- `com.server.service.auction.antisnipe`
  - `AntiSnipingStrategy`
  - `DefaultAntiSnipingStrategy`
- `com.server.service.auction.logging`
  - `IAuctionLogger`, `AuctionLogger`

### 3.7 DAO/Repository layer (SQL)
Các lớp trong `com.server.DAO` (đúng vai trò “DAO/Repository”):

**User & Role**
- `IUserRepository` (interface)
- `UserRepository`
  - Save user theo role (Strategy nội bộ `RoleDataSaver`).
  - Update full profile theo role (`RoleDataUpdater`).
  - Query user theo username/id.
- `UserQueryFactory`: build query join bảng theo role.
- `UserRowMapperFactory`: chọn mapper theo role.
- `AdminRepository`, `SellerRepository`, `BidderRepository` (+ interfaces tương ứng `IAdminRepository`, `ISellerRepository`, `IBidderRepository`).

**Item**
- `ItemRepository`
  - `saveItem(Item)` insert theo subtype (Electronics/Art/Vehicle).
  - `getAllItems()` query + map result set → đúng subtype.

**Auction/Bid/AutoBid**
- `IAuctionRepository` + `AuctionRepository`
  - `create()`, `save()`, `findById()`, `findByStatusIn()`.
  - `findBidHistoryByAuction()`.
- `IBidTransactionRepository` + `BidTransactionRepository`
- `IAutoBidRepository` + `AutoBidRepository`
- `AuctionEventLogRepository` (log sự kiện đấu giá)

### 3.8 Domain Model layer (`com.server.model`)

**Base**
- `Entity`: base entity (id + timestamps… tùy triển khai)

**User hierarchy**
- `User` (abstract)
- `Bidder`, `Seller`, `Admin`
- Enums: `Role`, `Status`

**Item hierarchy**
- `Item` (abstract)
- `Electronics`, `Art`, `Vehicle`
- `ItemFactory`: factory dựng subtype từ DTO/type/extraProps.

**Auction & Transaction**
- `Auction`
- `BidTransaction`
- `AutoBidTracker`
- `AuctionEventLog`
- `PaymentProcessor` (khung xử lý thanh toán)

### 3.9 WebSocket realtime layer (`com.server.websocket`)
- `AuctionEventListener`: interface.
- `Broadcaster`: lưu danh sách client WebSocket + broadcast `AUCTION_UPDATE:<json>`.

### 3.10 Exception / Error handling
- `com.server.exception.*`: nhóm exception nghiệp vụ: Auth, Auction, Permission, Duplicate…
- `com.server.util.ResponseUtils`: chuẩn hóa `Response` khi lỗi/validation/internal.

### 3.11 Test classes (JUnit)
Trong `Bidding_Server/src/test/java/com/server/...`:
- `AuthControllerTest`
- `AuthServiceTest`
- `AdminServiceTest`
- `AuctionServiceTest`
- `AuctionServiceIntegrationTest`

---

## 4) Module `Bidding_Client/` (JavaFX + HTTP + WebSocket)

### 4.1 Entry point
- `com.client.Main`: mở UI (FXML `AuctionMenu.fxml`), trước đó gọi `MyWebSocketClient.connectToServer()`.
- `com.client.Launcher`: lớp launcher (thường dùng để chạy JavaFX khi đóng gói).

### 4.2 UI Controller (JavaFX)

**Auth** (`com.client.controller.auth`)
- `ClientApp`
- `LoginController`
- `RegisterController`

**Dashboard / Auction / Item** (`com.client.controller.dashboard`)
- `DashboardController`
- `ViewDashboardController`
- `ViewLiveAuctions` (màn hình đấu giá realtime: refresh list, đặt bid, auto-bid cancel/update, nhận `AuctionUpdateDTO` qua websocket)
- `CreateItemController`
- `MyInventoryController`
- `UserProfileController`
- `AdminDashboardController`

**Strategy cho Profile UI** (`com.client.controller.dashboard.strategy`)
- `IProfileStrategy`
- `ProfileUpdateStrategy`
- `AdminProfileStrategy`, `BidderProfileStrategy`, `SellerProfileStrategy`

### 4.3 Network/HTTP layer (`com.client.network`)

**HTTP Client dùng chung**
- `NetworkClient`
  - `HttpClient` dùng chung + `CookieManager` để giữ session/cookie.

**REST clients (gọi Javalin 7070)**
- `AuthNetwork`
  - `POST /api/register`, `POST /api/login`
  - `GET /api/users/profile?username=...`
  - `PUT /api/users/update`
- `ItemNetwork`
  - `GET /api/items` (async)
  - `POST /api/items` (async)
- `AuctionNetwork`
  - `GET /api/auctions/active`
  - `GET /api/auctions/{id}`
  - `POST /api/auctions/bid`
  - Auto-bid cancel/update.
- `AdminNetwork`, `SellerNetwork`, `BidderNetwork`
  - Có lớp network (cần đối chiếu endpoint server; một số có thể là planned/mở rộng).

**WebSocket**
- `MyWebSocketClient`
  - Singleton kết nối `ws://localhost:8080`.
  - Nhận tin `AUCTION_UPDATE:` và gọi `ViewLiveAuctions.updatePriceRealtime()`.

**Socket/TCP cũ**
- `SocketClient`: file hiện tại **trống**.

### 4.4 Session & Utilities
- `com.client.session.ClientSession`, `UserSession`: lưu trạng thái đăng nhập/user.
- `com.client.util.*`: điều hướng scene, animation, switch pane…

### 4.5 Resources (FXML/CSS/Image)
- `Bidding_Client/src/main/resources/fxml/*.fxml` (Login, Register, Dashboard, ViewLiveAuctions…)
- `login-style.css`
- `resources/images/*` (ảnh minh họa UI)

---

## 5) Báo cáo theo tính năng (User → Item → Auction)

### 5.1 User/Auth (đăng ký/đăng nhập/hồ sơ)

**Shared DTO**
- `LoginRequestDTO`, `RegisterRequestDTO`, `UserProfileResponseDTO`
- `BaseProfileUpdateDTO` (+ các DTO theo role)

**Server**
- Route: `POST /api/register`, `POST /api/login`, `GET /api/users/profile`, `PUT /api/users/update`
- Controller: `AuthController`
- Service: `AuthService`, `UserService`
- DAO: `UserRepository` (+ factories, mappers)
- Model: `User`, `Bidder`, `Seller`, `Admin`, `Role`, `Status`

**Client**
- UI: `LoginController`, `RegisterController`, `UserProfileController`
- Network: `AuthNetwork`
- Session: `ClientSession` / `UserSession`

**Ghi chú**
- `AuthController.processLoginRest()` có lưu session attribute `username` (cookie/session do Javalin quản lý).
- `NetworkClient` dùng `CookieManager` để giữ cookie giữa các request.

### 5.2 Item (tạo item / xem danh sách)

**Shared DTO**
- `CreateItemRequestDTO`, `ItemResponseDTO`

**Server**
- Route: `GET /api/items`, `POST /api/items`
- Command: `GetAllItemsCommand`, `CreateItemCommand`
- Service: `ItemService`
- DAO: `ItemRepository`
- Model: `Item` + (`Electronics`, `Art`, `Vehicle`), `ItemFactory`

**Client**
- UI: `CreateItemController`, `MyInventoryController`, `DashboardController`
- Network: `ItemNetwork`

### 5.3 Auction/Bidding (phiên đấu giá, bid, auto-bid, realtime)

**Shared DTO**
- `CreateAuctionDTO`
- `AuctionDetailDTO`, `AuctionUpdateDTO`
- `BidRequestDTO`, `BidHistoryDTO`
- `AutoBidCancelDTO`, `AutoBidUpdateDTO`

**Server**
- Route: nhóm `/api/auctions/*`
- Command/Controller:
  - Commands: `CreateAuctionCommand`, `GetActiveAuctionsCommand`, `PlaceBidCommand`, `GetAuctionDetailCommand`, `GetBidHistoryCommand`, `CancelAutoBidCommand`, `UpdateAutoBidAmountCommand`
  - (Có thêm `AuctionController` với các method tương ứng — hiện tại router đang gắn bằng Command cho nhiều endpoint.)
- Service:
  - `AuctionService` + subpackages strategy/processor/antisnipe/logging
- DAO:
  - `AuctionRepository`, `BidTransactionRepository`, `AutoBidRepository`, `AuctionEventLogRepository`
- WebSocket:
  - `ServerApp` nhận message
  - `Broadcaster` push `AUCTION_UPDATE:`

**Client**
- UI: `ViewLiveAuctions`
- REST: `AuctionNetwork`
- WebSocket: `MyWebSocketClient`

**Realtime message format**

- **Server → Client (đang dùng)**:
  - `AUCTION_UPDATE:{AuctionUpdateDTO json}`

- **Client → Server (Server có hỗ trợ, client hiện tại chưa dùng trực tiếp)**:
  - `PLACE_BID:{BidRequestDTO json}`
  - `CANCEL_AUTO_BID:{AutoBidCancelDTO json}`
  - `UPDATE_AUTO_BID:{AutoBidUpdateDTO json}`

---

## 6) Phụ lục A — Danh sách lớp theo module (đã rà soát theo source tree)

### 6.1 `shared/src/main/java`
- `com.shared.Utils`
- `com.shared.network`: `Request`, `Response`
- `com.shared.dto`:
  - `AdminProfileUpdateDTO`, `AuctionDetailDTO`, `AuctionUpdateDTO`, `AutoBidCancelDTO`, `AutoBidUpdateDTO`
  - `BaseProfileUpdateDTO`, `BidderProfileUpdateDTO`, `SellerProfileUpdateDTO`
  - `BidHistoryDTO`, `BidRequestDTO`
  - `CreateAuctionDTO`, `CreateItemRequestDTO`
  - `ItemResponseDTO`
  - `IUserProfileDTO`
  - `LoginRequestDTO`, `RegisterRequestDTO`
  - `UserProfileResponseDTO`

### 6.2 `Bidding_Server/src/main/java`
- Root: `ServerApp`
- `config`: `DBConnection`, `WebSocketConfig`
- `route`: `ApiRouter`, `LoginRoute`, `RegisterRoute`
- `controller`:
  - `AuthController`, `AuctionController`, `AdminController`, `UserController`, `ImageController`, `TransactionController`, `ClientHandler` (trống)
- `controller.command`:
  - `BaseApiCommand`, `CreateItemCommand`, `GetAllItemsCommand`
  - `CreateAuctionCommand`, `GetActiveAuctionsCommand`, `PlaceBidCommand`
  - `GetAuctionDetailCommand`, `GetBidHistoryCommand`
  - `CancelAutoBidCommand`, `UpdateAutoBidAmountCommand`
  - `UpdateProfileCommand`
- `service`: `AuthService`, `UserService`, `ItemService`, `AuctionService`, `AdminService`, `SellerService`, `BidderService`
- `service.auction`:
  - `antisnipe`: `AntiSnipingStrategy`, `DefaultAntiSnipingStrategy`
  - `logging`: `IAuctionLogger`, `AuctionLogger`
  - `processor`: `BidProcessor`, `ManualBidProcessor`, `AutoBidProcessor`
  - `strategy`: `BidValidationStrategy`, `BidValidationChain`, `BasicBidValidation`, `MinimumIncrementValidation`, `ReservePriceValidation` *(placeholder)*
- `DAO`:
  - `UserRepository`, `UserQueryFactory`, `UserRowMapperFactory`
  - `ItemRepository`
  - `AuctionRepository`, `BidTransactionRepository`, `AutoBidRepository`, `AuctionEventLogRepository`
  - `AdminRepository`, `SellerRepository`, `BidderRepository`
  - Interfaces: `IUserRepository`, `IAuctionRepository`, `IAutoBidRepository`, `IBidTransactionRepository`, `IAdminRepository`, `ISellerRepository`, `IBidderRepository`
- `model`:
  - `Entity`, `User`, `Admin`, `Bidder`, `Seller`, `Role`, `Status`
  - `Item`, `Electronics`, `Art`, `Vehicle`, `ItemFactory`
  - `Auction`, `BidTransaction`, `AutoBidTracker`, `AuctionEventLog`
  - `PaymentProcessor`
- `websocket`: `AuctionEventListener`, `Broadcaster`
- `util`: `ResponseUtils`
- `exception`: toàn bộ lớp exception trong package `com.server.exception`

### 6.3 `Bidding_Server/src/test/java`
- `AuthControllerTest`, `AuthServiceTest`, `AdminServiceTest`, `AuctionServiceTest`, `AuctionServiceIntegrationTest`

### 6.4 `Bidding_Client/src/main/java`
- Root: `Launcher`, `Main`
- `controller.auth`: `ClientApp`, `LoginController`, `RegisterController`
- `controller.dashboard`: `AdminDashboardController`, `CreateItemController`, `DashboardController`, `MyInventoryController`, `UserProfileController`, `ViewDashboardController`, `ViewLiveAuctions`
- `controller.dashboard.strategy`: `IProfileStrategy`, `ProfileUpdateStrategy`, `AdminProfileStrategy`, `BidderProfileStrategy`, `SellerProfileStrategy`
- `network`: `NetworkClient`, `AuthNetwork`, `ItemNetwork`, `AuctionNetwork`, `AdminNetwork`, `SellerNetwork`, `BidderNetwork`, `MyWebSocketClient`, `SocketClient` (trống)
- `session`: `ClientSession`, `UserSession`
- `util`: `DashboardNavigation`, `DashboardSearchBridge`, `SceneController`, `SmallAnimation`, `SpriteAnimation`, `SwitchPane`

---

## 7) Phụ lục B — Gợi ý cải tiến báo cáo/đối chiếu

- Đối chiếu thêm DB schema bằng `script_db.sql` để map bảng ↔ DAO (items, users, bidders, sellers, auctions, bid_transactions, auto_bids…).
- Đối chiếu `AdminNetwork/SellerNetwork/BidderNetwork` với endpoint server hiện tại để biết phần nào là **đã có** và phần nào là **planned**.
- Nếu cần “bản báo cáo dạng bảng”: có thể xuất matrix **Feature → Endpoint → Command/Controller → Service → DAO → Model/DTO**.


