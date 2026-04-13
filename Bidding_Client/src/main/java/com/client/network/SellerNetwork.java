package com.client.network;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.shared.network.Response;
import com.shared.dto.SellerProfileUpdateDTO;

public class SellerNetwork {
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private static final String BASE_URL = "http://localhost:7070/api/sellers";

    /**
     * Cập nhật số dư (Nạp tiền hoặc Trừ tiền cho Seller)
     * @param sellerId ID của Seller
     * @param newBalance Số dư mới
     * @return Response từ server
     */
    public CompletableFuture<Response> updateBalance(long sellerId, BigDecimal newBalance) {
        // Tạo request body
        String jsonBody = gson.toJson(new UpdateBalanceRequest(newBalance));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + sellerId + "/balance"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lấy thông tin Seller theo Username
     * @param username Tên người dùng
     * @return Response chứa thông tin Seller
     */
    public CompletableFuture<Response> getSellerByUsername(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/username?username=" + username))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lấy thông tin Seller theo ID
     * @param sellerId ID của Seller
     * @return Response chứa thông tin Seller
     */
    public CompletableFuture<Response> getSellerById(long sellerId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + sellerId))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Cập nhật thông tin hồ sơ Seller (bao gồm shopName và bankAccount)
     * @param sellerId ID của Seller
     * @param updateData Dữ liệu cần cập nhật
     * @return Response từ server
     */
    public CompletableFuture<Response> updateSellerProfile(long sellerId, SellerProfileUpdateDTO updateData) {
        String jsonBody = gson.toJson(updateData);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + sellerId + "/profile"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lấy danh sách các item đang bán của Seller
     * @param sellerId ID của Seller
     * @return Response chứa danh sách items
     */
    public CompletableFuture<Response> getSellerItems(long sellerId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + sellerId + "/items"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lấy thống kê doanh thu của Seller
     * @param sellerId ID của Seller
     * @return Response chứa thông tin thống kê
     */
    public CompletableFuture<Response> getSellerStatistics(long sellerId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + sellerId + "/statistics"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Rút tiền từ ví của Seller về tài khoản ngân hàng
     * @param sellerId ID của Seller
     * @param amount Số tiền cần rút
     * @return Response từ server
     */
    public CompletableFuture<Response> withdrawMoney(long sellerId, BigDecimal amount) {
        String jsonBody = gson.toJson(new WithdrawRequest(amount));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + sellerId + "/withdraw"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lớp helper để gửi request cập nhật số dư
     */
    private static class UpdateBalanceRequest {
        private final BigDecimal newBalance;

        public UpdateBalanceRequest(BigDecimal newBalance) {
            this.newBalance = newBalance;
        }

        public BigDecimal getNewBalance() {
            return newBalance;
        }
    }

    /**
     * Lớp helper để gửi request rút tiền
     */
    private static class WithdrawRequest {
        private final BigDecimal amount;

        public WithdrawRequest(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
