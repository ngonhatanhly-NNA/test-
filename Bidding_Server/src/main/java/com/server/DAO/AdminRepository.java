package com.server.DAO;

import java.util.*;
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
	
	public List<Map<String, String>> getAllSystemTransactions() {
		List<Map<String, String>> list = new ArrayList<>();
		String sql = "SELECT wt.id, u.username, wt.type, wt.amount, wt.created_at " +
					 "FROM wallet_transactions wt " +
					 "JOIN users u ON wt.user_id = u.id " +
					 "ORDER BY wt.created_at DESC LIMIT 100";
		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql);
			 ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				Map<String, String> row = new HashMap<>();
				row.put("id", String.valueOf(rs.getLong("id")));
				row.put("username", rs.getString("username"));
				row.put("type", rs.getString("type"));        // cột trong DB là 'type'
				row.put("amount", rs.getBigDecimal("amount").toPlainString());
				row.put("time", rs.getTimestamp("created_at").toString());
				list.add(row);
			}
		} catch (SQLException e) {
			logger.error("Lỗi lấy giao dịch toàn hệ thống: {}", e.getMessage(), e);
		}
		return list;
	}

    // 2. Lấy toàn bộ danh sách phiên đấu giá (cả đã xong và đang chạy)
	public List<Map<String, String>> getAllAuctionsForAdmin() {
		List<Map<String, String>> list = new ArrayList<>();
		String sql = "SELECT a.id, i.name AS itemName, a.status, a.end_time " +
					 "FROM auctions a " +
					 "JOIN items i ON a.item_id = i.id " +
					 "ORDER BY a.created_at DESC";
		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql);
			 ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				Map<String, String> row = new HashMap<>();
				row.put("id", String.valueOf(rs.getLong("id")));
				row.put("itemName", rs.getString("itemName"));
				row.put("status", rs.getString("status"));
				Timestamp ts = rs.getTimestamp("end_time");
				row.put("endTime", ts != null ? ts.toString() : "N/A");
				list.add(row);
			}
		} catch (SQLException e) {
			logger.error("Lỗi lấy danh sách đấu giá: {}", e.getMessage(), e);
		}
		return list;
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
