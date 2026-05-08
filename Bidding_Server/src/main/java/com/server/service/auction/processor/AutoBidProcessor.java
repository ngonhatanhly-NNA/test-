package com.server.service.auction.processor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;

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
        // 1. LẤY DATA TỪ DB ĐÚNG 1 LẦN
        List<AutoBidTracker> activeBids = autoBidRepository.findAllActiveByAuction(auction.getId());
        List<AutoBidTracker> deactivatedBids = new ArrayList<>();

        boolean priceChanged = true; // Cờ theo dõi xem trong một lượt quét có ai nâng giá không

        // Vòng lặp "Đấu trường": Chạy trên RAM cho đến khi phân thắng bại
        while (priceChanged) {
            priceChanged = false; // Reset cờ ở đầu mỗi hiệp đấu
            
            for (AutoBidTracker autoBid : activeBids) {
                // Bỏ qua nếu auto-bidder hiện tại đang là người dẫn đầu
                if (auction.getWinnerId() != null && autoBid.getBidderId() == auction.getWinnerId()) {
                    continue;
                }

                BigDecimal currentBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;
                
                // Lấy bước giá
                BigDecimal auctionStep = auction.getStepPrice();
                if (auctionStep == null || auctionStep.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal customStep = autoBid.getCustomStepPrice();
                BigDecimal actualStepPrice = auctionStep;
                if (customStep != null && customStep.compareTo(auctionStep) > 0) {
                    actualStepPrice = customStep;
                }

                BigDecimal minBid = calculateNextBidAmount(currentBid, actualStepPrice);

                // Kiểm tra xem auto-bidder có đủ budget không
                if (autoBid.getMaxBidAmount().compareTo(minBid) >= 0) {
                    BigDecimal autoBidAmount = calculateAutoBidAmount(currentBid, minBid, autoBid.getMaxBidAmount());

                    // Cập nhật trạng thái phiên nếu giá mới lớn hơn giá hiện tại
                    if (autoBidAmount.compareTo(currentBid) > 0) {
                        auction.setCurrentHighestBid(autoBidAmount);
                        auction.setWinnerId(autoBid.getBidderId());

                        // Đẩy vào Queue để xử lý bất đồng bộ
                        BidTransaction autoBidTx = new BidTransaction(auction.getId(), autoBid.getBidderId(), autoBidAmount);
                        autoBidTx.setAutoBid(true);
                        bidQueue.offer(autoBidTx);

                        // Ghi log đặt giá thành công ở đây
                        logger.info("Auto-bid placed for auction {} bidder {}: amount={}, maxAmount={}", 
                            auction.getId(), autoBid.getBidderId(), autoBidAmount, autoBid.getMaxBidAmount());

                        // CÓ NGƯỜI NÂNG GIÁ! Đánh dấu cờ true để bắt đầu hiệp đấu mới
                        priceChanged = true; 
                    }
                } else {
                    // KHÔNG ĐỦ TIỀN -> Đánh dấu bị loại (Chưa gọi DB vội)
                    autoBid.setActive(false);
                    deactivatedBids.add(autoBid);
                }
            }
            
            // Xóa những người đã hết tiền khỏi danh sách đấu trên RAM để hiệp sau không phải quét lại
            activeBids.removeIf(bid -> !bid.isActive());
        }
            
        // KẾT THÚC ĐẤU TRƯỜNG -> CẬP NHẬT DB NHỮNG NGƯỜI BỊ LOẠI 1 LẦN DUY NHẤT
        for (AutoBidTracker deactivated : deactivatedBids) {
            autoBidRepository.deactivate(auction.getId(), deactivated.getBidderId());
            logger.info("Auto-bid disabled for auction {} bidder {} due to insufficient max amount.", 
                        auction.getId(), deactivated.getBidderId());
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