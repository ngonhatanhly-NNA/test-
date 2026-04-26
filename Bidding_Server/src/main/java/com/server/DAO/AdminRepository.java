package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Admin;
import com.server.model.Seller;
import com.server.model.Status;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminRepository implements IAdminRepository {
    private static final Logger logger = LoggerFactory.getLogger(AdminRepository.class);

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
        } catch (SQLException e) {
            logger.error("Lỗi lấy admin theo username '{}': {}", username, e.getMessage(), e);
        }
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
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật last login IP cho admin {}: {}", adminId, e.getMessage(), e);
        }
    }

    // This method is now moved to UserRepository
    // @Override
    // public boolean updateUserStatus(long userId, Status newStatus) { ... }

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
            logger.info("Đã phê duyệt user {} thành seller với shop '{}'", seller.getId(), seller.getShopName());
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { logger.error("Rollback failed: {}", ex.getMessage(), ex); }
            logger.error("Lỗi phê duyệt seller cho user {}: {}", seller.getId(), e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { logger.error("Connection close failed: {}", e.getMessage(), e); }
        }
    }
}
