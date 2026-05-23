package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

public class MinimumIncrementValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        BigDecimal desiredPrice = request.getBidAmount(); // Tổng tiền muốn mua
        BigDecimal currentPrice = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;
        BigDecimal stepPrice = auction.getStepPrice();

        // 1. Phải cao hơn giá hiện tại
        if (desiredPrice.compareTo(currentPrice) <= 0) {
            // [ĐÃ ANH-HÓA] Chuỗi này văng về Client
            throw new AuctionException(
                    AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                    "Bid amount (" + desiredPrice.toPlainString() + ") must be higher than current price (" + currentPrice.toPlainString() + ")."
            );
        }

        // 2. Độ chênh lệch phải >= Bước giá
        BigDecimal difference = desiredPrice.subtract(currentPrice);
        if (stepPrice != null && stepPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (difference.compareTo(stepPrice) < 0) {
                // [ĐÃ ANH-HÓA] Chuỗi này văng về Client
                throw new AuctionException(
                        AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                        "Bid increment is too low. You must add at least " + stepPrice.toPlainString() + " VND."
                );
            }
        }
    }
}