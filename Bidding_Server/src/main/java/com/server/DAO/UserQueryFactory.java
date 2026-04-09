package com.server.DAO;

import com.server.model.Role;
import java.util.EnumMap;
import java.util.Map;

/**
 * Factory + Registry Pattern - OCP Compliance
 */
public class UserQueryFactory {

    private static final Map<Role, String> queryRegistry = new EnumMap<>(Role.class);

    static {
        queryRegistry.put(Role.ADMIN, getAdminQuery());
        queryRegistry.put(Role.SELLER, getSellerQuery());
        queryRegistry.put(Role.BIDDER, getBidderQuery());
    }

    // Cho phép mở rộng động từ bên ngoài (Tuân thủ OCP )
    public static void registerQuery(Role role, String query) {
        queryRegistry.put(role, query);
    }

    public static String getUserQueryByRole(Role role) {
        return queryRegistry.getOrDefault(role, getBidderQuery());
    }

    private static String getAdminQuery() {
        return "SELECT u.*, a.roleLevel, a.lastLoginIp FROM users u LEFT JOIN admins a ON u.id = a.user_id WHERE u.username = ?";
    }

    private static String getSellerQuery() {
        return "SELECT u.*, b.walletBalance, b.creditCardInfo, s.shopName, s.rating, s.totalReviews, s.bankAccountNumber, s.isVerified " +
                "FROM users u LEFT JOIN bidders b ON u.id = b.user_id LEFT JOIN sellers s ON b.user_id = s.bidder_id WHERE u.username = ?";
    }

    private static String getBidderQuery() {
        return "SELECT u.*, b.walletBalance, b.creditCardInfo FROM users u LEFT JOIN bidders b ON u.id = b.user_id WHERE u.username = ?";
    }

    public static String getFullUserQuery() {
        return "SELECT u.*, a.roleLevel, a.lastLoginIp, b.walletBalance, b.creditCardInfo, s.shopName, s.rating, s.totalReviews, s.bankAccountNumber, s.isVerified " +
                "FROM users u LEFT JOIN admins a ON u.id = a.user_id LEFT JOIN bidders b ON u.id = b.user_id LEFT JOIN sellers s ON b.user_id = s.bidder_id " +
                "WHERE u.username = ?";
    }
}