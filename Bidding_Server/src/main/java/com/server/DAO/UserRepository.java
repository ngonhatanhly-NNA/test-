package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Admin;
import com.server.model.Bidder;
import com.server.model.Seller;
import com.server.model.User;
import com.server.model.Role;

import com.shared.dto.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepository implements IUserRepository {
	private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

	// ==========================================
	// 1. STRATEGY LƯU DỮ LIỆU TẠO MỚI (SAVE)
	// ==========================================
	private interface RoleDataSaver {
		void save(Connection conn, long userID, User user) throws Exception;
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

	// ==========================================
	// 2. STRATEGY CẬP NHẬT DỮ LIỆU (UPDATE)
	// ==========================================
	private interface RoleDataUpdater {
		void update(Connection conn, long userId, BaseProfileUpdateDTO dto) throws Exception;
	}

	private static final Map<Role, RoleDataUpdater> roleUpdaters = new HashMap<>();
	static {
		roleUpdaters.put(Role.ADMIN, (conn, id, dto) -> {
			AdminProfileUpdateDTO adminDto = (AdminProfileUpdateDTO) dto;
			// No-op for admin profile update
		});

		roleUpdaters.put(Role.BIDDER, (conn, id, dto) -> {
			BidderProfileUpdateDTO bidderDto = (BidderProfileUpdateDTO) dto;
			updateBidderData(conn, id, bidderDto);
		});

		roleUpdaters.put(Role.SELLER, (conn, id, dto) -> {
			SellerProfileUpdateDTO sellerDto = (SellerProfileUpdateDTO) dto;
			// Seller kế thừa Bidder nên nhét SellerDTO vào hàm của Bidder đc
			updateBidderData(conn, id, sellerDto);
			updateSellerData(conn, id, sellerDto);
		});
	}

	public boolean updateUser(User user) {
		String sql = "UPDATE users SET email=?, fullName=?, phoneNumber=?, address=? WHERE id=?";

		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, user.getEmail());
			pstmt.setString(2, user.getFullName());
			pstmt.setString(3, user.getPhoneNumber());
			pstmt.setString(4, user.getAddress());
			pstmt.setLong(5, user.getId());

			return pstmt.executeUpdate() > 0;

		} catch (Exception e) {
			logger.error("LỖI CẬP NHẬT USER DATABASE: {}", e.getMessage(), e);
			return false;
		}
	}

	public boolean updateFullProfile(BaseProfileUpdateDTO dto, Role role, long userId) {
		Connection conn = null;
		try {
			conn = DBConnection.getInstance().getConnection();
			conn.setAutoCommit(false);

			// Update thông tin chung (
			String sqlUser = "UPDATE users SET email=?, fullName=?, phoneNumber=?, address=? WHERE id=?";
			try (PreparedStatement psUser = conn.prepareStatement(sqlUser)) {
				psUser.setString(1, dto.getEmail());
				psUser.setString(2, dto.getFullName());
				psUser.setString(3, dto.getPhoneNumber());
				psUser.setString(4, dto.getAddress());
				psUser.setLong(5, userId);
				psUser.executeUpdate();
			}

			//Strategy để cập nhật các bảng con tuỳ theo role
			RoleDataUpdater updater = roleUpdaters.get(role);
			if (updater != null) updater.update(conn, userId, dto);

			conn.commit();
			return true;

		} catch (Exception e) {
			logger.error("LỖI CẬP NHẬT DB VÀO USER: {}", e.getMessage(), e);
			if (conn != null) {
				try { conn.rollback(); } catch (Exception ex) { logger.error("Rollback failed: {}", ex.getMessage(), ex); }
			}
			return false;
		} finally {
			if (conn != null) {
				try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { logger.error("Connection close failed: {}", e.getMessage(), e); }
			}
		}
	}

	public boolean updatePassword(String username, String newPassword) {
		String sql = "UPDATE users SET passwordHash = ? WHERE username = ?";

		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, newPassword);
			pstmt.setString(2, username);

			return pstmt.executeUpdate() > 0;

		} catch (Exception e) {
			logger.error("LỖI ĐỔI MẬT KHẨU TRONG DATABASE: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==========================================
	// 3. HÀM LƯU USER CHÍNH (Đã tuân thủ OCP)
	// ==========================================
	public boolean saveUser(User user) {
		String insertUserSql = "INSERT INTO users (username, passwordHash, email, fullName, phoneNumber, address, status, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		Connection conn = null;

		try {
			conn = DBConnection.getInstance().getConnection();
			conn.setAutoCommit(false);

			try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {

				pstmtUser.setString(1, user.getUsername());
				pstmtUser.setString(2, user.getPasswordHash());
				pstmtUser.setString(3, user.getEmail());
				pstmtUser.setString(4, user.getFullName());
				pstmtUser.setString(5, user.getPhoneNumber());
				pstmtUser.setString(6, user.getAddress());
				pstmtUser.setString(7, user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
				pstmtUser.setString(8, user.getRole() != null ? user.getRole().name() : "BIDDER");

				int affectedRows = pstmtUser.executeUpdate();
				if (affectedRows == 0) {
					conn.rollback();
					return false;
				}

				try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						long newUserId = generatedKeys.getLong(1);
						user.setId(newUserId);

						// Delegate việc lưu dữ liệu con cho Strategy
						RoleDataSaver saver = roleSavers.get(user.getClass());
						if (saver != null) saver.save(conn, newUserId, user);
					}
				}

				conn.commit();
				return true;
			}

		} catch (Exception e) {
			if (conn != null) {
				try { conn.rollback(); logger.error("LỖI LƯU DATABASE - ĐÃ ROLLBACK DỮ LIỆU!"); }
				catch (Exception rollbackEx) { logger.error("Rollback failed: {}", rollbackEx.getMessage(), rollbackEx); }
			}
			logger.error("LỖI LƯU USER VÀO DATABASE: {}", e.getMessage(), e);
			return false;

		} finally {
			if (conn != null) {
				try { conn.setAutoCommit(true); conn.close(); }
				catch (Exception ex) { logger.error("Connection close failed: {}", ex.getMessage(), ex); }
			}
		}
	}

	// ==========================================
	// 4. CÁC HÀM HELPER LƯU & CẬP NHẬT BẢNG CON
	// ==========================================
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

	private static void updateBidderData(Connection conn, long userId, BidderProfileUpdateDTO dto) throws Exception {
		try (PreparedStatement pstmt = conn.prepareStatement("UPDATE bidders SET creditCardInfo=? WHERE user_id=?")) {
			pstmt.setString(1, dto.getCreditCardInfo());
			pstmt.setLong(2, userId);
			pstmt.executeUpdate();
		}
	}

	private static void updateSellerData(Connection conn, long userId, SellerProfileUpdateDTO dto) throws Exception {
		// SỬ DỤNG UPSERT: Nếu chưa có thì INSERT, nếu có ID này rồi thì tự động chuyển sang UPDATE (Nếu chỉ update thì khi từ bidder update role seller sẽ bị lỗi vì chưa có record nào trong bảng sellers, còn nếu dùng UPSERT thì sẽ tự động thêm record mới vào nếu chưa có, hoặc cập nhật nếu đã tồn tại)
		String sql = "INSERT INTO sellers (bidder_id, shopName, bankAccountNumber) VALUES (?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE shopName = VALUES(shopName), bankAccountNumber = VALUES(bankAccountNumber)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, dto.getShopName());
			pstmt.setString(2, dto.getBankAccountNumber());
			pstmt.setLong(3, userId);
			pstmt.executeUpdate();
		}
	}

	//Todo: Update admin profile nếu có thêm trường nào cần update sau này thì làm tương tự như bidder/seller, tạo DTO riêng cho admin rồi thêm vào RoleDataUpdater

	// ==========================================
	// 5. CÁC HÀM GET & UPDATE PHÍA NGOÀI GỌI VÀO
	// ==========================================
	public User getUserByUsername(String username) {
		String sql = UserQueryFactory.getFullUserQuery();

		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, username);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String roleStr = rs.getString("role");
					Role roleEnum = (roleStr != null) ? Role.valueOf(roleStr.toUpperCase()) : Role.BIDDER;

					UserRowMapper mapper = UserRowMapperFactory.getMapperByRole(roleEnum);
					return mapper.mapRow(rs);
				}
			}
		} catch (Exception e) {
			logger.error("LỖI LẤY USER THEO USERNAME '{}': {}", username, e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Lấy User theo ID
	 */
	public User getUserById(long userId) {
		String sql = UserQueryFactory.getFullUserQueryById();

		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setLong(1, userId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String roleStr = rs.getString("role");
					Role roleEnum = (roleStr != null) ? Role.valueOf(roleStr.toUpperCase()) : Role.BIDDER;

					UserRowMapper mapper = UserRowMapperFactory.getMapperByRole(roleEnum);
					return mapper.mapRow(rs);
				}
			}
		} catch (Exception e) {
			logger.error("LỖI LẤY USER THEO ID {}: {}", userId, e.getMessage(), e);
		}
		return null;
	}

    /**
     * Cập nhật vai trò của một người dùng trong CSDL.
     * @param username Tên đăng nhập của người dùng.
     * @param newRole Vai trò mới.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    public boolean updateUserRole(String username, Role newRole) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.name()); // Chuyển Enum thành String (ví dụ: "SELLER")
            pstmt.setString(2, username);

            int affectedRows = pstmt.executeUpdate();
            logger.info("Cập nhật vai trò cho '{}' thành '{}', số dòng ảnh hưởng: {}", username, newRole.name(), affectedRows);
            return affectedRows > 0;
        } catch (Exception e) {
            logger.error("LỖI CẬP NHẬT VAI TRÒ CHO USER '{}': {}", username, e.getMessage(), e);
            return false;
        }
    }
}
