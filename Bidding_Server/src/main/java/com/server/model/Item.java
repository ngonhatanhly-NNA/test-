package com.server.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private BigDecimal startingPrice; // Chỉ có duy nhất 1, giá người bán muốn bán
    private String condition;
    private List<String> imageUrls;
    private int sellerId; // ID của seller tạo item này

    public Item(){};
    public Item(long id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice; // Chỉ xuất hiện đầu tiền
        this.condition = condition;
        
        this.imageUrls = new ArrayList<>(imageUrls);
        this.sellerId = 0;
    }

    public Item(long id, int sellerId, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls) {
        super(id);
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.condition = condition;
        this.imageUrls = new ArrayList<>(imageUrls);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name;}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description;}

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition;  }

    // Getter
    public BigDecimal getStartingPrice() { return startingPrice; }

    // Qản lí các bức ảnh vật phẩm
    public List<String> getImageUrls() { 
        return new ArrayList<>(imageUrls); // Trả về bản sao để tránh bị chỉnh sửa list gốc
    }
    
    public void addImageUrl(String url) {
        this.imageUrls.add(url);
    }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public abstract void printInfo();
}