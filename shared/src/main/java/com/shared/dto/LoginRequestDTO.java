package com.shared.dto;

import java.io.Serializable;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

// DTO dùng cho Đăng nhập
public class LoginRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    public LoginRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequestDTO that = (LoginRequestDTO) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return "LoginRequestDTO{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}