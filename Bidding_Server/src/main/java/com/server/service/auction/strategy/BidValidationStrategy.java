package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;

/**
 * Strategy Pattern: Xác thực giá đặt theo các quy tắc khác nhau
 */
public interface BidValidationStrategy {
    void validate(BidRequestDTO request, Auction auction) throws AuctionException;
}

