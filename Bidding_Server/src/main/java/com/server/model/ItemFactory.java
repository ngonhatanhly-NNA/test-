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
                String brand = (String) extraProps.get("brand");
                String model = (String) extraProps.get("model");
                int warranty = extraProps.get("warrantyMonths") != null ? (Integer) extraProps.get("warrantyMonths") : 0;

                return new Electronics(id, name, description, startPrice, condition, imageUrls, brand, model, warranty);

            case "VEHICLE":
                int manufactureYear = extraProps.get("manufactureYear") != null ? (Integer) extraProps.get("manufactureYear") : 2000;
                int mileage = extraProps.get("mileage") != null ? (Integer) extraProps.get("mileage") : 0;
                String vinNumber = (String) extraProps.get("vinNumber"); // Sửa 'make' ảo tưởng thành vinNumber chuẩn

                return new Vehicle(id, name, description, startPrice, condition, imageUrls, manufactureYear, mileage, vinNumber);

            case "ART":
                String artistName = (String) extraProps.get("artistName");
                String material = (String) extraProps.get("material");
                boolean hasCertificate = extraProps.get("hasCertificateOfAuthenticity") != null ? (Boolean) extraProps.get("hasCertificateOfAuthenticity") : false;

                return new Art(id, name, description, startPrice, condition, imageUrls, artistName, material, hasCertificate);

            default:
                throw new IllegalArgumentException("Hệ thống không hỗ trợ loại sản phẩm: " + type);
        }
    }
}