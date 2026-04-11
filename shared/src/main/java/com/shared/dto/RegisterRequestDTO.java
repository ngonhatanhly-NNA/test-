package com.shared.dto;

import java.io.Serializable;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

// DTO dùng cho Đăng ký
public class RegisterRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password; // Client gửi mật khẩu chưa băm

    @SerializedName("email")
    private String email;

    @SerializedName("fullName")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRequestDTO that = (RegisterRequestDTO) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password) &&
                Objects.equals(email, that.email) && Objects.equals(fullName, that.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, email, fullName);
    }

    @Override
    public String toString() {
        return "RegisterRequestDTO{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}