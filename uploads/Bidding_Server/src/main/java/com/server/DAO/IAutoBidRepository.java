package com.server.DAO;

import com.server.model.AutoBidTracker;
import java.util.List;

/**
 * Interface for AutoBid Repository
 * Dependency Inversion Principle
 */
public interface IAutoBidRepository {
    void saveOrUpdate(AutoBidTracker autoBid);
    AutoBidTracker findByAuctionAndBidder(long auctionId, long bidderId);
    List<AutoBidTracker> findAllActiveByAuction(long auctionId);
    void deactivate(long auctionId, long bidderId);
    void delete(long auctionId, long bidderId);
}

