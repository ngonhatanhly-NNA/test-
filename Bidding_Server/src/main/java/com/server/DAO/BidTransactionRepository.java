package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BidTransactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(BidTransactionRepository.class);

    public BidTransactionRepository() {
    }

    public void save(BidTransaction bid) {
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, event_seq, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bindBidInsertParams(pstmt, bid);

            pstmt.executeUpdate();
            logger.info("Bid transaction saved to DB: Auction {}, Bidder {}, Amount {}",
                    bid.getAuctionId(), bid.getBidderId(), bid.getBidAmount());
        } catch (SQLException e) {
            logger.error("Error saving bid for auction {}: {}", bid.getAuctionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save bid transaction for auction " + bid.getAuctionId(), e);
        }
    }

    public void saveBatchTransactional(List<BidTransaction> bids) {
        if (bids == null || bids.isEmpty()) {
            return;
        }
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, event_seq, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (BidTransaction bid : bids) {
                    bindBidInsertParams(pstmt, bid);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("Error saving bid batch for auction {}: {}",
                    bids.get(0).getAuctionId(), e.getMessage(), e);
            throw new RuntimeException("Không thể lưu bid batch vào database", e);
        }
    }

    public long findMaxEventSequenceByAuction(long auctionId) {
        String sql = "SELECT COALESCE(MAX(event_seq), 0) FROM bid_transactions WHERE auction_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy max event_seq cho auction {}: {}", auctionId, e.getMessage(), e);
        }
        return 0L;
    }

    /**
     * Lấy tất cả bid của một phiên đấu giá
     */
    public List<BidTransaction> findByAuction(long auctionId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = """
                SELECT * FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY event_seq DESC, id DESC
                """;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapBid(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy bid history cho auction {}: {}", auctionId, e.getMessage(), e);
        }
        return bids;
    }

    /**
     * Lấy lịch sử bid của một phiên đấu giá với giới hạn
     */
    public List<BidTransaction> findBidHistory(long auctionId, int limit) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = """
                SELECT * FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY event_seq DESC, id DESC
                LIMIT ?
                """;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapBid(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy bid history (limit {}) cho auction {}: {}", limit, auctionId, e.getMessage(), e);
        }
        return bids;
    }

    /**
     * Lấy bid gần nhất của một phiên đấu giá
     */
    public BidTransaction findLatestBid(long auctionId) {
        String sql = """
                SELECT * FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY event_seq DESC, id DESC
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapBid(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy bid gần nhất cho auction {}: {}", auctionId, e.getMessage(), e);
        }
        return null;
    }

    private void bindBidInsertParams(PreparedStatement pstmt, BidTransaction bid) throws SQLException {
        pstmt.setLong(1, bid.getAuctionId());
        pstmt.setLong(2, bid.getBidderId());
        pstmt.setBigDecimal(3, bid.getBidAmount());
        pstmt.setTimestamp(4, Timestamp.valueOf(bid.getTimestamp()));
        pstmt.setBoolean(5, bid.isAutoBid());
        pstmt.setLong(6, bid.getEventSequence());
        pstmt.setTimestamp(7, Timestamp.valueOf(bid.getCreatedAt()));
    }

    private BidTransaction mapBid(ResultSet rs) throws SQLException {
        BidTransaction bid = new BidTransaction(
                rs.getLong("auction_id"),
                rs.getLong("bidder_id"),
                new BigDecimal(rs.getString("bid_amount"))
        );
        bid.setId(rs.getLong("id"));
        bid.setAutoBid(rs.getBoolean("is_auto_bid"));
        bid.setEventSequence(rs.getLong("event_seq"));
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            bid.setTimestamp(timestamp.toLocalDateTime());
        }
        return bid;
    }
}