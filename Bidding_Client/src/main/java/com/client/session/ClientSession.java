package com.client.session;

import com.shared.dto.UserProfileResponseDTO;

/**
 * Giữ user đã đăng nhập để các màn JavaFX (đặt giá, dashboard) dùng cùng một userId với server.
 */
public final class ClientSession {

    private static long userId;
    private static String username = "";
    private static String role = "";

    private ClientSession() {
    }

    public static void setUser(UserProfileResponseDTO profile) {
        if (profile == null) {
            clear();
            return;
        }
        userId = profile.getId();
        username = profile.getUsername() != null ? profile.getUsername() : "";
        role = profile.getRole() != null ? profile.getRole() : "";
    }

    public static void clear() {
        userId = 0L;
        username = "";
        role = "";
    }

    public static long getUserId() {
        return userId;
    }

    public static String getUsername() {
        return username;
    }

    public static String getRole() {
        return role;
    }

    public static boolean isLoggedIn() {
        return userId > 0;
    }
}
