# 📊 Phân Tích Hệ Thống Đấu Giá - Team13

## 🎯 TÌNH HÌNH HIỆN TẠI

### ✅ **Các Phần Đã Hoàn Thành**

#### 1. **Database Schema** (✅ HOÀN CHỈNH)
```sql
✓ users              - Bảng người dùng (gốc)
✓ admins             - Bảng quản trị viên (kế thừa users)
✓ bidders            - Bảng người đấu giá (kế thừa users)
✓ sellers            - Bảng người bán (kế thừa bidders)
✓ items              - Bảng sản phẩm (hỗ trợ Electronics, Art, Vehicle)
✓ auctions           - Bảng phiên đấu giá (10 cột)
✓ bid_transactions   - Bảng lịch sử bid (hỗ trợ manual & auto)
✓ auto_bids          - Bảng đặt giá tự động (với unique constraint)
```

**Khả năng:**
- Lưu trữ đầy đủ thông tin phiên đấu giá (thời gian, giá, người thắng)
- Hỗ trợ lịch sử bid đầy đủ
- Quản lý auto-bid tách biệt an toàn

---

#### 2. **Model Classes** (✅ HOÀN CHỈNH)
```java
✓ Auction            - Phiên đấu giá (extends Entity)
  - Các thuộc tính: itemId, sellerId, status, timeframe, giá
  - Enum Status: OPEN, RUNNING, FINISHED, CANCELED
  - Auto-timestamp khi thay đổi trạng thái

✓ BidTransaction     - Giao dịch bid (extends Entity)
  - Thuộc tính: auctionId, bidderId, bidAmount, timestamp, isAutoBid
  - Constructor đầy đủ, getters/setters chuẩn

✓ AutoBidTracker     - Quản lý auto-bid
  - Thuộc tính: auctionId, bidderId, maxBidAmount, isActive
```

---

#### 3. **Repository Layer** (✅ HOÀN CHỈNH)

**AuctionRepository:**
```java
✓ findById(long)                    - Tìm phiên theo ID
✓ findByStatusIn(List<Status>)     - Tìm các phiên đang hoạt động
✓ create(Auction)                   - Tạo phiên mới (trả về ID)
✓ save(Auction)                     - Cập nhật phiên (tên sai: nên là update)
✓ findBidHistoryByAuction(long)    - Lấy lịch sử bid
✓ findItemNameByItemId(long)       - Lấy tên sản phẩm
```

**BidTransactionRepository:**
```java
✓ save(BidTransaction)              - Lưu bid vào DB
✓ findByAuction(long)               - Lấy tất cả bid
✓ findBidHistory(long, int)         - Lấy bid với giới hạn
✓ findLatestBid(long)               - Lấy bid cuối cùng
```

**AutoBidRepository:**
```java
✓ saveOrUpdate(AutoBidTracker)      - Lưu hoặc cập nhật auto-bid
✓ findByAuctionAndBidder()          - Tìm auto-bid của người dùng
✓ findAllActiveByAuction()          - Lấy tất cả auto-bid hoạt động
✓ deactivate(long, long)            - Vô hiệu hóa auto-bid
✓ delete(long, long)                - Xóa auto-bid
```

**Đánh giá:** Sử dụng HikariCP Connection Pool, PreparedStatement chuẩn, BigDecimal cho tiền tệ

---

#### 4. **Service Layer** (✅ HOÀN CHỈNH)

**AuctionService (397 dòng):**

**Concurrent Collections:**
```java
✓ ConcurrentHashMap<Long, Auction>           - Cache phiên đấu giá
✓ ConcurrentHashMap<Long, ReentrantLock>    - Lock per auction
✓ ConcurrentHashMap<Long, ScheduledFuture<?>> - Scheduled tasks
✓ LinkedBlockingQueue<BidTransaction>        - Async bid queue
✓ ScheduledExecutorService (10 threads)      - Scheduling auctions end
```

**Business Methods:**
```java
✓ placeBid(BidRequestDTO)           - Đặt giá với validation & lock
✓ getAuctionDetail(long)            - Chi tiết phiên (cache-first)
✓ getActiveAuctions()               - Danh sách phiên hoạt động
✓ cancelAutoBid(long, long)         - Hủy auto-bid
✓ updateAutoBidAmount()             - Cập nhật giá tối đa
✓ setEventListener()                - Gắn listener cho broadcast
✓ shutdown()                        - Cleanup resources
```

**Anti-Sniping:**
```java
✓ handleAntiSniping(Auction)        - Extend thời gian nếu bid ở phút cuối
✓ DefaultAntiSnipingStrategy         - 30s extension
```

**Threading Model:**
```java
✓ ReentrantLock per auction         - Chống race condition
✓ ScheduledExecutorService          - End auction scheduling
✓ LinkedBlockingQueue + Thread      - Async bid persistence
```

---

#### 5. **Validation Chain** (✅ HOÀN CHỈNH)

**Chain of Responsibility Pattern:**
```java
✓ BidValidationChain               - Orchestrator
  ├─ BasicBidValidation            - Auction status, amount > 0
  ├─ MinimumIncrementValidation    - Bid >= current + stepPrice
  └─ ReservePriceValidation        - Bid >= starting price
```

---

#### 6. **Bid Processing** (✅ HOÀN CHỈNH)

**Manual Bid:**
```java
✓ ManualBidProcessor.process()
  - Cập nhật highest bid
  - Set winner ID
  - Lưu vào bidQueue (async)
  - Auto OPEN → RUNNING
```

**Auto Bid:**
```java
✓ AutoBidProcessor.process()
  - Lấy tất cả auto-bid hoạt động
  - Tự động nâng giá theo rule
  - Hỗ trợ increment step
```

---

#### 7. **Controller** (✅ HOÀN CHỈNH)

**AuctionController (138 dòng):**
```java
✓ getActiveAuctions(Context)       - GET /api/auctions/active
✓ placeBid(Context)                - POST /api/auctions/bid
✓ getAuctionDetail(Context)        - GET /api/auctions/{auctionId}
✓ cancelAutoBid(Context)           - POST /api/auctions/{auctionId}/auto-bid/cancel
✓ updateAutoBidAmount(Context)     - PUT /api/auctions/{auctionId}/auto-bid/update
✓ handleAuctionException()         - Custom error handling
```

---

#### 8. **Routes Setup** (✅ HOÀN CHỈNH)

**ApiRouter:**
```java
✓ POST   /api/auth/login
✓ POST   /api/auth/register
✓ GET    /api/items
✓ POST   /api/items
✓ GET    /api/auctions/active          ← Auction
✓ POST   /api/auctions/bid             ← Auction
✓ GET    /api/auctions/{auctionId}     ← Auction
✓ POST   /api/auctions/{id}/auto-bid/cancel
✓ PUT    /api/auctions/{id}/auto-bid/update
```

---

#### 9. **Logging** (✅ HOÀN CHỈNH)

**AuctionLogger:**
```java
✓ logBidPlaced()       - Ghi lại khi người dùng đặt giá
✓ logAutoBidEnabled()  - Ghi auto-bid được kích hoạt
✓ logAutoBidDisabled() - Ghi auto-bid bị hủy
✓ logTimeExtended()    - Ghi giờ được gia hạn
✓ logAuctionFinished() - Ghi phiên kết thúc
✓ logError()           - Ghi lỗi
✓ logInfo()            - Thông tin tổng quát
```

---

#### 10. **Exception Handling** (✅ HOÀN CHỈNH)

**AuctionException:**
```java
✓ ErrorCode enum:
  - AUCTION_NOT_FOUND
  - AUCTION_NOT_ACTIVE
  - INVALID_BID_AMOUNT
  - BID_AMOUNT_TOO_LOW
  - INVALID_AUTO_BID_CONFIG
  - AUCTION_ALREADY_FINISHED
```

---

#### 11. **Server Initialization** (✅ HOÀN CHỈNH)

**ServerApp.main():**
```java
✓ DB Connection check (HikariCP)
✓ WebSocket server (port 8080)
✓ REST API server (port 7070)
✓ Dependency Injection:
  - UserRepository → AuthService → AuthController
  - ItemRepository → ItemService
  - AuctionRepository + BidTransactionRepository + AutoBidRepository
    → BidValidationChain + Processors → AuctionService → AuctionController
✓ Event Listener (Broadcaster) gắn vào AuctionService
```

---

### ⚠️ **Các Phần CẦN HOÀN THIỆN**

#### 1. **WebSocket Message Handling** (❌ CHƯA IMPLEMENT)

**Hiện trạng:**
```java
// ServerApp.java - Line 37-40
@Override 
public void onMessage(WebSocket conn, String message) {
    System.out.println("nhận lệnh đấu giá: " + message);
    /// TODO: Code đấu giá, xử lí các lệnh Websocker khác
}
```

**Cần làm:**
- Parse JSON message từ client
- Route message đến AuctionService (thông qua Broadcaster)
- Gửi real-time update cho tất cả clients

**Chi tiết cần implement:**
```java
// Message type
- "PLACE_BID"     → placeBid()
- "CANCEL_AUTO"   → cancelAutoBid()
- "UPDATE_AUTO"   → updateAutoBidAmount()
```

---

#### 2. **Broadcaster Implementation** (⚠️ KHỞI TẠO NHƯNG CHƯA XỬ LÝ)

**Hiện trạng:**
```java
// ServerApp.java - Line 72, 79
Broadcaster broadcaster = new Broadcaster();
auctionService.setEventListener(broadcaster);
```

**Cần làm:**
- Implement `AuctionEventListener` interface trong Broadcaster
- Phương thức `onAuctionUpdate(AuctionUpdateDTO)` cần gửi WebSocket message

**Chi tiết:**
```java
public class Broadcaster implements AuctionEventListener {
    private static Set<WebSocket> clients = ConcurrentHashMap.newKeySet();
    
    public static void addClient(WebSocket conn) { clients.add(conn); }
    public static void removeClient(WebSocket conn) { clients.remove(conn); }
    
    @Override
    public void onAuctionUpdate(AuctionUpdateDTO update) {
        // Gửi JSON update đến tất cả clients
        String json = gson.toJson(update);
        clients.forEach(ws -> {
            if (ws.isOpen()) {
                ws.send(json);
            }
        });
    }
}
```

---

#### 3. **Create Auction API** (❌ CHƯA CÓ)

**Yêu cầu:**
- Endpoint: `POST /api/auctions`
- Input: `CreateAuctionDTO` (itemId, sellerId, startTime, endTime, stepPrice)
- Output: `AuctionDetailDTO` (phiên đấu giá mới tạo)

**Cần làm:**
```java
// AuctionController.java
public void createAuction(Context ctx) {
    CreateAuctionDTO request = gson.fromJson(ctx.body(), CreateAuctionDTO.class);
    long auctionId = auctionService.createAuction(request);
    // Return chi tiết
}

// AuctionService.java
public long createAuction(CreateAuctionDTO dto) {
    Auction auction = new Auction();
    auction.setItemId(dto.getItemId());
    auction.setSellerId(dto.getSellerId());
    // ... setup khác
    long id = auctionRepository.create(auction);
    cacheAndScheduleAuction(auction); // Cache ngay
    return id;
}

// ApiRouter.java
app.post("/api/auctions", new CreateAuctionCommand(auctionService));
```

---

#### 4. **Get Bid History API** (❌ CHƯA CÓ)

**Yêu cầu:**
- Endpoint: `GET /api/auctions/{auctionId}/bids`
- Output: List<BidHistoryDTO>

**Cần làm:**
```java
// AuctionController.java
public void getBidHistory(Context ctx) {
    long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
    List<BidHistoryDTO> history = auctionService.getBidHistory(auctionId);
    ctx.json(new Response("SUCCESS", "Loaded", history));
}

// AuctionService.java
public List<BidHistoryDTO> getBidHistory(long auctionId) {
    return bidRepository.findByAuction(auctionId)
        .stream()
        .map(bid -> new BidHistoryDTO(
            bid.getBidderId(),
            "User_" + bid.getBidderId(),
            bid.getBidAmount(),
            bid.getTimestamp(),
            bid.isAutoBid()
        ))
        .toList();
}

// ApiRouter.java
app.get("/api/auctions/{auctionId}/bids", new GetBidHistoryCommand(auctionService));
```

---

#### 5. **Test Edge Cases** (❌ CHƯA TEST KỸ)

**Các scenario cần kiểm tra:**

| Scenario | Trạng thái | Chi tiết |
|----------|-----------|---------|
| Bid ở giây cuối | ⚠️ | Anti-sniping mở rộng time, cần verify callback |
| Race condition | ⚠️ | ReentrantLock đã có, nhưng cần test concurrent |
| Auto-bid 3 người | ⚠️ | AutoBidProcessor loop qua tất cả, cần test |
| Auction end | ⚠️ | ScheduledExecutorService gọi finishAuction, cần verify cache clear |
| Bid queue overflow | ⚠️ | LinkedBlockingQueue unbounded, có thể memory leak |

---

#### 6. **Performance Optimization** (⚠️ ĐÁNG LƯỚI)

| Vấn đề | Hiện trạng | Đề xuất |
|-------|-----------|---------|
| Bid queue | Unbounded | Nên set max capacity (1000) để chống memory leak |
| Cache preload | Full load startup | OK, nhưng có thể delay lớn nếu DB lớn |
| Lock granularity | Per-auction | OK, đủ chi tiết |
| DB query | Mỗi save bid | OK, HikariCP pool connection |

---

#### 7. **Documentation** (❌ CHƯA VIẾT)

**Cần viết:**
- API Documentation (Swagger/OpenAPI)
- WebSocket Protocol Spec
- Database Schema Diagram
- Thread Safety Notes
- Error Code Reference

---

## 🔄 **Thread Handling - PHÂN TÍCH CHI TIẾT**

### **Đã Implement:**

#### 1. **ExecutorService** ✅
```java
// AuctionService.java - Line 45
private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

// Sử dụng để:
// 1. Schedule auction end (scheduleAuctionEnd)
// 2. Hủy task cũ khi gia hạn time
// 3. Cleanup resources
```

**Khả năng:**
- 10 threads chạy parallel
- Xử lý 10 phiên end cùng lúc
- Có bộ đệm task pending

**Đánh giá:** ✅ Đủ dùng

---

#### 2. **ReentrantLock** ✅
```java
// AuctionService.java - Line 40
private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

// Sử dụng:
// - Lock trước khi read/modify auction object
// - Chống race condition: 2 người bid cùng lúc trên 1 phiên
// - Reentrant: cùng thread có thể lock nhiều lần
```

**Code:**
```java
ReentrantLock lock = auctionLocks.get(request.getAuctionId());
lock.lock();
try {
    Auction auction = auctionCache.get(request.getAuctionId());
    // Modify
    bidProcessor.process(...);
} finally {
    lock.unlock();
}
```

**Đánh giá:** ✅ Chuẩn xác, ko deadlock risk

---

#### 3. **LinkedBlockingQueue** ✅
```java
// AuctionService.java - Line 44
private final LinkedBlockingQueue<BidTransaction> bidQueue = new LinkedBlockingQueue<>();

// Sử dụng:
// - Producer (placeBid): bidQueue.offer(transaction)
// - Consumer (bid-processor thread): bidQueue.take()
// - Async DB persistence
```

**Flow:**
```
Client bid → Lock + validate → Update memory → Queue → (Async) → DB
```

**Đánh giá:** ✅ Giúp request trả về nhanh

**⚠️ Issue:** Unbounded queue có thể memory leak
```java
// Đề xuất:
private final LinkedBlockingQueue<BidTransaction> bidQueue 
    = new LinkedBlockingQueue<>(1000); // Bounded
```

---

#### 4. **Background Thread** ✅
```java
// AuctionService.java - Line 296-315 (startBidQueueProcessor)
Thread processor = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            BidTransaction bid = bidQueue.take(); // Blocking
            try {
                bidRepository.save(bid);
            } catch (Exception e) {
                // Log lỗi
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}, "bid-processor");
processor.setDaemon(true);
processor.start();
```

**Đánh giá:** ✅ Chuẩn xác, Daemon thread cleanup khi shutdown

---

#### 5. **ConcurrentHashMap** ✅
```java
// AuctionService.java - Line 39-41
private final ConcurrentHashMap<Long, Auction> auctionCache;
private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks;
private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks;

// Lợi ích:
// - Atomic operations (putIfAbsent, compute)
// - Concurrent read (segmented locking)
// - Ko cần synchronized block
```

**Đánh giá:** ✅ Đúng cách dùng

---

### **KHÔNG Sử Dụng (và Ko cần):**

#### ❌ **Callable / Future** - Không cần
- Dùng cho: async task cần return value
- Dự án: ExecutorService đã đủ dùng

#### ❌ **Thread Pool cho bid processing** - Không cần
- Dùng LinkedBlockingQueue + 1 thread là chuẩn
- Tránh context switch overhead

#### ❌ **ReadWriteLock** - Không cần
- Dùng cho: read heavy workload
- Dự án: Auction modify frequently, ReentrantLock đơn giản hơn

#### ❌ **Volatile** - Không cần
- ConcurrentHashMap đã volatile
- Atomic operations đã handle memory visibility

---

## 📋 **TỔNG HỢP CÔNG VIỆC CẦN LÀM**

### **P0 - CRITICAL (Cần làm ngay để app chạy)**

| # | Công việc | Ước lượng | Ghi chú |
|---|----------|----------|---------|
| 1 | Implement WebSocket onMessage() | 30 min | Route message, call service |
| 2 | Implement Broadcaster.onAuctionUpdate() | 20 min | Send JSON to all clients |
| 3 | Test placeBid end-to-end | 45 min | Verify DB + cache + WebSocket |
| 4 | Fix LinkedBlockingQueue unbounded issue | 10 min | Set capacity 1000 |

**Total:** ~2h

---

### **P1 - HIGH (Nên làm trong sprint này)**

| # | Công việc | Ước lượng | Ghi chú |
|---|----------|----------|---------|
| 5 | Add Create Auction API | 1h | POST /api/auctions |
| 6 | Add Get Bid History API | 30 min | GET /api/auctions/{id}/bids |
| 7 | Integration test (3 users bidding) | 1h | Concurrent bid test |
| 8 | Add Swagger documentation | 45 min | API docs |

**Total:** ~3.25h

---

### **P2 - MEDIUM (Nên làm sau)**

| # | Công việc | Ước lượng | Ghi chú |
|---|----------|----------|---------|
| 9 | Load test (1000 concurrent bids) | 2h | JMeter script |
| 10 | Add Unit test cho validation | 1.5h | JUnit 5 |
| 11 | Add cache invalidation strategy | 1h | TTL or event-based |
| 12 | Add metrics (Micrometer) | 1h | Monitor bid latency |

**Total:** ~5.5h

---

### **P3 - LOW (Nice to have)**

| # | Công việc | Ước lượng | Ghi chú |
|---|----------|----------|---------|
| 13 | Add retry logic cho bid queue | 1h | Exponential backoff |
| 14 | Add circuit breaker (Resilience4j) | 1.5h | Chống cascade failure |
| 15 | Add distributed tracing (Jaeger) | 2h | Trace request flow |

**Total:** ~4.5h

---

## 🚀 **KHUYẾN NGHỊ NEXT STEPS**

### **Tuần 1 (Hoàn thiện & Test)**
```
Day 1-2: Implement WebSocket + Broadcaster (P0)
Day 3:   Test end-to-end single bid
Day 4:   Create + Bid History API (P1)
Day 5:   Integration test 3 users (P1)
```

### **Tuần 2 (Optimization & Scale)**
```
Day 1-2: Load test 100 concurrent bids
Day 3:   Fix bottleneck issues
Day 4:   Add caching layer (Redis - optional)
Day 5:   Prepare release
```

---

## 📊 **DESIGN PATTERNS USED**

| Pattern | Nơi dùng | Lợi ích |
|---------|----------|---------|
| **Factory** | ItemFactory, Processors | Giảm coupling |
| **Strategy** | Validation, Anti-sniping | Dễ extend |
| **Chain of Responsibility** | BidValidationChain | Modular validation |
| **Decorator** | Stream operations | Functional style |
| **Facade** | AuctionService | Centralize logic |
| **Singleton** | DBConnection, Repositories | Reuse resource |
| **Command** | ApiRouter | Decouple HTTP handler |

---

## 🔐 **SECURITY CONSIDERATIONS**

| Vấn đề | Trạng thái | Giải pháp |
|-------|-----------|----------|
| SQL Injection | ✅ | PreparedStatement |
| Race condition | ✅ | ReentrantLock |
| WebSocket auth | ⚠️ | Cần implement |
| Rate limiting | ❌ | Cần add |
| Input validation | ⚠️ | Cần strengthen |

---

## 📝 **CHẤT LƯỢNG CODE**

**Điểm mạnh:**
- ✅ Clean architecture (Controller-Service-DAO)
- ✅ SOLID principles (Interface-based, dependency injection)
- ✅ Thread-safe concurrent collections
- ✅ Proper exception handling
- ✅ Logging & tracing

**Điểm yếu:**
- ⚠️ Chưa có test cases
- ⚠️ Chưa có documentation
- ⚠️ WebSocket implementation chưa hoàn thành
- ⚠️ Performance metrics chưa add

---

## ✨ **KẾT LUẬN**

**Tổng thể:** Hệ thống đấu giá đã hoàn thành **~85%** logic và database
- ✅ Database schema chuẩn xác
- ✅ Repository layer đầy đủ
- ✅ Service layer có threading
- ✅ Validation & Anti-sniping ok
- ⚠️ Cần finish WebSocket + test

**Để app chạy được:** Cần implement WebSocket message handling + Broadcaster (~1h)

**Thread handling:** Đã use ExecutorService + ReentrantLock + LinkedBlockingQueue đúng cách, không cần Callable/Runnable thêm

