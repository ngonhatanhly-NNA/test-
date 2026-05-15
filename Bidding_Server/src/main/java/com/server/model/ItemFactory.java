package com.server.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemFactory {

    // Functional Interface đại diện cho hàm khởi tạo Item
    @FunctionalInterface
    public interface ItemCreator {
        Item create(int id, int sellerId, String name, String description, BigDecimal startPrice,
                    String condition, List<String> imageUrls, Map<String, Object> extraProps);
    }

    // Registry lưu trữ các bộ khởi tạo
    private static final Map<String, ItemCreator> registry = new HashMap<>();

    // Khối static Đăng ký Creator (Đóng vai trò Director điều phối các Builder)
    static {
        registry.put("ELECTRONICS", (id, sellerId, name, desc, price, cond, imgs, props) -> {
            // Thay vì gọi new Electronics(id, name, desc...) dài ngoằng
            // Ta gọi Electronics.Builder() và bắt đầu chuỗi (chaining) Lắp ráp.
            // Các thuộc tính chung (id, name...) được nối mượt mà với thuộc tính riêng (brand, model...)
            return new Electronics.Builder()
                    .id(id) // Lắp ID (Thuộc tính của lớp Cha)
                    .sellerId(sellerId) // Lắp ID người bán (Thuộc tính của lớp Cha)
                    .name(name) // Lắp Tên (Thuộc tính của lớp Cha)
                    .description(desc) // Lắp Mô tả
                    .startingPrice(price) // Lắp Giá khởi điểm
                    .condition(cond) // Lắp Tình trạng
                    .imageUrls(imgs) // Lắp danh sách Ảnh
                    // Bắt đầu lắp các thuộc tính ĐẶC THÙ của lớp Con (Electronics)
                    .brand(asString(props.get("brand")))
                    .model(asString(props.get("model")))
                    .warrantyMonths(asInt(props.get("warrantyMonths"), 0))
                    // Lệnh CHỐT: Gọi build() để thợ xây trả về sản phẩm hoàn thiện
                    .build();
        });

        registry.put("VEHICLE", (id, sellerId, name, desc, price, cond, imgs, props) -> {
            return new Vehicle.Builder()
                    .id(id)
                    .sellerId(sellerId)
                    .name(name)
                    .description(desc)
                    .startingPrice(price)
                    .condition(cond)
                    .imageUrls(imgs)
                    .manufactureYear(asInt(props.get("manufactureYear"), 2000))
                    .mileage(asInt(props.get("mileage"), 0))
                    .vinNumber(asString(props.get("vinNumber")))
                    .build();
        });

        registry.put("ART", (id, sellerId, name, desc, price, cond, imgs, props) -> {
            // =====================================================================
            // [NHIỆM VỤ CỦA HAWKIE]: Tự tay gõ lại đoạn này bằng Art.Builder()
            // Cẩn thận kiểu boolean của hasCertificateOfAuthenticity
            // Gợi ý: .hasCertificateOfAuthenticity(asBoolean(props.get("hasCertificateOfAuthenticity"), false))
            // =====================================================================

            return new Art.Builder()
                    .id(id)
                    .sellerId(sellerId)
                    .name(name)
                    .description(desc)
                    .startingPrice(price)
                    .condition(cond)
                    .imageUrls(imgs)
                    .artistName(asString(props.get("artist")))
                    .material(asString(props.get("material")))
                    .hasCertificateOfAuthenticity(asBoolean(props.get("hasCertificateOfAuthenticity"), false))
                    .build();
        });
    }

    // Sau này có type goị: ItemFactory.registerCreator("REAL_ESTATE", creatorLambda);
    public static void registerCreator(String type, ItemCreator creator) {
        registry.put(type.toUpperCase(), creator);
    }

    public static Item createItem(String type, int id, int sellerId, String name, String description,
                                  BigDecimal startPrice, String condition,
                                  List<String> imageUrls, Map<String, Object> extraProps) {

        //Kiểm tra đầu vào đề bảo vệ code (Không bao giờ tin input từ Frontend)
        if (imageUrls == null || imageUrls.isEmpty()) {imageUrls = new ArrayList<>();}  //Fix tạm thời để xử lý ảnh đầu vào sau
        if (extraProps == null || extraProps.isEmpty()) {extraProps = new HashMap<>();}

        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống!");
        }

        ItemCreator creator = registry.get(type.toUpperCase());
        if (creator == null) {
            throw new IllegalArgumentException("Hệ thống không hỗ trợ loại sản phẩm: " + type);
        }

        return creator.create(id, sellerId, name, description, startPrice, condition, imageUrls, extraProps);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static int asInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = String.valueOf(value).trim().toLowerCase();
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        return normalized.equals("true") || normalized.equals("yes") || normalized.equals("1");
    }
}