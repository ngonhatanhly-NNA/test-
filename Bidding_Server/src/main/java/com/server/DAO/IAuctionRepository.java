package com.server.DAO;

import com.server.model.Auction;
import java.util.List;

/**
 * Interface for Auction Repository
 * Dependency Inversion Principle: Depend on abstraction, not implementation
 */
public interface IAuctionRepository {
    List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses);
    void save(Auction auction);
}

