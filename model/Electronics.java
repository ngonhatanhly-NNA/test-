package com.server.model;

import java.math.BigDecimal;
import java.util.List;

public class Electronics extends Item {
    private String brand;     // Hãng không thể đổi
    private String model;     // Dòng máy không thể đổi
    private int warrantyMonths;     

    public Electronics(){};
    public Electronics(int id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls, String brand, String model, int warrantyMonths) {
        super(id, name, description, startingPrice, condition, imageUrls);
        this.brand = brand;
        this.model = model;
        this.warrantyMonths = warrantyMonths;
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