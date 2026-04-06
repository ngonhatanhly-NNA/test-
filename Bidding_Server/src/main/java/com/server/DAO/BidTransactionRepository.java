package com.server.DAO;
import com.server.config.*;
import com.server.model.BidTransaction;


import java.sql.*;

public class BidTransactionRepository {
    private final DBConnection dbConnection;

    public BidTransactionRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public void save(BidTransaction bid) {
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, bid.getAuctionId());
            pstmt.setLong(2, bid.getBidderId());
            pstmt.setString(3, bid.getBidAmount().toString());
            pstmt.setTimestamp(4, Timestamp.valueOf(bid.getTimestamp()));
            pstmt.setBoolean(5, bid.isAutoBid());
            pstmt.setTimestamp(6, Timestamp.valueOf(bid.getCreatedAt()));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving bid: " + e.getMessage());
        }
    }
}