package com.shared.dto;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public class SellerProfileUpdateDTO extends BidderProfileUpdateDTO {
    @SerializedName("shopName")
    private String shopName;

    @SerializedName("bankAccountNumber")
    private String bankAccountNumber;

    public SellerProfileUpdateDTO() {
        super();
    }

    public SellerProfileUpdateDTO(String email, String fullName, String phoneNumber, String address,
                                   String creditCardInfo, String shopName, String bankAccountNumber) {
        super(email, fullName, phoneNumber, address, creditCardInfo);
        this.shopName = shopName;
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SellerProfileUpdateDTO that = (SellerProfileUpdateDTO) o;
        return Objects.equals(shopName, that.shopName) && Objects.equals(bankAccountNumber, that.bankAccountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), shopName, bankAccountNumber);
    }

    @Override
    public String toString() {
        return "SellerProfileUpdateDTO{" +
                "shopName='" + shopName + '\'' +
                ", bankAccountNumber='" + bankAccountNumber + '\'' +
                ", " + super.toString() +
                '}';
    }
}