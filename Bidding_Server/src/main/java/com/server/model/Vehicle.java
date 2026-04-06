package com.server.model;

import java.util.List;

public class Vehicle extends Item {
    private int manufactureYear; // Năm sản xuất
    private String vinNumber;    // Không thể đổi
    private int mileage;               


    public Vehicle(){};
    public Vehicle(int id, String name, String description, double startingPrice, String condition, List<String> imageUrls, int manufactureYear, int mileage, String vinNumber) {
        super(id, name, description, startingPrice, condition, imageUrls);
        this.manufactureYear = manufactureYear;
        this.mileage = mileage;
        this.vinNumber = vinNumber;
    }

    public int getManufactureYear() { return manufactureYear; }
    public String getVinNumber() { return vinNumber; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { 
        if (mileage >= this.mileage) { // Số km chỉ tăng, k giảm
            this.mileage = mileage;
        } else {
            System.out.println("Lỗi: Số Km cập nhật không được nhỏ hơn số Km hiện tại.");
        }
    }

    @Override
    public void printInfo() {
        System.out.println("Vehicle: " + getName() + " | Year: " + manufactureYear + " | Mileage: " + mileage + "km | VIN: " + vinNumber);
    }
}