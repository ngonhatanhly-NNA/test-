package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.math.BigDecimal;

/**
 * Implementation: Kiểm tra giá tối thiểu (minimum increment)
 */
public class MinimumIncrementValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        BigDecimal addedAmount = request.getBidAmount();
        BigDecimal stepPrice = auction.getStepPrice();


        if (addedAmount.compareTo(stepPrice) < 0) {
            throw new AuctionException(
                AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                "Giá tối thiểu là " + stepPrice + ", bạn đặt " + request.getBidAmount()
            );
        }
    }
}

