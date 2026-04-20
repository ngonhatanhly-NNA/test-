package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Admin;
import com.server.model.Seller;
import com.server.model.Status;
import java.sql.*;

public class AdminRepository implements IAdminRepository {

    @Override
    public Admin getAdminByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND role = 'ADMIN'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Admin admin = new Admin(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("email"),
                            rs.getString("full_name"), 
                            rs.getString("phone_number"), 
                            rs.getString("address"),
                            Status.valueOf(rs.getString("status")),
                            rs.getString("role_level") 
                    );
                    admin.updateLoginIp(rs.getString("lastLoginIp")); // Fix column
                    return admin;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void updateLastLoginIp(long adminId, String ip) {
        String sql = "UPDATE users SET last_login_ip = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.setLong(2, adminId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public boolean updateUserStatus(long userId, Status newStatus) {
        // SQL thuc hien luu trang thai moi vao database
        String sql = "UPDATE users SET status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name()); // Luu ten Enum (ACTIVE, BANNED...)
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean promoteToSeller(Seller seller) {
        String sqlUpdateUser = "UPDATE users SET role = 'SELLER', updated_at = NOW() WHERE id = ?";
        String sqlInsertSeller = "INSERT INTO sellers (user_id, shop_name, bank_account) VALUES (?, ?, ?)";
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlUpdateUser)) {
                ps1.setLong(1, seller.getId());
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(sqlInsertSeller)) {
                ps2.setLong(1, seller.getId());
                ps2.setString(2, seller.getShopName());
                ps2.setString(3, seller.getBankAccountNumber());
                ps2.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
