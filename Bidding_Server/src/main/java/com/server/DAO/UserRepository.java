package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Admin;
import com.server.model.Bidder;
import com.server.model.Seller;
import com.server.model.User;
import com.server.model.Role;
import com.server.model.Status;

import com.shared.dto.UserProfileUpdateDTO;

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
			conn = DBConnection.getInstance().getConnection();

			// Tạm dừng auto-save để bật chế độ Transaction (Bảo vệ tính toàn vẹn dữ liệu)
			conn.setAutoCommit(false);

			try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {

				pstmtUser.setString(1, user.getUsername());
				pstmtUser.setString(2, user.getPasswordHash());
				pstmtUser.setString(3, user.getEmail());
				pstmtUser.setString(4, user.getFullName());
				pstmtUser.setString(5, user.getPhoneNumber());
				pstmtUser.setString(6, user.getAddress());

				// Lấy String từ Enum để lưu vào DB
				pstmtUser.setString(7, user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
				pstmtUser.setString(8, user.getRole() != null ? user.getRole().name() : "BIDDER");

				int affectedRows = pstmtUser.executeUpdate();
				if (affectedRows == 0) {
					conn.rollback(); // Lỗi thì quay xe
					return false;
				}

				// Lấy ID MySQL vừa cấp phát cho User mới (ĐÃ FIX SANG long)
				try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						long newUserId = generatedKeys.getLong(1);
						user.setId(newUserId);

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
			// Trả lại trạng thái bình thường cho Connection và ĐÓNG/TRẢ VỀ POOL
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close(); // QUAN TRỌNG: Lệnh này với HikariCP là trả kết nối về pool, tránh sập DB.
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	// --- CÁC HÀM HỖ TRỢ CHIA NHỎ (ĐÃ ĐỔI int userId THÀNH long userId) ---

	private void saveAdminData(Connection conn, long userId, Admin admin) throws Exception {
		String sql = "INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, userId);
			pstmt.setString(2, admin.getRoleLevel());
			pstmt.setString(3, admin.getLastLoginIp());
			pstmt.executeUpdate();
		}
	}

	private void saveBidderData(Connection conn, long userId, Bidder bidder) throws Exception {
		String sql = "INSERT INTO bidders (user_id, walletBalance, creditCardInfo) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, userId);
			pstmt.setBigDecimal(2, bidder.getWalletBalance());
			pstmt.setString(3, bidder.getCreditCardInfo());
			pstmt.executeUpdate();
		}
	}

	private void saveSellerData(Connection conn, long userId, Seller seller) throws Exception {
		String sql = "INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, bankAccountNumber, isVerified) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, userId);
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

		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, username);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					// Parse ra Enum
					Role roleEnum = Role.valueOf(rs.getString("role").toUpperCase());
					Status statusEnum = Status.valueOf(rs.getString("status").toUpperCase());

					// Đổi thành long id
					long id = rs.getLong("id");
					String pass = rs.getString("passwordHash");
					String email = rs.getString("email");
					String fullName = rs.getString("fullName");
					String phone = rs.getString("phoneNumber");
					String address = rs.getString("address");

					if (roleEnum == Role.ADMIN) {
						// Đã fix lỗi cú pháp Admin, truyền luôn roleLevel vào
						Admin admin = new Admin(id, username, pass, email, fullName, phone, address, statusEnum, rs.getString("roleLevel"));

						// Nếu Database có lưu LastLoginIp thì set luôn vào Object
						if (rs.getString("lastLoginIp") != null) {
							admin.updateLoginIp(rs.getString("lastLoginIp"));
						}
						return admin;

					} else if (roleEnum == Role.SELLER) {
						// Sử dụng biến statusEnum và roleEnum
						return new Seller(id, username, pass, email, fullName, phone, address, statusEnum, roleEnum,
								rs.getBigDecimal("walletBalance"),
								rs.getString("creditCardInfo"),
								rs.getString("shopName"),
								rs.getString("bankAccountNumber"),
								rs.getDouble("rating"),
								rs.getInt("totalReviews"),
								rs.getBoolean("isVerified"));

					} else {
						// Sử dụng biến statusEnum và roleEnum
						return new Bidder(id, username, pass, email, fullName, phone, address, statusEnum, roleEnum,
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

	public boolean updateFullProfile(UserProfileUpdateDTO dto, Role role, long userId) {
		Connection conn = null;
		try {
			conn = DBConnection.getInstance().getConnection();
			conn.setAutoCommit(false); // Bật Transaction

			// Basic user, can include seller and bidder
			String sqlUser = "UPDATE users SET email=?, fullName=?, phoneNumber=?, address=? WHERE id=?";
			try (PreparedStatement psUser = conn.prepareStatement(sqlUser)) {
				psUser.setString(1, dto.getEmail());
				psUser.setString(2, dto.getFullName());
				psUser.setString(3, dto.getPhoneNumber());
				psUser.setString(4, dto.getAddress());
				psUser.setLong(5, userId);
				psUser.executeUpdate();
			}

			// Nếu là BIDDER hoặc SELLER -> Cập nhật bảng bidders
			if (role == Role.BIDDER || role == Role.SELLER) {
				String sqlBidder = "UPDATE bidders SET creditCardInfo=? WHERE user_id=?";
				try (PreparedStatement psBidder = conn.prepareStatement(sqlBidder)) {
					psBidder.setString(1, dto.getCreditCardInfo());
					psBidder.setLong(2, userId);
					psBidder.executeUpdate();
				}
			}

			// Nếu là SELLER -> Cập nhật thêm bảng sellers
			if (role == Role.SELLER) {
				String sqlSeller = "UPDATE sellers SET shopName=?, bankAccountNumber=? WHERE bidder_id=?";
				try (PreparedStatement psSeller = conn.prepareStatement(sqlSeller)) {
					psSeller.setString(1, dto.getShopName());
					psSeller.setString(2, dto.getBankAccountNumber());
					psSeller.setLong(3, userId);
					psSeller.executeUpdate();
				}
			}

			conn.commit();
			return true;

		} catch (Exception e) {
			System.err.println("LỖI CẬP NHẬT DB VÀO USER");
			e.printStackTrace();
			if (conn != null) {
				try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
			}
			return false;
		} finally {
			if (conn != null) {
				try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { e.printStackTrace(); }
			}
		}
	}

	public boolean updatePassword(String username, String newPassword) {
		String sql = "UPDATE users SET passwordHash = ? WHERE username = ?";

		try (Connection conn = DBConnection.getInstance().getConnection();
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