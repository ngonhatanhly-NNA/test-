package com.server.DAO;

import com.server.config.DBConnection;
import com.server.exception.ItemException;
import com.server.model.Item;
import com.server.model.Art;
import com.server.model.Electronics;
import com.server.model.Vehicle;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemRepository {
    private static final Logger logger = LoggerFactory.getLogger(ItemRepository.class);

    // =====================================================================
    // HÀM 1: LƯU SẢN PHẨM MỚI VÀO DATABASE (NÉM EXCEPTION CHUẨN)
    // =====================================================================
    public long saveItem(Item item) {
        // Khôi phục lại đúng tên cột SQL nguyên bản
        String sql = "INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, " +
                "brand, model, warrantyMonths, " +
                "artistName, material, hasCertificateOfAuthenticity, " +
                "manufactureYear, vinNumber, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // PHỤC HỒI LOGIC SET THAM SỐ CHI TIẾT
            pstmt.setInt(1, item.getSellerId());
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setBigDecimal(5, item.getStartingPrice());
            pstmt.setString(6, item.getCondition());

            List<String> imgs = item.getImageUrls();
            String imageString = (imgs != null && !imgs.isEmpty()) ? String.join(",", imgs) : "";
            pstmt.setString(7, imageString);

            // PHỤC HỒI ĐA HÌNH
            if (item instanceof Electronics) {
                Electronics e = (Electronics) item;
                pstmt.setString(2, "Electronics");

                pstmt.setNull(11, java.sql.Types.VARCHAR);
                pstmt.setNull(12, java.sql.Types.VARCHAR);
                pstmt.setNull(13, java.sql.Types.BOOLEAN);
                pstmt.setNull(14, java.sql.Types.INTEGER);
                pstmt.setNull(15, java.sql.Types.VARCHAR);
                pstmt.setNull(16, java.sql.Types.INTEGER);

                pstmt.setString(8, e.getBrand());
                pstmt.setString(9, e.getModel());
                pstmt.setInt(10, e.getWarrantyMonths());

            } else if (item instanceof Art) {
                Art a = (Art) item;
                pstmt.setString(2, "Art");

                pstmt.setNull(8, java.sql.Types.VARCHAR);
                pstmt.setNull(9, java.sql.Types.VARCHAR);
                pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setNull(14, java.sql.Types.INTEGER);
                pstmt.setNull(15, java.sql.Types.VARCHAR);
                pstmt.setNull(16, java.sql.Types.INTEGER);

                pstmt.setString(11, a.getArtistName());
                pstmt.setString(12, a.getMaterial());
                pstmt.setBoolean(13, a.isHasCertificateOfAuthenticity());

            } else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                pstmt.setString(2, "Vehicle");

                pstmt.setNull(8, java.sql.Types.VARCHAR);
                pstmt.setNull(9, java.sql.Types.VARCHAR);
                pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setNull(11, java.sql.Types.VARCHAR);
                pstmt.setNull(12, java.sql.Types.VARCHAR);
                pstmt.setNull(13, java.sql.Types.BOOLEAN);

                pstmt.setInt(14, v.getManufactureYear());
                pstmt.setString(15, v.getVinNumber());
                pstmt.setInt(16, v.getMileage());
            }

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Không có dòng nào được lưu.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Không lấy được ID sản phẩm vừa tạo.");

        } catch (SQLException e) {
            logger.error("LỖI LƯU ITEM VÀO DATABASE: {}", e.getMessage(), e);
            throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, e.getMessage());
        }
    }

    // =====================================================================
    // HÀM 2: LẤY TOÀN BỘ DANH SÁCH (PHỤC HỒI MAPPING CHI TIẾT)
    // =====================================================================
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
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

                String normalizedType = normalizeItemType(itemType);

                if ("ELECTRONICS".equals(normalizedType)) {
                    String brand = rs.getString("brand");
                    String model = rs.getString("model");
                    int warrantyMonths = rs.getInt("warrantyMonths");
                    Electronics e = new Electronics(id, name, description, startingPrice, condition, imgList, brand, model, warrantyMonths);
                    e.setSellerId(sellerId);
                    itemList.add(e);

                } else if ("ART".equals(normalizedType)) {
                    String artistName = rs.getString("artistName");
                    String material = rs.getString("material");
                    boolean hasCertificateOfAuthenticity = rs.getBoolean("hasCertificateOfAuthenticity");
                    Art a = new Art(id, name, description, startingPrice, condition, imgList, artistName, material, hasCertificateOfAuthenticity);
                    a.setSellerId(sellerId);
                    itemList.add(a);

                } else if ("VEHICLE".equals(normalizedType)) {
                    int manufactureYear = rs.getInt("manufactureYear");
                    String vinNumber = rs.getString("vinNumber");
                    int mileage = rs.getInt("mileage");
                    Vehicle v = new Vehicle(id, name, description, startingPrice, condition, imgList, manufactureYear, mileage, vinNumber);
                    v.setSellerId(sellerId);
                    itemList.add(v);
                }
            }
        } catch (Exception e) {
            logger.error("LỖI LẤY DANH SÁCH ITEM: {}", e.getMessage(), e);
        }

        return itemList;
    }

    // =====================================================================
    // HÀM 3: LẤY DANH SÁCH ITEM THEO SELLER_ID
    // =====================================================================
    public List<Item> getItemsBySellerId(int sellerId) {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        logger.info("ItemRepository.getItemsBySellerId: Executing query for seller_id = {}", sellerId);

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String itemType = rs.getString("item_type");
                    int id = rs.getInt("id");
                    int retrievedSellerId = rs.getInt("seller_id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    BigDecimal startingPrice = rs.getBigDecimal("startingPrice");
                    String condition = rs.getString("item_condition");

                    logger.debug("ItemRepository: Found item {} (type={}, sellerId={}, name={})", id, itemType, retrievedSellerId, name);

                    String imgString = rs.getString("imageUrls");
                    List<String> imgList = new ArrayList<>();
                    if (imgString != null && !imgString.isEmpty()) {
                        imgList = java.util.Arrays.asList(imgString.split(","));
                    }

                    String normalizedType = normalizeItemType(itemType);

                    if ("ELECTRONICS".equals(normalizedType)) {
                        String brand = rs.getString("brand");
                        String model = rs.getString("model");
                        int warrantyMonths = rs.getInt("warrantyMonths");
                        Electronics e = new Electronics(id, name, description, startingPrice, condition, imgList, brand, model, warrantyMonths);
                        e.setSellerId(retrievedSellerId);
                        itemList.add(e);

                    } else if ("ART".equals(normalizedType)) {
                        String artistName = rs.getString("artistName");
                        String material = rs.getString("material");
                        boolean hasCertificateOfAuthenticity = rs.getBoolean("hasCertificateOfAuthenticity");
                        Art a = new Art(id, name, description, startingPrice, condition, imgList, artistName, material, hasCertificateOfAuthenticity);
                        a.setSellerId(retrievedSellerId);
                        itemList.add(a);

                    } else if ("VEHICLE".equals(normalizedType)) {
                        int manufactureYear = rs.getInt("manufactureYear");
                        String vinNumber = rs.getString("vinNumber");
                        int mileage = rs.getInt("mileage");
                        Vehicle v = new Vehicle(id, name, description, startingPrice, condition, imgList, manufactureYear, mileage, vinNumber);
                        v.setSellerId(retrievedSellerId);
                        itemList.add(v);
                    }
                }
            }
            logger.info("ItemRepository.getItemsBySellerId: Found {} items for seller_id = {}", itemList.size(), sellerId);
        } catch (Exception e) {
            logger.error("LỖI LẤY DANH SÁCH ITEM CỦA SELLER {}: {}", sellerId, e.getMessage(), e);
        }

        return itemList;
    }

    // =====================================================================
    // HÀM 4: LẤY TÊN THEO ID (Giữ nguyên)
    // =====================================================================
    public String findItemNameByItemId(long itemId) {
        String sql = "SELECT name FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy tên sản phẩm cho itemId {}: {}", itemId, e.getMessage(), e);
        }
        return null;
    }

    public Item findItemById(long itemId){
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
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

                    String normalizedType = normalizeItemType(itemType);

                    if ("ELECTRONICS".equals(normalizedType)) {
                        String brand = rs.getString("brand");
                        String model = rs.getString("model");
                        int warrantyMonths = rs.getInt("warrantyMonths");
                        Electronics e = new Electronics(id, name, description, startingPrice, condition, imgList, brand, model, warrantyMonths);
                        e.setSellerId(sellerId);
                        return e;

                    } else if ("ART".equals(normalizedType)) {
                        String artistName = rs.getString("artistName");
                        String material = rs.getString("material");
                        boolean hasCertificateOfAuthenticity = rs.getBoolean("hasCertificateOfAuthenticity");
                        Art a = new Art(id, name, description, startingPrice, condition, imgList, artistName, material, hasCertificateOfAuthenticity);
                        a.setSellerId(sellerId);
                        return a;

                    } else if ("VEHICLE".equals(normalizedType)) {
                        int manufactureYear = rs.getInt("manufactureYear");
                        String vinNumber = rs.getString("vinNumber");
                        int mileage = rs.getInt("mileage");
                        Vehicle v = new Vehicle(id, name, description, startingPrice, condition, imgList, manufactureYear, mileage, vinNumber);
                        v.setSellerId(sellerId);
                        return v;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi lấy thông tin sản phẩm cho itemId {}: {}", itemId, e.getMessage(), e);
        }
        return null;
    }

    public boolean delete(long itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Lỗi xóa sản phẩm với itemId {}: {}", itemId, e.getMessage(), e);
            return false;
        }
    }

    private String normalizeItemType(String itemType) {
        if (itemType == null) {
            return "";
        }
        return itemType.trim().toUpperCase();
    }
}
