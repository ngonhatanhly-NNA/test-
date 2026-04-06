package com.server.model;

import java.math.BigDecimal;
import java.util.List;

public class Art extends Item {
    private String artistName; // Tên tác giả không thể đổi
    private String material;
    private boolean hasCertificateOfAuthenticity; // Có thể cập nhật nếu sau này tìm thấy giấy chứng nhận

    // Artist phải đc certificated
    public Art(){};
    public Art(int id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls, String artistName, String material, boolean hasCertificateOfAuthenticity) {
        super(id, name, description, startingPrice, condition, imageUrls);
        this.artistName = artistName;
        this.material = material;
        this.hasCertificateOfAuthenticity = hasCertificateOfAuthenticity;
    }

    public String getArtistName() { return artistName; }
    public void setArtistName(String newName) {artistName = newName;}
    public String getMaterial() { return material; }

    public boolean isHasCertificateOfAuthenticity() { return hasCertificateOfAuthenticity; }
    public void setHasCertificateOfAuthenticity(boolean hasCertificateOfAuthenticity) {
        this.hasCertificateOfAuthenticity = hasCertificateOfAuthenticity;
    }

    @Override
    public void printInfo() {
        System.out.println("Art: " + getName() + " | Artist: " + artistName + " | Material: " + material + " | Authentic: " + hasCertificateOfAuthenticity);
    }
}