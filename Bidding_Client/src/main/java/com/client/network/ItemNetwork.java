package com.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;
import com.shared.network.Response;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemNetwork {
    // Không cần thuộc tính HttpClient client nữa
    private final Gson gson = new Gson();

    private static final String SERVER_URL = "http://localhost:7070/api/items";

    // Hàm gọi mạng (Trả về CompletableFuture để không làm đơ màn hình JavaFX)
    public CompletableFuture<List<ItemResponseDTO>> getAllItems() {

        // 1. Viết thư yêu cầu (Request)
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(SERVER_URL))
                .GET() // Xin dữ liệu thì dùng GET, gửi dữ liệu lên thì dùng POST
                .header("Accept", "application/json")
                .build();

        // 2. Giao cho Shipper chạy đi lấy hàng (Bất đồng bộ)
        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
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

    // Hàm lấy danh sách items của seller hiện tại
    public CompletableFuture<List<ItemResponseDTO>> getMyItems(long sellerId) {
        String url = SERVER_URL + "/seller/" + sellerId;
        System.out.println("ItemNetwork.getMyItems: Calling URL: " + url);

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String jsonString = response.body();
                    System.out.println("ItemNetwork.getMyItems: Response: " + jsonString);
                    Response serverResponse = gson.fromJson(jsonString, Response.class);

                    if ("SUCCESS".equals(serverResponse.getStatus())) {
                        String dataJson = gson.toJson(serverResponse.getData());
                        Type listType = new TypeToken<List<ItemResponseDTO>>(){}.getType();
                        List<ItemResponseDTO> items = gson.fromJson(dataJson, listType);
                        System.out.println("ItemNetwork.getMyItems: Successfully loaded " + items.size() + " items");
                        return items;
                    } else {
                        System.out.println("Lỗi từ Server: " + serverResponse.getMessage());
                        return new ArrayList<ItemResponseDTO>();
                    }
                })
                .exceptionally(e -> {
                    System.out.println("Lỗi kết nối mạng: " + e.getMessage());
                    e.printStackTrace();
                    return new ArrayList<ItemResponseDTO>();
                });
    }

    // Hàm gửi yêu cầu tạo sản phẩm lên Server
    public CompletableFuture<Response> createItem(CreateItemRequestDTO itemDTO) {
        String jsonBody = gson.toJson(itemDTO);

        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(SERVER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // GỌI THẲNG TỔNG ĐÀI (SERVER) ĐỂ LẤY SHIPPER (HTTP) CHUNG (Chứa Cookie)
        return NetworkClient.getInstance().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), Response.class))
                .exceptionally(e -> {
                    System.out.println("Lỗi mạng khi tạo Item: " + e.getMessage());
                    return new Response("ERROR", "Mất kết nối mạng cục bộ!", null);
                });
    }
}