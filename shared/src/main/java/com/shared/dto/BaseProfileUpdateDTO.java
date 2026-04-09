package com.shared.dto;

// dùng chung cho 3 người dùng
public abstract class BaseProfileUpdateDTO implements IUserProfileDTO {
    protected String email;
    protected String fullName;
    protected String phoneNumber;
    protected String address;

    public BaseProfileUpdateDTO() {}

    public BaseProfileUpdateDTO(String email, String fullName, String phoneNumber, String address) {
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    @Override
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}