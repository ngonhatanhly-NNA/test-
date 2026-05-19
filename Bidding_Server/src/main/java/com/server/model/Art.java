package com.server.model;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Art extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Art.class);
    private String artistName; // Tên tác giả không thể đổi
    private String material;
    private boolean hasCertificateOfAuthenticity; // Có thể cập nhật nếu sau này tìm thấy giấy chứng nhận

    // Artist phải đc certificated
    public Art(){};

//    public Art(int id, String name, String description, BigDecimal startingPrice, String condition, List<String> imageUrls, String artistName, String material, boolean hasCertificateOfAuthenticity) {
//        super(id, name, description, startingPrice, condition, imageUrls);
//        this.artistName = artistName;
//        this.material = material;
//        this.hasCertificateOfAuthenticity = hasCertificateOfAuthenticity;
//    }

    private Art(Builder builder) {
        super(builder);
        this.artistName = builder.artistName;
        this.material = builder.material;
        this.hasCertificateOfAuthenticity = builder.hasCertificateOfAuthenticity;
    }

    public static class Builder extends Item.Builder<Builder> {
        private String artistName;
        private String material;
        private boolean hasCertificateOfAuthenticity;

        @Override
        protected Builder self() {return this;}

        public Builder artistName(String artistName) {this.artistName = artistName; return this;}
        public Builder material(String material) {this.material = material; return this;}
        public Builder hasCertificateOfAuthenticity(boolean hasCertificate) {this.hasCertificateOfAuthenticity = hasCertificate; return this;}

        @Override
        public Art build() {return new Art(this);}
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
        logger.info("Art: {} | Artist: {} | Material: {} | Authentic: {}", getName(), artistName, material, hasCertificateOfAuthenticity);
    }
}