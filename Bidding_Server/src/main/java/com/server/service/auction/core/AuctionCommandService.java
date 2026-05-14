package com.server.service.auction.core;

import com.server.DAO.AuctionRepository;
import com.server.DAO.AutoBidRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.BidTransaction;
import com.server.model.Item;
import com.server.service.ItemService;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.websocket.AuctionEventListener;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidRequestDTO;
import com.shared.dto.CreateAuctionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionCommandService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionCommandService.class);

    private final AuctionSharedState sharedState;
    private final AuctionRepository auctionRepository;
    private final AutoBidRepository autoBidRepository;
    private final BidTransactionRepository bidRepository;
    private final ItemService itemService;

    private final BidValidationChain validationChain;
    private final AntiSnipingStrategy antiSnipingStrategy;
    private final BidProcessor bidProcessor;
    private final AutoBidProcessor autoBidProcessor;
    private final AuctionScheduler scheduler;
	private final AuctionQueryService queryService;

    private final ExecutorService bidDbSavers = Executors.newFixedThreadPool(5);
    private AuctionEventListener eventListener;

    public AuctionCommandService(AuctionSharedState sharedState, AuctionRepository auctionRepository, 
                                 AutoBidRepository autoBidRepository, BidTransactionRepository bidRepository, 
                                 ItemService itemService, BidValidationChain validationChain, 
                                 AntiSnipingStrategy antiSnipingStrategy, BidProcessor bidProcessor, 
                                 AutoBidProcessor autoBidProcessor, AuctionScheduler scheduler,
								 AuctionQueryService queryService) {
        this.sharedState = sharedState;
        this.auctionRepository = auctionRepository;
        this.autoBidRepository = autoBidRepository;
        this.bidRepository = bidRepository;
        this.itemService = itemService;
        this.validationChain = validationChain;
        this.antiSnipingStrategy = antiSnipingStrategy;
        this.bidProcessor = bidProcessor;
        this.autoBidProcessor = autoBidProcessor;
		this.queryService = queryService;
        this.scheduler = scheduler;

        startBidQueueProcessor();
    }

    public void setEventListener(AuctionEventListener listener) {
        this.eventListener = listener;
    }

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

        ReentrantLock lock = sharedState.auctionLocks.get(request.getAuctionId());
        if (lock == null) {
            logger.error("Phiên đấu giá không tồn tại: {}", request.getAuctionId());
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
        }

        lock.lock();
        try {
            Auction auction = sharedState.auctionCache.get(request.getAuctionId());

            validationChain.validate(request, auction);
            bidProcessor.process(request, auction, sharedState.bidQueue);
            handleAntiSniping(auction);

            if (request.isEnableAutoBid()) {
                registerAutoBid(request);
            }

            processAutoBidsFromOtherUsers(auction);

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
            BigDecimal startingBid = (item.getStartingPrice() != null) ? item.getStartingPrice() : BigDecimal.ZERO;
            auction.setCurrentHighestBid(startingBid);

            if (startTime.isAfter(LocalDateTime.now())) {
                auction.setStatus(Auction.AuctionStatus.SCHEDULED);
            } else {
                auction.setStatus(Auction.AuctionStatus.OPEN);
            }

            long auctionId = auctionRepository.create(auction);
            if (auctionId <= 0) {
                throw new AuctionException(AuctionException.ErrorCode.OPERATION_FAILED, "Không thể lưu phiên đấu giá vào Database.");
            }
            auction.setId(auctionId);

            if (auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                sharedState.cacheAuction(auction);
                scheduler.scheduleAuctionStart(auction);
            } else { 
                scheduler.cacheAndScheduleAuction(auction);
            }

            // Gọi qua Event Listener thay vì query
            if (eventListener != null) {
                if (eventListener != null) {
					eventListener.onAuctionCreated(queryService.toDetailDto(auction));
				}
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
            autoBid.setCustomStepPrice(customStepPrice);
            autoBid.setActive(true);

            autoBidRepository.saveOrUpdate(autoBid);
            logger.debug("Auto-bid amount set/updated for auction {}: {}", auctionId, bidderId);

            ReentrantLock lock = sharedState.auctionLocks.get(auctionId);
            if (lock != null) {
                lock.lock();
                try {
                    Auction auction = sharedState.auctionCache.get(auctionId);
                    if (auction != null &&
                            (auction.getStatus() == Auction.AuctionStatus.RUNNING || auction.getStatus() == Auction.AuctionStatus.OPEN)) {

                        processAutoBidsFromOtherUsers(auction);
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
            logger.error("Error in setting auto-bid: ", e);
            throw new AuctionException(AuctionException.ErrorCode.OPERATION_FAILED, "Lỗi khi cấu hình auto-bid: " + e.getMessage());
        }
    }
	
	/**
     * Đăng ký auto-bid cho người dùng
     */
    private void registerAutoBid(BidRequestDTO request) {
        if (request.getBidAmount() != null) {
            AutoBidTracker autoBid = new AutoBidTracker(
                    request.getAuctionId(), request.getBidderId(), request.getMaxAutoBidAmount()
            );
            autoBid.setCustomStepPrice(request.getCustomStepPrice());
            autoBidRepository.saveOrUpdate(autoBid);
            autoBid.setActive(true);
            logger.debug("Auto-bid enabled for auction {}: {}", request.getAuctionId(), request.getBidderId());
        }
    }
	
	/**
     * Handle anti-sniping: Extend auction time nếu bid ở giây cuối
     */
    private void handleAntiSniping(Auction auction) {
        if (antiSnipingStrategy.shouldExtendTime(auction)) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(antiSnipingStrategy.getExtensionMillis() / 1000);
            auction.setEndTime(newEndTime);
            scheduler.scheduleAuctionEnd(auction);
            logger.debug("Auction time extended for auction {}: {} seconds", auction.getId(), antiSnipingStrategy.getExtensionMillis() / 1000);
        }
    }
	
	/**
     * Xử lý tự động đặt giá cho các người dùng khác có auto-bid
     * Được gọi sau khi một đơn đặt giá thủ công thành công
     */
    private void processAutoBidsFromOtherUsers(Auction auction) {
        autoBidProcessor.process(auction, sharedState.bidQueue);
    }

    private AuctionUpdateDTO createUpdateDTO(Auction auction) {
        String bidderName = auction.getWinnerId() != null
                ? sharedState.resolveUserDisplayName(auction.getWinnerId(), "User")
                : "No bids";
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        return new AuctionUpdateDTO(auction.getId(), auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO,
                bidderName, Math.max(0, remaining));
    }

	 /**
     * Bắt đầu background thread để xử lý bid queue và lưu vào DB
     */
    private void startBidQueueProcessor() {
		// rent 5 threads để xử lsy bid lưu vào, tránh quá nhiều việc, vì đang xử lí trên toàn hệ thôgns
        for (int i = 0; i < 5; i++) {
            bidDbSavers.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        BidTransaction bid = sharedState.bidQueue.take();
                        try {
                            bidRepository.save(bid);
                            logger.debug("Bid transaction saved to DB: Auction {}, Bidder {}, Amount {}",
                                    bid.getAuctionId(), bid.getBidderId(), bid.getBidAmount());
                        } catch (Exception e) {
                            logger.error("Lỗi khi lưu bid vào DB: ", e);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.info("Bid queue processor thread interrupted, shutting down.");
                        break;
                    }
                }
            });
        }
    }

    public void shutdown() {
        bidDbSavers.shutdownNow();
    }
}