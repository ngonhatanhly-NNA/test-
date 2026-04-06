package com.shared.dto;

// DTO dùng cho Đăng ký
public class RegisterRequestDTO {
    private String username;
    private String password; // Client gửi mật khẩu chưa băm
    private String email;
    private String fullName;

    public RegisterRequestDTO(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
}