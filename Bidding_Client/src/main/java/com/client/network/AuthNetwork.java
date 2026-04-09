package com.client.network;

import com.google.gson.Gson;
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
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public CompletableFuture<Response> register (RegisterRequestDTO dto){
        String jsonBody = gson.toJson(dto);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder() // HTTP Request có dạng uri/header/method // build 1 lệnh mới gửi đi
                .uri(URI.create("http://localhost:7070/api/register")) // Gọi đúng đường dẫn API
                .header("Auction", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        //  Gửi đi và chờ Server trả lời
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response ->
                gson.fromJson(response.body(), Response.class));
    }

    public CompletableFuture<Response> login(LoginRequestDTO loginData){
        String jsonBody = gson.toJson(loginData);

        // Gửi APi
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), com.shared.network.Response.class)
        );
    }

    // Lấy thông tin User Profile
    public CompletableFuture<Response> getUserProfile(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/api/users/profile?username=" + username))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }

    // Cập nhật User Profile, BaseProfile theo poly thi thoai mai voi Seller, Admin nha
    public CompletableFuture<Response> updateProfile(BaseProfileUpdateDTO updateData) {
        String jsonBody = gson.toJson(updateData);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/api/users/update"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody)) // Dùng PUT hoặc POST tuỳ backend
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(
                response -> gson.fromJson(response.body(), Response.class)
        );
    }
}
