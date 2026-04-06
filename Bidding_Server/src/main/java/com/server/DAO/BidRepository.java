package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.BidTransaction;
import java.math.BigDecimal;
import com.server.config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BidRepository {

    // Lưu 1 lượt bid vào CSDL
    public void saveBidTransaction(int auctionId, BidTransaction transaction) {
        Connection conn = DBConnection.getDBConnection().getConnection();
        String sql = "INSERT INTO bid_history (auction_id, bidder_id, bid_amount, bid_time, is_auto_bid) VALUES (?, ?, ?, ?, ?)";

        // Sử dụng try-with-resources để TỰ ĐỘNG ĐÓNG kết nối sau khi chạy xong
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Truyền tham số vào dấu ? (Chống SQL Injection)
            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, transaction.getBidder().getId());

            // Ép kiểu double sang BigDecimal để lưu an toàn vào cột DECIMAL(15,2) trong MySQL
            pstmt.setBigDecimal(3, transaction.getBidAmount());

            // Chuyển LocalDateTime sang java.sql.Timestamp
            pstmt.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));

            pstmt.setBoolean(5, transaction.isAutoBid());

            // Thực thi lệnh INSERT
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu Bid Transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }
}