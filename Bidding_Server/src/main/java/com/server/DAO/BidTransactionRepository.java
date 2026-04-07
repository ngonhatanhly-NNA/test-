package com.server.DAO;
import com.server.config.DBConnection;
import com.server.model.BidTransaction;

import java.sql.*;

public class BidTransactionRepository {

    public BidTransactionRepository() {
    }

    public void save(BidTransaction bid) {
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        // ĐÃ SỬA: Gọi kết nối trực tiếp từ Singleton của HikariCP
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, bid.getAuctionId());
            pstmt.setLong(2, bid.getBidderId());

            // ĐÃ SỬA: Dùng chuẩn setBigDecimal cho tiền tệ
            pstmt.setBigDecimal(3, bid.getBidAmount());

            pstmt.setTimestamp(4, Timestamp.valueOf(bid.getTimestamp()));
            pstmt.setBoolean(5, bid.isAutoBid());
            pstmt.setTimestamp(6, Timestamp.valueOf(bid.getCreatedAt()));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving bid: " + e.getMessage());
        }
    }
}