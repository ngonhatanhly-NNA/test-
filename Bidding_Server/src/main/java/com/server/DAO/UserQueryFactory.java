package com.server.DAO;

import com.server.model.Admin;
import com.server.model.Bidder;
import com.server.model.Role;
import com.server.model.Seller;
import com.server.model.User;

/**
 * Factory Pattern - Tạo các query riêng biệt cho từng role
 * Tuân thủ Single Responsibility Principle
 */
public class UserQueryFactory {

    public static String getUserQueryByRole(Role role) {
        switch (role) {
            case ADMIN:
                return getAdminQuery();
            case SELLER:
                return getSellerQuery();
            case BIDDER:
            default:
                return getBidderQuery();
        }
    }

    /**
     * Query lấy thông tin Admin
     */
    private static String getAdminQuery() {
        return "SELECT u.*, " +
                "a.roleLevel, a.lastLoginIp " +
                "FROM users u " +
                "LEFT JOIN admins a ON u.id = a.user_id " +
                "WHERE u.username = ?";
    }

    /**
     * Query lấy thông tin Seller (bao gồm cả Bidder vì Seller kế thừa từ Bidder)
     */
    private static String getSellerQuery() {
        return "SELECT u.*, " +
                "b.walletBalance, b.creditCardInfo, " +
                "s.shopName, s.rating, s.totalReviews, s.bankAccountNumber, s.isVerified " +
                "FROM users u " +
                "LEFT JOIN bidders b ON u.id = b.user_id " +
                "LEFT JOIN sellers s ON b.user_id = s.bidder_id " +
                "WHERE u.username = ?";
    }

    /**
     * Query lấy thông tin Bidder
     */
    private static String getBidderQuery() {
        return "SELECT u.*, " +
                "b.walletBalance, b.creditCardInfo " +
                "FROM users u " +
                "LEFT JOIN bidders b ON u.id = b.user_id " +
                "WHERE u.username = ?";
    }

    /**
     * Query lấy toàn bộ user information (dùng khi chưa biết role)
     */
    public static String getFullUserQuery() {
        return "SELECT u.*, " +
                "a.roleLevel, a.lastLoginIp, " +
                "b.walletBalance, b.creditCardInfo, " +
                "s.shopName, s.rating, s.totalReviews, s.bankAccountNumber, s.isVerified " +
                "FROM users u " +
                "LEFT JOIN admins a ON u.id = a.user_id " +
                "LEFT JOIN bidders b ON u.id = b.user_id " +
                "LEFT JOIN sellers s ON b.user_id = s.bidder_id " +
                "WHERE u.username = ?";
    }
}

