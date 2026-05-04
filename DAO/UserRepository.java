package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.*;
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

	private interface RoleDataSaver {
		void save(Connection conn, long userID, User user) throws Exception;
	}

	private static final Map<Class<? extends User>, RoleDataSaver> roleSavers = new HashMap<>();
	static {
		roleSavers.put(Admin.class, (conn, id, user) -> saveAdminData(conn, id, (Admin) user));
		roleSavers.put(Bidder.class, (conn, id, user) -> saveBidderData(conn, id, (Bidder) user));
		roleSavers.put(Seller.class, (conn, id, user) -> {
			saveBidderData(conn, id, (Seller) user);
			saveSellerData(conn, id, (Seller) user);
		});
	}

	private interface RoleDataUpdater {
		void update(Connection conn, long userId, BaseProfileUpdateDTO dto) throws Exception;
	}

	private static final Map<Role, RoleDataUpdater> roleUpdaters = new HashMap<>();
	static {
		roleUpdaters.put(Role.ADMIN, (conn, id, dto) -> {});
		roleUpdaters.put(Role.BIDDER, (conn, id, dto) -> updateBidderData(conn, id, (BidderProfileUpdateDTO) dto));
		roleUpdaters.put(Role.SELLER, (conn, id, dto) -> {
			updateBidderData(conn, id, (SellerProfileUpdateDTO) dto);
			updateSellerData(conn, id, (SellerProfileUpdateDTO) dto);
		});
	}

	@Override
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
						RoleDataSaver saver = roleSavers.get(user.getClass());
						if (saver != null) saver.save(conn, newUserId, user);
					}
				}
				conn.commit();
				return true;
			}
		} catch (Exception e) {
			if (conn != null) {
				try { conn.rollback(); } catch (Exception rollbackEx) { logger.error("Rollback failed: {}", rollbackEx.getMessage(), rollbackEx); }
			}
			logger.error("LỖI LƯU USER VÀO DATABASE: {}", e.getMessage(), e);
			return false;
		} finally {
			if (conn != null) {
				try { conn.setAutoCommit(true); conn.close(); } catch (Exception ex) { logger.error("Connection close failed: {}", ex.getMessage(), ex); }
			}
		}
	}

	@Override
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

	@Override
	public boolean updateFullProfile(BaseProfileUpdateDTO dto, Role role, long userId) {
		Connection conn = null;
		try {
			conn = DBConnection.getInstance().getConnection();
			conn.setAutoCommit(false);
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

	@Override
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

	@Override
	public boolean updateUserStatus(long userId, Status newStatus) {
		String sql = "UPDATE users SET status = ? WHERE id = ?";
		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, newStatus.name());
			pstmt.setLong(2, userId);
			int affectedRows = pstmt.executeUpdate();
			if (affectedRows > 0) {
				logger.info("Đã cập nhật trạng thái cho user ID {} thành {}", userId, newStatus.name());
				return true;
			}
			return false;
		} catch (Exception e) {
			logger.error("Lỗi cập nhật trạng thái user {} sang {}: {}", userId, newStatus, e.getMessage(), e);
			return false;
		}
	}
    
    @Override
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

	public List<User> getAllUsers() {
		List<User> users = new ArrayList<>();
		String sql = UserQueryFactory.getFullUserQueryForAll();
		try (Connection conn = DBConnection.getInstance().getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql);
			 ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				String roleStr = rs.getString("role");
				Role roleEnum = (roleStr != null) ? Role.valueOf(roleStr.toUpperCase()) : Role.BIDDER;
				UserRowMapper mapper = UserRowMapperFactory.getMapperByRole(roleEnum);
				users.add(mapper.mapRow(rs));
			}
		} catch (Exception e) {
			logger.error("LỖI LẤY TẤT CẢ USER: {}", e.getMessage(), e);
		}
		return users;
	}
    
    public boolean updateUserRole(String username, Role newRole) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.name());
            pstmt.setString(2, username);

            int affectedRows = pstmt.executeUpdate();
            logger.info("Cập nhật vai trò cho '{}' thành '{}', số dòng ảnh hưởng: {}", username, newRole.name(), affectedRows);
            return affectedRows > 0;
        } catch (Exception e) {
            logger.error("LỖI CẬP NHẬT VAI TRÒ CHO USER '{}': {}", username, e.getMessage(), e);
            return false;
        }
    }

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
		String sql = "INSERT INTO sellers (bidder_id, shopName, bankAccountNumber) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE shopName = VALUES(shopName), bankAccountNumber = VALUES(bankAccountNumber)";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, dto.getShopName());
			pstmt.setString(2, dto.getBankAccountNumber());
			pstmt.setLong(3, userId);
			pstmt.executeUpdate();
		}
	}
    
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
}
