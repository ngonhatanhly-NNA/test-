package com.shared.dto;

public class SellerProfileUpdateDTO extends BidderProfileUpdateDTO {
    private String shopName;
    private String bankAccountNumber;

    public SellerProfileUpdateDTO() {
        super();
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
}