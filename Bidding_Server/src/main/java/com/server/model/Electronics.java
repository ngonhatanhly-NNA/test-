package com.server.model;

import java.math.BigDecimal;
import java.util.List;

public class Electronics extends Item {
    private String brand;     // Hãng không thể đổi
    private String model;     // Dòng máy không thể đổi
    private int warrantyMonths;     

    public Electronics(){};

//    public Electronics(int id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls, String brand, String model, int warrantyMonths) {
//        super(id, name, description, startingPrice, condition, imageUrls);
//        this.brand = brand;
//        this.model = model;
//        this.warrantyMonths = warrantyMonths;
//    }

    // Constructor nhận Builder của chính nó, rồi đẩy phần chung lên super(builder) cho Cha xử lý
    private Electronics(Builder builder) {
        super(builder);
        this.brand = builder.brand;
        this.model = builder.model;
        this.warrantyMonths = builder.warrantyMonths;
    }

    // Builder của Electronics kế thừa từ Builder của Item
    public static class Builder extends Item.Builder<Builder> {
        private String brand;
        private String model;
        private int warrantyMonths;

        @Override
        protected Builder self() { return this; } // Trả về chính mình

        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder warrantyMonths(int months) { this.warrantyMonths = months; return this; }

        @Override
        public Electronics build() {
            return new Electronics(this); // Gọi constructor private ở trên
        }
    }

    public String getBrand() { return brand; }
    public String getModel() { return model; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("Electronics: " + getName() + " | Brand: " + brand + " | Model: " + model + " | Warranty: " + warrantyMonths + " months");
    }
}