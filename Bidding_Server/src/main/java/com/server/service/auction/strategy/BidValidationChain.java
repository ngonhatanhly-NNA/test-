package com.server.service.auction.strategy;

import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.shared.dto.BidRequestDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * Chain of Responsibility Pattern: Xử lý chuỗi xác thực
 */
public class BidValidationChain {
    private final List<BidValidationStrategy> strategies;

    public BidValidationChain(List<BidValidationStrategy> strategies) {
        this.strategies = new ArrayList<>(strategies);
    }

    public void addStrategy(BidValidationStrategy strategy) {
        strategies.add(strategy);
    }

    /**
     * Chạy toàn bộ validation chain
     * Nếu bất kỳ strategy nào throw exception, ngừng ngay
     */
    public void validate(BidRequestDTO request, Auction auction) throws AuctionException {
        for (BidValidationStrategy strategy : strategies) {
            strategy.validate(request, auction);
        }
    }
}

