package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.*;
import com.shared.dto.TransferHistoryDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellerRepository implements ISellerRepository {
    private static final Logger logger = LoggerFactory.getLogger(SellerRepository.class);

    @Override
    public boolean promoteToSeller(long userId, String shopName, String bankAccountNumber) {
        String updateUserRoleSql = "UPDATE users SET role = ? WHERE id = ?";
        String insertSellerSql = "INSERT INTO sellers (bidder_id, shopName, bankAccountNumber) VALUES (?, ?, ?)";

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
        // DB dùng camelCase: shopName, bankAccountNumber
        String sql = "UPDATE sellers SET shopName = ?, bankAccountNumber = ? WHERE bidder_id = ?";
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
        String sql = "UPDATE sellers SET rating = ?, totalReviews = ? WHERE bidder_id = ?";
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
        // DB schema: users(camelCase), bidders(camelCase), sellers(camelCase + bidder_id PK)
        String sql = "SELECT u.id, u.username, u.passwordHash, u.email, u.fullName, u.phoneNumber, " +
                "u.address, u.status, u.role, " +
                "b.walletBalance, b.creditCardInfo, " +
                "s.shopName, s.bankAccountNumber, s.rating, s.totalReviews, s.isVerified " +
                "FROM users u " +
                "JOIN bidders b ON u.id = b.user_id " +
                "JOIN sellers s ON u.id = s.bidder_id " +
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

    @Override
    public List<Item> getItemsBySellerId(long sellerId) {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY id DESC";

        List<Item> itemList = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = mapResultSetToItem(rs);
                    if (item != null) {
                        itemList.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy items của seller {}: {}", sellerId, e.getMessage(), e);
        }
        return itemList;
    }

    /**
     * Lấy thống kê bán hàng của seller từ database.
     */
    @Override
    public Map<String, Object> getSellerStatistics(long sellerId) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Tổng số phiên đấu giá của seller
        String totalSql = "SELECT COUNT(*) AS totalAuctions FROM auctions WHERE seller_id = ?";
        // 2. Số phiên đã hoàn thành (SỬA: DB lưu FINISHED/CANCELED/PAID, không phải CLOSED/COMPLETED/ENDED)
        String completedSql = "SELECT COUNT(*) AS completedAuctions FROM auctions " +
                "WHERE seller_id = ? AND status IN ('FINISHED', 'CANCELED', 'PAID')";
        // 3. Số phiên đang hoạt động (SỬA LỖI 3: DB lưu OPEN/RUNNING, không phải ACTIVE)
        String activeSql = "SELECT COUNT(*) AS activeAuctions FROM auctions " +
                "WHERE seller_id = ? AND status IN ('OPEN', 'RUNNING')";
        // 4. Tổng doanh thu (tổng current_highest_bid của các phiên đã kết thúc có winner)
        String revenueSql = "SELECT COALESCE(SUM(current_highest_bid), 0) AS totalRevenue " +
                "FROM auctions WHERE seller_id = ? AND winner_id IS NOT NULL";
        // 5. Tổng số items
        String itemsSql = "SELECT COUNT(DISTINCT item_id) AS totalItems FROM auctions WHERE seller_id = ?";
        String inventorySql = "SELECT COUNT(*) AS totalItems FROM items WHERE seller_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(totalSql)) {
                ps.setLong(1, sellerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalAuctions", rs.getInt("totalAuctions"));
            }
            try (PreparedStatement ps = conn.prepareStatement(completedSql)) {
                ps.setLong(1, sellerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("completedAuctions", rs.getInt("completedAuctions"));
            }
            try (PreparedStatement ps = conn.prepareStatement(activeSql)) {
                ps.setLong(1, sellerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("activeAuctions", rs.getInt("activeAuctions"));
            }
            try (PreparedStatement ps = conn.prepareStatement(revenueSql)) {
                ps.setLong(1, sellerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalRevenue", rs.getBigDecimal("totalRevenue"));
            }
            try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                ps.setLong(1, sellerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalItems", rs.getInt("totalItems"));
            }
            try (PreparedStatement ps = conn.prepareStatement(inventorySql)) {
                ps.setLong(1, sellerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalItems", rs.getInt("totalItems"));
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy statistics cho seller {}: {}", sellerId, e.getMessage(), e);
        }

        return stats;
    }

    /**
     * Lấy lịch sử chuyển tiền của seller từ database.
     * Truy vấn các phiên đấu giá đã có winner, lấy thông tin buyer, số tiền và tên item.
     */
    @Override
    public List<TransferHistoryDTO> getTransferHistory(long sellerId) {
        List<TransferHistoryDTO> historyList = new ArrayList<>();
        String sql = "SELECT u.fullName AS buyerName, " +
                "a.current_highest_bid AS amount, " +
                "a.end_time AS transferTime, " +
                "i.name AS itemName " +
                "FROM auctions a " +
                "JOIN users u ON a.winner_id = u.id " +
                "JOIN items i ON a.item_id = i.id " +
                "WHERE a.seller_id = ? AND a.winner_id IS NOT NULL " +
                "ORDER BY a.end_time DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String buyerName = rs.getString("buyerName");
                    BigDecimal amount = rs.getBigDecimal("amount");
                    LocalDateTime transferTime = rs.getTimestamp("transferTime") != null
                            ? rs.getTimestamp("transferTime").toLocalDateTime()
                            : LocalDateTime.now();
                    String itemName = rs.getString("itemName");
                    historyList.add(new TransferHistoryDTO(buyerName, amount, transferTime, itemName));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy lịch sử chuyển tiền cho seller {}: {}", sellerId, e.getMessage(), e);
        }
        return historyList;
    }

    /**
     * Map ResultSet -> Seller (dùng tên cột camelCase theo DB schema).
     */
    private Seller mapResultSetToSeller(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("passwordHash");
        String email = rs.getString("email");
        String fullName = rs.getString("fullName");
        String phoneNumber = rs.getString("phoneNumber");
        String address = rs.getString("address");
        Status status = Status.valueOf(rs.getString("status"));
        Role role = Role.valueOf(rs.getString("role"));
        BigDecimal walletBalance = rs.getBigDecimal("walletBalance");
        String creditCardInfo = rs.getString("creditCardInfo");
        String shopName = rs.getString("shopName");
        String bankAccountNumber = rs.getString("bankAccountNumber");
        double rating = rs.getDouble("rating");
        int totalReviews = rs.getInt("totalReviews");
        boolean isVerified = rs.getBoolean("isVerified");

        return new Seller(id, username, passwordHash, email, fullName, phoneNumber, address,
                status, role, walletBalance, creditCardInfo,
                shopName, bankAccountNumber, rating, totalReviews, isVerified);
    }

    /**
     * Map ResultSet -> Item (Sử dụng Factory Method kết hợp Builder Pattern)
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        // [ĐÃ SỬA]: Lấy ĐẦY ĐỦ các thuộc tính chung từ ResultSet
        String itemType = rs.getString("item_type");
        int id = rs.getInt("id");

        // BỔ SUNG DÒNG NÀY: Móc cột seller_id từ Database ra để truyền cho Factory!
        int sellerId = rs.getInt("seller_id");

        String name = rs.getString("name");
        String description = rs.getString("description");
        BigDecimal startingPrice = rs.getBigDecimal("startingPrice");
        String condition = rs.getString("item_condition");

        String imgString = rs.getString("imageUrls");
        List<String> imgList = new ArrayList<>();
        if (imgString != null && !imgString.isEmpty()) {
            imgList = java.util.Arrays.asList(imgString.split(","));
        }

        //        //Thay thế Constructor cứng ngắc bằng Builder Pattern linh hoạt
//        if ("Electronics".equals(itemType)) {
//            return new Electronics.Builder()
//                    .id(id).name(name).description(description).startingPrice(startingPrice).condition(condition).imageUrls(imgList)
//                    .brand(rs.getString("brand"))
//                    .model(rs.getString("model"))
//                    .warrantyMonths(rs.getInt("warrantyMonths"))
//                    .build();
//        } else if ("Art".equals(itemType)) {
//            return new Art.Builder()
//                    .id(id).name(name).description(description).startingPrice(startingPrice).condition(condition).imageUrls(imgList)
//                    .artistName(rs.getString("artistName"))
//                    .material(rs.getString("material"))
//                    .hasCertificateOfAuthenticity(rs.getBoolean("hasCertificateOfAuthenticity"))
//                    .build();
//        } else if ("Vehicle".equals(itemType)) {
//            return new Vehicle.Builder()
//                    .id(id).name(name).description(description).startingPrice(startingPrice).condition(condition).imageUrls(imgList)
//                    .manufactureYear(rs.getInt("manufactureYear"))
//                    .mileage(rs.getInt("mileage"))
//                    .vinNumber(rs.getString("vinNumber"))
//                    .build();
//        }
//        return null;

        // Tống tất cả các cột thuộc tính riêng vào cái "Giỏ Đi Chợ" (Map)
        Map<String, Object> extraProps = new HashMap<>();
        extraProps.put("brand", rs.getString("brand"));
        extraProps.put("model", rs.getString("model"));
        extraProps.put("warrantyMonths", rs.getInt("warrantyMonths"));
        extraProps.put("artistName", rs.getString("artistName"));
        extraProps.put("material", rs.getString("material"));
        extraProps.put("hasCertificateOfAuthenticity", rs.getBoolean("hasCertificateOfAuthenticity"));
        extraProps.put("manufactureYear", rs.getInt("manufactureYear"));
        extraProps.put("vinNumber", rs.getString("vinNumber"));
        extraProps.put("mileage", rs.getInt("mileage"));

        // Chuẩn hóa chữ "Electronics" thành "ELECTRONICS" để khớp với Registry trong Factory
        String normalizedType = normalizeItemType(itemType);

        // Giao việc nặn Object cho Factory. Truyền đầy đủ sellerId vừa moi ra từ DB!
        return ItemFactory.createItem(normalizedType, id, sellerId, name, description, startingPrice, condition, imgList, extraProps);
    }

    private String normalizeItemType(String itemType) {
        return (itemType == null) ? "" : itemType.trim().toUpperCase();
    }
}