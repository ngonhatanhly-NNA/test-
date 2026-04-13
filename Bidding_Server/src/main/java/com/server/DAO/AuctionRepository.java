package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Auction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AuctionRepository implements IAuctionRepository {

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
            System.err.println("Lỗi tìm phiên đấu giá theo id: " + e.getMessage());
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
            System.err.println("Lỗi load phiên đấu giá: " + e.getMessage());
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
            System.err.println("Lỗi update phiên đấu giá: " + e.getMessage());
        }
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