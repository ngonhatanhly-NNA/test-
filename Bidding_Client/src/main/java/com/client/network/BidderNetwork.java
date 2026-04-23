package com.client.network;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.shared.network.Response;
import com.shared.dto.BidderProfileUpdateDTO;

public class BidderNetwork {
    private final Gson gson = new Gson();
    private static final String BASE_URL = "http://localhost:7070/api/bidders";

    /**
     * Cập nhật số dư (Nạp tiền hoặc Trừ tiền)
     * @param bidderId ID của người dùng
     * @param newBalance Số dư mới
     * @return Response từ server
     */
    public CompletableFuture<Response> updateBalance(long bidderId, BigDecimal newBalance) {
        // Tạo request body
        String jsonBody = gson.toJson(new UpdateBalanceRequest(newBalance));

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/" + bidderId + "/balance"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lấy thông tin Bidder theo Username
     * @param username Tên người dùng
     * @return Response chứa thông tin Bidder
     */
    public CompletableFuture<Response> getBidderByUsername(String username) {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/username?username=" + username))
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Lấy thông tin Bidder theo ID
     * @param bidderId ID của Bidder
     * @return Response chứa thông tin Bidder
     */
    public CompletableFuture<Response> getBidderById(long bidderId) {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/" + bidderId))
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    /**
     * Cập nhật thông tin hồ sơ Bidder
     * @param bidderId ID của Bidder
     * @param updateData Dữ liệu cần cập nhật
     * @return Response từ server
     */
    public CompletableFuture<Response> updateBidderProfile(long bidderId, BidderProfileUpdateDTO updateData) {
        String jsonBody = gson.toJson(updateData);

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/" + bidderId + "/profile"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
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
}
