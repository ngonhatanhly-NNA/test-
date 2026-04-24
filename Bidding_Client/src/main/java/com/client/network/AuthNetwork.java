package com.client.network;

import com.google.gson.Gson;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.shared.network.*;
import com.shared.dto.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;

public class AuthNetwork {
    private final Gson gson = new Gson();

    public CompletableFuture<Response> register (RegisterRequestDTO dto){
        String jsonBody = gson.toJson(dto);

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create("http://localhost:7070/api/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        //  Gửi đi và chờ Server trả lời
        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response ->
                gson.fromJson(response.body(), Response.class));
    }

    public CompletableFuture<Response> login(LoginRequestDTO loginData){
        String jsonBody = gson.toJson(loginData);

        // Gửi APi
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create("http://localhost:7070/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), com.shared.network.Response.class)
        );
    }

    // Lấy thông tin User Profile
    public CompletableFuture<Response> getUserProfile(String username) {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create("http://localhost:7070/api/users/profile?username=" + username))
                .GET()
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    // Cập nhật User Profile, BaseProfile theo poly thi thoai mai voi Seller, Admin nha
    public CompletableFuture<Response> updateProfile(BaseProfileUpdateDTO updateData) {
        String jsonBody = gson.toJson(updateData);

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create("http://localhost:7070/api/users/update"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody)) // Dùng PUT hoặc POST tuỳ backend
                .build();

        return NetworkClient.getInstance()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((httpRes, ex) -> {
                    if (ex != null) {
                        return new Response("ERROR", "Không thể kết nối tới Server: " + ex.getMessage(), null);
                    }

                    String body = httpRes.body();
                    int code = httpRes.statusCode();

                    try {
                        Response parsed = gson.fromJson(body, Response.class);
                        if (parsed != null) {
                            return parsed;
                        }
                    } catch (Exception ignored) {
                        // body có thể là HTML khi server 500, hoặc text thường
                    }

                    String briefBody = (body == null) ? "" : body.trim();
                    if (briefBody.length() > 250) {
                        briefBody = briefBody.substring(0, 250) + "...";
                    }
                    return new Response("ERROR", "HTTP " + code + " - Response không phải JSON: " + briefBody, null);
                });
    }

    /**
     * Gửi yêu cầu nâng cấp vai trò thành SELLER.
     * @param username Tên người dùng cần nâng cấp.
     * @return Một CompletableFuture chứa đối tượng Response từ server.
     */
    public CompletableFuture<Response> requestUpgradeToSeller(String username) {
        // Endpoint này phải khớp với endpoint bạn định nghĩa ở Server
        // Giả sử server sẽ lấy username từ session/token, nên body có thể rỗng
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create("http://localhost:7070/api/users/upgrade-to-seller"))
                .POST(HttpRequest.BodyPublishers.noBody()) // Gửi yêu cầu POST không có body
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }
}
