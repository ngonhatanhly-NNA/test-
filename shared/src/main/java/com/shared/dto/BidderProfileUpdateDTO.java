package com.shared.dto;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public class BidderProfileUpdateDTO extends BaseProfileUpdateDTO {
    @SerializedName("creditCardInfo")
    protected String creditCardInfo;

    public BidderProfileUpdateDTO() {
        super();
    }

    public BidderProfileUpdateDTO(String email, String fullName, String phoneNumber, String address, String creditCardInfo) {
        super(email, fullName, phoneNumber, address);
        this.creditCardInfo = creditCardInfo;
    }

    public String getCreditCardInfo() { return creditCardInfo; }
    public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BidderProfileUpdateDTO that = (BidderProfileUpdateDTO) o;
        return Objects.equals(creditCardInfo, that.creditCardInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), creditCardInfo);
    }

    @Override
    public String toString() {
        return "BidderProfileUpdateDTO{" +
                "creditCardInfo='" + creditCardInfo + '\'' +
                ", " + super.toString() +
                '}';
    }
}