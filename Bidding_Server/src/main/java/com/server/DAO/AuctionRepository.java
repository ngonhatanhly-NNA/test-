package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Auction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AuctionRepository {
    private final DBConnection dbConnection; // Dùng chung 1 luồng kết nối

    public AuctionRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // Lấy các phiên đấu giá lên Cache khi Server vừa khởi động
    public List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses) {
        List<Auction> auctions = new ArrayList<>();
        if (statuses == null || statuses.isEmpty()) return auctions;

        List<String> statusList = statuses.stream().map(Enum::name).toList();

        // Tự động tạo số lượng dấu '?' tương ứng với số trạng thái truyền vào
        String placeholders = String.join(",", Collections.nCopies(statusList.size(), "?"));
        String sql = "SELECT * FROM auctions WHERE status IN (" + placeholders + ")";

        // Chú ý: Dùng dbConnection.getConnection() trực tiếp là đúng chuẩn
        try (Connection conn = dbConnection.getDBConnection().getConnection();
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

        try (Connection conn = dbConnection.getDBConnection().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. CHỐNG VĂNG LỖI KHI CHƯA CÓ AI ĐẶT GIÁ (NULL)
            if (auction.getCurrentHighestBid() != null) {
                pstmt.setString(1, auction.getCurrentHighestBid().toString());
            } else {
                pstmt.setString(1, "0");
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