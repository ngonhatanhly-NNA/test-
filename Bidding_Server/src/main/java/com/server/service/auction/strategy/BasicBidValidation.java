package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;

/**
 * Implementation: Kiểm tra tính hợp lệ cơ bản của request và phiên đấu giá
 */
public class BasicBidValidation implements BidValidationStrategy {

    @Override
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        // Kiểm tra giá đặt không null
        if (request.getBidAmount() == null) {
            throw new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT, "Giá đặt không được để trống");
        }

        // Kiểm tra auction tồn tại
        if (auction == null) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
        }

        // Kiểm tra trạng thái auction
        if (auction.getStatus() != Auction.AuctionStatus.RUNNING &&
            auction.getStatus() != Auction.AuctionStatus.OPEN) {
            throw new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_ACTIVE);
        }
    }
}