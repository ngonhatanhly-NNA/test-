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
import java.util.*;

public class UserRepository {

	private interface RoleDataSaver {
		void save (Connection conn, long userID, User user) throws Exception;
	}
	private static final Map<Class<? extends User>, RoleDataSaver> roleSavers = new HashMap<>();
	static {
		roleSavers.put(Admin.class, (conn, id, user) -> saveAdminData(conn, id, (Admin) user));
		roleSavers.put(Bidder.class, (conn, id, user) -> saveBidderData(conn, id, (Bidder) user));
		roleSavers.put(Seller.class, (conn, id, user) -> {
			saveBidderData(conn, id, (Seller) user); // Seller kế thừa Bidder
			saveSellerData(conn, id, (Seller) user);
		});
	}

	private interface RoleDataUpdater {
		void update(Connection conn, long userId, UserProfileUpdateDTO dto) throws Exception;
	}

	private static final Map<Role, RoleDataUpdater> roleUpdaters = new HashMap<>();
	static {
		roleUpdaters.put(Role.ADMIN, (conn, id, dto) -> { /* Admin không có profile phụ trong DTO này */ });
		roleUpdaters.put(Role.BIDDER, (conn, id, dto) -> updateBidderData(conn, id, dto));
		roleUpdaters.put(Role.SELLER, (conn, id, dto) -> {
			updateBidderData(conn, id, dto);
			updateSellerData(conn, id, dto);
		});
	}
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
						// Nay tuan theo tieu chuan OOP, Closed gi a :))
						RoleDataSaver saver = roleSavers.get(user.getClass());
						if (saver != null) saver.save(conn, newUserId, user);
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

	// --- CÁC HÀM HỖ TRỢ CHIA NHỎ ---

	private static void saveAdminData(Connection conn, long userId, Admin admin) throws Exception {
		String sql = "INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, userId);
			pstmt.setString(2, admin.getRoleLevel());
			pstmt.setString(3, admin.getLastLoginIp());
			pstmt.executeUpdate();
		}
	}

	private static void saveBidderData(Connection conn, long userId, Bidder bidder) throws Exception {
		String sql = "INSERT INTO bidders (user_id, walletBalance, creditCardInfo) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, userId);
			pstmt.setBigDecimal(2, bidder.getWalletBalance());
			pstmt.setString(3, bidder.getCreditCardInfo());
			pstmt.executeUpdate();
		}
	}

	private static void saveSellerData(Connection conn, long userId, Seller seller) throws Exception {
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

	// HELPER_FUNCTION for update
	private static void updateBidderData(Connection conn, long userId, UserProfileUpdateDTO dto) throws Exception {
		try (PreparedStatement pstmt = conn.prepareStatement("UPDATE bidders SET creditCardInfo=? WHERE user_id=?")) {
			pstmt.setString(1, dto.getCreditCardInfo()); pstmt.setLong(2, userId); pstmt.executeUpdate();
		}
	}
	private static void updateSellerData(Connection conn, long userId, UserProfileUpdateDTO dto) throws Exception {
		try (PreparedStatement pstmt = conn.prepareStatement("UPDATE sellers SET shopName=?, bankAccountNumber=? WHERE bidder_id=?")) {
			pstmt.setString(1, dto.getShopName()); pstmt.setString(2, dto.getBankAccountNumber()); pstmt.setLong(3, userId); pstmt.executeUpdate();
		}
	}

	// Login & Lấy Profile
	public User getUserByUsername(String username) {
		String sql = UserQueryFactory.getFullUserQuery();

		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, username);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					// Parse ra Enum
					Role roleEnum = Role.valueOf(rs.getString("role").toUpperCase());
					// Sử dụng Factory method để tránh ì, else, sau update dễ hơn
					UserRowMapper mapper = UserRowMapperFactory.getMapperByRole(roleEnum);
					return mapper.mapRow(rs);
			}}
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

			RoleDataUpdater updater = roleUpdaters.get(role);
			if (updater != null) updater.update(conn, userId, dto);

			conn.commit();
			return true;

		} catch (Exception e) {
			System.err.println("LỖI CẬP NHẬT DB VÀO USER");
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