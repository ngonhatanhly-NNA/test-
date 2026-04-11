package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.*;
import java.math.BigDecimal;
import java.sql.*;

public class BidderRepository implements IBidderRepository {

    @Override
    public boolean updateBalance(long bidderId, BigDecimal newBalance) {
        String sql = "UPDATE bidders SET walletBalance = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newBalance);
            pstmt.setLong(2, bidderId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Bidder getBidderByUsername(String username) {
        // Dùng JOIN để lấy dữ liệu từ cả 2 bảng Users và Bidders cùng lúc
        String sql = "SELECT u.*, b.walletBalance, b.creditCardInfo " +
                "FROM users u JOIN bidders b ON u.id = b.user_id " +
                "WHERE u.username = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Khớp chính xác với Constructor dành cho Database trong Bidder.java của Phong
                return new Bidder(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("email"),
                        rs.getString("fullName"),
                        rs.getString("phoneNumber"),
                        rs.getString("address"),
                        Status.valueOf(rs.getString("status").toUpperCase()), // ACTIVE, BANNED...
                        Role.valueOf(rs.getString("role").toUpperCase()),     // BIDDER, ADMIN...
                        rs.getBigDecimal("walletBalance"),
                        rs.getString("creditCardInfo")
                );
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm Bidder theo username: " + username);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Bidder getBidderById(long id) {
        String sql = "SELECT u.*, b.walletBalance, b.creditCardInfo " +
                "FROM users u JOIN bidders b ON u.id = b.user_id " +
                "WHERE u.id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Bidder(
                        rs.getLong("id"), rs.getString("username"), rs.getString("passwordHash"),
                        rs.getString("email"), rs.getString("fullName"), rs.getString("phoneNumber"),
                        rs.getString("address"), Status.valueOf(rs.getString("status")),
                        Role.valueOf(rs.getString("role")), rs.getBigDecimal("walletBalance"),
                        rs.getString("creditCardInfo")
                );
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}