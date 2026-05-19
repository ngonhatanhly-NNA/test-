package com.shared.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class LoginResponseDTO implements Serializable {
    @SerializedName("profile")
    private UserProfileResponseDTO profile;

    @SerializedName("token")
    private String token;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(UserProfileResponseDTO profile, String token) {
        this.profile = profile;
        this.token = token;
    }

    public UserProfileResponseDTO getProfile() {
        return profile;
    }

    public String getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginResponseDTO that = (LoginResponseDTO) o;
        return Objects.equals(profile, that.profile) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, token);
    }
}