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

    /**
     * [MỚI] Lấy danh sách các phiên đấu giá đã kết thúc mà bidderId là người thắng.
     * Dùng để hiển thị "Won Auctions" ở My Inventory phía client.
     * @param bidderId ID của người dùng
     * @return Danh sách Auction có status=FINISHED và winner_id = bidderId
     */
    List<Auction> findWonAuctionsByBidderId(long bidderId);
}
