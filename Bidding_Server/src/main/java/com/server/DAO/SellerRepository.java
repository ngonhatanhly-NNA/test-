package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Seller;
import com.server.model.Role;
import com.server.model.Status;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellerRepository implements ISellerRepository {
    private static final Logger logger = LoggerFactory.getLogger(SellerRepository.class);

    @Override
    public boolean promoteToSeller(long userId, String shopName, String bankAccountNumber) {
        String updateUserRoleSql = "UPDATE users SET role = ? WHERE id = ?";
        String insertSellerSql = "INSERT INTO sellers (user_id, shop_name, bank_account_number) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement updateUserStmt = conn.prepareStatement(updateUserRoleSql);
                 PreparedStatement insertSellerStmt = conn.prepareStatement(insertSellerSql)) {

                updateUserStmt.setString(1, Role.SELLER.name());
                updateUserStmt.setLong(2, userId);
                int rowsUpdated = updateUserStmt.executeUpdate();

                insertSellerStmt.setLong(1, userId);
                insertSellerStmt.setString(2, shopName);
                insertSellerStmt.setString(3, bankAccountNumber);
                int rowsInserted = insertSellerStmt.executeUpdate();

                if (rowsUpdated > 0 && rowsInserted > 0) {
                    conn.commit();
                    logger.info("User {} đã được promote thành seller với shop '{}'", userId, shopName);
                    return true;
                } else {
                    conn.rollback();
                    logger.warn("Promote user {} thất bại, đã rollback", userId);
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Lỗi promote user {} thành seller: {}", userId, e.getMessage(), e);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Lỗi kết nối DB khi promote user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateShopDetails(long sellerId, String newShopName, String newBankAccount) {
        String sql = "UPDATE sellers SET shop_name = ?, bank_account_number = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newShopName);
            pstmt.setString(2, newBankAccount);
            pstmt.setLong(3, sellerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật shop details cho seller {}: {}", sellerId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateRating(long sellerId, double newRating, int newTotalReviews) {
        String sql = "UPDATE sellers SET rating = ?, total_reviews = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newRating);
            pstmt.setInt(2, newTotalReviews);
            pstmt.setLong(3, sellerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật rating cho seller {}: {}", sellerId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Seller findSellerByUserId(long userId) {
        String sql = "SELECT u.*, s.shop_name, s.rating, s.total_reviews, s.bank_account_number, s.is_verified, b.wallet_balance, b.credit_card_info " +
                     "FROM users u " +
                     "JOIN sellers s ON u.id = s.user_id " +
                     "JOIN bidders b ON u.id = b.user_id " +
                     "WHERE u.id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSeller(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi tìm seller theo userId {}: {}", userId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Sửa lại để sử dụng Constructor đầy đủ tham số của Seller,
     * thay vì dùng các hàm setter không tồn tại.
     */
    private Seller mapResultSetToSeller(ResultSet rs) throws SQLException {
        // Lấy tất cả dữ liệu từ ResultSet
        long id = rs.getLong("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String email = rs.getString("email");
        String fullName = rs.getString("full_name");
        String phoneNumber = rs.getString("phone_number");
        String address = rs.getString("address");
        Status status = Status.valueOf(rs.getString("status"));
        Role role = Role.valueOf(rs.getString("role"));
        BigDecimal walletBalance = rs.getBigDecimal("wallet_balance");
        String creditCardInfo = rs.getString("credit_card_info");
        String shopName = rs.getString("shop_name");
        String bankAccountNumber = rs.getString("bank_account_number");
        double rating = rs.getDouble("rating");
        int totalReviews = rs.getInt("total_reviews");
        boolean isVerified = rs.getBoolean("is_verified");

        // Gọi constructor đầy đủ tham số để tạo đối tượng Seller
        return new Seller(id, username, passwordHash, email, fullName, phoneNumber, address,
                          status, role, walletBalance, creditCardInfo,
                          shopName, bankAccountNumber, rating, totalReviews, isVerified);
    }
}

