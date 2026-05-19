package com.shared.dto;

import java.io.Serializable;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

// dùng chung cho 3 người dùng
public class BaseProfileUpdateDTO implements IUserProfileDTO, Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    protected long id;

    @SerializedName("email")
    protected String email;

    @SerializedName("fullName")
    protected String fullName;

    @SerializedName("phoneNumber")
    protected String phoneNumber;

    @SerializedName("address")
    protected String address;

    public BaseProfileUpdateDTO() {}

    public BaseProfileUpdateDTO(long id, String email, String fullName, String phoneNumber, String address) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public BaseProfileUpdateDTO(String email, String fullName, String phoneNumber, String address) {
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseProfileUpdateDTO that = (BaseProfileUpdateDTO) o;
        return id == that.id && Objects.equals(email, that.email) && Objects.equals(fullName, that.fullName) &&
                Objects.equals(phoneNumber, that.phoneNumber) && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, fullName, phoneNumber, address);
    }

    @Override
    public String toString() {
        return "BaseProfileUpdateDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}