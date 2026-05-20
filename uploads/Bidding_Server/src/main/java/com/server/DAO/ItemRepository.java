package com.server.DAO;

import com.server.config.DBConnection;
import com.server.exception.ItemException;
import com.server.model.Item;
import com.server.model.Art;
import com.server.model.Electronics;
import com.server.model.Vehicle;
import com.server.model.ItemFactory; // [MỚI] Gọi Factory vào làm việc

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemRepository {
    private static final Logger logger = LoggerFactory.getLogger(ItemRepository.class);

    // =====================================================================
    // HÀM 1: LƯU SẢN PHẨM MỚI VÀO DATABASE
    // [CHÚ THÍCH]: Hàm saveItem ta GIỮ NGUYÊN logic SQL if-else hiện tại.
    // Việc dùng Đa hình (Polymorphism) để khử if-else trong SQL là cực khó và
    // cần ORM (Hibernate). Hiện tại nó lưu đúng là được, không đụng vào!
    // =====================================================================
    public long saveItem(Item item) {
        String sql = "INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, " +
                "brand, model, warrantyMonths, artistName, material, hasCertificateOfAuthenticity, " +
                "manufactureYear, vinNumber, mileage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.getSellerId());
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setBigDecimal(5, item.getStartingPrice());
            pstmt.setString(6, item.getCondition());

            List<String> imgs = item.getImageUrls();
            String imageString = (imgs != null && !imgs.isEmpty()) ? String.join(",", imgs) : "";
            pstmt.setString(7, imageString);

            // Kiểm tra kiểu để lưu vào đúng cột
            if (item instanceof Electronics) {
                Electronics e = (Electronics) item;
                pstmt.setString(2, "Electronics");
                pstmt.setNull(11, java.sql.Types.VARCHAR); pstmt.setNull(12, java.sql.Types.VARCHAR); pstmt.setNull(13, java.sql.Types.BOOLEAN);
                pstmt.setNull(14, java.sql.Types.INTEGER); pstmt.setNull(15, java.sql.Types.VARCHAR); pstmt.setNull(16, java.sql.Types.INTEGER);
                pstmt.setString(8, e.getBrand()); pstmt.setString(9, e.getModel()); pstmt.setInt(10, e.getWarrantyMonths());
            } else if (item instanceof Art) {
                Art a = (Art) item;
                pstmt.setString(2, "Art");
                pstmt.setNull(8, java.sql.Types.VARCHAR); pstmt.setNull(9, java.sql.Types.VARCHAR); pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setNull(14, java.sql.Types.INTEGER); pstmt.setNull(15, java.sql.Types.VARCHAR); pstmt.setNull(16, java.sql.Types.INTEGER);
                pstmt.setString(11, a.getArtistName()); pstmt.setString(12, a.getMaterial()); pstmt.setBoolean(13, a.isHasCertificateOfAuthenticity());
            } else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                pstmt.setString(2, "Vehicle");
                pstmt.setNull(8, java.sql.Types.VARCHAR); pstmt.setNull(9, java.sql.Types.VARCHAR); pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setNull(11, java.sql.Types.VARCHAR); pstmt.setNull(12, java.sql.Types.VARCHAR); pstmt.setNull(13, java.sql.Types.BOOLEAN);
                pstmt.setInt(14, v.getManufactureYear()); pstmt.setString(15, v.getVinNumber()); pstmt.setInt(16, v.getMileage());
            }

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Không có dòng nào được lưu.");

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getLong(1);
            }
            throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Không lấy được ID sản phẩm vừa tạo.");

        } catch (SQLException e) {
            logger.error("LỖI LƯU ITEM VÀO DATABASE: {}", e.getMessage(), e);
            throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, e.getMessage());
        }
    }

    // =====================================================================
    // HÀM HELPER: TRÁI TIM CỦA VIỆC REFACTOR ÁP DỤNG FACTORY PATTERN (Đóng gói Logic)
    // =====================================================================
    /**
     * Biến 1 dòng ResultSet từ DB thành 1 đối tượng Item thông qua ItemFactory.
     * Repo từ nay không cần biết bên trong "Electronics" hay "Art" có thuộc tính gì!
     */
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String itemType = rs.getString("item_type");
        int id = rs.getInt("id");
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

        // Giao việc nặn Object cho Factory. Repo chỉ đứng nhìn và nhận thành quả!
        return ItemFactory.createItem(normalizedType, id, sellerId, name, description, startingPrice, condition, imgList, extraProps);
    }

    // =====================================================================
    // CÁC HÀM GET (Đã được dọn dẹp sạch sẽ nhờ mapRowToItem)
    // =====================================================================
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // [GỌN GÀNG] Chỉ 1 dòng thay vì 40 dòng if-else
                itemList.add(mapRowToItem(rs));
            }
        } catch (Exception e) {
            logger.error("LỖI LẤY DANH SÁCH ITEM: {}", e.getMessage(), e);
        }
        return itemList;
    }

    public List<Item> getItemsBySellerId(int sellerId) {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // [GỌN GÀNG]
                    itemList.add(mapRowToItem(rs));
                }
            }
        } catch (Exception e) {
            logger.error("LỖI LẤY DANH SÁCH ITEM CỦA SELLER {}: {}", sellerId, e.getMessage(), e);
        }
        return itemList;
    }

    public Item findItemById(long itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // [GỌN GÀNG]
                    return mapRowToItem(rs);
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi lấy thông tin sản phẩm cho itemId {}: {}", itemId, e.getMessage(), e);
        }
        return null;
    }

    // =====================================================================
    // CÁC HÀM KHÁC (Giữ nguyên)
    // =====================================================================
    public String findItemNameByItemId(long itemId) {
        String sql = "SELECT name FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy tên sản phẩm cho itemId {}: {}", itemId, e.getMessage(), e);
        }
        return null;
    }

    public boolean delete(long itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi xóa sản phẩm với itemId {}: {}", itemId, e.getMessage(), e);
            return false;
        }
    }

    private String normalizeItemType(String itemType) {
        return (itemType == null) ? "" : itemType.trim().toUpperCase();
    }
}