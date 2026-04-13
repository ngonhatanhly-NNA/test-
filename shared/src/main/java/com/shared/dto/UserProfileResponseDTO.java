package com.shared.dto;

import java.io.Serializable;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public class UserProfileResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private long id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("address")
    private String address;

    @SerializedName("role")
    private String role;

    @SerializedName("walletBalance")
    private double walletBalance; // Cần thiết để hiển thị số dư

    public UserProfileResponseDTO() {
    }

    // Constructor nhận các tham số tương ứng
    public UserProfileResponseDTO(long id, String username, String email, String fullName,
                                  String phoneNumber, String address, String role, double walletBalance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.walletBalance = walletBalance;
    }

    public String getRole() {
        return this.role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public double getWalletBalance() {
        return walletBalance;
    }
    // Getters để JavaFX xử lí

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfileResponseDTO that = (UserProfileResponseDTO) o;
        return id == that.id && Double.compare(that.walletBalance, walletBalance) == 0 &&
                Objects.equals(username, that.username) && Objects.equals(email, that.email) &&
                Objects.equals(fullName, that.fullName) && Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(address, that.address) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, fullName, phoneNumber, address, role, walletBalance);
    }

    @Override
    public String toString() {
        return "UserProfileResponseDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", address='" + address + '\'' +
                ", role='" + role + '\'' +
                ", walletBalance=" + walletBalance +
                '}';
    }
}