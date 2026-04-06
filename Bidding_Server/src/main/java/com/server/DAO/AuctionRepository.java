package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Auction;

import java.math.BigDecimal;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class AuctionRepository {
    private final DBConnection dbConnection; // Shared DBConnection

    public AuctionRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // Tìm các Auction đang hoạt động, cache server khởi động
    public List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses) {
        List<Auction> auctions = new ArrayList<>();
        List<String> statusList = Arrays.stream(statuses).map(Enum::name).toList();

        // placeholders: "OPEN", "RUNNING" -> ?, ?
        String placeholders = statusList.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM auctions WHERE status IN (" + placeholders + ")";

        try (Connection conn = dbConnection.getDBConnection().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < statusList.size(); i++) {
                pstmt.setString(i + 1, statusList.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = mapResultSetToAuction(rs);
                    auctions.add(auction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading auctions: " + e.getMessage());
        }
        return auctions;
    }


    // Ssave sau khi auction ket thuc ỏ bid thành công
    public void save(Auction auction) {
        String sql = """
            UPDATE auctions SET 
            current_highest_bid = ?, winner_id = ?, status = ?, end_time = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dbConnection.getDBConnection().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getCurrentHighestBid().toString());
            pstmt.setObject(2, auction.getWinnerId());
            pstmt.setString(3, auction.getStatus().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setTimestamp(5, Timestamp.valueOf(auction.getUpdatedAt()));
            pstmt.setLong(6, auction.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving auction: " + e.getMessage());
        }
    }

    // Chuyeenr DB thành Model
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setItemId(rs.getLong("item_id"));
        auction.setSellerId(rs.getLong("seller_id"));
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setStepPrice(new BigDecimal(rs.getString("step_price")));
        auction.setCurrentHighestBid(new BigDecimal(rs.getString("current_highest_bid")));
        auction.setWinnerId(rs.getLong("winner_id") > 0 ? rs.getLong("winner_id") : null);
        auction.setStatus(Auction.AuctionStatus.valueOf(rs.getString("status")));
        return auction;
    }
}