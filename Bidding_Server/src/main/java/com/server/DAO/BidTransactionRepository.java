package com.server.DAO;
import com.server.config.DBConnection;
import com.server.model.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

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

    /**
     * Lấy tất cả bid của một phiên đấu giá
     */
    public List<BidTransaction> findByAuction(long auctionId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction(
                        rs.getLong("auction_id"),
                        rs.getLong("bidder_id"),
                        new BigDecimal(rs.getString("bid_amount"))
                    );
                    bid.setId(rs.getLong("id"));
                    bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy bid history: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Lấy lịch sử bid của một phiên đấu giá với giới hạn
     */
    public List<BidTransaction> findBidHistory(long auctionId, int limit) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction(
                        rs.getLong("auction_id"),
                        rs.getLong("bidder_id"),
                        new BigDecimal(rs.getString("bid_amount"))
                    );
                    bid.setId(rs.getLong("id"));
                    bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy bid history: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Lấy bid gần nhất của một phiên đấu giá
     */
    public BidTransaction findLatestBid(long auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BidTransaction bid = new BidTransaction(
                        rs.getLong("auction_id"),
                        rs.getLong("bidder_id"),
                        new BigDecimal(rs.getString("bid_amount"))
                    );
                    bid.setId(rs.getLong("id"));
                    bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    return bid;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy bid gần nhất: " + e.getMessage());
        }
        return null;
    }
}