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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemRepository {
    private static final Logger logger = LoggerFactory.getLogger(ItemRepository.class);

    // =====================================================================
    // HÀM 1: LƯU SẢN PHẨM MỚI VÀO DATABASE (NÉM EXCEPTION CHUẨN)
    // =====================================================================
    public void saveItem(Item item) {
        // Khôi phục lại đúng tên cột SQL nguyên bản
        String sql = "INSERT INTO items (item_type, name, description, startingPrice, item_condition, imageUrls, " +
                "brand, model, warrantyMonths, " +
                "artistName, material, hasCertificateOfAuthenticity, " +
                "manufactureYear, vinNumber, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // PHỤC HỒI LOGIC SET THAM SỐ CHI TIẾT
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription()); // Đã vá lỗi thiếu tham số 3 hôm trước!
            pstmt.setBigDecimal(4, item.getStartingPrice());
            pstmt.setString(5, item.getCondition());

            List<String> imgs = item.getImageUrls();
            String imageString = (imgs != null && !imgs.isEmpty()) ? String.join(",", imgs) : "";
            pstmt.setString(6, imageString);

            // PHỤC HỒI ĐA HÌNH
            if (item instanceof Electronics) {
                Electronics e = (Electronics) item;
                pstmt.setString(1, "Electronics");

                pstmt.setNull(10, java.sql.Types.VARCHAR);
                pstmt.setNull(11, java.sql.Types.VARCHAR);
                pstmt.setNull(12, java.sql.Types.BOOLEAN);
                pstmt.setNull(13, java.sql.Types.INTEGER);
                pstmt.setNull(14, java.sql.Types.VARCHAR);
                pstmt.setNull(15, java.sql.Types.INTEGER);

                pstmt.setString(7, e.getBrand());
                pstmt.setString(8, e.getModel());
                pstmt.setInt(9, e.getWarrantyMonths());

            } else if (item instanceof Art) {
                Art a = (Art) item;
                pstmt.setString(1, "Art");

                pstmt.setNull(7, java.sql.Types.VARCHAR);
                pstmt.setNull(8, java.sql.Types.VARCHAR);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(13, java.sql.Types.INTEGER);
                pstmt.setNull(14, java.sql.Types.VARCHAR);
                pstmt.setNull(15, java.sql.Types.INTEGER);

                pstmt.setString(10, a.getArtistName());
                pstmt.setString(11, a.getMaterial());
                pstmt.setBoolean(12, a.isHasCertificateOfAuthenticity());

            } else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                pstmt.setString(1, "Vehicle");

                pstmt.setNull(7, java.sql.Types.VARCHAR);
                pstmt.setNull(8, java.sql.Types.VARCHAR);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(10, java.sql.Types.VARCHAR);
                pstmt.setNull(11, java.sql.Types.VARCHAR);
                pstmt.setNull(12, java.sql.Types.BOOLEAN);

                pstmt.setInt(13, v.getManufactureYear());
                pstmt.setString(14, v.getVinNumber());
                pstmt.setInt(15, v.getMileage());
            }

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Không có dòng nào được lưu.");
            }

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
                String name = rs.getString("name");
                String description = rs.getString("description");
                BigDecimal startingPrice = rs.getBigDecimal("startingPrice");
                String condition = rs.getString("item_condition");

                String imgString = rs.getString("imageUrls");
                List<String> imgList = new ArrayList<>();
                if (imgString != null && !imgString.isEmpty()) {
                    imgList = java.util.Arrays.asList(imgString.split(","));
                }

                if ("Electronics".equals(itemType)) {
                    String brand = rs.getString("brand");
                    String model = rs.getString("model");
                    int warrantyMonths = rs.getInt("warrantyMonths");
                    itemList.add(new Electronics(id, name, description, startingPrice, condition, imgList, brand, model, warrantyMonths));

                } else if ("Art".equals(itemType)) {
                    String artistName = rs.getString("artistName");
                    String material = rs.getString("material");
                    boolean hasCertificateOfAuthenticity = rs.getBoolean("hasCertificateOfAuthenticity");
                    itemList.add(new Art(id, name, description, startingPrice, condition, imgList, artistName, material, hasCertificateOfAuthenticity));

                } else if ("Vehicle".equals(itemType)) {
                    int manufactureYear = rs.getInt("manufactureYear");
                    String vinNumber = rs.getString("vinNumber");
                    int mileage = rs.getInt("mileage");
                    itemList.add(new Vehicle(id, name, description, startingPrice, condition, imgList, manufactureYear, mileage, vinNumber));
                }
            }
        } catch (Exception e) {
            logger.error("LỖI LẤY DANH SÁCH ITEM: {}", e.getMessage(), e);
        }

        return itemList;
    }

    // =====================================================================
    // HÀM 3: LẤY TÊN THEO ID (Giữ nguyên)
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
}