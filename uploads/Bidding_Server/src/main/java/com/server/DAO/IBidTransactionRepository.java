package com.server.DAO;

import com.server.model.BidTransaction;

/**
 * Interface for BidTransaction Repository
 * Dependency Inversion Principle
 */
public interface IBidTransactionRepository {
    void save(BidTransaction bid);
}

