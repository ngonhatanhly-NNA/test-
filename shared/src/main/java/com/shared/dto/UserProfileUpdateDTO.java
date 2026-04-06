package com.shared.dto;

public class UserProfileUpdateDTO {
    private String username;
    private String email;
    private String phoneNumber;
    private String address;

    // Constructor rỗng bắt buộc phải có để thằng Gson nó đọc được JSON
    public UserProfileUpdateDTO() {
    }

    // Constructor có tham số (phòng khi cần dùng khởi tạo nhanh)
    public UserProfileUpdateDTO(String username, String email, String phoneNumber, String address) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    // --- GETTERS ---
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    // --- SETTERS ---
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}