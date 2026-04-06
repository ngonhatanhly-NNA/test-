package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Admin;
import com.server.model.Bidder;
import com.server.model.Seller;
import com.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class UserRepository {

	// Hàm lưu User tổng quát (Sử dụng tính đa hình OOP)
	public boolean saveUser(User user) {
		String insertUserSql = "INSERT INTO users (username, passwordHash, email, fullName, phoneNumber, address, status, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		Connection conn = null;

		try {
			// Lấy kết nối duy nhất của hệ thống
			conn = DBConnection.getDBConnection().getConnection();

			// Tạm dừng auto-save để bật chế độ Transaction (Bảo vệ tính toàn vẹn dữ liệu)
			conn.setAutoCommit(false);

			try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {

				pstmtUser.setString(1, user.getUsername());
				pstmtUser.setString(2, user.getPasswordHash());
				pstmtUser.setString(3, user.getEmail());
				pstmtUser.setString(4, user.getFullName());
				pstmtUser.setString(5, user.getPhoneNumber());
				pstmtUser.setString(6, user.getAddress());
				pstmtUser.setString(7, user.getStatus());
				pstmtUser.setString(8, user.getRole());

				int affectedRows = pstmtUser.executeUpdate();
				if (affectedRows == 0) {
					conn.rollback(); // Lỗi thì quay xe
					return false;
				}

				// Lấy ID MySQL vừa cấp phát cho User mới
				try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						int newUserId = generatedKeys.getInt(1);

						// CHIA NHÁNH LƯU VÀO CÁC BẢNG CON
						if (user instanceof Admin) {
							saveAdminData(conn, newUserId, (Admin) user);
						} else if (user instanceof Seller) {
							// Seller kế thừa Bidder nên phải lưu ở cả 2 bảng
							saveBidderData(conn, newUserId, (Seller) user);
							saveSellerData(conn, newUserId, (Seller) user);
						} else if (user instanceof Bidder) {
							saveBidderData(conn, newUserId, (Bidder) user);
						}
					}
				}

				// Mọi thứ hoàn hảo -> Xác nhận lưu vĩnh viễn (Commit)
				conn.commit();
				return true;
			}

		} catch (Exception e) {
			// Có lỗi -> Hủy toàn bộ thao tác, rollback dữ liệu
			if (conn != null) {
				try {
					conn.rollback();
					System.err.println("LỖI LƯU DATABASE - ĐÃ ROLLBACK DỮ LIỆU!");
				} catch (Exception rollbackEx) {
					rollbackEx.printStackTrace();
				}
			}
			e.printStackTrace();
			return false;

		} finally {
			// Trả lại trạng thái bình thường cho Connection (Không đóng vì dùng chung theo cấu trúc của bạn)
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	// --- CÁC HÀM HỖ TRỢ CHIA NHỎ (HELPER METHODS) ---

	private void saveAdminData(Connection conn, int userId, Admin admin) throws Exception {
		String sql = "INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, userId);
			pstmt.setString(2, admin.getRoleLevel());
			pstmt.setString(3, admin.getLastLoginIp());
			pstmt.executeUpdate();
		}
	}

	private void saveBidderData(Connection conn, int userId, Bidder bidder) throws Exception {
		String sql = "INSERT INTO bidders (user_id, walletBalance, creditCardInfo) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, userId);
			pstmt.setBigDecimal(2, bidder.getWalletBalance());
			pstmt.setString(3, bidder.getCreditCardInfo());
			pstmt.executeUpdate();
		}
	}

	private void saveSellerData(Connection conn, int userId, Seller seller) throws Exception {
		String sql = "INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, bankAccountNumber, isVerified) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, userId);
			pstmt.setString(2, seller.getShopName());
			pstmt.setDouble(3, seller.getRating());
			pstmt.setInt(4, seller.getTotalReviews());
			pstmt.setString(5, seller.getBankAccountNumber());
			pstmt.setBoolean(6, seller.isVerified());
			pstmt.executeUpdate();
		}
	}

	// Login & Lấy Profile
	public User getUserByUsername(String username) {
		String sql = "SELECT u.*, " +
				"a.roleLevel, a.lastLoginIp, " +
				"b.walletBalance, b.creditCardInfo, " +
				"s.shopName, s.rating, s.totalReviews, s.bankAccountNumber, s.isVerified " +
				"FROM users u " +
				"LEFT JOIN admins a ON u.id = a.user_id " +
				"LEFT JOIN bidders b ON u.id = b.user_id " +
				"LEFT JOIN sellers s ON b.user_id = s.bidder_id " +
				"WHERE u.username = ?";

		// Tạo connection trong try-with-resources để tự động dọn dẹp
		try (Connection conn = DBConnection.getDBConnection().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, username);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String role = rs.getString("role");
					int id = rs.getInt("id");
					String pass = rs.getString("passwordHash");
					String email = rs.getString("email");
					String fullName = rs.getString("fullName");
					String phone = rs.getString("phoneNumber");
					String address = rs.getString("address");
					String status = rs.getString("status");

					if ("ADMIN".equals(role)) {
						Admin admin = new Admin(id, username, pass, email, fullName, phone, address, status, role);
						admin.setRoleLevel(rs.getString("roleLevel"));
						return admin;

					} else if ("SELLER".equals(role)) {
						return new Seller(id, username, pass, email, fullName, phone, address, status, role,
								rs.getBigDecimal("walletBalance"),
								rs.getString("creditCardInfo"),
								rs.getString("shopName"),
								rs.getString("bankAccountNumber"),
								rs.getDouble("rating"),
								rs.getInt("totalReviews"),
								rs.getBoolean("isVerified"));

					} else {
						return new Bidder(id, username, pass, email, fullName, phone, address, status, role,
								rs.getBigDecimal("walletBalance"),
								rs.getString("creditCardInfo"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// Cập nhật các thông tin còn sót khi đăng ký chưa có
	public boolean updateUser (User user) {
		// Basic user, can inlude seller and bidder
		String sql = "UPDATE users set email = ?, phoneNumber = ?, address = ? WHERE username = ?";
		try (Connection conn = DBConnection.getDBConnection().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.setString(1, user.getEmail());
			pstmt.setString(2, user.getPhoneNumber());
			pstmt.setString(3, user.getAddress());
			pstmt.setString(4, user.getUsername());

			int rowsAffected = pstmt.executeUpdate();
			return rowsAffected > 0;

		} catch (Exception e) {
			System.err.println("LỖI CẬP NHẬT DB VÀO USER");
			e.printStackTrace();
			return false;
		}
	}

	public boolean updatePassword(String username, String newPassword) {
		String sql = "UPDATE users SET passwordHash = ? WHERE username = ?";

		try (Connection conn = DBConnection.getDBConnection().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, newPassword);
			pstmt.setString(2, username);

			int rowsAffected = pstmt.executeUpdate();
			return rowsAffected > 0;

		} catch (Exception e) {
			System.err.println("LỖI ĐỔI MẬT KHẨU TRONG DATABASE: ");
			e.printStackTrace();
			return false;
		}
	}
}