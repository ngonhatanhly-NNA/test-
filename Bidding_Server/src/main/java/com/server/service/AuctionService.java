package com.server.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.DAO.AuctionRepository;
import com.server.DAO.AutoBidRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.DAO.UserRepository;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.BidTransaction;
import com.server.model.Item;
import com.server.model.User;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.websocket.AuctionEventListener;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidHistoryDTO;
import com.shared.dto.BidRequestDTO;
import com.shared.dto.CreateAuctionDTO;

/**
 * AuctionService: Quản lý toàn bộ logic đấu giá
 *
 * Design Patterns:
 * - Factory Pattern: Tạo bid processors
 * - Strategy Pattern: Validation, Anti-sniping, Processing
 * - Chain of Responsibility: Validation chain
 * - Decorator Pattern: Extend functionality
 * - Facade Pattern: Centralized logging
 * - Singleton Pattern: Repositories
 */

/** Có kiểm tra tính hợp lệ bằng các validation, đấu giá, cập nhật người dẫn đầu */
public class AuctionService {

    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    // ========== Dependencies ==========
    private final AuctionRepository auctionRepository;
    private final BidTransactionRepository bidRepository;
    private AutoBidRepository autoBidRepository;
    private final ItemService itemService;
    private final UserRepository userRepository;

    // ========== Concurrent Collections ==========
    private final ConcurrentHashMap<Long, Auction> auctionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userDisplayNameCache = new ConcurrentHashMap<>();

    // ========== Queues & Executors ==========
    private final LinkedBlockingQueue<BidTransaction> bidQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // ========== Strategies ==========
    private final BidValidationChain validationChain;
    private final AntiSnipingStrategy antiSnipingStrategy;
    private final BidProcessor bidProcessor;
    private final AutoBidProcessor autoBidProcessor;
    // ========== Event Listener ==========
    private AuctionEventListener eventListener;

    // ========== Constructor ==========
    public AuctionService(
            AuctionRepository auctionRepo,
            BidTransactionRepository bidRepo,
            AutoBidRepository autoBidRepo,
            ItemService itemService,
            UserRepository userRepository,
            BidValidationChain validationChain,
            AntiSnipingStrategy antiSnipingStrategy,
            BidProcessor bidProcessor,
            AutoBidProcessor autoBidProcessor) {

        this.auctionRepository = auctionRepo;
        this.bidRepository = bidRepo;
        this.autoBidRepository = autoBidRepo;
        this.itemService = itemService;
        this.userRepository = userRepository;

        this.validationChain = validationChain;
        this.antiSnipingStrategy = antiSnipingStrategy;
        this.bidProcessor = bidProcessor;
        this.autoBidProcessor = autoBidProcessor;

        init();
    }

    // ========== Init Methods ==========

    /**
     * Khởi tạo service: Tải các phiên đấu giá đang hoạt động từ DB vào cache.
     * Đảm bảo không bị mất dữ liệu khi server restart.
     */
    private void init() {
        try {
            List<Auction> auctions = auctionRepository.findByStatusIn(
                    List.of(Auction.AuctionStatus.SCHEDULED, Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING));

            for (Auction auction : auctions) {
                cacheAuction(auction); // Cache tất cả
                if (auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                    scheduleAuctionStart(auction);
                } else if (auction.getStatus() == Auction.AuctionStatus.OPEN || auction.getStatus() == Auction.AuctionStatus.RUNNING) {
                    scheduleAuctionEnd(auction);
                }
            }
            startBidQueueProcessor();

            logger.info("Đã khởi tạo {} phiên đấu giá từ DB vào cache", auctions.size());
        } catch (Exception e) {
            logger.error("Lỗi khi khởi tạo AuctionService", e);
        }
    }

    /**
     * Thêm phiên đấu giá vào cache
     */
    private void cacheAuction(Auction auction) {
        auctionCache.put(auction.getId(), auction);
        // Chỉ add lock nếu có thể bid
        if (auction.getStatus() == Auction.AuctionStatus.OPEN || auction.getStatus() == Auction.AuctionStatus.RUNNING) {
            auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
        }
    }

    /**
     * Thêm phiên đấu giá vào cache và lên lịch kết thúc (chỉ cho OPEN/RUNNING)
     */
    private void cacheAndScheduleAuction(Auction auction) {
        cacheAuction(auction);
        scheduleAuctionEnd(auction);
    }

    // ========== Main Business Logic ==========

    /**
     * Xử lý yêu cầu đặt giá từ client
     *
     * @throws AuctionException nếu validation thất bại hoặc có lỗi
     */
    public AuctionUpdateDTO placeBid(BidRequestDTO request) throws AuctionException {
        if (request == null) {
            logger.error("Request không được null");
            throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "Request không được null");
        }

        ReentrantLock lock = auctionLocks.get(request.getAuctionId());
        if (lock == null) {
            logger.error("Phiên đấu giá không tồn tại: {}", request.getAuctionId());
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
        }

        lock.lock();
        try {
            Auction auction = auctionCache.get(request.getAuctionId());

            // Check validation
            validationChain.validate(request, auction);

            // Process bid
            bidProcessor.process(request, auction, bidQueue);

            //Handle anti-sniping
            handleAntiSniping(auction);

            //Handle auto-bid registration
            if (request.isEnableAutoBid()) {
                registerAutoBid(request);
            }

            // Process auto-bids from other users
            processAutoBidsFromOtherUsers(auction);

            //Broadcast update
            AuctionUpdateDTO update = createUpdateDTO(auction);
            if (eventListener != null) {
                eventListener.onAuctionUpdate(update);
            }

            logger.debug("Bid placed for auction {}: {}", auction.getId(), request.getBidderId());

            return update;

        } catch (AuctionException e) {
            logger.error("Lỗi khi đặt giá cho phiên đấu giá {}: {}", request.getAuctionId(), e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Bắt đầu background thread để xử lý bid queue và lưu vào DB
     */
    private void startBidQueueProcessor() {
        Thread processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    BidTransaction bid = bidQueue.take();
                    try {
                        bidRepository.save(bid);
                    } catch (Exception e) {
                        logger.error("Lỗi khi lưu bid cho phiên đấu giá {}: {}", bid.getAuctionId(), e.getMessage());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Bid processor thread đã dừng");
                    break;
                }
            }
        }, "bid-processor");
        processor.setDaemon(true);
        processor.start();
    }


    // ==== Auction Network - DTO Convertor (need redefine) ====
    /**
     * Chi tiết phiên đấu giá: ưu tiên bản trong cache (giá / gia hạn giờ mới nhất),
     * nếu không có thì đọc từ DB (phiên đã kết thúc hoặc chưa load cache).
     */
    public AuctionDetailDTO getAuctionDetail(long auctionId) throws AuctionException {
        Auction auction = auctionCache.get(auctionId);
        if (auction == null) {
            auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND));
        }
        return toDetailDto(auction);
    }

    private AuctionDetailDTO toDetailDto(Auction auction) {
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();

        // 1. Lấy Display Name
        String bidderName = auction.getWinnerId() != null
                ? resolveUserDisplayName(auction.getWinnerId(), "User")
                : "No bids";
        String sellerName = resolveUserDisplayName(auction.getSellerId(), "Seller");

        // 2. Lấy thông tin Item
        Item item = itemService.getItemById(auction.getItemId());
        String itemName = (item != null && item.getName() != null) ? item.getName() : "Item #" + auction.getItemId();
        String itemDescription = (item != null && item.getDescription() != null) ? item.getDescription() : "(Không có mô tả)";
        String itemType = (item != null) ? itemService.extractItemType(item) : "GENERAL";
        Map<String, String> itemSpecifics = (item != null) ? itemService.extractItemSpecifics(item) : new HashMap<>();
        List<String> itemImageUrls = (item != null) ? item.getImageUrls() : new ArrayList<>();

        // 3. GỌI CONSTRUCTOR - KIỂM TRA KỸ THỨ TỰ (15 THAM SỐ)
        return new AuctionDetailDTO(
                auction.getId(),                                    // 1. auctionId (long)
                auction.getItemId(),                                 // 2. itemId (long)
                itemName,                                            // 3. itemName (String)
                itemDescription,                                     // 4. itemDescription (String)
                auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO, // 5. currentPrice (BigDecimal)
                bidderName,                                          // 6. highestBidderName (String)
                Math.max(0, remaining),                              // 7. remainingTime (long)
                auction.getStepPrice(),                              // 8. stepPrice (BigDecimal)
                itemType,                                            // 9. itemType (String)
                itemSpecifics,                                       // 10. itemSpecifics (Map)
                itemImageUrls,                                       // 11. itemImageUrls (List)
                auction.getSellerId(),                               // 12. sellerId (long)
                sellerName,                                          // 13. sellerName (String)
                auction.getStartTime().toString(),                   // 14. startTime (String)
                auction.getEndTime().toString()                      // 15. endTime (String)
        );
    }

    private AuctionUpdateDTO createUpdateDTO(Auction auction) {
        String bidderName = auction.getWinnerId() != null
                ? resolveUserDisplayName(auction.getWinnerId(), "User")
                : "No bids";
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        return new AuctionUpdateDTO(auction.getId(), auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO,
                bidderName, Math.max(0, remaining));
    }

    private String resolveUserDisplayName(long userId, String fallbackPrefix) {
        if (userId <= 0) {
            return fallbackPrefix + "_UNKNOWN";
        }

        String cached = userDisplayNameCache.get(userId);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        try {
            User user = userRepository.getUserById(userId);
            if (user != null) {
                String fullName = user.getFullName();
                String username = user.getUsername();
                String displayName = (fullName != null && !fullName.isBlank())
                        ? fullName
                        : ((username != null && !username.isBlank()) ? username : fallbackPrefix + "_" + userId);
                userDisplayNameCache.put(userId, displayName);
                return displayName;
            }
        } catch (Exception e) {
            logger.debug("Không lấy được user {} để hiển thị tên: {}", userId, e.getMessage());
        }

        return fallbackPrefix + "_" + userId;
    }

    /**
     * Handle anti-sniping: Extend auction time nếu bid ở giây cuối
     */
    private void handleAntiSniping(Auction auction) {
        if (antiSnipingStrategy.shouldExtendTime(auction)) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(antiSnipingStrategy.getExtensionMillis() / 1000);
            auction.setEndTime(newEndTime);
            scheduleAuctionEnd(auction);
            logger.debug("Auction time extended for auction {}: {} seconds", auction.getId(), antiSnipingStrategy.getExtensionMillis() / 1000);
        }
    }

    /**
     * Xử lý tự động đặt giá cho các người dùng khác có auto-bid
     * Được gọi sau khi một đơn đặt giá thủ công thành công
     */
    private void processAutoBidsFromOtherUsers(Auction auction) {
        autoBidProcessor.process(auction, this.bidQueue);
    }

    // ========== Scheduling & Cleanup ==========
    /**
     * Lên lịch kết thúc phiên đấu giá
     */
    private void scheduleAuctionEnd(Auction auction) {
        long delay = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delay > 0) {
            // Hủy task cũ nếu tồn tại
            ScheduledFuture<?> existingTask = scheduledTasks.get(auction.getId());
            if (existingTask != null) {
                existingTask.cancel(false);
            }

            // Lên lịch task mới
            ScheduledFuture<?> newTask = scheduler.schedule(
                    () -> finishAuction(auction.getId()),
                    delay,
                    TimeUnit.MILLISECONDS
            );
            scheduledTasks.put(auction.getId(), newTask);
        } else {
            finishAuction(auction.getId());
        }
    }

    /**
     * Lên lịch bắt đầu phiên đấu giá
     */
    private void scheduleAuctionStart(Auction auction) {
        long delay = Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis();
        if (delay > 0) {
            ScheduledFuture<?> newTask = scheduler.schedule(
                    () -> startAuction(auction.getId()),
                    delay,
                    TimeUnit.MILLISECONDS
            );
            scheduledTasks.put(auction.getId(), newTask);
        } else {
            startAuction(auction.getId());
        }
    }

    /**
     * Bắt đầu phiên đấu giá: chuyển status từ SCHEDULED sang OPEN và cache
     */
    private void startAuction(long auctionId) {
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction != null && auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                auction.setStatus(Auction.AuctionStatus.OPEN);
                auctionRepository.save(auction);
                cacheAndScheduleAuction(auction);
                logger.info("Phiên đấu giá {} đã bắt đầu", auctionId);

                // Broadcast auction started - client will update the list
                if (eventListener != null) {
                    eventListener.onAuctionCreated(toDetailDto(auction));
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi bắt đầu phiên đấu giá {}: {}", auctionId, e.getMessage());
        }
    }

    /**
     * Kết thúc phiên đấu giá và LƯU VÀO DATABASE.
     * Đảm bảo winner_id và status=FINISHED được persist để không mất khi restart.
     */
    private void finishAuction(long auctionId) {
        ReentrantLock lock = auctionLocks.get(auctionId);
        if (lock != null) {
            lock.lock();
            try {
                Auction auction = auctionCache.get(auctionId);
                if (auction != null &&
                        (auction.getStatus() == Auction.AuctionStatus.RUNNING ||
                                auction.getStatus() == Auction.AuctionStatus.OPEN)) {

                    auction.setStatus(Auction.AuctionStatus.FINISHED);
                    // Lưu vào DB: winner_id và status=FINISHED được ghi ngay lập tức
                    auctionRepository.save(auction);
                    logger.info("Phiên đấu giá {} đã kết thúc. Winner: {}, Giá: {}",
                            auctionId,
                            auction.getWinnerId() != null ? auction.getWinnerId() : "Không có",
                            auction.getCurrentHighestBid());

                    // Dọn dẹp resources khỏi cache (đã lưu DB rồi)
                    auctionCache.remove(auctionId);
                    auctionLocks.remove(auctionId);
                    scheduledTasks.remove(auctionId);

                    if (eventListener != null) {
                        eventListener.onAuctionFinished(auctionId);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    // ========== Public API Methods ==========

    public void setEventListener(AuctionEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Đăng ký auto-bid cho người dùng
     */
    private void registerAutoBid(BidRequestDTO request) {
        if (request.getBidAmount() != null) {
            AutoBidTracker autoBid = new AutoBidTracker(
                    request.getAuctionId(),
                    request.getBidderId(),
                    request.getMaxAutoBidAmount()
            );
            autoBid.setCustomStepPrice(request.getCustomStepPrice()); // LƯU CUSTOM STEP PRICE
            autoBidRepository.saveOrUpdate(autoBid);
            logger.debug("Auto-bid enabled for auction {}: {}", request.getAuctionId(), request.getBidderId());
        }
    }

    /**
     * Hủy auto-bid cho một người dùng
     */
    public void cancelAutoBid(long auctionId, long bidderId) throws AuctionException {
        try {
            autoBidRepository.deactivate(auctionId, bidderId);
            logger.debug("Auto-bid disabled for auction {}: {}", auctionId, bidderId);
        } catch (Exception e) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Cập nhật giá tối đa auto-bid hoặc đăng ký mới auto-bid
     */
    public void updateAutoBidAmount(long auctionId, long bidderId, BigDecimal newMaxAmount, BigDecimal customStepPrice) throws AuctionException {
        if (newMaxAmount == null || newMaxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuctionException(AuctionException.ErrorCode.INVALID_AUTO_BID_CONFIG, "Giá tối đa phải lớn hơn 0");
        }

       try {
            AutoBidTracker autoBid = new AutoBidTracker(auctionId, bidderId, newMaxAmount);
            autoBid.setCustomStepPrice(customStepPrice); // LƯU CUSTOM STEP PRICE
            autoBid.setActive(true);
            
            autoBidRepository.saveOrUpdate(autoBid);
            logger.debug("Auto-bid amount set/updated for auction {}: {}", auctionId, bidderId);

            // 2. Kích hoạt AutoBidProcessor ngay lập tức để xử lý nếu giá thỏa mãn điều kiện
            ReentrantLock lock = auctionLocks.get(auctionId);
            if (lock != null) {
                lock.lock();
                try {
                    Auction auction = auctionCache.get(auctionId);
                    if (auction != null && 
                       (auction.getStatus() == Auction.AuctionStatus.RUNNING || auction.getStatus() == Auction.AuctionStatus.OPEN)) {
                        
                        processAutoBidsFromOtherUsers(auction);
                        
                        // Thông báo real-time qua websocket nếu có thay đổi giá
                        AuctionUpdateDTO update = createUpdateDTO(auction);
                        if (eventListener != null) {
                            eventListener.onAuctionUpdate(update);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND, "Phiên đấu giá không hoạt động");
            }

        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Lỗi khi set auto-bid: ", e);
            throw new AuctionException(AuctionException.ErrorCode.OPERATION_FAILED, "Lỗi khi cấu hình auto-bid: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách TẤT CẢ các phiên đấu giá đang hoạt động (dùng cho trang Live Auctions chung).
     * Đọc từ cache (dữ liệu real-time nhất).
     */
    public List<AuctionDetailDTO> getActiveAuctions() {
        return auctionCache.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.SCHEDULED)
                .map(this::toDetailDto)
                .toList();
    }

    /**
     * [MỚI] Lấy các phiên đấu giá đang hoạt động của MỘT SELLER CỤ THỂ.
     * Dùng cho tab "Live Auction Monitoring" trong Seller Portal.
     * Kết hợp cache (real-time) + DB (để đảm bảo đồng bộ).
     */
    public List<AuctionDetailDTO> getActiveAuctionsBySeller(long sellerId) {
        // Lấy từ cache trước (có giá real-time)
        List<AuctionDetailDTO> fromCache = auctionCache.values().stream()
                .filter(a -> a.getSellerId() == sellerId &&
                        (a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.SCHEDULED))
                .map(this::toDetailDto)
                .toList();

        // Nếu cache rỗng, thử load từ DB (phòng trường hợp server vừa restart)
        if (fromCache.isEmpty()) {
            List<Auction> dbAuctions = auctionRepository.findByStatusIn(
                    List.of(Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING, Auction.AuctionStatus.SCHEDULED));
            dbAuctions.stream()
                    .filter(a -> a.getSellerId() == sellerId)
                    .forEach((var a) -> {
                        if (!auctionCache.containsKey(a.getId())) {
                            cacheAuction(a);
                            if (a.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                                scheduleAuctionStart(a);
                            } else if (a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.RUNNING) {
                                scheduleAuctionEnd(a);
                            }
                        }
                    });
            return auctionCache.values().stream()
                    .filter(a -> a.getSellerId() == sellerId &&
                            (a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.SCHEDULED))
                    .map(this::toDetailDto)
                    .toList();
        }
        return fromCache;
    }

    /**
     * [MỚI] Lấy danh sách các phiên đấu giá đã KẾT THÚC mà người dùng là NGƯỜI THẮNG.
     * Dùng cho phần "Won Auctions" ở My Inventory.
     * Đọc từ DB (không cache vì đã FINISHED).
     */
    public List<AuctionDetailDTO> getWonAuctionsByBidder(long bidderId) {
        List<Auction> wonAuctions = auctionRepository.findWonAuctionsByBidderId(bidderId);
        return wonAuctions.stream()
                .map(this::toDetailDto)
                .toList();
    }

    /**
     * Tạo phiên đấu giá mới
     */
    public long createAuction(CreateAuctionDTO dto) throws AuctionException {
        try {
            logger.info("Bắt đầu xử lý tạo Auction: ItemId={}, SellerId={}, Start={}, End={}, Step={}",
                    dto.getItemId(), dto.getSellerId(), dto.getStartTime(), dto.getEndTime(), dto.getStepPrice());

            if (dto.getItemId() <= 0 || dto.getSellerId() <= 0) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "ItemId và SellerId phải lớn hơn 0");
            }

            if (dto.getStepPrice() == null || dto.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "Step price phải lớn hơn 0");
            }

            Item item = itemService.getItemById(dto.getItemId());
            if (item == null) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_REQUEST, "Item không tồn tại hoặc không đúng subtype");
            }

            if (item.getSellerId() > 0 && item.getSellerId() != dto.getSellerId()) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_REQUEST, "Item không thuộc về seller này");
            }

            java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(dto.getStartTime());
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(dto.getEndTime());

            if (endTime.isBefore(startTime)) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "End time phải sau start time");
            }

            Auction auction = new Auction();
            auction.setItemId(dto.getItemId());
            auction.setSellerId(dto.getSellerId());
            auction.setStartTime(startTime);
            auction.setEndTime(endTime);
            auction.setStepPrice(dto.getStepPrice());
            auction.setCurrentHighestBid(BigDecimal.ZERO);

            // Set status dựa trên thời gian bắt đầu
            if (startTime.isAfter(LocalDateTime.now())) {
                auction.setStatus(Auction.AuctionStatus.SCHEDULED);
            } else {
                auction.setStatus(Auction.AuctionStatus.OPEN);
            }

            // LƯU VÀO DATABASE
            long auctionId = auctionRepository.create(auction);

            if (auctionId <= 0) {
                throw new AuctionException(AuctionException.ErrorCode.OPERATION_FAILED, "Không thể lưu phiên đấu giá vào Database.");
            }

            auction.setId(auctionId);

            // Xử lý dựa trên status
            if (auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                cacheAuction(auction);  // Thêm vào cache ngay lập tức để hiển thị
                scheduleAuctionStart(auction);
            } else {
                cacheAndScheduleAuction(auction);
            }

            if (eventListener != null) {
                eventListener.onAuctionCreated(toDetailDto(auction));
            }

            logger.info("Phiên đấu giá {} đã tạo thành công và lưu vào DB", auctionId);

            return auctionId;
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "Lỗi tạo phiên: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử đặt giá của một phiên đấu giá
     */
    public List<BidHistoryDTO> getBidHistory(long auctionId) {
        List<BidTransaction> bids = auctionRepository.findBidHistoryByAuction(auctionId);
        return bids.stream()
                .map(bid -> new BidHistoryDTO(
                        bid.getBidderId(),
                        resolveUserDisplayName(bid.getBidderId(), "User"),
                        bid.getBidAmount(),
                        bid.getTimestamp(),
                        bid.isAutoBid()
                ))
                .toList();
    }

    // Sập nguồn :)))
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("AuctionService đã shutdown");
    }
}
