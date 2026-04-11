package com.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shared.dto.ItemResponseDTO;
import com.shared.network.Response;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemNetwork {
    private final HttpClient client;
    private final Gson gson;

    // Địa chỉ của cái Lễ tân Server em setup ở ApiRouter
    private static final String SERVER_URL = "http://localhost:7070/api/items";

    public ItemNetwork() {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson(); // Lúc này Client MỚI CẦN dùng Gson để dịch JSON về lại Object
    }

    // Hàm gọi mạng (Trả về CompletableFuture để không làm đơ màn hình JavaFX)
    public CompletableFuture<List<ItemResponseDTO>> getAllItems() {

        // 1. Viết thư yêu cầu (Request)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .GET() // Xin dữ liệu thì dùng GET, gửi dữ liệu lên thì dùng POST
                .header("Accept", "application/json")
                .build();

        // 2. Giao cho Shipper chạy đi lấy hàng (Bất đồng bộ)
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // 3. Khui thùng hàng từ Server gửi về (Chuỗi String JSON)
                    String jsonString = response.body();

                    // 4. Dịch cục JSON đó thành cái form Response chung của nhóm
                    Response serverResponse = gson.fromJson(jsonString, Response.class);

                    // 5. Kiểm tra xem Server báo SUCCESS hay FAIL
                    if ("SUCCESS".equals(serverResponse.getStatus())) {

                        // --- TRICK CỦA GSON ĐỂ DỊCH LIST ---
                        // Vì thuộc tính "data" trong Response là kiểu Object chung chung,
                        // ta phải ép nó về đúng kiểu List<ItemResponseDTO>
                        String dataJson = gson.toJson(serverResponse.getData());
                        Type listType = new TypeToken<List<ItemResponseDTO>>(){}.getType();
                        List<ItemResponseDTO> items = gson.fromJson(dataJson, listType);

                        return items; // Trả danh sách về cho UI

                    } else {
                        System.out.println("Lỗi từ Server: " + serverResponse.getMessage());
                        return new ArrayList<ItemResponseDTO>(); // Lỗi thì trả về danh sách rỗng
                    }
                })
                .exceptionally(e -> {
                    // Bắt lỗi nếu Server bị tắt ngang, đứt cáp mạng...
                    System.out.println("Lỗi kết nối mạng: " + e.getMessage());
                    return new ArrayList<ItemResponseDTO>();
                });
    }
}