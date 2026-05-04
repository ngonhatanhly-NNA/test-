package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

/**
 * Xử lý Race Condition bằng cách so sánh giá kỳ vọng của Client
 * với giá thực tế đang có trên Server.
 */
public class OptimisticLockValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        // Giá hiện tại thực tế trên Server (có thể vừa bị ai đó mua)
        BigDecimal actualCurrentBid = auction.getCurrentHighestBid() != null ?
                                      auction.getCurrentHighestBid() : BigDecimal.ZERO;
        
        // Giá hiện tại mà người dùng đang nhìn thấy trên màn hình khi bấm nút
        BigDecimal expectedBid = request.getExpectedCurrentBid() != null ? 
                                 request.getExpectedCurrentBid() : BigDecimal.ZERO;

        // Xử lý Race Condition: Nếu giá bị lệch, lập tức đá văng request
        if (actualCurrentBid.compareTo(expectedBid) != 0) {
            throw new AuctionException(
                AuctionException.ErrorCode.INVALID_BID_AMOUNT, 
                "Giá sản phẩm đã bị cập nhật bởi người khác (Giá hiện tại: " + actualCurrentBid + "). Vui lòng tải lại trang và đặt lại!"
            );
        }
    }
}