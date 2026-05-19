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

//    public Item(long id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls) {
//        super(id);
//        this.name = name;
//        this.description = description;
//        this.startingPrice = startingPrice; // Chỉ xuất hiện đầu tiền
//        this.condition = condition;
//
//        this.imageUrls = new ArrayList<>(imageUrls);
//        this.sellerId = 0;
//    }

//    public Item(long id, int sellerId, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls) {
//        super(id);
//        this.sellerId = sellerId;
//        this.name = name;
//        this.description = description;
//        this.startingPrice = startingPrice;
//        this.condition = condition;
//        this.imageUrls = new ArrayList<>(imageUrls);
//    }

        // [THAY THẾ CONSTRUCTOR CŨ]
        // Constructor này nhận vào một đối tượng Builder để gán giá trị.
        // Dấu <?> nghĩa là nó chấp nhận bất kỳ Builder nào kế thừa từ Item.Builder.
        protected Item(Builder<?> builder) {
            super(builder.id); // Gọi lên Entity cha để gán ID
            this.name = builder.name;
            this.description = builder.description;
            this.startingPrice = builder.startingPrice;
            this.condition = builder.condition;
            this.imageUrls = builder.imageUrls != null ? builder.imageUrls : new ArrayList<>();
            this.sellerId = builder.sellerId;
        }

        // --- CẤU TRÚC BUILDER TRỪU TƯỢNG ---
        // T là kiểu của Builder con (để khi gọi hàm nó trả về đúng kiểu con, giúp chain được tiếp)
        public abstract static class Builder<T extends Builder<T>> {    //Recursive Generics
            private long id;
            private String name;
            private String description;
            private BigDecimal startingPrice;
            private String condition;
            private List<String> imageUrls;
            private int sellerId;

            // Hàm này cực kỳ quan trọng để "ép kiểu" ngược lại cho các lớp model con
            protected abstract T self();

            public T id(long id) { this.id = id; return self(); }
            public T name(String name) { this.name = name; return self(); }
            public T description(String description) { this.description = description; return self(); }
            public T startingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; return self(); }
            public T condition(String condition) { this.condition = condition; return self(); }
            public T imageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; return self(); }
            public T sellerId(int sellerId) { this.sellerId = sellerId; return self(); }

            public abstract Item build();
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