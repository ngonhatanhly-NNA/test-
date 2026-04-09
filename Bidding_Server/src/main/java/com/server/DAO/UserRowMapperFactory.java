package com.server.DAO;

import com.server.model.*;

import java.sql.ResultSet;

/**
 * Strategy Pattern - Tạo các user converter riêng biệt cho từng role
 * Tuân thủ Open/Closed Principle - dễ mở rộng khi thêm role mới
 */
interface UserRowMapper {
    User mapRow(ResultSet rs) throws Exception;
}

/**
 * Converter cho Admin
 */
class AdminRowMapper implements UserRowMapper {
    @Override
    public User mapRow(ResultSet rs) throws Exception {
        Role roleEnum = Role.valueOf(rs.getString("role").toUpperCase());
        Status statusEnum = Status.valueOf(rs.getString("status").toUpperCase());

        long id = rs.getLong("id");
        String username = rs.getString("username");
        String pass = rs.getString("passwordHash");
        String email = rs.getString("email");
        String fullName = rs.getString("fullName");
        String phone = rs.getString("phoneNumber");
        String address = rs.getString("address");

        Admin admin = new Admin(id, username, pass, email, fullName, phone, address, statusEnum, rs.getString("roleLevel"));

        if (rs.getString("lastLoginIp") != null) {
            admin.updateLoginIp(rs.getString("lastLoginIp"));
        }
        return admin;
    }
}

/**
 * Converter cho Seller
 */
class SellerRowMapper implements UserRowMapper {
    @Override
    public User mapRow(ResultSet rs) throws Exception {
        Role roleEnum = Role.valueOf(rs.getString("role").toUpperCase());
        Status statusEnum = Status.valueOf(rs.getString("status").toUpperCase());

        long id = rs.getLong("id");
        String username = rs.getString("username");
        String pass = rs.getString("passwordHash");
        String email = rs.getString("email");
        String fullName = rs.getString("fullName");
        String phone = rs.getString("phoneNumber");
        String address = rs.getString("address");

        return new Seller(id, username, pass, email, fullName, phone, address, statusEnum, roleEnum,
                rs.getBigDecimal("walletBalance"),
                rs.getString("creditCardInfo"),
                rs.getString("shopName"),
                rs.getString("bankAccountNumber"),
                rs.getDouble("rating"),
                rs.getInt("totalReviews"),
                rs.getBoolean("isVerified"));
    }
}

/**
 * Converter cho Bidder
 */
class BidderRowMapper implements UserRowMapper {
    @Override
    public User mapRow(ResultSet rs) throws Exception {
        Role roleEnum = Role.valueOf(rs.getString("role").toUpperCase());
        Status statusEnum = Status.valueOf(rs.getString("status").toUpperCase());

        long id = rs.getLong("id");
        String username = rs.getString("username");
        String pass = rs.getString("passwordHash");
        String email = rs.getString("email");
        String fullName = rs.getString("fullName");
        String phone = rs.getString("phoneNumber");
        String address = rs.getString("address");

        return new com.server.model.Bidder(id, username, pass, email, fullName, phone, address, statusEnum, roleEnum,
                rs.getBigDecimal("walletBalance"),
                rs.getString("creditCardInfo"));
    }
}

/**
 * Factory Pattern - Tạo mapper phù hợp theo role
 * Thay thế các chuỗi instanceof dài
 */
public class UserRowMapperFactory {
    public static UserRowMapper getMapperByRole(Role role) {
        switch (role) {
            case ADMIN:
                return new AdminRowMapper();
            case SELLER:
                return new SellerRowMapper();
            case BIDDER:
            default:
                return new BidderRowMapper();
        }
    }
}

