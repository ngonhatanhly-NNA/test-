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

    // Khối static Đăng ký Creator
    static {
        registry.put("ELECTRONICS", (id, sellerId, name, desc, price, cond, imgs, props) -> {
            String brand = (String) props.get("brand");
            String model = (String) props.get("model");
            int warranty = props.get("warrantyMonths") != null ? (Integer) props.get("warrantyMonths") : 0;
            Electronics e = new Electronics(id, name, desc, price, cond, imgs, brand, model, warranty);
            e.setSellerId(sellerId);
            return e;
        });

        registry.put("VEHICLE", (id, sellerId, name, desc, price, cond, imgs, props) -> {
            int manufactureYear = props.get("manufactureYear") != null ? (Integer) props.get("manufactureYear") : 2000;
            int mileage = props.get("mileage") != null ? (Integer) props.get("mileage") : 0;
            String vinNumber = (String) props.get("vinNumber");
            Vehicle v = new Vehicle(id, name, desc, price, cond, imgs, manufactureYear, mileage, vinNumber);
            v.setSellerId(sellerId);
            return v;
        });

        registry.put("ART", (id, sellerId, name, desc, price, cond, imgs, props) -> {
            String artistName = (String) props.get("artistName");
            String material = (String) props.get("material");
            boolean hasCertificate = props.get("hasCertificateOfAuthenticity") != null ? (Boolean) props.get("hasCertificateOfAuthenticity") : false;
            Art a = new Art(id, name, desc, price, cond, imgs, artistName, material, hasCertificate);
            a.setSellerId(sellerId);
            return a;
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
}