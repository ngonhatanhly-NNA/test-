package com.client.network;

import com.google.gson.Gson;
import com.shared.network.Response;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Lớp mạng dành cho các chức năng của Admin.
 * Giao tiếp với các endpoint /api/admin/* trên server.
 * Dùng CompletableFuture để không làm đơ UI JavaFX
 */
public class AdminNetwork {

    private static final String BASE_URL = "http://localhost:7070/api/admin";
    private static final Gson gson = new Gson();

    // ========================================================
    // 1. DASHBOARD
    // ========================================================

    /**
     * Lấy dữ liệu thống kê cho trang Dashboard của Admin.
     */
    public static CompletableFuture<Response> getDashboardData() {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/dashboard"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> {
                    System.out.println("Lỗi lấy dashboard data: " + e.getMessage());
                    return new Response("ERROR", "Không thể lấy dữ liệu dashboard", null);
                });
    }

    // ========================================================
    // 2. QUẢN LÝ NGƯỜI DÙNG
    // ========================================================

    /**
     * Lấy danh sách tất cả người dùng
     */
    public static CompletableFuture<Response> getAllUsers() {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/users"))
                .GET()
                .header("Accept", "application/json")
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Lỗi lấy danh sách user", null));
    }

    /**
     * Tìm kiếm thông tin một người dùng theo username.
     */
    public static CompletableFuture<Response> searchUser(String username) {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/users/search/" + username))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Người dùng không tồn tại", null));
    }

    /**
     * Gửi yêu cầu cấm tài khoản người dùng.
     */
    public static CompletableFuture<Response> banUser(String username, String reason) {
        String requestBody = gson.toJson(Map.of(
                "username", username,
                "reason", reason != null ? reason : "Vi phạm điều khoản"
        ));

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/users/ban"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể cấm tài khoản", null));
    }

    /**
     * Gửi yêu cầu gỡ cấm tài khoản người dùng.
     */
    public static CompletableFuture<Response> unbanUser(String username) {
        String requestBody = gson.toJson(Map.of("username", username));

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/users/unban"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể gỡ cấm tài khoản", null));
    }

    // ========================================================
    // 3. QUẢN LÝ SELLER
    // ========================================================

    /**
     * Gửi yêu cầu phê duyệt một Bidder thành Seller.
     */
    public static CompletableFuture<Response> approveSeller(String bidderUsername, String shopName, String bankAccount) {
        String requestBody = gson.toJson(Map.of(
                "username", bidderUsername,
                "shopName", shopName,
                "bankAccountNumber", bankAccount
        ));

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/sellers/approve"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể phê duyệt seller", null));
    }

    // ========================================================
    // 4. QUẢN LÝ SẢN PHẨM & TÀI CHÍNH
    // ========================================================

    /**
     * Lấy dữ liệu phân tích về các sản phẩm.
     */
    public static CompletableFuture<Response> getProductAnalytics() {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/items/analytics"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể lấy analytics", null));
    }

    /**
     * Lấy dữ liệu ước tính doanh thu.
     */
    public static CompletableFuture<Response> getRevenueEstimate() {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/finance/revenue-estimate"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể lấy doanh thu", null));
    }

    /**
     * Hủy phiên đấu giá
     */
    public static CompletableFuture<Response> cancelAuction(long auctionId, String reason) {
        String requestBody = gson.toJson(Map.of(
                "auctionId", auctionId,
                "reason", reason != null ? reason : "Admin cancelled"
        ));

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/auctions/cancel"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể hủy phiên đấu giá", null));
    }

    /**
     * Lấy lịch hoạt động của user
     */
    public static CompletableFuture<Response> getUserActivityLog(String username) {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/users/" + username + "/activity"))
                .GET()
                .header("Accept", "application/json")
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> new Response("ERROR", "Không thể lấy lịch hoạt động", null));
    }
}