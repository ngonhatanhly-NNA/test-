package com.server.model;

import java.math.BigDecimal;
import java.util.List;

public class Vehicle extends Item {
    private int manufactureYear; // Năm sản xuất
    private String vinNumber;    // Không thể đổi
    private int mileage;               


    public Vehicle(){};

//    public Vehicle(int id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls, int manufactureYear, int mileage, String vinNumber) {
//        super(id, name, description, startingPrice, condition, imageUrls);
//        this.manufactureYear = manufactureYear;
//        this.mileage = mileage;
//        this.vinNumber = vinNumber;
//    }

    //Constructor Art include Builder Obj
    private Vehicle(Builder builder) {
        super(builder);
        this.manufactureYear = builder.manufactureYear;
        this.vinNumber = builder.vinNumber;
        this.mileage = builder.mileage;
    }

    //Vehicle.Builder
    public static class Builder extends Item.Builder<Builder> {
        private int manufactureYear;
        private String vinNumber;
        private int mileage;

        @Override
        protected Builder self() {return this;}

        public Builder manufactureYear(int manufactureYear) {this.manufactureYear = manufactureYear; return this;}
        public Builder vinNumber(String vinNumber) {this.vinNumber = vinNumber; return this;}
        public Builder mileage(int mileage) {this.mileage = mileage; return this;}

        @Override
        public Vehicle build() {return new Vehicle(this);}

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