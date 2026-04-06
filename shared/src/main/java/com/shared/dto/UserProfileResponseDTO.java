package com.shared.dto;

public class UserProfileResponseDTO {
    private int id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String role;
    private double walletBalance; // Cần thiết để hiển thị số dư

    // Constructor nhận các tham số tương ứng
    public UserProfileResponseDTO(int id, String username, String email, String fullName,
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

    // Getters để JavaFX xử lí
}