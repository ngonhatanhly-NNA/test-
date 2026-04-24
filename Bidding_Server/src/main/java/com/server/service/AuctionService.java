package com.server.service;

import com.server.model.*;
import com.server.DAO.*;
import com.server.websocket.*;
import com.server.exception.AuctionException;
import com.server.service.auction.strategy.*;
import com.server.service.auction.processor.*;
import com.server.service.auction.antisnipe.*;
import com.server.service.auction.logging.AuctionLogger;
import com.shared.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // ========== Concurrent Collections ==========
    private final ConcurrentHashMap<Long, Auction> auctionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

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
            BidValidationChain validationChain,
            AntiSnipingStrategy antiSnipingStrategy,
            BidProcessor bidProcessor,
            AutoBidProcessor autoBidProcessor) {

        this.auctionRepository = auctionRepo;
        this.bidRepository = bidRepo;
        this.autoBidRepository = autoBidRepo;

        this.validationChain = validationChain;
        this.antiSnipingStrategy = antiSnipingStrategy;
        this.bidProcessor = bidProcessor;
        this.autoBidProcessor = autoBidProcessor;

        init();
    }

    // ========== Init Methods ==========

    /**
     * Khởi tạo service: Tải các phiên đấu giá đang hoạt động từ DB vào cache
     */
    private void init() {
        try {
            List<Auction> activeAuctions = auctionRepository.findByStatusIn(
                    List.of(Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING));

            activeAuctions.forEach(this::cacheAndScheduleAuction);
            startBidQueueProcessor();

            AuctionLogger.logInfo("Đã khởi tạo " + activeAuctions.size() + " phiên đấu giá");
        } catch (Exception e) {
            logger.error("Lỗi khi khởi tạo AuctionService", e);
            AuctionLogger.logError("INIT", 0, e.getMessage());
        }
    }

    /**
     * Thêm phiên đấu giá vào cache và lên lịch kết thúc
     */
    private void cacheAndScheduleAuction(Auction auction) {
        auctionCache.put(auction.getId(), auction);
        auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
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

            AuctionLogger.logBidPlaced(auction.getId(), request.getBidderId(),
                request.getBidAmount().toString(), false);

            return update;

        } catch (AuctionException e) {
            AuctionLogger.logError("PLACE_BID", request.getAuctionId(), e.getMessage());
            logger.error("Lỗi khi đặt giá cho phiên đấu giá {}: {}", request.getAuctionId(), e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

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
        String bidderName = auction.getWinnerId() != null ? "User_" + auction.getWinnerId() : "No bids";
        String itemName = auctionRepository.findItemNameByItemId(auction.getItemId());
        if (itemName == null || itemName.isBlank()) {
            itemName = "Item #" + auction.getItemId();
        }
        return new AuctionDetailDTO(
                auction.getId(),
                auction.getItemId(),
                itemName,
                auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO,
                bidderName,
                Math.max(0, remaining),
                auction.getStepPrice()
        );
    }

    private AuctionUpdateDTO createUpdateDTO(Auction auction) {
        String bidderName = auction.getWinnerId() != null ? "User_" + auction.getWinnerId() : "No bids";
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        return new AuctionUpdateDTO(auction.getId(), auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO,
            bidderName, Math.max(0, remaining));
    }

    /**
     * Handle anti-sniping: Extend auction time nếu bid ở giây cuối
     */
    private void handleAntiSniping(Auction auction) {
        if (antiSnipingStrategy.shouldExtendTime(auction)) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(antiSnipingStrategy.getExtensionMillis() / 1000);
            auction.setEndTime(newEndTime);
            scheduleAuctionEnd(auction);
            AuctionLogger.logTimeExtended(auction.getId(), antiSnipingStrategy.getExtensionMillis() / 1000);
        }
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
            autoBidRepository.saveOrUpdate(autoBid);
            AuctionLogger.logAutoBidEnabled(request.getAuctionId(), request.getBidderId(),
                request.getMaxAutoBidAmount().toString());
        }
    }

    /**
     * Xử lý tự động đặt giá cho các người dùng khác có auto-bid
     * Được gọi sau khi một đơn đặt giá thủ công thành công
     */
    private void processAutoBidsFromOtherUsers(Auction auction) {
        autoBidProcessor.process( auction, this.bidQueue);
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
     * Kết thúc phiên đấu giá
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
                    auctionRepository.save(auction);

                    // Dọn dẹp resources
                    auctionCache.remove(auctionId);
                    auctionLocks.remove(auctionId);
                    scheduledTasks.remove(auctionId);

                    AuctionLogger.logAuctionFinished(auctionId,
                        auction.getWinnerId() != null ? auction.getWinnerId() : 0,
                        auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid().toString() : "0");
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Bắt đầu background thread để xử lý bid queue
     */
    private void startBidQueueProcessor() {
        Thread processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    BidTransaction bid = bidQueue.take();
                    try {
                        bidRepository.save(bid);
                    } catch (Exception e) {
                        AuctionLogger.logError("SAVE_BID", bid.getAuctionId(), e.getMessage());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    AuctionLogger.logInfo("Bid processor thread đã dừng");
                    break;
                }
            }
        }, "bid-processor");
        processor.setDaemon(true);
        processor.start();
    }

    // ========== Public API Methods ==========

    public void setEventListener(AuctionEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Hủy auto-bid cho một người dùng
     */
    public void cancelAutoBid(long auctionId, long bidderId) throws AuctionException {
        try {
            autoBidRepository.deactivate(auctionId, bidderId);
            AuctionLogger.logAutoBidDisabled(auctionId, bidderId, "Người dùng hủy");
        } catch (Exception e) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Cập nhật giá tối đa auto-bid
     */
    public void updateAutoBidAmount(long auctionId, long bidderId, BigDecimal newMaxAmount) throws AuctionException {
        if (newMaxAmount == null || newMaxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuctionException(AuctionException.ErrorCode.INVALID_AUTO_BID_CONFIG, "Giá tối đa phải lớn hơn 0");
        }

        try {
            AutoBidTracker autoBid = autoBidRepository.findByAuctionAndBidder(auctionId, bidderId);
            if (autoBid != null) {
                autoBid.setMaxBidAmount(newMaxAmount);
                autoBidRepository.saveOrUpdate(autoBid);
                AuctionLogger.logAutoBidEnabled(auctionId, bidderId, newMaxAmount.toString());
            } else {
                throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND, "Auto-bid không tồn tại");
            }
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Lấy danh sách các phiên đấu giá đang hoạt động
     */
    public List<AuctionDetailDTO> getActiveAuctions() {
        return auctionCache.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN)
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Tạo phiên đấu giá mới
     */
    public long createAuction(CreateAuctionDTO dto) throws AuctionException {
        try {
            if (dto.getItemId() <= 0 || dto.getSellerId() <= 0) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "ItemId và SellerId phải lớn hơn 0");
            }

            if (dto.getStepPrice() == null || dto.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "Step price phải lớn hơn 0");
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
            auction.setStatus(Auction.AuctionStatus.OPEN);

            long auctionId = auctionRepository.create(auction);
            if (auctionId > 0) {
                auction.setId(auctionId);
                cacheAndScheduleAuction(auction);
                AuctionLogger.logInfo("Phiên đấu giá " + auctionId + " đã tạo thành công");
            }
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
                        "User_" + bid.getBidderId(),
                        bid.getBidAmount(),
                        bid.getTimestamp(),
                        bid.isAutoBid()
                ))
                .toList();
    }

    private AuctionDetailDTO convertToDTO(Auction auction) {
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        String bidderName = auction.getWinnerId() != null ? "User_" + auction.getWinnerId() : "No bids";
        return new AuctionDetailDTO(
            auction.getId(),
            auction.getItemId(),
            "Item #" + auction.getItemId(),
            auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO,
            bidderName,
            Math.max(0, remaining),
            auction.getStepPrice()
        );
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
