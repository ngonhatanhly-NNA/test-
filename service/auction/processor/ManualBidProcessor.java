package com.server.service.auction.processor;

import com.server.DAO.AuctionRepository;
import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.shared.dto.BidRequestDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Queue;

/**
 * Implementation: Xử lý bid thủ công.
 *
 * SỬA LỖI 1:
 * - Trước đây dùng currentBid.add(bidAmount) → cộng dồn SAI.
 *   Giờ set thẳng bidAmount làm current_highest_bid mới.
 * - Trước đây KHÔNG gọi auctionRepository.save() → khi restart, giá bị mất.
 *   Giờ gọi save() ngay để ghi vào DB trước khi trả kết quả về client.
 */
public class ManualBidProcessor implements BidProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ManualBidProcessor.class);
    private final AuctionRepository auctionRepository;

    public ManualBidProcessor(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Override
    public void process(BidRequestDTO request, Auction auction, Queue<BidTransaction> bidQueue) {
        // Cập nhật trạng thái OPEN -> RUNNING nếu đây là bid đầu tiên
        if (auction.getStatus() == Auction.AuctionStatus.OPEN) {
            auction.setStatus(Auction.AuctionStatus.RUNNING);
        }

        // FIX: Dùng thẳng bidAmount làm giá mới, KHÔNG cộng dồn
        BigDecimal newHighestBid = request.getBidAmount();
        auction.setCurrentHighestBid(newHighestBid);
        auction.setWinnerId(request.getBidderId());

        // FIX: Lưu ngay vào DB để giá không bị mất khi restart
        try {
            auctionRepository.save(auction);
        } catch (Exception e) {
            logger.error("Lỗi lưu giá đấu vào DB cho auction {}: {}", auction.getId(), e.getMessage(), e);
            throw new RuntimeException("Không thể lưu giá đấu vào database: " + e.getMessage());
        }

        // Đưa BidTransaction vào queue để lưu lịch sử giao dịch bất đồng bộ
        BidTransaction transaction = new BidTransaction(
                request.getAuctionId(),
                request.getBidderId(),
                newHighestBid
        );
        transaction.setAutoBid(false);
        bidQueue.offer(transaction);
    }
}