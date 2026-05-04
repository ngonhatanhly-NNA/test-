package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.AutoBidTracker;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository quản lý dữ liệu auto-bid trong database
 * Lưu trữ và truy vấn các yêu cầu tự động đặt giá của ngườii dùng
 */
public class AutoBidRepository {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidRepository.class);

    public AutoBidRepository() {}

    public void saveOrUpdate(AutoBidTracker autoBid) {
        String checkSql = "SELECT id FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";
        String updateSql = """
            UPDATE auto_bids SET max_bid_amount = ?, is_active = ?, custom_step_price = ?, updated_at = NOW()
            WHERE auction_id = ? AND bidder_id = ?
            """;
        String insertSql = """
            INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, is_active, custom_step_price, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            """;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setLong(1, autoBid.getAuctionId());
            checkStmt.setLong(2, autoBid.getBidderId());

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setBigDecimal(1, autoBid.getMaxBidAmount());
                        updateStmt.setBoolean(2, autoBid.isActive());
                        updateStmt.setBigDecimal(3, autoBid.getCustomStepPrice());
                        updateStmt.setLong(4, autoBid.getAuctionId());
                        updateStmt.setLong(5, autoBid.getBidderId());
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setLong(1, autoBid.getAuctionId());
                        insertStmt.setLong(2, autoBid.getBidderId());
                        insertStmt.setBigDecimal(3, autoBid.getMaxBidAmount());
                        insertStmt.setBoolean(4, autoBid.isActive());
                        insertStmt.setBigDecimal(5, autoBid.getCustomStepPrice());
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lưu auto-bid: {}", e.getMessage(), e);
        }
    }


    // Lấy auto-bid của một ngườii dùng cho một phiên đấu giá
    public AutoBidTracker findByAuctionAndBidder(long auctionId, long bidderId) {
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND bidder_id = ? AND is_active = true";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAutoBid(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy auto-bid cho auction {} bidder {}: {}", auctionId, bidderId, e.getMessage(), e);
        }
        return null;
    }

    // Lấy tất cả auto-bid hoạt động của một phiên đấu giá
    public List<AutoBidTracker> findAllActiveByAuction(long auctionId) {
        List<AutoBidTracker> autoBids = new ArrayList<>();
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND is_active = true";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    autoBids.add(mapResultSetToAutoBid(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy danh sách auto-bid cho auction {}: {}", auctionId, e.getMessage(), e);
        }
        return autoBids;
    }

    // Vô hiệu hóa auto-bid
    public void deactivate(long auctionId, long bidderId) {
        String sql = "UPDATE auto_bids SET is_active = false, updated_at = NOW() WHERE auction_id = ? AND bidder_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi vô hiệu hóa auto-bid cho auction {} bidder {}: {}", auctionId, bidderId, e.getMessage(), e);
        }
    }

    // Xóa auto-bid
    public void delete(long auctionId, long bidderId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi xóa auto-bid cho auction {} bidder {}: {}", auctionId, bidderId, e.getMessage(), e);
        }
    }

    private AutoBidTracker mapResultSetToAutoBid(ResultSet rs) throws SQLException {
        AutoBidTracker autoBid = new AutoBidTracker();
        autoBid.setAuctionId(rs.getLong("auction_id"));
        autoBid.setBidderId(rs.getLong("bidder_id"));

        String maxBidStr = rs.getString("max_bid_amount");
        autoBid.setMaxBidAmount(maxBidStr != null ? new BigDecimal(maxBidStr) : java.math.BigDecimal.ZERO);
        
        // Đọc custom_step_price
        String customStepStr = rs.getString("custom_step_price");
        autoBid.setCustomStepPrice(customStepStr != null ? new BigDecimal(customStepStr) : null);

        autoBid.setActive(rs.getBoolean("is_active"));
        return autoBid;
    }
}

