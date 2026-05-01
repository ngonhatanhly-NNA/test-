package com.server.service.auction.processor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.DAO.AutoBidRepository;
import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.BidTransaction;

/**
 * Xử lý tự động bid khi có bid mới hoặc khi phiên đấu giá mở cửa
 * - Lấy danh sách auto bid active cho phiên đấu giá
 * - Kiểm tra điều kiện và cập nhật phiên đấu giá nếu có auto bid hợp lệ
 * - Đẩy giao dịch auto bid vào queue để xử lý bất đồng bộ sau này
 */
public class AutoBidProcessor{
    private final AutoBidRepository autoBidRepository;
    private static final Logger logger = LoggerFactory.getLogger(AutoBidProcessor.class);

    public AutoBidProcessor(AutoBidRepository autoBidRepository) {
        this.autoBidRepository = autoBidRepository;
    }

    /**
     * Tính toán giá bid tiếp theo với step price
     * Nếu current bid là 0, trả về step price
     * Nếu không, trả về current bid + step price
     */
    private BigDecimal calculateNextBidAmount(BigDecimal currentBid, BigDecimal stepPrice) {
        if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
            return stepPrice;
        }
        return currentBid.add(stepPrice);
    }

    /**
     * Xử lý auto-bid cho một phiên đấu giá
     * - Tính giá tối thiểu cần đặt
     * - So sánh với max amount của auto-bidder
     * - Nếu hợp lệ, đặt auto-bid, nếu không thì tắt auto-bid
     */
    public void process(Auction auction, Queue<BidTransaction> bidQueue) {
        List<AutoBidTracker> activeBids = autoBidRepository.findAllActiveByAuction(auction.getId());

        for (AutoBidTracker autoBid : activeBids) {
            // Bỏ qua nếu auto-bidder hiện tại là người dẫn đầu
            if (autoBid.getBidderId() == auction.getWinnerId()) {
                logger.trace("Auto-bid {} is already the winner for auction {}", autoBid.getBidderId(), auction.getId());
                continue;
            }

            BigDecimal currentBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;
            BigDecimal stepPrice = auction.getStepPrice();
            
            if (stepPrice == null || stepPrice.compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid step price for auction {}: {}", auction.getId(), stepPrice);
                autoBidRepository.deactivate(auction.getId(), autoBid.getBidderId());
                continue;
            }

            // Tính giá tối thiểu cần đặt (current bid + step price)
            BigDecimal minBid = calculateNextBidAmount(currentBid, stepPrice);

            // Kiểm tra xem auto-bidder có đủ budget không
            if (autoBid.getMaxBidAmount().compareTo(minBid) >= 0) {
                // Tính giá auto-bid final: min(minBid, maxAutoBidAmount)
                // Nếu max amount >= minBid, đặt giá là min bid của họ hoặc max amount (nếu max amount < next level)
                BigDecimal autoBidAmount = calculateAutoBidAmount(currentBid, minBid, autoBid.getMaxBidAmount());

                // Cập nhật trạng thái phiên (chỉ nếu autoBidAmount > currentBid)
                if (autoBidAmount.compareTo(currentBid) > 0) {
                    auction.setCurrentHighestBid(autoBidAmount);
                    auction.setWinnerId(autoBid.getBidderId());

                    // Đẩy vào Queue để xử lý bất đồng bộ
                    BidTransaction autoBidTx = new BidTransaction(auction.getId(), autoBid.getBidderId(), autoBidAmount);
                    autoBidTx.setAutoBid(true);
                    bidQueue.offer(autoBidTx);

                    logger.info("Auto-bid placed for auction {} bidder {}: amount={}, maxAmount={}", 
                        auction.getId(), autoBid.getBidderId(), autoBidAmount, autoBid.getMaxBidAmount());
                } else {
                    logger.debug("Auto-bid amount {} is not greater than current bid {} for auction {}", 
                        autoBidAmount, currentBid, auction.getId());
                }
            } else {
                // Tắt auto-bid nếu max amount < required minimum bid
                autoBidRepository.deactivate(auction.getId(), autoBid.getBidderId());
                logger.info("Auto-bid disabled for auction {} bidder {}: maxAmount={} < minBid={}", 
                    auction.getId(), autoBid.getBidderId(), autoBid.getMaxBidAmount(), minBid);
            }
        }
    }

    /**
     * Tính toán giá auto-bid cuối cùng
     * - Nếu maxAmount <= minBid: trả về maxAmount
     * - Nếu maxAmount > minBid: trả về minBid (để quyền lợi cho auto-bidder sau)
     * 
     * Điều này giúp auto-bid cạnh tranh công bằng
     */
    private BigDecimal calculateAutoBidAmount(BigDecimal currentBid, BigDecimal minBid, BigDecimal maxAmount) {
        // Nếu max amount < current bid, là bất thường (không nên tới đây)
        if (maxAmount.compareTo(currentBid) <= 0) {
            return currentBid;
        }
        
        // Nếu max amount < min bid cần đặt, đặt hết max amount
        if (maxAmount.compareTo(minBid) < 0) {
            return maxAmount;
        }
        
        // Nếu max amount >= min bid, chỉ đặt min bid để lưu budget cho lần sau
        return minBid;
    }
}