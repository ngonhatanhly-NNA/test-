package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Auction;
import com.server.model.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionRepository implements IAuctionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRepository.class);

    // ĐÃ XÓA Constructor truyền DBConnection vì chúng ta dùng Singleton
    public AuctionRepository() {
    }
    public ItemRepository itemRepository = new ItemRepository();

    /**
     * Chi tiết một phiên đấu giá (kể cả đã kết thúc / không còn trong cache).
     */
    public Optional<Auction> findById(long auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi tìm phiên đấu giá theo id {}: {}", auctionId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tên sản phẩm (bảng items) — dùng cho DTO chi tiết phiên đấu giá.
     */
    public String findItemNameByItemId(long itemId) {
        return itemRepository.findItemNameByItemId(itemId);
    }

    // Lấy các phiên đấu giá lên Cache khi Server vừa khởi động
    public List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses) {
        List<Auction> auctions = new ArrayList<>();
        if (statuses == null || statuses.isEmpty()) return auctions;

        List<String> statusList = statuses.stream().map(Enum::name).toList();

        // Tự động tạo số lượng dấu '?' tương ứng với số trạng thái truyền vào
        String placeholders = String.join(",", Collections.nCopies(statusList.size(), "?"));
        String sql = "SELECT * FROM auctions WHERE status IN (" + placeholders + ")";

        // ĐÃ SỬA: Gọi trực tiếp từ Singleton
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < statusList.size(); i++) {
                pstmt.setString(i + 1, statusList.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi load phiên đấu giá: {}", e.getMessage(), e);
        }
        return auctions;
    }

    // Cập nhật lại thông tin sau khi có người đặt giá thành công
    public void save(Auction auction) {
        String sql = """
            UPDATE auctions SET 
            current_highest_bid = ?, winner_id = ?, status = ?, end_time = ?, updated_at = ?
            WHERE id = ?
            """;

        // ĐÃ SỬA: Gọi trực tiếp từ Singleton (Code cũ của bạn gọi getDBConnection() bị lỗi)
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. CHỐNG VĂNG LỖI KHI CHƯA CÓ AI ĐẶT GIÁ VÀ DÙNG CHUẨN BIG DECIMAL
            if (auction.getCurrentHighestBid() != null) {
                pstmt.setBigDecimal(1, auction.getCurrentHighestBid()); // Dùng setBigDecimal thay vì setString
            } else {
                pstmt.setBigDecimal(1, BigDecimal.ZERO);
            }

            // 2. CHỐNG LỖI VỚI WINNER_ID
            if (auction.getWinnerId() != null && auction.getWinnerId() > 0) {
                pstmt.setLong(2, auction.getWinnerId());
            } else {
                pstmt.setNull(2, Types.BIGINT); // Lưu NULL vào DB
            }

            pstmt.setString(3, auction.getStatus().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));

            // Chống lỗi nếu updatedAt rỗng
            Timestamp updated = auction.getUpdatedAt() != null ?
                    Timestamp.valueOf(auction.getUpdatedAt()) :
                    Timestamp.valueOf(java.time.LocalDateTime.now());
            pstmt.setTimestamp(5, updated);

            pstmt.setLong(6, auction.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi update phiên đấu giá {}: {}", auction.getId(), e.getMessage(), e);
        }
    }

    /**
     * Tạo phiên đấu giá mới
     */
    public long create(Auction auction) {
        String sql = """
            INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, winner_id, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, auction.getItemId());
            pstmt.setLong(2, auction.getSellerId());
            pstmt.setTimestamp(3, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setBigDecimal(5, auction.getStepPrice() != null ? auction.getStepPrice() : BigDecimal.ZERO);
            pstmt.setBigDecimal(6, auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO);
            pstmt.setNull(7, Types.BIGINT);
            pstmt.setString(8, auction.getStatus().name());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi tạo phiên đấu giá: {}", e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Lấy lịch sử bid của một phiên đấu giá
     */
    public List<BidTransaction> findBidHistoryByAuction(long auctionId) {
        List<BidTransaction> bidHistory = new ArrayList<>();
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
                    bidHistory.add(bid);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy lịch sử bid cho auction {}: {}", auctionId, e.getMessage(), e);
        }
        return bidHistory;
    }

    // Đổi từ dữ liệu thô MySQL sang Object Java
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setItemId(rs.getLong("item_id"));
        auction.setSellerId(rs.getLong("seller_id"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) auction.setStartTime(startTime.toLocalDateTime());

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) auction.setEndTime(endTime.toLocalDateTime());

        String stepPriceStr = rs.getString("step_price");
        auction.setStepPrice(stepPriceStr != null ? new BigDecimal(stepPriceStr) : BigDecimal.ZERO);

        // CHỐNG LỖI NullPointerException KHI PARSE CHUỖI
        String highestBidStr = rs.getString("current_highest_bid");
        auction.setCurrentHighestBid(highestBidStr != null ? new BigDecimal(highestBidStr) : BigDecimal.ZERO);

        // Lấy Id người chiến thắng chuẩn xác
        long winnerId = rs.getLong("winner_id");
        auction.setWinnerId(rs.wasNull() ? null : winnerId);

        auction.setStatus(Auction.AuctionStatus.valueOf(rs.getString("status")));
        return auction;
    }
}

