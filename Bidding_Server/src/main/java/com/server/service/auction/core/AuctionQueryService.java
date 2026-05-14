package com.server.service.auction.core;

import com.server.DAO.AuctionRepository;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.server.model.Item;
import com.server.service.ItemService;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.BidHistoryDTO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuctionQueryService {

    private final AuctionSharedState sharedState;
    private final AuctionRepository auctionRepository;
    private final ItemService itemService;
	private AuctionScheduler scheduler;

    public AuctionQueryService(AuctionSharedState sharedState, AuctionRepository auctionRepository, ItemService itemService) {
        this.sharedState = sharedState;
        this.auctionRepository = auctionRepository;
        this.itemService = itemService;
    }
	
	public void setScheduler(AuctionScheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
     * Chi tiết phiên đấu giá: ưu tiên bản trong cache (giá / gia hạn giờ mới nhất),
     * nếu không có thì đọc từ DB (phiên đã kết thúc hoặc chưa load cache).
     */
    public AuctionDetailDTO getAuctionDetail(long auctionId) throws AuctionException {
        Auction auction = sharedState.auctionCache.get(auctionId);
        if (auction == null) {
            auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND));
        }
        return toDetailDto(auction);
    }

	/**
     * Lấy lịch sử đặt giá của một phiên đấu giá
     */
    public List<BidHistoryDTO> getBidHistory(long auctionId) {
        List<BidTransaction> bids = auctionRepository.findBidHistoryByAuction(auctionId);
        return bids.stream()
                .map(bid -> new BidHistoryDTO(
                        bid.getBidderId(),
                        sharedState.resolveUserDisplayName(bid.getBidderId(), "User"),
                        bid.getBidAmount(),
                        bid.getTimestamp(),
                        bid.isAutoBid()
                ))
                .collect(Collectors.toList());
    }
	
	/**
     * Lấy danh sách TẤT CẢ các phiên đấu giá ĐANG DIỄN RA (OPEN hoặc RUNNING).
     */
    public List<AuctionDetailDTO> getActiveAuctions() {
        return sharedState.auctionCache.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.RUNNING)
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

	/**
     * Lấy danh sách các phiên đấu giá SẮP DIỄN RA (Chỉ SCHEDULED).
     */
    public List<AuctionDetailDTO> getUpcomingAuctions() {
        return sharedState.auctionCache.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.SCHEDULED)
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }
	
	/**
     * Lấy các phiên đấu giá đang hoạt động của MỘT SELLER CỤ THỂ.
     */
    public List<AuctionDetailDTO> getActiveAuctionsBySeller(long sellerId) {
        List<AuctionDetailDTO> fromCache = sharedState.auctionCache.values().stream()
                .filter(a -> a.getSellerId() == sellerId &&
                        (a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.SCHEDULED))
                .map(this::toDetailDto)
                .collect(Collectors.toList());

        if (fromCache.isEmpty()) {
            List<Auction> dbAuctions = auctionRepository.findByStatusIn(
                    List.of(Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING, Auction.AuctionStatus.SCHEDULED));
            dbAuctions.stream()
                    .filter(auctionItem -> auctionItem.getSellerId() == sellerId)
                    .forEach(auctionItem -> {
                        if (!sharedState.auctionCache.containsKey(auctionItem.getId())) {
                            sharedState.cacheAuction(auctionItem);
                            
							if (scheduler != null) {
								if (auctionItem.getStatus() == Auction.AuctionStatus.SCHEDULED || auctionItem.getStatus() == Auction.AuctionStatus.OPEN) {
									scheduler.scheduleAuctionStart(auctionItem);
								} else if (auctionItem.getStatus() == Auction.AuctionStatus.RUNNING) {
									scheduler.scheduleAuctionEnd(auctionItem);
								}
							}
                        }
                    });
            return sharedState.auctionCache.values().stream()
                    .filter(a -> a.getSellerId() == sellerId &&
                            (a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN || a.getStatus() == Auction.AuctionStatus.SCHEDULED))
                    .map(this::toDetailDto)
                    .collect(Collectors.toList());
        }
        return fromCache;
    }

	/**
     * Lấy danh sách các phiên đấu giá đã KẾT THÚC mà người dùng là NGƯỜI THẮNG.
     * Dùng cho phần "Won Auctions" ở My Inventory.
     * Đọc từ DB (không cache vì đã FINISHED).
     */
    public List<AuctionDetailDTO> getWonAuctionsByBidder(long bidderId) {
        List<Auction> wonAuctions = auctionRepository.findWonAuctionsByBidderId(bidderId);
        return wonAuctions.stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

	/**
     * Kiểm tra trạng thái đấu giá của một item theo itemId.
     * Trả về status string: "ACTIVE" | "FINISHED" | "NONE"
     * - ACTIVE:   item đang có phiên SCHEDULED / OPEN / RUNNING  → không cho tạo mới
     * - FINISHED: item đã đấu giá xong (FINISHED)                → hiển thị badge
     * - NONE:     chưa có phiên nào                              → cho phép tạo
     */
    public String getAuctionStatusByItemId(long itemId) {
		// Kieermt tra trong cache
        boolean activeInCache = sharedState.auctionCache.values().stream()
                .anyMatch(a -> a.getItemId() == itemId &&
                        (a.getStatus() == Auction.AuctionStatus.SCHEDULED
                                || a.getStatus() == Auction.AuctionStatus.OPEN
                                || a.getStatus() == Auction.AuctionStatus.RUNNING));
        if (activeInCache) return "ACTIVE";
		
		// Kiểm tra trogn db, phòng TH cache chưa load
        List<Auction> dbAuctions = auctionRepository.findByItemId(itemId);
        for (Auction a : dbAuctions) {
            Auction.AuctionStatus s = a.getStatus();
            if (s == Auction.AuctionStatus.SCHEDULED || s == Auction.AuctionStatus.OPEN || s == Auction.AuctionStatus.RUNNING) {
                return "ACTIVE";
            }
        }
		
		// Kieermt tra đẫ có fisnish chauw
        boolean hasFinished = dbAuctions.stream().anyMatch(a -> a.getStatus() == Auction.AuctionStatus.FINISHED);
        if (hasFinished) return "FINISHED";

        return "NONE";
    }

	/*
	* Chuyển thành DTO -> Controller
	*/
    public AuctionDetailDTO toDetailDto(Auction auction) {
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        String bidderName = auction.getWinnerId() != null
                ? sharedState.resolveUserDisplayName(auction.getWinnerId(), "User")
                : "No bids";
        String sellerName = sharedState.resolveUserDisplayName(auction.getSellerId(), "Seller");

        Item item = itemService.getItemById(auction.getItemId());
        String itemName = (item != null && item.getName() != null) ? item.getName() : "Item #" + auction.getItemId();
        String itemDescription = (item != null && item.getDescription() != null) ? item.getDescription() : "(Không có mô tả)";
        String itemType = (item != null) ? itemService.extractItemType(item) : "GENERAL";
        Map<String, String> itemSpecifics = (item != null) ? itemService.extractItemSpecifics(item) : new HashMap<>();
        List<String> itemImageUrls = (item != null) ? item.getImageUrls() : new ArrayList<>();

        return new AuctionDetailDTO(
                auction.getId(), auction.getItemId(), itemName, itemDescription,
                auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO,
                bidderName, Math.max(0, remaining), auction.getStepPrice(),
                itemType, itemSpecifics, itemImageUrls, auction.getSellerId(), sellerName,
                auction.getStartTime().toString(), auction.getEndTime().toString()
        );
    }
}