package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

/**
 * Implementation: Kiểm tra giá tối thiểu (minimum increment)
 * - Nếu chưa có ai đặt giá: giá đặt phải >= step price (giá khởi điểm)
 * - Nếu đã có giá: giá đặt phải >= current_highest_bid + step price
 */
public class MinimumIncrementValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        BigDecimal bidAmount = request.getBidAmount();
        BigDecimal stepPrice = auction.getStepPrice();
        BigDecimal currentHighestBid = auction.getCurrentHighestBid();

        // Xác định giá tối thiểu cần đặt
        BigDecimal minimumBid;

        if (currentHighestBid == null || currentHighestBid.compareTo(BigDecimal.ZERO) <= 0) {
            // Chưa có ai đặt giá: bắt đầu từ step price (giá khởi điểm)
            minimumBid = stepPrice;
        } else {
            // Đã có giá: phải >= current_highest_bid + step price
            minimumBid = currentHighestBid.add(stepPrice);
        }

        // Kiểm tra giá đặt có đủ tối thiểu không
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new AuctionException(
                AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                "Giá tối thiểu là " + minimumBid.toPlainString() + " VNĐ, bạn đặt " + bidAmount.toPlainString()
            );
        }
    }
}

