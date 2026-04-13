package com.client.network;

import com.google.gson.Gson;
import com.shared.network.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Lớp mạng dành cho các chức năng của Admin.
 * Giao tiếp với các endpoint /api/admin/* trên server.
 */
public class AdminNetwork {

    private static final String SERVER_URL = "http://localhost:7070/api/admin";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    // ========================================================
    // 1. DASHBOARD
    // ========================================================

    /**
     * Lấy dữ liệu thống kê cho trang Dashboard của Admin.
     */
    public static Response getDashboardData() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/dashboard"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }

    // ========================================================
    // 2. QUẢN LÝ NGƯỜI DÙNG
    // ========================================================

    /**
     * Tìm kiếm thông tin một người dùng theo username.
     */
    public static Response searchUser(String username) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/users/search/" + username))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }

    /**
     * Gửi yêu cầu cấm tài khoản người dùng.
     */
    public static Response banUser(String username) throws Exception {
        String requestBody = gson.toJson(Map.of("username", username));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/users/ban"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }

    /**
     * Gửi yêu cầu gỡ cấm tài khoản người dùng.
     */
    public static Response unbanUser(String username) throws Exception {
        String requestBody = gson.toJson(Map.of("username", username));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/users/unban"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }

    // ========================================================
    // 3. QUẢN LÝ SELLER
    // ========================================================

    /**
     * Gửi yêu cầu phê duyệt một Bidder thành Seller.
     */
    public static Response approveSeller(String bidderUsername) throws Exception {
        String requestBody = gson.toJson(Map.of("username", bidderUsername));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/sellers/approve"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }

    // ========================================================
    // 4. QUẢN LÝ SẢN PHẨM & TÀI CHÍNH
    // ========================================================

    /**
     * Lấy dữ liệu phân tích về các sản phẩm.
     */
    public static Response getProductAnalytics() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/items/analytics"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }

    /**
     * Lấy dữ liệu ước tính doanh thu.
     */
    public static Response getRevenueEstimate() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/finance/revenue-estimate"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(httpResponse.body(), Response.class);
    }
}