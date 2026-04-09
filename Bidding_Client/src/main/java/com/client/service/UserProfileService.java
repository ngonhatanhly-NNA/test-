package com.client.service;

import com.client.network.AuthNetwork;
import com.google.gson.Gson;
import com.shared.network.Response;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;

/**
 * Service Layer - Tách biệt Business Logic khỏi UI Controller
 * Single Responsibility: chỉ xử lý user profile logic
 * Dependency Inversion: nhận AuthNetwork thông qua constructor (có thể mock)
 */
public class UserProfileService {
    private final AuthNetwork authNetwork;
    private final Gson gson;

    public UserProfileService() {
        this(new AuthNetwork(), new Gson());
    }

    // Constructor với dependency injection - dễ test
    public UserProfileService(AuthNetwork authNetwork, Gson gson) {
        this.authNetwork = authNetwork;
        this.gson = gson;
    }

    /**
     * Lấy user profile từ server bất đồng bộ
     */
    public CompletableFuture<com.shared.dto.IUserProfileDTO> getUserProfile(String username) {
        return authNetwork.getUserProfile(username)
            .thenApply(response -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    try {
                        String jsonData = gson.toJson(response.getData());
                        String role = extractRole(response.getData());
                        java.sql.ResultSet rs = null; // Mock ResultSet từ JSON
                        return com.shared.dto.UserProfileDTOFactory.createFromResultSet(rs, role);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                return null;
            });
    }

    /**
     * Cập nhật user profile
     */
    public CompletableFuture<Response> updateProfile(com.shared.dto.UserProfileUpdateDTO updateData) {
        return authNetwork.updateProfile(updateData);
    }

    /**
     * Lấy role từ response data
     */
    private String extractRole(Object data) {
        if (data instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) data;
            Object role = map.get("role");
            return role != null ? role.toString() : null;
        }
        return null;
    }
}

