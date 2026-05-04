package com.server.DAO;

import com.server.model.*;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;

// Mapper xuwr lis cac du lieu tho tu DB len Java
interface UserRowMapper {
    User mapRow(ResultSet rs) throws Exception;
}

class AdminRowMapper implements UserRowMapper {
    @Override
    public User mapRow(ResultSet rs) throws Exception {
        Role roleEnum = Role.valueOf(rs.getString("role").toUpperCase());
        Status statusEnum = Status.valueOf(rs.getString("status").toUpperCase());
        Admin admin = new Admin(rs.getLong("id"), rs.getString("username"), rs.getString("passwordHash"), rs.getString("email"),
                rs.getString("fullName"), rs.getString("phoneNumber"), rs.getString("address"), statusEnum, rs.getString("roleLevel"));
        if (rs.getString("lastLoginIp") != null) {
            admin.updateLoginIp(rs.getString("lastLoginIp"));
        }
        return admin;
    }
}

class SellerRowMapper implements UserRowMapper {
    @Override
    public User mapRow(ResultSet rs) throws Exception {
        return new Seller(rs.getLong("id"), rs.getString("username"), rs.getString("passwordHash"), rs.getString("email"),
                rs.getString("fullName"), rs.getString("phoneNumber"), rs.getString("address"),
                Status.valueOf(rs.getString("status").toUpperCase()), Role.valueOf(rs.getString("role").toUpperCase()),
                rs.getBigDecimal("walletBalance"), rs.getString("creditCardInfo"), rs.getString("shopName"),
                rs.getString("bankAccountNumber"), rs.getDouble("rating"), rs.getInt("totalReviews"), rs.getBoolean("isVerified"));
    }
}

class BidderRowMapper implements UserRowMapper {
    @Override
    public User mapRow(ResultSet rs) throws Exception {
        return new Bidder(rs.getLong("id"), rs.getString("username"), rs.getString("passwordHash"), rs.getString("email"),
                rs.getString("fullName"), rs.getString("phoneNumber"), rs.getString("address"),
                Status.valueOf(rs.getString("status").toUpperCase()), Role.valueOf(rs.getString("role").toUpperCase()),
                rs.getBigDecimal("walletBalance"), rs.getString("creditCardInfo"));
    }
}

public class UserRowMapperFactory {
    private static final Map<Role, UserRowMapper> registry = new EnumMap<>(Role.class);

    static {
        registry.put(Role.ADMIN, new AdminRowMapper());
        registry.put(Role.SELLER, new SellerRowMapper());
        registry.put(Role.BIDDER, new BidderRowMapper());
    }

    public static void registerMapper(Role role, UserRowMapper mapper) {
        registry.put(role, mapper);
    }

    public static UserRowMapper getMapperByRole(Role role) {
        return registry.getOrDefault(role, new BidderRowMapper());
    }
}