package com.server.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ItemFactory {

    /**
     * @param type         Loại sản phẩm ("ELECTRONICS", "VEHICLE", "ART")
     * @param id           ID sản phẩm
     * @param name         Tên sản phẩm
     * @param description  Mô tả
     * @param startPrice   Giá khởi điểm (BigDecimal)
     * @param condition    Tình trạng (Mới, Cũ...)
     * @param imageUrls    Danh sách ảnh
     * @param extraProps   Map chứa các thuộc tính đặc thù của từng loại
     * @return Đối tượng Item tương ứng (Electronics, Vehicle, hoặc Art)
     */
    public static Item createItem(String type, int id, String name, String description,
                                  BigDecimal startPrice, String condition, 
                                  List<String> imageUrls, Map<String, Object> extraProps) {
        
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống!");
        }

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                // Ép kiểu các thuộc tính riêng của Electronics từ Map
                String brand = (String) extraProps.get("brand");
                String model = (String) extraProps.get("model");
                int warranty = extraProps.get("warrantyMonths") != null ? (Integer) extraProps.get("warrantyMonths") : 0;
                
                return new Electronics(id, name, description, startPrice, condition, imageUrls, brand, model, warranty);

            case "VEHICLE":
                // Giả định class Vehicle của bạn có các thuộc tính này
                String make = (String) extraProps.get("make");
                int year = extraProps.get("year") != null ? (Integer) extraProps.get("year") : 2000;
                double mileage = extraProps.get("mileage") != null ? (Double) extraProps.get("mileage") : 0.0;
                
                return new Vehicle(id, name, description, startPrice, condition, imageUrls, make, year, mileage);

            case "ART":
                // Giả định class Art của bạn có các thuộc tính này
                String artist = (String) extraProps.get("artist");
                String medium = (String) extraProps.get("medium"); // Chất liệu (Sơn dầu, Gỗ...)
                int yearCreated = extraProps.get("yearCreated") != null ? (Integer) extraProps.get("yearCreated") : 0;
                
                return new Art(id, name, description, startPrice, condition, imageUrls, artist, yearCreated, medium);

            default:
                throw new IllegalArgumentException("Hệ thống không hỗ trợ loại sản phẩm: " + type);
        }
    }
}