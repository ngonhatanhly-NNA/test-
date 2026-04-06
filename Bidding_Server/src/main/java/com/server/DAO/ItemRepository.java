package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Item;
// Chú ý: Cần import thêm các class con như Electronics, Art...
import com.server.model.Art;
import com.server.model.Electronics;
import com.server.model.Vehicle;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

    // =====================================================================
    // HÀM 1: LƯU SẢN PHẨM MỚI VÀO DATABASE (Dùng khi Seller đăng bán)
    // =====================================================================
    public boolean saveItem(Item item) {

        // NHIỆM VỤ 1: Viết câu lệnh SQL INSERT
        // Bảng items có các cột: name, description, startingPrice, highestBid, category, status, ownerUsername
        String sql = "INSERT INTO items (item_type, name, description, startingPrice, item_condition, imageUrls, " +
                "brand, model, warrantyMonths, " +
                "artistName, material, hasCertificateOfAuthenticity, " +
                "manufactureYear, vinNumber, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getDBConnection().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // NHIỆM VỤ 2: Nhét dữ liệu từ Object 'item' vào các dấu '?' ở câu SQL trên
            // SET DỮ LIỆU CHO CÁC THUỘC TÍNH CHUNG (Của lớp Item)
            // (Lưu ý: Đoạn imageUrls hơi phức tạp, có thể tạm bỏ qua gán Null hoặc tự viết hàm ghép chuỗi)
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setBigDecimal(4, item.getStartingPrice());
            pstmt.setString(5, item.getCondition());
            pstmt.setString(6, String.join(",", item.getImageUrls())); // Ghép list thành chuỗi "anh1.jpg,anh2.jpg"

            // XỬ LÝ ĐA HÌNH: KIỂM TRA NÓ LÀ CON NÀO ĐỂ ÉP KIỂU VÀ LẤY THUỘC TÍNH RIÊNG (Items -> Art, Elec, Vehicle)
            // TODO Future Work: Sửa đống code downcasting bằng 1 lớp tổng quát chống lặp code nhé
            if (item instanceof Electronics) {
                Electronics e = (Electronics) item; // Ép kiểu từ Item xuống Electronics (Downcasting)
                pstmt.setString(1, "Electronics"); // Điền item_type

                // Set NULL cho các cột của Art và Vehicle (Từ số 10 đến 15)
                pstmt.setNull(10, java.sql.Types.VARCHAR);
                pstmt.setNull(11, java.sql.Types.VARCHAR);
                pstmt.setNull(12, java.sql.Types.BOOLEAN);
                pstmt.setNull(13, java.sql.Types.INTEGER);
                pstmt.setNull(14, java.sql.Types.VARCHAR);
                pstmt.setNull(13, java.sql.Types.INTEGER);

                // Điền dữ liệu của Electronics
                pstmt.setString(7, e.getBrand());
                pstmt.setString(8, e.getModel());
                pstmt.setInt(9, e.getWarrantyMonths());


            } else if (item instanceof Art) {
                Art a = (Art) item;
                pstmt.setString(1, "Art");

                // Set NULL cho Elec (7, 8, 9) va Veh (13, 14, 15)
                pstmt.setNull(7, java.sql.Types.VARCHAR);
                pstmt.setNull(8, java.sql.Types.VARCHAR);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(13, java.sql.Types.INTEGER);
                pstmt.setNull(14, java.sql.Types.VARCHAR);
                pstmt.setNull(13, java.sql.Types.INTEGER);

                // Điền dữ liệu của Art (10, 11, 12)
                pstmt.setString(10, a.getArtistName());
                pstmt.setString(11, a.getMaterial());
                pstmt.setBoolean(12, a.isHasCertificateOfAuthenticity());

            } else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                pstmt.setString(1, "Vehicle");

                //Set NULL CHO ELEC VA ART (7->12)
                pstmt.setNull(7, java.sql.Types.VARCHAR);
                pstmt.setNull(8, java.sql.Types.VARCHAR);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(10, java.sql.Types.VARCHAR);
                pstmt.setNull(11, java.sql.Types.VARCHAR);
                pstmt.setNull(12, java.sql.Types.BOOLEAN);


                // Điền dữ liệu của Vehicle (13, 14, 15)
                pstmt.setInt(13, v.getManufactureYear());
                pstmt.setString(14, v.getVinNumber());
                pstmt.setInt(15, v.getMileage());
            }

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            System.out.println("LỖI LƯU ITEM VÀO DATABASE: ");
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================================
    // HÀM 2: LẤY TOÀN BỘ DANH SÁCH SẢN PHẨM (Để hiển thị lên Dashboard)
    // =====================================================================
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();

        // NHIỆM VỤ 3: Lấy tất cả các cột
        String sql = "SELECT * FROM items";

        //Cơ chế try-with-resources đa luồng Connection + Thư viện HikariCP ở config giúp chuyển luồng Connect cho người khác dùng, tối ưu luồng
        try (Connection conn = DBConnection.getDBConnection().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // 1. LẤY CỘT QUAN TRỌNG NHẤT: PHÂN LOẠI ITEM
                String itemType = rs.getString("item_type");

                // 2. LẤY CÁC THUỘC TÍNH CHUNG (Của thằng Cha - Item)
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                BigDecimal startingPrice = rs.getBigDecimal("startingPrice");
                String condition = rs.getString("item_condition");

                // (Xử lý chuỗi ảnh: Cắt cái chuỗi "anh1.jpg,anh2.jpg" thành cái List)
                String imgString = rs.getString("imageUrls");
                List<String> imgList = new ArrayList<>();
                if (imgString != null && !imgString.isEmpty()) {
                    imgList = java.util.Arrays.asList(imgString.split(","));
                }

                // 3. RẼ NHÁNH TẠO OBJECT VÀ NHÉT THUỘC TÍNH RIÊNG
                if ("Electronics".equals(itemType)) {
                    // Tạo vỏ hộp Electronics
                    String brand = rs.getString("brand");
                    String model = rs.getString("model");
                    int warrantyMonths = rs.getInt("warrantyMonths");
                    Electronics e = new Electronics(id, name, description, startingPrice, condition, imgList, brand, model, warrantyMonths);
                    // Set thuộc tính chung (Dùng Constructor nhé) + Nhét thuộc tính RIÊNG của Electronics lấy từ Database

                    // Thêm vào danh sách gửi về
                    itemList.add(e);

                } else if ("Art".equals(itemType)) {
                    String artistName = rs.getString("artistName");
                    String material = rs.getString("material");
                    boolean hasCertificateOfAuthenticity = rs.getBoolean("hasCertificateOfAuthenticity");

                    Art a = new Art(id, name, description, startingPrice, condition, imgList, artistName, material, hasCertificateOfAuthenticity);

                    itemList.add(a);

                } else if ("Vehicle".equals(itemType)) {
                    int manufactureYear = rs.getInt("manufactureYear");
                    String vinNumber = rs.getString("vinNumber");
                    int mileage = rs.getInt("mileage");

                    Vehicle v = new Vehicle(id, name, description, startingPrice, condition, imgList, manufactureYear, mileage, vinNumber);

                    itemList.add(v);
                }
            }
        } catch (Exception e) {
            System.out.println("LỖI LẤY DANH SÁCH ITEM: ");
            e.printStackTrace();
        }

        return itemList;
    }
}