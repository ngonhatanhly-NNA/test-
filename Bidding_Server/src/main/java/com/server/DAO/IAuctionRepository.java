package com.server.DAO;

import com.server.model.Auction;
import com.server.model.BidTransaction;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Auction Repository
 * Dependency Inversion Principle: Depend on abstraction, not implementation
 */
public interface IAuctionRepository {
    List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses);

    Optional<Auction> findById(long auctionId);

    void save(Auction auction);

    long create(Auction auction);

    List<BidTransaction> findBidHistoryByAuction(long auctionId);

    /**
     * Finds the item name for a given item ID.
     * @param itemId The ID of the item.
     * @return The name of the item.
     */
    String findItemNameByItemId(long itemId);
}
