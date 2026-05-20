package com.server.service.auction.processor;

import java.math.BigDecimal;
import java.util.List;

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
    public void process(BidRequestDTO request, Auction auction, List<BidTransaction> bidBatch) {
        if (auction.getStatus() == Auction.AuctionStatus.OPEN) {
            auction.setStatus(Auction.AuctionStatus.RUNNING);
        }

        // GIÁ MỚI CHÍNH LÀ GIÁ REQUEST GỬI LÊN (Không cộng dồn nữa)
        BigDecimal newHighestPrice = request.getBidAmount();
        BigDecimal currentHighestBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;

        // ================= LOGIC DÒNG TIỀN (FROZEN BALANCE) =================

        // 1. Đóng băng tiền người mới (Lấy toàn bộ số tiền newHighestPrice)
        Bidder newBidder = bidderRepository.getBidderById(request.getBidderId());
        BigDecimal newBidderPrevMain = null;
        if (newBidder != null) {
            BigDecimal main = newBidder.getWalletBalance();
            newBidderPrevMain = main;
            boolean freezeOk = bidderRepository.updateWalletBalances(newBidder.getId(), main.subtract(newHighestPrice), newHighestPrice);
            if (!freezeOk) {
                logger.error("Không thể đóng băng tiền cho Bidder {}. Hủy bid.", newBidder.getUsername());
                throw new RuntimeException("Không thể đóng băng tiền. Vui lòng thử lại.");
            }
            logger.info("Đã đóng băng {} của Bidder {}", newHighestPrice, newBidder.getUsername());
        }

        // 2. Hoàn tiền người cũ (Outbid)
        Bidder oldWinner = null;
        BigDecimal oldWinnerPrevMain = null;
        if (auction.getWinnerId() != null && auction.getWinnerId() > 0 && auction.getWinnerId() != request.getBidderId()) {
            oldWinner = bidderRepository.getBidderById(auction.getWinnerId());
            if (oldWinner != null) {
                oldWinnerPrevMain = oldWinner.getWalletBalance();
                // Hoàn lại số tiền currentHighestBid (giá cao nhất trước đó)
                boolean refundOk = bidderRepository.updateWalletBalances(oldWinner.getId(), oldWinnerPrevMain.add(currentHighestBid), BigDecimal.ZERO);
                if (!refundOk) {
                    // If refund fails, revert new bidder freeze and abort
                    if (newBidder != null && newBidderPrevMain != null) {
                        bidderRepository.updateWalletBalances(newBidder.getId(), newBidderPrevMain, BigDecimal.ZERO);
                    }
                    logger.error("Không thể hoàn tiền cho người cũ {}. Hủy bid.", oldWinner.getUsername());
                    throw new RuntimeException("Không thể hoàn tiền cho người cũ. Hủy bid.");
                }
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
            // rollback wallet changes
            logger.error("Không thể lưu giá đấu vào database. Revert wallet balance.", e);
            if (newBidder != null && newBidderPrevMain != null) {
                bidderRepository.updateWalletBalances(newBidder.getId(), newBidderPrevMain, BigDecimal.ZERO);
                logger.warn("Đã revert ví cho Bidder {} do lỗi lưu auction", newBidder.getUsername());
            }
            if (oldWinner != null && oldWinnerPrevMain != null) {
                // attempt to restore oldWinner's tempBalance? we set temp to zero earlier
                bidderRepository.updateWalletBalances(oldWinner.getId(), oldWinnerPrevMain.subtract(currentHighestBid), currentHighestBid);
                logger.warn("Đã revert ví cho Old Winner {} do lỗi lưu auction", oldWinner.getUsername());
            }
            throw new RuntimeException("Không thể lưu giá đấu vào database: " + e.getMessage());
        }

        BidTransaction transaction = new BidTransaction(
                request.getAuctionId(),
                request.getBidderId(),
                newHighestPrice
        );
        transaction.setAutoBid(false);
        bidBatch.add(transaction);
    }
}
