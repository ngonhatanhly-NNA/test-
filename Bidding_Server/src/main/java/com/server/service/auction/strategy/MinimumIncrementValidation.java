package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

/**
 * Implementation: Kiểm tra giá tối thiểu (Minimum Increment)
 * Đối với hệ thống Delta Bidding: Số tiền gửi lên (Tiền cộng thêm) phải >= Bước giá quy định
 */
public class MinimumIncrementValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        BigDecimal addedAmount = request.getBidAmount(); // Đây chính là số tiền cộng thêm
        BigDecimal stepPrice = auction.getStepPrice();

        // 1. Nếu phiên đấu giá có quy định bước giá
        if (stepPrice != null && stepPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (addedAmount.compareTo(stepPrice) < 0) {
                throw new AuctionException(
                    AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                    "Số tiền tăng thêm (" + addedAmount.toPlainString() + 
                    " VNĐ) không được nhỏ hơn bước giá tối thiểu (" + stepPrice.toPlainString() + " VNĐ)"
                );
            }
        } 
        // 2. Nếu không quy định bước giá, ít nhất cũng phải cộng thêm một số tiền > 0
        else {
            if (addedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AuctionException(
                    AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                    "Số tiền tăng thêm phải lớn hơn 0"
                );
            }
        }
    }
}