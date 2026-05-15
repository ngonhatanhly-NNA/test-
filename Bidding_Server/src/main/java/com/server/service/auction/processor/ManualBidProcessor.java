package com.server.service.auction.processor;

import java.math.BigDecimal;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.DAO.AuctionRepository;
import com.server.DAO.BidderRepository;
import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.server.model.Bidder;
import com.shared.dto.BidRequestDTO;

/**
 * Implementation: Xử lý bid thủ công.
 *
 * 
 * - Trước đây dùng currentBid.add(bidAmount) → cộng dồn SAI.
 *   Giờ set thẳng bidAmount làm current_highest_bid mới.
 * - Trước đây KHÔNG gọi auctionRepository.save() → khi restart, giá bị mất.
 *   Giờ gọi save() ngay để ghi vào DB trước khi trả kết quả về client.
 *
 *   -> Đã sửa lỗi cộng dồn sai và lỗi không lưu giá vào DB, đảm bảo tính nhất quán và độ tin cậy của hệ thống.
 */
public class ManualBidProcessor implements BidProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ManualBidProcessor.class);
    private final AuctionRepository auctionRepository;
    private final BidderRepository bidderRepository;

    public ManualBidProcessor(AuctionRepository auctionRepository, BidderRepository bidderRepository) {
        this.auctionRepository = auctionRepository;
        this.bidderRepository = bidderRepository;
    }

    @Override
    public void process(BidRequestDTO request, Auction auction, Queue<BidTransaction> bidQueue) {
        if (auction.getStatus() == Auction.AuctionStatus.OPEN) {
            auction.setStatus(Auction.AuctionStatus.RUNNING);
        }

        // GIÁ MỚI CHÍNH LÀ GIÁ REQUEST GỬI LÊN (Không cộng dồn nữa)
        BigDecimal newHighestPrice = request.getBidAmount();
        BigDecimal currentHighestBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;

        // ================= LOGIC DÒNG TIỀN (FROZEN BALANCE) =================

        // 1. Đóng băng tiền người mới (Lấy toàn bộ số tiền newHighestPrice)
        Bidder newBidder = bidderRepository.getBidderById(request.getBidderId());
        if (newBidder != null) {
            BigDecimal main = newBidder.getWalletBalance();
            bidderRepository.updateWalletBalances(newBidder.getId(), main.subtract(newHighestPrice), newHighestPrice);
            logger.info("Đã đóng băng {} của Bidder {}", newHighestPrice, newBidder.getUsername());
        }

        // 2. Hoàn tiền người cũ (Outbid)
        if (auction.getWinnerId() != null && auction.getWinnerId() > 0 && auction.getWinnerId() != request.getBidderId()) {
            Bidder oldWinner = bidderRepository.getBidderById(auction.getWinnerId());
            if (oldWinner != null) {
                BigDecimal oldMain = oldWinner.getWalletBalance();
                // Hoàn lại số tiền currentHighestBid (giá cao nhất trước đó)
                bidderRepository.updateWalletBalances(oldWinner.getId(), oldMain.add(currentHighestBid), BigDecimal.ZERO);
                logger.info("Đã hoàn trả {} cho Bidder {}", currentHighestBid, oldWinner.getUsername());
            }
        }
        // =====================================================================

        auction.setCurrentHighestBid(newHighestPrice);
        auction.setWinnerId(request.getBidderId());

        // ưu ngay vào DB để giá không bị mất khi restart
        try {
            auctionRepository.save(auction);
        } catch (Exception e) {
            throw new RuntimeException("Không thể lưu giá đấu vào database: " + e.getMessage());
        }

        BidTransaction transaction = new BidTransaction(
                request.getAuctionId(),
                request.getBidderId(),
                newHighestPrice
        );
        transaction.setAutoBid(false);
        bidQueue.offer(transaction); // Queue này đẩy xuống DB không hề bị ghi đè!
    }
}