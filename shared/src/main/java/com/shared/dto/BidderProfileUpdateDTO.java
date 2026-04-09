package com.shared.dto;

public class BidderProfileUpdateDTO extends BaseProfileUpdateDTO {
    protected String creditCardInfo;

    public BidderProfileUpdateDTO() {
        super();
    }

    public String getCreditCardInfo() { return creditCardInfo; }
    public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }
}